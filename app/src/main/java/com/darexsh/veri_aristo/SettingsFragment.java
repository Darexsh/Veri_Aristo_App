package com.darexsh.veri_aristo;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Build;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.Button;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import android.widget.ProgressBar;
import android.widget.LinearLayout;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import android.Manifest;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Locale;
import com.darexsh.veri_aristo.Constants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.prolificinteractive.materialcalendarview.BuildConfig;

// SettingsFragment allows users to configure app settings such as cycle start date, time, length, and background image
public class SettingsFragment extends Fragment {

    private MaterialButton btnSetTime;
    private MaterialButton btnSetStartDate;
    private MaterialButton btnSetCycleLength;
    private MaterialButton btnSetBackground;
    private MaterialButton btnSetCalendarRange;
    private MaterialButton btnResetApp;
    private MaterialButton btnBackupManage;
    private MaterialButton btnUpdateApp;
    private MaterialButton btnSetNotificationTimes;
    private MaterialButton btnSetLanguage;
    private MaterialButton btnSetButtonColor;
    private MaterialButton btnSetCircleColor;
    private MaterialButton btnSetCircleStyle;
    private MaterialButton btnWelcomeTour;
    private View advancedContent;
    private View advancedHeader;
    private android.widget.ImageButton btnAdvancedToggle;
    private android.widget.ImageButton btnSettingsInfo;
    private SharedViewModel viewModel;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<String> createBackupLauncher;
    private ActivityResultLauncher<String[]> restoreBackupLauncher;
    private ActivityResultLauncher<Intent> manageUnknownSourcesLauncher;
    private int[] buttonColorValues;
    private String[] buttonColorLabels;
    private String[] circleStyleLabels;
    private Drawable[] circleStylePreviews;
    private static final String RELEASES_URL = "https://api.github.com/repos/Darexsh/Veri_Aristo_App/releases";
    private File pendingApkFile;
    private AlertDialog downloadDialog;
    private ProgressBar downloadProgressBar;
    private TextView downloadProgressText;

    private interface ColorConsumer {
        void accept(int color);
    }


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
                AlertDialog dialog = new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.settings_permission_title)
                        .setMessage(R.string.settings_permission_message)
                        .setPositiveButton(R.string.dialog_ok, null)
                        .show();
                applyDialogButtonColors(dialog);
            }
        });

        // Initialize ActivityResultLauncher for picking images
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
                        return;
                    }
                    Uri uri = result.getData().getData();
                    if (uri == null) {
                        return;
                    }
                    String savedUri = saveBackgroundImage(uri);
                    if (savedUri != null) {
                        viewModel.setBackgroundImageUri(savedUri);
                    }
                }
        );

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

        manageUnknownSourcesLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (!isAdded()) {
                        return;
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                            && requireContext().getPackageManager().canRequestPackageInstalls()
                            && pendingApkFile != null) {
                        promptInstall(pendingApkFile);
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        TextView settingsTitle = view.findViewById(R.id.tv_settings_title);
        btnSettingsInfo = view.findViewById(R.id.btn_settings_info);
        btnSetTime = view.findViewById(R.id.btn_set_time);
        btnSetStartDate = view.findViewById(R.id.btn_set_start_date);
        btnSetCycleLength = view.findViewById(R.id.btn_set_cycle_length);
        btnSetBackground = view.findViewById(R.id.btn_set_background);
        btnSetCalendarRange = view.findViewById(R.id.btn_set_calendar_range);
        btnResetApp = view.findViewById(R.id.btn_reset_app);
        btnBackupManage = view.findViewById(R.id.btn_backup_manage);
        btnUpdateApp = view.findViewById(R.id.btn_update_app);
        btnWelcomeTour = view.findViewById(R.id.btn_welcome_tour);
        advancedContent = view.findViewById(R.id.advanced_content);
        advancedHeader = view.findViewById(R.id.advanced_header);
        btnAdvancedToggle = view.findViewById(R.id.btn_advanced_toggle);
        btnSetNotificationTimes = view.findViewById(R.id.btn_set_notification_times);
        btnSetLanguage = view.findViewById(R.id.btn_set_language);
        btnSetButtonColor = view.findViewById(R.id.btn_set_button_color);
        btnSetCircleColor = view.findViewById(R.id.btn_set_circle_color);
        btnSetCircleStyle = view.findViewById(R.id.btn_set_circle_style);

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

        viewModel.getButtonColor().observe(getViewLifecycleOwner(), color -> {
            if (color != null) {
                applyPrimaryButtonColor(color);
                updateButtonColorButtonText(color);
                btnSettingsInfo.setColorFilter(color);
            }
        });

        viewModel.getHomeCircleColor().observe(getViewLifecycleOwner(), color -> {
            if (color != null) {
                updateCircleColorButtonText(color);
                circleStylePreviews = null;
            }
        });

        viewModel.getHomeCircleStyle().observe(getViewLifecycleOwner(), style -> {
            if (style != null) {
                updateCircleStyleButtonText(style);
            }
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
        btnSetButtonColor.setOnClickListener(v -> showButtonColorDialog());
        btnSetCircleColor.setOnClickListener(v -> showCircleColorDialog());
        btnSetCircleStyle.setOnClickListener(v -> showCircleStyleDialog());
        btnWelcomeTour.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).restartWelcomeTour();
            }
        });
        btnUpdateApp.setOnClickListener(v -> checkForUpdates());
        advancedHeader.setOnClickListener(v -> toggleAdvancedSection());
        btnAdvancedToggle.setOnClickListener(v -> toggleAdvancedSection());
        btnSettingsInfo.setOnClickListener(v -> showAppInfoDialog());
        settingsTitle.setOnLongClickListener(v -> {
            showDebugDialog();
            return true;
        });

        updateLanguageButtonText();

        return view;
    }

    // Check if the app has permission to read media images, and open the gallery if granted
    private void checkStoragePermission() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(requireContext(), permission)
                == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }

    // Open the gallery to select a background image
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    @Nullable
    private String saveBackgroundImage(@NonNull Uri sourceUri) {
        File directory = new File(requireContext().getFilesDir(), "backgrounds");
        if (!directory.exists() && !directory.mkdirs()) {
            return null;
        }
        File targetFile = new File(directory, "background_image");
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(sourceUri);
             OutputStream outputStream = new FileOutputStream(targetFile)) {
            if (inputStream == null) {
                return null;
            }
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
            return Uri.fromFile(targetFile).toString();
        } catch (IOException e) {
            return null;
        }
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
        applyDialogButtonColors(dialog);
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
        applyDialogButtonColors(dialog);
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

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_cycle_length_title)
                .setMessage(R.string.settings_cycle_length_message)
                .setView(layout)
                .setPositiveButton(R.string.dialog_ok, (dlg, which) -> {
                    viewModel.setCycleLength(picker.getValue());
                    WidgetUpdater.updateAllWidgets(requireContext());
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
        applyDialogButtonColors(dialog);
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

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_calendar_range_title)
                .setMessage(R.string.settings_calendar_range_message)
                .setView(layout)
                .setPositiveButton(R.string.dialog_ok, (dlg, which) -> {
                    int pastAmount = pastValuePicker.getValue();
                    String pastUnit = pastUnitPicker.getValue() == 1 ? "years" : "months";
                    int futureAmount = futureValuePicker.getValue();
                    String futureUnit = futureUnitPicker.getValue() == 1 ? "years" : "months";
                    viewModel.setCalendarPastRange(pastAmount, pastUnit);
                    viewModel.setCalendarFutureRange(futureAmount, futureUnit);
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
        applyDialogButtonColors(dialog);
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

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_notification_times_title)
                .setMessage(R.string.settings_notification_times_message)
                .setView(layout)
                .setPositiveButton(R.string.dialog_ok, (dlg, which) -> {
                    viewModel.setRemovalReminderHours(removalPicker.getValue());
                    viewModel.setInsertionReminderHours(insertionPicker.getValue());
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
        applyDialogButtonColors(dialog);
    }

    private void showLanguageDialog() {
        String[] options = {getString(R.string.language_german), getString(R.string.language_english)};
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.language_title)
                .setItems(options, (dlg, which) -> {
                    String tag = which == 0 ? "de" : "en";
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag));
                    WidgetUpdater.updateAllWidgets(requireContext());
                    updateLanguageButtonText();
                    requireActivity().recreate();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
        applyDialogButtonColors(dialog);
    }

    private void updateLanguageButtonText() {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        String language = locales.isEmpty() ? Locale.getDefault().getLanguage() : locales.get(0).getLanguage();
        String label = "de".equals(language) ? getString(R.string.language_german) : getString(R.string.language_english);
        btnSetLanguage.setText(getString(R.string.language_button, label));
    }

    private void showButtonColorDialog() {
        Integer currentColor = viewModel.getButtonColor().getValue();
        int selectedColor = currentColor != null ? currentColor : SettingsRepository.DEFAULT_BUTTON_COLOR;
        showColorDialog(
                R.string.settings_button_color_dialog_title,
                selectedColor,
                R.string.settings_button_color_custom_title,
                R.string.settings_button_color_widget_note,
                color -> {
                    viewModel.setButtonColor(color);
                    WidgetUpdater.updateAllWidgets(requireContext());
                }
        );
    }

    private void showCircleColorDialog() {
        Integer currentColor = viewModel.getHomeCircleColor().getValue();
        int selectedColor = currentColor != null ? currentColor : SettingsRepository.DEFAULT_HOME_CIRCLE_COLOR;
        showColorDialog(
                R.string.settings_circle_color_dialog_title,
                selectedColor,
                R.string.settings_circle_color_custom_title,
                R.string.settings_circle_color_widget_note,
                color -> {
                    viewModel.setHomeCircleColor(color);
                    WidgetUpdater.updateAllWidgets(requireContext());
                }
        );
    }

    private void showColorDialog(int titleResId, int selectedColor, int customTitleResId, int noteResId, ColorConsumer onSelect) {
        ensureButtonColorOptionsLoaded();
        int selectedIndex = getButtonColorIndex(selectedColor);
        final int[] pendingColor = new int[]{selectedColor};

        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_button_color_list, null);
        android.widget.ListView listView = content.findViewById(R.id.list_button_colors);
        MaterialButton customButton = content.findViewById(R.id.btn_custom_color);
        MaterialButton cancelButton = content.findViewById(R.id.btn_cancel_color);
        android.widget.TextView widgetNote = content.findViewById(R.id.tv_color_dialog_note);
        if (widgetNote != null) {
            widgetNote.setText(noteResId);
            widgetNote.setVisibility(View.VISIBLE);
        }
        Integer buttonColor = viewModel != null ? viewModel.getButtonColor().getValue() : null;
        if (buttonColor != null) {
            ButtonColorHelper.applyPrimaryColor(customButton, buttonColor);
            ButtonColorHelper.applyPrimaryColor(cancelButton, buttonColor);
            customButton.setTextColor(Color.WHITE);
            cancelButton.setTextColor(Color.WHITE);
        }
        android.widget.ListAdapter adapter = new android.widget.ArrayAdapter<String>(
                requireContext(),
                R.layout.dialog_button_color_item,
                android.R.id.text1,
                buttonColorLabels
        ) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                View swatch = view.findViewById(R.id.view_color_swatch);
                if (swatch != null) {
                    swatch.setBackgroundColor(buttonColorValues[position]);
                }
                return view;
            }
        };

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(titleResId)
                .setView(content)
                .create();

        listView.setAdapter(adapter);
        listView.setChoiceMode(android.widget.ListView.CHOICE_MODE_SINGLE);
        if (selectedIndex >= 0) {
            listView.setItemChecked(selectedIndex, true);
        }
        listView.setOnItemClickListener((parent, view, position, id) -> {
            pendingColor[0] = buttonColorValues[position];
            onSelect.accept(pendingColor[0]);
            dialog.dismiss();
        });

        customButton.setOnClickListener(v -> {
            dialog.dismiss();
            showCustomColorDialog(customTitleResId, pendingColor[0], onSelect);
        });
        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        applyDialogButtonColors(dialog);
    }

    private void showCustomColorDialog(int titleResId, int initialColor, ColorConsumer onSelect) {
        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_button_color_custom, null);
        HsvColorWheelView colorWheel = content.findViewById(R.id.color_wheel);
        View preview = content.findViewById(R.id.view_color_preview);
        final int[] pendingColor = new int[]{initialColor};
        preview.setBackgroundTintList(android.content.res.ColorStateList.valueOf(initialColor));
        colorWheel.setColor(initialColor);
        colorWheel.setOnColorChangeListener(color -> {
            pendingColor[0] = color;
            preview.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        });

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(titleResId)
                .setView(content)
                .setPositiveButton(R.string.dialog_ok, (dlg, which) -> onSelect.accept(pendingColor[0]))
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
        applyDialogButtonColors(dialog);
    }

    private void applyPrimaryButtonColor(int color) {
        ButtonColorHelper.applyPrimaryColor(btnSetTime, color);
        ButtonColorHelper.applyPrimaryColor(btnSetStartDate, color);
        ButtonColorHelper.applyPrimaryColor(btnSetCycleLength, color);
        ButtonColorHelper.applyPrimaryColor(btnSetBackground, color);
        ButtonColorHelper.applyPrimaryColor(btnSetCalendarRange, color);
        ButtonColorHelper.applyPrimaryColor(btnBackupManage, color);
        ButtonColorHelper.applyPrimaryColor(btnUpdateApp, color);
        ButtonColorHelper.applyPrimaryColor(btnWelcomeTour, color);
        ButtonColorHelper.applyPrimaryColor(btnSetNotificationTimes, color);
        ButtonColorHelper.applyPrimaryColor(btnSetLanguage, color);
        ButtonColorHelper.applyPrimaryColor(btnSetButtonColor, color);
        ButtonColorHelper.applyPrimaryColor(btnSetCircleColor, color);
        ButtonColorHelper.applyPrimaryColor(btnSetCircleStyle, color);
    }

    private void applyDialogButtonColors(@Nullable AlertDialog dialog) {
        if (dialog == null || viewModel == null) {
            return;
        }
        Integer color = viewModel.getButtonColor().getValue();
        if (color == null) {
            return;
        }
        Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (positive != null) {
            positive.setTextColor(color);
        }
        Button negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (negative != null) {
            negative.setTextColor(color);
        }
        Button neutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        if (neutral != null) {
            neutral.setTextColor(color);
        }
    }

    private void toggleAdvancedSection() {
        boolean isVisible = advancedContent.getVisibility() == View.VISIBLE;
        advancedContent.setVisibility(isVisible ? View.GONE : View.VISIBLE);
        btnAdvancedToggle.animate()
                .rotation(isVisible ? 0f : 180f)
                .setDuration(150)
                .start();
    }

    private void updateButtonColorButtonText(int color) {
        btnSetButtonColor.setText(R.string.settings_button_color_format);
    }

    private void updateCircleColorButtonText(int color) {
        btnSetCircleColor.setText(R.string.settings_circle_color_format);
    }

    private void updateCircleStyleButtonText(int style) {
        ensureCircleStyleOptionsLoaded();
        String label = style >= 0 && style < circleStyleLabels.length
                ? circleStyleLabels[style]
                : circleStyleLabels[0];
        btnSetCircleStyle.setText(getString(R.string.settings_circle_style_format, label));
    }

    private void ensureCircleStyleOptionsLoaded() {
        if (circleStyleLabels == null) {
            circleStyleLabels = getResources().getStringArray(R.array.settings_circle_style_labels);
            circleStylePreviews = new Drawable[circleStyleLabels.length];
        }
    }

    private void showCircleStyleDialog() {
        ensureCircleStyleOptionsLoaded();
        Integer currentStyle = viewModel.getHomeCircleStyle().getValue();
        int selected = currentStyle != null ? currentStyle : SettingsRepository.DEFAULT_HOME_CIRCLE_STYLE;
        if (selected >= circleStyleLabels.length) {
            selected = 0;
        }
        android.widget.ListAdapter adapter = new android.widget.ArrayAdapter<String>(
                requireContext(),
                R.layout.item_circle_style_preview,
                android.R.id.text1,
                circleStyleLabels
        ) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                android.widget.ImageView preview = view.findViewById(R.id.img_style_preview);
                if (preview != null) {
                    preview.setImageDrawable(getCircleStylePreview(position));
                }
                return view;
            }
        };
        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_circle_style_list, null);
        android.widget.ListView listView = content.findViewById(R.id.list_circle_styles);
        listView.setChoiceMode(android.widget.ListView.CHOICE_MODE_SINGLE);
        listView.setAdapter(adapter);
        listView.setItemChecked(selected, true);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_circle_style_dialog_title)
                .setView(content)
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
        listView.setOnItemClickListener((parent, view, position, id) -> {
            viewModel.setHomeCircleStyle(position);
            WidgetUpdater.updateAllWidgets(requireContext());
            dialog.dismiss();
        });
        applyDialogButtonColors(dialog);
    }

    private Drawable getCircleStylePreview(int style) {
        if (circleStylePreviews != null && style >= 0 && style < circleStylePreviews.length) {
            if (circleStylePreviews[style] != null) {
                return circleStylePreviews[style];
            }
            int color = viewModel != null && viewModel.getHomeCircleColor().getValue() != null
                    ? viewModel.getHomeCircleColor().getValue()
                    : SettingsRepository.DEFAULT_HOME_CIRCLE_COLOR;
            circleStylePreviews[style] = buildCircleStylePreview(style, color);
            return circleStylePreviews[style];
        }
        return null;
    }

    private Drawable buildCircleStylePreview(int style, int color) {
        int size = dpToPx(36);
        int padding = dpToPx(4);
        int stroke = dpToPx(3);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint prog = new Paint(Paint.ANTI_ALIAS_FLAG);
        track.setStyle(Paint.Style.STROKE);
        prog.setStyle(Paint.Style.STROKE);
        track.setStrokeWidth(stroke);
        prog.setStrokeWidth(stroke);
        track.setColor(withAlpha(color, 80));
        prog.setColor(color);
        track.setStrokeCap(Paint.Cap.ROUND);
        prog.setStrokeCap(Paint.Cap.ROUND);

        RectF rect = new RectF(padding, padding, size - padding, size - padding);
        float sweep = 270f;

        if (style == HomeCircleView.STYLE_ARC) {
            canvas.drawArc(rect, -90f, sweep, false, prog);
            return new BitmapDrawable(getResources(), bitmap);
        }

        if (style == HomeCircleView.STYLE_SEGMENTED) {
            int segments = 10;
            float gap = 360f / segments * 0.55f;
            float segSweep = 360f / segments - gap;
            float start = -90f;
            for (int i = 0; i < segments; i++) {
                float segStart = start + i * (segSweep + gap);
                canvas.drawArc(rect, segStart, segSweep, false, track);
            }
            int filled = 6;
            for (int i = 0; i < filled; i++) {
                float segStart = start + i * (segSweep + gap);
                canvas.drawArc(rect, segStart, segSweep, false, prog);
            }
            return new BitmapDrawable(getResources(), bitmap);
        }

        if (style == HomeCircleView.STYLE_GRADIENT || style == HomeCircleView.STYLE_GRADIENT_GLOW) {
            Shader shader = new SweepGradient(size / 2f, size / 2f,
                    new int[]{withAlpha(color, 90), color, withAlpha(color, 90)},
                    new float[]{0f, 0.7f, 1f});
            prog.setShader(shader);
        }
        if (style == HomeCircleView.STYLE_GLOW || style == HomeCircleView.STYLE_GRADIENT_GLOW) {
            prog.setShadowLayer(dpToPx(4), 0f, 0f, withAlpha(color, 180));
        }
        if (style == HomeCircleView.STYLE_PULSE_LIGHT) {
            prog.setShadowLayer(dpToPx(3), 0f, 0f, withAlpha(color, 140));
        } else if (style == HomeCircleView.STYLE_PULSE_MEDIUM) {
            prog.setShadowLayer(dpToPx(5), 0f, 0f, withAlpha(color, 180));
        } else if (style == HomeCircleView.STYLE_PULSE_STRONG) {
            prog.setShadowLayer(dpToPx(7), 0f, 0f, withAlpha(color, 220));
        } else if (style == HomeCircleView.STYLE_HALO) {
            track.setColor(withAlpha(color, 120));
        } else if (style == HomeCircleView.STYLE_THIN) {
            track.setStrokeWidth(dpToPx(2));
            prog.setStrokeWidth(dpToPx(2));
        }

        canvas.drawArc(rect, 0f, 360f, false, track);
        canvas.drawArc(rect, -90f, sweep, false, prog);

        if (style == HomeCircleView.STYLE_MARKER) {
            Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
            dot.setColor(color);
            float angle = (float) Math.toRadians(-90 + sweep);
            float cx = rect.centerX();
            float cy = rect.centerY();
            float r = rect.width() / 2f;
            float x = cx + (float) Math.cos(angle) * r;
            float y = cy + (float) Math.sin(angle) * r;
            canvas.drawCircle(x, y, dpToPx(3), dot);
        }

        return new BitmapDrawable(getResources(), bitmap);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private int withAlpha(int color, int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        return (color & 0x00FFFFFF) | (clamped << 24);
    }

    private void ensureButtonColorOptionsLoaded() {
        if (buttonColorValues == null || buttonColorLabels == null) {
            buttonColorValues = getResources().getIntArray(R.array.settings_button_color_values);
            buttonColorLabels = getResources().getStringArray(R.array.settings_button_color_labels);
        }
    }

    private int getButtonColorIndex(int color) {
        ensureButtonColorOptionsLoaded();
        for (int i = 0; i < buttonColorValues.length; i++) {
            if (buttonColorValues[i] == color) {
                return i;
            }
        }
        return 0;
    }

    private String getButtonColorLabel(int color) {
        int index = getButtonColorIndex(color);
        if (index >= 0 && index < buttonColorLabels.length) {
            return buttonColorLabels[index];
        }
        return getString(R.string.settings_button_color_custom);
    }

    private String unitLabel(String unit) {
        if ("years".equals(unit)) {
            return getString(R.string.settings_unit_year_short);
        }
        return getString(R.string.settings_unit_month_short);
    }

    private void checkForUpdates() {
        Toast.makeText(requireContext(), R.string.update_checking_toast, Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                ReleaseInfo releaseInfo = fetchLatestReleaseInfo();
                if (releaseInfo == null) {
                    showToast(R.string.update_failed_toast);
                    return;
                }
                if (releaseInfo.downloadUrl == null) {
                    showToast(R.string.update_no_apk_toast);
                    return;
                }
                String currentVersion = getCurrentVersionName();
                int compare = compareVersions(currentVersion, releaseInfo.versionName);
                if (compare >= 0) {
                    showToast(R.string.update_latest_toast);
                    return;
                }
                showUpdateConfirmDialog(releaseInfo);
            } catch (Exception e) {
                showToast(R.string.update_failed_toast);
            }
        }).start();
    }

    private void showToast(int messageResId) {
        if (!isAdded()) {
            return;
        }
        requireActivity().runOnUiThread(() ->
                Toast.makeText(requireContext(), messageResId, Toast.LENGTH_SHORT).show()
        );
    }

    private void showUpdateConfirmDialog(ReleaseInfo releaseInfo) {
        if (!isAdded()) {
            return;
        }
        requireActivity().runOnUiThread(() -> {
            String version = releaseInfo.versionName != null ? releaseInfo.versionName : "";
            String message = getString(R.string.update_available_message, version);
            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.update_available_title)
                    .setMessage(message)
                    .setPositiveButton(R.string.update_install, (dlg, which) -> downloadAndInstall(releaseInfo))
                    .setNegativeButton(R.string.update_later, null)
                    .show();
            applyDialogButtonColors(dialog);
        });
    }

    private void downloadAndInstall(ReleaseInfo releaseInfo) {
        showDownloadDialog();
        new Thread(() -> {
            File apkFile = downloadApk(releaseInfo.downloadUrl, releaseInfo.versionName, this::updateDownloadProgress);
            dismissDownloadDialog();
            if (apkFile == null) {
                showToast(R.string.update_download_failed_toast);
                return;
            }
            promptInstall(apkFile);
        }).start();
    }

    private void showDownloadDialog() {
        if (!isAdded()) {
            return;
        }
        requireActivity().runOnUiThread(() -> {
            if (downloadDialog != null && downloadDialog.isShowing()) {
                return;
            }
            int padding = (int) (16 * getResources().getDisplayMetrics().density);
            LinearLayout container = new LinearLayout(requireContext());
            container.setOrientation(LinearLayout.VERTICAL);
            container.setPadding(padding, padding, padding, padding);

            downloadProgressBar = new ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal);
            downloadProgressBar.setIndeterminate(true);
            downloadProgressBar.setMax(100);

            downloadProgressText = new TextView(requireContext());
            downloadProgressText.setText("0%");
            downloadProgressText.setPadding(0, padding / 2, 0, 0);

            container.addView(downloadProgressBar);
            container.addView(downloadProgressText);

            downloadDialog = new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.update_download_title)
                    .setView(container)
                    .setCancelable(false)
                    .create();
            downloadDialog.show();
        });
    }

    private void dismissDownloadDialog() {
        if (!isAdded()) {
            return;
        }
        requireActivity().runOnUiThread(() -> {
            if (downloadDialog != null) {
                downloadDialog.dismiss();
                downloadDialog = null;
            }
        });
    }

    private void updateDownloadProgress(int percent) {
        if (!isAdded()) {
            return;
        }
        requireActivity().runOnUiThread(() -> {
            if (downloadProgressBar == null || downloadProgressText == null) {
                return;
            }
            if (percent < 0) {
                downloadProgressBar.setIndeterminate(true);
                downloadProgressText.setText("");
            } else {
                downloadProgressBar.setIndeterminate(false);
                downloadProgressBar.setProgress(percent);
                downloadProgressText.setText(percent + "%");
            }
        });
    }

    private ReleaseInfo fetchLatestReleaseInfo() throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(RELEASES_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("User-Agent", "Veri-Aristo-App");
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                return null;
            }
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }
            }

            JsonArray releases = JsonParser.parseString(body.toString()).getAsJsonArray();
            for (JsonElement element : releases) {
                JsonObject release = element.getAsJsonObject();
                boolean isDraft = release.get("draft").getAsBoolean();
                boolean isPrerelease = release.get("prerelease").getAsBoolean();
                if (isDraft || isPrerelease) {
                    continue;
                }
                String tag = release.has("tag_name") && !release.get("tag_name").isJsonNull()
                        ? release.get("tag_name").getAsString()
                        : null;
                String versionName = normalizeVersion(tag);
                JsonArray assets = release.getAsJsonArray("assets");
                String downloadUrl = findApkAssetUrl(assets);
                return new ReleaseInfo(versionName, downloadUrl);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }

    private String findApkAssetUrl(JsonArray assets) {
        if (assets == null) {
            return null;
        }
        for (JsonElement assetElement : assets) {
            JsonObject asset = assetElement.getAsJsonObject();
            String name = asset.has("name") && !asset.get("name").isJsonNull()
                    ? asset.get("name").getAsString()
                    : "";
            if (name.toLowerCase(Locale.US).endsWith(".apk")) {
                return asset.get("browser_download_url").getAsString();
            }
        }
        return null;
    }

    private interface ProgressListener {
        void onProgress(int percent);
    }

    private File downloadApk(String downloadUrl, String versionName, ProgressListener progressListener) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(downloadUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(20000);
            connection.setRequestProperty("User-Agent", "Veri-Aristo-App");
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            int totalBytes = connection.getContentLength();
            if (progressListener != null) {
                progressListener.onProgress(totalBytes > 0 ? 0 : -1);
            }
            File target = new File(requireContext().getCacheDir(),
                    "veri_aristo_update_" + (versionName != null ? versionName : "latest") + ".apk");
            try (InputStream inputStream = connection.getInputStream();
                 OutputStream outputStream = new FileOutputStream(target)) {
                byte[] buffer = new byte[8192];
                int read;
                long downloaded = 0;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                    downloaded += read;
                    if (progressListener != null && totalBytes > 0) {
                        int percent = (int) ((downloaded * 100) / totalBytes);
                        progressListener.onProgress(percent);
                    }
                }
                outputStream.flush();
            }
            return target;
        } catch (IOException e) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void promptInstall(File apkFile) {
        if (!isAdded()) {
            return;
        }
        pendingApkFile = apkFile;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!requireContext().getPackageManager().canRequestPackageInstalls()) {
                showToast(R.string.update_install_prompt_toast);
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + requireContext().getPackageName()));
                manageUnknownSourcesLauncher.launch(intent);
                return;
            }
        }
        Uri apkUri = FileProvider.getUriForFile(requireContext(),
                requireContext().getPackageName() + ".fileprovider", apkFile);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
        pendingApkFile = null;
    }

    private String normalizeVersion(String tag) {
        if (tag == null) {
            return null;
        }
        String trimmed = tag.trim();
        if (trimmed.startsWith("v") || trimmed.startsWith("V")) {
            return trimmed.substring(1);
        }
        return trimmed;
    }

    private int compareVersions(String current, String latest) {
        if (latest == null) {
            return 0;
        }
        int[] currentParts = parseVersionParts(current);
        int[] latestParts = parseVersionParts(latest);
        int max = Math.max(currentParts.length, latestParts.length);
        for (int i = 0; i < max; i++) {
            int c = i < currentParts.length ? currentParts[i] : 0;
            int l = i < latestParts.length ? latestParts[i] : 0;
            if (c != l) {
                return Integer.compare(c, l);
            }
        }
        return 0;
    }

    private int[] parseVersionParts(String version) {
        if (version == null || version.isEmpty()) {
            return new int[]{0};
        }
        String[] parts = version.split("\\.");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                result[i] = Integer.parseInt(parts[i].replaceAll("[^0-9]", ""));
            } catch (NumberFormatException e) {
                result[i] = 0;
            }
        }
        return result;
    }

    private static class ReleaseInfo {
        final String versionName;
        final String downloadUrl;

        ReleaseInfo(String versionName, String downloadUrl) {
            this.versionName = versionName;
            this.downloadUrl = downloadUrl;
        }
    }

    private void configureValuePicker(NumberPicker picker, int unitIndex, Integer currentValue) {
        int max = unitIndex == 1 ? 10 : 60;
        picker.setMinValue(0);
        picker.setMaxValue(max);
        int value = currentValue != null ? Math.min(currentValue, max) : 0;
        picker.setValue(value);
    }

    private String getCurrentVersionName() {
        try {
            android.content.pm.PackageManager packageManager = requireContext().getPackageManager();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                android.content.pm.PackageInfo info = packageManager.getPackageInfo(
                        requireContext().getPackageName(),
                        android.content.pm.PackageManager.PackageInfoFlags.of(0));
                return info.versionName;
            }
            android.content.pm.PackageInfo info = packageManager.getPackageInfo(
                    requireContext().getPackageName(), 0);
            return info.versionName;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return null;
        }
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
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_reset_title)
                .setMessage(R.string.settings_reset_message)
                .setPositiveButton(R.string.settings_reset_confirm, (dlg, which) -> resetAppData())
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
        applyDialogButtonColors(dialog);
    }

    private void showBackupDialog() {
        String[] options = {getString(R.string.backup_create), getString(R.string.backup_restore)};
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.backup_title)
                .setItems(options, (dlg, which) -> {
                    if (which == 0) {
                        createBackupLauncher.launch("veri_aristo_backup.json");
                    } else {
                        restoreBackupLauncher.launch(new String[]{"application/json"});
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
        applyDialogButtonColors(dialog);
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
        com.google.android.material.button.MaterialButton openEmail = content.findViewById(R.id.btn_open_email);
        com.google.android.material.button.MaterialButton openGithub = content.findViewById(R.id.btn_open_github);
        com.google.android.material.button.MaterialButton openCoffee = content.findViewById(R.id.btn_open_coffee);
        Integer buttonColor = viewModel.getButtonColor().getValue();
        if (buttonColor != null) {
            ButtonColorHelper.applyPrimaryColor(openEmail, buttonColor);
            ButtonColorHelper.applyPrimaryColor(openGithub, buttonColor);
            ButtonColorHelper.applyPrimaryColor(openCoffee, buttonColor);
        }

        appName.setText(R.string.app_info_name);
        appVersion.setText(getString(R.string.app_info_version, versionName));
        appDescription.setText(R.string.app_info_description);
        String developerLabel = getString(R.string.app_info_developer_label);
        String developerName = getString(R.string.app_info_developer_name);
        SpannableString developerText = new SpannableString(
                String.format(Locale.getDefault(), "%s %s", developerLabel, developerName));
        int labelEnd = developerLabel.length();
        int labelColor = buttonColor != null
                ? buttonColor
                : MaterialColors.getColor(appDeveloper, com.google.android.material.R.attr.colorPrimary);
        developerText.setSpan(new ForegroundColorSpan(labelColor), 0, labelEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        developerText.setSpan(new StyleSpan(Typeface.BOLD), 0, labelEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        appDeveloper.setText(developerText);

        openEmail.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:sichler.daniel@gmail.com"));
            startActivity(intent);
        });

        openGithub.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://linktr.ee/darexsh"));
            startActivity(intent);
        });

        openCoffee.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/darexsh"));
            startActivity(intent);
        });

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.app_info_title)
                .setView(content)
                .setPositiveButton(R.string.dialog_ok, null)
                .show();
        applyDialogButtonColors(dialog);
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
        viewModel.setButtonColor(SettingsRepository.DEFAULT_BUTTON_COLOR);
        viewModel.setHomeCircleColor(SettingsRepository.DEFAULT_HOME_CIRCLE_COLOR);
        viewModel.setHomeCircleStyle(SettingsRepository.DEFAULT_HOME_CIRCLE_STYLE);

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

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.debug_dialog_title)
                .setView(scrollView)
                .setPositiveButton(R.string.dialog_ok, null)
                .show();
        applyDialogButtonColors(dialog);
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
