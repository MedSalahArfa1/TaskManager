package com.fsb.taskmanager;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationReceiver extends BroadcastReceiver {
    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        String taskTitle = intent.getStringExtra("taskTitle");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "TASK_CHANNEL")
                .setSmallIcon(R.drawable.baseline_add_alert_24)
                .setContentTitle("Rappel - Task Manager")
                .setContentText("N'oubliez pas de complèter cette tâche: " + taskTitle)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(1001, builder.build());
    }
}

