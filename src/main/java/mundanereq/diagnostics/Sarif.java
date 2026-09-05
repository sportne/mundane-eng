package mundanereq.diagnostics;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import mundanereq.Interpreter;
import mundanereq.SourceFormat;
import mundanereq.Versions;
import mundanereq.compile.SemanticArtifact;
import mundanereq.output.JsonOutput;
import static mundanereq.output.JsonOutput.object;

/** Source-accurate point diagnostics; no speculative token spans or source rereads. */
public final class Sarif {
    private Sarif() {}
    public static byte[] emit(Path root, List<Interpreter.Source> sources, Interpreter.Result result,
            SourceFormat format, int status) {
        var diagnostics = result.diagnostics().stream().sorted(Comparator.comparing(Interpreter.Diagnostic::file)
                .thenComparingInt(Interpreter.Diagnostic::line).thenComparingInt(Interpreter.Diagnostic::column)
                .thenComparing(Interpreter.Diagnostic::code).thenComparing(Interpreter.Diagnostic::message)).toList();
        List<String> rules = diagnostics.stream().map(Interpreter.Diagnostic::code).distinct().sorted().toList();
        List<Object> results = new ArrayList<>();
        for (var diagnostic : diagnostics) {
            Map<String,Object> physical = object("artifactLocation", object("uri", relativeUri(root,diagnostic.file())));
            if (!SemanticArtifact.operational(diagnostic)) physical.put("region", object("startLine", diagnostic.line(),
                    "startColumn", SemanticArtifact.diagnosticColumn(diagnostic,sources)));
            results.add(object("ruleId", diagnostic.code(), "ruleIndex", rules.indexOf(diagnostic.code()),
                    "level", "error", "message", object("text", diagnostic.message().replace(root.toString(), ".")),
                    "locations", List.of(object("physicalLocation",physical))));
        }
        Map<String,Object> invocation = object("executionSuccessful", result.syntaxComplete() && status!=2, "exitCode",status);
        if (!result.syntaxComplete()) invocation.put("toolExecutionNotifications", List.of(object("level","error",
                "descriptor",object("id","incomplete-interpretation"),"message",object("text",
                        "Source interpretation is incomplete; absent-target checks may be omitted."))));
        Map<String,Object> run = object("tool",object("driver",object("name","mundanereq-validate","version",Versions.VALIDATE_VERSION,
                        "rules",rules.stream().map(id->object("id",id,"defaultConfiguration",object("level","error"))).toList())),
                "columnKind","unicodeCodePoints", "results", results, "invocations", List.of(invocation),
                "properties",object("sourceContract",format.contract,"commandContract",format==SourceFormat.YAML_04?Versions.VALIDATE_ATTRIBUTE_CONTRACT:Versions.VALIDATE_CONTRACT,
                        "sourceSetValid",result.valid(),"syntaxComplete",result.syntaxComplete()));
        return JsonOutput.encode(object("$schema","https://docs.oasis-open.org/sarif/sarif/v2.1.0/errata01/os/schemas/sarif-schema-2.1.0.json",
                "version",Versions.SARIF_VERSION,"runs",List.of(run)));
    }
    private static String relativeUri(Path root,String file) {
        Path path=Path.of(file).toAbsolutePath().normalize();
        if (!path.startsWith(root)) throw new IllegalArgumentException("diagnostic path is outside --root");
        String relative=root.relativize(path).toString().replace(java.io.File.separatorChar,'/');
        if (relative.isEmpty()) return "./";
        try {
            String uri=new URI(null,null,"/"+relative,null).toASCIIString().substring(1);
            return uri.split("/",2)[0].contains(":") ? "./"+uri : uri;
        } catch (URISyntaxException failure) { throw new IllegalArgumentException("cannot encode source path",failure); }
    }
}
