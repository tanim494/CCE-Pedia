package com.tanim.ccepedia;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ADMConfigFragment extends Fragment {

    private EditText editVersion, editUpdateLink;
    private Button btnSaveAppConfig;

    private FirebaseFirestore firestore;
    private DocumentReference configRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_adm_config, container, false);

        editVersion = view.findViewById(R.id.editVersion);
        editUpdateLink = view.findViewById(R.id.editUpdateLink);
        btnSaveAppConfig = view.findViewById(R.id.btnSaveAppConfig);

        firestore = FirebaseFirestore.getInstance();
        configRef = firestore.collection("appConfig").document("main");

        loadExistingConfig();

        btnSaveAppConfig.setOnClickListener(v -> saveConfig());

        return view;
    }

    private void loadExistingConfig() {
        configRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                if (doc.contains("version"))
                    editVersion.setText(String.valueOf(doc.getLong("version")));

                if (doc.contains("updateLink"))
                    editUpdateLink.setText(doc.getString("updateLink"));
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

        int versionCode = Integer.parseInt(versionStr);

        Map<String, Object> configMap = new HashMap<>();
        configMap.put("version", versionCode);
        configMap.put("updateLink", updateLink);

        configRef.set(configMap)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Config saved.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to save config.", Toast.LENGTH_SHORT).show());
    }
}
