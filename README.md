# Lumika Android

在 Android 上运行 Lumika 客户端.

最新版本 `v3.12.0` 已支持 Lumika 内核直接在 Android 环境下运行, 即装即用.

## 下载

请到 [Releases](https://github.com/ERR0RPR0MPT/lumika-android/releases) 下载最新已构建好的 APK.

## 使用

教程请见[哔哩哔哩](https://www.bilibili.com/video/BV1V34y1g78f/)

## 注意

Lumika Android 推荐处理 100M 以下的小文件.

Lumika 编解码所占用的存储空间极大, 1G 源文件编码后会产生大约 8~10G 的编码视频, 使用时请留意手机剩余存储空间, 

## 构建

请在 `app/libs` 目录下放置使用 `gomobile` 工具编译的 Lumika 内核的 `.aar` 动态库文件.

项目使用 `FFmpegKit` 库作为 `FFmpeg` 和 `FFprobe` 的 Java 封装库.

然后执行:

```shell
./gradlew assembleDebug
```

得到的 APK 位于 `app/build/outputs/apk/release/lumika-${version}-${ABI}.apk`.

使用 Release 模式编译出的应用可能存在 `crash` 等问题，请使用 Debug 模式编译.
