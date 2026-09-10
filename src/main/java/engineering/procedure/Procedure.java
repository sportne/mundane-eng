package engineering.procedure;
import static engineering.artifacts.Checks.*;
import engineering.artifacts.Json;
import engineering.domain.*;
import engineering.architecture.Architecture;
import engineering.configuration.Configuration;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.*;
import mundanereq.Versions;

public final class Procedure implements Model.Domain {
    public java.util.Map<String,String> changeGroups(){return java.util.Map.ofEntries(java.util.Map.entry("id","identity"),java.util.Map.entry("objective","procedure-and-criteria"),java.util.Map.entry("method","procedure-and-criteria"),java.util.Map.entry("activity","procedure-and-criteria"),java.util.Map.entry("requirements","procedure-and-criteria"),java.util.Map.entry("configuration","selected-revision"),java.util.Map.entry("telemetryInterface","procedure-and-criteria"),java.util.Map.entry("commandInterface","procedure-and-criteria"),java.util.Map.entry("environment","procedure-and-criteria"),java.util.Map.entry("clock","procedure-and-criteria"),java.util.Map.entry("session","procedure-and-criteria"),java.util.Map.entry("events","procedure-and-criteria"),java.util.Map.entry("expected","procedure-and-criteria"),java.util.Map.entry("heartbeatInterface","procedure-and-criteria"));}
    public Object schema(){return engineering.artifacts.Json.read(ProcedureSchema.JSON.getBytes(java.nio.charset.StandardCharsets.UTF_8));}

    public String kind(){return "procedure";}public String format(){return Versions.PROCEDURE_ARTIFACT;}public String source(){return Versions.PROCEDURE_SOURCE;}public String version(){return Versions.PROCEDURE_VERSION;}public String contract(){return Versions.PROCEDURE_CONTRACT;}
    public static Model.Context context(Path root){return new Model.Context(root,Map.of("architecture",new Architecture(),"configuration",new Configuration(),"procedure",new Procedure()));}
    public Map<String,Object> lookup(Map<String,Object> values,String kind,String ident) {if(!kind.equals("procedure")||!values.get("id").equals(ident))throw new IllegalArgumentException("missing-target or wrong-kind");return values;}
    public void validate(Map<String,Object> d,Model.Context c) {
        var authored=new TreeMap<>(d);authored.put("format",source());Schema.validate(authored,Json.read(ProcedureSchema.JSON.getBytes(StandardCharsets.UTF_8)));
        c.reference(d.get("activity"),"activity");var baseline=c.reference(d.get("configuration"),"baseline");
        var config=c.imports.get(map(d.get("configuration")).get("scope"));if(!map(map(config.get("values")).get("configuration")).get("environment").equals(d.get("environment")))throw new IllegalArgumentException("procedure applicability mismatch");
        for(Object r:list(d.get("requirements"))){c.reference(r,"requirement");selected(baseline,map(r),"requirements",c);}
        for(String field:List.of("telemetryInterface","commandInterface","heartbeatInterface")) {
            var r=map(d.get(field));var target=c.reference(r,"interface");if(!target.get("kind").equals(field.equals("telemetryInterface")?"telemetry":field.equals("heartbeatInterface")?"heartbeat":"command"))throw new IllegalArgumentException("wrong interface function");selected(baseline,r,"architecture",c);
        }
        long prior=-1;for(var event:Model.rows(d,"events")){long at=((Number)event.get("atMs")).longValue();if(at<prior)throw new IllegalArgumentException("unordered procedure events");prior=at;}
        if(d.get("method").equals("manual-inspection")&&!list(d.get("events")).isEmpty()||d.get("method").equals("simulation")&&list(d.get("events")).isEmpty())throw new IllegalArgumentException("method/event mismatch");
        Model.unique(list(d.get("expected")));for(var criterion:Model.rows(d,"expected"))fieldValue(text(criterion.get("field")),criterion.get("equals"));
    }
    private static void selected(Map<String,Object> baseline,Map<String,Object> ref,String kind,Model.Context c) {
        var entry=c.selections.get(ref.get("scope"));if(Model.rows(baseline,"members").stream().noneMatch(m->m.get("kind").equals(kind)&&m.get("sha256").equals(entry.get("sha256"))))throw new IllegalArgumentException("subject is not selected by configuration");
    }
    public static void fieldValue(String field,Object value) {
        if(field.equals("queueDepth")){if(!(value instanceof Number n)||Schema.number(n)<0)throw new IllegalArgumentException("invalid queue depth");}
        else if(field.equals("combinedFault")){if(!(value instanceof Boolean))throw new IllegalArgumentException("invalid combined fault flag");}
        else {
            var allowed=switch(field) {
                case "state"->Set.of("fresh","stale","unknown");case "commandState"->Set.of("idle","pending","acknowledged","inhibited","timed-out","quarantined");
                case "powerState"->Set.of("mains","backup-assumed","synthetic-wiring-observed");case "linkState"->Set.of("up","lost","unknown");case "onboardResponse"->Set.of("unspecified-aircraft-local");default->throw new IllegalArgumentException("unknown observed field");};
            if(!allowed.contains(value))throw new IllegalArgumentException("unsupported field value");
        }
    }
    public String view(Map<String,Object> a,Model.Context c) {
        var d=map(a.get("values"));return "# Procedure\n\n"+Model.sourceLink(a,c,"/objective",text(d.get("objective")))+"\n\n"+Model.escape(Json.write(d))+"\n\nSimulation/manual fixture only; no aircraft execution.\n";
    }
}
