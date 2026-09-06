package mundanereq.editor;

import static engineering.artifacts.Checks.*;
import static engineering.artifacts.Json.object;
import engineering.artifacts.*;
import java.nio.file.*;
import java.util.*;

public final class EditorImportsTest {
    private EditorImportsTest() {}
    public static Map<String,Object> packet() throws Exception {
        String source=Files.readString(Path.of("examples/attributes/system.mreq.yaml"));
        String artifact=Files.readString(Path.of("experiments/0036-project-attributes/golden/requirements.json"));
        var a=map(Json.read(artifact.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        var sources=new ArrayList<Object>();
        for(Object raw:list(a.get("sources"))) {
            String name=text(map(raw).get("path"));sources.add(object("path","vendor/"+name,"text",Files.readString(Path.of(name))));
        }
        assert !source.isEmpty();
        return object("selection",object("path","editor-imports.json","text",Json.write(object("format","mundane-editor-imports-0.1","manifest","imports.json","sourceRoots",object("req","vendor")))),
            "manifest",object("path","imports.json","text",Json.write(object("format","mundane-imports-0.1","imports",List.of(object("scope","req","path","compiled.json","kind","requirements","sha256",Snapshots.hash(artifact.getBytes(java.nio.charset.StandardCharsets.UTF_8)),"dependsOn",List.of()))))),
            "artifacts",List.of(object("path","compiled.json","text",artifact)),"sources",sources);
    }
    public static void run() throws Exception {
        var packet=packet();var result=EditorImports.read(packet);
        assert !result.targets().isEmpty();
        var resolved=ImportedNavigation.resolve(result,List.of());
        assert resolved.targets().values().stream().allMatch(t->t.get("sourceState").equals("matching"));
        var one=result.targets().values().iterator().next();
        var ref=object("scope",one.scope(),"kind",one.kind(),"id",one.id(),"location",object("path","local.yaml","start",object("line",1,"column",1),"end",object("line",1,"column",2)));
        assert ImportedNavigation.resolve(result,List.of(ref)).navigation().size()==1;
        var forged=new EditorImports.Target(one.scope(),one.kind(),one.id(),one.title(),one.status(),one.revision(),one.file(),one.sourceDigest(),
            object("path","ignored","start",object("line",1,"column",1),"end",object("line",1,"column",2)),one.sourceText(),"matching");
        var badOrigin=ImportedNavigation.resolve(new EditorImports.Selection(Map.of(forged.key(),forged)),List.of(ref));
        assert badOrigin.navigation().isEmpty();assert badOrigin.diagnostics().getFirst().get("severity").equals("warning");
        assert result.targets().values().stream().allMatch(t->t.state().equals("matching"));
        var missing=copy(packet);
        var sources=new ArrayList<Object>(list(missing.get("sources")));var first=new TreeMap<>(map(sources.getFirst()));first.put("text",null);sources.set(0,first);missing.put("sources",sources);
        assert EditorImports.read(missing).targets().values().stream().anyMatch(t->t.state().equals("unavailable"));
        first.put("text","changed\n");assert EditorImports.read(missing).targets().values().stream().anyMatch(t->t.state().equals("modified"));
        for(String bad:List.of("pin","scope","dependency","kind","format","mapping","duplicates","inventory")) {
            var p=copy(packet);var manifest=map(p.get("manifest"));var m=map(Json.read(((String)manifest.get("text")).getBytes(java.nio.charset.StandardCharsets.UTF_8)));var entry=map(list(m.get("imports")).getFirst());
            switch(bad) {
                case "pin" -> entry.put("sha256","0".repeat(64));
                case "scope" -> entry.put("scope","work");
                case "dependency" -> entry.put("dependsOn",List.of("req"));
                case "kind" -> entry.put("kind","verification-plan");
                case "format" -> m.put("format","unsupported");
                case "mapping" -> entry.put("scope","unmapped");
                case "inventory" -> p.put("sources",List.of());
                default -> { }
            }
            m.put("imports",bad.equals("duplicates")?List.of(entry,entry):List.of(entry));manifest.put("text",Json.write(m));p.put("manifest",manifest);reject(p);
        }
        var duplicate=copy(packet);var selection=map(duplicate.get("selection"));selection.put("text","{\"format\":\"bad\",\"format\":\"bad\"}");duplicate.put("selection",selection);reject(duplicate);
        System.out.println("PASS editor imports: validated compiled attributes, exact pins, mapped hashes, missing/changed sources and rejected scopes/dependencies/JSON/inventories");
    }
    private static Map<String,Object> copy(Object value){return map(Json.read(Json.bytes(value)));}
    private static void reject(Object value){try{EditorImports.read(value);}catch(IllegalArgumentException|Problem expected){return;}throw new AssertionError("invalid editor imports accepted");}
}
