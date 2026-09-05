package engineering.impact;

import static engineering.artifacts.Json.object;
import engineering.artifacts.Json;
import java.util.List;
import java.util.Map;

/** Small serialized fixtures; compiler-to-consumer coverage lives in the workflow suite. */
final class ImpactFixtures {
    private ImpactFixtures() {}
    static Map<String,Object> location(String file) { return object("path", file, "line", 2, "column", 1); }
    static Map<String,Object> snapshot(String file) { return object("path", file, "sha256", "0".repeat(64)); }
    static Map<String,Object> envelope(String kind, String format, String source, String file) {
        return object("artifactKind", kind, "format", format, "sourceContract", source,
                "compiler", object("name", "fixture", "version", "test", "contract", "test"),
                "complete", true, "sources", List.of(snapshot(file)), "diagnostics", List.of());
    }
    static Map<String,Object> requirement(String id, String... parents) {
        var span = object("path", "req.yaml", "start", object("line", 2, "column", 1), "end", object("line", 2, "column", 9));
        var fields = object("id", List.of(span), "title", List.of(span), "statement", List.of(span));
        Map<String,Object> refs = new java.util.TreeMap<>();
        for (String parent : parents) refs.put(parent, span);
        if (parents.length > 0) fields.put("decomposes", List.of(span));
        return object("values", object("id", id, "title", id, "allocation", null, "source", null,
                "rationale", null, "statement", List.of(object("kind", "prose", "text", "The unit shall retain records.")), "decomposes", List.of(parents)),
                "locations", object("record", span, "fields", fields, "references", refs));
    }
    static Map<String,Object> requirements(boolean cycle) {
        var a = envelope("requirements", "mundanereq-requirements-0.1", "mundanereq-yaml-0.3", "req.yaml");
        a.put("requirements", List.of(cycle ? requirement("TOP", "LOW") : requirement("TOP"), requirement("LOW", "TOP")));
        return a;
    }
    static Map<String,Object> plan() {
        var a = envelope("verification-plan", "mundane-plan-0.1", "mundane-plan-source-0.1", "plan.tsv");
        a.put("plans", List.of(object("id", "PLAN", "context", "device", "baselineScope", "baseline", "currentScope", "req", "location", location("plan.tsv"))));
        a.put("activities", List.of(object("id", "TEST", "method", "test", "objective", "Check records", "expectedEvidence", "Observations", "location", location("plan.tsv"))));
        a.put("coverage", List.of(object("planId", "PLAN", "activityId", "TEST", "requirementId", "LOW", "location", location("plan.tsv"))));
        return a;
    }
    static Map<String,Object> item(String id, List<String> deps, List<Object> relations) {
        return object("values", object("id", id, "kind", "task", "title", id, "status", "Planned", "dependencies", deps, "relations", relations,
                "planning", object("stage", "", "type", "", "condition", "", "unlocks", "", "statusNote", ""), "body", "Review the change."),
                "location", location(id + ".yaml"), "metadataLocation", location(id + ".yaml"));
    }
    static Map<String,Object> work() {
        var a = envelope("work-items", "mundane-work-items-0.2", "mundane-work-yaml-0.2", "FIX.yaml");
        a.put("selection", snapshot("set.json"));
        a.put("sources", List.of(snapshot("FIX.yaml"), snapshot("FOLLOW.yaml"), snapshot("NOTE.yaml")));
        a.put("items", List.of(item("FIX", List.of(), List.of(object("relation", "addresses", "scope", "plan", "kind", "verification-activity", "target", "TEST"))),
                item("FOLLOW", List.of("FIX"), List.of()),
                item("NOTE", List.of(), List.of(object("relation", "relates-to", "scope", "req", "kind", "requirement", "target", "TOP")))));
        return a;
    }
    static Map<String,Object> imported(String scope, Map<String,Object> artifact) {
        return object("scope", scope, "path", scope + ".json", "sha256", engineering.artifacts.Snapshots.hash(Json.bytes(artifact)), "dependsOn", List.of(), "artifact", artifact);
    }
    static List<Object> imports() {
        return List.of(imported("req", requirements(false)), imported("baseline", requirements(false)), imported("plan", plan()), imported("work", work()));
    }
}
