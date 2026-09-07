package engineering.work;

import static engineering.artifacts.Checks.*;
import static engineering.artifacts.Json.object;
import engineering.artifacts.*;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import mundanereq.Versions;

public final class WorkCompiler {
    private WorkCompiler() {}
    public static WorkResult compile(Path root,String manifest) {
        Snapshots reads=new Snapshots(root);List<Object> sources=new ArrayList<>(),diagnostics=new ArrayList<>();Map<String,Object> items=new TreeMap<>();Object selection=null;int status=0;
        try {
            var input=reads.read(manifest);selection=object("path",input.path(),"sha256",input.sha256());
            List<String> files=new ArrayList<>();
            try {
                var m=map(Snapshots.json(input));
                keys(m,"format","source","files");
                if(!Versions.WORK_SET.equals(m.get("format"))||!Versions.WORK_SOURCE.equals(m.get("source")))throw new IllegalArgumentException("unsupported YAML work selection");
                var raw=list(m.get("files"));if(raw.isEmpty()||raw.size()>10000)throw new IllegalArgumentException("invalid work selection count");
                var unique=new TreeSet<String>();for(Object value:raw)if(!unique.add(path(value)))throw new IllegalArgumentException("duplicate selected path");files.addAll(unique);
            } catch(IllegalArgumentException e) {throw new Problem("invalid-work-set",e.getMessage(),manifest);}
            for(String file:files) {
                try {
                    var source=reads.read(file,1024*1024);sources.add(object("path",file,"sha256",source.sha256()));addItem(items,source);
                } catch(Problem p) {diagnostics.add(p.diagnostic());status=Math.max(status,p.operational()?2:1);}
            }
            reads.recheck();
        } catch(Problem p) {diagnostics.add(p.diagnostic());status=Math.max(status,p.operational()?2:1);}
        var output=object("artifactKind","work-items","format",Versions.WORK_ARTIFACT,"sourceContract",Versions.WORK_SOURCE,
            "compiler",object("name","mundane-work","version",Versions.WORK_VERSION,"contract",Versions.WORK_CONTRACT),
            "complete",status==0,"selection",selection,"sources",sources,"items",status==0?new ArrayList<>(items.values()):List.of(),"diagnostics",diagnostics);
        if(Json.bytes(output).length>16*1024*1024) {
            status=1;output.put("complete",false);output.put("items",List.of());output.put("diagnostics",List.of(new Problem("work-output-limit","compiled output exceeds 16 MiB",manifest).diagnostic()));
        }
        return new WorkResult(output,status);
    }
    /** Buffer semantics only: no selection, filesystem, imports or generated artifact. */
    public record SnapshotResult(List<Map<String,Object>> items,List<Map<String,Object>> diagnostics) {
        public boolean valid() { return diagnostics.isEmpty(); }
    }
    public static SnapshotResult compileSnapshots(List<Snapshots.Snapshot> sources) {
        if(sources.isEmpty()||sources.size()>128)throw new IllegalArgumentException("select 1–128 work files");
        var paths=new TreeSet<String>();long total=0;
        var items=new TreeMap<String,Object>();var diagnostics=new ArrayList<Map<String,Object>>();
        for(var source:sources.stream().sorted(java.util.Comparator.comparing(Snapshots.Snapshot::path)).toList()) {
            path(source.path());
            if(!paths.add(source.path()))throw new IllegalArgumentException("duplicate selected path");
            int length=source.bytes().length;total+=length;
            if(length>1024*1024||total>16*1024*1024)throw new IllegalArgumentException("work snapshot exceeds limit");
            try {addItem(items,source);} catch(Problem p) {diagnostics.add(p.diagnostic());}
        }
        return new SnapshotResult(diagnostics.isEmpty()?items.values().stream().map(Checks::map).toList():List.of(),List.copyOf(diagnostics));
    }
    private static void addItem(Map<String,Object> items,Snapshots.Snapshot source) {
        var item=parse(source);var values=map(item.get("values"));
        values.put("dependencies",list(values.get("dependencies")).stream().map(Checks::id).sorted().toList());
        values.put("relations",list(values.get("relations")).stream().sorted(java.util.Comparator.comparing(Json::write)).toList());
        item.put("values",values);String id=text(values.get("id"));
        if(items.putIfAbsent(id,item)!=null)throw new Problem("duplicate-work-id","duplicate work ID "+id,map(item.get("location")));
    }
    private static Map<String,Object> parse(Snapshots.Snapshot snapshot) {
        String file=snapshot.path();int line=1;
        try {
            String source=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(snapshot.bytes())).toString();
            if(source.startsWith("\ufeff")||!source.endsWith("\n")||source.indexOf(0)>=0||source.replace("\r\n","").indexOf('\r')>=0)throw new IllegalArgumentException("invalid physical work source");
            return WorkYaml.parse(file,source);
        } catch(java.nio.charset.CharacterCodingException|IllegalArgumentException e) {throw new Problem("invalid-work-source",e.getMessage(),object("path",file,"line",line,"column",1));}
    }
}
