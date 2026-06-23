package com.example.duritor;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class RegionListActivity extends DrawerActivity {

    private ListView regionListView;
    private EditText searchEditText;
    private FloatingActionButton addRegionButton;

    private DatabaseReference regionRef;
    private List<RegionItem> masterRegionList;
    private List<RegionItem> filteredRegionList;
    private RegionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupDrawerShell(R.layout.activity_region_list, R.id.nav_regions, R.string.title_regions);

        regionListView = findViewById(R.id.regionListView);
        searchEditText = findViewById(R.id.searchEditText);
        addRegionButton = findViewById(R.id.addRegionButton);

        masterRegionList = new ArrayList<>();
        filteredRegionList = new ArrayList<>();
        adapter = new RegionAdapter();
        regionListView.setAdapter(adapter);

        regionRef = FirebaseDatabase.getInstance().getReference("regions");

        addRegionButton.setOnClickListener(v ->
                startActivity(new Intent(RegionListActivity.this, RegionFormActivity.class))
        );

        loadRegions();

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadRegions() {
        regionRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                masterRegionList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String id = child.getKey();
                    String name = child.child("name").getValue(String.class);
                    String description = child.child("description").getValue(String.class);
                    String orchardName = child.child("orchardName").getValue(String.class);

                    RegionItem item = new RegionItem();
                    item.id = id;
                    item.name = name != null ? name : "Unnamed Region";
                    item.description = description != null ? description : "";
                    item.orchardName = orchardName != null ? orchardName : "";
                    item.treeCount = 0;

                    masterRegionList.add(item);
                }
                filterList(searchEditText.getText().toString());
            }
            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(RegionListActivity.this, "Failed to load regions", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void filterList(String query) {
        filteredRegionList.clear();
        if (query.isEmpty()) {
            filteredRegionList.addAll(masterRegionList);
        } else {
            for (RegionItem item : masterRegionList) {
                if (item.name.toLowerCase().contains(query.toLowerCase()) ||
                        item.description.toLowerCase().contains(query.toLowerCase()) ||
                        item.orchardName.toLowerCase().contains(query.toLowerCase())) {
                    filteredRegionList.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void confirmDelete(String regionId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Region")
                .setMessage("Are you sure you want to delete this region?")
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

    private class RegionAdapter extends BaseAdapter {
        @Override
        public int getCount() { return filteredRegionList.size(); }
        @Override
        public Object getItem(int position) { return filteredRegionList.get(position); }
        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(RegionListActivity.this)
                        .inflate(R.layout.region_item, parent, false);
            }

            RegionItem item = filteredRegionList.get(position);

            TextView nameText = convertView.findViewById(R.id.regionNameText);
            TextView descText = convertView.findViewById(R.id.regionDescriptionText);
            TextView treeText = convertView.findViewById(R.id.regionTreeCount);

            // Buttons
            TextView editBtn = convertView.findViewById(R.id.editButton);
            TextView deleteBtn = convertView.findViewById(R.id.deleteButton);

            nameText.setText(item.name);
            descText.setText(item.description);
            treeText.setText("🌳 " + item.treeCount + " Trees");

            // Edit Button
            editBtn.setOnClickListener(v -> {
                Intent intent = new Intent(RegionListActivity.this, RegionFormActivity.class);
                intent.putExtra("regionId", item.id);
                startActivity(intent);
            });

            // Delete Button
            deleteBtn.setOnClickListener(v -> confirmDelete(item.id));

            return convertView;
        }
    }

    private static class RegionItem {
        String id;
        String name;
        String description;
        String orchardName;
        int treeCount;
    }
}