```kotlin
                         \\           //
                          \\  .ooo.  //
                           .@@@@@@@@@.
                         :@@@@@@@@@@@@@:
                        :@@. '@@@@@' .@@:
                        @@@@@@@@@@@@@@@@@
                        @@@@@@@@@@@@@@@@@

                   :@@ :@@@@@@@@@@@@@@@@@. @@:
                   @@@ '@@@@@@@@@@@@@@@@@, @@@
                   @@@ '@@@@@@@@@@@@@@@@@, @@@
                   @@@ '@@@@@@@@@@@@@@@@@, @@@
                   @@@ '@@@@@@@@@@@@@@@@@, @@@
                   @@@ '@@@@@@@@@@@@@@@@@, @@@
                   @@@ '@@@@@@@@@@@@@@@@@, @@@
                        @@@@@@@@@@@@@@@@@
                        '@@@@@@@@@@@@@@@'
                           @@@@   @@@@
                           @@@@   @@@@
                           @@@@   @@@@
                           '@@'   '@@'

     :@@@.
   .@@@@@@@:   +@@       `@@      @@`   @@     @@
  .@@@@'@@@@:  +@@       `@@      @@`   @@     @@
  @@@     @@@  +@@       `@@      @@`   @@     @@
 .@@       @@: +@@   @@@ `@@      @@` @@@@@@ @@@@@@  @@;@@@@@
 @@@       @@@ +@@  @@@  `@@      @@` @@@@@@ @@@@@@  @@@@@@@@@
 @@@       @@@ +@@ @@@   `@@@@@@@@@@`   @@     @@    @@@   :@@
 @@@       @@@ +@@@@@    `@@@@@@@@@@`   @@     @@    @@#    @@+
 @@@       @@@ +@@@@@+   `@@      @@`   @@     @@    @@:    @@#
  @@:     .@@` +@@@+@@   `@@      @@`   @@     @@    @@#    @@+
  @@@.   .@@@  +@@  @@@  `@@      @@`   @@     @@    @@@   ,@@
   @@@@@@@@@   +@@   @@@ `@@      @@`   @@@@   @@@@  @@@@#@@@@
    @@@@@@@    +@@   #@@ `@@      @@`   @@@@:  @@@@: @@'@@@@@
                                                     @@:
                                                     @@:
                                                     @@:
```

> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

> 对于 Android Developer 来说，很多开源库都是属于**开发必备**的知识点，从使用方式到实现原理再到源码解析，这些都需要我们有一定程度的了解和运用能力。所以我打算来写一系列关于开源库**源码解析**和**实战演练**的文章，初定的目标是 **EventBus、ARouter、LeakCanary、Retrofit、Glide、OkHttp、Coil** 等七个知名开源库，希望对你有所帮助 🤣🤣

本文基于 OkHttp 的以下版本进行讲解。值得一提的是，OkHttp 和 OkIO 目前已经被官方用 Kotlin 语言重写了一遍，所以还没学 Kotlin 的同学可能连源码都比较难看懂了，Kotlin 入门可以看我的这篇文章：[两万六千字带你 Kotlin 入门](https://juejin.im/post/6880602489297895438)

```groovy
dependencies {
    implementation 'com.squareup.okhttp3:okhttp:4.9.0'
}
```

先来看一个小例子，后面的讲解都会基于这个例子涉及到的模块来展开

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 */
const val URL = "https://publicobject.com/helloworld.txt"

fun main() {
    val okHttClient = OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(10))
        .readTimeout(Duration.ofSeconds(10))
        .writeTimeout(Duration.ofSeconds(10))
        .retryOnConnectionFailure(true)
        .build()
    val request = Request.Builder().url(URL).build()
    val call = okHttClient.newCall(request)
    val response = call.execute()
    println(response.body?.string())
}
```

以上代码就完成了一次 Get 请求，其包含的操作有：

1. 通过 Builder 模式得到 OkHttpClient，OkHttpClient 包含了对网络请求的全局配置信息，包括 **链接超时时间、读写超时时间、链接失败重试** 等各种配置
2. 通过 Builder 模式得到 Request，Request 包含了本次网络请求的所有请求参数，包括 **url、method、headers、body** 等
3. 通过 newCall 方法得到 Call，Call 就用于发起请求，可用于执行 **同步请求（execute）、异步请求（enqueue）、取消请求（cancel）** 等各种操作
4. 调用 execute 方法发起同步请求并返回一个 Response 对象，Response 就包含了此次网络请求的所有返回信息，如果请求失败的话此方法会抛出异常
5. 拿到 Response 对象的 body 并以字符串流的方式进行读取，打印结果即文章开头的 Android 机器人彩蛋

# 一、OkHttpClient

OkHttpClient 使用了 Builder 模式来完成初始化，其提供了很多的配置参数，每个选项都有默认值，但大多数时候我们还是需要来进行自定义，所以也有必要来了解下其包含的所有参数

```kotlin
class Builder constructor() {
    //调度器
    internal var dispatcher: Dispatcher = Dispatcher()
    //连接池
    internal var connectionPool: ConnectionPool = ConnectionPool()
    //拦截器列表
    internal val interceptors: MutableList<Interceptor> = mutableListOf()
    //网络拦截器列表
    internal val networkInterceptors: MutableList<Interceptor> = mutableListOf()
    //事件监听
    internal var eventListenerFactory: EventListener.Factory = EventListener.NONE.asFactory()
    //连接失败的时候是否重试
    internal var retryOnConnectionFailure = true 
    //源服务器身份验证
    internal var authenticator: Authenticator = Authenticator.NONE
    //是否允许重定向
    internal var followRedirects = true
    //是否允许ssl重定向
    internal var followSslRedirects = true
    //Cookie
    internal var cookieJar: CookieJar = CookieJar.NO_COOKIES
    //缓存
    internal var cache: Cache? = null
    //DNS
    internal var dns: Dns = Dns.SYSTEM
    //代理
    internal var proxy: Proxy? = null
    //代理选择器
    internal var proxySelector: ProxySelector? = null
    //代理身份验证
    internal var proxyAuthenticator: Authenticator = Authenticator.NONE
    //Socket工厂
    internal var socketFactory: SocketFactory = SocketFactory.getDefault()
    //安全套接层
    internal var sslSocketFactoryOrNull: SSLSocketFactory? = null
    internal var x509TrustManagerOrNull: X509TrustManager? = null
    internal var connectionSpecs: List<ConnectionSpec> = DEFAULT_CONNECTION_SPECS
    //HTTP 协议
    internal var protocols: List<Protocol> = DEFAULT_PROTOCOLS
    //主机名字确认
    internal var hostnameVerifier: HostnameVerifier = OkHostnameVerifier
    //证书链
    internal var certificatePinner: CertificatePinner = CertificatePinner.DEFAULT
    internal var certificateChainCleaner: CertificateChainCleaner? = null
    internal var callTimeout = 0
    internal var connectTimeout = 10_000
    //读超时
    internal var readTimeout = 10_000
    //写超时
    internal var writeTimeout = 10_000
    //ping 之间的时间间隔
    internal var pingInterval = 0
    internal var minWebSocketMessageToCompress = RealWebSocket.DEFAULT_MINIMUM_DEFLATE_SIZE
    internal var routeDatabase: RouteDatabase? = null
}
```

# 二、Request 

Request 包含了网络请求时的所有请求参数，一共包含以下五个：

1. url。即本次的网络请求地址以及可能包含的 query 键值对
2. method。即请求方式，可选参数有 GET、HEAD、POST、DELETE、PUT、PATCH
3. headers。即请求头，可用来存 token、时间戳等
4. body。即请求体
5. tags。可用来唯一标识本次请求

```kotlin
internal var url: HttpUrl? = null
internal var method: String
internal var headers: Headers.Builder
internal var body: RequestBody? = null
/** A mutable map of tags, or an immutable empty map if we don't have any. */
internal var tags: MutableMap<Class<*>, Any> = mutableMapOf()
```

# 三、Call 

当调用 `okHttClient.newCall(request)`时就会得到一个 Call 对象

```kotlin
/** Prepares the [request] to be executed at some point in the future. */
override fun newCall(request: Request): Call = RealCall(this, request, forWebSocket = false)
```

Call 是一个接口，我们可以将其看做是网络请求的启动器，可用于发起同步请求或异步请求，**但重复发起多次请求的话会抛出异常**

```kotlin
interface Call : Cloneable {
    
  //返回本次网络请求的 Request 对象
  fun request(): Request
    
  //发起同步请求，可能会抛出异常
  @Throws(IOException::class)
  fun execute(): Response
    
  //发起异步请求，通过 Callback 来回调最终结果 
  fun enqueue(responseCallback: Callback)

  //取消网络请求
  fun cancel()

  //是否已经发起过请求
  fun isExecuted(): Boolean

  //是否已经取消请求
  fun isCanceled(): Boolean
  
  //超时计算
  fun timeout(): Timeout

  //同个 Call 不允许重复发起请求，想要再次发起请求可以通过此方法得到一个新的 Call 对象
  public override fun clone(): Call

  fun interface Factory {
    fun newCall(request: Request): Call
  }
}
```

`newCall` 方法返回的实际类型是 RealCall，它是 Call 接口的唯一实现类

当我们调用 `execute` 方法发起同步请求时，其主要逻辑是：

1. 判读是否重复请求
2. 事件记录
3. 将自身加入到 dispatcher 中，并在请求结束时从 dispatcher 中移除自身
4. 通过 `getResponseWithInterceptorChain` 方法得到 Response 对象

```kotlin
class RealCall(
  val client: OkHttpClient,
  /** The application's original request unadulterated by redirects or auth headers. */
  val originalRequest: Request,
  val forWebSocket: Boolean
) : Call {
    
  override fun execute(): Response {
    check(executed.compareAndSet(false, true)) { "Already Executed" }
    timeout.enter()
    callStart()
    try {
      client.dispatcher.executed(this)
      return getResponseWithInterceptorChain()
    } finally {
      client.dispatcher.finished(this)
    }
  }
  
}
```

# 四、Dispatcher

从上面的分析可以看出来，`getResponseWithInterceptorChain` 方法就是重头戏了，其返回了我们最终得到的 Response。但这里先不介绍该方法，先来看看 Dispatcher 的逻辑

Dispatcher 是一个调度器，用于对全局的网络请求进行缓存调度，其包含以下几个成员变量

```kotlin
var maxRequests = 64

var maxRequestsPerHost = 5

/** Ready async calls in the order they'll be run. */
private val readyAsyncCalls = ArrayDeque<AsyncCall>()

/** Running asynchronous calls. Includes canceled calls that haven't finished yet. */
private val runningAsyncCalls = ArrayDeque<AsyncCall>()

/** Running synchronous calls. Includes canceled calls that haven't finished yet. */
private val runningSyncCalls = ArrayDeque<RealCall>()
```

- maxRequests。同一时间允许并发执行网络请求的最大线程数
- maxRequestsPerHost。同一 host 下的最大同时请求数
- readyAsyncCalls。保存当前等待执行的异步任务
- runningAsyncCalls。保存当前正在执行的异步任务
- runningSyncCalls。保存当前正在执行的同步任务

客户端不应该无限制地同时发起多个网络请求，因为除了网络资源所限外，系统资源也是有限的，每个请求都需要由一个线程来执行，而系统支持并发执行的线程数量是有限的，所以 OkHttp 内部就使用 maxRequests 来控制同时执行异步请求的最大线程数。此外，OkHttp 为了提高效率，允许多个指向同一 host 的网络请求共享同一个 Socket，而最大共享数量即 maxRequestsPerHost

为了统计以上两个运行参数，就需要使用 readyAsyncCalls、runningAsyncCalls 和 runningSyncCalls 来保存当前正在执行或者准备执行的网络请求。runningSyncCalls 用于保存**当前正在执行的同步任务**，其存储的是 RealCall。readyAsyncCalls 和 runningAsyncCalls 用于保存**异步任务**，其存储的是 AsyncCall

## 1、同步请求

RealCall 的 `execute()` 方法在开始请求前，会先将自身传给 dispatcher，在请求结束后又会从 dispatcher 中移除

```kotlin
class RealCall(
  val client: OkHttpClient,
  /** The application's original request unadulterated by redirects or auth headers. */
  val originalRequest: Request,
  val forWebSocket: Boolean
) : Call {
 
  override fun execute(): Response {
    check(executed.compareAndSet(false, true)) { "Already Executed" }
    timeout.enter()
    callStart()
    try {
        //添加到 dispatcher
      client.dispatcher.executed(this)
      return getResponseWithInterceptorChain()
    } finally {
        //从 dispatcher 中移除
      client.dispatcher.finished(this)
    }
  }
    
}
```

Dispatcher 内部也只是相应的将 RealCall 添加到 runningSyncCalls 中或者是将其从 runningSyncCalls 中移除，保存到 runningSyncCalls 的目的是为了方便**统计当前所有正在运行的请求总数**以及**能够取消所有请求**。此外，由于同步请求会直接运行在调用者所在线程上，所以同步请求并不会受 maxRequests 的限制

```kotlin
class Dispatcher constructor() {
    
      /** Used by [Call.execute] to signal it is in-flight. */
  	  @Synchronized 
      internal fun executed(call: RealCall) {
    	runningSyncCalls.add(call)
      }
    
      /** Used by [Call.execute] to signal completion. */
  	  internal fun finished(call: RealCall) {
    	finished(runningSyncCalls, call)
  	  }

  	  private fun <T> finished(calls: Deque<T>, call: T) {
    	val idleCallback: Runnable?
    	synchronized(this) {
      		if (!calls.remove(call)) throw AssertionError("Call wasn't in-flight!")
      		idleCallback = this.idleCallback
    	}
        //判断是否有需要处理的网络请求
    	val isRunning = promoteAndExecute()
    	if (!isRunning && idleCallback != null) {
      		idleCallback.run()
    	}
     }
    
}
```

## 2、异步请求

RealCall 的 `enqueue`方法会将外部传入的 Callback 包装为一个 AsyncCall 对象后传给 dispatcher

```kotlin
class RealCall(
  val client: OkHttpClient,
  /** The application's original request unadulterated by redirects or auth headers. */
  val originalRequest: Request,
  val forWebSocket: Boolean
) : Call {
    
  override fun enqueue(responseCallback: Callback) {
    check(executed.compareAndSet(false, true)) { "Already Executed" }
    callStart()
    client.dispatcher.enqueue(AsyncCall(responseCallback))
  }
    
}
```

由于 `enqueue`对应的是异步请求，所以 OkHttp 内部就需要自己构造一个线程来执行请求，在请求结束后再通过 Callback 来将结果值回调给外部，异步请求逻辑对应的载体就是 AsyncCall 这个类

AsyncCall 是 RealCall 的非静态内部类，所以 AsyncCall 可以访问到 RealCall 的所有变量和方法。此外，AsyncCall 继承了 Runnable 接口，其 `executeOn` 方法就用于传入一个线程池对象来执行`run` 方法。`run` 方法内还是调用了 `getResponseWithInterceptorChain()`方法来获取 response，并通过 Callback 来将执行结果（不管成功还是失败）回调出去，在请求结束后也会将自身从 dispatcher 中移除

```kotlin
internal inner class AsyncCall(private val responseCallback: Callback) : Runnable {
	
    @Volatile var callsPerHost = AtomicInteger(0)
  		private set

	fun reuseCallsPerHostFrom(other: AsyncCall) {
  		this.callsPerHost = other.callsPerHost
	}

	fun executeOn(executorService: ExecutorService) {
        client.dispatcher.assertThreadDoesntHoldLock()
        var success = false
        try {
            executorService.execute(this)
            success = true
        } catch (e: RejectedExecutionException) {
            val ioException = InterruptedIOException("executor rejected")
            ioException.initCause(e)
            noMoreExchanges(ioException)
            responseCallback.onFailure(this@RealCall, ioException)
        } finally {
            if (!success) {
                client.dispatcher.finished(this) // This call is no longer running!
            }
        }
    }

    override fun run() {
        threadName("OkHttp ${redactedUrl()}") {
            var signalledCallback = false
            timeout.enter()
            try {
                val response = getResponseWithInterceptorChain()
                signalledCallback = true
                responseCallback.onResponse(this@RealCall, response)
            } catch (e: IOException) {
                if (signalledCallback) {
                    // Do not signal the callback twice!
                    Platform.get().log("Callback failure for ${toLoggableString()}", Platform.INFO, e)
                } else {
                    responseCallback.onFailure(this@RealCall, e)
                }
            } catch (t: Throwable) {
                cancel()
                if (!signalledCallback) {
                    val canceledException = IOException("canceled due to $t")
                    canceledException.addSuppressed(t)
                    responseCallback.onFailure(this@RealCall, canceledException)
                }
                throw t
            } finally {
                client.dispatcher.finished(this)
            }
        }
    }
}
```

Dispatcher 在拿到 AsyncCall 对象后，会先将其存到 readyAsyncCalls 中，然后通过 `findExistingCallWithHost`方法来查找当前是否有指向同一 Host 的异步请求，有的话则交换 callsPerHost 变量，该变量就用于标记当前指向同一 Host 的请求数量，最后调用 `promoteAndExecute` 方法来判断当前是否允许发起请求

```kotlin
class Dispatcher constructor() {
 
   internal fun enqueue(call: AsyncCall) {
    synchronized(this) {
      readyAsyncCalls.add(call)

      // Mutate the AsyncCall so that it shares the AtomicInteger of an existing running call to
      // the same host.
      if (!call.call.forWebSocket) {
        //查找当前是否有指向同一 Host 的异步请求
        val existingCall = findExistingCallWithHost(call.host)
        if (existingCall != null) call.reuseCallsPerHostFrom(existingCall)
      }
    }
    promoteAndExecute()
  }

  private fun findExistingCallWithHost(host: String): AsyncCall? {
    for (existingCall in runningAsyncCalls) {
      if (existingCall.host == host) return existingCall
    }
    for (existingCall in readyAsyncCalls) {
      if (existingCall.host == host) return existingCall
    }
    return null
  }
    
}
```

由于当前正在执行的网络请求总数可能已经达到限制，或者是指向同一 Host 的请求也达到限制了，所以 `promoteAndExecute()`方法就用于从待执行列表 readyAsyncCalls 中获取当前符合运行条件的所有请求，将请求存到 runningAsyncCalls 中，并调用线程池来执行

```kotlin
private fun promoteAndExecute(): Boolean {
    this.assertThreadDoesntHoldLock()

    val executableCalls = mutableListOf<AsyncCall>()
    val isRunning: Boolean
    synchronized(this) {
      val i = readyAsyncCalls.iterator()
      while (i.hasNext()) {
        val asyncCall = i.next()
	    //如果当前正在执行的异步请求总数已经超出限制，则直接返回
        if (runningAsyncCalls.size >= this.maxRequests) break // Max capacity.
        //如果指向同个 Host 的请求总数已经超出限制，则取下一个请求
        if (asyncCall.callsPerHost.get() >= this.maxRequestsPerHost) continue // Host max capacity.

        i.remove()
        //将 callsPerHost 递增加一，表示指向该 Host 的链接数加一了
        asyncCall.callsPerHost.incrementAndGet()
        //将 asyncCall 存到可执行列表中
        executableCalls.add(asyncCall)
        //将 asyncCall 存到正在执行列表中
        runningAsyncCalls.add(asyncCall)
      }
      isRunning = runningCallsCount() > 0
    }

    //执行所有符合条件的请求
    for (i in 0 until executableCalls.size) {
      val asyncCall = executableCalls[i]
      asyncCall.executeOn(executorService)
    }

    return isRunning
  }
```

## 3、ArrayDeque 

上面有讲到，三种请求的存储容器是 ArrayDeque。ArrayDeque 属于非线程安全的双端队列，所以涉及到多线程操作时都需要外部主动线程同步。那么让我们想一想，OkHttp 选择 ArrayDeque 作为任务容器的理由是什么？以我粗浅的眼光来看，有以下几点：

- ArrayDeque 内部使用数组结构来存储数据，元素具有明确的先后顺序，这符合我们对网络请求**先到先执行**的基本预期
- 在选择符合运行条件的异步请求时，需要对 readyAsyncCalls 进行遍历，数组在遍历效率上会比较高
- 在遍历到符合条件的请求后，需要将请求从 readyAsyncCalls 中移除并转移到 runningAsyncCalls 中，而 ArrayDeque 作为双端队列，在内存空间利用率上比较高
- Dispatcher 面对的就是多线程环境，本身就需要进行线程同步，选择 ArrayDeque 这个非线程安全的容器可以省去多余的线程同步消耗

## 4、线程池

OkHttp 的异步请求是交由其内部的线程池来完成的，该线程池就长这样：

```kotlin
private var executorServiceOrNull: ExecutorService? = null

@get:Synchronized
@get:JvmName("executorService") val executorService: ExecutorService
get() {
  if (executorServiceOrNull == null) {
    executorServiceOrNull = ThreadPoolExecutor(0, Int.MAX_VALUE, 60, TimeUnit.SECONDS,
        SynchronousQueue(), threadFactory("$okHttpName Dispatcher", false))
  }
  return executorServiceOrNull!!
}
```

该线程池的参数设置有什么优势呢？以我粗浅的眼光来看，有以下两点：

1. 核心线程数为 0，线程超时时间是 60 秒。说明在没有待执行的任务的时候，如果线程闲置了 60 秒，那么线程就会被回收，这可以避免空闲线程白白浪费系统资源，适合于移动设备资源紧缺的情景
2. 允许的最大线程数为 Int.MAX_VALUE，可以看做是完全没有限制的，且任务队列是 SynchronousQueue。SynchronousQueue 的特点是当有任务入队时，必须等待该任务被消费否则入队操作就会一直被阻塞，而由于线程池允许的最大线程数量是无限的，所以每个入队的任务都能马上交由线程处理（交付给空闲线程或者新建一个线程来处理），这就保证了任务的处理及时性，符合我们对网络请求应该尽快发起并完成的期望

虽然线程池本身对于最大线程数几乎没有限制，但是由于提交任务的操作还受 maxRequests 的控制，所以实际上该线程池最多同时运行 maxRequests 个线程

## 5、推动请求执行

既然 OkHttp 内部的线程池是不可能无限制地新建线程来执行请求的，那么当请求总数已达到 maxRequests 后，后续的请求只能是先处于等待状态，那么这些等待状态的请求会在什么时候被启动呢？

同步请求和异步请求结束后都会调用到 Dispatcher 的两个 `finished` 方法，在这两个方法里又会触发到 `promoteAndExecute()`方法去遍历任务列表来执行，此时就推动了待处理列表的任务执行操作。所以说，Dispatcher 中的请求都可以看做是在自发性地启动，每个请求结束都会自动触发下一个请求执行（如果有的话），省去了多余的定时检查这类操作

```kotlin
/** Used by [AsyncCall.run] to signal completion. */
internal fun finished(call: AsyncCall) {
	call.callsPerHost.decrementAndGet()
	finished(runningAsyncCalls, call)
}

/** Used by [Call.execute] to signal completion. */
internal fun finished(call: RealCall) {
	finished(runningSyncCalls, call)
}

private fun <T> finished(calls: Deque<T>, call: T) {
	val idleCallback: Runnable?
	synchronized(this) {
  		if (!calls.remove(call)) throw AssertionError("Call wasn't in-flight!")
  		idleCallback = this.idleCallback
	}
	//判断当前是否有可以启动的待执行任务，有的话则启动
	val isRunning = promoteAndExecute()

	if (!isRunning && idleCallback != null) {
  		idleCallback.run()
	}
}
```

## 6、总结

- 如果是同步请求，那么网络请求过程就会直接在调用者所在线程上完成，不受 Dispatcher 的控制
- 如果是异步请求，该请求会先存到待执行列表 readyAsyncCalls 中，该请求是否可以立即发起受 maxRequests 和 maxRequestsPerHost 两个条件的限制。如果符合条件，那么就会从 readyAsyncCalls 取出并存到 runningAsyncCalls 中，然后交由 OkHttp 内部的线程池来执行
- 不管外部是同步请求还是异步请求，内部都是通过调用`getResponseWithInterceptorChain()`方法来拿到 Response 的
- Dispatcher 内部的线程池本身允许同时运行 Int.MAX_VALUE 个线程，但是实际上的线程数量还是受 maxRequests 的控制

# 五、RealInterceptorChain 

重点再来看 `getResponseWithInterceptorChain()`方法，其主要逻辑就是通过拦截器来完成整个网络请求过程。在该方法中，除了会获取外部主动设置的拦截器外，也会默认添加以下几个拦截器

1. RetryAndFollowUpInterceptor。负责失败重试以及重定向
2. BridgeInterceptor。负责对用户构造的 Request 进行转换，添加必要的 header 和 cookie，在得到 response 后如果有需要的会进行 gzip 解压
3. CacheInterceptor。用于处理缓存
4. ConnectInterceptor。负责和服务器建立连接
5. CallServerInterceptor。负责向服务器发送请求和从服务器接收数据

最后，request 和 interceptors 会用来生成一个 RealInterceptorChain 对象，由其来最终返回 response

```kotlin
@Throws(IOException::class)
internal fun getResponseWithInterceptorChain(): Response {
    // Build a full stack of interceptors.
    val interceptors = mutableListOf<Interceptor>()
    //添加开发者设置的拦截器
    interceptors += client.interceptors
    
    //添加默认的拦截器
    interceptors += RetryAndFollowUpInterceptor(client)
    interceptors += BridgeInterceptor(client.cookieJar)
    interceptors += CacheInterceptor(client.cache)
    interceptors += ConnectInterceptor
      
    if (!forWebSocket) {
      //如果不是 WebSocket 的话，那就再添加开发者设置的 NetworkInterceptors
      interceptors += client.networkInterceptors
    }
      
    //CallServerInterceptor 是实际上发起网络请求的地方
    interceptors += CallServerInterceptor(forWebSocket)

    val chain = RealInterceptorChain(
        call = this,
        interceptors = interceptors,
        index = 0,
        exchange = null,
        request = originalRequest,
        connectTimeoutMillis = client.connectTimeoutMillis,
        readTimeoutMillis = client.readTimeoutMillis,
        writeTimeoutMillis = client.writeTimeoutMillis
    )

    var calledNoMoreExchanges = false
    try {
      val response = chain.proceed(originalRequest)
      if (isCanceled()) {
        response.closeQuietly()
        throw IOException("Canceled")
      }
      return response
    } catch (e: IOException) {
      calledNoMoreExchanges = true
      throw noMoreExchanges(e) as Throwable
    } finally {
      if (!calledNoMoreExchanges) {
        noMoreExchanges(null)
      }
    }
}  
```

**Interceptor 是 OkHttp 里很重要的一环，OkHttp 也是靠此为开发者提供了很高的自由度**。Interceptor 接口本身只包含一个 `intercept` 方法，在此方法内可拿到原始的 Request 对象以及最终的 Response

```kotlin
fun interface Interceptor {
   @Throws(IOException::class)
   fun intercept(chain: Chain): Response   
}
```

例如，我们可以自定义一个 LogInterceptor 来打印网络请求的请求参数以及最终的返回值

```kotlin
class LogInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        println(request)
        val response = chain.proceed(request)
        println(response)
        return response
    }

}
```

Interceptor 的实现初衷是为了给开发者提供一个可以控制网络请求的**发起过程**及**收尾工作**的入口，例如**添加 header、日志记录、请求拦截、ResponseBody修改 **等，每个 Interceptor 只负责自己关心的操作，那么势必就会有添加多个 Interceptor 的需求

我们知道，只有让每个 Interceptor 都依次处理完 request 之后，OkHttp 才能根据最终的 request 对象去联网请求得到 response，所以每个 Interceptor 需要依次拿到 request 进行自定义处理。请求到 response 后，Interceptor 可能还需要对 response 进行处理，那么就还需要将 response 再依次传递给每个 Interceptor。那么，怎么实现将多个 Interceptor 给串联起来呢？

**这里来看一个简化版本的 Interceptor 实现思路**

假设我们自己定义的 Interceptor 实现类有两个：LogInterceptor 和 HeaderInterceptor，这里只是简单地将获取到 request 和 response 的时机给打印出来，重点是要看每个 Interceptor 的先后调用顺序。为了将两个 Interceptor 给串联起来，RealInterceptorChain 会循环获取 index 指向的下一个 Interceptor 对象，每次构建一个新的 RealInterceptorChain 对象作为参数来调用 `intercept` 方法，这样外部只需要调用一次 `realInterceptorChain.proceed` 就可以拿到最终的 response 对象

```kotlin
class Request

class Response

interface Chain {

    fun request(): Request

    fun proceed(request: Request): Response

}

interface Interceptor {

    fun intercept(chain: Chain): Response

}

class RealInterceptorChain(
    private val request: Request,
    private val interceptors: List<Interceptor>,
    private val index: Int
) : Chain {

    private fun copy(index: Int): RealInterceptorChain {
        return RealInterceptorChain(request, interceptors, index)
    }

    override fun request(): Request {
        return request
    }

    override fun proceed(request: Request): Response {
        val next = copy(index = index + 1)
        val interceptor = interceptors[index]
        val response = interceptor.intercept(next)
        return response
    }

}

class LogInterceptor : Interceptor {
    override fun intercept(chain: Chain): Response {
        val request = chain.request()
        println("LogInterceptor -- getRequest")
        val response = chain.proceed(request)
        println("LogInterceptor ---- getResponse")
        return response
    }
}

class HeaderInterceptor : Interceptor {
    override fun intercept(chain: Chain): Response {
        val request = chain.request()
        println("HeaderInterceptor -- getRequest")
        val response = chain.proceed(request)
        println("HeaderInterceptor ---- getResponse")
        return response
    }
}

fun main() {
    val interceptorList = mutableListOf<Interceptor>()
    interceptorList.add(LogInterceptor())
    interceptorList.add(HeaderInterceptor())
    val request = Request()
    val realInterceptorChain = RealInterceptorChain(request, interceptorList, 0)
    val response = realInterceptorChain.proceed(request)
    println("main response")
}
```

上面的代码看着思路还可以，可是当运行后就会发现抛出了 IndexOutOfBoundsException，因为代码里并没有对 index 进行越界判断。而且，上面的代码也缺少了一个真正的生成 Response 对象的地方，每个 Interceptor 只是在进行中转调用而已，因此还需要一个来真正地完成网络请求并返回 Response 对象的地方，即 CallServerInterceptor

所以，CallServerInterceptor 的`intercept` 方法就用来真正地执行网络请求并生成 Response 对象，在这里就不能再调用 `proceed` 方法了，且 CallServerInterceptor 需要放在 interceptorList 的最后一位

```kotlin
class CallServerInterceptor : Interceptor {
    override fun intercept(chain: Chain): Response {
        val request = chain.request()
        println("CallServerInterceptor -- getRequest")
        val response = Response()
        println("CallServerInterceptor ---- getResponse")
        return response
    }
}

fun main() {
    val interceptorList = mutableListOf<Interceptor>()
    interceptorList.add(LogInterceptor())
    interceptorList.add(HeaderInterceptor())
    interceptorList.add(CallServerInterceptor())
    val request = Request()
    val realInterceptorChain = RealInterceptorChain(request, interceptorList, 0)
    val response = realInterceptorChain.proceed(request)
    println("main response")
}
```

最终的运行结果如下所示，可以看出来，`intercept` 方法是根据添加顺序来调用，而 response 是按照反方向来传递

```kotlin
LogInterceptor -- getRequest
HeaderInterceptor -- getRequest
CallServerInterceptor -- getRequest
CallServerInterceptor ---- getResponse
HeaderInterceptor ---- getResponse
LogInterceptor ---- getResponse
main response
```

以上代码我简化了 OkHttp 在实现 RealInterceptorChain 时的思路，其本质上就是通过将多个拦截器以责任链的方式来一层层调用，上一个拦截器处理完后将就将结果传给下一个拦截器，直到最后一个拦截器（即 CallServerInterceptor ）处理完后将 Response 再一层层往上传递

```kotlin
class RealInterceptorChain(
  internal val call: RealCall,
  private val interceptors: List<Interceptor>,
  private val index: Int,
  internal val exchange: Exchange?,
  internal val request: Request,
  internal val connectTimeoutMillis: Int,
  internal val readTimeoutMillis: Int,
  internal val writeTimeoutMillis: Int
) : Interceptor.Chain {
    
  internal fun copy(
    index: Int = this.index,
    exchange: Exchange? = this.exchange,
    request: Request = this.request,
    connectTimeoutMillis: Int = this.connectTimeoutMillis,
    readTimeoutMillis: Int = this.readTimeoutMillis,
    writeTimeoutMillis: Int = this.writeTimeoutMillis
  ) = RealInterceptorChain(call, interceptors, index, exchange, request, connectTimeoutMillis,
      readTimeoutMillis, writeTimeoutMillis)
    
  @Throws(IOException::class)
  override fun proceed(request: Request): Response {
    ···
    val next = copy(index = index + 1, request = request)
    val interceptor = interceptors[index]
    @Suppress("USELESS_ELVIS")
    val response = interceptor.intercept(next) ?: throw NullPointerException(
        "interceptor $interceptor returned null")
    ···
    return response
  }
    
}
```

# 六、Interceptor

我们在构建 OkHttpClient 的时候，添加拦截器的方法分为两类：`addInterceptor`和`addNetworkInterceptor`

```kotlin
val okHttClient = OkHttpClient.Builder()
    .addInterceptor { chain ->
        chain.proceed(chain.request())
    }
    .addNetworkInterceptor { chain ->
        chain.proceed(chain.request())
    }
    .build()
```

Interceptor 和 NetworkInterceptor 分别被称为**应用拦截器**和**网络拦截器**，那么它们有什么区别呢？

前面有讲到，OkHttp 在执行拦截器的时候，是按照如下顺序的，这个顺序就已经决定了不同拦截器的调用时机差异

```kotlin
val interceptors = mutableListOf<Interceptor>()
interceptors += client.interceptors
interceptors += RetryAndFollowUpInterceptor(client)
interceptors += BridgeInterceptor(client.cookieJar)
interceptors += CacheInterceptor(client.cache)
interceptors += ConnectInterceptor
if (!forWebSocket) {
  	interceptors += client.networkInterceptors
}
interceptors += CallServerInterceptor(forWebSocket)
```

- 由于应用拦截器处于列表头部，所以在整个责任链路中应用拦截器会首先被执行，即使之后在 RetryAndFollowUpInterceptor 中发生了**请求失败重试或者网络重定向**等情况，应用拦截器也只会被触发一次，但网络拦截器会被调用多次
- 网络拦截器位于 CacheInterceptor 之后，那么当 CacheInterceptor  命中缓存的时候就不会去执行网络请求了，此时网络拦截器就不会被调用，因此网络拦截器是存在短路的可能。此外，网络拦截器位于 ConnectInterceptor 之后，在调用网络拦截器之前就已经准备好网络链接了，说明网络拦截器本身就关联着实际的网络请求逻辑
- 从单次请求流程上来看，应用拦截器被调用并不意味着真正有发起了网络请求，而网络拦截器被调用就说明的确发起了一次网络请求。因此如果我们希望通过拦截器来记录网络请求详情的话，就需要考虑两者的调用时机差异：应用拦截器无法感知到 OkHttp 自动添加的一些 header，但是网络拦截器可以；应用拦截器除非主动中断请求，否则每次请求一定都会被执行，但网络拦截器可能存在被短路的可能

借用官方的一张图片来表示

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/46eebdde38824c5491cdb54450dadc7e~tplv-k3u1fbpfcp-zoom-1.image)

这里可以根据 [square](https://github.com/square/okhttp/blob/master/samples/guide/src/main/java/okhttp3/recipes/Progress.java) 官方提供的一个例子，来实现在下载一张 10 MB 图片的时候通过拦截器对下载进度进行监听，并同时把图片下载到系统的桌面

实现思路就是对原始的 ResponseBody 进行多一层代理，计算已经从网络中读取到的字节数和资源的 contentLength 之间的百分比，从而得到下载进度。此外，因为该拦截器是和确切的网络请求相关，所以应该要设为网络拦截器才比较合理

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 */
fun main() {
    run()
}

interface ProgressListener {
    fun update(bytesRead: Long, contentLength: Long, done: Boolean)
}

private fun run() {
    val request = Request.Builder()
        .url("https://images.pexels.com/photos/5177790/pexels-photo-5177790.jpeg")
        .build()
    val progressListener: ProgressListener = object : ProgressListener {
        var firstUpdate = true
        override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
            if (done) {
                println("completed")
            } else {
                if (firstUpdate) {
                    firstUpdate = false
                    if (contentLength == -1L) {
                        println("content-length: unknown")
                    } else {
                        System.out.format("content-length: %d\n", contentLength)
                    }
                }
                println(bytesRead)
                if (contentLength != -1L) {
                    System.out.format("%d%% done\n", 100 * bytesRead / contentLength)
                }
            }
        }
    }
    val client = OkHttpClient.Builder()
        .addNetworkInterceptor { chain: Interceptor.Chain ->
            val originalResponse = chain.proceed(chain.request())
            originalResponse.newBuilder()
                .body(ProgressResponseBody(originalResponse.body!!, progressListener))
                .build()
        }
        .build()
    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            throw IOException("Unexpected code $response")
        }
        val desktopDir = FileSystemView.getFileSystemView().homeDirectory
        val imageFile = File(desktopDir, "${System.currentTimeMillis()}.jpeg")
        imageFile.createNewFile()
        //读取 InputStream 写入到图片文件中
        response.body!!.byteStream().copyTo(imageFile.outputStream())
    }
}

private class ProgressResponseBody constructor(
    private val responseBody: ResponseBody,
    private val progressListener: ProgressListener
) : ResponseBody() {

    private var bufferedSource: BufferedSource? = null

    override fun contentType(): MediaType? {
        return responseBody.contentType()
    }

    override fun contentLength(): Long {
        return responseBody.contentLength()
    }

    override fun source(): BufferedSource {
        if (bufferedSource == null) {
            bufferedSource = source(responseBody.source()).buffer()
        }
        return bufferedSource!!
    }

    private fun source(source: Source): Source {

        return object : ForwardingSource(source) {

            var totalBytesRead = 0L

            @Throws(IOException::class)
            override fun read(sink: Buffer, byteCount: Long): Long {
                val bytesRead = super.read(sink, byteCount)
                // read() returns the number of bytes read, or -1 if this source is exhausted.
                totalBytesRead += if (bytesRead != -1L) bytesRead else 0
                progressListener.update(totalBytesRead, responseBody.contentLength(), bytesRead == -1L)
                return bytesRead
            }
        }
    }

}
```

进度输出就类似于：

```kotlin
content-length: 11448857
467
0% done
1836
0% done
3205

···

99% done
11442570
99% done
11448857
100% done
completed
```

# 七、结尾

关于 OkHttp 的源码讲解到这里就结束了，但本文还缺少了对 ConnectInterceptor 和 CallServerInterceptor 的讲解，这两者是 OkHttp 完成实际网络请求的地方，涉及到了 Connection 和 Socket 这些比较底层的领域，我没法讲得多清晰，就直接略过这块内容了~~

OkHttp 的运行效率很高，但在使用上还是比较原始，一般我们还是需要在 OkHttp 之上进行一层封装，Retrofit 就是一个对 OkHttp 的优秀封装库，对 Retrofit 的源码讲解可以看我的这篇文章：[三方库源码笔记（7）-Retrofit 源码详解](https://juejin.im/post/6886121327845965838)

下篇文章就来写关于 OkHttp 拦截器的实战内容吧