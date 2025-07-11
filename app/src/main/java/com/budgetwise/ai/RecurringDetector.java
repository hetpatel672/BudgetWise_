package com.budgetwise.ai;

import android.content.Context;
import android.util.Log;
import com.budgetwise.data.models.Transaction;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class RecurringDetector {
    private static final String TAG = "RecurringDetector";
    private static final long DAY_MILLIS = TimeUnit.DAYS.toMillis(1);
    private static final long WEEK_MILLIS = TimeUnit.DAYS.toMillis(7);
    private static final long MONTH_MILLIS = TimeUnit.DAYS.toMillis(30);
    
    private final Context context;
    private final AINotificationManager notificationManager;

    public RecurringDetector(Context context) {
        this.context = context;
        this.notificationManager = new AINotificationManager(context);
    }

    public List<RecurringPattern> detectRecurringTransactions(List<Transaction> transactions) {
        List<RecurringPattern> patterns = new ArrayList<>();
        Map<String, List<Transaction>> groupedByDescription = groupTransactionsByDescription(transactions);
        
        for (Map.Entry<String, List<Transaction>> entry : groupedByDescription.entrySet()) {
            List<Transaction> similarTransactions = entry.getValue();
            if (similarTransactions.size() >= 3) { // Need at least 3 occurrences
                RecurringPattern pattern = analyzePattern(similarTransactions);
                if (pattern != null) {
                    patterns.add(pattern);
                    
                    // Trigger notification for newly detected recurring pattern
                    if (!pattern.isMarkedAsRecurring()) {
                        notificationManager.showReminder(
                            "Recurring Transaction Detected",
                            String.format("🔁 '%s' appears to be recurring every %d days. Mark as recurring?", 
                                pattern.getDescription(), pattern.getIntervalDays()),
                            pattern.hashCode()
                        );
                    }
                }
            }
        }
        
        return patterns;
    }

    private Map<String, List<Transaction>> groupTransactionsByDescription(List<Transaction> transactions) {
        Map<String, List<Transaction>> grouped = new HashMap<>();
        
        for (Transaction transaction : transactions) {
            String key = normalizeDescription(transaction.getDescription());
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(transaction);
        }
        
        return grouped;
    }

    private String normalizeDescription(String description) {
        return description.toLowerCase()
            .replaceAll("\\d+", "") // Remove numbers
            .replaceAll("[^a-zA-Z\\s]", "") // Remove special characters
            .trim();
    }

    private RecurringPattern analyzePattern(List<Transaction> transactions) {
        if (transactions.size() < 3) return null;
        
        // Sort by date
        transactions.sort((t1, t2) -> t1.getDate().compareTo(t2.getDate()));
        
        List<Long> intervals = new ArrayList<>();
        for (int i = 1; i < transactions.size(); i++) {
            long interval = transactions.get(i).getDate().getTime() - 
                           transactions.get(i-1).getDate().getTime();
            intervals.add(interval);
        }
        
        // Check if intervals are consistent (within 3 days tolerance)
        long avgInterval = intervals.stream().mapToLong(Long::longValue).sum() / intervals.size();
        boolean isConsistent = intervals.stream()
            .allMatch(interval -> Math.abs(interval - avgInterval) <= 3 * DAY_MILLIS);
        
        if (isConsistent) {
            RecurringType type = determineRecurringType(avgInterval);
            return new RecurringPattern(
                transactions.get(0).getDescription(),
                transactions.get(0).getCategory(),
                transactions.get(0).getAmount(),
                (int) (avgInterval / DAY_MILLIS),
                type,
                transactions.get(0).isRecurring()
            );
        }
        
        return null;
    }

    private RecurringType determineRecurringType(long intervalMillis) {
        long days = intervalMillis / DAY_MILLIS;
        
        if (days <= 1) return RecurringType.DAILY;
        if (days >= 6 && days <= 8) return RecurringType.WEEKLY;
        if (days >= 28 && days <= 32) return RecurringType.MONTHLY;
        if (days >= 88 && days <= 95) return RecurringType.QUARTERLY;
        if (days >= 360 && days <= 370) return RecurringType.YEARLY;
        
        return RecurringType.CUSTOM;
    }

    public static class RecurringPattern {
        private final String description;
        private final String category;
        private final double amount;
        private final int intervalDays;
        private final RecurringType type;
        private final boolean markedAsRecurring;

        public RecurringPattern(String description, String category, double amount, 
                              int intervalDays, RecurringType type, boolean markedAsRecurring) {
            this.description = description;
            this.category = category;
            this.amount = amount;
            this.intervalDays = intervalDays;
            this.type = type;
            this.markedAsRecurring = markedAsRecurring;
        }

        // Getters
        public String getDescription() { return description; }
        public String getCategory() { return category; }
        public double getAmount() { return amount; }
        public int getIntervalDays() { return intervalDays; }
        public RecurringType getType() { return type; }
        public boolean isMarkedAsRecurring() { return markedAsRecurring; }
    }

    public enum RecurringType {
        DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY, CUSTOM
    }
}