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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.finalproject_vertigrow.R;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.play.core.integrity.IntegrityManager;
import com.google.android.play.core.integrity.IntegrityManagerFactory;
import com.google.android.play.core.integrity.IntegrityTokenRequest;
import com.google.android.play.core.integrity.IntegrityTokenResponse;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // Constants
    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = "MainActivity";
    // Your cloud project number from Google Cloud Console - UPDATED TO CORRECT VALUE
    private static final long CLOUD_PROJECT_NUMBER = 42961822919L;

    // Firebase instances
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    
    // Play Integrity
    private IntegrityManager integrityManager;
    
    // Progress Dialog
    private AlertDialog progressDialog;

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
        initializeIntegrityManager();
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
        loginButton.setOnClickListener(v -> {
            // Use Play Integrity to protect email login
            executeEmailLoginAction(v);
        });
        googleSignInButton.setOnClickListener(v -> {
            // Use Play Integrity to protect Google sign-in
            executeGoogleLoginAction(v);
        });
    }
    
    // Initialize Play Integrity
    private void initializeIntegrityManager() {
        integrityManager = IntegrityManagerFactory.create(getApplicationContext());
    }
    
    // Verify app integrity using Play Integrity API
    private void verifyAppIntegrity() {
        Log.d(TAG, "Verifying app integrity...");
        
        // Generate a nonce (unique for each request)
        String nonce = UUID.randomUUID().toString();
        
        IntegrityTokenRequest request = IntegrityTokenRequest.builder()
            .setCloudProjectNumber(CLOUD_PROJECT_NUMBER)
            .setNonce(nonce)
            .build();
            
        integrityManager.requestIntegrityToken(request)
            .addOnSuccessListener(
                this,
                new OnSuccessListener<IntegrityTokenResponse>() {
                    @Override
                    public void onSuccess(IntegrityTokenResponse response) {
                        String token = response.token();
                        
                        // More detailed logging for debugging
                        Log.d(TAG, "======= PLAY INTEGRITY SUCCESS =======");
                        Log.d(TAG, "Integrity token received: " + 
                              (token.length() > 10 ? token.substring(0, 10) + "..." : "empty"));
                        Log.d(TAG, "Token length: " + token.length());
                        Log.d(TAG, "========================================");
                        
                        // Hide progress dialog
                        hideSecurityCheckDialog();
                        
                        // Show success animation
                        showSecuritySuccess();
                        
                        // Proceed with login after integrity check
                            handleEmailLogin();
                    }
                })
            .addOnFailureListener(
                this,
                new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // More detailed logging for debugging
                        Log.e(TAG, "======= PLAY INTEGRITY FAILED =======");
                        Log.e(TAG, "Integrity verification failed: " + e.getMessage());
                        
                        // Log more specific error details if possible
                        if (e instanceof ApiException) {
                            ApiException apiException = (ApiException) e;
                            Log.e(TAG, "Error code: " + apiException.getStatusCode());
                            Log.e(TAG, "Error details: " + CommonStatusCodes.getStatusCodeString(apiException.getStatusCode()));
                        }
                        
                        Log.e(TAG, "======================================");
                        
                        // Hide progress dialog
                        hideSecurityCheckDialog();
                        
                        Toast.makeText(MainActivity.this, 
                            "App integrity check failed, but allowing login for development", 
                            Toast.LENGTH_LONG).show();
                    }
                });
    }
    
    // Execute login with Play Integrity check for email login
    private void executeEmailLoginAction(View v) {
        // First validate the input fields
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Now proceed with security check
        // Show security check dialog
        showSecurityCheckDialog();
        
        // Generate a nonce (unique for each request)
        String nonce = UUID.randomUUID().toString();
        
        Log.d(TAG, "==== STARTING PLAY INTEGRITY CHECK ====");
        Log.d(TAG, "Using Cloud Project Number: " + CLOUD_PROJECT_NUMBER);
        Log.d(TAG, "Generated nonce: " + nonce);
        
        try {
            IntegrityTokenRequest request = IntegrityTokenRequest.builder()
                .setCloudProjectNumber(CLOUD_PROJECT_NUMBER)
                .setNonce(nonce)
                .build();
                
            integrityManager.requestIntegrityToken(request)
                .addOnSuccessListener(
                    this,
                    new OnSuccessListener<IntegrityTokenResponse>() {
                        @Override
                        public void onSuccess(IntegrityTokenResponse response) {
                            String token = response.token();
                            
                            // More detailed logging for debugging
                            Log.d(TAG, "======= PLAY INTEGRITY SUCCESS =======");
                            Log.d(TAG, "Integrity token received: " + 
                                (token.length() > 10 ? token.substring(0, 10) + "..." : "empty"));
                            Log.d(TAG, "Token length: " + token.length());
                            Log.d(TAG, "========================================");
                            
                            // Show a visual indicator of success
                            Toast.makeText(MainActivity.this, "Security check passed!", Toast.LENGTH_SHORT).show();
                            
                            // Hide progress dialog
                            hideSecurityCheckDialog();
                            
                            // Show success animation
                            showSecuritySuccess();
                            
                            // Proceed with login after integrity check
                            handleEmailLogin();
                        }
                    })
                .addOnFailureListener(
                    this,
                    new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // More detailed logging for debugging
                            Log.e(TAG, "======= PLAY INTEGRITY FAILED =======");
                            Log.e(TAG, "Integrity verification failed: " + e.getMessage());
                            
                            // Log more specific error details if possible
                            if (e instanceof ApiException) {
                                ApiException apiException = (ApiException) e;
                                Log.e(TAG, "Error code: " + apiException.getStatusCode());
                                Log.e(TAG, "Error details: " + CommonStatusCodes.getStatusCodeString(apiException.getStatusCode()));
                            }
                            
                            Log.e(TAG, "======================================");
                            
                            // Show a visual indicator of failure
                            Toast.makeText(MainActivity.this, "Security check failed! Login blocked.", Toast.LENGTH_LONG).show();
                            
                            // Hide progress dialog
                            hideSecurityCheckDialog();
                            
                            // Show security error dialog
                            showSecurityError(e);
                            
                            // SECURITY BLOCK: Don't allow login on failure
                            // handleEmailLogin(); -- REMOVED
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "EXCEPTION CREATING INTEGRITY REQUEST: " + e.getMessage(), e);
            Toast.makeText(this, "Error setting up security check: " + e.getMessage(), Toast.LENGTH_LONG).show();
            hideSecurityCheckDialog();
        }
    }
    
    // Execute login with Play Integrity check for Google sign-in
    private void executeGoogleLoginAction(View v) {
        // Show security check dialog
        showSecurityCheckDialog();
        
        // Generate a nonce (unique for each request)
        String nonce = UUID.randomUUID().toString();
        
        Log.d(TAG, "==== STARTING PLAY INTEGRITY CHECK (GOOGLE) ====");
        Log.d(TAG, "Using Cloud Project Number: " + CLOUD_PROJECT_NUMBER);
        Log.d(TAG, "Generated nonce: " + nonce);
        
        try {
            IntegrityTokenRequest request = IntegrityTokenRequest.builder()
                .setCloudProjectNumber(CLOUD_PROJECT_NUMBER)
                .setNonce(nonce)
                .build();
                
            integrityManager.requestIntegrityToken(request)
                .addOnSuccessListener(
                    this,
                    new OnSuccessListener<IntegrityTokenResponse>() {
                        @Override
                        public void onSuccess(IntegrityTokenResponse response) {
                            String token = response.token();
                            
                            // More detailed logging for debugging
                            Log.d(TAG, "======= PLAY INTEGRITY SUCCESS (GOOGLE) =======");
                            Log.d(TAG, "Integrity token received: " + 
                                (token.length() > 10 ? token.substring(0, 10) + "..." : "empty"));
                            Log.d(TAG, "Token length: " + token.length());
                            Log.d(TAG, "========================================");
                            
                            // Show a visual indicator of success
                            Toast.makeText(MainActivity.this, "Security check passed!", Toast.LENGTH_SHORT).show();
                            
                            // Hide progress dialog
                            hideSecurityCheckDialog();
                            
                            // Show success animation
                            showSecuritySuccess();
                            
                            // Proceed with Google sign-in after integrity check
                            handleGoogleSignIn();
                        }
                    })
                .addOnFailureListener(
                    this,
                    new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // More detailed logging for debugging
                            Log.e(TAG, "======= PLAY INTEGRITY FAILED (GOOGLE) =======");
                            Log.e(TAG, "Integrity verification failed: " + e.getMessage());
                            
                            // Log more specific error details if possible
                            if (e instanceof ApiException) {
                                ApiException apiException = (ApiException) e;
                                Log.e(TAG, "Error code: " + apiException.getStatusCode());
                                Log.e(TAG, "Error details: " + CommonStatusCodes.getStatusCodeString(apiException.getStatusCode()));
                            }
                            
                            Log.e(TAG, "======================================");
                            
                            // Show a visual indicator of failure
                            Toast.makeText(MainActivity.this, "Security check failed! Login blocked.", Toast.LENGTH_LONG).show();
                            
                            // Hide progress dialog
                            hideSecurityCheckDialog();
                            
                            // Show security error dialog
                            showSecurityError(e);
                            
                            // SECURITY BLOCK: Don't allow login on failure
                            // handleGoogleSignIn(); -- REMOVED
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "EXCEPTION CREATING INTEGRITY REQUEST (GOOGLE): " + e.getMessage(), e);
            Toast.makeText(this, "Error setting up security check: " + e.getMessage(), Toast.LENGTH_LONG).show();
            hideSecurityCheckDialog();
        }
    }

    private void handleEmailLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

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

    // Show a styled security check dialog
    private void showSecurityCheckDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.dialog_security_check, null);
        builder.setView(view);
        builder.setCancelable(false);
        progressDialog = builder.create();
        if (!isFinishing()) {
            progressDialog.show();
        }
    }
    
    // Hide the security check dialog
    private void hideSecurityCheckDialog() {
        if (progressDialog != null && progressDialog.isShowing() && !isFinishing()) {
            progressDialog.dismiss();
        }
    }
    
    // Show success animation briefly
    private void showSecuritySuccess() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.dialog_security_success, null);
        builder.setView(view);
        builder.setCancelable(false);
        AlertDialog successDialog = builder.create();
        if (!isFinishing()) {
            successDialog.show();
            
            // Dismiss after a short delay
            new android.os.Handler().postDelayed(() -> {
                if (successDialog.isShowing() && !isFinishing()) {
                    successDialog.dismiss();
                }
            }, 1500);
        }
    }
    
    // Show security error dialog
    private void showSecurityError(Exception e) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.dialog_security_error, null);
        builder.setView(view);
        
        // Get error message TextView
        android.widget.TextView errorMessageView = view.findViewById(R.id.errorMessageTextView);
        String errorMessage = "Security verification failed. For your protection, login has been blocked.";
        
        // Get specific error details if possible
        if (e instanceof ApiException) {
            ApiException apiException = (ApiException) e;
            errorMessage += "\n\nError: " + CommonStatusCodes.getStatusCodeString(apiException.getStatusCode());
        } else if (e.getMessage() != null) {
            errorMessage += "\n\nError: " + e.getMessage();
        }
        
        // Set error message
        errorMessageView.setText(errorMessage);
        
        // Set up dismiss button
        android.widget.Button dismissButton = view.findViewById(R.id.dismissButton);
        
        // Create and show the dialog
        AlertDialog errorDialog = builder.create();
        
        // Set button click listener - just dismiss the dialog
        dismissButton.setOnClickListener(v -> {
            if (errorDialog.isShowing() && !isFinishing()) {
                errorDialog.dismiss();
            }
        });
        
        if (!isFinishing()) {
            errorDialog.show();
        }
    }
}