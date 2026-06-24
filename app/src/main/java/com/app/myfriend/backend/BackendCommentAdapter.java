package com.app.myfriend.backend;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.myfriend.R;
import com.squareup.picasso.Picasso;
import com.tylersuehr.socialtextview.SocialTextView;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class BackendCommentAdapter extends RecyclerView.Adapter<BackendCommentAdapter.CommentViewHolder> {

    private final List<BackendCommentItem> comments = new ArrayList<>();

    public BackendCommentAdapter() {
    }

    public void submitList(List<BackendCommentItem> items) {
        comments.clear();
        comments.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Reusing comment_list layout, similar to AdapterComment
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.comment_list, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        BackendCommentItem comment = comments.get(position);

        holder.name.setText(comment.name);
        holder.message.setLinkText(comment.message);
        holder.time.setText(comment.time);

        String imageUrl = BackendAuthApi.resolveUrl(comment.image);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Picasso.get().load(imageUrl).placeholder(R.drawable.avatar).error(R.drawable.avatar).into(holder.dp);
        } else {
            holder.dp.setImageResource(R.drawable.avatar);
        }

        // Hide features not currently supported by BackendCommentItem to keep it clean
        if (holder.mediaLayout != null) holder.mediaLayout.setVisibility(View.GONE);
        if (holder.reply != null) holder.reply.setVisibility(View.GONE);
        if (holder.more != null) holder.more.setVisibility(View.GONE);

        // Hide like/liked UI as BackendCommentItem doesn't include reaction info yet
        View like = holder.itemView.findViewById(R.id.like);
        if (like != null) like.setVisibility(View.GONE);
        View liked = holder.itemView.findViewById(R.id.liked);
        if (liked != null) liked.setVisibility(View.GONE);
        View noLikes = holder.itemView.findViewById(R.id.noLikes);
        if (noLikes != null) noLikes.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        final CircleImageView dp;
        final TextView name, time, reply;
        final SocialTextView message;
        final View mediaLayout, more;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            dp = itemView.findViewById(R.id.dp);
            name = itemView.findViewById(R.id.name);
            message = itemView.findViewById(R.id.username); // ID 'username' is used for comment text in comment_list.xml
            time = itemView.findViewById(R.id.time);
            reply = itemView.findViewById(R.id.reply);
            mediaLayout = itemView.findViewById(R.id.media_layout);
            more = itemView.findViewById(R.id.more);
        }
    }
}
