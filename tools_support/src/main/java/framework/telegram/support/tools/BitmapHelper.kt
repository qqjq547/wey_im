package framework.telegram.support.tools

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.*

/**
 * Created by hu on 2018/3/16.
 */
object BitmapHelper {
    fun getBitmapSize(file: File, size: IntArray) {
        if (size.size >= 2) {
            var options = BitmapFactory.Options()
            options.inJustDecodeBounds = true

            var os: InputStream? = null
            try {
                os = BufferedInputStream(FileInputStream(file))
                BitmapFactory.decodeStream(os, null, options)
                size[0] = options.outWidth
                size[1] = options.outHeight
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                SafeReleaseUtils.close(os)
            }
        }
    }

    fun saveBitmap(file: File, bitmap: Bitmap) {
        var os: OutputStream? = null
        try {
            os = BufferedOutputStream(FileOutputStream(file))
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (os != null) {
                os.close()
            }
        }
    }
}