package capstone.udacity.todos.softwareengineeringtodos.data.provider;

import capstone.udacity.todos.softwareengineeringtodos.data.provider.util.ColumnMetadata;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;


public abstract class TodosContent {

    public static final Uri CONTENT_URI = Uri.parse("content://" + TodosProvider.AUTHORITY);

    private TodosContent() {
    }

    /**
     * Created in version 1
     */
    public static final class Todos extends TodosContent {

        private static final String LOG_TAG = Todos.class.getSimpleName();

        public static final String TABLE_NAME = "todos";
        public static final String TYPE_ELEM_TYPE = "vnd.android.cursor.item/todos-todos";
        public static final String TYPE_DIR_TYPE = "vnd.android.cursor.dir/todos-todos";

        public static final Uri CONTENT_URI = Uri.parse(TodosContent.CONTENT_URI + "/" + TABLE_NAME);

        public enum Columns implements ColumnMetadata {
            ID(BaseColumns._ID, "integer"),
            UUID("uuid", "text"),
            DONE("done", "integer"),
            TITLE("title", "text"),
            NOTES("notes", "text"),
            DATE("date", "integer"),
            DATE_CREATED("date_created", "integer");


            private final String mName;
            private final String mType;

            Columns(String name, String type) {
                mName = name;
                mType = type;
            }

            @Override
            public int getIndex() {
                return ordinal();
            }

            @Override
            public String getName() {
                return mName;
            }

            @Override
            public String getType() {
                return mType;
            }
        }

        public static final String[] PROJECTION = new String[] {
                Columns.ID.getName(),
                Columns.UUID.getName(),
                Columns.DONE.getName(),
                Columns.TITLE.getName(),
                Columns.NOTES.getName(),
                Columns.DATE.getName(),
                Columns.DATE_CREATED.getName()
        };

        private Todos() {
            // No private constructor
        }

        public static void createTable(SQLiteDatabase db) {
            if (TodosProvider.ACTIVATE_ALL_LOGS) {
                Log.d(LOG_TAG, "Todos | createTable start");
            }
            db.execSQL("CREATE TABLE " + TABLE_NAME + " (" + Columns.ID.getName() + " " + Columns.ID.getType()
                    + ", " + Columns.UUID.getName() + " " + Columns.UUID.getType() + ", " + Columns.DONE.getName()
                    + " " + Columns.DONE.getType() + ", " + Columns.TITLE.getName() + " " + Columns.TITLE.getType()
                    + ", " + Columns.NOTES.getName() + " " + Columns.NOTES.getType() + ", " + Columns.DATE.getName()
                    + " " + Columns.DATE.getType() + ", " + Columns.DATE_CREATED.getName()
                    + " " + Columns.DATE_CREATED.getType() + ", PRIMARY KEY (" + Columns.ID.getName() + ")" + ");");

            db.execSQL("CREATE INDEX todos_uuid on " + TABLE_NAME + "(" + Columns.UUID.getName() + ");");
            db.execSQL("CREATE INDEX todos_done on " + TABLE_NAME + "(" + Columns.DONE.getName() + ");");
            db.execSQL("CREATE INDEX todos_title on " + TABLE_NAME + "(" + Columns.TITLE.getName() + ");");
            if (TodosProvider.ACTIVATE_ALL_LOGS) {
                Log.d(LOG_TAG, "Todos | createTable end");
            }
        }

        // Version 1 : Creation of the table
        public static void upgradeTable(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (TodosProvider.ACTIVATE_ALL_LOGS) {
                Log.d(LOG_TAG, "Todos | upgradeTable start");
            }

            if (oldVersion < 1) {
                Log.i(LOG_TAG, "Upgrading from version " + oldVersion + " to " + newVersion
                        + ", data will be lost!");

                db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME + ";");
                createTable(db);
                return;
            }


            if (oldVersion != newVersion) {
                throw new IllegalStateException("Error upgrading the database to version "
                        + newVersion);
            }

            if (TodosProvider.ACTIVATE_ALL_LOGS) {
                Log.d(LOG_TAG, "Todos | upgradeTable end");
            }
        }

        static String getBulkInsertString() {
            return new StringBuilder("INSERT INTO ").append(TABLE_NAME).append(" ( ").append(Columns.ID.getName())
                    .append(", ").append(Columns.UUID.getName()).append(", ").append(Columns.DONE.getName())
                    .append(", ").append(Columns.TITLE.getName()).append(", ").append(Columns.NOTES.getName())
                    .append(", ").append(Columns.DATE.getName()).append(", ").append(Columns.DATE_CREATED.getName())
                    .append(" ) VALUES (?, ?, ?, ?, ?, ?, ?)").toString();
        }

        static void bindValuesInBulkInsert(SQLiteStatement stmt, ContentValues values) {
            int i = 1;
            String value;
            stmt.bindLong(i++, values.getAsLong(Columns.ID.getName()));
            stmt.bindString(i++, values.getAsString(Columns.UUID.getName()));
            stmt.bindLong(i++, values.getAsLong(Columns.DONE.getName()));
            value = values.getAsString(Columns.TITLE.getName());
            stmt.bindString(i++, value != null ? value : "");
            value = values.getAsString(Columns.NOTES.getName());
            stmt.bindString(i++, value != null ? value : "");
            stmt.bindLong(i++, values.getAsLong(Columns.DATE.getName()));
            stmt.bindLong(i++, values.getAsLong(Columns.DATE_CREATED.getName()));
        }
    }
}

