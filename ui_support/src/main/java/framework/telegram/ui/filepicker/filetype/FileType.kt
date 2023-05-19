package framework.telegram.ui.filepicker.filetype

/**
 *
 * @author rosu
 * @date 2018/11/27
 */
interface FileType {
    /**
     * 文件类型
     */
    val fileType:String

    val fileIconResId:Int

    val fileSmallResId:Int
    /**
     * 传入文件路径，判断是否为该类型
     * @param fileName String
     * @return Boolean
     */
    fun verify(fileName:String):Boolean
}