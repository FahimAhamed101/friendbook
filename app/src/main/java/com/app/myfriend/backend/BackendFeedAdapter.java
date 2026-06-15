package com.app.myfriend.backend;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

    public interface FeedActionListener {
        void onLike(BackendFeedPost post);

        void onComment(BackendFeedPost post);

        void onShare(BackendFeedPost post);

        void onSave(BackendFeedPost post);
    }

    private final List<BackendFeedPost> posts = new ArrayList<>();
    private final FeedActionListener actionListener;

    public BackendFeedAdapter(FeedActionListener actionListener) {
        this.actionListener = actionListener;
    }

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
        holder.meta.setText(buildMeta(post));

        if (TextUtils.isEmpty(post.title)) {
            holder.title.setVisibility(View.GONE);
        } else {
            holder.title.setVisibility(View.VISIBLE);
            holder.title.setText(post.title);
        }

        String content = TextUtils.isEmpty(post.content) ? "No details added." : post.content;
        holder.content.setText(content);

        if (TextUtils.isEmpty(post.linkUrl)) {
            if ("video".equalsIgnoreCase(post.type) && !TextUtils.isEmpty(post.attachmentUrl)) {
                holder.linkLabel.setVisibility(View.VISIBLE);
                holder.linkLabel.setText("Watch video");
            } else {
                holder.linkLabel.setVisibility(View.GONE);
            }
        } else {
            holder.linkLabel.setVisibility(View.VISIBLE);
            holder.linkLabel.setText(TextUtils.isEmpty(post.ctaLabel) ? "Open shared link" : post.ctaLabel);
        }

        holder.stats.setText(post.likeCount + " likes  " + post.commentCount + " comments  " + post.shareCount + " shares  " + post.saveCount + " saves");
        holder.likeButton.setText(post.likedByViewer ? "Liked" : "Like");
        holder.saveButton.setText(post.savedByViewer ? "Saved" : "Save");
        holder.commentButton.setEnabled(post.commentsOpen);
        holder.commentButton.setAlpha(post.commentsOpen ? 1f : 0.55f);

        String authorImageUrl = BackendAuthApi.resolveUrl(post.authorImage);
        if (!authorImageUrl.isEmpty() && authorImageUrl.startsWith("http")) {
            Picasso.get().load(authorImageUrl).placeholder(R.drawable.avatar).error(R.drawable.avatar).into(holder.authorImage);
        } else {
            holder.authorImage.setImageResource(R.drawable.avatar);
        }

        String postImageUrl = BackendAuthApi.resolveUrl(post.image);
        if (!postImageUrl.isEmpty() && postImageUrl.startsWith("http")) {
            holder.postImage.setVisibility(View.VISIBLE);
            Picasso.get().load(postImageUrl).fit().centerCrop().into(holder.postImage);
        } else {
            holder.postImage.setVisibility(View.GONE);
        }

        boolean canInteract = actionListener != null && post.id != null && post.id.matches("^[a-fA-F0-9]{24}$");
        holder.likeButton.setEnabled(canInteract);
        holder.shareButton.setEnabled(canInteract);
        holder.saveButton.setEnabled(canInteract);
        holder.commentButton.setEnabled(canInteract && post.commentsOpen);

        holder.likeButton.setOnClickListener(v -> {
            if (actionListener != null && canInteract) {
                actionListener.onLike(post);
            }
        });
        holder.commentButton.setOnClickListener(v -> {
            if (actionListener != null && canInteract && post.commentsOpen) {
                actionListener.onComment(post);
            }
        });
        holder.shareButton.setOnClickListener(v -> {
            if (actionListener != null && canInteract) {
                actionListener.onShare(post);
            }
        });
        holder.saveButton.setOnClickListener(v -> {
            if (actionListener != null && canInteract) {
                actionListener.onSave(post);
            }
        });
        holder.linkLabel.setOnClickListener(v -> openPrimaryLink(v, post));
        holder.postImage.setOnClickListener(v -> openPrimaryLink(v, post));
    }

    private void openPrimaryLink(View view, BackendFeedPost post) {
        String rawTarget = !TextUtils.isEmpty(post.linkUrl) ? post.linkUrl : post.attachmentUrl;
        String target = BackendAuthApi.resolveUrl(rawTarget);
        if (TextUtils.isEmpty(target)) {
            return;
        }

        Uri uri = Uri.parse(target);
        if (uri.getScheme() == null || uri.getScheme().trim().isEmpty()) {
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        view.getContext().startActivity(intent);
    }

    private String buildMeta(BackendFeedPost post) {
        String activity = String.valueOf(post.activity == null ? "" : post.activity).trim();
        String published = String.valueOf(post.published == null ? "" : post.published).trim();
        if (!activity.isEmpty() && !published.isEmpty()) {
            return activity + " - " + published;
        }
        if (!activity.isEmpty()) {
            return activity;
        }
        return published;
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class FeedViewHolder extends RecyclerView.ViewHolder {
        final CircleImageView authorImage;
        final TextView authorName;
        final TextView authorHandle;
        final TextView meta;
        final TextView title;
        final TextView content;
        final TextView linkLabel;
        final TextView stats;
        final ImageView postImage;
        final Button likeButton;
        final Button commentButton;
        final Button shareButton;
        final Button saveButton;

        FeedViewHolder(@NonNull View itemView) {
            super(itemView);
            authorImage = itemView.findViewById(R.id.feedAuthorImage);
            authorName = itemView.findViewById(R.id.feedAuthorName);
            authorHandle = itemView.findViewById(R.id.feedAuthorHandle);
            meta = itemView.findViewById(R.id.feedMeta);
            title = itemView.findViewById(R.id.feedTitle);
            content = itemView.findViewById(R.id.feedContent);
            linkLabel = itemView.findViewById(R.id.feedLinkLabel);
            stats = itemView.findViewById(R.id.feedStats);
            postImage = itemView.findViewById(R.id.feedImage);
            likeButton = itemView.findViewById(R.id.feedLikeButton);
            commentButton = itemView.findViewById(R.id.feedCommentButton);
            shareButton = itemView.findViewById(R.id.feedShareButton);
            saveButton = itemView.findViewById(R.id.feedSaveButton);
        }
    }
}
