package mundanereq.editor;

import static engineering.artifacts.Checks.*;
import static engineering.artifacts.Json.object;
import java.util.*;

/** Resolution never falls back across scopes or follows an unverified source origin. */
final class ImportedNavigation {
    private ImportedNavigation() {}
    record Result(List<Map<String,Object>> navigation,List<Map<String,Object>> diagnostics,Map<String,Map<String,Object>> targets) {}
    static Result resolve(EditorImports.Selection selection,List<Map<String,Object>> references) {
        var targets=new TreeMap<String,Map<String,Object>>();var anchors=new HashMap<String,Map<String,Map<String,Object>>>();
        var locations=new TreeMap<String,Map<String,Object>>();
        for(var target:selection.targets().values()) {
            String state=target.state();Map<String,Object> span=null;
            if(state.equals("matching")) {
                String cacheKey=target.kind()+":"+target.file();
                if(!anchors.containsKey(cacheKey)) {
                    try {anchors.put(cacheKey,SourceAnchors.read(target.file(),target.sourceText(),target.kind()));}
                    catch(IllegalArgumentException|NoSuchElementException|org.snakeyaml.engine.v2.exceptions.YamlEngineException error){anchors.put(cacheKey,Map.of());}
                }
                span=anchors.get(cacheKey).get(target.id());
                if(span==null||!agrees(target,span)) {state="origin-mismatch";span=null;}
            }
            var description=target.describe();description.put("sourceState",state);targets.put(target.key(),description);
            if(span!=null)locations.put(target.key(),span);
        }
        var navigation=new ArrayList<Map<String,Object>>();var diagnostics=new ArrayList<Map<String,Object>>();
        for(var reference:references) {
            if(reference.get("scope").equals("work"))continue;
            String key=reference.get("scope")+":"+reference.get("kind")+":"+reference.get("id");
            var target=targets.get(key);String state=target==null?"missing-target":text(target.get("sourceState"));
            if(state.equals("matching"))navigation.add(object("reference",reference.get("location"),"target",locations.get(key),"key",key));
            else {
                var span=map(reference.get("location"));var start=map(span.get("start"));
                diagnostics.add(object("path",span.get("path"),"line",start.get("line"),"column",start.get("column"),"end",span.get("end"),
                    "severity",target==null?"error":"warning","code",target==null?"editor-import-target":"editor-import-source",
                    "message",target==null?"No imported target "+key:"Navigation unavailable for "+key+": source "+state+"; compiled revision "+target.get("revision")));
            }
        }
        return new Result(navigation,diagnostics,targets);
    }
    private static boolean samePoint(Map<String,Object> a,Map<String,Object> b) {
        return integer(a.get("line"))==integer(b.get("line"))&&integer(a.get("column"))==integer(b.get("column"));
    }
    private static boolean agrees(EditorImports.Target target,Map<String,Object> span) {
        if(target.kind().equals("requirement"))return samePoint(map(target.origin().get("start")),map(span.get("start")))&&samePoint(map(target.origin().get("end")),map(span.get("end")));
        var start=map(span.get("start"));return integer(target.origin().get("line"))==integer(start.get("line"))&&integer(target.origin().get("column"))==integer(start.get("column"));
    }
}
