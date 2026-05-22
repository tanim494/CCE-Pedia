package com.tanim.ccepedia;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.TypedValue;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import androidx.fragment.app.Fragment;

public class DeveloperFragment extends Fragment {
    private LinearLayout moderatorListContainer;
    private FirebaseFirestore db;

    private View githubButton, facebookButton, linkedInButton;

    private static class AppUser {
        String name;
        String studentId;
        String role;
        String department;
        long viewCount;

        public AppUser(String name, String studentId, String role, String department, long viewCount) {
            this.name = name;
            this.studentId = studentId;
            this.role = role;
            this.department = department;
            this.viewCount = viewCount;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_developer, container, false);

        moderatorListContainer = view.findViewById(R.id.moderatorListContainer);
        db = FirebaseFirestore.getInstance();

        githubButton = view.findViewById(R.id.githubButton);
        facebookButton = view.findViewById(R.id.facebookButton);
        linkedInButton = view.findViewById(R.id.linkedinButton);

        TextView appVersion = view.findViewById(R.id.appVersionText);
        appVersion.setText("App Version " + BuildConfig.VERSION_NAME);

        fetchModerators();
        fetchDeveloperLinks();

        return view;
    }

    private void fetchDeveloperLinks() {
        db.collection("appConfig").document("main")
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        setupLinkButton(githubButton, doc.getString("dev_github"));
                        setupLinkButton(facebookButton, doc.getString("dev_facebook"));
                        setupLinkButton(linkedInButton, doc.getString("dev_linkedin"));
                    } else {
                        Toast.makeText(getContext(), "Failed to load developer links.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load developer links.", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupLinkButton(View button, String url) {
        if (url != null && !url.isEmpty()) {
            button.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            });
        }
    }


    private void fetchModerators() {
        db.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<AppUser> moderators = new ArrayList<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String role = document.getString("role");

                            if (role != null && (role.equalsIgnoreCase("admin") || role.equalsIgnoreCase("moderator"))) {

                                String name = document.getString("name");
                                String studentId = document.getString("id");
                                String department = document.getString("department");

                                Long viewCountLong = document.getLong("viewCount");
                                long viewCount = (viewCountLong != null) ? viewCountLong : 0L;

                                String displayRole = role.substring(0, 1).toUpperCase() + role.substring(1).toLowerCase();

                                if (name != null) {
                                    moderators.add(new AppUser(name, studentId, displayRole, department, viewCount));
                                }
                            }
                        }

                        displayModerators(moderators);
                    } else {
                        Toast.makeText(getContext(), "Failed to load moderator list.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void displayModerators(List<AppUser> users) {
        if (users.isEmpty()) {
            return;
        }

        Collections.sort(users, (u1, u2) -> {
            boolean u1IsAdmin = u1.role.equalsIgnoreCase("Admin");
            boolean u2IsAdmin = u2.role.equalsIgnoreCase("Admin");

            if (u1IsAdmin && !u2IsAdmin) return -1;
            if (!u1IsAdmin && u2IsAdmin) return 1;

            return Long.compare(u2.viewCount, u1.viewCount);
        });

        for (AppUser user : users) {
            MaterialCardView card = new MaterialCardView(requireContext());
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, 4);
            card.setLayoutParams(cardParams);
            card.setCardElevation(0);
            card.setRadius(0);

            LinearLayout itemLayout = new LinearLayout(getContext());
            itemLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            itemLayout.setOrientation(LinearLayout.HORIZONTAL);
            itemLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
            itemLayout.setPadding(16, 16, 16, 16);

            MaterialCardView iconCard = new MaterialCardView(requireContext());
            int iconSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics());
            iconCard.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));
            iconCard.setRadius(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics()));
            iconCard.setCardBackgroundColor(getResources().getColor(R.color.GreenLight, null));
            iconCard.setCardElevation(0);

            ImageView icon = new ImageView(requireContext());
            icon.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            icon.setPadding(8, 8, 8, 8);
            icon.setImageResource(R.drawable.ic_profile);
            icon.setColorFilter(getResources().getColor(R.color.Green, null));
            iconCard.addView(icon);

            LinearLayout textLayout = new LinearLayout(getContext());
            LinearLayout.LayoutParams textLayoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            textLayoutParams.setMarginStart(16);
            textLayout.setLayoutParams(textLayoutParams);
            textLayout.setOrientation(LinearLayout.VERTICAL);

            TextView nameRoleText = new TextView(getContext());
            nameRoleText.setText(user.name);
            nameRoleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            nameRoleText.setTextColor(getResources().getColor(R.color.textPrimary, null));
            nameRoleText.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView studentIdText = new TextView(getContext());
            String idText = (user.studentId != null ? user.studentId : "N/A");
            String deptText = (user.department != null ? " • " + user.department : "");
            String roleText = " • " + user.role;
            studentIdText.setText(idText + deptText + roleText);
            studentIdText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            studentIdText.setTextColor(getResources().getColor(R.color.textSecondary, null));

            textLayout.addView(nameRoleText);
            textLayout.addView(studentIdText);

            TextView viewCountText = new TextView(getContext());
            viewCountText.setText("Views: " + user.viewCount);
            viewCountText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            viewCountText.setTextColor(getResources().getColor(R.color.Green, null));
            viewCountText.setTypeface(null, android.graphics.Typeface.BOLD);
            viewCountText.setPadding(8, 4, 8, 4);

            itemLayout.addView(iconCard);
            itemLayout.addView(textLayout);
            itemLayout.addView(viewCountText);

            card.addView(itemLayout);
            moderatorListContainer.addView(card);
        }
    }
}