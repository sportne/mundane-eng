package engineering.evidence;
import static engineering.artifacts.Checks.*;
import engineering.artifacts.*;
import engineering.domain.*;
import engineering.procedure.Procedure;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.*;
import mundanereq.Versions;

/** Recompute evidence interpretation from selected raw bytes, never stored verdicts. */
public final class Evidence implements Model.Domain {
    public String kind(){return "evidence";}public String format(){return Versions.EVIDENCE_ARTIFACT;}public String source(){return Versions.RUN_ARTIFACT;}public String version(){return Versions.EVIDENCE_VERSION;}public String contract(){return Versions.EVIDENCE_CONTRACT;}
    public static List<Map<String,Object>> subjects(Map<String,Object> procedure){return list(procedure.get("imports")).stream().map(engineering.artifacts.Checks::map).map(e->Json.object("scope",e.get("scope"),"sha256",e.get("sha256"))).sorted(Comparator.comparing(e->text(e.get("scope")))).toList();}
    public static Map<String,Object> selectedProcedure(Model.Context c){if(c.imports.size()!=1||!c.imports.containsKey("procedure"))throw new IllegalArgumentException("evidence requires exactly the procedure import");return c.imports.get("procedure");}
    public void validate(Map<String,Object> d,Model.Context c) {
        keys(d,"run","resource");var run=map(d.get("run"));var resource=map(d.get("resource"));keys(resource,"path","sha256");
        Schema.validate(run,Json.read(RunSchema.JSON.getBytes(StandardCharsets.UTF_8)));
        if(!Json.write(Snapshots.json(c.readPinned(resource))).equals(Json.write(run)))throw new IllegalArgumentException("raw evidence differs from compiled values");
        var procedure=selectedProcedure(c);var p=map(procedure.get("values"));var selection=c.selections.get("procedure");
        var subject=map(run.get("procedure"));if(!subject.get("id").equals(p.get("id"))||!subject.get("sha256").equals(selection.get("sha256")))throw new IllegalArgumentException("wrong-procedure-revision");
        var subjects=list(run.get("subjects")).stream().map(engineering.artifacts.Checks::map).sorted(Comparator.comparing(e->text(e.get("scope")))).toList();
        if(!subjects.equals(subjects(procedure)))throw new IllegalArgumentException("wrong-subject-revisions");
        if(!run.get("environment").equals(p.get("environment")))throw new IllegalArgumentException("wrong-environment");
        var adapter=map(run.get("adapter"));if(!adapter.get("buildSha256").equals(EvidenceBuild.SHA256))throw new IllegalArgumentException("unknown-adapter-build");
        var runtime=map(run.get("runtime"));if(!adapter.get("runtimeSha256").equals(runtime.get("sha256")))throw new IllegalArgumentException("wrong-runtime-pin");c.readPinned(runtime,64*1024*1024);
        boolean manual=p.get("method").equals("manual-inspection");
        if(!adapter.get("name").equals(manual?"synthetic-manual-inspection":"gcs-event-simulator")||manual!=adapter.get("faultModel").equals("manual"))throw new IllegalArgumentException("wrong-adapter-method");
        if(manual){if(run.get("manualSource")==null)throw new IllegalArgumentException("missing-manual-source");c.readPinned(map(run.get("manualSource")));}
        else if(run.get("manualSource")!=null)throw new IllegalArgumentException("unexpected-manual-source");
        long before=-1;for(var o:Model.rows(run,"observations")) {long at=((Number)o.get("atMs")).longValue();if(at<before)throw new IllegalArgumentException("unordered observations");before=at;Procedure.fieldValue(text(o.get("field")),o.get("value"));}
    }
    public static Map<String,Object> evaluate(Map<String,Object> p,Map<String,Object> run) {
        var outcomes=new ArrayList<Map<String,Object>>();String status=text(run.get("execution"));
        if(!status.equals("completed"))return Json.object("state",status,"criteria",outcomes);
        if(!Json.write(run.get("clock")).equals(Json.write(p.get("clock"))))return Json.object("state","inconclusive","criteria",outcomes);
        double uncertainty=Schema.number(map(run.get("clock")).get("uncertaintyMs"));status="pass";
        for(var criterion:Model.rows(p,"expected")) {
            double after=Schema.number(criterion.get("afterMs")),end=after+Schema.number(criterion.get("withinMs"));
            var samples=Model.rows(run,"observations").stream().filter(o->o.get("field").equals(criterion.get("field"))&&Schema.number(o.get("atMs"))>=after&&Schema.number(o.get("atMs"))+uncertainty<=end).toList();
            String state=samples.isEmpty()?"inconclusive":samples.stream().anyMatch(o->equal(o.get("value"),criterion.get("equals")))?"pass":"fail";
            if(state.equals("fail"))status="fail";else if(state.equals("inconclusive")&&!status.equals("fail"))status="inconclusive";
            outcomes.add(Json.object("criterion",criterion.get("id"),"state",state,"observations",samples));
        }
        return Json.object("state",status,"criteria",outcomes);
    }
    private static boolean equal(Object a,Object b){if(a instanceof Number x&&b instanceof Number y)return new BigDecimal(x.toString()).compareTo(new BigDecimal(y.toString()))==0;return Objects.equals(a,b);}
    public static Map<String,Object> read(Path file,Model.Context c) {var a=Model.read(file,c,new Evidence());if(!map(list(a.get("sources")).get(0)).equals(map(map(a.get("values")).get("resource"))))throw new IllegalArgumentException("evidence source inventory mismatch");return a;}
    public String view(Map<String,Object> a,Model.Context c) {
        var d=map(a.get("values"));var run=map(d.get("run"));return "# Evidence inspection\n\n"+Model.sourceLink(a,c,"",text(run.get("id")))+"\n\n"+Model.escape(Json.write(evaluate(map(selectedProcedure(c).get("values")),run)))+"\n\n"+Model.escape(Json.write(run))+"\n\nSimulated or synthetic observations; adequacy and authorization are not established.\n";
    }
}
