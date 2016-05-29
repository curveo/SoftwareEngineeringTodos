package capstone.udacity.todos.softwareengineeringtodos.widgets;

import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import capstone.udacity.todos.softwareengineeringtodos.R;
import capstone.udacity.todos.softwareengineeringtodos.TodosBaseActivity;
import capstone.udacity.todos.softwareengineeringtodos.data.provider.TodosContent;
import capstone.udacity.todos.softwareengineeringtodos.fragments.TodoItemDetailFragment;

public class TodoRemoteViewsService extends RemoteViewsService {

    public static final String TAG = TodoRemoteViewsService.class.getName();

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {

            private Cursor data = null;

            @Override
            public void onCreate() {
                // Nothing to do
            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }
                final long identityToken = Binder.clearCallingIdentity();

                data = getContentResolver().query(TodosContent.Todos.CONTENT_URI, // URI
                        TodosContent.Todos.PROJECTION,            // Projection
                        TodosContent.Todos.Columns.DONE + " = ?", // Selection
                        new String[]{"0"},                        // Selection args
                        TodosContent.Todos.Columns.DONE + " ASC, " /* Sort by Done status first */
                                + TodosContent.Todos.Columns.DATE_CREATED + " DESC"); // Then sort by date created)

                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                int count = (data == null) ? 0 : data.getCount();
                if (TodosBaseActivity.DEBUG)
                    Log.d(TAG, "getCount: " + count);
                return count;
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (TodosBaseActivity.DEBUG)

                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }
                RemoteViews views = new RemoteViews(getPackageName(),
                        R.layout.widget_todo_list_item);

                views.setTextViewText(R.id.title_text, data.getString(TodosContent.Todos.Columns.TITLE.ordinal()));
                views.setTextViewText(R.id.notes_text, data.getString(TodosContent.Todos.Columns.NOTES.ordinal()));

                final Intent fillInIntent = new Intent();
                int itemId = data.getInt(TodosContent.Todos.Columns.ID.ordinal());
                fillInIntent.putExtra(TodoItemDetailFragment.ARG_ITEM_ID, String.valueOf(itemId));
                views.setOnClickFillInIntent(R.id.metadata_linear_layout, fillInIntent);

                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_todo_list_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (TodosBaseActivity.DEBUG)
                    Log.d(TAG, "getItemId");
                if (data.moveToPosition(position)) {
                    return data.getLong(TodosContent.Todos.Columns.ID.ordinal());
                }
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
