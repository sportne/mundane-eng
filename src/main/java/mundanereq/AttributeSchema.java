package mundanereq;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.charset.StandardCharsets;
import java.util.*;
import mundane.attributes.AttributeRules;
import mundane.json.Json;
import mundanereq.source.SourcePosition;
import mundanereq.source.SourceSpan;

/** Explicit bounded declaration snapshot with retained JSON token provenance. */
public record AttributeSchema(Interpreter.Source source,Map<String,Object> definition,
        Map<String,SourceSpan> locations,List<Interpreter.Diagnostic> diagnostics) {
    public static final int MAX_BYTES=1024*1024;
    public boolean valid(){return diagnostics.isEmpty();}
    public String name(){return (String)definition.get("name");}
    public static AttributeSchema read(Path input,Path root) {
        Path path=input.toAbsolutePath().normalize();
        if(root!=null) {
            if(!path.startsWith(root))throw new IllegalArgumentException("attribute schema is outside --root");
            try {if(!path.toRealPath().startsWith(root.toRealPath()))throw new IllegalArgumentException("attribute schema resolves outside --root");}catch(IOException ignored){/* Report unavailable below. */}
        }
        try {
            var attributes=Files.readAttributes(path,BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS);
            if(!attributes.isRegularFile())throw new IOException("declaration must be an explicit regular non-symlink file");
            byte[] bytes;try(var stream=Files.newInputStream(path,LinkOption.NOFOLLOW_LINKS)){bytes=stream.readNBytes(MAX_BYTES+1);}
            return parse(new Interpreter.Source(path.toString(),bytes,attributes.fileKey()));
        }catch(IOException e){return new AttributeSchema(new Interpreter.Source(path.toString(),new byte[0]),null,Map.of(),List.of(new Interpreter.Diagnostic(path.toString(),1,1,"attribute-schema-unavailable",e.getMessage())));}
    }
    public static AttributeSchema parse(Interpreter.Source source) {
        byte[] bytes=source.bytes();String text=new String(bytes,StandardCharsets.UTF_8);Json.Document doc=null;
        try {
            if(bytes.length>MAX_BYTES||text.startsWith("\ufeff")||!text.endsWith("\n")||text.replace("\r\n","").contains("\r"))throw new AttributeRules.Invalid("","invalid declaration encoding/termination/size");
            doc=Json.document(bytes,16);var definition=AttributeRules.definition(doc.value());var locations=new TreeMap<String,SourceSpan>();
            for(String key:AttributeRules.map(definition.get("attributes"),"/attributes").keySet())locations.put(key,span(source.file(),text,doc.values().get(Json.pointer("/attributes",key))));
            return new AttributeSchema(source,definition,Map.copyOf(locations),List.of());
        }catch(IllegalArgumentException e) {
            int offset=0;String code="attribute-schema-invalid";
            if(e instanceof Json.Failure f){offset=f.offset;if(f.duplicate)code="attribute-schema-duplicate";}
            if(e instanceof AttributeRules.Invalid invalid&&doc!=null){var range=(e.getMessage().startsWith("unknown field")||e.getMessage().startsWith("invalid or reserved")&&invalid.pointer.startsWith("/attributes/"))?doc.keys().get(invalid.pointer):doc.values().get(invalid.pointer);if(range==null)range=doc.keys().get(invalid.pointer);if(range!=null)offset=range.start();}
            var point=position(source.file(),text,Math.min(offset,text.length()));
            return new AttributeSchema(source,null,Map.of(),List.of(new Interpreter.Diagnostic(source.file(),point.line(),point.column(),code,e.getMessage())));
        }
    }
    public AttributeSchema relocated(String path) {
        var locations=new TreeMap<String,SourceSpan>();this.locations.forEach((k,v)->locations.put(k,new SourceSpan(new SourcePosition(path,v.start().line(),v.start().column()),new SourcePosition(path,v.end().line(),v.end().column()))));
        return new AttributeSchema(new Interpreter.Source(path,source.bytes(),source.fileKey()),definition,Map.copyOf(locations),diagnostics.stream().map(d->new Interpreter.Diagnostic(path,d.line(),d.column(),d.code(),d.message())).toList());
    }
    public void recheck() throws IOException {
        Path path=Path.of(source.file());var a=Files.readAttributes(path,BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS);
        byte[] bytes;try(var stream=Files.newInputStream(path,LinkOption.NOFOLLOW_LINKS)){bytes=stream.readNBytes(MAX_BYTES+1);}
        if(!a.isRegularFile()||source.fileKey()!=null&&!Objects.equals(source.fileKey(),a.fileKey())||!Arrays.equals(bytes,source.bytes()))throw new IOException("attribute-schema-changed: selected declaration changed since reading");
    }
    private static SourceSpan span(String file,String text,Json.Range range){return new SourceSpan(position(file,text,range.start()),position(file,text,range.end()));}
    private static SourcePosition position(String file,String text,int offset){int line=1,start=0;for(int i=0;i<offset;i++)if(text.charAt(i)=='\n'){line++;start=i+1;}return new SourcePosition(file,line,text.codePointCount(start,offset)+1);}
}
