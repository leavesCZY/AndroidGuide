> 最近一直在了解关于**kotlin协程**的知识，那最好的学习资料自然是官方提供的学习文档了，看了看后我就萌生了翻译官方文档的想法。前后花了要接近一个月时间，一共九篇文章，在这里也分享出来，希望对读者有所帮助。个人知识所限，有些翻译得不是太顺畅，也希望读者能提出意见
> 
> 协程官方文档：[coroutines-guide](https://github.com/Kotlin/kotlinx.coroutines/blob/master/coroutines-guide.md)
>
> 协程官方文档中文翻译：[coroutines-cn-guide](https://github.com/leavesC/AndroidAllGuide/tree/master/kotlin_coroutine)
> 
> 协程官方文档中文译者：[leavesC](https://github.com/leavesC)

[TOC]

Deferred 值提供了在协程之间传递单个值的方便方法，而通道（Channels）提供了一种传输值流的方法

### 一、通道基础（Channel basics）

通道在概念上非常类似于 ```BlockingQueue```，它们之间的一个关键区别是：通道有一个挂起的 send 函数和一个挂起的 receive 函数，而不是一个阻塞的 put 操作和一个阻塞的 take 操作

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

fun main() = runBlocking {
//sampleStart
    val channel = Channel<Int>()
    launch {
        // this might be heavy CPU-consuming computation or async logic, we'll just send five squares
        for (x in 1..5) channel.send(x * x)
    }
    // here we print five received integers:
    repeat(5) { println(channel.receive()) }
    println("Done!")
//sampleEnd
}
```

输出结果是:

```kotlin
1
4
9
16
25
Done!
```

### 二、关闭和迭代通道（Closing and iteration over channels）

与队列不同，通道可以关闭，以此来表明元素已发送完成。在接收方，使用常规的 for 循环从通道接收元素是比较方便的

从概念上讲，close 类似于向通道发送一个特殊的 cloase 标记。一旦接收到这个 close 标记，迭代就会停止，因此可以保证接收到 close 之前发送的所有元素：

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

fun main() = runBlocking {
//sampleStart
    val channel = Channel<Int>()
    launch {
        for (x in 1..5) channel.send(x * x)
        channel.close() // we're done sending
    }
    // here we print received values using `for` loop (until the channel is closed)
    for (y in channel) println(y)
    println("Done!")
//sampleEnd
}
```

### 三、构建通道生产者（Building channel producers）

协程生成元素序列(sequence )的模式非常常见。这是可以经常在并发编程中发现的生产者-消费者模式的一部分。你可以将这样一个生产者抽象为一个以 channel 为参数的函数，但这与必须从函数返回结果的常识相反

有一个方便的名为 product 的协程构造器，它使得在 producer 端执行该操作变得很容易；还有一个扩展函数 consumerEach，它替换了consumer 端的 for 循环：

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

fun CoroutineScope.produceSquares(): ReceiveChannel<Int> = produce {
    for (x in 1..5) send(x * x)
}

fun main() = runBlocking {
//sampleStart
    val squares = produceSquares()
    squares.consumeEach { println(it) }
    println("Done!")
//sampleEnd
}
```

### 四、管道（Pipelines）

管道是一种模式，是一个协程正在生成的可能是无穷多个元素的值流

```kotlin
fun CoroutineScope.produceNumbers() = produce<Int> {
    var x = 1
    while (true) send(x++) // infinite stream of integers starting from 1
}
```

存在一个或多个协程对值流进行取值，进行一些处理并产生一些其它结果。在下面的示例中，每个返回值也是入参值（数字）的平方值

```kotlin
fun CoroutineScope.square(numbers: ReceiveChannel<Int>): ReceiveChannel<Int> = produce {
    for (x in numbers) send(x * x)
}
```

启动并连接整个管道：

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

fun main() = runBlocking {
//sampleStart
    val numbers = produceNumbers() // produces integers from 1 and on
    val squares = square(numbers) // squares integers
    repeat(5) {
        println(squares.receive()) // print first five
    }
    println("Done!") // we are done
    coroutineContext.cancelChildren() // cancel children coroutines
//sampleEnd
}

fun CoroutineScope.produceNumbers() = produce<Int> {
    var x = 1
    while (true) send(x++) // infinite stream of integers starting from 1
}

fun CoroutineScope.square(numbers: ReceiveChannel<Int>): ReceiveChannel<Int> = produce {
    for (x in numbers) send(x * x)
}
```

> 创建协程的所有函数都被定义为 CoroutineScope 的扩展，因此我们可以依赖结构化并发来确保应用程序中没有延迟的全局协程

### 五、使用管道的素数（Prime numbers with pipeline）

让我们以一个使用协程管道生成素数的例子，将管道发挥到极致。我们从一个无限的数字序列开始

```kotlin
fun CoroutineScope.numbersFrom(start: Int) = produce<Int> {
    var x = start
    while (true) send(x++) // infinite stream of integers from start
}
```

以下管道过滤传入的数字流，删除所有可被给定素数整除的数字：

```kotlin
fun CoroutineScope.filter(numbers: ReceiveChannel<Int>, prime: Int) = produce<Int> {
    for (x in numbers) if (x % prime != 0) send(x)
}
```

现在，我们通过从2开始一个数字流，从当前通道获取一个质数，并为找到的每个质数启动新的管道：

```kotlin
numbersFrom(2) -> filter(2) -> filter(3) -> filter(5) -> filter(7) ... 
```

下面的示例代码打印了前十个质数，在主线程的上下文中运行整个管道。因为所有的协程都是在主 runBlocking 协程的范围内启动的，所以我们不必保留所有已启动的协程的显式引用。我们使用扩展函数 cancelChildren 来取消打印前十个质数后的所有子协程

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

fun main() = runBlocking {
//sampleStart
    var cur = numbersFrom(2)
    repeat(10) {
        val prime = cur.receive()
        println(prime)
        cur = filter(cur, prime)
    }
    coroutineContext.cancelChildren() // cancel all children to let main finish
//sampleEnd    
}

fun CoroutineScope.numbersFrom(start: Int) = produce<Int> {
    var x = start
    while (true) send(x++) // infinite stream of integers from start
}

fun CoroutineScope.filter(numbers: ReceiveChannel<Int>, prime: Int) = produce<Int> {
    for (x in numbers) if (x % prime != 0) send(x)
}
```

运行结果：

```kotlin
2
3
5
7
11
13
17
19
23
29
```

注意，你可以使用标准库中的 iterator 协程构造器来构建相同的管道。将 product 替换为 iterator，send 替换为 yield，receive 替换为 next，ReceiveChannel 替换为 iterator，并去掉协程作用域。你也不需要再使用 runBlocking 。但是，使用如上所示的通道的管道的好处是，如果在 Dispatchers.Default 上下文中运行它，它实际上可以利用多个 CPU 来执行代码  

但无论如何，如上所述的替代方案也是一个非常不切实际的来寻找素数的方法。实际上，管道确实涉及一些其他挂起调用（如对远程服务的异步调用），并且这些管道不能使用 sequence/iterator 来构建，因为它们不允许任意挂起，而 product 是完全异步的

### 六、扇出（Fan-out）

多个协程可以从同一个通道接收数据，在它们之间分配任务。让我们从一个周期性地生成整数（每秒10个数）的 producer 协程开始：

```kotlin
fun CoroutineScope.produceNumbers() = produce<Int> {
    var x = 1 // start from 1
    while (true) {
        send(x++) // produce next
        delay(100) // wait 0.1s
    }
}
```

然后我们可以有多个处理器(processor)协程。在本例中，他们只需打印他们的 id 和接收的数字：

```kotlin
fun CoroutineScope.launchProcessor(id: Int, channel: ReceiveChannel<Int>) = launch {
    for (msg in channel) {
        println("Processor #$id received $msg")
    }    
}
```

现在让我们启动5个处理器，让它们工作几乎一秒钟。看看会发生什么：

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

fun main() = runBlocking<Unit> {
//sampleStart
    val producer = produceNumbers()
    repeat(5) { launchProcessor(it, producer) }
    delay(950)
    producer.cancel() // cancel producer coroutine and thus kill them all
//sampleEnd
}

fun CoroutineScope.produceNumbers() = produce<Int> {
    var x = 1 // start from 1
    while (true) {
        send(x++) // produce next
        delay(100) // wait 0.1s
    }
}

fun CoroutineScope.launchProcessor(id: Int, channel: ReceiveChannel<Int>) = launch {
    for (msg in channel) {
        println("Processor #$id received $msg")
    }    
}
```

尽管接收每个特定整数的处理器 id 可能不同，但运行结果将类似于以下输出：

```kotlin
Processor #2 received 1
Processor #4 received 2
Processor #0 received 3
Processor #1 received 4
Processor #3 received 5
Processor #2 received 6
Processor #4 received 7
Processor #0 received 8
Processor #1 received 9
Processor #3 received 10
```

请注意，取消 producer 协程会关闭其通道，从而最终终止 processor  协程正在执行的通道上的迭代

另外，请注意我们如何使用 for 循环在通道上显式迭代以在 launchProcessor 代码中执行 fan-out。与 consumeEach 不同，这个 for 循环模式在多个协程中使用是完全安全的。如果其中一个 processor  协程失败，则其他处理器仍将处理通道，而通过 consumeEach  写入的处理器总是在正常或异常完成时消费（取消）底层通道

### 七、扇入（Fan-in）

多个协程可以发送到同一个通道。例如，有一个字符串通道和一个挂起函数，函数以指定的延迟将指定的字符串重复发送到此通道：

```kotlin
suspend fun sendString(channel: SendChannel<String>, s: String, time: Long) {
    while (true) {
        delay(time)
        channel.send(s)
    }
}
```

现在，让我们看看如果启动两个协程来发送字符串会发生什么情况（在本例中，我们将它们作为主协程的子协程，在主线程的上下文中启动）：

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

fun main() = runBlocking {
//sampleStart
    val channel = Channel<String>()
    launch { sendString(channel, "foo", 200L) }
    launch { sendString(channel, "BAR!", 500L) }
    repeat(6) { // receive first six
        println(channel.receive())
    }
    coroutineContext.cancelChildren() // cancel all children to let main finish
//sampleEnd
}

suspend fun sendString(channel: SendChannel<String>, s: String, time: Long) {
    while (true) {
        delay(time)
        channel.send(s)
    }
}
```

运行结果：

```kotlin
foo
foo
BAR!
foo
foo
BAR!
```

### 八、带缓冲的通道（Buffered channels）

到目前为止显示的通道都没有缓冲区。无缓冲通道在发送方和接收方同时调用发送和接收操作时传输元素。如果先调用 send，则在调用 receive 之前会将其挂起；如果先调用 receive ，则在调用 send 之前会将其挂起

Channel() 工厂函数和 produce 构建器都采用可选的参数 ```capacity```  来指定缓冲区大小。 缓冲用于允许发送者在挂起之前发送多个元素，类似于具有指定容量的 BlockingQueue，它在缓冲区已满时才阻塞

查看以下代码的效果：

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

fun main() = runBlocking<Unit> {
//sampleStart
    val channel = Channel<Int>(4) // create buffered channel
    val sender = launch { // launch sender coroutine
        repeat(10) {
            println("Sending $it") // print before sending each element
            channel.send(it) // will suspend when buffer is full
        }
    }
    // don't receive anything... just wait....
    delay(1000)
    sender.cancel() // cancel sender coroutine
//sampleEnd    
}
```

使用了容量为4的缓冲通道，所以将打印五次：

```kotlin
Sending 0
Sending 1
Sending 2
Sending 3
Sending 4
```

前四个元素被添加到缓冲区内，sender 在尝试发送第五个元素时挂起

### 九、通道是公平的（Channels are fair）

对通道的发送和接收操作，对于从多个协程调用它们的顺序是公平的。它们按先入先出的顺序提供，例如，先调用 receive 的协程先获取到元素。在下面的示例中，两个协程 “ping” 和 “pong” 从共享的 “table” 通道接收 “ball” 对象

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

//sampleStart
data class Ball(var hits: Int)

fun main() = runBlocking {
    val table = Channel<Ball>() // a shared table
    launch { player("ping", table) }
    launch { player("pong", table) }
    table.send(Ball(0)) // serve the ball
    delay(1000) // delay 1 second
    coroutineContext.cancelChildren() // game over, cancel them
}

suspend fun player(name: String, table: Channel<Ball>) {
    for (ball in table) { // receive the ball in a loop
        ball.hits++
        println("$name $ball")
        delay(300) // wait a bit
        table.send(ball) // send the ball back
    }
}
//sampleEnd
```

“ping” 协程首先开始运行，所以它是第一个接收到 ball 的。即使 “ping” 协程在将 ball 重新送回给 table 后又立即开始进行 receive，但 ball 还是会被 “pong” 接收到，因为它已经先在等待接收了：

```kotlin
ping Ball(hits=1)
pong Ball(hits=2)
ping Ball(hits=3)
pong Ball(hits=4)
```

请注意，有时由于所使用的执行者的性质，通道可能会产生看起来不公平的执行效果。有关详细信息，请参阅此 [issue](https://github.com/Kotlin/kotlinx.coroutines/issues/111)

### 十、计时器通道（Ticker channels）

计时器通道是一种特殊的会合(rendezvous)通道，自该通道的最后一次消耗以来，每次给定的延迟时间结束后都将返回 Unit 值。尽管它看起来是无用处的，但它是一个有用的构建块，可以创建复杂的基于时间的 produce 管道和进行窗口化操作以及其它时间相关的处理。计时器通道可用于 select 执行 “on tick” 操作

要创建这样的通道，请使用工厂方法 ticker。如果不需要通道发送更多元素了，请对其使用 ReceiveChannel.cancel 取消发送

现在让我们看看它在实践中是如何工作的：

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

fun main() = runBlocking<Unit> {
    val tickerChannel = ticker(delayMillis = 100, initialDelayMillis = 0) // create ticker channel
    var nextElement = withTimeoutOrNull(1) { tickerChannel.receive() }
    println("Initial element is available immediately: $nextElement") // initial delay hasn't passed yet

    nextElement = withTimeoutOrNull(50) { tickerChannel.receive() } // all subsequent elements has 100ms delay
    println("Next element is not ready in 50 ms: $nextElement")

    nextElement = withTimeoutOrNull(60) { tickerChannel.receive() }
    println("Next element is ready in 100 ms: $nextElement")

    // Emulate large consumption delays
    println("Consumer pauses for 150ms")
    delay(150)
    // Next element is available immediately
    nextElement = withTimeoutOrNull(1) { tickerChannel.receive() }
    println("Next element is available immediately after large consumer delay: $nextElement")
    // Note that the pause between `receive` calls is taken into account and next element arrives faster
    nextElement = withTimeoutOrNull(60) { tickerChannel.receive() } 
    println("Next element is ready in 50ms after consumer pause in 150ms: $nextElement")

    tickerChannel.cancel() // indicate that no more elements are needed
}
```

运行结果：

```kotlin
Initial element is available immediately: kotlin.Unit
Next element is not ready in 50 ms: null
Next element is ready in 100 ms: kotlin.Unit
Consumer pauses for 150ms
Next element is available immediately after large consumer delay: kotlin.Unit
Next element is ready in 50ms after consumer pause in 150ms: kotlin.Unit
```

请注意，ticker 能感知到消费端可能处于暂停状态，并且在默认的情况下，如果发生暂停，将会延迟下一个元素的生成，尝试保持生成元素的固定速率

可选的，ticker 函数的 mode 参数可以指定为 TickerMode.FIXED_DELAY，以保证元素之间的固定延迟