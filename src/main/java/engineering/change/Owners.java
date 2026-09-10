package engineering.change;
import static engineering.artifacts.Checks.*;
import engineering.domain.*;
import engineering.artifacts.*;
import engineering.work.WorkArtifact;
import java.nio.file.Path;
import java.util.*;
/** Explicit compiled adapters, with semantic field policy owned by each domain. */
public final class Owners {
    private Owners() {}
    public static Map<String,Model.Domain> all(){var d=engineering.assurance.Assurance.adapters();d.put("operations",new engineering.operations.Operations());d.put("equipment",new engineering.equipment.Equipment());d.put("budget",new engineering.budget.Budget());d.put("assessment",new engineering.evidence.Assessment());return d;}
    public static Model.Context context(Path root){return new Model.Context(root,all());}
    public static boolean known(String kind){return all().containsKey(kind)||Set.of("requirements","verification-plan","work-items").contains(kind);}
    public static void validate(Map<String,Object> a,Model.Context c,String file) {
        String kind=text(a.get("artifactKind"));
        if(kind.equals("requirements"))Artifacts.requirements(a,file);
        else if(kind.equals("verification-plan"))Artifacts.plan(a,file);
        else if(kind.equals("work-items"))WorkArtifact.validate(a,file);
        else {var owner=all().get(kind);if(owner!=null)Model.validateSelected(a,owner,c);if(kind.equals("evidence")&&!map(list(a.get("sources")).getFirst()).equals(map(map(a.get("values")).get("resource"))))throw new IllegalArgumentException("evidence source inventory mismatch");}
    }
    public static Map<String,Object> values(Map<String,Object> a) {
        String kind=text(a.get("artifactKind"));
        if(kind.equals("requirements"))return Json.object("requirements",Model.rows(a,"requirements").stream().map(r->{var v=Artifacts.values(r);v.put("attributes",map(r.get("values")).getOrDefault("attributes",Map.of()));return v;}).toList(),"declarations",Artifacts.attributeDefinition(a));
        if(kind.equals("work-items"))return Json.object("items",Model.rows(a,"items").stream().map(r->r.get("values")).toList());
        if(kind.equals("verification-plan"))return Json.object("plans",strip(a.get("plans")),"activities",strip(a.get("activities")),"coverage",strip(a.get("coverage")));
        return map(a.getOrDefault("values",Map.of()));
    }
    private static Object strip(Object v) {
        if(v instanceof Map<?,?>){var result=new TreeMap<String,Object>();map(v).forEach((k,x)->{if(!k.equals("location"))result.put(k,strip(x));});return result;}
        if(v instanceof List<?>)return list(v).stream().map(Owners::strip).toList();return v;
    }
    public static Map<String,Object> origin(Map<String,Object> a,String pointer) {
        Map<String,Object> loc=null;
        String[] tokens=pointer.split("/");
        if(pointer.startsWith("/declarations")&&a.get("attributeSchema") instanceof Map<?,?>){
            var declaration=map(a.get("attributeSchema"));var spans=map(declaration.get("locations"));
            Object selected=tokens.length>3?spans.get(tokens[3].replace("~1","/").replace("~0","~")):null;
            if(selected==null&&!spans.isEmpty())selected=spans.values().iterator().next();
            if(selected!=null)return Json.object("source",declaration.get("source"),"location",point(map(selected)));
        }
        if(a.containsKey("locations")){
            var locations=map(a.get("locations"));String at=pointer;
            while(!locations.containsKey(at)&&!at.isEmpty())at=at.substring(0,Math.max(0,at.lastIndexOf('/')));
            loc=map(locations.getOrDefault(at,locations.get("")));
        } else {
            String kind=text(a.get("artifactKind"));String field=kind.equals("requirements")?"requirements":kind.equals("work-items")?"items":tokens.length>1&&Set.of("plans","activities","coverage").contains(tokens[1])?tokens[1]:"plans";
            var rows=Model.rows(a,field);int index=0;String[] parts=pointer.split("/");
            if(parts.length>2&&parts[2].matches("[0-9]+"))index=Math.min(Integer.parseInt(parts[2]),Math.max(0,rows.size()-1));
            if(!rows.isEmpty()){
                var row=rows.get(index);
                if(kind.equals("requirements")){
                    var locations=map(row.get("locations"));var span=map(locations.get("record"));
                    if(parts.length>3){
                        var fields=map(locations.get("fields"));Object matches=fields.get(parts[3]);
                        if(matches instanceof List<?> list&&!list.isEmpty())span=map(list.getFirst());
                        if(parts[3].equals("attributes")&&parts.length>4&&map(locations.get("attributes")).get(parts[4]) instanceof Map<?,?> attribute)span=map(map(attribute).get("value"));
                    }
                    loc=point(span);
                }
                else loc=map(row.get("location"));
            }
        }
        if(loc==null)return Map.of();String file=text(loc.get("path"));var source=Model.rows(a,"sources").stream().filter(x->x.get("path").equals(file)).findFirst().orElse(null);
        return source==null?Map.of():Json.object("source",source,"location",loc);
    }
    private static Map<String,Object> point(Map<String,Object> span){return Json.object("path",span.get("path"),"line",map(span.get("start")).get("line"),"column",map(span.get("start")).get("column"));}
    public static String link(Map<String,Object> origin,Model.Context c,String label) {
        if(origin.isEmpty())return Model.escape(label)+" (source unavailable)";
        var loc=map(origin.get("location"));var source=map(origin.get("source"));
        try{c.readPinned(source);return "["+Model.escape(label)+"]("+c.root.resolve(text(source.get("path"))).toUri().toASCIIString()+"#L"+loc.get("line")+")";}
        catch(Problem unavailable){return Model.escape(label)+" (source revision unavailable)";}
    }
    public static Map<String,String> groups(String kind) {
        return switch(kind) {
            case "requirements" -> Map.of("requirements","requirement-values","declarations","attribute-declarations");
            case "verification-plan" -> Map.of("plans","verification-planning","activities","verification-planning","coverage","planned-coverage");
            case "work-items" -> Map.of("items","corrective-work");
            default -> all().containsKey(kind)?all().get(kind).changeGroups():Map.of();
        };
    }
}
