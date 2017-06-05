package glumo.com.glumo.fragment;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.DefaultValueFormatter;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IFillFormatter;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ViewPortHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import glumo.com.glumo.R;
import glumo.com.glumo.adapter.GlucoseReadAdapter;
import glumo.com.glumo.application.GlumoApplication;
import glumo.com.glumo.object.GlucoseRead;
import glumo.com.glumo.util.Appearance;
import glumo.com.glumo.util.DBManager;

/**
 * This class handles the view of analysis data in the activity
 */
public class AnalysisFragment extends Fragment {

    // buttons layout
    private LinearLayout analysisPeriodSelectionContainer;
    private Button todayAnalysisButton;
    private Button weekAnalysisButton;
    private Button monthAnalysisButton;

    // functionality strings
    private final String selectedButtonKey = "selectedButton";
    private final String todayString = "today";
    private final String weekString = "week";
    private final String monthString = "month";
    private String selectedButton = "";
    private String oldAxisValue = "";

    // textviews
    private TextView averageGlucoseLevel;
    private TextView minimumGlucoseLevel;
    private TextView maximumGlucoseLevel;
    private TextView fromDateTextView;
    private TextView toDateTextView;
    private TextView hypoglycemiaHitsTextViex;
    private TextView hyperglycemiaHitsTextViex;

    // chart view
    private View view;
    private int analysisPeriodSelectionContainerBackgroundColor;
    private LineChart lineChart;
    private PieChart pieChart;
    private Context ctx = GlumoApplication.getContext();

    // axis values
    private boolean firstTwoDateInGraphAxis = false;
    private int dateAxisCutLevel = -1;

    // adapter and recycler view
    private GlucoseReadAdapter glucoseReadAdapter;
    private RecyclerView glucoseReadRecyclerView;

    /**
     * This method just enables options menu
     * @param savedInstanceState previously saved instance
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    /**
     * This method just handles the hiding of the option menu
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
     * This method handles the creation of the graphical view of the activity
     * @param inflater  layout inflater
     * @param container viewgroup container
     * @param savedInstanceState previously saved instance state
     * @return the processed view
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // inflate the view
        view = inflater.inflate(R.layout.fragment_analysis, container, false);

        Appearance.setTitle(getString(R.string.drawer_analysis));
        Appearance.updateActionBarAndStatusBarAndGradientColor();

        // getting the elements that will be rendered with proper values retrieved from the DB
        averageGlucoseLevel = (TextView) view.findViewById(R.id.average_value);
        minimumGlucoseLevel = (TextView) view.findViewById(R.id.minimum_value);
        maximumGlucoseLevel = (TextView) view.findViewById(R.id.maximum_value);
        fromDateTextView = (TextView) view.findViewById(R.id.date_from);
        toDateTextView = (TextView) view.findViewById(R.id.date_to);
        hypoglycemiaHitsTextViex = (TextView) view.findViewById(R.id.times_in_hypoglycemia);
        hyperglycemiaHitsTextViex = (TextView) view.findViewById(R.id.times_in_hyperglycemia);

        // buttons
        analysisPeriodSelectionContainer = (LinearLayout) view.findViewById(R.id.analysis_period_selection_container);
        todayAnalysisButton = (Button) view.findViewById(R.id.analysis_today_button);
        weekAnalysisButton = (Button) view.findViewById(R.id.analysis_week_button);
        monthAnalysisButton = (Button) view.findViewById(R.id.analysis_month_button);

        // graphical features for the data to be displayed
        analysisPeriodSelectionContainerBackgroundColor = GlumoApplication.getIntPreference(R.string.theme_color);
        Drawable analysisPeriodSelectionContainerBackground = analysisPeriodSelectionContainer.getBackground();
        GradientDrawable gradientDrawable = (GradientDrawable) analysisPeriodSelectionContainerBackground;
        gradientDrawable.setColor(analysisPeriodSelectionContainerBackgroundColor);

        selectButton(todayAnalysisButton);

        // associating listener to button, in order to generate the graph of the analysis of the current day
        todayAnalysisButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedButton = todayString;
                selectButton(todayAnalysisButton);
                deselectButton(weekAnalysisButton);
                deselectButton(monthAnalysisButton);
                createGraphs();
            }
        });

        // associating listener to button, in order to generate the graph of the analysis of the current week
        weekAnalysisButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedButton = weekString;
                selectButton(weekAnalysisButton);
                deselectButton(todayAnalysisButton);
                deselectButton(monthAnalysisButton);
                createGraphs();
            }
        });

        // associating listener to button, in order to generate the graph of the analysis of the current month
        monthAnalysisButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedButton = monthString;
                selectButton(monthAnalysisButton);
                deselectButton(todayAnalysisButton);
                deselectButton(weekAnalysisButton);
                createGraphs();
            }
        });

        // instantiating the adapter and getting the recycler view
        glucoseReadAdapter = new GlucoseReadAdapter();
        glucoseReadRecyclerView = (RecyclerView) view.findViewById(R.id.glucose_read_analysys_recycler_view);
        glucoseReadRecyclerView.setHasFixedSize(true);
        glucoseReadRecyclerView.setAdapter(glucoseReadAdapter);

        return view;
    }

    /**
     * This method handles the graphical features for the button given as parameter, in order to display it as selected
     * @param button button to be process
     */
    private void selectButton(Button button){
        button.setBackground(ContextCompat.getDrawable(GlumoApplication.getContext(), R.drawable.analysis_period_selection_button_active_background));
        button.setTextColor(analysisPeriodSelectionContainerBackgroundColor);
    }

    /**
     * This method handles the graphical features for the button given as parameter, in order to display it as deselected
     * @param button
     */
    private void deselectButton(Button button){
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setTextColor(Color.WHITE);
    }

    /**
     * This method just keeps track of the selected button in order to keep it selected when the orientation of the screen
     * is changed
     * @param outState
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(selectedButtonKey, selectedButton);
        super.onSaveInstanceState(outState);
    }

    /**
     * This method handles the selection of a button on the basis of a previous instance
     * @param savedInstanceState
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // if there is a previous instance, retrieve the selection
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(selectedButtonKey))
                selectedButton = savedInstanceState.getString(selectedButtonKey);
        }

        // otherwise select current day button
        else
            selectedButton = todayString;

        // generate graph
        createGraphs();
    }

    /**
     * This method handles the creation of the analysis graph
     */
    private void createGraphs() {

        // list of reads
        List<GlucoseRead> list;

        // json objects to contains data from a range of dates
        JSONObject json;

        // keeps track of the range of dates
        int howManyDaysAgo = 1;

        // on the basis of the selected button, display different data (day, week or month)
        switch (selectedButton) {
            case todayString :
                selectButton(todayAnalysisButton);
                deselectButton(monthAnalysisButton);
                deselectButton(weekAnalysisButton);
                howManyDaysAgo = 1;
                break;

            case weekString :
                selectButton(weekAnalysisButton);
                deselectButton(todayAnalysisButton);
                deselectButton(monthAnalysisButton);
                howManyDaysAgo = 7;
                break;

            case monthString :
                selectButton(monthAnalysisButton);
                deselectButton(todayAnalysisButton);
                deselectButton(weekAnalysisButton);
                howManyDaysAgo = 30;
                break;
        }

        // filling json objects with the dates belonging to the range
        json = GlumoApplication.db.getRangeOfGlucoseReads(howManyDaysAgo, true);

        // if the json contains data, generate graph
        if (json != null) {
            try {

                // get list of values
                list = (List<GlucoseRead>) json.get("list");
                int list_size = list.size();

                // if list contains values, set graphical features on the basis of them
                if (list_size != 0) {
                    fromDateTextView.setText(list.get(list_size-1).getTime());
                    toDateTextView.setText(list.get(0).getTime());

                    averageGlucoseLevel.setText(String.valueOf(json.getInt("average")));
                    minimumGlucoseLevel.setText(String.valueOf(json.getInt("min")));
                    maximumGlucoseLevel.setText(String.valueOf(json.getInt("max")));

                    hypoglycemiaHitsTextViex.setText(String.valueOf(json.getInt("hypoHits")));
                    hyperglycemiaHitsTextViex.setText(String.valueOf(json.getInt("hyperHits")));

                    // inserting pie chart (normal hits, danger hits, warning hits, hypo hits, hyper hits)
                    insertPieChart(
                            (Integer) json.get("normalHits"),
                            (Integer) json.get("dangerHits"),
                            (Integer) json.get("warningHits"),
                            (Integer) json.get("hypoHits"),
                            (Integer) json.get("hyperHits")
                    );

                    // inserting line chart
                    insertLineChart(list);

                    // adding the log to the recycler view
                    glucoseReadAdapter.setGlucoseReads(list);

                }

                else {
                    fromDateTextView.setText("");
                    toDateTextView.setText("");

                    averageGlucoseLevel.setText("");
                    minimumGlucoseLevel.setText("");
                    maximumGlucoseLevel.setText("");

                    hypoglycemiaHitsTextViex.setText("");
                    hyperglycemiaHitsTextViex.setText("");

                    if (pieChart != null)
                        pieChart.setVisibility(View.INVISIBLE);
                    if (lineChart != null)
                        lineChart.setVisibility(View.INVISIBLE);

                    glucoseReadAdapter.setGlucoseReads(new ArrayList<GlucoseRead>());
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // otherwise, set empty texts for the dates
        else {
            fromDateTextView.setText("");
            toDateTextView.setText("");
        }
    }

    /**
     * This method handles the creation of a pie chart, given the values as parameters
     * @param normalValue normal value
     * @param dangerValue danger value
     * @param warningValue warning value
     * @param hypoValue hypoglycemic value
     * @param hyperValue hyperglycemic value
     */
    private void insertPieChart(int normalValue, int dangerValue, int warningValue, int hypoValue, int hyperValue ) {

        // pie chart
        pieChart = (PieChart) view.findViewById(R.id.pie_chart);

        pieChart.setVisibility(View.VISIBLE);

        // entries for the chart (based on parameters)
        PieEntry normalEntry = new PieEntry(normalValue, (normalValue != 0 ? "normal" : "") );
        PieEntry dangerEntry = new PieEntry(dangerValue, (dangerValue != 0 ? "danger" : "") );
        PieEntry warningEntry = new PieEntry(warningValue, (warningValue != 0 ? "warning" : "") );
        PieEntry hypoEntry = new PieEntry(hypoValue, (hypoValue != 0 ? "hypo" : "") );
        PieEntry hyperEntry = new PieEntry(hyperValue, (hyperValue != 0 ? "hyper" : "") );

        // appending entries to array list
        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(normalEntry);
        entries.add(dangerEntry);
        entries.add(warningEntry);
        entries.add(hypoEntry);
        entries.add(hyperEntry);

        // generating dataset with the list, and associating colors to it
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ContextCompat.getColor(getActivity(), R.color.TURQUOISE),
                ContextCompat.getColor(getActivity(), R.color.CARROT),
                ContextCompat.getColor(getActivity(), R.color.ORANGE),
                ContextCompat.getColor(getActivity(), R.color.ALIZARIN),
                ContextCompat.getColor(getActivity(), R.color.POMEGRANATE));
        PieData data = new PieData(dataSet);

        // text graphical features
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

        // chart title
        SpannableString s = new SpannableString("Blood sugar\nlevel hits");
        s.setSpan(new RelativeSizeSpan(1.4f), 0, 22, 0);
        s.setSpan(new StyleSpan(Typeface.NORMAL), 0, 22, 0);
        s.setSpan(new ForegroundColorSpan(ContextCompat.getColor(ctx, R.color.WET_ASPHALT)), 0, 22, 0);
        pieChart.setCenterText(s);

        // graphical features
        pieChart.setData(data);
        pieChart.getLegend().setEnabled(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.invalidate();
        pieChart.setHoleRadius(45f);
        pieChart.setTransparentCircleAlpha(0);
        pieChart.setHighlightPerTapEnabled(false);
        pieChart.setRotationAngle(180f);

        // if energy save mode is off, add animation
        if ((GlumoApplication.getBooleanPreference(R.string.energy_save_mode_switch_preference)))
            pieChart.animate();
        else
            pieChart.animateY(1000, Easing.EasingOption.EaseInOutQuad);
    }

    /**
     * This method handles the creation of a line chart, given the values as parameterslog
     * @param list list of glucose reads
     */
    private void insertLineChart (List<GlucoseRead> list) {

        // line chart
        lineChart = (LineChart) view.findViewById(R.id.line_chart);

        lineChart.setVisibility(View.VISIBLE);

        // handling date format
        final SimpleDateFormat sdf1 = new SimpleDateFormat(DBManager.dateFormat);
        final SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy MMM dd-HH:mm:ss");

        // y values for the graph
        ArrayList<Entry> yVals = new ArrayList<Entry>();
        int size = list.size();
        long tempDate = 0L;

        // iterating over the list, collects glucose values for each date
        for (int i = 0; i < size; i++) {
            try {
                tempDate = sdf1.parse(list.get(size-i-1).getTime()).getTime();
            } catch (ParseException e) {
                Toast.makeText(ctx, ctx.getString(R.string.data_corrupted), Toast.LENGTH_SHORT).show();
                tempDate = 0L;
            }
            yVals.add(new Entry( tempDate , list.get(size-i-1).getGlucose()));
        }

        lineChart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dateAxisCutLevel = -1;
            }
        });

        // handling axis values
        lineChart.getXAxis().setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {

                String tempAxisValue = sdf2.format(new Date(((long) value)));
                if ( dateAxisCutLevel == -1 ) {
                    firstTwoDateInGraphAxis = true;
                    dateAxisCutLevel = 0;
                    oldAxisValue = "";
                }

                if ("".equals(oldAxisValue)) {
                    oldAxisValue = tempAxisValue;
                    tempAxisValue = "";
                }

                else if (firstTwoDateInGraphAxis){
                    firstTwoDateInGraphAxis = false;
                    if (tempAxisValue.substring(0, 4).equals(oldAxisValue.substring(0, 4))) {   // same year
                        //Log.d("cut", "cut year, so is :" + tempAxisValue.substring(5));
                        tempAxisValue = tempAxisValue.substring(5);
                        dateAxisCutLevel += 5;
                        if (tempAxisValue.substring(0, 3).equals(oldAxisValue.substring(5, 8))) { // same month
                            //Log.d("cut", "cut month, so is :" + tempAxisValue.substring(4));
                            tempAxisValue = tempAxisValue.substring(4);
                            dateAxisCutLevel += 4;
                            if (tempAxisValue.substring(0, 2).equals(oldAxisValue.substring(9, 11))) { // same day
                                //Log.d("cut", "cut day, so is :" + tempAxisValue.substring(4));
                                tempAxisValue = tempAxisValue.substring(4);
                                dateAxisCutLevel += 4;
                                if (tempAxisValue.substring(0, 2).equals(oldAxisValue.substring(13, 15))) { // same hour
                                    //Log.d("cut", "cut hour, so is :" + tempAxisValue.substring(3));
                                    tempAxisValue = tempAxisValue.substring(3);
                                    dateAxisCutLevel += 3;
                                } else {
                                    //Log.d("cut", "NOT cut hour, so is :" + tempAxisValue.substring(0, 4));
                                    tempAxisValue = tempAxisValue.substring(0, 5);
                                }
                            } else {
                                //Log.d("cut", "NOT cut day, so is :" + tempAxisValue.substring(0, 6));
                                tempAxisValue = tempAxisValue.substring(0, 6);
                            }
                        } else {
                            //Log.d("cut", "not cut month, 1 is :" + tempAxisValue.substring(0, 3) + " and the other is :" + oldAxisValue.substring(5, 8) + " here");
                            tempAxisValue = tempAxisValue.substring(0, 3);
                        }
                    } else {
                        //Log.d("cut", "NOT cut year, so is :" + tempAxisValue.substring(0, 4));
                        tempAxisValue = tempAxisValue.substring(0, 4);
                    }
                }
                else {
                    tempAxisValue = tempAxisValue.substring(dateAxisCutLevel);
                }
                return tempAxisValue;

            }
        });

        final LineDataSet set1;

        if (lineChart.getData() != null &&
                lineChart.getData().getDataSetCount() > 0) {
            set1 = (LineDataSet) lineChart.getData().getDataSetByIndex(0);
            set1.setValues(yVals);
            lineChart.getData().notifyDataChanged();
            lineChart.notifyDataSetChanged();
        } else {
            // create a dataset and give it a type
            set1 = new LineDataSet(yVals, "DataSet 1");

            set1.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            set1.setCubicIntensity(0.2f);
            set1.setDrawCircles(true);
            set1.setCircleColor(ContextCompat.getColor(getActivity(), R.color.TURQUOISE));
            set1.setDrawFilled(true);
            set1.setLineWidth(1.8f);
            set1.setHighLightColor(Color.rgb(244, 117, 117));
            set1.setColor(ContextCompat.getColor(getActivity(), R.color.TURQUOISE));
            set1.setFillColor(ContextCompat.getColor(getActivity(), R.color.TURQUOISE));
            set1.setFillAlpha(50);
            set1.setDrawHorizontalHighlightIndicator(false);
            set1.setFillFormatter(new IFillFormatter() {
                @Override
                public float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider) {
                    return -10;
                }
            });



            // create a data object with the datasets
            LineData data = new LineData(set1);
            data.setValueTextSize(9f);
            data.setDrawValues(false);

            lineChart.getDescription().setEnabled(false);
            lineChart.setDrawGridBackground(false);
            lineChart.getLegend().setEnabled(false);

            // set data
            lineChart.setData(data);
            // dont forget to refresh the drawing
            lineChart.invalidate();
        }
        if ((GlumoApplication.getBooleanPreference(R.string.energy_save_mode_switch_preference)))
            lineChart.animate();
        else
            lineChart.animateXY(1000, 1000);
    }
}