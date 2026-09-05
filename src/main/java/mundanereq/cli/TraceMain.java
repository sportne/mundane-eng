package mundanereq.cli;

import java.io.PrintStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import mundanereq.Interpreter;
import mundanereq.SourceFormat;
import mundanereq.Versions;
import mundanereq.trace.TraceAnalyzer;

/** Focused decomposition trace command selected by Research 0016. */
public final class TraceMain {
    static final String TOOL_VERSION = Versions.TRACE_VERSION;
    static final String SOURCE_CONTRACT = Versions.SOURCE_CUSTOM;

    private enum Operation {
        PARENTS("parents", "Direct higher-level requirements for %s:"),
        CHILDREN("children", "Direct lower-level requirements for %s:"),
        HIGHER("higher", "Higher-level decomposition paths from %s:"),
        IMPACT("impact", "Lower-level impact paths to %s:");

        private final String command;
        private final String header;

        Operation(String command, String header) {
            this.command = command;
            this.header = header;
        }

        static Operation parse(String value) {
            for (Operation operation : values()) {
                if (operation.command.equals(value)) return operation;
            }
            return null;
        }
    }

    private TraceMain() {}

    public static void main(String[] arguments) {
        System.exit(run(arguments, System.out, System.err));
    }

    static int run(String[] arguments, PrintStream out, PrintStream err) {
        try {
            SourceInvocation selected = SourceInvocation.parse(arguments,true);
            return CommandOutput.finish(out, err, runSelected(selected.arguments(), out, err, selected.format()));
        } catch (IllegalArgumentException exception) {
            err.println(exception.getMessage());
            return CommandOutput.finish(out, err, 2);
        }
    }

    private static int runSelected(String[] arguments, PrintStream out, PrintStream err, SourceFormat sourceFormat) {
        AttributeInvocation attributes=AttributeInvocation.parse(arguments,sourceFormat);arguments=attributes.arguments();
        if (arguments.length == 1 && arguments[0].equals("--help")) {
            out.print(usage());
            out.println("Optional leading selector: --source=custom-0.2 or --source=yaml-0.3 or --source=yaml-0.4; YAML 0.4 accepts --attribute-schema PATH");
            return 0;
        }
        if (arguments.length == 1 && arguments[0].equals("--version")) {
            out.printf("mundanereq-trace %s; source contract %s%n", TOOL_VERSION, sourceFormat.contract);
            if(sourceFormat==SourceFormat.YAML_04)out.println(Versions.TRACE_ATTRIBUTE_CONTRACT);
            return 0;
        }
        if (arguments.length < 3) {
            err.print(usage());
            return 2;
        }

        Operation operation = Operation.parse(arguments[0]);
        if (operation == null) {
            err.println("unknown trace operation: " + arguments[0]);
            err.print(usage());
            return 2;
        }
        String queryId = arguments[1];
        if (!Interpreter.isValidRequirementId(queryId)) {
            err.printf("query:1:1: invalid-query-id: '%s' is not a valid requirement identifier%n", queryId);
            return 2;
        }

        List<Path> inputs = new ArrayList<>();
        try {
            for (int index = 2; index < arguments.length; index++) inputs.add(Path.of(arguments[index]));
        } catch (InvalidPathException exception) {
            err.println("invalid input path: " + exception.getMessage());
            return 2;
        }

        Interpreter.Selection selection = Interpreter.selectInputs(inputs, sourceFormat);
        if (!selection.valid()) {
            renderDiagnostics(selection.diagnostics(), err);
            return 2;
        }
        mundanereq.AttributeSchema schema=attributes.schema()==null?null:mundanereq.AttributeSchema.read(attributes.schema(),null);
        Interpreter.Result result = Interpreter.interpretSources(selection.sources(), sourceFormat,schema);
        if (!result.valid()) {
            renderDiagnostics(result.diagnostics(), err);
            return 2;
        }
        if (!result.byId().containsKey(queryId)) {
            err.printf(
                    "query:1:1: missing-requirement: requirement '%s' does not exist in the selected source set%n",
                    queryId);
            return 2;
        }

        TraceAnalyzer analyzer = new TraceAnalyzer(result);
        out.println(operation.header.formatted(queryId));
        switch (operation) {
            case PARENTS -> renderDirect(analyzer.parents(queryId), out);
            case CHILDREN -> renderDirect(analyzer.children(queryId), out);
            case HIGHER -> renderTransitive(analyzer.higher(queryId), out);
            case IMPACT -> renderTransitive(analyzer.impact(queryId), out);
        }
        return 0;
    }

    private static void renderDirect(List<String> ids, PrintStream out) {
        if (ids.isEmpty()) {
            out.println("(none)");
        } else {
            ids.forEach(out::println);
        }
    }

    private static void renderTransitive(TraceAnalyzer.TransitiveResult result, PrintStream out) {
        if (result.paths().isEmpty()) {
            out.println("(none)");
        } else {
            result.paths().forEach(path -> out.printf(
                    "%d %s%n", path.distance(), String.join(" -> ", path.path())));
        }
        result.cycles().forEach(cycle -> out.println("Cycle observed among: " + String.join(" ", cycle)));
    }

    private static void renderDiagnostics(List<Interpreter.Diagnostic> diagnostics, PrintStream err) {
        diagnostics.forEach(diagnostic -> err.printf(
                "%s:%d:%d: %s: %s%n",
                diagnostic.file(),
                diagnostic.line(),
                diagnostic.column(),
                diagnostic.code(),
                diagnostic.message()));
    }

    private static String usage() {
        return "Usage: mundanereq-trace parents ID FILE_OR_DIRECTORY...\n"
                + "       mundanereq-trace children ID FILE_OR_DIRECTORY...\n"
                + "       mundanereq-trace higher ID FILE_OR_DIRECTORY...\n"
                + "       mundanereq-trace impact ID FILE_OR_DIRECTORY...\n"
                + "       mundanereq-trace --help\n"
                + "       mundanereq-trace --version\n";
    }
}
