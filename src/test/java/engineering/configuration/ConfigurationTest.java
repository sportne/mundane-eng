package engineering.configuration;
import engineering.artifacts.*;
import engineering.domain.*;
import engineering.domainsource.Source;
import java.nio.file.*;
import java.util.*;
import static engineering.artifacts.Checks.*;

public final class ConfigurationTest {
    private ConfigurationTest() {}
    public static void run() throws Exception {
        Path root=Files.createTempDirectory("configuration-test-");
        try {
            Files.writeString(root.resolve("asset.txt"),"synthetic resource\n");String pin=Snapshots.hash(Files.readAllBytes(root.resolve("asset.txt")));
            // JSON presentation is a YAML subset and uses the real maintained YAML reader.
            var value=Json.object("format",mundanereq.Versions.CONFIGURATION_SOURCE,"purpose","simulation-engineering",
                "configuration",Json.object("id","CFG","stage","designed","environment","simulation","observationBasis","design-intent","selections",List.of(Json.object("slot","asset","memberScope","asset"))),
                "baseline",Json.object("id","BL","members",List.of(Json.object("scope","asset","kind","native-resource","format","text/plain","path","asset.txt","sha256",pin,"required",true,"appliesTo",List.of("simulation"))),"sourceMappings",List.of()),"previous",null,"change",null);
            Files.write(root.resolve("source.yaml"),Json.bytes(value));var domain=new Configuration();var a=Source.compile("source.yaml",null,Configuration.context(root),domain);Files.write(root.resolve("baseline.json"),Json.bytes(a));
            var c=Configuration.context(root);Model.read(root.resolve("baseline.json"),c,domain);var receipt=Publication.publish(a,c,"baseline.json","store");
            Path retained=root.resolve(text(receipt.get("root")));var reread=Model.read(retained.resolve("baseline.json"),Configuration.context(retained),domain);
            if(!Json.write(a).equals(Json.write(reread)))throw new AssertionError("retained baseline differs");
            Publication.publish(a,c,"baseline.json","store");
            Files.writeString(retained.resolve("asset.txt"),"tampered\n");reject(()->Publication.publish(a,c,"baseline.json","store"));
            Files.writeString(root.resolve("asset.txt"),"changed\n");reject(()->Model.read(root.resolve("baseline.json"),Configuration.context(root),domain));
            Files.delete(root.resolve("asset.txt"));reject(()->Model.read(root.resolve("baseline.json"),Configuration.context(root),domain));
            var member=map(list(map(value.get("baseline")).get("members")).get(0));member.put("required",false);mapMutable(value,"baseline").put("members",List.of(member));Files.write(root.resolve("source.yaml"),Json.bytes(value));
            var optional=Source.compile("source.yaml",null,Configuration.context(root),domain);var findings=domain.resolve(map(optional.get("values")),Configuration.context(root));
            if(!findings.get(0).get("state").equals("optional-resource-unavailable"))throw new AssertionError("optional missing hidden");
            member.put("path","../escape");mapMutable(value,"baseline").put("members",List.of(member));Files.write(root.resolve("source.yaml"),Json.bytes(value));reject(()->Source.compile("source.yaml",null,Configuration.context(root),domain));
        }finally{try(var walk=Files.walk(root)){for(Path p:walk.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}}
        System.out.println("PASS configuration: real compilation, retained publication, immutable existing revision, tamper/missing/optional/path failure semantics");
    }
    @SuppressWarnings("unchecked") private static Map<String,Object> mapMutable(Map<String,Object> v,String k){return (Map<String,Object>)v.get(k);}
    private static void reject(Runnable r){try{r.run();throw new AssertionError("invalid accepted");}catch(IllegalArgumentException|Problem|java.io.UncheckedIOException expected){}}
}
