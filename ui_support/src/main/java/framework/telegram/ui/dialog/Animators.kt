package framework.telegram.ui.dialog

import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import android.widget.ProgressBar

/**
 * Created by lzh on 19-6-24.
 * INFO:
 */
object Animators {

    fun makeDeterminateCircularPrimaryProgressAnimator(
            progressBar: ProgressBar): ValueAnimator {
        val animator = ValueAnimator.ofInt(0, 150)
        animator.duration = 6000
        animator.interpolator = LinearInterpolator()
        animator.repeatCount = ValueAnimator.INFINITE
        animator.addUpdateListener { animator ->
            val value = animator.animatedValue as Int
            progressBar.progress = value

        }
        return animator
    }

}
