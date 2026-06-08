package com.app.myfriend.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.makeramen.roundedimageview.RoundedImageView;
import com.app.myfriend.MediaViewActivity;
import com.app.myfriend.NightMode;
import com.app.myfriend.R;
import com.app.myfriend.model.ModelAdsPost;
import com.app.myfriend.profile.UserProfileActivity;
import com.app.myfriend.search.SearchActivity;
import com.squareup.picasso.Picasso;
import com.tylersuehr.socialtextview.SocialTextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;


public class AdapterAdsPost extends RecyclerView.Adapter<AdapterAdsPost.MyHolder>{

    final Context context;
    final List<ModelAdsPost> userList;
    NightMode nightMode;
    public AdapterAdsPost(Context context, List<ModelAdsPost> userList) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        nightMode = new NightMode(context);
        if (nightMode.loadNightModeState().equals("night")){
            View view = LayoutInflater.from(context).inflate(R.layout.ads_post_view_night, parent, false);   return new MyHolder(view);
        }else {
            View view = LayoutInflater.from(context).inflate(R.layout.ads_post_view, parent, false);   return new MyHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, @SuppressLint("RecyclerView") int position) {

        if (userList.get(position).getExpiry().equals("no")){
            holder.stat.setText("Status: OFF");
        }else {
            long timestamp = Long.parseLong(userList.get(position).getExpiry()); // Example timestamp (in milliseconds)
            Date date = new Date(timestamp);
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
            String dateString = dateFormat.format(date);
            holder.stat.setText("Ends On: "+ dateString);
        }

        FirebaseDatabase.getInstance().getReference("Users").child(userList.get(position).getId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //User
                holder.ads_tv_name.setText(snapshot.child("name").getValue().toString());

                holder.ads_tv_username.setText(snapshot.child("username").getValue().toString());

                if (!snapshot.child("photo").getValue().toString().isEmpty()){
                    Picasso.get().load(snapshot.child("photo").getValue().toString()).into(holder.ads_profile_photo);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });



        holder.ads_tv_tweet_text.setLinkText(userList.get(position).getText());
        holder.ads_tv_tweet_text.setOnLinkClickListener(new SocialTextView.OnLinkClickListener() {
            @Override
            public void onLinkClicked(int i, String s) {

                int views = Integer.parseInt(userList.get(position).getpViews()) + 1;
                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("pViews", String.valueOf(views));
                FirebaseDatabase.getInstance().getReference("AdsPost").child(userList.get(position).getpId()).updateChildren(hashMap);

                if (i == 1) {
                    Intent intent = new Intent(context, SearchActivity.class);
                    intent.putExtra("hashtag", s);
                    context.startActivity(intent);
                } else if (i == 2) {
                    String username = s.replaceFirst("@", "");
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
                    Query query = ref.orderByChild("username").equalTo(username.trim());
                    query.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                for (DataSnapshot ds : snapshot.getChildren()) {
                                    String id = ds.child("id").getValue().toString();
                                    if (id.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                                        Snackbar.make(holder.itemView, "It's you", Snackbar.LENGTH_LONG).show();
                                    } else {
                                        Intent intent = new Intent(context, UserProfileActivity.class);
                                        intent.putExtra("hisUID", id);
                                        context.startActivity(intent);
                                    }
                                }
                            } else {
                                Snackbar.make(holder.itemView, "Invalid username, can't find user with this username", Snackbar.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Snackbar.make(holder.itemView, error.getMessage(), Snackbar.LENGTH_LONG).show();
                        }
                    });
                } else if (i == 16) {
                    if (!s.startsWith("https://") && !s.startsWith("http://")) {
                        s = "http://" + s;
                    }
                    Intent openUrlIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(s));
                    context.startActivity(openUrlIntent);
                } else if (i == 4) {
                    Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", s, null));
                    context.startActivity(intent);
                } else if (i == 8) {
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("mailto:"));
                    intent.putExtra(Intent.EXTRA_EMAIL, s);
                    intent.putExtra(Intent.EXTRA_SUBJECT, "");
                    context.startActivity(intent);

                }
            }
        });

        //Post
        if (userList.get(position).getType().equals("image")){
            holder.ads_media.setVisibility(View.VISIBLE);
            holder.ads_media_layout.setVisibility(View.VISIBLE);
            Picasso.get().load(userList.get(position).getImage()).into(holder.ads_media);
            holder.ads_play.setVisibility(View.GONE);
        }else if (userList.get(position).getType().equals("video")){
            holder.ads_media_layout.setVisibility(View.VISIBLE);
            holder.ads_media.setVisibility(View.VISIBLE);
            Glide.with(context).asBitmap().load(userList.get(position).getVideo()).thumbnail(0.1f).into(holder.ads_media);
            holder.ads_play.setVisibility(View.VISIBLE);
        }else {
            holder.ads_media_layout.setVisibility(View.GONE);
            holder.ads_media.setVisibility(View.GONE);
            holder.ads_play.setVisibility(View.GONE);
        }

        //HandelClick
        holder.ads_media_layout.setOnClickListener(v -> {
            int views = Integer.parseInt(userList.get(position).getpViews())+1;
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("pViews", String.valueOf(views));
            FirebaseDatabase.getInstance().getReference("AdsPost").child(userList.get(position).getpId()).updateChildren(hashMap);

            if (userList.get(position).getType().equals("image")){
                Intent intent = new Intent(context, MediaViewActivity.class);
                intent.putExtra("type", userList.get(position).getType());
                intent.putExtra("uri",userList.get(position).getImage());
                context.startActivity(intent);
            }else
            if (userList.get(position).getType().equals("video")){
                Intent intent = new Intent(context, MediaViewActivity.class);
                intent.putExtra("type", userList.get(position).getType());
                intent.putExtra("uri", userList.get(position).getVideo());
                context.startActivity(intent);
            }

        });

        holder.ads_media.setOnClickListener(v -> {
            int views = Integer.parseInt(userList.get(position).getpViews())+1;
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("pViews", String.valueOf(views));
            FirebaseDatabase.getInstance().getReference("AdsPost").child(userList.get(position).getpId()).updateChildren(hashMap);

            if (userList.get(position).getType().equals("image")){
                Intent intent = new Intent(context, MediaViewActivity.class);
                intent.putExtra("type",userList.get(position).getType());
                intent.putExtra("uri", userList.get(position).getImage());
                context.startActivity(intent);
            }else
            if (userList.get(position).getType().equals("video")){
                Intent intent = new Intent(context, MediaViewActivity.class);
                intent.putExtra("type", userList.get(position).getType());
                intent.putExtra("uri", userList.get(position).getVideo());
                context.startActivity(intent);
            }

        });

        holder.views.setText(userList.get(position).getpViews());


        holder.delete.setOnClickListener(v -> {

            FirebaseDatabase.getInstance().getReference("AdsPost").child(userList.get(position).getpId()).getRef().removeValue();
            Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show();
            ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
            params.height = 0;
            holder.itemView.setLayoutParams(params);

        });

    }


    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class MyHolder extends RecyclerView.ViewHolder{

        CircleImageView ads_profile_photo;
        TextView ads_tv_name,ads_tv_username;
        SocialTextView ads_tv_tweet_text;
        ImageView ads_verified,ads_play;
        RelativeLayout ads_media_layout;
        RoundedImageView ads_media;
        Button delete;
        TextView views;
        TextView stat;

        public MyHolder(@NonNull View itemView) {
            super(itemView);

            stat = itemView.findViewById(R.id.stat);
            ads_profile_photo = itemView.findViewById(R.id.ads_profile_photo);
            ads_tv_name = itemView.findViewById(R.id.ads_tv_name);
            ads_tv_username = itemView.findViewById(R.id.ads_tv_username);
            ads_tv_tweet_text = itemView.findViewById(R.id.ads_tv_tweet_text);
            ads_verified = itemView.findViewById(R.id.ads_verified);
            ads_play = itemView.findViewById(R.id.ads_play);
            ads_media_layout = itemView.findViewById(R.id.ads_media_layout);
            ads_media = itemView.findViewById(R.id.ads_media);

            views = itemView.findViewById(R.id.views);
            delete = itemView.findViewById(R.id.delete);

        }

    }
}
