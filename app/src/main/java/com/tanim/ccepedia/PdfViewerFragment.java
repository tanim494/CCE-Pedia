package com.tanim.ccepedia;

import android.Manifest;
import android.content.ContentValues;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.barteksc.pdfviewer.PDFView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class PdfViewerFragment extends Fragment {
    private static final String ARG_URL = "fileUrl";
    private static final String ARG_FILENAME = "fileName";
    private static final int REQUEST_WRITE_PERMISSION = 1001;

    private String fileUrl, fileName;
    private PDFView pdfView;

    public static PdfViewerFragment newInstance(String url, String fileName) {
        PdfViewerFragment fragment = new PdfViewerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_URL, url);
        args.putString(ARG_FILENAME, fileName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pdf_viewer, container, false);

        pdfView = view.findViewById(R.id.pdfView);
        Button downloadButton = view.findViewById(R.id.downloadButton);

        if (getArguments() != null) {
            fileUrl = getArguments().getString(ARG_URL);
            fileName = getArguments().getString(ARG_FILENAME);
            downloadAndDisplayPdf(fileUrl);
        }

        downloadButton.setOnClickListener(v -> {
            if (fileUrl != null && !fileUrl.isEmpty()) {
                checkPermissionAndDownload(fileUrl);
            } else {
                Toast.makeText(requireContext(), "No PDF URL found", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void downloadAndDisplayPdf(String urlString) {
        new Thread(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream input = connection.getInputStream();
                File file = File.createTempFile("temp_pdf", ".pdf", requireContext().getCacheDir());
                FileOutputStream output = new FileOutputStream(file);

                byte[] buffer = new byte[1024];
                int byteCount;
                while ((byteCount = input.read(buffer)) != -1) {
                    output.write(buffer, 0, byteCount);
                }

                output.close();
                input.close();

                requireActivity().runOnUiThread(() -> pdfView.fromFile(file)
                        .enableSwipe(true)
                        .swipeHorizontal(false)
                        .enableDoubletap(true)
                        .load());

            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Failed to load PDF: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void checkPermissionAndDownload(String url) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_PERMISSION);
        } else {
            new DownloadPdfTask().execute(url);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_WRITE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (fileUrl != null && !fileUrl.isEmpty()) {
                    new DownloadPdfTask().execute(fileUrl);
                }
            } else {
                Toast.makeText(requireContext(), "Permission denied. Cannot download PDF.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class DownloadPdfTask extends AsyncTask<String, Void, Boolean> {
        private String savedFilePath = null;

        @Override
        protected Boolean doInBackground(String... urls) {
            String urlToDownload = urls[0];
            try {
                URL url = new URL(urlToDownload);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream input = connection.getInputStream();

                String extension = ".pdf";  // PDF extension fixed
                String originalFileName = PdfViewerFragment.this.fileName;
                String fileNameToSave;

                if (originalFileName == null || originalFileName.trim().isEmpty()) {
                    fileNameToSave = "CCE_Pedia_" + System.currentTimeMillis() + extension;
                } else if (!originalFileName.toLowerCase().endsWith(".pdf")) {
                    fileNameToSave = "CCE_Pedia_" + originalFileName + extension;
                } else {
                    fileNameToSave = "CCE_Pedia_" + originalFileName;
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Downloads.DISPLAY_NAME, fileNameToSave);
                    values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                    values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/CCE Pedia/Resources");

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
                        savedFilePath = Environment.DIRECTORY_DOWNLOADS + "/CCE Pedia/Resources/" + fileNameToSave;
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    File downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    File ccePediaFolder = new File(downloadsFolder, "CCE Pedia/Resources");
                    if (!ccePediaFolder.exists()) ccePediaFolder.mkdirs();

                    File file = new File(ccePediaFolder, fileNameToSave);
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
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(requireContext(), "Downloaded to: " + savedFilePath, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(requireContext(), "Failed to download PDF", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
