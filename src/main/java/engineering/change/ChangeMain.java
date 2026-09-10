package engineering.change;
import static engineering.artifacts.Checks.*;
import engineering.domain.*;
import engineering.domainsource.Source;
import java.io.PrintStream;
public final class ChangeMain {
    private ChangeMain(){}
    public static void main(String[] args){System.exit(run(args,System.out,System.err));}
    public static int run(String[] args,PrintStream out,PrintStream err){var d=new Change();return Cli.run(args,out,err,d,o->{
        Cli.arity(o,o.command().equals("query")?3:1);var c=Owners.context(o.root());
        if(o.command().equals("compile"))return Source.compile(o.inputs().getFirst(),o.imports(),c,d);
        var a=Model.read(o.root().resolve(o.inputs().getFirst()),c,d);Object result=switch(o.command()) {
            case "check"->a;case "view"->d.view(a,c);case "analyze"->d.analyze(map(a.get("values")),c);
            case "query"->{var r=d.analyze(map(a.get("values")),c);r.put("paths",Change.paths(r,o.inputs().get(1),Integer.parseInt(o.inputs().get(2))));yield r;}
            default->throw new IllegalArgumentException("unknown command");};c.snapshots.recheck();return result;
    });}
}
