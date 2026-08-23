package com.tanim.ccepedia;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.text.TextUtils;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * Receives class-reminder alarms from {@link ClassReminderScheduler} and posts the heads-up
 * notification, then re-syncs so the class that just fired rolls forward to next week. Also listens
 * for boot / app-update broadcasts to re-arm all alarms (the system drops them on reboot).
 */
public class ClassReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "class_reminders";
    private static final int NOTIF_ID_BASE = 5000;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;

        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            // Alarms don't survive a reboot or reinstall — rebuild them from the cached routine.
            ClassReminderScheduler.sync(context);
            return;
        }

        // Otherwise this is a class reminder firing.
        String course = intent.getStringExtra(ClassReminderScheduler.EXTRA_COURSE);
        String timeLabel = intent.getStringExtra(ClassReminderScheduler.EXTRA_TIME);
        int index = intent.getIntExtra(ClassReminderScheduler.EXTRA_INDEX, 0);

        if (!TextUtils.isEmpty(course)) {
            showReminder(context, course, timeLabel, index);
        }

        // One-shot alarm: reschedule this class for next week and keep the rest fresh.
        ClassReminderScheduler.sync(context);
    }

    private void showReminder(Context context, String course, String timeLabel, int index) {
        createChannel(context);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, index, new Intent(context, MainActivity.class), flags);

        String text = context.getString(R.string.routine_reminder_text,
                course, timeLabel, ClassReminderScheduler.LEAD_MINUTES);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setContentTitle(context.getString(R.string.routine_reminder_title))
                .setContentText(text)
                .setColor(Color.parseColor("#6200EE"))
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return; // no permission → drop silently, same as the FCM path
        }
        NotificationManagerCompat.from(context).notify(NOTIF_ID_BASE + index, builder.build());
    }

    private void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.routine_reminder_channel_name),
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(context.getString(R.string.routine_reminder_channel_desc));

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
