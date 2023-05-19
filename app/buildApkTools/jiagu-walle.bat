@echo off
set package=%date:~0,4%-%date:~5,2%-%date:~8,2%---%time:~0,2%-%time:~3,2%-%time:~6,2%

echo Signature
apksigner-27.jar sign --ks D:\xtelegram\app\deshi.jks  --ks-key-alias deshi  --ks-pass pass:deshi123456  --key-pass pass:deshi123456  --out jiagu-target/app_legu_signed.apk   jiagu-target/*app-release*_legu.apk

echo ChannelInfo: 
for /f "delims=[" %%i in (jiagu-target/channelInfo.txt) do echo %%i

echo Start packaging channel package......
java -jar walle-cli-all.jar batch -f jiagu-target/channelInfo.txt jiagu-target/app_legu_signed.apk release/%package%
cd release/%package%
ren *.apk release.apk
cd ../..
echo Packaged, apk path is release/%package%
del jiagu-target\app_legu_signed.apk
java -jar walle-cli-all.jar show release/%package%/release.apk
pause