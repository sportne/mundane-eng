package engineering.domain;

import static engineering.artifacts.Checks.*;
import java.math.BigDecimal;
import java.util.*;

/** Closed, local schema subset used by the checked-in domain contracts; no remote resolution. */
public final class Schema {
    private Schema() {}
    public static void validate(Object value,Object schema) { check(value,map(schema),map(schema),"",0); }
    private static void fail(String at,String message) { throw new IllegalArgumentException("schema "+at+": "+message); }
    private static void check(Object v,Map<String,Object> s,Map<String,Object> root,String at,int depth) {
        if(depth>40)fail(at,"schema depth exceeded");
        if(s.containsKey("$ref")) {
            String ref=text(s.get("$ref"));if(!ref.startsWith("#/$defs/"))fail(at,"nonlocal schema reference");
            check(v,map(map(root.get("$defs")).get(ref.substring(8))),root,at,depth+1);return;
        }
        if(s.containsKey("const")&&!Objects.equals(v,s.get("const")))fail(at,"constant mismatch");
        if(s.containsKey("enum")&&!list(s.get("enum")).contains(v))fail(at,"unsupported value");
        if(s.containsKey("anyOf")) {
            boolean valid=false;for(Object candidate:list(s.get("anyOf")))try {check(v,map(candidate),root,at,depth+1);valid=true;break;}catch(IllegalArgumentException ignored) { }
            if(!valid)fail(at,"no matching alternative");
        }
        if(s.containsKey("allOf"))for(Object candidate:list(s.get("allOf")))check(v,map(candidate),root,at,depth+1);
        if(s.containsKey("if")) {
            boolean matches=true;try {check(v,map(s.get("if")),root,at,depth+1);}catch(IllegalArgumentException ignored){matches=false;}
            String branch=matches?"then":"else";if(s.containsKey(branch))check(v,map(s.get(branch)),root,at,depth+1);
        }
        if(s.containsKey("type")) {
            String type=text(s.get("type"));boolean valid=switch(type) {
                case "object" -> v instanceof Map<?,?>;case "array" -> v instanceof List<?>;
                case "string" -> v instanceof String;case "boolean" -> v instanceof Boolean;case "null" -> v==null;
                case "number" -> v instanceof Number;
                case "integer" -> v instanceof Number n && new BigDecimal(n.toString()).stripTrailingZeros().scale()<=0;
                default -> throw new IllegalArgumentException("unsupported schema type");
            };if(!valid)fail(at,"expected "+type);
        }
        if(v instanceof Map<?,?>) {
            var m=map(v);var properties=s.containsKey("properties")?map(s.get("properties")):Map.<String,Object>of();
            if(s.containsKey("required"))for(Object key:list(s.get("required")))if(!m.containsKey(text(key)))fail(at,"missing "+key);
            for(var e:m.entrySet()) {
                if(properties.containsKey(e.getKey()))check(e.getValue(),map(properties.get(e.getKey())),root,at+"/"+e.getKey(),depth+1);
                else if(Boolean.FALSE.equals(s.get("additionalProperties")))fail(at,"unknown field "+e.getKey());
            }
        }
        if(v instanceof List<?> a) {
            if(a.size()>10000)fail(at,"too many items");
            if(s.containsKey("minItems")&&a.size()<number(s.get("minItems")))fail(at,"too few items");
            if(s.containsKey("maxItems")&&a.size()>number(s.get("maxItems")))fail(at,"too many items");
            if(Boolean.TRUE.equals(s.get("uniqueItems"))&&new HashSet<>(a).size()!=a.size())fail(at,"duplicate item");
            if(s.containsKey("items"))for(int i=0;i<a.size();i++)check(a.get(i),map(s.get("items")),root,at+"/"+i,depth+1);
        }
        if(v instanceof String str) {
            if(s.containsKey("minLength")&&str.length()<number(s.get("minLength")))fail(at,"short string");
            if(s.containsKey("maxLength")&&str.length()>number(s.get("maxLength")))fail(at,"long string");
            if(s.containsKey("pattern")&&!str.matches(text(s.get("pattern"))))fail(at,"pattern mismatch");
        }
        if(v instanceof Number) {
            double n=number(v);if(!Double.isFinite(n))fail(at,"non-finite number");
            if(s.containsKey("minimum")&&n<number(s.get("minimum")))fail(at,"below minimum");
            if(s.containsKey("maximum")&&n>number(s.get("maximum")))fail(at,"above maximum");
        }
    }
    public static double number(Object v) { if(!(v instanceof Number n))throw new IllegalArgumentException("expected number");return n.doubleValue(); }
}
