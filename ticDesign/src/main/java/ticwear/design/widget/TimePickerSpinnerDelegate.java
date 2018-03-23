/*
 * Copyright (C) 2016 Mobvoi Inc.
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ticwear.design.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Locale;

import ticwear.design.R;

import static android.view.View.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
import static android.view.View.IMPORTANT_FOR_ACCESSIBILITY_YES;

/**
 * A delegate implementing the basic spinner-based TimePicker.
 */
class TimePickerSpinnerDelegate extends TimePicker.AbstractTimePickerDelegate {
    private static final boolean DEFAULT_ENABLED_STATE = true;
    private static final int HOURS_IN_HALF_DAY = 12;

    // state
    private boolean mIs24HourView;
    private boolean mIsAm;

    // ui components
    private final NumberPicker mHourSpinner;
    private final NumberPicker mMinuteSpinner;
    private final NumberPicker mSecondSpinner;
    private final NumberPicker mAmPmSpinner;
    private final EditText mHourSpinnerInput;
    private final EditText mMinuteSpinnerInput;
    private final EditText mSecondSpinnerInput;
    private final EditText mAmPmSpinnerInput;
    private final TextView mDivider;
    private final TextView mDivider2;

    // Note that the legacy implementation of the TimePicker is
    // using a button for toggling between AM/PM while the new
    // version uses a NumberPicker spinner. Therefore the code
    // accommodates these two cases to be backwards compatible.
    private final Button mAmPmButton;

    private final String[] mAmPmStrings;

    private boolean mIsEnabled = DEFAULT_ENABLED_STATE;
    private Calendar mTempCalendar;
    private boolean mHourWithTwoDigit;
    private char mHourFormat;

    public TimePickerSpinnerDelegate(TimePicker delegator, Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(delegator, context);

        // process style attributes
        final TypedArray a = mContext.obtainStyledAttributes(
                attrs, R.styleable.TimePicker, defStyleAttr, defStyleRes);
        final int layoutResourceId = a.getResourceId(
                R.styleable.TimePicker_tic_legacyLayout, R.layout.time_picker_ticwear);
        a.recycle();

        final LayoutInflater inflater = LayoutInflater.from(mContext);
        inflater.inflate(layoutResourceId, mDelegator, true);

        // hour
        mHourSpinner = (NumberPicker) delegator.findViewById(R.id.tic_hour);
        mHourSpinner.setOnFocusChangeListener(mDelegator);
        mHourSpinner.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            public void onValueChange(NumberPicker spinner, int oldVal, int newVal) {
                updateInputState();
                if (!is24HourView()) {
                    if ((oldVal == HOURS_IN_HALF_DAY - 1 && newVal == HOURS_IN_HALF_DAY) ||
                            (oldVal == HOURS_IN_HALF_DAY && newVal == HOURS_IN_HALF_DAY - 1)) {
                        mIsAm = !mIsAm;
                        updateAmPmControl();
                    }
                }
                onTimeChanged();
            }
        });
        mHourSpinnerInput = (EditText) mHourSpinner.findViewById(R.id.numberpicker_input);
        mHourSpinnerInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);

        // divider (only for the new widget style)
        mDivider = (TextView) mDelegator.findViewById(R.id.tic_divider);
        if (mDivider != null) {
            setDividerText(mDivider);
        }

        // minute
        mMinuteSpinner = (NumberPicker) mDelegator.findViewById(R.id.tic_minute);
        mMinuteSpinner.setMinValue(0);
        mMinuteSpinner.setMaxValue(59);
        mMinuteSpinner.setOnLongPressUpdateInterval(100);
        mMinuteSpinner.setFormatter(NumberPicker.getTwoDigitFormatter());
        mMinuteSpinner.setOnFocusChangeListener(mDelegator);
        mMinuteSpinner.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            public void onValueChange(NumberPicker spinner, int oldVal, int newVal) {
                updateInputState();
                int minValue = mMinuteSpinner.getMinValue();
                int maxValue = mMinuteSpinner.getMaxValue();
                if (oldVal == maxValue && newVal == minValue) {
                    int newHour = mHourSpinner.getValue() + 1;
                    if (!is24HourView() && newHour == HOURS_IN_HALF_DAY) {
                        mIsAm = !mIsAm;
                        updateAmPmControl();
                    }
                    mHourSpinner.setValue(newHour);
                } else if (oldVal == minValue && newVal == maxValue) {
                    int newHour = mHourSpinner.getValue() - 1;
                    if (!is24HourView() && newHour == HOURS_IN_HALF_DAY - 1) {
                        mIsAm = !mIsAm;
                        updateAmPmControl();
                    }
                    mHourSpinner.setValue(newHour);
                }
                onTimeChanged();
            }
        });
        mMinuteSpinnerInput = (EditText) mMinuteSpinner.findViewById(R.id.numberpicker_input);
        mMinuteSpinnerInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);

        mDivider2 = (TextView) mDelegator.findViewById(R.id.tic_divider2);
        if (mDivider2 != null) {
            setDividerText(mDivider2);
        }

        mSecondSpinner = (NumberPicker) mDelegator.findViewById(R.id.tic_seconds);
        mSecondSpinner.setMinValue(0);
        mSecondSpinner.setMaxValue(59);
        mSecondSpinner.setOnLongPressUpdateInterval(100);
        mSecondSpinner.setFormatter(NumberPicker.getTwoDigitFormatter());
        mSecondSpinner.setOnFocusChangeListener(mDelegator);
        mSecondSpinner.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            public void onValueChange(NumberPicker spinner, int oldVal, int newVal) {
                updateInputState();
                int minValue = mSecondSpinner.getMinValue();
                int maxValue = mSecondSpinner.getMaxValue();
                if (oldVal == maxValue && newVal == minValue) {
                    int newMinute = mMinuteSpinner.getValue() + 1;
                    mMinuteSpinner.setValue(newMinute);
                } else if (oldVal == minValue && newVal == maxValue) {
                    int newMinute = mMinuteSpinner.getValue() - 1;
                    mMinuteSpinner.setValue(newMinute);
                }
                onTimeChanged();
            }
        });
        mSecondSpinnerInput = (EditText) mSecondSpinner.findViewById(R.id.numberpicker_input);
        mSecondSpinnerInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);

        // Get the localized am/pm strings and use them in the spinner.
        mAmPmStrings = getAmPmStrings(context);

        // am/pm
        final View amPmView = mDelegator.findViewById(R.id.tic_amPm);
        if (amPmView instanceof Button) {
            mAmPmSpinner = null;
            mAmPmSpinnerInput = null;
            mAmPmButton = (Button) amPmView;
            mAmPmButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View button) {
                    button.requestFocus();
                    mIsAm = !mIsAm;
                    updateAmPmControl();
                    onTimeChanged();
                }
            });
        } else {
            mAmPmButton = null;
            mAmPmSpinner = (NumberPicker) amPmView;
            mAmPmSpinner.setMinValue(0);
            mAmPmSpinner.setMaxValue(1);
            mAmPmSpinner.setDisplayedValues(mAmPmStrings);
            mAmPmSpinner.setOnFocusChangeListener(mDelegator);
            mAmPmSpinner.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                    updateInputState();
                    picker.requestFocus();
                    mIsAm = !mIsAm;
                    updateAmPmControl();
                    onTimeChanged();
                }
            });
            mAmPmSpinnerInput = (EditText) mAmPmSpinner.findViewById(R.id.numberpicker_input);
            mAmPmSpinnerInput.setImeOptions(isAmPmAtStart() ?
                    EditorInfo.IME_ACTION_NEXT : EditorInfo.IME_ACTION_DONE);
        }

        if (isAmPmAtStart()) {
            // Move the am/pm view to the beginning
            ViewGroup amPmParent = (ViewGroup) delegator.findViewById(R.id.tic_timePickerLayout);
            amPmParent.removeView(amPmView);
            amPmParent.addView(amPmView, 0);
            // Swap layout margins if needed. They may be not symmetrical (Old Standard Theme
            // for example and not for Holo Theme)
            ViewGroup.MarginLayoutParams lp =
                    (ViewGroup.MarginLayoutParams) amPmView.getLayoutParams();
            final int startMargin = lp.getMarginStart();
            final int endMargin = lp.getMarginEnd();
            if (startMargin != endMargin) {
                lp.setMarginStart(endMargin);
                lp.setMarginEnd(startMargin);
            }
        }

        getHourFormatData();

        // update controls to initial state
        updateHourControl();
        updateMinuteControl();
        updateSecondControl();
        updateAmPmControl();

        // set to current time
        setCurrentHour(mTempCalendar.get(Calendar.HOUR_OF_DAY));
        setCurrentMinute(mTempCalendar.get(Calendar.MINUTE));
        setCurrentSecond(mTempCalendar.get(Calendar.SECOND));

        if (!isEnabled()) {
            setEnabled(false);
        }

        // If not explicitly specified this view is important for accessibility.
        if (mDelegator.getImportantForAccessibility() == IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
            mDelegator.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        }
    }

    private void getHourFormatData() {
        final String bestDateTimePattern = DateFormat.getBestDateTimePattern(mCurrentLocale,
                (mIs24HourView) ? "Hm" : "hm");
        final int lengthPattern = bestDateTimePattern.length();
        mHourWithTwoDigit = false;
        char hourFormat = '\0';
        // Check if the returned pattern is single or double 'H', 'h', 'K', 'k'. We also save
        // the hour format that we found.
        for (int i = 0; i < lengthPattern; i++) {
            final char c = bestDateTimePattern.charAt(i);
            if (c == 'H' || c == 'h' || c == 'K' || c == 'k') {
                mHourFormat = c;
                if (i + 1 < lengthPattern && c == bestDateTimePattern.charAt(i + 1)) {
                    mHourWithTwoDigit = true;
                }
                break;
            }
        }
    }

    private boolean isAmPmAtStart() {
        final String bestDateTimePattern = DateFormat.getBestDateTimePattern(mCurrentLocale,
                "hm" /* skeleton */);

        return bestDateTimePattern.startsWith("a");
    }

    /**
     * The time separator is defined in the Unicode CLDR and cannot be supposed to be ":".
     *
     * See http://unicode.org/cldr/trac/browser/trunk/common/main
     *
     * We pass the correct "skeleton" depending on 12 or 24 hours view and then extract the
     * separator as the character which is just after the hour marker in the returned pattern.
     */
    private void setDividerText(TextView divider) {
        final String skeleton = (mIs24HourView) ? "Hm" : "hm";
        final String bestDateTimePattern = DateFormat.getBestDateTimePattern(mCurrentLocale,
                skeleton);
        final String separatorText;
        int hourIndex = bestDateTimePattern.lastIndexOf('H');
        if (hourIndex == -1) {
            hourIndex = bestDateTimePattern.lastIndexOf('h');
        }
        if (hourIndex == -1) {
            // Default case
            separatorText = ":";
        } else {
            int minuteIndex = bestDateTimePattern.indexOf('m', hourIndex + 1);
            if  (minuteIndex == -1) {
                separatorText = Character.toString(bestDateTimePattern.charAt(hourIndex + 1));
            } else {
                separatorText = bestDateTimePattern.substring(hourIndex + 1, minuteIndex);
            }
        }
        divider.setText(separatorText);
    }

    public void setSecondsPickerVisible(boolean visible){
        int visibility = visible? View.VISIBLE : View.GONE;
        mDivider2.setVisibility(visibility);
        mSecondSpinner.setVisibility(visibility);
        mSecondSpinnerInput.setVisibility(visibility);
        if (visible) {
            setIs24HourView(true);
        } else {
            setIs24HourView(DateFormat.is24HourFormat(mContext));
        }
    }

    @Override
    public void setCurrentHour(Integer currentHour) {
        setCurrentHour(currentHour, true);
    }

    private void setCurrentHour(Integer currentHour, boolean notifyTimeChanged) {
        // why was Integer used in the first place?
        if (currentHour == null || currentHour == getCurrentHour()) {
            return;
        }
        if (!is24HourView()) {
            // convert [0,23] ordinal to wall clock display
            if (currentHour >= HOURS_IN_HALF_DAY) {
                mIsAm = false;
                if (currentHour > HOURS_IN_HALF_DAY) {
                    currentHour = currentHour - HOURS_IN_HALF_DAY;
                }
            } else {
                mIsAm = true;
                if (currentHour == 0) {
                    currentHour = HOURS_IN_HALF_DAY;
                }
            }
            updateAmPmControl();
        }
        mHourSpinner.setValue(currentHour);
        if (notifyTimeChanged) {
            onTimeChanged();
        }
    }

    @Override
    public Integer getCurrentHour() {
        int currentHour = mHourSpinner.getValue();
        if (is24HourView()) {
            return currentHour;
        } else if (mIsAm) {
            return currentHour % HOURS_IN_HALF_DAY;
        } else {
            return (currentHour % HOURS_IN_HALF_DAY) + HOURS_IN_HALF_DAY;
        }
    }

    @Override
    public void setCurrentMinute(Integer currentMinute) {
        if (currentMinute == getCurrentMinute()) {
            return;
        }
        mMinuteSpinner.setValue(currentMinute);
        onTimeChanged();
    }

    @Override
    public Integer getCurrentMinute() {
        return mMinuteSpinner.getValue();
    }

    @Override
    public void setCurrentSecond(Integer currentSecond) {
        if (currentSecond == getCurrentSecond()) {
            return;
        }
        mSecondSpinner.setValue(currentSecond);
        onTimeChanged();
    }

    @Override
    public Integer getCurrentSecond() {
        return mSecondSpinner.getValue();
    }

    @Override
    public void setIs24HourView(Boolean is24HourView) {
        if (mIs24HourView == is24HourView) {
            return;
        }
        // cache the current hour since spinner range changes and BEFORE changing mIs24HourView!!
        int currentHour = getCurrentHour();
        // Order is important here.
        mIs24HourView = is24HourView;
        getHourFormatData();
        updateHourControl();
        // set value after spinner range is updated
        setCurrentHour(currentHour, false);
        updateMinuteControl();
        updateSecondControl();
        updateAmPmControl();
    }

    @Override
    public boolean is24HourView() {
        return mIs24HourView;
    }

    @Override
    public void setOnTimeChangedListener(TimePicker.OnTimeChangedListener onTimeChangedListener) {
        mOnTimeChangedListener = onTimeChangedListener;
    }

    @Override
    public void setEnabled(boolean enabled) {
        mMinuteSpinner.setEnabled(enabled);
        if (mDivider != null) {
            mDivider.setEnabled(enabled);
        }
        if (mDivider2 != null) {
            mDivider2.setEnabled(enabled);
        }
        mSecondSpinner.setEnabled(enabled);
        mHourSpinner.setEnabled(enabled);
        if (mAmPmSpinner != null) {
            mAmPmSpinner.setEnabled(enabled);
        } else {
            mAmPmButton.setEnabled(enabled);
        }
        mIsEnabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return mIsEnabled;
    }

    @Override
    public int getBaseline() {
        return mHourSpinner.getBaseline();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        setCurrentLocale(newConfig.locale);
    }

    @Override
    public Parcelable onSaveInstanceState(Parcelable superState) {
        return new SavedState(superState, getCurrentHour(), getCurrentMinute(), getCurrentSecond());
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        setCurrentHour(ss.getHour());
        setCurrentMinute(ss.getMinute());
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        onPopulateAccessibilityEvent(event);
        return true;
    }

    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        int flags = DateUtils.FORMAT_SHOW_TIME;
        if (mIs24HourView) {
            flags |= DateUtils.FORMAT_24HOUR;
        } else {
            flags |= DateUtils.FORMAT_12HOUR;
        }
        mTempCalendar.set(Calendar.HOUR_OF_DAY, getCurrentHour());
        mTempCalendar.set(Calendar.MINUTE, getCurrentMinute());
        String selectedDateUtterance = DateUtils.formatDateTime(mContext,
                mTempCalendar.getTimeInMillis(), flags);
        event.getText().add(selectedDateUtterance);
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        event.setClassName(TimePicker.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        info.setClassName(TimePicker.class.getName());
    }

    @Override
    public View getCurrentFocusedPicker() {
        if (mDelegator == null || mDelegator.getChildCount() == 0) {
            return null;
        }
        return getFocusedLeafChild(mDelegator);
    }

    @Override
    public View getNextFocusPicker(View current) {
        if (mDelegator == null || mDelegator.getChildCount() == 0) {
            return null;
        }
        if (current == null) {
            current = getCurrentFocusedPicker();
        }

        return current == null ? null : mDelegator.focusSearch(current, View.FOCUS_FORWARD);
    }

    private View getFocusedLeafChild(View root) {
        if (root instanceof NumberPicker) {
            return root.hasFocus() ? root : null;
        }

        if (root instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) root;
            return getFocusedLeafChild(parent.getFocusedChild());
        }

        return null;
    }

    private void updateInputState() {
        // Make sure that if the user changes the value and the IME is active
        // for one of the inputs if this widget, the IME is closed. If the user
        // changed the value via the IME and there is a next input the IME will
        // be shown, otherwise the user chose another means of changing the
        // value and having the IME up makes no sense.
        InputMethodManager inputMethodManager = (InputMethodManager) mContext.getSystemService(
                Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            if (inputMethodManager.isActive(mHourSpinnerInput)) {
                mHourSpinnerInput.clearFocus();
                inputMethodManager.hideSoftInputFromWindow(mDelegator.getWindowToken(), 0);
            } else if (inputMethodManager.isActive(mMinuteSpinnerInput)) {
                mMinuteSpinnerInput.clearFocus();
                inputMethodManager.hideSoftInputFromWindow(mDelegator.getWindowToken(), 0);
            } else if (inputMethodManager.isActive(mSecondSpinnerInput)) {
                mSecondSpinnerInput.clearFocus();
                inputMethodManager.hideSoftInputFromWindow(mDelegator.getWindowToken(), 0);
            } else if (inputMethodManager.isActive(mAmPmSpinnerInput)) {
                mAmPmSpinnerInput.clearFocus();
                inputMethodManager.hideSoftInputFromWindow(mDelegator.getWindowToken(), 0);
            }
        }
    }

    private void updateAmPmControl() {
        if (is24HourView()) {
            if (mAmPmSpinner != null) {
                mAmPmSpinner.setVisibility(View.GONE);
            } else {
                mAmPmButton.setVisibility(View.GONE);
            }
        } else {
            int index = mIsAm ? Calendar.AM : Calendar.PM;
            if (mAmPmSpinner != null) {
                mAmPmSpinner.setValue(index);
                mAmPmSpinner.setVisibility(View.VISIBLE);
            } else {
                mAmPmButton.setText(mAmPmStrings[index]);
                mAmPmButton.setVisibility(View.VISIBLE);
            }
        }
        mDelegator.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
    }

    /**
     * Sets the current locale.
     *
     * @param locale The current locale.
     */
    @Override
    public void setCurrentLocale(Locale locale) {
        super.setCurrentLocale(locale);
        mTempCalendar = Calendar.getInstance(locale);
    }

    private void onTimeChanged() {
        mDelegator.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
        if (mOnTimeChangedListener != null) {
            mOnTimeChangedListener.onTimeChanged(mDelegator, getCurrentHour(),
                    getCurrentMinute(), getCurrentSecond());
        }
    }

    private void updateHourControl() {
        if (is24HourView()) {
            // 'k' means 1-24 hour
            if (mHourFormat == 'k') {
                mHourSpinner.setMinValue(1);
                mHourSpinner.setMaxValue(24);
            } else {
                mHourSpinner.setMinValue(0);
                mHourSpinner.setMaxValue(23);
            }
        } else {
            // 'K' means 0-11 hour
            if (mHourFormat == 'K') {
                mHourSpinner.setMinValue(0);
                mHourSpinner.setMaxValue(11);
            } else {
                mHourSpinner.setMinValue(1);
                mHourSpinner.setMaxValue(12);
            }
        }
        mHourSpinner.setFormatter(mHourWithTwoDigit ? NumberPicker.getTwoDigitFormatter() : null);
    }

    private void updateMinuteControl() {
        if ((is24HourView() || isAmPmAtStart()) && (View.GONE == mSecondSpinner.getVisibility())) {
            mMinuteSpinnerInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        } else {
            mMinuteSpinnerInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        }
    }

    private void updateSecondControl() {
        if (is24HourView() || isAmPmAtStart()) {
            mSecondSpinnerInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        } else {
            mMinuteSpinnerInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        }
    }

    /**
     * Used to save / restore state of time picker
     */
    private static class SavedState extends View.BaseSavedState {
        private final int mHour;
        private final int mMinute;
        private final int mSecond;

        private SavedState(Parcelable superState, int hour, int minute, int second) {
            super(superState);
            mHour = hour;
            mMinute = minute;
            mSecond = second;
        }

        private SavedState(Parcel in) {
            super(in);
            mHour = in.readInt();
            mMinute = in.readInt();
            mSecond = in.readInt();
        }

        public int getHour() {
            return mHour;
        }

        public int getMinute() {
            return mMinute;
        }

        public int getSecond() {
            return mSecond;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mHour);
            dest.writeInt(mMinute);
            dest.writeInt(mSecond);
        }

        @SuppressWarnings({"unused", "hiding"})
        public static final Parcelable.Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    public static String[] getAmPmStrings(Context context) {
        return new DateFormatSymbols().getAmPmStrings();
    }
}
