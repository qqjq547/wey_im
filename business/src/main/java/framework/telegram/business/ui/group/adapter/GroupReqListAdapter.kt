package framework.telegram.business.ui.group.adapter

import android.graphics.drawable.ColorDrawable
import android.text.TextUtils
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chad.library.adapter.base.BaseViewHolder
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.drawee.generic.RoundingParams
import com.im.domain.pb.CommonProto
import com.im.domain.pb.GroupProto
import framework.telegram.business.R
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.tools.ExpandClass.getSimpleColor
import framework.telegram.support.tools.TimeUtils
import framework.telegram.ui.doubleclick.recycler.AppBaseQuickAdapter
import framework.telegram.ui.image.AppImageView
import framework.telegram.ui.text.AppTextView
import framework.telegram.ui.utils.ScreenUtils

class GroupReqListAdapter : AppBaseQuickAdapter<GroupReqListAdapter.GroupReqInfoItem, BaseViewHolder>(R.layout.bus_group_item_add_req) {

    class GroupReqInfoItem(val data: GroupProto.GroupReqInfo) {
        var checkUser = data.checkUser
        var status = data.groupReqStatus
        var type = data.groupReqType
    }

    private val mMineUid by lazy { AccountManager.getLoginAccount(AccountInfo::class.java).getUserId() }

    override fun convert(helper: BaseViewHolder, item: GroupReqInfoItem?) {
        val data = item?.data
        if (data != null && helper != null) {
            helper.getView<AppTextView>(R.id.app_text_view_name).maxWidth = ScreenUtils.getScreenWidth(BaseApp.app) - ScreenUtils.dp2px(BaseApp.app, 210.0f)
            when (item.type) {
                CommonProto.GroupReqType.GROUP_IS_BANNED -> {
                    // v1.4.0 需要添加多种类型，表示已删除的群通知
                    helper.getView<AppImageView>(R.id.image_view_icon).setImageURI(data.pic)
                    helper.getView<AppTextView>(R.id.app_text_view_name).text = data.groupName
                    helper.getView<TextView>(R.id.text_view_time).text = " · ${TimeUtils.timeFormatToChat(BaseApp.app, data.createTime)}"
                    helper.getView<TextView>(R.id.text_view_req_reason).text = item.data.msg
                    helper.setGone(R.id.text_view_req_reason, true)
                    helper.setGone(R.id.text_view_operate_admin, false)
                    helper.setGone(R.id.text_view_button_status, false)
                    helper.setGone(R.id.image_view_button_agree, false)
                    helper.setGone(R.id.text_view_introduce, false)
                }
                CommonProto.GroupReqType.GROUP_IS_DISABLE -> {
                    // v1.6.0 需要添加多种类型，表示群解散
                    helper.getView<AppImageView>(R.id.image_view_icon).setImageURI(data.pic)
                    helper.getView<AppTextView>(R.id.app_text_view_name).text = data.groupName
                    helper.getView<TextView>(R.id.text_view_time).text = " · ${TimeUtils.timeFormatToChat(BaseApp.app, data.createTime)}"
                    helper.getView<TextView>(R.id.text_view_req_reason).text = item.data.msg
                    helper.setGone(R.id.text_view_req_reason, true)
                    helper.setGone(R.id.text_view_operate_admin, false)
                    helper.setGone(R.id.text_view_button_status, false)
                    helper.setGone(R.id.image_view_button_agree, false)
                    helper.setGone(R.id.text_view_introduce, false)
                }
                CommonProto.GroupReqType.GROUP_MEMBER_SHUTUP -> {
                    helper.getView<AppImageView>(R.id.image_view_icon).setImageURI(data.pic)
                    helper.getView<AppTextView>(R.id.app_text_view_name).text = data.groupName
                    helper.getView<TextView>(R.id.text_view_time).text = " · ${TimeUtils.timeFormatToChat(BaseApp.app, data.createTime)}"
                    helper.getView<TextView>(R.id.text_view_req_reason).text = item.data.msg
                    helper.setGone(R.id.text_view_req_reason, true)
                    helper.setGone(R.id.text_view_operate_admin, false)
                    helper.setGone(R.id.text_view_button_status, false)
                    helper.setGone(R.id.image_view_button_agree, false)
                    helper.setGone(R.id.text_view_introduce, false)
                }
                CommonProto.GroupReqType.GROUP_TRANSFER -> {
                    // 群转让
                    // 接口下发，只有被转让人收到
                    helper.getView<AppImageView>(R.id.image_view_icon).setImageURI(data.pic)
                    setIconParams(helper.getView(R.id.image_view_icon), false)
                    helper.getView<AppTextView>(R.id.app_text_view_name).text = data.groupName
                    helper.getView<TextView>(R.id.text_view_time).text = " · ${TimeUtils.timeFormatToChat(BaseApp.app, data.createTime)}"
                    helper.getView<AppTextView>(R.id.text_view_req_reason).text = item.data.msg
                    helper.setGone(R.id.text_view_introduce, false)
                    helper.setGone(R.id.text_view_req_reason, true)
                    helper.setGone(R.id.text_view_operate_admin, false)

                    helper.setGone(R.id.text_view_button_status, false)
                    helper.setGone(R.id.image_view_button_agree, false)

                }
                CommonProto.GroupReqType.GROUP_SET_ADMIN -> {
                    // 设置管理员
                    // 接口下发，只有被设置人收到
                    helper.getView<AppImageView>(R.id.image_view_icon).setImageURI(data.pic)
                    setIconParams(helper.getView(R.id.image_view_icon), false)
                    helper.getView<AppTextView>(R.id.app_text_view_name).text = data.groupName
                    helper.getView<TextView>(R.id.text_view_time).text = " · ${TimeUtils.timeFormatToChat(BaseApp.app, data.createTime)}"
                    helper.getView<AppTextView>(R.id.text_view_req_reason).text = item.data.msg
                    helper.setGone(R.id.text_view_introduce, false)
                    helper.setGone(R.id.text_view_req_reason, true)
                    helper.setGone(R.id.text_view_operate_admin, false)

                    helper.setGone(R.id.text_view_button_status, false)
                    helper.setGone(R.id.image_view_button_agree, false)
                }
                CommonProto.GroupReqType.GROUP_CANCLE_ADMIN -> {
                    // 取消管理员
                    // 接口下发，只有被设置人收到
                    helper.getView<AppImageView>(R.id.image_view_icon).setImageURI(data.pic)
                    setIconParams(helper.getView(R.id.image_view_icon), false)
                    helper.getView<AppTextView>(R.id.app_text_view_name).text = data.groupName
                    helper.getView<TextView>(R.id.text_view_time).text = " · ${TimeUtils.timeFormatToChat(BaseApp.app, data.createTime)}"
                    helper.getView<AppTextView>(R.id.text_view_req_reason).text = item.data.msg
                    helper.setGone(R.id.text_view_introduce, false)
                    helper.setGone(R.id.text_view_req_reason, true)
                    helper.setGone(R.id.text_view_operate_admin, false)

                    helper.setGone(R.id.text_view_button_status, false)
                    helper.setGone(R.id.image_view_button_agree, false)
                }
                CommonProto.GroupReqType.GROUP_LINK,
                CommonProto.GroupReqType.GROUP_QR_CODE -> {
                    // 主动扫描二维码入群
                    when (item.status) {
                        CommonProto.GroupReqStatus.CHECKING -> {
                            // 接口下发，只有群主能收到
                            // 用户扫码入群等待审核
                            helper.getView<AppImageView>(R.id.image_view_icon).setImageURI(data.targetUser?.icon)
                            setIconParams(helper.getView(R.id.image_view_icon), true)
                            helper.getView<AppTextView>(R.id.app_text_view_name).text = getDisplayName(data.targetUser)
                            helper.getView<TextView>(R.id.text_view_time).text = " · ${TimeUtils.timeFormatToChat(BaseApp.app, data.createTime)}"
                            helper.getView<AppTextView>(R.id.text_view_req_reason).text = String.format(mContext.getString(R.string.apply_to_join_mat), data.groupName)
                            helper.setGone(R.id.text_view_req_reason, true)
                            helper.setGone(R.id.text_view_operate_admin, false)

                            if (!TextUtils.isEmpty(data.msg)) {
                                helper.getView<AppTextView>(R.id.text_view_introduce).text = data.msg
                                helper.setGone(R.id.text_view_introduce, true)
                            } else {
                                helper.setGone(R.id.text_view_introduce, false)
                            }

                            //待处理
                            helper.addOnClickListener(R.id.image_view_button_agree)
                            helper.setGone(R.id.image_view_button_agree, true)
                            helper.setGone(R.id.text_view_button_status, false)

                            helper.addOnClickListener(R.id.relative_layout)
                        }
                        CommonProto.GroupReqStatus.AGREE -> {
                            // 接口下发，只有审核这条请求的群主能收到
                            // 用户扫码入群已同意
                            helper.getView<AppImageView>(R.id.image_view_icon).setImageURI(data.targetUser?.icon)
                            setIconParams(helper.getView(R.id.image_view_icon), true)
                            helper.getView<AppTextView>(R.id.app_text_view_name).text = getDisplayName(data.targetUser)
                            helper.getView<TextView>(R.id.text_view_time).text = " · ${TimeUtils.timeFormatToChat(BaseApp.app, data.createTime)}"
                            helper.getView<AppTextView>(R.id.text_view_req_reason).text = String.format(mContext.getString(R.string.apply_to_join_mat), data.groupName)
                            helper.setGone(R.id.text_view_req_reason, true)

                            if (!TextUtils.isEmpty(data.msg)) {
                                helper.getView<AppTextView>(R.id.text_view_introduce).text = data.msg
                                helper.setGone(R.id.text_view_introduce, true)
                            } else {
                                helper.setGone(R.id.text_view_introduce, false)
                            }

                            if (item.checkUser.uid != mMineUid && !TextUtils.isEmpty(getDisplayName(item.checkUser))) {
                                helper.getView<AppTextView>(R.id.text_view_operate_admin).text = String.format(mContext.getString(R.string.processing_manager_sign), getDisplayName(item.checkUser))
                                helper.setGone(R.id.text_view_operate_admin, true)
                            } else {
                                helper.setGone(R.id.text_view_operate_admin, false)
                            }

                            val textView = helper.getView<TextView>(R.id.text_view_button_status)
                            textView.setBackgroundResource(R.drawable.common_corners_trans_edeff2_6_0)
                            textView.text = mContext.getString(R.string.agreed)
                            textView.setTextColor(mContext.getSimpleColor(R.color.a2a4a7))
                            helper.setGone(R.id.text_view_button_status, true)
                            helper.setGone(R.id.image_view_button_agree, false)
                        }
                        CommonProto.GroupReqStatus.REFUSE -> {
                            // 接口下发，只有审核这条请求的群主能收到
                            // 用户扫码入群已拒绝
                            helper.getView<AppImageView>(R.id.image_view_icon).setImageURI(data.targetUser?.icon)
                            setIconParams(helper.getView(R.id.image_view_icon), true)
                            helper.getView<AppTextView>(R.id.app_text_view_name).text = getDisplayName(data.targetUser)
                            helper.getView<TextView>(R.id.text_view_time).text = " · ${TimeUtils.timeFormatToChat(BaseApp.app, data.createTime)}"
                            helper.getView<AppTextView>(R.id.text_view_req_reason).text = String.format(mContext.getString(R.string.apply_to_join_mat), data.groupName)
                            helper.setGone(R.id.text_view_req_reason, true)

                            if (!TextUtils.isEmpty(data.msg)) {
                                helper.getView<AppTextView>(R.id.text_view_introduce).text = data.msg
                                helper.setGone(R.id.text_view_introduce, true)
                            } else {
                                helper.setGone(R.id.text_view_introduce, false)
                            }

                            if (item.checkUser.uid != mMineUid && !TextUtils.isEmpty(getDisplayName(item.checkUser))) {
                                helper.getView<AppTextView>(R.id.text_view_operate_admin).text = String.format(mContext.getString(R.string.processing_manager_sign), getDisplayName(item.checkUser))
                                helper.setGone(R.id.text_view_operate_admin, true)
                            } else {
                                helper.setGone(R.id.text_view_operate_admin, false)
                            }

                            val textView = helper.getView<TextView>(R.id.text_view_button_status)
                            textView.setBackgroundResource(R.drawable.common_corners_trans_edeff2_6_0)
                            textView.text = mContext.getString(R.string.denied)
                            textView.setTextColor(mContext.getSimpleColor(R.color.a2a4a7))
                            helper.setGone(R.id.text_view_button_status, true)
                            helper.setGone(R.id.image_view_button_agree, false)
                        }
                        else -> {
                            // 未知状态
                        }
                    }
                }
                CommonProto.GroupReqType.GROUP_OWNER_CHECK_QR_CODE -> {
                    when (item.status) {
                        CommonProto.GroupReqStatus.REFUSE -> {
                            // 接口下发，只有被审核者能收到
                            // 用户扫码入群已拒绝
                            helper.getView<AppImageView>(R.id.image_view_icon).setImageURI(item.checkUser?.icon)
                            setIconParams(helper.getView(R.id.image_view_icon), true)
                            helper.getView<AppTextView>(R.id.app_text_view_name).text = getDisplayName(item.checkUser)
                            helper.getView<TextView>(R.id.text_view_time).text = " · ${TimeUtils.timeFormatToChat(BaseApp.app, data.createTime)}"
                            helper.getView<AppTextView>(R.id.text_view_req_reason).text = item.data.msg
                            helper.setGone(R.id.text_view_introduce, false)
                            helper.setGone(R.id.text_view_req_reason, true)
                            helper.setGone(R.id.text_view_operate_admin, false)

                            helper.setGone(R.id.text_view_button_status, false)
                            helper.setGone(R.id.image_view_button_agree, false)
                        }
                    }
                }
                CommonProto.GroupReqType.GROUP_INVITE -> {
                    when (item.status) {
                        CommonProto.GroupReqStatus.CHECKING -> {
                            // 接口下发，只有审核这条请求的群主能收到或被邀请者能收到
                            // 用户被邀请入群等待审核
                            if (data.targetUser.uid == mMineUid) {
                                // 被邀请者
                                helper.getView<AppImageView>(R.id.image_view_icon).setImageURI(data.pic)
                                setIconParams(helper.getView(R.id.image_view_icon), false)
                                helper.getView<AppTextView>(R.id.app_text_view_name).text = data.groupName
                                helper.getView<TextView>(R.id.text_view_time).text = " · ${TimeUtils.timeFormatToChat(BaseApp.app, data.createTime)}"

                                helper.getView<AppTextView>(R.id.text_view_req_reason).text = String.format(mContext.getString(R.string.invite_you_to_join_a_group_chat), getDisplayName(data.fromUser))
                                helper.setGone(R.id.text_view_req_reason, true)
                                helper.setGone(R.id.text_view_introduce, false)
                                helper.setGone(R.id.text_view_operate_admin, false)
                            } else {
                                // 群主
                                helper.getView<AppImageView>(R.id.image_view_icon).setImageURI(data.targetUser?.icon)
                                setIconParams(helper.getView(R.id.image_view_icon), true)
                                helper.getView<AppTextView>(R.id.app_text_view_name).text = getDisplayName(data.targetUser)
                                helper.getView<TextView>(R.id.text_view_time).text = " · ${TimeUtils.timeFormatToChat(BaseApp.app, data.createTime)}"

                                helper.getView<AppTextView>(R.id.text_view_req_reason).text = String.format(mContext.getString(R.string.apply_to_join_mat), data.groupName)
                                helper.setGone(R.id.text_view_req_reason, true)
                                helper.setGone(R.id.text_view_operate_admin, false)

                                if (!TextUtils.isEmpty(data.msg)) {
                                    helper.getView<AppTextView>(R.id.text_view_introduce).text = data.msg
                                    helper.setGone(R.id.text_view_introduce, true)
                                } else {
                                    helper.setGone(R.id.text_view_introduce, false)
                                }
                            }

                            //待处理
                            helper.addOnClickListener(R.id.image_view_button_agree)
                            helper.setGone(R.id.text_view_button_status, false)
                            helper.setGone(R.id.image_view_button_agree, true)

                            helper.addOnClickListener(R.id.relative_layout)
                        }
                        CommonProto.GroupReqStatus.AGREE -> {
                            // 接口下发，只有被邀请者能收到
                            // 用户邀请入群已同意
                            helper.getView<AppImageView>(R.id.image_view_icon).setImageURI(data.pic)
                            setIconParams(helper.getView(R.id.image_view_icon), false)
                            helper.getView<AppTextView>(R.id.app_text_view_name).text = data.groupName
                            helper.getView<TextView>(R.id.text_view_time).text = " · ${TimeUtils.timeFormatToChat(BaseApp.app, data.createTime)}"

                            if (data.targetUser.uid == mMineUid) {
                                // 被邀请者
                                helper.getView<AppImageView>(R.id.image_view_icon).setImageURI(data.pic)
                                setIconParams(helper.getView(R.id.image_view_icon), false)
                                helper.getView<AppTextView>(R.id.app_text_view_name).text = data.groupName
                                helper.getView<TextView>(R.id.text_view_time).text = " · ${TimeUtils.timeFormatToChat(BaseApp.app, data.createTime)}"

                                helper.getView<AppTextView>(R.id.text_view_req_reason).text = String.format(mContext.getString(R.string.invite_you_to_join_a_group_chat), getDisplayName(data.fromUser))
                                helper.setGone(R.id.text_view_req_reason, true)
                                helper.setGone(R.id.text_view_introduce, false)
                            } else {
                                // 群主
                                helper.getView<AppImageView>(R.id.image_view_icon).setImageURI(data.targetUser?.icon)
                                setIconParams(helper.getView(R.id.image_view_icon), true)
                                helper.getView<AppTextView>(R.id.app_text_view_name).text = getDisplayName(data.targetUser)
                                helper.getView<TextView>(R.id.text_view_time).text = " · ${TimeUtils.timeFormatToChat(BaseApp.app, data.createTime)}"

                                helper.getView<AppTextView>(R.id.text_view_req_reason).text = String.format(mContext.getString(R.string.apply_to_join_mat), data.groupName)
                                helper.setGone(R.id.text_view_req_reason, true)

                                if (!TextUtils.isEmpty(data.msg)) {
                                    helper.getView<AppTextView>(R.id.text_view_introduce).text = data.msg
                                    helper.setGone(R.id.text_view_introduce, true)
                                } else {
                                    helper.setGone(R.id.text_view_introduce, false)
                                }
                            }

                            if (data.checkUser.uid != mMineUid && !TextUtils.isEmpty(getDisplayName(data.checkUser))) {
                                helper.getView<AppTextView>(R.id.text_view_operate_admin).text = String.format(mContext.getString(R.string.processing_manager_sign),getDisplayName(data.checkUser))
                                helper.setGone(R.id.text_view_operate_admin, true)
                            } else {
                                helper.setGone(R.id.text_view_operate_admin, false)
                            }

                            val textView = helper.getView<TextView>(R.id.text_view_button_status)
                            textView.setBackgroundResource(R.drawable.common_corners_trans_edeff2_6_0)
                            textView.text = mContext.getString(R.string.agreed)
                            textView.setTextColor(mContext.getSimpleColor(R.color.a2a4a7))
                            helper.setGone(R.id.text_view_button_status, true)
                            helper.setGone(R.id.image_view_button_agree, false)
                        }
                        CommonProto.GroupReqStatus.REFUSE -> {
                            // 接口下发，只有被邀请者能收到
                            // 用户邀请入群已拒绝
                            helper.getView<AppImageView>(R.id.image_view_icon).setImageURI(data.pic)
                            setIconParams(helper.getView(R.id.image_view_icon), false)
                            helper.getView<AppTextView>(R.id.app_text_view_name).text = data.groupName
                            helper.getView<TextView>(R.id.text_view_time).text = " · ${TimeUtils.timeFormatToChat(BaseApp.app, data.createTime)}"

                            if (data.targetUser.uid == mMineUid) {
                                // 被邀请者
                                helper.getView<AppImageView>(R.id.image_view_icon).setImageURI(data.pic)
                                setIconParams(helper.getView(R.id.image_view_icon), false)
                                helper.getView<AppTextView>(R.id.app_text_view_name).text = data.groupName
                                helper.getView<TextView>(R.id.text_view_time).text = " · ${TimeUtils.timeFormatToChat(BaseApp.app, data.createTime)}"

                                helper.getView<AppTextView>(R.id.text_view_req_reason).text = String.format(mContext.getString(R.string.invite_you_to_join_a_group_chat), getDisplayName(data.fromUser))
                                helper.setGone(R.id.text_view_req_reason, true)
                                helper.setGone(R.id.text_view_introduce, false)
                            } else {
                                // 群主
                                helper.getView<AppImageView>(R.id.image_view_icon).setImageURI(data.targetUser?.icon)
                                setIconParams(helper.getView(R.id.image_view_icon), true)
                                helper.getView<AppTextView>(R.id.app_text_view_name).text = getDisplayName(data.targetUser)
                                helper.getView<TextView>(R.id.text_view_time).text = " · ${TimeUtils.timeFormatToChat(BaseApp.app, data.createTime)}"

                                helper.getView<AppTextView>(R.id.text_view_req_reason).text = String.format(mContext.getString(R.string.apply_to_join_mat), data.groupName)
                                helper.setGone(R.id.text_view_req_reason, true)

                                if (!TextUtils.isEmpty(data.msg)) {
                                    helper.getView<AppTextView>(R.id.text_view_introduce).text = data.msg
                                    helper.setGone(R.id.text_view_introduce, true)
                                } else {
                                    helper.setGone(R.id.text_view_introduce, false)
                                }
                            }

                            if (data.checkUser.uid != mMineUid && !TextUtils.isEmpty(getDisplayName(data.checkUser))) {
                                helper.getView<AppTextView>(R.id.text_view_operate_admin).text = String.format(mContext.getString(R.string.processing_manager_sign),getDisplayName(data.checkUser))
                                helper.setGone(R.id.text_view_operate_admin, true)
                            } else {
                                helper.setGone(R.id.text_view_operate_admin, false)
                            }

                            val textView = helper.getView<TextView>(R.id.text_view_button_status)
                            textView.setBackgroundResource(R.drawable.common_corners_trans_edeff2_6_0)
                            textView.text = mContext.getString(R.string.denied)
                            textView.setTextColor(mContext.getSimpleColor(R.color.a2a4a7))
                            helper.setGone(R.id.text_view_button_status, true)
                            helper.setGone(R.id.image_view_button_agree, false)
                        }
                        else -> {
                            // 未知状态
                        }
                    }
                }
                CommonProto.GroupReqType.GROUP_MEMBER_CHECK -> {
                    when (item.status) {
                        CommonProto.GroupReqStatus.REFUSE -> {
                            // 接口下发，只有邀请者能收到
                            // 用户扫码入群已拒绝
                            helper.getView<AppImageView>(R.id.image_view_icon).setImageURI(data.targetUser.icon)
                            setIconParams(helper.getView(R.id.image_view_icon), true)
                            helper.getView<AppTextView>(R.id.app_text_view_name).text = getDisplayName(data.targetUser)
                            helper.getView<TextView>(R.id.text_view_time).text = " · ${TimeUtils.timeFormatToChat(BaseApp.app, data.createTime)}"
                            helper.getView<AppTextView>(R.id.text_view_req_reason).text = item.data.msg
                            helper.setGone(R.id.text_view_introduce, false)
                            helper.setGone(R.id.text_view_req_reason, true)
                            helper.setGone(R.id.text_view_operate_admin, false)

                            helper.setGone(R.id.text_view_button_status, false)
                            helper.setGone(R.id.image_view_button_agree, false)
                        }
                        else -> {
                            // 未知状态
                        }
                    }
                }
                CommonProto.GroupReqType.GROUP_OWNER_CHECK_INVITE -> {
                    when (item.status) {
                        CommonProto.GroupReqStatus.REFUSE -> {
                            // 接口下发，只有被邀请者能收到
                            // 用户扫码入群已拒绝
                            helper.getView<AppImageView>(R.id.image_view_icon).setImageURI(data.pic)
                            setIconParams(helper.getView(R.id.image_view_icon), false)
                            helper.getView<AppTextView>(R.id.app_text_view_name).text = data.groupName
                            helper.getView<TextView>(R.id.text_view_time).text = " · ${TimeUtils.timeFormatToChat(BaseApp.app, data.createTime)}"

                            helper.getView<AppTextView>(R.id.text_view_req_reason).text =item.data.msg
                            helper.setGone(R.id.text_view_introduce, false)
                            helper.setGone(R.id.text_view_req_reason, true)
                            helper.setGone(R.id.text_view_operate_admin, false)

                            helper.setGone(R.id.text_view_button_status, false)
                            helper.setGone(R.id.image_view_button_agree, false)
                        }
                        else -> {
                            // 未知状态
                        }
                    }
                }
                else->{
                    helper.getView<AppImageView>(R.id.image_view_icon).setImageURI(data.pic)
                    helper.getView<AppTextView>(R.id.app_text_view_name).text = data.groupName
                    helper.getView<TextView>(R.id.text_view_req_reason).text = item.data.msg
                    helper.setGone(R.id.text_view_req_reason, true)
                    helper.setGone(R.id.text_view_operate_admin, false)
                    helper.setGone(R.id.text_view_button_status, false)
                    helper.setGone(R.id.image_view_button_agree, false)
                    helper.setGone(R.id.text_view_introduce, false)
                    helper.setGone(R.id.text_view_time,false)
                }
            }

            helper.addOnLongClickListener(R.id.relative_layout)
        }
    }

    private fun getDisplayName(user: CommonProto.UserBase?): String {
        return if (user != null) {
            if (TextUtils.isEmpty(user.friendRelation.remarkName)) user.nickName else user.friendRelation.remarkName
        } else {
            ""
        }
    }

    private fun setIconParams(view: AppImageView, asCircle: Boolean) {
//        if (asCircle) {
        val roundingParams = RoundingParams.fromCornersRadius(5f)
        roundingParams.roundAsCircle = true
        view.hierarchy.roundingParams = roundingParams
        view.hierarchy.actualImageScaleType = ScalingUtils.ScaleType.FOCUS_CROP
        view.hierarchy.setPlaceholderImage(R.drawable.common_holder_one_group)
        view.hierarchy.setBackgroundImage(ColorDrawable(ContextCompat.getColor(view.context, R.color.edeff2)))
        view.hierarchy.fadeDuration = 300
//        } else {
//            val roundingParams = RoundingParams.fromCornersRadius(ScreenUtils.dp2px(BaseApp.app, 4.0f).toFloat())
//            roundingParams.roundAsCircle = false
//            view.hierarchy.roundingParams = roundingParams
//            view.hierarchy.actualImageScaleType = ScalingUtils.ScaleType.FOCUS_CROP
//            view.hierarchy.setPlaceholderImage(R.drawable.common_holder_one_group)
//            view.hierarchy.setBackgroundImage(ColorDrawable(ContextCompat.getColor(view.context, R.color.edeff2)))
//            view.hierarchy.fadeDuration = 300
//        }
    }
}

