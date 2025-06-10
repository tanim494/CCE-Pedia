package com.tanim.ccepedia;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.UserViewHolder> {

    private List<UserListModel> users;
    private Context context;

    public UserListAdapter(Context context, List<UserListModel> users) {
        this.context = context;
        this.users = users;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_list, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserListModel user = users.get(position);
        holder.tvName.setText(user.getName());
        holder.tvStudentId.setText(user.getId());
        holder.tvSemester.setText(user.getSemester());
        holder.tvGender.setText(user.getGender());
        holder.tvRole.setText(user.getRole() == null ? "User" : user.getRole());

        holder.tvEmail.setText(user.getEmail() != null ? user.getEmail() : "No Email");
        holder.tvPhone.setText(user.getPhone() != null ? user.getPhone() : "No Phone");

        // Handle Verified Status
        if (user.isVerified()) {
            holder.tvUserVerified.setText("Verified");
            holder.tvUserVerified.setTextColor(ContextCompat.getColor(context, R.color.Green));
        } else {
            holder.tvUserVerified.setText("Non Verified");
            holder.tvUserVerified.setTextColor(ContextCompat.getColor(context, R.color.Red));
        }

        holder.itemView.setOnClickListener(v -> {
            showRoleChangeDialog(user);
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    private void showRoleChangeDialog(UserListModel user) {
        String[] roles = {"admin", "moderator", "user"};

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Change Role for " + user.getName());
        builder.setSingleChoiceItems(roles, getRolePosition(user.getRole(), roles), (dialog, which) -> {
            String selectedRole = roles[which].equals("user") ? null : roles[which];
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .whereEqualTo("studentid", user.getId())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            String docId = queryDocumentSnapshots.getDocuments().get(0).getId();
                            FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(docId)
                                    .update("role", selectedRole)
                                    .addOnSuccessListener(unused -> {
                                        user.setRole(selectedRole);
                                        notifyDataSetChanged();
                                    });
                        }
                    });
            dialog.dismiss();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private int getRolePosition(String role, String[] roles) {
        if (role == null) return 2; // user
        for (int i = 0; i < roles.length; i++) {
            if (roles[i].equalsIgnoreCase(role)) return i;
        }
        return 2;
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvStudentId, tvSemester, tvGender, tvRole, tvEmail, tvPhone, tvUserVerified;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvStudentId = itemView.findViewById(R.id.tvStudentId);
            tvSemester = itemView.findViewById(R.id.tvSemester);
            tvGender = itemView.findViewById(R.id.tvGender);
            tvRole = itemView.findViewById(R.id.tvRole);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvUserVerified = itemView.findViewById(R.id.tvUserVerified);
        }
    }
}
