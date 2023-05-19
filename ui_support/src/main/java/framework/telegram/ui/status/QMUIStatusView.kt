package framework.telegram.ui.status

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import framework.telegram.ui.R
import framework.telegram.ui.widget.QMUIEmptyViewNew
import java.lang.ref.WeakReference

class QMUIStatusView{
    private lateinit var mWeakReference: WeakReference<Context>
    private var mView: View ? = null
    private var mParentView: View ? = null

    constructor(activity: AppCompatActivity){
        mParentView = (activity.findViewById(android.R.id.content) as ViewGroup).getChildAt(0)
        if (mParentView != null)
            mWeakReference = WeakReference(mParentView!!.context)
    }

    constructor(fragment: Fragment){
        if (fragment.context != null) {
            mParentView = fragment.view
            mWeakReference = WeakReference(fragment.context!!)
        }
        else
            return
    }

    constructor(fragment: Fragment,@IdRes parentIdRes: Int){
        if (fragment.context != null && fragment.activity != null) {
            mParentView = fragment.activity!!.findViewById(parentIdRes)
            mWeakReference = WeakReference(fragment.context!!)
        }
        else
            return
    }


    fun showLoadingView(){
        mWeakReference.get()?.let {
            dismiss()
            mView = QMUIViewBuilder(QMUIViewBuilder.TYPE.LOADING_VIEW)
                    .setContext(mParentView?.context?.getString(R.string.loading)?:"")
                    .build(it)
            (mParentView!!.parent as ViewGroup).addView(mView)
//            (mView as QMUILoadingView).start()
            (mView as QMUIEmptyViewNew).show()
        }
    }

    fun showEmptyView(btnText: String,rid:Int=0){
        mWeakReference.get()?.let {
            dismiss()
            mView = QMUIViewBuilder(QMUIViewBuilder.TYPE.EMPTY_VIEW, btnText)
                    .setEmptyImage(rid).build(it)
            (mParentView!!.parent as ViewGroup).addView(mView)
            (mView as QMUIEmptyViewNew).show()
        }

    }

    fun showEmptyView(rid:Int=0){
        mWeakReference.get()?.let {
            dismiss()
            mView = QMUIViewBuilder(QMUIViewBuilder.TYPE.EMPTY_VIEW, mParentView?.context?.getString(R.string.no_data)?:"")
                    .setEmptyImage(rid).build(it)
            (mParentView!!.parent as ViewGroup).addView(mView)
            (mView as QMUIEmptyViewNew).show()
        }

    }

    fun showErrorView(clickListener: View.OnClickListener){
        mWeakReference.get()?.let {
            dismiss()
            mView = QMUIViewBuilder(QMUIViewBuilder.TYPE.ERROR_VIEW, mParentView?.context?.getString(R.string.tautology)?:"", clickListener)
                    .build(it)
            (mParentView!!.parent as ViewGroup).addView(mView)
            (mView as QMUIEmptyViewNew).show()
        }

    }

    fun showErrorView(btnText: String){
        mWeakReference.get()?.let {
            dismiss()
            mView = QMUIViewBuilder(QMUIViewBuilder.TYPE.ERROR_VIEW, btnText, null)
                    .build(it)
            (mParentView!!.parent as ViewGroup).addView(mView)
            (mView as QMUIEmptyViewNew).show()
        }

    }

    fun showErrorView(){
        mWeakReference.get()?.let {
            dismiss()
            mView = QMUIViewBuilder(QMUIViewBuilder.TYPE.ERROR_VIEW, mParentView?.context?.getString(R.string.fail_to_load)?:"", null)
                    .build(it)
            (mParentView!!.parent as ViewGroup).addView(mView)
            (mView as QMUIEmptyViewNew).show()
        }
       
    }

    fun dismiss(){
        if (mParentView != null && mView != null) {
            (mParentView!!.parent as ViewGroup).removeView(mView)
            mView = null
        }
    }
}