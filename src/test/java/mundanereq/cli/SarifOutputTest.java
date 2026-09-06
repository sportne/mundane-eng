package mundanereq.cli;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Delivery failures remain failures even for otherwise clean SARIF output. */
public final class SarifOutputTest {
    private SarifOutputTest() {}
    public static void run() {
        String root="conformance/0.3/valid";
        String[] args={"--output=sarif","--root",root,root};
        for(int limit:List.of(0,37,Integer.MAX_VALUE)) {
            PrintStream failure=new PrintStream(new OutputStream() {
                private int count;
                @Override public void write(int value) throws IOException { if(count++>=limit) throw new IOException("injected write failure"); }
                @Override public void flush() throws IOException { throw new IOException("injected flush failure"); }
            },false,StandardCharsets.UTF_8);
            require(ValidatorMain.run(args,failure,sink())==2,"SARIF output prefix/flush failure");
        }
        PrintStream closed=sink();closed.close();
        require(ValidatorMain.run(args,closed,sink())==2,"closed stdout");
        closed=sink();closed.close();require(ValidatorMain.run(args,sink(),closed)==2,"closed stderr");
        for(String[] invalid:List.of(new String[]{"--output=sarif",root},
                new String[]{"--output=sarif","--root",root,"--root",root,root},
                new String[]{"--output=sarif","--output=sarif","--root",root,root},
                new String[]{"--output=unknown",root},new String[]{"--root",root,root},
                new String[]{"--output=sarif","--root",root,"README.md"},
                new String[]{"--output=sarif","--root",root})) {
            ByteArrayOutputStream output=new ByteArrayOutputStream();
            require(ValidatorMain.run(invalid,new PrintStream(output),sink())==2 && output.size()==0,"invalid invocation has no SARIF");
        }
        System.out.println("PASS SARIF invocation rejection and closed/prefix/flush output failures");
    }
    private static PrintStream sink(){return new PrintStream(new ByteArrayOutputStream());}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
