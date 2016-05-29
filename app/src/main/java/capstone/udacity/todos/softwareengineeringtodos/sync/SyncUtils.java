/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package capstone.udacity.todos.softwareengineeringtodos.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import capstone.udacity.todos.softwareengineeringtodos.data.provider.TodosProvider;

/**
 * Static helper methods for working with the sync framework.
 */
public class SyncUtils {
    private static final String TAG = "SyncUtils";

    private static final int NET_CONNECT_TIMEOUT_MILLIS = 15000;  // 15 seconds
    private static final int NET_READ_TIMEOUT_MILLIS = 10000;  // 10 seconds

    private static final long SYNC_FREQUENCY = 60 * 30;  // Every 30 minutes.
    private static final String CONTENT_AUTHORITY = TodosProvider.AUTHORITY;
    private static final String PREF_SETUP_COMPLETE = "setup_complete";
    // Value below must match the account type specified in res/xml/syncadapter.xml
   public static final String ACCOUNT_TYPE = "capstone.udacity.todos.account";

    public static Account getAccount() {
        return new Account("todo-account", ACCOUNT_TYPE);
    }

    /**
     * Create an entry for this application in the system account list, if it isn't already there.
     *
     * @param context Context
     */
    @TargetApi(Build.VERSION_CODES.FROYO)
    public static void CreateSyncAccount(Context context) {
        boolean newAccount = false;
        boolean setupComplete = PreferenceManager
                .getDefaultSharedPreferences(context).getBoolean(PREF_SETUP_COMPLETE, false);

        // Create account, if it's missing. (Either first run, or user has deleted account.)
        Account account = getAccount();
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        if (accountManager.addAccountExplicitly(account, null, null)) {
            // Inform the system that this account supports sync
            ContentResolver.setIsSyncable(account, CONTENT_AUTHORITY, 1);
            // Inform the system that this account is eligible for auto sync when the network is up
            ContentResolver.setSyncAutomatically(account, CONTENT_AUTHORITY, true);
            // Recommend a schedule for automatic synchronization. The system may modify this based
            // on other scheduled syncs and network utilization.
            ContentResolver.addPeriodicSync(
                    account, CONTENT_AUTHORITY, new Bundle(),SYNC_FREQUENCY);
            newAccount = true;
        }

        // Schedule an initial sync if we detect problems with either our account or our local
        // data has been deleted. (Note that it's possible to clear app data WITHOUT affecting
        // the account list, so wee need to check both.)
        if (newAccount || !setupComplete) {
            TriggerRefresh();
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putBoolean(PREF_SETUP_COMPLETE, true).commit();
        }
    }

    /**
     * Helper method to trigger an immediate sync ("refresh").
     *
     * <p>This should only be used when we need to preempt the normal sync schedule. Typically, this
     * means the user has pressed the "refresh" button.
     *
     * Note that SYNC_EXTRAS_MANUAL will cause an immediate sync, without any optimization to
     * preserve battery life. If you know new data is available (perhaps via a GCM notification),
     * but the user is not actively waiting for that data, you should omit this flag; this will give
     * the OS additional freedom in scheduling your sync request.
     */
    public static void TriggerRefresh() {
        Bundle bnd = new Bundle();
        // Disable sync backoff and ignore sync preferences. In other words...perform sync NOW!
        bnd.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        bnd.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        // Set Sync account, Content authority, and Extras
        ContentResolver.requestSync(getAccount(), TodosProvider.AUTHORITY,bnd);
    }

    /**
     * Given a string representation of a URL, sets up a connection and gets an input stream.
     */
    public static InputStream httpURLInputStream(final URL url, String method) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(NET_READ_TIMEOUT_MILLIS /* milliseconds */);
        conn.setConnectTimeout(NET_CONNECT_TIMEOUT_MILLIS /* milliseconds */);
        conn.setRequestMethod(method);
        conn.setDoInput(true);
        // Starts the query
        conn.connect();
        return conn.getInputStream();
    }


    public static HttpURLConnection getURLConnection(String urlString, String method) throws Exception {
        Log.d(TAG, " getURLConnection with url: "+urlString+", with method: "+method);
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setConnectTimeout(NET_CONNECT_TIMEOUT_MILLIS); //Timeout to establish a connection
        conn.setReadTimeout(NET_READ_TIMEOUT_MILLIS); //Timeout to read for data
        conn.setRequestMethod(method); //Set method to GET or POST depending on the situation.
        conn.setDoInput(true); //Tells the client we accept input from the server

        return conn;
    }

    public static Exception uploadToServer(final String url, String method, final JSONObject jsonObject) {
        Exception error = null;
        Log.d(TAG, "uploadToServer: url: " + url);
        HttpURLConnection conn = null;
        try {
            conn = SyncUtils.getURLConnection(url,method);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.connect();

            byte[] buf = jsonObject.toString().getBytes("UTF-8");
            OutputStream os = conn.getOutputStream();
            os.write(buf);
            os.close();

            Log.d(TAG, "uploadToServer results: " + inputStreamToString(conn));
            return null;
        } catch (IOException e ) {
            e.printStackTrace(); //Problem with the data
            error = e;
        } catch (Exception e) {
            e.printStackTrace(); //Problem with the URL
            error = e;
        } finally {
            if(conn != null) {
                //Use disconnect to make sure the socket is closed when done.
                conn.disconnect();
            }
        }
        Log.d(TAG, "uploadToServer error returning: " + error);
        return error;
    }

    /**
     * Convenience method for parsing HTTP data.
     * @param {@link HttpURLConnection} - connection to the server
     */
    public static String inputStreamToString(HttpURLConnection connection) throws IOException
    {
        long time = System.currentTimeMillis(); //Instrumentation measurement
        InputStream is = null;
        try {
            is = connection.getInputStream();
        } catch (IOException ioe) {
            //Open errorStream to get error data from server.
            is = connection.getErrorStream();
            char[] buffer = new char[1024];
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));//,"UTF-8"));
            int read;
            while( (read = br.read(buffer)) != -1) {
                sb.append(buffer, 0, read);
            }
            br.close();
            is.close();
            Log.e(TAG, "inputStreamToString server error: " + sb.toString());
            throw ioe;
        }
        if(is == null)
            return "No response body from server.";
        int count = 0;
        char[] buffer = new char[1024];
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));//,"UTF-8"));
        int read;
        while( (read = br.read(buffer)) != -1) {
            sb.append(buffer, 0, read);
            count += read;
        }
        br.close();
        is.close();

        Log.d(TAG, "read: " + count + " chars, input length" + sb.length()
                + (System.currentTimeMillis() - time));
        return sb.toString();
    }

}
