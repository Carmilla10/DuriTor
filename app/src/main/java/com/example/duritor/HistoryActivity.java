package com.example.duritor;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistoryActivity extends DrawerActivity {

    private ListView historyListView;
    private DatabaseReference historyRef;
    private final List<FallEvent> events = new ArrayList<>();
    private final List<FallEvent> filteredEvents = new ArrayList<>();
    private HistoryAdapter adapter;
    private TextView chipAll, chipPending, chipCollected;
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupDrawerShell(
                R.layout.activity_history,
                R.id.nav_history,
                R.string.title_history
        );

        historyListView = findViewById(R.id.historyListView);
        adapter = new HistoryAdapter();
        historyListView.setAdapter(adapter);

        // Filter chips
        chipAll = findViewById(R.id.chipAll);
        chipPending = findViewById(R.id.chipPending);
        chipCollected = findViewById(R.id.chipCollected);

        chipAll.setOnClickListener(v -> {
            currentFilter = "all";
            updateChips();
            applyFilter();
        });

        chipPending.setOnClickListener(v -> {
            currentFilter = "pending";
            updateChips();
            applyFilter();
        });

        chipCollected.setOnClickListener(v -> {
            currentFilter = "collected";
            updateChips();
            applyFilter();
        });

        historyRef = FirebaseDatabase.getInstance().getReference("fallEvents");
        loadHistory();
    }

    private void updateChips() {
        chipAll.setBackgroundResource(R.drawable.bg_chip_inactive);
        chipPending.setBackgroundResource(R.drawable.bg_chip_inactive);
        chipCollected.setBackgroundResource(R.drawable.bg_chip_inactive);

        chipAll.setTextColor(getColor(R.color.text_primary));
        chipPending.setTextColor(getColor(R.color.text_primary));
        chipCollected.setTextColor(getColor(R.color.text_primary));

        if (currentFilter.equals("all")) {
            chipAll.setBackgroundResource(R.drawable.bg_chip_active);
            chipAll.setTextColor(getColor(R.color.white));
        } else if (currentFilter.equals("pending")) {
            chipPending.setBackgroundResource(R.drawable.bg_chip_active);
            chipPending.setTextColor(getColor(R.color.white));
        } else if (currentFilter.equals("collected")) {
            chipCollected.setBackgroundResource(R.drawable.bg_chip_active);
            chipCollected.setTextColor(getColor(R.color.white));
        }
    }

    private void applyFilter() {
        filteredEvents.clear();
        for (FallEvent event : events) {
            if (currentFilter.equals("all")) {
                filteredEvents.add(event);
            } else if (currentFilter.equals("pending") && !event.collected) {
                filteredEvents.add(event);
            } else if (currentFilter.equals("collected") && event.collected) {
                filteredEvents.add(event);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void loadHistory() {
        historyRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                events.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    String eventId = child.getKey();
                    if (eventId == null) continue;

                    FallEvent event = new FallEvent();
                    event.id = eventId;
                    event.alert = valueOrDefault(child.child("alert").getValue(String.class), "Durian Fall Detected!");
                    event.date = valueOrDefault(child.child("date").getValue(String.class), "-");
                    event.time = valueOrDefault(child.child("time").getValue(String.class), "-");
                    event.orchard = valueOrDefault(child.child("orchardName").getValue(String.class), "Unknown Orchard");
                    event.region = valueOrDefault(child.child("regionName").getValue(String.class), "Unknown Region");
                    event.tree = valueOrDefault(child.child("treeName").getValue(String.class),
                            valueOrDefault(child.child("treeId").getValue(String.class), "Unknown Tree"));
                    event.photoUrl = child.child("photoUrl").getValue(String.class);
                    Boolean collected = child.child("collected").getValue(Boolean.class);
                    event.collected = collected != null && collected;
                    events.add(event);
                }

                // Sort by date/time (newest first)
                events.sort((e1, e2) -> {
                    String d1 = e1.date + " " + e1.time;
                    String d2 = e2.date + " " + e2.time;
                    return d2.compareTo(d1);
                });

                applyFilter();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HistoryActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String valueOrDefault(String value, String fallback) {
        return (value != null && !value.isEmpty()) ? value : fallback;
    }

    private String formatDate(String date) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
            Date parsedDate = inputFormat.parse(date);
            if (parsedDate != null) {
                return outputFormat.format(parsedDate);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return date;
    }

    private void collectEvent(FallEvent event) {
        if (event.collected) {
            Toast.makeText(this, "Already collected", Toast.LENGTH_SHORT).show();
            return;
        }

        String collectedAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());

        Map<String, Object> updates = new HashMap<>();
        updates.put("collected", true);
        updates.put("collectedAt", collectedAt);

        historyRef.child(event.id).updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(HistoryActivity.this, "✅ Marked as collected!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(HistoryActivity.this, "Failed to mark as collected", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDeleteEvent(FallEvent event) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete this event?")
                .setPositiveButton("Delete", (dialog, which) -> deleteEvent(event))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteEvent(FallEvent event) {
        historyRef.child(event.id).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(HistoryActivity.this, "🗑️ Event deleted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(HistoryActivity.this, "Failed to delete event", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class HistoryAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return filteredEvents.isEmpty() ? 1 : filteredEvents.size();
        }

        @Override
        public Object getItem(int position) {
            return filteredEvents.isEmpty() ? null : filteredEvents.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (filteredEvents.isEmpty()) {
                TextView emptyView = new TextView(HistoryActivity.this);
                emptyView.setText("📭 No events yet");
                emptyView.setTextColor(ContextCompat.getColor(HistoryActivity.this, R.color.text_secondary));
                emptyView.setPadding(0, 40, 0, 40);
                emptyView.setTextSize(16f);
                emptyView.setGravity(View.TEXT_ALIGNMENT_CENTER);
                return emptyView;
            }

            ViewHolder holder;
            if (convertView == null || convertView.getTag() == null) {
                convertView = LayoutInflater.from(HistoryActivity.this)
                        .inflate(R.layout.history_item, parent, false);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            FallEvent event = filteredEvents.get(position);

            // Date Header
            String formattedDate = formatDate(event.date);
            holder.dateHeader.setText(formattedDate);

            // Time
            holder.timeText.setText(event.time);

            // Alert
            holder.alertText.setText(event.alert);

            // Location: Orchard • Region
            holder.locationText.setText(event.orchard + " • " + event.region);

            // Tree
            holder.treeText.setText(event.tree);

            // Collect & Delete - Text Only
            if (event.collected) {
                holder.collectButton.setText("Collected");
                holder.collectButton.setTextColor(ContextCompat.getColor(HistoryActivity.this, R.color.text_hint));
                holder.collectButton.setEnabled(false);
            } else {
                holder.collectButton.setText("Collect");
                holder.collectButton.setTextColor(ContextCompat.getColor(HistoryActivity.this, R.color.primary));
                holder.collectButton.setEnabled(true);
            }

            holder.collectButton.setOnClickListener(v -> collectEvent(event));
            holder.deleteButton.setOnClickListener(v -> confirmDeleteEvent(event));

            // Image
            if (event.photoUrl != null && !event.photoUrl.isEmpty() && !event.photoUrl.equals("null")) {
                holder.historyImage.setVisibility(View.VISIBLE);
                Glide.with(HistoryActivity.this)
                        .load(event.photoUrl)
                        .placeholder(android.R.drawable.ic_menu_camera)
                        .error(android.R.drawable.ic_menu_camera)
                        .centerCrop()
                        .into(holder.historyImage);
            } else {
                holder.historyImage.setVisibility(View.GONE);
            }

            return convertView;
        }
    }

    private static class ViewHolder {
        final TextView dateHeader;
        final TextView timeText;
        final TextView alertText;
        final TextView locationText;
        final TextView treeText;
        final TextView collectButton;
        final TextView deleteButton;
        final ImageView historyImage;

        ViewHolder(View view) {
            dateHeader = view.findViewById(R.id.dateHeader);
            timeText = view.findViewById(R.id.historyTime);
            alertText = view.findViewById(R.id.historyAlert);
            locationText = view.findViewById(R.id.historyLocation);
            treeText = view.findViewById(R.id.historyTree);
            collectButton = view.findViewById(R.id.collectButton);
            deleteButton = view.findViewById(R.id.deleteButton);
            historyImage = view.findViewById(R.id.historyImage);
        }
    }

    private static class FallEvent {
        String id;
        String alert;
        String date;
        String time;
        String orchard;
        String region;
        String tree;
        String photoUrl;
        boolean collected;
    }
}