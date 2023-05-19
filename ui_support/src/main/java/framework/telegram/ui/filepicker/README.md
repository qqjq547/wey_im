# 说明
1.  https://github.com/rosuH/AndroidFilePicker.git
2.  将源项目的gradle版本和Android support进行修改
3.  源项目文档：
    >   https://github.com/rosuH/AndroidFilePicker/blob/master/README_CN.md



     - 在 `Activity` 或 `Fragment` 中启动
        - 从一行代码开始
     - 浏览本地存储中的所有文件
        - 自定义根目录
        - 内置默认文件类型和文件鉴别器
        - 或者你可以自己实现文件类型
     - 自定义列表过滤器
        - 只想显示图片（或视频，音频......）？ 没问题！
        - 当然，你也可只显示文件夹
     - 自定义`item`点击事件：只需要实现监听器
     - 四个内置主题和自定义主题
     - 还有更多待你自己探索的特性（？）
    *获取结果*：`onActivityResult`接受消息，然后调用`FilePickerManager.obtainData()`获取保存的数据，**结果是所选取文件的路径列表(`ArrayList<String>()`)**

    ```kotlin
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            FilePickerManager.instance.REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    val list = FilePickerManager.instance.obtainData()
                    // do your work
                } else {
                    Toast.makeText(this@SampleActivity, "没有选择任何东西~", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    ```

    功能 & 特点

    1. 链式调用
    2. 默认选中实现
       - 点击条目(`item`)无默认实现
       - 点击`CheckBox`为选中
       - 长按条目为更改选中状态：选中/取消选中
    3. 内置四种主题配色 + 可自定义配色
       - 查看主题颜色示意图，然后调用`setTheme()`传入自定义主题
    4. 默认实现多种文件类型
       - 实现`IFileType`接口来实现你的文件类型
       - 实现`AbstractFileType`抽象类来实现你的文件类型甄别器
    5. 公开文件过滤接口
       - 实现`AbstractFileFilter`抽象类来定制你自己的文件过滤器，这样可以控制文件列表的展示内容
    6. 多种可配置选项
       1. 选中时是否忽略文件夹
       2. 是否显示隐藏文件夹（以符号`.`开头的，视为隐藏文件或隐藏文件夹）
       3. 可配置导航栏的文本，默认显示、多选文本、取消选择文本以及根目录默认名称
    7. 公开条目(`item`)选择监听器，可自定义条目被点击的实现

