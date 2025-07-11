package com.budgetwise.ui.analytics;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;
import com.budgetwise.data.models.Transaction;
import com.budgetwise.data.repository.BudgetRepository;
import com.budgetwise.ai.EnhancedIntelligenceService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AnalyticsViewModel extends ViewModel {
    private final BudgetRepository repository;
    private final EnhancedIntelligenceService intelligenceService;
    
    private final MediatorLiveData<Map<String, Double>> categorySpending = new MediatorLiveData<>();
    private final MediatorLiveData<Map<String, Double>> monthlySpending = new MediatorLiveData<>();
    private final MediatorLiveData<SpendingTrend> spendingTrend = new MediatorLiveData<>();
    private final MediatorLiveData<String> topCategory = new MediatorLiveData<>();
    private final MediatorLiveData<Double> averageDaily = new MediatorLiveData<>();
    private final MediatorLiveData<Double> savingsRate = new MediatorLiveData<>();

    public enum SpendingTrend {
        INCREASING, DECREASING, STABLE
    }

    public AnalyticsViewModel(BudgetRepository repository, EnhancedIntelligenceService intelligenceService) {
        this.repository = repository;
        this.intelligenceService = intelligenceService;
        setupMediators();
    }

    private void setupMediators() {
        categorySpending.addSource(repository.getTransactions(), this::calculateCategorySpending);
        monthlySpending.addSource(repository.getTransactions(), this::calculateMonthlySpending);
        spendingTrend.addSource(repository.getTransactions(), this::calculateSpendingTrend);
        topCategory.addSource(repository.getTransactions(), this::calculateTopCategory);
        averageDaily.addSource(repository.getTransactions(), this::calculateAverageDaily);
        savingsRate.addSource(repository.getTransactions(), this::calculateSavingsRate);
    }

    private void calculateCategorySpending(List<Transaction> transactions) {
        Map<String, Double> categoryData = new HashMap<>();
        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        
        for (Transaction transaction : transactions) {
            if (transaction.getType() == Transaction.TransactionType.EXPENSE && 
                transaction.getDate().getTime() > thirtyDaysAgo) {
                
                String category = transaction.getCategory();
                categoryData.put(category, 
                    categoryData.getOrDefault(category, 0.0) + transaction.getAmount());
            }
        }
        
        categorySpending.setValue(categoryData);
    }

    private void calculateMonthlySpending(List<Transaction> transactions) {
        Map<String, Double> monthlyData = new HashMap<>();
        
        // Calculate spending for last 6 months
        long now = System.currentTimeMillis();
        for (int i = 0; i < 6; i++) {
            long monthStart = now - ((long) (i + 1) * 30 * 24 * 60 * 60 * 1000);
            long monthEnd = now - ((long) i * 30 * 24 * 60 * 60 * 1000);
            
            double monthSpending = transactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
                .filter(t -> t.getDate().getTime() >= monthStart && t.getDate().getTime() < monthEnd)
                .mapToDouble(Transaction::getAmount)
                .sum();
            
            String monthLabel = "Month " + (6 - i);
            monthlyData.put(monthLabel, monthSpending);
        }
        
        monthlySpending.setValue(monthlyData);
    }

    private void calculateSpendingTrend(List<Transaction> transactions) {
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
        
        if (lastWeekSpending == 0) {
            spendingTrend.setValue(SpendingTrend.STABLE);
            return;
        }
        
        double changePercentage = ((thisWeekSpending - lastWeekSpending) / lastWeekSpending) * 100;
        
        if (changePercentage > 10) {
            spendingTrend.setValue(SpendingTrend.INCREASING);
        } else if (changePercentage < -10) {
            spendingTrend.setValue(SpendingTrend.DECREASING);
        } else {
            spendingTrend.setValue(SpendingTrend.STABLE);
        }
    }

    private void calculateTopCategory(List<Transaction> transactions) {
        Map<String, Double> categoryTotals = new HashMap<>();
        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        
        for (Transaction transaction : transactions) {
            if (transaction.getType() == Transaction.TransactionType.EXPENSE && 
                transaction.getDate().getTime() > thirtyDaysAgo) {
                
                String category = transaction.getCategory();
                categoryTotals.put(category, 
                    categoryTotals.getOrDefault(category, 0.0) + transaction.getAmount());
            }
        }
        
        String top = categoryTotals.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("No data");
            
        topCategory.setValue(top);
    }

    private void calculateAverageDaily(List<Transaction> transactions) {
        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        
        double totalSpending = transactions.stream()
            .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
            .filter(t -> t.getDate().getTime() > thirtyDaysAgo)
            .mapToDouble(Transaction::getAmount)
            .sum();
        
        averageDaily.setValue(totalSpending / 30.0);
    }

    private void calculateSavingsRate(List<Transaction> transactions) {
        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        
        double totalIncome = transactions.stream()
            .filter(t -> t.getType() == Transaction.TransactionType.INCOME)
            .filter(t -> t.getDate().getTime() > thirtyDaysAgo)
            .mapToDouble(Transaction::getAmount)
            .sum();
            
        double totalExpenses = transactions.stream()
            .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
            .filter(t -> t.getDate().getTime() > thirtyDaysAgo)
            .mapToDouble(Transaction::getAmount)
            .sum();
        
        if (totalIncome > 0) {
            double savings = totalIncome - totalExpenses;
            double rate = (savings / totalIncome) * 100;
            savingsRate.setValue(Math.max(0, rate));
        } else {
            savingsRate.setValue(0.0);
        }
    }

    public LiveData<Map<String, Double>> getCategorySpending() {
        return categorySpending;
    }

    public LiveData<Map<String, Double>> getMonthlySpending() {
        return monthlySpending;
    }

    public LiveData<SpendingTrend> getSpendingTrend() {
        return spendingTrend;
    }

    public LiveData<String> getTopCategory() {
        return topCategory;
    }

    public LiveData<Double> getAverageDaily() {
        return averageDaily;
    }

    public LiveData<Double> getSavingsRate() {
        return savingsRate;
    }
}
