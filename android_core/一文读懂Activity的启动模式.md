> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

Activity 的启动模式本身是一个挺难理解的知识点，大多数开发者对这个概念的了解可能只限于四种 launchMode 属性值，但启动模式其实还需要受 Intent flag 的影响。而且 Activity 启动模式并不只是单纯地用来启动一个 Activity，实际上还会直接影响到用户的直观感受和使用体验，因为启动模式直接就决定了应用的任务栈和返回栈，这都是用户能直接接触到的

本篇文章就来简单介绍下 Activity 的启动模式，希望对你有所帮助 🤣🤣

# 1、任务栈

**任务栈**是指用户在执行某项工作时与之互动的一系列 Activity 的集合，这些 Activity 按照打开的顺序排列在一个后进先出的栈中。例如，电子邮件应用包含一个 Activity 来显示邮件列表，当用户选择一封邮件时就会打开一个新的 Activity 来显示邮件详情，这个新的 Activity 就会添加到任务栈中，被推送到栈顶部并获得焦点，从而获得了与用户进行交互的机会。当用户按返回键时，邮件详情 Activity 就会从任务栈中退出并被销毁，邮件列表 Activity 就会成为新的栈顶并重新获得焦点

任务栈代表的是一个整体，本身包含了多个 Activity，当任务栈中的所有 Activity 都被弹出后，任务栈也就随之就被回收了。就像下图所示，三个 Activity 通过相继启动组成了一个任务栈，Activity 1 是整个任务栈的根 Activity，当用户不断按返回键，Activity 就会依次被弹出

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/4f2312098b3d4fb28c3bbd272beb85bb~tplv-k3u1fbpfcp-zoom-1.image)

# 2、返回栈

**返回栈**是从用户使用的角度来进行定义的，返回栈中包含一个或多个任务栈，但同时只会有一个任务栈够处于前台，只有处于前台任务栈的 Activity 才能与用户交互

例如，用户先启动了应用 A，先后打开了 Activity 1 和 Activity 2，此时 Task A 是前台任务栈。之后用户又点击 Home 键回到了桌面，启动了应用 B，又先后打开了 的 Activity 3 和 Activity 4，此时 Task B 就成为了前台任务栈，Task A 成了后台任务栈。用户点击返回键的过程中依次展现的页面就会是 Activity 4 -> Activity 3 -> 桌面

而如果用户在打开应用 B 时并没有回到桌面，而是直接通过应用 A 启动了应用 B 的话，用户点击返回键的过程中依次展现的页面就会是 Activity 4 -> Activity 3 -> Activity 2 -> Activity 1 -> 桌面

返回栈所表示的就是当用户不断回退页面时所能看到的一系列 Activity 的集合，而这些页面可能是处于多个不同的任务栈中。在第一种情况中，返回栈只包含 Task B 一个任务栈，所以当 Task B 被清空后就会直接回到桌面。在第二种情况中，返回栈中包含 Task A 和 Task B 两个任务栈，所以当 Task B 被清空后也会先切回到 Task A，等到 Task A 也被清空后才会回到桌面

需要注意的是，返回栈中包含的多个任务栈之间并没有强制的先后顺序，多个任务栈之间的叠加关系可以随时发现变化。例如，当应用 A 启动了应用 B 后，Task B 是处于 Task A 之上，但之后如果应用 B 又反向启动了应用 A 的话，Task A 就会重新成为前台 Task 并覆盖在 Task B 之上

# 3、taskAffinity

返回栈这个概念对应的就是 taskAffinity，是 Activity 在 AndroidManifest 文件中声明的一个属性值。taskAffinity 翻译为“亲和性”，用于表示特定 Activity 倾向于将自身存放在哪个任务栈中

在默认情况下，同一应用中的所有 Activity 会具有相同的亲和性，所有 Activity 默认会以当前应用的 applicationId 作为自己的 taskAffinity 属性值。我们可以手动为应用内的部分 Activity 指定特定的 taskAffinity，从而将这部分 Activity 进行分组

```xml
<activity
    android:name=".StandardActivity"
    android:launchMode="standard"
    android:taskAffinity="task.test1" />
<activity
    android:name=".SingleTopActivity"
    android:launchMode="singleTop"
    android:taskAffinity="task.test2" />
<activity
    android:name=".SingleTaskActivity"
    android:launchMode="singleTask"
    android:taskAffinity="task.test3" />
<activity
    android:name=".SingleInstanceActivity"
    android:launchMode="singleInstance"
    android:taskAffinity="task.test4" />
```

从概念上讲，具有相同 taskAffinity 的 Activity 归属于同一任务栈（实际上并不一定）。从用户的角度来看则是归属于同一“应用”，因为每种 taskAffinity 在最近任务列表中会各自独占一个列表项，看起来就像一个个单独的应用，而实际上这些列表项可能是来自于同个应用

# 4、启动模式

Activity 的启动模式是一个很复杂的难点，其决定了要启动的 Activity 和任务栈还有返回栈之间的关联关系，直接影响到了用户的直观感受

启动模式就由 launchMode 和 Intent flag 这两者来共同决定，我们可以通过两种方式来进行定义：

- 在 AndroidManifest 文件中为 Activity 定义 launchMode 属性值，一共包含四种类型的属性值
- 当通过 `startActivity(Intent)` 启动 Activity 时，向 `Intent` 添加或设置 flag 标记位，通过该 flag 来定义启动模式

如果只看四个 launchMode 的话其实并不难理解，可是再考虑多应用交互还有 Intent flag 的话，情况就会变得复杂很多，其复杂性和难点主要就在于：单个任务栈包含的 Activity 可以是来自于不同的应用、单个应用也可以包含多个任务栈、返回栈包含的多个任务栈之间也可以进行顺序切换、甚至任务栈中的 Activity 也可以被迁移到另外一个任务栈、Intent flag 可以多个组合使用

有些启动模式可通过 launchMode 来定义，但不能通过 Intent flag 定义，同样，有些启动模式可通过 Intent flag 定义，却不能在 launchMode 中定义。两者互相补充，但不能完全互相替代，且 Intent flag 的优先级会更高一些

# 5、launchMode

launchMode 一共包含以下四种属性值：

- standard。默认模式。系统会在启动该 Activity 的任务栈中创建一个目标 Activity 的新实例，使该目标 Activity 成为任务栈的栈顶。该模式下允许先后启动多个相同的目标 Activity，一个任务栈可以拥有多个目标 Activity 实例，且不同 Activity 实例可以属于不同的任务栈
- singleTop。如果当前任务栈的顶部已存在目标 Activity 的实例，则系统会通过调用其 `onNewIntent()` 方法来将 Intent 转送给该实例并进行复用，否则会创建一个目标 Activity 的新实例。目标 Activity 可以多次实例化，不同实例可以属于不同的任务栈，一个任务栈可以拥有多个实例（此时多个实例不会连续叠放在一起）
- singleTask。如果系统当前不包含目标 Activity 的目标任务栈，那么系统就会先创建出目标任务栈，然后实例化目标 Activity 使之成为任务栈的根 Activity。如果系统当前包含目标任务栈，且该任务栈中已存在该目标 Activity 的实例，则系统会通过调用其 `onNewIntent()` 方法将 Intent 转送给该现有实例，而不会创建新实例，并同时弹出该目标 Activity 之上的所有其它实例，使目标 Activity 成为栈顶。如果系统当前包含目标任务栈，但该任务栈不包含目标 Activity 实例，则会实例化目标 Activity 并将其入栈。因此，系统全局一次只能有一个目标 Activity 实例存在
- singleInstance。与 `singleTask` 相似，唯一不同的是通过 singleInstance 启动的 Activity 会独占一个任务栈，系统不会将其和其它 Activity 放置到同个任务栈中，由该 Activity 启动的任何 Activity 都会在其它的任务栈中打开

四种 launchMode 还是很好理解的，当中比较特殊的应该属 singleTask，使用 singleTask 标记的 Activity 会有将自己存放在特定任务栈的倾向。如果目标任务栈和目标 Activity 都已经存在，则会进行复用，否则才会创建目标任务栈和目标 Activity。singleInstance 则是在 singleTask 的基础上多了一个“独占任务栈”的特性

采用 `singleTask` 启动的 Activity 添加到返回栈的过程就如下图所示。一开始返回栈中只包含 Activity 1 和 Activity 2 组成的任务栈，当 Activity 2 启动了处于后台的 Activity Y 时，Activity Y 和 Activity X 组成的任务栈就会被转到前台，覆盖住当前任务栈。最终返回栈中就变成了四个 Activity

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/3afd90bba8ee4b1187de27622f25cead~tplv-k3u1fbpfcp-zoom-1.image)

再来写个 Demo 来验证下这四种 launchMode 的效果

声明四种不同 launchMode 的 Activity，每个 Activity 均声明了不同的 taskAffinity

```xml
<activity
    android:name=".StandardActivity"
    android:launchMode="standard"
    android:taskAffinity="task.a" />
<activity
    android:name=".SingleTopActivity"
    android:launchMode="singleTop"
    android:taskAffinity="task.b" />
<activity
    android:name=".SingleTaskActivity"
    android:launchMode="singleTask"
    android:taskAffinity="task.c" />
<activity
    android:name=".SingleInstanceActivity"
    android:launchMode="singleInstance"
    android:taskAffinity="task.d" />
```

通过打印 Activity 的 `hashCode()` 方法返回值来判断 Activity 的实例是否被复用了，再通过 `getTaskId()` 方法来判断 Activity 处于哪个任务栈中

```kotlin
/**
 * @Author: leavesCZY
 * @Desc:
 * @公众号：字节数组
 */
abstract class BaseLaunchModeActivity : BaseActivity() {

    override val bind by getBind<ActivityBaseLaunchModeBinding>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind.tvTips.text =
            getTip() + "\n" + "hashCode: " + hashCode() + "\n" + "taskId: " + taskId
        bind.btnStartStandardActivity.setOnClickListener {
            startActivity(StandardActivity::class.java)
        }
        bind.btnStartSingleTopActivity.setOnClickListener {
            startActivity(SingleTopActivity::class.java)
        }
        bind.btnStartSingleTaskActivity.setOnClickListener {
            startActivity(SingleTaskActivity::class.java)
        }
        bind.btnStartSingleInstanceActivity.setOnClickListener {
            startActivity(SingleInstanceActivity::class.java)
        }
        log("onCreate")
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        log("onNewIntent")
    }

    override fun onDestroy() {
        super.onDestroy()
        log("onDestroy")
    }

    abstract fun getTip(): String

    private fun log(log: String) {
        Log.e(getTip(), log + " " + "hashCode: " + hashCode() + " " + "taskId: " + taskId)
    }

}

class StandardActivity : BaseLaunchModeActivity() {

    override fun getTip(): String {
        return "StandardActivity"
    }

}
```

四个 Activity 相继互相启动，查看输出的日志，可以看出 SingleTaskActivity 和 SingleInstanceActivity 均处于独立的任务栈中，而 StandardActivity 和 SingleTopActivity 处于同个任务栈中。说明 taskAffinity 对于 standard 和 singleTop 这两种模式不起作用

```kotlin
E/StandardActivity: onCreate hashCode: 31933912 taskId: 37
E/SingleTopActivity: onCreate hashCode: 95410735 taskId: 37
E/SingleTaskActivity: onCreate hashCode: 255733510 taskId: 38
E/SingleInstanceActivity: onCreate hashCode: 20352185 taskId: 39
```

再依次启动 SingleTaskActivity 和 SingleTopActivity。可以看到 SingleTaskActivity 被复用了，且在 38 这个任务栈上启动了一个新的 SingleTopActivity 实例。之所以没有复用 SingleTopActivity，是因为之前的 SingleTopActivity 是在 37 任务栈中，并非当前任务栈

```kotlin
E/SingleTaskActivity: onNewIntent hashCode: 255733510 taskId: 38
E/SingleTopActivity: onCreate hashCode: 20652250 taskId: 38
```

再启动一次 SingleTopActivity，两次 StandardActivity。可以看到 SingleTopActivity 的确在当前任务栈中被复用了，并均创建了两个新的 StandardActivity 实例。说明 singleTop 想要被复用需要当前任务栈的栈顶就是目标 Activity，而 standard 模式每次均会创建新实例

```kotlin
E/SingleTopActivity: onNewIntent hashCode: 20652250 taskId: 38
E/StandardActivity: onCreate hashCode: 252563788 taskId: 38
E/StandardActivity: onCreate hashCode: 25716630 taskId: 38
```

再依次启动 SingleTaskActivity 和 SingleInstanceActivity。可以看到 SingleTaskActivity 和 SingleInstanceActivity 均被复用了，且 SingleTaskActivity 之上的三个 Activity 均从任务栈中被弹出销毁了，SingleTaskActivity 成为了 task 38 新的栈顶 Activity

```kotlin
E/StandardActivity: onDestroy hashCode: 252563788 taskId: 38
E/SingleTopActivity: onDestroy hashCode: 20652250 taskId: 38
E/SingleTaskActivity: onNewIntent hashCode: 255733510 taskId: 38
E/StandardActivity: onDestroy hashCode: 25716630 taskId: 38
E/SingleInstanceActivity: onNewIntent hashCode: 20352185 taskId: 39
```

再依次启动 StandardActivity 和 SingleTopActivity。可以看到创建了一个新的任务栈，且启动的是两个新的 Activity 实例。由于 SingleInstanceActivity 所在的任务栈只会由其自身所独占，所以 StandardActivity 启动时就需要创建一个新的任务栈用来容纳自身

```kotlin
E/StandardActivity: onCreate hashCode: 89641200 taskId: 40
E/SingleTopActivity: onCreate hashCode: 254021317 taskId: 40
```

可以做个总结：

- standard 和 singleTop 这两种模式下 taskAffinity 属性均不会生效，这两种模式启动的 Activity 总会尝试加入到启动者所在的任务栈中，如果启动者是 singleInstance 的话则会创建一个新的任务栈
- standard 模式的 Activity 每次启动都会创建一个新的实例，不会考虑任何复用
- singleTop 模式的 Activity 想要被复用，需要启动者所在的任务栈的栈顶就是该 Activity 实例
- singleTask 模式的 Activity 事实上是系统全局单例，只要实例没有被回收就会一直被复用。singleTask 可以通过声明 taskAffinity 从而在一个特定的任务栈中被启动，且允许其它 Activity 一起共享同一个任务栈。如果不声明 taskAffinity 的话就会尝试寻找或者主动创建 taskAffinity 为 applicationId 的任务栈，然后在该任务栈中创建或复用 Activity
- singleInstance 可以看做是 singleTask 的加强版，singleInstance 在任何时候都会独占一个任务栈，不管是否声明了 taskAffinity。在 singleInstance 任务栈中启动的其它 Activity 都会加入到其它任务栈中

需要注意的是，以上结论只适用于没有主动添加 Intent flag 的情况，如果同时添加了 Intent flag 的话就会出现很多奇奇怪怪的现象了

# 6、Intent flag

在启动 Activity 时，我们可以通过在传送给 `startActivity(Intent)` 方法的 Intent 中设置多个相应的 flag 来修改 Activity 与其任务栈的默认关联，即 Intent flag 的优先级会比 launchMode 高

Intent 提供的设置 flag 的方法有以下两个，一个是覆盖设置，一个是增量添加

```java
private int mFlags;

public @NonNull Intent setFlags(@Flags int flags) {
    mFlags = flags;
    return this;
}

public @NonNull Intent addFlags(@Flags int flags) {
    mFlags |= flags;
    return this;
}
```

通过如下方式来添加 flag 并启动 Activity

```kotlin
val intent = Intent(this, StandardActivity::class.java)
intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
startActivity(intent)
```

如果 Activity 的启动模式只由 launchMode 定义的话，那么在运行时 Activity 的启动模式就再也无法改变了，相当于被写死了，所以 launchMode 适合于那些具有固定情景的业务。而 Intent flag 存在的意义就是为了改变或者补充 launchMode，适合于那些大部分情况下固定，少数情况下需要动态进行变化的场景，例如在某些情况下不希望 singleInstance 模式的 Activity 被重用，此时就可以通过 Intent flag 来动态实现

而这也造成了 Intent flag 很难理清楚逻辑，因为 Intent flag 往往需要组合使用，且还需要考虑和 launchMode 的各种组合配置，两者并不是简单的进行替换

Intent flag 有很多个，比较常见的有四个，这里就简单介绍下这几种 Intent flag

- FLAG_ACTIVITY_NEW_TASK 
- FLAG_ACTIVITY_SINGLE_TOP
- FLAG_ACTIVITY_CLEAR_TOP
- FLAG_ACTIVITY_CLEAR_TASK

## FLAG_ACTIVITY_NEW_TASK 

FLAG_ACTIVITY_NEW_TASK 应该是大多数开发者最熟悉的一个 flag，比较常用的一个场景就是用于在非 ActivityContext 环境下启动 Activity。Android 系统默认情况下是会将待启动的 Activity 加入到启动者所在的任务栈，而如果启动 Activity 的是 ServiceContext 的话，此时系统就不确定该如何存放目标 Activity 了，此时就会抛出一个 RuntimeException

```kotlin
java.lang.RuntimeException: Unable to start service github.leavesc.launchmode.MyService@e3183b7 with Intent { cmp=github.leavesc.demo/github.leavesc.launchmode.MyService }: android.util.AndroidRuntimeException: Calling startActivity() from outside of an Activity  context requires the FLAG_ACTIVITY_NEW_TASK flag. Is this really what you want?
```

从异常信息可以看出此时 Intent 需要添加一个 FLAG_ACTIVITY_NEW_TASK 才行，添加后 Activity 就可以正常启动了

```kotlin
val intent = Intent(this, StandardActivity::class.java)
intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
startActivity(intent)
```

FLAG_ACTIVITY_NEW_TASK 也有一个隐含的知识点，上文有讲到 **standard 和 singleTop 这两种模式下 taskAffinity 属性均不会生效**，但这个结论也只适用于没有主动添加 Intent flag 的情况

FLAG_ACTIVITY_NEW_TASK 和 standard 模式的组合情况可以总结为：

- standard 没有设置 taskAffinity。此时系统就会去复用或者创建一个默认任务栈，然后直接在该任务栈上新建一个 Activity 实例入栈
- standard 有设置 taskAffinity。此时又可以分为当前系统是否存在 taskAffinity 关联的任务栈两种情况
  - 不存在目标任务栈。此时系统就会创建目标任务栈，然后直接在该任务栈上新建一个 Activity 实例入栈
  - 存在目标任务栈。此时系统会判断任务栈中是否已经存在目标 Activity 的实例，如果不存在的话则新建一个 Activity 实例入栈。如果存在目标实例的话，则只是将该任务栈转到前台而已，既不会新建 Activity 实例，也不会回调 `onNewIntent`方法，甚至也不管该 Activity 实例是否处于栈顶，总之只要存在相同实例就不做任何响应。。。。。。。

可以看到，FLAG_ACTIVITY_NEW_TASK 的语义还是有点费解的，该标记位可以使得 taskAffinity 生效，创建或者复用任务栈并将其转到前台，但并不要求必须创建一个新的 Activity 实例，而是只要 Activity 实例有存在即可，而且也无需该 Activity 实例就在栈顶

## FLAG_ACTIVITY_SINGLE_TOP

FLAG_ACTIVITY_SINGLE_TOP 这个 flag 看名字就很容易和 singleTop 联系在一起，实际上该 flag 也的确起到了和 singleTop 相同的作用

只要待启动的 Activity 添加了该标记位，且当前任务栈的栈顶就是目标 Activity，那么该 Activity 实例就会被复用，并且回调其`onNewIntent`方法，即使该 Activity 声明了 standard 模式，这相当于将 Activity 的 launchMode 覆盖为了 singleTop

## FLAG_ACTIVITY_CLEAR_TOP

FLAG_ACTIVITY_CLEAR_TOP 这个 flag 则是起到了清除目标 Activity 之上所有 Activity 的作用。例如，假设当前要启动的 Activity 已经在目标任务栈中了，那么设置该 flag 后系统就会清除目标 Activity 之上的所有其它 Activity，但系统最终并不一定会复用现有的目标 Activity 实例，有可能是销毁后再次创建一个新的实例

看个例子。先以不携带 flag 的方式启动 StandardActivity 和 SingleTopActivity，此时日志信息如下

```kotlin
E/StandardActivity: onCreate hashCode: 76763823 taskId: 39
E/SingleTopActivity: onCreate hashCode: 217068130 taskId: 39
```

再启动一次 StandardActivity，此时就带上 FLAG_ACTIVITY_CLEAR_TOP。此时就会看到最开始启动的两个 Activity 都会销毁了，并且再次新建了一个 StandardActivity 实例入栈

```kotlin
E/StandardActivity: onDestroy hashCode: 76763823 taskId: 39
E/StandardActivity: onCreate hashCode: 51163106 taskId: 39
E/SingleTopActivity: onDestroy hashCode: 217068130 taskId: 39
```

而如果同时加上 FLAG_ACTIVITY_SINGLE_TOP 和 FLAG_ACTIVITY_CLEAR_TOP 两个 flag 的话，那么 SingleTopActivity 就会被弹出，StandardActivity 会被复用，并且回调其`onNewIntent`方法，两个 flag 相当于组合出了 singleTask 的效果。这一个效果读者可以自行验证

## FLAG_ACTIVITY_CLEAR_TASK

FLAG_ACTIVITY_CLEAR_TASK 的源码注释标明了该 flag 必须和 FLAG_ACTIVITY_NEW_TASK 组合使用，它起到的作用就是将目标任务栈中的所有 Activity 情空，然后新建一个目标 Activity 实例入栈，该 flag 的优先级很高，即使是 singleInstance 类型的 Activity 也会被销毁

看个例子。先启动一个 SingleInstanceActivity，然后以添加了 NEW_TASK 和 CLEAR_TASK 两个 flag 的方式再次启动 SingleInstanceActivity，可以看到旧的 Activity 实例被销毁了，并重建了一个新实例入栈，但比较奇怪的一点就是：旧的 Activity 实例的 `onNewIntent` 方法同时也被调用了

```kotlin
E/SingleInstanceActivity: onCreate hashCode: 144724929 taskId: 47
E/SingleInstanceActivity: onNewIntent hashCode: 144724929 taskId: 47
E/SingleInstanceActivity: onCreate hashCode: 106721743 taskId: 47
E/SingleInstanceActivity: onDestroy hashCode: 144724929 taskId: 47
```

# 7、总结

关于 Activity 的启动模式的讲解到这里就结束了，最后再强调一遍，launchMode 和 Intent flag 的各种组合效果还是有点过于难理解了，使得我很难全面地进行描述，再加上似乎还存在版本兼容性问题，使用起来就更加麻烦了，所以我觉得开发者只需要有个大致的印象即可，当真正要使用的时候再来亲自测试验证效果就好，不必强行记忆

以上各个示例 Demo 点这里：[AndroidOpenSourceDemo](https://github.com/leavesCZY/AndroidOpenSourceDemo)

# 8、参考资料

- [了解任务和返回堆栈](https://developer.android.com/guide/components/activities/tasks-and-back-stack?hl=zh-cn)