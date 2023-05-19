package framework.telegram.business.ui.other

import android.os.Bundle
import com.alibaba.android.arouter.facade.annotation.Route
import com.bumptech.glide.Glide
import com.facebook.drawee.backends.pipeline.Fresco
import framework.telegram.support.BaseActivity
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_BUS_CLEAR_STORAGE
import framework.telegram.support.account.AccountManager
import framework.telegram.support.tools.FileUtils
import framework.telegram.support.tools.ThreadUtils
import framework.telegram.support.tools.file.DirManager
import framework.telegram.ui.dialog.AppDialog
import kotlinx.android.synthetic.main.bus_clear_storage_activity.*

@Route(path = ROUNTE_BUS_CLEAR_STORAGE)
class ClearStorageActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bus_clear_storage_activity)

        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }

        custom_toolbar.showCenterTitle(getString(R.string.clear_storage_title))

        ThreadUtils.runOnIOThread {
            val totalSize = DirManager.getClearableFileSize(applicationContext, AccountManager.getLoginAccountUUid()) + Fresco.getImagePipeline().usedDiskCacheSize
            ThreadUtils.runOnUIThread {
                if (totalSize <= FileUtils.ONE_MB_BI.toLong()) {
                    storage_cache_size.text = "0M"
                } else {
                    storage_cache_size.text = "${FileUtils.byteCountToDisplaySize(totalSize)}"
                }
            }
        }

        storage_cache_clear_button.setOnClickListener {
            AppDialog.show(this@ClearStorageActivity, this@ClearStorageActivity) {
                message(text = getString(R.string.clear_storage_dialog_tip))
                negativeButton(text = context.getString(R.string.confirm_two), click = {
                    ThreadUtils.runOnIOThread {
                        Glide.get(applicationContext).clearDiskCache()
                        Fresco.getImagePipeline().clearDiskCaches()
                        DirManager.clearStorage(applicationContext, AccountManager.getLoginAccountUUid())

                        ThreadUtils.runOnUIThread {
                            this@ClearStorageActivity.storage_cache_size.text = "0M"
                        }
                    }
                })
                positiveButton(text = context.getString(R.string.cancel))
            }
        }
    }
}