package com.tanim.ccepedia;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class NoticeAdapter extends RecyclerView.Adapter<NoticeAdapter.NoticeViewHolder> {

    public interface OnItemClickListener {
        void onEditClick(Notice notice);
        void onDeleteClick(Notice notice);
    }

    private List<Notice> noticeList;
    private OnItemClickListener listener;

    public NoticeAdapter(List<Notice> noticeList, OnItemClickListener listener) {
        this.noticeList = noticeList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoticeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new NoticeViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull NoticeViewHolder holder, int position) {
        Notice notice = noticeList.get(position);
        holder.tvText.setText(notice.getText());
        holder.tvLink.setText(notice.getLink() != null && !notice.getLink().isEmpty() ? notice.getLink() : "(No Link)");

        holder.itemView.setOnClickListener(v -> listener.onEditClick(notice));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onDeleteClick(notice);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return noticeList.size();
    }

    public static class NoticeViewHolder extends RecyclerView.ViewHolder {
        TextView tvText, tvLink;
        public NoticeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvText = itemView.findViewById(android.R.id.text1);
            tvLink = itemView.findViewById(android.R.id.text2);
        }
    }
}
