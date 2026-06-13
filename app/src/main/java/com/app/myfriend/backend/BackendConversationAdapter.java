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

public class BackendConversationAdapter extends RecyclerView.Adapter<BackendConversationAdapter.ConversationViewHolder> {

    public interface OnConversationClickListener {
        void onConversationClicked(BackendConversationItem item);
    }

    private final List<BackendConversationItem> items = new ArrayList<>();
    private final OnConversationClickListener listener;

    public BackendConversationAdapter(OnConversationClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<BackendConversationItem> conversations) {
        items.clear();
        items.addAll(conversations);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_backend_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        BackendConversationItem item = items.get(position);
        holder.name.setText(item.participantName);
        holder.subtitle.setText(item.participantSubtitle == null || item.participantSubtitle.trim().isEmpty() ? item.lastMessageText : item.participantSubtitle);
        holder.lastMessage.setText(item.lastMessageText == null || item.lastMessageText.trim().isEmpty() ? "No messages yet" : item.lastMessageText);
        holder.time.setText(item.lastMessageAt == null ? "" : item.lastMessageAt);
        holder.unread.setText(String.valueOf(item.unreadCount));
        holder.unread.setVisibility(item.unreadCount > 0 ? View.VISIBLE : View.GONE);
        String imageUrl = BackendAuthApi.resolveUrl(item.participantAvatarUrl);
        if (!imageUrl.isEmpty() && imageUrl.startsWith("http")) {
            Picasso.get().load(imageUrl).placeholder(R.mipmap.ic_launcher).error(R.mipmap.ic_launcher).into(holder.avatar);
        } else {
            holder.avatar.setImageResource(R.mipmap.ic_launcher);
        }
        holder.itemView.setOnClickListener(v -> listener.onConversationClicked(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ConversationViewHolder extends RecyclerView.ViewHolder {
        final CircleImageView avatar;
        final TextView name;
        final TextView subtitle;
        final TextView lastMessage;
        final TextView time;
        final TextView unread;

        ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.conversationAvatar);
            name = itemView.findViewById(R.id.conversationName);
            subtitle = itemView.findViewById(R.id.conversationSubtitle);
            lastMessage = itemView.findViewById(R.id.conversationLastMessage);
            time = itemView.findViewById(R.id.conversationTime);
            unread = itemView.findViewById(R.id.conversationUnread);
        }
    }
}
