package engineering.impact;

import static engineering.artifacts.Checks.*;
import static engineering.impact.ImpactGraphTest.check;
import engineering.artifacts.Json;
import engineering.artifacts.Problem;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class ImpactViewTest {
    private ImpactViewTest() {}
    public static void main(String[] args) throws Exception { exercise(args.length == 0 ? null : Path.of(args[0]).toAbsolutePath().toString()); }
    public static void run() throws Exception { exercise(null); }
    private static void exercise(String binary) throws Exception {
        Path root = Files.createTempDirectory("impact-view-");
        try {
            ImpactCliTest.writeInputs(root);
            var result = ImpactAnalyzer.analyze(root, "imports.json", "req:requirement:TOP", 8);
            check(result.status() == 0, "query fixture failed");
            var output = map(Json.read(Json.bytes(result.output())));
            String report = ImpactView.render(output);
            check(report.equals(ImpactView.render(output)), "report nondeterministic");
            check(report.contains("Derived impact report") && report.contains("SHA-256") && report.contains("./req.yaml#L2") && report.contains("./plan.yaml#L2") && report.contains("coverage-current") && report.contains("depends-on"), "report missing paths or provenance");
            Files.write(root.resolve("query.json"), Json.bytes(output));
            String[] args = {"view", "--root", root.toString(), root.resolve("query.json").toString()};
            check(new String(ImpactCliTest.invoke(args, 0, binary), StandardCharsets.UTF_8).equals(report), "CLI report differs");
            var bounded = ImpactAnalyzer.analyze(root, "imports.json", "req:requirement:TOP", 1).output();
            check(ImpactView.render(bounded).contains("**TRUNCATED:"), "truncation hidden");
            var empty = ImpactAnalyzer.analyze(root, "imports.json", "work:work-item:NOTE", 8).output();
            check(ImpactView.render(empty).contains("No selected node is reachable"), "empty query unexplained");
            for (String field : List.of("nodes", "edges", "query", "complete", "format")) {
                var bad = map(Json.read(Json.bytes(output)));
                bad.put(field, switch (field) { case "nodes", "edges" -> List.of(); case "query" -> Map.of(); case "complete" -> false; default -> "future"; });
                reject(bad);
                Files.write(root.resolve("query.json"), Json.bytes(bad));
                check(ImpactCliTest.invoke(args, 1, binary).length == 0, "failed report emitted bytes");
            }
            var bad = map(Json.read(Json.bytes(output))); var query = map(bad.get("query"));
            query.put("truncated", 0); bad.put("query", query); reject(bad);
            query.put("truncated", false); query.put("affected", List.of()); reject(bad);
            // A valid source path may contain Markdown punctuation, spaces, Unicode or a URI colon.
            var unusual = map(Json.read(Json.bytes(output).clone()));
            var imported = new java.util.ArrayList<Object>();
            for (Object value : list(unusual.get("imports"))) {
                var e = map(value);
                if (e.get("scope").equals("req")) {
                    e.put("artifact", Json.read(Json.write(e.get("artifact")).replace("req.yaml", "<script>[x] (é):req.yaml").getBytes(StandardCharsets.UTF_8)));
                }
                imported.add(e);
            }
            var graph = ImpactGraph.build(imported); unusual.put("imports", imported); unusual.put("nodes", graph.nodes()); unusual.put("edges", graph.edges());
            unusual.put("query", ImpactQuery.query(graph, "req:requirement:TOP", 8));
            String escaped = ImpactView.render(unusual);
            check(!escaped.contains("<script>") && escaped.contains("./%3Cscript%3E%5Bx%5D%20%28%C3%A9%29%3Areq.yaml#L2"), "unsafe source URL");
            Files.write(root.resolve("query.json"), Json.bytes(output));
            for (int limit : List.of(0, 50)) {
                PrintStream broken = new PrintStream(new OutputStream() {
                    int left = limit;
                    @Override public void write(int value) throws IOException { if (left-- <= 0) throw new IOException("closed output"); }
                });
                check(ImpactMain.run(args, broken, new PrintStream(new ByteArrayOutputStream())) == 2, "report output failure accepted");
            }
            Files.writeString(root.resolve("query.json"), "[]"); check(ImpactCliTest.invoke(args, 1, binary).length == 0, "nonobject report accepted");
        } finally {
            try (var paths = Files.walk(root)) { for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path); }
        }
        System.out.println("PASS impact reports: recomputation, forged results, truncation, empty results, safe links and output failures" + (binary == null ? "" : "; JVM/native parity"));
    }
    private static void reject(Map<String,Object> value) {
        try { ImpactView.render(value); throw new AssertionError("forged report accepted"); }
        catch (Problem expected) { check(!expected.operational(), "unexpected operational error"); }
    }
}
