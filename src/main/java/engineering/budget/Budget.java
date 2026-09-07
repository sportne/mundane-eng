package engineering.budget;

import static engineering.artifacts.Checks.*;
import engineering.artifacts.*;
import engineering.domain.*;
import engineering.architecture.Architecture;
import engineering.configuration.Configuration;
import engineering.equipment.Equipment;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import mundanereq.Versions;

/** Bounded resource budgets and conditional repairable availability; no safety scoring. */
public final class Budget implements Model.Domain {
    public String kind(){return "budget";}
    public String format(){return Versions.BUDGET_ARTIFACT;}
    public String source(){return Versions.BUDGET_SOURCE;}
    public String version(){return Versions.BUDGET_VERSION;}
    public String contract(){return Versions.BUDGET_CONTRACT;}
    public static Model.Context context(Path root){return new Model.Context(root,Map.of("architecture",new Architecture(),"configuration",new Configuration(),"equipment",new Equipment(),"budget",new Budget()));}
    public Map<String,Object> lookup(Map<String,Object> d,String kind,String ident){if(!kind.equals("budget")||!d.get("id").equals(ident))throw new IllegalArgumentException("missing budget target");return d;}
    public void validate(Map<String,Object> d,Model.Context c){analyze(d,c);}
    public Map<String,Object> analyze(Map<String,Object> d,Model.Context c) {
        var authored=new TreeMap<>(d);authored.put("format",source());Schema.validate(authored,Json.read(BudgetSchema.JSON.getBytes(StandardCharsets.UTF_8)));
        c.reference(d.get("configuration"),"baseline");String equipmentScope=text(d.get("equipmentScope"));
        var equipment=c.imports.get(equipmentScope);
        if(equipment==null||!equipment.get("artifactKind").equals("equipment"))throw new IllegalArgumentException("missing equipment import");
        var eq=map(equipment.get("values"));String equipmentConfiguration=text(map(eq.get("configuration")).get("scope"));
        String configurationScope=text(map(d.get("configuration")).get("scope"));
        if(Model.rows(equipment,"imports").stream().noneMatch(i->equipmentConfiguration.equals(i.get("scope")) && c.selections.get(configurationScope).get("sha256").equals(i.get("sha256"))))throw new IllegalArgumentException("equipment configuration differs");
        var findings=new ArrayList<Map<String,Object>>();
        if(!d.get("reviewedEquipmentSha256").equals(c.selections.get(equipmentScope).get("sha256")))findings.add(Json.object("code","equipment-review-stale","subject",d.get("id")));
        var equipmentContext=c.child();equipmentContext.select(equipment.get("imports"));
        for(var finding:new Equipment().analyze(eq,equipmentContext))findings.add(Json.object("code","equipment-"+finding.get("code"),"subject",finding.get("subject")));
        var evaluator=new Quantities.Evaluator(d);var parameterRatings=new TreeMap<String,String>();
        for(var p:Model.rows(d,"parameters")) {
            if(p.get("role").equals("fraction") && (!p.get("unit").equals("1")||Quantities.number(p.get("max")).compareTo(BigDecimal.ONE)>0))
                throw new IllegalArgumentException("fraction must be dimensionless within zero and one");
            if(!p.get("basis").equals("assumption")&&p.get("evidence")==null)throw new IllegalArgumentException("non-assumed quantity requires evidence");
            if(p.get("evidence")!=null)c.readPinned(map(p.get("evidence")));
            if(p.get("rating")!=null) {
                var r=map(p.get("rating"));var ref=map(r.get("instance"));
                if(!ref.get("scope").equals(equipmentScope))throw new IllegalArgumentException("rating outside selected equipment");
                var instance=c.reference(ref,"instance");var part=Model.find(Model.rows(eq,"parts"),text(instance.get("part")));
                var rating=Model.find(Model.rows(part,"ratings"),text(r.get("rating")));
                var value=Quantities.parameter(Json.object("unit",rating.get("unit"),"min",rating.get("value"),"max",rating.get("value")));
                var bound=evaluator.get(text(p.get("id")));bound.same(value);
                if(value.low().compareTo(bound.low())<0||value.high().compareTo(bound.high())>0)throw new IllegalArgumentException("selected rating outside parameter bounds");
                if(!p.get("basis").equals(rating.get("basis")))throw new IllegalArgumentException("rating evidence basis differs");
                parameterRatings.put(text(p.get("id")),ref.get("id")+":"+r.get("rating"));
            }
        }
        var loads=new TreeSet<String>();for(var instance:Model.rows(eq,"instances"))if(Model.find(Model.rows(eq,"parts"),text(instance.get("part"))).get("role").equals("load"))loads.add(text(instance.get("id")));
        Model.unique(Model.rows(d,"checks"));Model.unique(Model.rows(d,"reliability"));
        var checks=new ArrayList<Map<String,Object>>();
        for(var check:Model.rows(d,"checks")) {
            String actualId=text(check.get("actual")),limitId=text(check.get("limit"));var actual=evaluator.get(actualId);var limit=evaluator.get(limitId);actual.same(limit);
            boolean atMost=check.get("relation").equals("at-most");var margin=atMost?limit.add(actual,true):actual.add(limit,true);
            String state=margin.low().signum()>=0?"pass":margin.high().signum()<0?"fail":"indeterminate";
            if(check.get("kind").equals("peak-power")) {
                if(!atMost||!actual.dimension().equals(Quantities.unit("W").dimension()))throw new IllegalArgumentException("invalid peak-power check");
                var covered=new HashSet<String>();for(String leaf:evaluator.leaves.get(actualId))covered.add(parameterRatings.get(leaf));
                for(String instance:loads)if(!covered.contains(instance+":PEAK")){findings.add(Json.object("code","missing-peak-load","subject",instance));state="unavailable";}
            }
            String unit=declaredUnit(d,actualId);
            checks.add(Json.object("id",check.get("id"),"state",state,"actual",actual.output(unit),"limit",limit.output(unit),"margin",margin.output(unit)));
        }
        var reliability=new ArrayList<Map<String,Object>>();
        for(var analysis:Model.rows(d,"reliability")) {
            if(analysis.get("evidence")!=null)c.readPinned(map(analysis.get("evidence")));
            if(analysis.get("independence").equals("supported")&&analysis.get("evidence")==null)throw new IllegalArgumentException("independence support requires evidence");
            Set<String> members=new HashSet<>();var shared=map(analysis.get("sharedCause"));
            var availability=Quantities.availability(evaluator.get(text(shared.get("mtbf"))),evaluator.get(text(shared.get("repair"))));
            for(var member:Model.rows(analysis,"members")) {
                var ref=map(member.get("instance"));c.reference(ref,"instance");String instance=text(ref.get("id"));
                if(!ref.get("scope").equals(equipmentScope)||!loads.contains(instance)||!members.add(instance))throw new IllegalArgumentException("duplicate, shared-power or nonload reliability member");
                availability=availability.multiply(Quantities.availability(evaluator.get(text(member.get("mtbf"))),evaluator.get(text(member.get("repair")))),false);
            }
            if(!members.equals(loads))throw new IllegalArgumentException("reliability inventory incomplete");
            boolean unknown=analysis.get("independence").equals("unknown");
            reliability.add(Json.object("id",analysis.get("id"),"state",unknown?"unavailable":"conditional-estimate","independence",analysis.get("independence"),"availability",unknown?null:availability.output("1"),"sharedCause",shared.get("id")));
        }
        var values=new TreeMap<String,Object>();evaluator.values.forEach((id,value)->values.put(id,value.output(declaredUnit(d,id))));
        var bases=new TreeSet<String>();for(var p:Model.rows(d,"parameters"))bases.add(text(p.get("basis")));
        return Json.object("format","mundane-budget-analysis-0.1","evaluator",version(),"configuration",d.get("configuration"),"equipmentSha256",c.selections.get(equipmentScope).get("sha256"),"inputBases",bases.stream().toList(),"values",values,"checks",checks,"reliability",reliability,"findings",findings,"approval","none");
    }
    private static String declaredUnit(Map<String,Object> d,String id) {
        for(String field:List.of("parameters","formulas"))for(var row:Model.rows(d,field))if(row.get("id").equals(id))return text(row.get("unit"));
        throw new IllegalArgumentException("missing quantity");
    }
    public String view(Map<String,Object> a,Model.Context c) {
        var result=analyze(map(a.get("values")),c);var text=new StringBuilder("# Budget and availability inspection\n\n").append(Model.sourceLink(a,c,"/id",engineering.artifacts.Checks.text(map(a.get("values")).get("id")))).append("\n\nInput bases: ").append(Model.escape(result.get("inputBases"))).append("\n\n| Check | State | Margin |\n| --- | --- | --- |\n");
        for(var check:Model.rows(result,"checks"))text.append("| ").append(Model.escape(check.get("id"))).append(" | ").append(check.get("state")).append(" | ").append(Model.escape(Json.write(check.get("margin")))).append(" |\n");
        return text.append("\n```json\n").append(Json.write(result)).append("\n```\n\nConditional engineering estimates; no numerical safety or release approval.\n").toString();
    }
}
