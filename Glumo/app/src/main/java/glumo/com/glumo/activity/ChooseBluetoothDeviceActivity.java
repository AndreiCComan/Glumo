package glumo.com.glumo.activity;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import glumo.com.glumo.R;
import glumo.com.glumo.adapter.BluetoothListAdapter;
import glumo.com.glumo.application.GlumoApplication;
import glumo.com.glumo.util.BluetoothUtil;


/**
 *  This class lets the user set for the first time the default bluetooth paired device
 */
public class ChooseBluetoothDeviceActivity extends AppCompatActivity {

    private TextView btStatusInfo;  // info about the bluetooth status
    private ListView listV;         // bluetooth paired devices list view

    /**
     *  Our handler for received Intents. This will be called whenever an Intent
     *  with a registered action is broadcasted. Registered are:
     *  - BT_GET_PAIRED_DEVICES_R
     *  - BT_N_SUPPORTED
     */
    private BroadcastReceiver localReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {   // if action is not null
                switch (action) {   // switch over it

                    case GlumoApplication.BT_N_SUPPORTED:
                        btStatusInfo.setVisibility(View.VISIBLE);
                        break;

                    case GlumoApplication.BT_GET_PAIRED_DEVICES_R:          // get the bluetooth devices list
                        Serializable list = intent.getSerializableExtra(GlumoApplication.BT_PAIRED_DEVICES_LIST_TAG);
                        ArrayList<BluetoothDevice> devicesList = new ArrayList<>();
                        if (list != null) {                                 // if not null
                            devicesList = (ArrayList<BluetoothDevice>) list;// cast it into an array lsit
                        }
                        if (!devicesList.isEmpty()) {                       // it it is not empty
                            refreshPairedDevicesList( ((List)devicesList) );// call the refresh paired list method
                        }
                        else {                                              // else, set the proper text error
                            btStatusInfo.setText(getResources().getString(R.string.bluetooth_choose_device_no_paired_devices_alert));
                        }
                        break;

                    default:
                        break;
                }
            }
        }
    };

    /**
     * This method handles the layout of the view for this activity
     * @param savedInstanceState previously saved instance
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);                                 // super class method

        setContentView(R.layout.activity_bluetooth_choose_device);          // set the activity layout

        btStatusInfo = (TextView) findViewById(R.id.brm_info);              // getting references
        listV = (ListView) findViewById(R.id.bluetooth_choose_device_devices_list_view);

        Button choose_button = (Button) findViewById(R.id.bluetooth_choose_device_choose_button);
        choose_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {                           // Set listener for choose device button
                // make the BT_GET_PAIRED_DEVICES search starts
                BluetoothUtil.BTIntent(GlumoApplication.BT_GET_PAIRED_DEVICES);
            }
        });
        Button do_it_later_button = (Button) findViewById(R.id.bluetooth_choose_device_do_it_later_button);
        do_it_later_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {                      // Set listener for do_it_later button
                // go to the next activity
                goToNextActivity();
            }
        });
        BluetoothUtil.BTIntent(GlumoApplication.BT_GET_PAIRED_DEVICES);
    }

    /**
     * This method handles the filters of the bluetooth intent
     */
    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filters = new IntentFilter();                        // associate the intents to the broadcast receiver, then register it
        filters.addAction(GlumoApplication.BT_N_SUPPORTED);               // bluetooth not supported intent
        filters.addAction(GlumoApplication.BT_GET_PAIRED_DEVICES_R);      // response to the get paired device request intent
        LocalBroadcastManager.getInstance(this).registerReceiver(localReceiver, filters);
    }


    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(localReceiver);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    /**
     *  Given the devices list, refresh the list view
     */
    private void refreshPairedDevicesList(List devicesList) {
        BluetoothListAdapter btListAdpt = new BluetoothListAdapter(this, R.layout.bluetooth_device_row, devicesList);
        listV.setAdapter(btListAdpt);
        listV.setOnItemClickListener(new AdapterView.OnItemClickListener() {                         // on item click
            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {

                // get the device associated to the clicked item
                BluetoothDevice bluetoothDevice = (BluetoothDevice)adapter.getItemAtPosition(position);

                // set the relative preference
                GlumoApplication.setPreference(R.string.paired_device_preference, bluetoothDevice.getName() + GlumoApplication.stringBetweenDeviceNameAndAddress + bluetoothDevice.getAddress());

                // go to the next activity
                goToNextActivity();
            }
        });
    }

    /**
     * This method handles the transition to a new activity, choosing between the main activity and the
     * end of configuration if not yet completed
     */
    private void goToNextActivity () {
        // checks if the first configuration has successfully been completed. If TRUE
        if (GlumoApplication.getBooleanPreference(R.string.first_configuration_completed)) {
            // create the intent for the main activity
            Intent goToMainActivity = new Intent(ChooseBluetoothDeviceActivity.this, MainActivity.class);
            // start the main activity
            startActivity(goToMainActivity);
        }
        else {
            // start the end-of-configuration activity
            Intent endOfFirstConfigurationActivity = new Intent(ChooseBluetoothDeviceActivity.this, EndOfFirstConfigurationActivity.class);
            startActivity(endOfFirstConfigurationActivity);
        }
    }
}