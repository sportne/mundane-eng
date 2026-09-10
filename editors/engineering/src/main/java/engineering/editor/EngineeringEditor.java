package engineering.editor;
import static engineering.artifacts.Checks.*;
import engineering.artifacts.*;
import engineering.domain.*;
import engineering.change.*;
import mundane.yaml.Yaml;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Read-only working copies and pinned compiled indexes; deliberately no filesystem or native execution. */
public final class EngineeringEditor {
    private EngineeringEditor(){}
    public static final String SOURCE="mundane-engineering-editor-0.1";
    public static Map<String,Model.Domain> owners(){var result=Owners.all();result.remove("evidence");result.put("change",new Change());result.put("manual-observation",new Model.Domain(){
        public String kind(){return "manual-observation";}public String source(){return "mundane-manual-observation-yaml-0.1";}public String format(){return source();}public String version(){return mundanereq.Versions.EVIDENCE_VERSION;}public String contract(){return "working-copy-schema";}
        public Object schema(){return Json.read(engineering.evidence.ManualSchema.JSON.getBytes(StandardCharsets.UTF_8));}
        public void validate(Map<String,Object> v,Model.Context c){throw new IllegalArgumentException("manual observations require the evidence CLI");}
        public String view(Map<String,Object> a,Model.Context c){throw new IllegalArgumentException("manual observations require the evidence CLI");}
    });return result;}
    private record File(String path,String kind,Yaml.Document document,Map<String,Object> values,List<?> imports) {}
    private record Target(String id,Map<String,Object> value,Map<String,Object> artifact) {}
    public static Map<String,Object> analyze(Object input) {
        var request=map(input);keys(request,"protocol","source","files","schema","imports","cursor");
        if(!mundanereq.Versions.EDITOR_PROTOCOL.equals(request.get("protocol"))||!SOURCE.equals(request.get("source"))||request.get("schema")!=null)throw new IllegalArgumentException("unsupported engineering editor request");
        var packet=map(request.get("imports"));keys(packet,"artifacts","sources");
        if(list(request.get("files")).isEmpty()||list(request.get("files")).size()>128||list(packet.get("artifacts")).size()>128||list(packet.get("sources")).size()>256)throw new IllegalArgumentException("engineering snapshot count limit");
        var artifacts=new TreeMap<String,Map<String,Object>>();var nativeText=new TreeMap<String,String>();var sourceTexts=new TreeMap<String,String>();
        for(Object raw:list(packet.get("artifacts"))){var p=map(raw);keys(p,"path","text");String name=path(p.get("path"));if(nativeText.putIfAbsent(name,string(p.get("text")))!=null)throw new IllegalArgumentException("duplicate imported artifact");}
        for(Object raw:list(packet.get("sources"))){var p=map(raw);keys(p,"path","text");if(sourceTexts.putIfAbsent(path(p.get("path")),string(p.get("text")))!=null)throw new IllegalArgumentException("duplicate imported source");}
        var files=new ArrayList<File>();var diagnostics=new ArrayList<Map<String,Object>>();var definitions=new ArrayList<Map<String,Object>>();var navigation=new ArrayList<Map<String,Object>>();var suggestions=new ArrayList<Map<String,Object>>();Map<String,Object> hover=null;
        Set<String> selected=new HashSet<>();
        for(Object raw:list(request.get("files"))) {
            var f=map(raw);keys(f,"path","text","kind","imports","importError");String name=path(f.get("path")),kind=text(f.get("kind"));
            if(!selected.add(name))throw new IllegalArgumentException("duplicate selected source");
            var owner=owners().get(kind);if(owner==null)throw new IllegalArgumentException("unsupported engineering source family");
            Yaml.Document doc;
            try{doc=Yaml.document(string(f.get("text")).getBytes(StandardCharsets.UTF_8),1024*1024,true);}
            catch(Yaml.Failure e){diagnostics.add(diagnostic(name,e.point,"yaml",e.getMessage()));continue;}
            catch(IllegalArgumentException e){diagnostics.add(diagnostic(name,new Yaml.Point(1,1),"yaml",e.getMessage()));continue;}
            if(!(doc.value() instanceof Map<?,?>)){diagnostics.add(diagnostic(name,new Yaml.Point(1,1),"engineering-schema","Expected a YAML mapping"));continue;}
            var values=map(doc.value());var file=new File(name,kind,doc,values,list(f.get("imports")));files.add(file);
            if(f.get("importError")!=null)diagnostics.add(diagnostic(name,new Yaml.Point(1,1),"engineering-import",string(f.get("importError"))));
            try{Schema.validate(values,owner.schema());}
            catch(IllegalArgumentException e){
                String message=e.getMessage();String pointer=message.startsWith("schema ")?message.substring(7,message.indexOf(':',7)):"";
                diagnostics.add(diagnostic(name,point(doc,pointer),"engineering-schema",message));
            }
            Set<String> scopes=new HashSet<>();
            for(Object rawEntry:file.imports)try{
                var e=map(rawEntry);keys(e,"scope","kind","format","path","sha256");if(!scopes.add(id(e.get("scope"))))throw new IllegalArgumentException("duplicate import scope");
                String imported=path(e.get("path"));String text=nativeText.get(imported);
                if(text==null||!Snapshots.hash(text.getBytes(StandardCharsets.UTF_8)).equals(digest(e.get("sha256"))))throw new IllegalArgumentException("missing or changed compiled import "+imported);
                var a=map(Json.read(text.getBytes(StandardCharsets.UTF_8)));
                if(!e.get("kind").equals(a.get("artifactKind"))||!e.get("format").equals(a.get("format")))throw new IllegalArgumentException("compiled import kind/format mismatch");
                var importedOwner=Owners.all().get(e.get("kind"));
                if(importedOwner!=null){Model.envelope(a,importedOwner);if(!e.get("kind").equals("evidence")){var authored=new TreeMap<>(map(a.get("values")));authored.put("format",importedOwner.source());Schema.validate(authored,importedOwner.schema());}}
                else if(e.get("kind").equals("requirements"))Artifacts.requirements(a,imported);
                else if(e.get("kind").equals("verification-plan"))Artifacts.plan(a,imported);
                else if(e.get("kind").equals("work-items"))engineering.work.WorkArtifact.validate(a,imported);
                else throw new IllegalArgumentException("unsupported compiled import");
                artifacts.put(imported,a);
            }catch(IllegalArgumentException|Problem e){diagnostics.add(diagnostic(name,new Yaml.Point(1,1),"engineering-import",e.getMessage()));}
            walkReferences(file,values,"",artifacts,sourceTexts,diagnostics,definitions,navigation);
        }
        if(request.get("cursor")!=null){
            var cursor=map(request.get("cursor"));keys(cursor,"path","line","column");int line=integer(cursor.get("line")),column=integer(cursor.get("column"));String name=path(cursor.get("path"));
            var file=files.stream().filter(f->f.path.equals(name)).findFirst().orElse(null);
            if(file==null||line<1||column<1)throw new IllegalArgumentException("cursor is outside the selected snapshot");
            if(file!=null){
                String pointer=file.document.values().entrySet().stream().filter(e->contains(e.getValue(),line,column)).max(Comparator.comparingInt(e->e.getKey().length())).map(Map.Entry::getKey).orElse("");
                var range=file.document.values().get(pointer);Object value=at(file.values,pointer);
                String parent=pointer.contains("/")?pointer.substring(0,pointer.lastIndexOf('/')):"";Object container=at(file.values,parent);
                var schema=fieldSchema(owners().get(file.kind).schema(),pointer);
                if(range!=null&&!(value instanceof Map<?,?>)&&!(value instanceof List<?>)){
                    String description="Working-copy "+file.kind+" "+pointer+". Full native resource, evidence and readiness checks require the owning CLI.";
                    if(container instanceof Map<?,?>&&pointer.endsWith("/id")&&map(container).containsKey("scope")&&map(container).containsKey("kind")){
                        var ref=map(container);var candidates=targets(file,string(ref.get("scope")),string(ref.get("kind")),artifacts);
                        for(var candidate:candidates)suggestions.add(suggestion(file,range,candidate.id,"Typed "+ref.get("kind")+" in "+ref.get("scope")+"; compiled revision"));
                        description="Typed "+ref.get("kind")+" in "+ref.get("scope")+". Selected compiled revision; unsaved sources do not update evidence.";
                    }else if(schema.containsKey("enum"))for(Object option:list(schema.get("enum")))if(option instanceof String)suggestions.add(suggestion(file,range,text(option),"Allowed "+pointer+" value"));
                    hover=Json.object("text",description,"location",span(file.path,range));
                }
            }
        }
        var merged=new TreeMap<String,Map<String,Object>>();
        for(var definition:definitions){String key=text(definition.get("id"));var prior=merged.get(key);
            if(prior==null){var copy=new TreeMap<>(definition);copy.put("references",new ArrayList<>(list(definition.get("references"))));merged.put(key,copy);}
            else {var refs=new ArrayList<Object>(list(prior.get("references")));refs.addAll(list(definition.get("references")));prior.put("references",refs);}
        }
        definitions=new ArrayList<>(merged.values());
        boolean valid=diagnostics.isEmpty();
        return Json.object("protocol",mundanereq.Versions.EDITOR_PROTOCOL,"valid",valid,"diagnostics",diagnostics,"definitions",definitions,"importNavigation",navigation,"suggestions",suggestions,"hover",hover,"formatting",List.of(),"validationLevel","working-copy-schema-and-typed-index","nativeAssessments","not-executed");
    }
    private static String string(Object value){if(!(value instanceof String s))throw new IllegalArgumentException("expected string");return s;}
    private static Map<String,Object> diagnostic(String path,Yaml.Point point,String code,String message){return Json.object("path",path,"line",point.line(),"column",point.column(),"code",code,"message",message);}
    private static Yaml.Point point(Yaml.Document d,String pointer){String at=pointer;while(!d.values().containsKey(at)&&!at.isEmpty())at=at.substring(0,Math.max(0,at.lastIndexOf('/')));return d.values().getOrDefault(at,d.values().get("")).start();}
    private static Map<String,Object> span(String path,Yaml.Range r){return Json.object("path",path,"start",Json.object("line",r.start().line(),"column",r.start().column()),"end",Json.object("line",r.end().line(),"column",r.end().column()));}
    private static boolean contains(Yaml.Range r,int line,int column){return (line>r.start().line()||line==r.start().line()&&column>=r.start().column())&&(line<r.end().line()||line==r.end().line()&&column<=r.end().column());}
    private static Map<String,Object> suggestion(File file,Yaml.Range range,String value,String detail){return Json.object("label",value,"insertText",Json.write(value),"detail",detail,"location",span(file.path,range));}
    private static Object at(Object value,String pointer){
        if(pointer.isEmpty())return value;
        for(String part:pointer.substring(1).split("/")){part=part.replace("~1","/").replace("~0","~");if(value instanceof Map<?,?>)value=map(value).get(part);else if(value instanceof List<?> list&&part.matches("[0-9]+")&&Integer.parseInt(part)<list.size())value=list.get(Integer.parseInt(part));else return null;}return value;
    }
    private static Map<String,Object> fieldSchema(Object raw,String pointer){
        var root=map(raw);var schema=root;
        for(String part:pointer.isEmpty()?new String[0]:pointer.substring(1).split("/")){
            if(schema.containsKey("$ref"))schema=map(map(root.get("$defs")).get(text(schema.get("$ref")).substring(8)));
            if(schema.containsKey("properties"))schema=map(map(schema.get("properties")).getOrDefault(part,Map.of()));
            else if(schema.containsKey("items"))schema=map(schema.get("items"));else return Map.of();
        }return schema;
    }
    private static List<Target> targets(File file,String scope,String kind,Map<String,Map<String,Object>> imports){
        Map<String,Object> artifact=null;Map<String,Object> values=file.values;Model.Domain owner=owners().get(file.kind);
        if(!scope.equals("self")){
            var e=file.imports.stream().map(engineering.artifacts.Checks::map).filter(i->i.get("scope").equals(scope)).findFirst().orElse(null);if(e==null)return List.of();
            artifact=imports.get(e.get("path"));if(artifact==null)return List.of();values=Owners.values(artifact);owner=Owners.all().get(artifact.get("artifactKind"));
        }
        Set<String> ids=new TreeSet<>();collectIds(values,ids);var result=new ArrayList<Target>();
        for(String id:ids)try{
            Map<String,Object> target;
            if(artifact!=null&&kind.equals("requirement"))target=map(Artifacts.requirements(artifact,"import").get(id).get("values"));
            else if(artifact!=null&&kind.equals("activity"))target=Model.find(Model.rows(artifact,"activities"),id);
            else if(scope.equals("self")&&file.kind.equals("safety")&&kind.equals("event"))target=Model.find(Model.rows(map(values.get("faultTree")),"events"),id);
            else if(owner!=null)target=owner.lookup(values,kind,id);
            else continue;
            result.add(new Target(id,target,artifact));
        }catch(IllegalArgumentException|NullPointerException ignored){}
        return result;
    }
    private static void collectIds(Object value,Set<String> ids){
        if(value instanceof Map<?,?>){var m=map(value);if(m.get("id") instanceof String id)ids.add(id);for(Object child:m.values())collectIds(child,ids);}
        else if(value instanceof List<?>)for(Object child:list(value))collectIds(child,ids);
    }
    private static void walkReferences(File file,Object value,String pointer,Map<String,Map<String,Object>> imports,Map<String,String> sources,List<Map<String,Object>> diagnostics,List<Map<String,Object>> definitions,List<Map<String,Object>> navigation){
        if(value instanceof Map<?,?>){
            var m=map(value);
            if(m.keySet().equals(Set.of("scope","kind","id"))&&m.values().stream().allMatch(String.class::isInstance)){
                String scope=string(m.get("scope")),kind=string(m.get("kind")),id=string(m.get("id"));String at=pointer+"/id";var reference=span(file.path,file.document.values().get(at));
                // Prior baselines are selected native resources, checked by configuration CLI.
                if(!(file.kind.equals("configuration")&&scope.equals("previous"))){
                    var targets=targets(file,scope,kind,imports).stream().filter(t->t.id.equals(id)).toList();
                    if(targets.size()!=1)diagnostics.add(diagnostic(file.path,point(file.document,at),"engineering-reference","Unresolved typed "+scope+":"+kind+":"+id));
                    else if(scope.equals("self")){
                        String targetPointer=findId(file.values,id,"",file.document);if(targetPointer!=null)definitions.add(Json.object("id",file.path+":"+kind+":"+id,"location",span(file.path,file.document.values().get(targetPointer)),"references",List.of(Json.object("id",file.path+":"+kind+":"+id,"location",reference))));
                    }else{
                        var target=targets.getFirst();String targetPointer=findId(Owners.values(target.artifact),id,"",null);var origin=Owners.origin(target.artifact,targetPointer==null?"":targetPointer);
                        if(!origin.isEmpty()){
                            var selected=map(origin.get("source"));String text=sources.get(selected.get("path"));
                            if(text!=null&&Snapshots.hash(text.getBytes(StandardCharsets.UTF_8)).equals(selected.get("sha256"))){
                                // Refine a source point to the exact YAML ID scalar, only at the pinned source revision.
                                try{var doc=Yaml.document(text.getBytes(StandardCharsets.UTF_8),1024*1024,true);String p=findId(doc.value(),id,"",doc);
                                    if(p!=null)navigation.add(Json.object("reference",reference,"target",span(text(selected.get("path")),doc.values().get(p))));
                                }catch(IllegalArgumentException ignored){}
                            }
                        }
                    }
                }
            }
            for(var e:m.entrySet())walkReferences(file,e.getValue(),mundane.json.Json.pointer(pointer,e.getKey()),imports,sources,diagnostics,definitions,navigation);
        }else if(value instanceof List<?> list)for(int i=0;i<list.size();i++)walkReferences(file,list.get(i),pointer+"/"+i,imports,sources,diagnostics,definitions,navigation);
    }
    private static String findId(Object value,String id,String pointer,Yaml.Document doc){
        if(value instanceof Map<?,?>){var m=map(value);if(id.equals(m.get("id"))&&!m.containsKey("scope"))return pointer+"/id";for(var e:m.entrySet()){String result=findId(e.getValue(),id,mundane.json.Json.pointer(pointer,e.getKey()),doc);if(result!=null)return result;}}
        else if(value instanceof List<?> list)for(int i=0;i<list.size();i++){String result=findId(list.get(i),id,pointer+"/"+i,doc);if(result!=null)return result;}
        return null;
    }
}
