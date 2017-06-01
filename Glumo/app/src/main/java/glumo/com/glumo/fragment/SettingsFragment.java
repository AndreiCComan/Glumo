package glumo.com.glumo.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.RingtonePreference;
import android.preference.SwitchPreference;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import glumo.com.glumo.R;
import glumo.com.glumo.activity.ChooseBluetoothDeviceActivity;
import glumo.com.glumo.activity.EasterEggActivity;
import glumo.com.glumo.application.GlumoApplication;
import glumo.com.glumo.util.Appearance;
import glumo.com.glumo.util.BluetoothUtil;
import glumo.com.glumo.util.BroadcastAndAlarmManager;
import glumo.com.glumo.util.ShakePhoneListener;


/**
 * This class handles the display and functionality of the settings activity
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    // easter egg settings
    private final int CLICKS_FOR_EASTER_EGG = 10;
    private int numberOFClicksCurrentlyForEasterEgg = 0;
    private Toast toast;

    /**
     * This method retrieves the preferences to be displayed
     * @param savedInstanceState previously saved instance
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        getPreferenceScreen().removePreference(findPreference(getString(R.string.first_configuration_completed)));
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            getPreferenceScreen().removePreference(findPreference(getString(R.string.theme_preference_category)));
        }
        // setting options menu
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
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {

        // general title
        Appearance.setTitle(getString(R.string.drawer_settings));
        Appearance.updateActionBarAndStatusBarAndGradientColor();

        // setting preferences
        SharedPreferences s = android.preference.PreferenceManager.getDefaultSharedPreferences(GlumoApplication.getContext());
        onSharedPreferenceChanged(s, getString(R.string.ringtone_alarm_preference));
        onSharedPreferenceChanged(s, getString(R.string.alarm_SMS_message_recipient_number_preference));
        onSharedPreferenceChanged(s, getString(R.string.alarm_SMS_message_default_text));
        onSharedPreferenceChanged(s, getString(R.string.paired_device_preference));
        findPreference(getString(R.string.paired_device_preference)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent bluetoothReadingModeIntent = new Intent(getActivity(), ChooseBluetoothDeviceActivity.class);
                startActivity(bluetoothReadingModeIntent);
                return true;
            }
        });
        findPreference(getString(R.string.app_version_preference)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                // easter egg
                numberOFClicksCurrentlyForEasterEgg++;
                int difference = CLICKS_FOR_EASTER_EGG - numberOFClicksCurrentlyForEasterEgg;
                if(difference > 0){
                    String text = difference + " " + getString(R.string.clicks_to_easter_egg);
                    if(toast == null){
                        toast = Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT);
                    }
                    toast.setText(text);
                    toast.show();
                }else{
                    Intent easterEggIntent = new Intent(getActivity(), EasterEggActivity.class);
                    startActivity(easterEggIntent);
                }
                return true;
            }
        });

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    /**
     * This methods handle the retrieval of preferences
     */
    @Override
    public void onStart() {
        super.onStart();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }
    @Override
    public void onStop() {
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onStop();
    }

    /**
     * This method handles the changes in the preferences of the user
     * @param sharedPreferences preferences
     * @param key preference key
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference preference = findPreference(key);        // Get the preference from the key

        // For NumberPickerPreference
        if (preference instanceof glumo.com.glumo.object.NumberPickerPreference) {
            // the summary to reflect the new value is setted by the class itself
        }
        // For SwitchPreference
        else if (preference instanceof SwitchPreference) {
            // the summary to reflect the new value is setted by the class itself
            // if the switch preference is the alarm sms one
            if (key.equals(getString(R.string.alarm_SMS_switch_preference))) {
                // if the user switched to "true"
                if (sharedPreferences.getBoolean(key, true)) {
                    // If the app doesn't have the permission, ask for it
                    if (ContextCompat.checkSelfPermission(GlumoApplication.getContext(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this.getActivity(), new String[]{Manifest.permission.SEND_SMS}, GlumoApplication.PERMISSIONS_REQUEST_CODE_SEND_SMS);
                    }
                }
                else {
                    ((SwitchPreference) preference).setChecked(false);
                }
            }
            else if (key.equals(getString(R.string.shake_phone_to_make_read_switch_preference))) {
                ShakePhoneListener.unregister(BroadcastAndAlarmManager.shakePhoneListener);
                if (sharedPreferences.getBoolean(key, true)) {
                    BroadcastAndAlarmManager.shakePhoneListener = ShakePhoneListener.register();
                }
            }
        }
        // For Ringtone preferences
        else if (preference instanceof RingtonePreference) {
            Ringtone ringtone = RingtoneManager.getRingtone( preference.getContext(), Uri.parse(sharedPreferences.getString(key, "")));
            if (ringtone == null) {
                preference.setSummary(null);    // clear the summary if there was a lookup error.
            }
            else {                              // set the summary to reflect the new ringtone display name
                String name = ringtone.getTitle(preference.getContext());
                preference.setSummary(name);
            }
        }
        // For Color preferences (theme color)
        else if (preference instanceof com.thebluealliance.spectrum.SpectrumPreference) {
            Appearance.updateActionBarAndStatusBarAndGradientColor();  // update the theme
        }
        // For everything else
        else {
            // Set the summary to reflect the new value.
            preference.setSummary( sharedPreferences.getString(key, ""));
        }


        // handling bluetooth functionality on the basis of the preferences
        if (getResources().getString(R.string.update_asynchronously_data_frequency_switch_preference).equals(key)) {
            if (GlumoApplication.getBooleanPreference(R.string.first_configuration_completed)) {
                BluetoothUtil.setAsyncRead(
                        Integer.valueOf(GlumoApplication.getStringPreference(R.string.update_asynchronously_data_frequency_preference)),
                        GlumoApplication.getBooleanPreference(R.string.update_asynchronously_data_frequency_switch_preference));
            }
        }
        else if (getResources().getString(R.string.update_asynchronously_data_frequency_preference).equals(key)) {
            BluetoothUtil.setAsyncRead(
                    Integer.valueOf(GlumoApplication.getStringPreference(R.string.update_asynchronously_data_frequency_preference)),
                    GlumoApplication.getBooleanPreference(R.string.update_asynchronously_data_frequency_switch_preference));
        }
    }
}