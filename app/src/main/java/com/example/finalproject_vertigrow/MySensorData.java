package com.example.finalproject_vertigrow;

public class MySensorData {

    // Define the field(s) you want to store in Firestore
    private String message;

    // No-argument constructor required for Firestore
    public MySensorData() {
        // Firestore uses this to create the object
    }

    // Constructor to initialize the object with data
    public MySensorData(String message) {
        this.message = message;
    }

    // Getter for the message field
    public String getMessage() {
        return message;
    }

    // Setter for the message field
    public void setMessage(String message) {
        this.message = message;
    }
}
