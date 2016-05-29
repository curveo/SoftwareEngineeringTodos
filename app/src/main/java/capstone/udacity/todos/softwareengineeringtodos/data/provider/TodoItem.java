package capstone.udacity.todos.softwareengineeringtodos.data.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.provider.Settings;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TodoItem {

    public static final String TODO_SERVER_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final String TODO_SERVER_DATE_OUTGOING_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    private int mId;
    private String mUuid;
    private String mTitle;
    private String mNotes;
    private int mDone;
    private Long mTimeStamp;
    private Long mCreatedTimeStamp;

    public TodoItem(int id, String title, String notes, boolean done, Long timeStamp, Long createdTimeStamp) {
        this.mId = id;
        this.mTitle = title;

        this.mNotes = notes;
        this.mDone = (done)?1:0;
        this.mTimeStamp = timeStamp;
        mCreatedTimeStamp = createdTimeStamp;
    }

    public int getId() {
        return mId;
    }

    public String getUuid() {
        return mUuid;
    }

    public String getTitle() {
        return mTitle;
    }

    public int isDoneInt() {
        return mDone;
    }

    public boolean isDone() {
        return (mDone == 1)?true:false;
    }

    public String getNtoes() {
        return mNotes;
    }

    public Long getTimeStamp() {
        return mTimeStamp;
    }

    public Long getCreatedTimeStamp() {
        return mCreatedTimeStamp;
    }

    public JSONObject toJsonObject(String uuid) throws JSONException {
        JSONObject jsonObj = new JSONObject();

        jsonObj.put("id",mId);
        jsonObj.put("uuid", uuid);
        jsonObj.put("done",isDoneInt());
        jsonObj.put("title",mTitle);
        jsonObj.put("notes",mNotes);
        jsonObj.put("date_modified",TodoItem.timeStampLongToDateString(mTimeStamp));
        jsonObj.put("date_created", TodoItem.timeStampLongToDateString(mCreatedTimeStamp));

        return jsonObj;
    }

    public void setUuid(String uuid) {
        this.mUuid = uuid;
    }

    public ContentValues getTodoContentValues() {
        ContentValues todoValues = new ContentValues();

        todoValues.put(TodosContent.Todos.Columns.ID.getName(), getId());
        todoValues.put(TodosContent.Todos.Columns.UUID.getName(), getUuid());
        todoValues.put(TodosContent.Todos.Columns.DONE.getName(), isDoneInt());
        todoValues.put(TodosContent.Todos.Columns.TITLE.getName(), getTitle());
        todoValues.put(TodosContent.Todos.Columns.NOTES.getName(), getNtoes());
        todoValues.put(TodosContent.Todos.Columns.DATE.getName(), getTimeStamp());
        todoValues.put(TodosContent.Todos.Columns.DATE_CREATED.getName(), getCreatedTimeStamp());

        return todoValues;
    }

    public static TodoItem buildTodoFromCursor(Cursor c) throws Exception {
        int id = c.getInt(TodosContent.Todos.Columns.ID.ordinal());
        String title = c.getString(TodosContent.Todos.Columns.TITLE.ordinal());
        String notes = c.getString(TodosContent.Todos.Columns.NOTES.ordinal());
        int done = c.getInt(TodosContent.Todos.Columns.DONE.ordinal());
        Long modifiedTimestamp = c.getLong(TodosContent.Todos.Columns.DATE.ordinal());
        Long createTimeStamp = c.getLong(TodosContent.Todos.Columns.DATE_CREATED.ordinal());

        if (id == 0 || title == null || modifiedTimestamp == null || createTimeStamp == null) {
            throw new Exception("Error parsing object from cursor");
        }

        TodoItem todoItem = new TodoItem(id,title,notes,(done == 0)?false:true,modifiedTimestamp, createTimeStamp);

        return todoItem;
    }

    public static Long dateStringToTimeStamp(String timeStamp) throws ParseException {
        SimpleDateFormat simpleDate = new SimpleDateFormat(TODO_SERVER_DATE_FORMAT, Locale.US);
        Long time =  simpleDate.parse(timeStamp).getTime();
        return time;
    }

    public static String timeStampLongToDateString(Long timeStamp) {
        SimpleDateFormat simpleDate = new SimpleDateFormat(TODO_SERVER_DATE_OUTGOING_FORMAT, Locale.US);
        String dateString = simpleDate.format(new Date(timeStamp));
        return dateString;
    }

    public static String getUUIDForDevice(Context context) {
        return Settings.Secure.getString(context.getContentResolver(),Settings.Secure.ANDROID_ID);
    }
}
