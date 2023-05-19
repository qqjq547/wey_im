package framework.telegram.message.ui.face

import android.content.Context
import com.facebook.common.util.UriUtil
import com.im.domain.pb.UserProto
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.R
import framework.telegram.message.event.DynamicFaceUpdateEvent
import framework.telegram.message.http.HttpManager
import framework.telegram.message.http.creator.UserHttpReqCreator
import framework.telegram.message.http.getResult
import framework.telegram.message.http.getResultWithCache
import framework.telegram.message.http.protocol.UserHttpProtocol
import framework.telegram.message.manager.upload.UploadManager
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.cache.kotlin.applyCache
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.ui.face.dynamic.DynamicFaceBean
import io.reactivex.Observable

class DynamicFaceManagerPresenterImpl : DynamicFaceManagerContract.Presenter {
    private var mContext: Context?
    private var mView: DynamicFaceManagerContract.View
    private var mObservalbe: Observable<ActivityEvent>

    private val mMineUid by lazy { AccountManager.getLoginAccount(AccountInfo::class.java).getUserId() }

    constructor(view: DynamicFaceManagerContract.View, context: Context?, observable: Observable<ActivityEvent>) {
        this.mView = view
        this.mObservalbe = observable
        this.mContext = context
        view.setPresenter(this)
    }

    override fun start() {
        loadData()
    }

    override fun loadData(postEvent: Boolean) {
//        mView.showLoading()
        HttpManager.getStore(UserHttpProtocol::class.java)
                .getEmoticon(object : HttpReq<UserProto.GetEmoticonReq>() {
                    override fun getData(): UserProto.GetEmoticonReq {
                        return UserHttpReqCreator.getEmoticon()
                    }
                })
                .applyCache("${mMineUid}_dynamic_face_cache", framework.telegram.support.system.cache.stategy.CacheStrategy.firstRemote())
                .getResultWithCache(mObservalbe, {
                    val faces = mutableListOf<DynamicFaceBean>()
                    faces.add(DynamicFaceBean(-1, UriUtil.getUriForResourceId(R.drawable.ic_add_dynamic_face).toString(), 0, 0))

                    it.data.emoticonsList.forEach { emoticon ->
                        faces.add(DynamicFaceBean(emoticon.emoticonId, emoticon.emoticonUrl, emoticon.width, emoticon.height))
                    }

                    mView.refreshListUI(faces)

                    if(postEvent){
                        EventBus.publishEvent(DynamicFaceUpdateEvent())
                    }
                }, {
                    mView.showError(it.message)
                })
    }

    override fun addEmoticon(face: DynamicFaceBean) {
        mView.showLoading()

        UploadManager.uploadFile(face.path, { url ->
            HttpManager.getStore(UserHttpProtocol::class.java)
                    .addEmoticon(object : HttpReq<UserProto.AddEmoticonReq>() {
                        override fun getData(): UserProto.AddEmoticonReq {
                            return UserHttpReqCreator.addEmoticon(url, face.width, face.height)
                        }
                    })
                    .getResult(mObservalbe, {
                        mView.dismissLoading()
                        mView.showAddEmoticonComplete()

                        loadData()
                    }, {
                        mView.showError(it.message)
                    })
        }, {
            mView.showError(mContext?.getString(R.string.string_dynamic_face_upload_fail))
        })
    }

    override fun delEmoticons(faces: List<DynamicFaceBean>) {
        mView.showLoading()

        val ids = mutableListOf<Long>()
        faces.forEach {
            ids.add(it.id)
        }
        HttpManager.getStore(UserHttpProtocol::class.java)
                .delEmoticons(object : HttpReq<UserProto.DelEmoticonReq>() {
                    override fun getData(): UserProto.DelEmoticonReq {
                        return UserHttpReqCreator.delEmoticons(ids)
                    }
                }).getResult(mObservalbe, {
                    mView.dismissLoading()

                    loadData()
                }, {
                    mView.showError(it.message)
                })
    }
}

