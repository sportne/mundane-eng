package engineering.impact;

import static engineering.artifacts.Checks.*;
import static engineering.artifacts.Json.object;
import engineering.artifacts.Artifacts;
import engineering.artifacts.Json;
import engineering.artifacts.Problem;
import engineering.work.WorkArtifact;
import engineering.work.WorkGraph;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** The versioned impact policy over serialized domain facts; no source readers. */
public final class ImpactGraph {
    public record Graph(List<Object> nodes, List<Object> edges) {}
    private final Map<String,Object> nodes = new TreeMap<>();
    private final Map<String,Object> edges = new TreeMap<>();
    private ImpactGraph() {}

    public static Graph build(List<?> imports) {
        if (imports.isEmpty() || imports.size() > 100)
            throw new Problem("impact-limit", "expected 1..100 imports", "imports");
        ImpactGraph graph = new ImpactGraph();
        Map<String,Map<String,Object>> artifacts = new TreeMap<>();
        Map<String,List<String>> dependencies = new TreeMap<>();
        Map<String,Map<String,Object>> locations = new TreeMap<>();
        List<Object> workImports = new ArrayList<>();
        Set<String> requirements = new java.util.TreeSet<>();
        for (Object value : imports) {
            var entry = map(value);
            keys(entry, "scope", "path", "sha256", "artifact", "dependsOn");
            String scope = id(entry.get("scope")), file = path(entry.get("path"));
            digest(entry.get("sha256"));
            var artifact = map(entry.get("artifact"));
            if (artifacts.put(scope, artifact) != null)
                throw new Problem("duplicate-scope", "duplicate scope " + scope, file);
            var deps = list(entry.get("dependsOn")).stream().map(x -> id(x)).toList();
            if (new HashSet<>(deps).size() != deps.size())
                throw new IllegalArgumentException("duplicate build dependency");
            dependencies.put(scope, deps);
            locations.put(scope, object("path", file, "line", 1, "column", 1));
            workImports.add(object("scope", scope, "path", file, "sha256", entry.get("sha256"), "artifact", artifact));
            switch (text(artifact.get("artifactKind"))) {
                case "requirements" -> {
                    requirements.add(scope);
                    for (var r : Artifacts.requirements(artifact, file).entrySet()) {
                        var span = map(map(r.getValue().get("locations")).get("record"));
                        graph.node(scope, "requirement", r.getKey(), start(span));
                    }
                }
                case "verification-plan" -> {
                    Artifacts.plan(artifact, file);
                    for (Object p : list(artifact.get("plans"))) {
                        var r = map(p);
                        graph.node(scope, "verification-plan", id(r.get("id")), map(r.get("location")));
                    }
                    for (Object p : list(artifact.get("activities"))) {
                        var r = map(p);
                        graph.node(scope, "verification-activity", id(r.get("id")), map(r.get("location")));
                    }
                }
                case "work-items" -> {
                    for (var r : WorkArtifact.validate(artifact, file).entrySet())
                        graph.node(scope, "work-item", r.getKey(), map(r.getValue().get("location")));
                }
                default -> throw new Problem("wrong-kind", "unsupported artifact kind", file);
            }
        }
        WorkGraph.acyclic(dependencies, "build-cycle", locations);
        // Reuse domain validity rules even for relationships excluded from impact.
        var work = WorkGraph.evaluateImports(workImports, ignored -> {});
        for (Object value : work.edges()) {
            var e = map(value);
            String relation = text(e.get("relation"));
            if (!Set.of("addresses", "depends-on").contains(relation)) continue;
            String from = text(e.get("from")), scope = from.substring(0, from.indexOf(':'));
            var loc = map(e.get("location"));
            String qualified = text(loc.get("path"));
            graph.edge(text(e.get("to")), from, relation, null, scope,
                    object("path", qualified.substring(scope.length() + 1), "line", loc.get("line"), "column", loc.get("column")));
        }
        for (var entry : artifacts.entrySet()) {
            String scope = entry.getKey(); var artifact = entry.getValue();
            if (requirements.contains(scope)) {
                for (Object value : list(artifact.get("requirements"))) {
                    var r = map(value); var v = map(r.get("values"));
                    var refs = map(map(r.get("locations")).get("references"));
                    for (Object parent : list(v.get("decomposes")))
                        graph.edge(key(scope, "requirement", text(parent)), key(scope, "requirement", text(v.get("id"))),
                                "decomposes", null, scope, start(map(refs.get(text(parent)))));
                }
            } else if ("verification-plan".equals(artifact.get("artifactKind"))) {
                Map<String,Map<String,Object>> plans = new TreeMap<>();
                for (Object value : list(artifact.get("plans"))) {
                    var p = map(value); plans.put(id(p.get("id")), p);
                    resolve(p.get("baselineScope"), requirements, qualified(scope, map(p.get("location"))));
                    resolve(p.get("currentScope"), requirements, qualified(scope, map(p.get("location"))));
                }
                for (Object value : list(artifact.get("coverage"))) {
                    var row = map(value); var p = plans.get(text(row.get("planId")));
                    var loc = map(row.get("location"));
                    String plan = key(scope, "verification-plan", text(row.get("planId")));
                    String activity = key(scope, "verification-activity", text(row.get("activityId")));
                    for (String revision : List.of("baseline", "current")) {
                        String reqScope = resolve(p.get(revision + "Scope"), requirements, qualified(scope, loc));
                        graph.edge(key(reqScope, "requirement", text(row.get("requirementId"))), activity,
                                "coverage-" + revision, plan, scope, loc);
                    }
                    graph.edge(activity, plan, "activity-plan", plan, scope, loc);
                }
            }
        }
        return new Graph(new ArrayList<>(graph.nodes.values()), new ArrayList<>(graph.edges.values()));
    }

    private static String resolve(Object selected, Set<String> requirements, Map<String,Object> location) {
        if (selected == null) {
            if (requirements.size() != 1) throw new Problem("ambiguous-scope", "expected exactly one requirement scope", location);
            return requirements.iterator().next();
        }
        String scope = id(selected);
        if (!requirements.contains(scope)) throw new Problem("missing-scope", "unknown requirement scope " + scope, location);
        return scope;
    }
    public static String key(String scope, String kind, String id) { return scope + ":" + kind + ":" + id; }
    private static Map<String,Object> start(Map<String,Object> span) {
        var start = map(span.get("start"));
        return object("path", span.get("path"), "line", start.get("line"), "column", start.get("column"));
    }
    private static Map<String,Object> qualified(String scope, Map<String,Object> location) {
        return object("scope", scope, "path", location.get("path"), "line", location.get("line"), "column", location.get("column"));
    }
    private void node(String scope, String kind, String id, Map<String,Object> location) {
        String key = key(scope, kind, id);
        if (nodes.put(key, object("key", key, "scope", scope, "kind", kind, "id", id, "location", qualified(scope, location))) != null)
            throw new IllegalArgumentException("duplicate node " + key);
        if (nodes.size() > 10000) throw new Problem("impact-limit", "graph exceeds 10000 nodes", location);
    }
    private void edge(String from, String to, String relation, String context, String scope, Map<String,Object> location) {
        if (!nodes.containsKey(from) || !nodes.containsKey(to))
            throw new Problem("missing-target", "missing impact endpoint " + from + " -> " + to, qualified(scope, location));
        var edge = object("from", from, "to", to, "relation", relation, "context", context, "location", qualified(scope, location));
        edges.put(Json.write(edge), edge);
        if (edges.size() > 100000) throw new Problem("impact-limit", "graph exceeds 100000 edges", location);
    }
}
