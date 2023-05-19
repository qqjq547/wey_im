package com.dds.gestureunlock;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.dds.gestureunlock.fragment.GestureCreateFragment;
import com.dds.gestureunlock.fragment.GestureVerifyFragment;
import com.dds.gestureunlock.vo.ConfigGestureVO;
import com.dds.gestureunlock.vo.ResultVerifyVO;

import framework.telegram.support.BaseActivity;
import framework.telegram.ui.R;

/**
 * File Description: 手势密码解锁认证Activity
 */
public abstract class GestureUnlockActivity extends BaseActivity {
    private static final String TAG = "dds_test";

    public static final int VERIFY_REQUEST_CODE = 0x00098;
    public static final int MODIFY_REQUEST_CODE = 0x00096;
    public static final int CREATE_REQUEST_CODE = 0x00094;

    public static final String KEY = "key";
    public static final String ICON = "icon";
    public static final String TYPE = "type";
    public static final String CLOSE = "canClose";

    private Fragment currentFragment;
    private GestureCreateFragment mGestureCreateFragment;
    private GestureVerifyFragment mGestureVerifyFragment;
    private String key;
    private String icon;
    private int type;
    private boolean canClose;

    private ImageButton backBtn;
    private TextView titleTextView;
    private TextView rightTextView;
    private View toolbar;
    private View line;

    public static final int TYPE_GESTURE_CREATE = 1;
    public static final int TYPE_GESTURE_VERIFY = 2;
    public static final int TYPE_GESTURE_MODIFY = 3;

    private ConfigGestureVO mVerifyConfigGestureVO = ConfigGestureVO.defaultConfig();
    private ConfigGestureVO mModifyConfigGestureVO = ConfigGestureVO.defaultConfig();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gesture_unlock);
        initView();
        initVar();
        initListener();
    }

    private void initView() {
        toolbar = findViewById(R.id.toolbar);
        line  = findViewById(R.id.line);
        backBtn = findViewById(R.id.btn_go_back);
        titleTextView = findViewById(R.id.tv_toolbar_title);
        rightTextView = findViewById(R.id.tv_toolbar_right_button);
    }

    private void initVar() {
        Intent intent = getIntent();
        key = intent.getStringExtra(KEY);
        icon = intent.getStringExtra(ICON);
        type = intent.getIntExtra(TYPE, 2);
        canClose = intent.getBooleanExtra(CLOSE, false);
        if (TextUtils.isEmpty(key)) {
            //无效操作，退出
            finish();
        } else {
            if (type == TYPE_GESTURE_CREATE) {
                //初始化手势密码
                showCreateGestureLayout(true);
            } else if (type == TYPE_GESTURE_VERIFY) {
                //手势密码认证
                showVerifyGestureLayout();
            } else if (type == TYPE_GESTURE_MODIFY) {
                // 修改手势密码
                showModifyGestureLayout();
            } else {
                //无效操作，退出
                finish();
            }
        }
    }

    private void initListener() {
        backBtn.setOnClickListener(v -> finish());
        rightTextView.setOnClickListener(v -> GestureUnlockActivity.this.onChangeAccount());
    }

    /**
     * 显示初始化手势密码的布局
     */
    private void showCreateGestureLayout(boolean showChangeLock) {
        titleTextView.setText(getString(R.string.gesture_set_pwd));
        toolbar.setVisibility(View.VISIBLE);
        rightTextView.setVisibility(View.GONE);
        line.setVisibility(View.VISIBLE);
        toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.f8f8f8));

        if (mGestureCreateFragment == null) {
            mGestureCreateFragment = GestureCreateFragment.newInstance(showChangeLock);
            mGestureCreateFragment.setGestureCreateListener(new GestureCreateFragment.GestureCreateListener() {

                @Override
                public void onCreateFinished(String gestureCode) {
                    // 创建手势密码完成
                    GestureUnlock.getInstance().setGestureCode(GestureUnlockActivity.this, key, gestureCode);
                    setResult(Activity.RESULT_OK);
                    finish();
                }

                @Override
                public void onEventOccur(int eventCode) {

                }

                @Override
                public void onChangeOtherLock() {
                    GestureUnlockActivity.this.onChangeOtherLock();
                }
            });
        }
        mGestureCreateFragment.setData(ConfigGestureVO.defaultConfig());
        safeAddFragment(mGestureCreateFragment, R.id.fragment_container, "GestureCreateFragment");
    }

    /**
     * 显示验证手势密码的布局
     */
    private void showVerifyGestureLayout() {
        if (canClose) {
            titleTextView.setText(getString(R.string.plugin_uexGestureUnlock_verificationBeginPrompt));
            titleTextView.setVisibility(View.VISIBLE);
            backBtn.setVisibility(View.VISIBLE);
            rightTextView.setVisibility(View.GONE);
            line.setVisibility(View.VISIBLE);
            toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.f8f8f8));

        } else {
            titleTextView.setVisibility(View.GONE);
            backBtn.setVisibility(View.GONE);
            rightTextView.setVisibility(View.VISIBLE);

            line.setVisibility(View.GONE);
            toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.white));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.BLACK);
        }
        if (mGestureVerifyFragment == null) {
            mGestureVerifyFragment = GestureVerifyFragment.newInstance(!canClose);
            mGestureVerifyFragment.setGestureVerifyListener(new GestureVerifyFragment.GestureVerifyListener() {

                @Override
                public void onVerifyResult(ResultVerifyVO result, int leftCount) {
                    if (result.isFinished()) {
                        //验证成功
                        GestureUnlock.getInstance().setUnlockErrorTotalCount(GestureUnlockActivity.this, key + "_verify", 0);
                        Log.d(TAG, "onVerifyResult:验证成功");
                        if (canClose) {
                            GestureUnlock.getInstance().setUnlockErrorCount(GestureUnlockActivity.this, key + "_modify", mVerifyConfigGestureVO.getMaximumAllowTrialTimes());
                        } else {
                            GestureUnlock.getInstance().setUnlockErrorCount(GestureUnlockActivity.this, key + "_verify", mVerifyConfigGestureVO.getMaximumAllowTrialTimes());
                        }
                        onUnlockSuccess();
                        setResult(Activity.RESULT_OK);
                    } else {
                        Log.d(TAG, "onVerifyResult:验证失败");
                        int totalCount = GestureUnlock.getInstance().getUnlockErrorTotalCount(GestureUnlockActivity.this, key + "_verify") + 1;
                        GestureUnlock.getInstance().setUnlockErrorTotalCount(GestureUnlockActivity.this, key + "_verify", totalCount);

                        if (canClose) {
                            GestureUnlock.getInstance().setUnlockErrorCount(GestureUnlockActivity.this, key + "_modify", leftCount);
                        } else {
                            GestureUnlock.getInstance().setUnlockErrorCount(GestureUnlockActivity.this, key + "_verify", leftCount);
                        }
                    }
                }

                @Override
                public void closeLayout() {
                    finish();
                }

                @Override
                public void onEventOccur(int eventCode) {
                    Log.d(TAG, "onEventOccur:" + eventCode);
                }

                @Override
                public void onErrorMax() {
                    Log.d(TAG, "onErrorMax--->");
                    if (canClose) {
                        GestureUnlock.getInstance().setUnlockErrorMax(GestureUnlockActivity.this, key + "_modify");
                    } else {
                        GestureUnlock.getInstance().setUnlockErrorMax(GestureUnlockActivity.this, key + "_verify");
                    }
                }

                @Override
                public void onForgetPwd() {
                    GestureUnlockActivity.this.onForgetPwd();
                }

                @Override
                public void onDisableAccount() {
                    GestureUnlockActivity.this.onDisableAccount();
                }
            });
        }

        mVerifyConfigGestureVO.setTotalErrorCount(GestureUnlock.getInstance().getUnlockErrorTotalCount(GestureUnlockActivity.this, key + "_verify"));
        mVerifyConfigGestureVO.setDisableAccountIsOpen(isDisableAccountIsOpen());
        mVerifyConfigGestureVO.setIconImage(icon);
        if (canClose) {
            mVerifyConfigGestureVO.setCurrentAllowTrialTimes(GestureUnlock.getInstance().getUnlockErrorCount(this, key + "_modify"));
        } else {
            mVerifyConfigGestureVO.setCurrentAllowTrialTimes(GestureUnlock.getInstance().getUnlockErrorCount(this, key + "_verify"));
        }
        mGestureVerifyFragment.setData(mVerifyConfigGestureVO);
        safeAddFragment(mGestureVerifyFragment, R.id.fragment_container, "GestureVerifyFragment");

        if (canClose) {
            long errorMax = GestureUnlock.getInstance().getUnlockErrorMax(this, key + "_modify");
            mGestureVerifyFragment.setGestureCodeData(GestureUnlock.getInstance().getGestureCodeSet(this, key), errorMax);
        } else {
            long errorMax = GestureUnlock.getInstance().getUnlockErrorMax(this, key + "_verify");
            mGestureVerifyFragment.setGestureCodeData(GestureUnlock.getInstance().getGestureCodeSet(this, key), errorMax);
        }
    }


    private void showModifyGestureLayout() {
        titleTextView.setText(getString(R.string.gesture_input_old_pwd));
        toolbar.setVisibility(View.VISIBLE);
        rightTextView.setVisibility(View.GONE);

        line.setVisibility(View.VISIBLE);
        toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.f8f8f8));

        if (mGestureVerifyFragment == null) {
            mGestureVerifyFragment = new GestureVerifyFragment();
            mGestureVerifyFragment.setGestureVerifyListener(new GestureVerifyFragment.GestureVerifyListener() {

                @Override
                public void onVerifyResult(ResultVerifyVO result, int leftCount) {
                    if (result.isFinished()) {
                        //验证成功
                        Log.d(TAG, "onVerifyResult:验证成功");
                        GestureUnlock.getInstance().setUnlockErrorCount(GestureUnlockActivity.this, key + "_modify", mVerifyConfigGestureVO.getMaximumAllowTrialTimes());
                        titleTextView.setText(R.string.gesture_set_new_pwd);
                        showCreateGestureLayout(false);
                    } else {
                        Log.d(TAG, "onVerifyResult:验证失败");
                        GestureUnlock.getInstance().setUnlockErrorCount(GestureUnlockActivity.this, key + "_modify", leftCount);
                    }
                }

                @Override
                public void closeLayout() {

                }

                @Override
                public void onEventOccur(int eventCode) {
                    Log.d(TAG, "onEventOccur:" + eventCode);
                }

                @Override
                public void onErrorMax() {
                    Log.d(TAG, "onErrorMax--->");
                    GestureUnlock.getInstance().setUnlockErrorMax(GestureUnlockActivity.this, key + "_modify");
                }

                @Override
                public void onForgetPwd() {
                    GestureUnlockActivity.this.onForgetPwd();
                }

                @Override
                public void onDisableAccount() {

                }
            });
        }

        mModifyConfigGestureVO.setCurrentAllowTrialTimes(GestureUnlock.getInstance().getUnlockErrorCount(this, key + "_modify"));
        mGestureVerifyFragment.setData(mModifyConfigGestureVO);
        safeAddFragment(mGestureVerifyFragment, R.id.fragment_container, "GestureVerifyFragment");
        mGestureVerifyFragment.setGestureCodeData(GestureUnlock.getInstance().getGestureCodeSet(this, key), GestureUnlock.getInstance().getUnlockErrorMax(this, key + "_modify"));
    }

    protected abstract void onForgetPwd();

    protected abstract void onChangeAccount();

    protected abstract void onUnlockSuccess();

    protected abstract void onChangeOtherLock();

    protected abstract void onDisableAccount();

    protected abstract boolean isDisableAccountIsOpen();

    /**
     * 检查fragment是否已经加入，防止重复
     */
    private void safeAddFragment(Fragment fragment, int id, String tag) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        //优先检查，fragment是否存在，避免重叠
        Fragment tempFragment = fragmentManager.findFragmentByTag(tag);
        if (tempFragment != null) {
            fragment = tempFragment;
        }
        if (fragment.isAdded()) {
            addOrShowFragment(fragmentTransaction, fragment, id, tag);
        } else {
            if (currentFragment != null && currentFragment.isAdded()) {
                fragmentTransaction.hide(currentFragment).add(id, fragment, tag).commit();
            } else {
                fragmentTransaction.add(id, fragment, tag).commit();
            }
            currentFragment = fragment;
        }
    }

    /**
     * 添加或者直接显示
     *
     * @param transaction
     * @param fragment
     * @param containerLayoutId
     * @param tag
     */
    private void addOrShowFragment(FragmentTransaction transaction, Fragment fragment, int containerLayoutId, String tag) {
        if (currentFragment == fragment)
            return;
        if (!fragment.isAdded()) { // 如果当前fragment未被添加，则添加到Fragment管理器中
            transaction.hide(currentFragment).add(containerLayoutId, fragment, tag).commit();
        } else {
            transaction.hide(currentFragment).show(fragment).commit();
        }
        currentFragment.setUserVisibleHint(false);
        currentFragment = fragment;
        currentFragment.setUserVisibleHint(true);
    }

    @Override
    public void onBackPressed() {
        if (type != TYPE_GESTURE_VERIFY || canClose) {
            super.onBackPressed();
        }
    }
}
