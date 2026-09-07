package mundanereq.cli;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import mundanereq.*;
import mundane.json.Json;

public final class AttributeCompilationTest {
    private AttributeCompilationTest() {}
    public static void run() throws Exception {
        Path root=Files.createTempDirectory("attribute-compile-");
        try {
            Path schema=root.resolve("schema.yaml"),source=root.resolve("source.mreq.yaml");
            byte[] definition=Files.readAllBytes(Path.of("examples/attributes/requirement-attributes.yaml")),requirements=Files.readAllBytes(Path.of("examples/attributes/system.mreq.yaml"));
            String[] args={"--root",root.toString(),"--attribute-schema",schema.toString(),source.toString()};
            for(boolean changeSchema:new boolean[]{true,false}) {
                Files.write(schema,definition);Files.write(source,requirements);var bytes=new ByteArrayOutputStream();
                int code=CompileMain.compile(args,SourceFormat.YAML_04,new PrintStream(bytes),new PrintStream(new ByteArrayOutputStream()),()->{try{Files.writeString(changeSchema?schema:source,"changed\n");}catch(IOException e){throw new UncheckedIOException(e);}});
                var a=(Map<?,?>)Json.read(bytes.toByteArray());require(code==2&&Boolean.FALSE.equals(a.get("complete"))&&((List<?>)a.get("requirements")).isEmpty(),"changed snapshot suppresses publication");
                require(((Map<?,?>)((List<?>)a.get("diagnostics")).getFirst()).get("ruleId").equals(changeSchema?"attribute-schema-changed":"input-changed"),"snapshot rule");
            }
            Files.write(schema,definition);Files.write(source,requirements);
            var result=Interpreter.interpretSources(List.of(new Interpreter.Source("source.mreq.yaml",requirements)),SourceFormat.YAML_04,AttributeSchema.read(schema,null));
            try {mundanereq.compile.SemanticArtifact.emit(List.of(),result,SourceFormat.YAML_03);throw new AssertionError("attributes silently downgraded");}catch(IllegalArgumentException expected){require(expected.getMessage().contains("old"),"explicit old-output rejection");}
            PrintStream broken=new PrintStream(new OutputStream(){int n;public void write(int v)throws IOException{if(++n>8)throw new IOException("partial");}});
            List<String> full=new ArrayList<>();full.add("--source=yaml-0.4");full.addAll(List.of(args));
            require(CompileMain.run(full.toArray(String[]::new),broken,new PrintStream(new ByteArrayOutputStream()))==2,"attribute compiler output failure");
        }finally{try(var paths=Files.walk(root)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}}
        System.out.println("PASS attribute compilation: source/schema rechecks suppress records, old-output downgrade rejection and actual partial stdout");
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
