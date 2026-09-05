package engineering.impact;

import static engineering.artifacts.Checks.*;
import engineering.artifacts.Json;
import engineering.artifacts.Problem;
import engineering.work.WorkArtifact;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import mundanereq.Versions;

/** Disposable root-relative Markdown; rechecks derived claims without reading source. */
public final class ImpactView {
    private ImpactView() {}
    public static String render(Map<String,Object> output) {
        try {
            keys(output, "format", "complete", "analyzer", "selection", "imports", "nodes", "edges", "query", "diagnostics");
            if (!Versions.IMPACT_ARTIFACT.equals(output.get("format")) || !Boolean.TRUE.equals(output.get("complete")) || !list(output.get("diagnostics")).isEmpty())
                throw new IllegalArgumentException("unsupported or incomplete impact analysis");
            var analyzer = map(output.get("analyzer")); keys(analyzer, "name", "version", "contract");
            if (!"mundane-impact".equals(analyzer.get("name")) || !Versions.IMPACT_CONTRACT.equals(analyzer.get("contract")))
                throw new IllegalArgumentException("unsupported impact producer contract");
            text(analyzer.get("version")); WorkArtifact.snapshot(output.get("selection"));
            var imports = list(output.get("imports"));
            var graph = ImpactGraph.build(imports);
            var query = map(output.get("query")); keys(query, "from", "depth", "truncated", "affected");
            var expected = ImpactQuery.query(graph, text(query.get("from")), integer(query.get("depth")));
            if (!same(graph.nodes(), output.get("nodes")) || !same(graph.edges(), output.get("edges")) || !same(expected, query))
                throw new IllegalArgumentException("graph or query disagrees with embedded artifacts");
            Map<String,Map<String,Object>> nodes = new TreeMap<>();
            for (Object value : graph.nodes()) { var node = map(value); nodes.put(text(node.get("key")), node); }
            String seed = text(query.get("from"));
            StringBuilder b = new StringBuilder("# Derived impact report\n\nGenerated from explicit compiled snapshots. Do not edit this report; regenerate it\nfrom authoritative source. Results identify possible review consequences, not\ninvalidity, completion, execution, approval or satisfaction. Authored status is unchanged.\nSource links are relative to the analysis root and may display newer source than\nthe recorded snapshot.\n\n");
            b.append("Starting node: ").append(nodeLink(nodes.get(seed))).append(".\n\n");
            b.append("Depth bound: ").append(query.get("depth")).append(". One shortest explanatory path per affected node.\n\n");
            if (Boolean.TRUE.equals(query.get("truncated")))
                b.append("**TRUNCATED: additional selected nodes lie beyond the depth bound.**\n\n");
            var affected = list(query.get("affected"));
            b.append("Affected nodes within this selection and policy: ").append(affected.size()).append(".\n\n");
            if (affected.isEmpty()) b.append("No selected node is reachable under this policy; effects outside this selection are unknown.\n\n");
            for (Object value : affected) {
                var result = map(value);
                b.append("## ").append(escape(text(result.get("node")))).append("\n\n");
                b.append("Review candidate: ").append(nodeLink(nodes.get(text(result.get("node"))))).append(".\n\n");
                for (Object step : list(result.get("path"))) {
                    var edge = map(step);
                    b.append("- ").append(nodeLink(nodes.get(text(edge.get("from"))))).append(" → ")
                            .append(nodeLink(nodes.get(text(edge.get("to"))))).append(" via ").append(escape(text(edge.get("relation"))))
                            .append("; ").append(link("declaration", map(edge.get("location"))));
                    if (edge.get("context") != null) b.append("; context ").append(nodeLink(nodes.get(text(edge.get("context")))));
                    b.append(".\n");
                    if (b.length() > ImpactAnalyzer.OUTPUT_LIMIT) throw new Problem("impact-limit", "report exceeds 16 MiB", "analysis");
                }
                b.append('\n');
            }
            b.append("## Selected revisions\n\nThese digests record exact input bytes; human-authored IDs retain identity.\nProvenance is not a signature or evidence of approval.\n\n");
            var selection = map(output.get("selection"));
            b.append("- Import selection: ").append(escape(text(selection.get("path")))).append("; SHA-256 ").append(selection.get("sha256")).append("\n");
            var sorted = new ArrayList<>(imports); sorted.sort(Comparator.comparing(x -> text(map(x).get("scope"))));
            for (Object value : sorted) {
                var entry = map(value);
                b.append("- Scope ").append(escape(text(entry.get("scope")))).append(": ").append(escape(text(entry.get("path"))))
                        .append("; SHA-256 ").append(entry.get("sha256")).append("\n");
            }
            String report = b.toString();
            if (report.getBytes(StandardCharsets.UTF_8).length > ImpactAnalyzer.OUTPUT_LIMIT)
                throw new Problem("impact-limit", "report exceeds 16 MiB", "analysis");
            return report;
        } catch (IllegalArgumentException e) {
            throw new Problem("invalid-impact-analysis", e.getMessage(), "analysis");
        }
    }
    private static boolean same(Object a, Object b) { return Json.write(a).equals(Json.write(b)); }
    private static String nodeLink(Map<String,Object> node) { return link(text(node.get("key")), map(node.get("location"))); }
    private static String link(String label, Map<String,Object> location) {
        StringBuilder uri = new StringBuilder("./");
        for (byte value : text(location.get("path")).getBytes(StandardCharsets.UTF_8)) {
            int c = value & 255;
            if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || "/._-".indexOf(c) >= 0) uri.append((char)c);
            else uri.append(String.format(Locale.ROOT, "%%%02X", c));
        }
        return "[" + escape(label) + "](" + uri + "#L" + integer(location.get("line")) + ")";
    }
    private static String escape(String text) {
        StringBuilder b = new StringBuilder();
        text.codePoints().forEach(c -> {
            if (c < 32 || "&<>|[]`*_\\\"'".indexOf(c) >= 0) b.append("&#").append(c).append(';');
            else b.appendCodePoint(c);
        });
        return b.toString();
    }
}
