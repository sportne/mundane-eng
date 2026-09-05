package mundanereq.cli;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import mundanereq.Interpreter;
import mundanereq.SourceFormat;
import mundanereq.Versions;
import mundanereq.compile.SemanticArtifact;

/** Independently usable requirement compiler; semantic output contract 0.1. */
public final class CompileMain {
    private CompileMain() {}

    public static void main(String[] arguments) {
        System.exit(run(arguments, System.out, System.err));
    }

    static int run(String[] arguments, PrintStream out, PrintStream err) {
        int status;
        try {
            SourceInvocation selected = SourceInvocation.parse(arguments,true);
            status = compile(selected.arguments(), selected.format(), out, err);
        } catch (IllegalArgumentException exception) {
            err.println("compile-failed: " + exception.getMessage());
            status = 2;
        }
        return CommandOutput.finish(out, err, status);
    }

    private static int compile(String[] arguments, SourceFormat format, PrintStream out, PrintStream err) {return compile(arguments,format,out,err,()->{});}
    static int compile(String[] arguments, SourceFormat format, PrintStream out, PrintStream err,Runnable beforeRecheck) {
        AttributeInvocation attributes=AttributeInvocation.parse(arguments,format);arguments=attributes.arguments();
        if (arguments.length == 1 && arguments[0].equals("--help")) {
            out.print(usage()); return 0;
        }
        if (arguments.length == 1 && arguments[0].equals("--version")) {
            out.printf("mundanereq-compile %s; source contract %s; output %s; command %s%n",
                    Versions.COMPILE_VERSION, format.contract, format==SourceFormat.YAML_04?Versions.REQUIREMENT_ATTRIBUTE_ARTIFACT:Versions.REQUIREMENT_ARTIFACT, format==SourceFormat.YAML_04?Versions.COMPILE_ATTRIBUTE_CONTRACT:Versions.COMPILE_CONTRACT);
            return 0;
        }
        Path root = null;
        List<Path> inputs = new ArrayList<>();
        boolean ended = false;
        for (int i = 0; i < arguments.length; i++) {
            String arg = arguments[i];
            if (!ended && arg.equals("--")) ended = true;
            else if (!ended && arg.equals("--root")) {
                if (root != null || i + 1 == arguments.length) throw new IllegalArgumentException("supply --root exactly once with a directory");
                root = Path.of(arguments[++i]).toAbsolutePath().normalize();
            } else if (!ended && arg.startsWith("-")) throw new IllegalArgumentException("unknown option: " + arg);
            else inputs.add(Path.of(arg).toAbsolutePath().normalize());
        }
        if (root == null || inputs.isEmpty() || !Files.isDirectory(root)) {
            err.print(usage()); return 2;
        }
        for (Path input : inputs) if (!input.startsWith(root)) throw new IllegalArgumentException("input is outside --root: " + input);
        mundanereq.AttributeSchema schema=attributes.schema()==null?null:mundanereq.AttributeSchema.read(attributes.schema(),root);
        var scopedSchema=schema==null?null:schema.relocated(relative(root,schema.source().file()));
        Interpreter.Selection selection = Interpreter.selectInputs(inputs, format);
        List<Interpreter.Source> sources = new ArrayList<>();
        for (var source : selection.sources()) {
            sources.add(new Interpreter.Source(relative(root, source.file()), source.bytes(), source.fileKey()));
        }
        Interpreter.Result result;
        if (selection.valid()) result = Interpreter.interpretSources(sources, format,scopedSchema);
        else {
            List<Interpreter.Diagnostic> diagnostics = new ArrayList<>();
            for (var d : selection.diagnostics()) diagnostics.add(new Interpreter.Diagnostic(relative(root, d.file()),
                    d.line(), d.column(), d.code(), d.message().replace(root.toString(), ".")));
            result = new Interpreter.Result(List.of(), Map.of(), Map.of(), diagnostics, sources.size());
        }
        beforeRecheck.run();
        if(format==SourceFormat.YAML_04&&result.valid()) {
            String changedPath=relative(root,selection.sources().getFirst().file());String code="input-changed";
            try {
                for(var source:selection.sources()) {changedPath=relative(root,source.file());recheck(source);}
                if(schema!=null) {changedPath=relative(root,schema.source().file());code="attribute-schema-changed";schema.recheck();}
            }catch(java.io.IOException e) {
                result=new Interpreter.Result(List.of(),Map.of(),Map.of(),List.of(new Interpreter.Diagnostic(changedPath,1,1,code,e.getMessage().replace(root.toString(),"."))),sources.size(),List.of(),false,scopedSchema);
            }
        }
        byte[] artifact;
        try {
            artifact = SemanticArtifact.emit(sources, result, format);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            err.println("serialization-failed: " + exception.getMessage()); return 2;
        }
        out.writeBytes(artifact);
        return result.diagnostics().stream().anyMatch(SemanticArtifact::operational) ? 2 : result.valid() ? 0 : 1;
    }

    private static void recheck(Interpreter.Source source) throws java.io.IOException {
        Path p=Path.of(source.file());var attributes=Files.readAttributes(p,java.nio.file.attribute.BasicFileAttributes.class,java.nio.file.LinkOption.NOFOLLOW_LINKS);
        byte[] current;try(var stream=Files.newInputStream(p,java.nio.file.LinkOption.NOFOLLOW_LINKS)){current=stream.readNBytes(source.bytes().length+1);}
        if(!attributes.isRegularFile()||source.fileKey()!=null&&!java.util.Objects.equals(source.fileKey(),attributes.fileKey())||!java.util.Arrays.equals(source.bytes(),current))throw new java.io.IOException("selected source changed since reading");
    }
    private static String relative(Path root, String file) {
        Path path = Path.of(file).toAbsolutePath().normalize();
        if (!path.startsWith(root)) throw new IllegalArgumentException("selected source is outside --root");
        String value = root.relativize(path).toString().replace(java.io.File.separatorChar, '/');
        return value.isEmpty() ? "." : value;
    }

    private static String usage() {
        return "Usage: mundanereq-compile [--source=custom-0.2|--source=yaml-0.3|--source=yaml-0.4] [--attribute-schema PATH] --root DIRECTORY [--] INPUT...\n"
                + "       mundanereq-compile [--source=...] --help|--version\n";
    }
}
