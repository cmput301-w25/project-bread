package com.example.bread.firebase;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.LocalCacheSettings;
import com.google.firebase.firestore.PersistentCacheIndexManager;
import com.google.firebase.firestore.PersistentCacheSettings;

/**
 *
 * Role / Purpose:
 * - Represents a service class for handling Firebase Firestore database operations.
 * - Initializes and provides access to Firebase Firestore.
 * - Enables offline persistent caching and optional auto-indexing.
 * - Manages network connectivity checks for Firestore operations.
 *
 * Design Patterns:
 * - Singleton: Ensures a single instance of FirebaseFirestore.
 *
 * Outstanding Issues:
 * - Error handling and logging for Firestore operations could be made more comprehensive.
 *
 */
public class FirebaseService {
    private static final String TAG = "FirebaseService";
    private static boolean IS_INITIALIZED = false;
    private FirebaseFirestore db;
    private static Context applicationContext;

    /**
     * Default constructor for FirebaseService.
     * Initializes Firestore with persistent disk cache (offline support)
     * and optional auto-indexing (if available).
     */
    public FirebaseService() {
        if (!IS_INITIALIZED) {
            this.db = FirebaseFirestore.getInstance();
            LocalCacheSettings persistentCache = PersistentCacheSettings.newBuilder()
                    .build();
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(persistentCache)
                    .build();

            db.setFirestoreSettings(settings);

            //    This speeds up queries by automatically creating local indexes in the cache.
            PersistentCacheIndexManager indexManager = db.getPersistentCacheIndexManager();
            if (indexManager != null) {
                indexManager.enableIndexAutoCreation();
            }

            IS_INITIALIZED = true;
        }
    }

    /**
     * Initialize the application context for network checks
     * @param context Application context
     */
    public static void initializeContext(Context context) {
        if (applicationContext == null) {
            applicationContext = context.getApplicationContext();
            Log.d(TAG, "Context initialized for network connectivity checks");
        }
    }

    /**
     * Check if the device currently has internet connectivity
     *
     * @return true if connected, false otherwise
     */
    public static boolean isNetworkConnected() {
        if (applicationContext == null) {
            Log.w(TAG, "Context not initialized. Call initializeContext() first.");
            return true; // Default to true to avoid potential issues
        }

        ConnectivityManager connectivityManager =
                (ConnectivityManager) applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkCapabilities capabilities = connectivityManager
                    .getNetworkCapabilities(connectivityManager.getActiveNetwork());

            return capabilities != null &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }

        return false;
    }

    /**
     * Constructor for FirebaseService that takes a Firestore database instance.
     * @param db the Firestore database instance
     */
    public FirebaseService(FirebaseFirestore db) {
        this.db = db;
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