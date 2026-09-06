package mundanereq.editor;

import java.util.List;
import java.util.Map;
import mundane.json.Json;

/** Snapshot boundary behavior, independent of the editor UI. */
public final class EditorBridgeTest {
    private EditorBridgeTest() {}
    public static void run() {
        var request = request();
        assert EditorMain.analyze(request).get("protocol").equals(EditorMain.PROTOCOL);
        reject(Json.object("protocol", "unknown"));
        for (String field : List.of("protocol", "source", "files")) {
            var changed = new java.util.TreeMap<>(request);
            changed.put(field, "invalid"); reject(changed);
        }
        for (String path : List.of("../a", "/a", "a//b", "a/./b", "C:/a")) {
            var changed = new java.util.TreeMap<>(request);
            changed.put("files", List.of(Json.object("path", path, "text", ""))); reject(changed);
        }
        var duplicate = new java.util.TreeMap<>(request);
        duplicate.put("files", List.of(file(), file())); reject(duplicate);
    }
    public static Map<String, Object> request() {
        return Json.object("protocol", EditorMain.PROTOCOL, "source", "yaml-0.3", "files", List.of(file()), "schema", null);
    }
    private static Map<String,Object> file() {
        return Json.object("path", "a.yaml", "text", "format: \"mundanereq-yaml-0.3\"\nrequirements:\n  - id: \"A\"\n    title: \"A title\"\n    statement: \"The system shall store data.\"\n");
    }
    private static void reject(Object request) {
        try { EditorMain.analyze(request); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("invalid request accepted");
    }
}
