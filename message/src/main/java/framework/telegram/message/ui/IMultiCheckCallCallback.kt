package framework.telegram.message.ui

interface IMultiCheckCallCallback {

    fun showCheckMessages()

    fun clickAllChecked(isChecked: Boolean): Int

    fun clickBatchDelete()

    fun dismissCheckMessages()
}