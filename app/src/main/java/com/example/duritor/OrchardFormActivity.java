package com.example.duritor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class OrchardFormActivity extends DrawerActivity {

    private EditText orchardNameEdit;
    private EditText orchardLocationEdit;
    private EditText orchardLatEdit;
    private EditText orchardLngEdit;
    private EditText orchardDescriptionEdit;
    private Button saveOrchardButton;
    private DatabaseReference orchardRef;
    private String orchardId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        orchardId = getIntent().getStringExtra("orchardId");
        int titleRes = orchardId != null ? R.string.title_edit_orchard : R.string.title_add_orchard;
        setupDrawerShell(R.layout.activity_orchard_form, R.id.nav_orchards, titleRes);

        orchardNameEdit = findViewById(R.id.orchardNameEdit);
        orchardLocationEdit = findViewById(R.id.orchardLocationEdit);
        orchardLatEdit = findViewById(R.id.orchardLatEdit);
        orchardLngEdit = findViewById(R.id.orchardLngEdit);
        orchardDescriptionEdit = findViewById(R.id.orchardDescriptionEdit);
        saveOrchardButton = findViewById(R.id.saveOrchardButton);

        orchardRef = FirebaseDatabase.getInstance().getReference("orchards");

        if (orchardId != null) {
            loadOrchard();
        }

        saveOrchardButton.setOnClickListener(v -> saveOrchard());
    }

    private void loadOrchard() {
        orchardRef.child(orchardId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    return;
                }
                orchardNameEdit.setText(snapshot.child("name").getValue(String.class));
                orchardLocationEdit.setText(snapshot.child("location").getValue(String.class));
                orchardLatEdit.setText(snapshot.child("lat").getValue(String.class));
                orchardLngEdit.setText(snapshot.child("lng").getValue(String.class));
                orchardDescriptionEdit.setText(snapshot.child("description").getValue(String.class));
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(OrchardFormActivity.this, "Unable to load orchard", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveOrchard() {
        String name = orchardNameEdit.getText().toString().trim();
        String location = orchardLocationEdit.getText().toString().trim();
        String lat = orchardLatEdit.getText().toString().trim();
        String lng = orchardLngEdit.getText().toString().trim();
        String description = orchardDescriptionEdit.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Enter orchard name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (orchardId == null) {
            orchardId = orchardRef.push().getKey();
        }

        Map<String, Object> update = new HashMap<>();
        update.put("name", name);
        update.put("location", location);
        update.put("lat", lat);
        update.put("lng", lng);
        update.put("description", description);

        orchardRef.child(orchardId).updateChildren(update).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(OrchardFormActivity.this, "Orchard saved", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(OrchardFormActivity.this, OrchardListActivity.class));
                finish();
            } else {
                Toast.makeText(OrchardFormActivity.this, "Failed to save orchard", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
