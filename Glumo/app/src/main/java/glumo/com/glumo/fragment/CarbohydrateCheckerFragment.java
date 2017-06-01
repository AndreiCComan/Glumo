package glumo.com.glumo.fragment;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import glumo.com.glumo.R;
import glumo.com.glumo.activity.MainActivity;
import glumo.com.glumo.adapter.FoodItemAdapter;
import glumo.com.glumo.adapter.FoodItemLoader;
import glumo.com.glumo.adapter.FoodItemsInListAdapter;
import glumo.com.glumo.application.GlumoApplication;
import glumo.com.glumo.object.FoodItem;
import glumo.com.glumo.util.Appearance;
import glumo.com.glumo.util.ApplicationUtil;

import static android.app.Activity.RESULT_OK;

/**
 * This class handles the check of the carbohydrates in selected food
 */
public class CarbohydrateCheckerFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<FoodItem>> {

    // view elements
    private View view;
    private LinearLayout searchItemContainer;
    private EditText searchItemEditText;
    private ImageView microphoneImageView;
    private ImageView cancelImageView;
    private ProgressBar loadingIndicator;
    private FoodItemAdapter foodItemAdapter;
    private RecyclerView foodItemRecyclerView;
    private LinearLayout emptyView;
    private LoaderManager loaderManager;
    private SlidingUpPanelLayout slidingUpPanelLayout;
    private ImageView slideArrow;
    private String lastSearchedAliment;
    private TextView numberOfItemsInList;
    private Button goToBolusCalculator;
    private LinearLayout totalCarbohydratesContainer;
    private TextView totalCarbohydratesValue;
    private FoodItemsInListAdapter foodItemsInListAdapter;
    private RecyclerView foodItemsInListRecyclerView;

    // list of foods
    private List<FoodItem> listOfItemsWithinList;

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
     * This method handles the creation of the view for the acvity
     * @param inflater layout inflater
     * @param container viewgroup container
     * @param savedInstanceState previously saved instance
     * @return processed view
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the view
        view = inflater.inflate(R.layout.fragment_carbohydrate_checker, container, false);

        // Set actionbar title
        Appearance.setTitle(getString(R.string.drawer_carbohydrate_checker));

        // Remove actionbar shadow
        Appearance.removeActionBarShadow();

        // Update statusbar and actionbar color
        Appearance.updateActionBarAndStatusBarAndGradientColor();

        // bolus calculator button
        goToBolusCalculator = (Button) view.findViewById(R.id.go_to_bolus_calculator_button);

        // associating listener to bolus calculator button
        goToBolusCalculator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle bundle = new Bundle();
                bundle.putString("totalCarbohydratesValue",totalCarbohydratesValue.getText().toString());
                BolusCalculatorFragment bolusCalculatorFragment = new BolusCalculatorFragment();
                bolusCalculatorFragment.setArguments(bundle);
                getFragmentManager().beginTransaction().replace(R.id.content_frame, bolusCalculatorFragment).commit();
                MainActivity.checkMenuItem(2);
            }
        });

        // progress indicator
        loadingIndicator = (ProgressBar) view.findViewById(R.id.loading_indicator);
        emptyView = (LinearLayout) view.findViewById(R.id.empty_view);
        /*----------------------------------------------------------------------------------------*/
        // SEARCH ITEM SECTION

        // Inflate the searchitem container
        searchItemContainer = (LinearLayout) view.findViewById(R.id.search_item_container);

        // Set background color for the searchitem container
        searchItemContainer.setBackgroundColor(GlumoApplication.getPreferences().getInt(getString(R.string.theme_color), 0));

        // Get referecne to the microphone imageview
        microphoneImageView = (ImageView) view.findViewById(R.id.microphone_image_view);

        // Set click listener to microphone imageview
        microphoneImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptSpeechInput();
            }
        });

        // Get referene to the cancel imageview
        cancelImageView = (ImageView) view.findViewById(R.id.cancel_image_view);
        // Set click listener to cancel imageview
        cancelImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchItemEditText.getText().clear();
            }
        });

        // Get reference to the edittext within searchitem
        searchItemEditText = (EditText) view.findViewById(R.id.search_item_edit_text);

        // Add listener to the edittext within searchitem
        searchItemEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            // handling text visibility
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    cancelImageView.setVisibility(View.GONE);
                    microphoneImageView.setVisibility(View.VISIBLE);
                } else {
                    microphoneImageView.setVisibility(View.GONE);
                    cancelImageView.setVisibility(View.VISIBLE);
                }
            }
        });

        // Set listener on keyboard search button pressed
        searchItemEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    performSearch();
                    Appearance.hideKeyboard(view);
                    return true;
                }
                return false;
            }
        });



        /*----------------------------------------------------------------------------------------*/
        // TOTAL CARBOHYDRATES SECTION

        // view elements
        numberOfItemsInList = (TextView) view.findViewById(R.id.number_of_items);
        totalCarbohydratesContainer = (LinearLayout) view.findViewById(R.id.total_carbohydrates_container);
        totalCarbohydratesValue = (TextView) view.findViewById(R.id.total_carbohydrates_value);

        slideArrow = (ImageView) view.findViewById(R.id.slide_arrow);
        slidingUpPanelLayout = (SlidingUpPanelLayout) view.findViewById(R.id.sliding_layout);
        slidingUpPanelLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {

            /**
             * This method sets the rotation of the arrow
             * @param panel view of the panel
             * @param slideOffset offset for the transition
             */
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                slideArrow.setRotation(180 * slideOffset);
            }


            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
            }
        });

        /*----------------------------------------------------------------------------------------*/
        // RECYCLER VIEWS SECTION

        /*--second-recyclerview--*/
        foodItemsInListRecyclerView = (RecyclerView) view.findViewById(R.id.food_items_in_list_recycler_view);
        foodItemsInListRecyclerView.setHasFixedSize(true);
        foodItemsInListAdapter = new FoodItemsInListAdapter(numberOfItemsInList,
                totalCarbohydratesContainer,
                totalCarbohydratesValue);
        foodItemsInListRecyclerView.setAdapter(foodItemsInListAdapter);

        /*--first-recyclerview--*/
        foodItemRecyclerView = (RecyclerView) view.findViewById(R.id.food_item_recycler_view);
        foodItemRecyclerView.setHasFixedSize(true);
        foodItemAdapter = new FoodItemAdapter(numberOfItemsInList,
                totalCarbohydratesContainer,
                totalCarbohydratesValue,
                foodItemsInListAdapter);
        foodItemRecyclerView.setAdapter(foodItemAdapter);

        /*----------------------------------------------------------------------------------------*/
        // LOADER SECTION

        // Get a reference to the LoaderManager, in order to interact with loaders.
        loaderManager = getLoaderManager();

        if (getLoaderManager().getLoader(GlumoApplication.FOOD_ITEM_LOADER_ID) != null) {
            loaderManager.initLoader(GlumoApplication.FOOD_ITEM_LOADER_ID, null, this);
        }

        return view;
    }

    /*--------------------------------------------------------------------------------------------*/
    // SPEECH SECTION

    /**
     * This method handles the speech input for the food search
     */
    private void promptSpeechInput() {

        // handling intent
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_prompt));
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 10);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 20);

        // attempting to perform the search, if failed notify user
        try {
            startActivityForResult(intent, GlumoApplication.REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(GlumoApplication.getContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * This method handles the conclusion of the task, on the basis of the outcome of the
     * operations performed
     * @param requestCode request code
     * @param resultCode result code
     * @param data data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // on the basis of the request code, choose what to do
        switch (requestCode) {
            case GlumoApplication.REQ_CODE_SPEECH_INPUT: {

                // if operation was successful, perform search
                if (resultCode == RESULT_OK && data != null) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    String searchText = result.get(0);
                    searchItemEditText.setText(searchText);
                    searchItemEditText.setSelection(searchText.length());
                    performSearch();
                }
            }
            break;
        }
    }

    /*--------------------------------------------------------------------------------------------*/
    // LOADER SECTION

    /**
     * This method performs the food search
     */
    private void performSearch() {
        if (ApplicationUtil.isDeviceOnline()) {

            // getting element to be searched
            String alimentToSearch = searchItemEditText.getText().toString();

            // if the user is performing a new search
            if (!alimentToSearch.equals(lastSearchedAliment)) {

                // show loading indicator
                loadingIndicator.setVisibility(View.VISIBLE);

                // set visibility of items
                foodItemRecyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);

                // last searched aliment
                lastSearchedAliment = alimentToSearch;

                // Destroy the loader
                getLoaderManager().destroyLoader(GlumoApplication.FOOD_ITEM_LOADER_ID);

                // Initialize the loader
                loaderManager.initLoader(GlumoApplication.FOOD_ITEM_LOADER_ID, null, this);
            }

        // otherwise notify user
        } else {
            // First, hide loading indicator so error message will be visible
            View loadingIndicator = view.findViewById(R.id.loading_indicator);
            loadingIndicator.setVisibility(View.GONE);
            Toast.makeText(GlumoApplication.getContext(),
                    getString(R.string.no_internet_connection),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * This method loads the food item
     * @param i
     * @param bundle
     * @return food item loader
     */
    @Override
    public Loader<List<FoodItem>> onCreateLoader(int i, Bundle bundle) {
        return new FoodItemLoader(GlumoApplication.getContext(), searchItemEditText.getText().toString());
    }

    /**
     * This method handles the conclusion of the loading
     * @param loader loader
     * @param foodItems list of food items
     */
    @Override
    public void onLoadFinished(Loader<List<FoodItem>> loader, List<FoodItem> foodItems) {

        // hide the loading indicator
        loadingIndicator.setVisibility(View.GONE);

        // if foods are found, show them
        if (foodItems != null && !foodItems.isEmpty()) {
            emptyView.setVisibility(View.GONE);
            foodItemRecyclerView.setVisibility(View.VISIBLE);
            foodItemAdapter.setFoodItems(foodItems);

        // otherwise, notify user with error
        } else {
            emptyView.setVisibility(View.VISIBLE);
            foodItemRecyclerView.setVisibility(View.GONE);
            showErrorMessage();
        }
    }

    /**
     * This method just clears the loader
     * @param loader
     */
    @Override
    public void onLoaderReset(Loader<List<FoodItem>> loader) {
    }


    /**
     * This method just display an error message if no food is found
     */
    private void showErrorMessage() {
        Toast.makeText(GlumoApplication.getContext(),
                getString(R.string.no_search_results),
                Toast.LENGTH_SHORT).show();
    }

    /*--------------------------------------------------------------------------------------------*/
    // SAVE STATE SECTION

    /**
     * This method just keeps track of the current state of the task
     * @param outState
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Save the state of the slideArrow
        outState.putFloat("slideArrowState", slideArrow.getRotation());
        // Save the state of the loadingIndicator
        outState.putInt("loadingIndicatorState",
                loadingIndicator.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);
        // Save the state of the totalCarbohydratesContainer
        outState.putInt("totalCarbohydratesContainerState",
                totalCarbohydratesContainer.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);
        // Save the state of numberOfItemsInList
        outState.putString("numberOfItemsInListState", numberOfItemsInList.getText().toString());
        // Save the state of totalCarbohydratesValue
        outState.putString("totalCarbohydratesValueState", totalCarbohydratesValue.getText().toString());
        // Save the list
        outState.putParcelableArrayList("listOfItemsWithinList", (ArrayList) listOfItemsWithinList);
        // Save the state of the items within the list
        if (foodItemsInListAdapter != null) {
            foodItemsInListAdapter.saveStates(outState);
        }
        super.onSaveInstanceState(outState);
    }

    /**
     * This method chooses what to do on the basis of the state saved
     * @param savedInstanceState previously saved instance
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if(savedInstanceState != null){
            // Get the state of the listOfItemsWithinList
            if(savedInstanceState.containsKey("listOfItemsWithinList")){
                listOfItemsWithinList = savedInstanceState.getParcelableArrayList("listOfItemsWithinList");
            }
            // Get the state of slideArrow
            if(savedInstanceState.containsKey("slideArrowState")){
                slideArrow.setRotation(savedInstanceState.getFloat("slideArrowState"));
            }
            // Get the state of the loadingIndicator
            if(savedInstanceState.containsKey("loadingIndicatorState")){
                if(savedInstanceState.getInt("loadingIndicatorState") == View.VISIBLE)
                    loadingIndicator.setVisibility(View.VISIBLE);
                else
                    loadingIndicator.setVisibility(View.GONE);
            }
            // Get the state of the totalCarbohydratesContainer
            if(savedInstanceState.containsKey("totalCarbohydratesContainerState")){
                if(savedInstanceState.getInt("totalCarbohydratesContainerState") == View.VISIBLE)
                    totalCarbohydratesContainer.setVisibility(View.VISIBLE);
                else
                    totalCarbohydratesContainer.setVisibility(View.GONE);
            }
            // Get the state of numberOfItemsInList
            if(savedInstanceState.containsKey("numberOfItemsInListState")){
                numberOfItemsInList.setText(savedInstanceState.getString("numberOfItemsInListState"));
            }
            if(savedInstanceState.containsKey("totalCarbohydratesValueState")){
                totalCarbohydratesValue.setText(savedInstanceState.getString("totalCarbohydratesValueState"));
            }
        }else{
            listOfItemsWithinList = new ArrayList<FoodItem>();
        }

        if(foodItemAdapter != null){
            foodItemAdapter.setFoodItemsInList(listOfItemsWithinList);
        }
        if (foodItemsInListAdapter != null) {
            foodItemsInListAdapter.restoreStates(savedInstanceState);
            foodItemsInListAdapter.setFoodItems(listOfItemsWithinList);
        }
        super.onActivityCreated(savedInstanceState);
    }
}