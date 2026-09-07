package engineering.software;

import static engineering.artifacts.Checks.*;
import engineering.artifacts.*;
import engineering.domain.*;
import engineering.architecture.Architecture;
import engineering.configuration.Configuration;
import engineering.safety.Safety;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import mundanereq.Versions;

/** Project security relationships; native producers retain their facts and formats. */
public final class Software implements Model.Domain {
    public String kind(){return "software";}
    public String format(){return Versions.SOFTWARE_ARTIFACT;}
    public String source(){return Versions.SOFTWARE_SOURCE;}
    public String version(){return Versions.SOFTWARE_VERSION;}
    public String contract(){return Versions.SOFTWARE_CONTRACT;}
    public static Model.Context context(Path root) {
        return new Model.Context(root,Map.of("architecture",new Architecture(),"configuration",new Configuration(),
            "safety",new Safety(),"software",new Software()));
    }
    public Map<String,Object> lookup(Map<String,Object> values,String kind,String ident) {
        if(!kind.equals("software")||!ident.equals(values.get("id")))throw new IllegalArgumentException("missing software target");
        return values;
    }
    public void validate(Map<String,Object> values,Model.Context context){analyze(values,context);}
    public Map<String,Object> analyze(Map<String,Object> values,Model.Context context) {
        var authored=new TreeMap<>(values);authored.put("format",source());
        Schema.validate(authored,Json.read(SoftwareSchema.JSON.getBytes(StandardCharsets.UTF_8)));
        var baseline=context.reference(values.get("configuration"),"baseline");
        var build=map(values.get("build"));
        for(Object resource:build.values())context.readPinned(map(resource));
        var binary=map(build.get("binary"));
        String configScope=text(map(values.get("configuration")).get("scope"));
        var config=map(map(context.imports.get(configScope).get("values")).get("configuration"));
        Set<Object> selectedScopes=new HashSet<>();
        for(var selection:Model.rows(config,"selections"))selectedScopes.add(selection.get("memberScope"));
        if(Model.rows(baseline,"members").stream().noneMatch(m -> selectedScopes.contains(m.get("scope")) && binary.get("sha256").equals(m.get("sha256")) && binary.get("path").equals(m.get("path"))))
            throw new IllegalArgumentException("binary not selected in exact baseline");
        var provenance=Slsa.inspect(nativeJson(build.get("provenance"),context),build);
        var bom=CycloneDx.inspect(nativeJson(build.get("sbom"),context),text(binary.get("sha256")));
        var scan=map(values.get("scan"));String state=text(scan.get("state"));
        List<Map<String,Object>> findings=List.of();
        if(state.equals("complete")) {
            var scanBom=CycloneDx.inspect(nativeJson(scan.get("result"),context),text(binary.get("sha256")));
            if(!scanBom.components().equals(bom.components())||!scanBom.reachable().equals(bom.reachable()))
                throw new IllegalArgumentException("scan component revisions differ from selected BOM");
            findings=scanBom.vulnerabilities();
        } else if(scan.get("result")!=null)throw new IllegalArgumentException("uncompleted scan cannot supply successful result");
        Model.unique(Model.rows(values,"boundaries"));Model.unique(Model.rows(values,"threats"));
        Model.unique(Model.rows(values,"controls"));Model.unique(Model.rows(values,"reviews"));
        for(var boundary:Model.rows(values,"boundaries"))context.reference(boundary.get("interface"),"interface");
        for(var threat:Model.rows(values,"threats"))Model.find(Model.rows(values,"boundaries"),text(threat.get("boundary")));
        for(var control:Model.rows(values,"controls")) {
            Model.find(Model.rows(values,"threats"),text(control.get("threat")));
            context.reference(control.get("requirement"),"requirement");context.reference(control.get("safetyControl"),"control");
            var safety=context.imports.get(text(map(control.get("safetyControl")).get("scope")));
            String scope=text(map(map(map(safety.get("values")).get("context")).get("configuration")).get("scope"));
            if(Model.rows(safety,"imports").stream().noneMatch(i->scope.equals(i.get("scope")) &&
                    context.selections.get(configScope).get("sha256").equals(i.get("sha256"))))
                throw new IllegalArgumentException("safety control belongs to a different configuration revision");
        }
        var reviews=new ArrayList<Map<String,Object>>();
        for(var review:Model.rows(values,"reviews")) {
            boolean current=review.get("sbomSha256").equals(map(build.get("sbom")).get("sha256")) && state.equals("complete") &&
                review.get("scanSha256").equals(map(scan.get("result")).get("sha256")) &&
                review.get("configurationSha256").equals(context.selections.get(configScope).get("sha256"));
            boolean found=findings.stream().anyMatch(f->f.get("advisory").equals(review.get("advisory")));
            reviews.add(Json.object("id",review.get("id"),"state",current&&found?"current-authored-review":"stale-or-unavailable-review"));
        }
        return Json.object("format","mundane-software-analysis-0.1","configuration",config.get("id"),"stage",config.get("stage"),
            "baseline",baseline.get("id"),"build",provenance,"nativeResources",build,"scanner",Json.object("tool",scan.get("tool"),"version",scan.get("version")),"scanState",state,"findings",findings,"reviews",reviews,
            "controls",values.get("controls"),"authorization","none");
    }
    private static Map<String,Object> nativeJson(Object resource,Model.Context context) {
        return map(Snapshots.json(context.readPinned(map(resource))));
    }
    public String view(Map<String,Object> artifact,Model.Context context) {
        var result=analyze(map(artifact.get("values")),context);
        return "# Software provenance and security\n\n"+Model.sourceLink(artifact,context,"/id",text(map(artifact.get("values")).get("id")))+
            "\n\n```json\n"+Json.write(result)+"\n```\n\nUnsigned native records and authored judgments; no release authorization.\n";
    }
}
