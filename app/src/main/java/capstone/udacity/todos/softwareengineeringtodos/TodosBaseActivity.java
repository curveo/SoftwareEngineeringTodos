package capstone.udacity.todos.softwareengineeringtodos;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class TodosBaseActivity extends AppCompatActivity {


    public static final boolean DEBUG = true; // Used to switch logging on/off in activites
    protected Tracker mTracker;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Start Google Analytics tracking
        ((TODOSApplication) getApplication()).startTracking();
        mTracker = ((TODOSApplication) getApplication()).getTracker();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String shortName = this.getClass().getSimpleName();
        mTracker.setScreenName(shortName);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    /**
     * Convenience method to track an event from a fragment
     * @param category of event EX: "Action"
     * @param name of event EX: "Button Clicked"
     */
    public void trackEvent(String category, String name) {
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(name)
                .build());
    }
}
