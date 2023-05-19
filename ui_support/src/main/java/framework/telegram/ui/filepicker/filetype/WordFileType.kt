package framework.telegram.ui.filepicker.filetype

import framework.telegram.ui.R

/**
 *
 * Word文件类型
 */
class WordFileType : FileType {

    override val fileType: String
        get() = "Word"
    override val fileIconResId: Int
        get() = R.drawable.ic_word_file_picker
    override val fileSmallResId: Int
        get() = R.drawable.ic_word_file_picker_small

            override fun verify(fileName: String): Boolean {
        /**
         * 使用 endWith 是不可靠的，因为文件名有可能是以格式结尾，但是没有 . 符号
         * 比如 文件名仅为：example_png
         */
        val isHasSuffix = fileName.contains(".")
        if (!isHasSuffix){
            // 如果没有 . 符号，即是没有文件后缀
            return false
        }
        val suffix = fileName.substring(fileName.lastIndexOf(".")  + 1)
        return when (suffix){
            "doc", "docx", "odt", "pages", "rtf", "tex", "wpd", "wps" -> {
                true
            }
            else -> {
                false
            }
        }
    }
}