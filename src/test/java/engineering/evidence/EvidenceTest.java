package engineering.evidence;
import static engineering.artifacts.Checks.*;
import engineering.artifacts.Json;
import engineering.domain.Model;
import mundane.yaml.Yaml;
import java.nio.file.*;
import java.util.*;

public final class EvidenceTest {
    private EvidenceTest() {}
    public static void run() throws Exception {
        var arch=map(Yaml.document(Files.readAllBytes(Path.of("examples/ground-control-station/design/architecture.yaml")),100000,true).value());
        var telemetry=Model.find(list(arch.get("interfaces")),"IF-TELEMETRY");var command=Model.find(list(arch.get("interfaces")),"IF-COMMAND");var heartbeat=Model.find(list(arch.get("interfaces")),"IF-HEARTBEAT");
        for(String name:List.of("stale","combined")) {
            var p=map(Yaml.document(Files.readAllBytes(Path.of("examples/ground-control-station/design/procedure-"+name+".yaml")),100000,true).value());
            var observations=Simulator.execute(p,telemetry,command,heartbeat,"nominal");var run=Json.object("execution","completed","clock",p.get("clock"),"observations",observations);
            if(!Evidence.evaluate(p,run).get("state").equals("pass"))throw new AssertionError(Json.write(Evidence.evaluate(p,run)));
            // Expectations are not simulator input: changing them cannot change raw observations.
            var altered=map(Json.read(Json.bytes(p)));var criteria=new ArrayList<Object>(list(altered.get("expected")));var first=map(criteria.get(0));first.put("equals",name.equals("stale")?"fresh":false);criteria.set(0,first);altered.put("expected",criteria);
            if(!Json.write(Simulator.execute(altered,telemetry,command,heartbeat,"nominal")).equals(Json.write(observations)))throw new AssertionError("simulator echoed expectations");
            if(!Evidence.evaluate(altered,run).get("state").equals("fail"))throw new AssertionError("wrong expectation passed");
            run.put("observations",List.of());if(!Evidence.evaluate(p,run).get("state").equals("inconclusive"))throw new AssertionError("missing observations passed");
            for(String status:List.of("skipped","interrupted")){run.put("execution",status);if(!Evidence.evaluate(p,run).get("state").equals(status))throw new AssertionError("execution state lost");}
            run.put("execution","completed");run.put("clock",Json.object("kind","synthetic-monotonic","unit","ms","uncertaintyMs",999));if(!Evidence.evaluate(p,run).get("state").equals("inconclusive"))throw new AssertionError("uncertain clock passed");
            if(name.equals("stale")) {
                run.put("clock",p.get("clock"));run.put("observations",Simulator.execute(p,telemetry,command,heartbeat,"suppress-stale"));if(!Evidence.evaluate(p,run).get("state").equals("fail"))throw new AssertionError("simulator defect escaped");
            }
        }
        System.out.println("PASS evidence: event-driven simulator separate from expectations, stale/combined fault runs, injected defect, missing/uncertain/skipped/interrupted outcomes");
    }
}
