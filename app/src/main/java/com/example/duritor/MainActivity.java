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

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends DrawerActivity {

    private TextView alertText, timeText, regionText, orchardText;
    private TextView orchardCountText, regionCountText, treeCountText;
    private ImageView capturedImageView;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;
    private String currentDisplayedEventId = "";
    private static final String CHANNEL_ID = "fall_alert_channel";
    private String lastNotifiedEventId = "";
    private boolean initialFallEventsLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupDrawer(R.id.nav_dashboard, getString(R.string.app_name));

        alertText = findViewById(R.id.alertText);
        timeText = findViewById(R.id.timeText);
        regionText = findViewById(R.id.regionText);
        orchardText = findViewById(R.id.orchardText);
        orchardCountText = findViewById(R.id.orchardCountText);
        regionCountText = findViewById(R.id.regionCountText);
        treeCountText = findViewById(R.id.treeCountText);
        capturedImageView = findViewById(R.id.capturedImageView);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        createNotificationChannel();

        databaseReference = FirebaseDatabase.getInstance().getReference("fallEvents");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.hasChildren()) {
                    DataSnapshot latestEvent = findLatestEvent(snapshot);
                    if (latestEvent != null) {
                        if (!initialFallEventsLoaded) {
                            String initialEventId = latestEvent.getKey();
                            if (initialEventId != null) {
                                lastNotifiedEventId = initialEventId;
                            }
                        }
                        updateFallEvent(latestEvent, initialFallEventsLoaded);
                    }
                } else {
                    runOnUiThread(() -> {
                        alertText.setText("No fall detected");
                        timeText.setText("Waiting for fall...");
                        regionText.setText("...");
                        orchardText.setText("...");
                        capturedImageView.setImageResource(android.R.drawable.ic_menu_camera);
                        currentDisplayedEventId = "";
                    });
                }
                initialFallEventsLoaded = true;
            }

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

        FirebaseDatabase.getInstance().getReference("regions").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                regionCountText.setText(String.valueOf(snapshot.getChildrenCount()));
            }
            @Override
            public void onCancelled(DatabaseError error) {
                regionCountText.setText("—");
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

    private DataSnapshot findLatestEvent(DataSnapshot snapshot) {
        DataSnapshot latest = null;
        String latestTimestampKey = null;

        for (DataSnapshot child : snapshot.getChildren()) {
            String date = child.child("date").getValue(String.class);
            String time = child.child("time").getValue(String.class);
            if (date == null) date = "";
            if (time == null) time = "";

            String sortKey = date + " " + time;
            if (latest == null || sortKey.compareTo(latestTimestampKey) > 0) {
                latest = child;
                latestTimestampKey = sortKey;
            }
        }

        // If current event is collected, find the next latest uncollected event
        if (latest != null && currentDisplayedEventId.equals(latest.getKey())) {
            Boolean isCollected = latest.child("collected").getValue(Boolean.class);
            if (isCollected != null && isCollected) {
                // Current event is collected, find next one
                DataSnapshot nextLatest = null;
                String nextLatestTimestampKey = null;

                for (DataSnapshot child : snapshot.getChildren()) {
                    Boolean collected = child.child("collected").getValue(Boolean.class);
                    if (collected == null || !collected) { // Skip collected events
                        String date = child.child("date").getValue(String.class);
                        String time = child.child("time").getValue(String.class);
                        if (date == null) date = "";
                        if (time == null) time = "";

                        String sortKey = date + " " + time;
                        if (nextLatest == null || sortKey.compareTo(nextLatestTimestampKey) > 0) {
                            nextLatest = child;
                            nextLatestTimestampKey = sortKey;
                        }
                    }
                }

                // If no uncollected events, show the latest anyway
                return nextLatest != null ? nextLatest : latest;
            }
        }

        return latest;
    }

    private void updateFallEvent(DataSnapshot snapshot, boolean isInitialLoad) {
        String eventId = snapshot.getKey();
        currentDisplayedEventId = eventId;

        String alert = snapshot.child("alert").getValue(String.class);
        String date = snapshot.child("date").getValue(String.class);
        String time = snapshot.child("time").getValue(String.class);
        String photoUrl = snapshot.child("photoUrl").getValue(String.class);
        String orchardName = snapshot.child("orchardName").getValue(String.class);
        String regionName = snapshot.child("regionName").getValue(String.class);
        String treeName = snapshot.child("treeName").getValue(String.class);

        if (alert == null || alert.isEmpty()) alert = "Durian Fall Detected!";
        if (date == null || date.isEmpty()) date = "Unknown Date";
        if (time == null || time.isEmpty()) time = "Unknown Time";
        if (orchardName == null || orchardName.isEmpty()) orchardName = "Unknown Orchard";
        if (regionName == null || regionName.isEmpty()) regionName = "Unknown Region";
        if (treeName == null || treeName.isEmpty()) treeName = "Unknown Tree";

        final String fullAlertMessage = alert;
        final String displayTime = time;
        final String displayRegion = regionName;
        final String displayOrchard = orchardName;
        final String finalPhotoUrl = photoUrl;
        final String finalEventId = eventId;
        final boolean isNewEvent = !isInitialLoad && !finalEventId.equals(lastNotifiedEventId);

        runOnUiThread(() -> {
            alertText.setText(fullAlertMessage);
            alertText.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.alert_active));

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (alertText != null) {
                    alertText.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.text_primary));
                }
            }, 3000);

            timeText.setText("🕒 " + displayTime);
            regionText.setText("📍 " + displayRegion);
            orchardText.setText("🌳 " + displayOrchard);

            // Load latest image with improved cache busting and error handling
            if (finalPhotoUrl != null && !finalPhotoUrl.trim().isEmpty() && !finalPhotoUrl.equals("null")) {
                try {
                    // Add timestamp to URL to force fresh load
                    String bustedUrl = finalPhotoUrl.contains("?")
                            ? finalPhotoUrl + "&t=" + System.currentTimeMillis()
                            : finalPhotoUrl + "?t=" + System.currentTimeMillis();

                    Glide.with(MainActivity.this)
                            .load(bustedUrl)
                            .skipMemoryCache(true)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .placeholder(android.R.drawable.ic_menu_camera)
                            .error(android.R.drawable.ic_menu_camera)
                            .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                                @Override
                                public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bu
                                        mptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                                    Log.e("MainActivity", "Image load failed: " + finalPhotoUrl, e);
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glid
                                        e.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource)
                                {
                                    Log.d("MainActivity", "Image loaded successfully from: " + dataSource);
                                    return false;
                                }
                            })
                            .centerCrop()
                            .into(capturedImageView);
                } catch (Exception e) {
                    Log.e("MainActivity", "Exception loading image", e);
                    capturedImageView.setImageResource(android.R.drawable.ic_menu_camera);
                }
            } else {
                Log.w("MainActivity", "Invalid photoUrl: " + finalPhotoUrl);
                capturedImageView.setImageResource(android.R.drawable.ic_menu_camera);
            }

            if (isNewEvent) {
                Toast.makeText(MainActivity.this, fullAlertMessage, Toast.LENGTH_LONG).show();
                sendLocalNotification("🚨 Fall Alert", fullAlertMessage);
                lastNotifiedEventId = finalEventId;
            }
        });
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