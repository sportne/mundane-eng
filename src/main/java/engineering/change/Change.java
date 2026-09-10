package engineering.change;
import static engineering.artifacts.Checks.*;
import engineering.artifacts.*;
import engineering.domain.*;
import java.util.*;
import mundanereq.Versions;

/** Meaning comparison, exact selected-pin staleness and prospective paths are separate results. */
public final class Change implements Model.Domain {
    public String kind(){return "change";}public String source(){return Versions.CHANGE_SOURCE;}public String format(){return Versions.CHANGE_ARTIFACT;}public String version(){return Versions.CHANGE_VERSION;}public String contract(){return Versions.CHANGE_CONTRACT;}
    public Object schema(){return Json.read(ChangeSchema.JSON.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    public record Inventory(Map<String,Map<String,Object>> entries,Map<String,Map<String,Object>> artifacts) {}
    public static Inventory inventory(Object entries,Model.Context c) {
        var selected=new TreeMap<String,Map<String,Object>>();var artifacts=new TreeMap<String,Map<String,Object>>();var paths=new HashSet<String>();
        if(list(entries).isEmpty()||list(entries).size()>100)throw new IllegalArgumentException("select 1..100 artifacts");
        for(Object value:list(entries)) {
            var e=map(value);keys(e,"scope","kind","format","path","sha256");String scope=id(e.get("scope"));
            if(scope.equals("self")||selected.putIfAbsent(scope,e)!=null||!paths.add(path(e.get("path"))))throw new IllegalArgumentException("duplicate scope or ambiguous selected path");
            var a=map(Snapshots.json(c.readPinned(e)));if(!e.get("kind").equals(a.get("artifactKind"))||!e.get("format").equals(a.get("format")))throw new IllegalArgumentException("kind/format mismatch");
            Owners.validate(a,c,text(e.get("path")));artifacts.put(scope,a);
        }
        return new Inventory(selected,artifacts);
    }
    public void validate(Map<String,Object> v,Model.Context c) {
        var authored=new TreeMap<>(v);authored.put("format",source());Schema.validate(authored,schema());
        if(!c.imports.isEmpty())throw new IllegalArgumentException("change selections belong to before/after");
        var before=inventory(v.get("before"),c);var after=inventory(v.get("after"),c);
        for(String scope:before.entries.keySet())if(after.entries.containsKey(scope)&&!before.entries.get(scope).get("kind").equals(after.entries.get(scope).get("kind")))throw new IllegalArgumentException("logical scope changed artifact kind");
    }
    public static List<Map<String,Object>> differences(Object before,Object after,String at,Map<String,String> groups) {
        var result=new ArrayList<Map<String,Object>>();diff(before,after,at,groups,result,0);return result;
    }
    private static void diff(Object a,Object b,String at,Map<String,String> groups,List<Map<String,Object>> out,int depth) {
        if(a instanceof Number&&b instanceof Number&&new java.math.BigDecimal(a.toString()).compareTo(new java.math.BigDecimal(b.toString()))==0)return;
        if(Json.write(a).equals(Json.write(b)))return;
        if(depth>40||out.size()>=10000)throw new IllegalArgumentException("change comparison limit");
        if(a instanceof Map<?,?>&&b instanceof Map<?,?>) {
            var left=map(a);var right=map(b);Set<String> keys=new TreeSet<>(left.keySet());keys.addAll(right.keySet());
            for(String key:keys){
                String pointer=mundane.json.Json.pointer(at,key);
                if(!left.containsKey(key)||!right.containsKey(key)){
                    if(out.size()>=10000)throw new IllegalArgumentException("change comparison limit");
                    var change=difference(left.get(key),right.get(key),pointer,groups);
                    change.put("beforePresent",left.containsKey(key));change.put("afterPresent",right.containsKey(key));out.add(change);
                }else diff(left.get(key),right.get(key),pointer,groups,out,depth+1);
            }
        } else if(a instanceof List<?> left&&b instanceof List<?> right&&left.size()==right.size()) {
            for(int i=0;i<left.size();i++)diff(left.get(i),right.get(i),at+"/"+i,groups,out,depth+1);
        } else {
            out.add(difference(a,b,at,groups));
        }
    }
    private static Map<String,Object> difference(Object a,Object b,String at,Map<String,String> groups){
        String field=at.split("/",-1).length>1?at.split("/",-1)[1].replace("~1","/").replace("~0","~"):"";
        return Json.object("pointer",at,"classification",groups.getOrDefault(field,"unknown"),"before",a,"after",b);
    }
    public Map<String,Object> analyze(Map<String,Object> v,Model.Context c) {
        var before=inventory(v.get("before"),c);var after=inventory(v.get("after"),c);Set<String> scopes=new TreeSet<>(before.entries.keySet());scopes.addAll(after.entries.keySet());
        Map<String,String> aliases=new TreeMap<>();
        for(var inventory:List.of(before,after))for(var e:inventory.entries.entrySet()) {
            String path=text(e.getValue().get("path"));String prior=aliases.putIfAbsent(path,e.getKey());if(prior!=null&&!prior.equals(e.getKey()))throw new IllegalArgumentException("path reused by different logical scope");
        }
        var edges=new ArrayList<Map<String,Object>>();var unresolved=new ArrayList<Map<String,Object>>();var stale=new HashSet<String>();
        for(var entry:after.artifacts.entrySet())for(Object raw:artifactDependencies(entry.getValue())) {
            var dependency=map(raw);String from=aliases.get(dependency.get("path"));
            if(from==null||!after.entries.containsKey(from)){unresolved.add(Json.object("consumer",entry.getKey(),"selection",dependency,"state","unselected-dependency"));continue;}
            var current=after.entries.get(from);if(!dependency.get("kind").equals(current.get("kind")))throw new IllegalArgumentException("dependency kind mismatch");
            boolean match=dependency.get("sha256").equals(current.get("sha256"));if(!match)stale.add(entry.getKey());
            edges.add(Json.object("from",from,"to",entry.getKey(),"relation","selected-input","selectedSha256",dependency.get("sha256"),"currentSha256",current.get("sha256"),"state",match?"current":"stale"));
        }
        edges.sort(Comparator.comparing(Json::write));var nodes=new ArrayList<Map<String,Object>>();boolean complete=unresolved.isEmpty();
        for(String scope:scopes) {
            var a=before.artifacts.get(scope);var b=after.artifacts.get(scope);String kind=text((b==null?a:b).get("artifactKind"));boolean known=Owners.known(kind);complete&=known;
            List<Map<String,Object>> delta=!known||a==null||b==null?List.of():differences(Owners.values(a),Owners.values(b),"",Owners.groups(kind));
            delta=delta.stream().map(d->{d.put("beforeOrigin",Owners.origin(a,text(d.get("pointer"))));d.put("afterOrigin",Owners.origin(b,text(d.get("pointer"))));return d;}).toList();
            boolean same=a!=null&&b!=null&&before.entries.get(scope).get("sha256").equals(after.entries.get(scope).get("sha256"));
            String change=!known?"unknown":a==null?"added":b==null?"removed":!delta.isEmpty()?"semantic-change":same?"unchanged":!Objects.equals(a.get("imports"),b.get("imports"))?"selection-change":"presentation-only";
            if(delta.stream().anyMatch(d->d.get("classification").equals("unknown")))complete=false;
            nodes.add(Json.object("scope",scope,"kind",kind,"origin",known?Owners.origin(b==null?a:b,""):Map.of(),"change",change,"supportState",!known?"unknown":stale.contains(scope)?"stale":change.equals("unchanged")||change.equals("presentation-only")?"no-direct-staleness":"reassessment-required","before",before.entries.get(scope),"after",after.entries.get(scope),"differences",delta));
        }
        return Json.object("format","mundane-change-analysis-0.1","id",v.get("id"),"complete",complete,"nodes",nodes,"edges",edges,"unresolved",unresolved,"authorization","none");
    }
    private static List<?> artifactDependencies(Map<String,Object> a) {
        var owner=Owners.all().get(a.get("artifactKind"));return owner==null?List.of():owner.dependencies(a);
    }
    public static List<Map<String,Object>> paths(Map<String,Object> analysis,String from,int depth) {
        if(depth<1||depth>64)throw new IllegalArgumentException("depth must be 1..64");
        if(Model.rows(analysis,"nodes").stream().noneMatch(n->n.get("scope").equals(from)))throw new IllegalArgumentException("unknown origin scope");
        var found=new TreeMap<String,List<String>>();var queue=new ArrayDeque<List<String>>();queue.add(List.of(from));found.put(from,List.of(from));
        while(!queue.isEmpty()) {var path=queue.remove();if(path.size()>depth)continue;
            for(var e:Model.rows(analysis,"edges"))if(e.get("from").equals(path.getLast())) {String target=text(e.get("to"));if(found.containsKey(target))continue;var next=new ArrayList<>(path);next.add(target);found.put(target,next);queue.add(next);}
        }
        found.remove(from);return found.entrySet().stream().map(e->Json.object("target",e.getKey(),"path",e.getValue(),"meaning","prospective-review-only")).toList();
    }
    public String view(Map<String,Object> a,Model.Context c) {
        var result=analyze(map(a.get("values")),c);var out=new StringBuilder("# Engineering change\n\n");
        out.append(Model.sourceLink(a,c,"/id",text(map(a.get("values")).get("id")))).append("\n\n| Scope | Meaning | Direct support |\n| --- | --- | --- |\n");
        for(var n:Model.rows(result,"nodes"))out.append("| ").append(Owners.link(map(n.get("origin")),c,text(n.get("scope")))).append(" | ").append(n.get("change")).append(" | ").append(n.get("supportState")).append(" |\n");
        return out+"\n"+Model.escape(Json.write(result))+"\n\nProspective paths do not establish satisfaction or accepted dispositions.\n";
    }
}
