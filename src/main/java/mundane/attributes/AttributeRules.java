package mundane.attributes;

import java.util.*;
import mundane.json.Json;

/** Pure decoded schema/value rules shared by source and serialized boundaries. No I/O. */
public final class AttributeRules {
    private AttributeRules() {}
    public static final String FORMAT=mundanereq.Versions.ATTRIBUTE_SCHEMA;
    public static final Set<String> RESERVED=Set.of("id","title","allocation","statement","rationale","source","decomposes","format","requirements","attributes","attribute-schema");
    public static final class Invalid extends IllegalArgumentException {
        private static final long serialVersionUID=1L;
        public final String pointer;
        public Invalid(String pointer,String message){super(message);this.pointer=pointer;}
    }
    public static String name(Object v,String p,boolean attribute) {
        if(!(v instanceof String s)||s.length()>64||!s.matches("[a-z][a-z0-9]*(?:-[a-z0-9]+)*")||attribute&&(RESERVED.contains(s)||s.startsWith("mreq-")||s.startsWith("mundane-")))throw new Invalid(p,"invalid or reserved attribute/schema name");
        return s;
    }
    public static String text(Object v,String p) {
        if(!(v instanceof String s)||s.isEmpty()||s.codePoints().anyMatch(c->c<32||c>=127&&c<=159||c>=0xd800&&c<=0xdfff)||space(s.codePointAt(0))||space(s.codePointBefore(s.length())))throw new Invalid(p,"expected nonempty unpadded single-line Unicode text");
        return s;
    }
    private static boolean space(int c){return Character.isWhitespace(c)||Character.isSpaceChar(c);}
    public static Map<String,Object> map(Object v,String p) {
        if(!(v instanceof Map<?,?> raw))throw new Invalid(p,"expected object");
        Map<String,Object> m=new TreeMap<>();for(var e:raw.entrySet()) {if(!(e.getKey() instanceof String k))throw new Invalid(p,"expected string key");m.put(k,e.getValue());}return m;
    }
    private static void keys(Map<String,Object> m,String p,String... keys) {
        Set<String> wanted=Set.of(keys);
        for(String k:m.keySet())if(!wanted.contains(k))throw new Invalid(Json.pointer(p,k),"unknown field "+k);
        for(String k:keys)if(!m.containsKey(k))throw new Invalid(p,"missing field "+k);
    }
    public static Map<String,Object> definition(Object raw) {
        var d=map(raw,"");keys(d,"","format","name","attributes");if(!FORMAT.equals(d.get("format")))throw new Invalid("/format","unsupported attribute schema format");name(d.get("name"),"/name",false);
        var attributes=map(d.get("attributes"),"/attributes");if(attributes.isEmpty()||attributes.size()>128)throw new Invalid("/attributes","expected 1..128 declarations");
        var normalized=new TreeMap<String,Object>();
        for(var entry:attributes.entrySet()) {
            String p=Json.pointer("/attributes",entry.getKey());name(entry.getKey(),p,true);var a=map(entry.getValue(),p);Object type=a.get("type");
            if(!"text".equals(type)&&!"enum".equals(type))throw new Invalid(a.containsKey("type")?p+"/type":p,"expected text or enum type");
            if(type.equals("text"))keys(a,p,"type","required","description");else keys(a,p,"type","required","description","values");
            if(!(a.get("required") instanceof Boolean))throw new Invalid(p+"/required","required must be a boolean");text(a.get("description"),p+"/description");
            if(type.equals("enum")) {
                if(!(a.get("values") instanceof List<?> members)||members.isEmpty()||members.size()>256)throw new Invalid(p+"/values","expected 1..256 enum values");
                var sorted=new TreeSet<String>();int i=0;for(Object member:members) {String v=text(member,p+"/values/"+i);if(!sorted.add(v))throw new Invalid(p+"/values/"+i,"duplicate enum value");i++;}a.put("values",List.copyOf(sorted));
            }
            normalized.put(entry.getKey(),Collections.unmodifiableMap(a));
        }
        d.put("attributes",Collections.unmodifiableMap(normalized));return Collections.unmodifiableMap(d);
    }
    public static void values(Map<String,Object> definition,Map<String,String> values) {
        if(definition==null) {if(!values.isEmpty())throw new Invalid("","attributes need a schema");return;}
        var declarations=map(definition.get("attributes"),"/attributes");
        for(var e:values.entrySet()) {
            if(!declarations.containsKey(e.getKey()))throw new Invalid(e.getKey(),"unknown attribute");String value=text(e.getValue(),e.getKey());var d=map(declarations.get(e.getKey()),e.getKey());
            if(d.get("type").equals("enum")&&!((List<?>)d.get("values")).contains(value))throw new Invalid(e.getKey(),"value is not an exact enum member");
        }
        for(var e:declarations.entrySet())if(Boolean.TRUE.equals(map(e.getValue(),e.getKey()).get("required"))&&!values.containsKey(e.getKey()))throw new Invalid(e.getKey(),"missing required attribute");
    }
}
