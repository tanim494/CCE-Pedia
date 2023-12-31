package com.tanim.ccepedia;

import static com.tanim.ccepedia.LoginActivity.PREFS_NAME;
import static com.tanim.ccepedia.LoginActivity.SEMESTER_KEY;
import static com.tanim.ccepedia.LoginActivity.STUDENT_NAME_KEY;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import com.onesignal.OneSignal;
import com.onesignal.debug.LogLevel;
import com.onesignal.Continue;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigation;
    LinearLayout sideMenu;
    TextView toolBarTxt;
    TextView studentName;
    TextView studentSemester;
    TextView updateBt;
    TextView noticeText;
    String noticeLink; // Declare websLink as a class variable
    String updateLink;
    float databaseVersion;    //Newer Version from database
    String menuResLink = "https://cce.iiuc.ac.bd"; //default link for menu drawer semester button
    private ImageView menuIcon; //Menu drawer icon
    DatabaseReference noticeTextDb;
    DatabaseReference noticeLinkDb;
    DatabaseReference updateVersionDb;
    DatabaseReference updateLinkDb;
    private int semesterId;
    final int animTime = 1000;
    private static final String ONESIGNAL_APP_ID = "1bb2a02a-0e83-4bb2-b810-86bfa76798d8";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseMessaging.getInstance().subscribeToTopic("notification");
        //Setting the views
        initializeViews();
        //Creating database reference
        setupFirebase();
        //Setup OneSignal
        setupOneSignal();
        //SharedPreference for Name and Semester
        setupSharedPreferences();
        //Setup database listener for data
        setupDatabaseListeners();
        //Setup different button or textview click listener
        setupClickListeners();

        FragmentManager fgMan = getSupportFragmentManager();
        FragmentTransaction tran = fgMan.beginTransaction();

        Home nFg = new Home();
        tran.replace(R.id.Midcontainer, nFg);
        tran.commit();

        bottomNavigation.setOnItemSelectedListener(item -> {
            //If side menu if open, closing it
            if (sideMenu.getVisibility() == View.VISIBLE) {
                sideMenuClose();
            }

            if (item.getItemId() == R.id.nv_home) {
                FragmentManager fgMan1 = getSupportFragmentManager();
                FragmentTransaction tran1 = fgMan1.beginTransaction();

                Home nFg1 = new Home();
                tran1.replace(R.id.Midcontainer, nFg1);
                tran1.commit();
                toolBarTxt.setText("CCE Pedia");

            } else if (item.getItemId() == R.id.nv_faculty) {
                FragmentManager fgMan1 = getSupportFragmentManager();
                FragmentTransaction tran1 = fgMan1.beginTransaction();

                Faculty nFg1 = new Faculty();
                tran1.replace(R.id.Midcontainer, nFg1);
                tran1.commit();
                toolBarTxt.setText("Faculties");

            } else if (item.getItemId() == R.id.nv_resource) {
                FragmentManager fgMan1 = getSupportFragmentManager();
                FragmentTransaction tran1 = fgMan1.beginTransaction();

                Resources nFg1 = new Resources();
                tran1.replace(R.id.Midcontainer, nFg1);
                tran1.commit();
                toolBarTxt.setText("Resources");

            } else if (item.getItemId() == R.id.nv_author) {
                FragmentManager fgMan1 = getSupportFragmentManager();
                FragmentTransaction tran1 = fgMan1.beginTransaction();

                Author nFg1 = new Author();
                tran1.replace(R.id.Midcontainer, nFg1);
                tran1.commit();
                toolBarTxt.setText("Author");

            }
            return true;
        });
    }

    private void setupOneSignal() {
        // Verbose Logging set to help debug issues, remove before releasing your app.
        OneSignal.getDebug().setLogLevel(LogLevel.VERBOSE);

        // OneSignal Initialization
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID);

        // requestPermission will show the native Android notification permission prompt.
        // NOTE: It's recommended to use a OneSignal In-App Message to prompt instead.
        OneSignal.getNotifications().requestPermission(true, Continue.with(r -> {
            if (r.isSuccess()) {
                if (r.getData()) {
                    // `requestPermission` completed successfully and the user has accepted permission
                }
                else {
                    // `requestPermission` completed successfully but the user has rejected permission
                }
            }
            else {
                // `requestPermission` completed unsuccessfully, check `r.getThrowable()` for more info on the failure reason
            }
        }));
    }

    private void initializeViews() {
        // Initialize UI views here
        menuIcon = findViewById(R.id.menuIcon);
        sideMenu = findViewById(R.id.sideMenu);
        toolBarTxt = findViewById(R.id.toolText);
        updateBt = findViewById(R.id.updateButton);
        noticeText = findViewById(R.id.noticeButton);
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void setupFirebase() {
        // Set up Firebase here
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://cce-pedia-5284c-default-rtdb.asia-southeast1.firebasedatabase.app/");
        noticeTextDb = database.getReference("message"); //Text to show in notice box
        noticeLinkDb = database.getReference("link"); //Link to open when notice box is clicked
        updateVersionDb = database.getReference("version"); //Newest version
        updateLinkDb = database.getReference("update-link"); //Link to download newest version
    }

    private void setupSharedPreferences() {
        // Set up SharedPreferences here
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String studentName = settings.getString(STUDENT_NAME_KEY, "Name"); //Default "Name" when no input is found
        semesterId = settings.getInt(SEMESTER_KEY, 1); // Default to Semester 1 if not found

        this.studentName = findViewById(R.id.stuName); //Student Name in menu drawer
        studentSemester = findViewById(R.id.stuSem); // Student Semester in menu drawer
        this.studentName.setText("Name: " + studentName);
        studentSemester.setText("Semester: " + semesterId);
    }

    private void setupDatabaseListeners() {
        //Fetching the notice text
        noticeTextDb.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String message = snapshot.getValue(String.class);
                noticeText.setText(message);
                noticeText.setTextSize(15);
                noticeText.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
        // Fetch the link to open when notice box is clicked
        noticeLinkDb.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                noticeLink = snapshot.getValue(String.class);
                // To open link when notice button is clicked
                noticeText.setOnClickListener(view -> {
                    if (noticeLink != null) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(noticeLink));
                        startActivity(intent);
                    }
                });
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
        // Fetch the newest version in decimal
        updateVersionDb.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                float userVersion = Float.parseFloat(BuildConfig.VERSION_NAME) * 100; //User installed version in decimal
                databaseVersion = Integer.parseInt(Objects.requireNonNull(snapshot.getValue(String.class)));
                if (databaseVersion > userVersion) {
                    updateBt.setVisibility(View.VISIBLE);
                    updateBt.setText("Update available, Click to download");
                    updateBt.setOnClickListener(view -> {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.valueOf(updateLink)));
                        startActivity(intent);
                    });
                } else {
                    updateBt.setVisibility(View.GONE);
                }
            }
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
        //Fetch the update link
        updateLinkDb.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                updateLink = snapshot.getValue(String.class);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void setupClickListeners() {
        // Set up click listeners here
        menuIcon.setOnClickListener(v -> {

            if (sideMenu.getVisibility() == View.VISIBLE) {
                sideMenuClose();
            } else {
                sideMenuOpen();
            }
        });
        Button ediIn = findViewById(R.id.editInfo);
        ediIn.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        TextView menuSemRes = findViewById(R.id.menuSemRes);
        menuSemRes.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(menuResLink));
            startActivity(intent);
        });
        switch (semesterId) {
            case 1:
                menuSemRes.setText("1st Semester Resources");
                menuResLink = "https://jpst.it/3q4Ai";
                break;
            case 2:
                menuSemRes.setText("2nd Semester Resources");
                menuResLink = "https://jpst.it/3q4Em";
                break;
            case 3:
                menuSemRes.setText("3rd Semester Resources");
                menuResLink = "https://jpst.it/3q4I3";
                break;
            case 4:
                menuSemRes.setText("4th Semester Resources");
                menuResLink = "https://jpst.it/3q4Jm";
                break;
            case 5:
                menuSemRes.setText("5th Semester Resources");
                menuResLink = "https://jpst.it/3q6UJ";
                break;
            case 6:
                menuSemRes.setText("6th Semester Resources");
                menuResLink = "https://jpst.it/3q6Wm";
                break;
            case 7:
                menuSemRes.setText("7th Semester Resources");
                menuResLink = "https://jpst.it/3q6X0";
                break;
            case 8:
                menuSemRes.setText("8th Semester Resources");
                menuResLink = "https://jpst.it/3q6XV";
                break;
        }
    }

    private void sideMenuOpen() {
        Animation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(animTime); // Set the duration in milliseconds
        findViewById(R.id.sideMenu).startAnimation(fadeIn);
        sideMenu.setVisibility(View.VISIBLE);
        menuIcon.setImageResource(R.drawable.nv_mn_close);
    }

    private void sideMenuClose() {
        Animation fadeOut = new AlphaAnimation(1f, 0f);
        fadeOut.setDuration(animTime); // Set the duration in milliseconds
        findViewById(R.id.sideMenu).startAnimation(fadeOut);
        sideMenu.setVisibility(View.INVISIBLE);
        menuIcon.setImageResource(R.drawable.nv_menu);
    }

    @Override
    public void onBackPressed() {
        WebView webView = findViewById(R.id.webContent);
        if (webView != null && webView.canGoBack()) {
            webView.goBack(); // Go back in WebView history
        } else {
            super.onBackPressed();
        }
    }
}
