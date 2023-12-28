package com.tanim.ccepedia;

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
        TextView questionPage = rootView.findViewById(R.id.openQuestionPage);
        TextView semesterResourcesPage = rootView.findViewById(R.id.openSemesterResourcesPage);
        TextView batchWise = rootView.findViewById(R.id.batchWise);
        TextView impPage = rootView.findViewById(R.id.openImpPage);
        TextView busPage = rootView.findViewById(R.id.openBusPage);
        TextView routinePage = rootView.findViewById(R.id.openRoutinePage);

        facebookPage.setOnClickListener(v -> openWebPage("https://www.facebook.com/profile.php?id=100090282199663"));

        facebookFemPage.setOnClickListener(v -> openWebPage("https://www.facebook.com/profile.php?id=100091710725410"));

        questionPage.setOnClickListener(v -> openWebPage("https://drive.google.com/drive/folders/19kpQtcze690uvLwJRz0uhfmy4Ji0qp7e"));

        batchWise.setOnClickListener(v -> openWebPage("https://jpst.it/3wo0t"));

        impPage.setOnClickListener(v -> openWebPage("https://jpst.it/3q6PY"));

        routinePage.setOnClickListener(v -> {
            setupWebFragment();
            WebContent.setLink("https://jpst.it/3q4Rc");
        });

        busPage.setOnClickListener(v -> {
            setupWebFragment();
            WebContent.setLink("https://jpst.it/3q4Pl");
        });

        semesterResourcesPage.setOnClickListener(v -> {
            FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.Midcontainer, new SemesterResources());
            fragmentTransaction.addToBackStack(null); // Optional: Add transaction to the back stack
            fragmentTransaction.commit();
        });

        return rootView;
    }
    private void openWebPage(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }
    private void setupWebFragment() {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.Midcontainer, new WebContent());
        fragmentTransaction.addToBackStack(null); // Optional: Add transaction to the back stack
        fragmentTransaction.commit();
    }

}
