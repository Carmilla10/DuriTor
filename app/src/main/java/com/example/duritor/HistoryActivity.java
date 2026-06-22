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
import com.google.android.material.button.MaterialButton;
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
    private HistoryAdapter adapter;

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

        historyRef = FirebaseDatabase.getInstance().getReference("fallEvents");
        loadHistory();
    }

    private void loadHistory() {
        historyRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                events.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    String eventId = child.getKey();
                    if (eventId == null) {
                        continue;
                    }

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

                adapter.notifyDataSetChanged();
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

    private void collectEvent(FallEvent event) {
        if (event.collected) {
            Toast.makeText(this, R.string.history_already_collected, Toast.LENGTH_SHORT).show();
            return;
        }

        String collectedAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());

        Map<String, Object> updates = new HashMap<>();
        updates.put("collected", true);
        updates.put("collectedAt", collectedAt);

        historyRef.child(event.id).updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(HistoryActivity.this, R.string.history_collected_success, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(HistoryActivity.this, "Could not mark as collected", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDeleteEvent(FallEvent event) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.history_delete_title)
                .setMessage(R.string.history_delete_message)
                .setPositiveButton(R.string.history_delete, (dialog, which) -> deleteEvent(event))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void deleteEvent(FallEvent event) {
        Intent intent = new Intent("com.example.duritor.DELETE_EVENT");
        sendBroadcast(intent);

        historyRef.child(event.id).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(HistoryActivity.this, R.string.history_delete_success, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(HistoryActivity.this, "Could not delete event", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class HistoryAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return events.isEmpty() ? 1 : events.size();
        }

        @Override
        public Object getItem(int position) {
            return events.isEmpty() ? null : events.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (events.isEmpty()) {
                TextView emptyView = new TextView(HistoryActivity.this);
                emptyView.setText(R.string.history_empty);
                emptyView.setTextColor(ContextCompat.getColor(HistoryActivity.this, R.color.text_secondary));
                emptyView.setPadding(0, 24, 0, 24);
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

            FallEvent event = events.get(position);
            holder.alertText.setText("🚨 " + event.alert);
            holder.dateText.setText("📅 " + event.date);
            holder.timeText.setText("🕒 " + event.time);
            holder.orchardText.setText("🌳 " + event.orchard);
            holder.regionText.setText("📍 " + event.region);
            holder.treeText.setText("🌴 " + event.tree);

            if (event.collected) {
                holder.statusText.setText("✅ " + getString(R.string.history_status_collected));
                holder.statusText.setTextColor(ContextCompat.getColor(HistoryActivity.this, R.color.primary));
                holder.collectButton.setEnabled(false);
                holder.collectButton.setAlpha(0.5f);
            } else {
                holder.statusText.setText("⏳ " + getString(R.string.history_status_pending));
                holder.statusText.setTextColor(ContextCompat.getColor(HistoryActivity.this, R.color.text_secondary));
                holder.collectButton.setEnabled(true);
                holder.collectButton.setAlpha(1f);
            }

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

            holder.collectButton.setOnClickListener(v -> collectEvent(event));
            holder.deleteButton.setOnClickListener(v -> confirmDeleteEvent(event));

            return convertView;
        }
    }

    private static class ViewHolder {
        final TextView alertText;
        final TextView dateText;
        final TextView timeText;
        final TextView orchardText;
        final TextView regionText;
        final TextView treeText;
        final ImageView historyImage;
        final TextView statusText;
        final MaterialButton collectButton;
        final MaterialButton deleteButton;

        ViewHolder(View view) {
            alertText = view.findViewById(R.id.historyAlert);
            dateText = view.findViewById(R.id.historyDate);
            timeText = view.findViewById(R.id.historyTime);
            orchardText = view.findViewById(R.id.historyOrchard);
            regionText = view.findViewById(R.id.historyRegion);
            treeText = view.findViewById(R.id.historyTree);
            historyImage = view.findViewById(R.id.historyImage);
            statusText = view.findViewById(R.id.historyStatus);
            collectButton = view.findViewById(R.id.itemCollectButton);
            deleteButton = view.findViewById(R.id.itemDeleteButton);
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