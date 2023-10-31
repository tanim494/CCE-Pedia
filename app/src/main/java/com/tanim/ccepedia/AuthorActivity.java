package com.tanim.ccepedia;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class AuthorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_author);

        // GitHub Button
        Button githubButton = findViewById(R.id.githubButton);
        githubButton.setOnClickListener(view -> openGitHubProfile());

        // Facebook Button
        Button facebookButton = findViewById(R.id.facebookButton);
        facebookButton.setOnClickListener(view -> openFacebookProfile());

        Button websiteButton = findViewById(R.id.websiteButton);
        websiteButton.setOnClickListener(view -> openWebsite());

        Button formButton = findViewById(R.id.formButton);
        formButton.setOnClickListener(view -> openForm());
    }

    private void openGitHubProfile() {
        // Replace with your GitHub profile URL
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/tanim494"));
        startActivity(intent);
    }

    private void openFacebookProfile() {
        // Replace with your Facebook profile URL
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://m.facebook.com/profile.php/?id=100012799027969"));
        startActivity(intent);
    }

    private void openWebsite() {
        // Replace with your GitHub profile URL
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://tanim.codes"));
        startActivity(intent);
    }

    private void openForm() {
        // Replace with your GitHub profile URL
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://forms.gle/FbP5xcFKteDjGVZr5"));
        startActivity(intent);
    }
}
