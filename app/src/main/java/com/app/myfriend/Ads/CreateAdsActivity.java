package com.app.myfriend.Ads;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.hendraanggrian.appcompat.widget.SocialEditText;
import com.app.myfriend.NightMode;
import com.app.myfriend.R;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class CreateAdsActivity extends AppCompatActivity {

    private Uri image_uri, video_uri;
    private static final int IMAGE_PICK_CODE = 1000;
    private static final int PERMISSION_CODE = 1001;
    private static final int PICK_VIDEO_REQUEST = 1;
    String type = "none";
    MediaController mediaController;

    ImageView postImage;
    VideoView postVideo;
    ImageView delete;
    ProgressBar pb;

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
        setContentView(R.layout.activity_create_ads);
        //Id
        ImageView back  = findViewById(R.id.imageView);
        CircleImageView dp = findViewById(R.id.circleImageView3);
        SocialEditText editText = findViewById(R.id.postText);
        Button postButton = findViewById(R.id.button2);
        ImageView uImage = findViewById(R.id.uImage);
        ImageView uVideo = findViewById(R.id.uVideo);
        delete = findViewById(R.id.delete);
        postImage = findViewById(R.id.image);
        postVideo = findViewById(R.id.video);
        pb = findViewById(R.id.pb);

        //HideControls
        mediaController = new MediaController(this);
        postVideo.setMediaController(mediaController);
        mediaController.setAnchorView(postVideo);
        MediaController ctrl = new MediaController(CreateAdsActivity.this);
        ctrl.setVisibility(View.GONE);
        postVideo.setMediaController(ctrl);
        postVideo.setOnPreparedListener(mp -> mp.setLooping(true));

        //Post
        postButton.setOnClickListener(v -> {

            pb.setVisibility(View.VISIBLE);
            String postText = Objects.requireNonNull(editText.getText()).toString().trim();
            if (postText.isEmpty()){
                pb.setVisibility(View.GONE);

                Toast.makeText(this, "Enter caption", Toast.LENGTH_SHORT).show();

            }  else {
                switch (type) {
                    case "none": {
                        String timeStamp = String.valueOf(System.currentTimeMillis());
                        HashMap<Object, String> hashMap = new HashMap<>();
                        hashMap.put("id", FirebaseAuth.getInstance().getCurrentUser().getUid());
                        hashMap.put("pId", timeStamp);
                        hashMap.put("text", postText);
                        hashMap.put("pViews", "0");
                        hashMap.put("type", "text");
                        hashMap.put("expiry", "no");
                        hashMap.put("video", "no");
                        hashMap.put("image", "no");
                        hashMap.put("pTime", timeStamp);
                        DatabaseReference dRef = FirebaseDatabase.getInstance().getReference("AdsPost");
                        dRef.child(timeStamp).setValue(hashMap).addOnSuccessListener(aVoid -> {
                            editText.setText("");
                            pb.setVisibility(View.GONE);

                            Toast.makeText(this, "Posted", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(CreateAdsActivity.this, AdsManagerActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();

                        });
                        break;
                    }
                    case "image": {
                        String timeStamp = String.valueOf(System.currentTimeMillis());
                        String filePathAndName = "Post/" + "Post_" + timeStamp;
                        StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathAndName);
                        ref.putFile(image_uri)
                                .addOnSuccessListener(taskSnapshot -> {
                                    Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                                    while (!uriTask.isSuccessful()) ;
                                    String downloadUri = Objects.requireNonNull(uriTask.getResult()).toString();
                                    if (uriTask.isSuccessful()) {
                                        String timeStamp12 = String.valueOf(System.currentTimeMillis());
                                        HashMap<Object, String> hashMap = new HashMap<>();
                                        hashMap.put("id", FirebaseAuth.getInstance().getCurrentUser().getUid());
                                        hashMap.put("pId", timeStamp12);
                                        hashMap.put("text", postText);
                                        hashMap.put("pViews", "0");
                                        hashMap.put("type", "image");
                                        hashMap.put("expiry", "no");
                                        hashMap.put("video", "no");
                                        hashMap.put("image", downloadUri);
                                        hashMap.put("pTime", timeStamp12);
                                        DatabaseReference dRef = FirebaseDatabase.getInstance().getReference("AdsPost");
                                        dRef.child(timeStamp12).setValue(hashMap).addOnSuccessListener(aVoid -> {
                                            editText.setText("");
                                            pb.setVisibility(View.GONE);
                                            postImage.setImageURI(null);
                                            type = "none";
                                            delete.setVisibility(View.GONE);

                                            Toast.makeText(this, "Posted", Toast.LENGTH_SHORT).show();

                                            Intent intent = new Intent(CreateAdsActivity.this, AdsManagerActivity.class);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                            startActivity(intent);
                                            finish();

                                        });
                                    }
                                });

                        break;
                    }
                    case "video": {
                        String timeStamp = String.valueOf(System.currentTimeMillis());
                        String filePathAndName = "Post/" + "Post_" + timeStamp;
                        StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathAndName);
                        ref.putFile(video_uri)
                                .addOnSuccessListener(taskSnapshot -> {
                                    Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                                    while (!uriTask.isSuccessful()) ;
                                    String downloadUri = Objects.requireNonNull(uriTask.getResult()).toString();
                                    if (uriTask.isSuccessful()) {
                                        String timeStamp1 = String.valueOf(System.currentTimeMillis());
                                        HashMap<Object, String> hashMap = new HashMap<>();
                                        hashMap.put("id", FirebaseAuth.getInstance().getCurrentUser().getUid());
                                        hashMap.put("pId", timeStamp1);
                                        hashMap.put("text", postText);
                                        hashMap.put("pViews", "0");
                                        hashMap.put("type", "video");
                                        hashMap.put("expiry", "no");
                                        hashMap.put("video", downloadUri);
                                        hashMap.put("image", "no");
                                        hashMap.put("pTime", timeStamp1);
                                        DatabaseReference dRef = FirebaseDatabase.getInstance().getReference("AdsPost");
                                        dRef.child(timeStamp1).setValue(hashMap).addOnSuccessListener(aVoid -> {
                                            editText.setText("");
                                            postVideo.setVisibility(View.GONE);
                                            type = "none";
                                            pb.setVisibility(View.GONE);
                                            delete.setVisibility(View.GONE);

                                            Toast.makeText(this, "Posted", Toast.LENGTH_SHORT).show();

                                            Intent intent = new Intent(CreateAdsActivity.this, AdsManagerActivity.class);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                            startActivity(intent);
                                            finish();

                                        });
                                    }
                                });
                        break;
                    }
                }
            }

        });

        //OnClick
        back.setOnClickListener(v -> onBackPressed());
        uImage.setOnClickListener(v -> {
            switch (type) {
                case "none":
                    //Check Permission
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES)
                                == PackageManager.PERMISSION_DENIED) {
                            String[] permissions = {Manifest.permission.READ_MEDIA_IMAGES};
                            requestPermissions(permissions, PERMISSION_CODE);
                        } else {
                            pickImageFromGallery();
                        }
                    } else {
                        pickImageFromGallery();
                    }
                    break;
                case "image":
                    postImage.setImageURI(null);
                    delete.setVisibility(View.GONE);
                    //Check Permission
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES)
                                == PackageManager.PERMISSION_DENIED) {
                            String[] permissions = {Manifest.permission.READ_MEDIA_IMAGES};
                            requestPermissions(permissions, PERMISSION_CODE);
                        } else {
                            pickImageFromGallery();
                        }
                    } else {
                        pickImageFromGallery();
                    }
                    break;
                case "video":
                    postVideo.setVisibility(View.GONE);
                    delete.setVisibility(View.GONE);
                    //Check Permission
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES)
                                == PackageManager.PERMISSION_DENIED) {
                            String[] permissions = {Manifest.permission.READ_MEDIA_IMAGES};
                            requestPermissions(permissions, PERMISSION_CODE);
                        } else {
                            pickImageFromGallery();
                        }
                    } else {
                        pickImageFromGallery();
                    }
                    break;
            }
        });
        uVideo.setOnClickListener(v -> {
            switch (type) {
                case "none":
                    //Check Permission
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES)
                                == PackageManager.PERMISSION_DENIED) {
                            String[] permissions = {Manifest.permission.READ_MEDIA_IMAGES};
                            requestPermissions(permissions, PERMISSION_CODE);
                        } else {
                            chooseVideo();
                        }
                    } else {
                        chooseVideo();
                    }
                    break;
                case "image":
                    postImage.setImageURI(null);
                    delete.setVisibility(View.GONE);
                    type = "none";
                    //Check Permission
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES)
                                == PackageManager.PERMISSION_DENIED) {
                            String[] permissions = {Manifest.permission.READ_MEDIA_IMAGES};
                            requestPermissions(permissions, PERMISSION_CODE);
                        } else {
                            chooseVideo();
                        }
                    } else {
                        chooseVideo();
                    }
                    break;
                case "video":
                    postVideo.setVisibility(View.GONE);
                    type = "none";
                    delete.setVisibility(View.GONE);
                    //Check Permission
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES)
                                == PackageManager.PERMISSION_DENIED) {
                            String[] permissions = {Manifest.permission.READ_MEDIA_IMAGES};
                            requestPermissions(permissions, PERMISSION_CODE);
                        } else {
                            chooseVideo();
                        }
                    } else {
                        chooseVideo();
                    }
                    break;
            }

        });

        delete.setOnClickListener(v -> {
            if (type.equals("image")){
                postImage.setImageURI(null);
                type = "none";
                delete.setVisibility(View.GONE);
            } else if (type.equals("video")) {
                postVideo.setVisibility(View.GONE);
                type = "none";
                delete.setVisibility(View.GONE);
            }
        });

        //SetDp
        FirebaseDatabase.getInstance().getReference("Users").child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String mDp = Objects.requireNonNull(snapshot.child("photo").getValue()).toString();
                if (!mDp.isEmpty()){
                    Picasso.get().load(mDp).placeholder(R.drawable.avatar).into(dp);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

    }

    //ImagePick
    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_CODE);
    }

    //VideoPick
    private void chooseVideo() {
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_VIDEO_REQUEST);
    }

    //Permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(this, "Storage Permission Allowed", Toast.LENGTH_SHORT).show();

            } else {
                Toast.makeText(this, "Storage Permission is Required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //callBack
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == IMAGE_PICK_CODE   && data != null && data.getData() != null){
            image_uri = data.getData();
            postImage.setImageURI(image_uri);
            type = "image";
            postImage.setVisibility(View.VISIBLE);
            delete.setVisibility(View.VISIBLE);
        }
        if (requestCode == PICK_VIDEO_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            video_uri = data.getData();
            postVideo.setVideoURI(video_uri);
            postVideo.start();
            type = "video";
            postVideo.setVisibility(View.VISIBLE);
            delete.setVisibility(View.VISIBLE);
        }
    }

}