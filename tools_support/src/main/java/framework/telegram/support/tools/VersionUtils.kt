package framework.telegram.support.tools

/**
 * 版本操作工具类
 */
object VersionUtils {

    fun getVersionName(versionName: String): Int {
        val p = "."
        try {
            var version = ""
            if (versionName.contains(p)) {
                val arr = versionName.split(p)
                if (arr.size >= 3) {
                    arr.forEach {
                        version += it
                    }

                }
            }
            return version.toInt()

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

}
