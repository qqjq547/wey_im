package framework.ideas.common.db

import framework.ideas.common.model.common.SecretKeyModel
import framework.ideas.common.model.contacts.ContactDataModel
import framework.ideas.common.model.group.GroupInfoModel
import framework.ideas.common.model.im.MessageModel
import framework.ideas.common.model.im.MessageReceiptModel
import io.realm.DynamicRealm
import io.realm.FieldAttribute
import io.realm.RealmMigration
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey

class DBMigration : RealmMigration {

    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        val schema = realm.schema
        var lastVersion = oldVersion
        // Migrate from version 1 to version 2
        if (lastVersion == 1L) {
            val contactSchema = schema.get(ContactDataModel::class.java.simpleName)
            contactSchema?.addField("phone", String::class.java)
            contactSchema?.addField("isDeleteMe", Int::class.java)

            val messageSchema = schema.get(MessageModel::class.java.simpleName)
            messageSchema?.addField("refMsg", String::class.java)

            val groupInfoSchema = schema.get(GroupInfoModel::class.java.simpleName)
            groupInfoSchema?.addField("forShutupGroup", Boolean::class.java)
            groupInfoSchema?.addField("bfUpdateData", Boolean::class.java)
            groupInfoSchema?.addField("bfPushNotice", Boolean::class.java)
            groupInfoSchema?.addField("bfSetAdmin", Boolean::class.java)
            groupInfoSchema?.addField("memberRole", Int::class.java)
            groupInfoSchema?.addField("notice", String::class.java)
            groupInfoSchema?.addField("noticeId", Long::class.java)

            lastVersion++
        }

        if (lastVersion == 2L){
            val contactSchema = schema.get(ContactDataModel::class.java.simpleName)
            contactSchema?.addField("bfReadCancel", Boolean::class.java)
            contactSchema?.addField("msgCancelTime", Int::class.java)
            contactSchema?.addField("bfScreenshot", Boolean::class.java)

            val messageSchema = schema.get(MessageModel::class.java.simpleName)
            messageSchema?.addField("expireTime", Long::class.java)
            messageSchema?.addField("snapchatTime", Int::class.java)
            lastVersion++
        }
        if (lastVersion == 3L){
            val contactSchema = schema.get(ContactDataModel::class.java.simpleName)
            contactSchema?.addField("readReceipt", Boolean::class.java)
            contactSchema?.addField("commonGroupNum", Int::class.java)

            val messageSchema = schema.get(MessageModel::class.java.simpleName)
            messageSchema?.addField("showAlreadyRead", Boolean::class.java)
            lastVersion++
        }

        if (lastVersion == 4L){
            val groupSchema = schema.get(GroupInfoModel::class.java.simpleName)
            groupSchema?.addField("bfMember", Boolean::class.java)
            lastVersion++
        }

        if (lastVersion == 5L){
            val secretKeySchema = schema.get(SecretKeyModel::class.java.simpleName)
            secretKeySchema?.addField("isWeb", Boolean::class.java)
            lastVersion++
        }

        if (lastVersion == 6L){
            val groupSchema = schema.get(GroupInfoModel::class.java.simpleName)
            groupSchema?.addField("bfGroupReadCancel", Boolean::class.java)
            groupSchema?.addField("groupMsgCancelTime", Int::class.java)

            val messageSchema = schema.get(MessageModel::class.java.simpleName)
            messageSchema?.addField("chatType", Int::class.java)

            val contactSchema = schema.get(ContactDataModel::class.java.simpleName)
            contactSchema?.addField("bfCancel", Boolean::class.java)

            lastVersion++
        }

        if (lastVersion == 7L){
            val secretKeySchema = schema.get(SecretKeyModel::class.java.simpleName)
            secretKeySchema?.addField("time", Long::class.java)
            lastVersion++
        }

        if (lastVersion == 8L){
            val messageSchema = schema.get(MessageModel::class.java.simpleName)
            messageSchema?.addField("receiptCount", Int::class.java)
            lastVersion++
        }
        if (lastVersion == 9L){
            val contactSchema = schema.get(ContactDataModel::class.java.simpleName)
            contactSchema?.addField("bfBanned", Boolean::class.java)
            contactSchema?.addField("identify", String::class.java)

            val groupSchema = schema.get(GroupInfoModel::class.java.simpleName)
            groupSchema?.addField("bfGroupBanned", Boolean::class.java)

            lastVersion++
        }
        if (lastVersion == 10L){
            schema.create("MessageReceiptModel")
            .addField("id",Long::class.java, FieldAttribute.PRIMARY_KEY)
            .addField("msgId", Long::class.java, FieldAttribute.INDEXED)
                    .addField("senderUid",Long::class.java)
                    .addField("deliverTime",Long::class.java)
                    .addField("readTime",Long::class.java)
                    .addField("readedAttachmentTime",Long::class.java)
            lastVersion++
        }
        if (lastVersion == 11L){
            val messageModel = schema.get(MessageReceiptModel::class.java.simpleName)
            messageModel?.addField("messageType", Int::class.java)
            lastVersion++
        }
        if (lastVersion == 12L){
            val contactSchema = schema.get(ContactDataModel::class.java.simpleName)
            contactSchema?.addField("uploadContactTime", Long::class.java)
            lastVersion++
        }
        if (lastVersion == 13L){
            schema.create("RLogModel")
                    .addField("id",Long::class.java, FieldAttribute.PRIMARY_KEY)
                    .addField("time",Long::class.java)
                    .addField("level",Int::class.java)
                    .addField("tag",String::class.java)
                    .addField("log",String::class.java)
                    .addField("mark",String::class.java)
                    .addField("sort",Long::class.java)
            lastVersion++
        }

        if (lastVersion == 14L){
            val contactSchema = schema.get(ContactDataModel::class.java.simpleName)
                contactSchema?.addField("searchPhone", String::class.java)
            lastVersion++
        }

        if (lastVersion == 15L){
            val contactSchema = schema.get(ContactDataModel::class.java.simpleName)
            contactSchema?.addField("searchNoteName", String::class.java)
            contactSchema?.addField("searchNickName", String::class.java)
            contactSchema?.addField("shortNoteName", String::class.java)
            contactSchema?.addField("shortNickName", String::class.java)

            val groupSchema = schema.get(GroupInfoModel::class.java.simpleName)
            groupSchema?.addField("searchName", String::class.java)
            groupSchema?.addField("shortName", String::class.java)

            lastVersion++
        }
        if (lastVersion == 16L){
            schema.create("SearchDBModel")
                    .addField("chatId",Long::class.java, FieldAttribute.PRIMARY_KEY)
                    .addField("chatType",Int::class.java)
                    .addField("msgTime",Long::class.java)
            lastVersion++
        }

    }

    /**
     * java.lang.IllegalArgumentException: Configurations cannot be different if used to open the same file.
     * The most likely cause is that equals() and hashCode() are not overridden in the migration class:
     * framework.ideas.common.db.DBMigration
     */
    override fun hashCode(): Int {
        return DBMigration::class.java.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        return other is DBMigration
    }
}
