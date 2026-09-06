package mundanereq.cli;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import mundanereq.Interpreter;
import mundanereq.SourceFormat;

/** Behavioral checks of YAML source, output delivery and snapshot safety. */
public final class YamlWorkflowTest {
    private static final String BASIC = "format: \"mundanereq-yaml-0.3\"\nrequirements:\n"
            + "  - id: \"001\"\n    title: \"status: ready # Δ 😀\"\n    statement: \"The device shall retain data.\"\n";
    private YamlWorkflowTest() {}

    public static void run() throws Exception {
        sourceRules();
        Path temp = Files.createTempDirectory("mundanereq-yaml-test-");
        try {
            cliBehavior(temp);
            snapshotSafety(temp);
            outputFailures(temp);
        } finally { delete(temp); }
        System.out.println("PASS YAML rules, formatter snapshot protection and CLI output failures");
    }

    static void require(boolean condition, Object message) {
        if (!condition) throw new AssertionError(message);
    }

    private static Interpreter.Result parse(String text) {
        return Interpreter.interpretSources(List.of(new Interpreter.Source("case.mreq.yaml", text.getBytes(StandardCharsets.UTF_8))), SourceFormat.YAML_03);
    }

    private static void sourceRules() {
        var corpus = Interpreter.interpretInputs(List.of(Path.of("conformance/0.3/valid")));
        require(corpus.valid(), corpus.diagnostics());
        require(parse(BASIC).valid(), parse(BASIC).diagnostics());
        var authoring = Interpreter.interpretInputs(List.of(Path.of("conformance/0.3/authoring")), SourceFormat.YAML_03);
        require(authoring.valid(), authoring.diagnostics());
        require(((Interpreter.MathBlock) authoring.byId().get("CHILD").statement().get(1)).payload().equals("i = 1\n\nj = i + 1"), "literal math decoding");
        require(((Interpreter.ProseBlock) authoring.byId().get("TOP").statement().getFirst()).text().equals("The logger shall retain measurements for review."), "folded paragraph decoding");

        List<String> invalid = List.of(
                BASIC.replace("\"001\"", "001"), BASIC.replace("\"001\"", "true"),
                BASIC.replace("\"001\"", "\"-bad\""), BASIC.replace("\"001\"", "\"A\\n\""),
                BASIC.replace("\"status: ready # Δ 😀\"", "status # ready"),
                BASIC.replace("\"status: ready # Δ 😀\"", "\"bad\\tvalue\""),
                BASIC.replace("\"status: ready # Δ 😀\"", "\" trailing \""),
                BASIC.replace("\"status: ready # Δ 😀\"", "null"),
                BASIC.replace("    title:", "    unknown:"), BASIC.replace("    title:", "    id: \"002\"\n    title:"),
                BASIC + "    decomposes: [\"missing\"]\n", BASIC + "    decomposes: [\"001\", \"001\"]\n",
                BASIC + "    allocation: \"\"\n", BASIC + "    rationale: []\n", BASIC + "    decomposes: []\n",
                BASIC.replace("\"001\"", "&name \"001\""), BASIC.replace("\"001\"", "*name"),
                BASIC.replace("\"001\"", "!!str \"001\""), "---\n" + BASIC + "---\n" + BASIC,
                "%YAML 1.2\n---\n" + BASIC, BASIC.replace("mundanereq-yaml-0.3", "unknown"),
                BASIC.substring(BASIC.indexOf("requirements:")), BASIC.replace("\"The device shall retain data.\"", "|\n      first\n      second"),
                BASIC.replace("\"The device shall retain data.\"", ">\n      wrapped text"),
                BASIC + "    rationale: {prose: \"wrong shape\"}\n", BASIC + "    source: false\n",
                BASIC + "    statement: \"duplicate\"\n", BASIC.replace("    title:", "   title:"),
                "format: \"mundanereq-yaml-0.3\"\nrequirements: []\n", "\ufeff" + BASIC,
                BASIC.stripTrailing(), BASIC.replace("title:", "title:\t"), BASIC.replace("title:", "title:\r"));
        for (String text : invalid) require(!parse(text).valid(), "accepted invalid source: " + text);
        // Seeded generation exercises semantic fidelity, Unicode and reordered mappings.
        Random random = new Random(42);
        for (int i = 0; i < 100; i++) {
            String id = "R" + random.nextInt(100000);
            String text = BASIC.replace("\"001\"", "\"" + id + "\"");
            var result = parse(text);
            require(result.valid() && result.byId().containsKey(id), result.diagnostics());
            require(parse(text.replace("id:", "bogus:")).valid() == false, "mutation survived");
        }
        var two = parse("format: \"mundanereq-yaml-0.3\"\nrequirements:\n  - id: \"A\"\n  - id: \"B\"\n");
        require(two.diagnostics().size() == 4, two.diagnostics());
        require(two.diagnostics().stream().allMatch(d -> d.line() >= 3), two.diagnostics());
        require(!parse("format: \"mundanereq-yaml-0.3\"\nrequirements: " + "[".repeat(17) + "\"x\"" + "]".repeat(17) + "\n").valid(), "nesting bound");
        String many = "format: \"mundanereq-yaml-0.3\"\nrequirements:\n" + "  - id: \"A\"\n".repeat(10001);
        require(parse(many).diagnostics().getFirst().code().equals("yaml-limit"), "record limit");
        require(parse("#" + "x".repeat(8 * 1024 * 1024) + "\n").diagnostics().getFirst().code().equals("yaml-limit"), "byte limit");
        require(parse("format: \"mundanereq-yaml-0.3\"\nrequirements:\n" + "  - id: \"A\"\n".repeat(100)).diagnostics().size() == 100, "diagnostic bound");
        var duplicate = Interpreter.interpretSources(List.of(new Interpreter.Source("a", BASIC.getBytes(StandardCharsets.UTF_8)),
                new Interpreter.Source("b", BASIC.getBytes(StandardCharsets.UTF_8))), SourceFormat.YAML_03);
        require(duplicate.diagnostics().stream().anyMatch(d -> d.code().equals("duplicate-id")), duplicate);
        var partial = Interpreter.interpretSources(List.of(new Interpreter.Source("a", (BASIC + "    decomposes: [\"X\"]\n").getBytes(StandardCharsets.UTF_8)),
                new Interpreter.Source("b", "bad: [\n".getBytes(StandardCharsets.UTF_8))), SourceFormat.YAML_03);
        require(!partial.valid() && partial.diagnostics().stream().noneMatch(d -> d.code().equals("dangling-reference")), partial);
        var badUtf = Interpreter.interpretSources(List.of(new Interpreter.Source("bad", new byte[] {(byte) 0xff})), SourceFormat.YAML_03);
        require(badUtf.diagnostics().getFirst().code().equals("invalid-utf8"), badUtf);
    }

    private static void cliBehavior(Path temp) throws Exception {
        Path yaml = temp.resolve("source.mreq.yaml"); Files.writeString(yaml, BASIC.replace("\n", "\r\n"));
        Files.writeString(temp.resolve("unrelated.yaml"), "not requirement input");
        require(call(ValidatorMain::run, "--source=yaml-0.3", temp.toString()).status == 0, "YAML discovery");
        require(call(ValidatorMain::run, yaml.toString()).status == 0, "YAML default");
        require(call(ValidatorMain::run, "--source=unknown", yaml.toString()).status == 2, "unknown source format");
        require(call(TraceMain::run, "--source=yaml-0.3", "impact", "001", yaml.toString()).status == 0, "YAML trace");
        require(call(FormatterMain::run, "--source=yaml-0.3", "--check", yaml.toString()).status == 1, "CRLF needs formatting");
        require(call(FormatterMain::run, "--source=yaml-0.3", "--write", yaml.toString()).status == 0, "YAML write");
        require(Files.readString(yaml).equals(BASIC), "formatter changed source content");
        require(call(FormatterMain::run, "--source=yaml-0.3", "--check", yaml.toString()).status == 0, "idempotence");
        Files.writeString(yaml, BASIC.replace("id:", "bad:")); byte[] invalid = Files.readAllBytes(yaml);
        require(call(FormatterMain::run, "--source=yaml-0.3", "--write", yaml.toString()).status == 2, "invalid formatting");
        require(Arrays.equals(invalid, Files.readAllBytes(yaml)), "invalid input rewritten");
    }

    private static void snapshotSafety(Path temp) throws Exception {
        for (String kind : List.of("edit", "delete", "replace")) {
            Path dir = Files.createDirectory(temp.resolve(kind)); List<Path> paths = new ArrayList<>();
            for (String id : List.of("A", "B", "C")) {
                Path file = dir.resolve(id + ".mreq.yaml");
                Files.writeString(file, BASIC.replace("001", id).replace("\n", "\r\n")); paths.add(file);
            }
            var selection = Interpreter.selectInputs(paths); Map<Path, byte[]> formatted = new LinkedHashMap<>();
            for (var source : selection.sources()) formatted.put(Path.of(source.file()), new String(source.bytes(), StandardCharsets.UTF_8).replace("\r\n", "\n").getBytes(StandardCharsets.UTF_8));
            Path target = paths.get(1); byte[] bytes = Files.readAllBytes(target);
            if (kind.equals("edit")) Files.writeString(target, "external edit\n");
            else if (kind.equals("delete")) Files.delete(target);
            else { Path held = dir.resolve("held"); Files.move(target, held); Files.write(target, bytes); }
            ByteArrayOutputStream errors = new ByteArrayOutputStream();
            int code = FormatterMain.writeFiles(selection.sources(), formatted, new PrintStream(OutputStream.nullOutputStream()), new PrintStream(errors));
            require(code == 2 && errors.toString(StandardCharsets.UTF_8).contains("Changed:") && errors.toString(StandardCharsets.UTF_8).contains("Unprocessed:"), kind + errors);
            require(!Files.readString(paths.get(0)).contains("\r"), "first file not completed");
            require(Files.readString(paths.get(2)).contains("\r"), "later file changed");
            if (kind.equals("edit")) require(Files.readString(target).equals("external edit\n"), "external edit overwritten");
            if (kind.equals("replace")) require(Arrays.equals(bytes, Files.readAllBytes(target)), "replacement overwritten");
            if (kind.equals("delete")) require(!Files.exists(target), "deleted file recreated");
            try (var files = Files.list(dir)) { require(files.noneMatch(p -> p.toString().endsWith(".tmp")), "temporary leak"); }
            if (kind.equals("replace")) {
                require(call(FormatterMain::run, "--write", dir.toString()).status == 0, "retry failed");
                require(call(FormatterMain::run, "--check", dir.toString()).status == 0, "retry not idempotent");
            }
        }
    }

    private static void outputFailures(Path temp) throws Exception {
        for (Command command : List.<Command>of(ValidatorMain::run, FormatterMain::run, TraceMain::run, CompileMain::run)) {
            for (String option : List.of("--help", "--version")) {
                for (int fail : List.of(0, 10, -1)) {
                    PrintStream out = new PrintStream(new FaultStream(fail));
                    require(command.run(new String[] {option}, out, new PrintStream(OutputStream.nullOutputStream())) == 2, "output failure success");
                }
                PrintStream closed = new PrintStream(new ByteArrayOutputStream()); closed.close();
                require(command.run(new String[] {option}, closed, new PrintStream(OutputStream.nullOutputStream())) == 2, "closed output success");
            }
            require(command.run(new String[] {}, new PrintStream(OutputStream.nullOutputStream()), new PrintStream(new FaultStream(0))) == 2, "diagnostic failure");
            require(command.run(new String[] {}, new PrintStream(new FaultStream(0)), new PrintStream(new FaultStream(0))) == 2, "both streams failure");
        }
        Path bad = temp.resolve("bad.mreq.yaml"); Files.writeString(bad, "invalid\n");
        require(ValidatorMain.run(new String[] {bad.toString()}, new PrintStream(OutputStream.nullOutputStream()), new PrintStream(new FaultStream(0))) == 2, "diagnostic failure must override source status 1");
        require(ValidatorMain.run(new String[] {"conformance/0.3/valid"}, new PrintStream(new FaultStream(10)), new PrintStream(OutputStream.nullOutputStream())) == 2, "normal validator output failure");
    }

    private static final class FaultStream extends OutputStream {
        int remaining;
        FaultStream(int remaining) { this.remaining = remaining; }
        @Override public void write(int value) throws IOException { if (remaining == 0) throw new IOException("injected write"); if (remaining > 0) remaining--; }
        @Override public void flush() throws IOException { if (remaining < 0) throw new IOException("injected flush"); }
    }

    interface Command { int run(String[] args, PrintStream out, PrintStream err); }
    record Call(int status, String out, String err) {}
    static Call call(Command command, String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(); ByteArrayOutputStream err = new ByteArrayOutputStream();
        int status = command.run(args, new PrintStream(out, true, StandardCharsets.UTF_8), new PrintStream(err, true, StandardCharsets.UTF_8));
        return new Call(status, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
    }
    static void delete(Path path) throws IOException {
        try (var paths = Files.walk(path)) { for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(p); }
    }
}
