package com.example.finalproject_vertigrow.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.example.finalproject_vertigrow.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;

public class AdminActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private Button logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Initialize views
        logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> performSignOut());
    }

    private void performSignOut() {
        // Sign out from Firebase
        mAuth.signOut();

        // Sign out from Google
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, task -> {
                    // Revoke access to make sure user has to pick account next time
                    mGoogleSignInClient.revokeAccess()
                            .addOnCompleteListener(revokeTask -> {
                                Toast.makeText(AdminActivity.this,
                                        "Logged out successfully", Toast.LENGTH_SHORT).show();

                                // Redirect to login screen
                                Intent intent = new Intent(AdminActivity.this, MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            });
                });
    }
}