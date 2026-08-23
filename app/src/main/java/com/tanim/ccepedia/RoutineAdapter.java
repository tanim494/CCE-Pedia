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

/**
 * Lists the student's routine entries grouped by day with collapsible sections.
 * Two view types: TYPE_HEADER (day label + expand/collapse) and TYPE_ENTRY (class row).
 */
public class RoutineAdapter extends RecyclerView.Adapter<RoutineAdapter.ViewHolder> {

    public interface Listener {
        void onEdit(int index);
        void onDelete(int index);
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ENTRY = 1;

    private final List<RoutineEntry> items;
    private final Listener listener;
    private final boolean[] expanded = new boolean[RoutineStore.DAY_NAMES.length];

    // Maps flat adapter position -> (dayIndex, entryIndexInDay)
    private final List<PositionMap> positionMap = new ArrayList<>();

    public RoutineAdapter(List<RoutineEntry> items, Listener listener) {
        this.items = items;
        this.listener = listener;
        // Default all days expanded
        for (int i = 0; i < expanded.length; i++) expanded[i] = true;
        rebuildPositionMap();
    }

    /** Called when the underlying entries list changes. */
    public void setItems(List<RoutineEntry> newItems) {
        if (items == newItems) {
            // Same list reference (activity passes its own list) — just rebuild map
            rebuildPositionMap();
            notifyDataSetChanged();
            return;
        }
        items.clear();
        items.addAll(newItems);
        rebuildPositionMap();
        notifyDataSetChanged();
    }

    /** Toggle expand/collapse for a day. */
    public void toggleDay(int dayIndex) {
        if (dayIndex < 0 || dayIndex >= expanded.length) return;
        expanded[dayIndex] = !expanded[dayIndex];
        rebuildPositionMap();
        notifyDataSetChanged();
    }

    /** Rebuilds the flat position map from the grouped entries. */
    private void rebuildPositionMap() {
        positionMap.clear();
        for (int day = 0; day < RoutineStore.DAY_NAMES.length; day++) {
            // Header always present
            positionMap.add(new PositionMap(day, -1, true));

            if (!expanded[day]) continue;

            // Entries for this day
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getDay() == day) {
                    positionMap.add(new PositionMap(day, i, false));
                }
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= 0 && position < positionMap.size()) {
            return positionMap.get(position).isHeader ? TYPE_HEADER : TYPE_ENTRY;
        }
        return TYPE_ENTRY;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = (viewType == TYPE_HEADER)
                ? R.layout.item_routine_day_header
                : R.layout.item_routine_entry;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new ViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position < 0 || position >= positionMap.size()) return;

        PositionMap map = positionMap.get(position);
        if (map.isHeader) {
            bindHeader(holder, map.day);
        } else {
            bindEntry(holder, map.entryIndex);
        }
    }

    private void bindHeader(ViewHolder holder, int day) {
        holder.dayHeader.setText(RoutineStore.dayName(day));
        int count = 0;
        for (RoutineEntry e : items) if (e.getDay() == day) count++;
        holder.dayCount.setText(count == 1 ? "1 class" : count + " classes");
        holder.expandIcon.setRotation(expanded[day] ? 180f : 0f);

        holder.itemView.setOnClickListener(v -> {
            if (day >= 0 && day < expanded.length) {
                toggleDay(day);
            }
        });
    }

    private void bindEntry(ViewHolder holder, int entryIndex) {
        RoutineEntry entry = items.get(entryIndex);
        holder.day.setText(RoutineStore.dayName(entry.getDay()));
        String time = RoutineStore.startMinutes(entry.getEnd()) >= 0
                ? RoutineStore.format12(entry.getStart()) + " – " + RoutineStore.format12(entry.getEnd())
                : RoutineStore.format12(entry.getStart());
        holder.detail.setText(time + " · " + entry.getCourse());

        holder.itemView.setOnClickListener(v -> {
            if (entryIndex != RecyclerView.NO_POSITION) listener.onEdit(entryIndex);
        });
        holder.delete.setOnClickListener(v -> {
            if (entryIndex != RecyclerView.NO_POSITION) listener.onDelete(entryIndex);
        });
    }

    @Override
    public int getItemCount() {
        return positionMap.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        // Header views
        final TextView dayHeader;
        final TextView dayCount;
        final ImageView expandIcon;
        // Entry views
        final TextView day;
        final TextView detail;
        final ImageButton delete;

        ViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            if (viewType == TYPE_HEADER) {
                dayHeader = itemView.findViewById(R.id.tv_day_header);
                dayCount = itemView.findViewById(R.id.tv_day_count);
                expandIcon = itemView.findViewById(R.id.iv_expand);
                day = null;
                detail = null;
                delete = null;
            } else {
                day = itemView.findViewById(R.id.tv_routine_day);
                detail = itemView.findViewById(R.id.tv_routine_detail);
                delete = itemView.findViewById(R.id.btn_routine_delete);
                dayHeader = null;
                dayCount = null;
                expandIcon = null;
            }
        }
    }

    private static class PositionMap {
        final int day;
        final int entryIndex;
        final boolean isHeader;

        PositionMap(int day, int entryIndex, boolean isHeader) {
            this.day = day;
            this.entryIndex = entryIndex;
            this.isHeader = isHeader;
        }
    }
}