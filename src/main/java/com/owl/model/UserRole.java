package com.owl.model;

public enum UserRole {
    SUPERADMIN("Super Admin", "Controls all tenants and can access any tenant instance"),
    ADMIN("Admin", "Full access at tenant level"),
    SUPERVISOR("Supervisor", "Limited access as decided by admin"),
    AGENT("Agent", "Limited access as decided by supervisor or admin");

    private final String displayName;
    private final String description;

    UserRole(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean hasAccessTo(UserRole targetRole) {
        return this.ordinal() <= targetRole.ordinal();
    }

    public boolean canManage(UserRole targetRole) {
        return this.ordinal() < targetRole.ordinal();
    }
}
