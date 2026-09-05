package engineering.impact;

import static engineering.artifacts.Checks.*;
import static engineering.artifacts.Json.object;
import engineering.artifacts.Problem;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Bounded breadth-first reachability with one deterministic shortest explanation. */
public final class ImpactQuery {
    private ImpactQuery() {}
    public static Map<String,Object> query(ImpactGraph.Graph graph, String from, int depth) {
        if (depth < 1 || depth > 64) throw new IllegalArgumentException("depth must be 1..64");
        Map<String,List<Map<String,Object>>> outgoing = new TreeMap<>();
        for (Object value : graph.nodes()) outgoing.put(text(map(value).get("key")), new ArrayList<>());
        if (!outgoing.containsKey(from)) throw new Problem("unknown-impact-node", "unknown selected node " + from, "query");
        for (Object value : graph.edges()) {
            var edge = map(value); outgoing.get(text(edge.get("from"))).add(edge);
        }
        var queue = new ArrayDeque<String>(); queue.add(from);
        Map<String,Integer> distances = new HashMap<>(); distances.put(from, 0);
        Map<String,Map<String,Object>> predecessor = new TreeMap<>();
        while (!queue.isEmpty()) {
            String node = queue.removeFirst(); int distance = distances.get(node);
            if (distance == depth) continue;
            for (var edge : outgoing.get(node)) {
                String to = text(edge.get("to"));
                if (distances.putIfAbsent(to, distance + 1) == null) {
                    predecessor.put(to, edge); queue.addLast(to);
                }
            }
        }
        boolean truncated = false;
        for (String node : distances.keySet())
            for (var edge : outgoing.get(node))
                if (!distances.containsKey(text(edge.get("to")))) truncated = true;
        List<Object> affected = new ArrayList<>();
        for (String node : predecessor.keySet()) {
            List<Object> path = new ArrayList<>(); String step = node;
            while (!step.equals(from)) {
                var edge = predecessor.get(step); path.add(edge); step = text(edge.get("from"));
            }
            java.util.Collections.reverse(path);
            affected.add(object("node", node, "path", path));
        }
        return object("from", from, "depth", depth, "truncated", truncated, "affected", affected);
    }
}
