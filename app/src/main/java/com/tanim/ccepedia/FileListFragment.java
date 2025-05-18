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

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FileListFragment extends Fragment {

    private static final String ARG_SEMESTER_ID = "semesterId";
    private static final String ARG_COURSE_ID = "courseId";

    private String semesterId;
    private String courseId;

    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private List<FileItem> fileList = new ArrayList<>();
    private FileAdapter adapter;

    public static FileListFragment newInstance(String semesterId, String courseId) {
        FileListFragment fragment = new FileListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SEMESTER_ID, semesterId);
        args.putString(ARG_COURSE_ID, courseId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            semesterId = getArguments().getString(ARG_SEMESTER_ID);
            courseId = getArguments().getString(ARG_COURSE_ID);
        }
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_file_list, container, false);
        recyclerView = view.findViewById(R.id.fileRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new FileAdapter(requireContext(), fileList, item -> {
            PdfViewerFragment fragment = PdfViewerFragment.newInstance(item.getUrl(), item.getFileName());
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.Midcontainer, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        recyclerView.setAdapter(adapter);

        fetchFiles();
        return view;
    }

    private void fetchFiles() {
        db.collection("semesters")
                .document(semesterId)
                .collection("courses")
                .document(courseId)
                .collection("files")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(requireContext(), "No files found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    fileList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String fileName = doc.getString("fileName");
                        String url = doc.getString("url");
                        String uploader = doc.getString("uploadedBy");

                        fileList.add(new FileItem(fileName, url, uploader));
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to load files: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
