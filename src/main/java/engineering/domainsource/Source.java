package engineering.domainsource;

import static engineering.artifacts.Checks.*;
import engineering.artifacts.*;
import engineering.domain.Model;
import mundane.yaml.Yaml;
import java.util.*;

/** Shared decoding/provenance only. Domain validation is an explicit owner callback. */
public final class Source {
    private Source() {}
    public static Map<String,Object> compile(String file,String selection,Model.Context context,Model.Domain domain) {return compile(file,selection,context,domain,()->{});}
    public static Map<String,Object> compile(String file,String selection,Model.Context context,Model.Domain domain,Runnable beforeRecheck) {
        var s=context.snapshots.read(file,8*1024*1024);var document=Yaml.document(s.bytes(),8*1024*1024,true);var values=map(document.value());
        if(!domain.source().equals(values.remove("format")))throw new IllegalArgumentException("unsupported-format");
        var imports=Model.imports(context.snapshots,selection);context.select(imports);domain.validate(values,context);
        var locations=new TreeMap<String,Object>();document.values().forEach((p,r)->{if(!p.equals("/format"))locations.put(p,Json.object("path",file,"line",r.start().line(),"column",r.start().column()));});
        var result=Json.object("artifactKind",domain.kind(),"format",domain.format(),"sourceContract",domain.source(),
            "compiler",Json.object("name","mundane-"+domain.kind(),"version",domain.version(),"contract",domain.contract()),"complete",true,
            "sources",List.of(Json.object("path",file,"sha256",s.sha256())),"locations",locations,"imports",imports,"values",values,"diagnostics",List.of());
        Model.envelope(result,domain);beforeRecheck.run();context.snapshots.recheck();return result;
    }
}
