> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

在 2021 年的七月份，Google 发布了 Jetpack Compose 的 1.0 正式版本，这是一个适用于 Android 平台的声明式 UI 开发框架，可供开发者在生产环境中使用。到了十二月份，JetBrains 也随之发布了适用于多个平台的声明式 UI 开发框架 Compose Multiplatform 的 1.0 正式版本，意味着此时也适用于商业项目了

既然 Compose Multiplatform 发布了正式版，我也想着来做一次实践，之前我曾经用 Jetpack Compose 写了一个 **俄罗斯方块** 小游戏：[学不动也要学，Jetpack Compose 玩一把俄罗斯方块](https://juejin.cn/post/6974585048762679310)，由于整个应用的逻辑和 UI 都是完全用 Kotlin 和 Jetpack Compose 实现的，因此很简单地就通过 Compose Multiplatform 将其移植到了 Windows 平台：[compose_tetris](https://github.com/leavesCZY/compose_tetris)

整个移除过程十分顺利，移植成本不高，最终效果看着也不错，此文章就来简单讲讲 Compose Multiplatform 的一些小细节，希望对你有所帮助 ~

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/36daca5583924d9c93b3f0e8e9c4d76e~tplv-k3u1fbpfcp-zoom-1.image)

# Compose Multiplatform

Compose Multiplatform 可以看做是 Jetpack Compose 的超集，在 Jetpack Compose 的基础上扩展出了跨平台的能力，两者共享了大部分的核心公共 API，所以 Compose Multiplatform 的很多基础库均还是以 `androidx.compose.xxx` 作为包名，这使得已经通过 Jetpack Compose 实现的 Android 应用可以比较方便地移植到其它平台，两者具有完美的互操作性

Compose Multiplatform 支持市面上的大部分主流平台：

| Target platform                    | Target preset                                                |
| ---------------------------------- | ------------------------------------------------------------ |
| Kotlin/JVM                         | jvm                                                          |
| Kotlin/JS                          | js                                                           |
| Android applications and libraries | android                                                      |
| Android NDK                        | androidNativeArm32、androidNativeArm64、androidNativeX86、androidNativeX64 |
| iOS                                | iosArm32、iosArm64、iosX64、iosSimulatorArm64                |
| watchOS                            | watchosArm32、watchosArm64、watchosX86、watchosX64、watchosSimulatorArm64 |
| tvOS                               | tvosArm64、tvosX64、tvosSimulatorArm64                       |
| macOS                              | macosX64、macosArm64                                         |
| Linux                              | linuxArm64、linuxArm32Hfp、linuxMips32、linuxMipsel32、linuxX64 |
| Windows                            | mingwX64、mingwX86                                           |
| WebAssembly                        | wasm32                                                       |

Compose Multiplatform 通过用不同的编译器编译同一份代码来生成各端的不同产物，从而达到跨平台的目的，最终的编译产物和目标平台完全相容。例如，通过 Kotlin/JVM 为 JVM 和 Android 平台生成 jar/aar 文件、通过 Kotlin/Native 为 IOS 平台生成 framework 文件、通过 Kotlin/JS 为 Web 平台生成 JavaScript 文件，最终调用的还是原生 API，这使得采用 Compose Multiplatform 不会导致性能损耗，且不会像 Flutter 那样明显增大应用体积

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/1e2018c2cf6444ee93e406d2ef865a9a~tplv-k3u1fbpfcp-zoom-1.image)

Compose Multiplatform 在移动端的跨平台框架子集叫做 Kotlin Multiplatform Mobile（KMM），对于移动端的开发人员来说，KMM 的正式发布也意味着多了一种实现 Android 和 IOS 跨平台开发的方案。和 Flutter 不同，KMM 并不追求在各个平台使用一套完全相同的 UI 和 代码，没有像 Flutter 那样内置一个统一的图型绘制引擎，因此 KMM 虽然支持多端复用 UI，但功能还比较弱，在构建 UI 时还是需要依赖于平台 API。KMM 侧重于在 UI 层以下共享一套适用于所有平台的通用业务逻辑，统一通过 Kotlin 来编写业务代码，并同时保持和原生开发语言（Java、Objective-C、Swift、JavaScript 等）之间的互通性，具备灵活性的同时也保留了原生编程的优势，仅在需要的时候才来编写平台特定代码，例如实现原生 UI，调用平台特定 API 等

# 项目结构

对于在不同平台上实现的同个应用来说，其业务逻辑往往是高度相似的，在没有实施跨平台方案之前，业务逻辑和 UI 交互均需要各端各自进行实现和维护，开发成本和开发复杂度随着应用更迭逐步加大

在采用了 Compose Multiplatform 实现跨平台开发后，就可以将各端的业务逻辑统一抽取为一个公用的 `commonMain` 模块，各个平台再通过依赖该模块来实现代码复用。此外，对于相似平台，例如 Linux、Mingw、Mac 等桌面平台，其 **业务逻辑、布局结构、交互逻辑** 等会更加相似，此时就可以通过分层结构从 `commonMain` 模块再扩展出一个专属于桌面平台的公用模块 `desktopMain`，进一步重用代码。类似地，如果面向的是移动端，也可以再抽取出一个 `appMain` 模块作为 Android 和 IOS 的公用模块

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/be343b98de2d48c4850bbb4344e9fce7~tplv-k3u1fbpfcp-zoom-1.image)

通过 IDEA 的 Kotlin Multiplatform Mobile 插件生成的模板项目，不同平台的代码已经分属于不同的模块了，同时包含一个 `common` 模块用于存放公用的业务逻辑，通过划分模块来实现代码隔离

以 compose_tetris 为例，其目标平台是 Android 和 Desktop，所以包含三个主模块：`common、android、desktop`。`common`又划分为三个子模块：`commonMain、androidMain、desktopMain`。`commonMain` 包含的是同时适用于 Android 和 Desktop 的业务逻辑，由于其并不隶属于特定平台，因此也不具备直接调用平台 API 的能力，而是交由 `androidMain` 和 `desktopMain` 来实现特定的平台能力

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/c0403b4cbc8f4b798e3d756515a414bc~tplv-k3u1fbpfcp-zoom-1.image)

- commonMain。用于存放适用于任何平台的业务逻辑、通用布局、expect 声明等
- androidMain。专属于 Android 平台的模块，在 commonMain 的基础上根据 Android 平台的特性进行扩展，例如，将引用的业务逻辑注入到 ViewModel 中交由 android 模块进行引用、实现 expect 声明、引用原生的 `android.graphics.Canvas` 以便绘制  UI
- desktopMain。专属于 Desktop 平台的模块，在 commonMain 的基础上根据 Desktop 平台的特性进行扩展，例如，将引用的布局结构扩展为大屏模式、增加监听键盘点击事件的功能、实现 expect 声明、引用支持硬件加速的 `org.jetbrains.skia.Canvas` 以便绘制 UI
- android。最终的平台模块
- desktop。最终的平台模块

最终整个项目的实际依赖关系如下所示

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/da8cd308ef3c44329e224035ee9da51c~tplv-k3u1fbpfcp-zoom-1.image)

# sourceSets

按道理来说，虽然 `android` 和 `desktop` 都引用了 `common` 模块，但 `android` 和 `desktopMain` 之间应该是相互隔离的，`desktop` 和 `androidMain` 之间也一样。此外，虽然 `androidMain` 和 `desktopMain` 同属于 `common` 模块，但两者之间也是不能相互引用的

Compose Multiplatform 通过 `sourceSets` 来实现上述目的，`sourceSets` 就用于声明项目结构和模块间的依赖关系

以 compose_tetris 为例，在 `common` 模块的 `build.gradle.kts` 文件中，通过 `sourceSets` 将自身划分为了三个子模块，再通过 NamedDomainObjectContainer 将声明的 `named` 指向要划分的子模块，各个子模块又在内部声明各自需要的依赖。Kotlin 标准库会自动引入到每个模块内，不需要特意声明

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/5570dac717aa4ceca0b8d04ec61d9e5f~tplv-k3u1fbpfcp-zoom-1.image)

注意，此时 `android` 模块虽然通过 `implementation(project(":common"))` 引用了整个 `common` 模块，但实际上能引用的只是 `commonMain` 和 `androidMain` 两个子模块，无法引用到 `desktopMain` 中的代码，`desktop` 模块同理

> Compose Multiplatform 具体是如何实现 **差异化引用** 的我还不太清楚，猜测应该是在 plugin 中实现的，因为三个子模块的名字是固定的，属于预定义的配置，修改模块名将导致编译失败，所以应该是在 plugin 中通过模块名来匹配允许引用的子模块

# 引用依赖

compose_tetris 整个项目的依赖类型根据平台可以分为三种：common、android、desktop。顾名思义，common 也即适用于任意平台的通用依赖，当中不能调用任何平台 API，也不能引用任何只适用于单独平台的依赖库，所以在 Android 开发中比较流行的 OkHttp、Gson、RxJava 等库都不能在 `commonMain` 模块中使用

而为了能够在`commonMain` 模块中构建适用于任意平台的 UI，`commonMain` 模块所引用的 common 依赖其实都是 `org.jetbrains.compose.xxx` 而非 `androidx.compose.xxx`，jetbrainsCompose 可以看做是对 androidxCompose 进行了一层包装，复刻了 androidxCompose 的大部分依赖库，保持 **包名、类签名、方法签名** 不变

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/2de06a23e3f14d9f8e0b4530718833dc~tplv-k3u1fbpfcp-zoom-1.image)

从项目的依赖列表中可以看到，引用的 jetbrainsCompose 依赖会分为 **desktop 版本** 和 **非 desktop 版本** 两种

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/3f3a937aa7a242fba0d2b75edc006f9a~tplv-k3u1fbpfcp-zoom-1.image)

`commonMain` 模块所引用的均是 **非 desktop 版本**，不会调用任何平台 API，也即不会和 Android 平台以及其它任意平台关联，使得`commonMain` 模块可以保证平台无关性。等到编译阶段，会自动为不同平台提供签名信息相同但具体实现不同的底层依赖，如果目标平台是 Android，最终引用的会是 androidxCompose；如果目标平台是 Desktop，则引用的是 jetbrainsCompose-desktop

Kotlin 官方也提供了一些适用于多平台的基础库：

- Json 解析：[kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)
- Http 请求：[ktor](https://github.com/ktorio/ktor)
- SQLite 操作：[sqldelight](https://github.com/cashapp/sqldelight)

更多多平台基础库看这里：[Kotlin-Multiplatform-Libraries](https://github.com/AAkira/Kotlin-Multiplatform-Libraries)

# 调用平台 API

调用特定平台 API 是跨平台开发中绕不过的一个点。假设有一个获取手机型号的需求，获取手机型号的整体处理流程可以是属于可以共用的业务逻辑，但实际获取手机型号的过程就需要在不同平台上进行差异化实现了，Android 平台需要通过调用 `android.os.Build.MODEL` 来获取，IOS 平台需要通过 `UIDevice.current.model` 来获取

Compose Multiplatform 通过 `expect / actual` 机制来提供调用特定平台 API 的能力。在该机制下，`commonMain` 模块通过 `expect` 关键字来声明需要交由特定平台实现的 **函数、类、接口** 等多种 Kotlin 语法结构，`androidMain` 和 `desktopMain` 模块再通过 `actual` 关键字在自身平台上进行实现

以 compose_tetris 为例，在游戏过程中需要播放音效，Android 和 Desktop 均需要提供 Play Sound 的能力。Android 平台上我使用的是系统提供的 `android.media.AudioManager`，这自然不能在 Desktop 平台上使用，此时就需要来差异化实现 Play Sound 的功能了

在 `commonMain` 模块中，我将播放音效的能力抽象为 SoundPlayer 接口，再通过 `expect` 关键字修饰的 `getSoundPlayer()`方法来获取 SoundPlayer 对象，此方法相当于一个抽象方法，在 `commonMain` 模块中不需要具体实现，但可以正常调用。之后，`androidMain` 和 `desktopMain` 两个模块在相同包名下再各自实现`getSoundPlayer()`方法，通过 `actual` 关键字来修饰实现方法，当编译平台代码时，“方法声明”和“具体实现”就会被桥接在一起，从而达到平台差异化实现的目的

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/d5315c3cd4aa407480ede982b966d1d9~tplv-k3u1fbpfcp-zoom-1.image)

类似地，Compose Multiplatform 也是通过`expect / actual` 机制来实现跨平台绘制 UI

Jetpack Compose 除了在界面渲染时需要依赖于 Android 原生 API 外，其它大部分特性都可以做到平台无关。Jetpack Compose 在绘制文本的时候就需要通过调用 `android.graphics.Canvas` 的 `drawText`方法来实现，因此在 `androidx.compose.ui:ui-graphics:1.0.1` 中就包含一个 `nativeCanvas` 扩展属性用于获取原生平台的 Canvas 对象，也即 Android 平台的 `android.graphics.Canvas`。nativeCanvas 属性也是采用了`expect / actual` 机制，其 expect 声明就放在同个库的 Canvas 类内部

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/69954c53bac74e40bd9c40adebbf9515~tplv-k3u1fbpfcp-zoom-1.image)

而在 `org.jetbrains.compose.ui:ui-graphics-desktop:1.0.1`中，`nativeCanvas` 属性对应的是 Skiko 库中的 `org.jetbrains.skia.Canvas`

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/100c6fa2fe9642a7bf0aecc93a7d5886~tplv-k3u1fbpfcp-zoom-1.image)

Skia 是一个优秀的开源 2D 图形库，支持硬件加速，提供了适用于各种硬件和软件平台的通用 API，是 Chrome、Chrome OS、Flutter 等应用和框架所采用的图形引擎，JetBrains 通过 Kotlin 语言对 Skia 做了一层封装，也即 Skiko（Skia for Kotlin），使得在各个平台均可以通过统一的 Kotlin API 来调用 Skia 进行图形绘制

所以说，Compose Multiplatform 内部会通过为不同平台引入不同依赖来实现跨平台。在 `commonMain` 模块中我们引入的是 `org.jetbrains.compose.ui:ui-graphics:1.0.1`，该依赖库在保持和 androidx-graphics 签名信息相同的前提下剔除了和 Android 相关联的代码，只保留 expect，没有 actual，向外部暴露公共 API 并保证其平台无关性。当目标平台是 Desktop 时，其最终依赖则变成了 jetbrains-graphics-desktop，当中就实现了 jetbrains-graphics 中声明的所有 expect，通过 Skiko 来桥接需要实现的 Cnavas 对象。当目标平台是 Android 时，则还是会继续依赖 androidx-graphics，通过 `android.graphics.Canvas` 来桥接需要实现的 Cnavas 对象

此外，从 `androidx.compose.ui:ui-graphics:1.0.1` 可以看出来 Jetpack Compose 本身也是在为 Jetbrains Compose 预留着实现跨平台开发的能力入口，毕竟 Jetpack Compose 如果只是单单用于 Android 平台的话，在声明 `nativeCanvas` 属性的时候就没有必要采用 `expect / actual` 机制了

# 复用 UI

从文章开头给出的效果图可以看出来，compose_tetris 在 Android 和 Desktop 两端的布局结构是基本一致的，一些小的区别点有：

Android：

- 整个布局的宽高尺寸即 Android 手机屏幕的大小，即全屏显示
- 整个布局是承载在 Activity 中的，当 Activity 处于非前台状态时（onPause）需要自动暂停游戏
- 需要设置系统状态栏的背景色和图标颜色，保持色调一致，同时预留出足够的 SystemBarsPadding，避免被系统状态栏遮挡住布局

Desktop：

- 整个布局设定的尺寸是 1200 x 900 px，但不能超出电脑屏幕宽高大小的五分之四
- 需要监听键盘的点击事件，为游戏增加通过点击键盘控制方块 **左移、右移、旋转** 的功能

除了这些区别点外，游戏的布局逻辑和内部的界面渲染逻辑都是可以复用的，对应 `commonMain` 模块中的 MainScreen，各个平台再通过嵌套 MainScreen 的方式来进行自定义实现

```kotlin
@Composable
fun MainScreen(modifier: Modifier, tetrisLogic: ITetrisLogic) {
    val tetrisState by tetrisLogic.tetrisStateFlow.collectAsState()
    ···
    ComposeTetrisTheme {
        Scaffold(
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.background)
                .then(other = modifier)
        ) {
            TetrisBody(tetrisScreen = {
                TetrisScreen(tetrisState = tetrisState)
            }, tetrisButton = {
                TetrisButton(tetrisState = tetrisState, playListener = playListener)
            })
        }
    }
}
```

`androidMain` 模块的 AndroidMainScreen 通过 accompanist 来控制状态栏，再通过 LifecycleObserver 来判断 Activity 是否处于前台 

```kotlin
@Composable
fun AndroidMainScreen() {
    val systemUiController = rememberSystemUiController()
    systemUiController.setStatusBarColor(
        color = Color.Transparent,
        darkIcons = true
    )

    ···

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(key1 = Unit) {
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                dispatchAction(action = Action.Resume)
            }

            override fun onPause(owner: LifecycleOwner) {
                dispatchAction(action = Action.Background)
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
    ProvideWindowInsets {
        MainScreen(
            modifier = Modifier.systemBarsPadding().padding(top = 10.dp),
            tetrisLogic = tetrisViewModel
        )
    }
}
```

`desktopMain` 模块的 DesktopMainScreen 则通过 onKeyEvent 回调来监听键盘的点击事件

```kotlin
@Composable
fun DesktopMainScreen() {
    val tetrisViewModel by remember {
        mutableStateOf(DesktopTetrisViewModel(delegate = TetrisLogicImpl()))
    }
    val onKeyEvent: (KeyEvent) -> Boolean = remember {
        {
            when (it.key) {
                Key.DirectionLeft -> {
                    if (it.type == KeyEventType.KeyDown) {
                        tetrisViewModel.dispatch(action = Action.Transformation(TransformationType.Left))
                    }
                }
                Key.DirectionRight -> {
                    if (it.type == KeyEventType.KeyDown) {
                        tetrisViewModel.dispatch(action = Action.Transformation(TransformationType.Right))
                    }
                }
                Key.DirectionUp -> {
                    if (it.type == KeyEventType.KeyUp) {
                        tetrisViewModel.dispatch(action = Action.Transformation(TransformationType.Rotate))
                    }
                }
                Key.DirectionDown -> {
                    if (it.type == KeyEventType.KeyUp) {
                        tetrisViewModel.dispatch(action = Action.Transformation(TransformationType.Fall))
                    }
                }
            }
            true
        }
    }
    val focusRequester = remember {
        FocusRequester()
    }
    LaunchedEffect(key1 = Unit) {
        focusRequester.requestFocus()
    }
    MainScreen(
        modifier = Modifier.padding(top = 30.dp)
            .focusRequester(focusRequester = focusRequester)
            .focusable(enabled = true)
            .onKeyEvent(onKeyEvent = onKeyEvent),
        tetrisLogic = tetrisViewModel
    )
}
```

# 复用业务逻辑

实现跨端复用业务逻辑的思路可以参考一位开发者在 GitHub 上和 JetBrains 官方的讨论：[Useful reading about porting Android apps to Desktop](https://github.com/JetBrains/compose-jb/discussions/1587)

该开发者有一个已经在 Android 平台实现的应用，准备移植到 Windows 平台，在 Android 平台上该开发者通过 ViewModel 来承载具体的业务逻辑，其希望该逻辑能够在 Windows 平台上进行复用

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/c226a7ff154a421f9da5256911cc9b9e~tplv-k3u1fbpfcp-zoom-1.image)

看过 ViewModel 源码的同学应该知道，ViewModel 的内部实现是和 Android 平台强关联的，创建和存储 ViewModel 的逻辑都依赖于 ViewModelStoreOwner（对应 Activity 和 Fragment），想要让 JetBrains 提供可以无缝迁移的等比实现不太现实，因此 JetBrains 官方给出的建议是：将具体的业务逻辑从 ViewModel 中抽取出来并声明为独立的个体，然后再通过代理的方式注入到 ViewModel 中，在 Windows 平台也通过此方式来实现，以此来复用业务逻辑

以 compose_tetris 为例，俄罗斯方块的操作逻辑是完全适用于任何平台的，如果我们专注的仅是 Android 平台的话，这部分逻辑自然可以直接在 ViewModel 中实现。而为了实现跨端复用，此时就需要将该业务逻辑抽取为一个独立的个体了，对应 TetrisLogicImpl 这个纯 Kotlin 类，保存在 `commonMain` 模块中

```kotlin
/**
 * @Author: leavesCZY
 * @Github: https://github.com/leavesCZY
 * @Desc:
 */
interface ITetrisLogic {

    val tetrisStateFlow: StateFlow<TetrisState>

    fun provideScope(coroutineScope: CoroutineScope)

    fun dispatch(action: Action)

}

class TetrisLogicImpl : ITetrisLogic {

    private lateinit var coroutineScope: CoroutineScope

    private val _tetrisStateFlow = MutableStateFlow(TetrisState())

    override val tetrisStateFlow = _tetrisStateFlow.asStateFlow()

    override fun provideScope(coroutineScope: CoroutineScope) {
        this.coroutineScope = coroutineScope
    }

    override fun dispatch(action: Action) {
        //TODO
    }

}
```

在 Android 平台中，将 TetrisLogicImpl 注入到 ViewModel 中，此时 ViewModel 就单纯作为业务逻辑的承载体了，不包含具体的实现逻辑，通过代理的方式来间接实现 ITetrisLogic 接口，将所有需要触发的操作均转交给 TetrisLogicImpl 即可。AndroidTetrisViewModel 保存在 `androidMain` 模块中

```kotlin
class AndroidTetrisViewModel(delegate: TetrisLogicImpl) : ViewModel(), ITetrisLogic by delegate {

    init {
        provideScope(coroutineScope = viewModelScope)
    }

}
```

Desktop 平台也通过相同方式来实现，将 DesktopTetrisViewModel 保存在 `desktopMain` 模块中

```kotlin
class DesktopTetrisViewModel(delegate: TetrisLogicImpl) : ITetrisLogic by delegate {

    init {
        provideScope(GlobalScope)
    }

}
```

由于 Android 平台存在页面生命周期的概念，因此 AndroidTetrisViewModel 向 TetrisLogicImpl 注入的协程作用域是 viewModelScope；而在 Desktop 平台，当关闭游戏时整个进程也就结束了，因此注入的是 GlobalScope

# 结尾

详细的源码看这里：[compose_tetris](https://github.com/leavesCZY/compose_tetris)

从 Jetpack Compose 发布正式版那段时间起，我先后写了几篇关于 Jetpack Compose 的文章，可以帮助读者更快入门，希望对你有所帮助 ~

- [学不动也要学，Jetpack Compose 玩一把俄罗斯方块](https://juejin.cn/post/6974585048762679310)
- [学不动也要学，Jetpack Compose 写一个 IM APP（一）](https://juejin.cn/post/6991429231821684773)
- [学不动也要学，Jetpack Compose 写一个 IM APP（二）](https://juejin.cn/post/7028397244894330917)

# 参考资料

本文部分图片来自于 Kotlin 官网

- [Kotlin Multiplatform](https://kotlinlang.org/docs/mpp-intro.html)
- [compose-jb](https://github.com/JetBrains/compose-jb)