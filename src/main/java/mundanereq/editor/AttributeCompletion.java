package mundanereq.editor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mundane.json.Json;
import mundanereq.AttributeSchema;
import mundanereq.Interpreter;
import mundanereq.source.SourcePosition;
import mundanereq.source.SourceSpan;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.lowlevel.Compose;
import org.snakeyaml.engine.v2.api.lowlevel.Parse;
import org.snakeyaml.engine.v2.common.ScalarStyle;
import org.snakeyaml.engine.v2.events.*;
import org.snakeyaml.engine.v2.nodes.*;
import org.snakeyaml.engine.v2.schema.CoreSchema;

/** Syntax-only cursor assistance; semantic verdicts still come from Interpreter. */
final class AttributeCompletion {
    private AttributeCompletion() {}
    static List<Map<String,Object>> suggest(Interpreter.Source source, AttributeSchema schema, int line, int column) {
        if (schema == null || !schema.valid()) return List.of();
        String text = new String(source.bytes(), StandardCharsets.UTF_8);
        String[] lines = text.split("\n", -1);
        if (line < 1 || line > lines.length) return List.of();
        String raw = lines[line - 1].replace("\r", "");
        if (column < 1 || column > raw.codePointCount(0, raw.length()) + 1) return List.of();
        // Repair only the cursor's incomplete key, only for a syntax tree used below.
        // The resulting path must still be requirements[i].attributes[key]. No repaired
        // text or semantic value is ever published or passed to the validator.
        boolean partial = raw.matches(" +[a-z0-9-]*");
        int indentation = raw.length() - raw.stripLeading().length();
        String key = raw.strip();
        String parsed = text;
        if (partial) {
            if (column <= indentation) return List.of();
            lines[line - 1] = " ".repeat(indentation) + (key.isEmpty() ? "editor-placeholder" : key) + ": null";
            parsed = String.join("\n", lines);
        }
        try {
            Map<String,Node> root = mapping(compose(parsed));
            if (!scalar(root.get("format")).equals("mundanereq-yaml-0.4")
                    || !scalar(root.get("attributeSchema")).equals(schema.name())) return List.of();
            if (!(root.get("requirements") instanceof SequenceNode requirements)) return List.of();
            Map<?,?> declarations = (Map<?,?>) schema.definition().get("attributes");
            for (Node record : requirements.getValue()) {
                Map<String,Node> fields = mapping(record);
                if (!(fields.get("attributes") instanceof MappingNode attributes)) continue;
                Map<String,Node> values = mapping(attributes);
                for (var tuple : attributes.getValue()) {
                    Node name = tuple.getKeyNode(), value = tuple.getValueNode();
                    String attribute = scalar(name);
                    var start = name.getStartMark().orElseThrow();
                    if (partial && start.getLine() + 1 == line && start.getColumn() == indentation) {
                        var suggestions = new ArrayList<Map<String,Object>>();
                        for (Object declared : declarations.keySet().stream().sorted().toList()) {
                            String candidate = (String) declared;
                            if (values.containsKey(candidate) && !candidate.equals(attribute) || !candidate.startsWith(key)) continue;
                            Map<?,?> definition = (Map<?,?>) declarations.get(candidate);
                            suggestions.add(suggestion(candidate, candidate + ": ", source.file(), line, indentation + 1,
                                    raw.codePointCount(0, raw.length()) + 1, definition));
                        }
                        return suggestions;
                    }
                    if (partial || !(value instanceof ScalarNode node) || !simple(node)) continue;
                    var begin = value.getStartMark().orElseThrow(); var end = value.getEndMark().orElseThrow();
                    if (begin.getLine() + 1 != line || end.getLine() + 1 != line) continue;
                    Map<?,?> definition = declarations.get(attribute) instanceof Map<?,?> map ? map : Map.of();
                    if (!"enum".equals(definition.get("type"))) continue;
                    boolean empty = Tag.NULL.equals(node.getTag()) && node.getValue().isEmpty();
                    int first = empty ? column : begin.getColumn() + 1;
                    int last = empty ? column : end.getColumn() + 1;
                    int keyEnd = name.getEndMark().orElseThrow().getColumn();
                    if (empty && (column <= keyEnd + 1 || !raw.substring(raw.offsetByCodePoints(0, keyEnd)).matches(": *"))) continue;
                    if (column < first || column > last) continue;
                    var suggestions = new ArrayList<Map<String,Object>>();
                    for (Object allowed : (List<?>) definition.get("values")) {
                        String candidate = (String) allowed;
                        suggestions.add(suggestion(candidate, Json.write(candidate), source.file(), line, first, last, definition));
                    }
                    return suggestions;
                }
            }
        } catch (IllegalArgumentException | org.snakeyaml.engine.v2.exceptions.YamlEngineException ignored) {
            // Incomplete structure cannot establish a safe attribute position.
        }
        return List.of();
    }
    static Map<String,Object> describe(Interpreter.Source source, AttributeSchema schema, int line, int column) {
        if (schema == null || !schema.valid()) return null;
        try {
            Map<String,Node> root = mapping(compose(new String(source.bytes(), StandardCharsets.UTF_8)));
            if (!scalar(root.get("format")).equals("mundanereq-yaml-0.4")
                    || !scalar(root.get("attributeSchema")).equals(schema.name())
                    || !(root.get("requirements") instanceof SequenceNode requirements)) return null;
            Map<?,?> declarations = (Map<?,?>) schema.definition().get("attributes");
            for (Node record : requirements.getValue()) {
                if (!(mapping(record).get("attributes") instanceof MappingNode attributes)) continue;
                mapping(attributes); // Duplicate names make the context ambiguous.
                for (var tuple : attributes.getValue()) {
                    String name = scalar(tuple.getKeyNode());
                    if (!(declarations.get(name) instanceof Map<?,?> definition)) continue;
                    for (Node token : List.of(tuple.getKeyNode(), tuple.getValueNode())) {
                        if (!(token instanceof ScalarNode scalar) || !simple(scalar)) continue;
                        var start = token.getStartMark().orElseThrow(); var end = token.getEndMark().orElseThrow();
                        if ((line > start.getLine() + 1 || line == start.getLine() + 1 && column >= start.getColumn() + 1)
                                && (line < end.getLine() + 1 || line == end.getLine() + 1 && column < end.getColumn() + 1)) {
                            return Json.object("name", name, "definition", definition,
                                    "declaration", EditorMain.span(schema.locations().get(name)),
                                    "location", EditorMain.span(new SourceSpan(new SourcePosition(source.file(), start.getLine() + 1, start.getColumn() + 1),
                                            new SourcePosition(source.file(), end.getLine() + 1, end.getColumn() + 1))));
                        }
                    }
                }
            }
        } catch (IllegalArgumentException | org.snakeyaml.engine.v2.exceptions.YamlEngineException ignored) {
            // Hover requires an unambiguous parsed token; it never repairs source.
        }
        return null;
    }
    private static Map<String,Object> suggestion(String label, String insertion, String path, int line, int first, int last, Map<?,?> definition) {
        return Json.object("label", label, "insertText", insertion, "location", EditorMain.span(new SourceSpan(
                new SourcePosition(path, line, first), new SourcePosition(path, line, last))),
                "detail", definition.get("type") + (Boolean.TRUE.equals(definition.get("required")) ? " (required)" : " (optional)"));
    }
    private static boolean simple(ScalarNode node) {
        return node.getScalarStyle() == ScalarStyle.PLAIN || node.getScalarStyle() == ScalarStyle.SINGLE_QUOTED || node.getScalarStyle() == ScalarStyle.DOUBLE_QUOTED;
    }
    private static String scalar(Node node) { return node instanceof ScalarNode scalar ? scalar.getValue() : ""; }
    private static Map<String,Node> mapping(Node node) {
        var result = new HashMap<String,Node>();
        if (node instanceof MappingNode mapping) for (var tuple : mapping.getValue()) {
            if (!(tuple.getKeyNode() instanceof ScalarNode key) || result.put(key.getValue(), tuple.getValueNode()) != null)
                throw new IllegalArgumentException("ambiguous mapping");
        }
        return result;
    }
    private static Node compose(String text) {
        var settings = LoadSettings.builder().setSchema(new CoreSchema()).setAllowDuplicateKeys(false)
                .setMaxAliasesForCollections(0).setCodePointLimit(8 * 1024 * 1024).setUseMarks(true).build();
        int depth = 0, documents = 0;
        for (Event event : new Parse(settings).parseString(text)) {
            if (event instanceof CollectionStartEvent && ++depth > 16 || event instanceof AliasEvent
                    || event instanceof NodeEvent node && node.getAnchor().isPresent()
                    || event instanceof ScalarEvent scalar && scalar.getTag().isPresent()
                    || event instanceof CollectionStartEvent collection && collection.getTag().isPresent())
                throw new IllegalArgumentException("unsupported YAML structure");
            if (event instanceof CollectionEndEvent) --depth;
            if (event instanceof DocumentStartEvent start && (++documents > 1 || start.getSpecVersion().isPresent() || !start.getTags().isEmpty()))
                throw new IllegalArgumentException("unsupported YAML document");
        }
        return new Compose(settings).composeString(text).orElseThrow(() -> new IllegalArgumentException("empty YAML"));
    }
}
