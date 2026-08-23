package com.tanim.ccepedia;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.json.JSONObject;


public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private FirebaseFirestore db;

    private RecyclerView noticesRecyclerView;
    private RecyclerView latestUpdatesRecyclerView;
    private ImageView homeBannerImage;
    private MaterialCardView homeBannerCard;
    private TextView tvLatestAnnouncementsTitle, tvHubTitle, tvHubEyebrow;
    private SwipeRefreshLayout swipeRefreshLayout;

    private NoticeAdapter noticeAdapter;
    private LatestUpdatesAdapter latestUpdatesAdapter;

    private Button adminBtn, uploadBtn;
    private LinearLayout controlLayout;

    // Unread-count pill on the community card; refreshed in onResume.
    private TextView communityUnreadBadge;
    // The community card + its icon chip and subtitle: the whole row lights up when unread > 0.
    private MaterialCardView communityCard;
    private ImageView communityIcon;
    private TextView communitySubtitle;
    // Continuous "breathing" pulse on the icon chip while unread > 0; cancelled when caught up / paused.
    private ObjectAnimator communityPulse;

    // "Today's Classes" card (personal routine): full list of programmatic rows (no collapse).
    private MaterialCardView cardRoutine;
    private LinearLayout routineContainer;
    private TextView tvRoutineEyebrow;
    private TextView tvRoutineStatus;

    /** Assumed class length (minutes) — how long a started class is treated as "in session". */
    private static final int ASSUMED_CLASS_MINUTES = 90;

    // Live-ticking state. The rows are inflated once per routine change (renderRoutineCard) and their
    // views cached here; the 1-second ticker only rewrites the dynamic bits (countdown, colours, pills)
    // so it never re-inflates or touches disk. Ticks run only while Home is resumed.
    private final Handler routineHandler = new Handler(Looper.getMainLooper());
    private Runnable routineTicker;
    private final List<RoutineRow> routineRows = new ArrayList<>();
    private List<RoutineEntry> todaysClasses;   // today's entries, sorted (null until built)
    private int routineToday = -1;              // the day index the rows were built for

    /** Cached view refs for one class row, so per-second updates skip findViewById. */
    private static class RoutineRow {
        final TextView time, course, badge;
        final View dot;
        RoutineRow(View row) {
            time = row.findViewById(R.id.tv_time);
            course = row.findViewById(R.id.tv_course);
            badge = row.findViewById(R.id.tv_badge);
            dot = row.findViewById(R.id.v_dot);
        }
    }

    // Tracks in-flight loads so pull-to-refresh spinner is dismissed only once both finish
    private boolean noticesLoading = false;
    private boolean updatesLoading = false;

    private final List<String> latestUpdatesList = new ArrayList<>();
    private final List<Notice> noticesList = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        controlLayout = view.findViewById(R.id.controlLayout);
        adminBtn = view.findViewById(R.id.adminBtn);
        uploadBtn = view.findViewById(R.id.uploadBtn);

        noticesRecyclerView = view.findViewById(R.id.noticesRecyclerView);
        latestUpdatesRecyclerView = view.findViewById(R.id.latestUpdatesRecyclerView);
        tvLatestAnnouncementsTitle = view.findViewById(R.id.tv_latest_announcements_title);
        homeBannerImage = view.findViewById(R.id.homeBannerImage);
        homeBannerCard = view.findViewById(R.id.homeBannerCard);
        tvHubTitle = view.findViewById(R.id.tv_hub_title);
        tvHubEyebrow = view.findViewById(R.id.tv_hub_eyebrow);

        db = FirebaseFirestore.getInstance();

        AnalyticsHelper.logScreenView("Home", TAG);

        setupNotices();
        setupBentoGrid(view);
        setupLatestUpdates();
        setupStudentHub(view);
        setupSwipeRefresh();

        loadAllContent();
        setUserData();

        return view;
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout == null) return;
        swipeRefreshLayout.setColorSchemeResources(R.color.Green, R.color.Blue);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            AnalyticsHelper.logUserAction("pull_to_refresh", "home", "refresh");
            loadAllContent();
        });
    }

    private void loadAllContent() {
        loadAllNotices();
        loadLatestUpdates();
        loadDynamicBanner();
    }

    // Dismiss the refresh spinner only after both list loads have completed
    private void onLoadFinished() {
        if (!noticesLoading && !updatesLoading && swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void setupBentoGrid(View v) {
        communityCard = v.findViewById(R.id.card_community);
        communityCard.setOnClickListener(view -> openCommunity());
        communityUnreadBadge = v.findViewById(R.id.community_unread_badge);
        communityIcon = v.findViewById(R.id.community_icon);
        communitySubtitle = v.findViewById(R.id.community_subtitle);
        
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

        final String resourceLabel = semesterDisplay;
        v.findViewById(R.id.card_resources).setOnClickListener(view -> {
            String semesterId = currentSemester.equalsIgnoreCase("Outgoing") ? "semester_8" : "semester_" + currentSemester;
            AnalyticsHelper.logResourceAccess("semester_resources", resourceLabel, currentSemester);
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
                    if (!isAdded() || getContext() == null) return;
                    if (doc != null && doc.exists()) {
                        String imageUrl = doc.getString("homeBannerUrl");
                        String clickUrl = doc.getString("homeBannerClickUrl");

                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            if (getContext() != null) {
                                Glide.with(getContext())
                                        .load(imageUrl)
                                        .into(homeBannerImage);
                                homeBannerCard.setVisibility(View.VISIBLE);
                                Animation pulse = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse);
                                homeBannerImage.startAnimation(pulse);

                                if (clickUrl != null && !clickUrl.isEmpty()) {
                                    homeBannerImage.setOnClickListener(v -> openWebPage(clickUrl));
                                }
                            }
                        } else {
                            homeBannerCard.setVisibility(View.GONE);
                        }
                    } else {
                        homeBannerCard.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    homeBannerCard.setVisibility(View.GONE);
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
        noticesLoading = true;
        db.collection("notices")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded() || noticesRecyclerView == null) return;
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
                    noticesLoading = false;
                    onLoadFinished();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading announcements", e);
                    AnalyticsHelper.logError("load_notices", e.getMessage() != null ? e.getMessage() : "unknown");
                    noticesLoading = false;
                    onLoadFinished();
                    if (!isAdded() || noticesRecyclerView == null) return;
                    noticesRecyclerView.setVisibility(View.GONE);
                    showErrorWithRetry(getString(R.string.error_load_notices), this::loadAllNotices);
                });
    }

    private void loadLatestUpdates() {
        updatesLoading = true;
        db.collection("messages")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded() || latestUpdatesRecyclerView == null) return;
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
                    updatesLoading = false;
                    onLoadFinished();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading updates", e);
                    AnalyticsHelper.logError("load_updates", e.getMessage() != null ? e.getMessage() : "unknown");
                    updatesLoading = false;
                    onLoadFinished();
                    if (!isAdded() || latestUpdatesRecyclerView == null) return;
                    latestUpdatesRecyclerView.setVisibility(View.GONE);
                    tvLatestAnnouncementsTitle.setVisibility(View.GONE);
                });
    }

    private void showErrorWithRetry(String message, Runnable retryAction) {
        if (!isAdded() || getContext() == null) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.error_title)
                .setMessage(message)
                .setPositiveButton(R.string.retry, (d, w) -> retryAction.run())
                .setNegativeButton(R.string.cancel, null)
                .show();
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
            AnalyticsHelper.logNoticeClick(link);
            openWebPage(link);
        } else {
            Toast.makeText(getContext(), R.string.no_notice_link, Toast.LENGTH_SHORT).show();
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
        // Header shows today's date + the next prayer time instead of a name greeting
        // (the user's name already lives in the top bar).
        updatePrayerHeader();
        setupRoutineCard(v);

        View shareBtn = v.findViewById(R.id.btn_share_app);
        if (shareBtn != null) {
            shareBtn.setOnClickListener(view -> shareApp());
        }
        View copyBtn = v.findViewById(R.id.btn_copy_invite);
        if (copyBtn != null) {
            copyBtn.setOnClickListener(view -> copyInvite());
        }
    }

    // ===================== Today's Classes (personal routine) =====================
    // Mirrors the prayer-header pattern: paint instantly from the SharedPreferences cache, then
    // refresh from users/{uid} in the background. Rows are added programmatically to a LinearLayout
    // (not a nested RecyclerView) so the card lives happily inside the NestedScrollView. The toggle
    // is the user's show/hide button; its state persists in RoutineStore.

    private void setupRoutineCard(View v) {
        cardRoutine = v.findViewById(R.id.card_routine);
        routineContainer = v.findViewById(R.id.routine_container);
        tvRoutineEyebrow = v.findViewById(R.id.tv_routine_eyebrow);
        tvRoutineStatus = v.findViewById(R.id.tv_routine_status);

        if (cardRoutine != null) {
            cardRoutine.setOnClickListener(view -> openRoutineEditor());
        }

        // Check display preference for routine visibility
        if (getContext() != null && !DisplayPrefs.isShowRoutine(getContext())) {
            cardRoutine.setVisibility(View.GONE);
            return;
        }
        cardRoutine.setVisibility(View.VISIBLE);

        updateRoutineCard();
    }

    private void openRoutineEditor() {
        if (getContext() == null) return;
        startActivity(new Intent(requireContext(), RoutineEditorActivity.class));
    }

    /** Renders the card from cache (instant), then refreshes it from Firestore. */
    private void updateRoutineCard() {
        if (!isAdded() || getContext() == null) return;
        if (!DisplayPrefs.isShowRoutine(getContext())) {
            if (cardRoutine != null) cardRoutine.setVisibility(View.GONE);
            stopRoutineTicker();
            return;
        }
        if (cardRoutine != null) cardRoutine.setVisibility(View.VISIBLE);
        renderRoutineCard();
        refreshRoutine();
    }

    /** (Re)builds the row structure from the cached routine, paints the live state, and starts ticking. */
    private void renderRoutineCard() {
        if (routineContainer == null || getContext() == null) return;

        routineContainer.removeAllViews();
        routineRows.clear();
        todaysClasses = null;

        int cPrimary = ContextCompat.getColor(requireContext(), R.color.textPrimary);
        int cSecondary = ContextCompat.getColor(requireContext(), R.color.textSecondary);

        List<RoutineEntry> all = RoutineStore.loadCache(requireContext());
        if (all.isEmpty()) {
            setHeader(getString(R.string.routine_eyebrow_routine),
                    getString(R.string.routine_status_setup), cPrimary);
            stopRoutineTicker();
            return;
        }

        int today = RoutineStore.todayIndex(); // -1 on Thu/Fri
        routineToday = today;
        List<RoutineEntry> todays = new ArrayList<>();
        if (today >= 0) {
            for (RoutineEntry e : all) {
                if (e.getDay() == today) todays.add(e);
            }
        }

        if (todays.isEmpty()) {
            // Nothing today: header says so, body points to the next upcoming class (any day).
            String eyebrow = today >= 0
                    ? RoutineStore.dayName(today)
                    : getString(R.string.routine_eyebrow_routine);
            setHeader(eyebrow, getString(R.string.routine_status_no_classes), cSecondary);

            NextClassInfo nextClass = findNextUpcomingClass(all, today);
            if (nextClass != null) {
                addRoutineMessage(getString(R.string.routine_no_classes_today_next,
                        RoutineStore.dayName(nextClass.entry.getDay()),
                        RoutineStore.format12(nextClass.entry.getStart()),
                        nextClass.entry.getCourse(),
                        formatCountdown(nextClass.minutesUntil)));
            }
            stopRoutineTicker();
            return;
        }

        todaysClasses = todays;

        // Inflate one row per class with its static text; the dynamic bits (colour, dot, pill) are
        // painted by updateRoutineDynamic() so the ticker can refresh them without re-inflating.
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (RoutineEntry e : todays) {
            View row = inflater.inflate(R.layout.item_routine_today, routineContainer, false);
            RoutineRow holder = new RoutineRow(row);
            holder.time.setText(RoutineStore.format12(e.getStart()));
            holder.course.setText(e.getCourse());
            routineRows.add(holder);
            routineContainer.addView(row);
        }

        updateRoutineDynamic(); // paint the current state now
        startRoutineTicker();   // keep the countdown live
    }

    /**
     * Cheap per-second refresh: recomputes the current/next class and rewrites the countdown, row
     * colours and pills in place — no inflation, no disk I/O. Rebuilds fully only on day rollover.
     */
    private void updateRoutineDynamic() {
        if (!isAdded() || getContext() == null) return;
        if (todaysClasses == null || todaysClasses.isEmpty() || routineRows.isEmpty()) return;

        // Crossed midnight while Home was open → the day's classes changed; rebuild.
        if (routineToday != RoutineStore.todayIndex()) {
            renderRoutineCard();
            return;
        }

        int cPrimary = ContextCompat.getColor(requireContext(), R.color.textPrimary);
        int cSecondary = ContextCompat.getColor(requireContext(), R.color.textSecondary);
        int cAccent = ContextCompat.getColor(requireContext(), R.color.accent_schedule);
        int cAccentSoft = ContextCompat.getColor(requireContext(), R.color.accent_schedule_soft);

        int nowSec = RoutineStore.nowSeconds();
        int nowMin = nowSec / 60;

        // Current class = the latest one already started; next = the first not yet started.
        int currentIdx = -1;
        for (int i = 0; i < todaysClasses.size(); i++) {
            if (RoutineStore.startMinutes(todaysClasses.get(i).getStart()) <= nowMin) currentIdx = i;
            else break;
        }
        int nextIdx = -1;
        for (int i = 0; i < todaysClasses.size(); i++) {
            if (RoutineStore.startMinutes(todaysClasses.get(i).getStart()) > nowMin) { nextIdx = i; break; }
        }
        // The current class's real end (explicit end time, else assumed length capped at the next start).
        int curEndSec = (currentIdx != -1) ? effectiveEndSeconds(currentIdx, nextIdx) : -1;
        boolean inSession = currentIdx != -1 && nowSec < curEndSec;

        // Header: weekday + a live status line.
        if (tvRoutineEyebrow != null) {
            tvRoutineEyebrow.setText(RoutineStore.dayName(routineToday));
            tvRoutineEyebrow.setVisibility(View.VISIBLE);
        }
        if (tvRoutineStatus != null) {
            if (inSession) {
                // Count down to the class's real end (explicit end time, else the fallback).
                tvRoutineStatus.setText(getString(R.string.routine_status_in_class,
                        todaysClasses.get(currentIdx).getCourse(), formatLive(curEndSec - nowSec)));
                tvRoutineStatus.setTextColor(cAccent);
            } else if (nextIdx != -1) {
                int startSec = RoutineStore.startMinutes(todaysClasses.get(nextIdx).getStart()) * 60;
                tvRoutineStatus.setText(getString(R.string.routine_status_next,
                        todaysClasses.get(nextIdx).getCourse(), formatLive(startSec - nowSec)));
                tvRoutineStatus.setTextColor(cPrimary);
            } else {
                tvRoutineStatus.setText(getString(R.string.routine_status_done));
                tvRoutineStatus.setTextColor(cSecondary);
            }
        }

        // Rows.
        for (int i = 0; i < routineRows.size() && i < todaysClasses.size(); i++) {
            RoutineRow r = routineRows.get(i);
            boolean isCurrent = inSession && i == currentIdx;
            boolean isNext = i == nextIdx;
            boolean started = RoutineStore.startMinutes(todaysClasses.get(i).getStart()) <= nowMin;
            boolean isPast = started && !isCurrent;

            int color = isCurrent ? cAccent : (isPast ? cSecondary : cPrimary);
            r.time.setTextColor(color);
            r.course.setTextColor(color);

            // The dot marks the row in focus: the current class, or (before anything starts) the next.
            boolean focus = isCurrent || (!inSession && isNext);
            r.dot.setVisibility(focus ? View.VISIBLE : View.INVISIBLE);

            if (isCurrent) {
                showBadge(r.badge, getString(R.string.routine_badge_now), cAccent, Color.WHITE);
            } else if (isNext) {
                showBadge(r.badge, getString(R.string.routine_badge_next), cAccentSoft, cAccent);
            } else {
                r.badge.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Effective end (seconds since midnight) of the class at {@code idx}. Uses its explicit end time
     * when set; otherwise assumes {@link #ASSUMED_CLASS_MINUTES}, capped so it never runs past the
     * next class's start — so a class with a long gap before the next one doesn't read as hours-long.
     */
    private int effectiveEndSeconds(int idx, int nextIdx) {
        int startMin = RoutineStore.startMinutes(todaysClasses.get(idx).getStart());
        int explicitEnd = RoutineStore.startMinutes(todaysClasses.get(idx).getEnd());
        if (explicitEnd > startMin) return explicitEnd * 60;

        int endMin = startMin + ASSUMED_CLASS_MINUTES;
        if (nextIdx != -1) {
            endMin = Math.min(endMin, RoutineStore.startMinutes(todaysClasses.get(nextIdx).getStart()));
        }
        return endMin * 60;
    }

    /** Starts the 1-second countdown ticker (no-op if already running). */
    private void startRoutineTicker() {
        if (routineTicker != null) return;
        routineTicker = new Runnable() {
            @Override
            public void run() {
                updateRoutineDynamic();
                routineHandler.postDelayed(this, 1000);
            }
        };
        routineHandler.postDelayed(routineTicker, 1000);
    }

    /** Stops the countdown ticker so it doesn't run while Home is hidden. */
    private void stopRoutineTicker() {
        if (routineTicker != null) {
            routineHandler.removeCallbacks(routineTicker);
            routineTicker = null;
        }
    }

    /** Sets the routine card's header eyebrow + status line (used for the empty / no-class states). */
    private void setHeader(String eyebrow, String status, int statusColor) {
        if (tvRoutineEyebrow != null) {
            tvRoutineEyebrow.setText(eyebrow == null ? "" : eyebrow);
            tvRoutineEyebrow.setVisibility(eyebrow == null || eyebrow.isEmpty() ? View.GONE : View.VISIBLE);
        }
        if (tvRoutineStatus != null) {
            tvRoutineStatus.setText(status == null ? "" : status);
            tvRoutineStatus.setTextColor(statusColor);
        }
    }

    /** Tints the pill background and sets its label + text colour, then shows it. */
    private void showBadge(TextView badge, String label, int bgColor, int textColor) {
        if (badge == null) return;
        badge.setText(label);
        badge.setTextColor(textColor);
        badge.setBackgroundTintList(ColorStateList.valueOf(bgColor));
        badge.setVisibility(View.VISIBLE);
    }

    /** Live countdown: "MM:SS" under an hour (ticks each second), "1 h 20 min" beyond. */
    private String formatLive(int totalSeconds) {
        if (totalSeconds < 0) totalSeconds = 0;
        if (totalSeconds >= 3600) {
            int m = totalSeconds / 60;
            int h = m / 60, mm = m % 60;
            return mm == 0 ? h + " h" : h + " h " + mm + " min";
        }
        int m = totalSeconds / 60, s = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", m, s);
    }

    /** Adds a single centered helper line (empty routine / no classes today) to the container. */
    private void addRoutineMessage(String message) {
        if (getContext() == null || routineContainer == null) return;
        TextView tv = new TextView(requireContext());
        tv.setText(message);
        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary));
        tv.setTextSize(13f);
        int pad = Math.round(4 * getResources().getDisplayMetrics().density);
        tv.setPadding(0, pad, 0, pad);
        routineContainer.addView(tv);
    }

    /**
     * Finds the next upcoming class after today.
     * Scans days in IIUC order (today+1 → Wed, then Sat → today-1) and returns earliest class with minutes until.
     */
    private NextClassInfo findNextUpcomingClass(List<RoutineEntry> all, int todayIndex) {
        if (all == null || all.isEmpty()) return null;

        int nowMin = RoutineStore.nowMinutes();
        int daysInWeek = RoutineStore.DAY_NAMES.length;
        Calendar now = Calendar.getInstance();
        int todayDayOfWeek = now.get(Calendar.DAY_OF_WEEK);

        // Check each future day in order (wrapping Sat-Wed)
        for (int offset = 1; offset <= daysInWeek; offset++) {
            int day = (todayIndex + offset) % daysInWeek;
            RoutineEntry earliestToday = null;

            for (RoutineEntry e : all) {
                if (e.getDay() != day) continue;
                int startMin = RoutineStore.startMinutes(e.getStart());
                if (startMin < 0) continue;
                // If today, only consider future classes; for future days, any class counts
                if (day == todayIndex && startMin < nowMin) continue;
                if (earliestToday == null || startMin < RoutineStore.startMinutes(earliestToday.getStart())) {
                    earliestToday = e;
                }
            }
            if (earliestToday != null) {
                int startMin = RoutineStore.startMinutes(earliestToday.getStart());
                int minutesUntil = calculateMinutesUntil(todayDayOfWeek, nowMin, day, startMin, offset);
                return new NextClassInfo(earliestToday, minutesUntil);
            }
        }
        return null;
    }

    /**
     * Calculates minutes from now to a class on a specific day.
     * @param todayDayOfWeek Calendar.DAY_OF_WEEK for today
     * @param nowMin Minutes since midnight now
     * @param targetDay RoutineStore day index (0=Sat...4=Wed)
     * @param targetStartMin Class start minutes since midnight
     * @param dayOffset Days ahead (1-5)
     */
    private int calculateMinutesUntil(int todayDayOfWeek, int nowMin, int targetDay, int targetStartMin, int dayOffset) {
        // Map RoutineStore day index to Calendar day of week
        int[] dayMap = {Calendar.SATURDAY, Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY};
        int targetDayOfWeek = dayMap[targetDay];

        int daysDiff;
        if (targetDayOfWeek >= todayDayOfWeek) {
            daysDiff = targetDayOfWeek - todayDayOfWeek;
        } else {
            daysDiff = 7 - (todayDayOfWeek - targetDayOfWeek);
        }

        // If same day, use time difference; otherwise full days + time
        if (daysDiff == 0) {
            return targetStartMin - nowMin;
        } else {
            return daysDiff * 24 * 60 - nowMin + targetStartMin;
        }
    }

    /** Formats minutes into "d D:hh H:mm M" string. */
    private String formatCountdown(int totalMinutes) {
        if (totalMinutes < 0) totalMinutes = 0;
        int days = totalMinutes / (24 * 60);
        int hours = (totalMinutes % (24 * 60)) / 60;
        int minutes = totalMinutes % 60;
        return String.format(Locale.getDefault(), "%d D:%02d H:%02d M", days, hours, minutes);
    }

    private static class NextClassInfo {
        final RoutineEntry entry;
        final int minutesUntil;

        NextClassInfo(RoutineEntry entry, int minutesUntil) {
            this.entry = entry;
            this.minutesUntil = minutesUntil;
        }
    }

    /** Reads users/{uid}.routine, updates the cache, and re-renders (guarded by isAdded()). */
    private void refreshRoutine() {
        if (getContext() == null || db == null) return;
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        db.collection("users").document(auth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded() || getContext() == null) return;
                    RoutineStore.saveCache(requireContext(), RoutineStore.fromFirestore(doc.get("routine")));
                    renderRoutineCard();
                    // Self-heal reminders on every Home refresh (covers app launch + cross-device edits).
                    ClassReminderScheduler.sync(requireContext());
                })
                .addOnFailureListener(e -> {
                    // Keep the cached list; the card stays populated offline.
                    Log.w(TAG, "Routine refresh failed", e);
                });
    }

    // ===================== Header: tri-calendar date + prayer times =====================
    // Line 1 (date): English (device) + Bengali in Bangla script + Hijri in English, from the free
    // bangladatetoday.com API (no key, Asia/Dhaka). Line 2 (prayers): the current + next salat from
    // the free Aladhan API. Both are cached per day in SharedPreferences, so the header renders
    // instantly and offline and each API is hit at most once a day; the current/next split is
    // computed on device so it advances as the day progresses.

    private static final String PREFS_PRAYER = "prayer_times";
    private static final String KEY_PRAYER_DATE = "date";        // yyyy-MM-dd this data is for
    private static final String KEY_PRAYER_TIMINGS = "timings";  // "HH:mm|HH:mm|HH:mm|HH:mm|HH:mm"
    private static final String KEY_CAL_DATE = "cal_date";       // yyyy-MM-dd the date strings are for
    private static final String KEY_CAL_BENGALI = "cal_bengali"; // e.g. "৪ ভাদ্র ১৪৩৩"
    private static final String KEY_CAL_HIJRI = "cal_hijri";     // e.g. "5 Rabi' al-Awwal 1448"
    private static final String[] PRAYER_NAMES = {"Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"};

    /** Renders both header lines from cache (instant/offline), then refreshes each in the background. */
    private void updatePrayerHeader() {
        if (!isAdded() || getContext() == null) return;

        boolean showDate = DisplayPrefs.isShowDate(getContext());
        boolean showSalat = DisplayPrefs.isShowSalat(getContext());

        tvHubEyebrow.setVisibility(showDate ? View.VISIBLE : View.GONE);
        tvHubTitle.setVisibility(showSalat ? View.VISIBLE : View.GONE);

        if (showDate) renderDateLine();
        if (showSalat) renderPrayerLine();

        refreshCalendar();
        refreshPrayerTimes();
    }

    private String todayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    /** Line 1: English date (always, from device) + Bengali (Bangla) and Hijri (English). */
    private void renderDateLine() {
        if (tvHubEyebrow == null) return;

        Date now = new Date();
        StringBuilder line = new StringBuilder(
                new SimpleDateFormat("EEE · d MMM yyyy", Locale.ENGLISH).format(now));

        String bengali = null, hijri = null;
        if (getContext() != null) {
            SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_PRAYER, Context.MODE_PRIVATE);
            if (todayKey().equals(prefs.getString(KEY_CAL_DATE, null))) {
                bengali = prefs.getString(KEY_CAL_BENGALI, null);
                hijri = prefs.getString(KEY_CAL_HIJRI, null);
            }
        }
        // Bengali is computed on-device when the API hasn't cached today, so line 1's Bangla date
        // works with zero network; the API value (authoritative) overwrites the cache on arrival.
        if (bengali == null || bengali.isEmpty()) bengali = computeBengaliDate(now);

        if (bengali != null && !bengali.isEmpty()) line.append("  ·  ").append(bengali);
        if (hijri != null && !hijri.isEmpty()) line.append("  ·  ").append(hijri);

        tvHubEyebrow.setText(line.toString());
    }

    /** Fetches the tri-calendar date on a background thread unless it's already cached for today. */
    private void refreshCalendar() {
        if (getContext() == null) return;
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_PRAYER, Context.MODE_PRIVATE);
        final String today = todayKey();
        if (today.equals(prefs.getString(KEY_CAL_DATE, null))
                && prefs.getString(KEY_CAL_BENGALI, null) != null) {
            return; // cache already serves today
        }

        new Thread(() -> {
            String[] cal = fetchCalendar(today); // { bengali (Bangla), hijri (English) } or null
            if (cal == null) return; // silent: line 1 keeps its English-only text

            prefs.edit()
                    .putString(KEY_CAL_DATE, today)
                    .putString(KEY_CAL_BENGALI, cal[0])
                    .putString(KEY_CAL_HIJRI, cal[1])
                    .apply();

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    if (isAdded()) renderDateLine();
                });
            }
        }).start();
    }

    /** GETs today's Bengali (Bangla script) + Hijri (English) date; returns {bengali, hijri} or null. */
    private String[] fetchCalendar(String date) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("https://www.bangladatetoday.com/api/convert?date=" + date);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) return null;

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
            }

            JSONObject root = new JSONObject(sb.toString());

            JSONObject bengali = root.getJSONObject("bengali");
            String bengaliDisplay = toBanglaDigits(bengali.getInt("day")) + " "
                    + bengali.getJSONObject("monthName").getString("bn") + " "
                    + toBanglaDigits(bengali.getInt("year"));

            JSONObject hijri = root.getJSONObject("hijri");
            String hijriDisplay = hijri.getInt("day") + " "
                    + hijri.getJSONObject("monthName").getString("en") + " "
                    + hijri.getInt("year");

            return new String[]{bengaliDisplay, hijriDisplay};
        } catch (Exception e) {
            Log.w(TAG, "Calendar fetch failed", e);
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /** Converts the ASCII digits in a number to Bangla numerals (০–৯). */
    private String toBanglaDigits(int n) {
        final char[] bn = {'০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯'};
        String s = String.valueOf(n);
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            out.append(c >= '0' && c <= '9' ? bn[c - '0'] : c);
        }
        return out.toString();
    }

    // Bangla month names (revised Bangladesh calendar; Poila Boishakh always falls on 14 April).
    private static final String[] BENGALI_MONTHS = {
            "বৈশাখ", "জ্যৈষ্ঠ", "আষাঢ়", "শ্রাবণ", "ভাদ্র", "আশ্বিন",
            "কার্তিক", "অগ্রহায়ণ", "পৌষ", "মাঘ", "ফাল্গুন", "চৈত্র"
    };

    /**
     * On-device Bengali (Bangladesh) date, e.g. "৪ ভাদ্র ১৪৩৩" — the offline fallback for line 1.
     * Follows the 2019 Bangla Academy rule: Poila Boishakh is fixed to 14 April; months 1–5 have
     * 31 days, 6–11 have 30, and Choitro (12) has 30 — or 31 when the next Gregorian year is a leap
     * year. Anchored to 14 Apr 2026 = 1 Boishakh 1433, verified against the live API.
     */
    private String computeBengaliDate(Date date) {
        Calendar d = Calendar.getInstance();
        d.setTime(date);
        int g = d.get(Calendar.YEAR);
        int gMonth = d.get(Calendar.MONTH);   // 0 = January
        int gDay = d.get(Calendar.DAY_OF_MONTH);

        // Before 14 April we're still in the previous Bengali year.
        int startYear = g;
        if (gMonth < Calendar.APRIL || (gMonth == Calendar.APRIL && gDay < 14)) {
            startYear = g - 1;
        }

        Calendar start = Calendar.getInstance();
        start.clear();
        start.set(startYear, Calendar.APRIL, 14, 12, 0, 0);

        Calendar today = Calendar.getInstance();
        today.clear();
        today.set(g, gMonth, gDay, 12, 0, 0);

        int dayOfYear = (int) Math.round(
                (today.getTimeInMillis() - start.getTimeInMillis()) / 86400000.0) + 1;
        if (dayOfYear < 1) return null;

        int nextG = startYear + 1;
        boolean leap = (nextG % 4 == 0 && nextG % 100 != 0) || nextG % 400 == 0;
        int[] lengths = {31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 30, leap ? 31 : 30};

        int m = 0;
        while (m < 12 && dayOfYear > lengths[m]) {
            dayOfYear -= lengths[m];
            m++;
        }
        if (m >= 12) return null; // safety — a well-formed year never overflows

        return toBanglaDigits(dayOfYear) + " " + BENGALI_MONTHS[m] + " " + toBanglaDigits(startYear - 593);
    }

    /** Line 2: if today's timings are cached, show the current + next salat; else leave the default. */
    private void renderPrayerLine() {
        if (getContext() == null || tvHubTitle == null) return;
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_PRAYER, Context.MODE_PRIVATE);
        if (!todayKey().equals(prefs.getString(KEY_PRAYER_DATE, null))) return;
        String timings = prefs.getString(KEY_PRAYER_TIMINGS, null);
        if (timings != null) applyPrayerLine(timings.split("\\|"));
    }

    /** Fetches today's timings on a background thread unless they're already cached for today. */
    private void refreshPrayerTimes() {
        if (getContext() == null) return;
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_PRAYER, Context.MODE_PRIVATE);
        final String today = todayKey();
        if (today.equals(prefs.getString(KEY_PRAYER_DATE, null))
                && prefs.getString(KEY_PRAYER_TIMINGS, null) != null) {
            return; // cache already serves today
        }

        new Thread(() -> {
            String timings = fetchPrayerTimings();
            if (timings == null) return; // silent: header keeps its current/default text

            prefs.edit()
                    .putString(KEY_PRAYER_DATE, today)
                    .putString(KEY_PRAYER_TIMINGS, timings)
                    .apply();

            final String[] parts = timings.split("\\|");
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    if (isAdded()) applyPrayerLine(parts);
                });
            }
        }).start();
    }

    /** GETs the 5 daily prayer timings for Chittagong; returns "HH:mm|..x5" or null on any failure. */
    private String fetchPrayerTimings() {
        HttpURLConnection conn = null;
        try {
            // method=1 → University of Islamic Sciences, Karachi (standard for the subcontinent);
            // school=1 → Hanafi Asr, the norm in Bangladesh.
            URL url = new URL("https://api.aladhan.com/v1/timingsByCity"
                    + "?city=Chittagong&country=Bangladesh&method=1&school=1");
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) return null;

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
            }

            JSONObject timings = new JSONObject(sb.toString())
                    .getJSONObject("data").getJSONObject("timings");

            StringBuilder out = new StringBuilder();
            for (int i = 0; i < PRAYER_NAMES.length; i++) {
                if (i > 0) out.append('|');
                String raw = timings.getString(PRAYER_NAMES[i]);
                int space = raw.indexOf(' '); // strip any suffix, e.g. "18:24 (+06)"
                out.append(space > 0 ? raw.substring(0, space) : raw);
            }
            return out.toString();
        } catch (Exception e) {
            Log.w(TAG, "Prayer times fetch failed", e);
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /** Shows "Now &lt;current&gt; · Next &lt;next&gt;" with the labels dimmed, computing both from now. */
    private void applyPrayerLine(String[] times) {
        if (times == null || times.length < PRAYER_NAMES.length || tvHubTitle == null || getContext() == null) return;

        Calendar now = Calendar.getInstance();
        int nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

        int nextIdx = -1;
        for (int i = 0; i < PRAYER_NAMES.length; i++) {
            int m = parseMinutes(times[i]);
            if (m > nowMin) { nextIdx = i; break; }
        }
        if (nextIdx == -1) nextIdx = 0; // past Isha → next is tomorrow's Fajr
        // Current = the salat in effect now: the one before "next", wrapping to Isha before Fajr / after Isha.
        int curIdx = (nextIdx == 0) ? PRAYER_NAMES.length - 1 : nextIdx - 1;

        String curTime = formatTime12(times[curIdx]);
        String nextTime = formatTime12(times[nextIdx]);
        if (curTime == null || nextTime == null) return;

        // Countdown to the next salat, shown dimmed in brackets after its time.
        int deltaMin = parseMinutes(times[nextIdx]) - nowMin;
        if (deltaMin < 0) deltaMin += 24 * 60; // wrapped to tomorrow's Fajr
        int h = deltaMin / 60, mm = deltaMin % 60;
        String countdown = h > 0 ? ("in " + h + "h " + mm + "m") : ("in " + mm + "m");

        int labelColor = ContextCompat.getColor(requireContext(), R.color.textSecondary);
        SpannableStringBuilder sb = new SpannableStringBuilder();
        sb.append(PRAYER_NAMES[curIdx]).append(" ").append(curTime);
        sb.append("   ·   ");
        sb.append(PRAYER_NAMES[nextIdx]).append(" ").append(nextTime);
        int bStart = sb.length();
        sb.append("  (").append(countdown).append(")");
        sb.setSpan(new ForegroundColorSpan(labelColor), bStart, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvHubTitle.setText(sb);
    }

    /** "HH:mm" (with optional trailing suffix) → minutes since midnight, or -1 if unparseable. */
    private int parseMinutes(String hhmm) {
        if (hhmm == null) return -1;
        int space = hhmm.indexOf(' ');
        if (space > 0) hhmm = hhmm.substring(0, space);
        String[] p = hhmm.split(":");
        if (p.length < 2) return -1;
        try {
            return Integer.parseInt(p[0].trim()) * 60 + Integer.parseInt(p[1].trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** "HH:mm" → "h:mm AM/PM", or null if unparseable. */
    private String formatTime12(String hhmm) {
        int m = parseMinutes(hhmm);
        if (m < 0) return null;
        int h = m / 60, min = m % 60;
        String ampm = h >= 12 ? "PM" : "AM";
        int h12 = h % 12;
        if (h12 == 0) h12 = 12;
        return String.format(Locale.getDefault(), "%d:%02d %s", h12, min, ampm);
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
        AnalyticsHelper.logCommunityAction("open");
        Intent intent = new Intent(requireContext(), CommunityActivity.class);
        requireActivity().startActivity(intent);
    }

    private void openWebPage(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshCommunityBadge();
        updatePrayerHeader();
        updateRoutineCard();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Don't tick the countdown while Home isn't in the foreground.
        stopRoutineTicker();
        // Don't run the icon pulse off-screen; onResume re-evaluates unread and restarts it if needed.
        stopCommunityPulse();
    }

    /**
     * Updates the community-card unread pill from on-device read tracking. Unread = messages newer
     * than the locally stored "last seen" timestamp; the pill is prefixed with '@' when any of them
     * mention the current user. First run on a device baselines "now" so history isn't counted.
     */
    private void refreshCommunityBadge() {
        if (communityUnreadBadge == null || db == null || getContext() == null) return;

        String studentId = UserData.getInstance().getStudentId();
        if (studentId == null) {
            clearCommunityUnread();
            return;
        }

        SharedPreferences prefs = requireContext()
                .getSharedPreferences(CommunityActivity.PREFS_COMMUNITY, Context.MODE_PRIVATE);
        String key = CommunityActivity.lastSeenKey(studentId);

        if (!prefs.contains(key)) {
            prefs.edit().putLong(key, System.currentTimeMillis()).apply();
            clearCommunityUnread();
            return;
        }

        Date lastSeen = new Date(prefs.getLong(key, System.currentTimeMillis()));

        // Unread count — single-field range, no composite index required.
        db.collection("community_messages")
                .whereGreaterThan("timestamp", lastSeen)
                .count()
                .get(AggregateSource.SERVER)
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded() || communityUnreadBadge == null) return;
                    long unread = snapshot.getCount();
                    if (unread <= 0) {
                        clearCommunityUnread();
                    } else {
                        checkMentionCue(lastSeen, unread);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || communityUnreadBadge == null) return;
                    Log.w(TAG, "Unread count failed", e);
                    clearCommunityUnread();
                });
    }

    /** Prefixes the pill with '@' when any unread message mentions the current user. */
    private void checkMentionCue(Date lastSeen, long unread) {
        String studentId = UserData.getInstance().getStudentId();
        db.collection("community_messages")
                .whereGreaterThan("timestamp", lastSeen)
                .whereArrayContains("mentions", studentId)
                .count()
                .get(AggregateSource.SERVER)
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded() || communityUnreadBadge == null) return;
                    showBadge(unread, snapshot.getCount() > 0);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || communityUnreadBadge == null) return;
                    // Composite index may still be building — fall back to the plain count.
                    Log.w(TAG, "Mention-cue count failed (composite index may be missing)", e);
                    showBadge(unread, false);
                });
    }

    private void showBadge(long unread, boolean mentioned) {
        String number = unread > 9 ? getString(R.string.unread_count_capped) : String.valueOf(unread);
        communityUnreadBadge.setText(mentioned ? getString(R.string.unread_mention_format, number) : number);
        communityUnreadBadge.setVisibility(View.VISIBLE);
        highlightCommunityCard(number, (int) unread);
        startCommunityPulse();
    }

    /**
     * Lights up the whole community card so the unread count actually draws the eye: accent stroke,
     * a brighter icon-chip halo, and a live "N new messages" subtitle in the accent colour.
     */
    private void highlightCommunityCard(String number, int count) {
        Context ctx = getContext();
        if (ctx == null) return;
        if (communityCard != null) {
            communityCard.setStrokeColor(ContextCompat.getColor(ctx, R.color.accent_community));
        }
        if (communityIcon != null) {
            communityIcon.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.accent_community_glow)));
        }
        if (communitySubtitle != null) {
            communitySubtitle.setText(
                    getResources().getQuantityString(R.plurals.community_new_messages, count, number));
            communitySubtitle.setTextColor(ContextCompat.getColor(ctx, R.color.accent_community));
        }
    }

    /**
     * Starts a continuous, gentle breathing pulse on the community icon chip while there are unread
     * messages. Scale is a render transform, so it never reflows the row. Idempotent — an already-running
     * pulse is left alone. Skipped when the system animator scale is 0 (reduced motion / animations off),
     * leaving just the static highlight.
     */
    private void startCommunityPulse() {
        if (communityIcon == null) return;
        if (communityPulse != null && communityPulse.isStarted()) return;

        Context ctx = getContext();
        if (ctx != null && android.provider.Settings.Global.getFloat(ctx.getContentResolver(),
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f) {
            return;
        }

        communityPulse = ObjectAnimator.ofPropertyValuesHolder(communityIcon,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.10f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.10f));
        communityPulse.setDuration(650);
        communityPulse.setRepeatCount(ObjectAnimator.INFINITE);
        communityPulse.setRepeatMode(ObjectAnimator.REVERSE);
        communityPulse.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        communityPulse.start();
    }

    /** Stops the breathing pulse and restores the icon chip to its resting size. */
    private void stopCommunityPulse() {
        if (communityPulse != null) {
            communityPulse.cancel();
            communityPulse = null;
        }
        if (communityIcon != null) {
            communityIcon.setScaleX(1f);
            communityIcon.setScaleY(1f);
        }
    }

    /** Reverts the community card to its resting look (and hides the pill) once the user is caught up. */
    private void clearCommunityUnread() {
        if (communityUnreadBadge != null) {
            communityUnreadBadge.setVisibility(View.GONE);
        }
        stopCommunityPulse();
        Context ctx = getContext();
        if (ctx == null) return;
        if (communityCard != null) {
            communityCard.setStrokeColor(ContextCompat.getColor(ctx, R.color.dividerColor));
        }
        if (communityIcon != null) {
            communityIcon.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.accent_community_soft)));
        }
        if (communitySubtitle != null) {
            communitySubtitle.setText(R.string.community_chat_subtitle);
            communitySubtitle.setTextColor(ContextCompat.getColor(ctx, R.color.textSecondary));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopRoutineTicker();
        routineRows.clear();
        todaysClasses = null;
        stopCommunityPulse();
        communityCard = null;
        communityIcon = null;
        communitySubtitle = null;
        communityUnreadBadge = null;
        // Detach adapters and null view references so the destroyed view hierarchy
        // can be garbage collected while any pending Firebase callbacks are guarded by isAdded().
        if (noticesRecyclerView != null) {
            noticesRecyclerView.setAdapter(null);
        }
        if (latestUpdatesRecyclerView != null) {
            latestUpdatesRecyclerView.setAdapter(null);
        }
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(null);
        }
        swipeRefreshLayout = null;
        noticesRecyclerView = null;
        latestUpdatesRecyclerView = null;
        homeBannerImage = null;
        homeBannerCard = null;
        tvLatestAnnouncementsTitle = null;
        tvHubTitle = null;
        controlLayout = null;
        adminBtn = null;
        uploadBtn = null;
        communityUnreadBadge = null;
        cardRoutine = null;
        routineContainer = null;
        tvRoutineEyebrow = null;
        tvRoutineStatus = null;
        noticeAdapter = null;
        latestUpdatesAdapter = null;
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