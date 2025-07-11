package com.budgetwise.ml;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.MutableLiveData;
import com.budgetwise.data.models.Budget;
import com.budgetwise.data.models.Transaction;
import com.budgetwise.data.repository.BudgetRepository;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class LocalIntelligenceService {
    private static final String TAG = "LocalIntelligenceService";
    
    private final Context context;
    private final BudgetRepository repository;
    private final ExecutorService executorService;
    private final MutableLiveData<List<String>> insightsLiveData = new MutableLiveData<>();
    
    // Category mapping for auto-categorization
    private final Map<String, String> categoryKeywords = new HashMap<>();
    
    public LocalIntelligenceService(Context context, BudgetRepository repository) {
        this.context = context;
        this.repository = repository;
        this.executorService = Executors.newSingleThreadExecutor();
        initializeCategoryKeywords();
    }

    private void initializeCategoryKeywords() {
        categoryKeywords.put("grocery", "Food & Dining");
        categoryKeywords.put("restaurant", "Food & Dining");
        categoryKeywords.put("coffee", "Food & Dining");
        categoryKeywords.put("gas", "Transportation");
        categoryKeywords.put("fuel", "Transportation");
        categoryKeywords.put("uber", "Transportation");
        categoryKeywords.put("taxi", "Transportation");
        categoryKeywords.put("netflix", "Entertainment");
        categoryKeywords.put("spotify", "Entertainment");
        categoryKeywords.put("movie", "Entertainment");
        categoryKeywords.put("amazon", "Shopping");
        categoryKeywords.put("walmart", "Shopping");
        categoryKeywords.put("target", "Shopping");
        categoryKeywords.put("pharmacy", "Healthcare");
        categoryKeywords.put("doctor", "Healthcare");
        categoryKeywords.put("hospital", "Healthcare");
        categoryKeywords.put("rent", "Housing");
        categoryKeywords.put("mortgage", "Housing");
        categoryKeywords.put("utilities", "Bills & Utilities");
        categoryKeywords.put("electric", "Bills & Utilities");
        categoryKeywords.put("internet", "Bills & Utilities");
    }

    public String categorizeTransaction(String description) {
        if (description == null || description.trim().isEmpty()) {
            return "Other";
        }
        
        String lowerDescription = description.toLowerCase();
        
        for (Map.Entry<String, String> entry : categoryKeywords.entrySet()) {
            if (lowerDescription.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        return "Other";
    }

    public void analyzeSpendingPatterns() {
        executorService.execute(() -> {
            try {
                List<Transaction> transactions = repository.getCachedTransactions();
                List<String> insights = new ArrayList<>();
                
                // Analyze spending by category
                Map<String, Double> categorySpending = analyzeCategorySpending(transactions);
                insights.addAll(generateCategoryInsights(categorySpending));
                
                // Analyze spending trends
                insights.addAll(analyzeSpendingTrends(transactions));
                
                // Check budget alerts
                insights.addAll(generateBudgetAlerts());
                
                // Detect anomalies
                insights.addAll(detectSpendingAnomalies(transactions));
                
                insightsLiveData.postValue(insights);
                Log.d(TAG, "Generated " + insights.size() + " insights");
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to analyze spending patterns", e);
            }
        });
    }

    private Map<String, Double> analyzeCategorySpending(List<Transaction> transactions) {
        Map<String, Double> categorySpending = new HashMap<>();
        
        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        
        for (Transaction transaction : transactions) {
            if (transaction.getType() == Transaction.TransactionType.EXPENSE && 
                transaction.getDate().getTime() > thirtyDaysAgo) {
                
                String category = transaction.getCategory();
                categorySpending.put(category, 
                    categorySpending.getOrDefault(category, 0.0) + transaction.getAmount());
            }
        }
        
        return categorySpending;
    }

    private List<String> generateCategoryInsights(Map<String, Double> categorySpending) {
        List<String> insights = new ArrayList<>();
        
        if (categorySpending.isEmpty()) {
            return insights;
        }
        
        // Find top spending category
        String topCategory = categorySpending.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("Unknown");
            
        double topAmount = categorySpending.get(topCategory);
        insights.add(String.format("Your highest spending category this month is %s ($%.2f)", 
            topCategory, topAmount));
        
        // Calculate total spending
        double totalSpending = categorySpending.values().stream()
            .mapToDouble(Double::doubleValue)
            .sum();
            
        if (totalSpending > 0) {
            double topPercentage = (topAmount / totalSpending) * 100;
            if (topPercentage > 40) {
                insights.add(String.format("%s represents %.1f%% of your spending. Consider reviewing this category.", 
                    topCategory, topPercentage));
            }
        }
        
        return insights;
    }

    private List<String> analyzeSpendingTrends(List<Transaction> transactions) {
        List<String> insights = new ArrayList<>();
        
        try {
            long now = System.currentTimeMillis();
            long thisWeek = now - (7L * 24 * 60 * 60 * 1000);
            long lastWeek = thisWeek - (7L * 24 * 60 * 60 * 1000);
            
            double thisWeekSpending = transactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
                .filter(t -> t.getDate().getTime() > thisWeek)
                .mapToDouble(Transaction::getAmount)
                .sum();
                
            double lastWeekSpending = transactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
                .filter(t -> t.getDate().getTime() > lastWeek && t.getDate().getTime() <= thisWeek)
                .mapToDouble(Transaction::getAmount)
                .sum();
            
            if (lastWeekSpending > 0) {
                double changePercentage = ((thisWeekSpending - lastWeekSpending) / lastWeekSpending) * 100;
                
                if (Math.abs(changePercentage) > 20) {
                    String trend = changePercentage > 0 ? "increased" : "decreased";
                    insights.add(String.format("Your spending has %s by %.1f%% compared to last week", 
                        trend, Math.abs(changePercentage)));
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to analyze spending trends", e);
        }
        
        return insights;
    }

    private List<String> generateBudgetAlerts() {
        List<String> alerts = new ArrayList<>();
        List<Budget> budgets = repository.getCachedBudgets();
        
        for (Budget budget : budgets) {
            if (!budget.isActive()) continue;
            
            double spentPercentage = budget.getSpentPercentage();
            
            if (budget.isOverBudget()) {
                alerts.add(String.format("⚠️ You've exceeded your %s budget by $%.2f", 
                    budget.getCategory(), budget.getSpentAmount() - budget.getBudgetAmount()));
            } else if (spentPercentage > 80) {
                alerts.add(String.format("⚠️ You've used %.1f%% of your %s budget", 
                    spentPercentage, budget.getCategory()));
            } else if (spentPercentage > 50) {
                alerts.add(String.format("You've used %.1f%% of your %s budget", 
                    spentPercentage, budget.getCategory()));
            }
        }
        
        return alerts;
    }

    private List<String> detectSpendingAnomalies(List<Transaction> transactions) {
        List<String> anomalies = new ArrayList<>();
        
        try {
            // Calculate average daily spending
            long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
            
            List<Transaction> recentExpenses = transactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
                .filter(t -> t.getDate().getTime() > thirtyDaysAgo)
                .collect(Collectors.toList());
            
            if (recentExpenses.size() < 5) return anomalies;
            
            double averageAmount = recentExpenses.stream()
                .mapToDouble(Transaction::getAmount)
                .average()
                .orElse(0.0);
            
            // Find transactions that are significantly higher than average
            double threshold = averageAmount * 3; // 3x average
            
            List<Transaction> largeTransactions = recentExpenses.stream()
                .filter(t -> t.getAmount() > threshold)
                .sorted((t1, t2) -> Double.compare(t2.getAmount(), t1.getAmount()))
                .limit(3)
                .collect(Collectors.toList());
            
            for (Transaction transaction : largeTransactions) {
                anomalies.add(String.format("Large expense detected: $%.2f for %s", 
                    transaction.getAmount(), transaction.getDescription()));
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to detect anomalies", e);
        }
        
        return anomalies;
    }

    public MutableLiveData<List<String>> getInsightsLiveData() {
        return insightsLiveData;
    }

    public List<String> generateBudgetSuggestions(List<Transaction> transactions) {
        List<String> suggestions = new ArrayList<>();
        
        executorService.execute(() -> {
            try {
                Map<String, Double> categorySpending = analyzeCategorySpending(transactions);
                
                for (Map.Entry<String, Double> entry : categorySpending.entrySet()) {
                    String category = entry.getKey();
                    double monthlySpending = entry.getValue();
                    
                    // Check if there's already a budget for this category
                    boolean hasBudget = repository.getCachedBudgets().stream()
                        .anyMatch(b -> b.getCategory().equals(category) && b.isActive());
                    
                    if (!hasBudget && monthlySpending > 50) {
                        double suggestedBudget = monthlySpending * 1.1; // 10% buffer
                        suggestions.add(String.format("Consider setting a budget of $%.2f for %s", 
                            suggestedBudget, category));
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to generate budget suggestions", e);
            }
        });
        
        return suggestions;
    }
}
