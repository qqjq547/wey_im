package framework.telegram.ui.text;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import framework.telegram.ui.R;
import framework.telegram.ui.utils.ScreenUtils;

/**
 * Created by yanggl on 2019/9/18 16:04
 * 在edittext上增加，清空，显示隐藏按钮，发送验证码按钮
 * 还有点击发送验证码监听和一个简化的输入监听
 */
public class EasyEditText extends LinearLayout {

    private EditText mEt;
    private ImageView mIvDelete;
    private ImageView mIvSee;
    private CountdownButton mBtnCountdown;

    private boolean mShowIvDelete;
    private boolean mShowIvSee;
    private boolean mShowBtnCountdown;

    private EasyOnTextChange mEasyOnTextChange;
    private EasyOnSendVerifyCode mEasyOnSendVerifyCode;

    private boolean mShowText = true;

    private TextWatcher mTextWatcher;

    public EasyEditText(Context context) {
        this(context, null);
    }

    public EasyEditText(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EasyEditText(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.setOrientation(LinearLayout.HORIZONTAL);
        this.setPadding(ScreenUtils.dp2px(getContext(),16),0,ScreenUtils.dp2px(getContext(),16),0);
        this.setBackgroundResource(R.drawable.common_corners_trans_edeff2_6_0);
        LayoutInflater.from(context).inflate(R.layout.layout_easy_edittext, this,true);
        mEt = findViewById(R.id.et);
        mIvDelete = findViewById(R.id.iv_delete);
        mIvSee = findViewById(R.id.iv_see);
        mBtnCountdown = findViewById(R.id.btn_count_down);
    }

    /**
     * @param showDelete           是否显示清空按钮
     * @param showSee              是否显示 显示隐藏文字按钮，显示这个按钮时默认隐藏文字
     * @param showCountdown        是否显示发送验证码按钮
     * @param easyOnSendVerifyCode 用户点击发送验证码按钮回调，仅在可点击时点击会回调，返回值为是否将发送按钮设置为发送状态
     * @param easyOnTextChange     etittext输入文字时的回调
     * 这个方法可以在代码中实时调用，以切换到不同状态
     * 调用时会清空上一次数据
     */
    public void initEasyEditText(boolean showDelete,
                                 boolean showSee,
                                 boolean showCountdown,
                                 EasyOnSendVerifyCode easyOnSendVerifyCode,
                                 EasyOnTextChange easyOnTextChange) {
        mShowIvDelete = showDelete;
        mShowIvSee = showSee;
        mShowBtnCountdown = showCountdown;
        mEasyOnTextChange = easyOnTextChange;
        mEasyOnSendVerifyCode = easyOnSendVerifyCode;

        mIvDelete.setVisibility(showDelete ? View.VISIBLE : View.GONE);
        mIvSee.setVisibility(showSee ? View.VISIBLE : View.GONE);
        mBtnCountdown.setVisibility(showCountdown ? View.VISIBLE : View.GONE);

        mEt.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                boolean show = !TextUtils.isEmpty(mEt.getText().toString()) && hasFocus;
                showHideDeleteSee(show);
            }
        });

        if (mTextWatcher != null) {
            //避免设置多个TextWatcher
            mEt.removeTextChangedListener(mTextWatcher);
        }
        mTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String str = s.toString();
                if (str == null) {
                    str = "";
                }
                showHideDeleteSee(!TextUtils.isEmpty(str));
                if (mEasyOnTextChange != null) {
                    mEasyOnTextChange.onTextChange(str);
                }
            }
        };
        mEt.addTextChangedListener(mTextWatcher);

        mIvDelete.setOnClickListener(v -> mEt.setText(""));

        mIvSee.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mShowText = !mShowText;
                showHideText();
            }
        });

        mBtnCountdown.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEasyOnSendVerifyCode != null && mEasyOnSendVerifyCode.onSendVerifyCode()) {
//                    mBtnCountdown.sendVerifyCode();//不倒数
                }
            }
        });

        clear();
    }

    private void showHideDeleteSee(boolean b) {
        if (b) {
            if (mShowIvDelete) {
                mIvDelete.setVisibility(View.VISIBLE);
            }
            if (mShowIvSee) {
                mIvSee.setVisibility(View.VISIBLE);
            }
        } else {
            if (mShowIvDelete) {
                mIvDelete.setVisibility(View.GONE);
            }
            if (mShowIvSee) {
                mIvSee.setVisibility(View.GONE);
            }
        }
    }

    private void showHideText() {
        if (mShowText) {
            mIvSee.setImageResource(R.drawable.common_password_visible);
            mEt.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
        } else {
            mIvSee.setImageResource(R.drawable.common_password_invisible);
            mEt.setTransformationMethod(PasswordTransformationMethod.getInstance());//设置密码不可见
        }
    }

    public EditText getEt() {
        return mEt;
    }

    public void clear() {
        mEt.setText("");
        mShowText = !mShowIvSee;
        showHideText();
    }

    public interface EasyOnTextChange {
        void onTextChange(String text);
    }

    public interface EasyOnSendVerifyCode {
        boolean onSendVerifyCode();
    }

    public CountdownButton getCountDown(){
        return mBtnCountdown;
    }
}
