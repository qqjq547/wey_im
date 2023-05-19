package framework.telegram.message.ui.share

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.webkit.MimeTypeMap
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.support.BaseActivity
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.ThreadUtils
import framework.telegram.ui.filepicker.config.FilePickerConfig
import framework.telegram.ui.filepicker.utils.FileUtils
import java.io.File

@Route(path = Constant.ARouter.ROUNTE_MSG_SHARE_CHATS)
class ShareToChatsActivity : BaseActivity() {

    private val mShareFileList = arrayListOf<HashMap<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initShareFileList()
        if (mShareFileList.size > 0) {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_FORWARD_CHATS)
                    .withSerializable("share_list", mShareFileList)
                    .withBoolean("isFromShare",true)
                    .navigation()
        }
        ThreadUtils.runOnUIThread(500) {
            finish()
        }
    }

    /**
     * 分开单选和多选文件发送的uri
     */
    private fun initShareFileList() {
        if (intent == null) {
            return
        }

        mShareFileList.clear()
        when {
            intent.action == Intent.ACTION_SEND -> {
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                when {
                    uri != null -> {
                        val fileLength = getFileLength(uri)
                        when {
                            fileLength <= 0 -> toast(getString(R.string.the_file_is_empty))
                            fileLength > FilePickerConfig.MaxFileSize -> toast(getString(R.string.maximum_file_limit_50m))
                            else -> mShareFileList.add(getFileInfo(uri))
                        }
                    }
                    "text/plain" == intent.type -> {
                        val hashMap = HashMap<String, String>(2)
                        hashMap["mimetype"] = "text/plain"
                        hashMap["path"] = intent.clipData?.getItemAt(0)?.text.toString()
                        mShareFileList.add(hashMap)
                    }
                    else -> toast(getString(R.string.the_file_is_empty))
                }

            }
            intent.action == Intent.ACTION_SEND_MULTIPLE -> {
                val uriList = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) ?: arrayListOf()
                for (uri in uriList) {
                    if (uri == null) {
                        toast(getString(R.string.the_file_is_empty))
                        break
                    }
                    if (getFileLength(uri) < FilePickerConfig.MaxFileSize)
                        mShareFileList.add(getFileInfo(uri))
                    else {
                        toast(getString(R.string.maximum_file_limit_50m))
                        break
                    }
                }
            }
            else -> finish()
        }
    }

    /**
     * 获取分享文件的path和mimetype
     */
    private fun getFileInfo(uri: Uri): HashMap<String, String> {
        val hashMap = HashMap<String, String>(2)
        var path = ""
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri, proj, null, null, null)
        cursor?.let {
            try {
                val columnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                cursor.moveToFirst()
                path = cursor.getString(columnIndex)
                hashMap["path"] = path
                when (val suffix = FileUtils.getSuffix(path)) {
                    "jpg", "png", "gif" -> {
                        hashMap["mimetype"] = "image/$suffix"
                    }
                    "mp4" -> {
                        hashMap["mimetype"] = "video/mp4"
                    }
                    "txt", "TXT" -> {
                        hashMap["mimetype"] = ""
                    }
                    else -> {
                        hashMap["mimetype"] = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(path))!!
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                hashMap["mimetype"] = ""
            } finally {
                cursor.close()
            }
        }
        return hashMap
    }

    /**
     * 获取分享文件的大小
     */
    private fun getFileLength(uri: Uri): Long {
        var length = 0L
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri, proj, null, null, null)
        cursor?.let {
            if (cursor.count > 0) {
                val columnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                if (columnIndex > -1) {
                    cursor.moveToFirst()
                    val path = cursor.getString(columnIndex)
                    if (!TextUtils.isEmpty(path) && File(path).exists()) {
                        length = File(path).length()
                    }
                }

                cursor.close()
            }
        }

        return length
    }
}
