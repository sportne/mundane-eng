package engineering.impact;

import static engineering.artifacts.Checks.*;
import static engineering.artifacts.Json.object;
import engineering.artifacts.Problem;
import engineering.artifacts.Snapshots;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import mundanereq.Versions;

/** Explicit compiled-file selection; callers recheck snapshots before publication. */
public final class ImpactInputs {
    public record Selection(Map<String,Object> manifest, List<Object> imports) {}
    private ImpactInputs() {}

    public static Selection read(Snapshots reads, String manifest) {
        var snapshot = reads.read(manifest);
        var declaration = map(Snapshots.json(snapshot));
        keys(declaration, "format", "imports");
        if (!Versions.IMPORT_FORMAT.equals(declaration.get("format")))
            throw new Problem("unsupported-format", "unsupported import declaration", manifest);
        var entries = list(declaration.get("imports"));
        if (entries.isEmpty() || entries.size() > 100)
            throw new Problem("impact-limit", "expected 1..100 imports", manifest);
        var scopes = new HashSet<String>();
        List<Object> imports = new ArrayList<>();
        for (Object value : entries) {
            var entry = map(value);
            keys(entry, "scope", "path", "kind", "sha256", "dependsOn");
            String scope = id(entry.get("scope"));
            if (!scopes.add(scope)) throw new Problem("duplicate-scope", "duplicate scope " + scope, manifest);
            var artifactSnapshot = reads.read(path(entry.get("path")));
            if (entry.get("sha256") != null && !digest(entry.get("sha256")).equals(artifactSnapshot.sha256()))
                throw new Problem("digest-mismatch", "artifact does not match pin for " + scope, manifest);
            var artifact = map(Snapshots.json(artifactSnapshot));
            if (!text(entry.get("kind")).equals(artifact.get("artifactKind")))
                throw new Problem("wrong-kind", "declared import kind differs", manifest);
            imports.add(object("scope", scope, "path", artifactSnapshot.path(), "sha256", artifactSnapshot.sha256(),
                    "artifact", artifact, "dependsOn", entry.get("dependsOn")));
        }
        imports.sort(Comparator.comparing(x -> text(map(x).get("scope"))));
        return new Selection(object("path", manifest, "sha256", snapshot.sha256()), imports);
    }
}
