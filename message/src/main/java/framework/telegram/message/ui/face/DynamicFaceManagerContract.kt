package framework.telegram.message.ui.face

import framework.ideas.common.model.contacts.ContactDataModel
import framework.ideas.common.model.group.GroupInfoModel
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView
import framework.telegram.ui.face.dynamic.DynamicFaceBean

interface DynamicFaceManagerContract {

    interface Presenter : BasePresenter {
        fun loadData(postEvent: Boolean = true)

        fun addEmoticon(face: DynamicFaceBean)

        fun delEmoticons(faces: List<DynamicFaceBean>)
    }

    interface View : BaseView<Presenter> {
        //显示加载中
        fun showLoading()

        //刷新界面
        fun refreshListUI(list: List<DynamicFaceBean>)

        //显示加载中
        fun showEmpty()

        //显示错误界面
        fun showError(errStr: String?)

        fun dismissLoading()

        fun showAddEmoticonComplete()
    }
}