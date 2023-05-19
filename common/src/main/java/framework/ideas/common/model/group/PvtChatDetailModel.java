package framework.ideas.common.model.group;

import com.chad.library.adapter.base.entity.MultiItemEntity;
import com.im.domain.pb.CommonProto;

public class PvtChatDetailModel implements MultiItemEntity {
    private boolean isSelect = false;
    private CommonProto.MsgReceiptStatusBase bean;
    private boolean showAlreadyRead;

    public boolean isShowAlreadyRead() {
        return showAlreadyRead;
    }

    public void setShowAlreadyRead(boolean showAlreadyRead) {
        this.showAlreadyRead = showAlreadyRead;
    }

    public PvtChatDetailModel(CommonProto.MsgReceiptStatusBase bean) {
        this.bean = bean;
    }

    public CommonProto.MsgReceiptStatusBase getBean() {
        return bean;
    }

    public void setSelect(boolean isSelect) {
        this.isSelect = isSelect;
    }

    public Boolean isSelect() {
        return isSelect;
    }

    @Override
    public int getItemType() {
        return PVT_CHAT_DETAIL_TYPE;
    }

    public static final int PVT_CHAT_DETAIL_TYPE = 314;
}
