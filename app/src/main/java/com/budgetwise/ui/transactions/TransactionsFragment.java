package com.budgetwise.ui.transactions;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.budgetwise.BudgetWiseApplication;
import com.budgetwise.databinding.FragmentTransactionsBinding;
import com.budgetwise.ui.adapters.TransactionAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class TransactionsFragment extends Fragment {
    private FragmentTransactionsBinding binding;
    private TransactionsViewModel viewModel;
    private TransactionAdapter adapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTransactionsBinding.inflate(inflater, container, false);
        
        setupViewModel();
        setupRecyclerView();
        setupFab();
        observeData();
        
        return binding.getRoot();
    }

    private void setupViewModel() {
        TransactionsViewModelFactory factory = new TransactionsViewModelFactory(
            BudgetWiseApplication.getInstance().getBudgetRepository()
        );
        viewModel = new ViewModelProvider(this, factory).get(TransactionsViewModel.class);
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter(transaction -> {
            // Handle transaction click - open edit dialog
            AddTransactionDialogFragment dialog = AddTransactionDialogFragment.newInstance(transaction);
            dialog.show(getParentFragmentManager(), "edit_transaction");
        });
        
        binding.recyclerViewTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewTransactions.setAdapter(adapter);
    }

    private void setupFab() {
        binding.fabAddTransaction.setOnClickListener(v -> {
            AddTransactionDialogFragment dialog = AddTransactionDialogFragment.newInstance(null);
            dialog.show(getParentFragmentManager(), "add_transaction");
        });
    }

    private void observeData() {
        viewModel.getTransactions().observe(getViewLifecycleOwner(), transactions -> {
            adapter.submitList(transactions);
            updateEmptyState(transactions.isEmpty());
        });

        viewModel.getTotalIncome().observe(getViewLifecycleOwner(), income -> {
            binding.textTotalIncome.setText(String.format("$%.2f", income));
        });

        viewModel.getTotalExpenses().observe(getViewLifecycleOwner(), expenses -> {
            binding.textTotalExpenses.setText(String.format("$%.2f", expenses));
        });
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            binding.layoutEmptyState.setVisibility(View.VISIBLE);
            binding.recyclerViewTransactions.setVisibility(View.GONE);
        } else {
            binding.layoutEmptyState.setVisibility(View.GONE);
            binding.recyclerViewTransactions.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
