package com.tanim.ccepedia;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private TextView nameText, idText, emailText, phoneText, semesterText;
    private TextInputLayout nameInputLayout, idInputLayout, phoneInputLayout, semesterInputLayout;
    private EditText nameEdit, idEdit, phoneEdit, semesterEdit;
    private Button editButton, logoutButton, saveButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize views
        nameText = view.findViewById(R.id.nameText);
        idText = view.findViewById(R.id.idText);
        emailText = view.findViewById(R.id.emailText);
        phoneText = view.findViewById(R.id.phoneText);
        semesterText = view.findViewById(R.id.semesterText);

        nameInputLayout = view.findViewById(R.id.nameInputLayout);
        idInputLayout = view.findViewById(R.id.idInputLayout);
        phoneInputLayout = view.findViewById(R.id.phoneInputLayout);
        semesterInputLayout = view.findViewById(R.id.semesterInputLayout);


        nameEdit = view.findViewById(R.id.nameEdit);
        idEdit = view.findViewById(R.id.idEdit);
        phoneEdit = view.findViewById(R.id.phoneEdit);
        semesterEdit = view.findViewById(R.id.semesterEdit);

        editButton = view.findViewById(R.id.editButton);
        logoutButton = view.findViewById(R.id.logoutButton);
        saveButton = view.findViewById(R.id.saveButton);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadUserData();

        editButton.setOnClickListener(v -> switchToEditMode());
        saveButton.setOnClickListener(v -> saveUserData());
        logoutButton.setOnClickListener(v -> logoutUser());

        return view;
    }

    private void loadUserData() {
        UserData user = UserData.getInstance();

        nameText.setText("Name: " + user.getName());
        idText.setText("ID: " + user.getId());
        emailText.setText("Email: " + user.getEmail());
        phoneText.setText("Phone: " + user.getPhone());
        semesterText.setText("Semester: " + user.getSemester());

        nameEdit.setText(user.getName());
        idEdit.setText(user.getId());
        phoneEdit.setText(user.getPhone());
        semesterEdit.setText(user.getSemester());
    }

    private void switchToEditMode() {
        nameText.setVisibility(View.GONE);
        idText.setVisibility(View.GONE);
        phoneText.setVisibility(View.GONE);
        semesterText.setVisibility(View.GONE);

        nameInputLayout.setVisibility(View.VISIBLE);
        idInputLayout.setVisibility(View.VISIBLE);
        phoneInputLayout.setVisibility(View.VISIBLE);
        semesterInputLayout.setVisibility(View.VISIBLE);

        editButton.setVisibility(View.GONE);
        saveButton.setVisibility(View.VISIBLE);
    }


    private void switchToViewMode() {
        nameInputLayout.setVisibility(View.GONE);
        idInputLayout.setVisibility(View.GONE);
        phoneInputLayout.setVisibility(View.GONE);
        semesterInputLayout.setVisibility(View.GONE);

        nameText.setVisibility(View.VISIBLE);
        idText.setVisibility(View.VISIBLE);
        phoneText.setVisibility(View.VISIBLE);
        semesterText.setVisibility(View.VISIBLE);

        editButton.setVisibility(View.VISIBLE);
        saveButton.setVisibility(View.GONE);
    }


    private void saveUserData() {
        String newName = nameEdit.getText().toString().trim();
        String newId = idEdit.getText().toString().trim();
        String newPhone = phoneEdit.getText().toString().trim();
        String newSemester = semesterEdit.getText().toString().trim();

        if (newName.isEmpty() || newId.isEmpty() || newPhone.isEmpty() || newSemester.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPhone.matches("\\d{11}")) {
            Toast.makeText(getContext(), "Invalid phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid)
                .update("name", newName,
                        "id", newId,
                        "phone", newPhone,
                        "semester", newSemester)
                .addOnSuccessListener(aVoid -> {
                    UserData user = UserData.getInstance();
                    user.setName(newName);
                    user.setId(newId);
                    user.setPhone(newPhone);
                    user.setSemester(newSemester);

                    loadUserData();
                    switchToViewMode();
                    Toast.makeText(getContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void logoutUser() {
        mAuth.signOut();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();
    }
}
