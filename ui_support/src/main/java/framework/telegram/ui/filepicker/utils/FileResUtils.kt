package framework.telegram.ui.filepicker.utils

import android.text.TextUtils
import framework.telegram.ui.R
import framework.telegram.ui.filepicker.filetype.*

/**
 * 根据后缀名获取对应Res id
 */
object FileResUtils {

    private val allDefaultFileType: ArrayList<FileType> by lazy {
        val fileTypes = ArrayList<FileType>()
        fileTypes.add(AudioFileType())
        fileTypes.add(RasterImageFileType())
        fileTypes.add(CompressedFileType())
        fileTypes.add(DataBaseFileType())
        fileTypes.add(ExecutableFileType())
        fileTypes.add(FontFileType())
        fileTypes.add(PageLayoutFileType())
        fileTypes.add(TextFileType())
        fileTypes.add(WordFileType())
        fileTypes.add(ExcelFileType())
        fileTypes.add(VideoFileType())
        fileTypes.add(WebFileType())
        fileTypes.add(PowerPointFileType())
        fileTypes
    }

    /**
     * @param suffix 后缀名
     * @param isGetSmallIcon 是否取小图标
     */
    fun get(fileName: String?, isGetSmallIcon: Boolean = true): Int {
        if (!TextUtils.isEmpty(fileName)) {
            for (type in allDefaultFileType) {
                if (type.verify(fileName!!.toLowerCase())) {
                    return if (isGetSmallIcon)
                        type.fileSmallResId
                    else
                        type.fileIconResId
                }
            }
        }

        return if (isGetSmallIcon)
            R.drawable.ic_unknown_file_picker_small
        else
            R.drawable.ic_unknown_file_picker
    }
}