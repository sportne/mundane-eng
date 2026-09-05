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
            if(args.length==1&&args[0].equals("--help"))out.println("Usage: mundane-work compile --root DIRECTORY MANIFEST");
            else if(args.length==1&&args[0].equals("--version"))out.println("mundane-work "+Versions.WORK_VERSION+"; "+Versions.WORK_CONTRACT+"; "+Versions.WORK_SOURCE+"; "+Versions.WORK_ARTIFACT+"; "+Versions.WORK_ANALYSIS);
            else {
                if(args.length<1||!args[0].equals("compile"))throw new IllegalArgumentException("expected compile");
                Map<String,String> opts=new TreeMap<>();String input=null;boolean ended=false;
                for(int i=1;i<args.length;i++) {
                    String arg=args[i];if(!ended&&arg.equals("--")){ended=true;continue;}
                    if(!ended&&arg.startsWith("--")) {
                        if(!arg.equals("--root")||opts.containsKey(arg)||i+1==args.length)throw new IllegalArgumentException("unknown/duplicate/missing option "+arg);
                        opts.put(arg,args[++i]);
                    } else {if(input!=null)throw new IllegalArgumentException("expected one input");input=arg;}
                }
                if(input==null||!opts.containsKey("--root"))throw new IllegalArgumentException("supply --root and input");
                Path root=Path.of(opts.get("--root")).toAbsolutePath().normalize();if(!Files.isDirectory(root))throw new IllegalArgumentException("root is not a directory");
                String file=new Snapshots(root).argument(Path.of(input));var result=WorkCompiler.compile(root,file);out.writeBytes(Json.bytes(result.output()));status=result.status();
            }
        } catch(IllegalArgumentException e) {err.println("invocation-failed: "+e.getMessage());status=2;}
        return Command.finish(out,err,status);
    }
}
