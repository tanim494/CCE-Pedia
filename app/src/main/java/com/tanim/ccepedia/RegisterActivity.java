package com.tanim.ccepedia;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Patterns;
import android.widget.ArrayAdapter;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import android.widget.AutoCompleteTextView;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText nameEditText, idEditText, phoneEditText, emailEditText, passwordEditText;
    private AutoCompleteTextView genderAutoComplete, semesterAutoComplete;
    private AutoCompleteTextView departmentAutoComplete;
    private Button registerButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DepartmentRepository departmentRepository;
    private List<String> departmentIdList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        departmentRepository = new DepartmentRepository();

        nameEditText = findViewById(R.id.nameEditText);
        idEditText = findViewById(R.id.idEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);

        genderAutoComplete = findViewById(R.id.genderSpinner);
        semesterAutoComplete = findViewById(R.id.semesterSpinner);
        departmentAutoComplete = findViewById(R.id.departmentSpinner);

        registerButton = findViewById(R.id.registerButton);

        String[] semesters = new String[10];
        semesters[0] = "Select Semester";
        for (int i = 1; i <= 8; i++) {
            semesters[i] = String.valueOf(i);
        }
        semesters[9] = "Outgoing";

        ArrayAdapter<String> semesterAdapter = new ArrayAdapter<>(this, R.layout.dropdown_item, semesters);
        semesterAutoComplete.setAdapter(semesterAdapter);

        String[] genders = {"Select Gender", "Male", "Female"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, R.layout.dropdown_item, genders);
        genderAutoComplete.setAdapter(genderAdapter);

        loadDepartmentSpinner();

        registerButton.setOnClickListener(v -> registerUser());
    }

    private void loadDepartmentSpinner() {
        departmentRepository.fetchAllDepartmentIds()
                .addOnSuccessListener(ids -> {
                    departmentIdList.add("Select Department");
                    departmentIdList.addAll(ids);

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            R.layout.dropdown_item,
                            departmentIdList
                    );
                    departmentAutoComplete.setAdapter(adapter);

                    departmentAutoComplete.setText("Select Department", false);
                })
                .addOnFailureListener(e -> {
                    showAlert("Error loading departments. Check network.");
                    registerButton.setEnabled(false);
                });
    }

    private boolean isValidStudentId(String id) {
        if (id == null) return false;
        String idPattern = "^[a-zA-Z]{1,3}[0-9]{5,8}$";
        Pattern pattern = Pattern.compile(idPattern);
        Matcher matcher = pattern.matcher(id);
        return matcher.matches();
    }

    private void registerUser() {
        final String name = nameEditText.getText().toString().trim();
        final String rawId = idEditText.getText().toString().trim();
        final String phone = phoneEditText.getText().toString().trim();
        final String email = emailEditText.getText().toString().trim();
        final String password = passwordEditText.getText().toString().trim();

        final String gender = genderAutoComplete.getText().toString();
        final String semester = semesterAutoComplete.getText().toString();
        final String departmentId = departmentAutoComplete.getText().toString();


        if (name.isEmpty() || rawId.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showAlert("Please fill in all required fields");
            return;
        }

        String sanitizedId = rawId.replaceAll("[^a-zA-Z0-9]", "");

        if (!isValidStudentId(sanitizedId)) {
            showAlert("Invalid Student ID. Insert your correct student ID (e.g., E221013 or C221013).");
            return;
        }

        final String finalId = sanitizedId.toUpperCase();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showAlert("Invalid email format");
            return;
        }

        if (!isStrongPassword(password)) {
            showAlert("Password must be at least 6 characters long.");
            return;
        }

        if (gender.equals("Select Gender") || gender.isEmpty() || semester.equals("Select Semester") || semester.isEmpty() || departmentId.equals("Select Department") || departmentId.isEmpty()) {
            showAlert("Please select Gender, Semester, and Department.");
            return;
        }

        String tempDeptCode = departmentId.replace("Select Department", "").trim();
        final String finalDeptCode;

        if (!tempDeptCode.isEmpty()) {
            finalDeptCode = tempDeptCode.replace("dept_", "").toUpperCase();
        } else {
            showAlert("Please select a valid Department.");
            return;
        }

        registerButton.setEnabled(false);

        // Create the account directly; a duplicate email is reported by Firebase as a
        // collision error below. (The old fetchSignInMethodsForEmail() pre-check is
        // deprecated and returns nothing when Email Enumeration Protection is enabled.)
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, authTask -> {
                    if (authTask.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user == null) {
                            showAlert("Registration failed. Please try again.");
                            registerButton.setEnabled(true);
                            return;
                        }
                        user.sendEmailVerification()
                                .addOnCompleteListener(emailTask -> {
                                    if (emailTask.isSuccessful()) {
                                        User newUser = new User(name, finalId, phone, email, gender, semester, finalDeptCode);
                                        db.collection("users")
                                                .document(user.getUid())
                                                .set(newUser)
                                                .addOnSuccessListener(aVoid -> {
                                                    AnalyticsHelper.logSignUp("email");
                                                    showAlert("Registration successful. Please verify your email.");
                                                    mAuth.signOut();
                                                    new Handler(Looper.getMainLooper()).postDelayed(this::finish, 1500);
                                                })
                                                .addOnFailureListener(e -> {
                                                    showAlert("Error saving user data: " + e.getMessage());
                                                    registerButton.setEnabled(true);
                                                });
                                    } else {
                                        showAlert("Failed to send verification email.");
                                        registerButton.setEnabled(true);
                                    }
                                });
                    } else {
                        Exception ex = authTask.getException();
                        if (ex instanceof FirebaseAuthUserCollisionException) {
                            showAlert("Email already in use");
                        } else {
                            showAlert(ex != null ? ex.getMessage() : "Registration failed. Please try again.");
                        }
                        registerButton.setEnabled(true);
                    }
                });
    }

    private boolean isStrongPassword(String password) {
        return password.length() >= 6;
    }

    private void showAlert(String message) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Alert!")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    public static class User {
        private String name;
        private String id;
        private String phone;
        private String email;
        private String gender;
        private String semester;
        private String department;

        public User() {
        }

        public User(String name, String id, String phone, String email, String gender, String semester, String department) {
            this.name = name;
            this.id = id;
            this.phone = phone;
            this.email = email;
            this.gender = gender;
            this.semester = semester;
            this.department = department;
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


        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }

    }
}