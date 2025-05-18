package com.tanim.ccepedia;

import android.os.Bundle;
import android.os.Handler;
import android.util.Patterns;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {

    private EditText nameEditText, idEditText, phoneEditText, emailEditText, passwordEditText;
    private Spinner genderSpinner, semesterSpinner;
    private Button registerButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize Views
        nameEditText = findViewById(R.id.nameEditText);
        idEditText = findViewById(R.id.idEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        genderSpinner = findViewById(R.id.genderSpinner);
        semesterSpinner = findViewById(R.id.semesterSpinner);
        registerButton = findViewById(R.id.registerButton);

        // Populate Semester Spinner
        ArrayAdapter<String> semesterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        semesterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (int i = 1; i <= 8; i++) {
            semesterAdapter.add(String.valueOf(i));
        }
        semesterSpinner.setAdapter(semesterAdapter);

        // Populate Gender Spinner
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderAdapter.add("Male");
        genderAdapter.add("Female");
        genderSpinner.setAdapter(genderAdapter);

        // Register Button Click Listener
        registerButton.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String name = nameEditText.getText().toString().trim();
        String id = idEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String gender = genderSpinner.getSelectedItem().toString();
        String semester = semesterSpinner.getSelectedItem().toString();

        if (name.isEmpty() || id.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!phone.matches("\\d{11}")) {
            Toast.makeText(this, "Invalid phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isStrongPassword(password)) {
            Toast.makeText(this, "Password must be at least 6 characters long and include a-z, and number.", Toast.LENGTH_LONG).show();
            return;
        }

        registerButton.setEnabled(false);

        mAuth.fetchSignInMethodsForEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                boolean emailExists = task.getResult().getSignInMethods() != null && !task.getResult().getSignInMethods().isEmpty();
                if (emailExists) {
                    Toast.makeText(this, "Email already in use", Toast.LENGTH_SHORT).show();
                    registerButton.setEnabled(true);
                } else {
                    mAuth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(this, authTask -> {
                                if (authTask.isSuccessful()) {
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    if (user != null) {
                                        user.sendEmailVerification()
                                                .addOnCompleteListener(emailTask -> {
                                                    if (emailTask.isSuccessful()) {
                                                        User newUser = new User(name, id, phone, email, gender, semester);
                                                        db.collection("users")
                                                                .document(user.getUid())
                                                                .set(newUser)
                                                                .addOnSuccessListener(aVoid -> {
                                                                    Toast.makeText(this, "Registration successful. Please verify your email.", Toast.LENGTH_LONG).show();
                                                                    mAuth.signOut();
                                                                    new Handler().postDelayed(this::finish, 1500);
                                                                })
                                                                .addOnFailureListener(e -> {
                                                                    Toast.makeText(this, "Error saving user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                                    registerButton.setEnabled(true);
                                                                });
                                                    } else {
                                                        Toast.makeText(this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                                                        registerButton.setEnabled(true);
                                                    }
                                                });
                                    }
                                } else {
                                    Toast.makeText(this, authTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    registerButton.setEnabled(true);
                                }
                            });
                }
            } else {
                Toast.makeText(this, "Failed to check email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                registerButton.setEnabled(true);
            }
        });
    }

    private boolean isStrongPassword(String password) {
        return password.length() >= 6 &&
                password.matches(".*[a-z].*") &&
                password.matches(".*\\d.*");
    }

    // User class to represent the user data
    public static class User {
        private String name;
        private String id;
        private String phone;
        private String email;
        private String gender;
        private String semester;

        public User(String name, String id, String phone, String email, String gender, String semester) {
            this.name = name;
            this.id = id;
            this.phone = phone;
            this.email = email;
            this.gender = gender;
            this.semester = semester;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }

        public String getSemester() { return semester; }
        public void setSemester(String semester) { this.semester = semester; }
    }
}
