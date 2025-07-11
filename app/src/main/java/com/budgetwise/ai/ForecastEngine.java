package com.budgetwise.ai;

import android.content.Context;
import android.util.Log;
import com.budgetwise.data.models.Transaction;
import java.util.*;
import java.util.stream.Collectors;

public class ForecastEngine {
    private static final String TAG = "ForecastEngine";
    private static final int FORECAST_DAYS = 30; // Forecast for next 30 days
    private static final int ANALYSIS_DAYS = 90; // Analyze last 90 days for patterns
    
    private final Context context;
    private final AINotificationManager notificationManager;

    public ForecastEngine(Context context) {
        this.context = context;
        this.notificationManager = new AINotificationManager(context);
    }

    public ForecastResult generateForecast(List<Transaction> transactions) {
        long now = System.currentTimeMillis();
        long analysisStart = now - (ANALYSIS_DAYS * 24 * 60 * 60 * 1000L);
        
        // Filter recent transactions for analysis
        List<Transaction> recentTransactions = transactions.stream()
            .filter(t -> t.getDate().getTime() >= analysisStart)
            .collect(Collectors.toList());

        if (recentTransactions.size() < 10) {
            return new ForecastResult(0, 0, 0, ForecastTrend.STABLE, "Insufficient data for forecast");
        }

        // Calculate spending patterns
        double avgDailySpending = calculateAverageDaily(recentTransactions, Transaction.TransactionType.EXPENSE);
        double avgDailyIncome = calculateAverageDaily(recentTransactions, Transaction.TransactionType.INCOME);
        
        // Apply trend analysis
        ForecastTrend trend = analyzeTrend(recentTransactions);
        double trendMultiplier = getTrendMultiplier(trend);
        
        // Generate forecasts
        double forecastSpending = avgDailySpending * FORECAST_DAYS * trendMultiplier;
        double forecastIncome = avgDailyIncome * FORECAST_DAYS;
        double forecastSavings = forecastIncome - forecastSpending;
        
        // Generate insights and notifications
        String insights = generateInsights(forecastSpending, forecastIncome, forecastSavings, trend);
        triggerForecastNotifications(forecastSavings, trend);
        
        return new ForecastResult(forecastSpending, forecastIncome, forecastSavings, trend, insights);
    }

    private double calculateAverageDaily(List<Transaction> transactions, Transaction.TransactionType type) {
        List<Transaction> filtered = transactions.stream()
            .filter(t -> t.getType() == type)
            .collect(Collectors.toList());
        
        if (filtered.isEmpty()) return 0;
        
        double total = filtered.stream().mapToDouble(Transaction::getAmount).sum();
        return total / ANALYSIS_DAYS;
    }

    private ForecastTrend analyzeTrend(List<Transaction> transactions) {
        // Split into two periods and compare
        long now = System.currentTimeMillis();
        long midPoint = now - (ANALYSIS_DAYS * 24 * 60 * 60 * 1000L / 2);
        
        List<Transaction> firstHalf = transactions.stream()
            .filter(t -> t.getDate().getTime() < midPoint)
            .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
            .collect(Collectors.toList());
        
        List<Transaction> secondHalf = transactions.stream()
            .filter(t -> t.getDate().getTime() >= midPoint)
            .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
            .collect(Collectors.toList());
        
        if (firstHalf.isEmpty() || secondHalf.isEmpty()) {
            return ForecastTrend.STABLE;
        }
        
        double firstHalfAvg = firstHalf.stream().mapToDouble(Transaction::getAmount).sum() / (ANALYSIS_DAYS / 2);
        double secondHalfAvg = secondHalf.stream().mapToDouble(Transaction::getAmount).sum() / (ANALYSIS_DAYS / 2);
        
        double changePercent = ((secondHalfAvg - firstHalfAvg) / firstHalfAvg) * 100;
        
        if (changePercent > 15) return ForecastTrend.INCREASING;
        if (changePercent < -15) return ForecastTrend.DECREASING;
        return ForecastTrend.STABLE;
    }

    private double getTrendMultiplier(ForecastTrend trend) {
        switch (trend) {
            case INCREASING: return 1.15; // 15% increase
            case DECREASING: return 0.85; // 15% decrease
            case STABLE:
            default: return 1.0;
        }
    }

    private String generateInsights(double spending, double income, double savings, ForecastTrend trend) {
        StringBuilder insights = new StringBuilder();
        
        insights.append(String.format("Next 30 days forecast:\n"));
        insights.append(String.format("• Expected spending: $%.2f\n", spending));
        insights.append(String.format("• Expected income: $%.2f\n", income));
        insights.append(String.format("• Projected savings: $%.2f\n", savings));
        
        if (savings < 0) {
            insights.append("⚠️ Warning: Projected deficit. Consider reducing expenses.\n");
        } else if (savings > income * 0.2) {
            insights.append("✅ Great! You're on track to save over 20% of income.\n");
        }
        
        switch (trend) {
            case INCREASING:
                insights.append("📈 Spending trend is increasing. Monitor expenses closely.\n");
                break;
            case DECREASING:
                insights.append("📉 Spending trend is decreasing. Good financial discipline!\n");
                break;
            case STABLE:
                insights.append("➡️ Spending pattern is stable.\n");
                break;
        }
        
        return insights.toString();
    }

    private void triggerForecastNotifications(double forecastSavings, ForecastTrend trend) {
        // Notify if savings trend is concerning
        if (forecastSavings < 0) {
            notificationManager.showWarning(
                "Savings Alert",
                "📉 Your savings trend is decreasing. Consider reviewing your spending!",
                1001
            );
        }
        
        // Notify about spending trends
        if (trend == ForecastTrend.INCREASING) {
            notificationManager.showAlert(
                "Spending Trend Alert",
                "📈 Your spending has been increasing. Time to review your budget!",
                1002
            );
        }
    }

    public Map<String, Double> getCategoryForecasts(List<Transaction> transactions) {
        Map<String, Double> forecasts = new HashMap<>();
        
        // Group by category and calculate forecasts
        Map<String, List<Transaction>> byCategory = transactions.stream()
            .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
            .collect(Collectors.groupingBy(Transaction::getCategory));
        
        for (Map.Entry<String, List<Transaction>> entry : byCategory.entrySet()) {
            String category = entry.getKey();
            List<Transaction> categoryTransactions = entry.getValue();
            
            double avgDaily = calculateAverageDaily(categoryTransactions, Transaction.TransactionType.EXPENSE);
            double forecast = avgDaily * FORECAST_DAYS;
            
            forecasts.put(category, forecast);
        }
        
        return forecasts;
    }

    public static class ForecastResult {
        private final double forecastSpending;
        private final double forecastIncome;
        private final double forecastSavings;
        private final ForecastTrend trend;
        private final String insights;

        public ForecastResult(double forecastSpending, double forecastIncome, double forecastSavings,
                            ForecastTrend trend, String insights) {
            this.forecastSpending = forecastSpending;
            this.forecastIncome = forecastIncome;
            this.forecastSavings = forecastSavings;
            this.trend = trend;
            this.insights = insights;
        }

        // Getters
        public double getForecastSpending() { return forecastSpending; }
        public double getForecastIncome() { return forecastIncome; }
        public double getForecastSavings() { return forecastSavings; }
        public ForecastTrend getTrend() { return trend; }
        public String getInsights() { return insights; }
    }

    public enum ForecastTrend {
        INCREASING, DECREASING, STABLE
    }
}