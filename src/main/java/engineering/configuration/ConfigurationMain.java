package engineering.configuration;
import static engineering.artifacts.Checks.*;
import engineering.artifacts.Json;
import engineering.domain.*;
import engineering.domainsource.Source;
import java.io.PrintStream;

public final class ConfigurationMain {
    private ConfigurationMain() {}
    public static void main(String[] args){System.exit(run(args,System.out,System.err));}
    public static int run(String[] args,PrintStream out,PrintStream err) {
        var domain=new Configuration();return Cli.run(args,out,err,domain,o->{
            Cli.arity(o,o.command().equals("compare")||o.command().equals("publish")?2:1);var c=Configuration.context(o.root());
            if(o.command().equals("compile"))return Source.compile(o.inputs().get(0),o.imports(),c,domain);
            var a=Model.read(o.root().resolve(o.inputs().get(0)),c,domain);
            Object result=switch(o.command()) {
                case "check" -> a;
                case "view" -> domain.view(a,c);
                case "resolve" -> Json.object("format","mundane-configuration-availability-0.1","baseline",map(map(a.get("values")).get("baseline")).get("id"),"resources",domain.resolve(map(a.get("values")),c),"authorization","none");
                case "compare" -> {var other=Model.read(o.root().resolve(o.inputs().get(1)),c.child(),domain);yield Json.object("format","mundane-configuration-comparison-0.1","changes",Configuration.compare(map(a.get("values")),map(other.get("values"))));}
                case "publish" -> Publication.publish(a,c,o.inputs().get(0),o.inputs().get(1));
                default -> throw new IllegalArgumentException("unknown command");
            };c.snapshots.recheck();return result;
        });
    }
}
