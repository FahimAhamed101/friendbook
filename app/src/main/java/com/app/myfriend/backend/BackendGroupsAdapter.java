package com.app.myfriend.backend;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.myfriend.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class BackendGroupsAdapter extends RecyclerView.Adapter<BackendGroupsAdapter.GroupViewHolder> {

    public interface OnGroupClickListener {
        void onGroupClicked(BackendGroup group);
        void onJoinClicked(BackendGroup group);
    }

    private final List<BackendGroup> groups = new ArrayList<>();
    private final OnGroupClickListener listener;

    public BackendGroupsAdapter(OnGroupClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<BackendGroup> items) {
        groups.clear();
        groups.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new GroupViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_backend_person, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        BackendGroup group = groups.get(position);
        holder.name.setText(group.name);
        holder.subtitle.setText(group.category + " • " + group.memberCount + " members");

        String iconUrl = BackendAuthApi.resolveUrl(group.iconUrl);
        if (!iconUrl.isEmpty()) {
            Picasso.get().load(iconUrl).placeholder(R.drawable.group_icon).into(holder.image);
        } else {
            holder.image.setImageResource(R.drawable.group_icon);
        }

        holder.actionButton.setText(group.isMember ? "Joined" : "Join");
        holder.actionButton.setEnabled(!group.isMember);

        holder.itemView.setOnClickListener(v -> listener.onGroupClicked(group));
        holder.actionButton.setOnClickListener(v -> listener.onJoinClicked(group));
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    static class GroupViewHolder extends RecyclerView.ViewHolder {
        final CircleImageView image;
        final TextView name, subtitle;
        final Button actionButton;

        GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.personAvatar);
            name = itemView.findViewById(R.id.personName);
            subtitle = itemView.findViewById(R.id.personSubtitle);
            actionButton = itemView.findViewById(R.id.personAction);
        }
    }
}
