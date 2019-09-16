package com.eccos.splitywifi;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.Map;

import static android.os.SystemClock.sleep;

public class InfoNet extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getNetwork();

        sleep(900);

        setContentView(R.layout.activity_info_net);
        getSupportActionBar().hide();
    }

    private void getNetwork() {
        final String tag = getIntent().getStringExtra("tag");
        //Log.e("TAG", tag);

        Firebase myFirebaseRef = new Firebase("https://wisplit-2e9e8.firebaseio.com/");
        Firebase ref = myFirebaseRef.child("networks").child(tag);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    //Log.e("TAG", dataSnapshot.getKey());

                    Map data = (Map) dataSnapshot.getValue();

                    String name = (String) (data.get("name"));
                    String phone = (String) (data.get("phone"));
                    String email = (String) (data.get("email"));
                    String wifi = (String) (data.get("wifi"));
                    String price = (String) (data.get("price"));

                    final TextView tvName = (TextView) findViewById(R.id.name);
                    final TextView tvPhone = (TextView) findViewById(R.id.phone);
                    final TextView tvEmail = (TextView) findViewById(R.id.email);
                    final TextView tvWifi = (TextView) findViewById(R.id.wifi_name);
                    final TextView tvPrice = (TextView) findViewById(R.id.price);

                    //Setting Firebase values to Text Views
                    tvName.setText(name); tvPhone.setText(phone); tvEmail.setText(email); tvWifi.setText(wifi); tvPrice.setText(price);
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) { }
        });
    }
}
