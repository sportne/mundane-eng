package engineering.work;

import static engineering.artifacts.Checks.*;
import static engineering.artifacts.Json.object;
import engineering.artifacts.*;
import java.util.*;
import java.util.function.Consumer;

/** Pure serialized analysis; no source readers or parsers. */
public final class WorkGraph {
    private WorkGraph() {}
    public record Evaluation(List<Object> edges,List<Object> findings) {}
    public static Evaluation evaluate(Map<String,Object> primary,List<?> imports,Consumer<String> resource) {
        var selected=new ArrayList<Object>();
        selected.add(object("scope","work","path","work.json","sha256","0".repeat(64),"artifact",primary));
        selected.addAll(imports);
        return evaluateImports(selected,resource);
    }
    /** Evaluate an explicit global scope selection, without reserving a primary scope. */
    public static Evaluation evaluateImports(List<?> imports,Consumer<String> resource) {
        Map<String,Map<String,Map<String,Object>>> work=new TreeMap<>();
        Map<String,Set<String>> targets=new TreeMap<>();Set<String> scopes=new HashSet<>();
        for(Object value:imports) {
            var entry=map(value);keys(entry,"scope","path","sha256","artifact");String scope=id(entry.get("scope"));path(entry.get("path"));digest(entry.get("sha256"));
            if(!scopes.add(scope))throw new Problem("duplicate-scope","duplicate/reserved scope "+scope,"imports");
            var a=map(entry.get("artifact"));String kind=text(a.get("artifactKind"));String file=text(entry.get("path"));
            if(kind.equals("work-items")) {var items=WorkArtifact.validate(a,file);work.put(scope,items);addWorkTargets(targets,scope,items);}
            else if(kind.equals("requirements"))targets.put(scope+":requirement",Artifacts.requirements(a,file).keySet());
            else if(kind.equals("verification-plan")) {
                Artifacts.plan(a,file);Set<String> plans=new TreeSet<>(),activities=new TreeSet<>();
                for(Object p:list(a.get("plans")))plans.add(id(map(p).get("id")));for(Object p:list(a.get("activities")))activities.add(id(map(p).get("id")));
                targets.put(scope+":verification-plan",plans);targets.put(scope+":verification-activity",activities);
            } else throw new Problem("wrong-kind","unsupported imported artifact kind",file);
        }
        Map<String,List<String>> dependencies=new TreeMap<>(),supersedes=new TreeMap<>();Map<String,Map<String,Object>> locations=new TreeMap<>();
        List<Object> edges=new ArrayList<>(),findings=new ArrayList<>();Set<String> retired=new TreeSet<>(),supersededTargets=new HashSet<>();
        int nodes=0,edgeCount=0;
        for(var scopeEntry:work.entrySet())for(var entry:scopeEntry.getValue().entrySet()) {
            if(++nodes>10000)throw new Problem("invalid-work-artifact","aggregate work-item limit exceeded","work");
            String scope=scopeEntry.getKey(),id=entry.getKey(),key=scope+":work-item:"+id;var record=entry.getValue();var v=map(record.get("values"));var loc=Artifacts.qualified(map(record.get("metadataLocation")),scope);
            locations.put(key,loc);dependencies.put(key,new ArrayList<>());supersedes.put(key,new ArrayList<>());
            if(v.get("status").equals("Superseded"))retired.add(key);
            List<String> unfinished=new ArrayList<>();
            for(Object depValue:list(v.get("dependencies"))) {
                String dep=text(depValue);var target=scopeEntry.getValue().get(dep);
                if(target==null||!"task".equals(map(target.get("values")).get("kind")))throw new Problem("missing-work-target","dependency must name a local task: "+dep,loc);
                String to=scope+":work-item:"+dep;dependencies.get(key).add(to);edges.add(object("from",key,"relation","depends-on","to",to,"location",loc));
                if(!"Complete".equals(map(target.get("values")).get("status")))unfinished.add(dep);
            }
            Collections.sort(unfinished);if(scope.equals("work"))findings.add(object("id",id,"status",v.get("status"),"unfinishedDependencies",unfinished));
            for(Object relation:list(v.get("relations"))) {
                var r=map(relation);String role=text(r.get("relation")),kind=text(r.get("kind")),target=text(r.get("target")),to;
                if(kind.equals("resource")) {resource.accept(target);to="resource:"+target;}
                else {
                    String targetScope=text(r.get("scope")),targetType=targetScope+":"+kind;
                    if(!targets.containsKey(targetType))throw new Problem("wrong-kind","unknown scope or incompatible target kind: "+targetType,loc);
                    if(!targets.get(targetType).contains(target))throw new Problem("missing-work-target","missing target "+targetType+":"+target,loc);
                    to=targetType+":"+target;
                }
                edges.add(object("from",key,"relation",role,"to",to,"location",loc));
                if(role.equals("supersedes")){supersedes.get(key).add(to);supersededTargets.add(to);}
            }
            edgeCount+=list(v.get("dependencies")).size()+list(v.get("relations")).size();
            if(edgeCount>100000)throw new Problem("invalid-work-artifact","aggregate relationship limit exceeded",loc);
        }
        acyclic(dependencies,"dependency-cycle",locations);acyclic(supersedes,"supersession-cycle",locations);
        for(String key:retired)if(!supersededTargets.contains(key))throw new Problem("missing-supersession","Superseded item lacks an incoming supersedes link: "+key,locations.get(key));
        edges.sort(Comparator.comparing(Json::write));findings.sort(Comparator.comparing(x->text(map(x).get("id"))));return new Evaluation(edges,findings);
    }
    private static void addWorkTargets(Map<String,Set<String>> targets,String scope,Map<String,Map<String,Object>> items){targets.put(scope+":work-item",items.keySet());}
    public static void acyclic(Map<String,List<String>> graph,String code,Map<String,Map<String,Object>> locations) {
        Map<String,Integer> remaining=new TreeMap<>();Map<String,List<String>> inverse=new TreeMap<>();
        for(String key:graph.keySet()){remaining.put(key,graph.get(key).size());inverse.put(key,new ArrayList<>());}
        for(var entry:graph.entrySet())for(String to:entry.getValue()) {
            if(!graph.containsKey(to))throw new Problem("missing-dependency","unknown build dependency "+to,locations.get(entry.getKey()));
            inverse.get(to).add(entry.getKey());
        }
        var ready=new TreeSet<String>();for(var entry:remaining.entrySet())if(entry.getValue()==0)ready.add(entry.getKey());int visited=0;
        while(!ready.isEmpty()){String key=ready.pollFirst();visited++;for(String from:inverse.get(key))if(remaining.compute(from,(k,n)->n-1)==0)ready.add(from);}
        if(visited!=graph.size()){String key=remaining.entrySet().stream().filter(e->e.getValue()>0).findFirst().orElseThrow().getKey();throw new Problem(code,"cyclic relationships include "+key,locations.get(key));}
    }
}
