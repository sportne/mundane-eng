package engineering.assurance;

import static engineering.artifacts.Checks.*;
import engineering.artifacts.*;
import engineering.domain.Model;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

/** Deliberately small DSSE/Ed25519/public-JWK adapter. Trust selection belongs to the caller. */
public final class ReviewSignatures {
    private ReviewSignatures() {}
    public static final String TYPE="application/vnd.mundane.review+json";
    public static Map<String,PublicKey> keys(Object document) {
        var result=new TreeMap<String,PublicKey>();var entries=list(map(document).get("keys"));
        if(entries.size()>1000)throw new IllegalArgumentException("too many trust keys");
        for(Object item:entries) {
            var key=map(item);
            if(key.containsKey("d")||!"OKP".equals(key.get("kty"))||!"Ed25519".equals(key.get("crv")) ||
                key.containsKey("use")&&!"sig".equals(key.get("use")) || key.containsKey("key_ops")&&!list(key.get("key_ops")).equals(List.of("verify")))
                throw new IllegalArgumentException("expected public Ed25519 verification JWK");
            String kid=id(key.get("kid"));byte[] raw=Base64.getUrlDecoder().decode(text(key.get("x")));
            if(raw.length!=32)throw new IllegalArgumentException("invalid Ed25519 public key length");
            byte[] prefix=HexFormat.of().parseHex("302a300506032b6570032100"),der=Arrays.copyOf(prefix,prefix.length+raw.length);
            System.arraycopy(raw,0,der,prefix.length,raw.length);
            try {if(result.putIfAbsent(kid,KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(der)))!=null)throw new IllegalArgumentException("duplicate trusted key ID");}
            catch(GeneralSecurityException e){throw new IllegalArgumentException("invalid Ed25519 public key",e);}
        }
        return result;
    }
    public static byte[] pae(String type,byte[] payload) {
        byte[] prefix=("DSSEv1 "+type.getBytes(StandardCharsets.UTF_8).length+" "+type+" "+payload.length+" ").getBytes(StandardCharsets.UTF_8);
        byte[] bytes=Arrays.copyOf(prefix,prefix.length+payload.length);System.arraycopy(payload,0,bytes,prefix.length,payload.length);return bytes;
    }
    public static boolean verify(Map<String,Object> record,Model.Context c,Map<String,PublicKey> trusted) {
        if(record.get("signature")==null)return false;
        var envelope=map(Snapshots.json(c.readPinned(map(record.get("signature")))));
        try {
            if(!TYPE.equals(envelope.get("payloadType")))return false;
            byte[] payload=Base64.getDecoder().decode(text(envelope.get("payload")));
            var expected=new TreeMap<>(record);expected.remove("signature");
            if(!Json.write(Json.read(payload)).equals(Json.write(expected)))return false;
            var signatures=list(envelope.get("signatures"));if(signatures.size()>1000)return false;
            for(Object item:signatures) {
                var sig=map(item);String kid=text(sig.get("keyid"));var key=trusted.get(kid);
                if(key==null||!kid.equals(record.get("reviewer")))continue;
                var verifier=Signature.getInstance("Ed25519");verifier.initVerify(key);verifier.update(pae(TYPE,payload));
                if(verifier.verify(Base64.getDecoder().decode(text(sig.get("sig")))))return true;
            }
            return false;
        } catch(GeneralSecurityException|IllegalArgumentException e){return false;}
    }
}
