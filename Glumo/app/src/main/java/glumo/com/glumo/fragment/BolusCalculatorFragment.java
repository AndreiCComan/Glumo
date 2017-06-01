package glumo.com.glumo.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.shawnlin.numberpicker.NumberPicker;

import java.util.Random;

import glumo.com.glumo.R;
import glumo.com.glumo.application.GlumoApplication;
import glumo.com.glumo.util.Appearance;
import me.itangqi.waveloadingview.WaveLoadingView;

/**
 * This class handles the computation of bolus
 */
public class BolusCalculatorFragment extends Fragment {

    // view elements
    private View view;
    private LinearLayout bolusWaveContainer;
    private WaveLoadingView bolusWave;
    private Button calculateInsulinUnitsButton;
    private EditText carbohydrateValue;
    private TextView generalTip;
    private NumberPicker numberPickerIcarbRatio;

    /**
     * This method just sets the options menu
     * @param savedInstanceState options menu
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    /**
     * This method just handles the hiding of the options menu
     * @param menu options menu
     */
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem actionRefreshMenuItem = menu.findItem(R.id.action_refresh);
        actionRefreshMenuItem.setVisible(false);
        MenuItem actionShakePhoneMenuItem = menu.findItem(R.id.action_shake_phone);
        actionShakePhoneMenuItem.setVisible(false);
    }

    /**
     * This method handles the creation of the view for the activity
     * @param inflater layout inflater
     * @param container viewgroup container
     * @param savedInstanceState previously saved instance
     * @return processed view
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // view
        view = inflater.inflate(R.layout.fragment_bolus_calculator, container, false);

        // general title
        Appearance.setTitle(getString(R.string.drawer_bolus_calculator));
        Appearance.addActionBarShadow();
        Appearance.updateActionBarAndStatusBarAndGradientColor();

        // number picker
        numberPickerIcarbRatio = (NumberPicker) view.findViewById(R.id.number_picker_icarb_ratio);
        numberPickerIcarbRatio.setValue(GlumoApplication.getIntPreference(R.string.ic_ratio_preference));

        // array of tips
        final String[] tipsArray = getResources().getStringArray(R.array.tips_array);

        // tip element
        generalTip = (TextView) view.findViewById(R.id.general_tip);
        generalTip.setText(getRandomTip(tipsArray));

        // indicator of bolus wave
        bolusWaveContainer = (LinearLayout) view.findViewById(R.id.bolus_wave_container);

        // value of carbohydrates
        carbohydrateValue = (EditText) view.findViewById(R.id.carbohydrate_value_within_bolus_calculator);

        // Set listener on keyboard search button pressed
        carbohydrateValue.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    calculateInsulinUnitsAndUpdateTextView();
                    Appearance.hideKeyboard(view);
                    showWave();
                    return true;
                }
                return false;
            }
        });

        // bolus wave loading view
        bolusWave = (WaveLoadingView) view.findViewById(R.id.bolus_wave);
        bolusWave.setWaveColor(GlumoApplication.getIntPreference(R.string.theme_color));

        // button for calculating insulin
        calculateInsulinUnitsButton = (Button) view.findViewById(R.id.calculate_insulin_units_button);

        // associating listener to calculation button
        calculateInsulinUnitsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calculateInsulinUnitsAndUpdateTextView();
                Appearance.hideKeyboard(view);
                showWave();
            }
        });

        // show total carbohydrates value
        Bundle bundle = getArguments();
        if(bundle != null && bundle.containsKey("totalCarbohydratesValue")){
            String totalCarbohydratesValue = bundle.getString("totalCarbohydratesValue");
            carbohydrateValue.setText(totalCarbohydratesValue);
            calculateInsulinUnitsButton.callOnClick();
        }
        return view;
    }

    /**
     * This method just javes the wave animation
     */
    private void hideWave() {
        bolusWaveContainer.setVisibility(View.GONE);
    }

    /**
     * This method handles the display of the wave animation
     */
    private void showWave() {

        // string for carbohydrates value
        String carbohydrateString = carbohydrateValue.getText().toString();

        // if value is obtained, perform the calculation
        if(carbohydrateString!=null && !carbohydrateString.equals("") && !carbohydrateString.equals("0")){
            double carbohydrate = Double.valueOf(carbohydrateString);
            double icarbRatio = numberPickerIcarbRatio.getValue();
            double result = carbohydrate/icarbRatio;
            double roundedResult = Math.floor( result * 10 ) / 10;

            // display wave
            if(roundedResult >= 1 && roundedResult < 2)
                bolusWave.setCenterTitle((String.valueOf(roundedResult) + " " + getString(R.string.unit_of_insulin)));
            else
                bolusWave.setCenterTitle((String.valueOf(roundedResult) + " " + getString(R.string.units_of_insulin)));
            bolusWaveContainer.setVisibility(View.VISIBLE);
        }
    }

    /**
     * This method just handles the save of the task status (wave and tips)
     * @param outState outstate
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Save the state of the wave
        outState.putInt("bolusWaveState", bolusWaveContainer.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);
        // Save the state of the tip
        outState.putString("tipState", generalTip.getText().toString());
        // Save the center title of the wave
        outState.putString("waveCenterTitleState", bolusWave.getCenterTitle());
        super.onSaveInstanceState(outState);
    }

    /**
     * This method handles the elements of the view on the basis of the state given as parameter
     * @param savedInstanceState state of the task
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Get the state of the wave
            if (savedInstanceState.containsKey("bolusWaveState")) {
                if (savedInstanceState.getInt("bolusWaveState") == View.VISIBLE){
                    bolusWaveContainer.setVisibility(View.VISIBLE);
                } else{
                    bolusWaveContainer.setVisibility(View.GONE);
                }
            }
            // Get the state of the tip
            if(savedInstanceState.containsKey("tipState")){
                generalTip.setText(savedInstanceState.getString("tipState"));
            }
            // Get the state of the wave center title
            if(savedInstanceState.containsKey("waveCenterTitleState")){
                bolusWave.setCenterTitle(savedInstanceState.getString("waveCenterTitleState"));
            }
        }
        super.onActivityCreated(savedInstanceState);
    }

    /**
     * This method just retrieves a random tip to display
     * @param array array of tips
     * @return tip string
     */
    private String getRandomTip(String[] array) {
        int rnd = new Random().nextInt(array.length);
        return array[rnd];
    }

    private void calculateInsulinUnitsAndUpdateTextView(){
    }

}