package com.owl.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Chat record stored in a per-tenant database (collection: chats).
 */
@Document(collection = "chats")
public class ChatRecord {
    @Id
    private String id;

    @Indexed
    private String tenantId;

    private String question;
    private String answer;
    private boolean cacheHit;
    private List<String> sources; // filenames or URLs
    private boolean encrypted;
    private String iv; // base64 IV when encrypted

    private OffsetDateTime createdAt = OffsetDateTime.now();

    public ChatRecord() {}

    public ChatRecord(String tenantId, String question, String answer, boolean cacheHit, List<String> sources) {
        this.tenantId = tenantId;
        this.question = question;
        this.answer = answer;
        this.cacheHit = cacheHit;
        this.sources = sources;
    }

    public String getId() { return id; }
    public String getTenantId() { return tenantId; }
    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public boolean isCacheHit() { return cacheHit; }
    public List<String> getSources() { return sources; }
    public boolean isEncrypted() { return encrypted; }
    public String getIv() { return iv; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public void setId(String id) { this.id = id; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public void setQuestion(String question) { this.question = question; }
    public void setAnswer(String answer) { this.answer = answer; }
    public void setCacheHit(boolean cacheHit) { this.cacheHit = cacheHit; }
    public void setSources(List<String> sources) { this.sources = sources; }
    public void setEncrypted(boolean encrypted) { this.encrypted = encrypted; }
    public void setIv(String iv) { this.iv = iv; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
