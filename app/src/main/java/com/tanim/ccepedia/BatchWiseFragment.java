package com.tanim.ccepedia;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class BatchWiseFragment extends Fragment {

    private static final String ARG_GENDER = "gender";
    private String gender;
    private RecyclerView recyclerView;
    private DriveLinkAdapter adapter;
    private List<DriveLink> linkList;
    private List<DriveLink> fullLinkList;
    private androidx.appcompat.widget.SearchView searchView;
    private View searchCard;
    private TextView emptyStateTextView;

    public static BatchWiseFragment newInstance(String gender) {
        BatchWiseFragment fragment = new BatchWiseFragment();
        Bundle args = new Bundle();
        args.putString(ARG_GENDER, gender);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            gender = getArguments().getString(ARG_GENDER);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_batch_wise, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewBatchWise);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        searchView = view.findViewById(R.id.searchViewBatchResources);
        searchCard = view.findViewById(R.id.searchCard);
        emptyStateTextView = view.findViewById(R.id.emptyStateBatchText);

        linkList = new ArrayList<>();
        fullLinkList = new ArrayList<>();
        adapter = new DriveLinkAdapter(linkList);
        recyclerView.setAdapter(adapter);

        setupSearch();
        fetchBatchWiseLinks();

        return view;
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.filterList(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapter != null) {
                    adapter.filterList(newText);
                }
                return true;
            }
        });
    }


    private void fetchBatchWiseLinks() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String deptCode = UserData.getInstance().getDepartmentName();

        if (deptCode == null || deptCode.isEmpty()) {
            Toast.makeText(getContext(), "Department not set.", Toast.LENGTH_SHORT).show();
            return;
        }

        CollectionReference collectionRef;

        if (deptCode.equalsIgnoreCase("CCE")) {
            collectionRef = db.collection("batch_wise_links");
        } else {
            String deptDocumentId = "dept_" + deptCode.toLowerCase();

            collectionRef = db.collection("departments")
                    .document(deptDocumentId)
                    .collection("batch_wise_links");
        }

        emptyStateTextView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);

        collectionRef
                .whereEqualTo("gender", gender)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    fullLinkList.clear();
                    for (DocumentSnapshot doc : querySnapshots) {
                        String title = doc.getString("title");
                        String url = doc.getString("url");
                        DriveLink link = new DriveLink(title, url);
                        fullLinkList.add(link);
                    }

                    linkList.clear();
                    linkList.addAll(fullLinkList);
                    adapter.setFullList(fullLinkList);
                    adapter.notifyDataSetChanged();

                    if (fullLinkList.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        if (searchCard != null) searchCard.setVisibility(View.GONE);
                        emptyStateTextView.setText("No batch resources found for the " + deptCode + " department.");
                        emptyStateTextView.setVisibility(View.VISIBLE);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        if (searchCard != null) searchCard.setVisibility(View.VISIBLE);
                        emptyStateTextView.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load batch wise links for " + deptCode, Toast.LENGTH_SHORT).show();
                    recyclerView.setVisibility(View.GONE);
                    emptyStateTextView.setText("Failed to connect to resources for " + deptCode + ".");
                    emptyStateTextView.setVisibility(View.VISIBLE);
                });
    }
}