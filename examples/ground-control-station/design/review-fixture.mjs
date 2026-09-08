// Test-only signer: keys live in an explicitly supplied temporary directory, never source.
import { generateKeyPairSync, createPrivateKey, sign, verify } from 'node:crypto';
import { readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
const [command,dir,input,output]=process.argv.slice(2);
const type='application/vnd.mundane.review+json';
function pae(payload){return Buffer.concat([Buffer.from(`DSSEv1 ${Buffer.byteLength(type)} ${type} ${payload.length} `),payload]);}
if(command==='keygen') {
  const {privateKey,publicKey}=generateKeyPairSync('ed25519');
  writeFileSync(join(dir,'private.pem'),privateKey.export({format:'pem',type:'pkcs8'}),{mode:0o600});
  writeFileSync(join(dir,'trust.jwks.json'),JSON.stringify({keys:[{...publicKey.export({format:'jwk'}),kid:'fixture-reviewer',use:'sig',key_ops:['verify']}]}));
} else if(command==='sign') {
  const payload=readFileSync(input);const key=createPrivateKey(readFileSync(join(dir,'private.pem')));
  const signature=sign(null,pae(payload),key);
  if(!verify(null,pae(payload),key,signature))throw new Error('fixture signature verification failed');
  writeFileSync(output,JSON.stringify({payloadType:type,payload:payload.toString('base64'),signatures:[{keyid:'fixture-reviewer',sig:signature.toString('base64')}]}));
} else {throw new Error('unknown fixture command');}
