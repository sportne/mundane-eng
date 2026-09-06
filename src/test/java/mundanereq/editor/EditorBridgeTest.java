package mundanereq.editor;

import java.util.List;
import java.util.Map;
import mundane.json.Json;

/** Snapshot boundary behavior, independent of the editor UI. */
public final class EditorBridgeTest {
    private EditorBridgeTest() {}
    public static void run() throws Exception {
        completion();
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
    private static void completion() throws Exception {
        String declaration = java.nio.file.Files.readString(java.nio.file.Path.of("editors/vscode/test/fixtures/schema.json"));
        var schema = mundanereq.AttributeSchema.parse(new mundanereq.Interpreter.Source("schema.json", declaration.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        String base = "format: \"mundanereq-yaml-0.4\"\nattributeSchema: \"logger-metadata\"\nrequirements:\n  - id: \"A\"\n    title: \"A title\"\n    statement: \"The system shall store data.\"\n";
        String empty = base + "    attributes:\n      ";
        assert suggest(empty, schema).size() == 2 : "required and optional names";
        assert suggest(base + "    attributes:\n      discipline: \"software\"\n      ow", schema).size() == 1;
        for (String suffix : List.of("    rationale: |-\n      ow", "    attributes:\n      # ow", "    attributes:\n      discipline: # comment"))
            assert suggest(base + suffix, schema).isEmpty() : suffix;
        assert suggest(base + "    attributes:\n      discipline: ", schema).size() == 3;
        assert suggest(base + "    attributes:\n      discipline: |\n        software", schema).isEmpty();
    }
    private static java.util.List<Map<String,Object>> suggest(String text, mundanereq.AttributeSchema schema) {
        var lines = text.split("\n", -1);
        return AttributeCompletion.suggest(new mundanereq.Interpreter.Source("a.yaml", text.getBytes(java.nio.charset.StandardCharsets.UTF_8)), schema,
                lines.length, lines[lines.length - 1].codePointCount(0, lines[lines.length - 1].length()) + 1);
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
