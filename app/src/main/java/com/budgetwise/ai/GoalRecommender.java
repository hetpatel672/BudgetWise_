package com.budgetwise.ai;

import android.content.Context;
import com.budgetwise.data.models.Transaction;
import java.util.*;
import java.util.stream.Collectors;

public class GoalRecommender {
    private final Context context;
    private final AINotificationManager notificationManager;

    public GoalRecommender(Context context) {
        this.context = context;
        this.notificationManager = new AINotificationManager(context);
    }

    public List<GoalRecommendation> generateGoalRecommendations(List<Transaction> transactions) {
        List<GoalRecommendation> recommendations = new ArrayList<>();
        
        // Analyze savings potential
        SavingsAnalysis analysis = analyzeSavingsPattern(transactions);
        
        // Generate different types of goal recommendations
        recommendations.addAll(generateSavingsGoals(analysis));
        recommendations.addAll(generateSpendingReductionGoals(analysis));
        recommendations.addAll(generateCategoryOptimizationGoals(analysis));
        
        // Trigger notifications for promising goals
        triggerGoalNotifications(recommendations, analysis);
        
        return recommendations;
    }

    private SavingsAnalysis analyzeSavingsPattern(List<Transaction> transactions) {
        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        
        List<Transaction> recentTransactions = transactions.stream()
            .filter(t -> t.getDate().getTime() > thirtyDaysAgo)
            .collect(Collectors.toList());
        
        double monthlyIncome = recentTransactions.stream()
            .filter(t -> t.getType() == Transaction.TransactionType.INCOME)
            .mapToDouble(Transaction::getAmount)
            .sum();
        
        double monthlyExpenses = recentTransactions.stream()
            .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
            .mapToDouble(Transaction::getAmount)
            .sum();
        
        double currentSavings = monthlyIncome - monthlyExpenses;
        double savingsRate = monthlyIncome > 0 ? (currentSavings / monthlyIncome) * 100 : 0;
        
        // Analyze spending by category
        Map<String, Double> categorySpending = recentTransactions.stream()
            .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
            .collect(Collectors.groupingBy(
                Transaction::getCategory,
                Collectors.summingDouble(Transaction::getAmount)
            ));
        
        return new SavingsAnalysis(monthlyIncome, monthlyExpenses, currentSavings, 
                                 savingsRate, categorySpending);
    }

    private List<GoalRecommendation> generateSavingsGoals(SavingsAnalysis analysis) {
        List<GoalRecommendation> goals = new ArrayList<>();
        
        // Emergency fund goal
        if (analysis.getCurrentSavings() > 0) {
            double emergencyFundTarget = analysis.getMonthlyExpenses() * 6; // 6 months expenses
            double monthsToReach = emergencyFundTarget / analysis.getCurrentSavings();
            
            goals.add(new GoalRecommendation(
                GoalType.EMERGENCY_FUND,
                "Emergency Fund",
                emergencyFundTarget,
                (int) Math.ceil(monthsToReach),
                String.format("Build a 6-month emergency fund of $%.2f. At current savings rate of $%.2f/month, you'll reach this in %.0f months.",
                    emergencyFundTarget, analysis.getCurrentSavings(), monthsToReach),
                GoalPriority.HIGH
            ));
        }
        
        // Savings rate improvement goal
        if (analysis.getSavingsRate() < 20) {
            double targetSavingsRate = Math.min(analysis.getSavingsRate() + 5, 20);
            double targetSavingsAmount = (analysis.getMonthlyIncome() * targetSavingsRate) / 100;
            double additionalSavingsNeeded = targetSavingsAmount - analysis.getCurrentSavings();
            
            goals.add(new GoalRecommendation(
                GoalType.SAVINGS_RATE,
                String.format("Improve Savings Rate to %.0f%%", targetSavingsRate),
                additionalSavingsNeeded,
                3, // 3 months to improve
                String.format("Increase your savings rate from %.1f%% to %.0f%% by saving an additional $%.2f per month.",
                    analysis.getSavingsRate(), targetSavingsRate, additionalSavingsNeeded),
                GoalPriority.MEDIUM
            ));
        }
        
        return goals;
    }

    private List<GoalRecommendation> generateSpendingReductionGoals(SavingsAnalysis analysis) {
        List<GoalRecommendation> goals = new ArrayList<>();
        
        // Find highest spending categories for reduction goals
        List<Map.Entry<String, Double>> sortedCategories = analysis.getCategorySpending().entrySet()
            .stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(3)
            .collect(Collectors.toList());
        
        for (Map.Entry<String, Double> entry : sortedCategories) {
            String category = entry.getKey();
            double currentSpending = entry.getValue();
            
            if (currentSpending > 200) { // Only suggest for significant spending
                double reductionTarget = currentSpending * 0.15; // 15% reduction
                double newTarget = currentSpending - reductionTarget;
                
                goals.add(new GoalRecommendation(
                    GoalType.SPENDING_REDUCTION,
                    String.format("Reduce %s Spending", category),
                    reductionTarget,
                    2, // 2 months to achieve
                    String.format("Reduce %s spending from $%.2f to $%.2f (15%% reduction) to save $%.2f monthly.",
                        category, currentSpending, newTarget, reductionTarget),
                    GoalPriority.MEDIUM
                ));
            }
        }
        
        return goals;
    }

    private List<GoalRecommendation> generateCategoryOptimizationGoals(SavingsAnalysis analysis) {
        List<GoalRecommendation> goals = new ArrayList<>();
        
        // Suggest optimization for categories with high variance or potential
        for (Map.Entry<String, Double> entry : analysis.getCategorySpending().entrySet()) {
            String category = entry.getKey();
            double spending = entry.getValue();
            
            // Suggest specific optimizations based on category
            switch (category.toLowerCase()) {
                case "food & dining":
                    if (spending > 400) {
                        goals.add(new GoalRecommendation(
                            GoalType.CATEGORY_OPTIMIZATION,
                            "Optimize Food Spending",
                            spending * 0.2,
                            1,
                            "Try meal planning and cooking at home more often to reduce dining expenses by 20%.",
                            GoalPriority.LOW
                        ));
                    }
                    break;
                    
                case "transportation":
                    if (spending > 300) {
                        goals.add(new GoalRecommendation(
                            GoalType.CATEGORY_OPTIMIZATION,
                            "Optimize Transportation",
                            spending * 0.15,
                            2,
                            "Consider carpooling, public transport, or combining trips to reduce transportation costs.",
                            GoalPriority.LOW
                        ));
                    }
                    break;
                    
                case "entertainment":
                    if (spending > 200) {
                        goals.add(new GoalRecommendation(
                            GoalType.CATEGORY_OPTIMIZATION,
                            "Optimize Entertainment",
                            spending * 0.25,
                            1,
                            "Look for free or low-cost entertainment alternatives to reduce spending by 25%.",
                            GoalPriority.LOW
                        ));
                    }
                    break;
            }
        }
        
        return goals;
    }

    private void triggerGoalNotifications(List<GoalRecommendation> recommendations, SavingsAnalysis analysis) {
        // Notify about positive savings trends
        if (analysis.getCurrentSavings() > 0 && analysis.getSavingsRate() > 10) {
            notificationManager.showAlert(
                "Savings Goal Opportunity",
                String.format("🎯 Saved $%.2f this month! Consider setting a savings goal?", 
                    analysis.getCurrentSavings()),
                2001
            );
        }
        
        // Notify about high-priority recommendations
        for (GoalRecommendation rec : recommendations) {
            if (rec.getPriority() == GoalPriority.HIGH) {
                notificationManager.showAlert(
                    "Goal Recommendation",
                    String.format("💡 %s - %s", rec.getTitle(), rec.getDescription()),
                    rec.hashCode()
                );
            }
        }
    }

    private static class SavingsAnalysis {
        private final double monthlyIncome;
        private final double monthlyExpenses;
        private final double currentSavings;
        private final double savingsRate;
        private final Map<String, Double> categorySpending;

        public SavingsAnalysis(double monthlyIncome, double monthlyExpenses, double currentSavings,
                             double savingsRate, Map<String, Double> categorySpending) {
            this.monthlyIncome = monthlyIncome;
            this.monthlyExpenses = monthlyExpenses;
            this.currentSavings = currentSavings;
            this.savingsRate = savingsRate;
            this.categorySpending = categorySpending;
        }

        // Getters
        public double getMonthlyIncome() { return monthlyIncome; }
        public double getMonthlyExpenses() { return monthlyExpenses; }
        public double getCurrentSavings() { return currentSavings; }
        public double getSavingsRate() { return savingsRate; }
        public Map<String, Double> getCategorySpending() { return categorySpending; }
    }

    public static class GoalRecommendation {
        private final GoalType type;
        private final String title;
        private final double targetAmount;
        private final int timeframeMonths;
        private final String description;
        private final GoalPriority priority;

        public GoalRecommendation(GoalType type, String title, double targetAmount,
                                int timeframeMonths, String description, GoalPriority priority) {
            this.type = type;
            this.title = title;
            this.targetAmount = targetAmount;
            this.timeframeMonths = timeframeMonths;
            this.description = description;
            this.priority = priority;
        }

        // Getters
        public GoalType getType() { return type; }
        public String getTitle() { return title; }
        public double getTargetAmount() { return targetAmount; }
        public int getTimeframeMonths() { return timeframeMonths; }
        public String getDescription() { return description; }
        public GoalPriority getPriority() { return priority; }
    }

    public enum GoalType {
        EMERGENCY_FUND, SAVINGS_RATE, SPENDING_REDUCTION, CATEGORY_OPTIMIZATION
    }

    public enum GoalPriority {
        LOW, MEDIUM, HIGH
    }
}