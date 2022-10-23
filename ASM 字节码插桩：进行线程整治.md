> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

最近看了 京东零售技术 发表的一篇文章：[AOP 技术在 APP 开发中的多场景实践](https://mp.weixin.qq.com/s?__biz=MzUyMDAxMjQ3Ng==&mid=2247497288&idx=1&sn=a3aa54ab49f428106037886462c72335&chksm=f9f2619bce85e88dbc95d76f3a3528f2948687e38c75eedf9e2dee4c39d38e2910737fa0d847&scene=178&cur_album_id=1844068124342484996#rd) ，文章中介绍了 AOP 技术的一种使用场景：线程使用数优化。看完之后感觉挺有实践意义的，但文章中并没有给出具体的实现代码，所以我就做了一次实操，基本实现了文章中介绍的效果，本文就来详细介绍下我的实现始末，并给出具体的实现代码。本文部分内容也直接引用自该文章，在此表示感谢

在实现 Android 端 APP 稳定性治理的过程中，和线程相关的 OOM 问题都是避免不了的。线程是一种昂贵的系统资源，其“昂贵”不仅在于创建线程所需要的资源开销，还在于使用过程中带来的资源消耗。一个系统能够支持同时运行的线程总数受限于该系统所拥有的处理器数目和内存大小等硬件条件，线程的运行需要占用处理器时间片，系统中处于运行状态的线程越多，每个线程单位时间内能分配到的时间片就会越少，线程调度带来的上下文切换的次数就会越多，最终导致处理器真正用于运算的时间就会越少。此外，在现实场景中一个程序在其整个生命周期内需要交由线程执行的任务数量往往是远多于系统所能支持同时运行的最大线程数，如果线程数不断累加的话就会不可避免地抛出 OOM。当线程数量超载时就会在安卓虚拟机层抛出异常：`java.lang.OutOfMemoryError: pthread_create (1040KB stack) failed`

由于线程数超载导致 OOM 的原因可以归类为三种：

- 线程池数量过多。每个线程池都需要守护线程以及过多的空间开销，过多的线程池使用对于内存资源消耗过大
- 常驻线程过多。常驻线程指的是处于 waiting（等待）、blocked（阻塞）以及 runnable（运行）的线程，这在使用了线程池时比较容易出现。每个线程池会包含有一定数量的核心线程和非核心线程，默认情况下核心线程即使处于闲置状态也是不会被回收的，即默认不受 KeepAliveTime 属性的限制，这使得核心线程有可能一直处于闲置状态得不到释放，极大浪费系统资源
- 大量的匿名线程。匿名线程指的是在代码中随处启动的线程，这种方式虽然可以实现快速、优先级最高的异步化，但过多的匿名线程对于问题排查难度、稳定性都是一种挑战

对于一个经过长久迭代的项目来说，以上问题不仅仅会出现在自研业务中，同时还会涉及到多个三方 SDK 或者是开源库，而想要同时推动三方 SDK 和开源库进行线程优化是不太现实的，此时采用字节码插桩这种不局限于特定业务形态和非侵入式的方式就成了比较靠谱的一种选择

进行线程整治想要实现的效果有两点：

- 将各个匿名线程进行统一命名，命名规则：创建线程时所在类的类名 + 不断递增的线程号 + 原始代码中设置的线程名（如果有的话），方便出现异常时能够快速定位问题所在
- 统一各类线程池的超时机制。对 Executors 中的各个 newXXXXThreadPool 进行 hook，将线程池中的每个线程均按照匿名线程的规则进行命名，为线程设置统一的超时时间，且允许回收核心线程

# 一、匿名线程

先随意声明一个 Thread 对象，对应在项目中存在的各类匿名线程

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/cd3b413c83b74d829f8aafb4d23bb957~tplv-k3u1fbpfcp-zoom-1.image)

反编译查看对应的字节码指令

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/289fdaca979247a283be676c2d7d694b~tplv-k3u1fbpfcp-zoom-1.image)

从当中就可以看出几点信息：

- 第 23 行到第 27 行就对应调用 `new Thread(Runnable , String)` 这个过程
- 第 23 行的 new 指令明确指向了 java/lang/Thread
- 第 26 行的 ldc 指令代表的是从常量池中获取常量值的操作，也即获取线程名字符串 `thread name`
- 第 27 行的 invokespecial 指令对应的是调用 Thread 构造函数的过程，包含两个入参参数

该匿名线程在声明时主动设置了线程名，而项目中其它匿名线程可能并没有进行设置，而是只传入了 Runnable。因此，在字节码层面上就要考虑两种不同的指令结构了：**包含一个入参参数** 和 **包含两个入参参数**

而如果要在字节码层面上来手动生成或者是拼接 ThreadName 的话会比较麻烦，所以我采取的是一个比较巧妙的方法：将要声明的 Thread 对象替换为我们自定义的 Thread 子类，为该子类多声明一个用于传入类名的构造参数，通过这种方式就无需关心在声明 Thread 对象时到底传入了几个构造参数了，只需要在调用 invokespecial 指令时固定在末尾多传入一个代表当前类名的 String 参数即可

来看下实际的编码实现

先定义一个 Thread 子类，为其多声明一个入参参数 className，然后手动将 **原始代码中设置的线程名** 和 **className** 按照既定规则拼接在一起作为最终的线程名

```kotlin
/**
 * @Author: leavesCZY
 * @Desc:
 * @公众号：字节数组
 */
class OptimizedThread(runnable: Runnable?, name: String?, className: String) :
    Thread(runnable, generateThreadName(name, className)) {

    companion object {

        private val threadId = AtomicInteger(0)

        private fun generateThreadName(name: String?, className: String): String {
            return className + "-" + threadId.getAndIncrement() + if (name.isNullOrBlank()) {
                ""
            } else {
                "-$name"
            }
        }

    }

    constructor(runnable: Runnable, className: String) : this(runnable, null, className)

    constructor(name: String, className: String) : this(null, name, className)

}
```

之后，在 Transform 阶段只要遍历到指向 `java/lang/Thread ` 的 TypeInsnNode，且不是包含在 ThreadFactory 内部的话，那就是要处理的匿名线程了

```kotlin
/**
 * @Author: leavesCZY
 * @Desc:
 * @公众号：字节数组
 */
class OptimizedThreadTransform(private val config: OptimizedThreadConfig) : BaseTransform() {

    companion object {

        private const val threadClass = "java/lang/Thread"

        private const val threadFactoryClass = "java/util/concurrent/ThreadFactory"

        private const val threadFactoryNewThreadMethodDesc =
            "newThread(Ljava/lang/Runnable;)Ljava/lang/Thread;"

    }

    override fun modifyClass(byteArray: ByteArray): ByteArray {
        val classNode = ClassNode()
        val classReader = ClassReader(byteArray)
        classReader.accept(classNode, ClassReader.EXPAND_FRAMES)
        val methods = classNode.methods
        val taskList = mutableListOf<() -> Unit>()
        if (!methods.isNullOrEmpty()) {
            for (methodNode in methods) {
                val instructionIterator = methodNode.instructions?.iterator()
                if (instructionIterator != null) {
                    while (instructionIterator.hasNext()) {
                        val instruction = instructionIterator.next()
                        when (instruction.opcode) {
                            Opcodes.NEW -> {
                                val typeInsnNode = instruction as? TypeInsnNode
                                if (typeInsnNode?.desc == threadClass) {
                                    //如果是在 ThreadFactory 内初始化线程，则不处理
                                    if (!classNode.isThreadFactoryMethod(methodNode)) {
                                        taskList.add {
                                            transformNew(
                                                classNode,
                                                methodNode,
                                                instruction
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        taskList.forEach {
            it.invoke()
        }
        val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
        classNode.accept(classWriter)
        return classWriter.toByteArray()
    }

    private fun ClassNode.isThreadFactoryMethod(methodNode: MethodNode): Boolean {
        return this.interfaces?.contains(threadFactoryClass) == true
                && methodNode.nameWithDesc == threadFactoryNewThreadMethodDesc
    }

}
```

找到到目标指令后，就将 `java/lang/Thread ` 替换为 OptimizedThread，然后继续向后遍历找到调用 Thread 构造参数的指令，为该指令多插入一个 String 类型的方法入参参数声明，然后将 className 作为构造参数传给 OptimizedThread，至此就完成替换了

```kotlin
private fun transformNew(
    classNode: ClassNode,
    methodNode: MethodNode,
    typeInsnNode: TypeInsnNode
) {
    val instructions = methodNode.instructions
    val typeInsnNodeIndex = instructions.indexOf(typeInsnNode)
    //从 typeInsnNode 指令开始遍历，找到调用 Thread 构造函数的指令，然后对其进行替换
    for (index in typeInsnNodeIndex + 1 until instructions.size()) {
        val node = instructions[index]
        if (node is MethodInsnNode && node.isThreadInitMethodInsn()) {
            //将 Thread 替换为 OptimizedThread
            typeInsnNode.desc = config.formatOptimizedThreadClass
            node.owner = config.formatOptimizedThreadClass
            //为调用 Thread 构造函数的指令多插入一个 String 类型的方法入参参数声明
            node.insertArgument(String::class.java)
            //将 ClassName 作为构造参数传给 OptimizedThread
            instructions.insertBefore(node, LdcInsnNode(classNode.simpleClassName))
            break
        }
    }
}
```

# 二、线程池

`java.util.concurrent.Executors` 类中用于获取线程池的方法有十二个之多

```java
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

这十二个静态方法按照入参参数来进行区分的话有三种：

- 不包含入参参数
- 包含一个入参参数：代表线程数的 int 类型或者是 ThreadFactory
- 包含两个入参参数：代表线程数的 int 类型加 ThreadFactory

按照返回值来进行区分的话有两种：

- ExecutorService
- ScheduledExecutorService

在进行 hook 的时候就需要考虑对应代码传入了多少个入参参数以及方法的返回值类型了。为了统一线程池中的线程命名规则，就需要替换传入的 ThreadFactory，并且传入代表当前类名的 String 参数。为了能够为线程设置统一的超时时间，以及允许回收核心线程，就需要能够获取到 ThreadPoolExecutor 对象了

JDK 中的源码我们是没法进行 hook 的，而如果要在字节码层面上来进行以上修改的话会比较麻烦，所以我采取的是和处理匿名线程时类似的思路：声明一个 OptimizedExecutors 类，先在当中声明和 Executors 中各类 `newXXXThreadPool` 签名信息一致的方法，然后再为每个方法多添加一个 String 类型的入参参数，将指向 Executors 的字节码均改为 OptimizedExecutors，这样我们就可以 OptimizedExecutors 中自由配置线程池参数了

来看下实际的编码实现

首先是定义拿来进行替换的 OptimizedExecutors，为每个生成的 ThreadPoolExecutor 统一设置五秒的超时时间并允许回收核心线程，在 NamedThreadFactory 中定义统一的线程命名规则

```kotlin
/**
 * @Author: leavesCZY
 * @Desc:
 * @公众号：字节数组
 */
object OptimizedExecutors {

    private const val defaultThreadKeepAliveTime = 5000L

    @JvmStatic
    fun newFixedThreadPool(nThreads: Int, className: String): ExecutorService {
        return newFixedThreadPool(nThreads, null, className)
    }

    @JvmStatic
    fun newFixedThreadPool(
        nThreads: Int,
        threadFactory: ThreadFactory?,
        className: String
    ): ExecutorService {
        return getOptimizedExecutorService(
            nThreads, nThreads,
            0L, TimeUnit.MILLISECONDS,
            LinkedBlockingQueue(),
            threadFactory, className
        )
    }
    
	···

    private fun getOptimizedExecutorService(
        corePoolSize: Int,
        maximumPoolSize: Int,
        keepAliveTime: Long,
        unit: TimeUnit,
        workQueue: BlockingQueue<Runnable>,
        threadFactory: ThreadFactory? = null,
        className: String,
    ): ExecutorService {
        val executor = ThreadPoolExecutor(
            corePoolSize, maximumPoolSize,
            keepAliveTime, unit,
            workQueue,
            NamedThreadFactory(threadFactory, className)
        )
        executor.setKeepAliveTime(defaultThreadKeepAliveTime, TimeUnit.MILLISECONDS)
        executor.allowCoreThreadTimeOut(true)
        return executor
    }

    private class NamedThreadFactory(
        private val threadFactory: ThreadFactory?,
        private val className: String
    ) : ThreadFactory {

        private val threadId = AtomicInteger(0)

        override fun newThread(runnable: Runnable): Thread {
            val originThread = threadFactory?.newThread(runnable)
            val threadName =
                className + "-" + threadId.getAndIncrement() + if (originThread != null) {
                    "-" + originThread.name
                } else {
                    ""
                }
            val thread = originThread ?: Thread(runnable)
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

之后，在 Transform 阶段只要遍历到调用了 Executors 中静态方法的指令的话，就将该指令中的 Executors 替换为 OptimizedExecutors，然后添加多插入的参数

```kotlin
/**
 * @Author: leavesCZY
 * @Desc:
 * @公众号：字节数组
 */
class OptimizedThreadTransform(private val config: OptimizedThreadConfig) : BaseTransform() {

    companion object {

        private const val executorsClass = "java/util/concurrent/Executors"

    }

    override fun modifyClass(byteArray: ByteArray): ByteArray {
        val classNode = ClassNode()
        val classReader = ClassReader(byteArray)
        classReader.accept(classNode, ClassReader.EXPAND_FRAMES)
        val methods = classNode.methods
        val taskList = mutableListOf<() -> Unit>()
        if (!methods.isNullOrEmpty()) {
            for (methodNode in methods) {
                val instructionIterator = methodNode.instructions?.iterator()
                if (instructionIterator != null) {
                    while (instructionIterator.hasNext()) {
                        val instruction = instructionIterator.next()
                        when (instruction.opcode) {
                            Opcodes.INVOKESTATIC -> {
                                val methodInsnNode = instruction as? MethodInsnNode
                                if (methodInsnNode?.owner == executorsClass) {
                                    taskList.add {
                                        transformInvokeStatic(
                                            classNode,
                                            methodNode,
                                            instruction
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        taskList.forEach {
            it.invoke()
        }
        val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
        classNode.accept(classWriter)
        return classWriter.toByteArray()
    }

    private fun transformInvokeStatic(
        classNode: ClassNode,
        methodNode: MethodNode,
        methodInsnNode: MethodInsnNode
    ) {
        val pointMethod = config.threadHookPointList.find { it.methodName == methodInsnNode.name }
        if (pointMethod != null) {
            //将 Executors 替换为 OptimizedExecutors
            methodInsnNode.owner = config.formatOptimizedThreadPoolClass
            //为调用 newFixedThreadPool 等方法的指令多插入一个 String 类型的方法入参参数声明
            methodInsnNode.insertArgument(String::class.java)
            //将 ClassName 作为入参参数传给 newFixedThreadPool 等方法
            methodNode.instructions.insertBefore(
                methodInsnNode,
                LdcInsnNode(classNode.simpleClassName)
            )
        }
    }

}
```

# 三、需慎重

需要注意，上述的手段并不全是可以在项目中进行全盘套用的

为线程设置一个统一和容易标识的线程名有利于出现异常时能够快速定位问题，这是比较有意义的。但为线程池设置统一的超长时间和允许回收核心线程，这是需要考虑使用场景的，设置不当反而可能会加大 OOM 的概率

如果大部分情况下线程池需要处理的任务比较少且又不允许回收核心线程的话，就会导致核心线程大部分时间处于阻塞状态得不到释放，浪费系统资源，此时通过 hook 来允许回收核心线程是比较有意义的。而如果线程池需要处理的任务是属于定时执行的话，例如固定每六秒就会有一个新任务到来，此时通过 hook 设置了线程超时时间为五秒，那就有可能导致线程刚被回收不久又需要重新创建了，这种反复的创建和回收反而加大了系统开销，反而不如直接允许核心线程常驻。此外，线程从被创建到被系统选中调度执行是有一定耗时的，这不适合于某些对响应速度要求比较高的场景，此时允许回收核心线程也是不太合适的

所以说，在针对线程池进行整治的时候，需要同时考虑该线程池是应用于什么业务场景，根据用途来进行特殊配置

关于线程池的实现细节，可以看我写的一篇 ThreadPoolExecutor 解读文章，对设计初衷和实现始末进行了详细介绍：[聊聊 Java 多线程（5）-超详细的 ThreadPoolExecutor 源码解析](https://juejin.cn/post/6901317365561032712)

# 四、源码

最后也给出完整的源码：[ASM_Transform](https://github.com/leavesCZY/ASM_Transform)