package com.budgetwise.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.budgetwise.BudgetWiseApplication;
import com.budgetwise.databinding.FragmentSettingsBinding;
import com.budgetwise.ui.settings.backup.BackupActivity;
import com.budgetwise.utils.ThemeManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding binding;
    private SettingsViewModel viewModel;
    private ThemeManager themeManager;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        
        setupViewModel();
        setupThemeManager();
        setupClickListeners();
        observeData();
        
        return binding.getRoot();
    }

    private void setupViewModel() {
        SettingsViewModelFactory factory = new SettingsViewModelFactory(
            BudgetWiseApplication.getInstance().getBudgetRepository()
        );
        viewModel = new ViewModelProvider(this, factory).get(SettingsViewModel.class);
    }

    private void setupThemeManager() {
        themeManager = new ThemeManager(requireContext());
        updateThemeDisplay();
    }

    private void setupClickListeners() {
        // Backup & Restore
        binding.cardBackupRestore.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), BackupActivity.class);
            startActivity(intent);
        });

        // Theme Selection
        binding.cardTheme.setOnClickListener(v -> showThemeDialog());

        // Security Settings
        binding.cardSecurity.setOnClickListener(v -> {
            // TODO: Open security settings
        });

        // About
        binding.cardAbout.setOnClickListener(v -> showAboutDialog());

        // Export Data
        binding.cardExportData.setOnClickListener(v -> {
            viewModel.exportData();
        });

        // Clear All Data
        binding.cardClearData.setOnClickListener(v -> showClearDataDialog());
    }

    private void observeData() {
        viewModel.getDataStats().observe(getViewLifecycleOwner(), stats -> {
            binding.textTransactionCount.setText(String.format("%d transactions", stats.transactionCount));
            binding.textBudgetCount.setText(String.format("%d budgets", stats.budgetCount));
            binding.textDataSize.setText(String.format("%.1f KB", stats.dataSizeKB));
        });
    }

    private void showThemeDialog() {
        String[] themes = {"Light", "Dark", "System Default"};
        int currentTheme = themeManager.getCurrentTheme();

        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Choose Theme")
            .setSingleChoiceItems(themes, currentTheme, (dialog, which) -> {
                themeManager.setTheme(which);
                updateThemeDisplay();
                applyTheme(which);
                dialog.dismiss();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void applyTheme(int theme) {
        switch (theme) {
            case 0: // Light
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case 1: // Dark
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case 2: // System
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    private void updateThemeDisplay() {
        String[] themes = {"Light", "Dark", "System Default"};
        int currentTheme = themeManager.getCurrentTheme();
        binding.textCurrentTheme.setText(themes[currentTheme]);
    }

    private void showAboutDialog() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("About BudgetWise")
            .setMessage("BudgetWise v1.0\n\nA secure, offline personal finance manager with AI-powered insights.\n\n• AES-256 encryption\n• Local ML intelligence\n• Premium Material 3 UI\n• Zero data collection")
            .setPositiveButton("OK", null)
            .show();
    }

    private void showClearDataDialog() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Clear All Data")
            .setMessage("This will permanently delete all your transactions, budgets, and settings. This action cannot be undone.")
            .setPositiveButton("Clear", (dialog, which) -> {
                viewModel.clearAllData();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
