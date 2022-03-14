> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

在 2019 年的 Google/IO 大会上，亮相了一个全新的 Android 原生 UI 开发框架 Jetpack Compose。与 IOS 的 SwiftUI 一样，Jetpack Compose 也是一个声明式的 UI  框架，随着 Android 和 IOS 两大移动平台相继推出了自己平台专属的声明式 UI 框架，标志着整个行业已开始转向声明性界面模型，该模型大大简化了与构建和更新界面关联的工程设计

经过两年多的打磨，到了 2021 年七月底，Google 正式发布了 Jetpack Compose 的 1.0 版本，这是 Compose 的稳定版本，可供开发者在生产环境中使用

引用 Google 官网对 Jetpack Compose 的介绍：Jetpack Compose 是用于构建原生 Android 界面的新工具包。它可简化并加快 Android 上的界面开发，帮助您使用更少的代码、强大的工具和直观的 Kotlin API，快速打造生动而精彩的应用

其核心功能包括：

- 互操作性：Compose 可以和既有的应用进行互操作。您可以将 Compose UI 嵌入 View，反之亦然。您可以只在屏幕上添加一个按钮，也把自己创建的自定义视图保留在现在用 Compose 打造的界面中
- Jetpack 集成：Compose 和大家熟知且喜爱的 Jetpack 开发库天然整合。通过与 Navigation、Paging、LiveData (或 Flow/RxJava)、ViewModel 和 Hilt 的整合，Compose 可以与您现有的架构完美共存
- Material：Compose 提供了 Material Design 组件和主题的实现，使您能够轻松构建符合您的品牌个性的美观应用。Material 主题系统更容易理解和追踪，再也不需要翻阅多个 XML 文件
- 列表：Compose 的 Lazy 组件为数据列表的呈现提供了一种简单扼要且功能强大的方式，而且将模版代码精简到了最少
- 动画：Compose 简明的动画 API 让您可以更轻松地打造出让用户眼前一亮的体验

# 一、compose_chat

技术的世界总是在不断变化的，新的技术总在不断涌现，我数了一下，现在一名 Android 应用开发工程师需要掌握的最基础技能有以下几个，有点 MMP 的感觉 🤣🤣

需要掌握的 UI 开发框架：

- 传统的 View 体系
- 跨平台的 Flutter
- 最新的 Jetpack Compose
- xxxxx

需要掌握的语言有：

- Java
- Dart
- Kotlin
- xxxxx

实践出真理，学不动也要学 🤣🤣 Jetpack Compose 大概率会成为以后 Android 原生应用开发时的首选技术方案，所以我也做了一次实战演练，花了两个月时间断断续续写了一个完全用 Compose 实现的 IM APP，实现了基本的好友聊天功能

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbeccad79d94d0dbff85bc02d7ccf04~tplv-k3u1fbpfcp-zoom-1.image)

整个 APP 采用的是单 Activity 形式，内部通过 navigation 来模拟多屏幕跳转，使用了单向数据流模式，所有业务逻辑均通过 ViewModel 来完成，业务逻辑的处理结果再以可组合函数的入参参数的形式回传给 UI 层，底层通过腾讯云的 IM SDK 来实现通信

![](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/28bcba206ff54bfda826e27c092fd4f8~tplv-k3u1fbpfcp-watermark.image)

本文主要介绍的是 Jetpack Compose 的各种重点概念和功能点，并不会涉及太多 compose_chat 的实际编码内容，但会以 compose_chat 作为讲解时的辅助例子。读者如果想要学习 Jetpack Compose 的话，compose_chat 会是一个很好的入门学习项目，希望对你有所帮助 🤣🤣

# 二、命令式与声明式

长期以来，Android 的视图层次结构都可以表示为一个视图树，若干个 View 和 ViewGroup 以嵌套的关系组成整个视图树，开发者通过 XML 来声明整个视图树的层次结构，再通过 `findViewById()`来拿到每个控件的引用。当状态发生变化需要刷新 UI 时，就通过主动调用控件的特定方法来更新 UI

整个过程就类似如下所示。通过 TextView 来显示用户名，在用户名发生变化时需要通过主动调用 `TextView.setText` 方法来刷新 UI，开发者直接持有并维护着每个视图结点，想要更新视图都需要开发者直接向控件下发“指令”，这整个过程的复杂度和出错概率随着需要维护的控件数量的增加而增加。这种方式就属于**命令式**

```kotlin
val tvUserName = findViewById<TextView>(R.id.tvUserName)

fun onUserNameChanged(userName: String) {
    tvUserName.text = userName
}

onUserNameChanged("业志陈")
```

Compose 则是通过声明一系列不包含返回值的**可组合（Composable）函数**来构建界面的。可组合函数只负责描述所需的屏幕状态，且不提供任何视图控件的引用给到开发者。可组合函数可以包含入参参数，参数就用来参与描述屏幕状态，当需要改变屏幕状态时，也是通过**生成新的入参参数并再次调用可组合函数**来实现 UI 刷新的

整个过程就类似如下所示。Compose 中对应 TextView 的 "控件" 就是 `Text()` 函数，Greeting 函数在拿到用户名 name 后，就通过 `Text()` 函数来进行显示，其接收一个 String 参数用于在屏幕上显示文本信息。在这整个过程中，开发者不持有任何视图节点的引用，而是以描述的方式来声明视图应该如何呈现，且视图并不直接持有状态，而是依赖状态来生成自身。这种方式就属于**声明式**

<img src="https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/d456efd9169e4d4aa64751dcd52514cc~tplv-k3u1fbpfcp-zoom-1.image" style="zoom: 43%;" />

声明式 UI 的一个很显著的特点就是：视图是否存在是根据有没有被声明过来决定的

以 compose_chat 为例，主界面包含一个悬浮按钮 floatingActionButton

<img src="https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/434b51ee5b124dde91618759d5c6a116~tplv-k3u1fbpfcp-watermark.image" style="zoom:80%;" />

该按钮只会在 Friendship 页面出现，HomeScreen 依靠`if (screenSelected == ViewScreen.Friendship)`语句来决定其出现时机，只有 if 语句为 true，FloatingActionButton() 函数才会被执行，此时才会显示悬浮按钮

这和原生的 View 体系有很大区别，按 View 体系的做法来实现的话，FloatingActionButton 会对应一个控件对象且对象是一直存在的，只是我们选择性地通过 `View.Gone()` 来将其隐藏。而按 Compose 的做法，只有 FloatingActionButton() 函数被执行了悬浮按钮才会存在，否则对于当前屏幕来说，悬浮按钮就相当于从来没有出现过

```kotlin
@Composable
fun HomeScreen(
    navController: NavHostController,
    screenSelected: ViewScreen,
    onTabSelected: (ViewScreen) -> Unit
) {
    ChatTheme(appTheme = appTheme) {
        ProvideWindowInsets {
            ModalBottomSheetLayout() {
                Scaffold(
                    floatingActionButton = {
                        if (screenSelected == ViewScreen.Friendship) {
                            FloatingActionButton(
                                backgroundColor = MaterialTheme.colors.primary,
                                onClick = {
                                    sheetContentAnimateTo(targetValue = ModalBottomSheetValue.Expanded)
                                }) {
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    tint = Color.White,
                                    contentDescription = null,
                                )
                            }
                        }
                    },
                )
            }
        }
    }
}
```

# 三、可组合函数

带有 `@Composable`注解的函数即可组合函数，该注解就用于告知 Compose 编译器：此函数旨在将数据转换为界面。例如，Compose 的`Text()` 函数就提供了 TextView 的所有功能，开箱即用，方法签名如下所示

```kotlin
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
)
```

当中，最需要关注的当属 Modifier 这个入参参数了，Compose 提供了一系列开箱即用的“控件”函数，例如，对应 FrameLayout 的 Box()、对应 ImageView 的 Image()、对应 RecyclerView 的 LazyColumn() 等，这些控件都包含一个 Modifier 入参参数。Modifier 功能十分强大，每个控件的**宽高大小、位置、方向、对齐、裁切、间距、背景色、点击、甚至手势识别**等功能都需要通过它来完成，每个功能都通过扩展函数的方式来声明，以链式调用的方式进行使用

```kotlin
Text(
        modifier = Modifier
            .weight(weight = messageWidthWeight, fill = false)
            .padding(
                start = messageStartPadding,
                top = messageTopPadding,
                end = messageEndPadding
            )
            .clip(shape = messageShape)
            .background(color = friendMsgBgColor)
            .pointerInput(key1 = Unit) {
                detectTapGestures(
                    onLongPress = {
                        onLongPressMessage(textMessage)
                    },
                )
            }
            .padding(
                start = messageInnerPadding,
                top = messageInnerPadding,
                end = messageInnerPadding,
                bottom = messageInnerPadding
            ),
        text = textMessage.msg,
        style = MaterialTheme.typography.subtitle1,
        textAlign = TextAlign.Left,
    )
```

Compose 提供的一系列“控件”函数基本已经能够满足我们的日常开发需求了，我们在开发 Compose 应用时，就可以通过嵌套组合官方提供的控件来实现各种自定义需求

以 compose_chat 为例，好友发送的每一条消息都通过 FriendTextMessageItem() 来显示，包括好友头像和文本消息，当中便嵌套使用到了多个官方控件

![](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/76dfa61a8ac544718f0677b6ce9e39ee~tplv-k3u1fbpfcp-watermark.image)

# 四、状态 

应用的状态（State）是指可以随时间变化的任何值，其定义十分宽泛，从函数的入参参数到应用的背景色都包括在内

对于 Android 传统的 View 视图结构来说，控件会直接持有着 State。例如，EditText 的内部就包含一个 CharSequence 类型的全局变量 mText，用于存储 EditText 当前显示的文本。当想要改变文本内容时，就需要通过手动调用 `EditText.setText` 方法来改变 mText，EditText 也随之刷新，mText 即 EditText 持有的 State

而 Compose 通过组合多个可组合函数来描述整个屏幕状态并以此来绘制屏幕，更新视图的唯一途径就是**生成新的入参参数并再次调用可组合函数**，新的入参参数就代表想要的屏幕状态，每当 State 更新时就会触发可组合函数进行重组，从而实现 UI 刷新。在这整个过程中，可组合函数并不直接持有 State，而是通过读取 State 来确定自身应该如何显示

Compose 中的 OutlinedTextField 函数在功能上就类似于 EditText，但 OutlinedTextField 想要实现和 EditText 相同的效果就会显得稍微麻烦一点

看以下示例，当用户不断向 OutlinedTextField 输入信息时，由于其 value 一直为空字符串，所以 OutlinedTextField 呈现出来的也只会一直是空空如也，因为 OutlinedTextField 只会在 value 值发生变化的时候才进行 UI 刷新

```kotlin
@Composable
fun HelloContent() {
    OutlinedTextField(
        value = "",
        onValueChange = {
            
        }
    )
}
```

想要让 OutlinedTextField 实现和 EditText 相同的效果，就需要有一个中间值对其状态进行描述，也即以下的 name。mutableStateOf 函数返回的是与 Compose 运行时集成的可观察类型，当 mutableState 值发生变化时就会触发所有读取该值的可组合函数进行重组。而 remember 的作用就是让 Compose 在初始组合期间将由其保存的值存储在组合中，并在重组期间返回存储的值，从而使得值可以跨越重组被保留下来，当然这个保留期限也只限于可组合函数的单次生命周期内

```kotlin
@Composable
fun HelloContent() {
    var name by remember { mutableStateOf("") }
    OutlinedTextField(
        value = name,
        onValueChange = {
            name = it
        }
    )
}
```

以 compose_chat 为例，其主界面如下所示

<img src="https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/43009ac4ef25400c95c123ed47193dbb~tplv-k3u1fbpfcp-watermark.image" style="zoom:80%;" />

主界面由 HomeScreen 函数声明，内部一共包含三个子界面，对应 ConversationScreen、FriendshipScreen、UserProfileScreen 三个函数。HomeScreen 就通过 screenSelected 来控制当前需要显示哪个子界面，当 screenSelected 发生变化时，就会触发 HomeScreen 函数重组，使得 when 表达式再次执行，从而显示新的子界面

```kotlin
@Composable
fun HomeScreen(
    navController: NavHostController,
    screenSelected: HomeScreenTab,
    onTabSelected: (HomeScreenTab) -> Unit,
    backDispatcher: OnBackPressedDispatcher,
) {
    ModalBottomSheetLayout() {
        Scaffold() {
            when (screenSelected) {
                HomeScreenTab.Conversation -> {
                    ConversationScreen()
                }
                HomeScreenTab.Friendship -> {
                    FriendshipScreen()
                }
                HomeScreenTab.UserProfile -> {
                    UserProfileScreen()
                }
            }
        }
    }
}
```

# 五、状态提升

看以下例子。name 的存在，相当于使得 HelloContent 这个可组合函数包含了一个内部状态，该状态对于调用方来说是完全不可见的。如果调用方不需要控制状态的变化或者是接收状态变化的通知，那么隐藏内部状态就变得十分有用，因为这样就降低了调用方的调用成本。但是，具有内部状态的可组合项往往不易重复使用，也更难测试

```kotlin
@Composable
fun HelloContent() {
    var name by remember { mutableStateOf("") }
    OutlinedTextField(
        value = name,
        onValueChange = {
            name = it
        }
    )
}
```

如果想要将 HelloContent 改造为无状态模式的话，就需要进行**状态提升**，状态提升是一种将状态移至可组合项的调用方以使可组合项无状态的模式。仿照 OutlinedTextField 的方式，将状态均交由外部提供，这样 name 这个状态值就交由调用方 HelloContentOwner 来进行维护并持有了

```kotlin
@Composable
fun HelloContent(name: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = name,
        onValueChange = onValueChange
    )
}

@Composable
fun HelloContentOwner() {
    var name by remember { mutableStateOf("") }
    HelloContent(name = name) {
        name = it
    }
}
```

以 compose_chat 为例。HomeScreen 的子界面是通过点击底部的 BottomBar 来进行切换的，即切换子界面的请求是从 BottomBar 发起的。BottomBar 需要拿到 screenSelected 才能决定应该选中哪个 tab，HomeScreen 也需要拿到 screenSelected 才能知道当前应该显示哪个子界面，因此 BottomBar 就不应该直接持有 screenSelected 这个状态，而是应该交由调用方来提供，这里就使用到了状态提升这个概念

BottomBar 不直接持有 screenSelected，而是由 HomeScreen 来提供。当用户点击 BottomBar 时，该点击事件也会从 BottomBar 向上传递给 HomeScreen（HomeScreen 再将该事件回调给更上层的调用方），由最上层的调用方来负责改变 screenSelected 的当前值，以此触发 BottomBar 和 HomeScreen 进行重组

在这整个过程中，BottomBar 并不包含任何内部状态，而只负责将用户的点击事件传递给调用方，其本身是**无状态**的。而 onTabSelected 这个回调函数就相当于**应用的业务逻辑**，其负责对用户的点击事件进行响应并改变 screenSelected 的值，screenSelected 就相当于**应用的状态**。当 screenSelected 发生变化时，Compose 就会将最新的状态值传给可组合函数，以此触发屏幕重新绘制。可组合函数由于状态发生变化导致再次执行的过程就称为**重组**

```kotlin
@Composable
fun HomeScreen(
    navController: NavHostController,
    screenSelected: HomeScreenTab,
    onTabSelected: (HomeScreenTab) -> Unit,
    backDispatcher: OnBackPressedDispatcher,
) {
    ChatTheme() {
        ProvideWindowInsets {
            ModalBottomSheetLayout {
                Scaffold(
                    bottomBar = {
                        HomeScreenBottomBar(
                            screenList = ViewScreen.values().toList(),
                            screenSelected = screenSelected,
                            onTabSelected = onTabSelected
                        )
                    },
                ) {
                    when (screenSelected) {
                        HomeScreenTab.Conversation -> {
                            ConversationScreen()
                        }
                        HomeScreenTab.Friendship -> {
                            FriendshipScreen()
                        }
                        HomeScreenTab.UserProfile -> {
                            UserProfileScreen()
                        }
                    }
                }
            }
        }
    }
}
```

在这整个过程中，状态是向下传递的，而事件是向上传递的。状态下降、事件上升的这种模式称为“单向数据流”，通过遵循单向数据流，可以将在界面中显示状态的可组合项与应用中存储和更改状态的部分解耦。因此，应用的界面状态应该都交由可组合函数的入参参数来定义，而应用的业务逻辑应该交由 ViewModel 来容纳并处理，业务逻辑的处理结果再以新的入参参数的形式传递给可组合函数，以此对用户进行响应和界面更新。遵循这种规则后，UI 层就有了一个统一且单一的数据源，这样应用才更不容易出错

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/63441d2fd5ec4798a1207b2715db34ea~tplv-k3u1fbpfcp-watermark.image)

提升状态时，有三条规则可帮助开发者弄清楚状态应去向何处：

1. 状态应至少提升到使用该状态（读取）的所有可组合项的**最低共同父项**
2. 状态应至少提升到**它可以发生变化（写入）的最高级别**
3. 如果**两种状态发生变化以响应相同的事件**，它们应**一起提升**

> 我认为，遵循状态提升的理念并非一定要做到所有可组合项均无状态。虽然 BottomBar 做到了无状态，但最终 screenSelected 也需要转交给上一级的调用方进行持有，调用方是有状态的。虽然可以将 screenSelected 再次提升到 ViewModel 中进行持有，但像这种不依赖外部环境（例如，网络请求，系统配置等），仅依靠用户事件进行变化的状态，我觉得将其提升到使用该状态（读取）的所有可组合项的**最低共同父项**即可，不必强行做到所有可组合项均无状态

# 六、纯函数

在很多讲解关于程序设计最佳实践的文章或者书籍里，都会推荐一个编码原则：**尽可能使用 val、不可变对象及纯函数来设计程序**。这个原则在 Compose 中也同样需要遵守，因为一个合格的可组合函数就应该属于**纯函数**，幂等且没有副作用

何谓纯函数？假如一个函数使用相同的入参参数重复执行时，总是能获得相同的运行结果，且运行结果不会影响任何外部状态，也不用担心重复执行会对外部造成改变，那么这个函数就称为纯函数

纯函数不具备副作用，具有引用透明性。副作用就是指修改了某处的某些东西，比如：

- 引用或修改了外部变量的值
- 执行了 IO 操作，比如读写了 SharedPreferences
- 执行了 UI 操作，比如修改了一个按钮的可操作状态

以下例子就不属于纯函数，由于受到外部变量的影响，使用相同入参参数多次执行 count 函数的结果并不全部相同，且每次执行都会影响到外部环境（使用到了 println 函数），这些都属于副作用

```kotlin
var count = 1

fun count(x: Int): Int {
    count += x
    println(count)
    return count
}
```

使用 Compose 时需要注意：

- 可组合函数可以按任何顺序执行
- 可组合函数可以并行执行
- 重组会跳过尽可能多的可组合函数和 lambda
- 重组是乐观的操作，可能会被取消
- 可组合函数可能会像动画的每一帧一样非常频繁地运行

看以下例子。按照我们的直观理解，ButtonRow 中的三个函数是会顺序执行的，且三个函数会得到一个依次递增的 count 值，但这在 Compose 中是没有保障的。为了提升运行效率，Compose 可能会在后台线程中以并行方式来执行多个可组合函数，这也意味着可组合函数之间的先后顺序是不确定的。而且由于存在**智能重组**，Compose 会自动识别出哪些可组合函数并没有发生更改从而跳过重组，而仅仅重新执行发生变化的部分函数，从而仅更新屏幕的某一部分

Compose 的这些举措都是为了提升程序的运行效率还有屏幕的绘制效率，但这也导致可组合函数在读写外部变量时是完全没有保障的，我们无法假设在执行 MiddleScreen 前 StartScreen 就已经修改了 count，两者的顺序可能完全相反甚至根本没有被执行

```kotlin
@Composable
fun ButtonRow() {
    StartScreen()
    MiddleScreen()
    EndScreen()
}

var count = 0

@Composable
fun StartScreen() {
    Text(text = count.toString())
    count += 1
}

@Composable
fun MiddleScreen() {
    Text(text = count.toString())
    count += 1
}

@Composable
fun EndScreen() {
    Text(text = count.toString())
    count += 1
}
```

此外，由于可组合函数可能会像每一帧一样频繁地重新执行，例如在执行动画时，可组合函数会快速地执行，以避免在播放动画期间出现卡顿，这个过程中可组合函数就会不断地在被重复调用。如果可组合函数存在副作用，例如内部存在读写 SharedPreferences 的操作，可能一秒钟就会被调用几百次，这就会严重影响到屏幕的绘制效率，从而导致页面卡顿了，所以对于这类执行成本高昂的操作，需要放到后台协程中执行，并将执行结果作为参数传递给可组合函数

所以说，可组合函数需要做到无副作用才能得到正确的期望结果。此外，对于相同的入参参数，可组合函数应该一直呈现相同的表现形式。多个可组合函数之间应该保持状态独立，不能具有互相依赖性。对于共享的状态，应该以入参参数的形式进行传递，并且将状态放在最顶级函数或者 ViewModel 中进行维护

# 七、副作用

在某些情况下，可组合函数可能无法做到完全无副作用，例如，我们在切换应用主题的时候希望系统状态栏也能跟着一起改变背景色，此时就说可组合函数产生了副作用。为了处理这种情况，Compose 也提供了 Effect API，以便以可预测的方式执行这些附带效应

以 compose_chat 为例。设置系统状态栏的颜色是通过 SetSystemBarsColor() 函数来实现的。我们希望在应用刚启动时，以及应用主题发生变化时，SetSystemBarsColor() 都能够被执行一次，这可以通过 DisposableEffect 来实现

当 DisposableEffect 进入组合时，或者是 key 发生变化时，DisposableEffect 内的代码就会被执行，从而改变系统状态栏，除此之外每次界面重组时都不会再次执行，从而避免了无意义的调用

```kotlin
@Composable
fun SetSystemBarsColor(
    key: Any = Unit,
    statusBarColor: Color = MaterialTheme.colors.background,
    navigationBarColor: Color = MaterialTheme.colors.background
) {
    val systemUiController = rememberSystemUiController()
    val isLight = MaterialTheme.colors.isLight
    DisposableEffect(key1 = key) {
        systemUiController.setStatusBarColor(
            color = statusBarColor,
            darkIcons = isLight
        )
        systemUiController.setNavigationBarColor(
            color = navigationBarColor,
            darkIcons = isLight
        )
        systemUiController.systemBarsDarkContentEnabled = isLight
        onDispose {

        }
    }
}
```

HomeScreen 传给 SetSystemBarsColor 的 Key 也即 appTheme，因此每次切换主题后都能保证 DisposableEffect 会再次执行

```kotlin
@Composable
fun HomeScreen(
    navController: NavHostController,
    screenSelected: HomeScreenTab,
    onTabSelected: (HomeScreenTab) -> Unit
) {
    val homeViewModel = viewModel<HomeViewModel>()
    val appTheme by homeViewModel.appTheme.collectAsState()
    ChatTheme(appTheme = appTheme) {
        ProvideWindowInsets {
            SetSystemBarsColor(
                key = appTheme,
                statusBarColor = Color.Transparent,
                navigationBarColor = MaterialTheme.colors.primaryVariant
            )
        }
    }
}
```

> 更多关于 Effect API 的介绍请看这里：[Compose 中的附带效应](https://developer.android.google.cn/jetpack/compose/side-effects)

# 八、布局

布局是每个 UI 框架都必须的功能，不管是 View、Flutter、还是 Compose，都必须提供一些开箱即用的布局控件，这里就介绍下 compose_chat 中使用得比较多的 ConstraintLayout 和 LazyColumn

## ConstraintLayout 

在以往的 Android View 视图体系下，我们都会尽量避免在布局中进行多层嵌套，因为嵌套层次越深，在测量 View 时需要的次数和时间就会越多，这就会严重影响到应用的运行性能了，因此 Google 官方也建议开发者尽量使用 ConstraintLayout 进行布局，实现扁平化布局。但 Compose 不同，由于 Compose 可以避免多次测量，因此开发者可以根据需要进行深层次嵌套，而不用担心会影响性能。同时 Compose 也提供了自己专属的 constraintlayout-compose，用于实现约束定位

以 compose_chat 为例。每个好友列表 Item 就对应 FriendshipItem() 函数，其内部就使用到了 ConstraintLayout 进行布局，其使用思路和 View 版本的差不多。首先需要通过 createRefs() 来声明“控件”的引用，然后通过 constrainAs 将引用和“控件”绑定在一起，类似于为“控件”声明 ID，之后每个“控件”之间就可以通过 linkTo 方法进行关联定位了，当中 parent 即指 ConstraintLayout 自身

![](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/5fce8b4dc0d545a99715de767c6445d7~tplv-k3u1fbpfcp-watermark.image)

## LazyColumn

在以往的 Android View 视图体系下，我们在加载长列表时一般是通过 RecyclerView 来实现的，以便能够缓存复用 Item，RecyclerView 在滑动性能上还是很优越的，能有效地避免由于数据量过大导致滑动卡顿的情况，缺点就是需要由开发者来声明各种 Adapter 和 ViewHolder，这一点比较麻烦

Compose 中对应 RecyclerView 的是 LazyColumn() 函数，从名字就可以猜出该函数是个竖向滑动列表，且实现了懒加载。实际上 LazyColumn 也的确实现了 Item 的缓存复用机制，重点在使用上要比 RecyclerView 简单很多，因为我们再也不用声明各种繁琐的 Adapter 和 ViewHolder 了

以 compose_chat 为例。在拿到好友列表 friendList 后，通过 for 循环，每个 PersonProfile 就对应 LazyColumn 的一个列表项 item()，在 item() 函数中声明的 FriendshipItem() 即每个列表项要呈现的视图。通过循环调用  item() 函数，就可以完成整个滑动列表数据项的声明了，而且即使存在多种样式的 Item，也通过相同方式来声明即可，就像以下例子中我为列表添加了一个底部间距 Spacer

```kotlin
@Composable
fun FriendshipScreen(
    paddingValues: PaddingValues,
    friendList: List<PersonProfile>,
    onClickFriend: (PersonProfile) -> Unit
) {
    Scaffold(
        modifier = Modifier
            .padding(bottom = paddingValues.calculateBottomPadding())
            .fillMaxSize()
    ) {
        if (friendList.isEmpty()) {
            EmptyView()
        } else {
            LazyColumn {
                friendList.forEach {
                    item(key = it.userId) {
                        FriendshipItem(personProfile = it, onClickFriend = onClickFriend)
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}
```

# 九、动画  &  手势操作 

Jetpack Compose 提供了一些功能强大且可扩展的 API，可用于在应用界面中轻松实现各种动画效果。动画在现代移动应用中至关重要，其目的是实现自然流畅、易于理解的用户体验。许多 Jetpack Compose 动画 API 可以提供可组合函数，就像布局和其他界面元素一样；它们由使用 Kotlin 协程挂起函数构建的较低级别 API 提供支持

此外，Compose 也提供了多种 API 用于检测用户互动生成的手势。API 涵盖各种用例：

- 其中一些**级别较高**，旨在覆盖最常用的手势。例如，clickable修饰符可用于轻松检测点击，此外它还提供无障碍功能，并在点按时显示视觉指示（例如涟漪）
- 还有一些不太常用的手势检测器，它们在**较低级别**提供更大的灵活性，例如 PointerInputScope.detectTapGestures 或 PointerInputScope.detectDragGestures，但不提供额外功能

以 compose_chat 为例。个人资料页 ProfileScreen 就同时应用到了动画和手势操作：背景图同时包含了 **裁切 + 缩放 + 旋转** 三种动画，用户可以拖拽头像进行移动，当松手时头像也会通过**弹簧动画**自动移回原位

<img src="https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a37c2618a7df40e496d95c56ce45441e~tplv-k3u1fbpfcp-watermark.image" style="zoom:80%;" />

## 动画

前文很多地方都有讲到：更新视图的唯一途径就是生成新的入参参数并再次调用可组合函数，动画也是如此。想要让视图以一种连贯且自然的方式进行变换，那么意思也就是说需要有一个值生成器来连贯地改变可组合函数的参数值

ProfileScreen 使用`rememberInfiniteTransition()`来实现这种效果。InfiniteTransition 通过 animateFloat、animateValue、animateColor 等方式来保存子动画，这些动画一进入组合阶段就开始运行，除非被移除，否则不会停止。再为 InfiniteTransition 指定一个初始值和一个结束值，并指定动画以反转的形式来回运行，animateValue 就会在这两个值之间不断地连贯变化，之后将 animateValue 应用到背景图的布局参数上即可实现动画效果

```kotlin
val animateValue by rememberInfiniteTransition().animateFloat(
    initialValue = 1.3f, targetValue = 1.9f,
    animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse,
    ),
)
NetworkImage(
    data = userFaceUrl,
    modifier = Modifier
        .constrainAs(ref = background) {

        }
        .fillMaxWidth()
        .aspectRatio(ratio = 5f / 4f)
        .scrim(colors = listOf(Color(0x40000000), Color(0x40F4F4F4)))
        .clip(shape = BezierShape(padding = animateValue * 100)) //裁切
        .scale(scale = animateValue) //缩放
        .rotate(degrees = animateValue * 10.3f) //旋转
)
```

## 手势操作 

Compose 中的 Modifier 十分强大，不仅仅是用于进行布局，像点击事件、手势操作等一样需要依靠其来完成，pointerInput 函数就用于识别用户的手势操作，Modifier 同时提供了 offset 函数用于控制控件的偏移量，通过结合 pointerInput 和 offset  两个函数来动态改变控件的偏移量，就可以实现拖拽用户头像了

拖拽 OutlinedAvatar 的过程中系统会不断回调 onDrag 函数，在回调里通过用户的拖拽值不断改变 offsetX 和 offsetY 两个值，就可以不断触发 OutlinedAvatar 进行重组，以此实现拖拽效果。当用户松手时，onDragEnd 函数会被回调，再通过 Animatable 将 OutlinedAvatar 的偏移量重置为零，这样就可以实现自动移回原位的效果了

```kotlin
val coroutineScope = rememberCoroutineScope()
var offsetX by remember { mutableStateOf(0f) }
var offsetY by remember { mutableStateOf(0f) }
OutlinedAvatar(
    data = userFaceUrl,
    modifier = Modifier
        .offset {
            IntOffset(
                x = offsetX.roundToInt(),
                y = offsetY.roundToInt()
            )
        }
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = {

                },
                onDragCancel = {

                },
                onDragEnd = {
                    coroutineScope.launch {
                        Animatable(
                            initialValue = Offset(offsetX, offsetY),
                            typeConverter = Offset.VectorConverter
                        ).animateTo(
                            targetValue = Offset(x = 0f, y = 0f),
                            animationSpec = SpringSpec(dampingRatio = Spring.DampingRatioHighBouncy),
                            block = {
                                offsetX = value.x
                                offsetY = value.y
                            }
                        )
                    }
                },
                onDrag = { change, dragAmount ->
                    change.consumeAllChanges()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                },
            )
        }
)
```

# 十、主题

在以前，Android 应用实现多主题 Theme 切换时都需要声明多个 XML 文件，例如在实现夜间模式时就需要两套 colors.xml 和 styles.xml，这种机制在性能上不能说低，但在易用性上的确不高

Compose 的 Theme 就比较优秀了，完全基于 Kotlin 语言来实现，避免了原生实现方式的那种割裂感，相对原生的实现方式在性能和易用性上都提升了很多。Compose 提供了 MaterialTheme 这一种基于 Material Design 风格样式的主题，`androidx.compose.material` 包内提供的所有控件都遵循 MaterialTheme 进行设计，保证了整个应用统一的风格样式

当使用 Android Studio 创建一个 Compose 工程时，会自动在 `ui.theme` 包目录下创建以下四个文件，当中就默认提供了 Dark 和 Light 两种主题样式

![](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/4a03f06817fc49d89ae4bba6f39bffe8~tplv-k3u1fbpfcp-watermark.image)

DarkColorPalette 和 LightColorPalette 分别定义了在夜间模式和日间模式下使用的颜色值，通过选取不同的 colors 对象传给 MaterialTheme 就可以实现不同的主题样式

```kotlin
private val DarkColorPalette = darkColors(
    primary = Purple200,
    primaryVariant = Purple700,
    secondary = Teal200
)

private val LightColorPalette = lightColors(
    primary = Purple500,
    primaryVariant = Purple700,
    secondary = Teal200
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable() () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }
    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
```

MaterialTheme 由颜色 colors、排版 typography、形状 shapes 共同组成，当自定义这些属性后，所做的更改会自动反映在所有用来构建应用的组件中。MaterialTheme 会将不同的配置项映射保存为应用的环境变量，例如我们传入的 colors 就保存为了静态常量 LocalColors

```kotlin
internal val LocalColors = staticCompositionLocalOf { lightColors() }

@Composable
fun MaterialTheme(
    colors: Colors = MaterialTheme.colors,
    typography: Typography = MaterialTheme.typography,
    shapes: Shapes = MaterialTheme.shapes,
    content: @Composable () -> Unit
) {
    val rememberedColors = remember {
        // Explicitly creating a new object here so we don't mutate the initial [colors]
        // provided, and overwrite the values set in it.
        colors.copy()
    }.apply { updateColorsFrom(colors) }
    val rippleIndication = rememberRipple()
    val selectionColors = rememberTextSelectionColors(rememberedColors)
    CompositionLocalProvider(
        LocalColors provides rememberedColors,
        LocalContentAlpha provides ContentAlpha.high,
        LocalIndication provides rippleIndication,
        LocalRippleTheme provides MaterialRippleTheme,
        LocalShapes provides shapes,
        LocalTextSelectionColors provides selectionColors,
        LocalTypography provides typography
    ) {
        ProvideTextStyle(value = typography.body1, content = content)
    }
}
```

Compose 中提供的各种“控件”函数默认都会来读取 LocalColors 中的颜色值来绘制自身，例如 Surface 默认就以 `MaterialTheme.colors.surface`作为背景色

```kotlin
@Composable
fun Surface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(color),
    border: BorderStroke? = null,
    elevation: Dp = 0.dp,
    content: @Composable () -> Unit
)
```

以 compose_chat 为例，一共提供了三套主题：Light、Dark、Pink

<img src="https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/2156aaef2d2b4fe882e51976d93c0685~tplv-k3u1fbpfcp-watermark.image" style="zoom:80%;" />

三种主题类型就对应枚举类 AppTheme 和三套颜色值，根据当前选中的主题类型传给 MaterialTheme 不同的颜色值即可

```kotlin
private val LightColorPalette = lightColors(
    background = BackgroundColorLight,
    primary = PrimaryColorLight,
    primaryVariant = PrimaryVariantColorLight,
    surface = SurfaceColorLight,
    secondary = DivideColorLight,
)

private val DarkColorPalette = darkColors(
    background = BackgroundColorDark,
    primary = PrimaryColorDark,
    primaryVariant = PrimaryVariantColorDark,
    surface = SurfaceColorDark,
    secondary = DivideColorDark,
)

private val PinkColorPalette = lightColors(
    background = BackgroundColorPink,
    primary = PrimaryColorPink,
    primaryVariant = PrimaryVariantColorPink,
    surface = SurfaceColorPink,
    secondary = DivideColorPink,
)

@Composable
fun ChatTheme(
    appTheme: AppTheme = AppThemeHolder.currentTheme,
    content: @Composable () -> Unit
) {
    val colors = when (appTheme) {
        AppTheme.Light -> {
            LightColorPalette
        }
        AppTheme.Dark -> {
            DarkColorPalette
        }
        AppTheme.Pink -> {
            PinkColorPalette
        }
    }
    val typography = if (appTheme.isDarkTheme()) {
        DarkTypography
    } else {
        LightTypography
    }
    MaterialTheme(
        colors = colors,
        typography = typography,
        shapes = AppShapes,
        content = content
    )
}
```

Compose 的主题切换也依赖于可组合函数的重组操作。以 compose_chat  为例，HomeScreen 内部整体是包裹在 ChatTheme 中的，当切换了应用主题，即改变了 appTheme 时，就会触发 ChatTheme 和 HomeScreen 进行重组，重组过程就会读取到最新的主题配置，从而实现了主题切换

```kotlin
@Composable
fun HomeScreen(
    navController: NavHostController,
    screenSelected: ViewScreen,
    onTabSelected: (ViewScreen) -> Unit
) {
    val homeViewModel = viewModel<HomeViewModel>()
    val appTheme by homeViewModel.appTheme.collectAsState()
    ChatTheme(appTheme = appTheme) {
        
    }
}
```

可以看出来，Compose 的主题切换是完全依赖于内存读写的，避免了原生 Android 方式还需要通过  IO 流去读取 XML 文件的情况，在执行效率上相对会高很多，而且在定义主题时也十分方便，仅需要多声明一种 Colors 对象即可实现，类型安全且有助于减少代码量

# 十一、结尾

关于 Jetpack Compose 的大部分知识点都讲完了，自我感觉 compose_chat 能很好的帮助读者入门，最后当然也少不了源码了

项目地址：[compose_chat](https://github.com/leavesCZY/compose_chat)

APK 下载尝鲜：[compose_chat](https://github.com/leavesCZY/compose_chat/releases)

由于腾讯云 IM SDK 免费版最多只能注册一百个账号，因此读者如果发现注册不了的话，可以使用以下几个我预先注册好的账号，但多设备同时登陆的话会互相挤掉线 ~~

- Google
- Android
- Compose
- Flutter
- Java
- Kotlin
- Dart
- Jetpack
- ViewModel
- LiveData

# 十二、参考

- https://developer.android.google.cn/jetpack/compose/mental-model