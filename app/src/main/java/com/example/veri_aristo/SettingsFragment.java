package com.example.veri_aristo;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.ScrollView;
import android.graphics.Typeface;
import android.net.Uri;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import android.Manifest;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.material.button.MaterialButton;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Locale;
import com.example.veri_aristo.Constants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import com.google.gson.Gson;

// SettingsFragment allows users to configure app settings such as cycle start date, time, length, and background image
public class SettingsFragment extends Fragment {

    private MaterialButton btnSetTime;
    private MaterialButton btnSetStartDate;
    private MaterialButton btnSetCycleLength;
    private MaterialButton btnSetBackground;
    private MaterialButton btnSetCalendarRange;
    private MaterialButton btnResetApp;
    private MaterialButton btnBackupManage;
    private MaterialButton btnSetNotificationTimes;
    private MaterialButton btnSetLanguage;
    private SharedViewModel viewModel;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<String> createBackupLauncher;
    private ActivityResultLauncher<String[]> restoreBackupLauncher;

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
                        .setTitle(R.string.settings_permission_title)
                        .setMessage(R.string.settings_permission_message)
                        .setPositiveButton(R.string.dialog_ok, null)
                        .show();
            }
        });

        // Initialize ActivityResultLauncher for picking images
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                // Update the ViewModel with the new background image URI
                viewModel.setBackgroundImageUri(uri.toString());

                // Persist the URI permission to allow access to the image later
                requireContext().getContentResolver().takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
        });

        createBackupLauncher = registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"), uri -> {
            if (uri != null) {
                writeBackup(uri);
            }
        });

        restoreBackupLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) {
                readBackup(uri);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        TextView settingsTitle = view.findViewById(R.id.tv_settings_title);
        android.widget.ImageButton infoButton = view.findViewById(R.id.btn_settings_info);
        btnSetTime = view.findViewById(R.id.btn_set_time);
        btnSetStartDate = view.findViewById(R.id.btn_set_start_date);
        btnSetCycleLength = view.findViewById(R.id.btn_set_cycle_length);
        btnSetBackground = view.findViewById(R.id.btn_set_background);
        btnSetCalendarRange = view.findViewById(R.id.btn_set_calendar_range);
        btnResetApp = view.findViewById(R.id.btn_reset_app);
        btnBackupManage = view.findViewById(R.id.btn_backup_manage);
        btnSetNotificationTimes = view.findViewById(R.id.btn_set_notification_times);
        btnSetLanguage = view.findViewById(R.id.btn_set_language);

        SharedViewModelFactory factory = new SharedViewModelFactory(requireActivity().getApplication());
        viewModel = new ViewModelProvider(requireActivity(), factory).get(SharedViewModel.class);

        viewModel.getStartDate().observe(getViewLifecycleOwner(), calendar -> {
            if (calendar != null) {
                updateDateButtonText(calendar);
                updateTimeButtonText(calendar);
            }
        });

        viewModel.getCycleLength().observe(getViewLifecycleOwner(), length -> {
            if (length != null) {
                updateCycleLengthButtonText(length);
            }
        });

        viewModel.getCalendarPastAmount().observe(getViewLifecycleOwner(), amount -> {
            String unit = viewModel.getCalendarPastUnit().getValue();
            Integer futureAmount = viewModel.getCalendarFutureAmount().getValue();
            String futureUnit = viewModel.getCalendarFutureUnit().getValue();
            updateCalendarRangeButtonText(amount, unit, futureAmount, futureUnit);
        });

        viewModel.getCalendarPastUnit().observe(getViewLifecycleOwner(), unit -> {
            Integer amount = viewModel.getCalendarPastAmount().getValue();
            Integer futureAmount = viewModel.getCalendarFutureAmount().getValue();
            String futureUnit = viewModel.getCalendarFutureUnit().getValue();
            updateCalendarRangeButtonText(amount, unit, futureAmount, futureUnit);
        });

        viewModel.getCalendarFutureAmount().observe(getViewLifecycleOwner(), amount -> {
            Integer pastAmount = viewModel.getCalendarPastAmount().getValue();
            String pastUnit = viewModel.getCalendarPastUnit().getValue();
            String futureUnit = viewModel.getCalendarFutureUnit().getValue();
            updateCalendarRangeButtonText(pastAmount, pastUnit, amount, futureUnit);
        });

        viewModel.getCalendarFutureUnit().observe(getViewLifecycleOwner(), unit -> {
            Integer pastAmount = viewModel.getCalendarPastAmount().getValue();
            String pastUnit = viewModel.getCalendarPastUnit().getValue();
            Integer futureAmount = viewModel.getCalendarFutureAmount().getValue();
            updateCalendarRangeButtonText(pastAmount, pastUnit, futureAmount, unit);
        });

        viewModel.getRemovalReminderHours().observe(getViewLifecycleOwner(), hours -> {
            Integer insertionHours = viewModel.getInsertionReminderHours().getValue();
            updateNotificationTimesButtonText(hours, insertionHours);
        });

        viewModel.getInsertionReminderHours().observe(getViewLifecycleOwner(), hours -> {
            Integer removalHours = viewModel.getRemovalReminderHours().getValue();
            updateNotificationTimesButtonText(removalHours, hours);
        });

        viewModel.getRemovalReminderHours().observe(getViewLifecycleOwner(), hours -> {
            Integer insertionHours = viewModel.getInsertionReminderHours().getValue();
            updateNotificationTimesButtonText(hours, insertionHours);
        });

        viewModel.getInsertionReminderHours().observe(getViewLifecycleOwner(), hours -> {
            Integer removalHours = viewModel.getRemovalReminderHours().getValue();
            updateNotificationTimesButtonText(removalHours, hours);
        });

        // Set up button click listeners
        btnSetTime.setOnClickListener(v -> showTimePicker());
        btnSetStartDate.setOnClickListener(v -> showDatePicker());
        btnSetCycleLength.setOnClickListener(v -> showCycleLengthDialog());
        btnSetBackground.setOnClickListener(v -> checkStoragePermission());
        btnSetCalendarRange.setOnClickListener(v -> showCalendarRangeDialog());
        btnResetApp.setOnClickListener(v -> showResetDialog());
        btnBackupManage.setOnClickListener(v -> showBackupDialog());
        btnSetNotificationTimes.setOnClickListener(v -> showNotificationTimesDialog());
        btnSetLanguage.setOnClickListener(v -> showLanguageDialog());
        infoButton.setOnClickListener(v -> showAppInfoDialog());
        settingsTitle.setOnLongClickListener(v -> {
            showDebugDialog();
            return true;
        });

        updateLanguageButtonText();

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

    private void updateTimeButtonText(Calendar calendar) {
        String timeText = getString(R.string.settings_time_button_format,
                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
        btnSetTime.setText(timeText);
    }

    private void updateDateButtonText(Calendar calendar) {
        String dateText = getString(R.string.settings_date_button_format,
                calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR));
        btnSetStartDate.setText(dateText);
    }

    private void updateCycleLengthButtonText(int cycleLength) {
        String text = getString(R.string.settings_cycle_length_button, cycleLength);
        btnSetCycleLength.setText(text);
    }

    private void updateCalendarRangeButtonText(Integer pastAmount, String pastUnit,
                                               Integer futureAmount, String futureUnit) {
        if (pastAmount == null || pastUnit == null || futureAmount == null || futureUnit == null) {
            return;
        }
        String text = getString(R.string.settings_calendar_range_button,
                pastAmount, unitLabel(pastUnit), futureAmount, unitLabel(futureUnit));
        btnSetCalendarRange.setText(text);
    }

    private void updateNotificationTimesButtonText(Integer removalHours, Integer insertionHours) {
        if (removalHours == null || insertionHours == null) {
            return;
        }
        String text = getString(R.string.settings_notification_times_button, removalHours, insertionHours);
        btnSetNotificationTimes.setText(text);
    }

    // Show a TimePickerDialog to select the time
    private void showTimePicker() {
        final Calendar currentCalendar = viewModel.getStartDate().getValue() != null
                ? (Calendar) viewModel.getStartDate().getValue().clone()
                : Calendar.getInstance();

        TimePickerDialog dialog = new TimePickerDialog(requireContext(), (TimePicker view, int selectedHour, int selectedMinute) -> {
            currentCalendar.set(Calendar.HOUR_OF_DAY, selectedHour);
            currentCalendar.set(Calendar.MINUTE, selectedMinute);
            viewModel.setStartDate(currentCalendar);
            WidgetUpdater.updateAllWidgets(requireContext());
        }, currentCalendar.get(Calendar.HOUR_OF_DAY), currentCalendar.get(Calendar.MINUTE), true);

        dialog.show();
    }

    // Show a DatePickerDialog to select the start date
    private void showDatePicker() {
        final Calendar currentCalendar = viewModel.getStartDate().getValue() != null
                ? (Calendar) viewModel.getStartDate().getValue().clone()
                : Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                (DatePicker view, int selectedYear, int selectedMonth, int selectedDay) -> {
                    currentCalendar.set(selectedYear, selectedMonth, selectedDay);
                    viewModel.setStartDate(currentCalendar);
                    WidgetUpdater.updateAllWidgets(requireContext());
                }, currentCalendar.get(Calendar.YEAR), currentCalendar.get(Calendar.MONTH), currentCalendar.get(Calendar.DAY_OF_MONTH));

        dialog.show();
    }

    // Show a dialog to set the cycle length
    private void showCycleLengthDialog() {
        NumberPicker picker = new NumberPicker(requireContext());
        picker.setMinValue(14);
        picker.setMaxValue(35);

        Integer currentCycleLength = viewModel.getCycleLength().getValue();
        if (currentCycleLength != null) {
            int clamped = Math.max(14, Math.min(35, currentCycleLength));
            picker.setValue(clamped);
        } else {
            picker.setValue(21);
        }

        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);
        layout.addView(picker);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_cycle_length_title)
                .setMessage(R.string.settings_cycle_length_message)
                .setView(layout)
                .setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
                    viewModel.setCycleLength(picker.getValue());
                    WidgetUpdater.updateAllWidgets(requireContext());
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void showCalendarRangeDialog() {
        NumberPicker pastValuePicker = new NumberPicker(requireContext());
        NumberPicker pastUnitPicker = new NumberPicker(requireContext());
        NumberPicker futureValuePicker = new NumberPicker(requireContext());
        NumberPicker futureUnitPicker = new NumberPicker(requireContext());

        String[] unitValues = new String[]{getString(R.string.settings_unit_month), getString(R.string.settings_unit_year)};
        pastUnitPicker.setDisplayedValues(unitValues);
        pastUnitPicker.setMinValue(0);
        pastUnitPicker.setMaxValue(unitValues.length - 1);

        futureUnitPicker.setDisplayedValues(unitValues);
        futureUnitPicker.setMinValue(0);
        futureUnitPicker.setMaxValue(unitValues.length - 1);

        Integer currentPastAmount = viewModel.getCalendarPastAmount().getValue();
        String currentPastUnit = viewModel.getCalendarPastUnit().getValue();
        Integer currentFutureAmount = viewModel.getCalendarFutureAmount().getValue();
        String currentFutureUnit = viewModel.getCalendarFutureUnit().getValue();

        pastUnitPicker.setValue("years".equals(currentPastUnit) ? 1 : 0);
        futureUnitPicker.setValue("months".equals(currentFutureUnit) ? 0 : 1);

        configureValuePicker(pastValuePicker, pastUnitPicker.getValue(), currentPastAmount);
        configureValuePicker(futureValuePicker, futureUnitPicker.getValue(), currentFutureAmount);

        pastUnitPicker.setOnValueChangedListener((picker, oldVal, newVal) ->
                configureValuePicker(pastValuePicker, newVal, pastValuePicker.getValue()));
        futureUnitPicker.setOnValueChangedListener((picker, oldVal, newVal) ->
                configureValuePicker(futureValuePicker, newVal, futureValuePicker.getValue()));

        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);
        layout.addView(buildPickerRow(getString(R.string.settings_calendar_range_back), pastValuePicker, pastUnitPicker));
        layout.addView(buildPickerRow(getString(R.string.settings_calendar_range_forward), futureValuePicker, futureUnitPicker));

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_calendar_range_title)
                .setMessage(R.string.settings_calendar_range_message)
                .setView(layout)
                .setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
                    int pastAmount = pastValuePicker.getValue();
                    String pastUnit = pastUnitPicker.getValue() == 1 ? "years" : "months";
                    int futureAmount = futureValuePicker.getValue();
                    String futureUnit = futureUnitPicker.getValue() == 1 ? "years" : "months";
                    viewModel.setCalendarPastRange(pastAmount, pastUnit);
                    viewModel.setCalendarFutureRange(futureAmount, futureUnit);
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void showNotificationTimesDialog() {
        NumberPicker removalPicker = new NumberPicker(requireContext());
        NumberPicker insertionPicker = new NumberPicker(requireContext());
        removalPicker.setMinValue(0);
        removalPicker.setMaxValue(24);
        insertionPicker.setMinValue(0);
        insertionPicker.setMaxValue(24);

        Integer currentRemoval = viewModel.getRemovalReminderHours().getValue();
        Integer currentInsertion = viewModel.getInsertionReminderHours().getValue();
        removalPicker.setValue(currentRemoval != null ? currentRemoval : 6);
        insertionPicker.setValue(currentInsertion != null ? currentInsertion : 6);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);
        layout.addView(buildPickerRow(getString(R.string.settings_notification_before_removal), removalPicker, null));
        layout.addView(buildPickerRow(getString(R.string.settings_notification_before_insertion), insertionPicker, null));

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_notification_times_title)
                .setMessage(R.string.settings_notification_times_message)
                .setView(layout)
                .setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
                    viewModel.setRemovalReminderHours(removalPicker.getValue());
                    viewModel.setInsertionReminderHours(insertionPicker.getValue());
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void showLanguageDialog() {
        String[] options = {getString(R.string.language_german), getString(R.string.language_english)};
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.language_title)
                .setItems(options, (dialog, which) -> {
                    String tag = which == 0 ? "de" : "en";
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag));
                    WidgetUpdater.updateAllWidgets(requireContext());
                    updateLanguageButtonText();
                    requireActivity().recreate();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void updateLanguageButtonText() {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        String language = locales.isEmpty() ? Locale.getDefault().getLanguage() : locales.get(0).getLanguage();
        String label = "de".equals(language) ? getString(R.string.language_german) : getString(R.string.language_english);
        btnSetLanguage.setText(getString(R.string.language_button, label));
    }

    private String unitLabel(String unit) {
        if ("years".equals(unit)) {
            return getString(R.string.settings_unit_year_short);
        }
        return getString(R.string.settings_unit_month_short);
    }

    private void configureValuePicker(NumberPicker picker, int unitIndex, Integer currentValue) {
        int max = unitIndex == 1 ? 10 : 60;
        picker.setMinValue(0);
        picker.setMaxValue(max);
        int value = currentValue != null ? Math.min(currentValue, max) : 0;
        picker.setValue(value);
    }

    private android.widget.LinearLayout buildPickerRow(String labelText, NumberPicker valuePicker, NumberPicker unitPicker) {
        android.widget.LinearLayout row = new android.widget.LinearLayout(requireContext());
        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        row.setPadding(0, 12, 0, 12);

        TextView label = new TextView(requireContext());
        label.setText(labelText);
        label.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        valuePicker.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        if (unitPicker != null) {
            unitPicker.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        }

        row.addView(label);
        row.addView(valuePicker);
        if (unitPicker != null) {
            row.addView(unitPicker);
        }
        return row;
    }

    private void showResetDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_reset_title)
                .setMessage(R.string.settings_reset_message)
                .setPositiveButton(R.string.settings_reset_confirm, (dialog, which) -> resetAppData())
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void showBackupDialog() {
        String[] options = {getString(R.string.backup_create), getString(R.string.backup_restore)};
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.backup_title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        createBackupLauncher.launch("veri_aristo_backup.json");
                    } else {
                        restoreBackupLauncher.launch(new String[]{"application/json"});
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void showAppInfoDialog() {
        String versionName = "1.0";
        try {
            android.content.pm.PackageInfo info = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0);
            versionName = info.versionName;
        } catch (android.content.pm.PackageManager.NameNotFoundException ignored) {
        }

        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_app_info, null);
        TextView appName = content.findViewById(R.id.tv_app_name);
        TextView appVersion = content.findViewById(R.id.tv_app_version);
        TextView appDescription = content.findViewById(R.id.tv_app_description);
        TextView appDeveloper = content.findViewById(R.id.tv_app_developer);
        TextView appEmail = content.findViewById(R.id.tv_app_email);
        TextView appGithub = content.findViewById(R.id.tv_app_github);
        com.google.android.material.button.MaterialButton openEmail = content.findViewById(R.id.btn_open_email);
        com.google.android.material.button.MaterialButton openGithub = content.findViewById(R.id.btn_open_github);

        appName.setText(R.string.app_info_name);
        appVersion.setText(getString(R.string.app_info_version, versionName));
        appDescription.setText(R.string.app_info_description);
        appDeveloper.setText(getString(R.string.app_info_developer, "Darexsh by Daniel Sichler"));
        appEmail.setText(getString(R.string.app_info_email, "sichler.daniel@gmail.com"));
        appGithub.setText(getString(R.string.app_info_github, "https://github.com/Darexsh"));

        appEmail.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:sichler.daniel@gmail.com"));
            startActivity(intent);
        });

        openEmail.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:sichler.daniel@gmail.com"));
            startActivity(intent);
        });

        openGithub.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Darexsh"));
            startActivity(intent);
        });

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.app_info_title)
                .setView(content)
                .setPositiveButton(R.string.dialog_ok, null)
                .show();
    }

    private void resetAppData() {
        cancelAllScheduledNotifications();

        String uriStr = viewModel.getBackgroundImageUri().getValue();
        if (uriStr != null) {
            try {
                requireContext().getContentResolver().releasePersistableUriPermission(
                        Uri.parse(uriStr), Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) {
            }
        }

        viewModel.getRepository().clearAllData();
        requireContext().getSharedPreferences("notes_prefs", Context.MODE_PRIVATE)
                .edit().clear().apply();
        requireContext().deleteSharedPreferences("app_prefs");
        requireContext().deleteSharedPreferences("notes_prefs");

        viewModel.getRepository().saveCycleHistory(new ArrayList<>());
        viewModel.setBackgroundImageUri(null);
        viewModel.setCycleLength(21);
        Calendar resetStart = Calendar.getInstance();
        resetStart.set(Calendar.HOUR_OF_DAY, 18);
        resetStart.set(Calendar.MINUTE, 0);
        resetStart.set(Calendar.SECOND, 0);
        resetStart.set(Calendar.MILLISECOND, 0);
        viewModel.setStartDate(resetStart);
        viewModel.setCalendarPastRange(12, "months");
        viewModel.setCalendarFutureRange(2, "years");
        viewModel.setRemovalReminderHours(6);
        viewModel.setInsertionReminderHours(6);

        WidgetUpdater.updateAllWidgets(requireContext());
        Toast.makeText(requireContext(), R.string.settings_reset_done, Toast.LENGTH_SHORT).show();
    }

    private void cancelAllScheduledNotifications() {
        Intent intent = new Intent(requireContext(), NotificationReceiver.class);
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        int daysRange = 365;
        for (int d = -daysRange; d < daysRange; d++) {
            for (int offset = 0; offset < 6; offset++) {
                long triggerTime = System.currentTimeMillis() + d * 24L * 60 * 60 * 1000;
                int requestCode = (int) ((triggerTime / 1000) % Integer.MAX_VALUE) + offset;

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        requireContext(),
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

    private void writeBackup(Uri uri) {
        BackupData data = new BackupData();
        data.version = 1;
        data.appPrefs = readPrefs("app_prefs");
        data.notesPrefs = readPrefs("notes_prefs");

        try (OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri)) {
            if (outputStream == null) {
                Toast.makeText(requireContext(), R.string.backup_failed_toast, Toast.LENGTH_SHORT).show();
                return;
            }
            String json = new Gson().toJson(data);
            outputStream.write(json.getBytes(StandardCharsets.UTF_8));
            Toast.makeText(requireContext(), R.string.backup_created_toast, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.backup_failed_toast, Toast.LENGTH_SHORT).show();
        }
    }

    private void readBackup(Uri uri) {
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                Toast.makeText(requireContext(), R.string.backup_read_failed_toast, Toast.LENGTH_SHORT).show();
                return;
            }
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            byte[] bufferData = new byte[4096];
            int nRead;
            while ((nRead = inputStream.read(bufferData, 0, bufferData.length)) != -1) {
                buffer.write(bufferData, 0, nRead);
            }
            String json = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
            BackupData backupData = new Gson().fromJson(json, BackupData.class);
            if (backupData == null) {
                Toast.makeText(requireContext(), R.string.backup_invalid_toast, Toast.LENGTH_SHORT).show();
                return;
            }
            writePrefs("app_prefs", backupData.appPrefs);
            writePrefs("notes_prefs", backupData.notesPrefs);

            viewModel.getRepository().clearNotificationFlags();

            SettingsRepository restoredRepository = new SettingsRepository(requireContext());
            viewModel.setBackgroundImageUri(restoredRepository.getBackgroundImageUri());
            viewModel.setCycleLength(restoredRepository.getCycleLength());
            viewModel.setStartDate(restoredRepository.getStartDate());
            viewModel.setCalendarPastRange(restoredRepository.getCalendarPastAmount(),
                    restoredRepository.getCalendarPastUnit());
            viewModel.setCalendarFutureRange(restoredRepository.getCalendarFutureAmount(),
                    restoredRepository.getCalendarFutureUnit());
            viewModel.setRemovalReminderHours(restoredRepository.getRemovalReminderHours());
            viewModel.setInsertionReminderHours(restoredRepository.getInsertionReminderHours());

            WidgetUpdater.updateAllWidgets(requireContext());
            Toast.makeText(requireContext(), R.string.backup_restored_toast, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.backup_read_failed_toast, Toast.LENGTH_SHORT).show();
        }
    }

    private Map<String, PrefValue> readPrefs(String name) {
        Map<String, ?> all = requireContext().getSharedPreferences(name, Context.MODE_PRIVATE).getAll();
        Map<String, PrefValue> result = new HashMap<>();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Boolean) {
                result.put(entry.getKey(), new PrefValue("boolean", value));
            } else if (value instanceof Integer) {
                result.put(entry.getKey(), new PrefValue("int", value));
            } else if (value instanceof Long) {
                result.put(entry.getKey(), new PrefValue("long", value));
            } else if (value instanceof Float) {
                result.put(entry.getKey(), new PrefValue("float", value));
            } else if (value instanceof String) {
                result.put(entry.getKey(), new PrefValue("string", value));
            }
        }
        return result;
    }

    private void writePrefs(String name, Map<String, PrefValue> values) {
        if (values == null) {
            return;
        }
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences(name, Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        for (Map.Entry<String, PrefValue> entry : values.entrySet()) {
            PrefValue prefValue = entry.getValue();
            if (prefValue == null || prefValue.type == null) {
                continue;
            }
            switch (prefValue.type) {
                case "boolean":
                    editor.putBoolean(entry.getKey(), toBoolean(prefValue.value));
                    break;
                case "int":
                    editor.putInt(entry.getKey(), toInt(prefValue.value));
                    break;
                case "long":
                    editor.putLong(entry.getKey(), toLong(prefValue.value));
                    break;
                case "float":
                    editor.putFloat(entry.getKey(), toFloat(prefValue.value));
                    break;
                case "string":
                    editor.putString(entry.getKey(), prefValue.value != null ? String.valueOf(prefValue.value) : null);
                    break;
                default:
                    break;
            }
        }
        editor.apply();
    }

    private boolean toBoolean(Object value) {
        return value instanceof Boolean && (Boolean) value;
    }

    private int toInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    private long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    private float toFloat(Object value) {
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return 0f;
    }

    private static class BackupData {
        int version;
        Map<String, PrefValue> appPrefs;
        Map<String, PrefValue> notesPrefs;
    }

    private static class PrefValue {
        String type;
        Object value;

        PrefValue(String type, Object value) {
            this.type = type;
            this.value = value;
        }
    }

    private void showDebugDialog() {
        TextView content = new TextView(requireContext());
        content.setTypeface(Typeface.MONOSPACE);
        content.setTextSize(12f);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        content.setPadding(padding, padding, padding, padding);
        content.setText(buildDebugInfo());

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.addView(content);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.debug_dialog_title)
                .setView(scrollView)
                .setPositiveButton(R.string.dialog_ok, null)
                .show();
    }

    private String buildDebugInfo() {
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        Calendar now = Calendar.getInstance();

        Calendar baseStart = viewModel.getStartDate().getValue();
        if (baseStart == null) {
            baseStart = Calendar.getInstance();
        }
        int cycleLength = getSafeCycleLength();
        int ringFreeDays = Constants.RING_FREE_DAYS;
        int removalReminderHours = viewModel.getRepository().getRemovalReminderHours();
        int insertionReminderHours = viewModel.getRepository().getInsertionReminderHours();

        Calendar currentStart = (Calendar) baseStart.clone();
        Calendar removalDate = (Calendar) currentStart.clone();
        removalDate.add(Calendar.DAY_OF_MONTH, cycleLength);
        Calendar reinsertionDate = (Calendar) removalDate.clone();
        reinsertionDate.add(Calendar.DAY_OF_MONTH, ringFreeDays);

        int guard = 0;
        while (now.after(reinsertionDate) && guard < 500) {
            currentStart.add(Calendar.DAY_OF_MONTH, cycleLength + ringFreeDays);
            removalDate = (Calendar) currentStart.clone();
            removalDate.add(Calendar.DAY_OF_MONTH, cycleLength);
            reinsertionDate = (Calendar) removalDate.clone();
            reinsertionDate.add(Calendar.DAY_OF_MONTH, ringFreeDays);
            guard++;
        }

        Integer pastAmount = viewModel.getCalendarPastAmount().getValue();
        String pastUnit = viewModel.getCalendarPastUnit().getValue();
        Integer futureAmount = viewModel.getCalendarFutureAmount().getValue();
        String futureUnit = viewModel.getCalendarFutureUnit().getValue();

        Calendar pastLimit = (Calendar) now.clone();
        pastLimit.add(Calendar.MONTH, -Math.max(convertToMonths(pastAmount, pastUnit), 0));
        Calendar futureLimit = (Calendar) now.clone();
        futureLimit.add(Calendar.MONTH, Math.max(convertToMonths(futureAmount, futureUnit), 0));

        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.debug_now)).append(": ").append(format.format(now.getTime())).append("\n");
        sb.append(getString(R.string.debug_base_start)).append(": ").append(format.format(baseStart.getTime())).append("\n");
        sb.append(getString(R.string.debug_cycle_length, cycleLength)).append("\n");
        sb.append(getString(R.string.debug_ring_free, ringFreeDays)).append("\n");
        sb.append(getString(R.string.debug_removal_reminder, removalReminderHours)).append("\n");
        sb.append(getString(R.string.debug_insertion_reminder, insertionReminderHours)).append("\n");
        sb.append(getString(R.string.debug_current_start)).append(": ").append(format.format(currentStart.getTime())).append("\n");
        sb.append(getString(R.string.debug_removal)).append(": ").append(format.format(removalDate.getTime())).append("\n");
        sb.append(getString(R.string.debug_insertion)).append(": ").append(format.format(reinsertionDate.getTime())).append("\n");
        sb.append(getString(R.string.debug_notified_for_cycle)).append(": ")
                .append(viewModel.getRepository().wasNotificationScheduledForCycle(currentStart.getTimeInMillis()))
                .append("\n");
        sb.append(getString(R.string.debug_calendar_range)).append(": ")
                .append(pastAmount).append(" ").append(unitLabel(pastUnit))
                .append(" ").append(getString(R.string.settings_calendar_range_back))
                .append(", ")
                .append(futureAmount).append(" ").append(unitLabel(futureUnit))
                .append(" ").append(getString(R.string.settings_calendar_range_forward))
                .append("\n");
        sb.append(getString(R.string.debug_calendar_limits)).append(": ")
                .append(format.format(pastLimit.getTime()))
                .append(" -> ")
                .append(format.format(futureLimit.getTime()))
                .append("\n\n");

        sb.append(getString(R.string.debug_scheduled_notifications)).append(":\n");
        appendNotificationLine(sb, getString(R.string.debug_two_weeks_remaining), shiftDays(removalDate, -14), cycleLength >= 14);
        appendNotificationLine(sb, getString(R.string.debug_one_week_remaining), shiftDays(removalDate, -7), cycleLength >= 7);
        appendNotificationLine(sb, getString(R.string.debug_removal_reminder_line, removalReminderHours),
                shiftHours(removalDate, -removalReminderHours), removalReminderHours > 0);
        appendNotificationLine(sb, getString(R.string.debug_removal), removalDate, true);
        appendNotificationLine(sb, getString(R.string.debug_insertion_reminder_line, insertionReminderHours),
                shiftHours(reinsertionDate, -insertionReminderHours), insertionReminderHours > 0);
        appendNotificationLine(sb, getString(R.string.debug_insertion), reinsertionDate, true);

        return sb.toString();
    }

    private int getSafeCycleLength() {
        Integer cycleLength = viewModel.getCycleLength().getValue();
        return cycleLength != null && cycleLength > 0 ? cycleLength : 21;
    }

    private int convertToMonths(Integer amount, String unit) {
        if (amount == null || unit == null) {
            return 0;
        }
        return "years".equals(unit) ? amount * 12 : amount;
    }

    private Calendar shiftDays(Calendar base, int days) {
        Calendar cal = (Calendar) base.clone();
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal;
    }

    private Calendar shiftHours(Calendar base, int hours) {
        Calendar cal = (Calendar) base.clone();
        cal.add(Calendar.HOUR_OF_DAY, hours);
        return cal;
    }

    private void appendNotificationLine(StringBuilder sb, String label, Calendar time, boolean enabled) {
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        boolean inPast = time.getTimeInMillis() <= System.currentTimeMillis();
        sb.append("- ").append(label).append(": ").append(format.format(time.getTime()));
        sb.append(enabled ? "" : getString(R.string.debug_disabled_suffix));
        sb.append(inPast ? getString(R.string.debug_past_suffix) : getString(R.string.debug_future_suffix));
        sb.append("\n");
    }
}
