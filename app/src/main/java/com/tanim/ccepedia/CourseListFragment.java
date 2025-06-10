package com.tanim.ccepedia;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CourseListFragment extends Fragment {

    private static final String ARG_SEMESTER_ID = "semesterId";
    private String semesterId;

    private FirebaseFirestore db;
    private ListView listView;
    private List<String> courseList = new ArrayList<>();

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
        listView = view.findViewById(R.id.courseListView);
        fetchCourses();
        return view;
    }

    private void fetchCourses() {
        db.collection("semesters")
                .document(semesterId)
                .collection("courses")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(requireContext(), "No courses found", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    courseList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        courseList.add(doc.getId());
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, courseList);
                    listView.setAdapter(adapter);

                    listView.setOnItemClickListener((parent, view, position, id) -> {
                        String selectedCourse = courseList.get(position);
                        openFileListFragment(selectedCourse);
                    });
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to load courses: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void openFileListFragment(String courseId) {
        FileListFragment fragment = FileListFragment.newInstance(semesterId, courseId);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.Midcontainer, fragment)
                .addToBackStack(null)
                .commit();
    }
}
