package com.tanim.ccepedia;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

/**
 * Drives the @mention autocomplete list in the community chat. Backed by the same
 * {@link UserListModel} used by the admin user directory; emits the picked user via a callback.
 */
public class MentionSuggestionAdapter extends RecyclerView.Adapter<MentionSuggestionAdapter.ViewHolder> {

    public interface OnMentionSelectedListener {
        void onMentionSelected(UserListModel user);
    }

    private final List<UserListModel> suggestions;
    private final OnMentionSelectedListener listener;

    public MentionSuggestionAdapter(List<UserListModel> suggestions, OnMentionSelectedListener listener) {
        this.suggestions = suggestions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mention_suggestion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserListModel user = suggestions.get(position);

        String name = user.getName() != null ? user.getName() : "";
        holder.name.setText(name);
        holder.id.setText(user.getStudentId() != null ? user.getStudentId() : "");
        holder.initial.setText(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.getDefault()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMentionSelected(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return suggestions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView initial;
        final TextView name;
        final TextView id;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            initial = itemView.findViewById(R.id.mention_initial);
            name = itemView.findViewById(R.id.mention_name);
            id = itemView.findViewById(R.id.mention_id);
        }
    }
}
