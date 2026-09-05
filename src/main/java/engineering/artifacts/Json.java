package engineering.artifacts;

import java.util.Map;

/** Compatibility facade over the domain-independent strict JSON codec. */
public final class Json {
    private Json() {}
    public static Object read(byte[] bytes) {return mundane.json.Json.read(bytes);}
    public static Map<String,Object> object(Object... pairs) {return mundane.json.Json.object(pairs);}
    public static byte[] bytes(Object value) {return mundane.json.Json.bytes(value);}
    public static String write(Object value) {return mundane.json.Json.write(value);}
}
