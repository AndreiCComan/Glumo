package glumo.com.glumo.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import glumo.com.glumo.R;
import glumo.com.glumo.application.GlumoApplication;

/**
 * This class handles the sending of emergency sms messages
 */
public class EmergencyMessageActivity extends AppCompatActivity {

    // buttons
    private Button acceptButton;
    private Button declineButton;

    /**
     * This method handles the layout for the view of the activity
     * @param savedInstanceState previously saved instance
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setContentView(R.layout.activity_emergency_message);

        // buttons
        declineButton = (Button) findViewById(R.id.emergency_message_decline);
        acceptButton = (Button) findViewById(R.id.emergency_message_accept);

        // associating listeners to buttons
        declineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GlumoApplication.setPreference(R.string.alarm_SMS_switch_preference, false);
                goToChooseDiabetesTypeActivity();
            }
        });
        acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(GlumoApplication.getContext(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(EmergencyMessageActivity.this, new String[]{Manifest.permission.SEND_SMS}, GlumoApplication.PERMISSIONS_REQUEST_CODE_SEND_SMS);
                }
                else {
                    GlumoApplication.setPreference(R.string.alarm_SMS_switch_preference, true);
                    goToChooseDiabetesTypeActivity();
                }
            }
        });
    }

    /**
     * This method sets sms preferences on the basis of the permission
     * @param requestCode request code
     * @param permissions permissions
     * @param grantResults grant results
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case GlumoApplication.PERMISSIONS_REQUEST_CODE_SEND_SMS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    GlumoApplication.setPreference(R.string.alarm_SMS_switch_preference, true);

                } else {
                    GlumoApplication.setPreference(R.string.alarm_SMS_switch_preference, false);
                }
                goToChooseDiabetesTypeActivity();
            }
            break;
        }
    }

    /**
     * This method handles the transition to the next activity
     */
    private void goToChooseDiabetesTypeActivity () {
        Intent goToChooseDiabetesTypeActivity = new Intent(EmergencyMessageActivity.this, ChooseDiabetesTypeActivity.class);
        startActivity(goToChooseDiabetesTypeActivity);
    }
}











