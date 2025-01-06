package app.shashi.AdminTalk.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;
import app.shashi.AdminTalk.models.Message;
import app.shashi.AdminTalk.utils.Constants;
import app.shashi.AdminTalk.utils.FirebaseHelper;
import app.shashi.AdminTalk.utils.NotificationHelper;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import android.app.Notification;
import android.app.PendingIntent;
import androidx.core.app.NotificationCompat;
import app.shashi.AdminTalk.R;
import app.shashi.AdminTalk.activities.ChatActivity;


public class MessageNotificationService extends Service {
    private DatabaseReference databaseRef;
    private ChildEventListener messagesListener;
    private boolean isAdmin;
    private String currentUserId;
    private static final int FOREGROUND_SERVICE_ID = 1001;


    @Override
    public void onCreate() {
        super.onCreate();
        currentUserId = FirebaseHelper.getCurrentUserUid();
        isAdmin = FirebaseHelper.isAdmin();
        startForeground(FOREGROUND_SERVICE_ID, createForegroundNotification());
        setupMessageListener();
    }

    private Notification createForegroundNotification() {
        Intent notificationIntent = new Intent(this, ChatActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
                .setContentTitle("AdminTalk")
                .setContentText("Listening for new messages")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        
        return START_STICKY;
    }
    private void setupMessageListener() {
        if (isAdmin) {
            
            databaseRef = FirebaseDatabase.getInstance().getReference(Constants.CHATS_REF);
            listenToAllChats();
        } else {
            
            databaseRef = FirebaseHelper.getChatReference(currentUserId)
                    .child(Constants.MESSAGES_REF);
            listenToUserChat();
        }
    }

    private void listenToAllChats() {
        databaseRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot chatSnapshot, String previousChildName) {
                
                String userId = chatSnapshot.getKey();
                if (!userId.equals(Constants.ADMIN_UID)) {
                    DatabaseReference messagesRef = chatSnapshot.getRef()
                            .child(Constants.MESSAGES_REF);
                    listenToMessages(messagesRef, userId);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {}
            @Override
            public void onChildRemoved(DataSnapshot snapshot) {}
            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}
            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    private void listenToUserChat() {
        messagesListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                Message message = snapshot.getValue(Message.class);
                if (message != null && !message.getSenderId().equals(currentUserId)) {
                    showNotification(message);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {}
            @Override
            public void onChildRemoved(DataSnapshot snapshot) {}
            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}
            @Override
            public void onCancelled(DatabaseError error) {}
        };
        databaseRef.addChildEventListener(messagesListener);
    }

    private void listenToMessages(DatabaseReference messagesRef, String userId) {
        messagesRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                Message message = snapshot.getValue(Message.class);
                if (message != null && !message.getSenderId().equals(currentUserId)) {
                    showNotification(message);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {}
            @Override
            public void onChildRemoved(DataSnapshot snapshot) {}
            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}
            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    private void showNotification(Message message) {
        String title = message.getSenderName();
        NotificationHelper.showMessageNotification(
                this,
                title,
                message.getText(),
                message.getSenderId(),
                message.getSenderName()
        );
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (messagesListener != null) {
            databaseRef.removeEventListener(messagesListener);
        }
    }
}