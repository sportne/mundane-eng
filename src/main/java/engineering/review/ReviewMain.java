package engineering.review;
import static engineering.artifacts.Checks.*;
import engineering.artifacts.*;
import engineering.domain.*;
import engineering.change.*;
import engineering.assurance.*;
import java.util.Map;
public final class ReviewMain {
    private ReviewMain(){}
    public static void main(String[] args){System.exit(run(args,System.out,System.err));}
    public static int run(String[] args,java.io.PrintStream out,java.io.PrintStream err){
        var identity=new Model.Domain(){
            public String kind(){return "review";}public String format(){return "mundane-review-0.1";}public String source(){return "compiled-inventory";}public String version(){return mundanereq.Versions.REVIEW_VERSION;}public String contract(){return mundanereq.Versions.REVIEW_CONTRACT;}
            public void validate(Map<String,Object> v,Model.Context c){throw new IllegalArgumentException("review has no authored source");}
            public String view(Map<String,Object> a,Model.Context c){throw new IllegalArgumentException("explicit time and trust required");}
        };
        return Cli.run(args,out,err,identity,o->{
            Cli.arity(o,5);if(!o.command().equals("analyze")&&!o.command().equals("view"))throw new IllegalArgumentException("expected analyze or view");
            var c=Owners.context(o.root());var artifact=Model.read(o.root().resolve(o.inputs().get(0)),c,new Change());
            var trust=c.snapshots.read(o.inputs().get(2));var report=Review.analyze(artifact,c,Assurance.time(o.inputs().get(1)),ReviewSignatures.keys(Snapshots.json(trust)),o.inputs().get(3),o.inputs().get(4));
            report.put("trustSetSha256",trust.sha256());String result=o.command().equals("view")?Review.view(report,c):Json.write(report)+"\n";if(result.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>16*1024*1024)throw new IllegalArgumentException("review output exceeds 16 MiB");c.snapshots.recheck();return result;
        });
    }
}
