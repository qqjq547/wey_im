package framework.ideas.common.model.contacts;

import com.chad.library.adapter.base.entity.MultiItemEntity;

import framework.ideas.common.model.group.GroupInfoModel;
import framework.telegram.support.system.pinyin.FastPinyin;
import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

public class ContactDataModel extends RealmObject implements MultiItemEntity {

    public static ContactDataModel createContact(long uid, String nickName, int sex, String icon, String noteName, boolean bfStar,
                                                 boolean bfMyBlack, boolean bfDisturb, boolean bfVerify, boolean bfMyContacts, String letter, String depict, String signature,
                                                 boolean bfDisShowOnline, boolean onlineStatus, long lastOnlineTime, boolean isShowLastOnlineTime, String phone, boolean isDeleteMe,
                                                 boolean bfReadCancel, int msgCancelTime, boolean bfScreenshot, boolean readReceipt, int commonGroupNum, boolean bfCancel, boolean bfBanned, String identify) {
        ContactDataModel model = new ContactDataModel();
        model.uid = uid;
        model.nickName = nickName;
        model.searchNickName = FastPinyin.Companion.toAllPinyin(nickName);
        model.shortNickName = FastPinyin.Companion.toAllPinyinFirst(nickName,false);
        model.sex = sex;
        model.icon = icon;
        model.noteName = noteName;
        model.searchNoteName = FastPinyin.Companion.toAllPinyin(noteName);
        model.shortNoteName = FastPinyin.Companion.toAllPinyinFirst(noteName,false);
        model.bfStar = bfStar;
        model.bfMyBlack = bfMyBlack;
        model.bfDisturb = bfDisturb;
        model.bfMyContacts = bfMyContacts;
        model.bfVerify = bfVerify;
        model.bfDisShowOnline = bfDisShowOnline;
        model.letter = letter;
        model.depict = depict;
        model.signature = signature;
        model.onlineStatus = onlineStatus;
        model.lastOnlineTime = lastOnlineTime;
        model.isShowLastOnlineTime = isShowLastOnlineTime;
        model.phone = phone;
        model.searchPhone = phone.replace(" ","");
        model.isDeleteMe = isDeleteMe ? 1 : 0;
        model.bfReadCancel = bfReadCancel;
        model.msgCancelTime = msgCancelTime;
        model.bfScreenshot = bfScreenshot;
        model.readReceipt = readReceipt;
        model.commonGroupNum = commonGroupNum;
        model.bfCancel = bfCancel;
        model.bfBanned = bfBanned;
        model.identify = identify;
        return model;
    }

    public ContactDataModel copyContactDataModel() {
        ContactDataModel model = new ContactDataModel();
        model.uid = uid;
        model.nickName = nickName;
        model.searchNickName = searchNickName;
        model.shortNickName = shortNickName;
        model.sex = sex;
        model.icon = icon;
        model.noteName = noteName;
        model.searchNoteName = searchNoteName;
        model.shortNoteName = shortNoteName;
        model.bfStar = bfStar;
        model.bfMyBlack = bfMyBlack;
        model.bfDisturb = bfDisturb;
        model.bfMyContacts = bfMyContacts;
        model.bfVerify = bfVerify;
        model.bfDisShowOnline = bfDisShowOnline;
        model.letter = letter;
        model.depict = depict;
        model.signature = signature;
        model.onlineStatus = onlineStatus;
        model.lastOnlineTime = lastOnlineTime;
        model.isShowLastOnlineTime = isShowLastOnlineTime;
        model.phone = phone;
        model.searchPhone = searchPhone;
        model.isDeleteMe = isDeleteMe;
        model.bfReadCancel = bfReadCancel;
        model.msgCancelTime = msgCancelTime;
        model.bfScreenshot = bfScreenshot;
        model.readReceipt = readReceipt;
        model.commonGroupNum = commonGroupNum;
        model.bfCancel = bfCancel;
        model.bfBanned = bfBanned;
        model.identify = identify;
        model.uploadContactTime = uploadContactTime;
        return model;
    }

    @PrimaryKey
    private long uid = 0;//用户id

    private String nickName;//昵称

    private String searchNickName;//昵称(搜索用的)

    private String shortNickName;//昵称(搜索用的)

    private int sex;//性别

    private String icon;//昵称

    private String noteName;//备注名

    private String searchNoteName;//备注名(搜索用的)

    private String shortNoteName;//昵称(搜索用的)

    private boolean bfStar; //星标

    private boolean bfMyBlack;//我的黑名单用户

    private boolean bfDisturb;//免打扰

    private boolean bfMyContacts; //是否联系人（区分联系人卡/非联系人卡）

    private boolean bfVerify;//是否需要申请验证(非联系人)

    private boolean bfDisShowOnline;//是否显示online

    private String letter;//昵称首字母

    private String depict;//描述

    private String signature;//个性签名

    private boolean onlineStatus;//最后上下线状态

    private long lastOnlineTime;//最后上下线时间

    private boolean isShowLastOnlineTime;//是否显示最后在线时间

    private String phone;

    private String searchPhone;//给搜索用的

    private int isDeleteMe = 0;//是否把我删除了

    private boolean bfReadCancel;//是否开启阅后即焚
    private int msgCancelTime;//焚毁时间长度,单位秒
    private boolean bfScreenshot;//是否开启截屏通知
    private boolean readReceipt;//是否开启已读回执

    private int commonGroupNum;//共同群聊

    private boolean bfCancel;//是否注销了

    private boolean bfBanned;//是否被禁

    private String identify;//Bufa号

    private long uploadContactTime = 0L;//更新用户列表的时间

    public boolean isBfCancel() {
        return bfCancel;
    }

    public void setBfCancel(boolean bfCancel) {
        this.bfCancel = bfCancel;
    }

    public boolean isReadReceipt() {
        return readReceipt;
    }

    public void setReadReceipt(boolean readReceipt) {
        this.readReceipt = readReceipt;
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

    public int getSex() {
        return sex;
    }

    public void setSex(int sex) {
        this.sex = sex;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getNoteName() {
        return noteName;
    }

    public void setNoteName(String noteName) {
        this.noteName = noteName;
    }

    public String getLetter() {
        return letter;
    }

    public void setLetter(String letter) {
        this.letter = letter;
    }

    public void setDepict(String depict) {
        this.depict = depict;
    }

    public String getDepict() {
        return depict;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getSignature() {
        return signature;
    }

    public boolean isOnlineStatus() {
        return onlineStatus;
    }

    public void setOnlineStatus(boolean onlineStatus) {
        this.onlineStatus = onlineStatus;
    }

    public boolean isShowLastOnlineTime() {
        return isShowLastOnlineTime;
    }

    public void setShowLastOnlineTime(boolean showLastOnlineTime) {
        isShowLastOnlineTime = showLastOnlineTime;
    }

    public long getLastOnlineTime() {
        return lastOnlineTime;
    }

    public void setLastOnlineTime(long lastOnlineTime) {
        this.lastOnlineTime = lastOnlineTime;
    }

    public void setBfDisturb(boolean bfDisturb) {
        this.bfDisturb = bfDisturb;
    }

    public void setBfVerify(boolean bfVerify) {
        this.bfVerify = bfVerify;
    }

    public void setBfMyBlack(boolean bfMyBlack) {
        this.bfMyBlack = bfMyBlack;
    }

    public void setBfMyContacts(boolean bfMyContacts) {
        this.bfMyContacts = bfMyContacts;
    }

    public void setBfStar(boolean bfStar) {
        this.bfStar = bfStar;
    }

    public boolean isBfDisturb() {
        return bfDisturb;
    }

    public boolean isBfMyBlack() {
        return bfMyBlack;
    }

    public boolean isBfMyContacts() {
        return bfMyContacts;
    }

    public boolean isBfStar() {
        return bfStar;
    }

    public boolean isBfVerify() {
        return bfVerify;
    }

    public boolean isBfDisShowOnline() {
        return bfDisShowOnline;
    }

    public void setBfDisShowOnline(boolean bfDisShowOnline) {
        this.bfDisShowOnline = bfDisShowOnline;
    }

    @Override
    public int getItemType() {
        return CONTACT_ITEM_TYPE;
    }

    public static final int CONTACT_ITEM_TYPE = 202;

    public String getDisplayName() {
        return noteName.isEmpty() ? nickName : noteName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Boolean getDeleteMe() {
        return isDeleteMe == 1;
    }

    public void setDeleteMe(Boolean deleteMe) {
        isDeleteMe = deleteMe ? 1 : 0;
    }

    public boolean isBfReadCancel() {
        return bfReadCancel;
    }

    public void setBfReadCancel(boolean bfReadCancel) {
        this.bfReadCancel = bfReadCancel;
    }

    public int getMsgCancelTime() {
        return msgCancelTime;
    }

    public void setMsgCancelTime(int msgCancelTime) {
        this.msgCancelTime = msgCancelTime;
    }

    public boolean isBfScreenshot() {
        return bfScreenshot;
    }

    public void setBfScreenshot(boolean bfScreenshot) {
        this.bfScreenshot = bfScreenshot;
    }

    public int getCommonGroupNum() {
        return commonGroupNum;
    }

    public void setCommonGroupNum(int commonGroupNum) {
        this.commonGroupNum = commonGroupNum;
    }

    public boolean isBfBanned() {
        return bfBanned;
    }

    public void setBfBanned(boolean bfBanned) {
        this.bfBanned = bfBanned;
    }

    public String getIdentify() {
        return identify;
    }

    public void setIdentify(String identify) {
        this.identify = identify;
    }

    public long getUploadContactTime() {
        return uploadContactTime;
    }

    public void setUploadContactTime(long uploadContactTime) {
        this.uploadContactTime = uploadContactTime;
    }

    public String getSearchPhone() {
        return searchPhone;
    }

    public void setSearchPhone(String searchPhone) {
        this.searchPhone = searchPhone;
    }

    public String getSearchNickName() {
        return searchNickName;
    }

    public String getSearchNoteName() {
        return searchNoteName;
    }

    public String getShortNickName() {
        return shortNickName;
    }

    public String getShortNoteName() {
        return shortNoteName;
    }

    public void setSearchNoteName(String searchNoteName) {
        this.searchNoteName = searchNoteName;
    }

    public void setShortNoteName(String shortNoteName) {
        this.shortNoteName = shortNoteName;
    }
}