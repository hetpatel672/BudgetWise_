package com.budgetwise.ai;

import android.content.Context;
import com.budgetwise.data.models.Transaction;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class CooldownDetector {
    private static final long RAPID_THRESHOLD = TimeUnit.MINUTES.toMillis(15); // 15 minutes
    private static final int MIN_TRANSACTIONS_FOR_ALERT = 3;
    private static final double HIGH_AMOUNT_THRESHOLD = 100.0; // $100+
    
    private final Context context;
    private final AINotificationManager notificationManager;

    public CooldownDetector(Context context) {
        this.context = context;
        this.notificationManager = new AINotificationManager(context);
    }

    public CooldownResult analyzeSpendingPattern(List<Transaction> transactions) {
        // Sort transactions by date (most recent first)
        List<Transaction> sortedTransactions = new ArrayList<>(transactions);
        sortedTransactions.sort((t1, t2) -> t2.getDate().compareTo(t1.getDate()));
        
        // Analyze recent spending bursts
        List<SpendingBurst> bursts = detectSpendingBursts(sortedTransactions);
        
        // Check for current rapid spending
        boolean isCurrentlyRapidSpending = isCurrentlyInRapidSpending(sortedTransactions);
        
        // Generate recommendations
        List<String> recommendations = generateCooldownRecommendations(bursts, isCurrentlyRapidSpending);
        
        // Trigger notifications if needed
        if (isCurrentlyRapidSpending) {
            triggerCooldownNotification(sortedTransactions);
        }
        
        return new CooldownResult(bursts, isCurrentlyRapidSpending, recommendations);
    }

    private List<SpendingBurst> detectSpendingBursts(List<Transaction> transactions) {
        List<SpendingBurst> bursts = new ArrayList<>();
        
        for (int i = 0; i < transactions.size() - MIN_TRANSACTIONS_FOR_ALERT + 1; i++) {
            List<Transaction> window = new ArrayList<>();
            
            // Collect transactions within the rapid threshold
            for (int j = i; j < transactions.size(); j++) {
                Transaction current = transactions.get(j);
                
                if (window.isEmpty()) {
                    window.add(current);
                } else {
                    long timeDiff = window.get(0).getDate().getTime() - current.getDate().getTime();
                    if (timeDiff <= RAPID_THRESHOLD) {
                        window.add(current);
                    } else {
                        break;
                    }
                }
            }
            
            // Check if this window constitutes a spending burst
            if (window.size() >= MIN_TRANSACTIONS_FOR_ALERT) {
                SpendingBurst burst = analyzeSpendingBurst(window);
                if (burst != null) {
                    bursts.add(burst);
                    i += window.size() - 1; // Skip analyzed transactions
                }
            }
        }
        
        return bursts;
    }

    private SpendingBurst analyzeSpendingBurst(List<Transaction> burstTransactions) {
        if (burstTransactions.size() < MIN_TRANSACTIONS_FOR_ALERT) return null;
        
        // Filter only expense transactions
        List<Transaction> expenses = burstTransactions.stream()
            .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        
        if (expenses.size() < MIN_TRANSACTIONS_FOR_ALERT) return null;
        
        double totalAmount = expenses.stream().mapToDouble(Transaction::getAmount).sum();
        long startTime = expenses.get(expenses.size() - 1).getDate().getTime();
        long endTime = expenses.get(0).getDate().getTime();
        long duration = endTime - startTime;
        
        // Determine burst severity
        BurstSeverity severity = determineBurstSeverity(expenses.size(), totalAmount, duration);
        
        // Analyze categories involved
        Set<String> categories = new HashSet<>();
        for (Transaction t : expenses) {
            categories.add(t.getCategory());
        }
        
        return new SpendingBurst(
            expenses.size(),
            totalAmount,
            duration,
            severity,
            new ArrayList<>(categories),
            expenses.get(0).getDate(), // Most recent transaction date
            generateBurstDescription(expenses.size(), totalAmount, duration, categories)
        );
    }

    private BurstSeverity determineBurstSeverity(int transactionCount, double totalAmount, long duration) {
        // High severity: Many transactions, high amount, short time
        if (transactionCount >= 5 && totalAmount >= 300 && duration <= TimeUnit.MINUTES.toMillis(10)) {
            return BurstSeverity.HIGH;
        }
        
        // Medium severity: Moderate activity
        if ((transactionCount >= 4 && totalAmount >= 200) || 
            (transactionCount >= 3 && totalAmount >= 400) ||
            (duration <= TimeUnit.MINUTES.toMillis(5))) {
            return BurstSeverity.MEDIUM;
        }
        
        // Low severity: Basic rapid spending
        return BurstSeverity.LOW;
    }

    private boolean isCurrentlyInRapidSpending(List<Transaction> transactions) {
        if (transactions.size() < MIN_TRANSACTIONS_FOR_ALERT) return false;
        
        long now = System.currentTimeMillis();
        List<Transaction> recentTransactions = new ArrayList<>();
        
        // Collect transactions from the last 15 minutes
        for (Transaction t : transactions) {
            if (t.getType() == Transaction.TransactionType.EXPENSE) {
                long timeDiff = now - t.getDate().getTime();
                if (timeDiff <= RAPID_THRESHOLD) {
                    recentTransactions.add(t);
                } else {
                    break; // Transactions are sorted by date
                }
            }
        }
        
        return recentTransactions.size() >= MIN_TRANSACTIONS_FOR_ALERT;
    }

    private List<String> generateCooldownRecommendations(List<SpendingBurst> bursts, boolean currentlyRapid) {
        List<String> recommendations = new ArrayList<>();
        
        if (currentlyRapid) {
            recommendations.add("🛑 Take a 15-minute break before making another purchase");
            recommendations.add("💭 Ask yourself: 'Do I really need this right now?'");
            recommendations.add("📝 Write down what you want to buy and review it later");
        }
        
        if (!bursts.isEmpty()) {
            SpendingBurst latestBurst = bursts.get(0);
            
            if (latestBurst.getSeverity() == BurstSeverity.HIGH) {
                recommendations.add("⚠️ Consider setting a daily spending limit");
                recommendations.add("🔒 Remove payment methods from quick access");
            }
            
            if (latestBurst.getCategories().size() > 2) {
                recommendations.add("🎯 Focus spending on one category at a time");
            }
            
            recommendations.add("📊 Review your recent purchases to identify patterns");
        }
        
        return recommendations;
    }

    private void triggerCooldownNotification(List<Transaction> recentTransactions) {
        int count = (int) recentTransactions.stream()
            .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
            .limit(10) // Check last 10 transactions
            .filter(t -> (System.currentTimeMillis() - t.getDate().getTime()) <= RAPID_THRESHOLD)
            .count();
        
        if (count >= MIN_TRANSACTIONS_FOR_ALERT) {
            notificationManager.showAlert(
                "Rapid Spending Detected",
                String.format("🛑 Multiple entries logged quickly (%d transactions). Review now?", count),
                4001
            );
        }
    }

    private String generateBurstDescription(int count, double amount, long duration, Set<String> categories) {
        long minutes = duration / (1000 * 60);
        String timeDesc = minutes == 0 ? "less than a minute" : minutes + " minute(s)";
        
        return String.format("Spending burst: %d transactions totaling $%.2f in %s across %s",
            count, amount, timeDesc, 
            categories.size() == 1 ? categories.iterator().next() : categories.size() + " categories");
    }

    public static class CooldownResult {
        private final List<SpendingBurst> spendingBursts;
        private final boolean currentlyRapidSpending;
        private final List<String> recommendations;

        public CooldownResult(List<SpendingBurst> spendingBursts, boolean currentlyRapidSpending,
                            List<String> recommendations) {
            this.spendingBursts = spendingBursts;
            this.currentlyRapidSpending = currentlyRapidSpending;
            this.recommendations = recommendations;
        }

        // Getters
        public List<SpendingBurst> getSpendingBursts() { return spendingBursts; }
        public boolean isCurrentlyRapidSpending() { return currentlyRapidSpending; }
        public List<String> getRecommendations() { return recommendations; }
    }

    public static class SpendingBurst {
        private final int transactionCount;
        private final double totalAmount;
        private final long duration;
        private final BurstSeverity severity;
        private final List<String> categories;
        private final Date timestamp;
        private final String description;

        public SpendingBurst(int transactionCount, double totalAmount, long duration,
                           BurstSeverity severity, List<String> categories, Date timestamp,
                           String description) {
            this.transactionCount = transactionCount;
            this.totalAmount = totalAmount;
            this.duration = duration;
            this.severity = severity;
            this.categories = categories;
            this.timestamp = timestamp;
            this.description = description;
        }

        // Getters
        public int getTransactionCount() { return transactionCount; }
        public double getTotalAmount() { return totalAmount; }
        public long getDuration() { return duration; }
        public BurstSeverity getSeverity() { return severity; }
        public List<String> getCategories() { return categories; }
        public Date getTimestamp() { return timestamp; }
        public String getDescription() { return description; }
    }

    public enum BurstSeverity {
        LOW, MEDIUM, HIGH
    }
}