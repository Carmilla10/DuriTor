package com.example.duritor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseBackgroundService extends Service {

    private DatabaseReference databaseReference;
    private static final String CHANNEL_ID = "fall_service";
    private String lastNotifiedEventId = "";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        // FIX: Create Notification object, not Builder
        Notification notification = getForegroundNotification();
        startForeground(1001, notification);

        // Listen for new fall events
        databaseReference = FirebaseDatabase.getInstance().getReference("fallEvents");

        databaseReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                String eventId = snapshot.getKey();
                if (eventId != null && !eventId.equals(lastNotifiedEventId)) {
                    lastNotifiedEventId = eventId;
                    sendFallNotification(snapshot);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    private void sendFallNotification(DataSnapshot snapshot) {
        String alert = snapshot.child("alert").getValue(String.class);
        String orchard = snapshot.child("orchardName").getValue(String.class);
        String tree = snapshot.child("treeName").getValue(String.class);
        String date = snapshot.child("date").getValue(String.class);
        String time = snapshot.child("time").getValue(String.class);

        if (alert == null) alert = "Durian Fall Detected!";
        if (orchard == null) orchard = "Unknown Orchard";
        if (tree == null) tree = "Unknown Tree";

        String message = alert + " at " + orchard + " - " + tree;
        String timeText = (date != null ? date : "") + " " + (time != null ? time : "");

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle("🚨 Fall Alert")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message + "\n" + timeText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    // FIX: Return Notification object, not Builder
    private Notification getForegroundNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Duritor Monitoring")
                .setContentText("Listening for fall detections...")
                .setSmallIcon(R.drawable.logo)
                .setContentIntent(pendingIntent);

        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Fall Detection Service",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}