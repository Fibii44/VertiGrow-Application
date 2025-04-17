package com.example.finalproject_vertigrow.utils;

import java.security.SecureRandom;

/**
 * Utility class for generating secure random passwords
 */
public class PasswordGenerator {
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%^&*()_-+=<>?";
    private static final SecureRandom random = new SecureRandom();

    /**
     * Generates a secure random password with the specified length
     * @param length Length of the password to generate
     * @return A string containing the generated password
     */
    public static String generatePassword(int length) {
        if (length < 8) {
            length = 8; // Minimum password length for security
        }
        
        // Define the characters to use in the password
        String allChars = LOWERCASE + UPPERCASE + DIGITS + SPECIAL;
        
        StringBuilder password = new StringBuilder();
        
        // Ensure at least one character from each set
        password.append(LOWERCASE.charAt(random.nextInt(LOWERCASE.length())));
        password.append(UPPERCASE.charAt(random.nextInt(UPPERCASE.length())));
        password.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
        password.append(SPECIAL.charAt(random.nextInt(SPECIAL.length())));
        
        // Fill the rest of the password with random characters
        for (int i = 4; i < length; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }
        
        // Shuffle the password characters
        char[] passwordArray = password.toString().toCharArray();
        for (int i = 0; i < passwordArray.length; i++) {
            int j = random.nextInt(passwordArray.length);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }
        
        return new String(passwordArray);
    }
} 