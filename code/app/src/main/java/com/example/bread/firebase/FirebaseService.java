package com.example.bread.firebase;

import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseService {
    private static boolean IS_INITIALIZED = false;
    private FirebaseFirestore db;

    public FirebaseService() {
        if (!IS_INITIALIZED) {
            db = FirebaseFirestore.getInstance();
            IS_INITIALIZED = true;
        }
    }

    public FirebaseFirestore getDb() {
        if (db == null) {
            db = FirebaseFirestore.getInstance();
        }
        return db;
    }
}
