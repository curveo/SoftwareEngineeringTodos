package capstone.udacity.todos.softwareengineeringtodos;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;

public class NewTodoActivity extends TodosBaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_todo);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

}
