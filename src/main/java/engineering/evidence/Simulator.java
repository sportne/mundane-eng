package engineering.evidence;
import static engineering.artifacts.Checks.*;
import engineering.artifacts.Json;
import engineering.domain.*;
import java.util.*;

/** Deterministic event model. Deliberately never reads procedure.expected. No I/O or aircraft backend. */
public final class Simulator {
    private Simulator() {}
    public static List<Map<String,Object>> execute(Map<String,Object> p,Map<String,Object> telemetry,Map<String,Object> command,Map<String,Object> heartbeat,String fault) {
        if(!Set.of("nominal","suppress-stale").contains(fault))throw new IllegalArgumentException("unsupported simulator fault model");
        if(!p.get("method").equals("simulation"))throw new IllegalArgumentException("manual procedure cannot execute simulator");
        double freshness=quantity(telemetry,"freshness"),clockLimit=quantity(telemetry,"clockUncertainty"),ackTimeout=quantity(command,"ackTimeout"),lossTimeout=quantity(heartbeat,"lossTimeout");
        long received=-1,beat=-1,sent=-1;double sampleAge=0,uncertainty=0;String state="unknown",link="unknown",power="mains",commandState="idle",session=text(p.get("session")),request="";int queue=0;
        List<Map<String,Object>> observations=new ArrayList<>();
        for(var event:Model.rows(p,"events")) {
            long now=((Number)event.get("atMs")).longValue();String kind=text(event.get("kind"));
            if(beat>=0&&now-beat>=lossTimeout)link="lost";
            if(sent>=0&&commandState.equals("pending")&&now-sent>=ackTimeout)commandState="timed-out";
            if(kind.equals("telemetry")){received=now;sampleAge=Schema.number(event.get("ageMs"));uncertainty=Schema.number(event.get("uncertaintyMs"));}
            if(kind.equals("heartbeat")){beat=now;link="up";}
            if(kind.equals("mains-loss"))power="backup-assumed";
            if(kind.equals("link-loss")){link="lost";beat=-1;}
            if(kind.equals("restart")){session=text(event.get("session"));received=-1;beat=-1;sent=-1;queue=0;commandState="idle";link="unknown";request="";}
            state=received<0||!link.equals("up")||uncertainty>clockLimit?"unknown":sampleAge+(now-received)+uncertainty>=freshness?"stale":"fresh";
            if(kind.equals("command")) {
                if(!state.equals("fresh")||!power.equals("mains")||!session.equals(event.get("session")))commandState="inhibited";
                else if(queue==0){request=text(event.get("request"));sent=now;queue=1;commandState="pending";}
                // Existing intent is not replaced or automatically retried by another request.
            }
            if(kind.equals("ack")) {
                if(session.equals(event.get("session"))&&request.equals(event.get("request"))&&commandState.equals("pending"))commandState="acknowledged";
                else commandState="quarantined";
            }
            String display=fault.equals("suppress-stale")&&state.equals("stale")?"fresh":state;
            var values=Json.object("state",display,"commandState",commandState,"powerState",power,"linkState",link,"queueDepth",queue,"combinedFault",power.equals("backup-assumed")&&link.equals("lost"),"onboardResponse","unspecified-aircraft-local");
            for(var value:values.entrySet())observations.add(Json.object("atMs",now,"field",value.getKey(),"value",value.getValue()));
        }
        return observations;
    }
    private static double quantity(Map<String,Object> policy,String field){return Schema.number(map(map(policy.get("policy")).get(field)).get("value"));}
}
