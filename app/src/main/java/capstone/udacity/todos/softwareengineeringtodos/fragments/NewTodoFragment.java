package capstone.udacity.todos.softwareengineeringtodos.fragments;

import android.content.ContentValues;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import butterknife.BindView;
import butterknife.ButterKnife;
import capstone.udacity.todos.softwareengineeringtodos.R;
import capstone.udacity.todos.softwareengineeringtodos.TodosBaseActivity;
import capstone.udacity.todos.softwareengineeringtodos.data.provider.TodoItem;
import capstone.udacity.todos.softwareengineeringtodos.data.provider.TodosContent;

public class NewTodoFragment extends Fragment implements View.OnClickListener {

    public static final String TAG = NewTodoFragment.class.getSimpleName();

    @BindView(R.id.button_save) Button mSaveButton;
    @BindView(R.id.button_cancel) Button mCancelButton;
    @BindView(R.id.title_edit_textview) EditText mEditTitle;
    @BindView(R.id.notes_edit_textview) EditText mEditNotes;

    public NewTodoFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_new_todo, container, false);
        ButterKnife.bind(this, v);

        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mSaveButton.setOnClickListener(this);
        mCancelButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == mSaveButton) {
            View invalidView = getInvalidInputView();
            if(invalidView == null) {
                Uri inserted = getContext().getContentResolver().insert(TodosContent.Todos.CONTENT_URI,
                        getInsertValues());
                Log.d(TAG, "Inserted item: " + inserted);
            } else {
                Snackbar.make(invalidView, "Please add text",Snackbar.LENGTH_SHORT).show();
                invalidView.requestFocus();
                return;
            }
        } else if (v == mCancelButton) {
            mEditTitle.setText("");
            mEditNotes.setText("");
        }
        getActivity().onBackPressed();

        ((TodosBaseActivity) getActivity()).trackEvent("Action",
                (v == mSaveButton) ? "Create Save Clicked":"Cancel Create Clicked");
    }

    private View getInvalidInputView() {
        if (mEditTitle.getText().length() == 0) {
            return mEditTitle;
        }
        return null;
    }

    private ContentValues getInsertValues() {
        ContentValues todoValues = new ContentValues();
        todoValues.put(TodosContent.Todos.Columns.UUID.getName(), TodoItem.getUUIDForDevice(getContext()));
        todoValues.put(TodosContent.Todos.Columns.DONE.getName(), 0); // Mark new one not done
        todoValues.put(TodosContent.Todos.Columns.TITLE.getName(), mEditTitle.getText().toString());
        todoValues.put(TodosContent.Todos.Columns.NOTES.getName(), mEditNotes.getText().toString());
        // Timestamp current time for creation date.
        Long createdTimeStamp = System.currentTimeMillis();
        todoValues.put(TodosContent.Todos.Columns.DATE.getName(), createdTimeStamp);
        todoValues.put(TodosContent.Todos.Columns.DATE_CREATED.getName(), createdTimeStamp);

        return todoValues;
    }
}
