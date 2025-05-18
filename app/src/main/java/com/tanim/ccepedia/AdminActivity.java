package com.tanim.ccepedia;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;

public class AdminActivity extends AppCompatActivity {

    private Button sendPushNotificationBtn;
    private Button manageResourcesBtn;
    private EditText notificationTitleEditText;
    private EditText notificationMessageEditText;

    private DatabaseReference resourcesDb;
    private DatabaseReference usersDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        sendPushNotificationBtn = findViewById(R.id.sendPushNotificationBtn);
        manageResourcesBtn = findViewById(R.id.manageResourcesBtn);
        notificationTitleEditText = findViewById(R.id.notificationTitleEditText);
        notificationMessageEditText = findViewById(R.id.notificationMessageEditText);

        // Firebase reference for resources and users
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://cce-pedia-5284c-default-rtdb.asia-southeast1.firebasedatabase.app/");
        resourcesDb = database.getReference("resources");
        usersDb = database.getReference("users");

        // Send push notification
        sendPushNotificationBtn.setOnClickListener(v -> {
            String title = notificationTitleEditText.getText().toString();
            String message = notificationMessageEditText.getText().toString();
            if (!title.isEmpty() && !message.isEmpty()) {
                sendPushNotification(title, message);
            } else {
                showErrorDialog("Please enter both title and message.");
            }
        });

        // Manage resources
        manageResourcesBtn.setOnClickListener(v -> {
            // Open a dialog or another activity to add/edit resources
            manageResources();
        });
    }

    private void sendPushNotification(String title, String message) {
        // Construct the FCM message payload
        JSONObject notification = new JSONObject();
        try {
            notification.put("title", title);
            notification.put("body", message);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Get the FCM tokens of users (You would typically send a notification to a segment of users, not all)
        FirebaseMessaging.getInstance().send(new RemoteMessage.Builder("cce-pedia-firebase.messaging")
                .setMessageId(Integer.toString((int) System.currentTimeMillis()))
                .addData("title", title)
                .addData("message", message)
                .build());
    }

    private void manageResources() {
        // You can open a dialog or activity here to manage resources (add/edit)
        new AlertDialog.Builder(this)
                .setTitle("Manage Resources")
                .setMessage("You can edit resources in the Firebase Database.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // New functionality to edit user data or change user roles
    private void editUserData(String userId, String newName, String newSemester) {
        // Update user data in Firebase (e.g., change name or semester)
        usersDb.child(userId).child("name").setValue(newName);
        usersDb.child(userId).child("semester").setValue(newSemester);

        new AlertDialog.Builder(this)
                .setTitle("User Updated")
                .setMessage("User data has been updated successfully.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // Example method to delete a user from the database
    private void deleteUser(String userId) {
        usersDb.child(userId).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        new AlertDialog.Builder(this)
                                .setTitle("User Deleted")
                                .setMessage("The user has been deleted successfully.")
                                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                                .show();
                    } else {
                        showErrorDialog("Failed to delete user.");
                    }
                });
    }
}
