package com.example.duritor;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends DrawerActivity {

    private TextView alertText, timeText, orchardCountText, treeCountText;
    private ImageView capturedImageView;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;
    private String currentDisplayedEventId = "";
    private static final String CHANNEL_ID = "fall_alert_channel";
    private String lastNotifiedEventId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupDrawer(R.id.nav_dashboard, getString(R.string.app_name));

        alertText = findViewById(R.id.alertText);
        timeText = findViewById(R.id.timeText);
        orchardCountText = findViewById(R.id.orchardCountText);
        treeCountText = findViewById(R.id.treeCountText);
        capturedImageView = findViewById(R.id.capturedImageView);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        createNotificationChannel();

        databaseReference = FirebaseDatabase.getInstance().getReference("fallEvents");

        databaseReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                String eventId = snapshot.getKey();
                if (eventId != null && !eventId.equals(lastNotifiedEventId)) {
                    lastNotifiedEventId = eventId;
                    updateFallEvent(snapshot);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
                if (snapshot.exists() && snapshot.hasChildren()) {
                    updateFallEvent(snapshot);
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {
                String removedId = snapshot.getKey();
                if (removedId != null && removedId.equals(currentDisplayedEventId)) {
                    runOnUiThread(() -> {
                        alertText.setText("No fall detected");
                        timeText.setText("Waiting for fall...");
                        capturedImageView.setImageResource(android.R.drawable.ic_menu_camera);
                        currentDisplayedEventId = "";
                    });
                }
            }

            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("MainActivity", "Database error: " + error.getMessage());
            }
        });

        loadCounts();

        findViewById(R.id.historyButton).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, HistoryActivity.class)));
        findViewById(R.id.mapButton).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, MapActivity.class)));
        findViewById(R.id.orchardButton).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, OrchardListActivity.class)));
        findViewById(R.id.treeButton).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, TreeListActivity.class)));
        findViewById(R.id.analyticsButton).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AnalyticsActivity.class)));
        findViewById(R.id.profileButton).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ProfileActivity.class)));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Fall Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void sendLocalNotification(String title, String message) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat.from(this).notify((int) System.currentTimeMillis(), builder.build());
    }

    private void loadCounts() {
        FirebaseDatabase.getInstance().getReference("orchards").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                orchardCountText.setText(String.valueOf(snapshot.getChildrenCount()));
            }
            @Override
            public void onCancelled(DatabaseError error) {
                orchardCountText.setText("—");
            }
        });

        FirebaseDatabase.getInstance().getReference("trees").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                treeCountText.setText(String.valueOf(snapshot.getChildrenCount()));
            }
            @Override
            public void onCancelled(DatabaseError error) {
                treeCountText.setText("—");
            }
        });
    }

    private void updateFallEvent(DataSnapshot snapshot) {
        String eventId = snapshot.getKey();

        String alert = snapshot.child("alert").getValue(String.class);
        String date = snapshot.child("date").getValue(String.class);
        String time = snapshot.child("time").getValue(String.class);
        String photoUrl = snapshot.child("photoUrl").getValue(String.class);
        String orchardName = snapshot.child("orchardName").getValue(String.class);
        String treeName = snapshot.child("treeName").getValue(String.class);

        if (alert == null || alert.isEmpty()) alert = "Durian Fall Detected!";
        if (date == null || date.isEmpty()) date = getCurrentDate();
        if (time == null || time.isEmpty()) time = getCurrentTime();
        if (orchardName == null || orchardName.isEmpty()) orchardName = "Unknown Orchard";
        if (treeName == null || treeName.isEmpty()) treeName = "Unknown Tree";

        final String displayLocation = orchardName + " - " + treeName;
        final String displayDateTime = date + " " + time;
        final String fullAlertMessage = alert + " at " + displayLocation;
        final String finalPhotoUrl = photoUrl;
        final String finalEventId = eventId;
        final boolean isNewEvent = !finalEventId.equals(currentDisplayedEventId);

        runOnUiThread(() -> {
            alertText.setText(fullAlertMessage);
            alertText.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.alert_active));

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (alertText != null) {
                    alertText.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.text_primary));
                }
            }, 3000);

            timeText.setText(displayDateTime);

            if (finalPhotoUrl != null && !finalPhotoUrl.isEmpty() && !finalPhotoUrl.equals("null")) {
                Glide.with(MainActivity.this)
                        .load(finalPhotoUrl)
                        .placeholder(android.R.drawable.ic_menu_camera)
                        .error(android.R.drawable.ic_menu_camera)
                        .centerCrop()
                        .into(capturedImageView);
            } else {
                capturedImageView.setImageResource(android.R.drawable.ic_menu_camera);
            }

            if (isNewEvent) {
                Toast.makeText(MainActivity.this, fullAlertMessage, Toast.LENGTH_LONG).show();
                sendLocalNotification("🚨 Fall Alert", fullAlertMessage);
                currentDisplayedEventId = finalEventId;
            }
        });
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }
}