package framework.telegram.support.tools

import android.content.Context
import android.graphics.Color
import com.bigkoo.pickerview.builder.OptionsPickerBuilder
import com.bigkoo.pickerview.listener.OnOptionsSelectListener
import com.bigkoo.pickerview.view.OptionsPickerView
import framework.telegram.support.BaseApp
import framework.telegram.support.R

/**
 * Created by huyf on 2019/9/17 16:23
 */
object AppLockTimePickerUtil {

    fun showSelectTimePicker(context: Context, defaultTimeValue: Int, onConfirm: ((timeName: String, timeValue: Int) -> Unit)): OptionsPickerView<String> {
        val mTimeItems = arrayListOf<String>(
                context.getString(R.string.minute_1)
                , context.getString(R.string.minute_5)
                , context.getString(R.string.minute_10)
                , context.getString(R.string.minute_15)
                , context.getString(R.string.hour_1)
                , context.getString(R.string.hour_5)
                , context.getString(R.string.hour_12))

        val pvOptions: OptionsPickerView<String> = OptionsPickerBuilder(context, OnOptionsSelectListener { options1, _, _, _ ->
            //点击确定时回调
            var time = 0
            when (options1) {
                0 -> {
                    time = 60
                }
                1 -> {
                    time = 300
                }
                2 -> {
                    time = 600
                }
                3 -> {
                    time = 900
                }
                4 -> {
                    time = 3600
                }
                5 -> {
                    time = 3600 * 5
                }
                6 -> {
                    time = 3600 * 12
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
            60 -> {
                return 0
            }
            300 -> {
                return 1
            }
            600 -> {
                return 2
            }
            900 -> {
                return 3
            }
            3600 -> {
                return 4
            }
            3600 * 5 -> {
                return 5
            }
            3600 * 12 -> {
                return 6
            }
        }
        return 0
    }

    fun timeValue2TimeName(time: Int): String {
        val mTimeItems = arrayListOf<String>(
                BaseApp.app.getString(R.string.minute_1)
                , BaseApp.app.getString(R.string.minute_5)
                , BaseApp.app.getString(R.string.minute_10)
                , BaseApp.app.getString(R.string.minute_15)
                , BaseApp.app.getString(R.string.hour_1)
                , BaseApp.app.getString(R.string.hour_5)
                , BaseApp.app.getString(R.string.hour_12) )
        when (time) {
            60 -> {
                return mTimeItems[0]
            }
            300 -> {
                return mTimeItems[1]
            }
            600 -> {
                return mTimeItems[2]
            }
            900 -> {
                return mTimeItems[3]
            }
            3600 -> {
                return mTimeItems[4]
            }
            3600 * 5 -> {
                return mTimeItems[5]
            }
            3600 * 12 -> {
                return mTimeItems[6]
            }
        }
        return String.format(BaseApp.app.getString(R.string.sec_mat), time.toString())
    }
}