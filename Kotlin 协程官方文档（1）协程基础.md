> 最近一直在了解关于 **Kotlin协程** 的知识，那最好的学习资料自然是官方提供的学习文档了，看了看后我就萌生了翻译官方文档的想法。前后花了要接近一个月时间，一共九篇文章，在这里也分享出来，希望对读者有所帮助。个人知识所限，有些翻译得不是太顺畅，也希望读者能提出意见
> 
> 协程官方文档：[coroutines-guide](https://github.com/Kotlin/kotlinx.coroutines/blob/master/coroutines-guide.md)

此章节涵盖了协程的基本概念

# 一、你的第一个协程程序

运行以下代码：

```kotlin
import kotlinx.coroutines.*

fun main() {
    GlobalScope.launch { // 在后台启动一个新协程，并继续执行之后的代码
        delay(1000L) // 非阻塞式地延迟一秒
        println("World!") // 延迟结束后打印
    }
    println("Hello,") //主线程继续执行，不受协程 delay 所影响
    Thread.sleep(2000L) // 主线程阻塞式睡眠2秒，以此来保证JVM存活
}
```

输出结果

```kotlin
Hello,
World!
```

本质上，协程可以称为**轻量级线程**。协程在 CoroutineScope （协程作用域）的上下文中通过 launch、async 等协程构造器（coroutine builder）来启动。在上面的例子中，在 GlobalScope ，即**全局作用域**内启动了一个新的协程，这意味着该协程的生命周期只受整个应用程序的生命周期的限制，即只要整个应用程序还在运行中，只要协程的任务还未结束，该协程就可以一直运行

可以将以上的协程改写为常用的 thread 形式，可以获得相同的结果

```kotlin
fun main() {
    thread {
        Thread.sleep(1000L)
        println("World!")
    }
    println("Hello,")
    Thread.sleep(2000L)
}
```

但是如果仅仅是将 ```GlobalScope.launch``` 替换为 ```thread``` 的话，编译器将提示错误：

```kotlin
Suspend function 'delay' should be called only from a coroutine or another suspend function
```

这是由于 ```delay()``` 是一个挂起函数（suspending function），挂起函数只能由协程或者其它挂起函数进行调度。挂起函数不会阻塞线程，而是会将协程挂起，在特定的时候才再继续运行

> 开发者需要明白，协程是运行于线程上的，一个线程可以运行多个（可以是几千上万个）协程。线程的调度行为是由 OS 来操纵的，而协程的调度行为是可以由开发者来指定并由编译器来实现的。当协程 A 调用 delay(1000L) 函数来指定延迟1秒后再运行时，协程 A 所在的线程只是会转而去执行协程 B，等到1秒后再把协程 A 加入到可调度队列里。所以说，线程并不会因为协程的延时而阻塞，这样可以极大地提高线程的并发灵活度

# 二、桥接阻塞与非阻塞的世界

在第一个协程程序里，混用了非阻塞代码 ```delay()``` 与阻塞代码 ```Thread.sleep()``` ，使得我们很容易就搞混当前程序是否是阻塞的。可以改用 runBlocking 来明确这种情形

```kotlin
import kotlinx.coroutines.*

fun main() { 
    GlobalScope.launch { // launch a new coroutine in background and continue
        delay(1000L)
        println("World!")
    }
    println("Hello,") // main thread continues here immediately
    runBlocking {     // but this expression blocks the main thread
        delay(2000L)  // ... while we delay for 2 seconds to keep JVM alive
    } 
}
```

运行结果和第一个程序是一样的，但是这段代码只使用了非阻塞延迟。主线程调用了 runBlocking 函数，直到 runBlocking 内部的所有协程执行完成后，之后的代码才会继续执行

可以将以上代码用更喜欢的方式来重写，使用 runBlocking 来包装 main 函数的执行体：

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking<Unit> { // start main coroutine
    GlobalScope.launch { // launch a new coroutine in background and continue
        delay(1000L)
        println("World!")
    }
    println("Hello,") // main coroutine continues here immediately
    delay(2000L)      // delaying for 2 seconds to keep JVM alive
}
```

这里 ``runBlocking<Unit> { ... } `` 作为用于启动顶层主协程的适配器。我们显式地指定它的返回类型 Unit，因为 kotlin 中 main 函数必须返回 Unit 类型，但一般我们都可以省略类型声明，因为编译器可以自动推导（这需要代码块的最后一行代码语句没有返回值或者返回值为 Unit）

这也是为挂起函数编写单元测试的一种方法：

```kotlin
class MyTest {
    @Test
    fun testMySuspendingFunction() = runBlocking<Unit> {
        // here we can use suspending functions using any assertion style that we like
    }
}
```

需要注意的是，runBlocking 代码块默认运行于其声明所在的线程，而 launch 代码块默认运行于线程池中，可以通过打印当前线程名来进行区分

# 三、等待作业

延迟一段时间来等待另一个协程运行并不是一个好的选择，可以显式（非阻塞的方式）地等待协程执行完成

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
	//sampleStart
    val job = GlobalScope.launch { // launch a new coroutine and keep a reference to its Job
        delay(1000L)
        println("World!")
    }
    println("Hello,")
    job.join() // wait until child coroutine completes
	//sampleEnd    
}
```

现在，代码的运行结果仍然是相同的，但是主协程与后台作业的持续时间没有任何关系，这样好多了

# 四、结构化并发

以上对于协程的使用还有一些需要改进的地方。```GlobalScope.launch``` 会创建一个顶级协程。尽管它很轻量级，但在运行时还是会消耗一些内存资源。如果开发者忘记保留对该协程的引用，它将可以一直运行直到整个应用程序停止。我们会遇到一些比较麻烦的情形，比如协程中的代码被挂起（比如错误地延迟了太多时间），或者启动了太多协程导致内存不足。此时我们需要手动保留对所有已启动协程的引用以便在需要的时候停止协程，但这很容易出错

kotlin 提供了更好的解决方案。我们可以在代码中使用结构化并发。正如我们通常使用线程那样（线程总是全局的），我们可以在特定的范围内来启动协程

在上面的示例中，我们通过 runBlocking 将 main() 函数转为协程。每个协程构造器（包括 runBlocking）都会将 CoroutineScope 的实例添加到其代码块的作用域中。我们可以在这个作用域中启动协程，而不必显式地 ```join```，因为外部协程（示例代码中的 runBlocking）在其作用域中启动的所有协程完成之前不会结束。因此，我们可以简化示例代码：

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking { // this: CoroutineScope
    launch { // launch a new coroutine in the scope of runBlocking
        delay(1000L)
        println("World!")
    }
    println("Hello,")
}
```
launch 函数是 CoroutineScope 的扩展函数，而 runBlocking 的函数体参数也是被声明为 CoroutineScope 的扩展函数，所以 launch 函数就隐式持有了和 runBlocking 相同的协程作用域。此时即使 delay 再久， ``println("World!")`` 也一定会被执行

# 五、作用域构建器

除了使用官方的几个协程构建器所提供的协程作用域之外，还可以使用 ```coroutineScope``` 来声明自己的作用域。coroutineScope 用于创建一个协程作用域，直到所有启动的子协程都完成后才结束

runBlocking 和 coroutineScope 看起来很像，因为它们都需要等待其内部所有相同作用域的子协程结束后才会结束自己。两者的主要区别在于 runBlocking 方法会阻塞当前线程，而 coroutineScope 只是挂起并释放底层线程以供其它协程使用。由于这个差别，所以 runBlocking 是一个普通函数，而 coroutineScope 是一个挂起函数

可以通过以下示例来演示：

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking { // this: CoroutineScope
    launch { 
        delay(200L)
        println("Task from runBlocking")
    }
    
    coroutineScope { // Creates a coroutine scope
        launch {
            delay(500L) 
            println("Task from nested launch")
        }
    
        delay(100L)
        println("Task from coroutine scope") // This line will be printed before the nested launch
    }
    
    println("Coroutine scope is over") // This line is not printed until the nested launch completes
}
```

运行结果：

```kotlin
Task from coroutine scope
Task from runBlocking
Task from nested launch
Coroutine scope is over 
```

注意，在 “Task from coroutine scope” 消息打印之后，在等待 launch 执行完之前 ，将执行并打印“Task from runBlocking”，尽管此时 coroutineScope 尚未完成

# 六、提取函数并重构

抽取 launch 内部的代码块为一个独立的函数，需要将之声明为挂起函数。挂起函数可以像常规函数一样在协程中使用，但它们的额外特性是：可以依次使用其它挂起函数（如 delay 函数）来使协程挂起 

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    launch { doWorld() }
    println("Hello,")
}

// this is your first suspending function
suspend fun doWorld() {
    delay(1000L)
    println("World!")
}
```

但是如果提取出的函数包含一个在当前作用域中调用的协程构建器的话该怎么办？ 在这种情况下，所提取函数上只有 ```suspend``` 修饰符是不够的。为 ```CoroutineScope``` 写一个扩展函数 ```doWorld``` 是其中一种解决方案，但这可能并非总是适用的，因为它并没有使 API 更加清晰。 常用的解决方案是要么显式将 ```CoroutineScope``` 作为包含该函数的类的一个字段， 要么当外部类实现了 ```CoroutineScope``` 时隐式取得。 作为最后的手段，可以使用 ```CoroutineScope(coroutineContext)```，不过这种方法结构上并不安全， 因为你不能再控制该方法执行的作用域。只有私有 API 才能使用这个构建器。

# 七、协程是轻量级的

运行以下代码：

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    repeat(100_000) { // launch a lot of coroutines
        launch {
            delay(1000L)
            print(".")
        }
    }
}
```

以上代码启动了10万个协程，每个协程延时一秒后都会打印输出。如果改用线程来完成的话，很大可能会发生内存不足异常，但用协程来完成的话就可以轻松胜任

# 八、全局协程类似于守护线程

以下代码在 GlobalScope 中启动了一个会长时间运行的协程，它每秒打印两次 "I'm sleeping" ，然后在延迟一段时间后从 main 函数返回

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    //sampleStart
    GlobalScope.launch {
        repeat(1000) { i ->
            println("I'm sleeping $i ...")
            delay(500L)
        }
    }
    delay(1300L) // just quit after delay
    //sampleEnd    
}
```

你可以运行代码并看到它打印了三行后终止运行：

```kotlin
I'm sleeping 0 ...
I'm sleeping 1 ...
I'm sleeping 2 ...
```

这是由于 launch 函数依附的协程作用域是 GlobalScope，而非 runBlocking 所隐含的作用域。在 GlobalScope 中启动的协程无法使进程保持活动状态，它们就像守护线程（当主线程消亡时，守护线程也将消亡）
