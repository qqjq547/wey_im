package framework.telegram.message.ui.media

import framework.ideas.common.model.im.MessageModel
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

interface MediaPresenter<T> {

    interface Presenter:BasePresenter {

        fun getFileFromGroup(targetId: Long)

        fun getFileFromPv(targetId: Long)

        fun getMediaFromGroup(targetId: Long)

        fun getMediaFromPv(targetId: Long)

    }


    interface View: BaseView<Presenter>{

        fun showData(list: MutableList<MessageModel>)

    }
}