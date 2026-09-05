package engineering.artifacts;

import static engineering.artifacts.Checks.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import mundanereq.Versions;

/** The first bounded resolver, using serialized artifacts rather than source parsers. */
public final class Linker {
    private Linker() {}
    public record Result(Map<String,Object> output,int status) {}
    public static Result link(Path root,String manifest,String planFile,String context) { return link(root,manifest,planFile,context,()->{}); }
    public static Result link(Path root,String manifest,String planFile,String context,Runnable beforeRecheck) {
        Snapshots snapshots=new Snapshots(root);
        Map<String,Object> output=Json.object("format",Versions.LINK_ARTIFACT,"complete",false,"context",context,
                "linker",Json.object("name","mundane-link","version",Versions.LINK_VERSION,"contract",Versions.LINK_CONTRACT),
                "plans",List.of(),"imports",List.of(),"planArtifact",null,"edges",List.of(),"inverse",Map.of(),"diagnostics",List.of());
        String current=manifest;
        try {
            var declaration=map(Snapshots.json(snapshots.read(manifest)));keys(declaration,"format","imports");
            if(!Versions.IMPORT_FORMAT.equals(declaration.get("format"))) throw new Problem("unsupported-format","unsupported import declaration",manifest);
            List<?> declarations=list(declaration.get("imports"));
            if(declarations.isEmpty()||declarations.size()>100) throw new Problem("invalid-import","expected 1..100 imports",manifest);
            Map<String,Map<String,Object>> imports=new TreeMap<>();Map<String,List<String>> dependencies=new TreeMap<>();
            Map<String,Map<String,Map<String,Object>>> records=new TreeMap<>();
            for(Object value:declarations) {
                current=manifest;var d=map(value);keys(d,"scope","path","kind","sha256","dependsOn");String scope=id(d.get("scope"));
                if(imports.containsKey(scope)) throw new Problem("duplicate-scope","duplicate scope "+scope,manifest);
                if(!"requirements".equals(d.get("kind"))) throw new Problem("wrong-kind","only requirement imports are supported",manifest);
                List<String> deps=list(d.get("dependsOn")).stream().map(Checks::id).toList();
                if(new HashSet<>(deps).size()!=deps.size()) throw new Problem("invalid-import","duplicate dependency",manifest);dependencies.put(scope,deps);
                String pin=d.get("sha256")==null?null:digest(d.get("sha256"));String file=path(d.get("path"));current=file;
                var snapshot=snapshots.read(file);
                if(pin!=null&&!pin.equals(snapshot.sha256())) throw new Problem("digest-mismatch","artifact does not match pin for "+scope,manifest);
                var artifact=map(Snapshots.json(snapshot));
                if(Versions.REQUIREMENT_ATTRIBUTE_ARTIFACT.equals(artifact.get("format"))) {
                    output.put("format",Versions.LINK_ATTRIBUTE_ARTIFACT);
                    output.put("linker",Json.object("name","mundane-link","version",Versions.LINK_VERSION,"contract",Versions.LINK_ATTRIBUTE_CONTRACT));
                }
                records.put(scope,Artifacts.requirements(artifact,file));
                imports.put(scope,Json.object("scope",scope,"path",file,"sha256",snapshot.sha256(),"artifact",artifact));
            }
            current=manifest;
            for(String scope:dependencies.keySet()) cycle(scope,dependencies,new HashSet<>(),new HashSet<>(),manifest);
            output.put("imports",new ArrayList<>(imports.values()));
            current=planFile;var snapshot=snapshots.read(planFile);var plan=map(Snapshots.json(snapshot));Artifacts.plan(plan,planFile);
            output.put("planArtifact",Json.object("path",planFile,"sha256",snapshot.sha256(),"artifact",plan));
            Map<String,Map<String,Object>> selected=new TreeMap<>();
            for(Object value:list(plan.get("plans"))) { var p=map(value);if(context==null||context.equals(p.get("context"))) selected.put(text(p.get("id")),p); }
            if(selected.isEmpty()) throw new Problem("unknown-context","no plan has selected context",planFile);
            output.put("plans",new ArrayList<>(selected.keySet()));
            Map<String,String> baseline=new TreeMap<>(),currentScopes=new TreeMap<>();
            for(var entry:selected.entrySet()) {
                var location=Artifacts.qualified(map(entry.getValue().get("location")),"plan");
                baseline.put(entry.getKey(),scope(entry.getValue().get("baselineScope"),imports.keySet(),location));
                currentScopes.put(entry.getKey(),scope(entry.getValue().get("currentScope"),imports.keySet(),location));
            }
            List<Map<String,Object>> edges=new ArrayList<>();
            for(Object value:list(plan.get("coverage"))) {
                var row=map(value);String planId=text(row.get("planId"));if(!selected.containsKey(planId)) continue;
                String target=text(row.get("requirementId"));var loc=Artifacts.qualified(map(row.get("location")),"plan");
                for(String scope:List.of(baseline.get(planId),currentScopes.get(planId))) if(!records.get(scope).containsKey(target)) throw new Problem("missing-target","missing "+scope+":"+target,loc);
                edges.add(Json.object("planId",planId,"activityId",row.get("activityId"),"requirementId",target,
                        "context",selected.get(planId).get("context"),"baselineScope",baseline.get(planId),"currentScope",currentScopes.get(planId),"location",loc));
            }
            edges.sort(Comparator.comparing((Map<String,Object> e)->text(e.get("planId"))).thenComparing(e->text(e.get("activityId"))).thenComparing(e->text(e.get("requirementId"))));
            Map<String,Object> inverse=new TreeMap<>();Map<String,List<Integer>> indexes=new TreeMap<>();
            for(int i=0;i<edges.size();i++) {var edge=edges.get(i);String key=edge.get("currentScope")+":"+edge.get("requirementId");indexes.computeIfAbsent(key,ignored->new ArrayList<>()).add(i);}
            inverse.putAll(indexes);beforeRecheck.run();snapshots.recheck();
            output.put("edges",edges);output.put("inverse",inverse);output.put("complete",true);return new Result(output,0);
        } catch(Problem problem) { output.put("diagnostics",List.of(problem.diagnostic()));return new Result(output,problem.operational()?2:1); }
        catch(IllegalArgumentException error) { output.put("diagnostics",List.of(new Problem(current.equals(manifest)?"invalid-import":"invalid-artifact",error.getMessage(),current).diagnostic()));return new Result(output,1); }
    }
    private static String scope(Object selected,Set<String> scopes,Map<String,Object> location) {
        if(selected==null) {if(scopes.size()!=1) throw new Problem("ambiguous-scope","unqualified scope has multiple imports",location);return scopes.iterator().next();}
        String value=id(selected);if(!scopes.contains(value)) throw new Problem("missing-scope","unknown scope "+value,location);return value;
    }
    private static void cycle(String scope,Map<String,List<String>> deps,Set<String> active,Set<String> done,String manifest) {
        if(!deps.containsKey(scope)) throw new Problem("missing-dependency","missing dependency "+scope,manifest);
        if(active.contains(scope)) throw new Problem("build-cycle","cyclic import build dependency at "+scope,manifest);
        if(done.contains(scope)) return;active.add(scope);
        for(String dep:deps.get(scope)) cycle(dep,deps,active,done,manifest);
        active.remove(scope);done.add(scope);
    }
}
