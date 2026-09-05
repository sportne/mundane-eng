package engineering.work;

import static engineering.artifacts.Checks.*;
import static engineering.artifacts.Json.object;
import engineering.artifacts.*;
import java.nio.file.Path;
import java.util.*;
import mundanereq.Versions;

public final class WorkAnalyzer {
    private WorkAnalyzer() {}
    public static WorkResult analyze(Path root,String file,String manifest) {
        Snapshots reads=new Snapshots(root);List<Object> imports=new ArrayList<>(),diagnostics=new ArrayList<>();Map<String,Object> resources=new TreeMap<>();Object work=null,selection=null;int status=0;WorkGraph.Evaluation result=null;
        try {
            var input=reads.read(file);var a=map(Snapshots.json(input));WorkArtifact.validate(a,file);work=object("path",file,"sha256",input.sha256(),"artifact",a);
            var declaration=reads.read(manifest);selection=object("path",manifest,"sha256",declaration.sha256());
            var m=map(Snapshots.json(declaration));keys(m,"format","imports");if(!Versions.IMPORT_FORMAT.equals(m.get("format")))throw new Problem("unsupported-format","unsupported import declaration",manifest);
            var list=list(m.get("imports"));if(list.size()>100)throw new IllegalArgumentException("too many imports");
            Map<String,List<String>> graph=new TreeMap<>();Map<String,Map<String,Object>> locations=new TreeMap<>();
            for(Object value:list) {
                var entry=map(value);keys(entry,"scope","path","kind","sha256","dependsOn");String scope=id(entry.get("scope"));
                if(scope.equals("work")||graph.containsKey(scope))throw new Problem("duplicate-scope","duplicate/reserved import scope "+scope,manifest);
                var deps=new ArrayList<String>();for(Object d:list(entry.get("dependsOn")))if(!deps.add(id(d)))throw new IllegalArgumentException("duplicate build dependency");
                if(new HashSet<>(deps).size()!=deps.size())throw new IllegalArgumentException("duplicate build dependency");graph.put(scope,deps);locations.put(scope,object("path",manifest,"line",1,"column",1));
                String kind=text(entry.get("kind"));if(!Set.of("requirements","verification-plan","work-items").contains(kind))throw new Problem("wrong-kind","unsupported import kind",manifest);
                var snap=reads.read(path(entry.get("path")));if(entry.get("sha256")!=null&&!digest(entry.get("sha256")).equals(snap.sha256()))throw new Problem("digest-mismatch","import pin differs",manifest);
                var imported=map(Snapshots.json(snap));if(!kind.equals(imported.get("artifactKind")))throw new Problem("wrong-kind","declared import kind differs",manifest);
                imports.add(object("scope",scope,"path",snap.path(),"sha256",snap.sha256(),"artifact",imported));
            }
            WorkGraph.acyclic(graph,"build-cycle",locations);imports.sort(Comparator.comparing(x->text(map(x).get("scope"))));
            result=WorkGraph.evaluate(a,imports,path->{if(!resources.containsKey(path)){var snap=reads.read(path);resources.put(path,object("path",path,"sha256",snap.sha256()));}});
            reads.recheck();
        } catch(Problem p) {diagnostics.add(p.diagnostic());status=p.operational()?2:1;}
          catch(IllegalArgumentException e) {diagnostics.add(new Problem("invalid-import",e.getMessage(),manifest).diagnostic());status=1;}
        var output=object("format",Versions.WORK_ANALYSIS,"complete",status==0,"analyzer",object("name","mundane-work","version",Versions.WORK_VERSION,"contract",Versions.WORK_CONTRACT),
            "workArtifact",work,"selection",selection,"imports",imports,"resources",new ArrayList<>(resources.values()),"edges",status==0?result.edges():List.of(),"findings",status==0?result.findings():List.of(),"diagnostics",diagnostics);
        if(Json.bytes(output).length>16*1024*1024){status=Math.max(status,1);output.put("complete",false);output.put("workArtifact",null);output.put("imports",List.of());output.put("edges",List.of());output.put("findings",List.of());output.put("diagnostics",List.of(new Problem("work-output-limit","analysis exceeds 16 MiB",file).diagnostic()));}
        return new WorkResult(output,status);
    }
}
