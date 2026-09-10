package engineering.editor;
import engineering.artifacts.Json;
public final class EngineeringEditorMain {
    private EngineeringEditorMain(){}
    public static void main(String[] args){
        try{
            if(args.length==1&&args[0].equals("--version"))System.out.write(Json.bytes(Json.object("version",mundanereq.Versions.ENGINEERING_EDITOR_VERSION,"protocol",mundanereq.Versions.EDITOR_PROTOCOL,"source",EngineeringEditor.SOURCE)));
            else{
                if(args.length!=0)throw new IllegalArgumentException("engineering bridge accepts stdin only");
                byte[] input=System.in.readNBytes(16*1024*1024+1);if(input.length>16*1024*1024)throw new IllegalArgumentException("request exceeds 16 MiB");
                byte[] output=Json.bytes(EngineeringEditor.analyze(Json.read(input)));if(output.length>16*1024*1024)throw new IllegalArgumentException("response exceeds 16 MiB");System.out.write(output);
            }
            System.out.flush();if(System.out.checkError())throw new java.io.IOException("output unavailable");
        }catch(java.io.IOException|IllegalArgumentException|engineering.artifacts.Problem e){System.err.println("engineering-editor: "+e.getMessage());System.exit(2);}
    }
}
