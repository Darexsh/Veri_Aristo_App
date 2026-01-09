package com.darexsh.veri_aristo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public final class CycleWidgetUtils {

    private CycleWidgetUtils() {
    }

    public static final class State {
        public final int daysLeft;
        public final int maxProgress;
        public final int currentProgress;
        public final String label;
        public final String removalText;
        public final String insertionText;

        public State(int daysLeft, int maxProgress, int currentProgress, String label, String removalText, String insertionText) {
            this.daysLeft = daysLeft;
            this.maxProgress = maxProgress;
            this.currentProgress = currentProgress;
            this.label = label;
            this.removalText = removalText;
            this.insertionText = insertionText;
        }
    }

    public static State calculateState(Context context) {
        SettingsRepository repository = new SettingsRepository(context);
        Calendar baseStart = repository.getStartDate();
        Calendar now = Calendar.getInstance();
        int cycleLength = repository.getCycleLength();

        Calendar currentStart = (Calendar) baseStart.clone();
        currentStart.set(Calendar.SECOND, 0);
        currentStart.set(Calendar.MILLISECOND, 0);

        int delayDays = repository.getCycleDelayDays(currentStart.getTimeInMillis());
        Calendar removalDate = (Calendar) currentStart.clone();
        removalDate.add(Calendar.DAY_OF_MONTH, cycleLength + delayDays);
        Calendar reinsertionDate = (Calendar) removalDate.clone();
        reinsertionDate.add(Calendar.DAY_OF_MONTH, Constants.RING_FREE_DAYS);

        int guard = 0;
        while (now.after(reinsertionDate) && guard < 200) {
            currentStart.add(Calendar.DAY_OF_MONTH, cycleLength + Constants.RING_FREE_DAYS + delayDays);
            delayDays = repository.getCycleDelayDays(currentStart.getTimeInMillis());
            removalDate = (Calendar) currentStart.clone();
            removalDate.add(Calendar.DAY_OF_MONTH, cycleLength + delayDays);
            reinsertionDate = (Calendar) removalDate.clone();
            reinsertionDate.add(Calendar.DAY_OF_MONTH, Constants.RING_FREE_DAYS);
            guard++;
        }

        int remainingDays;
        int maxProgress;
        int currentProgress;
        String label;

        if (now.before(removalDate)) {
            remainingDays = daysUntil(now, removalDate);
            maxProgress = cycleLength;
            currentProgress = maxProgress - remainingDays;
            label = context.getString(R.string.home_days_left);
        } else if (now.before(reinsertionDate)) {
            remainingDays = daysUntil(now, reinsertionDate);
            maxProgress = Constants.RING_FREE_DAYS;
            currentProgress = maxProgress - remainingDays;
            label = context.getString(R.string.home_days_until_insertion);
        } else {
            remainingDays = cycleLength;
            maxProgress = cycleLength;
            currentProgress = 0;
            label = context.getString(R.string.home_days_left);
        }

        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        String removalText = context.getString(R.string.home_removal_on, format.format(removalDate.getTime()));
        String insertionText = context.getString(R.string.home_insertion_on, format.format(reinsertionDate.getTime()));

        return new State(remainingDays, maxProgress, currentProgress, label, removalText, insertionText);
    }

    private static int daysUntil(Calendar from, Calendar to) {
        long millisLeft = to.getTimeInMillis() - from.getTimeInMillis();
        int days = (int) (millisLeft / (24 * 60 * 60 * 1000));
        return Math.max(days, 0);
    }

    public static Bitmap buildRingBitmap(Context context, float progressFraction, int ringColor) {
        int sizePx = context.getResources().getDimensionPixelSize(R.dimen.widget_ring_size);
        int strokePx = context.getResources().getDimensionPixelSize(R.dimen.widget_ring_stroke);
        float clamped = Math.max(0f, Math.min(1f, progressFraction));

        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(strokePx);
        trackPaint.setColor(Color.WHITE);
        trackPaint.setAlpha(80);

        Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(strokePx);
        progressPaint.setColor(ringColor);

        float radius = (sizePx - strokePx) / 2f;
        float cx = sizePx / 2f;
        float cy = sizePx / 2f;

        canvas.drawCircle(cx, cy, radius, trackPaint);
        canvas.drawArc(cx - radius, cy - radius, cx + radius, cy + radius,
                -90f, 360f * clamped, false, progressPaint);

        return bitmap;
    }
}
