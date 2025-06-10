package com.tanim.ccepedia;

import android.app.AlertDialog;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;


public class ManageMessagesNoticesFragment extends Fragment {

    private Button btnAddNotice, btnAddMessage;
    private RecyclerView recyclerViewNotices, recyclerViewMessages;

    private FirebaseFirestore firestore;
    private CollectionReference noticesRef, messagesRef;

    private List<Notice> noticeList = new ArrayList<>();
    private List<Message> messageList = new ArrayList<>();

    private NoticeAdapter noticeAdapter;
    private MessageAdapter messageAdapter;

    private ListenerRegistration noticesListener;
    private ListenerRegistration messagesListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_manage_messages_notices, container, false);

        btnAddNotice = root.findViewById(R.id.btnAddNotice);
        btnAddMessage = root.findViewById(R.id.btnAddMessage);
        recyclerViewNotices = root.findViewById(R.id.recyclerViewNotices);
        recyclerViewMessages = root.findViewById(R.id.recyclerViewMessages);

        firestore = FirebaseFirestore.getInstance();
        noticesRef = firestore.collection("notices");
        messagesRef = firestore.collection("messages");

        setupRecyclerViews();
        setupListeners();

        btnAddNotice.setOnClickListener(v -> showAddNoticeDialog());
        btnAddMessage.setOnClickListener(v -> showAddMessageDialog());

        return root;
    }

    private void setupRecyclerViews() {
        noticeAdapter = new NoticeAdapter(noticeList, new NoticeAdapter.OnItemClickListener() {
            @Override
            public void onEditClick(Notice notice) {
                showEditNoticeDialog(notice);
            }

            @Override
            public void onDeleteClick(Notice notice) {
                confirmDeleteNotice(notice);
            }
        });

        messageAdapter = new MessageAdapter(messageList, new MessageAdapter.OnItemClickListener() {
            @Override
            public void onEditClick(Message message) {
                showEditMessageDialog(message);
            }

            @Override
            public void onDeleteClick(Message message) {
                confirmDeleteMessage(message);
            }
        });

        recyclerViewNotices.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewNotices.setAdapter(noticeAdapter);

        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewMessages.setAdapter(messageAdapter);
    }

    private void setupListeners() {
        noticesListener = noticesRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Toast.makeText(getContext(), "Listen failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            if (value != null) {
                noticeList.clear();
                for (QueryDocumentSnapshot doc : value) {
                    Notice notice = doc.toObject(Notice.class);
                    notice.setId(doc.getId());
                    noticeList.add(notice);
                }
                noticeAdapter.notifyDataSetChanged();
            }
        });

        messagesListener = messagesRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Toast.makeText(getContext(), "Listen failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            if (value != null) {
                messageList.clear();
                for (QueryDocumentSnapshot doc : value) {
                    Message message = doc.toObject(Message.class);
                    message.setId(doc.getId());
                    messageList.add(message);
                }
                messageAdapter.notifyDataSetChanged();
            }
        });
    }

    // Notice dialogs and CRUD

    private void showAddNoticeDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_edit_notice, null);
        EditText etText = dialogView.findViewById(R.id.etNoticeText);
        EditText etLink = dialogView.findViewById(R.id.etNoticeLink);

        new AlertDialog.Builder(getContext())
                .setTitle("Add Notice")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String text = etText.getText().toString().trim();
                    String link = etLink.getText().toString().trim();
                    if (TextUtils.isEmpty(text)) {
                        Toast.makeText(getContext(), "Notice text cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    addNotice(text, link);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addNotice(String text, String link) {
        Notice notice = new Notice(null, text, link);
        noticesRef.add(notice)
                .addOnSuccessListener(docRef -> Toast.makeText(getContext(), "Notice added", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to add notice: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showEditNoticeDialog(Notice notice) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_edit_notice, null);
        EditText etText = dialogView.findViewById(R.id.etNoticeText);
        EditText etLink = dialogView.findViewById(R.id.etNoticeLink);

        etText.setText(notice.getText());
        etLink.setText(notice.getLink());

        new AlertDialog.Builder(getContext())
                .setTitle("Edit Notice")
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String text = etText.getText().toString().trim();
                    String link = etLink.getText().toString().trim();
                    if (TextUtils.isEmpty(text)) {
                        Toast.makeText(getContext(), "Notice text cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updateNotice(notice.getId(), text, link);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateNotice(String id, String text, String link) {
        noticesRef.document(id)
                .update("text", text, "link", link)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Notice updated", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update notice: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void confirmDeleteNotice(Notice notice) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Notice")
                .setMessage("Are you sure you want to delete this notice?")
                .setPositiveButton("Delete", (dialog, which) -> deleteNotice(notice))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteNotice(Notice notice) {
        noticesRef.document(notice.getId())
                .delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Notice deleted", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to delete notice: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Message dialogs and CRUD

    private void showAddMessageDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_edit_message, null);
        EditText etText = dialogView.findViewById(R.id.etMessageText);

        new AlertDialog.Builder(getContext())
                .setTitle("Add Message")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String text = etText.getText().toString().trim();
                    if (TextUtils.isEmpty(text)) {
                        Toast.makeText(getContext(), "Message text cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    addMessage(text);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addMessage(String text) {
        Message message = new Message(null, text);
        messagesRef.add(message)
                .addOnSuccessListener(docRef -> Toast.makeText(getContext(), "Message added", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to add message: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showEditMessageDialog(Message message) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_edit_message, null);
        EditText etText = dialogView.findViewById(R.id.etMessageText);

        etText.setText(message.getText());

        new AlertDialog.Builder(getContext())
                .setTitle("Edit Message")
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String text = etText.getText().toString().trim();
                    if (TextUtils.isEmpty(text)) {
                        Toast.makeText(getContext(), "Message text cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updateMessage(message.getId(), text);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateMessage(String id, String text) {
        messagesRef.document(id)
                .update("text", text)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Message updated", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update message: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void confirmDeleteMessage(Message message) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Message")
                .setMessage("Are you sure you want to delete this message?")
                .setPositiveButton("Delete", (dialog, which) -> deleteMessage(message))
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void deleteMessage(Message message) {
        messagesRef.document(message.getId())
                .delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Message deleted", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to delete message: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up Firestore listeners
        if (noticesListener != null) {
            noticesListener.remove();
        }
        if (messagesListener != null) {
            messagesListener.remove();
        }
    }
}


