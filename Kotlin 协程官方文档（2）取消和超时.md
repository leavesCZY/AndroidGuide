> 最近一直在了解关于 **Kotlin协程** 的知识，那最好的学习资料自然是官方提供的学习文档了，看了看后我就萌生了翻译官方文档的想法。前后花了要接近一个月时间，一共九篇文章，在这里也分享出来，希望对读者有所帮助。个人知识所限，有些翻译得不是太顺畅，也希望读者能提出意见
> 
> 协程官方文档：[coroutines-guide](https://github.com/Kotlin/kotlinx.coroutines/blob/master/coroutines-guide.md)

本节讨论协程的取消和超时

# 一、取消协程执行

在一个长时间运行的应用程序中，我们可能需要对协程进行细粒度控制。例如，用户可能关闭了启动了协程的页面，现在不再需要其运行结果，此时就应该主动取消协程。launch 函数的返回值 Job 对象就可用于取消正在运行的协程

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
//sampleStart
    val job = launch {
        repeat(1000) { i ->
            println("job: I'm sleeping $i ...")
            delay(500L)
        }
    }
    delay(1300L) // delay a bit
    println("main: I'm tired of waiting!")
    job.cancel() // cancels the job
    job.join() // waits for job's completion 
    println("main: Now I can quit.")
    //sampleEnd    
}
```

运行结果

```kotlin
job: I'm sleeping 0 ...
job: I'm sleeping 1 ...
job: I'm sleeping 2 ...
main: I'm tired of waiting!
main: Now I can quit.
```

只要 main 函数调用了 ```job.cancel```，我们就看不到 job 协程的任何输出了，因为它已被取消。还有一个 Job 的扩展函数 ```cancelAndJoin``` ，它结合了 ```cancel``` 和 ```join``` 的调用。

> cancel() 函数用于取消协程，join() 函数用于阻塞等待协程执行结束。之所以连续调用这两个方法，是因为 cancel() 函数调用后会马上返回而不是等待协程结束后再返回，所以此时协程不一定是马上就停止了，为了确保协程执行结束后再执行后续代码，此时就需要调用 join() 方法来阻塞等待。可以通过调用 Job 的扩展函数 cancelAndJoin() 来完成相同操作

```kotlin
public suspend fun Job.cancelAndJoin() {
    cancel()
    return join()
}
```

# 二、取消操作是协作完成的

协程的取消操作是协作(cooperative)完成的，协程必须协作才能取消。```kotlinx.coroutines``` 中的所有挂起函数都是可取消的，它们在运行时会检查协程是否被取消了，并在取消时抛出 CancellationException 。但是，如果协程正在执行计算任务，并且未检查是否已处于取消状态的话，则无法取消协程，如以下示例所示：

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    //sampleStart
    val startTime = System.currentTimeMillis()
    val job = launch(Dispatchers.Default) {
        var nextPrintTime = startTime
        var i = 0
        while (i < 5) { // computation loop, just wastes CPU
            // print a message twice a second
            if (System.currentTimeMillis() >= nextPrintTime) {
                println("job: I'm sleeping ${i++} ...")
                nextPrintTime += 500L
            }
        }
    }
    delay(1300L) // delay a bit
    println("main: I'm tired of waiting!")
    job.cancelAndJoin() // cancels the job and waits for its completion
    println("main: Now I can quit.")
    //sampleEnd    
}
```

运行代码可以看到即使在 cancel 之后协程 job 也会继续打印 "I'm sleeping" ，直到 Job 在迭代五次后（运行条件不再成立）自行结束

```kotlin
job: I'm sleeping 0 ...
job: I'm sleeping 1 ...
job: I'm sleeping 2 ...
main: I'm tired of waiting!
job: I'm sleeping 3 ...
job: I'm sleeping 4 ...
main: Now I can quit.
```

# 三、使计算代码可取消

有两种方法可以使计算类型的代码可以被取消。第一种方法是定期调用一个挂起函数来检查取消操作，```yieid()``` 函数是一个很好的选择。另一个方法是显示检查取消操作。让我们来试试后一种方法

使用 ```while (isActive)``` 替换前面例子中的 ```while (i < 5)```

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    //sampleStart
    val startTime = System.currentTimeMillis()
    val job = launch(Dispatchers.Default) {
        var nextPrintTime = startTime
        var i = 0
        while (isActive) { // cancellable computation loop
            // print a message twice a second
            if (System.currentTimeMillis() >= nextPrintTime) {
                println("job: I'm sleeping ${i++} ...")
                nextPrintTime += 500L
            }
        }
    }
    delay(1300L) // delay a bit
    println("main: I'm tired of waiting!")
    job.cancelAndJoin() // cancels the job and waits for its completion
    println("main: Now I can quit.")
    //sampleEnd    
}
```

如你所见，现在这个循环被取消了。isActive 是一个可通过 CoroutineScope 对象在协程内部使用的扩展属性

```kotlin
job: I'm sleeping 0 ...
job: I'm sleeping 1 ...
job: I'm sleeping 2 ...
main: I'm tired of waiting!
main: Now I can quit.
```

# 四、用 finally 关闭资源

可取消的挂起函数在取消时会抛出 CancellationException，可以用常用的方式来处理这种情况。例如，```try {...} finally {...}``` 表达式和 kotlin 的 ```use``` 函数都可用于在取消协程时执行回收操作

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    //sampleStart
    val job = launch {
        try {
            repeat(1000) { i ->
                println("job: I'm sleeping $i ...")
                delay(500L)
            }
        } finally {
            println("job: I'm running finally")
        }
    }
    delay(1300L) // delay a bit
    println("main: I'm tired of waiting!")
    job.cancelAndJoin() // cancels the job and waits for its completion
    println("main: Now I can quit.")
    //sampleEnd    
}
```

join() 和 cancelAndJoin() 两个函数都会等待所有回收操作完成后再继续执行之后的代码，因此上面的示例生成以下输出：

```kotlin
job: I'm sleeping 0 ...
job: I'm sleeping 1 ...
job: I'm sleeping 2 ...
main: I'm tired of waiting!
job: I'm running finally
main: Now I can quit.
```

# 五、运行不可取消的代码块

如果在上一个示例中的 ```finally``` 块中使用挂起函数，将会导致抛出 CancellationException，因为此时协程已经被取消了（例如，在 finally 中先调用 delay(1000L) 函数，将导致之后的输出语句不执行）。通常这并不是什么问题，因为所有性能良好的关闭操作（关闭文件、取消作业、关闭任何类型的通信通道等）通常都是非阻塞的，且不涉及任何挂起函数。但是，在极少数情况下，当需要在取消的协程中调用挂起函数时，可以使用 withContext 函数和 NonCancellable 上下文将相应的代码包装在 ```withContext(NonCancellable) {...}``` 代码块中，如下例所示：

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    //sampleStart
    val job = launch {
        try {
            repeat(1000) { i ->
                println("job: I'm sleeping $i ...")
                delay(500L)
            }
        } finally {
            withContext(NonCancellable) {
                println("job: I'm running finally")
                delay(1000L)
                println("job: And I've just delayed for 1 sec because I'm non-cancellable")
            }
        }
    }
    delay(1300L) // delay a bit
    println("main: I'm tired of waiting!")
    job.cancelAndJoin() // cancels the job and waits for its completion
    println("main: Now I can quit.")
    //sampleEnd    
}
```

此时，即使在 finally 代码块中调用了挂起函数，其也将正常生效，且之后的输出语句也会正常输出

```kotlin
job: I'm sleeping 0 ...
job: I'm sleeping 1 ...
job: I'm sleeping 2 ...
main: I'm tired of waiting!
job: I'm running finally
job: And I've just delayed for 1 sec because I'm non-cancellable
main: Now I can quit.
```

# 六、超时

大多数情况下，我们会主动取消协程的原因是由于其执行时间已超出预估的最长时间。虽然我们可以手动跟踪对相应 Job 的引用，并在超时后取消 Job，但官方也提供了 withTimeout 函数来完成此类操作。看一下示例：

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    //sampleStart
    withTimeout(1300L) {
        repeat(1000) { i ->
            println("I'm sleeping $i ...")
            delay(500L)
        }
    }
    //sampleEnd
}
```

运行结果：

```kotlin
I'm sleeping 0 ...
I'm sleeping 1 ...
I'm sleeping 2 ...
Exception in thread "main" kotlinx.coroutines.TimeoutCancellationException: Timed out waiting for 1300 ms
```

withTimeout 引发的 ```TimeoutCancellationException``` 是 CancellationException 的子类。之前我们从未在控制台上看过 CancellationException 这类异常的堆栈信息。这是因为对于一个已取消的协程来说，CancellationException 被认为是触发协程结束的正常原因。但是，在这个例子中，我们在主函数中使用了 ```withTimeout``` 函数，该函数会主动抛出 TimeoutCancellationException

你可以通过使用 ```try{...}catch（e:TimeoutCancellationException）{...}``` 代码块来对任何情况下的超时操作执行某些特定的附加操作，或者通过使用 ```withTimeoutOrNull``` 函数以便在超时时返回 null 而不是抛出异常

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    //sampleStart
    val result = withTimeoutOrNull(1300L) {
        repeat(1000) { i ->
            println("I'm sleeping $i ...")
            delay(500L)
        }
        "Done" // will get cancelled before it produces this result
    }
    println("Result is $result")
    //sampleEnd
}
```

此时将不会打印出异常信息

```kotlin
I'm sleeping 0 ...
I'm sleeping 1 ...
I'm sleeping 2 ...
Result is null
```