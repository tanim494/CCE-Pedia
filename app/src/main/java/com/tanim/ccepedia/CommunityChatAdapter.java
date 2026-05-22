package com.tanim.ccepedia;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import android.content.ClipData;
import android.content.ClipboardManager;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommunityChatAdapter extends RecyclerView.Adapter<CommunityChatAdapter.MessageViewHolder> {

    public interface MessageInteractionListener {
        void onDeleteMessage(CommunityMessage message);
    }

    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;

    private final List<CommunityMessage> messageList;
    private final String currentStudentId;
    private final MessageInteractionListener listener;

    public CommunityChatAdapter(List<CommunityMessage> messageList, String currentStudentId, MessageInteractionListener listener) {
        this.messageList = messageList;
        this.currentStudentId = currentStudentId;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        CommunityMessage message = messageList.get(position);

        String messageStudentId = message.getUserStudentId();

        if (currentStudentId != null && currentStudentId.equals(messageStudentId)) {
            return VIEW_TYPE_MESSAGE_SENT;
        } else {
            return VIEW_TYPE_MESSAGE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;

        if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
        }

        return new MessageViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        CommunityMessage message = messageList.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView senderName;
        TextView messageTime;

        private final SimpleDateFormat timeFormatter = new SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault());
        private final MessageInteractionListener listener;

        public MessageViewHolder(View itemView, MessageInteractionListener listener) {
            super(itemView);
            this.listener = listener;

            messageText = itemView.findViewById(R.id.text_message_body);
            messageTime = itemView.findViewById(R.id.text_message_time);
            senderName = itemView.findViewById(R.id.text_message_name);

            if (messageTime != null) {
                messageTime.setVisibility(View.VISIBLE);
            }

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    CommunityMessage message = ((CommunityChatAdapter) getBindingAdapter()).messageList.get(position);
                    listener.onDeleteMessage(message);
                }
            });

            messageText.setOnLongClickListener(v -> {
                String textToCopy = messageText.getText().toString();
                ClipboardManager clipboard = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Community Message", textToCopy);

                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(v.getContext(), "Message copied to clipboard", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }

        public void bind(CommunityMessage message) {
            messageText.setText(message.getMessageText());

            android.text.util.Linkify.addLinks(messageText, android.text.util.Linkify.WEB_URLS);
            messageText.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());

            messageText.setLinkTextColor(android.graphics.Color.parseColor("#00BCD4"));

            if (senderName != null) {
                String dept = message.getUserDepartment();
                String deptDisplay = (dept != null && !dept.isEmpty()) ? " (" + dept + ")" : "";

                senderName.setText(message.getUserName() + " - " + message.getUserStudentId() + deptDisplay);
            }

            Date timestamp = message.getTimestamp();
            if (timestamp != null) {
                messageTime.setText(timeFormatter.format(timestamp));
            } else {
                messageTime.setText("");
            }
        }
    }
}