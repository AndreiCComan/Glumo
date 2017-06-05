package glumo.com.glumo.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.CardView;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import glumo.com.glumo.R;
import glumo.com.glumo.activity.MainActivity;
import glumo.com.glumo.application.GlumoApplication;
import glumo.com.glumo.object.GlucoseRead;
import glumo.com.glumo.object.UtilityCardView;
import glumo.com.glumo.util.Appearance;
import glumo.com.glumo.util.BluetoothUtil;
import glumo.com.glumo.util.DBManager;

public class HomeFragment extends Fragment {
    // constants
    final static int ANIMATION_FAST = 500;
    final static int ANIMATION_MEDIUM = 1000;
    final static int ANIMATION_SLOW = 2000;

    // view elements
    private View view;

    private TextView averageGlucoseLevel;
    private TextView minimumGlucoseLevel;
    private TextView maximumGlucoseLevel;

    private UtilityCardView[] cardViews = new UtilityCardView[4];
    private int cardAndPaddingHeight = 0;
    private LinearLayout cardViewWrapper;

    private PieChart pieChart;
    private ScrollView scrollView;
    private FrameLayout frameLayout;

    private Context ctx = GlumoApplication.getContext();

    private ValueAnimator animation = ValueAnimator.ofFloat(0, 1);

    private int themeColor;


    // cached data elements
    private JSONObject cachedData = null;
    private boolean areWeUsingCachedData = false;
    private final String cachedDataKey = "cachedDataKey";
    private final String cachedDataArrayListKey = "cachedDataArrayListKey";

    private boolean paused = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // inflate the view
        view = inflater.inflate(R.layout.fragment_home, container, false);

        // handling appearance
        Appearance.setTitle(getString(R.string.drawer_home));
        Appearance.removeActionBarShadow();

        // setting theme color
        themeColor = GlumoApplication.getIntPreference(R.string.theme_color);

        // getting the elements that will be rendered with proper values retrieved from the DB
        averageGlucoseLevel = (TextView) view.findViewById(R.id.average_value);
        minimumGlucoseLevel = (TextView) view.findViewById(R.id.minimum_value);
        maximumGlucoseLevel = (TextView) view.findViewById(R.id.maximum_value);

        // cardviews displaying glucose values
        cardViews[0] = new UtilityCardView(
                null,
                ((LinearLayout) view.findViewById(R.id.last_glucose_value_container)),
                ((TextView) view.findViewById(R.id.last_glucose_value)),
                ((ImageView) view.findViewById(R.id.last_glucose_value_arrow)),
                null,
                0);

        cardViews[1] = new UtilityCardView(
                ((CardView) (view.findViewById(R.id.second_last_glucose_value_container)).getParent()),
                ((LinearLayout) view.findViewById(R.id.second_last_glucose_value_container)),
                ((TextView) view.findViewById(R.id.second_last_glucose_value)),
                ((ImageView) view.findViewById(R.id.second_last_glucose_value_arrow)),
                ((TextView) view.findViewById(R.id.second_last_glucose_value_time)),
                0);

        cardViews[2] = new UtilityCardView(
                ((CardView) (view.findViewById(R.id.third_last_glucose_value_container)).getParent()),
                ((LinearLayout) view.findViewById(R.id.third_last_glucose_value_container)),
                ((TextView) view.findViewById(R.id.third_last_glucose_value)),
                ((ImageView) view.findViewById(R.id.third_last_glucose_value_arrow)),
                ((TextView) view.findViewById(R.id.third_last_glucose_value_time)),
                0);

        cardViews[3] = new UtilityCardView(
                ((CardView) (view.findViewById(R.id.fourth_last_glucose_value_container)).getParent()),
                ((LinearLayout) view.findViewById(R.id.fourth_last_glucose_value_container)),
                ((TextView) view.findViewById(R.id.fourth_last_glucose_value)),
                ((ImageView) view.findViewById(R.id.fourth_last_glucose_value_arrow)),
                ((TextView) view.findViewById(R.id.fourth_last_glucose_value_time)),
                0);

        // get reference to the root layout. This is mandatory for toolbar elevation effect
        frameLayout = (FrameLayout) view.findViewById(R.id.home_root_frame_layout);

        // get reference to the child layout. This is mandatory for toolbar elevation effect
        scrollView = (ScrollView) view.findViewById(R.id.home_scroll_view);

        // scroll listener
        scrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                if (MainActivity.currentFragmentClass.equals(HomeFragment.class.toString())) {
                    // get the difference
                    if (scrollView.getScrollY() == frameLayout.getScrollY()) {
                        Appearance.removeActionBarShadow();
                    } else {
                        Appearance.addActionBarShadow();
                    }
                }
            }
        });

        // home scroll view
        ViewTreeObserver vto = view.findViewById(R.id.home_scroll_view).getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                view.findViewById(R.id.home_scroll_view).getViewTreeObserver().removeOnGlobalLayoutListener(this);
                cardViewWrapper = (LinearLayout) view.findViewById(R.id.glucose_value_cards_wrapper);
                int cardHeight = cardViews[1].getGlucoseValueLayoutCardView().getMeasuredHeight();
                int wrapperHeight = cardViewWrapper.getMeasuredHeight();
                int cardPadding = (wrapperHeight - (3 * cardHeight)) / 3;
                UtilityCardView.setCardHeight(cardHeight);
                UtilityCardView.setCardPadding(cardPadding);
                cardAndPaddingHeight = cardHeight + cardPadding;

                // update the layout
                updateLayoutStaticValues();
            }
        });

        // associate the intent to the broadcast receiver for receiving the glucose read values
        IntentFilter filters = new IntentFilter(GlumoApplication.BT_READ_GLUCOSE_R);
        // register it
        LocalBroadcastManager.getInstance(ctx).registerReceiver(localGlucoseReadIntentReceiver, filters);
        // the fragment is not paused

        return view;
    }


    @Override
    public void onResume() {
        super.onResume();
        paused = false;
    }


    @Override
    public void onPause() {
        // end the animation (if it was running)
        if (animation.isRunning())
            animation.end();
        // the fragment is paused
        paused = true;
        super.onPause();
    }

    // this method handles the saving of cached data
    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Save the json cached data (if any)
        if (cachedData != null) {
            try {
                // saving the List<GlucoseRead>
                outState.putParcelableArrayList(cachedDataArrayListKey, (ArrayList<GlucoseRead>) cachedData.get("list"));
                // save the json and its values
                outState.putString(cachedDataKey, cachedData.toString());
            } catch (JSONException e) {
                handleException(e, "HOME_FRAGMENT", "onSaveInstanceState - format error in saved JSONObject cachedData - get(\"list\")");
                cachedData = null;
            }
        }
        super.onSaveInstanceState(outState);
    }

    // this method retrieves the cached data
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Get the the json cached data (if any)
            if (savedInstanceState.containsKey(cachedDataKey)) {
                try {
                    // getting the cached data JSONObject
                    cachedData = new JSONObject(savedInstanceState.getString(cachedDataKey));
                    // getting also the list
                    cachedData.put("list", savedInstanceState.getParcelableArrayList(cachedDataArrayListKey));
                } catch (JSONException e) {
                    handleException(e, "HOME_FRAGMENT", "onActivityCreated - format error in saved JSONObject");
                    cachedData = null;
                }
            }
        }
        super.onActivityCreated(savedInstanceState);
    }


    //  This method updates all the interface after a glucose read (i.e.: last value, average-min-max, graphs, ...)
    public void updateLayoutStaticValues() {
        boolean energySaveMode = (GlumoApplication.getBooleanPreference(R.string.energy_save_mode_switch_preference));
        JSONObject json;

        // if there are cached data, put them in the json object, otherwise make a new read
        if (cachedData != null) {
            json = cachedData;
            areWeUsingCachedData = true;
        } else {
            json = GlumoApplication.db.getRangeOfGlucoseReads(1, true);
            cachedData = json;
            areWeUsingCachedData = false;
        }

        // values that have been set for hypoglycemia and hyperglicemia
        int[] thresholds = {GlumoApplication.getIntPreference(R.string.hypoglycemia_preference), GlumoApplication.getIntPreference(R.string.hyperglycemia_preference)};

        // if the json file contains data, extract the list of glucose reads
        if (json != null) {
            try {
                List<GlucoseRead> list = (ArrayList<GlucoseRead>) json.get("list");
                int list_size = list.size();
                final GlucoseRead[] glucoseReads = new GlucoseRead[5];
                final int[] startColors = new int[4];
                final int[] endColors = new int[4];

                // if the energy save mode is off, swith cards
                if (!energySaveMode) {
                    UtilityCardView temp = cardViews[1];
                    cardViews[1] = cardViews[3];
                    cardViews[3] = cardViews[2];
                    cardViews[2] = temp;
                }

                // get first 5 reads, empty reads if there are less than 5
                for (int i = 0; i < 5; i++) {
                    glucoseReads[i] = (list_size > i ? list.get(i) : new GlucoseRead("0000-00-00 00:00:00", 0));
                }

                // handle arrow symbol: upwards if the values is incrementing, downwards if it's decrementing, horyzontal otherwise
                for (int i = 0; i < 4; i++) {

                    ColorDrawable tempColor = ((ColorDrawable) (cardViews[i].getGlucoseValueLayout().getBackground()));
                    if (tempColor == null || tempColor.getColor() == 0) {
                        startColors[i] = themeColor;
                    } else {
                        startColors[i] = tempColor.getColor();
                    }
                    endColors[i] = ContextCompat.getColor(ctx, Appearance.getColorBasedOnThresholds(glucoseReads[i].getGlucose(), thresholds));

                    if (glucoseReads[i].getGlucose() > glucoseReads[i + 1].getGlucose())
                        cardViews[i].getGlucoseValueArrow().setRotation(-45);
                    else if (glucoseReads[i].getGlucose() == glucoseReads[i + 1].getGlucose())
                        cardViews[i].getGlucoseValueArrow().setRotation(0);
                    else
                        cardViews[i].getGlucoseValueArrow().setRotation(45);

                    if (!energySaveMode)
                        Appearance.countAnimation(Integer.valueOf(cardViews[i].getGlucoseTextViewValue().getText().toString()), glucoseReads[i].getGlucose(), cardViews[i].getGlucoseTextViewValue(), ANIMATION_MEDIUM);
                    else
                        cardViews[i].getGlucoseTextViewValue().setText(String.valueOf(glucoseReads[i].getGlucose()));

                    if (i != 0) {
                        cardViews[i].getGlucoseValueTime().setText(glucoseReads[i].getTime().substring(11));
                        cardViews[i].setActualGlucoseLayoutTranslation((int) cardViews[i].getGlucoseValueLayoutCardView().getTranslationY());
                    }
                }

                // if energy save mode is off, add animation
                if (!energySaveMode) {
                    animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator valueAnimator) {
                            int[] intermediateColors = new int[4];
                            // get the current color position
                            float position = valueAnimator.getAnimatedFraction();

                            for (int i = 0; i < 4; i++) {
                                // get the color given position
                                intermediateColors[i] = Appearance.blendColors(startColors[i], endColors[i], position);
                                cardViews[i].getGlucoseValueLayout().setBackgroundColor(intermediateColors[i]);
                                if (i == 0) {
                                    Appearance.setActionBarColor(intermediateColors[i]);
                                    Appearance.setStatusBarColor(intermediateColors[i]);
                                } else {
                                    if (i != 1) {
                                        cardViews[i].getGlucoseValueLayoutCardView().setTranslationY(cardViews[i].getActualGlucoseLayoutTranslation() + (position * cardAndPaddingHeight));
                                    } else {
                                        cardViews[i].getGlucoseValueLayoutCardView().setTranslationY(cardViews[i].getActualGlucoseLayoutTranslation() - (position * 2 * cardAndPaddingHeight));

                                    }
                                }
                            }
                        }
                    });

                    // updating the card objects with the values associated to the current graphic situation
                    animation.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            cardViews[1].addActualGlucoseLayoutTranslation((-2 * (cardAndPaddingHeight)));
                            cardViews[2].addActualGlucoseLayoutTranslation(cardAndPaddingHeight);
                            cardViews[3].addActualGlucoseLayoutTranslation(cardAndPaddingHeight);
                            animation.removeAllListeners();
                        }
                    });

                    // start the animation if not currently going
                    if (!animation.isRunning())
                        animation.setDuration(ANIMATION_MEDIUM).start();
                }

                // if save energy mode is on, simply display the cards (no animations)
                else {
                    Appearance.setActionBarColor(endColors[0]);
                    Appearance.setStatusBarColor(endColors[0]);
                    for (int i = 0; i < 4; i++) {
                        cardViews[i].getGlucoseValueLayout().setBackgroundColor(endColors[i]);
                    }
                }

                if (list_size > 0) {

                    // set text for maximum, minimum and average glucose values
                    averageGlucoseLevel.setText(String.valueOf(json.getInt("average")));
                    minimumGlucoseLevel.setText(String.valueOf(json.getInt("min")));
                    maximumGlucoseLevel.setText(String.valueOf(json.getInt("max")));

                    // inserting pie chart with normal hits, danger hits, warning hits, hypoglycemic hits, hyperglicemyc hits
                    insertPieChart(
                            (Integer) json.get("normalHits"),
                            (Integer) json.get("dangerHits"),
                            (Integer) json.get("warningHits"),
                            (Integer) json.get("hypoHits"),
                            (Integer) json.get("hyperHits")
                    );
                }

            } catch (JSONException e) {
                handleException(e, "HOME_FRAGMENT", "updateLayoutStaticValues - format error in JSONObject from DB while working on it");
            }
        }

        // if there are no data, display a message
        else {
            Appearance.setActionBarColor(themeColor);
            Appearance.setStatusBarColor(themeColor);
            String noData = getResources().getString(R.string.no_data);
            cardViews[0].getGlucoseTextViewValue().setText(noData);
        }
    }

    /**
     * this method inserts a pie charts containing the values set as parameters
     *
     * @param normalValue  normal value hits
     * @param dangerValue  danger value hits
     * @param warningValue warning value hits
     * @param hypoValue    hypo value hits
     * @param hyperValue   hyper value hits
     */
    private void insertPieChart(int normalValue, int dangerValue, int warningValue, int hypoValue, int hyperValue) {
        pieChart = (PieChart) view.findViewById(R.id.pie_chart);

        // istantiating entries
        PieEntry normalEntry = new PieEntry(normalValue, (normalValue != 0 ? "normal" : ""));
        PieEntry dangerEntry = new PieEntry(dangerValue, (dangerValue != 0 ? "danger" : ""));
        PieEntry warningEntry = new PieEntry(warningValue, (warningValue != 0 ? "warning" : ""));
        PieEntry hypoEntry = new PieEntry(hypoValue, (hypoValue != 0 ? "hypo" : ""));
        PieEntry hyperEntry = new PieEntry(hyperValue, (hyperValue != 0 ? "hyper" : ""));

        // adding entries to list
        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(normalEntry);
        entries.add(dangerEntry);
        entries.add(warningEntry);
        entries.add(hypoEntry);
        entries.add(hyperEntry);

        // istantiating data set for the chart
        PieDataSet dataSet = new PieDataSet(entries, "");

        // setting colors for the chart
        dataSet.setColors(ContextCompat.getColor(getActivity(), R.color.TURQUOISE),
                ContextCompat.getColor(getActivity(), R.color.CARROT),
                ContextCompat.getColor(getActivity(), R.color.ORANGE),
                ContextCompat.getColor(getActivity(), R.color.ALIZARIN),
                ContextCompat.getColor(getActivity(), R.color.POMEGRANATE));
        PieData data = new PieData(dataSet);

        // graphical settings for the text of the value
        data.setValueTextColor(ContextCompat.getColor(getActivity(), R.color.white));
        data.setValueTextSize(25f);
        data.setValueFormatter(new IValueFormatter() {
            @Override
            public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
                if (value != 0)
                    return "" + ((int) value);
                else
                    return "";
            }
        });

        dataSet.setSliceSpace(2);

        // title of the chart
        SpannableString s = new SpannableString("Blood sugar\nlevel hits");
        s.setSpan(new RelativeSizeSpan(1.4f), 0, 22, 0);
        s.setSpan(new StyleSpan(Typeface.NORMAL), 0, 22, 0);
        s.setSpan(new ForegroundColorSpan(ContextCompat.getColor(ctx, R.color.WET_ASPHALT)), 0, 22, 0);
        pieChart.setCenterText(s);

        // graphical features of the chart
        pieChart.getLegend().setEnabled(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.invalidate();
        pieChart.setHoleRadius(45f);
        pieChart.setTransparentCircleAlpha(0);
        pieChart.setHighlightPerTapEnabled(false);
        //pieChart.setMaxAngle(180f); // HALF CHART
        pieChart.setRotationAngle(180f);
        pieChart.setData(data);

        // if energy save mode is on OR if we are using cached data ( -> screen already on), just display data
        if ((GlumoApplication.getBooleanPreference(R.string.energy_save_mode_switch_preference)) || areWeUsingCachedData)
            pieChart.animate();
        else
            pieChart.animateY(1000, Easing.EasingOption.EaseInOutQuad);
    }


    // This class is a listener, which is invoked every time a read is performed
    private BroadcastReceiver localGlucoseReadIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            // incapsulates the action of the intent
            String action = intent.getAction();

            // if action is a read of the glucose value,
            if (action.equals(GlumoApplication.BT_READ_GLUCOSE_R)) {
                // set the glucose read request status to false
                MainActivity.setIsGlucoseReadRequestSent(false);
                // hide the loading wheel
                MainActivity.hideWheel();
                // if we already have some data to work with, just update 'em
                if (cachedData != null) {
                    try {

                        // updating minimum, maximum and average values
                        int value = Integer.valueOf(intent.getStringExtra(GlumoApplication.ET_GLUCOMETER_RESPONSE_TAG));
                        if (cachedData.getInt("max") < value)           // updating max value
                            cachedData.put("max", value);
                        if (cachedData.getInt("min") > value)           // updating min value
                            cachedData.put("min", value);
                        int oldAverage = cachedData.getInt("average");  // updating average and cont
                        int oldCont = cachedData.getInt("cont");
                        cachedData.put("average", ((oldAverage * oldCont) + (value)) / (oldCont + 1));
                        cachedData.put("cont", oldCont + 1);

                        // computing danger level on the basis of the value that has been read
                        String readDangerLevel = DBManager.getGlucoseDangerLevel(value);

                        // updating hits (normal, warning, ...)
                        int oldHitsValue;

                        // on the basis of the danger level update cached data
                        switch (readDangerLevel) {
                            case "hyperglycemia":
                                oldHitsValue = cachedData.getInt("hyperHits");
                                cachedData.put("hyperHits", oldHitsValue + 1);
                                break;
                            case "hypoglycemia":
                                oldHitsValue = cachedData.getInt("hypoHits");
                                cachedData.put("hypoHits", oldHitsValue + 1);
                                break;
                            case "danger":
                                oldHitsValue = cachedData.getInt("dangerHits");
                                cachedData.put("dangerHits", oldHitsValue + 1);
                                break;
                            case "warning":
                                oldHitsValue = cachedData.getInt("warningHits");
                                cachedData.put("warningHits", oldHitsValue + 1);
                                break;
                            case "normal":
                                oldHitsValue = cachedData.getInt("normalHits");
                                cachedData.put("normalHits", oldHitsValue + 1);
                                break;
                        }

                        // finally, update the list
                        List<GlucoseRead> oldList = (List<GlucoseRead>) cachedData.get("list");
                        oldList.add(0, new GlucoseRead(DBManager.getCurrentTimestamp(), value));
                        cachedData.put("list", oldList);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        handleException(e, "HOME_FRAGMENT", "localGlucoseReadIntentReceiver - format error in saved JSONObject");
                        cachedData = null;
                    }
                }
                if (!paused) {
                    updateLayoutStaticValues();
                }
            }
        }
    };


    // exception handler
    private void handleException(Exception e, String tag, String message) {
        e.printStackTrace();
        Log.e(tag, message);
    }
}