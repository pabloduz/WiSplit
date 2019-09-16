package com.eccos.splitywifi;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;

import com.firebase.client.FirebaseError;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.firebase.client.Firebase;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

import static android.os.SystemClock.sleep;
import static com.eccos.splitywifi.MainActivity.id;

public class AddNetForm extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;

    private static final String TAG = "AddNetForm";

    private LocationCallback locationCallback;

    private Firebase myFirebaseRef;

    private LocationRequest locationRequestNew;

    private boolean noUserNetwork;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Firebase.setAndroidContext(this);
        setContentView(R.layout.activity_add_net_form);

        getSupportActionBar().hide();

        final EditText name = (EditText) findViewById(R.id.name);
        final EditText phone = (EditText) findViewById(R.id.phone);
        final EditText email = (EditText) findViewById(R.id.email);
        final EditText wifi = (EditText) findViewById(R.id.wifi_name);

        verifyUserNetwork();

        SharedPreferences spCurrency = this.getSharedPreferences("PREF_CURRENCY", Context.MODE_PRIVATE);

        String currency = spCurrency.getString("PREF_CURRENCY", "$");


        final EditText price = (EditText) findViewById(R.id.price);
        price.addTextChangedListener(new NumberTextWatcher(price, "#,###", currency));

        startLocationUpdates();

        final Button button = (Button) findViewById(R.id.send_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if( TextUtils.isEmpty(name.getText())) {
                    /**
                     *   You can Toast a message here that the Username is Empty
                     **/

                    name.setError("First name is required!");

                }else if (TextUtils.isEmpty(email.getText())) {
                    email.setError("Email is required!");

                }else if (TextUtils.isEmpty(wifi.getText())) {
                    wifi.setError("WiFi Name is required!");

                }else if (TextUtils.isEmpty(price.getText())) {
                    price.setError("Price is required!");

                }else {
                    if (noUserNetwork) {
                        // your handler code here
                        try {
                            fusedLocationClient.getLastLocation()
                                    .addOnSuccessListener(AddNetForm.this, new OnSuccessListener<Location>() {
                                        @Override
                                        public void onSuccess(final Location location) {
                                            // Got last known location. In some rare situations this can be null.

                                            if (location != null) {
                                                //final LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());

                                                Log.e("Cord", "Latitude: " + location.getLatitude() + " Longitude: " + location.getLongitude());

                                                //Getting reference to Firebase
                                                myFirebaseRef = new Firebase("https://wisplit-2e9e8.firebaseio.com/");

                                                //Saving all info into the Firebase
                                                Map mInformation = new HashMap();

                                                if (TextUtils.isEmpty(phone.getText())) {
                                                    mInformation.put("phone", "0000000000");
                                                } else {
                                                    mInformation.put("phone", phone.getText().toString());
                                                }

                                                String id = id(getApplication());
                                                mInformation.put("user", id);

                                                mInformation.put("name", name.getText().toString());
                                                mInformation.put("email", email.getText().toString());
                                                mInformation.put("wifi", wifi.getText().toString());
                                                mInformation.put("price", price.getText().toString());

                                                Firebase fb = myFirebaseRef.child("networks").push();
                                                fb.setValue(mInformation);

                                                String key= fb.getKey();

                                                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("/locations");
                                                GeoFire geoFire = new GeoFire(ref);

                                                geoFire.setLocation(key, new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {
                                                    @Override
                                                    public void onComplete(String key, DatabaseError error) {
                                                        if (error != null) {
                                                            System.err.println("There was an error saving the location to GeoFire: " + error);
                                                        } else {
                                                            System.out.println("Location saved on server successfully!");
                                                        }
                                                    }
                                                });


                                                //User has set a network offer
                                                SharedPreferences sharedPrefs = getApplicationContext().getSharedPreferences("PREF_NET", Context.MODE_PRIVATE);
                                                sharedPrefs.edit().putBoolean("PREF_NET", false).commit();

                                                sleep(150);

                                                Intent myIntent = new Intent(AddNetForm.this, MainActivity.class);
                                                startActivity(myIntent);

                                            } else {
                                                Log.e(TAG, "Location is null.");

                                                View contextView = AddNetForm.this.findViewById(android.R.id.content);

                                                Snackbar.make(contextView, R.string.location_null, Snackbar.LENGTH_LONG)
                                                        .show();
                                            }
                                        }
                                    });

                        } catch (SecurityException ex) {
                            Log.e(TAG, "Requires location permission.");
                        }

                    } else {
                        View contextView = AddNetForm.this.findViewById(android.R.id.content);

                        Snackbar.make(contextView, R.string.userNetwork, Snackbar.LENGTH_LONG)
                                .show();
                    }
                }
            }
        });
    }

    public void verifyUserNetwork(){
        SharedPreferences sharedPrefs = this.getSharedPreferences("PREF_NET", Context.MODE_PRIVATE);

//      sharedPrefs.edit().putBoolean(PREF_NET, false).commit();

        noUserNetwork = sharedPrefs.getBoolean("PREF_NET", true);
    }


    public void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(AddNetForm.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(AddNetForm.this);

            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null) {
                        return;
                    }
                    for (Location location : locationResult.getLocations()) {
                        // Update UI with location data

                    }
                }

                ;
            };

            locationRequestNew = LocationRequest.create();

            locationRequestNew.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequestNew.setFastestInterval(2500);
            locationRequestNew.setInterval(5000);


            fusedLocationClient.requestLocationUpdates(locationRequestNew,
                    locationCallback,
                    null /* Looper */);

        }
    }

    /**
     * Called after the start and in between pauses and running.
     */
    @Override
    public void onResume() {
        super.onResume();
        startLocationUpdates();
    }

    /**
     * Executed when the process is running on the background.
     */
    public void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
}

//                myIntent.putExtra("name", name.getText());
//                myIntent.putExtra("phone", phone.getText());
//                myIntent.putExtra("email", email.getText());
//                myIntent.putExtra("wifi_name", wifi_name.getText());
//                myIntent.putExtra("price_asked", price_asked.getText());

//                Get last know location here