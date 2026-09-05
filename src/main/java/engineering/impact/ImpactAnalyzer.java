package engineering.impact;

import static engineering.artifacts.Json.object;
import engineering.artifacts.Json;
import engineering.artifacts.Problem;
import engineering.artifacts.Snapshots;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import mundanereq.Versions;

public final class ImpactAnalyzer {
    public static final int OUTPUT_LIMIT = 16 * 1024 * 1024;
    public record Result(Map<String,Object> output, int status) {}
    private ImpactAnalyzer() {}
    public static Result analyze(Path root, String manifest, String from, int depth) {
        return analyze(root, manifest, from, depth, () -> {});
    }
    public static Result analyze(Path root, String manifest, String from, int depth, Runnable beforeRecheck) {
        Map<String,Object> output = object("format", Versions.IMPACT_ARTIFACT, "complete", false,
                "analyzer", object("name", "mundane-impact", "version", Versions.IMPACT_VERSION, "contract", Versions.IMPACT_CONTRACT),
                "selection", null, "imports", List.of(), "nodes", List.of(), "edges", List.of(), "query", null, "diagnostics", List.of());
        try {
            var reads = new Snapshots(root);
            var inputs = ImpactInputs.read(reads, manifest);
            output.put("selection", inputs.manifest());
            var graph = ImpactGraph.build(inputs.imports());
            var query = ImpactQuery.query(graph, from, depth);
            output.put("imports", inputs.imports()); output.put("nodes", graph.nodes()); output.put("edges", graph.edges());
            output.put("query", query); output.put("complete", true);
            if (Json.bytes(output).length > OUTPUT_LIMIT) throw new Problem("impact-limit", "output exceeds 16 MiB", manifest);
            beforeRecheck.run(); reads.recheck();
            return new Result(output, 0);
        } catch (Problem p) {
            return failure(output, p);
        } catch (IllegalArgumentException e) {
            return failure(output, new Problem("invalid-impact-input", e.getMessage(), manifest));
        }
    }
    private static Result failure(Map<String,Object> output, Problem p) {
        output.put("complete", false); output.put("imports", List.of()); output.put("nodes", List.of());
        output.put("edges", List.of()); output.put("query", null); output.put("diagnostics", List.of(p.diagnostic()));
        return new Result(output, p.operational() ? 2 : 1);
    }
}
