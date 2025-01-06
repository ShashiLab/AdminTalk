package app.shashi.AdminTalk;

import android.app.Application;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import app.shashi.AdminTalk.utils.Constants;
import app.shashi.AdminTalk.utils.NotificationHelper;

public class AdminTalkApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);
        remoteConfig.fetchAndActivate().addOnCompleteListener(task -> {
            Constants.ADMIN_UID = remoteConfig.getString("ADMIN_UID");
        });

        NotificationHelper.createNotificationChannel(this);
    }
}
