cmake   ^
-H.\    ^
-B.\ninja   ^
-DANDROID_ABI=armeabi-v7a   ^
-DANDROID_PLATFORM=android-23   ^
-DANDROID_NDK=C:/Users\HP\AppData\Local\Android\Sdk\ndk\26.1.10909125   ^
-DCMAKE_TOOLCHAIN_FILE=C:\Users\HP\AppData\Local\Android\Sdk\ndk\26.1.10909125\build\cmake\android.toolchain.cmake  ^
-G Ninja

ninja -C .\ninja