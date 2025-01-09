package app.shashi.AdminTalk.activities;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.text.util.Linkify;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.*;

import app.shashi.AdminTalk.R;
import app.shashi.AdminTalk.adapters.MessageAdapter;
import app.shashi.AdminTalk.models.Message;
import app.shashi.AdminTalk.utils.Constants;
import app.shashi.AdminTalk.utils.FirebaseHelper;
import app.shashi.AdminTalk.utils.AuthHelper;
import app.shashi.AdminTalk.utils.PresenceHelper;

public class ChatActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private TextInputEditText messageInput;
    private MaterialButton sendButton;
    private TextView chatTitle;
    private TextView userStatusView;
    private ValueEventListener messagesListener;
    private String selectedUserId;
    private String currentUserId;
    private String chatId;
    private boolean isAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        currentUserId = currentUser.getUid();
        selectedUserId = getIntent().getStringExtra(Constants.EXTRA_USER_ID);
        String selectedUserName = getIntent().getStringExtra(Constants.EXTRA_USER_NAME);

        initializeViews(selectedUserName);

        AuthHelper.isAdmin().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                isAdmin = task.getResult();
                chatId = isAdmin ? selectedUserId : currentUserId;
                initializeChat();
            } else {
                Toast.makeText(this, "Failed to verify permissions", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void initializeViews(String selectedUserName) {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        chatTitle = findViewById(R.id.chat_title);
        chatTitle.setText(selectedUserName != null ? selectedUserName : "Chat");

        userStatusView = findViewById(R.id.user_status);
        userStatusView.setText("Offline");

        recyclerView = findViewById(R.id.recycler_view);
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList, currentUserId, this::formatTimestamp);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(messageAdapter);
        recyclerView.setItemAnimator(null);

        messageInput = findViewById(R.id.message_input);
        messageInput.setLinkTextColor(getColor(R.color.link_color));
        messageInput.setAutoLinkMask(Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES | Linkify.PHONE_NUMBERS);

        sendButton = findViewById(R.id.send_button);
        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void initializeChat() {
        if (chatId == null) return;

        
        PresenceHelper.initializePresence(currentUserId, chatId);

        
        String otherUserId = isAdmin ? selectedUserId : currentUserId;
        PresenceHelper.monitorPresence(chatId, otherUserId, (isOnline, lastSeen) -> {
            updateUserStatus(isOnline, lastSeen);
        });

        
        messagesListener = createMessageListener();
        FirebaseHelper.getChatReference(chatId)
                .child(Constants.MESSAGES_REF)
                .addValueEventListener(messagesListener);
    }

    private void updateUserStatus(boolean isOnline, long lastSeen) {
        runOnUiThread(() -> {
            if (isOnline) {
                userStatusView.setText("Online");
                userStatusView.setTextColor(getColor(R.color.online_color));
            } else {
                String lastSeenText = lastSeen > 0 ?
                        "Last seen " + getTimeAgo(lastSeen) : "Offline";
                userStatusView.setText(lastSeenText);
                userStatusView.setTextColor(getColor(R.color.offline_color));
            }
        });
    }

    private String getTimeAgo(long timeInMillis) {
        long now = System.currentTimeMillis();
        long diff = now - timeInMillis;

        if (diff < 60000) { 
            return "just now";
        } else if (diff < 3600000) { 
            long minutes = diff / 60000;
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        } else if (diff < 86400000) { 
            long hours = diff / 3600000;
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        } else { 
            long days = diff / 86400000;
            return days + (days == 1 ? " day ago" : " days ago");
        }
    }

    private ValueEventListener createMessageListener() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Message> newMessages = new ArrayList<>();
                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    Message message = messageSnapshot.getValue(Message.class);
                    if (message != null && !messageExists(message)) {
                        newMessages.add(message);
                    }
                }

                if (!newMessages.isEmpty()) {
                    messageList.addAll(newMessages);
                    sortMessages();
                    messageAdapter.notifyDataSetChanged();
                    recyclerView.scrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
            }
        };
    }

    private String formatTimestamp(long timestamp) {
        Date date = new Date(timestamp);
        boolean is24HourFormat = DateFormat.is24HourFormat(this);
        String pattern = is24HourFormat ? "HH:mm" : "hh:mm a";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
        return sdf.format(date);
    }

    private boolean messageExists(Message newMessage) {
        for (Message message : messageList) {
            if (message.getTimestamp() == newMessage.getTimestamp() &&
                    message.getSenderId().equals(newMessage.getSenderId())) {
                return true;
            }
        }
        return false;
    }

    private void sortMessages() {
        Collections.sort(messageList, (m1, m2) ->
                Long.compare(m1.getTimestamp(), m2.getTimestamp()));
    }

    private void sendMessage() {
        String messageText = messageInput.getText() != null ?
                messageInput.getText().toString().trim() : "";

        if (messageText.isEmpty() || chatId == null) {
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }

        DatabaseReference messagesRef = FirebaseHelper.getChatReference(chatId)
                .child(Constants.MESSAGES_REF);
        String messageId = messagesRef.push().getKey();

        if (messageId != null) {
            Message message = new Message(
                    messageText,
                    System.currentTimeMillis(),
                    currentUser.getUid(),
                    currentUser.getDisplayName() != null ?
                            currentUser.getDisplayName() : "Anonymous"
            );

            messagesRef.child(messageId).setValue(message)
                    .addOnSuccessListener(aVoid -> messageInput.setText(""))
                    .addOnFailureListener(e ->
                            Toast.makeText(ChatActivity.this,
                                    "Failed to send message", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentUserId != null && chatId != null) {
            PresenceHelper.updatePresence(chatId, currentUserId, true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (currentUserId != null && chatId != null) {
            PresenceHelper.updatePresence(chatId, currentUserId, false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatId != null && currentUserId != null) {
            PresenceHelper.cleanup(chatId, currentUserId);
        }
        if (messagesListener != null) {
            FirebaseHelper.getChatReference(chatId)
                    .child(Constants.MESSAGES_REF)
                    .removeEventListener(messagesListener);
        }
    }
}