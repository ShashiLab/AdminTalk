package app.shashi.AdminTalk.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import app.shashi.AdminTalk.services.MessageNotificationService;
import com.google.firebase.auth.FirebaseAuth;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                Intent serviceIntent = new Intent(context, MessageNotificationService.class);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            }
        }
    }
}