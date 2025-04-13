package com.example.finalproject_vertigrow.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.finalproject_vertigrow.R;
import com.example.finalproject_vertigrow.models.User;
import com.google.android.gms.auth.api.signin.*;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.FirebaseFirestore;


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
    private Button loginButton, googleSignInButton, signUpButton;

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
        signUpButton = findViewById(R.id.signUpButton);
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
        signUpButton.setOnClickListener(v -> navigateToSignUp());
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
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            checkAndCreateGoogleUser(firebaseUser);
                        }
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkAndCreateGoogleUser(FirebaseUser firebaseUser) {
        db.collection("users").document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        // Create new user using the User model
                        User newUser = new User(
                                firebaseUser.getDisplayName(),
                                firebaseUser.getEmail(),
                                "user",  // Default role
                                "google" // Auth provider
                        );

                        // Save to Firestore using the model's toMap method
                        db.collection("users").document(firebaseUser.getUid())
                                .set(newUser.toMap())
                                .addOnSuccessListener(aVoid ->
                                        navigateToUserScreen(newUser.getName(), newUser.getRole()))
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error creating user", e);
                                    Toast.makeText(this, "Error creating user profile",
                                            Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        String role = document.getString("role");
                        String name = document.getString("name");
                        navigateToUserScreen(name, role);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking user", e);
                    Toast.makeText(this, "Error checking user data",
                            Toast.LENGTH_SHORT).show();
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
                        navigateToUserScreen(name, role);
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

    // Navigation Methods
    private void navigateToSignUp() {
        startActivity(new Intent(this, SignUpActivity.class));
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