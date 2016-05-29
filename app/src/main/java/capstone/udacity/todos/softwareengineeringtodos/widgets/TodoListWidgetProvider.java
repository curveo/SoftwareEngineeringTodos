package capstone.udacity.todos.softwareengineeringtodos.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.RemoteViews;

import capstone.udacity.todos.softwareengineeringtodos.R;
import capstone.udacity.todos.softwareengineeringtodos.TodoItemDetailActivity;
import capstone.udacity.todos.softwareengineeringtodos.TodosActivity;

public class TodoListWidgetProvider extends AppWidgetProvider {

    public static final String APPWIDGET_UPDATE = "android.appwidget.action.APPWIDGET_UPDATE";
    public static final String APPWIDGET_UPDATE_OPTIONS = "android.appwidget.action.APPWIDGET_UPDATE_OPTIONS";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("TodoListWidgetProvider", "onReceive");
        super.onReceive(context, intent);
        if (APPWIDGET_UPDATE.equals(intent.getAction())
                || APPWIDGET_UPDATE_OPTIONS.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, getClass()));
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d("TodoListWidgetProvider", "onUpdATE");
        for (int appWidgetId : appWidgetIds) {
            Intent intent = new Intent(context, TodoRemoteViewsService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.todo_app_widget);

            views.setRemoteAdapter(R.id.widget_list, intent);
            views.setEmptyView(R.id.widget_list, R.id.widget_empty);

            // PendingIntent to show TodosActivity when title or no results clicked
            PendingIntent startAppPendingIntent = PendingIntent.getActivity(context, 0,
                    new Intent(context, TodosActivity.class), 0);
            views.setOnClickPendingIntent(R.id.widget_header_text, startAppPendingIntent);
            views.setOnClickPendingIntent(R.id.widget_empty, startAppPendingIntent);

            // PendingIntent to launch detail activity.
            PendingIntent pendingIntent = TaskStackBuilder.create(context)
                    .addNextIntent(new Intent(context, TodoItemDetailActivity.class))
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

            views.setPendingIntentTemplate(R.id.widget_list, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }
}
