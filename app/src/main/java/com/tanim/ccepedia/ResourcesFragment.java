package com.tanim.ccepedia;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class ResourcesFragment extends Fragment {

    private MaterialCardView cardFacebookPage;
    private MaterialCardView cardFacebookFemPage;
    private MaterialCardView cardSemesterResourcesPage;
    private MaterialCardView cardBatchWise;
    private MaterialCardView cardBusPage;
    private MaterialCardView cardDriveLinks;

    private TextView textFacebookPage;
    private TextView textFacebookFemPage;

    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_resources, container, false);

        db = FirebaseFirestore.getInstance();

        cardFacebookPage = rootView.findViewById(R.id.cardFacebookPage);
        cardFacebookFemPage = rootView.findViewById(R.id.cardFacebookFemPage);
        cardSemesterResourcesPage = rootView.findViewById(R.id.cardSemesterResourcesPage);
        cardBatchWise = rootView.findViewById(R.id.cardBatchWise);
        cardBusPage = rootView.findViewById(R.id.cardBusPage);
        cardDriveLinks = rootView.findViewById(R.id.cardDriveLinks);

        textFacebookPage = rootView.findViewById(R.id.openFacebookPage);
        textFacebookFemPage = rootView.findViewById(R.id.openFacebookFemPage);

        setupStaticListeners();
        fetchDynamicLinks();

        return rootView;
    }

    private void setupStaticListeners() {
        cardBatchWise.setOnClickListener(v -> showGenderDialog());
        cardBusPage.setOnClickListener(v -> openFragment(new BusScheduleFragment()));
        cardSemesterResourcesPage.setOnClickListener(v -> openFragment(new SemesterResources()));
        cardDriveLinks.setOnClickListener(v -> openFragment(new DriveLinksFragment()));

        // Fallback until the dynamic club links load — and if the fetch fails, taps still respond.
        cardFacebookPage.setOnClickListener(v -> notifyLinkUnavailable());
        cardFacebookFemPage.setOnClickListener(v -> notifyLinkUnavailable());
    }

    private void fetchDynamicLinks() {
        db.collection("appConfig").document("main")
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    if (doc != null && doc.exists()) {

                        Map<String, String> fbClubData = asStringMap(doc.get("club_link_male"));
                        Map<String, String> fbFemaleData = asStringMap(doc.get("club_link_female"));

                        setupLinkButton(cardFacebookPage, textFacebookPage, fbClubData);
                        setupLinkButton(cardFacebookFemPage, textFacebookFemPage, fbFemaleData);

                    } else {
                        Toast.makeText(requireContext(), "Failed to load external links.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Failed to load external links.", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupLinkButton(MaterialCardView button, TextView textView, Map<String, String> data) {
        if (data != null && data.containsKey("title") && data.containsKey("url")) {
            String title = data.get("title");
            String url = data.get("url");

            textView.setText(title);
            button.setOnClickListener(v -> openWebPage(url));
        } else {
            button.setOnClickListener(v -> notifyLinkUnavailable());
        }
    }

    private void openWebPage(String url) {
        if (url == null || url.isEmpty()) {
            notifyLinkUnavailable();
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Context ctx = getContext();
            if (ctx != null) Toast.makeText(ctx, "Couldn't open the link.", Toast.LENGTH_SHORT).show();
        }
    }

    private void notifyLinkUnavailable() {
        Context ctx = getContext();
        if (ctx != null) Toast.makeText(ctx, "Link not available.", Toast.LENGTH_SHORT).show();
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> asStringMap(Object value) {
        return value instanceof Map ? (Map<String, String>) value : null;
    }

    private void openFragment(Fragment fragment) {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.Midcontainer, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }


    private void showGenderDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Section")
                .setItems(new String[]{"Male", "Female"}, (dialog, which) -> {
                    String gender = (which == 0) ? "male" : "female";
                    openBatchWiseFragment(gender);
                })
                .show();
    }

    private void openBatchWiseFragment(String gender) {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.Midcontainer, BatchWiseFragment.newInstance(gender));
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
}