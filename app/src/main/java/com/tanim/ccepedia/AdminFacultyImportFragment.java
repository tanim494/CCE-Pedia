package com.tanim.ccepedia;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminFacultyImportFragment extends Fragment {

    private AutoCompleteTextView spinnerDepartment;
    private EditText editFacultyData;
    private MaterialButton btnRunImport;

    private FirebaseFirestore db;
    private DepartmentRepository departmentRepository;
    private List<String> departmentDisplayList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_faculty_import, container, false);

        spinnerDepartment = view.findViewById(R.id.spinnerDepartment);
        editFacultyData = view.findViewById(R.id.editFacultyData);
        btnRunImport = view.findViewById(R.id.btnRunImport);

        db = FirebaseFirestore.getInstance();
        departmentRepository = new DepartmentRepository();

        loadDepartmentSpinner();

        btnRunImport.setOnClickListener(v -> confirmAndRunImport());

        return view;
    }

    private void loadDepartmentSpinner() {
        departmentRepository.fetchAllDepartmentIds()
                .addOnSuccessListener(ids -> {
                    departmentDisplayList = ids.stream()
                            .map(id -> id.replace("dept_", "").toUpperCase())
                            .collect(Collectors.toList());

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            getContext(), R.layout.dropdown_item, departmentDisplayList);
                    spinnerDepartment.setAdapter(adapter);
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to load departments.", Toast.LENGTH_SHORT).show());
    }

    private CollectionReference getTargetCollection(String deptCode) {
        if (deptCode.equalsIgnoreCase("CCE")) {
            return db.collection("faculties");
        } else {
            String deptDocumentId = "dept_" + deptCode.toLowerCase();
            return db.collection("departments")
                    .document(deptDocumentId)
                    .collection("faculties");
        }
    }

    private void confirmAndRunImport() {
        String data = editFacultyData.getText().toString().trim();
        String selectedDeptCode = spinnerDepartment.getText().toString();

        if (data.isEmpty()) {
            Toast.makeText(getContext(), "Paste data into the text box first.", Toast.LENGTH_SHORT).show();
            return;
        }

        CollectionReference targetCollection = getTargetCollection(selectedDeptCode);
        if (targetCollection == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Confirm Batch Import")
                .setMessage("This will add all faculty members in the text box to the '" + selectedDeptCode + "' department. Are you sure?")
                .setPositiveButton("Yes, Import Now", (dialog, which) -> {
                    btnRunImport.setEnabled(false);
                    Toast.makeText(getContext(), "Checking existing data...", Toast.LENGTH_SHORT).show();

                    targetCollection.get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                int currentCount = queryDocumentSnapshots.size();
                                int startingOrder = currentCount + 1;
                                runImportScript(targetCollection, data, startingOrder, selectedDeptCode);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Failed to check existing count. Import aborted.", Toast.LENGTH_SHORT).show();
                                btnRunImport.setEnabled(true);
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void runImportScript(CollectionReference targetCollection, String data, int startingOrder, String selectedDeptCode) {
        WriteBatch batch = db.batch();

        String[] lines = data.split("\n");
        List<Map<String, Object>> facultyList = new ArrayList<>();
        int order = startingOrder;

        for (String line : lines) {
            if (line.trim().isEmpty()) continue;

            String[] fields = line.split(",");
            if (fields.length != 4) {
                Toast.makeText(getContext(), "Import Failed: A line has incorrect format. Expected 4 fields (Name,Designation,Phone,PhotoUrl), found " + fields.length, Toast.LENGTH_LONG).show();
                btnRunImport.setEnabled(true);
                return;
            }

            try {
                Map<String, Object> faculty = new HashMap<>();
                faculty.put("name", fields[0].trim());
                faculty.put("designation", fields[1].trim());
                String phone = fields[2].trim();
                faculty.put("phone", phone.equalsIgnoreCase("N/A") ? null : phone);
                faculty.put("photoUrl", fields[3].trim());
                faculty.put("order", order);

                facultyList.add(faculty);
                order++;

            } catch (Exception e) {
                Toast.makeText(getContext(), "Import Failed: Error processing line: " + line, Toast.LENGTH_LONG).show();
                btnRunImport.setEnabled(true);
                return;
            }
        }

        if (facultyList.isEmpty()) {
            Toast.makeText(getContext(), "No valid faculty data was found to import.", Toast.LENGTH_SHORT).show();
            btnRunImport.setEnabled(true);
            return;
        }

        for (Map<String, Object> faculty : facultyList) {
            DocumentReference docRef = targetCollection.document();
            batch.set(docRef, faculty);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "SUCCESS: " + facultyList.size() + " new faculty members added to " + selectedDeptCode, Toast.LENGTH_LONG).show();
                    editFacultyData.setText("");
                    btnRunImport.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "IMPORT FAILED: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnRunImport.setEnabled(true);
                });
    }
}