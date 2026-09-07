package engineering.evidence;
import static engineering.artifacts.Checks.*;
import engineering.artifacts.*;
import engineering.domain.*;
import engineering.domainsource.Source;
import engineering.procedure.Procedure;
import mundane.yaml.Yaml;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import mundanereq.Versions;

/** Explicit adapters and orchestration. Interpretation remains in YAML-free model classes. */
public final class EvidenceMain {
    private EvidenceMain() {}
    public static void main(String[] args){System.exit(run(args,System.out,System.err));}
    private static Map<String,Object> runtime(Model.Context c,String file) {
        var snapshot=c.snapshots.read(file,64*1024*1024);
        try {
            Path executable=Path.of(ProcessHandle.current().info().command().orElseThrow());
            if(Files.size(executable)>64L*1024*1024||!snapshot.sha256().equals(Snapshots.hash(Files.readAllBytes(executable))))throw new IllegalArgumentException("runtime resource must match the executing native adapter");
        }catch(IOException e){throw new UncheckedIOException(e);}
        return Json.object("path",file,"sha256",snapshot.sha256());
    }
    private static Map<String,Object> adapter(Map<String,Object> runtime,boolean manual,String fault){return Json.object("name",manual?"synthetic-manual-inspection":"gcs-event-simulator","version","0.1","buildSha256",EvidenceBuild.SHA256,"runtimeSha256",runtime.get("sha256"),"faultModel",manual?"manual":fault);}
    private static Map<String,Object> procedureSelection(String file,Model.Context c) {
        var snapshot=c.snapshots.read(file);var entry=Json.object("scope","procedure","kind","procedure","format",Versions.PROCEDURE_ARTIFACT,"path",file,"sha256",snapshot.sha256());c.select(List.of(entry));return entry;
    }
    private static Map<String,Object> compile(String procedureFile,String rawFile,Model.Context c) {
        var selection=procedureSelection(procedureFile,c);var s=c.snapshots.read(rawFile);var raw=map(Snapshots.json(s));
        var resource=Json.object("path",rawFile,"sha256",s.sha256());var values=Json.object("run",raw,"resource",resource);var domain=new Evidence();domain.validate(values,c);
        var locations=new TreeMap<String,Object>();points(values,"",locations,Json.object("path",rawFile,"line",1,"column",1));
        var a=Json.object("artifactKind",domain.kind(),"format",domain.format(),"sourceContract",domain.source(),"compiler",Json.object("name","mundane-evidence","version",domain.version(),"contract",domain.contract()),"complete",true,"sources",List.of(resource),"locations",locations,"imports",List.of(selection),"values",values,"diagnostics",List.of());
        Model.envelope(a,domain);return a;
    }
    private static void points(Object value,String p,Map<String,Object> points,Map<String,Object> location) {
        points.put(p,location);if(value instanceof Map<?,?>)for(var e:map(value).entrySet())points(e.getValue(),mundane.json.Json.pointer(p,e.getKey()),points,location);
        else if(value instanceof List<?> l)for(int i=0;i<l.size();i++)points(l.get(i),p+"/"+i,points,location);
    }
    public static int run(String[] args,PrintStream out,PrintStream err) {
        return Cli.run(args,out,err,new Evidence(),o->{
            var c=Procedure.context(o.root());String first=o.inputs().get(0);Object result;
            switch(o.command()) {
                case "simulate" -> {
                    Cli.arity(o,3);var a=Model.read(o.root().resolve(first),c,new Procedure());var p=map(a.get("values"));var runtime=runtime(c,o.inputs().get(1));String fault=o.inputs().get(2);
                    var observations=Simulator.execute(p,c.reference(p.get("telemetryInterface"),"interface"),c.reference(p.get("commandInterface"),"interface"),c.reference(p.get("heartbeatInterface"),"interface"),fault);
                    result=Json.object("format",Versions.RUN_ARTIFACT,"id","RUN-"+p.get("id")+"-"+fault,"procedure",Json.object("id",p.get("id"),"sha256",c.snapshots.read(first).sha256()),"subjects",Evidence.subjects(a),"environment",p.get("environment"),"clock",p.get("clock"),"adapter",adapter(runtime,false,fault),"runtime",runtime,"manualSource",null,"execution","completed","observations",observations,"limitations",List.of("in-memory simulation only; no aircraft or physical hardware","acknowledgement does not establish aircraft execution","fault model: "+fault));
                }
                case "normalize-manual" -> {
                    Cli.arity(o,2);var s=c.snapshots.read(first,8*1024*1024);var d=map(Yaml.document(s.bytes(),8*1024*1024,true).value());Schema.validate(d,Json.read(ManualSchema.JSON.getBytes(StandardCharsets.UTF_8)));
                    var runtime=runtime(c,o.inputs().get(1));d.put("format",Versions.RUN_ARTIFACT);d.put("runtime",runtime);d.put("adapter",adapter(runtime,true,"manual"));d.put("manualSource",Json.object("path",first,"sha256",s.sha256()));result=d;
                }
                case "import" -> {Cli.arity(o,2);result=compile(first,o.inputs().get(1),c);}
                case "assess" -> {Cli.arity(o,1);result=Source.compile(first,null,c,new Assessment());}
                case "check","view" -> {Cli.arity(o,1);var a=Evidence.read(o.root().resolve(first),c);result=o.command().equals("view")?new Evidence().view(a,c):a;}
                case "analyze" -> {
                    Cli.arity(o,o.inputs().size());var target=Model.read(o.root().resolve(first),c,new Procedure());String targetPin=c.snapshots.read(first).sha256();
                    List<Map<String,Object>> runs=new ArrayList<>(),assessments=new ArrayList<>();Set<String> seen=new HashSet<>();
                    for(String file:o.inputs().subList(1,o.inputs().size())) {
                        var head=map(Snapshots.json(c.snapshots.read(file)));var child=c.child();
                        if(Versions.ASSESSMENT_ARTIFACT.equals(head.get("format"))){var a=Model.read(o.root().resolve(file),child,new Assessment());assessments.add(map(a.get("values")));}
                        else {
                            var a=Evidence.read(o.root().resolve(file),child);var d=map(a.get("values"));var run=map(d.get("run"));String pin=text(map(d.get("resource")).get("sha256"));
                            if(!seen.add(pin))continue;
                            var outcome=map(run.get("procedure")).get("sha256").equals(targetPin)?Evidence.evaluate(map(target.get("values")),run):Json.object("state","stale","criteria",List.of());
                            runs.add(Json.object("id",run.get("id"),"rawSha256",pin,"outcome",outcome));
                        }
                    }
                    var dispositions=new ArrayList<Map<String,Object>>();for(var assessment:assessments) {
                        var subject=map(assessment.get("run"));boolean matched=runs.stream().anyMatch(r->r.get("id").equals(subject.get("id"))&&r.get("rawSha256").equals(subject.get("sha256")));
                        dispositions.add(Json.object("assessment",assessment,"subjectState",matched?"matched":"unavailable-or-stale"));
                    }
                    boolean pass=!runs.isEmpty()&&runs.stream().allMatch(r->map(r.get("outcome")).get("state").equals("pass"));
                    var states=new HashSet<String>();for(var r:runs)states.add(text(map(r.get("outcome")).get("state")));
                    result=Json.object("format","mundane-evidence-analysis-0.1","procedure",Json.object("id",map(target.get("values")).get("id"),"sha256",targetPin),"requirements",map(target.get("values")).get("requirements"),"configuration",map(target.get("values")).get("configuration"),"runs",runs,"conflictingRuns",states.contains("pass")&&states.contains("fail"),"observedSupport",pass,"assessments",dispositions,"adequacyEstablished",false,"authorization","none");
                }
                default -> throw new IllegalArgumentException("unknown command");
            }
            c.snapshots.recheck();return result;
        });
    }
}
