package com.tanim.ccepedia;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    private final List<FileItem> displayList;
    private final List<FileItem> fullList;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(FileItem item);
        void onDeleteClick(FileItem item);
    }

    public FileAdapter(List<FileItem> initialFiles, OnItemClickListener listener) {
        this.fullList = new ArrayList<>(initialFiles);
        this.displayList = new ArrayList<>(initialFiles);
        this.listener = listener;
    }

    public void updateData(List<FileItem> newFiles) {
        this.fullList.clear();
        this.fullList.addAll(newFiles);
        filterList("");
    }

    public void filterList(String query) {
        displayList.clear();
        if (query.isEmpty()) {
            displayList.addAll(fullList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (FileItem item : fullList) {
                if (item.getFileName().toLowerCase().contains(lowerCaseQuery) ||
                        item.getUploader().toLowerCase().contains(lowerCaseQuery)) {
                    displayList.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        FileItem item = displayList.get(position);
        holder.tvFileName.setText(item.getFileName());

        String uploaderText = item.getUploader() != null && !item.getUploader().isEmpty()
                ? "Contributed by " + item.getUploader()
                : "Public Resource";
        holder.tvUploader.setText(uploaderText);

        // Delete button visibility logic
        String userRole = UserData.getInstance().getRole();
        String currentUserId = UserData.getInstance().getStudentId();

        boolean canDelete = false;
        if ("admin".equalsIgnoreCase(userRole)) {
            canDelete = true;
        } else if ("moderator".equalsIgnoreCase(userRole)) {
            if (currentUserId != null && item.getUploaderStudentId() != null &&
                    currentUserId.equalsIgnoreCase(item.getUploaderStudentId())) {
                canDelete = true;
            }
        }

        holder.btnDelete.setVisibility(canDelete ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(item));
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    public static class FileViewHolder extends RecyclerView.ViewHolder {
        TextView tvFileName;
        TextView tvUploader;
        ImageView ivFileIcon;
        ImageButton btnDelete;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tv_file_name);
            tvUploader = itemView.findViewById(R.id.tv_uploader);
            ivFileIcon = itemView.findViewById(R.id.iv_file_icon);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}