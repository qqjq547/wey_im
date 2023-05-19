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
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import framework.telegram.ui.R;


/**
 * Abstract class for PIN set activity.
 * Subclass this activity to show SetPin screen.
 * All subclasses should implement isPinCorrect() method
 *
 * @since 1.0.0
 */
public abstract class SetPinActivity extends BasePinActivity {

    public static final String KEY = "key";

    public static final String SHOW_OTHER_LOCK_TYPE = "showOtherLockType";

    public static final int CREATE_REQUEST_CODE = 0x00999;

    protected String key;

    protected boolean showOtherLockType;

    /**
     * Stores the first PIN entered by user. Used for confirmation
     */
    private String firstPin = "";

    private TextView otherLockTypeButton, resetInputButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        key = getIntent().getStringExtra(KEY);
        showOtherLockType = getIntent().getBooleanExtra(SHOW_OTHER_LOCK_TYPE, false);

        if (TextUtils.isEmpty(key)) {
            //无效操作，退出
            finish();
        } else {
            initView();

            setLabel(getString(R.string.applock_pincode_title_input));

            otherLockTypeButton = findViewById(R.id.other_lock_type);
            if (showOtherLockType) {
                otherLockTypeButton.setVisibility(View.VISIBLE);
                otherLockTypeButton.setOnClickListener(v -> {
                    // 换其他方式
                    onChangeLockType();
                });
            }

            resetInputButton = findViewById(R.id.reset_input);
            resetInputButton.setVisibility(View.GONE);
            resetInputButton.setOnClickListener(v -> {
                firstPin = "";
                resetStatus();

                if (showOtherLockType) {
                    otherLockTypeButton.setVisibility(View.VISIBLE);
                }

                resetInputButton.setVisibility(View.GONE);
                setLabel(getString(R.string.applock_pincode_title_input));
            });
        }
    }

    private void initView() {
        View toolbar = findViewById(R.id.toolbar);
        ImageButton backBtn = findViewById(R.id.btn_go_back);
        toolbar.setVisibility(View.VISIBLE);
        backBtn.setVisibility(View.VISIBLE);

        TextView titleTextView = findViewById(R.id.tv_toolbar_title);
        titleTextView.setText(getString(R.string.applock_pincode_title_setting));

        TextView rightTextView = findViewById(R.id.tv_toolbar_right_button);
        rightTextView.setVisibility(View.GONE);

        backBtn.setOnClickListener(v -> finish());
    }

    /**
     * Implementation of BasePinActivity method
     *
     * @param pin PIN value entered by user
     */
    @Override
    public void onComplete(String pin) {
        resetStatus();

        if ("".equals(firstPin)) {
            firstPin = pin;
            setLabel(getString(R.string.applock_pincode_input_again));
            otherLockTypeButton.setVisibility(View.GONE);
            resetInputButton.setVisibility(View.VISIBLE);
        } else {
            if (pin.equals(firstPin)) {
                PinCodeUnlock.getInstance().setPinCode(SetPinActivity.this, key, firstPin);
                setResult(Activity.RESULT_OK);
                finish();
            } else {
                setLabel(getString(R.string.applock_pincode_input_error_again), ContextCompat.getColor(SetPinActivity.this, R.color.f50d2e));
                resetInputButton.setVisibility(View.VISIBLE);
                startDotErrorStyle();
            }
        }
    }

    @Override
    public void onEmpty() {

    }

    public abstract void onChangeLockType();

    @Override
    public void onBackPressed() {

    }
}
