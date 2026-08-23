package com.tanim.ccepedia;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.List;

/**
 * Pages the bus schedule image(s) inside a ViewPager2. Each page shows one full image
 * (Regular / Friday) with its own loading spinner that clears once Glide finishes.
 */
public class BusSchedulePagerAdapter extends RecyclerView.Adapter<BusSchedulePagerAdapter.PageViewHolder> {

    private final Context context;
    private final List<String> imageUrls;

    public BusSchedulePagerAdapter(Context context, List<String> imageUrls) {
        this.context = context;
        this.imageUrls = imageUrls;
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_bus_schedule_page, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        holder.pageSpinner.setVisibility(View.VISIBLE);
        Glide.with(context)
                .load(imageUrls.get(position))
                .fitCenter()
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        holder.pageSpinner.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        holder.pageSpinner.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(holder.pageImage);
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        ImageView pageImage;
        ProgressBar pageSpinner;

        PageViewHolder(@NonNull View itemView) {
            super(itemView);
            pageImage = itemView.findViewById(R.id.pageImage);
            pageSpinner = itemView.findViewById(R.id.pageSpinner);
        }
    }
}
