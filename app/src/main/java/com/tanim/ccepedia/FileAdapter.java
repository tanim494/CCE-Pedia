package com.tanim.ccepedia;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    private List<FileItem> fileList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(FileItem item);
        void onDeleteClick(FileItem item);  // New callback for delete
    }

    public FileAdapter(List<FileItem> fileList, OnItemClickListener listener) {
        this.fileList = fileList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        FileItem item = fileList.get(position);
        holder.fileName.setText(item.getFileName());
        holder.uploaderName.setText("Uploaded by: " + item.getUploader());

        // You can use a fixed PDF icon, or load thumbnails if available
        holder.fileThumbnail.setImageResource(R.drawable.ic_pdf);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });

        String currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        boolean isAdmin = UserData.getInstance().getRole().equalsIgnoreCase("admin");

        // Show delete button only if current user is uploader or admin
        if (isAdmin || (currentUserEmail != null && currentUserEmail.equalsIgnoreCase(item.getUploader()))) {
            holder.deleteButton.setVisibility(View.VISIBLE);
        } else {
            holder.deleteButton.setVisibility(View.GONE);
        }

        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    public static class FileViewHolder extends RecyclerView.ViewHolder {
        TextView fileName, uploaderName;
        ImageView fileThumbnail;
        ImageButton deleteButton;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.fileName);
            uploaderName = itemView.findViewById(R.id.uploaderName);
            fileThumbnail = itemView.findViewById(R.id.fileThumbnail);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}
