package engineering.operations;
import engineering.artifacts.*;
import java.util.*;
import java.nio.file.*;
public final class OperationsTest {
    private OperationsTest() {}
    public static void run() throws Exception {
        var v=Json.object("candidates",List.of(Json.object("id","A","predecessor",null),Json.object("id","B","predecessor","A")));
        if(!Operations.descendant(v,"B","A")||Operations.descendant(v,"A","B"))throw new AssertionError("history direction");
        v.put("candidates",List.of(Json.object("id","A","predecessor","B"),Json.object("id","B","predecessor","A")));
        try{Operations.descendant(v,"B","X");throw new AssertionError("cycle accepted");}catch(IllegalArgumentException expected){}
        var dir=Files.createTempDirectory("operations-snapshots-");
        try {
            Files.writeString(dir.resolve("native"),"original");var snapshots=new Snapshots(dir);
            var first=snapshots.read("native");if(first!=snapshots.read("native")||snapshots.captured().size()!=1)throw new AssertionError("shared resource charged twice");
            try{snapshots.read("native",2);throw new AssertionError("cached size bypass");}catch(Problem expected){}
            Files.writeString(dir.resolve("native"),"changed");if(snapshots.read("native").sha256().equals(first.sha256()))throw new AssertionError("stale repeated read");try{snapshots.recheck();throw new AssertionError("changed shared resource accepted");}catch(Problem expected){}
        }finally{Files.deleteIfExists(dir.resolve("native"));Files.delete(dir);}
        System.out.println("PASS operations: predecessor direction/cycles and bounded shared snapshots with final mutation detection");
    }
}
