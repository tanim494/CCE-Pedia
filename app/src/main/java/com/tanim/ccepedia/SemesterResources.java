package com.tanim.ccepedia;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
 public class SemesterResources extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_semester_resources, container, false);

            // Find the TextViews by their IDs
            TextView semester1 = rootView.findViewById(R.id.semester1);
            TextView semester2 = rootView.findViewById(R.id.semester2);
            TextView semester3 = rootView.findViewById(R.id.semester3);
            TextView semester4 = rootView.findViewById(R.id.semester4);
            TextView semester5 = rootView.findViewById(R.id.semester5);
            TextView semester6 = rootView.findViewById(R.id.semester6);
            TextView semester7 = rootView.findViewById(R.id.semester7);
            TextView semester8 = rootView.findViewById(R.id.semester8);

            // Set click listeners for each TextView
            semester1.setOnClickListener(v -> openWebPage("https://jpst.it/3q4Ai"));

            semester2.setOnClickListener(v -> openWebPage("https://jpst.it/3q4Em"));

            semester3.setOnClickListener(v -> openWebPage("https://jpst.it/3q4I3"));

            semester4.setOnClickListener(v -> openWebPage("https://jpst.it/3q4Jm"));

            semester5.setOnClickListener(v -> openWebPage("https://jpst.it/3q6UJ"));

            semester6.setOnClickListener(v -> openWebPage("https://jpst.it/3q6Wm"));

            semester7.setOnClickListener(v -> openWebPage("https://jpst.it/3q6X0"));

            semester8.setOnClickListener(v -> openWebPage("https://jpst.it/3q6XV"));

            return rootView;
        }

        private void openWebPage(String url) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        }
    }
