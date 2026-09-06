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
    /** Exact relation value spans for editor consumers; opaque body text stays opaque. */
    public static List<Map<String,Object>> relations(List<Snapshots.Snapshot> sources) {
        var result=new ArrayList<Map<String,Object>>();
        for(var source:sources) {
            Node root=WorkYaml.compose(source.path(),new String(source.bytes(),StandardCharsets.UTF_8));
            var fields=mapping(root);
            if(!(fields.get("relations") instanceof SequenceNode relations))continue;
            for(Node entry:relations.getValue()) {
                var relation=mapping(entry);Node target=relation.get("target");
                String role=optional(relation.get("relation")),kind=optional(relation.get("kind"));
                if(!Set.of("addresses","relates-to","supersedes").contains(role)||!Set.of("requirement","work-item").contains(kind)||!simple(target))continue;
                result.add(object("scope",scalar(relation.get("scope")),"kind",kind,"id",scalar(target),"role",role,"location",span(source.path(),target)));
            }
        }
        result.sort(Comparator.comparing(engineering.artifacts.Json::write));return result;
    }
    public record Assistance(List<Map<String,Object>> suggestions,Map<String,Object> hover) {}
    private record Slot(Node key,Node value,List<String> choices,String help,Map<String,String> targets) {}
    public static Assistance assist(List<Snapshots.Snapshot> sources,String file,int line,int column) {
        var none=new Assistance(List.of(),null);
        var source=sources.stream().filter(s->s.path().equals(file)).findFirst().orElseThrow();
        String text=new String(source.bytes(),StandardCharsets.UTF_8);String[] lines=text.split("\n",-1);
        if(line>lines.length)return none;String raw=lines[line-1].replace("\r","");
        if(column>raw.codePointCount(0,raw.length())+1)return none;
        try {
            Node root=WorkYaml.compose(file,text);WorkYaml.value(file,root);
            var fields=mapping(root);
            if(!mundanereq.Versions.WORK_SOURCE.equals(scalar(fields.get("format"))))return none;
            String kind=scalar(fields.get("kind")),id=scalar(fields.get("id"));engineering.artifacts.Checks.id(id);
            if(!Set.of("task","issue").contains(kind))return none;
            var targets=targets(sources,id);var slots=new ArrayList<Slot>();
            var statuses=kind.equals("task")?WorkValues.TASK_STATUS:WorkValues.ISSUE_STATUS;
            slots.add(new Slot(key(root,"status"),fields.get("status"),statuses.stream().sorted().toList(),
                "Authored "+kind+" status. Allowed values: "+String.join(", ",new TreeSet<>(statuses))+". Suggestions do not update status automatically.",Map.of()));
            if(kind.equals("task")&&fields.get("dependencies") instanceof SequenceNode deps) {
                for(Node dep:deps.getValue()) {
                    var used=new HashSet<String>();for(Node other:deps.getValue())if(other!=dep&&other instanceof ScalarNode scalar)used.add(scalar.getValue());
                    slots.add(new Slot(key(root,"dependencies"),dep,targets.keySet().stream().filter(t->!used.contains(t)).toList(),
                        "Local task prerequisite. The selected task must exist; dependency cycles are invalid.",targets));
                }
            }
            if(fields.get("relations") instanceof SequenceNode relations)for(Node entry:relations.getValue()) {
                var relation=mapping(entry);
                slots.add(new Slot(key(entry,"relation"),relation.get("relation"),WorkValues.RELATIONS.stream().sorted().toList(),
                    "Typed relation: addresses, relates-to, supersedes or evidence. Evidence requires an unscoped resource; supersedes requires a work-item target. Imported targets are not resolved by this editor.",Map.of()));
                if("work-item".equals(optional(relation.get("kind")))&&"work".equals(optional(relation.get("scope")))
                    &&Set.of("addresses","relates-to","supersedes").contains(optional(relation.get("relation")))) {
                    slots.add(new Slot(key(entry,"target"),relation.get("target"),List.copyOf(targets.keySet()),
                        "Selected local task target in scope work. Imported scopes and evidence resources are outside editor resolution.",targets));
                }
            }
            for(Slot slot:slots) {
                if(hit(slot.key(),line,column,false))return new Assistance(List.of(),object("text",slot.help(),"location",span(file,slot.key())));
                Node node=slot.value();if(!simple(node))continue;
                boolean empty=node instanceof ScalarNode scalar&&Tag.NULL.equals(node.getTag())&&scalar.getValue().isEmpty();
                boolean match=hit(node,line,column,true);
                // Empty scalars have zero-width parser marks. Accept only their own
                // blank suffix, never a comment or a following line.
                if(empty) {
                    var mark=node.getStartMark().orElseThrow();int start=mark.getColumn();
                    match=mark.getLine()+1==line && column>=start+1
                        && start<=raw.codePointCount(0,raw.length()) && raw.substring(raw.offsetByCodePoints(0,start)).isBlank();
                }
                if(!match)continue;
                var location=empty?object("path",file,"start",object("line",line,"column",column),"end",object("line",line,"column",column)):span(file,node);
                var suggestions=slot.choices().stream().map(choice->object("label",choice,"insertText",engineering.artifacts.Json.write(choice),
                    "detail","work-item: "+slot.help(),"location",location)).toList();
                String description=slot.help();String target=slot.targets().get(optional(node));
                if(target!=null)description+="\n\n"+target;
                var hover=hit(node,line,column,false)?object("text",description,"location",span(file,node)):null;
                return new Assistance(suggestions,hover);
            }
        } catch(IllegalArgumentException|engineering.artifacts.Problem|org.snakeyaml.engine.v2.exceptions.YamlEngineException ignored) {
            // Incomplete or unsupported structures have no guessed field meaning.
        }
        return none;
    }
    private static Map<String,String> targets(List<Snapshots.Snapshot> sources,String self) {
        var targets=new TreeMap<String,String>();var counts=new HashMap<String,Integer>();
        for(var source:sources) {
            var result=WorkCompiler.compileSnapshots(List.of(source));
            if(!result.valid())continue;
            var values=engineering.artifacts.Checks.map(result.items().getFirst().get("values"));String id=(String)values.get("id");
            counts.merge(id,1,Integer::sum);
            if(values.get("kind").equals("task")&&!id.equals(self))targets.put(id,id+" — "+values.get("title")+" ("+values.get("status")+")");
        }
        targets.keySet().removeIf(id->counts.get(id)!=1);return targets;
    }
    private static String optional(Node node) {return node instanceof ScalarNode scalar&&Tag.STR.equals(scalar.getTag())?scalar.getValue():"";}
    private static Node key(Node root,String name) {
        return ((MappingNode)root).getValue().stream().filter(t->name.equals(optional(t.getKeyNode()))).map(NodeTuple::getKeyNode).findFirst().orElse(null);
    }
    private static boolean hit(Node node,int line,int column,boolean inclusiveEnd) {
        if(!simple(node))return false;
        var start=node.getStartMark().orElseThrow();var end=node.getEndMark().orElseThrow();
        return start.getLine()+1==line&&column>=start.getColumn()+1&&(inclusiveEnd?column<=end.getColumn()+1:column<end.getColumn()+1);
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
