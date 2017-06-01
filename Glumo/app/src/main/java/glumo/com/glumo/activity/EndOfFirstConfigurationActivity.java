package glumo.com.glumo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import glumo.com.glumo.R;
import glumo.com.glumo.application.GlumoApplication;

/**
 * This simple activity acknowledges the end of the first configuration, and redirect the user to the Main Activity
 */
public class EndOfFirstConfigurationActivity extends AppCompatActivity {
    private Button startButton;

    /**
     * This method just handles the layout for the view of the activity
     * @param savedInstanceState previously saved instance
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GlumoApplication.setPreference(R.string.first_configuration_completed, true);

        // Set layout
        setContentView(R.layout.activity_end_of_first_configuration);
        // get the getting_started button
        startButton = (Button) findViewById(R.id.end_of_first_configuration_button);
        // associate its listener
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent goToMainActivity = new Intent(EndOfFirstConfigurationActivity.this, MainActivity.class);
                startActivity(goToMainActivity);
            }
        });
    }
}