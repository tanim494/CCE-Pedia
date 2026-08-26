package com.tanim.ccepedia;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class FileListFragment extends Fragment {

    private static final String ARG_SEMESTER_ID = "semesterId";
    private static final String ARG_COURSE_ID = "courseId";
    private static final String ARG_DEPT_CODE = "deptCode";

    private String semesterId;
    private String courseId;
    private String deptCode;

    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private FileAdapter adapter;
    private ProgressBar loadingSpinner;
    private SearchView searchView;
    private TextView emptyStateText;
    private MaterialButton shareCommunityButton;

    public static FileListFragment newInstance(String semesterId, String courseId, String deptCode) {
        FileListFragment fragment = new FileListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SEMESTER_ID, semesterId);
        args.putString(ARG_COURSE_ID, courseId);
        args.putString(ARG_DEPT_CODE, deptCode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            semesterId = getArguments().getString(ARG_SEMESTER_ID);
            courseId = getArguments().getString(ARG_COURSE_ID);
            deptCode = getArguments().getString(ARG_DEPT_CODE);
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
        loadingSpinner = view.findViewById(R.id.loadingSpinner);
        searchView = view.findViewById(R.id.searchViewFiles);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        shareCommunityButton = view.findViewById(R.id.shareCommunityButton);
        TextView resourceHeader = view.findViewById(R.id.resourceHeaderText);
        TextView resourceSubtitle = view.findViewById(R.id.resourceSubtitleText);

        String semesterDisplay = semesterId != null ? semesterId.replace("semester_", "Semester ") : "Semester";
        resourceHeader.setText(courseId != null && !courseId.isEmpty() ? courseId : "Course");
        resourceSubtitle.setText(deptCode != null && !deptCode.isEmpty()
                ? deptCode + " / " + semesterDisplay
                : semesterDisplay);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new FileAdapter(new ArrayList<>(), new FileAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(FileItem item) {
                if (item.getUploaderStudentId() != null) {
                    ViewTracker.incrementUserViewCount(item.getUploaderStudentId());
                }

                PdfViewerFragment fragment = PdfViewerFragment.newInstance(item.getUrl(), item.getFileName(), item.getUploaderStudentId());
                getParentFragmentManager()
                        .beginTransaction()
                        .replace(R.id.Midcontainer, fragment)
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            public void onDeleteClick(FileItem item) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete File")
                        .setMessage("Are you sure you want to delete \"" + item.getFileName() + "\"?")
                        .setPositiveButton("Yes", (dialog, which) -> deleteFile(item))
                        .setNegativeButton("No", null)
                        .show();
            }
        });

        recyclerView.setAdapter(adapter);

        setupSearch();
        fetchFiles();

        shareCommunityButton.setOnClickListener(v -> {
            String title = deptCode + "/" + semesterDisplay + "/" + courseId + " Resources";
            ShareToCommunityDialog.show(requireContext(), CommunityShare.TYPE_FILE_LIST, "", title);
        });

        return view;
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.filterList(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filterList(newText);
                return true;
            }
        });
    }

    private CollectionReference getFilesCollectionRef() {
        if (deptCode == null || deptCode.isEmpty()) {
            Toast.makeText(getContext(), "Department not set.", Toast.LENGTH_SHORT).show();
            return null;
        }

        if (deptCode.equalsIgnoreCase("CCE")) {
            return db.collection("semesters")
                    .document(semesterId)
                    .collection("courses")
                    .document(courseId)
                    .collection("files");
        } else {
            String deptDocumentId = "dept_" + deptCode.toLowerCase();

            return db.collection("departments")
                    .document(deptDocumentId)
                    .collection("semesters")
                    .document(semesterId)
                    .collection("courses")
                    .document(courseId)
                    .collection("files");
        }
    }


    private void fetchFiles() {
        CollectionReference filesCollectionRef = getFilesCollectionRef();

        if (filesCollectionRef == null) {
            loadingSpinner.setVisibility(View.GONE);
            emptyStateText.setText("Failed to load files: Department ID missing.");
            emptyStateText.setVisibility(View.VISIBLE);
            return;
        }

        loadingSpinner.setVisibility(View.VISIBLE);
        emptyStateText.setVisibility(View.GONE);

        filesCollectionRef
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    loadingSpinner.setVisibility(View.GONE);
                    if (queryDocumentSnapshots.isEmpty()) {
                        emptyStateText.setText("No files found for this course in " + deptCode + ".");
                        emptyStateText.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                        return;
                    }
                    recyclerView.setVisibility(View.VISIBLE);

                    List<FileItem> fetchedList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String id = doc.getId();
                        String fileName = doc.getString("fileName");
                        String url = doc.getString("url");
                        String uploader = doc.getString("uploadedBy");
                        String uploaderStudentId = doc.getString("uploaderStudentId");

                        FileItem item = new FileItem(id, fileName, url, uploader);
                        item.setUploaderStudentId(uploaderStudentId);
                        fetchedList.add(item);
                    }

                    adapter.updateData(fetchedList);
                })
                .addOnFailureListener(e -> {
                    loadingSpinner.setVisibility(View.GONE);
                    emptyStateText.setText("Error connecting to database. Please check your network.");
                    emptyStateText.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                });
    }

    private void deleteFile(FileItem item) {
        CollectionReference filesCollectionRef = getFilesCollectionRef();

        if (filesCollectionRef == null) {
            Toast.makeText(requireContext(), "Cannot delete: Department ID missing.", Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(requireContext(), "Deleting file...", Toast.LENGTH_SHORT).show();

        String storageDept = UserData.getInstance().getDepartmentName();

        StorageReference fileRef = FirebaseStorage.getInstance().getReference()
                .child(storageDept + "/" + semesterId + "/" + courseId + "/" + item.getFileName());

        fileRef.delete()
                .addOnSuccessListener(aVoid -> filesCollectionRef
                        .document(item.getId())
                        .delete()
                        .addOnSuccessListener(aVoid1 -> {
                            Toast.makeText(requireContext(), "File deleted successfully", Toast.LENGTH_SHORT).show();
                            fetchFiles();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(requireContext(), "Failed to delete metadata: " + e.getMessage(), Toast.LENGTH_SHORT).show()))
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed to delete file: object does not exist at location. Check path.", Toast.LENGTH_SHORT).show());
    }
}