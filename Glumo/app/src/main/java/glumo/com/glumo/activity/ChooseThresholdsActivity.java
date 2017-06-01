package glumo.com.glumo.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.lantouzi.wheelview.WheelView;

import java.util.ArrayList;
import java.util.List;

import glumo.com.glumo.R;
import glumo.com.glumo.application.GlumoApplication;


/**
 * This first-configuration activity lets the user to choose the glucose thresholds through simple WheelViews
 */
public class ChooseThresholdsActivity extends AppCompatActivity {

    // WheelViews and respective values
    private WheelView hyperglycemiaWheelView, hypoglycemiaWheelView;
    private String hyperglycemiaValue, hypoglycemiaValue;
    private Activity thisActivity = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setContentView(R.layout.activity_choose_thresholds);

        // Get WheelViews references
        hyperglycemiaWheelView = (WheelView) findViewById(R.id.hyperglycemia_wheel_view);
        hypoglycemiaWheelView = (WheelView) findViewById(R.id.hypoglycemia_wheel_view);

        // WheelView items
        final List<String> items = new ArrayList<>();
        for (int i = 50; i <= 400; i=i+5) {
            items.add(String.valueOf(i));
        }

        // Set items to WheelViews
        hyperglycemiaWheelView.setItems(items);
        hypoglycemiaWheelView.setItems(items);

        // Set start indexes for WheelViews
        hyperglycemiaWheelView.selectIndex(18);
        hypoglycemiaWheelView.selectIndex(4);

        // Get reference to the button
        final Button button = (Button) findViewById(R.id.choose_thresholds_button);

        // Button click listener
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // get the choosen values
                hyperglycemiaValue = items.get(hyperglycemiaWheelView.getSelectedPosition());
                hypoglycemiaValue = items.get(hypoglycemiaWheelView.getSelectedPosition());

                if (Integer.valueOf(hypoglycemiaValue) >= Integer.valueOf(hyperglycemiaValue)) {
                    Toast.makeText(thisActivity, getResources().getString(R.string.choose_thresholds_activity_error_message), Toast.LENGTH_LONG).show();
                }
                else {
                    // set the chosen values in the preferences
                    GlumoApplication.setPreference(R.string.hyperglycemia_preference, Integer.valueOf(hyperglycemiaValue));
                    GlumoApplication.setPreference(R.string.hypoglycemia_preference, Integer.valueOf(hypoglycemiaValue));

                    // start the next first-configuration activity
                    Intent goToChooseBTDeviceActivity = new Intent(ChooseThresholdsActivity.this, ChooseBluetoothDeviceActivity.class);
                    startActivity(goToChooseBTDeviceActivity);
                }
            }
        });
    }
}