package engineering.work;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import engineering.artifacts.Checks;
import engineering.artifacts.Json;

public final class WorkBoundaryTest {
    private WorkBoundaryTest() {}
    public static void run() {
        var bytes=new ByteArrayOutputStream();var errors=new ByteArrayOutputStream();
        int code=WorkMain.run(new String[]{"compile","--root",".","examples/work-items/work-items.json"},new PrintStream(bytes),new PrintStream(errors));
        if(code!=0)throw new AssertionError(errors.toString(StandardCharsets.UTF_8));
        var a=Checks.map(Json.read(bytes.toByteArray()));if(WorkArtifact.validate(a,"work.json").size()!=2)throw new AssertionError("missing items");
        for(int prefix:new int[]{0,37}) {
            PrintStream broken=new PrintStream(new OutputStream(){int left=prefix;@Override public void write(int value)throws IOException{if(left--<=0)throw new IOException("closed output");}});
            if(WorkMain.run(new String[]{"compile","--root",".","examples/work-items/work-items.json"},broken,new PrintStream(new ByteArrayOutputStream()))!=2)throw new AssertionError("failed output accepted");
        }
        PrintStream closed=new PrintStream(new ByteArrayOutputStream());closed.close();
        if(WorkMain.run(new String[]{"--version"},new PrintStream(new ByteArrayOutputStream()),closed)!=2)throw new AssertionError("closed Java stderr accepted");
        bytes.reset();if(WorkMain.run(new String[]{"compile","--root",".","--root",".","examples/work-items/work-items.json"},new PrintStream(bytes),new PrintStream(errors))!=2||bytes.size()!=0)throw new AssertionError("bad invocation emitted artifact");
        System.out.println("PASS work-item compilation boundary and output failure handling");
    }
}
