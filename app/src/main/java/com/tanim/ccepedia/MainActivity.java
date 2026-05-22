package com.tanim.ccepedia;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.widget.TextView;
import android.view.MenuItem;
import android.Manifest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
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

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;


public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    String updateLink;
    float databaseVersion;
    float userVersion;
    String userRole;

    FirebaseFirestore firestore;
    DocumentReference configDocRef;
    ListenerRegistration configListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        checkForUpdate();
        checkForNotificationPermission();
        FirebaseMessaging.getInstance().subscribeToTopic("notification");

        setUserData();
        setupDatabaseListeners();
        setupClickListeners();
        loadFragment();

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

                if (currentFragment instanceof HomeFragment || (getSupportFragmentManager().getBackStackEntryCount() == 0 && currentFragment == null)) {

                    new MaterialAlertDialogBuilder(MainActivity.this)
                            .setTitle("Exit IIUC Pedia?")
                            .setMessage("Are you sure you want to close the application?")
                            .setPositiveButton("Yes", (dialog, which) -> finish())
                            .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
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

    private void checkForUpdate() {
        new Handler().postDelayed(() -> {
            if (databaseVersion > userVersion) {

                SpannableString title = new SpannableString("Update Available");
                title.setSpan(new StyleSpan(Typeface.BOLD), 0, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                new MaterialAlertDialogBuilder(this)
                        .setTitle(title)
                        .setMessage("A new version of IIUC Pedia is ready to download. Update now to get the latest features and improvements.")
                        .setCancelable(false)
                        .setPositiveButton("Update", (dialog, which) -> {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(updateLink));
                            startActivity(intent);
                        })
                        .setNegativeButton("Later", (dialog, which) -> dialog.dismiss())
                        .show();
            }
        }, 2000);
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
    }

    private void setupDatabaseListeners() {
        firestore = FirebaseFirestore.getInstance();
        configDocRef = firestore.collection("appConfig").document("main");

        configListener = configDocRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                Double version = snapshot.getDouble("version");
                updateLink = snapshot.getString("updateLink");

                if (version != null) {
                    databaseVersion = version.floatValue();
                }

                userVersion = Math.round(Float.parseFloat(com.tanim.ccepedia.BuildConfig.VERSION_NAME) * 100);
            }
        });
    }

    private void setupClickListeners() {
        binding.customHeader.setOnClickListener(view -> openProfileFragment());
        binding.ivAppLogo.setOnClickListener(view -> toggleTheme());
    }

    private void toggleTheme() {
        SharedPreferences sharedPreferences = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("DarkMode", false);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            editor.putBoolean("DarkMode", false);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            editor.putBoolean("DarkMode", true);
        }
        editor.apply();
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
    }
}