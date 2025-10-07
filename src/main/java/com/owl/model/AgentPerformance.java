package com.owl.model;

import java.time.LocalDateTime;
import java.util.Map;

public class AgentPerformance {
    private int totalChats;
    private int completedChats;
    private int transferredChats;
    private double averageResponseTime; // in seconds
    private double averageChatDuration; // in minutes
    private double customerSatisfactionScore; // 1-5 scale
    private int totalMessages;
    private LocalDateTime lastUpdated;
    
    // Performance by time period
    private Map<String, PerformanceMetrics> dailyMetrics;
    private Map<String, PerformanceMetrics> weeklyMetrics;
    private Map<String, PerformanceMetrics> monthlyMetrics;
    
    public AgentPerformance() {
        this.totalChats = 0;
        this.completedChats = 0;
        this.transferredChats = 0;
        this.averageResponseTime = 0.0;
        this.averageChatDuration = 0.0;
        this.customerSatisfactionScore = 0.0;
        this.totalMessages = 0;
        this.lastUpdated = LocalDateTime.now();
    }
    
    // Getters and Setters
    public int getTotalChats() { return totalChats; }
    public void setTotalChats(int totalChats) { this.totalChats = totalChats; }
    
    public int getCompletedChats() { return completedChats; }
    public void setCompletedChats(int completedChats) { this.completedChats = completedChats; }
    
    public int getTransferredChats() { return transferredChats; }
    public void setTransferredChats(int transferredChats) { this.transferredChats = transferredChats; }
    
    public double getAverageResponseTime() { return averageResponseTime; }
    public void setAverageResponseTime(double averageResponseTime) { this.averageResponseTime = averageResponseTime; }
    
    public double getAverageChatDuration() { return averageChatDuration; }
    public void setAverageChatDuration(double averageChatDuration) { this.averageChatDuration = averageChatDuration; }
    
    public double getCustomerSatisfactionScore() { return customerSatisfactionScore; }
    public void setCustomerSatisfactionScore(double customerSatisfactionScore) { this.customerSatisfactionScore = customerSatisfactionScore; }
    
    public int getTotalMessages() { return totalMessages; }
    public void setTotalMessages(int totalMessages) { this.totalMessages = totalMessages; }
    
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    
    public Map<String, PerformanceMetrics> getDailyMetrics() { return dailyMetrics; }
    public void setDailyMetrics(Map<String, PerformanceMetrics> dailyMetrics) { this.dailyMetrics = dailyMetrics; }
    
    public Map<String, PerformanceMetrics> getWeeklyMetrics() { return weeklyMetrics; }
    public void setWeeklyMetrics(Map<String, PerformanceMetrics> weeklyMetrics) { this.weeklyMetrics = weeklyMetrics; }
    
    public Map<String, PerformanceMetrics> getMonthlyMetrics() { return monthlyMetrics; }
    public void setMonthlyMetrics(Map<String, PerformanceMetrics> monthlyMetrics) { this.monthlyMetrics = monthlyMetrics; }
    
    // Helper methods
    public double getCompletionRate() {
        return totalChats > 0 ? (double) completedChats / totalChats * 100 : 0.0;
    }
    
    public double getTransferRate() {
        return totalChats > 0 ? (double) transferredChats / totalChats * 100 : 0.0;
    }
    
    public void updatePerformance(int chatsCompleted, int chatsTransferred, double responseTime, double chatDuration, int messages) {
        this.completedChats += chatsCompleted;
        this.transferredChats += chatsTransferred;
        this.totalMessages += messages;
        
        // Update averages
        int totalCompleted = this.completedChats + this.transferredChats;
        if (totalCompleted > 0) {
            this.averageResponseTime = (this.averageResponseTime * (totalCompleted - 1) + responseTime) / totalCompleted;
            this.averageChatDuration = (this.averageChatDuration * (totalCompleted - 1) + chatDuration) / totalCompleted;
        }
        
        this.lastUpdated = LocalDateTime.now();
    }
    
    public static class PerformanceMetrics {
        private int chats;
        private double responseTime;
        private double satisfaction;
        private int messages;
        
        public PerformanceMetrics() {}
        
        public PerformanceMetrics(int chats, double responseTime, double satisfaction, int messages) {
            this.chats = chats;
            this.responseTime = responseTime;
            this.satisfaction = satisfaction;
            this.messages = messages;
        }
        
        // Getters and Setters
        public int getChats() { return chats; }
        public void setChats(int chats) { this.chats = chats; }
        
        public double getResponseTime() { return responseTime; }
        public void setResponseTime(double responseTime) { this.responseTime = responseTime; }
        
        public double getSatisfaction() { return satisfaction; }
        public void setSatisfaction(double satisfaction) { this.satisfaction = satisfaction; }
        
        public int getMessages() { return messages; }
        public void setMessages(int messages) { this.messages = messages; }
    }
}
