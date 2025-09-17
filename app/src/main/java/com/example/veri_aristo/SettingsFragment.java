package com.example.veri_aristo;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import android.Manifest;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.material.button.MaterialButton;
import java.util.Calendar;

// SettingsFragment allows users to configure app settings such as cycle start date, time, length, and background image
public class SettingsFragment extends Fragment {

    // SharedPreferences keys for storing settings
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_START_DAY = "start_day";
    private static final String KEY_START_MONTH = "start_month";
    private static final String KEY_START_YEAR = "start_year";
    private static final String KEY_SET_TIME_HOUR = "set_time_hour";
    private static final String KEY_SET_TIME_MINUTE = "set_time_minute";
    private static final String KEY_CYCLE_LENGTH = "cycle_length";
    private static final String KEY_BACKGROUND_IMAGE_URI = "background_image_uri";

    // Default values for settings
    private int day;
    private int month;
    private int year;
    private int hour = 18;
    private int minute = 0;
    private int cycleLength = 21;
    private String backgroundImageUri;
    private MaterialButton btnSetTime;
    private MaterialButton btnSetStartDate;
    private MaterialButton btnSetCycleLength;
    private MaterialButton btnSetBackground;
    private SharedViewModel viewModel;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize ActivityResultLauncher for permission requests
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                openGallery();
            } else {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Berechtigung erforderlich")
                        .setMessage("Die Berechtigung für den Zugriff auf Bilder ist erforderlich, um ein Hintergrundbild auszuwählen.")
                        .setPositiveButton("OK", null)
                        .show();
            }
        });

        // Initialize ActivityResultLauncher for picking images
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                backgroundImageUri = uri.toString();

                // Save the selected image URI to SharedPreferences
                saveBackgroundImageToPrefs(backgroundImageUri);

                // Update the ViewModel with the new background image URI
                viewModel.setBackgroundImageUri(backgroundImageUri);

                // Persist the URI permission to allow access to the image later
                requireContext().getContentResolver().takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        btnSetTime = view.findViewById(R.id.btn_set_time);
        btnSetStartDate = view.findViewById(R.id.btn_set_start_date);
        btnSetCycleLength = view.findViewById(R.id.btn_set_cycle_length);
        btnSetBackground = view.findViewById(R.id.btn_set_background);

        loadTimeFromPrefs();            // Load saved time from SharedPreferences
        loadDateFromPrefs();            // Load saved date from SharedPreferences
        loadCycleLengthFromPrefs();     // Load saved cycle length from SharedPreferences
        loadBackgroundImageFromPrefs(); // Load saved background image URI from SharedPreferences

        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        // Set initial values in ViewModel
        viewModel.setCycleLength(cycleLength);
        viewModel.setStartDay(day);
        viewModel.setStartMonth(month);
        viewModel.setStartYear(year);
        viewModel.setHour(hour);
        viewModel.setMinute(minute);
        viewModel.setBackgroundImageUri(backgroundImageUri);

        // Update button texts with current settings
        updateTimeButtonText();
        updateDateButtonText();
        updateCycleLengthButtonText();

        // Set up button click listeners
        btnSetTime.setOnClickListener(v -> showTimePicker());
        btnSetStartDate.setOnClickListener(v -> showDatePicker());
        btnSetCycleLength.setOnClickListener(v -> showCycleLengthDialog());
        btnSetBackground.setOnClickListener(v -> checkStoragePermission());

        return view;
    }

    // Check if the app has permission to read media images, and open the gallery if granted
    private void checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES)
                == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
        }
    }

    // Open the gallery to select a background image
    private void openGallery() {
        pickImageLauncher.launch("image/*");
    }

    // Load the time settings from SharedPreferences
    private void loadTimeFromPrefs() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        hour = prefs.getInt(KEY_SET_TIME_HOUR, 18);
        minute = prefs.getInt(KEY_SET_TIME_MINUTE, 0);
    }

    // Save the time settings to SharedPreferences
    private void saveTimeToPrefs(int h, int m) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putInt(KEY_SET_TIME_HOUR, h)
                .putInt(KEY_SET_TIME_MINUTE, m)
                .apply();
    }

    // Update the button text to reflect the current time settings
    private void updateTimeButtonText() {
        String timeText = String.format("Einlege-Zeit einstellen: %02d:%02d", hour, minute);
        btnSetTime.setText(timeText);
    }

    // Show a TimePickerDialog to select the time
    private void showTimePicker() {
        TimePickerDialog dialog = new TimePickerDialog(requireContext(), (TimePicker view, int selectedHour, int selectedMinute) -> {
            hour = selectedHour;
            minute = selectedMinute;
            saveTimeToPrefs(hour, minute);
            updateTimeButtonText();
            viewModel.setHour(hour);
            viewModel.setMinute(minute);
        }, hour, minute, true);

        dialog.show();
    }

    // Load the start date settings from SharedPreferences
    private void loadDateFromPrefs() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        Calendar calendar = Calendar.getInstance();

        day = prefs.getInt(KEY_START_DAY, calendar.get(Calendar.DAY_OF_MONTH));
        month = prefs.getInt(KEY_START_MONTH, calendar.get(Calendar.MONTH));
        year = prefs.getInt(KEY_START_YEAR, calendar.get(Calendar.YEAR));
    }

    // Save the start date settings to SharedPreferences
    private void saveDateToPrefs(int d, int m, int y) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putInt(KEY_START_DAY, d)
                .putInt(KEY_START_MONTH, m)
                .putInt(KEY_START_YEAR, y)
                .apply();
    }

    // Update the button text to reflect the current start date settings
    private void updateDateButtonText() {
        String dateText = String.format("Einlege-Datum einstellen: %02d.%02d.%d", day, month + 1, year);
        btnSetStartDate.setText(dateText);
    }

    // Show a DatePickerDialog to select the start date
    private void showDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                (DatePicker view, int selectedYear, int selectedMonth, int selectedDay) -> {
                    day = selectedDay;
                    month = selectedMonth;
                    year = selectedYear;
                    saveDateToPrefs(day, month, year);
                    updateDateButtonText();
                    viewModel.setStartDay(day);
                    viewModel.setStartMonth(month);
                    viewModel.setStartYear(year);
                }, year, month, day);

        dialog.show();
    }

    // Load the cycle length from SharedPreferences
    private void loadCycleLengthFromPrefs() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        cycleLength = prefs.getInt(KEY_CYCLE_LENGTH, 21);
    }

    // Save the cycle length to SharedPreferences
    private void saveCycleLengthToPrefs(int length) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putInt(KEY_CYCLE_LENGTH, length)
                .apply();
    }

    // Update the button text to reflect the current cycle length
    private void updateCycleLengthButtonText() {
        String text = "Zykluslänge einstellen: " + cycleLength + " Tage";
        btnSetCycleLength.setText(text);
    }

    // Show a dialog to set the cycle length
    private void showCycleLengthDialog() {
        final EditText input = new EditText(requireContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(cycleLength));

        new AlertDialog.Builder(requireContext())
                .setTitle("Zykluslänge einstellen")
                .setMessage("Gib die Zykluslänge in Tagen ein:")
                .setView(input)
                .setPositiveButton("OK", (dialog, which) -> {
                    String value = input.getText().toString();
                    if (!value.isEmpty()) {
                        int newLength = Integer.parseInt(value);
                        if (newLength > 0) {
                            cycleLength = newLength;
                            saveCycleLengthToPrefs(cycleLength);
                            updateCycleLengthButtonText();
                            viewModel.setCycleLength(cycleLength);
                        }
                    }
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    // Load the background image URI from SharedPreferences
    private void loadBackgroundImageFromPrefs() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        backgroundImageUri = prefs.getString(KEY_BACKGROUND_IMAGE_URI, null);
    }

    // Save the background image URI to SharedPreferences
    private void saveBackgroundImageToPrefs(String uri) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_BACKGROUND_IMAGE_URI, uri)
                .apply();
    }
}