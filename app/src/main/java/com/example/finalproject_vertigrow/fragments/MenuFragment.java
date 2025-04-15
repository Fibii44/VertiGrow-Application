package com.example.finalproject_vertigrow.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.finalproject_vertigrow.R;
import com.example.finalproject_vertigrow.activities.MainActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;

public class MenuFragment extends Fragment {
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private Button logoutButton;
    private LinearLayout profileOption;
    private LinearLayout aboutOption;
    private LinearLayout helpOption;

    public MenuFragment() {
        // Required empty public constructor
    }

    public static MenuFragment newInstance() {
        return new MenuFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_menu, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso);

        // Initialize views
        logoutButton = view.findViewById(R.id.logoutButton);
        profileOption = view.findViewById(R.id.profile_option);
        aboutOption = view.findViewById(R.id.about_option);
        helpOption = view.findViewById(R.id.help_option);

        // Set click listeners
        logoutButton.setOnClickListener(v -> performSignOut());
        
        // About Us click listener
        aboutOption.setOnClickListener(v -> {
             AboutUsFragment aboutUsFragment = AboutUsFragment.newInstance();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, aboutUsFragment)
                    .addToBackStack(null)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
        });
        
        // Profile click listener
        profileOption.setOnClickListener(v -> {
            ProfileFragment profileFragment = ProfileFragment.newInstance();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, profileFragment)
                    .addToBackStack(null)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
        });
        
        // Help click listener
        helpOption.setOnClickListener(v -> {
            HelpFragment helpFragment = HelpFragment.newInstance();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, helpFragment)
                    .addToBackStack(null)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
        });
    }
    
    private void performSignOut() {
        // Sign out from Firebase
        mAuth.signOut();

        // Sign out from Google
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(requireActivity(), task -> {
                    // Revoke access to make sure user has to pick account next time
                    mGoogleSignInClient.revokeAccess()
                            .addOnCompleteListener(revokeTask -> {
                                Toast.makeText(requireContext(),
                                        "Logged out successfully", Toast.LENGTH_SHORT).show();

                                // Redirect to login screen
                                Intent intent = new Intent(requireContext(), MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                requireActivity().finish();
                            });
                });
    }
} 