package framework.telegram.business.ui.group.bean

import android.text.TextUtils
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.im.domain.pb.CommonProto

/**
 * Created by yanggl on 2019/8/29 17:49
 */
class AdministratorModel(val i:Int) : MultiItemEntity {

    object Type {
        val TEXT = 0
        val ITEM = 1
    }

    override fun getItemType(): Int {
        return i
    }

    public var uid:Long=0
    public var titleName:String=""
    public var icon:String=""
    public var groupNickName:String=""
    public var remarkName:String=""
    public var nickName:String=""
    public var onlineStatus:Boolean=false
    public var lastOnlineTime:Long=0
    public var isShowLastOnlineTime:Boolean=false
    public var right: CommonProto.AdminRightBase?=null
    public var isSetAdmin: Boolean = false

    fun getDisplayName(): String {
        val displayName = if (TextUtils.isEmpty(remarkName)) groupNickName else remarkName
        return if (TextUtils.isEmpty(displayName)) nickName else displayName
    }
}