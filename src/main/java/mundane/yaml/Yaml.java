package mundane.yaml;

import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.*;
import mundane.json.Json;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.lowlevel.Compose;
import org.snakeyaml.engine.v2.api.lowlevel.Parse;
import org.snakeyaml.engine.v2.events.*;
import org.snakeyaml.engine.v2.exceptions.*;
import org.snakeyaml.engine.v2.nodes.*;
import org.snakeyaml.engine.v2.schema.CoreSchema;

/** Bounded YAML presentation for independently validated declaration and plan values. */
public final class Yaml {
    private Yaml() {}
    public record Point(int line,int column) {}
    public record Range(Point start,Point end) {}
    public record Document(Object value,Map<String,Range> keys,Map<String,Range> values) {}
    public static final class Failure extends IllegalArgumentException {
        private static final long serialVersionUID=1L;
        public final transient Point point;
        public final boolean duplicate;
        Failure(String message,Point point,boolean duplicate){super(message);this.point=point;this.duplicate=duplicate;}
    }
    private static Point point(Mark m){return new Point(m.getLine()+1,m.getColumn()+1);}
    private static Failure fail(String message,Node n,boolean duplicate){return new Failure(message,point(n.getStartMark().orElseThrow()),duplicate);}
    public static Document document(byte[] bytes,int maxBytes) {
        try {
            if(bytes.length>maxBytes)throw new IllegalArgumentException("YAML source exceeds byte limit");
            String source=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            if(source.startsWith("\ufeff")||!source.endsWith("\n")||source.replace("\r\n","").contains("\r"))throw new IllegalArgumentException("invalid YAML encoding or line termination");
            var settings=LoadSettings.builder().setSchema(new CoreSchema()).setAllowDuplicateKeys(false)
                .setMaxAliasesForCollections(0).setCodePointLimit(maxBytes).setUseMarks(true).build();
            int documents=0,depth=0;
            var containers=new ArrayDeque<int[]>();
            for(Event event:new Parse(settings).parseString(source)) {
                String failure=null;
                if(event instanceof ScalarEvent||event instanceof CollectionStartEvent) {
                    if(!containers.isEmpty()) {
                        int[] parent=containers.peek();
                        if(parent[0]==1&&parent[1]%2==0&&event instanceof ScalarEvent scalar&&scalar.getValue().equals("<<"))failure="merge keys are not supported";
                        parent[1]++;
                    }
                    if(event instanceof CollectionStartEvent)containers.push(new int[]{event instanceof MappingStartEvent?1:0,0});
                }
                if(event instanceof CollectionEndEvent)containers.pop();
                if(event instanceof DocumentStartEvent d&&(++documents>1||d.getSpecVersion().isPresent()||!d.getTags().isEmpty()))failure="expected one document without directives";
                if(event instanceof AliasEvent||event instanceof NodeEvent n&&n.getAnchor().isPresent())failure="anchors and aliases are not supported";
                if(event instanceof ScalarEvent s&&s.getTag().isPresent()||event instanceof CollectionStartEvent c&&c.getTag().isPresent())failure="explicit tags are not supported";
                if(event instanceof CollectionStartEvent&&++depth>16)failure="collection depth exceeds 16";
                if(event instanceof CollectionEndEvent)depth--;
                if(failure!=null)throw new Failure(failure,point(event.getStartMark().orElseThrow()),false);
            }
            Node root=new Compose(settings).composeString(source).orElseThrow(()->new IllegalArgumentException("expected a YAML document"));
            Map<String,Range> keys=new TreeMap<>(),values=new TreeMap<>();
            Object value=value(root,"",keys,values);
            return new Document(value,Map.copyOf(keys),Map.copyOf(values));
        } catch(MarkedYamlEngineException e) {
            throw new Failure(e.getProblem(),e.getProblemMark().map(Yaml::point).orElse(new Point(1,1)),false);
        } catch(java.nio.charset.CharacterCodingException e) {
            throw new Failure("invalid UTF-8",new Point(1,1),false);
        } catch(YamlEngineException e) {
            throw new Failure(e.getMessage(),new Point(1,1),false);
        }
    }
    private static Object value(Node node,String pointer,Map<String,Range> keys,Map<String,Range> values) {
        values.put(pointer,new Range(point(node.getStartMark().orElseThrow()),point(node.getEndMark().orElseThrow())));
        if(node instanceof ScalarNode scalar) {
            if(Tag.STR.equals(scalar.getTag()))return scalar.getValue();
            if(Tag.NULL.equals(scalar.getTag()))return null;
            if(Tag.BOOL.equals(scalar.getTag())&&Set.of("true","false").contains(scalar.getValue()))return Boolean.valueOf(scalar.getValue());
            throw fail("expected string or explicit lowercase boolean/null; quote numeric text",node,false);
        }
        if(node instanceof SequenceNode sequence) {
            List<Object> result=new ArrayList<>();int index=0;
            for(Node child:sequence.getValue())result.add(value(child,pointer+"/"+index++,keys,values));
            return result;
        }
        if(node instanceof MappingNode mapping) {
            Map<String,Object> result=new TreeMap<>();
            for(NodeTuple tuple:mapping.getValue()) {
                if(!(tuple.getKeyNode() instanceof ScalarNode key)||!Tag.STR.equals(key.getTag()))throw fail("mapping keys must be strings",tuple.getKeyNode(),false);
                String name=key.getValue(),p=Json.pointer(pointer,name);
                if(name.equals("<<")||result.containsKey(name))throw fail("duplicate or merge key "+name,key,true);
                keys.put(p,new Range(point(key.getStartMark().orElseThrow()),point(key.getEndMark().orElseThrow())));
                result.put(name,value(tuple.getValueNode(),p,keys,values));
            }
            return result;
        }
        throw fail("unsupported YAML node",node,false);
    }
}
