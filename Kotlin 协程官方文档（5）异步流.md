> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

> 最近一直在了解关于 **Kotlin协程** 的知识，那最好的学习资料自然是官方提供的学习文档了，看了看后我就萌生了翻译官方文档的想法。前后花了要接近一个月时间，一共九篇文章，在这里也分享出来，希望对读者有所帮助。个人知识所限，有些翻译得不是太顺畅，也希望读者能提出意见
> 
> 协程官方文档：[coroutines-guide](https://github.com/Kotlin/kotlinx.coroutines/blob/master/coroutines-guide.md)

挂起函数可以异步返回单个值，但如何返回多个异步计算值呢？这就是 kotlin Flows（流） 的用处了

# 一、表示多个值

可以使用集合在 kotlin 中表示多个值。例如，有一个函数 foo()，它返回包含三个数字的 List，然后使用 forEach 打印它们

```kotlin
fun foo(): List<Int> = listOf(1, 2, 3)
 
fun main() {
    foo().forEach { value -> println(value) } 
}
```

输出结果：

```kotlin
1
2
3
```

## 1.1、序列

如果我们使用一些 CPU 消耗型 的阻塞代码（每次计算需要100毫秒）来计算数字，那么我们可以使用一个序列(Sequence)来表示数字：

```kotlin
fun foo(): Sequence<Int> = sequence {
    // sequence builder
    for (i in 1..3) {
        Thread.sleep(100) // pretend we are computing it
        yield(i) // yield next value
    }
}

fun main() {
    foo().forEach { value -> println(value) }
}
```

这段代码输出相同的数字列表，但每打印一个数字前都需要等待100毫秒

## 1.2、挂起函数

上一节的代码的计算操作会阻塞运行代码的主线程。当这些值由异步代码计算时，我们可以用 suspend 修饰符标记函数 foo，以便它可以在不阻塞的情况下执行其工作，并将结果作为列表返回

```kotlin
import kotlinx.coroutines.*                 
                           
//sampleStart
suspend fun foo(): List<Int> {
    delay(1000) // pretend we are doing something asynchronous here
    return listOf(1, 2, 3)
}

fun main() = runBlocking<Unit> {
    foo().forEach { value -> println(value) } 
}
//sampleEnd
```

这段代码在等待一秒后输出数字

## 1.3、Flows

使用 List< Int > 作为返回值类型，意味着我们只能同时返回所有值。为了表示异步计算的值流，我们可以使用 Flow< Int > 类型，就像同步计算值的 Sequence< Int > 类型一样

```kotlin
//sampleStart
fun foo(): Flow<Int> = flow { // flow builder
    for (i in 1..3) {
        delay(100) // pretend we are doing something useful here
        emit(i) // emit next value
    }
}

fun main() = runBlocking<Unit> {
    // Launch a concurrent coroutine to check if the main thread is blocked
    launch {
        for (k in 1..3) {
            println("I'm not blocked $k")
            delay(100)
        }
    }
    // Collect the flow
    foo().collect { value -> println(value) }
}
//sampleEnd
```

此代码在打印每个数字前等待100毫秒，但不会阻塞主线程。通过从主线程中运行的单独协程中每隔100毫秒打印了一次 “I'm not blocked”，可以验证这一点：

```kotlin
I'm not blocked 1
1
I'm not blocked 2
2
I'm not blocked 3
3
```

请注意，代码与前面示例中的 Flow 有以下不同：

- Flow 类型的构造器函数名为 flow
- flow{...} 中的代码块可以挂起
- foo 函数不再标记 suspend 修饰符
- 值通过 emit 函数从流中发出
- 通过 collect 函数从 flow 中取值

> 我们可以用 Thread.sleep 来代替 flow{...} 中的 delay，可以看到在这种情况下主线程被阻塞住了

# 二、流是冷的

Flows 是冷流（cold streams），类似于序列（sequences），flow builder 中的代码在开始收集流值之前不会运行。在下面的示例中可以清楚地看到这一点：

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

//sampleStart      
fun foo(): Flow<Int> = flow { 
    println("Flow started")
    for (i in 1..3) {
        delay(100)
        emit(i)
    }
}

fun main() = runBlocking<Unit> {
    println("Calling foo...")
    val flow = foo()
    println("Calling collect...")
    flow.collect { value -> println(value) } 
    println("Calling collect again...")
    flow.collect { value -> println(value) } 
}
//sampleEnd
```

运行结果：

```kotlin
Calling foo...
Calling collect...
Flow started
1
2
3
Calling collect again...
Flow started
1
2
3
```

这是 foo() 函数（返回了 flow）未标记 ```suspend``` 修饰符的一个关键原因。```foo()``` 本身返回很快，不会进行任何等待。flow 每次收集时都会启动，这就是我们再次调用 ```collect``` 时会看到“flow started”的原因

# 三、取消流

Flow 采用和协程取同样的协作取消。但是，Flow 实现基础并没有引入额外的取消点，它对于取消操作是完全透明的。通常，流的收集操作可以在当流在一个可取消的挂起函数（如 delay）中挂起的时候取消，否则不能取消

以下示例展示了在 withTimeoutOrNull 块中流如何在超时时被取消并停止执行

```kotlin
//sampleStart
fun foo(): Flow<Int> = flow {
    for (i in 1..3) {
        delay(100)
        println("Emitting $i")
        emit(i)
    }
}

fun main() = runBlocking<Unit> {
    withTimeoutOrNull(250) {
        // Timeout after 250ms
        foo().collect { value -> println(value) }
    }
    println("Done")
}
//sampleEnd
```

注意，foo() 函数中的 Flow 只传出两个数字，得到以下输出：

```kotlin
Emitting 1
1
Emitting 2
2
Done
```

相对应的，可以注释掉 flow 中的 delay 函数，并增大 for 循环的循环范围，此时可以发现 flow 没有被取消，因为 flow 中没有引入额外的挂起点

```kotlin
//sampleStart
fun foo(): Flow<Int> = flow {
    for (i in 1..Int.MAX_VALUE) {
//        delay(100)
        println("Emitting $i")
        emit(i)
    }
}

fun main() = runBlocking<Unit> {
    withTimeoutOrNull(250) {
        // Timeout after 250ms
        foo().collect { value -> println(value) }
    }
    println("Done")
}
//sampleEnd
```

# 四、流构建器

前面例子中的 flow{...} 是最基础的一个流构建器，还有其它的构建器可以更容易地声明流：

- flowOf() 定义了一个发出固定值集的流构建器
- 可以使用扩展函数 ```.asFlow()``` 将各种集合和序列转换为流

因此，从流中打印从 1 到 3 的数字的例子可以改写成：

```kotlin
fun main() = runBlocking<Unit> {
    //sampleStart
    // Convert an integer range to a flow
    (1..3).asFlow().collect { value -> println(value) }
    //sampleEnd
}   
```

# 五、中间流运算符

可以使用运算符来转换流，就像使用集合和序列一样。中间运算符应用于上游流并返回下游流。这些运算符是冷操作符，和流一样。此类运算符本身不是挂起函数，它工作得很快，其返回一个新的转换后的流，但引用仅包含对新流的操作定义，并不马上进行转换

基础运算符有着熟悉的名称，例如 map 和 filter。流运算符和序列的重要区别在于流运算符中的代码可以调用挂起函数

例如，可以使用 map 运算符将传入请求流映射为结果值，即使执行请求是由挂起函数实现的长时间运行的操作：

```kotlin
//sampleStart
suspend fun performRequest(request: Int): String {
    delay(1000) // imitate long-running asynchronous work
    return "response $request"
}

fun main() = runBlocking<Unit> {
    (1..3).asFlow() // a flow of requests
        .map { request -> performRequest(request) }
        .collect { response -> println(response) }
}
//sampleEnd
```

运行结果共有三行，每一秒打印一行输出

```kotlin
response 1
response 2
response 3
```

## 5.1、转换操作符

在流的转换运算符中，最常用的一个称为 ```transform```。它可以用来模拟简单的数据转换（就像 map 和 filter），以及实现更复杂的转换。使用 ```transform``` 运算符，我们可以发出任意次数的任意值

例如，通过使用 ```transform```，我们可以在执行长时间运行的异步请求之前发出一个字符串，并在该字符串后面跟随一个响应：

```kotlin
suspend fun performRequest(request: Int): String {
    delay(1000) // imitate long-running asynchronous work
    return "response $request"
}

fun main() = runBlocking<Unit> {
    //sampleStart
    (1..3).asFlow() // a flow of requests
        .transform { request ->
            emit("Making request $request")
            emit(performRequest(request))
        }
        .collect { response -> println(response) }
    //sampleEnd
}
```

输出值：

```kotlin
Making request 1
response 1
Making request 2
response 2
Making request 3
response 3
```

## 5.2、限长运算符

限长中间运算符在达到相应限制时取消流的执行。协程中的取消总是通过抛出异常来实现，这样所有的资源管理函数（例如 try { ... } finally { ... } ）就可以在取消时正常执行

```kotlin
//sampleStart
fun numbers(): Flow<Int> = flow {
    try {
        emit(1)
        emit(2)
        println("This line will not execute")
        emit(3)
    } finally {
        println("Finally in numbers")
    }
}

fun main() = runBlocking<Unit> {
    numbers()
        .take(2) // take only the first two
        .collect { value -> println(value) }
}
//sampleEnd
```

这段代码的输出清楚地显示了 numbers() 函数中的 flow{...} 函数体在发出第二个数字后就停止了：

```kotlin
1
2
Finally in numbers
```

# 六、流运算符

终端流运算符是用于启动流的挂起函数。```collect``` 是最基本的终端流运算符，但还有其它终端运算符，可以使得操作更加简便：

- 转换为各种集合，如 toList 和 toSet 函数
- first 运算符用于获取第一个值，single 运算符用于确保流发出单个值
- 使用 reduce 和 fold 将流还原为某个值

例如：

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun main() = runBlocking<Unit> {
//sampleStart         
    val sum = (1..5).asFlow()
        .map { it * it } // squares of numbers from 1 to 5                           
        .reduce { a, b -> a + b } // sum them (terminal operator)
    println(sum)
//sampleEnd     
}
```

输出单个值：

```kotlin
55
```

# 七、流是连续的

除非使用对多个流进行操作的特殊运算符，否则每个流的单独集合都是按顺序执行的。集合直接在调用终端运算符的协程中工作，默认情况下不会启动新的协程。每个发出的值都由所有中间运算符从上游到下游进行处理，然后在之后传递给终端运算符

请参阅以下示例，该示例过滤偶数并将其映射到字符串：

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun main() = runBlocking<Unit> {
//sampleStart         
    (1..5).asFlow()
        .filter {
            println("Filter $it")
            it % 2 == 0              
        }              
        .map { 
            println("Map $it")
            "string $it"
        }.collect { 
            println("Collect $it")
        }    
//sampleEnd                  
}
```

输出：

```kotlin
Filter 1
Filter 2
Map 2
Collect string 2
Filter 3
Filter 4
Map 4
Collect string 4
Filter 5
```

# 八、流上下文

流的收集总是在调用协程的上下文中执行。例如，如果存在 foo 流，则无论 foo 流的实现详细信息如何，以下代码都将在该开发者指定的上下文中执行：

```kotlin
withContext(context) {
    foo.collect { value ->
        println(value) // run in the specified context 
    }
}
```

流的这个特性称为上下文保留

所以，默认情况下，flow{...} 中的代码在相应流的收集器提供的上下文中运行。例如，观察 foo 的实现，它打印调用它的线程并发出三个数字：

```kotlin
fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")

//sampleStart
fun foo(): Flow<Int> = flow {
    log("Started foo flow")
    for (i in 1..3) {
        emit(i)
    }
}

fun main() = runBlocking<Unit> {
    foo().collect { value -> log("Collected $value") }
}
//sampleEnd
```

运行结果：

```kotlin
[main @coroutine#1] Started foo flow
[main @coroutine#1] Collected 1
[main @coroutine#1] Collected 2
[main @coroutine#1] Collected 3
```

由于 ```foo().collect``` 是在主线程调用的，所以 foo 流也是在主线程中调用。对于不关心执行上下文且不阻塞调用方的快速返回代码或者异步代码，这是完美的默认设置

## 8.1、错误地使用 withContext

但是，可能需要在 Dispatchers 的上下文中执行长时间运行的占用 CPU 的代码，可能需要在 Dispatchers.Main 的上下文中执行默认代码和 UI 更新。通常，withContext 用于在使用 kotlin 协程时更改代码中的上下文，但 ```fow{...}``` 中的代码必须遵守上下文本保留属性，并且不允许从其它上下文中触发

尝试运行以下代码：

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
                      
//sampleStart
fun foo(): Flow<Int> = flow {
    // The WRONG way to change context for CPU-consuming code in flow builder
    kotlinx.coroutines.withContext(Dispatchers.Default) {
        for (i in 1..3) {
            Thread.sleep(100) // pretend we are computing it in CPU-consuming way
            emit(i) // emit next value
        }
    }
}

fun main() = runBlocking<Unit> {
    foo().collect { value -> println(value) } 
}            
//sampleEnd
```

代码会生成以下异常：

```
Exception in thread "main" java.lang.IllegalStateException: Flow invariant is violated:
		Flow was collected in [CoroutineId(1), "coroutine#1":BlockingCoroutine{Active}@5511c7f8, BlockingEventLoop@2eac3323],
		but emission happened in [CoroutineId(1), "coroutine#1":DispatchedCoroutine{Active}@2dae0000, DefaultDispatcher].
		Please refer to 'flow' documentation or use 'flowOn' instead
	at ...
```

## 8.2、flowOn 运算符

有个例外情况，flowOn 函数能用于改变流发送值时的上下文。改变流上下文的正确方式如下面的示例所示，该示例还打印了相应线程的名称，以显示所有线程的工作方式：

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")
           
//sampleStart
fun foo(): Flow<Int> = flow {
    for (i in 1..3) {
        Thread.sleep(100) // pretend we are computing it in CPU-consuming way
        log("Emitting $i")
        emit(i) // emit next value
    }
}.flowOn(Dispatchers.Default) // RIGHT way to change context for CPU-consuming code in flow builder

fun main() = runBlocking<Unit> {
    foo().collect { value ->
        log("Collected $value") 
    } 
}            
//sampleEnd
```

注意，flow{...} 在后台线程中工作，而在主线程中进行取值

这里要注意的另一件事是 flowOn 操作符改变了流的默认顺序性质。现在取值操作发生在协程 "coroutine#1" 中，而发射值的操作同时运行在另一个线程中的协程 "coroutine#2" 上。当必须在上游流的上下文中更改 CoroutineDispatcher 时，flowOn 运算符将为该上游流创建另一个协程

# 九、缓冲

从收集流所需的总时间的角度来看，在不同的协程中运行流的不同部分可能会有所帮助，特别是当涉及到长时间运行的异步操作时。例如，假设 foo() 流的发射很慢，生成元素需要100毫秒；收集器也很慢，处理元素需要300毫秒。让我们看看用三个数字收集这样的流需要多长时间：

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.system.*

//sampleStart
fun foo(): Flow<Int> = flow {
    for (i in 1..3) {
        delay(100) // pretend we are asynchronously waiting 100 ms
        emit(i) // emit next value
    }
}

fun main() = runBlocking<Unit> { 
    val time = measureTimeMillis {
        foo().collect { value -> 
            delay(300) // pretend we are processing it for 300 ms
            println(value) 
        } 
    }   
    println("Collected in $time ms")
}
//sampleEnd
```

以上代码会产生如下类似的结果，整个收集过程大约需要1200毫秒（三个数字，每个400毫秒）

```kotlin
1
2
3
Collected in 1220 ms
```

我们可以在流上使用 buffer 运算符，在运行取集代码的同时运行 foo() 的发值代码，而不是按顺序运行它们

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.system.*

fun foo(): Flow<Int> = flow {
    for (i in 1..3) {
        delay(100) // pretend we are asynchronously waiting 100 ms
        emit(i) // emit next value
    }
}

fun main() = runBlocking<Unit> { 
//sampleStart
    val time = measureTimeMillis {
        foo()
            .buffer() // buffer emissions, don't wait
            .collect { value -> 
                delay(300) // pretend we are processing it for 300 ms
                println(value) 
            } 
    }   
    println("Collected in $time ms")
//sampleEnd
}
```

这可以得到相同的输出结果但运行速度更快，因为我们已经有效地创建了一个处理管道，第一个数字只需要等待100毫秒，然后只需要花费300毫秒来处理每个数字。这样运行大约需要1000毫秒：

```kotlin
1
2
3
Collected in 1071 ms
```

请注意，flowOn 运算符在必须更改 CoroutineDispatcher 时使用相同的缓冲机制，但这里我们显示地请求缓冲而不更改执行上下文

## 9.1、合并

当流用于表示操作或操作状态更新的部分结果时，可能不需要处理每个值，而是只处理最近的值。在这种情况下，当取值器处理中间值太慢时，可以使用合并运算符跳过中间值。在前面的例子的基础上再来修改下：

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.system.*

fun foo(): Flow<Int> = flow {
    for (i in 1..3) {
        delay(100) // pretend we are asynchronously waiting 100 ms
        emit(i) // emit next value
    }
}

fun main() = runBlocking<Unit> { 
//sampleStart
    val time = measureTimeMillis {
        foo()
            .conflate() // conflate emissions, don't process each one
            .collect { value -> 
                delay(300) // pretend we are processing it for 300 ms
                println(value) 
            } 
    }   
    println("Collected in $time ms")
//sampleEnd
}
```

可以看到，虽然第一个数字仍在处理中，但第二个数字和第三个数字已经生成，因此第二个数字被合并（丢弃），只有最近的一个数字（第三个）被交付给取值器：

```kotlin
1
3
Collected in 758 ms
```

## 9.2、处理最新值

在发射端和处理端都很慢的情况下，合并是加快处理速度的一种方法。它通过丢弃发射的值来实现。另一种方法是取消慢速收集器，并在每次发出新值时重新启动它。有一系列 xxxLatest 运算符与 xxx 运算符执行相同的基本逻辑，但是在新值产生的时候取消执行其块中的代码。在前面的示例中，我们尝试将 conflate 更改为 collectLatest：

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.system.*

fun foo(): Flow<Int> = flow {
    for (i in 1..3) {
        delay(100) // pretend we are asynchronously waiting 100 ms
        emit(i) // emit next value
    }
}

fun main() = runBlocking<Unit> { 
//sampleStart
    val time = measureTimeMillis {
        foo()
            .collectLatest { value -> // cancel & restart on the latest value
                println("Collecting $value") 
                delay(300) // pretend we are processing it for 300 ms
                println("Done $value") 
            } 
    }   
    println("Collected in $time ms")
//sampleEnd
}
```

由于 collectLatest 的主体需要延迟300毫秒，而每100毫秒会发出一个新值，因此我们可以看到 collectLatest 代码块得到了每一个发射值，但最终只完成了最后一个值：

```kotlin
Collecting 1
Collecting 2
Collecting 3
Done 3
Collected in 741 ms
```

# 十、组合多个流

有许多方法可以组合多个流

## 10.1、zip

与 Kotlin 标准库中的 Sequence.zip 扩展函数一样，流有一个 zip 运算符，用于组合两个流的相应值：

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun main() = runBlocking<Unit> { 
//sampleStart                                                                           
    val nums = (1..3).asFlow() // numbers 1..3
    val strs = flowOf("one", "two", "three") // strings 
    nums.zip(strs) { a, b -> "$a -> $b" } // compose a single string
        .collect { println(it) } // collect and print
//sampleEnd
}
```

运行结果：

```kotlin
1 -> one
2 -> two
3 -> three
```

## 10.2、Combine

当 flow 表示变量或操作的最新值时（参阅有关 conflation 的相关章节），可能需要执行依赖于相应流的最新值的计算，并在任何上游流发出值时重新计算它。相应的运算符族称为 combine

例如，如果上例中的数字每300毫秒更新一次，但字符串每400毫秒更新一次，则使用 zip 运算符压缩它们仍会产生相同的结果，尽管结果是每400毫秒打印一次

> 在本例中，我们使用中间运算符 onEach 来延迟每个元素，并使发出样本流的代码更具声明性，更加简短

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun main() = runBlocking<Unit> { 
//sampleStart                                                                           
    val nums = (1..3).asFlow().onEach { delay(300) } // numbers 1..3 every 300 ms
    val strs = flowOf("one", "two", "three").onEach { delay(400) } // strings every 400 ms
    val startTime = System.currentTimeMillis() // remember the start time 
    nums.zip(strs) { a, b -> "$a -> $b" } // compose a single string with "zip"
        .collect { value -> // collect and print 
            println("$value at ${System.currentTimeMillis() - startTime} ms from start") 
        } 
//sampleEnd
}
```

但是，如果在此处使用 combine 运算符而不是 zip 时：

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun main() = runBlocking<Unit> { 
//sampleStart                                                                           
    val nums = (1..3).asFlow().onEach { delay(300) } // numbers 1..3 every 300 ms
    val strs = flowOf("one", "two", "three").onEach { delay(400) } // strings every 400 ms          
    val startTime = System.currentTimeMillis() // remember the start time 
    nums.combine(strs) { a, b -> "$a -> $b" } // compose a single string with "combine"
        .collect { value -> // collect and print 
            println("$value at ${System.currentTimeMillis() - startTime} ms from start") 
        } 
//sampleEnd
}
```

我们得到了完全不同的输出：

```kotlin
1 -> one at 452 ms from start
2 -> one at 651 ms from start
2 -> two at 854 ms from start
3 -> two at 952 ms from start
3 -> three at 1256 ms from start
```

# 十一、展平流

流表示异步接收的值序列，因此在每个值触发对另一个值序列的请求的情况下很容易获取新值。例如，我们可以使用以下函数，该函数返回相隔500毫秒的两个字符串流：

```kotlin
fun requestFlow(i: Int): Flow<String> = flow {
    emit("$i: First") 
    delay(500) // wait 500 ms
    emit("$i: Second")    
}
```

现在，如果我们有一个包含三个整数的流，并为每个整数调用 requestFlow，如下所示：

```kotlin
(1..3).asFlow().map { requestFlow(it) }
```

然后我们最终得到一个流（flow< flow< String >>），需要将其展平为单独一个流以进行进一步处理。集合和序列对此提供了 flatten 和 flatMap 运算符。然而，由于流的异步特性，它们需要不同的展开模式，因此流上有一系列 flattening 运算符

## 11.1、flatMapConcat

flatMapConcat 和 flattencat 运算符实现了 Concatenating 模式，它们是与序列运算符最直接的类比。它们等待内部流完成，然后开始收集下一个流，如下例所示：

```kotlin
fun requestFlow(i: Int): Flow<String> = flow {
    emit("$i: First")
    delay(500) // wait 500 ms
    emit("$i: Second")
}

fun main() = runBlocking<Unit> {
    //sampleStart
    val startTime = System.currentTimeMillis() // remember the start time
    (1..3).asFlow().onEach { delay(100) } // a number every 100 ms
        .flatMapConcat { requestFlow(it) }
        .collect { value ->
            // collect and print
            println("$value at ${System.currentTimeMillis() - startTime} ms from start")
        }
//sampleEnd
}
```

flatMapConcat 的顺序特性在输出结果中清晰可见：

```kotlin
1: First at 121 ms from start
1: Second at 622 ms from start
2: First at 727 ms from start
2: Second at 1227 ms from start
3: First at 1328 ms from start
3: Second at 1829 ms from start
```

## 11.2、flatMapMerge

另一种 flattening 模式是同时收集所有传入流并将其值合并到单个流中，以便尽快发出值。它由 flatMapMerge  和 flattenMerge 运算符实现。它们都接受一个可选的并发参数，该参数用于限制同时收集的并发流的数量（默认情况下等于 DEFAULT_CONCURRENCY）

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun requestFlow(i: Int): Flow<String> = flow {
    emit("$i: First") 
    delay(500) // wait 500 ms
    emit("$i: Second")    
}

fun main() = runBlocking<Unit> { 
//sampleStart
    val startTime = System.currentTimeMillis() // remember the start time 
    (1..3).asFlow().onEach { delay(100) } // a number every 100 ms 
        .flatMapMerge { requestFlow(it) }                                                                           
        .collect { value -> // collect and print 
            println("$value at ${System.currentTimeMillis() - startTime} ms from start") 
        } 
//sampleEnd
}
```

flatMapMerge 的并发性是显而易见的：

```kotlin
1: First at 136 ms from start
2: First at 231 ms from start
3: First at 333 ms from start
1: Second at 639 ms from start
2: Second at 732 ms from start
3: Second at 833 ms from start
```

请注意，flatMapMerge 按顺序调用其代码块（{requestFlow(it)}），但同时收集结果流，这相当于先执行序列 map{requestFlow(it)}，然后对返回值调用 flattenMerge

## 11.3、flatMapLatest

与“Processing the latest value（处理最新值）”章节介绍的 collectLatest 操作符类似，存在相应的 "Latest" flattening 模式。在该模式下，一旦发出新流，将取消先前已发出的流。这通过 flatMapLatest 运算符实现

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun requestFlow(i: Int): Flow<String> = flow {
    emit("$i: First") 
    delay(500) // wait 500 ms
    emit("$i: Second")    
}

fun main() = runBlocking<Unit> { 
//sampleStart
    val startTime = System.currentTimeMillis() // remember the start time 
    (1..3).asFlow().onEach { delay(100) } // a number every 100 ms 
        .flatMapLatest { requestFlow(it) }                                                                           
        .collect { value -> // collect and print 
            println("$value at ${System.currentTimeMillis() - startTime} ms from start") 
        } 
//sampleEnd
}
```

本例中的输出很好的演示了 flatMapLatest 的工作原理

```kotlin
1: First at 142 ms from start
2: First at 322 ms from start
3: First at 425 ms from start
3: Second at 931 ms from start
```

请注意，当新值到来时，flatMapLatest 将取消其块中的所有代码（{requestFlow(it)}）。requestFlow 函数本身的调用是很快速的，并非挂起函数，如果其内部不包含额外的挂起点，那么它就不能被取消，所以此处就在其内部使用了 delay 函数，使其可以达到被取消的目的

# 十二、流异常

当发射器或运算符内部的代码引发异常时，流收集器可以结束运行，但会出现异常。有几种方法可以处理这些异常

## 12.1、收集器 try 与 catch

收集器可以使用 kotlin 的 try/catch 代码块来处理异常

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

//sampleStart
fun foo(): Flow<Int> = flow {
    for (i in 1..3) {
        println("Emitting $i")
        emit(i) // emit next value
    }
}

fun main() = runBlocking<Unit> {
    try {
        foo().collect { value ->         
            println(value)
            check(value <= 1) { "Collected $value" }
        }
    } catch (e: Throwable) {
        println("Caught $e")
    } 
}            
//sampleEnd
```

此代码成功捕获 collect 运算符中的异常，如我们所见，在此之后不再发出任何值：

```kotlin
Emitting 1
1
Emitting 2
2
Caught java.lang.IllegalStateException: Collected 2
```

## 12.2、一切都已捕获

前面的示例实际上捕获了发射器或任何中间或终端运算符中发生的任何异常。例如，让我们更改代码，以便将发出的值映射到字符串，但相应的代码会产生异常：

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

//sampleStart
fun foo(): Flow<String> = 
    flow {
        for (i in 1..3) {
            println("Emitting $i")
            emit(i) // emit next value
        }
    }
    .map { value ->
        check(value <= 1) { "Crashed on $value" }                 
        "string $value"
    }

fun main() = runBlocking<Unit> {
    try {
        foo().collect { value -> println(value) }
    } catch (e: Throwable) {
        println("Caught $e")
    } 
}            
//sampleEnd
```

仍捕获此异常并停止收集：

```kotlin
Emitting 1
string 1
Emitting 2
Caught java.lang.IllegalStateException: Crashed on 2
```

# 十三、异常透明性

但是发射器的代码如何封装其异常处理行为呢？

flows 对于异常必须是透明的，并且在 flow{...} 构建器中发射值有可能抛出异常时，异常必须显式地从 try/catch 块内部抛出。这保证了抛出异常的收集器始终可以使用 try/catch 来捕获异常，如前一个示例所示

发射器可以使用 catch 运算符来保持此异常的透明性，并允许封装其异常处理行为。catch 运算符可以分析异常并根据捕获到的异常以不同的方式对其作出反应：

- 可以使用 throw 重新引发异常
- 使用 catch 的 emit 可以将异常转换为值的 emission
- 异常可以被其他代码忽略、记录或处理

例如，让我们在捕获异常时发出一段文本：

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun foo(): Flow<String> = 
    flow {
        for (i in 1..3) {
            println("Emitting $i")
            emit(i) // emit next value
        }
    }
    .map { value ->
        check(value <= 1) { "Crashed on $value" }                 
        "string $value"
    }

fun main() = runBlocking<Unit> {
//sampleStart
    foo()
        .catch { e -> emit("Caught $e") } // emit on exception
        .collect { value -> println(value) }
//sampleEnd
}            
```

示例代码的输出结果是与之前相同的，即使我们不再在代码周围使用 try/catch

## 13.1、透明捕获

catch 中间运算符遵循异常透明性，只捕获上游异常（即 catch 上所有运算符的异常，而不是 catch 下所有运算符的异常）。如果 collect{...}（放在 catch 下面）抛出异常，程序将退出：

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

//sampleStart
fun foo(): Flow<Int> = flow {
    for (i in 1..3) {
        println("Emitting $i")
        emit(i)
    }
}

fun main() = runBlocking<Unit> {
    foo()
        .catch { e -> println("Caught $e") } // does not catch downstream exceptions
        .collect { value ->
            check(value <= 1) { "Collected $value" }                 
            println(value) 
        }
}            
//sampleEnd
```

尽管存在 catch 运算符，但不会打印 “Caught ...” 日志

## 13.2、声明式捕获

我们可以将 catch 运算符的声明性与处理所有异常的愿望结合起来，方法是将 collect 运算符原先所要做的操作移动到 onEach 中，并将其放在 catch 运算符之前。此流的取值操作必须由不带参数的 collect() 函数来调用触发：

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun foo(): Flow<Int> = flow {
    for (i in 1..3) {
        println("Emitting $i")
        emit(i)
    }
}

fun main() = runBlocking<Unit> {
//sampleStart
    foo()
        .onEach { value ->
            check(value <= 1) { "Collected $value" }                 
            println(value) 
        }
        .catch { e -> println("Caught $e") }
        .collect()
//sampleEnd
}            
```

现在我们可以看到打印了一条 “Caught ...” 消息，至此我们捕获了所有异常，而无需显式使用 try/catch 

# 十四、流完成

当流收集完成时（正常或异常），它可能需要执行一个操作。正如你可能已经注意到的，它可以通过两种方式完成：命令式或声明式

## 14.1、命令式 finally 块

除了 try/catch 外，收集器还可以使用 finally 在收集完成时执行操作

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

//sampleStart
fun foo(): Flow<Int> = (1..3).asFlow()

fun main() = runBlocking<Unit> {
    try {
        foo().collect { value -> println(value) }
    } finally {
        println("Done")
    }
}            
//sampleEnd
```

此代码打印 fon() 流生成的三个数字，之后跟随 "Done" 字符串

```kotlin
1
2
3
Done
```

## 14.2、声明式处理

对于声明性方法，flow 有一个 onCompletion 中间运算符，该运算符在流完全收集后调用

前面的示例可以使用 onCompletion 运算符重写，并生成相同的输出：

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun foo(): Flow<Int> = (1..3).asFlow()

fun main() = runBlocking<Unit> {
//sampleStart
    foo()
        .onCompletion { println("Done") }
        .collect { value -> println(value) }
//sampleEnd
}            
```

onCompletion 的主要优点是包含一个 lambda 参数，该 lambda 包含一个可空的 Throwable 参数，该 Throwable 参数可用于确定流收集是正常完成还是异常完成。在以下示例中，foo() 流在发出数字1后引发异常：

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

//sampleStart
fun foo(): Flow<Int> = flow {
    emit(1)
    throw RuntimeException()
}

fun main() = runBlocking<Unit> {
    foo()
        .onCompletion { cause -> if (cause != null) println("Flow completed exceptionally") }
        .catch { cause -> println("Caught exception") }
        .collect { value -> println(value) }
}            
//sampleEnd
```

如你所料，将打印：

```kotlin
1
Flow completed exceptionally
Caught exception
```

与 catch 运算符不同，onCompletion 运算符不处理异常。正如我们从上面的示例代码中看到的，异常仍然会流向下游。它将被传递给其他完成 onCompletion 运算符，并可以使用 catch 运算符进行处理

## 14.3、仅限上游异常

就像 catch 操作符一样，onCompletion 只看到来自上游的异常，而看不到下游的异常。例如，运行以下代码：

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

//sampleStart
fun foo(): Flow<Int> = (1..3).asFlow()

fun main() = runBlocking<Unit> {
    foo()
        .onCompletion { cause -> println("Flow completed with $cause") }
        .collect { value ->
            check(value <= 1) { "Collected $value" }                 
            println(value) 
        }
}
//sampleEnd
```

我们可以看到  completion cause 为空，但流收集失败并抛出异常:

```kotlin
1
Flow completed with null
Exception in thread "main" java.lang.IllegalStateException: Collected 2
```

# 十五、命令式还是声明式

现在我们知道如何收集流，并以命令式和声明式的方式处理它的完成和异常。这里很自然的就有了个问题，应该首选哪种方法呢？为什么？作为一个库，我们不提倡任何特定的方法，并且相信这两种方式都是有效的，应该根据你自己的偏好和代码风格来选择

# 十六、启动流

很容易使用流来表示来自某个数据源的异步事件。在这种情况下，我们需要一个模拟的 addEventListener 函数，该函数将一段代码注册为对传入事件的响应，并继续进一步工作。onEach 运算符可以担任此角色。然而，onEach 是一个中间运算符。我们还需要一个终端运算符来收集数据。否则，只注册 onEach 是没有效果的

如果在 onEach 之后使用 collect 终端运算符，则在 collect 之后的代码将等待流被收集完成后再运行：

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

//sampleStart
// Imitate a flow of events
fun events(): Flow<Int> = (1..3).asFlow().onEach { delay(100) }

fun main() = runBlocking<Unit> {
    events()
        .onEach { event -> println("Event: $event") }
        .collect() // <--- Collecting the flow waits
    println("Done")
}            
//sampleEnd
```

如你所见，将打印

```kotlin
Event: 1
Event: 2
Event: 3
Done
```

launchIn 终端运算符在这里是很实用的。通过将 collect 替换为 launchIn，我们可以在单独的协程中启动收集流数据的操作，以便立即继续执行下一步的代码：

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

// Imitate a flow of events
fun events(): Flow<Int> = (1..3).asFlow().onEach { delay(100) }

//sampleStart
fun main() = runBlocking<Unit> {
    events()
        .onEach { event -> println("Event: $event") }
        .launchIn(this) // <--- Launching the flow in a separate coroutine
    println("Done")
}            
//sampleEnd
```

运行结果：

```kotlin
Done
Event: 1
Event: 2
Event: 3
```

launchIn 所需的参数用于指定启动用于收集流的协程的作用域。在上面的示例中，此作用域来自 runBlocking，因此当流运行时，runBlocking 作用域等待其子协程完成，并阻止主函数返回和终止此示例代码

在实际应用程序中，作用域将来自生命周期是有限的实体。一旦此实体的生命周期终止，相应的作用域将被取消，从而取消相应流的收集。onEach { ... }.launchIn(scope) 的工作方式与 addEventListener 类似。但是，不需要相应的 removeEventListener 函数，因为 cancellation 和结构化并发可以达到这个目的

请注意，launchIn 还返回一个 Job 对象，该 Job 仅可用于取消相应的流数据收集协程，而不取消整个作用域或加入它

# 十七、Flow and Reactive Streams

For those who are familiar with Reactive Streams or reactive frameworks such as RxJava and project Reactor, design of the Flow may look very familiar.

Indeed, its design was inspired by Reactive Streams and its various implementations. But Flow main goal is to have as simple design as possible, be Kotlin and suspension friendly and respect structured concurrency. Achieving this goal would be impossible without reactive pioneers and their tremendous work. You can read the complete story in Reactive Streams and Kotlin Flows article.

While being different, conceptually, Flow is a reactive stream and it is possible to convert it to the reactive (spec and TCK compliant) Publisher and vice versa. Such converters are provided by kotlinx.coroutines out-of-the-box and can be found in corresponding reactive modules (kotlinx-coroutines-reactive for Reactive Streams, kotlinx-coroutines-reactor for Project Reactor and kotlinx-coroutines-rx2 for RxJava2). Integration modules include conversions from and to Flow, integration with Reactor's Context and suspension-friendly ways to work with various reactive entities.