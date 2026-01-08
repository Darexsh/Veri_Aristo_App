package com.example.veri_aristo;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Calendar;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SettingsRepository {

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_START_DAY = "start_day";
    private static final String KEY_START_MONTH = "start_month";
    private static final String KEY_START_YEAR = "start_year";
    private static final String KEY_SET_TIME_HOUR = "set_time_hour";
    private static final String KEY_SET_TIME_MINUTE = "set_time_minute";
    private static final String KEY_CYCLE_LENGTH = "cycle_length";
    private static final String KEY_BACKGROUND_IMAGE_URI = "background_image_uri";
    private static final String KEY_CYCLE_HISTORY = "cycle_history";
    private static final String KEY_CALENDAR_PAST_MONTHS = "calendar_past_months";
    private static final String KEY_CALENDAR_FUTURE_YEARS = "calendar_future_years";
    private static final String KEY_CALENDAR_PAST_AMOUNT = "calendar_past_amount";
    private static final String KEY_CALENDAR_PAST_UNIT = "calendar_past_unit";
    private static final String KEY_CALENDAR_FUTURE_AMOUNT = "calendar_future_amount";
    private static final String KEY_CALENDAR_FUTURE_UNIT = "calendar_future_unit";
    private static final String KEY_REMOVAL_REMINDER_HOURS = "removal_reminder_hours";
    private static final String KEY_INSERTION_REMINDER_HOURS = "insertion_reminder_hours";

    private final SharedPreferences sharedPreferences;

    public SettingsRepository(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public Calendar getStartDate() {
        Calendar calendar = Calendar.getInstance();
        int day = sharedPreferences.getInt(KEY_START_DAY, calendar.get(Calendar.DAY_OF_MONTH));
        int month = sharedPreferences.getInt(KEY_START_MONTH, calendar.get(Calendar.MONTH));
        int year = sharedPreferences.getInt(KEY_START_YEAR, calendar.get(Calendar.YEAR));
        int hour = sharedPreferences.getInt(KEY_SET_TIME_HOUR, 18);
        int minute = sharedPreferences.getInt(KEY_SET_TIME_MINUTE, 0);
        calendar.set(year, month, day, hour, minute);
        return calendar;
    }

    public void saveStartDate(Calendar calendar) {
        sharedPreferences.edit()
                .putInt(KEY_START_DAY, calendar.get(Calendar.DAY_OF_MONTH))
                .putInt(KEY_START_MONTH, calendar.get(Calendar.MONTH))
                .putInt(KEY_START_YEAR, calendar.get(Calendar.YEAR))
                .putInt(KEY_SET_TIME_HOUR, calendar.get(Calendar.HOUR_OF_DAY))
                .putInt(KEY_SET_TIME_MINUTE, calendar.get(Calendar.MINUTE))
                .apply();
    }

    public int getCycleLength() {
        return sharedPreferences.getInt(KEY_CYCLE_LENGTH, 21);
    }

    public void saveCycleLength(int length) {
        sharedPreferences.edit().putInt(KEY_CYCLE_LENGTH, length).apply();
    }

    public String getBackgroundImageUri() {
        return sharedPreferences.getString(KEY_BACKGROUND_IMAGE_URI, null);
    }

    public void saveBackgroundImageUri(String uri) {
        sharedPreferences.edit().putString(KEY_BACKGROUND_IMAGE_URI, uri).apply();
    }

    public int getCalendarPastAmount() {
        if (sharedPreferences.contains(KEY_CALENDAR_PAST_AMOUNT)) {
            return sharedPreferences.getInt(KEY_CALENDAR_PAST_AMOUNT, 12);
        }
        return sharedPreferences.getInt(KEY_CALENDAR_PAST_MONTHS, 12);
    }

    public String getCalendarPastUnit() {
        if (sharedPreferences.contains(KEY_CALENDAR_PAST_UNIT)) {
            return sharedPreferences.getString(KEY_CALENDAR_PAST_UNIT, "months");
        }
        return "months";
    }

    public void saveCalendarPastRange(int amount, String unit) {
        sharedPreferences.edit()
                .putInt(KEY_CALENDAR_PAST_AMOUNT, amount)
                .putString(KEY_CALENDAR_PAST_UNIT, unit)
                .apply();
    }

    public int getCalendarFutureAmount() {
        if (sharedPreferences.contains(KEY_CALENDAR_FUTURE_AMOUNT)) {
            return sharedPreferences.getInt(KEY_CALENDAR_FUTURE_AMOUNT, 2);
        }
        if (sharedPreferences.contains(KEY_CALENDAR_FUTURE_YEARS)) {
            return sharedPreferences.getInt(KEY_CALENDAR_FUTURE_YEARS, 2);
        }
        return 2;
    }

    public String getCalendarFutureUnit() {
        if (sharedPreferences.contains(KEY_CALENDAR_FUTURE_UNIT)) {
            return sharedPreferences.getString(KEY_CALENDAR_FUTURE_UNIT, "years");
        }
        if (sharedPreferences.contains(KEY_CALENDAR_FUTURE_YEARS)) {
            return "years";
        }
        return "years";
    }

    public void saveCalendarFutureRange(int amount, String unit) {
        sharedPreferences.edit()
                .putInt(KEY_CALENDAR_FUTURE_AMOUNT, amount)
                .putString(KEY_CALENDAR_FUTURE_UNIT, unit)
                .apply();
    }

    public List<Cycle> getCycleHistory() {
        Gson gson = new Gson();
        String json = sharedPreferences.getString(KEY_CYCLE_HISTORY, null);
        Type type = new TypeToken<ArrayList<Cycle>>() {}.getType();
        List<Cycle> cycleHistory = gson.fromJson(json, type);
        return cycleHistory != null ? cycleHistory : new ArrayList<>();
    }

    public void saveCycleHistory(List<Cycle> cycleHistory) {
        Gson gson = new Gson();
        String json = gson.toJson(cycleHistory);
        sharedPreferences.edit().putString(KEY_CYCLE_HISTORY, json).apply();
    }

    public boolean wasNotificationScheduledForCycle(long cycleStartMillis) {
        return sharedPreferences.getBoolean("notified_" + cycleStartMillis, false);
    }

    public void setNotificationScheduledForCycle(long cycleStartMillis) {
        sharedPreferences.edit().putBoolean("notified_" + cycleStartMillis, true).apply();
    }

    public void clearNotificationScheduledForCycle(long cycleStartMillis) {
        sharedPreferences.edit().putBoolean("notified_" + cycleStartMillis, false).apply();
    }

    public int getRemovalReminderHours() {
        return sharedPreferences.getInt(KEY_REMOVAL_REMINDER_HOURS, 6);
    }

    public void setRemovalReminderHours(int hours) {
        sharedPreferences.edit().putInt(KEY_REMOVAL_REMINDER_HOURS, hours).apply();
    }

    public int getInsertionReminderHours() {
        return sharedPreferences.getInt(KEY_INSERTION_REMINDER_HOURS, 6);
    }

    public void setInsertionReminderHours(int hours) {
        sharedPreferences.edit().putInt(KEY_INSERTION_REMINDER_HOURS, hours).apply();
    }

    public int getNotificationSettingsHash() {
        int cycleLength = getCycleLength();
        int removalHours = getRemovalReminderHours();
        int insertionHours = getInsertionReminderHours();
        int hash = 17;
        hash = 31 * hash + cycleLength;
        hash = 31 * hash + removalHours;
        hash = 31 * hash + insertionHours;
        return hash;
    }

    public int getNotificationSettingsHashForCycle(long cycleStartMillis) {
        return sharedPreferences.getInt("notified_settings_" + cycleStartMillis, 0);
    }

    public void setNotificationSettingsHashForCycle(long cycleStartMillis, int hash) {
        sharedPreferences.edit().putInt("notified_settings_" + cycleStartMillis, hash).apply();
    }

    public void clearAllData() {
        sharedPreferences.edit().clear().apply();
    }

    public void clearNotificationFlags() {
        android.content.SharedPreferences.Editor editor = sharedPreferences.edit();
        for (String key : sharedPreferences.getAll().keySet()) {
            if (key.startsWith("notified_") || key.startsWith("notified_settings_")) {
                editor.remove(key);
            }
        }
        editor.apply();
    }

    public int getCycleDelayDays(long cycleStartMillis) {
        return sharedPreferences.getInt(cycleDelayKey(cycleStartMillis), 0);
    }

    public void setCycleDelayDays(long cycleStartMillis, int delayDays) {
        sharedPreferences.edit().putInt(cycleDelayKey(cycleStartMillis), delayDays).apply();
    }

    private String cycleDelayKey(long cycleStartMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(cycleStartMillis);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        return "cycle_delay_" + year + "_" + month + "_" + day;
    }
}
