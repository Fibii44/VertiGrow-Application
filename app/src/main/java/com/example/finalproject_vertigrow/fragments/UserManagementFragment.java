package com.example.finalproject_vertigrow.fragments;


import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject_vertigrow.R;
import com.example.finalproject_vertigrow.models.User;
import com.example.finalproject_vertigrow.utils.EmailHelper;
import com.example.finalproject_vertigrow.utils.PasswordGenerator;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserManagementFragment extends Fragment {

    private RecyclerView recyclerUsers;
    private EditText editSearchUser;
    private Button buttonSearch;
    private ProgressBar progressBar;
    private TextView textNoUsers;
    private View emptyStateContainer;
    private FirebaseFirestore db;
    private UserAdapter userAdapter;
    private List<User> userList = new ArrayList<>();
    private List<User> filteredUserList = new ArrayList<>();
    
    // Array of color resources for avatar backgrounds
    private static final int[] AVATAR_COLORS = new int[]{
        android.R.color.holo_blue_light,
        android.R.color.holo_green_light,
        android.R.color.holo_orange_light,
        android.R.color.holo_purple,
        android.R.color.holo_red_light,
        R.color.purple_500,
        R.color.teal_700
    };
    
    public UserManagementFragment() {
        // Required empty public constructor
    }

    public static UserManagementFragment newInstance() {
        return new UserManagementFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        try {
            // Initialize Firestore
            db = FirebaseFirestore.getInstance();
            
            // Initialize views
            recyclerUsers = view.findViewById(R.id.recycler_users);
            editSearchUser = view.findViewById(R.id.edit_search_user);
            progressBar = view.findViewById(R.id.progress_bar);
            textNoUsers = view.findViewById(R.id.text_no_users);
            emptyStateContainer = view.findViewById(R.id.empty_state_container);
            
            // Set up FAB
            View fabAddUser = view.findViewById(R.id.fab_add_user);
            if (fabAddUser != null) {
                fabAddUser.setOnClickListener(v -> showAddUserDialog());
            }
            
            // Set up RecyclerView
            recyclerUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
            userAdapter = new UserAdapter(filteredUserList);
            recyclerUsers.setAdapter(userAdapter);
            
            // Set up search functionality
            setupSearch();
            
            // Load all users
            loadUsers();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error initializing view: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void setupSearch() {
        // Add text change listener to filter as user types
        editSearchUser.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Filter the list with each character change
                filterUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });
    }
    
    private void filterUsers(String query) {
        filteredUserList.clear();
        
        if (query.isEmpty()) {
            // If query is empty, show all users
            filteredUserList.addAll(userList);
        } else {
            // Filter by name or email
            query = query.toLowerCase();
            for (User user : userList) {
                if (user.getName().toLowerCase().contains(query) || 
                    user.getEmail().toLowerCase().contains(query)) {
                    filteredUserList.add(user);
                }
            }
        }
        
        userAdapter.notifyDataSetChanged();
        updateEmptyView();
    }
    
    private void updateEmptyView() {
        if (filteredUserList.isEmpty()) {
            emptyStateContainer.setVisibility(View.VISIBLE);
            recyclerUsers.setVisibility(View.GONE);
        } else {
            emptyStateContainer.setVisibility(View.GONE);
            recyclerUsers.setVisibility(View.VISIBLE);
        }
    }
    
    private void loadUsers() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerUsers.setVisibility(View.GONE);
        emptyStateContainer.setVisibility(View.GONE);
        
        db.collection("users")
            .whereEqualTo("role", "user")  // Only get users with role "user"
            .get()
            .addOnCompleteListener(task -> {
                progressBar.setVisibility(View.GONE);
                
                if (task.isSuccessful()) {
                    userList.clear();
                    
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        try {
                            User user = new User();
                            user.setId(document.getId());
                            user.setName(document.getString("name"));
                            user.setEmail(document.getString("email"));
                            user.setRole(document.getString("role"));
                            
                            // Get the createdAt timestamp
                            if (document.contains("createdAt")) {
                                user.setCreatedAt(document.getLong("createdAt"));
                            }
                            
                            userList.add(user);
                        } catch (Exception e) {
                            // Log error but continue with other users
                            Toast.makeText(requireContext(), 
                                    "Error parsing user data: " + e.getMessage(), 
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                    
                    // Initialize filtered list with all users
                    filteredUserList.clear();
                    filteredUserList.addAll(userList);
                    userAdapter.notifyDataSetChanged();
                    
                    // Update empty view
                    updateEmptyView();
                } else {
                    Toast.makeText(requireContext(), 
                            "Error loading users: " + task.getException().getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    textNoUsers.setText("Error loading users");
                    textNoUsers.setVisibility(View.VISIBLE);
                }
            });
    }
    
    private void viewUserFarms(User user) {
        // Navigate to the UserFarmsFragment to display farms for this user
        UserFarmsFragment userFarmsFragment = UserFarmsFragment.newInstance(user.getId(), user.getName());
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, userFarmsFragment)
                .addToBackStack(null)
                .commit();
    }
    
    private void showUserOptionsMenu(View view, User user) {
        try {
            PopupMenu popup = new PopupMenu(requireContext(), view);
            popup.getMenuInflater().inflate(R.menu.menu_user_options, popup.getMenu());
            
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_view_farms) {
                    viewUserFarms(user);
                    return true;
                } else if (itemId == R.id.action_edit_user) {
                    showEditUserDialog(user);
                    return true;
                } else if (itemId == R.id.action_delete_user) {
                    showDeleteUserDialog(user);
                    return true;
                }
                return false;
            });
            
            popup.show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), 
                "Could not show menu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showEditUserDialog(User user) {
        // Create custom dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_user, null);
        builder.setView(dialogView);
        
        // Find views
        EditText editName = dialogView.findViewById(R.id.edit_name);
        EditText editEmail = dialogView.findViewById(R.id.edit_email);
        EditText editPassword = dialogView.findViewById(R.id.edit_password);
        Button buttonCancel = dialogView.findViewById(R.id.button_cancel);
        Button buttonSave = dialogView.findViewById(R.id.button_save);
        TextView title = dialogView.findViewById(R.id.dialog_title);
        View passwordInfoLayout = dialogView.findViewById(R.id.password_info_layout);
        
        // Set up the dialog for editing
        if (title != null) {
            title.setText("Edit User");
        }
        buttonSave.setText("Update");
        
        // Pre-fill with user data
        editName.setText(user.getName());
        editEmail.setText(user.getEmail());
        editEmail.setEnabled(false); // Email cannot be changed
        
        // Hide password field and info layout since we can't edit passwords
        if (passwordInfoLayout != null) {
            passwordInfoLayout.setVisibility(View.GONE);
        }
        
        TextInputLayout passwordLayout = dialogView.findViewById(R.id.password_input_layout);
        if (passwordLayout != null) {
            passwordLayout.setVisibility(View.GONE);
        } else {
            // If we can't find the layout, just hide the EditText
            editPassword.setVisibility(View.GONE);
        }
        
        // Create dialog without setting buttons (we use our custom ones)
        AlertDialog dialog = builder.create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.show();
        
        // Set up button click listeners
        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        
        buttonSave.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            
            // Validate inputs
            if (name.isEmpty()) {
                editName.setError("Name is required");
                return;
            }
            
            // Proceed with updating the user (only name)
            updateUser(user, name);
            dialog.dismiss();
        });
    }
    
    private void updateUser(User user, String name) {
        progressBar.setVisibility(View.VISIBLE);
        
        // Update name in Firestore
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        
        db.collection("users").document(user.getId())
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                progressBar.setVisibility(View.GONE);
                
                // Update user in lists
                user.setName(name);
                userAdapter.notifyDataSetChanged();
                
                Toast.makeText(requireContext(), "User updated successfully", Toast.LENGTH_SHORT).show();
                
                // Log the action
                logUserUpdate(user);
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), 
                        "Failed to update user: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            });
    }
    
    private void logUserUpdate(User user) {
        // Create log entry in Firestore
        db.collection("logs")
            .add(new com.example.finalproject_vertigrow.models.LogEntry(
                null, // id will be set by Firebase
                null, // admin user ID would be here
                null, // No farm ID for this action
                "Updated user " + user.getEmail(),
                System.currentTimeMillis(),
                "USER_UPDATE"
            ));
    }
    
    private void showDeleteUserDialog(User user) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete " + user.getName() + "? This action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> deleteUser(user))
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void deleteUser(User user) {
        progressBar.setVisibility(View.VISIBLE);
        
        db.collection("users").document(user.getId())
            .delete()
            .addOnSuccessListener(aVoid -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), 
                        "User deleted successfully", 
                        Toast.LENGTH_SHORT).show();
                
                // Update lists
                userList.remove(user);
                filteredUserList.remove(user);
                userAdapter.notifyDataSetChanged();
                updateEmptyView();
                
                // Log the action
                logUserDeletion(user);
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), 
                        "Failed to delete user: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            });
    }
    
    private void logUserDeletion(User user) {
        // Create log entry in Firestore
        db.collection("logs")
            .add(new com.example.finalproject_vertigrow.models.LogEntry(
                null, // id will be set by Firebase
                null, // admin user ID would be here
                null, // No farm ID for this action
                "Deleted user " + user.getEmail(),
                System.currentTimeMillis(),
                "USER_DELETE"
            ));
    }
    
    private void showAddUserDialog() {
        // Create custom dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_user, null);
        builder.setView(dialogView);
        
        // Find views
        EditText editName = dialogView.findViewById(R.id.edit_name);
        EditText editEmail = dialogView.findViewById(R.id.edit_email);
        EditText editPassword = dialogView.findViewById(R.id.edit_password);
        Button buttonCancel = dialogView.findViewById(R.id.button_cancel);
        Button buttonSave = dialogView.findViewById(R.id.button_save);
        TextView title = dialogView.findViewById(R.id.dialog_title);
        View passwordInfoLayout = dialogView.findViewById(R.id.password_info_layout);
        
        // Make sure title is set correctly (though it's already the default in XML)
        if (title != null) {
            title.setText("Add New User");
        }
        
        // Hide password field since we will auto-generate it
        TextInputLayout passwordLayout = dialogView.findViewById(R.id.password_input_layout);
        if (passwordLayout != null) {
            passwordLayout.setVisibility(View.GONE);
        } else {
            // If we can't find the layout, just hide the EditText
            editPassword.setVisibility(View.GONE);
        }
        
        // Create dialog without setting buttons (we use our custom ones)
        AlertDialog dialog = builder.create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.show();
        
        // Set up button click listeners
        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        
        buttonSave.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String email = editEmail.getText().toString().trim();
            
            // Validate inputs
            if (name.isEmpty()) {
                editName.setError("Name is required");
                return;
            }
            
            if (email.isEmpty()) {
                editEmail.setError("Email is required");
                return;
            }
            
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                editEmail.setError("Invalid email format");
                return;
            }
            
            // Generate a secure random password
            String generatedPassword = PasswordGenerator.generatePassword(10);
            
            // Proceed with adding the user with the generated password
            addNewUser(name, email, generatedPassword);
            dialog.dismiss();
        });
    }
    
    private void addNewUser(String name, String email, String password) {
        progressBar.setVisibility(View.VISIBLE);
        
        // First check if email already exists
        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    if (!task.getResult().isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "Email already exists", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Create user in Firebase Authentication
                    FirebaseAuth mAuth = FirebaseAuth.getInstance();
                    mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener(authResult -> {
                            String userId = authResult.getUser().getUid();
                            
                            // Create user in Firestore
                            User newUser = new User();
                            newUser.setName(name);
                            newUser.setEmail(email);
                            newUser.setRole("user"); // Default role
                            newUser.setCreatedAt(System.currentTimeMillis());
                            newUser.setId(userId);
                            
                            // Convert to Map for Firestore
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("name", newUser.getName());
                            userData.put("email", newUser.getEmail());
                            userData.put("role", newUser.getRole());
                            userData.put("createdAt", newUser.getCreatedAt());
                            userData.put("passwordChanged", false); // Require user to change password on first login
                            userData.put("googleUid", null); // Mark that this is a manual account
                            
                            // Add to Firestore
                            db.collection("users").document(userId)
                                .set(userData)
                                .addOnSuccessListener(aVoid -> {
                                    progressBar.setVisibility(View.GONE);
                                    
                                    // Add to lists
                                    userList.add(newUser);
                                    filteredUserList.add(newUser);
                                    userAdapter.notifyDataSetChanged();
                                    updateEmptyView();
                                    
                                    // Send email with login credentials
                                    boolean emailInitiated = sendWelcomeEmail(email, name, password);
                                    
                                    String message = "User added successfully";
                                    if (emailInitiated) {
                                        message += " and welcome email is being sent";
                                    } else {
                                        message += " but failed to send welcome email";
                                    }
                                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                                    
                                    // Log the action
                                    logUserAddition(newUser);
                                })
                                .addOnFailureListener(e -> {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(requireContext(), 
                                            "Failed to add user data: " + e.getMessage(), 
                                            Toast.LENGTH_LONG).show();
                                });
                        })
                        .addOnFailureListener(e -> {
                            progressBar.setVisibility(View.GONE);
                            
                            // Get more detailed Firebase Auth error message
                            String errorMessage = "Failed to create account";
                            
                            if (e.getMessage() != null) {
                                // Log the detailed error for debugging
                                Log.e("UserManagement", "Firebase Auth Error: " + e.getMessage());
                                
                                if (e.getMessage().contains("email address is already in use")) {
                                    errorMessage = "Email address is already in use by another account";
                                } else if (e.getMessage().contains("password is invalid")) {
                                    errorMessage = "Password must be at least 6 characters";
                                } else if (e.getMessage().contains("network error")) {
                                    errorMessage = "Network error - check your internet connection";
                                } else {
                                    // Include the original error for other cases
                                    errorMessage = "Authentication error: " + e.getMessage();
                                }
                            }
                            
                            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
                        });
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), 
                            "Error checking email: " + task.getException().getMessage(), 
                            Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    private boolean sendWelcomeEmail(String email, String name, String password) {
        // Use the EmailHelper utility class to send credentials
        return EmailHelper.sendCredentialEmail(requireContext(), email, name, password);
    }
    
    private void logUserAddition(User user) {
        // Create log entry in Firestore
        db.collection("logs")
            .add(new com.example.finalproject_vertigrow.models.LogEntry(
                null, // id will be set by Firebase
                null, // admin user ID would be here
                null, // No farm ID for this action
                "Added new user " + user.getEmail(),
                System.currentTimeMillis(),
                "USER_ADD"
            ));
    }
    
    // Adapter for the users RecyclerView
    private class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
        
        private List<User> users;
        
        public UserAdapter(List<User> users) {
            this.users = users;
        }
        
        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_user, parent, false);
            return new UserViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            User user = users.get(position);
            
            // Set user name and get initial for avatar
            holder.textName.setText(user.getName());
            if (user.getName() != null && !user.getName().isEmpty()) {
                String initial = user.getName().substring(0, 1).toUpperCase();
                holder.textInitial.setText(initial);
                // Set random background color based on name
                int colorPosition = Math.abs(user.getName().hashCode()) % AVATAR_COLORS.length;
                holder.textInitial.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(holder.itemView.getContext(), AVATAR_COLORS[colorPosition])));
            } else {
                holder.textInitial.setText("?");
            }
            
            holder.textEmail.setText(user.getEmail());
            
            // Set join date text if available
            if (user.getCreatedAt() != 0) {
                holder.textJoinDate.setVisibility(View.VISIBLE);
                holder.textJoinDate.setText("Joined: " + formatDate(user.getCreatedAt()));
            } else {
                holder.textJoinDate.setVisibility(View.GONE);
            }
            
            // Set role with appropriate styling
            String role = user.getRole();
            holder.textRole.setText(role);
            
            // Setup dropdown menu
            holder.buttonMoreOptions.setOnClickListener(v -> showUserOptionsMenu(v, user));
        }
        
        @Override
        public int getItemCount() {
            return users.size();
        }
        
        // Helper method to format timestamp to readable date
        private String formatDate(long timestamp) {
            try {
                java.util.Date date = new java.util.Date(timestamp);
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault());
                return sdf.format(date);
            } catch (Exception e) {
                return "Unknown date";
            }
        }
        
        class UserViewHolder extends RecyclerView.ViewHolder {
            TextView textName, textEmail, textJoinDate, textInitial, textRole;
            ImageButton buttonMoreOptions;
            
            public UserViewHolder(@NonNull View itemView) {
                super(itemView);
                textName = itemView.findViewById(R.id.text_user_name);
                textEmail = itemView.findViewById(R.id.text_user_email);
                textJoinDate = itemView.findViewById(R.id.text_join_date);
                textInitial = itemView.findViewById(R.id.text_user_initial);
                textRole = itemView.findViewById(R.id.text_user_role);
                buttonMoreOptions = itemView.findViewById(R.id.button_more_options);
            }
        }
    }
} 