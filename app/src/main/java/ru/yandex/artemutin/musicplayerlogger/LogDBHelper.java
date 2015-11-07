package ru.yandex.artemutin.musicplayerlogger;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LogDBHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "TrackLog", LOG_TB_NAME = "track_log";
    public static final int DB_VERSION = 1;
    enum colNames{A,B}
    /**
     * Create a helper object to create, open, and/or manage a database.
     * This method always returns very quickly.  The database is not actually
     * created or opened until one of {@link #getWritableDatabase} or
     * {@link #getReadableDatabase} is called.
     *
     * @param context to use to open or create the database
     *                {@link #onUpgrade} will be used to upgrade the database; if the database is
     *                newer, {@link #onDowngrade} will be used to downgrade the database
     */
    public LogDBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    /**
     * Called when the database is created for the first time. This is where the
     * creation of tables and the initial population of the tables should happen.
     *
     * @param db The database.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE "+ LOG_TB_NAME + "(id INT AUTO INCREMENT, track VARCHAR(255) NOT NULL, " +
                "album VARCHAR(255), artist VARCHAR(255), datetime LONG NOT NULL, status TINYINT NOT NULL, PRIMARY KEY(id) );");
        db.execSQL("CREATE TABLE transfer_log (id INT AUTO INCREMENT, first_track_id INT, " +
                "last_track_id INT, datetime LONG NOT NULL, PRIMARY KEY(id), FOREIGN KEY (first_track_id) REFERENCES track_log(id)," +
                "FOREIGN KEY (last_track_id) REFERENCES track_log(id) );");
    }

    /**
     * Called when the database needs to be upgraded. The implementation
     * should use this method to drop tables, add tables, or do anything else it
     * needs to upgrade to the new schema version.
     * <p>
     * <p>
     * The SQLite ALTER TABLE documentation can be found
     * <a href="http://sqlite.org/lang_altertable.html">here</a>. If you add new columns
     * you can use ALTER TABLE to insert them into a live table. If you rename or remove columns
     * you can use ALTER TABLE to rename the old table, then create the new table and then
     * populate the new table with the contents of the old table.
     * </p><p>
     * This method executes within a transaction.  If an exception is thrown, all changes
     * will automatically be rolled back.
     * </p>
     *
     * @param db         The database.
     * @param oldVersion The old database version.
     * @param newVersion The new database version.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
