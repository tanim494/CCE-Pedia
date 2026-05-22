package com.tanim.ccepedia;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.material.card.MaterialCardView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


public class AdminFragment extends Fragment {

    private MaterialCardView btnBatchLinks, btnDriveLinks, btnMessages, btnBusSchedule, btnAppConfig, btnUserList, btnImport;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_admin, container, false);

        btnUserList = view.findViewById(R.id.btnUserList);
        btnImport = view.findViewById(R.id.importFaculty);
        btnBatchLinks = view.findViewById(R.id.btnBatchLinks);
        btnDriveLinks = view.findViewById(R.id.btnDriveLinks);
        btnMessages = view.findViewById(R.id.btnMessages);
        btnBusSchedule = view.findViewById(R.id.btnBusSchedule);
        btnAppConfig = view.findViewById(R.id.btnAppConfig);

        btnUserList.setOnClickListener(v -> replaceFragment(new AdminUserListFragment()));
        btnImport.setOnClickListener(v -> replaceFragment(new AdminFacultyImportFragment()));
        btnBatchLinks.setOnClickListener(v -> replaceFragment(new AdminBatchLinkFragment()));
        btnDriveLinks.setOnClickListener(v -> replaceFragment(new AdminLinksFragment()));
        btnMessages.setOnClickListener(v -> replaceFragment(new ManageMessagesNoticesFragment()));
        btnBusSchedule.setOnClickListener(v -> replaceFragment(new AdminBusFragment()));
        btnAppConfig.setOnClickListener(v -> replaceFragment(new AdminConfigFragment()));

        return view;
    }

    private void replaceFragment(Fragment fragment) {
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.Midcontainer, fragment)
                .addToBackStack(null)
                .commit();
    }
}
