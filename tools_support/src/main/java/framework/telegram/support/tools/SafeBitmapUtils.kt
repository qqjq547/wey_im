package framework.telegram.support.tools

import android.content.res.Resources
import android.graphics.*
import android.media.ExifInterface
import android.media.ThumbnailUtils
import android.provider.MediaStore
import android.view.View
import framework.telegram.support.tools.exception.ClientException
import java.io.*

class SafeBitmapUtils {

    companion object {

        fun decodeByteArray(bytes: ByteArray, offset: Int, length: Int, options: BitmapFactory.Options? = null): Bitmap? {
            return try {
                if (options == null) {
                    BitmapFactory.decodeByteArray(bytes, offset, length)
                } else {
                    BitmapFactory.decodeByteArray(bytes, offset, length, options)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                null
            }
        }

        fun decodeStream(inputStream: InputStream, options: BitmapFactory.Options? = null): Bitmap? {
            var newInputStream: InputStream = inputStream
            if (inputStream !is BufferedInputStream) {
                newInputStream = BufferedInputStream(inputStream)
            }

            return try {
                if (options == null) {
                    BitmapFactory.decodeStream(newInputStream)
                } else {
                    BitmapFactory.decodeStream(newInputStream, null, options)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                null
            }
        }

        fun decodeResource(resources: Resources, resId: Int, options: BitmapFactory.Options? = null): Bitmap? {
            return try {
                if (options == null) {
                    BitmapFactory.decodeResource(resources, resId)
                } else {
                    BitmapFactory.decodeResource(resources, resId, options)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                null
            }
        }

        /**
         * bitmap转为base64
         *
         * @param bitmap
         * @return
         */
        fun bitmapToBase64(bitmap: Bitmap?): String? {
            var result: String? = null
            var baos: ByteArrayOutputStream? = null
            try {
                if (bitmap != null) {
                    baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                    val bitmapBytes = baos.toByteArray()
                    result = Base64.encodeToString(bitmapBytes, Base64.DEFAULT)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                SafeReleaseUtils.close(baos)
            }
            return result
        }

        /**
         * base64转为bitmap
         *
         * @param base64Data
         * @return
         */
        fun base64ToBitmap(base64Data: String): Bitmap? {
            val bytes = Base64.decode(base64Data, Base64.DEFAULT)
            return decodeByteArray(bytes, 0, bytes.size)
        }

        /**
         * 获取视频缩略图
         *
         * @param videoPath
         * @param width
         * @param height
         * @return
         */
        fun getVideoThumbnail(videoPath: String, width: Int, height: Int): Bitmap? {
            // 获取视频的缩略图
            val bitmap = ThumbnailUtils.createVideoThumbnail(videoPath, MediaStore.Images.Thumbnails.MINI_KIND)
            if (bitmap != null && !bitmap.isRecycled) {
                val sampleBitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height, ThumbnailUtils.OPTIONS_RECYCLE_INPUT)
                if (bitmap != sampleBitmap && !bitmap.isRecycled) {
                    bitmap.recycle()
                    System.gc()
                }
                return sampleBitmap
            } else {
                return null
            }
        }

        fun getViewBitmap(v: View): Bitmap? {
            try {
                v.clearFocus()
                v.isPressed = false
            } catch (e: Throwable) {
                e.printStackTrace()
            }

            val willNotCache = v.willNotCacheDrawing()
            v.setWillNotCacheDrawing(false)

            // Reset the drawing cache background color to fully transparent
            // for the duration of this operation
            val color = v.drawingCacheBackgroundColor
            v.drawingCacheBackgroundColor = 0

            if (color != 0) {
                v.destroyDrawingCache()
            }

            v.buildDrawingCache()

            val cacheBitmap = v.drawingCache
            if (cacheBitmap == null || cacheBitmap.isRecycled) {
                return null
            }

            val bitmap = cloneBitmap(cacheBitmap)

            // Restore the view
            v.destroyDrawingCache()
            v.setWillNotCacheDrawing(willNotCache)
            v.drawingCacheBackgroundColor = color

            return bitmap
        }

        fun makeViewBitmap(v: View): Bitmap? {
            v.layout(v.left, v.top, v.measuredWidth, v.measuredHeight)
            val bitmap = Bitmap.createBitmap(v.measuredWidth, v.measuredHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val bgDrawable = v.background
            if (bgDrawable != null)
                bgDrawable.draw(canvas)
            else
                canvas.drawColor(Color.TRANSPARENT)
            v.draw(canvas)
            canvas.save()

            return bitmap
        }

        fun makeViewBitmap(v: View, width: Int, height: Int): Bitmap? {
            return makeViewBitmap(v, width, height, Color.TRANSPARENT)
        }

        fun makeViewBitmap(v: View, width: Int, height: Int, bgColor: Int): Bitmap? {
            v.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
            v.layout(v.left, v.top, v.right, v.bottom)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(bgColor)
            v.draw(canvas)
            canvas.save()
            return bitmap
        }

        /**
         * 保存Bitmap到文件
         *
         * @param savePath
         * @param image
         * @throws ClientException
         */
        @Throws(ClientException::class)
        fun saveBitmap(image: Bitmap, savePath: File, compressFormat: Bitmap.CompressFormat? = Bitmap.CompressFormat.JPEG): Boolean {
            var bos: BufferedOutputStream? = null
            return try {
                if (savePath.exists()) {
                    savePath.delete()
                }
                bos = BufferedOutputStream(FileOutputStream(savePath))
                image.compress(compressFormat ?: Bitmap.CompressFormat.JPEG, 100, bos)
                true
            } catch (e: Throwable) {
                false
            } finally {
                SafeReleaseUtils.close(bos)
            }
        }

        /**
         * 复制Bitmap
         *
         * @param source
         * @return
         */
        fun cloneBitmap(source: Bitmap?): Bitmap? {
            if (source == null) {
                return null
            }

            // 由于使用android.graphics.Bitmap.createBitmap(Bitmap src)方法创建新的Bitmap对象时，
            // 会先判断当前作为参数传进来的图片大小是否和想要的尺寸一致，且Bitmap的Mutable属性为false，
            // 如果满足则不创建直接返回此对象。并且在调用所创建Bitmap对象的recycle()进行回收时，两个Bitmap对象同时被回收。
            // 例如用bitmapA来创建新的bitmapB，当bitmapA调用recycle()时，bitmapA和bitmapB都被回收掉了，导致程序绘制出现异常。
            val bitmap = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawBitmap(source, 0f, 0f, Paint())
            return bitmap
        }

        /**
         * 缩放Bitmap
         *
         * @param bitmap    所要转换的bitmap
         * @param newWidth  新的宽
         * @param newHeight 新的高
         * @return 指定宽高的bitmap
         */
        fun zoomBitmap(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap? {
            // 获得图片的宽高
            val width = bitmap.width
            val height = bitmap.height
            // 计算缩放比例
            val scaleWidth = newWidth.toFloat() / width
            val scaleHeight = newHeight.toFloat() / height
            // 取得想要缩放的matrix参数
            val matrix = Matrix()
            matrix.postScale(scaleWidth, scaleHeight)
            // 得到新的图片
            return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
        }

        fun zoomBitmapFromFile(file: File, reqWidth: Int, reqHeight: Int): Bitmap? {
            val bitmap = decodeSampledBitmapFromFile(file, reqWidth, reqHeight)
            val newBitmap = zoomBitmap(bitmap!!, reqWidth, reqHeight)
            if (newBitmap != bitmap) {
                SafeReleaseUtils.recycle(bitmap)
            }
            return newBitmap
        }

        /**
         * 计算缩放比例
         *
         * @param options
         * @param reqWidth
         * @param reqHeight
         * @return
         */
        private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int? {
            val height = options.outHeight
            val width = options.outWidth
            return calculateInSampleSize(width, height, reqWidth, reqHeight)
        }

        /**
         * 计算缩放比例
         *
         * @param width
         * @param height
         * @param reqWidth
         * @param reqHeight
         * @return
         */
        private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int? {
            var inSampleSize = 1
            if (height > reqHeight || width > reqWidth) {
                inSampleSize = if (width < height) {
                    Math.round(height.toFloat() / reqHeight.toFloat())
                } else {
                    Math.round(width.toFloat() / reqWidth.toFloat())
                }
            }
            return inSampleSize
        }

        /**
         * 获取缩略图
         *
         * @param file
         * @param reqWidth
         * @param reqHeight
         * @return
         */
        fun decodeSampledBitmapFromFile(file: File, reqWidth: Int, reqHeight: Int): Bitmap? {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(file.absolutePath, options)
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight) ?: 1
            options.inJustDecodeBounds = false
            var inputStream: InputStream? = null
            return try {
                inputStream = FileInputStream(File(file.absolutePath))
                BitmapFactory.decodeStream(inputStream, Rect(), options)
            } catch (e: IOException) {
                e.printStackTrace()
                null
            } finally {
                SafeReleaseUtils.close(inputStream)
            }
        }

        /**
         * 获取图片宽高
         *
         * @param file
         * @return
         */
        fun getBitmapSize(file: File): IntArray? {
            return try {
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeFile(file.absolutePath, options)
                intArrayOf(options.outWidth, options.outHeight)
            } catch (e: Exception) {
                intArrayOf(0, 0)
            }
        }

        /**
         * 获取图片旋转值
         *
         * @param imageFilePath
         * @return
         */
        fun getExifRotation(imageFilePath: String?): Int? {
            if (imageFilePath == null)
                return 0

            return try {
                val exif = ExifInterface(imageFilePath)
                // We only recognize a subset of orientation tag values
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_UNDEFINED)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> ExifInterface.ORIENTATION_UNDEFINED
                }
            } catch (e: IOException) {
                0
            }
        }

        /**
         * 旋转Bitmap
         *
         * @param angle
         * @param bitmap
         * @return
         */
        fun rotaingBitmap(bitmap: Bitmap, angle: Int): Bitmap? {
            val matrix = Matrix()
            matrix.postRotate(angle.toFloat())
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }

        /**
         * 灰度处理Bitmap
         *
         * @param bitmap
         * @return
         */
        fun grey(bitmap: Bitmap): Bitmap? {
            val width = bitmap.width
            val height = bitmap.height
            val greyBitmap = Bitmap.createBitmap(width, height,
                    Bitmap.Config.ARGB_8888)
            val canvas = Canvas(greyBitmap)
            val paint = Paint()
            val colorMatrix = ColorMatrix()
            colorMatrix.setSaturation(0f)
            val colorMatrixFilter = ColorMatrixColorFilter(colorMatrix)
            paint.colorFilter = colorMatrixFilter
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            return greyBitmap
        }

        /**
         * 圆形处理Bitmap
         *
         * @param bitmap
         * @return
         */
        fun circular(bitmap: Bitmap): Bitmap? {
            val output = if (bitmap.width > bitmap.height) {
                Bitmap.createBitmap(bitmap.height, bitmap.height, Bitmap.Config.ARGB_8888)
            } else {
                Bitmap.createBitmap(bitmap.width, bitmap.width, Bitmap.Config.ARGB_8888)
            }

            val canvas = Canvas(output)
            val color = -0xbdbdbe
            val paint = Paint()
            val rect = Rect(0, 0, bitmap.width, bitmap.height)
            var r = if (bitmap.width > bitmap.height) {
                (bitmap.height / 2).toFloat()
            } else {
                (bitmap.width / 2).toFloat()
            }
            paint.isAntiAlias = true
            canvas.drawARGB(0, 0, 0, 0)
            paint.color = color
            canvas.drawCircle(r, r, r, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(bitmap, rect, rect, paint)
            return output
        }

        /**
         * 圆角处理Bitmap
         *
         * @param bitmap
         * @param radius
         * @return
         */
        fun round(bitmap: Bitmap, radius: Int): Bitmap? {
            val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val color = -0xbdbdbe
            val paint = Paint()
            val rect = Rect(0, 0, bitmap.width, bitmap.height)
            val rectF = RectF(rect)
            val roundPx = radius.toFloat()
            paint.isAntiAlias = true
            canvas.drawARGB(0, 0, 0, 0)
            paint.color = color
            canvas.drawRoundRect(rectF, roundPx, roundPx, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(bitmap, rect, rect, paint)
            return output
        }

        /**
         * 透明度处理Bitmap
         *
         * @param bitmap
         * @param alpha
         * @return
         */
        fun alpha(bitmap: Bitmap, alpha: Int): Bitmap? {
            val matrixItems = floatArrayOf(1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, alpha / 255f, 0f, 0f, 0f, 0f, 0f, 1f)
            val width = bitmap.width
            val height = bitmap.height
            val alphaBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(alphaBitmap)
            val paint = Paint()
            val colorMatrix = ColorMatrix(matrixItems)
            val colorMatrixFilter = ColorMatrixColorFilter(colorMatrix)
            paint.colorFilter = colorMatrixFilter
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            return alphaBitmap
        }
    }
}