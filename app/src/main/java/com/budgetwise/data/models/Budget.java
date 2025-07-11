package com.budgetwise.data.models;

import java.util.UUID;

public class Budget {
    private String id;
    private String category;
    private double budgetAmount;
    private double spentAmount;
    private BudgetPeriod period;
    private long startDate;
    private long endDate;
    private boolean isActive;
    private long createdAt;
    private long updatedAt;

    public enum BudgetPeriod {
        WEEKLY, MONTHLY, QUARTERLY, YEARLY
    }

    public Budget() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.isActive = true;
        this.spentAmount = 0.0;
    }

    public Budget(String category, double budgetAmount, BudgetPeriod period) {
        this();
        this.category = category;
        this.budgetAmount = budgetAmount;
        this.period = period;
        calculatePeriodDates();
    }

    private void calculatePeriodDates() {
        long now = System.currentTimeMillis();
        this.startDate = now;
        
        switch (period) {
            case WEEKLY:
                this.endDate = now + (7 * 24 * 60 * 60 * 1000L);
                break;
            case MONTHLY:
                this.endDate = now + (30 * 24 * 60 * 60 * 1000L);
                break;
            case QUARTERLY:
                this.endDate = now + (90 * 24 * 60 * 60 * 1000L);
                break;
            case YEARLY:
                this.endDate = now + (365 * 24 * 60 * 60 * 1000L);
                break;
        }
    }

    public double getRemainingAmount() {
        return budgetAmount - spentAmount;
    }

    public double getSpentPercentage() {
        return budgetAmount > 0 ? (spentAmount / budgetAmount) * 100 : 0;
    }

    public boolean isOverBudget() {
        return spentAmount > budgetAmount;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { 
        this.category = category;
        this.updatedAt = System.currentTimeMillis();
    }

    public double getBudgetAmount() { return budgetAmount; }
    public void setBudgetAmount(double budgetAmount) { 
        this.budgetAmount = budgetAmount;
        this.updatedAt = System.currentTimeMillis();
    }

    public double getSpentAmount() { return spentAmount; }
    public void setSpentAmount(double spentAmount) { 
        this.spentAmount = spentAmount;
        this.updatedAt = System.currentTimeMillis();
    }

    public BudgetPeriod getPeriod() { return period; }
    public void setPeriod(BudgetPeriod period) { 
        this.period = period;
        calculatePeriodDates();
        this.updatedAt = System.currentTimeMillis();
    }

    public long getStartDate() { return startDate; }
    public long getEndDate() { return endDate; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { 
        isActive = active;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
}
