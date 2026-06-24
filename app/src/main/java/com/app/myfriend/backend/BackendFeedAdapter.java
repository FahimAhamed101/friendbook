package com.app.myfriend.backend;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.myfriend.R;
import com.app.myfriend.search.SearchActivity;
import com.github.pgreze.reactions.ReactionPopup;
import com.github.pgreze.reactions.ReactionsConfig;
import com.github.pgreze.reactions.ReactionsConfigBuilder;
import com.squareup.picasso.Picasso;
import com.tylersuehr.socialtextview.SocialTextView;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import me.jagar.chatvoiceplayerlibrary.VoicePlayerView;

public class BackendFeedAdapter extends RecyclerView.Adapter<BackendFeedAdapter.FeedViewHolder> {

    public interface FeedActionListener {
        void onLike(BackendFeedPost post, String type);
        void onComment(BackendFeedPost post);
        void onShare(BackendFeedPost post);
        void onSave(BackendFeedPost post);
        void onAuthorClick(String authorId);
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

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull FeedViewHolder holder, int position) {
        BackendFeedPost post = posts.get(position);

        // Author Info
        holder.authorName.setText(post.authorName);
        holder.authorHandle.setText(post.authorHandle);

        StringBuilder metaBuilder = new StringBuilder();
        if (!TextUtils.isEmpty(post.activity)) metaBuilder.append(post.activity).append(" ");
        if (!TextUtils.isEmpty(post.published)) metaBuilder.append(" . ").append(post.published);
        if (!TextUtils.isEmpty(post.location)) metaBuilder.append(" . ").append(post.location);
        holder.time.setText(metaBuilder.toString());

        String authorImageUrl = BackendAuthApi.resolveUrl(post.authorImage);
        if (!authorImageUrl.isEmpty() && authorImageUrl.startsWith("http")) {
            Picasso.get().load(authorImageUrl).placeholder(R.drawable.avatar).error(R.drawable.avatar).into(holder.authorImage);
        } else {
            holder.authorImage.setImageResource(R.drawable.avatar);
        }

        // Content logic similar to AdapterPost
        if ("bg".equalsIgnoreCase(post.type)) {
            holder.bgText.setVisibility(View.VISIBLE);
            holder.content.setVisibility(View.GONE);
            holder.bgText.setLinkText(post.content);
            setupSocialText(holder.bgText);
        } else {
            holder.bgText.setVisibility(View.GONE);
            holder.content.setVisibility(View.VISIBLE);
            holder.content.setLinkText(post.content);
            setupSocialText(holder.content);
        }

        if (TextUtils.isEmpty(post.title)) {
            holder.title.setVisibility(View.GONE);
        } else {
            holder.title.setVisibility(View.VISIBLE);
            holder.title.setText(post.title);
        }

        // Media Section logic
        resetMediaViews(holder);
        String mediaUrl = BackendAuthApi.resolveUrl(post.image);
        if ("audio".equalsIgnoreCase(post.type) || !post.audioSources.isEmpty()) {
            holder.voicePlayerView.setVisibility(View.VISIBLE);
            String audioUrl = post.audioSources.isEmpty() ? mediaUrl : post.audioSources.get(0).url;
            holder.voicePlayerView.setAudio(BackendAuthApi.resolveUrl(audioUrl));
        } else if (!mediaUrl.isEmpty()) {
            holder.postImage.setVisibility(View.VISIBLE);
            Picasso.get().load(mediaUrl).placeholder(R.drawable.cover).into(holder.postImage);
            if ("video".equalsIgnoreCase(post.type)) {
                holder.videoPlayIcon.setVisibility(View.VISIBLE);
            }
        } else if (!TextUtils.isEmpty(post.linkUrl)) {
            holder.linkLabel.setVisibility(View.VISIBLE);
            holder.linkLabel.setText(TextUtils.isEmpty(post.ctaLabel) ? "Open Link" : post.ctaLabel);
        }

        // Stats
        holder.likeCount.setText(String.valueOf(post.likeCount));
        holder.commentCount.setText(String.valueOf(post.commentCount));
        holder.viewCount.setText(String.valueOf(post.viewCount));

        // Reaction logic
        updateReactionUI(holder, post);

        // Action Buttons & Listeners
        boolean canInteract = actionListener != null && post.id != null && post.id.matches("^[a-fA-F0-9]{24}$");

        ReactionsConfig config = new ReactionsConfigBuilder(holder.itemView.getContext())
                .withReactions(new int[]{
                        R.drawable.ic_thumb, R.drawable.ic_love, R.drawable.ic_laugh,
                        R.drawable.ic_wow, R.drawable.ic_sad, R.drawable.ic_angry
                })
                .withPopupAlpha(1)
                .build();

        ReactionPopup popup = new ReactionPopup(holder.itemView.getContext(), config, (pos) -> {
            if (canInteract) {
                String type = "like";
                if (pos == 1) type = "love";
                else if (pos == 2) type = "haha";
                else if (pos == 3) type = "wow";
                else if (pos == 4) type = "sad";
                else if (pos == 5) type = "angry";
                actionListener.onLike(post, type);
            }
            return true;
        });

        holder.likeButton.setOnTouchListener(popup);
        holder.likeButton.setOnClickListener(v -> {
            if (canInteract) actionListener.onLike(post, "like");
        });

        holder.commentButton.setOnClickListener(v -> {
            if (canInteract) actionListener.onComment(post);
        });

        holder.shareButton.setOnClickListener(v -> {
            if (canInteract) actionListener.onShare(post);
        });

        holder.saveButton.setOnClickListener(v -> {
            if (canInteract) actionListener.onSave(post);
        });

        View.OnClickListener authorClick = v -> {
            if (actionListener != null) actionListener.onAuthorClick(post.id);
        };
        holder.authorImage.setOnClickListener(authorClick);
        holder.authorName.setOnClickListener(authorClick);

        holder.postImage.setOnClickListener(v -> openPrimaryLink(v, post));
        holder.linkLabel.setOnClickListener(v -> openPrimaryLink(v, post));
    }

    private void setupSocialText(SocialTextView textView) {
        textView.setOnLinkClickListener((type, value) -> {
            if (type == 1) { // 1 = Hashtag
                Intent intent = new Intent(textView.getContext(), SearchActivity.class);
                intent.putExtra("hashtag", value);
                textView.getContext().startActivity(intent);
            } else if (type == 16) { // 16 = URL
                String url = value;
                if (!url.startsWith("https://") && !url.startsWith("http://")) {
                    url = "http://" + url;
                }
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                textView.getContext().startActivity(intent);
            }
        });
    }

    private void resetMediaViews(FeedViewHolder holder) {
        holder.postImage.setVisibility(View.GONE);
        holder.videoPlayIcon.setVisibility(View.GONE);
        holder.voicePlayerView.setVisibility(View.GONE);
        holder.linkLabel.setVisibility(View.GONE);
        holder.bgText.setVisibility(View.GONE);
    }

    private void updateReactionUI(FeedViewHolder holder, BackendFeedPost post) {
        holder.likeIcon.setImageResource(R.drawable.ic_like);
        holder.likeText.setText("Like");

        if (post.likedByViewer && post.viewerReaction != null) {
            holder.likeText.setText(post.viewerReaction.substring(0, 1).toUpperCase() + post.viewerReaction.substring(1));
            switch (post.viewerReaction.toLowerCase()) {
                case "like": holder.likeIcon.setImageResource(R.drawable.ic_thumb); break;
                case "love": holder.likeIcon.setImageResource(R.drawable.ic_love); break;
                case "haha": holder.likeIcon.setImageResource(R.drawable.ic_laugh); break;
                case "wow": holder.likeIcon.setImageResource(R.drawable.ic_wow); break;
                case "sad": holder.likeIcon.setImageResource(R.drawable.ic_sad); break;
                case "angry": holder.likeIcon.setImageResource(R.drawable.ic_angry); break;
            }
        }

        holder.saveIcon.setImageResource(post.savedByViewer ? R.drawable.ic_save : R.drawable.ic_save);
        holder.saveText.setText(post.savedByViewer ? "Saved" : "Save");
    }

    private void openPrimaryLink(View view, BackendFeedPost post) {
        String rawTarget = !TextUtils.isEmpty(post.linkUrl) ? post.linkUrl : post.attachmentUrl;
        String target = BackendAuthApi.resolveUrl(rawTarget);
        if (TextUtils.isEmpty(target)) return;

        Uri uri = Uri.parse(target);
        if (uri.getScheme() == null) return;

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        view.getContext().startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class FeedViewHolder extends RecyclerView.ViewHolder {
        final CircleImageView authorImage;
        final TextView authorName, authorHandle, time;
        final TextView title, linkLabel, likeCount, commentCount, viewCount, likeText, saveText;
        final SocialTextView content, bgText;
        final ImageView postImage, videoPlayIcon, likeIcon, saveIcon, moreButton;
        final LinearLayout likeButton, commentButton, shareButton, saveButton;
        final VoicePlayerView voicePlayerView;

        FeedViewHolder(@NonNull View itemView) {
            super(itemView);
            authorImage = itemView.findViewById(R.id.feedAuthorImage);
            authorName = itemView.findViewById(R.id.feedAuthorName);
            authorHandle = itemView.findViewById(R.id.feedAuthorHandle);
            time = itemView.findViewById(R.id.feedTime);
            title = itemView.findViewById(R.id.feedTitle);
            content = itemView.findViewById(R.id.feedContent);
            bgText = itemView.findViewById(R.id.feedBgText);
            postImage = itemView.findViewById(R.id.feedImage);
            videoPlayIcon = itemView.findViewById(R.id.feedVideoPlayIcon);
            voicePlayerView = itemView.findViewById(R.id.feedVoicePlayerView);
            linkLabel = itemView.findViewById(R.id.feedLinkLabel);
            likeCount = itemView.findViewById(R.id.feedLikeCount);
            commentCount = itemView.findViewById(R.id.feedCommentCount);
            viewCount = itemView.findViewById(R.id.feedViewCount);
            likeIcon = itemView.findViewById(R.id.feedLikeIcon);
            likeText = itemView.findViewById(R.id.feedLikeText);
            saveIcon = itemView.findViewById(R.id.feedSaveIcon);
            saveText = itemView.findViewById(R.id.feedSaveText);
            moreButton = itemView.findViewById(R.id.feedMoreButton);
            likeButton = itemView.findViewById(R.id.feedLikeButton);
            commentButton = itemView.findViewById(R.id.feedCommentButton);
            shareButton = itemView.findViewById(R.id.feedShareButton);
            saveButton = itemView.findViewById(R.id.feedSaveButton);
        }
    }
}
