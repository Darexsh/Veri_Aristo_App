package com.example.veri_aristo;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;

public final class WidgetUpdater {

    private WidgetUpdater() {
    }

    public static void updateAllWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        updateSmallWidgets(context, manager);
        updateLargeWidgets(context, manager);
    }

    private static void updateSmallWidgets(Context context, AppWidgetManager manager) {
        int[] widgetIds = manager.getAppWidgetIds(new ComponentName(context, CycleWidgetSmallProvider.class));
        for (int appWidgetId : widgetIds) {
            CycleWidgetSmallProvider.updateAppWidget(context, manager, appWidgetId);
        }
    }

    private static void updateLargeWidgets(Context context, AppWidgetManager manager) {
        int[] widgetIds = manager.getAppWidgetIds(new ComponentName(context, CycleWidgetLargeProvider.class));
        for (int appWidgetId : widgetIds) {
            CycleWidgetLargeProvider.updateAppWidget(context, manager, appWidgetId);
        }
    }
}
