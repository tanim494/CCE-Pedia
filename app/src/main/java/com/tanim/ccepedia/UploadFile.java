package com.tanim.ccepedia;

import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.provider.OpenableColumns;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UploadFile extends Fragment {

    private Button btnPickFile, btnUpload;
    private EditText etFileName;
    private Spinner spinnerSemester, spinnerCourse;
    private ProgressBar progressBar;
    private TextView thankText;
    private Uri selectedFileUri = null;

    private String[] semesters = {"1", "2", "3", "4", "5", "6", "7", "8"};

    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private ActivityResultLauncher<String> filePickerLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_upload_file, container, false);

        btnPickFile = view.findViewById(R.id.btnPickFile);
        btnUpload = view.findViewById(R.id.btnUpload);
        etFileName = view.findViewById(R.id.etFileName);
        spinnerSemester = view.findViewById(R.id.spinnerSemester);
        spinnerCourse = view.findViewById(R.id.spinnerCourse);
        progressBar = view.findViewById(R.id.progressBar);
        thankText = view.findViewById(R.id.thankText);

        // Initialize Firebase instances
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        btnUpload.setEnabled(false); // disable upload until file is picked

        setupSemesterSpinner();
        setupFilePicker();
        setupListeners();
        setupThank();

        return view;
    }

    private void setupThank() {
        UserData user = UserData.getInstance();
        String name = user.getName();
        String id = user.getId();

        String message = "Thank you, " + name + " (ID: " + id + ")" +
                " for your valuable contribution to CCE Pedia. 🙏";

        SpannableString spannable = new SpannableString(message);

        // Make name bold and colored
        int startName = message.indexOf(name);
        int endName = startName + name.length();
        spannable.setSpan(new StyleSpan(Typeface.BOLD), startName, endName, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.orange)), startName, endName, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Make ID bold
        int startID = message.indexOf(id);
        int endID = startID + id.length();
        spannable.setSpan(new StyleSpan(Typeface.BOLD), startID, endID, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        thankText.setText(spannable);
    }

    private void setupSemesterSpinner() {
        ArrayAdapter<String> semesterAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, semesters);
        semesterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSemester.setAdapter(semesterAdapter);

        spinnerCourse.setEnabled(false);

        spinnerSemester.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedSemester = "semester_" + semesters[position];
                spinnerCourse.setEnabled(false);
                spinnerCourse.setAdapter(null);

                // Fetch courses from Firestore under semesters/{semester}/courses collection
                db.collection("semesters")
                        .document(selectedSemester)
                        .collection("courses")
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                List<String> courseList = new ArrayList<>();
                                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                    courseList.add(doc.getId());
                                }
                                ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(requireContext(),
                                        android.R.layout.simple_spinner_item, courseList);
                                courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                spinnerCourse.setAdapter(courseAdapter);
                                spinnerCourse.setEnabled(true);
                            } else {
                                Toast.makeText(requireContext(), "No courses found for " + semesters[position], Toast.LENGTH_SHORT).show();
                                spinnerCourse.setEnabled(false);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(requireContext(), "Failed to load courses: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            spinnerCourse.setEnabled(false);
                        });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                spinnerCourse.setEnabled(false);
            }
        });
    }

    private void setupFilePicker() {
        filePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                selectedFileUri = uri;
                String fileName = getFileName(uri);
                etFileName.setText(fileName != null ? fileName : "");
                btnUpload.setEnabled(true);
            }
        });

        // Restrict to PDFs only
        btnPickFile.setOnClickListener(v -> filePickerLauncher.launch("application/pdf"));
    }

    private void setupListeners() {
        btnUpload.setOnClickListener(v -> {
            if (selectedFileUri == null) {
                Toast.makeText(requireContext(), "Please select a file first", Toast.LENGTH_SHORT).show();
                return;
            }
            String fileName = etFileName.getText().toString().trim();
            if (fileName.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a file name", Toast.LENGTH_SHORT).show();
                return;
            }

            String semester = spinnerSemester.getSelectedItem() != null ? spinnerSemester.getSelectedItem().toString() : null;
            String course = spinnerCourse.getSelectedItem() != null ? spinnerCourse.getSelectedItem().toString() : null;

            if (semester == null || semester.isEmpty()) {
                Toast.makeText(requireContext(), "Please select a semester", Toast.LENGTH_SHORT).show();
                return;
            }
            if (course == null || course.isEmpty()) {
                Toast.makeText(requireContext(), "Please select a course", Toast.LENGTH_SHORT).show();
                return;
            }

            // Confirmation dialog
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Confirm Upload")
                    .setMessage("Upload file:\n" + fileName + "\nSemester: " + semester + "\nCourse: " + course + "\nProceed?")
                    .setPositiveButton("Yes", (dialog, which) -> uploadFile(selectedFileUri, fileName, "semester_" + semester, course))
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    private void uploadFile(Uri fileUri, String fileName, String semester, String course) {
        progressBar.setVisibility(View.VISIBLE);
        btnUpload.setEnabled(false);

        StorageReference storageRef = storage.getReference();

        // Path in Firebase Storage: semesters/semester_1/CourseA/filename.ext
        String filePath = semester + "/" + course + "/" + fileName;

        StorageReference fileRef = storageRef.child(filePath);

        fileRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            String downloadUrl = uri.toString();
                            saveFileMetadata(fileName, downloadUrl, semester, course);
                        }))
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    btnUpload.setEnabled(true);
                });
    }

    private void saveFileMetadata(String fileName, String downloadUrl, String semester, String course) {
        Map<String, Object> fileData = new HashMap<>();
        fileData.put("fileName", fileName);
        fileData.put("url", downloadUrl);
        fileData.put("uploadedAt", System.currentTimeMillis());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            fileData.put("uploadedBy", user.getEmail());
        }

        db.collection("semesters")
                .document(semester)
                .collection("courses")
                .document(course)
                .collection("files")
                .add(fileData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(requireContext(), "File uploaded successfully", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    btnUpload.setEnabled(true);
                    etFileName.setText("");
                    selectedFileUri = null;
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to save file metadata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    btnUpload.setEnabled(true);
                });
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = requireContext().getContentResolver()
                    .query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }
}
