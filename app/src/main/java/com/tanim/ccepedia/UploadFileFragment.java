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
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UploadFileFragment extends Fragment {

    private MaterialButton btnPickFile, btnUpload;
    private EditText etFileName;
    private AutoCompleteTextView spinnerSemester, spinnerCourse;
    private ProgressBar progressBar;
    private TextView thankText;
    private Uri selectedFileUri = null;

    private String[] semesters = {"1", "2", "3", "4", "5", "6", "7", "8"};

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String deptCode;

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

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        deptCode = UserData.getInstance().getDepartmentName();

        btnUpload.setEnabled(false);

        setupSemesterSpinner();
        setupFilePicker();
        setupListeners();
        setupThank();

        return view;
    }

    private DocumentReference getDepartmentRootRef() {
        if (deptCode == null || deptCode.isEmpty()) {
            Toast.makeText(getContext(), "Department not set for upload.", Toast.LENGTH_SHORT).show();
            return null;
        }

        if (deptCode.equalsIgnoreCase("CCE")) {
            return db.collection("semesters").document("DUMMY");
        } else {
            String deptDocumentId = "dept_" + deptCode.toLowerCase();
            return db.collection("departments").document(deptDocumentId);
        }
    }

    private void setupThank() {
        UserData user = UserData.getInstance();
        String name = user.getName();
        String studentId = user.getStudentId();

        String message = "Thank you, " + name + " (ID: " + studentId + ")" +
                " for your valuable contribution to IIUC Pedia. 🙏";

        SpannableString spannable = new SpannableString(message);

        int startName = message.indexOf(name);
        int endName = startName + name.length();
        spannable.setSpan(new StyleSpan(Typeface.BOLD), startName, endName, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.textPrimary)), startName, endName, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        int startID = message.indexOf(studentId);
        int endID = startID + studentId.length();
        spannable.setSpan(new StyleSpan(Typeface.BOLD), startID, endID, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        thankText.setText(spannable);
    }

    private void setupSemesterSpinner() {
        ArrayAdapter<String> semesterAdapter = new ArrayAdapter<>(requireContext(),
                R.layout.dropdown_item, semesters);
        spinnerSemester.setAdapter(semesterAdapter);

        spinnerCourse.setEnabled(false);

        final DocumentReference deptRootRef = getDepartmentRootRef();
        if (deptRootRef == null) return;

        spinnerSemester.setOnItemClickListener((parent, view, position, id) -> {
            String selectedSemesterId = "semester_" + semesters[position];
            spinnerCourse.setEnabled(false);
            spinnerCourse.setText("");
            spinnerCourse.setAdapter(null);
            checkUploadReadiness();

            CollectionReference coursesRef;

            if (deptCode.equalsIgnoreCase("CCE")) {
                coursesRef = deptRootRef.getFirestore().collection("semesters")
                        .document(selectedSemesterId)
                        .collection("courses");
            } else {
                coursesRef = deptRootRef.collection("semesters")
                        .document(selectedSemesterId)
                        .collection("courses");
            }

            coursesRef
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!isAdded()) return;
                        List<String> courseList = new ArrayList<>();
                        if (!queryDocumentSnapshots.isEmpty()) {
                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                courseList.add(doc.getId());
                            }
                            ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(requireContext(),
                                    R.layout.dropdown_item, courseList);
                            spinnerCourse.setAdapter(courseAdapter);
                            spinnerCourse.setEnabled(true);
                        } else {
                            Toast.makeText(requireContext(), "No courses found for semester " + semesters[position], Toast.LENGTH_SHORT).show();
                        }
                        checkUploadReadiness();
                    })
                    .addOnFailureListener(e -> {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Failed to load courses: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        spinnerCourse.setEnabled(false);
                        checkUploadReadiness();
                    });
        });

        spinnerCourse.setOnItemClickListener((parent, view, position, id) -> checkUploadReadiness());
    }

    private void setupFilePicker() {
        filePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null && isAdded()) {
                selectedFileUri = uri;
                String fileName = getFileName(uri);
                etFileName.setText(fileName != null ? fileName : "");
                btnPickFile.setText(fileName != null ? fileName : "File Selected");
                btnPickFile.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_right));
                checkUploadReadiness();
            } else {
                selectedFileUri = null;
                btnPickFile.setText("Choose PDF File");
                btnPickFile.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_pdf));
                checkUploadReadiness();
            }
        });

        btnPickFile.setOnClickListener(v -> filePickerLauncher.launch("application/pdf"));
    }

    private void setupListeners() {
        etFileName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                checkUploadReadiness();
            }
        });

        btnUpload.setOnClickListener(v -> {
            String fileName = etFileName.getText().toString().trim();
            if (fileName.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a file name", Toast.LENGTH_SHORT).show();
                return;
            }

            String semester = spinnerSemester.getText().toString().trim();
            String course = spinnerCourse.getText().toString().trim();

            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Confirm Upload")
                    .setMessage("Upload file:\n" + fileName + "\nSemester: " + semester + "\nCourse: " + course + "\nProceed?")
                    .setPositiveButton("Yes", (dialog, which) -> uploadFile(selectedFileUri, fileName, "semester_" + semester, course))
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    private void checkUploadReadiness() {
        boolean fileSelected = selectedFileUri != null;
        boolean fileNameValid = !etFileName.getText().toString().trim().isEmpty();
        boolean semesterSelected = !spinnerSemester.getText().toString().trim().isEmpty();
        boolean courseSelected = !spinnerCourse.getText().toString().trim().isEmpty() && spinnerCourse.isEnabled();

        btnUpload.setEnabled(fileSelected && fileNameValid && semesterSelected && courseSelected);
    }

    private void uploadFile(Uri fileUri, String fileName, String semesterId, String courseId) {
        progressBar.setVisibility(View.VISIBLE);
        btnUpload.setEnabled(false);

        String storageFileName = fileName;
        String storageDept = UserData.getInstance().getDepartmentName();

        StorageReference storageRef = storage.getReference();
        String filePath = storageDept + "/" + semesterId + "/" + courseId + "/" + storageFileName;
        StorageReference fileRef = storageRef.child(filePath);

        fileRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> saveFileMetadata(fileName, uri.toString(), semesterId, courseId))
                        .addOnFailureListener(e -> showUploadError("Upload failed: " + e.getMessage())))
                .addOnFailureListener(e -> showUploadError("Upload failed: " + e.getMessage()));
    }

    // Reset the form's busy state and report an error, but only while still attached — these fire
    // from async callbacks that may return after the user has navigated away.
    private void showUploadError(String message) {
        if (!isAdded()) return;
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        progressBar.setVisibility(View.GONE);
        btnUpload.setEnabled(true);
    }

    private void saveFileMetadata(String fileName, String downloadUrl, String semesterId, String courseId) {
        Map<String, Object> fileData = new HashMap<>();
        fileData.put("fileName", fileName);
        fileData.put("url", downloadUrl);
        fileData.put("uploadedAt", System.currentTimeMillis());

        UserData user = UserData.getInstance();
        if (user != null) {
            String uploaderNameId = user.getName() + " (ID: " + user.getStudentId() + ")";
            fileData.put("uploadedBy", uploaderNameId);
            fileData.put("uploaderStudentId", user.getStudentId());
        } else {
            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if (firebaseUser != null) {
                fileData.put("uploadedBy", firebaseUser.getEmail());
            }
        }

        CollectionReference filesCollectionRef;
        String currentDeptCode = UserData.getInstance().getDepartmentName();

        if (currentDeptCode.equalsIgnoreCase("CCE")) {
            filesCollectionRef = db.collection("semesters")
                    .document(semesterId)
                    .collection("courses")
                    .document(courseId)
                    .collection("files");
        } else {
            String deptDocumentId = "dept_" + currentDeptCode.toLowerCase();

            filesCollectionRef = db.collection("departments")
                    .document(deptDocumentId)
                    .collection("semesters")
                    .document(semesterId)
                    .collection("courses")
                    .document(courseId)
                    .collection("files");
        }


        filesCollectionRef
                .document(toDocId(fileName))
                .set(fileData)
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded()) return;
                    progressBar.setVisibility(View.GONE);
                    etFileName.setText("");
                    selectedFileUri = null;
                    btnPickFile.setText("Choose PDF File");
                    btnPickFile.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_pdf));
                    checkUploadReadiness();
                    showUploadSuccessDialog(fileName, downloadUrl);
                })
                .addOnFailureListener(e -> showUploadError("Failed to save file metadata: " + e.getMessage()));
    }

    // Key each course's file docs on the (sanitized) file name so re-uploading the same name
    // overwrites its metadata in place instead of adding a duplicate list entry — matching Storage,
    // which already overwrites the object at the same path. Firestore IDs can't contain '/'.
    private String toDocId(String fileName) {
        String id = fileName.trim().replaceAll("[/\\\\]", "_");
        if (id.isEmpty() || id.equals(".") || id.equals("..")) {
            id = "file_" + System.currentTimeMillis();
        }
        return id;
    }

    // Offer to announce the fresh upload in the Community Chat (attributed to the uploader by
    // CommunityShare), or just dismiss.
    private void showUploadSuccessDialog(String fileName, String downloadUrl) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("File uploaded")
                .setMessage("\"" + fileName + "\" was uploaded successfully. Share it to the Community Chat?")
                .setPositiveButton("Share to Community", (dialog, which) ->
                        CommunityShare.post(requireContext(), CommunityShare.TYPE_PDF, downloadUrl, fileName, "New upload"))
                .setNegativeButton("Dismiss", null)
                .show();
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