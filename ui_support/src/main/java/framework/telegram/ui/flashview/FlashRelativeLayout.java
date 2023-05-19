package framework.telegram.ui.flashview;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

public class FlashRelativeLayout extends RelativeLayout {

    public FlashRelativeLayout(Context context) {
        this(context, null);
    }

    public FlashRelativeLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlashRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void flash(int width,int height) {
        final View backgroundView = new View(getContext());
        backgroundView.setLayoutParams(new LayoutParams(width, height));
        backgroundView.setBackgroundColor(Color.parseColor("#0a1d47"));
        backgroundView.setAlpha(0.0f);
        addView(backgroundView);

        postDelayed(() -> {
            ObjectAnimator animator = ObjectAnimator.ofFloat(backgroundView, "alpha", 0.0f, 0.3f);
            animator.setDuration(200);
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    postDelayed(() -> {
                        ObjectAnimator animator1 = ObjectAnimator.ofFloat(backgroundView, "alpha", 0.3f, 0.0f);
                        animator1.setDuration(200);
                        animator.addListener(new Animator.AnimatorListener() {

                            @Override
                            public void onAnimationStart(Animator animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                removeView(backgroundView);
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {

                            }

                            @Override
                            public void onAnimationRepeat(Animator animation) {

                            }
                        });
                        animator1.start();
                    }, 500);
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            animator.start();
        }, 200);
    }
}
