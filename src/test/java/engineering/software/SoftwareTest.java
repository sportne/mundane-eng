package engineering.software;
import engineering.artifacts.Json;
import java.util.*;
public final class SoftwareTest {
    private SoftwareTest() {}
    public static void run() {
        String pin="a".repeat(64);
        var origin=Json.object("uri","urn:source","sha256",pin);
        var build=Json.object("binary",Json.object("sha256",pin),"source",origin,"recipe",origin);
        var definition=Json.object("buildType","urn:recipe","resolvedDependencies",List.of(Json.object("uri","urn:source","digest",Json.object("sha256",pin))));
        var statement=Json.object("_type","https://in-toto.io/Statement/v1","predicateType","https://slsa.dev/provenance/v1",
            "subject",List.of(Json.object("digest",Json.object("sha256",pin))),"predicate",Json.object("buildDefinition",definition,"runDetails",Json.object("builder",Json.object("id","urn:builder"))));
        Slsa.inspect(statement,build);statement.put("futureField",true);Slsa.inspect(statement,build);
        definition.put("resolvedDependencies",List.of());reject(()->Slsa.inspect(statement,build));
        var component=Json.object("type","application","bom-ref","host","name","host","version","1","hashes",List.of(Json.object("alg","SHA-256","content",pin)));
        var bom=Json.object("bomFormat","CycloneDX","specVersion","1.6","version",1,"metadata",Json.object("component",component),"components",List.of(),"dependencies",List.of(Json.object("ref","host","dependsOn",List.of("host"))));
        if(!CycloneDx.inspect(bom,pin).reachable().equals(Set.of("host")))throw new AssertionError("cyclic dependency traversal");
        reject(()->CycloneDx.inspect(bom,"b".repeat(64)));
        bom.put("dependencies",List.of());reject(()->CycloneDx.inspect(bom,pin));
        bom.put("specVersion","1.7");reject(()->CycloneDx.inspect(bom,pin));
        System.out.println("PASS native standards adapters: SLSA extension fields and revision rejection; bounded BOM dependency cycles, identity and version rejection");
    }
    private static void reject(Runnable action){try{action.run();throw new AssertionError("invalid input accepted");}catch(IllegalArgumentException expected){}}
}
