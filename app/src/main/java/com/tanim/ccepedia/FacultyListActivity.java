package com.tanim.ccepedia;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast; // Import Toast

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class FacultyListActivity extends AppCompatActivity {

    String[] facultyNames = {"Engr. Md. Razu Ahmed", "Dr. Mohammed Saifuddin", "Amanul Hoque", "Md Jiabul Hoque", "Areez Hafiz Md Zahid", "Mohammad Nadib Hasan", "Md. Humayun Kabir", "Hassan Jaki", "Zarin Tanzim"};
    String[] facultyDesignations = {"Professor & Chairman", "Assistant Professor", "Assistant Professor", "Lecturer", "Lecturer", "Lecturer", "Lecturer", "Lecturer", "Lecturer"};
    String[] facultyContactNumbers = {"01874543665", "01945767849", "01818673657", "01989685996", "01977723403", "01521483958", "01515286984", "01950853985", "01850925671"};
    int[] facultyImages = {R.drawable.razus, R.drawable.saifs, R.drawable.amans, R.drawable.jias, R.drawable.dtea, R.drawable.nadibs, R.drawable.rabbys, R.drawable.zakis, R.drawable.dtea};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_list);

        ArrayAdapter<String> facultyAdapter = new ArrayAdapter<String>(this, R.layout.faculty_item, R.id.facultyName, facultyNames) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                ImageView facultyImage = view.findViewById(R.id.facultyImage);
                TextView facultyName = view.findViewById(R.id.facultyName);
                TextView facultyDesignation = view.findViewById(R.id.facultyDesignation);
                TextView facultyContactNumber = view.findViewById(R.id.facultyContactNumber);

                facultyImage.setImageResource(facultyImages[position]);
                facultyName.setText(facultyNames[position]);
                facultyDesignation.setText(facultyDesignations[position]);
                facultyContactNumber.setText(facultyContactNumbers[position]);

                return view;
            }
        };

        ListView facultyListView = findViewById(R.id.facultyListView);
        facultyListView.setAdapter(facultyAdapter);

        // Add the OnItemClickListener for opening the dialer and displaying a toast
        facultyListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String phoneNumber = facultyContactNumbers[position];
                String facultyName = facultyNames[position];

                // Open the dialer
                dialPhoneNumber(phoneNumber);

                // Display a toast with the faculty's name
                Toast.makeText(FacultyListActivity.this, "Calling " + facultyName + " Sir", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void dialPhoneNumber(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(intent);
    }
}
