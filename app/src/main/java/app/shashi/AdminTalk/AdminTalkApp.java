package app.shashi.AdminTalk;

import android.app.Application;
import com.google.firebase.auth.FirebaseAuth;
import app.shashi.AdminTalk.utils.NotificationHelper;
import app.shashi.AdminTalk.utils.PresenceHelper;

public class AdminTalkApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.createNotificationChannel(this);

        
        FirebaseAuth.getInstance().addAuthStateListener(auth -> {
            if (auth.getCurrentUser() != null) {
                String userId = auth.getCurrentUser().getUid();
                
                PresenceHelper.initializePresence(userId, userId);
            }
        });
    }
}