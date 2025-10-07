package com.owl.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "plans")
public class Plan {
    @Id
    private String id;
    @Indexed(unique = true)
    private String name; // free|pro|enterprise or custom
    private int monthlyRequests;
    private int burstCredits;
    private double monthlyBudgetUsd;
    private String sla; // e.g., 99.5%

    public Plan() {}
    public Plan(String name, int monthlyRequests, int burstCredits, double monthlyBudgetUsd, String sla) {
        this.name = name; this.monthlyRequests = monthlyRequests; this.burstCredits = burstCredits; this.monthlyBudgetUsd = monthlyBudgetUsd; this.sla = sla;
    }
    public String getId() { return id; }
    public String getName() { return name; }
    public int getMonthlyRequests() { return monthlyRequests; }
    public int getBurstCredits() { return burstCredits; }
    public double getMonthlyBudgetUsd() { return monthlyBudgetUsd; }
    public String getSla() { return sla; }
}

