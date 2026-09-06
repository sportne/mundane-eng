package mundanereq;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import mundanereq.source.SourceDocument;
import mundanereq.source.SourceSpan;
import mundanereq.source.SourcePosition;

/** Strict interpretation of explicitly selected requirement source profiles. */
public final class Interpreter {
    private static final Pattern ID_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");
    public record Diagnostic(String file, int line, int column, String code, String message) {}

    public record Location(String file, int line, int column) {}

    public sealed interface ContentBlock permits ProseBlock, MathBlock {}

    public record ProseBlock(String text) implements ContentBlock {}

    public record MathBlock(String language, String payload) implements ContentBlock {}

    record RelationshipLocation(String target, int line, int column) {}

    public record Requirement(
            String id,
            String title,
            String allocation,
            List<ContentBlock> statement,
            List<ContentBlock> rationale,
            String source,
            Set<String> decomposes, Map<String,String> attributes) {
        public Requirement(String id,String title,String allocation,List<ContentBlock> statement,List<ContentBlock> rationale,String source,Set<String> decomposes) {
            this(id,title,allocation,statement,rationale,source,decomposes,Map.of());
        }
        public Requirement {
            attributes=Map.copyOf(attributes);
            statement = List.copyOf(statement);
            rationale = rationale == null ? null : List.copyOf(rationale);
            decomposes = Set.copyOf(decomposes);
        }
    }

    /** Retained syntax provenance; independent from semantic requirement values. */
    public record RequirementOrigin(String id, SourceSpan record,
            Map<String, List<SourceSpan>> fields, Map<String, SourceSpan> references, Map<String,AttributeLocation> attributes) {
        public RequirementOrigin(String id,SourceSpan record,Map<String,List<SourceSpan>> fields,Map<String,SourceSpan> references) {this(id,record,fields,references,Map.of());}
        public RequirementOrigin {
            attributes=Map.copyOf(attributes);
            Map<String, List<SourceSpan>> copied = new HashMap<>();
            fields.forEach((key, value) -> copied.put(key, List.copyOf(value)));
            fields = Map.copyOf(copied);
            references = Map.copyOf(references);
        }
    }

    public record AttributeLocation(SourceSpan name,SourceSpan value) {}

    record ParsedRequirement(
            Requirement requirement, Location location, List<RelationshipLocation> relationshipLocations,
            RequirementOrigin origin) {}

    public record Source(String file, byte[] bytes, Object fileKey) {
        public Source(String file, byte[] bytes) { this(file, bytes, null); }
        public Source {
            bytes = bytes.clone();
        }

        @Override
        public byte[] bytes() {
            return bytes.clone();
        }
    }

    /** Deterministically selected physical sources or diagnostics preventing selection. */
    public record Selection(List<Source> sources, List<Diagnostic> diagnostics) {
        public Selection {
            sources = List.copyOf(sources);
            diagnostics = List.copyOf(diagnostics);
        }

        public boolean valid() {
            return diagnostics.isEmpty();
        }
    }

    public record Result(
            List<Requirement> requirements,
            Map<String, Requirement> byId,
            Map<String, Set<String>> outgoing,
            List<Diagnostic> diagnostics,
            int fileCount,
            List<RequirementOrigin> origins,
            boolean syntaxComplete, AttributeSchema attributeSchema) {
        public Result(List<Requirement> requirements,Map<String,Requirement> byId,Map<String,Set<String>> outgoing,List<Diagnostic> diagnostics,int fileCount,List<RequirementOrigin> origins,boolean syntaxComplete) {
            this(requirements,byId,outgoing,diagnostics,fileCount,origins,syntaxComplete,null);
        }
        public Result(List<Requirement> requirements, Map<String, Requirement> byId,
                Map<String, Set<String>> outgoing, List<Diagnostic> diagnostics, int fileCount,
                List<RequirementOrigin> origins) {
            this(requirements, byId, outgoing, diagnostics, fileCount, origins, diagnostics.isEmpty());
        }
        public Result(List<Requirement> requirements, Map<String, Requirement> byId,
                Map<String, Set<String>> outgoing, List<Diagnostic> diagnostics, int fileCount) {
            this(requirements, byId, outgoing, diagnostics, fileCount, List.of());
        }
        public Result {
            origins = List.copyOf(origins);
            requirements = List.copyOf(requirements);
            byId = Map.copyOf(byId);
            Map<String, Set<String>> copiedOutgoing = new HashMap<>();
            outgoing.forEach((id, targets) -> copiedOutgoing.put(id, Set.copyOf(targets)));
            outgoing = Map.copyOf(copiedOutgoing);
            diagnostics = List.copyOf(diagnostics);
        }

        public boolean valid() {
            return syntaxComplete && diagnostics.isEmpty();
        }
    }

    private record Decoded(SourceDocument document, List<Diagnostic> diagnostics) {}

    private Interpreter() {}

    public static boolean isValidRequirementId(String value) {
        return value != null && ID_PATTERN.matcher(value).matches();
    }

    private static Result emptyResult(List<Diagnostic> diagnostics, int fileCount) {
        return new Result(List.of(), Map.of(), Map.of(), diagnostics, fileCount);
    }

    public static Result interpretInputs(List<Path> inputs) {
        return interpretInputs(inputs, SourceFormat.YAML_03);
    }

    public static Result interpretInputs(List<Path> inputs, SourceFormat format) {
        Selection selection = selectInputs(inputs, format);
        if (!selection.valid()) {
            return emptyResult(selection.diagnostics(), selection.sources().size());
        }
        return interpretSources(selection.sources(), format);
    }

    public static Selection selectInputs(List<Path> inputs) {
        return selectInputs(inputs, SourceFormat.YAML_03);
    }

    public static Selection selectInputs(List<Path> inputs, SourceFormat format) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        TreeSet<Path> files = new TreeSet<>();
        for (Path input : inputs) {
            discover(input.toAbsolutePath().normalize(), true, files, diagnostics, format);
        }
        sortDiagnostics(diagnostics);
        if (!diagnostics.isEmpty()) return new Selection(List.of(), diagnostics);
        if (files.isEmpty()) {
            String input = inputs.isEmpty() ? "." : inputs.getFirst().toString();
            return new Selection(
                    List.of(),
                    List.of(diagnostic(input, 1, 1, "no-source-files", "no " + format.suffix + " source files were selected")));
        }

        List<Source> sources = new ArrayList<>();
        for (Path file : files) {
            try {
                var attributes = Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                byte[] bytes;
                try (var input = Files.newInputStream(file)) {
                    bytes = input.readNBytes(YamlRequirements.MAX_BYTES + 1);
                }
                sources.add(new Source(file.toString(), bytes, attributes.fileKey()));
            } catch (IOException exception) {
                diagnostics.add(diagnostic(file.toString(), 1, 1, "input-unavailable", exception.getMessage()));
            }
        }
        if (!diagnostics.isEmpty()) {
            sortDiagnostics(diagnostics);
            return new Selection(sources, diagnostics);
        }
        return new Selection(sources, List.of());
    }

    private static void discover(
            Path input, boolean explicit, Set<Path> files, List<Diagnostic> diagnostics, SourceFormat format) {
        BasicFileAttributes attributes;
        try {
            attributes = Files.readAttributes(input, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException exception) {
            diagnostics.add(diagnostic(input.toString(), 1, 1, "input-unavailable", exception.getMessage()));
            return;
        }

        if (attributes.isSymbolicLink()) return;
        if (attributes.isRegularFile()) {
            if (explicit || input.getFileName().toString().endsWith(format.suffix)) files.add(input);
            return;
        }
        if (!attributes.isDirectory()) return;

        try {
            Files.walkFileTree(input, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes ignored) {
                    if (!directory.equals(input) && directory.getFileName().toString().equals(".git")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes fileAttributes) {
                    if (fileAttributes.isRegularFile() && file.getFileName().toString().endsWith(format.suffix)) {
                        files.add(file.toAbsolutePath().normalize());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exception) {
                    diagnostics.add(diagnostic(file.toString(), 1, 1, "input-unavailable", exception.getMessage()));
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException exception) {
            diagnostics.add(diagnostic(input.toString(), 1, 1, "input-unavailable", exception.getMessage()));
        }
    }

    public static Result interpretSources(List<Source> inputSources) {
        return interpretSources(inputSources, SourceFormat.YAML_03);
    }

    public static Result interpretSources(List<Source> inputSources, SourceFormat format) {return interpretSources(inputSources,format,null);}
    public static Result interpretSources(List<Source> inputSources, SourceFormat format,AttributeSchema schema) {
        if(schema!=null&&!schema.valid())return new Result(List.of(),Map.of(),Map.of(),schema.diagnostics(),inputSources.size(),List.of(),false,schema);
        if (inputSources.isEmpty()) {
            return emptyResult(
                    List.of(diagnostic(".", 1, 1, "no-source-files", "no " + format.suffix + " source files were selected")),
                    0);
        }
        List<Source> sources = inputSources.stream()
                .sorted(Comparator.comparing(Source::file))
                .toList();
        List<ParsedRequirement> parsedRequirements = new ArrayList<>();
        List<Diagnostic> diagnostics = new ArrayList<>();

        for (Source source : sources) {
            if (source.bytes().length > YamlRequirements.MAX_BYTES) {
                diagnostics.add(diagnostic(source.file(), 1, 1, "yaml-limit", "source exceeds 8 MiB"));
                continue;
            }
            Decoded decoded = decode(source);
            diagnostics.addAll(decoded.diagnostics());
            if (decoded.document() == null) continue;
            parsedRequirements.addAll(YamlRequirements.parse(source, diagnostics,format,schema));
        }

        boolean incomplete = !diagnostics.isEmpty();
        Map<String, Requirement> byId = new HashMap<>();
        Map<String, Location> firstLocationById = new HashMap<>();
        for (ParsedRequirement parsed : parsedRequirements) {
            Requirement requirement = parsed.requirement();
            Requirement prior = byId.putIfAbsent(requirement.id(), requirement);
            if (prior != null) {
                Location priorLocation = firstLocationById.get(requirement.id());
                diagnostics.add(diagnostic(
                        parsed.location().file(),
                        parsed.location().line(),
                        parsed.location().column(),
                        "duplicate-id",
                        "requirement '%s' was already defined at %s:%d"
                                .formatted(requirement.id(), priorLocation.file(), priorLocation.line())));
            } else {
                firstLocationById.put(requirement.id(), parsed.location());
            }
        }

        for (ParsedRequirement parsed : parsedRequirements) {
            for (RelationshipLocation relationship : parsed.relationshipLocations()) {
                if (!byId.containsKey(relationship.target())
                        && !incomplete) {
                    diagnostics.add(diagnostic(
                            parsed.location().file(),
                            relationship.line(),
                            relationship.column(),
                            "dangling-reference",
                            "decomposes target '%s' does not exist in the selected source set"
                                    .formatted(relationship.target())));
                }
            }
        }

        List<Requirement> requirements = parsedRequirements.stream().map(ParsedRequirement::requirement).toList();
        Map<String, Set<String>> outgoing = new HashMap<>();
        for (Requirement requirement : requirements) {
            outgoing.put(requirement.id(), requirement.decomposes());
        }
        sortDiagnostics(diagnostics);
        return new Result(requirements, byId, outgoing, diagnostics, sources.size(),
                parsedRequirements.stream().map(ParsedRequirement::origin).toList(), !incomplete,schema);
    }

    private static Decoded decode(Source source) {
        byte[] bytes = source.bytes();
        List<Diagnostic> diagnostics = new ArrayList<>();
        if (bytes.length >= 3
                && Byte.toUnsignedInt(bytes[0]) == 0xef
                && Byte.toUnsignedInt(bytes[1]) == 0xbb
                && Byte.toUnsignedInt(bytes[2]) == 0xbf) {
            diagnostics.add(diagnostic(
                    source.file(), 1, 1, "byte-order-mark", "UTF-8 byte-order marks are not allowed"));
        }

        String text = null;
        var decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        ByteBuffer input = ByteBuffer.wrap(bytes);
        CharBuffer output = CharBuffer.allocate(bytes.length + 1);
        CoderResult decoded = decoder.decode(input, output, true);
        if (decoded.isUnderflow()) decoded = decoder.flush(output);
        if (decoded.isError()) {
            int[] position = bytePosition(bytes, input.position());
            diagnostics.add(diagnostic(
                    source.file(), position[0], position[1], "invalid-utf8", "source is not valid UTF-8"));
        } else {
            output.flip();
            text = output.toString();
        }

        if (text != null) {
            for (int offset = 0; offset < text.length(); ) {
                int character = text.codePointAt(offset);
                if (character == 0) {
                    int[] position = textPosition(text, offset);
                    diagnostics.add(diagnostic(
                            source.file(), position[0], position[1], "nul-byte", "NUL bytes are not allowed"));
                    break;
                } else if (character == '\t') {
                    int[] position = textPosition(text, offset);
                    diagnostics.add(diagnostic(source.file(), position[0], position[1], "tab", "tabs are not allowed"));
                    break;
                } else if (isDisallowedControl(character)) {
                    int[] position = textPosition(text, offset);
                    diagnostics.add(diagnostic(
                            source.file(), position[0], position[1], "control-character", "control characters are not allowed"));
                    break;
                } else if (character == '\r' && (offset + 1 >= text.length() || text.charAt(offset + 1) != '\n')) {
                    int[] position = textPosition(text, offset);
                    diagnostics.add(diagnostic(
                            source.file(), position[0], position[1], "line-ending", "a carriage return must be followed by a line feed"));
                    break;
                }
                offset += Character.charCount(character);
            }
            if (!text.endsWith("\n")) {
                int[] position = textPosition(text, text.length());
                diagnostics.add(diagnostic(
                        source.file(), position[0], position[1], "final-line-ending", "source must end with a line ending"));
            }
        }

        if (!diagnostics.isEmpty()) return new Decoded(null, List.copyOf(diagnostics));
        try {
            return new Decoded(SourceDocument.read(source.file(), bytes), List.of());
        } catch (CharacterCodingException exception) {
            return new Decoded(
                    null,
                    List.of(diagnostic(source.file(), 1, 1, "invalid-utf8", "source is not valid UTF-8")));
        }
    }

    private static boolean isDisallowedControl(int character) {
        return (character >= 0x01 && character <= 0x08)
                || character == 0x0b
                || character == 0x0c
                || (character >= 0x0e && character <= 0x1f)
                || (character >= 0x7f && character <= 0x9f);
    }

    private static int[] bytePosition(byte[] bytes, int offset) {
        int line = 1;
        int column = 1;
        for (int index = 0; index < offset; index++) {
            if (bytes[index] == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        return new int[] {line, column};
    }

    private static int[] textPosition(String text, int offset) {
        int line = 1;
        int column = 1;
        for (int index = 0; index < offset; ) {
            int character = text.codePointAt(index);
            if (character == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
            index += Character.charCount(character);
        }
        return new int[] {line, column};
    }

    private static Diagnostic diagnostic(
            String file, int line, int column, String code, String message) {
        return new Diagnostic(file, line, column, code, message == null ? "input is unavailable" : message);
    }

    private static void sortDiagnostics(List<Diagnostic> diagnostics) {
        diagnostics.sort(Comparator.comparing(Diagnostic::file)
                .thenComparingInt(Diagnostic::line)
                .thenComparingInt(Diagnostic::column)
                .thenComparing(Diagnostic::code));
    }
}
