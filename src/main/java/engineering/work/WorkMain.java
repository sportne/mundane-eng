package engineering.work;

import engineering.artifacts.*;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import mundanereq.Versions;

public final class WorkMain {
    private WorkMain() {}
    public static void main(String[] args) {System.exit(run(args,System.out,System.err));}
    public static int run(String[] args,PrintStream out,PrintStream err) {
        int status=0;
        try {
            if(args.length==1&&args[0].equals("--help"))out.println("Usage: mundane-work compile --root DIRECTORY MANIFEST; analyze --root DIRECTORY --imports MANIFEST ARTIFACT");
            else if(args.length==1&&args[0].equals("--version"))out.println("mundane-work "+Versions.WORK_VERSION+"; "+Versions.WORK_CONTRACT+"; "+Versions.WORK_SOURCE+"; "+Versions.WORK_ARTIFACT+"; "+Versions.WORK_ANALYSIS);
            else {
                if(args.length<1||!java.util.Set.of("compile","analyze").contains(args[0]))throw new IllegalArgumentException("expected compile or analyze");
                Map<String,String> opts=new TreeMap<>();String input=null;boolean ended=false;
                for(int i=1;i<args.length;i++) {
                    String arg=args[i];if(!ended&&arg.equals("--")){ended=true;continue;}
                    if(!ended&&arg.startsWith("--")) {
                        if(!(arg.equals("--root")||args[0].equals("analyze")&&arg.equals("--imports"))||opts.containsKey(arg)||i+1==args.length)throw new IllegalArgumentException("unknown/duplicate/missing option "+arg);
                        opts.put(arg,args[++i]);
                    } else {if(input!=null)throw new IllegalArgumentException("expected one input");input=arg;}
                }
                if(input==null||!opts.containsKey("--root"))throw new IllegalArgumentException("supply --root and input");
                Path root=Path.of(opts.get("--root")).toAbsolutePath().normalize();if(!Files.isDirectory(root))throw new IllegalArgumentException("root is not a directory");
                var paths=new Snapshots(root);String file=paths.argument(Path.of(input));
                if(args[0].equals("analyze")&&!opts.containsKey("--imports"))throw new IllegalArgumentException("analyze requires --imports");
                WorkResult result=args[0].equals("compile")?WorkCompiler.compile(root,file):WorkAnalyzer.analyze(root,file,paths.argument(Path.of(opts.get("--imports"))));
                out.writeBytes(Json.bytes(result.output()));status=result.status();
            }
        } catch(IllegalArgumentException e) {err.println("invocation-failed: "+e.getMessage());status=2;}
        return Command.finish(out,err,status);
    }
}
