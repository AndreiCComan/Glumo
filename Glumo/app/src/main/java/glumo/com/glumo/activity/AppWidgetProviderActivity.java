package glumo.com.glumo.activity;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.RemoteViews;

import glumo.com.glumo.R;
import glumo.com.glumo.application.GlumoApplication;
import glumo.com.glumo.util.Appearance;

/**
 * This class handles the app widget
 */
public class AppWidgetProviderActivity extends AppWidgetProvider {

    // widget view
    public static int oldValue = -1;
    private static final int SET_DOWN_ARROW = 1;
    private static final int SET_EQUAL_ARROW = 0;
    private static final int SET_UP_ARROW = 2;
    private static RemoteViews remoteViews;

    /**
     * This method performs the widget changes on the basis of the intent received
     * @param context app context
     * @param intent intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        // intent action
        String action = intent.getAction();

        // if read glucose intent is received
        if (GlumoApplication.BT_READ_GLUCOSE_R.equals(action)) {

            // get value
            int newValue = Integer.valueOf(intent.getStringExtra(GlumoApplication.ET_GLUCOMETER_RESPONSE_TAG));

            // get view of widget
            remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

            // get hypoglycemia and hyperglycemia thresholds
            int[] thresholds = {GlumoApplication.getIntPreference(R.string.hypoglycemia_preference), GlumoApplication.getIntPreference(R.string.hyperglycemia_preference)};

            // get color on the basis of thresholds
            int color = Appearance.getColorBasedOnThresholds(newValue, thresholds);
            remoteViews.setInt(R.id.widget_layout, "setBackgroundColor", ContextCompat.getColor(GlumoApplication.getContext(), color));

            // handle arrow on the basis of the previous value
            if (newValue == oldValue)
                setArrowPosition(SET_EQUAL_ARROW);
            else if (newValue < oldValue)
                setArrowPosition(SET_UP_ARROW);
            else
                setArrowPosition(SET_DOWN_ARROW);

            // set text
            remoteViews.setTextViewText(R.id.glucose_level_widget, String.valueOf(newValue));

            // Necessarily for update
            ComponentName thisWidget = new ComponentName(context, AppWidgetProviderActivity.class);

            // Widget manager
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

            // Update widget
            appWidgetManager.updateAppWidget(thisWidget, remoteViews);

            // new OldValue = current new value
            oldValue = newValue;
        }
    }

    // TODO vedere se si può sistemare il problema del widget e commentare
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {


        for (int i = 0; i < appWidgetIds.length; i++) {
            int currentWidgetId = appWidgetIds[i];

            Intent intent = new Intent(GlumoApplication.getContext(), MainActivity.class);

            PendingIntent pending = PendingIntent.getActivity(context, 0, intent, 0);
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            views.setOnClickPendingIntent(R.id.widget_layout, pending);

            appWidgetManager.updateAppWidget(currentWidgetId, views);
        }
    }

    /**
     * This method just sets the arrow position on the basis of the given parameter
     * @param arrowPosition arrow position
     */
    private static void setArrowPosition(int arrowPosition) {
        switch (arrowPosition) {
            case SET_UP_ARROW: {
                // 0 degrees
                remoteViews.setInt(R.id.arrow_glucose_widget, "setVisibility", View.INVISIBLE);
                // 45 degrees
                remoteViews.setInt(R.id.arrow_glucose_widget_p45, "setVisibility", View.VISIBLE);
                // -45 degrees
                remoteViews.setInt(R.id.arrow_glucose_widget_n45, "setVisibility", View.INVISIBLE);
                break;
            }
            case SET_EQUAL_ARROW: {
                // 0 degrees
                remoteViews.setInt(R.id.arrow_glucose_widget, "setVisibility", View.VISIBLE);
                // 45 degrees
                remoteViews.setInt(R.id.arrow_glucose_widget_p45, "setVisibility", View.INVISIBLE);
                // -45 degrees
                remoteViews.setInt(R.id.arrow_glucose_widget_n45, "setVisibility", View.INVISIBLE);
                break;
            }
            case SET_DOWN_ARROW: {
                // 0 degrees
                remoteViews.setInt(R.id.arrow_glucose_widget, "setVisibility", View.INVISIBLE);
                // 45 degrees
                remoteViews.setInt(R.id.arrow_glucose_widget_p45, "setVisibility", View.INVISIBLE);
                // -45 degrees
                remoteViews.setInt(R.id.arrow_glucose_widget_n45, "setVisibility", View.VISIBLE);
                break;
            }
        }
    }
}