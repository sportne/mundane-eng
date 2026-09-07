package engineering.configuration;
import static engineering.artifacts.Checks.*;
import engineering.artifacts.*;
import engineering.domain.*;
import engineering.architecture.Architecture;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import mundanereq.Versions;

/** Configuration identity, applicability and resource checks; no source readers. */
public final class Configuration implements Model.Domain {
    public String kind(){return "configuration";} public String format(){return Versions.CONFIGURATION_ARTIFACT;}
    public String source(){return Versions.CONFIGURATION_SOURCE;} public String version(){return Versions.CONFIGURATION_VERSION;} public String contract(){return Versions.CONFIGURATION_CONTRACT;}
    public static Model.Context context(Path root) {return new Model.Context(root,Map.of("architecture",(a,c)->Model.validateSelected(a,new Architecture(),c),"configuration",(a,c)->Model.validateSelected(a,new Configuration(),c)));}
    private static final Set<String> NATIVE=Set.of("gcs-equipment-assumption-0.1","python-source","version-declarations-json","mundanereq-yaml-0.4","mundanereq-attributes-yaml-0.1","mundane-architecture-yaml-0.1","text/plain");
    public void validate(Map<String,Object> data,Model.Context c) {resolve(data,c);}
    public List<Map<String,Object>> resolve(Map<String,Object> data,Model.Context c) {
        var authored=new TreeMap<>(data);authored.put("format",source());Schema.validate(authored,Json.read(ConfigurationSchema.JSON.getBytes(StandardCharsets.UTF_8)));
        var config=map(data.get("configuration"));var baseline=map(data.get("baseline"));id(config.get("id"));id(baseline.get("id"));
        var members=new TreeMap<String,Map<String,Object>>();var artifacts=new TreeMap<String,Map<String,Object>>();var findings=new ArrayList<Map<String,Object>>();
        for(var member:Model.rows(baseline,"members")) {
            String scope=id(member.get("scope"));if(scope.equals("self")||members.putIfAbsent(scope,member)!=null)throw new IllegalArgumentException("ambiguous-member");
            if(!list(member.get("appliesTo")).contains(config.get("environment")))throw new IllegalArgumentException("inapplicable-member");
            String kind=text(member.get("kind")),format=text(member.get("format")),file=path(member.get("path"));digest(member.get("sha256"));
            boolean known=switch(kind){case "requirements"->Set.of(Versions.REQUIREMENT_ARTIFACT,Versions.REQUIREMENT_ATTRIBUTE_ARTIFACT).contains(format);case "architecture"->Versions.ARCHITECTURE_ARTIFACT.equals(format);case "native-resource"->NATIVE.contains(format);default->false;};
            if(!known)throw new IllegalArgumentException("unsupported-format");
            // Only an absent path can be optional; escapes, unreadable files and mismatches are errors.
            if(!Files.exists(c.root.resolve(file),LinkOption.NOFOLLOW_LINKS)&&Boolean.FALSE.equals(member.get("required"))) {findings.add(Json.object("scope",scope,"state","optional-resource-unavailable"));continue;}
            var snapshot=c.readPinned(member);findings.add(Json.object("scope",scope,"state","available"));
            if(!kind.equals("native-resource")) {
                var a=map(Snapshots.json(snapshot));if(!format.equals(a.get("format")))throw new IllegalArgumentException("invalid-artifact");
                if(kind.equals("requirements"))Artifacts.requirements(a,file);else Model.validateSelected(a,new Architecture(),c);
                artifacts.put(scope,a);
            }
        }
        var slots=new HashSet<String>();for(var selection:Model.rows(config,"selections")) {
            if(!slots.add(id(selection.get("slot"))))throw new IllegalArgumentException("contradictory-selection");
            if(!members.containsKey(selection.get("memberScope")))throw new IllegalArgumentException("missing-member");
        }
        var mappings=new HashMap<String,Map<String,Object>>();for(var m:Model.rows(baseline,"sourceMappings")) {
            String scope=id(m.get("artifactScope")),source=path(m.get("sourcePath"));
            if((!members.containsKey(scope)||members.get(scope).get("kind").equals("native-resource"))||!members.containsKey(m.get("memberScope"))||mappings.putIfAbsent(scope+":"+source,m)!=null)throw new IllegalArgumentException("invalid-source-mapping");
        }
        var consumed=new HashSet<String>();for(var entry:artifacts.entrySet()) {
            var a=entry.getValue();var origins=new ArrayList<Object>(list(a.get("sources")));if(a.get("attributeSchema")!=null)origins.add(map(a.get("attributeSchema")).get("source"));
            for(Object origin:origins) {
                var o=map(origin);String key=entry.getKey()+":"+path(o.get("path"));var m=mappings.get(key);if(m==null)throw new IllegalArgumentException("missing-source-mapping");consumed.add(key);
                var selected=members.get(m.get("memberScope"));if(!selected.get("sha256").equals(o.get("sha256")))throw new IllegalArgumentException("source-revision-mismatch");
            }
        }
        for(String key:mappings.keySet())if(!artifacts.containsKey(key.substring(0,key.indexOf(':'))))consumed.add(key);
        if(!consumed.equals(mappings.keySet()))throw new IllegalArgumentException("unused-source-mapping");
        String stage=text(config.get("stage"));if(!Map.of("designed","design-intent","built","synthetic-assembly","deployed","synthetic-deployment").get(stage).equals(config.get("observationBasis")))throw new IllegalArgumentException("stage-basis-mismatch");
        if(!stage.equals("designed")&&data.get("previous")==null)throw new IllegalArgumentException("missing-prior-baseline");
        if(data.get("previous")!=null) {
            var old=prior(map(data.get("previous")),c);
            if(!stage.equals("designed")&&!Map.of("built","designed","deployed","built").get(stage).equals(map(old.get("configuration")).get("stage")))throw new IllegalArgumentException("invalid-stage-chain");
            if(!config.get("environment").equals(map(old.get("configuration")).get("environment")))throw new IllegalArgumentException("prior-applicability-mismatch");
        }
        if(data.get("change")!=null)prior(map(map(data.get("change")).get("from")),c);
        return findings;
    }
    private Map<String,Object> prior(Map<String,Object> ref,Model.Context c) {
        var r=Model.ref(ref.get("ref"),"baseline");if(!r.get("scope").equals("previous"))throw new IllegalArgumentException("invalid-prior-scope");
        var a=map(Snapshots.json(c.readPinned(ref)));Model.validateSelected(a,this,c);var v=map(a.get("values"));if(!map(v.get("baseline")).get("id").equals(r.get("id")))throw new IllegalArgumentException("invalid-prior-reference");return v;
    }
    public static List<Map<String,Object>> compare(Map<String,Object> a,Map<String,Object> b) {
        var left=selections(a);var right=selections(b);var result=new ArrayList<Map<String,Object>>();var slots=new TreeSet<>(left.keySet());slots.addAll(right.keySet());
        for(String slot:slots) {
            var l=left.get(slot);var r=right.get(slot);String change=null;
            if(l==null||r==null)change="selection-added-or-removed";
            else if(!projection(l).equals(projection(r)))change="selected-revision-or-contract-changed";
            else if(!l.get("path").equals(r.get("path")))change="locator-only-change";
            if(change!=null)result.add(Json.object("slot",slot,"change",change));
        }
        for(String field:List.of("stage","environment","observationBasis"))if(!map(a.get("configuration")).get(field).equals(map(b.get("configuration")).get(field)))result.add(Json.object("slot","configuration-"+field,"change","changed"));
        return result;
    }
    private static Map<String,Object> projection(Map<String,Object> member){var r=new TreeMap<>(member);r.remove("path");r.put("appliesTo",new TreeSet<>(list(member.get("appliesTo")).stream().map(engineering.artifacts.Checks::text).toList()));return r;}
    private static Map<String,Map<String,Object>> selections(Map<String,Object> d) {
        var members=Model.rows(map(d.get("baseline")),"members");var result=new TreeMap<String,Map<String,Object>>();
        for(var s:Model.rows(map(d.get("configuration")),"selections"))result.put(text(s.get("slot")),members.stream().filter(m->m.get("scope").equals(s.get("memberScope"))).findFirst().orElseThrow());return result;
    }
    public String view(Map<String,Object> a,Model.Context c) {
        var d=map(a.get("values"));var findings=resolve(d,c);StringBuilder s=new StringBuilder("# Configuration inspection\n\n");
        s.append(Model.sourceLink(a,c,"/configuration",text(map(d.get("configuration")).get("id")))).append("\n\n").append(Model.escape(Json.write(d.get("configuration")))).append("\n\n| Scope | Availability | Selected revision |\n| --- | --- | --- |\n");
        for(var f:findings){var member=Model.rows(map(d.get("baseline")),"members").stream().filter(m->m.get("scope").equals(f.get("scope"))).findFirst().orElseThrow();s.append("| ").append(Model.escape(f.get("scope"))).append(" | ").append(f.get("state")).append(" | ").append(member.get("sha256")).append(" |\n");}
        return s.append("\nAvailability does not establish evidence adequacy, actual installation or release authorization.\n").toString();
    }
}
