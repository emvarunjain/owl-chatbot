package com.owl.retrieval.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

public class Dto {
    public record SearchReq(@NotBlank String tenantId,
                            @NotBlank String q,
                            String document,
                            @Min(1) int topK) {}

    public record Doc(String text, Map<String,Object> metadata) {}
    public record SearchRes(List<Doc> docs) {}

    public record AddReq(@NotBlank String tenantId, List<Doc> docs) {}
    public record AddRes(int added) {}
}

