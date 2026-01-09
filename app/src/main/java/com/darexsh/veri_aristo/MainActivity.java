package com.darexsh.veri_aristo;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.jakewharton.threetenabp.AndroidThreeTen;

import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

// MainActivity serves as the entry point for the app, managing fragments and navigation
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATION_PERMISSION = 1;
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_LAST_VERSION = "last_version";
    private static final long BACK_PRESS_WINDOW_MS = 2000;

    private FragmentManager fragmentManager;
    private ImageButton btnNotes;
    private BottomNavigationView bottomNavigationView;
    private long lastBackPressedAt = 0L;
    private SharedViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidThreeTen.init(this);
        setContentView(R.layout.activity_main);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int lastVersion = prefs.getInt(KEY_LAST_VERSION, -1);

        try {
            int currentVersion = getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionCode;

            if (currentVersion > lastVersion) {
                clearAllScheduledNotifications();
                prefs.edit().putInt(KEY_LAST_VERSION, currentVersion).apply();
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        fragmentManager = getSupportFragmentManager();
        btnNotes = findViewById(R.id.btn_notes);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        SharedViewModelFactory factory = new SharedViewModelFactory(getApplication());
        viewModel = new ViewModelProvider(this, factory).get(SharedViewModel.class);
        viewModel.getButtonColor().observe(this, color -> {
            if (color != null) {
                applyBottomNavColors(color);
            }
        });

        // Standardmäßig Home-Fragment laden
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), false);
            btnNotes.setVisibility(View.VISIBLE);
        }

        btnNotes.setOnClickListener(v -> {
            loadFragment(new NotesFragment(), true);
            btnNotes.setVisibility(View.GONE);
        });

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
                loadFragment(selectedFragment, true);
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
                Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);
                if (!(currentFragment instanceof HomeFragment)) {
                    fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    loadFragment(new HomeFragment(), false);
                    bottomNavigationView.setSelectedItemId(R.id.nav_home);
                    btnNotes.setVisibility(View.VISIBLE);
                    lastBackPressedAt = 0L;
                    return;
                }

                long now = System.currentTimeMillis();
                if (now - lastBackPressedAt < BACK_PRESS_WINDOW_MS) {
                    finish();
                } else {
                    lastBackPressedAt = now;
                    Toast.makeText(MainActivity.this, R.string.main_double_back_exit, Toast.LENGTH_SHORT).show();
                }
            }
        });

        handleOpenHomeIntent(getIntent());

        // Create notification channel and request permission
        createNotificationChannel();
        requestNotificationPermission();
    }

    private void applyBottomNavColors(int selectedColor) {
        int unselectedColor = ContextCompat.getColor(this, android.R.color.darker_gray);
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{-android.R.attr.state_checked}
        };
        int[] colors = new int[]{selectedColor, unselectedColor};
        ColorStateList tintList = new ColorStateList(states, colors);
        bottomNavigationView.setItemIconTintList(tintList);
        bottomNavigationView.setItemTextColor(tintList);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleOpenHomeIntent(intent);
    }

    private void handleOpenHomeIntent(Intent intent) {
        if (intent == null || !intent.getBooleanExtra("open_home", false)) {
            return;
        }
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        loadFragment(new HomeFragment(), false);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        btnNotes.setVisibility(View.VISIBLE);
    }

    // Load the specified fragment and add it to the back stack
    private void loadFragment(Fragment fragment) {
        loadFragment(fragment, true);
    }

    private void loadFragment(Fragment fragment, boolean addToBackStack) {
        androidx.fragment.app.FragmentTransaction transaction = fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }

    // Create a notification channel for Android O and above
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "reminder_channel",
                    getString(R.string.notifications_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(getString(R.string.notifications_channel_description));
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

    private void clearAllScheduledNotifications() {
        Intent intent = new Intent(this, NotificationReceiver.class);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        int daysRange = 365;
        for (int d = -daysRange; d < daysRange; d++) {
            for (int offset = 0; offset < 6; offset++) {
                long triggerTime = System.currentTimeMillis() + d * 24L * 60 * 60 * 1000;
                int requestCode = (int) ((triggerTime / 1000) % Integer.MAX_VALUE) + offset;

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        this,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
                );

                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent);
                    pendingIntent.cancel();
                }
            }
        }
    }
}
