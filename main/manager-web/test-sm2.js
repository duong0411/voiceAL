const sm2 = require('sm-crypto').sm2;
const keypair = sm2.generateKeyPairHex();
const pubKey = keypair.publicKey;

const encrypted = sm2.doEncrypt("testpassword", pubKey, 1);
console.log("sm-crypto doEncrypt output length:", encrypted.length);
console.log("sm-crypto doEncrypt starts with 04:", encrypted.startsWith("04"));
console.log("sm-crypto doEncrypt first 10 chars:", encrypted.substring(0, 10));
