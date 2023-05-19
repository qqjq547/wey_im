package framework.telegram.message.ui.preview

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import com.alibaba.android.arouter.facade.annotation.Route
import com.bumptech.glide.Glide
import com.bumptech.glide.disklrucache.DiskLruCache
import com.bumptech.glide.load.engine.cache.DiskCache
import com.bumptech.glide.load.engine.cache.SafeKeyGenerator
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.bumptech.glide.signature.EmptySignature
import framework.telegram.message.R
import framework.telegram.message.bridge.Constant
import framework.telegram.support.BaseApp
import framework.telegram.support.StatusBarUtil
import framework.telegram.support.account.AccountManager
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.FileHelper
import framework.telegram.support.tools.SafeBitmapUtils
import framework.telegram.support.tools.file.DirManager

import java.io.File

import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.utils.BitmapUtils
import framework.telegram.ui.utils.glide.DataCacheKey
import framework.telegram.ui.widget.CommonLoadindView
import framework.telegram.ui.widget.scale.ImageSource
import framework.telegram.ui.widget.scale.SubsamplingScaleImageView
import kotlinx.android.synthetic.main.msg_activity_avatar_preview.*
import kotlinx.android.synthetic.main.msg_activity_download_picture.custom_toolbar
import kotlinx.android.synthetic.main.msg_common_preview_activity.*
import java.io.IOException
import java.util.*

@Route(path = Constant.ARouter.ROUNTE_MSG_AVATAR_PREVIEW)
class AvatarPreviewActivity : AppCompatActivity() {

    private var mImageFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var uri: Uri? = null
        val source = intent.getStringExtra("imageUrl")
        if (!TextUtils.isEmpty(source)) {
            uri = Uri.parse(source)
        }

        if (uri == null) {
            finish()
            return
        }

        setContentView(R.layout.msg_activity_avatar_preview)
        overridePendingTransition(R.anim.anim_alpha_in, 0)
        StatusBarUtil.justMDarkMode(this)
        StatusBarUtil.immersive(this)

        custom_toolbar.setToolbarColor(R.color.transparent)
        custom_toolbar.setStatuBarColor(R.color.transparent)

        init(uri)
    }

    private fun init(source: Uri) {
        val progress = findViewById<CommonLoadindView>(R.id.normal_background_progress)
        custom_toolbar.setBackIcon(R.drawable.ic_close_white) {
            finish()
        }

        scale_image_view.setOnClickListener { finish() }
        scale_image_view?.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
        scale_image_view?.minScale = 1F//最小显示比例
        scale_image_view?.maxScale = 20.0F//最大显示比例

        Glide.with(this@AvatarPreviewActivity).load(source).into(object : SimpleTarget<Drawable>() {
            override fun onResourceReady(p0: Drawable, p1: Transition<in Drawable>?) {
                val bitmap = BitmapUtils.drawable2Bitmap(p0)
                scale_image_view?.setImage(ImageSource.bitmap(bitmap))

                getCacheFile(source.toString())?.absoluteFile?.let {
                    mImageFile = it
                }
                progress?.clearAnimation()
                progress?.visibility = View.GONE
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                super.onLoadFailed(errorDrawable)
                progress?.clearAnimation()
                progress?.visibility = View.GONE
            }
        })

        scale_image_view?.setOnLongClickListener(View.OnLongClickListener {
            if (!hasWindowFocus()) {
                return@OnLongClickListener false
            }

            val list = ArrayList<String>()
            list.add(getString(R.string.save_the_image))
            list.add(getString(R.string.cancel))
            AppDialog.showBottomListView(this@AvatarPreviewActivity, this@AvatarPreviewActivity, list) { _, index, _ ->
                when (index) {
                    0 -> {
                        saveBitmap(mImageFile)
                    }
                    1 -> {
                    }
                }
            }
            false
        })
    }

    private fun getCacheFile(url: String?): File? {
        if (TextUtils.isEmpty(url)) {
            return null
        }

        val dataCacheKey = DataCacheKey(GlideUrl(url), EmptySignature.obtain())
        val safeKeyGenerator = SafeKeyGenerator()
        val safeKey = safeKeyGenerator.getSafeKey(dataCacheKey)
        try {
            val cacheSize = 100 * 1000 * 1000L
//            val uuid = if (!AccountManager.hasLoginAccount()) UUID.nameUUIDFromBytes("default".toByteArray()).toString() else AccountManager.getLoginAccountUUid()
            val uuid = UUID.nameUUIDFromBytes("default".toByteArray()).toString()
            val cacheDir = File(DirManager.getImageCacheDir(this, uuid), DiskCache.Factory.DEFAULT_DISK_CACHE_DIR)
            val diskLruCache = DiskLruCache.open(cacheDir, 1, 1, cacheSize)
            val value = diskLruCache.get(safeKey)
            if (value != null) {
                return value.getFile(0)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    private fun saveBitmap(file: File?) {
        if (file?.exists() == true) {
            if (FileHelper.insertImageToGallery(this@AvatarPreviewActivity, file)) {
                toast(getString(R.string.save_success))
            } else {
                toast(getString(R.string.save_fail))
            }
        } else {
            toast(getString(R.string.save_fail))
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.anim_alpha_out)
    }

}
