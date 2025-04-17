package com.example.finalproject_vertigrow.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.finalproject_vertigrow.R;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

public class LinkAccountActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText passwordEditText;
    private Button linkButton, cancelButton;
    private String email;
    private AuthCredential googleCredential;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link_account);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        // Get data from intent
        email = getIntent().getStringExtra("email");
        String googleIdToken = getIntent().getStringExtra("googleIdToken");
        googleCredential = GoogleAuthProvider.getCredential(googleIdToken, null);

        // Initialize views
        passwordEditText = findViewById(R.id.password_edit_text);
        linkButton = findViewById(R.id.link_button);
        cancelButton = findViewById(R.id.cancel_button);

        // Set up title and instructions
        TextView titleTextView = findViewById(R.id.title_text_view);
        TextView instructionsTextView = findViewById(R.id.instructions_text_view);
        
        titleTextView.setText("Link Your Accounts");
        instructionsTextView.setText("This email (" + email + 
                ") already has an account. Please enter your manual account password to link with Google Sign-in.");

        // Set up buttons
        linkButton.setOnClickListener(v -> linkAccounts());
        cancelButton.setOnClickListener(v -> {
            // Sign out from Google
            mAuth.signOut();
            finish();
        });
    }

    private void linkAccounts() {
        String password = passwordEditText.getText().toString().trim();
        
        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            return;
        }

        // Show progress
        findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);
        
        // First sign in with email/password
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Now link with Google credential
                    mAuth.getCurrentUser().linkWithCredential(googleCredential)
                        .addOnCompleteListener(linkTask -> {
                            findViewById(R.id.progress_bar).setVisibility(View.GONE);
                            
                            if (linkTask.isSuccessful()) {
                                Toast.makeText(LinkAccountActivity.this, 
                                    "Accounts linked successfully!", Toast.LENGTH_SHORT).show();
                                    
                                // Get current user details from Firestore
                                FirebaseUser user = mAuth.getCurrentUser();
                                if (user != null) {
                                    checkUserInFirestore(user.getUid());
                                }
                            } else {
                                Toast.makeText(LinkAccountActivity.this,
                                    "Failed to link accounts: " + linkTask.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                            }
                        });
                } else {
                    findViewById(R.id.progress_bar).setVisibility(View.GONE);
                    Toast.makeText(LinkAccountActivity.this,
                        "Incorrect password. Please try again.",
                        Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    private void checkUserInFirestore(String userId) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener(document -> {
                if (document.exists()) {
                    String role = document.getString("role");
                    String name = document.getString("name");
                    
                    // Navigate to appropriate screen
                    Intent intent;
                    if ("admin".equals(role)) {
                        intent = new Intent(this, AdminActivity.class);
                    } else {
                        intent = new Intent(this, UserActivity.class);
                    }
                    intent.putExtra("userName", name);
                    intent.putExtra("userRole", role);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                    finish();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                mAuth.signOut();
                finish();
            });
    }
} 