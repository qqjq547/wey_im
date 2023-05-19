package framework.telegram.message.ui.group

import com.chad.library.adapter.base.entity.MultiItemEntity
import framework.ideas.common.model.im.MessageModel
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView
import io.realm.RealmResults

interface MessageDetailContract {

    interface Presenter : BasePresenter {

        fun loadModelData(msgLocalId: Long)

        fun destory()
    }

    interface View : BaseView<Presenter> {

        fun loadPageSuccess(list: MutableList<MultiItemEntity>)

        fun loadPageFail(str: String, hasData: Boolean)
    }
}