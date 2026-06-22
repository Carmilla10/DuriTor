package com.example.duritor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegionListActivity extends DrawerActivity {

    private Button addRegionButton;
    private ListView regionListView;
    private DatabaseReference regionRef;
    private List<String> regionIds;
    private List<String> regionDisplay;
    private Map<String, Map<String, Object>> regionMap;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupDrawerShell(R.layout.activity_region_list, R.id.nav_regions, R.string.title_regions);

        addRegionButton = findViewById(R.id.addRegionButton);
        regionListView = findViewById(R.id.regionListView);

        regionIds = new ArrayList<>();
        regionDisplay = new ArrayList<>();
        regionMap = new HashMap<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, regionDisplay);
        regionListView.setAdapter(adapter);

        regionRef = FirebaseDatabase.getInstance().getReference("regions");

        addRegionButton.setOnClickListener(v -> startActivity(new Intent(RegionListActivity.this, RegionFormActivity.class)));

        regionListView.setOnItemClickListener((parent, view, position, id) -> showOptionsDialog(position));

        loadRegions();
    }

    private void loadRegions() {
        regionRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                regionIds.clear();
                regionDisplay.clear();
                regionMap.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    String regionId = child.getKey();
                    if (regionId == null) continue;
                    String name = child.child("name").getValue(String.class);
                    String orchardName = child.child("orchardName").getValue(String.class);
                    if (name == null || name.isEmpty()) {
                        name = "Unnamed Region";
                    }
                    String displayText = name;
                    if (orchardName != null && !orchardName.isEmpty()) {
                        displayText += " (" + orchardName + ")";
                    }
                    regionIds.add(regionId);
                    regionDisplay.add(displayText);
                    Map<String, Object> region = new HashMap<>();
                    region.put("name", name);
                    region.put("orchardId", child.child("orchardId").getValue(String.class));
                    region.put("orchardName", orchardName != null ? orchardName : "");
                    region.put("description", child.child("description").getValue(String.class));
                    regionMap.put(regionId, region);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(RegionListActivity.this, "Failed to load regions: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showOptionsDialog(int position) {
        String regionId = regionIds.get(position);
        String regionName = regionDisplay.get(position);

        new AlertDialog.Builder(this)
                .setTitle(regionName)
                .setItems(new CharSequence[]{"Edit", "Delete"}, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(RegionListActivity.this, RegionFormActivity.class);
                        intent.putExtra("regionId", regionId);
                        startActivity(intent);
                    } else {
                        confirmDelete(regionId);
                    }
                })
                .show();
    }

    private void confirmDelete(String regionId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Region")
                .setMessage("Remove this region and keep associated trees?")
                .setPositiveButton("Delete", (dialog, which) -> deleteRegion(regionId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteRegion(String regionId) {
        regionRef.child(regionId).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(RegionListActivity.this, "Region deleted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(RegionListActivity.this, "Delete failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
