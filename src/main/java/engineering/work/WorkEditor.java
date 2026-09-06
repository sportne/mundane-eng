package engineering.work;

import static engineering.artifacts.Json.object;
import engineering.artifacts.Snapshots;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.snakeyaml.engine.v2.nodes.*;
import org.snakeyaml.engine.v2.common.ScalarStyle;

/** Work-owned structural editor origins; never searches narrative strings for IDs. */
public final class WorkEditor {
    private WorkEditor() {}
    public static List<Map<String,Object>> definitions(List<Snapshots.Snapshot> sources) {
        var result=new ArrayList<Map<String,Object>>();
        for(var source:sources) {
            var fields=mapping(WorkYaml.compose(source.path(),new String(source.bytes(),StandardCharsets.UTF_8)));
            var references=new ArrayList<Map<String,Object>>();
            if(fields.get("dependencies") instanceof SequenceNode dependencies)for(Node dep:dependencies.getValue()) {
                if(simple(dep))references.add(object("id",scalar(dep),"location",span(source.path(),dep)));
            }
            if(simple(fields.get("id")))result.add(object("id",scalar(fields.get("id")),"location",span(source.path(),fields.get("id")),"references",references));
        }
        result.sort(Comparator.comparing(x->(String)x.get("id")));return result;
    }
    static Map<String,Node> mapping(Node node) {
        if(!(node instanceof MappingNode mapping))throw new IllegalArgumentException("expected mapping");
        var fields=new TreeMap<String,Node>();
        for(var pair:mapping.getValue()) {
            String key=scalar(pair.getKeyNode());
            if(fields.putIfAbsent(key,pair.getValueNode())!=null)throw new IllegalArgumentException("duplicate key");
        }
        return fields;
    }
    static String scalar(Node node) {
        if(!(node instanceof ScalarNode scalar)||!Tag.STR.equals(scalar.getTag()))throw new IllegalArgumentException("expected string");
        return scalar.getValue();
    }
    static boolean simple(Node node) {
        return node instanceof ScalarNode scalar && scalar.getScalarStyle()!=ScalarStyle.LITERAL && scalar.getScalarStyle()!=ScalarStyle.FOLDED
            && node.getStartMark().orElseThrow().getLine()==node.getEndMark().orElseThrow().getLine();
    }
    static Map<String,Object> span(String file,Node node) {
        var start=node.getStartMark().orElseThrow();var end=node.getEndMark().orElseThrow();
        return object("path",file,"start",object("line",start.getLine()+1,"column",start.getColumn()+1),
            "end",object("line",end.getLine()+1,"column",end.getColumn()+1));
    }
}
