> 最近一直在了解关于 **Kotlin协程** 的知识，那最好的学习资料自然是官方提供的学习文档了，看了看后我就萌生了翻译官方文档的想法。前后花了要接近一个月时间，一共九篇文章，在这里也分享出来，希望对读者有所帮助。个人知识所限，有些翻译得不是太顺畅，也希望读者能提出意见
> 
> 协程官方文档：[coroutines-guide](https://github.com/Kotlin/kotlinx.coroutines/blob/master/coroutines-guide.md)

本节讨论协程关于异常的处理和取消异常。我们已经知道，取消协程会使得在挂起点抛出 CancellationException，而协程机制会忽略这个异常。但是，如果在取消期间抛出异常，或者协程的多个子协程抛出异常，此时会发生什么情况呢?

# 一、异常的传播

协程构建器有两种类型：自动传播异常（launch 和 actor）和向用户公开异常（async 和 product）。前者将异常视为未捕获异常，类似于 Java 的 Thread.uncaughtExceptionHandler，而后者则需要由开发者自己来处理最终的异常，例如通过 await 或 receive（product 和 receive 在 Channels 章节介绍）

可以通过在 GlobalScope 创建协程的简单示例来演示：

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    val job = GlobalScope.launch {
        println("Throwing exception from launch")
        throw IndexOutOfBoundsException() // Will be printed to the console by Thread.defaultUncaughtExceptionHandler
    }
    job.join()
    println("Joined failed job")
    val deferred = GlobalScope.async {
        println("Throwing exception from async")
        throw ArithmeticException() // Nothing is printed, relying on user to call await
    }
    try {
        deferred.await()
        println("Unreached")
    } catch (e: ArithmeticException) {
        println("Caught ArithmeticException")
    }
}
```

运行结果：

```kotlin
Throwing exception from launch
Exception in thread "DefaultDispatcher-worker-2 @coroutine#2" java.lang.IndexOutOfBoundsException
Joined failed job
Throwing exception from async
Caught ArithmeticException
```

# 二、CoroutineExceptionHandler

如果不想将所有的异常都打印到控制台上，CoroutineExceptionHandler 上下文元素可以作为协程全局通用的 catch 块，在这里进行自定义日志记录或异常处理。它类似于对线程使用 Thread.uncaughtExceptionHandler

在 JVM 上，可以通过 ServiceLoader 注册 CoroutineExceptionHandler 来重新定义所有协程的全局异常处理器。全局异常处理程序类似于 Thread.defaultUncaughtExceptionHandler ，后者在没有注册其它特定处理程序时使用。在 Android 上，uncaughtExceptionPreHandler 作为全局协程异常处理程序存在

CoroutineExceptionHandler 只在预计不会由用户处理的异常上调用，因此在 async  这类协程构造器中注册它没有任何效果

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    //sampleStart
    val handler = CoroutineExceptionHandler { _, exception -> 
        println("Caught $exception") 
    }
    val job = GlobalScope.launch(handler) {
        throw AssertionError()
    }
    val deferred = GlobalScope.async(handler) {
        throw ArithmeticException() // Nothing will be printed, relying on user to call deferred.await()
    }
    joinAll(job, deferred)
    //sampleEnd    
}
```

运行结果：

```kotlin
Caught java.lang.AssertionError
```

# 三、取消和异常

取消和异常是紧密联系的。协程在内部使用 CancellationException 来进行取消，所有处理程序都会忽略这类异常，因此它们仅用作调试信息的额外来源，这些信息可以用 catch 块捕获。当使用 Job.cancel 取消协程时，协程将停止运行，但不会取消其父协程

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    //sampleStart
    val job = launch {
        val child = launch {
            try {
                delay(Long.MAX_VALUE)
            } finally {
                println("Child is cancelled")
            }
        }
        yield()
        println("Cancelling child")
        child.cancel()
        child.join()
        yield()
        println("Parent is not cancelled")
    }
    job.join()
    //sampleEnd    
}
```

运行结果：

```kotlin
Cancelling child
Child is cancelled
Parent is not cancelled
```

如果协程遇到 CancellationException 以外的异常，它将用该异常取消其父级。无法重写此行为，它用于为不依赖于 CoroutineExceptionHandler 实现的结构化并发，为之提供稳定的协程层次结构。当父级的所有子级终止时，父级将处理原始异常

> 这也是为什么在这些示例中，总是将 CoroutineExceptionHandler 作为参数传递给在 GlobalScope 中创建的协程中的原因。将 CoroutineExceptionHandler 设置给主 runBlocking 范围内启动的协程是没有意义的，因为尽管设置了异常处理器，主协程在其子级异常抛出后仍将被取消

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    //sampleStart
    val handler = CoroutineExceptionHandler { _, exception -> 
        println("Caught $exception") 
    }
    val job = GlobalScope.launch(handler) {
        launch { // the first child
            try {
                delay(Long.MAX_VALUE)
            } finally {
                withContext(NonCancellable) {
                    println("Children are cancelled, but exception is not handled until all children terminate")
                    delay(100)
                    println("The first child finished its non cancellable block")
                }
            }
        }
        launch { // the second child
            delay(10)
            println("Second child throws an exception")
            throw ArithmeticException()
        }
    }
    job.join()
    //sampleEnd    
}
```

运行结果：

```kotlin
Second child throws an exception
Children are cancelled, but exception is not handled until all children terminate
The first child finished its non cancellable block
Caught java.lang.ArithmeticException
```

> CoroutineExceptionHandler 将等到所有子协程运行结束后再回调。second child 抛出异常后将联动导致 first child 结束运行，之后再将异常交予 CoroutineExceptionHandler 处理

# 四、异常聚合

如果一个协程的多个子协程抛出异常会发生什么情况呢？一般的规则是“第一个异常会获胜”，因此第一个抛出的异常将传递给异常处理器进行处理，但这也有可能会导致异常丢失。例如，如果在某个协程在抛出异常后，第二个协程在其 finally 块中抛出异常，此时第二个协程的异常将不会传递给 CoroutineExceptionHandler

> 其中一个解决方案是分别抛出每个异常。await 应该有相同的机制来避免行为不一致，这将导致协程的实现细节（无论它是否将部分工作委托给其子级）泄漏给其异常处理器

```kotlin
import kotlinx.coroutines.*
import java.io.*

fun main() = runBlocking {
    val handler = CoroutineExceptionHandler { _, exception ->
        println("Caught $exception with suppressed ${exception.suppressed.contentToString()}")
    }
    val job = GlobalScope.launch(handler) {
        launch {
            try {
                delay(Long.MAX_VALUE)
            } finally {
                throw ArithmeticException()
            }
        }
        launch {
            delay(100)
            throw IOException()
        }
        delay(Long.MAX_VALUE)
    }
    job.join()  
}
```

> 注意：以上代码只能在支持 suppressed exceptions 的 JDK7+ 版本上正常运行

运行结果：

```kotlin
Caught java.io.IOException with suppressed [java.lang.ArithmeticException]
```

导致协程停止的异常在默认情况下是会被透传，不会被包装的

```kotlin
import kotlinx.coroutines.*
import java.io.*

fun main() = runBlocking {
    //sampleStart
    val handler = CoroutineExceptionHandler { _, exception ->
        println("Caught original $exception")
    }
    val job = GlobalScope.launch(handler) {
        val inner = launch {
            launch {
                launch {
                    throw IOException()
                }
            }
        }
        try {
            inner.join()
        } catch (e: CancellationException) {
            println("Rethrowing CancellationException with original cause")
            throw e
        }
    }
    job.join()
    //sampleEnd    
}
```

运行结果：

```kotlin
Rethrowing CancellationException with original cause
Caught original java.io.IOException
```

> 即使捕获到了 inner 被取消的异常信息，但最终传递给 CoroutineExceptionHandler 的还是 inner 内部真实的异常信息

# 五、Supervision

正如我们之前所研究的，取消是一种双向关系，会在整个协程层次结构中传播。但如果需要单向取消呢？

此类需求的一个很好的例子在某个范围内定义了 Job 的 UI 组件。如果 UI 组件的任意一个子任务失败了，此时并不一定需要取消（实际上是终止）整个 UI 组件。但是如果 UI 组件的生命周期结束了（并且取消了它的 Job），那么就必须取消所有子 Job， 因为它们的结果不再是必需的

另一个例子是一个服务器进程，它生成几个子 Job 并且需要监测它们的执行，跟踪它们的失败时机，并且仅重新启动那么失败的子 Job

## 5.1、SupervisorJob

出于这些目的，可以使用 SupervisorJob。它类似于常规的 Job，唯一的例外是取消操作只向下传播。用一个例子很容易演示：

```kotlin
import kotlinx.coroutines.*

fun main() = runBlocking {
    val supervisor = SupervisorJob()
    with(CoroutineScope(coroutineContext + supervisor)) {
        // launch the first child -- its exception is ignored for this example (don't do this in practice!)
        val firstChild = launch(CoroutineExceptionHandler { _, _ -> }) {
            println("First child is failing")
            throw AssertionError("First child is cancelled")
        }
        // launch the second child
        val secondChild = launch {
            firstChild.join()
            // Cancellation of the first child is not propagated to the second child
            println("First child is cancelled: ${firstChild.isCancelled}, but second one is still active")
            try {
                delay(Long.MAX_VALUE)
            } finally {
                // But cancellation of the supervisor is propagated
                println("Second child is cancelled because supervisor is cancelled")
            }
        }
        // wait until the first child fails & completes
        firstChild.join()
        println("Cancelling supervisor")
        supervisor.cancel()
        secondChild.join()
    }
}
```

运行结果：

```kotlin
First child is failing
First child is cancelled: true, but second one is still active
Cancelling supervisor
Second child is cancelled because supervisor is cancelled
```

## 5.2、supervisorScope

对于作用域并发，可以使用 supervisorScope 代替 coroutineScope 来实现相同的目的。它只在一个方向上传播取消操作，并且仅在自身失败时才取消所有子级。它也像 coroutineScope 一样在结束运行之前等待所有的子元素结束运行

```kotlin
import kotlin.coroutines.*
import kotlinx.coroutines.*

fun main() = runBlocking {
    try {
        supervisorScope {
            val child = launch {
                try {
                    println("Child is sleeping")
                    delay(Long.MAX_VALUE)
                } finally {
                    println("Child is cancelled")
                }
            }
            // Give our child a chance to execute and print using yield 
            yield()
            println("Throwing exception from scope")
            throw AssertionError()
        }
    } catch(e: AssertionError) {
        println("Caught assertion error")
    }
}
```

输出结果：

```kotlin
Child is sleeping
Throwing exception from scope
Child is cancelled
Caught assertion error
```

以下例子展示了 supervisorScope 中取消操作的单向传播性，子协程的异常不会导致其它子协程取消

```kotlin
fun main() = runBlocking {
    supervisorScope {
        val child1 = launch {
            try {
                for (time in 1..Long.MAX_VALUE) {
                    println("Child 1 is printing: $time")
                    delay(1000)
                }
            } finally {
                println("Child 1 is cancelled")
            }
        }
        val child2 = launch {
            println("Child 2 is sleeping")
            delay(3000)
            println("Child 2 throws an exception")
            throw AssertionError()
        }
    }
}
```

运行结果：

```kotlin
Child 1 is printing: 1
Child 2 is sleeping
Child 1 is printing: 2
Child 1 is printing: 3
Child 1 is printing: 4
Child 2 throws an exception
Exception in thread "main" java.lang.AssertionError
Child 1 is printing: 5
Child 1 is printing: 6
······
```

## 5.3、监督协程中的异常

常规 job 和 supervisor job 的另一个重要区别在于异常处理。每个子级都应该通过异常处理机制自己处理其异常。这种差异来自于这样一个事实：supervisorScope 中子元素的失败不会传导给父级

```kotlin
import kotlin.coroutines.*
import kotlinx.coroutines.*

fun main() = runBlocking {
    val handler = CoroutineExceptionHandler { _, exception -> 
        println("Caught $exception") 
    }
    supervisorScope {
        val child = launch(handler) {
            println("Child throws an exception")
            throw AssertionError()
        }
        println("Scope is completing")
    }
    println("Scope is completed")
}
```

运行结果：

```kotlin
Scope is completing
Child throws an exception
Caught java.lang.AssertionError
Scope is completed
```