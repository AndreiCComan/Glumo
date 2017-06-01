package glumo.com.glumo.object;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.NumberPicker;

import glumo.com.glumo.R;

/**
 * This class handles the number picker object
 */
public class NumberPickerPreference extends DialogPreference {

    // constant values for the picker
    public static final int DEFAULT_MAX_VALUE = 100;
    public static final int DEFAULT_MIN_VALUE = 0;
    public static final boolean DEFAULT_WRAP_SELECTOR_WHEEL = true;

    private final int minValue;
    private final int maxValue;
    private final boolean wrapSelectorWheel;

    // picker and value
    private NumberPicker picker;
    private int value;

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.dialogPreferenceStyle);
    }

    /**
     * This method simply sets the attributes values
     * @param context context
     * @param attrs attributes
     * @param defStyleAttr style attributes
     */
    public NumberPickerPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.NumberPickerPreference);
        minValue = a.getInteger(R.styleable.NumberPickerPreference_minValue, DEFAULT_MIN_VALUE);
        maxValue = a.getInteger(R.styleable.NumberPickerPreference_maxValue, DEFAULT_MAX_VALUE);
        wrapSelectorWheel = a.getBoolean(R.styleable.NumberPickerPreference_wrapSelectorWheel, DEFAULT_WRAP_SELECTOR_WHEEL);
        a.recycle();
    }

    /**
     * This method simply creates a dialog with the picker and returns it
     * @return dialog
     */
    @Override
    protected View onCreateDialogView() {
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;

        picker = new NumberPicker(getContext());
        picker.setLayoutParams(layoutParams);

        FrameLayout dialogView = new FrameLayout(getContext());
        dialogView.addView(picker);
        return dialogView;
    }

    /**
     * This method simply sets the values of the picker
     * @param view view
     */
    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        picker.setMinValue(minValue);
        picker.setMaxValue(maxValue);
        picker.setWrapSelectorWheel(wrapSelectorWheel);
        picker.setValue(getValue());
    }

    /**
     * This method handles the case in which the user's response to the dialog
     * is positive, setting new value with the picker
     * @param positiveResult boolean response from user
     */
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            picker.clearFocus();
            int newValue = picker.getValue();
            if (callChangeListener(newValue)) {
                setValue(newValue);
                setSummary(String.valueOf(getValue()));
            }
        }
    }

    /**
     * This method simply gets the minimu value of the picker
     * @param a array
     * @param index index of the array
     * @return min value
     */
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, minValue);
    }

    /**
     * This method simply sets the default value from the picker, possibly keeping the persistent
     * one
     * @param restorePersistedValue persistent value
     * @param defaultValue default value
     */
    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        setValue(restorePersistedValue ? getPersistedInt(minValue) : (Integer) defaultValue);
    }

    /**
     * This method simply sets a persistent value
     * @param value
     */
    public void setValue(int value) {
        this.value = value;
        persistInt(this.value);
    }

    /**
     * This method simply returns the attribute VALUE
     * @return value
     */
    public int getValue() {
        return this.value;
    }

    /**
     * This method simply sets the text value of the preferences with the VALUE attributes
     * @param parent parent
     * @return super
     */
    @Override
    protected View onCreateView(ViewGroup parent) {
        setSummary(String.valueOf(getValue()));
        return super.onCreateView(parent);
    }
}
