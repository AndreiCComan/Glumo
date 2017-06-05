package glumo.com.glumo.fragment;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AppKeyPair;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;

import glumo.com.glumo.R;
import glumo.com.glumo.adapter.FileListAdapter;
import glumo.com.glumo.application.GlumoApplication;
import glumo.com.glumo.util.Appearance;
import glumo.com.glumo.util.ApplicationUtil;
import glumo.com.glumo.util.DBManager;

/**
 * This class handles the dropbox backup option
 */
public class BackupDropBoxFragment extends Fragment {

    // status of the api request
    public static String dropBoxRequestStatus = "null";

    // api instance
    private DropboxAPI<AndroidAuthSession> mDBApi = null;

    // view elements
    private View view;
    private Button importData;
    private Button exportData;
    private ListView fileListView;
    private ProgressDialog mProgress;

    /**
     * This method just sets the options menu
     * @param savedInstanceState previously saved instance
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
     * This method handles the creation of the activity view
     * @param inflater layout inflater
     * @param container viewgroup container
     * @param savedInstanceState previously saved instance
     * @return processed view
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // view
        view = inflater.inflate(R.layout.fragment_backup_dropbox, container, false);

        // general title
        Appearance.setTitle(getString(R.string.drawer_backup_data_dropbox));
        Appearance.removeActionBarShadow();

        // dropbox color
        int dropBoxColor = ContextCompat.getColor(GlumoApplication.getContext(), R.color.dropbox_blue);
        Appearance.setStatusBarColor(dropBoxColor);
        Appearance.setActionBarColor(dropBoxColor);

        // progress dialog
        mProgress = new ProgressDialog(getActivity());
        mProgress.setMessage(getResources().getString(R.string.backup_hang_on));

        // buttons for import/export
        importData = (Button) view.findViewById(R.id.import_data);
        exportData = (Button) view.findViewById(R.id.export_data);

        // associating listener to buttons, in order to perform the action
        importData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doDropBoxAction("getFilesList");
            }
        });

        exportData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doDropBoxAction("upload");
            }
        });

        return view;
    }

    /**
     * This method is called when the activity is resumed. It handles the action to perform on the basis
     * of the status of the request
     */
    @Override
    public void onResume() {
        super.onResume();

        // if there is an action to be performed (upload, download...) and also a mDBapi
        if (dropBoxRequestStatus.split(GlumoApplication.generalStringDelimiter).length > 1) {
            doDropBoxAction(dropBoxRequestStatus.split(GlumoApplication.generalStringDelimiter)[1]);
        }
        else if (dropBoxRequestStatus != "null" && mDBApi != null) {

            // if the authentication was successful
            if (mDBApi.getSession().authenticationSuccessful()) {
                try {
                    // Required to complete auth, sets the access token on the session
                    mDBApi.getSession().finishAuthentication();

                    // start the async task to perform the action
                    doDropBoxAction(dropBoxRequestStatus);

                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }
            else {
                Toast.makeText(getActivity(), "authentication failed", Toast.LENGTH_LONG).show();
                dropBoxRequestStatus = "null";
            }

        }
    }

    /**
     * This method is called when the activity is paused. It unlinks the session, if it was linked.
     */
    @Override
    public void onPause() {
        super.onPause();
        if (mDBApi != null) {
            if (mDBApi.getSession().isLinked()) {
                mDBApi.getSession().unlink();
            }
        }
    }


    /**
     * Initialize the Session of the Key pair to authenticate with dropbox
     * Start the authentication flow.
     * If Dropbox app is installed, SDK will switch to it otherwise it will fallback to the browser.
     * @param action : if the async task that invoked this method was trying to upload a backup -> "upload".
     *                 if the async task that invoked this method was trying to get the files lsit -> "getFilesList".
     *                 Otherwise, if it was trying to downloading a file -> name of the file
     */
    protected void doDropBoxAction(String action) {

        // if the app has not the proper permissions
        if (    ContextCompat.checkSelfPermission(GlumoApplication.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                ||
                ContextCompat.checkSelfPermission(GlumoApplication.getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            dropBoxRequestStatus = action;
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, GlumoApplication.PERMISSIONS_REQUEST_CODE_READ_WRITE_EXTERNAL_STORAGE_DROPBOX);
        }

        // otherwise
        else {

           // if the session is null
           if (mDBApi == null) {
               // store app key and secret key
               AppKeyPair appKeys = new AppKeyPair(GlumoApplication.APP_PUBLIC_KEY_DROPBOX, GlumoApplication.APP_SECRET_KEY_DROPBOX);

               // create the session given the keys
               AndroidAuthSession session = new AndroidAuthSession(appKeys);

               // Pass app key pair to the new DropboxAPI object.
               mDBApi = new DropboxAPI<>(session);
           }

           // if NOT AUTHENTICATED YET
           if (!mDBApi.getSession().authenticationSuccessful()) {

               // save the requested action
               dropBoxRequestStatus = action;

               // start authentication
               mDBApi.getSession().startOAuth2Authentication(getActivity());
           }

           // if AUTHENTICATED
           else {
               switch (action) {
                   // do a backup upload
                   case "upload":
                       new Upload().execute();
                       break;
                   // get the files list in dropbox
                   case "getFilesList":
                       new GetFilesList().execute();
                       break;
                   // null action
                   case "null":
                       break;
                   // do a backup download
                   default:
                       new Download().execute(action);
               }
               // null the status
               dropBoxRequestStatus = "null";
           }
       }
    }


    /**
     *  This method takes as input a list, and with a recycler view populates the list of files in the Dropbox directory
     *  @param data : the array list of files name
     */
    private void populateFilesList (ArrayList<String> data) {

        // file list to populate
        fileListView = (ListView) view.findViewById(R.id.dropbox_files_list_view);
        FileListAdapter fileListAdapter = new FileListAdapter(getActivity(), R.layout.file_item_row, data);
        fileListView.setAdapter(fileListAdapter);

        // associating a listener to every item of the view
        fileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                // file name
                final String fileName = (String) adapterView.getItemAtPosition(position);

                // dialog: it asks whether the user is sure about performing the recovery
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){

                            // if the user clicks YES, perform recovery
                            case DialogInterface.BUTTON_POSITIVE:
                                doDropBoxAction(fileName);
                                break;

                            // otherwise, go back
                            case DialogInterface.BUTTON_NEGATIVE:
                                break;
                        }
                    }
                };

                // display dialog
                Context context = GlumoApplication.getContext();
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(context.getString(R.string.dialog_import_data_question))
                        .setPositiveButton(context.getString(R.string.dialog_import_data_confirm), dialogClickListener)
                        .setNegativeButton(context.getString(R.string.dialog_import_data_decline), dialogClickListener).show();
            }
        });
    }

    /**
     * This class performs the retrieval of the list of files currently stored in the cloud
     */
    private class GetFilesList extends AsyncTask<Void, Boolean, ArrayList<String>> {

        /**
         * This method fills the list of files and returns it
         * @param arg0
         * @return list of files
         */
        protected ArrayList<String> doInBackground(Void... arg0) {

            // the files string list
            ArrayList<String> filesList = null;

            // If there is a network connection, fetch data
            if (ApplicationUtil.isDeviceOnline()) {

                try {
                    // initialize the array list
                    filesList = new ArrayList<>();

                    // my entry
                    DropboxAPI.Entry myEntry;

                    try {
                        // get the directory metadata
                        myEntry = mDBApi.metadata("/", 1000, null, true, null);
                    } catch (DropboxException e) {
                        e.printStackTrace();
                        return null;
                    }

                    // any CSV file found is added to the list
                    for (DropboxAPI.Entry ent : myEntry.contents) {
                        String fileName = ent.fileName();
                        if (fileName.endsWith(".csv")) {
                            filesList.add(ent.fileName());
                        }
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
            else {
                return null;
            }

            return filesList;
        }

        /**
         * This method just shows the progress dialog
         */
        @Override
        protected void onPreExecute() {
            mProgress.show();
        }

        /**
         * This method handles the conclusion of the task
         * @param result
         */
        @Override
        protected void onPostExecute(ArrayList<String> result) {

            // hiding progress dialog
            mProgress.hide();

            // if no suitable files are fuond, notify user
            if (result.size() == 0){
                Context context = GlumoApplication.getContext();
                Toast.makeText(context, context.getString(R.string.no_csv_files_found), Toast.LENGTH_SHORT).show();

            // otherwise, go on
            }else{
                populateFilesList(result);
            }
        }
    }

    /**
     * This class handles the upload of files on the cloud
     */
    private class Upload extends AsyncTask<Void, Boolean, Boolean> {

        /**
         * This method performs in background, actually handling the upload operation
         * @param arg0
         * @return outcome code for the operation (boolean success)
         */
        protected Boolean doInBackground(Void... arg0) {

            // did we manage to successfully upload the file?
            boolean success = true;

            // we will use this later on
            DropboxAPI.Entry response = null;

            // the temporarily backup file
            File file = null;

            // If there is a network connection, fetch data
            if (ApplicationUtil.isDeviceOnline()) {

                try {

                    // getting the file name
                    String fileName = GlumoApplication.db.exportToCsv();

                    // if the file name is null, it means that the file wasn't created
                    if (fileName != null) {

                        // getting the file
                        file = new File(Environment.getExternalStorageDirectory(), fileName);

                        // creating a file input stream from the file
                        FileInputStream inputStream = new FileInputStream(file);

                        // put the file in dropbox
                        response = mDBApi.putFile("/" + fileName, inputStream, file.length(), null, null);

                        // delete the file when the app is closed
                        file.delete();
                    }

                    // the file was not created
                    else {
                        success = false;
                    }
                } catch (Exception e) {
                    if (file != null)
                        file.delete();
                    success = false;
                    e.printStackTrace();
                }
            }
            else {
                success = false;
            }

            if (response == null) {
                success = false;
            }
            else if (response.rev.isEmpty())
                success = false;
            return success;
        }

        /**
         * This method just shows the progress dialog
         */
        @Override
        protected void onPreExecute() {
            mProgress.show();
        }

        /**
         * This method handles the conclusion of the task, on the basis of the outcome value
         * of the operations performed
         * @param result outcome value
         */
        @Override
        protected void onPostExecute(Boolean result) {

            // hiding progress dialog
            mProgress.hide();

            // on the basis of the code, notify user
            Context context = GlumoApplication.getContext();
            if(result){
                Toast.makeText(context, context.getString(R.string.file_export_success), Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(context, context.getString(R.string.file_export_fail), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * This class handles the download of backup files from the cloud
     */
    private class Download extends AsyncTask<String, Void, Boolean> {

        protected Boolean doInBackground(String... arg0) {

            // If there is a network connection, fetch data
            if (ApplicationUtil.isDeviceOnline()) {

                // a temporary file that will contain the data from the DropBox file
                String fileName = "glucoses_" + DBManager.getCurrentTimestamp() + ".csv";
                File file = new File(Environment.getExternalStorageDirectory(), fileName);

                // the output stream
                FileOutputStream outputStream = null;
                try {
                    // getting an output stream from the just created file
                    outputStream = new FileOutputStream(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return false;
                }

                try {
                    // trying to download the file
                    mDBApi.getFile(arg0[0], null, outputStream, null);
                } catch (DropboxException e) {
                    e.printStackTrace();
                    return false;
                }

                // call the import function
                return GlumoApplication.db.importFromCsv(fileName, true);
            }
            else {
                return false;
            }
        }

        /**
         * This method just shows the progress dialog
         */
        @Override
        protected void onPreExecute() {
            mProgress.show();
        }

        /**
         * This method handles the conclusion of the task, on the basis of the outcome code of the
         * previous operations
         * @param result outcome code
         */
        @Override
        protected void onPostExecute(Boolean result) {

            // hiding progress dialog
            mProgress.hide();

            // on the basis of the outcome code, notify user
            Context context = GlumoApplication.getContext();
            if(result){
                Toast.makeText(context, context.getString(R.string.file_import_success), Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(context, context.getString(R.string.file_import_corrupted_or_already_present), Toast.LENGTH_SHORT).show();
            }
        }
    }



}