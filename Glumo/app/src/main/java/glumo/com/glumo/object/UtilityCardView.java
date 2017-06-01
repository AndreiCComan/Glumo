package glumo.com.glumo.object;

import android.support.v7.widget.CardView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * This method handles the utility card object
 */

public class UtilityCardView {

    // view elements
    private CardView glucoseValueLayoutCardView;
    private LinearLayout glucoseValueLayout;
    private TextView glucoseTextViewValue;
    private ImageView glucoseValueArrow;
    private TextView glucoseValueTime;
    private int actualGlucoseLayoutTranslation;

    private static int cardHeight;
    private static int cardPadding;

    /**
     * This constructor method simply sets the attributes of the objects
     * @param glucoseValueLayoutCardView glucose value layout card view
     * @param glucoseValueLayout glucose value layout
     * @param glucoseTextViewValue glucose text view value
     * @param glucoseValueArrow glucose value arrow
     * @param glucoseValueTime glucose value time
     * @param actualGlucoseLayoutTranslation actual glucose layout translation
     */
    public UtilityCardView (CardView glucoseValueLayoutCardView,
                     LinearLayout glucoseValueLayout,
                     TextView glucoseTextViewValue,
                     ImageView glucoseValueArrow,
                     TextView glucoseValueTime,
                     int actualGlucoseLayoutTranslation) {
        setGlucoseValueLayoutCardView(glucoseValueLayoutCardView);
        setGlucoseValueLayout(glucoseValueLayout);
        setGlucoseTextViewValue(glucoseTextViewValue);
        setGlucoseValueArrow(glucoseValueArrow);
        setGlucoseValueTime(glucoseValueTime);
        setActualGlucoseLayoutTranslation(actualGlucoseLayoutTranslation);
    }

    // gets card height
    public static int getCardHeight() {
        return cardHeight;
    }

    // sets card height
    public static void setCardHeight(int cardHeight) {
        UtilityCardView.cardHeight = cardHeight;
    }

    // gets card padding
    public static int getCardPadding() {
        return cardPadding;
    }

    // sets card height
    public static void setCardPadding(int cardPadding) {
        UtilityCardView.cardPadding = cardPadding;
    }

    // gets glucose value layout
    public LinearLayout getGlucoseValueLayout() {
        return glucoseValueLayout;
    }

    // sets glucose value layout
    public void setGlucoseValueLayout(LinearLayout glucoseValueLayout) {
        this.glucoseValueLayout = glucoseValueLayout;
    }

    // gets glucose value layout card view
    public CardView getGlucoseValueLayoutCardView() {
        return glucoseValueLayoutCardView;
    }

    // sets glucose value layout card view
    public void setGlucoseValueLayoutCardView(CardView glucoseValueLayoutCardView) {
        this.glucoseValueLayoutCardView = glucoseValueLayoutCardView;
    }

    // gets glucose value arrow
    public ImageView getGlucoseValueArrow() {
        return glucoseValueArrow;
    }

    // sets glucose value arrow
    public void setGlucoseValueArrow(ImageView glucoseValueArrow) {
        this.glucoseValueArrow = glucoseValueArrow;
    }

    // gets glucose value time
    public TextView getGlucoseValueTime() {
        return glucoseValueTime;
    }

    // sets glucose value time
    public void setGlucoseValueTime(TextView glucoseValueTime) {
        this.glucoseValueTime = glucoseValueTime;
    }

    // gets actual glucose layout translation
    public int getActualGlucoseLayoutTranslation() {
        return actualGlucoseLayoutTranslation;
    }

    // sets actual glucose layout translation
    public void setActualGlucoseLayoutTranslation(int actualGlucoseLayoutTranslation) {
        this.actualGlucoseLayoutTranslation = actualGlucoseLayoutTranslation;
    }

    // gets glucose text view value
    public TextView getGlucoseTextViewValue() {
        return glucoseTextViewValue;
    }

    // sets glucose text view value
    public void setGlucoseTextViewValue(TextView glucoseTextViewValue) {
        this.glucoseTextViewValue = glucoseTextViewValue;
    }

    // increment actual glucose layout translation
    public void addActualGlucoseLayoutTranslation (int value) {
        this.actualGlucoseLayoutTranslation += value;
    }
}
