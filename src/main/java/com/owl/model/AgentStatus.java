package com.owl.model;

public enum AgentStatus {
    ONLINE("Online", "Agent is online and available"),
    AVAILABLE("Available", "Agent is available for new chats"),
    BUSY("Busy", "Agent is currently handling a chat"),
    OFFLINE("Offline", "Agent is offline"),
    BREAK("Break", "Agent is on break");

    private final String displayName;
    private final String description;

    AgentStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean canReceiveChats() {
        return this == AVAILABLE;
    }

    public boolean isActive() {
        return this == ONLINE || this == AVAILABLE || this == BUSY;
    }
}
