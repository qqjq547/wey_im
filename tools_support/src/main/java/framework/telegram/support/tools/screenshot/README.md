ScreenShotUtils

- 首先判断当前Activity是否在前台以及是否有读取文件和获取任务栈的权限(暂不使用此方法,默认跳过此方法)
    1. android.permission.READ_EXTERNAL_STORAGE
    2. android.permission.GET_TASKS
    3. android:name="android.permission.PACKAGE_USAGE_STATS"  tools:ignore="ProtectedPermissions" )
    4. MediaStore.Images.Media.INTERNAL_CONTENT_URI
    5. MediaStore.Images.Media.EXTERNAL_CONTENT_URI
- 然后启动下面方式来检测
- 利用FileObserver监听某个目录中资源变化情况
- 利用ContentObserver监听全部资源的变化
- 监听截屏快捷按键 ( 由于厂商自定义Android系统的多样性，再加上快捷键的不同以及第三方应用，监听截屏快捷键这事基本不靠谱，可以直接忽略 )
- 当监听到并且判断为合规则后，发送ScreenShotDetectionEvent
- 调用者需自行根据生命周期来调用startScreenShotsListen和stopScreenShotsListen
- 请求权限建议在设置 "截图通知" 里做(暂不使用此方法,默认跳过此方法)
