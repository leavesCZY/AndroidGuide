> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

我曾经分别在不同平台上实现了同一种自定义效果

<img src="https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/8562d4c1f32e4eeaab4471e0aa01a64c~tplv-k3u1fbpfcp-watermark.image" style="zoom:80%;" /><img src="https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/aa19f7beedf3483da85bf8948c7c043d~tplv-k3u1fbpfcp-watermark.image" style="zoom:80%;" />

分别是：

- Kotlin 语言实现的 View 版本：[一文读懂 View 的 Measure、Layout、Draw 流程](https://juejin.cn/post/6939540905581887502)
- Dart 语言实现的 Flutter 版本：[Flutter 实战：用贝塞尔曲线画一个波浪球](https://juejin.cn/post/7098329401707724814)

现如今 Jetpack Compose 也逐渐流行起来了，能实现自定义控件也是对一名应用开发者最基本的要求，本篇文章就再来介绍下如何用 Jetpack Compose 实现相同效果，最终的效果图：

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/7b6aaf38932043ea8cf54f2402b5e3fa~tplv-k3u1fbpfcp-zoom-1.image)

# 一、Canvas  &  DrawScope

在原生的 Android View 体系下，我们要实现一个自定义 View 所需要的基本步骤有：

- 继承 android.view.View，在子类的构造函数中通过 AttributeSet 拿到在 XML 文件中声明的各个属性值
- 重写 onMeasure 和 onSizeChanged 两个方法，拿到 View 的宽高信息
- 重写 onLayout 方法，确定子 View 的位置信息（如果是自定义 ViewGroup 的话）
- 重写 onDraw 方法，通过 Paint、Path 等向 Canvas 绘制图形，从而实现各种自定义效果
- 重写 onVisibilityChanged、onAttachedToWindow、onDetachedFromWindow 等方法，在适当的时候开启动画或者停止动画，避免资源浪费和内存泄漏（如果有使用到 Animator 的话）

整个流程的重点就是 `onDraw` 方法了，开发者在这里拿到 Canvas 对象，也即画布，然后通过各种 API 在画布上绘制图形。例如，`canvas.drawLine`就用于绘制直线，`canvas.drawPath` 就用于绘制路径

按我自己的理解，通过 Jetpack Compose 实现一个自定义控件所需要的基本步骤有：

- 通过 BoxWithConstraints 拿到父项的约束条件，即以此拿到控件允许占有的最大空间和最小空间，包括：minWidth、maxWidth、minHeight、maxHeight 等
- 通过 Canvas() 函数来调用 drawLine、drawPath 等 API，绘制自定义图形
- 对于一些 Jetpack Compose 目前还不支持的绘制功能，可以通过 drawIntoCanvas 方法拿到原生 Android 环境的 Canvas 和 Paint 对象，利用原生 Android 环境的 API 方法来实现部分绘制需求
- 将上述操作封装为可组合函数，以函数入参参数的形式向外暴露必要的绘制参数，该可组合函数即我们最终实现的自定义 View 了

可以看到，在 Jetpack Compose 体系架构下，实现自定义控件的步骤和原生方式相比有着挺大的差别。最终实现的控件对应的也是一个可组合函数，而非一个类。而且我们不用再多在意控件本身的可见性和生命周期了，因为 Jetpack  Compose 会负责以高效的方式创建和释放对象，即使我们使用到了 Animator，Jetpack Compose 也会在可组合函数的生命周期结束的时候就自动停止动画

Jetpack Compose 的主要思路也是通过 Canvas 对象来绘制各种图形，通过 `Canvas()` 函数来提供绘制能力。`Canvas()` 是一个可组合函数，通过 DrawScope 来暴露 `drawLine` 和 `drawPath` 等方法。DrawScope 是一个维护自身状态且限定了作用域的绘图环境

```kotlin
@Composable
fun Canvas(modifier: Modifier, onDraw: DrawScope.() -> Unit) =
    Spacer(modifier.drawBehind(onDraw))
```

例如，如果我们要绘制一条从屏幕左上角到右下角的直线时

```kotlin
@Preview(showBackground = true, widthDp = 160, heightDp = 160)
@Composable
fun DrawLine() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        drawLine(
            start = Offset(x = 0f, y = 0f),
            end = Offset(x = canvasWidth, y = canvasHeight),
            color = Color.Green,
            strokeWidth = 10f,
        )
    }
}
```

![](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/807b57741be54f49bf9a198e3d853d88~tplv-k3u1fbpfcp-watermark.image)

绘制一个带有渐变背景色的半圆

```kotlin
@Preview(showBackground = true, widthDp = 160, heightDp = 160)
@Composable
fun DrawPath() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val path = Path()
        path.addArc(oval = Rect(0f, 0f, canvasWidth, canvasHeight), 0f, 180f)
        drawPath(
            path = path,
            brush = Brush.linearGradient(colors = listOf(Color.Blue, Color.Cyan, Color.LightGray))
        )
    }
}
```

![](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/b01364bceb9b4393b7c52a5240240212~tplv-k3u1fbpfcp-watermark.image)

此外 DrawScope 也提供了变换坐标系的能力，比如常见的 translate、rotate、scale 等

将上述半圆进行旋转

```kotlin
@Preview(showBackground = true, widthDp = 160, heightDp = 160)
@Composable
fun DrawPath() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val path = Path()
        path.addArc(oval = Rect(0f, 0f, canvasWidth, canvasHeight), 0f, 180f)
        rotate(degrees = 90f, pivot = center) {
            drawPath(
                path = path,
                brush = Brush.linearGradient(colors = listOf(Color.Blue, Color.Cyan, Color.LightGray))
            )
        }
    }
}
```

![](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/d4b3010671e24230ac4942590a527455~tplv-k3u1fbpfcp-watermark.image)

# 二、drawText

目前 DrawScope 还没有提供类似于 `drawText` 的方法，即 Jetpack Compose 目前还不支持直接进行文本绘制，这方面的需求需要通过 Android 框架原生的 Canvas 对象来实现

`Canvas()` 通过 `drawIntoCanvas()` 函数来向外暴露原生的 Canvas 对象，并提供了 `asFrameworkPaint()` 函数用于将 Jetpack Compose 环境的 Paint 对象转换为原生环境的 Paint 对象，这样我们就可以使用原生 Canvas 环境的各种方法了

例如，可以在上述半圆的基础上结合原生的 Canvas 再绘制一段文本

```kotlin
@Preview(showBackground = true, widthDp = 160, heightDp = 160)
@Composable
fun DrawPath() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val path = Path()
        path.addArc(oval = Rect(0f, 0f, canvasWidth, canvasHeight), 0f, 180f)
        rotate(degrees = 90f, pivot = center) {
            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Blue,
                        Color.Cyan,
                        Color.LightGray
                    )
                )
            )
        }
        drawIntoCanvas {
            //将 Jetpack Compose 环境的 Paint 对象转换为原生的 Paint 对象
            val textPaint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                isDither = true
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = android.graphics.Paint.Align.CENTER
            }
            textPaint.color = android.graphics.Color.RED
            textPaint.textSize = 50f
            val fontMetrics = textPaint.fontMetrics
            val top = fontMetrics.top
            val bottom = fontMetrics.bottom
            val centerX = size.width / 2f
            val centerY = size.height / 2f - top / 2f - bottom / 2f
            //拿到原生的 Canvas 对象
            val nativeCanvas = it.nativeCanvas
            nativeCanvas.drawText(
                "学不动也要学",
                centerX, centerY, textPaint
            )
        }
    }
}
```

![](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/91406c7430064d47bfe986568eb15a96~tplv-k3u1fbpfcp-watermark.image)

# 三、drawWithContent

我们除了可以直接使用 `Canvas()` 函数来实现各种自定义 View 外，Jetpack Compose 还提供了 `drawWithContent` 函数用于扩展现有控件。`drawWithContent` 函数是 Modifier 的扩展函数，`drawWithContent` 函数上执行的各种绘制操作，都会同步给 Modifier 所在控件的 Canvas 上，以此对任意控件进行自定义绘制

`drawWithContent` 函数提供了 ContentDrawScope 对象用于支持外部进行自定义绘制，ContentDrawScope 是 DrawScope 的子接口，所以可以直接使用上述介绍的各种 draw 方法。

```kotlin
fun Modifier.drawWithContent(
    onDraw: ContentDrawScope.() -> Unit
): Modifier = this.then(
    DrawWithContentModifier(
        onDraw = onDraw,
        inspectorInfo = debugInspectorInfo {
            name = "drawWithContent"
            properties["onDraw"] = onDraw
        }
    )
)
```

例如，假设现在我们希望在任意控件的右上角绘制一个红色小圆点，那么就可以将函数声明为 Modifier 的扩展函数，在 `drawWithContent` 中获取到 Canvas 的宽高信息，即拿到 Modifier 所在控件的宽高信息，然后定位到控件的右上角绘制出一个红色圆点即可

```kotlin
fun Modifier.redPoint(pointSize: Dp): Modifier = drawWithContent {
    drawContent()
    drawIntoCanvas {
        val paint = Paint().apply {
            color = Color.Red
        }
        it.drawCircle(
            center = Offset(x = size.width, y = 0f),
            radius = (pointSize / 2).toPx(),
            paint = paint
        )
    }
}
```

之后，只要在任意控件的 Modifier 参数中调用上述扩展函数，就可以直接在该控件上绘制出一个红色圆点了

```kotlin
@Preview(showBackground = true, widthDp = 160, heightDp = 160)
@Composable
fun DrawWithContentSample() {
    Spacer(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = 30.dp)
            .background(color = Color.LightGray)
            .redPoint(pointSize = 12.dp)
    )
}
```

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/df131d47902948c0856b82902ca410a1~tplv-k3u1fbpfcp-watermark.image)

有个细节需要注意。`drawWithContent` 中的 `drawContent()` 代表的是声明在 redPoint 之后的绘制行为，如果我们不主动执行该方法，那么声明在 redPoint 之后的绘制行为都不会生效。而如果将 `drawContent()` 放到 `drawIntoCanvas` 之后执行的话，`drawContent()` 就会绘制在 redPoint 的上面。即我们可以通过控制 `drawContent()` 的执行顺序来控制 `drawIntoCanvas` 所绘制的 Z 轴层级

例如，先在 redPoint 之后声明背景色 background，如果 `drawIntoCanvas` 先执行的话就会导致红色圆点被 background 覆盖了一部分内容区域，就像以下效果图所示，如果 `drawContent` 先执行的话就不会被覆盖住

```kotlin
fun Modifier.redPoint(pointSize: Dp): Modifier = drawWithContent {
    drawIntoCanvas {
        val paint = Paint().apply {
            color = Color.Red
        }
        it.drawCircle(
            center = Offset(x = size.width, y = 0f),
            radius = (pointSize / 2).toPx(),
            paint = paint
        )
    }
    drawContent()
}

@Preview(showBackground = true, widthDp = 160, heightDp = 160)
@Composable
fun DrawWithContentSample() {
    Spacer(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = 30.dp)
            .redPoint(pointSize = 12.dp)
            .background(color = Color.LightGray)
    )
}
```

![](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/84996cf570d140da8e3f1c563d9a6dc9~tplv-k3u1fbpfcp-watermark.image)

# 四、WaveLoading

有了上述基础后，就可以来动手实现以下效果了

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/7b6aaf38932043ea8cf54f2402b5e3fa~tplv-k3u1fbpfcp-zoom-1.image)

先来总结下 WaveLoading 的特点，这样才能归纳出实现该效果所需要的步骤

1. 控件的主体是一个不规则的半圆，顶部以类似于波浪的形式从左往右上下波动运行
2. 波浪可以自定义颜色，此处以 waveColor 命名
3. 波浪的起伏线将嵌入的文本分为上下两种颜色，上边的文本颜色和 waveColor 保持一致，下边的文本颜色以 downTextColor 命名，文本的上下分割线一直在动态变化中

虽然波浪是不断运动的，但只要能够绘制出其中一帧的图形，其动态效果就能通过不断改变波浪的位置参数来完成，所以这里先把控件当成静态的，先实现其静态效果即可。将绘制步骤拆解为以下几步：

1. 绘制颜色为 waveColor 的文本，将其绘制在 canvas 的最底层。绘制文本的操作都需要交由 Android 原生的 Canvas 和 Path 来实现
2. 根据控件的宽高信息构建一个不超出范围的最大圆形路径 circlePath
3. 以 circlePath 的水平中间线作为波浪的起伏线，在起伏线的上边和下边分别利用贝塞尔曲线绘制一段连续的波浪 path，将 path 的首尾两端以矩形的形式连接在一起，构成 wavePath
4. 取 circlePath 和 wavePath 的交集 resultPath，绘制出 resultPath，用 waveColor 填充， 此时就得到了半圆形的球形波浪了
5. 通过 `clipPath(path = resultPath, clipOp = ClipOp.Intersect)` 方法裁切画布，再绘制颜色为 downTextColor 的文本，此时绘制的 downTextColor 文本只会显示 resultPath 范围内的部分，从而使得先后两次不同时间绘制的文本上下拼接在了一起，从而得到有不同颜色范围的文本
6. 利用 rememberInfiniteTransition 动画不断改变 wavePath 起始点的 X 坐标，从而得到波浪不断从左往右前进的效果

有了思路后代码就很简单了，总的也就才一百行左右，代码量相比 View 版本和 Flutter 版本都要少得多

```kotlin
@Composable
fun WaveLoading(
    modifier: Modifier,
    text: String,
    textSize: TextUnit,
    waveColor: Color,
    downTextColor: Color = Color.White,
    animationSpec: InfiniteRepeatableSpec<Float> = infiniteRepeatable(
        animation = tween(
            durationMillis = 500,
            easing = CubicBezierEasing(0.2f, 0.2f, 0.7f, 0.9f)
        ),
        repeatMode = RepeatMode.Restart
    )
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current.density
        val circleSizeDp = minOf(maxWidth, maxHeight)
        val circleSizePx = circleSizeDp.value * density
        val waveWidth = circleSizePx / 1.2f
        val waveHeight = circleSizePx / 10f
        val textPaint by remember {
            mutableStateOf(Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                isDither = true
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = android.graphics.Paint.Align.CENTER
            })
        }
        val wavePath by remember {
            mutableStateOf(Path())
        }
        val circlePath by remember {
            mutableStateOf(Path().apply {
                addArc(
                    Rect(0f, 0f, circleSizePx, circleSizePx),
                    0f, 360f
                )
            })
        }
        val animateValue by rememberInfiniteTransition().animateFloat(
            initialValue = 0f,
            targetValue = waveWidth,
            animationSpec = animationSpec,
        )
        Canvas(modifier = modifier.requiredSize(size = circleSizeDp)) {
            drawTextToCenter(
                text = text,
                textPaint = textPaint,
                textSize = textSize.toPx(),
                textColor = waveColor.toArgb()
            )
            wavePath.reset()
            wavePath.moveTo(-waveWidth + animateValue, circleSizePx / 2.3f)
            var i = -waveWidth
            while (i < circleSizePx + waveWidth) {
                wavePath.relativeQuadraticBezierTo(waveWidth / 4f, -waveHeight, waveWidth / 2f, 0f)
                wavePath.relativeQuadraticBezierTo(waveWidth / 4f, waveHeight, waveWidth / 2f, 0f)
                i += waveWidth
            }
            wavePath.lineTo(circleSizePx, circleSizePx)
            wavePath.lineTo(0f, circleSizePx)
            wavePath.close()
            val resultPath = Path.combine(
                path1 = circlePath,
                path2 = wavePath,
                operation = PathOperation.Intersect
            )
            drawPath(path = resultPath, color = waveColor)
            clipPath(path = resultPath, clipOp = ClipOp.Intersect) {
                drawTextToCenter(
                    text = text,
                    textPaint = textPaint,
                    textSize = textSize.toPx(),
                    textColor = downTextColor.toArgb()
                )
            }
        }
    }
}

private fun DrawScope.drawTextToCenter(
    text: String,
    textPaint: android.graphics.Paint,
    textSize: Float,
    textColor: Int
) {
    textPaint.textSize = textSize
    textPaint.color = textColor
    val fontMetrics = textPaint.fontMetrics
    val x = size.width / 2f
    val y = size.height / 2f - (fontMetrics.top + fontMetrics.bottom) / 2f
    drawContext.canvas.nativeCanvas.drawText(text, x, y, textPaint)
}
```

之后就像普通的可组合函数一样进行调用即可

```kotlin
Column(
    modifier = Modifier.verticalScroll(state = ScrollState(0)),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    WaveLoading(
        modifier = Modifier.size(size = 220.dp),
        text = "開",
        textSize = 150.sp,
        waveColor = Color(0xFF3949AB)
    )
    WaveLoading(
        modifier = Modifier.size(size = 220.dp),
        text = "心",
        textSize = 150.sp,
        waveColor = Color(0xFF00897B)
    )
}
```

# 五、结尾

最后当然也少不了 WaveLoading 的完整示例代码了，有需要的同学点击这里下载：[ComposeWaveLoading](https://github.com/leavesCZY/ComposeWaveLoading)