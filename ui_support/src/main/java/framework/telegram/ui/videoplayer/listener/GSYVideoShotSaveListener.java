package framework.telegram.ui.videoplayer.listener;


import java.io.File;

/**
 * 截屏保存结果
 * Created by guoshuyu on 2017/9/21.
 */

public interface GSYVideoShotSaveListener {
    void result(boolean success, File file);
}
