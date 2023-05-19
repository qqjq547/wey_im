package framework.telegram.ui.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import framework.telegram.ui.utils.FileUtils
import java.io.*

class BitmapUtils {

    companion object {

        /**
         * 根据指定的图像路径和大小来获取缩略图
         *
         * @param path      图像的路径
         * @param newPath   压缩后图像存放的路径
         * @param maxWidth  指定输出图像的宽度
         * @param maxHeight 指定输出图像的高度
         * @return 生成的缩略图
         */
        @Throws(IOException::class)
        fun revitionImageSize(path: String, newPath: String, maxWidth: Int, maxHeight: Int): String? {
            try {
                var `in` = BufferedInputStream(FileInputStream(File(path)))
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeStream(`in`, null, options)
                `in`.close()
                var i = 0
                while (true) {
                    if (options.outWidth shr i <= maxWidth && options.outHeight shr i <= maxHeight) {
                        `in` = BufferedInputStream(FileInputStream(File(path)))
                        options.inSampleSize = Math.pow(2.0, i.toDouble()).toInt()
                        options.inJustDecodeBounds = false
                        val bitmap = BitmapFactory.decodeStream(`in`, null, options)
                        FileUtils.saveBitmap(bitmap, File(newPath))
                        break
                    }
                    i += 1
                }

                return newPath
            } catch (e: Exception) {
                return path
            }
        }

        fun getImageSize(path: String): IntArray? {
            try {
                var `in` = BufferedInputStream(FileInputStream(File(path)))
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeStream(`in`, null, options)
                `in`.close()
                return intArrayOf(options.outWidth, options.outHeight)
            } catch (e: Exception) {
                return intArrayOf(0, 0)
            }
        }

        fun getImageSize(byteArray: ByteArray): IntArray? {
            try {
                val  inputStream =  ByteArrayInputStream(byteArray)
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()
                return intArrayOf(options.outWidth, options.outHeight)
            } catch (e: Exception) {
                return intArrayOf(0, 0)
            }
        }

        fun drawable2Bitmap(drawable: Drawable): Bitmap {
            val bitmap = Bitmap
                    .createBitmap(
                            drawable.intrinsicWidth,
                            drawable.intrinsicHeight,
                            if (drawable.opacity != PixelFormat.OPAQUE)
                                Bitmap.Config.ARGB_8888
                            else
                                Bitmap.Config.RGB_565)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, drawable.intrinsicWidth,
                    drawable.intrinsicHeight)
            drawable.draw(canvas)
            return bitmap
        }
    }


}
