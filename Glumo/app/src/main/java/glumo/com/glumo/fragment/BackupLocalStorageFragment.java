package glumo.com.glumo.fragment;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
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
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import glumo.com.glumo.R;
import glumo.com.glumo.adapter.FileListAdapter;
import glumo.com.glumo.application.GlumoApplication;
import glumo.com.glumo.util.Appearance;

/**
 * This class handles the local storage backup option
 */
public class BackupLocalStorageFragment extends Fragment {

    // view elements
    private View view;
    private Button importData;
    private Button exportData;
    private ListView fileListView;
    private ProgressDialog mProgress;
    private FrameLayout localStorageViewContainer;

    // max depth within which the search is performed in the file system
    public static final int maxDepthSearch = 4;

    // request status code
    public static String localStorageRequestStatus = "null";

    /**
     * This method just sets the options menu
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    /**
     * This method handles the hiding of the options menu
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
        view = inflater.inflate(R.layout.fragment_backup_local_storage, container, false);

        // general title
        Appearance.setTitle(getString(R.string.local_storage_backup_title));
        Appearance.removeActionBarShadow();

        // get reference to the fragment container
        localStorageViewContainer = (FrameLayout) view.findViewById(R.id.local_storage_view_container);

        // color
        int localStorageColor = GlumoApplication.getIntPreference(R.string.theme_color);
        Appearance.setStatusBarColor(localStorageColor);
        Appearance.setActionBarColor(localStorageColor);
        localStorageViewContainer.setBackgroundColor(localStorageColor);

        // progress dialog
        mProgress = new ProgressDialog(getActivity());
        mProgress.setMessage(getResources().getString(R.string.backup_hang_on));

        // buttons export/import
        importData = (Button) view.findViewById(R.id.import_data);
        importData.setTextColor(localStorageColor);
        exportData = (Button) view.findViewById(R.id.export_data);
        exportData.setTextColor(localStorageColor);

        // associating listener to buttons
        importData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doLocalStorageAction("getFilesList");
            }
        });
        exportData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doLocalStorageAction("export");

            }
        });

        return view;
    }

    /**
     * This method is called when the activity is resumed. It handles the choice of the action to be
     * performed on the basis of the status code
     */
    @Override
    public void onResume() {
        super.onResume();
        if (localStorageRequestStatus != "null") {
            doLocalStorageAction(localStorageRequestStatus);
        }
    }


    /**
     * Centralize the import/export/getListOfFiles action
     * @param action : if export file -> "export".
     *                 if get list of files -> "getFilesList".
     *                 Otherwise (import) -> name of the file
     */
    public void doLocalStorageAction (String action) {

        // checking permissions
        if (    ContextCompat.checkSelfPermission(GlumoApplication.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                ||
                ContextCompat.checkSelfPermission(GlumoApplication.getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            localStorageRequestStatus = action;
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE,}, GlumoApplication.PERMISSIONS_REQUEST_CODE_READ_WRITE_EXTERNAL_STORAGE);
        }

        // if permission are granted, perform action on the basis of the parameter string
        else {
            switch (action) {

                // export file
                case "export":{

                    // show progress dialog
                    mProgress.show();

                    // perform export
                    String result = GlumoApplication.db.exportToCsv();

                    // hide progress dialog
                    mProgress.hide();

                    // on the basis of the outcome, notify user
                    Context context = GlumoApplication.getContext();
                    if(result!=null){
                        Toast.makeText(GlumoApplication.getContext(), context.getString(R.string.file_export_success), Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(GlumoApplication.getContext(), context.getString(R.string.file_export_fail), Toast.LENGTH_SHORT).show();
                    }
                    break;
                }

                // get the files list in external storage
                case "getFilesList":
                    refreshFilesList();
                    break;

                // null action
                case "null":
                    break;

                // import file
                default:{

                    // show progress dialog
                    mProgress.show();

                    // perform import
                    boolean result = GlumoApplication.db.importFromCsv(action, false);

                    // hide progress dialog
                    mProgress.hide();

                    // on the basis of the outcome, notify user
                    Context context = GlumoApplication.getContext();
                    if(result){
                        Toast.makeText(GlumoApplication.getContext(), context.getString(R.string.file_import_success), Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(GlumoApplication.getContext(), context.getString(R.string.file_import_fail), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            // set the status as null
            localStorageRequestStatus = "null";
        }
    }

    /**
     * This method, with a recycler view, populates the list with all CSV files from local storage
     */
    private void refreshFilesList () {

        // the local external storage directory
        final String parentDir = Environment.getExternalStorageDirectory().getAbsolutePath();

        // the list of the names of the files that will be displayed
        ArrayList<String> inFiles = getListFiles(new File(parentDir), 0);

        // if no CSV files are found, notify user
        if (inFiles.size() == 0){
            Context context = GlumoApplication.getContext();
            Toast.makeText(context, context.getString(R.string.no_csv_files_found), Toast.LENGTH_SHORT).show();

        // otherwise, go on
        }else{

            // list of files
            fileListView = (ListView) view.findViewById(R.id.local_storage_files_list_view);
            FileListAdapter fileListAdapter = new FileListAdapter(getActivity(), R.layout.file_item_row, inFiles);
            fileListView.setAdapter(fileListAdapter);

            // associating a listener to every item of the list
            fileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                    final String fileName = (String) adapterView.getItemAtPosition(position);

                    // dialog: asking the user to confirm its choice about recoverying his data
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which){

                                // if the user clicks on YES, perform import
                                case DialogInterface.BUTTON_POSITIVE:
                                    doLocalStorageAction(fileName.substring(parentDir.length()+1));
                                    break;

                                // otherwise, go back
                                case DialogInterface.BUTTON_NEGATIVE:
                                    break;
                            }
                        }
                    };

                    // show dialog
                    Context context = GlumoApplication.getContext();
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage(context.getString(R.string.dialog_import_data_question))
                            .setPositiveButton(context.getString(R.string.dialog_import_data_confirm), dialogClickListener)
                            .setNegativeButton(context.getString(R.string.dialog_import_data_decline), dialogClickListener).show();
                }
            });
        }
    }


    /**
     *  A (potentially) recursive function that retrieves the CSV files from the external storage
     *  @param parentDir: the root directory
     *  @param depth: the current directory depth from the root
     *  @return inFiles : the list of CSV files in that directory (and , (potentially) recursively, in its subdirectories)
     */
    private ArrayList<String> getListFiles(File parentDir, int depth) {

        // list of csv files
        ArrayList<String> inFiles = new ArrayList<>();

        // check depth
        if (depth < maxDepthSearch) {
            File[] files = parentDir.listFiles();

            // if csv files are found, add them to list (recursively until max depth is reached)
            if (files != null) {
                if(files.length!=0){
                    for (File file : files) {
                        if (file.isDirectory()) {
                            inFiles.addAll(getListFiles(file, depth + 1));
                        } else {
                            if (file.getName().endsWith(".csv")) {
                                inFiles.add(parentDir + "/" + file.getName());
                            }
                        }
                    }
                }
            }
        }
        return inFiles;
    }
}
