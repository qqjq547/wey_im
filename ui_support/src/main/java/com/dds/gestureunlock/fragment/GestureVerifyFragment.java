package com.dds.gestureunlock.fragment;

import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dds.gestureunlock.GestureUnlock;
import com.dds.gestureunlock.JsConst;
import com.dds.gestureunlock.util.GestureUtil;
import com.dds.gestureunlock.util.ResourceUtil;
import com.dds.gestureunlock.vo.ConfigGestureVO;
import com.dds.gestureunlock.vo.ResultFailedVO;
import com.dds.gestureunlock.vo.ResultVerifyVO;
import com.dds.gestureunlock.widget.GestureContentView;
import com.dds.gestureunlock.widget.GestureDrawLine;

import framework.telegram.ui.image.AppImageView;

public class GestureVerifyFragment extends GestureBaseFragment {

    public static GestureVerifyFragment newInstance(boolean visibleForgetPwd) {
        Bundle args = new Bundle();
        args.putBoolean("visibleForgetPwd", visibleForgetPwd);
        GestureVerifyFragment fragment = new GestureVerifyFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private RelativeLayout mBg;
    private AppImageView mImgUserLogo;
    private TextView mTextTip;
    private GestureContentView mGestureContentView;
    private GestureVerifyListener mListener;

    private String mGestureCode;
    private long mErrorMaxTime;
    private ConfigGestureVO mData;
    private int mLeftCount;
    private int mTotalErrorCount;

    /**
     * 手势密码验证结果监听回调接口
     */
    public interface GestureVerifyListener {
        void onVerifyResult(ResultVerifyVO result, int leftCount);

        void closeLayout();

        void onEventOccur(int eventCode);

        void onErrorMax();

        void onForgetPwd();

        void onDisableAccount();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(ResourceUtil.getResLayoutID("plugin_uexgestureunlock_gesture_verify"), container, false);
        mBg = view.findViewById(ResourceUtil.getResIdID("plugin_uexGestureUnlock_bg"));
        mImgUserLogo = view.findViewById(ResourceUtil.getResIdID("plugin_uexGestureUnlock_user_logo"));
        mTextTip = view.findViewById(ResourceUtil.getResIdID("plugin_uexGestureUnlock_text_tip"));
        TextView textForgetPwd = view.findViewById(ResourceUtil.getResIdID("plugin_uexGestureUnlock_forget_pwd_button"));
        FrameLayout gestureContainer = view.findViewById(ResourceUtil.getResIdID("plugin_uexGestureUnlock_gesture_container"));

        if (mData != null) {
            setUpData();
        }

        if (getArguments() != null && getArguments().getBoolean("visibleForgetPwd") && !mData.isDisableAccountIsOpen()) {
            textForgetPwd.setVisibility(View.VISIBLE);
            textForgetPwd.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onForgetPwd();
                }
            });
        } else {
            textForgetPwd.setVisibility(View.GONE);
        }

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.topMargin = GestureUtil.getScreenDisplay(this.getActivity())[0] / 8;
        layoutParams.leftMargin = GestureUtil.getScreenDisplay(this.getActivity())[0] / 8;
        layoutParams.addRule(RelativeLayout.BELOW, ResourceUtil.getResIdID("plugin_uexGestureUnlock_gesture_tip_layout"));
        gestureContainer.setLayoutParams(layoutParams);
        setParentViewFrameLayout(gestureContainer);
        // 初始化一个显示各个点的viewGroup
        mGestureContentView = new GestureContentView(this.getActivity(), true, mGestureCode, mErrorMaxTime, mGestureCallBack, mDrawArrowListener, mData);
        // 设置手势解锁显示到哪个布局里面
        mGestureContentView.setParentView(gestureContainer);

        if (mListener != null) {
            mListener.onEventOccur(JsConst.EVENT_PLUGIN_INIT);
            mListener.onEventOccur(JsConst.EVENT_START_VERIFY);
        }

        return view;
    }

    private void setTextTipError(String tips) {
        mTextTip.setTextColor(mData.getErrorThemeColor());
        mTextTip.setText(tips);
    }

    private void setTextTipNormal(String tips) {
        mTextTip.setTextColor(mData.getNormalTextThemeColor());
        mTextTip.setText(tips);
    }

    private void setUpData() {
        mLeftCount = mData.getCurrentAllowTrialTimes();
        mTotalErrorCount = mData.getTotalErrorCount();

        if (!TextUtils.isEmpty(mData.getBackgroundImage())) {
            mBg.setBackgroundDrawable(new BitmapDrawable(ResourceUtil.getLocalImg(this.getActivity(), mData.getBackgroundImage())));
        } else {
            mBg.setBackgroundColor(mData.getBackgroundColor());
        }

        if (!TextUtils.isEmpty(mData.getIconImage())) {
            mImgUserLogo.setVisibility(View.VISIBLE);
            mImgUserLogo.setImageURI(mData.getIconImage());
        } else {
            mImgUserLogo.setVisibility(View.VISIBLE);
            mImgUserLogo.setImageURI(Uri.EMPTY);
        }

        setTextTipNormal(mData.getVerificationBeginPrompt());
    }

    public void setGestureVerifyListener(GestureVerifyListener listener) {
        this.mListener = listener;
    }

    public void setGestureCodeData(String code, long errorMaxTime) {
        this.mGestureCode = code;
        this.mErrorMaxTime = errorMaxTime;
    }

    public void setData(ConfigGestureVO data) {
        if (data == null) {
            mData = new ConfigGestureVO();
        } else {
            this.mData = data;
        }
        super.setData(mData);
    }

    private GestureDrawLine.GestureCallBack mGestureCallBack = new GestureDrawLine.GestureCallBack() {

        @Override
        public void onGestureCodeInput(String inputCode) {

        }

        @Override
        public void checkedSuccess() {
            if (mData.isDisableAccountIsOpen() && mTotalErrorCount >= 20) {
                mListener.onDisableAccount();
            } else {
                mGestureContentView.clearDrawLineState(mData.getSuccessRemainInterval(), false);
                setTextTipNormal(mData.getVerificationSucceedPrompt());
                mGestureContentView.postDelayed(() -> {
                    if (mListener != null) {
                        ResultVerifyVO resultVerifyVO = new ResultVerifyVO();
                        resultVerifyVO.setIsFinished(true);
                        mListener.onVerifyResult(resultVerifyVO, 5);
                        mListener.onEventOccur(JsConst.EVENT_VERIFY_SUCCESS);
                        mListener.closeLayout();
                    }
                }, mData.getSuccessRemainInterval());
            }
        }

        @Override
        public void checkedFail() {
            if (mListener != null) {
                mListener.onEventOccur(JsConst.EVENT_VERIFY_ERROR);
            }

            mGestureContentView.clearDrawLineState(mData.getMinimumCodeLength(), true);
            // 左右移动动画
            Animation shakeAnimation = AnimationUtils.loadAnimation(GestureVerifyFragment.this.getActivity(), ResourceUtil.getResAnimID("plugin_uexgestureunlock_shake"));
            mTextTip.startAnimation(shakeAnimation);

            mTotalErrorCount++;
            if (mData.isDisableAccountIsOpen() && mTotalErrorCount >= 20) {
                mListener.onDisableAccount();
            } else {
                mLeftCount--;
                if (mData.isDisableAccountIsOpen() && mTotalErrorCount == 19) {
                    String tips = mData.getVerificationErrorDisableAccountWarn();
                    setTextTipError(tips);

                    if (mListener != null) {
                        ResultFailedVO result = new ResultFailedVO();
                        result.setIsFinished(false);
                        result.setErrorCode(JsConst.ERROR_CODE_TOO_MANY_TRY);
                        result.setErrorString(ResourceUtil.getString("plugin_uexGestureUnlock_errorCodeTooManyTry"));
                        mListener.onVerifyResult(result, mLeftCount);
                    }
                } else {
                    if (mLeftCount == 0) {
                        String tips = String.format(mData.getVerificationErrorMaxPrompt(), 5);
                        setTextTipError(tips);

                        // 还原设置
                        mLeftCount = mData.getMaximumAllowTrialTimes();
                        mErrorMaxTime = System.currentTimeMillis();
                        mGestureContentView.setErrorMaxTime(mErrorMaxTime);

                        if (mListener != null) {
                            ResultFailedVO result = new ResultFailedVO();
                            result.setIsFinished(false);
                            result.setErrorCode(JsConst.ERROR_CODE_TOO_MANY_TRY);
                            result.setErrorString(ResourceUtil.getString("plugin_uexGestureUnlock_errorCodeTooManyTry"));
                            mListener.onVerifyResult(result, mLeftCount);
                            mListener.onErrorMax();
                        }
                    } else {
                        String tips = String.format(mData.getVerificationErrorPrompt(), mLeftCount);
                        setTextTipError(tips);

                        if (mListener != null) {
                            ResultFailedVO result = new ResultFailedVO();
                            result.setIsFinished(false);
                            result.setErrorCode(JsConst.ERROR_CODE_TOO_MANY_TRY);
                            result.setErrorString(ResourceUtil.getString("plugin_uexGestureUnlock_errorCodeTooManyTry"));
                            mListener.onVerifyResult(result, mLeftCount);
                        }
                    }
                }
            }
        }

        @Override
        public void checkedErrorMax(long pastTime) {
            if (mListener != null) {
                mListener.onEventOccur(JsConst.EVENT_VERIFY_ERROR);
            }

            mGestureContentView.clearDrawLineState(mData.getMinimumCodeLength(), true);
            // 左右移动动画
            Animation shakeAnimation = AnimationUtils.loadAnimation(GestureVerifyFragment.this.getActivity(), ResourceUtil.getResAnimID("plugin_uexgestureunlock_shake"));
            mTextTip.startAnimation(shakeAnimation);

            String tips = String.format(mData.getVerificationErrorMaxPrompt(), 5 - (pastTime / 1000 / 60));
            setTextTipError(tips);

            if (mListener != null) {
                ResultFailedVO result = new ResultFailedVO();
                result.setIsFinished(false);
                result.setErrorCode(JsConst.ERROR_CODE_TOO_MANY_TRY);
                result.setErrorString(ResourceUtil
                        .getString("plugin_uexGestureUnlock_errorCodeTooManyTry"));
                mListener.onVerifyResult(result, 5);
            }
        }
    };
}
