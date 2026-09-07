package engineering.safety;
import static engineering.artifacts.Checks.*;
import engineering.artifacts.Json;
import engineering.domain.*;
import engineering.configuration.Configuration;
import engineering.architecture.Architecture;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.*;
import mundanereq.Versions;

/** Typed safety facts and explained gaps; no inferred risk acceptance. */
public final class Safety implements Model.Domain {
    public String kind(){return "safety";}public String format(){return Versions.SAFETY_ARTIFACT;}public String source(){return Versions.SAFETY_SOURCE;}public String version(){return Versions.SAFETY_VERSION;}public String contract(){return Versions.SAFETY_CONTRACT;}
    public static Model.Context context(Path root){return new Model.Context(root,Map.of("architecture",(a,c)->Model.validateSelected(a,new Architecture(),c),"configuration",(a,c)->Model.validateSelected(a,new Configuration(),c)));}
    private static final Map<String,String> GROUPS=Map.of("hazard","hazards","control","controls","cause","causes","assumption","assumptions","failure-mode","failureModes");
    private static Map<String,Object> local(Object raw,String kind,Map<String,Object> d) {
        var r=Model.ref(raw,kind);if(!r.get("scope").equals("self"))throw new IllegalArgumentException("local safety ownership required");return Model.find(list(d.get(GROUPS.get(kind))),text(r.get("id")));
    }
    private static void locals(Map<String,Object> row,String field,String kind,Map<String,Object> d){for(Object r:list(row.get(field)))local(r,kind,d);}
    public void validate(Map<String,Object> d,Model.Context c) {
        var authored=new TreeMap<>(d);authored.put("format",source());Schema.validate(authored,Json.read(SafetySchema.JSON.getBytes(StandardCharsets.UTF_8)));id(d.get("id"));
        for(String group:GROUPS.values())Model.unique(list(d.get(group)));Model.unique(list(d.get("severityScale")));
        var context=map(d.get("context"));c.reference(context.get("configuration"),"baseline");for(Object r:list(context.get("modes")))c.reference(r,"mode");
        var scale=new HashSet<String>();for(var s:Model.rows(d,"severityScale"))scale.add(text(s.get("id")));
        for(var h:Model.rows(d,"hazards")) {
            if(!scale.contains(h.get("severity")))throw new IllegalArgumentException("unknown-severity");locals(h,"causes","cause",d);locals(h,"controls","control",d);locals(h,"assumptions","assumption",d);
        }
        for(var control:Model.rows(d,"controls")) {
            locals(control,"hazards","hazard",d);for(Object r:list(control.get("requirements")))c.reference(r,"requirement");for(Object r:list(control.get("obligations")))c.reference(r,"activity");
        }
        for(var failure:Model.rows(d,"failureModes")) {
            c.reference(failure.get("component"),"component");local(failure.get("detection"),"control",d);locals(failure,"effects","hazard",d);locals(failure,"causes","cause",d);locals(failure,"mitigations","control",d);locals(failure,"assumptions","assumption",d);
        }
        var tree=map(d.get("faultTree"));validateTree(tree);for(var e:Model.rows(tree,"events"))if(e.get("cause")!=null)local(e.get("cause"),"cause",d);
        var scopes=new HashSet<String>();for(var reviewed:Model.rows(d,"reviewedAgainst"))if(!scopes.add(text(reviewed.get("scope")))||!c.selections.containsKey(reviewed.get("scope")))throw new IllegalArgumentException("unknown or duplicate review subject");
    }
    public static void validateTree(Map<String,Object> tree) {
        Model.unique(list(tree.get("events")));Map<String,Map<String,Object>> events=new TreeMap<>();for(var e:Model.rows(tree,"events"))events.put(text(e.get("id")),e);
        var visited=new HashSet<String>();visit(tree.get("top"),events,new HashSet<>(),visited,0);if(!visited.equals(events.keySet()))throw new IllegalArgumentException("unreachable-event");
    }
    private static void visit(Object raw,Map<String,Map<String,Object>> events,Set<String> visiting,Set<String> done,int depth) {
        var r=Model.ref(raw,"event");if(!r.get("scope").equals("self"))throw new IllegalArgumentException("nonlocal event");String ident=text(r.get("id"));var e=events.get(ident);if(e==null)throw new IllegalArgumentException("missing-event");
        if(depth>128||visiting.contains(ident))throw new IllegalArgumentException("fault-tree-cycle-or-depth");if(done.contains(ident))return;visiting.add(ident);
        var inputs=list(e.get("inputs"));if(e.get("operator").equals("basic")){if(!inputs.isEmpty()||e.get("cause")==null)throw new IllegalArgumentException("invalid-basic-event");}
        else {if(!Set.of("and","or").contains(e.get("operator"))||inputs.size()<2||e.get("cause")!=null||new HashSet<>(inputs).size()!=inputs.size())throw new IllegalArgumentException("invalid-gate");for(Object child:inputs)visit(child,events,visiting,done,depth+1);}
        visiting.remove(ident);done.add(ident);
    }
    public List<Map<String,Object>> analyze(Map<String,Object> d,Model.Context c) {
        List<Map<String,Object>> result=new ArrayList<>();
        for(var h:Model.rows(d,"hazards")) {
            if(list(h.get("controls")).isEmpty())result.add(finding(h.get("id"),"missing-control"));
            result.add(finding(h.get("id"),"residual-risk-"+map(h.get("residualRisk")).get("state")));
            for(Object raw:list(h.get("controls"))) {var control=local(raw,"control",d);if(list(control.get("hazards")).stream().map(engineering.artifacts.Checks::map).noneMatch(r->r.get("id").equals(h.get("id"))))result.add(finding(control.get("id"),"control-hazard-mismatch"));}
        }
        for(var control:Model.rows(d,"controls"))for(Object raw:list(control.get("hazards"))) {
            var hazard=local(raw,"hazard",d);if(list(hazard.get("controls")).stream().map(engineering.artifacts.Checks::map).noneMatch(r->r.get("id").equals(control.get("id"))))result.add(finding(control.get("id"),"orphan-control"));
        }
        for(var a:Model.rows(d,"assumptions"))if(a.get("state").equals("unverified"))result.add(finding(a.get("id"),"unverified-assumption"));
        for(var r:Model.rows(d,"reviewedAgainst"))if(!r.get("sha256").equals(c.selections.get(r.get("scope")).get("sha256")))result.add(finding(r.get("scope"),"review-stale"));return result;
    }
    private static Map<String,Object> finding(Object subject,String code){return Json.object("subject",subject,"code",code);}
    public Map<String,Object> query(Map<String,Object> d,String ident) {
        var h=Model.find(list(d.get("hazards")),ident);var controls=new ArrayList<Map<String,Object>>();for(Object r:list(h.get("controls")))controls.add(local(r,"control",d));
        return Json.object("hazard",h,"controls",controls,"acceptance","not-established");
    }
    public String view(Map<String,Object> a,Model.Context c) {
        var d=map(a.get("values"));StringBuilder out=new StringBuilder("# Safety inspection\n\n");
        for(String group:List.of("hazards","controls","failureModes","causes","assumptions")) {
            out.append("## ").append(group).append("\n\n| Record | Facts and references |\n| --- | --- |\n");int index=0;
            for(var row:Model.rows(d,group))out.append("| ").append(Model.sourceLink(a,c,"/"+group+"/"+index++,text(row.get("id")))).append(" | ").append(Model.escape(Json.write(row))).append(" |\n");out.append('\n');
        }
        out.append("## Findings\n\n").append(Model.escape(Json.write(analyze(d,c)))).append("\n\n```mermaid\nflowchart TD\n");
        var nodes=new TreeMap<String,String>();int nodeIndex=0;for(var e:Model.rows(map(d.get("faultTree")),"events"))nodes.put(text(e.get("id")),"n"+nodeIndex++);
        for(var e:Model.rows(map(d.get("faultTree")),"events")){String name=nodes.get(e.get("id"));out.append(name).append("[\"").append(e.get("id")).append(" ").append(e.get("operator")).append("\"]\n");for(Object raw:list(e.get("inputs")))out.append(name).append(" --> ").append(nodes.get(map(raw).get("id"))).append('\n');}
        return out.append("```\n\nQualitative structure and coverage do not establish accepted residual risk or authorize deployment.\n").toString();
    }
}
