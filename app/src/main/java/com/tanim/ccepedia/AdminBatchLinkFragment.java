package com.tanim.ccepedia;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminBatchLinkFragment extends Fragment {

    private AutoCompleteTextView spinnerGender;
    private AutoCompleteTextView spinnerDepartment;
    private EditText editTitle, editUrl;
    private MaterialButton btnAdd, btnUpdate, btnDelete;
    private ListView listViewLinks;

    private FirebaseFirestore db;
    private DepartmentRepository departmentRepository;
    private ArrayList<BatchLink> batchLinks = new ArrayList<>();
    private ArrayAdapter<String> listAdapter;
    private List<String> departmentIdList = new ArrayList<>();

    private String selectedDocId = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_batch_link, container, false);

        spinnerGender = view.findViewById(R.id.spinnerGender);
        spinnerDepartment = view.findViewById(R.id.spinnerDepartment);
        editTitle = view.findViewById(R.id.editTitle);
        editUrl = view.findViewById(R.id.editUrl);
        btnAdd = view.findViewById(R.id.btnAdd);
        btnUpdate = view.findViewById(R.id.btnUpdate);
        btnDelete = view.findViewById(R.id.btnDelete);
        listViewLinks = view.findViewById(R.id.listViewLinks);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_item, new String[]{"Male", "Female"});
        spinnerGender.setAdapter(spinnerAdapter);

        db = FirebaseFirestore.getInstance();
        departmentRepository = new DepartmentRepository();

        loadDepartmentSpinner();

        spinnerDepartment.setOnItemClickListener((parent, view1, position, id) -> loadBatchLinks());
        spinnerGender.setOnItemClickListener((parent, view1, position, id) -> loadBatchLinks());

        btnAdd.setOnClickListener(v -> addBatchLink());
        btnUpdate.setOnClickListener(v -> updateBatchLink());
        btnDelete.setOnClickListener(v -> deleteBatchLink());

        listViewLinks.setOnItemClickListener((parent, view1, position, id) -> {
            BatchLink selectedLink = batchLinks.get(position);
            selectedDocId = selectedLink.id;

            String linkGender = selectedLink.gender != null ? selectedLink.gender.trim() : "";

            if (linkGender.equalsIgnoreCase("Male")) {
                spinnerGender.setText("Male", false);
            } else {
                spinnerGender.setText("Female", false);
            }

            editTitle.setText(selectedLink.title);
            editUrl.setText(selectedLink.url);

            btnAdd.setVisibility(View.GONE);
            btnUpdate.setVisibility(View.VISIBLE);
            btnDelete.setVisibility(View.VISIBLE);
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

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            requireContext(),
                            R.layout.dropdown_item,
                            displayNames
                    );
                    spinnerDepartment.setAdapter(adapter);

                    if(displayNames.contains("CCE")) {
                        spinnerDepartment.setText("CCE", false);
                        loadBatchLinks();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load departments.", Toast.LENGTH_SHORT).show();
                });
    }

    private CollectionReference getCollectionRef() {
        String selectedDeptCode = spinnerDepartment.getText().toString();

        if (selectedDeptCode.isEmpty()) {
            Toast.makeText(getContext(), "Please select a department.", Toast.LENGTH_SHORT).show();
            return null;
        }

        CollectionReference collectionRef;

        if (selectedDeptCode.equalsIgnoreCase("CCE")) {
            collectionRef = db.collection("batch_wise_links");
        } else {
            String deptDocumentId = "dept_" + selectedDeptCode.toLowerCase();

            collectionRef = db.collection("departments")
                    .document(deptDocumentId)
                    .collection("batch_wise_links");
        }
        return collectionRef;
    }

    private void loadBatchLinks() {
        CollectionReference collectionRef = getCollectionRef();
        if (collectionRef == null) {
            batchLinks.clear();
            if(listAdapter != null) listAdapter.notifyDataSetChanged();
            return;
        }

        collectionRef
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    batchLinks.clear();
                    ArrayList<String> titles = new ArrayList<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String gender = doc.getString("gender");
                        String title = doc.getString("title");
                        String url = doc.getString("url");
                        batchLinks.add(new BatchLink(doc.getId(), gender, title, url));
                        titles.add(title + " (" + gender + ")");
                    }

                    listAdapter = new ArrayAdapter<>(getContext(),
                            android.R.layout.simple_list_item_1,
                            titles);
                    listViewLinks.setAdapter(listAdapter);

                    resetForm();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to load links.", Toast.LENGTH_SHORT).show());
    }

    private void addBatchLink() {
        CollectionReference collectionRef = getCollectionRef();
        if (collectionRef == null) return;

        String gender = spinnerGender.getText().toString().trim().toLowerCase();
        String title = editTitle.getText().toString().trim();
        String url = editUrl.getText().toString().trim();

        if (title.isEmpty() || url.isEmpty()) {
            Toast.makeText(getContext(), "Please fill title and url", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("gender", gender);
        data.put("title", title);
        data.put("url", url);

        collectionRef
                .add(data)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Link added", Toast.LENGTH_SHORT).show();
                    loadBatchLinks();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to add link", Toast.LENGTH_SHORT).show());
    }

    private void updateBatchLink() {
        if (selectedDocId == null) return;

        CollectionReference collectionRef = getCollectionRef();
        if (collectionRef == null) return;

        String gender = spinnerGender.getText().toString().trim().toLowerCase();
        String title = editTitle.getText().toString().trim();
        String url = editUrl.getText().toString().trim();

        if (title.isEmpty() || url.isEmpty()) {
            Toast.makeText(getContext(), "Please fill title and url", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("gender", gender);
        data.put("title", title);
        data.put("url", url);

        collectionRef
                .document(selectedDocId)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Link updated", Toast.LENGTH_SHORT).show();
                    loadBatchLinks();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update link", Toast.LENGTH_SHORT).show());
    }

    private void deleteBatchLink() {
        if (selectedDocId == null) return;

        CollectionReference collectionRef = getCollectionRef();
        if (collectionRef == null) return;

        collectionRef
                .document(selectedDocId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Link deleted", Toast.LENGTH_SHORT).show();
                    loadBatchLinks();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to delete link", Toast.LENGTH_SHORT).show());
    }

    private void resetForm() {
        selectedDocId = null;
        editTitle.setText("");
        editUrl.setText("");
        btnAdd.setVisibility(View.VISIBLE);
        btnUpdate.setVisibility(View.GONE);
        btnDelete.setVisibility(View.GONE);
    }

    private static class BatchLink {
        String id;
        String gender;
        String title;
        String url;

        BatchLink(String id, String gender, String title, String url) {
            this.id = id;
            this.gender = gender;
            this.title = title;
            this.url = url;
        }
    }
}