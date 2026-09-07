package engineering.budget;

import static engineering.artifacts.Checks.*;
import engineering.domain.Model;
import java.math.*;
import java.util.*;

/** Bounded dimensional interval arithmetic. Directed rounding preserves enclosure. */
public final class Quantities {
    private Quantities() {}
    public static final MathContext DOWN=new MathContext(34,RoundingMode.FLOOR);
    public static final MathContext UP=new MathContext(34,RoundingMode.CEILING);
    public record Dimension(int power,int time,int data) {
        Dimension combine(Dimension other,int sign) {
            var d=new Dimension(power+sign*other.power,time+sign*other.time,data+sign*other.data);
            if(Math.abs(d.power)>8||Math.abs(d.time)>8||Math.abs(d.data)>8)throw new IllegalArgumentException("dimension bound exceeded");return d;
        }
    }
    public record Unit(BigDecimal factor,Dimension dimension) {}
    public static Unit unit(String name) {
        return switch(name) {
            case "1" -> u("1",0,0,0);case "W" -> u("1",1,0,0);case "kW" -> u("1000",1,0,0);
            case "Wh" -> u("3600",1,1,0);case "kWh" -> u("3600000",1,1,0);
            case "s" -> u("1",0,1,0);case "ms" -> u("0.001",0,1,0);case "h" -> u("3600",0,1,0);
            case "Mbps" -> u("1000000",0,-1,1);case "bps" -> u("1",0,-1,1);
            case "GB" -> u("8000000000",0,0,1);case "MB" -> u("8000000",0,0,1);case "bit" -> u("1",0,0,1);
            default -> throw new IllegalArgumentException("unsupported unit "+name);
        };
    }
    private static Unit u(String factor,int power,int time,int data){return new Unit(new BigDecimal(factor),new Dimension(power,time,data));}
    public static BigDecimal number(Object raw) {
        if(!(raw instanceof Number))throw new IllegalArgumentException("expected numeric quantity");
        var value=new BigDecimal(raw.toString());if(value.abs().compareTo(BigDecimal.ONE.scaleByPowerOfTen(100))>0)throw new IllegalArgumentException("quantity bound exceeded");return value;
    }
    public record Interval(BigDecimal low,BigDecimal high,Dimension dimension) {
        public Interval {
            if(low.compareTo(high)>0)throw new IllegalArgumentException("reversed interval");
            number(low);number(high);
        }
        public Interval add(Interval other,boolean subtract) {
            same(other);return subtract?new Interval(low.subtract(other.high,DOWN),high.subtract(other.low,UP),dimension):
                new Interval(low.add(other.low,DOWN),high.add(other.high,UP),dimension);
        }
        public Interval multiply(Interval other,boolean divide) {
            if(divide&&other.low.signum()<=0&&other.high.signum()>=0)throw new IllegalArgumentException("division interval includes zero");
            var lows=new ArrayList<BigDecimal>();var highs=new ArrayList<BigDecimal>();
            for(var a:List.of(low,high))for(var b:List.of(other.low,other.high)) {
                lows.add(divide?a.divide(b,DOWN):a.multiply(b,DOWN));highs.add(divide?a.divide(b,UP):a.multiply(b,UP));
            }
            return new Interval(Collections.min(lows),Collections.max(highs),dimension.combine(other.dimension,divide?-1:1));
        }
        public void same(Interval other){if(!dimension.equals(other.dimension))throw new IllegalArgumentException("incompatible dimensions");}
        public Map<String,Object> output(String name) {
            var u=unit(name);if(!dimension.equals(u.dimension))throw new IllegalArgumentException("incompatible declared output unit");
            return engineering.artifacts.Json.object("min",low.divide(u.factor,DOWN),"max",high.divide(u.factor,UP),"unit",name);
        }
    }
    public static Interval parameter(Map<String,Object> p) {
        var u=unit(text(p.get("unit")));return new Interval(number(p.get("min")).multiply(u.factor,DOWN),number(p.get("max")).multiply(u.factor,UP),u.dimension);
    }
    public static final class Evaluator {
        private final Map<String,Map<String,Object>> formulas=new TreeMap<>();
        public final Map<String,Interval> values=new TreeMap<>();
        public final Map<String,Set<String>> leaves=new TreeMap<>();
        public Evaluator(Map<String,Object> data) {
            var ids=new HashSet<String>();
            for(var p:Model.rows(data,"parameters")) {
                String id=text(p.get("id"));if(!ids.add(id))throw new IllegalArgumentException("duplicate quantity ID");
                values.put(id,parameter(p));leaves.put(id,Set.of(id));
            }
            for(var f:Model.rows(data,"formulas")) {
                String id=text(f.get("id"));if(!ids.add(id))throw new IllegalArgumentException("duplicate quantity ID");formulas.put(id,f);
            }
            if(ids.size()>1000)throw new IllegalArgumentException("too many quantities");
            for(String id:formulas.keySet())evaluate(id,new HashSet<>());
        }
        public Interval get(String id){var value=values.get(id);if(value==null)throw new IllegalArgumentException("missing quantity "+id);return value;}
        private Interval evaluate(String id,Set<String> visiting) {
            if(values.containsKey(id))return values.get(id);
            if(visiting.size()>=64||!visiting.add(id))throw new IllegalArgumentException("formula cycle or depth exceeded");
            var f=formulas.get(id);if(f==null)throw new IllegalArgumentException("missing formula input "+id);
            var inputs=list(f.get("inputs"));String op=text(f.get("op"));
            if(inputs.size()<2||inputs.size()>16||Set.of("difference","ratio").contains(op)&&inputs.size()!=2)throw new IllegalArgumentException("invalid formula arity");
            Interval result=null;var leafSet=new TreeSet<String>();
            for(Object raw:inputs) {
                String input=text(raw);var next=evaluate(input,visiting);leafSet.addAll(leaves.get(input));
                if(result==null)result=next;
                else result=switch(op){case "sum"->result.add(next,false);case "difference"->result.add(next,true);case "product"->result.multiply(next,false);case "ratio"->result.multiply(next,true);default->throw new IllegalArgumentException("unsupported operation");};
            }
            result.output(text(f.get("unit")));visiting.remove(id);values.put(id,result);leaves.put(id,leafSet);return result;
        }
    }
    public static Interval availability(Interval mtbf,Interval repair) {
        mtbf.same(repair);
        if(!mtbf.dimension.equals(unit("h").dimension)||mtbf.low.signum()<=0||repair.low.signum()<0)throw new IllegalArgumentException("invalid MTBF or repair time");
        return new Interval(mtbf.low.divide(mtbf.low.add(repair.high),DOWN),mtbf.high.divide(mtbf.high.add(repair.low),UP),unit("1").dimension);
    }
}
