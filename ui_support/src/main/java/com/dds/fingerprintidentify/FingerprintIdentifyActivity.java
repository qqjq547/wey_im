package com.dds.fingerprintidentify;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.codersun.fingerprintcompat.AFingerDialog;
import com.codersun.fingerprintcompat.AonFingerChangeCallback;
import com.codersun.fingerprintcompat.FingerManager;
import com.codersun.fingerprintcompat.SimpleFingerCheckCallback;

import java.lang.ref.WeakReference;

import framework.telegram.support.BaseActivity;
import framework.telegram.ui.R;

public abstract class FingerprintIdentifyActivity extends BaseActivity {

    public static final int REQUEST_CODE = 0x00099;

    public static final String KEY = "key";
    public static final String ICON = "icon";
    public static final String OPERATION = "openFingerprint";

    private boolean openFingerprint = false;
    private String key;
    private String icon;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fingerprint_identify);

        key = getIntent().getStringExtra(KEY);
        icon = getIntent().getStringExtra(ICON);
        if (TextUtils.isEmpty(key)) {
            //无效操作，退出
            finish();
        } else {
            openFingerprint = getIntent().getBooleanExtra(OPERATION, false);
            switch (FingerManager.checkSupport(FingerprintIdentifyActivity.this)) {
                case DEVICE_UNSUPPORTED:
                    showToast(getString(R.string.fingerprint_unsupport));
                    showGestureUnlock();
                    break;
                case SUPPORT_WITHOUT_DATA:
                    showToast(getString(R.string.fingerprint_system_no_data));
                    showGestureUnlock();
                    break;
                case SUPPORT:
                    FingerManager.build().setApplication(getApplication())
                            .setTitle(getString(R.string.fingerprint_title))
                            .setDes(getString(R.string.fingerprint_pressdown))
                            .setNegativeText(getString(R.string.cancel))
                            .setFingerDialogApi23(new MyFingerDialog(this, openFingerprint))
                            .setFingerCheckCallback(new SimpleFingerCheckCallback() {

                                @Override
                                public void onSucceed() {
                                    showToast(getString(R.string.fingerprint_verify_success));
                                    onUnlockSuccess();
                                    setResult(Activity.RESULT_OK);
                                    finish();
                                }

                                @Override
                                public void onError(String error) {
                                    showToast(getString(R.string.fingerprint_verify_fail));
                                    showGestureUnlock();
                                }

                                @Override
                                public void onCancel() {
                                    showGestureUnlock();
                                }
                            })
                            .setFingerChangeCallback(new AonFingerChangeCallback() {

                                @Override
                                protected void onFingerDataChange() {
                                    if (openFingerprint) {
                                        showToastLongTime(getString(R.string.fingerprint_verify_data_changed_1));
                                    } else {
                                        showToastLongTime(getString(R.string.fingerprint_verify_data_changed_2));
                                    }

                                    try {
                                        FingerManager.updateFingerData(FingerprintIdentifyActivity.this);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                    FingerprintIdentifyActivity.this.onFingerDataChange();
                                    showGestureUnlock();
                                }
                            })
                            .create()
                            .startListener(FingerprintIdentifyActivity.this);
                    break;
            }
        }
    }

    protected abstract void onFingerDataChange();

    protected abstract void showAppUnlockActivity(String key, String icon);

    protected abstract void onUnlockSuccess();

    private void showGestureUnlock() {
        if (openFingerprint) {
            setResult(Activity.RESULT_CANCELED);
        } else {
            showAppUnlockActivity(key, icon);
        }

        finish();
    }

    private void showToast(String msg) {
        Toast.makeText(FingerprintIdentifyActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    private void showToastLongTime(String msg) {
        Toast.makeText(FingerprintIdentifyActivity.this, msg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
        //禁止回退
    }

    @SuppressLint("ValidFragment")
    public static class MyFingerDialog extends AFingerDialog implements View.OnClickListener {

        private WeakReference<FingerprintIdentifyActivity> mActivityRef;

        private TextView titleTv;

        private TextView desTv;

        private boolean isSuccess;

        private boolean openFingerprint;

        public MyFingerDialog(FingerprintIdentifyActivity activity, boolean openFingerprint) {
            mActivityRef = new WeakReference<>(activity);
            this.openFingerprint = openFingerprint;
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            super.onCreateView(inflater, container, savedInstanceState);
            View view = inflater.inflate(R.layout.dialog_finger, null);

            titleTv = view.findViewById(com.codersun.fingerprintcompat.R.id.finger_dialog_title_tv);
            desTv = view.findViewById(com.codersun.fingerprintcompat.R.id.finger_dialog_des_tv);
            TextView cancelTv = view.findViewById(com.codersun.fingerprintcompat.R.id.finger_dialog_cancel_tv);
            cancelTv.setOnClickListener(this);

            titleTv.setText(getString(R.string.fingerprint_title));
            desTv.setText(getString(R.string.fingerprint_pressdown));

            return view;
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);

            if (!isSuccess && mActivityRef != null && mActivityRef.get() != null) {
                if (!openFingerprint) {
                    mActivityRef.get().showGestureUnlock();
                } else {
                    mActivityRef.get().finish();
                }
            }
        }

        @Override
        public void onSucceed() {
            isSuccess = true;
            dismiss();
        }

        @Override
        public void onFailed() {
            titleTv.setText(getString(R.string.fingerprint_title));
            desTv.setText(getString(R.string.fingerprint_verify_fail));
        }

        @Override
        public void onHelp(String help) {
            titleTv.setText(getString(R.string.fingerprint_title));
            desTv.setText(getString(R.string.fingerprint_pressdown));
        }

        @Override
        public void onError(String error) {
            titleTv.setText(getString(R.string.fingerprint_title));
            desTv.setText(getString(R.string.fingerprint_verify_fail));
        }

        @Override
        public void onCancelAuth() {
            dismiss();
        }

        @Override
        public void onClick(View v) {
            dismiss();
        }
    }
}
