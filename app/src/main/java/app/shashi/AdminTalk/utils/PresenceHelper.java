package app.shashi.AdminTalk.utils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

import java.util.HashMap;
import java.util.Map;


public class PresenceHelper {
    private static Map<String, ValueEventListener> presenceListeners = new HashMap<>();
    private static DatabaseReference userPresenceRef;
    private static DatabaseReference connectedRef;
    private static ValueEventListener connectedListener;

    public static void initializePresence(String userId, String chatId) {
        if (userId == null) return;

        connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        
        userPresenceRef = FirebaseHelper.getChatReference(chatId)
                .child("presence")
                .child(userId);

        if (connectedListener != null) {
            connectedRef.removeEventListener(connectedListener);
        }

        connectedListener = connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class) != null &&
                        snapshot.getValue(Boolean.class);

                if (connected) {
                    Map<String, Object> presenceData = new HashMap<>();
                    presenceData.put("online", true);
                    presenceData.put("lastSeen", ServerValue.TIMESTAMP);

                    Map<String, Object> offlineData = new HashMap<>();
                    offlineData.put("online", false);
                    offlineData.put("lastSeen", ServerValue.TIMESTAMP);

                    userPresenceRef.onDisconnect().setValue(offlineData);
                    userPresenceRef.setValue(presenceData);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                
            }
        });
    }

    public static void monitorPresence(String chatId, String userId, PresenceCallback callback) {
        DatabaseReference presenceRef = FirebaseHelper.getChatReference(chatId)
                .child("presence")
                .child(userId);

        ValueEventListener presenceListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    boolean isOnline = snapshot.child("online").getValue(Boolean.class);
                    long lastSeen = snapshot.child("lastSeen").getValue(Long.class);
                    callback.onPresenceChanged(isOnline, lastSeen);
                } else {
                    callback.onPresenceChanged(false, 0);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onPresenceChanged(false, 0);
            }
        };

        presenceListeners.put(userId, presenceListener);
        presenceRef.addValueEventListener(presenceListener);
    }

    public static void updatePresence(String chatId, String userId, boolean online) {
        if (userId == null) return;

        Map<String, Object> presenceData = new HashMap<>();
        presenceData.put("online", online);
        presenceData.put("lastSeen", ServerValue.TIMESTAMP);

        FirebaseHelper.getChatReference(chatId)
                .child("presence")
                .child(userId)
                .setValue(presenceData);
    }

    public static void cleanup(String chatId, String userId) {
        if (connectedListener != null && connectedRef != null) {
            connectedRef.removeEventListener(connectedListener);
            connectedListener = null;
        }

        ValueEventListener listener = presenceListeners.remove(userId);
        if (listener != null) {
            FirebaseHelper.getChatReference(chatId)
                    .child("presence")
                    .child(userId)
                    .removeEventListener(listener);
        }
    }

    public interface PresenceCallback {
        void onPresenceChanged(boolean isOnline, long lastSeen);
    }
}