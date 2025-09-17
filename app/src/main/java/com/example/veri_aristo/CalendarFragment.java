package com.example.veri_aristo;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import android.text.style.LineBackgroundSpan;
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
        viewModel.getCycleLength().observe(getViewLifecycleOwner(), value -> updateCalendar());
        viewModel.getStartDay().observe(getViewLifecycleOwner(), value -> updateCalendar());
        viewModel.getStartMonth().observe(getViewLifecycleOwner(), value -> updateCalendar());
        viewModel.getStartYear().observe(getViewLifecycleOwner(), value -> updateCalendar());
    }

    // Refresh calendar with updated cycle data
    private void updateCalendar() {
        Calendar startDate = getStartDate();
        int cycleLength = getCycleLength();
        calendarView.removeDecorators();
        setupCalendarDecorators(startDate, cycleLength);
        calendarView.addDecorator(new TodayBorderDecorator()); // Add today border decorator
    }

    // Retrieve start date
    private Calendar getStartDate() {
        int day, month, year;
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
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day);
        return calendar;
    }

    // Retrieve cycle length
    private int getCycleLength() {
        if (viewModel.getCycleLength().getValue() != null) {
            return viewModel.getCycleLength().getValue();
        } else {
            SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
            return prefs.getInt("cycle_length", 21);
        }
    }

    // Setup calendar decorators
    private void setupCalendarDecorators(Calendar startDate, int cycleLength) {
        List<DayViewDecorator> decorators = new ArrayList<>();

        Calendar pastLimit = (Calendar) startDate.clone();
        pastLimit.add(Calendar.YEAR, -1);
        Calendar futureLimit = (Calendar) startDate.clone();
        futureLimit.add(Calendar.YEAR, 2);

        Calendar currentPastStart = (Calendar) startDate.clone();
        while (true) {
            currentPastStart.add(Calendar.DAY_OF_MONTH, -(cycleLength + ringFreeDays));
            if (currentPastStart.before(pastLimit)) break;

            Calendar removalDate = (Calendar) currentPastStart.clone();
            removalDate.add(Calendar.DAY_OF_MONTH, cycleLength);
            Calendar newInsertionDate = (Calendar) removalDate.clone();
            newInsertionDate.add(Calendar.DAY_OF_MONTH, ringFreeDays);

            Calendar greenStart = (Calendar) currentPastStart.clone();
            greenStart.add(Calendar.DAY_OF_MONTH, 1);
            Calendar greenEnd = (Calendar) removalDate.clone();
            greenEnd.add(Calendar.DAY_OF_MONTH, -1);

            Calendar redStart = (Calendar) removalDate.clone();
            redStart.add(Calendar.DAY_OF_MONTH, 1);
            Calendar redEnd = (Calendar) removalDate.clone();
            redEnd.add(Calendar.DAY_OF_MONTH, 6);

            decorators.add(new SingleDayDecorator(Color.argb(127, 0, 255, 255), toCalendarDay(currentPastStart)));
            decorators.add(new SingleDayDecorator(Color.argb(127, 255, 255, 0), toCalendarDay(removalDate)));
            decorators.add(new SingleDayDecorator(Color.argb(127, 0, 255, 255), toCalendarDay(newInsertionDate)));
            decorators.add(new RangeDayDecorator(Color.argb(127, 0, 255, 0), toCalendarDay(greenStart), toCalendarDay(greenEnd)));
            decorators.add(new RangeDayDecorator(Color.argb(127, 255, 0, 0), toCalendarDay(redStart), toCalendarDay(redEnd)));
        }

        Calendar currentStartDate = (Calendar) startDate.clone();
        while (currentStartDate.before(futureLimit)) {
            Calendar removalDate = (Calendar) currentStartDate.clone();
            removalDate.add(Calendar.DAY_OF_MONTH, cycleLength);
            Calendar newInsertionDate = (Calendar) removalDate.clone();
            newInsertionDate.add(Calendar.DAY_OF_MONTH, ringFreeDays);

            Calendar greenStart = (Calendar) currentStartDate.clone();
            greenStart.add(Calendar.DAY_OF_MONTH, 1);
            Calendar greenEnd = (Calendar) removalDate.clone();
            greenEnd.add(Calendar.DAY_OF_MONTH, -1);

            Calendar redStart = (Calendar) removalDate.clone();
            redStart.add(Calendar.DAY_OF_MONTH, 1);
            Calendar redEnd = (Calendar) removalDate.clone();
            redEnd.add(Calendar.DAY_OF_MONTH, 6);

            decorators.add(new SingleDayDecorator(Color.argb(127, 0, 255, 255), toCalendarDay(currentStartDate)));
            decorators.add(new SingleDayDecorator(Color.argb(127, 255, 255, 0), toCalendarDay(removalDate)));
            decorators.add(new SingleDayDecorator(Color.argb(127, 0, 255, 255), toCalendarDay(newInsertionDate)));
            decorators.add(new RangeDayDecorator(Color.argb(127, 0, 255, 0), toCalendarDay(greenStart), toCalendarDay(greenEnd)));
            decorators.add(new RangeDayDecorator(Color.argb(127, 255, 0, 0), toCalendarDay(redStart), toCalendarDay(redEnd)));

            currentStartDate = (Calendar) newInsertionDate.clone();
        }

        for (DayViewDecorator d : decorators) {
            calendarView.addDecorator(d);
        }
    }

    // Single day decorator
    private static class SingleDayDecorator implements DayViewDecorator {
        private final int color;
        private final CalendarDay day;

        public SingleDayDecorator(int color, CalendarDay day) {
            this.color = color;
            this.day = day;
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return this.day.equals(day);
        }

        @Override
        public void decorate(DayViewFacade view) {
            GradientDrawable drawable = createCircleDrawable(color);
            view.setBackgroundDrawable(drawable);
        }
    }

    // Range of days decorator
    private static class RangeDayDecorator implements DayViewDecorator {
        private final int color;
        private final CalendarDay startDay;
        private final CalendarDay endDay;

        public RangeDayDecorator(int color, CalendarDay startDay, CalendarDay endDay) {
            this.color = color;
            this.startDay = startDay;
            this.endDay = endDay;
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return !day.isBefore(startDay) && !day.isAfter(endDay);
        }

        @Override
        public void decorate(DayViewFacade view) {
            GradientDrawable drawable = createCircleDrawable(color);
            view.setBackgroundDrawable(drawable);
        }
    }

    // Today border decorator
    private static class TodayBorderDecorator implements DayViewDecorator {
        private final CalendarDay today = CalendarDay.today();

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return day.equals(today);
        }

        @Override
        public void decorate(DayViewFacade view) {
            view.addSpan(new LineBackgroundSpan() {
                @Override
                public void drawBackground(Canvas canvas, Paint paint, int left, int right, int top,
                                           int baseline, int bottom, CharSequence text, int start, int end, int lineNum) {
                    int cx = (left + right) / 2;
                    int cy = (top + bottom) / 2;
                    int radius = Math.min(right - left, bottom - top) / 2 + 40; // slightly bigger than background
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(5);
                    paint.setColor(Color.LTGRAY);
                    canvas.drawCircle(cx, cy, radius, paint);
                }
            });
        }
    }

    private static GradientDrawable createCircleDrawable(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        drawable.setSize(40, 40);
        return drawable;
    }

    private CalendarDay toCalendarDay(Calendar calendar) {
        return CalendarDay.from(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
        );
    }
}
