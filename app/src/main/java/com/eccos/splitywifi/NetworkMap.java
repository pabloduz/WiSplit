package com.eccos.splitywifi;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import androidx.annotation.NonNull;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryDataEventListener;
import com.google.android.gms.maps.model.VisibleRegion;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.ActivityCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class NetworkMap extends SupportMapFragment implements OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener{

    private GoogleMap mMap;

    private static final String TAG = "NetworkMap";

    private static final int MY_PERMISSION_REQUEST_FINE_LOCATION = 101;

    private FusedLocationProviderClient fusedLocationClient;

    private LocationCallback locationCallback;

    private LocationRequest locationRequest;

    private boolean firstExecution = true;

    private boolean noCurrency = true;

    private FloatingActionButton fab;

    private Handler handler;

    private ArrayList<String> markerKeyList = new ArrayList<>();




    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createLocationRequest(); startLocationUpdates();

        Firebase.setAndroidContext(getActivity());

        fab= getActivity().findViewById(R.id.fab);
        handler= new Handler();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        getMapAsync(this);
    }

    protected void createLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setSmallestDisplacement(1);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(getActivity());
        client.checkLocationSettings(builder.build());
    }

    public void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());


            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null) {
                        return;
                    }

                    for (Location location : locationResult.getLocations()) {
                        // Update UI with location data
                        LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());

                        if (mMap != null) {
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(myLocation));
                        }
                    }
                }
            };

            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    null /* Looper */);

        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET}, MY_PERMISSION_REQUEST_FINE_LOCATION);
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case MY_PERMISSION_REQUEST_FINE_LOCATION:

                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //permission was granted do nothing and carry on

                } else {
                    Toast.makeText(getActivity(), "This app requires location permissions to be granted.", Toast.LENGTH_LONG).show();
                    getActivity().finish();
                }

                break;
        }
    }

    /**
     * Called after the start and in between pauses and running.
     */
    @Override
    public void onResume() {
        super.onResume();

        if (MainActivity.isNetworkAvailable(getActivity()))
        {
            if (firstExecution) {
                showSnackBar(R.string.find_net);

                firstExecution = false;

            } else {
                showSnackBar(R.string.add_net);

                startLocationUpdates();
            }
        }else{
            /*No internet so give a custom Toast / Alert dialogue and exit the application */
            showSnackBar(R.string.check_internet);

            Log.e("TAG", "Internet is not available.");
        }

        markerKeyList.clear();

        fab = getActivity().findViewById(R.id.fab);
        fab.hide();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                fab.show();
            }
        }, 3500);
    }

    /**
     * Executed when the process is running on the background.
     */
    public void onPause() {
        super.onPause();

        stopLocationUpdates();
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
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
    public void onMapReady(final GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnInfoWindowClickListener(this);
        mMap.getUiSettings().setMapToolbarEnabled(false);

//        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        try {
            mMap.setMyLocationEnabled(true);

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(getActivity(), new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(final Location location) {
                            if (location != null) {
                                // Got last known location. In some rare situations this can be null.
                                final LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());

                                mMap.moveCamera(CameraUpdateFactory.newLatLng(myLocation));
                                mMap.animateCamera(CameraUpdateFactory.zoomTo(18));

                                mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
                                    @Override
                                    public void onCameraIdle() {
                                        getNetworks();
//                                        Log.e("UPDATE", "Camera Idle");
                                    }
                                });

                                //Get user's currency if not set already
                                if(noCurrency){
                                    getCurrency(myLocation);
                                }
                            }

                        }
                    }).addOnFailureListener(new OnFailureListener() {

                @Override
                public void onFailure(@NonNull Exception e) {
                    showSnackBar(R.string.check_gps);

                    Log.d(TAG, "Error trying to get last GPS location");
                    e.printStackTrace();
                }
            });


            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent myIntent = new Intent(getActivity(), AddNet.class);
                    startActivity(myIntent);
                }
            });


        } catch (SecurityException ex) {
            Log.e(TAG, "Requires location permission.");
        }
    }

    private void getNetworks() {
        LatLng centerPosition= mMap.getCameraPosition().target;

        double radius= getMapVisibleRadius();
//        Log.e("RADIUS", "" + radius);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("/locations");
//        Log.e("TAG", "" + ref);
        GeoFire geoFire = new GeoFire(ref);

        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(centerPosition.latitude, centerPosition.longitude), radius);

        geoQuery.addGeoQueryDataEventListener(new GeoQueryDataEventListener() {

            @Override
            public void onDataEntered(DataSnapshot dataSnapshot, GeoLocation location) {
                String key= dataSnapshot.getKey();

                if(!markerKeyList.contains(key)) {

                    markerKeyList.add(key);

                    updateMapMarker(dataSnapshot);

                    Log.e("TAG", "Does not contain. Update realized!");
                }
            }

            @Override
            public void onDataExited(DataSnapshot dataSnapshot) {
                // ...
            }

            @Override
            public void onDataMoved(DataSnapshot dataSnapshot, GeoLocation location) {
                // ...
            }


            @Override
            public void onDataChanged(DataSnapshot dataSnapshot, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                // ...
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                // ...
            }

        });
    }


    private double getMapVisibleRadius() {
        VisibleRegion visibleRegion = mMap.getProjection().getVisibleRegion();

        float[] diagonalDistance = new float[1];

        LatLng farLeft = visibleRegion.farLeft;
        LatLng nearRight = visibleRegion.nearRight;

        Location.distanceBetween(
                farLeft.latitude,
                farLeft.longitude,
                nearRight.latitude,
                nearRight.longitude,
                diagonalDistance
        );

        return (diagonalDistance[0] / 2) / 1000;
    }


    public void updateMapMarker(DataSnapshot dataSnapshot) {
        try{
            Map data = (Map) dataSnapshot.getValue();

            // Get recorded latitude and longitude
             ArrayList mCoordinate = (ArrayList) data.get("l");

             double latitude = (double) (mCoordinate.get(0));
             double longitude = (double) (mCoordinate.get(1));

             // Create LatLng for marker location
             final LatLng mLatlng = new LatLng(latitude, longitude);

            //Get the DataSnapshot key
            final String key = dataSnapshot.getKey();

            Firebase myFirebaseRef = new Firebase("https://wisplit-2e9e8.firebaseio.com/");
            Firebase ref = myFirebaseRef.child("networks").child(key);

            ref.addListenerForSingleValueEvent(new ValueEventListener() {

                @Override
                public void onDataChange(com.firebase.client.DataSnapshot dataSnapshot) {

                    Map data = (Map) dataSnapshot.getValue();

                    String user = (String) data.get("user");
                    String wifi = (String) data.get("wifi");
                    String price = (String) data.get("price");

                    // Add a marker for logged location
                    MarkerOptions marker = new MarkerOptions().position(mLatlng).title(wifi).snippet(price).icon(BitmapDescriptorFactory.fromResource(R.drawable.clip_wifi));
                    Marker mMarker = mMap.addMarker(marker);

                    ArrayList pin = new ArrayList<>(2);
                    pin.add(key); pin.add(user);

                    mMarker.setTag(pin);
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {

                }

            });

        }catch (Exception ex){

            Log.e(TAG, "FireBase exception: " + ex.getMessage());
        }
    }

    /** Called when the user clicks a marker. */
    @Override
    public void onInfoWindowClick(Marker marker) {
        ArrayList<String> tag = (ArrayList) marker.getTag();

        Intent myIntent = new Intent(getActivity(), SignalNet.class);
        myIntent.putExtra("tag", tag);
        startActivity(myIntent);

    }




    private void getCurrency(LatLng location){
        Geocoder geocoder = new Geocoder(getActivity(), Locale.getDefault());
        List<Address> addresses = null;

        try {
            addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1);

            if (!addresses.isEmpty())
            {
                String countryCode= addresses.get(0).getCountryCode();

                Locale locale = new Locale("", countryCode);
                Currency currency = Currency.getInstance(locale);
                String symbol = currency.getSymbol();

                SharedPreferences sharedPrefs = getContext().getSharedPreferences("PREF_CURRENCY", Context.MODE_PRIVATE);
                sharedPrefs.edit().putString("PREF_CURRENCY", symbol).commit();

                noCurrency= false;

                //Log.e("TAG", symbol);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showSnackBar(int text) {
        View contextView = getActivity().findViewById(android.R.id.content);

        Snackbar.make(contextView, text, Snackbar.LENGTH_LONG)
                .show();
    }
}

//Toast.makeText(getActivity(), "Find WiFi networks near you.", Toast.LENGTH_LONG).show();

//private static final String TAG_ = NetworkMap.class.getSimpleName();

