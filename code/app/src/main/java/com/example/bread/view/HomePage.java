package com.example.bread.view;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.example.bread.R;
import com.example.bread.databinding.ActivityHomePageBinding;
import com.example.bread.fragment.AddMoodEventFragment;
import com.example.bread.fragment.FollowRequestsFragment;
import com.example.bread.fragment.HistoryFragment;
import com.example.bread.fragment.HomeFragment;
import com.example.bread.fragment.MapFragment;
import com.example.bread.fragment.ProfileFragment;
import com.example.bread.fragment.UserSearchFragment;

public class HomePage extends AppCompatActivity {

    ActivityHomePageBinding binding;

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //followed the following video for navigation bar implementation, accessed on Feb 27 2025
        //https://www.youtube.com/watch?v=jOFLmKMOcK0
        binding = ActivityHomePageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        replaceFragment(new HomeFragment());

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.home) {
                replaceFragment(new HomeFragment());
            } else if (itemId == R.id.map) {
                replaceFragment(new MapFragment());
            } else if (itemId == R.id.add) {
                // Different approaches in the two versions:
                // 1. Your branch: Starts AddMoodEventActivity
                // 2. Main branch: Uses AddMoodEventFragment
                // We'll use the fragment approach from main:
                replaceFragment(new AddMoodEventFragment());

                // If you need the activity approach, uncomment these lines:
                /*
                Intent intent = new Intent(HomePage.this, AddMoodEventActivity.class);
                startActivity(intent);
                return false; // Don't select the tab
                */
            } else if (itemId == R.id.history) {
                replaceFragment(new HistoryFragment());
            } else if (itemId == R.id.profile) {
                replaceFragment(new ProfileFragment());
            }

            return true;  // Important to return true to indicate the item was selected
        });
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.frame_layout, fragment);
        transaction.commit();
    }

    public void selectHomeNavigation() {
        binding.bottomNavigationView.setSelectedItemId(R.id.home);
    }

    // Method to navigate to specific fragments from your branch
    public void navigateToFragment(String fragmentName) {
        Fragment fragment = null;

        switch (fragmentName) {
            case "followRequests":
                fragment = new FollowRequestsFragment();
                break;
            case "userSearch":
                fragment = new UserSearchFragment();
                break;
            case "profile":
                fragment = new ProfileFragment();
                break;
        }

        if (fragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.frame_layout, fragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }
}