package engineering.assurance;
import engineering.artifacts.Json;
import java.nio.charset.StandardCharsets;
import java.util.*;
public final class AssuranceTest {
    private AssuranceTest() {}
    public static void run() {
        byte[] expected="DSSEv1 2 é 2 {}".getBytes(StandardCharsets.UTF_8);
        if(!Arrays.equals(expected,ReviewSignatures.pae("é","{}".getBytes(StandardCharsets.UTF_8))))throw new AssertionError("DSSE byte lengths");
        if(!ReviewSignatures.keys(Json.object("keys",List.of())).isEmpty())throw new AssertionError();
        reject(()->ReviewSignatures.keys(Json.object("keys",List.of(Json.object("kty","OKP","crv","Ed25519","kid","secret","x","AAAA","d","secret")))));
        reject(()->Assurance.time("2026-09-07"));
        System.out.println("PASS assurance: DSSE UTF-8 byte lengths, explicit time and public-only trust boundary");
    }
    private static void reject(Runnable action){try{action.run();throw new AssertionError("invalid accepted");}catch(IllegalArgumentException expected){}}
}
