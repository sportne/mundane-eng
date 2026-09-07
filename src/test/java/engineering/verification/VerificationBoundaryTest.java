package engineering.verification;

import engineering.artifacts.Checks;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class VerificationBoundaryTest {
    private VerificationBoundaryTest() {}
    public static void run() throws Exception {
        PlanYamlTest.run();
        Path root=Path.of("experiments/0028-verification-contract").toAbsolutePath();
        String[] plan={"--root",root.toString(),root.resolve("source").toString()};
        String[] verify={"--root",root.toString(),"--plan",root.resolve("fixtures/plan.json").toString(),root.resolve("fixtures/imports.json").toString()};
        for(boolean compiler:List.of(true,false)) for(int limit:List.of(0,29,Integer.MAX_VALUE)) {
            PrintStream out=new PrintStream(new OutputStream(){private int count;
                @Override public void write(int value)throws IOException{if(count++>=limit)throw new IOException("prefix failed");}
                @Override public void flush()throws IOException{throw new IOException("flush failed");}});
            int status=compiler?PlanMain.run(plan,out,sink()):VerifyMain.run(verify,out,sink());
            if(status!=2)throw new AssertionError("output failure succeeded");
        }
        PrintStream closed=sink();closed.close();
        if(PlanMain.run(plan,sink(),closed)!=2||VerifyMain.run(verify,sink(),closed)!=2)throw new AssertionError("closed stderr succeeded");
        Path temporary=Files.createTempDirectory("plan-snapshot-");
        try {
            for(String name:List.of("plan.yaml"))Files.copy(root.resolve("source").resolve(name),temporary.resolve(name));
            var result=PlanCompiler.compile(temporary,temporary,()->{
                try{Files.writeString(temporary.resolve("plan.yaml"),"changed\n");}catch(IOException ex){throw new IllegalStateException(ex);}});
            if(result.status()!=2||Boolean.TRUE.equals(result.output().get("complete"))||!Checks.list(result.output().get("coverage")).isEmpty())throw new AssertionError("changed plan published complete");
        } finally {try(var paths=Files.walk(temporary)){for(Path path:paths.sorted(java.util.Comparator.reverseOrder()).toList())Files.delete(path);}}
        System.out.println("PASS plan/analyzer prefix, flush and stderr failures; plan snapshot change suppresses publication");
    }
    private static PrintStream sink(){return new PrintStream(new ByteArrayOutputStream());}
}
