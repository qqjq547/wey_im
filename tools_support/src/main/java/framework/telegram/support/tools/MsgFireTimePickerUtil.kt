package framework.telegram.support.tools

import android.content.Context
import android.graphics.Color
import com.bigkoo.pickerview.builder.OptionsPickerBuilder
import com.bigkoo.pickerview.listener.OnOptionsSelectListener
import com.bigkoo.pickerview.view.OptionsPickerView
import framework.telegram.support.BaseApp
import framework.telegram.support.R

/**
 * Created by yanggl on 2019/9/17 16:23
 */
object MsgFireTimePickerUtil {
    fun showSelectTimePicker(context: Context , defaultTimeValue:Int , onConfirm: ((timeName: String, timeValue: Int) -> Unit)) :OptionsPickerView<String> {
        val mTimeItems = arrayListOf<String>(context.getString(R.string.seconds_5)
                , context.getString(R.string.seconds_10)
                , context.getString(R.string.seconds_30)
                , context.getString(R.string.minute_1)
                , context.getString(R.string.hour_1)
                , context.getString(R.string.hour_6)
                , context.getString(R.string.hour_12)
                , context.getString(R.string.day_1)
                , context.getString(R.string.day_3)
                , context.getString(R.string.day_7))

        val pvOptions: OptionsPickerView<String> = OptionsPickerBuilder(context, OnOptionsSelectListener { options1, _, _, _ ->
            //点击确定时回调
            var time = 0
            when (options1) {
                0 -> {
                    time = 5
                }
                1 -> {
                    time = 10
                }
                2 -> {
                    time = 30
                }
                3 -> {
                    time = 60
                }
                4 -> {
                    time = 3600
                }
                5 -> {
                    time = 3600 * 6
                }
                6 -> {
                    time = 3600 * 12
                }
                7 -> {
                    time = 3600 * 24
                }
                8 -> {
                    time = 3600 * 24 * 3
                }
                9 -> {
                    time = 3600 * 24 * 7
                }
            }
            onConfirm.invoke(mTimeItems[options1], time)
        }).setOptionsSelectChangeListener { options1, options2, options3 ->
            //滚轮改变时回调
        }
                .setSubmitText(context.getString(R.string.confirm))//确定按钮文字
                .setCancelText(context.getString(R.string.cancel))//取消按钮文字
                .setSubCalSize(13)//确定和取消文字大小
                .setSubmitColor(Color.parseColor("#178aff"))//确定按钮文字颜色
                .setCancelColor(Color.parseColor("#464646"))//取消按钮文字颜色
                .setTitleBgColor(Color.WHITE)//标题背景颜色
                .setBgColor(Color.WHITE)//滚轮背景颜色
                .setContentTextSize(23)//滚轮文字大小
                .isCenterLabel(false) //是否只显示中间选中项的label文字，false则每项item全部都带有label。
                .setCyclic(false, false, false)//循环与否
                .setSelectOptions(getDagaultOptions(defaultTimeValue))  //设置默认选中项
                .isDialog(false)//是否显示为对话框样式
                .isRestoreItem(false)//切换时是否还原，设置默认选中第一项。
                .build<String>()
        pvOptions.setPicker(mTimeItems)//添加数据源
        pvOptions.show()
        return pvOptions
    }

    private fun getDagaultOptions(defaultTimeValue: Int): Int {
        when (defaultTimeValue) {
            5 -> {
                return 0
            }
            10 -> {
                return 1
            }
            30 -> {
                return 2
            }
            60 -> {
                return 3
            }
            3600 -> {
                return 4
            }
            3600 * 6 -> {
                return 5
            }
            3600 * 12 -> {
                return 6
            }
            3600 * 24 -> {
                return 7
            }
            3600 * 24 * 3 -> {
                return 8
            }
            3600 * 24 * 7 -> {
                return 9
            }
        }
        return 0
    }

    fun timeValue2TimeName(time: Int): String {
        val mTimeItems = arrayListOf<String>(BaseApp.app.getString(R.string.seconds_5)
                , BaseApp.app.getString(R.string.seconds_10)
                , BaseApp.app.getString(R.string.seconds_30)
                , BaseApp.app.getString(R.string.minute_1)
                , BaseApp.app.getString(R.string.hour_1)
                , BaseApp.app.getString(R.string.hour_6)
                , BaseApp.app.getString(R.string.hour_12)
                , BaseApp.app.getString(R.string.day_1)
                , BaseApp.app.getString(R.string.day_3)
                , BaseApp.app.getString(R.string.day_7))
        when (time) {
            5 -> {
                return mTimeItems[0]
            }
            10 -> {
                return mTimeItems[1]
            }
            30 -> {
                return mTimeItems[2]
            }
            60 -> {
                return mTimeItems[3]
            }
            3600 -> {
                return mTimeItems[4]
            }
            3600 * 6 -> {
                return mTimeItems[5]
            }
            3600 * 12 -> {
                return mTimeItems[6]
            }
            3600 * 24 -> {
                return mTimeItems[7]
            }
            3600 * 24 * 3 -> {
                return mTimeItems[8]
            }
            3600 * 24 * 7 -> {
                return mTimeItems[9]
            }
        }
        return String.format(BaseApp.app.getString(R.string.sec_mat),time.toString())
    }

   
    fun showSelectTimePickerChat(context: Context , defaultTimeValue:Int , onConfirm: ((timeName: String, timeValue: Int) -> Unit)) :OptionsPickerView<String> {
         val mTimeItemsChat = arrayListOf<String>(context.getString(R.string.seconds_5)
                , context.getString(R.string.seconds_10)
                , context.getString(R.string.seconds_30)
                , context.getString(R.string.minute_1)
                , context.getString(R.string.hour_1)
                , context.getString(R.string.hour_6)
                , context.getString(R.string.hour_12)
                , context.getString(R.string.day_1)
                 , context.getString(R.string.day_3)
                , context.getString(R.string.day_7)
                , context.getString(R.string.close))
        val pvOptions: OptionsPickerView<String> = OptionsPickerBuilder(context, OnOptionsSelectListener { options1, _, _, _ ->
            //点击确定时回调
            var time = 0
            when (options1) {
                0 -> {
                    time = 5
                }
                1 -> {
                    time = 10
                }
                2 -> {
                    time = 30
                }
                3 -> {
                    time = 60
                }
                4 -> {
                    time = 3600
                }
                5 -> {
                    time = 3600 * 6
                }
                6 -> {
                    time = 3600 * 12
                }
                7 -> {
                    time = 3600 * 24
                }
                8 -> {
                    time = 3600 * 24 * 3
                }
                9 -> {
                    time = 3600 * 24 * 7
                }
                10 ->{
                    time = -1
                }
            }
            onConfirm.invoke(mTimeItemsChat[options1], time)
        }).setOptionsSelectChangeListener { options1, options2, options3 ->
            //滚轮改变时回调
        }
                .setSubmitText(context.getString(R.string.confirm))//确定按钮文字
                .setCancelText(context.getString(R.string.cancel))//取消按钮文字
                .setSubCalSize(13)//确定和取消文字大小
                .setSubmitColor(Color.parseColor("#178aff"))//确定按钮文字颜色
                .setCancelColor(Color.parseColor("#464646"))//取消按钮文字颜色
                .setTitleBgColor(Color.WHITE)//标题背景颜色
                .setBgColor(Color.WHITE)//滚轮背景颜色
                .setContentTextSize(23)//滚轮文字大小
                .isCenterLabel(false) //是否只显示中间选中项的label文字，false则每项item全部都带有label。
                .setCyclic(false, false, false)//循环与否
                .setSelectOptions(getDagaultOptions(defaultTimeValue))  //设置默认选中项
                .isDialog(false)//是否显示为对话框样式
                .isRestoreItem(false)//切换时是否还原，设置默认选中第一项。
                .build<String>()
        pvOptions.setPicker(mTimeItemsChat)//添加数据源
        pvOptions.show()
        return pvOptions
    }

    fun timeValueBanTimeName(time: Int): String {
       val timeItems = getBanTimeList()
        when (time) {
            3600 -> {
                return  BaseApp.app.getString(R.string.shut_up)
            }
            3600 * 24 -> {
                return BaseApp.app.getString(R.string.shut_up)
            }
            3600 * 24 * 3 -> {
                return BaseApp.app.getString(R.string.shut_up)
            }
            3600 * 24 * 7 -> {
                return BaseApp.app.getString(R.string.shut_up)
            }
            -2 -> {
                return  BaseApp.app.getString(R.string.forever)
            }
        }
        return BaseApp.app.getString(R.string.close)
    }

    private fun getBanTimeList():List<String>{
        return  arrayListOf<String>(BaseApp.app.getString(R.string.hour_1)
                , BaseApp.app.getString(R.string.day_1)
                , BaseApp.app.getString(R.string.day_3)
                , BaseApp.app.getString(R.string.day_7)
                , BaseApp.app.getString(R.string.forever))
    }

    fun showSelectBanTimePicker(context: Context , defaultTimeValue:Int , onConfirm: ((timeName: String, timeValue: Int) -> Unit)) :OptionsPickerView<String> {
        val timeItems = getBanTimeList()
        val pvOptions: OptionsPickerView<String> = OptionsPickerBuilder(context, OnOptionsSelectListener { options1, _, _, _ ->
            //点击确定时回调
            var time = 0
            when (options1) {
                0 -> {
                    time = 3600
                }
                1 -> {
                    time = 3600 * 24
                }
                2 -> {
                    time = 3600 * 24 * 3
                }
                3 -> {
                    time = 3600 * 24 * 7
                }
                4 ->{
                    time = -2
                }
            }
            onConfirm.invoke(timeItems[options1], time)
        }).setOptionsSelectChangeListener { options1, options2, options3 ->
            //滚轮改变时回调
        }
                .setSubmitText(context.getString(R.string.confirm))//确定按钮文字
                .setCancelText(context.getString(R.string.cancel))//取消按钮文字
                .setSubCalSize(13)//确定和取消文字大小
                .setSubmitColor(Color.parseColor("#178aff"))//确定按钮文字颜色
                .setCancelColor(Color.parseColor("#464646"))//取消按钮文字颜色
                .setTitleBgColor(Color.WHITE)//标题背景颜色
                .setBgColor(Color.WHITE)//滚轮背景颜色
                .setContentTextSize(23)//滚轮文字大小
                .isCenterLabel(false) //是否只显示中间选中项的label文字，false则每项item全部都带有label。
                .setCyclic(false, false, false)//循环与否
                .setSelectOptions(getDagaultOptions(defaultTimeValue))  //设置默认选中项
                .isDialog(false)//是否显示为对话框样式
                .isRestoreItem(false)//切换时是否还原，设置默认选中第一项。
                .build<String>()
        pvOptions.setPicker(timeItems)//添加数据源
        return pvOptions
    }
}