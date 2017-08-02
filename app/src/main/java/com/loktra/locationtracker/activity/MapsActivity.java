package com.loktra.locationtracker.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.loktra.locationtracker.R;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import ng.max.slideview.SlideView;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback{

    private GoogleMap mMap;
    private ArrayList<LatLng> pointList;
    private long startTime;
    private long endTime;
    private boolean isFirstTime = true;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private Boolean mRequestingLocationUpdates;
    private LocationManager locationManager;
    private LocationListener lListener;
    private static long TIME_TO_GET_UPDATE_IN_MILIS = 1000 * 10;

    @BindView(R.id.btn_start_shift)
    SlideView startButton;

    @BindView(R.id.btn_end_shift)
    SlideView endButton;

    @BindView(R.id.tv_shift_time)
    TextView shiftTime;

    @BindView(R.id.time_layout)
    CardView timeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        ButterKnife.bind(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        pointList = new ArrayList<>();
        lListener = new Mylocationlistener();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mRequestingLocationUpdates = false;
        setSlideListeners();

    }

    // Set slide listener on start and end shift slider buttons
    private void setSlideListeners() {
        startButton.setOnSlideCompleteListener(new SlideView.OnSlideCompleteListener() {
            @Override
            public void onSlideComplete(SlideView slideView) {
                // vibrate the device
                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(100);

                resetValues();
                startUpdatesButtonHandler();
            }
        });

        endButton.setOnSlideCompleteListener(new SlideView.OnSlideCompleteListener() {
            @Override
            public void onSlideComplete(SlideView slideView) {
                // vibrate the device
                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(100);

                showTimeLayout();
                drawRouteOnMap();
                stopUpdatesButtonHandler();
            }
        });
    }

    // Draw line route on map with list on latLng points
    private void drawRouteOnMap() {
        if(pointList.size()>0){
            MarkerOptions markerEnd = new MarkerOptions().position(pointList.get(pointList.size()-1)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            mMap.addMarker(markerEnd);
        }
        PolylineOptions lineOptions = new PolylineOptions();
        lineOptions.addAll(pointList);
        lineOptions.width(12);
        lineOptions.color(Color.BLUE);
        lineOptions.geodesic(true);
        mMap.addPolyline(lineOptions);
    }

    // Show shift time layout after shift is ended
    private void showTimeLayout() {
        startButton.setVisibility(View.VISIBLE);
        endButton.setVisibility(View.GONE);
        timeLayout.setVisibility(View.VISIBLE);
        endTime = System.currentTimeMillis();
        int minutes = (int) (((endTime - startTime) / (1000*60)) % 60);
        int hours   = (int) (((endTime - startTime) / (1000*60*60)) % 24);
        String totalTime = hours + "h " + minutes + "m";
        shiftTime.setText(totalTime);
    }

    // Reset values when a new shift is started
    private void resetValues() {
        startTime = System.currentTimeMillis();
        isFirstTime = true;
        mMap.clear();
        pointList.clear();
        startButton.setVisibility(View.GONE);
        endButton.setVisibility(View.VISIBLE);
        timeLayout.setVisibility(View.GONE);
    }

    // Request permission for location if not already allowed and request for location updates
    public void startUpdatesButtonHandler() {
        if (!mRequestingLocationUpdates) {
            mRequestingLocationUpdates = true;
        } else {
            return;
        }

        if (Build.VERSION.SDK_INT < 23) {
            startLocationUpdates();
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                showRationaleDialog();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        }
    }

    public void stopUpdatesButtonHandler() {
        if (mRequestingLocationUpdates) {
            stopLocationUpdates();
            mRequestingLocationUpdates = false;
        }
    }

    // Start location updates if GPS is on otherwise promt to on GPS.
    private void startLocationUpdates() {
        boolean gps_enabled = false;
        LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }catch (Exception ex){}
        if(!gps_enabled){
           askToEnableGPS();
        }

        if (ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, TIME_TO_GET_UPDATE_IN_MILIS , 0, lListener);
        }
    }

    // Prompt user to enable GPS if it's not on
    private void askToEnableGPS() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setMessage("location service must be on for this app to work");
        dialog.setPositiveButton("yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                MapsActivity.this.startActivity(myIntent);
            }
        });
        dialog.setNegativeButton("no", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                // TODO Auto-generated method stub
                askToEnableGPS();
            }
        });
        dialog.show();
    }

    // Stop receiving location updates for this app
    protected void stopLocationUpdates() {
        locationManager.removeUpdates(lListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationUpdates();
                } else {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        mRequestingLocationUpdates = false;
                    } else {
                        showRationaleDialog();
                    }
                }
                break;
            }
        }
    }

    // Show dialog when user decline location access request
    private void showRationaleDialog() {
        new AlertDialog.Builder(this)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(MapsActivity.this,
                                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(MapsActivity.this, "Location access not granted", Toast.LENGTH_SHORT).show();
                        mRequestingLocationUpdates = false;
                    }
                })
                .setCancelable(false)
                .setMessage("This app requires location access to work properly.")
                .show();
    }

    @Override
    protected void onDestroy() {
        stopLocationUpdates();
        super.onDestroy();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    // Class to listen location updates
    private class Mylocationlistener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            if (location != null) {
                LatLng position  = new LatLng(location.getLatitude(),location.getLongitude());
                if(isFirstTime){
                    isFirstTime = false;
                    CameraPosition cameraPosition = new CameraPosition.Builder()
                            .target(position)
                            .zoom(15)
                            .build();
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                    MarkerOptions markerStart = new MarkerOptions().position(position).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                    mMap.addMarker(markerStart);
                }
                pointList.add(position);
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub
            askToEnableGPS();
        }

        @Override
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // TODO Auto-generated method stub

        }

    }


}
