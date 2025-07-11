package com.budgetwise.ai;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.MutableLiveData;
import com.budgetwise.data.models.Budget;
import com.budgetwise.data.models.Transaction;
import com.budgetwise.data.repository.BudgetRepository;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EnhancedIntelligenceService {
    private static final String TAG = "EnhancedIntelligenceService";
    
    private final Context context;
    private final BudgetRepository repository;
    private final ExecutorService executorService;
    
    // AI Modules
    private final RecurringDetector recurringDetector;
    private final AnomalyDetector anomalyDetector;
    private final ForecastEngine forecastEngine;
    private final EfficiencyTracker efficiencyTracker;
    private final GoalRecommender goalRecommender;
    private final DuplicateGuard duplicateGuard;
    private final SummaryGenerator summaryGenerator;
    private final CooldownDetector cooldownDetector;
    
    // Live Data for UI updates
    private final MutableLiveData<List<String>> insightsLiveData = new MutableLiveData<>();
    private final MutableLiveData<ForecastEngine.ForecastResult> forecastLiveData = new MutableLiveData<>();
    private final MutableLiveData<SummaryGenerator.WeeklySummary> weeklySummaryLiveData = new MutableLiveData<>();
    
    public EnhancedIntelligenceService(Context context, BudgetRepository repository) {
        this.context = context;
        this.repository = repository;
        this.executorService = Executors.newFixedThreadPool(3);
        
        // Initialize AI modules
        this.recurringDetector = new RecurringDetector(context);
        this.anomalyDetector = new AnomalyDetector(context);
        this.forecastEngine = new ForecastEngine(context);
        this.efficiencyTracker = new EfficiencyTracker(context);
        this.goalRecommender = new GoalRecommender(context);
        this.duplicateGuard = new DuplicateGuard(context);
        this.summaryGenerator = new SummaryGenerator(context);
        this.cooldownDetector = new CooldownDetector(context);
    }

    public void runCompleteAnalysis() {
        executorService.execute(() -> {
            try {
                List<Transaction> transactions = repository.getCachedTransactions();
                List<Budget> budgets = repository.getCachedBudgets();
                
                if (transactions.isEmpty()) {
                    insightsLiveData.postValue(Arrays.asList("Add some transactions to get AI insights!"));
                    return;
                }
                
                List<String> allInsights = new ArrayList<>();
                
                // Run all AI analyses
                analyzeRecurringPatterns(transactions, allInsights);
                analyzeAnomalies(transactions, allInsights);
                generateForecast(transactions, allInsights);
                analyzeBudgetEfficiency(budgets, transactions, allInsights);
                generateGoalRecommendations(transactions, allInsights);
                generateWeeklySummary(transactions, budgets);
                
                // Update live data
                insightsLiveData.postValue(allInsights);
                
                Log.d(TAG, "Complete AI analysis finished with " + allInsights.size() + " insights");
                
            } catch (Exception e) {
                Log.e(TAG, "Error in complete analysis", e);
                insightsLiveData.postValue(Arrays.asList("Analysis temporarily unavailable"));
            }
        });
    }

    private void analyzeRecurringPatterns(List<Transaction> transactions, List<String> insights) {
        try {
            List<RecurringDetector.RecurringPattern> patterns = recurringDetector.detectRecurringTransactions(transactions);
            
            for (RecurringDetector.RecurringPattern pattern : patterns) {
                if (!pattern.isMarkedAsRecurring()) {
                    insights.add(String.format("🔁 Detected recurring pattern: %s every %d days ($%.2f)",
                        pattern.getDescription(), pattern.getIntervalDays(), pattern.getAmount()));
                }
            }
            
            if (patterns.size() > 3) {
                insights.add(String.format("📅 You have %d recurring transaction patterns. Consider automating these!", patterns.size()));
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error analyzing recurring patterns", e);
        }
    }

    private void analyzeAnomalies(List<Transaction> transactions, List<String> insights) {
        try {
            List<AnomalyDetector.AnomalyResult> anomalies = anomalyDetector.detectAnomalies(transactions);
            
            // Add high-priority anomalies to insights
            for (AnomalyDetector.AnomalyResult anomaly : anomalies) {
                if (anomaly.getSeverity() == AnomalyDetector.AnomalySeverity.HIGH ||
                    anomaly.getSeverity() == AnomalyDetector.AnomalySeverity.CRITICAL) {
                    insights.add(String.format("⚠️ %s", anomaly.getDescription()));
                }
            }
            
            if (anomalies.size() > 5) {
                insights.add("🔍 Multiple unusual spending patterns detected. Review your recent transactions.");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error analyzing anomalies", e);
        }
    }

    private void generateForecast(List<Transaction> transactions, List<String> insights) {
        try {
            ForecastEngine.ForecastResult forecast = forecastEngine.generateForecast(transactions);
            forecastLiveData.postValue(forecast);
            
            // Add forecast insights
            if (forecast.getForecastSavings() < 0) {
                insights.add(String.format("📉 Forecast: Projected deficit of $%.2f next month", 
                    Math.abs(forecast.getForecastSavings())));
            } else if (forecast.getForecastSavings() > 1000) {
                insights.add(String.format("💰 Forecast: Projected savings of $%.2f next month!", 
                    forecast.getForecastSavings()));
            }
            
            switch (forecast.getTrend()) {
                case INCREASING:
                    insights.add("📈 Your spending trend is increasing. Consider reviewing your budget.");
                    break;
                case DECREASING:
                    insights.add("📉 Great! Your spending trend is decreasing.");
                    break;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error generating forecast", e);
        }
    }

    private void analyzeBudgetEfficiency(List<Budget> budgets, List<Transaction> transactions, List<String> insights) {
        try {
            List<EfficiencyTracker.EfficiencyResult> results = efficiencyTracker.analyzeBudgetEfficiency(budgets, transactions);
            
            for (EfficiencyTracker.EfficiencyResult result : results) {
                switch (result.getStatus()) {
                    case OVER_BUDGET:
                        insights.add(String.format("🚨 %s budget exceeded by $%.2f", 
                            result.getCategory(), result.getActualSpent() - result.getBudgetAmount()));
                        break;
                    case SPENDING_TOO_FAST:
                        insights.add(String.format("⚡ %s: %.0f%% used with %d days remaining", 
                            result.getCategory(), result.getBudgetUsedPercent(), result.getDaysRemaining()));
                        break;
                    case ON_TRACK:
                        if (result.getEfficiencyScore() > 90) {
                            insights.add(String.format("✅ %s budget is perfectly on track!", result.getCategory()));
                        }
                        break;
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error analyzing budget efficiency", e);
        }
    }

    private void generateGoalRecommendations(List<Transaction> transactions, List<String> insights) {
        try {
            List<GoalRecommender.GoalRecommendation> recommendations = goalRecommender.generateGoalRecommendations(transactions);
            
            // Add high-priority goal recommendations
            for (GoalRecommender.GoalRecommendation rec : recommendations) {
                if (rec.getPriority() == GoalRecommender.GoalPriority.HIGH) {
                    insights.add(String.format("🎯 Goal suggestion: %s", rec.getTitle()));
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error generating goal recommendations", e);
        }
    }

    private void generateWeeklySummary(List<Transaction> transactions, List<Budget> budgets) {
        try {
            SummaryGenerator.WeeklySummary summary = summaryGenerator.generateWeeklySummary(transactions, budgets);
            weeklySummaryLiveData.postValue(summary);
            
        } catch (Exception e) {
            Log.e(TAG, "Error generating weekly summary", e);
        }
    }

    // Public methods for specific AI features
    public DuplicateGuard.DuplicateCheckResult checkForDuplicate(Transaction newTransaction) {
        List<Transaction> existingTransactions = repository.getCachedTransactions();
        return duplicateGuard.checkForDuplicate(newTransaction, existingTransactions);
    }

    public CooldownDetector.CooldownResult analyzeCooldownStatus() {
        List<Transaction> transactions = repository.getCachedTransactions();
        return cooldownDetector.analyzeSpendingPattern(transactions);
    }

    public String categorizeTransaction(String description) {
        // Enhanced categorization with more sophisticated logic
        if (description == null || description.trim().isEmpty()) {
            return "Other";
        }
        
        String lowerDescription = description.toLowerCase();
        
        // Food & Dining
        if (containsAny(lowerDescription, "restaurant", "cafe", "coffee", "starbucks", "mcdonald", 
                       "pizza", "burger", "food", "dining", "lunch", "dinner", "breakfast", "grocery")) {
            return "Food & Dining";
        }
        
        // Transportation
        if (containsAny(lowerDescription, "gas", "fuel", "uber", "lyft", "taxi", "bus", "train", 
                       "metro", "parking", "toll", "car", "vehicle")) {
            return "Transportation";
        }
        
        // Entertainment
        if (containsAny(lowerDescription, "movie", "cinema", "netflix", "spotify", "game", "concert", 
                       "theater", "entertainment", "music", "streaming")) {
            return "Entertainment";
        }
        
        // Shopping
        if (containsAny(lowerDescription, "amazon", "walmart", "target", "store", "shop", "mall", 
                       "clothing", "clothes", "shoes", "electronics")) {
            return "Shopping";
        }
        
        // Healthcare
        if (containsAny(lowerDescription, "doctor", "hospital", "pharmacy", "medical", "health", 
                       "dentist", "clinic", "medicine", "prescription")) {
            return "Healthcare";
        }
        
        // Bills & Utilities
        if (containsAny(lowerDescription, "electric", "electricity", "water", "gas bill", "internet", 
                       "phone", "cable", "utility", "bill", "payment")) {
            return "Bills & Utilities";
        }
        
        // Housing
        if (containsAny(lowerDescription, "rent", "mortgage", "housing", "apartment", "home", 
                       "property", "maintenance", "repair")) {
            return "Housing";
        }
        
        return "Other";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    // Getters for LiveData
    public MutableLiveData<List<String>> getInsightsLiveData() {
        return insightsLiveData;
    }

    public MutableLiveData<ForecastEngine.ForecastResult> getForecastLiveData() {
        return forecastLiveData;
    }

    public MutableLiveData<SummaryGenerator.WeeklySummary> getWeeklySummaryLiveData() {
        return weeklySummaryLiveData;
    }
}