package com.owl.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class DlpService {
    private final boolean enabled;

    private static final Pattern EMAIL = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[A-Za-z]{2,6}");
    private static final Pattern SSN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern PHONE = Pattern.compile("(\\+?\\d{1,3}[ -]?)?(?:(?:\\(\\d{3}\\))|\\d{3})[ -]?\\d{3}[ -]?\\d{4}");

    public DlpService(@Value("${owl.dlp.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    public String redact(String text) {
        if (!enabled || text == null || text.isBlank()) return text;
        String t = EMAIL.matcher(text).replaceAll("[redacted-email]");
        t = SSN.matcher(t).replaceAll("[redacted-ssn]");
        t = PHONE.matcher(t).replaceAll("[redacted-phone]");
        return t;
    }
}

