> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

> 目前，多线程编程可以说是在大部分平台和应用上都需要实现的一个基本需求。本系列文章就来对 **Java 平台下的多线程编程知识**进行讲解，从**概念入门**、**底层实现**到**上层应用**都会涉及到，预计一共会有五篇文章，希望对你有所帮助 😎😎
>
> 本篇文章是第二篇，介绍实现多线程同步的各类方案，涉及多种多线程同步机制，是开发者在语言层面上对多线程运行所做的规则设定

# 一、线程同步机制

前面的文章有介绍到，多线程安全问题概括来说表现为三个方面：**原子性、可见性、有序性**。多线程安全问题的产生前提是存在多个线程并发访问（不全是读）同一份共享数据，而会产生多线程安全问题的根本原因是**多个线程间缺少一套用于协调各个线程间的数据访问和行为交互的机制，即缺少线程同步机制**

多线程为程序引入了异步行为，相应的就必须提供一种线程同步机制来保障在需要时能够强制多线程同步的方法。当多个线程间存在共享资源时，需要以某种方式来确保每次只有一个线程能够使用资源。例如，如果希望两个线程进行通信并共享某个复杂的数据结构（例如链表），就需要以某种方式来确保它们相互之间不会发生冲突。也就是说，当一个线程正在读取该数据结构时，必须阻止另外一个线程向该数据结构写入数据

Java 为同步提供了语言级的支持，同步的关键是**监视器**，监视器是用作互斥锁的对象。在给定时刻，只有一个线程可以拥有监视器。当线程取得锁时，也就是进入了监视器。其它所有企图进入加锁监视器的线程都会被挂起，直到持有监视器的线程退出监视器

从广义上来说，Java 平台提供的线程同步机制包括：**锁、volatile、final、static 以及一些 API（Object.wait()、Object.notify() 等）**

# 二、锁的分类

既然线程安全问题的产生前提是存在多个线程并发访问（不全是读）共享数据，那么为了保障线程安全，我们就可以通过将多个线程对共享数据的并发访问转换为串行访问，从而来避免线程安全问题。将多个线程对共享数据的访问限制为串行访问，即限制共享数据一次只能被一个线程访问，该线程访问结束后其它线程才能对其进行访问

Java 就是通过这种思路提供了**锁(Lock)** 这种线程同步机制来保障线程安全。锁具有排他性，一次只能被一个线程持有（这里所说的锁不包含读写锁这类共享锁），这种锁就被称为排他锁或者互斥锁。锁的持有线程可以对锁保护的共享数据进行访问，访问结束后持有线程就必须释放锁，以便其它线程能够后续对共享数据进行访问。锁的持有线程在其获得锁之后和释放锁之前这段时间内所执行的代码被称为临界区。因此，临界区一次只能被一个线程执行，共享数据只允许在临界区内进行访问

按照 Java 虚拟机对锁的实现方式的划分，Java 平台中的锁包括**内部锁**和**显式锁**。内部锁是通过 `synchronize` 关键字实现的。显式锁是通过 `java.util.concurrent.locks.Lock` 接口的实现类来实现的。内部锁仅支持非公平调度策略，显式锁既支持公平调度策略也支持非公平调度策略

锁能够保护共享数据以实现线程安全，起的作用包括保障原子性、保障可见性和保障有序性

- 锁通过互斥来保障原子性。锁保证了临界区代码一次只能被一个线程执行，临界区代码被执行期间其它线程无法访问相应的共享数据，从而排除了多个线程同时访问共享变量从而导致竞态的可能性，这使得临界区所执行的操作具备了原子性。虽然实现并发是多线程编程的目标，但是这种并发往往是带有局部串行
- 可见性的保障是通过写线程冲刷处理器缓存和读线程刷新处理器缓存这两个动作实现的。在 Java 平台。锁的获得隐含着刷新处理器缓存这个动作，这使得读线程在获得锁之后且执行临界区代码之前，可以将写线程对共享变量所做的更新同步到该线程执行处理器的高速缓存中；而锁的释放隐含着冲刷处理器缓存这个动作，这使得写线程对共享变量所做的更新能够被推送到该线程执行处理器的高速缓存中，从而对读线程可见。因此，锁能够保障可见性
- 锁能够保障有序性。由于锁对原子性和可见性的保障，使得锁的持有线程对临界区内对各个共享数据的更新同时对外部线程可见，相当于临界区中执行的一系列操作在外部线程看来就是完全按照源代码顺序执行的，即外部线程对这些操作的感知顺序与源代码顺序一致，所以说锁保障了临界区的有序性。尽管锁能够保障有序性，但临界区内依然可能存在重排序，但临界区代码不会被重排序到临界区之外，而临界区之外的代码有可能被重排序到临界区之内

锁的原子性及对可见性的保障合在一起，可保障临界区内的代码能够读取到共享数据的相对新值。再由于锁的互斥性，同一个锁所保护的共享数据一次只能被一个线程访问，因此线程在临界区中所读取到的共享数据的相对新值同时也是最新值

需要注意的是，锁对可见性、原子性和有序性的保障是有条件的，需要同时满足以下两个条件，否则就还是会存在线程安全问题

- 多个线程在访问同一组共享数据的时候必须使用同一个锁
- 即使是对共享数据进行只读操作，其执行线程也必须持有相应的锁

之所以需要保障以上两个要求，是由于一旦某个线程进入了一个锁句柄引导的**同步方法/同步代码块**，其它线程就都无法再进入同个锁句柄引导的任何**同步方法/同步代码块**，但是仍然可以继续调用其它**非同步方法/非同步代码块**，而如果**非同步方法/非同步代码块**也对共享数据进行了访问，那么此时依然会存在竞态

## 1、内部锁

Java 平台中的任何一个对象都有一个唯一与之关联的锁，被称为**监视器**或者**内部锁**。内部锁是通过关键字 `synchronize` 实现的，可用来修饰实例方法、静态方法、代码块等

`synchronize` 关键字修饰的方法就被称为同步方法，同步方法的整个方法体就是一个临界区。用 `synchronize` 修饰的实例方法和静态方法就分别称为同步实例方法和同步静态方法

```java
public class Test {

    //同步静态方法
    public synchronized static void funName1() {

    }

    //同步方法
    public synchronized void funName2() {

    }

}
```

`synchronize` 关键字修饰的代码块就被称为同步块。当中，`lock`被称为锁句柄。锁句柄是对一个对象的引用，锁句柄对应的监视器就称为相应同步块的引导锁

```java
public class Test {

    private final Object lock = new Object();

    public void funName1() {
        //同步块
        synchronized (lock) {

        }
    }

}
```

锁句柄如果为当前对象（this），那就相当于同步实例方法，如下两个同步方法是等价的

```java
public class Test {
    
    public void funName1() {
        synchronized (this) {

        }
    }

    public synchronized void funName2() {

    }

}
```

同步静态方法则相当于以当前类对象为引导锁的同步块，如下两个同步方法是等价的

```java
public class Test {
    
    public synchronized static void funName1() {

    }
    
    public void funName2() {
        synchronized (Test.class) {
    
        }
    }

}
```

作为锁句柄的变量通常使用 `private final`修饰，这是因为锁句柄的变量一旦被改变，会导致执行同一个同步块的多个线程实际上使用不同的锁，从而导致竞态

对于内部锁来说，线程在执行临界区内代码之前必须获得该临界区的引导锁，执行完后就会自动释放引导锁，引导锁的申请和释放是由 Java 虚拟机代为执行的，这也是 `synchronized`被称为内部锁的原因。且由于 Java 编译器对同步块代码的特殊处理，即使临界区抛出异常，内部锁也会被自动释放，所以内部锁不会导致锁泄漏

## 2、显式锁

显式锁从 JDK 1.5 开始被引入 ，其作用与内部锁相同，但相比内部锁其功能会丰富很多。显式锁由 `java.concurrent.locks.Lock` 接口来定义，默认实现类是 `java.util.concurrent.locks.ReentrantLock`

Lock 的使用方式较为灵活，可以在方法 A 内申请锁，在方法 B 再进行释放。其基本使用方式如下所示

```java
private Lock lock = new ReentrantLock(false);

private void funName() {
    //申请锁
    lock.lock();
    try {
        //action
    } finally {
        //释放锁
        lock.unlock();
    }
}
```

ReentrantLock 既支持公平调度策略也支持非公平调度策略，通过其一个参数的构造函数来指定，传值为 true 表示公平锁，false 表示非公平锁， 默认使用非公平调度策略。此外，由于虚拟机并不会自动为我们释放锁，所以为了避免锁泄漏，一般会将 `Lock.unlock()`方法放在 `finally` 中执行，以保证临界区内的代码不管是正常结束还是异常退出，相应的锁释放操作都会被执行

## 3、内部锁和显式锁的比较

内部锁是基于代码块的锁

其缺点主要有以下几点：

1. 使用上缺少灵活性。锁的申请和释放操作被限制在一个代码块或者方法体内部
2. 功能有限。例如，当一个线程申请某个正被其它线程持有的内部锁时，该线程只能被暂停，等待锁被释放后再次申请，而无法取消申请或者是限时申请，且不支持线程中断
3. 仅支持非公平调度策略

其优点主要有以下几点：

1. 使用简单
2. 由于 Java 编译器的保障，所以使用时不会造成锁泄露，保障了安全性

------

显式锁是基于对象的锁

其缺点主要有以下几点：

1. 需要开发者自己来保障不会发生锁泄露

其优点主要有以下几点：

1. 相对内部锁在使用上更具灵活性，可以跨方法来完成锁的申请和释放操作
2. 功能相对内部锁要丰富许多。例如，可以通过 `Lock.isLocked()`判断当前线程是否已经持有该锁、通过 `Lock.tryLock()` 尝试申请锁以避免由于锁被其它线程持有而导致当前线程被暂停、通过 `Lock.tryLock(long,TimeUnit)` 在指定时间范围内尝试申请锁、`Lock.lockInterruptibly()` 支持线程中断
3. 同时支持公平调度策略和非公平调度策略

## 4、读写锁

锁的排他性使得多个线程无法以线程安全的方式在同一时刻对共享数据进行**只读取而不更新**的操作，这在**共享数据读取频繁但更新频率较低**的情况下降低了系统的并发性，读写锁就是为了应对这种问题而诞生的。读写锁（Read/Wirte Lock）是一种改进型的排他锁，也被称为**共享/排他锁**。读写锁允许多个线程同时读取共享变量，但是一次只允许一个线程对共享变量进行更新。任何线程读取共享变量的时候，其它线程无法更新这些变量；一个线程更新共享变量的时候，其它线程都无法读取和更新这些变量

Java 平台的读写锁由 `java.util.concurrent.locks.ReadWriteLock` 接口来定义，其默认实现类是 `java.util.concurrent.locks.ReentrantReadWriteLock`

`ReadWriteLock` 接口定义了两个方法，分别用来获取**读锁**（ReadLock）和**写锁**（WriteLock）

```java
public interface ReadWriteLock {
    /**
     * Returns the lock used for reading.
     *
     * @return the lock used for reading
     */
    Lock readLock();

    /**
     * Returns the lock used for writing.
     *
     * @return the lock used for writing
     */
    Lock writeLock();
}
```

读线程在访问共享变量的时候必须持有读锁，读锁是可以共享的，它可以同时被多个线程持有，提高了只读操作的并发性。写线程在访问共享变量的时候必须持有写锁，写锁是排他的，即一个线程持有写锁的时候其它线程无法获得同个读写锁的读锁和写锁

读写锁的使用方式与显式锁相似，也需要由开发者自己来保障避免锁泄露

```java
public class Test {

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private final Lock readLock = readWriteLock.readLock();

    private final Lock writeLock = readWriteLock.writeLock();

    public void reader() {
        readLock.lock();
        try {
            //在此区域读取共享变量
        } finally {
            readLock.unlock();
        }
    }

    public void writer() {
        writeLock.lock();
        try {
            //在此区域更新共享变量
        } finally {
            writeLock.unlock();
        }
    }

}
```

读写锁在原子性、可见性和有序性保障方面，它所起的作用和普通的排他锁是一样的，但由于读写锁内部实现比内部锁和其它显式锁要复杂很多，因此读写锁适合于在以下条件同时得以满足的场景下使用：

- 只读操作比写操作要频繁得多
- 读线程持有锁的时间比较长

只有同时满足以上两个条件的时候读写锁才是比较适合的，否则可能反而会比普通排他锁增大性能开销

此外，ReentrantReadWriteLock 支持锁的降级，即一个线程持有写锁的同时可以继续获得相应的读锁。但 ReentrantReadWriteLock 不支持锁的升级，即无法在持有读锁的同时获得相应的写锁

## 5、内部锁和读写锁的性能比较

这里，我们以一个简单的例子来比较下内部锁和读写锁之间的性能差异。假设存在数量相等的读线程和写线程，读线程负责打印出共享变量整数值 index 的当前值大小，写线程负责对共享变量整数值 index 进行递增加一。读线程和写线程各自有多个，每个线程间的行为是互相独立的。这里分别通过使用“内部锁”和“读写锁”来规范每个线程的行为必须是串行的，通过比较不同方式下所需的时间耗时来对比两种锁之间的性能高低

首先，`Printer` 接口定义了读线程和写线程需要做的行为操作，`ReadWriteLockPrinter` 类是读写锁方式的实现，`SynchronizedPrinter` 类是内部锁方式的实现

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 */
interface Printer {

    fun read()

    fun write()

    fun sleep() {
        Thread.sleep(200)
    }

}

/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 */
class ReadWriteLockPrinter : Printer {

    private val readWriteLock = ReentrantReadWriteLock(true)

    private val readLock = readWriteLock.readLock()

    private val writeLock = readWriteLock.writeLock()

    private var index = 0

    override fun read() {
        readLock.lock()
        try {
            sleep()
        } finally {
            println("读取到数据： $index" + "，time: " + System.currentTimeMillis())
            readLock.unlock()
        }
    }

    override fun write() {
        writeLock.lock()
        try {
            sleep()
            index++
        } finally {
            println("写入数据： $index" + "，time: " + System.currentTimeMillis())
            writeLock.unlock()
        }
    }
}

/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 */
class SynchronizedPrinter : Printer {

    private var index = 0

    @Synchronized
    override fun read() {
        sleep()
        println("读取到数据： $index" + "，time: " + System.currentTimeMillis())
    }

    @Synchronized
    override fun write() {
        sleep()
        index++
        println("写入数据： $index" + "，time: " + System.currentTimeMillis())
    }

}
```

再来定义读线程和写线程，两种线程使用的是同个 Printer 对象

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 */
class PrinterReadThread(private val printer: Printer) : Thread() {

    override fun run() {
        printer.read()
    }

}

class PrinterWriteThread(private val printer: Printer) : Thread() {

    override fun run() {
        printer.write()
    }

}
```

通过切换不同的 Printer 实现即可大致对比不同的锁的性能高低

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 */
fun main() {
    val printer: Printer = SynchronizedPrinter()
	//val printer: Printer = ReadWriteLockPrinter()

    val threadNum = 10

    val writeThreadList = mutableListOf<Thread>()
    for (i in 1..threadNum) {
        writeThreadList.add(PrinterWriteThread(printer))
    }

    val readThreadList = mutableListOf<Thread>()
    for (i in 1..threadNum) {
        readThreadList.add(PrinterReadThread(printer))
    }

    //启动所有读线程和所有写线程
    writeThreadList.forEach {
        it.start()
    }
    readThreadList.forEach {
        it.start()
    }

}
```

最后的日志输出类似如下所示。虽然即使多次运行来取平均值也不具备严格的对比意义，但是也可以大致对比出不同锁之间的性能高低。从日志也可以看出，当使用读写锁时多个读线程读取数据所需要的总耗时几乎是零

```kotlin
# 内部锁 消耗 3801 毫秒 
写入数据： 1，time: 1597151018862
读取到数据： 1，time: 1597151019062
读取到数据： 1，time: 1597151019262
读取到数据： 1，time: 1597151019462
读取到数据： 1，time: 1597151019662
读取到数据： 1，time: 1597151019862
读取到数据： 1，time: 1597151020062
读取到数据： 1，time: 1597151020262
读取到数据： 1，time: 1597151020462
读取到数据： 1，time: 1597151020662
读取到数据： 1，time: 1597151020862
写入数据： 2，time: 1597151021062
写入数据： 3，time: 1597151021262
写入数据： 4，time: 1597151021462
写入数据： 5，time: 1597151021663
写入数据： 6，time: 1597151021863
写入数据： 7，time: 1597151022063
写入数据： 8，time: 1597151022263
写入数据： 9，time: 1597151022463
写入数据： 10，time: 1597151022663


# 读写锁 消耗 2000 毫秒 
写入数据： 1，time: 1597151078704
写入数据： 2，time: 1597151078904
写入数据： 3，time: 1597151079104
写入数据： 4，time: 1597151079304
写入数据： 5，time: 1597151079504
写入数据： 6，time: 1597151079704
写入数据： 7，time: 1597151079904
写入数据： 8，time: 1597151080104
写入数据： 9，time: 1597151080304
写入数据： 10，time: 1597151080504
读取到数据： 10，time: 1597151080704
读取到数据： 10，time: 1597151080704
读取到数据： 10，time: 1597151080704
读取到数据： 10，time: 1597151080704
读取到数据： 10，time: 1597151080704
读取到数据： 10，time: 1597151080704
读取到数据： 10，time: 1597151080704
读取到数据： 10，time: 1597151080704
读取到数据： 10，time: 1597151080704
读取到数据： 10，time: 1597151080704
```

## 6、锁的开销

锁的开销主要包含几点：

1. 上下文切换与线程调度开销。一个线程在申请已经被其它线程持有的锁时，该线程就有可能会被暂停运行，直到锁被释放后被该线程申请到，也有可能不会被暂停运行，而是采用忙等策略直到锁被释放。如果申请锁的线程被暂停，Java 虚拟机就需要为被暂停的线程维护一个等待队列，以便后续锁的持有线程释放锁时将这些线程唤醒。线程的暂停与唤醒就是一个上下文切换的过程，并且 Java 虚拟机维护等待队列也是有着一定消耗。如果是非争用锁则不会产生上下文切换和等待队列的开销
2. 内存同步、编译器优化受限的开销。锁的底层实现需要使用到内存屏障，而内部屏障会产生直接和间接的开销。直接开销是内存屏障所的冲刷写处理器、清空无效化队列等行为所导致的开销。间接开销包含：禁止部分代码重排序从而阻碍编译器优化。无论是争用锁还是非争用锁都会产生这部分开销，但如果非争用锁最终可以被采用**锁消除**技术进行优化的话，那么就可以消除掉这个锁带来的开销
3. 限制可伸缩性。采用锁的目的是使得多个线程间的**并发改为带有局部串行的并发**，实现这个目的后带来的副作用就是使得系统的局部计算行为（同步代码块）的吞吐率降低，限制系统的可伸缩性，导致处理器资源的浪费

# 三、wait / notify

在单线程编程中，如果程序要执行的操作需要满足一定的运行条件后才可以执行，那么我们可以将目标操作放到一个 if 语句中，让目标操作只有在运行条件得以满足时才会被执行

而在多线程编程中，目标操作的运行条件可能涉及到多个线程间的共享变量，即运行条件可能是由多个线程来共同决定的。对于目标操作的执行线程来说，运行条件可能只是暂时未满足的，其它线程可能在稍后就会更新运行条件涉及的共享变量从而使得运行条件成立。因此，我们可以选择将当前线程暂停，等待其它线程更新了共享变量使得运行条件成立后，再由其它线程来将被暂停的线程唤醒以便让其执行目标操作

当中，一个线程因为要执行的目标动作所需的保护条件未满足而被暂停的过程就被称为等待（wait）。一个线程更新了共享变量，使得其它线程所需的保护条件得以满足并唤醒那些被暂停的线程的过程就被称为通知（notify）

在 Java 平台上，以下两类方法可用于实现等待和通知，Object 可以是任何对象。由于等待线程和通知线程在实现等待和通知的时候必须是调用同一个对象的 wait、notify 方法，且其执行线程必须持有该对象的内部锁，所以等待线程和通知线程是同步在同一个对象上的两种线程

- Object.wait()/Object.wait(long)。这两个方法的作用是使其执行线程暂停，生命周期变为 WAITING，可用于实现等待。其执行线程就被称为等待线程
- Object.notify()/Object.notifyAll()。这两个方法的作用是唤醒一个或多个被暂停的线程，可用于实现通知。其执行线程就被称为通知线程

## 1、wait

使用 `Object.wait()` 实现等待，其代码模板如以下伪代码所示：

```kotlin
//在调用 wait 方法前获得相应对象的内部锁
synchronized(someObject){
    while (保护条件不成立) {
        //调用 wait 方法暂停当前线程，并同时释放已持有的锁
        someObject.wait()
    }
    //能执行到这里说明保护条件已经满足
    //执行目标动作
    doAction()
}
```

当中，**保护条件**是一个包含共享变量的布尔表达式

当保护条件不成立时，因执行 `someObject.wait()` 而被暂停的线程就被称为对象 someObject 上的等待线程。由于一个对象的 `wait()` 方法可以被多个线程执行，因此一个对象可能存在多个等待线程。此外，由于一个线程只有在持有一个对象的内部锁的情况下才能够调用该对象的 `wait()` 方法，因此 `Object.wait()` 总是放在相应对象所引导的临界区之中。`someObject.wait()` 会以原子操作的方式使其执行线程（即等待线程）暂停并使该线程释放其持有的 someObject 对应的内部锁。当等待线程被暂停的时候其对 `someObject.wait()` 方法的调用并不会返回，只有当等待线程被通知线程唤醒且重新申请到 someObject 对应的内部锁时，才会继续执行 `someObject.wait()` 内部剩余的指令，这时 `wait()` 才会返回

当等待线程被唤醒时，等待线程在其被唤醒继续运行到其再次申请到相应对象的内部锁的这段时间内，其它线程有可能会抢先获得相应的内部锁并更新了相关共享变量导致保护条件再次不成立，因此 `someObject.wait()` 调用返回之后我们需要再次判断此时保护条件是否成立。所以，对保护条件的判断以及 `someObject.wait()` 的调用应该放在循环语句之中，以确保目标动作一定只在保护条件成立的情况下才会被执行

此外，等待线程对保护条件的判断以及目标动作的执行必须是原子操作，否则可能产生竞态，即目标动作被执行前的那一刻其它线程可能对共享变量进行了更新又使得保护条件重新不成立。因此，**保护条件的判断、目标动作的执行、Object.wait() 的调用都必须放在同一个对象所引导的临界区中**

## 2、notify

使用 `Object.notify()` 实现通知，其代码模板如以下伪代码所示：

```kotlin
synchronized(someObject){
    //更新等待线程的保护条件涉及的共享变量
    updateSharedState()
    //唤醒等待线程
    someObject.notify()
}
```

由于只有在持有一个对象的内部锁的情况下才能够执行该对象的 `notify()` 方法，所以 `Object.notify()` 方法也总是放在相应对象内部锁所引导的临界区之内。也正因为如此， `Object.wait()` 在暂停其执行线程的同时也必须释放 Object 的内部锁，否则通知线程就永远也无法来唤醒等待线程。和 `Object.wait()` 不同，`Object.notify()` 方法本身并不会释放内部锁，只有在其所在的临界区代码执行结束后才会被释放。因此，为了使得等待线程在被唤醒后能够尽快获得相应的内部锁，我们要尽量将 `Object.notify()` 代码放在靠近临界区结束的地方，否则如果 `Object.notify()`唤醒了等待线程而通知线程又迟迟不释放内部锁，就有可能导致等待线程再次经历上下文切换，从而浪费系统资源

调用 `Object.notify()` 所唤醒的线程仅是 Object 对象上的任意一个等待线程，所以被唤醒的线程有可能并不是我们真正想要唤醒的线程。因此，有时我们需要改用 `Object.notifyAll()` 方法，该方法可以唤醒 Object 上的所有等待线程。被唤醒的线程就都有了抢夺相应 Object 对象的内部锁的机会。而如果被唤醒的线程在占用处理器继续运行后且申请到内部锁之前，有其它线程（被唤醒的等待线程之一或者是新到来的线程）先持有了内部锁，那么这个被唤醒的线程可能又会再次被暂停，等待再次被唤醒的机会，而这个过程会导致上下文切换

wait/notify 机制也被应用于 Thread 类内部。例如，`Thread.join()` 方法提供了在某个线程运行结束前暂停该方法调用者线程的功能，内部也使用到 `wait()` 方法来暂停调用者线程，等到线程终止后 JVM 内部就会通过 `notifyAll()`方法来唤醒所有等待线程

```java
public final synchronized void join(long millis) throws InterruptedException {
    ···
    if (millis == 0) {
        while (isAlive()) {
            wait(0);
        }
    } else {
        ···
    }
}
```

## 3、wait / notify 存在的问题

用 wait / notify 实现的等待和通知可能会遇到以下两个问题：

1. 过早唤醒。假设存在多个等待线程同步在对象 someObject 上，每个等待线程的运行保护条件并不完成相同。当通知线程更新了某个等待线程的运行保护条件涉及的共享变量并使之成立时，由于 `someObject.notify()` 方法具体会唤醒哪个线程对于开发者来说是不可预知的，所以我们只能使用 `someObject.notifyAll()` 方法，此时就会导致那些运行条件还不成立的等待线程也被唤醒，这种现象就叫做过早唤醒。过早唤醒会使得那些运行条件还不满足的等待线程也被唤醒运行，当这些线程再次判断到当前运行条件不满足时又会再次调用 `someObject.wait()` 方法暂停
2. 多次的线程上下文切换。对于一次完整的 wait 和 notify 过程，等待线程执行 `someObject.wait()` 方法至少会导致等待线程对相应内部锁的两次申请和两次释放，通知线程执行 `someObject.notify()` 方法则会导致通知线程对相应内部锁的一次申请和一次释放。每个线程每次锁的申请与释放操作都对应着一次线程上下文切换

## 4、生产者与消费者

wait 和 notify 两个方法的协作可以通过一个经典的问题来展示：生产者与消费者问题。对于一家商店来说，其能承载的商品最大数是固定的，最多为 MAX_COUNT。存在多个生产者为商店生产商品，生产者生产商品不能使得商店内的商品数量超出 MAX_COUNT。存在多个消费者从商店消费商品，消费者消费过后最低使商店的商品总数变为零。生产者只有当商店内的商品总数小于 MAX_COUNT 时才能继续生产，消费者只有当商店内的商品总数大于零时才能进行消费。因此，当商店内的商品总数小于 MAX_COUNT 时需要通知生产者开始生产，否则需要暂停生产。当商店内的商品总数大于零时需要通知消费者来消费，否则需要暂停消费

上述的生产者与消费者分别对应多个线程，商店就相当于多个线程间的共享数据。生产者线程的运行保护条件是：**当前商品总数不能大于等于 MAX_COUNT**。消费者线程的运行保护条件是：**当前商品总数要大于 0**

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 */
private val LOCK = Object()

private const val MAX_COUNT = 10

class Shop(var goodsCount: Int)

fun main() {
    val shop = Shop(0)
    val producerSize = 4
    val consumerSize = 8

    for (i in 1..producerSize) {
        Producer(shop, "生产者-${i}").apply {
            start()
        }
    }

    for (i in 1..consumerSize) {
        Consumer(shop, "消费者-${i}").apply {
            start()
        }
    }
}

class Producer(private val shop: Shop, name: String) : Thread(name) {

    override fun run() {
        while (true) {
            Tools.randomSleep()
            synchronized(LOCK) {
                while (shop.goodsCount >= MAX_COUNT) {
                    println("${name}-商品总数已达到最大数量，停止生产")
                    LOCK.wait()
                }
                val number = Tools.randomInt(1, MAX_COUNT - shop.goodsCount)
                shop.goodsCount = shop.goodsCount + number
                println("$name  =====  新增了 $number 件商品，当前剩余: ${shop.goodsCount}")
                LOCK.notifyAll()
            }
        }
    }

}

class Consumer(private val shop: Shop, name: String) : Thread(name) {

    override fun run() {
        while (true) {
            Tools.randomSleep()
            synchronized(LOCK) {
                while (shop.goodsCount <= 0) {
                    println("${name}-商品已经被消费光了，停止消费")
                    LOCK.wait()
                }
                val number = Tools.randomInt(1, shop.goodsCount)
                shop.goodsCount = shop.goodsCount - number
                println("$name  ----  消费了 $number 件商品，当前剩余: ${shop.goodsCount}")
                LOCK.notifyAll()
            }
        }
    }

}
```

其运行结果类似于如下所示

```kotlin
生产者-2  =====  新增了 7 件商品，当前剩余: 7
消费者-7  ----  消费了 4 件商品，当前剩余: 3
消费者-7  ----  消费了 2 件商品，当前剩余: 1
消费者-8  ----  消费了 1 件商品，当前剩余: 0
生产者-4  =====  新增了 2 件商品，当前剩余: 2
消费者-2  ----  消费了 1 件商品，当前剩余: 1
生产者-1  =====  新增了 8 件商品，当前剩余: 9
消费者-1  ----  消费了 3 件商品，当前剩余: 6
消费者-1  ----  消费了 2 件商品，当前剩余: 4
消费者-3  ----  消费了 1 件商品，当前剩余: 3
消费者-1  ----  消费了 2 件商品，当前剩余: 1
消费者-5  ----  消费了 1 件商品，当前剩余: 0
消费者-8-商品已经被消费光了，停止消费
消费者-7-商品已经被消费光了，停止消费
生产者-3  =====  新增了 1 件商品，当前剩余: 1
消费者-7  ----  消费了 1 件商品，当前剩余: 0
消费者-8-商品已经被消费光了，停止消费
消费者-2-商品已经被消费光了，停止消费
生产者-2  =====  新增了 3 件商品，当前剩余: 3
消费者-2  ----  消费了 2 件商品，当前剩余: 1
消费者-8  ----  消费了 1 件商品，当前剩余: 0
消费者-6-商品已经被消费光了，停止消费

······
```

# 四、线程同步工具类

## 1、Condition

wait/notify 存在过早唤醒、可能多次线程上下文切换次数、无法区分 `Obejct.wait(long)` 方法在返回时是由于超时还是由于线程被唤醒等一系列问题，可以使用 JDK 1.5 开始引入的 `java.util.concurrent.locks.Condition` 接口来解决这些问题

Condition 接口定义的 await()、singal()、singalAll() 等方法相当于 Object.wait()、Object.notify()、Object.notifyAll()。Object.wait()/notify() 方法要求其执行线程必须持有相应对象的内部锁，类似的，Condition.await()/singal() 也要求其执行线程必须持有创建该 Condition 实例的显式锁

使用 Condition  实现等待和通知，其代码模板如以下伪代码所示。`Lock.newCondition()` 方法的返回值就是一个 Condition 实例。每个 Condition 实例内部都维护了一个用于存储等待线程的队列，`Condition.await()` 方法的执行线程会被暂停并存入等待队列中。`Condition.notify()` 方法会分别使等待队列中的一个线程被唤醒，而用同一个 Lock 创建的其它 Condition 实例中的等待线程并不会收到影响。这就使得我们可以精准唤醒目标线程，避免过早唤醒，减少线程上下文切换的次数

```kotlin
class ConditionDemo {

    private val lock = ReentrantLock()

    private val conditionA = lock.newCondition()

    private val conditionB = lock.newCondition()

    fun waitA() {
        lock.lock()
        try {
            while (运行保护条件A不成立) {
                conditionA.await()
            }
            //执行目标操作
            doAction()
        } finally {
            lock.unlock()
        }
    }

    fun notifyA() {
        lock.lock()
        try {
            //更新等待线程的保护条件涉及的共享变量
            updateSharedStateA()
            //唤醒等待线程
            conditionA.signal()
        } finally {
            lock.unlock()
        }
    }

    fun waitB() {
        lock.lock()
        try {
            while (运行保护条件B不成立) {
                conditionB.await()
            }
            //执行目标操作
            doAction()
        } finally {
            lock.unlock()
        }
    }

    fun notifyB() {
        lock.lock()
        try {
            //更新等待线程的保护条件涉及的共享变量
            updateSharedStateB()
            //唤醒等待线程
            conditionB.signal()
        } finally {
            lock.unlock()
        }
    }

}
```

这里通过设计一个简单的阻塞队列来演示下 Condition 的用法

在 Queue 中，Lock 保障了 put 操作和 take 操作的线程安全性，两个不同的 Condition 实例又保障了 putThread 和 takeThread 在各自运行保护条件成立时，可以只唤醒相应的线程

```kotlin
class Queue<T> constructor(private val size: Int) {

    private val lock = ReentrantLock()

    //当队列已满时，put thread 就成为 notFull 上的等待线程
    private val notFull = lock.newCondition()

    //当队列为空时，take thread 就成为 notEmpty 上的等待线程
    private val notEmpty = lock.newCondition()

    private val items = mutableListOf<T>()

    fun put(x: T) {
        lock.lock()
        try {
            while (items.size == size) {
                println("当前队列已满，暂停 put 操作...")
                notFull.await()
            }
            println("当前队列未满，执行 put 操作...")
            items.add(x)
            //唤醒 TakeThread
            notEmpty.signal()
        } finally {
            lock.unlock()
        }
    }

    fun take(): T {
        lock.lock()
        try {
            while (items.size == 0) {
                println("当前队列为空，暂停 take 操作...")
                notEmpty.await()
            }
            println("当前队列不为空，执行 take 操作...")
            val x = items[0]
            items.removeAt(0)
            //唤醒 PutThread
            notFull.signal()
            return x
        } finally {
            lock.unlock()
        }
    }

}
```

PutThread 负责循环向阻塞队列存入八条数据，TakeThread 负责循环从阻塞队列获取九条数据，TakeThread 随机休眠的时间相对 PutThread 会更长，而阻塞队列的队长为四，那么在程序运行过程中大概率可以看到由于阻塞队列已满从而导致 PutThread 被暂停的现象，且程序运行到最后 TakeThread 会为了获取第九条数据而一直处于等待状态

```kotlin
class PutThread(private val intQueue: Queue<Int>) : Thread() {

    override fun run() {
        for (i in 1..8) {
            sleep(Random.nextLong(1, 50))
            intQueue.put(i)
        }
    }

}

class TakeThread(private val intQueue: Queue<Int>) : Thread() {

    override fun run() {
        for (i in 1..9) {
            sleep(Random.nextLong(10, 100))
            println("TakeThread get value: " + intQueue.take())
        }
    }

}

/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 */
fun main() {
    val intQueue = Queue<Int>(4)
    val putThread = PutThread(intQueue)
    val takeThread = TakeThread(intQueue)
    putThread.start()
    takeThread.start()
}
```

从程序的输出结果可以看到 TakeThread 最终将一直处于等待状态

```kotlin
当前队列未满，执行 put 操作...
当前队列未满，执行 put 操作...
当前队列未满，执行 put 操作...
当前队列不为空，执行 take 操作...
TakeThread get value : 1
当前队列未满，执行 put 操作...
当前队列未满，执行 put 操作...
当前队列已满，暂停 put 操作...
当前队列不为空，执行 take 操作...
TakeThread get value : 2
当前队列未满，执行 put 操作...
当前队列不为空，执行 take 操作...
TakeThread get value : 3
当前队列未满，执行 put 操作...
当前队列不为空，执行 take 操作...
TakeThread get value : 4
当前队列未满，执行 put 操作...
当前队列不为空，执行 take 操作...
TakeThread get value : 5
当前队列不为空，执行 take 操作...
TakeThread get value : 6
当前队列不为空，执行 take 操作...
TakeThread get value : 7
当前队列不为空，执行 take 操作...
TakeThread get value : 8
当前队列为空，暂停 take 操作...
```

## 2、CountDownLatch

有时候会存在某个线程需要等待其它线程完成特定操作后才能继续运行的需求，此时使用 `Object.wait()` 和 `Object.notify()`也可以满足需求，但是使用上会比较繁琐，此时可以考虑通过 CountDownLatch 来实现

CountDownLatch 可用来实现一个或多个线程等待其它线程完成特定操作后才继续运行的功能，这组操作被称为先决条件。CountDownLatch 内部会维护一个用于标记需要等待完成的先决条件的数量的计数器，当每个先决条件完成时，先决条件的执行线程就通过调用 `CountDownLatch.countDown()` 来使计算器减一。而 `CountDownLatch.await()` 就相当于一个受保护方法，其保护条件为“计算器值为零”，当计算器值不为零时，调用了 `CountDownLatch.await()` 方法的执行线程都会被暂停。当所有先决条件都完成时，即当“计算器值为零”的保护条件成立时，CountDownLatch 上的所有等待线程就都会被唤醒，继续运行

当计数器的值达到 0 之后，该计数器的值就不再发生变化，后续继续调用 `CountDownLatch.countDown()` 也不会导致抛出异常，且再次调用 `CountDownLatch.await()` 方法也不会导致线程被暂停。因此，一个 CountDownLatch 实例只能用来实现一次等待和一次通知

来看一个简单的例子。假设在程序启动时需要确保三个基础服务（ServiceA、ServiceB、ServiceC）先被初始化完成，且为了加快初始化速度，每个基础服务均交由一个工作者线程来完成初始化任务。此时就可以通过 CountDownLatch 来保证 main 线程一直处于等待状态直到所有的工作者线程的任务均结束（不管初始化成功还是失败）

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 */
fun main() {
    val serviceManager = ServicesManager()
    serviceManager.startServices()
    println("等待所有 Services 执行完毕")
    val allSuccess = serviceManager.checkState()
    println("执行结果： $allSuccess")
}

class ServicesManager {

    private val countDownLatch = CountDownLatch(3)

    private val serviceList = mutableListOf<AbstractService>()

    init {
        serviceList.add(ServiceA("ServiceA", countDownLatch))
        serviceList.add(ServiceB("ServiceB", countDownLatch))
        serviceList.add(ServiceC("ServiceC", countDownLatch))
    }

    fun startServices() {
        serviceList.forEach {
            it.start()
        }
    }

    fun checkState(): Boolean {
        countDownLatch.await()
        return serviceList.find { !it.checkState() } == null
    }

}

abstract class AbstractService(private val countDownLatch: CountDownLatch) {

    private var success = false

    abstract fun doTask(): Boolean

    fun start() {
        thread {
            try {
                success = doTask()
            } finally {
                countDownLatch.countDown()
            }
        }
    }

    fun checkState(): Boolean {
        return success
    }

}

class ServiceA(private val serviceName: String, countDownLatch: CountDownLatch) : AbstractService(countDownLatch) {

    override fun doTask(): Boolean {
        Thread.sleep(2000)
        println("${serviceName}执行完毕")
        return true
    }

}

class ServiceB(private val serviceName: String, countDownLatch: CountDownLatch) : AbstractService(countDownLatch) {

    override fun doTask(): Boolean {
        Thread.sleep(4000)
        println("${serviceName}执行完毕")
        return true
    }

}

class ServiceC(private val serviceName: String, countDownLatch: CountDownLatch) : AbstractService(countDownLatch) {

    override fun doTask(): Boolean {
        Thread.sleep(3000)
        if (Random.nextBoolean()) {
            throw RuntimeException("$serviceName failed")
        } else {
            println("${serviceName}执行完毕")
        }
        return true
    }

}
```

ServiceC 会随机抛出异常，所以程序的运行结果会分为以下两种可能。且为了保证当任务失败时 main 线程也可以收到唤醒通知，需要确保 `CountDownLatch.countDown()` 是放在 finally 代码块中

```kotlin
# 成功的情况
等待所有 Services 执行完毕
ServiceA执行完毕
ServiceC执行完毕
ServiceB执行完毕
执行结果： true


# 失败的情况
等待所有 Services 执行完毕
ServiceA执行完毕
Exception in thread "Thread-2" java.lang.RuntimeException: ServiceC failed
	at thread.ServiceC.doTask(CountDownLatchTest.kt:93)
	at thread.AbstractService$start$1.invoke(CountDownLatchTest.kt:55)
	at thread.AbstractService$start$1.invoke(CountDownLatchTest.kt:46)
	at kotlin.concurrent.ThreadsKt$thread$thread$1.run(Thread.kt:30)
ServiceB执行完毕
执行结果： false
```

## 3、CyclicBarrier

JDK 1.5 引入了 `java.util.concurrent.CyclicBarrier` 类用于实现多个线程间的相互等待。CyclicBarrier 可用于这么一种场景：假设存在一个集合点，在所有线程均执行到集合点之前，每个执行到指定集合点的线程均会被暂停。当所有线程均执行到指定集合点时，即当最后一个线程执行到集合点时，所有被暂停的线程都会自动被唤醒并继续执行

CyclicBarrier 的字面意思可以理解为：可循环使用的屏障。而集合点就相当于一个“屏障”，除非所有线程都抵达到了屏障，否则每个到达的线程都会被拒之门外（即被暂停运行）。而当最后一个线程到来时，“屏障”就会自动消失，即最后一个到来的线程不会被暂停，而是继续向下执行代码，同时唤醒所有其它之前被暂停的线程

CyclicBarrier 类涉及到的线程数量可以通过其构造参数 **parties** 来指定，`CyclicBarrier.await()` 方法就用于标记当前线程执行到了指定集合点。在功能上 CyclicBarrier 与 CountDownLatch 相似，但 CyclicBarrier 实例是可以重复使用的，在所有线程都被唤醒之后，任何线程再次执行 `CyclicBarrier.await()` 方法又会被暂停，直到最后一个线程也执行了该方法。所以，`CyclicBarrier.await()` 方法既是等待方法也是通知方法，最后一个执行线程就相当于通知线程，其它线程就相当于等待线程，线程的具体类别由其运行时序来动态区分，而非靠调用方法的不同

再来看一个简单的例子。存在三个输出不同字符串内容的 PrintThread 线程，每个线程每输出一次，均需要等待其它线程也输出一次后才能再次输出，但三个线程每次的输出先后顺序可以随意

```kotlin
class PrintThread(private val cyclicBarrier: CyclicBarrier, private val content: String) : Thread() {

    override fun run() {
        while (true) {
            sleep(Random.nextLong(300, 1000))
            println("打印完成：${content}")
            if (cyclicBarrier.parties == cyclicBarrier.numberWaiting + 1) {
                println()
            }
            cyclicBarrier.await()
        }
    }

}

fun main() {
    val threadNum = 3
    val cyclicBarrier = CyclicBarrier(threadNum)
    val threadList = mutableListOf<Thread>()
    for (i in 1..threadNum) {
        threadList.add(PrintThread(cyclicBarrier, "index_$i"))
    }
    threadList.forEach {
        it.start()
    }
}
```

程序的输出结果类似如下所示。每一轮输出的三条数据先后顺序并不固定，但每一轮的内容一定不会重复

```kotlin
打印完成：index_2
打印完成：index_1
打印完成：index_3

打印完成：index_2
打印完成：index_1
打印完成：index_3

打印完成：index_2
打印完成：index_1
打印完成：index_3

打印完成：index_3
打印完成：index_1
打印完成：index_2

打印完成：index_1
打印完成：index_3
打印完成：index_2

...
```

## 4、Semaphore

Semaphore 可用于实现互斥以及流量控制，在某些资源有限的场景下限制可以同时访问资源的最大线程数。例如，假设当前有几十上百个线程需要连接数据库进行数据存取，而数据库支持的最大连接数只有十个，此时就可以通过 Semaphore 来限制线程的最大并发数，当已经有十个线程连接到了数据库时，多余的请求线程就会被暂停

看个简单的例子。对于以下代码，线程池同时发起的请求有三十个，而 Semaphore 限制了最大线程并发数是四个，所以最终的输出结果就是会每隔两秒输出四行内容

```kotlin
fun main() {
    val threadNum = 30
    val threadPool = Executors.newFixedThreadPool(threadNum)
    val semaphore = Semaphore(4)
    for (index in 1..threadNum) {
        threadPool.execute {
            semaphore.acquire()
            try {
                Thread.sleep(2000)
                println("over $index")
            } finally {
                semaphore.release()
            }
        }
    }
}
```

## 5、Exchanger

Exchanger 可用于实现在两个线程之间交换数据的功能。当线程 A 先通过 Exchanger 发起交换数据的请求时，线程 A 会被暂停运行直到线程 B 也发起交换数据的请求，当数据交换完成后，两个线程就会各自继续运行

```kotlin
fun main() {
    val exchanger = Exchanger<String>()
    val threadA = object : Thread() {
        override fun run() {
            sleep(2000)
            val result = exchanger.exchange("A")
            println("Thread A: $result")
        }
    }
    val threadB = object : Thread() {
        override fun run() {
            sleep(2000)
            val result = exchanger.exchange("B")
            println("Thread B: $result")
        }
    }
    threadA.start()
    threadB.start()
}
```

```kotlin
Thread B: A
Thread A: B
```

# 五、ThreadLocal

以上介绍的几个线程同步工具类，目的都是为了在多个线程之间实现一种等待机制，即在线程 A 完成目标行为前，线程 B 能够依靠这些线程同步工具类进行等待，直到线程 A 完成

ThreadLocal 不太一样，ThreadLocal 也是用于多线程环境，但其实现的目的是为了进行数据隔离，为每一个线程维护一个独有的全局变量，从而解决共享变量的并发安全问题

例如，以下代码中 mainThread 可以获取到值但 subThread 获取到的是 null，因为 mainThread 进行了赋值操作而 subThread 没有，每个线程只会获取到自己对 ThreadLocal 的赋值结果

```java
public static void main(String[] args) throws InterruptedException {
    ThreadLocal<String> threadLocal = new ThreadLocal<>();
    threadLocal.set("业志陈");
    Thread subThread = new Thread() {
        @Override
        public void run() {
            System.out.println("subThread :" + threadLocal.get());
        }
    };
    subThread.start();
    subThread.join();
    System.out.println("mainThread: " + threadLocal.get());
}
```

```java
subThread :null
mainThread: 业志陈
```

这里来简单看下 ThreadLocal 的源码实现

ThreadLocal 是一个泛型类，其对外部开放的方法主要就是 `get()、set(T)、remove()、initialValue()` 四个方法

`get()` 方法用于获取 ThreadLocal 保存的值，该方法会根据当前线程获取到一个 ThreadLocalMap，从名字上就可以看出 ThreadLocalMap 具有存储键值对的特性（虽然并没有实现 Map 接口），会以当前 ThreadLocal 对象作为 key 来进行取值。可以看到，该 ThreadLocalMap 就保存在当前线程所代表的 Thread 对象中，即 `threadLocals`，所以不同线程间会维护单独一份数据，从而实现数据隔离

如果 ThreadLocal 还未赋值过，则会调用 `setInitialValue()` 方法来进行初始化，所以我们可以通过重写 `initialValue()` 方法来设置 ThreadLocal 对所有线程的默认初始值

```java
public T get() {
    Thread t = Thread . currentThread ();
    ThreadLocalMap map = getMap (t);
    if (map != null) {
        ThreadLocalMap.Entry e = map . getEntry (this);
        if (e != null) {
            @SuppressWarnings("unchecked")
            T result =(T) e . value;
            return result;
        }
    }
    return setInitialValue();
}

private T setInitialValue() {
    T value = initialValue ();
    Thread t = Thread . currentThread ();
    ThreadLocalMap map = getMap (t);
    if (map != null)
        map.set(this, value);
    else
        createMap(t, value);
    return value;
}

protected T initialValue() {
    return null;
}

ThreadLocalMap getMap(Thread t) {
    return t.threadLocals;
}

void createMap(Thread t, T firstValue) {
    t.threadLocals = new ThreadLocalMap (this, firstValue);
}
```

`set(T)` 和 `remove()`两个方法也是以当前 ThreadLocal 作为 key，对 ThreadLocalMap 进行赋值操作和移除操作

```java
public void set(T value) {
    Thread t = Thread.currentThread();
    ThreadLocalMap map = getMap(t);
    if (map != null)
        map.set(this, value);
    else
        createMap(t, value);
}

 public void remove() {
     ThreadLocalMap m = getMap(Thread.currentThread());
     if (m != null)
         m.remove(this);
 }
```

可以看到，ThreadLocal 的主要实现逻辑还是在于 ThreadLocalMap，每个 Thread 都有自己单独的一个 ThreadLocalMap 对象，ThreadLocal 就以自身作为 key 来存储在 ThreadLocalMap 中，每个 ThreadLocal 就用于为本线程维护一个特定的值，从而使得不同线程间即使是使用同个 ThreadLocal 对象也可以单独维护一个特定的值

```java
public class Thread implements Runnable {
    
    ThreadLocal.ThreadLocalMap threadLocals = null;
    
}
```

由于一个线程可以关联多个 ThreadLocal，所以 ThreadLocalMap 就以数组的形式 `Entry[]` 来存储 ThreadLocal

```java
static class ThreadLocalMap {

    static class Entry extends WeakReference<ThreadLocal<?>> {
        /** The value associated with this ThreadLocal. */
        Object value;

        Entry(ThreadLocal<?> k, Object v) {
            super(k);
            value = v;
        }
    }

    private static final int INITIAL_CAPACITY = 16;

    private Entry[] table;

    private int size = 0;

    ThreadLocalMap(ThreadLocal<?> firstKey, Object firstValue) {
        table = new Entry[INITIAL_CAPACITY];
        int i = firstKey.threadLocalHashCode & (INITIAL_CAPACITY - 1);
        table[i] = new Entry(firstKey, firstValue);
        size = 1;
        setThreshold(INITIAL_CAPACITY);
    }

    ···
}
```

ThreadLocal 和关联的 value 会被包装为一个 Entry 对象，Entry 以弱引用的方式来保存 ThreadLocal。ThreadLocalMap 的一个重要知识点就是考察为什么要以弱引用的方式来保存 ThreadLocal

想象这么一个场景，就能明白为什么不使用强引用的方式

假设创建 ThreadLocal 变量的是线程 A，线程 B 访问了 ThreadLocal 后就会将其保存到自身的 ThreadLocalMap 中。当线程 A 结束运行，由于使用了弱引用，ThreadLocal 在没有外部强引用时一进行 GC 就会被回收，避免了内存泄漏，这种情况就是最理想的了。如果使用强引用，那么就需要等到线程 B 也结束运行后 ThreadLocal 才能被回收，而这可能需要一段比较长的时间，即使在这个过程中线程 B 都没有再次使用 ThreadLocal

但依靠弱引用也无法完全避免内存泄漏问题，因为线程 A 可能是线程池中的某个线程，可以不断被复用，此时即使 ThreadLocal 被回收了，其对应的 value 却会一直被保留着，这就造成了 ThreadLocalMap 会保留着 key 为 null 但 value 不为 null 的 Entry 对象，value 无法被回收，此时就一样会造成内存泄漏。为了解决该问题，就需要在线程 A 不再需要使用到 ThreadLocal 时主动调用 `remove()` 方法移除掉 Entry 对象

# 六、线程中断机制

以上介绍的几种线程间协作方法的使用初衷都是**希望线程完成各自操作后能互相通知**。可是还存在这么一种情况：一个线程请求另外一个线程停止其正在执行的任务。例如，对于一个地图应用来说，当用户退出应用时，就需要停止后台线程正在执行的定位任务，因为此时该任务对于用户来说是不需要的了

一个线程向另一个线程发起请求，希望其停止任务的机制就称为**线程中断机制**。中断（interrupt）是由发起线程向目标线程发送的一种指示，该指示用于表示发起线程希望目标线程停止其正在执行的任务。发起线程的中断请求并不是一个强制性的行为，目标线程可能会在收到中断指示时停止任务，也可能完全不做任何响应，这取决于目标线程对中断请求的处理逻辑

Java 平台会为每个线程维护一个被称为**中断标记**的布尔型变量来表示相应线程是否收到了中断，值为 true 则表示收到了中断请求。Thread 类包含以下几个和中断相关的方法

```kotlin
public class Thread implements Runnable {
    
    //向此线程发起中断请求
    public void interrupt() {
    	...
    }
    
    //在获取中断标记的同时将中断标记置为 false
    public static boolean interrupted() {
        ...
    }

    //获取中断标记
    public boolean isInterrupted() {
        ...
    }
    
}
```

目标线程收到中断请求后所执行的操作，被称为目标线程对中断的响应，简称中断响应。目标线程对中断的响应类型一般包括：

- 无影响。例如，`ReentrantLock.lock()` 或者内部锁申请等操作时，都不会对中断进行响应，即不会停止当前正在执行的操作
- 取消任务的运行。例如，目标线程可以在每次执行任务前均检查中断标记，当中断标记为 true 时则取消当前任务，但还是会继续处理其他任务
- 停止线程。即令目标线程放弃执行所有任务，生命周期状态变更为 TERMINATED

Java 标准库中的许多阻塞方法对中断的响应都是抛出 InterruptedException 等异常。能够响应中断的方法通常是在执行阻塞操作前判断中断标记，若中断标记为 true 则直接抛出 InterruptedException。例如，`ReentrantLock.lockInterruptibly()` 方法会在执行申请锁这个阻塞操作前检查当前线程的中断标记，当中断标记为 true 时则会抛出 InterruptedException。而按照惯例，抛出 InterruptedException 异常的方法一般都会在抛出该异常之前将当前线程的中断标记重置为 false。例如，`ReentrantLock.lockInterruptibly()` 方法会通过在 `acquireInterruptibly()` 方法里调用 `Thread.interrupted()` 来获取并重置中断标记

```kotlin
public final void acquireInterruptibly(int arg) throws InterruptedException {
    if (Thread.interrupted())
        throw new InterruptedException();
    if (!tryAcquire(arg))
        doAcquireInterruptibly(arg);
}
```

如果目标线程在收到中断请求的时候已经由于执行了一些阻塞操作而处于暂停状态，那么 Java 虚拟机可能会将目标线程唤醒，从而使得目标线程被唤醒后继续执行的代码可以再次得到响应中断的机会

# 七、如何实现单例模式

单例模式是 GOF 设计模式中比较容易理解且应用非常广泛的一种设计模式，但是实现一个能够在多线程环境下正常运行且兼顾到性能的单例模式却不是一个简单的事情，这需要我们同时运用到**锁、volatile 变量、原子性、可见性、有序性**等多方面的知识

## 1、单线程环境

在单线程环境下，我们无需考虑原子性、可见性、有序性等问题，所以仅需要做到懒加载即可

```java
public final class Singleton {

    private static Singleton instance = null;

    private Singleton() {

    }

    public static Singleton getInstance() {
        if (instance == null) { //操作1
            instance = new Singleton(); //操作2
        }
        return instance;
    }

}
```

## 2、双重检查锁定

对于上述的在单线程环境下可以正常使用的单例模式，在多线程环境下就很容易出现问题。`getInstance()`方法本身是基于 **check-then-act** 操作来判断是否需要初始化共享变量的，该操作并不是一个原子操作。在 instance 还为 null 时，假设有两个线程 T1 和 T2 同时执行到操作1，接着在 T1 执行操作2之前 T2 已经执行完操作2，在下一时刻，当 T1 执行到操作2的时候，即使 instance 当前已经不为 null，但是 T1 此时依然会多创建一个实例，这就导致了多个实例的创建

首先，我们最先想到的可能是通过加锁来避免这种情况

```java
public static Singleton getInstance() {
    synchronized (Singleton.class) {
        if (instance == null) {
            instance = new Singleton();
        }
    }
    return instance;
}
```

上述方式实现的单例模式固然是线程安全的，但是这也意味着 `getInstance()`方法的任何一个执行线程都需要申请锁，为了避免无谓的锁开销，人们又想到以下这种方法，即双重检查锁定。在执行临界区代码前先判断 instance 是否为 null，如果不为 null ，则直接返回 instance 变量，否则才执行临界区代码来完成 instance 变量的初始化

```java
public static Singleton getInstance() {
    if (instance == null) { //操作1
        synchronized (Singleton.class) {
            if (instance == null) { //操作2
                instance = new Singleton(); //操作3
            }
        }
    }
    return instance;
}
```

上述代码表现出来的初始化逻辑可以分为两种情况，这两种情况的前置前提是：存在两个线程 T1 和 T2 ，线程 T1 执行到了操作1，线程 T2 执行到了临界区

- 当线程 T1 执行到操作1的时候线程 T2 已经执行完了操作3，发现此时 instance 不为 null，直接返回 instance 变量，避免了锁的开销
- 当线程 T1 执行到操作1的时候发现 instance 为 null，此时线程 T2 还处于执行操作3之前，那么当线程 T2 执行临界区结束之前，线程 T1 均会处于等待状态。当线程 T2 执行完毕，线程 T1 进入临界区后，由于此时线程 T1 是在临界区内读取共享变量 instance 的，因此 T1 可以发现此刻 instance 不为 null，于是 T1 不会执行操作3，从而避免了再次创建一个实例

上述代码看起来似乎避免了锁的开销又保障了线程安全，但还是有着一些逻辑缺陷，因为该方法仅考虑到了可见性，而没有考虑到发生重排序的情况

操作3可以分解为以下三条伪指令所代表的子操作

```java
objRef = allocate(Singleton.class) //子操作1，分配对象所需的存储空间
invokeConstructor(objRef) //子操作2，初始化 objRef 引用的对象
instance = objRef //子操作3，将对象引用写入共享变量
```

由于临界区内的代码是有可能被重排序的，因此，JIT 编译器可能将上述的子操作重排序为：子操作1 -> 子操作3 -> 子操作2。即在初始化对象之前将对象的引用写入实例变量 instance。由于锁对有序性的保障是有条件的，而线程 T1 在临界区之外检查 instance 是否为 null 的时候并没有加锁，因此上述重排序对于线程 T1 来说是有影响的，这会使得线程 T1 得到一个不为 null 但内部还未完全初始化完毕的 instance 变量，从而造成一些意想不到的错误

在分析清楚问题的原因后，解决方法也就不难想到：只要将 instance 变量采用 volatile 修饰即可，这实际上是利用了 volatile 关键字的以下两个作用：

- 保障可见性。一个线程通过执行 `instance = new Singleton()` 修改了 instance 变量值，其它线程可以读取到相应的值
- 保障有序性。由于 volatile 能够禁止 volatile 变量写操作与该操作之前的任何读、写操作进行重排序，因此，用 volatile 修饰 instance 相当于禁止 JIT 编译器以及处理器将子操作2重排序到子操作3之后，这保障了一个线程读取到 instance 变量所引用的实例时该实例已经初始化完毕

因此，双重检查锁定的单例模式其正确的实现方式如下所示

```java
public final class Singleton {

    private static volatile Singleton instance = null;

    private Singleton() {

    }

    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }

    public static void main(String[] args) {
        Singleton singleton = Singleton.getInstance();
    }

}
```

## 3、静态内部类

类的静态变量被初次访问时会触发 Java 虚拟机对该类进行初始化，即该类的静态变量的值会变为其初始值而不再是默认值（例如，引用型变量的默认值是 null，int 的默认值是 0）。因此，静态方法 `getInstance()` 被调用的时候 Java 虚拟机会初始化这个方法所访问的内部静态类 InstanceHolder。这使得 InstanceHolder 的静态变量 INSTANCE 被初始化，从而使得 Singleton 类的唯一实例得以创建。由于类的静态变量只会创建一次，因此 Singleton 也只会有一个实例变量

```java
public final class Singleton {

    private Singleton() {

    }

    private final static class InstanceHolder {

        final static Singleton INSTANCE = new Singleton();

    }

    public static Singleton getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public static void main(String[] args) {
        Singleton singleton = Singleton.getInstance();
    }

}
```

## 4、枚举类

枚举类 Singleton 相当于一个单例类，其字段 INSTANCE 相当于该类的唯一实例。这个实例是在 `Singleton.INSTANCE` 初次被引用的时候才会被初始化的。仅访问 Singleton 本身（例如 `Singleton.class.getName()` ）并不会导致 Singleton 的唯一实例被初始化

```java
public class SingletonExample {

    public static void main(String[] args) {
        Singleton.INSTANCE.doSomething();
    }

    public enum Singleton {

        INSTANCE;

        void doSomething() {

        }

    }

}
```