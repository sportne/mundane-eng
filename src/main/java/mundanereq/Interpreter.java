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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import mundanereq.source.SourceDocument;
import mundanereq.source.SourceSpan;
import mundanereq.source.SourcePosition;

/** Strict interpretation of explicitly selected requirement source profiles. */
public final class Interpreter {
    private static final Pattern ID_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");
    private static final Pattern OPENER_PATTERN = Pattern.compile("requirement (.+)");
    private static final Pattern FIELD_PATTERN = Pattern.compile("([a-z]+):.*", Pattern.DOTALL);
    private static final Set<String> FIELD_NAMES = Set.of(
            "title", "allocation", "statement", "rationale", "source", "decomposes");

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

    private record BodyLine(String text, int lineIndex) {}

    private record Scalar(String value) {}

    private record Body(List<ContentBlock> blocks) {}

    private Interpreter() {}

    public static boolean isValidRequirementId(String value) {
        return value != null && ID_PATTERN.matcher(value).matches();
    }

    private static Result emptyResult(List<Diagnostic> diagnostics, int fileCount) {
        return new Result(List.of(), Map.of(), Map.of(), diagnostics, fileCount);
    }

    public static Result interpretInputs(List<Path> inputs) {
        return interpretInputs(inputs, SourceFormat.CUSTOM_02);
    }

    public static Result interpretInputs(List<Path> inputs, SourceFormat format) {
        Selection selection = selectInputs(inputs, format);
        if (!selection.valid()) {
            return emptyResult(selection.diagnostics(), selection.sources().size());
        }
        return interpretSources(selection.sources(), format);
    }

    public static Selection selectInputs(List<Path> inputs) {
        return selectInputs(inputs, SourceFormat.CUSTOM_02);
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
                if (format != SourceFormat.CUSTOM_02) {
                    try (var input = Files.newInputStream(file)) {
                        bytes = input.readNBytes(YamlRequirements.MAX_BYTES + 1);
                    }
                } else {
                    bytes = Files.readAllBytes(file);
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
        return interpretSources(inputSources, SourceFormat.CUSTOM_02);
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
            if (format != SourceFormat.CUSTOM_02 && source.bytes().length > YamlRequirements.MAX_BYTES) {
                diagnostics.add(diagnostic(source.file(), 1, 1, "yaml-limit", "source exceeds 8 MiB"));
                continue;
            }
            Decoded decoded = decode(source);
            diagnostics.addAll(decoded.diagnostics());
            if (decoded.document() == null) continue;
            if (format != SourceFormat.CUSTOM_02) {
                parsedRequirements.addAll(YamlRequirements.parse(source, diagnostics,format,schema));
                continue;
            }
            parsedRequirements.addAll(new Parser(decoded.document()).parse(diagnostics));
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

    private static boolean isScalarBoundaryWhitespace(int character) {
        return (character >= 0x09 && character <= 0x0d)
                || character == 0x20
                || character == 0x85
                || character == 0x00a0
                || character == 0x1680
                || (character >= 0x2000 && character <= 0x200a)
                || character == 0x2028
                || character == 0x2029
                || character == 0x202f
                || character == 0x205f
                || character == 0x3000;
    }

    private static final class Parser {
        private final String file;
        private final List<String> lines;
        private int index;
        private Map<String, List<SourceSpan>> fieldSpans;

        Parser(SourceDocument document) {
            this.file = document.name();
            this.lines = document.lines().stream()
                    .map(line -> line.physicalLine().text())
                    .toList();
        }

        List<ParsedRequirement> parse(List<Diagnostic> diagnostics) {
            List<ParsedRequirement> requirements = new ArrayList<>();
            int initialErrors = diagnostics.size();
            boolean requiresSeparation = false;
            while (index < lines.size()) {
                int triviaLines = 0;
                while (index < lines.size() && (line().isEmpty() || isCommentLine(line()))) {
                    index++;
                    triviaLines++;
                }
                if (index >= lines.size()) break;
                int start = index;
                try {
                    if (requiresSeparation && triviaLines == 0) {
                        fail(index, 1, "record-separation", "requirement records must be separated by a blank line or comment line");
                    }
                    if (!line().startsWith("requirement")) {
                        String code = line().equals("end requirement")
                                ? "unmatched-record-end" : "content-outside-record";
                        fail(index, 1, code, "nonblank content must occur inside a requirement record");
                    }
                    requirements.add(parseRecord());
                    requiresSeparation = true;
                } catch (ParseFailure failure) {
                    if (diagnostics.size() - initialErrors == 99) {
                        diagnostics.add(lineDiagnostic(start, 1, "recovery-limit",
                                "diagnostic limit reached; source is incomplete"));
                        break;
                    }
                    diagnostics.add(failure.diagnostic);
                    recover(start);
                    requiresSeparation = false;
                }
            }
            if (requirements.isEmpty() && diagnostics.size() == initialErrors) {
                diagnostics.add(lineDiagnostic(0, 1, "empty-source-file",
                        "a source file must contain at least one requirement record"));
            }
            return requirements;
        }

        // Only an unindented valid opener with source separation is a candidate.
        // Indented prose/math lookalikes never synchronize. An unclosed math block
        // makes the remainder ambiguous, so retain the prefix and stop recovery.
        private void recover(int start) {
            boolean math = false;
            boolean separated = false;
            for (int cursor = start + 1; cursor < lines.size(); cursor++) {
                String text = lines.get(cursor);
                if (text.equals("  math latex")) math = true;
                else if (text.equals("  end math")) math = false;
                if (!math && separated && OPENER_PATTERN.matcher(text).matches()
                        && isValidRequirementId(text.substring(12))) {
                    index = cursor;
                    return;
                }
                separated = text.isEmpty() || text.startsWith("#");
            }
            index = lines.size();
        }

        private ParsedRequirement parseRecord() {
            int start = index;
            fieldSpans = new HashMap<>();
            Matcher opener = OPENER_PATTERN.matcher(line());
            if (!opener.matches() || !ID_PATTERN.matcher(opener.group(1)).matches()) {
                fail(index, 1, "invalid-id", "requirement opener must contain a valid ID");
            }
            String id = opener.group(1);
            addSpan("id", span(index, 13, index, 13 + id.length()));
            Set<String> seen = new HashSet<>();
            index++;
            skipComments();

            requireNext("title", seen);
            Scalar title = parseScalar("title");
            seen.add("title");
            skipComments();

            String allocation = null;
            if ("allocation".equals(fieldName(lineOrNull()))) {
                Scalar parsed = parseScalar("allocation");
                allocation = parsed.value();
                seen.add("allocation");
                skipComments();
            }

            requireNext("statement", seen);
            Body statement = parseBody("statement", true);
            seen.add("statement");
            skipComments();

            List<ContentBlock> rationale = null;
            if ("rationale".equals(fieldName(lineOrNull()))) {
                Body parsed = parseBody("rationale", false);
                rationale = parsed.blocks();
                seen.add("rationale");
                skipComments();
            }

            String source = null;
            if ("source".equals(fieldName(lineOrNull()))) {
                Scalar parsed = parseScalar("source");
                source = parsed.value();
                seen.add("source");
                skipComments();
            }

            List<String> decomposes = new ArrayList<>();
            List<RelationshipLocation> relationshipLocations = new ArrayList<>();
            Set<String> targets = new HashSet<>();
            while ("decomposes".equals(fieldName(lineOrNull()))) {
                int relationshipLine = index;
                Scalar parsed = parseScalar("decomposes");
                if (!ID_PATTERN.matcher(parsed.value()).matches()) {
                    fail(relationshipLine, 13, "invalid-reference-id", "decomposes must contain a valid requirement ID");
                }
                if (!targets.add(parsed.value())) {
                    fail(relationshipLine, 1, "duplicate-relationship", "decomposes target '%s' is repeated".formatted(parsed.value()));
                }
                decomposes.add(parsed.value());
                relationshipLocations.add(new RelationshipLocation(parsed.value(), relationshipLine + 1, 13));
                seen.add("decomposes");
                skipComments();
            }

            if (index >= lines.size()) {
                fail(index - 1, 1, "missing-record-end", "requirement record is missing 'end requirement'");
            }
            Diagnostic unexpected = unexpected(seen);
            if (unexpected != null) throw new ParseFailure(unexpected);
            index++;

            return new ParsedRequirement(
                    new Requirement(
                            id,
                            title.value(),
                            allocation,
                            statement.blocks(),
                            rationale,
                            source,
                            Set.copyOf(decomposes)),
                    new Location(file, start + 1, 13),
                    List.copyOf(relationshipLocations),
                    new RequirementOrigin(id, span(start, 1, index - 1, endColumn(index - 1)),
                            fieldSpans, referenceSpans(relationshipLocations)));
        }

        private int endColumn(int lineIndex) {
            String text = lines.get(lineIndex);
            return text.codePointCount(0, text.length()) + 1;
        }

        private SourceSpan span(int startLine, int startColumn, int endLine, int endColumn) {
            return new SourceSpan(new SourcePosition(file, startLine + 1, startColumn),
                    new SourcePosition(file, endLine + 1, endColumn));
        }

        private void addSpan(String name, SourceSpan value) {
            fieldSpans.computeIfAbsent(name, ignored -> new ArrayList<>()).add(value);
        }

        private Map<String, SourceSpan> referenceSpans(List<RelationshipLocation> relationships) {
            Map<String, SourceSpan> result = new HashMap<>();
            for (var r : relationships) {
                result.put(r.target(), span(r.line() - 1, r.column(), r.line() - 1,
                        r.column() + r.target().length()));
            }
            return result;
        }

        private void requireNext(String expected, Set<String> seen) {
            if (expected.equals(fieldName(lineOrNull()))) return;
            Diagnostic unexpected = unexpected(seen);
            if (unexpected != null && Set.of(
                            "duplicate-field", "unknown-field", "out-of-order-field", "nested-record")
                    .contains(unexpected.code())) {
                throw new ParseFailure(unexpected);
            }
            fail(Math.min(index, Math.max(0, lines.size() - 1)), 1, "missing-field", "required field '%s' is missing".formatted(expected));
        }

        private Scalar parseScalar(String name) {
            String prefix = name + ": ";
            String valueLine = lineOrNull();
            if (valueLine == null || !valueLine.startsWith(prefix)) {
                fail(index, 1, "field-form", "%s must use '%svalue' on one line".formatted(name, prefix));
            }
            String value = valueLine.substring(prefix.length());
            if (value.isEmpty()) {
                fail(index, prefix.length() + 1, "empty-or-padded-scalar", "%s must contain a nonempty value without leading or trailing whitespace".formatted(name));
            }
            if (isScalarBoundaryWhitespace(value.codePointAt(0))) {
                fail(index, prefix.length() + 1, "empty-or-padded-scalar", "%s must contain a nonempty value without leading or trailing whitespace".formatted(name));
            }
            if (isScalarBoundaryWhitespace(value.codePointBefore(value.length()))) {
                int column = prefix.codePointCount(0, prefix.length()) + value.codePointCount(0, value.length());
                fail(index, column, "empty-or-padded-scalar", "%s must contain a nonempty value without leading or trailing whitespace".formatted(name));
            }
            addSpan(name, span(index, prefix.length() + 1, index, endColumn(index)));
            index++;
            return new Scalar(value);
        }

        private Body parseBody(String name, boolean allowMath) {
            int bodyStart = index;
            if (!((name + ":").equals(lineOrNull()))) {
                fail(index, 1, "field-form", "%s must occur alone as '%s:'".formatted(name, name));
            }
            index++;
            List<BodyLine> bodyLines = new ArrayList<>();
            while (index < lines.size()) {
                String valueLine = line();
                if (valueLine.isEmpty()) {
                    bodyLines.add(new BodyLine("", index));
                    index++;
                } else if (valueLine.startsWith("  ")) {
                    bodyLines.add(new BodyLine(valueLine.substring(2), index));
                    index++;
                } else if (Character.isWhitespace(valueLine.charAt(0))) {
                    fail(index, 1, "body-indentation", "%s body lines require two structural spaces".formatted(name));
                } else {
                    break;
                }
            }
            if (bodyLines.stream().noneMatch(bodyLine -> !bodyLine.text().isEmpty())) {
                fail(index - bodyLines.size() - 1, 1, "empty-body", "%s must contain at least one nonblank line".formatted(name));
            }
            addSpan(name, span(bodyStart, 1, index - 1, endColumn(index - 1)));
            return new Body(foldBody(bodyLines, allowMath));
        }

        private List<ContentBlock> foldBody(List<BodyLine> bodyLines, boolean allowMath) {
            List<ContentBlock> blocks = new ArrayList<>();
            List<String> prose = new ArrayList<>();
            for (int bodyIndex = 0; bodyIndex < bodyLines.size(); bodyIndex++) {
                BodyLine bodyLine = bodyLines.get(bodyIndex);
                if (bodyLine.text().isEmpty()) {
                    flushProse(blocks, prose);
                    continue;
                }
                if (allowMath && bodyLine.text().equals("math latex")) {
                    flushProse(blocks, prose);
                    List<String> payload = new ArrayList<>();
                    boolean hasContent = false;
                    boolean closed = false;
                    for (bodyIndex++; bodyIndex < bodyLines.size(); bodyIndex++) {
                        BodyLine mathLine = bodyLines.get(bodyIndex);
                        if (mathLine.text().equals("end math")) {
                            closed = true;
                            break;
                        }
                        if (mathLine.text().isEmpty()) {
                            payload.add("");
                            continue;
                        }
                        if (!mathLine.text().startsWith("  ")) {
                            fail(mathLine.lineIndex(), 3, "math-indentation", "nonblank math payload lines require four source spaces: two for the field and two for math");
                        }
                        String payloadLine = mathLine.text().substring(2);
                        if (!payloadLine.isEmpty()) hasContent = true;
                        payload.add(payloadLine);
                    }
                    if (!closed) {
                        fail(bodyLine.lineIndex(), 3, "unterminated-math", "math latex block is missing 'end math'");
                    }
                    if (!hasContent) {
                        fail(bodyLine.lineIndex(), 3, "empty-math", "math latex payload must not be empty");
                    }
                    blocks.add(new MathBlock("latex", String.join("\n", payload)));
                    continue;
                }
                if (allowMath && bodyLine.text().equals("end math")) {
                    fail(bodyLine.lineIndex(), 3, "unexpected-math-end", "'end math' has no matching math block");
                }
                prose.add(bodyLine.text());
            }
            flushProse(blocks, prose);
            return List.copyOf(blocks);
        }

        private void flushProse(List<ContentBlock> blocks, List<String> prose) {
            if (!prose.isEmpty()) {
                blocks.add(new ProseBlock(String.join(" ", prose)));
                prose.clear();
            }
        }

        private Diagnostic unexpected(Set<String> seen) {
            String valueLine = lineOrNull();
            if (valueLine == null || valueLine.equals("end requirement")) return null;
            if (valueLine.startsWith("requirement ")) {
                return lineDiagnostic(index, 1, "nested-record", "a requirement record cannot begin before the current record ends");
            }
            String name = fieldName(valueLine);
            if (name != null) {
                if (!FIELD_NAMES.contains(name)) {
                    return lineDiagnostic(index, 1, "unknown-field", "unknown field '%s'".formatted(name));
                }
                if (seen.contains(name) && !name.equals("decomposes")) {
                    return lineDiagnostic(index, 1, "duplicate-field", "field '%s' may occur only once".formatted(name));
                }
                return lineDiagnostic(index, 1, "out-of-order-field", "field '%s' is out of order".formatted(name));
            }
            return lineDiagnostic(index, 1, "malformed-record", "expected a field or 'end requirement'");
        }

        private String fieldName(String valueLine) {
            if (valueLine == null) return null;
            Matcher field = FIELD_PATTERN.matcher(valueLine);
            return field.matches() ? field.group(1) : null;
        }

        private boolean isCommentLine(String valueLine) {
            return valueLine.startsWith("#");
        }

        private void skipComments() {
            while (index < lines.size() && isCommentLine(line())) index++;
        }

        private String line() {
            return lines.get(index);
        }

        private String lineOrNull() {
            return index < lines.size() ? line() : null;
        }

        private Diagnostic lineDiagnostic(int lineIndex, int column, String code, String message) {
            return diagnostic(file, lineIndex + 1, column, code, message);
        }

        private void fail(int lineIndex, int column, String code, String message) {
            throw new ParseFailure(lineDiagnostic(Math.max(0, lineIndex), column, code, message));
        }
    }

    @SuppressWarnings("serial")
    private static final class ParseFailure extends RuntimeException {
        final Diagnostic diagnostic;

        ParseFailure(Diagnostic diagnostic) {
            this.diagnostic = diagnostic;
        }
    }

    public static String normalizedInventory(List<Requirement> requirements) {
        StringBuilder output = new StringBuilder();
        requirements.stream()
                .sorted(Comparator.comparing(Requirement::id))
                .forEach(requirement -> {
                    output.append("requirement ").append(escape(requirement.id())).append('\n');
                    output.append("title ").append(escape(requirement.title())).append('\n');
                    appendNullable(output, "allocation", requirement.allocation());
                    appendBlocks(output, "statement", requirement.statement());
                    appendBlocks(output, "rationale", requirement.rationale());
                    appendNullable(output, "source", requirement.source());
                    requirement.decomposes().stream()
                            .sorted()
                            .forEach(target -> output.append("decomposes ").append(escape(target)).append('\n'));
                    output.append("end requirement\n\n");
                });
        if (!output.isEmpty()) output.setLength(output.length() - 1);
        return output.toString();
    }

    private static void appendBlocks(StringBuilder output, String name, List<ContentBlock> blocks) {
        if (blocks == null) {
            output.append(name).append(" null\n");
            return;
        }
        for (ContentBlock block : blocks) {
            if (block instanceof ProseBlock prose) {
                output.append(name).append(" prose ").append(escape(prose.text())).append('\n');
            } else if (block instanceof MathBlock math) {
                output.append(name)
                        .append(" math ")
                        .append(escape(math.language()))
                        .append(' ')
                        .append(escape(math.payload()))
                        .append('\n');
            }
        }
    }

    private static void appendNullable(StringBuilder output, String name, String value) {
        output.append(name);
        if (value == null) output.append(" absent\n");
        else output.append(" value ").append(escape(value)).append('\n');
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r");
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
