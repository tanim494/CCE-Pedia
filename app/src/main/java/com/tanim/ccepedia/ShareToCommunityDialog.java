package com.tanim.ccepedia;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

/**
 * The optional "add a note" sheet shown before a resource is posted to the Community Chat.
 * Keeps the viewer fragments thin: they call {@link #show} with the resource, the note is
 * optional, and Share hands off to {@link CommunityShare#post}.
 */
final class ShareToCommunityDialog {

    private ShareToCommunityDialog() {}

    static void show(Context context, String type, String url, String title) {
        View content = LayoutInflater.from(context).inflate(R.layout.dialog_share_to_community, null);
        TextInputEditText noteInput = content.findViewById(R.id.etShareNote);

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.share_to_community)
                .setView(content)
                .setPositiveButton(R.string.share_action_share, (dialog, which) -> {
                    String caption = noteInput.getText() != null ? noteInput.getText().toString() : "";
                    CommunityShare.post(context, type, url, title, caption);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
