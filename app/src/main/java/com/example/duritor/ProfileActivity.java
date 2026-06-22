package com.example.duritor;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends DrawerActivity {

    private TextView profileEmailText;
    private EditText profileNameEdit;
    private Button saveProfileButton;
    private Button changePasswordButton;
    private Button logoutButton;
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupDrawerShell(R.layout.activity_profile, R.id.nav_profile, R.string.title_profile);

        profileEmailText = findViewById(R.id.profileEmailText);
        profileNameEdit = findViewById(R.id.profileNameEdit);
        saveProfileButton = findViewById(R.id.saveProfileButton);
        changePasswordButton = findViewById(R.id.changePasswordButton);
        logoutButton = findViewById(R.id.logoutButton);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
            finish();
            return;
        }

        profileEmailText.setText("Email: " + user.getEmail());
        usersRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
        loadUserProfile();

        saveProfileButton.setOnClickListener(v -> saveProfile());
        changePasswordButton.setOnClickListener(v -> showPasswordDialog());
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void loadUserProfile() {
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String name = snapshot.child("displayName").getValue(String.class);
                if (name != null) {
                    profileNameEdit.setText(name);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProfile() {
        String displayName = profileNameEdit.getText().toString().trim();
        if (displayName.isEmpty()) {
            Toast.makeText(this, "Enter a display name", Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("displayName", displayName);
        usersRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(ProfileActivity.this, "Profile updated", Toast.LENGTH_SHORT).show();

                // Refresh the drawer header to show the new name
                refreshDrawerHeader();

            } else {
                Toast.makeText(ProfileActivity.this, "Could not update profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showPasswordDialog() {
        final EditText passwordInput = new EditText(this);
        passwordInput.setHint("New Password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setView(passwordInput)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newPassword = passwordInput.getText().toString().trim();
                    if (newPassword.length() < 6) {
                        Toast.makeText(ProfileActivity.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        user.updatePassword(newPassword).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(ProfileActivity.this, "Password updated", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(ProfileActivity.this, "Password update failed", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}