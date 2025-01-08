package app.shashi.AdminTalk;

import android.app.Application;

import app.shashi.AdminTalk.utils.NotificationHelper;

public class AdminTalkApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.createNotificationChannel(this);
    }
}
