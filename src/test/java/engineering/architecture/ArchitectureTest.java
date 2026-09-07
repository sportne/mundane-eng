package engineering.architecture;

import static engineering.artifacts.Checks.*;
import engineering.artifacts.*;
import engineering.domain.*;
import engineering.domainsource.Source;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class ArchitectureTest {
    private ArchitectureTest() {}
    public static void run() throws Exception {
        Path root=Files.createTempDirectory("architecture-test-");
        try {
            Files.copy(Path.of("examples/ground-control-station/seed/requirements.yaml"),root.resolve("requirements.yaml"));
            Files.copy(Path.of("examples/ground-control-station/seed/attributes.yaml"),root.resolve("attributes.yaml"));
            var bytes=new ByteArrayOutputStream();var err=new ByteArrayOutputStream();
            var process=new ProcessBuilder("java","-cp",System.getProperty("java.class.path"),"mundanereq.cli.CompileMain","--source=yaml-0.4","--root",root.toString(),"--attribute-schema",root.resolve("attributes.yaml").toString(),root.resolve("requirements.yaml").toString()).redirectError(root.resolve("error.txt").toFile()).start();
            bytes.writeBytes(process.getInputStream().readAllBytes());if(process.waitFor()!=0)throw new AssertionError(Files.readString(root.resolve("error.txt")));Files.write(root.resolve("req.json"),bytes.toByteArray());
            var imports=Json.object("format",mundanereq.Versions.DOMAIN_IMPORTS,"imports",List.of(Json.object("scope","gcs-req","kind","requirements","format","mundanereq-requirements-0.2","path","req.json","sha256",Snapshots.hash(bytes.toByteArray()))));
            Files.write(root.resolve("imports.json"),Json.bytes(imports));
            String source=Files.readString(Path.of("examples/ground-control-station/design/architecture.yaml"));Files.writeString(root.resolve("architecture.yaml"),source);
            var domain=new Architecture();var a=Source.compile("architecture.yaml","imports.json",new Model.Context(root,Map.of()),domain);Files.write(root.resolve("architecture.json"),Json.bytes(a));
            var context=new Model.Context(root,Map.of());var read=Model.read(root.resolve("architecture.json"),context,domain);String view=domain.view(read,context);
            if(!view.contains("#L")||!view.contains("IF-TELEMETRY")||!view.contains("flowchart"))throw new AssertionError("missing source-linked architecture views");
            var cases=map(mundane.yaml.Yaml.document(Files.readAllBytes(Path.of("examples/ground-control-station/design/architecture-cases.yaml")),100000,true).value());
            for(Object item:list(cases.get("cases"))) {
                var test=map(item);if(list(test.get("changes")).isEmpty())continue;
                var v=map(Json.read(Json.bytes(a.get("values"))));
                for(Object c:list(test.get("changes")))change(v,map(c));
                reject(()->domain.validate(v,newContext(root,a)));
            }
            for(String from:List.of("mavlink-2-common-sim/1","gcs-intent/1")) {
                Files.writeString(root.resolve("architecture.yaml"),source.replace(from,"unsupported/9"));reject(()->Source.compile("architecture.yaml","imports.json",new Model.Context(root,Map.of()),domain));
            }
            Files.writeString(root.resolve("architecture.yaml"),source);
            reject(()->Source.compile("architecture.yaml","imports.json",new Model.Context(root,Map.of()),domain,()->{try{Files.writeString(root.resolve("architecture.yaml"),source+"\n# changed\n");}catch(IOException e){throw new UncheckedIOException(e);}}));
            if(!domain.view(read,context).contains("revision unavailable"))throw new AssertionError("stale source link");
            Files.writeString(root.resolve("req.json"),"{}\n");reject(()->Model.read(root.resolve("architecture.json"),new Model.Context(root,Map.of()),domain));
            reject(()->mundane.yaml.Yaml.document("n: 12\n".getBytes(java.nio.charset.StandardCharsets.UTF_8),100));
            for(String bad:List.of("n: .inf\n","n: 0x12\n","n: &a 1\n","n: 1\nn: 2\n","n: 1e9999\n"))reject(()->mundane.yaml.Yaml.document(bad.getBytes(java.nio.charset.StandardCharsets.UTF_8),100,true));
            var failure=new PrintStream(new OutputStream(){@Override public void write(int b)throws IOException{throw new IOException("failure");}});
            if(ArchitectureMain.run(new String[]{"--version"},failure,new PrintStream(err))!=2)throw new AssertionError("output failure");
        }finally{try(var paths=Files.walk(root)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}}
        System.out.println("PASS architecture: compiled boundaries, design mutations, unsupported profiles, numeric opt-in, source origins, snapshot/pin and output failures");
    }
    private static Model.Context newContext(Path root,Map<String,Object> a){var c=new Model.Context(root,Map.of());c.select(a.get("imports"));return c;}
    @SuppressWarnings("unchecked")
    private static void change(Map<String,Object> data,Map<String,Object> c) {
        var path=list(c.get("path"));Object target=data;
        for(Object part:path.subList(0,path.size()-1))target=target instanceof List<?> a?a.get(((Number)part).intValue()):((Map<String,Object>)target).get(part);
        Object key=path.get(path.size()-1);
        if(target instanceof Map<?,?> raw){var m=(Map<String,Object>)raw;if(Boolean.TRUE.equals(c.get("remove")))m.remove(key);else m.put((String)key,c.get("value"));}
        else ((List<Object>)target).set(((Number)key).intValue(),c.get("value"));
    }
    private static void reject(Runnable r){try{r.run();throw new AssertionError("invalid input accepted");}catch(IllegalArgumentException|Problem expected){}}
}
