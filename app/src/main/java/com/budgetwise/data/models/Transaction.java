package com.budgetwise.data.models;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

public class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private double amount;
    private String description;
    private String category;
    private TransactionType type;
    private Date date;
    private String notes;
    private boolean isRecurring;
    private long createdAt;
    private long updatedAt;

    public enum TransactionType {
        INCOME, EXPENSE, TRANSFER
    }

    public Transaction() {
        this.id = UUID.randomUUID().toString();
        this.date = new Date();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public Transaction(double amount, String description, String category, TransactionType type) {
        this();
        this.amount = amount;
        this.description = description;
        this.category = category;
        this.type = type;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { 
        this.amount = amount;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { 
        this.description = description;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { 
        this.category = category;
        this.updatedAt = System.currentTimeMillis();
    }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { 
        this.type = type;
        this.updatedAt = System.currentTimeMillis();
    }

    public Date getDate() { return date; }
    public void setDate(Date date) { 
        this.date = date;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { 
        this.notes = notes;
        this.updatedAt = System.currentTimeMillis();
    }

    public boolean isRecurring() { return isRecurring; }
    public void setRecurring(boolean recurring) { 
        isRecurring = recurring;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
}
