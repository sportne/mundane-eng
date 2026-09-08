package engineering.assurance;
import static engineering.artifacts.Checks.*;
import engineering.artifacts.*;
import engineering.domain.*;
import engineering.domainsource.Source;
import java.io.PrintStream;
public final class AssuranceMain {
    private AssuranceMain() {}
    public static void main(String[] args){System.exit(run(args,System.out,System.err));}
    public static int run(String[] args,PrintStream out,PrintStream err){var d=new Assurance();return Cli.run(args,out,err,d,o->{
        boolean analysis=o.command().equals("analyze")||o.command().equals("view")||o.command().equals("query");
        Cli.arity(o,analysis?(o.command().equals("query")?4:3):1);var c=Assurance.context(o.root());
        if(o.command().equals("compile"))return Source.compile(o.inputs().get(0),o.imports(),c,d);
        var a=Model.read(o.root().resolve(o.inputs().get(0)),c,d);var v=map(a.get("values"));Object result;
        if(o.command().equals("check"))result=a;
        else if(o.command().equals("subjects"))result=d.subjects(v,c);
        else if(analysis){var trust=c.snapshots.read(o.inputs().get(2));var r=d.analyze(v,c,Assurance.time(o.inputs().get(1)),ReviewSignatures.keys(Snapshots.json(trust)));r.put("trustSetSha256",trust.sha256());
            if(o.command().equals("query")){String id=o.inputs().get(3);Model.find(Model.rows(v,"claims"),id);r.put("claims",Model.rows(r,"claims").stream().filter(row->row.get("id").equals(id)).toList());}
            result=o.command().equals("view")?d.report(a,c,r):r;
        }else throw new IllegalArgumentException("unknown command");c.snapshots.recheck();return result;
    });}
}
