> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

> 最近一直在了解关于 **Kotlin协程** 的知识，那最好的学习资料自然是官方提供的学习文档了，看了看后我就萌生了翻译官方文档的想法。前后花了要接近一个月时间，一共九篇文章，在这里也分享出来，希望对读者有所帮助。个人知识所限，有些翻译得不是太顺畅，也希望读者能提出意见
> 
> 协程官方文档：[coroutines-guide](https://github.com/Kotlin/kotlinx.coroutines/blob/master/coroutines-guide.md)

select 表达式可以同时等待多个挂起函数，并选择第一个可用的函数来执行

> 选择表达式是 ```kotlinx.coroutines``` 的一个实验性的特性，这些 API 预计将在 ```kotlinx.coroutines``` 库的即将到来的更新中衍化，并可能会有突破性的变化

# 一、Selecting from channels

我们现在有两个字符串生产者：```fizz``` 和 ```buzz``` 。其中 ```fizz``` 每 300 毫秒生成一个字符串“Fizz”：

```kotlin
fun CoroutineScope.fizz() = produce<String> {
    while (true) { // sends "Fizz" every 300 ms
        delay(300)
        send("Fizz")
    }
}
```

接着 ```buzz``` 每 500 毫秒生成一个字符串“Buzz!”：

```kotlin
fun CoroutineScope.buzz() = produce<String> {
    while (true) { // sends "Buzz!" every 500 ms
        delay(500)
        send("Buzz!")
    }
}
```

使用挂起函数 receive，我们可以从两个通道接收其中一个的数据。但是 select 表达式允许我们使用其 onReceive 子句同时从两者接收：

```kotlin
suspend fun selectFizzBuzz(fizz: ReceiveChannel<String>, buzz: ReceiveChannel<String>) {
    select<Unit> { // <Unit> means that this select expression does not produce any result 
        fizz.onReceive { value ->  // this is the first select clause
            println("fizz -> '$value'")
        }
        buzz.onReceive { value ->  // this is the second select clause
            println("buzz -> '$value'")
        }
    }
}
```

让我们运行代码 7 次：

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.*

fun CoroutineScope.fizz() = produce<String> {
    while (true) { // sends "Fizz" every 300 ms
        delay(300)
        send("Fizz")
    }
}

fun CoroutineScope.buzz() = produce<String> {
    while (true) { // sends "Buzz!" every 500 ms
        delay(500)
        send("Buzz!")
    }
}

suspend fun selectFizzBuzz(fizz: ReceiveChannel<String>, buzz: ReceiveChannel<String>) {
    select<Unit> { // <Unit> means that this select expression does not produce any result 
        fizz.onReceive { value ->  // this is the first select clause
            println("fizz -> '$value'")
        }
        buzz.onReceive { value ->  // this is the second select clause
            println("buzz -> '$value'")
        }
    }
}

fun main() = runBlocking<Unit> {
//sampleStart
    val fizz = fizz()
    val buzz = buzz()
    repeat(7) {
        selectFizzBuzz(fizz, buzz)
    }
    coroutineContext.cancelChildren() // cancel fizz & buzz coroutines
//sampleEnd        
}
```

运行结果：

```kotlin
fizz -> 'Fizz'
buzz -> 'Buzz!'
fizz -> 'Fizz'
fizz -> 'Fizz'
buzz -> 'Buzz!'
fizz -> 'Fizz'
buzz -> 'Buzz!'
```

# 二、Selecting on close

当通道关闭时，select 中的 onReceive 子句会失败并导致相应的 select 引发异常。我们可以使用 onReceiveOrNull 子句在通道关闭时执行特定操作。下面的示例还显示了 select 是一个返回其查询方法结果的表达式：

```kotlin
suspend fun selectAorB(a: ReceiveChannel<String>, b: ReceiveChannel<String>): String =
    select<String> {
        a.onReceiveOrNull { value -> 
            if (value == null) 
                "Channel 'a' is closed" 
            else 
                "a -> '$value'"
        }
        b.onReceiveOrNull { value -> 
            if (value == null) 
                "Channel 'b' is closed"
            else    
                "b -> '$value'"
        }
    }
```

注意，onReceiveOrNull 是一个扩展函数，仅可用于具有不可为空元素的通道，这样就不会意外混淆**通道是已关闭**还是**返回了空值**这两种情况

让我们将其与生成四次“Hello”字符串的通道 ```a``` 和生成四次“World”字符串的通道 ```b``` 一起使用：

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.*

suspend fun selectAorB(a: ReceiveChannel<String>, b: ReceiveChannel<String>): String =
    select<String> {
        a.onReceiveOrNull { value -> 
            if (value == null) 
                "Channel 'a' is closed" 
            else 
                "a -> '$value'"
        }
        b.onReceiveOrNull { value -> 
            if (value == null) 
                "Channel 'b' is closed"
            else    
                "b -> '$value'"
        }
    }
    
fun main() = runBlocking<Unit> {
//sampleStart
    val a = produce<String> {
        repeat(4) { send("Hello $it") }
    }
    val b = produce<String> {
        repeat(4) { send("World $it") }
    }
    repeat(8) { // print first eight results
        println(selectAorB(a, b))
    }
    coroutineContext.cancelChildren()  
//sampleEnd      
}    
```

这段代码的结果非常有趣，所以我们将在细节中分析它：

```kotlin
a -> 'Hello 0'
a -> 'Hello 1'
b -> 'World 0'
a -> 'Hello 2'
a -> 'Hello 3'
b -> 'World 1'
Channel 'a' is closed
Channel 'a' is closed
```

从中可以观察到几点

首先，select 偏向于第一个子句。当同时可以选择多个子句时，将选择其中的第一个子句。在这里，两个通道都在不断地产生字符串，因此作为 select 中的第一个子句的通道获胜。但是，因为我们使用的是无缓冲通道，所以 a 在其发送调用时会不时地被挂起，从而给了 b 发送的机会

第二个观察结果是，当通道已经关闭时，onReceiveOrNull 将立即被选中

# 三、Selecting to send

select 表达式有 onSend 子句，可以与 selection 的偏向性质结合使用。
让我们写一个整数生产者的例子，当主通道上的消费者跟不上时，它会将其值发送到 ```side``` 通道：

```kotlin
fun CoroutineScope.produceNumbers(side: SendChannel<Int>) = produce<Int> {
    for (num in 1..10) { // produce 10 numbers from 1 to 10
        delay(100) // every 100 ms
        select<Unit> {
            onSend(num) {} // Send to the primary channel
            side.onSend(num) {} // or to the side channel     
        }
    }
}
```

消费者将会非常缓慢，每个数值处理需要 250 毫秒：

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.*

fun CoroutineScope.produceNumbers(side: SendChannel<Int>) = produce<Int> {
    for (num in 1..10) { // produce 10 numbers from 1 to 10
        delay(100) // every 100 ms
        select<Unit> {
            onSend(num) {} // Send to the primary channel
            side.onSend(num) {} // or to the side channel     
        }
    }
}

fun main() = runBlocking<Unit> {
//sampleStart
    val side = Channel<Int>() // allocate side channel
    launch { // this is a very fast consumer for the side channel
        side.consumeEach { println("Side channel has $it") }
    }
    produceNumbers(side).consumeEach { 
        println("Consuming $it")
        delay(250) // let us digest the consumed number properly, do not hurry
    }
    println("Done consuming")
    coroutineContext.cancelChildren()  
//sampleEnd      
}
```

让我们看看会发生什么：

```kotlin
Consuming 1
Side channel has 2
Side channel has 3
Consuming 4
Side channel has 5
Side channel has 6
Consuming 7
Side channel has 8
Side channel has 9
Consuming 10
Done consuming
```

# 四、Selecting deferred values

延迟值可以使用 onAwait 子句来查询。让我们启动一个异步函数，它在随机的延迟后会延迟返回字符串：

```kotlin
fun CoroutineScope.asyncString(time: Int) = async {
    delay(time.toLong())
    "Waited for $time ms"
}
```

让我们随机启动十余个异步函数，每个都延迟随机的时间

```kotlin
fun CoroutineScope.asyncStringsList(): List<Deferred<String>> {
    val random = Random(3)
    return List(12) { asyncString(random.nextInt(1000)) }
}
```

现在，main 函数等待它们中的第一个完成，并统计仍处于活动状态的延迟值的数量。注意，我们在这里使用 ```select``` 表达式事实上是一种 Kotlin DSL，因此我们可以使用任意代码为它提供子句。在本例中，我们遍历一个延迟值列表，为每个延迟值提供 ```onAwait``` 子句。

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.selects.*
import java.util.*
    
fun CoroutineScope.asyncString(time: Int) = async {
    delay(time.toLong())
    "Waited for $time ms"
}

fun CoroutineScope.asyncStringsList(): List<Deferred<String>> {
    val random = Random(3)
    return List(12) { asyncString(random.nextInt(1000)) }
}

fun main() = runBlocking<Unit> {
//sampleStart
    val list = asyncStringsList()
    val result = select<String> {
        list.withIndex().forEach { (index, deferred) ->
            deferred.onAwait { answer ->
                "Deferred $index produced answer '$answer'"
            }
        }
    }
    println(result)
    val countActive = list.count { it.isActive }
    println("$countActive coroutines are still active")
//sampleEnd
}
```

输出结果：

```kotlin
Deferred 4 produced answer 'Waited for 128 ms'
11 coroutines are still active
```

# 五、Switch over a channel of deferred values

现在我们来编写一个通道生产者函数，它消费一个产生延迟字符串的通道，并等待每个接收的延迟值，但它只在下一个延迟值到达或者通道关闭之前处于运行状态。此示例将 onReceiveOrNull 和 onAwait 子句放在同一个 ```select``` 中：

```kotlin
fun CoroutineScope.switchMapDeferreds(input: ReceiveChannel<Deferred<String>>) = produce<String> {
    var current = input.receive() // start with first received deferred value
    while (isActive) { // loop while not cancelled/closed
        val next = select<Deferred<String>?> { // return next deferred value from this select or null
            input.onReceiveOrNull { update ->
                update // replaces next value to wait
            }
            current.onAwait { value ->  
                send(value) // send value that current deferred has produced
                input.receiveOrNull() // and use the next deferred from the input channel
            }
        }
        if (next == null) {
            println("Channel was closed")
            break // out of loop
        } else {
            current = next
        }
    }
}
```

为了测试它，我们将用一个简单的异步函数，它在特定的延迟后返回特定的字符串：

```kotlin
fun CoroutineScope.asyncString(str: String, time: Long) = async {
    delay(time)
    str
}
```

main 函数只是启动一个协程来打印 ```switchMapDeferreds``` 的结果并向它发送一些测试数据：

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.*
    
fun CoroutineScope.switchMapDeferreds(input: ReceiveChannel<Deferred<String>>) = produce<String> {
    var current = input.receive() // start with first received deferred value
    while (isActive) { // loop while not cancelled/closed
        val next = select<Deferred<String>?> { // return next deferred value from this select or null
            input.onReceiveOrNull { update ->
                update // replaces next value to wait
            }
            current.onAwait { value ->  
                send(value) // send value that current deferred has produced
                input.receiveOrNull() // and use the next deferred from the input channel
            }
        }
        if (next == null) {
            println("Channel was closed")
            break // out of loop
        } else {
            current = next
        }
    }
}

fun CoroutineScope.asyncString(str: String, time: Long) = async {
    delay(time)
    str
}

fun main() = runBlocking<Unit> {
//sampleStart
    val chan = Channel<Deferred<String>>() // the channel for test
    launch { // launch printing coroutine
        for (s in switchMapDeferreds(chan)) 
            println(s) // print each received string
    }
    chan.send(asyncString("BEGIN", 100))
    delay(200) // enough time for "BEGIN" to be produced
    chan.send(asyncString("Slow", 500))
    delay(100) // not enough time to produce slow
    chan.send(asyncString("Replace", 100))
    delay(500) // give it time before the last one
    chan.send(asyncString("END", 500))
    delay(1000) // give it time to process
    chan.close() // close the channel ... 
    delay(500) // and wait some time to let it finish
//sampleEnd
}
```

代码的执行结果：

```kotlin
BEGIN
Replace
END
Channel was closed
```
