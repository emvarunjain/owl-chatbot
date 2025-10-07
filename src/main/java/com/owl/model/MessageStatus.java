package com.owl.model;

public enum MessageStatus {
    SENT("Sent", "Message has been sent"),
    DELIVERED("Delivered", "Message has been delivered"),
    SEEN("Seen", "Message has been seen by recipient"),
    FAILED("Failed", "Message failed to send");

    private final String displayName;
    private final String description;

    MessageStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
