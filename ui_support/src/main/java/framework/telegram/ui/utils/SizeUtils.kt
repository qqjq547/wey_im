package framework.telegram.ui.utils

import framework.telegram.ui.tools.GifUtils.LIMIT_SIZE
import kotlin.math.max

class SizeUtils {

    companion object {

        fun calculateDynamicNewSize(imageRealWidth: Int, imageRealHeight: Int): IntArray {
            // 计算调整后大小
            // 参考 GifUtils compressImageDataWithLongWidth
            val longSideWidth = max(imageRealWidth, imageRealHeight)
            if (LIMIT_SIZE > longSideWidth) {
                return intArrayOf(imageRealWidth, imageRealHeight)
            }

            val ratio = LIMIT_SIZE / longSideWidth.toFloat()
            val resizeWidth = (imageRealWidth.toFloat() * ratio).toInt()
            val resizeHeight = (imageRealHeight.toFloat() * ratio).toInt()

            return intArrayOf(resizeWidth, resizeHeight)
        }


        public fun calculateNewSize(w: Float, h: Float, maxSize: Float, minSize: Float): IntArray {
            var width = w
            var height = h
            val ratio = width / height

            if (ratio < (minSize / maxSize) || ratio > (maxSize / minSize)) {
                //异形图
                if (ratio < (minSize / maxSize)) {
                    height = width * maxSize / minSize //矫正高度
                } else {
                    width = height * maxSize / minSize //矫正宽度
                }
            }

            return if (width > maxSize || height > maxSize) {
                //宽或高大于临界值
                if (width >= height) {
                    //宽大于临界值
                    val newHeight = maxSize * height / width
                    intArrayOf(maxSize.toInt(), newHeight.toInt())
                } else {
                    //高大于临界值
                    val newWidth = maxSize * width / height
                    intArrayOf(newWidth.toInt(), maxSize.toInt())
                }
            } else if (width < minSize || height < minSize) {
                //宽或高小于临界值
                if (width < height) {
                    //宽小于临界值
                    val newHeight = minSize * height / width
                    intArrayOf(minSize.toInt(), newHeight.toInt())
                } else {
                    //高小于临界值
                    val newWidth = minSize * width / height
                    intArrayOf(newWidth.toInt(), minSize.toInt())
                }
            } else {
                //宽和高都在临界值内
                intArrayOf(width.toInt(), height.toInt())
            }
        }
    }
}