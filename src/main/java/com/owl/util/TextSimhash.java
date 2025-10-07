package com.owl.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

public final class TextSimhash {
    private TextSimhash() {}

    public static long simhash64(String text) {
        int[] bits = new int[64];
        String[] tokens = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\s]"," ").split("\\s+");
        for (String t : tokens) {
            if (t.length() < 2) continue;
            long h = hash64(t);
            for (int i=0;i<64;i++) bits[i] += ((h>>>i)&1L)==1L ? 1 : -1;
        }
        long out=0L; for (int i=0;i<64;i++) if (bits[i]>0) out |= (1L<<i);
        return out;
    }

    private static long hash64(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
            long x=0; for (int i=0;i<8;i++) x = (x<<8) | (dig[i]&0xff);
            return x;
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}

