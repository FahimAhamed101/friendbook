package com.app.myfriend.backend;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.myfriend.R;
import com.squareup.picasso.Picasso;
import com.tylersuehr.socialtextview.SocialTextView;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class BackendReelAdapter extends RecyclerView.Adapter<BackendReelAdapter.ReelViewHolder> {

    private final List<BackendFeedPost> reels = new ArrayList<>();
    private final Context context;

    public BackendReelAdapter(Context context) {
        this.context = context;
    }

    public void submitList(List<BackendFeedPost> items) {
        reels.clear();
        reels.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ReelViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.reel_list, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ReelViewHolder holder, int position) {
        BackendFeedPost reel = reels.get(position);

        holder.description.setLinkText(reel.content);
        holder.name.setText("@" + reel.authorHandle.replace("@", ""));

        String authorImg = BackendAuthApi.resolveUrl(reel.authorImage);
        if (!authorImg.isEmpty()) {
            Picasso.get().load(authorImg).placeholder(R.drawable.avatar).into(holder.avatar);
        }

        holder.textLike.setText(String.valueOf(reel.likeCount));
        holder.textComment.setText(String.valueOf(reel.commentCount));
        holder.views.setText(String.valueOf(reel.viewCount));

        String videoUrl = BackendAuthApi.resolveUrl(reel.attachmentUrl);
        if (videoUrl != null && !videoUrl.isEmpty()) {
            holder.videoView.setVideoPath(videoUrl);
            holder.videoView.setOnPreparedListener(mp -> {
                holder.progressBar.setVisibility(View.GONE);
                mp.start();
                mp.setLooping(true);
            });
            holder.videoView.setOnErrorListener((mp, what, extra) -> {
                holder.progressBar.setVisibility(View.GONE);
                return true;
            });
        }

        holder.avatar.setOnClickListener(v -> {
            Intent intent = new Intent(context, BackendUserProfileActivity.class);
            intent.putExtra("userId", reel.id); // Note: reel.id is the post ID, but we might need author ID
            // Actually BackendFeedPost should have authorId
            context.startActivity(intent);
        });

        holder.comment.setOnClickListener(v -> {
            Intent intent = new Intent(context, BackendCommentActivity.class);
            intent.putExtra("postId", reel.id);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return reels.size();
    }

    static class ReelViewHolder extends RecyclerView.ViewHolder {
        final VideoView videoView;
        final LinearLayout like, comment;
        final ImageView share, more, like_img;
        final CircleImageView avatar;
        final TextView name, textLike, textComment, views;
        final SocialTextView description;
        final ProgressBar progressBar;

        ReelViewHolder(@NonNull View itemView) {
            super(itemView);
            videoView = itemView.findViewById(R.id.videoView);
            like = itemView.findViewById(R.id.like);
            comment = itemView.findViewById(R.id.comment);
            share = itemView.findViewById(R.id.share);
            avatar = itemView.findViewById(R.id.avatar);
            name = itemView.findViewById(R.id.name);
            textLike = itemView.findViewById(R.id.textLike);
            textComment = itemView.findViewById(R.id.textComment);
            description = itemView.findViewById(R.id.description);
            like_img = itemView.findViewById(R.id.like_img);
            more = itemView.findViewById(R.id.more);
            views = itemView.findViewById(R.id.views);
            progressBar = itemView.findViewById(R.id.pb);
        }
    }
}
