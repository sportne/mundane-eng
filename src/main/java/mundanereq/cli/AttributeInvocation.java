package mundanereq.cli;

import java.nio.file.Path;
import java.util.ArrayList;
import mundanereq.SourceFormat;

record AttributeInvocation(Path schema,String[] arguments) {
    static AttributeInvocation parse(String[] args,SourceFormat format) {
        var remaining=new ArrayList<String>();Path schema=null;boolean ended=false;
        for(int i=0;i<args.length;i++) {
            String arg=args[i];if(arg.equals("--"))ended=true;
            if(!ended&&arg.equals("--attribute-schema")) {
                if(format!=SourceFormat.YAML_04||schema!=null||i+1==args.length)throw new IllegalArgumentException("--attribute-schema requires yaml-0.4 and exactly one path");
                schema=Path.of(args[++i]);
            }else remaining.add(arg);
        }
        if(schema!=null&&(remaining.contains("--help")||remaining.contains("--version")))throw new IllegalArgumentException("schema selection requires source inputs");
        return new AttributeInvocation(schema,remaining.toArray(String[]::new));
    }
}
