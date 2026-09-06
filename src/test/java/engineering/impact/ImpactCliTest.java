package engineering.impact;

import static engineering.artifacts.Checks.*;
import static engineering.artifacts.Json.object;
import static engineering.impact.ImpactGraphTest.check;
import engineering.artifacts.Json;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ImpactCliTest {
    private ImpactCliTest() {}
    public static void main(String[] args) throws Exception { exercise(args.length == 0 ? null : Path.of(args[0]).toAbsolutePath().toString()); }
    public static void run() throws Exception { exercise(null); }
    static void writeInputs(Path root) throws IOException {
        List<Object> entries = new ArrayList<>();
        for (Object value : ImpactFixtures.imports()) {
            var e = map(value); var a = map(e.get("artifact"));
            Files.write(root.resolve(text(e.get("path"))), Json.bytes(a));
            entries.add(object("scope", e.get("scope"), "kind", a.get("artifactKind"), "path", e.get("path"), "sha256", e.get("sha256"), "dependsOn", e.get("dependsOn")));
        }
        Files.write(root.resolve("imports.json"), Json.bytes(object("format", "mundane-imports-0.1", "imports", entries)));
    }
    private static void exercise(String binary) throws Exception {
        Path root = Files.createTempDirectory("impact-cli-");
        try {
            writeInputs(root);
            String[] args = {"query", "--root", root.toString(), "--from", "req:requirement:TOP", root.resolve("imports.json").toString()};
            byte[] good = invoke(args, 0, binary);
            var output = map(Json.read(good)); var query = map(output.get("query"));
            check(query.get("truncated").equals(false), "unexpected truncation");
            check(list(query.get("affected")).stream().map(x -> map(x).get("node")).toList().equals(List.of(
                    "plan:verification-activity:TEST", "plan:verification-plan:PLAN", "req:requirement:LOW", "work:work-item:FIX", "work:work-item:FOLLOW")), "wrong affected nodes");
            check(java.util.Arrays.equals(good, invoke(args, 0, binary)), "query nondeterministic");
            String[] bounded = {"query", "--root", root.toString(), "--from", "req:requirement:TOP", "--depth", "1", root.resolve("imports.json").toString()};
            var q = map(map(Json.read(invoke(bounded, 0, binary))).get("query"));
            check(q.get("truncated").equals(true) && list(q.get("affected")).size() == 1, "depth bound hidden");
            var cycle = ImpactGraph.build(List.of(ImpactFixtures.imported("req", ImpactFixtures.requirements(true))));
            check(ImpactQuery.query(cycle, "req:requirement:TOP", 1).get("truncated").equals(false), "visited cycle falsely truncated");
            var unknown = args.clone(); unknown[4] = "req:requirement:ABSENT";
            var failed = map(Json.read(invoke(unknown, 1, binary)));
            check(failed.get("complete").equals(false) && failed.get("query") == null && list(failed.get("nodes")).isEmpty(), "failure published results");
            var malformed = args.clone(); malformed[4] = "TOP"; invoke(malformed, 2, binary);
            var noFile = args.clone(); noFile[5] = root.resolve("absent.json").toString(); invoke(noFile, 2, binary);
            for (String depth : List.of("0", "65", "-1", "1.5", "99999999999999")) {
                var invalid = bounded.clone(); invalid[6] = depth; invoke(invalid, 2, binary);
            }
            for (String name : List.of("--help", "--version", "--")) {
                Files.copy(root.resolve("imports.json"), root.resolve(name));
                var literal = new String[]{"query", "--root", root.toString(), "--from", "req:requirement:TOP", "--", root.resolve(name).toString()};
                invoke(literal, 0, binary);
            }
            for (int limit : List.of(0, 41, Integer.MAX_VALUE)) {
                var broken = new PrintStream(new OutputStream() {
                    private int count;
                    @Override public void write(int value) throws IOException { if (count++ >= limit) throw new IOException("closed pipe"); }
                    @Override public void flush() throws IOException { throw new IOException("flush failed"); }
                });
                check(ImpactMain.run(args, broken, sink()) == 2, "output failure accepted");
            }
            var closed = sink(); closed.close();
            check(ImpactMain.run(args, closed, sink()) == 2 && ImpactMain.run(args, sink(), closed) == 2, "closed stream accepted");
            var changed = ImpactAnalyzer.analyze(root, "imports.json", "req:requirement:TOP", 8, () -> {
                try { Files.writeString(root.resolve("req.json"), "{}\n"); } catch (IOException e) { throw new IllegalStateException(e); }
            });
            check(changed.status() == 2 && list(changed.output().get("edges")).isEmpty(), "changed read published edges");
            invoke(args, 1, binary); // Exact pin now mismatches.
            Files.writeString(root.resolve("imports.json"), "{broken"); invoke(args, 1, binary);
        } finally {
            try (var paths = Files.walk(root)) { for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(p); }
        }
        System.out.println("PASS impact query paths, bounds, cycles, failures, snapshot changes" + (binary == null ? "" : " and JVM/native parity"));
    }
    static byte[] invoke(String[] args, int expected, String binary) throws Exception {
        var out = new ByteArrayOutputStream(); var err = new ByteArrayOutputStream();
        int status = ImpactMain.run(args, new PrintStream(out), new PrintStream(err));
        check(status == expected, "expected " + expected + ", got " + status + ": " + err + out);
        if (binary != null) {
            List<String> command = new ArrayList<>(List.of(binary)); command.addAll(List.of(args));
            Path stdout = Files.createTempFile("impact-native-", ".out"), stderr = Files.createTempFile("impact-native-", ".err");
            try {
                var process = new ProcessBuilder(command).redirectOutput(stdout.toFile()).redirectError(stderr.toFile()).start();
                if (!process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)) { process.destroyForcibly(); throw new AssertionError("native timeout"); }
                check(process.exitValue() == expected && java.util.Arrays.equals(out.toByteArray(), Files.readAllBytes(stdout)) && java.util.Arrays.equals(err.toByteArray(), Files.readAllBytes(stderr)), "native/JVM mismatch");
            } finally { Files.delete(stdout); Files.delete(stderr); }
        }
        return out.toByteArray();
    }
    private static PrintStream sink() { return new PrintStream(new ByteArrayOutputStream()); }
}
