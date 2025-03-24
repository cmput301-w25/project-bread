package com.example.bread.view;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.example.bread.R;
import com.example.bread.databinding.ActivityHomePageBinding;
import com.example.bread.fragment.AddMoodEventFragment;
import com.example.bread.fragment.HistoryFragment;
import com.example.bread.fragment.HomeFragment;
import com.example.bread.fragment.MapFragment;
import com.example.bread.fragment.ProfileFragment;

/**
 * Represents the home page of the app, where users can navigate to different fragments.
 */
public class HomePage extends AppCompatActivity {

    ActivityHomePageBinding binding;

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
                // Show AddMoodEventFragment as a dialog
                AddMoodEventFragment dialogFragment = new AddMoodEventFragment();
                dialogFragment.show(getSupportFragmentManager(), "AddMoodEventFragment");
                return true; // Keep the current fragment (e.g., HomeFragment) displayed
            } else if (itemId == R.id.history) {
                replaceFragment(new HistoryFragment());
            } else if (itemId == R.id.profile) {
                replaceFragment(new ProfileFragment());
            }

            return true; // Indicate the item was selected
        });
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction().setCustomAnimations(
                R.anim.slide_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out
        );
        transaction.replace(R.id.frame_layout, fragment);
        transaction.commit();
    }

    public void selectHomeNavigation() {
        binding.bottomNavigationView.setSelectedItemId(R.id.home);
    }
}
