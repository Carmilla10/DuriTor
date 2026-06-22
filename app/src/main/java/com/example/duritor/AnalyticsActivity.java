package com.example.duritor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AnalyticsActivity extends DrawerActivity {

    private TextView totalFallsText;
    private TextView todayFallsText;
    private TextView collectedText;
    private TextView pendingText;
    private TextView collectionRateText;
    private TextView farmSummaryText;
    private LinearLayout orchardBarsContainer;
    private LinearLayout treeBarsContainer;
    private TextView orchardBarsEmpty;
    private TextView treeBarsEmpty;

    private int orchardCount;
    private int treeCount;
    private boolean orchardsLoaded;
    private boolean treesLoaded;
    private boolean fallsLoaded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupDrawerShell(
                R.layout.activity_analytics,
                R.id.nav_analytics,
                R.string.title_analytics
        );

        totalFallsText = findViewById(R.id.totalFallsText);
        todayFallsText = findViewById(R.id.todayFallsText);
        collectedText = findViewById(R.id.collectedText);
        pendingText = findViewById(R.id.pendingText);
        collectionRateText = findViewById(R.id.collectionRateText);
        farmSummaryText = findViewById(R.id.farmSummaryText);
        orchardBarsContainer = findViewById(R.id.orchardBarsContainer);
        treeBarsContainer = findViewById(R.id.treeBarsContainer);
        orchardBarsEmpty = findViewById(R.id.orchardBarsEmpty);
        treeBarsEmpty = findViewById(R.id.treeBarsEmpty);

        loadAnalytics();
    }

    private void loadAnalytics() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        database.getReference("orchards").addListenerForSingleValueEvent(new SimpleCounterListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                orchardCount = (int) snapshot.getChildrenCount();
                orchardsLoaded = true;
                maybeUpdateSummary();
            }
        });

        database.getReference("trees").addListenerForSingleValueEvent(new SimpleCounterListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                treeCount = (int) snapshot.getChildrenCount();
                treesLoaded = true;
                maybeUpdateSummary();
            }
        });

        database.getReference("fallEvents").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                int total = 0;
                int todayCount = 0;
                int collected = 0;
                int pending = 0;

                Map<String, Integer> orchardFalls = new HashMap<>();
                Map<String, Integer> treeFalls = new HashMap<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    total++;

                    String date = child.child("date").getValue(String.class);
                    if (date != null && date.contains(today)) {
                        todayCount++;
                    }

                    Boolean isCollected = child.child("collected").getValue(Boolean.class);
                    if (isCollected != null && isCollected) {
                        collected++;
                    } else {
                        pending++;
                    }

                    String orchard = child.child("orchardName").getValue(String.class);
                    if (orchard == null || orchard.isEmpty()) {
                        orchard = "Unknown orchard";
                    }
                    orchardFalls.put(orchard, orchardFalls.getOrDefault(orchard, 0) + 1);

                    String tree = child.child("treeName").getValue(String.class);
                    if (tree == null || tree.isEmpty()) {
                        tree = "Unknown tree";
                    }
                    treeFalls.put(tree, treeFalls.getOrDefault(tree, 0) + 1);
                }

                totalFallsText.setText(String.valueOf(total));
                todayFallsText.setText(String.valueOf(todayCount));
                collectedText.setText(String.valueOf(collected));
                pendingText.setText(String.valueOf(pending));

                int rate = total == 0 ? 0 : Math.round((collected * 100f) / total);
                collectionRateText.setText(rate + "%");

                renderBars(orchardBarsContainer, orchardBarsEmpty, orchardFalls);
                renderBars(treeBarsContainer, treeBarsEmpty, treeFalls);

                fallsLoaded = true;
                maybeUpdateSummary();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AnalyticsActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void maybeUpdateSummary() {
        if (!orchardsLoaded || !treesLoaded || !fallsLoaded) {
            return;
        }
        farmSummaryText.setText(getString(
                R.string.analytics_farm_summary,
                orchardCount,
                treeCount
        ));
    }

    private void renderBars(LinearLayout container, TextView emptyView, Map<String, Integer> counts) {
        container.removeAllViews();

        if (counts.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            return;
        }

        emptyView.setVisibility(View.GONE);

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(counts.entrySet());
        Collections.sort(entries, (a, b) -> Integer.compare(b.getValue(), a.getValue()));

        int max = entries.get(0).getValue();
        int limit = Math.min(entries.size(), 5);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < limit; i++) {
            Map.Entry<String, Integer> entry = entries.get(i);
            View row = inflater.inflate(R.layout.analytics_bar_row, container, false);

            TextView label = row.findViewById(R.id.barLabel);
            TextView value = row.findViewById(R.id.barValue);
            View fill = row.findViewById(R.id.barFill);

            label.setText(entry.getKey());
            value.setText(String.valueOf(entry.getValue()));

            ViewGroup parent = (ViewGroup) fill.getParent();
            parent.post(() -> {
                int width = parent.getWidth();
                int barWidth = max == 0 ? 0 : (int) (width * (entry.getValue() / (float) max));
                ViewGroup.LayoutParams params = fill.getLayoutParams();
                params.width = barWidth;
                fill.setLayoutParams(params);
            });

            container.addView(row);
        }
    }

    private abstract static class SimpleCounterListener implements ValueEventListener {
        @Override
        public void onCancelled(@NonNull DatabaseError error) {
        }
    }
}
