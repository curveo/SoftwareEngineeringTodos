package capstone.udacity.todos.softwareengineeringtodos;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import capstone.udacity.todos.softwareengineeringtodos.data.provider.TodoItem;
import capstone.udacity.todos.softwareengineeringtodos.fragments.TodoFragment.OnListFragmentInteractionListener;
import capstone.udacity.todos.softwareengineeringtodos.data.provider.TodosContent;

public class TodoRecyclerViewAdapter extends RecyclerView.Adapter<TodoRecyclerViewAdapter.ViewHolder> {

    public static final String TAG = TodoRecyclerViewAdapter.class.getSimpleName();

    private Context mContext;
    private Cursor mCursor;

    private final OnListFragmentInteractionListener mListener;

    public TodoRecyclerViewAdapter(Context context, Cursor data, OnListFragmentInteractionListener listener) {
        mContext = context;
        mCursor = data;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_todo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        mCursor.moveToPosition(position);
        String title = mCursor.getString(TodosContent.Todos.Columns.TITLE.ordinal());
        String notes = mCursor.getString(TodosContent.Todos.Columns.NOTES.ordinal());
        final boolean done = (mCursor.getInt(TodosContent.Todos.Columns.DONE.ordinal()) == 0) ? false:true;

        holder.mId = (mCursor.getInt(TodosContent.Todos.Columns.ID.ordinal()));
        holder.mTitleLabel.setText(title);
        holder.mNotesLabel.setText(notes);

        holder.mDoneCheck.setOnCheckedChangeListener(null);
        holder.mDoneCheck.setChecked(done);

        holder.mDoneCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(done == isChecked)
                    return;

                Long timeStamp = System.currentTimeMillis();
                int id = holder.mId;
                ContentValues updateContent = new ContentValues();
                updateContent.put(TodosContent.Todos.Columns.DONE.getName(), (isChecked)?1:0);
                updateContent.put(TodosContent.Todos.Columns.DATE.getName(), timeStamp);

                if (TodosBaseActivity.DEBUG) {
                    Log.d(TAG, "onCheckChanged: checked: " + updateContent.getAsInteger(TodosContent.Todos.Columns.DONE.getName()
                            + ", date: " + updateContent.getAsString(TodosContent.Todos.Columns.DATE.getName())));
                }

                int row = mContext.getContentResolver().update(TodosContent.Todos.CONTENT_URI.buildUpon()
                        .appendPath(String.valueOf(id)).build() , updateContent, null, null);
                Log.d(TAG, "onCheckChanged row: " + row);
            }
        });

        holder.mId = mCursor.getInt(TodosContent.Todos.Columns.ID.ordinal());
        holder.setTimeStamps(mCursor.getLong(TodosContent.Todos.Columns.DATE.ordinal()),
                mCursor.getLong(TodosContent.Todos.Columns.DATE_CREATED.ordinal()));

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    TodoItem item = new TodoItem(holder.mId,holder.mTitleLabel.getText().toString(),
                            holder.mNotesLabel.getText().toString(), holder.mDoneCheck.isChecked(),
                            holder.mTimeStamp, holder.mCreatedTimeStamp);
                    mListener.onListFragmentInteraction(item);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        if(null == mCursor) {
            return 0;
        }
        return mCursor.getCount();
    }

    public void changeCursor(Cursor data) {
        mCursor = data;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public int mId;
        public Long mTimeStamp;
        public Long mCreatedTimeStamp;

        public final TextView mTitleLabel;
        public final TextView mNotesLabel;
        public final CheckBox mDoneCheck;

        public final View mView;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mTitleLabel = (TextView) view.findViewById(R.id.title_text);
            mNotesLabel = (TextView) view.findViewById(R.id.notes_text);
            mDoneCheck = (CheckBox) view.findViewById(R.id.done_check);
        }

        public void setTimeStamps(Long modifiedTimeStamp, Long createdTimeStamp) {
            mTimeStamp = modifiedTimeStamp;
            mCreatedTimeStamp = createdTimeStamp;
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mTitleLabel.getText() + "'";
        }
    }
}
