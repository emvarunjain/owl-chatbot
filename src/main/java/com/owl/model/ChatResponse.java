package com.owl.model;

import java.util.List;

/** Canonical chat response DTO for the API. */
public record ChatResponse(String answer, List<String> sources, String chatId, String safety) { }
