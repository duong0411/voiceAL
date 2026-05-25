const sm2 = require('sm-crypto').sm2;

// Generate key pair
const keypair = sm2.generateKeyPairHex();
const pubKey128 = keypair.publicKey; // 128 chars
const pubKey130 = "04" + pubKey128; // 130 chars with 04 prefix

console.log("128-char public key length:", pubKey128.length);
console.log("130-char public key length:", pubKey130.length);

try {
    const encrypted128 = sm2.doEncrypt("testpassword", pubKey128, 1);
    console.log("Encryption with 128-char key SUCCESS:", encrypted128.substring(0, 20) + "...");
} catch (e) {
    console.error("Encryption with 128-char key FAILED:", e.message);
}

try {
    const encrypted130 = sm2.doEncrypt("testpassword", pubKey130, 1);
    console.log("Encryption with 130-char key SUCCESS:", encrypted130.substring(0, 20) + "...");
} catch (e) {
    console.error("Encryption with 130-char key FAILED:", e.message);
}
