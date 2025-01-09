package app.shashi.AdminTalk.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.imageview.ShapeableImageView;
import com.bumptech.glide.Glide;
import java.util.List;
import java.util.Map;
import app.shashi.AdminTalk.R;
import app.shashi.AdminTalk.models.User;
import app.shashi.AdminTalk.activities.UserListActivity.UserPresenceInfo;
import app.shashi.AdminTalk.activities.UserListActivity.MessagePreview;
import android.text.format.DateUtils;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private List<User> userList;
    private final Map<String, UserPresenceInfo> presenceMap;
    private final Map<String, MessagePreview> messagePreviewMap;
    private final OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public UserAdapter(List<User> userList,
                       Map<String, UserPresenceInfo> presenceMap,
                       Map<String, MessagePreview> messagePreviewMap,
                       OnUserClickListener listener) {
        this.userList = userList;
        this.presenceMap = presenceMap;
        this.messagePreviewMap = messagePreviewMap;
        this.listener = listener;
    }

    public void updateList(List<User> newList) {
        this.userList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        UserPresenceInfo presenceInfo = presenceMap.get(user.getId());
        MessagePreview messagePreview = messagePreviewMap.get(user.getId());
        holder.bind(user, presenceInfo, messagePreview, listener);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        private final ShapeableImageView profileImage;
        private final TextView nameText;
        private final TextView emailText;
        private final View statusIndicator;
        private final TextView lastSeenText;
        private final TextView lastMessageText;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.image_profile);
            nameText = itemView.findViewById(R.id.text_name);
            emailText = itemView.findViewById(R.id.text_email);
            statusIndicator = itemView.findViewById(R.id.status_indicator);
            lastSeenText = itemView.findViewById(R.id.text_last_seen);
            lastMessageText = itemView.findViewById(R.id.text_last_message);
        }

        public void bind(final User user,
                         UserPresenceInfo presenceInfo,
                         MessagePreview messagePreview,
                         final OnUserClickListener listener) {
            nameText.setText(user.getName());
            emailText.setText(user.getEmail());

            
            boolean isOnline = presenceInfo != null && presenceInfo.isOnline;
            long lastSeen = presenceInfo != null ? presenceInfo.lastSeen : 0;

            statusIndicator.setBackgroundResource(
                    isOnline ? R.drawable.status_online : R.drawable.status_offline
            );

            if (isOnline) {
                lastSeenText.setText("Online");
                lastSeenText.setTextColor(itemView.getContext().getColor(R.color.online_color));
            } else if (lastSeen > 0) {
                String timeAgo = DateUtils.getRelativeTimeSpanString(
                        lastSeen,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE
                ).toString();
                lastSeenText.setText("Last seen " + timeAgo);
                lastSeenText.setTextColor(itemView.getContext().getColor(R.color.offline_color));
            } else {
                lastSeenText.setText("Offline");
                lastSeenText.setTextColor(itemView.getContext().getColor(R.color.offline_color));
            }

            
            if (messagePreview != null) {
                lastMessageText.setVisibility(View.VISIBLE);
                lastMessageText.setText(messagePreview.lastMessage);
            } else {
                lastMessageText.setVisibility(View.GONE);
            }

            
            Glide.with(itemView.getContext())
                    .load(user.getPhotoUrl())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .circleCrop()
                    .into(profileImage);

            itemView.setOnClickListener(v -> listener.onUserClick(user));
        }
    }
}