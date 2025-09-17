package com.example.veri_aristo;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

// Fragment for displaying a calendar with cycle-related date decorations
public class CalendarFragment extends Fragment {

    private MaterialCalendarView calendarView;
    private final int ringFreeDays = 7; // 6 days ring-free + 1 insertion day
    private SharedViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate calendar layout
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        // Initialize calendar view and apply theme
        calendarView = view.findViewById(R.id.calendarView);
        calendarView.setDateTextAppearance(R.style.Theme_Veri_Aristo);

        // Initialize shared ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        // Set up calendar view properties
        updateCalendar();
        setupObservers();

        return view;
    }

    // Observe cycle data changes and update calendar
    private void setupObservers() {
        viewModel.getCycleLength().observe(getViewLifecycleOwner(), value -> updateCalendar()); // Observe cycle length changes
        viewModel.getStartDay().observe(getViewLifecycleOwner(), value -> updateCalendar());    // Observe start day changes
        viewModel.getStartMonth().observe(getViewLifecycleOwner(), value -> updateCalendar());  // Observe start month changes
        viewModel.getStartYear().observe(getViewLifecycleOwner(), value -> updateCalendar());   // Observe start year changes
    }

    // Refresh calendar with updated cycle data
    private void updateCalendar() {
        Calendar startDate = getStartDate();                // Get start date from ViewModel or SharedPreferences
        int cycleLength = getCycleLength();                 // Get cycle length from ViewModel or SharedPreferences
        calendarView.removeDecorators();                    // Clear previous decorators
        setupCalendarDecorators(startDate, cycleLength);    // Set up decorators for the calendar
    }

    // Retrieve start date from ViewModel or SharedPreferences
    private Calendar getStartDate() {
        int day, month, year;

        // Check if start date is set in ViewModel, otherwise use SharedPreferences
        if (viewModel.getStartDay().getValue() != null &&
                viewModel.getStartMonth().getValue() != null &&
                viewModel.getStartYear().getValue() != null) {

            day = viewModel.getStartDay().getValue();
            month = viewModel.getStartMonth().getValue();
            year = viewModel.getStartYear().getValue();
        } else {
            SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
            day = prefs.getInt("start_day", Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
            month = prefs.getInt("start_month", Calendar.getInstance().get(Calendar.MONTH));
            year = prefs.getInt("start_year", Calendar.getInstance().get(Calendar.YEAR));
        }

        // Create a Calendar instance with the retrieved date
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day);

        return calendar;
    }

    // Retrieve cycle length from ViewModel or SharedPreferences
    private int getCycleLength() {
        if (viewModel.getCycleLength().getValue() != null) {
            return viewModel.getCycleLength().getValue();
        } else {
            SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
            return prefs.getInt("cycle_length", 21);
        }
    }

    // Set up decorators for the calendar based on the cycle length and start date
    private void setupCalendarDecorators(Calendar startDate, int cycleLength) {
        List<DayViewDecorator> decorators = new ArrayList<>();

        // Define limits for past and future cycles, 1 year in the past
        Calendar pastLimit = (Calendar) startDate.clone();
        pastLimit.add(Calendar.YEAR, -1);

        // Define limit for future cycles, 2 years in the future
        Calendar futureLimit = (Calendar) startDate.clone();
        futureLimit.add(Calendar.YEAR, 2);

        // Backward loop for past cycles
        Calendar currentPastStart = (Calendar) startDate.clone();
        while (true) {
            currentPastStart.add(Calendar.DAY_OF_MONTH, -(cycleLength + ringFreeDays));

            if (currentPastStart.before(pastLimit)) break;

            // Decorate the past cycle
            Calendar removalDate = (Calendar) currentPastStart.clone();
            removalDate.add(Calendar.DAY_OF_MONTH, cycleLength);

            // Calculate the new insertion date
            Calendar newInsertionDate = (Calendar) removalDate.clone();
            newInsertionDate.add(Calendar.DAY_OF_MONTH, ringFreeDays);

            // Define the green range for the cycle
            Calendar greenStart = (Calendar) currentPastStart.clone();
            greenStart.add(Calendar.DAY_OF_MONTH, 1);
            Calendar greenEnd = (Calendar) removalDate.clone();
            greenEnd.add(Calendar.DAY_OF_MONTH, -1);

            // Define the red range for the cycle
            Calendar redStart = (Calendar) removalDate.clone();
            redStart.add(Calendar.DAY_OF_MONTH, 1);
            Calendar redEnd = (Calendar) removalDate.clone();
            redEnd.add(Calendar.DAY_OF_MONTH, 6);

            // Add decorators for the current cycle
            decorators.add(new SingleDayDecorator(Color.argb(127, 0, 255, 255), toCalendarDay(currentPastStart)));
            decorators.add(new SingleDayDecorator(Color.argb(127, 255, 255, 0), toCalendarDay(removalDate)));
            decorators.add(new SingleDayDecorator(Color.argb(127, 0, 255, 255), toCalendarDay(newInsertionDate)));
            decorators.add(new RangeDayDecorator(Color.argb(127, 0, 255, 0), toCalendarDay(greenStart), toCalendarDay(greenEnd)));
            decorators.add(new RangeDayDecorator(Color.argb(127, 255, 0, 0), toCalendarDay(redStart), toCalendarDay(redEnd)));
        }

        // Forward loop for future cycles
        Calendar currentStartDate = (Calendar) startDate.clone();
        while (currentStartDate.before(futureLimit)) {
            // Add the cycle length and ring-free days to the current start date
            Calendar removalDate = (Calendar) currentStartDate.clone();
            removalDate.add(Calendar.DAY_OF_MONTH, cycleLength);

            // Calculate the new insertion date
            Calendar newInsertionDate = (Calendar) removalDate.clone();
            newInsertionDate.add(Calendar.DAY_OF_MONTH, ringFreeDays);

            // Define the green range for the cycle
            Calendar greenStart = (Calendar) currentStartDate.clone();
            greenStart.add(Calendar.DAY_OF_MONTH, 1);
            Calendar greenEnd = (Calendar) removalDate.clone();
            greenEnd.add(Calendar.DAY_OF_MONTH, -1);

            // Define the red range for the cycle
            Calendar redStart = (Calendar) removalDate.clone();
            redStart.add(Calendar.DAY_OF_MONTH, 1);
            Calendar redEnd = (Calendar) removalDate.clone();
            redEnd.add(Calendar.DAY_OF_MONTH, 6);

            // Add decorators for the current cycle
            decorators.add(new SingleDayDecorator(Color.argb(127, 0, 255, 255), toCalendarDay(currentStartDate)));
            decorators.add(new SingleDayDecorator(Color.argb(127, 255, 255, 0), toCalendarDay(removalDate)));
            decorators.add(new SingleDayDecorator(Color.argb(127, 0, 255, 255), toCalendarDay(newInsertionDate)));
            decorators.add(new RangeDayDecorator(Color.argb(127, 0, 255, 0), toCalendarDay(greenStart), toCalendarDay(greenEnd)));
            decorators.add(new RangeDayDecorator(Color.argb(127, 255, 0, 0), toCalendarDay(redStart), toCalendarDay(redEnd)));

            // Update the current start date for the next cycle
            currentStartDate = (Calendar) newInsertionDate.clone();
        }

        // Add all decorators to the calendar view
        for (DayViewDecorator d : decorators) {
            calendarView.addDecorator(d);
        }
    }


    // Decorator for a single day
    private static class SingleDayDecorator implements DayViewDecorator {
        private final int color;
        private final CalendarDay day;

        // Constructor for SingleDayDecorator
        public SingleDayDecorator(int color, CalendarDay day) {
            this.color = color;
            this.day = day;
        }

        @Override
        // Check if the decorator should apply to the given day
        public boolean shouldDecorate(CalendarDay day) {
            return this.day.equals(day);
        }

        @Override
        // Apply the decoration to the day view
        public void decorate(DayViewFacade view) {
            GradientDrawable drawable = createCircleDrawable(color);
            view.setBackgroundDrawable(drawable);
        }
    }

    // Decorator for a range of days
    private static class RangeDayDecorator implements DayViewDecorator {
        private final int color;
        private final CalendarDay startDay;
        private final CalendarDay endDay;

        // Constructor for RangeDayDecorator
        public RangeDayDecorator(int color, CalendarDay startDay, CalendarDay endDay) {
            this.color = color;
            this.startDay = startDay;
            this.endDay = endDay;
        }

        @Override
        // Check if the decorator should apply to the given day
        public boolean shouldDecorate(CalendarDay day) {
            return !day.isBefore(startDay) && !day.isAfter(endDay);
        }

        @Override
        // Apply the decoration to the day view
        public void decorate(DayViewFacade view) {
            GradientDrawable drawable = createCircleDrawable(color);
            view.setBackgroundDrawable(drawable);
        }
    }

    // Create a circular drawable with the specified color
    private static GradientDrawable createCircleDrawable(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        drawable.setSize(40, 40);
        return drawable;
    }

    // Convert a Calendar instance to a CalendarDay instance
    private CalendarDay toCalendarDay(Calendar calendar) {
        return CalendarDay.from(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
        );
    }
}
