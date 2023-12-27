package com.tanim.ccepedia;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class Home extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Handle the website link
        TextView websiteLink = view.findViewById(R.id.websiteLink);
        websiteLink.setOnClickListener(view1 -> {
            // Open the website link when clicked
            String url = "https://drive.google.com/file/d/16WpJ6bwxiWST7WjKM9h473h5vmHgSFBb/view?usp=drive_link";
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        });

        return view;
    }
}
