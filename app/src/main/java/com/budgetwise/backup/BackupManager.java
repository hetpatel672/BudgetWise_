package com.budgetwise.backup;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import com.budgetwise.data.models.Budget;
import com.budgetwise.data.models.Transaction;
import com.budgetwise.data.repository.BudgetRepository;
import com.budgetwise.security.EncryptionManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackupManager {
    private static final String TAG = "BackupManager";
    private static final String BACKUP_FOLDER = "BudgetWise";
    private static final String BACKUP_FILE_PREFIX = "backup_";
    private static final String BACKUP_FILE_EXTENSION = ".json";
    private static final int MAX_BACKUP_FILES = 3;
    
    private final Context context;
    private final BudgetRepository repository;
    private final EncryptionManager encryptionManager;
    private final ExecutorService executorService;
    private final Gson gson;

    public BackupManager(Context context, BudgetRepository repository, EncryptionManager encryptionManager) {
        this.context = context;
        this.repository = repository;
        this.encryptionManager = encryptionManager;
        this.executorService = Executors.newSingleThreadExecutor();
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();
    }

    public interface BackupCallback {
        void onSuccess(String filePath);
        void onError(String error);
    }

    public void createBackup(BackupCallback callback) {
        executorService.execute(() -> {
            try {
                // Create backup data structure
                BackupData backupData = new BackupData();
                backupData.transactions = repository.getCachedTransactions();
                backupData.budgets = repository.getCachedBudgets();
                backupData.timestamp = System.currentTimeMillis();
                backupData.version = "1.0";

                // Convert to JSON
                String jsonData = gson.toJson(backupData);
                
                // Encrypt the data
                String encryptedData = encryptionManager.encrypt(jsonData);
                
                // Create backup file
                File backupFile = createBackupFile();
                
                // Write encrypted data to file
                try (FileWriter writer = new FileWriter(backupFile)) {
                    writer.write(encryptedData);
                }
                
                // Clean up old backups
                cleanupOldBackups();
                
                Log.d(TAG, "Backup created successfully: " + backupFile.getAbsolutePath());
                callback.onSuccess(backupFile.getAbsolutePath());
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to create backup", e);
                callback.onError("Failed to create backup: " + e.getMessage());
            }
        });
    }

    public void restoreBackup(String filePath, BackupCallback callback) {
        executorService.execute(() -> {
            try {
                File backupFile = new File(filePath);
                if (!backupFile.exists()) {
                    callback.onError("Backup file not found");
                    return;
                }

                // Read encrypted data from file
                StringBuilder encryptedData = new StringBuilder();
                try (FileReader reader = new FileReader(backupFile)) {
                    char[] buffer = new char[1024];
                    int length;
                    while ((length = reader.read(buffer)) != -1) {
                        encryptedData.append(buffer, 0, length);
                    }
                }

                // Decrypt the data
                String jsonData = encryptionManager.decrypt(encryptedData.toString());
                
                // Parse JSON
                BackupData backupData = gson.fromJson(jsonData, BackupData.class);
                
                // Validate backup data
                if (backupData == null || backupData.transactions == null || backupData.budgets == null) {
                    callback.onError("Invalid backup file format");
                    return;
                }

                // Clear existing data and restore from backup
                // Note: In a real implementation, you might want to ask user for confirmation
                restoreDataFromBackup(backupData);
                
                Log.d(TAG, "Backup restored successfully from: " + filePath);
                callback.onSuccess("Backup restored successfully");
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to restore backup", e);
                callback.onError("Failed to restore backup: " + e.getMessage());
            }
        });
    }

    private File createBackupFile() throws IOException {
        // Create backup directory
        File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File backupDir = new File(documentsDir, BACKUP_FOLDER);
        
        if (!backupDir.exists() && !backupDir.mkdirs()) {
            throw new IOException("Failed to create backup directory");
        }

        // Create backup file with timestamp
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timestamp = dateFormat.format(new Date());
        String fileName = BACKUP_FILE_PREFIX + timestamp + BACKUP_FILE_EXTENSION;
        
        return new File(backupDir, fileName);
    }

    private void cleanupOldBackups() {
        try {
            File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            File backupDir = new File(documentsDir, BACKUP_FOLDER);
            
            if (!backupDir.exists()) return;

            File[] backupFiles = backupDir.listFiles((dir, name) -> 
                name.startsWith(BACKUP_FILE_PREFIX) && name.endsWith(BACKUP_FILE_EXTENSION));
            
            if (backupFiles == null || backupFiles.length <= MAX_BACKUP_FILES) return;

            // Sort files by last modified date (oldest first)
            java.util.Arrays.sort(backupFiles, (f1, f2) -> 
                Long.compare(f1.lastModified(), f2.lastModified()));

            // Delete oldest files
            int filesToDelete = backupFiles.length - MAX_BACKUP_FILES;
            for (int i = 0; i < filesToDelete; i++) {
                if (backupFiles[i].delete()) {
                    Log.d(TAG, "Deleted old backup: " + backupFiles[i].getName());
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to cleanup old backups", e);
        }
    }

    private void restoreDataFromBackup(BackupData backupData) {
        // Clear existing data
        // Note: This is a simplified implementation
        // In production, you might want to create a transaction-like operation
        
        // Restore transactions
        for (Transaction transaction : backupData.transactions) {
            repository.addTransaction(transaction);
        }
        
        // Restore budgets
        for (Budget budget : backupData.budgets) {
            repository.addBudget(budget);
        }
    }

    public void getAvailableBackups(BackupListCallback callback) {
        executorService.execute(() -> {
            try {
                File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                File backupDir = new File(documentsDir, BACKUP_FOLDER);
                
                if (!backupDir.exists()) {
                    callback.onSuccess(new File[0]);
                    return;
                }

                File[] backupFiles = backupDir.listFiles((dir, name) -> 
                    name.startsWith(BACKUP_FILE_PREFIX) && name.endsWith(BACKUP_FILE_EXTENSION));
                
                if (backupFiles == null) {
                    callback.onSuccess(new File[0]);
                    return;
                }

                // Sort files by last modified date (newest first)
                java.util.Arrays.sort(backupFiles, (f1, f2) -> 
                    Long.compare(f2.lastModified(), f1.lastModified()));

                callback.onSuccess(backupFiles);
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to get available backups", e);
                callback.onError("Failed to get available backups: " + e.getMessage());
            }
        });
    }

    public interface BackupListCallback {
        void onSuccess(File[] backupFiles);
        void onError(String error);
    }

    private static class BackupData {
        public List<Transaction> transactions;
        public List<Budget> budgets;
        public long timestamp;
        public String version;
    }
}
