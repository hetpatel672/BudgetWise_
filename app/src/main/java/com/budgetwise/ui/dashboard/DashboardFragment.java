package com.budgetwise.ui.dashboard;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.budgetwise.BudgetWiseApplication;
import com.budgetwise.databinding.FragmentDashboardBinding;
import com.budgetwise.ui.adapters.BudgetAdapter;
import com.budgetwise.ui.adapters.RecentTransactionAdapter;
import com.budgetwise.ui.animations.AnimationUtils;
import com.budgetwise.ui.transactions.AddTransactionDialogFragment;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DashboardFragment extends Fragment {
    private FragmentDashboardBinding binding;
    private DashboardViewModel viewModel;
    private BudgetAdapter budgetAdapter;
    private RecentTransactionAdapter transactionAdapter;
    private Handler animationHandler = new Handler(Looper.getMainLooper());

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        
        setupViewModel();
        setupRecyclerViews();
        setupClickListeners();
        setupGreeting();
        observeData();
        startEntranceAnimations();
        
        return binding.getRoot();
    }

    private void setupViewModel() {
        DashboardViewModelFactory factory = new DashboardViewModelFactory(
            BudgetWiseApplication.getInstance().getBudgetRepository(),
            BudgetWiseApplication.getInstance().getIntelligenceService()
        );
        viewModel = new ViewModelProvider(this, factory).get(DashboardViewModel.class);
    }

    private void setupRecyclerViews() {
        // Setup budgets RecyclerView
        budgetAdapter = new BudgetAdapter();
        binding.recyclerViewBudgets.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewBudgets.setAdapter(budgetAdapter);

        // Setup recent transactions RecyclerView
        transactionAdapter = new RecentTransactionAdapter();
        binding.recyclerViewRecentTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewRecentTransactions.setAdapter(transactionAdapter);
    }

    private void setupClickListeners() {
        // FAB click
        binding.fabAddTransaction.setOnClickListener(v -> {
            AnimationUtils.pulse(v);
            AddTransactionDialogFragment dialog = AddTransactionDialogFragment.newInstance(null);
            dialog.show(getParentFragmentManager(), "add_transaction");
        });
        
        // Quick action cards
        binding.cardAddIncome.setOnClickListener(v -> {
            AnimationUtils.scaleIn(v);
            // TODO: Open add income dialog
        });
        
        binding.cardAddExpense.setOnClickListener(v -> {
            AnimationUtils.scaleIn(v);
            // TODO: Open add expense dialog
        });
        
        binding.cardViewAnalytics.setOnClickListener(v -> {
            AnimationUtils.scaleIn(v);
            Navigation.findNavController(v).navigate(com.budgetwise.R.id.navigation_analytics);
        });
        
        // View all transactions
        binding.buttonViewAllTransactions.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(com.budgetwise.R.id.navigation_transactions);
        });
        
        // Refresh insights
        binding.buttonRefreshInsights.setOnClickListener(v -> {
            AnimationUtils.pulse(v);
            BudgetWiseApplication.getInstance().getIntelligenceService().runCompleteAnalysis();
        });
    }
    
    private void setupGreeting() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        
        String greeting;
        if (hour < 12) {
            greeting = "Good Morning! ☀️";
        } else if (hour < 17) {
            greeting = "Good Afternoon! 🌤️";
        } else {
            greeting = "Good Evening! 🌙";
        }
        
        binding.textGreeting.setText(greeting);
    }
    
    private void startEntranceAnimations() {
        // Hide all views initially
        binding.cardInsights.setAlpha(0f);
        binding.cardAddIncome.setAlpha(0f);
        binding.cardAddExpense.setAlpha(0f);
        binding.cardViewAnalytics.setAlpha(0f);
        
        // Animate views in sequence
        animationHandler.postDelayed(() -> AnimationUtils.fadeIn(binding.cardInsights), 200);
        animationHandler.postDelayed(() -> AnimationUtils.slideInFromBottom(binding.cardAddIncome), 400);
        animationHandler.postDelayed(() -> AnimationUtils.slideInFromBottom(binding.cardAddExpense), 500);
        animationHandler.postDelayed(() -> AnimationUtils.slideInFromBottom(binding.cardViewAnalytics), 600);
    }
    private void observeData() {
        viewModel.getBudgets().observe(getViewLifecycleOwner(), budgets -> {
            budgetAdapter.submitList(budgets);
            updateBudgetSummary(budgets);
            
            // Animate budget list
            if (!budgets.isEmpty()) {
                animationHandler.postDelayed(() -> 
                    AnimationUtils.staggeredAnimation(binding.recyclerViewBudgets, 100), 800);
            }
        });

        viewModel.getRecentTransactions().observe(getViewLifecycleOwner(), transactions -> {
            transactionAdapter.submitList(transactions);
            
            // Animate transaction list
            if (!transactions.isEmpty()) {
                animationHandler.postDelayed(() -> 
                    AnimationUtils.staggeredAnimation(binding.recyclerViewRecentTransactions, 80), 1000);
            }
        });

        viewModel.getTotalBalance().observe(getViewLifecycleOwner(), balance -> {
            // Animate balance count up
            AnimationUtils.countUpAnimation(binding.textTotalBalance, 0, balance, "$", "");
        });

        viewModel.getMonthlySpending().observe(getViewLifecycleOwner(), spending -> {
            // Animate spending count up
            AnimationUtils.countUpAnimation(binding.textMonthlySpending, 0, spending, "$", "");
        });

        viewModel.getInsights().observe(getViewLifecycleOwner(), insights -> {
            updateInsightsSection(insights);
        });
    }

    private void updateBudgetSummary(java.util.List<com.budgetwise.data.models.Budget> budgets) {
        if (budgets.isEmpty()) {
            binding.textBudgetSummary.setText("No budgets set");
            return;
        }

        int totalBudgets = budgets.size();
        long overBudget = budgets.stream().mapToLong(b -> b.isOverBudget() ? 1 : 0).sum();
        
        binding.textBudgetSummary.setText(String.format("%d budgets, %d over limit", totalBudgets, overBudget));
        
        // Shake if over budget
        if (overBudget > 0) {
            AnimationUtils.shake(binding.textBudgetSummary);
        }
    }

    private void updateInsightsSection(java.util.List<String> insights) {
        if (insights.isEmpty()) {
            binding.textInsights.setText("No insights available");
        } else {
            binding.textInsights.setText(insights.get(0)); // Show first insight
            AnimationUtils.fadeIn(binding.textInsights);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        animationHandler.removeCallbacksAndMessages(null);
        binding = null;
    }
}
