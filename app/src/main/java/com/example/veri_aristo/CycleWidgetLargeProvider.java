package com.example.veri_aristo;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class CycleWidgetLargeProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, android.os.Bundle newOptions) {
        updateAppWidget(context, appWidgetManager, appWidgetId);
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_ring_large);
        CycleWidgetUtils.State state = CycleWidgetUtils.calculateState(context);
        SettingsRepository repository = new SettingsRepository(context);
        views.setInt(R.id.widget_bg_image, "setColorFilter", repository.getButtonColor());

        views.setTextViewText(R.id.tv_widget_days_number, String.valueOf(state.daysLeft));
        views.setTextViewText(R.id.tv_widget_days_label, state.label);
        views.setTextViewText(R.id.tv_widget_removal, state.removalText);
        views.setTextViewText(R.id.tv_widget_insertion, state.insertionText);
        float fraction = state.maxProgress > 0
                ? (float) state.currentProgress / (float) state.maxProgress
                : 0f;
        int ringColor = repository.getHomeCircleColor();
        views.setImageViewBitmap(R.id.img_widget_ring, CycleWidgetUtils.buildRingBitmap(context, fraction, ringColor));
        views.setOnClickPendingIntent(R.id.widget_root, buildLaunchIntent(context, appWidgetId));

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static PendingIntent buildLaunchIntent(Context context, int requestCode) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("open_home", true);
        return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
