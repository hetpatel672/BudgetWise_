package com.budgetwise.ui.analytics;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.budgetwise.data.repository.BudgetRepository;
import com.budgetwise.ai.EnhancedIntelligenceService;

public class AnalyticsViewModelFactory implements ViewModelProvider.Factory {
    private final BudgetRepository repository;
    private final EnhancedIntelligenceService intelligenceService;

    public AnalyticsViewModelFactory(BudgetRepository repository, EnhancedIntelligenceService intelligenceService) {
        this.repository = repository;
        this.intelligenceService = intelligenceService;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(AnalyticsViewModel.class)) {
            return (T) new AnalyticsViewModel(repository, intelligenceService);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
