package engineering.assurance;

import static engineering.artifacts.Checks.*;
import engineering.artifacts.*;
import engineering.domain.*;
import engineering.architecture.Architecture;
import engineering.configuration.Configuration;
import engineering.safety.Safety;
import engineering.software.Software;
import engineering.procedure.Procedure;
import engineering.evidence.Evidence;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.*;
import java.time.*;
import java.util.*;
import mundanereq.Versions;

/** Configuration-specific argument graph; observations, human decisions and authority stay distinct. */
public final class Assurance implements Model.Domain {
    public String kind(){return "assurance";}public String format(){return Versions.ASSURANCE_ARTIFACT;}public String source(){return Versions.ASSURANCE_SOURCE;}public String version(){return Versions.ASSURANCE_VERSION;}public String contract(){return Versions.ASSURANCE_CONTRACT;}
    public static Map<String,Model.Domain> adapters() {return new HashMap<>(Map.of("architecture",new Architecture(),"configuration",new Configuration(),"safety",new Safety(),"software",new Software(),"procedure",new Procedure(),"evidence",new Evidence(),"assurance",new Assurance()));}
    public static Model.Context context(Path root){return new Model.Context(root,adapters());}
    public Map<String,Object> lookup(Map<String,Object> v,String kind,String id){if(!kind.equals("assurance")||!id.equals(v.get("id")))throw new IllegalArgumentException("missing assurance target");return v;}
    public static Instant time(Object value){try{return Instant.parse(text(value));}catch(DateTimeException e){throw new IllegalArgumentException("expected explicit ISO UTC instant",e);}}
    public static Map<String,Object> selected(Model.Context c,String scope,String kind) {
        var a=c.imports.get(scope);if(a==null||!kind.equals(a.get("artifactKind")))throw new IllegalArgumentException("missing or wrong-kind scope "+scope);return a;
    }
    public static String configurationSha(Map<String,Object> v,Model.Context c){return text(c.selections.get(text(map(v.get("configuration")).get("scope"))).get("sha256"));}
    public static void sameConfiguration(Map<String,Object> a,Object reference,String sha) {
        String scope=text(map(reference).get("scope"));
        if(Model.rows(a,"imports").stream().noneMatch(i->scope.equals(i.get("scope"))&&sha.equals(i.get("sha256"))))throw new IllegalArgumentException("wrong configuration revision");
    }
    public void validate(Map<String,Object> v,Model.Context c) {
        var authored=new TreeMap<>(v);authored.put("format",source());Schema.validate(authored,Json.read(AssuranceSchema.JSON.getBytes(StandardCharsets.UTF_8)));
        c.reference(v.get("configuration"),"baseline");String config=configurationSha(v,c);
        for(String name:List.of("claims","obligations","reviews","waivers"))Model.unique(Model.rows(v,name));
        var claims=Model.rows(v,"claims");Set<String> reached=new HashSet<>();visit(text(v.get("rootClaim")),claims,new HashSet<>(),reached,0);
        if(reached.size()!=claims.size())throw new IllegalArgumentException("unreachable claim");
        for(var claim:claims) {
            boolean leaf=list(claim.get("children")).isEmpty();
            if(!leaf&&(claim.get("procedure")!=null||!list(claim.get("evidenceScopes")).isEmpty()))throw new IllegalArgumentException("aggregate support belongs to children");
            if(claim.get("procedure")!=null){c.reference(claim.get("procedure"),"procedure");var a=selected(c,text(map(claim.get("procedure")).get("scope")),"procedure");sameConfiguration(a,map(a.get("values")).get("configuration"),config);}
            for(Object ref:list(claim.get("requirements")))c.reference(ref,"requirement");
            for(Object ref:list(claim.get("safetyControls"))){c.reference(ref,"control");var a=selected(c,text(map(ref).get("scope")),"safety");sameConfiguration(a,map(map(a.get("values")).get("context")).get("configuration"),config);}
            for(Object scope:list(claim.get("softwareScopes"))){var a=selected(c,text(scope),"software");sameConfiguration(a,map(a.get("values")).get("configuration"),config);}
            for(Object scope:list(claim.get("evidenceScopes")))evidence(c,text(scope));
        }
        for(var obligation:Model.rows(v,"obligations")) {
            Model.find(claims,text(obligation.get("claim")));
            if("not-applicable".equals(obligation.get("status"))&&text(obligation.get("rationale")).isBlank())throw new IllegalArgumentException("not-applicable needs rationale");
            for(Object scope:list(obligation.get("evidenceScopes")))evidence(c,text(scope));
        }
        for(String name:List.of("reviews","waivers"))for(var record:Model.rows(v,name)) {
            Model.find(Model.rows(v,name.equals("reviews")?"claims":"obligations"),text(record.get(name.equals("reviews")?"claim":"obligation")));
            if(!time(record.get("issuedAt")).isBefore(time(record.get("expiresAt"))))throw new IllegalArgumentException("empty review validity interval");
            if(record.get("signature")!=null)c.readPinned(map(record.get("signature")));
        }
    }
    private static void visit(String id,List<Map<String,Object>> claims,Set<String> active,Set<String> reached,int depth) {
        if(depth>64||!active.add(id))throw new IllegalArgumentException("circular or excessive claim support");
        if(reached.add(id))for(Object child:list(Model.find(claims,id).get("children")))visit(text(child),claims,active,reached,depth+1);
        active.remove(id);
    }
    private static Map<String,Object> evidence(Model.Context c,String scope) {
        var a=selected(c,scope,"evidence");var d=map(a.get("values"));
        if(!map(list(a.get("sources")).get(0)).equals(map(d.get("resource"))))throw new IllegalArgumentException("evidence source inventory mismatch");return a;
    }
    public Map<String,Object> subjects(Map<String,Object> v,Model.Context c) {
        var subject=new TreeMap<>(v);subject.remove("reviews");subject.remove("waivers");
        try {return Json.object("caseSha256",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Json.bytes(Json.object("values",subject,"imports",new ArrayList<>(c.selections.values()))))),"configurationSha256",configurationSha(v,c));}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    public static String runState(Model.Context c,String scope,String expectedProcedureSha) {
        var a=evidence(c,scope);var run=map(map(a.get("values")).get("run"));var selection=Model.rows(a,"imports").get(0);
        if(expectedProcedureSha!=null&&!expectedProcedureSha.equals(selection.get("sha256")))return "stale";
        var p=map(Snapshots.json(c.readPinned(selection)));
        return text(Evidence.evaluate(map(p.get("values")),run).get("state"));
    }
    private static String combine(Collection<String> states) {
        if(states.contains("disputed")||states.contains("pass")&&states.contains("fail"))return "disputed";
        if(states.contains("stale"))return "stale";
        return !states.isEmpty()&&states.stream().allMatch(s->s.equals("supported")||s.equals("pass"))?"supported":"unsupported";
    }
    private static String support(String id,Map<String,Object> v,Model.Context c,Map<String,String> results) {
        if(results.containsKey(id))return results.get(id);var claim=Model.find(Model.rows(v,"claims"),id);var states=new ArrayList<String>();
        for(Object child:list(claim.get("children")))states.add(support(text(child),v,c,results));
        if(list(claim.get("children")).isEmpty()&&claim.get("procedure")!=null) {
            String pin=text(c.selections.get(text(map(claim.get("procedure")).get("scope"))).get("sha256"));
            for(Object scope:list(claim.get("evidenceScopes")))states.add(runState(c,text(scope),pin));
        }
        String state=combine(states);results.put(id,state);return state;
    }
    private String recordState(Map<String,Object> r,Map<String,Object> v,Model.Context c,Instant at,Map<String,PublicKey> keys) {
        var subject=subjects(v,c);
        if(!subject.get("caseSha256").equals(r.get("caseSha256"))||!subject.get("configurationSha256").equals(r.get("configurationSha256")))return "stale";
        if(at.isBefore(time(r.get("issuedAt")))||!at.isBefore(time(r.get("expiresAt"))))return "expired-or-not-yet-valid";
        if(!r.get("role").equals(v.get("requiredRole"))||list(v.get("authors")).contains(r.get("reviewer")))return "role-policy-unmet";
        return ReviewSignatures.verify(r,c,keys)?"current":"identity-unverified";
    }
    public Map<String,Object> analyze(Map<String,Object> v,Model.Context c,Instant at,Map<String,PublicKey> keys) {
        var support=new TreeMap<String,String>();support(text(v.get("rootClaim")),v,c,support);
        var reviews=new ArrayList<Map<String,Object>>();var waivers=new ArrayList<Map<String,Object>>();
        for(var r:Model.rows(v,"reviews"))reviews.add(Json.object("id",r.get("id"),"claim",r.get("claim"),"state",recordState(r,v,c,at,keys),"decision",r.get("decision")));
        for(var r:Model.rows(v,"waivers"))waivers.add(Json.object("id",r.get("id"),"obligation",r.get("obligation"),"state",recordState(r,v,c,at,keys)));
        var claims=new ArrayList<Map<String,Object>>();boolean ready=true;
        for(var claim:Model.rows(v,"claims")) {
            var selected=reviews.stream().filter(r->r.get("claim").equals(claim.get("id"))).toList();
            boolean adequate=selected.stream().anyMatch(r->r.get("state").equals("current")&&r.get("decision").equals("adequate"));
            boolean objection=selected.stream().anyMatch(r->r.get("state").equals("current")&&!r.get("decision").equals("adequate"));
            String state=support.get(claim.get("id"));if(objection)state="disputed";
            boolean ok=state.equals("supported")&&adequate&&!objection;ready&=ok;
            claims.add(Json.object("id",claim.get("id"),"observedSupport",support.get(claim.get("id")),"state",state,"reviewAdequate",adequate&&!objection,"ready",ok));
        }
        var obligations=new ArrayList<Map<String,Object>>();
        for(var o:Model.rows(v,"obligations")) {
            String state="open";
            if(o.get("status").equals("not-applicable"))state="not-applicable";
            else if(o.get("status").equals("fulfilled")&&!list(o.get("evidenceScopes")).isEmpty()&&list(o.get("evidenceScopes")).stream().allMatch(s->runState(c,text(s),null).equals("pass"))) {
                // Fulfilment evidence must also be for this exact configuration.
                for(Object scope:list(o.get("evidenceScopes"))){var e=evidence(c,text(scope));var p=map(Snapshots.json(c.readPinned(Model.rows(e,"imports").get(0))));sameConfiguration(p,map(p.get("values")).get("configuration"),configurationSha(v,c));}
                state="fulfilled";
            }
            else if(Boolean.TRUE.equals(o.get("waivable"))&&waivers.stream().anyMatch(w->w.get("obligation").equals(o.get("id"))&&w.get("state").equals("current")))state="waived";
            ready&=!state.equals("open");obligations.add(Json.object("id",o.get("id"),"claim",o.get("claim"),"state",state,"reason",o.get("description")));
        }
        return Json.object("format","mundane-assurance-analysis-0.1","id",v.get("id"),"evaluatedAt",at.toString(),"subjects",subjects(v,c),"claims",claims,"reviews",reviews,"waivers",waivers,"obligations",obligations,"localReadiness",ready,"authorization","none","limitations",v.get("limitations"));
    }
    public String view(Map<String,Object> a,Model.Context c){throw new IllegalArgumentException("view requires explicit time and caller trust set");}
    public String report(Map<String,Object> a,Model.Context c,Map<String,Object> result) {
        var v=map(a.get("values"));StringBuilder out=new StringBuilder("# Assurance readiness\n\n");
        out.append(Model.sourceLink(a,c,"/id",text(v.get("id")))).append("\n\n");
        for(String category:List.of("claims","obligations","reviews","waivers"))for(int i=0;i<Model.rows(v,category).size();i++)out.append("- ").append(Model.sourceLink(a,c,"/"+category+"/"+i+"/id",text(Model.rows(v,category).get(i).get("id")))).append("\n");
        return out+"\n```json\n"+Json.write(result)+"\n```\n\nLocal scoped checks only; no release authorization.\n";
    }
}
