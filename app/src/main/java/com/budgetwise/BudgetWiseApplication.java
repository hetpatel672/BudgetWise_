package com.budgetwise;

import android.app.Application;
import android.content.Context;
import com.budgetwise.data.repository.BudgetRepository;
import com.budgetwise.security.EncryptionManager;
import com.budgetwise.ai.EnhancedIntelligenceService;
import com.budgetwise.utils.ThemeManager;
import com.budgetwise.notifications.NotificationManager;

public class BudgetWiseApplication extends Application {
    private static BudgetWiseApplication instance;
    private BudgetRepository budgetRepository;
    private EncryptionManager encryptionManager;
    private EnhancedIntelligenceService intelligenceService;
    private ThemeManager themeManager;
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        initializeServices();
    }

    private void initializeServices() {
        encryptionManager = new EncryptionManager(this);
        budgetRepository = new BudgetRepository(this, encryptionManager);
        intelligenceService = new EnhancedIntelligenceService(this, budgetRepository);
        themeManager = new ThemeManager(this);
        notificationManager = new NotificationManager(this);
        
        // Apply dark mode by default for enhanced UI
        themeManager.setTheme(ThemeManager.THEME_DARK);
        themeManager.applyTheme();
        
        // Schedule backup reminder
        notificationManager.scheduleBackupReminder();
        
        // Start AI analysis
        intelligenceService.runCompleteAnalysis();
    }

    public static BudgetWiseApplication getInstance() {
        return instance;
    }

    public BudgetRepository getBudgetRepository() {
        return budgetRepository;
    }

    public EncryptionManager getEncryptionManager() {
        return encryptionManager;
    }

    public EnhancedIntelligenceService getIntelligenceService() {
        return intelligenceService;
    }

    public ThemeManager getThemeManager() {
        return themeManager;
    }

    public NotificationManager getNotificationManager() {
        return notificationManager;
    }
}
