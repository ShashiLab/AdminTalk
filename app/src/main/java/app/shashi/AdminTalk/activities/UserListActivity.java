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
import com.google.firebase.database.*;
import app.shashi.AdminTalk.R;
import app.shashi.AdminTalk.adapters.UserAdapter;
import app.shashi.AdminTalk.models.User;
import app.shashi.AdminTalk.utils.AuthHelper;
import app.shashi.AdminTalk.utils.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserListActivity extends AppCompatActivity implements UserAdapter.OnUserClickListener {
    private RecyclerView recyclerView;
    private UserAdapter userAdapter;
    private List<User> userList;
    private List<User> filteredList;
    private Map<String, ValueEventListener> presenceListeners;
    private Map<String, Boolean> userPresenceStatus;
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        userList = new ArrayList<>();
        filteredList = new ArrayList<>();
        presenceListeners = new HashMap<>();
        userPresenceStatus = new HashMap<>();

        initializeViews();
        setupSearchView();
        setupRecyclerView();
        loadUsers();
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

    private void filterUsers(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(userList);
        } else {
            String lowercaseQuery = query.toLowerCase().trim();
            for (User user : userList) {
                if (user.getName().toLowerCase().contains(lowercaseQuery) ||
                        user.getEmail().toLowerCase().contains(lowercaseQuery)) {
                    filteredList.add(user);
                }
            }
        }
        sortUsers(filteredList);
        userAdapter.updateList(filteredList);
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.recycler_view);
        userAdapter = new UserAdapter(filteredList, userPresenceStatus, this);

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

    private void sortUsers(List<User> users) {
        Collections.sort(users, (user1, user2) -> {
            Boolean status1 = userPresenceStatus.get(user1.getId());
            Boolean status2 = userPresenceStatus.get(user2.getId());
            boolean isOnline1 = status1 != null && status1;
            boolean isOnline2 = status2 != null && status2;

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
                        
                        for (ValueEventListener listener : presenceListeners.values()) {
                            FirebaseDatabase.getInstance().getReference("presence")
                                    .removeEventListener(listener);
                        }
                        presenceListeners.clear();

                        userList.clear();
                        AuthHelper.isAdmin().addOnCompleteListener(task -> {
                            boolean isAdmin = task.isSuccessful() && task.getResult();

                            for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                User user = userSnapshot.getValue(User.class);
                                if (user != null) {
                                    
                                    if (!isAdmin || !userSnapshot.child("admin").exists()) {
                                        userList.add(user);
                                        setupPresenceListener(user.getId());
                                    }
                                }
                            }
                            filterUsers(searchView.getQuery().toString());
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }
    private void setupPresenceListener(String userId) {
        DatabaseReference presenceRef = FirebaseDatabase.getInstance()
                .getReference("presence").child(userId);

        ValueEventListener presenceListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isOnline = snapshot.getValue(Boolean.class);
                userPresenceStatus.put(userId, isOnline != null && isOnline);
                sortUsers(filteredList);
                userAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                userPresenceStatus.put(userId, false);
                userAdapter.notifyDataSetChanged();
            }
        };

        presenceListeners.put(userId, presenceListener);
        presenceRef.addValueEventListener(presenceListener);
    }

    @Override
    public void onUserClick(User user) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(Constants.EXTRA_USER_ID, user.getId());
        intent.putExtra(Constants.EXTRA_USER_NAME, user.getName());
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (Map.Entry<String, ValueEventListener> entry : presenceListeners.entrySet()) {
            FirebaseDatabase.getInstance().getReference("presence")
                    .child(entry.getKey())
                    .removeEventListener(entry.getValue());
        }
        presenceListeners.clear();
    }
}