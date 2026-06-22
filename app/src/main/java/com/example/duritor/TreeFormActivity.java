package com.example.duritor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;


import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TreeFormActivity extends DrawerActivity {

    private Spinner treeOrchardSpinner;
    private EditText treeIdEdit;
    private EditText treeVarietyEdit;
    private EditText treeAgeEdit;
    private EditText treeNotesEdit;
    private Button saveTreeButton;
    private DatabaseReference orchardsRef;
    private DatabaseReference treesRef;
    private String treeId;
    private List<String> orchardIds;
    private List<String> orchardNames;
    private ArrayAdapter<String> orchardAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        treeId = getIntent().getStringExtra("treeId");
        int titleRes = treeId != null ? R.string.title_edit_tree : R.string.title_add_tree;
        setupDrawerShell(R.layout.activity_tree_form, R.id.nav_trees, titleRes);

        treeOrchardSpinner = findViewById(R.id.treeOrchardSpinner);
        treeIdEdit = findViewById(R.id.treeIdEdit);
        treeVarietyEdit = findViewById(R.id.treeVarietyEdit);
        treeAgeEdit = findViewById(R.id.treeAgeEdit);
        treeNotesEdit = findViewById(R.id.treeNotesEdit);
        saveTreeButton = findViewById(R.id.saveTreeButton);

        orchardsRef = FirebaseDatabase.getInstance().getReference("orchards");
        treesRef = FirebaseDatabase.getInstance().getReference("trees");

        orchardIds = new ArrayList<>();
        orchardNames = new ArrayList<>();
        orchardAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, orchardNames);
        orchardAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        treeOrchardSpinner.setAdapter(orchardAdapter);

        loadOrchards();

        if (treeId != null) {
            loadTree();
        }

        saveTreeButton.setOnClickListener(v -> saveTree());
    }

    private void loadOrchards() {
        orchardsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                orchardIds.clear();
                orchardNames.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String orchardId = child.getKey();
                    String name = child.child("name").getValue(String.class);
                    orchardIds.add(orchardId);
                    if (name == null || name.isEmpty()) {
                        name = "Unnamed Orchard";
                    }
                    orchardNames.add(name);
                }
                orchardAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(TreeFormActivity.this, "Could not load orchards", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTree() {
        if (treeId == null) return;
        treesRef.child(treeId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                treeIdEdit.setText(snapshot.child("treeId").getValue(String.class));
                treeVarietyEdit.setText(snapshot.child("durianVariety").getValue(String.class));
                treeAgeEdit.setText(snapshot.child("age").getValue(String.class));
                treeNotesEdit.setText(snapshot.child("notes").getValue(String.class));
                String orchardId = snapshot.child("orchardId").getValue(String.class);
                if (orchardId != null) {
                    int index = orchardIds.indexOf(orchardId);
                    if (index >= 0) {
                        treeOrchardSpinner.setSelection(index);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(TreeFormActivity.this, "Could not load tree", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveTree() {
        String treeCode = treeIdEdit.getText().toString().trim();
        String variety = treeVarietyEdit.getText().toString().trim();
        String age = treeAgeEdit.getText().toString().trim();
        String notes = treeNotesEdit.getText().toString().trim();
        int orchardPosition = treeOrchardSpinner.getSelectedItemPosition();

        if (treeCode.isEmpty()) {
            Toast.makeText(this, "Enter tree ID", Toast.LENGTH_SHORT).show();
            return;
        }
        if (variety.isEmpty()) {
            Toast.makeText(this, "Enter durian variety", Toast.LENGTH_SHORT).show();
            return;
        }
        if (orchardPosition < 0 || orchardPosition >= orchardIds.size()) {
            Toast.makeText(this, "Select an orchard", Toast.LENGTH_SHORT).show();
            return;
        }

        String orchardId = orchardIds.get(orchardPosition);
        String orchardName = orchardNames.get(orchardPosition);

        if (treeId == null) {
            treeId = treesRef.push().getKey();
        }

        Map<String, Object> update = new HashMap<>();
        update.put("treeId", treeCode);
        update.put("name", treeCode);
        update.put("durianVariety", variety);
        update.put("age", age);
        update.put("notes", notes);
        update.put("orchardId", orchardId);
        update.put("orchardName", orchardName);

        treesRef.child(treeId).updateChildren(update).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(TreeFormActivity.this, "Tree saved", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(TreeFormActivity.this, TreeListActivity.class));
                finish();
            } else {
                Toast.makeText(TreeFormActivity.this, "Could not save tree", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
