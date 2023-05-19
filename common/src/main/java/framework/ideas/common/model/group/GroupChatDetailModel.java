package framework.ideas.common.model.group;

import com.chad.library.adapter.base.entity.MultiItemEntity;
import com.im.domain.pb.CommonProto;

import framework.ideas.common.model.im.MessageReceiptModel;

public class GroupChatDetailModel implements MultiItemEntity {
    private boolean isSelect = false;
    private MessageReceiptModel bean;
    private boolean canSelect = true;

    public GroupChatDetailModel(MessageReceiptModel bean, Boolean canSelect) {
        this.bean = bean;
        this.canSelect = canSelect;
    }

    public MessageReceiptModel getBean() {
        return bean;
    }

    public void setSelect(boolean isSelect) {
        this.isSelect = isSelect;
    }

    public Boolean isSelect() {
        return isSelect;
    }

    public Boolean canSelect() {
        return canSelect;
    }

    @Override
    public int getItemType() {
        return GROUP_CHAT_DETAIL_TYPE;
    }

    public static final int GROUP_CHAT_DETAIL_TYPE = 313;
}
