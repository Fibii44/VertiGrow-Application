package com.example.finalproject_vertigrow.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.finalproject_vertigrow.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class PasswordChangeActivity extends AppCompatActivity {

    private EditText newPasswordEditText;
    private EditText confirmPasswordEditText;
    private Button changePasswordButton;
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userName;
    private String userRole;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_change);
        
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        // Get user data from intent
        userName = getIntent().getStringExtra("userName");
        userRole = getIntent().getStringExtra("userRole");
        
        // Initialize views
        newPasswordEditText = findViewById(R.id.new_password_edit_text);
        confirmPasswordEditText = findViewById(R.id.confirm_password_edit_text);
        changePasswordButton = findViewById(R.id.change_password_button);
        
        // Set up button click listener
        changePasswordButton.setOnClickListener(v -> validateAndChangePassword());
    }
    
    private void validateAndChangePassword() {
        String newPassword = newPasswordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        
        // Validate passwords
        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (newPassword.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Change password
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.updatePassword(newPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Update password change flag in Firestore
                        updatePasswordChangedFlag(user.getUid());
                    } else {
                        Toast.makeText(PasswordChangeActivity.this, 
                                "Failed to change password: " + task.getException().getMessage(), 
                                Toast.LENGTH_SHORT).show();
                    }
                });
        }
    }
    
    private void updatePasswordChangedFlag(String userId) {
        db.collection("users").document(userId)
            .update("passwordChanged", true)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(PasswordChangeActivity.this, 
                        "Password changed successfully", 
                        Toast.LENGTH_SHORT).show();
                
                // Navigate to appropriate screen based on user role
                navigateToUserScreen();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(PasswordChangeActivity.this, 
                        "Error updating user data: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            });
    }
    
    private void navigateToUserScreen() {
        Intent intent;
        if ("admin".equals(userRole)) {
            intent = new Intent(this, AdminActivity.class);
        } else {
            intent = new Intent(this, UserActivity.class);
        }
        intent.putExtra("userName", userName);
        intent.putExtra("userRole", userRole);
        startActivity(intent);
        finish();
    }
    
    @Override
    public void onBackPressed() {
        // Prevent user from going back without changing password
        Toast.makeText(this, "Please change your password before continuing", Toast.LENGTH_SHORT).show();
    }
} 