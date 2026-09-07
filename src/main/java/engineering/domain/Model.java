package engineering.domain;

import static engineering.artifacts.Checks.*;
import engineering.artifacts.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.BiConsumer;
import mundanereq.Versions;

/** Bounded provenance and local resource infrastructure; domain meanings stay with their owners. */
public final class Model {
    private Model() {}
    public interface Domain {
        String kind(); String format(); String source(); String version(); String contract();
        void validate(Map<String,Object> values,Context context);
        String view(Map<String,Object> artifact,Context context);
    }
    public static final class Context {
        public final Snapshots snapshots;
        public final Path root;
        public final Map<String,Map<String,Object>> imports=new TreeMap<>();
        public final Map<String,Map<String,Object>> selections=new TreeMap<>();
        private final Map<String,BiConsumer<Map<String,Object>,Context>> adapters;
        private final int depth;
        public Context(Path root,Map<String,BiConsumer<Map<String,Object>,Context>> adapters) {this(root,adapters,new Snapshots(root),0);}
        private Context(Path root,Map<String,BiConsumer<Map<String,Object>,Context>> adapters,Snapshots snapshots,int depth) {if(depth>16)throw new IllegalArgumentException("import depth exceeds 16");this.root=root;this.snapshots=snapshots;this.adapters=adapters;this.depth=depth;}
        public Context child(){return new Context(root,adapters,snapshots,depth+1);}
        public void select(Object entries) {
            for(Object item:list(entries)) {
                var e=map(item);keys(e,"scope","kind","format","path","sha256");String scope=id(e.get("scope"));
                if(scope.equals("self")||selections.putIfAbsent(scope,e)!=null)throw new IllegalArgumentException("ambiguous or reserved scope");
                String kind=text(e.get("kind"));var raw=readPinned(e);var artifact=map(Snapshots.json(raw));
                if(!e.get("format").equals(artifact.get("format"))||!kind.equals(artifact.get("artifactKind")))throw new IllegalArgumentException("import kind/format mismatch");
                if(kind.equals("requirements"))Artifacts.requirements(artifact,raw.path());
                else if(kind.equals("verification-plan"))Artifacts.plan(artifact,raw.path());
                else {
                    var adapter=adapters.get(kind);if(adapter==null)throw new IllegalArgumentException("unsupported import kind "+kind);adapter.accept(artifact,this);
                }
                imports.put(scope,artifact);
            }
        }
        public Snapshots.Snapshot readPinned(Map<String,Object> e) {
            String file=path(e.get("path")),pin=digest(e.get("sha256"));var s=snapshots.read(file);
            if(!s.sha256().equals(pin))throw new Problem("digest-mismatch","selected revision changed",file);return s;
        }
        public Map<String,Object> reference(Object raw,String kind) {
            var r=ref(raw,kind);var a=imports.get(r.get("scope"));if(a==null)throw new IllegalArgumentException("missing-scope "+r.get("scope"));
            if(kind.equals("requirement")) {
                var result=Artifacts.requirements(a,"import").get(r.get("id"));if(result==null)throw new IllegalArgumentException("missing-requirement");return map(result.get("values"));
            }
            if(kind.equals("activity")) {
                Artifacts.plan(a,"import");return find(list(a.get("activities")),text(r.get("id")));
            }
            var values=map(a.get("values"));
            if(kind.equals("baseline"))return equalId(map(values.get("baseline")),r);
            if(kind.equals("procedure"))return equalId(values,r);
            String group=switch(kind){case "interface"->"interfaces";case "component"->"components";case "mode"->"modes";case "hazard"->"hazards";case "control"->"controls";default->throw new IllegalArgumentException("unsupported reference kind");};
            return find(list(values.get(group)),text(r.get("id")));
        }
    }
    private static Map<String,Object> equalId(Map<String,Object> v,Map<String,Object> r) {if(!v.get("id").equals(r.get("id")))throw new IllegalArgumentException("missing-target");return v;}
    public static Map<String,Object> ref(Object value,String kind) {
        var r=map(value);keys(r,"scope","kind","id");id(r.get("scope"));id(r.get("id"));
        if(!kind.equals(r.get("kind")))throw new IllegalArgumentException("wrong-kind: expected "+kind);return r;
    }
    public static Map<String,Object> find(List<?> rows,String ident) {for(Object row:rows){var r=map(row);if(ident.equals(r.get("id")))return r;}throw new IllegalArgumentException("missing-target "+ident);}
    public static void unique(List<?> rows) {Set<String> ids=new HashSet<>();for(Object row:rows)if(!ids.add(id(map(row).get("id"))))throw new IllegalArgumentException("duplicate-id");}
    public static List<Map<String,Object>> rows(Map<String,Object> d,String name) {return list(d.get(name)).stream().map(engineering.artifacts.Checks::map).toList();}
    public static void envelope(Map<String,Object> a,Domain domain) {
        keys(a,"artifactKind","format","sourceContract","compiler","complete","sources","locations","imports","values","diagnostics");
        if(!domain.kind().equals(a.get("artifactKind"))||!domain.format().equals(a.get("format"))||!domain.source().equals(a.get("sourceContract")))throw new IllegalArgumentException("unsupported-format");
        if(!Boolean.TRUE.equals(a.get("complete"))||!list(a.get("diagnostics")).isEmpty())throw new IllegalArgumentException("incomplete-import");
        var producer=map(a.get("compiler"));keys(producer,"name","version","contract");
        if(!("mundane-"+domain.kind()).equals(producer.get("name"))||!domain.version().equals(producer.get("version"))||!domain.contract().equals(producer.get("contract")))throw new IllegalArgumentException("unsupported producer contract");
        var sources=list(a.get("sources"));if(sources.size()!=1)throw new IllegalArgumentException("expected one domain source");
        var source=map(sources.get(0));keys(source,"path","sha256");String file=path(source.get("path"));digest(source.get("sha256"));
        var locations=map(a.get("locations"));if(!locations.containsKey(""))throw new IllegalArgumentException("missing root location");
        for(Object loc:locations.values())location(loc,Set.of(file));
        // Every normalized value must have a source point; no forged new values without origins.
        pointers(a.get("values"),"",locations);
        var scopes=new HashSet<String>();for(Object e:list(a.get("imports"))) {
            var entry=map(e);keys(entry,"scope","kind","format","path","sha256");String scope=id(entry.get("scope"));
            if(scope.equals("self")||!scopes.add(scope))throw new IllegalArgumentException("ambiguous scope");text(entry.get("kind"));text(entry.get("format"));path(entry.get("path"));digest(entry.get("sha256"));
        }
    }
    private static void pointers(Object value,String at,Map<String,Object> locs) {
        if(!locs.containsKey(at))throw new IllegalArgumentException("missing source location "+at);
        if(value instanceof Map<?,?>)for(var e:map(value).entrySet())pointers(e.getValue(),mundane.json.Json.pointer(at,e.getKey()),locs);
        else if(value instanceof List<?> a)for(int i=0;i<a.size();i++)pointers(a.get(i),at+"/"+i,locs);
    }
    public static void validateSelected(Map<String,Object> a,Domain domain,Context parent) {envelope(a,domain);var nested=parent.child();nested.select(a.get("imports"));domain.validate(map(a.get("values")),nested);}
    public static Map<String,Object> read(Path file,Context context,Domain domain) {
        var a=map(Snapshots.json(context.snapshots.read(context.snapshots.argument(file))));envelope(a,domain);context.select(a.get("imports"));domain.validate(map(a.get("values")),context);return a;
    }
    public static String escape(Object s) {return String.valueOf(s).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("|","&#124;").replace("\n"," ").replace("\r"," ").replace("[","&#91;").replace("]","&#93;").replace("`","&#96;");}
    public static String sourceLink(Map<String,Object> a,Context c,String pointer,String label) {
        var source=map(list(a.get("sources")).get(0));var loc=map(map(a.get("locations")).getOrDefault(pointer,map(a.get("locations")).get("")));
        try {
            var snapshot=c.snapshots.read(path(source.get("path")));
            if(!snapshot.sha256().equals(source.get("sha256")))return escape(label)+" (source revision unavailable)";
            return "["+escape(label)+"]("+c.root.resolve(path(source.get("path"))).toAbsolutePath().toUri().toASCIIString()+"#L"+loc.get("line")+")";
        }catch(Problem unavailable){return escape(label)+" (source unavailable)";}
    }
    public static List<?> imports(Snapshots snapshots,String file) {
        if(file==null)return List.of();var d=map(Snapshots.json(snapshots.read(file)));keys(d,"format","imports");
        if(!Versions.DOMAIN_IMPORTS.equals(d.get("format")))throw new IllegalArgumentException("unsupported import selection");return list(d.get("imports"));
    }
}
