package com.budgetwise.ui.settings.backup;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.budgetwise.databinding.ActivityBackupBinding;

public class BackupActivity extends AppCompatActivity {
    private ActivityBackupBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBackupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupClickListeners();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Backup & Restore");
        }
    }

    private void setupClickListeners() {
        // TODO: Implement backup and restore functionality
        binding.buttonBackup.setOnClickListener(v -> {
            // Implement backup logic
        });

        binding.buttonRestore.setOnClickListener(v -> {
            // Implement restore logic
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}