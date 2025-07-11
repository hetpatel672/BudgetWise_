package com.budgetwise.ai;

import android.content.Context;
import com.budgetwise.data.models.Transaction;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class DuplicateGuard {
    private static final long DUPLICATE_TIME_WINDOW = TimeUnit.MINUTES.toMillis(30); // 30 minutes
    private static final double AMOUNT_TOLERANCE = 0.01; // $0.01 tolerance
    
    private final Context context;
    private final AINotificationManager notificationManager;

    public DuplicateGuard(Context context) {
        this.context = context;
        this.notificationManager = new AINotificationManager(context);
    }

    public DuplicateCheckResult checkForDuplicate(Transaction newTransaction, List<Transaction> existingTransactions) {
        List<Transaction> potentialDuplicates = findPotentialDuplicates(newTransaction, existingTransactions);
        
        if (!potentialDuplicates.isEmpty()) {
            DuplicateConfidence confidence = calculateConfidence(newTransaction, potentialDuplicates.get(0));
            
            // Trigger notification for high confidence duplicates
            if (confidence == DuplicateConfidence.HIGH) {
                notificationManager.showAlert(
                    "Duplicate Transaction Detected",
                    String.format("❗ This looks like a duplicate: $%.2f for %s. Proceed anyway?", 
                        newTransaction.getAmount(), newTransaction.getDescription()),
                    newTransaction.hashCode()
                );
            }
            
            return new DuplicateCheckResult(true, potentialDuplicates, confidence, 
                generateDuplicateMessage(newTransaction, potentialDuplicates.get(0), confidence));
        }
        
        return new DuplicateCheckResult(false, new ArrayList<>(), DuplicateConfidence.NONE, "");
    }

    private List<Transaction> findPotentialDuplicates(Transaction newTransaction, List<Transaction> existingTransactions) {
        List<Transaction> potentialDuplicates = new ArrayList<>();
        
        for (Transaction existing : existingTransactions) {
            if (isPotentialDuplicate(newTransaction, existing)) {
                potentialDuplicates.add(existing);
            }
        }
        
        // Sort by similarity score (most similar first)
        potentialDuplicates.sort((t1, t2) -> {
            double score1 = calculateSimilarityScore(newTransaction, t1);
            double score2 = calculateSimilarityScore(newTransaction, t2);
            return Double.compare(score2, score1);
        });
        
        return potentialDuplicates;
    }

    private boolean isPotentialDuplicate(Transaction newTransaction, Transaction existing) {
        // Check time window
        long timeDiff = Math.abs(newTransaction.getDate().getTime() - existing.getDate().getTime());
        if (timeDiff > DUPLICATE_TIME_WINDOW) {
            return false;
        }
        
        // Check amount similarity
        double amountDiff = Math.abs(newTransaction.getAmount() - existing.getAmount());
        if (amountDiff > AMOUNT_TOLERANCE) {
            return false;
        }
        
        // Check transaction type
        if (newTransaction.getType() != existing.getType()) {
            return false;
        }
        
        // Check description similarity
        double descriptionSimilarity = calculateDescriptionSimilarity(
            newTransaction.getDescription(), existing.getDescription());
        
        return descriptionSimilarity > 0.7; // 70% similarity threshold
    }

    private double calculateSimilarityScore(Transaction t1, Transaction t2) {
        double score = 0.0;
        
        // Amount similarity (40% weight)
        double amountDiff = Math.abs(t1.getAmount() - t2.getAmount());
        double amountSimilarity = Math.max(0, 1 - (amountDiff / Math.max(t1.getAmount(), t2.getAmount())));
        score += amountSimilarity * 0.4;
        
        // Description similarity (40% weight)
        double descriptionSimilarity = calculateDescriptionSimilarity(t1.getDescription(), t2.getDescription());
        score += descriptionSimilarity * 0.4;
        
        // Time proximity (20% weight)
        long timeDiff = Math.abs(t1.getDate().getTime() - t2.getDate().getTime());
        double timeSimilarity = Math.max(0, 1 - (double) timeDiff / DUPLICATE_TIME_WINDOW);
        score += timeSimilarity * 0.2;
        
        return score;
    }

    private double calculateDescriptionSimilarity(String desc1, String desc2) {
        if (desc1 == null || desc2 == null) return 0.0;
        
        String normalized1 = normalizeDescription(desc1);
        String normalized2 = normalizeDescription(desc2);
        
        // Use Levenshtein distance for similarity
        int distance = levenshteinDistance(normalized1, normalized2);
        int maxLength = Math.max(normalized1.length(), normalized2.length());
        
        return maxLength > 0 ? 1.0 - (double) distance / maxLength : 1.0;
    }

    private String normalizeDescription(String description) {
        return description.toLowerCase()
            .replaceAll("[^a-zA-Z0-9\\s]", "") // Remove special characters
            .replaceAll("\\s+", " ") // Normalize whitespace
            .trim();
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }

    private DuplicateConfidence calculateConfidence(Transaction newTransaction, Transaction existing) {
        double similarityScore = calculateSimilarityScore(newTransaction, existing);
        
        if (similarityScore >= 0.95) return DuplicateConfidence.VERY_HIGH;
        if (similarityScore >= 0.85) return DuplicateConfidence.HIGH;
        if (similarityScore >= 0.75) return DuplicateConfidence.MEDIUM;
        if (similarityScore >= 0.65) return DuplicateConfidence.LOW;
        
        return DuplicateConfidence.NONE;
    }

    private String generateDuplicateMessage(Transaction newTransaction, Transaction existing, DuplicateConfidence confidence) {
        long timeDiff = Math.abs(newTransaction.getDate().getTime() - existing.getDate().getTime());
        long minutesDiff = timeDiff / (1000 * 60);
        
        String confidenceText = confidence.toString().toLowerCase().replace("_", " ");
        
        return String.format("Potential duplicate detected (%s confidence): Similar transaction of $%.2f for '%s' was recorded %d minutes ago.",
            confidenceText, existing.getAmount(), existing.getDescription(), minutesDiff);
    }

    public static class DuplicateCheckResult {
        private final boolean isDuplicate;
        private final List<Transaction> potentialDuplicates;
        private final DuplicateConfidence confidence;
        private final String message;

        public DuplicateCheckResult(boolean isDuplicate, List<Transaction> potentialDuplicates,
                                  DuplicateConfidence confidence, String message) {
            this.isDuplicate = isDuplicate;
            this.potentialDuplicates = potentialDuplicates;
            this.confidence = confidence;
            this.message = message;
        }

        // Getters
        public boolean isDuplicate() { return isDuplicate; }
        public List<Transaction> getPotentialDuplicates() { return potentialDuplicates; }
        public DuplicateConfidence getConfidence() { return confidence; }
        public String getMessage() { return message; }
    }

    public enum DuplicateConfidence {
        NONE, LOW, MEDIUM, HIGH, VERY_HIGH
    }
}