package mundanereq.cli;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import mundanereq.*;
import mundane.json.Json;

public final class AttributeValidationTest {
    private AttributeValidationTest() {}
    public static void run() throws Exception {
        Path root=Path.of("examples/attributes");var schema=AttributeSchema.read(root.resolve("requirement-attributes.yaml"),null);
        var source=new Interpreter.Source("source.mreq.yaml",Files.readAllBytes(root.resolve("system.mreq.yaml")));
        var result=Interpreter.interpretSources(List.of(source),SourceFormat.YAML_04,schema);
        require(result.valid(),"valid attributes");require(result.byId().get("SYS-002").attributes().equals(Map.of("discipline","electronics")),"no optional synthesis");
        var origin=result.origins().getFirst().attributes().get("discipline");require(origin.name().start().line()==9&&origin.value().start().column()==19,"attribute name/value spans");
        var broken=new Interpreter.Source(source.file(),new String(source.bytes(),java.nio.charset.StandardCharsets.UTF_8).replace("discipline: \"software\"","discipline: \"invalid\"").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        result=Interpreter.interpretSources(List.of(broken),SourceFormat.YAML_04,schema);require(!result.valid()&&!result.syntaxComplete()&&result.byId().keySet().equals(Set.of("SYS-002")),"valid-neighbor recovery");
        var merged=new Interpreter.Source(source.file(),new String(source.bytes(),java.nio.charset.StandardCharsets.UTF_8).replace("discipline: \"software\"","<<: {discipline: \"software\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        require(!Interpreter.interpretSources(List.of(merged),SourceFormat.YAML_04,schema).valid(),"no hidden merge defaults");
        Json.document(("[".repeat(16)+"0"+"]".repeat(16)).getBytes(),16);
        try {Json.document(("[".repeat(17)+"0"+"]".repeat(17)).getBytes(),16);throw new AssertionError("depth accepted");}catch(Json.Failure expected){require(expected.getMessage().contains("depth"),"depth diagnostic");}
        String yaml=Files.readString(root.resolve("requirement-attributes.yaml"));
        require(AttributeSchema.parse(new Interpreter.Source("schema.yaml",("# project policy\n"+yaml).getBytes(java.nio.charset.StandardCharsets.UTF_8))).valid(),"YAML comments accepted");
        for(String invalid:List.of(
            yaml.replace("mundanereq-attributes-yaml-0.1","mundanereq-attribute-schema-0.1"),
            yaml.replace("required: true","required: \"true\""),
            yaml.replace("required: true","required: TRUE"),
            yaml.replace("type: enum","type: enum\n    type: enum"),
            yaml.replace("attributes:","attributes: &attrs"),
            yaml.replace("type: enum","<<: {type: enum}"),
            yaml.replace("type: enum","type: !!str enum"),
            yaml+"---\n{}\n",yaml.replace("name: logger-metadata","name: 123"))) {
            require(!AttributeSchema.parse(new Interpreter.Source("schema.yaml",invalid.getBytes(java.nio.charset.StandardCharsets.UTF_8))).valid(),"reject unsupported YAML declaration: "+invalid);
        }
        var duplicate=AttributeSchema.parse(new Interpreter.Source("schema.yaml",yaml.replace("type: enum","type: enum\n    type: enum").getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        require(duplicate.diagnostics().getFirst().code().equals("attribute-schema-duplicate")&&duplicate.diagnostics().getFirst().column()==5,"duplicate YAML key token location");
        String[] args={"--source=yaml-0.4","--attribute-schema",root.resolve("requirement-attributes.yaml").toString(),root.resolve("system.mreq.yaml").toString()};
        for(boolean flush:new boolean[]{false,true}) {
            PrintStream failing=new PrintStream(new OutputStream(){int written;public void write(int value)throws IOException{if(!flush&&++written>8)throw new IOException("partial stdout");}public void flush()throws IOException{if(flush)throw new IOException("flush failure");}});
            require(ValidatorMain.run(args,failing,new PrintStream(new ByteArrayOutputStream()))==2,"attribute stdout failure");
        }
        System.out.println("PASS attribute values/origins, optional absence, recovery, merge rejection, depth limits and output failures");
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
