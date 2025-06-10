package com.tanim.ccepedia;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton, resetPasswordButton;
    private TextView registerTextView, forgotPasswordTextView;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String selectedTheme = HomeActivity.ThemePref.getTheme(this);

        // Apply the theme before calling super.onCreate()
        if (selectedTheme.equals(HomeActivity.ThemePref.THEME_BLUE)) {
            setTheme(R.style.Theme_CCEPedia_Blue);
        } else {
            setTheme(R.style.Theme_CCEPedia_Green);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Bind the UI elements
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        resetPasswordButton = findViewById(R.id.resetPasswordButton);
        registerTextView = findViewById(R.id.registerTextView);
        forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView);

        // Hide reset password button initially
        resetPasswordButton.setVisibility(View.GONE);
        // Hide password input field and register link when reset password is shown
        passwordEditText.setVisibility(View.VISIBLE);
        registerTextView.setVisibility(View.VISIBLE);

        // Login Button Click Listener
        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter both email and password", Toast.LENGTH_SHORT).show();
            } else {
                loginUser(email, password);
            }
        });

        // Register Link Click Listener
        registerTextView.setOnClickListener(v -> goToRegister());

        // Forgot Password Link Click Listener
        forgotPasswordTextView.setOnClickListener(v -> toggleLoginView());

        // Reset Password Button Click Listener
        resetPasswordButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter your email", Toast.LENGTH_SHORT).show();
            } else {
                resetPassword(email);
            }
        });
    }

    private void handleUserDocument(DocumentSnapshot snapshot) {
        if (snapshot.exists()) {
            // Populate Singleton with user data
            UserData user = UserData.getInstance();
            user.setId(snapshot.getString("id"));
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

            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        } else {
            showAlert("User data not found, contact the developer");
        }
    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            if (user.isEmailVerified()) {
                                String uid = user.getUid();

                                // Update "verified" field to true
                                FirebaseFirestore db = FirebaseFirestore.getInstance();
                                db.collection("users")
                                        .document(uid)
                                        .update("verified", true)
                                        .addOnSuccessListener(aVoid -> {
                                            // fetch user data
                                            db.collection("users")
                                                    .document(uid)
                                                    .get()
                                                    .addOnSuccessListener(this::handleUserDocument)
                                                    .addOnFailureListener(e ->
                                                            showAlert("Failed to fetch user data"));
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(LoginActivity.this, "Failed to update verification status", Toast.LENGTH_SHORT).show());

                            } else {
                                // Resend email verification
                                user.sendEmailVerification()
                                        .addOnSuccessListener(aVoid ->
                                                showAlert("Email not verified. A verification email has been sent."))
                                        .addOnFailureListener(e ->
                                                showAlert("Failed to resend verification email. Please try again."));
                            }
                        }
                    } else {
                        showAlert("Incorrect Email/Password");
                    }
                });
    }

    private void showAlert(String message) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Alert!")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }
    private void toggleLoginView() {
        CardView loginCardView = findViewById(R.id.loginCardView);
        loginCardView.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    // Toggle views after fade out
                    if (resetPasswordButton.getVisibility() == View.VISIBLE) {
                        showLoginView();
                    } else {
                        showResetPasswordView();
                    }

                    // Fade in
                    loginCardView.setAlpha(0f); // reset alpha
                    loginCardView.animate()
                            .alpha(1f)
                            .setDuration(200)
                            .start();
                })
                .start();
    }




    private void showLoginView() {
        resetPasswordButton.setVisibility(View.GONE);
        loginButton.setVisibility(View.VISIBLE);
        passwordEditText.setVisibility(View.VISIBLE); // Show password input field again
        registerTextView.setVisibility(View.VISIBLE);
        forgotPasswordTextView.setVisibility(View.VISIBLE); // Show forgot password link again
        forgotPasswordTextView.setText("Forgot Password?");
    }

    // Show Reset Password UI
    private void showResetPasswordView() {
        // Hide password input field and login button
        passwordEditText.setVisibility(View.GONE);
        loginButton.setVisibility(View.GONE);
        // Show reset password button
        resetPasswordButton.setVisibility(View.VISIBLE);
        // Hide register link
        registerTextView.setVisibility(View.GONE);
        //forgotPasswordTextView.setVisibility(View.GONE);
        forgotPasswordTextView.setText("Go to Login");
    }

    // Reset Password Functionality
    private void resetPassword(String email) {
        if (email.isEmpty()) {
            Toast.makeText(LoginActivity.this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if the email is registered
        mAuth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        {
                            // Send password reset email
                            mAuth.sendPasswordResetEmail(email)
                                    .addOnCompleteListener(resetTask -> {
                                        if (resetTask.isSuccessful()) {
                                            Toast.makeText(LoginActivity.this, "Password reset email sent", Toast.LENGTH_SHORT).show();
                                            // Switch back to login view after sending reset email
                                            showLoginView();
                                        } else {
                                            Toast.makeText(LoginActivity.this, "Failed to send reset email", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Failed to check email", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Go to Register Activity
    private void goToRegister() {
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
    }
}
