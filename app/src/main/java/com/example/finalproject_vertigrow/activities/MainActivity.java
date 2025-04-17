package com.example.finalproject_vertigrow.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.finalproject_vertigrow.R;
import com.example.finalproject_vertigrow.models.User;
import com.google.android.gms.auth.api.signin.*;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // Constants
    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = "MainActivity";

    // Firebase instances
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;

    // UI Elements
    private EditText emailEditText, passwordEditText;
    private Button loginButton, googleSignInButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeFirebase();
        initializeViews();
        setupGoogleSignIn();
        setupClickListeners();
        checkExistingUser();
    }

    // Initialization Methods
    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    private void initializeViews() {
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        googleSignInButton = findViewById(R.id.googleSignInButton);
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> handleEmailLogin());
        googleSignInButton.setOnClickListener(v -> handleGoogleSignIn());
    }

    // Authentication Methods
    private void handleEmailLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkUserInFirestore(user.getUid());
                        }
                    } else {
                        Toast.makeText(MainActivity.this,
                                "Login Failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleGoogleSignIn() {
        try {
            // Start Google sign-in directly without showing initial warning
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        } catch (Exception e) {
            Log.e(TAG, "Error launching sign in: " + e.getMessage());
            Toast.makeText(this, "Sign in error: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            handleGoogleSignInResult(data);
        }
    }

    private void handleGoogleSignInResult(Intent data) {
        GoogleSignIn.getSignedInAccountFromIntent(data)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        GoogleSignInAccount account = task.getResult();
                        firebaseAuthWithGoogle(account);
                    } else {
                        Toast.makeText(this, "Google sign-in failed: " +
                                task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        // Get the email from Google
        String email = account.getEmail();
        
        // First check if this email exists in Firestore
        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!queryDocumentSnapshots.isEmpty()) {
                    // User with this email exists
                    DocumentSnapshot userDoc = queryDocumentSnapshots.getDocuments().get(0);
                    String firebaseUserId = userDoc.getId();
                    String googleUid = userDoc.getString("googleUid");
                    String name = userDoc.getString("name");
                    
                    if (googleUid == null) {
                        // First time Google sign-in for existing email user
                        // Show warning dialog
                        new AlertDialog.Builder(this)
                            .setTitle("Account Found")
                            .setMessage("We found an existing account for " + email + ". Do you want to link it with your Google account?\n\nAfter linking, you'll be able to sign in using either method.")
                            .setPositiveButton("Link Accounts", (dialog, which) -> {
                                // We need the user's password to link accounts properly
                                showPasswordEntryDialog(account, firebaseUserId);
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> {
                                // User canceled, sign out from Google
                                mGoogleSignInClient.signOut();
                                mAuth.signOut();
                            })
                            .setCancelable(false)
                            .show();
                    } else {
                        // User already has a Google UID, use it
                        proceedWithGoogleSignIn(account);
                    }
                } else {
                    // No user with this email
                    Toast.makeText(this, "Unauthorized account. Please contact an administrator.", Toast.LENGTH_LONG).show();
                    mGoogleSignInClient.signOut();
                    mAuth.signOut();
                }
            })
            .addOnFailureListener(e -> {
                Log.e("MainActivity", "Error checking user email", e);
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    // Show a dialog to get the user's password for account linking
    private void showPasswordEntryDialog(GoogleSignInAccount googleAccount, String userId) {
        // Create a custom dialog with password field
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_password_entry, null);
        builder.setView(view);
        
        // Get references to views
        EditText passwordEditText = view.findViewById(R.id.password_edit_text);
        Button cancelButton = view.findViewById(R.id.cancel_button);
        Button linkButton = view.findViewById(R.id.link_button);
        
        // Create the dialog
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        
        // Set button click listeners
        cancelButton.setOnClickListener(v -> {
            dialog.dismiss();
            // User canceled, sign out from Google
            mGoogleSignInClient.signOut();
            mAuth.signOut();
        });
        
        linkButton.setOnClickListener(v -> {
            String password = passwordEditText.getText().toString().trim();
            if (password.isEmpty()) {
                passwordEditText.setError("Please enter your password");
                return;
            }
            
            // Get user email from Google account
            String email = googleAccount.getEmail();
            
            // Sign in with email/password first
            mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    // Successfully authenticated with email/password
                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    
                    // Get Google credentials
                    AuthCredential googleCredential = 
                        GoogleAuthProvider.getCredential(googleAccount.getIdToken(), null);
                    
                    // Link the Google credential to this user
                    firebaseUser.linkWithCredential(googleCredential)
                        .addOnSuccessListener(linkResult -> {
                            // Update Firestore with googleUid
                            db.collection("users").document(userId)
                                .update("googleUid", googleAccount.getId())
                                .addOnSuccessListener(aVoid -> {
                                    dialog.dismiss();
                                    Toast.makeText(MainActivity.this, 
                                        "Account successfully linked with Google", 
                                        Toast.LENGTH_SHORT).show();
                                    
                                    // Check user in Firestore and proceed
                                    checkUserInFirestore(userId);
                                })
                                .addOnFailureListener(e -> {
                                    dialog.dismiss();
                                    Toast.makeText(MainActivity.this,
                                        "Error updating account: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                                });
                        })
                        .addOnFailureListener(e -> {
                            dialog.dismiss();
                            Toast.makeText(MainActivity.this,
                                "Error linking accounts: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                            
                            // Sign out both
                            mGoogleSignInClient.signOut();
                            mAuth.signOut();
                        });
                })
                .addOnFailureListener(e -> {
                    // Authentication failed
                    passwordEditText.setError("Incorrect password");
                    Toast.makeText(MainActivity.this,
                        "Authentication failed: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                });
        });
        
        dialog.show();
    }

    private void proceedWithGoogleSignIn(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        checkUserInFirestore(user.getUid());
                    }
                } else {
                    Toast.makeText(this, "Authentication failed: " + task.getException().getMessage(), 
                            Toast.LENGTH_SHORT).show();
                }
                });
    }

    // Firestore Methods
    private void checkExistingUser() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            checkUserInFirestore(currentUser.getUid());
        }
    }

    private void checkUserInFirestore(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String role = document.getString("role");
                        String name = document.getString("name");
                        String googleUid = document.getString("googleUid");
                        
                        // Check if this is a Google sign-in and this document has a googleUid
                        FirebaseUser currentUser = mAuth.getCurrentUser();
                        if (currentUser != null && googleUid != null && !googleUid.equals(userId)) {
                            // Check if this user is signed in with Google
                            boolean isGoogleSignIn = false;
                            for (UserInfo profile : currentUser.getProviderData()) {
                                if (GoogleAuthProvider.PROVIDER_ID.equals(profile.getProviderId())) {
                                    isGoogleSignIn = true;
                                    break;
                                }
                            }
                            
                            if (isGoogleSignIn) {
                                // This is a Google sign-in but we have the wrong document
                                // Get the document with the correct ID
                                db.collection("users").document(googleUid)
                                        .get()
                                        .addOnSuccessListener(correctDoc -> {
                                            if (correctDoc.exists()) {
                                                // Use data from the correct document
                                                String correctRole = correctDoc.getString("role");
                                                String correctName = correctDoc.getString("name");
                                                Boolean passwordChanged = correctDoc.getBoolean("passwordChanged");
                                                
                                                if (passwordChanged != null && passwordChanged) {
                                                    navigateToUserScreen(correctName, correctRole);
                                                } else {
                                                    navigateToPasswordChange(correctName, correctRole);
                                                }
                                            } else {
                                                // Fallback to original document if correct one not found
                                                checkPasswordChanged(document);
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            // Fallback to original document on error
                                            checkPasswordChanged(document);
                                        });
                            } else {
                                // Normal flow - check if password change needed
                                checkPasswordChanged(document);
                            }
                        } else {
                            // Normal flow - check if password change needed
                            checkPasswordChanged(document);
                        }
                    } else {
                        Toast.makeText(this, "User data not found",
                                Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                });
    }
    
    // Helper method to check if password change is needed
    private void checkPasswordChanged(DocumentSnapshot document) {
        String role = document.getString("role");
        String name = document.getString("name");
        Boolean passwordChanged = document.getBoolean("passwordChanged");
        
        if (passwordChanged != null && passwordChanged) {
            // Password already changed, proceed to normal flow
            navigateToUserScreen(name, role);
        } else {
            // Password change required
            navigateToPasswordChange(name, role);
        }
    }

    // Navigation Methods
    private void navigateToPasswordChange(String name, String role) {
        Intent intent = new Intent(this, PasswordChangeActivity.class);
        intent.putExtra("userName", name);
        intent.putExtra("userRole", role);
        startActivity(intent);
        finish();
    }

    private void navigateToUserScreen(String name, String role) {
        Intent intent;
        if ("admin".equals(role)) {
            intent = new Intent(this, AdminActivity.class);
        } else {
            intent = new Intent(this, UserActivity.class);
        }
        intent.putExtra("userName", name);
        intent.putExtra("userRole", role);
        startActivity(intent);
        finish();
    }
}