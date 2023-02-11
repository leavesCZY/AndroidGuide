> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

> 最近一直在了解关于 **Kotlin协程** 的知识，那最好的学习资料自然是官方提供的学习文档了，看了看后我就萌生了翻译官方文档的想法。前后花了要接近一个月时间，一共九篇文章，在这里也分享出来，希望对读者有所帮助。个人知识所限，有些翻译得不是太顺畅，也希望读者能提出意见
> 
> 协程官方文档：[coroutines-guide](https://github.com/Kotlin/kotlinx.coroutines/blob/master/coroutines-guide.md)

协程总是在由 Kotlin 标准库中定义的 CoroutineContext 表示的某个上下文中执行

协程上下文包含多种子元素。主要的元素是协程作业（Job，我们之前见过），以及它的调度器（Dispatche，本节将介绍）

# 一、调度器和线程

协程上下文（coroutine context）包含一个协程调度器（参阅 CoroutineDispatcher），协程调度器 用于确定执行协程的目标载体，即运行于哪个线程，包含一个还是多个线程。协程调度器可以将协程的执行操作限制在特定线程上，也可以将其分派到线程池中，或者让它无限制地运行

所有协程构造器（如 launch 和 async）都接受一个可选参数，即 CoroutineContext ，该参数可用于显式指定要创建的协程和其它上下文元素所要使用的调度器

请尝试以下示例：

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking<Unit> {
    //sampleStart
    launch { // context of the parent, main runBlocking coroutine
        println("main runBlocking      : I'm working in thread ${Thread.currentThread().name}")
    }
    launch(Dispatchers.Unconfined) { // not confined -- will work with main thread
        println("Unconfined            : I'm working in thread ${Thread.currentThread().name}")
    }
    launch(Dispatchers.Default) { // will get dispatched to DefaultDispatcher 
        println("Default               : I'm working in thread ${Thread.currentThread().name}")
    }
    launch(newSingleThreadContext("MyOwnThread")) { // will get its own new thread
        println("newSingleThreadContext: I'm working in thread ${Thread.currentThread().name}")
    }
    //sampleEnd    
}
```

运行结果如下所示，日志的输出顺序可能不同

```kotlin
Unconfined            : I'm working in thread main
Default               : I'm working in thread DefaultDispatcher-worker-1
newSingleThreadContext: I'm working in thread MyOwnThread
main runBlocking      : I'm working in thread main
```

当 ```launch {...}``` 在不带参数的情况下使用时，它从外部的协程作用域继承上下文和调度器。在本例中，它继承于在主线程中中运行的 runBlocking 协程的上下文

Dispatchers.Unconfined 是一个特殊的调度器，看起来似乎也在主线程中运行，但实际上它是一种不同的机制，稍后将进行解释

在 GlobalScope 中启动协程时默认使用的调度器是 Dispatchers.default，并使用共享的后台线程池，因此 ```launch(Dispatchers.default){...}``` 与 ```GlobalScope.launch{...}``` 是使用相同的调度器

newSingleThreadContext 用于为协程专门创建一个新的线程来运行。专用线程是非常昂贵的资源。在实际的应用程序中，它必须在不再需要时使用 close 函数释放掉，或者存储在顶级变量中以此实现在整个应用程序中重用

# 二、Unconfined vs confined dispatcher

Dispatchers.Unconfined 调度器在调用者线程中启动一个协程，但它仅仅只是运行到第一个挂起点。在挂起之后，它将恢复线程中的协程，该协程完全由调用的挂起函数决定。Unconfined 调度器适用于既不消耗CPU时间和不更新任何受限于特定线程的共享数据（如UI）的协程

另一方面，调度器是默认继承于外部的协程作用域的。尤其是 runBlocking 启动的协程的调度器只能是调用者所在的线程，因此继承 runBlocking 的结果是在此线程上的调度执行操作是可预测的 FIFO 

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking<Unit> {
    //sampleStart
    launch(Dispatchers.Unconfined) { // not confined -- will work with main thread
        println("Unconfined      : I'm working in thread ${Thread.currentThread().name}")
        delay(500)
        println("Unconfined      : After delay in thread ${Thread.currentThread().name}")
    }
    launch { // context of the parent, main runBlocking coroutine
        println("main runBlocking: I'm working in thread ${Thread.currentThread().name}")
        delay(1000)
        println("main runBlocking: After delay in thread ${Thread.currentThread().name}")
    }
    //sampleEnd    
}
```

运行结果：

```kotlin
Unconfined      : I'm working in thread main
main runBlocking: I'm working in thread main
Unconfined      : After delay in thread kotlinx.coroutines.DefaultExecutor
main runBlocking: After delay in thread main
```

因此，从 ```runBlocking{...}``` 继承了上下文的协程继续在主线程中执行，而调度器是 unconfined 的协程，在 delay 函数之后的代码则默认运行于 delay 函数所使用的运行线程

>  unconfined 调度器是一种高级机制，可以在某些极端情况下提供帮助而不需要调度协程以便稍后执行或产生不希望的副作用， 因为某些操作必须立即在协程中执行。 非受限调度器不应该在一般的代码中使用

# 三、调试协程和线程

协程可以在一个线程上挂起，在另一个线程上继续运行。即使使用单线程的调度器，也可能很难明确知道协程当前在做什么、在哪里、处于什么状态。调试线程的常用方法是在在日志文件中为每条日志语句加上线程名，日志框架普遍支持此功能。当使用协程时，线程名本身没有提供太多的上下文信息，因此 ```kotlinx.coroutines``` 包含了调试工具以便使协程调试起来更加容易

开启 JVM 的 ```-Dkotlinx.coroutines.debug``` 配置后运行以下代码：

```kotlin
import kotlinx.coroutines.*

fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")

fun main() = runBlocking<Unit> {
    //sampleStart
    val a = async {
        log("I'm computing a piece of the answer")
        6
    }
    val b = async {
        log("I'm computing another piece of the answer")
        7
    }
    log("The answer is ${a.await() * b.await()}")
    //sampleEnd    
}
```

共有三个协程。runBlocking 中的主协程（#1）和两个计算延迟值a（#2）和b（#3）的协程。它们都在 runBlocking 的上下文中执行，并且仅限于主线程。此代码的输出为：

```kotlin
[main @coroutine#2] I'm computing a piece of the answer
[main @coroutine#3] I'm computing another piece of the answer
[main @coroutine#1] The answer is 42
```

log 函数在方括号中打印线程名，可以看到协程都运行于主线程，线程名后附有有当前正在执行的协程的标识符。当调试模式打开时，此标识符将连续分配给所有创建的协程

当使用 -ea 选项运行 JVM 时，调试模式也将打开，可以在 DEBUG_PROPERTY_NAME 属性文档中阅读有关调试工具的更多信息

# 四、在线程间切换

开启 JVM 的 ```-Dkotlinx.coroutines.debug``` 配置后运行以下代码：

```kotlin
import kotlinx.coroutines.*

fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")

fun main() {
    //sampleStart
    newSingleThreadContext("Ctx1").use { ctx1 ->
        newSingleThreadContext("Ctx2").use { ctx2 ->
            runBlocking(ctx1) {
                log("Started in ctx1")
                withContext(ctx2) {
                    log("Working in ctx2")
                }
                log("Back to ctx1")
            }
        }
    }
    //sampleEnd    
}
```

这里演示了几种新技巧。一个是对显示指定的上下文使用 runBlocking，另一个是使用 withContext 函数更改协程的上下文并同时仍然保持在另一个协程中，如你在下面的输出中所看到的：

```kotlin
[Ctx1 @coroutine#1] Started in ctx1
[Ctx2 @coroutine#1] Working in ctx2
[Ctx1 @coroutine#1] Back to ctx1
```

注意，本例还使用了 kotlin 标准库中的 ```use``` 函数用来在不再需要时释放 newSingleThreadContext 所创建的线程

# 五、上下文中的 Job

协程中的 Job 是其上下文中的一部分，可以通过 ```coroutineContext[Job]```  表达式从上下文中获取到

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking<Unit> {
    //sampleStart
    println("My job is ${coroutineContext[Job]}")
    //sampleEnd    
}
```

在 debug 模式下，输出结果类似于：

```kotlin
My job is "coroutine#1":BlockingCoroutine{Active}@6d311334
```

注意，CoroutineScope 的 isActive 属性只是 ```coroutineContext[Job]?.isActive == true``` 的一种简便写法

```kotlin
public val CoroutineScope.isActive: Boolean
    get() = coroutineContext[Job]?.isActive ?: true
```

# 六、子协程

当一个协程在另外一个协程的协程作用域中启动时，它将通过 CoroutineScope.coroutineContext 继承其上下文，新启动的协程的 Job 将成为父协程的 Job 的子 Job。当父协程被取消时，它的所有子协程也会递归地被取消

但是，当使用 GlobalScope 启动协程时，协程的 Job 没有父级。因此，它不受其启动的作用域和独立运作范围的限制

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking<Unit> {
    //sampleStart
    // launch a coroutine to process some kind of incoming request
    val request = launch {
        // it spawns two other jobs, one with GlobalScope
        GlobalScope.launch {
            println("job1: I run in GlobalScope and execute independently!")
            delay(1000)
            println("job1: I am not affected by cancellation of the request")
        }
        // and the other inherits the parent context
        launch {
            delay(100)
            println("job2: I am a child of the request coroutine")
            delay(1000)
            println("job2: I will not execute this line if my parent request is cancelled")
        }
    }
    delay(500)
    request.cancel() // cancel processing of the request
    delay(1000) // delay a second to see what happens
    println("main: Who has survived request cancellation?")
    //sampleEnd
}
```

运行结果是：

```kotlin
job1: I run in GlobalScope and execute independently!
job2: I am a child of the request coroutine
job1: I am not affected by cancellation of the request
main: Who has survived request cancellation?
```

# 七、父协程的职责

父协程总是会等待其所有子协程完成。父协程不必显式跟踪它启动的所有子协程，也不必使用 Job.join 在末尾等待子协程完成

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking<Unit> {
    //sampleStart
    // launch a coroutine to process some kind of incoming request
    val request = launch {
        repeat(3) { i -> // launch a few children jobs
            launch  {
                delay((i + 1) * 200L) // variable delay 200ms, 400ms, 600ms
                println("Coroutine $i is done")
            }
        }
        println("request: I'm done and I don't explicitly join my children that are still active")
    }
    request.join() // wait for completion of the request, including all its children
    println("Now processing of the request is complete")
    //sampleEnd
}
```

运行结果：

```kotlin
request: I'm done and I don't explicitly join my children that are still active
Coroutine 0 is done
Coroutine 1 is done
Coroutine 2 is done
Now processing of the request is complete
```

# 八、为协程命名以便调试

当协程经常需要进行日志调试时，协程自动分配到的 ID 是很有用处的，你只需要关联来自同一协程的日志记录。但是，当一个协程绑定到一个特定请求的处理或者执行某个特定的后台任务时，最好显式地为它命名，以便进行调试。CoroutineName 上下文元素的作用与线程名相同，它包含在启用调试模式时执行此协程的线程名中

以下代码展示了此概念：

```kotlin
import kotlinx.coroutines.*

fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")

fun main() = runBlocking(CoroutineName("main")) {
    //sampleStart
    log("Started main coroutine")
    // run two background value computations
    val v1 = async(CoroutineName("v1coroutine")) {
        delay(500)
        log("Computing v1")
        252
    }
    val v2 = async(CoroutineName("v2coroutine")) {
        delay(1000)
        log("Computing v2")
        6
    }
    log("The answer for v1 / v2 = ${v1.await() / v2.await()}")
    //sampleEnd    
}
```

开启 ```-Dkotlinx.coroutines.debug``` JVM 配置后的输出结果类似于：

```kotlin
[main @main#1] Started main coroutine
[main @v1coroutine#2] Computing v1
[main @v2coroutine#3] Computing v2
[main @main#1] The answer for v1 / v2 = 42
```

# 九、组合上下文元素

有时我们需要为协程上下文定义多个元素。我们可以用 ```+``` 运算符。例如，我们可以同时使用显式指定的调度器和显式指定的名称来启动协程

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking<Unit> {
    //sampleStart
    launch(Dispatchers.Default + CoroutineName("test")) {
        println("I'm working in thread ${Thread.currentThread().name}")
    }
    //sampleEnd    
}
```

开启 ```-Dkotlinx.coroutines.debug``` JVM 配置后的输出结果是：

```kotlin
I'm working in thread DefaultDispatcher-worker-1 @test#2
```

# 十、协程作用域

让我们把关于作用域、子元素和 Job 的知识点放在一起。假设我们的应用程序有一个具有生命周期的对象，但该对象不是协程。例如，我们正在编写一个Android应用程序，并在Android Activity中启动各种协程，以执行异步操作来获取和更新数据、指定动画等。当 Activity 销毁时，必须取消所有协程以避免内存泄漏。当然，我们可以手动操作上下文和 Job 来绑定 Activity 和协程的生命周期。但是，kotlinx.coroutines 提供了一个抽象封装：CoroutineScope。你应该已经对协程作用域很熟悉了，因为所有的协程构造器都被声明为它的扩展函数

我们通过创建与 Activity 生命周期相关联的协程作用域的实例来管理协程的生命周期。CoroutineScope 的实例可以通过 CoroutineScope() 或 MainScope() 的工厂函数来构建。前者创建通用作用域，后者创建 UI 应用程序的作用域并使用 Dispatchers.Main 作为默认的调度器

```kotlin
class Activity {
    private val mainScope = MainScope()
    
    fun destroy() {
        mainScope.cancel()
    }
    // to be continued ...
}
```

或者，我们可以在这个 Activity 类中实现 CoroutineScope 接口。最好的实现方式是对默认工厂函数使用委托。我们还可以将所需的调度器（在本例中使用Dispatchers.Default）与作用域结合起来：

```kotlin
    class Activity : CoroutineScope by CoroutineScope(Dispatchers.Default) {
    // to be continued ...
```

现在，我们可以在这个 Activity 内启动协程，而不必显示地指定它们的上下文。为了演示，我们启动了十个分别延时不同时间的协程：

```kotlin
    // class Activity continues
    fun doSomething() {
        // launch ten coroutines for a demo, each working for a different time
        repeat(10) { i ->
            launch {
                delay((i + 1) * 200L) // variable delay 200ms, 400ms, ... etc
                println("Coroutine $i is done")
            }
        }
    }
} // class Activity ends
```

在主函数中，我们创建 Activity 对象，调用测试 doSomething 函数，并在500毫秒后销毁该活动。这将取消从 doSomething 中启动的所有协程。我们可以看到这一点，因为在销毁 activity 对象后，即使我们再等待一会儿，也不会再打印消息

```kotlin
import kotlin.coroutines.*
import kotlinx.coroutines.*

class Activity : CoroutineScope by CoroutineScope(Dispatchers.Default) {

    fun destroy() {
        cancel() // Extension on CoroutineScope
    }
    // to be continued ...

    // class Activity continues
    fun doSomething() {
        // launch ten coroutines for a demo, each working for a different time
        repeat(10) { i ->
            launch {
                delay((i + 1) * 200L) // variable delay 200ms, 400ms, ... etc
                println("Coroutine $i is done")
            }
        }
    }
} // class Activity ends

fun main() = runBlocking<Unit> {
    //sampleStart
    val activity = Activity()
    activity.doSomething() // run test function
    println("Launched coroutines")
    delay(500L) // delay for half a second
    println("Destroying activity!")
    activity.destroy() // cancels all coroutines
    delay(1000) // visually confirm that they don't work
    //sampleEnd    
}
```

输出结果：

```kotlin
Launched coroutines
Coroutine 0 is done
Coroutine 1 is done
Destroying activity!
```

如你所见，只有前两个协程会打印一条消息，其它的则会被 ```Activity.destroy()``` 中的 ```job.cancel()``` 所取消

# 十一、线程局部数据

有时，将一些线程局部数据传递到协程或在协程之间传递是有实际用途的。但是，由于协程没有绑定到任何特定的线程，如果手动完成，这可能会导致模板代码

对于 ThreadLocal，扩展函数 asContextElement 可用于解决这个问题。它创建一个附加的上下文元素，该元素保持 ```ThreadLocal``` 给定的值，并在每次协程切换其上下文时恢复该值

很容易在实践中证明：

```kotlin
import kotlinx.coroutines.*

val threadLocal = ThreadLocal<String?>() // declare thread-local variable

fun main() = runBlocking<Unit> {
    //sampleStart
    threadLocal.set("main")
    println("Pre-main, current thread: ${Thread.currentThread()}, thread local value: '${threadLocal.get()}'")
    val job = launch(Dispatchers.Default + threadLocal.asContextElement(value = "launch")) {
        println("Launch start, current thread: ${Thread.currentThread()}, thread local value: '${threadLocal.get()}'")
        yield()
        println("After yield, current thread: ${Thread.currentThread()}, thread local value: '${threadLocal.get()}'")
    }
    job.join()
    println("Post-main, current thread: ${Thread.currentThread()}, thread local value: '${threadLocal.get()}'")
    //sampleEnd    
}
```

在本例中，我们使用 Dispatchers.Default 在后台线程池中启动一个新的协程，因为它可以在线程池中不同的线程之间来回切换工作，但它仍然具有我们使用 ```threadLocal.asContextElement（value="launch"）```指定的线程局部变量的值，无论协程在哪个线程上执行。因此，输出结果是（开启了调试模式）：

```kotlin
Pre-main, current thread: Thread[main @coroutine#1,5,main], thread local value: 'main'
Launch start, current thread: Thread[DefaultDispatcher-worker-1 @coroutine#2,5,main], thread local value: 'launch'
After yield, current thread: Thread[DefaultDispatcher-worker-2 @coroutine#2,5,main], thread local value: 'launch'
Post-main, current thread: Thread[main @coroutine#1,5,main], thread local value: 'main'
```

我们很容易就忘记设置相应的上下文元素。如果运行协程的线程会有多个，则从协程访问的线程局部变量可能会有意外值。为了避免这种情况，建议使用 ensurePresent 方法，并在使用不当时可以快速失败

ThreadLocal 具备一等支持，可以与任何基础 kotlinx.coroutines 一起使用。不过，它有一个关键限制：当线程局部变量发生变化时，新值不会传导到协程调用方（因为上下文元素无法跟踪所有的线程本地对象引用）。并且更新的值在下次挂起时丢失。使用 withContext 更新协程中线程的局部值，有关详细信息，请参阅 asContextElement

或者，值可以存储在一个可变的类计数器中(var i: Int)，而类计数器又存储在一个线程局部变量中，但是，在这种情况下，您完全有责任同步此计数器中变量的潜在并发修改

有关高级用法，比如与 logging MDC, transactional contexts或其它在内部使用线程局部变量传递数据的库集成，请参阅实现了 ThreadContextElement 接口的文档