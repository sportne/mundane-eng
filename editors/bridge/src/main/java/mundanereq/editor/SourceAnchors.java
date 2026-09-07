package mundanereq.editor;

import static engineering.artifacts.Json.object;
import java.util.*;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.lowlevel.Compose;
import org.snakeyaml.engine.v2.api.lowlevel.Parse;
import org.snakeyaml.engine.v2.events.*;
import org.snakeyaml.engine.v2.nodes.*;
import org.snakeyaml.engine.v2.common.ScalarStyle;
import org.snakeyaml.engine.v2.schema.CoreSchema;

/** Check structural source ID anchors without interpreting imported domain semantics. */
final class SourceAnchors {
    private SourceAnchors() {}
    static Map<String,Map<String,Object>> read(String file,String text,String kind) {
        var settings=LoadSettings.builder().setSchema(new CoreSchema()).setAllowDuplicateKeys(false).setMaxAliasesForCollections(0)
            .setCodePointLimit(8*1024*1024).setUseMarks(true).build();
        int documents=0,depth=0;
        for(Event event:new Parse(settings).parseString(text)) {
            if(event instanceof DocumentStartEvent d&&(++documents>1||d.getSpecVersion().isPresent()||!d.getTags().isEmpty())
                ||event instanceof AliasEvent||event instanceof NodeEvent n&&n.getAnchor().isPresent()
                ||event instanceof ScalarEvent s&&s.getTag().isPresent()||event instanceof CollectionStartEvent c&&c.getTag().isPresent())
                throw new IllegalArgumentException("unsupported source structure");
            if(event instanceof CollectionStartEvent&&++depth>16)throw new IllegalArgumentException("source nesting limit");
            if(event instanceof CollectionEndEvent)depth--;
        }
        Node root=new Compose(settings).composeString(text).orElseThrow();var fields=mapping(root);var header=string(fields.get("format"));
        List<Node> records;
        if(kind.equals("requirement")) {
            if(!Set.of("mundanereq-yaml-0.3","mundanereq-yaml-0.4").contains(header)||!(fields.get("requirements") instanceof SequenceNode list))throw new IllegalArgumentException("expected requirement source");
            records=list.getValue();
        } else {if(!"mundane-work-yaml-0.2".equals(header))throw new IllegalArgumentException("expected work source");records=List.of(root);}
        Map<String,Map<String,Object>> result=new TreeMap<>();
        for(Node record:records) {
            Node id=mapping(record).get("id");String value=string(id);
            if(!(id instanceof ScalarNode scalar)||scalar.getScalarStyle()==ScalarStyle.LITERAL||scalar.getScalarStyle()==ScalarStyle.FOLDED)continue;
            var start=id.getStartMark().orElseThrow();var end=id.getEndMark().orElseThrow();
            if(start.getLine()!=end.getLine())continue;
            if(result.putIfAbsent(value,object("path",file,"start",object("line",start.getLine()+1,"column",start.getColumn()+1),
                "end",object("line",end.getLine()+1,"column",end.getColumn()+1)))!=null)throw new IllegalArgumentException("ambiguous source ID");
        }
        return result;
    }
    private static String string(Node node) {
        if(!(node instanceof ScalarNode s)||!Tag.STR.equals(s.getTag()))throw new IllegalArgumentException("expected string");return s.getValue();
    }
    private static Map<String,Node> mapping(Node node) {
        if(!(node instanceof MappingNode mapping))throw new IllegalArgumentException("expected mapping");
        Map<String,Node> result=new TreeMap<>();
        for(var tuple:mapping.getValue()) {
            String key=string(tuple.getKeyNode());
            if(key.equals("<<")||result.putIfAbsent(key,tuple.getValueNode())!=null)throw new IllegalArgumentException("ambiguous source mapping");
        }
        return result;
    }
}
