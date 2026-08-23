package com.tanim.ccepedia;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class SemesterAdapter extends RecyclerView.Adapter<SemesterAdapter.SemesterViewHolder> {

    private final List<Semester> semesterList;
    private final OnSemesterClickListener clickListener;
    private Context context;

    public interface OnSemesterClickListener {
        void onSemesterClick(String semesterId);
    }

    public SemesterAdapter(List<Semester> semesterList, OnSemesterClickListener clickListener) {
        this.semesterList = semesterList;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public SemesterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_semester_card, parent, false);
        return new SemesterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SemesterViewHolder holder, int position) {
        Semester semester = semesterList.get(position);
        holder.bind(semester, clickListener);
    }

    @Override
    public int getItemCount() {
        return semesterList.size();
    }

    public class SemesterViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView, numberCard;
        TextView tvNumber;
        TextView tvStatusTag;
        TextView tvTitle;

        public SemesterViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardSemester);
            numberCard = itemView.findViewById(R.id.cv_number_container);
            tvNumber = itemView.findViewById(R.id.tv_semester_number);
            tvStatusTag = itemView.findViewById(R.id.tv_status_tag);
            tvTitle = itemView.findViewById(R.id.tv_semester_title);
        }

        public void bind(Semester semester, OnSemesterClickListener listener) {
            tvNumber.setText(semester.getNumber());
            tvStatusTag.setText(semester.getStatus());
            tvTitle.setText(semester.getTitle());

            cardView.setOnClickListener(v -> listener.onSemesterClick(semester.getId()));

            applyStatusColors(semester.getStatus());
        }

        private void applyStatusColors(String status) {
            Context ctx = itemView.getContext();
            int numberCardColor;
            int numberTextColor;

            switch (status) {
                case "Completed":
                    numberCardColor = ContextCompat.getColor(ctx, R.color.GreenLight);
                    numberTextColor = ContextCompat.getColor(ctx, R.color.GreenDark);
                    break;
                case "Current":
                    numberCardColor = ContextCompat.getColor(ctx, R.color.BlueLight);
                    numberTextColor = ContextCompat.getColor(ctx, R.color.Blue);
                    break;
                case "Upcoming":
                default:
                    numberCardColor = ContextCompat.getColor(ctx, R.color.surface_variant);
                    numberTextColor = ContextCompat.getColor(ctx, R.color.textSecondary);
                    break;
            }

            numberCard.setCardBackgroundColor(numberCardColor);
            tvNumber.setTextColor(numberTextColor);

            tvStatusTag.setTextColor(numberTextColor);
            if (tvStatusTag.getBackground() != null) {
                // mutate() so each tag tints independently (the shape drawable is shared).
                tvStatusTag.getBackground().mutate().setTint(numberCardColor);
            }
        }
    }
}