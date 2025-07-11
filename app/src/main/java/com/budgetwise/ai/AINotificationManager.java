package com.budgetwise.ai;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.budgetwise.MainActivity;
import com.budgetwise.R;

public class AINotificationManager {
    private static final String CHANNEL_ALERTS = "ai_alerts";
    private static final String CHANNEL_REMINDERS = "ai_reminders";
    private static final String CHANNEL_SUMMARIES = "ai_summaries";
    private static final String CHANNEL_WARNINGS = "ai_warnings";
    
    private final Context context;
    private final NotificationManagerCompat notificationManager;

    public AINotificationManager(Context context) {
        this.context = context;
        this.notificationManager = NotificationManagerCompat.from(context);
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            
            // Alerts Channel
            NotificationChannel alertsChannel = new NotificationChannel(
                CHANNEL_ALERTS, "AI Alerts", NotificationManager.IMPORTANCE_DEFAULT);
            alertsChannel.setDescription("Smart financial alerts and suggestions");
            manager.createNotificationChannel(alertsChannel);
            
            // Reminders Channel
            NotificationChannel remindersChannel = new NotificationChannel(
                CHANNEL_REMINDERS, "AI Reminders", NotificationManager.IMPORTANCE_DEFAULT);
            remindersChannel.setDescription("Payment reminders and recurring transactions");
            manager.createNotificationChannel(remindersChannel);
            
            // Summaries Channel
            NotificationChannel summariesChannel = new NotificationChannel(
                CHANNEL_SUMMARIES, "AI Summaries", NotificationManager.IMPORTANCE_LOW);
            summariesChannel.setDescription("Weekly and monthly financial summaries");
            manager.createNotificationChannel(summariesChannel);
            
            // Warnings Channel
            NotificationChannel warningsChannel = new NotificationChannel(
                CHANNEL_WARNINGS, "AI Warnings", NotificationManager.IMPORTANCE_HIGH);
            warningsChannel.setDescription("Budget warnings and anomaly alerts");
            manager.createNotificationChannel(warningsChannel);
        }
    }

    public void showAlert(String title, String message, int notificationId) {
        showNotification(title, message, notificationId, CHANNEL_ALERTS, NotificationCompat.PRIORITY_DEFAULT);
    }

    public void showReminder(String title, String message, int notificationId) {
        showNotification(title, message, notificationId, CHANNEL_REMINDERS, NotificationCompat.PRIORITY_DEFAULT);
    }

    public void showSummary(String title, String message, int notificationId) {
        showNotification(title, message, notificationId, CHANNEL_SUMMARIES, NotificationCompat.PRIORITY_LOW);
    }

    public void showWarning(String title, String message, int notificationId) {
        showNotification(title, message, notificationId, CHANNEL_WARNINGS, NotificationCompat.PRIORITY_HIGH);
    }

    private void showNotification(String title, String message, int notificationId, String channelId, int priority) {
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(priority)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true);

        notificationManager.notify(notificationId, builder.build());
    }
}