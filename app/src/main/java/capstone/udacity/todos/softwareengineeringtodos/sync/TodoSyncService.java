package capstone.udacity.todos.softwareengineeringtodos.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class TodoSyncService extends Service {
    public static final String TAG = TodoSyncService.class.getSimpleName();

    private static final Object sSyncAdapterLock = new Object();
    private static TodoSyncAdapter sTodoSyncAdapter = null;

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate - TodoSyncService");
        synchronized (sSyncAdapterLock) {
            if (sTodoSyncAdapter == null) {
                sTodoSyncAdapter = new TodoSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sTodoSyncAdapter.getSyncAdapterBinder();
    }
}