package com.example.duritor;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
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

public class TreeListActivity extends DrawerActivity {

    private Spinner treeOrchardFilterSpinner;
    private Button addTreeButton;
    private ListView treeListView;
    private DatabaseReference orchardsRef;
    private DatabaseReference treesRef;
    private List<String> orchardIds;
    private List<String> orchardNames;
    private Map<String, String> orchardNameById;
    private List<String> treeIds;
    private List<String> treeDisplay;
    private List<Map<String, Object>> allTreeData;
    private ArrayAdapter<String> treeAdapter;
    private ArrayAdapter<String> orchardFilterAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupDrawerShell(R.layout.activity_tree_list, R.id.nav_trees, R.string.title_trees);

        treeOrchardFilterSpinner = findViewById(R.id.treeOrchardFilterSpinner);
        addTreeButton = findViewById(R.id.addTreeButton);
        treeListView = findViewById(R.id.treeListView);

        orchardsRef = FirebaseDatabase.getInstance().getReference("orchards");
        treesRef = FirebaseDatabase.getInstance().getReference("trees");

        orchardIds = new ArrayList<>();
        orchardNames = new ArrayList<>();
        orchardNameById = new HashMap<>();
        treeIds = new ArrayList<>();
        treeDisplay = new ArrayList<>();
        allTreeData = new ArrayList<>();

        treeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, treeDisplay);
        treeListView.setAdapter(treeAdapter);

        orchardFilterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, orchardNames);
        orchardFilterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        treeOrchardFilterSpinner.setAdapter(orchardFilterAdapter);

        treeOrchardFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                filterTreeList(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                filterTreeList(0);
            }
        });

        addTreeButton.setOnClickListener(v -> startActivity(new Intent(TreeListActivity.this, TreeFormActivity.class)));

        treeListView.setOnItemClickListener((parent, view, position, id) -> showTreeOptions(position));

        loadOrchards();
        loadTrees();
    }

    private void loadOrchards() {
        orchardsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                orchardIds.clear();
                orchardNames.clear();
                orchardNameById.clear();
                orchardNames.add("All Orchards");
                orchardIds.add("");

                for (DataSnapshot child : snapshot.getChildren()) {
                    String orchardId = child.getKey();
                    String name = child.child("name").getValue(String.class);
                    if (name == null || name.isEmpty()) {
                        name = "Unnamed Orchard";
                    }
                    orchardIds.add(orchardId);
                    orchardNames.add(name);
                    orchardNameById.put(orchardId, name);
                }
                orchardFilterAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(TreeListActivity.this, "Failed to load orchards", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTrees() {
        treesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                allTreeData.clear();
                treeIds.clear();
                treeDisplay.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                        String treeId = child.getKey();
                    if (treeId == null) continue;
                    String name = child.child("name").getValue(String.class);
                    String variety = child.child("durianVariety").getValue(String.class);
                    String orchardId = child.child("orchardId").getValue(String.class);
                    String orchardName = orchardNameById.getOrDefault(orchardId, "Unknown Orchard");
                    String regionName = child.child("regionName").getValue(String.class);
                    if (regionName == null || regionName.isEmpty()) {
                        regionName = "Unknown Region";
                    }
                    if (name == null) {
                        name = "Unnamed Tree";
                    }
                    if (variety == null || variety.isEmpty()) {
                        variety = "Unknown Variety";
                    }
                    String display = name + " - " + variety + " (" + orchardName + ", " + regionName + ")";
                    Map<String, Object> tree = new HashMap<>();
                    tree.put("name", name);
                    tree.put("treeId", treeId);
                    tree.put("durianVariety", variety);
                    tree.put("age", child.child("age").getValue(String.class));
                    tree.put("orchardId", orchardId);
                    tree.put("orchardName", orchardName);
                    tree.put("regionName", regionName);
                    tree.put("notes", child.child("notes").getValue(String.class));
                    treeIds.add(treeId);
                    allTreeData.add(tree);
                    treeDisplay.add(display);
                }
                filterTreeList(treeOrchardFilterSpinner.getSelectedItemPosition());
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(TreeListActivity.this, "Failed to load trees", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterTreeList(int position) {
        if (position < 0 || position >= orchardIds.size()) {
            position = 0;
        }
        String selectedOrchardId = orchardIds.get(position);
        treeDisplay.clear();

        for (int i = 0; i < allTreeData.size(); i++) {
            Map<String, Object> tree = allTreeData.get(i);
            String orchardId = (String) tree.get("orchardId");
            if (selectedOrchardId.isEmpty() || selectedOrchardId.equals(orchardId)) {
                String name = (String) tree.get("name");
                String variety = (String) tree.get("durianVariety");
                String orchardName = (String) tree.get("orchardName");
                treeDisplay.add(name + " - " + variety + " (" + orchardName + ")");
            }
        }
        treeAdapter.notifyDataSetChanged();
    }

    private void showTreeOptions(int position) {
        String selectedOrchardId = orchardIds.get(treeOrchardFilterSpinner.getSelectedItemPosition());
        int index = position;
        if (!selectedOrchardId.isEmpty()) {
            List<Integer> filtered = new ArrayList<>();
            for (int i = 0; i < allTreeData.size(); i++) {
                if (selectedOrchardId.equals(allTreeData.get(i).get("orchardId"))) {
                    filtered.add(i);
                }
            }
            if (position >= 0 && position < filtered.size()) {
                index = filtered.get(position);
            }
        }
        String treeId = treeIds.get(index);
        String treeName = treeDisplay.get(position);

        new AlertDialog.Builder(this)
                .setTitle(treeName)
                .setItems(new CharSequence[]{"Edit", "Delete"}, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(TreeListActivity.this, TreeFormActivity.class);
                        intent.putExtra("treeId", treeId);
                        startActivity(intent);
                    } else {
                        confirmDelete(treeId);
                    }
                })
                .show();
    }

    private void confirmDelete(String treeId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Tree")
                .setMessage("Remove this tree from the system?")
                .setPositiveButton("Delete", (dialog, which) -> treesRef.child(treeId).removeValue())
                .setNegativeButton("Cancel", null)
                .show();
    }
}
