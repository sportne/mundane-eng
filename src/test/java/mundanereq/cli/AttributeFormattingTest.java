package mundanereq.cli;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import mundanereq.*;

public final class AttributeFormattingTest {
    private AttributeFormattingTest() {}
    public static void run() throws Exception {
        Path root=Files.createTempDirectory("attribute-write-");
        try {
            Path declaration=root.resolve("schema.json");byte[] original=Files.readAllBytes(Path.of("examples/attributes/requirement-attributes.json"));Files.write(declaration,original);
            var schema=AttributeSchema.read(declaration,null);List<Interpreter.Source> sources=new ArrayList<>();Map<Path,byte[]> formatted=new LinkedHashMap<>();
            for(int i=0;i<3;i++) {Path p=root.resolve(i+".mreq.yaml");Files.writeString(p,"old\r\n");var a=Files.readAttributes(p,java.nio.file.attribute.BasicFileAttributes.class);sources.add(new Interpreter.Source(p.toString(),Files.readAllBytes(p),a.fileKey()));formatted.put(p,"old\n".getBytes());}
            var out=new ByteArrayOutputStream();var err=new ByteArrayOutputStream();
            int status=FormatterMain.writeFiles(sources,formatted,new PrintStream(out),new PrintStream(err),schema,index->{if(index==1)try{Files.writeString(declaration,new String(original).replace("contact","changed contact"));}catch(IOException e){throw new UncheckedIOException(e);}});
            require(status==2&&out.size()==0&&err.toString().contains("attribute-schema-changed")&&err.toString().contains(declaration.toString()),"schema edit is operational failure at declaration");
            require(Files.readString(root.resolve("0.mreq.yaml")).equals("old\n"),"first write recorded");
            require(Files.readString(root.resolve("1.mreq.yaml")).equals("old\r\n")&&Files.readString(root.resolve("2.mreq.yaml")).equals("old\r\n"),"remaining sources preserved");
            require(err.toString().contains("Changed: "+root.resolve("0.mreq.yaml"))&&err.toString().contains("Unprocessed: "+root.resolve("2.mreq.yaml")),"recoverable completed/remaining report");
            require(Files.readString(declaration).contains("changed contact"),"external schema edit retained");
            Path valid=root.resolve("valid.mreq.yaml");Files.write(declaration,original);
            byte[] authored=Files.readString(Path.of("examples/attributes/system.mreq.yaml")).replace("\n","\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);Files.write(valid,authored);
            var declarationSnapshot=AttributeSchema.read(declaration,null);
            var before=Interpreter.interpretSources(List.of(new Interpreter.Source(valid.toString(),authored)),SourceFormat.YAML_04,declarationSnapshot);
            int formattedStatus=FormatterMain.run(new String[]{"--source=yaml-0.4","--attribute-schema",declaration.toString(),"--write",valid.toString()},new PrintStream(new ByteArrayOutputStream()),new PrintStream(new ByteArrayOutputStream()));
            var after=Interpreter.interpretSources(List.of(new Interpreter.Source(valid.toString(),Files.readAllBytes(valid))),SourceFormat.YAML_04,AttributeSchema.read(declaration,null));
            require(formattedStatus==0&&before.valid()&&after.valid()&&before.requirements().equals(after.requirements())&&before.attributeSchema().definition().equals(after.attributeSchema().definition()),"parse-format-parse values and definitions agree");
            try(var files=Files.list(root)){require(files.noneMatch(p->p.getFileName().toString().endsWith(".tmp")),"temporary cleanup");}
        }finally{try(var files=Files.walk(root)){for(Path p:files.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}}
        System.out.println("PASS schema change between formatter writes: completed/remaining paths, preserved external edit and temporary cleanup");
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
