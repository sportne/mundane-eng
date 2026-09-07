package engineering.procedure;
import engineering.domain.*;
import engineering.domainsource.Source;
import java.io.PrintStream;
public final class ProcedureMain {
    private ProcedureMain() {}
    public static void main(String[] args){System.exit(run(args,System.out,System.err));}
    public static int run(String[] args,PrintStream out,PrintStream err){var domain=new Procedure();return Cli.run(args,out,err,domain,o->{
        Cli.arity(o,1);var c=Procedure.context(o.root());if(o.command().equals("compile"))return Source.compile(o.inputs().get(0),o.imports(),c,domain);
        var a=Model.read(o.root().resolve(o.inputs().get(0)),c,domain);Object result=switch(o.command()){case "check"->a;case "view"->domain.view(a,c);default->throw new IllegalArgumentException("unknown command");};c.snapshots.recheck();return result;
    });}
}
