package com.tanim.ccepedia;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ADMBatchLinkFragment extends Fragment {

    private Spinner spinnerGender;
    private EditText editTitle, editUrl;
    private Button btnAdd, btnUpdate, btnDelete;
    private ListView listViewLinks;

    private FirebaseFirestore db;
    private ArrayList<BatchLink> batchLinks = new ArrayList<>();
    private ArrayAdapter<String> listAdapter;

    private String selectedDocId = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_adm_batch_link, container, false);

        spinnerGender = view.findViewById(R.id.spinnerGender);
        editTitle = view.findViewById(R.id.editTitle);
        editUrl = view.findViewById(R.id.editUrl);
        btnAdd = view.findViewById(R.id.btnAdd);
        btnUpdate = view.findViewById(R.id.btnUpdate);
        btnDelete = view.findViewById(R.id.btnDelete);
        listViewLinks = view.findViewById(R.id.listViewLinks);

        // Setup Spinner dynamically
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"Male", "Female"}
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(spinnerAdapter);

        db = FirebaseFirestore.getInstance();

        loadBatchLinks();

        btnAdd.setOnClickListener(v -> addBatchLink());

        btnUpdate.setOnClickListener(v -> updateBatchLink());

        btnDelete.setOnClickListener(v -> deleteBatchLink());

        listViewLinks.setOnItemClickListener((parent, view1, position, id) -> {
            BatchLink selectedLink = batchLinks.get(position);
            selectedDocId = selectedLink.id;
            spinnerGender.setSelection(selectedLink.gender.equals("Male") ? 0 : 1);
            editTitle.setText(selectedLink.title);
            editUrl.setText(selectedLink.url);

            btnAdd.setVisibility(View.GONE);
            btnUpdate.setVisibility(View.VISIBLE);
            btnDelete.setVisibility(View.VISIBLE);
        });

        return view;
    }

    private void loadBatchLinks() {
        db.collection("batch_wise_links")
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
        String gender = spinnerGender.getSelectedItem().toString();
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

        db.collection("batch_wise_links")
                .add(data)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Link added", Toast.LENGTH_SHORT).show();
                    loadBatchLinks();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to add link", Toast.LENGTH_SHORT).show());
    }

    private void updateBatchLink() {
        if (selectedDocId == null) return;

        String gender = spinnerGender.getSelectedItem().toString();
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

        db.collection("batch_wise_links")
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

        db.collection("batch_wise_links")
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
        spinnerGender.setSelection(0);
        editTitle.setText("");
        editUrl.setText("");
        btnAdd.setVisibility(View.VISIBLE);
        btnUpdate.setVisibility(View.GONE);
        btnDelete.setVisibility(View.GONE);
    }

    // Helper class to store batch link data
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
