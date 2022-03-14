> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

单纯看官方的介绍或者是网络上的文章，开发者也许已经对 Jetpack Compose 有这么一个印象了：**使用 Jetpack Compose 时我们可以深层次地嵌套布局而不用担心会影响性能**。这是 Google 在介绍 Jetpack Compose 时经常拿来和原生 View 体系进行比较的一个特性，也是介绍其优势时的一个着重点，本文就来介绍这一方面的相关知识点，涉及到的内容有：

- 原生 View 体系下，我们一直强调 **要减少布局的嵌套层次**，那这么做的意义是什么呢
- Jetpack Compose 的布局模型
- Jetpack Compose 如何实现自定义布局
- Jetpack Compose 是如何避免多次测量的
- Jetpack Compose **固有特性测量** 的作用，以及如何进行适配

# 减少布局嵌套的意义

进行应用性能优化最基本的一个方向就是 **减少布局的嵌套层次**，这是很多开发者都知道的一个知识点，这里先来讲下这么做的意义所在

以 FrameLayout 嵌套多个 TextView 为例，此时只有一层嵌套

```kotlin
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="wrap_content"
    android:orientation="vertical"
    android:layout_height="match_parent">

    <TextView
        android:id="@+id/textView1"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:padding="20dp"
        android:text="Jetpack Compose" />

    <TextView
        android:id="@+id/textView2"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="业志陈" />

    <TextView
        android:id="@+id/textView3"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="公众号：字节数组" />

</FrameLayout>
```

该布局有以下特点

- FrameLayout 的宽度是 wrap_content，即打算宽度由其子项来决定，子项的最大宽度是多少，FrameLayout 的宽度即是多少
- textView2 和 textView3 的宽度是 match_parent，即希望宽度占满整个 FrameLayout，FrameLayout 的宽度是多少，这两个 TextView 的宽度即是多少

此时就出现了一个矛盾点：FrameLayout 的宽度由三个子项的最大宽度来决定，因此需要先测量出所有子项的最大宽度后才能确定自身的宽度。而 textView2 和 textView3 又希望宽度和 FrameLayout 保持一致，因此只有先测量出 FrameLayout 的宽度后才能决定自身的宽度。这就相当于陷入了一个死循环中

当然，FrameLayout 在进行 `measure` 时也已经考虑到这种情况了，应对策略就是进行两次 `measure` 操作，其 `onMeasure`方法在逻辑上可以分为两步

第一步，FrameLayout 会按照正常流程来测量每一个子项。在这个过程中，如果 FrameLayout 的宽或高使用的是 `wrap_content` 的话，那么就将使用了 `match_parent` 的子项都保存到 `mMatchParentChildren` 中

```java
@Override
protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int count = getChildCount();
    //layout_width 或 layout_height 是否设置了 wrap_content
    final boolean measureMatchParentChildren =
            MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY ||
            MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY;
    mMatchParentChildren.clear();

    int maxHeight = 0;
    int maxWidth = 0;
    int childState = 0;

    for (int i = 0; i < count; i++) {
        final View child = getChildAt(i);
        if (mMeasureAllChildren || child.getVisibility() != GONE) {
            //第一次测量子项的宽高
            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            maxWidth = Math.max(maxWidth,
                    child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin);
            maxHeight = Math.max(maxHeight,
                    child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin);
            childState = combineMeasuredStates(childState, child.getMeasuredState());
            if (measureMatchParentChildren) {
                if (lp.width == LayoutParams.MATCH_PARENT ||
                        lp.height == LayoutParams.MATCH_PARENT) {
                    //将使用了 match_parent 的子项保存起来
                    mMatchParentChildren.add(child);
                }
            }
        }
    }

    ···

}
```

第二步，FrameLayout 会构建出新的 MeasureSpec 来重新测量 `mMatchParentChildren`，也即 textView2 和 textView3。此时这两个 TextView 就会得到一个新的 MeasureSpec，size 即第一步得到的 `measuredWidth`（需要减去 padding 和 margin），mode 变成了 `EXACTLY`，对应的也即 `match_parent` 这个属性值的含义

```java
@Override
protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

    ···

    if (count > 1) {
        for (int i = 0; i < count; i++) {
            final View child = mMatchParentChildren.get(i);
            final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

            final int childWidthMeasureSpec;
            if (lp.width == LayoutParams.MATCH_PARENT) {
                final int width = Math.max(0, getMeasuredWidth()
                        - getPaddingLeftWithForeground() - getPaddingRightWithForeground()
                        - lp.leftMargin - lp.rightMargin);
                childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                        width, MeasureSpec.EXACTLY);
            } else {
                childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
                        getPaddingLeftWithForeground() + getPaddingRightWithForeground() +
                        lp.leftMargin + lp.rightMargin,
                        lp.width);
            }

            ···
            //第二次测量子项的宽高
            child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
        }
    }
}
```

所以说，在这种简单的嵌套结构中，**父布局和子项之间的宽度是相互影响并通过双方来一起确定的**，导致了需要先后执行多次 measure 操作才能完成布局要求：FrameLayout 一次，textView1 一次，textView2 和 textView3 各两次，总计六次

而在实际开发中面对的情况其实会更加复杂：

- 即使使用的不是 FrameLayout，改为 LinearLayout 一样会有这个问题。此外，虽然我们一般也不会用 `wrap_content` 来嵌套 `match_parent`，但如果搭配使用 LinearLayout 和 `layout_weight`的话，测量次数也不止一次
- 如果将 textView2 和 textView3 替换为其它 ViewGroup 的话，将连锁导致 ViewGroup 内嵌的其它子项也要跟着一起重新测量
- 如果该布局是用于 Activity 的话，由于 ViewRootImpl 的 `performTraversals()` 会在 Activity 启动时被调用两次（API Leave 31），因此 FrameLayout 需要测量两次，连锁导致 textView1 测量两次，textView2 和 textView3 各测量四次，总计十二次

所以说，布局结构的嵌套层次越深，布局的测量次数和时间是会呈指数级增长的，从而连锁导致整个视图绘制到屏幕上的时间就越长，对用户来说应用的流畅度就越低，用户体验就越差

# 布局模型

Jetpack Compose 作为一个全新的现代 Android 原生 UI 开发框架，从设计之初自然就会考虑如何避免原生 View 体系的弊端，因此从底层就直接限制了 **不允许进行多次测量**，布局组件不能为了尝试不同的测量配置而多次测量任何子元素，否则将直接抛出 IllegalStateException。这使得我们可以根据需要进行深层次嵌套，此时测量次数也只会是线性增长，而不用担心会影响性能

引用官方的例子来说明：[Compose 布局基础知识](https://developer.android.google.cn/jetpack/compose/layouts/basics)

Jetpack Compose 的布局模型通过单次传递即可完成界面树的布局，其传递方式和原生 View 体系一样是通过递归的方式来实现。首先，系统会要求每个节点对自身进行测量，然后以递归方式完成所有子节点的测量，并将尺寸约束条件沿着树向下传递给子节点。再然后，确定叶节点的尺寸和放置位置，并将经过解析的尺寸和放置指令沿着树向上回传。简而言之，父节点会在其子节点之前进行测量，但会在其子节点的尺寸和放置位置确定之后再对自身进行调整

举个例子，以下的 `SearchResult()` 函数会生成相对应的界面树

```kotlin
@Composable
fun SearchResult(...) {
  Row(...) {
    Image(...)
    Column(...) {
      Text(...)
      Text(..)
    }
  }
}

SearchResult
  Row
    Image
    Column
      Text
      Text
```

在 `SearchResult` 示例中，界面树布局遵循以下顺序：

1. 系统要求根节点 `Row` 对自身进行测量
2. 根节点 `Row` 要求其第一个子节点（即 `Image`）进行测量
3. `Image` 是一个叶节点（也就是说，它没有子节点），因此该节点会报告尺寸并返回放置指令
4. 根节点 `Row` 要求其第二个子节点（即 `Column`）进行测量
5. 节点 `Column` 要求其第一个子节点 `Text` 进行测量
6. 由于第一个节点 `Text` 是叶节点，因此该节点会报告尺寸并返回放置指令
7. 节点 `Column` 要求其第二个子节点 `Text` 进行测量
8. 由于第二个节点 `Text` 是叶节点，因此该节点会报告尺寸并返回放置指令
9. 现在，节点 `Column` 已测量其子节点，并已确定其子节点的尺寸和放置位置，接下来它可以确定自己的尺寸和放置位置了
10. 现在，根节点 `Row` 已测量其子节点，并已确定其子节点的尺寸和放置位置，接下来它可以确定自己的尺寸和放置位置了

所以说，Jetpack Compose 在通过 Composition、Layout、Drawing 三个步骤（也即 **组合、布局、绘制**）将状态转换为界面元素的过程中，Layout 阶段就用来确定每个节点的尺寸信息和位置信息。每个节点均需要先完成包含的所有子项的测量工作后才能确定自己的尺寸，在确定自身尺寸之后才能来决定子项的位置信息；而如果某个子项同时又内嵌了其它子项的话，该子项就需要根据以上步骤递归完成自身的测量工作

`SearchResult()` 函数的 `measure` 和 `place` 顺序就如下图所示

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/0ad798b23da140d7b04734597cc78d81~tplv-k3u1fbpfcp-zoom-1.image)

# 自定义布局

这里来实现一个自定义布局以便加强理解，就叫它 `CustomLayout` 吧，在其内部的每个子项都会按照声明顺序逐次往右下角放置，且边界线对齐

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a312e6e811a0407eb880ab41f98941b9~tplv-k3u1fbpfcp-zoom-1.image)

CustomLayout 的使用方式和 Row、Column 等组件完全一样，随意嵌套即可

```kotlin
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeCustomLayoutTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(align = Alignment.Center),
                    color = MaterialTheme.colors.background
                ) {
                    CustomLayout()
                }
            }
        }
    }

}

@Composable
private fun CustomLayout() {
    CustomLayout(
        modifier = Modifier
            .background(color = Color.Yellow)
    ) {
        Spacer(
            modifier = Modifier
                .background(color = Color.Green)
                .size(size = 40.dp)
        )
        Spacer(
            modifier = Modifier
                .background(color = Color.Cyan)
                .size(size = 40.dp)
        )
        Spacer(
            modifier = Modifier
                .background(color = Color.Magenta)
                .size(size = 40.dp)
        )
        Spacer(
            modifier = Modifier
                .background(color = Color.Red)
                .size(size = 40.dp)
        )
    }
}
```

Jetpack Compose 的自定义布局是通过 `Layout` 方法来实现的

```kotlin
@Composable 
inline fun Layout(
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    measurePolicy: MeasurePolicy
)
```

- content。布局组件所包含的所有子项
- modifier。对布局组件声明的 Modifier，可以通过该值来声明自定义布局的尺寸
- measurePolicy。测量策略，布局组件包含的所有子项的尺寸和位置都是在当中进行设置

我们要实现各种自定义布局，最重要的一点就是要来计算各个子项应该获得的尺寸和位置，对应的逻辑都通过 MeasurePolicy 接口来实现，该接口一共包含五个方法，这里只要关注 `measure` 方法即可

```kotlin
fun interface MeasurePolicy {

    fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult

}
```

- measurables。每一个 Measurable 都对应自定义布局中的一个子项，同时也是子项的测量句柄，通过调用其内部的 `measure(constraints: Constraints)` 方法来对子项进行测量
- `constraints`。自定义的布局组件的父级对其的约束条件

要实现 CustomLayout，基本的步骤有：

1. `measure` 方法传入的 constraints 参数，代表的是 CustomLayout 的上一级对其的布局约束条件，包含的约束条件有 `minWidth、maxWidth、minHeight、maxHeight`，由于这里不想让上一级布局设定的 `minWidth` 和 `minHeight` 对子项产生影响，因此要将其重置为 0，或者说根据实际需要来构建一个新的 Constraints
2. CustomLayout 的整体宽高是通过累加内部所有子项的宽高而得来的，因此要先测量出每个子项的宽高后才能确定 CustomLayout 自身的宽高
3. 当确定了 CustomLayout 的宽高大小后，通过 `MeasureScope.layout` 方法来传递宽高信息
4. `MeasureScope.layout` 方法会提供一个 `Placeable.PlacementScope` 作用域，在这个作用域内部通过 `placeable.place`、`placeable.placeRelative` 等方法来放置每一个子项，即在这里计算每个子项在坐标系中的位置

```kotlin
@Composable
fun CustomLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier,
        measurePolicy = object : MeasurePolicy {
            override fun MeasureScope.measure(
                measurables: List<Measurable>,
                constraints: Constraints
            ): MeasureResult {
                if (measurables.isEmpty()) {
                    return layout(
                        constraints.minWidth,
                        constraints.minHeight
                    ) {}
                }
                //第一步
                val contentConstraints = constraints.copy(minWidth = 0, minHeight = 0)
                val placeables = arrayOfNulls<Placeable>(measurables.size)
                var layoutWidth = 0
                var layoutHeight = 0
                //第二步，测量所有子项，累加所有子项的宽高值
                measurables.forEachIndexed { index, measurable ->
                    val placeable = measurable.measure(contentConstraints)
                    placeables[index] = placeable
                    layoutWidth += placeable.width
                    layoutHeight += placeable.height
                }
                //第三步，传递布局自身所占据的宽高
                return layout(layoutWidth, layoutHeight) {
                    var top = 0
                    var left = 0
                    //第四步，计算每个子项应该放置的坐标值
                    placeables.forEach { placeable ->
                        if (placeable != null) {
                            placeable.place(position = IntOffset(x = left, y = top))
                            top += placeable.height
                            left += placeable.width
                        }
                    }
                }
            }
        }
    )
}
```

# 固有特性测量

看完以上内容后，其实依然还是不知道 Jetpack Compose 是如何来避免多次测量的

需要进行多次测量，往往是因为 **父布局和子项的宽高需要两者来共同确定**，这算作是比较常见的场景，这是由业务需求来决定的，无关乎我们使用的是什么 UI 框架。而在 Android 原生的 View 体系下，系统一开始的设计思路也决定了在 XML 声明的视图结构不可避免地需要进行多次测量。Jetpack Compose 作为现代的新型 Android 原生 UI 开发框架，在设计之初也自然会想着怎么用更高效的方式来实现这种业务场景

以 CustomLayout 为例，假设现在想要在 CustomLayout 之间插入一个分隔线 Divider

```kotlin
Divider(
    color = Color.Black,
    modifier = Modifier
		.width(10.dp)
        .fillMaxHeight()
)
```

Divider 希望自身高度和父布局保持一致，这种情况就类似于文章开头给出的例子了：CustomLayout 的高度需要通过累加所有子项（除了 Divider）的高度得到，因此需要测量完所有的子项后才能决定自身高度，而 Divider 在测量阶段又希望直接指定为父布局的高度

如果不对这种需求进行适配的话，Divider 的高度将等于 CustomLayout 能获得的最大高度，也即屏幕高度，因此 CustomLayout 整体视图最终就会超出整个屏幕

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/44bf7a5f43f347b3b65e540b64d692a1~tplv-k3u1fbpfcp-zoom-1.image)

为了解决这种问题，并避免需要多次测量，Jetpack Compose 提供的解决方案就是：**固有特性测量**

官方对固有特性测量这个特性描述得不是很清晰，以下内容属于我自己的个人理解，也许会有点理解偏差，读者要有自己的判断

还是以 CustomLayout 为例，先来看下固有特性测量的使用方式，这里主要就做了两点改动：

- CustomLayout 通过 `IntrinsicSize.Min` 来声明自己期望按最小固有高度来进行显示，相对应的还有一个 `IntrinsicSize.Max`
- 为 CustomLayout 新增了一个扩展函数 `matchParentHeight()`，交给 Divider 使用，用于表明该子项想要直接占满父布局高度

```kotlin
@Composable
private fun CustomLayout() {
    CustomLayout(
        modifier = Modifier
            .height(intrinsicSize = IntrinsicSize.Min)
            .background(color = Color.Yellow)
    ) {
        Spacer(
            modifier = Modifier
                .background(color = Color.Green)
                .size(size = 40.dp)
        )
        Spacer(
            modifier = Modifier
                .background(color = Color.Cyan)
                .size(size = 40.dp)
        )
        Divider(
            modifier = Modifier
                .width(width = 6.dp)
                .matchParentHeight(),
            color = Color.Gray
        )
        Spacer(
            modifier = Modifier
                .background(color = Color.Magenta)
                .size(size = 40.dp)
        )
        Spacer(
            modifier = Modifier
                .background(color = Color.Red)
                .size(size = 40.dp)
        )
    }
}
```

固有特性测量就用于满足 **父布局和子项的宽高需要两者来共同确定** 这种需求，其提供了一种机制用于在正式测量前能够让布局组件预先获取到子项对尺寸的 **期望值**，这些期望值包括：在特定的宽度下，子项能够正常显示的最小高度和最大高度分别是多少？以及在特定的高度下，子项能够正常显示的最小宽度和最大宽度又分别是多少？在拿到这些期望值后，也就知道了父布局在特定宽度或高度下能够正常显示的尺寸大小范围

MeasurePolicy 中的四个 Intrinsic 方法就代表这些期望值的获取途径。例如，当我们使用了 `height(intrinsicSize = IntrinsicSize.Min)`后，布局阶段就会来调用 `minIntrinsicHeight` 方法；使用了 `height(intrinsicSize = IntrinsicSize.Max)` 后，就会来调用 `maxIntrinsicHeight` 方法

```kotlin
fun interface MeasurePolicy {

    fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult

    fun IntrinsicMeasureScope.minIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int

    fun IntrinsicMeasureScope.minIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int

    fun IntrinsicMeasureScope.maxIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int

    fun IntrinsicMeasureScope.maxIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int
    
}
```

这四个方法都有默认实现，但默认实现未必能满足我们的需求，我们需要根据实际需求来重写特定方法。例如，我们可以通过重写 `minIntrinsicHeight`方法来计算出 CustomLayout 在特定 width 下能够正常显示的最小高度是多少。此时，除了调用了 `matchParentHeight()`方法的子项之外，其它子项的 `minIntrinsicHeight` 累加之后的值即是 CustomLayout 能够接受的最小高度

```kotlin
override fun IntrinsicMeasureScope.minIntrinsicHeight(
    measurables: List<IntrinsicMeasurable>,
    width: Int
): Int {
    var maxHeight = 0
    measurables.forEach {
        if (!it.matchParentHeight()) {
            maxHeight += it.minIntrinsicHeight(width)
        }
    }
    return maxHeight
}
```

重写这些方法后，就会影响 CustomLayout 获取到的 Constraints 中 `minWidth、maxWidth、minHeight、maxHeight` 等值的大小，使得 CustomLayout 能够感知到调用方允许自己显示的最小尺寸和最大尺寸

在适配固有特性测量之前，由于 CustomLayout 是根布局，所以 Constraints 对应的最大尺寸即屏幕宽高，类似于 `Constraints(minWidth = 0, maxWidth = 1080, minHeight = 0, maxHeight = 1776)`，因此 Divider 采用了 `fillMaxHeight()` 修饰后就会直接撑满整个屏幕高度

在适配固有特性测量之后，由于 CustomLayout  使用了`IntrinsicSize.Min` 来进行修饰，在语义上就是希望 CustomLayout 能够按照最小高度来进行显示，因此 Constraints 对应的最小和最大高度就变成了 `minIntrinsicHeight` 方法的返回值，类似于 `Constraints(minWidth = 0, maxWidth = 1080, minHeight = 480, maxHeight = 480)`，此时 Divider 的高度就会直接固定为 480 px 了，而不会越界

所以说，在适配了固有特性测量机制后，四个 Intrinsic 方法相当于是在正式开始 measure 之前进行的一次粗略测量，一次性计算出能接受的尺寸范围，CustomLayout 就无需在 measure 阶段来多次测量子项了，而是改为依靠 Intrinsic 方法来影响子项的测量结果，从而避免了多次测量

# 适配固有特性测量

再来介绍下如何让 CustomLayout 适配固有特性测量

为了能够识别出哪一个子项希望占满布局高度，就需要将这种 “期望” 传递给 CustomLayout，也即子项需要能够传参给到父布局。Jetpack Compose 通过 ParentDataModifier 来实现参数传递，我们在使用 Column 时声明的 `weight` 参数也是通过 ParentDataModifier 来传递的，IntrinsicMeasurable 接口内部就包含一个 Any? 类型的 `parentData` 参数

CustomLayout 通过声明自己专属的 CustomLayoutParentData 类来作为参数载体，并通过扩展函数 `matchParentHeight()`来传递参数值

```kotlin
@LayoutScopeMarker
@Immutable
interface CustomLayoutScope {

    @Stable
    fun Modifier.matchParentHeight(): Modifier

}

private object CustomLayoutScopeInstance : CustomLayoutScope {

    override fun Modifier.matchParentHeight(): Modifier {
        return this.then(
            LayoutMatchParentHeightImpl(
                matchParentHeight = true,
                inspectorInfo = debugInspectorInfo {
                    name = "matchParentHeight"
                    value = true
                    properties["matchParentHeight"] = true
                }
            )
        )
    }

}

internal data class CustomLayoutParentData(
    val matchParentHeight: Boolean = false
)

internal class LayoutMatchParentHeightImpl(
    val matchParentHeight: Boolean,
    inspectorInfo: InspectorInfo.() -> Unit
) : ParentDataModifier, InspectorValueInfo(inspectorInfo) {

    override fun Density.modifyParentData(parentData: Any?): Any {
        return (parentData as? CustomLayoutParentData)
            ?: CustomLayoutParentData(matchParentHeight = matchParentHeight)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as LayoutMatchParentHeightImpl
        if (matchParentHeight != other.matchParentHeight) return false
        return true
    }

    override fun hashCode(): Int {
        return matchParentHeight.hashCode()
    }

    override fun toString(): String {
        return "LayoutMatchParentHeightImpl(matchParentHeight=$matchParentHeight)"
    }

}
```

之后只要判断 IntrinsicMeasurable 包含的 `parentData` 成员变量是否是 CustomLayoutParentData 即可获取到传参值 `matchParentHeight`

```kotlin
private fun IntrinsicMeasurable.matchParentHeight(): Boolean {
    return (parentData as? CustomLayoutParentData)?.matchParentHeight ?: false
}
```

最终 CustomLayout 相对之前主要有以下改动：

1. 为子项提供一个专属的特定布局作用域 CustomLayoutScope，确保 `matchParentHeight()`方法只有 CustomLayout 的子项才能使用
2. Divider 会影响 CustomLayout 占据的整体宽度，但不会影响 CustomLayout 的整体高度，因此在进行 measure 时就要分情况来判断是否需要累加 layoutHeight
3. 在计算子项的坐标值时，除了 Divider 的 Y 坐标需要固定为 0 外，其它子项照旧

```kotlin
@Composable
fun CustomLayout(
    modifier: Modifier = Modifier,
    content: @Composable CustomLayoutScope.() -> Unit
) {
    Layout(
        content = { CustomLayoutScopeInstance.content() },
        modifier = modifier,
        measurePolicy = object : MeasurePolicy {

            private fun IntrinsicMeasurable.matchParentHeight(): Boolean {
                return (parentData as? CustomLayoutParentData)?.matchParentHeight ?: false
            }

            override fun IntrinsicMeasureScope.minIntrinsicHeight(
                measurables: List<IntrinsicMeasurable>,
                width: Int
            ): Int {
                var maxHeight = 0
                measurables.forEach {
                    if (!it.matchParentHeight()) {
                        maxHeight += it.minIntrinsicHeight(width)
                    }
                }
                return maxHeight
            }

            override fun MeasureScope.measure(
                measurables: List<Measurable>,
                constraints: Constraints
            ): MeasureResult {
                if (measurables.isEmpty()) {
                    return layout(
                        constraints.minWidth,
                        constraints.minHeight
                    ) {}
                }
                val contentConstraints = constraints.copy(minWidth = 0, minHeight = 0)
                val dividerConstraints = constraints.copy(minWidth = 0)
                val placeables = arrayOfNulls<Placeable>(measurables.size)
                val matchParentHeightChildren = mutableListOf<Placeable>()
                var layoutWidth = 0
                var layoutHeight = 0
                measurables.forEachIndexed { index, measurable ->
                    val placeable = if (measurable.matchParentHeight()) {
                        measurable.measure(dividerConstraints).apply {
                            layoutWidth += width
                            matchParentHeightChildren.add(this)
                        }
                    } else {
                        measurable.measure(contentConstraints).apply {
                            layoutWidth += width
                            layoutHeight += height
                        }
                    }
                    placeables[index] = placeable
                }
                return layout(layoutWidth, layoutHeight) {
                    var top = 0
                    var left = 0
                    placeables.forEach { placeable ->
                        placeable as Placeable
                        if (matchParentHeightChildren.contains(placeable)) {
                            placeable.place(position = IntOffset(x = left, y = 0))
                        } else {
                            placeable.place(position = IntOffset(x = left, y = top))
                            top += placeable.height
                        }
                        left += placeable.width
                    }
                }
            }
        }
    )
}
```

最终 CustomLayout 就可以正常显示 Divider 了

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/b71462bda2e2459ba68ce3dd7f8b08ca~tplv-k3u1fbpfcp-zoom-1.image)

# 结尾

Jetpack Compose 除了通过固有特性测量机制避免多次测量外，也少了将 XML 文件反射实例化为 View 的步骤，减少了 I/O 操作，这也是 Jetpack Compose 的一个性能优势点

此外，对于我们的开发体验也有很大提升：

- 从命令式转向了声明式，使得我们可以专注于状态管理，减少了出现问题的概率
- 少了很多割裂感，无需在各个 Java、Kotlin、XML 文件之间来回切换，不管是 UI 还是业务逻辑，都是直接 Kotlin 搞定（但现阶段 Preview 功能感觉还是好慢）
- 由于 Android 各个版本之间的差异性，同一套 View 体系代码经常会在不同系统版本上有着不同的风格，导致我们经常需要定义各种 style 和 theme 来保证 UI 统一，采用 Jetpack Compose 后就没有这种烦恼了，由其来抹平各个系统版本的差异性

Jetpack Compose 目前更新得很快，随着后期的不断优化，相信也是会越来越好 ~

最后也给出本文完整的示例代码：[ComposeCustomLayout](https://github.com/leavesCZY/ComposeCustomLayout)