package com.example.duritor;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MapActivity extends DrawerActivity {

    private static final int LOCATION_REQUEST_CODE = 1001;
    private TextView mapLatitudeText;
    private TextView mapLongitudeText;
    private Button refreshLocationButton;
    private Button navigateButton;
    private Spinner mapOrchardSpinner;
    private DatabaseReference orchardsRef;
    private List<String> orchardIds;
    private List<String> orchardNames;
    private ArrayAdapter<String> orchardAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupDrawerShell(R.layout.activity_map, R.id.nav_map, R.string.title_map);

        mapLatitudeText = findViewById(R.id.mapLatitudeText);
        mapLongitudeText = findViewById(R.id.mapLongitudeText);
        refreshLocationButton = findViewById(R.id.refreshLocationButton);
        navigateButton = findViewById(R.id.navigateButton);
        mapOrchardSpinner = findViewById(R.id.mapOrchardSpinner);

        orchardsRef = FirebaseDatabase.getInstance().getReference("orchards");
        orchardIds = new ArrayList<>();
        orchardNames = new ArrayList<>();
        orchardAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, orchardNames);
        orchardAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mapOrchardSpinner.setAdapter(orchardAdapter);

        refreshLocationButton.setOnClickListener(v -> refreshLocation());
        navigateButton.setOnClickListener(v -> openNavigation());

        loadOrchards();
        refreshLocation();
    }

    private void loadOrchards() {
        orchardsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                orchardIds.clear();
                orchardNames.clear();
                orchardIds.add("");
                orchardNames.add("Select orchard");
                for (DataSnapshot orchardSnapshot : snapshot.getChildren()) {
                    String id = orchardSnapshot.getKey();
                    String name = orchardSnapshot.child("name").getValue(String.class);
                    if (id != null) {
                        orchardIds.add(id);
                        orchardNames.add(name != null ? name : "Unnamed Orchard");
                    }
                }
                orchardAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(MapActivity.this, "Unable to load orchards", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void refreshLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            return;
        }

        Location location = null;
        if (locationManager != null) {
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
        }

        if (location != null) {
            setLocationLabels(location.getLatitude(), location.getLongitude());
        } else {
            Toast.makeText(this, "Unable to determine current location. Please check GPS.", Toast.LENGTH_LONG).show();
        }
    }

    private void setLocationLabels(double latitude, double longitude) {
        mapLatitudeText.setText("Latitude: " + latitude);
        mapLongitudeText.setText("Longitude: " + longitude);
    }

    private void openNavigation() {
        int position = mapOrchardSpinner.getSelectedItemPosition();
        if (position <= 0 || position >= orchardIds.size()) {
            Toast.makeText(this, "Please select an orchard first.", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedId = orchardIds.get(position);
        orchardsRef.child(selectedId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String lat = snapshot.child("lat").getValue(String.class);
                String lng = snapshot.child("lng").getValue(String.class);
                String name = snapshot.child("name").getValue(String.class);
                String location = snapshot.child("location").getValue(String.class);

                if (lat != null && lng != null && !lat.isEmpty() && !lng.isEmpty()) {
                    String uri = "google.navigation:q=" + lat + "," + lng;
                    startNavigation(uri);
                } else if (location != null && !location.isEmpty()) {
                    String uri = "geo:0,0?q=" + Uri.encode(location + " " + (name != null ? name : "Orchard"));
                    startNavigation(uri);
                } else {
                    Toast.makeText(MapActivity.this, "Orchard has no coordinates.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(MapActivity.this, "Unable to retrieve orchard location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startNavigation(String uriString) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
        intent.setPackage("com.google.android.apps.maps");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "Google Maps is not installed.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            refreshLocation();
        }
    }
}
