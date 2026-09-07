package engineering.software;

import static engineering.artifacts.Checks.*;
import engineering.domain.Model;
import java.util.*;

/** Consumes the SLSA v1 correspondence projection, not signatures or builder trust. */
public final class Slsa {
    private Slsa() {}
    public static Map<String,Object> inspect(Map<String,Object> statement, Map<String,Object> build) {
        if (!"https://in-toto.io/Statement/v1".equals(statement.get("_type")) ||
            !"https://slsa.dev/provenance/v1".equals(statement.get("predicateType")))
            throw new IllegalArgumentException("unsupported provenance version or envelope");
        var binary=map(build.get("binary"));
        long matches=Model.rows(statement,"subject").stream().filter(s ->
            binary.get("sha256").equals(map(s.get("digest")).get("sha256"))).count();
        if(matches!=1) throw new IllegalArgumentException("binary subject missing or ambiguous");
        var predicate=map(statement.get("predicate"));
        var definition=map(predicate.get("buildDefinition"));
        String buildType=text(definition.get("buildType"));
        String builder=text(map(map(predicate.get("runDetails")).get("builder")).get("id"));
        var dependencies=Model.rows(definition,"resolvedDependencies");
        for(String role:List.of("source","recipe")) {
            var expected=map(build.get(role));
            var matchesUri=dependencies.stream().filter(d -> expected.get("uri").equals(d.get("uri"))).toList();
            if(matchesUri.size()!=1 || !expected.get("sha256").equals(map(matchesUri.get(0).get("digest")).get("sha256")))
                throw new IllegalArgumentException("missing or mismatched "+role+" revision");
        }
        return engineering.artifacts.Json.object("buildType",buildType,"builder",builder,"authentication","not-verified");
    }
}
