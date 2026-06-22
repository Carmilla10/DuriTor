package com.example.duritor;

import android.content.DialogInterface;
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

public class OrchardListActivity extends DrawerActivity {

    private Button addOrchardButton;
    private ListView orchardListView;
    private DatabaseReference orchardRef;
    private List<String> orchardIds;
    private List<String> orchardDisplay;
    private Map<String, Map<String, Object>> orchardMap;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupDrawerShell(R.layout.activity_orchard_list, R.id.nav_orchards, R.string.title_orchards);

        addOrchardButton = findViewById(R.id.addOrchardButton);
        orchardListView = findViewById(R.id.orchardListView);

        orchardIds = new ArrayList<>();
        orchardDisplay = new ArrayList<>();
        orchardMap = new HashMap<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, orchardDisplay);
        orchardListView.setAdapter(adapter);

        orchardRef = FirebaseDatabase.getInstance().getReference("orchards");

        addOrchardButton.setOnClickListener(v -> startActivity(new Intent(OrchardListActivity.this, OrchardFormActivity.class)));

        orchardListView.setOnItemClickListener((parent, view, position, id) -> showOptionsDialog(position));

        loadOrchards();
    }

    private void loadOrchards() {
        orchardRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                orchardIds.clear();
                orchardDisplay.clear();
                orchardMap.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    String orchardId = child.getKey();
                    String name = child.child("name").getValue(String.class);
                    String location = child.child("location").getValue(String.class);
                    String displayText = name != null ? name : "Unnamed Orchard";
                    if (location != null && !location.isEmpty()) {
                        displayText += " (" + location + ")";
                    }
                    orchardIds.add(orchardId);
                    orchardDisplay.add(displayText);
                    Map<String, Object> orchard = new HashMap<>();
                    orchard.put("name", name != null ? name : "");
                    orchard.put("location", location != null ? location : "");
                    orchard.put("lat", child.child("lat").getValue(String.class));
                    orchard.put("lng", child.child("lng").getValue(String.class));
                    orchardMap.put(orchardId, orchard);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(OrchardListActivity.this, "Failed to load orchards: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showOptionsDialog(int position) {
        String orchardId = orchardIds.get(position);
        String orchardName = orchardDisplay.get(position);

        new AlertDialog.Builder(this)
                .setTitle(orchardName)
                .setItems(new CharSequence[]{"Edit", "Delete"}, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(OrchardListActivity.this, OrchardFormActivity.class);
                        intent.putExtra("orchardId", orchardId);
                        startActivity(intent);
                    } else {
                        confirmDelete(orchardId);
                    }
                })
                .show();
    }

    private void confirmDelete(String orchardId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Orchard")
                .setMessage("Remove this orchard and its trees?")
                .setPositiveButton("Delete", (dialog, which) -> deleteOrchard(orchardId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteOrchard(String orchardId) {
        orchardRef.child(orchardId).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DatabaseReference treeRef = FirebaseDatabase.getInstance().getReference("trees");
                treeRef.orderByChild("orchardId").equalTo(orchardId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        for (DataSnapshot treeSnapshot : snapshot.getChildren()) {
                            treeSnapshot.getRef().removeValue();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                    }
                });
                Toast.makeText(OrchardListActivity.this, "Orchard deleted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(OrchardListActivity.this, "Delete failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
