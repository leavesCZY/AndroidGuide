在 Android 端应用开发领域，应该已经有挺多开发者在使用 Jetpack Compose 了吧？

在 2021 年的七月份，Google 发布了 Jetpack Compose 的 1.0 正式版本，同年十二月份，JetBrains 也随之发布了适用于多个平台的声明式 UI 开发框架 Compose Multiplatform 的 1.0 正式版本，当前最新版本号也已经到了 1.6.11

早在 Jetpack Compose 发布 1.0 正式版之前，我就已经用 Jetpack Compose 写了一个 Android 端的俄罗斯方块小游戏。在 Compose Multiplatform 发布正式版后不久，我又将其移植到了 Windows 平台，最近我又将其扩展到了 MacOS 和 Linux 平台

- [学不动也要学，Jetpack Compose 写一个俄罗斯方块](https://juejin.cn/post/6974585048762679310)
- [不止 Android，Compose Multiplatform 初探](https://juejin.cn/post/7062533562460799013)

项目地址：[compose-multiplatform-tetris](https://github.com/leavesCZY/compose-multiplatform-tetris)

![](https://upload-images.jianshu.io/upload_images/2552605-30f9781ae1b55f21.png)

compose-multiplatform-tetris 是我个人的一个练手项目，说实话其实没啥实际用途。我最近又想用 Compose Multiplatform 来做点有实际意义的事情，想到项目中有用到腾讯的 xlog，就用 Compose Multiplatform 写了一个跨平台的 xlog 日志解析工具，免去以前需要安装 Python SDK 和通过命令行来解析日志的步骤，多少还是有点实际意义

在此也开源分享给有需要的同学

![](https://upload-images.jianshu.io/upload_images/2552605-ead0e3e629ba52ad.png)

compose-multiplatform-xlog-decode 当前一共支持 Windows、MacOs、Linux 三个平台。虽然 Compose Multiplatform 也支持移动端，我想要同时写一个 Android 端解密应用也不难，但感觉在移动端实现这种功能有点鸡肋，就只写了桌面端工具

如果开发者已经能比较熟练地使用 Jetpack Compose 的话，那上手 Compose Multiplatform 的难度就很低了，其本身也屏蔽了大部分平台相关的特性，开发者大部分精力都可以只关注于业务逻辑。compose-multiplatform-xlog-decode 的实现难点也主要在于如何解析 xlog 日志文件

xlog 是腾讯开源的一个日志记录与存储框架，支持对日志进行压缩和加密。相对应的，xlog 官方也提供了相应的解压和解密代码，对应两份不同的 Python 文件

- 日志有进行加密：[decode_mars_crypt_log_file.py](https://github.com/Tencent/mars/blob/master/mars/xlog/crypt/decode_mars_crypt_log_file.py)
- 日志没有进行加密：[decode_mars_nocrypt_log_file.py](https://github.com/Tencent/mars/blob/master/mars/xlog/crypt/decode_mars_nocrypt_log_file.py)

我就照着这两个 Python 文件，将其翻译成了功能等价的 kotlin 代码

xlog 中每段日志的结构体如下所示。在 AppednerModeSync (同步模式)下，每写一行日志都会组装成一个日志结构体写入到日志文件中。在 AppednerModeAsync (异步模式)下，mmap 中的数据是是一个日志结构体，每当往 mmap 中写入一行日志数据时，同时会修改结构体中的 length 的值

```kotlin
|magic start(char)|seq(uint16_t)|begin hour(char)|end hour(char)|length(uint32_t)|crypt key(uint32_t)|log|magic end(char)|
```

知道这种数据结构后，我们就可以通过遍历整份日志文件，找到每一个以 magic start 开头并以 magic end 结尾的日志体，从而定位到日志内容 log

以上的日志结构体，我将其翻译成 kotlin 中的一个 sealed class，每一个子类就相当于一个魔数

```kotlin
//magic start(char)
//seq(uint16_t)
//begin hour(char)
//end hour(char)
//length(uint32_t)
//crypt key(uint32_t)
//log
//magic end(char)
private sealed class Magic(val byteSize: Int) {
    data object MagicStart : Magic(byteSize = 1)
    data object Seq : Magic(byteSize = 2)
    data object BeginHour : Magic(byteSize = 1)
    data object EndHour : Magic(byteSize = 1)
    data object Length : Magic(byteSize = 4)
    data class CryptKey(val length: Int) : Magic(byteSize = length)
    data object MagicEnd : Magic(byteSize = 1) {
        const val MARK: Byte = 0x00
    }
}
```

除了需要知道日志体的结构外，还需要知道日志体是按照什么规则来进行存储的，也即需要知道其加密算法和压缩算法

- xlog 默认采用 ecdh + tea 混合加密算法，也支持替换加密算法或者直接就不进行加密，所以 CryptKey 的字节长度并不固定
- xlog 默认提供了 zlib 和 zstd 两种日志压缩算法供开发者选择
- xlog 日志文件中除了包含开发者主动写入的内容外，还包含 xlog 自动添加的一些附带信息，这些信息有的就不会进行加密和压缩

也就是说，每个日志结构体并不一定会进行加密，也并不一定会进行压缩，即使有加密和压缩，加密算法和压缩算法也并不固定

为了解析出存储规则，就需要靠 magic start 这个魔数值来标识了，不同类型的日志体其 magic start 值各不相同

我将每种存储规则都抽象成 MagicStartMark 类

```kotlin
private enum class MagicStartMark(val mark: Byte) {
    NoCompressStart(mark = 0x03),
    NoCompressStart1(mark = 0x06),
    NoCompressNoCryptStart(mark = 0x08),

    CompressStart(mark = 0x04),
    CompressStart1(mark = 0x05),
    CompressStart2(mark = 0x07),
    CompressNoCryptStart(mark = 0x09),

    SyncZstdStart(mark = 0x0A),
    SyncNoCryptZstdStart(mark = 0x0B),

    AsyncZstdStart(mark = 0x0C),
    AsyncNoCryptZstdStart(mark = 0x0D)
}
```

举个例子

- 如果日志结构体的 magic start 等于 CompressStart2，意味着日志进行了压缩和加密，且加密算法是 zlib
- 如果日志结构体的 magic start 等于 CompressNoCryptStart，意味着日志进行了压缩，但没有进行加密

原始的日志文件就相当于一个字节数组，通过遍历整个 ByteArray 就可以一步步解析出每个日志体的详细数据结构和算法类型了

```kotlin
private fun decodeLogSpace(privateKey: String, buffer: ByteArray, offset: Int): LogSpace? {
    val bufferSize = buffer.size
    if (offset < 0 || offset >= bufferSize) {
        return null
    }
    for (index in offset..<bufferSize) {
        val mark = buffer[index]
        val magicStartMark = MagicStartMark.entries.find { it.mark == mark }
        if (magicStartMark != null) {
            val cryptKeyMagic = decodeCryptKeyMagic(magicStartMark = magicStartMark)
            val cryptKeyMagicByteSize = cryptKeyMagic.byteSize
            val lengthMarkStartIndex = index + marksSizeBeforeLengthMagic
            val lengthMarkEndIndex = lengthMarkStartIndex + Magic.Length.byteSize
            if (lengthMarkEndIndex < bufferSize) {
                val logByteSize = ByteBuffer.wrap(
                    buffer,
                    lengthMarkStartIndex,
                    lengthMarkEndIndex - lengthMarkStartIndex
                ).order(ByteOrder.LITTLE_ENDIAN).getInt()
                val endMarkIndex = index + marksSizeBeforeCryptKeyMagic + cryptKeyMagicByteSize + logByteSize
                if (endMarkIndex in 0..<bufferSize && buffer[endMarkIndex] == Magic.MagicEnd.MARK) {
                    val logStartIndex = index + marksSizeBeforeCryptKeyMagic + cryptKeyMagicByteSize
                    val logBuffer = buffer.copyOfRange(
                        fromIndex = logStartIndex,
                        toIndex = logStartIndex + logByteSize
                    )
                    val teaKey = if (magicStartMark == MagicStartMark.CompressStart2 ||
                        magicStartMark == MagicStartMark.AsyncZstdStart
                    ) {
                        if (privateKey.isBlank()) {
                            throw IllegalArgumentException("日志有进行加密，需输入私钥")
                        }
                        decodeTeaKey(
                            privateKey = privateKey,
                            buffer = buffer,
                            magicStartIndex = index,
                            cryptKeyMagic = cryptKeyMagic
                        )
                    } else {
                        null
                    }
                    return LogSpace(
                        magicStartMark = magicStartMark,
                        cryptKeyMagic = cryptKeyMagic,
                        teaKey = teaKey,
                        log = logBuffer
                    )
                }
            }
        }
    }
    return null
}

private fun decodeTeaKey(
    buffer: ByteArray,
    privateKey: String,
    magicStartIndex: Int,
    cryptKeyMagic: Magic.CryptKey
): ByteArray {
    val publicKeyBytes = buffer.copyOfRange(
        fromIndex = magicStartIndex + marksSizeBeforeCryptKeyMagic,
        toIndex = magicStartIndex + marksSizeBeforeCryptKeyMagic + cryptKeyMagic.byteSize
    )
    val publicKeyHexString = StringUtils.byteArrayToHexString(bytes = publicKeyBytes)
    val publicKeyHexStringFormatted = String.format("04%s", publicKeyHexString)
    val teaKey = DecryptUtils.getECDHKey(
        publicKey = StringUtils.hexStringToByteArray(hexString = publicKeyHexStringFormatted),
        privateKey = StringUtils.hexStringToByteArray(hexString = privateKey)
    )
    return teaKey
}

private fun decodeLogSpace(logSpace: LogSpace): ByteArray {
    return when (val magicStart = logSpace.magicStartMark) {
        MagicStartMark.CompressStart2, MagicStartMark.AsyncZstdStart -> {
            val teaKey = logSpace.teaKey!!
            val decryptedLogBuffer = DecryptUtils.teaDecrypt(
                encryptedData = logSpace.log,
                key = teaKey
            )
            if (magicStart == MagicStartMark.CompressStart2) {
                DecompressUtils.zlibDecompress(data = decryptedLogBuffer)
            } else {
                DecompressUtils.zstdDecompress(data = decryptedLogBuffer)
            }
        }

        MagicStartMark.AsyncNoCryptZstdStart -> {
            DecompressUtils.zstdDecompress(data = logSpace.log)
        }

        MagicStartMark.CompressStart, MagicStartMark.CompressNoCryptStart -> {
            DecompressUtils.zlibDecompress(data = logSpace.log)
        }

        MagicStartMark.NoCompressStart, MagicStartMark.NoCompressStart1,
        MagicStartMark.NoCompressNoCryptStart, MagicStartMark.CompressStart1,
        MagicStartMark.SyncZstdStart, MagicStartMark.SyncNoCryptZstdStart -> {
            logSpace.log
        }
    }
}
```

compose-multiplatform-xlog-decode 目前可以正常解析按 xlog 默认配置生成的日志文件，也即支持按 **AppednerModeAsync、ecdh + tea 混合加密算法、zlib 压缩算法** 等规则生成的日志文件。我本来想一并支持 zstd 压缩算法，但不知道因为什么原因一直生成不了按 zstd 算法进行压缩的日志文件，也就没有进行测试了，但项目中还是有实现相应的解压逻辑

Compose Multiplatform 默认提供了 Windows、MacOs、Linux 等平台的打包指令，通过 packageReleaseExe、packageReleaseAppImage、packageReleaseDistributionForCurrentOS 这类 Task 就可以生成特定平台的产物

```kotlin
compose.desktop {
    application {
        mainClass = "github.leavesczy.xlog.decode.MainKt"
        val mPackageName = "compose-multiplatform-xlog-decode"
        nativeDistributions {
            includeAllModules = false
            modules = arrayListOf("jdk.unsupported", "java.desktop", "java.logging")
            when (currentOS) {
                OS.Windows -> {
                    targetFormats(TargetFormat.AppImage, TargetFormat.Exe)
                }

                OS.MacOS -> {
                    targetFormats(TargetFormat.Dmg)
                }

                OS.Linux -> {
                    targetFormats(TargetFormat.Deb, TargetFormat.Rpm)
                }
            }
            packageName = mPackageName
            packageVersion = "1.0.0"
            description = "compose multiplatform xlog decode"
            copyright = "© 2024 leavesCZY. All rights reserved."
            vendor = "leavesCZY"
            val resourcesDir = project.file("src/main/resources")
            windows {
                menuGroup = packageName
                dirChooser = true
                perUserInstall = true
                shortcut = true
                menu = true
                upgradeUuid = "D542171E-5CDC-428E-BF21-68FBAD85369F"
                iconFile.set(resourcesDir.resolve("windows_launch_icon.ico"))
                installationPath = packageName
            }
            macOS {
                bundleID = mPackageName
                setDockNameSameAsPackageName = true
                appStore = true
                iconFile.set(resourcesDir.resolve("macos_launch_icon.icns"))
            }
            linux {
                shortcut = true
                menuGroup = mPackageName
                iconFile.set(resourcesDir.resolve("linux_launch_icon.png"))
            }
        }
    }
}
```

由于目前 Compose Multiplatform 还不支持交叉编译，所以在生成特定平台产物时均需要在相应的系统下进行编译才行。如果每次发布时都只靠单个人的人力物力来生成多种平台产物的话，那就十分麻烦了

为了更优雅点，compose-multiplatform-xlog-decode 通过 Github Action 来实现自动化打包并发布 release。Github Action 功能十分强大，可以指定使用特定的操作系统，并依次执行指定的指令

```kotlin
create-release-distribution:
strategy:
  matrix:
    os: [ windows-latest , ubuntu-latest , macos-13 , macos-14 ]
runs-on: ${{ matrix.os }}
name: create release distribution
needs: current-time

steps:
  - if: matrix.os != 'macos-14'
    name: setup jdk
    uses: actions/setup-java@v4
    with:
      java-version: "18"
      distribution: "zulu"
      architecture: x64

  - if: matrix.os == 'macos-14'
    name: setup jdk
    uses: actions/setup-java@v4
    with:
      java-version: "18"
      distribution: "zulu"
      architecture: aarch64

  - name: checkout
    uses: actions/checkout@v4

  - name: grant execute permission for gradlew
    run: chmod +x gradlew

  - name: packageReleaseDistributionForCurrentOS
    run: ./gradlew packageReleaseDistributionForCurrentOS

  - if: matrix.os == 'windows-latest'
    name: rename File
    run: |
      mv ./build/compose/binaries/main-release/exe/compose-multiplatform-xlog-decode-1.0.0.exe ./build/compose/binaries/main-release/exe/compose-multiplatform-xlog-decode-windows-x64.exe

  - if: matrix.os == 'windows-latest'
    name: zip AppImage
    uses: thedoctor0/zip-release@0.7.6
    with:
      type: "zip"
      filename: "compose-multiplatform-xlog-decode-windows-x64.zip"
      directory: "./build/compose/binaries/main-release/app/compose-multiplatform-xlog-decode"

  - if: matrix.os == 'ubuntu-latest'
    name: rename File
    run: |
      mv /home/runner/work/compose-multiplatform-xlog-decode/compose-multiplatform-xlog-decode/build/compose/binaries/main-release/deb/compose-multiplatform-xlog-decode_1.0.0_amd64.deb      /home/runner/work/compose-multiplatform-xlog-decode/compose-multiplatform-xlog-decode/build/compose/binaries/main-release/deb/compose-multiplatform-xlog-decode-linux-amd64.deb
      mv /home/runner/work/compose-multiplatform-xlog-decode/compose-multiplatform-xlog-decode/build/compose/binaries/main-release/rpm/compose-multiplatform-xlog-decode-1.0.0-1.x86_64.rpm   /home/runner/work/compose-multiplatform-xlog-decode/compose-multiplatform-xlog-decode/build/compose/binaries/main-release/rpm/compose-multiplatform-xlog-decode-linux-x86_64.rpm

  - if: matrix.os == 'macos-13'
    name: rename File
    run: |
      mv /Users/runner/work/compose-multiplatform-xlog-decode/compose-multiplatform-xlog-decode/build/compose/binaries/main-release/dmg/compose-multiplatform-xlog-decode-1.0.0.dmg /Users/runner/work/compose-multiplatform-xlog-decode/compose-multiplatform-xlog-decode/build/compose/binaries/main-release/dmg/compose-multiplatform-xlog-decode-mac-x64.dmg

  - if: matrix.os == 'macos-14'
    name: rename File
    run: |
      mv /Users/runner/work/compose-multiplatform-xlog-decode/compose-multiplatform-xlog-decode/build/compose/binaries/main-release/dmg/compose-multiplatform-xlog-decode-1.0.0.dmg /Users/runner/work/compose-multiplatform-xlog-decode/compose-multiplatform-xlog-decode/build/compose/binaries/main-release/dmg/compose-multiplatform-xlog-decode-mac-arm64.dmg

  - name: create a release
    uses: ncipollo/release-action@v1
    with:
      artifacts: "**/compose-multiplatform-xlog-decode-windows-x64.exe,**/compose-multiplatform-xlog-decode-windows-x64.zip,**/*.deb,**/*.rpm,**/*.dmg"
      body: "create by workflows"
      allowUpdates: true
      artifactErrorsFailBuild: false
      generateReleaseNotes: false
      tag: ${{needs.current-time.outputs.currentTime}}
      name: ${{needs.current-time.outputs.currentTime}}
      token: ${{secrets.ACTION_TOKEN}}
```

相应的代码我都已经开源到了 Github，有兴趣的同学可以去下载试试：[compose-multiplatform-xlog-decode](https://github.com/leavesCZY/compose-multiplatform-xlog-decode) 

另外，如果只想要解析逻辑不需要 GUI 的话，也可以只引用项目中的 core 模块，当中就只包含解析逻辑
