package com.owl.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.OffsetDateTime;

@Document(collection = "feedbacks")
public class FeedbackRecord {
    @Id
    private String id;
    @Indexed private String tenantId;
    private String chatId;
    private int rating; // 1..5
    private Boolean helpful; // optional thumbs up/down
    private String comment;
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public FeedbackRecord() {}
    public FeedbackRecord(String tenantId, String chatId, int rating, Boolean helpful, String comment) {
        this.tenantId = tenantId;
        this.chatId = chatId;
        this.rating = rating;
        this.helpful = helpful;
        this.comment = comment;
    }

    public String getId() { return id; }
    public String getTenantId() { return tenantId; }
    public String getChatId() { return chatId; }
    public int getRating() { return rating; }
    public Boolean getHelpful() { return helpful; }
    public String getComment() { return comment; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}

