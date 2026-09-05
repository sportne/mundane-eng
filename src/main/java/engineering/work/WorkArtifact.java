package engineering.work;

import static engineering.artifacts.Checks.*;
import engineering.artifacts.Problem;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.HashSet;
import mundanereq.Versions;

/** Serialized-only validation: no work-item or requirement source parser dependency. */
public final class WorkArtifact {
    private WorkArtifact() {}
    public static Map<String,Map<String,Object>> validate(Map<String,Object> a,String file) {
        try {
            keys(a,"artifactKind","format","sourceContract","compiler","complete","selection","sources","items","diagnostics");
            if(!"work-items".equals(a.get("artifactKind")))throw new Problem("wrong-kind","expected work-items",file);
            if(!Versions.WORK_ARTIFACT.equals(a.get("format"))||!Versions.WORK_SOURCE.equals(a.get("sourceContract")))throw new Problem("unsupported-format","unsupported work-item format",file);
            if(!Boolean.TRUE.equals(a.get("complete")))throw new Problem("incomplete-import","work-item artifact is incomplete",file);
            if(!list(a.get("diagnostics")).isEmpty())throw new IllegalArgumentException("complete artifact has diagnostics");
            var compiler=map(a.get("compiler"));keys(compiler,"name","version","contract");for(Object v:compiler.values())text(v);
            snapshot(a.get("selection"));Set<String> paths=new HashSet<>();
            for(Object source:list(a.get("sources")))if(!paths.add(snapshot(source)))throw new IllegalArgumentException("duplicate source path");
            var items=list(a.get("items"));if(items.isEmpty()||items.size()>10000||paths.size()!=items.size())throw new IllegalArgumentException("invalid work inventory size");
            Map<String,Map<String,Object>> result=new TreeMap<>();Set<String> usedPaths=new HashSet<>();
            for(Object item:items) {
                var r=map(item);keys(r,"values","location","metadataLocation");var v=map(r.get("values"));WorkValues.validate(v);
                var loc=location(r.get("location"),paths);var meta=location(r.get("metadataLocation"),paths);
                if(integer(loc.get("line"))!=1||integer(loc.get("column"))!=1||integer(meta.get("line"))!=4||integer(meta.get("column"))!=1||!loc.get("path").equals(meta.get("path"))||!usedPaths.add(text(loc.get("path"))))throw new IllegalArgumentException("invalid work source locations");
                if(result.put(id(v.get("id")),r)!=null)throw new IllegalArgumentException("duplicate work ID");
            }
            return result;
        } catch(IllegalArgumentException ex) {throw new Problem("invalid-work-artifact",ex.getMessage(),file);}
    }
    public static String snapshot(Object value) {var s=map(value);keys(s,"path","sha256");digest(s.get("sha256"));return path(s.get("path"));}
}
