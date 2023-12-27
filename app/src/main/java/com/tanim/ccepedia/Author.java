package com.tanim.ccepedia;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

public class Author extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_author, container, false);

        // GitHub Button
        LinearLayout githubButton = view.findViewById(R.id.githubButton);
        githubButton.setOnClickListener(view1 -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/tanim494"));
            startActivity(intent);
        });

        // Facebook Button
        LinearLayout facebookButton = view.findViewById(R.id.facebookButton);
        facebookButton.setOnClickListener(view1 -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://m.facebook.com/tanim494"));
            startActivity(intent);
        });

        LinearLayout websiteButton = view.findViewById(R.id.websiteButton);
        websiteButton.setOnClickListener(view1 -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://tanim.codes"));
            startActivity(intent);
        });

        LinearLayout authorAddress = view.findViewById(R.id.authorAddress);
        authorAddress.setOnClickListener(view1 -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.app.goo.gl/KimSYJ3GMz9F6QfMA"));
            startActivity(intent);
        });

        TextView requestButton = view.findViewById(R.id.requestButton);
        requestButton.setOnClickListener(view1 -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://forms.gle/FbP5xcFKteDjGVZr5"));
            startActivity(intent);
        });

        LinearLayout authorMail = view.findViewById(R.id.authorMail);
        authorMail.setOnClickListener(view1 -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("message/rfc822"); // Set the MIME type for email

            intent.putExtra(Intent.EXTRA_EMAIL, new String[] {"Tanim494@gmail.com"}); // Replace with the recipient's email address
            intent.putExtra(Intent.EXTRA_SUBJECT, "Request Feature or Suggestion for CCE Pedia"); // Set the email subject
            intent.putExtra(Intent.EXTRA_TEXT, "I am <-Your Name->, from CCE Batch - . \n I have a suggestion for CCE Pedia. \n ............."); // Set the email body

            try {
                startActivity(Intent.createChooser(intent, "Send Email"));
            } catch (ActivityNotFoundException e) {
                // Handle the case where no email client is installed on the device
                Toast.makeText(getActivity(), "No email app installed", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

}