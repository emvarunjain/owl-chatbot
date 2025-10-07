package com.owl.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class GuardrailsService {
    public enum SafetyOutcome { SAFE, REFUSE, REVIEW }

    private final boolean enabled;
    private final SafetyModelService safety;

    private static final Pattern PROHIBITED = Pattern.compile("(bomb|weapon|explosive|child\s*abuse|credit\s*card|ssn)\b", Pattern.CASE_INSENSITIVE);

    public GuardrailsService(@Value("${owl.guardrails.enabled:false}") boolean enabled,
                             SafetyModelService safety) { this.enabled = enabled; this.safety = safety; }

    public SafetyOutcome classifyQuestion(String text) {
        if (!enabled) return SafetyOutcome.SAFE;
        if (text == null || text.isBlank()) return SafetyOutcome.REVIEW;
        String out = safety.classify(text);
        if ("REFUSE".equals(out)) return SafetyOutcome.REFUSE;
        if ("SAFE".equals(out)) return SafetyOutcome.SAFE;
        return SafetyOutcome.REVIEW;
    }

    public SafetyOutcome classifyAnswer(String text) {
        if (!enabled) return SafetyOutcome.SAFE;
        if (text == null || text.isBlank()) return SafetyOutcome.REVIEW;
        String out = safety.classify(text);
        if ("REFUSE".equals(out)) return SafetyOutcome.REFUSE;
        if ("SAFE".equals(out)) return SafetyOutcome.SAFE;
        return SafetyOutcome.REVIEW;
    }
}
