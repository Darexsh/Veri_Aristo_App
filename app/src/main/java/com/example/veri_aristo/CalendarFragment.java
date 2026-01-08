package com.example.veri_aristo;

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
import com.example.veri_aristo.Constants;

public class CalendarFragment extends Fragment {

    private MaterialCalendarView calendarView;
    private final int ringFreeDays = Constants.RING_FREE_DAYS; // 6 days ring-free + 1 insertion day
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
        SharedViewModelFactory factory = new SharedViewModelFactory(requireActivity().getApplication());
        viewModel = new ViewModelProvider(requireActivity(), factory).get(SharedViewModel.class);

        // Set up calendar view properties
        updateCalendar();
        setupObservers();

        return view;
    }

    // Observe cycle data changes and update calendar
    private void setupObservers() {
        viewModel.getCycleLength().observe(getViewLifecycleOwner(), value -> updateCalendar());
        viewModel.getStartDate().observe(getViewLifecycleOwner(), value -> updateCalendar());
        viewModel.getCalendarPastAmount().observe(getViewLifecycleOwner(), value -> updateCalendar());
        viewModel.getCalendarPastUnit().observe(getViewLifecycleOwner(), value -> updateCalendar());
        viewModel.getCalendarFutureAmount().observe(getViewLifecycleOwner(), value -> updateCalendar());
        viewModel.getCalendarFutureUnit().observe(getViewLifecycleOwner(), value -> updateCalendar());
    }

    // Refresh calendar with updated cycle data
    private void updateCalendar() {
        Calendar startDate = getStartDate();
        int cycleLength = getCycleLength();
        int pastMonths = getCalendarPastMonths();
        int futureMonths = getCalendarFutureMonths();
        calendarView.removeDecorators();
        setupCalendarDecorators(startDate, cycleLength, pastMonths, futureMonths);
        calendarView.addDecorator(new TodayBorderDecorator()); // Add today border decorator
    }

    // Retrieve start date
    private Calendar getStartDate() {
        if (viewModel.getStartDate().getValue() != null) {
            return viewModel.getStartDate().getValue();
        }
        return Calendar.getInstance();
    }

    // Retrieve cycle length
    private int getCycleLength() {
        if (viewModel.getCycleLength().getValue() != null) {
            return viewModel.getCycleLength().getValue();
        }
        return 21;
    }

    private int getCalendarPastMonths() {
        Integer amount = viewModel.getCalendarPastAmount().getValue();
        String unit = viewModel.getCalendarPastUnit().getValue();
        if (amount != null && unit != null) {
            return "years".equals(unit) ? amount * 12 : amount;
        }
        return 12;
    }

    private int getCalendarFutureMonths() {
        Integer amount = viewModel.getCalendarFutureAmount().getValue();
        String unit = viewModel.getCalendarFutureUnit().getValue();
        if (amount != null && unit != null) {
            return "years".equals(unit) ? amount * 12 : amount;
        }
        return 24;
    }

    // Setup calendar decorators
    private void setupCalendarDecorators(Calendar startDate, int cycleLength, int pastMonths, int futureMonths) {
        List<DayViewDecorator> decorators = new ArrayList<>();

        Calendar today = Calendar.getInstance();
        Calendar pastLimit = (Calendar) today.clone();
        pastLimit.add(Calendar.MONTH, -Math.max(pastMonths, 0));
        Calendar futureLimit = (Calendar) today.clone();
        futureLimit.add(Calendar.MONTH, Math.max(futureMonths, 0));

        int baseStepDays = cycleLength + ringFreeDays;
        if (baseStepDays <= 0) {
            return;
        }

        Calendar currentStartDate = (Calendar) startDate.clone();
        currentStartDate.set(Calendar.SECOND, 0);
        currentStartDate.set(Calendar.MILLISECOND, 0);
        if (currentStartDate.after(pastLimit)) {
            while (currentStartDate.after(pastLimit)) {
                currentStartDate.add(Calendar.DAY_OF_MONTH, -baseStepDays);
            }
        } else {
            while (currentStartDate.before(pastLimit)) {
                int delayDays = getDelayDaysForStart(currentStartDate);
                currentStartDate.add(Calendar.DAY_OF_MONTH, baseStepDays + delayDays);
            }
        }

        int guard = 0;
        while (!currentStartDate.after(futureLimit) && guard < 2000) {
            int delayDays = getDelayDaysForStart(currentStartDate);
            int stepDays = cycleLength + ringFreeDays + delayDays;
            Calendar removalDate = (Calendar) currentStartDate.clone();
            removalDate.add(Calendar.DAY_OF_MONTH, cycleLength + delayDays);
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

            if (isWithinRange(currentStartDate, pastLimit, futureLimit)) {
                decorators.add(new SingleDayDecorator(Color.argb(127, 0, 255, 255), toCalendarDay(currentStartDate)));
            }
            if (isWithinRange(removalDate, pastLimit, futureLimit)) {
                decorators.add(new SingleDayDecorator(Color.argb(127, 255, 255, 0), toCalendarDay(removalDate)));
            }
            if (isWithinRange(newInsertionDate, pastLimit, futureLimit)) {
                decorators.add(new SingleDayDecorator(Color.argb(127, 0, 255, 255), toCalendarDay(newInsertionDate)));
            }
            if (isOverlappingRange(greenStart, greenEnd, pastLimit, futureLimit)) {
                decorators.add(new RangeDayDecorator(Color.argb(127, 0, 255, 0), toCalendarDay(greenStart), toCalendarDay(greenEnd)));
            }
            if (isOverlappingRange(redStart, redEnd, pastLimit, futureLimit)) {
                decorators.add(new RangeDayDecorator(Color.argb(127, 255, 0, 0), toCalendarDay(redStart), toCalendarDay(redEnd)));
            }

            currentStartDate.add(Calendar.DAY_OF_MONTH, stepDays);
            guard++;
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

    private int getDelayDaysForStart(Calendar startDate) {
        Calendar normalized = (Calendar) startDate.clone();
        normalized.set(Calendar.MILLISECOND, 0);
        normalized.set(Calendar.SECOND, 0);
        return viewModel.getRepository().getCycleDelayDays(normalized.getTimeInMillis());
    }

    private boolean isWithinRange(Calendar date, Calendar start, Calendar end) {
        return !date.before(start) && !date.after(end);
    }

    private boolean isOverlappingRange(Calendar rangeStart, Calendar rangeEnd, Calendar windowStart, Calendar windowEnd) {
        return !rangeEnd.before(windowStart) && !rangeStart.after(windowEnd);
    }
}
