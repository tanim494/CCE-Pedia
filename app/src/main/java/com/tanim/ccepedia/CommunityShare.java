package com.tanim.ccepedia;

import android.content.Context;
import android.widget.Toast;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Posts a resource (a PDF or a web link) into the Community Chat as an attachment message.
 *
 * The written document is shape-identical to a normal chat message (mirrors
 * {@code CommunityActivity.sendMessage()}) plus three attachment fields, so existing readers and
 * the unread-badge query keep working, and older clients simply ignore the extra keys.
 */
public final class CommunityShare {

    public static final String TYPE_PDF = "pdf";
    public static final String TYPE_LINK = "link";

    private CommunityShare() {}

    /**
     * @param caption optional note typed by the sharer; may be null or empty.
     */
    static void post(Context context, String type, String url, String title, String caption) {
        UserData user = UserData.getInstance();
        if (user == null || user.getStudentId() == null) {
            Toast.makeText(context, R.string.share_signed_out, Toast.LENGTH_SHORT).show();
            return;
        }
        if (url == null || url.isEmpty()) {
            Toast.makeText(context, R.string.share_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> message = new HashMap<>();
        message.put("userStudentId", user.getStudentId());
        message.put("userEmail", user.getEmail());
        message.put("userName", user.getName());
        message.put("userDepartment", user.getDepartmentName());
        message.put("messageText", caption == null ? "" : caption.trim());
        message.put("timestamp", FieldValue.serverTimestamp());
        message.put("attachmentType", type);
        message.put("attachmentUrl", url);
        message.put("attachmentTitle", title != null ? title : "");

        // Use the application context for the async result: the sharing screen may be gone by then.
        Context appContext = context.getApplicationContext();
        FirebaseFirestore.getInstance().collection("community_messages").add(message)
                .addOnSuccessListener(ref ->
                        Toast.makeText(appContext, R.string.share_success, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(appContext, R.string.share_failed, Toast.LENGTH_SHORT).show());
    }
}
