package engineering.budget;
import static engineering.artifacts.Checks.*;
import engineering.artifacts.Json;
import mundane.yaml.Yaml;
import java.nio.file.*;
import java.math.BigDecimal;
import java.util.*;
public final class BudgetTest {
    private BudgetTest() {}
    public static void run() throws Exception {
        var d=map(Yaml.document(Files.readAllBytes(Path.of("examples/ground-control-station/design/budget.yaml")),8*1024*1024,true).value());
        var e=new Quantities.Evaluator(d);
        for(var expected:Map.of("TOTAL-POWER",90.0,"RUNTIME",.96,"NETWORK-TOTAL",15.0,"STORAGE",7.2).entrySet()) {
            String unit=switch(expected.getKey()){case "TOTAL-POWER"->"W";case "RUNTIME"->"h";case "NETWORK-TOTAL"->"Mbps";default->"GB";};
            double actual=((Number)e.get(expected.getKey()).output(unit).get("min")).doubleValue();
            if(Math.abs(actual-expected.getValue())>1e-12)throw new AssertionError("independent reference disagreement");
        }
        var third=new Quantities.Interval(BigDecimal.ONE,BigDecimal.ONE,Quantities.unit("1").dimension()).multiply(new Quantities.Interval(new BigDecimal(3),new BigDecimal(3),Quantities.unit("1").dimension()),true);
        if(third.low().multiply(new BigDecimal(3)).compareTo(BigDecimal.ONE)>0||third.high().multiply(new BigDecimal(3)).compareTo(BigDecimal.ONE)<0)throw new AssertionError("rounding fails to enclose one third");
        reject(()->third.multiply(new Quantities.Interval(BigDecimal.ZERO,BigDecimal.ONE,third.dimension()),true));
        reject(()->e.get("TOTAL-POWER").add(e.get("RUNTIME"),false));
        var bad=map(Json.read(Json.bytes(d)));var formulas=new ArrayList<Object>(list(bad.get("formulas")));var formula=map(formulas.get(0));formula.put("inputs",List.of("TOTAL-POWER","HOST-PEAK"));formulas.set(0,formula);bad.put("formulas",formulas);reject(()->new Quantities.Evaluator(bad));
        System.out.println("PASS budget arithmetic: independent hand results, dimensional rejection, cyclic formula, zero divisor and directed interval enclosure");
    }
    private static void reject(Runnable action){try{action.run();throw new AssertionError("invalid calculation accepted");}catch(IllegalArgumentException expected){}}
}
