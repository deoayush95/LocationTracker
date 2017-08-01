package com.loktra.locationtracker.activity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
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
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ArrayList<LatLng> pointList;
    private LocationManager locationManager;
    private LocationListener lListener;
    private long startTime;
    private long endTime;
    private boolean isFirstTime = true;

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;

    @BindView(R.id.btn_start_shift)
    Button startButton;

    @BindView(R.id.btn_end_shift)
    Button endButton;

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
        lListener = new mylocationlistener();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        endButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startButton.setVisibility(View.VISIBLE);
                endButton.setVisibility(View.GONE);
                timeLayout.setVisibility(View.VISIBLE);
                locationManager.removeUpdates(lListener);
                endTime = System.currentTimeMillis();
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

                int minutes = (int) (((endTime - startTime) / (1000*60)) % 60);
                int hours   = (int) (((endTime - startTime) / (1000*60*60)) % 24);

                String totalTime = hours + "h " + minutes + "m";
                shiftTime.setText(totalTime);
            }
        });


    }

    private void requestLocationData() {
        Log.e("LOCATION CHANGED", "workinggg");
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000 , 0, lListener);
//        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000 , 0, lListener);

      /*  if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            Log.e("LOCATION CHANGED", "workinggg");
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000*10, 0, lListener);
        }*/
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
       /* if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

        }
        Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e("LOCATION CHANGED", "workinggg permission");

                    startButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startTime = System.currentTimeMillis();
                            isFirstTime = true;
                            mMap.clear();
                            pointList.clear();
                            requestLocationData();
                            startButton.setVisibility(View.GONE);
                            endButton.setVisibility(View.VISIBLE);
                            timeLayout.setVisibility(View.GONE);
                        }
                    });
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    private class mylocationlistener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {

            Log.e("LOCATION CHANGED", "called in interval");
            if (location != null) {
                Log.e("LOCATION CHANGED", location.getLatitude() + "");
                Log.e("LOCATION CHANGED", location.getLongitude() + "");
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
                Toast.makeText(MapsActivity.this,
                        "My Current location:\nLatitude:"+location.getLatitude() + "\nLogitude:" + location.getLongitude(),Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub

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

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest()
                .setInterval(TimeUnit.SECONDS.toMillis(1000))
                .setFastestInterval(TimeUnit.SECONDS.toMillis(1000))
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

}
