package com.tanim.ccepedia;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import androidx.appcompat.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AdminUserListFragment extends Fragment {

    private AutoCompleteTextView spinnerGender, spinnerSemester, spinnerRole, spinnerDepartment;
    private SearchView searchView;
    private RecyclerView recyclerViewUsers;
    private UserListAdapter userAdapter;
    private List<UserListModel> userList;
    private List<UserListModel> allUsers;
    private TextView tvUserCount;

    private FirebaseFirestore db;
    private DepartmentRepository departmentRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_user_list, container, false);

        db = FirebaseFirestore.getInstance();
        departmentRepository = new DepartmentRepository();

        spinnerGender = view.findViewById(R.id.spinnerGender);
        spinnerSemester = view.findViewById(R.id.spinnerSemester);
        spinnerRole = view.findViewById(R.id.spinnerRole);
        spinnerDepartment = view.findViewById(R.id.spinnerDepartment);
        searchView = view.findViewById(R.id.searchView);
        tvUserCount = view.findViewById(R.id.tvUserCount);
        recyclerViewUsers = view.findViewById(R.id.recyclerViewUsers);

        // Keep the (permanently expanded) search box from stealing focus on entry — otherwise
        // it pops the keyboard, and the first Back press only dismisses that instead of leaving.
        searchView.clearFocus();

        userList = new ArrayList<>();
        allUsers = new ArrayList<>();
        userAdapter = new UserListAdapter(getContext(), userList);

        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewUsers.setAdapter(userAdapter);

        setupSpinners();

        loadUsers(null, null, null, null);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterBySearch(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterBySearch(newText);
                return false;
            }
        });

        return view;
    }

    private void setupSpinners() {
        int itemLayout = android.R.layout.simple_spinner_dropdown_item;

        String[] genders = {"All", "male", "female"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(getContext(), itemLayout, genders);
        spinnerGender.setAdapter(genderAdapter);

        String[] semesters = new String[10];
        semesters[0] = "All";
        for (int i = 1; i <= 8; i++) {
            semesters[i] = String.valueOf(i);
        }
        semesters[9] = "Outgoing";

        ArrayAdapter<String> semesterAdapter = new ArrayAdapter<>(getContext(), itemLayout, semesters);
        spinnerSemester.setAdapter(semesterAdapter);

        String[] roles = {"All", "admin", "moderator", "user"};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(getContext(), itemLayout, roles);
        spinnerRole.setAdapter(roleAdapter);

        loadDepartmentOptions(itemLayout);

        AdapterView.OnItemClickListener filterListener = (parent, view1, position, id1) -> applyFiltersAndSearch();

        spinnerGender.setOnItemClickListener(filterListener);
        spinnerSemester.setOnItemClickListener(filterListener);
        spinnerRole.setOnItemClickListener(filterListener);
        spinnerDepartment.setOnItemClickListener(filterListener);
    }

    private void loadDepartmentOptions(int itemLayout) {
        departmentRepository.fetchAllDepartmentIds()
                .addOnSuccessListener(ids -> {
                    List<String> deptCodes = ids.stream()
                            .map(id -> id.replace("dept_", "").toUpperCase())
                            .collect(Collectors.toList());

                    deptCodes.add(0, "All");

                    ArrayAdapter<String> departmentAdapter = new ArrayAdapter<>(getContext(), itemLayout, deptCodes);
                    spinnerDepartment.setAdapter(departmentAdapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading department filters.", Toast.LENGTH_SHORT).show();
                    ArrayAdapter<String> fallbackAdapter = new ArrayAdapter<>(getContext(), itemLayout, new String[]{"All"});
                    spinnerDepartment.setAdapter(fallbackAdapter);
                });
    }

    private void applyFiltersAndSearch() {
        String gender = spinnerGender.getText().toString().trim();
        if ("All".equals(gender) || gender.isEmpty()) gender = null;

        String semester = spinnerSemester.getText().toString().trim();
        if ("All".equals(semester) || semester.isEmpty()) semester = null;

        String role = spinnerRole.getText().toString().trim();
        if ("All".equals(role) || role.isEmpty()) role = null;

        String departmentFilter = spinnerDepartment.getText().toString().trim();
        if ("All".equals(departmentFilter) || departmentFilter.isEmpty()) departmentFilter = null;

        loadUsers(gender, semester, role, departmentFilter);

        filterBySearch(searchView.getQuery().toString());
    }

    private void loadUsers(String genderFilter, String semesterFilter, String roleFilter, String departmentFilter) {

        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    List<UserListModel> fetchedUsers = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        UserListModel user = doc.toObject(UserListModel.class);
                        fetchedUsers.add(user);
                    }

                    Collections.sort(fetchedUsers, (u1, u2) -> {
                        if (u1.getLastLoggedIn() == null && u2.getLastLoggedIn() == null) return 0;
                        if (u1.getLastLoggedIn() == null) return 1;
                        if (u2.getLastLoggedIn() == null) return -1;
                        return u2.getLastLoggedIn().compareTo(u1.getLastLoggedIn());
                    });

                    userList.clear();
                    allUsers.clear();

                    for (UserListModel user : fetchedUsers) {
                        boolean genderMatch = (genderFilter == null || (user.getGender() != null && user.getGender().equalsIgnoreCase(genderFilter)));

                        boolean semesterMatch = (semesterFilter == null || (user.getSemester() != null && user.getSemester().equalsIgnoreCase(semesterFilter)));

                        boolean roleMatch;
                        if (roleFilter == null) {
                            roleMatch = true;
                        } else if (roleFilter.equals("user")) {
                            roleMatch = (user.getRole() == null || user.getRole().isEmpty() || user.getRole().equalsIgnoreCase("user"));
                        } else {
                            roleMatch = roleFilter.equalsIgnoreCase(user.getRole());
                        }

                        boolean departmentMatch = (departmentFilter == null ||
                                (user.getDepartmentName() != null && user.getDepartmentName().equalsIgnoreCase(departmentFilter)));


                        if (genderMatch && semesterMatch && roleMatch && departmentMatch) {
                            allUsers.add(user);
                        }
                    }

                    userList.addAll(allUsers);
                    userAdapter.notifyDataSetChanged();
                    updateUserCount();
                });
    }

    private void filterBySearch(String query) {
        if (query == null) query = "";
        query = query.toLowerCase();

        List<UserListModel> filteredList = new ArrayList<>();

        for (UserListModel user : allUsers) {
            if ((user.getName() != null && user.getName().toLowerCase().contains(query)) ||
                    (user.getStudentId() != null && user.getStudentId().toLowerCase().contains(query))) {
                filteredList.add(user);
            }
        }

        userList.clear();
        userList.addAll(filteredList);
        userAdapter.notifyDataSetChanged();
        updateUserCount();
    }

    private void updateUserCount() {
        int count = userList.size();
        String countText = count + " user(s)";
        tvUserCount.setText(countText);
    }
}