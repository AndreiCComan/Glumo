package glumo.com.glumo.application;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import glumo.com.glumo.util.DBManager;

/*  "Application" class. Provides the most of the methods to interact with the DB and the preferences
 *  Besides, stores the most of the static variables used in the application
 */
public class GlumoApplication extends Application {

    // Intent list
    public static final String BT_ON = "glumo.com.glumo.util.bluetoothutil.on";
    public static final String BT_OFF = "glumo.com.glumo.util.bluetoothutil.of";
    public static final String BT_N_SUPPORTED = "glumo.com.glumo.util.bluetoothutil.notsupported";
    public static final String BT_D_UNREACHABLE = "glumo.com.glumo.util.bluetoothutil.deviceunreachable";
    public static final String BT_N_SELECTED_DEVICE = "glumo.com.glumo.util.bluetoothutil.notselecteddevice";
    public static final String BT_UNKNOWN_INTENT_ACTION = "glumo.com.glumo.util.bluetoothutil.notselecteddevice";
    public static final String BT_READ_GLUCOSE = "glumo.com.glumo.util.bluetoothutil.readglucose";
    public static final String BT_READ_GLUCOSE_R = "glumo.com.glumo.util.bluetoothutil.readglucoseresponse";
    public static final String BT_GET_PAIRED_DEVICES = "glumo.com.glumo.util.bluetoothutil.getpaireddevices";
    public static final String BT_GET_PAIRED_DEVICES_R = "glumo.com.glumo.util.bluetoothutil.getpaireddevicesresponse";
    public static final String BAM_ALARM_NOTIFICATION_ACKNOWLEDGED = "glumo.com.glumo.util.broadcastandalarmmanager.alarmnotificationacknowledged";
    public static final String BAM_ALARM_NOTIFICATION_NOT_ACKNOWLEDGED = "glumo.com.glumo.util.broadcastandalarmmanager.alarmnotificationnotacknowledged";

    // Bluetooth status int list
    public static final int BT_NOT_SELECTED_DEVICE = -3;
    public static final int BT_DEVICE_UNREACHABLE = -2;
    public static final int BT_NOT_SUPPORTED = -1;
    public static final int BT_BLUETOOTH_OFF = 0;
    public static final int BT_BLUETOOTH_ON = 1;

    //  Intent tag list for extras
    public static final String ET_GLUCOMETER_RESPONSE_TAG = "glucose_data";
    public static final String BT_INTENT_UNSUCCESSFUL_TRIES_TAG = "unsuccessful_tries";
    public static final String BT_OLD_INTENT_ACTION_TAG = "action_tag";
    public static final String BT_PAIRED_DEVICES_LIST_TAG = "paired_devices_list";

    // Android -> Arduino command list
    public static final int BT_READ_COMMAND = 1;

    // Arduino response stuff
    public static final char ARDUINO_EOM_CHAR = '*';

    // Pending intents request codes
    public static final int PI_ASYN_READ_CODE = 1;
    public static final int PI_SEND_ALARM_SMS_CODE = 2;
    public static final int PI_ACK_ALARM_CODE = 3;

    // Start activity for result request codes
    public static final int REQ_CODE_SPEECH_INPUT = 100;

    // Permissions requestes codes
    public static final int PERMISSIONS_REQUEST_CODE_SEND_SMS = 1;
    public static final int PERMISSIONS_REQUEST_CODE_READ_WRITE_EXTERNAL_STORAGE = 2;
    public static final int PERMISSIONS_REQUEST_CODE_READ_WRITE_EXTERNAL_STORAGE_DRIVE = 3;
    public static final int PERMISSIONS_REQUEST_CODE_READ_WRITE_EXTERNAL_STORAGE_DROPBOX = 4;



    // Notification IDs
    public static final int ALARM_NOTIFICATION_ID = 1;


    final static public String APP_PUBLIC_KEY_DROPBOX = "lpkfs1ejy0t1gu7";
    final static public String APP_SECRET_KEY_DROPBOX = "c7mrnrd4fxjoj4b";




    // Miscellaneous
    public static final String stringBetweenDeviceNameAndAddress = "\nMAC: ";
    public static final String generalStringDelimiter = "dsbgtdesfsfesfjyvvdbfnyredsfgdhyertsfdvbstrvebs5et";

    /*--------------------------------------------------------------------------------------------*/
    // Search item API | United States Department of Agriculture
    public static final int FOOD_ITEM_LOADER_ID = 1;
    public static final String USDA_REQUEST_URL = "https://api.nal.usda.gov/ndb";
    public static final String SEARCH_BY_QUERY = "/search";
    public static final String SEARCH_BY_NDBNO = "/reports";

    public static final String API_KEY_PARAM = "api_key";
    public static final String API_KEY_VALUE = "PsbF0P5Lek0u29myzEK3JmRum31AZs3PuJUmghZ6";

    public static final String QUERY_PARAM = "q";

    public static final String DATA_SOURCE_PARAM = "ds";
    public static final String DATA_SOURCE_VALUE = "Standard Reference";

    public static final String MAX_ROWS_PARAM = "max";
    public static final String MAX_ROWS_VALUE = "7";

    public static final String NDBNO_PARAM = "ndbno";
    public static final String REPORT_TYPE_PARAM = "type";
    public static final String REPORT_TYPE_VALUE = "s";

    public static final String FORMAT_PARAM = "format";
    public static final String FORMAT_VALUE = "JSON";

    public static final String CARBOHYDRATE_NUTRIENT_ID = "205";
    public static final String PROTEIN_NUTRIENT_ID = "203";
    public static final String FAT_NUTRIENT_ID = "204";
    /*--------------------------------------------------------------------------------------------*/


    // other variables
    private static Context context;
    private static SharedPreferences preferences;
    private static SharedPreferences.Editor editor;
    public static DBManager db;

    @Override
    public void onCreate(){
        super.onCreate();
        context = getApplicationContext();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        editor = preferences.edit();
        db = new DBManager(context);
    }

    // getters and setters
    public static Context getContext(){
        return context;
    }

    public static SharedPreferences getPreferences(){
        return preferences;
    }

    public static boolean setPreference( int id, String value) {
        try {
            editor.putString(context.getString(id), value);
            editor.commit();
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    public static boolean setPreference( int id, int value) {
        try {
            editor.putInt(context.getString(id), value);
            editor.commit();
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    public static boolean setPreference( int id, boolean value) {
        try {
            editor.putBoolean(context.getString(id), value);
            editor.commit();
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    public static boolean getBooleanPreference( int id ) {
        try {
            boolean preference = preferences.getBoolean(context.getString(id), false);
            return preference;
        }
        catch (Exception e) {
            return false;
        }
    }

    public static int getIntPreference(int id ){
        return preferences.getInt(context.getString(id), 0);
    }

    public static String getStringPreference(int id){
        return preferences.getString(context.getString(id), "");
    }

}
