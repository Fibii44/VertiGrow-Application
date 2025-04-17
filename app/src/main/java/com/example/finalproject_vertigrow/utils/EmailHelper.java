package com.example.finalproject_vertigrow.utils;

import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import java.util.List;
import java.net.URLEncoder;

/**
 * Utility class for handling email operations
 */
public class EmailHelper {
    private static final String TAG = "EmailHelper";
    private static final String GMAIL_PACKAGE = "com.google.android.gm";
    // Gmail compose activity
    private static final String GMAIL_ACTIVITY = "com.google.android.gm.ComposeActivityGmail";
    
    /**
     * Sends credentials using the device's email app (Gmail preferred)
     * Admin just needs to click "Send" button
     * 
     * @param context The context for displaying toast messages
     * @param receiverEmail Recipient email address
     * @param userName User's name
     * @param password The generated password
     * @return true if the process was initiated, false otherwise
     */
    public static boolean sendCredentialEmail(Context context, String receiverEmail, String userName, String password) {
        // First try to open Gmail directly
        boolean sent = tryGmail(context, receiverEmail, userName, password);
        
        // If Gmail is not available, try any other email app
        if (!sent) {
            sent = tryGenericEmailApp(context, receiverEmail, userName, password);
        }
        
        return sent;
    }
    
    /**
     * Attempts to send email via Gmail app specifically by targeting Gmail's compose activity
     */
    private static boolean tryGmail(Context context, String receiverEmail, String userName, String password) {
        try {
            // Target the Gmail compose activity directly
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            
            // Set the component to specifically target Gmail's compose activity
            ComponentName componentName = new ComponentName(GMAIL_PACKAGE, GMAIL_ACTIVITY);
            intent.setComponent(componentName);
            
            // Set recipients
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{receiverEmail});
            
            // Set subject
            intent.putExtra(Intent.EXTRA_SUBJECT, "VertiGrow App - Your Account Details");
            
            // Create email body
            String emailBody = "Dear " + userName + ",\n\n" +
                    "Your VertiGrow account has been created. Please use the following credentials to log in:\n\n" +
                    "Email: " + receiverEmail + "\n" +
                    "Temporary Password: " + password + "\n\n" +
                    "We recommend changing your password after you log in for the first time.\n\n" +
                    "Thank you,\n" +
                    "The VertiGrow Team";
            
            // Set the body
            intent.putExtra(Intent.EXTRA_TEXT, emailBody);
            
            // Check if Gmail is available
            PackageManager packageManager = context.getPackageManager();
            List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
            boolean isGmailInstalled = !activities.isEmpty();
            
            if (isGmailInstalled) {
                // Set flag to open in new task
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                
                // Show instruction
                Toast.makeText(context, 
                        "Please send the email with credentials", 
                        Toast.LENGTH_LONG).show();
                
                return true;
            } else {
                Log.e(TAG, "Gmail app is not installed or not accessible");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to open Gmail app: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Method to send email using any available email app
     */
    private static boolean tryGenericEmailApp(Context context, String receiverEmail, String userName, String password) {
        try {
            // Use ACTION_SEND for any email app
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("text/plain");
            
            // Set recipients
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{receiverEmail});
            
            // Set subject
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "VertiGrow App - Your Account Details");
            
            // Create email body
            String emailBody = "Dear " + userName + ",\n\n" +
                    "Your VertiGrow account has been created. Please use the following credentials to log in:\n\n" +
                    "Email: " + receiverEmail + "\n" +
                    "Temporary Password: " + password + "\n\n" +
                    "We recommend changing your password after you log in for the first time.\n\n" +
                    "Thank you,\n" +
                    "The VertiGrow Team";
            
            // Set the body
            emailIntent.putExtra(Intent.EXTRA_TEXT, emailBody);
            
            // Start the email app with chooser
            context.startActivity(Intent.createChooser(emailIntent, "Send credentials via email"));
            
            // Show instruction
            Toast.makeText(context, 
                    "Please select your email app and send the credentials", 
                    Toast.LENGTH_LONG).show();
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to open email app: " + e.getMessage(), e);
            Toast.makeText(context, 
                    "Could not open any email app: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            return false;
        }
    }
} 