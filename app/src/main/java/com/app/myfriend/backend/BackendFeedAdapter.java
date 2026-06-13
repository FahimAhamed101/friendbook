package com.app.myfriend.backend;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.myfriend.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class BackendFeedAdapter extends RecyclerView.Adapter<BackendFeedAdapter.FeedViewHolder> {

    private final List<BackendFeedPost> posts = new ArrayList<>();

    public void submitList(List<BackendFeedPost> items) {
        posts.clear();
        posts.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FeedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_backend_feed_post, parent, false);
        return new FeedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedViewHolder holder, int position) {
        BackendFeedPost post = posts.get(position);
        holder.authorName.setText(post.authorName);
        holder.authorHandle.setText(post.authorHandle);
        holder.activity.setText(post.activity + " • " + post.published);
        holder.content.setText(TextUtils.isEmpty(post.content) ? "No text content." : post.content);
        holder.stats.setText(post.likeCount + " likes  •  " + post.commentCount + " comments  •  " + post.shareCount + " shares");

        String authorImageUrl = BackendAuthApi.resolveUrl(post.authorImage);
        if (!authorImageUrl.isEmpty() && authorImageUrl.startsWith("http")) {
            Picasso.get().load(authorImageUrl).placeholder(R.mipmap.ic_launcher).error(R.mipmap.ic_launcher).into(holder.authorImage);
        } else {
            holder.authorImage.setImageResource(R.mipmap.ic_launcher);
        }

        String postImageUrl = BackendAuthApi.resolveUrl(post.image);
        if (!postImageUrl.isEmpty() && postImageUrl.startsWith("http")) {
            holder.postImage.setVisibility(View.VISIBLE);
            Picasso.get().load(postImageUrl).fit().centerCrop().into(holder.postImage);
        } else {
            holder.postImage.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class FeedViewHolder extends RecyclerView.ViewHolder {
        final CircleImageView authorImage;
        final TextView authorName;
        final TextView authorHandle;
        final TextView activity;
        final TextView content;
        final TextView stats;
        final ImageView postImage;

        FeedViewHolder(@NonNull View itemView) {
            super(itemView);
            authorImage = itemView.findViewById(R.id.feedAuthorImage);
            authorName = itemView.findViewById(R.id.feedAuthorName);
            authorHandle = itemView.findViewById(R.id.feedAuthorHandle);
            activity = itemView.findViewById(R.id.feedActivity);
            content = itemView.findViewById(R.id.feedContent);
            stats = itemView.findViewById(R.id.feedStats);
            postImage = itemView.findViewById(R.id.feedImage);
        }
    }
}
