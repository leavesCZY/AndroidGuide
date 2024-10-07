我在 Github 上有一个开源库，一个适用于 Android 的字节码插桩库：[Track](https://github.com/leavesCZY/Track)，提供了一些方便实用的字节码插桩功能，引用方可以直接开箱即用

Track 在 v1.0.0 版本包含以下四个功能点：

- viewClickTrack：View Click 双击防抖
- composeClickTrack：Jetpack Compose Click 双击防抖
- toastTrack：收拢应用内所有的 Toast.show 方法，可用于解决在 Android 7.1 系统上 Toast 由于 WindowToken 失效从而导致应用崩溃的问题
- replaceClassTrack：修改指定类的继承关系，将指定父类替换为另一个类

最近有位同学向 Track 提了一个 [issues](https://github.com/leavesCZY/Track/issues/6)，说希望我为 Track 添加一个代码替换功能，其本意是 **想要将项目中所有获取 Android ID 的方法指向到其它自定义的方法**

这个需求也许是来源于当前国内严格的隐私安全合规要求：在用户同意隐私协议之前，应用不能有收集用户隐私数据（Android ID、Device ID、MAC）的行为。这就和我以前写过的一篇文章有点关系了：[ASM 字节码插桩：助力隐私合规](https://juejin.cn/post/7046207125785149448)

在这篇文章里，我介绍了如何通过字节码插桩的方式来定位项目中的隐私合规问题，以 **静态扫描** 加上 **动态记录** 的方式来排查当前项目中所有的隐私操作。这种方式已经能够排查出大部分的隐私合规问题了，但并不保险。因为随着项目更迭，随时可能有新的隐私安全问题被引入进来，而如果每次发版前都要重新走一遍上述流程来排查问题的话，效率也十分感人。更为保险的做法应该是直接拦截项目中所有获取敏感数据的方法，根据用户是否已同意隐私协议来动态决定是否要真正去执行敏感操作

当时我并未实现此功能，只是在文章末尾留下了一个 TODO

![](https://upload-images.jianshu.io/upload_images/2552605-90af344e41a37b7e.png)

本文我就来解决 issues 并补上这个 TODO，顺便也为 Track 添加了三个新的功能点，发布了 v1.1.0 版本：

- replaceFieldTrack：修改指定字段的调用链，将指定字段替换为另一个字段
- replaceMethodTrack：修改指定方法的调用链，将指定方法替换为另一个方法
- optimizedThreadTrack：收拢应用内所有的 Executors 线程池方法。可用于实现线程整治，统一应用内所有的线程池实例

## 原理

在 issues 中提到的所谓的 **代码替换**，可以简单划分为以下四种

- 替换调用实例方法的指令。例如替换 `telephonyManager.getDeviceId()`
- 替换调用实例字段的指令。例如替换 `Point().x`
- 替换调用静态方法的指令。例如替换 `Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)`
- 替换调用静态字段的指令。例如替换 `android.os.Build.BRAND`

以 `telephonyManager.getDeviceId()` 为例，要实现的效果如下所示。该方法被转交由 SystemMethodProxy 内自定义的 `getDeviceId()` 方法来实现，我们可以在此处根据用户是否已同意隐私协议，来动态决定是否要真正地去获取 deviceId

```kotlin
//插桩前
private fun getDeviceId(context: Context): String {
    val telephonyManager = context.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
    return telephonyManager.deviceId!!
}

//插桩后
private fun getDeviceId(context: Context): String {
    val telephonyManager = context.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
    return SystemMethodProxy.getDeviceId(telephonyManager = telephonyManager)
}

private object SystemMethodProxy {
    @JvmStatic
    fun getDeviceId(telephonyManager: TelephonyManager): String {
        val agreed: Boolean
        return if (agreed) {
            telephonyManager.deviceId
        } else {
            "empty"
        }
    }
}
```

通过 Android Studio 可以直接查看 Kotlin 代码对应的字节码信息，我们可以很直观地看出来 `telephonyManager.getDeviceId` 和 `SystemMethodProxy.getDeviceId` 这两个方法之间的字节码差异

![](https://upload-images.jianshu.io/upload_images/2552605-11f226195fcf40b0.png)

`telephonyManager.getDeviceId` 方法在字节码层面符合以下规则：

- 调用的是实例方法，所以对应 INVOKEVIRTUAL
- 调用的实例对象是 TelephonyManager 类型，所以对应 `android/telephony/TelephonyManager` 
- 不包含入参参数，且返回值类型是 String，所以方法签名是 `()Ljava/lang/String;` 

`SystemMethodProxy.getDeviceId` 方法在字节码层面符合以下规则：

- 调用的是静态方法，所以对应 INVOKESTATIC
- 调用的实例对象是 SystemMethodProxy 类型，所以对应 `github/leavesczy/track/replace/instruction/SystemMethodProxy` 
- 多了一个入参参数 TelephonyManager，所以方法签名变成了 `(Landroid/telephony/TelephonyManager;)Ljava/lang/String;`

`telephonyManager.getDeviceId()` 对应的是实例方法，再来举个调用静态方法的例子：`Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)`

![](https://upload-images.jianshu.io/upload_images/2552605-a7eb31053661739c.png)

上图在字节码层面上仅有一个差异：调用方从 `android/provider/Settings$Secure` 变成了 `github/leavesczy/track/replace/instruction/SystemMethodProxy`。由于前后调用的都是静态方法，且无需像实例方法那样将调用方作为入参参数，所以方法调用指令、方法名、方法签名等都保持不变

在了解插桩前后的字节码差异后，我们要做的就是在字节码层面按照规则一一进行修改，不管要替换的是字段还是方法都是如此

Track 中的 `replaceFieldTrack` 和 `replaceMethodTrack` 就是分别用来替换字段和方法

- replaceFieldTrack：修改指定字段的调用链，将指定字段替换为另一个字段
- replaceMethodTrack：修改指定方法的调用链，将指定方法替换为另一个方法

开发者在引入 Track Plugin 后，声明自己想要替换的字段和方法的特征信息，并提供相应的中转类 SystemFieldProxy 和 SystemMethodProxy，即可实现上述效果了

```kotlin
replaceFieldTrack {
    isEnabled = true
    include = setOf()
    exclude = setOf()
    instructions = setOf(
        ReplaceInstruction(
            owner = "android.os.Build",
            name = "BRAND",
            descriptor = "Ljava/lang/String;",
            proxyOwner = "github.leavesczy.track.replace.instruction.SystemFieldProxy"
        )
    )
}

replaceMethodTrack {
    isEnabled = true
    include = setOf()
    exclude = setOf()
    val systemMethodProxyOwner = "github.leavesczy.track.replace.instruction.SystemMethodProxy"
    instructions = setOf(
        ReplaceInstruction(
            owner = "android.telephony.TelephonyManager",
            name = "getDeviceId",
            descriptor = "()Ljava/lang/String;",
            proxyOwner = systemMethodProxyOwner
        ),
        ReplaceInstruction(
            owner = "android.provider.Settings\$Secure",
            name = "getString",
            descriptor = "(Landroid/content/ContentResolver;Ljava/lang/String;)Ljava/lang/String;",
            proxyOwner = systemMethodProxyOwner
        )
    )
}
```

之后，Track 就会同时根据 owner、name、descriptor 等信息找到所有符合规则的字段和方法，然后将其一一修改为指向 proxyOwner

```kotlin
private class ReplaceInstructionMethodVisitor(
    api: Int,
    methodVisitor: MethodVisitor,
    private val classNode: ClassNode,
    private val config: ReplaceInstructionConfig
) : MethodVisitor(api, methodVisitor) {

    override fun visitFieldInsn(
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?
    ) {
        val find = config.instructions.find {
            it.owner == owner && it.name == name && (it.descriptor.isEmpty() || it.descriptor == descriptor)
        }
        if (find != null && opcode == Opcodes.GETSTATIC) {
            val proxyOwner = replacePeriodWithSlash(className = find.proxyOwner)
            super.visitFieldInsn(opcode, proxyOwner, name, descriptor)
            LogPrint.normal(tag = config.extensionName) {
                "${classNode.name} 发现符合规则的指令：$owner $name $descriptor , 替换为 $proxyOwner $name $descriptor ，完成处理..."
            }
        } else {
            super.visitFieldInsn(opcode, owner, name, descriptor)
        }
    }

    override fun visitMethodInsn(
        opcode: Int,
        owner: String,
        name: String,
        descriptor: String,
        isInterface: Boolean
    ) {
        val find = config.instructions.find {
            it.owner == owner && it.name == name && (it.descriptor.isEmpty() || it.descriptor == descriptor)
        }
        val mOpcode: Int
        val mOwner: String
        val mDescriptor: String
        if (find != null) {
            mOwner = replacePeriodWithSlash(className = find.proxyOwner)
            if (opcode == Opcodes.INVOKEVIRTUAL) {
                mOpcode = Opcodes.INVOKESTATIC
                mDescriptor = insetAsFirstArgument(descriptor = descriptor, owner = owner)
            } else {
                mOpcode = opcode
                mDescriptor = descriptor
            }
            LogPrint.normal(tag = config.extensionName) {
                "${classNode.name} 发现符合规则的指令：$owner $name $descriptor , 替换为 $mOwner $name $mDescriptor ，完成处理..."
            }
        } else {
            mOpcode = opcode
            mOwner = owner
            mDescriptor = descriptor
        }
        super.visitMethodInsn(mOpcode, mOwner, name, mDescriptor, isInterface)
    }

    private fun insetAsFirstArgument(descriptor: String, owner: String): String {
        val argumentsDescriptor = descriptor.substringAfter('(').substringBefore(')')
        val arguments = argumentsDescriptor.split(";").toMutableList().apply {
            add(0, "L${owner}")
        }.joinToString(separator = ";")
        return "(${arguments})" + descriptor.substringAfter(')')
    }

}
```

## 扩展

`replaceMethodTrack` 的使用场景十分丰富。例如，假设你的项目中有一个三方依赖库包含一个会引发崩溃的方法，而三方依赖库作者却迟迟没有修复，此时就可以通过 `replaceMethodTrack` 将该方法的实现逻辑转交由我们自定义的方法来实现，从而避免崩溃

而对应到 Track，基于 `replaceMethodTrack` 我还扩展实现了两个功能：

- optimizedThreadTrack：收拢应用内所有的 Executors 线程池方法，可用于实现线程整治，统一应用内所有的线程池实例
- toastTrack：收拢应用内所有的 Toast.show 方法，可用于解决在 Android 7.1 系统上 Toast 由于 WindowToken 失效从而导致应用崩溃的问题

以 `optimizedThreadTrack` 为例

我们知道，JDK 中的 `java.util.concurrent.Executors` 类内包含多个创建线程池 ThreadPoolExecutor 的方法，例如 `newSingleThreadExecutor、newCachedThreadPool、newScheduledThreadPool` 等

```kotlin
public class Executors {

    public static ExecutorService newFixedThreadPool(int nThreads)

    public static ExecutorService newWorkStealingPool(int parallelism)

    public static ExecutorService newWorkStealingPool()

    public static ExecutorService newFixedThreadPool(int nThreads, ThreadFactory threadFactory)

    public static ExecutorService newSingleThreadExecutor()

    public static ExecutorService newSingleThreadExecutor(ThreadFactory threadFactory)

    public static ExecutorService newCachedThreadPool()

    public static ExecutorService newCachedThreadPool(ThreadFactory threadFactory)

    public static ScheduledExecutorService newSingleThreadScheduledExecutor()

    public static ScheduledExecutorService newSingleThreadScheduledExecutor(ThreadFactory threadFactory)

    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize)

    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize, ThreadFactory threadFactory)

}
```

除了我们的自有代码会去通过 Executors 创建线程池外，很多依赖的三方库也会使用到。而这些线程池的 `corePoolSize、maximumPoolSize、keepAliveTime、threadFactory` 等参数可能设置得并不合理：`corePoolSize` 可能数量过大，导致并行的线程数量过大；`keepAliveTime` 可能时间过长，导致空闲线程长时间无法被回收

`optimizedThreadTrack` 就用于解决上述问题。通过将项目内的这些方法均归拢到开发者指定的类中，开发者可在此类中统一线程名、线程优先级等属性，甚至也可以直接返回一个单例模式的线程池实例，从而缩减应用内的线程数量

例如，开发者可以像以下代码一样来承接 Executors 的线程池方法，在里面设置一个统一的 `keepAliveTime` 和 `threadFactory`

```kotlin
optimizedThreadTrack {
    isEnabled = true
    include = setOf()
    exclude = setOf()
    proxyOwner = "github.leavesczy.track.thread.OptimizedExecutors"
    methods = setOf(
        "newSingleThreadExecutor",
        "newCachedThreadPool"
    )
}
```

```kotlin
package github.leavesczy.track.thread

internal object OptimizedExecutors {

    private const val DEFAULT_THREAD_KEEP_ALIVE_TIME = 3000L

    @JvmStatic
    @JvmOverloads
    fun newSingleThreadExecutor(threadFactory: ThreadFactory? = null): ExecutorService {
        return getOptimizedExecutorService(
            corePoolSize = 1,
            maximumPoolSize = 1,
            keepAliveTime = 0L,
            unit = TimeUnit.MILLISECONDS,
            workQueue = LinkedBlockingQueue(),
            threadFactory = threadFactory
        )
    }

    @JvmStatic
    @JvmOverloads
    fun newCachedThreadPool(threadFactory: ThreadFactory? = null): ExecutorService {
        return getOptimizedExecutorService(
            corePoolSize = 0,
            maximumPoolSize = Integer.MAX_VALUE,
            keepAliveTime = 60L,
            unit = TimeUnit.SECONDS,
            workQueue = SynchronousQueue(),
            threadFactory = threadFactory
        )
    }

    private fun getOptimizedExecutorService(
        corePoolSize: Int,
        maximumPoolSize: Int,
        keepAliveTime: Long,
        unit: TimeUnit,
        workQueue: BlockingQueue<Runnable>,
        threadFactory: ThreadFactory?
    ): ExecutorService {
        val executor = ThreadPoolExecutor(
            corePoolSize, maximumPoolSize,
            keepAliveTime, unit,
            workQueue,
            NamedThreadFactory(threadFactory)
        )
        executor.setKeepAliveTime(DEFAULT_THREAD_KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS)
        executor.allowCoreThreadTimeOut(true)
        return executor
    }

    private class NamedThreadFactory(
        private val threadFactory: ThreadFactory?
    ) : ThreadFactory {

        private val threadId = AtomicInteger(0)

        override fun newThread(runnable: Runnable): Thread {
            val thread = threadFactory?.newThread(runnable) ?: Thread(runnable)
            val threadName = buildString {
                append("[threadId : ${threadId.getAndIncrement()}]")
                append(" - ")
                append("[threadName : ${thread.name}]")
            }
            thread.name = threadName
            if (thread.isDaemon) {
                thread.isDaemon = false
            }
            if (thread.priority != Thread.NORM_PRIORITY) {
                thread.priority = Thread.NORM_PRIORITY
            }
            return thread
        }

    }

}
```

> `optimizedThreadTrack` 和 `toastTrack` 均都是通过 `replaceMethodTrack` 来实现的，Track 仅是对其进行了封装，使其更加容易接入。开发者直接通过 `replaceMethodTrack` 也可以实现相同的功能。也正因如此，`toastTrack` 和 `optimizedThreadTrack` 才要求开发者自定义的方法名必须和原始的方法名保持一致，这样 Track 才能将方法一一对应上

我之所以会想到要开发 `optimizedThreadTrack` 这个功能点，也是源于我以前的一篇文章：[ASM 字节码插桩：进行线程整治](https://juejin.cn/post/7044339202997092383)，更多的应用场景可以参照该文章

需要注意，上述的手段并不全是可以在项目中进行全盘套用的

为线程设置一个统一和容易标识的线程名有利于出现异常时能够快速定位问题，这是比较有意义的。但为线程池设置统一的超长时间和允许回收核心线程，这是需要考虑使用场景的，设置不当反而可能会加大 OOM 的概率

如果大部分情况下线程池需要处理的任务比较少且又不允许回收核心线程的话，就会导致核心线程大部分时间处于阻塞状态得不到释放，浪费系统资源，此时通过 hook 来允许回收核心线程是比较有意义的。而如果线程池需要处理的任务是属于定时执行的话，例如固定每六秒就会有一个新任务到来，此时通过 hook 设置了线程超时时间为五秒，那就有可能导致线程刚被回收不久又需要重新创建了，这种反复的创建和回收反而加大了系统开销，反而不如直接允许核心线程常驻。此外，线程从被创建到被系统选中调度执行是有一定耗时的，这不适合于某些对响应速度要求比较高的场景，此时允许回收核心线程也是不太合适的

所以说，在针对线程池进行整治的时候，需要同时考虑该线程池是应用于什么业务场景，根据用途来进行特殊配置

关于线程池的实现细节，可以看我写的一篇 ThreadPoolExecutor 解读文章，对设计初衷和实现始末进行了详细介绍：[聊聊 Java 多线程（5）-超详细的 ThreadPoolExecutor 源码解析](https://juejin.cn/post/6901317365561032712)

## Github

对 Track 有兴趣的同学，可以到 Github 查看：[Track](https://github.com/leavesCZY/Track)

有发现问题的话也欢迎反馈
