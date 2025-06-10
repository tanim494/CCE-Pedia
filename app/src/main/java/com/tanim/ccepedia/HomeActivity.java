package com.tanim.ccepedia;

import static android.widget.Toast.LENGTH_LONG;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.messaging.FirebaseMessaging;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;


public class HomeActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigation;
    LinearLayout sideMenu;
    TextView toolBarTxt;
    TextView studentName;
    TextView studentId;
    TextView studentSemester;
    Button adminBtn, uploadBtn;
    TextView themeSwitcher;
    String updateLink;
    float databaseVersion;
    float userVersion;
    String userRole;

    FirebaseFirestore firestore;
    DocumentReference configDocRef;
    ListenerRegistration configListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String selectedTheme = ThemePref.getTheme(this);
        if (selectedTheme.equals(ThemePref.THEME_BLUE)) {
            setTheme(R.style.Theme_CCEPedia_Blue);
        } else {
            setTheme(R.style.Theme_CCEPedia_Green);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        checkForUpdate();
        FirebaseMessaging.getInstance().subscribeToTopic("notification");

        initializeViews();
        setUserData();
        setupDatabaseListeners();
        setupClickListeners();
        loadFragment();
        Toast.makeText(this, "Welcome to CCE Pedia, " + UserData.getInstance().getName(), LENGTH_LONG).show();
    }

    private void checkForUpdate() {
        new Handler().postDelayed(() -> {
            if (databaseVersion > userVersion) {

                SpannableString title = new SpannableString("Update Available");
                title.setSpan(new StyleSpan(Typeface.BOLD), 0, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                new MaterialAlertDialogBuilder(this)
                        .setTitle(title)
                        .setMessage("A new version of CCEPedia is ready to download. Update now to get the latest features and improvements.")
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




    private void initializeViews() {
        sideMenu = findViewById(R.id.sideMenu);
        toolBarTxt = findViewById(R.id.toolText);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        themeSwitcher = findViewById(R.id.switchTheme);

        studentName = findViewById(R.id.studentName);
        studentId = findViewById(R.id.studentId);
        studentSemester = findViewById(R.id.studentSemester);
        uploadBtn = findViewById(R.id.uploadBtn);
        adminBtn = findViewById(R.id.adminBtn);
    }

    @SuppressLint("SetTextI18n")
    private void setUserData() {
        UserData user = UserData.getInstance();
        studentName.setText("Name: " + user.getName());
        studentId.setText("ID: " + user.getId());
        studentSemester.setText("Semester: " + user.getSemester());
        userRole = user.getRole();

        if (userRole.equalsIgnoreCase("admin")) {
            adminBtn.setVisibility(View.VISIBLE);
            adminBtn.setOnClickListener(v -> openAdminMode());
            uploadBtn.setVisibility(View.VISIBLE);
            uploadBtn.setOnClickListener(v -> openFileUpload());
        } else if (userRole.equalsIgnoreCase("moderator")) {
            uploadBtn.setVisibility(View.VISIBLE);
            uploadBtn.setOnClickListener(v -> openFileUpload());
        }
    }

    @SuppressLint("SetTextI18n")
    private void openAdminMode() {sideMenuClose();
        AdminFragment adminFragment = new AdminFragment();
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.Midcontainer, adminFragment)
                .addToBackStack(null)
                .commit();
        toolBarTxt.setText("Admin Control");
    }

    @SuppressLint("SetTextI18n")
    private void openFileUpload() {
        sideMenuClose();
        UploadFile uploadFragment = new UploadFile();
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.Midcontainer, uploadFragment)
                .addToBackStack(null)
                .commit();
        toolBarTxt.setText("Upload Resources");
    }

    private void setupDatabaseListeners() {
        firestore = FirebaseFirestore.getInstance();
        configDocRef = firestore.collection("appConfig").document("main");

        configListener = configDocRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                // Handle error if needed
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                Double version = snapshot.getDouble("version");
                updateLink = snapshot.getString("updateLink");

                if (version != null) {
                    databaseVersion = version.floatValue();
                }

                // Get current app version
                userVersion = Math.round(Float.parseFloat(BuildConfig.VERSION_NAME) * 100);
            }
        });
    }


    private void setupClickListeners() {
        findViewById(R.id.menuIcon).setOnClickListener(v -> {
            if (sideMenu.getVisibility() == View.VISIBLE) {
                sideMenuClose();
            } else {
                sideMenuOpen();
            }
        });

        findViewById(R.id.profileBtn).setOnClickListener(view -> openProfileFragment());
        setupMenuSemesterData();

        themeSwitcher.setOnClickListener(v -> {
            String current = ThemePref.getTheme(this);
            if (current.equals(ThemePref.THEME_BLUE)) {
                // Switch to GREEN
                bottomNavigation.setSelectedItemId(R.id.nv_home);
                ThemePref.setTheme(this, ThemePref.THEME_GREEN);
                Toast.makeText(this, "Switched to Green Theme", LENGTH_LONG).show();
            } else {
                // Switch to BLUE
                bottomNavigation.setSelectedItemId(R.id.nv_home);
                ThemePref.setTheme(this, ThemePref.THEME_BLUE);
                Toast.makeText(this, "Switched to Blue Theme", LENGTH_LONG).show();
            }
            recreate(); // recreate to apply theme
        });

    }

    @SuppressLint("SetTextI18n")
    private void openProfileFragment() {
        sideMenuClose();
        toolBarTxt.setText("User Profile");

        ProfileFragment profileFragment = new ProfileFragment();
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.Midcontainer, profileFragment)
                .addToBackStack(null)
                .commit();
    }

    @SuppressLint("SetTextI18n")
    private void setupMenuSemesterData() {
        Button menuSemRes = findViewById(R.id.menuSemRes);
        UserData user = UserData.getInstance();
        String semester = user.getSemester();

        switch (semester) {
            case "1":
                menuSemRes.setText("1st Semester Resources");
                break;
            case "2":
                menuSemRes.setText("2nd Semester Resources");
                break;
            case "3":
                menuSemRes.setText("3rd Semester Resources");
                break;
            case "4":
                menuSemRes.setText("4th Semester Resources");
                break;
            case "5":
                menuSemRes.setText("5th Semester Resources");
                break;
            case "6":
                menuSemRes.setText("6th Semester Resources");
                break;
            case "7":
                menuSemRes.setText("7th Semester Resources");
                break;
            case "8":
                menuSemRes.setText("8th Semester Resources");
                break;
        }

        menuSemRes.setOnClickListener(view -> view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() -> {
            view.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
            openCourseListFragment("semester_" + semester);  // <<< This opens the right fragment
        }).start());
    }

    @SuppressLint("SetTextI18n")
    private void openCourseListFragment(String semesterId) {
        sideMenuClose();
        toolBarTxt.setText("Resources");
        bottomNavigation.setSelectedItemId(R.id.nv_resource);

        CourseListFragment fragment = CourseListFragment.newInstance(semesterId);
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.Midcontainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    @SuppressLint({"SetTextI18n", "NonConstantResourceId"})
    private void loadFragment() {
        FragmentManager fgMan = getSupportFragmentManager();
        FragmentTransaction tran = fgMan.beginTransaction();
        tran.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        tran.replace(R.id.Midcontainer, new Home());
        tran.commit();

        bottomNavigation.setOnItemSelectedListener(item -> {
            if (sideMenu.getVisibility() == View.VISIBLE) {
                sideMenuClose();
            }

            FragmentTransaction tran1 = getSupportFragmentManager().beginTransaction();
            tran1.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);

            switch (item.getItemId()) {
                case R.id.nv_home:
                    tran1.replace(R.id.Midcontainer, new Home());
                    toolBarTxt.setText("CCE Pedia");
                    break;
                case R.id.nv_faculty:
                    tran1.replace(R.id.Midcontainer, new Faculty());
                    toolBarTxt.setText("Faculties");
                    break;
                case R.id.nv_resource:
                    tran1.replace(R.id.Midcontainer, new Resources());
                    toolBarTxt.setText("Resources");
                    break;
                case R.id.nv_author:
                    tran1.replace(R.id.Midcontainer, new Author());
                    toolBarTxt.setText("Author");
                    break;
            }

            tran1.commit();
            return true;
        });
    }

    private void sideMenuOpen() {
        setupMenuSemesterData();
        sideMenu.setVisibility(View.VISIBLE);
        sideMenu.setTranslationX(-sideMenu.getWidth());
        sideMenu.animate().translationX(0).setDuration(300).start();

        ImageView menuIcon = findViewById(R.id.menuIcon);
        menuIcon.animate().rotation(180).setDuration(300).withEndAction(() ->
                menuIcon.setImageResource(R.drawable.nv_menu)
        ).start();
    }

    private void sideMenuClose() {
        sideMenu.animate().translationX(-sideMenu.getWidth()).setDuration(300).withEndAction(() ->
                sideMenu.setVisibility(View.INVISIBLE)
        ).start();

        ImageView menuIcon = findViewById(R.id.menuIcon);
        menuIcon.animate().rotation(0).setDuration(300).withEndAction(() ->
                menuIcon.setImageResource(R.drawable.nv_menu)
        ).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (configListener != null) {
            configListener.remove();
        }
    }

    public static class ThemePref {
        private static final String PREF_NAME = "theme_pref";
        private static final String KEY_THEME = "selected_theme";

        public static final String THEME_GREEN = "green";
        public static final String THEME_BLUE = "blue";

        public static void setTheme(Context context, String theme) {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_THEME, theme).apply();
        }

        public static String getTheme(Context context) {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            return prefs.getString(KEY_THEME, THEME_BLUE); // default = blue
        }
    }

}
