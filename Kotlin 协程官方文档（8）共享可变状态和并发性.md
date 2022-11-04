> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

> 最近一直在了解关于 **Kotlin协程** 的知识，那最好的学习资料自然是官方提供的学习文档了，看了看后我就萌生了翻译官方文档的想法。前后花了要接近一个月时间，一共九篇文章，在这里也分享出来，希望对读者有所帮助。个人知识所限，有些翻译得不是太顺畅，也希望读者能提出意见
> 
> 协程官方文档：[coroutines-guide](https://github.com/Kotlin/kotlinx.coroutines/blob/master/coroutines-guide.md)

可以使用多线程调度器（如 Dispatchers.Default）并发执行协程，它呈现了所有常见的并发问题。主要问题是对共享可变状态的同步访问。在协程作用域中解决这个问题的一些方法类似于多线程世界中的方法，但有一些其它方法是独有的

# 一、一个问题

让我们启动一百个协程，都做同样的操作一千次。我们还将计算它们的完成时间，以便进一步比较：

```kotlin
suspend fun massiveRun(action: suspend () -> Unit) {
    val n = 100  // number of coroutines to launch
    val k = 1000 // times an action is repeated by each coroutine
    val time = measureTimeMillis {
        coroutineScope { // scope for coroutines 
            repeat(n) {
                launch {
                    repeat(k) { action() }
                }
            }
        }
    }
    println("Completed ${n * k} actions in $time ms")    
}
```

我们从一个非常简单的操作开始，该操作使用多线程调度器 Dispatchers.Default，并增加一个共享的可变变量

```kotlin
import kotlinx.coroutines.*
import kotlin.system.*    

suspend fun massiveRun(action: suspend () -> Unit) {
    val n = 100  // number of coroutines to launch
    val k = 1000 // times an action is repeated by each coroutine
    val time = measureTimeMillis {
        coroutineScope { // scope for coroutines 
            repeat(n) {
                launch {
                    repeat(k) { action() }
                }
            }
        }
    }
    println("Completed ${n * k} actions in $time ms")    
}

//sampleStart
var counter = 0

fun main() = runBlocking {
    withContext(Dispatchers.Default) {
        massiveRun {
            counter++
        }
    }
    println("Counter = $counter")
}
//sampleEnd    
```

最后会打印出什么呢？不太可能打印出 “Counter=100000”，因为100个协程从多个线程并发地递增 counter 而不进行任何同步。

# 二、Volatiles 是没有作用的

有一种常见的误解是：将变量标记为 volatile 可以解决并发问题。让我们试试：

```kotlin
import kotlinx.coroutines.*
import kotlin.system.*

suspend fun massiveRun(action: suspend () -> Unit) {
    val n = 100  // number of coroutines to launch
    val k = 1000 // times an action is repeated by each coroutine
    val time = measureTimeMillis {
        coroutineScope { // scope for coroutines 
            repeat(n) {
                launch {
                    repeat(k) { action() }
                }
            }
        }
    }
    println("Completed ${n * k} actions in $time ms")    
}

//sampleStart
@Volatile // in Kotlin `volatile` is an annotation 
var counter = 0

fun main() = runBlocking {
    withContext(Dispatchers.Default) {
        massiveRun {
            counter++
        }
    }
    println("Counter = $counter")
}
//sampleEnd    
```

这段代码运行得比较慢，但是我们在最后仍然没有得到“Counter=100000”，因为 volatile 变量保证了可线性化（这是“atomic”的一个技术术语）对相应变量的读写，但不提供更大行为的原子性（在我们的例子中指递增操作）

# 三、线程安全的数据结构

对线程和协程都有效的一个解决方案是使用线程安全的（也称为同步、可线性化或原子）数据结构，该结构为需要在共享状态上执行的相应操作提供所有必要的同步保障。对于一个简单的计数器，我们可以使用 AtomicInteger 类，该类具有保证原子性的 incrementAndGet 方法

```kotlin
import kotlinx.coroutines.*
import java.util.concurrent.atomic.*
import kotlin.system.*

suspend fun massiveRun(action: suspend () -> Unit) {
    val n = 100  // number of coroutines to launch
    val k = 1000 // times an action is repeated by each coroutine
    val time = measureTimeMillis {
        coroutineScope { // scope for coroutines 
            repeat(n) {
                launch {
                    repeat(k) { action() }
                }
            }
        }
    }
    println("Completed ${n * k} actions in $time ms")    
}

//sampleStart
var counter = AtomicInteger()

fun main() = runBlocking {
    withContext(Dispatchers.Default) {
        massiveRun {
            counter.incrementAndGet()
        }
    }
    println("Counter = $counter")
}
//sampleEnd    
```

这是解决这个特殊问题的最快方法。它适用于普通计数器、集合、队列和其他标准数据结构及其基本操作。但是，它不容易扩展到复杂的状态或没有实现好了的线程安全的复杂操作

# 四、以细粒度限制线程

线程限制是解决共享可变状态问题的一种方法，其中对特定共享状态的所有访问都限制在一个线程内。它通常用于 UI 应用程序，其中所有的 UI 状态都限制在“单个事件分派”或“应用程序线程”中。通过使用单线程上下文，可以很容易地使用协程来实现上述的计数器

```kotlin
import kotlinx.coroutines.*
import kotlin.system.*

suspend fun massiveRun(action: suspend () -> Unit) {
    val n = 100  // number of coroutines to launch
    val k = 1000 // times an action is repeated by each coroutine
    val time = measureTimeMillis {
        coroutineScope { // scope for coroutines 
            repeat(n) {
                launch {
                    repeat(k) { action() }
                }
            }
        }
    }
    println("Completed ${n * k} actions in $time ms")    
}

//sampleStart
val counterContext = newSingleThreadContext("CounterContext")
var counter = 0

fun main() = runBlocking {
    withContext(Dispatchers.Default) {
        massiveRun {
            // confine each increment to a single-threaded context
            withContext(counterContext) {
                counter++
            }
        }
    }
    println("Counter = $counter")
}
//sampleEnd      
```

这段代码运行得非常缓慢，因为它执行细粒度的线程限制。每个单独的增值操作都使用 withContext(counterContext) 从多线程 Dispatchers.Default 上下文切换到单线程上下文

# 五、以粗粒度限制线程

在实践中，线程限制是在比较大的范围内执行的，例如，更新状态的逻辑的范围被限制在单个线程中。下面的示例就是这样做的，首先在单线程上下文中运行每个协程

```kotlin
import kotlinx.coroutines.*
import kotlin.system.*

suspend fun massiveRun(action: suspend () -> Unit) {
    val n = 100  // number of coroutines to launch
    val k = 1000 // times an action is repeated by each coroutine
    val time = measureTimeMillis {
        coroutineScope { // scope for coroutines 
            repeat(n) {
                launch {
                    repeat(k) { action() }
                }
            }
        }
    }
    println("Completed ${n * k} actions in $time ms")    
}

//sampleStart
val counterContext = newSingleThreadContext("CounterContext")
var counter = 0

fun main() = runBlocking {
    // confine everything to a single-threaded context
    withContext(counterContext) {
        massiveRun {
            counter++
        }
    }
    println("Counter = $counter")
}
//sampleEnd     
```

现在这段代码的运行速度会快得多，并产生了正确的结果

# 六、互斥

互斥问题的解决方案是保护共享状态的所有修改操作，其中的关键代码永远不会同时执行。在一个阻塞的世界中，通常会使用 ```synchronized``` 或 ```ReentrantLock```。协程的替换方案称为互斥(Mutex)。它具有 ```lock``` 和 ```unlock``` 函数以划定一个关键位置。关键的区别在于 ```Mutex.lock()``` 是一个挂起函数。它不会阻塞线程

还有一个扩展函数 withLock 可以方便地来实现 ```mutex.lock(); try {...} finally { mutex.unlock() } ```

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.*
import kotlin.system.*

suspend fun massiveRun(action: suspend () -> Unit) {
    val n = 100  // number of coroutines to launch
    val k = 1000 // times an action is repeated by each coroutine
    val time = measureTimeMillis {
        coroutineScope { // scope for coroutines 
            repeat(n) {
                launch {
                    repeat(k) { action() }
                }
            }
        }
    }
    println("Completed ${n * k} actions in $time ms")    
}

//sampleStart
val mutex = Mutex()
var counter = 0

fun main() = runBlocking {
    withContext(Dispatchers.Default) {
        massiveRun {
            // protect each increment with lock
            mutex.withLock {
                counter++
            }
        }
    }
    println("Counter = $counter")
}
//sampleEnd
```

本例中的锁是细粒度的，因此它也付出了某些代价（消耗）。但是，在某些情况下这是一个很好的选择，比如你必须定期修改某些共享状态，但不具备修改共享状态所需的原生线程

# 七、Actors

actor 是一个实体，由一个协程、被限制并封装到这个协程中的状态以及一个与其它协程通信的通道组成。简单的 actor 可以写成函数，但具有复杂状态的 actor 更适合类

有一个 actor 协程构造器，它可以方便地将 actor 的 mailbox  channel 合并到其接收的消息的作用域中，并将 send channel 合并到生成的 job  对象中，以便可以将对 actor 的单个引用作为其句柄引有

使用 actor 的第一步是定义一类 actor 将要处理的消息。kotlin 的密封类非常适合这个目的。在 CounterMsg 密封类中，我们用 IncCounter 消息来定义递增计数器，用 GetCounter 消息来获取其值，后者需要返回值。为此，这里使用 CompletableDeferred communication primitive，它表示将来已知（通信）的单个值

```kotlin
// Message types for counterActor
sealed class CounterMsg
object IncCounter : CounterMsg() // one-way message to increment counter
class GetCounter(val response: CompletableDeferred<Int>) : CounterMsg() // a request with reply
```

然后，我们定义一个函数，该函数使用 actor 协程构造器来启动 actor：

```kotlin
// This function launches a new counter actor
fun CoroutineScope.counterActor() = actor<CounterMsg> {
    var counter = 0 // actor state
    for (msg in channel) { // iterate over incoming messages
        when (msg) {
            is IncCounter -> counter++
            is GetCounter -> msg.response.complete(counter)
        }
    }
}
```

代码很简单：

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlin.system.*

suspend fun massiveRun(action: suspend () -> Unit) {
    val n = 100  // number of coroutines to launch
    val k = 1000 // times an action is repeated by each coroutine
    val time = measureTimeMillis {
        coroutineScope { // scope for coroutines 
            repeat(n) {
                launch {
                    repeat(k) { action() }
                }
            }
        }
    }
    println("Completed ${n * k} actions in $time ms")    
}

// Message types for counterActor
sealed class CounterMsg
object IncCounter : CounterMsg() // one-way message to increment counter
class GetCounter(val response: CompletableDeferred<Int>) : CounterMsg() // a request with reply

// This function launches a new counter actor
fun CoroutineScope.counterActor() = actor<CounterMsg> {
    var counter = 0 // actor state
    for (msg in channel) { // iterate over incoming messages
        when (msg) {
            is IncCounter -> counter++
            is GetCounter -> msg.response.complete(counter)
        }
    }
}

//sampleStart
fun main() = runBlocking<Unit> {
    val counter = counterActor() // create the actor
    withContext(Dispatchers.Default) {
        massiveRun {
            counter.send(IncCounter)
        }
    }
    // send a message to get a counter value from an actor
    val response = CompletableDeferred<Int>()
    counter.send(GetCounter(response))
    println("Counter = ${response.await()}")
    counter.close() // shutdown the actor
}
//sampleEnd    
```

在什么上下文中执行 actor 本身并不重要（为了正确）。actor  是一个协程，并且协程是按顺序执行的，因此将状态限制到特定的协程可以解决共享可变状态的问题。实际上，actors 可以修改自己的私有状态，但只能通过消息相互影响（避免需要任何锁）

actor 比使用锁更为有效，因为在这种情况下，它总是有工作要做，根本不需要切换到不同的上下文

> 注意，actor 协程构造器是一个双重的 product 协程构造器 。actor 与它接收消息的通道相关联，而 producer 与向其发送元素的通道相关联