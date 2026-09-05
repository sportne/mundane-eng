package engineering.impact;

import static engineering.artifacts.Checks.*;
import static engineering.artifacts.Json.object;
import engineering.artifacts.Problem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ImpactGraphTest {
    private ImpactGraphTest() {}
    public static void run() throws Exception {
        var imports = ImpactFixtures.imports();
        var graph = ImpactGraph.build(imports);
        check(graph.nodes().size() == 9 && graph.edges().size() == 7, "expected scoped graph");
        edge(graph, "req:requirement:TOP", "req:requirement:LOW", "decomposes");
        edge(graph, "baseline:requirement:LOW", "plan:verification-activity:TEST", "coverage-baseline");
        edge(graph, "req:requirement:LOW", "plan:verification-activity:TEST", "coverage-current");
        edge(graph, "plan:verification-activity:TEST", "work:work-item:FIX", "addresses");
        edge(graph, "work:work-item:FIX", "work:work-item:FOLLOW", "depends-on");
        check(graph.edges().stream().noneMatch(x -> map(x).get("to").equals("work:work-item:NOTE")), "generic link must not propagate");
        var shuffled = new ArrayList<>(imports); Collections.reverse(shuffled);
        check(graph.equals(ImpactGraph.build(shuffled)), "manifest order changed graph");
        var cycles = List.of(ImpactFixtures.imported("req", ImpactFixtures.requirements(true)));
        check(ImpactGraph.build(cycles).edges().size() == 2, "decomposition cycle rejected");
        reject(() -> ImpactGraph.build(List.of(imports.get(0), imports.get(0))), "duplicate-scope");
        reject(() -> ImpactGraph.build(List.of(imports.get(2))), "missing-scope");
        reject(() -> ImpactGraph.build(List.of(imports.get(0), imports.get(3))), "wrong-kind");
        var cycle = map(imports.get(0)); cycle.put("dependsOn", List.of("req"));
        reject(() -> ImpactGraph.build(List.of(cycle)), "build-cycle");
        cycle.put("dependsOn", List.of("absent"));
        reject(() -> ImpactGraph.build(List.of(cycle)), "missing-dependency");
        var bad = ImpactFixtures.plan();
        bad.put("plans", List.of(object("id", "PLAN", "context", "device", "baselineScope", null, "currentScope", null, "location", ImpactFixtures.location("plan.tsv"))));
        reject(() -> ImpactGraph.build(List.of(imports.get(0), imports.get(1), ImpactFixtures.imported("plan", bad))), "ambiguous-scope");
        check(ImpactGraph.build(List.of(imports.get(0), ImpactFixtures.imported("plan", bad))).nodes().size() == 4, "single scope resolution failed");
        bad.put("coverage", List.of(object("planId", "PLAN", "activityId", "TEST", "requirementId", "MISSING", "location", ImpactFixtures.location("plan.tsv"))));
        reject(() -> ImpactGraph.build(List.of(imports.get(0), ImpactFixtures.imported("plan", bad))), "missing-target");
        var root = java.nio.file.Files.createTempDirectory("impact-inputs-");
        try {
            var a = ImpactFixtures.requirements(false);
            var bytes = engineering.artifacts.Json.bytes(a);
            java.nio.file.Files.write(root.resolve("req.json"), bytes);
            var entry = object("scope", "req", "kind", "requirements", "path", "req.json", "sha256", engineering.artifacts.Snapshots.hash(bytes), "dependsOn", List.of());
            var manifest = object("format", "mundane-imports-0.1", "imports", List.of(entry));
            java.nio.file.Files.write(root.resolve("imports.json"), engineering.artifacts.Json.bytes(manifest));
            var reads = new engineering.artifacts.Snapshots(root);
            check(ImpactGraph.build(ImpactInputs.read(reads, "imports.json").imports()).nodes().size() == 2, "pinned import failed");
            reads.recheck();
            java.nio.file.Files.writeString(root.resolve("req.json"), "{}\n");
            reject(() -> ImpactInputs.read(new engineering.artifacts.Snapshots(root), "imports.json"), "digest-mismatch");
            reject(reads::recheck, "input-changed");
        } finally {
            try (var paths = java.nio.file.Files.walk(root)) {
                for (var p : paths.sorted(java.util.Comparator.reverseOrder()).toList()) java.nio.file.Files.delete(p);
            }
        }
        System.out.println("PASS impact graph directions, scope isolation, declarations, ordering and invalid selections");
    }
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    static void reject(Runnable action, String code) {
        try { action.run(); throw new AssertionError("accepted " + code); }
        catch (Problem p) { check(p.code.equals(code), "expected " + code + ", got " + p.code); }
    }
    private static void edge(ImpactGraph.Graph graph, String from, String to, String role) {
        var e = graph.edges().stream().map(x -> map(x)).filter(x -> x.get("from").equals(from) && x.get("to").equals(to) && x.get("relation").equals(role)).findFirst().orElseThrow();
        var loc = map(e.get("location"));
        check(integer(loc.get("line")) == 2 && !text(loc.get("path")).contains(":"), "lost declaration coordinates");
    }
}
