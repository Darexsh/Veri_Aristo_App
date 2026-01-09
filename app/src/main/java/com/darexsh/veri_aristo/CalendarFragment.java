package com.darexsh.veri_aristo;

import android.app.AlertDialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import android.text.style.LineBackgroundSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.res.ColorStateList;
import androidx.core.graphics.ColorUtils;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import com.darexsh.veri_aristo.Constants;

public class CalendarFragment extends Fragment {

    private MaterialCalendarView calendarView;
    private final int ringFreeDays = Constants.RING_FREE_DAYS; // 6 days ring-free + 1 insertion day
    private SharedViewModel viewModel;
    private View legendWearView;
    private View legendRingFreeView;
    private View legendRemovalView;
    private View legendInsertionView;
    private static final int CALENDAR_ALPHA = 127;
    private static final int LEGEND_ALPHA = 255;
    private int[] colorValues;
    private String[] colorLabels;

    private interface ColorConsumer {
        void accept(int color);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate calendar layout
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);
        calendarView = view.findViewById(R.id.calendarView);
        calendarView.setDateTextAppearance(R.style.Theme_Veri_Aristo);
        calendarView.post(() -> tintCalendarArrows(resolveCalendarHeaderColor()));
        legendWearView = view.findViewById(R.id.view_legend_wear);
        legendRingFreeView = view.findViewById(R.id.view_legend_ring_free);
        legendRemovalView = view.findViewById(R.id.view_legend_removal);
        legendInsertionView = view.findViewById(R.id.view_legend_insertion);

        if (legendWearView != null) {
            legendWearView.setOnClickListener(v -> showLegendColorDialog(
                    R.string.settings_calendar_wear_color_dialog_title,
                    R.string.settings_calendar_wear_color_custom_title,
                    getCalendarWearColor(),
                    viewModel::setCalendarWearColor
            ));
        }
        if (legendRingFreeView != null) {
            legendRingFreeView.setOnClickListener(v -> showLegendColorDialog(
                    R.string.settings_calendar_ring_free_color_dialog_title,
                    R.string.settings_calendar_ring_free_color_custom_title,
                    getCalendarRingFreeColor(),
                    viewModel::setCalendarRingFreeColor
            ));
        }
        if (legendRemovalView != null) {
            legendRemovalView.setOnClickListener(v -> showLegendColorDialog(
                    R.string.settings_calendar_removal_color_dialog_title,
                    R.string.settings_calendar_removal_color_custom_title,
                    getCalendarRemovalColor(),
                    viewModel::setCalendarRemovalColor
            ));
        }
        if (legendInsertionView != null) {
            legendInsertionView.setOnClickListener(v -> showLegendColorDialog(
                    R.string.settings_calendar_insertion_color_dialog_title,
                    R.string.settings_calendar_insertion_color_custom_title,
                    getCalendarInsertionColor(),
                    viewModel::setCalendarInsertionColor
            ));
        }

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
        viewModel.getCalendarWearColor().observe(getViewLifecycleOwner(), value -> {
            updateCalendar();
            updateLegendColors();
        });
        viewModel.getCalendarRingFreeColor().observe(getViewLifecycleOwner(), value -> {
            updateCalendar();
            updateLegendColors();
        });
        viewModel.getCalendarRemovalColor().observe(getViewLifecycleOwner(), value -> {
            updateCalendar();
            updateLegendColors();
        });
        viewModel.getCalendarInsertionColor().observe(getViewLifecycleOwner(), value -> {
            updateCalendar();
            updateLegendColors();
        });
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
        updateLegendColors();
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

        int wearColor = ColorUtils.setAlphaComponent(getCalendarWearColor(), CALENDAR_ALPHA);
        int ringFreeColor = ColorUtils.setAlphaComponent(getCalendarRingFreeColor(), CALENDAR_ALPHA);
        int removalColor = ColorUtils.setAlphaComponent(getCalendarRemovalColor(), CALENDAR_ALPHA);
        int insertionColor = ColorUtils.setAlphaComponent(getCalendarInsertionColor(), CALENDAR_ALPHA);

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
                decorators.add(new SingleDayDecorator(insertionColor, toCalendarDay(currentStartDate)));
            }
            if (isWithinRange(removalDate, pastLimit, futureLimit)) {
                decorators.add(new SingleDayDecorator(removalColor, toCalendarDay(removalDate)));
            }
            if (isWithinRange(newInsertionDate, pastLimit, futureLimit)) {
                decorators.add(new SingleDayDecorator(insertionColor, toCalendarDay(newInsertionDate)));
            }
            if (isOverlappingRange(greenStart, greenEnd, pastLimit, futureLimit)) {
                decorators.add(new RangeDayDecorator(wearColor, toCalendarDay(greenStart), toCalendarDay(greenEnd)));
            }
            if (isOverlappingRange(redStart, redEnd, pastLimit, futureLimit)) {
                decorators.add(new RangeDayDecorator(ringFreeColor, toCalendarDay(redStart), toCalendarDay(redEnd)));
            }

            currentStartDate.add(Calendar.DAY_OF_MONTH, stepDays);
            guard++;
        }

        for (DayViewDecorator d : decorators) {
            calendarView.addDecorator(d);
        }
    }

    private void tintCalendarArrows(int color) {
        tintImageViews(calendarView, color);
    }

    private void updateLegendColors() {
        applyLegendColor(legendWearView, getCalendarWearColor());
        applyLegendColor(legendRingFreeView, getCalendarRingFreeColor());
        applyLegendColor(legendRemovalView, getCalendarRemovalColor());
        applyLegendColor(legendInsertionView, getCalendarInsertionColor());
    }

    private void showLegendColorDialog(int titleResId, int customTitleResId, int selectedColor, ColorConsumer onSelect) {
        ensureColorOptionsLoaded();
        int selectedIndex = getColorIndex(selectedColor);
        final int[] pendingColor = new int[]{selectedColor};

        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_button_color_list, null);
        android.widget.ListView listView = content.findViewById(R.id.list_button_colors);
        com.google.android.material.button.MaterialButton customButton = content.findViewById(R.id.btn_custom_color);
        com.google.android.material.button.MaterialButton cancelButton = content.findViewById(R.id.btn_cancel_color);
        android.widget.TextView widgetNote = content.findViewById(R.id.tv_color_dialog_note);
        if (widgetNote != null) {
            widgetNote.setVisibility(View.GONE);
        }

        android.widget.ListAdapter adapter = new android.widget.ArrayAdapter<String>(
                requireContext(),
                R.layout.dialog_button_color_item,
                android.R.id.text1,
                colorLabels
        ) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                View swatch = view.findViewById(R.id.view_color_swatch);
                if (swatch != null) {
                    swatch.setBackgroundColor(colorValues[position]);
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
            pendingColor[0] = colorValues[position];
            onSelect.accept(pendingColor[0]);
            dialog.dismiss();
        });

        customButton.setOnClickListener(v -> {
            dialog.dismiss();
            showCustomLegendColorDialog(customTitleResId, pendingColor[0], onSelect);
        });
        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showCustomLegendColorDialog(int titleResId, int initialColor, ColorConsumer onSelect) {
        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_button_color_custom, null);
        HsvColorWheelView colorWheel = content.findViewById(R.id.color_wheel);
        View preview = content.findViewById(R.id.view_color_preview);
        final int[] pendingColor = new int[]{initialColor};
        preview.setBackgroundTintList(ColorStateList.valueOf(initialColor));
        colorWheel.setColor(initialColor);
        colorWheel.setOnColorChangeListener(color -> {
            pendingColor[0] = color;
            preview.setBackgroundTintList(ColorStateList.valueOf(color));
        });

        new AlertDialog.Builder(requireContext())
                .setTitle(titleResId)
                .setView(content)
                .setPositiveButton(R.string.dialog_ok, (dialog, which) -> onSelect.accept(pendingColor[0]))
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void ensureColorOptionsLoaded() {
        if (colorValues == null || colorLabels == null) {
            colorValues = getResources().getIntArray(R.array.settings_button_color_values);
            colorLabels = getResources().getStringArray(R.array.settings_button_color_labels);
        }
    }

    private int getColorIndex(int color) {
        ensureColorOptionsLoaded();
        for (int i = 0; i < colorValues.length; i++) {
            if (colorValues[i] == color) {
                return i;
            }
        }
        return 0;
    }

    private void applyLegendColor(View view, int color) {
        if (view == null) {
            return;
        }
        int tintedColor = ColorUtils.setAlphaComponent(color, LEGEND_ALPHA);
        view.setBackgroundTintList(ColorStateList.valueOf(tintedColor));
    }

    private int getCalendarWearColor() {
        Integer value = viewModel.getCalendarWearColor().getValue();
        return value != null ? value : SettingsRepository.DEFAULT_CALENDAR_WEAR_COLOR;
    }

    private int getCalendarRingFreeColor() {
        Integer value = viewModel.getCalendarRingFreeColor().getValue();
        return value != null ? value : SettingsRepository.DEFAULT_CALENDAR_RING_FREE_COLOR;
    }

    private int getCalendarRemovalColor() {
        Integer value = viewModel.getCalendarRemovalColor().getValue();
        return value != null ? value : SettingsRepository.DEFAULT_CALENDAR_REMOVAL_COLOR;
    }

    private int getCalendarInsertionColor() {
        Integer value = viewModel.getCalendarInsertionColor().getValue();
        return value != null ? value : SettingsRepository.DEFAULT_CALENDAR_INSERTION_COLOR;
    }

    private void tintImageViews(View view, int color) {
        if (view instanceof ImageView) {
            ((ImageView) view).setColorFilter(color, PorterDuff.Mode.SRC_IN);
            return;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                tintImageViews(group.getChildAt(i), color);
            }
        }
    }

    private int resolveThemeColor(int attr, int fallback) {
        TypedValue typedValue = new TypedValue();
        if (requireContext().getTheme().resolveAttribute(attr, typedValue, true)) {
            if (typedValue.resourceId != 0) {
                return requireContext().getColor(typedValue.resourceId);
            }
            return typedValue.data;
        }
        return fallback;
    }

    private int resolveCalendarHeaderColor() {
        TextView header = findLargestTextView(calendarView);
        if (header != null) {
            return header.getCurrentTextColor();
        }
        return resolveThemeColor(com.google.android.material.R.attr.colorOnSurface, Color.WHITE);
    }

    private TextView findLargestTextView(View root) {
        if (root instanceof TextView) {
            return (TextView) root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            TextView best = null;
            float bestSize = 0f;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView candidate = findLargestTextView(group.getChildAt(i));
                if (candidate != null) {
                    float size = candidate.getTextSize();
                    if (size > bestSize) {
                        bestSize = size;
                        best = candidate;
                    }
                }
            }
            return best;
        }
        return null;
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
