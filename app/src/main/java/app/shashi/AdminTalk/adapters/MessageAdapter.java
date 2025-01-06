package app.shashi.AdminTalk.adapters;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.text.util.LinkifyCompat;
import androidx.recyclerview.widget.RecyclerView;
import app.shashi.AdminTalk.R;
import app.shashi.AdminTalk.models.Message;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final List<Message> messageList;
    private final String currentUserId;
    private final TimestampFormatter timestampFormatter;

    public interface TimestampFormatter {
        String formatTimestamp(long timestamp);
    }

    public MessageAdapter(List<Message> messageList, String currentUserId, TimestampFormatter timestampFormatter) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
        this.timestampFormatter = timestampFormatter;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_SENT) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
        }
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);

        
        SpannableString messageText = new SpannableString(message.getText());

        
        LinkifyCompat.addLinks(messageText, Linkify.ALL);

        
        holder.messageText.setText(messageText);
        holder.messageText.setMovementMethod(LinkMovementMethod.getInstance());
        holder.messageText.setLinkTextColor(holder.itemView.getContext().getColor(R.color.link_color));

        holder.timeText.setText(timestampFormatter.formatTimestamp(message.getTimestamp()));

        if (getItemViewType(position) == VIEW_TYPE_RECEIVED) {
            holder.nameText.setText(message.getSenderName());
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        return message.getSenderId().equals(currentUserId) ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView timeText;
        TextView nameText;

        MessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message);
            timeText = itemView.findViewById(R.id.text_time);
            nameText = itemView.findViewById(R.id.text_name);

            
            messageText.setTextIsSelectable(true);
        }
    }
}