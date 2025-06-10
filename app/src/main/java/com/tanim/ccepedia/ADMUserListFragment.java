package com.tanim.ccepedia;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import androidx.appcompat.widget.SearchView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ADMUserListFragment extends Fragment {

    private Spinner spinnerGender, spinnerSemester, spinnerRole, spinnerVerified;  // Add spinnerVerified
    private SearchView searchView;
    private RecyclerView recyclerViewUsers;
    private UserListAdapter userAdapter;
    private List<UserListModel> userList;
    private List<UserListModel> allUsers;

    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_user_list, container, false);

        db = FirebaseFirestore.getInstance();

        spinnerGender = view.findViewById(R.id.spinnerGender);
        spinnerSemester = view.findViewById(R.id.spinnerSemester);
        spinnerRole = view.findViewById(R.id.spinnerRole);
        spinnerVerified = view.findViewById(R.id.spinnerVerified);  // Initialize spinnerVerified
        searchView = view.findViewById(R.id.searchView);
        searchView.setIconified(false);
        recyclerViewUsers = view.findViewById(R.id.recyclerViewUsers);

        userList = new ArrayList<>();
        allUsers = new ArrayList<>();
        userAdapter = new UserListAdapter(getContext(), userList);

        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewUsers.setAdapter(userAdapter);

        setupSpinners();

        // Load all users initially
        loadUsers(null, null, null, null);

        // Search listener
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
        // Gender spinner options
        String[] genders = {"All", "male", "female"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, genders);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(genderAdapter);

        // Semester spinner options
        String[] semesters = new String[9];
        semesters[0] = "All";
        for (int i = 1; i <= 8; i++) {
            semesters[i] = String.valueOf(i);
        }
        ArrayAdapter<String> semesterAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, semesters);
        semesterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSemester.setAdapter(semesterAdapter);

        // Role spinner options
        String[] roles = {"All", "admin", "moderator", "user"};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, roles);
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(roleAdapter);

        // Verified spinner options
        String[] verifiedOptions = {"All", "Verified", "Not Verified"};
        ArrayAdapter<String> verifiedAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, verifiedOptions);
        verifiedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVerified.setAdapter(verifiedAdapter);

        // Listener for all filters
        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFiltersAndSearch();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerGender.setOnItemSelectedListener(filterListener);
        spinnerSemester.setOnItemSelectedListener(filterListener);
        spinnerRole.setOnItemSelectedListener(filterListener);
        spinnerVerified.setOnItemSelectedListener(filterListener);  // Add listener here
    }

    private void applyFiltersAndSearch() {
        String gender = spinnerGender.getSelectedItem().toString();
        if (gender.equals("All")) gender = null;

        String semester = spinnerSemester.getSelectedItem().toString();
        if (semester.equals("All")) semester = null;

        String role = spinnerRole.getSelectedItem().toString();
        if (role.equals("All")) role = null;

        String verifiedString = spinnerVerified.getSelectedItem().toString();
        Boolean verifiedFilter = null;
        if (verifiedString.equals("Verified")) {
            verifiedFilter = true;
        } else if (verifiedString.equals("Not Verified")) {
            verifiedFilter = false;
        }

        loadUsers(gender, semester, role, verifiedFilter);

        filterBySearch(searchView.getQuery().toString());
    }

    // Added verifiedFilter param
    private void loadUsers(String genderFilter, String semesterFilter, String roleFilter, Boolean verifiedFilter) {
        db.collection("users").get().addOnSuccessListener(queryDocumentSnapshots -> {
            userList.clear();
            allUsers.clear();

            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                UserListModel user = doc.toObject(UserListModel.class);

                boolean genderMatch = (genderFilter == null || (user.getGender() != null && user.getGender().equalsIgnoreCase(genderFilter)));
                boolean semesterMatch = (semesterFilter == null || (user.getSemester() != null && user.getSemester().equalsIgnoreCase(semesterFilter)));
                boolean roleMatch;

                if (roleFilter == null) {
                    roleMatch = true;
                } else if (roleFilter.equals("user")) {
                    roleMatch = (user.getRole() == null || user.getRole().equalsIgnoreCase("user"));
                } else {
                    roleMatch = roleFilter.equalsIgnoreCase(user.getRole());
                }

                boolean verifiedMatch = true;
                if (verifiedFilter != null) {
                    // Assuming user.getVerified() returns Boolean (nullable)
                    Boolean userVerified = user.isVerified();
                    if (userVerified == null) userVerified = false; // treat null as not verified
                    verifiedMatch = userVerified.equals(verifiedFilter);
                }

                if (genderMatch && semesterMatch && roleMatch && verifiedMatch) {
                    userList.add(user);
                    allUsers.add(user);
                }
            }
            userAdapter.notifyDataSetChanged();
        });
    }

    private void filterBySearch(String query) {
        query = query.toLowerCase();
        List<UserListModel> filteredList = new ArrayList<>();

        for (UserListModel user : allUsers) {
            if ((user.getName() != null && user.getName().toLowerCase().contains(query)) ||
                    (user.getId() != null && user.getId().toLowerCase().contains(query))) {
                filteredList.add(user);
            }
        }

        userList.clear();
        userList.addAll(filteredList);
        userAdapter.notifyDataSetChanged();
    }
}
