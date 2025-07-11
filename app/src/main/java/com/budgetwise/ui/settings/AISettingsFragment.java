package com.budgetwise.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.budgetwise.BudgetWiseApplication;
import com.budgetwise.ai.EnhancedIntelligenceService;
import com.budgetwise.databinding.FragmentAiSettingsBinding;
import com.budgetwise.utils.ThemeManager;

public class AISettingsFragment extends Fragment {
    private FragmentAiSettingsBinding binding;
    private EnhancedIntelligenceService intelligenceService;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAiSettingsBinding.inflate(inflater, container, false);
        
        intelligenceService = BudgetWiseApplication.getInstance().getIntelligenceService();
        
        setupClickListeners();
        loadSettings();
        
        return binding.getRoot();
    }

    private void setupClickListeners() {
        // Toggle AI notifications
        binding.switchAiNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save preference
            getContext().getSharedPreferences("ai_settings", 0)
                .edit()
                .putBoolean("notifications_enabled", isChecked)
                .apply();
        });

        // Toggle budget alerts
        binding.switchBudgetAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            getContext().getSharedPreferences("ai_settings", 0)
                .edit()
                .putBoolean("budget_alerts_enabled", isChecked)
                .apply();
        });

        // Toggle weekly summaries
        binding.switchWeeklySummaries.setOnCheckedChangeListener((buttonView, isChecked) -> {
            getContext().getSharedPreferences("ai_settings", 0)
                .edit()
                .putBoolean("weekly_summaries_enabled", isChecked)
                .apply();
        });

        // Toggle goal recommendations
        binding.switchGoalRecommendations.setOnCheckedChangeListener((buttonView, isChecked) -> {
            getContext().getSharedPreferences("ai_settings", 0)
                .edit()
                .putBoolean("goal_recommendations_enabled", isChecked)
                .apply();
        });

        // Toggle anomaly detection
        binding.switchAnomalyDetection.setOnCheckedChangeListener((buttonView, isChecked) -> {
            getContext().getSharedPreferences("ai_settings", 0)
                .edit()
                .putBoolean("anomaly_detection_enabled", isChecked)
                .apply();
        });

        // Run AI analysis button
        binding.buttonRunAnalysis.setOnClickListener(v -> {
            binding.buttonRunAnalysis.setEnabled(false);
            binding.buttonRunAnalysis.setText("Running Analysis...");
            
            intelligenceService.runCompleteAnalysis();
            
            // Re-enable button after delay
            v.postDelayed(() -> {
                binding.buttonRunAnalysis.setEnabled(true);
                binding.buttonRunAnalysis.setText("Run AI Analysis");
            }, 3000);
        });
    }

    private void loadSettings() {
        var prefs = getContext().getSharedPreferences("ai_settings", 0);
        
        binding.switchAiNotifications.setChecked(prefs.getBoolean("notifications_enabled", true));
        binding.switchBudgetAlerts.setChecked(prefs.getBoolean("budget_alerts_enabled", true));
        binding.switchWeeklySummaries.setChecked(prefs.getBoolean("weekly_summaries_enabled", true));
        binding.switchGoalRecommendations.setChecked(prefs.getBoolean("goal_recommendations_enabled", true));
        binding.switchAnomalyDetection.setChecked(prefs.getBoolean("anomaly_detection_enabled", true));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}