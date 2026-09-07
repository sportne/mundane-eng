package engineering.architecture;
import engineering.domain.*;
import engineering.domainsource.Source;
import java.io.PrintStream;
import java.util.Map;

public final class ArchitectureMain {
    private ArchitectureMain() {}
    public static void main(String[] args){System.exit(run(args,System.out,System.err));}
    public static int run(String[] args,PrintStream out,PrintStream err) {
        var domain=new Architecture();return Cli.run(args,out,err,domain,o->{
            Cli.arity(o,1);var context=new Model.Context(o.root(),Map.of());
            Object result=switch(o.command()) {
                case "compile" -> Source.compile(o.inputs().get(0),o.imports(),context,domain);
                case "check","view" -> {var a=Model.read(o.root().resolve(o.inputs().get(0)),context,domain);yield o.command().equals("view")?domain.view(a,context):a;}
                default -> throw new IllegalArgumentException("unknown command");
            };context.snapshots.recheck();return result;
        });
    }
}
