package com.app.myfriend.backend;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.myfriend.R;

import java.util.ArrayList;
import java.util.List;

public class BackendMessageAdapter extends RecyclerView.Adapter<BackendMessageAdapter.MessageViewHolder> {

    private final List<BackendMessageItem> items = new ArrayList<>();

    public void submitList(List<BackendMessageItem> messages) {
        items.clear();
        items.addAll(messages);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).sentByViewer ? 1 : 0;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = viewType == 1 ? R.layout.item_backend_message_right : R.layout.item_backend_message_left;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        BackendMessageItem item = items.get(position);
        holder.message.setText(item.content);
        holder.time.setText(item.createdAt == null ? "" : item.createdAt);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        final TextView message;
        final TextView time;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.backendMessageText);
            time = itemView.findViewById(R.id.backendMessageTime);
        }
    }
}
