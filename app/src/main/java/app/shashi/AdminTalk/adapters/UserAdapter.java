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

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private final List<User> userList;
    private final Map<String, Boolean> presenceMap;
    private final OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public UserAdapter(List<User> userList, Map<String, Boolean> presenceMap, OnUserClickListener listener) {
        this.userList = userList;
        this.presenceMap = presenceMap;
        this.listener = listener;
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
        Boolean isOnline = presenceMap.get(user.getId());
        holder.bind(user, isOnline != null && isOnline, listener);
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

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.image_profile);
            nameText = itemView.findViewById(R.id.text_name);
            emailText = itemView.findViewById(R.id.text_email);
            statusIndicator = itemView.findViewById(R.id.status_indicator);
        }

        public void bind(final User user, boolean isOnline, final OnUserClickListener listener) {
            nameText.setText(user.getName());
            emailText.setText(user.getEmail());

            statusIndicator.setBackgroundResource(
                    isOnline ? R.drawable.status_online : R.drawable.status_offline
            );

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