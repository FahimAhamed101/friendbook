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

public class BackendPeopleAdapter extends RecyclerView.Adapter<BackendPeopleAdapter.PersonViewHolder> {

    public interface OnPersonClickListener {
        void onPersonClicked(BackendPerson person);

        void onActionClicked(BackendPerson person);
    }

    private final List<BackendPerson> people = new ArrayList<>();
    private final OnPersonClickListener listener;

    public BackendPeopleAdapter(OnPersonClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<BackendPerson> items) {
        people.clear();
        people.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PersonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_backend_person, parent, false);
        return new PersonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PersonViewHolder holder, int position) {
        BackendPerson person = people.get(position);
        holder.name.setText(person.name);
        holder.subtitle.setText(person.subtitle);
        holder.action.setText(person.actionLabel == null || person.actionLabel.trim().isEmpty() ? "Message" : person.actionLabel);
        String imageUrl = BackendAuthApi.resolveUrl(person.image);
        if (!imageUrl.isEmpty() && imageUrl.startsWith("http")) {
            Picasso.get().load(imageUrl).placeholder(R.drawable.avatar).error(R.drawable.avatar).into(holder.avatar);
        } else {
            holder.avatar.setImageResource(R.drawable.avatar);
        }
        holder.action.setOnClickListener(v -> listener.onActionClicked(person));
        holder.itemView.setOnClickListener(v -> listener.onPersonClicked(person));
    }

    @Override
    public int getItemCount() {
        return people.size();
    }

    static class PersonViewHolder extends RecyclerView.ViewHolder {
        final CircleImageView avatar;
        final TextView name;
        final TextView subtitle;
        final Button action;

        PersonViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.personAvatar);
            name = itemView.findViewById(R.id.personName);
            subtitle = itemView.findViewById(R.id.personSubtitle);
            action = itemView.findViewById(R.id.personAction);
        }
    }
}
