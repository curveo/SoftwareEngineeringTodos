package capstone.udacity.todos.softwareengineeringtodos.fragments;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;

import butterknife.BindView;
import butterknife.ButterKnife;
import capstone.udacity.todos.softwareengineeringtodos.R;
import capstone.udacity.todos.softwareengineeringtodos.TodosBaseActivity;
import capstone.udacity.todos.softwareengineeringtodos.data.provider.TodosContent;
import capstone.udacity.todos.softwareengineeringtodos.sync.TodoSyncAdapter;

import static capstone.udacity.todos.softwareengineeringtodos.TodosBaseActivity.DEBUG;

/**
 * Created by curtis on 5/14/16.
 */
public class EditTodoDialogFragment extends DialogFragment implements View.OnClickListener {

    public static final String TAG = EditTodoDialogFragment.class.getSimpleName();

    public static final String ITEM_ID = "itemid_args";
    public static final String TITLE = "title_args";
    public static final String NOTES = "notes_args";

    @BindView(R.id.title_edit_textview)
    EditText mTitleText;
    @BindView(R.id.notes_edit_textview)
    EditText mNotesText;
    @BindView(R.id.save_button)
    Button mSaveButton;
    @BindView(R.id.cancel_button)
    Button mCancelButton;
    @BindView(R.id.delete_button)
    Button mDeleteButton;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int style = DialogFragment.STYLE_NO_TITLE;
        int theme = android.R.style.Theme_Material_Dialog_Alert;
        setStyle(style, theme);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_fragment_edit_todo, container, false);
        ButterKnife.bind(this,v);

        mTitleText.setText(getArguments().getString(TITLE));
        mNotesText.setText(getArguments().getString(NOTES));

        mSaveButton.setOnClickListener(this);
        mCancelButton.setOnClickListener(this);
        mDeleteButton.setOnClickListener(this);

        return v;
    }

    @Override
    public void onClick(View v) {
        String trackString = "unknown";
        if (v == mSaveButton) {
            if (DEBUG) {
                Log.d("DIALOG", "save button clicked");
            }
            trackString = "Save";
            View invalidView = getInvalidInputView();
            if (invalidView == null) {
                Long timeStamp = System.currentTimeMillis();

                ContentValues updateContent = new ContentValues();
                updateContent.put(TodosContent.Todos.Columns.TITLE.getName(), mTitleText.getText().toString());
                updateContent.put(TodosContent.Todos.Columns.NOTES.getName(), mNotesText.getText().toString());
                updateContent.put(TodosContent.Todos.Columns.DATE.getName(), timeStamp);

                if (DEBUG) {
                    Log.d(TAG, "save button clicked with contentvalues: title: "
                            + updateContent.getAsString(TodosContent.Todos.Columns.TITLE.getName()
                            + ", notes " + updateContent.getAsString(TodosContent.Todos.Columns.NOTES.getName()
                            + ", date: " + updateContent.getAsString(TodosContent.Todos.Columns.DATE.getName()))));
                }
                int rows = getContext().getContentResolver().update(TodosContent.Todos.CONTENT_URI.buildUpon()
                        .appendPath(String.valueOf(getArguments().getInt(ITEM_ID))).build() , updateContent, null, null);
                if (DEBUG) {
                    Log.d(TAG, "saved rows: " + rows);
                }
                dismiss();

            } else {
                Snackbar.make(invalidView, "Please add text",Snackbar.LENGTH_SHORT).show();
                invalidView.requestFocus();
                return; //Return and do not send analytic action
            }
        } else if (v == mCancelButton) {
            trackString = "Cancel";
            dismiss();
        } else if (v == mDeleteButton) {
            if (DEBUG) {
                Log.d("DIALOG", "delete button clicked");
            }
            trackString = "Delete";

            AlertDialog confirm = new AlertDialog.Builder(getActivity())
                    .setTitle(getString(R.string.alert_confirm_title))
                    .setPositiveButton(getString(R.string.delete_button_text), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            new DeleteItemFromServerTask().execute(String.valueOf(getArguments().getInt(ITEM_ID)));
                        }
                    }).setNegativeButton(getString(R.string.cancel_button_text), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).create();

            confirm.show();
        }

        ((TodosBaseActivity) getActivity()).trackEvent("Action",trackString + " clicked in Edit Dialog");
    }

    private View getInvalidInputView() {
        if (mTitleText.getText().length() == 0) {
            return mTitleText;
        }
        return null;
    }

    private class DeleteItemFromServerTask extends AsyncTask<String,Void,Exception> {

        @Override
        protected Exception doInBackground(String... params) {
            Exception returnedException = null;
            try {
                returnedException = TodoSyncAdapter.deleteServerItem(getContext(), params[0]);
            } catch (JSONException e) {
                return e;
            }
            return returnedException;
        }

        @Override
        protected void onPostExecute(Exception e) {
            if (e != null) {
                Toast.makeText(getContext(), getString(R.string.error_server_delete_todo),Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error in async task", e);
            } else {
                Uri uri = TodosContent.Todos.CONTENT_URI.buildUpon()
                        .appendPath(String.valueOf(getArguments().getInt(ITEM_ID)))
                        .build();
                int rows = getActivity().getContentResolver().delete(uri, null, null);
                if (DEBUG) {
                    Log.d(TAG, "deleted rows affected: " + rows);
                }
                dismiss();
            }
        }
    }
}
