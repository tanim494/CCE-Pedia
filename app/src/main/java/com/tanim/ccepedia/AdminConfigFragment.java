package com.tanim.ccepedia;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class AdminConfigFragment extends Fragment {

    private EditText editVersion, editUpdateLink, editApkLink;
    private com.google.android.material.switchmaterial.SwitchMaterial switchForceUpdate;
    private EditText editDevGithub, editDevFacebook, editDevLinkedin;
    private EditText editClubMaleTitle, editClubMaleUrl, editClubFemaleTitle, editClubFemaleUrl;
    private EditText editHomeBannerUrl, editHomeBannerClickUrl;

    private MaterialButton toggleDevLinks, toggleResourceLinks;
    private LinearLayout layoutDevLinks, layoutResourceLinks;

    private MaterialButton btnSaveAppConfig;
    private FirebaseFirestore firestore;
    private DocumentReference configRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_admin_config, container, false);

        editVersion = view.findViewById(R.id.editVersion);
        editUpdateLink = view.findViewById(R.id.editUpdateLink);
        editApkLink = view.findViewById(R.id.editApkLink);
        switchForceUpdate = view.findViewById(R.id.switchForceUpdate);

        editHomeBannerUrl = view.findViewById(R.id.editHomeBannerUrl);
        editHomeBannerClickUrl = view.findViewById(R.id.editHomeBannerClickUrl);

        editDevGithub = view.findViewById(R.id.editDevGithub);
        editDevFacebook = view.findViewById(R.id.editDevFacebook);
        editDevLinkedin = view.findViewById(R.id.editDevLinkedin);

        editClubMaleTitle = view.findViewById(R.id.editClubMaleTitle);
        editClubMaleUrl = view.findViewById(R.id.editClubMaleUrl);
        editClubFemaleTitle = view.findViewById(R.id.editClubFemaleTitle);
        editClubFemaleUrl = view.findViewById(R.id.editClubFemaleUrl);

        btnSaveAppConfig = view.findViewById(R.id.btnSaveAppConfig);

        toggleDevLinks = view.findViewById(R.id.toggleDevLinks);
        layoutDevLinks = view.findViewById(R.id.layoutDevLinks);
        toggleResourceLinks = view.findViewById(R.id.toggleResourceLinks);
        layoutResourceLinks = view.findViewById(R.id.layoutResourceLinks);

        firestore = FirebaseFirestore.getInstance();
        configRef = firestore.collection("appConfig").document("main");

        setupToggleListeners();
        loadExistingConfig();

        btnSaveAppConfig.setOnClickListener(v -> saveConfig());

        return view;
    }

    private void setupToggleListeners() {
        toggleDevLinks.setOnClickListener(v -> {
            if (layoutDevLinks.getVisibility() == View.GONE) {
                layoutDevLinks.setVisibility(View.VISIBLE);
                toggleDevLinks.setText("Developer Links (Click to Collapse)");
            } else {
                layoutDevLinks.setVisibility(View.GONE);
                toggleDevLinks.setText("Developer Links (Click to Expand)");
            }
        });

        toggleResourceLinks.setOnClickListener(v -> {
            if (layoutResourceLinks.getVisibility() == View.GONE) {
                layoutResourceLinks.setVisibility(View.VISIBLE);
                toggleResourceLinks.setText("Resource Links (Click to Collapse)");
            } else {
                layoutResourceLinks.setVisibility(View.GONE);
                toggleResourceLinks.setText("Resource Links (Click to Expand)");
            }
        });
    }

    private void loadExistingConfig() {
        configRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                if (doc.contains("versionCode")) {
                    Long versionCode = doc.getLong("versionCode");
                    if (versionCode != null) {
                        editVersion.setText(String.valueOf(versionCode.intValue()));
                    }
                }

                Boolean force = doc.getBoolean("forceUpdate");
                switchForceUpdate.setChecked(force != null && force);

                if (doc.contains("updateLink"))
                    editUpdateLink.setText(doc.getString("updateLink"));

                if (doc.contains("apkLink"))
                    editApkLink.setText(doc.getString("apkLink"));

                if (doc.contains("homeBannerUrl"))
                    editHomeBannerUrl.setText(doc.getString("homeBannerUrl"));

                if (doc.contains("homeBannerClickUrl"))
                    editHomeBannerClickUrl.setText(doc.getString("homeBannerClickUrl"));

                if (doc.contains("dev_github"))
                    editDevGithub.setText(doc.getString("dev_github"));

                if (doc.contains("dev_facebook"))
                    editDevFacebook.setText(doc.getString("dev_facebook"));

                if (doc.contains("dev_linkedin"))
                    editDevLinkedin.setText(doc.getString("dev_linkedin"));

                Map<String, String> clubMale = (Map<String, String>) doc.get("club_link_male");
                if (clubMale != null) {
                    editClubMaleTitle.setText(clubMale.get("title"));
                    editClubMaleUrl.setText(clubMale.get("url"));
                }

                Map<String, String> clubFemale = (Map<String, String>) doc.get("club_link_female");
                if (clubFemale != null) {
                    editClubFemaleTitle.setText(clubFemale.get("title"));
                    editClubFemaleUrl.setText(clubFemale.get("url"));
                }
            }
        });
    }

    private void saveConfig() {
        String versionStr = editVersion.getText().toString().trim();
        String updateLink = editUpdateLink.getText().toString().trim();

        if (TextUtils.isEmpty(versionStr)) {
            editVersion.setError("Version required");
            return;
        }

        if (TextUtils.isEmpty(updateLink)) {
            editUpdateLink.setError("Update link required");
            return;
        }

        int versionCode;
        try {
            versionCode = Integer.parseInt(versionStr);
        } catch (NumberFormatException e) {
            editVersion.setError("Invalid number");
            return;
        }

        Map<String, Object> configMap = new HashMap<>();
        configMap.put("versionCode", versionCode);
        configMap.put("forceUpdate", switchForceUpdate.isChecked());
        configMap.put("updateLink", updateLink);
        configMap.put("apkLink", editApkLink.getText().toString().trim());

        configMap.put("homeBannerUrl", editHomeBannerUrl.getText().toString().trim());
        configMap.put("homeBannerClickUrl", editHomeBannerClickUrl.getText().toString().trim());

        configMap.put("dev_github", editDevGithub.getText().toString().trim());
        configMap.put("dev_facebook", editDevFacebook.getText().toString().trim());
        configMap.put("dev_linkedin", editDevLinkedin.getText().toString().trim());

        Map<String, Object> clubMaleMap = new HashMap<>();
        clubMaleMap.put("title", editClubMaleTitle.getText().toString().trim());
        clubMaleMap.put("url", editClubMaleUrl.getText().toString().trim());
        configMap.put("club_link_male", clubMaleMap);

        Map<String, Object> clubFemaleMap = new HashMap<>();
        clubFemaleMap.put("title", editClubFemaleTitle.getText().toString().trim());
        clubFemaleMap.put("url", editClubFemaleUrl.getText().toString().trim());
        configMap.put("club_link_female", clubFemaleMap);

        // Merge (not update): creates appConfig/main if it doesn't exist yet and preserves any
        // fields not written here, so the admin never has to seed the document by hand.
        configRef.set(configMap, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) Toast.makeText(getContext(), "Config updated.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) Toast.makeText(getContext(), "Failed to update config.", Toast.LENGTH_SHORT).show();
                });
    }
}