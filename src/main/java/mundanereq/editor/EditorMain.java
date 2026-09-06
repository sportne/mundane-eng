package mundanereq.editor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import mundane.json.Json;
import mundanereq.AttributeSchema;
import mundanereq.Interpreter;
import mundanereq.SourceFormat;

/** One bounded, read-only editor snapshot per process; never reads project files. */
public final class EditorMain {
    public static final String PROTOCOL = mundanereq.Versions.EDITOR_PROTOCOL;
    public static final int MAX_REQUEST = 16 * 1024 * 1024;
    private EditorMain() {}

    public static void main(String[] args) {
        try {
            if (args.length == 1 && args[0].equals("--version")) {
                System.out.write(Json.bytes(Json.object("version", mundanereq.Versions.EDITOR_VERSION,
                        "protocol", PROTOCOL, "project", mundanereq.Versions.EDITOR_PROJECT)));
                System.out.flush();
                if (System.out.checkError()) throw new IOException("editor output unavailable");
                return;
            }
            if (args.length != 0) throw new IllegalArgumentException("editor bridge accepts JSON on stdin only");
            byte[] bytes = System.in.readNBytes(MAX_REQUEST + 1);
            if (bytes.length > MAX_REQUEST) throw new IllegalArgumentException("snapshot exceeds 16 MiB");
            byte[] output = Json.bytes(analyze(Json.read(bytes)));
            if (output.length > MAX_REQUEST) throw new IllegalArgumentException("response exceeds 16 MiB");
            System.out.write(output);
            System.out.flush();
            if (System.out.checkError()) throw new IOException("editor output unavailable");
        } catch (IOException | IllegalArgumentException error) {
            System.err.println("editor-request: " + error.getMessage());
            System.exit(2);
        }
    }

    public static Map<String, Object> analyze(Object value) {
        Map<String, Object> request = object(value);
        if (!request.keySet().containsAll(java.util.Set.of("protocol", "source", "files", "schema"))
                || !java.util.Set.of("protocol", "source", "files", "schema", "cursor").containsAll(request.keySet()))
            throw new IllegalArgumentException("unexpected snapshot fields");
        if (!PROTOCOL.equals(request.get("protocol"))) throw new IllegalArgumentException("unsupported editor protocol");
        if (mundanereq.Versions.WORK_SOURCE.equals(request.get("source"))) return work(request);
        SourceFormat format = switch (string(request.get("source"))) {
            case "yaml-0.3" -> SourceFormat.YAML_03;
            case "yaml-0.4" -> SourceFormat.YAML_04;
            default -> throw new IllegalArgumentException("unsupported requirement profile");
        };
        List<?> files = request.get("files") instanceof List<?> list ? list : List.of();
        if (files.isEmpty() || files.size() > 128) throw new IllegalArgumentException("select 1–128 files");
        var sources = new ArrayList<Interpreter.Source>();
        var paths = new HashSet<String>();
        for (Object file : files) {
            var source = source(file, 8 * 1024 * 1024);
            if (!paths.add(source.file())) throw new IllegalArgumentException("duplicate selected path");
            sources.add(source);
        }
        AttributeSchema schema = null;
        if (request.get("schema") != null) {
            if (format != SourceFormat.YAML_04) throw new IllegalArgumentException("declarations require YAML 0.4");
            var source = source(request.get("schema"), AttributeSchema.MAX_BYTES);
            if (!paths.add(source.file())) throw new IllegalArgumentException("schema is also selected as requirements");
            schema = AttributeSchema.parse(source);
        }
        List<Map<String,Object>> suggestions = List.of();
        Map<String,Object> hover = null;
        if (request.get("cursor") != null) {
            var cursor = object(request.get("cursor"));
            if (!cursor.keySet().equals(java.util.Set.of("path", "line", "column"))) throw new IllegalArgumentException("invalid cursor fields");
            String path = string(cursor.get("path"));
            var source = sources.stream().filter(file -> file.file().equals(path)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("cursor file is not selected"));
            int line = coordinate(cursor.get("line")), column = coordinate(cursor.get("column"));
            if (format == SourceFormat.YAML_04) {
                suggestions = AttributeCompletion.suggest(source, schema, line, column);
                hover = AttributeCompletion.describe(source, schema, line, column);
            }
        }
        var result = Interpreter.interpretSources(sources, format, schema);
        return Json.object("protocol", PROTOCOL, "valid", result.valid(), "suggestions", suggestions, "hover", hover, "formatting", result.valid() ? sources.stream().filter(source -> new String(source.bytes(), StandardCharsets.UTF_8).contains("\r\n"))
                        .map(source -> Json.object("path", source.file(), "text", new String(source.bytes(), StandardCharsets.UTF_8).replace("\r\n", "\n"))).toList() : List.of(),
                "definitions", result.valid() ? result.origins().stream().map(origin -> Json.object(
                        "id", origin.id(), "location", span(origin.fields().get("id").getFirst()),
                        "references", origin.references().entrySet().stream().sorted(Map.Entry.comparingByKey())
                                .map(entry -> Json.object("id", entry.getKey(), "location", span(entry.getValue()))).toList())).toList() : List.of(),
                "diagnostics", result.diagnostics().stream().map(d -> Json.object("path", d.file(),
                        "line", d.line(), "column", d.column(), "code", d.code(), "message", d.message())).toList());
    }

    private static Map<String,Object> work(Map<String,Object> request) {
        if(request.get("schema")!=null)throw new IllegalArgumentException("work snapshots do not accept a schema");
        if(!(request.get("files") instanceof List<?> files))throw new IllegalArgumentException("expected selected files");
        var sources=new ArrayList<engineering.artifacts.Snapshots.Snapshot>();
        for(Object value:files) {
            var input=source(value,1024*1024);
            sources.add(new engineering.artifacts.Snapshots.Snapshot(input.file(),input.bytes(),null));
        }
        if(request.get("cursor")!=null) {
            var cursor=object(request.get("cursor"));
            if(!cursor.keySet().equals(java.util.Set.of("path","line","column")))throw new IllegalArgumentException("invalid cursor fields");
            coordinate(cursor.get("line"));coordinate(cursor.get("column"));
            if(sources.stream().noneMatch(s->s.path().equals(cursor.get("path"))))throw new IllegalArgumentException("cursor file is not selected");
        }
        var result=engineering.work.WorkCompiler.compileSnapshots(sources);
        return Json.object("protocol",PROTOCOL,"valid",result.valid(),"diagnostics",result.diagnostics().stream().map(d->{
            var location=object(d.get("location"));return Json.object("path",location.get("path"),"line",location.get("line"),
                "column",location.get("column"),"code",d.get("code"),"message",d.get("message"));
        }).toList(),"definitions",List.of(),"formatting",List.of(),"suggestions",List.of(),"hover",null);
    }

    static Map<String,Object> span(mundanereq.source.SourceSpan span) {
        return Json.object("path", span.start().source(),
                "start", Json.object("line", span.start().line(), "column", span.start().column()),
                "end", Json.object("line", span.end().line(), "column", span.end().column()));
    }

    private static Interpreter.Source source(Object value, int maximum) {
        var file = object(value);
        if (!file.keySet().equals(java.util.Set.of("path", "text"))) throw new IllegalArgumentException("invalid file snapshot fields");
        String path = string(file.get("path"));
        if (path.isEmpty() || path.startsWith("/") || path.contains("\\") || path.contains(":")
                || java.util.Arrays.stream(path.split("/", -1)).anyMatch(p -> p.isEmpty() || p.equals(".") || p.equals("..")))
            throw new IllegalArgumentException("snapshot paths must be normalized project-relative paths");
        byte[] bytes = string(file.get("text")).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > maximum) throw new IllegalArgumentException("file snapshot exceeds limit");
        return new Interpreter.Source(path, bytes);
    }

    private static int coordinate(Object value) {
        if (!(value instanceof Number number) || number.doubleValue() != number.intValue() || number.intValue() < 1 || number.intValue() > 8 * 1024 * 1024)
            throw new IllegalArgumentException("invalid cursor coordinate");
        return number.intValue();
    }

    private static String string(Object value) {
        if (!(value instanceof String text)) throw new IllegalArgumentException("expected text");
        return text;
    }

    private static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?> map)) throw new IllegalArgumentException("expected object");
        var result = new java.util.TreeMap<String, Object>();
        map.forEach((key, item) -> result.put(string(key), item));
        return result;
    }
}
