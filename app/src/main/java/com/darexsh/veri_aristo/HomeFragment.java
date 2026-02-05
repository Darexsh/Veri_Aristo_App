package com.darexsh.veri_aristo;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

// HomeFragment displays the current cycle status and allows users to manage their cycle settings
public class HomeFragment extends Fragment {

    // Number of days after removal before the ring can be reinserted
    private static final int RING_FREE_DAYS = Constants.RING_FREE_DAYS;
    private SharedViewModel viewModel;
    private int currentCircleStyle = SettingsRepository.DEFAULT_HOME_CIRCLE_STYLE;
    private int currentCircleColor = SettingsRepository.DEFAULT_HOME_CIRCLE_COLOR;
    private float pulsePhase = 0f;
    private boolean pulseUp = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        final HomeCircleView circularProgress = view.findViewById(R.id.circularProgress); // Home circle for cycle status
        final TextView tvRemovalDate = view.findViewById(R.id.tv_removal_date); // TextView to display the removal date
        final TextView tvSecondaryDate = view.findViewById(R.id.tv_secondary_date);
        final TextView tvDaysNumber = view.findViewById(R.id.tv_days_number);   // TextView to display the number of days left
        final TextView tvDaysLabel = view.findViewById(R.id.tv_days_left_label);    // TextView to display the label for days left
        final ImageView backgroundImageView = view.findViewById(R.id.background_image); // ImageView for the background image
        final MaterialButton btnDelayCycle = view.findViewById(R.id.btn_delay_cycle);
        final android.widget.ImageButton btnDelayInfo = view.findViewById(R.id.btn_delay_info);

        SharedViewModelFactory factory = new SharedViewModelFactory(requireActivity().getApplication());
        viewModel = new ViewModelProvider(requireActivity(), factory).get(SharedViewModel.class);

        viewModel.getButtonColor().observe(getViewLifecycleOwner(), color -> {
            if (color != null) {
                ButtonColorHelper.applyPrimaryColor(btnDelayCycle, color);
                btnDelayInfo.setColorFilter(color);
            }
        });
        btnDelayInfo.setOnClickListener(v -> showDelayInfoDialog());
        viewModel.getHomeCircleColor().observe(getViewLifecycleOwner(), color -> {
            if (color != null) {
                currentCircleColor = color;
                circularProgress.setIndicatorColor(color);
                applyHomeCircleStyle(circularProgress, currentCircleStyle, currentCircleColor);
            }
        });

        viewModel.getHomeCircleStyle().observe(getViewLifecycleOwner(), style -> {
            if (style != null) {
                currentCircleStyle = style;
                applyHomeCircleStyle(circularProgress, currentCircleStyle, currentCircleColor);
            }
        });

        // Set background image if available
        Runnable loadBackgroundImage = () -> {
            String uriStr = viewModel.getBackgroundImageUri().getValue();

            if (uriStr != null) {
                try {
                    Uri uri = Uri.parse(uriStr);
                    String scheme = uri.getScheme();
                    if ("file".equalsIgnoreCase(scheme)) {
                        File file = new File(uri.getPath());
                        if (file.exists()) {
                            try (InputStream inputStream = new FileInputStream(file)) {
                                Drawable drawable = Drawable.createFromStream(inputStream, uri.toString());
                                backgroundImageView.setImageDrawable(drawable);
                            }
                        } else {
                            backgroundImageView.setImageResource(R.drawable.default_bg);
                            viewModel.setBackgroundImageUri(null);
                        }
                    } else {
                        boolean hasPermission = false;
                        for (UriPermission perm : requireContext().getContentResolver().getPersistedUriPermissions()) {
                            if (perm.getUri().equals(uri) && perm.isReadPermission()) {
                                hasPermission = true;
                                break;
                            }
                        }

                        if (hasPermission) {
                            try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri)) {
                                Drawable drawable = Drawable.createFromStream(inputStream, uri.toString());
                                backgroundImageView.setImageDrawable(drawable);
                            }
                        } else {
                            backgroundImageView.setImageResource(R.drawable.default_bg);
                            viewModel.setBackgroundImageUri(null);
                        }
                    }

                } catch (SecurityException | IOException e) {
                    e.printStackTrace();
                    backgroundImageView.setImageResource(R.drawable.default_bg);
                    viewModel.setBackgroundImageUri(null);
                }
            } else {
                backgroundImageView.setImageResource(R.drawable.default_bg);
            }
        };

        // Observe changes in the ViewModel
        viewModel.getBackgroundImageUri().observe(getViewLifecycleOwner(), uri -> loadBackgroundImage.run());

        // Observe changes in the ViewModel and update the UI accordingly
        Runnable updateUi = () -> {
            Calendar vmStartDate = viewModel.getStartDate().getValue();
            Integer cycleLength = viewModel.getCycleLength().getValue();

            if (vmStartDate == null || cycleLength == null) {
                // Data not yet loaded
                return;
            }

            // Initialize calendar instances for cycle calculations
            Calendar displayNow = DebugTimeProvider.now(viewModel.getRepository());
            Calendar systemNow = Calendar.getInstance();
            Calendar startDate = (Calendar) vmStartDate.clone();
            startDate.set(Calendar.SECOND, 0);
            startDate.set(Calendar.MILLISECOND, 0);

            int delayDays = viewModel.getRepository().getCycleDelayDays(startDate.getTimeInMillis());
            Calendar removalDate = (Calendar) startDate.clone();
            removalDate.add(Calendar.DAY_OF_MONTH, cycleLength + delayDays);

            Calendar reinsertionDate = (Calendar) removalDate.clone();
            reinsertionDate.add(Calendar.DAY_OF_MONTH, RING_FREE_DAYS);

            // Retrieve cycle history from preferences
            List<Cycle> cycleHistory = viewModel.getRepository().getCycleHistory();
            Calendar tempStartDate = (Calendar) startDate.clone();
            Calendar tempRemovalDate = (Calendar) removalDate.clone();
            Calendar tempReinsertionDate = (Calendar) reinsertionDate.clone();

            Calendar lastSavedReinsertionDate = getLastSavedReinsertionDate(cycleHistory);
            long lastSavedReinsertionMillis = lastSavedReinsertionDate != null ? lastSavedReinsertionDate.getTimeInMillis() : 0;

            int maxCycles = 100;
            int count = 0;

            // Calculate cycles until today if last reinsertion date is in the past
            while (tempReinsertionDate.getTimeInMillis() <= systemNow.getTimeInMillis() && count < maxCycles) {
                if (tempReinsertionDate.getTimeInMillis() > lastSavedReinsertionMillis) {
                    saveCycleToHistory(viewModel, tempStartDate.getTimeInMillis(), tempRemovalDate.getTimeInMillis(), CycleType.INSERTION);
                    saveCycleToHistory(viewModel, tempRemovalDate.getTimeInMillis(), tempReinsertionDate.getTimeInMillis(), CycleType.REMOVAL);
                }

                int tempDelayDays = viewModel.getRepository().getCycleDelayDays(tempStartDate.getTimeInMillis());
                tempStartDate.add(Calendar.DAY_OF_MONTH, cycleLength + RING_FREE_DAYS + tempDelayDays);
                tempRemovalDate = (Calendar) tempStartDate.clone();
                tempRemovalDate.add(Calendar.DAY_OF_MONTH, cycleLength + tempDelayDays);
                tempReinsertionDate = (Calendar) tempRemovalDate.clone();
                tempReinsertionDate.add(Calendar.DAY_OF_MONTH, RING_FREE_DAYS);
                count++;
            }

            Calendar nowDay = startOfDay(displayNow);
            Calendar reinsertionDay = startOfDay(reinsertionDate);

            // Adjust start date to the current cycle window (day-based to avoid skipping the current day)
            while (nowDay.after(reinsertionDay)) {
                startDate.add(Calendar.DAY_OF_MONTH, cycleLength + RING_FREE_DAYS + delayDays);
                delayDays = viewModel.getRepository().getCycleDelayDays(startDate.getTimeInMillis());
                removalDate = (Calendar) startDate.clone();
                removalDate.add(Calendar.DAY_OF_MONTH, cycleLength + delayDays);
                reinsertionDate = (Calendar) removalDate.clone();
                reinsertionDate.add(Calendar.DAY_OF_MONTH, RING_FREE_DAYS);
                reinsertionDay = startOfDay(reinsertionDate);
            }

            int remainingDays;
            String labelText;
            String primaryTextDate;
            String secondaryTextDate;
            int progressMax;
            int progressValue;

            // Calculate remaining days and progress based on day changes (midnight), except at removal/insertion time
            if (displayNow.before(removalDate)) {
                remainingDays = daysBetweenDays(displayNow, removalDate);

                labelText = getString(R.string.home_days_left);
                progressMax = cycleLength;
                progressValue = progressMax - remainingDays;

                String removalDateText = String.format("%02d.%02d.%d",
                        removalDate.get(Calendar.DAY_OF_MONTH),
                        removalDate.get(Calendar.MONTH) + 1,
                        removalDate.get(Calendar.YEAR));
                String reinsertionDateText = String.format("%02d.%02d.%d",
                        reinsertionDate.get(Calendar.DAY_OF_MONTH),
                        reinsertionDate.get(Calendar.MONTH) + 1,
                        reinsertionDate.get(Calendar.YEAR));
                primaryTextDate = getString(R.string.home_removal_on, removalDateText);
                secondaryTextDate = getString(R.string.home_insertion_on, reinsertionDateText);
            } else if (displayNow.before(reinsertionDate)) {
                remainingDays = daysBetweenDays(displayNow, reinsertionDate);

                labelText = getString(R.string.home_days_until_insertion);
                progressMax = RING_FREE_DAYS;
                progressValue = progressMax - remainingDays;

                String reinsertionDateText = String.format("%02d.%02d.%d",
                        reinsertionDate.get(Calendar.DAY_OF_MONTH),
                        reinsertionDate.get(Calendar.MONTH) + 1,
                        reinsertionDate.get(Calendar.YEAR));
                String removalDateText = String.format("%02d.%02d.%d",
                        removalDate.get(Calendar.DAY_OF_MONTH),
                        removalDate.get(Calendar.MONTH) + 1,
                        removalDate.get(Calendar.YEAR));
                primaryTextDate = getString(R.string.home_insertion_on, reinsertionDateText);
                secondaryTextDate = getString(R.string.home_removal_on, removalDateText);
            } else {
                remainingDays = cycleLength;
                labelText = getString(R.string.home_days_left);
                progressMax = cycleLength;
                progressValue = 0;

                String removalDateText = String.format("%02d.%02d.%d",
                        removalDate.get(Calendar.DAY_OF_MONTH),
                        removalDate.get(Calendar.MONTH) + 1,
                        removalDate.get(Calendar.YEAR));
                String reinsertionDateText = String.format("%02d.%02d.%d",
                        reinsertionDate.get(Calendar.DAY_OF_MONTH),
                        reinsertionDate.get(Calendar.MONTH) + 1,
                        reinsertionDate.get(Calendar.YEAR));
                primaryTextDate = getString(R.string.home_removal_on, removalDateText);
                secondaryTextDate = getString(R.string.home_insertion_on, reinsertionDateText);
            }

            circularProgress.setMax(progressMax);
            circularProgress.setProgress(progressValue);
            tvDaysNumber.setText(String.valueOf(remainingDays));
            tvDaysLabel.setText(labelText);
            String timeText = String.format("%02d:%02d", startDate.get(Calendar.HOUR_OF_DAY), startDate.get(Calendar.MINUTE));
            String timeSuffix = getString(R.string.home_at_time, timeText);
            primaryTextDate = primaryTextDate + " " + timeSuffix;
            secondaryTextDate = secondaryTextDate + " " + timeSuffix;
            tvRemovalDate.setText(primaryTextDate);
            tvSecondaryDate.setText(secondaryTextDate);

            long cycleStartMillis = startDate.getTimeInMillis();
            int settingsHash = viewModel.getRepository().getNotificationSettingsHash();
            int scheduledHash = viewModel.getRepository().getNotificationSettingsHashForCycle(cycleStartMillis);
            if (systemNow.before(reinsertionDate)
                    && (!viewModel.getRepository().wasNotificationScheduledForCycle(cycleStartMillis)
                    || scheduledHash != settingsHash)) {
                scheduleRingCycleNotifications(
                        viewModel,
                        (Calendar) startDate.clone(),
                        (Calendar) removalDate.clone(),
                        (Calendar) reinsertionDate.clone(),
                        cycleLength
                );
                viewModel.getRepository().setNotificationScheduledForCycle(cycleStartMillis);
                viewModel.getRepository().setNotificationSettingsHashForCycle(cycleStartMillis, settingsHash);
            }
        };

        // Observe changes in the ViewModel and update the UI
        viewModel.getStartDate().observe(getViewLifecycleOwner(), val -> updateUi.run());
        viewModel.getCycleLength().observe(getViewLifecycleOwner(), val -> updateUi.run());
        viewModel.getRemovalReminderHours().observe(getViewLifecycleOwner(), val -> updateUi.run());
        viewModel.getInsertionReminderHours().observe(getViewLifecycleOwner(), val -> updateUi.run());

        btnDelayCycle.setOnClickListener(v -> showDelayDialog(viewModel));

        updateUi.run();
        return view;
    }

    private void showDelayInfoDialog() {
        Spanned message = Html.fromHtml(
                getString(R.string.home_delay_info_message),
                Html.FROM_HTML_MODE_LEGACY
        );
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.home_delay_info_title)
                .setMessage(message)
                .setPositiveButton(R.string.dialog_ok, null)
                .show();
    }

    // Save a cycle event to the history
    private void saveCycleToHistory(SharedViewModel viewModel, long dateMillis, long endDateMillis, CycleType type) {
        List<Cycle> cycleHistory = viewModel.getRepository().getCycleHistory();

        for (Cycle c : cycleHistory) {
            if (c.getDateMillis() == dateMillis
                    && c.getEndDateMillis() == endDateMillis
                    && c.getType() == type) {
                return; // Duplikat
            }
        }

        cycleHistory.add(new Cycle(dateMillis, endDateMillis, type));
        viewModel.getRepository().saveCycleHistory(cycleHistory);
    }

    // Get the last saved reinsertion date from the cycle history
    private Calendar getLastSavedReinsertionDate(List<Cycle> cycleHistory) {
        long latestRemovalDate = 0;
        for (Cycle cycle : cycleHistory) {
            if (CycleType.REMOVAL == cycle.getType() && cycle.getDateMillis() > latestRemovalDate) {
                latestRemovalDate = cycle.getDateMillis();
            }
        }
        if (latestRemovalDate == 0) return null;

        Calendar reinsertionDate = Calendar.getInstance();
        reinsertionDate.setTimeInMillis(latestRemovalDate);
        reinsertionDate.add(Calendar.DAY_OF_MONTH, RING_FREE_DAYS);
        return reinsertionDate;
    }

    // Schedule notifications for the ring cycle events
    private void scheduleRingCycleNotifications(SharedViewModel viewModel, Calendar startDate, Calendar removalDate,
                                                Calendar reinsertionDate, int cycleLength) {
        int hour = startDate.get(Calendar.HOUR_OF_DAY);
        int minute = startDate.get(Calendar.MINUTE);

        // Cancel previous notifications for this cycle to prevent duplicates
        cancelNotificationsForCycle(startDate, removalDate, reinsertionDate);

        // ---- Cycle Progress Notifications ----
        Calendar twoWeeksRemaining = (Calendar) removalDate.clone();
        twoWeeksRemaining.add(Calendar.DAY_OF_MONTH, -14);
        twoWeeksRemaining.set(Calendar.HOUR_OF_DAY, hour);
        twoWeeksRemaining.set(Calendar.MINUTE, minute);
        if (cycleLength >= 14) {
            scheduleNotification(twoWeeksRemaining,
                    getString(R.string.notif_cycle_duration_title),
                    getString(R.string.notif_two_weeks_remaining),
                    0);
        }

        Calendar oneWeekRemaining = (Calendar) removalDate.clone();
        oneWeekRemaining.add(Calendar.DAY_OF_MONTH, -7);
        oneWeekRemaining.set(Calendar.HOUR_OF_DAY, hour);
        oneWeekRemaining.set(Calendar.MINUTE, minute);
        if (cycleLength >= 7) {
            scheduleNotification(oneWeekRemaining,
                    getString(R.string.notif_cycle_duration_title),
                    getString(R.string.notif_one_week_remaining),
                    1);
        }

        // ---- Ring Removal Notifications ----
        int removalReminderHours = viewModel.getRepository().getRemovalReminderHours();
        if (removalReminderHours > 0) {
            Calendar removalReminder = (Calendar) removalDate.clone();
            removalReminder.add(Calendar.HOUR_OF_DAY, -removalReminderHours);
            scheduleNotification(removalReminder,
                    getString(R.string.notif_remove_title),
                    getString(R.string.notif_remove_in_hours, removalReminderHours),
                    2);
        }

        Calendar removalExact = (Calendar) removalDate.clone();
        removalExact.set(Calendar.HOUR_OF_DAY, hour);
        removalExact.set(Calendar.MINUTE, minute);
        String removalTimeText = String.format("%02d:%02d", hour, minute);
        scheduleNotification(removalExact,
                getString(R.string.notif_remove_title),
                getString(R.string.notif_remove_now, removalTimeText),
                3);

        // ---- Ring Insertion Notifications ----
        int insertionReminderHours = viewModel.getRepository().getInsertionReminderHours();
        if (insertionReminderHours > 0) {
            Calendar reinsertionReminder = (Calendar) reinsertionDate.clone();
            reinsertionReminder.add(Calendar.HOUR_OF_DAY, -insertionReminderHours);
            scheduleNotification(reinsertionReminder,
                    getString(R.string.notif_insert_title),
                    getString(R.string.notif_insert_in_hours, insertionReminderHours),
                    4);
        }

        Calendar reinsertionExact = (Calendar) reinsertionDate.clone();
        reinsertionExact.set(Calendar.HOUR_OF_DAY, hour);
        reinsertionExact.set(Calendar.MINUTE, minute);
        String reinsertionTimeText = String.format("%02d:%02d", hour, minute);
        scheduleNotification(reinsertionExact,
                getString(R.string.notif_insert_title),
                getString(R.string.notif_insert_now, reinsertionTimeText),
                5);
    }

    // Schedule a single notification with a unique ID
    private void scheduleNotification(Calendar calendar, String title, String message, int uniqueIdOffset) {
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            return;
        }
        Intent intent = new Intent(requireContext(), NotificationReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("message", message);

        // Generate a unique request code using milliseconds and an offset
        int requestCode = (int) ((calendar.getTimeInMillis() / 1000) % Integer.MAX_VALUE) + uniqueIdOffset;

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Schedule exact alarm with AlarmManager
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }

    // Cancel all notifications for a specific cycle to avoid duplicates
    private void cancelNotificationsForCycle(Calendar startDate, Calendar removalDate, Calendar reinsertionDate) {
        Intent intent = new Intent(requireContext(), NotificationReceiver.class);
        List<Calendar> targets = new ArrayList<>();

        Calendar twoWeeksRemaining = (Calendar) removalDate.clone();
        twoWeeksRemaining.add(Calendar.DAY_OF_MONTH, -14);
        targets.add(twoWeeksRemaining);

        Calendar oneWeekRemaining = (Calendar) removalDate.clone();
        oneWeekRemaining.add(Calendar.DAY_OF_MONTH, -7);
        targets.add(oneWeekRemaining);

        int removalReminderHours = viewModel.getRepository().getRemovalReminderHours();
        if (removalReminderHours > 0) {
            Calendar removalReminder = (Calendar) removalDate.clone();
            removalReminder.add(Calendar.HOUR_OF_DAY, -removalReminderHours);
            targets.add(removalReminder);
        }

        targets.add((Calendar) removalDate.clone());

        int insertionReminderHours = viewModel.getRepository().getInsertionReminderHours();
        if (insertionReminderHours > 0) {
            Calendar reinsertionReminder = (Calendar) reinsertionDate.clone();
            reinsertionReminder.add(Calendar.HOUR_OF_DAY, -insertionReminderHours);
            targets.add(reinsertionReminder);
        }

        targets.add((Calendar) reinsertionDate.clone());

        for (int i = 0; i < targets.size(); i++) {
            long triggerTime = targets.get(i).getTimeInMillis();
            int requestCode = (int) ((triggerTime / 1000) % Integer.MAX_VALUE) + i;

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    requireContext(),
                    requestCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );

            if (pendingIntent != null) {
                AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
            }
        }
    }

    private void showDelayDialog(SharedViewModel viewModel) {
        NumberPicker picker = new NumberPicker(requireContext());
        picker.setMinValue(0);
        picker.setMaxValue(21);
        int currentDelay = viewModel.getRepository()
                .getCycleDelayDays(viewModel.getStartDate().getValue().getTimeInMillis());
        if (currentDelay <= 0) {
            picker.setValue(7);
        } else {
            picker.setValue(Math.min(21, Math.max(0, currentDelay)));
        }

        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);
        layout.addView(picker);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.home_delay_title)
                .setMessage(R.string.home_delay_message)
                .setView(layout)
                .setPositiveButton(R.string.home_delay_confirm, (dialog, which) -> {
                    Calendar baseStart = viewModel.getStartDate().getValue();
                    Integer cycleLength = viewModel.getCycleLength().getValue();
                    if (baseStart == null || cycleLength == null) {
                        return;
                    }

                    Calendar now = Calendar.getInstance();
                    Calendar currentStart = (Calendar) baseStart.clone();
                    currentStart.set(Calendar.SECOND, 0);
                    currentStart.set(Calendar.MILLISECOND, 0);
                    int delayDays = viewModel.getRepository().getCycleDelayDays(currentStart.getTimeInMillis());
                    Calendar removalDate = (Calendar) currentStart.clone();
                    removalDate.add(Calendar.DAY_OF_MONTH, cycleLength + delayDays);
                    Calendar reinsertionDate = (Calendar) removalDate.clone();
                    reinsertionDate.add(Calendar.DAY_OF_MONTH, RING_FREE_DAYS);

                    while (now.after(reinsertionDate)) {
                        currentStart.add(Calendar.DAY_OF_MONTH, cycleLength + RING_FREE_DAYS + delayDays);
                        delayDays = viewModel.getRepository().getCycleDelayDays(currentStart.getTimeInMillis());
                        removalDate = (Calendar) currentStart.clone();
                        removalDate.add(Calendar.DAY_OF_MONTH, cycleLength + delayDays);
                        reinsertionDate = (Calendar) removalDate.clone();
                        reinsertionDate.add(Calendar.DAY_OF_MONTH, RING_FREE_DAYS);
                    }

                    long cycleStartMillis = currentStart.getTimeInMillis();
                    int previousDelay = viewModel.getRepository().getCycleDelayDays(cycleStartMillis);
                    Calendar oldRemoval = (Calendar) currentStart.clone();
                    oldRemoval.add(Calendar.DAY_OF_MONTH, cycleLength + previousDelay);
                    Calendar oldReinsertion = (Calendar) oldRemoval.clone();
                    oldReinsertion.add(Calendar.DAY_OF_MONTH, RING_FREE_DAYS);
                    cancelNotificationsForCycle(currentStart, oldRemoval, oldReinsertion);

                    viewModel.getRepository().setCycleDelayDays(cycleStartMillis, picker.getValue());
                    viewModel.getRepository().clearNotificationScheduledForCycle(cycleStartMillis);

                    int newDelayDays = picker.getValue();
                    Calendar newRemoval = (Calendar) currentStart.clone();
                    newRemoval.add(Calendar.DAY_OF_MONTH, cycleLength + newDelayDays);
                    Calendar newReinsertion = (Calendar) newRemoval.clone();
                    newReinsertion.add(Calendar.DAY_OF_MONTH, RING_FREE_DAYS);
                    if (now.before(newReinsertion)) {
                        scheduleRingCycleNotifications(
                                viewModel,
                                (Calendar) currentStart.clone(),
                                (Calendar) newRemoval.clone(),
                                (Calendar) newReinsertion.clone(),
                                cycleLength
                        );
                        viewModel.getRepository().setNotificationScheduledForCycle(cycleStartMillis);
                    viewModel.getRepository().setNotificationSettingsHashForCycle(
                            cycleStartMillis, viewModel.getRepository().getNotificationSettingsHash());
                    }

                    viewModel.setStartDate((Calendar) baseStart.clone());
                    WidgetUpdater.updateAllWidgets(requireContext());
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private static Calendar startOfDay(Calendar source) {
        Calendar day = (Calendar) source.clone();
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);
        return day;
    }

    private static int daysBetweenDays(Calendar from, Calendar to) {
        Calendar fromDay = startOfDay(from);
        Calendar toDay = startOfDay(to);
        long millisLeft = toDay.getTimeInMillis() - fromDay.getTimeInMillis();
        int days = (int) (millisLeft / (24L * 60L * 60L * 1000L));
        return Math.max(days, 0);
    }

    private void applyHomeCircleStyle(HomeCircleView progress, int style, int color) {
        if (progress == null) {
            return;
        }
        progress.setStyle(style);
        progress.setIndicatorColor(color);
        if (style == HomeCircleView.STYLE_PULSE_LIGHT
                || style == HomeCircleView.STYLE_PULSE_MEDIUM
                || style == HomeCircleView.STYLE_PULSE_STRONG) {
            startPulse(progress);
        } else {
            stopPulse();
        }
    }

    private void startPulse(HomeCircleView progress) {
        progress.removeCallbacks(pulseRunnable);
        progress.post(pulseRunnable);
    }

    private void stopPulse() {
        if (getView() != null) {
            getView().removeCallbacks(pulseRunnable);
        }
    }

    private final Runnable pulseRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentCircleStyle != HomeCircleView.STYLE_PULSE_LIGHT
                    && currentCircleStyle != HomeCircleView.STYLE_PULSE_MEDIUM
                    && currentCircleStyle != HomeCircleView.STYLE_PULSE_STRONG) {
                return;
            }
            pulsePhase += pulseUp ? 0.06f : -0.06f;
            if (pulsePhase >= 1f) {
                pulsePhase = 1f;
                pulseUp = false;
            } else if (pulsePhase <= 0f) {
                pulsePhase = 0f;
                pulseUp = true;
            }
            HomeCircleView circle = getView() != null ? getView().findViewById(R.id.circularProgress) : null;
            if (circle != null) {
                circle.setPulsePhase(pulsePhase);
                circle.postDelayed(this, 40);
            }
        }
    };

}
