package com.tanim.ccepedia;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class HomeActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigation;
    LinearLayout sideMenu;
    TextView toolBarTxt;
    TextView studentName;
    TextView studentSemester;
    TextView noticeText;
    TextView adminMode, moderatorMode;
    String noticeLink;
    String updateLink;
    float databaseVersion;
    float userVersion;
    String userRole;
    String menuResLink;

    DatabaseReference noticeTextDb;
    DatabaseReference noticeLinkDb;
    DatabaseReference updateVersionDb;
    DatabaseReference updateLinkDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        checkForUpdate();
        FirebaseMessaging.getInstance().subscribeToTopic("notification");

        setupFirebase();
        initializeViews();
        setUserData();
        setupDatabaseListeners();
        setupClickListeners();
        loadFragment();
    }

    private void checkForUpdate() {
        new Handler().postDelayed(() -> {
            if (databaseVersion > userVersion) {
                Dialog dialog = new Dialog(HomeActivity.this, R.style.DialogSlideAnim);
                dialog.setContentView(R.layout.update_dialog);
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                dialog.setCancelable(false);
                dialog.show();

                dialog.findViewById(R.id.okay_text).setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(updateLink));
                    startActivity(intent);
                });

                dialog.findViewById(R.id.cancel_text).setOnClickListener(v -> dialog.dismiss());
            }
        }, 2000);
    }

    private void initializeViews() {
        sideMenu = findViewById(R.id.sideMenu);
        toolBarTxt = findViewById(R.id.toolText);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        noticeText = findViewById(R.id.noticeButton);
        studentName = findViewById(R.id.studentName);
        studentSemester = findViewById(R.id.studentSemester);
        adminMode = findViewById(R.id.adminMode);
        moderatorMode = findViewById(R.id.moderatorMode);
    }

    private void setupFirebase() {
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://cce-pedia-5284c-default-rtdb.asia-southeast1.firebasedatabase.app/");
        noticeTextDb = database.getReference("message");
        noticeLinkDb = database.getReference("link");
        updateVersionDb = database.getReference("version");
        updateLinkDb = database.getReference("update-link");
    }

    private void setUserData() {
        UserData user = UserData.getInstance();
        studentName.setText("Name: " + user.getName());
        studentSemester.setText("Semester: " + user.getSemester());
        userRole = user.getRole();

        if (userRole.equalsIgnoreCase("admin")) {
            adminMode.setVisibility(View.VISIBLE);
            adminMode.setOnClickListener(v -> openFileUpload());
        } else if (userRole.equalsIgnoreCase("moderator")) {
            moderatorMode.setVisibility(View.VISIBLE);
            moderatorMode.setOnClickListener(v -> openFileUpload());
        } else {
        }
    }

    private void openFileUpload() {
        sideMenuClose();
        UploadFile uploadFragment = new UploadFile();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.Midcontainer, uploadFragment)
                .addToBackStack(null)
                .commit();

        toolBarTxt.setText("Upload Resources");
    }


    private void redirectToLogin() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupDatabaseListeners() {
        noticeTextDb.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String message = snapshot.getValue(String.class);
                noticeText.setAlpha(0f); // Start transparent
                noticeText.setText(message);
                noticeText.animate().alpha(1f).setDuration(300).start(); // Fade in
                noticeText.setTextSize(15);
                noticeText.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        noticeLinkDb.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                noticeLink = snapshot.getValue(String.class);
                noticeText.setOnClickListener(view -> {
                    if (noticeLink != null) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(noticeLink));
                        startActivity(intent);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        updateVersionDb.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userVersion = Float.parseFloat(BuildConfig.VERSION_NAME) * 100;
                databaseVersion = snapshot.getValue(Float.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        updateLinkDb.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                updateLink = snapshot.getValue(String.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
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

        findViewById(R.id.logOut).setOnClickListener(view -> redirectToLogin());
        setupMenuSemesterData();
    }

    private void setupMenuSemesterData() {
        TextView menuSemRes = findViewById(R.id.menuSemRes);
        UserData user = UserData.getInstance();

        switch (user.getSemester().toString()) {
            case "1":
                menuSemRes.setText("1st Semester Resources");
                menuResLink = "https://jpst.it/3q4Ai";
                break;
            case "2":
                menuSemRes.setText("2nd Semester Resources");
                menuResLink = "https://jpst.it/3q4Em";
                break;
            case "3":
                menuSemRes.setText("3rd Semester Resources");
                menuResLink = "https://jpst.it/3q4I3";
                break;
            case "4":
                menuSemRes.setText("4th Semester Resources");
                menuResLink = "https://jpst.it/3q4Jm";
                break;
            case "5":
                menuSemRes.setText("5th Semester Resources");
                menuResLink = "https://jpst.it/3q6UJ";
                break;
            case "6":
                menuSemRes.setText("6th Semester Resources");
                menuResLink = "https://jpst.it/3q6Wm";
                break;
            case "7":
                menuSemRes.setText("7th Semester Resources");
                menuResLink = "https://jpst.it/3q6X0";
                break;
            case "8":
                menuSemRes.setText("8th Semester Resources");
                menuResLink = "https://jpst.it/3q6XV";
                break;
        }

        menuSemRes.setOnClickListener(view -> {
            view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() -> {
                view.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                if (menuResLink != null) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(menuResLink));
                    startActivity(intent);
                }
            }).start();
        });
    }

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
    public void onBackPressed() {
        WebView webView = findViewById(R.id.webContent);
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else super.onBackPressed();
    }
}
