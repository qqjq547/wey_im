package framework.telegram.message.connect.bean;

import android.text.TextUtils;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;
import com.im.pb.IMPB;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import framework.ideas.common.bean.FileMessageContentBean;
import framework.ideas.common.bean.ImageMessageContentBean;
import framework.ideas.common.bean.LocationMessageContentBean;
import framework.ideas.common.bean.NameCardMessageContentBean;
import framework.ideas.common.bean.NoticeMessageBean;
import framework.ideas.common.bean.RefMessageBean;
import framework.ideas.common.bean.VideoMessageContentBean;
import framework.ideas.common.bean.VoiceMessageContentBean;
import framework.ideas.common.model.im.MessageModel;
import framework.telegram.message.manager.ArouterServiceManager;
import framework.telegram.support.tools.AESHelper;
import framework.telegram.support.tools.HexString;
import framework.telegram.support.tools.MD5;
import framework.telegram.support.tools.framework.telegram.support.UriUtils;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

public class SocketPackageBean {

    //CLIENT TO SERVER
    public static short MESSAGE_TYPE_HEART_REQ = 1000;//心跳
    public static short MESSAGE_TYPE_LOGIN_REQ = 1100;//登录
    public static short MESSAGE_TYPE_LOGOUT_REQ = 1101;//注销
    public static short MESSAGE_TYPE_SEND_ONE_TO_ONE_MSG_REQ = 1102;//发送私聊消息
    public static short MESSAGE_TYPE_GET_ONE_TO_ONE_MSG_COMMEND = 1103;//收到私聊消息(已废弃)
    public static short MESSAGE_TYPE_SEND_ONE_TO_ONE_STREAM_REQ = 1105;//发起一对一的流媒体消息
    public static short MESSAGE_TYPE_SEND_ONE_TO_ONE_STREAM_OPETARE_REQ = 1106;//操作一对一的流媒体消息
    public static short MESSAGE_TYPE_SEND_ONE_TO_ONE_STREAM_REFRESH_TOKEN_REQ = 1107;//一对一的流媒体刷新token
    public static short MESSAGE_TYPE_RECALL_PVT_MSG_COMMEND = 1108;//撤回私聊消息
    public static short MESSAGE_TYPE_RECV_RECALL_PVT_MSG_COMMEND = 1109;//已收到撤回私聊消息
    public static short MESSAGE_TYPE_RECEIPT_MSG_COMMEND = 1110;//发送回执消息
    public static short MESSAGE_TYPE_RECV_PVT_RECEIPT_MSG_COMMEND = 1111;//发送已收到回执消息
    public static short MESSAGE_TYPE_RECV_KEYPAIR_CHANGE_COMMEND = 1112;//发送已接收用户密钥变化信息
    public static short MESSAGE_TYPE_RECV_SCREEN_SHOTS_COMMEND = 1113;//发送截屏通知消息
    public static short MESSAGE_TYPE_RECV_ADD_CONTACT_OPERATE_COMMEND = 1114;//发送已接收好友处理消息
    public static short MESSAGE_TYPE_INPUTTING_STATUS_COMMEND = 1115;//发送正在输入状态
    public static short MESSAGE_TYPE_SEND_GROUP_MSG_REQ = 2101;//发送群聊消息
    public static short MESSAGE_TYPE_GET_GROUP_MSG_COMMEND = 2102;//收到群聊消息(已废弃)
    public static short MESSAGE_TYPE_RECALL_GROUP_MSG_COMMEND = 2103;//撤回群聊消息
    public static short MESSAGE_TYPE_RECV_RECALL_GROUP_MSG_COMMEND = 2104;//已收到撤回群聊消息
    public static short MESSAGE_TYPE_RECV_GROUP_OPERATE_MSG_COMMEND = 2105;//已收到群操作消息
    public static short MESSAGE_TYPE_RECV_GROUP_RECEIPT_MSG_COMMEND = 2106;//发送已收到群回执消息

    //SERVER TO CLIENT
    public static short MESSAGE_TYPE_HEART_RESP = 1200;//心跳答复
    public static short MESSAGE_TYPE_LOGIN_RESP = 1201;//登录答复
    public static short MESSAGE_TYPE_SEND_ONE_TO_ONE_MSG_RESP = 1202;//私聊消息发送成功
    public static short MESSAGE_TYPE_GET_ONE_TO_ONE_MSG = 1203;//接收私聊消息
    public static short MESSAGE_TYPE_KICK_OFF = 1204;
    public static short MESSAGE_TYPE_GET_ONE_TO_ONE_STREAM_OPERATE = 1206;//一对一的流媒体操作消息
    public static short MESSAGE_TYPE_GET_ONE_TO_ONE_STREAM_BUILD = 1207;//双方同意建立一对一的流媒体消息
    public static short MESSAGE_TYPE_GET_ONE_TO_ONE_STREAM_RESP = 1208;//一对一的流媒体消息响应
    public static short MESSAGE_TYPE_GET_ONE_TO_ONE_STREAM_NEW_TOKEN = 1209;//一对一的流媒体新token
    public static short MESSAGE_TYPE_ADD_CONTACT_OPERATE = 1210;//好友请求处理消息
    public static short MESSAGE_TYPE_ADD_CONTACT = 1211;//好友请求数量
    public static short MESSAGE_TYPE_RECALL_PVT_MSG = 1212;//撤回私聊消息
    public static short MESSAGE_TYPE_USER_ON_OFF_LINE = 1213;//用户上下线
    public static short MESSAGE_TYPE_KICK_USER = 1214;//踢人消息
    public static short MESSAGE_TYPE_RECEIPT_MSG = 1215;//消息回执
    public static short MESSAGE_TYPE_RECEIPT_MSG_RESP = 1216;//推送发送回执消息成功
    public static short MESSAGE_TYPE_KEYPAIR_CHANGE_RESP = 1217;//推送用户密钥信息变化
    public static short MESSAGE_TYPE_RECALL_PVT_MSG_RESP = 1218;//推送发送撤回私聊消息成功消息
    public static short MESSAGE_TYPE_SEREEN_SHOTS = 1219;//推送截屏消息
    public static short MESSAGE_TYPE_INPUTTING_STATUS = 1220;//推送对方正在输入状态
    public static short MESSAGE_TYPE_WEB_ONLINE_STATUS = 1221;//推送web端登录状态
    public static short MESSAGE_TYPE_SEND_GROUP_MSG_RESP = 2201;//群聊消息发送成功
    public static short MESSAGE_TYPE_GET_GROUP_MSG = 2202;//接收群聊消息
    public static short MESSAGE_TYPE_JOIN_GROUP = 2203;//群请求数量
    public static short MESSAGE_TYPE_JOIN_GROUP_OPERATE = 2204;//群请求处理消息
    public static short MESSAGE_TYPE_RECALL_GROUP_MSG = 2205;//撤回群消息
    public static short MESSAGE_TYPE_RECALL_GROUP_MSG_RESP = 2206;//推送发送撤回群聊消息成功消息
    public static short MESSAGE_TYPE_PUSH_GROUP_MSG_RECEIPT_RESP = 2207;//推送群消息回执状态
    public static short MESSAGE_TYPE_CONFIRM = 7777;//响应确认消息
    public static short MESSAGE_TYPE_ERROR = 9999;//错误消息

    public static SocketPackageBean toOneToOneMsgSocketPackage(String appSecretKey, String webSecretKey, String myselfWebSecretKey, int myselfKeyVersion, String attachmentKey, MessageModel msgModel, int snapchatTime) {
        try {
            byte[] byteArray = messageToBytes(msgModel);
            String contentMd5 = MD5.md5(byteArray);
            if (byteArray != null) {
                // 加密
                IMPB.MessageContent appMsgContent = null;
                if (!TextUtils.isEmpty(appSecretKey)) {
                    ByteString appBs = ByteString.copyFrom(HexString.hexToBuffer(AESHelper.encrypt(byteArray, appSecretKey)));
                    appMsgContent = IMPB.MessageContent.newBuilder()
                            .setContent(appBs)
                            .setAttachmentKey(TextUtils.isEmpty(attachmentKey) ? "" : AESHelper.encrypt(attachmentKey.getBytes(), appSecretKey))
                            .build();
                } else {
                    appMsgContent = IMPB.MessageContent.newBuilder()
                            .setContent(ByteString.copyFrom(byteArray))
                            .setAttachmentKey(TextUtils.isEmpty(attachmentKey) ? "" : attachmentKey)
                            .build();
                }

                IMPB.MessageContent webMsgContent = null;
                if (!TextUtils.isEmpty(webSecretKey)) {
                    ByteString webBs = ByteString.copyFrom(HexString.hexToBuffer(AESHelper.encrypt(byteArray, webSecretKey)));
                    webMsgContent = IMPB.MessageContent.newBuilder()
                            .setContent(webBs)
                            .setAttachmentKey(TextUtils.isEmpty(attachmentKey) ? "" : AESHelper.encrypt(attachmentKey.getBytes(), webSecretKey))
                            .build();
                } else {
                    webMsgContent = IMPB.MessageContent.newBuilder()
                            .setContent(ByteString.copyFrom(byteArray))
                            .setAttachmentKey(TextUtils.isEmpty(attachmentKey) ? "" : attachmentKey)
                            .build();
                }

                IMPB.MessageContent myselfWebMsgContent = null;
                if (!TextUtils.isEmpty(myselfWebSecretKey)) {
                    ByteString myselWebBs = ByteString.copyFrom(HexString.hexToBuffer(AESHelper.encrypt(byteArray, myselfWebSecretKey)));
                    myselfWebMsgContent = IMPB.MessageContent.newBuilder()
                            .setContent(myselWebBs)
                            .setAttachmentKey(TextUtils.isEmpty(attachmentKey) ? "" : AESHelper.encrypt(attachmentKey.getBytes(), myselfWebSecretKey))
                            .build();
                } else {
                    myselfWebMsgContent = IMPB.MessageContent.newBuilder()
                            .setContent(ByteString.copyFrom(byteArray))
                            .setAttachmentKey(TextUtils.isEmpty(attachmentKey) ? "" : attachmentKey)
                            .build();
                }

                IMPB.OneToOneMessage oneToOneMessage = IMPB.OneToOneMessage.newBuilder()
                        .setSendUid(msgModel.getSenderId())
                        .setReceiveUid(msgModel.getTargetId())
                        .setMsgType(IMPB.MessageType.valueOf(msgModel.getType()))
                        .setContentMd5(contentMd5)
                        .setContent(appMsgContent.getContent())
                        .setAttachmentKey(appMsgContent.getAttachmentKey())
                        .setVersion(myselfKeyVersion)
                        .setAppContent(appMsgContent)
                        .setWebContent(webMsgContent)
                        .setMyselfWebContent(myselfWebMsgContent)
                        .setSnapchatTime(snapchatTime)
                        .build();
                IMPB.SendOneToOneMessage sendOneToOneMessage = IMPB.SendOneToOneMessage.newBuilder()
                        .setOneToOneMsg(oneToOneMessage)
                        .setFlag(Long.valueOf(msgModel.getFlag()))
                        .build();
                return new SocketPackageBean(SocketPackageBean.MESSAGE_TYPE_SEND_ONE_TO_ONE_MSG_REQ, sendOneToOneMessage);
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static SocketPackageBean toGroupMsgSocketPackage(String secretKey, int keyVersion, String attachmentKey, MessageModel msgModel, int snapchatTime) {
        try {
            byte[] byteArray = messageToBytes(msgModel);
            if (byteArray != null) {
                // @人
                List<Long> atUidsLong = new ArrayList<>();
                if (!TextUtils.isEmpty(msgModel.getAtUids())) {
                    String[] atUids = msgModel.getAtUids().split(",");
                    for (String atUid : atUids) {
                        atUidsLong.add(Long.valueOf(atUid));
                    }
                }

                // 加密
                byte[] data;
                int version;
                if (msgModel.getType() == MessageModel.MESSAGE_TYPE_NOTICE || TextUtils.isEmpty(secretKey)) {
                    // 群公告也不加密(服务器需要解密获取公告内容)
                    data = byteArray;
                    version = 0;
                } else {
                    data = HexString.hexToBuffer(AESHelper.encrypt(byteArray, secretKey));
                    version = keyVersion;
                }

                IMPB.GroupMessage groupMessage = IMPB.GroupMessage.newBuilder()
                        .setSendUid(msgModel.getSenderId())
                        .setGroupId(msgModel.getTargetId())
                        .setMsgType(IMPB.MessageType.valueOf(msgModel.getType()))
                        .setContent(ByteString.copyFrom(data))
                        .addAllAtUids(atUidsLong)
                        .setVersion(version)
                        .setContentMd5(MD5.md5(byteArray))
                        .setAttachmentKey(TextUtils.isEmpty(attachmentKey) ? "" : TextUtils.isEmpty(secretKey) ? attachmentKey : AESHelper.encrypt(attachmentKey.getBytes(), secretKey))
                        .setSnapchatTime(snapchatTime)
                        .build();
                IMPB.SendGroupMessage sendGroupMessage = IMPB.SendGroupMessage.newBuilder()
                        .setGroupMsg(groupMessage)
                        .setFlag(Long.valueOf(msgModel.getFlag()))
                        .build();
                return new SocketPackageBean(SocketPackageBean.MESSAGE_TYPE_SEND_GROUP_MSG_REQ, sendGroupMessage);
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static SocketPackageBean toOneToOneMsgSocketPackageUnDetrypt(MessageModel msgModel, int snapchatTime) {
        try {
            byte[] byteArray = messageToBytes(msgModel);
            String contentMd5 = MD5.md5(byteArray);
            if (byteArray != null) {
                IMPB.MessageContent msgContent = IMPB.MessageContent.newBuilder()
                        .setContent(ByteString.copyFrom(byteArray))
                        .setAttachmentKey("")
                        .build();

                IMPB.OneToOneMessage oneToOneMessage = IMPB.OneToOneMessage.newBuilder()
                        .setSendUid(msgModel.getSenderId())
                        .setReceiveUid(msgModel.getTargetId())
                        .setMsgType(IMPB.MessageType.valueOf(msgModel.getType()))
                        .setContentMd5(contentMd5)
                        .setContent(msgContent.getContent())
                        .setAttachmentKey(msgContent.getAttachmentKey())
                        .setVersion(0)
                        .setAppContent(msgContent)
                        .setWebContent(msgContent)
                        .setMyselfWebContent(msgContent)
                        .setSnapchatTime(snapchatTime)
                        .build();
                IMPB.SendOneToOneMessage sendOneToOneMessage = IMPB.SendOneToOneMessage.newBuilder()
                        .setOneToOneMsg(oneToOneMessage)
                        .setFlag(Long.valueOf(msgModel.getFlag()))
                        .build();

                return new SocketPackageBean(SocketPackageBean.MESSAGE_TYPE_SEND_ONE_TO_ONE_MSG_REQ, sendOneToOneMessage);
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static SocketPackageBean toGroupMsgSocketPackageUnDetrypt(MessageModel msgModel) {
        try {
            byte[] byteArray = messageToBytes(msgModel);
            if (byteArray != null) {
                // @人
                List<Long> atUidsLong = new ArrayList<>();
                if (!TextUtils.isEmpty(msgModel.getAtUids())) {
                    String[] atUids = msgModel.getAtUids().split(",");
                    for (String atUid : atUids) {
                        atUidsLong.add(Long.valueOf(atUid));
                    }
                }

                IMPB.GroupMessage groupMessage = IMPB.GroupMessage.newBuilder()
                        .setSendUid(msgModel.getSenderId())
                        .setGroupId(msgModel.getTargetId())
                        .setMsgType(IMPB.MessageType.valueOf(msgModel.getType()))
                        .setContent(ByteString.copyFrom(byteArray))
                        .addAllAtUids(atUidsLong)
                        .setVersion(0)
                        .setContentMd5(MD5.md5(byteArray))
                        .setAttachmentKey("")
                        .build();
                IMPB.SendGroupMessage sendGroupMessage = IMPB.SendGroupMessage.newBuilder()
                        .setGroupMsg(groupMessage)
                        .setFlag(Long.valueOf(msgModel.getFlag()))
                        .build();
                return new SocketPackageBean(SocketPackageBean.MESSAGE_TYPE_SEND_GROUP_MSG_REQ, sendGroupMessage);
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static byte[] messageToBytes(MessageModel msgModel) {
        byte[] byteArray = null;

        try {
            IMPB.ReferenceObj referenceObj;
            if (msgModel.getRefMessageBean() != null) {
                RefMessageBean refMessage = msgModel.getRefMessageBean();
                referenceObj = IMPB.ReferenceObj.newBuilder()
                        .setMsgId(refMessage.msgId)
                        .setContent(refMessage.content)
                        .setType(IMPB.MessageType.forNumber(refMessage.type))
                        .setNickname(refMessage.nickname)
                        .setUid(refMessage.uid).build();

                if (msgModel.getType() == MessageModel.MESSAGE_TYPE_TEXT) {
                    byteArray = IMPB.TextObj.newBuilder().setContent(msgModel.getContent()).setRef(referenceObj).build().toByteArray();
                } else if (msgModel.getType() == MessageModel.MESSAGE_TYPE_NOTICE) {
                    NoticeMessageBean contentBean = msgModel.getNoticeMessageBean();
                    byteArray = IMPB.GroupNoticeObj.newBuilder().setContent(contentBean.content).setShowNotify(contentBean.showNotify).setNoticeId(contentBean.noticeId).build().toByteArray();
                } else if (msgModel.getType() == MessageModel.MESSAGE_TYPE_IMAGE) {
                    ImageMessageContentBean contentBean = msgModel.getImageMessageContent();
                    byteArray = IMPB.ImageObj.newBuilder()
                            .setUrl(contentBean.imageFileUri)
                            .setThumbUrl(contentBean.imageThumbFileUri)
                            .setFileSize(new File(UriUtils.parseUri(contentBean.imageFileBackupUri).getPath()).length())
                            .setWidth(contentBean.width)
                            .setHeight(contentBean.height)
                            .setRef(referenceObj)
                            .build().toByteArray();
                } else if (msgModel.getType() == MessageModel.MESSAGE_TYPE_DYNAMIC_IMAGE) {
                    ImageMessageContentBean contentBean = msgModel.getDynamicImageMessageBean();
                    byteArray = IMPB.DynamicImageObj.newBuilder()
                            .setEmoticonId(contentBean.emoticonId)
                            .setUrl(contentBean.imageFileUri)
                            .setThumbUrl(contentBean.imageThumbFileUri)
                            .setFileSize(new File(UriUtils.parseUri(contentBean.imageFileBackupUri).getPath()).length())
                            .setWidth(contentBean.width)
                            .setHeight(contentBean.height)
                            .setRef(referenceObj)
                            .build().toByteArray();
                } else if (msgModel.getType() == MessageModel.MESSAGE_TYPE_VOICE) {
                    VoiceMessageContentBean contentBean = msgModel.getVoiceMessageContent();

                    byte[] aHighByteArr = new byte[contentBean.highDArr.length];
                    for (int i = 0; i < aHighByteArr.length; i++) {
                        aHighByteArr[i] = (byte) contentBean.highDArr[i];
                    }

                    byteArray = IMPB.AudioObj.newBuilder()
                            .setUrl(contentBean.recordFileUri)
                            .setFileSize(new File(UriUtils.parseUri(contentBean.recordFileBackupUri).getPath()).length())
                            .setDuration(contentBean.recordTime)
                            .setWaveData(ByteString.copyFrom(aHighByteArr))
                            .setRef(referenceObj)
                            .build().toByteArray();
                } else if (msgModel.getType() == MessageModel.MESSAGE_TYPE_VIDEO) {
                    VideoMessageContentBean contentBean = msgModel.getVideoMessageContent();
                    byteArray = IMPB.VideoObj.newBuilder()
                            .setUrl(contentBean.videoFileUri)
                            .setThumbUrl(contentBean.videoThumbFileUri)
                            .setFileSize(new File(UriUtils.parseUri(contentBean.videoFileBackupUri).getPath()).length())
                            .setWidth(contentBean.width)
                            .setHeight(contentBean.height)
                            .setDuration(contentBean.videoTime)
                            .setRef(referenceObj)
                            .build().toByteArray();
                } else if (msgModel.getType() == MessageModel.MESSAGE_TYPE_LOCATION) {
                    LocationMessageContentBean contentBean = msgModel.getLocationMessageContentBean();
                    byteArray = IMPB.LocationObj.newBuilder()
                            .setLat(contentBean.lat)
                            .setLng(contentBean.lng)
                            .setAddress(contentBean.address)
                            .setRef(referenceObj)
                            .build().toByteArray();
                } else if (msgModel.getType() == MessageModel.MESSAGE_TYPE_FILE) {
                    FileMessageContentBean contentBean = msgModel.getFileMessageContentBean();
                    byteArray = IMPB.FileObj.newBuilder()
                            .setName(contentBean.name)
                            .setSize(contentBean.size)
                            .setFileUrl(contentBean.fileUri)
                            .setMimeType(contentBean.mimeType)
                            .setRef(referenceObj)
                            .build().toByteArray();
                } else if (msgModel.getType() == MessageModel.MESSAGE_TYPE_NAMECARD) {
                    NameCardMessageContentBean contentBean = msgModel.getNameCardMessageContent();
                    byteArray = IMPB.NameCardObj.newBuilder()
                            .setNickName(contentBean.nickName)
                            .setIcon(contentBean.icon)
                            .setUid(contentBean.uid)
                            .setRef(referenceObj)
                            .build().toByteArray();
                }
            } else {
                if (msgModel.getType() == MessageModel.MESSAGE_TYPE_TEXT) {
                    byteArray = IMPB.TextObj.newBuilder().setContent(msgModel.getContent()).build().toByteArray();
                } else if (msgModel.getType() == MessageModel.MESSAGE_TYPE_NOTICE) {
                    NoticeMessageBean contentBean = msgModel.getNoticeMessageBean();
                    byteArray = IMPB.GroupNoticeObj.newBuilder().setContent(contentBean.content).setShowNotify(contentBean.showNotify).setNoticeId(contentBean.noticeId).build().toByteArray();
                } else if (msgModel.getType() == MessageModel.MESSAGE_TYPE_IMAGE) {
                    ImageMessageContentBean contentBean = msgModel.getImageMessageContent();
                    byteArray = IMPB.ImageObj.newBuilder()
                            .setUrl(contentBean.imageFileUri)
                            .setThumbUrl(contentBean.imageThumbFileUri)
                            .setFileSize(new File(UriUtils.parseUri(contentBean.imageFileBackupUri).getPath()).length())
                            .setWidth(contentBean.width)
                            .setHeight(contentBean.height)
                            .build().toByteArray();
                } else if (msgModel.getType() == MessageModel.MESSAGE_TYPE_DYNAMIC_IMAGE) {
                    ImageMessageContentBean contentBean = msgModel.getDynamicImageMessageBean();
                    byteArray = IMPB.DynamicImageObj.newBuilder()
                            .setUrl(contentBean.imageFileUri)
                            .setEmoticonId(contentBean.emoticonId)
                            .setThumbUrl(contentBean.imageThumbFileUri)
                            .setFileSize(new File(UriUtils.parseUri(contentBean.imageFileBackupUri).getPath()).length())
                            .setWidth(contentBean.width)
                            .setHeight(contentBean.height)
                            .build().toByteArray();
                } else if (msgModel.getType() == MessageModel.MESSAGE_TYPE_VOICE) {
                    VoiceMessageContentBean contentBean = msgModel.getVoiceMessageContent();

                    byte[] aHighByteArr = new byte[contentBean.highDArr.length];
                    for (int i = 0; i < aHighByteArr.length; i++) {
                        aHighByteArr[i] = (byte) contentBean.highDArr[i];
                    }

                    byteArray = IMPB.AudioObj.newBuilder()
                            .setUrl(contentBean.recordFileUri)
                            .setFileSize(new File(UriUtils.parseUri(contentBean.recordFileBackupUri).getPath()).length())
                            .setDuration(contentBean.recordTime)
                            .setWaveData(ByteString.copyFrom(aHighByteArr))
                            .build().toByteArray();
                } else if (msgModel.getType() == MessageModel.MESSAGE_TYPE_VIDEO) {
                    VideoMessageContentBean contentBean = msgModel.getVideoMessageContent();
                    byteArray = IMPB.VideoObj.newBuilder()
                            .setUrl(contentBean.videoFileUri)
                            .setThumbUrl(contentBean.videoThumbFileUri)
                            .setFileSize(new File(UriUtils.parseUri(contentBean.videoFileBackupUri).getPath()).length())
                            .setWidth(contentBean.width)
                            .setHeight(contentBean.height)
                            .setDuration(contentBean.videoTime)
                            .build().toByteArray();
                } else if (msgModel.getType() == MessageModel.MESSAGE_TYPE_LOCATION) {
                    LocationMessageContentBean contentBean = msgModel.getLocationMessageContentBean();
                    byteArray = IMPB.LocationObj.newBuilder()
                            .setLat(contentBean.lat)
                            .setLng(contentBean.lng)
                            .setAddress(contentBean.address)
                            .build().toByteArray();
                } else if (msgModel.getType() == MessageModel.MESSAGE_TYPE_FILE) {
                    FileMessageContentBean contentBean = msgModel.getFileMessageContentBean();
                    byteArray = IMPB.FileObj.newBuilder()
                            .setName(contentBean.name)
                            .setSize(contentBean.size)
                            .setFileUrl(contentBean.fileUri)
                            .setMimeType(contentBean.mimeType)
                            .build().toByteArray();
                } else if (msgModel.getType() == MessageModel.MESSAGE_TYPE_NAMECARD) {
                    NameCardMessageContentBean contentBean = msgModel.getNameCardMessageContent();
                    byteArray = IMPB.NameCardObj.newBuilder()
                            .setNickName(contentBean.nickName)
                            .setIcon(contentBean.icon)
                            .setUid(contentBean.uid)
                            .setIdentify(contentBean.identify)
                            .build().toByteArray();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return byteArray;
    }

    private short messageType;

    private byte[] data;

    public SocketPackageBean(short messageType) {
        this.messageType = messageType;
    }

    public SocketPackageBean(short messageType, MessageLite data) {
        this.messageType = messageType;
        this.data = data.toByteArray();
    }

    public SocketPackageBean(short messageType, byte[] data) {
        this.messageType = messageType;
        this.data = data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    public void setMessageType(short messageType) {
        this.messageType = messageType;
    }

    public short getMessageType() {
        return messageType;
    }

}
