package com.tanim.ccepedia;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.content.ActivityNotFoundException;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Environment;
import android.provider.Settings;
import androidx.core.content.FileProvider;
import java.io.File;
import android.text.format.Formatter;
import android.view.View;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.widget.TextView;
import android.widget.Toast;
import android.view.MenuItem;
import android.Manifest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import androidx.activity.OnBackPressedCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.tanim.ccepedia.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.messaging.FirebaseMessaging;

import androidx.appcompat.widget.SearchView;


import android.view.Menu;
import android.view.MenuItem;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import com.bumptech.glide.Glide;

import com.google.firebase.auth.FirebaseAuth;

import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    // Extras for opening a resource shared from the Community Chat directly in the middle container.
    public static final String EXTRA_OPEN_TYPE = "open_resource_type";
    public static final String EXTRA_OPEN_URL = "open_resource_url";
    public static final String EXTRA_OPEN_TITLE = "open_resource_title";

    // Tags the shared viewer so a single back press from it returns to the Community Chat.
    private static final String TAG_SHARED_FROM_CHAT = "shared_from_chat";

    String updateLink;
    String apkLink;
    int latestVersionCode;
    boolean forceUpdate;
    private boolean updatePrompted = false;
    private long apkDownloadId = -1L;
    private BroadcastReceiver apkDownloadReceiver;
    private static final String APK_FILE_NAME = "iiucpedia-update.apk";
    private static final String UPDATE_PREFS = "UpdatePrefs";
    private static final String KEY_INSTALL_CONSENT = "apk_install_consent";
    private androidx.appcompat.app.AlertDialog updateProgressDialog;
    private LinearProgressIndicator updateProgressBar;
    private TextView updateProgressPercent, updateProgressSize;
    private Handler progressHandler;
    private Runnable progressPoller;
    String userRole;

    FirebaseFirestore firestore;
    DocumentReference configDocRef;
    ListenerRegistration configListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        checkForNotificationPermission();
        FirebaseMessaging.getInstance().subscribeToTopic("notification");

        cleanupStaleApk(); // downloaded update APK is temporary — clear any leftover from a prior run
        fetchUserDataFromFirestore();
        setupDatabaseListeners();
        setupClickListeners();
        loadFragment();

        // A resource tapped in the Community Chat is handed to us to show in the middle container.
        if (savedInstanceState == null) {
            maybeOpenSharedResource(getIntent());
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.Midcontainer);

                if (currentFragment instanceof WebFragment) {
                    WebFragment webFragment = (WebFragment) currentFragment;
                    if (webFragment.canGoBack()) {
                        webFragment.goBack();
                        return;
                    }
                }

                // A resource opened from the Community Chat returns there on back (web history first).
                if (currentFragment != null && TAG_SHARED_FROM_CHAT.equals(currentFragment.getTag())) {
                    if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                        getSupportFragmentManager().popBackStack();
                    }
                    startActivity(new Intent(MainActivity.this, CommunityActivity.class));
                    return;
                }

                if (currentFragment instanceof HomeFragment || (getSupportFragmentManager().getBackStackEntryCount() == 0 && currentFragment == null)) {

                    new MaterialAlertDialogBuilder(MainActivity.this)
                            .setTitle(R.string.exit_title)
                            .setMessage(R.string.exit_message)
                            .setPositiveButton(R.string.yes, (dialog, which) -> finish())
                            .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
                            .show();

                }
                else if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                }
                else {
                    binding.bottomNavigation.setSelectedItemId(R.id.nv_home);

                    FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
                    tran.setCustomAnimations(android.R.anim.fade_in,
                            android.R.anim.fade_out,
                            android.R.anim.fade_in,
                            android.R.anim.fade_out);
                    tran.replace(R.id.Midcontainer, new HomeFragment());
                    tran.commit();
                }
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        maybeOpenSharedResource(intent);
    }

    /**
     * Opens a resource shared from the Community Chat inside the middle fragment container, using
     * the same viewers as normal resource browsing. Runs on a fresh launch and on re-delivery
     * (CommunityActivity relaunches us with CLEAR_TOP | SINGLE_TOP). Back returns to what was shown.
     */
    private void maybeOpenSharedResource(Intent intent) {
        if (intent == null) return;
        String url = intent.getStringExtra(EXTRA_OPEN_URL);
        if (url == null || url.isEmpty()) return;

        String type = intent.getStringExtra(EXTRA_OPEN_TYPE);
        String title = intent.getStringExtra(EXTRA_OPEN_TITLE);

        Fragment fragment = CommunityShare.TYPE_LINK.equals(type)
                ? WebFragment.newInstance(url)
                : PdfViewerFragment.newInstance(url, title, null);

        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                        android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.Midcontainer, fragment, TAG_SHARED_FROM_CHAT)
                .addToBackStack(null)
                .commit();

        // Consume the extras so a later getIntent() (e.g. after a config change) won't reopen it.
        intent.removeExtra(EXTRA_OPEN_URL);
        intent.removeExtra(EXTRA_OPEN_TYPE);
        intent.removeExtra(EXTRA_OPEN_TITLE);
    }

    private void checkForUpdate() {
        // Driven by the appConfig listener: show at most once per session, and only when the
        // server advertises a build newer than this installed APK (integer versionCode compare).
        if (updatePrompted) return;
        if (latestVersionCode <= com.tanim.ccepedia.BuildConfig.VERSION_CODE) return;

        updatePrompted = true;
        showUpdateDialog();
    }

    // Extracted from checkForUpdate() so a forced update can be re-shown (after the user backs out
    // of consent, cancels the download, or a download fails) without tripping the once-per-session
    // guard above.
    private void showUpdateDialog() {
        SpannableString title = new SpannableString(getString(R.string.update_available));
        title.setSpan(new StyleSpan(Typeface.BOLD), 0, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(forceUpdate ? R.string.update_message_forced : R.string.update_message)
                .setCancelable(false)
                .setPositiveButton(R.string.update, (d, w) -> { /* overridden after show() */ });

        // Optional updates can be postponed; a forced update offers no way out but updating.
        if (!forceUpdate) {
            builder.setNegativeButton(R.string.later, (dialog, which) -> dialog.dismiss());
        }

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();

        // Control dismissal ourselves. The in-app download path dismisses this dialog and hands off
        // to the consent/progress dialogs (which re-show it if a forced update is cancelled). The
        // browser fallback keeps a forced dialog up, so leaving for the browser can't strand the
        // user on an unsupported build.
        dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            boolean willDownload = apkLink != null && !apkLink.trim().isEmpty();
            if (willDownload || !forceUpdate) {
                dialog.dismiss();
            }
            startUpdate();
        });
    }

    private void openUpdateLink() {
        String url = updateLink;
        if (url == null || url.trim().isEmpty()) {
            Toast.makeText(this, R.string.update_link_missing, Toast.LENGTH_SHORT).show();
            return;
        }
        url = url.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.update_link_failed, Toast.LENGTH_SHORT).show();
        }
    }

    // Update button entry point: download + install the APK when a direct apkLink is set,
    // otherwise fall back to opening the website link in a browser. The first APK download is
    // gated behind a one-time consent dialog (installing outside the Play Store).
    private void startUpdate() {
        if (apkLink == null || apkLink.trim().isEmpty()) {
            openUpdateLink();
        } else if (hasInstallConsent()) {
            downloadAndInstallApk();
        } else {
            showInstallConsentDialog();
        }
    }

    // Shown once, the first time the user opts into an in-app APK update, to explain that the app
    // downloads and installs the update itself (not via the Play Store). After acceptance it is
    // remembered, so later updates go straight to downloading.
    private void showInstallConsentDialog() {
        SpannableString title = new SpannableString(getString(R.string.update_consent_title));
        title.setSpan(new StyleSpan(Typeface.BOLD), 0, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(R.string.update_consent_message)
                .setCancelable(false)
                .setPositiveButton(R.string.update_consent_accept, (d, w) -> {
                    setInstallConsent();
                    downloadAndInstallApk();
                })
                .setNegativeButton(R.string.cancel, (d, w) -> {
                    d.dismiss();
                    if (forceUpdate) showUpdateDialog(); // can't slip past a mandatory update
                })
                .show();
    }

    private boolean hasInstallConsent() {
        return getSharedPreferences(UPDATE_PREFS, MODE_PRIVATE).getBoolean(KEY_INSTALL_CONSENT, false);
    }

    private void setInstallConsent() {
        getSharedPreferences(UPDATE_PREFS, MODE_PRIVATE)
                .edit().putBoolean(KEY_INSTALL_CONSENT, true).apply();
    }

    private void downloadAndInstallApk() {
        String link = apkLink.trim();
        if (!link.startsWith("http://") && !link.startsWith("https://")) {
            link = "https://" + link;
        }

        // On Android 8+, launching the installer needs the user's "install unknown apps" consent for us.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !getPackageManager().canRequestPackageInstalls()) {
            promptEnableUnknownSources();
            return;
        }

        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (dm == null || dir == null) {
            openUpdateLink(); // download service/storage unavailable — degrade to the browser link
            return;
        }

        // Start clean so a stale or half-finished APK can't be installed by mistake.
        cleanupStaleApk();

        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(link));
            request.setTitle(getString(R.string.app_name));
            request.setDescription(getString(R.string.update_downloading));
            request.setMimeType("application/vnd.android.package-archive");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, APK_FILE_NAME);

            registerApkDownloadReceiver();
            apkDownloadId = dm.enqueue(request);
            showDownloadProgressDialog();
        } catch (Exception e) {
            Toast.makeText(this, R.string.update_link_failed, Toast.LENGTH_SHORT).show();
        }
    }

    // In-app progress while the APK downloads. The system also posts a download notification, so
    // leaving the app mid-download is fine — the completion receiver still installs on return.
    private void showDownloadProgressDialog() {
        View content = getLayoutInflater().inflate(R.layout.dialog_update_progress, null);
        updateProgressBar = content.findViewById(R.id.updateProgressBar);
        updateProgressPercent = content.findViewById(R.id.updateProgressPercent);
        updateProgressSize = content.findViewById(R.id.updateProgressSize);

        SpannableString title = new SpannableString(getString(R.string.update_downloading_title));
        title.setSpan(new StyleSpan(Typeface.BOLD), 0, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        updateProgressDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setView(content)
                .setCancelable(false)
                .setNegativeButton(R.string.cancel, (d, w) -> cancelDownload())
                .create();
        updateProgressDialog.show();

        startProgressPolling();
    }

    private void startProgressPolling() {
        if (progressHandler == null) progressHandler = new Handler(getMainLooper());
        progressPoller = new Runnable() {
            @Override
            public void run() {
                if (pollDownloadProgress()) {
                    progressHandler.postDelayed(this, 400);
                }
            }
        };
        progressHandler.post(progressPoller);
    }

    // Reads the download's current bytes/status and updates the dialog. Returns true to keep
    // polling, false once a terminal state (success/failure) has been handled.
    private boolean pollDownloadProgress() {
        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm == null) return false;

        Cursor c = dm.query(new DownloadManager.Query().setFilterById(apkDownloadId));
        if (c == null) return true;
        try {
            if (!c.moveToFirst()) return true;

            int status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            long downloaded = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            long total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

            if (status == DownloadManager.STATUS_FAILED) {
                stopProgressPolling();
                dismissProgressDialog();
                Toast.makeText(this, R.string.update_download_failed, Toast.LENGTH_LONG).show();
                if (forceUpdate) showUpdateDialog();
                return false;
            }

            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                // Snap to 100%; the completion receiver drives installDownloadedApk().
                if (updateProgressBar != null) updateProgressBar.setProgressCompat(100, true);
                if (updateProgressPercent != null) {
                    updateProgressPercent.setText(getString(R.string.update_progress_percent, 100));
                }
                return false;
            }

            // In flight (pending / running / paused).
            if (updateProgressBar != null && updateProgressPercent != null && updateProgressSize != null) {
                if (total > 0) {
                    int pct = (int) (downloaded * 100 / total);
                    updateProgressBar.setIndeterminate(false);
                    updateProgressBar.setProgressCompat(pct, true);
                    updateProgressPercent.setText(getString(R.string.update_progress_percent, pct));
                    updateProgressSize.setText(getString(R.string.update_progress_size,
                            Formatter.formatShortFileSize(this, downloaded),
                            Formatter.formatShortFileSize(this, total)));
                } else {
                    // Server didn't advertise a size yet — show indeterminate until it does.
                    updateProgressBar.setIndeterminate(true);
                    updateProgressPercent.setText(R.string.update_downloading);
                    updateProgressSize.setText("");
                }
            }
            return true;
        } finally {
            c.close();
        }
    }

    private void stopProgressPolling() {
        if (progressHandler != null && progressPoller != null) {
            progressHandler.removeCallbacks(progressPoller);
        }
        progressPoller = null;
    }

    private void dismissProgressDialog() {
        if (updateProgressDialog != null) {
            if (updateProgressDialog.isShowing() && !isFinishing()) {
                updateProgressDialog.dismiss();
            }
            updateProgressDialog = null;
        }
        updateProgressBar = null;
        updateProgressPercent = null;
        updateProgressSize = null;
    }

    private void cancelDownload() {
        stopProgressPolling();
        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm != null && apkDownloadId != -1L) {
            dm.remove(apkDownloadId);
        }
        apkDownloadId = -1L;
        unregisterApkReceiver();
        dismissProgressDialog();
        if (forceUpdate) showUpdateDialog(); // a mandatory update can't be cancelled away
    }

    private void registerApkDownloadReceiver() {
        if (apkDownloadReceiver != null) return;
        apkDownloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
                if (id != apkDownloadId) return;
                unregisterApkReceiver();
                installDownloadedApk();
            }
        };
        // ACTION_DOWNLOAD_COMPLETE is a system broadcast, so it must be flagged exported on API 34+.
        ContextCompat.registerReceiver(this, apkDownloadReceiver,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                ContextCompat.RECEIVER_EXPORTED);
    }

    private void installDownloadedApk() {
        stopProgressPolling();
        dismissProgressDialog();

        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (dm == null || dir == null) return;

        boolean success = false;
        Cursor c = dm.query(new DownloadManager.Query().setFilterById(apkDownloadId));
        if (c != null) {
            if (c.moveToFirst()) {
                int statusIdx = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                success = statusIdx >= 0 && c.getInt(statusIdx) == DownloadManager.STATUS_SUCCESSFUL;
            }
            c.close();
        }

        File apk = new File(dir, APK_FILE_NAME);
        if (!success || !apk.exists()) {
            Toast.makeText(this, R.string.update_download_failed, Toast.LENGTH_LONG).show();
            if (forceUpdate) showUpdateDialog();
            return;
        }

        try {
            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", apk);
            Intent install = new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(contentUri, "application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(install);
        } catch (Exception e) {
            Toast.makeText(this, R.string.update_download_failed, Toast.LENGTH_LONG).show();
            if (forceUpdate) showUpdateDialog();
        }
    }

    private void promptEnableUnknownSources() {
        Toast.makeText(this, R.string.update_allow_installs, Toast.LENGTH_LONG).show();
        try {
            startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + getPackageName())));
        } catch (ActivityNotFoundException e) {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES));
            } catch (ActivityNotFoundException ignored) {
                Toast.makeText(this, R.string.update_link_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void unregisterApkReceiver() {
        if (apkDownloadReceiver != null) {
            try {
                unregisterReceiver(apkDownloadReceiver);
            } catch (IllegalArgumentException ignored) {
            }
            apkDownloadReceiver = null;
        }
    }

    // The update APK is disposable: it only needs to survive from download until the installer
    // reads it. Wipe any leftover copy (e.g. after an update installed and the app relaunched, or
    // after an abandoned download) so a ~60 MB file doesn't linger in app storage.
    private void cleanupStaleApk() {
        File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (dir == null) return;
        File apk = new File(dir, APK_FILE_NAME);
        if (apk.exists()) apk.delete();
    }

    private void checkForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        101
                );
            }
        }
    }


    @SuppressLint("SetTextI18n")
    private void setUserData() {
        UserData user = UserData.getInstance();

        binding.userNameTextView.setText(user.getName());
        binding.userId.setText(user.getStudentId() + ", " + user.getDepartmentName() + " (" + user.getSemester() + " Semester)");
        userRole = user.getRole();

        loadProfileImage();
    }

    public void loadProfileImage() {
        UserData user = UserData.getInstance();
        String photoUrl = user.getPhotoUrl();
        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(this).load(photoUrl).circleCrop().into(binding.profileImage);
            binding.profileImage.setColorFilter(null);
            binding.profileImage.setImageTintList(null); // Clear app:tint for real image
        } else {
            binding.profileImage.setImageResource(R.drawable.ic_profile);
            binding.profileImage.setColorFilter(ContextCompat.getColor(this, R.color.textSecondary));
        }
    }

    private void fetchUserDataFromFirestore() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance().collection("users").document(uid)
                .get()
                .addOnSuccessListener((DocumentSnapshot snapshot) -> {
                    if (snapshot.exists()) {
                        UserData user = UserData.getInstance();
                        user.setStudentId(snapshot.getString("id"));
                        user.setName(snapshot.getString("name"));
                        user.setEmail(snapshot.getString("email"));
                        user.setGender(snapshot.getString("gender"));
                        user.setPhone(snapshot.getString("phone"));
                        user.setSemester(snapshot.getString("semester"));

                        String role = snapshot.getString("role");
                        if (role == null || role.isBlank()) {
                            user.setRole("");
                        } else {
                            user.setRole(role);
                        }

                        Long viewCount = snapshot.getLong("viewCount");
                        user.setViewCount(viewCount != null ? viewCount : 0);

                        String departmentName = snapshot.getString("department");
                        if (departmentName == null || departmentName.isEmpty()) {
                            departmentName = "CCE";
                        }
                        user.setDepartmentName(departmentName);

                        String photoUrl = snapshot.getString("photoUrl");
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            user.setPhotoUrl(photoUrl);
                        }

                        String profileLink = snapshot.getString("profileLink");
                        if (profileLink != null && !profileLink.isEmpty()) {
                            user.setProfileLink(profileLink);
                        }

                        setUserData();
                    }
                })
                .addOnFailureListener(e -> {
                    // Fallback to cached UserData
                    setUserData();
                });
    }

    private void setupDatabaseListeners() {
        firestore = FirebaseFirestore.getInstance();
        configDocRef = firestore.collection("appConfig").document("main");

        configListener = configDocRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                Long versionCode = snapshot.getLong("versionCode");
                updateLink = snapshot.getString("updateLink");
                apkLink = snapshot.getString("apkLink");
                Boolean force = snapshot.getBoolean("forceUpdate");

                latestVersionCode = (versionCode != null) ? versionCode.intValue() : 0;
                forceUpdate = (force != null) && force;

                // Check here (not on a fixed timer) so the prompt waits for the real config to arrive.
                checkForUpdate();
            }
        });
    }

    private void setupClickListeners() {
        binding.customHeader.setOnClickListener(view -> openProfileFragment());
        binding.ivAppLogo.setOnClickListener(view -> toggleTheme());
        // Long-press the logo to follow the device (system) theme
        binding.ivAppLogo.setOnLongClickListener(view -> {
            followSystemTheme();
            return true;
        });
    }

    private void toggleTheme() {
        SharedPreferences sharedPreferences = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("DarkMode", false);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            editor.putBoolean("DarkMode", false);
            editor.putBoolean("FollowSystem", false);
            AnalyticsHelper.logUserAction("theme_changed", "settings", "light");
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            editor.putBoolean("DarkMode", true);
            editor.putBoolean("FollowSystem", false);
            AnalyticsHelper.logUserAction("theme_changed", "settings", "dark");
        }
        editor.apply();
    }

    private void followSystemTheme() {
        SharedPreferences sharedPreferences = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        sharedPreferences.edit()
                .putBoolean("FollowSystem", true)
                .apply();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AnalyticsHelper.logUserAction("theme_changed", "settings", "system");
        Toast.makeText(this, R.string.theme_follow_system, Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("SetTextI18n")
    private void openProfileFragment() {

        ProfileFragment profileFragment = new ProfileFragment();
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out)
                .replace(R.id.Midcontainer, profileFragment)
                .addToBackStack(null)
                .commit();
    }

    @SuppressLint({"SetTextI18n", "NonConstantResourceId"})
    private void loadFragment() {
        FragmentManager fgMan = getSupportFragmentManager();
        FragmentTransaction tran = fgMan.beginTransaction();
        tran.setCustomAnimations(android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out);
        tran.replace(R.id.Midcontainer, new HomeFragment());
        tran.commit();


        binding.bottomNavigation.setOnItemSelectedListener(item -> {

            FragmentTransaction tran1 = getSupportFragmentManager().beginTransaction();
            tran1.setCustomAnimations(android.R.anim.fade_in,
                    android.R.anim.fade_out,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out);

            int itemId = item.getItemId();
            if (itemId == R.id.nv_home) {
                tran1.replace(R.id.Midcontainer, new HomeFragment());
            } else if (itemId == R.id.nv_faculty) {
                tran1.replace(R.id.Midcontainer, new FacultyFragment());
            } else if (itemId == R.id.nv_resource) {
                tran1.replace(R.id.Midcontainer, new ResourcesFragment());
            } else if (itemId == R.id.nv_author) {
                tran1.replace(R.id.Midcontainer, new DeveloperFragment());
            }

            tran1.commit();
            return true;
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (configListener != null) {
            configListener.remove();
        }
        stopProgressPolling();
        dismissProgressDialog();
        unregisterApkReceiver();
    }
}
