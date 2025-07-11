package com.budgetwise.ui.settings;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;
import com.budgetwise.data.repository.BudgetRepository;

public class SettingsViewModel extends ViewModel {
    private final BudgetRepository repository;
    private final MediatorLiveData<DataStats> dataStats = new MediatorLiveData<>();

    public static class DataStats {
        public int transactionCount;
        public int budgetCount;
        public double dataSizeKB;
    }

    public SettingsViewModel(BudgetRepository repository) {
        this.repository = repository;
        setupMediators();
    }

    private void setupMediators() {
        dataStats.addSource(repository.getTransactions(), transactions -> {
            DataStats stats = new DataStats();
            stats.transactionCount = transactions.size();
            stats.budgetCount = repository.getCachedBudgets().size();
            stats.dataSizeKB = calculateDataSize(transactions.size(), stats.budgetCount);
            dataStats.setValue(stats);
        });
    }

    private double calculateDataSize(int transactionCount, int budgetCount) {
        // Rough estimate: each transaction ~0.5KB, each budget ~0.2KB
        return (transactionCount * 0.5) + (budgetCount * 0.2);
    }

    public LiveData<DataStats> getDataStats() {
        return dataStats;
    }

    public void exportData() {
        // TODO: Implement data export functionality
    }

    public void clearAllData() {
        // Clear all transactions and budgets
        for (String transactionId : repository.getCachedTransactions().stream()
                .map(t -> t.getId()).toArray(String[]::new)) {
            repository.deleteTransaction(transactionId);
        }
        
        for (String budgetId : repository.getCachedBudgets().stream()
                .map(b -> b.getId()).toArray(String[]::new)) {
            repository.deleteBudget(budgetId);
        }
    }
}
