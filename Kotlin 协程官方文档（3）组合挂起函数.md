> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

> 最近一直在了解关于 **Kotlin协程** 的知识，那最好的学习资料自然是官方提供的学习文档了，看了看后我就萌生了翻译官方文档的想法。前后花了要接近一个月时间，一共九篇文章，在这里也分享出来，希望对读者有所帮助。个人知识所限，有些翻译得不是太顺畅，也希望读者能提出意见
> 
> 协程官方文档：[coroutines-guide](https://github.com/Kotlin/kotlinx.coroutines/blob/master/coroutines-guide.md)

本节来介绍构成挂起函数的各种方法

# 一、默认顺序

假设我们有两个定义于其它位置的挂起函数，它们用于执行一些有用操作，比如某种远程服务调用或者是计算操作。我们假设这两个函数是有实际意义的，但实际上它们只是为了模拟情况而延迟了一秒钟

```kotlin
suspend fun doSomethingUsefulOne(): Int {
    delay(1000L) // pretend we are doing something useful here
    return 13
}

suspend fun doSomethingUsefulTwo(): Int {
    delay(1000L) // pretend we are doing something useful here, too
    return 29
}
```

在实践中，如果我们需要依靠第一个函数的运行结果来决定是否需要调用或者如何调用第二个函数，此时我们就需要按顺序来运行这两个函数

我们使用默认顺序来调用这两个函数，因为协程中的代码和常规代码一样，在默认情况下是顺序的执行的。下面来计算两个挂起函数运行所需的总时间

```kotlin
import kotlinx.coroutines.*
import kotlin.system.*

fun main() = runBlocking<Unit> {
    //sampleStart
    val time = measureTimeMillis {
        val one = doSomethingUsefulOne()
        val two = doSomethingUsefulTwo()
        println("The answer is ${one + two}")
    }
    println("Completed in $time ms")
    //sampleEnd    
}

suspend fun doSomethingUsefulOne(): Int {
    delay(1000L) // pretend we are doing something useful here
    return 13
}

suspend fun doSomethingUsefulTwo(): Int {
    delay(1000L) // pretend we are doing something useful here, too
    return 29
}
```

将得到类似于下边这样的输出，可以看出函数是按顺序先后执行的

```kotlin
The answer is 42
Completed in 2007 ms
```

# 二、使用 async 并发

如果 doSomethingUsefulOne() 和 doSomethingUsefulTwo() 这两个函数之间没有依赖关系，并且我们希望通过同时执行这两个操作（并发）以便更快地得到答案，此时就需要用到 ```async``` 了

从概念上讲，async 就类似于 launch。async 启动一个单独的协程，这是一个与所有其它协程同时工作的轻量级协程。不同之处在于，launch 返回 Job 对象并且不携带任何运行结果值。而 async 返回一个轻量级非阻塞的 ```Deferred``` 对象，可用于在之后取出返回值，可以通过调用 Deferred 的 ```await()``` 方法来获取最终结果。此外，Deferred 也实现了 Job 接口，所以也可以根据需要来取消它

```kotlin
import kotlinx.coroutines.*
import kotlin.system.*

fun main() = runBlocking<Unit> {
    //sampleStart
    val time = measureTimeMillis {
        val one = async { doSomethingUsefulOne() }
        val two = async { doSomethingUsefulTwo() }
        println("The answer is ${one.await() + two.await()}")
    }
    println("Completed in $time ms")
    //sampleEnd    
}

suspend fun doSomethingUsefulOne(): Int {
    delay(1000L) // pretend we are doing something useful here
    return 13
}

suspend fun doSomethingUsefulTwo(): Int {
    delay(1000L) // pretend we are doing something useful here, too
    return 29
}
```

运行结果类似于以下所示

```kotlin
The answer is 42
Completed in 1014 ms
```

可以看到运行耗时几乎是减半了，因为这两个协程是同时运行，总的耗时时间可以说是取决于耗时最长的任务。需要注意，协程的并发总是显式的

# 三、惰性启动 async

可选的，可以将 async 的 ```start``` 参数设置为 ```CoroutineStart.lazy``` 使其变为懒加载模式。在这种模式下，只有在主动调用 Deferred 的 ```await()``` 或者 ```start()``` 方法时才会启动协程。运行以下示例：

```kotlin
import kotlinx.coroutines.*
import kotlin.system.*

fun main() = runBlocking<Unit> {
    //sampleStart
    val time = measureTimeMillis {
        val one = async(start = CoroutineStart.LAZY) { doSomethingUsefulOne() }
        val two = async(start = CoroutineStart.LAZY) { doSomethingUsefulTwo() }
        // some computation
        one.start() // start the first one
        two.start() // start the second one
        println("The answer is ${one.await() + two.await()}")
    }
    println("Completed in $time ms")
    //sampleEnd    
}

suspend fun doSomethingUsefulOne(): Int {
    delay(1000L) // pretend we are doing something useful here
    return 13
}

suspend fun doSomethingUsefulTwo(): Int {
    delay(1000L) // pretend we are doing something useful here, too
    return 29
}
```

将得到以下类似输出：

```kotlin
The answer is 42
Completed in 1016 ms
```

以上定义了两个协程，但没有像前面的例子那样直接执行，而是将控制权交给了开发者，由开发者通过调用 ```start()``` 函数来确切地开始执行。首先启动了协程 one，然后启动了协程 two，然后再等待协程运行结束

注意，如果只是在 ```println``` 中调用了 ```await()``` 而不首先调用 ```start()``` ,这将形成顺序行为，因为 ```await()``` 会启动协程并等待其完成，这不是 lazy 模式的预期结果。```async(start=CoroutineStart.LAZY)``` 的用例是标准标准库中的 lazy 函数的替代品，用于在值的计算涉及挂起函数的情况下

# 四、异步风格的函数

我们可以定义异步风格的函数，使用带有显式 GlobalScope 引用的异步协程生成器来调用 ```doSomethingUsefulOne``` 和 ```doSomethingUsefulTwo``` 函数。用 “…Async” 后缀来命名这些函数，以此来强调它们用来启动异步计算，并且需要通过其返回的延迟值来获取结果

```kotlin
// The result type of somethingUsefulOneAsync is Deferred<Int>
fun somethingUsefulOneAsync() = GlobalScope.async {
    doSomethingUsefulOne()
}

// The result type of somethingUsefulTwoAsync is Deferred<Int>
fun somethingUsefulTwoAsync() = GlobalScope.async {
    doSomethingUsefulTwo()
}
```

注意，这些 ```xxxAsync``` 函数不是挂起函数，它们可以从任何地方调用。但是，调用这些函数意味着是要用异步形式来执行操作

以下示例展示了它们在协程之外的使用：

```kotlin
import kotlinx.coroutines.*
import kotlin.system.*

//sampleStart
// note that we don't have `runBlocking` to the right of `main` in this example
fun main() {
    val time = measureTimeMillis {
        // we can initiate async actions outside of a coroutine
        val one = somethingUsefulOneAsync()
        val two = somethingUsefulTwoAsync()
        // but waiting for a result must involve either suspending or blocking.
        // here we use `runBlocking { ... }` to block the main thread while waiting for the result
        runBlocking {
            println("The answer is ${one.await() + two.await()}")
        }
    }
    println("Completed in $time ms")
}
//sampleEnd

fun somethingUsefulOneAsync() = GlobalScope.async {
    doSomethingUsefulOne()
}

fun somethingUsefulTwoAsync() = GlobalScope.async {
    doSomethingUsefulTwo()
}

suspend fun doSomethingUsefulOne(): Int {
    delay(1000L) // pretend we are doing something useful here
    return 13
}

suspend fun doSomethingUsefulTwo(): Int {
    delay(1000L) // pretend we are doing something useful here, too
    return 29
}
```

> 这里展示的带有异步函数的编程样式仅供说明，因为它是其它编程语言中的流行样式。强烈建议不要将此样式与 kotlin 协程一起使用，因为如下所述

想象一下，如果在 ```val one = somethingUsefulOneAsync()``` 和 ```one.await()``` 这两行代码之间存在逻辑错误，导致程序抛出异常，正在执行的操作也被中止，此时会发生什么情况？通常，全局的错误处理者可以捕获此异常，为开发人员记录并报告错误，但是程序可以继续执行其它操作。但是这里 ```somethingUsefulOneAsync()``` 函数仍然还在后台运行（因为其协程作用域是 GlobalScope），即使其启动者已经被中止了。这个问题不会在结构化并发中出现，如下一节所示

# 五、使用 async 的结构化并发

让我们以 ```Concurrent using async``` 章节为例，提取一个同时执行 ```doSomethingUsefulOne()``` 和 ```doSomethingUsefulTwo()``` 并返回其结果之和的函数。因为 async 函数被定义为 CoroutineScope 上的一个扩展函数，所以我们需要将它放在 CoroutineScope 中，这就是  coroutineScope 函数提供的功能：

```kotlin
suspend fun concurrentSum(): Int = coroutineScope {
    val one = async { doSomethingUsefulOne() }
    val two = async { doSomethingUsefulTwo() }
    one.await() + two.await()
}
```

这样，如果 ```concurrentSum()``` 函数发生错误并引发异常，则在其作用域中启动的所有协程都将被取消

```kotlin
import kotlinx.coroutines.*
import kotlin.system.*

fun main() = runBlocking<Unit> {
    //sampleStart
    val time = measureTimeMillis {
        println("The answer is ${concurrentSum()}")
    }
    println("Completed in $time ms")
    //sampleEnd    
}

suspend fun concurrentSum(): Int = coroutineScope {
    val one = async { doSomethingUsefulOne() }
    val two = async { doSomethingUsefulTwo() }
    one.await() + two.await()
}

suspend fun doSomethingUsefulOne(): Int {
    delay(1000L) // pretend we are doing something useful here
    return 13
}

suspend fun doSomethingUsefulTwo(): Int {
    delay(1000L) // pretend we are doing something useful here, too
    return 29
}
```

从 main 函数的输出结果来看，两个操作仍然是同时执行的

```kotlin
The answer is 42
Completed in 1017 ms
```

取消操作始终通过协程的层次结构来进行传播

```kotlin
fun main() = runBlocking<Unit> {
    try {
        failedConcurrentSum()
    } catch(e: ArithmeticException) {
        println("Computation failed with ArithmeticException")
    }
}

suspend fun failedConcurrentSum(): Int = coroutineScope {
    val one = async<Int> {
        try {
            delay(Long.MAX_VALUE) // Emulates very long computation
            42
        } finally {
            println("First child was cancelled")
        }
    }
    val two = async<Int> {
        println("Second child throws an exception")
        throw ArithmeticException()
    }
    one.await() + two.await()
}
```

需要注意协程 one 和正在等待的父级是如何在协程 two 失败时取消的

```kotlin
Second child throws an exception
First child was cancelled
Computation failed with ArithmeticException
```