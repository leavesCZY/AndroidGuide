> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

> 目前，多线程编程可以说是在大部分平台和应用上都需要实现的一个基本需求。本系列文章就来对 **Java 平台下的多线程编程知识**进行讲解，从**概念入门**、**底层实现**到**上层应用**都会涉及到，预计一共会有五篇文章，希望对你有所帮助 😎😎
>
> 本篇文章是第五篇，应该也是最后一篇了，从现实需求出发到源码介绍，一步步理清楚线程池的作用和优势

线程池（ThreadPool）面对的是外部复杂多变的多线程环境，既需要保证多线程环境下的状态同步，也需要最大化对每个线程的利用率，还需要留给子类足够多的余地来实现功能扩展。所以说，线程池的难点在于如何实现，而在概念上其实还是挺简单的。在 Java 中，线程池这个概念一般都认为对应的是 JDK 中的 ThreadPoolExecutor 类及其各种衍生类，本篇文章就从实现思路出发，探索 ThreadPoolExecutor 的源码到底是如何实现的以及为什么这么实现

# 一、线程池

线程是一种昂贵的系统资源，其“昂贵”不仅在于创建线程所需要的资源开销，还在于使用过程中带来的资源消耗。一个系统能够支持同时运行的线程总数受限于该系统所拥有的处理器数目和内存大小等硬件条件，线程的运行需要占用处理器时间片，系统中处于运行状态的线程越多，每个线程单位时间内能分配到的时间片就会越少，线程调度带来的上下文切换的次数就会越多，最终导致处理器真正用于运算的时间就会越少。此外，在现实场景中一个程序在其整个生命周期内需要交由线程执行的任务数量往往是远多于系统所能支持同时运行的最大线程数。基于以上原因，为每个任务都创建一个线程来负责执行是不太现实的。那么，我们最直接的一个想法就是要考虑怎么来实现线程的复用了

**线程池**就是实现线程复用的一种有效方式。线程池的思想可以看做是对**资源是有限的而需要处理的任务几乎是无限的**这样一个现状的应对措施。线程池的一般实现思路是：线程池内部预先创建或者是先后创建一定数量的线程，外部将需要执行的任务作为一个对象提交给线程池，由线程池选择某条空闲线程来负责执行。如果所有线程都处于工作状态且线程总数已经达到限制条件了，则先将任务缓存到任务队列中，线程再不断从任务队列中取出任务并执行。因此，线程池可以看做是基于**生产者-消费者模式**的一种服务，内部维护的多个线程相当于消费者，提交的任务相当于产品，提交任务的外部就相当于生产者

# 二、思考下

好了，既然已经对线程池这个概念有了基本的了解，那么就再来思考下线程池应该具备的功能以及应该如何来实现线程池

1. 线程池中的线程最大数量应该如何限定？

既然我们不可能无限制地创建线程，那么在创建线程池前就需要为其设定一个最大数量，我们称之为**最大线程池大小（maximumPoolSize）**，当线程池中的当前线程总数达到 maximumPoolSize 后就不应该再创建线程了。在开发中，我们需要根据运行设备的硬件条件和任务类型（I/O 密集型或者 CPU 密集型）来实际衡量该数值的大小，但任务的提交频率和任务的所需执行时间是不固定的，所以线程池的 maximumPoolSize 也应该支持动态调整

2. 线程池中的线程应该在什么时候被创建呢？

一般来说，如果线程池中的线程数量还没有达到 maximumPoolSize 时，我们可以等到当外部提交了任务时再来创建线程进行处理。但是，线程从被创建到被调度器选中运行，之间也是有着一定时间间隔的。从提高任务的处理响应速度这方面考虑，我们也可以选择预先就创建一批线程进行等待

3. 线程池中的线程可以一直存活着吗？

程序运行过程中可能只是偶发性地大批量提交任务，而大部分时间只是比较零散地提交少量任务，这就导致线程池中的线程可能会在一段时间内处于空闲状态。如果线程池中的线程只要创建了就可以一直存活着的话，那么线程池的“性价比”就显得没那么高了。所以，当线程处于空闲状态的时间超出允许的最大空闲时间（keepAliveTime）后，我们就应该将其回收，避免白白浪费系统资源。而又为了避免频繁地创建和销毁线程，线程池需要缓存一定数量的线程，即使其处于空闲状态也不会进行回收，这类线程我们就称之为**核心线程**，相应的线程数量就称之为**核心线程池大小（corePoolSize）**。大于 corePoolSize 而小于等于 maximumPoolSize 的那一部分线程，就称之为**非核心线程**

4. 如何实现线程的复用？

我们知道，当 `Thread.run()` 方法执行结束后线程就会被回收了，那么想要实现线程的复用，那么就要考虑如何避免退出 `Thread.run()` 了。这里，我们可以通过**循环向任务队列取值**的方式来实现。上面有提到，如果外部提交的任务过多，那么任务就需要被缓存到任务队列中。那么，我们就可以考虑使用一个阻塞队列来存放任务。线程循环从任务队列中取任务，如果队列不为空，那么就可以马上拿到任务进行执行；如果队列为空，那么就让线程一直阻塞等待，直到外部提交了任务被该线程拿到或者由于超时退出循环。通过这种**循环获取+阻塞等待**的方式，就可以实现线程复用的目的

5. 如何尽量实现线程的复用？

这个问题和“如何实现线程的复用”不太一样，“如何实现线程的复用”针对的是单个线程的复用流程，本问题针对的是整个线程池范围的复用。线程池中需要使用到任务队列进行缓存，那么任务队列的使用时机可以有以下几种：

- 当线程数已经达到 maximumPoolSize ，且所有线程均处于工作状态时，此后外部提交的任务才被缓存到任务队列中
- 当核心线程都已经被创建了时，此后外部提交的任务就被缓存到任务队列中，当任务队列满了后才创建非核心线程来循环处理任务

很明显的，第二种方案会更加优秀。由于核心线程一般情况下是会被长久保留的，核心线程的存在保证了外部提交的任务一直有在被循环处理。如果外部提交的大部分都是耗时较短的任务或者任务的提交频率比较低的话，那么任务队列就可能没那么容易满，第二种方案就可以尽量避免去创建非核心线程。而且对于“偶发性地大批量提交任务，而大部分时间只是比较零散地提交少量任务”这种情况，第二种方案也会更加合适。当然，在任务的处理速度方面，第一种方案就会高一些，但是如果想要尽量提高第二种方案的任务处理速度的话，也可以通过**将任务队列的容量调小**的方式来实现

6. 当任务队列满了后该如何处理？

如果线程池实在“忙不过来”的话，那么任务队列也是有可能满的，那么就需要为这种情况指定处理策略。当然，我们也可以选择使用一个**无界队列**来缓存任务，但是无界队列容易掩盖掉一些程序异常。因为有界队列之所以会满，可能是由于发生线程池死锁或者依赖的某个基础服务失效导致的，从而令线程池中的任务一直迟迟得不到解决。如果使用的是无界队列的话，就可能使得当系统发生异常时程序还是看起来运转正常，从而降低了系统健壮性。所以，最常用的还是有界队列

现实需求是多样化的，在实现线程池时就需要留有交由外部自定义处理策略的余地。例如，当队列满了后，我们可以选择直接抛出异常来向外部“告知”这一异常情况。对于重要程度较低的任务，可以选择直接抛弃该任务，也可以选择抛弃队列头的任务而尝试接纳新到来的任务。如果任务必须被执行的话，也可以直接就在提交任务的线程上进行执行

以上就是线程池在实现过程中需要主要考虑的几个点，下面就来看下 Java 实际上是怎么实现线程池的

# 三、ThreadPoolExecutor

`java.util.concurrent.ThreadPoolExecutor` 类就是 Java 对线程池的默认实现，下文如果没有特别说明的话，所说的线程池就是指 `ThreadPoolExecutor`

ThreadPoolExecutor 的继承关系如下图所示

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/6b73cde5111446a8bd4e41af1b028e61~tplv-k3u1fbpfcp-zoom-1.image)

ThreadPoolExecutor 实现的最顶层接口是 Executor。Executor 提供了一种将**任务的提交**和**任务的执行**两个操作进行解耦的思路：客户端无需关注执行任务的线程是如何创建、运行和回收的，只需要将任务的执行逻辑包装为一个 Runnable 对象传递进来即可，由 Executor 的实现类自己来完成最复杂的执行逻辑

ExecutorService 接口在 Executor 的基础上扩展了一些功能：

1. 扩展执行任务的能力。例如：获取任务的执行结果、取消任务等功能
2. 提供了关闭线程池、停止线程池，以及阻塞等待线程池完全终止的方法

AbstractExecutorService 则是上层的抽象类，负责将任务的执行流程串联起来，从而使得下层的实现类 ThreadPoolExecutor 只需要实现一个执行任务的方法即可

也正如上文所说的那样，ThreadPoolExecutor 可以看做是基于**生产者-消费者模式**的一种服务，内部维护的多个线程相当于消费者，提交的任务相当于产品，提交任务的外部就相当于生产者。其整个运行流程如下图所示

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/31321bc8231648f2a455106a31e94884~tplv-k3u1fbpfcp-zoom-1.image)

而在线程池的整个生命周期中，以下三个关于线程数量的统计结果也影响着线程池的流程走向

1. 当前线程池大小（currentPoolSize）。表示当前实时状态下线程池中线程的数量
2. 最大线程池大小（maximumPoolSize）。表示线程池中允许存在的线程的数量上限
3. 核心线程池大小（corePoolSize）。表示一个不大于 maximumPoolSize 的线程数量上限

三者之间的关系如下：

```java
当前线程池大小  ≤  核心线程池大小  ≤  最大线程池大小
or
核心线程池大小  ≤  当前线程池大小  ≤  最大线程池大小
```

当中，除了“当前线程池大小”是对线程池现有的工作者线程进行实时计数的结果，其它两个值都是对线程池配置的参数值。三个值的作用在上文也都已经介绍了

ThreadPoolExecutor 中参数最多的一个构造函数的声明如下所示：

```java
public ThreadPoolExecutor(int corePoolSize,
                          int maximumPoolSize,
                          long keepAliveTime,
                          TimeUnit unit,
                          BlockingQueue<Runnable> workQueue,
                          ThreadFactory threadFactory,
                          RejectedExecutionHandler handler) {
    if (corePoolSize < 0 ||
        maximumPoolSize <= 0 ||
        maximumPoolSize < corePoolSize ||
        keepAliveTime < 0)
        throw new IllegalArgumentException();
    if (workQueue == null || threadFactory == null || handler == null)
        throw new NullPointerException();
    this.acc = System.getSecurityManager() == null ?
            null :
            AccessController.getContext();
    this.corePoolSize = corePoolSize;
    this.maximumPoolSize = maximumPoolSize;
    this.workQueue = workQueue;
    this.keepAliveTime = unit.toNanos(keepAliveTime);
    this.threadFactory = threadFactory;
    this.handler = handler;
}
```

- corePoolSize ：用于指定线程池的核心线程数大小
- maximumPoolSize：用于指定最大线程池大小
- keepAliveTime、unit   ：一起用于指定线程池中空闲线程的最大存活时间
- workQueue ：任务队列，相当于生产者-消费者模式中的传输管道，用于存放待处理的任务
- threadFactory：用于指定创建线程的线程工厂
- handler：用于指定当任务队列已满且线程数量达到 maximumPoolSize 时任务的处理策略

**下面就以点见面，从细节处来把握整个线程池的流程走向**

## 1、线程池的状态如何保存

这里所说的“状态”指的是一个**复合数据**，包含“**线程池生命周期状态**”和“**线程池当前线程数量**”这两项。线程池从启动到最终终止，其内部需要记录其当前状态来决定流程走向。而线程池的当前线程数量，也关乎着线程是否需要进行回收以及是否需要执行任务的拒绝策略

线程池一共包含以下五种生命周期状态，涵盖了线程池从启动到终止的这整个范围。线程池的生命周期状态可以按顺序跃迁，但无法反向回转，每个状态的数值大小也是逐步递增的

```java
//运行状态，线程池正处于运行中
private static final int RUNNING    = -1 << COUNT_BITS;
//关闭状态，当调用 shutdown() 方法后处于这个状态，任务队列中的任务会继续处理，但不再接受新任务，
private static final int SHUTDOWN   =  0 << COUNT_BITS;
//停止状态，当调用 shutdownNow() 方法后处于这个状态
//任务队列中的任务也不再处理且作为方法返回值返回，此后不再接受新任务
private static final int STOP       =  1 << COUNT_BITS;
//TERMINATED 之前的临时状态，当线程都被回收且任务队列已清空后就会处于这个状态
private static final int TIDYING    =  2 << COUNT_BITS;
//终止状态，在处于 TIDYING 状态后会立即调用 terminated() 方法，调用完成就会马上转到此状态
private static final int TERMINATED =  3 << COUNT_BITS;
```

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/42fd1e9319824fdca526dbcdaeb4fe90~tplv-k3u1fbpfcp-zoom-1.image)

在日常开发中，如果我们想要用一个 int 类型的 state 变量来表示这五种状态的话，那么就可能是通过让 state 分别取值 **1，2，3，4，5** 来进行标识，而 state 作为一个 int 类型是一共有三十二位的，那其实上仅需要占用三位就足够标识了，即 2 x 2 x 2 = 8 种可能。那还剩下 29 位可以用来存放其它数据

实际上 ThreadPoolExecutor 就是通过将一个 32 位的 int 类型变量分割为两段，**高 3 位用来表示线程池的当前生命周期状态，低 29 位就拿来表示线程池的当前线程数量**，从而做到用一个变量值来维护两份数据，这个变量值就是 `ctl`。从 `ctl` 的初始值就可以知道线程池的初始生命周期状态( runState )是 `RUNNING`，初始线程数量 ( workerCount )是 0。这种用一个变量去存储两个值的做法，可以避免在做相关决策时出现不一致的情况，且不必为了维护两者的一致而使用锁，后续需要获取线程池的**当前生命周期状态**和**线程数量**的时候，也可以直接采用位运算的方式获取，在速度上相比基本运算会快很多

```java
private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
```

ThreadPoolExecutor 还声明了以下两个常量用来参与位运算

```java
//用来表示线程数量的位数，即 29
private static final int COUNT_BITS = Integer.SIZE - 3;
//线程池所能表达的最大线程数，即一个“高3位全是0，低29位全是1”的数值
private static final int CAPACITY   = (1 << COUNT_BITS) - 1;
```

相应的，那么就需要有几个方法可以来分别取“生命周期状态”和“线程数”这两个值，以及将这两个值合并保存的方法，这几个方法都使用到了位运算

```java
// Packing and unpacking ctl

//通过按位取反 + 按位与运算，将 c 的高3位保留，舍弃低29位，从而得到线程池状态
private static int runStateOf(int c)     { return c & ~CAPACITY; }

//通过按位与运算，将 c 的高3位舍弃，保留低29位，从而得到工作线程数
private static int workerCountOf(int c)  { return c & CAPACITY; }

//rs，即 runState，线程池的生命周期状态
//wc，即 workerCount，工作线程数量
//通过按位或运算来合并值
private static int ctlOf(int rs, int wc) { return rs | wc; }

private static boolean runStateLessThan(int c, int s) {
    return c < s;
}

private static boolean runStateAtLeast(int c, int s) {
    return c >= s;
}

//用于判断线程池是否处于 RUNNING 状态
//由于五个状态值的大小是依次递增的，所以只需要和 SHUTDOWN 比较即可
private static boolean isRunning(int c) {
    return c < SHUTDOWN;
}

public boolean isShutdown() {
    return !isRunning(ctl.get());
}

//用于判断当前状态是否处于 SHUTDOWN、STOP、TIDYING 三者中的一个
public boolean isTerminating() {
    int c = ctl.get();
    return !isRunning(c) && runStateLessThan(c, TERMINATED);
}

//用于判断当前状态是否处于 TERMINATED
public boolean isTerminated() {
    return runStateAtLeast(ctl.get(), TERMINATED);
}
```

## 2、线程的创建流程

在初始状态下，客户端每提交一个任务，线程池就会通过 `threadFactory` 创建线程来处理该任务，如果开发者没有指定 `threadFactory` 的话，则会使用 `Executors.DefaultThreadFactory`。线程池在最先开始创建的线程属于核心线程，线程数量在大于 corePoolSize 而小于等于 maximumPoolSize 这个范围内的线程属于非核心线程。需要注意的是，核心线程和非核心线程并非是两种不同类型的线程对象，这两个概念只是对不同数量范围内的线程进行的区分，实质上这两者指向的都是同一类型

线程的创建流程可以通过任务的提交流程来了解，任务的提交流程图如下所示

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/5743941998c94452872373a79b68c90a~tplv-k3u1fbpfcp-zoom-1.image)

线程池开放了多个让外部提交任务的方法，这里主要看 `execute(Runnable command)` 方法。该方法需要先后多次校验状态值，因为线程池面对的调用方可以来自于多个不同的线程。可能在当前线程提交任务的同时，其它线程就刚好关闭了线程池或者是调整了线程池的线程大小参数，需要考虑当前的线程数量是否已经达到限制了

```java
public void execute(Runnable command) {
    if (command == null)
        throw new NullPointerException();
    int c = ctl.get();
    if (workerCountOf(c) < corePoolSize) {
        //如果当前线程数还未达到 corePoolSize，则尝试创建一个核心线程来处理任务
        //addWorker 可能会因为线程池被关闭了、线程数量超出限制等原因返回 false
        if (addWorker(command, true))
            return;
        c = ctl.get();
    }

    if (isRunning(c) && workQueue.offer(command)) { //线程池还处于运行状态且成功添加任务到任务队列
        //需要重新检查下运行状态
        //因为等执行到这里时，线程池可能被其它线程关闭了
        int recheck = ctl.get();

        //1、如果线程池已经处于非运行状态了
        //1.1、如果移除 command 成功，则走拒绝策略
        //1.2、如果移除 command 失败（因为 command 可能已经被其它线程拿去执行了），则走第 3 步
        //2、如果线程池还处于运行状态，则走第 3 步
        //3、如果当前线程数量为 0，则创建线程进行处理
        //第 3 步的意义在于：corePoolSize 可以被设为 0，所以这里需要检查下，在需要的时候创建一个非核心线程
        if (! isRunning(recheck) && remove(command))
            reject(command);
        else if (workerCountOf(recheck) == 0)
            addWorker(null, false);
    }
    //如果线程池处于非运行状态了，或者是处于运行状态但队列已满了，此时就会走到这里
    //在这里尝试创建一个非核心线程
    //如果线程创建失败，说明要么是线程池当前状态大于等于 STOP，或者是任务队列已满且线程总数达到 maximumPoolSize 了
    //此时就走拒绝策略
    else if (!addWorker(command, false))
        reject(command);
}
```

当中，`addWorker(Runnable firstTask, boolean core)` 方法用于尝试创建并启动线程，同时将线程保存到 `workers`。第一个参数用于指定线程启动时需要执行的第一个任务，可以为 null。第二个参数用于指定要创建的是否是核心线程，这个参数会关系到线程是否能被成功创建

该方法在实际创建线程前，都需要先通过 CAS 来更新（递增加一）当前的线程总数，通过 for 循环来不断进行重试。当 CAS 成功后，则会再来实际进行线程的创建操作。但在这时候线程也未必能够创建成功，因为在 CAS 成功后线程池可能被关闭了，或者是在创建线程时抛出异常了，此时就需要回滚对 workerCount 的修改

该方法如果返回 true，意味着新创建了一个 Worker 线程，同时线程也被启动了

该方法如果返回 false，则可能是由于以下情况：

1. 生命周期状态大于等于 STOP
2. 生命周期状态等于 SHUTDOWN，但 firstTask 不为 null，或者任务队列为空
3. 当前线程数已经超出限制
4. 符合创建线程的条件，但创建过程中或启动线程的过程中抛出了异常

```java
private boolean addWorker(Runnable firstTask, boolean core) {
    //下面的 for 主要逻辑：
    //在创建线程前通过 CAS 原子性地将“工作者线程数量递增加一”
    //由于 CAS 可能会失败，所以将之放到 for 循环中进行循环重试
    //每次循环前后都需要检查下当前状态是否允许创建线程
    retry:
    for (;;) {
        int c = ctl.get();
        int rs = runStateOf(c);

        // Check if queue empty only if necessary.
        if (rs >= SHUTDOWN &&
            ! (rs == SHUTDOWN &&
               firstTask == null &&
               ! workQueue.isEmpty()))   
            //当外部调用 shutdown() 方法后，线程池状态会变迁为 SHUTDOWN
            //此时依然允许创建线程来对队列中的任务进行处理，但是不会再接受新任务
            //除这种情况之外不允许在非 RUNNING 的时候还创建线程
            return false;

        for (;;) {  
            int wc = workerCountOf(c);
            if (wc >= CAPACITY ||
                wc >= (core ? corePoolSize : maximumPoolSize))
                //当前线程数已经超出最大限制
                return false;
            if (compareAndIncrementWorkerCount(c))
                //通过 CAS 更新工作者线程数成功后就跳出循环，去实际创建线程
                break retry;
            c = ctl.get();  // Re-read ctl
            if (runStateOf(c) != rs)
                //循环过程中线程池状态被改变了，重新循环
                continue retry;
            // else CAS failed due to workerCount change; retry inner loop
        }
    }

    boolean workerStarted = false;
    boolean workerAdded = false;
    Worker w = null;
    try {
        w = new Worker(firstTask);
        final Thread t = w.thread;
        if (t != null) {
            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                // Recheck while holding lock.
                // Back out on ThreadFactory failure or if
                // shut down before lock acquired.
                int rs = runStateOf(ctl.get());

                if (rs < SHUTDOWN ||
                    (rs == SHUTDOWN && firstTask == null)) {
                    if (t.isAlive()) // precheck that t is startable
                        throw new IllegalThreadStateException();
                    workers.add(w);
                    //更新线程池曾经达到的最大线程数
                    int s = workers.size();
                    if (s > largestPoolSize)
                        largestPoolSize = s;
                    workerAdded = true;
                }
            } finally {
                mainLock.unlock();
            }
            if (workerAdded) {
                t.start();
                workerStarted = true;
            }
        }
    } finally {
        //如果线程没有被成功启动，则需要将该任务从队列中移除并重新更新工作者线程数
        if (! workerStarted)
            addWorkerFailed(w);
    }
    return workerStarted;
}
```

## 3、线程的执行流程

上面所讲的线程其实指的是 ThreadPoolExecutor 的内部类 Worker ，Worker 内部包含了一个 Thread 对象，所以本文就把 Worker 实例也看做线程来对待

Worker 继承于 AbstractQueuedSynchronizer，意味着 Worker 就相当于一个锁。之没有使用 synchronized 或者 ReentrantLock，是因为它们都是**可重入锁**，Worker 继承于 AQS 为的就是自定义实现**不可重入**的特性来辅助判断线程是否处于执行任务的状态：在开始执行任务前进行加锁，在任务执行结束后解锁，以便在后续通过判断 Worker 是否处于锁定状态来得知其是否处于执行阶段

1. Worker 在开始执行任务前会执行 `Worker.lock()` ，表明线程正在执行任务
2. 如果 Worker 处于锁定状态，则不应该对其进行中断，避免任务执行一半就被打断
3. 如果 Worker 处于非锁定状态，说明其当前是处于阻塞获取任务的状态，此时才允许对其进行中断
4. 线程池在执行 `shutdown()` 方法或 `shutdownNow()` 方法时会调用 `interruptIdleWorkers()` 方法来回收空闲的线程，`interruptIdleWorkers()` 方法会使用`Worker.tryLock()` 方法来尝试获取锁，由于 Worker 是不可重入锁，所以如果锁获取成功就说明线程处于空闲状态，此时才可以进行回收

Worker 同时也是 Runnable 类型，`thread` 是通过 `getThreadFactory().newThread(this)` 来创建的，即将 Worker 本身作为构造参数传给 Thread 进行初始化，所以在 `thread` 启动的时候 Worker 的 `run()` 方法就会被执行

```java
private final class Worker extends AbstractQueuedSynchronizer implements Runnable  {
    /**
     * This class will never be serialized, but we provide a
     * serialVersionUID to suppress a javac warning.
     */
    private static final long serialVersionUID = 6138294804551838833L;

    /** Thread this worker is running in.  Null if factory fails. */
    final Thread thread;

    //线程要执行的第一个任务，可能为 null
    /** Initial task to run.  Possibly null. */
    Runnable firstTask;

    //用于标记 Worker 执行过的任务数（不管成功与否都记录）
    /** Per-thread task counter */
    volatile long completedTasks;

    /**
     * Creates with given first task and thread from ThreadFactory.
     * @param firstTask the first task (null if none)
     */
    Worker(Runnable firstTask) {
        setState(-1); // inhibit interrupts until runWorker
        this.firstTask = firstTask;
        this.thread = getThreadFactory().newThread(this);
    }

    /** Delegates main run loop to outer runWorker  */
    public void run() {
        runWorker(this);
    }

    // Lock methods
    //
    // The value 0 represents the unlocked state.
    // The value 1 represents the locked state.

    protected boolean isHeldExclusively() {
        return getState() != 0;
    }

    //只有在 state 值为 0 的时候才能获取到锁，以此实现不可重入的特性
    protected boolean tryAcquire(int unused) {
        if (compareAndSetState(0, 1)) {
            setExclusiveOwnerThread(Thread.currentThread());
            return true;
        }
        return false;
    }

    protected boolean tryRelease(int unused) {
        setExclusiveOwnerThread(null);
        setState(0);
        return true;
    }

    public void lock()        { acquire(1); }
    public boolean tryLock()  { return tryAcquire(1); }
    public void unlock()      { release(1); }
    public boolean isLocked() { return isHeldExclusively(); }

    //向线程发起中断请求
    void interruptIfStarted() {
        Thread t;
        if (getState() >= 0 && (t = thread) != null && !t.isInterrupted()) {
            try {
                t.interrupt();
            } catch (SecurityException ignore) {
            }
        }
    }
}
```

`runWorker(Worker)` 方法就是线程正式进行任务执行的地方。该方法通过 while 循环不断从任务队列中取出任务来进行执行，如果 `getTask()`方法返回了 null，那此时就需要将此线程进行回收。如果在任务执行过程中抛出了异常，那也需要回收此线程

```java
final void runWorker(Worker w) {
    Thread wt = Thread.currentThread();
    Runnable task = w.firstTask;
    w.firstTask = null;

    //因为 Worker 的默认值是 -1，而 Worker 的 interruptIfStarted() 方法只有在 state >=0 的时候才允许进行中断
    //所以这里调用 unlock() 并不是为了解锁，而是为了让 Worker 的 state 值变为 0，让 Worker 允许外部进行中断
    //所以，即使客户端调用了 shutdown 或者 shutdownNow 方法，在 Worker 线程还未执行到这里前，无法在 interruptWorkers() 方法里发起中断请求
    w.unlock(); // allow interrupts

    //用于标记是否由于被打断而非正常结束导致的线程终止
    //为 true 表示非正常结束
    boolean completedAbruptly = true;

    try {
        // 如果 getTask() 为 null，说明线程池已经被停止或者需要进行线程回收
        while (task != null || (task = getTask()) != null) {

            //在开始执行任务前进行加锁，在任务执行结束后解锁
            //以便在后续通过判断 Worker 是否处于锁定状态来得知其是否处于执行阶段
            w.lock();
            // If pool is stopping, ensure thread is interrupted;
            // if not, ensure thread is not interrupted.  This
            // requires a recheck in second case to deal with
            // shutdownNow race while clearing interrupt
            //确保当状态值大于等于 STOP 时有向线程发起过中断请求
            if ((runStateAtLeast(ctl.get(), STOP) 
                ||
                (Thread.interrupted() && runStateAtLeast(ctl.get(), STOP))) 
                &&
                !wt.isInterrupted())
                wt.interrupt();
            try {
                beforeExecute(wt, task);
                Throwable thrown = null;
                try {
                    task.run();
                } catch (RuntimeException x) {
                    thrown = x; throw x;
                } catch (Error x) {
                    thrown = x; throw x;
                } catch (Throwable x) {
                    thrown = x; throw new Error(x);
                } finally {
                    afterExecute(task, thrown);
                }
            } finally {
                task = null;
                w.completedTasks++;
                w.unlock();
            }
        }
        completedAbruptly = false;
    } finally {
        //回收此线程
        processWorkerExit(w, completedAbruptly);
    }
}

private Runnable getTask() {
    boolean timedOut = false; // Did the last poll() time out?

    for (; ; ) {
        int c = ctl.get();
        int rs = runStateOf(c);

        // Check if queue empty only if necessary.
        //如何当前状态大于等于 STOP，则返回 null
        //如何当前状态是 SHUTDOWN 且任务队列为空，则返回 null
        if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {
            decrementWorkerCount();
            return null;
        }

        int wc = workerCountOf(c);

        // Are workers subject to culling?
        //timed 用于标记从任务队列中取任务时是否需要进行超时控制
        //如果允许回收空闲核心线程或者是当前的线程总数已经超出 corePoolSize 了，那么就需要进行超时控制
        boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;

        //1. 线程总数超出 maximumPoolSize
        //2. 允许回收核心线程，且核心线程的空闲时间已达到限制了
        //如果以上两种情况之一有一个满足，且当前线程数大于 1 或者任务队列为空时就返回 null（如果 CAS 更新 WorkerCount 成功的话）
        //避免在任务队列不为空且只有一个线程时还回收线程导致任务没人处理
        if ((wc > maximumPoolSize || (timed && timedOut))
                && (wc > 1 || workQueue.isEmpty())) {
            if (compareAndDecrementWorkerCount(c))
                return null;
            continue;
        }

        try {
            Runnable r = timed ?
                    workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) :
                    workQueue.take();
            if (r != null)
                return r;
            //如果 r 为 null，说明是由于超时导致 poll 返回了 null
            //在下一次循环时将判断是否回收此线程
            timedOut = true;
        } catch (InterruptedException retry) {
            timedOut = false;
        }
    }
}
```

`getTask()` 方法获取任务的流程图如下所示：

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/27aa45cbba2848de85cb20fdccd5ae42~tplv-k3u1fbpfcp-zoom-1.image)

## 4、线程的回收流程

当外部调用了线程池的以下几个方法之一时，就会触发到线程的回收机制：

1. 允许回收核心线程：allowCoreThreadTimeOut()
2. 重置核心线程池大小：setCorePoolSize()
3. 重置最大线程池大小：setMaximumPoolSize()
4. 重置线程最大空闲时间：setKeepAliveTime()
5. 关闭线程池：shutdown()
6. 停止线程池：shutdownNow()

```java
/**
 * 用于控制核心线程是否可以由于空闲时间超时而被回收
 * 超时时间和非核心线程一样由 keepAliveTime 来指定
 *
 * @param value
 */
public void allowCoreThreadTimeOut(boolean value) {
    if (value && keepAliveTime <= 0)
        throw new IllegalArgumentException("Core threads must have nonzero keep alive times");
    if (value != allowCoreThreadTimeOut) {
        allowCoreThreadTimeOut = value;
        if (value)
            //回收掉空闲线程
            interruptIdleWorkers();
    }
}

/**
 * 重置 corePoolSize
 *
 * @param corePoolSize
 */
public void setCorePoolSize(int corePoolSize) {
    if (corePoolSize < 0)
        throw new IllegalArgumentException();
    int delta = corePoolSize - this.corePoolSize;
    this.corePoolSize = corePoolSize;
    if (workerCountOf(ctl.get()) > corePoolSize)
        //如果当前的线程总数已经超出新的 corePoolSize 的话那就进行线程回收
        interruptIdleWorkers();
    else if (delta > 0) {
        //会走进这里，说明新的 corePoolSize 比原先的大，但当前线程总数还小于等于新的 corePoolSize
        //此时如果任务队列不为空的话，那么就需要创建一批新的核心线程来处理任务
        //delta 和 workQueueSize 中的最小值就是需要启动的线程数
        //而如果在创建过程中任务队列已经空了（被其它线程拿去处理了），那就不再创建线程
        int k = Math.min(delta, workQueue.size());
        while (k-- > 0 && addWorker(null, true)) {
            if (workQueue.isEmpty())
                break;
        }
    }
}

/**
 * 用于设置线程池允许存在的最大活跃线程数
 *
 * @param maximumPoolSize
 */
public void setMaximumPoolSize(int maximumPoolSize) {
    if (maximumPoolSize <= 0 || maximumPoolSize < corePoolSize)
        throw new IllegalArgumentException();
    this.maximumPoolSize = maximumPoolSize;
    if (workerCountOf(ctl.get()) > maximumPoolSize)
        //回收掉空闲线程
        interruptIdleWorkers();
}

/**
 * 用于设置非核心线程在空闲状态能够存活的时间
 *
 * @param time
 * @param unit
 */
public void setKeepAliveTime(long time, TimeUnit unit) {
    if (time < 0)
        throw new IllegalArgumentException();
    //为了避免频繁创建线程，核心线程如果允许超时回收的话，超时时间不能为 0
    if (time == 0 && allowsCoreThreadTimeOut())
        throw new IllegalArgumentException("Core threads must have nonzero keep alive times");
    long keepAliveTime = unit.toNanos(time);
    long delta = keepAliveTime - this.keepAliveTime;
    this.keepAliveTime = keepAliveTime;
    if (delta < 0) //如果新设置的值比原先的超时时间小的话，那就需要去回收掉空闲线程
        interruptIdleWorkers();
}

 /**
 * 关闭线程池
 */
public void shutdown() {
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        checkShutdownAccess();
        //将当前状态设置为 SHUTDOWN
        advanceRunState(SHUTDOWN);
        //回收掉空闲线程
        interruptIdleWorkers();
        onShutdown(); // hook for ScheduledThreadPoolExecutor
    } finally {
        mainLock.unlock();
    }
    //尝试看是否能把线程池状态置为 TERMINATED
    tryTerminate();
}

/**
 * 停止线程池
 *
 * @return 任务队列中缓存的所有任务
 */
public List<Runnable> shutdownNow() {
    List<Runnable> tasks;
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        checkShutdownAccess();
        //将当前状态设置为 STOP
        advanceRunState(STOP);
        //回收掉空闲线程
        interruptWorkers();
        //获取任务队列中缓存的所有任务
        tasks = drainQueue();
    } finally {
        mainLock.unlock();
    }
    //尝试看是否能把线程池状态置为 TERMINATED
    tryTerminate();
    return tasks;
}
```

上述的几个方法最终都会调用 `interruptIdleWorkers(boolean onlyOne)` 方法来回收空闲线程。该方法通过向线程发起中断请求来使 Worker 退出 `runWorker(Worker w)` 方法，最终会调用 `processWorkerExit(Worker w, boolean completedAbruptly)` 方法来完成实际的线程回收操作

```java
private void interruptIdleWorkers() {
    interruptIdleWorkers(false);
}

private void interruptIdleWorkers(boolean onlyOne) {
    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        for (Worker w : workers) {
            Thread t = w.thread;
            //仅在线程的中断标记为 false 时才发起中断，避免重复发起中断请求
            //且仅在 w.tryLock() 能成功（即 Worker 并非处于执行任务的阶段）才发起中断，避免任务还未执行完就被打断
            if (!t.isInterrupted() && w.tryLock()) {
                try {
                    t.interrupt();
                } catch (SecurityException ignore) {
                } finally {
                    w.unlock();
                }
            }
            if (onlyOne)
                break;
        }
    } finally {
        mainLock.unlock();
    }
}

/**
 * 回收线程
 *
 * @param w                 Worker
 * @param completedAbruptly 是否是由于任务执行过程抛出异常导致需要来回收线程
 *                          true：由于任务抛出异常
 *                          false：由于线程空闲时间达到限制条件
 */
private void processWorkerExit(Worker w, boolean completedAbruptly) {
    if (completedAbruptly) // If abrupt, then workerCount wasn't adjusted
        decrementWorkerCount();

    final ReentrantLock mainLock = this.mainLock;
    mainLock.lock();
    try {
        //更新线程池总共处理过的任务数
        completedTaskCount += w.completedTasks;
        //移除此线程
        workers.remove(w);
    } finally {
        mainLock.unlock();
    }

    tryTerminate();

    int c = ctl.get();
    if (runStateLessThan(c, STOP)) {
        //在任务队列不为空的时候，需要确保至少有一个线程可以来处理任务，否则就还是需要再创建一个新线程
        if (!completedAbruptly) {
            int min = allowCoreThreadTimeOut ? 0 : corePoolSize;
            if (min == 0 && !workQueue.isEmpty())
                min = 1;
            if (workerCountOf(c) >= min)
                return; // replacement not needed
        }
        addWorker(null, false);
    }
}
```

除了上述几个方法会主动触发到线程回收机制外，当线程池满足以下几种情况之一时，也会进行线程的回收：

1. 非核心线程的空闲时间超出了 keepAliveTime
2. allowCoreThreadTimeOut 为 true 且核心线程的空闲时间超出了 keepAliveTime

以上几种情况其触发时机主要看 `getTask()` 方法就可以。在向任务队列 workQueue 获取任务前，通过判断当前线程池的 `allowCoreThreadTimeOut、corePoolSize、workerCount` 等参数来决定是否需要对“从任务队列获取任务”这个操作进行限时。如果需要进行限时且获取任务的时间超出 keepAliveTime 的话，那就说明此线程的空闲时间已经达到限制了，需要对其进行回收

```java
private Runnable getTask() {
    boolean timedOut = false; // Did the last poll() time out?

    for (; ; ) {
        int c = ctl.get();
        int rs = runStateOf(c);

        if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {
            decrementWorkerCount();
            return null;
        }

        int wc = workerCountOf(c);

        // Are workers subject to culling?
        //timed 用于标记从任务队列中取任务时是否需要进行超时控制
        //如果允许回收空闲核心线程或者是当前的线程总数已经超出 corePoolSize 了，那么就需要进行超时控制
        boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;

        //1. 线程总数超出 maximumPoolSize
        //2. 允许回收核心线程，且核心线程的空闲时间已达到限制了
        //如果以上两种情况之一有一个满足，且当前线程数大于 1 或者任务队列为空时就返回 null（如果 CAS 更新 WorkerCount 成功的话）
        //避免在任务队列不为空且只有一个线程时还回收线程导致任务没人处理
        if ((wc > maximumPoolSize || (timed && timedOut))
                && (wc > 1 || workQueue.isEmpty())) {
            if (compareAndDecrementWorkerCount(c))
                return null;
            continue;
        }

        try {
            Runnable r = timed ?
                    workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) :
                    workQueue.take();
            if (r != null)
                return r;
            timedOut = true;
            //如果能执行到 timedOut = true 说明是由于超时导致 poll 返回了 null
            //之所以不在判断到 r 不为 null 的时候就直接 return 出去
            //是因为可能在获取任务的过程中外部又重新修改了 allowCoreThreadTimeOut 和 corePoolSize 等配置
            //导致此时又不需要回收此线程了，所以就在下一次循环时再判断是否回收此线程
        } catch (InterruptedException retry) {
            timedOut = false;
        }
    }
}
```

以上就是线程池基本所有的线程回收流程。线程回收机制有助于节约系统资源，但如果 **corePoolSize、keepAliveTime** 等参数设置得和系统的实际运行情况不符的话，反而会导致线程频繁地被创建和回收，反而加大了资源开销

## 5、线程池的关闭流程

`shutdown()` 和 `shutdownNow()` 方法可以用来关闭和停止线程池

- shutdown()。使用该方法，已提交的任务会被继续执行，而后续新提交的任务则会走拒绝策略。该方法返回时，线程池可能尚未走向终止状态 TERMINATED，即线程池中可能还有线程还在执行任务
- shutdownNow()。使用该方法，正在运行的线程会尝试停止，任务队列中的任务也不会执行而是作为方法返回值返回。由于该方法是通过调用 `Thread.interrupt()` 方法来停止正在执行的任务的，因此某些无法响应中断的任务可能需要等到任务完成后才能停止线程

由于这两个方法调用过后线程池都不会再接收新任务了，所以在回收空闲线程后，还需要检查下线程是否都已经回收完毕了，是的话则需要将线程池的生命周期状态向 TIDYING 和 TERMINATED 迁移

```java
final void tryTerminate() {
    for (;;) {
        int c = ctl.get();
        //在以下几种情况不需要终止线程池：
        //1.还处于运行状态
        //2.已经处于 TIDYING 或 TERMINATED 状态
        //3.处于 SHUTDOWN 状态且还有待处理的任务
        if (isRunning(c) ||
                runStateAtLeast(c, TIDYING) ||
                (runStateOf(c) == SHUTDOWN && ! workQueue.isEmpty()))
            return;
        //在达到 TIDYING 状态前需要确保所有线程都被关闭了
        if (workerCountOf(c) != 0) { // Eligible to terminate
            interruptIdleWorkers(ONLY_ONE);
            return;
        }

        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            if (ctl.compareAndSet(c, ctlOf(TIDYING, 0))) {
                try {
                    //terminated() 方法执行完毕后，线程池状态就从 TIDYING 转为 TERMINATED 了，此时线程池就走向终止了
                    terminated();
                } finally {
                    ctl.set(ctlOf(TERMINATED, 0));
                    //唤醒所有在等待线程池 TERMINATED 的线程
                    termination.signalAll();
                }
                return;
            }
        } finally {
            mainLock.unlock();
        }
        // else retry on failed CAS
    }
}
```

## 6、任务队列的选择

阻塞队列(BlockingQueue)是一个支持两个附加操作的队列。这两个附加的操作是：在队列为空时，获取数据的线程会阻塞等待直到从队列获取到任务。当队列已满时，存储数据的线程会阻塞等待直到队列空出位置可以存入数据。阻塞队列常用于生产者和消费者的场景，生产者是往队列里添加数据的线程，消费者是从队列里取出数据的线程。阻塞队列就是生产者存放数据的容器，而消费者也只从容器里取数据

线程池实现解耦的关键就是有了 **任务队列/阻塞队列** 的存在。线程池中是以**生产者消费者模式+阻塞队列**来实现的，任务队列负责缓存外部提交的任务，线程负责从任务队列取出任务，这样客户端提交的任务就避免了和线程直接关联

选择不同的阻塞队列可以实现不一样的任务存取策略：

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/deee7e19ca544678bf862481adbabed0~tplv-k3u1fbpfcp-zoom-1.image)

## 7、任务的拒绝策略

随着客户端不断地提交任务，当前线程池大小也会不断增加。在当前线程池大小达到 corePoolSize 的时候，新提交的任务会被缓存到任务队列之中，由线程后续不断从队列中取出任务并执行。当任务队列满了之后，线程池就会创建非核心线程。当线程总数达到 maximumPoolSize 且所有线程都处于工作状态，同时任务队列也满了后，客户端再次提交任务时就会被拒绝。而被拒绝的任务具体的处理策略则由 `RejectedExecutionHandler` 来进行定义

```java
public interface RejectedExecutionHandler {

    void rejectedExecution(Runnable r, ThreadPoolExecutor executor);
    
}
```

当客户端提交的任务被拒绝时，线程池关联的 RejectedExecutionHandler 对象的 `rejectedExecution` 方法就会被调用，相应的拒绝策略可以由客户端来指定

ThreadPoolExecutor 提供了以下几种拒绝策略，默认使用的是 AbortPolicy

| 实现类              | 策略                                                         |
| ------------------- | ------------------------------------------------------------ |
| AbortPolicy         | 直接抛出异常，是 ThreadPoolExecutor 的默认策略               |
| DiscardPolicy       | 直接丢弃该任务，不做任何响应也不会抛出异常                   |
| DiscardOldestPolicy | 如果线程池未被停止，则将工作队列中最老的任务丢弃，然后尝试接纳该任务 |
| CallerRunsPolicy    | 如果线程池未被停止，则直接在客户端线程上执行该任务           |

任务的拒绝策略只会在提交任务的时候被触发，即只在 `execute(Runnable command)` 方法中被触发到。`execute(Runnable command)` 方法会判断当前状态是否允许接受该任务，如果不允许的话则会走拒绝任务的流程

```java
public void execute(Runnable command) {
    if (command == null)
        throw new NullPointerException();
    int c = ctl.get();
    if (workerCountOf(c) < corePoolSize) {
        if (addWorker(command, true))
            return;
        c = ctl.get();
    }

    if (isRunning(c) && workQueue.offer(command)) { //线程池还处于运行状态且成功添加任务到任务队列
        //需要重新检查下运行状态
        //因为等执行到这里时，线程池可能被其它线程关闭了
        int recheck = ctl.get();

        //1、如果线程池已经处于非运行状态了
        //1.1、如果移除 command 成功，则走拒绝策略
        //1.2、如果移除 command 失败（因为 command 可能已经被其它线程拿去执行了），则走第 3 步
        //2、如果线程池还处于运行状态，则走第 3 步
        //3、如果当前线程数量为 0，则创建线程进行处理
        //第 3 步的意义在于：corePoolSize 可以被设为 0，所以这里需要检查下，在需要的时候创建一个非核心线程
        if (! isRunning(recheck) && remove(command))
            reject(command);
        else if (workerCountOf(recheck) == 0)
            addWorker(null, false);
    }
    //如果线程池处于非运行状态了，或者是处于运行状态但队列已满了，此时就会走到这里
    //在这里尝试创建一个非核心线程
    //如果线程创建失败，说明要么是线程池当前状态大于等于 STOP，或者是任务队列已满且线程总数达到 maximumPoolSize 了
    //此时就走拒绝策略
    else if (!addWorker(command, false))
        reject(command);
}

final void reject(Runnable command) {
    handler.rejectedExecution(command, this);
}
```

## 8、监控线程池的运行状态

ThreadPoolExecutor 提供了多个配置参数以便满足多种不同的需求，这些配置参数包含：**corePoolSize、maximumPoolSize、keepAliveTime、allowCoreThreadTimeOut 等**。但很多时候我们一开始使用线程池时并不知道该如何配置参数才最为适应当前需求，那么就只能通过监控线程池的运行状态来进行考察，最终得到一份最合理的配置参数

可以通过 ThreadPoolExecutor 的以下几个属性来监控线程池的运行状态：

1. taskCount：线程池已执行结束（不管成功与否）的任务数加上任务队列中目前包含的任务数
2. completedTaskCount：线程池已执行结束（不管成功与否）的任务数，小于等于 taskCount
3. largestPoolSize：线程池曾经创建过的最大线程数量。如果该数值等于 maximumPoolSize 那就说明线程池曾经满过
4. getPoolSize()：获取当前线程总数
5. getActiveCount()：获取当前正在执行任务的线程总数

此外，ThreadPoolExecutor 也预留了几个钩子方法可以由子类去实现。通过以下几个方法，就可以实现每个任务开始执行前和执行后，以及线程池走向终止时插入一些自定义的监控代码，以此来实现：**计算任务的平均执行时间、最小执行时间和最大执行时间**等功能

```java
protected void beforeExecute(Thread t, Runnable r) { }

protected void afterExecute(Runnable r, Throwable t) { }

protected void terminated() { }
```

# 四、Executors

Executors 是 JDK 提供的一个线程池创建工具类，封装了很多个创建 ExecutorService 实例的方法，这里就来介绍下这几个方法，这些线程池的差别主要都是由于选择了不同的任务队列导致的，读者需要先认识下以下几种任务队列

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/b34aa0ed8322449d821b8f07666a66e2~tplv-k3u1fbpfcp-zoom-1.image)

`newFixedThreadPool`方法创建的线程池，核心线程数和最大线程数都是 nThreads，所以线程池在任何时候最多也只会有 nThreads 个线程在同时运行，且在停止线程池前所有线程都不会被回收。LinkedBlockingQueue 的默认容量是 Integer.MAX_VALUE，近乎无限，在线程繁忙的情况下有可能导致等待处理的任务持续堆积，使得系统频繁 GC，最终导致 OOM

此类线程池适合用于希望所有任务都能够被执行的情况

```java
public static ExecutorService newFixedThreadPool(int nThreads) {
    return new ThreadPoolExecutor(nThreads, nThreads,
                                  0L, TimeUnit.MILLISECONDS,
                                  new LinkedBlockingQueue<Runnable>());
}

public static ExecutorService newFixedThreadPool(int nThreads, ThreadFactory threadFactory) {
    return new ThreadPoolExecutor(nThreads, nThreads,
                                  0L, TimeUnit.MILLISECONDS,
                                  new LinkedBlockingQueue<Runnable>(),
                                  threadFactory);
}
```

`newSingleThreadExecutor`方法创建的线程池，核心线程数和最大线程数都是 1，所以线程池在任何时候最多也只会有 1 个线程在同时运行，且在停止线程池前所有线程都不会被回收。由于使用了 LinkedBlockingQueue，所以在极端情况下也是有发生 OOM 的可能

此类线程池适合用于执行需要串行处理的任务，或者是任务的提交间隔比任务的执行时间长的情况

```java
public static ExecutorService newSingleThreadExecutor() {
    return new FinalizableDelegatedExecutorService
        (new ThreadPoolExecutor(1, 1,
                                0L, TimeUnit.MILLISECONDS,
                                new LinkedBlockingQueue<Runnable>()));
}

public static ExecutorService newSingleThreadExecutor(ThreadFactory threadFactory) {
    return new FinalizableDelegatedExecutorService
        (new ThreadPoolExecutor(1, 1,
                                0L, TimeUnit.MILLISECONDS,
                                new LinkedBlockingQueue<Runnable>(),
                                threadFactory));
}
```

`newCachedThreadPool`方法创建的线程池，核心线程数是 0，最大线程数是 Integer.MAX_VALUE，所以允许同时运行的线程数量近乎无限。再加上 SynchronousQueue 是一个不储存元素的阻塞队列，每当有新任务到来时，如果当前没有空闲线程的话就会马上启动一个新线程来执行任务，这使得任务总是能够很快被执行，提升了响应速度，但同时也存在由于要执行的任务过多导致一直创建线程的可能性，这在任务耗时过长且任务量过多的情况下也可能导致 OOM

此类线程池适合用于对任务的处理速度要求比较高的情况

```java
public static ExecutorService newCachedThreadPool() {
    return new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                                  60L, TimeUnit.SECONDS,
                                  new SynchronousQueue<Runnable>());
}

public static ExecutorService newCachedThreadPool(ThreadFactory threadFactory) {
    return new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                                  60L, TimeUnit.SECONDS,
                                  new SynchronousQueue<Runnable>(),
                                  threadFactory);
}
```

`newScheduledThreadPool`方法创建的线程池对应的是 ScheduledThreadPoolExecutor，其继承于 ThreadPoolExecutor 并实现了 ScheduledExecutorService 接口，在线程池的基础上扩展实现了执行定时任务的能力。ScheduledThreadPoolExecutor 的核心线程数由入参 corePoolSize 决定，最大线程数是 Integer.MAX_VALUE，keepAliveTime 是 0 秒，所以该线程池可能同时运行近乎无限的线程，但一旦当前没有待执行的任务的话，线程就会马上被回收

此类线程池适合用于需要定时多次执行特定任务的情况

```java
public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize) {
    return new ScheduledThreadPoolExecutor(corePoolSize);
}

public static ScheduledExecutorService newScheduledThreadPool(
        int corePoolSize, ThreadFactory threadFactory) {
    return new ScheduledThreadPoolExecutor(corePoolSize, threadFactory);
}
```

`newSingleThreadScheduledExecutor`方法 `newScheduledThreadPool` 方法基本一样，只是直接指定了核心线程数为 1

```java
public static ScheduledExecutorService newSingleThreadScheduledExecutor() {
    return new DelegatedScheduledExecutorService
        (new ScheduledThreadPoolExecutor(1));
}

public static ScheduledExecutorService newSingleThreadScheduledExecutor(ThreadFactory threadFactory) {
    return new DelegatedScheduledExecutorService
        (new ScheduledThreadPoolExecutor(1, threadFactory));
}
```

# 五、线程池故障

## 1、线程池死锁

多个线程会因为循环等待对方持有的排他性资源而导致死锁，线程池也可能会因为多个任务间的相互依赖而导致**线程池死锁**。例如，如果在线程池中执行的任务 A 在其执行过程中又向同个线程池提交了任务 B，且任务 A 的执行结束又依赖于任务 B 的执行结果，那么就可能出现这样的一种极端情形：线程池中的所有正在执行任务的线程都在等待其它任务的处理结果，而这些任务均在任务队列中处于待执行状态，且由于线程总数已经达到线程池的最大线程数量限制，所以任务队列中的任务就会一直无法被执行，最终导致所有任务都无法完成，从而形成线程池死锁

因此，提交给同一个线程池的任务必须是没有互相依赖关系的。对于有依赖关系的任务，应该提交给不同的线程池，以此来避免死锁的发生

## 2、线程泄漏

**线程泄漏**指由于某种原因导致线程池中实际可用的线程变少的一种异常情况。如果线程泄漏持续存在，那么线程池中的线程会越来越少，最终使得线程池再也无法处理任务。导致线程泄露的原因可能有两种：由于线程异常自动终止或者由于程序缺陷导致线程处于非有效运行状态。前者通常是由于 `Thread.run()` 方法中没有捕获到任务抛出的 Exception 或者 Error 导致的，使得相应线程被提前终止而没有相应更新线程池当前的线程数量，ThreadPoolExecutor 内部已经对这种情形进行了预防。后者可能是由于客户端提交的任务包含阻塞操作（Object.wait() 等操作），而该操作又没有相应的时间或者条件方面的限制，那么就有可能导致线程一直处于等待状态而无法执行其它任务，这样最终也是形成了线程泄漏

# 六、总结

线程池通过复用一定数量的线程来执行不断被提交的任务，除了可以节约线程这种有限而昂贵的资源外，还包含以下好处：

- 提高响应速度。ThreadPoolExecutor 提供了预先创建一定数量的线程的功能，使得后续提交的任务可以立即被执行而无须等待线程被创建，从而提高了系统的响应速度
- 抵消线程创建的开销。一个线程可以先后用于执行多个任务，那创建线程带来的成本（资源和时间）就可以看做是被平摊到其执行的所有任务中。一个线程执行的任务越多，那么创建该线程的“性价比”就越高
- 封装了任务的具体执行过程。线程池封装了每个线程在创建、管理、复用、回收等各个阶段的逻辑，使得客户端代码只需要提交任务和获取任务的执行结果，而无须关心任务的具体执行过程。即使后续想要将任务的执行方式从并发改为串行，往往也只需要修改线程池内部的处理逻辑即可，而无需修改客户端代码
- 减少销毁线程的开销。JVM 在销毁一个已经停止的线程时也有着资源和时间方面的开销，采用线程池可以避免频繁地创建线程，从而减少了销毁线程的次数，减少了相应开销

# 七、参考资料

[Java线程池实现原理及其在美团业务中的实践](https://tech.meituan.com/2020/04/02/java-pooling-pratice-in-meituan.html)