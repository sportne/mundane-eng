package engineering.change;
import engineering.artifacts.Json;
import java.util.*;
public final class ChangeTest {
    private ChangeTest(){}
    public static void run() throws Exception {
        var differences=Change.differences(Json.object("limit",1),Json.object("limit",2),"",Map.of("limit","timing"));
        if(!differences.getFirst().get("classification").equals("timing"))throw new AssertionError();
        if(!Change.differences(Json.object("future",1),Json.object("future",2),"",Map.of()).getFirst().get("classification").equals("unknown"))throw new AssertionError();
        if(!Change.differences(Json.object("limit",1),Json.object("limit",new java.math.BigDecimal("1.00")),"",Map.of("limit","timing")).isEmpty())throw new AssertionError("numeric spelling changed meaning");
        var absent=Change.differences(Map.of(),Json.object("limit",null),"",Map.of("limit","timing"));
        if(absent.size()!=1||!Boolean.FALSE.equals(absent.getFirst().get("beforePresent")))throw new AssertionError("absent versus null was lost");
        var graph=Json.object("nodes",List.of(Json.object("scope","a"),Json.object("scope","b"),Json.object("scope","c")),"edges",List.of(Json.object("from","a","to","b"),Json.object("from","b","to","a"),Json.object("from","b","to","c")));
        if(Change.paths(graph,"a",1).size()!=1||Change.paths(graph,"a",2).size()!=2)throw new AssertionError("bounded cyclic traversal");
        var root=java.nio.file.Files.createTempDirectory("change-validation-cache-");
        try {
            java.nio.file.Files.writeString(root.resolve("resource.txt"),"original");
            String pin=engineering.artifacts.Snapshots.hash("original".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            java.nio.file.Files.writeString(root.resolve("source.yaml"),"format: fixture-source\nresource:\n  path: resource.txt\n  sha256: "+pin+"\n");
            int[] calls={0};
            var owner=new engineering.domain.Model.Domain(){
                public String kind(){return "fixture";}public String source(){return "fixture-source";}public String format(){return "fixture-artifact";}public String version(){return "1";}public String contract(){return "fixture-contract";}
                public void validate(Map<String,Object> v,engineering.domain.Model.Context c){calls[0]++;c.readPinned(engineering.artifacts.Checks.map(v.get("resource")));}
                public String view(Map<String,Object> a,engineering.domain.Model.Context c){return "";}
            };
            var artifact=engineering.domainsource.Source.compile("source.yaml",null,new engineering.domain.Model.Context(root,Map.of()),owner);
            calls[0]=0;var c=new engineering.domain.Model.Context(root,Map.of());
            engineering.domain.Model.validateSelected(artifact,owner,c);
            engineering.domain.Model.validateSelected(engineering.artifacts.Checks.map(Json.read(Json.bytes(artifact))),owner,c);
            if(calls[0]!=1)throw new AssertionError("identical selected revisions revalidated");
            var deep=c;for(int i=0;i<16;i++)deep=deep.child();
            try{engineering.domain.Model.validateSelected(artifact,owner,deep);throw new AssertionError("cache bypassed import depth bound");}catch(IllegalArgumentException expected){}
            java.nio.file.Files.writeString(root.resolve("resource.txt"),"changed");
            engineering.domain.Model.validateSelected(artifact,owner,c);
            try{c.snapshots.recheck();throw new AssertionError("cached transitive mutation accepted");}catch(engineering.artifacts.Problem expected){}
            try{engineering.domain.Model.validateSelected(artifact,owner,new engineering.domain.Model.Context(root,Map.of()));throw new AssertionError("validation cache escaped its command");}catch(engineering.artifacts.Problem expected){}
        }finally{try(var files=java.nio.file.Files.walk(root)){for(var file:files.sorted(Comparator.reverseOrder()).toList())java.nio.file.Files.delete(file);}}
        System.out.println("PASS change: domain field classification, unknown changes, bounded deterministic cyclic reachability");
    }
}
