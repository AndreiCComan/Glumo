package glumo.com.glumo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import glumo.com.glumo.R;
import glumo.com.glumo.application.GlumoApplication;
import glumo.com.glumo.util.BroadcastAndAlarmManager;
import glumo.com.glumo.util.ShakePhoneListener;

/**
 *  This simple activity checks if the first configuration has successfully been completed
 *  If it's true, it jumps directly to the Main Activity
 */
public class GettingStartedActivity extends AppCompatActivity {
    private Button startButton;

    /**
     * This method just handles the layout for the view of the activity
     * @param savedInstanceState previously saved instance
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // load (only once) the user preferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // when opening the app, if the user previously enabled the shake functionality, register it
        if (GlumoApplication.getBooleanPreference(R.string.shake_phone_to_make_read_switch_preference)) {
            BroadcastAndAlarmManager.shakePhoneListener = ShakePhoneListener.register();
        }

        // checks if the first configuration has successfully been completed. If TRUE
        if (GlumoApplication.getBooleanPreference(R.string.first_configuration_completed)) {

            setContentView(R.layout.gradient);

            // create the intent for the main activity
            Intent goToMainActivity = new Intent(GettingStartedActivity.this, MainActivity.class);
            // start the main activity
            startActivity(goToMainActivity);
        }
        // if FALSE
        else {

            // Set layout
            setContentView(R.layout.activity_getting_started);
            // get the getting_started button
            startButton = (Button) findViewById(R.id.getting_started_button);
            // associate its listener
            startButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent goToEmergencyMessageActivity = new Intent(GettingStartedActivity.this, EmergencyMessageActivity.class);
                    startActivity(goToEmergencyMessageActivity);
                }
            });
        }
    }

}











