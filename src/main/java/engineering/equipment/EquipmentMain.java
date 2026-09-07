package engineering.equipment;
import static engineering.artifacts.Checks.*;
import engineering.artifacts.Json;
import engineering.domain.*;
import engineering.domainsource.Source;
import java.io.PrintStream;
public final class EquipmentMain {
    private EquipmentMain() {}
    public static void main(String[] args){System.exit(run(args,System.out,System.err));}
    public static int run(String[] args,PrintStream out,PrintStream err) {
        var domain=new Equipment();return Cli.run(args,out,err,domain,o->{
            Cli.arity(o,1);var c=Equipment.context(o.root());
            if(o.command().equals("compile"))return Source.compile(o.inputs().get(0),o.imports(),c,domain);
            var a=Model.read(o.root().resolve(o.inputs().get(0)),c,domain);
            Object result=switch(o.command()) {
                case "check" -> a;case "view" -> domain.view(a,c);
                case "bom" -> domain.render(a,c,true,false);case "wiring" -> domain.render(a,c,false,true);
                case "analyze" -> Json.object("format","mundane-equipment-analysis-0.1","findings",domain.analyze(map(a.get("values")),c),"procurementApproval","none");
                default -> throw new IllegalArgumentException("unknown command");
            };c.snapshots.recheck();return result;
        });
    }
}
