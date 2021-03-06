package eecs448_first_team.calender_app;

/**
 * author: Hans Brown
 * date: 9-18-16
 * purpose: a SQLite Database interfacing page: Instantiate an object to get access
 * to the database for reading and writing values. Primary database access methods are
 * getCalendarDetails(long) and setCalendarDetails(long,String)
 * Many thanks to Android (https://developer.android.com/index.html) for its help
 *
 * Updated by "Team One" for Project 2.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CalendarEventDb extends SQLiteOpenHelper
{

    private SQLiteDatabase rdb; //readable database (for fetching values)
    private SQLiteDatabase wdb; //writable database (for editing values)
    private static final int DATABASE_VER = 3; //arbitrary database version representation: to be incremented to prevent conflict issues
	private static final String DATABASE_NAME = "CalendarEventTable.db";
    private Calendar timeCalendar;
    private class CalendarEventTable implements BaseColumns {
        //IMPLIED Property _ID returns unique ID of object in database for direct reference
        static final String Table_Name = "Event";
        static final String Column_Details = "Details";
        static final String Column_Start_Date = "StartDate";
        static final String Column_End_Date = "EndDate";
    }

    //when given to SQLite, this command creates the Table and defines its columns. TEXT is equivalent to String
    //and INTEGER is equivalent to Long (the class-number, not the lowercase number)
    private static final String SQL_CREATE_ENTRIES = "CREATE TABLE " + CalendarEventTable.Table_Name + "("
            + CalendarEventTable._ID + " INTEGER PRIMARY KEY,"
            + CalendarEventTable.Column_Details + " TEXT,"
            + CalendarEventTable.Column_Start_Date + " INTEGER,"
            + CalendarEventTable.Column_End_Date + " INTEGER"
            + ")";
    //when called, deletes the table outright, all data is lost
    private static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + CalendarEventTable.Table_Name;

    //2nd parameter in cursor creation method db.query()
    //specifies what Columns from table to return
    private static final String[] PARAMETERS_TABLE_RETURN_COLUMNS = {CalendarEventTable._ID,CalendarEventTable.Column_Start_Date,CalendarEventTable.Column_End_Date,CalendarEventTable.Column_Details};

    /**
     * Constructs this interface to the app's SQLite database.
     * @param context the context (any Activity is a context) used by base class constructor
     */
	public CalendarEventDb(Context context)
	{
		super(context,DATABASE_NAME,null,DATABASE_VER);
	}
    /**
     * Called automatically when a database is being accessed while database does not exist.
     * Takes a SQLiteDatabase to represent the Phone-side database class to write with.
     * Creates a new empty database for user.
     * @param db The SQLiteDatabase to be constructed. Has no meaningful purpose since there is only ever one database
     */
	public void onCreate(SQLiteDatabase db)
	{
		db.execSQL(SQL_CREATE_ENTRIES);
	}

    /**precondition: database was changed, we are upgrading to a new database.
     * postcondition: new database created, all existing data lost.
     * here we could do specific logic to transfer data from
     * old database to new database.
     * @param db the phone database used to hold table
     * @param oldVersion the old version the database exists at
     * @param newVersion the new version to update the database to
     */
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL(SQL_DELETE_ENTRIES);
		onCreate(db);
	}

    /**
     * Get the events in the databse between two arbitrary start and end times.
     * precondition: database already instantiated.
     * postcondition: if readable database was not accessible, it is now accessible.
     * @param start The start time to search
     * @param end The end time to search
     * @return List of events that exist between these two times. null if error in db request.
     */
    public List<CalendarEvent> getCalendarEvents(Long start, Long end) {
        if(rdb == null)
            rdb = this.getReadableDatabase();

        //used to search (you have to use strings)
        String[] searchArgs = {end.toString(), start.toString()};

        try {
            // db query
            Cursor searchContainer = rdb.query(
                    CalendarEventTable.Table_Name, //query the CalendarEventTable
                    PARAMETERS_TABLE_RETURN_COLUMNS, //give me ID, Date, and Details columns that...
                    CalendarEventTable.Column_Start_Date + " < ? AND " + CalendarEventTable.Column_End_Date + " >= ?",
                    searchArgs, //... it matches the date I gave
                    null, //don't group rows
                    null, //don't filter by row groups
                    CalendarEventTable.Column_Start_Date + ", " + CalendarEventTable.Column_End_Date
            );

            // create a list based on rows returned
            List<CalendarEvent> events = new ArrayList<CalendarEvent>(searchContainer.getCount());
            for (int i = 0; i < searchContainer.getCount(); i++) {
                searchContainer.moveToPosition(i);

                // init new calendar event
                CalendarEvent returnCalendarEvent = new CalendarEvent();

                returnCalendarEvent.setID(searchContainer.getLong(0));
                returnCalendarEvent.setStartDate(searchContainer.getLong(1));
                returnCalendarEvent.setEndDate(searchContainer.getLong(2));
                returnCalendarEvent.setDetails(searchContainer.getString(3));

                // add it to list
                events.add(returnCalendarEvent);
            }

            return events;
        } catch(Exception e) {
            return null; //either there's nothing there, or table is corrupted
        }
    }

    /**
     * Get the calendar event with the specified id
     * precondition: database already instantiated.
     * postcondition: if readable database was not accessible, it is now accessible.
     * @param id The identifier to query the event
     * @return Event containing informaton of event id. null if database query failed or no matching row
     */
    public CalendarEvent getCalendarEvent(Long id) {
        try {
            if (rdb == null)
                rdb = this.getReadableDatabase();

            String[] searchArgs = {id.toString()}; //used to search (you have to use strings)

            Cursor searchContainer = rdb.query(
                    CalendarEventTable.Table_Name, //query the CalendarEventTable
                    PARAMETERS_TABLE_RETURN_COLUMNS, //give me ID, Date, and Details columns that...
                    CalendarEventTable._ID + "= ?",
                    searchArgs, //... it matches the date I gave
                    null, //don't group rows
                    null, //don't filter by row groups
                    null // don't sort
            );

            if (searchContainer.getCount() == 0) {
                return null;
            } else {
                searchContainer.moveToFirst();

                //as specified in the RETURN_COLUMNS, 0th column is Date, 1st column is Details
                CalendarEvent returnCalendarEvent = new CalendarEvent();
                returnCalendarEvent.setID(searchContainer.getLong(0));
                returnCalendarEvent.setStartDate(searchContainer.getLong(1));
                returnCalendarEvent.setEndDate(searchContainer.getLong(2));
                returnCalendarEvent.setDetails(searchContainer.getString(3));

                return returnCalendarEvent;
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Edit an event.
     * @param event The event to edit
     * @return True if Details were added successfully, False if Details adding failed (possibly table corrupted)
     */
    public boolean updateEvent(CalendarEvent event) {
        try {
            if (wdb == null)
                wdb = this.getWritableDatabase();

            ContentValues values = new ContentValues();

            values.put(CalendarEventTable.Column_Start_Date, event.getStartDate());
            values.put(CalendarEventTable.Column_End_Date, event.getEndDate());
            values.put(CalendarEventTable.Column_Details, event.getDetails());

            String[] searchArgs = {event.getID().toString()}; //used to search (you have to use strings)

            int num_updated = wdb.update(CalendarEventTable.Table_Name, values, CalendarEventTable._ID + "= ?", searchArgs);

            return num_updated > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Adds a new event.
     * @param event The event to store in the database.
     * @return True if Details were added successfully, False if Details adding failed (possibly table corrupted)
     */
    public boolean addEvent(CalendarEvent event) {
        try {
            if (wdb == null)
                wdb = this.getWritableDatabase();

            ContentValues values = new ContentValues();

            values.put(CalendarEventTable.Column_Start_Date, event.getStartDate());
            values.put(CalendarEventTable.Column_End_Date, event.getEndDate());
            values.put(CalendarEventTable.Column_Details, event.getDetails());

            Long id = wdb.insert(CalendarEventTable.Table_Name, null, values);

            return id != -1;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Delete an event based on information provided.
     * @param event The event to store in the database. "getID()" must be non-null.
     * @return True if events was removed, False if event removing failed (possibly table corrupted)
     */
    public boolean deleteEvent(CalendarEvent event) {
        try {
            if (wdb == null)
                wdb = this.getWritableDatabase();

            //used to search (you have to use strings)
            String[] searchArgs = {event.getID().toString()};

            // delete request to db
            int num_deleted = wdb.delete(CalendarEventTable.Table_Name, CalendarEventTable._ID + " = ?", searchArgs);

            return num_deleted > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Handle db version changes.
     * precondition: user reverting to old database version.
     * postcondition: new database created, all existing data lost.
     * @param db the database physically on the phone associated with this application
     * @param oldVersion the previous database version integer
     * @param newVersion the new version integer to update the database to
     */
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		//or possible reversion implementation
		onUpgrade(db,oldVersion,newVersion);
	}
}