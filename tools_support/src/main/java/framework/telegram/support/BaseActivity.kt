package framework.telegram.support

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.annotation.CallSuper
import androidx.annotation.CheckResult
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.trello.rxlifecycle3.LifecycleProvider
import com.trello.rxlifecycle3.LifecycleTransformer
import com.trello.rxlifecycle3.RxLifecycle
import com.trello.rxlifecycle3.android.ActivityEvent
import com.trello.rxlifecycle3.android.RxLifecycleAndroid
import com.umeng.analytics.MobclickAgent
import framework.telegram.support.tools.language.LocalManageUtil
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

abstract class BaseActivity : AppCompatActivity(), LifecycleProvider<ActivityEvent> {

    private val lifecycleSubject = BehaviorSubject.create<ActivityEvent>()

    @androidx.annotation.NonNull
    @CheckResult
    override fun lifecycle(): Observable<ActivityEvent> {
        return lifecycleSubject.hide()
    }

    @androidx.annotation.NonNull
    @CheckResult
    override fun <T> bindUntilEvent(@androidx.annotation.NonNull event: ActivityEvent): LifecycleTransformer<T> {
        return RxLifecycle.bindUntilEvent(lifecycleSubject, event)
    }

    @androidx.annotation.NonNull
    @CheckResult
    override fun <T> bindToLifecycle(): LifecycleTransformer<T> {
        return RxLifecycleAndroid.bindActivity(lifecycleSubject)
    }

    @CallSuper
    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleSubject.onNext(ActivityEvent.CREATE)
        if (isPortraitScreen()){
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        ActivityManager.addActivity(this)
        StatusBarUtil.justMDarkMode(this)
        StatusBarUtil.immersive(this)
    }

    @CallSuper
    override fun onStart() {
        super.onStart()
        lifecycleSubject.onNext(ActivityEvent.START)
    }

    @CallSuper
    override fun onResume() {
        super.onResume()
        lifecycleSubject.onNext(ActivityEvent.RESUME)
        MobclickAgent.onResume(this)
    }

    @CallSuper
    override fun onPause() {
        lifecycleSubject.onNext(ActivityEvent.PAUSE)
        super.onPause()
        MobclickAgent.onPause(this)
    }

    @CallSuper
    override fun onStop() {
        lifecycleSubject.onNext(ActivityEvent.STOP)
        super.onStop()
    }

    @CallSuper
    override fun onDestroy() {
        lifecycleSubject.onNext(ActivityEvent.DESTROY)
        ActivityManager.removeActivity(this)
        super.onDestroy()
    }

    open fun isPortraitScreen():Boolean = true

    override fun attachBaseContext(newBase: Context) {
        val context = languageWork(newBase)
        super.attachBaseContext(context)
    }

    private fun languageWork(context: Context): Context {
        // 8.0及以上使用createConfigurationContext设置configuration
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            updateResources(context)
        } else {
            context
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun updateResources(context: Context): Context {
        val locale = LocalManageUtil.getSetLanguageLocale() ?: return context
        val configuration = context.resources.configuration
        configuration.setLocale(locale)
        configuration.setLocales(LocaleList(locale))
        return context.createConfigurationContext(configuration)
    }
}
