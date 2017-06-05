package glumo.com.glumo.util;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SmsManager;

import glumo.com.glumo.R;
import glumo.com.glumo.activity.MainActivity;
import glumo.com.glumo.application.GlumoApplication;
import glumo.com.glumo.fragment.HomeFragment;

import static android.content.Context.ALARM_SERVICE;
import static android.os.SystemClock.elapsedRealtime;

/**
 * Our handler for Intents, that are :
 * - BluetoothAdapter.ACTION_STATE_CHANGED (system broadcast)
 * - Intent.ACTION_BOOT_COMPLETED (system broadcast)
 * - GlumoApplication.BT_READ_GLUCOSE (directly addressed to this class)
 * - GlumoApplication.BT_READ_GLUCOSE_R (directly addressed to this class)
 * - GlumoApplication.BT_D_UNREACHABLE (directly addressed to this class)
 * - GlumoApplication.BT_N_SELECTED_DEVICE (directly addressed to this class)
 * - GlumoApplication.BAM_ALARM_NOTIFICATION_ACKNOWLEDGED (directly addressed to this class)
 * - GlumoApplication.BAM_ALARM_NOTIFICATION_NOT_ACKNOWLEDGED (directly addressed to this class)
 * <p>
 * This class also manages alarms (SMS) and notifications
 */
public class BroadcastAndAlarmManager extends BroadcastReceiver {

    private static int currentUnacknowledgedAlarmsNumber = 0;       // how many times did we tried to contact the glucometer unsuccessfully?
    private static Context ctx = GlumoApplication.getContext();     // simply the context
    public static ShakePhoneListener shakePhoneListener = null;

    private static int maxSecondsToAcknowledgeTheAlarms = 5;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null) {
            switch (action) {

                case BluetoothAdapter.ACTION_STATE_CHANGED:         // just send, through LocalBroadcast, a BT_ON / BT_OFF intent
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    if (state == BluetoothAdapter.STATE_OFF) {      // BT_OFF
                        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(GlumoApplication.BT_OFF));
                    } else if (state == BluetoothAdapter.STATE_ON) {  // BT_ON
                        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(GlumoApplication.BT_ON));
                    }
                    break;


                case Intent.ACTION_BOOT_COMPLETED:          // when boot is completed, this should make start the async read alarm
                    if (GlumoApplication.getBooleanPreference(R.string.first_configuration_completed)) {
                        BluetoothUtil.setAsyncRead(
                                Integer.valueOf(GlumoApplication.getStringPreference(R.string.update_asynchronously_data_frequency_preference)),
                                GlumoApplication.getBooleanPreference(R.string.update_asynchronously_data_frequency_switch_preference));
                    }
                    break;


                case GlumoApplication.BT_READ_GLUCOSE:      // received when the async read alarm triggers
                    BluetoothUtil.BTIntent(GlumoApplication.BT_READ_GLUCOSE);
                    break;


                case GlumoApplication.BT_READ_GLUCOSE_R:    // received when a glucose lecture has been done
                    currentUnacknowledgedAlarmsNumber = 0;  // reset the unreachable tries cont
                    int value = Integer.valueOf(intent.getStringExtra(GlumoApplication.ET_GLUCOMETER_RESPONSE_TAG));
                    int[] thresholds = {GlumoApplication.getIntPreference(R.string.hypoglycemia_preference), GlumoApplication.getIntPreference(R.string.hyperglycemia_preference)};
                    if (value <= thresholds[0]) {
                        sendNotificationAndAlarm(
                                ctx.getString(R.string.hypoglycemia),
                                ctx.getString(R.string.glicemy) + value + ctx.getString(R.string.glicemy_is_under_threshold),
                                true);
                    } else if (value >= thresholds[1]) {
                        sendNotificationAndAlarm(
                                ctx.getString(R.string.hyperglycemia),
                                ctx.getString(R.string.glicemy) + value + ctx.getString(R.string.glicemy_is_above_threshold),
                                true);
                    } else {
                        sendReadNotification(value, thresholds);
                    }
                    GlumoApplication.db.insertGlucoseRead(value);
                    break;


                case GlumoApplication.BT_D_UNREACHABLE:     // received when destination device is unreachable
                    MainActivity.afterGlucoseReadRequestResponse();
                    sendNotificationAndAlarm(
                            ctx.getString(R.string.notification_unreachable_device_title),
                            ctx.getString(R.string.notification_unreachable_device_text),
                            true);
                    break;

                case GlumoApplication.BT_N_SELECTED_DEVICE:// received when no glucometer device was selected
                    MainActivity.afterGlucoseReadRequestResponse();
                    sendNotificationAndAlarm(
                            ctx.getString(R.string.notification_not_selected_device_title),
                            ctx.getString(R.string.notification_not_selected_device_text),
                            false);
                    break;

                case GlumoApplication.BAM_ALARM_NOTIFICATION_ACKNOWLEDGED:
                    // reset the unreachable tries cont
                    currentUnacknowledgedAlarmsNumber = 0;

                    // cancel the alarm that would send the alarm SMS (if any)
                    Intent smsIntent = new Intent(ctx, BroadcastAndAlarmManager.class);
                    smsIntent.setAction(GlumoApplication.BAM_ALARM_NOTIFICATION_NOT_ACKNOWLEDGED);
                    PendingIntent piSmsIntent = PendingIntent.getBroadcast(ctx, GlumoApplication.PI_SEND_ALARM_SMS_CODE, smsIntent, 0);
                    ((AlarmManager) context.getSystemService(ALARM_SERVICE)).cancel(piSmsIntent);

                    // open the main activity
                    Intent i1 = new Intent(context, MainActivity.class);
                    i1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(i1);

                    break;


                case GlumoApplication.BAM_ALARM_NOTIFICATION_NOT_ACKNOWLEDGED:

                    // increment the number of not acknowledged notifications
                    currentUnacknowledgedAlarmsNumber++;
                    // if the number is above the chosen threshold
                    if (currentUnacknowledgedAlarmsNumber >= GlumoApplication.getIntPreference(R.string.unacknowledged_notifications_alarm_threshold_preference)) {
                        sendSMSAlarmMessage();
                    }
                    break;


                default:
                    break;
            }
        }
    }


    /**
     * This method builds up the notification whenever there was a read of the glucometer
     *
     * @param value : the glucose read value
     */
    public static void sendReadNotification(int value, int[] thr) {
        String currentFragment = (MainActivity.currentFragmentClass == null) ? "null" : MainActivity.currentFragmentClass;// TODO aggiunto

        if (!HomeFragment.class.toString().equals(currentFragment)) {

            // setting the light basing on the relative glucose value
            int color = ContextCompat.getColor(ctx, Appearance.getColorBasedOnThresholds(value, thr));

            // get the notification builder and build the notification with its properties
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(ctx)
                    .setSmallIcon(R.mipmap.glumo_white)
                    .setLargeIcon(BitmapFactory.decodeResource(ctx.getResources(), R.mipmap.glumo))
                    .setContentTitle(GlumoApplication.getContext().getString(R.string.notification_read_value_title))
                    .setContentText(String.valueOf(value))
                    .setAutoCancel(true)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setLights(color, 2000, 2000)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

            // getting the NotificationManager and issue the just created notification
            NotificationManager mNotifyMgr = (NotificationManager) ctx.getSystemService(ctx.NOTIFICATION_SERVICE);
            mNotifyMgr.notify(GlumoApplication.ALARM_NOTIFICATION_ID, mBuilder.build());
        }
    }


    /**
     * This method builds up the alarm notification and issues it
     *
     * @param notificationTitle : the notification title
     * @param notificationText  : the notification text
     * @param alarm             : has the alarm that will trigger if the user doesn't acknowledge the alarm in time to be set?
     */
    private static void sendNotificationAndAlarm(String notificationTitle, String notificationText, final boolean alarm) {

        Intent ackAlarmIntent = new Intent(ctx, BroadcastAndAlarmManager.class);            // Click on the notification will broadcast its action
        ackAlarmIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        ackAlarmIntent.setAction(GlumoApplication.BAM_ALARM_NOTIFICATION_ACKNOWLEDGED);     // actually setting the action and, below, creating the pending intent
        PendingIntent ackAlarmPI = PendingIntent.getBroadcast(ctx, GlumoApplication.PI_ACK_ALARM_CODE, ackAlarmIntent, 0);

        // get the notification builder and build the notification with its properties
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(ctx)
                .setSmallIcon(R.mipmap.glumo_white)
                .setLargeIcon(BitmapFactory.decodeResource(ctx.getResources(), R.mipmap.glumo))
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setContentIntent(ackAlarmPI)
                .setSound(Uri.parse(GlumoApplication.getStringPreference(R.string.ringtone_alarm_preference)), AudioAttributes.USAGE_ALARM);

        if (alarm) {

            // setting the alarm timer, that will trigger if the user doesn't acknowledge the alarm in time
            AlarmManager alarmManager = (AlarmManager) ctx.getSystemService(ALARM_SERVICE);
            Intent smsIntent = new Intent(ctx, BroadcastAndAlarmManager.class);
            smsIntent.setAction(GlumoApplication.BAM_ALARM_NOTIFICATION_NOT_ACKNOWLEDGED);
            smsIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            PendingIntent piSmsIntent = PendingIntent.getBroadcast(ctx, GlumoApplication.PI_SEND_ALARM_SMS_CODE, smsIntent, 0);
            alarmManager.set(AlarmManager.ELAPSED_REALTIME, elapsedRealtime() + (1000 * maxSecondsToAcknowledgeTheAlarms), piSmsIntent);
        }
        // getting the NotificationManager and issue the just created notification
        NotificationManager mNotifyMgr = (NotificationManager) ctx.getSystemService(ctx.NOTIFICATION_SERVICE);
        mNotifyMgr.notify(GlumoApplication.ALARM_NOTIFICATION_ID, mBuilder.build());

        PendingIntent.getBroadcast(ctx, GlumoApplication.PI_ACK_ALARM_CODE, ackAlarmIntent, PendingIntent.FLAG_NO_CREATE);
    }


    /**
     * This method checks the SMS permission and, if it has it, sends the alarm sms
     *
     * @return true if the app has the permission or the sms alarm is enabled (and therefore an alarm SMS should be have sent), false otherwise or if there was an exception
     */
    private static boolean sendSMSAlarmMessage() {
        boolean hasPermission = false;
        // if the app has the permission AND the option is enabled
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                GlumoApplication.getBooleanPreference(R.string.alarm_SMS_switch_preference)) {
            try {

                // if the number is not a number, throws exception
                Long.valueOf(GlumoApplication.getStringPreference(R.string.alarm_SMS_message_recipient_number_preference));

                SmsManager.getDefault().sendTextMessage(
                        GlumoApplication.getStringPreference(R.string.alarm_SMS_message_recipient_number_preference),
                        null,
                        GlumoApplication.getStringPreference(R.string.alarm_SMS_message_default_text),
                        null, null);
                hasPermission = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return hasPermission;
    }
}