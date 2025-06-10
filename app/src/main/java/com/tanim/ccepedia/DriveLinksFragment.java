package com.tanim.ccepedia;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class DriveLinksFragment extends Fragment {

    private RecyclerView recyclerView;
    private DriveLinkAdapter adapter;
    private List<DriveLink> driveLinks;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_drive_links, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewDriveLinks);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        driveLinks = new ArrayList<>();
        adapter = new DriveLinkAdapter(driveLinks);
        recyclerView.setAdapter(adapter);

        fetchDriveLinksFromDB();

        return view;
    }

    private void fetchDriveLinksFromDB() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("drive_links")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    driveLinks.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String title = doc.getString("title");
                        String url = doc.getString("url");
                        driveLinks.add(new DriveLink(title, url));
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to load links", Toast.LENGTH_SHORT).show()
                );
    }
}
