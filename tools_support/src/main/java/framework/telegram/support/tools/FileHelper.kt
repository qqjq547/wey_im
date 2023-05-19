package framework.telegram.support.tools

import android.content.*
import android.content.Intent.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
import android.util.Log
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.tools.file.DirManager
import framework.telegram.support.tools.framework.telegram.support.UriUtils
import java.io.*
import java.nio.channels.FileChannel


/**
 * Created by lzh
 * time: 2018/1/4.
 * info:
 */
object FileHelper {

    fun getFileType(path: String): String = path.substring(path.lastIndexOf(".") + 1)

    fun getFileTypeByFile(file: File) {
        getFileType(file.absoluteFile.toString())
    }

    fun isExists(file: File?): Boolean {
        return file != null && file.exists()
    }

    fun isGifFile(path: String): Boolean {
        var inputStream: InputStream? = null
        return try {
            var buf = ByteArray(1)
            inputStream = FileInputStream(path)
            inputStream.read(buf)
            buf[0] == 71.toByte()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            SafeReleaseUtils.close(inputStream)
        }
    }

    fun copyFile(path: String, newPath: String) {
        var inputChannel: FileChannel? = null
        var outputChannel: FileChannel? = null
        try {
            inputChannel = FileInputStream(path).channel
            outputChannel = FileOutputStream(newPath).channel
            outputChannel?.transferFrom(inputChannel, 0, inputChannel?.size() ?: 0)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            inputChannel?.close()
            outputChannel?.close()
        }
    }

    fun file2Byte(tradeFile: File): ByteArray? {
        var buffer: ByteArray? = null
        try {
            val fis = FileInputStream(tradeFile)
            val bos = ByteArrayOutputStream()
            val b = ByteArray(1024)
            var n = 0

            do {
                n = fis.read(b)
                if (n == -1) {
                    break
                }
                bos.write(b, 0, n)

            } while (true)

            fis.close()
            bos.close()
            buffer = bos.toByteArray()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return buffer
    }

    fun byte2File(buf: ByteArray, file: File) {
        var bos: BufferedOutputStream? = null
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(file)
            bos = BufferedOutputStream(fos)
            bos.write(buf)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (bos != null) {
                try {
                    bos.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            if (fos != null) {
                try {
                    fos.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun insertImageToGallery(context: Context, file: File, mimeType: String = "image/jpeg", fileSuffix: String = "jpg"): Boolean {
        try {
            val dir = File(Environment.getExternalStorageDirectory(),Environment.DIRECTORY_PICTURES)
            val tempFile = File(dir, "${System.currentTimeMillis()}.$fileSuffix")
            copyFile(file.absolutePath, tempFile.absolutePath)
            val uri = Uri.fromFile(tempFile)
            context.sendBroadcast(Intent(ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
            return true
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        return false
    }

    fun insertVideoToGallery(context: Context, file: File) {
        val dir = File(Environment.getExternalStorageDirectory(),Environment.DIRECTORY_MOVIES)
        val tempFile = File(dir, "${System.currentTimeMillis()}.mp4")
        this.copyFile(file.absolutePath, tempFile.absolutePath)

        try {
            val localContentResolver = context.contentResolver
            val localContentValues = getVideoContentValues(tempFile, System.currentTimeMillis())
            var localUri = localContentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, localContentValues)
            if (localUri == null)
                localUri = Uri.fromFile(tempFile)
            context.sendBroadcast(Intent(ACTION_MEDIA_SCANNER_SCAN_FILE, localUri))
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
    }

    fun getVideoContentValues(paramFile: File, paramLong: Long): ContentValues {
        val localContentValues = ContentValues()
        localContentValues.put("title", paramFile.name)
        localContentValues.put("_display_name", paramFile.name)
        localContentValues.put("mime_type", "video/mp4")
        localContentValues.put("datetaken", paramLong)
        localContentValues.put("date_modified", paramLong)
        localContentValues.put("date_added", paramLong)
        localContentValues.put("_data", paramFile.absolutePath)
        localContentValues.put("_size", paramFile.length())
        return localContentValues
    }

    fun getImageContentValues(name: String, mimeType: String): ContentValues {
        val localContentValues = ContentValues()
//        ContentValues values = new ContentValues();
//        values.put(Images.Media.TITLE, title);
//        values.put(Images.Media.DESCRIPTION, description);
//        values.put(Images.Media.MIME_TYPE, "image/jpeg");
        localContentValues.put("title", name)
        localContentValues.put("_display_name", name)
        localContentValues.put("mime_type", mimeType)
        localContentValues.put("description", "")
//        localContentValues.put("datetaken", paramLong)
//        localContentValues.put("date_modified", paramLong)
//        localContentValues.put("date_added", paramLong)
//        localContentValues.put("_data", paramFile.absolutePath)
//        localContentValues.put("_size", paramFile.length())
        return localContentValues
    }

    fun writeFileData(file: File, conent: String) {
        var out: BufferedWriter? = null
        try {
            out = BufferedWriter(OutputStreamWriter(FileOutputStream(file, true)))
            out.write(conent);
        } catch (e: Exception) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (e: IOException) {
                e.printStackTrace();
            }
        }
    }
}