package com.example.bread.fragment;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bread.R;
import com.example.bread.controller.FollowRequestAdapter;
import com.example.bread.model.FollowRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ProfileFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ProfileFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private ListView listViewRequests;
    private FollowRequestAdapter adapter;
    private List<FollowRequest> followRequests = new ArrayList<>();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private TextView followersCountText, followingCountText;

    public ProfileFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ProfileFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ProfileFragment newInstance(String param1, String param2) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        followersCountText = view.findViewById(R.id.followersCountText);
        followingCountText = view.findViewById(R.id.followingCountText);
        listViewRequests = view.findViewById(R.id.listViewFollowRequests);
        adapter = new FollowRequestAdapter(getContext(), followRequests);
        listViewRequests.setAdapter(adapter);

        fetchFollowRequests(); // Fetch and populate the list
        setupRealtimeUpdates();
        return view;
    }

    private void fetchFollowRequests() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();  // Fetching current user ID from Firebase Authentication

        db.collection("followRequests")
                .whereEqualTo("receiverId", currentUserId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(getContext(), "Error listening to follow requests: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    followRequests.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        FollowRequest request = doc.toObject(FollowRequest.class);
                        request.setId(doc.getId());
                        followRequests.add(request);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void setupRealtimeUpdates() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e("ProfileFragment", "No user logged in.");
            Toast.makeText(getContext(), "User not logged in.", Toast.LENGTH_LONG).show();
            return; // Exit if no user is logged in
        }

        String currentUserId = currentUser.getUid();
        DocumentReference userRef = db.collection("participants").document(currentUserId);

        userRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.e("ProfileFragment", "Listen failed.", e);
                    Toast.makeText(getContext(), "Error fetching profile data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    updateProfileCounts(snapshot);
                } else {
                    Log.e("ProfileFragment", "Current user data not found.");
                    Toast.makeText(getContext(), "Profile data not available.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void updateProfileCounts(DocumentSnapshot snapshot) {
        Long followersCount = snapshot.getLong("followersCount");
        Long followingCount = snapshot.getLong("followingCount");
        followersCountText.setText("Followers: " + (followersCount != null ? followersCount.toString() : "0"));
        followingCountText.setText("Following: " + (followingCount != null ? followingCount.toString() : "0"));
    }
}