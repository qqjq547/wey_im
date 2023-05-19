package framework.telegram.ui.qr.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.util.Log
import androidx.annotation.RequiresApi

import androidx.appcompat.app.AppCompatActivity
import framework.telegram.support.BaseActivity
import framework.telegram.support.tools.language.LocalManageUtil

import framework.telegram.ui.R


/**
 * Initial the camera
 *
 *
 * 默认的二维码扫描Activity
 */
open class CaptureActivity : BaseActivity() {

    /**
     * 二维码解析回调函数
     */
    private var analyzeCallback: CodeUtils.AnalyzeCallback = object : CodeUtils.AnalyzeCallback {
        override fun onAnalyzeSuccess( result: String) {
            implAnalyzeUrl(result)
        }

        override fun onAnalyzeFailed() {
            impFailAnalyzeUrl()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera)
        val captureFragment = CaptureFragmentNew()
        captureFragment.analyzeCallback = analyzeCallback
        supportFragmentManager.beginTransaction().replace(R.id.fl_zxing_container, captureFragment).commit()

    }

    open fun implAnalyzeUrl( result: String){

    }

    open fun impFailAnalyzeUrl( ){

    }
}