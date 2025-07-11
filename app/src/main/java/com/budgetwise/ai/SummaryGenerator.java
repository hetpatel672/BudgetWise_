package com.budgetwise.ai;

import android.content.Context;
import com.budgetwise.data.models.Budget;
import com.budgetwise.data.models.Transaction;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class SummaryGenerator {
    private final Context context;
    private final AINotificationManager notificationManager;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());

    public SummaryGenerator(Context context) {
        this.context = context;
        this.notificationManager = new AINotificationManager(context);
    }

    public WeeklySummary generateWeeklySummary(List<Transaction> transactions, List<Budget> budgets) {
        long now = System.currentTimeMillis();
        long weekAgo = now - (7L * 24 * 60 * 60 * 1000);
        
        List<Transaction> weeklyTransactions = transactions.stream()
            .filter(t -> t.getDate().getTime() > weekAgo)
            .collect(Collectors.toList());
        
        WeeklySummary summary = analyzeWeeklyData(weeklyTransactions, budgets);
        
        // Trigger weekly summary notification
        triggerWeeklySummaryNotification(summary);
        
        return summary;
    }

    public MonthlySummary generateMonthlySummary(List<Transaction> transactions, List<Budget> budgets) {
        long now = System.currentTimeMillis();
        long monthAgo = now - (30L * 24 * 60 * 60 * 1000);
        
        List<Transaction> monthlyTransactions = transactions.stream()
            .filter(t -> t.getDate().getTime() > monthAgo)
            .collect(Collectors.toList());
        
        return analyzeMonthlyData(monthlyTransactions, budgets);
    }

    private WeeklySummary analyzeWeeklyData(List<Transaction> transactions, List<Budget> budgets) {
        double totalIncome = transactions.stream()
            .filter(t -> t.getType() == Transaction.TransactionType.INCOME)
            .mapToDouble(Transaction::getAmount)
            .sum();
        
        double totalExpenses = transactions.stream()
            .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
            .mapToDouble(Transaction::getAmount)
            .sum();
        
        double netSavings = totalIncome - totalExpenses;
        
        // Analyze spending by category
        Map<String, Double> categorySpending = transactions.stream()
            .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
            .collect(Collectors.groupingBy(
                Transaction::getCategory,
                Collectors.summingDouble(Transaction::getAmount)
            ));
        
        // Find top spending category
        String topCategory = categorySpending.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("None");
        
        double topCategoryAmount = categorySpending.getOrDefault(topCategory, 0.0);
        
        // Analyze budget performance
        List<BudgetPerformance> budgetPerformances = analyzeBudgetPerformance(budgets, transactions);
        
        // Generate insights
        List<String> insights = generateWeeklyInsights(totalIncome, totalExpenses, netSavings, 
                                                     categorySpending, budgetPerformances);
        
        // Calculate comparison with previous week
        WeeklyComparison comparison = calculateWeeklyComparison(transactions);
        
        return new WeeklySummary(
            totalIncome, totalExpenses, netSavings, categorySpending,
            topCategory, topCategoryAmount, budgetPerformances, insights, comparison
        );
    }

    private MonthlySummary analyzeMonthlyData(List<Transaction> transactions, List<Budget> budgets) {
        double totalIncome = transactions.stream()
            .filter(t -> t.getType() == Transaction.TransactionType.INCOME)
            .mapToDouble(Transaction::getAmount)
            .sum();
        
        double totalExpenses = transactions.stream()
            .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
            .mapToDouble(Transaction::getAmount)
            .sum();
        
        double netSavings = totalIncome - totalExpenses;
        double savingsRate = totalIncome > 0 ? (netSavings / totalIncome) * 100 : 0;
        
        // Daily spending analysis
        Map<String, Double> dailySpending = analyzeDailySpending(transactions);
        double avgDailySpending = dailySpending.values().stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
        
        // Category analysis
        Map<String, Double> categorySpending = transactions.stream()
            .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
            .collect(Collectors.groupingBy(
                Transaction::getCategory,
                Collectors.summingDouble(Transaction::getAmount)
            ));
        
        return new MonthlySummary(
            totalIncome, totalExpenses, netSavings, savingsRate,
            categorySpending, dailySpending, avgDailySpending,
            transactions.size(), generateMonthlyInsights(totalIncome, totalExpenses, savingsRate)
        );
    }

    private List<BudgetPerformance> analyzeBudgetPerformance(List<Budget> budgets, List<Transaction> transactions) {
        List<BudgetPerformance> performances = new ArrayList<>();
        
        for (Budget budget : budgets) {
            if (!budget.isActive()) continue;
            
            double spent = transactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
                .filter(t -> t.getCategory().equals(budget.getCategory()))
                .mapToDouble(Transaction::getAmount)
                .sum();
            
            double percentUsed = (spent / budget.getBudgetAmount()) * 100;
            BudgetStatus status = determineBudgetStatus(percentUsed);
            
            performances.add(new BudgetPerformance(
                budget.getCategory(), budget.getBudgetAmount(), spent, percentUsed, status
            ));
        }
        
        return performances;
    }

    private BudgetStatus determineBudgetStatus(double percentUsed) {
        if (percentUsed > 100) return BudgetStatus.OVER_BUDGET;
        if (percentUsed > 90) return BudgetStatus.CRITICAL;
        if (percentUsed > 75) return BudgetStatus.WARNING;
        if (percentUsed > 50) return BudgetStatus.ON_TRACK;
        return BudgetStatus.UNDER_BUDGET;
    }

    private List<String> generateWeeklyInsights(double income, double expenses, double savings,
                                              Map<String, Double> categorySpending,
                                              List<BudgetPerformance> budgetPerformances) {
        List<String> insights = new ArrayList<>();
        
        // Savings insight
        if (savings > 0) {
            insights.add(String.format("💰 Great job! You saved $%.2f this week", savings));
        } else if (savings < 0) {
            insights.add(String.format("⚠️ You spent $%.2f more than you earned this week", Math.abs(savings)));
        }
        
        // Top spending category
        if (!categorySpending.isEmpty()) {
            String topCategory = categorySpending.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Unknown");
            double topAmount = categorySpending.get(topCategory);
            insights.add(String.format("🏆 Top spending: %s ($%.2f)", topCategory, topAmount));
        }
        
        // Budget alerts
        long overBudgetCount = budgetPerformances.stream()
            .mapToLong(bp -> bp.getStatus() == BudgetStatus.OVER_BUDGET ? 1 : 0)
            .sum();
        
        if (overBudgetCount > 0) {
            insights.add(String.format("🚨 %d budget(s) exceeded this week", overBudgetCount));
        }
        
        return insights;
    }

    private List<String> generateMonthlyInsights(double income, double expenses, double savingsRate) {
        List<String> insights = new ArrayList<>();
        
        insights.add(String.format("📊 Monthly savings rate: %.1f%%", savingsRate));
        
        if (savingsRate >= 20) {
            insights.add("🌟 Excellent! You're saving over 20% of your income");
        } else if (savingsRate >= 10) {
            insights.add("👍 Good savings rate! Consider increasing to 20%");
        } else if (savingsRate > 0) {
            insights.add("💡 Try to increase your savings rate to at least 10%");
        } else {
            insights.add("⚠️ Focus on reducing expenses to start saving");
        }
        
        return insights;
    }

    private WeeklyComparison calculateWeeklyComparison(List<Transaction> allTransactions) {
        long now = System.currentTimeMillis();
        long thisWeekStart = now - (7L * 24 * 60 * 60 * 1000);
        long lastWeekStart = thisWeekStart - (7L * 24 * 60 * 60 * 1000);
        
        double thisWeekSpending = allTransactions.stream()
            .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
            .filter(t -> t.getDate().getTime() > thisWeekStart)
            .mapToDouble(Transaction::getAmount)
            .sum();
        
        double lastWeekSpending = allTransactions.stream()
            .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
            .filter(t -> t.getDate().getTime() > lastWeekStart && t.getDate().getTime() <= thisWeekStart)
            .mapToDouble(Transaction::getAmount)
            .sum();
        
        double changePercent = lastWeekSpending > 0 ? 
            ((thisWeekSpending - lastWeekSpending) / lastWeekSpending) * 100 : 0;
        
        return new WeeklyComparison(thisWeekSpending, lastWeekSpending, changePercent);
    }

    private Map<String, Double> analyzeDailySpending(List<Transaction> transactions) {
        return transactions.stream()
            .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
            .collect(Collectors.groupingBy(
                t -> dateFormat.format(t.getDate()),
                Collectors.summingDouble(Transaction::getAmount)
            ));
    }

    private void triggerWeeklySummaryNotification(WeeklySummary summary) {
        String message;
        if (summary.getNetSavings() > 0) {
            message = String.format("📊 Weekly Summary: You saved $%.2f this week! Top spending: %s ($%.2f)",
                summary.getNetSavings(), summary.getTopCategory(), summary.getTopCategoryAmount());
        } else {
            message = String.format("📊 Weekly Summary: Spent $%.2f this week. Top category: %s ($%.2f)",
                summary.getTotalExpenses(), summary.getTopCategory(), summary.getTopCategoryAmount());
        }
        
        notificationManager.showSummary(
            "Weekly Financial Summary",
            message,
            3001
        );
    }

    // Data classes
    public static class WeeklySummary {
        private final double totalIncome;
        private final double totalExpenses;
        private final double netSavings;
        private final Map<String, Double> categorySpending;
        private final String topCategory;
        private final double topCategoryAmount;
        private final List<BudgetPerformance> budgetPerformances;
        private final List<String> insights;
        private final WeeklyComparison comparison;

        public WeeklySummary(double totalIncome, double totalExpenses, double netSavings,
                           Map<String, Double> categorySpending, String topCategory, double topCategoryAmount,
                           List<BudgetPerformance> budgetPerformances, List<String> insights,
                           WeeklyComparison comparison) {
            this.totalIncome = totalIncome;
            this.totalExpenses = totalExpenses;
            this.netSavings = netSavings;
            this.categorySpending = categorySpending;
            this.topCategory = topCategory;
            this.topCategoryAmount = topCategoryAmount;
            this.budgetPerformances = budgetPerformances;
            this.insights = insights;
            this.comparison = comparison;
        }

        // Getters
        public double getTotalIncome() { return totalIncome; }
        public double getTotalExpenses() { return totalExpenses; }
        public double getNetSavings() { return netSavings; }
        public Map<String, Double> getCategorySpending() { return categorySpending; }
        public String getTopCategory() { return topCategory; }
        public double getTopCategoryAmount() { return topCategoryAmount; }
        public List<BudgetPerformance> getBudgetPerformances() { return budgetPerformances; }
        public List<String> getInsights() { return insights; }
        public WeeklyComparison getComparison() { return comparison; }
    }

    public static class MonthlySummary {
        private final double totalIncome;
        private final double totalExpenses;
        private final double netSavings;
        private final double savingsRate;
        private final Map<String, Double> categorySpending;
        private final Map<String, Double> dailySpending;
        private final double avgDailySpending;
        private final int transactionCount;
        private final List<String> insights;

        public MonthlySummary(double totalIncome, double totalExpenses, double netSavings, double savingsRate,
                            Map<String, Double> categorySpending, Map<String, Double> dailySpending,
                            double avgDailySpending, int transactionCount, List<String> insights) {
            this.totalIncome = totalIncome;
            this.totalExpenses = totalExpenses;
            this.netSavings = netSavings;
            this.savingsRate = savingsRate;
            this.categorySpending = categorySpending;
            this.dailySpending = dailySpending;
            this.avgDailySpending = avgDailySpending;
            this.transactionCount = transactionCount;
            this.insights = insights;
        }

        // Getters
        public double getTotalIncome() { return totalIncome; }
        public double getTotalExpenses() { return totalExpenses; }
        public double getNetSavings() { return netSavings; }
        public double getSavingsRate() { return savingsRate; }
        public Map<String, Double> getCategorySpending() { return categorySpending; }
        public Map<String, Double> getDailySpending() { return dailySpending; }
        public double getAvgDailySpending() { return avgDailySpending; }
        public int getTransactionCount() { return transactionCount; }
        public List<String> getInsights() { return insights; }
    }

    public static class BudgetPerformance {
        private final String category;
        private final double budgetAmount;
        private final double spent;
        private final double percentUsed;
        private final BudgetStatus status;

        public BudgetPerformance(String category, double budgetAmount, double spent, 
                               double percentUsed, BudgetStatus status) {
            this.category = category;
            this.budgetAmount = budgetAmount;
            this.spent = spent;
            this.percentUsed = percentUsed;
            this.status = status;
        }

        // Getters
        public String getCategory() { return category; }
        public double getBudgetAmount() { return budgetAmount; }
        public double getSpent() { return spent; }
        public double getPercentUsed() { return percentUsed; }
        public BudgetStatus getStatus() { return status; }
    }

    public static class WeeklyComparison {
        private final double thisWeekSpending;
        private final double lastWeekSpending;
        private final double changePercent;

        public WeeklyComparison(double thisWeekSpending, double lastWeekSpending, double changePercent) {
            this.thisWeekSpending = thisWeekSpending;
            this.lastWeekSpending = lastWeekSpending;
            this.changePercent = changePercent;
        }

        // Getters
        public double getThisWeekSpending() { return thisWeekSpending; }
        public double getLastWeekSpending() { return lastWeekSpending; }
        public double getChangePercent() { return changePercent; }
    }

    public enum BudgetStatus {
        UNDER_BUDGET, ON_TRACK, WARNING, CRITICAL, OVER_BUDGET
    }
}