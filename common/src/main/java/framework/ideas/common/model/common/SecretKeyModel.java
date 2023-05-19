package framework.ideas.common.model.common;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class SecretKeyModel extends RealmObject {

    public static SecretKeyModel createUserSecretKeyModel(long targetGid, String publicKey, int keyVersion, boolean isWeb, long time) {
        SecretKeyModel model = new SecretKeyModel();
        model.id = System.nanoTime();
        model.targetType = 0;
        model.targetId = targetGid;
        model.publicKey = publicKey;
        model.keyVersion = keyVersion;
        model.isWeb = isWeb;
        model.time = time;
        return model;
    }

    public static SecretKeyModel createGroupSecretKeyModel(long targetUid, String publicKey, int keyVersion, String secretKey, long time) {
        SecretKeyModel model = new SecretKeyModel();
        model.id = System.nanoTime();
        model.targetType = 1;
        model.targetId = targetUid;
        model.publicKey = publicKey;
        model.keyVersion = keyVersion;
        model.secretKey = secretKey;
        model.isWeb = false;
        model.time = time;
        return model;
    }

    public SecretKeyModel copySecretKeyModel() {
        SecretKeyModel model = new SecretKeyModel();
        model.id = id;
        model.targetType = targetType;
        model.targetId = targetId;
        model.publicKey = publicKey;
        model.keyVersion = keyVersion;
        model.secretKey = secretKey;
        model.isWeb = isWeb;
        model.time = time;
        return model;
    }

    @PrimaryKey
    private long id = 0;

    private long targetType = 0;

    private long targetId = 0;

    private String publicKey;

    private String secretKey;

    private int keyVersion;

    private boolean isWeb;

    private long time;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTargetType() {
        return targetType;
    }

    public void setTargetType(long targetType) {
        this.targetType = targetType;
    }

    public long getTargetId() {
        return targetId;
    }

    public void setTargetId(long targetId) {
        this.targetId = targetId;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public int getKeyVersion() {
        return keyVersion;
    }

    public void setKeyVersion(int keyVersion) {
        this.keyVersion = keyVersion;
    }

    public void setWeb(boolean web) {
        isWeb = web;
    }

    public boolean isWeb() {
        return isWeb;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getTime() {
        return time;
    }
}
