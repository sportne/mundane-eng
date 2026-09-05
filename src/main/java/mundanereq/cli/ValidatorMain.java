package mundanereq.cli;

import java.io.PrintStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Files;
import mundanereq.diagnostics.Sarif;
import java.util.ArrayList;
import java.util.List;
import mundanereq.Interpreter;
import mundanereq.SourceFormat;
import mundanereq.Versions;

/** Focused command-line validator for the provisional 0.2 source contract. */
public final class ValidatorMain {
    static final String TOOL_VERSION = Versions.VALIDATE_VERSION;
    static final String SOURCE_CONTRACT = Versions.SOURCE_CUSTOM;

    private ValidatorMain() {}

    public static void main(String[] arguments) {
        System.exit(run(arguments, System.out, System.err));
    }

    static int run(String[] arguments, PrintStream out, PrintStream err) {
        try {
            SourceInvocation selected = SourceInvocation.parse(arguments);
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
            out.printf("mundanereq-validate %s; source contract %s%n", TOOL_VERSION, sourceFormat.contract);
            return 0;
        }

        List<Path> inputs = new ArrayList<>();
        boolean optionsEnded = false;
        boolean sarif = false;
        Path root = null;
        try {
            for (int index=0; index<arguments.length; index++) {
                String argument=arguments[index];
                if (!optionsEnded && argument.equals("--")) {
                    optionsEnded = true;
                } else if (!optionsEnded && argument.equals("--output=sarif")) {
                    if (sarif) throw new IllegalArgumentException("duplicate --output option");
                    sarif=true;
                } else if (!optionsEnded && argument.equals("--root")) {
                    if (root!=null || index+1==arguments.length) throw new IllegalArgumentException("supply --root once with a directory");
                    root=Path.of(arguments[++index]).toAbsolutePath().normalize();
                } else if (!optionsEnded && argument.startsWith("-")) {
                    err.println("unknown option: " + argument);
                    err.print(usage());
                    return 2;
                } else {
                    inputs.add(Path.of(argument));
                }
            }
        } catch (InvalidPathException exception) {
            err.println("invalid input path: " + exception.getMessage());
            return 2;
        }

        if (inputs.isEmpty()) {
            err.print(usage());
            return 2;
        }

        if (sarif) {
            if (root==null || !Files.isDirectory(root)) throw new IllegalArgumentException("SARIF requires --root DIR");
            for (Path input:inputs) if (!input.toAbsolutePath().normalize().startsWith(root)) {
                throw new IllegalArgumentException("input is outside --root: "+input);
            }
            mundanereq.AttributeSchema schema=attributes.schema()==null?null:mundanereq.AttributeSchema.read(attributes.schema(),root);
            Interpreter.Selection selected=Interpreter.selectInputs(inputs,sourceFormat);
            Interpreter.Result result=selected.valid() ? Interpreter.interpretSources(selected.sources(),sourceFormat,schema)
                    : new Interpreter.Result(List.of(),java.util.Map.of(),java.util.Map.of(),selected.diagnostics(),selected.sources().size());
            int status=result.diagnostics().stream().anyMatch(ValidatorMain::isOperational) ? 2 : result.valid() ? 0 : 1;
            out.writeBytes(Sarif.emit(root,selected.sources(),result,sourceFormat,status));
            return status;
        }
        if (root!=null) throw new IllegalArgumentException("--root requires --output=sarif");
        mundanereq.AttributeSchema schema=attributes.schema()==null?null:mundanereq.AttributeSchema.read(attributes.schema(),null);
        Interpreter.Selection selection=Interpreter.selectInputs(inputs,sourceFormat);
        Interpreter.Result result=selection.valid()?Interpreter.interpretSources(selection.sources(),sourceFormat,schema):new Interpreter.Result(List.of(),java.util.Map.of(),java.util.Map.of(),selection.diagnostics(),selection.sources().size());
        if (!result.diagnostics().isEmpty()) {
            result.diagnostics().forEach(diagnostic -> err.println(render(diagnostic)));
            return result.diagnostics().stream().anyMatch(ValidatorMain::isOperational) ? 2 : 1;
        }

        int relationships = result.outgoing().values().stream().mapToInt(java.util.Set::size).sum();
        out.printf(
                "Validated %d requirements and %d decomposition relationships from %d %s as %s.%n",
                result.requirements().size(),
                relationships,
                result.fileCount(),
                result.fileCount() == 1 ? "file" : "files",
                sourceFormat.contract);
        return 0;
    }

    private static boolean isOperational(Interpreter.Diagnostic diagnostic) {
        return diagnostic.code().equals("input-unavailable") || diagnostic.code().equals("no-source-files") || diagnostic.code().equals("attribute-schema-unavailable");
    }

    private static String render(Interpreter.Diagnostic diagnostic) {
        return "%s:%d:%d: %s: %s".formatted(
                diagnostic.file(),
                diagnostic.line(),
                diagnostic.column(),
                diagnostic.code(),
                diagnostic.message());
    }

    private static String usage() {
        return "Usage: mundanereq-validate [--] FILE_OR_DIRECTORY...\n"
                + "       mundanereq-validate --output=sarif --root DIR [--] FILE_OR_DIRECTORY...\n"
                + "       mundanereq-validate --help\n"
                + "       mundanereq-validate --version\n";
    }
}
