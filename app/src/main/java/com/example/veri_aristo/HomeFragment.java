package com.example.veri_aristo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

// HomeFragment displays the current cycle status and allows users to manage their cycle settings
public class HomeFragment extends Fragment {

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_START_DAY = "start_day";
    private static final String KEY_START_MONTH = "start_month";
    private static final String KEY_START_YEAR = "start_year";
    private static final String KEY_SET_TIME_HOUR = "set_time_hour";
    private static final String KEY_SET_TIME_MINUTE = "set_time_minute";
    private static final String KEY_CYCLE_LENGTH = "cycle_length";
    private static final String KEY_CYCLE_HISTORY = "cycle_history";
    private static final String KEY_BACKGROUND_IMAGE_URI = "background_image_uri";

    // Number of days after removal before the ring can be reinserted
    private static final int RING_FREE_DAYS = 7;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        final CircularProgressIndicator circularProgress = view.findViewById(R.id.circularProgress); // Circular progress indicator for cycle status
        final TextView tvRemovalDate = view.findViewById(R.id.tv_removal_date); // TextView to display the removal date
        final TextView tvDaysNumber = view.findViewById(R.id.tv_days_number);   // TextView to display the number of days left
        final TextView tvDaysLabel = view.findViewById(R.id.tv_days_left_label);    // TextView to display the label for days left
        final ImageView backgroundImageView = view.findViewById(R.id.background_image); // ImageView for the background image

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, requireContext().MODE_PRIVATE);
        Calendar calendar = Calendar.getInstance();

        int defaultDay = prefs.getInt(KEY_START_DAY, calendar.get(Calendar.DAY_OF_MONTH));  // Default start day from preferences or current day
        int defaultMonth = prefs.getInt(KEY_START_MONTH, calendar.get(Calendar.MONTH));     // Default start month from preferences or current month
        int defaultYear = prefs.getInt(KEY_START_YEAR, calendar.get(Calendar.YEAR));        // Default start year from preferences or current year
        int defaultCycleLength = prefs.getInt(KEY_CYCLE_LENGTH, 21);                        // Default cycle length from preferences or 21 days
        int defaultHour = prefs.getInt(KEY_SET_TIME_HOUR, 18);                              // Default hour for notifications from preferences or 18:00
        int defaultMinute = prefs.getInt(KEY_SET_TIME_MINUTE, 0);                           // Default minute for notifications from preferences or 00 minutes
        String backgroundImageUri = prefs.getString(KEY_BACKGROUND_IMAGE_URI, null);        // Default background image URI from preferences or null

        SharedViewModel viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        // Set background image if available
        if (backgroundImageUri != null) {
            backgroundImageView.setImageURI(Uri.parse(backgroundImageUri));
        }

        // Observe changes in the ViewModel and update the UI accordingly
        Runnable updateUi = () -> {
            int day = viewModel.getStartDay().getValue() != null ? viewModel.getStartDay().getValue() : defaultDay;
            int month = viewModel.getStartMonth().getValue() != null ? viewModel.getStartMonth().getValue() : defaultMonth;
            int year = viewModel.getStartYear().getValue() != null ? viewModel.getStartYear().getValue() : defaultYear;
            int cycleLength = viewModel.getCycleLength().getValue() != null ? viewModel.getCycleLength().getValue() : defaultCycleLength;
            int hour = viewModel.getHour().getValue() != null ? viewModel.getHour().getValue() : defaultHour;
            int minute = viewModel.getMinute().getValue() != null ? viewModel.getMinute().getValue() : defaultMinute;
            String bgUri = viewModel.getBackgroundImageUri().getValue() != null ? viewModel.getBackgroundImageUri().getValue() : backgroundImageUri;

            // Update background image if available
            if (bgUri != null) {
                backgroundImageView.setImageURI(Uri.parse(bgUri));
            } else {
                backgroundImageView.setImageDrawable(null);
            }

            // Calculate the cycle dates based on the selected start date and cycle length
            Calendar now = Calendar.getInstance();
            Calendar startDate = Calendar.getInstance();
            startDate.set(year, month, day, hour, minute, 0);
            startDate.set(Calendar.MILLISECOND, 0);

            // If the start date is in the future, set it to today
            Calendar removalDate = (Calendar) startDate.clone();
            removalDate.add(Calendar.DAY_OF_MONTH, cycleLength);

            // Calculate the reinsertion date after the removal date
            Calendar reinsertionDate = (Calendar) removalDate.clone();
            reinsertionDate.add(Calendar.DAY_OF_MONTH, RING_FREE_DAYS);

            // If the start date is in the future, adjust it to today
            List<Cycle> cycleHistory = getCycleHistory(prefs);
            Calendar tempStartDate = (Calendar) startDate.clone();
            Calendar tempRemovalDate = (Calendar) removalDate.clone();
            Calendar tempReinsertionDate = (Calendar) reinsertionDate.clone();

            // Check if the start date is in the future
            Calendar lastSavedReinsertionDate = getLastSavedReinsertionDate(cycleHistory);
            long lastSavedReinsertionMillis = lastSavedReinsertionDate != null ? lastSavedReinsertionDate.getTimeInMillis() : 0;

            // If the last saved reinsertion date is in the future, calculate cycles until today
            int maxZyklen = 100;
            int count = 0;

            // Calculate cycles until today if the last saved reinsertion date is in the future
            while (tempReinsertionDate.getTimeInMillis() <= now.getTimeInMillis() && count < maxZyklen) {
                if (tempReinsertionDate.getTimeInMillis() > lastSavedReinsertionMillis) {
                    saveCycleToHistory(prefs, tempStartDate.getTimeInMillis(), tempRemovalDate.getTimeInMillis(), "insertion");
                    saveCycleToHistory(prefs, tempRemovalDate.getTimeInMillis(), tempReinsertionDate.getTimeInMillis(), "removal");
                }

                tempStartDate.add(Calendar.DAY_OF_MONTH, cycleLength + RING_FREE_DAYS);
                tempRemovalDate = (Calendar) tempStartDate.clone();
                tempRemovalDate.add(Calendar.DAY_OF_MONTH, cycleLength);
                tempReinsertionDate = (Calendar) tempRemovalDate.clone();
                tempReinsertionDate.add(Calendar.DAY_OF_MONTH, RING_FREE_DAYS);
                count++;
            }

            // If the start date is in the future, adjust it to today
            while (now.after(reinsertionDate)) {
                startDate.add(Calendar.DAY_OF_MONTH, cycleLength + RING_FREE_DAYS);
                removalDate = (Calendar) startDate.clone();
                removalDate.add(Calendar.DAY_OF_MONTH, cycleLength);
                reinsertionDate = (Calendar) removalDate.clone();
                reinsertionDate.add(Calendar.DAY_OF_MONTH, RING_FREE_DAYS);
            }

            int remainingDays;
            String labelText;
            String bottomTextDate;
            int maxProgress;
            int currentProgress;

            // Calculate remaining days and progress based on the current date and cycle dates
            if (now.before(removalDate)) {
                long millisLeft = removalDate.getTimeInMillis() - now.getTimeInMillis();
                remainingDays = (int) (millisLeft / (24 * 60 * 60 * 1000));
                remainingDays = Math.max(remainingDays, 0);

                labelText = "Tage verbleibend";
                maxProgress = cycleLength;
                currentProgress = maxProgress - remainingDays;

                bottomTextDate = "Entfernung am " + String.format("%02d.%02d.%d",
                        removalDate.get(Calendar.DAY_OF_MONTH),
                        removalDate.get(Calendar.MONTH) + 1,
                        removalDate.get(Calendar.YEAR));
            } else if (now.before(reinsertionDate)) {
                long millisLeft = reinsertionDate.getTimeInMillis() - now.getTimeInMillis();
                remainingDays = (int) (millisLeft / (24 * 60 * 60 * 1000));
                remainingDays = Math.max(remainingDays, 0);

                labelText = "Tage bis zum Einsetzen";
                maxProgress = RING_FREE_DAYS;
                currentProgress = maxProgress - remainingDays;

                bottomTextDate = "Einsetzen am " + String.format("%02d.%02d.%d",
                        reinsertionDate.get(Calendar.DAY_OF_MONTH),
                        reinsertionDate.get(Calendar.MONTH) + 1,
                        reinsertionDate.get(Calendar.YEAR));
            } else {
                remainingDays = cycleLength;
                labelText = "Tage verbleibend";
                maxProgress = cycleLength;
                currentProgress = 0;

                bottomTextDate = "Entfernung am " + String.format("%02d.%02d.%d",
                        removalDate.get(Calendar.DAY_OF_MONTH),
                        removalDate.get(Calendar.MONTH) + 1,
                        removalDate.get(Calendar.YEAR));
            }

            circularProgress.setMax(maxProgress);
            circularProgress.setProgress(currentProgress);
            tvDaysNumber.setText(String.valueOf(remainingDays));
            tvDaysLabel.setText(labelText);
            bottomTextDate += " um " + String.format("%02d:%02d", hour, minute) + " Uhr";
            tvRemovalDate.setText(bottomTextDate);
            if (now.before(reinsertionDate)) {
                scheduleRingCycleNotifications(
                        (Calendar) startDate.clone(),
                        (Calendar) removalDate.clone(),
                        (Calendar) reinsertionDate.clone()
                );
            }
        };

        // Observe changes in the ViewModel and update the UI
        viewModel.getStartDay().observe(getViewLifecycleOwner(), val -> updateUi.run());
        viewModel.getStartMonth().observe(getViewLifecycleOwner(), val -> updateUi.run());
        viewModel.getStartYear().observe(getViewLifecycleOwner(), val -> updateUi.run());
        viewModel.getCycleLength().observe(getViewLifecycleOwner(), val -> updateUi.run());
        viewModel.getHour().observe(getViewLifecycleOwner(), val -> updateUi.run());
        viewModel.getMinute().observe(getViewLifecycleOwner(), val -> updateUi.run());
        viewModel.getBackgroundImageUri().observe(getViewLifecycleOwner(), uri -> {
            if (uri != null) {
                backgroundImageView.setImageURI(Uri.parse(uri));
            } else {
                backgroundImageView.setImageDrawable(null);
            }
        });

        updateUi.run();
        return view;
    }

    // Save a cycle event to the history in SharedPreferences
    private void saveCycleToHistory(SharedPreferences prefs, long dateMillis, long endDateMillis, String type) {
        Gson gson = new Gson();
        List<Cycle> cycleHistory = getCycleHistory(prefs);

        for (Cycle c : cycleHistory) {
            if (c.getDateMillis() == dateMillis && c.getType().equals(type)) {
                return; // Duplikat
            }
        }

        cycleHistory.add(new Cycle(dateMillis, endDateMillis, type));
        prefs.edit().putString(KEY_CYCLE_HISTORY, gson.toJson(cycleHistory)).apply();
    }

    // Retrieve the cycle history from SharedPreferences
    private List<Cycle> getCycleHistory(SharedPreferences prefs) {
        Gson gson = new Gson();
        String json = prefs.getString(KEY_CYCLE_HISTORY, null);
        Type type = new TypeToken<List<Cycle>>(){}.getType();
        List<Cycle> cycleHistory = gson.fromJson(json, type);
        return cycleHistory != null ? cycleHistory : new ArrayList<>();
    }

    // Get the last saved reinsertion date from the cycle history
    private Calendar getLastSavedReinsertionDate(List<Cycle> cycleHistory) {
        long latestRemovalDate = 0;
        for (Cycle cycle : cycleHistory) {
            if ("removal".equals(cycle.getType()) && cycle.getDateMillis() > latestRemovalDate) {
                latestRemovalDate = cycle.getDateMillis();
            }
        }
        if (latestRemovalDate == 0) return null;

        Calendar reinsertionDate = Calendar.getInstance();
        reinsertionDate.setTimeInMillis(latestRemovalDate);
        reinsertionDate.add(Calendar.DAY_OF_MONTH, RING_FREE_DAYS);
        return reinsertionDate;
    }

    // Schedule a notification for a specific date and time
    private void scheduleNotification(Calendar calendar, String title, String message) {
        Intent intent = new Intent(requireContext(), NotificationReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("message", message);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                (int) calendar.getTimeInMillis(), // unique ID
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }

    // Schedule notifications for the ring cycle events
    private void scheduleRingCycleNotifications(Calendar startDate, Calendar removalDate, Calendar reinsertionDate) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long cycleStart = startDate.getTimeInMillis();

        // Check if notifications for this cycle have already been set
        String notificationKey = "notified_" + cycleStart;
        if (prefs.getBoolean(notificationKey, false)) {
            return;
        }

        // Schedule notifications for the cycle events
        Calendar week1 = (Calendar) startDate.clone();
        week1.add(Calendar.DAY_OF_MONTH, 7);
        week1.set(Calendar.HOUR_OF_DAY, 18);
        week1.set(Calendar.MINUTE, 0);
        scheduleNotification(week1, "Zyklusfortschritt", "Noch zwei Wochen Tragezeit.");

        Calendar week2 = (Calendar) startDate.clone();
        week2.add(Calendar.DAY_OF_MONTH, 14);
        week2.set(Calendar.HOUR_OF_DAY, 18);
        week2.set(Calendar.MINUTE, 0);
        scheduleNotification(week2, "Zyklusfortschritt", "Noch eine Woche Tragezeit.");

        Calendar reinsertion10 = (Calendar) reinsertionDate.clone();
        reinsertion10.set(Calendar.HOUR_OF_DAY, 10);
        reinsertion10.set(Calendar.MINUTE, 0);
        scheduleNotification(reinsertion10, "Ring einsetzen", "Heute um 18 Uhr wieder einsetzen!");

        Calendar reinsertion18 = (Calendar) reinsertionDate.clone();
        reinsertion18.set(Calendar.HOUR_OF_DAY, 18);
        reinsertion18.set(Calendar.MINUTE, 0);
        scheduleNotification(reinsertion18, "Ring einsetzen", "Ring wieder einsetzen!");

        Calendar removal10 = (Calendar) removalDate.clone();
        removal10.set(Calendar.HOUR_OF_DAY, 10);
        removal10.set(Calendar.MINUTE, 0);
        scheduleNotification(removal10, "Ring entfernen", "Heute um 18 Uhr Ring entfernen!");

        Calendar removal18 = (Calendar) removalDate.clone();
        removal18.set(Calendar.HOUR_OF_DAY, 18);
        removal18.set(Calendar.MINUTE, 0);
        scheduleNotification(removal18, "Ring entfernen", "Ring wieder entfernen!");

        // Mark this cycle as notified
        prefs.edit().putBoolean(notificationKey, true).apply();
    }
}