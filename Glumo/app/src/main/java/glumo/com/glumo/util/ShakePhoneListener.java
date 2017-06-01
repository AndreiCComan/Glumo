package glumo.com.glumo.util;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import glumo.com.glumo.activity.MainActivity;
import glumo.com.glumo.application.GlumoApplication;
import glumo.com.glumo.fragment.HomeFragment;


/** this class is used in order to get the shake phone event to refresh the last glucose read
 */
public class ShakePhoneListener implements SensorEventListener {

    // SENSOR LISTENER SECTION
    private static SensorManager mSensorManager = null;                 // the sensor manager
    private static float mAccel = 0.00f;;                               // acceleration apart from gravity
    private static float mAccelCurrent = SensorManager.GRAVITY_EARTH;;  // current acceleration including gravity
    private static float mAccelLast = SensorManager.GRAVITY_EARTH;;     // last acceleration including gravity

    private static Long lastRequestTimeStamp = 0L;                      // the last request timestamp (in order to avoid sending multiple requests on the same shake phone event)

    private static Context ctx = GlumoApplication.getContext();         // the application content, for convenience

    private static final int sensorSensibility = 18;                    // how easy is to trigger the shake event?
    private static final int intervalBetweenRequests = 10000;           // the minimum amount of time between to requests


    @Override
    public void onSensorChanged(SensorEvent se) {

        // retrieving the values
        float x = se.values[0];
        float y = se.values[1];
        float z = se.values[2];

        // the (old) current acceleration is the old one for this new event
        mAccelLast = mAccelCurrent;

        // calculating the current acceleration
        mAccelCurrent = (float) Math.sqrt((double) (x*x + y*y + z*z));

        // the difference between the accelerations
        float delta = mAccelCurrent - mAccelLast;

        // perform low-cut filter
        mAccel = mAccel * 0.9f + delta;

        // if the threshold is exceeded and enough time has passed since the old sent request timestamp
        if ( mAccel > sensorSensibility && (System.currentTimeMillis() - lastRequestTimeStamp) > intervalBetweenRequests ) {

            // the last request timestamp is now
            lastRequestTimeStamp = System.currentTimeMillis();

            // if the home fragment is currently displayed
            if (HomeFragment.class.toString().equals(MainActivity.currentFragmentClass)) {

                // use the Main activity method to send the request
                // this in order to avoid requests coming simultaneously
                // from both shake event and click on refresh icon
                MainActivity.sendGlucoseReadRequest();
            }

            // otherwise, send the request manually
            else {

                // anyway, set the glucose request sent status
                MainActivity.setIsGlucoseReadRequestSent(true);

                // send the intent
                BluetoothUtil.BTIntent(GlumoApplication.BT_READ_GLUCOSE);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // do nothing
    }


    /** this method is used to register the listener. It is called either from the settings
     *  or from the GettingStartedActivity, if in the settings the shake phone listener is enabled
     *  @return shakePhoneListener the shakePhoneListener instance
     */
    public static ShakePhoneListener register () {

        // create a new shake phone listener instance
        ShakePhoneListener shakePhoneListener = new ShakePhoneListener();

        // if the sensor manager is null (either the app is just being created
        // or the shake listener is enabled for the first time in settings)
        if (mSensorManager == null) {
            mSensorManager = (SensorManager) ctx.getSystemService(ctx.SENSOR_SERVICE);
        }

        // register the listener
        mSensorManager.registerListener(shakePhoneListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);

        // return
        return shakePhoneListener;
    }


    /** this method is used to unregister the listener. It is called either from the settings
     *  or when the app is closed
     *  @param shakePhoneListener the shakePhoneListener instance
     */
    public static void unregister (ShakePhoneListener shakePhoneListener) {

        // a check on the variable
        if (shakePhoneListener != null)

            // un register the listener
            mSensorManager.unregisterListener(shakePhoneListener);
    }
}
