package com.tanim.ccepedia;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DriveLinkItemAdapter extends RecyclerView.Adapter<DriveLinkItemAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(DriveLinkItem item);
    }

    public interface OnItemDeleteClickListener {
        void onItemDeleteClick(DriveLinkItem item);
    }

    private List<DriveLinkItem> driveLinkItemList;
    private final OnItemClickListener clickListener;
    private final OnItemDeleteClickListener deleteClickListener;

    public DriveLinkItemAdapter(List<DriveLinkItem> driveLinkItemList,
                                OnItemClickListener clickListener,
                                OnItemDeleteClickListener deleteClickListener) {
        this.driveLinkItemList = driveLinkItemList;
        this.clickListener = clickListener;
        this.deleteClickListener = deleteClickListener;
    }

    @NonNull
    @Override
    public DriveLinkItemAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_adm_drive_link, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull DriveLinkItemAdapter.ViewHolder holder, int position) {
        DriveLinkItem item = driveLinkItemList.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvUrl.setText(item.getUrl());

        holder.itemView.setOnClickListener(v -> clickListener.onItemClick(item));
        holder.btnDelete.setOnClickListener(v -> deleteClickListener.onItemDeleteClick(item));
    }

    @Override
    public int getItemCount() {
        return driveLinkItemList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvUrl;
        ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvDriveTitle);
            tvUrl = itemView.findViewById(R.id.tvDriveUrl);
            btnDelete = itemView.findViewById(R.id.btnDeleteDriveLink);
        }
    }
}
