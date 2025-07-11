package com.budgetwise.ui.transactions;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;
import com.budgetwise.data.models.Transaction;
import com.budgetwise.data.repository.BudgetRepository;
import java.util.List;

public class TransactionsViewModel extends ViewModel {
    private final BudgetRepository repository;
    private final MediatorLiveData<Double> totalIncome = new MediatorLiveData<>();
    private final MediatorLiveData<Double> totalExpenses = new MediatorLiveData<>();

    public TransactionsViewModel(BudgetRepository repository) {
        this.repository = repository;
        setupMediators();
    }

    private void setupMediators() {
        totalIncome.addSource(repository.getTransactions(), transactions -> {
            double income = transactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.INCOME)
                .mapToDouble(Transaction::getAmount)
                .sum();
            totalIncome.setValue(income);
        });

        totalExpenses.addSource(repository.getTransactions(), transactions -> {
            double expenses = transactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
                .mapToDouble(Transaction::getAmount)
                .sum();
            totalExpenses.setValue(expenses);
        });
    }

    public LiveData<List<Transaction>> getTransactions() {
        return repository.getTransactions();
    }

    public LiveData<Double> getTotalIncome() {
        return totalIncome;
    }

    public LiveData<Double> getTotalExpenses() {
        return totalExpenses;
    }

    public void deleteTransaction(String transactionId) {
        repository.deleteTransaction(transactionId);
    }
}
