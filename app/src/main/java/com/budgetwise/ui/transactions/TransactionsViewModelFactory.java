package com.budgetwise.ui.transactions;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.budgetwise.data.repository.BudgetRepository;

public class TransactionsViewModelFactory implements ViewModelProvider.Factory {
    private final BudgetRepository repository;

    public TransactionsViewModelFactory(BudgetRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(TransactionsViewModel.class)) {
            return (T) new TransactionsViewModel(repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
