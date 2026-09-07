package engineering.verification;

import static engineering.artifacts.Checks.*;
import engineering.artifacts.Artifacts;
import engineering.artifacts.Json;
import engineering.artifacts.Problem;
import engineering.artifacts.Snapshots;
import mundane.yaml.Yaml;
import java.nio.file.Path;
import java.util.*;
import mundanereq.Versions;

/** Explicit YAML plans, activities and coverage, independent of requirement source. */
public final class PlanCompiler {
    private PlanCompiler() {}
    public record Result(Map<String,Object> output,int status) {}
    public static Result compile(Path root,Path directory) {return compile(root,directory,()->{});}
    public static Result compile(Path root,Path directory,Runnable beforeRecheck) {
        Snapshots snapshots=new Snapshots(root);
        Map<String,Object> output=Json.object("artifactKind","verification-plan","format",Versions.PLAN_ARTIFACT,
                "sourceContract",Versions.PLAN_SOURCE,"compiler",Json.object("name","mundane-plan","version",Versions.PLAN_VERSION,"contract",Versions.PLAN_CONTRACT),
                "complete",false,"sources",List.of(),"plans",List.of(),"activities",List.of(),"coverage",List.of(),"diagnostics",List.of());
        List<Map<String,Object>> sources=new ArrayList<>();
        String current="plan.yaml";
        Map<String,Object> location=Json.object("path",current,"line",1,"column",1);
        try {
            current=snapshots.argument(directory.resolve("plan.yaml"));location.put("path",current);
            var snapshot=snapshots.read(current,8*1024*1024);
            sources.add(Json.object("path",current,"sha256",snapshot.sha256()));
            var document=Yaml.document(snapshot.bytes(),8*1024*1024);var authored=map(document.value());
            keys(authored,"format","plans","activities","coverage");
            if(!Versions.PLAN_SOURCE.equals(authored.get("format")))throw new Problem("unsupported-format","unsupported plan YAML source identifier",location);
            Map<String,Object> candidate=new TreeMap<>(output);
            Set<String> planIds=new HashSet<>(),activityIds=new HashSet<>(),tuples=new HashSet<>();
            for(String section:List.of("plans","activities","coverage")) {
                var rows=list(authored.get(section));
                location=point(current,document.values().get("/"+section).start());
                if(rows.size()>10000)throw new IllegalArgumentException("sequence exceeds 10000 records");
                List<Map<String,Object>> records=new ArrayList<>();int index=0;
                for(Object value:rows) {
                    location=point(current,document.values().get("/"+section+"/"+index++).start());
                    var row=map(value);
                    switch(section) {
                        case "plans" -> {
                            row.putIfAbsent("baselineScope",null);row.putIfAbsent("currentScope",null);
                            keys(row,"id","context","baselineScope","currentScope");
                            if(!planIds.add(id(row.get("id"))))throw new IllegalArgumentException("duplicate plan ID");
                            sourceText(row.get("context"));
                            for(String scope:List.of("baselineScope","currentScope"))if(row.get(scope)!=null)id(row.get(scope));
                        }
                        case "activities" -> {
                            keys(row,"id","method","objective","expectedEvidence");
                            if(!activityIds.add(id(row.get("id"))))throw new IllegalArgumentException("duplicate activity ID");
                            if(!Set.of("test","analysis","inspection","demonstration","review").contains(text(row.get("method"))))throw new IllegalArgumentException("unknown activity method");
                            sourceText(row.get("objective"));sourceText(row.get("expectedEvidence"));
                        }
                        case "coverage" -> {
                            keys(row,"planId","activityId","requirementId");
                            String p=id(row.get("planId")),a=id(row.get("activityId")),r=id(row.get("requirementId"));
                            if(!planIds.contains(p)||!activityIds.contains(a))throw new IllegalArgumentException("unknown plan or activity reference");
                            if(!tuples.add(p+":"+a+":"+r))throw new IllegalArgumentException("duplicate coverage assertion");
                        }
                        default -> throw new IllegalStateException(section);
                    }
                    row.put("location",location);records.add(row);
                }
                if(section.equals("coverage"))records.sort(Comparator.comparing((Map<String,Object> c)->text(c.get("planId"))).thenComparing(c->text(c.get("activityId"))).thenComparing(c->text(c.get("requirementId"))));
                else records.sort(Comparator.comparing(r->text(r.get("id"))));
                candidate.put(section,records);
            }
            candidate.put("complete",true);candidate.put("sources",sources);
            Artifacts.plan(candidate,current);beforeRecheck.run();snapshots.recheck();return new Result(candidate,0);
        } catch(Problem problem) {output.put("sources",sources);output.put("diagnostics",List.of(problem.diagnostic()));return new Result(output,problem.operational()?2:1);}
        catch(IllegalArgumentException error) {
            if(error instanceof Yaml.Failure failure)location=point(current,failure.point);
            output.put("sources",sources);output.put("diagnostics",List.of(new Problem("invalid-plan",error.getMessage(),location).diagnostic()));return new Result(output,1);
        }
    }
    private static Map<String,Object> point(String path,Yaml.Point point){return Json.object("path",path,"line",point.line(),"column",point.column());}
    private static void sourceText(Object value) {
        String cell=text(value);
        if(!cell.equals(cell.strip())||cell.codePoints().anyMatch(c->c<32||c>=127&&c<=159||c==0xfeff))throw new IllegalArgumentException("padded or control-containing text");
    }
}
