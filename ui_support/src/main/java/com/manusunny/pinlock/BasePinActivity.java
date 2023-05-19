/*
 * Copyright (C) 2015. Manu Sunny <manupsunny@gmail.com>
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
 *
 */

package com.manusunny.pinlock;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import com.andrognito.pinlockview.IndicatorDots;
import com.andrognito.pinlockview.PinLockListener;
import com.andrognito.pinlockview.PinLockView;

import framework.telegram.support.BaseActivity;
import framework.telegram.ui.R;

/**
 * Abstract class for basic PIN activity.
 * All subclasses should implement onCompleted(String) method.
 *
 * @since 1.0.0
 */
public abstract class BasePinActivity extends BaseActivity implements PinLockListener {

    /**
     * Holds reference to label added to the UI
     */
    private TextView label;

    /**
     * Holds reference to StatusDots added to the UI
     */
    private PinLockView lockView;

    private IndicatorDots indicatorDots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);
        lockView = findViewById(R.id.pin_lock_view);
        lockView.setPinLockListener(this);

        indicatorDots = findViewById(R.id.indicator_dots);
        lockView.attachIndicatorDots(indicatorDots);

        label = findViewById(R.id.label);
    }

    /**
     * Setting label text as String value passed
     *
     * @param text Text to be set as label text
     */
    public void setLabel(String text) {
        setLabel(text, Color.BLACK);
    }

    public void setLabel(String text, int textColor) {
        label.setText(text);
        label.setTextColor(textColor);
    }

    public void startDotErrorStyle() {
        lockView.startDotErrorStyle();
    }

    /**
     * Reset StatusDots to initial state where no dots are filled
     */
    public void resetStatus() {
        lockView.resetPinLockView();
    }

    @Override
    public abstract void onComplete(String pin);

    @Override
    public abstract void onEmpty();

    @Override
    public void onPinChange(int pinLength, String intermediatePin) {
    }
}
