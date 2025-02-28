package com.example.bread.firebase;

import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseService {
    private static boolean IS_INITIALIZED = false;
    private FirebaseFirestore db;

    public FirebaseService() {
        if (!IS_INITIALIZED) {
            this.db = FirebaseFirestore.getInstance();
            IS_INITIALIZED = true;
        }
    }

    public synchronized FirebaseFirestore getDb() {
        if (db == null) {
            this.db = FirebaseFirestore.getInstance();
        }
        return db;
    }
}
