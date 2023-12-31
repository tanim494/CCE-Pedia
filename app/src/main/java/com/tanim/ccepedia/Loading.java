package com.tanim.ccepedia;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class Loading extends AppCompatActivity {
    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String FIRSTRUN_KEY = "firstrun";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);


        new Handler().postDelayed(() -> {
            if (isFirstRun()) {
                // First run: open the login activity
                Toast.makeText(Loading.this, "To continue, Enter your info.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Loading.this, LoginActivity.class);
                startActivity(intent);
            } else {
                // Not the first run: open the main activity
                Intent intent = new Intent(Loading.this, MainActivity.class);
                startActivity(intent);
            }
            finish(); // Close the splash activity
        }, 1500); // 3000 milliseconds (3 seconds)

    }
    private boolean isFirstRun() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean firstRun = preferences.getBoolean(FIRSTRUN_KEY, true);
        if (firstRun) {
            // Set the flag to false to indicate that it's no longer the first run
            preferences.edit().putBoolean(FIRSTRUN_KEY, false).apply();
        }
        return firstRun;
    }
}
