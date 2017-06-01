package glumo.com.glumo.activity;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import glumo.com.glumo.R;
import glumo.com.glumo.application.GlumoApplication;
import glumo.com.glumo.fragment.AnalysisFragment;
import glumo.com.glumo.fragment.BackupDriveFragment;
import glumo.com.glumo.fragment.BackupDropBoxFragment;
import glumo.com.glumo.fragment.BackupLocalStorageFragment;
import glumo.com.glumo.fragment.BolusCalculatorFragment;
import glumo.com.glumo.fragment.CarbohydrateCheckerFragment;
import glumo.com.glumo.fragment.CreditsFragment;
import glumo.com.glumo.fragment.HomeFragment;
import glumo.com.glumo.fragment.SettingsFragment;
import glumo.com.glumo.util.Appearance;
import glumo.com.glumo.util.BluetoothUtil;

/**
 * This class
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    // view elements
    private DrawerLayout mDrawerLayout;
    private static NavigationView mNavigationView;
    private ActionBarDrawerToggle mDrawerToggle;
    private Toolbar toolbar;
    private static View progressWheelLayout;
    private static Menu menu;
    private ProgressBar progressWheel;

    public static String currentFragmentClass = null;
    public static boolean isGlucoseReadRequestSent = false;

    private static final Object sendGlucoseReadRequestLock = new Object();


    /**
     * This method just handles the layout for the view of the activity
     *
     * @param savedInstanceState previously saved instance
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set the content for the main activity
        setContentView(R.layout.activity_main);

        // get reference to the toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);

        // set the toolbar as actionbar
        setSupportActionBar(toolbar);

        // set the actionbar elevation to 0, it will be changed when the user scrolls
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // get reference to the drawer layout
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        // get reference to the navigation view
        mNavigationView = (NavigationView) findViewById(R.id.navigation_view);

        // create the action bar toggle
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.drawer_open, R.string.drawer_close);

        // drawer settings
        mDrawerToggle.setDrawerIndicatorEnabled(true);

        mDrawerLayout.addDrawerListener(mDrawerToggle);

        mNavigationView.setNavigationItemSelectedListener(this);

        // appearance features
        Appearance.setUpMainActivity(MainActivity.this);
        Appearance.setUpTitle();
        Appearance.updateActionBarAndStatusBarAndGradientColor();

        // layout features
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        progressWheelLayout = inflater.inflate(R.layout.progress_layout, null);
        progressWheel = (ProgressBar) progressWheelLayout.findViewById(R.id.progress_wheel);
        progressWheel.getIndeterminateDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        if (savedInstanceState == null) {
            setFragment(new HomeFragment());
        }
    }

    /**
     * This method just handles the progress wheel whenever a request in sent
     *
     * @param menu
     * @return true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.home_fragment_menu, menu);
        if (isGlucoseReadRequestSent()) {
            showWheel();
        } else {
            hideWheel();
        }
        return true;
    }


    /**
     * This method handles the actions to perform when an option is selected from
     * the options menu
     *
     * @param item selected item
     * @return boolean
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // getting item id
        int id = item.getItemId();

        // on the basis of the id
        switch (id) {

            // tell user to shake phone with a dialog
            case R.id.action_shake_phone: {
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle(getResources().getString(R.string.shake_dialog_title));

                // if functionality is enabled, notify user
                if (GlumoApplication.getBooleanPreference(R.string.shake_phone_to_make_read_switch_preference))
                    alertDialog.setMessage(getResources().getString(R.string.shake_dialog_text_enabled));

                    // otherwise, notify user about the possibility of using the functionality
                else
                    alertDialog.setMessage(getResources().getString(R.string.shake_dialog_text_disabled));
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getResources().getString(R.string.shake_dialog_button),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();

                // hide progression wheel
                hideWheel();

                break;
            }

            // sending glucose request
            case R.id.action_refresh: {
                sendGlucoseReadRequest();
                break;
            }
        }
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This method handles the action to perform once an entry is selected from the side
     * navigation bar
     *
     * @param item selected item
     * @return true
     */
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {

            // go to home fragment
            case R.id.drawer_home: {
                Fragment fragment = new HomeFragment();
                setFragment(fragment);
                break;
            }

            // go to analysis fragment
            case R.id.drawer_analysis: {
                Fragment fragment = new AnalysisFragment();
                setFragment(fragment);
                break;
            }

            // go to bolus calculator fragment
            case R.id.drawer_bolus_calculator: {
                Fragment fragment = new BolusCalculatorFragment();
                setFragment(fragment);
                break;
            }

            // go to carbohydrates checker fragment
            case R.id.drawer_carbohydrate_checker: {
                Fragment fragment = new CarbohydrateCheckerFragment();
                setFragment(fragment);
                break;
            }

            // go to settings fragment
            case R.id.drawer_settings: {
                Fragment fragment = new SettingsFragment();
                setFragment(fragment);
                break;
            }

            // go to credits fragment
            case R.id.drawer_credits: {
                Fragment fragment = new CreditsFragment();
                setFragment(fragment);
                break;
            }
            case R.id.drawer_backup_local_storage: {
                Fragment fragment = new BackupLocalStorageFragment();
                setFragment(fragment);
                break;
            }
            case R.id.drawer_backup_dropbox: {
                Fragment fragment = new BackupDropBoxFragment();
                setFragment(fragment);
                break;
            }
            case R.id.drawer_backup_drive: {
                Fragment fragment = new BackupDriveFragment();
                setFragment(fragment);
                break;
            }
        }

        mDrawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * This method just syncs the state of the drawer (open / close)
     *
     * @param savedInstanceState previously saved instance
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onBackPressed() {
    }

    /**
     * This method simply gets the current fragment
     */
    @Override
    protected void onResume() {
        super.onResume();
        currentFragmentClass = this.getFragmentManager().findFragmentById(R.id.content_frame).getClass().toString();
    }

    /**
     * This method simply discards the current fragment variable
     */
    @Override
    protected void onPause() {
        currentFragmentClass = "null";
        super.onPause();
    }


    @Override
    protected void onDestroy() {
        afterGlucoseReadRequestResponse();
        super.onDestroy();
    }


    /**
     * This method handles the settings of the preferences on the basis of the status of the
     * request
     *
     * @param requestCode  request code
     * @param permissions  permissions
     * @param grantResults grant results
     */
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case GlumoApplication.PERMISSIONS_REQUEST_CODE_SEND_SMS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // if true, no need to set the preference to "true".
                } else {
                    // if false, instead, ...
                    GlumoApplication.setPreference(R.string.alarm_SMS_switch_preference, false);
                }
                break;
            }
            case GlumoApplication.PERMISSIONS_REQUEST_CODE_READ_WRITE_EXTERNAL_STORAGE_DROPBOX: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    BackupDropBoxFragment.dropBoxRequestStatus = "onResume" + GlumoApplication.generalStringDelimiter + BackupDropBoxFragment.dropBoxRequestStatus;
                } else {
                    BackupDropBoxFragment.dropBoxRequestStatus = "null";
                }
                break;
            }
            case GlumoApplication.PERMISSIONS_REQUEST_CODE_READ_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // do nothing
                } else {
                    BackupLocalStorageFragment.localStorageRequestStatus = "null";
                }
                break;
            }
            case GlumoApplication.PERMISSIONS_REQUEST_CODE_READ_WRITE_EXTERNAL_STORAGE_DRIVE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    // do nothing
                } else {
                    BackupDriveFragment.driveRequestStatus = "null";
                }
                break;
            }
        }
    }

    /**
     * This method simply sets the current fragment variable on the basis of the parameter given
     *
     * @param fragment fragment
     */
    private void setFragment(Fragment fragment) {
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.content_frame, fragment);
        fragmentTransaction.commitAllowingStateLoss();
        currentFragmentClass = fragment.getClass().toString();
    }

    /**
     * This method simply sets as checked the item at the given position
     *
     * @param position item position
     */
    public static void checkMenuItem(int position) {
        mNavigationView.getMenu().getItem(position).setChecked(true);
    }

    /**
     * This method simply sets the variable isGlucoseReadRequestSent with the state
     * given as parameter
     *
     * @param state state
     */
    public static void setIsGlucoseReadRequestSent(boolean state) {
        isGlucoseReadRequestSent = state;
    }

    /**
     * This method simply returns the boolean value of isGlucoseReadRequestSent
     *
     * @return boolean value
     */
    public static boolean isGlucoseReadRequestSent() {
        return isGlucoseReadRequestSent;
    }

    /**
     * This method handles the sending of the request for a glucose read
     */
    public static void sendGlucoseReadRequest() {

        synchronized (sendGlucoseReadRequestLock) {

            // if the current fragment is not null (so the app is opened)
            if (currentFragmentClass != null) {

                // if we are in the home fragment
                if (HomeFragment.class.toString().equals(currentFragmentClass)) {

                    // if there is not an already sent request
                    if (!isGlucoseReadRequestSent()) {

                        // set the glucose read request status to true
                        setIsGlucoseReadRequestSent(true);

                        showWheel();

                        // make the intent start
                        BluetoothUtil.BTIntent(GlumoApplication.BT_READ_GLUCOSE);
                    }
                }
            }
        }
    }

    /**
     * This method handles the conclusion of the task for the sending of the request
     */
    public static void afterGlucoseReadRequestResponse() {

        synchronized (sendGlucoseReadRequestLock) {

            // if the current fragment is not null (so the app is opened)
            if (currentFragmentClass != null) {

                // set the glucose read request status to false
                setIsGlucoseReadRequestSent(false);

                // if we are in the home fragment
                if (HomeFragment.class.toString().equals(currentFragmentClass)) {

                    // hide the loading wheel
                    hideWheel();
                }
            }
        }
    }

    /**
     * This method simply hides the progress wheel
     */
    public static void hideWheel() {

        // if the menu is not null (safety check)
        if (menu != null) {

            // hide the loading wheel
            menu.findItem(R.id.action_refresh).setActionView(null);
        }
    }

    /**
     * This method simply shows the progress wheel
     */
    public static void showWheel() {
        // if the menu is not null (safety check)
        if (menu != null) {

            // show the loading wheel
            menu.findItem(R.id.action_refresh).setActionView(progressWheelLayout);
        }
    }

}