package glumo.com.glumo.util;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Set;
import glumo.com.glumo.R;
import glumo.com.glumo.activity.AppWidgetProviderActivity;
import glumo.com.glumo.application.GlumoApplication;

import static android.os.SystemClock.elapsedRealtime;

/**
 * This utility class wraps some needful functions regarding the bluetooth management.
 *  The functionalities are mostly available through intents (get paired devices, send command, ... ), that are
 *  - BT_READ_GLUCOSE : intent for make a glucose read
 *  - BT_GET_PAIRED_DEVICES : intent for retrieve the list of paired devices
 *  The response for these intents will be send through intents too (BT_INTENT -> BT_INTENT_R)
 */
public final class BluetoothUtil extends IntentService {

    // bluetooth variables
    private static Context ctx = GlumoApplication.getContext();

    private static BluetoothAdapter bt_adapter = BluetoothAdapter.getDefaultAdapter();
    private static BluetoothSocket bt_socket = null;                 // the socket we will use to communicate with the device
    private static OutputStream o_s = null;                          // the output stream
    private static InputStream i_s = null;                           // the input stream
    private static String bt_device_MAC = null;                      // the device we are going to connect to
    private static int maximum_connection_tries = 3;                 // if the arduino does not responde, how many times will I try to communicate with it?
    private static int command_interval = 5000;                      // how much should I wait to send the next command to the arduino? (in order to make the connection closure happen succesfully)
    private static AlarmManager alarmManager;                        // the alarm manager

    @Override
    public void onCreate() {
        super.onCreate();
    }


    /**
     * Constructor.
     */
    public BluetoothUtil() {
        super("BluetoothUtil");
    }


    /**
     * This method encapsulates the parameters in an intent, and then throws it to this IntentService
     *  @param action : the intent action
     *  @param extras : a list of array made of two strings ["extra_data_tag", "extra_data"]
     */
    public static void BTIntent (String action, String[] ... extras) {
        Intent intent = new Intent(ctx, BluetoothUtil.class);
        buildIntent(intent, action, extras);                    // call the method to build the intent
        ctx.startService(intent);                               // start the service
    }


    /**
     * This method do the same as before, but receive directly an intent
     *  @param intent : the intent to send
     */
    public static void BTIntent (Intent intent) {
        ctx.startService(intent);     // start the service
    }


    /**
     *  This method encapsulates the parameters in an intent, and then uses a local broadcast to send it
     *  It also creates a custom intent directly bounded with the BroadcastAndAlarmManager class, in order to make it receive the intent to
     *  Indeed, since BroadcastAndAlarmManager is not a local broadcast receiver, it would not receive its
     *  @param action : the intent action
     *  @param extras : a list of array made of two strings ["extra_data_tag", "extra_data"]
     */
    public static void BTResponse (String action, String[] ... extras) {
        String myExtras = "";

        if (extras != null) {
            for (String[] extra : extras) {
                myExtras += "(" + extra[0] + ", " + extra[1] + ") ";
            }
        }

        // intent section
        Intent localIntent  = new Intent();              // the intent
        buildIntent(localIntent, action, extras);       // build it and send it through the LocalBroadcastManager
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(localIntent);

        Intent intentToBAM = new Intent(ctx, BroadcastAndAlarmManager.class);
        intentToBAM.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        buildIntent(intentToBAM, action, extras);       // build the intent and send it directly to BroadcastAndAlarmManager
        ctx.sendBroadcast(intentToBAM);

        Intent intentToWidget = new Intent(ctx, AppWidgetProviderActivity.class);
        intentToWidget.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        buildIntent(intentToWidget, action, extras);    // build the intent and send it directly to AppWidgetProviderActivity
        ctx.sendBroadcast(intentToWidget);
    }


    /**
     * This method do the same as before, but receive directly an intent
     *  @param localIntent : the intent to send
     */
    public static void BTResponse (Intent localIntent) {
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(localIntent);
    }


    /**
     *  This method delete the previous alarm and make another starts with the new interval
     *  @param minutes : the read interval we want to wait between each automatic makeRead request.
     *                   It has to be one of the AlarmManager INTERVAL (INTERVAL_FIFTEEN_MINUTES, INTERVAL_HALF_HOUR, ...)
     *  @param enable : enable the alarm (true) or disable it (false)?
     */
    public static void setAsyncRead(long minutes, boolean enable) {
        alarmManager = (AlarmManager) ctx.getSystemService(ALARM_SERVICE);  // get the alarm manager
        Intent intent = new Intent(ctx, BroadcastAndAlarmManager.class);    // create the intent
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.setAction(GlumoApplication.BT_READ_GLUCOSE);                 // set it's action

        // create the pending intent that will trigger the glucose read
        PendingIntent pendingIntent = PendingIntent.getBroadcast(ctx, GlumoApplication.PI_ASYN_READ_CODE, intent, 0);

        // cancel the old alarm (if any)
        alarmManager.cancel(pendingIntent);

        if (enable) {
            minutes = minutes * 60000; // convert from minutes to milliseconds
            // make the alarm start
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, elapsedRealtime() + minutes, minutes, pendingIntent);
        }
    }


    /**
     *  This checks the bluetooth status and, if everything is supposed to be ok, "executes" the intent basing on its action
     *  Here there is the list of the possible intents that can be sent by this method:
     *  - BT_GET_PAIRED_DEVICES_R
     *  - BT_N_SELECTED_DEVICE
     *  - BT_READ_GLUCOSE_R
     *  - BT_D_UNREACHABLE
     *  - BT_N_SUPPORTED
     */
    @Override
    protected void onHandleIntent(Intent intent) {

        bt_device_MAC = GlumoApplication.getStringPreference(R.string.paired_device_preference);

        int status = checkBluetoothStatus();
        String action = intent.getAction();

        if (getIntentUnsuccessfulTries(intent) >= maximum_connection_tries) {
            BTResponse(GlumoApplication.BT_D_UNREACHABLE);      // respond with the proper intent
            return;
        }

        switch (status) {                                       // switch on the bluetooth status

            case GlumoApplication.BT_BLUETOOTH_OFF :            // CASE : THE BLUETOOTH IS OFF
                if (action.equals(GlumoApplication.BT_READ_GLUCOSE)) {
                    incrementIntentUnsuccessfulTries(intent);   // increment the unsuccessful tries number is the intent was "BT_READ_GLUCOSE", in order to try it again
                    try { Thread.sleep(command_interval); }     // wait let the bluetooth activate
                    catch (InterruptedException e) {}           // catch the exception by doing nothing (it's not so important)
                    BTIntent(intent);                           // re-send the intent
                }
                break;                                          // break

            case GlumoApplication.BT_NOT_SUPPORTED :            // CASE : BLUETOOTH NOT SUPPORTED. SEND THE PROPER INTENT
                BTResponse(GlumoApplication.BT_N_SUPPORTED);    // respond with the proper intent
                break;                                          // break

            case GlumoApplication.BT_BLUETOOTH_ON :             // CASE : THE BLUETOOTH IS ON
                // ACTION - GET THE LIST OF PAIRED DEVICES
                if (action.equals(GlumoApplication.BT_GET_PAIRED_DEVICES)) {
                    Set <BluetoothDevice> devices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
                    ArrayList<BluetoothDevice> devicesList = new ArrayList<>();     // the list that will be inserted in the intent
                    for (BluetoothDevice device : devices) {                        // for each device
                        devicesList.add(device);                                    // push it into the list
                    }
                    Intent localIntent = new Intent();                              // the intent
                    localIntent.setAction(GlumoApplication.BT_GET_PAIRED_DEVICES_R);// set the action and, below, the list
                    localIntent.putExtra(GlumoApplication.BT_PAIRED_DEVICES_LIST_TAG, devicesList);
                    BTResponse(localIntent);                                        // respond with the proper intent
                }


                // DESTINATION ADDRESS STILL NOT DEFINED. SEND THE PROPER INTENT
                else if (bt_device_MAC == null || "null".equals(bt_device_MAC) || "".equals(bt_device_MAC)) {
                    BTResponse(GlumoApplication.BT_N_SELECTED_DEVICE);    // respond with the proper intent
                }


                // CASE : ACTION - MAKE A GLUCOSE READ
                else if (action.equals(GlumoApplication.BT_READ_GLUCOSE)) {
                    bt_device_MAC = bt_device_MAC.split(GlumoApplication.stringBetweenDeviceNameAndAddress)[1];
                    try {
                        // SEND MESSAGE SECTION
                        BluetoothDevice paired_device = bt_adapter.getRemoteDevice(bt_device_MAC);
                        bt_socket = (BluetoothSocket) paired_device.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(paired_device, 1);
                        BluetoothAdapter.getDefaultAdapter().cancelDiscovery(); // cancel any bluetooth discovery
                        bt_socket.connect();    // connect to the device
                        o_s = bt_socket.getOutputStream();                      // get the output stream and, below, write the commad
                        o_s.write(String.valueOf(GlumoApplication.BT_READ_COMMAND).getBytes());
                        o_s.flush();                                            // flush the data
                        // RECEIVE RESPONSE SECTION
                        boolean end_of_message = false;     // did we reach the end of the response message?
                        int wait_cont = 0;                  // wait cont, to be able to understand if we lost the connnection while receiving the response
                        i_s = bt_socket.getInputStream();   // get the input stream
                        String response = "";               // the string that will contain the response

                        while (!end_of_message) {                       // while we receive the eom char (specified in ConstantValues)

                            int available_bytes = i_s.available();      // how many bytes are available?
                            byte[] buffer = new byte[available_bytes];  // dynamically instantiate the buffer

                            if (i_s.available() > 0) {                  // if there are bytes available
                                i_s.read(buffer);                       // read them

                                for (int i = 0; i < available_bytes; i++) {         // for each byte
                                    char b = ((char) (buffer[i] & 0xFF));           // cast it into char

                                    if (b == GlumoApplication.ARDUINO_EOM_CHAR) {   // if the byte is the eom byte
                                        end_of_message = true;                      // eom = true
                                        String selectedSensor = GlumoApplication.getStringPreference(R.string.selected_sensor_preference);
                                        // if the selected sensor is "FreeStyle Libre"
                                        if (ctx.getString(R.string.freestyle_libre_sensor_name).equals(selectedSensor)) {
                                            // response : tag is equal to "response", content is the response
                                            String responseData = FreeStyleLibreDataDecoder.decodeData(response);
                                            if (responseData.equals("-1"))
                                                throw new IOException ("wrong_formatted_data");
                                            else
                                                BTResponse(GlumoApplication.BT_READ_GLUCOSE_R, new String[]{GlumoApplication.ET_GLUCOMETER_RESPONSE_TAG, responseData});
                                        }
                                    }
                                    else {                                          // otherwise
                                        response += String.valueOf(b);              // just add it to the string
                                    }
                                }
                            }
                            else {                                      // if there aren't bytes available
                                wait_cont++;                            // increment the wait cont
                                if (wait_cont > 40) {                   // if wait cont > 40 (so we have waited at least 2 seconds)
                                    i_s.close();                        // close the i stream
                                    o_s.close();                        // close the o stream
                                    bt_socket.close();                  // close connection
                                    throw new IOException ("target_device_disconnected");
                                }
                                Thread.sleep(50);                       // wait 50 ms
                            }
                        }

                        // END OF COMMUNICATION SECTION
                        i_s.close();                            // close the i stream
                        o_s.close();                            // close the o stream
                        bt_socket.close();                      // close connection
                        Thread.sleep(command_interval);         // sleep in order to let the arduino close the connection

                    } catch (NoSuchMethodException | InterruptedException | IOException | InvocationTargetException | IllegalAccessException e) {
                        e.printStackTrace();
                        incrementIntentUnsuccessfulTries(intent);   // increment the number of the unsuccessful connection tries
                        try {
                            Thread.sleep(command_interval);         // wait for some time then try again to send the intent
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                        BTIntent(intent);                           // send the intent
                    }
                }
                break;

            default :   // DEFAULT : respond with an UNKNOWN_INTENT_ACTION intent, putting as extra the unknown intent action
                BTResponse(GlumoApplication.BT_UNKNOWN_INTENT_ACTION, new String[]{GlumoApplication.BT_OLD_INTENT_ACTION_TAG, action});
                break;
        }
        return;
    }


    /**  This method simply checks the bluetooth status through the bluetooth adapter
     *  @return status : BT_NOT_SUPPORTED if bluetooth is not supported
     *                   BT_BLUETOOTH_OFF if bluetooth is turned off (and then it'll turn it on)
     *                   BT_BLUETOOTH_ON  if bluetooth is turned on
     */
    private static int checkBluetoothStatus() {
        int status;                                      // the status of the bluetooth
        if (bt_adapter == null) {                        // if so, the device does not support Bluetooth
            status = GlumoApplication.BT_NOT_SUPPORTED;  // set the proper status
        } else if (bt_adapter.isEnabled()) {             // if the bluetooth is enabled
            status = GlumoApplication.BT_BLUETOOTH_ON;   // just set the proper status
        } else {                                         // if bluetooth is enabled
            status = GlumoApplication.BT_BLUETOOTH_OFF;  // set the proper status and activate the bluetooth
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBTIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(enableBTIntent);
        }
        return status;                                  // eventually return
    }


    /**
     *  This utility method actually builds the intents with the action and the extra tags
     *  @param intent : the intent
     *  @param action : the intent action
     *  @param extras : a list of array made of two strings ["extra_data_tag", "extra_data"]
     */
    private static void buildIntent (Intent intent, String action, String[] ... extras) {
        intent.setAction(action);                   // set the intent action
        for (String[] extra : extras) {             // for each extra tag
            intent.putExtra(extra[0], extra[1]);    // insert the tag in the intent
        }
    }


    /**
     *  This just get the value of the BT_INTENT_UNSUCCESSFUL_TRIES_TAG tag from the intent, if any. Otherwise, it initializes it.
     *  @param intent : the intent
     *  @return tries_number : the BT_INTENT_UNSUCCESSFUL_TRIES_TAG tag content (0 if it wasn't set)
     */
    private static int getIntentUnsuccessfulTries (Intent intent) {
        int tries_number = intent.getIntExtra(GlumoApplication.BT_INTENT_UNSUCCESSFUL_TRIES_TAG, -1);
        if (tries_number == -1) {                                               // if the intent hasn't this extra
            intent.putExtra(GlumoApplication.BT_INTENT_UNSUCCESSFUL_TRIES_TAG, 0);    // create it and initialize it
            tries_number = 0;                                                   // now it's 0
        }
        return tries_number;
    }


    /**
     *  This just increment the value of the BT_INTENT_UNSUCCESSFUL_TRIES_TAG tag
     *  @param intent : the intent
     *  @return tries_number : the BT_INTENT_UNSUCCESSFUL_TRIES_TAG tag content incremented by one
     */
    private static int incrementIntentUnsuccessfulTries (Intent intent) {
        int tries_number = getIntentUnsuccessfulTries(intent);
        tries_number++;
        intent.putExtra(GlumoApplication.BT_INTENT_UNSUCCESSFUL_TRIES_TAG, tries_number);
        return tries_number;
    }
}
