package app.shashi.AdminTalk.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import app.shashi.AdminTalk.R;
import app.shashi.AdminTalk.adapters.MessageAdapter;
import app.shashi.AdminTalk.models.Message;
import app.shashi.AdminTalk.utils.Constants;
import app.shashi.AdminTalk.utils.FirebaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private EditText messageInput;
    private ImageButton sendButton;
    private TextView chatTitle;
    private Toolbar toolbar;

    private FirebaseUser currentUser;
    private DatabaseReference chatRef;
    private boolean isAdmin;
    private String lastChatUserId; 
    private ValueEventListener messagesListener;

    private String selectedUserId;
    private String selectedUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        
        selectedUserId = getIntent().getStringExtra(Constants.EXTRA_USER_ID);
        selectedUserName = getIntent().getStringExtra(Constants.EXTRA_USER_NAME);

        initializeViews();
        setupToolbar();
        initializeChat();
    }


    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        chatTitle = findViewById(R.id.chat_title);
        recyclerView = findViewById(R.id.recycler_view);
        messageInput = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.send_button);

        
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList, currentUser.getUid());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(messageAdapter);

        
        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        chatTitle.setText(selectedUserName != null ? selectedUserName : "Chat");
    }

    private void initializeChat() {
        isAdmin = FirebaseHelper.isAdmin();

        if (isAdmin && selectedUserId != null) {
            
            chatRef = FirebaseHelper.getChatReference(selectedUserId);
            listenToUserMessages(selectedUserId);
        } else if (!isAdmin) {
            
            String currentUserUid = FirebaseHelper.getCurrentUserUid();
            chatRef = FirebaseHelper.getChatReference(currentUserUid);
            listenToUserMessages(currentUserUid);
        }
    }
    private void listenToUserMessages(String userId) {
        ValueEventListener messageListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    Message message = messageSnapshot.getValue(Message.class);
                    if (message != null) {
                        
                        if (!messageExists(message)) {
                            messageList.add(message);
                            lastChatUserId = userId; 
                            sortMessages();
                            messageAdapter.notifyDataSetChanged();
                            recyclerView.scrollToPosition(messageList.size() - 1);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
            }
        };

        FirebaseHelper.getUserChats(userId).addValueEventListener(messageListener);
    }

    private boolean messageExists(Message newMessage) {
        for (Message message : messageList) {
            if (message.getTimestamp() == newMessage.getTimestamp()
                    && message.getSenderId().equals(newMessage.getSenderId())) {
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
        String messageText = messageInput.getText().toString().trim();
        if (!messageText.isEmpty()) {
            String senderId = currentUser.getUid();
            String chatId = isAdmin ? lastChatUserId : senderId;

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
                        senderId,
                        currentUser.getDisplayName()
                );

                messagesRef.child(messageId).setValue(message)
                        .addOnSuccessListener(aVoid -> {
                            messageInput.setText("");
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(ChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show()
                        );
            }
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
    protected void onDestroy() {
        super.onDestroy();
        
        if (messagesListener != null && chatRef != null) {
            chatRef.removeEventListener(messagesListener);
        }
    }
}