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

public class ChatActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private TextInputEditText messageInput;
    private MaterialButton sendButton;
    private TextView chatTitle;
    private TextView userStatusView;
    private String lastChatUserId;
    private boolean isAdmin;
    private ValueEventListener messagesListener;
    private ValueEventListener statusListener;
    private DatabaseReference userPresenceRef;
    private String selectedUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        selectedUserId = getIntent().getStringExtra(Constants.EXTRA_USER_ID);
        String selectedUserName = getIntent().getStringExtra(Constants.EXTRA_USER_NAME);
        lastChatUserId = selectedUserId;

        initializeViews(currentUser.getUid(), selectedUserName);
        initializeChat(currentUser.getUid(), selectedUserId);
    }

    private void initializeViews(String currentUserId, String selectedUserName) {
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

    private void initializeChat(String currentUserId, String selectedUserId) {
        isAdmin = FirebaseHelper.isAdmin();
        String chatUserId = isAdmin ? selectedUserId : currentUserId;

        if (chatUserId != null) {
            messagesListener = createMessageListener();
            FirebaseHelper.getUserChats(chatUserId).addValueEventListener(messagesListener);

            setupPresence(currentUserId, isAdmin ? selectedUserId : Constants.ADMIN_UID);
        }
    }

    private void setupPresence(String currentUserId, String otherUserId) {
        DatabaseReference presenceRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        userPresenceRef = FirebaseHelper.getUserPresenceReference(currentUserId);

        presenceRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                if (connected != null && connected) {
                    userPresenceRef.onDisconnect().setValue(false);
                    userPresenceRef.setValue(true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Failed to setup presence", Toast.LENGTH_SHORT).show();
            }
        });

        DatabaseReference otherUserPresenceRef = FirebaseHelper.getUserPresenceReference(otherUserId);
        statusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isOnline = snapshot.getValue(Boolean.class);
                updateUserStatus(isOnline != null && isOnline);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                updateUserStatus(false);
            }
        };
        otherUserPresenceRef.addValueEventListener(statusListener);
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

    private void updateUserStatus(boolean isOnline) {
        runOnUiThread(() -> {
            userStatusView.setText(isOnline ? "Online" : "Offline");
            userStatusView.setTextColor(getColor(isOnline ? R.color.online_color : R.color.offline_color));
        });
    }

    private String formatTimestamp(long timestamp) {
        Date date = new Date(timestamp);
        boolean is24HourFormat = DateFormat.is24HourFormat(this);
        String pattern = is24HourFormat ? "HH:mm" : "hh:mm a";

        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
        sdf.setTimeZone(TimeZone.getDefault());
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

        if (messageText.isEmpty()) {
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }

        String chatId = isAdmin ? lastChatUserId : currentUser.getUid();
        if (chatId == null) {
            Toast.makeText(this, "Please select a chat first", Toast.LENGTH_SHORT).show();
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
        if (userPresenceRef != null) {
            userPresenceRef.setValue(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (userPresenceRef != null) {
            userPresenceRef.setValue(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (statusListener != null && selectedUserId != null) {
            FirebaseHelper.getUserPresenceReference(selectedUserId)
                    .removeEventListener(statusListener);
        }
        if (messagesListener != null) {
            FirebaseHelper.getUserChats(isAdmin ? lastChatUserId :
                            FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .removeEventListener(messagesListener);
        }
        if (userPresenceRef != null) {
            userPresenceRef.setValue(false);
        }
    }
}