package glumo.com.glumo.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import glumo.com.glumo.R;


/**
 *  This first-configuration activity lets the user to choose the glucose sensor
 *  Only supported sensors will be displayed
 */
public class EasterEggActivity extends AppCompatActivity {

    /**
     * This method just handles the layout for the view of the activity
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setContentView(R.layout.activity_easter_egg);

        // confirm sensor button
        final Button button = (Button) findViewById(R.id.go_back_button);

        // Button click listener
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }
}
