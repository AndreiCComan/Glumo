package glumo.com.glumo.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.opencsv.CSVWriter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import glumo.com.glumo.R;
import glumo.com.glumo.application.GlumoApplication;
import glumo.com.glumo.object.GlucoseRead;

/**
 * This class handles the operation related to the database
 */
public class DBManager extends SQLiteOpenHelper{

    // private values
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "glucoseData";
    private static final String TABLE_GLUCOSES = "glucoses";
    private static final String KEY_ID = "id";
    private static final String KEY_TIME = "time";
    private static final String KEY_GLUCOSE = "glucose";

    public static String dateFormat = "yyyy-MM-dd HH:mm:ss";

    public DBManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * This method simply creates the db table
     * @param db database
     */
    @Override
    public void onCreate(SQLiteDatabase db) {

        // building the table
        String CREATE_GLUCOSE_TABLE =
                "CREATE TABLE " + TABLE_GLUCOSES + "("
                        + KEY_ID + " INTEGER PRIMARY KEY,"
                        + KEY_TIME + " TEXT,"
                        + KEY_GLUCOSE + " INTEGER"
                        + ")";
        db.execSQL(CREATE_GLUCOSE_TABLE);
    }


    /**
     * For further versions
     * @param db database
     * @param oldVersion old version
     * @param newVersion new version
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // here there will be the code to update the DB in further versions
    }

    /**
     * Delete all the entries
     */
    public void deleteAllEntries () {
        // the kill-all query
        String query = "delete from " + TABLE_GLUCOSES;

        // getting the writable database object and execute the query
        SQLiteDatabase db = this.getReadableDatabase();
        db.execSQL(query);
    }


    /**
     *  This method gets a matrix of string where each line is [timeStamp , GlucoseReadValue]
     *  @param data : DB data, CSV file with NO HEADER
     *  @return boolean : was the operation successful?
     */
    public boolean insertGlucoseReads(ArrayList<String[]> data){
        // getting the writable database object
        SQLiteDatabase db = this.getReadableDatabase();

        String columns = KEY_TIME + ", " + KEY_GLUCOSE;
        String query = "INSERT INTO " + TABLE_GLUCOSES + " ( " + columns + " ) values (";
        int length = data.size();
        for (int i = 0 ; i < length ; i++) {
            query += (data.get(i)[0] + ", " + data.get(i)[1] + ")");
            if (i != length -1)
                query += ", (";
            else
                query += ";";
        }

        try {

            db.beginTransaction();
            // delete all the entries
            deleteAllEntries();
            // import the new entries
            db.execSQL(query);
            // success?
            db.setTransactionSuccessful();

        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        finally {
            db.endTransaction();
        }
        return true;
    }


    /**
     *  This method just takes the glucoseValue, retrieves the timestamp and finally invokes the insertGlucoseRead(int, string) method
     *  @param glucoseValue : the glucose value
     */
    public void insertGlucoseRead(int glucoseValue){
        // invoking the specific insertGlucoseRead query
        insertGlucoseRead(glucoseValue, getCurrentTimestamp());
    }


    /**
     *  This method takes the glucoseValue and a timestamp. Then inserts 'em into the DB
     *  @param glucoseValue : the glucose value
     *  @param time : "dateFormat" time format
     */
    public void insertGlucoseRead(int glucoseValue, String time){

        // getting the writable database object
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        // putting the values in the content values object
        values.put(KEY_TIME, time);
        values.put(KEY_GLUCOSE, glucoseValue);

        // inserting the new entry and closing the db
        db.insert(TABLE_GLUCOSES, null, values);
        db.close();
    }


    /**
     *  This method returns all the data contained in DB
     *  @return dbData : an array list of string pairs containing all the glucose reads (with header)
     */
    public ArrayList<String[]> getAllGlucoseReads(){

        // creating and initializing the array list
        ArrayList<String[]> dbData = new ArrayList<>();
        String [] headerString = {KEY_TIME, KEY_GLUCOSE};
        dbData.add(headerString);

        // building the query.
        String query = "SELECT * FROM " + TABLE_GLUCOSES;

        // getting the writable database object and the cursor
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        while(cursor.moveToNext()){
            // array made of glucose time and glucose read value
            String [] tempString = {cursor.getString(1), cursor.getString(2)};
            dbData.add(tempString);
        }
        // closing the cursors
        cursor.close();

        return dbData;
    }


    /**
     *  This method returns the glucose reads within the given interval of time
     *  Depending on the given parameters, this method can also retrieve all the entries
     *  @param lowerTime : "dateFormat" time format
     *  @param upperTime : "dateFormat" time format
     *  @param extras : has the function to calculate extras (min, max, average, ...)?
     *  @return executeQuery(query, extras)
     */
    public JSONObject getRangeOfGlucoseReads(String lowerTime, String upperTime, boolean extras){

        // building the query.
        String query = "SELECT * FROM " + TABLE_GLUCOSES
                + " WHERE"
                + " datetime(" + KEY_TIME + ") >= datetime(\"" + lowerTime + "\")"
                + " AND"
                + " datetime(" + KEY_TIME + ") <= datetime(\"" + upperTime + "\")"
                + " ORDER BY datetime(" + KEY_TIME + ") DESC";

        return executeQuery(query, extras);
    }


    /**
     *  This method creates a query for getting the list of glucose reads since howManyDaysAgo days
     *  @param howManyDaysAgo : how many days we have to start getting the reads from?
     *  @param extras : has the function to calculate extras (min, max, average, ...)?
     *  @return executeQuery(query, extras)
     */
    public JSONObject getRangeOfGlucoseReads(int howManyDaysAgo, boolean extras){

        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -howManyDaysAgo);
        Date todate1 = cal.getTime();
        String fromdate = sdf.format(todate1);

        // building up the query
        String query = "SELECT * FROM " + TABLE_GLUCOSES
                + " WHERE"
                + " datetime(" + KEY_TIME + ") >= datetime(\"" + fromdate + "\")"
                + " ORDER BY datetime(" + KEY_TIME + ") DESC";

        return executeQuery(query, extras);
    }

    
    /**
     *  This method creates a query for the last X (howManyReads) glucose reads
     *  @param howManyReads : how many reads we want to retrieve
     *  @param extras : has the function to calculate extras (min, max, average, ...)?
     *  @return executeQuery(query, extras)
     */
    public JSONObject getLastXGlucoseReads(int howManyReads, boolean extras){

        // building up the query
        String query = "SELECT * FROM " + TABLE_GLUCOSES
                + " ORDER BY datetime(" + KEY_TIME + ")"
                + " DESC LIMIT " + howManyReads;

        return executeQuery(query, extras);
    }


    /**
     *  This util method takes a string query as parameters
     *  Elaborates the query and calculates some useful values (min, max, average)
     *  @param query : the query to perform
     *  @param extras : has the function to calculate extras (min, max, average, ...)?
     *  @return json : {list : matching_glucose_List , max : max_value_among_list, min : min_value_among_list, average : average:value, ...} or null if no entries matched the query
     */
    private JSONObject executeQuery (String query, boolean extras) {

        int min = -1, max = 0, average = 0, cont = 0, tempValue, normalHits = 0, warningHits = 0, dangerHits = 0, hypoHits = 0, hyperHits = 0;

        // the GlucoseRead list
        ArrayList<GlucoseRead> readList = new ArrayList<>();;

        // the json that will be returned
        JSONObject json = null;

        // getting the writable database object and the cursor
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        // using the cursor to get all the values
        if (cursor != null) {
            json = new JSONObject();
            if (cursor.moveToFirst()) {
                do {
                    // elaborating min - max - average glucose values
                    tempValue = cursor.getInt(2);
                    if (extras) {
                        if (tempValue < min || min == -1)
                            min = tempValue;
                        if (tempValue > max)
                            max = tempValue;

                        // calculating wheter the value is normal - warning - danger
                        String readDangerLevel = getGlucoseDangerLevel(tempValue);
                        switch (readDangerLevel) {
                            case "hyperglycemia":
                                hyperHits++;
                                break;
                            case "hypoglycemia":
                                hypoHits++;
                                break;
                            case "danger":
                                dangerHits++;
                                break;
                            case "warning":
                                warningHits++;
                                break;
                            case "normal":
                                normalHits++;
                                break;
                        }

                        // updating average and cont
                        average += tempValue;
                        cont++;
                    }

                    // setting the glucose read values and adding to the list
                    GlucoseRead read = new GlucoseRead();
                    read.setTime(cursor.getString(1));
                    read.setGlucose(tempValue);
                    readList.add(read);
                } while (cursor.moveToNext());

                if (extras) {
                    average = (average / cont);
                }

                try {
                    if (extras) {
                        json.put("max", max);
                        json.put("min", min);
                        json.put("average", average);
                        json.put("cont", cont);
                        json.put("dangerHits", dangerHits);
                        json.put("warningHits", warningHits);
                        json.put("normalHits", normalHits);
                        json.put("hyperHits", hyperHits);
                        json.put("hypoHits", hypoHits);
                    }
                    json.put("list", readList);

                } catch (JSONException e) {
                    json = null;
                    Toast.makeText(GlumoApplication.getContext(), GlumoApplication.getContext().getString(R.string.data_in_database_corrupted), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
            else {
                try {
                    if (extras) {
                        json.put("max", Integer.MIN_VALUE);
                        json.put("min", Integer.MAX_VALUE);
                        json.put("average", 0);
                        json.put("cont", 0);
                        json.put("dangerHits", 0);
                        json.put("warningHits", 0);
                        json.put("normalHits", 0);
                        json.put("hyperHits", 0);
                        json.put("hypoHits", 0);
                    }
                    json.put("list", readList);
                } catch (JSONException e) {
                    json = null;
                    Toast.makeText(GlumoApplication.getContext(), GlumoApplication.getContext().getString(R.string.data_in_database_corrupted), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        }
        return json;
    }


    /**
     *  This util method calculates whether the value is normal - warning - danger
     *  @param glucoseValue : the glucose value
     *  @return level : normal - warning - danger - hypoglycemia - hyperglycemia
     */
    public static String getGlucoseDangerLevel (int glucoseValue) {
        int [] thresholds = {GlumoApplication.getIntPreference(R.string.hypoglycemia_preference), GlumoApplication.getIntPreference(R.string.hyperglycemia_preference)};
        int percentage = ((thresholds[1] - glucoseValue)*100) / (thresholds[1] - thresholds[0]);
        String level = "";
        if (percentage < 0)
            level = "hyperglycemia";
        else if (percentage > 100)
            level = "hypoglycemia";
        else if (percentage < 15 || percentage > 85)
            level = "danger";
        else if (percentage < 35 || percentage > 75)
            level = "warning";
        else
            level = "normal";
        return level;
    }


    /**
     *  This util method calculates the current timestamp according to the default chosen format.
     *  @return timestamp : the timestamp in String
     */
    public static String getCurrentTimestamp () {
        // getting the data
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        String timestamp = sdf.format(cal.getTime()).toString();
        return timestamp;
    }


    /**
     *  This util method calculates the current timestamp according to the given date format.
     *  @return timestamp : the timestamp in String
     */
    public static String getCurrentTimestamp (String format) {
        // getting the data
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        String timestamp = sdf.format(cal.getTime()).toString();
        return timestamp;
    }


    /**
     *  This method just creates a CSV file from DB data, and saves it as glucoses currentTimestamp.csv in the storage
     *  @return fileName : the file name
     */
    public String exportToCsv (){
        // getting the storage directory
        File exportDir = new File(Environment.getExternalStorageDirectory(), "");

        // if the directory doesn't exist, create it
        if (!exportDir.exists()){
            exportDir.mkdirs();
        }

        // creating the new empty file adding the timestamp to its name
        String timestamp = getCurrentTimestamp("yyyy MMM EE HH:mm:ss");
        String fileName = "glucoses " + timestamp + ".csv";
        File file = new File(exportDir, fileName);

        try {
            // actually trying to create the file
            file.createNewFile();

            // the CSV writer
            CSVWriter csvWrite = null;
            csvWrite = new CSVWriter(new FileWriter(file));

            // filling the array list with data
            ArrayList<String[]> data = getAllGlucoseReads();
            int length = data.size();
            for (int i = 0 ; i < length ; i++) {
                String [] tempString =  {data.get(i)[0], data.get(i)[1]};
                csvWrite.writeNext( tempString );
            }
            // closing the cursors
            csvWrite.close();
        } catch (IOException e) {
            e.printStackTrace();
            fileName = null;
            Log.e("DB_MANAGER", "exportToCsv - failed to create CSV file");
        }
        return fileName;
    }


    /**
     *  This method just fills the DB with data from a CSV file
     *  @param fileName : file path
     *  @param delete : delete the file after the import?
     *  @return  has been the file successfully imported in the DB?
     */
    public boolean importFromCsv (String fileName, boolean delete) {
        // the base file path
        String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();

        // the matrix that will be filled with data
        ArrayList<String[]> data = new ArrayList<>();

        try {
            // getting the file composing the base path and the given path
            FileReader file = new FileReader(baseDir + File.separator + fileName);
            BufferedReader buffer = new BufferedReader(file);

            // skipping the header
            String line = buffer.readLine();

            // for each line of data
            while ((line = buffer.readLine()) != null) {
                String[] str = line.split(",");
                String [] tempString = {str[0], str[1]};
                data.add(tempString);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        // call the method to insert data
        if (!GlumoApplication.db.insertGlucoseReads(data))
            return false;

        File theFile;

        // if the file has to be deleted
        if (delete) {
            theFile = new File(baseDir, fileName);
            if (theFile.exists())
                theFile.delete();
        }
        return true;
    }
}