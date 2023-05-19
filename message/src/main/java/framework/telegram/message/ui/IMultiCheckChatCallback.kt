package framework.telegram.message.ui

interface IMultiCheckChatCallback {

    fun showCheckMessages()

    fun clickAllChecked(isChecked: Boolean): Int

    fun clickBatchDelete()

    fun hasUnread(): Boolean

    fun clickBatchSetReaded()

    fun dismissCheckMessages()
}