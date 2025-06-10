package com.tanim.ccepedia;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class Home extends Fragment {

    FirebaseFirestore db;

    @Override
    public ScrollView onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();

        // Root ScrollView with requireContext()
        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // Vertical LinearLayout inside ScrollView
        LinearLayout mainLayout = new LinearLayout(requireContext());
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        int paddingPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        mainLayout.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        scrollView.addView(mainLayout);

        // Load Notices
        db.collection("notices")
                //.orderBy("timestamp", Query.Direction.DESCENDING) // Optional sorting if needed
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return;  // Ensure fragment still attached

                    if (queryDocumentSnapshots.isEmpty()) {
                        TextView noNotice = createNoticeTextView("No notices at the moment.");
                        mainLayout.addView(noNotice);
                    } else {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String noticeText = doc.getString("text");
                            String link = doc.getString("link");

                            if (noticeText != null && !noticeText.isEmpty()) {
                                TextView noticeView = createNoticeTextView("🔔 Notice: " + noticeText);

                                if (link != null && !link.isEmpty()) {
                                    noticeView.setClickable(true);
                                    noticeView.setOnClickListener(v -> {
                                        Intent intent = new Intent(Intent.ACTION_VIEW);
                                        intent.setData(Uri.parse(link));
                                        startActivity(intent);
                                    });
                                }
                                mainLayout.addView(noticeView);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    TextView errorNotice = createNoticeTextView("Failed to load notices.");
                    mainLayout.addView(errorNotice);
                });

        // Load Messages
        db.collection("messages")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return;

                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String msg = doc.getString("text");
                            if (msg != null && !msg.isEmpty()) {
                                TextView msgView = createMessageTextView("🗨 Tanim: " + msg);
                                mainLayout.addView(msgView);
                            }
                        }
                    } else {
                        TextView noMsg = createMessageTextView("No messages at the moment.");
                        mainLayout.addView(noMsg);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    TextView errorView = createMessageTextView("Failed to load messages.");
                    mainLayout.addView(errorView);
                });

        return scrollView;
    }

    // Helper method to create styled notice TextView
    private TextView createNoticeTextView(String text) {
        TextView noticeView = new TextView(requireContext());
        noticeView.setText(text);
        noticeView.setTextSize(16);
        noticeView.setTypeface(null, android.graphics.Typeface.BOLD);
        noticeView.setBackgroundResource(R.drawable.buttonbg); // your notice background drawable
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        noticeView.setPadding(padding, padding, padding, padding);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, padding); // bottom margin for spacing
        noticeView.setLayoutParams(params);

        // Optional elevation for shadow effect (API 21+)
        noticeView.setElevation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics()));

        return noticeView;
    }

    // Helper method to create styled message TextView
    private TextView createMessageTextView(String text) {
        TextView msgView = new TextView(requireContext());
        msgView.setText(text);
        msgView.setTextSize(16);
        msgView.setTextColor(getResources().getColor(android.R.color.white));
        msgView.setBackgroundResource(R.drawable.textbg);
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        msgView.setPadding(padding, padding, padding, padding);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, padding / 2);
        msgView.setLayoutParams(params);

        return msgView;
    }
}
