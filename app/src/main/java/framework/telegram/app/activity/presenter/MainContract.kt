package framework.telegram.app.activity.presenter

import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

/**
 * Created by lzh on 19-5-17.
 * INFO:
 */
interface MainContract {

    interface Presenter : BasePresenter {

        fun savePerfectInfo(vibration: Boolean, sound: Boolean)

        fun uploadOpenInstallData(params: String, type: Int,channelCode:String)

        fun autoLoginForResult()

    }

    interface View : BaseView<Presenter> {

    }
}