package com.budgetwise.ai;

import android.content.Context;
import android.util.Log;
import com.budgetwise.data.models.Transaction;
import java.util.*;
import java.util.stream.Collectors;

public class AnomalyDetector {
    private static final String TAG = "AnomalyDetector";
    private static final double ANOMALY_THRESHOLD = 2.5; // Standard deviations
    private static final int MIN_TRANSACTIONS = 10; // Minimum transactions needed for analysis
    
    private final Context context;
    private final AINotificationManager notificationManager;

    public AnomalyDetector(Context context) {
        this.context = context;
        this.notificationManager = new AINotificationManager(context);
    }

    public List<AnomalyResult> detectAnomalies(List<Transaction> transactions) {
        List<AnomalyResult> anomalies = new ArrayList<>();
        
        if (transactions.size() < MIN_TRANSACTIONS) {
            return anomalies; // Not enough data for meaningful analysis
        }

        // Group transactions by category for category-specific analysis
        Map<String, List<Transaction>> byCategory = transactions.stream()
            .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
            .collect(Collectors.groupingBy(Transaction::getCategory));

        for (Map.Entry<String, List<Transaction>> entry : byCategory.entrySet()) {
            String category = entry.getKey();
            List<Transaction> categoryTransactions = entry.getValue();
            
            if (categoryTransactions.size() >= 5) { // Need minimum transactions per category
                anomalies.addAll(detectCategoryAnomalies(category, categoryTransactions));
            }
        }

        // Detect overall spending anomalies
        anomalies.addAll(detectOverallAnomalies(transactions));
        
        // Trigger notifications for detected anomalies
        for (AnomalyResult anomaly : anomalies) {
            if (anomaly.getSeverity() == AnomalySeverity.HIGH) {
                notificationManager.showWarning(
                    "Unusual Transaction Detected",
                    String.format("⚠️ Unusual %s transaction: $%.2f in %s", 
                        anomaly.getType().toString().toLowerCase(),
                        anomaly.getAmount(), 
                        anomaly.getCategory()),
                    anomaly.hashCode()
                );
            }
        }

        return anomalies;
    }

    private List<AnomalyResult> detectCategoryAnomalies(String category, List<Transaction> transactions) {
        List<AnomalyResult> anomalies = new ArrayList<>();
        
        // Calculate statistics for the category
        double[] amounts = transactions.stream()
            .mapToDouble(Transaction::getAmount)
            .toArray();
        
        double mean = Arrays.stream(amounts).average().orElse(0.0);
        double stdDev = calculateStandardDeviation(amounts, mean);
        
        // Find outliers
        for (Transaction transaction : transactions) {
            double zScore = Math.abs((transaction.getAmount() - mean) / stdDev);
            
            if (zScore > ANOMALY_THRESHOLD) {
                AnomalyType type = transaction.getAmount() > mean ? 
                    AnomalyType.UNUSUALLY_HIGH : AnomalyType.UNUSUALLY_LOW;
                
                AnomalySeverity severity = determineSeverity(zScore);
                
                anomalies.add(new AnomalyResult(
                    transaction,
                    type,
                    severity,
                    category,
                    zScore,
                    String.format("Amount $%.2f is %.1f standard deviations from average $%.2f", 
                        transaction.getAmount(), zScore, mean)
                ));
            }
        }
        
        return anomalies;
    }

    private List<AnomalyResult> detectOverallAnomalies(List<Transaction> transactions) {
        List<AnomalyResult> anomalies = new ArrayList<>();
        
        // Detect rapid spending (multiple transactions in short time)
        anomalies.addAll(detectRapidSpending(transactions));
        
        // Detect unusual timing (transactions at unusual hours)
        anomalies.addAll(detectUnusualTiming(transactions));
        
        return anomalies;
    }

    private List<AnomalyResult> detectRapidSpending(List<Transaction> transactions) {
        List<AnomalyResult> anomalies = new ArrayList<>();
        
        // Sort by date
        List<Transaction> sorted = transactions.stream()
            .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
            .sorted((t1, t2) -> t1.getDate().compareTo(t2.getDate()))
            .collect(Collectors.toList());
        
        // Look for clusters of transactions within 1 hour
        for (int i = 0; i < sorted.size() - 2; i++) {
            Transaction t1 = sorted.get(i);
            Transaction t2 = sorted.get(i + 1);
            Transaction t3 = sorted.get(i + 2);
            
            long timeDiff1 = t2.getDate().getTime() - t1.getDate().getTime();
            long timeDiff2 = t3.getDate().getTime() - t2.getDate().getTime();
            
            // If 3 transactions within 1 hour
            if (timeDiff1 <= 3600000 && timeDiff2 <= 3600000) {
                anomalies.add(new AnomalyResult(
                    t3, // Latest transaction
                    AnomalyType.RAPID_SPENDING,
                    AnomalySeverity.MEDIUM,
                    "Multiple Categories",
                    3.0,
                    "Multiple transactions detected within 1 hour"
                ));
            }
        }
        
        return anomalies;
    }

    private List<AnomalyResult> detectUnusualTiming(List<Transaction> transactions) {
        List<AnomalyResult> anomalies = new ArrayList<>();
        
        // Analyze typical spending hours
        Map<Integer, Integer> hourCounts = new HashMap<>();
        for (Transaction transaction : transactions) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(transaction.getDate());
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            hourCounts.put(hour, hourCounts.getOrDefault(hour, 0) + 1);
        }
        
        // Find transactions at unusual hours (very late night/early morning)
        for (Transaction transaction : transactions) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(transaction.getDate());
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            
            // Consider 2 AM - 6 AM as unusual spending hours
            if (hour >= 2 && hour <= 6 && transaction.getAmount() > 50) {
                anomalies.add(new AnomalyResult(
                    transaction,
                    AnomalyType.UNUSUAL_TIMING,
                    AnomalySeverity.LOW,
                    transaction.getCategory(),
                    2.0,
                    String.format("Transaction at unusual hour: %02d:00", hour)
                ));
            }
        }
        
        return anomalies;
    }

    private double calculateStandardDeviation(double[] values, double mean) {
        double sum = 0.0;
        for (double value : values) {
            sum += Math.pow(value - mean, 2);
        }
        return Math.sqrt(sum / values.length);
    }

    private AnomalySeverity determineSeverity(double zScore) {
        if (zScore > 4.0) return AnomalySeverity.CRITICAL;
        if (zScore > 3.0) return AnomalySeverity.HIGH;
        if (zScore > 2.5) return AnomalySeverity.MEDIUM;
        return AnomalySeverity.LOW;
    }

    public static class AnomalyResult {
        private final Transaction transaction;
        private final AnomalyType type;
        private final AnomalySeverity severity;
        private final String category;
        private final double score;
        private final String description;

        public AnomalyResult(Transaction transaction, AnomalyType type, AnomalySeverity severity,
                           String category, double score, String description) {
            this.transaction = transaction;
            this.type = type;
            this.severity = severity;
            this.category = category;
            this.score = score;
            this.description = description;
        }

        // Getters
        public Transaction getTransaction() { return transaction; }
        public AnomalyType getType() { return type; }
        public AnomalySeverity getSeverity() { return severity; }
        public String getCategory() { return category; }
        public double getScore() { return score; }
        public String getDescription() { return description; }
        public double getAmount() { return transaction.getAmount(); }
    }

    public enum AnomalyType {
        UNUSUALLY_HIGH, UNUSUALLY_LOW, RAPID_SPENDING, UNUSUAL_TIMING, DUPLICATE_SUSPECTED
    }

    public enum AnomalySeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}