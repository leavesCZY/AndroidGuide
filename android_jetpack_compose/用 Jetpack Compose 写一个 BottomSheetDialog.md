> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

BottomSheetDialog 是 Android Material 库中提供的一个弹窗类，其特点就是会从屏幕底部弹出，支持拖拽回弹效果，以及拖拽关闭弹窗，在 Android 应用开发中广泛应用

Jetpack Compose 也提供了一个同样的弹窗效果，即 Compose Material 库中的 BottomSheetScaffold，其将整体页面分为了 `content`  和 `sheetContent` 两个区域，`content` 代表的是常驻状态的主屏幕布局，`sheetContent` 代表的是想从底部弹出的布局

```kotlin
import androidx.compose.material.BottomSheetScaffold

@Composable
private fun BottomSheetScaffoldDemo() {
    BottomSheetScaffold(sheetContent = {

    }, content = {

    })
}
```

BottomSheetScaffold 完全足以拿来实现 BottomSheetDialog 的效果了，但目前 Google 已经推出了 Material 设计的最新版本，也即 Compose Material 3，而 Material 3 目前并没有提供 BottomSheetScaffold，因此在只想要使用 Material 3 的情况下，我只能自己来实现一个 Compose 版本的 BottomSheetDialog 了

最终的效果如下所示

![](https://upload-images.jianshu.io/upload_images/2552605-359c6875d2122c15.gif)

此 Compose 版本的 BottomSheetDialog 和原生的 Dialog 一样，也支持 `cancelable`、`canceledOnTouchOutside` 两个属性，用于控制：是否允许通过点击返回键关闭弹窗、是否允许拖拽关闭弹窗、是否允许通过点击弹窗外部区域来关闭弹窗。此外，此弹窗也无需强制嵌套在某个布局以内，相对 BottomSheetScaffold 来说使用上会更加灵活

来讲下具体的实现思路

先定义好所有需要的参数。`visible` 属性用于控制弹窗当前是否可见，根据声明式 UI 的特点，该属性就需要交由外部来维护，BottomSheetDialog 再通过 `onDismissRequest` 方法将关闭弹窗的请求交由外部处理。`content` 代表的就是弹窗的具体布局

```kotlin
@Composable
fun BottomSheetDialog(
    modifier: Modifier = Modifier,
    visible: Boolean,
    cancelable: Boolean = true,
    canceledOnTouchOutside: Boolean = true,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
)
```

BottomSheetDialog 通过 BackHandler 来拦截点击返回键的事件，BackHandler 内部是通过原生的 OnBackPressedDispatcher 来实现的，这里设置其只在弹窗可见时才进行拦截，在 `cancelable` 为 true 时才将拦截的事件转交由外部处理

```kotlin
BackHandler(enabled = visible, onBack = {
    if (cancelable) {
        onDismissRequest()
    }
})
```

之后需要为弹窗设置一个淡入淡出的半透明背景色，通过 AnimatedVisibility 来实现即可。再通过 `clickableNoRipple` 拦截页面整体的点击事件，在 `canceledOnTouchOutside` 为 true 时才将拦截的事件转交由外部处理

```kotlin
AnimatedVisibility(
    visible = visible,
    enter = fadeIn(animationSpec = tween(durationMillis = 400, easing = LinearEasing)),
    exit = fadeOut(animationSpec = tween(durationMillis = 400, easing = LinearEasing))
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0x99000000))
            .clickableNoRipple {
                if (canceledOnTouchOutside) {
                    onDismissRequest()
                }
            }
    )
}
```

由于 Compose 提供的 `clickable` 方法默认会带上水波纹的效果，点击弹窗背景时并不需要，因此我通过自定义的 `clickable` 方法去掉了水波纹

```kotlin
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    composed {
        clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }) {
            onClick()
        }
    }
```

由于弹窗的背景色和弹窗内容区域 InnerDialog 应该是上下层叠的关系，所以两者应该位于同个 Box 下，Box 的 Modifier 开放给外部使用者

```kotlin
@Composable
fun BottomSheetDialog(
    modifier: Modifier = Modifier,
    visible: Boolean,
    cancelable: Boolean = true,
    canceledOnTouchOutside: Boolean = true,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    BackHandler(enabled = visible, onBack = {
        if (cancelable) {
            onDismissRequest()
        }
    })
    Box(modifier = modifier) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(durationMillis = 400, easing = LinearEasing)),
            exit = fadeOut(animationSpec = tween(durationMillis = 400, easing = LinearEasing))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = Color(0x99000000))
                    .clickableNoRipple {
                        if (canceledOnTouchOutside) {
                            onDismissRequest()
                        }
                    }
            )
        }
        InnerDialog()
    }
}
```

InnerDialog 需要有从下往上弹出，并从上往下消失的效果，通过自定义 AnimatedVisibility 的 `enter` 和 `exit` 动画即可实现

```kotlin
@Composable
private fun BoxScope.InnerDialog(
    visible: Boolean,
    cancelable: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        modifier = Modifier
            .clickableNoRipple {

            }
            .align(alignment = Alignment.BottomCenter),
        visible = visible,
        enter = slideInVertically(
            animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing),
            initialOffsetY = { 2 * it }
        ),
        exit = slideOutVertically(
            animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing),
            targetOffsetY = { it }
        ),
    ) {
        content()
    }
}
```

为了能够拖拽弹窗上下移动，这里通过 `draggable` 方法来检测拖拽手势，用 offsetY 来记录弹窗的 Y 坐标偏移量，同时通过 `animateFloatAsState` 以动画的形式平滑过度不同的 offsetY 值并触发重组，从而实现弹窗随用户的手势而上下滑动。此外，当用户松手 `onDragStopped` 时，再将 offsetY 重置为 0，从而实现弹窗拖拽回调的效果

```kotlin
@Composable
private fun BoxScope.InnerDialog(
    visible: Boolean,
    cancelable: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    var offsetY by remember {
        mutableStateOf(0f)
    }
    val offsetYAnimate by animateFloatAsState(targetValue = offsetY)
    AnimatedVisibility(
        modifier = Modifier
            .clickableNoRipple {

            }
            .align(alignment = Alignment.BottomCenter)
            .offset(offset = {
                IntOffset(0, offsetYAnimate.roundToInt())
            })
            .draggable(
                state = rememberDraggableState(
                    onDelta = {
                        offsetY = (offsetY + it.toInt()).coerceAtLeast(0f)
                    }
                ),
                orientation = Orientation.Vertical,
                onDragStarted = {

                },
                onDragStopped = {
                    offsetY = 0f
                }
            ),
        visible = visible,
        enter = slideInVertically(
            animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing),
            initialOffsetY = { 2 * it }
        ),
        exit = slideOutVertically(
            animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing),
            targetOffsetY = { it }
        ),
    ) {
        content()
    }
}
```

此外，原生的 BottomSheetDialog 还有个特点：当用户向下拖拽的距离不超出某个界限值时，弹窗会有向上回弹恢复的效果；当超出界限值时，则会直接关闭整个弹窗。为了实现这个效果，我们可以定义当用户向下拖拽的偏移量大于弹窗的一半高度时就直接关闭弹窗，否则就让其回弹

通过查看 BottomSheetScaffold 的源码，可以看到其是通过 `onGloballyPositioned` 方法来拿到整个 `sheetContent` 的高度，这里可以仿照其思路拿到整个 InnerDialog 的高度 `bottomSheetHeight` ，在 `onDragStopped` 方法对比拖拽距离即可

```kotlin
@Composable
private fun BoxScope.InnerDialog(
    visible: Boolean,
    cancelable: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    var offsetY by remember {
        mutableStateOf(0f)
    }
    val offsetYAnimate by animateFloatAsState(targetValue = offsetY)
    var bottomSheetHeight by remember { mutableStateOf(0f) }
    AnimatedVisibility(
        modifier = Modifier
            .clickableNoRipple {

            }
            .align(alignment = Alignment.BottomCenter)
            .onGloballyPositioned {
                bottomSheetHeight = it.size.height.toFloat()
            }
            .offset(offset = {
                IntOffset(0, offsetYAnimate.roundToInt())
            })
            .draggable(
                state = rememberDraggableState(
                    onDelta = {
                        offsetY = (offsetY + it.toInt()).coerceAtLeast(0f)
                    }
                ),
                orientation = Orientation.Vertical,
                onDragStarted = {

                },
                onDragStopped = {
                    if (cancelable && offsetY > bottomSheetHeight / 2) {
                        onDismissRequest()
                    } else {
                        offsetY = 0f
                    }
                }
            ),
        visible = visible,
        enter = slideInVertically(
            animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing),
            initialOffsetY = { 2 * it }
        ),
        exit = slideOutVertically(
            animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing),
            targetOffsetY = { it }
        ),
    ) {
        content()
    }
}
```

此外，还有个小细节需要注意。当用户向下拖拽关闭了弹窗时，offsetY 可能还不等于 0，这就会导致下次弹出时弹窗还会保持该偏移量，导致弹窗只展示了部分。因此需要当 InnerDialog 退出重组时，手动将 offsetY 重置为 0

```kotlin
DisposableEffect(key1 = null) {
    onDispose {
        offsetY = 0f
    }
}
```

至此，BottomSheetDialog 就完成了，向 BottomSheetDialog 传入想要展示的布局即可

```kotlin
BottomSheetDialog(
    modifier = Modifier,
    visible = viewState.visible,
    cancelable = true,
    canceledOnTouchOutside = true,
    onDismissRequest = viewState.onDismissRequest
) {
    DialogContent()
}


@Composable
private fun DialogContent(onDismissRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(fraction = 0.7f)
            .clip(shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(color = Color(0xFF009688)),
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            modifier = Modifier
                .align(alignment = Alignment.CenterHorizontally),
            onClick = {
                onDismissRequest()
            }) {
            Text(
                modifier = Modifier.padding(all = 4.dp),
                text = "dismissDialog",
                fontSize = 16.sp
            )
        }
    }
}
```

这里给出完整的源码：[ComposeBottomSheetDialog](https://github.com/leavesCZY/ComposeBottomSheetDialog)