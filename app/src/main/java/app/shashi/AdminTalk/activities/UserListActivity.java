package app.shashi.AdminTalk.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.divider.MaterialDividerItemDecoration;
import com.google.firebase.database.*;
import app.shashi.AdminTalk.R;
import app.shashi.AdminTalk.adapters.UserAdapter;
import app.shashi.AdminTalk.models.User;
import app.shashi.AdminTalk.utils.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class UserListActivity extends AppCompatActivity implements UserAdapter.OnUserClickListener {
    private RecyclerView recyclerView;
    private UserAdapter userAdapter;
    private List<User> userList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);
        userList = new ArrayList<>();
        initializeViews();
        setupRecyclerView();
        loadUsers();
    }

    private void initializeViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.recycler_view);
        userAdapter = new UserAdapter(userList, this);

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

    private void loadUsers() {
        FirebaseDatabase.getInstance().getReference(Constants.USERS_REF)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        userList.clear();
                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                            User user = userSnapshot.getValue(User.class);
                            if (user != null && !user.getId().equals(Constants.ADMIN_UID)) {
                                userList.add(user);
                            }
                        }

                        Collections.sort(userList, new Comparator<User>() {
                            @Override
                            public int compare(User user1, User user2) {
                                return user1.getName().compareTo(user2.getName());
                            }
                        });

                        userAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        
                    }
                });
    }

    @Override
    public void onUserClick(User user) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(Constants.EXTRA_USER_ID, user.getId());
        intent.putExtra(Constants.EXTRA_USER_NAME, user.getName());
        startActivity(intent);
    }
}