package engineering.work;

import static engineering.artifacts.Checks.*;
import static engineering.artifacts.Json.object;
import engineering.artifacts.Problem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import mundanereq.Versions;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.lowlevel.Compose;
import org.snakeyaml.engine.v2.api.lowlevel.Parse;
import org.snakeyaml.engine.v2.events.*;
import org.snakeyaml.engine.v2.exceptions.Mark;
import org.snakeyaml.engine.v2.exceptions.MarkedYamlEngineException;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;
import org.snakeyaml.engine.v2.nodes.*;
import org.snakeyaml.engine.v2.schema.CoreSchema;

/** YAML presentation validation belongs to work items, independently of requirements. */
final class WorkYaml {
    private WorkYaml() {}
    static Map<String,Object> parse(String file,String source) {
        Map<String,Object> declaration=object("path",file,"line",1,"column",1);
        try {
            Node root=compose(file,source);
            declaration=point(file,root.getStartMark().orElseThrow());
            var values=map(value(file,root));
            required(values,"format","id","kind","title","status","body");
            if(!Versions.WORK_SOURCE.equals(values.remove("format")))throw new IllegalArgumentException("unsupported work source format");
            if(!values.containsKey("dependencies"))values.put("dependencies",List.of());if(!values.containsKey("relations"))values.put("relations",List.of());
            // Explicit null is invalid; only omitted annotations/lists receive defaults.
            var planning=new TreeMap<String,Object>();for(String k:List.of("stage","type","condition","unlocks","statusNote"))planning.put(k,"");
            if(values.containsKey("planning"))planning.putAll(map(values.get("planning")));
            values.put("planning",planning);WorkValues.validate(values);
            Node id=((MappingNode)root).getValue().stream().filter(t->((ScalarNode)t.getKeyNode()).getValue().equals("id")).findFirst().orElseThrow().getValueNode();
            return object("values",values,"location",point(file,id.getStartMark().orElseThrow()),"metadataLocation",declaration);
        } catch(MarkedYamlEngineException e) {
            throw new Problem("invalid-work-source",e.getProblem(),e.getProblemMark().map(m->point(file,m)).orElse(declaration));
        } catch(YamlEngineException|IllegalArgumentException e) {
            throw new Problem("invalid-work-source",e.getMessage(),declaration);
        }
    }
    /** The same bounded presentation checks for semantic parsing and editor origins. */
    static Node compose(String file,String source) {
        var settings=LoadSettings.builder().setSchema(new CoreSchema()).setAllowDuplicateKeys(false)
            .setMaxAliasesForCollections(0).setCodePointLimit(1024*1024).setUseMarks(true).build();
            int documents=0,depth=0;
            var containers=new java.util.ArrayDeque<int[]>();
            for(Event event:new Parse(settings).parseString(source)) {
                String failure=null;
                // Composition may expand merge keys: reject them while keys are still explicit.
                if(event instanceof ScalarEvent||event instanceof CollectionStartEvent) {
                    if(!containers.isEmpty()) {
                        int[] parent=containers.peek();
                        if(parent[0]==1&&parent[1]%2==0&&event instanceof ScalarEvent scalar&&scalar.getValue().equals("<<"))failure="merge keys are not supported";
                        parent[1]++;
                    }
                    if(event instanceof CollectionStartEvent)containers.push(new int[]{event instanceof MappingStartEvent?1:0,0});
                }
                if(event instanceof CollectionEndEvent)containers.pop();
                if(event instanceof DocumentStartEvent d && (++documents>1||d.getSpecVersion().isPresent()||!d.getTags().isEmpty()))failure="expected one document without directives";
                if(event instanceof AliasEvent||event instanceof NodeEvent n&&n.getAnchor().isPresent())failure="anchors and aliases are not supported";
                if(event instanceof ScalarEvent s&&s.getTag().isPresent()||event instanceof CollectionStartEvent c&&c.getTag().isPresent())failure="explicit tags are not supported";
                if(event instanceof CollectionStartEvent&&++depth>16)failure="collection nesting exceeds 16";
                if(event instanceof CollectionEndEvent)depth--;
                if(failure!=null)throw new Problem("invalid-work-source",failure,point(file,event.getStartMark().orElseThrow()));
            }
        return new Compose(settings).composeString(source).orElseThrow(()->new IllegalArgumentException("expected a work-item document"));
    }
    private static Map<String,Object> point(String file,Mark mark) {return object("path",file,"line",mark.getLine()+1,"column",mark.getColumn()+1);}
    static Object value(String file,Node node) {
        if(node instanceof ScalarNode scalar) {
            if(Tag.STR.equals(scalar.getTag()))return scalar.getValue();
            if(Tag.NULL.equals(scalar.getTag()))return null;
            throw new Problem("invalid-work-source","expected string, not an implicitly typed scalar",point(file,node.getStartMark().orElseThrow()));
        }
        if(node instanceof SequenceNode sequence) {var result=new ArrayList<Object>();for(Node child:sequence.getValue())result.add(value(file,child));return result;}
        if(node instanceof MappingNode mapping) {
            var result=new TreeMap<String,Object>();
            for(NodeTuple entry:mapping.getValue()) {
                Node key=entry.getKeyNode();
                if(!(key instanceof ScalarNode scalar)||!Tag.STR.equals(key.getTag()))throw new Problem("invalid-work-source","mapping keys must be strings",point(file,key.getStartMark().orElseThrow()));
                String name=scalar.getValue();
                if(name.equals("<<")||result.containsKey(name))throw new Problem("invalid-work-source","duplicate or merge key "+name,point(file,key.getStartMark().orElseThrow()));
                result.put(name,value(file,entry.getValueNode()));
            }
            return result;
        }
        throw new IllegalArgumentException("unsupported YAML node");
    }
}
