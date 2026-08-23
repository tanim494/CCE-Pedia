package com.tanim.ccepedia;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BusScheduleFragment extends Fragment {

    private ViewPager2 schedulePager;
    private TabLayout scheduleTabs;
    private ProgressBar loadingSpinner;
    private LinearLayout contactsLayout;
    private MaterialButton downloadButton, shareButton;

    private FirebaseFirestore db;
    private String appUpdateLink;
    private String pendingDownloadUrl;

    // Parallel lists: the swipeable pages and their tab labels (e.g. "Regular", "Friday").
    private final List<String> pageUrls = new ArrayList<>();
    private final List<String> pageLabels = new ArrayList<>();

    private static final int REQUEST_WRITE_STORAGE = 112;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_bus_schedule, container, false);

        schedulePager = rootView.findViewById(R.id.schedulePager);
        scheduleTabs = rootView.findViewById(R.id.scheduleTabs);
        contactsLayout = rootView.findViewById(R.id.contactsLayout);
        downloadButton = rootView.findViewById(R.id.downloadButton);
        shareButton = rootView.findViewById(R.id.shareButton);
        loadingSpinner = rootView.findViewById(R.id.loadingSpinner);

        db = FirebaseFirestore.getInstance();

        loadingSpinner.setVisibility(View.VISIBLE);
        fetchAppConfig();
        loadBusScheduleFromFirestore();

        downloadButton.setOnClickListener(v -> {
            String url = currentUrl();
            if (url != null && !url.isEmpty()) {
                pendingDownloadUrl = url;
                checkPermissionAndDownload(url);
            } else {
                Toast.makeText(requireContext(), "Download link not available", Toast.LENGTH_SHORT).show();
            }
        });

        shareButton.setOnClickListener(v -> {
            String url = currentUrl();
            if (url != null && !url.isEmpty()) {
                shareCurrentSchedule();
            } else {
                Toast.makeText(requireContext(), "Nothing to share yet", Toast.LENGTH_SHORT).show();
            }
        });

        return rootView;
    }

    /** URL of the page the user is currently viewing (Regular or Friday). */
    private String currentUrl() {
        if (pageUrls.isEmpty()) return null;
        int pos = schedulePager.getCurrentItem();
        if (pos < 0 || pos >= pageUrls.size()) return null;
        return pageUrls.get(pos);
    }

    private String currentLabel() {
        int pos = schedulePager.getCurrentItem();
        if (pos >= 0 && pos < pageLabels.size()) return pageLabels.get(pos);
        return "";
    }

    // Reads the app-download promo link used in the share caption, mirroring WebFragment.
    private void fetchAppConfig() {
        db.collection("appConfig").document("main")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        appUpdateLink = documentSnapshot.getString("updateLink");
                    }
                });
    }

    private void loadBusScheduleFromFirestore() {
        db.collection("resources").document("bus_schedule")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!isAdded()) return;

                    if (documentSnapshot.exists()) {
                        String url = documentSnapshot.getString("url");
                        String fridayUrl = documentSnapshot.getString("fridayUrl");
                        List<Map<String, Object>> contacts = (List<Map<String, Object>>) documentSnapshot.get("contacts");

                        pageUrls.clear();
                        pageLabels.clear();
                        if (url != null && !url.isEmpty()) {
                            pageUrls.add(url);
                            pageLabels.add("Regular");
                        }
                        if (fridayUrl != null && !fridayUrl.isEmpty()) {
                            pageUrls.add(fridayUrl);
                            pageLabels.add("Friday");
                        }

                        loadingSpinner.setVisibility(View.GONE);

                        if (!pageUrls.isEmpty()) {
                            schedulePager.setVisibility(View.VISIBLE);
                            schedulePager.setAdapter(new BusSchedulePagerAdapter(requireContext(), pageUrls));

                            if (pageUrls.size() > 1) {
                                scheduleTabs.setVisibility(View.VISIBLE);
                                new TabLayoutMediator(scheduleTabs, schedulePager,
                                        (tab, position) -> tab.setText(pageLabels.get(position))
                                ).attach();
                            } else {
                                scheduleTabs.setVisibility(View.GONE);
                            }
                        } else {
                            schedulePager.setVisibility(View.GONE);
                            scheduleTabs.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), "No image URL found", Toast.LENGTH_SHORT).show();
                        }

                        if (contacts != null && !contacts.isEmpty()) {
                            addContactsToLayout(contacts);
                        } else {
                            contactsLayout.removeAllViews();
                            TextView noContacts = new TextView(requireContext());
                            noContacts.setText("No important contacts found");
                            contactsLayout.addView(noContacts);
                        }
                    } else {
                        loadingSpinner.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "No bus schedule found in database", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    loadingSpinner.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Failed to load bus schedule", Toast.LENGTH_SHORT).show();
                });
    }

    private void addContactsToLayout(List<Map<String, Object>> contacts) {
        contactsLayout.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (Map<String, Object> contact : contacts) {
            String name = (String) contact.get("name");
            String phone = (String) contact.get("phone");

            if (name != null && phone != null) {
                View contactView = inflater.inflate(R.layout.item_contact, contactsLayout, false);
                TextView nameView = contactView.findViewById(R.id.contactNameTextView);
                TextView numberView = contactView.findViewById(R.id.contactNumberTextView);

                nameView.setText(name);
                numberView.setText(phone);

                contactView.setOnClickListener(v -> {
                    Intent callIntent = new Intent(Intent.ACTION_DIAL);
                    callIntent.setData(Uri.parse("tel:" + phone));
                    startActivity(callIntent);
                });

                contactsLayout.addView(contactView);
            }
        }
    }

    // Shares the currently visible schedule image as an actual picture, with the app promo caption.
    // Uses the app's private cache (no storage permission) + FileProvider, like PdfViewerFragment.
    private void shareCurrentSchedule() {
        final String url = currentUrl();
        final String label = currentLabel();
        if (url == null || url.isEmpty()) {
            Toast.makeText(requireContext(), "Nothing to share yet", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(requireContext(), "Preparing image…", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            Uri shareUri = null;
            try {
                URL downloadUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) downloadUrl.openConnection();
                connection.connect();

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream input = connection.getInputStream();

                    String extension = url.contains(".") ? url.substring(url.lastIndexOf(".")) : ".jpg";
                    if (extension.length() > 5 || extension.contains("/")) {
                        extension = ".jpg";
                    }

                    File file = new File(requireContext().getCacheDir(),
                            "share_bus_" + System.currentTimeMillis() + extension);
                    try (FileOutputStream output = new FileOutputStream(file)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = input.read(buffer)) != -1) {
                            output.write(buffer, 0, bytesRead);
                        }
                    }
                    input.close();

                    shareUri = FileProvider.getUriForFile(
                            requireContext(),
                            requireContext().getPackageName() + ".fileprovider",
                            file);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!isAdded()) return;

            final Uri finalUri = shareUri;
            requireActivity().runOnUiThread(() -> {
                if (finalUri == null) {
                    Toast.makeText(requireContext(), "Failed to prepare image for sharing", Toast.LENGTH_SHORT).show();
                    return;
                }

                String caption = label.isEmpty()
                        ? "Bus Schedule — Shared from IIUC Pedia"
                        : "Bus Schedule (" + label + ") — Shared from IIUC Pedia";
                if (appUpdateLink != null && !appUpdateLink.isEmpty()) {
                    caption += "\n\nDownload IIUC Pedia from - " + appUpdateLink;
                }

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/*");
                shareIntent.putExtra(Intent.EXTRA_STREAM, finalUri);
                shareIntent.putExtra(Intent.EXTRA_TEXT, caption);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Shared from IIUC Pedia");
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_chooser_title)));
            });
        }).start();
    }

    private void checkPermissionAndDownload(String fileUrl) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE);
        } else {
            downloadFile(fileUrl);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingDownloadUrl != null) {
                    downloadFile(pendingDownloadUrl);
                }
            } else {
                Toast.makeText(requireContext(), "Permission denied. Cannot download file.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void downloadFile(String urlString) {
        Toast.makeText(requireContext(), "Starting download...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            boolean success = false;
            String savedFilePath = null;
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    success = false;
                } else {
                    InputStream input = connection.getInputStream();
                    String extension = urlString.contains(".") ? urlString.substring(urlString.lastIndexOf(".")) : ".jpg";
                    String fileName = "bus_schedule_" + System.currentTimeMillis() + extension;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ContentValues values = new ContentValues();
                        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                        values.put(MediaStore.Downloads.MIME_TYPE, extension.equalsIgnoreCase(".pdf") ? "application/pdf" : "image/jpeg");
                        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/IIUC Pedia/Bus Schedule");

                        Uri uri = requireContext().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                        if (uri != null) {
                            try (OutputStream output = requireContext().getContentResolver().openOutputStream(uri)) {
                                byte[] buffer = new byte[4096];
                                int bytesRead;
                                while ((bytesRead = input.read(buffer)) != -1) {
                                    output.write(buffer, 0, bytesRead);
                                }
                            }
                            savedFilePath = "Downloads/IIUC Pedia/Bus Schedule/" + fileName;
                            success = true;
                        }
                    } else {
                        File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "IIUC Pedia/Bus Schedule");
                        if (!folder.exists()) folder.mkdirs();
                        File file = new File(folder, fileName);
                        try (FileOutputStream output = new FileOutputStream(file)) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = input.read(buffer)) != -1) {
                                output.write(buffer, 0, bytesRead);
                            }
                        }
                        savedFilePath = file.getAbsolutePath();
                        success = true;
                    }
                    input.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                success = false;
            }

            final boolean finalSuccess = success;
            final String finalPath = savedFilePath;
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    if (finalSuccess) {
                        Toast.makeText(requireContext(), "Downloaded to: " + finalPath, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(requireContext(), "Failed to download bus schedule.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Best-effort cleanup of any cached images we created for sharing.
        try {
            File cacheDir = requireContext().getCacheDir();
            File[] files = cacheDir.listFiles((dir, name) -> name.startsWith("share_bus_"));
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
        } catch (Exception ignored) {
        }
    }
}
