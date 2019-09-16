package com.eccos.splitywifi;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.view.View;
import com.google.android.material.navigation.NavigationView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private FragmentManager fragmentManager;
    private NetworkMap networkMap;
    private RequestNetMap requestNetMap;
    private FloatingActionButton fab;

    private static String uniqueID = null;
    private static final String PREF_UNIQUE_ID = "PREF_UNIQUE_ID";
    private static final int MY_PERMISSION_REQUEST_FINE_LOCATION = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case MY_PERMISSION_REQUEST_FINE_LOCATION:

                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //permission was granted do nothing and carry on
                    setContentView(R.layout.activity_main);

                    Toolbar toolbar = findViewById(R.id.toolbar);
                    setSupportActionBar(toolbar);
                    //getSupportActionBar().setSubtitle("Find WiFi networks near you.");

                    Firebase.setAndroidContext(this);

                    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                    ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                            this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
                    drawer.addDrawerListener(toggle);
                    toggle.syncState();

                    NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
                    navigationView.setNavigationItemSelectedListener(this);

                    networkMap = new NetworkMap();

                    requestNetMap = new RequestNetMap();

                    boolean isRequestFragment  = getIntent().getBooleanExtra("isRequestFragment", false);


                    if(!isRequestFragment){
                        createFragment(networkMap, "NetworkMap");

                    }else{
                        createFragment(requestNetMap, "RequestNetMap");

                    }

                    id(this);

                } else {
                    Toast.makeText(this, "This app requires location permissions to be granted.", Toast.LENGTH_LONG).show();
                    this.finish();
                }

                break;
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void createFragment(Fragment fragment, String name){
        fragmentManager = getSupportFragmentManager();

        FragmentTransaction transaction = fragmentManager.beginTransaction();

        transaction.add(R.id.container, fragment, name);

        transaction.commitAllowingStateLoss();
    }


    public void startFragment(Fragment fragment, String name){
        fragmentManager = getSupportFragmentManager();

        FragmentTransaction transaction = fragmentManager.beginTransaction();

        transaction.replace(R.id.container, fragment, name);

        transaction.commit();
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.find_wifi) {
            startFragment(networkMap, "NetworkMap" );

        } else if (id == R.id.request_wifi) {
            startFragment(requestNetMap, "RequestNetMap");

        } else if (id == R.id.nav_delete) {
            fab = findViewById(R.id.fab);
            fab.hide();

            Handler handler= new Handler();
            handler.postDelayed(new Runnable(){
                @Override
                public void run(){
                    fab.show();
                }
            }, 3500);

            View contextView = findViewById(android.R.id.content);

            Snackbar.make(contextView, R.string.confirm, Snackbar.LENGTH_LONG)
            .setAction(R.string.ok, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Respond to the click, such as by undoing the modification that caused
                    // this message to be displayed

                    Firebase myFirebaseRef = new Firebase("https://wisplit-2e9e8.firebaseio.com/");

                    final Firebase refNetwork = myFirebaseRef.child("networks");
                    final Firebase refNetworkLocation = myFirebaseRef.child("locations");
                    final Firebase refRequest = myFirebaseRef.child("requests");
                    final Firebase refRequestLocation = myFirebaseRef.child("locations_");


                    refNetwork.addListenerForSingleValueEvent(new ValueEventListener() {

                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            for (DataSnapshot ds : dataSnapshot.getChildren()){
                                Map data = (Map) ds.getValue();

                                String key= ds.getKey();

                                String id= id(getApplication());

                                String user= (String)(data.get("user"));

                                if(id.equals(user)){

                                    refNetwork.child(key).removeValue();
                                    refNetworkLocation.child(key).removeValue();


                                }
                            }

                            updateMarker("net");
                        }

                        @Override
                        public void onCancelled(FirebaseError firebaseError) { }
                    });

                    refRequest.addListenerForSingleValueEvent(new ValueEventListener() {

                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            for (DataSnapshot ds : dataSnapshot.getChildren()){
                                Map data = (Map) ds.getValue();

                                String key= ds.getKey();

                                String id= id(getApplication());

                                String user= (String)(data.get("user"));

                                if(id.equals(user)){

                                    refRequest.child(key).removeValue();
                                    refRequestLocation.child(key).removeValue();


                                }
                            }

                            updateMarker("req");


                            //Reset the network and request creation permission

                            SharedPreferences spNet = getApplicationContext().getSharedPreferences("PREF_NET", Context.MODE_PRIVATE);
                            spNet.edit().putBoolean("PREF_NET", true).commit();

                            SharedPreferences spReq = getApplicationContext().getSharedPreferences("PREF_REQ", Context.MODE_PRIVATE);
                            spReq.edit().putBoolean("PREF_REQ", true).commit();
                        }

                        @Override
                        public void onCancelled(FirebaseError firebaseError) { }
                    });

                }

            }).show();



        } else if (id == R.id.nav_share) {
            final String appPackageName = getPackageName(); // package name of the app

            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            String shareBody = "A lot of savings in you way!\nI recommend you this app so you can start sharing WiFi and splitting bills:\n\n" + "https://play.google.com/store/apps/details?id=" + appPackageName;
            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Subject Here");
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(sharingIntent, "Share via"));
        } else if (id == R.id.nav_rate){
            final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object

            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
            } catch (android.content.ActivityNotFoundException ex) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
            }
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void updateMarker(String id){
        String name= getCurrentFragment();

        if(name.equals("NetworkMap")){

            if(id.equals("net")){
                networkMap = new NetworkMap();

                startFragment(networkMap, name);
            }

        }else{

            if(id.equals("req")){
                requestNetMap= new RequestNetMap();

                startFragment(requestNetMap, name);
            }
        }
    }

    private String getCurrentFragment(){
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.container);
        return currentFragment.getTag();

    }

    public static boolean isNetworkAvailable(Context con) {
        try {
            ConnectivityManager cm = (ConnectivityManager) con
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();

            if (networkInfo != null && networkInfo.isConnected()) {
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public synchronized static String id(Context context) {
        if (uniqueID == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(PREF_UNIQUE_ID, Context.MODE_PRIVATE);
            uniqueID = sharedPrefs.getString(PREF_UNIQUE_ID, null);

            if (uniqueID == null) {
                uniqueID = UUID.randomUUID().toString();
                sharedPrefs.edit().putString(PREF_UNIQUE_ID, uniqueID).commit();
            }
        }

        //Log.e(PREF_UNIQUE_ID, uniqueID);

        return uniqueID;
    }

}

//Share you WiFi and split the bill

//        TelephonyManager telephonyManager = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
//        String countryCode= telephonyManager.getNetworkCountryIso();
//
//        Locale locale = Locale.getDefault();
//        Currency currency = Currency.getInstance(locale);
//        String symbol = currency.getSymbol();
//
//        Log.e("TAG", symbol);