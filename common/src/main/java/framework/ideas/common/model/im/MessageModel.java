package framework.ideas.common.model.im;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.chad.library.adapter.base.entity.MultiItemEntity;
import com.im.pb.IMPB;

import framework.ideas.common.R;
import framework.ideas.common.audio.AudioSampleUtils;
import framework.ideas.common.bean.FileMessageContentBean;
import framework.ideas.common.bean.LocationMessageContentBean;
import framework.ideas.common.bean.NoticeMessageBean;
import framework.ideas.common.bean.RefMessageBean;
import framework.ideas.common.bean.TipMessageContentBean;
import framework.ideas.common.bean.ImageMessageContentBean;
import framework.ideas.common.bean.NameCardMessageContentBean;
import framework.ideas.common.bean.StreamMessageContentBean;
import framework.ideas.common.bean.VideoMessageContentBean;
import framework.ideas.common.bean.VoiceMessageContentBean;
import framework.telegram.support.BaseApp;
import framework.telegram.support.system.gson.GsonInstanceCreater;
import framework.telegram.ui.emoji.EmojiInformation;
import framework.telegram.ui.emoji.EmojiUtils;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

public class MessageModel extends RealmObject implements MultiItemEntity {

    /**
     * 消息样式（可用于区别显示UI）
     */
    public static final int LOCAL_TYPE_MYSELF_TEXT = 0;
    public static final int LOCAL_TYPE_OTHER_TEXT = 1;
    public static final int LOCAL_TYPE_MYSELF_VOICE = 2;
    public static final int LOCAL_TYPE_OTHER_VOICE = 3;
    public static final int LOCAL_TYPE_MYSELF_VIDEO = 4;
    public static final int LOCAL_TYPE_OTHER_VIDEO = 5;
    public static final int LOCAL_TYPE_MYSELF_IMAGE = 6;
    public static final int LOCAL_TYPE_OTHER_IMAGE = 7;
    public static final int LOCAL_TYPE_MYSELF_STREAM_MEDIA = 8;
    public static final int LOCAL_TYPE_OTHER_STREAM_MEDIA = 9;
    public static final int LOCAL_TYPE_MYSELF_NAMECARD = 10;
    public static final int LOCAL_TYPE_OTHER_NAMECARD = 11;
    public static final int LOCAL_TYPE_MYSELF_LOCATION = 12;
    public static final int LOCAL_TYPE_OTHER_LOCATION = 13;
    public static final int LOCAL_TYPE_MYSELF_FILE = 14;
    public static final int LOCAL_TYPE_OTHER_FILE = 15;
    public static final int LOCAL_TYPE_MYSELF_NOTICE = 16;
    public static final int LOCAL_TYPE_OTHER_NOTICE = 17;
    public static final int LOCAL_TYPE_MYSELF_DYNAMIC_IMAGE = 18;
    public static final int LOCAL_TYPE_OTHER_DYNAMIC_IMAGE = 19;

    public static final int LOCAL_TYPE_TIP = 101;
    public static final int LOCAL_TYPE_MYSELF_RECALL = 201;
    public static final int LOCAL_TYPE_OTHER_RECALL = 202;
    public static final int LOCAL_TYPE_MYSELF_UNKNOW = 301;
    public static final int LOCAL_TYPE_OTHER_UNKNOW = 302;
    public static final int LOCAL_TYPE_MYSELF_UNDECRYPT = 303;
    public static final int LOCAL_TYPE_OTHER_UNDECRYPT = 304;

    /**
     * 消息类型
     */
    public static final int MESSAGE_TYPE_UNDECRYPT = -2;//无法解密的消息，客户端自定义
    public static final int MESSAGE_TYPE_UNKNOW = -1;//不支持的消息格式，客户端自定义
    public static final int MESSAGE_TYPE_TEXT = 0;
    public static final int MESSAGE_TYPE_IMAGE = 1;
    public static final int MESSAGE_TYPE_VOICE = 2;
    public static final int MESSAGE_TYPE_VIDEO = 3;
    public static final int MESSAGE_TYPE_LOCATION = 4;
    public static final int MESSAGE_TYPE_NAMECARD = 5;
    public static final int MESSAGE_TYPE_SYSTEM = 6;
    public static final int MESSAGE_TYPE_FILE = 7;
    public static final int MESSAGE_TYPE_NOTICE = 8;
    public static final int MESSAGE_TYPE_DYNAMIC_IMAGE = 9;

    public static final int MESSAGE_TYPE_STREAM = 100;//流媒体，客户端自定义
    public static final int MESSAGE_TYPE_GROUP_TIP = 103;//群提示，客户端自定义
    public static final int MESSAGE_TYPE_SYSTEM_TIP = 104;//系统提示，客户端自定义

    public static final int MESSAGE_TYPE_RECALL_SUCCESS = 201;//已撤回成功的消息，客户端自定义
    public static final int MESSAGE_TYPE_ERROR_TIP = 202;//错误提示消息，客户端自定义
    public static final int MESSAGE_TYPE_RECALL = 203;//撤回的消息，客户端自定义

    /**
     * 发送状态
     */
    public static final int STATUS_SEND_FAIL = -1;//发送失败
    public static final int STATUS_SENDING = 0;//发送中
    public static final int STATUS_SENDED_NO_RESP = 1;//已发送但服务器未答复
    public static final int STATUS_SENDED_HAS_RESP = 2;//已发送且服务器已答复
    public static final int STATUS_ATTACHMENT_PROCESSING = 99;//附件处理中
    public static final int STATUS_ATTACHMENT_UPLOADING = 100;//附件上传中

    public static MessageModel createTextMessage(String text, long sendTime, long[] atUids, MessageModel refMessageBean, int snapchatTime, long senderId, long targetId, int chatType) {
        MessageModel msgModel = new MessageModel();
        msgModel.id = System.nanoTime();
        msgModel.flag = String.valueOf(System.nanoTime());
        msgModel.senderId = senderId;
        msgModel.targetId = targetId;
        msgModel.ownerUid = senderId;
        msgModel.type = MESSAGE_TYPE_TEXT;
        msgModel.isSend = 1;
        msgModel.content = text;
        msgModel.status = MessageModel.STATUS_SENDING;
        msgModel.time = sendTime;
        msgModel.snapchatTime = snapchatTime;
        msgModel.chatType = chatType;

        if (refMessageBean != null) {
            RefMessageBean refMessage = new RefMessageBean();
            refMessage.msgId = refMessageBean.msgId;
            refMessage.content = refMessageBean.getRefContent();
            refMessage.type = refMessageBean.getRefMessageType();
            refMessage.uid = refMessageBean.ownerUid;
            refMessage.nickname = refMessageBean.ownerName;
            msgModel.refMsg = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(refMessage);
        }

        StringBuilder sb = new StringBuilder();
        if (atUids != null && atUids.length > 0) {
            long firstAtUid = atUids[0];
            if (firstAtUid == -1) {
                sb.append(firstAtUid);
            } else {
                sb.append(firstAtUid);
                for (int i = 1; i < atUids.length; i++) {
                    sb.append(",").append(atUids[i]);
                }
            }
        }
        msgModel.atUids = sb.toString();
        msgModel.isAtMe = 0;
        return msgModel;
    }

    public static MessageModel createNoticeMessage(long noticeId, String noticeContent, Boolean showNotify, long sendTime, long senderId, long targetGid, int chatType) {
        NoticeMessageBean noticeMessageBean = new NoticeMessageBean();
        noticeMessageBean.noticeId = noticeId;
        noticeMessageBean.content = noticeContent;
        noticeMessageBean.showNotify = showNotify;

        MessageModel msgModel = new MessageModel();
        msgModel.id = System.nanoTime();
        msgModel.flag = String.valueOf(System.nanoTime());
        msgModel.senderId = senderId;
        msgModel.targetId = targetGid;
        msgModel.ownerUid = senderId;
        msgModel.type = MESSAGE_TYPE_NOTICE;
        msgModel.isSend = 1;
        msgModel.content = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(noticeMessageBean);
        msgModel.status = MessageModel.STATUS_SENDING;
        msgModel.time = sendTime;
        msgModel.chatType = chatType;

        return msgModel;
    }

    public static MessageModel createNameCardMessage(long uid, String nickName, String icon, String identify, long sendTime, MessageModel refMessageBean, int snapchatTime, long senderId, long targetId, int chatType) {
        NameCardMessageContentBean nameCardMessageContentBean = new NameCardMessageContentBean();
        nameCardMessageContentBean.uid = uid;
        nameCardMessageContentBean.nickName = nickName;
        nameCardMessageContentBean.icon = icon;
        nameCardMessageContentBean.identify = identify;

        MessageModel msgModel = new MessageModel();
        msgModel.id = System.nanoTime();
        msgModel.flag = String.valueOf(System.nanoTime());
        msgModel.senderId = senderId;
        msgModel.targetId = targetId;
        msgModel.ownerUid = senderId;
        msgModel.type = MESSAGE_TYPE_NAMECARD;
        msgModel.isSend = 1;
        msgModel.content = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(nameCardMessageContentBean);
        msgModel.status = MessageModel.STATUS_SENDING;
        msgModel.time = sendTime;
        msgModel.snapchatTime = snapchatTime;
        msgModel.chatType = chatType;

        if (refMessageBean != null) {
            RefMessageBean refMessage = new RefMessageBean();
            refMessage.msgId = refMessageBean.msgId;
            refMessage.content = refMessageBean.getRefContent();
            refMessage.type = refMessageBean.getRefMessageType();
            refMessage.uid = refMessageBean.ownerUid;
            refMessage.nickname = refMessageBean.ownerName;
            msgModel.refMsg = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(refMessage);
        }

        return msgModel;
    }

    public static MessageModel createVoiceMessage(int recordTime, String recordFileUri, int[] highDArr, String attachmentKey, long sendTime,
                                                  MessageModel refMessageBean, int snapchatTime, long senderId, long targetId, int chatType) {
        VoiceMessageContentBean voiceMessageContentBean = new VoiceMessageContentBean();
        voiceMessageContentBean.recordTime = recordTime;
        if (highDArr != null && highDArr.length > 100) {
            voiceMessageContentBean.highDArr = highDArr;
            voiceMessageContentBean.localHighDArr = AudioSampleUtils.INSTANCE.adjustedSamples(highDArr, recordTime);
        }

        if (recordFileUri.startsWith("file://")) {
            voiceMessageContentBean.recordFileBackupUri = recordFileUri;
        } else {
            voiceMessageContentBean.recordFileUri = recordFileUri;
        }

        MessageModel msgModel = new MessageModel();
        msgModel.id = System.nanoTime();
        msgModel.flag = String.valueOf(System.nanoTime());
        msgModel.senderId = senderId;
        msgModel.targetId = targetId;
        msgModel.ownerUid = senderId;
        msgModel.type = MESSAGE_TYPE_VOICE;
        msgModel.isSend = 1;
        msgModel.attachmentKey = attachmentKey;
        msgModel.content = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(voiceMessageContentBean);
        msgModel.status = MessageModel.STATUS_ATTACHMENT_PROCESSING;
        msgModel.time = sendTime;
        msgModel.snapchatTime = snapchatTime;
        msgModel.chatType = chatType;

        if (refMessageBean != null) {
            RefMessageBean refMessage = new RefMessageBean();
            refMessage.msgId = refMessageBean.msgId;
            refMessage.content = refMessageBean.getRefContent();
            refMessage.type = refMessageBean.getRefMessageType();
            refMessage.uid = refMessageBean.ownerUid;
            refMessage.nickname = refMessageBean.ownerName;
            msgModel.refMsg = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(refMessage);
        }

        return msgModel;
    }

    public static MessageModel createImageMessage(String imageFileUri, String imageThumbFileUri, int width, int height, String attachmentKey, long sendTime,
                                                  MessageModel refMessageBean, int snapchatTime, long senderId, long targetId, int chatType) {
        ImageMessageContentBean imageMessageContentBean = new ImageMessageContentBean();
        imageMessageContentBean.width = width;
        imageMessageContentBean.height = height;
        if (imageFileUri.startsWith("file://")) {
            imageMessageContentBean.imageFileBackupUri = imageFileUri;
        } else {
            imageMessageContentBean.imageFileUri = imageFileUri;
        }
        if (imageThumbFileUri.startsWith("file://")) {
            imageMessageContentBean.imageThumbFileBackupUri = imageThumbFileUri;
        } else {
            imageMessageContentBean.imageThumbFileUri = imageThumbFileUri;
        }

        MessageModel msgModel = new MessageModel();
        msgModel.id = System.nanoTime();
        msgModel.flag = String.valueOf(System.nanoTime());
        msgModel.senderId = senderId;
        msgModel.targetId = targetId;
        msgModel.ownerUid = senderId;
        msgModel.type = MESSAGE_TYPE_IMAGE;
        msgModel.isSend = 1;
        msgModel.attachmentKey = attachmentKey;
        msgModel.content = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(imageMessageContentBean);
        msgModel.status = MessageModel.STATUS_ATTACHMENT_PROCESSING;
        msgModel.time = sendTime;
        msgModel.snapchatTime = snapchatTime;
        msgModel.chatType = chatType;

        if (refMessageBean != null) {
            RefMessageBean refMessage = new RefMessageBean();
            refMessage.msgId = refMessageBean.msgId;
            refMessage.content = refMessageBean.getRefContent();
            refMessage.type = refMessageBean.getRefMessageType();
            refMessage.uid = refMessageBean.ownerUid;
            refMessage.nickname = refMessageBean.ownerName;
            msgModel.refMsg = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(refMessage);
        }

        return msgModel;
    }

    public static MessageModel createForwardImageMessage(String imageFileUri, String imageThumbFileUri, int width, int height, String attachmentKey, long sendTime,
                                                         long senderId, long targetId, int chatType, int snapchatTime) {
        ImageMessageContentBean imageMessageContentBean = new ImageMessageContentBean();
        imageMessageContentBean.imageFileUri = imageFileUri;
        imageMessageContentBean.imageThumbFileUri = imageThumbFileUri;
        imageMessageContentBean.width = width;
        imageMessageContentBean.height = height;

        MessageModel msgModel = new MessageModel();
        msgModel.id = System.nanoTime();
        msgModel.flag = String.valueOf(System.nanoTime());
        msgModel.senderId = senderId;
        msgModel.targetId = targetId;
        msgModel.ownerUid = senderId;
        msgModel.type = MESSAGE_TYPE_IMAGE;
        msgModel.snapchatTime = snapchatTime;
        msgModel.isSend = 1;
        msgModel.attachmentKey = attachmentKey;
        msgModel.content = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(imageMessageContentBean);
        msgModel.status = MessageModel.STATUS_SENDING;
        msgModel.time = sendTime;
        msgModel.chatType = chatType;

        return msgModel;
    }

    public static MessageModel createDynamicImageMessage(long emoticonId, String imageFileUri, String imageThumbFileUri, int width, int height, String attachmentKey, long sendTime,
                                                         MessageModel refMessageBean, int snapchatTime, long senderId, long targetId, int chatType) {
        ImageMessageContentBean imageMessageContentBean = new ImageMessageContentBean();
        imageMessageContentBean.emoticonId = emoticonId;
        imageMessageContentBean.width = width;
        imageMessageContentBean.height = height;
        if (imageFileUri.startsWith("file://")) {
            imageMessageContentBean.imageFileBackupUri = imageFileUri;
        } else {
            imageMessageContentBean.imageFileUri = imageFileUri;
        }
        if (imageThumbFileUri.startsWith("file://")) {
            imageMessageContentBean.imageThumbFileBackupUri = imageThumbFileUri;
        } else {
            imageMessageContentBean.imageThumbFileUri = imageThumbFileUri;
        }

        MessageModel msgModel = new MessageModel();
        msgModel.id = System.nanoTime();
        msgModel.flag = String.valueOf(System.nanoTime());
        msgModel.senderId = senderId;
        msgModel.targetId = targetId;
        msgModel.ownerUid = senderId;
        msgModel.type = MESSAGE_TYPE_DYNAMIC_IMAGE;
        msgModel.snapchatTime = snapchatTime;
        msgModel.isSend = 1;
        msgModel.attachmentKey = attachmentKey;
        msgModel.content = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(imageMessageContentBean);
        msgModel.status = MessageModel.STATUS_ATTACHMENT_PROCESSING;
        msgModel.time = sendTime;
        msgModel.snapchatTime = snapchatTime;
        msgModel.chatType = chatType;

        if (refMessageBean != null) {
            RefMessageBean refMessage = new RefMessageBean();
            refMessage.msgId = refMessageBean.msgId;
            refMessage.content = refMessageBean.getRefContent();
            refMessage.type = refMessageBean.getRefMessageType();
            refMessage.uid = refMessageBean.ownerUid;
            refMessage.nickname = refMessageBean.ownerName;
            msgModel.refMsg = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(refMessage);
        }

        return msgModel;
    }

    public static MessageModel createForwardDynamicImageMessage(long emoticonId, String imageFileUri, String imageThumbFileUri, String imageFileBackUri, int width, int height, String attachmentKey, long sendTime,
                                                                long senderId, long targetId, long size, int chatType, int snapchatTime) {
        ImageMessageContentBean imageMessageContentBean = new ImageMessageContentBean();
        imageMessageContentBean.emoticonId = emoticonId;
        imageMessageContentBean.imageFileUri = imageFileUri;
        imageMessageContentBean.imageThumbFileUri = imageThumbFileUri;
        imageMessageContentBean.imageFileBackupUri = imageFileBackUri;
        imageMessageContentBean.width = width;
        imageMessageContentBean.height = height;
        imageMessageContentBean.size = size;

        MessageModel msgModel = new MessageModel();
        msgModel.id = System.nanoTime();
        msgModel.flag = String.valueOf(System.nanoTime());
        msgModel.senderId = senderId;
        msgModel.targetId = targetId;
        msgModel.ownerUid = senderId;
        msgModel.snapchatTime = snapchatTime;
        msgModel.type = MESSAGE_TYPE_DYNAMIC_IMAGE;
        msgModel.isSend = 1;
        msgModel.attachmentKey = attachmentKey;
        msgModel.content = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(imageMessageContentBean);
        msgModel.status = MessageModel.STATUS_SENDING;
        msgModel.time = sendTime;
        msgModel.chatType = chatType;

        return msgModel;
    }

    public static MessageModel createVideoMessage(String videoFileUri, String videoThumbFileUri, int width, int height, int videoTime, String attachmentKey, long sendTime,
                                                  MessageModel refMessageBean, int snapchatTime, long senderId, long targetId, int chatType) {
        VideoMessageContentBean videoMessageContentBean = new VideoMessageContentBean();
        videoMessageContentBean.width = width;
        videoMessageContentBean.height = height;
        videoMessageContentBean.videoTime = videoTime;
        if (videoFileUri.startsWith("file://")) {
            videoMessageContentBean.videoFileBackupUri = videoFileUri;
        } else {
            videoMessageContentBean.videoFileUri = videoFileUri;
        }
        if (videoThumbFileUri.startsWith("file://")) {
            videoMessageContentBean.videoThumbFileBackupUri = videoThumbFileUri;
        } else {
            videoMessageContentBean.videoThumbFileUri = videoThumbFileUri;
        }

        MessageModel msgModel = new MessageModel();
        msgModel.id = System.nanoTime();
        msgModel.flag = String.valueOf(System.nanoTime());
        msgModel.senderId = senderId;
        msgModel.targetId = targetId;
        msgModel.ownerUid = senderId;
        msgModel.type = MESSAGE_TYPE_VIDEO;
        msgModel.isSend = 1;
        msgModel.attachmentKey = attachmentKey;
        msgModel.content = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(videoMessageContentBean);
        msgModel.status = MessageModel.STATUS_ATTACHMENT_PROCESSING;
        msgModel.time = sendTime;
        msgModel.snapchatTime = snapchatTime;
        msgModel.chatType = chatType;

        if (refMessageBean != null) {
            RefMessageBean refMessage = new RefMessageBean();
            refMessage.msgId = refMessageBean.msgId;
            refMessage.content = refMessageBean.getRefContent();
            refMessage.type = refMessageBean.getRefMessageType();
            refMessage.uid = refMessageBean.ownerUid;
            refMessage.nickname = refMessageBean.ownerName;
            msgModel.refMsg = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(refMessage);
        }

        return msgModel;
    }

    public static MessageModel createForwardVideoMessage(String videoFileUri, String videoThumbFileUri, int width, int height, int videoTime, String attachmentKey,
                                                         long sendTime, long senderId, long targetId, int chatType, int snapchatTime) {
        VideoMessageContentBean videoMessageContentBean = new VideoMessageContentBean();
        videoMessageContentBean.videoFileUri = videoFileUri;
        videoMessageContentBean.videoThumbFileUri = videoThumbFileUri;
        videoMessageContentBean.width = width;
        videoMessageContentBean.height = height;
        videoMessageContentBean.videoTime = videoTime;

        MessageModel msgModel = new MessageModel();
        msgModel.id = System.nanoTime();
        msgModel.flag = String.valueOf(System.nanoTime());
        msgModel.senderId = senderId;
        msgModel.targetId = targetId;
        msgModel.ownerUid = senderId;
        msgModel.type = MESSAGE_TYPE_VIDEO;
        msgModel.snapchatTime = snapchatTime;
        msgModel.isSend = 1;
        msgModel.attachmentKey = attachmentKey;
        msgModel.content = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(videoMessageContentBean);
        msgModel.status = MessageModel.STATUS_SENDING;
        msgModel.time = sendTime;
        msgModel.chatType = chatType;

        return msgModel;
    }

    public static MessageModel createStreamMessage(String sessionId, int streamType, int isSend, long sendTime, long senderId, long targetId, int chatType, int streamStatus) {
        StreamMessageContentBean streamMessageContentBean = new StreamMessageContentBean();
        streamMessageContentBean.status = streamStatus;
        streamMessageContentBean.streamType = streamType;
        streamMessageContentBean.startTime = 0;
        streamMessageContentBean.endTime = 0;
        streamMessageContentBean.isSend = isSend;

        MessageModel msgModel = new MessageModel();
        msgModel.id = System.nanoTime();
        msgModel.flag = sessionId;
        msgModel.senderId = senderId;
        msgModel.targetId = targetId;
        msgModel.ownerUid = senderId;
        msgModel.type = MESSAGE_TYPE_STREAM;
        msgModel.isSend = isSend;
        msgModel.content = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(streamMessageContentBean);
        msgModel.status = MessageModel.STATUS_SENDED_HAS_RESP;
        msgModel.time = sendTime;
        msgModel.isRead = 1;
        msgModel.chatType = chatType;
        return msgModel;
    }

    public static MessageModel createLocationMessage(long lat, long lng, String address, long sendTime, MessageModel refMessageBean, int snapchatTime, long senderId, long targetId, int chatType) {
        LocationMessageContentBean locationMessageContentBean = new LocationMessageContentBean();
        locationMessageContentBean.lat = lat;
        locationMessageContentBean.lng = lng;
        locationMessageContentBean.address = address;

        MessageModel msgModel = new MessageModel();
        msgModel.id = System.nanoTime();
        msgModel.flag = String.valueOf(System.nanoTime());
        msgModel.senderId = senderId;
        msgModel.targetId = targetId;
        msgModel.ownerUid = senderId;
        msgModel.type = MESSAGE_TYPE_LOCATION;
        msgModel.isSend = 1;
        msgModel.content = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(locationMessageContentBean);
        msgModel.status = MessageModel.STATUS_SENDING;
        msgModel.time = sendTime;
        msgModel.snapchatTime = snapchatTime;
        msgModel.chatType = chatType;

        if (refMessageBean != null) {
            RefMessageBean refMessage = new RefMessageBean();
            refMessage.msgId = refMessageBean.msgId;
            refMessage.content = refMessageBean.getRefContent();
            refMessage.type = refMessageBean.getRefMessageType();
            refMessage.uid = refMessageBean.ownerUid;
            refMessage.nickname = refMessageBean.ownerName;
            msgModel.refMsg = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(refMessage);
        }

        return msgModel;
    }

    public static MessageModel createFileMessage(String name, long size, String mimeType, String fileUri, String attachmentKey, long sendTime,
                                                 MessageModel refMessageBean, int snapchatTime, long senderId, long targetId, int chatType) {
        FileMessageContentBean fileMessageContentBean = new FileMessageContentBean();
        fileMessageContentBean.fileBackupUri = fileUri;
        fileMessageContentBean.name = name;
        fileMessageContentBean.size = size;
        fileMessageContentBean.mimeType = mimeType;
        if (fileUri.startsWith("file://")) {
            fileMessageContentBean.fileBackupUri = fileUri;
        } else {
            fileMessageContentBean.fileUri = fileUri;
        }

        MessageModel msgModel = new MessageModel();
        msgModel.id = System.nanoTime();
        msgModel.flag = String.valueOf(System.nanoTime());
        msgModel.senderId = senderId;
        msgModel.targetId = targetId;
        msgModel.ownerUid = senderId;
        msgModel.type = MESSAGE_TYPE_FILE;
        msgModel.isSend = 1;
        msgModel.attachmentKey = attachmentKey;
        msgModel.content = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(fileMessageContentBean);
        msgModel.status = MessageModel.STATUS_SENDING;
        msgModel.time = sendTime;
        msgModel.snapchatTime = snapchatTime;
        msgModel.chatType = chatType;

        if (refMessageBean != null) {
            RefMessageBean refMessage = new RefMessageBean();
            refMessage.msgId = refMessageBean.msgId;
            refMessage.content = refMessageBean.getRefContent();
            refMessage.type = refMessageBean.getRefMessageType();
            refMessage.uid = refMessageBean.ownerUid;
            refMessage.nickname = refMessageBean.ownerName;
            msgModel.refMsg = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(refMessage);
        }

        return msgModel;
    }

    public static MessageModel createForwardFileMessage(String name, long size, String mimeType, String fileUrl, String attachmentKey, long sendTime, long senderId, long targetId, int chatType, int snapchatTime) {
        FileMessageContentBean fileMessageContentBean = new FileMessageContentBean();
        fileMessageContentBean.fileUri = fileUrl;
        fileMessageContentBean.name = name;
        fileMessageContentBean.size = size;
        fileMessageContentBean.mimeType = mimeType;

        MessageModel msgModel = new MessageModel();
        msgModel.id = System.nanoTime();
        msgModel.flag = String.valueOf(System.nanoTime());
        msgModel.senderId = senderId;
        msgModel.targetId = targetId;
        msgModel.ownerUid = senderId;
        msgModel.type = MESSAGE_TYPE_FILE;
        msgModel.snapchatTime = snapchatTime;
        msgModel.isSend = 1;
        msgModel.attachmentKey = attachmentKey;
        msgModel.content = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(fileMessageContentBean);
        msgModel.status = MessageModel.STATUS_SENDING;
        msgModel.time = sendTime;
        msgModel.chatType = chatType;

        return msgModel;
    }

    public static MessageModel createUnKnowMessage(int type, byte[] contentBytes, String contentMd5, String attachmentKey, long sendTime, int snapchatTime, long senderId, long targetId, int chatType) {
        MessageModel msgModel = new MessageModel();
        msgModel.id = System.nanoTime();
        msgModel.flag = String.valueOf(System.nanoTime());
        msgModel.senderId = senderId;
        msgModel.targetId = targetId;
        msgModel.ownerUid = senderId;
        msgModel.originalType = type;
        msgModel.type = MESSAGE_TYPE_UNKNOW;
        msgModel.isSend = 1;
        msgModel.contentBytes = contentBytes;
        msgModel.contentMd5 = contentMd5;
        msgModel.attachmentKey = attachmentKey;
        msgModel.time = sendTime;
        msgModel.atUids = "";
        msgModel.isAtMe = 0;
        msgModel.snapchatTime = snapchatTime;
        msgModel.chatType = chatType;

        return msgModel;
    }

    public static MessageModel createUnDecryptMessage(int type, byte[] contentBytes, int keyVersion, String contentMd5, String attachmentKey, long sendTime,
                                                      int snapchatTime, long senderId, long targetId, int chatType) {
        MessageModel msgModel = new MessageModel();
        msgModel.id = System.nanoTime();
        msgModel.flag = String.valueOf(System.nanoTime());
        msgModel.senderId = senderId;
        msgModel.targetId = targetId;
        msgModel.ownerUid = senderId;
        msgModel.originalType = type;
        msgModel.type = MESSAGE_TYPE_UNDECRYPT;
        msgModel.isSend = 1;
        msgModel.contentBytes = contentBytes;
        msgModel.keyVersion = keyVersion;
        msgModel.contentMd5 = contentMd5;
        msgModel.attachmentKey = attachmentKey;
        msgModel.time = sendTime;
        msgModel.atUids = "";
        msgModel.isAtMe = 0;
        msgModel.snapchatTime = snapchatTime;
        msgModel.chatType = chatType;

        return msgModel;
    }

    public static MessageModel parseTextMessage(long msgId, String text, String contentMd5, long sendTime, long[] atUids, IMPB.ReferenceObj ref, int snapchatTime,
                                                long senderId, String ownerName, String ownerIcon, long ownerUid, long targetUid, int chatType) {
        MessageModel msgModel = new MessageModel();
        msgModel.id = System.nanoTime();
        msgModel.msgId = msgId;
        msgModel.senderId = senderId;
        msgModel.targetId = targetUid;
        msgModel.ownerUid = ownerUid;
        msgModel.ownerIcon = ownerIcon;
        msgModel.ownerName = ownerName;
        msgModel.type = MESSAGE_TYPE_TEXT;
        msgModel.content = text;
        msgModel.contentMd5 = contentMd5;
        msgModel.time = sendTime;
        msgModel.snapchatTime = snapchatTime;
        msgModel.chatType = chatType;

        if (ref != null && ref.getMsgId() > 0) {
            RefMessageBean refMessage = new RefMessageBean();
            refMessage.msgId = ref.getMsgId();
            refMessage.content = ref.getContent();
            refMessage.type = ref.getTypeValue();
            refMessage.uid = ref.getUid();
            refMessage.nickname = ref.getNickname();
            msgModel.refMsg = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(refMessage);
        }

        boolean isAtMe = false;
        StringBuilder sb = new StringBuilder();
        if (atUids != null && atUids.length > 0) {
            long firstAtUid = atUids[0];
            if (firstAtUid == -1) {
                isAtMe = true;
                sb.append(firstAtUid);
            } else {
                if (firstAtUid == targetUid) {
                    isAtMe = true;
                }

                sb.append(firstAtUid);
                for (int i = 1; i < atUids.length; i++) {
                    long atUid = atUids[i];
                    if (atUid == targetUid) {
                        isAtMe = true;
                    }
                    sb.append(",").append(atUid);
                }
            }
        }
        msgModel.atUids = sb.toString();
        msgModel.isAtMe = isAtMe ? 1 : 0;

        return msgModel;
    }

    public static MessageModel parseNoticeMessage(long msgId, long noticeId, String noticeContent, boolean showNotify, String contentMd5, long sendTime,
                                                  long senderId, String ownerName, String ownerIcon, long ownerUid, long targetUid, int chatType) {
        NoticeMessageBean noticeMessageBean = new NoticeMessageBean();
        noticeMessageBean.noticeId = noticeId;
        noticeMessageBean.content = noticeContent;
        noticeMessageBean.showNotify = showNotify;

        MessageModel msgModel = new MessageModel();
        msgModel.id = System.nanoTime();
        msgModel.msgId = msgId;
        msgModel.senderId = senderId;
        msgModel.targetId = targetUid;
        msgModel.ownerUid = ownerUid;
        msgModel.ownerIcon = ownerIcon;
        msgModel.ownerName = ownerName;
        msgModel.type = MESSAGE_TYPE_NOTICE;
        msgModel.content = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(noticeMessageBean);
        msgModel.contentMd5 = contentMd5;
        msgModel.time = sendTime;
        msgModel.chatType = chatType;

        return msgModel;
    }

    public static MessageModel parseNameCardMessage(long msgId, long uid, String nickName, String icon, String identify, String contentMd5, long sendTime, IMPB.ReferenceObj ref, int snapchatTime,
                                                    long senderId, String ownerName, String ownerIcon, long ownerUid, long targetUid, int chatType) {
        NameCardMessageContentBean nameCardMessageContentBean = new NameCardMessageContentBean();
        nameCardMessageContentBean.uid = uid;
        nameCardMessageContentBean.nickName = nickName;
        nameCardMessageContentBean.icon = icon;
        nameCardMessageContentBean.identify = identify;

        MessageModel msgModel = new MessageModel();
        msgModel.id = System.nanoTime();
        msgModel.msgId = msgId;
        msgModel.senderId = senderId;
        msgModel.targetId = targetUid;
        msgModel.ownerUid = ownerUid;
        msgModel.ownerIcon = ownerIcon;
        msgModel.ownerName = ownerName;
        msgModel.type = MESSAGE_TYPE_NAMECARD;
        msgModel.content = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(nameCardMessageContentBean);
        msgModel.contentMd5 = contentMd5;
        msgModel.time = sendTime;
        msgModel.snapchatTime = snapchatTime;
        msgModel.chatType = chatType;

        if (ref != null && ref.getMsgId() > 0) {
            RefMessageBean refMessage = new RefMessageBean();
            refMessage.msgId = ref.getMsgId();
            refMessage.content = ref.getContent();
            refMessage.type = ref.getTypeValue();
            refMessage.uid = ref.getUid();
            refMessage.nickname = ref.getNickname();
            msgModel.refMsg = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(refMessage);
        }

        return msgModel;
    }

    public static MessageModel parseVoiceMessage(long msgId, int recordTime, String recordFileUri, int[] highDArr, String contentMd5, String attachmentKey, long sendTime,
                                                 IMPB.ReferenceObj ref, int snapchatTime, long senderId, String ownerName, String ownerIcon, long ownerUid, long targetUid, int chatType) {
        VoiceMessageContentBean voiceMessageContentBean = new VoiceMessageContentBean();
        voiceMessageContentBean.recordTime = recordTime;
        voiceMessageContentBean.recordFileUri = recordFileUri;
        if (highDArr != null && highDArr.length > 100) {
            voiceMessageContentBean.highDArr = highDArr;
            voiceMessageContentBean.localHighDArr = AudioSampleUtils.INSTANCE.adjustedSamples(highDArr, recordTime);
        }

        MessageModel msgModel = new MessageModel();
        msgModel.id = System.nanoTime();
        msgModel.msgId = msgId;
        msgModel.senderId = senderId;
        msgModel.targetId = targetUid;
        msgModel.ownerUid = ownerUid;
        msgModel.ownerIcon = ownerIcon;
        msgModel.ownerName = ownerName;
        msgModel.type = MESSAGE_TYPE_VOICE;
        msgModel.content = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(voiceMessageContentBean);
        msgModel.contentMd5 = contentMd5;
        msgModel.attachmentKey = attachmentKey;
        msgModel.time = sendTime;
        msgModel.snapchatTime = snapchatTime;
        msgModel.chatType = chatType;

        if (ref != null && ref.getMsgId() > 0) {
            RefMessageBean refMessage = new RefMessageBean();
            refMessage.msgId = ref.getMsgId();
            refMessage.content = ref.getContent();
            refMessage.type = ref.getTypeValue();
            refMessage.uid = ref.getUid();
            refMessage.nickname = ref.getNickname();
            msgModel.refMsg = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(refMessage);
        }

        return msgModel;
    }

    public static MessageModel parseImageMessage(long msgId, String imageFileUri, String imageThumbFileUri, int width, int height, String contentMd5, String attachmentKey,
                                                 long sendTime, IMPB.ReferenceObj ref, int snapchatTime, long senderId, String ownerName, String ownerIcon, long ownerUid, long targetUid, int chatType) {
        ImageMessageContentBean imageMessageContentBean = new ImageMessageContentBean();
        imageMessageContentBean.imageFileUri = imageFileUri;
        imageMessageContentBean.imageThumbFileUri = imageThumbFileUri;
        imageMessageContentBean.width = width;
        imageMessageContentBean.height = height;

        MessageModel msgModel = new MessageModel();
        msgModel.id = System.nanoTime();
        msgModel.msgId = msgId;
        msgModel.senderId = senderId;
        msgModel.targetId = targetUid;
        msgModel.ownerUid = ownerUid;
        msgModel.ownerIcon = ownerIcon;
        msgModel.ownerName = ownerName;
        msgModel.type = MESSAGE_TYPE_IMAGE;
        msgModel.content = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(imageMessageContentBean);
        msgModel.contentMd5 = contentMd5;
        msgModel.attachmentKey = attachmentKey;
        msgModel.time = sendTime;
        msgModel.snapchatTime = snapchatTime;
        msgModel.chatType = chatType;

        if (ref != null && ref.getMsgId() > 0) {
            RefMessageBean refMessage = new RefMessageBean();
            refMessage.msgId = ref.getMsgId();
            refMessage.content = ref.getContent();
            refMessage.type = ref.getTypeValue();
            refMessage.uid = ref.getUid();
            refMessage.nickname = ref.getNickname();
            msgModel.refMsg = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(refMessage);
        }

        return msgModel;
    }

    public static MessageModel parseDynamicImageMessage(long msgId, long emoticonId, String imageFileUri, String imageThumbFileUri, int width, int height, String contentMd5, String attachmentKey,
                                                        long sendTime, IMPB.ReferenceObj ref, int snapchatTime, long senderId, String ownerName, String ownerIcon, long ownerUid, long targetUid, long size, int chatType) {
        ImageMessageContentBean imageMessageContentBean = new ImageMessageContentBean();
        imageMessageContentBean.emoticonId = emoticonId;
        imageMessageContentBean.imageFileUri = imageFileUri;
        imageMessageContentBean.imageThumbFileUri = imageThumbFileUri;
        imageMessageContentBean.width = width;
        imageMessageContentBean.height = height;
        imageMessageContentBean.size = size;

        MessageModel msgModel = new MessageModel();
        msgModel.id = System.nanoTime();
        msgModel.msgId = msgId;
        msgModel.senderId = senderId;
        msgModel.targetId = targetUid;
        msgModel.ownerUid = ownerUid;
        msgModel.ownerIcon = ownerIcon;
        msgModel.ownerName = ownerName;
        msgModel.type = MESSAGE_TYPE_DYNAMIC_IMAGE;
        msgModel.content = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(imageMessageContentBean);
        msgModel.contentMd5 = contentMd5;
        msgModel.attachmentKey = attachmentKey;
        msgModel.time = sendTime;
        msgModel.snapchatTime = snapchatTime;
        msgModel.chatType = chatType;

        if (ref != null && ref.getMsgId() > 0) {
            RefMessageBean refMessage = new RefMessageBean();
            refMessage.msgId = ref.getMsgId();
            refMessage.content = ref.getContent();
            refMessage.type = ref.getTypeValue();
            refMessage.uid = ref.getUid();
            refMessage.nickname = ref.getNickname();
            msgModel.refMsg = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(refMessage);
        }

        return msgModel;
    }

    public static MessageModel parseVideoMessage(long msgId, String videoFileUri, String videoThumbFileUri, int width, int height, int videoTime, String contentMd5,
                                                 String attachmentKey, long sendTime, IMPB.ReferenceObj ref, int snapchatTime, long senderId, String ownerName, String ownerIcon, long ownerUid, long targetUid, int chatType) {
        VideoMessageContentBean videoMessageContentBean = new VideoMessageContentBean();
        videoMessageContentBean.videoFileUri = videoFileUri;
        videoMessageContentBean.videoThumbFileUri = videoThumbFileUri;
        videoMessageContentBean.width = width;
        videoMessageContentBean.height = height;
        videoMessageContentBean.videoTime = videoTime;

        MessageModel msgModel = new MessageModel();
        msgModel.id = System.nanoTime();
        msgModel.msgId = msgId;
        msgModel.senderId = senderId;
        msgModel.targetId = targetUid;
        msgModel.ownerUid = ownerUid;
        msgModel.ownerIcon = ownerIcon;
        msgModel.ownerName = ownerName;
        msgModel.type = MESSAGE_TYPE_VIDEO;
        msgModel.content = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(videoMessageContentBean);
        msgModel.contentMd5 = contentMd5;
        msgModel.attachmentKey = attachmentKey;
        msgModel.time = sendTime;
        msgModel.snapchatTime = snapchatTime;
        msgModel.chatType = chatType;

        if (ref != null && ref.getMsgId() > 0) {
            RefMessageBean refMessage = new RefMessageBean();
            refMessage.msgId = ref.getMsgId();
            refMessage.content = ref.getContent();
            refMessage.type = ref.getTypeValue();
            refMessage.uid = ref.getUid();
            refMessage.nickname = ref.getNickname();
            msgModel.refMsg = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(refMessage);
        }

        return msgModel;
    }

    public static MessageModel parseLocationMessage(long msgId, String address, long lat, long lng, String contentMd5, long sendTime, IMPB.ReferenceObj ref,
                                                    int snapchatTime, long senderId, String ownerName, String ownerIcon, long ownerUid, long targetUid, int chatType) {
        LocationMessageContentBean locationMessageContentBean = new LocationMessageContentBean();
        locationMessageContentBean.lat = lat;
        locationMessageContentBean.lng = lng;
        locationMessageContentBean.address = address;

        MessageModel msgModel = new MessageModel();
        msgModel.id = System.nanoTime();
        msgModel.msgId = msgId;
        msgModel.senderId = senderId;
        msgModel.targetId = targetUid;
        msgModel.ownerUid = ownerUid;
        msgModel.ownerIcon = ownerIcon;
        msgModel.ownerName = ownerName;
        msgModel.type = MESSAGE_TYPE_LOCATION;
        msgModel.content = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(locationMessageContentBean);
        msgModel.contentMd5 = contentMd5;
        msgModel.time = sendTime;
        msgModel.snapchatTime = snapchatTime;
        msgModel.chatType = chatType;

        if (ref != null && ref.getMsgId() > 0) {
            RefMessageBean refMessage = new RefMessageBean();
            refMessage.msgId = ref.getMsgId();
            refMessage.content = ref.getContent();
            refMessage.type = ref.getTypeValue();
            refMessage.uid = ref.getUid();
            refMessage.nickname = ref.getNickname();
            msgModel.refMsg = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(refMessage);
        }

        return msgModel;
    }

    public static MessageModel parseFileMessage(long msgId, String fileUri, String name, String mimeType, long size, String contentMd5, String attachmentKey,
                                                long sendTime, IMPB.ReferenceObj ref, int snapchatTime, long senderId, String ownerName, String ownerIcon, long ownerUid, long targetUid, int chatType) {
        FileMessageContentBean fileMessageContentBean = new FileMessageContentBean();
        fileMessageContentBean.size = size;
        fileMessageContentBean.fileUri = fileUri;
        fileMessageContentBean.name = name;
        fileMessageContentBean.mimeType = mimeType;

        MessageModel msgModel = new MessageModel();
        msgModel.id = System.nanoTime();
        msgModel.msgId = msgId;
        msgModel.senderId = senderId;
        msgModel.targetId = targetUid;
        msgModel.ownerUid = ownerUid;
        msgModel.ownerIcon = ownerIcon;
        msgModel.ownerName = ownerName;
        msgModel.type = MESSAGE_TYPE_FILE;
        msgModel.content = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(fileMessageContentBean);
        msgModel.contentMd5 = contentMd5;
        msgModel.attachmentKey = attachmentKey;
        msgModel.time = sendTime;
        msgModel.snapchatTime = snapchatTime;
        msgModel.chatType = chatType;

        if (ref != null && ref.getMsgId() > 0) {
            RefMessageBean refMessage = new RefMessageBean();
            refMessage.msgId = ref.getMsgId();
            refMessage.content = ref.getContent();
            refMessage.type = ref.getTypeValue();
            refMessage.uid = ref.getUid();
            refMessage.nickname = ref.getNickname();
            msgModel.refMsg = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(refMessage);
        }

        return msgModel;
    }

    public static MessageModel parseSystemTipMessage(long msgId, String msg, long sendTime, long senderId, String ownerName, String ownerIcon, long ownerUid, long targetUid, int chatType) {
        TipMessageContentBean tipMessageContentBean = new TipMessageContentBean();
        tipMessageContentBean.msgTip = msg;

        MessageModel msgModel = new MessageModel();
        msgModel.id = System.nanoTime();
        msgModel.msgId = msgId;
        msgModel.senderId = senderId;
        msgModel.targetId = targetUid;
        msgModel.ownerUid = ownerUid;
        msgModel.ownerIcon = ownerIcon;
        msgModel.ownerName = ownerName;
        msgModel.type = MESSAGE_TYPE_SYSTEM_TIP;
        msgModel.content = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(tipMessageContentBean);
        msgModel.time = sendTime;
        msgModel.isRead = 1;
        msgModel.chatType = chatType;
        return msgModel;
    }

    public static MessageModel parseGroupTipMessage(String msg, long sendTime, long senderId, long ownerUid, long targetUid, int chatType) {
        TipMessageContentBean tipMessageContentBean = new TipMessageContentBean();
        tipMessageContentBean.msgTip = msg;

        MessageModel msgModel = new MessageModel();
        msgModel.id = System.nanoTime();
        msgModel.msgId = System.nanoTime();
        msgModel.senderId = senderId;
        msgModel.targetId = targetUid;
        msgModel.ownerUid = ownerUid;
        msgModel.type = MESSAGE_TYPE_GROUP_TIP;
        msgModel.content = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(tipMessageContentBean);
        msgModel.time = sendTime;
        msgModel.isRead = 1;
        msgModel.chatType = chatType;
        return msgModel;
    }

    public static MessageModel parseErrorTipMessage(String msg, long errorCode, long sendTime, long senderId, long ownerUid, long targetUid, int chatType) {
        TipMessageContentBean tipMessageContentBean = new TipMessageContentBean();
        tipMessageContentBean.errorCode = errorCode;
        tipMessageContentBean.msgTip = msg;

        MessageModel msgModel = new MessageModel();
        msgModel.id = System.nanoTime();
        msgModel.msgId = System.nanoTime();
        msgModel.senderId = senderId;
        msgModel.targetId = targetUid;
        msgModel.ownerUid = ownerUid;
        msgModel.type = MESSAGE_TYPE_ERROR_TIP;
        msgModel.content = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(tipMessageContentBean);
        msgModel.time = sendTime;
        msgModel.isRead = 1;
        msgModel.chatType = chatType;
        return msgModel;
    }

    public static MessageModel parseUnKnowMessage(long msgId, int type, byte[] contentBytes, String contentMd5, String attachmentKey, long sendTime,
                                                  int snapchatTime, long senderId, String ownerName, String ownerIcon, long ownerUid, long targetUid, int chatType) {
        MessageModel msgModel = new MessageModel();
        msgModel.id = System.nanoTime();
        msgModel.msgId = msgId;
        msgModel.senderId = senderId;
        msgModel.targetId = targetUid;
        msgModel.ownerUid = ownerUid;
        msgModel.ownerIcon = ownerIcon;
        msgModel.ownerName = ownerName;
        msgModel.originalType = type;
        msgModel.type = MESSAGE_TYPE_UNKNOW;
        msgModel.contentBytes = contentBytes;
        msgModel.contentMd5 = contentMd5;
        msgModel.attachmentKey = attachmentKey;
        msgModel.time = sendTime;
        msgModel.atUids = "";
        msgModel.isAtMe = 0;
        msgModel.snapchatTime = snapchatTime;
        msgModel.chatType = chatType;

        return msgModel;
    }

    public static MessageModel parseUnDecryptMessage(long msgId, int type, byte[] contentBytes, int keyVersion, String contentMd5, String attachmentKey, long sendTime,
                                                     int snapchatTime, long senderId, String ownerName, String ownerIcon, long ownerUid, long targetUid, int chatType) {
        MessageModel msgModel = new MessageModel();
        msgModel.id = System.nanoTime();
        msgModel.msgId = msgId;
        msgModel.senderId = senderId;
        msgModel.targetId = targetUid;
        msgModel.ownerUid = ownerUid;
        msgModel.ownerIcon = ownerIcon;
        msgModel.ownerName = ownerName;
        msgModel.originalType = type;
        msgModel.type = MESSAGE_TYPE_UNDECRYPT;
        msgModel.contentBytes = contentBytes;
        msgModel.keyVersion = keyVersion;
        msgModel.contentMd5 = contentMd5;
        msgModel.attachmentKey = attachmentKey;
        msgModel.time = sendTime;
        msgModel.atUids = "";
        msgModel.isAtMe = 0;
        msgModel.snapchatTime = snapchatTime;
        msgModel.chatType = chatType;

        return msgModel;
    }

    public MessageModel copyMessage() {
        MessageModel msgModel = new MessageModel();
        msgModel.id = id;
        msgModel.msgId = msgId;
        msgModel.flag = flag;
        msgModel.senderId = senderId;
        msgModel.targetId = targetId;
        msgModel.ownerUid = ownerUid;
        msgModel.type = type;
        msgModel.isSend = isSend;
        msgModel.content = content;
        msgModel.contentBytes = contentBytes;
        msgModel.status = status;
        msgModel.time = time;
        msgModel.atUids = atUids;
        msgModel.originalType = originalType;
        msgModel.isAtMe = isAtMe;
        msgModel.isRetry = isRetry;
        msgModel.isRead = isRead;
        msgModel.readTime = readTime;
        msgModel.isReadedAttachment = isReadedAttachment;
        msgModel.readedAttachmentTime = readedAttachmentTime;
        msgModel.isDeliver = isDeliver;
        msgModel.deliverTime = deliverTime;
        msgModel.contentMd5 = contentMd5;
        msgModel.attachmentKey = attachmentKey;
        msgModel.keyVersion = keyVersion;
        msgModel.ownerName = ownerName;
        msgModel.ownerIcon = ownerIcon;
        msgModel.refMsg = refMsg;
        msgModel.expireTime = expireTime;
        msgModel.snapchatTime = snapchatTime;
        msgModel.chatType = chatType;
        msgModel.receiptCount = receiptCount;

        msgModel.mImageMessageContentBean = mImageMessageContentBean;
        msgModel.mDynamicImageMessageContentBean = mDynamicImageMessageContentBean;
        msgModel.mVoiceMessageContentBean = mVoiceMessageContentBean;
        msgModel.mVideoMessageContentBean = mVideoMessageContentBean;
        msgModel.mStreamMessageContentBean = mStreamMessageContentBean;
        msgModel.mNameCardMessageContentBean = mNameCardMessageContentBean;
        msgModel.mTipMessageContentBean = mTipMessageContentBean;
        msgModel.mFileMessageContentBean = mFileMessageContentBean;
        msgModel.mLocationMessageContentBean = mLocationMessageContentBean;
        msgModel.mRefMessageBean = mRefMessageBean;
        msgModel.mNoticeMessageBean = mNoticeMessageBean;

        msgModel.setShowAlreadyRead(showAlreadyRead);
        return msgModel;
    }

    public boolean hasAttachment() {
        return type == MESSAGE_TYPE_IMAGE || type == MESSAGE_TYPE_DYNAMIC_IMAGE || type == MESSAGE_TYPE_VOICE || type == MESSAGE_TYPE_VIDEO || type == MESSAGE_TYPE_FILE;
    }

    public String getChatContentDescribe() {
        if (type == MESSAGE_TYPE_TEXT) {
            EmojiInformation emojiInformation = EmojiUtils.emojiInformation(content);
            if (emojiInformation.isOnlyEmojis) {
                return BaseApp.app.getString(R.string.expression_sign);
            } else {
                return content;
            }
        } else if (type == MESSAGE_TYPE_IMAGE) {
            return BaseApp.app.getString(R.string.picture_sign);
        } else if (type == MESSAGE_TYPE_VOICE) {
            return BaseApp.app.getString(R.string.voice_sign);
        } else if (type == MESSAGE_TYPE_VIDEO) {
            return BaseApp.app.getString(R.string.video_sign);
        } else if (type == MESSAGE_TYPE_STREAM) {
            if (getStreamMessageContent().streamType == 0) {
                return BaseApp.app.getString(R.string.voice_communication_sign);
            } else {
                return BaseApp.app.getString(R.string.video_call_sign);
            }
        } else if (type == MESSAGE_TYPE_LOCATION) {
            return BaseApp.app.getString(R.string.location_sign);
        } else if (type == MESSAGE_TYPE_FILE) {
            return BaseApp.app.getString(R.string.file_sign) + getFileMessageContentBean().name;
        } else if (type == MESSAGE_TYPE_NAMECARD) {
            return BaseApp.app.getString(R.string.business_card_sign);
        } else if (type == MESSAGE_TYPE_NOTICE) {
            return BaseApp.app.getString(R.string.group_of_announcement_sign);
        } else if (type == MESSAGE_TYPE_DYNAMIC_IMAGE) {
            return BaseApp.app.getString(R.string.picture_dynamic_sign);
        } else if (type == MESSAGE_TYPE_SYSTEM) {
            return getTipMessageContentBean().msgTip;
        } else if (type == MESSAGE_TYPE_RECALL) {
            return content;
        } else if (type == MESSAGE_TYPE_RECALL_SUCCESS) {
            return content;
        } else if (type == MESSAGE_TYPE_GROUP_TIP) {
            return getTipMessageContentBean().msgTip;
        } else if (type == MESSAGE_TYPE_SYSTEM_TIP) {
            return getTipMessageContentBean().msgTip;
        } else {
            return BaseApp.app.getString(R.string.unkown_message_sign);
        }
    }

    @PrimaryKey
    private long id = 0;

    @Index
    private long msgId = 0;//服务器消息id  (等于0代表没发成功)

    @Index
    private String flag = "";//随机id(普通消息为本地生成，流媒体消息为channelName，错误消息为错误号码)

    private long senderId = 0;//发送方的uid（可能是群gid，也可能是用户uid）

    private long targetId = 0;//对方的id（可能是群成员uid，也可能是联系人uid）

    private long ownerUid = 0;//此信息发送者的id（可能是群成员uid，也可能是联系人uid）

    private String ownerName;//此信息发送者的名字

    private String ownerIcon;//此信息发送者的头像

    @Index
    private int type = MESSAGE_TYPE_TEXT;//消息类型

    private int originalType = 0;//原始消息类型

    private int keyVersion = 0;

    private int isSend = 0;//我是否是发送方

    private long time = 0;//时间

    private String content = "";//消息内容

    private String refMsg = "";//引用消息内容

    private String atUids = "";//@的人

    @Index
    private int isAtMe;//是否@了我

    private byte[] contentBytes;//无法解析的消息内容

    @Index
    private int status = 0;//消息状态 -1发送失败 0发送中 1已发送但服务器未答复 2已发送且服务器已答复 99附件处理中

    private int isRetry = 0;//是否重发

    private int isDeliver = 0;//是否已送达

    private long deliverTime = 0L;//送达时间

    @Index
    private int isRead = 0;//是否已读

    private long readTime = 0L;//已读时间

    @Index
    private int isReadedAttachment = 0;//是否已打开附件

    private long readedAttachmentTime = 0;

    private String contentMd5;//content md5值

    private String attachmentKey;//附件解密key

    private int snapchatTime;//自动销毁时间

    private long expireTime;//过期时间

    private boolean showAlreadyRead = true;//此条消息是否展示已读状态，由发这条消息时，私聊双方的‘已读回执’开关决定

    private int chatType;//会话类型 从v1.3.1版本开始，旧版本没有，默认值0

    private int receiptCount;//需要收集的回执数量

    public int getChatType() {
        return chatType;
    }

    public void setChatType(int chatType) {
        this.chatType = chatType;
    }

    public boolean isShowAlreadyRead() {
        return showAlreadyRead;
    }

    public void setShowAlreadyRead(boolean showAlreadyRead) {
        this.showAlreadyRead = showAlreadyRead;
    }

    public long getDeliverTime() {
        return deliverTime;
    }

    public void setDeliverTime(long deliverTime) {
        this.deliverTime = deliverTime;
    }

    public int getIsDeliver() {
        return isDeliver;
    }

    public void setIsDeliver(int isDeliver) {
        this.isDeliver = isDeliver;
    }

    public void setReadTime(long readTime) {
        this.readTime = readTime;
    }

    public long getReadTime() {
        return readTime;
    }

    public void setKeyVersion(int keyVersion) {
        this.keyVersion = keyVersion;
    }

    public int getKeyVersion() {
        return keyVersion;
    }

    public void setOriginalType(int originalType) {
        this.originalType = originalType;
    }

    public int getOriginalType() {
        return originalType;
    }

    public void setReadedAttachmentTime(long readedAttachmentTime) {
        this.readedAttachmentTime = readedAttachmentTime;
    }

    public long getReadedAttachmentTime() {
        return readedAttachmentTime;
    }

    public void setContentMd5(String contentMd5) {
        this.contentMd5 = contentMd5;
    }

    public String getContentMd5() {
        return contentMd5;
    }

    public void setAttachmentKey(String attachmentKey) {
        this.attachmentKey = attachmentKey;
    }

    public String getAttachmentKey() {
        return attachmentKey;
    }

    public int getReceiptCount() {
        return receiptCount;
    }

    public void setReceiptCount(int receiptCount) {
        this.receiptCount = receiptCount;
    }

    public int getDurationTime() {
        if (type == MESSAGE_TYPE_VOICE) {
            return getVoiceMessageContent().recordTime;
        } else if (type == MESSAGE_TYPE_VIDEO) {
            return getVideoMessageContent().videoTime;
        } else {
            return 0;
        }
    }

    public int getOriginalMessageType() {
        if (type == MESSAGE_TYPE_TEXT) {
            return IMPB.MessageType.text_VALUE;
        } else if (type == MESSAGE_TYPE_IMAGE) {
            return IMPB.MessageType.image_VALUE;
        } else if (type == MESSAGE_TYPE_VOICE) {
            return IMPB.MessageType.audio_VALUE;
        } else if (type == MESSAGE_TYPE_VIDEO) {
            return IMPB.MessageType.video_VALUE;
        } else if (type == MESSAGE_TYPE_NAMECARD) {
            return IMPB.MessageType.nameCard_VALUE;
        } else if (type == MESSAGE_TYPE_LOCATION) {
            return IMPB.MessageType.location_VALUE;
        } else if (type == MESSAGE_TYPE_SYSTEM) {
            return IMPB.MessageType.system_VALUE;
        } else if (type == MESSAGE_TYPE_FILE) {
            return IMPB.MessageType.file_VALUE;
        } else if (type == MESSAGE_TYPE_NOTICE) {
            return IMPB.MessageType.notice_VALUE;
        } else if (type == MESSAGE_TYPE_DYNAMIC_IMAGE) {
            return IMPB.MessageType.dynamicImage_VALUE;
        } else if (type == MESSAGE_TYPE_UNDECRYPT) {
            return originalType;
        } else if (type == MESSAGE_TYPE_UNKNOW) {
            return originalType;
        } else {
            return -1;
        }
    }

    @Override
    public int getItemType() {
        if (expireTime > 0L && System.currentTimeMillis() >= expireTime && chatType == 0) {//如果是群聊消息，不会消失，只有私聊或者旧版本才会消失，兼容旧版本，旧版chatType == 0
            // 如果消息为阅后即焚消息且已过期，则消息类型变为已撤回的消息(UI表现形式为消失不见)
            if (isSend == 1) {
                return LOCAL_TYPE_MYSELF_RECALL;
            } else {
                return LOCAL_TYPE_OTHER_RECALL;
            }
        } else {
            if (isSend == 1) {
                if (type == MESSAGE_TYPE_TEXT) {
                    return LOCAL_TYPE_MYSELF_TEXT;
                } else if (type == MESSAGE_TYPE_IMAGE) {
                    return LOCAL_TYPE_MYSELF_IMAGE;
                } else if (type == MESSAGE_TYPE_VOICE) {
                    return LOCAL_TYPE_MYSELF_VOICE;
                } else if (type == MESSAGE_TYPE_VIDEO) {
                    return LOCAL_TYPE_MYSELF_VIDEO;
                } else if (type == MESSAGE_TYPE_STREAM) {
                    return LOCAL_TYPE_MYSELF_STREAM_MEDIA;
                } else if (type == MESSAGE_TYPE_NAMECARD) {
                    return LOCAL_TYPE_MYSELF_NAMECARD;
                } else if (type == MESSAGE_TYPE_LOCATION) {
                    return LOCAL_TYPE_MYSELF_LOCATION;
                } else if (type == MESSAGE_TYPE_FILE) {
                    return LOCAL_TYPE_MYSELF_FILE;
                } else if (type == MESSAGE_TYPE_NOTICE) {
                    return LOCAL_TYPE_MYSELF_NOTICE;
                } else if (type == MESSAGE_TYPE_DYNAMIC_IMAGE) {
                    return LOCAL_TYPE_MYSELF_DYNAMIC_IMAGE;
                } else if (type == MESSAGE_TYPE_RECALL) {
                    return LOCAL_TYPE_MYSELF_RECALL;
                } else if (type == MESSAGE_TYPE_RECALL_SUCCESS) {
                    return LOCAL_TYPE_MYSELF_RECALL;
                } else if (type == MESSAGE_TYPE_SYSTEM_TIP) {
                    return LOCAL_TYPE_TIP;
                } else if (type == MESSAGE_TYPE_ERROR_TIP) {
                    return LOCAL_TYPE_TIP;
                } else if (type == MESSAGE_TYPE_UNDECRYPT) {
                    return LOCAL_TYPE_MYSELF_UNDECRYPT;
                } else {
                    return LOCAL_TYPE_MYSELF_UNKNOW;
                }
            } else {
                if (type == MESSAGE_TYPE_TEXT) {
                    return LOCAL_TYPE_OTHER_TEXT;
                } else if (type == MESSAGE_TYPE_IMAGE) {
                    return LOCAL_TYPE_OTHER_IMAGE;
                } else if (type == MESSAGE_TYPE_VOICE) {
                    return LOCAL_TYPE_OTHER_VOICE;
                } else if (type == MESSAGE_TYPE_VIDEO) {
                    return LOCAL_TYPE_OTHER_VIDEO;
                } else if (type == MESSAGE_TYPE_STREAM) {
                    return LOCAL_TYPE_OTHER_STREAM_MEDIA;
                } else if (type == MESSAGE_TYPE_NAMECARD) {
                    return LOCAL_TYPE_OTHER_NAMECARD;
                } else if (type == MESSAGE_TYPE_LOCATION) {
                    return LOCAL_TYPE_OTHER_LOCATION;
                } else if (type == MESSAGE_TYPE_FILE) {
                    return LOCAL_TYPE_OTHER_FILE;
                } else if (type == MESSAGE_TYPE_NOTICE) {
                    return LOCAL_TYPE_OTHER_NOTICE;
                } else if (type == MESSAGE_TYPE_DYNAMIC_IMAGE) {
                    return LOCAL_TYPE_OTHER_DYNAMIC_IMAGE;
                } else if (type == MESSAGE_TYPE_RECALL) {
                    return LOCAL_TYPE_OTHER_RECALL;
                } else if (type == MESSAGE_TYPE_RECALL_SUCCESS) {
                    return LOCAL_TYPE_OTHER_RECALL;
                } else if (type == MESSAGE_TYPE_SYSTEM_TIP) {
                    return LOCAL_TYPE_TIP;
                } else if (type == MESSAGE_TYPE_GROUP_TIP) {
                    return LOCAL_TYPE_TIP;
                } else if (type == MESSAGE_TYPE_ERROR_TIP) {
                    return LOCAL_TYPE_TIP;
                } else if (type == MESSAGE_TYPE_UNDECRYPT) {
                    return LOCAL_TYPE_OTHER_UNDECRYPT;
                } else {
                    return LOCAL_TYPE_OTHER_UNKNOW;
                }
            }
        }
    }

    public int getRefMessageType() {
        if (type == MESSAGE_TYPE_TEXT) {
            return IMPB.MessageType.text_VALUE;
        } else if (type == MESSAGE_TYPE_IMAGE) {
            return IMPB.MessageType.image_VALUE;
        } else if (type == MESSAGE_TYPE_VOICE) {
            return IMPB.MessageType.audio_VALUE;
        } else if (type == MESSAGE_TYPE_VIDEO) {
            return IMPB.MessageType.video_VALUE;
        } else if (type == MESSAGE_TYPE_NAMECARD) {
            return IMPB.MessageType.nameCard_VALUE;
        } else if (type == MESSAGE_TYPE_LOCATION) {
            return IMPB.MessageType.location_VALUE;
        } else if (type == MESSAGE_TYPE_FILE) {
            return IMPB.MessageType.file_VALUE;
        } else if (type == MESSAGE_TYPE_NOTICE) {
            return IMPB.MessageType.notice_VALUE;
        } else if (type == MESSAGE_TYPE_DYNAMIC_IMAGE) {
            return IMPB.MessageType.dynamicImage_VALUE;
        } else {
            return -1;
        }
    }

    public String getRefContent() {
        if (type == MESSAGE_TYPE_TEXT) {
            return getTextMessageContent();
        } else if (type == MESSAGE_TYPE_NOTICE) {
            return BaseApp.app.getString(R.string.group_of_announcement_sign);
        } else if (type == MESSAGE_TYPE_FILE) {
            return getFileMessageContentBean().name;
        } else if (type == MESSAGE_TYPE_VOICE) {
            return BaseApp.app.getString(R.string.voice_sign);
        } else if (type == MESSAGE_TYPE_IMAGE) {
            return BaseApp.app.getString(R.string.picture_sign);
        } else if (type == MESSAGE_TYPE_VIDEO) {
            return BaseApp.app.getString(R.string.video);
        } else if (type == MESSAGE_TYPE_NAMECARD) {
            return BaseApp.app.getString(R.string.business_card_sign);
        } else if (type == MESSAGE_TYPE_LOCATION) {
            return BaseApp.app.getString(R.string.geographic_position_sign);
        } else if (type == MESSAGE_TYPE_DYNAMIC_IMAGE) {
            return BaseApp.app.getString(R.string.picture_dynamic_sign);
        } else {
            return "";
        }
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public long getMsgId() {
        return msgId;
    }

    public void setMsgId(long msgId) {
        this.msgId = msgId;
    }

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    public long getSenderId() {
        return senderId;
    }

    public void setSenderId(long senderId) {
        this.senderId = senderId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerIcon() {
        return ownerIcon;
    }

    public void setOwnerIcon(String ownerIcon) {
        this.ownerIcon = ownerIcon;
    }

    public long getTargetId() {
        return targetId;
    }

    public void setTargetId(long targetId) {
        this.targetId = targetId;
    }

    public long getOwnerUid() {
        return ownerUid;
    }

    public void setOwnerUid(long ownerUid) {
        this.ownerUid = ownerUid;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getIsSend() {
        return isSend;
    }

    public void setIsSend(int isSend) {
        this.isSend = isSend;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getIsRead() {
        return isRead;
    }

    public void setIsRead(int isRead) {
        this.isRead = isRead;
    }

    public byte[] getContentBytes() {
        return contentBytes;
    }

    public void setContentBytes(byte[] contentBytes) {
        this.contentBytes = contentBytes;
    }

    public String getAtUids() {
        return atUids;
    }

    public void setAtUids(String atUids) {
        this.atUids = atUids;
    }

    public int getIsAtMe() {
        return isAtMe;
    }

    public void setIsAtMe(int isAtMe) {
        this.isAtMe = isAtMe;
    }

    public int getIsRetry() {
        return isRetry;
    }

    public void setIsRetry(int isRetry) {
        this.isRetry = isRetry;
    }

    public int isReadedAttachment() {
        return isReadedAttachment;
    }

    public void setReadedAttachment(int isReadedAttachment) {
        this.isReadedAttachment = isReadedAttachment;
    }

    public String getRefMsg() {
        return refMsg;
    }

    public void setRefMsg(String refMsg) {
        this.refMsg = refMsg;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public void setSnapchatTime(int snapchatTime) {
        this.snapchatTime = snapchatTime;
    }

    public int getSnapchatTime() {
        return snapchatTime;
    }

    @Ignore
    private VoiceMessageContentBean mVoiceMessageContentBean = null;
    @Ignore
    private ImageMessageContentBean mImageMessageContentBean = null;
    @Ignore
    private VideoMessageContentBean mVideoMessageContentBean = null;
    @Ignore
    private StreamMessageContentBean mStreamMessageContentBean = null;
    @Ignore
    private NameCardMessageContentBean mNameCardMessageContentBean = null;
    @Ignore
    private TipMessageContentBean mTipMessageContentBean = null;
    @Ignore
    private FileMessageContentBean mFileMessageContentBean = null;
    @Ignore
    private LocationMessageContentBean mLocationMessageContentBean = null;
    @Ignore
    private RefMessageBean mRefMessageBean = null;
    @Ignore
    private NoticeMessageBean mNoticeMessageBean = null;
    @Ignore
    private ImageMessageContentBean mDynamicImageMessageContentBean = null;

    public String getTextMessageContent() {
        return content;
    }

    public RefMessageBean getRefMessageBean() {
        if (mRefMessageBean == null && !TextUtils.isEmpty(refMsg)) {
            mRefMessageBean = fromJson(refMsg, RefMessageBean.class);
        }
        return mRefMessageBean;
    }

    public void setRefMessageBean(RefMessageBean refMessageBean) {
        content = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(refMessageBean);
        mRefMessageBean = null;
        mRefMessageBean = getRefMessageBean();
    }

    public VoiceMessageContentBean getVoiceMessageContent() {
        if (mVoiceMessageContentBean == null && !TextUtils.isEmpty(content)) {
            mVoiceMessageContentBean = fromJson(content, VoiceMessageContentBean.class);
        }
        return mVoiceMessageContentBean;
    }

    public void setVoiceMessageContent(VoiceMessageContentBean voiceMessageContent) {
        content = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(voiceMessageContent);
        mVoiceMessageContentBean = null;
        mVoiceMessageContentBean = getVoiceMessageContent();
    }

    public ImageMessageContentBean getImageMessageContent() {
        if (mImageMessageContentBean == null && !TextUtils.isEmpty(content)) {
            mImageMessageContentBean = fromJson(content, ImageMessageContentBean.class);
        }
        return mImageMessageContentBean;
    }

    public void setImageMessageContent(ImageMessageContentBean imageMessageContent) {
        content = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(imageMessageContent);
        mImageMessageContentBean = null;
        mImageMessageContentBean = getImageMessageContent();
    }

    public VideoMessageContentBean getVideoMessageContent() {
        if (mVideoMessageContentBean == null && !TextUtils.isEmpty(content)) {
            mVideoMessageContentBean = fromJson(content, VideoMessageContentBean.class);
        }
        return mVideoMessageContentBean;
    }

    public void setVideoMessageContent(VideoMessageContentBean videoMessageContent) {
        content = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(videoMessageContent);
        mVideoMessageContentBean = null;
        mVideoMessageContentBean = getVideoMessageContent();
    }

    public StreamMessageContentBean getStreamMessageContent() {
        if (mStreamMessageContentBean == null && !TextUtils.isEmpty(content)) {
            mStreamMessageContentBean = fromJson(content, StreamMessageContentBean.class);
        }
        return mStreamMessageContentBean;
    }

    public void setStreamMessageContent(StreamMessageContentBean streamMessageContent) {
        content = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(streamMessageContent);
        mStreamMessageContentBean = null;
        mStreamMessageContentBean = getStreamMessageContent();
    }

    public NameCardMessageContentBean getNameCardMessageContent() {
        if (mNameCardMessageContentBean == null && !TextUtils.isEmpty(content)) {
            mNameCardMessageContentBean = fromJson(content, NameCardMessageContentBean.class);
        }
        return mNameCardMessageContentBean;
    }

    public void setNameCardMessageContent(NameCardMessageContentBean nameCardMessageContentBean) {
        content = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(nameCardMessageContentBean);
        mNameCardMessageContentBean = null;
        mNameCardMessageContentBean = getNameCardMessageContent();
    }

    public TipMessageContentBean getTipMessageContentBean() {
        if (mTipMessageContentBean == null && !TextUtils.isEmpty(content)) {
            mTipMessageContentBean = fromJson(content, TipMessageContentBean.class);
        }
        return mTipMessageContentBean;
    }

    public void setTipMessageContentBean(TipMessageContentBean tipMessageContentBean) {
        content = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(tipMessageContentBean);
        mTipMessageContentBean = null;
        mTipMessageContentBean = getTipMessageContentBean();
    }

    public FileMessageContentBean getFileMessageContentBean() {
        if (mFileMessageContentBean == null && !TextUtils.isEmpty(content)) {
            mFileMessageContentBean = fromJson(content, FileMessageContentBean.class);
        }
        return mFileMessageContentBean;
    }

    public void setFileMessageContentBean(FileMessageContentBean fileMessageContentBean) {
        content = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(fileMessageContentBean);
        mFileMessageContentBean = null;
        mFileMessageContentBean = getFileMessageContentBean();
    }

    public LocationMessageContentBean getLocationMessageContentBean() {
        if (mLocationMessageContentBean == null && !TextUtils.isEmpty(content)) {
            mLocationMessageContentBean = fromJson(content, LocationMessageContentBean.class);
        }
        return mLocationMessageContentBean;
    }

    public void setLocationMessageContentBean(LocationMessageContentBean locationMessageContentBean) {
        content = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(locationMessageContentBean);
        mLocationMessageContentBean = null;
        mLocationMessageContentBean = getLocationMessageContentBean();
    }

    public NoticeMessageBean getNoticeMessageBean() {
        if (mNoticeMessageBean == null && !TextUtils.isEmpty(content)) {
            mNoticeMessageBean = fromJson(content, NoticeMessageBean.class);
        }
        return mNoticeMessageBean;
    }

    public void setNoticeMessageBean(NoticeMessageBean noticeMessageBean) {
        content = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(noticeMessageBean);
        mNoticeMessageBean = null;
        mNoticeMessageBean = getNoticeMessageBean();
    }

    public ImageMessageContentBean getDynamicImageMessageBean() {
        if (mDynamicImageMessageContentBean == null && !TextUtils.isEmpty(content)) {
            mDynamicImageMessageContentBean = fromJson(content, ImageMessageContentBean.class);
        }
        return mDynamicImageMessageContentBean;
    }

    public void setDynamicImageMessageBean(ImageMessageContentBean imageMessageContent) {
        content = GsonInstanceCreater.INSTANCE.getDefaultGson().toJson(imageMessageContent);
        mDynamicImageMessageContentBean = null;
        mDynamicImageMessageContentBean = getDynamicImageMessageBean();
    }

    public String getSimpleContent() {
        if (type == MESSAGE_TYPE_TEXT) {
            return content;
        } else if (type == MESSAGE_TYPE_VOICE) {
            return getVoiceMessageContent().recordFileUri.replace("http://", "");
        } else if (type == MESSAGE_TYPE_IMAGE) {
            return getImageMessageContent().imageFileUri.replace("http://", "");
        } else if (type == MESSAGE_TYPE_VIDEO) {
            return getVideoMessageContent().videoFileUri.replace("http://", "");
        } else if (type == MESSAGE_TYPE_NAMECARD) {
            return String.valueOf(getNameCardMessageContent().uid);
        } else if (type == MESSAGE_TYPE_LOCATION) {
            return getLocationMessageContentBean().lat + "," + getLocationMessageContentBean().lng;
        } else if (type == MESSAGE_TYPE_FILE) {
            return getFileMessageContentBean().fileUri.replace("http://", "");
        } else if (type == MESSAGE_TYPE_NOTICE) {
            return content;
        } else if (type == MESSAGE_TYPE_DYNAMIC_IMAGE) {
            return getImageMessageContent().imageFileUri.replace("http://", "");
        } else {
            return "";
        }
    }

    private <T> T fromJson(String json, Class<T> classOfT) {
        try {
            return GsonInstanceCreater.INSTANCE.getDefaultGson().fromJson(json, classOfT);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return ((MessageModel) obj).msgId == msgId &&
                ((MessageModel) obj).id == id;
    }
}