package engineering.work;

import static engineering.artifacts.Checks.*;
import engineering.artifacts.Json;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Work-item meanings shared by source construction and strict serialized validation. */
public final class WorkValues {
    private WorkValues() {}
    public static final Set<String> TASK_STATUS=Set.of("Ready","Planned","Conditional","In progress","Complete","Superseded");
    public static final Set<String> RELATIONS=Set.of("addresses","relates-to","supersedes","evidence");
    public static final Set<String> ISSUE_STATUS=Set.of("Open","Closed","Superseded");
    public static String single(Object value) {
        String s=text(value);
        if(s.codePoints().anyMatch(c->c<32||c>=127&&c<=159)||space(s.codePointAt(0))||space(s.codePointBefore(s.length())))
            throw new IllegalArgumentException("expected unpadded single-line text");
        return s;
    }
    private static boolean space(int c) {return Character.isWhitespace(c)||Character.isSpaceChar(c);}
    public static String string(Object value) {
        if(!(value instanceof String s)||s.indexOf(0)>=0||s.codePoints().anyMatch(c->c>=0xd800&&c<=0xdfff))
            throw new IllegalArgumentException("expected Unicode string without NUL");
        return s;
    }
    public static void validate(Map<String,Object> v) {
        keys(v,"id","kind","title","status","dependencies","relations","planning","body");
        id(v.get("id"));single(v.get("title"));String kind=text(v.get("kind"));
        if(!Set.of("task","issue").contains(kind))throw new IllegalArgumentException("unknown work-item kind");
        if(!(kind.equals("task")?TASK_STATUS:ISSUE_STATUS).contains(text(v.get("status"))))throw new IllegalArgumentException("invalid work-item status");
        var deps=list(v.get("dependencies"));if(deps.size()>1000||kind.equals("issue")&&!deps.isEmpty())throw new IllegalArgumentException("invalid dependency count/kind");
        Set<String> ids=new HashSet<>();for(Object dep:deps)if(!ids.add(id(dep)))throw new IllegalArgumentException("duplicate dependency");
        var plan=map(v.get("planning"));keys(plan,"stage","type","condition","unlocks","statusNote");for(Object value:plan.values())string(value);
        if(string(v.get("body")).isBlank())throw new IllegalArgumentException("empty work-item body");
        var relations=list(v.get("relations"));if(relations.size()>1000)throw new IllegalArgumentException("too many relations");
        Set<String> seen=new HashSet<>();
        for(Object value:relations) {
            var r=map(value);keys(r,"relation","scope","kind","target");String role=text(r.get("relation")),targetKind=text(r.get("kind"));
            if(role.equals("evidence")) {
                if(!targetKind.equals("resource")||r.get("scope")!=null)throw new IllegalArgumentException("evidence must cite an unscoped local resource");
                path(r.get("target"));
            } else {
                if(!RELATIONS.contains(role))throw new IllegalArgumentException("unknown relation");
                id(r.get("scope"));id(r.get("target"));
                if(!Set.of("work-item","requirement","verification-plan","verification-activity").contains(targetKind)||role.equals("supersedes")&&!targetKind.equals("work-item"))
                    throw new IllegalArgumentException("unsupported relation target kind");
            }
            if(!seen.add(Json.write(r)))throw new IllegalArgumentException("duplicate relation");
        }
    }
}
