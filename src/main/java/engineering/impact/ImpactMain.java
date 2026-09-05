package engineering.impact;

import engineering.artifacts.Command;
import engineering.artifacts.Json;
import engineering.artifacts.Snapshots;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import mundanereq.Versions;

public final class ImpactMain {
    private ImpactMain() {}
    public static void main(String[] args) { System.exit(run(args, System.out, System.err)); }
    public static int run(String[] args, PrintStream out, PrintStream err) {
        int status = 0;
        try {
            if (args.length == 1 && args[0].equals("--help"))
                out.println("Usage: mundane-impact query --root DIRECTORY --from SCOPE:KIND:ID [--depth 1..64] [--] IMPORTS");
            else if (args.length == 1 && args[0].equals("--version"))
                out.println("mundane-impact " + Versions.IMPACT_VERSION + "; " + Versions.IMPACT_ARTIFACT + "; " + Versions.IMPACT_CONTRACT);
            else {
                var options = options(args);
                var result = ImpactAnalyzer.analyze(options.root(), options.input(), options.from(), options.depth());
                out.writeBytes(Json.bytes(result.output())); status = result.status();
            }
        } catch (IllegalArgumentException e) { err.println("invocation-failed: " + e.getMessage()); status = 2; }
        return Command.finish(out, err, status);
    }
    private record Options(Path root, String input, String from, int depth) {}
    private static Options options(String[] args) {
        if (args.length == 0 || !args[0].equals("query")) throw new IllegalArgumentException("expected query");
        Map<String,String> options = new HashMap<>(); String input = null; boolean ended = false;
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (!ended && arg.equals("--")) { ended = true; continue; }
            if (!ended && arg.startsWith("--")) {
                if (!Set.of("--root", "--from", "--depth").contains(arg) || options.containsKey(arg) || i + 1 == args.length)
                    throw new IllegalArgumentException("unknown, duplicate or incomplete option: " + arg);
                options.put(arg, args[++i]);
            } else {
                if (input != null) throw new IllegalArgumentException("expected one input");
                input = arg;
            }
        }
        if (input == null || !options.containsKey("--root") || !options.containsKey("--from"))
            throw new IllegalArgumentException("supply --root, --from and one input");
        String from = options.get("--from");
        if (!from.matches("[A-Za-z0-9][A-Za-z0-9._-]*:(requirement|verification-plan|verification-activity|work-item):[A-Za-z0-9][A-Za-z0-9._-]*"))
            throw new IllegalArgumentException("expected scoped node SCOPE:KIND:ID");
        String number = options.getOrDefault("--depth", "8");
        if (!number.matches("[0-9]{1,2}")) throw new IllegalArgumentException("depth must be 1..64");
        int depth = Integer.parseInt(number);
        if (depth < 1 || depth > 64) throw new IllegalArgumentException("depth must be 1..64");
        Path root = Path.of(options.get("--root")).toAbsolutePath().normalize();
        return new Options(root, new Snapshots(root).argument(Path.of(input)), from, depth);
    }
}
