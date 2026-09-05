package engineering.work;

import java.util.Map;

/** A serialized command result and its process exit status. */
public record WorkResult(Map<String,Object> output,int status) {}
