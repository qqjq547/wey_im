package framework.telegram.message.ui.media

import android.annotation.SuppressLint
import com.trello.rxlifecycle3.android.FragmentEvent
import framework.ideas.common.model.im.MessageModel
import framework.telegram.message.manager.ArouterServiceManager
import io.reactivex.Observable
import io.realm.Realm
import io.realm.RealmQuery
import io.realm.RealmResults
import io.realm.Sort
import java.text.SimpleDateFormat
import java.util.*

class MediaPresentImpl(observable: Observable<FragmentEvent>, view: MediaPresenter.View) : MediaPresenter.Presenter {

    private val mObservable = observable
    private val mView: MediaPresenter.View = view
    private var mRealm: Realm? = null
    private val mDataList = mutableListOf<MessageModel>()

    init {
        mView.setPresenter(this)
    }

    override fun start() {

    }

    override fun getFileFromGroup(targetId: Long) {
        mRealm = ArouterServiceManager.messageService.newSearchGroupContentRealm(targetId)
        getChatData(mRealm, MessageModel.MESSAGE_TYPE_FILE)
    }

    override fun getFileFromPv(targetId: Long) {
        mRealm = ArouterServiceManager.messageService.newSearchContentRealm(targetId)
        getChatData(mRealm, MessageModel.MESSAGE_TYPE_FILE)
    }

    override fun getMediaFromGroup(targetId: Long) {
        mRealm = ArouterServiceManager.messageService.newSearchGroupContentRealm(targetId)
        getChatData(mRealm, MessageModel.MESSAGE_TYPE_IMAGE)

    }

    override fun getMediaFromPv(targetId: Long) {
        mRealm = ArouterServiceManager.messageService.newSearchContentRealm(targetId)
        getChatData(mRealm, MessageModel.MESSAGE_TYPE_IMAGE)

    }

    private fun getChatData(realm: Realm?, msgType: Int) {
        mDataList.clear()
        val default = 0
        var models: RealmResults<MessageModel>? = null
        var mQuery: RealmQuery<MessageModel>? = null
        realm?.let {
            it.executeTransactionAsync(Realm.Transaction { realm ->
                mQuery = realm.where(MessageModel::class.java)
                if (msgType == MessageModel.MESSAGE_TYPE_FILE) {
                    mQuery?.equalTo("type", MessageModel.MESSAGE_TYPE_FILE)
                } else if (msgType == MessageModel.MESSAGE_TYPE_IMAGE) {
                    mQuery?.beginGroup()
                                ?.equalTo("type", MessageModel.MESSAGE_TYPE_IMAGE)
                                ?.or()
                                ?.equalTo("type", MessageModel.MESSAGE_TYPE_VIDEO)
                            ?.endGroup()
                }
                models = mQuery
                        ?.equalTo("snapchatTime", default)
                        ?.sort("time", Sort.DESCENDING)
                        ?.findAll()
                models?.forEach { msg ->
                    if (msg.type == MessageModel.MESSAGE_TYPE_IMAGE || msg.type == MessageModel.MESSAGE_TYPE_VIDEO)
                        addToMediaList(mDataList, msg.copyMessage())
                    else if (msg.type == MessageModel.MESSAGE_TYPE_FILE)
                        addToFileList(mDataList, msg.copyMessage())
                }
            }, Realm.Transaction.OnSuccess {
                mView.showData(softListByTime(mDataList))
            })
            it.close()
        }
    }

    private fun addToFileList(list: MutableList<MessageModel>, msg: MessageModel){
        if (msg.fileMessageContentBean != null ) {
            list.add(msg)
        }
    }

    private fun addToMediaList(list: MutableList<MessageModel>, msg: MessageModel){
        if (msg.imageMessageContent != null && msg.imageMessageContent.imageFileUri != null) {
            list.add(msg)
        }
        else if (msg.videoMessageContent != null && msg.videoMessageContent.videoFileUri != null)
            list.add(msg)
    }

    private fun softListByTime(list: MutableList<MessageModel>): MutableList<MessageModel> {
        val outList = mutableListOf<MessageModel>()
        list.forEach {
            if (outList.size == 0 || coverTime(outList.last().time) != coverTime(it.time)){
                val timeContent = it.copyMessage()
                timeContent.type = MessageModel.LOCAL_TYPE_OTHER_UNKNOW
                timeContent.content = coverTime(it.time)
                outList.add(timeContent)
            }
            outList.add(it)
        }
        return outList
    }

    @SuppressLint("SimpleDateFormat")
    private fun coverTime(time: Long): String{
        val date = Date(time)
        val simpleDateFormat = SimpleDateFormat("MM-yyyy")
        return simpleDateFormat.format(date)
    }
}