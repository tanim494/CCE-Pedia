package com.tanim.ccepedia;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class LoadingActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SharedPreferences sharedPreferences = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("DarkMode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        TextView versionText = findViewById(R.id.appVersionText);
        versionText.setText(getString(R.string.app_version, BuildConfig.VERSION_NAME));

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        new Handler().postDelayed(() -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();

            if (currentUser == null) {
                Toast.makeText(this, "Login to continue.", Toast.LENGTH_SHORT).show();
                goToLogin();
            } else {
                currentUser.reload()
                        .addOnSuccessListener(unused -> {
                            FirebaseUser updatedUser = mAuth.getCurrentUser();
                            if (updatedUser == null) {
                                Toast.makeText(this, "Account not found. Please login again.", Toast.LENGTH_SHORT).show();
                                goToLogin();
                                return;
                            }

                            String uid = updatedUser.getUid();
                            db.collection("users")
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
        }, 1500);
    }

    private void updateLastLoggedIn() {
        if (mAuth.getCurrentUser() == null) {
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("lastLoggedIn", FieldValue.serverTimestamp());

        db.collection("users")
                .document(uid)
                .set(updateData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                })
                .addOnFailureListener(e -> {
                });
    }

    private void handleUserDocument(DocumentSnapshot snapshot) {
        if (snapshot.exists()) {
            UserData user = UserData.getInstance();
            user.setStudentId(snapshot.getString("id"));
            user.setName(snapshot.getString("name"));
            user.setEmail(snapshot.getString("email"));
            user.setGender(snapshot.getString("gender"));
            user.setPhone(snapshot.getString("phone"));
            user.setSemester(snapshot.getString("semester"));

            String role = snapshot.getString("role");
            user.setRole(role != null ? role : "");

            Long viewCount = snapshot.getLong("viewCount");
            user.setViewCount(viewCount != null ? viewCount : 0);

            String departmentCode = snapshot.getString("department");

            if (departmentCode == null || departmentCode.isEmpty()) {
                departmentCode = "CCE";
            }

            user.setDepartmentName(departmentCode);

            updateLastLoggedIn();

            if (departmentCode.equalsIgnoreCase("CCE")) {
                goToHome();
            } else {
                goToHome();
            }
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
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}