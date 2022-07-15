> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/ff3f47f4b8724e6fbaeb0dc07706707b~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助

“受益”于目前 Android 手机各类屏幕尺寸长短不定、宽高比例大小不一的现状，**屏幕适配** 依然是 Android 应用开发时绕不开的问题

我们在日常开发中使用得最多的尺寸单位应该是 dp 了，Google 也推荐开发者尽量使用 dp 作为单位值，因为系统会根据屏幕的实际情况来完成 dp 和 px 之间的对应换算，使得同个 dp 值的显示效果在不同手机屏幕上不会相差太大。但直接使用 dp 值后的最终显示效果只能说不会和设计稿相差太远，想要做到完美适配还远远不够

举个例子。我们在进行 UI 开发时，一般都是按照设计稿的规定来给 View 设定宽高和间距，假设设计稿是按照 1080 x 1920 px，420 dpi 的屏幕标准来进行设计的，也即设计稿的宽高是 411 x 731 dp，那对于一个希望占据屏幕一半宽度的 ImageView 来说，在设计稿中的宽即 205.5 dp

那么，对于一台 1440 x 2880 px，560 dpi 的真机，其宽高是 411 x 822 dp，此时我们在布局文件中就可以直接使用设计稿中给出来的宽度值 205.5 dp，使得 ImageView 在这台真机上就占据了屏幕的一半宽度。虽然设计稿和这台真机的屏幕像素尺寸并不相同，但由于屏幕像素密度的存在，使得两者的 dp 宽度是一样的，从而让开发者可以直接套用设计稿给出的 dp 值就能满足要求

既然有了 dp，那我们为什么还需要进行屏幕适配呢？当然也是因为 dp 只能适用于大部分正常情况了。以上情况之所以能够实现完美适配，那也是因为举的例子刚好也是完美的：1440  / 1080 = 560 / 420 = 1.3333，两者的 px 宽度比例和像素密度比例刚好相等，所以此时使用 dp 才能刚好适用

再来看一个不怎么完美的例子，以另外两台真机为例：

- 华为 nova5：1080 x 2259 px，480 dpi，屏幕宽度为 1080 / (480 / 160) = 360 dp
- 三星 Galaxy S10：1080 x 2137 px，420 dpi，屏幕宽度为 1080 / (420 / 160) = 411 dp

可以看到，在像素宽度相同的情况下，不同手机的像素密度是有可能不一样的。手机厂家有可能是根据屏幕像素和屏幕尺寸来共同决定该值的大小，但不管怎样，这就造成了应用的实际效果与设计稿之间无法对应的情况：对于一个 180 dp 宽度的 View 来说，在华为 nova5 上能占据一半的屏幕宽度，但在三星 Galaxy S10 上却只有 180 / 411 = 0.43，这就造成了一定偏差

以上就是之所以需要进行屏幕适配的原因了

# View 体系的适配方案

在去年，我就已经写了一篇文章，详细地介绍了 Android 开发中目前处于主流或曾经主流的屏幕适配方案：[一文读懂 Android 主流屏幕适配方案](https://juejin.cn/post/6999445137491230728)，一共介绍了三种适配方案：

- 今日头条方案
- 宽高限定符方案
- smallestWidth 方案

三种方案的基本适配原理：

- 今日头条方案。基于系统将 dp 转换为 px 的公式 `px = dp * density` 来实现适配，通过在运行时动态修改 density 值的大小，使得修改后计算出的屏幕宽度就等于设计稿的宽度，从而使得在不同屏幕尺寸下我们都可以直接使用设计稿给出的 dp 值，且无需准备多套 dimens 文件
- 宽高限定符方案。通过穷举市面上所有 Android 手机的屏幕像素尺寸来实现适配，通过比例换算来为不同分辨率的屏幕分别准备一套 dimens 文件，应用在运行时再去引用和当前设备 **完全匹配** 的 dimens 文件，以此来实现屏幕适配
- smallestWidth 方案。适配原理和宽高限定符方案一样，也是通过比例换算来为不同尺寸的屏幕分别准备一套 dimens 文件，应用在运行时再去引用和当前设备 **最匹配** 的 dimens 文件，以此来实现屏幕适配

目前，除了 **宽高限定符方案** 性价比太低已经很少使用外，其它两种依然是当前的主流解决方案，各有优缺点：

- 今日头条方案。优点：可以直接使用设计稿中的 dp 值，无需准备多套 dimens 文件进行映射，因此不会增大 apk 体积，且在三种方案中 UI 还原度最高，其它两种方案都需要精准命中屏幕尺寸后才能达到此方案的还原度。缺点：由于此方案会影响到应用全局，如果我们引入了一些第三方库的话，三方库中的界面也会随之被影响到，可能会造成效果变形，此时就需要进行额外处理了
- smallestWidth 方案。优点：容错率高，在 320 ~ 460 dp 之间每 10 dp 就提供一套 dimens 文件就足够使用了，想要囊括更多设备的话也可以再缩短步长，基本不用担心最终效果会与设计稿偏差太多，且不会影响到三方库。缺点：需要生成多套 dimens 文件，增大了 apk 体积

# Compose 默认的适配机制

再来看下 Jetpack Compose 默认是如何进行屏幕适配的

在 View 体系下，不管我们在布局文件中使用的是什么尺寸单位，最终系统在使用时都需要将其转换为 px，对应 TypedValue 的 `applyDimension` 方法。例如，dp 值最终都需要通过 `px = dp * density` 公式来完成换算，这个规则对于 Jetpack Compose 也一样适用

```java
public static float applyDimension(int unit, float value, DisplayMetrics metrics) {
    switch (unit) {
        case COMPLEX_UNIT_PX:
            return value;
        case COMPLEX_UNIT_DIP:
            return value * metrics.density;
        case COMPLEX_UNIT_SP:
            return value * metrics.scaledDensity;
        case COMPLEX_UNIT_PT:
            return value * metrics.xdpi * (1.0f/72);
        case COMPLEX_UNIT_IN:
            return value * metrics.xdpi;
        case COMPLEX_UNIT_MM:
            return value * metrics.xdpi * (1.0f/25.4f);
    }
    return 0;
}
```

目前，我们在 Jetpack Compose 中只能采用 dp 单位来设定尺寸大小，对应 Dp 类

以 Modifier 的扩展函数 `Modifier.size(width: Dp, height: Dp)` 为例，其宽高大小均是 Dp 类型，在生成尺寸约束 Constraints 时，也是将 dp 转换为 px 后使用，对应 `Dp.roundToPx()` 方法

![](https://upload-images.jianshu.io/upload_images/2552605-31661b7636d1e81a.png)

继续跟踪 `Dp.roundToPx()` 方法，可以看到 dp 和 px 之间的转换方式和原生 View 体系一样，也是按照 `px = dp * density` 的公式来进行转换的，density 值由 Density 接口来定义和提供

![](https://upload-images.jianshu.io/upload_images/2552605-e3c9abfbf80de6bf.png)

此外，Density 接口还定义了 `fontScale` 变量，这就很容易让人联想到 sp 单位了。实际上，Density 接口的确提供了多个方法，用来实现 dp、px、sp 等单位之间的互相转换

![](https://upload-images.jianshu.io/upload_images/2552605-d08a3c9b9e53095c.png)

通过一步步查找 Density 接口的实现类，最终可以定位到 `AndroidDensity.android` 类，Jetpack Compose 就是在这里通过 Context 来获取对应的 `density` 和 `fontScale`

![](https://upload-images.jianshu.io/upload_images/2552605-8f3515d4627ff8b9.png)

此外，看过一些 Jetpack Compose 内部源码的同学应该知道，连接 Compose 和 View 体系之间的桥梁是 AndroidComposeView 类，而 AndroidComposeView 就通过 `fun Density(context: Context)` 方法来初始化其内部声明的 density 对象，CompositionLocals 类的 `ProvideCommonCompositionLocals` 方法又通过 LocalDensity 来将 AndroidComposeView 持有的 density 对象暴露给外部，从而使得框架内部和开发者均可以通过 `LocalDensity.current` 来获取到当前的 Density 对象，也即通过此方法拿到了 Android 系统的 `density` 和 `fontScale` 两个参数

![](https://upload-images.jianshu.io/upload_images/2552605-3416a5117f3af600.png)

根据以上线索，我们可以推断出 Jetpack Compose 目前采用的屏幕适配机制其实就和 Android 原生的 View 体系一样，都是以 **屏幕像素密度** 作为适配基础，这使得 Jetpack Compose 一样存在文章开头介绍的问题，在不同手机屏幕上的显示效果相比设计稿都会有一点点误差

> 说个题外话。在我看来，Jetpack Compose 设计 Density 接口的初衷就是为了实现多平台。如果 Jetpack Compose 仅仅是用于 Android 平台的话，直接获取当前设备的 density 值完成单位转换即可，Density 接口的存在意义并不大；而为了方便 Compose Multiplatform 实现跨平台，Google 官方才设计了 Density 接口。例如，Compose Multiplatform 支持在 Android 和 Windows 平台之间复用同一套 Compose UI，而相同的 dp 值在电脑屏幕上必须显示得更加大才行，通过抽象出 Density 接口，Compose Multiplatform 就可以为 Windows 平台提供更加合适的 density 值，从而使得显示效果更加适合电脑屏幕

# Compose 实现完美适配

所以说，在默认情况下，View 体系和 Jetpack Compose 都会存在和设计稿稍有偏差的问题。而上文介绍的几种 View 体系适配方案，单纯从实现效果上来说的话，今日头条方案应该是属于最优解了，但隐性成本相对也是最高的：由于修改了系统的 density 值，因此会影响到应用全局，对于已经迭代了很久的项目来说，中途引入此方案大概率会影响到现有的适配方案；即使是新项目，又需要考虑到此方案对于三方库的影响，不能由于主项目的变动导致三方库自身界面变形

可以看出来，如果想要采用今日头条方案的话，前提就在于：**是否能够细粒度地控制方案的作用范围，将生效范围控制在特定 Activity、特定 Fragment、甚至特定 View 内部**

想要满足此前提，对于 View 体系来说是很麻烦的，但对于 Jetpack Compose 来说，今日头条方案则真正成为了最优解：UI 还原度最高、无需生成多套 dimens 文件、作用范围自由可控、我甚至想不到会有什么缺点

看一个小例子

假设设计稿的屏幕宽度是 360 dp，内部有一个 180 dp 的控件，UI 设计师希望在不同屏幕上该控件均能占据屏幕的一半宽度

以下代码中嵌套了四个 Spacer 控件，每个 Spacer 的宽度均是 180 dp，但第二个 `Greeting()` 函数我将其放到了修改过后的 LocalDensity 中，按照今日头条方案给出的公式计算出此设备的目标 density 值，将其作为新的 LocalDensity

```kotlin
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val displayMetrics = LocalContext.current.resources.displayMetrics
                        val fontScale = LocalDensity.current.fontScale
                        val density = displayMetrics.density
                        val widthPixels = displayMetrics.widthPixels
                        val widthDp = widthPixels / density
                        val display =
                            "density: $density\nwidthPixels: $widthPixels\nwidthDp: $widthDp"
                        Text(text = display)
                        Greeting()
                        CompositionLocalProvider(
                            LocalDensity provides Density(
                                density = widthPixels / 360.0f,
                                fontScale = fontScale
                            )
                        ) {
                            Greeting()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(
            modifier = Modifier
                .size(
                    width = 180.dp,
                    height = 100.dp
                )
                .background(color = Color.Green)
                .align(alignment = Alignment.Start)
        )
        Spacer(
            modifier = Modifier
                .size(
                    width = 180.dp,
                    height = 100.dp
                )
                .background(color = Color.Cyan)
                .align(alignment = Alignment.End)
        )
    }
}
```

在三台不同分辨率的模拟器上运行代码，查看显示效果

![](https://upload-images.jianshu.io/upload_images/2552605-c8803a18860c7637.png)

很明显就可以看出来，三台模拟器的屏幕宽度并非刚好就是 360 dp，因此前两个 Spacer 控件并没有达到预期效果；而第二个 `Greeting()` 函数仅仅是多嵌套在了一个 CompositionLocalProvider 中而已，直接套用设计稿给出的尺寸就让两个 Spacer 控件在不同屏幕上均占据了屏幕的一半宽度，完美达到了设计稿的要求

由于我们可以在单独一个 Activity 中使用 Jetpack Compose，甚至可以在某个组合函数中局部修改 LocalDensity 值，因此我们可以细粒度地控制今日头条方案的生效范围，使其只在局部生效而不用担心会影响到其它业务模块，真正做到了完美适配且引入成本极低

此外，我们主动修改 LocalDensity 还有一个好处：可以自由控制应用内文字的显示大小。在默认情况下，采用了 sp 作为文字单位的应用，文字的显示大小是会随系统字体大小的变化而变化的，这是因为 sp 转换为 px 的公式 `px = sp * fontScale * density` 多了一个参数 fontScale，fontScale 代表的就是当前系统字体的缩放比例，当我们调大系统字体后，fontScale 会随之变大，连锁导致计算出的 px 值也随之变大。因此，通过主动修改 LocalDensity，我们还可以很方便地设置 Jetpack Compose 的字体大小，进一步完善应用最终的显示效果

# 结尾

得益于 Jetpack Compose 预留了 LocalDensity 这个入口，使得今日头条方案在 Jetpack Compose 中显得十分简单易用，读者也可以将这部分代码抽取到 MaterialTheme 中，使其默认生效，进一步减少代码量

```kotlin
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }
    val fontScale = LocalDensity.current.fontScale
    val displayMetrics = LocalContext.current.resources.displayMetrics
    val widthPixels = displayMetrics.widthPixels
    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = {
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = widthPixels / 360.0f,
                    fontScale = fontScale
                )
            ) {
                content()
            }
        }
    )
}
```

此外，smallestWidth 方案也一样适用于 Jetpack Compose，`androidx.compose.ui:ui:xxx` 库中就提供了 `dimensionResource` 方法用于获取 dimension 值，此方法会将获取到的值转换为 Dp 类型。因此如果读者项目中原本已经使用了 smallestWidth 方案的话，在 Jetpack Compose 中也依然可以继续使用

如果读者对于今日头条方案和 smallestWidth 方案还不是很了解的话，参照 [一文读懂 Android 主流屏幕适配方案](https://juejin.cn/post/6999445137491230728) 这篇文章就可以了 ~