package com.tanim.ccepedia;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.annotation.NonNull;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.tanim.ccepedia.CommunityChatAdapter.MessageInteractionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

public class CommunityActivity extends AppCompatActivity implements MessageInteractionListener {
    private static final String TAG = "CommunityActivity";

    // On-device read tracking, shared with HomeFragment's unread badge.
    public static final String PREFS_COMMUNITY = "community_prefs";
    public static String lastSeenKey(String studentId) {
        return "last_seen_ts_" + studentId;
    }

    private static final int MAX_MENTION_SUGGESTIONS = 6;

    private FirebaseFirestore db;
    private CollectionReference chatRef;

    private RecyclerView recyclerView;
    private CommunityChatAdapter chatAdapter;
    private final List<CommunityMessage> messageList = new ArrayList<>();
    private LinearLayoutManager layoutManager;

    private EditText messageEditText;
    private FloatingActionButton sendButton;

    // Reply composition — non-null while the user is composing a reply to a specific message.
    private CommunityMessage replyingTo;
    private View replyPreviewContainer;
    private TextView replyPreviewName;
    private TextView replyPreviewSnippet;
    private ImageView replyPreviewClose;

    // @mention autocomplete
    private MaterialCardView mentionCard;
    private RecyclerView mentionRecyclerView;
    private MentionSuggestionAdapter mentionAdapter;
    private final List<UserListModel> mentionSuggestions = new ArrayList<>();
    private List<UserListModel> directoryCache = null;   // lazily loaded on first '@'
    private boolean directoryLoading = false;
    private boolean isInsertingMention = false;          // guards the TextWatcher during programmatic insert
    // Display name -> student ID for mentions picked this session; reconciled against the text at send.
    private final LinkedHashMap<String, String> pendingMentions = new LinkedHashMap<>();

    private final String currentUserEmail = UserData.getInstance().getEmail();
    private final String currentStudentId = UserData.getInstance().getStudentId();
    private final String currentUserName = UserData.getInstance().getName();
    private final String currentUserDepartment = UserData.getInstance().getDepartmentName();

    private boolean isLoading = false;
    private boolean moreMessagesAvailable = true;
    private DocumentSnapshot oldestMessageSnapshot = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);

        Toolbar toolbar = findViewById(R.id.toolbar_community);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Community Chat");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.communityChatRecyclerView);
        messageEditText = findViewById(R.id.communityMessageEditText);
        sendButton = findViewById(R.id.communitySendButton);

        replyPreviewContainer = findViewById(R.id.replyPreviewContainer);
        replyPreviewName = findViewById(R.id.replyPreviewName);
        replyPreviewSnippet = findViewById(R.id.replyPreviewSnippet);
        replyPreviewClose = findViewById(R.id.replyPreviewClose);
        replyPreviewClose.setOnClickListener(v -> cancelReply());

        layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);

        chatAdapter = new CommunityChatAdapter(messageList, currentStudentId, this);
        recyclerView.setAdapter(chatAdapter);
        attachSwipeToReply();

        db = FirebaseFirestore.getInstance();
        chatRef = db.collection("community_messages");

        setupMentionAutocomplete();

        sendButton.setOnClickListener(v -> sendMessage());
        messageEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (layoutManager.findFirstCompletelyVisibleItemPosition() == 0 && dy < 0) {
                    if (!isLoading && moreMessagesAvailable) {
                        loadMoreMessages();
                    }
                }
            }
        });

        loadMessages();
    }

    @Override
    public void onReplyToMessage(CommunityMessage message) {
        showReplyPreview(message);
    }

    @Override
    public void onMessageLongPressed(CommunityMessage message) {
        showMessageActions(message);
    }

    /** Tapping a shared-resource card opens it inside MainActivity's middle fragment container. */
    @Override
    public void onOpenAttachment(CommunityMessage message) {
        // Fragment-only types (like bus_schedule, course_list, file_list) don't need a URL — they load from Firestore.
        boolean needsUrl = !CommunityShare.TYPE_BUS_SCHEDULE.equals(message.getAttachmentType())
                && !CommunityShare.TYPE_COURSE_LIST.equals(message.getAttachmentType())
                && !CommunityShare.TYPE_FILE_LIST.equals(message.getAttachmentType());
        if (needsUrl && (message.getAttachmentUrl() == null || message.getAttachmentUrl().isEmpty())) {
            Toast.makeText(this, R.string.share_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        // Reuse the MainActivity instance already below us in this task: CLEAR_TOP | SINGLE_TOP
        // clears the chat off the stack and delivers the resource to MainActivity.onNewIntent(),
        // which shows it in the middle container just like a normally-opened resource.
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(MainActivity.EXTRA_OPEN_TYPE, message.getAttachmentType());
        intent.putExtra(MainActivity.EXTRA_OPEN_URL, message.getAttachmentUrl());
        intent.putExtra(MainActivity.EXTRA_OPEN_TITLE, message.getAttachmentTitle());
        startActivity(intent);
    }

    private boolean canCurrentUserDelete(CommunityMessage message) {
        if (UserData.getInstance() == null) return false;
        String currentUserRole = UserData.getInstance().getRole();
        if ("admin".equals(currentUserRole)) {
            return true;
        }
        return currentStudentId != null && currentStudentId.equalsIgnoreCase(message.getUserStudentId());
    }

    public void showDeleteDialog(CommunityMessage message) {
        if (canCurrentUserDelete(message)) {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Message")
                    .setMessage("Are you sure you want to delete this message? This action cannot be undone.")
                    .setPositiveButton("Delete", (dialog, which) -> deleteMessage(message))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }

    private void deleteMessage(CommunityMessage message) {
        if (message.getTimestamp() == null) {
            Toast.makeText(this, "Cannot delete message without a valid timestamp.", Toast.LENGTH_SHORT).show();
            return;
        }

        chatRef.whereEqualTo("timestamp", message.getTimestamp())
                .whereEqualTo("userStudentId", message.getUserStudentId())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        String documentId = task.getResult().getDocuments().get(0).getId();
                        chatRef.document(documentId).delete()
                                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Message deleted.", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error deleting message", e);
                                    Toast.makeText(this, "Failed to delete message: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    } else {
                        Log.w(TAG, "Message not found for deletion query.", task.getException());
                        Toast.makeText(this, "Message not found or failed to query.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ---------------------------------------------------------------------------------------------
    // Reply + message actions
    // ---------------------------------------------------------------------------------------------

    /**
     * Swipe (either direction) on a message triggers a reply to it. The swipe is intentionally
     * un-completable: huge swipe-threshold + escape-velocity mean the row always recovers back to
     * translationX=0 and {@code onSwiped} never fires — so we never issue a notify* from the swipe
     * path, which would otherwise race {@code loadMessages()}'s per-snapshot rebinds. We detect the
     * trigger from the real finger delta and commit the reply on release, in {@code clearView}.
     */
    private void attachSwipeToReply() {
        final Drawable replyIcon = ContextCompat.getDrawable(this, R.drawable.ic_reply);
        if (replyIcon != null) {
            replyIcon.setTint(ContextCompat.getColor(this, R.color.accent_community));
        }
        final float maxGutterPx = 64f * getResources().getDisplayMetrics().density;

        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            private boolean replyTriggered = false;

            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder vh) {
                return 2f; // unreachable by distance
            }

            @Override
            public float getSwipeEscapeVelocity(float defaultValue) {
                return Float.MAX_VALUE; // unreachable by flick
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder vh, int actionState) {
                super.onSelectedChanged(vh, actionState);
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    replyTriggered = false;
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView rv,
                                    @NonNull RecyclerView.ViewHolder vh, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                if (actionState != ItemTouchHelper.ACTION_STATE_SWIPE) {
                    super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive);
                    return;
                }

                View row = vh.itemView;
                float triggerPx = row.getWidth() * 0.32f;

                // Clamp the visible translation to a fixed gutter so the row never flies off-screen.
                float clamped = Math.max(-maxGutterPx, Math.min(dX, maxGutterPx));
                super.onChildDraw(c, rv, vh, clamped, dY, actionState, isCurrentlyActive);

                // Arm once the real finger delta crosses the trigger; the reply commits on release.
                if (isCurrentlyActive && !replyTriggered && triggerPx > 0 && Math.abs(dX) >= triggerPx) {
                    replyTriggered = true;
                    row.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP,
                            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
                }

                if (replyIcon != null && triggerPx > 0 && Math.abs(dX) > 2f) {
                    float progress = Math.min(1f, Math.abs(dX) / triggerPx);
                    int iw = replyIcon.getIntrinsicWidth();
                    int ih = replyIcon.getIntrinsicHeight();
                    int top = row.getTop() + (row.getHeight() - ih) / 2;
                    int left = dX > 0
                            ? row.getLeft() + (int) ((maxGutterPx - iw) / 2f)
                            : row.getRight() - (int) ((maxGutterPx - iw) / 2f) - iw;
                    replyIcon.setBounds(left, top, left + iw, top + ih);
                    replyIcon.setAlpha((int) (progress * 255));
                    replyIcon.draw(c);
                    replyIcon.setAlpha(255);
                }
            }

            @Override
            public void clearView(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
                super.clearView(rv, vh);
                if (replyTriggered) {
                    replyTriggered = false;
                    int pos = vh.getBindingAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION && pos < messageList.size()) {
                        onReplyToMessage(messageList.get(pos));
                    }
                }
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
                // Unreachable — thresholds above prevent completion. Required by the abstract class.
            }
        };

        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
    }

    private void showReplyPreview(CommunityMessage message) {
        replyingTo = message;

        String name;
        if (currentStudentId != null && currentStudentId.equalsIgnoreCase(message.getUserStudentId())) {
            name = getString(R.string.reply_to_self);
        } else {
            name = message.getUserName() != null ? message.getUserName() : "";
        }
        replyPreviewName.setText(getString(R.string.reply_preview_prefix, name));
        replyPreviewSnippet.setText(message.getMessageText() != null ? message.getMessageText() : "");
        replyPreviewContainer.setVisibility(View.VISIBLE);

        messageEditText.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(messageEditText, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void cancelReply() {
        replyingTo = null;
        if (replyPreviewContainer != null) {
            replyPreviewContainer.setVisibility(View.GONE);
        }
    }

    /** Collapses whitespace and caps the stored quote so reply docs stay small and 1–2 lines tall. */
    private String buildReplySnippet(String text) {
        if (text == null) return "";
        String collapsed = text.replaceAll("\\s+", " ").trim();
        if (collapsed.length() > 120) {
            return collapsed.substring(0, 120).trim() + "…";
        }
        return collapsed;
    }

    private void copyToClipboard(CommunityMessage message) {
        String text = message.getMessageText() != null ? message.getMessageText() : "";
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("Community Message", text));
            Toast.makeText(this, "Message copied to clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    private void showMessageActions(CommunityMessage message) {
        List<String> items = new ArrayList<>();
        items.add(getString(R.string.msg_action_reply));
        items.add(getString(R.string.msg_action_copy));
        if (canCurrentUserDelete(message)) {
            items.add(getString(R.string.msg_action_delete));
        }
        CharSequence[] options = items.toArray(new CharSequence[0]);

        new MaterialAlertDialogBuilder(this)
                .setItems(options, (dialog, which) -> {
                    String choice = items.get(which);
                    if (choice.equals(getString(R.string.msg_action_reply))) {
                        showReplyPreview(message);
                    } else if (choice.equals(getString(R.string.msg_action_copy))) {
                        copyToClipboard(message);
                    } else if (choice.equals(getString(R.string.msg_action_delete))) {
                        showDeleteDialog(message);
                    }
                })
                .show();
    }

    private void loadMessages() {
        isLoading = true;
        chatRef.orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        isLoading = false;
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        oldestMessageSnapshot = snapshots.getDocuments().get(snapshots.size() - 1);

                        List<CommunityMessage> newMessages = new ArrayList<>();
                        for (CommunityMessage message : snapshots.toObjects(CommunityMessage.class)) {
                            newMessages.add(message);
                        }
                        Collections.reverse(newMessages);

                        boolean wasAtBottom = isNearBottom();
                        chatAdapter.submitMessages(newMessages);
                        if (wasAtBottom) recyclerView.scrollToPosition(messageList.size() - 1);
                    }
                    isLoading = false;
                    moreMessagesAvailable = snapshots.size() >= 50;
                });
    }

    /**
     * True when the newest messages are already on screen (or the list is empty, i.e. first load).
     * Lets a new message pull the view to the bottom without yanking the user down while they're
     * scrolled up reading history.
     */
    private boolean isNearBottom() {
        if (messageList.isEmpty()) return true;
        int last = layoutManager.findLastVisibleItemPosition();
        return last == RecyclerView.NO_POSITION || last >= messageList.size() - 2;
    }

    private void loadMoreMessages() {
        if (oldestMessageSnapshot == null) return;

        isLoading = true;
        moreMessagesAvailable = false;

        chatRef.orderBy("timestamp", Query.Direction.DESCENDING)
                .startAfter(oldestMessageSnapshot)
                .limit(50)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots != null && !snapshots.isEmpty()) {
                        oldestMessageSnapshot = snapshots.getDocuments().get(snapshots.size() - 1);

                        List<CommunityMessage> olderMessages = new ArrayList<>();
                        for (CommunityMessage message : snapshots.toObjects(CommunityMessage.class)) {
                            olderMessages.add(message);
                        }
                        Collections.reverse(olderMessages);

                        List<CommunityMessage> combined = new ArrayList<>(olderMessages);
                        combined.addAll(messageList);
                        chatAdapter.submitMessages(combined);

                    }
                    isLoading = false;
                })
                .addOnFailureListener(e -> {
                    isLoading = false;
                    Toast.makeText(this, "Failed to load older messages.", Toast.LENGTH_SHORT).show();
                });
    }

    private void sendMessage() {
        String messageText = messageEditText.getText().toString().trim();

        if (messageText.isEmpty()) {
            Toast.makeText(this, "Message cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Reconcile picked mentions against the final text — keep only those whose "@Name"
        // token still survives in what's actually being sent.
        List<String> mentionIds = new ArrayList<>();
        List<String> mentionNames = new ArrayList<>();
        for (Map.Entry<String, String> entry : pendingMentions.entrySet()) {
            String name = entry.getKey();
            if (messageText.contains("@" + name)) {
                mentionNames.add(name);
                mentionIds.add(entry.getValue());
            }
        }

        Map<String, Object> message = new HashMap<>();
        message.put("userStudentId", currentStudentId);
        message.put("userEmail", currentUserEmail);
        message.put("userName", currentUserName);
        message.put("userDepartment", currentUserDepartment);
        message.put("messageText", messageText);
        message.put("timestamp", FieldValue.serverTimestamp());
        if (!mentionIds.isEmpty()) {
            message.put("mentions", mentionIds);
            message.put("mentionNames", mentionNames);
        }
        if (replyingTo != null) {
            // Store the actual sender name (not "You") so every reader sees who was quoted.
            String replyName = replyingTo.getUserName();
            message.put("replyToName", replyName != null ? replyName : "");
            message.put("replyToText", buildReplySnippet(replyingTo.getMessageText()));
            message.put("replyToStudentId", replyingTo.getUserStudentId());
        }

        chatRef.add(message)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Message sent: " + documentReference.getId());
                    messageEditText.setText("");
                    pendingMentions.clear();
                    hideMentionSuggestions();
                    cancelReply();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending message", e);
                    Toast.makeText(this, "Failed to send message.", Toast.LENGTH_SHORT).show();
                });
    }

    // ---------------------------------------------------------------------------------------------
    // @mention autocomplete
    // ---------------------------------------------------------------------------------------------

    private void setupMentionAutocomplete() {
        mentionCard = findViewById(R.id.mentionSuggestionCard);
        mentionRecyclerView = findViewById(R.id.mentionSuggestionRecyclerView);
        mentionRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mentionAdapter = new MentionSuggestionAdapter(mentionSuggestions, this::onMentionSelected);
        mentionRecyclerView.setAdapter(mentionAdapter);

        messageEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                if (isInsertingMention) return;
                updateMentionSuggestions();
            }
        });
    }

    /** Re-derives the active @-token at the cursor and shows/hides the suggestion list. */
    private void updateMentionSuggestions() {
        int cursor = messageEditText.getSelectionStart();
        Editable text = messageEditText.getText();
        if (text == null || cursor < 0) {
            hideMentionSuggestions();
            return;
        }

        int at = findActiveAtIndex(text, cursor);
        if (at < 0) {
            hideMentionSuggestions();
            return;
        }

        if (directoryCache == null) {
            if (!directoryLoading) loadDirectory();   // re-runs this method on success
            return;
        }

        String query = text.subSequence(at + 1, cursor).toString();
        List<UserListModel> results = filterDirectory(query);
        if (results.isEmpty()) {
            hideMentionSuggestions();
            return;
        }
        showMentionSuggestions(results);
    }

    /**
     * Index of the '@' that begins the mention token ending at the cursor, or -1 if none.
     * Valid only when the '@' is at the start or follows whitespace, and no newline sits between
     * it and the cursor.
     */
    private int findActiveAtIndex(CharSequence s, int cursor) {
        for (int i = cursor - 1; i >= 0; i--) {
            char c = s.charAt(i);
            if (c == '\n') return -1;
            if (c == '@') {
                if (i == 0 || Character.isWhitespace(s.charAt(i - 1))) return i;
                return -1;
            }
        }
        return -1;
    }

    private List<UserListModel> filterDirectory(String query) {
        List<UserListModel> out = new ArrayList<>();
        if (directoryCache == null) return out;

        String q = query.toLowerCase(Locale.getDefault()).trim();
        for (UserListModel user : directoryCache) {
            if (out.size() >= MAX_MENTION_SUGGESTIONS) break;
            String name = user.getName();
            String id = user.getStudentId();
            if (name == null) continue;
            boolean match = q.isEmpty()
                    || name.toLowerCase(Locale.getDefault()).contains(q)
                    || (id != null && id.toLowerCase(Locale.getDefault()).contains(q));
            if (match) out.add(user);
        }
        return out;
    }

    private void loadDirectory() {
        directoryLoading = true;
        db.collection("users")
                .get()
                .addOnSuccessListener(snapshots -> {
                    directoryLoading = false;
                    List<UserListModel> list = new ArrayList<>();
                    for (UserListModel user : snapshots.toObjects(UserListModel.class)) {
                        if (user.getName() == null || user.getName().isEmpty()) continue;
                        if (user.getStudentId() == null) continue;
                        // Don't offer to mention yourself.
                        if (currentStudentId != null && currentStudentId.equalsIgnoreCase(user.getStudentId())) continue;
                        list.add(user);
                    }
                    directoryCache = list;
                    updateMentionSuggestions();   // token may still be active
                })
                .addOnFailureListener(e -> {
                    directoryLoading = false;
                    Log.w(TAG, "Failed to load user directory for mentions", e);
                });
    }

    private void showMentionSuggestions(List<UserListModel> results) {
        mentionSuggestions.clear();
        mentionSuggestions.addAll(results);
        mentionAdapter.notifyDataSetChanged();
        mentionRecyclerView.scrollToPosition(0);
        mentionCard.setVisibility(android.view.View.VISIBLE);
    }

    private void hideMentionSuggestions() {
        if (mentionCard != null) {
            mentionCard.setVisibility(android.view.View.GONE);
        }
    }

    /** Replaces the active @-token with "@Display Name " and records the pick for send-time reconcile. */
    private void onMentionSelected(UserListModel user) {
        Editable text = messageEditText.getText();
        if (text == null) return;
        int cursor = messageEditText.getSelectionStart();
        int at = findActiveAtIndex(text, cursor);
        if (at < 0) return;

        String display = user.getName();
        String replacement = "@" + display + " ";

        isInsertingMention = true;
        text.replace(at, cursor, replacement);
        isInsertingMention = false;

        pendingMentions.put(display, user.getStudentId());
        hideMentionSuggestions();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Leaving the chat means everything currently loaded has been seen — stamp "now" so the
        // Home unread badge reads zero on return. Own just-sent messages fall at/under this too.
        if (currentStudentId != null) {
            getSharedPreferences(PREFS_COMMUNITY, MODE_PRIVATE)
                    .edit()
                    .putLong(lastSeenKey(currentStudentId), System.currentTimeMillis())
                    .apply();
        }
    }
}