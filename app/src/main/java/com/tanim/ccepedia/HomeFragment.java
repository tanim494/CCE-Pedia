package com.tanim.ccepedia;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;


public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private FirebaseFirestore db;

    private RecyclerView noticesRecyclerView;
    private RecyclerView latestUpdatesRecyclerView;
    private ImageView homeBannerImage;
    private TextView tvLatestAnnouncementsTitle, tvHubTitle;

    private NoticeAdapter noticeAdapter;
    private LatestUpdatesAdapter latestUpdatesAdapter;

    private Button adminBtn, uploadBtn;
    private LinearLayout controlLayout;

    private final List<String> latestUpdatesList = new ArrayList<>();
    private final List<Notice> noticesList = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        controlLayout = view.findViewById(R.id.controlLayout);
        adminBtn = view.findViewById(R.id.adminBtn);
        uploadBtn = view.findViewById(R.id.uploadBtn);

        noticesRecyclerView = view.findViewById(R.id.noticesRecyclerView);
        latestUpdatesRecyclerView = view.findViewById(R.id.latestUpdatesRecyclerView);
        tvLatestAnnouncementsTitle = view.findViewById(R.id.tv_latest_announcements_title);
        homeBannerImage = view.findViewById(R.id.homeBannerImage);
        tvHubTitle = view.findViewById(R.id.tv_hub_title);

        db = FirebaseFirestore.getInstance();

        setupNotices();
        setupBentoGrid(view);
        setupLatestUpdates();
        setupStudentHub(view);

        loadAllNotices();
        loadLatestUpdates();
        loadDynamicBanner();
        setUserData();

        return view;
    }

    private void setupBentoGrid(View v) {
        v.findViewById(R.id.card_community).setOnClickListener(view -> openCommunity());
        
        TextView resTitle = v.findViewById(R.id.tv_res_title);
        String currentSemester = UserData.getInstance().getSemester();
        
        String semesterDisplay;
        if (currentSemester.equalsIgnoreCase("Outgoing")) {
            semesterDisplay = "8th Semester Resources";
        } else {
            String suffix;
            try {
                int sem = Integer.parseInt(currentSemester);
                if (sem == 1) suffix = "st";
                else if (sem == 2) suffix = "nd";
                else if (sem == 3) suffix = "rd";
                else suffix = "th";
                semesterDisplay = sem + suffix + " Semester Resources";
            } catch (Exception e) {
                semesterDisplay = currentSemester + " Semester Resources";
            }
        }
        resTitle.setText(semesterDisplay);

        v.findViewById(R.id.card_resources).setOnClickListener(view -> {
            String semesterId = currentSemester.equalsIgnoreCase("Outgoing") ? "semester_8" : "semester_" + currentSemester;
            openCourseListFragment(semesterId);
        });

        v.findViewById(R.id.card_portal).setOnClickListener(view -> openStudentPortal());
        v.findViewById(R.id.card_tracker).setOnClickListener(view -> openBusTracker());
        v.findViewById(R.id.card_schedule).setOnClickListener(view -> openBusSchedule());
        v.findViewById(R.id.card_java).setOnClickListener(view -> openWebFragment("https://github.com/tanim494/CodeForces"));
    }

    private void loadDynamicBanner() {
        db.collection("appConfig").document("main").get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        String imageUrl = doc.getString("homeBannerUrl");
                        String clickUrl = doc.getString("homeBannerClickUrl");

                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            if (getContext() != null) {
                                Glide.with(getContext())
                                        .load(imageUrl)
                                        .into(homeBannerImage);
                                homeBannerImage.setVisibility(View.VISIBLE);
                                Animation pulse = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse);
                                homeBannerImage.startAnimation(pulse);

                                if (clickUrl != null && !clickUrl.isEmpty()) {
                                    homeBannerImage.setOnClickListener(v -> openWebPage(clickUrl));
                                }
                            }
                        } else {
                            homeBannerImage.setVisibility(View.GONE);
                        }
                    } else {
                        homeBannerImage.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    homeBannerImage.setVisibility(View.GONE);
                });
    }

    private void setupNotices() {
        noticesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(noticesRecyclerView);
        noticeAdapter = new NoticeAdapter(noticesList, this::handleNoticeClick);
        noticesRecyclerView.setAdapter(noticeAdapter);
    }

    private void setupLatestUpdates() {
        latestUpdatesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        latestUpdatesAdapter = new LatestUpdatesAdapter(latestUpdatesList);
        latestUpdatesRecyclerView.setAdapter(latestUpdatesAdapter);
    }

    private void loadAllNotices() {
        db.collection("notices")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    noticesList.clear();
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String text = doc.getString("text");
                            String link = doc.getString("link");

                            if (text != null && !text.isEmpty()) {
                                noticesList.add(new Notice(text, link));
                            }
                        }
                    }

                    if (noticesList.isEmpty()) {
                        noticesRecyclerView.setVisibility(View.GONE);
                    } else {
                        noticesRecyclerView.setVisibility(View.VISIBLE);
                    }

                    noticeAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading announcements", e);
                    noticesRecyclerView.setVisibility(View.GONE);
                });
    }

    private void loadLatestUpdates() {
        db.collection("messages")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    latestUpdatesList.clear();
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String msg = doc.getString("text");
                            if (msg != null) {
                                latestUpdatesList.add(msg);
                            }
                        }
                    }

                    if (latestUpdatesList.isEmpty()) {
                        latestUpdatesRecyclerView.setVisibility(View.GONE);
                        tvLatestAnnouncementsTitle.setVisibility(View.GONE);
                    } else {
                        latestUpdatesRecyclerView.setVisibility(View.VISIBLE);
                        tvLatestAnnouncementsTitle.setVisibility(View.VISIBLE);
                    }
                    latestUpdatesAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading updates", e);
                    latestUpdatesRecyclerView.setVisibility(View.GONE);
                    tvLatestAnnouncementsTitle.setVisibility(View.GONE);
                });
    }

    private void setUserData() {
        String userRole = UserData.getInstance().getRole();

        if (userRole != null && userRole.equalsIgnoreCase("admin")) {
            controlLayout.setVisibility(View.VISIBLE);
            adminBtn.setVisibility(View.VISIBLE);
            adminBtn.setOnClickListener(v -> openAdminMode());

            uploadBtn.setVisibility(View.VISIBLE);
            uploadBtn.setOnClickListener(v -> openFileUpload());
        } else if (userRole != null && userRole.equalsIgnoreCase("moderator")) {
            controlLayout.setVisibility(View.VISIBLE);
            uploadBtn.setVisibility(View.VISIBLE);
            uploadBtn.setOnClickListener(v -> openFileUpload());
        }
    }

    private void handleNoticeClick(String link) {
        if (link != null && !link.isEmpty()) {
            openWebPage(link);
        } else {
            Toast.makeText(getContext(), "No external link available for this notice.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openWebFragment(String url) {
        WebFragment fragment = WebFragment.newInstance(url);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.Midcontainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void openStudentPortal() {
        StudentPortalFragment portalFragment = new StudentPortalFragment();

        getParentFragmentManager().beginTransaction()
                .replace(R.id.Midcontainer, portalFragment)
                .addToBackStack(null)
                .commit();
    }

    private void openBusTracker() {
        String url = "https://transport.iiuc.ac.bd/student/home";
        openCustomTab(url);
    }

    private void openCustomTab(String url) {
        if (getContext() == null) return;
        try {
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();

            builder.setToolbarColor(ContextCompat.getColor(getContext(), R.color.Green));

            CustomTabsIntent customTabsIntent = builder.build();
            customTabsIntent.launchUrl(requireContext(), Uri.parse(url));

        } catch (Exception e) {
            Log.e(TAG, "Custom Tabs failed, falling back to external browser.", e);
            openWebPage(url);
        }
    }

    private void openCourseListFragment(String semesterId) {
        CourseListFragment fragment = CourseListFragment.newInstance(semesterId);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.Midcontainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void openBusSchedule() {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.Midcontainer, new BusScheduleFragment());
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void setupStudentHub(View v) {
        String userName = UserData.getInstance().getName();
        if (userName != null && !userName.isEmpty() && isAdded()) {
            String firstName = userName.split(" ")[0];
            tvHubTitle.setText(getString(R.string.hub_greeting, firstName));
        }

        View shareCard = v.findViewById(R.id.card_share_app);
        if (shareCard != null) {
            shareCard.setOnClickListener(view -> shareApp());
        }
    }

    private void shareApp() {
        db.collection("appConfig").document("main").get()
                .addOnSuccessListener(doc -> {
                    String shareMessage = "Download IIUC Pedia - Your ultimate IIUC resource hub! 🎓\n\n";
                    if (doc != null && doc.exists()) {
                        String updateLink = doc.getString("updateLink");
                        if (updateLink != null && !updateLink.isEmpty()) {
                            shareMessage += "Get it here: " + updateLink;
                        }
                    }

                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "IIUC Pedia App");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                    startActivity(Intent.createChooser(shareIntent, "Share IIUC Pedia via"));
                })
                .addOnFailureListener(e -> {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, "Download IIUC Pedia - Your ultimate university resource hub! 🎓");
                    startActivity(Intent.createChooser(shareIntent, "Share IIUC Pedia via"));
                });
    }

    private void openAdminMode() {
        AdminFragment adminFragment = new AdminFragment();
        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.Midcontainer, adminFragment)
                .addToBackStack(null)
                .commit();
    }

    private void openFileUpload() {
        UploadFileFragment uploadFragment = new UploadFileFragment();
        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.Midcontainer, uploadFragment)
                .addToBackStack(null)
                .commit();
    }

    private void openCommunity() {
        Intent intent = new Intent(requireContext(), CommunityActivity.class);
        requireActivity().startActivity(intent);
    }

    private void openWebPage(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    public static class Notice {
        public final String text;
        public final String link;

        public Notice(String text, String link) {
            this.text = text;
            this.link = link;
        }
    }

    public static class NoticeAdapter extends RecyclerView.Adapter<NoticeAdapter.ViewHolder> {
        private final List<Notice> items;
        private final NoticeClickListener listener;

        public interface NoticeClickListener {
            void onNoticeClick(String link);
        }

        public NoticeAdapter(List<Notice> items, NoticeClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notice, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Notice item = items.get(position);
            holder.noticeText.setText(item.text);

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNoticeClick(item.link);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView noticeText;

            public ViewHolder(View itemView) {
                super(itemView);
                noticeText = itemView.findViewById(R.id.noticeContentTextView);
            }
        }
    }

    public static class LatestUpdatesAdapter extends RecyclerView.Adapter<LatestUpdatesAdapter.ViewHolder> {
        private final List<String> data;

        public LatestUpdatesAdapter(List<String> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_latest_update, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.textView.setText(data.get(position));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public final TextView textView;

            public ViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.update_text);
            }
        }
    }
}