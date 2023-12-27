package com.tanim.ccepedia;

import static com.tanim.ccepedia.LoginActivity.PREFS_NAME;
import static com.tanim.ccepedia.LoginActivity.SEMESTER_KEY;
import static com.tanim.ccepedia.LoginActivity.STUDENT_NAME_KEY;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

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
    float databaseVersion;    //Newer Version from database
    float userVersion =  Float.parseFloat(BuildConfig.VERSION_NAME) * 100; //User installed version in decimal
    String menuResLink = "https://cce.iiuc.ac.bd"; //default link for menu drawer semester button
    private ImageView menuIcon; //Menu drawer icon

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        menuIcon = findViewById(R.id.menuIcon);
        sideMenu = findViewById(R.id.sideMenu);
        toolBarTxt = findViewById(R.id.toolText);
        updateBt = findViewById(R.id.updateButton);
        noticeText = findViewById(R.id.noticeButton);
        bottomNavigation  = findViewById(R.id.bottomNavigation);

        FirebaseDatabase database = FirebaseDatabase.getInstance("https://cce-pedia-5284c-default-rtdb.asia-southeast1.firebasedatabase.app/");
        DatabaseReference noticeTextDb = database.getReference("message"); //Text to show in notice box
        DatabaseReference noticeLinkDb = database.getReference("link"); //Link to open when notice box is clicked
        DatabaseReference updateVersionDb = database.getReference("version"); //Newest version
        DatabaseReference updateLinkDb = database.getReference("update-link"); //Link to download newest version

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String studentName = settings.getString(STUDENT_NAME_KEY, "Name");
        int semesterId = settings.getInt(SEMESTER_KEY, 1); // Default to Semester 1 if not found

        this.studentName = findViewById(R.id.stuName); //Student Name in menu drawer
        studentSemester = findViewById(R.id.stuSem); // Student Semester in menu drawer
        this.studentName.setText("Name: " + studentName);
        studentSemester.setText("Semester: " + semesterId);

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
        // Fetch the link to open when notice box clicked
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
                databaseVersion = Integer.parseInt(Objects.requireNonNull(snapshot.getValue(String.class)));
                if (databaseVersion > userVersion) {
                    updateBt.setVisibility(View.VISIBLE);
                    updateBt.setText("Update available, Click to download");
                    updateBt.setOnClickListener(view -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.valueOf(updateLinkDb)));
                    startActivity(intent);
                    });
                } else {
                    updateBt.setVisibility(View.GONE);
                }
            }
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

                    menuIcon.setOnClickListener(v -> {
                        if (sideMenu.getVisibility() == View.VISIBLE) {
                            sideMenu.setVisibility(View.INVISIBLE);
                            menuIcon.setImageResource(R.drawable.menu);
                        } else {
                            sideMenu.setVisibility(View.VISIBLE);
                            menuIcon.setImageResource(R.drawable.mn_close);
                        }
                    });

                    Button ediIn = findViewById(R.id.editInfo);
                    ediIn.setOnClickListener(view -> {
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        startActivity(intent);
                    });

                    FragmentManager fgMan = getSupportFragmentManager();
                    FragmentTransaction tran = fgMan.beginTransaction();

                    Home nFg = new Home();
                    tran.replace(R.id.Midcontainer, nFg);
                    tran.commit();


                    bottomNavigation.setOnNavigationItemSelectedListener(item -> {

                        if (item.getItemId() == R.id.nv_home) {
                            FragmentManager fgMan1 = getSupportFragmentManager();
                            FragmentTransaction tran1 = fgMan1.beginTransaction();

                            Home nFg1 = new Home();
                            tran1.replace(R.id.Midcontainer, nFg1);
                            tran1.commit();
                            toolBarTxt.setText("CCE Pedia");
                            sideMenu.setVisibility(View.INVISIBLE);
                            menuIcon.setImageResource(R.drawable.menu);

                        } else if (item.getItemId() == R.id.nv_faculty) {
                            FragmentManager fgMan1 = getSupportFragmentManager();
                            FragmentTransaction tran1 = fgMan1.beginTransaction();

                            Faculty nFg1 = new Faculty();
                            tran1.replace(R.id.Midcontainer, nFg1);
                            tran1.commit();
                            toolBarTxt.setText("Faculties");
                            sideMenu.setVisibility(View.INVISIBLE);
                            menuIcon.setImageResource(R.drawable.menu);

                        } else if (item.getItemId() == R.id.nv_resource) {
                            FragmentManager fgMan1 = getSupportFragmentManager();
                            FragmentTransaction tran1 = fgMan1.beginTransaction();

                            Resources nFg1 = new Resources();
                            tran1.replace(R.id.Midcontainer, nFg1);
                            tran1.commit();
                            toolBarTxt.setText("Resources");
                            sideMenu.setVisibility(View.INVISIBLE);
                            menuIcon.setImageResource(R.drawable.menu);

                        } else if (item.getItemId() == R.id.nv_author) {
                            FragmentManager fgMan1 = getSupportFragmentManager();
                            FragmentTransaction tran1 = fgMan1.beginTransaction();

                            Author nFg1 = new Author();
                            tran1.replace(R.id.Midcontainer, nFg1);
                            tran1.commit();
                            toolBarTxt.setText("Author");
                            sideMenu.setVisibility(View.INVISIBLE);
                            menuIcon.setImageResource(R.drawable.menu);

                        }
                        return true;
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
            }
