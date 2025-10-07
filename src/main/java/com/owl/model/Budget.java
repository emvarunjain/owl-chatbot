package com.owl.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "budgets")
public class Budget {
    @Id
    private String id;
    @Indexed(unique = true)
    private String tenantId;
    private double monthlyBudgetUsd;
    private double spentUsdCurrentMonth;
    private String monthKey; // e.g., 2025-10

    public Budget() {}
    public Budget(String tenantId, double monthlyBudgetUsd, String monthKey) {
        this.tenantId = tenantId;
        this.monthlyBudgetUsd = monthlyBudgetUsd;
        this.monthKey = monthKey;
    }

    public String getId() { return id; }
    public String getTenantId() { return tenantId; }
    public double getMonthlyBudgetUsd() { return monthlyBudgetUsd; }
    public double getSpentUsdCurrentMonth() { return spentUsdCurrentMonth; }
    public String getMonthKey() { return monthKey; }
    public void setMonthlyBudgetUsd(double v) { this.monthlyBudgetUsd = v; }
    public void setSpentUsdCurrentMonth(double v) { this.spentUsdCurrentMonth = v; }
    public void setMonthKey(String m) { this.monthKey = m; }
}

