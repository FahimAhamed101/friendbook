package com.app.myfriend.pages;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.app.myfriend.NightMode;
import com.app.myfriend.R;
import com.app.myfriend.adapter.AdapterPagesPost;
import com.app.myfriend.model.ModelPostGroup;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class PagesProfileActivity extends AppCompatActivity {

    String pageId = "";

    NightMode sharedPref;

    RecyclerView post;
    AdapterPagesPost adapterPost;
    List<ModelPostGroup> modelPosts;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPref = new NightMode(this);
        if (sharedPref.loadNightModeState().equals("night")){
            setTheme(R.style.DarkTheme);
        }else if (sharedPref.loadNightModeState().equals("dim")){
            setTheme(R.style.DimTheme);
        }else setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pages_profile);
        pageId = getIntent().getStringExtra("id");

        findViewById(R.id.edit).setOnClickListener(view -> onBackPressed());

        post = findViewById(R.id.post);
        post.setLayoutManager(new LinearLayoutManager(PagesProfileActivity.this));
        modelPosts = new ArrayList<>();
        getAllPost();

        TextView posts = findViewById(R.id.posts);
        TextView followers = findViewById(R.id.followers);

        FirebaseDatabase.getInstance().getReference("Pages").child(pageId).child("Posts").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                posts.setText(""+dataSnapshot.getChildrenCount());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        FirebaseDatabase.getInstance().getReference().child("Pages").child(pageId).child("Follow").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                followers.setText(""+dataSnapshot.getChildrenCount());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        ImageView photo = findViewById(R.id.dp);
        ImageView cover = findViewById(R.id.cover);
        TextView name = findViewById(R.id.name);
        TextView username = findViewById(R.id.topName);
        TextView bio = findViewById(R.id.username);
        TextView link = findViewById(R.id.link);

        findViewById(R.id.ediProfile).setOnClickListener(view -> {
            Intent intent = new Intent(this, EditPagesActivity.class);
            intent.putExtra("id", pageId);
            startActivity(intent);
        });

        findViewById(R.id.create_post).setOnClickListener(view -> {
            Intent intent = new Intent(this, CreatePostPagesActivity.class);
            intent.putExtra("id", pageId);
            startActivity(intent);
        });

        FirebaseDatabase.getInstance().getReference().child("Pages").child(pageId).child("Follow").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).exists()){
                    findViewById(R.id.follow).setVisibility(View.GONE);
                    findViewById(R.id.unfollow).setVisibility(View.VISIBLE);
                }else {
                    findViewById(R.id.follow).setVisibility(View.VISIBLE);
                    findViewById(R.id.unfollow).setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        findViewById(R.id.follow).setOnClickListener(v -> {
            FirebaseDatabase.getInstance().getReference().child("Pages").child(pageId).child("Follow").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(true);
            findViewById(R.id.follow).setVisibility(View.GONE);
            findViewById(R.id.unfollow).setVisibility(View.VISIBLE);
            FirebaseDatabase.getInstance().getReference("FollowPages").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child(pageId).child(pageId).setValue(true);

        });

        findViewById(R.id.unfollow).setOnClickListener(v -> {
            FirebaseDatabase.getInstance().getReference().child("Pages").child(pageId).child("Follow").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).getRef().removeValue();
            findViewById(R.id.follow).setVisibility(View.VISIBLE);
            findViewById(R.id.unfollow).setVisibility(View.GONE);
            FirebaseDatabase.getInstance().getReference("FollowPages").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child(pageId).getRef().removeValue();
        });

        FirebaseDatabase.getInstance().getReference().child("Pages").child(pageId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){

                    if (!snapshot.child("photo").getValue().toString().isEmpty()){
                        Picasso.get().load(snapshot.child("photo").getValue().toString()).into(photo);
                    }

                    if (!snapshot.child("cover").getValue().toString().isEmpty()){
                        Picasso.get().load(snapshot.child("cover").getValue().toString()).into(cover);
                    }

                    name.setText(snapshot.child("name").getValue().toString());
                    username.setText(snapshot.child("username").getValue().toString());
                    bio.setText(snapshot.child("bio").getValue().toString());
                    link.setText(snapshot.child("link").getValue().toString());

                    if (snapshot.child("bio").getValue().toString().isEmpty()){
                        findViewById(R.id.bioLy).setVisibility(View.GONE);
                    }

                    if (snapshot.child("link").getValue().toString().isEmpty()){
                        findViewById(R.id.link_layout).setVisibility(View.GONE);
                    }

                    if (snapshot.child("id").getValue().toString().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                        findViewById(R.id.follow).setVisibility(View.GONE);
                        findViewById(R.id.unfollow).setVisibility(View.GONE);
                        findViewById(R.id.ediProfile).setVisibility(View.VISIBLE);
                        findViewById(R.id.create_post).setVisibility(View.VISIBLE);
                    }

                    findViewById(R.id.progressBar).setVisibility(View.GONE);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }
    private void getAllPost() {
        FirebaseDatabase.getInstance().getReference("Pages").child(pageId).child("Posts").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                modelPosts.clear();
                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    ModelPostGroup modelPost = ds.getValue(ModelPostGroup.class);
                    modelPosts.add(modelPost);
                    adapterPost = new AdapterPagesPost(PagesProfileActivity.this, modelPosts);
                    post.setAdapter(adapterPost);
                    findViewById(R.id.progressBar).setVisibility(View.GONE);
                    if (adapterPost.getItemCount() == 0){
                        findViewById(R.id.progressBar).setVisibility(View.GONE);
                        post.setVisibility(View.GONE);
                        findViewById(R.id.nothing).setVisibility(View.VISIBLE);
                    }else {
                        findViewById(R.id.progressBar).setVisibility(View.GONE);
                        post.setVisibility(View.VISIBLE);
                        findViewById(R.id.nothing).setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}