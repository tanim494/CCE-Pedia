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
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ProfileFragment extends Fragment {

    private TextView nameText, idText, emailText, phoneText, semesterText, viewCountText, departmentText;
    private EditText nameEdit, idEdit, phoneEdit;
    private AutoCompleteTextView semesterEdit, departmentEdit;
    private Button editButton, logoutButton, saveButton;
    private View academicInfoCard, contactInfoCard, statsCard, editModeLayout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DepartmentRepository departmentRepository;
    private List<String> departmentDisplayList = new ArrayList<>();


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        nameText = view.findViewById(R.id.nameText);
        idText = view.findViewById(R.id.idText);
        emailText = view.findViewById(R.id.emailText);
        phoneText = view.findViewById(R.id.phoneText);
        semesterText = view.findViewById(R.id.semesterText);
        viewCountText = view.findViewById(R.id.viewCountText);
        departmentText = view.findViewById(R.id.departmentText);

        nameEdit = view.findViewById(R.id.nameEdit);
        idEdit = view.findViewById(R.id.idEdit);
        phoneEdit = view.findViewById(R.id.phoneEdit);
        semesterEdit = view.findViewById(R.id.semesterEdit);
        departmentEdit = view.findViewById(R.id.departmentEdit);

        editButton = view.findViewById(R.id.editButton);
        logoutButton = view.findViewById(R.id.logoutButton);
        saveButton = view.findViewById(R.id.saveButton);

        academicInfoCard = view.findViewById(R.id.academicInfoCard);
        contactInfoCard = view.findViewById(R.id.contactInfoCard);
        statsCard = view.findViewById(R.id.statsCard);
        editModeLayout = view.findViewById(R.id.editModeLayout);

        editButton.setOnClickListener(v -> switchToEditMode());
        saveButton.setOnClickListener(v -> saveUserData());
        logoutButton.setOnClickListener(v -> logoutUser());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        departmentRepository = new DepartmentRepository();

        loadUserData();

        boolean canViewMetrics = UserData.getInstance().getRole() != null &&
                (UserData.getInstance().getRole().equalsIgnoreCase("admin") ||
                        UserData.getInstance().getRole().equalsIgnoreCase("moderator"));

        if (canViewMetrics) {
            statsCard.setVisibility(View.VISIBLE);
            long views = UserData.getInstance().getViewCount();
            viewCountText.setText("Total File Views: " + String.valueOf(views));
        } else {
            statsCard.setVisibility(View.GONE);
        }

        return view;
    }

    private void loadUserData() {
        UserData user = UserData.getInstance();

        nameText.setText(user.getName());
        idText.setText("ID: " + user.getStudentId());
        emailText.setText(user.getEmail());
        phoneText.setText("Phone: " + user.getPhone());
        semesterText.setText("Semester: " + user.getSemester());
        departmentText.setText("Dept: " + user.getDepartmentName());

        nameEdit.setText(user.getName());
        idEdit.setText(user.getStudentId());
        phoneEdit.setText(user.getPhone());
        semesterEdit.setText(user.getSemester());
        departmentEdit.setText(user.getDepartmentName());
    }

    private void switchToEditMode() {
        academicInfoCard.setVisibility(View.GONE);
        contactInfoCard.setVisibility(View.GONE);
        statsCard.setVisibility(View.GONE);
        logoutButton.setVisibility(View.GONE);
        editModeLayout.setVisibility(View.VISIBLE);

        String[] SEMESTERS = new String[] {"1", "2", "3", "4", "5", "6", "7", "8", "Outgoing"};
        ArrayAdapter<String> semesterAdapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_item, SEMESTERS);
        semesterEdit.setAdapter(semesterAdapter);
        semesterEdit.setOnClickListener(v -> semesterEdit.showDropDown());

        departmentRepository.fetchAllDepartmentIds()
                .addOnSuccessListener(ids -> {
                    departmentDisplayList = ids.stream()
                            .map(id -> id.replace("dept_", "").toUpperCase())
                            .collect(Collectors.toList());

                    ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(
                            requireContext(), R.layout.dropdown_item, departmentDisplayList);
                    departmentEdit.setAdapter(deptAdapter);

                    String currentDept = UserData.getInstance().getDepartmentName();
                    if (departmentDisplayList.contains(currentDept)) {
                        departmentEdit.setText(currentDept, false);
                    }

                    departmentEdit.setOnClickListener(v -> departmentEdit.showDropDown());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load departments.", Toast.LENGTH_SHORT).show();
                });


        editButton.setVisibility(View.GONE);
        saveButton.setVisibility(View.VISIBLE);
    }


    private void switchToViewMode() {
        editModeLayout.setVisibility(View.GONE);
        academicInfoCard.setVisibility(View.VISIBLE);
        contactInfoCard.setVisibility(View.VISIBLE);
        logoutButton.setVisibility(View.VISIBLE);

        boolean canViewMetrics = UserData.getInstance().getRole() != null &&
                (UserData.getInstance().getRole().equalsIgnoreCase("admin") ||
                        UserData.getInstance().getRole().equalsIgnoreCase("moderator"));

        if (canViewMetrics) {
            statsCard.setVisibility(View.VISIBLE);
        }

        editButton.setVisibility(View.VISIBLE);
        saveButton.setVisibility(View.GONE);
    }

    private boolean isValidStudentId(String id) {
        if (id == null) return false;
        String idPattern = "^[a-zA-Z]{1,3}[0-9]{5,8}$";
        Pattern pattern = Pattern.compile(idPattern);
        Matcher matcher = pattern.matcher(id);
        return matcher.matches();
    }

    private void saveUserData() {
        String newName = nameEdit.getText().toString().trim();
        String rawId = idEdit.getText().toString().trim();
        String newPhone = phoneEdit.getText().toString().trim();
        String newSemester = semesterEdit.getText().toString().trim();
        String newDepartmentCode = departmentEdit.getText().toString().trim();

        if (newName.isEmpty() || rawId.isEmpty() || newSemester.isEmpty() || newDepartmentCode.isEmpty()) {
            showAlert("Please fill all required fields");
            return;
        }

        String sanitizedId = rawId.replaceAll("[^a-zA-Z0-9]", "");
        if (!isValidStudentId(sanitizedId)) {
            showAlert("Invalid Student ID. Insert your correct student ID (e.g., E221013 or C221013).");
            return;
        }

        String finalId = sanitizedId.toUpperCase();

        if (!departmentDisplayList.contains(newDepartmentCode)) {
            showAlert("Please select a valid department from the list.");
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid)
                .update("name", newName,
                        "id", finalId,
                        "phone", newPhone,
                        "semester", newSemester,
                        "department", newDepartmentCode)
                .addOnSuccessListener(aVoid -> {
                    UserData user = UserData.getInstance();
                    user.setName(newName);
                    user.setStudentId(finalId);
                    user.setPhone(newPhone);
                    user.setSemester(newSemester);
                    user.setDepartmentName(newDepartmentCode);

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
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    private void showAlert(String message) {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle("Alert!")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }
}