package com.tanim.ccepedia;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lets a student build their personal class routine. Entries persist to {@code users/{uid}.routine}
 * (so they survive logout/reinstall) and are mirrored to a local cache for instant/offline paint on
 * Home. There's no Save button: every add/edit/delete immediately writes the whole array to
 * Firestore and the cache, matching the app's other admin editors.
 */
public class RoutineEditorActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String uid;

    private RecyclerView recyclerView;
    private TextView emptyView;
    private RoutineAdapter adapter;
    private final List<RoutineEntry> entries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routine_editor);

        MaterialToolbar toolbar = findViewById(R.id.routineToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.rvRoutine);
        emptyView = findViewById(R.id.tvRoutineEmpty);
        findViewById(R.id.fabAddRoutine).setOnClickListener(v -> showEditor(null, -1));

        db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        adapter = new RoutineAdapter(entries, new RoutineAdapter.Listener() {
            @Override
            public void onEdit(int index) {
                if (index >= 0 && index < entries.size()) showEditor(entries.get(index), index);
            }

            @Override
            public void onDelete(int index) {
                confirmDelete(index);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadRoutine();
    }

    /** Paints from cache instantly, then refreshes from Firestore (falling back to cache offline). */
    private void loadRoutine() {
        List<RoutineEntry> cached = RoutineStore.loadCache(this);
        entries.clear();
        entries.addAll(cached);
        adapter.setItems(entries);
        updateEmptyState();

        if (uid == null) return;
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (isFinishing() || isDestroyed()) return;
                    entries.clear();
                    entries.addAll(RoutineStore.fromFirestore(doc.get("routine")));
                    RoutineStore.sort(entries);
                    adapter.setItems(entries);
                    updateEmptyState();
                    RoutineStore.saveCache(this, entries);
                    // A routine edited on another device just synced down — re-arm reminders.
                    ClassReminderScheduler.sync(this);
                })
                .addOnFailureListener(e -> {
                    // Keep the cached list; the editor stays usable offline.
                });
    }

    /** Shared add/edit dialog. {@code existing == null} → add; otherwise edit the row at {@code index}. */
    private void showEditor(final RoutineEntry existing, final int index) {
        final boolean editing = existing != null;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_routine, null);
        MaterialAutoCompleteTextView spinnerDay = dialogView.findViewById(R.id.spinnerDay);
        TextInputEditText etStartTime = dialogView.findViewById(R.id.etStartTime);
        TextInputEditText etEndTime = dialogView.findViewById(R.id.etEndTime);
        TextInputEditText etCourse = dialogView.findViewById(R.id.etCourse);

        spinnerDay.setAdapter(new ArrayAdapter<>(this, R.layout.dropdown_item, RoutineStore.DAY_NAMES));

        // Hold the picked start/end as 24h "HH:mm"; the fields themselves show the 12h form.
        final String[] time = {editing ? existing.getStart() : null};
        final String[] end = {editing && existing.getEnd() != null && !existing.getEnd().isEmpty()
                ? existing.getEnd() : null};

        if (editing) {
            spinnerDay.setText(RoutineStore.dayName(existing.getDay()), false);
            etStartTime.setText(RoutineStore.format12(existing.getStart()));
            if (end[0] != null) etEndTime.setText(RoutineStore.format12(end[0]));
            etCourse.setText(existing.getCourse());
        }

        etStartTime.setOnClickListener(v -> {
            int hour = 9, minute = 0; // sensible default for a first class
            int current = RoutineStore.startMinutes(time[0]);
            if (current >= 0) {
                hour = current / 60;
                minute = current % 60;
            }
            new TimePickerDialog(this, (view, h, m) -> {
                time[0] = String.format(Locale.US, "%02d:%02d", h, m);
                etStartTime.setText(RoutineStore.format12(time[0]));

                // Auto-fill end to start + 90 min (IIUC's usual class length) when it's empty or no
                // longer after the new start; the user can still adjust it.
                int startMin = RoutineStore.startMinutes(time[0]);
                if (end[0] == null || RoutineStore.startMinutes(end[0]) <= startMin) {
                    int auto = Math.min(startMin + 90, 23 * 60 + 59);
                    end[0] = String.format(Locale.US, "%02d:%02d", auto / 60, auto % 60);
                    etEndTime.setText(RoutineStore.format12(end[0]));
                }
            }, hour, minute, false).show();
        });

        etEndTime.setOnClickListener(v -> {
            int startMin = RoutineStore.startMinutes(time[0]);
            int current = RoutineStore.startMinutes(end[0]);
            int base = current >= 0
                    ? current
                    : (startMin >= 0 ? Math.min(startMin + 90, 23 * 60 + 59) : 10 * 60);
            new TimePickerDialog(this, (view, h, m) -> {
                end[0] = String.format(Locale.US, "%02d:%02d", h, m);
                etEndTime.setText(RoutineStore.format12(end[0]));
            }, base / 60, base % 60, false).show();
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(editing ? "Edit Class" : "Add Class")
                .setView(dialogView)
                // Pass null so the button doesn't auto-dismiss; we validate and dismiss ourselves.
                .setPositiveButton(editing ? "Update" : "Add", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            // The Add/Update button stays disabled until day + start + a later end + course are all
            // set, so it can never be tapped (and the dialog never dismisses) while incomplete.
            Runnable refresh = () -> positive.setEnabled(
                    dayIndexOf(spinnerDay.getText().toString().trim()) >= 0
                            && time[0] != null && RoutineStore.startMinutes(time[0]) >= 0
                            && end[0] != null
                            && RoutineStore.startMinutes(end[0]) > RoutineStore.startMinutes(time[0])
                            && etCourse.getText() != null
                            && !etCourse.getText().toString().trim().isEmpty());

            TextWatcher watcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
                @Override public void afterTextChanged(Editable s) { refresh.run(); }
            };
            // Picking a day/time and picking end all call setText, so watching every field covers
            // every input path.
            spinnerDay.addTextChangedListener(watcher);
            etStartTime.addTextChangedListener(watcher);
            etEndTime.addTextChangedListener(watcher);
            etCourse.addTextChangedListener(watcher);
            refresh.run();

            positive.setOnClickListener(v -> {
                int day = dayIndexOf(spinnerDay.getText().toString().trim());
                String course = etCourse.getText() != null ? etCourse.getText().toString().trim() : "";
                if (day < 0 || time[0] == null || RoutineStore.startMinutes(time[0]) < 0
                        || end[0] == null
                        || RoutineStore.startMinutes(end[0]) <= RoutineStore.startMinutes(time[0])
                        || TextUtils.isEmpty(course)) {
                    return; // shouldn't happen (button is disabled), but guard anyway
                }

                RoutineEntry entry = new RoutineEntry(day, time[0], end[0], course);
                if (editing && index >= 0 && index < entries.size()) {
                    entries.set(index, entry);
                } else {
                    entries.add(entry);
                }
                persist();
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void confirmDelete(final int index) {
        if (index < 0 || index >= entries.size()) return;
        new AlertDialog.Builder(this)
                .setTitle("Delete Class")
                .setMessage("Remove this class from your routine?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (index >= 0 && index < entries.size()) {
                        entries.remove(index);
                        persist();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Sorts, re-renders, caches, and writes the whole array back to Firestore. */
    private void persist() {
        RoutineStore.sort(entries);
        adapter.setItems(entries);
        updateEmptyState();
        RoutineStore.saveCache(this, entries);
        // Reschedule pre-class reminders off the freshly saved routine.
        ClassReminderScheduler.sync(this);

        if (uid == null) {
            toast("Not signed in — saved on this device only");
            return;
        }
        db.collection("users").document(uid)
                .update("routine", RoutineStore.toFirestore(entries))
                .addOnSuccessListener(v -> toast("Routine saved"))
                .addOnFailureListener(e -> toast("Save failed: " + e.getMessage()));
    }

    private void updateEmptyState() {
        boolean empty = entries.isEmpty();
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private int dayIndexOf(String name) {
        for (int i = 0; i < RoutineStore.DAY_NAMES.length; i++) {
            if (RoutineStore.DAY_NAMES[i].equals(name)) return i;
        }
        return -1;
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
