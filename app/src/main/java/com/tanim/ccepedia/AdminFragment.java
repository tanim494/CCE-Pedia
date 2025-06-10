package com.tanim.ccepedia;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


public class AdminFragment extends Fragment {

    private Button btnBatchLinks, btnDriveLinks, btnMessages, btnBusSchedule, btnAppConfig, btnUserList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_admin, container, false);

        // Initialize buttons
        btnUserList = view.findViewById(R.id.btnUserList);
        btnBatchLinks = view.findViewById(R.id.btnBatchLinks);
        btnDriveLinks = view.findViewById(R.id.btnDriveLinks);
        btnMessages = view.findViewById(R.id.btnMessages);
        btnBusSchedule = view.findViewById(R.id.btnBusSchedule);
        btnAppConfig = view.findViewById(R.id.btnAppConfig);

        // Button click listeners
        btnUserList.setOnClickListener(v -> replaceFragment(new ADMUserListFragment()));
        btnBatchLinks.setOnClickListener(v -> replaceFragment(new ADMBatchLinkFragment()));
        btnDriveLinks.setOnClickListener(v -> replaceFragment(new ADMDriveLinksFragment()));
        btnMessages.setOnClickListener(v -> replaceFragment(new ManageMessagesNoticesFragment()));
        btnBusSchedule.setOnClickListener(v -> replaceFragment(new ADMBusFragment()));
        btnAppConfig.setOnClickListener(v -> replaceFragment(new ADMConfigFragment()));

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
