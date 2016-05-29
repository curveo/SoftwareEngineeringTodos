package capstone.udacity.todos.softwareengineeringtodos.fragments;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import capstone.udacity.todos.softwareengineeringtodos.R;
import capstone.udacity.todos.softwareengineeringtodos.TodosActivity;
import capstone.udacity.todos.softwareengineeringtodos.TodosBaseActivity;
import capstone.udacity.todos.softwareengineeringtodos.data.provider.TodoItem;
import capstone.udacity.todos.softwareengineeringtodos.data.provider.TodosContent;

import static capstone.udacity.todos.softwareengineeringtodos.TodosBaseActivity.DEBUG;

public class TodoItemDetailFragment extends Fragment  implements LoaderManager.LoaderCallbacks<Cursor>,
        CompoundButton.OnCheckedChangeListener {

    public static final String TAG = TodoItemDetailFragment.class.getSimpleName();
    public static final String ARG_ITEM_ID = "item_id";

    private int mItemId = -1;
    private TodoItem mItem;
    private Uri mUri;

    @BindView(R.id.done_check_detail)
    CheckBox mDoneCheck;
    @BindView(R.id.todoitem_detail_title)
    TextView mTitleText;
    @BindView(R.id.todoitem_detail_notes)
    TextView mNotesTextView;

    public TodoItemDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = Integer.valueOf(getArguments().getString(ARG_ITEM_ID));
            mUri = TodosContent.Todos.CONTENT_URI.buildUpon().appendPath(String.valueOf(mItemId)).build();
        }
        updateTitleString(null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.todoitem_detail, container, false);
        ButterKnife.bind(this,rootView);

        return rootView;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getLoaderManager().initLoader(0, null, this);
        if (savedInstanceState != null) {
            mItemId = savedInstanceState.getInt("mItemId");
            updateTitleString(String.valueOf(mItemId));
        }
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("mItemId",mItemId);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        ((TodosBaseActivity) getActivity()).trackEvent("Action","Item Done Status Toggled with " + isChecked);

        Long timeStamp = System.currentTimeMillis();

        ContentValues updateContent = new ContentValues();
        updateContent.put(TodosContent.Todos.Columns.DONE.getName(), (isChecked)?1:0);
        updateContent.put(TodosContent.Todos.Columns.DATE.getName(), timeStamp);

        int row = getActivity().getContentResolver().update(TodosContent.Todos.CONTENT_URI.buildUpon()
                .appendPath(String.valueOf(mItemId)).build() , updateContent, null, null);
        Log.d(TAG, "onCheckChanged row: " + row);

    }

    private void updateTitleString(String withId) {
        CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout) getActivity().findViewById(R.id.toolbar_layout);
        if (appBarLayout != null) {
            String s = getString(R.string.detail_fragment_title) + ((null != withId) ? withId:"");
            appBarLayout.setTitle(s);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (null != mUri) {
            return new CursorLoader(
                    getActivity(),
                    mUri,
                    TodosContent.Todos.PROJECTION,
                    null,
                    null,
                    null
            );
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (DEBUG) {
            Log.d(TAG, "onLoadFinished: cursor count: " + data.getCount());
        }
        if (data.getCount() > 0 && data.moveToFirst()) {
            try {
                mItem = TodoItem.buildTodoFromCursor(data);
            } catch (Exception e) {
                Log.e(TAG, "Error instantiating TodoItem", e);
            }

        } else {
            // No result must have been updated or removed.
            mItem = null;
            Handler h = new Handler();
            h.post(new Runnable() {

                @Override
                public void run() {
                    if (getActivity() instanceof TodosActivity) {
                        ((TodosActivity)getActivity()).loadPlaceHolderFragment();
                    } else {
                        getActivity().onBackPressed();
                    }
                }
            });
            return;
        }

        updateTitleString(String.valueOf(mItem.getId()));

        mDoneCheck.setOnCheckedChangeListener(null);
        mDoneCheck.setChecked(mItem.isDone());
        mDoneCheck.setOnCheckedChangeListener(this);

        mTitleText.setText(mItem.getTitle());
        mNotesTextView.setText(mItem.getNtoes());
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d(TAG, "onLoaderReset");
    }

    @OnClick(R.id.edit_button)
    public void onEditButtonClicked() {
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        Fragment prev = getActivity().getSupportFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment newFragment = new EditTodoDialogFragment();

        Bundle args = new Bundle();
        args.putInt(EditTodoDialogFragment.ITEM_ID, mItemId);
        args.putString(EditTodoDialogFragment.TITLE, mTitleText.getText().toString());
        args.putString(EditTodoDialogFragment.NOTES, mNotesTextView.getText().toString());
        newFragment.setArguments(args);

        newFragment.show(ft, "dialog");
    }
}
