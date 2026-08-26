package com.tanim.ccepedia;

import android.content.Context;
import android.widget.Toast;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
    public static final String TYPE_BUS_SCHEDULE = "bus_schedule";
    public static final String TYPE_COURSE_LIST = "course_list";
    public static final String TYPE_FILE_LIST = "file_list";

    private CommunityShare() {}

    /** Fragment types that load their data from Firestore and don't need a URL to reopen. */
    private static final Set<String> FRAGMENT_ONLY_TYPES = Set.of(
            TYPE_BUS_SCHEDULE,
            TYPE_COURSE_LIST,
            TYPE_FILE_LIST
    );

    /**
     * @param caption optional note typed by the sharer; may be null or empty.
     * @param url     required for TYPE_PDF and TYPE_LINK; ignored for fragment-only types.
     */
    static void post(Context context, String type, String url, String title, String caption) {
        UserData user = UserData.getInstance();
        if (user == null || user.getStudentId() == null) {
            Toast.makeText(context, R.string.share_signed_out, Toast.LENGTH_SHORT).show();
            return;
        }
        boolean needsUrl = !FRAGMENT_ONLY_TYPES.contains(type);
        if (needsUrl && (url == null || url.isEmpty())) {
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
        if (!needsUrl) {
            // fragment-only: store empty string so the field exists but isn't used
            message.put("attachmentUrl", "");
        } else {
            message.put("attachmentUrl", url);
        }
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
