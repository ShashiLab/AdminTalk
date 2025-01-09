package app.shashi.AdminTalk.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.divider.MaterialDividerItemDecoration;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import app.shashi.AdminTalk.R;
import app.shashi.AdminTalk.adapters.UserAdapter;
import app.shashi.AdminTalk.models.User;
import app.shashi.AdminTalk.models.Message;
import app.shashi.AdminTalk.utils.AuthHelper;
import app.shashi.AdminTalk.utils.Constants;
import app.shashi.AdminTalk.utils.FirebaseHelper;
import app.shashi.AdminTalk.utils.PresenceHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserListActivity extends AppCompatActivity implements
        UserAdapter.OnUserClickListener,
        UserAdapter.OnProfileClickListener {

    private RecyclerView recyclerView;
    private UserAdapter userAdapter;
    private List<User> userList;
    private List<User> filteredList;
    private Map<String, ValueEventListener> presenceListeners;
    private Map<String, ValueEventListener> messageListeners;
    private Map<String, UserPresenceInfo> userPresenceStatus;
    private Map<String, MessagePreview> messagePreviews;
    private SearchView searchView;
    private String currentUserId;
    private boolean isAdmin = false;

    public static class UserPresenceInfo {
        public boolean isOnline;
        public long lastSeen;

        UserPresenceInfo(boolean isOnline, long lastSeen) {
            this.isOnline = isOnline;
            this.lastSeen = lastSeen;
        }
    }

    public static class MessagePreview {
        public String lastMessage;
        public long timestamp;

        MessagePreview(String lastMessage, long timestamp) {
            this.lastMessage = lastMessage;
            this.timestamp = timestamp;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        initializeLists();
        initializeViews();
        setupSearchView();
        setupRecyclerView();
        checkAdminStatus();
    }

    private void initializeLists() {
        userList = new ArrayList<>();
        filteredList = new ArrayList<>();
        presenceListeners = new HashMap<>();
        messageListeners = new HashMap<>();
        userPresenceStatus = new HashMap<>();
        messagePreviews = new HashMap<>();
    }

    private void initializeViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        searchView = findViewById(R.id.search_view);
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterUsers(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterUsers(newText);
                return true;
            }
        });
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.recycler_view);
        userAdapter = new UserAdapter(filteredList, userPresenceStatus, messagePreviews, this, this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(userAdapter);

        MaterialDividerItemDecoration divider = new MaterialDividerItemDecoration(
                this,
                LinearLayoutManager.VERTICAL
        );
        divider.setLastItemDecorated(false);
        recyclerView.addItemDecoration(divider);
    }

    private void checkAdminStatus() {
        AuthHelper.isAdmin().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                isAdmin = task.getResult();
                loadUsers();
            }
        });
    }

    private void filterUsers(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(userList);
        } else {
            String lowercaseQuery = query.toLowerCase().trim();
            for (User user : userList) {
                if (user.getName().toLowerCase().contains(lowercaseQuery)) {
                    filteredList.add(user);
                }
            }
        }
        sortUsers(filteredList);
        userAdapter.updateList(filteredList);
    }

    private void sortUsers(List<User> users) {
        Collections.sort(users, (user1, user2) -> {
            MessagePreview preview1 = messagePreviews.get(user1.getId());
            MessagePreview preview2 = messagePreviews.get(user2.getId());

            
            if (preview1 != null && preview2 != null) {
                int timeCompare = Long.compare(preview2.timestamp, preview1.timestamp);
                if (timeCompare != 0) return timeCompare;
            } else if (preview1 != null) {
                return -1;
            } else if (preview2 != null) {
                return 1;
            }

            
            UserPresenceInfo status1 = userPresenceStatus.get(user1.getId());
            UserPresenceInfo status2 = userPresenceStatus.get(user2.getId());
            boolean isOnline1 = status1 != null && status1.isOnline;
            boolean isOnline2 = status2 != null && status2.isOnline;

            if (isOnline1 != isOnline2) {
                return isOnline1 ? -1 : 1;
            }

            
            return user1.getName().compareTo(user2.getName());
        });
    }

    private void loadUsers() {
        FirebaseDatabase.getInstance().getReference(Constants.USERS_REF)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        cleanupListeners();
                        userList.clear();

                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                            User user = userSnapshot.getValue(User.class);
                            if (user != null && !user.getId().equals(currentUserId)) {
                                boolean isUserAdmin = userSnapshot.child("admin").exists();
                                if ((isAdmin && !isUserAdmin) || (!isAdmin && isUserAdmin)) {
                                    userList.add(user);
                                    setupPresenceListener(user.getId());
                                    setupMessageListener(user.getId());
                                }
                            }
                        }
                        filterUsers(searchView.getQuery().toString());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void setupMessageListener(String userId) {
        String chatId = isAdmin ? userId : currentUserId;
        DatabaseReference messagesRef = FirebaseHelper.getChatReference(chatId)
                .child(Constants.MESSAGES_REF);

        ValueEventListener messageListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Message lastMessage = null;
                long lastTimestamp = 0;

                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    Message message = messageSnapshot.getValue(Message.class);
                    if (message != null && message.getTimestamp() > lastTimestamp) {
                        lastMessage = message;
                        lastTimestamp = message.getTimestamp();
                    }
                }

                if (lastMessage != null) {
                    messagePreviews.put(userId, new MessagePreview(
                            lastMessage.getText(),
                            lastMessage.getTimestamp()
                    ));
                    sortUsers(filteredList);
                    userAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        messageListeners.put(userId, messageListener);
        messagesRef.addValueEventListener(messageListener);
    }

    private void setupPresenceListener(String userId) {
        String chatId = isAdmin ? userId : currentUserId;
        ValueEventListener presenceListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    boolean isOnline = snapshot.child("online").getValue(Boolean.class);
                    long lastSeen = snapshot.child("lastSeen").getValue(Long.class);
                    userPresenceStatus.put(userId, new UserPresenceInfo(isOnline, lastSeen));
                } else {
                    userPresenceStatus.put(userId, new UserPresenceInfo(false, 0));
                }
                sortUsers(filteredList);
                userAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                userPresenceStatus.put(userId, new UserPresenceInfo(false, 0));
                userAdapter.notifyDataSetChanged();
            }
        };

        presenceListeners.put(userId, presenceListener);
        FirebaseHelper.getChatReference(chatId)
                .child("presence")
                .child(userId)
                .addValueEventListener(presenceListener);
    }

    private void cleanupListeners() {
        for (Map.Entry<String, ValueEventListener> entry : presenceListeners.entrySet()) {
            String chatId = isAdmin ? entry.getKey() : currentUserId;
            FirebaseHelper.getChatReference(chatId)
                    .child("presence")
                    .child(entry.getKey())
                    .removeEventListener(entry.getValue());
        }

        for (Map.Entry<String, ValueEventListener> entry : messageListeners.entrySet()) {
            String chatId = isAdmin ? entry.getKey() : currentUserId;
            FirebaseHelper.getChatReference(chatId)
                    .child(Constants.MESSAGES_REF)
                    .removeEventListener(entry.getValue());
        }

        presenceListeners.clear();
        messageListeners.clear();
        userPresenceStatus.clear();
        messagePreviews.clear();
    }

    @Override
    public void onUserClick(User user) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(Constants.EXTRA_USER_ID, user.getId());
        intent.putExtra(Constants.EXTRA_USER_NAME, user.getName());
        startActivity(intent);
    }

    @Override
    public void onProfileClick(User user) {
        Intent intent = new Intent(this, AboutUser.class);
        intent.putExtra(Constants.EXTRA_USER_ID, user.getId());
        intent.putExtra(Constants.EXTRA_USER_NAME, user.getName());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentUserId != null) {
            for (User user : userList) {
                String chatId = isAdmin ? user.getId() : currentUserId;
                PresenceHelper.updatePresence(chatId, currentUserId, true);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (currentUserId != null) {
            for (User user : userList) {
                String chatId = isAdmin ? user.getId() : currentUserId;
                PresenceHelper.updatePresence(chatId, currentUserId, false);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanupListeners();
    }
}