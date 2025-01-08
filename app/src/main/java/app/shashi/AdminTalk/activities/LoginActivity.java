package app.shashi.AdminTalk.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import app.shashi.AdminTalk.R;
import app.shashi.AdminTalk.models.User;
import app.shashi.AdminTalk.utils.AuthHelper;
import app.shashi.AdminTalk.utils.Constants;
import app.shashi.AdminTalk.utils.FirebaseHelper;
import app.shashi.AdminTalk.services.MessageNotificationService;
import app.shashi.AdminTalk.utils.PermissionHelper;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

public class LoginActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 9001;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private ImageView signInButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (!PermissionHelper.hasNotificationPermission(this)) {
            PermissionHelper.requestNotificationPermission(this);
        }

        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        signInButton = findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(v -> signIn());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PermissionHelper.NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this)
                        .setTitle("Notification Permission Required")
                        .setMessage("Notifications are required to receive new message alerts. Would you like to enable them in settings?")
                        .setPositiveButton("Settings", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

                dialogBuilder.show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            startNotificationService();
            startChatActivity();
        }
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToDatabase(user);
                            startNotificationService();
                        }
                        startChatActivity();
                    } else {
                        Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToDatabase(FirebaseUser user) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference(Constants.USERS_REF);
        User userProfile = new User(
                user.getUid(),
                user.getDisplayName(),
                user.getEmail(),
                user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null
        );
        usersRef.child(user.getUid()).setValue(userProfile);
    }

    private void startNotificationService() {
        Intent serviceIntent = new Intent(this, MessageNotificationService.class);
        startService(serviceIntent);
    }

    private void startChatActivity() {
        AuthHelper.isAdmin().addOnCompleteListener(task -> {
            boolean isAdmin = task.isSuccessful() && task.getResult();
            Intent intent = isAdmin ?
                    new Intent(this, UserListActivity.class) :
                    new Intent(this, ChatActivity.class);
            startActivity(intent);
            finish();
        });
    }
}