package framework.telegram.ui.tools

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.gifdecoder.GifHeaderParser
import com.bumptech.glide.gifdecoder.StandardGifDecoder
import com.bumptech.glide.load.resource.gif.GifBitmapProvider
import framework.telegram.ui.utils.BitmapUtils.Companion.getImageSize
import framework.telegram.ui.videoplayer.utils.AnimatedGifEncoder
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.max

/**
 * Created by lzh on 19-10-9.
 * INFO:
 */
object GifUtils {

    val LIMIT_SIZE = 400

    fun compressImageDataWithLongWidth(context: Context, rawData: ByteArray, limitLongWidth: Int): ByteArray? {
        val format = rawData.imageFormat()
        if (format == GifUtils.ImageFormat.UNKNOWN) {
            return null
        }
        val arr = getImageSize(rawData)
        val imageWidth = arr?.first()
        val imageHeight = arr?.last()
        val longSideWidth = max(imageWidth ?: 0, imageHeight ?: 0)

        if (longSideWidth <= limitLongWidth) {
            return rawData
        }

        if (format == GifUtils.ImageFormat.GIF) {
            // 压缩 Gif 分辨率太大编码时容易崩溃
            return compressGifDataWithLongWidth(context, rawData, limitLongWidth)
        }
        return null
    }


    /**
     * 返回同步压缩 gif 图片 Byte 数据 [rawData] 每一帧长边到 [limitLongWidth] 后的 Byte 数据
     */
    private fun compressGifDataWithLongWidth(context: Context, rawData: ByteArray, limitLongWidth: Int): ByteArray? {
        val gifDecoder = StandardGifDecoder(GifBitmapProvider(Glide.get(context).bitmapPool))
        val headerParser = GifHeaderParser()
        headerParser.setData(rawData)
        val header = headerParser.parseHeader()
        gifDecoder.setData(header, rawData)
        val frameCount = gifDecoder.frameCount

        // 计算调整后大小
        val longSideWidth = max(header.width, header.height)
        val ratio = limitLongWidth.toFloat() / longSideWidth.toFloat()
        val resizeWidth = (header.width.toFloat() * ratio).toInt()
        val resizeHeight = (header.height.toFloat() * ratio).toInt()


        val outputStream = ByteArrayOutputStream()
        val gifEncoder = AnimatedGifEncoder()
        var resultData: ByteArray? = null
        gifEncoder.start(outputStream)
        gifEncoder.setRepeat(0)


        // 每一帧进行缩放
        val time1 = System.currentTimeMillis()
        for (index in 0 until frameCount) {

            gifDecoder.advance()
            var imageFrame = gifDecoder.nextFrame
            if (imageFrame != null) {
                imageFrame = Bitmap.createScaledBitmap(imageFrame, resizeWidth, resizeHeight, true)
            }
            gifEncoder.setDelay(gifDecoder.getDelay(index))  // 计算帧的间隔
            gifEncoder.addFrame(imageFrame)
            imageFrame?.recycle()
        }

        try {
            gifEncoder.finish()
            resultData = outputStream.toByteArray()
            outputStream.close()
            return resultData
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return resultData
    }

    enum class ImageFormat {
        JPG, PNG, GIF, UNKNOWN
    }

    fun ByteArray.imageFormat(): ImageFormat {
        val headerData = this.slice(0..2)
        val hexString = headerData.fold(StringBuilder("")) { result, byte -> result.append((byte.toInt() and 0xFF).toString(16)) }.toString().toUpperCase()
        var imageFormat = GifUtils.ImageFormat.UNKNOWN
        when (hexString) {
            "FFD8FF" -> {
                imageFormat = GifUtils.ImageFormat.JPG
            }
            "89504E" -> {
                imageFormat = GifUtils.ImageFormat.PNG
            }
            "474946" -> {
                imageFormat = GifUtils.ImageFormat.GIF
            }
        }
        return imageFormat
    }


}