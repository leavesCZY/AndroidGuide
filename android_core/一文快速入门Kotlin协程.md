> 公众号：[字节数组](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/0357ed9ee08d4a5d92af66a72b002169~tplv-k3u1fbpfcp-watermark.image)，希望对你有所帮助 🤣🤣

在今年的三月份，我因为需要为项目搭建一个新的网络请求框架开始接触 Kotlin 协程。那时我司项目中同时存在着两种网络请求方式，采用的技术栈各不相同，Java、Kotlin、RxJava、LiveData 各种混搭，技术栈的不统一长远来看肯定是会造成很多不便的，所以当时就打算封装一个新的网络请求框架来作为项目的统一规范（前面的人估计也是这么想的，所以就造成了同个项目中的网络请求方式越来越多😂😂），那么就需要考虑采用什么技术栈来实现了

采用 Kotlin 语言来实现必不可少，都这年头了还用 Java 也说不过去。Retrofit 也必不可少，而当时 Retrofit 也已经原生支持 Kotlin 协程了，Google 官方推出的 Jetpack 协程扩展库也越来越多，就最终决定弃用 RxJava 拥抱 Kotlin 协程，将协程作为技术栈之一

当时我是通过翻译协程官方文档来作为入门手段，到现在也大半年了，回头来看感觉官方文档还是挺晦涩难懂的，就想着再来写一两篇入门或进阶的文章来加深下理解，希望对你有所帮助

附上我当时翻译的协程官方文档：

- [Kotlin 协程官方文档（1）-协程基础（Coroutine Basics）](https://juejin.cn/post/6844903972755472391)
- [Kotlin 协程官方文档（2）-取消和超时（Cancellation and Timeouts）](https://juejin.cn/post/6844904098899181582)
- [Kotlin 协程官方文档（3）-组合挂起函数（Coroutine Context and Dispatchers）](https://juejin.cn/post/6844904100102930445)
- [Kotlin 协程官方文档（4）-协程上下文和调度器（Coroutine Context and Dispatchers）](https://juejin.cn/post/6844904100103094280)
- [Kotlin 协程官方文档（5）-异步流（Asynchronous Flow）](https://juejin.cn/post/6844904101801639949)
- [Kotlin 协程官方文档（6）-通道（Channels）](https://juejin.cn/post/6844904102040698893)
- [Kotlin 协程官方文档（7）-异常处理（Exception Handling）](https://juejin.cn/post/6844904103080886285)
- [Kotlin 协程官方文档（8）-共享可变状态和并发性（Shared mutable state and concurrency）](https://juejin.cn/post/6844904104053964808)
- [Kotlin 协程官方文档（9）-选择表达式(实验阶段)（Select Expression (experimental)](https://juejin.cn/post/6844904106788667400)

# 一、Kotlin 协程

Kotlin [协程](https://links.jianshu.com/go?to=https%3A%2F%2FKotlinlang.org%2Fdocs%2Freference%2Fcoroutines-overview.html)提供了一种全新处理并发的方式，你可以在 Android 平台上使用它来简化异步执行的代码。协程从 Kotlin 1.3 版本开始引入，但这一概念在编程世界诞生的黎明之际就有了，最早使用协程的编程语言可以追溯到 1967 年的 [Simula](https://links.jianshu.com/go?to=https%3A%2F%2Fen.wikipedia.org%2Fwiki%2FSimula) 语言。在过去几年间，协程这个概念发展势头迅猛，现已经被诸多主流编程语言采用，比如 [Javascript](https://links.jianshu.com/go?to=https%3A%2F%2Fjavascript.info%2Fasync-await)、[C#](https://links.jianshu.com/go?to=https%3A%2F%2Fdocs.microsoft.com%2Fen-us%2Fdotnet%2Fcsharp%2Fprogramming-guide%2Fconcepts%2Fasync%2F)、[Python](https://links.jianshu.com/go?to=https%3A%2F%2Fdocs.python.org%2F3%2Flibrary%2Fasyncio-task.html)、[Ruby](https://links.jianshu.com/go?to=https%3A%2F%2Fruby-doc.org%2Fcore-2.1.1%2FFiber.html) 以及 [Go](https://links.jianshu.com/go?to=https%3A%2F%2Ftour.golang.org%2Fconcurrency%2F1) 等。Kotlin 协程是基于来自其他语言的既定概念

Goggle 官方推荐将 Kotlin 协程作为在 Android 上进行异步编程的解决方案，值得关注的功能点包括：

- **轻量**：你可以在单个线程上运行多个协程，因为协程支持[挂起](https://Kotlinlang.org/docs/reference/coroutines/basics.html)，不会使正在运行协程的线程阻塞。挂起比阻塞节省内存，且支持多个并行操作
- **内存泄露更少**：使用[结构化并发](https://Kotlinlang.org/docs/reference/coroutines/basics.html#structured-concurrency)机制在一个作用域内执行多个操作
- **内置取消支持**：[取消](https://Kotlinlang.org/docs/reference/coroutines/cancellation-and-timeouts.html)功能会自动通过正在运行的协程层次结构传播
- **Jetpack 集成**：许多 Jetpack 库都包含提供全面协程支持的[扩展](https://developer.android.google.cn/Kotlin/ktx)。某些库还提供自己的[协程作用域](https://developer.android.google.cn/topic/libraries/architecture/coroutines)，可供你用于结构化并发

引入依赖：

```kotlin
    implementation 'org.jetbrains.Kotlinx:Kotlinx-coroutines-core:1.4.2'
    implementation 'org.jetbrains.Kotlinx:Kotlinx-coroutines-android:1.4.2'
```

# 二、第一个协程

协程可以称为**轻量级线程**。Kotlin 协程在 CoroutineScope 的上下文中通过 launch、async 等**协程构造器**（CoroutineBuilder）来声明并启动

```kotlin
fun main() {
    GlobalScope.launch(context = Dispatchers.IO) {
        //延时一秒
        delay(1000)
        log("launch")
    }
    //主动休眠两秒，防止JVM过快退出
    Thread.sleep(2000)
    log("end")
}

private fun log(msg: Any?) = println("[${Thread.currentThread().name}] $msg")
```

```kotlin
[DefaultDispatcher-worker-1 @coroutine#1] launch
[main] end
```

在上面的例子中，通过 GlobalScope（即**全局作用域**）启动了一个协程，在延迟一秒后输出一行日志。从输出结果可以看出来，启动的协程是运行在协程内部的**线程池**中。虽然从表现结果上来看，启动一个协程类似于我们直接使用 Thread 来执行耗时任务，但实际上协程和线程有着本质上的区别。通过使用协程，可以极大的提高线程的并发效率，避免以往的嵌套回调地狱，极大提高了代码的可读性

以上代码就涉及到了协程的四个基础概念：

- suspend function。即挂起函数，delay 函数就是协程库提供的一个用于实现非阻塞式延时的挂起函数
- CoroutineScope。即协程作用域，GlobalScope 是 CoroutineScope 的一个实现类，用于指定协程的作用范围，可用于管理多个协程的生命周期，所有协程都需要通过 CoroutineScope 来启动
- CoroutineContext。即协程上下文，包含多种类型的配置参数。Dispatchers.IO 就是 CoroutineContext 这个抽象概念的一种实现，用于指定协程的运行载体，即用于指定协程要运行在哪类线程上
- CoroutineBuilder。即协程构建器，协程在 CoroutineScope 的上下文中通过 launch、async 等协程构建器来进行声明并启动。launch、async 等均被声明 CoroutineScope 的扩展方法

# 三、suspend function

如果上述例子试图直接在 GlobalScope 外调用 `delay()` 函数的话，IDE 就会提示一个错误：**Suspend function 'delay' should be called only from a coroutine or another suspend function**。意思是：`delay()` 函数是一个挂起函数，只能由协程或者由其它挂起函数来调用

`delay()` 函数就使用了 suspend 进行修饰，用 suspend 修饰的函数就是挂起函数

```kotlin
public suspend fun delay(timeMillis: Long)
```

读者在网上看关于协程的文章的时候，应该经常会看到这么一句话：**挂起函数不会阻塞其所在线程，而是会将协程挂起，在特定的时候才再恢复协程**

对于这句话我的理解是：`delay()` 函数类似于 Java 中的 `Thread.sleep()`，而之所以说 `delay()` 函数是非阻塞的，是因为它和单纯的线程休眠有着本质的区别。协程是运行于线程上的，一个线程可以运行多个（几千上万个）协程。线程的调度行为是由操作系统来管理的，而协程的调度行为是可以由开发者来指定并由编译器来实现的，协程能够细粒度地控制多个任务的执行时机和执行线程，当某个特定的线程上的所有协程被 suspend 后，该线程便可腾出资源去处理其他任务

例如，当在 ThreadA 上运行的 CoroutineA 调用了`delay(1000L)`函数指定延迟一秒后再运行，ThreadA 会转而去执行 CoroutineB，等到一秒后再来继续执行 CoroutineA。所以，ThreadA 并不会因为 CoroutineA 的延时而阻塞，而是能继续去执行其它任务，所以挂起函数并不会阻塞其所在线程，这样就极大地提高了线程的并发灵活度，最大化了线程的利用效率。而如果是使用`Thread.sleep()`的话，线程就只能干等着而不会去执行其它任务，降低了线程的利用效率

# 四、suspend function 的挂起与恢复

协程在常规函数的基础上添加了两项操作用于处理长时间运行的任务。在`invoke`（或 `call`）和`return`之外，协程添加了`suspend`和 `resume`：

- `suspend` 用于暂停执行当前协程，并保存所有局部变量
- `resume` 用于让已暂停的协程从暂停处继续执行

suspend 函数只能由其它 suspend 函数调用，或者是由协程来调用

以下示例展示了一项任务（假设 get 方法是一个网络请求任务）的简单协程实现：

```kotlin
suspend fun fetchDocs() {                             // Dispatchers.Main
    val result = get("https://developer.android.com") // Dispatchers.IO for `get`
    show(result)                                      // Dispatchers.Main
}

suspend fun get(url: String) = withContext(Dispatchers.IO) { /* ... */ }
```

在上面的示例中，`get()` 仍在主线程上被调用，但它会在启动网络请求之前暂停协程。`get()` 主体内通过调用 `withContext(Dispatchers.IO)` 创建了一个在 IO 线程池中运行的代码块，在该块内的任何代码都始终通过 IO 调度器执行。当网络请求完成后，`get()` 会恢复已暂停的协程，使得主线程协程可以直接拿到网络请求结果而不用使用回调来通知主线程。Retrofit 就是以这种方式来实现对协程的支持的

Kotlin 使用堆栈帧管理要运行哪个函数以及所有局部变量。暂停协程时，系统会复制并保存当前的堆栈帧以供稍后使用。恢复时，会将堆栈帧从其保存位置复制回来，然后函数再次开始运行。即使代码可能看起来像普通的顺序阻塞请求，协程也能确保网络请求避免阻塞主线程

在主线程进行的**暂停协程**和**恢复协程**的两个操作，既实现了将耗时任务交由后台线程完成，保障了主线程安全，又以同步代码的方式完成了实际上的多线程异步调用。可以说，在 Android 平台上协程主要就用来解决两个问题：

1. **处理耗时任务 (Long running tasks)**，这种任务常常会阻塞住主线程
2. **保证主线程安全 (Main-safety)** ，即确保安全地从主线程调用任何 suspend 函数

# 五、CoroutineScope 

CoroutineScope 即**协程作用域**，用于对协程进行追踪。如果我们启动了多个协程但是没有一个可以对其进行统一管理的途径的话，那么就会导致我们的代码臃肿杂乱，甚至发生**内存泄露**或者**任务泄露**。为了确保所有的协程都会被追踪，Kotlin 不允许在没有使用 CoroutineScope 的情况下启动新的协程。CoroutineScope 可被看作是一个具有超能力的 ExecutorService 的轻量级版本。它能启动新的协程，同时这个协程还具备上文所说的 suspend 和 resume 的优势

所有的协程都需要通过 CoroutineScope 来启动，它会跟踪它使用 `launch` 或 `async` 创建的所有协程，你可以随时调用 `scope.cancel()` 取消正在运行的协程。CoroutineScope 本身并不运行协程，它只是确保你不会失去对协程的追踪，即使协程被挂起也是如此。在 Android 中，某些 KTX 库为某些生命周期类提供了自己的 `CoroutineScope`。例如，`ViewModel` 有 [`viewModelScope`](https://developer.android.google.cn/reference/Kotlin/androidx/lifecycle/package-summary#(androidx.lifecycle.ViewModel).viewModelScope:Kotlinx.coroutines.CoroutineScope)，`Lifecycle` 有 [`lifecycleScope`](https://developer.android.google.cn/reference/Kotlin/androidx/lifecycle/package-summary#lifecyclescope)

CoroutineScope 大体上可以分为三种：

- GlobalScope。即全局协程作用域，在这个范围内启动的协程可以一直运行直到应用停止运行。GlobalScope 本身不会阻塞当前线程，且启动的协程相当于守护线程，不会阻止 JVM 结束运行
- runBlocking。一个顶层函数，和 GlobalScope 不一样，它会阻塞当前线程直到其内部所有相同作用域的协程执行结束
- 自定义 CoroutineScope。可用于实现主动控制协程的生命周期范围，对于 Android 开发来说最大意义之一就是可以避免内存泄露

## 1、GlobalScope

GlobalScope 属于**全局作用域**，这意味着通过 GlobalScope 启动的协程的生命周期只受整个应用程序的生命周期的限制，只要整个应用程序还在运行且协程的任务还未结束，协程就可以一直运行

GlobalScope 不会阻塞其所在线程，所以以下代码中主线程的日志会早于 GlobalScope 内部输出日志。此外，GlobalScope 启动的协程相当于守护线程，不会阻止 JVM 结束运行，所以如果将主线程的休眠时间改为三百毫秒的话，就不会看到 launch A 输出日志

```kotlin
fun main() {
    log("start")
    GlobalScope.launch {
        launch {
            delay(400)
            log("launch A")
        }
        launch {
            delay(300)
            log("launch B")
        }
        log("GlobalScope")
    }
    log("end")
    Thread.sleep(500)
}
```

```kotlin
[main] start
[main] end
[DefaultDispatcher-worker-1 @coroutine#1] GlobalScope
[DefaultDispatcher-worker-3 @coroutine#3] launch B
[DefaultDispatcher-worker-3 @coroutine#2] launch A
```

`GlobalScope.launch` 会创建一个顶级协程，尽管它很轻量级，但在运行时还是会消耗一些内存资源，且可以一直运行直到整个应用程序停止（只要任务还未结束），这可能会导致内存泄露，所以在日常开发中应该谨慎使用 GlobalScope

## 2、runBlocking

也可以使用 runBlocking 这个顶层函数来启动协程，runBlocking 函数的第二个参数即协程的执行体，该参数被声明为 CoroutineScope 的扩展函数，因此执行体就包含了一个隐式的 CoroutineScope，所以在 runBlocking 内部可以来直接启动协程

```kotlin
public fun <T> runBlocking(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> T): T
```

runBlocking 的一个方便之处就是：只有当内部**相同作用域**的所有协程都运行结束后，声明在 runBlocking 之后的代码才能执行，即 runBlocking 会阻塞其所在线程

看以下代码。runBlocking 内部启动的两个协程会各自做耗时操作，从输出结果可以看出来两个协程还是在交叉并发执行，且 runBlocking 会等到两个协程都执行结束后才会退出，外部的日志输出结果有明确的先后顺序。即 runBlocking 内部启动的协程是非阻塞式的，但 runBlocking 阻塞了其所在线程。此外，runBlocking 只会等待相同作用域的协程完成才会退出，而不会等待 GlobalScope 等其它作用域启动的协程

**所以说，runBlocking 本身带有阻塞线程的意味，但其内部运行的协程又是非阻塞的，读者需要意会这两者的区别**

```kotlin
fun main() {
    log("start")
    runBlocking {
        launch {
            repeat(3) {
                delay(100)
                log("launchA - $it")
            }
        }
        launch {
            repeat(3) {
                delay(100)
                log("launchB - $it")
            }
        }
        GlobalScope.launch {
            repeat(3) {
                delay(120)
                log("GlobalScope - $it")
            }
        }
    }
    log("end")
}
```

```kotlin
[main] start
[main] launchA - 0
[main] launchB - 0
[DefaultDispatcher-worker-1] GlobalScope - 0
[main] launchA - 1
[main] launchB - 1
[DefaultDispatcher-worker-1] GlobalScope - 1
[main] launchA - 2
[main] launchB - 2
[main] end
```

基于是否会阻塞线程的区别，以下代码中 runBlocking 会早于 GlobalScope 输出日志

```kotlin
fun main() {
    GlobalScope.launch(Dispatchers.IO) {
        delay(600)
        log("GlobalScope")
    }
    runBlocking {
        delay(500)
        log("runBlocking")
    }
    //主动休眠两百毫秒，使得和 runBlocking 加起来的延迟时间少于六百毫秒
    Thread.sleep(200)
    log("after sleep")
}
```

```kotlin
[main] runBlocking
[DefaultDispatcher-worker-1] GlobalScope
[main] after sleep
```

## 3、coroutineScope

`coroutineScope` 函数用于创建一个独立的协程作用域，直到所有启动的协程都完成后才结束自身。`runBlocking` 和 `coroutineScope` 看起来很像，因为它们都需要等待其内部所有相同作用域的协程结束后才会结束自己。两者的主要区别在于 `runBlocking` 方法会阻塞当前线程，而 `coroutineScope`不会阻塞线程，而是会挂起并释放底层线程以供其它协程使用。由于这个差别，`runBlocking` 是一个普通函数，而 `coroutineScope` 是一个挂起函数

```kotlin
fun main() = runBlocking {
    launch {
        delay(100)
        log("Task from runBlocking")
    }
    coroutineScope {
        launch {
            delay(500)
            log("Task from nested launch")
        }
        delay(100)
        log("Task from coroutine scope")
    }
    log("Coroutine scope is over")
}
```

```kotlin
[main] Task from coroutine scope
[main] Task from runBlocking
[main] Task from nested launch
[main] Coroutine scope is over
```

## 4、supervisorScope

`supervisorScope` 函数用于创建一个使用了 SupervisorJob 的 coroutineScope，该作用域的特点就是抛出的异常不会连锁取消同级协程和父协程

```kotlin
fun main() = runBlocking {
    launch {
        delay(100)
        log("Task from runBlocking")
    }
    supervisorScope {
        launch {
            delay(500)
            log("Task throw Exception")
            throw Exception("failed")
        }
        launch {
            delay(600)
            log("Task from nested launch")
        }
    }
    log("Coroutine scope is over")
}
```

```kotlin
[main @coroutine#2] Task from runBlocking
[main @coroutine#3] Task throw Exception
[main @coroutine#4] Task from nested launch
[main @coroutine#1] Coroutine scope is over
```

## 5、自定义 CoroutineScope

假设我们在 Activity 中先后启动了多个协程用于执行异步耗时操作，那么当 Activity 退出时，必须取消所有协程以避免内存泄漏。我们可以通过保留每一个 Job 引用然后在 `onDestroy`方法里来手动取消，但这种方式相当来说会比较繁琐和低效。kotlinx.coroutines 提供了 CoroutineScope 来管理多个协程的生命周期

我们可以通过创建与 Activity 生命周期相关联的协程作用域的实例来管理协程的生命周期。CoroutineScope 的实例可以通过 `CoroutineScope()` 或 `MainScope()` 的工厂函数来构建。前者创建通用作用域，后者创建 UI 应用程序的作用域并使用 Dispatchers.Main 作为默认的调度器

```kotlin
class Activity {

    private val mainScope = MainScope()

    fun onCreate() {
        mainScope.launch {
            repeat(5) {
                delay(1000L * it)
            }
        }
    }

    fun onDestroy() {
        mainScope.cancel()
    }

}
```

或者，我们可以通过委托模式来让 Activity 实现 CoroutineScope 接口，从而可以在 Activity 内直接启动协程而不必显示地指定它们的上下文，并且在 `onDestroy()`中自动取消所有协程

```kotlin
class Activity : CoroutineScope by CoroutineScope(Dispatchers.Default) {

    fun onCreate() {
        launch {
            repeat(5) {
                delay(200L * it)
                log(it)
            }
        }
        log("Activity Created")
    }

    fun onDestroy() {
        cancel()
        log("Activity Destroyed")
    }

}
```

从输出结果可以看出，当回调了`onDestroy()`方法后协程就不会再输出日志了

```kotlin
fun main() = runBlocking {
    val activity = Activity()
    activity.onCreate()
    delay(1000)
    activity.onDestroy()
    delay(1000)
}
```

```kotlin
[main @coroutine#1] Activity Created
[DefaultDispatcher-worker-1 @coroutine#2] 0
[DefaultDispatcher-worker-1 @coroutine#2] 1
[DefaultDispatcher-worker-1 @coroutine#2] 2
[main @coroutine#1] Activity Destroyed
```

已取消的作用域无法再创建协程。因此，仅当控制其生命周期的类被销毁时，才应调用 `scope.cancel()`。例如，使用 `viewModelScope` 时，[`ViewModel`](https://developer.android.google.cn/topic/libraries/architecture/viewmodel) 类会在 ViewModel 的 `onCleared()` 方法中自动取消作用域

# 六、CoroutineBuilder

## 1、launch

看下 `launch` 函数的方法签名。`launch` 是一个作用于 CoroutineScope 的扩展函数，用于在不阻塞当前线程的情况下启动一个协程，并返回对该协程任务的引用，即 Job 对象

```kotlin
public fun CoroutineScope.launch(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job
```

`launch` 函数共包含三个参数：

1. context。用于指定协程的上下文
2. start。用于指定协程的启动方式。默认值为 `CoroutineStart.DEFAULT`，即协程会在声明的同时就立即进入等待调度的状态，即可以立即执行的状态。可以通过将其设置为`CoroutineStart.LAZY`来实现延迟启动，即懒加载
3. block。用于传递协程的执行体，即希望交由协程执行的任务

可以看到 launchA 和 launchB 是并行交叉执行的

```kotlin
fun main() = runBlocking {
    val launchA = launch {
        repeat(3) {
            delay(100)
            log("launchA - $it")
        }
    }
    val launchB = launch {
        repeat(3) {
            delay(100)
            log("launchB - $it")
        }
    }
}
```

```kotlin
[main] launchA - 0
[main] launchB - 0
[main] launchA - 1
[main] launchB - 1
[main] launchA - 2
[main] launchB - 2
```

## 2、Job

Job 是协程的句柄。使用 `launch` 或 `async` 创建的每个协程都会返回一个 `Job` 实例，该实例唯一标识协程并管理其生命周期。Job 是一个接口类型，这里列举 Job 几个比较有用的属性和函数

```kotlin
	//当 Job 处于活动状态时为 true
	//如果 Job 未被取消或没有失败，则均处于 active 状态
    public val isActive: Boolean

	//当 Job 正常结束或者由于异常结束，均返回 true
    public val isCompleted: Boolean

	//当 Job 被主动取消或者由于异常结束，均返回 true
    public val isCancelled: Boolean

	//启动 Job
	//如果此调用的确启动了 Job，则返回 true
	//如果 Job 调用前就已处于 started 或者是 completed 状态，则返回 false 
    public fun start(): Boolean

	//用于取消 Job，可同时通过传入 Exception 来标明取消原因
    public fun cancel(cause: CancellationException? = null)

	//阻塞等待直到此 Job 结束运行
    public suspend fun join()

	//当 Job 结束运行时（不管由于什么原因）回调此方法，可用于接收可能存在的运行异常
    public fun invokeOnCompletion(handler: CompletionHandler): DisposableHandle
```

Job 具有以下几种状态值，每种状态对应的属性值各不相同

| **State**                        | [isActive](https://Kotlin.github.io/Kotlinx.coroutines/Kotlinx-coroutines-core/Kotlinx.coroutines/-job/is-active.html) | [isCompleted](https://Kotlin.github.io/Kotlinx.coroutines/Kotlinx-coroutines-core/Kotlinx.coroutines/-job/is-completed.html) | [isCancelled](https://Kotlin.github.io/Kotlinx.coroutines/Kotlinx-coroutines-core/Kotlinx.coroutines/-job/is-cancelled.html) |
| :------------------------------- | :----------------------------------------------------------- | :----------------------------------------------------------- | :----------------------------------------------------------- |
| *New* (optional initial state)   | false                                                        | false                                                        | false                                                        |
| *Active* (default initial state) | true                                                         | false                                                        | false                                                        |
| *Completing* (transient state)   | true                                                         | false                                                        | false                                                        |
| *Cancelling* (transient state)   | false                                                        | false                                                        | true                                                         |
| *Cancelled* (final state)        | false                                                        | true                                                         | true                                                         |
| *Completed* (final state)        | false                                                        | true                                                         | false                                                        |

```kotlin
fun main() {
    //将协程设置为延迟启动
    val job = GlobalScope.launch(start = CoroutineStart.LAZY) {
        for (i in 0..100) {
            //每循环一次均延迟一百毫秒
            delay(100)
        }
    }
    job.invokeOnCompletion {
        log("invokeOnCompletion：$it")
    }
    log("1. job.isActive：${job.isActive}")
    log("1. job.isCancelled：${job.isCancelled}")
    log("1. job.isCompleted：${job.isCompleted}")

    job.start()

    log("2. job.isActive：${job.isActive}")
    log("2. job.isCancelled：${job.isCancelled}")
    log("2. job.isCompleted：${job.isCompleted}")

    //休眠四百毫秒后再主动取消协程
    Thread.sleep(400)
    job.cancel(CancellationException("test"))

    //休眠四百毫秒防止JVM过快停止导致 invokeOnCompletion 来不及回调
    Thread.sleep(400)

    log("3. job.isActive：${job.isActive}")
    log("3. job.isCancelled：${job.isCancelled}")
    log("3. job.isCompleted：${job.isCompleted}")
}
```

```kotlin
[main] 1. job.isActive：false
[main] 1. job.isCancelled：false
[main] 1. job.isCompleted：false
[main] 2. job.isActive：true
[main] 2. job.isCancelled：false
[main] 2. job.isCompleted：false
[DefaultDispatcher-worker-2] invokeOnCompletion：java.util.concurrent.CancellationException: test
[main] 3. job.isActive：false
[main] 3. job.isCancelled：true
[main] 3. job.isCompleted：true
```

## 3、async

看下 `async` 函数的方法签名。`async` 也是一个作用于 CoroutineScope 的扩展函数，和 `launch` 的区别主要就在于：`async` 可以返回协程的执行结果，而 `launch` 不行

```kotlin
public fun <T> CoroutineScope.async(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T
): Deferred<T>
```

通过`await()`方法可以拿到 async 协程的执行结果，可以看到两个协程的总耗时是远少于七秒的，总耗时基本等于耗时最长的协程

```kotlin
fun main() {
    val time = measureTimeMillis {
        runBlocking {
            val asyncA = async {
                delay(3000)
                1
            }
            val asyncB = async {
                delay(4000)
                2
            }
            log(asyncA.await() + asyncB.await())
        }
    }
    log(time)
}
```

```kotlin
[main] 3
[main] 4070
```

> 由于 launch 和 async 仅能够在 CouroutineScope 中使用，所以任何创建的协程都会被该 scope 追踪。Kotlin 禁止创建不能够被追踪的协程，从而避免协程泄漏

## 4、async 的错误用法

修改下上述代码，可以发现两个协程的总耗时就会变为七秒左右

```kotlin
fun main() {
    val time = measureTimeMillis {
        runBlocking {
            val asyncA = async(start = CoroutineStart.LAZY) {
                delay(3000)
                1
            }
            val asyncB = async(start = CoroutineStart.LAZY) {
                delay(4000)
                2
            }
            log(asyncA.await() + asyncB.await())
        }
    }
    log(time)
}
```

```kotlin
[main] 3
[main] 7077
```

会造成这不同区别是因为 `CoroutineStart.LAZY`不会主动启动协程，而是直到调用`async.await()`或者`async.satrt()`后才会启动（即懒加载模式），所以`asyncA.await() + asyncB.await()`会导致两个协程其实是在顺序执行。而默认值 `CoroutineStart.DEFAULT` 参数会使得协程在声明的同时就被启动了（实际上还需要等待被调度执行，但可以看做是立即就执行了），所以即使 `async.await()`会阻塞当前线程直到协程返回结果值，但两个协程其实都是处于运行状态，所以总耗时就是四秒左右

此时可以通过先调用`start()`再调用`await()`来实现第一个例子的效果

```kotlin
asyncA.start()
asyncB.start()
log(asyncA.await() + asyncB.await())
```

## 5、async 并行分解

由 `suspend` 函数启动的所有协程都必须在该函数返回结果时停止，因此你可能需要保证这些协程在返回结果之前完成。借助 Kotlin 中的结构化并发机制，你可以定义用于启动一个或多个协程的 `coroutineScope`。然后，你可以使用 `await()`（针对单个协程）或 `awaitAll()`（针对多个协程）保证这些协程在从函数返回结果之前完成

例如，假设我们定义一个用于异步获取两个文档的 `coroutineScope`。通过对每个延迟引用调用 `await()`，我们可以保证这两项 `async` 操作在返回值之前完成：

```kotlin
	suspend fun fetchTwoDocs() =
    	coroutineScope {
        	val deferredOne = async { fetchDoc(1) }
        	val deferredTwo = async { fetchDoc(2) }
        	deferredOne.await()
        	deferredTwo.await()
   	}
```

你还可以对集合使用 `awaitAll()`，如以下示例所示：

```kotlin
suspend fun fetchTwoDocs() =        // called on any Dispatcher (any thread, possibly Main)
    coroutineScope {
        val deferreds = listOf(     // fetch two docs at the same time
            async { fetchDoc(1) },  // async returns a result for the first doc
            async { fetchDoc(2) }   // async returns a result for the second doc
        )
        deferreds.awaitAll()        // use awaitAll to wait for both network requests
}
```

虽然 `fetchTwoDocs()` 使用 `async` 启动新协程，但该函数使用 `awaitAll()` 等待启动的协程完成后才会返回结果。不过请注意，即使我们没有调用 `awaitAll()`，`coroutineScope` 构建器也会等到所有新协程都完成后才恢复名为 `fetchTwoDocs` 的协程。此外，`coroutineScope` 会捕获协程抛出的所有异常，并将其传送回调用方

## 6、Deferred

`async` 函数的返回值是一个 Deferred 对象。Deferred 是一个接口类型，继承于 Job 接口，所以 Job 包含的属性和方法 Deferred 都有，其主要就是在 Job 的基础上扩展了 `await()`方法

# 七、CoroutineContext

CoroutineContext 使用以下元素集定义协程的行为：

- Job：控制协程的生命周期
- CoroutineDispatcher：将工作分派到适当的线程
- CoroutineName：协程的名称，可用于调试
- CoroutineExceptionHandler：处理未捕获的异常

## 1、Job

协程中的 Job 是其上下文 CoroutineContext 中的一部分，可以通过 `coroutineContext[Job]` 表达式从上下文中获取到

以下两个 log 语句虽然是运行在不同的协程上，但是其指向的 Job 其实是同个对象

```kotlin
fun main() = runBlocking {
    val job = launch {
        log("My job is ${coroutineContext[Job]}")
    }
    log("My job is $job")
}
```

```kotlin
[main @coroutine#1] My job is "coroutine#2":StandaloneCoroutine{Active}@75a1cd57
[main @coroutine#2] My job is "coroutine#2":StandaloneCoroutine{Active}@75a1cd57
```

实际上 CoroutineScope 的 `isActive` 这个扩展属性只是 `coroutineContext[Job]?.isActive == true` 的一种简便写法

```kotlin
public val CoroutineScope.isActive: Boolean
    get() = coroutineContext[Job]?.isActive ?: true
```

## 2、CoroutineDispatcher

CoroutineContext 包含一个 CoroutineDispatcher（协程调度器）用于指定执行协程的目标载体，即**运行于哪个线程**。CoroutineDispatcher 可以将协程的执行操作限制在特定线程上，也可以将其分派到线程池中，或者让它无限制地运行。所有的协程构造器（如 launch 和 async）都接受一个可选参数，即 CoroutineContext ，该参数可用于显式指定要创建的协程和其它上下文元素所要使用的 CoroutineDispatcher

要在主线程之外运行代码，可以让 Kotlin 协程在 Default 或 IO 调度程序上执行工作。在 Kotlin 中，所有协程都必须在 CoroutineDispatcher 中运行，即使它们在主线程上运行也是如此。协程可以自行暂停，而 CoroutineDispatcher 负责将其恢复

Kotlin 协程库提供了四个 Dispatcher 用于指定在何处运行协程，大部分情况下我们只会接触以下三个：

- **Dispatchers.Main** - 使用此调度程序可在 Android 主线程上运行协程。此调度程序只能用于与界面交互和执行快速工作。示例包括调用 `suspend` 函数、运行 Android 界面框架操作，以及更新 [`LiveData`](https://developer.android.google.cn/topic/libraries/architecture/livedata) 对象
- **[Dispatchers.IO](http://Dispatchers.IO)** - 此调度程序经过了专门优化，适合在主线程之外执行磁盘或网络 I/O。示例包括使用 [Room 组件](https://developer.android.google.cn/topic/libraries/architecture/room)、从文件中读取数据或向文件中写入数据，以及运行任何网络操作
- **Dispatchers.Default** - 此调度程序经过了专门优化，适合在主线程之外执行占用大量 CPU 资源的工作。用例示例包括对列表排序和解析 JSON

```kotlin
fun main() = runBlocking<Unit> {
    launch {
        log("main runBlocking")
    }
    launch(Dispatchers.Default) {
        log("Default")
    }
    launch(Dispatchers.IO) {
        log("IO")
    }
    launch(newSingleThreadContext("MyOwnThread")) {
        log("newSingleThreadContext")
    }
}
```

```kotlin
[DefaultDispatcher-worker-1 @coroutine#3] Default
[DefaultDispatcher-worker-2 @coroutine#4] IO
[MyOwnThread @coroutine#5] newSingleThreadContext
[main @coroutine#2] main runBlocking
```

当 `launch {...}` 在不带参数的情况下使用时，它从外部的协程作用域继承上下文和调度器，即和 runBlocking 保持一致。而在 GlobalScope 中启动协程时默认使用的调度器是 Dispatchers.default，并使用共享的后台线程池，因此 `launch(Dispatchers.default){...}` 与 `GlobalScope.launch{...}` 是使用相同的调度器。`newSingleThreadContext` 用于为协程专门创建一个新的线程来运行，专用线程是一种成本非常昂贵的资源，在实际的应用程序中必须在不再需要时释放掉，或者存储在顶级变量中以便在整个应用程序中进行重用

## 3、withContext

对于以下代码，`get`方法内使用`withContext(Dispatchers.IO)` 创建了一个指定在 IO 线程池中运行的代码块，该区间内的任何代码都始终通过 IO 线程来执行。由于 `withContext` 方法本身就是一个挂起函数，因此 `get` 方法也必须定义为挂起函数

```kotlin
suspend fun fetchDocs() {                      // Dispatchers.Main
    val result = get("developer.android.com")  // Dispatchers.Main
    show(result)                               // Dispatchers.Main
}

suspend fun get(url: String) =                 // Dispatchers.Main
    withContext(Dispatchers.IO) {              // Dispatchers.IO (main-safety block)
        /* perform network IO here */          // Dispatchers.IO (main-safety block)
    }                                          // Dispatchers.Main
}
```

借助协程，你可以细粒度地来调度线程。由于`withContext()`支持让你在不引入回调的情况下控制任何代码的执行线程池，因此你可以将其应用于非常小的函数，例如从数据库中读取数据或执行网络请求。一种不错的做法是使用 `withContext()` 来确保每个函数都是主线程安全的，这意味着，你可以从主线程调用每个函数。这样，调用方就从不需要考虑应该使用哪个线程来执行函数了

在前面的示例中，`fetchDocs()` 方法在主线程上执行；不过，它可以安全地调用 `get`方法，这样会在后台执行网络请求。由于协程支持 `suspend` 和 `resume`，因此 `withContext` 块完成后，主线程上的协程会立即根据 `get` 结果恢复

与基于回调的等效实现相比，[`withContext()`](https://Kotlin.github.io/Kotlinx.coroutines/Kotlinx-coroutines-core/Kotlinx.coroutines/with-context.html) 不会增加额外的开销。此外在某些情况下，还可以优化 `withContext()` 调用，使其超越基于回调的等效实现。例如，如果某个函数对一个网络进行十次调用，你可以使用外部 `withContext()` 让 Kotlin 只切换一次线程。这样，即使网络库多次使用 `withContext()`，它也会留在同一调度程序上，并避免切换线程。此外，Kotlin 还优化了 `Dispatchers.Default` 与 `Dispatchers.IO` 之间的切换，以尽可能避免线程切换

> 利用一个使用线程池的调度程序（例如 `Dispatchers.IO` 或 `Dispatchers.Default`）不能保证代码块一直在同一线程上从上到下执行。在某些情况下，Kotlin 协程在 `suspend` 和 `resume` 后可能会将执行工作移交给另一个线程。这意味着，对于整个 `withContext()` 块，线程局部变量可能并不指向同一个值

## 4、CoroutineName

CoroutineName 用于为协程指定一个名字，方便调试和定位问题

```kotlin
fun main() = runBlocking<Unit>(CoroutineName("RunBlocking")) {
    log("start")
    launch(CoroutineName("MainCoroutine")) {
        launch(CoroutineName("Coroutine#A")) {
            delay(400)
            log("launch A")
        }
        launch(CoroutineName("Coroutine#B")) {
            delay(300)
            log("launch B")
        }
    }
}
```

```kotlin
[main @RunBlocking#1] start
[main @Coroutine#B#4] launch B
[main @Coroutine#A#3] launch A
```

## 5、CoroutineExceptionHandler

在下文的异常处理会讲到

## 6、组合上下文元素

有时我们需要为协程上下文定义多个元素，那就可以用 `+` 运算符。例如，我们可以同时为协程指定 Dispatcher 和 CoroutineName

```kotlin
fun main() = runBlocking<Unit> {
    launch(Dispatchers.Default + CoroutineName("test")) {
        log("Hello World")
    }
}
```

```kotlin
[DefaultDispatcher-worker-1 @test#2] Hello World
```

此外，由于 CoroutineContext 是由一组元素组成的，所以加号右侧的元素会覆盖加号左侧的元素，进而组成新创建的 CoroutineContext。比如，`(Dispatchers.Main, "name") + (Dispatchers.IO) = (Dispatchers.IO, "name")`

# 八、取消协程

如果用户退出某个启动了协程的 Activity/Fragment 的话，那么大部分情况下就应该取消所有协程

`job.cancel()`就用于取消协程，`job.join()`用于阻塞等待协程运行结束。因为 `cancel()` 函数调用后会马上返回而不是等待协程结束后再返回，所以此时协程不一定就是已经停止运行了。如果需要确保协程结束运行后再执行后续代码，就需要调用 `join()` 方法来阻塞等待。也可以通过调用 Job 的扩展函数 `cancelAndJoin()` 来完成相同操作，它结合了 `cancel` 和 `join`两个操作

```kotlin
fun main() = runBlocking {
    val job = launch {
        repeat(1000) { i ->
            log("job: I'm sleeping $i ...")
            delay(500L)
        }
    }
    delay(1300L)
    log("main: I'm tired of waiting!")
    job.cancel()
    job.join()
    log("main: Now I can quit.")
}
```

```kotlin
[main] job: I'm sleeping 0 ...
[main] job: I'm sleeping 1 ...
[main] job: I'm sleeping 2 ...
[main] main: I'm tired of waiting!
[main] main: Now I can quit.
```

## 1、协程可能无法取消

并不是所有协程都可以响应取消操作，协程的取消操作是需要协作(cooperative)完成的，协程必须协作才能取消。协程库中的所有挂起函数都是可取消的，它们在运行时会检查协程是否被取消了，并在取消时抛出 CancellationException 从而结束整个任务。但如果协程正在执行计算任务并且未检查是否已处于取消状态的话，就无法取消协程

所以即使以下代码主动取消了协程，协程也只会在完成既定循环后才结束运行，因为协程没有在每次循环前先进行检查，导致任务不受取消操作的影响

```kotlin
fun main() = runBlocking {
    val startTime = System.currentTimeMillis()
    val job = launch(Dispatchers.Default) {
        var nextPrintTime = startTime
        var i = 0
        while (i < 5) {
            if (System.currentTimeMillis() >= nextPrintTime) {
                log("job: I'm sleeping ${i++} ...")
                nextPrintTime += 500L
            }
        }
    }
    delay(1300L)
    log("main: I'm tired of waiting!")
    job.cancelAndJoin()
    log("main: Now I can quit.")
}
```

```kotlin
[DefaultDispatcher-worker-1] job: I'm sleeping 0 ...
[DefaultDispatcher-worker-1] job: I'm sleeping 1 ...
[DefaultDispatcher-worker-1] job: I'm sleeping 2 ...
[main] main: I'm tired of waiting!
[DefaultDispatcher-worker-1] job: I'm sleeping 3 ...
[DefaultDispatcher-worker-1] job: I'm sleeping 4 ...
[main] main: Now I can quit.
```

为了实现取消协程的目的，就需要为上述代码加上判断协程是否还处于可运行状态的逻辑，当不可运行时就主动退出协程。`isActive` 是 CoroutineScope 的扩展属性，就用于判断协程是否还处于可运行状态

```kotlin
fun main() = runBlocking {
    val startTime = System.currentTimeMillis()
    val job = launch(Dispatchers.Default) {
        var nextPrintTime = startTime
        var i = 0
        while (i < 5) {
            if (isActive) {
                if (System.currentTimeMillis() >= nextPrintTime) {
                    log("job: I'm sleeping ${i++} ...")
                    nextPrintTime += 500L
                }
            } else {
                return@launch
            }
        }
    }
    delay(1300L)
    log("main: I'm tired of waiting!")
    job.cancelAndJoin()
    log("main: Now I can quit.")
}
```

取消协程这个操作类似于在 Java 中调用`Thread.interrupt()`方法来向线程发起中断请求，这两个操作都不会强制停止协程和线程，外部只是相当于发起一个停止运行的请求，需要依靠协程和线程响应请求后主动停止运行。Kotlin 和 Java 之所以均没有提供一个可以直接强制停止协程或线程的方法，是因为这个操作可能会带来各种意想不到的情况。在停止协程和线程的时候，它们可能还持有着某些排他性资源（例如：锁，数据库链接），如果强制性地停止，它们持有的锁就会一直无法得到释放，导致其他协程和线程一直无法得到目标资源，最终可能导致线程死锁。所以`Thread.stop()`方法目前也是处于废弃状态，Java 官方并没有提供可靠的停止线程的方法

## 2、用 finally 释放资源

可取消的挂起函数在取消时会抛出 CancellationException，可以依靠`try {...} finally {...}` 或者 Kotlin 的 `use` 函数在取消协程后释放持有的资源

```kotlin
fun main() = runBlocking {
    val job = launch {
        try {
            repeat(1000) { i ->
                log("job: I'm sleeping $i ...")
                delay(500L)
            }
        } catch (e: Throwable) {
            log(e.message)
        } finally {
            log("job: I'm running finally")
        }
    }
    delay(1300L)
    log("main: I'm tired of waiting!")
    job.cancelAndJoin()
    log("main: Now I can quit.")
}
```

```kotlin
[main] job: I'm sleeping 0 ...
[main] job: I'm sleeping 1 ...
[main] job: I'm sleeping 2 ...
[main] main: I'm tired of waiting!
[main] StandaloneCoroutine was cancelled
[main] job: I'm running finally
[main] main: Now I can quit.
```

## 3、NonCancellable

如果在上一个例子中的 `finally` 块中再调用挂起函数的话，将会导致抛出 CancellationException，因为此时协程已经被取消了。通常我们并不会遇到这种情况，因为常见的资源释放操作都是非阻塞的，且不涉及任何挂起函数。但在极少数情况下我们需要在取消的协程中再调用挂起函数，此时可以使用 `withContext` 函数和 `NonCancellable`上下文将相应的代码包装在 `withContext(NonCancellable) {...}` 代码块中，NonCancellable 就用于创建一个无法取消的协程作用域

```kotlin
fun main() = runBlocking {
    log("start")
    val launchA = launch {
        try {
            repeat(5) {
                delay(50)
                log("launchA-$it")
            }
        } finally {
            delay(50)
            log("launchA isCompleted")
        }
    }
    val launchB = launch {
        try {
            repeat(5) {
                delay(50)
                log("launchB-$it")
            }
        } finally {
            withContext(NonCancellable) {
                delay(50)
                log("launchB isCompleted")
            }
        }
    }
    //延时一百毫秒，保证两个协程都已经被启动了
    delay(200)
    launchA.cancel()
    launchB.cancel()
    log("end")
}
```

```kotlin
[main] start
[main] launchA-0
[main] launchB-0
[main] launchA-1
[main] launchB-1
[main] launchA-2
[main] launchB-2
[main] end
[main] launchB isCompleted
```

## 4、父协程和子协程

当一个协程在另外一个协程的协程作用域中启动时，它将通过 `CoroutineScope.coroutineContext` 继承其上下文，新启动的协程就被称为子协程，子协程的 Job 将成为父协程 Job 的子 Job。父协程总是会等待其所有子协程都完成后才结束自身，所以父协程不必显式跟踪它启动的所有子协程，也不必使用 `Job.join` 在末尾等待子协程完成

所以虽然 parentJob 启动的三个子协程的延时时间各不相同，但它们最终都会打印出日志

```kotlin
fun main() = runBlocking {
    val parentJob = launch {
        repeat(3) { i ->
            launch {
                delay((i + 1) * 200L)
                log("Coroutine $i is done")
            }
        }
        log("request: I'm done and I don't explicitly join my children that are still active")
    }
}
```

```kotlin
[main @coroutine#2] request: I'm done and I don't explicitly join my children that are still active
[main @coroutine#3] Coroutine 0 is done
[main @coroutine#4] Coroutine 1 is done
[main @coroutine#5] Coroutine 2 is done
```

## 5、传播取消操作

一般情况下，协程的取消操作会通过协程的层次结构来进行传播。如果取消父协程或者父协程抛出异常，那么子协程都会被取消。而如果子协程被取消，则不会影响同级协程和父协程，但如果子协程抛出异常则也会导致同级协程和父协程被取消

对于以下代码，子协程 jon1 被取消并不影响子协程 jon2 和父协程继续运行，但父协程被取消后子协程都会被递归取消

```kotlin
fun main() = runBlocking {
    val request = launch {
        val job1 = launch {
            repeat(10) {
                delay(300)
                log("job1: $it")
                if (it == 2) {
                    log("job1 canceled")
                    cancel()
                }
            }
        }
        val job2 = launch {
            repeat(10) {
                delay(300)
                log("job2: $it")
            }
        }
    }
    delay(1600)
    log("parent job canceled")
    request.cancel()
    delay(1000)
}
```

```kotlin
[main @coroutine#3] job1: 0
[main @coroutine#4] job2: 0
[main @coroutine#3] job1: 1
[main @coroutine#4] job2: 1
[main @coroutine#3] job1: 2
[main @coroutine#3] job1 canceled
[main @coroutine#4] job2: 2
[main @coroutine#4] job2: 3
[main @coroutine#4] job2: 4
[main @coroutine#1] parent job canceled
```

## 6、withTimeout

`withTimeout` 函数用于指定协程的运行超时时间，如果超时则会抛出 TimeoutCancellationException，从而令协程结束运行

```kotlin
fun main() = runBlocking {
    log("start")
    val result = withTimeout(300) {
        repeat(5) {
            delay(100)
        }
        200
    }
    log(result)
    log("end")
}
```

```kotlin
[main] start
Exception in thread "main" kotlinx.coroutines.TimeoutCancellationException: Timed out waiting for 300 ms
	at kotlinx.coroutines.TimeoutKt.TimeoutCancellationException(Timeout.kt:186)
	at kotlinx.coroutines.TimeoutCoroutine.run(Timeout.kt:156)
	at kotlinx.coroutines.EventLoopImplBase$DelayedRunnableTask.run(EventLoop.common.kt:497)
	at kotlinx.coroutines.EventLoopImplBase.processNextEvent(EventLoop.common.kt:274)
	at kotlinx.coroutines.DefaultExecutor.run(DefaultExecutor.kt:69)
	at java.lang.Thread.run(Thread.java:748)
```

`withTimeout`方法抛出的 TimeoutCancellationException 是 CancellationException 的子类，之前我们并未在输出日志上看到关于 CancellationException 这类异常的堆栈信息，这是因为对于一个已取消的协程来说，CancellationException 被认为是触发协程结束的正常原因。但对于`withTimeout`方法来说，抛出异常是其上报超时情况的一种手段，所以该异常不会被协程内部消化掉

如果不希望因为异常导致协程结束，可以改用`withTimeoutOrNull`方法，如果超时就会返回 null

# 九、异常处理

当一个协程由于异常而运行失败时，它会传播这个异常并传递给它的父协程。接下来，父协程会进行下面几步操作：

- 取消它自己的子级
- 取消它自己
- 将异常传播并传递给它的父级

异常会到达层级的根部，而且当前 CoroutineScope 所启动的所有协程都会被取消，但协程并非都是一发现异常就执行以上流程，launch 和 async 在处理异常方面有着很大的差异

launch 将异常视为未捕获异常，类似于 Java 的 Thread.uncaughtExceptionHandler，当发现异常时就会马上抛出。async 期望最终是通过调用 await 来获取结果 (或者异常)，所以默认情况下它不会抛出异常。这意味着如果使用 async 启动新的协程，它会静默地将异常丢弃，直到调用 `async.await()` 才会得到目标值或者抛出存在的异常

例如，以下代码中 launchA 抛出的异常会先连锁导致 launchB 也被取消（抛出 JobCancellationException），然后再导致父协程 BlockingCoroutine 也被取消

```kotlin
fun main() = runBlocking {
    val launchA = launch {
        delay(1000)
        1 / 0
    }
    val launchB = launch {
        try {
            delay(1300)
            log("launchB")
        } catch (e: CancellationException) {
            e.printStackTrace()
        }
    }
    launchA.join()
    launchB.join()
}
```

```kotlin
kotlinx.coroutines.JobCancellationException: Parent job is Cancelling; job=BlockingCoroutine{Cancelling}@5eb5c224
Caused by: java.lang.ArithmeticException: / by zero
	at coroutines.CoroutinesMainKt$main$1$launchA$1.invokeSuspend(CoroutinesMain.kt:11)
	···
Exception in thread "main" java.lang.ArithmeticException: / by zero
	at coroutines.CoroutinesMainKt$main$1$launchA$1.invokeSuspend(CoroutinesMain.kt:11)
	···
```

## 1、CoroutineExceptionHandler

如果不想将所有的异常信息都打印到控制台上，那么可以使用 CoroutineExceptionHandler 作为协程的上下文元素之一，在这里进行自定义日志记录或异常处理，它类似于对线程使用 Thread.uncaughtExceptionHandler。但是，CoroutineExceptionHandler 只会在预计不会由用户处理的异常上调用，因此在 async 中使用它没有任何效果，当 async 内部发生了异常且没有捕获时，那么调用 `async.await()` 依然会导致应用崩溃

以下代码只会捕获到 launch 抛出的异常

```kotlin
fun main() = runBlocking {
    val handler = CoroutineExceptionHandler { _, exception ->
        log("Caught $exception")
    }
    val job = GlobalScope.launch(handler) {
        throw AssertionError()
    }
    val deferred = GlobalScope.async(handler) {
        throw ArithmeticException()
    }
    joinAll(job, deferred)
}
```

```kotlin
[DefaultDispatcher-worker-2] Caught java.lang.AssertionError
```

## 2、SupervisorJob

由于异常导致的取消在协程中是一种双向关系，会在整个协程层次结构中传播，但如果我们需要的是单向取消该怎么实现呢？

例如，假设在 Activity 中启动了多个协程，如果单个协程所代表的子任务失败了，此时并不一定需要连锁终止整个 Activity 内部的所有其它协程任务，即此时希望子协程的异常不会传播给同级协程和父协程。而当 Activity 退出后，父协程的异常（即 CancellationException）又应该连锁传播给所有子协程，终止所有子协程

可以使用 SupervisorJob 来实现上述效果，它类似于常规的 Job，唯一的区别就是取消操作只会向下传播，一个子协程的运行失败不会影响到其他子协程

例如，以下示例中 firstChild 抛出的异常不会导致 secondChild 被取消，但当 supervisor 被取消时 secondChild 也被同时取消了

```kotlin
fun main() = runBlocking {
    val supervisor = SupervisorJob()
    with(CoroutineScope(coroutineContext + supervisor)) {
        val firstChild = launch(CoroutineExceptionHandler { _, _ -> }) {
            log("First child is failing")
            throw AssertionError("First child is cancelled")
        }
        val secondChild = launch {
            firstChild.join()
            log("First child is cancelled: ${firstChild.isCancelled}, but second one is still active")
            try {
                delay(Long.MAX_VALUE)
            } finally {
                log("Second child is cancelled because supervisor is cancelled")
            }
        }
        firstChild.join()
        log("Cancelling supervisor")
        //取消所有协程
        supervisor.cancel()
        secondChild.join()
    }
}
```

```kotlin
[main] First child is failing
[main] First child is cancelled: true, but second one is still active
[main] Cancelling supervisor
[main] Second child is cancelled because supervisor is cancelled
```

但是，如果异常没有被处理且 CoroutineContext 没有包含一个 CoroutineExceptionHandler 的话，异常会到达默认线程的 ExceptionHandler。在 JVM 中，异常会被打印在控制台；而在 Android 中，无论异常在那个 Dispatcher 中发生，都会直接导致应用崩溃。所以如果上述例子中移除了 firstChild 包含的 CoroutineExceptionHandler 的话，就会导致 Android 应用崩溃

💥 **未被捕获的异常一定会被抛出，无论使用的是哪种 Job**

# 十、Android KTX

Android KTX 是包含在 Android [Jetpack](https://developer.android.google.cn/jetpack) 及其他 Android 库中的一组 Kotlin 扩展程序。KTX 扩展程序可以为 Jetpack、Android 平台及其他 API 提供简洁的惯用 Kotlin 代码。为此，这些扩展程序利用了多种 Kotlin 语言功能，其中就包括了对 Kotlin 协程的支持

## 1、ViewModel KTX

ViewModel KTX 库提供了一个 `viewModelScope`，用于在 ViewModel 启动协程，该作用域的生命周期和 ViewModel 相等，当 ViewModel 回调了 `onCleared()`方法后会自动取消所有当前 ViewModel 中的所有协程

引入依赖：

```groovy
    dependencies {
        implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:2.2.0"
    }
```

例如，以下 `fetchDocs()` 方法内就依靠 `viewModelScope` 启动了一个协程，用于在后台线程发起网络请求

```kotlin
class MyViewModel : ViewModel() {
    
    fun fetchDocs() {
        viewModelScope.launch {
            val result = get("https://developer.android.com")
            show(result)
        }
    }

    suspend fun get(url: String) = withContext(Dispatchers.IO) { /* ... */ }

}
```

## 2、Lifecycle KTX

Lifecycle KTX 为每个 [`Lifecycle`](https://developer.android.google.cn/topic/libraries/architecture/lifecycle) 对象定义了一个 `LifecycleScope`，该作用域具有生命周期安全的保障，在此范围内启动的协程会在 `Lifecycle` 被销毁时同时取消，可以使用 `lifecycle.coroutineScope` 或 `lifecycleOwner.lifecycleScope` 属性来拿到该 CoroutineScope

引入依赖：

```groovy
    dependencies {
        implementation "androidx.lifecycle:lifecycle-runtime-ktx:2.2.0"
    }
```

以下示例演示了如何使用 `lifecycleOwner.lifecycleScope` 异步创建预计算文本：

```kotlin
class MyFragment: Fragment() {
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            val params = TextViewCompat.getTextMetricsParams(textView)
            val precomputedText = withContext(Dispatchers.Default) {
                PrecomputedTextCompat.create(longTextContent, params)
            }
            TextViewCompat.setPrecomputedText(textView, precomputedText)
        }
    }
    
}
```

## 3、LiveData KTX

使用 LiveData 时，你可能需要异步计算值。例如，你可能需要检索用户的偏好设置并将其传送给界面。在这些情况下，LiveData KTX 提供了一个 `liveData` 构建器函数，该函数会调用 suspend 函数并将结果赋值给 LiveData

引入依赖：

```groovy
    dependencies {
        implementation "androidx.lifecycle:lifecycle-livedata-ktx:2.2.0"
    }
```

在以下示例中，`loadUser()` 是在其他地方声明的 suspend 函数。 你可以使用 `liveData` 构建器函数异步调用 `loadUser()`，然后使用 `emit()` 来发出结果：

```kotlin
val user: LiveData<User> = liveData {
    val data = database.loadUser() // loadUser is a suspend function.
    emit(data)
}
```

# 十一、参考资料

- https://github.com/Kotlin/Kotlinx.coroutines/blob/master/coroutines-guide.md
- https://developer.android.google.cn/kotlin/coroutines
- https://juejin.cn/post/6844904118180380680
- https://juejin.cn/post/6888259219008126983