package framework.ideas.common.model.contacts;

import com.chad.library.adapter.base.entity.MultiItemEntity;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class ContactReqModel extends RealmObject implements MultiItemEntity {

    public static ContactReqModel createContactReqModel(long uid, int type, String nickName, int gender, String icon, boolean bfFriend, String remarkName,
                                                        String msg, boolean bfMyBlack, long modifyTime, int addType, String signature) {
        ContactReqModel model = new ContactReqModel();
        model.id = System.nanoTime();
        model.type = type;
        model.uid = uid;
        model.nickName = nickName;
        model.gender = gender;
        model.icon = icon;
        model.bfFriend = bfFriend;
        model.remarkName = remarkName;
        model.msg = msg;
        model.bfMyBlack = bfMyBlack;
        model.modifyTime = modifyTime;
        model.addType = addType;
        model.signature = signature;
        return model;
    }


    @PrimaryKey
    private long id = 0;//id

    private int type;//0未处理 1已处理

    /**
     * 资料相关
     */
    private long uid; // 用户ID

    private String nickName; // 昵称

    private String icon; // 头像

    private int gender; // 性别 0保密  1男  2女

    /**
     * 关系相关
     */
    private boolean bfFriend; // 是否好友

    private String remarkName; // 备注名

    /**
     * 好友请求相关
     */
    private String msg; //申请信息

    private boolean bfMyBlack; //我的黑名单用户

    private long modifyTime; //最后更新时间

    private int addType; //添加方式 0手机号 1扫码 2群聊 3名片

    private String signature; //个性签名

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getAddType() {
        return addType;
    }

    public void setAddType(int addType) {
        this.addType = addType;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public int getGender() {
        return gender;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    public boolean getBfFriend() {
        return bfFriend;
    }

    public void setBfFriend(boolean bfFriend) {
        this.bfFriend = bfFriend;
    }

    public String getRemarkName() {
        return remarkName;
    }

    public void setRemarkName(String remarkName) {
        this.remarkName = remarkName;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public boolean getBfMyBlack() {
        return bfMyBlack;
    }

    public void setBfMyBlack(boolean bfMyBlack) {
        this.bfMyBlack = bfMyBlack;
    }

    public long getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(long modifyTime) {
        this.modifyTime = modifyTime;
    }

    @Override
    public int getItemType() {
        return CONTACT_REQ_ITEM_TYPE;
    }

    public static final int CONTACT_REQ_ITEM_TYPE = 204;
}
