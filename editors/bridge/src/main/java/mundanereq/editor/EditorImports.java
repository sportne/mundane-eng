package mundanereq.editor;

import static engineering.artifacts.Checks.*;
import static engineering.artifacts.Json.object;
import engineering.artifacts.*;
import engineering.work.WorkArtifact;
import engineering.work.WorkGraph;
import java.nio.charset.StandardCharsets;
import java.util.*;
import mundanereq.Versions;

/** Strict serialized imports over supplied bytes; never opens paths or builds artifacts. */
public final class EditorImports {
    private EditorImports() {}
    public record Target(String scope,String kind,String id,String title,String status,String revision,
            String file,String sourceDigest,Map<String,Object> origin,String sourceText,String sourceState) {
        public String key() {return scope+":"+kind+":"+id;}
        public String state() {return sourceState;}
        public Map<String,Object> describe() {return object("scope",scope,"kind",kind,"id",id,"title",title,"status",status,
            "revision",revision,"path",file,"sourceState",state());}
    }
    public record Selection(Map<String,Target> targets) {}
    public static Selection read(Object value) {
        var packet=map(value);keys(packet,"selection","manifest","artifacts","sources");
        var selection=file(packet.get("selection"),65536,false);var settings=map(Json.read(bytes(selection.get("text"))));
        keys(settings,"format","manifest","sourceRoots");
        if(!"mundane-editor-imports-0.1".equals(settings.get("format")))throw new IllegalArgumentException("unsupported editor import selection");
        var manifest=file(packet.get("manifest"),65536,false);
        if(!path(settings.get("manifest")).equals(manifest.get("path")))throw new IllegalArgumentException("import manifest path differs from selection");
        var declaration=map(Json.read(bytes(manifest.get("text"))));keys(declaration,"format","imports");
        if(!Versions.IMPORT_FORMAT.equals(declaration.get("format")))throw new IllegalArgumentException("unsupported import manifest");
        var roots=map(settings.get("sourceRoots"));for(var root:roots.entrySet()){id(root.getKey());root(root.getValue());}
        var artifacts=files(packet.get("artifacts"),100,16*1024*1024,false);
        var sources=files(packet.get("sources"),256,8*1024*1024,true);
        var hashes=new TreeMap<String,String>();sources.forEach((name,text)->{if(text!=null)hashes.put(name,Snapshots.hash(bytes(text)));});
        var declarations=list(declaration.get("imports"));if(declarations.size()>100)throw new IllegalArgumentException("too many imports");
        var wantedArtifacts=new TreeSet<String>();var wantedSources=new TreeSet<String>();
        Map<String,List<String>> graph=new TreeMap<>();Map<String,Map<String,Object>> locations=new TreeMap<>();
        Map<String,Target> targets=new TreeMap<>();
        for(Object raw:declarations) {
            var entry=map(raw);keys(entry,"scope","path","kind","sha256","dependsOn");String scope=id(entry.get("scope"));
            if(scope.equals("work")||graph.containsKey(scope))throw new Problem("duplicate-scope","duplicate/reserved editor import scope "+scope,(String)manifest.get("path"));
            var deps=list(entry.get("dependsOn")).stream().map(Checks::id).toList();
            if(new HashSet<>(deps).size()!=deps.size())throw new IllegalArgumentException("duplicate build dependency");
            graph.put(scope,deps);locations.put(scope,object("path",manifest.get("path"),"line",1,"column",1));
            if(!roots.containsKey(scope))throw new IllegalArgumentException("missing source root for "+scope);
            String prefix=root(roots.get(scope)),artifactPath=path(entry.get("path")),kind=text(entry.get("kind"));
            if(!Set.of("requirements","work-items").contains(kind))throw new Problem("wrong-kind","unsupported editor import kind "+kind,(String)manifest.get("path"));
            wantedArtifacts.add(artifactPath);String encoded=artifacts.get(artifactPath);
            if(encoded==null)throw new IllegalArgumentException("missing compiled snapshot "+artifactPath);
            String revision=Snapshots.hash(bytes(encoded));
            if(entry.get("sha256")!=null&&!digest(entry.get("sha256")).equals(revision))throw new Problem("digest-mismatch","compiled pin differs for "+scope,(String)manifest.get("path"));
            var artifact=map(Json.read(bytes(encoded)));Map<String,Map<String,Object>> records;
            if(kind.equals("requirements"))records=Artifacts.requirements(artifact,artifactPath);
            else {
                records=WorkArtifact.validate(artifact,artifactPath);
                if(!Versions.WORK_ARTIFACT.equals(artifact.get("format")))throw new Problem("unsupported-format","editor imports require YAML work items",artifactPath);
            }
            var digests=new TreeMap<String,String>();
            for(Object rawSource:list(artifact.get("sources"))) {
                var source=map(rawSource);String name=path(source.get("path"));digests.put(name,digest(source.get("sha256")));
                wantedSources.add(mapped(prefix,name));
            }
            for(var record:records.entrySet()) {
                if(targets.size()>=10000)throw new IllegalArgumentException("too many editor targets");
                var values=map(record.getValue().get("values"));Map<String,Object> origin;
                if(kind.equals("requirements")) {
                    var spans=list(map(map(record.getValue().get("locations")).get("fields")).get("id"));
                    if(spans.size()!=1)throw new IllegalArgumentException("ambiguous compiled ID origin");origin=map(spans.getFirst());
                } else origin=map(record.getValue().get("location"));
                String original=text(origin.get("path"));
                String name=mapped(prefix,original);
                var target=new Target(scope,kind.equals("requirements")?"requirement":"work-item",record.getKey(),text(values.get("title")),
                    kind.equals("work-items")?text(values.get("status")):null,revision,name,digests.get(original),origin,sources.get(name),sources.get(name)==null?"unavailable":Objects.equals(hashes.get(name),digests.get(original))?"matching":"modified");
                if(target.sourceDigest()==null)throw new IllegalArgumentException("target source absent from inventory");
                targets.put(target.key(),target);
            }
        }
        if(!roots.keySet().equals(graph.keySet()))throw new IllegalArgumentException("source roots must match import scopes exactly");
        if(!artifacts.keySet().equals(wantedArtifacts)||!sources.keySet().equals(wantedSources))throw new IllegalArgumentException("editor import snapshot inventory differs");
        WorkGraph.acyclic(graph,"build-cycle",locations);return new Selection(Collections.unmodifiableMap(targets));
    }
    private static Map<String,String> files(Object value,int count,int limit,boolean nullable) {
        var list=list(value);if(list.size()>count)throw new IllegalArgumentException("too many snapshot files");
        Map<String,String> result=new TreeMap<>();long total=0;
        for(Object raw:list) {
            var file=file(raw,limit,nullable);String path=(String)file.get("path"),text=(String)file.get("text");
            if(result.containsKey(path))throw new IllegalArgumentException("duplicate snapshot path");
            if(text!=null)total+=bytes(text).length;if(total>16*1024*1024)throw new IllegalArgumentException("snapshot bytes exceed limit");
            result.put(path,text);
        }
        return result;
    }
    private static Map<String,Object> file(Object value,int limit,boolean nullable) {
        var file=map(value);keys(file,"path","text");path(file.get("path"));
        if(file.get("text")==null&&nullable)return file;
        if(!(file.get("text") instanceof String s)||bytes(s).length>limit)throw new IllegalArgumentException("invalid bounded file text");
        return file;
    }
    private static byte[] bytes(Object value) {return ((String)value).getBytes(StandardCharsets.UTF_8);}
    private static String root(Object value) {return ".".equals(value)?"":path(value);}
    private static String mapped(String root,String file) {return root.isEmpty()?path(file):path(root+"/"+file);}
}
