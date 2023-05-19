package framework.telegram.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.animation.AnimationUtils
import android.widget.ImageView
import framework.telegram.ui.R

class CommonLoadindView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : ImageView(context, attrs, defStyleAttr) {

    init {
        this.setBackgroundResource(R.drawable.icon_md_loading)
        val animation = AnimationUtils.loadAnimation(this.context, R.anim.anim_rotate)
        this.startAnimation(animation)

    }

}