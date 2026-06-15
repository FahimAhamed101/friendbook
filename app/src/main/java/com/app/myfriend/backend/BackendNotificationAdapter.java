package com.app.myfriend.backend;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.myfriend.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class BackendNotificationAdapter extends RecyclerView.Adapter<BackendNotificationAdapter.NotificationViewHolder> {

    private final List<BackendNotificationItem> items = new ArrayList<>();

    public void submitList(List<BackendNotificationItem> notifications) {
        items.clear();
        items.addAll(notifications);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_backend_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        BackendNotificationItem item = items.get(position);
        holder.title.setText(item.title);
        holder.message.setText(item.message);
        holder.meta.setText(item.meta);
        String imageUrl = BackendAuthApi.resolveUrl(item.imageUrl);
        if (!imageUrl.isEmpty() && imageUrl.startsWith("http")) {
            Picasso.get().load(imageUrl).placeholder(R.drawable.avatar).error(R.drawable.avatar).into(holder.avatar);
        } else {
            holder.avatar.setImageResource(R.drawable.avatar);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        final CircleImageView avatar;
        final TextView title;
        final TextView message;
        final TextView meta;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.notificationAvatar);
            title = itemView.findViewById(R.id.notificationTitle);
            message = itemView.findViewById(R.id.notificationMessage);
            meta = itemView.findViewById(R.id.notificationMeta);
        }
    }
}
