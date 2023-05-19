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
import framework.telegram.ui.image.AppImageView;


/**
 * Abstract class for PIN confirm activity.
 * Subclass this activity to show ConfirmPin screen.
 * All subclasses should implement isPinCorrect() method
 *
 * @since 1.0.0
 */
public abstract class ConfirmPinActivity extends BasePinActivity {

    public static final String KEY = "key";
    public static final String ICON = "icon";
    public static final String TYPE = "type";

    public static final int VERIFY_REQUEST_CODE = 0x00998;

    protected String key, icon;
    protected int type;

    private AppImageView iconView;
    private ImageButton backBtn;
    private TextView rightTextView;
    private TextView titleTextView;
    private View toolbar,line;
    private int leftInputCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        key = getIntent().getStringExtra(KEY);
        icon = getIntent().getStringExtra(ICON);
        type = getIntent().getIntExtra(TYPE, 0);

        if (TextUtils.isEmpty(key) || type < 1 || type > 3) {
            //无效操作，退出
            finish();
        } else {
            initView();

            TextView forgetPinCodeButton = findViewById(R.id.forget_pin_code);
            forgetPinCodeButton.setOnClickListener(v -> {
                // 忘记数字密码
                onForgetPinCode();
            });

            if (type == 1) {
                // 关闭密码进行验证
                backBtn.setVisibility(View.VISIBLE);
                rightTextView.setVisibility(View.GONE);
                forgetPinCodeButton.setVisibility(View.GONE);
                iconView.setVisibility(View.INVISIBLE);
                leftInputCount = PinCodeUnlock.getInstance().getUnlockErrorCount(ConfirmPinActivity.this, key + "_modify");

                toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.f8f8f8));
                line.setVisibility(View.VISIBLE);
            } else if (type == 2) {
                // 修改密码进行验证
                backBtn.setVisibility(View.VISIBLE);
                rightTextView.setVisibility(View.GONE);
                forgetPinCodeButton.setVisibility(View.GONE);
                iconView.setVisibility(View.INVISIBLE);
                leftInputCount = PinCodeUnlock.getInstance().getUnlockErrorCount(ConfirmPinActivity.this, key + "_modify");

                toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.f8f8f8));
                line.setVisibility(View.VISIBLE);
            } else {
                // 验证
                backBtn.setVisibility(View.GONE);
                titleTextView.setVisibility(View.GONE);
                rightTextView.setVisibility(View.VISIBLE);
                if (isDisableAccountIsOpen()) {
                    forgetPinCodeButton.setVisibility(View.GONE);
                } else {
                    forgetPinCodeButton.setVisibility(View.VISIBLE);
                }
                iconView.setVisibility(View.VISIBLE);
                iconView.setImageURI(icon);
                leftInputCount = PinCodeUnlock.getInstance().getUnlockErrorCount(ConfirmPinActivity.this, key + "_verify");

                toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.white));
                line.setVisibility(View.GONE);
            }

            long pastTime = getUnlockErrorMax(type);
            if (pastTime < PinCodeUnlock.UNLOCK_ERROR_WAIT_TIME) {
                // 还处于上一次错误输入的时间限制内
                setLabel(String.format(getString(R.string.plugin_uexGestureUnlock_verificationErrorMaxPrompt), (5 - (pastTime / 1000 / 60))), ContextCompat.getColor(ConfirmPinActivity.this, R.color.f50d2e));
                startDotErrorStyle();
            } else {
                setLabel(getString(R.string.applock_pincode_title_verify));
            }
        }
    }

    private void initView() {
        toolbar = findViewById(R.id.toolbar);
        backBtn = findViewById(R.id.btn_go_back);
        toolbar.setVisibility(View.VISIBLE);

        line = findViewById(R.id.line);

        titleTextView = findViewById(R.id.tv_toolbar_title);
        titleTextView.setText(getString(R.string.applock_pincode_title_verify));

        rightTextView = findViewById(R.id.tv_toolbar_right_button);

        iconView = findViewById(R.id.unlock_user_logo);

        backBtn.setOnClickListener(v -> finish());
        rightTextView.setOnClickListener(v -> onChangeAccount());
    }

    /**
     * Implementation of BasePinActivity method
     *
     * @param pin PIN value entered by user
     */
    @Override
    public final void onComplete(String pin) {
        resetStatus();

        long pastTime = getUnlockErrorMax(type);
        if (pastTime < PinCodeUnlock.UNLOCK_ERROR_WAIT_TIME) {
            // 还处于上一次错误输入的时间限制内
            setLabel(String.format(getString(R.string.plugin_uexGestureUnlock_verificationErrorMaxPrompt), (5 - (pastTime / 1000 / 60))), ContextCompat.getColor(ConfirmPinActivity.this, R.color.f50d2e));
            startDotErrorStyle();
        } else {
            if (isPinCorrect(pin)) {
                // 输入正确
                int totalCount = 0;
                if (type == 3) {
                    totalCount = PinCodeUnlock.getInstance().getUnlockErrorTotalCount(ConfirmPinActivity.this, key + "_verify");
                    PinCodeUnlock.getInstance().setUnlockErrorTotalCount(ConfirmPinActivity.this, key + "_verify", 0);
                }

                if (totalCount >= 20 && isDisableAccountIsOpen()) {
                    onDisableAccount();
                } else {
                    setUnlockErrorCount(type, 5);
                    onConfirmSuccess();
                    setResult(Activity.RESULT_OK);
                    finish();
                }
            } else {
                // 输入错误
                int totalCount = 0;
                if (type == 3) {
                    totalCount = PinCodeUnlock.getInstance().getUnlockErrorTotalCount(ConfirmPinActivity.this, key + "_verify") + 1;
                    PinCodeUnlock.getInstance().setUnlockErrorTotalCount(ConfirmPinActivity.this, key + "_verify", totalCount);
                }

                if (totalCount >= 20 && isDisableAccountIsOpen()) {
                    onDisableAccount();
                } else {
                    leftInputCount--;
                    if (totalCount == 19 && isDisableAccountIsOpen()) {
                        setUnlockErrorCount(type, leftInputCount);
                        setLabel(getString(R.string.verification_error_disable_account_warn), ContextCompat.getColor(ConfirmPinActivity.this, R.color.f50d2e));
                        startDotErrorStyle();
                    } else {
                        if (leftInputCount > 0) {
                            setUnlockErrorCount(type, leftInputCount);
                            setLabel(String.format(getString(R.string.plugin_uexGestureUnlock_verificationErrorPrompt), leftInputCount), ContextCompat.getColor(ConfirmPinActivity.this, R.color.f50d2e));
                            startDotErrorStyle();
                        } else if (leftInputCount == 0) {
                            leftInputCount = 5;
                            setUnlockErrorCount(type, 5);
                            setUnlockErrorMax(type);
                            setLabel(String.format(getString(R.string.plugin_uexGestureUnlock_verificationErrorMaxPrompt), 5), ContextCompat.getColor(ConfirmPinActivity.this, R.color.f50d2e));
                            startDotErrorStyle();
                        } else {
                            // 不需处理
                        }
                    }
                }
            }
        }
    }

    private void setUnlockErrorCount(int type, int leftInputCount) {
        if (type == 1) {
            // 关闭密码进行验证
            PinCodeUnlock.getInstance().setUnlockErrorCount(ConfirmPinActivity.this, key + "_modify", leftInputCount);
        } else if (type == 2) {
            // 修改密码进行验证
            PinCodeUnlock.getInstance().setUnlockErrorCount(ConfirmPinActivity.this, key + "_modify", leftInputCount);
        } else {
            // 验证
            PinCodeUnlock.getInstance().setUnlockErrorCount(ConfirmPinActivity.this, key + "_verify", leftInputCount);
        }
    }

    private long getUnlockErrorMax(int type) {
        if (type == 1) {
            return System.currentTimeMillis() - PinCodeUnlock.getInstance().getUnlockErrorMax(ConfirmPinActivity.this, key + "_modify");
        } else if (type == 2) {
            return System.currentTimeMillis() - PinCodeUnlock.getInstance().getUnlockErrorMax(ConfirmPinActivity.this, key + "_modify");
        } else {
            return System.currentTimeMillis() - PinCodeUnlock.getInstance().getUnlockErrorMax(ConfirmPinActivity.this, key + "_verify");
        }
    }

    private void setUnlockErrorMax(int type) {
        if (type == 1) {
            // 关闭密码进行验证
            PinCodeUnlock.getInstance().setUnlockErrorMax(ConfirmPinActivity.this, key + "_modify");
        } else if (type == 2) {
            // 修改密码进行验证
            PinCodeUnlock.getInstance().setUnlockErrorMax(ConfirmPinActivity.this, key + "_modify");
        } else {
            // 验证
            PinCodeUnlock.getInstance().setUnlockErrorMax(ConfirmPinActivity.this, key + "_verify");
        }
    }

    protected abstract void onForgetPinCode();

    protected abstract void onChangeAccount();

    protected abstract void onConfirmSuccess();

    protected abstract void onDisableAccount();

    protected abstract boolean isDisableAccountIsOpen();

    @Override
    public void onEmpty() {

    }

    private boolean isPinCorrect(String pin) {
        return pin.equals(PinCodeUnlock.getInstance().getPinCodeSet(ConfirmPinActivity.this, key));
    }

    @Override
    public void onBackPressed() {

    }
}
