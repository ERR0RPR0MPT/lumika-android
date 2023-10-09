# Lumika Android

在 Android 上运行 Lumika 和 Lumika Web 的客户端.

## 下载

请到 [Releases](https://github.com/ERR0RPR0MPT/lumika-android/releases) 下载最新已构建好的 APK.

## 构建

请在 `src/main/libs/{abi}` 目录下放置静态编译的 `liblumika.so`, `libffmpeg.so` 和 `libffprobe.so` 三个库.

`liblumika.so` 只需将对应架构的 Lumika 可执行文件重命名即可, 静态编译的 `ffmpeg` 可执行文件请搜寻 `Google`.

然后执行:

```shell
./gradlew assembleDebug
```

得到的 APK 位于 `app/build/outputs/apk/release/lumika-${version}-${ABI}.apk`.
