package com.tanim.ccepedia;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.util.Linkify;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class CommunityChatAdapter extends RecyclerView.Adapter<CommunityChatAdapter.MessageViewHolder> {

    public interface MessageInteractionListener {
        void onReplyToMessage(CommunityMessage message);
        void onMessageLongPressed(CommunityMessage message);
        void onOpenAttachment(CommunityMessage message);
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

        return new MessageViewHolder(view, listener, viewType == VIEW_TYPE_MESSAGE_SENT);
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

    /**
     * Diffs the incoming list against the current one and dispatches granular updates, so unchanged
     * rows keep their views — and the user's scroll position — instead of the full-rebind churn of
     * notifyDataSetChanged(). Used for both the realtime snapshot and pagination.
     */
    public void submitMessages(List<CommunityMessage> newList) {
        List<CommunityMessage> oldList = new ArrayList<>(messageList);
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new MessageDiff(oldList, newList));
        messageList.clear();
        messageList.addAll(newList);
        diff.dispatchUpdatesTo(this);
    }

    /** Identity = same sender + same send-time; contents = the fields bind() actually renders. */
    private static class MessageDiff extends DiffUtil.Callback {
        private final List<CommunityMessage> oldList;
        private final List<CommunityMessage> newList;

        MessageDiff(List<CommunityMessage> oldList, List<CommunityMessage> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override public int getOldListSize() { return oldList.size(); }
        @Override public int getNewListSize() { return newList.size(); }

        @Override
        public boolean areItemsTheSame(int oldPos, int newPos) {
            CommunityMessage a = oldList.get(oldPos), b = newList.get(newPos);
            return Objects.equals(a.getUserStudentId(), b.getUserStudentId())
                    && timeMs(a) == timeMs(b);
        }

        @Override
        public boolean areContentsTheSame(int oldPos, int newPos) {
            CommunityMessage a = oldList.get(oldPos), b = newList.get(newPos);
            return Objects.equals(a.getMessageText(), b.getMessageText())
                    && Objects.equals(a.getUserName(), b.getUserName())
                    && Objects.equals(a.getUserDepartment(), b.getUserDepartment())
                    && Objects.equals(a.getReplyToName(), b.getReplyToName())
                    && Objects.equals(a.getReplyToText(), b.getReplyToText())
                    && Objects.equals(a.getAttachmentType(), b.getAttachmentType())
                    && Objects.equals(a.getAttachmentUrl(), b.getAttachmentUrl())
                    && Objects.equals(a.getAttachmentTitle(), b.getAttachmentTitle());
        }

        private static long timeMs(CommunityMessage m) {
            return m.getTimestamp() != null ? m.getTimestamp().getTime() : -1L;
        }
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView senderName;
        TextView messageTime;
        View quoteContainer;
        TextView quoteName;
        TextView quoteText;
        View attachmentCard;
        ImageView attachmentIcon;
        TextView attachmentTitle;

        private final SimpleDateFormat timeFormatter = new SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault());
        private final MessageInteractionListener listener;
        private final boolean isSent;

        public MessageViewHolder(View itemView, MessageInteractionListener listener, boolean isSent) {
            super(itemView);
            this.listener = listener;
            this.isSent = isSent;

            messageText = itemView.findViewById(R.id.text_message_body);
            messageTime = itemView.findViewById(R.id.text_message_time);
            senderName = itemView.findViewById(R.id.text_message_name);
            quoteContainer = itemView.findViewById(R.id.quote_container);
            quoteName = itemView.findViewById(R.id.quote_name);
            quoteText = itemView.findViewById(R.id.quote_text);
            attachmentCard = itemView.findViewById(R.id.attachment_card);
            attachmentIcon = itemView.findViewById(R.id.attachment_icon);
            attachmentTitle = itemView.findViewById(R.id.attachment_title);

            if (messageTime != null) {
                messageTime.setVisibility(View.VISIBLE);
            }

            // Long-press opens the action menu (Reply / Copy / Delete). Attach to BOTH the row and
            // the text view: messageText has LinkMovementMethod, which consumes its own touches, so
            // a long-press landing on the text would never reach itemView on its own. Long-press and
            // link taps are distinct gestures, so both keep working on the text view.
            View.OnLongClickListener longPress = v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    CommunityMessage message = ((CommunityChatAdapter) getBindingAdapter()).messageList.get(position);
                    listener.onMessageLongPressed(message);
                    return true;
                }
                return false;
            };
            itemView.setOnLongClickListener(longPress);
            messageText.setOnLongClickListener(longPress);
        }

        public void bind(CommunityMessage message) {
            String raw = message.getMessageText() != null ? message.getMessageText() : "";
            SpannableString spannable = new SpannableString(raw);

            // 1) Linkify URLs first so their spans are laid down before we overlay mentions.
            //    Operate on the Spannable directly (not the TextView) so mention spans still
            //    apply even when the message contains no links.
            Linkify.addLinks(spannable, Linkify.WEB_URLS);

            // 2) Overlay @mention spans: bold + colored, NO underline (that distinguishes a
            //    mention from a tappable link). Color respects the bubble and the emerald rule —
            //    accent_community on received; bold white on the dark-emerald sent bubble.
            List<String> mentionNames = message.getMentionNames();
            if (mentionNames != null && !mentionNames.isEmpty()) {
                int mentionColor = ContextCompat.getColor(
                        messageText.getContext(),
                        isSent ? R.color.white : R.color.accent_community);
                for (String name : mentionNames) {
                    if (name == null || name.isEmpty()) continue;
                    String token = "@" + name;
                    int from = 0;
                    int idx;
                    while ((idx = raw.indexOf(token, from)) >= 0) {
                        int end = idx + token.length();
                        spannable.setSpan(new StyleSpan(Typeface.BOLD), idx, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        spannable.setSpan(new ForegroundColorSpan(mentionColor), idx, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        from = end;
                    }
                }
            }

            messageText.setText(spannable);
            messageText.setMovementMethod(LinkMovementMethod.getInstance());

            // Link color must contrast with the bubble it sits on, in both light and night:
            // sent bubble is dark emerald → bright mint; received bubble is neutral → emerald accent.
            int linkColor = ContextCompat.getColor(
                    messageText.getContext(),
                    isSent ? R.color.chat_link_on_sent : R.color.accentColor);
            messageText.setLinkTextColor(linkColor);

            if (senderName != null) {
                String dept = message.getUserDepartment();
                String deptDisplay = (dept != null && !dept.isEmpty()) ? " (" + dept + ")" : "";

                senderName.setText(message.getUserName() + " - " + message.getUserStudentId() + deptDisplay);
            }

            // Reply quote: render the denormalized snapshot of the replied-to message above the
            // body, or hide the block (must be explicit — this holder may be recycled from a reply).
            if (quoteContainer != null) {
                String replyName = message.getReplyToName();
                if (replyName != null && !replyName.isEmpty()) {
                    if (quoteName != null) quoteName.setText(replyName);
                    if (quoteText != null) {
                        String replyText = message.getReplyToText();
                        quoteText.setText(replyText != null ? replyText : "");
                    }
                    quoteContainer.setVisibility(View.VISIBLE);
                } else {
                    quoteContainer.setVisibility(View.GONE);
                }
            }

            // Shared-resource attachment card — shown only when the message carries one. An
            // attachment posted without a note hides the (empty) body so just the card shows.
            if (attachmentCard != null) {
                String attachmentType = message.getAttachmentType();
                if (attachmentType != null && !attachmentType.isEmpty()) {
                    Context ctx = attachmentCard.getContext();
                    boolean isLink = CommunityShare.TYPE_LINK.equals(attachmentType);
                    if (attachmentIcon != null) {
                        attachmentIcon.setImageResource(isLink ? R.drawable.ic_link : R.drawable.ic_pdf);
                        int glyph = ContextCompat.getColor(ctx, isLink ? R.color.accent_java : R.color.accent_resources);
                        int soft = ContextCompat.getColor(ctx, isLink ? R.color.accent_java_soft : R.color.accent_resources_soft);
                        attachmentIcon.setImageTintList(ColorStateList.valueOf(glyph));
                        attachmentIcon.setBackgroundTintList(ColorStateList.valueOf(soft));
                    }
                    if (attachmentTitle != null) {
                        String title = message.getAttachmentTitle();
                        attachmentTitle.setText(title != null && !title.isEmpty() ? title : message.getAttachmentUrl());
                    }
                    attachmentCard.setVisibility(View.VISIBLE);
                    attachmentCard.setOnClickListener(v -> listener.onOpenAttachment(message));
                    messageText.setVisibility(raw.isEmpty() ? View.GONE : View.VISIBLE);
                } else {
                    attachmentCard.setVisibility(View.GONE);
                    attachmentCard.setOnClickListener(null);
                    messageText.setVisibility(View.VISIBLE);
                }
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