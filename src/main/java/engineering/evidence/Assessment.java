package engineering.evidence;
import engineering.domain.*;
import engineering.artifacts.Json;
import java.nio.charset.StandardCharsets;
import java.util.*;
import mundanereq.Versions;
public final class Assessment implements Model.Domain {
    public String kind(){return "assessment";}public String format(){return Versions.ASSESSMENT_ARTIFACT;}public String source(){return Versions.ASSESSMENT_SOURCE;}public String version(){return Versions.EVIDENCE_VERSION;}public String contract(){return Versions.EVIDENCE_CONTRACT;}
    public void validate(Map<String,Object> d,Model.Context c){var authored=new TreeMap<>(d);authored.put("format",source());Schema.validate(authored,Json.read(AssessmentSchema.JSON.getBytes(StandardCharsets.UTF_8)));if(!c.imports.isEmpty())throw new IllegalArgumentException("assessment imports are not supported");}
    public String view(Map<String,Object> a,Model.Context c){return "# Authored assessment\n\n"+Model.escape(Json.write(a.get("values")))+"\n\nReviewer identity and authorization are not authenticated.\n";}
}
