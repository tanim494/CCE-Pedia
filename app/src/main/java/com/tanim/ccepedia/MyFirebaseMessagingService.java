package com.tanim.ccepedia;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Handle the message here
        if (remoteMessage.getNotification() != null) {
            // Extract notification data
            String title = remoteMessage.getNotification().getTitle();
            String message = remoteMessage.getNotification().getBody();

            // Call a method to display the notification
            sendNotification(title, message);
        }
    }

    private void sendNotification(String title, String messageBody) {
        // Create a notification intent
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Create a PendingIntent to open your activity when the notification is clicked
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        // Build the notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "default_channel")
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_stat_onesignal_default) // Replace with your own icon
                .setContentIntent(pendingIntent);

        // Get the NotificationManager system service to display the notification
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, notificationBuilder.build());
    }
}
