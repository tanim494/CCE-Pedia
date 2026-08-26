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
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CourseListFragment extends Fragment implements CourseAdapter.OnCourseClickListener {

    private static final String ARG_SEMESTER_ID = "semesterId";
    private String semesterId;

    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private CourseAdapter adapter;
    private SearchView searchView;
    private TextView emptyStateTextView; // 🌟 NEW FIELD
    private ProgressBar loadingSpinner;
    private MaterialButton shareCommunityButton;

    public static CourseListFragment newInstance(String semesterId) {
        CourseListFragment fragment = new CourseListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SEMESTER_ID, semesterId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            semesterId = getArguments().getString(ARG_SEMESTER_ID);
        }
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_course_list, container, false);

        recyclerView = view.findViewById(R.id.courseRecyclerView);
        searchView = view.findViewById(R.id.searchViewCourses);
        emptyStateTextView = view.findViewById(R.id.emptyStateCoursesText);
        loadingSpinner = view.findViewById(R.id.loadingSpinner);
        shareCommunityButton = view.findViewById(R.id.shareCommunityButton);
        TextView resourceHeader = view.findViewById(R.id.resourceHeaderText);
        TextView resourceSubtitle = view.findViewById(R.id.resourceSubtitleText);

        String deptName = UserData.getInstance().getDepartmentName();
        String semesterDisplay = semesterId != null ? semesterId.replace("semester_", "Semester ") : "Semester";
        resourceHeader.setText(deptName != null && !deptName.isEmpty()
                ? deptName + " / " + semesterDisplay
                : semesterDisplay);
        resourceSubtitle.setText("Browse Courses");

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CourseAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);

        setupSearch();
        fetchCourses();

        shareCommunityButton.setOnClickListener(v -> {
            String title = deptName + "/" + semesterDisplay + " Resources";
            ShareToCommunityDialog.show(requireContext(), CommunityShare.TYPE_COURSE_LIST, "", title);
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

    private void fetchCourses() {
        String deptCode = UserData.getInstance().getDepartmentName();

        if (deptCode == null || deptCode.isEmpty()) {
            Toast.makeText(getContext(), "Department not set.", Toast.LENGTH_SHORT).show();
            return;
        }

        CollectionReference collectionRef;

        if (deptCode.equalsIgnoreCase("CCE")) {
            collectionRef = db.collection("semesters")
                    .document(semesterId)
                    .collection("courses");
        } else {
            String deptDocumentId = "dept_" + deptCode.toLowerCase();

            collectionRef = db.collection("departments")
                    .document(deptDocumentId)
                    .collection("semesters")
                    .document(semesterId)
                    .collection("courses");
        }

        emptyStateTextView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        loadingSpinner.setVisibility(View.VISIBLE);


        collectionRef
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    loadingSpinner.setVisibility(View.GONE);
                    List<Course> newCourseList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String id = doc.getId();

                        String title = id;

                        newCourseList.add(new Course(id, title));
                    }

                    adapter.updateData(newCourseList);

                    if (newCourseList.isEmpty()) {
                        emptyStateTextView.setText("No courses found for " + deptCode + " in this semester.");
                        emptyStateTextView.setVisibility(View.VISIBLE);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    loadingSpinner.setVisibility(View.GONE);
                    emptyStateTextView.setText("Failed to connect and load courses for " + deptCode + ".");
                    emptyStateTextView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                });
    }

    @Override
    public void onCourseClick(String courseId) {
        openFileListFragment(courseId);
    }

    private void openFileListFragment(String courseId) {
        String deptCode = UserData.getInstance().getDepartmentName();

        FileListFragment fragment = FileListFragment.newInstance(semesterId, courseId, deptCode);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.Midcontainer, fragment)
                .addToBackStack(null)
                .commit();
    }
}