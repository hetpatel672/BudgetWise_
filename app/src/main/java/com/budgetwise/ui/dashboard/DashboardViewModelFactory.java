package com.budgetwise.ui.dashboard;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.budgetwise.data.repository.BudgetRepository;
import com.budgetwise.ai.EnhancedIntelligenceService;

public class DashboardViewModelFactory implements ViewModelProvider.Factory {
    private final BudgetRepository repository;
    private final EnhancedIntelligenceService intelligenceService;

    public DashboardViewModelFactory(BudgetRepository repository, EnhancedIntelligenceService intelligenceService) {
        this.repository = repository;
        this.intelligenceService = intelligenceService;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(DashboardViewModel.class)) {
            return (T) new DashboardViewModel(repository, intelligenceService);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
