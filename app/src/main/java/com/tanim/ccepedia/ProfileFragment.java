package com.tanim.ccepedia;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ProfileFragment extends Fragment {

    private TextView nameText, idText, emailText, phoneText, semesterText, viewCountText, departmentText;
    private EditText nameEdit, idEdit, phoneEdit, profileLinkEdit;
    private AutoCompleteTextView semesterEdit, departmentEdit;
    private Button editButton, logoutButton, saveButton, btnChangeProfileImage;
    private View academicInfoCard, contactInfoCard, statsCard, editModeLayout, displayPrefsCard, routinePrefContent, profileLinkInputLayout, profileImageUploadLayout, profileHeaderCard;
    private ImageView profileImage, profileImagePreview;
    private SwitchMaterial switchShowDate, switchShowSalat, switchShowRoutine;

    private FirebaseStorage storage;
    private StorageReference storageRef;
    private Uri selectedImageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DepartmentRepository departmentRepository;
    private List<String> departmentDisplayList = new ArrayList<>();


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        nameText = view.findViewById(R.id.nameText);
        idText = view.findViewById(R.id.idText);
        emailText = view.findViewById(R.id.emailText);
        phoneText = view.findViewById(R.id.phoneText);
        semesterText = view.findViewById(R.id.semesterText);
        viewCountText = view.findViewById(R.id.viewCountText);
        departmentText = view.findViewById(R.id.departmentText);

        nameEdit = view.findViewById(R.id.nameEdit);
        idEdit = view.findViewById(R.id.idEdit);
        phoneEdit = view.findViewById(R.id.phoneEdit);
        semesterEdit = view.findViewById(R.id.semesterEdit);
        departmentEdit = view.findViewById(R.id.departmentEdit);
        profileLinkEdit = view.findViewById(R.id.profileLinkEdit);

        editButton = view.findViewById(R.id.editButton);
        logoutButton = view.findViewById(R.id.logoutButton);
        saveButton = view.findViewById(R.id.saveButton);
        btnChangeProfileImage = view.findViewById(R.id.btnChangeProfileImage);

        academicInfoCard = view.findViewById(R.id.academicInfoCard);
        contactInfoCard = view.findViewById(R.id.contactInfoCard);
        statsCard = view.findViewById(R.id.statsCard);
        editModeLayout = view.findViewById(R.id.editModeLayout);
        displayPrefsCard = view.findViewById(R.id.displayPrefsCard);
        routinePrefContent = view.findViewById(R.id.routinePrefContent);
        profileLinkInputLayout = view.findViewById(R.id.profileLinkInputLayout);
        profileImageUploadLayout = view.findViewById(R.id.profileImageUploadLayout);
        profileImagePreview = view.findViewById(R.id.profileImagePreview);
        profileImage = view.findViewById(R.id.profileImage);
        profileHeaderCard = view.findViewById(R.id.profileHeaderCard);
        switchShowDate = view.findViewById(R.id.switchShowDate);
        switchShowSalat = view.findViewById(R.id.switchShowSalat);
        switchShowRoutine = view.findViewById(R.id.switchShowRoutine);

        editButton.setOnClickListener(v -> switchToEditMode());
        saveButton.setOnClickListener(v -> saveUserData());
        logoutButton.setOnClickListener(v -> logoutUser());
        routinePrefContent.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), RoutineEditorActivity.class)));

        loadDisplayPrefs();
        setupDisplayPrefsListeners();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        departmentRepository = new DepartmentRepository();

        setupImagePicker();
        loadUserData();

        boolean canViewMetrics = UserData.getInstance().getRole() != null &&
                (UserData.getInstance().getRole().equalsIgnoreCase("admin") ||
                        UserData.getInstance().getRole().equalsIgnoreCase("moderator"));

        if (canViewMetrics) {
            statsCard.setVisibility(View.VISIBLE);
            long views = UserData.getInstance().getViewCount();
            viewCountText.setText("Total File Views: " + String.valueOf(views));
        } else {
            statsCard.setVisibility(View.GONE);
        }

        return view;
    }

    private void loadUserData() {
        UserData user = UserData.getInstance();

        nameText.setText(user.getName());
        idText.setText("ID: " + user.getStudentId());
        emailText.setText(user.getEmail());
        phoneText.setText("Phone: " + user.getPhone());
        semesterText.setText("Semester: " + user.getSemester());
        departmentText.setText("Dept: " + user.getDepartmentName());

        nameEdit.setText(user.getName());
        idEdit.setText(user.getStudentId());
        phoneEdit.setText(user.getPhone());
        semesterEdit.setText(user.getSemester());
        departmentEdit.setText(user.getDepartmentName());

        // Load profile image in view mode
        loadProfileImageViewMode();
    }

    private void loadProfileImageViewMode() {
        String photoUrl = UserData.getInstance().getPhotoUrl();
        if (profileImage == null) return;

        if (photoUrl != null && !photoUrl.isEmpty()) {
            // FIX: Clear both color filter and tint (ShapeableImageView uses app:tint)
            profileImage.clearColorFilter();
            profileImage.setImageTintList(null);

            Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_profile)
                    .circleCrop()
                    .into(profileImage);
        } else {
            profileImage.setImageResource(R.drawable.ic_profile);
            profileImage.setColorFilter(ContextCompat.getColor(requireContext(), R.color.textSecondary));
        }
    }



    private void switchToEditMode() {
        academicInfoCard.setVisibility(View.GONE);
        contactInfoCard.setVisibility(View.GONE);
        statsCard.setVisibility(View.GONE);
        displayPrefsCard.setVisibility(View.GONE);
        logoutButton.setVisibility(View.GONE);
        profileHeaderCard.setVisibility(View.GONE);
        editModeLayout.setVisibility(View.VISIBLE);

        String[] SEMESTERS = new String[] {"1", "2", "3", "4", "5", "6", "7", "8", "Outgoing"};
        ArrayAdapter<String> semesterAdapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_item, SEMESTERS);
        semesterEdit.setAdapter(semesterAdapter);
        semesterEdit.setOnClickListener(v -> semesterEdit.showDropDown());

        // Show profile link and image upload only for admin/moderator
        boolean isAdminOrMod = UserData.getInstance().getRole() != null &&
                (UserData.getInstance().getRole().equalsIgnoreCase("admin") ||
                        UserData.getInstance().getRole().equalsIgnoreCase("moderator"));
        if (profileLinkInputLayout != null) {
            profileLinkInputLayout.setVisibility(isAdminOrMod ? View.VISIBLE : View.GONE);
        }
        if (profileImageUploadLayout != null) {
            profileImageUploadLayout.setVisibility(isAdminOrMod ? View.VISIBLE : View.GONE);
        }

        // Load current profile link if exists
        if (isAdminOrMod && profileLinkEdit != null) {
            String profileLink = UserData.getInstance().getProfileLink();
            if (profileLink != null && !profileLink.isEmpty()) {
                profileLinkEdit.setText(profileLink);
            }
        }

        // Load current profile image if exists
        if (isAdminOrMod && profileImagePreview != null) {
            String photoUrl = UserData.getInstance().getPhotoUrl();
            if (photoUrl != null && !photoUrl.isEmpty()) {
                Glide.with(this).load(photoUrl).placeholder(R.drawable.ic_profile).into(profileImagePreview);
            } else {
                profileImagePreview.setImageResource(R.drawable.ic_profile);
            }
        }

        if (btnChangeProfileImage != null) {
            btnChangeProfileImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        }

        departmentRepository.fetchAllDepartmentIds()
                .addOnSuccessListener(ids -> {
                    departmentDisplayList = ids.stream()
                            .map(id -> id.replace("dept_", "").toUpperCase())
                            .collect(Collectors.toList());

                    ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(
                            requireContext(), R.layout.dropdown_item, departmentDisplayList);
                    departmentEdit.setAdapter(deptAdapter);

                    String currentDept = UserData.getInstance().getDepartmentName();
                    if (departmentDisplayList.contains(currentDept)) {
                        departmentEdit.setText(currentDept, false);
                    }

                    departmentEdit.setOnClickListener(v -> departmentEdit.showDropDown());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load departments.", Toast.LENGTH_SHORT).show();
                });


        editButton.setVisibility(View.GONE);
        saveButton.setVisibility(View.VISIBLE);
    }


    private void switchToViewMode() {
        editModeLayout.setVisibility(View.GONE);
        academicInfoCard.setVisibility(View.VISIBLE);
        contactInfoCard.setVisibility(View.VISIBLE);
        displayPrefsCard.setVisibility(View.VISIBLE);
        logoutButton.setVisibility(View.VISIBLE);
        profileHeaderCard.setVisibility(View.VISIBLE);

        loadProfileImageViewMode();

        boolean canViewMetrics = UserData.getInstance().getRole() != null &&
                (UserData.getInstance().getRole().equalsIgnoreCase("admin") ||
                        UserData.getInstance().getRole().equalsIgnoreCase("moderator"));

        if (canViewMetrics) {
            statsCard.setVisibility(View.VISIBLE);
        }

        editButton.setVisibility(View.VISIBLE);
        saveButton.setVisibility(View.GONE);
    }

    private boolean isValidStudentId(String id) {
        if (id == null) return false;
        String idPattern = "^[a-zA-Z]{1,3}[0-9]{5,8}$";
        Pattern pattern = Pattern.compile(idPattern);
        Matcher matcher = pattern.matcher(id);
        return matcher.matches();
    }

    private void saveUserData() {
        String newName = nameEdit.getText().toString().trim();
        String rawId = idEdit.getText().toString().trim();
        String newPhone = phoneEdit.getText().toString().trim();
        String newSemester = semesterEdit.getText().toString().trim();
        String newDepartmentCode = departmentEdit.getText().toString().trim();
        String newProfileLink = profileLinkEdit != null ? profileLinkEdit.getText().toString().trim() : "";

        if (newName.isEmpty() || rawId.isEmpty() || newSemester.isEmpty() || newDepartmentCode.isEmpty()) {
            showAlert("Please fill all required fields");
            return;
        }

        String sanitizedId = rawId.replaceAll("[^a-zA-Z0-9]", "");
        if (!isValidStudentId(sanitizedId)) {
            showAlert("Invalid Student ID. Insert your correct student ID (e.g., E221013 or C221013).");
            return;
        }

        String finalId = sanitizedId.toUpperCase();

        if (!departmentDisplayList.contains(newDepartmentCode)) {
            showAlert("Please select a valid department from the list.");
            return;
        }

        // Normalize profile link if provided
        if (!newProfileLink.isEmpty() && !newProfileLink.startsWith("http://") && !newProfileLink.startsWith("https://")) {
            newProfileLink = "https://" + newProfileLink;
        }

        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid)
                .update("name", newName,
                        "id", finalId,
                        "phone", newPhone,
                        "semester", newSemester,
                        "department", newDepartmentCode,
                        "profileLink", newProfileLink)
                .addOnSuccessListener(aVoid -> {
                    UserData user = UserData.getInstance();
                    user.setName(newName);
                    user.setStudentId(finalId);
                    user.setPhone(newPhone);
                    user.setSemester(newSemester);
                    user.setDepartmentName(newDepartmentCode);

                    loadUserData();
                    switchToViewMode();
                    Toast.makeText(getContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void logoutUser() {
        mAuth.signOut();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    private void setupImagePicker() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        uploadProfileImage(uri);
                    }
                });
    }

    private void uploadProfileImage(Uri imageUri) {
        if (getContext() == null || mAuth.getCurrentUser() == null) return;

        Toast.makeText(getContext(), "Uploading image...", Toast.LENGTH_SHORT).show();

        // Compress image to max 500KB
        new Thread(() -> {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int quality = 90;
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);

                // Reduce quality until under 500KB
                while (baos.size() > 500 * 1024 && quality > 10) {
                    baos.reset();
                    quality -= 10;
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
                }

                // If still too large, resize bitmap
                if (baos.size() > 500 * 1024) {
                    int maxDimension = 1024;
                    int width = bitmap.getWidth();
                    int height = bitmap.getHeight();
                    float scale = Math.min((float) maxDimension / width, (float) maxDimension / height);
                    if (scale < 1) {
                        Bitmap resized = Bitmap.createScaledBitmap(bitmap,
                                (int) (width * scale), (int) (height * scale), true);
                        baos.reset();
                        resized.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                        bitmap = resized;
                    }
                }

                byte[] imageData = baos.toByteArray();
                String uid = mAuth.getCurrentUser().getUid();
                StorageReference imageRef = storageRef.child("profile_pics/" + uid + ".jpg");

                UploadTask uploadTask = imageRef.putBytes(imageData);
                uploadTask.addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();
                        // Save to Firestore
                        db.collection("users").document(uid)
                                .update("photoUrl", downloadUrl)
                                .addOnSuccessListener(aVoid -> {
                                    UserData.getInstance().setPhotoUrl(downloadUrl);
                                    if (profileImagePreview != null) {
                                        Glide.with(ProfileFragment.this).load(downloadUrl).into(profileImagePreview);
                                    }
                                    // Refresh topbar in MainActivity
                                    if (getActivity() instanceof MainActivity) {
                                        ((MainActivity) getActivity()).loadProfileImage();
                                    }
                                    Toast.makeText(getContext(), "Profile picture updated", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    });
                }).addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

            } catch (IOException e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Failed to process image", Toast.LENGTH_SHORT).show());
                }
            }
        }).start();
    }

    private void showAlert(String message) {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle("Alert!")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void loadDisplayPrefs() {
        if (getContext() == null) return;
        boolean showDate = DisplayPrefs.isShowDate(getContext());
        boolean showSalat = DisplayPrefs.isShowSalat(getContext());
        boolean showRoutine = DisplayPrefs.isShowRoutine(getContext());

        switchShowDate.setChecked(showDate);
        switchShowSalat.setChecked(showSalat);
        switchShowRoutine.setChecked(showRoutine);
    }

    private void setupDisplayPrefsListeners() {
        if (getContext() == null) return;

        switchShowDate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            DisplayPrefs.setShowDate(getContext(), isChecked);
        });

        switchShowSalat.setOnCheckedChangeListener((buttonView, isChecked) -> {
            DisplayPrefs.setShowSalat(getContext(), isChecked);
        });

        switchShowRoutine.setOnCheckedChangeListener((buttonView, isChecked) -> {
            DisplayPrefs.setShowRoutine(getContext(), isChecked);
            // Pre-class reminders follow the routine toggle: scheduled when on, cancelled when off.
            ClassReminderScheduler.sync(getContext());
        });
    }
}