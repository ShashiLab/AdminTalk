package app.shashi.AdminTalk.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import java.util.Objects;

public class FirebaseHelper {
    private static DatabaseReference database = FirebaseDatabase.getInstance().getReference();

    public static DatabaseReference getUserPresenceReference(String userId) {
        return FirebaseDatabase.getInstance().getReference("presence").child(userId);
    }
    public static String getCurrentUserUid() {
        return Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
    }

    public static boolean isAdmin() {
        return false;
    }
    public static DatabaseReference getChatReference(String userId) {
        return database.child(Constants.CHATS_REF)
                .child(userId);
    }

    public static Query getUserChats(String userId) {
        return database.child(Constants.CHATS_REF)
                .child(userId)
                .child(Constants.MESSAGES_REF)
                .orderByChild("timestamp");
    }
}
