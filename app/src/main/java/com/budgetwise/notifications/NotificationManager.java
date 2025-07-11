package com.budgetwise.notifications;

import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.budgetwise.MainActivity;
import com.budgetwise.R;
import com.budgetwise.data.models.Budget;
import java.util.concurrent.TimeUnit;

public class NotificationManager {
    private static final String CHANNEL_ID = "budget_alerts";
    private static final String CHANNEL_NAME = "Budget Alerts";
    private static final int NOTIFICATION_ID_BASE = 1000;
    
    private final Context context;
    private final androidx.core.app.NotificationManagerCompat notificationManager;

    public NotificationManager(Context context) {
        this.context = context;
        this.notificationManager = NotificationManagerCompat.from(context);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notifications for budget alerts and reminders");
            
            android.app.NotificationManager manager = 
                (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }

    public void showBudgetAlert(Budget budget, BudgetAlertType alertType) {
        String title, message;
        
        switch (alertType) {
            case OVER_BUDGET:
                title = "Budget Exceeded!";
                message = String.format("You've exceeded your %s budget by $%.2f", 
                    budget.getCategory(), budget.getSpentAmount() - budget.getBudgetAmount());
                break;
            case APPROACHING_LIMIT:
                title = "Budget Alert";
                message = String.format("You've used %.1f%% of your %s budget", 
                    budget.getSpentPercentage(), budget.getCategory());
                break;
            default:
                return;
        }

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true);

        notificationManager.notify(NOTIFICATION_ID_BASE + budget.hashCode(), builder.build());
    }

    public void scheduleBackupReminder() {
        OneTimeWorkRequest backupReminderWork = new OneTimeWorkRequest.Builder(BackupReminderWorker.class)
            .setInitialDelay(24, TimeUnit.HOURS)
            .build();

        WorkManager.getInstance(context).enqueue(backupReminderWork);
    }

    public enum BudgetAlertType {
        OVER_BUDGET, APPROACHING_LIMIT
    }

    public static class BackupReminderWorker extends Worker {
        public BackupReminderWorker(Context context, WorkerParameters params) {
            super(context, params);
        }

        @Override
        public Result doWork() {
            NotificationManager notificationManager = new NotificationManager(getApplicationContext());
            notificationManager.showBackupReminder();
            return Result.success();
        }
    }

    private void showBackupReminder() {
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Backup Reminder")
            .setContentText("Don't forget to backup your financial data")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true);

        notificationManager.notify(NOTIFICATION_ID_BASE + 999, builder.build());
    }
}
