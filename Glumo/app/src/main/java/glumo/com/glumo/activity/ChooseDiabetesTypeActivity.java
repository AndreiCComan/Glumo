package glumo.com.glumo.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.shawnlin.numberpicker.NumberPicker;

import glumo.com.glumo.R;
import glumo.com.glumo.application.GlumoApplication;

/**
 *  This first-configuration activity lets the user to choose the diabetes type through a simple list
 *  If a diabetes type is not supported yet, the button will be disabled
 */
public class ChooseDiabetesTypeActivity extends AppCompatActivity {

    // the string that will contain the selected diabetes type value
    private String diabetesTypeValue;

    /**
     * This method handles the layout for the view of the activity
     * @param savedInstanceState previously saved instance
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setContentView(R.layout.activity_choose_diabetes_type);

        // Values to be displayed within diabetes type picker
        final String[] displayValues = new String[] {
                getString(R.string.type_1_diabetes),
                getString(R.string.type_2_diabetes),
                getString(R.string.gestational_diabetes)};

        // Confirm choose diabetes type button
        final Button button = (Button) findViewById(R.id.choose_diabetes_type_button);

        // Number picker user to display text instead of numbers
        final NumberPicker numberPicker = (NumberPicker) findViewById(R.id.number_picker_choose_diabetes_type_activity);
        numberPicker.setDisplayedValues(displayValues);

        // Button click listener
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // get the chosen value
                diabetesTypeValue = displayValues[numberPicker.getValue()];

                // set the chosen value in the preferences
                GlumoApplication.setPreference(R.string.diabetes_type_preference, diabetesTypeValue);

                // start the next first-configuration activity
                Intent chooseSensorActivityIntent = new Intent(ChooseDiabetesTypeActivity.this, ChooseSensorActivity.class);
                startActivity(chooseSensorActivityIntent);
            }
        });

        // Number picker listener
        numberPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int previousIndex, int currentIndex) {
                if(!displayValues[currentIndex].equals(getString(R.string.type_1_diabetes)))
                    button.setEnabled(false);
                else
                    button.setEnabled(true);
            }
        });
    }
}
