package com.tanim.ccepedia;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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

public class AdminBusFragment extends Fragment {

    private EditText editBusTitle, editBusUrl, editBusFridayUrl;
    private MaterialButton btnSaveBusSchedule;

    private FirebaseFirestore firestore;
    private DocumentReference busScheduleDocRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_bus, container, false);

        editBusTitle = view.findViewById(R.id.editBusTitle);
        editBusUrl = view.findViewById(R.id.editBusUrl);
        editBusFridayUrl = view.findViewById(R.id.editBusFridayUrl);
        btnSaveBusSchedule = view.findViewById(R.id.btnSaveBusSchedule);

        firestore = FirebaseFirestore.getInstance();
        busScheduleDocRef = firestore.collection("resources").document("bus_schedule");

        loadBusSchedule();

        btnSaveBusSchedule.setOnClickListener(v -> saveBusSchedule());

        return view;
    }

    private void loadBusSchedule() {
        busScheduleDocRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String title = documentSnapshot.getString("title");
                        String url = documentSnapshot.getString("url");
                        String fridayUrl = documentSnapshot.getString("fridayUrl");

                        editBusTitle.setText(title != null ? title : "");
                        editBusUrl.setText(url != null ? url : "");
                        editBusFridayUrl.setText(fridayUrl != null ? fridayUrl : "");
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to load bus schedule.", Toast.LENGTH_SHORT).show());
    }

    private void saveBusSchedule() {
        String title = editBusTitle.getText().toString().trim();
        String url = editBusUrl.getText().toString().trim();
        String fridayUrl = editBusFridayUrl.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            editBusTitle.setError("Title required");
            return;
        }
        if (TextUtils.isEmpty(url)) {
            editBusUrl.setError("URL required");
            return;
        }

        // Merge-write so the existing `contacts` array (managed elsewhere) is preserved.
        // fridayUrl is optional: an empty value clears the second page on the user screen.
        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("url", url);
        data.put("fridayUrl", fridayUrl);

        busScheduleDocRef.set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(getContext(), "Bus schedule updated.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to save bus schedule.", Toast.LENGTH_SHORT).show());
    }
}
