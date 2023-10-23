package com.tanim.ccepedia;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainMenuActivity extends AppCompatActivity {

    TextView notTx;
    TextView upDt;
    String webLink; // Declare websLink as a class variable
    String updateLink; //Newer version download link
    float coVer;    //Newer Version in decimal
    float userVer =  Float.parseFloat(BuildConfig.VERSION_NAME) * 100; //User installed version in decimal


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        notTx = findViewById(R.id.noticeText);
        upDt = findViewById(R.id.updateButton);

        FirebaseDatabase database = FirebaseDatabase.getInstance("https://cce-pedia-5284c-default-rtdb.asia-southeast1.firebasedatabase.app/");
        DatabaseReference comingText = database.getReference("message"); //Text to show in notice box
        DatabaseReference comingLink = database.getReference("link"); //Link to open when notice box is clicked
        DatabaseReference comingVer = database.getReference("version"); //Newest version
        DatabaseReference upLink = database.getReference("update-link"); //Link to download newest version

        // Fetch the message for notice box
        comingText.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String mes = snapshot.getValue(String.class);
                notTx.setText(mes);
                notTx.setTextSize(15);
                notTx.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        // Fetch the link to open when notice box clicked
        comingLink.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                webLink = snapshot.getValue(String.class);

                // Button to open the web link
                TextView openWebButton = findViewById(R.id.noticeText);
                openWebButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (webLink != null) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(webLink));
                            startActivity(intent);
                        }
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        // Fetch the newest version in decimal
        comingVer.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                coVer = Integer.parseInt(snapshot.getValue(String.class));
                if (coVer > userVer) {
                    upDt.setVisibility(View.VISIBLE);
                    TextView upBt = findViewById(R.id.updateButton);
                    upBt.setText("Update available, Click to download");
                    upBt.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(updateLink));
                                startActivity(intent);
                        }
                    });

                } else {
                    upDt.setVisibility(View.GONE);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        //Fetch the link to download newest version
        upLink.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                updateLink = snapshot.getValue(String.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });



        // Add buttons for other activities
        ImageButton introductionButton = findViewById(R.id.introductionButton);
        introductionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainMenuActivity.this, IntroductionActivity.class);
                startActivity(intent);
            }
        });

        ImageButton facultiesButton = findViewById(R.id.facultyButton);
        facultiesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainMenuActivity.this, FacultyListActivity.class);
                startActivity(intent);
            }
        });

        ImageButton resourcesButton = findViewById(R.id.resourcesButton);
        resourcesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainMenuActivity.this, ResourcesActivity.class);
                startActivity(intent);
            }
        });

        ImageButton authorButton = findViewById(R.id.authorButton);
        authorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainMenuActivity.this, AuthorActivity.class);
                startActivity(intent);
            }
        });
    }
}
