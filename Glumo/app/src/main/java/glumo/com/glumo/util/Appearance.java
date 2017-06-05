package glumo.com.glumo.util;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import glumo.com.glumo.R;
import glumo.com.glumo.activity.MainActivity;
import glumo.com.glumo.application.GlumoApplication;

/**
 * This class handles the elemets of appearance in the app
 */
public class Appearance {

    // colors
    private static final int TURQUOISE = -15024996;
    private static final int EMERALD = -13710223;
    private static final int PETER_RIVER = -13330213;
    private static final int AMETHYST = -6596170;
    private static final int WET_ASPHALT = -13350562;
    private static final int SUN_FLOWER = -932849;
    private static final int CARROT = -1671646;
    private static final int ALIZARIN = -1618884;
    private static final int PINK_THEME = -2202936;
    private static final int CONCRETE = -6969946;

    // view elements
    private static MainActivity mainActivity;
    private static SharedPreferences sharedPreferences = GlumoApplication.getPreferences();
    private static TextView toolbarTitle;
    private static NavigationView navigationView;
    private static LinearLayout drawerHeaderLinearLayout;

    // get reference to the main activity
    public static void setUpMainActivity(MainActivity activity) {
        mainActivity = activity;
    }

    // get reference to the title
    public static void setUpTitle() {
        toolbarTitle = (TextView) mainActivity.findViewById(R.id.toolbar_title);
    }

    // remove action bar shadow
    public static void removeActionBarShadow() {
        mainActivity.getSupportActionBar().setElevation(0);
    }

    // add action bar shadow
    public static void addActionBarShadow() {
        mainActivity.getSupportActionBar().setElevation(8);
    }

    // set action bar title
    public static void setTitle(String title) {
        toolbarTitle.setText(title);
    }

    // update action bar color
    public static void updateActionBarAndStatusBarAndGradientColor() {
        int color = sharedPreferences.getInt(mainActivity.getString(R.string.theme_color), 0);
        setActionBarColor(color);
        setStatusBarColor(color);

        navigationView = (NavigationView) mainActivity.findViewById(R.id.navigation_view);
        drawerHeaderLinearLayout = (LinearLayout) navigationView.getHeaderView(0);
        Drawable drawerHeaderLinearLayoutBackground = drawerHeaderLinearLayout.getBackground();
        GradientDrawable gradientDrawable = (GradientDrawable) drawerHeaderLinearLayoutBackground;
        Context context = GlumoApplication.getContext();
        switch (color) {
            case TURQUOISE: {
                gradientDrawable.setColors(new int[]{
                        ContextCompat.getColor(context, R.color.TURQUOISE),
                        ContextCompat.getColor(context, R.color.PETER_RIVER)});
                break;
            }
            case EMERALD: {
                gradientDrawable.setColors(new int[]{
                        ContextCompat.getColor(context, R.color.EMERALD),
                        ContextCompat.getColor(context, R.color.GREEN_SEA)});
                break;
            }
            case PETER_RIVER: {
                gradientDrawable.setColors(new int[]{
                        ContextCompat.getColor(context, R.color.PETER_RIVER),
                        ContextCompat.getColor(context, R.color.TURQUOISE)});
                break;
            }
            case AMETHYST:{
                gradientDrawable.setColors(new int[]{
                        ContextCompat.getColor(context, R.color.AMETHYST),
                        ContextCompat.getColor(context, R.color.WET_ASPHALT)});
                break;
            }
            case WET_ASPHALT:{
                gradientDrawable.setColors(new int[]{
                        ContextCompat.getColor(context, R.color.WET_ASPHALT),
                        ContextCompat.getColor(context, R.color.AMETHYST)});
                break;
            }
            case SUN_FLOWER:{
                gradientDrawable.setColors(new int[]{
                        ContextCompat.getColor(context, R.color.SUN_FLOWER),
                        ContextCompat.getColor(context, R.color.CARROT)});
                break;
            }
            case CARROT:{
                gradientDrawable.setColors(new int[]{
                        ContextCompat.getColor(context, R.color.CARROT),
                        ContextCompat.getColor(context, R.color.SUN_FLOWER)});
                break;
            }
            case ALIZARIN:{
                gradientDrawable.setColors(new int[]{
                        ContextCompat.getColor(context, R.color.ALIZARIN),
                        ContextCompat.getColor(context, R.color.CARROT)});
                break;
            }
            case PINK_THEME:{
                gradientDrawable.setColors(new int[]{
                        ContextCompat.getColor(GlumoApplication.getContext(), R.color.PINK_THEME),
                        ContextCompat.getColor(GlumoApplication.getContext(), R.color.ALIZARIN)});
                break;
            }
            case CONCRETE:{
                gradientDrawable.setColors(new int[]{
                        ContextCompat.getColor(context, R.color.CONCRETE),
                        ContextCompat.getColor(context, R.color.CLOUDS)});
                break;
            }
            default: {
                gradientDrawable.setColors(new int[]{
                        ContextCompat.getColor(GlumoApplication.getContext(), R.color.PINK_THEME),
                        ContextCompat.getColor(GlumoApplication.getContext(), R.color.ALIZARIN)});
            }
        }
    }

    // set action bar color
    public static void setActionBarColor(int color) {
        mainActivity.getSupportActionBar().setBackgroundDrawable(new ColorDrawable(color));
    }

    // set status bar color
    public static void setStatusBarColor(int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mainActivity.getWindow().setStatusBarColor(color);
        }
    }


    // rgb conversion
    public static int blendColors(int from, int to, float ratio) {
        final float inverseRatio = 1f - ratio;
        final float r = Color.red(to) * ratio + Color.red(from) * inverseRatio;
        final float g = Color.green(to) * ratio + Color.green(from) * inverseRatio;
        final float b = Color.blue(to) * ratio + Color.blue(from) * inverseRatio;
        return Color.rgb((int) r, (int) g, (int) b);
    }


    /**
     *  This util method takes the glucose value and return the relevant color
     *  @param glucoseValue : the glucose value. 0 if there is not a value yet
     *  @param thresholds : hyperglycemia and hypoglycemia thresholds
     *  @return color : amethyst - red - orange - green - silver
     */
    public static int getColorBasedOnThresholds(int glucoseValue, int[] thresholds) {
        int color;
        int percentage = ((thresholds[1] - glucoseValue) * 100) / (thresholds[1] - thresholds[0]);
        if (glucoseValue == 0)
            color = R.color.SILVER;
        else if (percentage < 0 || percentage > 100)
            color = R.color.AMETHYST;
        else if (percentage < 15 || percentage > 85)
            color = R.color.ALIZARIN;
        else if (percentage < 35 || percentage > 75)
            color = R.color.ORANGE;
        else
            color = R.color.TURQUOISE;
        return color;
    }

    /**
     * This util method takes the carbohydrates value and return the relevant color
     * @param carbohydrateValue carbohydrates value
     * @return color
     */
    public static int getColorBasedOnCarbohydrateValue(int carbohydrateValue){
        int color = 0;
        if(carbohydrateValue <= 25){
            color = R.color.TURQUOISE;
        }else if (carbohydrateValue <= 50){
            color = R.color.ORANGE;
        }else if (carbohydrateValue <= 75){
            color = R.color.CARROT;
        }else{
            color = R.color.ALIZARIN;
        }
        return color;
    }

    /**
     * This method simply handles the values for animation and starts it
     * @param fromNumber from number
     * @param toNumber to number
     * @param glucoseLevel glucose level
     * @param speed speed
     */
    public static void countAnimation(int fromNumber, int toNumber, final TextView glucoseLevel, int speed) {
        ValueAnimator animator = ValueAnimator.ofInt(fromNumber, toNumber);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                glucoseLevel.setText(animation.getAnimatedValue().toString());
            }
        });
        animator.setDuration(speed).start();
    }

    /**
     * This method applies an expand animation to the view given as parameter
     * @param v view
     */
    public static void expand(final View v) {
        v.measure(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT);
        final int targetHeight = v.getMeasuredHeight();

        // Older versions of android (pre API 21) cancel animations for views with a height of 0.
        v.getLayoutParams().height = 1;
        v.setVisibility(View.VISIBLE);
        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                v.getLayoutParams().height = interpolatedTime == 1
                        ? RecyclerView.LayoutParams.WRAP_CONTENT
                        : (int) (targetHeight * interpolatedTime);
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        // 1dp/ms
        a.setDuration((int) (targetHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }

    /**
     * This method applies a collapse animation to the view given as parameter
     * @param v view
     */
    public static void collapse(final View v) {
        final int initialHeight = v.getMeasuredHeight();

        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    v.setVisibility(View.GONE);
                } else {
                    v.getLayoutParams().height = initialHeight - (int) (initialHeight * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        // 1dp/ms
        a.setDuration((int) (initialHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }

    /**
     * This method simply handles the hiding of the keyboard
     * @param view view
     */
    public static void hideKeyboard(View view){
        InputMethodManager inputMethodManager = (InputMethodManager) GlumoApplication.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

}
