package framework.telegram.business.utils

/**
 * Created by lzh on 19-5-24.
 * INFO:  用来校验登录状态的信息
 */


import android.content.Context
import framework.telegram.business.R
import framework.telegram.support.tools.ExpandClass.toast

object UserInfoCheckUtil {

    /**
     * 检查手机号
     * @param context
     * @param mobile
     * @return
     */
    fun checkMobile(context: Context, mobile: String, countyCode: String): Boolean {
        if (ValidationUtils.isEmpty(mobile)) {
            context.toast(context.getString(R.string.bus_login_mobile_input_error_1))
            return false
        }
        if (!phoneNumberIsAvailable(mobile, countyCode)) {
            context.toast(context.getString(R.string.bus_login_mobile_input_error_2))
            return false
        }
        return true
    }


    fun checkMobile2(context: Context, mobile: String, countyCode: String): Boolean {
        if (ValidationUtils.isEmpty(mobile)) {
            return false
        }
        if (!phoneNumberIsAvailable(mobile, countyCode)) {
            return false
        }
        return true
    }

    /**
     * 检查密码
     * @param context
     * @param password
     * @return
     */
    fun checkPassword(context: Context, password: String): Boolean {
        if (ValidationUtils.isEmpty(password)) {
            context.toast(context.getString(R.string.bus_login_password_input_error_1))
            return false
        }
        if (password.length < 6 || password.length > 24) {
            context.toast(context.getString(R.string.bus_login_password_input_error_2))
            return false
        }
        return true
    }

    /**
     * 检查新旧密码
     * @param context
     * @return
     */
    fun doubleCheckPassword(context: Context, newPassword: String, oldPassword: String): Boolean {
        if (newPassword != oldPassword) {
            context.toast(context.getString(R.string.bus_login_password_input_error_3))
            return false
        }
        return true
    }

    /**
     * 检查短信
     * @param context
     * @param smdCode
     * @return
     */
    fun checkSmsCode(context: Context, smdCode: String): Boolean {
        if (ValidationUtils.isEmpty(smdCode)) {
            context.toast(context.getString(R.string.bus_login_sms_code_input_error_1))
            return false
        }
        if (!ValidationUtils.isNumber(smdCode) || smdCode.length != 4) {
            context.toast(context.getString(R.string.bus_login_sms_code_input_error_2))
            return false
        }
        return true
    }

    /**
     * 检查用户
     * @param context
     * @param userName
     * @return
     */
    fun checkUserName(context: Context, userName: String): Boolean {
        if (ValidationUtils.isEmpty(userName)) {
            context.toast(context.getString(R.string.common_user_name_input_error_1))
            return false
        }
        if (userName.length < 1 || userName.length > 10) {
            context.toast(context.getString(R.string.common_user_name_input_error_2))
            return false
        }
        return true
    }

    private fun phoneNumberIsAvailable(phoneNumber: String, countyCode: String): Boolean {
        if ("+86" == countyCode) {
            if (phoneNumber.length == 11 && ValidationUtils.isChinaMobileNumberValidation(phoneNumber)) {
                return true
            }
        } else if ("+84" == countyCode) {
            if (phoneNumber.length == 9) {
                return true
            }
        } else {
            if (ValidationUtils.isNumber(phoneNumber) && phoneNumber.length >= 5 && phoneNumber.length <= 20) {
                return true
            }
        }
        return false
    }

}
