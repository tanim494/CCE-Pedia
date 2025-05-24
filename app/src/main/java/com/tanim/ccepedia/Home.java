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

        // Root ScrollView
        ScrollView scrollView = new ScrollView(getContext());
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        scrollView.setBackgroundResource(R.drawable.default_bg);

        // Vertical LinearLayout inside ScrollView
        LinearLayout mainLayout = new LinearLayout(getContext());
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        int paddingPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        mainLayout.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        scrollView.addView(mainLayout);

        // Load Notices (Multiple)
        db.collection("notices")
                //.orderBy("timestamp", Query.Direction.DESCENDING) // Optional sorting if you have timestamp
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
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
                    TextView errorNotice = createNoticeTextView("Failed to load notices.");
                    mainLayout.addView(errorNotice);
                });

        // Load Messages
        db.collection("messages")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
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
                    TextView errorView = createMessageTextView("Failed to load messages.");
                    mainLayout.addView(errorView);
                });

        return scrollView;
    }

    // Helper method to create styled notice TextView
    private TextView createNoticeTextView(String text) {
        TextView noticeView = new TextView(getContext());
        noticeView.setText(text);
        noticeView.setTextSize(18);
        noticeView.setTextColor(getResources().getColor(android.R.color.holo_orange_dark)); // matches XML notice color
        noticeView.setBackgroundResource(R.drawable.buttonbg); // as per your updated XML for notice box
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        noticeView.setPadding(padding, padding, padding, padding);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, padding); // bottom margin for spacing
        noticeView.setLayoutParams(params);

        // Optional elevation for shadow effect (requires API 21+)
        noticeView.setElevation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics()));

        return noticeView;
    }

    // Helper method to create styled message TextView
    private TextView createMessageTextView(String text) {
        TextView msgView = new TextView(getContext());
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
