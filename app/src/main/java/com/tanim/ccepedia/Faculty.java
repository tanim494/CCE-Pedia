package com.tanim.ccepedia;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class Faculty extends Fragment {

    String[] facultyNames = {"Eng. Md. Razu Ahmed", "Dr. Mohammed Saifuddin", "Amanul Hoque", "Md Jiabul Hoque", "Areez Hafiz Md Zahid", "Mohammad Nadib Hasan", "Md. Humayun Kabir", "Hassan Jaki", "Zarin Tanzim"};
    String[] facultyDesignations = {"Professor & Chairman", "Assistant Professor", "Assistant Professor", "Lecturer", "Lecturer", "Lecturer", "Lecturer", "Lecturer", "Lecturer"};
    String[] facultyContactNumbers = {"01874543665", "01945767849", "01818673657", "01989685996", "01977723403", "01521483958", "01515286984", "01950853985", "01850925671"};
    int[] facultyImages = {R.drawable.razus, R.drawable.saifs, R.drawable.amans, R.drawable.jias, R.drawable.dtea, R.drawable.nadibs, R.drawable.rabbys, R.drawable.zakis, R.drawable.dtea};

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_faculty, container, false);

        FacultyAdapter facultyAdapter = new FacultyAdapter(requireContext(), facultyNames, facultyDesignations, facultyContactNumbers, facultyImages);

        ListView facultyListView = rootView.findViewById(R.id.facultyListView);
        facultyListView.setAdapter(facultyAdapter);

        // Add the OnItemClickListener for opening the dialer and displaying a toast
        facultyListView.setOnItemClickListener((parent, view, position, id) -> {
            String phoneNumber = facultyContactNumbers[position];
            String facultyName = facultyNames[position];

            // Open the dialer
            dialPhoneNumber(phoneNumber);

            // Display a toast with the Faculty's name
            Toast.makeText(requireContext(), "Calling " + facultyName + " Sir", Toast.LENGTH_SHORT).show();
        });

        return rootView;
    }

    private void dialPhoneNumber(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(intent);
    }

    // Custom ArrayAdapter to display Faculty information
    private static class FacultyAdapter extends ArrayAdapter<String> {
        private final String[] facultyNames;
        private final String[] facultyDesignations;
        private final String[] facultyContactNumbers;
        private final int[] facultyImages;

        FacultyAdapter(@NonNull android.content.Context context, String[] facultyNames, String[] facultyDesignations, String[] facultyContactNumbers, int[] facultyImages) {
            super(context, R.layout.faculty_item, facultyNames);
            this.facultyNames = facultyNames;
            this.facultyDesignations = facultyDesignations;
            this.facultyContactNumbers = facultyContactNumbers;
            this.facultyImages = facultyImages;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                view = inflater.inflate(R.layout.faculty_item, parent, false);
            }

            ImageView facultyImage = view.findViewById(R.id.facultyImage);
            facultyImage.setImageResource(facultyImages[position]);

            TextView facultyName = view.findViewById(R.id.fName);
            facultyName.setText(facultyNames[position]);

            TextView facultyDesignation = view.findViewById(R.id.fDesignation);
            facultyDesignation.setText(facultyDesignations[position]);

            TextView facultyPhoneNumber = view.findViewById(R.id.fPhoneNumber);
            facultyPhoneNumber.setText(facultyContactNumbers[position]);

            return view;
        }
    }
}
