package com.example.finalproject_vertigrow.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.finalproject_vertigrow.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {
    private TextView userName;
    private TextView userEmail;
    private LinearLayout editProfileOption;
    private LinearLayout changePasswordOption;

    public ProfileFragment() {
        // Required empty public constructor
    }

    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize views
        userName = view.findViewById(R.id.text_user_name);
        userEmail = view.findViewById(R.id.text_user_email);
        editProfileOption = view.findViewById(R.id.edit_profile_option);
        changePasswordOption = view.findViewById(R.id.change_password_option);
        
        // Load user data
        loadUserProfile();
        
        // Set click listeners
        editProfileOption.setOnClickListener(v -> {
            // TODO: Implement edit profile functionality
            Toast.makeText(requireContext(), "Edit profile feature coming soon", Toast.LENGTH_SHORT).show();
        });
        
        changePasswordOption.setOnClickListener(v -> {
            // TODO: Implement change password functionality
            Toast.makeText(requireContext(), "Change password feature coming soon", Toast.LENGTH_SHORT).show();
        });
    }
    
    /**
     * Load user profile information from Firebase
     */
    private void loadUserProfile() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        if (currentUser != null) {
            // Set user name (display name or email if display name is null)
            String displayName = currentUser.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                userName.setText(displayName);
            } else {
                // Use email as name if display name is not available
                userName.setText(currentUser.getEmail());
            }
            
            // Set user email
            userEmail.setText(currentUser.getEmail());
            
            // TODO: Load additional user data from Firestore if needed
        } else {
            // User is not logged in (this should not happen as the profile is only accessible when logged in)
            Toast.makeText(requireContext(), "Error loading profile", Toast.LENGTH_SHORT).show();
        }
    }
} 