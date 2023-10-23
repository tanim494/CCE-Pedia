package com.tanim.ccepedia;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ResourcesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resources);
    }

    public void openFacebookPage(View view) {
        String url = "https://www.facebook.com/profile.php?id=100090282199663";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
        showToast("Opening CCE Club Facebook Page");
    }

    public void openFacebookFemPage(View view) {
        String url = "https://www.facebook.com/profile.php?id=100091710725410";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
        showToast("Opening CCE Club Female Chapter Facebook Page");
    }

    public void openQuestionPage(View view) {
        String url = "https://drive.google.com/drive/folders/19kpQtcze690uvLwJRz0uhfmy4Ji0qp7e";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
        showToast("Opening Previous Question and Books");
    }

    public void openIPage(View view) {
        String url = "https://www.iiuc.ac.bd/cce/bachelor";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
        showToast("Opening IIUC CCE Website");
    }

    public void openBusPage(View view) {
        String url = "https://jpst.it/3q4Pl";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
        showToast("Opening Bus Schedule");
    }

    public void openRoutinePage(View view) {
        String url = "https://jpst.it/3q4Rc";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
        showToast("Opening Class Routine");
    }

    public void openImpPage(View view) {
        String url = "https://jpst.it/3q6PY";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
        showToast("Opening Important Resources");
    }

    public void openSemesterResourcesPage(View view) {
        // Create an intent to open the SemesterResourcesActivity
        Intent intent = new Intent(this, SemesterResourcesActivity.class);
        startActivity(intent);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
