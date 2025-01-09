package app.shashi.AdminTalk.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import app.shashi.AdminTalk.R;
import app.shashi.AdminTalk.models.ChatPreview;
import com.bumptech.glide.Glide;

import app.shashi.AdminTalk.models.Message;
import app.shashi.AdminTalk.models.User;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.ViewHolder> {
    private List<ChatPreview> chatPreviews;
    private OnUserClickListener listener;
    private SimpleDateFormat dateFormat;

    public interface OnUserClickListener {
        void onUserClick(String userId, String userName);
    }

    public UserListAdapter(List<ChatPreview> chatPreviews, OnUserClickListener listener) {
        this.chatPreviews = chatPreviews;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_preview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatPreview chatPreview = chatPreviews.get(position);
        User user = chatPreview.getUser();
        Message lastMessage = chatPreview.getLastMessage();

        holder.nameTextView.setText(user.getName());

        if (lastMessage != null) {
            holder.lastMessageTextView.setVisibility(View.VISIBLE);
            holder.timestampTextView.setVisibility(View.VISIBLE);

            holder.lastMessageTextView.setText(lastMessage.getText());
            holder.timestampTextView.setText(dateFormat.format(new Date(chatPreview.getTimestamp())));
        } else {
            holder.lastMessageTextView.setVisibility(View.GONE);
            holder.timestampTextView.setVisibility(View.GONE);
        }

        if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(user.getPhotoUrl())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .into(holder.profileImageView);
        } else {
            holder.profileImageView.setImageResource(R.drawable.ic_profile_placeholder);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserClick(user.getId(), user.getName());
            }
        });
    }

    @Override
    public int getItemCount() {
        return chatPreviews.size();
    }

    public void updateData(List<ChatPreview> newChatPreviews) {
        this.chatPreviews = newChatPreviews;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView profileImageView;
        TextView nameTextView;
        TextView lastMessageTextView;
        TextView timestampTextView;

        ViewHolder(View itemView) {
            super(itemView);
            profileImageView = itemView.findViewById(R.id.profileImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            lastMessageTextView = itemView.findViewById(R.id.lastMessageTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
        }
    }
}