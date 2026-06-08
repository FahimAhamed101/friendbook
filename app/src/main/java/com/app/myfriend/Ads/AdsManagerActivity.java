package com.app.myfriend.Ads;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.app.myfriend.NightMode;
import com.app.myfriend.R;
import com.app.myfriend.adapter.AdapterAdsPost;
import com.app.myfriend.model.ModelAdsPost;

import java.util.ArrayList;
import java.util.List;

public class AdsManagerActivity extends AppCompatActivity {
    RecyclerView rv;

    //Post
    AdapterAdsPost adapterUsers;
    List<ModelAdsPost> userList;

    NightMode sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPref = new NightMode(this);
        if (sharedPref.loadNightModeState().equals("night")){
            setTheme(R.style.DarkTheme);
        }else if (sharedPref.loadNightModeState().equals("dim")){
            setTheme(R.style.DimTheme);
        }else setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ads_manager);

        findViewById(R.id.imageView).setOnClickListener(v -> onBackPressed());

        findViewById(R.id.create).setOnClickListener(v -> {
            Intent intent = new Intent(AdsManagerActivity.this, CreateAdsActivity.class);
            startActivity(intent);
        });

        rv = findViewById(R.id.rv);
        rv.setHasFixedSize(true);
        rv.setLayoutManager(new LinearLayoutManager(this));
        userList = new ArrayList<>();
        getMyAds();

    }

    private void getMyAds() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("AdsPost");
        Query query = ref.orderByChild("id").equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid());
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userList.clear();
                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    ModelAdsPost modelPost = ds.getValue(ModelAdsPost.class);
                    userList.add(modelPost);
                    adapterUsers = new AdapterAdsPost(getApplicationContext(), userList);
                    rv.setAdapter(adapterUsers);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }
}