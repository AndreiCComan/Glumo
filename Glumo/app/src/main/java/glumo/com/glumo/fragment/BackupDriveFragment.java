package glumo.com.glumo.fragment;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import glumo.com.glumo.R;
import glumo.com.glumo.adapter.FileListAdapter;
import glumo.com.glumo.application.GlumoApplication;
import glumo.com.glumo.util.Appearance;
import glumo.com.glumo.util.ApplicationUtil;

public class BackupDriveFragment extends Fragment {

    public static String driveRequestStatus = "null";

    private GoogleAccountCredential mCredential;
    private ProgressDialog mProgress;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final String BUTTON_TEXT = "Call Drive API";
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {DriveScopes.DRIVE};

    private static final String fileNameIdDelimiter = "\nid: ";

    private View view;
    private Button importData;
    private Button exportData;
    private ListView fileListView;

    /**
     * This method sets the credentials for google drive
     * @param savedInstanceState previously saved instance state
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(GlumoApplication.getContext(), Arrays.asList(SCOPES)).setBackOff(new ExponentialBackOff());
    }

    /**
     * This method handles the hiding of unnecessary elements
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
     * This method handles the general view of the activity, in its initial configuration
     * @param inflater layout inflater
     * @param container view group container
     * @param savedInstanceState previously saved instance state
     * @return view that has been set
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_backup_drive, container, false);

        // title
        Appearance.setTitle(getString(R.string.drawer_backup_data_drive));
        Appearance.removeActionBarShadow();
        int dropBoxColor = ContextCompat.getColor(GlumoApplication.getContext(), R.color.google_drive_color);
        Appearance.setStatusBarColor(dropBoxColor);
        Appearance.setActionBarColor(dropBoxColor);

        // progress dialog
        mProgress = new ProgressDialog(getActivity());
        mProgress.setMessage(getResources().getString(R.string.backup_hang_on));

        // buttons
        importData = (Button) view.findViewById(R.id.import_data);
        exportData = (Button) view.findViewById(R.id.export_data);

        importData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doDriveAction("getFilesList");
            }
        });

        exportData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doDriveAction("upload");
            }
        });

        return view;
    }

    /**
     * This method calls the function to actually perform the action chosen when the app is resumed
     */
    @Override
    public void onResume() {
        super.onResume();
        if (driveRequestStatus != "null") {
            doDriveAction(driveRequestStatus);
        }

    }


    @Override
    public void onPause() {
        super.onPause();
    }





    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate. This method also initializes the Session of the Key pair to authenticate with dropbox
     * Start the authentication flow.
     * If Dropbox app is installed, SDK will switch to it otherwise it will fallback to the browser.
     * @param action : if the async task that invoked this method was trying to upload a backup -> "upload".
     *                 if the async task that invoked this method was trying to get the files lsit -> "getFilesList".
     *                 Otherwise, if it was trying to downloading a file -> name of the file
     */
    private void doDriveAction (String action) {

        // if the app has not the proper permissions
        if (    ContextCompat.checkSelfPermission(GlumoApplication.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                ||
                ContextCompat.checkSelfPermission(GlumoApplication.getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                ||
                ContextCompat.checkSelfPermission(GlumoApplication.getContext(), Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED){

            // save the request status
            driveRequestStatus = action;

            // start the activity for getting the permissions
            ActivityCompat.requestPermissions(getActivity(), new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.GET_ACCOUNTS},
                    GlumoApplication.PERMISSIONS_REQUEST_CODE_READ_WRITE_EXTERNAL_STORAGE_DRIVE);
        }

        // otherwise
        else {

            // if google play services is not available
            if (!isGooglePlayServicesAvailable()) {

                // keep track of the action
                driveRequestStatus = action;

                // acquire services
                acquireGooglePlayServices();
            }

            // otherwise, if no account was selected
            else if (mCredential.getSelectedAccountName() == null) {

                // keep track of action
                driveRequestStatus = action;

                // ask for the account
                startActivityForResult( mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
            }

            // otherwise
            else {

                // on the basis of the action, execute the operation associated to it
                switch (action) {
                    // do a backup upload
                    case "upload":
                        new Upload(mCredential).execute();
                        break;
                    // get the files list in drive
                    case "getFilesList":
                        new GetFilesList(mCredential).execute();
                        break;
                    // null action
                    case "null":
                        break;
                    // do a backup download
                    default:
                        new Download(mCredential).execute(action);
                }
                // null the status
                driveRequestStatus = "null";
            }
        }
    }




    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     *
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode  code indicating the result of the incoming
     *                    activity result.
     * @param data        Intent (containing result data) returned by incoming
     *                    activity result.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // on the basis of the request code, handles the action
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:

                // if google play services is installed, go on
                if (resultCode == getActivity().RESULT_OK) {
                    doDriveAction(driveRequestStatus);
                }

                // otherwise, notify user
                else {
                    Toast.makeText(getActivity(), getResources().getString(R.string.drive_backup_request_google_play_services), Toast.LENGTH_LONG).show();
                    driveRequestStatus = "null";
                }
                break;
            case REQUEST_ACCOUNT_PICKER:

                // if the user selects the account, go on
                if (resultCode == getActivity().RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        mCredential.setSelectedAccountName(accountName);
                        doDriveAction(driveRequestStatus);
                    }

                    // otherwise, notify user
                    else {
                        Toast.makeText(getActivity(), getResources().getString(R.string.drive_backup_error_no_account_selected), Toast.LENGTH_LONG).show();
                        driveRequestStatus = "null";
                    }
                }

                // otherwise, keep track of error
                else {
                    driveRequestStatus = "null";
                }
                break;

            // if the user grants authorization, go on
            case REQUEST_AUTHORIZATION:
                if (resultCode == getActivity().RESULT_OK) {
                    doDriveAction(driveRequestStatus);
                }

                // otherwise, keep track of error
                else {
                    driveRequestStatus = "null";
                }
                break;
        }
    }


    /**
     * Check that Google Play services APK is installed and up to date.
     *
     * @return true if Google Play Services is available and up to
     * date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(getActivity());
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }


    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(getActivity());
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }


    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     *
     * @param connectionStatusCode code describing the presence (or lack of)
     *                             Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(getActivity(), connectionStatusCode, REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }


    /**
     * This method populates the list of files contained in the cloud
     * @param data original list of files obtained
     */
    private void populateFilesList (ArrayList<String> data) {

        // getting list
        fileListView = (ListView) view.findViewById(R.id.drive_files_list_view);
        FileListAdapter fileListAdapter = new FileListAdapter(getActivity(), R.layout.file_item_row, data);
        fileListView.setAdapter(fileListAdapter);

        // listener associated to every item
        fileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                final String folderName = (String) adapterView.getItemAtPosition(position);

                // show dialog to user, to double check his choice
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){

                            // if user confirms, go on
                            case DialogInterface.BUTTON_POSITIVE:
                                doDriveAction(folderName);
                                break;

                            // otherwise, go back
                            case DialogInterface.BUTTON_NEGATIVE:
                                break;
                        }
                    }
                };

                // associated listener to dialog
                Context context = GlumoApplication.getContext();
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(context.getString(R.string.dialog_import_data_question))
                        .setPositiveButton(context.getString(R.string.dialog_import_data_confirm), dialogClickListener)
                        .setNegativeButton(context.getString(R.string.dialog_import_data_decline), dialogClickListener).show();
            }
        });
    }

    /**
     * This asynchronous method obtains the list of files contained in the cloud
     */
    private class GetFilesList extends AsyncTask<Void, Void, ArrayList<String>> {
        private com.google.api.services.drive.Drive mService = null;
        private Exception mLastError = null;

        // actually getting the list
        GetFilesList(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.drive.Drive.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Glumo")
                    .build();
        }

        /**
         * Background task to call Drive API.
         *
         * @param params no parameters needed for this task.
         */
        @Override
        protected ArrayList<String> doInBackground(Void... params) {
            if (ApplicationUtil.isDeviceOnline()) {
                try {
                    return getListOfFilesFromDrive();
                } catch (Exception e) {
                    mLastError = e;
                    cancel(true);
                    return null;
                }
            }
            else {
                return null;
            }
        }

        /**
         * Fetch a list of up to 100 file names and IDs.
         *
         * @return List of Strings describing files, or an empty list if no files
         * found.
         * @throws IOException
         */
        private ArrayList<String> getListOfFilesFromDrive() throws IOException {
            // Get a list of up to 100 files.
            ArrayList<String> fileInfo = new ArrayList<>();
            FileList result = mService.files().list()
                    .setPageSize(100)
                    .setFields("nextPageToken, files(id, name)")
                    .execute();

            // parsing files, extracting all the CSV ones
            List<File> files = result.getFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".csv")) {
                        fileInfo.add(file.getName() + fileNameIdDelimiter + file.getId());
                    }
                }
            }
            return fileInfo;
        }

        // shows progress dialog
        @Override
        protected void onPreExecute() {
            mProgress.show();
        }

        /**
         * This method handles the conclusion of the task
         * @param result the outcome of the previous operations
         */
        @Override
        protected void onPostExecute(ArrayList<String> result) {

            // hiding progress dialog
            mProgress.hide();

            // if no csv files are found, notify user
            if (result.size() == 0){
                Context context = GlumoApplication.getContext();
                Toast.makeText(context, context.getString(R.string.no_csv_files_found), Toast.LENGTH_SHORT).show();

                // otherwise, show list
            }else{
                populateFilesList(result);
            }
        }

        /**
         * This method handles any error previously generated
         */
        @Override
        protected void onCancelled() {

            // hiding progress dialog
            mProgress.hide();

            // notifying user on the basis of the error
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            REQUEST_AUTHORIZATION);
                } else {
                    Toast.makeText(getActivity(), getResources().getString(R.string.drive_backup_error), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(getActivity(), getResources().getString(R.string.drive_backup_error_request_canceled), Toast.LENGTH_LONG).show();

            }
        }
    }


    /**
     * This asynchronous method handles the upload of the backup file
     */
    private class Upload extends AsyncTask<Void, Boolean, Boolean> {
        private com.google.api.services.drive.Drive mService = null;
        private Exception mLastError = null;

        // constructor method
        Upload(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.drive.Drive.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Glumo")
                    .build();
        }

        /**
         * Background task to call Drive API.
         *
         * @param params no parameters needed for this task.
         */
        @Override
        protected Boolean doInBackground(Void... params) {
            // did we manage to successfully upload the file?
            boolean success = true;

            // checks if the device is online
            if (ApplicationUtil.isDeviceOnline()) {

                // getting the file name and path
                String fileNameDrive = GlumoApplication.db.exportToCsv();
                String fileNameLocal = Environment.getExternalStorageDirectory() + "/" + fileNameDrive;

                try {
                    // creates new file
                    File fileMetadata = new File();

                    // sets name
                    fileMetadata.setName(fileNameDrive);

                    // accesses file
                    java.io.File filePath = new java.io.File(fileNameLocal);
                    FileContent mediaContent = new FileContent("text/csv", filePath);
                    mService.files().create(fileMetadata, mediaContent)
                            .setFields("id")
                            .execute();

                    // getting the file
                    java.io.File fileToDelete = new java.io.File(Environment.getExternalStorageDirectory(), fileNameDrive);

                    // delete the file when the app is closed
                    fileToDelete.delete();

                } catch (Exception e) {
                    e.printStackTrace();
                    mLastError = e;
                    cancel(true);
                }
            }
            return success;
        }

        // showing progress dialog
        @Override
        protected void onPreExecute() {
            mProgress.show();
        }

        /**
         * This method handles the conclusion of the task
         * @param result the outcome of the previous operations
         */
        @Override
        protected void onPostExecute(Boolean result) {

            // hiding progress dialog and notifying user
            mProgress.hide();
            Toast.makeText(GlumoApplication.getContext(), GlumoApplication.getContext().getString(R.string.file_export_success), Toast.LENGTH_SHORT).show();
        }

        /**
         * This method handles errors notifications, showing to the user a short explanation
         * on the basis of the error code
         */
        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            REQUEST_AUTHORIZATION);
                } else {
                    Toast.makeText(getActivity(), getResources().getString(R.string.drive_backup_error), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(getActivity(), getResources().getString(R.string.drive_backup_error_request_canceled), Toast.LENGTH_LONG).show();
            }
        }
    }




    /**
     * This asynchronous method handles the upload of the backup file
     */
    private class Download extends AsyncTask<String, Void, Boolean> {
        private com.google.api.services.drive.Drive mService = null;
        private Exception mLastError = null;

        // constructor method
        Download(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.drive.Drive.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Glumo")
                    .build();
        }

        /**
         * Background task to call Drive API.
         *
         * @param params no parameters needed for this task.
         */
        @Override
        protected Boolean doInBackground(String... params) {

            // checking if device is online
            if (ApplicationUtil.isDeviceOnline()) {

                // getting file name and file id
                String fileName = params[0].split(fileNameIdDelimiter)[0];
                String fileId = params[0].split(fileNameIdDelimiter)[1];

                // accessing file
                java.io.File file = new java.io.File(Environment.getExternalStorageDirectory(), fileName);

                // creating outputstream
                FileOutputStream outputStream = null;

                try {

                    // associate outputstream to file
                    outputStream = new FileOutputStream(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return false;
                }
                try {
                    // download content do outputstream
                    mService.files().get(fileId).executeMediaAndDownloadTo(outputStream);
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }

                // import data to database from csv generated with the imported file
                return GlumoApplication.db.importFromCsv(fileName, true);
            }
            else {
                return false;
            }
        }

        // show progress dialog
        @Override
        protected void onPreExecute() {
            mProgress.show();
        }

        // hide progress dialog
        @Override
        protected void onPostExecute(Boolean result) {
            mProgress.hide();

            // on the basis of the outcome code, notify user
            Context context = GlumoApplication.getContext();
            if(result){
                Toast.makeText(context, context.getString(R.string.file_import_success), Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(context, context.getString(R.string.file_import_fail), Toast.LENGTH_SHORT).show();
            }
        }

        /**
         * This method handles errors notifications, showing to the user a short explanation
         * on the basis of the error code
         */
        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            REQUEST_AUTHORIZATION);
                } else {
                    Toast.makeText(getActivity(), getResources().getString(R.string.drive_backup_error), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(getActivity(), getResources().getString(R.string.drive_backup_error_request_canceled), Toast.LENGTH_LONG).show();

            }
        }
    }
}