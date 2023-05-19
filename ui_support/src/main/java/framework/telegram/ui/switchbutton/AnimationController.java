package framework.telegram.ui.switchbutton;


import android.os.Handler;
import android.os.Message;

class AnimationController {
    private static int ANI_WHAT = 256;
    private static int DEFAULT_VELOCITY = 7;
    private static int DEFAULT_FRAME_DURATION = 16;
    private AnimationController.AnimationHandler mHandler;
    private AnimationController.OnAnimateListener mOnAnimateListener;
    private boolean isAnimating = false;
    private int mFrame;
    private int mFrom;
    private int mTo;
    private int mVelocity;

    private AnimationController() {
        this.mVelocity = DEFAULT_VELOCITY;
        this.mHandler = new AnimationController.AnimationHandler();
    }

    static AnimationController getDefault() {
        AnimationController ac = new AnimationController();
        return ac;
    }

    AnimationController init(AnimationController.OnAnimateListener onAnimateListener) {
        if (onAnimateListener == null) {
            throw new IllegalArgumentException("onAnimateListener can not be null");
        } else {
            this.mOnAnimateListener = onAnimateListener;
            return this;
        }
    }

    void startAnimation(int from, int to) {
        this.isAnimating = true;
        this.mFrom = from;
        this.mTo = to;
        this.mFrame = this.mVelocity;
        if (this.mTo > this.mFrom) {
            this.mFrame = Math.abs(this.mVelocity);
        } else {
            if (this.mTo >= this.mFrom) {
                this.isAnimating = false;
                this.mOnAnimateListener.onAnimateComplete();
                return;
            }

            this.mFrame = -Math.abs(this.mVelocity);
        }

        this.mOnAnimateListener.onAnimationStart();
        (new AnimationController.RequireNextFrame()).run();
    }

    void stopAnimation() {
        this.isAnimating = false;
    }

    public void setVelocity(int velocity) {
        if (velocity <= 0) {
            this.mVelocity = DEFAULT_VELOCITY;
        } else {
            this.mVelocity = velocity;
        }
    }

    interface OnAnimateListener {
        void onAnimationStart();

        boolean continueAnimating();

        void onFrameUpdate(int var1);

        void onAnimateComplete();
    }

    class RequireNextFrame implements Runnable {
        RequireNextFrame() {
        }

        public void run() {
            if (AnimationController.this.isAnimating) {
                this.calcNextFrame();
                AnimationController.this.mOnAnimateListener.onFrameUpdate(AnimationController.this.mFrame);
                if (AnimationController.this.mOnAnimateListener.continueAnimating()) {
                    this.requireNextFrame();
                } else {
                    AnimationController.this.stopAnimation();
                    AnimationController.this.mOnAnimateListener.onAnimateComplete();
                }
            }
        }

        private void calcNextFrame() {
        }

        private void requireNextFrame() {
            Message msg = AnimationController.this.mHandler.obtainMessage();
            msg.what = AnimationController.ANI_WHAT;
            msg.obj = this;
            AnimationController.this.mHandler.sendMessageDelayed(msg, (long)AnimationController.DEFAULT_FRAME_DURATION);
        }
    }

    private static class AnimationHandler extends Handler {
        private AnimationHandler() {
        }

        public void handleMessage(Message msg) {
            if (msg.what == AnimationController.ANI_WHAT && msg.obj != null) {
                ((Runnable)msg.obj).run();
            }

        }
    }
}
