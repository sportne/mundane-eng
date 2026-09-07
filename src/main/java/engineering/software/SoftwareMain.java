package engineering.software;
import static engineering.artifacts.Checks.*;
import engineering.artifacts.Json;
import engineering.domain.*;
import engineering.domainsource.Source;
import java.io.PrintStream;
public final class SoftwareMain {
    private SoftwareMain() {}
    public static void main(String[] args){System.exit(run(args,System.out,System.err));}
    public static int run(String[] args,PrintStream out,PrintStream err) {
        var domain=new Software();return Cli.run(args,out,err,domain,o->{
            Cli.arity(o,o.command().equals("query")?2:1);var c=Software.context(o.root());
            if(o.command().equals("compile"))return Source.compile(o.inputs().get(0),o.imports(),c,domain);
            var a=Model.read(o.root().resolve(o.inputs().get(0)),c,domain);
            Object result=switch(o.command()) {
                case "check" -> a;
                case "view" -> domain.view(a,c);
                case "analyze" -> domain.analyze(map(a.get("values")),c);
                case "query" -> {
                    var analysis=domain.analyze(map(a.get("values")),c);
                    analysis.put("findings",Model.rows(analysis,"findings").stream().filter(f->f.get("advisory").equals(o.inputs().get(1))).toList());
                    yield analysis;
                }
                default -> throw new IllegalArgumentException("unknown command");
            };c.snapshots.recheck();return result;
        });
    }
}
