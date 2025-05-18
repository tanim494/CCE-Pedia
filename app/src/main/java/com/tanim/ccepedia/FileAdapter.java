package com.tanim.ccepedia;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    private Context context;
    private List<FileItem> fileList;
    private OnItemClickListener listener;

    // Interface for click callback
    public interface OnItemClickListener {
        void onItemClick(FileItem item);
    }

    public FileAdapter(Context context, List<FileItem> fileList, OnItemClickListener listener) {
        this.context = context;
        this.fileList = fileList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        FileItem item = fileList.get(position);
        holder.fileName.setText(item.getFileName());
        holder.uploaderName.setText("Uploaded by: " + item.getUploader());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
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

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.fileName);
            uploaderName = itemView.findViewById(R.id.uploaderName);
            fileThumbnail = itemView.findViewById(R.id.fileThumbnail);
        }
    }
}
