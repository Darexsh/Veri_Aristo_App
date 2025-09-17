package com.example.veri_aristo;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.jakewharton.threetenabp.AndroidThreeTen;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

// MainActivity serves as the entry point for the app, managing fragments and navigation
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATION_PERMISSION = 1;

    private FragmentManager fragmentManager;
    private ImageButton btnNotes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidThreeTen.init(this);
        setContentView(R.layout.activity_main);

        fragmentManager = getSupportFragmentManager();
        btnNotes = findViewById(R.id.btn_notes);

        // Standardmäßig Home-Fragment laden
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            btnNotes.setVisibility(View.VISIBLE);
        }

        btnNotes.setOnClickListener(v -> {
            loadFragment(new NotesFragment());
            btnNotes.setVisibility(View.GONE);
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                selectedFragment = new HomeFragment(); // Load HomeFragment
                btnNotes.setVisibility(View.VISIBLE);
            } else if (id == R.id.nav_calendar) {
                selectedFragment = new CalendarFragment(); // Load CalendarFragment
                btnNotes.setVisibility(View.GONE);
            } else if (id == R.id.nav_cycles) {
                selectedFragment = new CyclesFragment(); // Load CyclesFragment
                btnNotes.setVisibility(View.GONE);
            } else if (id == R.id.nav_settings) {
                selectedFragment = new SettingsFragment(); // Load SettingsFragment
                btnNotes.setVisibility(View.GONE);
            } else {
                btnNotes.setVisibility(View.GONE); // Hide notes button for other fragments
            }

            // If a valid fragment is selected, load it
            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }

            return false;
        });

        // Listen for back stack changes to manage visibility of the notes button
        fragmentManager.addOnBackStackChangedListener(() -> {
            Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);
            if (currentFragment instanceof HomeFragment) {
                btnNotes.setVisibility(View.VISIBLE);
            } else {
                btnNotes.setVisibility(View.GONE);
            }
        });

        // Handle back button presses to navigate through fragments
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (fragmentManager.getBackStackEntryCount() > 1) {
                    fragmentManager.popBackStack();
                } else {
                    finish();
                }
            }
        });

        // Create notification channel and request permission
        createNotificationChannel();
        requestNotificationPermission();
    }

    // Load the specified fragment and add it to the back stack
    private void loadFragment(Fragment fragment) {
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    // Create a notification channel for Android O and above
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "reminder_channel",
                    "Erinnerungen",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Benachrichtigungen für Ringzyklen");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    // Request notification permission for Android 13 and above
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION);
            }
        }
    }

    // Handle the result of the permission request
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted → proceed with notification scheduling
            } else {
                // Permission denied → handle accordingly
            }
        }
    }
}
