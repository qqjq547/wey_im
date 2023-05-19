package framework.telegram.support.tools.screenshot

import android.graphics.BitmapFactory
import android.os.FileObserver

class ScreenShotsFileObserver(system_screenshots_path: String, mask: Int,callback:(result: Boolean, targetName: String)->Unit) : FileObserver(system_screenshots_path,mask){

    private val MAX_RETRY:Int = 5
    private val DELAY: Long = 500
    private val SCREEN_SHOTS_DIR = system_screenshots_path
    private var lastTimeScreenShots: String = ""
    private val mCallBack = callback

    /**
     * The event handler, which must be implemented by subclasses.
     *
     *
     * This method is invoked on a special FileObserver thread.
     * It runs independently of any threads, so take care to use appropriate
     * synchronization!  Consider using [Handler.post] to shift
     * event handling work to the main thread to avoid concurrency problems.
     *
     *
     * Event handlers must not throw exceptions.
     *
     * @param event The type of event which happened
     * @param path The path, relative to the main monitored file or directory,
     * of the file or directory which triggered the event.  This value can
     * be `null` for certain events, such as [.MOVE_SELF].
     */
    override fun onEvent(event: Int, path: String?) {
        if (event == FileObserver.CREATE && path != null && lastTimeScreenShots != path){
            var isTarget: Boolean = false
            lastTimeScreenShots = path
            val targetPath = SCREEN_SHOTS_DIR + path
            var retry = 0
            while (true){
                try {
                    Thread.sleep(DELAY)
                }catch (e: Exception){}
                try {
                    BitmapFactory.decodeFile(targetPath)
                    if (!targetPath.contains("__thumb") && !targetPath.contains("__encrypt")) {
                        isTarget = true
                        break
                    }
                }catch (e: Exception){
                    if (retry > MAX_RETRY)
                        break
                    retry++
                }
            }
            mCallBack(isTarget,targetPath)
        }
    }
}