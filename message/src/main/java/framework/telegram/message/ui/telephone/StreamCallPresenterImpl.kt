package framework.telegram.message.ui.telephone

import android.annotation.SuppressLint
import com.trello.rxlifecycle3.RxLifecycle
import com.trello.rxlifecycle3.android.ActivityEvent
import com.trello.rxlifecycle3.android.FragmentEvent
import framework.ideas.common.model.im.StreamCallModel
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.bridge.bean.StreamCallItem
import framework.telegram.message.db.RealmCreator
import framework.telegram.support.account.AccountManager
import framework.telegram.support.tools.ThreadUtils
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.RealmResults
import io.realm.Sort
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class StreamCallPresenterImpl(view: StreamCallContract.View, private val isFragment: Boolean)
    : StreamCallContract.Presenter, RealmChangeListener<RealmResults<StreamCallModel>> {

    private var mFObservable: Observable<FragmentEvent>? = null
    private var mAObservable: Observable<ActivityEvent>? = null
    private var isSort: Boolean = false

    private val mView: StreamCallContract.View? = view

    private val mRealm by lazy { RealmCreator.getStreamCallHistoryRealm(mMineUid) }

    private val mMineUid by lazy { AccountManager.getLoginAccount(AccountInfo::class.java).getUserId() }

    private var mStreamCallModels: RealmResults<StreamCallModel>? = null

    private val mStreamCallList by lazy { ArrayList<StreamCallItem>() }

    init {
        mView?.setPresenter(this)
    }

    override fun start() {

    }

    override fun stop(){
        mRealm.removeAllChangeListeners()
        mRealm.close()
    }

    override fun setFObservable(o: Observable<FragmentEvent>) {
        this.mFObservable = o
        isSort = false
    }

    override fun setAObservable(o: Observable<ActivityEvent>) {
        this.mAObservable = o
        isSort = true
    }

    @SuppressLint("CheckResult")
    override fun getStreamCallHistoryData(chaterId: Long, isMissCall: Boolean, streamType: Int,date: Long) {
        val flowable = Flowable.just<Realm>(mRealm)
        if (isFragment) {
            flowable.compose(RxLifecycle.bindUntilEvent(mFObservable!! as Observable<FragmentEvent>, FragmentEvent.DESTROY))
        } else {
            flowable.compose(RxLifecycle.bindUntilEvent(mAObservable!! as Observable<ActivityEvent>, ActivityEvent.DESTROY))
        }
        flowable.subscribeOn(AndroidSchedulers.mainThread())
                .map {
                    if (isMissCall)
                        it.where(StreamCallModel::class.java)
                                ?.equalTo("chaterId",chaterId)
                                ?.and()
                                ?.equalTo("isSend",0.toInt())
                                ?.and()
                                ?.equalTo("streamType",streamType)
                                ?.and()
                                    ?.beginGroup()
                                        ?.equalTo("status",0.toInt())
                                        ?.or()
                                        ?.equalTo("status",3.toInt())
                                    ?.endGroup()
                                ?.greaterThanOrEqualTo("reqTime",coverTimeInMillis(date))
                                ?.sort("reqTime", Sort.DESCENDING)
                                ?.findAllAsync()
                    else
                        it.where(StreamCallModel::class.java)
                                ?.equalTo("chaterId",chaterId)
                                ?.and()
                                ?.equalTo("streamType",streamType)
                                ?.beginGroup()
                                    ?.equalTo("isSend",1.toInt())
                                    ?.or()
                                    ?.beginGroup()
                                        ?.equalTo("isSend",0.toInt())
                                        ?.notEqualTo("status",0.toInt())
                                        ?.notEqualTo("status",3.toInt())
                                    ?.endGroup()
                                ?.endGroup()
                                ?.greaterThanOrEqualTo("reqTime",coverTimeInMillis(date))
                                ?.sort("reqTime", Sort.DESCENDING)
                                ?.findAllAsync()
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    mStreamCallModels = it
                    mStreamCallModels?.addChangeListener(this)
                }
    }

    @SuppressLint("CheckResult")
    override fun getStreamCallHistoryData() {
        val flowable = Flowable.just<Realm>(mRealm)
        if (isFragment) {
            flowable.compose(RxLifecycle.bindUntilEvent(mFObservable!! as Observable<FragmentEvent>, FragmentEvent.DESTROY))
        } else {
            flowable.compose(RxLifecycle.bindUntilEvent(mAObservable!! as Observable<ActivityEvent>, ActivityEvent.DESTROY))
        }
        flowable.subscribeOn(AndroidSchedulers.mainThread())
                .map {
                    it.where(StreamCallModel::class.java)
                            ?.sort("reqTime", Sort.DESCENDING)
                            ?.findAllAsync()
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    mStreamCallModels = it
                    mStreamCallModels?.addChangeListener(this)
                }
    }


    @SuppressLint("CheckResult")
    override fun getStreamUnCallHistoryData() {
        val flowable = Flowable.just<Realm>(mRealm)
        if (isFragment) {
            flowable.compose(RxLifecycle.bindUntilEvent(mFObservable!! as Observable<FragmentEvent>, FragmentEvent.DESTROY))
        } else {
            flowable.compose(RxLifecycle.bindUntilEvent(mAObservable!! as Observable<ActivityEvent>, ActivityEvent.DESTROY))
        }
        flowable.subscribeOn(AndroidSchedulers.mainThread())
                .map {
                    it.where(StreamCallModel::class.java)
                            ?.equalTo("isSend",0.toInt())
                            ?.and()
                                ?.beginGroup()
                                    ?.equalTo("status",0.toInt())
                                    ?.or()
                                    ?.equalTo("status",3.toInt())
                                ?.endGroup()
                            ?.sort("reqTime", Sort.DESCENDING)
                            ?.findAllAsync()
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    mStreamCallModels = it
                    mStreamCallModels?.addChangeListener(this)
                }
    }

    override fun onChange(t: RealmResults<StreamCallModel>) {
        if (!t.isValid)
            return
        mStreamCallList.clear()

        t.forEach{ streamCallModel ->
            val currentItem = streamCallModel.copyStream()
            when(isSort){
                // 详情
                true -> {
                    val content = StreamCallItem(currentItem,0)
                    if (mStreamCallList.size == 0){
                        val timeCopy = StreamCallItem(currentItem,0)
                        timeCopy.nearCount = -1
                        mStreamCallList.add(timeCopy)
                    }else {
                        val last = mStreamCallList.last()
                        if (coverTime(last.data.reqTime) != coverTime(content.data.reqTime)) {
                            val timeCopy = StreamCallItem(currentItem,0)
                            timeCopy.nearCount = -1
                            mStreamCallList.add(timeCopy)
                        }
                    }
                    mStreamCallList.add(content)
                }
                // 全部通话记录
                false -> {
                    if (mStreamCallList.isNotEmpty()) {
                        var isFind = false
                        for (i in mStreamCallList.indices) {
                            val it = mStreamCallList[i]
                            /**
                             * 新规则：
                             * 同一天同一种拨打方式，A打给我，但我没有接听的，合并成了一条
                             * 同一天同一种拨打方式，我跟A的所有通话记录（除了 A打给我，我没有接听的 ）合并成了一条
                             */
                            // 同一天同一种拨打方式
                            if (coverTime(it.data.reqTime) == coverTime(currentItem.reqTime)
                                    && it.data.chaterId == currentItem.chaterId
                                    && it.data.streamType == currentItem.streamType){

                                // 未接听
                                if ((it.data.isSend == 0 && currentItem.isSend == 0)
                                        && (currentItem.status == 0 || currentItem.status == 3)
                                        && (it.data.status == 0 || it.data.status == 3)) {
                                    it.nearSessionIdList.add(currentItem.sessionId)
                                    isFind = true
                                    it.nearCount++
                                    break
                                }


                                else if (it.data.isSend == 1 && currentItem.isSend == 1){
                                    it.nearSessionIdList.add(currentItem.sessionId)
                                    isFind = true
                                    it.nearCount++
                                    break
                                } else if (it.data.isSend == 1 && currentItem.isSend == 0
                                        && (currentItem.status != 0 && currentItem.status != 3)){
                                    it.nearSessionIdList.add(currentItem.sessionId)
                                    isFind = true
                                    it.nearCount++
                                    break
                                } else if (it.data.isSend == 0
                                        && (it.data.status != 0 && it.data.status != 3)
                                        && currentItem.isSend == 0
                                        && (currentItem.status != 0 && currentItem.status != 3))
                                {
                                    it.nearSessionIdList.add(currentItem.sessionId)
                                    isFind = true
                                    it.nearCount++
                                    break
                                } else if  (it.data.isSend == 0
                                        && (it.data.status != 0 && it.data.status != 3)
                                        && currentItem.isSend == 1)
                                {
                                    it.nearSessionIdList.add(currentItem.sessionId)
                                    isFind = true
                                    it.nearCount++
                                    break
                                }
                            }
                        }
                        if (!isFind)
                            mStreamCallList.add(StreamCallItem(currentItem, 0))
                    }
                    else
                        mStreamCallList.add(StreamCallItem(currentItem,0))
                }
            }
        }

        ThreadUtils.runOnUIThread {
            mView?.update(mStreamCallList)
        }

    }

    /**
     * 时间戳转换为日期字符串
     */
    @SuppressLint("SimpleDateFormat")
    private fun coverTime(time: Long): String{
        val date = Date(time)
        val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy")
        return simpleDateFormat.format(date)
    }

    /**
     *
     * 获取某天的00：00：00的时间戳
     * 默认东八区
     */
    private fun coverTimeInMillis(targetDate: Long): Long {
        val tz = "GMT+8"
        val curTimeZone = TimeZone.getTimeZone(tz)
        val calendar = Calendar.getInstance(curTimeZone)
        calendar.timeInMillis = targetDate
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}