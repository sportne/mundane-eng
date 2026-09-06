package engineering.work;

import engineering.artifacts.*;
import static engineering.artifacts.Checks.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class WorkSnapshotTest {
    private WorkSnapshotTest() {}
    public static final String A="format: mundane-work-yaml-0.2\nid: A\nkind: task\ntitle: Alpha\nstatus: Planned\nbody: |\n  Literal prose.\n";
    public static Snapshots.Snapshot source(String path,String text) {return new Snapshots.Snapshot(path,text.getBytes(StandardCharsets.UTF_8),null);}
    public static void run() throws Exception {
        Path root=Files.createTempDirectory("work-snapshot");
        try {
            Files.writeString(root.resolve("a.yaml"),A);
            Files.writeString(root.resolve("set.json"),"{\"format\":\"mundane-work-set-0.2\",\"source\":\"mundane-work-yaml-0.2\",\"files\":[\"a.yaml\"]}");
            var buffer=WorkCompiler.compileSnapshots(List.of(source("a.yaml",A)));
            assert buffer.valid();assert buffer.items().equals(WorkCompiler.compile(root,"set.json").output().get("items"));
            for(String text:List.of(A.replace("Planned","Wrong"),A.replace("kind: task","kind: issue"),"format: [\n",A.stripTrailing())) {
                Files.writeString(root.resolve("a.yaml"),text);
                var bad=WorkCompiler.compileSnapshots(List.of(source("a.yaml",text)));
                assert !bad.valid();assert bad.diagnostics().equals(WorkCompiler.compile(root,"set.json").output().get("diagnostics"));
            }
            assert !WorkCompiler.compileSnapshots(List.of(source("a.yaml",A),source("b.yaml",A))).valid();
            var changed=WorkCompiler.compileSnapshots(List.of(source("a.yaml",A.replace("Alpha","Unsaved"))));
            assert map(changed.items().getFirst().get("values")).get("title").equals("Unsaved");
            assert !Files.readString(root.resolve("a.yaml")).contains("Unsaved");
            var request=Json.object("protocol",mundanereq.editor.EditorMain.PROTOCOL,"source","mundane-work-yaml-0.2","schema",null,
                "files",List.of(Json.object("path","a.yaml","text",A)));
            assert mundanereq.editor.EditorMain.analyze(request).get("valid").equals(true);
            for(Object schema:List.of("bad",Json.object("path","schema","text","{}"))) {
                request.put("schema",schema);boolean failed=false;
                try {mundanereq.editor.EditorMain.analyze(request);}catch(IllegalArgumentException e){failed=true;}
                assert failed;
            }
        } finally {try(var paths=Files.walk(root)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}}
        System.out.println("PASS work snapshots: disk/buffer semantics and failures agree, unsaved values do not write source");
    }
}
