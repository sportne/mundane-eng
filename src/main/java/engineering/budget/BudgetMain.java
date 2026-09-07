package engineering.budget;
import static engineering.artifacts.Checks.*;
import engineering.artifacts.Json;
import engineering.domain.*;
import engineering.domainsource.Source;
import java.io.PrintStream;
public final class BudgetMain {
    private BudgetMain() {}
    public static void main(String[] args){System.exit(run(args,System.out,System.err));}
    public static int run(String[] args,PrintStream out,PrintStream err) {
        var domain=new Budget();return Cli.run(args,out,err,domain,o->{
            Cli.arity(o,o.command().equals("compare")?2:1);var c=Budget.context(o.root());
            if(o.command().equals("compile"))return Source.compile(o.inputs().get(0),o.imports(),c,domain);
            var a=Model.read(o.root().resolve(o.inputs().get(0)),c,domain);
            Object result=switch(o.command()) {
                case "check" -> a;case "view" -> domain.view(a,c);case "calculate","analyze" -> domain.analyze(map(a.get("values")),c);
                case "compare" -> {
                    var otherContext=Budget.context(o.root());var b=Model.read(o.root().resolve(o.inputs().get(1)),otherContext,domain);
                    var before=domain.analyze(map(a.get("values")),c);var after=domain.analyze(map(b.get("values")),otherContext);otherContext.snapshots.recheck();
                    yield Json.object("format","mundane-budget-change-0.1","inputRevisionChanged",!a.get("sources").equals(b.get("sources"))||!a.get("imports").equals(b.get("imports")),"previousResultState",a.get("values").equals(b.get("values"))&&a.get("imports").equals(b.get("imports"))?"same-inputs":"stale-for-new-inputs","before",before,"after",after);
                }
                default -> throw new IllegalArgumentException("unknown command");
            };c.snapshots.recheck();return result;
        });
    }
}
