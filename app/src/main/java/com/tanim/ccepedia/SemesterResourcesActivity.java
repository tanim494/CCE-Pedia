package com.tanim.ccepedia;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SemesterResourcesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_semester_resources);
    }

    public void openSemester1(View view) {
        String url = "https://jpst.it/3q4Ai";
        openWebPage(url);
        showToast("Opening 1st Semester Resources");
    }

    public void openSemester2(View view) {
        String url = "https://jpst.it/3q4Em";
        openWebPage(url);
        showToast("Opening 2nd Semester Resources");
    }

    public void openSemester3(View view) {
        String url = "https://jpst.it/3q4I3";
        openWebPage(url);
        showToast("Opening 3rd Semester Resources");
    }

    public void openSemester4(View view) {
        String url = "https://jpst.it/3q4Jm";
        openWebPage(url);
        showToast("Opening 4th Semester Resources");
    }

    public void openSemester5(View view) {
        String url = "https://jpst.it/3q6UJ";
        openWebPage(url);
        showToast("Opening 5th Semester Resources");
    }

    public void openSemester6(View view) {
        String url = "https://jpst.it/3q6Wm";
        openWebPage(url);
        showToast("Opening 6th Semester Resources");
    }

    public void openSemester7(View view) {
        String url = "https://jpst.it/3q6X0";
        openWebPage(url);
        showToast("Opening 7th Semester Resources");
    }

    public void openSemester8(View view) {
        String url = "https://jpst.it/3q6XV";
        openWebPage(url);
        showToast("Opening 8th Semester Resources");
    }

    private void openWebPage(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
