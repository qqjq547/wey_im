package framework.telegram.support.tools.download

/**
 * Created by lzh on 19-7-23.
 * INFO:
 */

interface DownloadContract {

    interface Presenter {

        fun download()

        fun onDestroy()
    }

    interface View {
        fun downloadSucess(str: String)

        fun downloadError(str: String)

        fun setDownloadProgress(progress: Float)
    }
}