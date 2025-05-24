package com.tanim.ccepedia;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class BusScheduleFragment extends Fragment {

    private ImageView scheduleImage;
    private TextView scheduleTitle;
    private ProgressBar loadingSpinner;
    private LinearLayout contactsLayout;
    private Button downloadButton;
    private FirebaseFirestore db;
    private String imageUrl;

    private static final int REQUEST_WRITE_STORAGE = 112;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_bus_schedule, container, false);

        scheduleImage = rootView.findViewById(R.id.scheduleImage);
        scheduleTitle = rootView.findViewById(R.id.scheduleTitle);
        contactsLayout = rootView.findViewById(R.id.contactsLayout);
        downloadButton = rootView.findViewById(R.id.downloadButton);
        loadingSpinner = rootView.findViewById(R.id.loadingSpinner);

        db = FirebaseFirestore.getInstance();

        loadingSpinner.setVisibility(View.VISIBLE);
        loadBusScheduleFromFirestore();

        downloadButton.setOnClickListener(v -> {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                checkPermissionAndDownload(imageUrl);
            } else {
                Toast.makeText(requireContext(), "No image to download", Toast.LENGTH_SHORT).show();
            }
        });

        return rootView;
    }

    private void loadBusScheduleFromFirestore() {
        db.collection("resources").document("bus_schedule")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        imageUrl = documentSnapshot.getString("url");
                        String title = documentSnapshot.getString("title");
                        List<Map<String, Object>> contacts = (List<Map<String, Object>>) documentSnapshot.get("contacts");

                        if (title != null) {
                            scheduleTitle.setText(title);
                        } else {
                            scheduleTitle.setText("Bus Schedule");
                        }

                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(requireContext())
                                    .load(imageUrl)
                                    .into(scheduleImage);
                            loadingSpinner.setVisibility(View.GONE);
                        } else {
                            loadingSpinner.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), "No image URL found", Toast.LENGTH_SHORT).show();
                        }

                        if (contacts != null && !contacts.isEmpty()) {
                            addContactsToLayout(contacts);
                        } else {
                            TextView noContacts = new TextView(requireContext());
                            noContacts.setText("No important contacts found");
                            contactsLayout.addView(noContacts);
                        }
                    } else {
                        Toast.makeText(requireContext(), "No bus schedule found in database", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to load bus schedule", Toast.LENGTH_SHORT).show());
    }

    private void addContactsToLayout(List<Map<String, Object>> contacts) {
        contactsLayout.removeAllViews();

        for (Map<String, Object> contact : contacts) {
            String name = (String) contact.get("name");
            String phone = (String) contact.get("phone");

            if (name != null && phone != null) {
                TextView contactTextView = new TextView(requireContext(), null, 0, R.style.ButtonStyle);
                contactTextView.setText(name + ": " + phone);
                contactTextView.setAllCaps(false); // Disable all caps if needed
                contactTextView.setPadding(24, 24, 24, 24);

                // Set margin bottom to space between buttons
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, 0, 24);
                contactTextView.setLayoutParams(params);

                contactTextView.setOnClickListener(v -> {
                    Intent callIntent = new Intent(Intent.ACTION_DIAL);
                    callIntent.setData(Uri.parse("tel:" + phone));
                    Toast.makeText(requireContext(), "Calling " + name, Toast.LENGTH_SHORT).show();
                    startActivity(callIntent);
                });

                contactsLayout.addView(contactTextView);
            }
        }
    }


    private void checkPermissionAndDownload(String imageUrl) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE);
            this.imageUrl = imageUrl;
        } else {
            new DownloadImageTask().execute(imageUrl);
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Now permission is granted, retry download
                if (imageUrl != null) {
                    new DownloadImageTask().execute(imageUrl);
                }
            } else {
                Toast.makeText(requireContext(), "Permission denied. Cannot download file.", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private class DownloadImageTask extends AsyncTask<String, Void, Boolean> {

        private String savedFilePath = null;

        @Override
        protected Boolean doInBackground(String... urls) {
            String urlToDownload = urls[0];

            try {
                URL url = new URL(urlToDownload);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream input = connection.getInputStream();

                String extension = urlToDownload.substring(urlToDownload.lastIndexOf("."));
                String fileName = "bus_schedule_" + System.currentTimeMillis() + extension;

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                    values.put(MediaStore.Downloads.MIME_TYPE, "image/" + extension.replace(".", ""));
                    values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/CCE Pedia/Bus Schedule");

                    Uri uri = requireContext().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

                    if (uri != null) {
                        OutputStream output = requireContext().getContentResolver().openOutputStream(uri);
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = input.read(buffer)) != -1) {
                            output.write(buffer, 0, bytesRead);
                        }
                        output.close();
                        input.close();
                        savedFilePath = Environment.DIRECTORY_DOWNLOADS + "/CCE Pedia/Bus Schedule/" + fileName;
                        return true;
                    } else {
                        // ❗ Handle null uri case
                        return false;
                    }

                } else {
                    // Android 9 and below
                    File downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    File ccePediaFolder = new File(downloadsFolder, "CCE Pedia/Bus Schedule");
                    if (!ccePediaFolder.exists()) ccePediaFolder.mkdirs();

                    File file = new File(ccePediaFolder, fileName);
                    savedFilePath = file.getAbsolutePath();

                    FileOutputStream output = new FileOutputStream(file);
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = input.read(buffer)) != -1) {
                        output.write(buffer, 0, bytesRead);
                    }

                    output.close();
                    input.close();
                    return true;
                }

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

            // No unreachable code here — every path now returns
        }


        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(requireContext(), "Downloaded to: " + savedFilePath, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(requireContext(), "Failed to download bus schedule", Toast.LENGTH_SHORT).show();
            }
        }
    }


}
