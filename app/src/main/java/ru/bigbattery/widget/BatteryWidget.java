package ru.bigbattery.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.widget.RemoteViews;

public class BatteryWidget extends AppWidgetProvider {

    private static final String ACTION_REFRESH = "ru.bigbattery.widget.REFRESH";

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

        String text = percent + "%";
        SpannableString span = new SpannableString(text);
        span.setSpan(new RelativeSizeSpan(0.5f), text.length() - 1, text.length(), 0);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
        views.setTextViewText(R.id.percent, span);
        views.setTextColor(R.id.percent, percent <= 15 ? 0xFFFF5252 : 0xFFFFFFFF);

        // Нажатие на виджет - обновить прямо сейчас
        Intent refresh = new Intent(context, BatteryWidget.class);
        refresh.setAction(ACTION_REFRESH);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, refresh, flags);
        views.setOnClickPendingIntent(R.id.root, pi);

        for (int id : ids) {
            manager.updateAppWidget(id, views);
        }
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
