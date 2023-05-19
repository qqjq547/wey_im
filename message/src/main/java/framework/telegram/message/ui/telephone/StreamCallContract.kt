package framework.telegram.message.ui.telephone

import com.trello.rxlifecycle3.android.ActivityEvent
import com.trello.rxlifecycle3.android.FragmentEvent
import framework.telegram.message.bridge.bean.StreamCallItem
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView
import io.reactivex.Observable

interface StreamCallContract {

    interface Presenter: BasePresenter{

        fun setFObservable(o: Observable<FragmentEvent>)
        fun setAObservable(o: Observable<ActivityEvent>)
        /**
         * 获取与某人联系记录
         * @param chaterId
         */
        fun getStreamCallHistoryData(chaterId: Long, isMissCall: Boolean, streamType: Int, date: Long)

        /**
         * 获取全部的联系记录
         */
        fun getStreamCallHistoryData()

        /**
         * 获取全部的未接联系记录
         */
        fun getStreamUnCallHistoryData()

        fun stop()
    }

    interface View: BaseView<Presenter>{

        fun update(list: List<StreamCallItem>)
    }

}