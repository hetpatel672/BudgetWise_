package com.budgetwise.ui.dashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;
import com.budgetwise.data.models.Budget;
import com.budgetwise.data.models.Transaction;
import com.budgetwise.data.repository.BudgetRepository;
import com.budgetwise.ai.EnhancedIntelligenceService;
import java.util.List;
import java.util.stream.Collectors;

public class DashboardViewModel extends ViewModel {
    private final BudgetRepository repository;
    private final EnhancedIntelligenceService intelligenceService;
    
    private final MediatorLiveData<Double> totalBalance = new MediatorLiveData<>();
    private final MediatorLiveData<Double> monthlySpending = new MediatorLiveData<>();
    private final MediatorLiveData<List<Transaction>> recentTransactions = new MediatorLiveData<>();

    public DashboardViewModel(BudgetRepository repository, EnhancedIntelligenceService intelligenceService) {
        this.repository = repository;
        this.intelligenceService = intelligenceService;
        
        setupMediators();
        
        // Trigger analysis
        intelligenceService.runCompleteAnalysis();
    }

    private void setupMediators() {
        totalBalance.addSource(repository.getTransactions(), transactions -> {
            double balance = calculateTotalBalance(transactions);
            totalBalance.setValue(balance);
        });

        monthlySpending.addSource(repository.getTransactions(), transactions -> {
            double spending = calculateMonthlySpending(transactions);
            monthlySpending.setValue(spending);
        });

        recentTransactions.addSource(repository.getTransactions(), transactions -> {
            List<Transaction> recent = getRecentTransactions(transactions, 5);
            recentTransactions.setValue(recent);
        });
    }

    private double calculateTotalBalance(List<Transaction> transactions) {
        double balance = 0.0;
        for (Transaction transaction : transactions) {
            switch (transaction.getType()) {
                case INCOME:
                    balance += transaction.getAmount();
                    break;
                case EXPENSE:
                    balance -= transaction.getAmount();
                    break;
                case TRANSFER:
                    // Transfers don't affect total balance
                    break;
            }
        }
        return balance;
    }

    private double calculateMonthlySpending(List<Transaction> transactions) {
        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        
        return transactions.stream()
            .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
            .filter(t -> t.getDate().getTime() > thirtyDaysAgo)
            .mapToDouble(Transaction::getAmount)
            .sum();
    }

    private List<Transaction> getRecentTransactions(List<Transaction> transactions, int limit) {
        return transactions.stream()
            .sorted((t1, t2) -> t2.getDate().compareTo(t1.getDate()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    public LiveData<List<Budget>> getBudgets() {
        return repository.getBudgets();
    }

    public LiveData<Double> getTotalBalance() {
        return totalBalance;
    }

    public LiveData<Double> getMonthlySpending() {
        return monthlySpending;
    }

    public LiveData<List<Transaction>> getRecentTransactions() {
        return recentTransactions;
    }

    public LiveData<List<String>> getInsights() {
        return intelligenceService.getInsightsLiveData();
    }
}
