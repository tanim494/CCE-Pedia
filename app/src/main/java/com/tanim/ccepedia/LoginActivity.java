package com.tanim.ccepedia;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText emailEditText;
    private TextInputLayout emailField;
    private TextInputEditText passwordText;
    private TextInputLayout passwordField;

    private Button loginButton, resetPasswordButton;
    private TextView registerTextView, forgotPasswordTextView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);

        emailEditText = findViewById(R.id.emailEditText);
        passwordText = findViewById(R.id.passwordText);

        loginButton = findViewById(R.id.loginButton);
        resetPasswordButton = findViewById(R.id.resetPasswordButton);
        registerTextView = findViewById(R.id.registerTextView);
        forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView);

        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordText.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                showAlert("Please enter both email and password");
            } else {
                loginUser(email, password);
            }
        });

        registerTextView.setOnClickListener(v -> goToRegister());

        forgotPasswordTextView.setOnClickListener(v -> toggleLoginView());

        resetPasswordButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            if (email.isEmpty()) {
                showAlert("Please enter your email");
            } else {
                resetPassword(email);
            }
        });
    }

    private void updateLastLoggedIn() {
        if (mAuth.getCurrentUser() == null) {
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("lastLoggedIn", FieldValue.serverTimestamp());
        updateData.put("appVersion", getInstalledVersion());

        db.collection("users")
                .document(uid)
                .set(updateData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                })
                .addOnFailureListener(e -> {
                });
    }

    /** Version name the user currently has installed (e.g. "4.0"), or "" if unavailable. */
    private String getInstalledVersion() {
        try {
            String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            return version != null ? version : "";
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
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
            if (role == null || role.isBlank()) {
                user.setRole("");
            } else {
                user.setRole(role);
            }

            Long viewCount = snapshot.getLong("viewCount");
            user.setViewCount(viewCount != null ? viewCount : 0);

            String departmentName = snapshot.getString("department");

            if (departmentName == null || departmentName.isEmpty()) {
                departmentName = "CCE";
            }

            user.setDepartmentName(departmentName);

            // Load profile image URL
            String photoUrl = snapshot.getString("photoUrl");
            if (photoUrl != null && !photoUrl.isEmpty()) {
                user.setPhotoUrl(photoUrl);
            }

            // Load profile link
            String profileLink = snapshot.getString("profileLink");
            if (profileLink != null && !profileLink.isEmpty()) {
                user.setProfileLink(profileLink);
            }

            updateLastLoggedIn();

            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            showAlert("User data not found, contact the developer");
            loginButton.setEnabled(true);
        }
    }

    private void loginUser(String email, String password) {
        loginButton.setEnabled(false);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            if (user.isEmailVerified()) {
                                String uid = user.getUid();

                                AnalyticsHelper.logLogin("email");

                                db.collection("users")
                                        .document(uid)
                                        .get()
                                        .addOnSuccessListener(this::handleUserDocument)
                                        .addOnFailureListener(e -> {
                                            showAlert("Failed to fetch user data");
                                            loginButton.setEnabled(true);
                                        });

                            } else {
                                user.sendEmailVerification()
                                        .addOnSuccessListener(aVoid -> {
                                            showAlert("Email not verified. A verification email has been sent.");
                                            loginButton.setEnabled(true);
                                        })
                                        .addOnFailureListener(e -> {
                                            showAlert("Failed to resend verification email. Please try again.");
                                            loginButton.setEnabled(true);
                                        });
                            }
                        } else {
                            loginButton.setEnabled(true);
                        }
                    } else {
                        showAlert("Incorrect Email/Password");
                        loginButton.setEnabled(true);
                    }
                });
    }

    private void showAlert(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Alert!")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void toggleLoginView() {
        if (resetPasswordButton.getVisibility() == View.VISIBLE) {
            showLoginView();
        } else {
            showResetPasswordView();
        }
    }

    private void showLoginView() {
        resetPasswordButton.setVisibility(View.GONE);
        loginButton.setVisibility(View.VISIBLE);

        emailField.setVisibility(View.VISIBLE);
        passwordField.setVisibility(View.VISIBLE);

        registerTextView.setVisibility(View.VISIBLE);
        forgotPasswordTextView.setVisibility(View.VISIBLE);
        forgotPasswordTextView.setText("Forgot Password?");
    }

    private void showResetPasswordView() {
        passwordField.setVisibility(View.GONE);
        loginButton.setVisibility(View.GONE);

        resetPasswordButton.setVisibility(View.VISIBLE);

        registerTextView.setVisibility(View.GONE);

        emailField.setVisibility(View.VISIBLE);

        forgotPasswordTextView.setText("Go to Login");
    }

    private void resetPassword(String email) {
        if (email.isEmpty()) {
            showAlert("Please enter your email");
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(resetTask -> {
                    if (resetTask.isSuccessful()) {
                        showAlert("If this email is registered, a password reset link has been sent.");

                        showLoginView();
                    } else {
                        showAlert("Failed to send reset email. Please check the email address.");
                    }
                });
    }

    private void goToRegister() {
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
    }
}