package com.owl.service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@org.springframework.stereotype.Service
public class CryptoService {
    public static class Enc {
        public final String ciphertextB64; public final String ivB64;
        public Enc(String c, String i){ this.ciphertextB64=c; this.ivB64=i; }
    }

    public Enc encrypt(String plaintext, String keyB64) {
        try {
            byte[] key = Base64.getDecoder().decode(keyB64);
            byte[] iv = new byte[12]; new SecureRandom().nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            byte[] ct = c.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return new Enc(Base64.getEncoder().encodeToString(ct), Base64.getEncoder().encodeToString(iv));
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public String decrypt(String ciphertextB64, String ivB64, String keyB64) {
        try {
            byte[] key = Base64.getDecoder().decode(keyB64);
            byte[] iv = Base64.getDecoder().decode(ivB64);
            byte[] ct = Base64.getDecoder().decode(ciphertextB64);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            byte[] pt = c.doFinal(ct);
            return new String(pt, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
