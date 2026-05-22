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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.UserViewHolder> {

    private final Context context;
    private final List<UserListModel> userList;

    public UserListAdapter(Context context, List<UserListModel> userList) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_list, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserListModel user = userList.get(position);

        holder.tvName.setText(user.getName());
        holder.tvStudentId.setText(user.getStudentId() + " | " + user.getEmail());
        holder.tvEmail.setText(user.getEmail());
        holder.tvPhone.setText(user.getPhone());
        holder.tvSemester.setText(user.getSemester());
        holder.tvGender.setText((user.getGender() != null ? user.getGender() : "N/A"));

        String role = user.getRole();
        if (role == null || role.isEmpty()) {
            role = "user";
        }
        holder.tvRole.setText(role.toUpperCase(Locale.ROOT));

        String dept = user.getDepartmentName();
        if (dept == null || dept.isEmpty()) {
            dept = "N/A";
            holder.tvUserDepartment.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
        } else {
            holder.tvUserDepartment.setTextColor(ContextCompat.getColor(context, R.color.Green));
        }
        holder.tvUserDepartment.setText(dept);

        Date lastLoggedIn = user.getLastLoggedIn();
        if (lastLoggedIn != null) {
            SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
            String formattedDate = formatter.format(lastLoggedIn);
            holder.tvLastLoggedIn.setText("Last Seen: " + formattedDate);
        } else {
            holder.tvLastLoggedIn.setText("Last Seen: Unavailable");
        }

        holder.itemView.setOnClickListener(v -> showRoleChangeDialog(user));
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    private void showRoleChangeDialog(UserListModel user) {
        String[] roles = {"admin", "moderator", "user"};

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Change Role for " + user.getName());

        builder.setSingleChoiceItems(roles, getRolePosition(user.getRole(), roles), (dialog, which) -> {

            String selectedRole = roles[which].equals("user") ? null : roles[which];

            FirebaseFirestore.getInstance()
                    .collection("users")
                    .whereEqualTo("id", user.getStudentId())
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
                                    })
                                    .addOnFailureListener(e -> {
                                    });
                        } else {
                        }
                    })
                    .addOnFailureListener(e -> {
                    });
            dialog.dismiss();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private int getRolePosition(String role, String[] roles) {
        if (role == null) return 2;
        for (int i = 0; i < roles.length; i++) {
            if (roles[i].equalsIgnoreCase(role)) return i;
        }
        return 2;
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvStudentId, tvEmail, tvPhone, tvSemester, tvGender, tvRole, tvUserDepartment, tvLastLoggedIn;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvStudentId = itemView.findViewById(R.id.tvStudentId);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvSemester = itemView.findViewById(R.id.tvSemester);
            tvGender = itemView.findViewById(R.id.tvGender);
            tvRole = itemView.findViewById(R.id.tvRole);
            tvUserDepartment = itemView.findViewById(R.id.tvUserDepartment);
            tvLastLoggedIn = itemView.findViewById(R.id.tvLastLoggedIn);
        }
    }
}