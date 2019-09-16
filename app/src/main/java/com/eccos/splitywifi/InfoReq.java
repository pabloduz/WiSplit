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

public class InfoReq extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getRequest();

        sleep(900);

        setContentView(R.layout.activity_info_req);
        getSupportActionBar().hide();
    }

    private void getRequest() {
        final String tag = getIntent().getStringExtra("tag");
        //Log.e("TAG", tag);

        Firebase myFirebaseRef = new Firebase("https://wisplit-2e9e8.firebaseio.com/");
        Firebase ref = myFirebaseRef.child("requests").child(tag);


        ref.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                        //Log.e("TAG", dataSnapshot.getKey());

                        Map data = (Map) dataSnapshot.getValue();

                        String name = (String) (data.get("name"));
                        String phone = (String) (data.get("phone"));
                        String email = (String) (data.get("email"));
                        String price = (String) (data.get("price"));

                        final TextView tvName = (TextView) findViewById(R.id.name);
                        final TextView tvPhone = (TextView) findViewById(R.id.phone);
                        final TextView tvEmail = (TextView) findViewById(R.id.email);
                        final TextView tvPrice = (TextView) findViewById(R.id.price);

                        //Setting Firebase values to Text Views
                        tvName.setText(name); tvPhone.setText(phone); tvEmail.setText(email); tvPrice.setText(price);

                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) { }
        });
    }
}
