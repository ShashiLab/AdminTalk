package app.shashi.AdminTalk.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.GetTokenResult;

public class AuthHelper {
    private static final String ADMIN_CLAIM = "admin";
    private static Boolean isAdminCached = null;

    public static Task<Boolean> isAdmin() {
        if (isAdminCached != null) {
            TaskCompletionSource<Boolean> taskCompletionSource = new TaskCompletionSource<>();
            taskCompletionSource.setResult(isAdminCached);
            return taskCompletionSource.getTask();
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            TaskCompletionSource<Boolean> taskCompletionSource = new TaskCompletionSource<>();
            taskCompletionSource.setResult(false);
            return taskCompletionSource.getTask();
        }

        return currentUser.getIdToken(true)
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        return false;
                    }
                    GetTokenResult tokenResult = task.getResult();
                    isAdminCached = tokenResult.getClaims().containsKey(ADMIN_CLAIM) &&
                            (boolean) tokenResult.getClaims().get(ADMIN_CLAIM);
                    return isAdminCached;
                });
    }

    public static void clearCache() {
        isAdminCached = null;
    }
}