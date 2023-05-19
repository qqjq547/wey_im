package framework.ideas.common.audio

object AudioSampleUtils {

    fun getSamplesViewWidth(sampleSize: Int): Int {
        return (sampleSize * 2) + (sampleSize - 1)
    }

    fun getDefaultSamples(time: Int): IntArray {
        val samples = arrayListOf<Int>()
        val count = if (time > 3) {
            30 + (time - 3) * 2//每多一秒，在30个基础上多加2个点
        } else {
            30//最少取30个点
        }

        for (i in 1..count) {
            when {
                i % 5 == 0 -> samples.add(30)
                i % 5 == 1 -> samples.add(40)
                i % 5 == 2 -> samples.add(50)
                i % 5 == 3 -> samples.add(40)
                else -> samples.add(30)
            }
        }
        return samples.toIntArray()
    }

    fun adjustedSamples(highDArr: IntArray, recordTime: Int): IntArray {
        val numSamples = highDArr.size
        val maxReadSamples: Int = if (recordTime <= 3) {
            30//最少取30个点
        } else {
            30 + (recordTime - 3) * 2//每多一秒，在30个基础上多加2个点
        }

        // 算出需要取值的position，并暂存
        val adjustedSamples = IntArray(maxReadSamples)
        for (i in 0 until maxReadSamples) {
            adjustedSamples[i] = i * numSamples / maxReadSamples
        }

        var adjustedSamplesIndex = 0
        var maxSub = 0//从上一个找到的值开始到现在为止的最大值
        var preFindPosition = 0//上一次找到的索引
        var preMaxValue = 0
        var findNextPosition = adjustedSamples[adjustedSamplesIndex]
        var totalMaxValue = 0
        for (i in highDArr.indices) {
            maxSub += highDArr[i]

            //找区间最大值
            if (highDArr[i] > preMaxValue) {
                preMaxValue = highDArr[i]
            }

            if (i == findNextPosition) {
                //取均值并赋值
                adjustedSamples[adjustedSamplesIndex] = if (findNextPosition - preFindPosition <= 0) maxSub else maxSub / (findNextPosition - preFindPosition)

                //找总最大值
                if (preMaxValue > totalMaxValue) {
                    totalMaxValue = preMaxValue
                }

                //下一个需要找的点
                val next = ++adjustedSamplesIndex
                if (next < maxReadSamples) {
                    preFindPosition = findNextPosition
                    findNextPosition = adjustedSamples[adjustedSamplesIndex]
                    maxSub = 0
                    preMaxValue = 0
                } else {
                    break
                }
            }
        }

        //将值计算到5-100的区间
        for (i in 0 until maxReadSamples) {
            adjustedSamples[i] = Math.max(10, 100 * adjustedSamples[i] / totalMaxValue)
        }

        return adjustedSamples
    }
}