package mundanereq.cli;

import java.util.Arrays;
import mundanereq.SourceFormat;

/** Select the leading source profile shared by the requirement commands. */
record SourceInvocation(SourceFormat format, String[] arguments) {
    static SourceInvocation parse(String[] arguments) {
        if (arguments.length == 0 || !arguments[0].startsWith("--source=")) {
            return new SourceInvocation(SourceFormat.YAML_03, arguments);
        }
        String value = arguments[0].substring("--source=".length());
        for (SourceFormat format : SourceFormat.values()) {
            if (format.option.equals(value)) {
                return new SourceInvocation(format, Arrays.copyOfRange(arguments, 1, arguments.length));
            }
        }
        throw new IllegalArgumentException("unknown source contract: " + value);
    }
}
