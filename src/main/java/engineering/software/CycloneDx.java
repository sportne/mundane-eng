package engineering.software;

import static engineering.artifacts.Checks.*;
import engineering.artifacts.Json;
import engineering.domain.Model;
import java.util.*;

/** Independently replaceable CycloneDX JSON 1.6 SBOM/VEX consumed projection. */
public final class CycloneDx {
    private CycloneDx() {}
    public record Bom(Map<String,Map<String,Object>> components, Set<String> reachable,
                      List<Map<String,Object>> vulnerabilities) {}
    public static Bom inspect(Map<String,Object> bom,String binaryDigest) {
        if(!"CycloneDX".equals(bom.get("bomFormat")) || !"1.6".equals(bom.get("specVersion")))
            throw new IllegalArgumentException("unsupported CycloneDX version");
        integer(bom.get("version"));
        var primary=map(map(bom.get("metadata")).get("component"));
        String root=text(primary.get("bom-ref"));
        if(!binaryDigest.equals(hash(primary)))throw new IllegalArgumentException("BOM binary mismatch");
        var components=new TreeMap<String,Map<String,Object>>();
        var rows=new ArrayList<Map<String,Object>>();rows.add(primary);rows.addAll(Model.rows(bom,"components"));
        for(var component:rows) {
            String ref=text(component.get("bom-ref"));
            var projection=Json.object("type",text(component.get("type")),"name",text(component.get("name")),
                "version",text(component.get("version")),"sha256",hash(component));
            if(components.putIfAbsent(ref,projection)!=null)throw new IllegalArgumentException("duplicate BOM reference");
            if(component.containsKey("components"))throw new IllegalArgumentException("nested components need a separate adapter profile");
        }
        var graph=new TreeMap<String,List<String>>();
        for(var dependency:Model.rows(bom,"dependencies")) {
            String ref=text(dependency.get("ref"));
            var targets=list(dependency.get("dependsOn")).stream().map(engineering.artifacts.Checks::text).toList();
            if(!components.containsKey(ref)||!components.keySet().containsAll(targets)||graph.putIfAbsent(ref,targets)!=null)
                throw new IllegalArgumentException("ambiguous or missing dependency reference");
        }
        if(!graph.keySet().equals(components.keySet()))throw new IllegalArgumentException("incomplete dependency projection");
        Set<String> reachable=new TreeSet<>();var pending=new ArrayDeque<String>();pending.add(root);
        while(!pending.isEmpty()){String ref=pending.remove();if(reachable.add(ref))pending.addAll(graph.get(ref));}
        var findings=new ArrayList<Map<String,Object>>();Set<String> ids=new HashSet<>();
        for(Object raw:list(bom.getOrDefault("vulnerabilities",List.of()))) {
            var vulnerability=map(raw);String ident=text(vulnerability.get("id"));
            if(!ids.add(ident))throw new IllegalArgumentException("ambiguous advisory identity");
            if(Model.rows(vulnerability,"affects").isEmpty())throw new IllegalArgumentException("advisory has no scoped affected component");
            for(var affected:Model.rows(vulnerability,"affects")) {
                String ref=text(affected.get("ref"));
                if(!components.containsKey(ref))throw new IllegalArgumentException("unknown affected component");
                if(affected.containsKey("versions"))throw new IllegalArgumentException("version-range VEX needs a separate adapter profile");
                var analysis=map(vulnerability.getOrDefault("analysis",Map.of()));
                String state=text(analysis.getOrDefault("state","not-provided"));
                if(!Set.of("resolved","resolved_with_pedigree","exploitable","in_triage","false_positive","not_affected","not-provided").contains(state))
                    throw new IllegalArgumentException("unsupported VEX state");
                findings.add(Json.object("advisory",ident,"component",ref,"reachable",reachable.contains(ref),
                    "vexState",state,"detail",analysis.getOrDefault("detail","not provided")));
            }
        }
        return new Bom(components,reachable,findings);
    }
    private static String hash(Map<String,Object> component) {
        var hashes=Model.rows(component,"hashes").stream().filter(h->"SHA-256".equals(h.get("alg"))).toList();
        if(hashes.size()!=1)throw new IllegalArgumentException("exact component SHA-256 required");
        return digest(hashes.get(0).get("content"));
    }
}
