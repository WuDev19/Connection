package com.example.connection.data.worker;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.example.connection.constants.SharePreUtil;

public class WidgetProvider extends AppWidgetProvider {
    public static final String ACTION_REFRESH = "com.example.connection.WIDGET_REFRESH";

    @Override public void onEnabled(Context context) {
        super.onEnabled(context);
        SharePreUtil.getInstance(context).setWidget(true);
        WidgetUpdater.schedulePeriodic(context);
        WidgetSyncCoordinator.get(context).reconcile();
    }

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) WidgetUpdater.initializeIfNeeded(context, id);
        WidgetUpdater.schedulePeriodic(context);
        WidgetUpdater.requestRefresh(context);
        WidgetSyncCoordinator.get(context).reconcile();
    }

    @Override public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager, int id, Bundle options) {
        super.onAppWidgetOptionsChanged(context, manager, id, options);
        WidgetUpdater.requestRefresh(context);
    }

    @Override public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent != null && ACTION_REFRESH.equals(intent.getAction())) {
            int requested = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            for (int id : WidgetUpdater.widgetIds(context)) {
                if (id == requested) { WidgetUpdater.requestRefresh(context); break; }
            }
        }
    }

    @Override public void onDisabled(Context context) {
        super.onDisabled(context);
        SharePreUtil.getInstance(context).setWidget(false);
        WidgetUpdater.cancel(context);
        WidgetSyncCoordinator.get(context).reconcile();
    }

    @Override public void onDeleted(Context context, int[] ids) {
        for (int id : ids) WidgetUpdater.forget(context, id);
        super.onDeleted(context, ids);
    }

    @Override public void onRestored(Context context, int[] oldIds, int[] newIds) {
        for (int id : oldIds) WidgetUpdater.forget(context, id);
        for (int id : newIds) WidgetUpdater.initialize(context, id);
        WidgetUpdater.requestRefresh(context);
    }
}
