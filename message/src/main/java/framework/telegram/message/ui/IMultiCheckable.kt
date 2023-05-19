package framework.telegram.message.ui

interface IMultiCheckable {

    fun setCheckableMessage(showSetReaded: Boolean, countTitle: String, msgCount: Int)

    fun setAllChecked(allChecked: Boolean)

    fun dismissCheckMessages()
}