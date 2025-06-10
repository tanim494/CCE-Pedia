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

public class ADMBusFragment extends Fragment {

    private EditText editBusTitle, editBusUrl;
    private Button btnSaveBusSchedule;

    private FirebaseFirestore firestore;
    private DocumentReference busScheduleDocRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_adm_bus, container, false);

        editBusTitle = view.findViewById(R.id.editBusTitle);
        editBusUrl = view.findViewById(R.id.editBusUrl);
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

                        editBusTitle.setText(title != null ? title : "");
                        editBusUrl.setText(url != null ? url : "");
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to load bus schedule.", Toast.LENGTH_SHORT).show());
    }

    private void saveBusSchedule() {
        String title = editBusTitle.getText().toString().trim();
        String url = editBusUrl.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            editBusTitle.setError("Title required");
            return;
        }
        if (TextUtils.isEmpty(url)) {
            editBusUrl.setError("URL required");
            return;
        }

        busScheduleDocRef.update("title", title, "url", url)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(getContext(), "Bus schedule updated.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> {
                    // If doc doesn't exist, create it
                    busScheduleDocRef.set(new ManageBusSchedule(title, url))
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(getContext(), "Bus schedule saved.", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e2 ->
                                    Toast.makeText(getContext(), "Failed to save bus schedule.", Toast.LENGTH_SHORT).show());
                });
    }
}
