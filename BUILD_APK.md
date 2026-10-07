# APK 打包与安装

## 环境准备

安装 Android Studio、Android SDK Platform 34 和 JDK 17。用 Android Studio 打开项目并完成 Gradle Sync。若项目没有 Gradle Wrapper，可在 Android Studio 的 Gradle 工具窗口运行 `wrapper` 任务，或在安装 Gradle 后从项目根目录运行 `gradle wrapper --gradle-version 8.2.1`。

## Debug APK

在项目根目录运行：

```sh
./gradlew assembleDebug
```

APK 路径：`app/build/outputs/apk/debug/app-debug.apk`

连接手机并开启 USB 调试后，可运行：

```sh
./gradlew installDebug
```

## Release 签名

在项目根目录外生成密钥库，并妥善备份：

```sh
keytool -genkeypair -v \
  -keystore "$HOME/quicksnapdiary-release.jks" \
  -alias quicksnapdiary \
  -keyalg RSA -keysize 2048 -validity 10000
```

在 `~/.gradle/gradle.properties` 中配置以下属性（把路径和密码替换为自己的值）：

```properties
RELEASE_STORE_FILE=/Users/你的用户名/quicksnapdiary-release.jks
RELEASE_STORE_PASSWORD=你的密钥库密码
RELEASE_KEY_ALIAS=quicksnapdiary
RELEASE_KEY_PASSWORD=你的密钥密码
```

项目的 `app/build.gradle.kts` 会从 Gradle 属性读取这些签名配置。不要把密钥库或密码提交到版本控制。

生成 Release APK：

```sh
./gradlew assembleRelease
```

APK 路径：`app/build/outputs/apk/release/app-release.apk`

## 安装到手机

1. Gradle 直接安装 Debug 版本：

   ```sh
   ./gradlew installDebug
   ```

2. 使用 ADB 安装 APK：

   ```sh
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

3. 将 APK 传到手机，在文件管理器中点击 APK 并按提示安装。

直接安装时，Android 可能要求为当前使用的文件管理器开启“允许安装未知来源应用”。
