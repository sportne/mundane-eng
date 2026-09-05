package mundanereq.cli;

import java.util.Arrays;
import mundanereq.SourceFormat;

record SourceInvocation(SourceFormat format, String[] arguments) {
    static SourceInvocation parse(String[] arguments) {return parse(arguments,false);}
    static SourceInvocation parse(String[] arguments,boolean attributes) {
        if (arguments.length == 0 || !arguments[0].startsWith("--source=")) {
            return new SourceInvocation(SourceFormat.CUSTOM_02, arguments);
        }
        String value = arguments[0].substring("--source=".length());
        for (SourceFormat format : SourceFormat.values()) {
            if (format.option.equals(value)) {
                if(format==SourceFormat.YAML_04&&!attributes)throw new IllegalArgumentException("this command does not support yaml-0.4");
                return new SourceInvocation(format, Arrays.copyOfRange(arguments, 1, arguments.length));
            }
        }
        throw new IllegalArgumentException("unknown source contract: " + value);
    }
}
