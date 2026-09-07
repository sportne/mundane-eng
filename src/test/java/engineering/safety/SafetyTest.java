package engineering.safety;
import engineering.artifacts.Json;
import java.util.List;
import java.util.Map;
public final class SafetyTest {
    private SafetyTest() {}
    public static void run() {
        var basic=Json.object("id","B","operator","basic","inputs",List.of(),"cause",ref("cause","CAUSE"));
        var other=Json.object("id","C","operator","basic","inputs",List.of(),"cause",ref("cause","CAUSE"));
        var gate=Json.object("id","A","operator","or","inputs",List.of(ref("event","B"),ref("event","C")),"cause",null);
        var tree=Json.object("top",ref("event","A"),"events",List.of(gate,basic,other));Safety.validateTree(tree);
        gate.put("inputs",List.of(ref("event","A"),ref("event","C")));reject(tree);
        gate.put("inputs",List.of(ref("event","B"),ref("event","B")));reject(tree);
        gate.put("inputs",List.of(ref("event","B")));reject(tree);
        gate.put("inputs",List.of(ref("event","B"),ref("event","ABSENT")));reject(tree);
        gate.put("inputs",List.of(ref("event","B"),ref("event","C")));gate.put("operator","and");Safety.validateTree(tree);
        basic.put("inputs",List.of(ref("event","C")));reject(tree);
        System.out.println("PASS safety fault trees: common cause reuse, AND/OR, cycles, missing and duplicate events, malformed gates");
    }
    private static Map<String,Object> ref(String kind,String id){return Json.object("scope","self","kind",kind,"id",id);}
    private static void reject(Map<String,Object> tree){try{Safety.validateTree(tree);throw new AssertionError("invalid tree accepted");}catch(IllegalArgumentException expected){}}
}
