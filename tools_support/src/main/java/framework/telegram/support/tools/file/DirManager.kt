package framework.telegram.support.tools.file

import android.content.Context
import android.os.Environment
import android.text.TextUtils
import framework.telegram.support.tools.EnvironmentUtils
import framework.telegram.support.tools.FileUtils
import java.io.File

/**
 *
 * getVideoFileDir，用户保存下载的视频文件（区分账号，区分用户，可清除）
 * getVoiceFileDir，用户保存下载的音频文件（区分账号，区分用户，可清除）
 * getDownloadFileDir,用户保存下载的文件（区分账号，区分用户，可清除）

 * getImageFileDir  用于保存下载自己的二维码、群二维码（区分账号，不区分用户，可清除）
 * getCameraFileDir，用于保存照相机拍摄的照片和视频（区分账号，不区分用户，可清除）

 * getVoiceCacheDir,用于保存语音录制和转码的临时文件（区分账号，不区分用户，可清除）
 * getApkCacheDir,用于保存下载的安装包的临时文件（不区分账号，不区分用户，可清除）
 *
 */
object DirManager {

    private const val ROOT_DIR_NAME = "68gram"

    fun getClearableFileSize(context: Context, uuid: String): Long {
        val imageFileDirSize = FileUtils.sizeOfDirectory(getImageFileDir(context, uuid))
        val cameraFileDirSize = FileUtils.sizeOfDirectory(getCameraFileDir(context, uuid))
        val videoFileDirSize = FileUtils.sizeOfDirectory(getVideoFileDir(context, uuid))
        val voiceFileDirSize = FileUtils.sizeOfDirectory(getVoiceFileDir(context, uuid))
        val downloadFileDirSize = FileUtils.sizeOfDirectory(getDownloadFileDir(context, uuid))
        val imageCacheDirSize = FileUtils.sizeOfDirectory(getImageCacheDir(context, uuid))
        val voiceCacheDirSize = FileUtils.sizeOfDirectory(getVoiceCacheDir(context, uuid))
        val imageFileOldVerDirSize = FileUtils.sizeOfDirectory(getImageFileDirOldVersion(context, uuid))
        val cameraFileOldVerDirSize = FileUtils.sizeOfDirectory(getCameraFileDirOldVersion(context, uuid))
        val videoFileOldVerDirSize = FileUtils.sizeOfDirectory(getVideoFileDirOldVersion(context, uuid))
        val voiceFileOldVerDirSize = FileUtils.sizeOfDirectory(getVoiceFileDirOldVersion(context, uuid))
        val downloadFileOldVerDirSize = FileUtils.sizeOfDirectory(getDownloadFileDirOldVersion(context, uuid))
        return imageFileDirSize + cameraFileDirSize + videoFileDirSize + voiceFileDirSize + downloadFileDirSize + imageCacheDirSize + voiceCacheDirSize + imageFileOldVerDirSize + cameraFileOldVerDirSize + videoFileOldVerDirSize + voiceFileOldVerDirSize + downloadFileOldVerDirSize
    }

    fun clearStorage(context: Context, uuid: String) {
        FileUtils.deleteDirectory(getImageFileDir(context, uuid))
        FileUtils.deleteDirectory(getCameraFileDir(context, uuid))
        FileUtils.deleteDirectory(getVideoFileDir(context, uuid))
        FileUtils.deleteDirectory(getVoiceFileDir(context, uuid))
        FileUtils.deleteDirectory(getDownloadFileDir(context, uuid))
        FileUtils.deleteDirectory(getImageCacheDir(context, uuid))
        FileUtils.deleteDirectory(getVoiceCacheDir(context, uuid))
        FileUtils.deleteDirectory(getImageFileDirOldVersion(context, uuid))
        FileUtils.deleteDirectory(getCameraFileDirOldVersion(context, uuid))
        FileUtils.deleteDirectory(getVideoFileDirOldVersion(context, uuid))
        FileUtils.deleteDirectory(getVoiceFileDirOldVersion(context, uuid))
        FileUtils.deleteDirectory(getDownloadFileDirOldVersion(context, uuid))
    }

    fun getImageFileDir(context: Context, uuid: String): File {
        val dir = File(getTmpDir(context, ROOT_DIR_NAME, false, false, Environment.DIRECTORY_DOCUMENTS, uuid), "image_file")
        if (!dir.exists()) {
            dir.mkdirs()
        }

        FileUtils.makeNoMediaFile(dir)
        return dir
    }

    fun getCameraFileDir(context: Context, uuid: String): File {
        val dir = File(getTmpDir(context, ROOT_DIR_NAME, false, false, Environment.DIRECTORY_DOCUMENTS, uuid), "camera_file")
        if (!dir.exists()) {
            dir.mkdirs()
        }

        FileUtils.makeNoMediaFile(dir)
        return dir
    }

    fun getVideoFileDir(context: Context, uuid: String): File {
        val dir = File(getTmpDir(context, ROOT_DIR_NAME, false, false, Environment.DIRECTORY_DOCUMENTS, uuid), "video_file")
        if (!dir.exists()) {
            dir.mkdirs()
        }

        FileUtils.makeNoMediaFile(dir)
        return dir
    }

    /**
     * 获取用户的微视频及语音文件下载文件保存目录
     *
     * @return
     */
    fun getVoiceFileDir(context: Context, uuid: String): File {
        val dir = File(getTmpDir(context, ROOT_DIR_NAME, false, false, Environment.DIRECTORY_DOCUMENTS, uuid), "voice_file")
        if (!dir.exists()) {
            dir.mkdirs()
        }

        FileUtils.makeNoMediaFile(dir)
        return dir
    }

    fun getDownloadFileDir(context: Context, uuid: String): File {
        val dir = File(getTmpDir(context, ROOT_DIR_NAME, false, false, Environment.DIRECTORY_DOCUMENTS, uuid), "download_file")
        if (!dir.exists()) {
            dir.mkdirs()
        }

        FileUtils.makeNoMediaFile(dir)
        return dir
    }


    /**
     * 旧版本使用的缓存目录，仅用于清理缓存使用！！！！！！！！
     * --------------------------------start----------------------------------
     */
    private fun getImageFileDirOldVersion(context: Context, uuid: String): File {
        val dir = File(getTmpDir(context, ROOT_DIR_NAME, false, false, Environment.DIRECTORY_DOWNLOADS, uuid), "image_file")
        if (!dir.exists()) {
            dir.mkdirs()
        }

        FileUtils.makeNoMediaFile(dir)
        return dir
    }

    private fun getCameraFileDirOldVersion(context: Context, uuid: String): File {
        val dir = File(getTmpDir(context, ROOT_DIR_NAME, false, false, Environment.DIRECTORY_DOWNLOADS, uuid), "camera_file")
        if (!dir.exists()) {
            dir.mkdirs()
        }

        FileUtils.makeNoMediaFile(dir)
        return dir
    }

    private fun getVideoFileDirOldVersion(context: Context, uuid: String): File {
        val dir = File(getTmpDir(context, ROOT_DIR_NAME, false, false, Environment.DIRECTORY_DOWNLOADS, uuid), "video_file")
        if (!dir.exists()) {
            dir.mkdirs()
        }

        FileUtils.makeNoMediaFile(dir)
        return dir
    }

    private fun getVoiceFileDirOldVersion(context: Context, uuid: String): File {
        val dir = File(getTmpDir(context, ROOT_DIR_NAME, false, false, Environment.DIRECTORY_DOWNLOADS, uuid), "voice_file")
        if (!dir.exists()) {
            dir.mkdirs()
        }

        FileUtils.makeNoMediaFile(dir)
        return dir
    }

    private fun getDownloadFileDirOldVersion(context: Context, uuid: String): File {
        val dir = File(getTmpDir(context, ROOT_DIR_NAME, false, false, Environment.DIRECTORY_DOWNLOADS, uuid), "download_file")
        if (!dir.exists()) {
            dir.mkdirs()
        }

        FileUtils.makeNoMediaFile(dir)
        return dir
    }
    /**
     * 旧版本使用的缓存目录，仅用于清理缓存使用！！！！！！！！
     * --------------------------------end----------------------------------
     */

    fun getFileCacheDir(context: Context, uuid: String): File {
        val dir = File(getTmpDir(context, ROOT_DIR_NAME, false, true, null, uuid), "file_cache")
        if (!dir.exists()) {
            dir.mkdirs()
        }

        FileUtils.makeNoMediaFile(dir)
        return dir
    }

    /**
     * 图片存储临时目录(Bitmap直接保存为File的、裁剪图的、聊天背景、用户资料背景、首页背景)
     *
     * @return
     */
    fun getImageCacheDir(context: Context, uuid: String): File {
        val dir = File(getTmpDir(context, ROOT_DIR_NAME, false, true, null, uuid), "image_cache")
        if (!dir.exists()) {
            dir.mkdirs()
        }

        FileUtils.makeNoMediaFile(dir)
        return dir
    }

    fun getApkCacheDir(context: Context): File {
        val dir = File(getTmpDir(context, ROOT_DIR_NAME, false, true, null, null), "apk_file")
        if (!dir.exists()) {
            dir.mkdirs()
        }

        FileUtils.makeNoMediaFile(dir)
        return dir
    }

    /**
     * 获取用户的语音文件临时目录
     *
     * @return
     */
    fun getVoiceCacheDir(context: Context, uuid: String): File {
        val dir = File(getTmpDir(context, ROOT_DIR_NAME, false, true, null, uuid), "voice_cache")
        if (!dir.exists()) {
            dir.mkdirs()
        }

        FileUtils.makeNoMediaFile(dir)
        return dir
    }

    /**
     * 获取临时目录
     *
     * @param onlySdcard 是否只取sd卡上的目录
     * @param isCache    是否是缓存文件，是的话会被系统自动清理
     * @param type
     * [android.os.Environment.DIRECTORY_MUSIC],
     * [android.os.Environment.DIRECTORY_PODCASTS],
     * [android.os.Environment.DIRECTORY_RINGTONES],
     * [android.os.Environment.DIRECTORY_ALARMS],
     * [android.os.Environment.DIRECTORY_NOTIFICATIONS],
     * [android.os.Environment.DIRECTORY_PICTURES],
     * [android.os.Environment.DIRECTORY_DOCUMENTS],
     * [android.os.Environment.DIRECTORY_DOWNLOADS],
     * [android.os.Environment.DIRECTORY_MOVIES].
     * @return
     */
    private fun getTmpDir(context: Context, rootDirName: String, onlySdcard: Boolean, isCache: Boolean, type: String?, uuid: String?): File? {
        // 判断sd卡是否存在
        val sdCardExist = EnvironmentUtils.isExternalStorageExist()
        val sdCardEnable = EnvironmentUtils.isExternalStorageMountedReadWrite()
        if (onlySdcard && (!sdCardExist || !sdCardEnable)) {
            return null//仅使用sd卡,但此时sd卡不可用
        }

        var rootDir = if (isCache) EnvironmentUtils.getExternalCacheDir(context) else EnvironmentUtils.getExternalFilesDir(context, type)
        if (rootDir != null) {
            //sd卡可用
            if (!rootDir.exists() && !rootDir.mkdirs()) {
                //sd卡目录不可用
                val newRootDir = File(rootDir.getPath().replace("/sdcard/", "/sdcard-ext/"))
                if (newRootDir.mkdirs()) {
                    //sd卡兼容目录可用
                    rootDir = newRootDir
                }
            }
        }

        if (rootDir == null || !rootDir.exists() || !rootDir.canRead() || !rootDir.canWrite()) {
            if (onlySdcard) {
                return null
            } else {
                rootDir = if (isCache) context.cacheDir else context.filesDir
            }
        }

        var tmpDir = File(rootDir, rootDirName)
        if (!tmpDir.exists()) {
            tmpDir.mkdirs()
        }

        // 分用户
        if (!TextUtils.isEmpty(uuid)) {
            tmpDir = File(tmpDir, uuid!!)
        }

        return tmpDir
    }
}