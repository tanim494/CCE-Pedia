package com.tanim.ccepedia;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class FacultyAdapter extends ArrayAdapter<FacultyModel> {

    public FacultyAdapter(@NonNull Context context, ArrayList<FacultyModel> facultyList) {
        super(context, 0, facultyList);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.faculty_item, parent, false);
        }

        FacultyModel faculty = getItem(position);

        ImageView facultyImage = view.findViewById(R.id.facultyImage);
        TextView nameText = view.findViewById(R.id.fName);
        TextView designationText = view.findViewById(R.id.fDesignation);
        TextView phoneText = view.findViewById(R.id.fPhoneNumber);

        nameText.setText(faculty.getName());
        designationText.setText(faculty.getDesignation());
        phoneText.setText(faculty.getPhone());

        Glide.with(getContext())
                .load(faculty.getImageUrl())
                .placeholder(R.drawable.teacher) // fallback
                .into(facultyImage);

        return view;
    }
}
