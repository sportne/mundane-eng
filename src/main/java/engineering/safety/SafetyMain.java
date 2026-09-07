package engineering.safety;
import static engineering.artifacts.Checks.*;
import engineering.artifacts.Json;
import engineering.domain.*;
import engineering.domainsource.Source;
import java.io.PrintStream;
public final class SafetyMain {
    private SafetyMain() {}
    public static void main(String[] args){System.exit(run(args,System.out,System.err));}
    public static int run(String[] args,PrintStream out,PrintStream err) {
        var domain=new Safety();return Cli.run(args,out,err,domain,o->{
            Cli.arity(o,o.command().equals("query")?2:1);var c=Safety.context(o.root());
            if(o.command().equals("compile"))return Source.compile(o.inputs().get(0),o.imports(),c,domain);
            var a=Model.read(o.root().resolve(o.inputs().get(0)),c,domain);var d=map(a.get("values"));Object result=switch(o.command()) {
                case "check" -> a;case "view" -> domain.view(a,c);
                case "analyze" -> Json.object("format","mundane-safety-analysis-0.1","findings",domain.analyze(d,c),"riskAcceptance","not-established");
                case "query" -> domain.query(d,o.inputs().get(1));default -> throw new IllegalArgumentException("unknown command");
            };c.snapshots.recheck();return result;
        });
    }
}
