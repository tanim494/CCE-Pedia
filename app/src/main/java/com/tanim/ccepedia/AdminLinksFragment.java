package com.tanim.ccepedia;

import android.os.Bundle;
import android.text.TextUtils;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdminLinksFragment extends Fragment {

    private EditText editDriveTitle, editDriveUrl;
    private MaterialButton btnAddDriveLink;
    private RecyclerView rvDriveLinks;
    private AutoCompleteTextView spinnerDepartment;

    private FirebaseFirestore firestore;
    private DepartmentRepository departmentRepository;
    private DriveLinkItemAdapter adapter;
    private List<DriveLinkItem> driveLinkList;
    private List<String> departmentIdList = new ArrayList<>();
    private String editingDocId = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_admin_drive_links, container, false);

        editDriveTitle = view.findViewById(R.id.editDriveTitle);
        editDriveUrl = view.findViewById(R.id.editDriveUrl);
        btnAddDriveLink = view.findViewById(R.id.btnAddDriveLink);
        rvDriveLinks = view.findViewById(R.id.rvDriveLinks);
        spinnerDepartment = view.findViewById(R.id.spinnerDepartment);

        firestore = FirebaseFirestore.getInstance();
        departmentRepository = new DepartmentRepository();

        driveLinkList = new ArrayList<>();
        adapter = new DriveLinkItemAdapter(driveLinkList, this::onDriveLinkClicked, this::showDeleteConfirmationDialog);

        rvDriveLinks.setLayoutManager(new LinearLayoutManager(getContext()));
        rvDriveLinks.setAdapter(adapter);

        loadDepartmentSpinner();

        spinnerDepartment.setOnItemClickListener((parent, view1, position, id) -> {
            fetchDriveLinks();
            clearInput();
        });

        btnAddDriveLink.setOnClickListener(v -> {
            String title = editDriveTitle.getText().toString().trim();
            String url = editDriveUrl.getText().toString().trim();

            if (TextUtils.isEmpty(title)) {
                editDriveTitle.setError("Title required");
                return;
            }
            if (TextUtils.isEmpty(url)) {
                editDriveUrl.setError("URL required");
                return;
            }

            if (editingDocId == null) {
                addDriveLink(title, url);
            } else {
                updateDriveLink(editingDocId, title, url);
            }
        });

        return view;
    }

    private void loadDepartmentSpinner() {
        departmentRepository.fetchAllDepartmentIds()
                .addOnSuccessListener(ids -> {
                    departmentIdList = ids;

                    List<String> displayNames = ids.stream()
                            .map(id -> id.replace("dept_", "").toUpperCase())
                            .collect(Collectors.toList());

                    displayNames.add(0, "Select Department");

                    ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(
                            requireContext(),
                            R.layout.dropdown_item,
                            displayNames
                    );
                    spinnerDepartment.setAdapter(deptAdapter);

                    String currentDept = UserData.getInstance().getDepartmentName();
                    if (displayNames.contains(currentDept)) {
                        spinnerDepartment.setText(currentDept, false);
                        fetchDriveLinks();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load departments.", Toast.LENGTH_SHORT).show();
                });
    }

    private CollectionReference getCollectionRef() {
        String selectedDeptCode = spinnerDepartment.getText().toString();

        if (selectedDeptCode.equals("Select Department") || selectedDeptCode.isEmpty()) {
            Toast.makeText(getContext(), "Please select a department.", Toast.LENGTH_SHORT).show();
            return null;
        }

        if (selectedDeptCode.equalsIgnoreCase("CCE")) {
            return firestore.collection("drive_links");
        } else {
            String deptDocumentId = "dept_" + selectedDeptCode.toLowerCase();

            return firestore.collection("departments")
                    .document(deptDocumentId)
                    .collection("drive_links");
        }
    }


    private void fetchDriveLinks() {
        CollectionReference collectionRef = getCollectionRef();
        if (collectionRef == null) {
            driveLinkList.clear();
            adapter.notifyDataSetChanged();
            return;
        }

        collectionRef.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    driveLinkList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        DriveLinkItem driveLink = doc.toObject(DriveLinkItem.class);
                        if (driveLink != null) {
                            driveLink.setId(doc.getId());
                            driveLinkList.add(driveLink);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to load drive links.", Toast.LENGTH_SHORT).show());
    }

    private void addDriveLink(String title, String url) {
        CollectionReference collectionRef = getCollectionRef();
        if (collectionRef == null) return;

        DriveLinkItem newLink = new DriveLinkItem(title, url);
        collectionRef.add(newLink)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Drive link added.", Toast.LENGTH_SHORT).show();
                    clearInput();
                    fetchDriveLinks();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to add drive link.", Toast.LENGTH_SHORT).show());
    }

    private void updateDriveLink(String docId, String title, String url) {
        CollectionReference collectionRef = getCollectionRef();
        if (collectionRef == null) return;

        collectionRef.document(docId)
                .update("title", title, "url", url)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Drive link updated.", Toast.LENGTH_SHORT).show();
                    clearInput();
                    editingDocId = null;
                    btnAddDriveLink.setText("Add Link");
                    fetchDriveLinks();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to update drive link.", Toast.LENGTH_SHORT).show());
    }

    private void showDeleteConfirmationDialog(DriveLinkItem driveLink) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete the link:\n\"" + driveLink.getTitle() + "\"?\nThis action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    onDriveLinkDeleteConfirmed(driveLink);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void onDriveLinkDeleteConfirmed(DriveLinkItem driveLink) {
        CollectionReference collectionRef = getCollectionRef();
        if (collectionRef == null) return;

        collectionRef.document(driveLink.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Drive link deleted.", Toast.LENGTH_SHORT).show();
                    if (editingDocId != null && editingDocId.equals(driveLink.getId())) {
                        clearInput();
                        editingDocId = null;
                    }
                    fetchDriveLinks();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to delete drive link.", Toast.LENGTH_SHORT).show());
    }

    private void clearInput() {
        editDriveTitle.setText("");
        editDriveUrl.setText("");
        btnAddDriveLink.setText("Add Link");
    }

    private void onDriveLinkClicked(DriveLinkItem driveLink) {
        editingDocId = driveLink.getId();
        editDriveTitle.setText(driveLink.getTitle());
        editDriveUrl.setText(driveLink.getUrl());
        btnAddDriveLink.setText("Update Link");
    }
}