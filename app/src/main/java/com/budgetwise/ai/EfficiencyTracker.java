package com.budgetwise.ai;

import android.content.Context;
import com.budgetwise.data.models.Budget;
import com.budgetwise.data.models.Transaction;
import java.util.*;
import java.util.stream.Collectors;

public class EfficiencyTracker {
    private final Context context;
    private final AINotificationManager notificationManager;

    public EfficiencyTracker(Context context) {
        this.context = context;
        this.notificationManager = new AINotificationManager(context);
    }

    public List<EfficiencyResult> analyzeBudgetEfficiency(List<Budget> budgets, List<Transaction> transactions) {
        List<EfficiencyResult> results = new ArrayList<>();
        
        for (Budget budget : budgets) {
            if (!budget.isActive()) continue;
            
            EfficiencyResult result = analyzeBudget(budget, transactions);
            results.add(result);
            
            // Trigger notifications based on efficiency
            triggerEfficiencyNotifications(result);
        }
        
        return results;
    }

    private EfficiencyResult analyzeBudget(Budget budget, List<Transaction> transactions) {
        long now = System.currentTimeMillis();
        long periodStart = budget.getStartDate();
        long periodEnd = budget.getEndDate();
        
        // Calculate days elapsed and remaining
        long totalDays = (periodEnd - periodStart) / (24 * 60 * 60 * 1000);
        long daysElapsed = (now - periodStart) / (24 * 60 * 60 * 1000);
        long daysRemaining = Math.max(0, (periodEnd - now) / (24 * 60 * 60 * 1000));
        
        // Calculate spending in this period
        double actualSpent = transactions.stream()
            .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
            .filter(t -> t.getCategory().equals(budget.getCategory()))
            .filter(t -> t.getDate().getTime() >= periodStart && t.getDate().getTime() <= now)
            .mapToDouble(Transaction::getAmount)
            .sum();
        
        // Calculate efficiency metrics
        double budgetUsedPercent = (actualSpent / budget.getBudgetAmount()) * 100;
        double timeElapsedPercent = totalDays > 0 ? ((double) daysElapsed / totalDays) * 100 : 0;
        
        // Efficiency score: ideal is spending proportional to time elapsed
        double efficiencyScore = calculateEfficiencyScore(budgetUsedPercent, timeElapsedPercent);
        EfficiencyStatus status = determineStatus(budgetUsedPercent, timeElapsedPercent, daysRemaining);
        
        // Calculate projected spending
        double dailySpendRate = daysElapsed > 0 ? actualSpent / daysElapsed : 0;
        double projectedTotal = dailySpendRate * totalDays;
        
        String recommendation = generateRecommendation(status, budgetUsedPercent, timeElapsedPercent, 
                                                     daysRemaining, projectedTotal, budget.getBudgetAmount());
        
        return new EfficiencyResult(
            budget.getCategory(),
            budget.getBudgetAmount(),
            actualSpent,
            budgetUsedPercent,
            timeElapsedPercent,
            efficiencyScore,
            status,
            daysRemaining,
            projectedTotal,
            recommendation
        );
    }

    private double calculateEfficiencyScore(double budgetUsedPercent, double timeElapsedPercent) {
        if (timeElapsedPercent == 0) return 100; // Perfect at start
        
        double idealRatio = budgetUsedPercent / timeElapsedPercent;
        
        if (idealRatio <= 1.0) {
            // Under or on track
            return Math.min(100, 100 * (2 - idealRatio));
        } else {
            // Over spending
            return Math.max(0, 100 / idealRatio);
        }
    }

    private EfficiencyStatus determineStatus(double budgetUsedPercent, double timeElapsedPercent, long daysRemaining) {
        if (budgetUsedPercent >= 100) {
            return EfficiencyStatus.OVER_BUDGET;
        }
        
        if (timeElapsedPercent > 0) {
            double spendingRate = budgetUsedPercent / timeElapsedPercent;
            
            if (spendingRate > 1.2) {
                return EfficiencyStatus.SPENDING_TOO_FAST;
            } else if (spendingRate > 1.0) {
                return EfficiencyStatus.SLIGHTLY_OVER_PACE;
            } else if (spendingRate > 0.8) {
                return EfficiencyStatus.ON_TRACK;
            } else {
                return EfficiencyStatus.UNDER_SPENDING;
            }
        }
        
        return EfficiencyStatus.ON_TRACK;
    }

    private String generateRecommendation(EfficiencyStatus status, double budgetUsedPercent, 
                                        double timeElapsedPercent, long daysRemaining, 
                                        double projectedTotal, double budgetAmount) {
        switch (status) {
            case OVER_BUDGET:
                return String.format("⚠️ Budget exceeded! Consider reducing spending or adjusting budget by $%.2f", 
                                   projectedTotal - budgetAmount);
            
            case SPENDING_TOO_FAST:
                double dailyBudget = (budgetAmount - (budgetAmount * budgetUsedPercent / 100)) / Math.max(1, daysRemaining);
                return String.format("🚨 Spending too fast! Limit to $%.2f per day for remaining %d days", 
                                   dailyBudget, daysRemaining);
            
            case SLIGHTLY_OVER_PACE:
                return String.format("⚡ Slightly over pace. Consider slowing spending for next %d days", daysRemaining);
            
            case ON_TRACK:
                return "✅ Great! You're spending at a healthy pace";
            
            case UNDER_SPENDING:
                double availableExtra = (budgetAmount * (100 - budgetUsedPercent) / 100) - 
                                      (budgetAmount * (100 - timeElapsedPercent) / 100);
                return String.format("💰 Under-spending! You have $%.2f extra flexibility", availableExtra);
            
            default:
                return "📊 Monitor your spending patterns";
        }
    }

    private void triggerEfficiencyNotifications(EfficiencyResult result) {
        switch (result.getStatus()) {
            case OVER_BUDGET:
                notificationManager.showWarning(
                    "Budget Exceeded",
                    String.format("🚨 %s budget exceeded! $%.2f over limit", 
                        result.getCategory(), result.getActualSpent() - result.getBudgetAmount()),
                    result.hashCode()
                );
                break;
                
            case SPENDING_TOO_FAST:
                notificationManager.showWarning(
                    "Spending Alert",
                    String.format("🚨 %.0f%% of %s budget used. Adjust or slow down!", 
                        result.getBudgetUsedPercent(), result.getCategory()),
                    result.hashCode()
                );
                break;
                
            case SLIGHTLY_OVER_PACE:
                if (result.getBudgetUsedPercent() > 80) {
                    notificationManager.showAlert(
                        "Budget Warning",
                        String.format("⚠️ %.0f%% of %s budget used with %d days remaining", 
                            result.getBudgetUsedPercent(), result.getCategory(), result.getDaysRemaining()),
                        result.hashCode()
                    );
                }
                break;
        }
    }

    public static class EfficiencyResult {
        private final String category;
        private final double budgetAmount;
        private final double actualSpent;
        private final double budgetUsedPercent;
        private final double timeElapsedPercent;
        private final double efficiencyScore;
        private final EfficiencyStatus status;
        private final long daysRemaining;
        private final double projectedTotal;
        private final String recommendation;

        public EfficiencyResult(String category, double budgetAmount, double actualSpent,
                              double budgetUsedPercent, double timeElapsedPercent, double efficiencyScore,
                              EfficiencyStatus status, long daysRemaining, double projectedTotal,
                              String recommendation) {
            this.category = category;
            this.budgetAmount = budgetAmount;
            this.actualSpent = actualSpent;
            this.budgetUsedPercent = budgetUsedPercent;
            this.timeElapsedPercent = timeElapsedPercent;
            this.efficiencyScore = efficiencyScore;
            this.status = status;
            this.daysRemaining = daysRemaining;
            this.projectedTotal = projectedTotal;
            this.recommendation = recommendation;
        }

        // Getters
        public String getCategory() { return category; }
        public double getBudgetAmount() { return budgetAmount; }
        public double getActualSpent() { return actualSpent; }
        public double getBudgetUsedPercent() { return budgetUsedPercent; }
        public double getTimeElapsedPercent() { return timeElapsedPercent; }
        public double getEfficiencyScore() { return efficiencyScore; }
        public EfficiencyStatus getStatus() { return status; }
        public long getDaysRemaining() { return daysRemaining; }
        public double getProjectedTotal() { return projectedTotal; }
        public String getRecommendation() { return recommendation; }
    }

    public enum EfficiencyStatus {
        OVER_BUDGET, SPENDING_TOO_FAST, SLIGHTLY_OVER_PACE, ON_TRACK, UNDER_SPENDING
    }
}