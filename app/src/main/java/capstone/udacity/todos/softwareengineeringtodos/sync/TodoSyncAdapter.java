package capstone.udacity.todos.softwareengineeringtodos.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import capstone.udacity.todos.softwareengineeringtodos.BuildConfig;
import capstone.udacity.todos.softwareengineeringtodos.data.provider.TodoItem;
import capstone.udacity.todos.softwareengineeringtodos.data.provider.TodosContent;
import capstone.udacity.todos.softwareengineeringtodos.widgets.TodoListWidgetProvider;

public class TodoSyncAdapter extends AbstractThreadedSyncAdapter {
    public static final String TAG = TodoSyncAdapter.class.getSimpleName();
    public static final boolean DEBUG = false;

    public static String TODO_BASE_URL_HOST = BuildConfig.SERVER_HOST;
    public static String TODO_BASE_URL = "http://" + TODO_BASE_URL_HOST + "/v1/api/";
    private static final String TODO_GET_PATH = "all/";
    private static final String TODO_UPDATE_PATH = "update/";
    private static final String TODO_ADD_PATH = "new";
    private static final String TODO_DELETE_PATH = "delete/";
    private static final String TODO_BULKDELETE_PATH = "bulkdelete/";

    private String androidID;

    public TodoSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        if (DEBUG) Log.i(TAG, "Beginning network synchronization");
        androidID = TodoItem.getUUIDForDevice(getContext());
        try {
            final URL todoUrl = new URL(TODO_BASE_URL + TODO_GET_PATH + androidID);
            if (DEBUG) Log.d(TAG, "onPerformSync URL is: " + todoUrl.toString());
            InputStream stream = null;

            try {
                if (DEBUG) Log.i(TAG, "Streaming data from network: " + todoUrl);
                stream = SyncUtils.httpURLInputStream(todoUrl, "GET");
                StringBuffer buffer = new StringBuffer();

                if (stream == null) {
                    // Nothing to do.
                    return;
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing
                    return;
                }
                String resultsJson = buffer.toString();
                parseResultsJson(resultsJson);
            } finally {
                if (stream != null) {
                    stream.close();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Feed URL is malformed", e);
            syncResult.stats.numParseExceptions++;
            return;
        } catch (JSONException e) {
            Log.e(TAG, "JSONException parsing return results.");
            syncResult.stats.numParseExceptions++;
            return;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing results.",e);
            syncResult.stats.numParseExceptions++;
            return;
        }

    }

    private void parseResultsJson(String json) throws Exception {
        JSONArray todosArray = new JSONArray(json);
        HashMap<Integer,TodoItem> entriesMap = new HashMap<Integer, TodoItem>();

        // 1. check local against remote
        HashMap<Integer,TodoItem> newOutgoingItems = null;

        // Server has results
        if(todosArray.length() > 0) {
            for(int i = 0; i < todosArray.length(); i++ ) {
                JSONObject todo = todosArray.getJSONObject(i);
                if (DEBUG) Log.d(TAG, "todo found: " + todo.toString(2));
                TodoItem item = new TodoItem(todo.getInt("id_remote"), todo.getString("title"),
                        todo.getString("notes"), ((todo.getInt("done") > 0)? true:false),
                        TodoItem.dateStringToTimeStamp(todo.getString("date"))
                        ,TodoItem.dateStringToTimeStamp(todo.getString("date_created")));
                item.setUuid(todo.getString("uuid"));
                entriesMap.put(item.getId(), item);

                if (DEBUG) Log.d(TAG, "Adding TodoItem: " + item + " to hashmap");
            }

           newOutgoingItems = getNewTodosNeedingServerPush(entriesMap);

            //2. Check for server updated items
            Vector<ContentValues> localUpdateVector  = getLocalUpdates(entriesMap);
            //3.  Check for local updated items.
            HashMap<Integer,TodoItem> serverUpdateMap = getServerUpdates(entriesMap);
            //4. Check for new remote items
            Vector<ContentValues> newIncomingItemsVector = new Vector<>();

            // New items are found from left over entriesMap items after getServerUpdates completes.
            for(Map.Entry<Integer,TodoItem> item: entriesMap.entrySet()) {
                if (newIncomingItemsVector == null) {
                    newIncomingItemsVector = new Vector<>();
                }
                newIncomingItemsVector.add(item.getValue().getTodoContentValues());
            }

            //2a. Update local with updates from server
            if(localUpdateVector != null) {
                for(ContentValues updateContent: localUpdateVector) {
                    if (DEBUG) Log.d(TAG, "COUNTS: Server has updated Items: " + localUpdateVector.size());
                    String selectionClause = "_id = ?";
                    String[] matchVals = {updateContent.getAsString(TodosContent.Todos.Columns.ID.getName())};
                    getContext().getContentResolver().update(TodosContent.Todos.CONTENT_URI, updateContent, selectionClause, matchVals);
                }
            }

            //3a. Update server with local updates
            if(serverUpdateMap != null) {
                if (DEBUG)  Log.d(TAG, "COUNTS: Local has updates count = " + serverUpdateMap.size());
                for (TodoItem item: serverUpdateMap.values().toArray(new TodoItem[serverUpdateMap.size()])) {
                    JSONObject body = item.toJsonObject(androidID);
                    SyncUtils.uploadToServer(TODO_BASE_URL + TODO_UPDATE_PATH + item.getId(), "POST", body);
                }
            }

            //4a. Add new entries to DB
            int inserted = 0;
            if (newIncomingItemsVector != null && newIncomingItemsVector.size() > 0) {
                if (DEBUG) Log.d(TAG, "COUNTS: Remote has NEW items: " + newIncomingItemsVector.size());
                ContentValues[] cvArray = new ContentValues[newIncomingItemsVector.size()];
                newIncomingItemsVector.toArray(cvArray);
                inserted = getContext().getContentResolver().bulkInsert(TodosContent.Todos.CONTENT_URI, cvArray);
                if (DEBUG) Log.d(TAG, "COUNTS: inserted id: " + inserted);

            }

            //1a. Push new local items to server.
            if (newOutgoingItems.size() > 0) {
                postNewItemsToServer(newOutgoingItems.values().toArray(new TodoItem[newOutgoingItems.size()]));
            }

            // Update widgets and other UIs observers that need to know about it.
            getContext().sendBroadcast(new Intent(TodoListWidgetProvider.APPWIDGET_UPDATE));

        } else {
            Log.w(TAG, "No todos found in the return results.");
            // Get results from local and push to server

            //1a. Push new local items to server.
            newOutgoingItems = getNewTodosNeedingServerPush(entriesMap);
            if (newOutgoingItems.size() > 0) {
                if (DEBUG) Log.d(TAG, "COUNTS: Local has new to push to server: " + newOutgoingItems.size());
                postNewItemsToServer(newOutgoingItems.values().toArray(new TodoItem[newOutgoingItems.size()]));
            }
        }

    }

    private Cursor getCursorOfCurrentTodos() {
        Log.i(TAG, "Fetching local entries for merge");
        Uri uri = TodosContent.Todos.CONTENT_URI; // Get all entries
        Cursor c = getContext().getContentResolver().query(uri, TodosContent.Todos.PROJECTION, null, null, null);
        assert c != null;
        Log.i(TAG, "COUNTS: Found " + c.getCount() + " local entries. Computing merge solution...");
        return c;
    }

    private void postNewItemsToServer(TodoItem[] todoItems) throws JSONException {
        if (DEBUG) Log.d(TAG, "postNewItemsToServer array size: " + todoItems.length);

        JSONArray objsArray = new JSONArray();
        for (TodoItem item: todoItems) {
            objsArray.put(item.toJsonObject(androidID));
        }
        JSONObject body = new JSONObject().put("multitodos",objsArray);
        if (DEBUG) Log.d(TAG, "postNewItemsToServer JSONArray: " +objsArray.toString(2));
        try {
            SyncUtils.uploadToServer(TODO_BASE_URL + TODO_ADD_PATH, "POST", body);
        } catch (Exception e) {
            Log.e(TAG, "Error posting to server with URL: " + TODO_BASE_URL + TODO_ADD_PATH, e);
        }
    }

    public static Exception deleteServerItem(Context context, String id) throws JSONException {
        if (DEBUG) Log.d(TAG, "deleteServerItem called with id: " + id);
        HashMap<String, String> itemMap = new HashMap<>();
        itemMap.put("uuid", TodoItem.getUUIDForDevice(context));
        JSONObject obj = new JSONObject(itemMap);

        Uri uri  = Uri.parse(TODO_BASE_URL + TODO_DELETE_PATH).buildUpon().appendPath(id).build();

        if (DEBUG) Log.d(TAG, "deleting with uri: " + uri + "\n with params: " + obj.toString(2));
        return SyncUtils.uploadToServer(uri.toString(),"DELETE",obj);
    }

    public static Exception bulkDeleteServerItems(JSONArray items) throws JSONException {
        if (DEBUG) Log.d(TAG, "bulkDeleteServerItems");

        JSONObject obj = new JSONObject();
        obj.put("multitodos",items);
        Uri uri = Uri.parse(TODO_BASE_URL + TODO_BULKDELETE_PATH);

        return SyncUtils.uploadToServer(uri.toString(), "DELETE", obj);
    }

    private HashMap<Integer, TodoItem> getNewTodosNeedingServerPush(HashMap<Integer, TodoItem> incomingEntriesMap) throws Exception {
        HashMap<Integer,TodoItem> serverAdditionsMap = new HashMap<>();

        Cursor c = getCursorOfCurrentTodos();
        while(c.moveToNext()) {
            TodoItem item = TodoItem.buildTodoFromCursor(c);
            if(incomingEntriesMap.get(item.getId()) == null) {
                serverAdditionsMap.put(item.getId(), item);
            }
        }

        return serverAdditionsMap;
    }

    private HashMap<Integer,TodoItem> getServerUpdates(HashMap<Integer,TodoItem> incomingEntriesMap) throws Exception {
        HashMap<Integer, TodoItem> serverUpdatesMap = new HashMap<>();
        if (DEBUG) Log.d(TAG, "getServerUpdates BEFORE entries count: " + incomingEntriesMap.size());

        Cursor c = getCursorOfCurrentTodos();

        while (c.moveToNext()) {
            TodoItem savedItem = TodoItem.buildTodoFromCursor(c);
            TodoItem matchedItem = incomingEntriesMap.remove(savedItem.getId());

            if(matchedItem != null) {
                if (DEBUG) Log.d(TAG, "getServerUpdates: MATCH FOUND!");
                //Check updated timestamp
                Date savedItemDate = new Date(savedItem.getTimeStamp());
                Date serverItemDate = new Date(matchedItem.getTimeStamp());
                if (savedItemDate.getTime() > serverItemDate.getTime()) {
                    //Local wins add to local update.
                    serverUpdatesMap.put(savedItem.getId(), savedItem);
                }
            }
        }

        if (DEBUG) Log.d(TAG, "getServerUpdates AFTER PARSE entries count: " + incomingEntriesMap.size());
        return serverUpdatesMap;
    }

    private Vector<ContentValues> getLocalUpdates(HashMap<Integer,TodoItem> entriesMap) throws Exception {
        HashMap<Integer,TodoItem> updateEntriesMap = new HashMap<Integer, TodoItem>();

        if (DEBUG) Log.d(TAG, "getLocalUpdates BEFORE incomingEntries.size: " + entriesMap.size());
        Cursor c = getCursorOfCurrentTodos();

        while (c.moveToNext()) {
            TodoItem savedItem = TodoItem.buildTodoFromCursor(c);
            // Get not remove(id) here because it will be removed by the server updates parsing.
            TodoItem matchedItem = entriesMap.get(savedItem.getId());
            if(matchedItem != null) {
                if (DEBUG) Log.d(TAG, "Match found for id: " + matchedItem.getId() + ", stored timestamp is: "
                        + matchedItem.getTimeStamp() + ", incoming timestamp: "
                        + c.getString(TodosContent.Todos.Columns.DATE.ordinal()));

                Date savedItemDate = new Date(savedItem.getTimeStamp());
                Date serverItemDate = new Date(matchedItem.getTimeStamp());
                if (serverItemDate.getTime() > savedItemDate.getTime()) {
                    //Server wins add to local update.
                    updateEntriesMap.put(matchedItem.getId(), matchedItem);
                }
            }
        }

        if (DEBUG) Log.d(TAG, "getLocalUpdates AFTER Local Updates needed: updateEntries.size: " + updateEntriesMap.size());

        Vector<ContentValues> updateVector = null;
        for(Map.Entry<Integer, TodoItem> item: updateEntriesMap.entrySet()) {
            if(updateVector == null) {
                updateVector = new Vector<>();
            }
            updateVector.add(item.getValue().getTodoContentValues());
        }
        return updateVector;
    }

}
