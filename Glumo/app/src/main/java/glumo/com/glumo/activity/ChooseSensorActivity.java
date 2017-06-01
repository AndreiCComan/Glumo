package glumo.com.glumo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import glumo.com.glumo.R;
import glumo.com.glumo.application.GlumoApplication;
import in.goodiebag.carouselpicker.CarouselPicker;


/**
 *  This first-configuration activity lets the user to choose the glucose sensor
 *  Only supported sensors will be displayed
 */
public class ChooseSensorActivity extends AppCompatActivity {

    // view
    private final int FREESTYLE_LIBRE_SENSOR_POSITION = 0;
    private final int IHEALTH_SMART_SENSOR_POSITION = 1;
    private final int DEXCOM_G5_SENSOR_POSITION = 2;
    private CarouselPicker carouselPicker;
    private ImageView sensorLogo;
    private TextView sensorName;
    private Button button;

    /**
     * This method handles the layout for the view of the activity
     * @param savedInstanceState previously saved instance
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setContentView(R.layout.activity_choose_sensor);

        // Reference carouselPicker
        carouselPicker = (CarouselPicker) findViewById(R.id.sensor_carousel);

        // Reference sensorLogo
        sensorLogo = (ImageView) findViewById(R.id.sensor_logo);

        // Reference sensorName
        sensorName = (TextView) findViewById(R.id.sensor_name);

        // Reference sensor button
        button = (Button) findViewById(R.id.choose_sensor_button);

        // Case 1 : To populate the picker with images
        final List<CarouselPicker.PickerItem> imageItems = new ArrayList<>();
        imageItems.add(new CarouselPicker.DrawableItem(R.drawable.libre_sensor));
        imageItems.add(new CarouselPicker.DrawableItem(R.drawable.ihealth_smart));
        imageItems.add(new CarouselPicker.DrawableItem(R.drawable.dexcom_g5));

        //Create an adapter
        CarouselPicker.CarouselViewAdapter imageAdapter = new CarouselPicker.CarouselViewAdapter(this, imageItems, 0);
        //Set the adapter
        carouselPicker.setAdapter(imageAdapter);

        // Button click listener
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // set the chosen sensor in the preferences
                GlumoApplication.setPreference(R.string.selected_sensor_preference, "FreeStyle Libre");

                // start the next first-configuration activity
                Intent chooseThresholdsActivityIntent = new Intent(ChooseSensorActivity.this, ChooseThresholdsActivity.class);
                startActivity(chooseThresholdsActivityIntent);
            }
        });

        // adding listener to the picker
        carouselPicker.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            // handling view of the sensors
            @Override
            public void onPageSelected(int position) {
                switch (position){
                    case FREESTYLE_LIBRE_SENSOR_POSITION:{
                        button.setEnabled(true);
                        sensorLogo.setImageResource(R.drawable.freestyle_libre_small_logo);
                        sensorName.setText("FreeStyle Libre");
                        break;
                    }
                    case IHEALTH_SMART_SENSOR_POSITION:{
                        button.setEnabled(false);
                        sensorLogo.setImageResource(R.drawable.ihealth_smart_small_logo);
                        sensorName.setText("iHealth Smart");
                        break;
                    }
                    case DEXCOM_G5_SENSOR_POSITION:{
                        button.setEnabled(false);
                        sensorLogo.setImageResource(R.drawable.dexcom_g5_small_logo);
                        sensorName.setText("Dexcom G5");
                        break;
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }
}
