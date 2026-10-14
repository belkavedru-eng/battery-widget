package ru.bigbattery.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.widget.RemoteViews;

public class BatteryWidget extends AppWidgetProvider {

    private static final String ACTION_REFRESH = "ru.bigbattery.widget.REFRESH";

    // Цвета: белые цифры на полупрозрачной тёмной подложке (подложка задана в res/drawable/bg.xml)
    private static final int COLOR_NORMAL = 0xFFFFFFFF;
    private static final int COLOR_LOW = 0xFFFF5252;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null && !AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)
                && (ACTION_REFRESH.equals(action) || action.startsWith("android.intent.action."))) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(new ComponentName(context, BatteryWidget.class));
            if (ids.length > 0) {
                onUpdate(context, manager, ids);
            }
            return;
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        int percent = readPercent(context);
        for (int id : ids) {
            updateOne(context, manager, id, percent);
        }
    }

    // Вызывается, когда виджет растянули или сжали пальцем
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager,
                                          int id, Bundle newOptions) {
        updateOne(context, manager, id, readPercent(context));
    }

    private static void updateOne(Context context, AppWidgetManager manager, int id, int percent) {
        String text = percent + "%";
        SpannableString span = new SpannableString(text);
        span.setSpan(new RelativeSizeSpan(0.5f), text.length() - 1, text.length(), 0);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
        views.setTextViewText(R.id.percent, span);
        views.setTextColor(R.id.percent, percent <= 15 ? COLOR_LOW : COLOR_NORMAL);
        views.setTextViewTextSize(R.id.percent, TypedValue.COMPLEX_UNIT_DIP,
                textSizeFor(context, manager, id));

        // Нажатие на виджет - обновить прямо сейчас
        Intent refresh = new Intent(context, BatteryWidget.class);
        refresh.setAction(ACTION_REFRESH);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, refresh, flags);
        views.setOnClickPendingIntent(R.id.root, pi);

        manager.updateAppWidget(id, views);
    }

    // Размер цифр подбирается под текущий размер виджета
    private static float textSizeFor(Context context, AppWidgetManager manager, int id) {
        Bundle options = manager.getAppWidgetOptions(id);
        boolean landscape = context.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        int w = options.getInt(landscape
                ? AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH
                : AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
        int h = options.getInt(landscape
                ? AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT
                : AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0);
        if (w <= 0 || h <= 0) {
            return 54f;
        }
        float byWidth = (w - 16) / 2.2f;   // запас под "100%"
        float byHeight = (h - 16) / 1.2f;
        float size = Math.min(byWidth, byHeight);
        if (size < 20f) {
            size = 20f;
        }
        if (size > 220f) {
            size = 220f;
        }
        return size;
    }

    private static int readPercent(Context context) {
        Intent battery = context.getApplicationContext()
                .registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery == null) {
            return 0;
        }
        int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        if (level < 0 || scale <= 0) {
            return 0;
        }
        return Math.round(level * 100f / scale);
    }
}
