package mundanereq.cli;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import mundanereq.Interpreter;
import mundanereq.SourceFormat;

/** Observable recovery, strict-command barriers and bounded replayable mutations. */
public final class RecoveryVerificationTest {
    private RecoveryVerificationTest() {}
    public static void run() throws Exception {
        Path root = Path.of("experiments/0031-parser-recovery/invalid");
        var custom = Interpreter.interpretInputs(List.of(root.resolve("custom")));
        require(ids(custom).equals(Set.of("BEFORE", "BETWEEN", "AFTER", "REF")), "valid custom neighbors retained");
        require(!custom.syntaxComplete() && !custom.valid(), "custom recovery marked incomplete");
        require(custom.diagnostics().stream().map(d -> d.code()+":"+d.line()+":"+d.column()).toList()
                .equals(List.of("field-form:8:1", "field-form:21:1")), "two primary diagnostics, no dangling cascade");
        var yaml = Interpreter.interpretInputs(List.of(root.resolve("yaml")), SourceFormat.YAML_03);
        require(ids(yaml).equals(Set.of("BEFORE", "BETWEEN", "AFTER")), "valid YAML neighboring mappings retained");
        require(!yaml.syntaxComplete() && !yaml.valid() && yaml.diagnostics().size()==2, "YAML incomplete");
        String y = Files.readString(root.resolve("yaml/records.mreq.yaml"));
        require(parse(y.replace("mundanereq-yaml-0.3", "unknown"), SourceFormat.YAML_03).requirements().isEmpty(), "unknown YAML envelope has no records");
        require(parse(y + "[\n", SourceFormat.YAML_03).requirements().isEmpty(), "malformed YAML is file-fatal");
        require(!parse(y + "[\n", SourceFormat.YAML_03).syntaxComplete(), "fatal YAML incomplete");
        String good = record("GOOD");
        String bad = record("BAD").replace("title: BAD", "title:");
        require(ids(parse(good+"\n"+bad+"\n"+record("AFTER"))).equals(Set.of("GOOD", "AFTER")), "neighbors");
        require(ids(parse(good.replace("end requirement\n", "")+"\n"+record("AFTER"))).equals(Set.of("AFTER")), "missing terminator recovery");
        require(ids(parse("requirement ???\n\n"+record("AFTER"))).equals(Set.of("AFTER")), "malformed opener recovery");
        String math = "requirement MATH\ntitle: Math\nstatement:\n  math latex\n    requirement FAKE\n    end requirement\n";
        require(ids(parse(math+"  end math\nend requirement\n\n"+record("AFTER"))).equals(Set.of("MATH", "AFTER")), "math lookalikes opaque");
        require(ids(parse(good+"\n"+math+"\n"+record("AFTER"))).equals(Set.of("GOOD")), "unterminated math stops speculative recovery");
        // An indented complete-looking requirement within a bad body is never a record.
        require(ids(parse(bad.replace("  Shall respond.", "  requirement FAKE\n  title: Fake\n  statement:\n    Shall fake.\n  end requirement")+"\n"+good)).equals(Set.of("GOOD")), "body lookalike excluded");
        for (SourceFormat format : SourceFormat.values()) {
            var broken = Interpreter.interpretSources(List.of(new Interpreter.Source("broken",new byte[]{(byte)0xff}),
                    source("neighbor", format==SourceFormat.CUSTOM_02 ? good : "{\"format\":\""+format.contract+"\",\"requirements\":[{\"id\":\"GOOD\",\"title\":\"Good\",\"statement\":\"Shall respond.\",\"decomposes\":[\"ABSENT\"]}]}\n")), format);
            require(!broken.syntaxComplete() && broken.diagnostics().size()==1 && broken.diagnostics().getFirst().code().equals("invalid-utf8"), "decode failure prevents cascades");
        }
        var semantic = parse(record("A").replace("end requirement", "decomposes: ABSENT\nend requirement"));
        require(semantic.syntaxComplete() && !semantic.valid() && semantic.diagnostics().getFirst().code().equals("dangling-reference"), "complete parsing still validates relationships");
        Random random = new Random(1403);
        for (int iteration=0; iteration<100; iteration++) {
            StringBuilder input = new StringBuilder();
            Set<String> expected = new java.util.HashSet<>();
            for (int n=0;n<30;n++) {
                String id="R"+n; String value=record(id);
                if (random.nextBoolean()) value=value.replace("title: "+id, "title:"); else expected.add(id);
                input.append(value).append('\n');
            }
            var result = parse(input.toString());
            require(ids(result).equals(expected) && result.diagnostics().size()==30-expected.size(), "seed 1403 iteration "+iteration);
        }
        var limited = parse((bad+"\n").repeat(200));
        require(!limited.valid() && limited.diagnostics().size()==100 && limited.diagnostics().stream().anyMatch(d->d.code().equals("recovery-limit")), "bounded recovery diagnostics");
        for (String mode : List.of("custom", "yaml")) {
            String selector="--source="+(mode.equals("custom") ? "custom-0.2" : "yaml-0.3");
            Path dir=root.resolve(mode);
            require(ValidatorMain.run(new String[]{selector,dir.toString()},sink(),sink())==1, "strict validation rejects recovery");
            var original = Files.readAllBytes(dir.resolve(mode.equals("custom") ? "records.mreq" : "records.mreq.yaml"));
            require(FormatterMain.run(new String[]{selector,"--write",dir.toString()},sink(),sink())==2, "write-back rejects recovery");
            require(java.util.Arrays.equals(original,Files.readAllBytes(dir.resolve(mode.equals("custom") ? "records.mreq" : "records.mreq.yaml"))), "source preserved");
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            require(CompileMain.run(new String[]{selector,"--root",dir.toString(),dir.toString()},new PrintStream(bytes),sink())==1, "compiler rejects incomplete model");
            String compiled=bytes.toString(StandardCharsets.UTF_8);
            require(compiled.contains("\"complete\":false") && compiled.contains("\"requirements\":[]"), "no partial compiled publication");
        }
        System.out.println("PASS recovery: custom/YAML neighbors, locations, no cascades, math boundaries, strict command barriers, 100 seeded cases and diagnostic cap");
    }
    private static String record(String id) { return "requirement "+id+"\ntitle: "+id+"\nstatement:\n  Shall respond.\nend requirement\n"; }
    private static Interpreter.Source source(String file,String value) { return new Interpreter.Source(file,value.getBytes(StandardCharsets.UTF_8)); }
    private static Interpreter.Result parse(String value) { return parse(value,SourceFormat.CUSTOM_02); }
    private static Interpreter.Result parse(String value,SourceFormat format) { return Interpreter.interpretSources(List.of(source("fixture",value)),format); }
    private static Set<String> ids(Interpreter.Result result) { return result.requirements().stream().map(Interpreter.Requirement::id).collect(Collectors.toSet()); }
    private static PrintStream sink() { return new PrintStream(new ByteArrayOutputStream()); }
    private static void require(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
}
