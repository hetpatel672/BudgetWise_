package com.budgetwise.ui.analytics;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.budgetwise.BudgetWiseApplication;
import com.budgetwise.databinding.FragmentAnalyticsBinding;
import com.budgetwise.ui.views.PieChartView;
import com.budgetwise.ui.views.BarChartView;
import java.util.Map;

public class AnalyticsFragment extends Fragment {
    private FragmentAnalyticsBinding binding;
    private AnalyticsViewModel viewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAnalyticsBinding.inflate(inflater, container, false);
        
        setupViewModel();
        setupCharts();
        observeData();
        
        return binding.getRoot();
    }

    private void setupViewModel() {
        AnalyticsViewModelFactory factory = new AnalyticsViewModelFactory(
            BudgetWiseApplication.getInstance().getBudgetRepository(),
            BudgetWiseApplication.getInstance().getIntelligenceService()
        );
        viewModel = new ViewModelProvider(this, factory).get(AnalyticsViewModel.class);
    }

    private void setupCharts() {
        // Initialize custom chart views
        binding.pieChartCategories.setAnimationEnabled(true);
        binding.barChartMonthly.setAnimationEnabled(true);
    }

    private void observeData() {
        viewModel.getCategorySpending().observe(getViewLifecycleOwner(), categoryData -> {
            binding.pieChartCategories.setData(categoryData);
        });

        viewModel.getMonthlySpending().observe(getViewLifecycleOwner(), monthlyData -> {
            binding.barChartMonthly.setData(monthlyData);
        });

        viewModel.getSpendingTrend().observe(getViewLifecycleOwner(), trend -> {
            updateTrendIndicator(trend);
        });

        viewModel.getTopCategory().observe(getViewLifecycleOwner(), topCategory -> {
            binding.textTopCategory.setText(topCategory);
        });

        viewModel.getAverageDaily().observe(getViewLifecycleOwner(), avgDaily -> {
            binding.textAverageDaily.setText(String.format("$%.2f", avgDaily));
        });

        viewModel.getSavingsRate().observe(getViewLifecycleOwner(), savingsRate -> {
            binding.textSavingsRate.setText(String.format("%.1f%%", savingsRate));
            updateSavingsRateColor(savingsRate);
        });
    }

    private void updateTrendIndicator(AnalyticsViewModel.SpendingTrend trend) {
        switch (trend) {
            case INCREASING:
                binding.textSpendingTrend.setText("📈 Spending is increasing");
                binding.textSpendingTrend.setTextColor(Color.parseColor("#E53E3E"));
                break;
            case DECREASING:
                binding.textSpendingTrend.setText("📉 Spending is decreasing");
                binding.textSpendingTrend.setTextColor(Color.parseColor("#38A169"));
                break;
            case STABLE:
                binding.textSpendingTrend.setText("➡️ Spending is stable");
                binding.textSpendingTrend.setTextColor(Color.parseColor("#4A5568"));
                break;
        }
    }

    private void updateSavingsRateColor(double savingsRate) {
        int color;
        if (savingsRate >= 20) {
            color = Color.parseColor("#38A169"); // Green
        } else if (savingsRate >= 10) {
            color = Color.parseColor("#DD6B20"); // Orange
        } else {
            color = Color.parseColor("#E53E3E"); // Red
        }
        binding.textSavingsRate.setTextColor(color);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
