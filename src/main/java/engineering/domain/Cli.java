package engineering.domain;

import engineering.artifacts.*;
import java.io.PrintStream;
import java.nio.file.*;
import java.util.*;

/** Shared invocation and checked output; source callbacks live outside this component. */
public final class Cli {
    private Cli() {}
    public record Options(String command,Path root,String imports,List<String> inputs) {}
    @FunctionalInterface public interface Action { Object execute(Options options); }
    public static int run(String[] args,PrintStream out,PrintStream err,Model.Domain domain,Action action) {
        int status=0;
        try {
            if(Arrays.equals(args,new String[]{"--version"}))out.println("mundane-"+domain.kind()+" "+domain.version()+"; "+domain.contract()+"; "+domain.source()+"; "+domain.format());
            else if(Arrays.equals(args,new String[]{"--help"}))out.println("Usage: mundane-"+domain.kind()+" COMMAND --root DIRECTORY [--imports SELECTION.json] INPUT...\nCommands are documented in distribution/"+domain.kind()+".md");
            else {
                if(args.length<4)throw new IllegalArgumentException("command, --root and input required");
                Path root=null;String imports=null;List<String> inputs=new ArrayList<>();
                for(int i=1;i<args.length;i++) {
                    if(args[i].equals("--root")&&root==null&&i+1<args.length)root=Path.of(args[++i]).toAbsolutePath().normalize();
                    else if(args[i].equals("--imports")&&imports==null&&i+1<args.length)imports=Checks.path(args[++i]);
                    else if(args[i].startsWith("--"))throw new IllegalArgumentException("unknown/duplicate option");
                    else inputs.add(Checks.path(args[i]));
                }
                if(root==null||inputs.isEmpty())throw new IllegalArgumentException("missing root or input");
                Object result=action.execute(new Options(args[0],root,imports,List.copyOf(inputs)));
                if(result instanceof String text)out.print(text);else out.writeBytes(Json.bytes(result));
            }
        }catch(Problem e){err.writeBytes(Json.bytes(e.diagnostic()));status=e.operational()?2:1;}
        catch(IllegalArgumentException e){err.writeBytes(Json.bytes(Json.object("code","invalid-input","message",String.valueOf(e.getMessage()))));status=1;}
        catch(java.io.UncheckedIOException e){err.println("output-failed: "+e.getMessage());status=2;}
        return Command.finish(out,err,status);
    }
    public static void arity(Options o,int n) {if(o.inputs().size()!=n)throw new IllegalArgumentException("expected "+n+" input(s)");if(!o.command().equals("compile")&&o.imports()!=null)throw new IllegalArgumentException("imports belong to compile");}
}
