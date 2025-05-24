package com.tanim.ccepedia;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class Resources extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_resources, container, false);

        TextView facebookPage = rootView.findViewById(R.id.openFacebookPage);
        TextView facebookFemPage = rootView.findViewById(R.id.openFacebookFemPage);
        TextView semesterResourcesPage = rootView.findViewById(R.id.openSemesterResourcesPage);
        TextView batchWise = rootView.findViewById(R.id.batchWise);
        TextView busPage = rootView.findViewById(R.id.openBusPage);
        TextView driveLinks = rootView.findViewById(R.id.openDriveLinks);

        facebookPage.setOnClickListener(v -> openWebPage("https://www.facebook.com/profile.php?id=100090282199663"));

        facebookFemPage.setOnClickListener(v -> openWebPage("https://www.facebook.com/profile.php?id=100091710725410"));

        batchWise.setOnClickListener(v -> showGenderDialog());

        busPage.setOnClickListener(v -> {
            FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.Midcontainer, new BusScheduleFragment());
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        });


        semesterResourcesPage.setOnClickListener(v -> {
            FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.Midcontainer, new SemesterResources());
            fragmentTransaction.addToBackStack(null); // Optional: Add transaction to the back stack
            fragmentTransaction.commit();
        });

        driveLinks.setOnClickListener(v -> {
            FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.Midcontainer, new DriveLinksFragment());
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        });

        return rootView;
    }

    private void showGenderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Select Section")
                .setItems(new String[]{"Male", "Female"}, (dialog, which) -> {
                    String gender = (which == 0) ? "male" : "female";
                    openBatchWiseFragment(gender);
                });
        builder.show();
    }

    private void openBatchWiseFragment(String gender) {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.Midcontainer, BatchWiseFragment.newInstance(gender));
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void openWebPage(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

}
