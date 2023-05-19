# 使用说明

 **jiagu-walle.bat** 
 1. 只识别 jiagu-target文件夹下的channelInfo.txt(渠道信息)
 2. 只识别 jiagu-target文件夹下的带**legu.apk**(即已经加固apk)
 3. 会进行二次签名(使用的apksign.jar是android 27的)
 4. 会生成 release.apk
 5. 生成的apk只会在 release/(生成日期时间)下
 6. 会打印出channelInfo.txt(渠道信息)中的所有信息
 7. 会打印出release.apk所在位置