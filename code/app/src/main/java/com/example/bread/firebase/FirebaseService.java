package com.example.bread.firebase;

import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Service class for handling Firebase Firestore database operations.
 */
public class FirebaseService {
    private static boolean IS_INITIALIZED = false;
    private FirebaseFirestore db;

    /**
     * Default constructor for FirebaseService.
     */
    public FirebaseService() {
        if (!IS_INITIALIZED) {
            this.db = FirebaseFirestore.getInstance();
            IS_INITIALIZED = true;
        }
    }

    /**
     * Returns the Firestore database instance.
     *
     * @return the Firestore database instance
     */
    public synchronized FirebaseFirestore getDb() {
        if (db == null) {
            this.db = FirebaseFirestore.getInstance();
        }
        return db;
    }
}
