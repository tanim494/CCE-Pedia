package com.tanim.ccepedia;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ADMDriveLinksFragment extends Fragment {

    private EditText editDriveTitle, editDriveUrl;
    private Button btnAddDriveLink;
    private RecyclerView rvDriveLinks;

    private FirebaseFirestore firestore;
    private CollectionReference driveLinksRef;

    private DriveLinkItemAdapter adapter;
    private List<DriveLinkItem> driveLinkList;

    private String editingDocId = null; // null means adding new

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_adm_drive_links, container, false);

        editDriveTitle = view.findViewById(R.id.editDriveTitle);
        editDriveUrl = view.findViewById(R.id.editDriveUrl);
        btnAddDriveLink = view.findViewById(R.id.btnAddDriveLink);
        rvDriveLinks = view.findViewById(R.id.rvDriveLinks);

        firestore = FirebaseFirestore.getInstance();
        driveLinksRef = firestore.collection("drive_links");

        driveLinkList = new ArrayList<>();
        adapter = new DriveLinkItemAdapter(driveLinkList, this::onDriveLinkClicked, this::onDriveLinkDeleteClicked);

        rvDriveLinks.setLayoutManager(new LinearLayoutManager(getContext()));
        rvDriveLinks.setAdapter(adapter);

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

        fetchDriveLinks();

        return view;
    }

    private void fetchDriveLinks() {
        driveLinksRef.get()
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
        DriveLinkItem newLink = new DriveLinkItem(title, url);
        driveLinksRef.add(newLink)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Drive link added.", Toast.LENGTH_SHORT).show();
                    clearInput();
                    fetchDriveLinks();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to add drive link.", Toast.LENGTH_SHORT).show());
    }

    private void updateDriveLink(String docId, String title, String url) {
        driveLinksRef.document(docId)
                .update("title", title, "url", url)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Drive link updated.", Toast.LENGTH_SHORT).show();
                    clearInput();
                    editingDocId = null;
                    btnAddDriveLink.setText("Add Drive Link");
                    fetchDriveLinks();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to update drive link.", Toast.LENGTH_SHORT).show());
    }

    private void clearInput() {
        editDriveTitle.setText("");
        editDriveUrl.setText("");
        btnAddDriveLink.setText("Add Drive Link");
    }

    // Called when a drive link item is clicked to edit
    private void onDriveLinkClicked(DriveLinkItem driveLink) {
        editingDocId = driveLink.getId();
        editDriveTitle.setText(driveLink.getTitle());
        editDriveUrl.setText(driveLink.getUrl());
        btnAddDriveLink.setText("Update Link");
    }

    // Called when delete icon/button is clicked in item
    private void onDriveLinkDeleteClicked(DriveLinkItem driveLink) {
        driveLinksRef.document(driveLink.getId())
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
}
