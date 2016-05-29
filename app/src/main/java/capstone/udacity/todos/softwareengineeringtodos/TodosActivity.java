package capstone.udacity.todos.softwareengineeringtodos;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.analytics.HitBuilders;

import org.json.JSONArray;
import org.json.JSONException;

import butterknife.BindView;
import butterknife.ButterKnife;
import capstone.udacity.todos.softwareengineeringtodos.data.provider.TodoItem;
import capstone.udacity.todos.softwareengineeringtodos.data.provider.TodosContent;
import capstone.udacity.todos.softwareengineeringtodos.fragments.TodoFragment;
import capstone.udacity.todos.softwareengineeringtodos.fragments.TodoItemDetailFragment;
import capstone.udacity.todos.softwareengineeringtodos.sync.SyncUtils;
import capstone.udacity.todos.softwareengineeringtodos.sync.TodoSyncAdapter;

public class TodosActivity extends TodosBaseActivity implements NavigationView.OnNavigationItemSelectedListener,
        TodoFragment.OnListFragmentInteractionListener {
    public static final String TAG = TodosActivity.class.getName();

    private boolean mTwoPane;
    private TextView mNoResultsText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todos);

        MobileAds.initialize(this, getString(R.string.banner_ad_unit_id));

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(TodosActivity.this, NewTodoActivity.class);
                startActivity(i);

            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mNoResultsText = (TextView) findViewById(R.id.noresults_textview);

        // Check if device is a tablet and should get a two pane layout in landscape
        if (findViewById(R.id.todoitem_detail_container) != null) {
            mTwoPane = true;
            loadPlaceHolderFragment();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        try {
            switch (id) {
                case R.id.clear_local_db:
                    confirmDelete(0);
                    break;
                case R.id.clear_server_db:
                    confirmDelete(1);
                    break;
                case R.id.clear_all_data:
                    confirmDelete(2);
                    break;
                case R.id.about_item:
                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                    Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
                    if (prev != null) {
                        ft.remove(prev);
                    }
                    ft.addToBackStack(null);
                    AboutFragment frag = new AboutFragment();
                    Bundle b = new Bundle();
                    b.putString(AboutFragment.UUID_STRING, getString(R.string.uuid_about,
                            TodoItem.getUUIDForDevice(this)));
                    frag.setArguments(b);

                    frag.show(ft, "dialog");

                    mTracker.send(new HitBuilders.EventBuilder()
                            .setCategory("Action")
                            .setAction("About Viewed")
                            .build());

                    break;
                case R.id.sync_server_data:
                    SyncUtils.TriggerRefresh();
                    break;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error clearing server data", e);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void confirmDelete(final int callback) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.alert_confirm_title))
                .setPositiveButton(getString(R.string.alert_confirm_positive), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        try {
                            switch (callback) {
                                case 0:
                                    removeLocalData();
                                    break;
                                case 1:
                                    removeServerData(false);
                                    break;
                                case 2:
                                    removeServerData(true);
                                    break;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error try again", e);
                        }
                    }
                })
                .setNegativeButton(getString(R.string.cancel_button_text), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
        dialog.show();
    }


    private void removeLocalData() {
        int rowsDeleted = getContentResolver().delete(TodosContent.Todos.CONTENT_URI, null, null);
        if (DEBUG) {
            Log.d(TAG, "rowsDeleted: " + rowsDeleted);
        }
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Action")
                .setAction("Delete Local Database")
                .build());
    }

    private void removeServerData(boolean clearLocalDB) throws Exception {
        Cursor c = getContentResolver().query(TodosContent.Todos.CONTENT_URI, TodosContent.Todos.PROJECTION, null, null, null);
        if (c != null) {
            TodoItem[] items = new TodoItem[c.getCount()];
            JSONArray jArr = new JSONArray();
            for (int i = 0; i < c.getCount(); i++) {
                if (c.moveToPosition(i)) {
                    items[i] = TodoItem.buildTodoFromCursor(c);
                }
            }
            for (TodoItem item : items) {
                jArr.put(item.toJsonObject(TodoItem.getUUIDForDevice(this)));
            }

            new BulkDeleteAsyncTask().execute(jArr, clearLocalDB);
        }
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Action")
                .setAction("Delete Server Data")
                .build());
    }

    class BulkDeleteAsyncTask extends AsyncTask<Object, Void, Exception> {

        boolean mDeleteLocal;

        @Override
        protected Exception doInBackground(Object... params) {
            Exception exception = null;

            JSONArray jArr = (JSONArray) params[0];
            mDeleteLocal = (boolean) params[1];

            try {
                exception = TodoSyncAdapter.bulkDeleteServerItems(jArr);
            } catch (JSONException e) {
                Log.d(TAG, "Error in bulk delete", e);
                return e;
            }

            return exception;
        }

        @Override
        protected void onPostExecute(Exception e) {
            String msg;
            if (e == null) {
                msg = "Successfully deleted server items!";
                if (mDeleteLocal) {
                    removeLocalData();
                }
            } else {
                msg = "Error in bulk delete: " + e.getLocalizedMessage();
            }
            Toast.makeText(TodosActivity.this, msg, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    public void loadPlaceHolderFragment() {
        Fragment fragment = new PlaceHolderFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.todoitem_detail_container, fragment)
                .commit();
    }

    @Override
    public void onListFragmentInteraction(TodoItem item) {
        if (DEBUG) {
            Log.d(TAG, "onListFragmentInteraction called " + item.getTitle() + ", " + item.getNtoes()
                    + ", " + item.isDone());
        }
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Action")
                .setAction("Item Selected")
                .build());

        if (mTwoPane) {
            Bundle arguments = new Bundle();
            arguments.putString(TodoItemDetailFragment.ARG_ITEM_ID, String.valueOf(item.getId()));
            TodoItemDetailFragment fragment = new TodoItemDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.todoitem_detail_container, fragment)
                    .addToBackStack(null)
                    .commit();
        } else {
            Intent i = new Intent(this, TodoItemDetailActivity.class);
            i.putExtra(TodoItemDetailFragment.ARG_ITEM_ID, String.valueOf(item.getId()));
            startActivity(i);
        }
    }

    @Override
    public void onListResultsCountUpdated(int dataCount) {
        mNoResultsText.setVisibility((dataCount > 0) ? View.GONE : View.VISIBLE);
    }

    public static class AboutFragment extends DialogFragment {

        public static final String UUID_STRING = "uuid";

        @BindView(R.id.about_uuid)
        TextView mUUIDTextView;

        public AboutFragment() {

        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.about_dialog_fragment, container, false);
            ButterKnife.bind(this, v);

            String uuid = getArguments().getString(UUID_STRING);
            mUUIDTextView.setText(uuid);

            return v;
        }
    }

    public static class PlaceHolderFragment extends Fragment {

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.placeholder_fragment, container, false);
        }
    }

}
