package com.tanim.ccepedia;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
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
        String profileLink;
        String photoUrl;

        public AppUser(String name, String studentId, String role, String department, long viewCount, String profileLink, String photoUrl) {
            this.name = name;
            this.studentId = studentId;
            this.role = role;
            this.department = department;
            this.viewCount = viewCount;
            this.profileLink = profileLink;
            this.photoUrl = photoUrl;
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

        // Invite / share actions
        View shareBtn = view.findViewById(R.id.btn_share_app);
        if (shareBtn != null) {
            shareBtn.setOnClickListener(v -> shareApp());
        }
        View copyBtn = view.findViewById(R.id.btn_copy_invite);
        if (copyBtn != null) {
            copyBtn.setOnClickListener(v -> copyInvite());
        }

        fetchModerators();
        fetchDeveloperLinks();

        return view;
    }

    private void fetchDeveloperLinks() {
        db.collection("appConfig").document("main")
                .get()
                .addOnSuccessListener(doc -> {
                    // Prevent crash if fragment is detached before data loads
                    if (!isAdded()) return;

                    if (doc != null && doc.exists()) {
                        setupLinkButton(githubButton, doc.getString("dev_github"));
                        setupLinkButton(facebookButton, doc.getString("dev_facebook"));
                        setupLinkButton(linkedInButton, doc.getString("dev_linkedin"));
                    } else {
                        Toast.makeText(getContext(), "Failed to load developer links.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
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
                    // Prevent crash if fragment is detached before data loads
                    if (!isAdded()) return;

                    if (task.isSuccessful() && task.getResult() != null) {
                        List<AppUser> moderators = new ArrayList<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String role = document.getString("role");

                            if (role != null && (role.equalsIgnoreCase("admin") || role.equalsIgnoreCase("moderator"))) {
                                String name = document.getString("name");
                                String studentId = document.getString("id");
                                String department = document.getString("department");
                                String profileLink = document.getString("profileLink");
                                String photoUrl = document.getString("photoUrl");

                                Long viewCountLong = document.getLong("viewCount");
                                long viewCount = (viewCountLong != null) ? viewCountLong : 0L;

                                String displayRole = role.substring(0, 1).toUpperCase() + role.substring(1).toLowerCase();

                                if (name != null) {
                                    moderators.add(new AppUser(name, studentId, displayRole, department, viewCount, profileLink, photoUrl));
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

        // Rank purely by views, highest first — role (admin/moderator) no longer affects order.
        Collections.sort(users, (u1, u2) -> Long.compare(u2.viewCount, u1.viewCount));

        for (AppUser user : users) {
            MaterialCardView card = new MaterialCardView(requireContext());
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, 4);
            card.setLayoutParams(cardParams);
            card.setCardElevation(0);
            card.setRadius(0);
            card.setClickable(true);
            card.setFocusable(true);

            // Real ripple on tap for clear press feedback
            android.util.TypedValue rippleAttr = new android.util.TypedValue();
            requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, rippleAttr, true);
            card.setForeground(requireContext().getDrawable(rippleAttr.resourceId));

            LinearLayout itemLayout = new LinearLayout(getContext());
            itemLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            itemLayout.setOrientation(LinearLayout.HORIZONTAL);
            itemLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
            // The text column is two horizontal rows; don't let baseline alignment nudge it against the avatar.
            itemLayout.setBaselineAligned(false);
            itemLayout.setPadding(16, 16, 16, 16);

            MaterialCardView iconCard = new MaterialCardView(requireContext());
            int iconSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 52, getResources().getDisplayMetrics());
            iconCard.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));
            iconCard.setRadius(iconSize / 2f);
            iconCard.setCardBackgroundColor(getResources().getColor(R.color.surface_variant, null));
            iconCard.setCardElevation(0);

            ImageView icon = new ImageView(requireContext());
            icon.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
            icon.setPadding(0, 0, 0, 0);

            // Load profile image from photoUrl or fallback to default
            if (user.photoUrl != null && !user.photoUrl.isEmpty()) {
                Glide.with(requireContext())
                        .load(user.photoUrl)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .circleCrop()
                        .into(icon);
            } else {
                icon.setImageResource(R.drawable.ic_profile);
                icon.setColorFilter(getResources().getColor(R.color.textSecondary, null));
            }
            iconCard.addView(icon);

            // Text column: two rows that each pair a left label with a right-aligned trailing element,
            // so the view count sits on the name's line and the tap arrow sits on the ID line.
            LinearLayout textLayout = new LinearLayout(getContext());
            LinearLayout.LayoutParams textLayoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            textLayoutParams.setMarginStart(16);
            textLayout.setLayoutParams(textLayoutParams);
            textLayout.setOrientation(LinearLayout.VERTICAL);

            int rowGap = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());

            // Top row: name (fills the width) + view count (smaller, trailing).
            LinearLayout topRow = new LinearLayout(getContext());
            topRow.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView nameRoleText = new TextView(getContext());
            nameRoleText.setText(user.name);
            nameRoleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            nameRoleText.setTextColor(getResources().getColor(R.color.textPrimary, null));
            nameRoleText.setTypeface(null, android.graphics.Typeface.BOLD);
            nameRoleText.setMaxLines(1);
            nameRoleText.setEllipsize(android.text.TextUtils.TruncateAt.END);
            nameRoleText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

            TextView viewCountText = new TextView(getContext());
            viewCountText.setText("Views: " + user.viewCount);
            viewCountText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            viewCountText.setTextColor(getResources().getColor(R.color.textSecondary, null));
            viewCountText.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams viewParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            viewParams.setMarginStart(rowGap);
            viewCountText.setLayoutParams(viewParams);

            topRow.addView(nameRoleText);
            topRow.addView(viewCountText);

            // Bottom row: ID • Dept • Role (fills the width) + tap arrow (only when a profile link exists).
            LinearLayout bottomRow = new LinearLayout(getContext());
            bottomRow.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            bottomRow.setOrientation(LinearLayout.HORIZONTAL);
            bottomRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView studentIdText = new TextView(getContext());
            String idText = (user.studentId != null ? user.studentId : "N/A");
            String deptText = (user.department != null ? " • " + user.department : "");
            String roleText = " • " + user.role;
            studentIdText.setText(idText + deptText + roleText);
            studentIdText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            studentIdText.setTextColor(getResources().getColor(R.color.textSecondary, null));
            studentIdText.setMaxLines(1);
            studentIdText.setEllipsize(android.text.TextUtils.TruncateAt.END);
            studentIdText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

            bottomRow.addView(studentIdText);

            if (user.profileLink != null && !user.profileLink.isEmpty()) {
                ImageView rightArrow = new ImageView(requireContext());
                int arrowSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
                LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(arrowSize, arrowSize);
                arrowParams.setMarginStart(rowGap);
                rightArrow.setLayoutParams(arrowParams);
                rightArrow.setImageResource(R.drawable.ic_right);
                rightArrow.setColorFilter(getResources().getColor(R.color.textSecondary, null));
                bottomRow.addView(rightArrow);
            }

            textLayout.addView(topRow);
            textLayout.addView(bottomRow);

            itemLayout.addView(iconCard);
            itemLayout.addView(textLayout);

            card.addView(itemLayout);

            // Click to open profile link
            String finalLink = user.profileLink;
            card.setOnClickListener(v -> {
                if (finalLink == null || finalLink.isEmpty()) {
                    Toast.makeText(getContext(), "No profile link set", Toast.LENGTH_SHORT).show();
                    return;
                }
                String url = finalLink;
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getContext(), "Could not open link", Toast.LENGTH_SHORT).show();
                }
            });

            moderatorListContainer.addView(card);
        }
    }

    private void shareApp() {
        AnalyticsHelper.logShareEvent("app_share");
        buildInviteMessage(message -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject));
            shareIntent.putExtra(Intent.EXTRA_TEXT, message);
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_chooser_title)));
        });
    }

    /** Copies the same invite text the share sheet sends, for pasting into group chats, bios, etc. */
    private void copyInvite() {
        AnalyticsHelper.logShareEvent("app_copy");
        buildInviteMessage(message -> {
            Context ctx = getContext();
            if (ctx == null) return;
            ClipboardManager clipboard = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.share_subject), message));
                Toast.makeText(ctx, R.string.invite_copied, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Builds the invite text once (pitch + the configured download link, if any) and hands it to
     * {@code action}, so Share and Copy always send exactly the same message.
     */
    private void buildInviteMessage(OnInviteMessage action) {
        db.collection("appConfig").document("main").get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    String message = getString(R.string.share_message_default);
                    if (doc != null && doc.exists()) {
                        String updateLink = doc.getString("updateLink");
                        if (updateLink != null && !updateLink.isEmpty()) {
                            message += getString(R.string.share_message_link, updateLink);
                        }
                    }
                    action.onReady(message);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    action.onReady(getString(R.string.share_message_default));
                });
    }

    private interface OnInviteMessage {
        void onReady(String message);
    }
}