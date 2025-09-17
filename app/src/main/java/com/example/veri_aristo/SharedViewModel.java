package com.example.veri_aristo;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

// SharedViewModel is used to share data between fragments in the app
public class SharedViewModel extends ViewModel {

    // LiveData objects to hold the state of the cycle length, start date, and time
    private final MutableLiveData<Integer> cycleLength = new MutableLiveData<>();
    private final MutableLiveData<Integer> startDay = new MutableLiveData<>();
    private final MutableLiveData<Integer> startMonth = new MutableLiveData<>();
    private final MutableLiveData<Integer> startYear = new MutableLiveData<>();
    private final MutableLiveData<Integer> hour = new MutableLiveData<>();
    private final MutableLiveData<Integer> minute = new MutableLiveData<>();
    private final MutableLiveData<String> backgroundImageUri = new MutableLiveData<>();

    // Getters and setters for cycle length
    public LiveData<Integer> getCycleLength() {
        return cycleLength;
    }

    // Set the cycle length and notify observers
    public void setCycleLength(int length) {
        cycleLength.setValue(length);
    }

    // Getters and setters for start date and time
    public LiveData<Integer> getStartDay() {
        return startDay;
    }

    // Set the start day and notify observers
    public void setStartDay(int day) {
        startDay.setValue(day);
    }

    // Getters and setters for start month and year
    public LiveData<Integer> getStartMonth() {
        return startMonth;
    }

    // Set the start month and notify observers
    public void setStartMonth(int month) {
        startMonth.setValue(month);
    }

    // Getters and setters for start year
    public LiveData<Integer> getStartYear() {
        return startYear;
    }

    // Set the start year and notify observers
    public void setStartYear(int year) {
        startYear.setValue(year);
    }

    // Getters and setters for hour and minute
    public LiveData<Integer> getHour() {
        return hour;
    }

    // Set the hour and notify observers
    public void setHour(int h) {
        hour.setValue(h);
    }

    // Getters and setters for minute
    public LiveData<Integer> getMinute() {
        return minute;
    }

    // Set the minute and notify observers
    public void setMinute(int m) {
        minute.setValue(m);
    }

    // Getters and setters for background image URI
    public MutableLiveData<String> getBackgroundImageUri() { 
        return backgroundImageUri; 
    }

    // Set the background image URI and notify observers
    public void setBackgroundImageUri(String uri) { 
        this.backgroundImageUri.setValue(uri); 
    }
}
