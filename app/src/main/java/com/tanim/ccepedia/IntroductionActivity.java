package com.tanim.ccepedia;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class IntroductionActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_introduction);

        // Handle the website link
        TextView websiteLink = findViewById(R.id.websiteLink);
        websiteLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Open the website link when clicked
                String url = "https://drive.google.com/file/d/16WpJ6bwxiWST7WjKM9h473h5vmHgSFBb/view?usp=drive_link";
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
            }
        });
    }
}
