package framework.telegram.message.ui.location.presenter

import framework.telegram.message.ui.location.bean.ClientLatLng
import framework.telegram.message.ui.location.bean.ClientLocation
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView
import framework.telegram.support.tools.exception.ClientException

/**
 * Created by lzh on 19-5-17.
 * INFO:
 */
interface LocationContract {

    interface Presenter {

        /**
         * 获取当前位置信息
         *
         * @param checkOpenGps 是否检测GPS开关
         * @param hasAddress   是否包含地址位置信息
         * @param immediately  是否使用短时间内的缓存地理位置(false会获取最新的地理位置)
         */
        fun getClientLocation(checkOpenGps: Boolean, hasAddress: Boolean, immediately: Boolean)

        /**
         * 获取当前位置信息
         *
         * @param checkOpenGps 是否检测GPS开关
         * @param hasAddress   是否包含地址位置信息
         * @param immediately  是否使用短时间内的缓存地理位置(false会获取最新的地理位置)
         * @param quick        是否快速获取(false获取时间比较长)
         */
        fun getClientLocation(checkOpenGps: Boolean, hasAddress: Boolean, immediately: Boolean, quick: Boolean)

        fun onActivityResume()

        fun onActivityCreate()

        fun onActivityRelease()
    }

    interface View {

        /**
         * 获取到当前位置
         *
         * @param clientLatLng
         */
        fun onGetClientLocation(clientLatLng: ClientLatLng?, isCache: Boolean)

        /**
         * 获取到当前位置
         *
         * @param clientLocation
         */
        fun onGetClientLocation(clientLocation: ClientLocation?, isCache: Boolean)

        /**
         * 判断到当前GPS开关为关闭
         */
        fun onCheckGpsDisEnable()

        /**
         * 获取当前位置失败
         */
        fun onGetClientLocationError(e: ClientException)

        /**
         * GPS变为可用了
         */
        fun onGPSChangeEnable()

    }
}