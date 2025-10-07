package com.owl.controller;

import com.owl.model.ChatRecord;
import com.owl.service.ChatHistoryService;
import com.owl.service.EventPublisher;
import com.owl.service.PreferenceService;
import com.owl.service.TenantMongoManager;
import com.owl.model.FeedbackRecord;
import com.owl.security.TenantAuth;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/v1/feedback", "/api/feedback"})
public class FeedbackController {
    public record FeedbackRequest(@NotBlank String tenantId, @NotBlank String chatId,
                                  @Min(1) @Max(5) int rating, Boolean helpful, String comment) {}

    private final TenantMongoManager tenants;
    private final ChatHistoryService history;
    private final PreferenceService prefs;
    private final EventPublisher events;
    private final TenantAuth auth;

    public FeedbackController(TenantMongoManager tenants, ChatHistoryService history,
                              PreferenceService prefs, EventPublisher events, TenantAuth auth) {
        this.tenants = tenants;
        this.history = history;
        this.prefs = prefs;
        this.events = events;
        this.auth = auth;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> submit(@RequestBody FeedbackRequest req) {
        auth.authorize(req.tenantId());
        MongoTemplate tpl = tenants.templateForTenant(req.tenantId());
        FeedbackRecord rec = new FeedbackRecord(req.tenantId(), req.chatId(), req.rating(), req.helpful(), req.comment());
        tpl.save(rec);

        ChatRecord chat = history.getById(req.tenantId(), req.chatId());
        if (chat != null && (req.rating() >= 4 || Boolean.TRUE.equals(req.helpful()))) {
            List<String> sources = chat.getSources() == null ? List.of() : chat.getSources();
            prefs.save(req.tenantId(), chat.getQuestion(), chat.getAnswer(), sources, req.rating());
        }
        events.audit(req.tenantId(), "user", "FEEDBACK", Map.of("chatId", req.chatId(), "rating", req.rating()));
        events.feedback(req.tenantId(), req.chatId(), req.rating());
        return ResponseEntity.ok(Map.of("status", "ok", "id", rec.getId()));
    }
}
