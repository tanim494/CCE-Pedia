package com.tanim.ccepedia;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SemesterResources extends Fragment implements SemesterAdapter.OnSemesterClickListener {
    private RecyclerView recyclerView;
    private SemesterAdapter adapter;
    private List<Semester> semesterList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_semester_resources, container, false);

        recyclerView = rootView.findViewById(R.id.semester_recycler_view);

        semesterList = createSemesterData();

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new SemesterAdapter(semesterList, this);
        recyclerView.setAdapter(adapter);

        return rootView;
    }

    @Override
    public void onSemesterClick(String semesterId) {
        openCourseListFragment(semesterId);
    }


    private void openCourseListFragment(String semesterId) {
        CourseListFragment fragment = CourseListFragment.newInstance(semesterId);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.Midcontainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    private List<Semester> createSemesterData() {
        List<Semester> list = new ArrayList<>();

        int currentSemesterNumber;
        String currentSemesterString = UserData.getInstance().getSemester();
        
        if (currentSemesterString != null && currentSemesterString.equalsIgnoreCase("Outgoing")) {
            currentSemesterNumber = 9; // Higher than any semester count to mark all as completed
        } else if (currentSemesterString != null) {
            try {
                currentSemesterNumber = Integer.parseInt(currentSemesterString.replaceAll("[^0-9]", ""));
            } catch (Exception e) {
                currentSemesterNumber = 1;
            }
        } else {
            currentSemesterNumber = 1;
        }

        for (int i = 1; i <= 8; i++) {
            String title = "Semester " + i;
            String id = "semester_" + i;
            String status;

            if (i < currentSemesterNumber) {
                status = "Completed";
            } else if (i == currentSemesterNumber) {
                status = "Current";
            } else {
                status = "Upcoming";
            }

            list.add(new Semester(title, id, status));
        }

        return list;
    }
}