package com.tanim.ccepedia;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class Loading extends AppCompatActivity {
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        mAuth = FirebaseAuth.getInstance();

        new Handler().postDelayed(() -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();

            if (currentUser == null) {
                Toast.makeText(this, "Login to continue.", Toast.LENGTH_SHORT).show();
                goToLogin();
            } else {
                // 🔄 Reload the user to check if still valid
                currentUser.reload()
                        .addOnSuccessListener(unused -> {
                            // Check again in case account was deleted
                            FirebaseUser updatedUser = mAuth.getCurrentUser();
                            if (updatedUser == null) {
                                Toast.makeText(this, "Account not found. Please login again.", Toast.LENGTH_SHORT).show();
                                goToLogin();
                                return;
                            }

                            // ✅ Proceed to check Firestore user data
                            String uid = updatedUser.getUid();
                            FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(uid)
                                    .get()
                                    .addOnSuccessListener(this::handleUserDocument)
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Error loading profile. Try again.", Toast.LENGTH_SHORT).show();
                                        goToLogin();
                                    });
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
                            mAuth.signOut();
                            goToLogin();
                        });
            }
        }, 1500);  // Delay for splash screen
    }

    private void handleUserDocument(DocumentSnapshot snapshot) {
        if (snapshot.exists()) {
            // ✅ Populate singleton
            UserData user = UserData.getInstance();
            user.setId(snapshot.getString("id"));
            user.setName(snapshot.getString("name"));
            user.setEmail(snapshot.getString("email"));
            user.setGender(snapshot.getString("gender"));
            user.setPhone(snapshot.getString("phone"));
            user.setSemester(snapshot.getString("semester"));

            String role = snapshot.getString("role");
            user.setRole(role != null ? role : "");

            goToHome();
        } else {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(this, "Profile data not found. Please login.", Toast.LENGTH_SHORT).show();
            goToLogin();
        }
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void goToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
