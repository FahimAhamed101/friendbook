package com.app.myfriend;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.app.myfriend.calling.RingingActivity;
import com.app.myfriend.faceFilters.FaceFilters;
import com.app.myfriend.fragment.ChatFragment;
import com.app.myfriend.fragment.HomeFragment;
import com.app.myfriend.fragment.ProfileFragment;
import com.app.myfriend.group.GroupProfileActivity;
import com.app.myfriend.groupVideoCall.RingingGroupVideoActivity;
import com.app.myfriend.groupVoiceCall.RingingGroupVoiceActivity;
import com.app.myfriend.live.activities.GoBroadcastActivity;
import com.app.myfriend.marketPlace.PostProductActivity;
import com.app.myfriend.marketPlace.ProductDetailsActivity;
import com.app.myfriend.meeting.MeetingActivity;
import com.app.myfriend.menu.InviteActivity;
import com.app.myfriend.menu.TranslationActivity;
import com.app.myfriend.notifications.Token;
import com.app.myfriend.pages.PagesActivity;
import com.app.myfriend.podcast.GoPodcastBroadcastActivity;
import com.app.myfriend.post.CommentActivity;
import com.app.myfriend.post.CreatePostActivity;
import com.app.myfriend.profile.UserProfileActivity;
import com.app.myfriend.reel.ReelActivity;
import com.app.myfriend.reel.VideoEditActivity;
import com.app.myfriend.reel.ViewReelActivity;
import com.app.myfriend.story.AddStoryActivity;
import com.app.myfriend.watchParty.StartWatchPartyActivity;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;


@SuppressWarnings("ALL")
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    BottomNavigationView bottomNavigationView;
    Fragment selectedFragment = null;

    SharedMode sharedMode;

    //Bottom
    BottomSheetDialog more;
    LinearLayout post,reel,party,camera,meeting,live,podcast,sell,stories;

    //Permission
    private static final int VIDEO_PICK_CODE = 1002;
    private static final int PERMISSION_CODE = 1001; 

    private static final int PERMISSION_REQ_ID = 22;

    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
                      Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    NightMode sharedPref;

    long startTime;
    long endTime;


    @Override
    protected void onStart() {
        super.onStart();
        startTime = System.currentTimeMillis();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", (dialog, id) -> {
                    endTime = System.currentTimeMillis();
                    long timeSpend = endTime - startTime;
                    if (timeSpend > 200000){
                        addMoney();
                    }
                    MainActivity.super.onBackPressed();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void addMoney() {

        FirebaseDatabase.getInstance().getReference("Balance").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double add = Double.parseDouble(Objects.requireNonNull(snapshot.child("balance").getValue()).toString()) + 0.01;
                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("balance", String.valueOf(add));
                FirebaseDatabase.getInstance().getReference("Balance").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).updateChildren(hashMap);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPref = new NightMode(this);
        if (sharedPref.loadNightModeState().equals("night")){
            setTheme(R.style.DarkTheme);
        }else if (sharedPref.loadNightModeState().equals("dim")){
            setTheme(R.style.DimTheme);
        }else setTheme(R.style.AppTheme);
        sharedMode = new SharedMode(this);
        if (!sharedMode.loadNightModeState().isEmpty()){
            setApplicationLocale(sharedMode.loadNightModeState());
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


//        9.LOGO

        Uri uri = getIntent().getData();
        if (uri != null){
            List<String> params = uri.getPathSegments();
            String id = params.get(params.size() - 1);
            if (uri.toString().contains("user")){
                FirebaseDatabase.getInstance().getReference("Users").child(id).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (snapshot.exists()){
                            Intent intent = new Intent(MainActivity.this, UserProfileActivity.class);
                            intent.putExtra("hisUID", id);
                            startActivity(intent);
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }else if (uri.toString().contains("group")){
                FirebaseDatabase.getInstance().getReference("Groups").child(id).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (snapshot.exists()){
                            Intent intent = new Intent(MainActivity.this, GroupProfileActivity.class);
                            intent.putExtra("group", id);
                            intent.putExtra("type", "");
                            startActivity(intent);
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }else if (uri.toString().contains("post")){
                FirebaseDatabase.getInstance().getReference("Posts").child(id).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (snapshot.exists()){
                            Intent intent = new Intent(MainActivity.this, CommentActivity.class);
                            intent.putExtra("postID", id);
                            startActivity(intent);
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }else if (uri.toString().contains("reel")){
                FirebaseDatabase.getInstance().getReference("Reels").child(id).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (snapshot.exists()){
                            Intent intent = new Intent(MainActivity.this, ViewReelActivity.class);
                            intent.putExtra("id", id);
                            startActivity(intent);
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
            else if (uri.toString().contains("product")){
                FirebaseDatabase.getInstance().getReference("Product").child(id).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (snapshot.exists()){
                            Intent intent = new Intent(MainActivity.this, ProductDetailsActivity.class);
                            intent.putExtra("pId", id);
                            startActivity(intent);
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        }


        FirebaseDatabase.getInstance().getReference().child("Balance").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()){
                    HashMap<String, Object> hashMap = new HashMap<>();
                    hashMap.put("balance", "0");
                    FirebaseDatabase.getInstance().getReference().child("Balance").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(hashMap);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(navigationSelected);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                new HomeFragment()).commit();

        getGroupCall();
        getGroupCallVideo();
        addPost();

        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[2], PERMISSION_REQ_ID)) {
        }

        updateToken(FirebaseInstanceId.getInstance().getToken());

        //Call
        Query query = FirebaseDatabase.getInstance().getReference().child("calling").orderByChild("to").equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid());
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    for (DataSnapshot ds : snapshot.getChildren()){
                        if (ds.child("type").getValue().toString().equals("calling")){
                            Intent intent = new Intent(MainActivity.this, RingingActivity.class);
                            intent.putExtra("room", ds.child("room").getValue().toString());
                            intent.putExtra("from", ds.child("from").getValue().toString());
                            intent.putExtra("call", ds.child("call").getValue().toString());
                            startActivity(intent);
                            finish();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void getGroupCallVideo() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    if (ds.child("Participants").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).exists()){
                        for (DataSnapshot dataSnapshot1 : ds.child("Video").getChildren()){
                            if (dataSnapshot1.child("type").getValue().toString().equals("calling")){

                                if (!dataSnapshot1.child("from").getValue().toString().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                                    if (!dataSnapshot1.child("end").hasChild(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                                        if (!dataSnapshot1.child("ans").hasChild(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                                            Intent intent = new Intent(MainActivity.this, RingingGroupVideoActivity.class);
                                            intent.putExtra("room", dataSnapshot1.child("room").getValue().toString());
                                            intent.putExtra("group", ds.child("groupId").getValue().toString());
                                            startActivity(intent);
                                            finish();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    @SuppressLint("NonConstantResourceId")
    private final BottomNavigationView.OnNavigationItemSelectedListener navigationSelected =
            item -> {
                switch (item.getItemId()){
                    case R.id.nav_home:
                        selectedFragment = new HomeFragment();

                        break;
                    case R.id.nav_add:
                        more.show();
                        break;
                    case R.id.nav_reels:

                        Intent intent = new Intent(MainActivity.this, ReelActivity.class);
                        startActivity(intent);

                        break;
                    case R.id.nav_chat:
                        selectedFragment = new ChatFragment();
                        break;
                    case R.id.nav_user:
                        selectedFragment = new ProfileFragment();
                        break;
                }
                if (selectedFragment != null){
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                            selectedFragment).commit();
                }
                return true;
            };

    private void getGroupCall() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    if (ds.child("Participants").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).exists()){
                        for (DataSnapshot dataSnapshot1 : ds.child("Voice").getChildren()){
                            if (dataSnapshot1.child("type").getValue().toString().equals("calling")){
                                if (!dataSnapshot1.child("from").getValue().toString().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                                    if (!dataSnapshot1.child("end").hasChild(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                                        if (!dataSnapshot1.child("ans").hasChild(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                                            Intent intent = new Intent(MainActivity.this, RingingGroupVoiceActivity.class);
                                            intent.putExtra("room", dataSnapshot1.child("room").getValue().toString());
                                            intent.putExtra("group", ds.child("groupId").getValue().toString());
                                            startActivity(intent);
                                            finish();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void addPost() {
        if (more == null){
            @SuppressLint("InflateParams") View view = LayoutInflater.from(this).inflate(R.layout.add_bottom, null);
            post = view.findViewById(R.id.post);
            post.setOnClickListener(this);
            reel = view.findViewById(R.id.reel);
            reel.setOnClickListener(this);
            party = view.findViewById(R.id.party);
            party.setOnClickListener(this);
            meeting = view.findViewById(R.id.meeting);
            meeting.setOnClickListener(this);
            live = view.findViewById(R.id.live);
            live.setOnClickListener(this);
            podcast = view.findViewById(R.id.podcast);
            podcast.setOnClickListener(this);
            sell = view.findViewById(R.id.sell);
            sell.setOnClickListener(this);
            stories = view.findViewById(R.id.stories);
            stories.setOnClickListener(this);
            camera = view.findViewById(R.id.camera);
            camera.setOnClickListener(this);

            view.findViewById(R.id.pages).setOnClickListener(view1 -> {

                more.cancel();
                startActivity(new Intent(MainActivity.this, PagesActivity .class));

            });

            view.findViewById(R.id.referral).setOnClickListener(view1 -> {

                more.cancel();
                startActivity(new Intent(MainActivity.this, InviteActivity.class));

            });

            view.findViewById(R.id.translation).setOnClickListener(view1 -> {

                more.cancel();
                startActivity(new Intent(MainActivity.this, TranslationActivity.class));

            });

            more = new BottomSheetDialog(this);
            more.setContentView(view);
        }
    }

    private boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode);
            return false;
        }

        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.post:
                more.cancel();
                startActivity(new Intent(MainActivity.this, CreatePostActivity.class));
                break;
            case R.id.reel:
                more.cancel();
               selectReel();
                break;
            case R.id.party:
                more.cancel();
                startActivity(new Intent(MainActivity.this, StartWatchPartyActivity.class));
                break;
            case R.id.meeting:
                more.cancel();
                startActivity(new Intent(MainActivity.this, MeetingActivity.class));
                break;
            case R.id.live:
                more.cancel();
                String room = String.valueOf(System.currentTimeMillis());
                Query query = FirebaseDatabase.getInstance().getReference().child("Live").orderByChild("userid").equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid());
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()){
                            snapshot.getRef().removeValue();
                            Intent i = new Intent(getApplicationContext(), GoBroadcastActivity.class);
                            i.putExtra("name", room);
                            i.putExtra("type", "host");
                            startActivity(i);
                        }else {
                            Intent i = new Intent(getApplicationContext(), GoBroadcastActivity.class);
                            i.putExtra("name", room);
                            i.putExtra("type", "host");
                            startActivity(i);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
                break;
            case R.id.podcast:
                more.cancel();
                createPod();
                break;
            case R.id.sell:
                more.cancel();
                startActivity(new Intent(MainActivity.this, PostProductActivity.class));
                break;
            case R.id.stories:
                more.cancel();
                startActivity(new Intent(MainActivity.this, AddStoryActivity.class));
                break;
            case R.id.camera:
                more.cancel();
                startActivity(new Intent(MainActivity.this, FaceFilters.class));
                break;
        }
    }

    private void createPod() {
        String room = String.valueOf(System.currentTimeMillis());
        Query query = FirebaseDatabase.getInstance().getReference().child("Podcast").orderByChild("userid").equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    snapshot.getRef().removeValue();
                    Intent i = new Intent(getApplicationContext(), GoPodcastBroadcastActivity.class);
                    i.putExtra("name", room);
                    i.putExtra("type", "host");
                    startActivity(i);
                }else {
                    Intent i = new Intent(getApplicationContext(), GoPodcastBroadcastActivity.class);
                    i.putExtra("name", room);
                    i.putExtra("type", "host");
                    startActivity(i);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void selectReel() {

        //Check Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_DENIED){
                String[] permissions = {Manifest.permission.READ_MEDIA_IMAGES};
                requestPermissions(permissions, PERMISSION_CODE);
            }
            else {
                pickVideo();
            }
        }
        else {
            pickVideo();
        }

    }

    private void pickVideo() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("video/*");
        startActivityForResult(intent, VIDEO_PICK_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(findViewById(R.id.main), "Storage permission allowed", Snackbar.LENGTH_LONG).show();
                pickVideo();
            } else {
                Snackbar.make(findViewById(R.id.main), "Storage permission is required", Snackbar.LENGTH_LONG).show();
            }
        }

        if (requestCode == PERMISSION_REQ_ID) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED ||
                    grantResults[1] != PackageManager.PERMISSION_GRANTED ||
                    grantResults[2] != PackageManager.PERMISSION_GRANTED) {
//                showLongToast("Need permissions " + Manifest.permission.RECORD_AUDIO +
//                        "/" + Manifest.permission.CAMERA + "/" +
               // ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode);
            }

        }

    }

    private void showLongToast(final String msg) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(resultCode == RESULT_OK && requestCode == VIDEO_PICK_CODE && data != null){
            Uri video_uri = Objects.requireNonNull(data).getData();
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(getApplicationContext(), video_uri);
            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long timeInMilli = Long.parseLong(time);
            try {
                retriever.release();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (timeInMilli > 60000){
                Snackbar.make(findViewById(R.id.main), "Video must be of 1 minutes or less", Snackbar.LENGTH_LONG).show();
            }else {
                sendVideo(video_uri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void sendVideo(Uri videoUri) {
        Intent intent = new Intent(MainActivity.this, VideoEditActivity.class);
        intent.putExtra("uri", videoUri.toString());
        startActivity(intent);
    }

    private void updateToken(String token){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Tokens");
        Token mToken = new Token(token);
        ref.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(mToken);
    }

    private void setApplicationLocale(String locale) {
        Resources resources = getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        Configuration config = resources.getConfiguration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(new Locale(locale.toLowerCase()));
        } else {
            config.locale = new Locale(locale.toLowerCase());
        }
        resources.updateConfiguration(config, dm);
    }

}