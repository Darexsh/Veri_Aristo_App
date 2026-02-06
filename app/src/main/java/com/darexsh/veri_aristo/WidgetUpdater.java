package com.darexsh.veri_aristo;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Configuration;
import android.os.LocaleList;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import java.util.Locale;

public final class WidgetUpdater {

    private WidgetUpdater() {
    }

    public static void updateAllWidgets(Context context) {
        Context localized = getLocalizedContext(context);
        AppWidgetManager manager = AppWidgetManager.getInstance(localized);
        updateSmallWidgets(localized, manager);
        updateLargeWidgets(localized, manager);
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

    private static Context getLocalizedContext(Context context) {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        if (locales.isEmpty()) {
            return context;
        }
        Locale locale = locales.get(0);
        if (locale == null) {
            return context;
        }
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocales(new LocaleList(locale));
        return context.createConfigurationContext(config);
    }
}
