> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

> 目前，多线程编程可以说是在大部分平台和应用上都需要实现的一个基本需求。本系列文章就来对 **Java 平台下的多线程编程知识**进行讲解，从**概念入门**、**底层实现**到**上层应用**都会涉及到，预计一共会有五篇文章，希望对你有所帮助 😎😎
>
> 本篇文章是第一篇，先来介绍下 Java 多线程的基础概念以及需要面对的挑战，是后续文章的敲门砖

# 一、多线程编程

假设存在三个事件（事件A、事件B、事件C）需要我们完成，每个事件均包含一定的前置处理时间和等待完成时间，即每个事件均需要先处理一定时间，处理完成后再等待一段时间，等待过后该事件就算作已完成了。那么，我们就可以采用三种不同的方式来完成这三个事件：

- 串行。按照顺序依次来处理三个事件，待某个事件处理且等待结束后再处理下一个事件。这种方式需要消耗一个人力
- 并发。先处理事件 A，当事件 A 的前置处理完成后，转而来处理事件 B，当事件 B 的前置处理完成后，转而来处理事件 C，最后就只要等待三个事件结束即可。这种方式需要消耗一个人力
- 并行。三个事件分别转交由三个人进行同时处理。这种方式需要消耗三个人力

从直观上看，串行的处理效率最低，耗时最长，在每次等待事件完成的时间段内人力都被白白消耗了。并行的处理效率最高，耗时最短，理论上总的所需耗时取决于用时最长的那个事件，但其需要的人力成本也最高。并发的处理效率和耗时长短均介于串行和并行之间，需要的人力成本和串行持平（均低于并行）

从以上假设的情景映射到软件世界

- 并发就是在一段时间内以交替的方式来完成多个任务，使用多个线程来分别处理不同的任务，即使在单个处理器的情况下也可以通过**时间片切换**的技术来实现在一个时间段内运行多个线程，因此即使只有一个处理器也可以实现并发
- 并行就是以同时处理的方式来完成多个任务，使用多个线程来分别处理不同的任务，然后将多个线程分别转交给不同的处理器进行运行，因此并行需要有多个处理器才可以实现
- 而在现实情况下，程序需要同时执行的线程数量往往是远多于处理器的数量，并发才是我们的主要实现目标。因此，我们可以这么理解：**实现多线程编程的过程就是将任务的执行方式由串行改为并发的过程，即实现并发化，以此来尽量提高程序和硬件的运行效率**

那使用多线程有什么好处呢？

- 对于计算机来说，其处理器的运算速度相比存储和通信子系统要快了几个数量级，如果只采用单线程，那么当线程在处理磁盘 I/O、数据库访问等任务时，处理器就被闲置着没有活干了，这就造成了很大的性能浪费。此时就可以通过采用多线程来使处理器尽量处于运转状态，尽量利用到其运算能力

- 此处，对于一个服务端，衡量其性能高低好坏的一个重要标准之一是**每秒事务处理数（TPS）的大小**，它代表着一秒内服务端平均能响应的请求总数。服务端可能会在极小的时间段内收到多个请求，服务端的 TPS 就和程序的并发能力（即同时处理多项任务的能力）有着密切关联

- 再比如，在 Android 应用开发中，系统规定了只有 main 线程才可以进行 UI 绘制和刷新，如果不将耗时操作（IO读写、网络请求等）放到子线程进行处理，那么用户对应用的 UI 操作行为（点击屏幕、滑动列表等）很大可能就会由于无法及时被 main 线程处理，导致应用似乎被卡住了，最终用户可能就会放弃使用该应用了

所以，使用多线程编程可以最大限度地利用系统提供的处理能力，提高程序的吞吐率和响应性，避免性能浪费。但使用多线程就一定就能提高效率吗？这不一定

- 采用多线程后，各个线程间可能会相互竞争系统资源，例如处理器时间片、排他锁、带宽、硬盘读写等，而资源往往是有限且每次只能由一个线程使用的，并发编程的最终效益就往往受限于资源的有限分配，多个线程争用同一个排他性资源就会带来线程上下文切换甚至死锁等问题
- 例如，当采用多个线程来分段下载某个网络文件以此来希望减少下载耗时。由于带宽大小是固定的，使用多个线程同时进行下载首先就会拉低每个线程的平均可用带宽大小，每个线程下载到的单份资源也需要通过硬盘读写合并成一个完整的文件，每段资源的合并需要通过调度程度来按顺序写入，维护调度顺序的过程也是有着性能的消耗，多个线程进行 IO 读写也会加大发生线程上下文切换的次数。因此，某些情况下采用多线程可能会显得“并不那么值得”，需要我们根据实际情况来衡量使用

# 二、进程与线程

- 程序（Program）是对指令、数据及其组织形式的描述，是一种静态的概念
- 进程（Process）是程序的运行实例，每个被启动的程序就对应运行于操作系统上的一个进程，是一种动态的概念。进程是程序向操作系统申请资源（内存空间、文件句柄等）的基本单位，也是操作系统进行资源调度和资源分配的基本单位。运行一个 Java 程序实质上就是启动了一个 Java 虚拟机进程
- 线程（Thread）是进程中可独立执行的最小单位，也是操作系统能够进行运算调度的最小单位，也被称为轻量级进程。每个线程总是包含于特定的进程内，一个进程可以包含多个线程且至少包含一个线程，线程是进程中的实际运行单位。同一个进程中的所有线程共享该进程中的资源（内存空间、文件句柄等）
- 任务（Task）即线程所要完成的逻辑计算。线程在创建之初的目的就是为了让其来执行特定的逻辑计算，其所要完成的工作就称为该线程的任务

多线程编程是一种以线程为基本抽象单位的编程范式（Praadigm）。现代计算机操作系统几乎都支持多任务处理，多任务处理有两种不同的类型：**基于进程的多任务处理**和**基于线程的多任务处理**

- 基于进程的多任务处理指操作系统支持同时运行多个程序，进程是调度程序能够调度的最小代码单元。进程是重量级的任务，每个进程需要有自己的地址空间，进程间通信开销很大而且有很多限制，从一个进程上下文切换到另一个进程上下文的开销也很大
- 基于线程的多任务处理意味着单个进程可以同时执行多个任务，线程是调度程序能够调度的最小代码单元。基于线程的多任务处理需要的开销要比基于进程的多任务处理小得多。线程是轻量级的任务，它们共享同个进程下的资源，线程间通信的开销不大，并且同个进程下的不同线程上下文间的切换所需要的的开销要比不同进程上下文间的切换小得多

基于进程的多任务处理是由操作系统来实现并管理的，一般的程序开发接触不到这个层面。而基于线程的多任务处理则可以由程序开发者自己来实现并进行管理。可以说，多线程编程的一个目的就是为了实现**基于线程的多任务处理**

Java 对多线程提供了内置支持。Java 标准类库中的 `java.lang.Thread` 类就是对线程这个概念的抽象实现，提供了在不同的硬件和操作系统平台上对线程操作的统一处理，屏蔽了不同的硬件和操作系统的差异性

> Java 本身是一个多线程的平台，即使开发者没有主动创建线程，此时进程内还是使用到了多个线程（例如，还存在 GC 线程）。所谓的单线程编程往往指的是在程序中开发者没有主动创建线程

# 三、Thread

Java 标准类库 `java.lang.Thread` 是 Java 平台对线程这个概念的抽象实现，Thread 类或者其子类的一个实例就是一个线程

## 1、线程的属性

| 属性               | 含义                                                         |
| ------------------ | :----------------------------------------------------------- |
| 编号（ID）         | long。用于标识不同的线程，不同的线程拥有不同的编号，在 Java 虚拟机的单次生命周期内具有唯一性 |
| 名称（Name）       | String。用于区分不同的线程，方便开发和调试时定位问题，在 Java 虚拟机的单次生命周期内可以重复 |
| 类别（Daemon）     | boolean。值为 true 表示该线程为守护线程，否则为用户线程      |
| 优先级（Priority） | int。Java 定义了 1~10 的 10 个优先级，默认值为 5             |

按照线程是否会阻止 Java 虚拟机正常停止，Java 中的线程分为**用户线程**和**守护线程**。用户线程会阻止 Java 虚拟机的正常停止，即一个 Java 虚拟机只有在其所有用户线程都运行结束的情况下才能正常停止。而守护线程不会影响 Java 虚拟机的正常停止，即使应用程序中还有守护线程在运行也不影响 Java 虚拟机的正常停止。因此，守护线程适合用于执行一些重要性不是很高的任务。但如果 Java 虚拟机是被强制停止或者由于异常被停止的话，用户线程也无法阻止 Java 虚拟机的停止

Java 线程的优先级属性本质上只是一个给线程调度器的提示信息，以便于线程调度器决定优先调度哪些线程运行，优先级高的线程理论上会获得更多的处理器使用时间，但线程调度器并不保证一定按照线程优先级的高低来调度线程。此外， JVM 所在的操作系统可能会忽略甚至主动来修改我们对线程的优先级配置，且如果线程的优先级设置不当，甚至有可能导致线程永远无法得到运行，即产生线程饥饿

如果在线程 A 中创建了线程 B，那么线程 B 就称为线程 A 的子线程，线程 A 就称为线程 B 的父线程。由于 Java 虚拟机创建的 main 线程（主线程）负责执行 Java 程序的入口方法 `main()` 方法，因此 main 方法中创建的线程都是 main 线程的子线程或间接子线程。此外，一个线程是否是守护线程默认与其父线程相同，线程优先级的默认值也与其父线程的优先级相同。需要注意的是，虽然线程间具有这类父子关系，但是它们并不会相互影响对方的生命周期，一方线程生命周期的结束（不管是正常结束还是异常停止）并不影响另一个线程继续运行

## 2、线程的方法

| 方法                          | 说明                                                         |
| ----------------------------- | ------------------------------------------------------------ |
| static Thread currentThread() | 返回当前的执行线程对象                                       |
| static void yield()           | 使当前线程主动放弃其处理器的占用，但不一定会使当前线程暂停   |
| static void sleep(long)       | 使当前线程休眠指定时间                                       |
| void start()                  | 启动线程                                                     |
| void join()                   | 若线程 A 调用线程 B 的 join() 方法，则线程 A 会暂停运行，直到线程 B 运行结束 |
| void suspend()                | 暂停线程，进入睡眠状态（已废弃）                             |
| void resume()                 | 唤醒线程，和 suspend() 成对使用（已废弃）                    |
| void stop()                   | 停止线程（已废弃）                                           |

`Thread.suspend()` 和 `Thread.resume()` 是 Thread 类提供的用于暂停和唤醒线程的方法，用于在某些运行条件不满足的时候暂停执行任务，后续在运行条件满足的时候唤醒线程继续执行任务，但现在均已废弃

## 3、线程的状态

线程在它的整个生命周期内会先后处于不同状态，也可能会在多个状态间来回切换。对于给定的线程实例，可以使用 `Thread.getState()` 方法获取线程的状态，该方法返回 `Thread.State` 枚举类型值，用于标明在调用该方法时线程所处的当前状态，返回值包含以下几种可能：

| 值            | 状态                                                         |
| ------------- | ------------------------------------------------------------ |
| NEW           | 线程处于新建状态，还没有调用 start() 方法                    |
| RUNNABLE      | 线程要么当前正在执行，要么在获得处理器时间片之后就可以执行   |
| BLOCKED       | 线程因为发起了阻塞式操作，或者正在等待需要的资源时被挂起     |
| WAITING       | 线程因为等待某些动作而挂起执行。例如，因为调用了 wait() 或 join() 方法所以处于这种状态，这种状态下的线程需要由外部其它线程来唤醒自身 |
| TIMED_WAITING | 线程挂起一段指定的时间，当达到指定的时间到达后，线程就会转为 RUNNABLE 状态。例如当调用 sleep(long) 、wait(long) 、 join(long) 等方法时就会处于这种状态 |
| TERMINATED    | 已经运行结束的线程就处于此状态                               |

可以用更加通俗的语言来描述线程的生命周期：

1. 新建状态-NEW

   使用 `new` 关键字建立一个线程对象后，线程就处于新建状态，一个已创建但还未启动的线程就处于此状态。线程会保持这个状态直到被调用 `Thread.start()` 方法

2. 就绪状态-RUNNABLE

   当线程对象调用了 `start()` 方法之后，线程就会进入就绪状态。该状态可以看成一个复合状态，它包含两个子状态：**READY** 和 **RUNNING**。前者表示线程处于就绪队列中，当被线程调度器选中调度后就可以正式运行，变为 RUNNING 状态。后者表示线程正处于运行中，其 `run()` 方法对应的指令正在由处理器执行。当执行线程的 `yiedId()` 方法时，其状态可能会由 RUNNING 切换为 READY

3. 阻塞状态-BLOCKED

   当线程发起一个阻塞式的 IO 操作或者是在申请一个由其它线程持有的独占资源时，该线程就会处于此状态。处理 BLOCKED 状态的线程不会占用 CPU 资源。当其目标行为或者是目标资源被满足后，就可以切换为 RUNNABLE 状态

4. 无限期等待状态-WAITING

   当线程的运行需要满足某些执行条件而当前并不满足时，通常就会通过让该线程主动调用 `Object.wait()`、`Thread.join()` 等类似方法将线程切换为 WAITING 状态。当前状态是 WAITING 的线程处于暂停运行的状态，需要外部其它线程通过 `Object.notify()` 等方法来主动唤醒该线程

5. 限期等待状态-TIMED_WAITING

   TIMED_WAITING 状态和 WAITING 状态类似。区别在于 TIMED_WAITING 状态并非是线程本身完全无限制地进行等待，其等待行为带有指定时间范围的限制，当在指定时间内没有完成该线程所期望的特定操作时，该线程就会转为 RUNNABLE 状态。可以通过  `Object.wait(long)` 方法来使线程切换为 TIMED_WAITING 状态

6. 终止状态-TERMINATED

   当一个线程完成自身任务或者由于其它原因被迫终止时，线程就会切换到终止状态，至此线程的整个生命周期就结束了

由于一个线程在其整个生命周期内只能被启动一次，所以线程也只会处于一次 `NEW` 状态和一次 `TERMINATED` 状态。对于一个多线程系统来说，最理想的情况就是所有已启动且未结束的线程能一直处于 RUNNING 状态，但这是不可能实现的。在现实场景下线程会在多个状态间来回切换，且线程从 RUNNABLE 状态转换为 BLOCKED、WAITING 和 TIMED_WAITING 这几个状态中的任何一个时都意味着发生了线程上下文切换

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/ee0ac63316f7456092955a103bfb25c8~tplv-k3u1fbpfcp-zoom-1.image)

## 4、ThreadGroup

线程组（ThreadGroup）用来表示一组相关联的线程，它是 Thread 类包含的一个内部属性，可以通过 `Thread.getThreadGroup()`来获取该值。线程与线程组之间的关系类似于文件系统中文件与文件夹之间的关系，一个线程组可以包含多个线程以及其它线程组

如果在创建线程时没有显示指定线程所属的线程组的话，在默认情况下线程就被归类于其父线程所属的线程组下。从 Thread 类的以下方法就可以看出来：

```java
private void init(ThreadGroup g, Runnable target, String name,
                  long stackSize, AccessControlContext acc,
                  boolean inheritThreadLocals) {
    ····

    Thread parent = currentThread();
    SecurityManager security = System.getSecurityManager();
    if (g == null) {
        /* Determine if it's an applet or not */

        /* If there is a security manager, ask the security manager
           what to do. */
        if (security != null) {
            //默认也是返回 Thread.currentThread().getThreadGroup()
            g = security.getThreadGroup();
        }

        /* If the security doesn't have a strong opinion of the matter
           use the parent thread group. */
        if (g == null) {
            g = parent.getThreadGroup();
        }
    }
    ····

}
```

Java 虚拟机在创建 main 线程（所有线程的父线程）的时候会为其自动指定一个线程组，因此任何一个线程都有一个线程组与之相关联。且一个线程组的父线程组默认是在声明该线程组时所在线程的线程组，但并非所有线程组均有父线程组，最顶层线程组就不包含父线程组

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 */
fun main() {
    val mainThreadGroup = Thread.currentThread().threadGroup
    println(mainThreadGroup)
    println("mainThreadGroup.parent: " + mainThreadGroup.parent)
    println("mainThreadGroup.parent.parent: " + mainThreadGroup.parent.parent)
    val thread = Thread()
    println(thread.threadGroup)
    val thread2 = Thread(ThreadGroup("otherThreadGroup"), "thread")
    println(thread2.threadGroup)
//    java.lang.ThreadGroup[name=main,maxpri=10]
//    mainThreadGroup.parent: java.lang.ThreadGroup[name=system,maxpri=10]
//    mainThreadGroup.parent.parent: null
//    java.lang.ThreadGroup[name=main,maxpri=10]
//    java.lang.ThreadGroup[name=otherThreadGroup,maxpri=10]
}
```

ThreadGroup 本身存在设计缺陷问题，目前的使用场景有限，日常开发中可以无需理会

## 5、线程异常捕获

在很多时候，我们会通过创建一个线程池来执行任务，而当某个任务由于抛出异常导致其执行线程异常终止时，我们就需要对这种异常情况进行上报以便后续分析。要实现这个效果，就需要能够收到线程被异常终止时的事件通知，这就需要用到 `Thread.setUncaughtExceptionHandler(UncaughtExceptionHandler)` 方法

通过该方法我们可以在异常发生时且线程被停止前获取到相应的 Thread 对象和 Throwable 实例

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 */
fun main() {
    val runnable = Runnable {
        for (i in 4 downTo 0) {
            println(100 % i)
            Thread.sleep(100)
        }
    }
    val thread = Thread(runnable, "otherName")
    thread.setUncaughtExceptionHandler { t, e ->
        println("threadName: " + t.name)
        println("exc: $e")
    }
    thread.start()
}
```

```kotlin
0
1
4
2
4
0
0
1
0
0
threadName: otherName
exc: java.lang.ArithmeticException: / by zero
```

ThreadGroup 本身也实现了 UncaughtExceptionHandler 接口，所以如果 Thread 对象不包含关联的 UncaughtExceptionHandler 实例的话，则会将异常交由 ThreadGroup 来进行处理

从 Thread 类的以下逻辑就可以看出来

```java
public class Thread implements Runnable {

    private ThreadGroup group;

    private volatile UncaughtExceptionHandler uncaughtExceptionHandler;

    private void dispatchUncaughtException(Throwable e) {
        getUncaughtExceptionHandler().uncaughtException(this, e);
    }

    public UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return uncaughtExceptionHandler != null ?
            uncaughtExceptionHandler : group;
    }
    
}
```

ThreadGroup 默认情况下会将异常交由其父线程组进行处理，而对于不包含父线程组的线程组对象（顶层线程组），则会将异常交由 Thread 类的 `defaultUncaughtExceptionHandler` 进行处理。所以，我们可以通过 Thread 的静态方法 `setDefaultUncaughtExceptionHandler` 方法来为程序设置一个全局的默认异常处理器

```java
public class ThreadGroup implements Thread.UncaughtExceptionHandler {

    public void uncaughtException(Thread t, Throwable e) {
        if (parent != null) {
            //当有父线程组时，则将异常交由父线程组来处理
            parent.uncaughtException(t, e);
        } else {
            //当父线程组不存在时，则尝试将异常交由 DefaultUncaughtExceptionHandler 来处理
            Thread.UncaughtExceptionHandler ueh =
                Thread.getDefaultUncaughtExceptionHandler();
            if (ueh != null) {
                ueh.uncaughtException(t, e);
            } else if (!(e instanceof ThreadDeath)) {
                System.err.print("Exception in thread \""
                                 + t.getName() + "\" ");
                e.printStackTrace(System.err);
            }
        }
    }
    
}

public class Thread implements Runnable {

    private static volatile UncaughtExceptionHandler defaultUncaughtExceptionHandler;
    
    public static UncaughtExceptionHandler getDefaultUncaughtExceptionHandler(){
        return defaultUncaughtExceptionHandler;
    }
    
    //设置全局默认的异常处理器
    public static void setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler eh) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(
                new RuntimePermission("setDefaultUncaughtExceptionHandler")
                    );
        }

         defaultUncaughtExceptionHandler = eh;
     }
    
}
```

所以，当线程由于异常而终止时，UncaughtExceptionHandler 实例的选择优先级从高到低分别是：

1. Thread.uncaughtExceptionHandler
2. ThreadGroup.uncaughtExceptionHandler
3. Thread.defaultUncaughtExceptionHandler

## 6、ThreadFactory

在项目中先后需要使用到多个线程是一个普遍的需求，而如果每次均简单的通过 `new Thread()` 来创建线程的话，在出现问题时就很难定位问题所在。所以 Java 标准库也提供了创建线程的工厂方法，即 ThreadFactory 接口

```java
public interface ThreadFactory {

    Thread newThread(Runnable r);
    
}
```

ThreadFactory 提供了将要执行的任务 Runnable 与要创建的 Thread 相关联的方法，即我们可以通过 ThreadFactory 来标明 Thread 要执行的具体任务、为 Thread 设置一个有具体含义的名字、设置 Thread 的运行优先级等

例如，Executors 内部就包含了一个 `DefaultThreadFactory`，通过 `threadNumber` 自增的方式为每一个创建的线程设置了特定的线程名、确保线程是用户线程、确保线程的优先级为正常级别

```java
static class DefaultThreadFactory implements ThreadFactory {
    private static final AtomicInteger poolNumber = new AtomicInteger(1);
    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;

    DefaultThreadFactory() {
        SecurityManager s = System.getSecurityManager();
        group = (s != null) ? s.getThreadGroup() :
                              Thread.currentThread().getThreadGroup();
        namePrefix = "pool-" +
                      poolNumber.getAndIncrement() +
                     "-thread-";
    }

    public Thread newThread(Runnable r) {
        Thread t = new Thread(group, r,
                              namePrefix + threadNumber.getAndIncrement(),
                              0);
        if (t.isDaemon())
            t.setDaemon(false);
        if (t.getPriority() != Thread.NORM_PRIORITY)
            t.setPriority(Thread.NORM_PRIORITY);
        return t;
    }
}
```

而对于我们项目自己定义的线程池，使用 ThreadFactory 的一个比较有意义的用处是：为线程设置关联的 UncaughtExceptionHandler，这在提高系统的健壮性方面是很有好处的

## 7、线程的执行时机

我们可以简单地理解为运行一个线程就是要求 Java 虚拟机来调用 Runnable 对象的 `run()` 方法，从而使得线程的任务处理逻辑得以被执行。但我们通过 `Thread.start()` 启动一个线程并不意味着线程就能够马上被执行，线程的具体执行时机由**线程调度器**来决定，执行时机具有不确定性，甚至可能会由于线程活性故障而永远无法运行。此外，由于 `run()` 方法是 `public` 的，所以它也可以由外部主动来调用执行，但此时其任务就是由当前的运行线程来执行，这在大多数时候都是没有实际意义的

而不管是通过什么方式来创建线程，当线程的 `run()` 方法执行结束时（不管是正常结束还是由于异常而中断运行），线程的生命周期也就走到末尾了，其占用的资源会在后续被 Java 虚拟机垃圾回收。而且，线程是一次性资源，我们无法通过再次调用 `start()` 方法来重新启动线程，当多次调用该方法时会抛出 `IllegalThreadStateException`

# 四、多线程安全

实现多线程编程不是简单地声明多个 Thread 对象并启动就可以的了，在现实场景中，多个线程间往往是需要完成数据交换和行为交互等各种复杂操作的，而不是简单地“各行其是”。相比单线程，**使用多线程会带来许多在单线程下不存在或者根本不用考虑的问题**

## 1、竞态

先来看一个简单的例子。假设存在一个商店 Shop，其初始商店数量为零。存在四十个生产者 Producer 为其生产商品，每个 Producer 会各自为商店提供一个商品。那么，理论上当所有 Producer 生产完毕后，Shop 的商品数量 goodsCount 应该是四十

可是，运行以下代码后你会发现实际数量大概率是会少于四十

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 */
class Shop(var goodsCount: Int) {

    fun produce() {
        goodsCount++
    }

}

class Producer(private val shop: Shop) : Thread() {

    override fun run() {
        sleep(100)
        if (shop.goodsCount < 40) {
            shop.produce()
        }
        println("over")
    }

}

fun main() {
    val shop = Shop(0)
    val threads = mutableListOf<Thread>()
    for (i in 1..40) {
        threads.add(Producer(shop))
    }
    threads.forEach {
        it.start()
    }
    //保证所有 Producer Thread 都执行完毕会再执行之后的语句
    threads.forEach {
        it.join()
    }
    println("shop.goodsCount: " + shop.goodsCount)
}
```

以上代码就使用到了多个线程，单个 Producer 线程的行为逻辑是独立的，而多个 Producer 线程的行为逻辑对于 Shop 来说是互相交错且先后顺序不确定的，这就有可能导致一种情况：两个 Producer 同时判断到当前商品数量是十，然后同时为商店生产第十一件商品（**shop.goodsCount++**），最终就导致某个 Producer 生产的第十一号商品被另一个 Producer 覆盖了，两个 Producer 只生产了一件商品，即数据**更新无效/更新丢失**

`shop.goodsCount++` 这条语句虽然看起来像是一个不可分割的操作（原子操作），但它实际上相当于如下伪代码所表示的三个指令的组合：

```kotlin
load(shop.goodsCount , r1) //指令1，将变量 shop.goodsCount 的值从内存读到寄存器 r1
increment(r1) //指令2，将寄存器 r1 的值加1
store(shop.goodsCount , r1) //指令3，将寄存器 r1 的内容写入变量 shop.goodsCount 所对应的内存空间
```

多个 Producer 线程可能会同时各自执行上述指令。例如，假设当前 goodsCount 是十，Producer1 和 Producer2 同时执行到指令1，两个线程将 goodsCount 读到各自处理器的寄存器上，即每个线程会各自拥有一份副本数据，然后对各自寄存器的值进行自增加一的操作，当执行到指令三时，由于 goodsCount  所在的内存空间是特定的，所以两个 Producer 线程对内存空间上 goodsCount 的值的回传会存在相互覆盖的情况。即原本最终结果应该是**递增加二**的行为最终却只有**递增加一**

以上是多线程编程中经常会遇到的一个现象，即竞态。竞态是指计算的正确性依赖于相对时间顺序或者线程的交错。竞态并不一定就会导致计算结果不正确，它只是不排除计算结果时而正确时而错误的可能

------

从上述代码中可以总结出竞态的两种模式：**read-modify-write（读-改-写）**、**check-then-act（检测后行动）**

read-modify-write 的步骤即：读取一个共享变量的值，然后根据该值进行一些计算、接着根据计算结果更新共享变量的值。例如，上述代码中的 `goodsCount++` 就是这种模式，相当于如下伪代码所表示的三个指令的组合

```kotlin
load(shop.goodsCount , r1) //指令1，将变量 shop.goodsCount 的值从内存读到寄存器 r1
increment(r1) //指令2，将寄存器 r1 的值加1
store(shop.goodsCount , r1) //指令3，将寄存器 r1 的内容写入变量 shop.goodsCount 所对应的内存空间
```

线程 A 在执行完指令1，开始执行或者正在执行指令2时，线程 B 可能已经执行完了指令3，这使得线程 A 当前持有的共享变量 shop.goodsCount 是旧值，当线程 A 执行完指令3时，这就使得线程 B 对共享变量的更新被覆盖了，即造成了更新丢失

---

check-then-act 的步骤即：读取一个共享变量的值，根据该变量的值决定下一步的动作是什么。例如，以下代码就是这种模式

```kotlin
if (shop.goodsCount < 40) { //操作1
    shop.produce() //操作2
}
```

线程 A 在执行完操作1，开始执行操作2之前，线程 B 可能已经更新了共享变量 `shop.goodsCount` 的值导致 if 语句中的条件变为不成立，可此时线程 A 依然会执行操作2，这是因为线程 A 此时并不知道共享变量已经被更新且导致运行条件不成立了

> 从上述分析中我们可以总结出竞态产生的一般条件。设 Q1 和 Q2 是并发访问共享变量 V 的两个操作，这两个操作并非都是读操作。如果一个线程在执行 Q1 期间另外一个线程同时执行 Q2，那么无论 Q2 是读操作还是写操作都会导致竞态。从这个角度来看，竞态可以看做是由于访问（读取、更新）同一组共享变量的多个线程所执行的操作被相互交错而导致的。而上述代码中遇到的**更新丢失**和**读到脏数据**问题就是由于竞态的存在而导致的
>
> 需要注意的是，竞态的产生前提是涉及到了多个线程和共享变量。如果系统仅包含单个线程，或者不涉及共享变量，那么就不会产生竞态。对于局部变量（包括形式参数和方法体内定义的变量），由于不同的线程访问的是各自的那一份局部变量，因此局部变量的使用不会导致竞态

## 2、线程安全性

如果一个类在单线程环境下能够正常运行，并且在多线程环境下也不需要考虑运行时环境下的调度和交替执行，使用方也不必为其做多任何操作也能正常运行，那么我们就说该类是线程安全的，即这个类具有线程安全性。反之，如果一个类在单线程环境下正常运行而在多线程环境下无法正常运行，那么这个类就是非线程安全的。所以，只有非线程安全的类才会导致竞态

如果一个类不是线程安全的，我们就说它在多线程环境下存在多线程安全问题。以上定义也适用于多个线程间的共享数据

多线程安全问题概括来说表现为三个方面：**原子性、可见性、有序性**

### 原子性

原子的字面意思即**不可分割**。对于涉及共享变量访问的操作，若该操作从其执行线程以外的其它任意线程来看是不可分割的，那么该操作就是**原子操作**，相应的就称该操作具有**原子性**。所谓“不可分割”，是指访问（读、写）某个共享变量的操作从其执行线程以外的任何线程来看，该操作要么已经完成，要么尚未发生，其它线程不会看到该操作执行了一部分的中间效果

例如，假设存在一个共享的全局变量 Shop 对象，其存在一个 `update()` 方法。当线程 A 执行 `update()` 方法时，在线程 A 执行完语句1之后而未执行语句2之前，此时线程 B 就会看到 `goodsCount` 已递增加一而 `clerk` 还未递增加一的这样一个中间效果。此时，我们就说 `update()` 方法作为一个整体不具备原子性

```kotlin
class Shop(var goodsCount: Int, var clerk: Int) {

    fun update() {
        goodsCount++ //语句1
        clerk++ //语句2
    }

}
```

理解原子操作这个概念还需要注意以下两点：

- 原子操作是针对共享变量的操作而言的，仅涉及局部变量访问的操作无所谓是否是原子性，或者可以直接将其看作成原子操作
- 原子操作是从该操作的执行线程以外的其它线程的视角来描述的，也就是说它只在多线程环境下才有意义，所以可以将单线程环境下的所有操作均当做原子操作

总的来说，Java 中有两种方式来提供原子性：

-  第一种是使用锁（Lock）。锁具有排他性，它能够保障共享变量在任意一个时刻只能够被一个线程访问。这就排除了多个线程在同一时刻访问同一个共享变量而导致干扰与冲突的可能，从而消除了竞态
-  第二种是利用处理器提供的 CAS 指令。CAS 指令实现原子性的方式与锁在本质上是相同的，差别在于锁通常是在软件这一层面实现的，而 CAS 是直接在硬件（处理器和内存）这一层次实现的，可以被看做“硬件锁”

Java 语言规范规定了：**在 Java 语言中，64 位以外的任何类型的变量的读写操作都是原子操作**。而对于 long 和 double 等 64 位的数据类型的读写操作并不强制规定 Java 虚拟机必须保证其原子性，可以由 Java 虚拟机自己选择是否要实现。因此在多线程并发读写同一 long/double 型共享变量的情况下，一个线程可能会读取到其它线程更新该变量的“中间结果”。而之所以会有中间结果，是因为对于 64 位的存储空间的写操作，虚拟机可能会将其拆解为两个步骤来实现，比如先写低 32 位再写高 32 位，从而导致外部线程读取到一个中间结果值。但这个问题也不需要特意关注，因为目前商用 Java 虚拟机几乎都选择将 64 位数据的读写操作实现为原子操作。此外，Java 语言规范也特别规定了用 volatile 关键字修饰的 long/double 型变量的读写操作具有原子性。

需要注意的是， volatile 关键字仅能保障变量读写操作的原子性，但并不能保障其它操作（例如 read-modify-write 、check-then-act）的原子性

### 可见性

在多线程环境下，一个线程对某个共享变量进行更新之后，后续访问该变量的其它线程可能无法立即读取到这个更新的结果，甚至永远也无法读取到，这体现了多线程安全性问题中的一个：可见性。可见性是指一个线程对共享变量的更新结果对于其它读取相应共享变量的线程而言是否可见的问题。多线程程序在可见性方面存在问题意味着某些线程读取到了旧数据，而这往往会导致我们的程序出现意想不到的问题

会存在可见性问题。一方面是由于 JIT 编译器可能出于提高代码运行效率考虑而自动对代码进行一些“优化”，使得共享变量更新失效。一方面是由于处理器并不是直接对主内存中的共享变量进行访问，而是会各自在自己的高速缓存上保留着对共享变量的一份副本，处理器直接访问的是副本数据，对副本数据的修改需要同步回主内存后才可以对其它处理器可见。所以一个处理器对共享变量的更新结果并不一定能立即同步到其它处理器上，这就导致了可见性问题的出现

对于同一个共享变量而言，一个线程更新了该变量的值之后，如果其它线程能够读取到这个更新后的值，那么这个值就被称为该变量的相对新值。如果读取这个共享变量的线程在读取并使用该变量的时候其它线程无法更新该变量的值，那么该线程读取到的值就被称为该变量的最新值。可见性的保障仅仅意味着一个线程能够读取到共享变量的相对新值，而并不意味着线程能够读取到相应变量的最新值

可见性问题是由于使用了多线程所导致的，它与当前是单核处理器还是多核处理器无关。在单核处理器下，多线程并发是通过时间片分配技术来实现的，此时虽然多个线程都是运行在同个处理器上，但是由于在上下文切换的时候，一个线程对共享变量的修改会被当做其上下文信息保存起来，这也会导致另外一个线程无法立即读取到该线程对共享变量的修改

### 有序性

在说有序性之前，需要先介绍下**重排序**

顺序结构是结构化编程中的一种基本结构，它表示我们希望某个操作必须先于另外一个操作得以执行，但是在多核处理器环境下，这种操作执行顺序可能是没有保障的。编译器和处理器可以在保证不影响单线程执行结果的前提下，对源代码的指令进行重新排序执行，处理器可能不是完全依照程序的目标代码所指定的顺序来执行指令。另外，一个处理器上执行的多个操作，从其它处理器的角度来看其顺序可能与目标代码所指定的顺序不一致。这种现象就叫做重排序。重排序是对内存访问有关的操作（读和写）所做的一种优化，它可以在不影响单线程程序正确性的情况下提升程序的性能。但是，它可能对多线程程序的正确性产生影响，即它可能导致线程安全问题

重排序分为以下几种：

- 编译器优化的重排序。编译器在不改变单线程程序语义的前提下，可以重新安排语句的执行顺序
- 指令级并行的重排序。现代处理器采用了指令级并行技术来将多条指令重叠执行。如果不存在数据依赖性，处理器可以改变语句对应机器指令的执行顺序
- 内存系统的重排序。由于处理器使用缓存和读／写缓冲区，这使得加载和存储操作看上去可能是在乱序执行

有序性指的就是在什么情况下一个处理器上运行的线程所执行的内存访问顺序在另外一个处理器上运行的其它线程看来是乱序的。所谓乱序，是指内存访问操作的顺序看起来是发生了变化

## 3、不安全的线程安全类

上文有提到如何定义一个类是否是线程安全的，Java 也提供了很多被称为线程安全的类，例如 `java.util.Vector。`Vector 类内部的 `add()`、`removeAt()` 和 `size()` 等很多方法都使用了 synchronize 进行修饰，保证了在多线程环境下的安全性，但这种同步保障也无法阻止开发者在逻辑层面上写出不安全的代码

例如，对于以下代码。即使 `add()` 和 `removeAt()` 两个方法由于 Vector 类内部的同步处理，保障了两个方法一定是串行执行的，但由于方法调用端缺少了额外的同步处理，导致调用端可能会读取到一个过时的 `vector.size` 值，最终导致索引越界抛出 **ArrayIndexOutOfBoundsException**

所以说，线程安全类可能只是保障了其自身单次操作行为的线程安全性，使得我们在调用的时候不需要进行额外的同步保障。但对于使用方的一些特定顺序的连续调用，就还是需要在外部实现额外的同步手段来保证调用的正确性，否则就有可能用线程安全类写出不安全的代码

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 */
private val vector = Vector<Int>()

private val threadNum = 5

fun main() {
    val addThreadList = mutableListOf<Thread>()
    for (i in 1..threadNum) {
        val thread = object : Thread() {
            override fun run() {
                for (item in 1..10) {
                    vector.add(item)
                }
            }
        }
        addThreadList.add(thread)
    }
    val printThreadList = mutableListOf<Thread>()
    for (i in 1..threadNum) {
        val thread = object : Thread() {
            override fun run() {
                for (index in 1..vector.size) {
                    vector.removeAt(i)
                }
            }
        }
        printThreadList.add(thread)
    }
    addThreadList.forEach {
        it.start()
    }
    printThreadList.forEach {
        it.start()
    }
    addThreadList.forEach {
        it.join()
    }
    printThreadList.forEach {
        it.join()
    }
}
```

## 4、上下文切换

并发的实现和是否拥有多个处理器无关，即使只有单个处理器也能够通过处理器**时间片分配**技术来实现并发。操作系统通过给每个线程分配一小段占有处理器使用权的时间来供其运行，然后在每个线程的运行时间结束后又快速切换到下一个线程来运行，多个线程以这种断断续续的方式来实现并发并完成各自的任务。一个线程被剥夺处理器的使用权并暂停运行的过程就被称为切出，被线程调度器选中来占用处理器并运行的过程就被称为切入

操作系统会分出一个个时间片，每个线程每次运行会分配到若干个时间片，时间片决定了一个线程可以连续占用处理器运行的时间长度，一般是只有几十毫秒，单处理器上的多线程就是通过这种**时间片分配**的方式来实现并发。当一个进程中的一个线程由于其时间片用完或者由于其自身的原因被迫或者主动暂停其运行时，另外一个线程（当前进程中的线程或者其它进程中的线程）就可以被线程调度器选中来占用处理器并开始运行。这种一个线程被剥夺处理器的使用权并暂停运行，另外一个线程被赋予处理器的使用权并开始运行的过程就称为线程上下文切换

线程上下文切换是**处理器个数远小于系统所需要支持的并发线程数**的现实场景下的必然产物。这也意味着在线程切出和切入的时候操作系统需要保存和恢复相应线程的进度信息，即需要保存切入和切出那一刻相应线程所执行的指令进行到什么哪一步了。这个进度信息就被称为上下文

线程的生命周期状态在 RUNNABLE 状态与非 RUNNABLE 状态之间切换的过程就是上下文切换的过程。当被暂停的线程被操作系统选中获得继续运行的机会时，操作系统会恢复之前为该线程保存的上下文，以便其在此基础上继续完成其任务

按照导致上下文切换的因素来划分，可以将上下文切换划分为**自发性上下文切换**和**非自发性上下文切换**

- 自发性上下文切换。这种情况是线程由于其自身原因导致的切出。从 Java 平台的角度来看，一个线程在其运行过程中执行了以下任何一个操作都会引起自发性上下文切换
  - Thread.sleep()
  - Object.wait()
  - Thread.yieid()
  - Thread.join()
  - LockSupport.park()
  - I/O 操作
  - 等待被其它线程持有的锁
- 非自发性上下文切换。指线程由于线程调度器的原因被迫切出。这种情况往往是由于被切出的线程的时间片用完，或者有一个比被切出线程更高优先级的线程需要运行。此外，Java 虚拟机的垃圾回收动作也可能导致非自发性上下文切换，这是因为垃圾回收器在执行 GC 的过程中可能需要暂停所有应用线程才能完成

系统在一段时间内产生的上下文切换次数越多，由此导致的处理器资源消耗也就越多，相应的这段时间内真正能够用于执行目标代码的处理器资源就越少，因此我们也需要考虑尽量减少上下文切换的次数，这在后续文章中会介绍

# 五、线程调度

线程调度是指操作系统为线程分配处理器使用权的过程。主要的调度方式有两种：

- 协同式线程调度。在这种策略下，线程的执行时机由线程本身来决定，线程通过主动通知系统切换到另一个线程的方式来让出处理器的使用权。该策略的优点是实现简单，可以通过精准控制线程的执行顺序来避免线程安全性问题。缺点是可能会由于单个线程的代码缺陷问题导致无法切换到下一个线程，最终导致进程被阻塞
- 抢占式线程调度。这也是 Java 平台使用的线程调度策略。在这种策略下，由操作系统来决定当前处理器时间片交由哪个线程来使用，线程无法决定具体的运行时机和运行顺序。虽然我们可以通过 `Thread.yieid()` 方法来让出时间片，但是无法主动抢夺时间片，且虽然 Thread 类也提供了设置线程优先级的方法，但线程的具体执行顺序还是取决于其运行系统。该策略的优点是不会由于一个线程的问题导致整个进程被阻塞，且提高了并发性。缺点是实现较为复杂，且会带来多线程安全性问题

# 六、资源争用和资源调度

## 1、资源争用

一次只能被一个线程占用的资源称为排他性资源。常见的排他性资源包括**锁、处理器、文件**等。由于资源的稀缺性或者资源本身的特性，我们往往需要在多个线程间共享同一个排他性资源。当一个线程占用一个排他性资源而未释放其对资源的所有权时，存在其它线程同时试图访问该资源的现象就被称为**资源争用**，简称争用。显然，争用是在并发环境下产生的一种现象，同时试图访问一个已经被其它线程占用的排他性资源的线程数量越多，争用的程度就越高，反之争用的程度就越低。相应的争用就被称为**高争用和低争用**

同一时间段内，处于运行状态的线程数量越多，我们就称并发的程度越高，简称高并发。虽然高并发加大了争用的可能性，但是高并发未必就意味着高争用，因为线程并非就是一定会在某个时刻来一起申请资源，资源的申请操作对于多个线程来说可能是交错开的，或者每个线程持有排他性资源的时间很短。多线程编程的理想情况就是高并发、低争用

## 2、资源调度

当多个线程同时申请同一个排他性资源，申请资源失败的线程往往是会存入一个等待队列中，当后续资源被其持有线程释放时，如果刚好有一个活跃线程来申请资源，此时选择哪一个线程来获取资源的独占权就是一个资源调度的过程，资源调度策略的一个重要属性就是能否**保证公平性**。所谓公平性，是指资源的申请者是否严格按照申请顺序而被授予资源的独占权。如果资源的任何一个先申请者总是能够被比任何一个后申请者先获得资源的独占权，那么该策略就被称为**公平调度策略**。如果资源的后申请者可能比先申请者先获得资源的独占权，那么该策略就被称为**非公平调度策略**。注意，非公平调度策略往往只是不保证资源调度的公平性，即它只是允许不公平的资源调度现象，而不是表示它刻意造就不公平的资源调度

公平的资源调度策略不允许插队现象的出现，资源申请者总是按照先来后到的顺序获得资源的独占权。如果当前等待队列为空，则来申请资源的线程可以直接获得资源的独占权。如果等待队列不为空，那么每个新到来的线程就被插入等待队列的队尾。公平的资源调度策略的优点是：每个资源申请者从开始申请资源到获得相应资源的独占权所需时间的偏差会比较小，即每个申请者成功申请到资源所需的时间基本相同，且可以避免出现线程饥饿现象。缺点是吞吐率较低，为了保证 FIFO 加大了发生线程上下文切换的可能性

非公平的资源调度策略则允许插队现象。新到来的线程会直接尝试申请资源，只有当申请失败时才会将线程插入等待队列的队尾。假设两种多个线程一起竞争同一个排他性资源的场景：

1. 当资源被释放时，如果刚好有一个活跃线程来申请资源，该线程就可以直接抢占到资源，而无需去唤醒等待队列中的线程。这种场景相对公平调度策略就少了**将新到来的线程暂停**和**将等待队列队头的线程唤醒**的两个操作，而资源也一样有被得到使用
2. 即使等待队列中的某个线程已经被唤醒来试图抢占资源的独占权，如果新到来的活跃线程占用资源的时间不长的话，那么就有可能在被唤醒的线程开始申请资源之前，新到来的活跃线程已经释放了对资源的独占权，从而不妨碍被唤醒的线程申请资源。这种场景也一样避免了**将新到来的线程暂停**这么一个操作

因此，非公平调度策略的优点主要有两点：

1. 吞吐率一般来说会比公平调度策略高，即单位时间内它可以为更多的申请者调配资源
2. 降低了发生上下文切换的概率

非公平调度策略的缺点主要有两点：

1. 由于允许插队现象，极端情况下可能导致等待队列中的线程永远也无法获得其所需的资源，即出现**线程饥饿**的活性故障现象
2. 每个资源申请者从开始申请资源到获得相应资源的独占权所需时间的偏差可能较大，即有的线程可能很快就能申请到资源，而有的线程则要经历若干次暂停与唤醒才能成功申请到资源

综上所诉，公平调度策略适用于资源被持有的时间较长或者线程申请资源的平均时间间隔较长的情形，或者要求申请资源所需的时间偏差较小的情况。总的来说，使用公平调度策略的开销会比使用非公平调度策略的开销要大，因此在没有特别需求的情况下，应该默认使用非公平调度策略

# 七、参考的书籍

《Java 多线程编程实战指南（核心篇）》

《深入理解 Java 虚拟机》

《Java 并发编程的艺术》