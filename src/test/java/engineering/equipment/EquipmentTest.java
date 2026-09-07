package engineering.equipment;
import static engineering.artifacts.Checks.*;
import engineering.artifacts.Json;
import mundane.yaml.Yaml;
import java.nio.file.*;
import java.util.*;
public final class EquipmentTest {
    private EquipmentTest() {}
    public static void run() throws Exception {
        var d=map(Yaml.document(Files.readAllBytes(Path.of("examples/ground-control-station/design/equipment.yaml")),8*1024*1024,true).value());
        if(!Equipment.topology(d).isEmpty())throw new AssertionError("valid topology");
        var bad=clone(d);var cables=new ArrayList<Object>(list(bad.get("cables")));cables.remove(1);bad.put("cables",cables);
        if(Equipment.topology(bad).stream().noneMatch(f->f.get("code").equals("unprotected-power-path")))throw new AssertionError("lost protection");
        bad=clone(d);var cable=map(list(bad.get("cables")).get(0));cable.put("conductors",List.of(Json.object("fromPin","P","toPin","R"),Json.object("fromPin","R","toPin","P")));
        cables=new ArrayList<>(list(bad.get("cables")));cables.set(0,cable);bad.put("cables",cables);reject(bad);
        bad=clone(d);cables=new ArrayList<>(list(bad.get("cables")));cable=map(cables.get(0));cable.put("id","OTHER");cables.add(cable);bad.put("cables",cables);reject(bad);
        if(Equipment.bom(d).size()!=6)throw new AssertionError("BOM count");
        System.out.println("PASS equipment topology: shared protection, disconnected path, crossed pins, competing feeds and BOM projection");
    }
    private static Map<String,Object> clone(Map<String,Object> d){return map(Json.read(Json.bytes(d)));}
    private static void reject(Map<String,Object> d){try{Equipment.topology(d);throw new AssertionError("invalid topology accepted");}catch(IllegalArgumentException expected){}}
}
