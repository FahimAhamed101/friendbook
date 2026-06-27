package com.app.myfriend.story;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.iceteck.silicompressorr.SiliCompressor;
import com.app.myfriend.MainActivity;
import com.app.myfriend.R;
import com.app.myfriend.faceFilters.FaceFilters;
import com.app.myfriend.send.ImageEditingActivity;
import com.app.myfriend.send.VideoEditingActivity;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Objects;

@SuppressWarnings("ALL")
public class AddStoryActivity extends AppCompatActivity {

    private static final String TAG = "AddStoryActivity";
    private static final int IMAGE_PICKER_SELECT = 1000;

    Uri selectedMediaUri;
    ImageView image;
    VideoView video;
    String type = "";

    @SuppressLint("IntentReset")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_add_story);

        image = findViewById(R.id.image);
        video = findViewById(R.id.videoView);

        findViewById(R.id.back).setOnClickListener(v -> {
            Intent i = new Intent(AddStoryActivity.this, MainActivity.class);
            startActivity(i);
            finish();
        });

        findViewById(R.id.camera).setOnClickListener(v -> startActivity(new Intent(AddStoryActivity.this, FaceFilters.class)));

        findViewById(R.id.gallery).setOnClickListener(v -> {
            Intent pickIntent = new Intent(Intent.ACTION_PICK);
            pickIntent.setType("*/*");
            String[] mimeTypes = {"image/*", "video/*"};
            pickIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            startActivityForResult(pickIntent, IMAGE_PICKER_SELECT);
        });

        findViewById(R.id.edit).setOnClickListener(v -> {
            if (selectedMediaUri == null || type.isEmpty()) return;
            Intent i;
            if (type.equals("image")) {
                i = new Intent(AddStoryActivity.this, ImageEditingActivity.class);
            } else {
                i = new Intent(AddStoryActivity.this, VideoEditingActivity.class);
            }
            i.setData(selectedMediaUri);
            i.putExtra("type", "story");
            i.putExtra("uri", selectedMediaUri.toString());
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(i);
        });

        if (getIntent().hasExtra("type") && getIntent().hasExtra("uri")) {
            String uriStr = getIntent().getStringExtra("uri");
            selectedMediaUri = Uri.parse(uriStr);
            type = getIntent().getStringExtra("type");

            if ("image".equals(type)) {
                image.setVisibility(View.VISIBLE);
                video.setVisibility(View.GONE);
                Picasso.get().load(selectedMediaUri).into(image);
                findViewById(R.id.edit).setVisibility(View.VISIBLE);
            } else if ("video".equals(type)) {
                video.setVisibility(View.VISIBLE);
                image.setVisibility(View.GONE);
                findViewById(R.id.edit).setVisibility(View.VISIBLE);
                video.setVideoURI(selectedMediaUri);
                video.start();
                video.setOnPreparedListener(mp -> {
                    mp.setLooping(true);
                    mp.setVolume(0, 0);
                });
            }
        }

        findViewById(R.id.post).setOnClickListener(v -> {
            if (selectedMediaUri != null && !type.isEmpty()) {
                if (type.equals("image")) {
                    uploadImage();
                    Snackbar.make(v, "Uploading image...", Snackbar.LENGTH_SHORT).show();
                } else if (type.equals("video")) {
                    compressVideo();
                    Snackbar.make(v, "Compressing video...", Snackbar.LENGTH_SHORT).show();
                }
            } else {
                Snackbar.make(v, "Please select an image or video first", Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void compressVideo() {
        try {
            File outputDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
            new CompressVideo().execute("false", selectedMediaUri.toString(), outputDir.getPath());
        } catch (Exception e) {
            Log.e(TAG, "Compression setup failed", e);
            Toast.makeText(this, "Failed to start compression", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class CompressVideo extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... strings) {
            try {
                Uri mUri = Uri.parse(strings[1]);
                return SiliCompressor.with(AddStoryActivity.this).compressVideo(mUri, strings[2]);
            } catch (URISyntaxException e) {
                Log.e(TAG, "Video compression error", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s != null) {
                uploadVideo(Uri.fromFile(new File(s)));
            } else {
                Toast.makeText(AddStoryActivity.this, "Compression failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadVideo(Uri videoUri) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Story").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        String storyId = reference.push().getKey();
        long timeend = System.currentTimeMillis() + 86400000;

        String filePathAndName = "Story/Story_" + System.currentTimeMillis();
        FirebaseStorage.getInstance().getReference().child(filePathAndName).putFile(videoUri).addOnSuccessListener(taskSnapshot -> {
            taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(uri -> {
                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("imageUri", uri.toString());
                hashMap.put("timestart", ServerValue.TIMESTAMP);
                hashMap.put("timeend", timeend);
                hashMap.put("storyid", storyId);
                hashMap.put("type", "video");
                hashMap.put("userid", FirebaseAuth.getInstance().getCurrentUser().getUid());

                reference.child(storyId).setValue(hashMap).addOnCompleteListener(task -> {
                    Toast.makeText(AddStoryActivity.this, "Story uploaded", Toast.LENGTH_SHORT).show();
                    finish();
                });
            });
        }).addOnFailureListener(e -> Toast.makeText(AddStoryActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void uploadImage() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Story").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        String storyId = reference.push().getKey();
        long timeend = System.currentTimeMillis() + 86400000;

        String filePathAndName = "Story/Story_" + System.currentTimeMillis();
        FirebaseStorage.getInstance().getReference().child(filePathAndName).putFile(selectedMediaUri).addOnSuccessListener(taskSnapshot -> {
            taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(uri -> {
                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("imageUri", uri.toString());
                hashMap.put("timestart", ServerValue.TIMESTAMP);
                hashMap.put("timeend", timeend);
                hashMap.put("storyid", storyId);
                hashMap.put("type", "image");
                hashMap.put("userid", FirebaseAuth.getInstance().getCurrentUser().getUid());

                reference.child(storyId).setValue(hashMap).addOnCompleteListener(task -> {
                    Toast.makeText(AddStoryActivity.this, "Story uploaded", Toast.LENGTH_SHORT).show();
                    finish();
                });
            });
        }).addOnFailureListener(e -> Toast.makeText(AddStoryActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == IMAGE_PICKER_SELECT && data != null) {
            selectedMediaUri = data.getData();
            if (selectedMediaUri == null) return;

            String mimeType = getContentResolver().getType(selectedMediaUri);
            if (mimeType == null) {
                String extension = MimeTypeMap.getFileExtensionFromUrl(selectedMediaUri.toString());
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            }

            if (mimeType != null && mimeType.startsWith("image")) {
                image.setVisibility(View.VISIBLE);
                video.setVisibility(View.GONE);
                Picasso.get().load(selectedMediaUri).into(image);
                type = "image";
                findViewById(R.id.edit).setVisibility(View.VISIBLE);
            } else if (mimeType != null && mimeType.startsWith("video")) {
                handleVideoSelection(selectedMediaUri);
            }
        }
    }

    private void handleVideoSelection(Uri uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(this, uri);
            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long timeInMilli = Long.parseLong(time);
            retriever.release();

            if (timeInMilli > 30000) {
                Snackbar.make(findViewById(R.id.videoView), "Video must be 30 seconds or less", Snackbar.LENGTH_LONG).show();
            } else {
                video.setVisibility(View.VISIBLE);
                image.setVisibility(View.GONE);
                findViewById(R.id.edit).setVisibility(View.VISIBLE);
                type = "video";
                video.setVideoURI(uri);
                video.start();
                video.setOnPreparedListener(mp -> {
                    mp.setLooping(true);
                    mp.setVolume(0, 0);
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing video", e);
            Toast.makeText(this, "Error processing video", Toast.LENGTH_SHORT).show();
        }
    }
}
