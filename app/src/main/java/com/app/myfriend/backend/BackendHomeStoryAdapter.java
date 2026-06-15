package com.app.myfriend.backend;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.myfriend.R;
import com.makeramen.roundedimageview.RoundedImageView;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class BackendHomeStoryAdapter extends RecyclerView.Adapter<BackendHomeStoryAdapter.StoryViewHolder> {

    public interface OnStoryClickListener {
        void onStoryClicked(BackendHomeStory story);
    }

    private final List<BackendHomeStory> stories = new ArrayList<>();
    private final OnStoryClickListener listener;

    public BackendHomeStoryAdapter(OnStoryClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<BackendHomeStory> items) {
        stories.clear();
        stories.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_backend_home_story, parent, false);
        return new StoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoryViewHolder holder, int position) {
        BackendHomeStory story = stories.get(position);
        holder.name.setText(story.name);
        holder.badge.setText(story.badgeLabel);

        String imageUrl = BackendAuthApi.resolveUrl(story.imageUrl);
        if (!imageUrl.isEmpty() && imageUrl.startsWith("http")) {
            Picasso.get().load(imageUrl).placeholder(R.drawable.cover).error(R.drawable.cover).into(holder.cover);
            Picasso.get().load(imageUrl).placeholder(R.drawable.avatar).error(R.drawable.avatar).into(holder.avatar);
        } else {
            holder.cover.setImageResource(R.drawable.cover);
            holder.avatar.setImageResource(R.drawable.avatar);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onStoryClicked(story);
            }
        });
    }

    @Override
    public int getItemCount() {
        return stories.size();
    }

    static class StoryViewHolder extends RecyclerView.ViewHolder {
        final RoundedImageView cover;
        final CircleImageView avatar;
        final TextView badge;
        final TextView name;

        StoryViewHolder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.storyCover);
            avatar = itemView.findViewById(R.id.storyAvatar);
            badge = itemView.findViewById(R.id.storyBadge);
            name = itemView.findViewById(R.id.storyName);
        }
    }
}
