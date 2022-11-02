> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

> 目前，多线程编程可以说是在大部分平台和应用上都需要实现的一个基本需求。本系列文章就来对 **Java 平台下的多线程编程知识**进行讲解，从**概念入门**、**底层实现**到**上层应用**都会涉及到，预计一共会有五篇文章，希望对你有所帮助 😎😎
>
> 本篇文章是第四篇，来介绍 Java 平台下的锁机制，锁是 Java 开发者实现线程同步最为简单的一种方式

锁是 Java 开发者实现线程同步最为简单的一种方式，最简单的情形下我们只需要添加一个 synchronized 关键字就可以实现线程同步，但锁的分类细数下来也不少，JVM 自动为代码中的锁所做的优化措施也有很多，这里来对其详细讲一讲

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/2b7dad21c62045298097b38af2158c82~tplv-k3u1fbpfcp-zoom-1.image)

# 一、悲观锁、乐观锁

悲观锁与乐观锁两者体现了多个线程在对共享数据进行并发操作时的不同看法

对于多个线程间的共享数据，悲观锁认为自己在使用数据的时候很有可能会有其它线程也刚好前来修改数据，因为在使用数据前都会加上锁，确保在使用过程中数据不会被其它线程修改。synchronized 关键字和 Lock 接口的实现类都属于悲观锁

乐观锁则认为在使用数据的过程中其它线程也刚好前来修改数据的可能性很低，所以在使用数据前不会加锁，而只是在更新数据的时候判断数据之前是否有被别的线程更新了。如果数据没有被更新，当前线程就可以将自己修改后的数据成功写入。而如果数据已经被其它线程更新过了，则根据不同的实现方式来执行不同的补救操作（报错或者重复尝试）。乐观锁在 Java 中是通过使用**无锁编程**来实现的，最常采用的是 **CAS 算法**，`java.util.concurrent` 包中的原子类就是通过 **CAS 自旋**来实现的

总的来说，悲观锁适合写操作较多的场景，加锁可以保证执行写操作时数据的正确性。乐观锁适合读操作较多的场景，不加锁能够使读操作的性能大幅度提升

---

synchronized 关键字和 Lock 接口所代表的悲观锁比较常见，这里主要来看下乐观锁

乐观锁采用的 CAS 算法全称是 **Compare And Swap（比较与交换）**，是一种无锁算法，在不使用锁（所以也不会导致线程被阻塞）的情况下实现在多线程之间的变量同步

CAS 算法涉及到三个操作数：

- 需要读写的内存值 V
- 进行比较的值 A
- 要写入的新值 B

当且仅当 V 的值等于 A 时，CAS 才会用新值 B 来更新 V 的值，且保证了**“比较+更新”**这整个操作的原子性。当 V 的值不等于 A 时则不会执行任何操作。一般情况下，“更新”是一个会不断重试的操作

这里来看下 AtomicInteger 类的用于自增加一的方法 `incrementAndGet()` 是如何实现的。

```java
public class AtomicInteger extends Number implements java.io.Serializable {
  
  private static final Unsafe unsafe = Unsafe.getUnsafe();
  
  /**
    * Atomically increments by one the current value.
    *
    * @return the updated value
   */
  public final int incrementAndGet() {
  		return unsafe.getAndAddInt(this, valueOffset, 1) + 1;
  }
  
}
```

 `incrementAndGet()` 方法是通过 `unsafe.getAndAddInt()` 来实现的。`getAndAddInt()` 方法会循环获取给定对象 o 中的偏移量 offset 处的值 v，然后判断内存值是否等于 v。如果相等则将内存值修改为 v + delta，否则就继续整个循环进行重复尝试，直到修改成功才退出循环，并且将旧值返回。整个“比较+更新”操作封装在 `compareAndSwapInt()` 方法中，在 JNI 里是借助于一个 CPU 指令完成的，属于原子操作，可以保证多个线程都能够看到同一个变量的修改值

```java
public final int getAndAddInt(Object o, long offset, int delta) {
    int v;
    do {
        v = getIntVolatile(o, offset);
    } while (!compareAndSwapInt(o, offset, v, v + delta));
    return v;
}

public native int getIntVolatile(Object var1, long var2);

public final native boolean compareAndSwapInt(Object var1, long var2, int var4, int var5);
```

CAS 虽然很高效，但是也存在**ABA问题**，且如果 CAS 操作长时间不成功的话，会导致其一直自旋，给处理器带来非常大的开销

# 二、可重入锁、非可重入锁

锁是否可重入表示的是这么一种特性：锁的持有线程在不释放锁的前提下，是否能够再次申请到同一个锁。例如，如果 synchronized 是可重入锁，那么 `doSomething1()` 是可以正常执行的。如果 synchronized 是不可重入锁，那么 `doSomething1()` 就会导致死锁。而在现实情况下，Java 中 synchronized 和 ReentrantLock 都是可重入锁

```java
private synchronized void doSomething1() {
    doSomething2();
}

private synchronized void doSomething2() {

}
```

这里也以 ReentrantLock 作为例子来从看下其重入流程

我们知道，在使用 ReentrantLock 时，我们调用了 `Lock.lock()` N 次后就要相应调用 `Lock.unlock()` N 次才可以使得持有线程真正地释放了锁，那么这里自然就需要有个状态值来记录 ReentrantLock 申请了几次锁（即重入了几次）。ReentrantLock 包含一个内部类 Sync，Sync 继承自 AQS（AbstractQueuedSynchronizer）

AQS 中维护了一个 int 类型的同步状态值 status，其初始值为 0，在不同的场景下具有不同的含义，对于 ReentrantLock 来说就是用来标记重入次数。当线程尝试获取锁时，ReentrantLock 就会尝试获取并更新 status 值

- 如果 status 等于 0。表示锁没有被其它线程抢占，则把 status 置为 1，同时当前线程成功抢占到锁
- 如果 status 不等于 0。判断当前线程是否是该锁的持有线程，如果是的话则执行 status + 1，表示当前线程再次重入了一次。如果当前线程不是该锁的持有线程，则意味着抢占失败

当持有线程释放锁时，ReentrantLock 同样会先获取当前 status 值。如果 status - 1 等于 0，表示当前线程已经撤销了所有的申请操作，此时线程才会真正释放锁，否则持有线程就还是依然占用着锁

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/4bbe305c449a49ffaf529451b206a7b3~tplv-k3u1fbpfcp-zoom-1.image)

# 三、公平锁、非公平锁

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

综上所诉，公平调度策略适用于资源被持有的时间较长或者线程申请资源的平均时间间隔较长的情形，或者要求申请资源所需的时间偏差较小的情况。总的来说使用公平调度策略的开销会比使用非公平调度策略的开销要大，因此在没有特别需求的情况下，应该默认使用非公平调度策略

公平锁就是指采用了公平调度策略的锁，非公平锁就是指采用了非公平调度策略的锁。Java 中的 synchronized 就是非公平锁；而 ReentrantLock 既支持公平调度策略也支持非公平调度策略，且默认使用的也是非公平调度策略

这里来简单看下 ReentrantLock 的源码来了解下公平锁和非公平锁的实现区别

ReentrantLock 申请和释放锁的大部分逻辑都是在其内部类 Sync 里实现的。Sync 包含公平锁 `FairSync` 和非公平锁 `NonfairSync` 两个不同的子类实现，ReentrantLock 默认使用的是 `NonfairSync`

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/8147720091ee49e3a65aedbebb520ce4~tplv-k3u1fbpfcp-zoom-1.image)

`FairSync` 和 `NonfairSync` 在申请锁时的会分别调用以下两个方法，两者的唯一的区别只在于公平锁在获取同步状态时多了一个限制条件：`hasQueuedPredecessors()`

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/ee6a997023354ebf9a51913d7c30b5fc~tplv-k3u1fbpfcp-zoom-1.image)

`hasQueuedPredecessors()` 方法用于判断等待队列中是否有排在当前线程之前的线程，如果有返回 true，否则返回 false。所以说，ReentrantLock 的公平调度策略只有在等待队列为空时才允许当前的活跃线程执行申请锁的操作，而非公平调度策略则是直接就进行申请

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/b6c6e991456942ef86ee895a40ae7e71~tplv-k3u1fbpfcp-zoom-1.image)

# 四、互斥锁、共享锁

互斥锁也称为排他锁，是指该锁一次只能被一个线程持有，当某个线程已经在持有锁的时候其它来申请同个锁实例的线程只能进行等待，以此来保证临界区内共享数据的安全性。Java 中的 synchronized 和 `java.util.concurrent.locks.Lock` 接口的实现类就属于互斥锁

互斥锁使得多个线程无法以线程安全的方式在同一时刻对共享数据进行**只读取而不更新**的操作，这在**共享数据读取频繁但更新频率较低**的情况下降低了系统的并发性，共享锁就是为了应对这种问题而诞生的。共享锁是一种改进型的排他锁，也称为**共享/排他锁**。共享锁允许多个线程同时读取共享变量，但是一次只允许一个线程对共享变量进行更新。任何线程读取共享变量的时候，其它线程无法更新这些变量；一个线程更新共享变量的时候，其它线程都无法读取和更新这些变量

Java 平台中的**读写锁**就是对共享锁这个概念的实现，由 `java.util.concurrent.locks.ReadWriteLock` 接口来定义，其默认实现类是 `java.util.concurrent.locks.ReentrantReadWriteLock`

`ReadWriteLock` 接口定义了两个方法，分别用来获取**读锁**（ReadLock）和**写锁**（WriteLock）。ReadLock 是共享的，WriteLock 是排他的，ReadLock 和 WriteLock 的操作最终都要转交由内部类 Sync 来完成

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/8efb129766bd4aec899406ec0f627728~tplv-k3u1fbpfcp-zoom-1.image)

上面在讲“**可重入锁与非可重入锁**”这一节内容的时候，有提到：AQS 中维护了一个 int 类型的同步状态值 status，其初始值为 0，在不同的场景下具有不同的含义。对于 ReentrantReadWriteLock 来说，status 就用来标记当前持有读锁和写锁的线程分别是多少

而为了在一个 32 位的 int 类型整数上来存储两种不同含义的数据，就需要将 status 进行分段切割，高 16 位用来存储读锁当前被获取的次数，低 16 位用来存储写锁当前被获取的次数

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/4eea22a8ff894dbab70721d40022bedc~tplv-k3u1fbpfcp-zoom-1.image)

Sync 类内部就提供了两个分别用来计算读线程和写线程个数的方法

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/1065b9408a6944e6811187b68af57e4e~tplv-k3u1fbpfcp-zoom-1.image)

## 1、共享流程

这里先来看下线程在获取读锁时的申请流程，这里主要是要先前置判断下读写锁的写锁是否已经被持有了

```java
protected final int tryAcquireShared(int unused) {
    Thread current = Thread.currentThread();
    int c = getState();
    if (exclusiveCount(c) != 0 && getExclusiveOwnerThread() != current)
        //如果当前已经有线程持有了写锁，且该线程并非当前线程
        //则返回 -1，表示读锁获取失败
        //这里之所以要判断线程是否相等，是因为 ReentrantReadWriteLock 支持锁的降级，可以在已经持有写锁的时候申请读锁
        return -1;

    //下面就是多个线程前来申请读锁或者是同个线程多次申请读锁的流程了
    int r = sharedCount(c);
    if (!readerShouldBlock() &&
        r < MAX_COUNT &&
        compareAndSetState(c, c + SHARED_UNIT)) {
        if (r == 0) {
            firstReader = current;
            firstReaderHoldCount = 1;
        } else if (firstReader == current) {
            firstReaderHoldCount++;
        } else {
            HoldCounter rh = cachedHoldCounter;
            if (rh == null || rh.tid != getThreadId(current))
                cachedHoldCounter = rh = readHolds.get();
            else if (rh.count == 0)
                readHolds.set(rh);
            rh.count++;
        }
        return 1;
    }
    return fullTryAcquireShared(current);
}
```

而线程在释放读锁时，主要就是更新 state 值，将读线程数量减一，写线程数量不做改动

```java
protected final boolean tryReleaseShared(int unused) {
    Thread current = Thread.currentThread();
    if (firstReader == current) {
        // assert firstReaderHoldCount > 0;
        if (firstReaderHoldCount == 1)
            firstReader = null;
        else
            firstReaderHoldCount--;
    } else {
        HoldCounter rh = cachedHoldCounter;
        if (rh == null || rh.tid != getThreadId(current))
            rh = readHolds.get();
        int count = rh.count;
        if (count <= 1) {
            readHolds.remove();
            if (count <= 0)
                throw unmatchedUnlockException();
        }
        --rh.count;
    }
    for (;;) {
        int c = getState();
        //SHARED_UNIT 等于 1 << 16
        //c - SHARED_UNIT 就想当于 c 的高16位减1，低16位保持不变
        //从而使得读线程数量减1，写线程数量不变
        int nextc = c - SHARED_UNIT;
        //通过 CAS 来更新 state
        if (compareAndSetState(c, nextc))
            // Releasing the read lock has no effect on readers,
            // but it may allow waiting writers to proceed if
            // both read and write locks are now free.
            return nextc == 0;
    }
}
```

## 2、互斥流程

再来看下线程在获取写锁时的流程。主要是要考虑写锁的可重入性以及读写锁的公平性与否

```java
protected final boolean tryAcquire(int acquires) {
    /*
     * Walkthrough:
     * 1. If read count nonzero or write count nonzero
     *    and owner is a different thread, fail.
     * 2. If count would saturate, fail. (This can only
     *    happen if count is already nonzero.)
     * 3. Otherwise, this thread is eligible for lock if
     *    it is either a reentrant acquire or
     *    queue policy allows it. If so, update state
     *    and set owner.
     */
    Thread current = Thread.currentThread();
    int c = getState();
    int w = exclusiveCount(c); //获取当前写线程数量
    if (c != 0) {
        // (Note: if c != 0 and w == 0 then shared count != 0)
        //1. 如果 c != 0 && w == 0 成立，说明当前存在读线程，返回 false，写锁获取失败
        //2. 如果 c != 0 && w != 0 && current != getExclusiveOwnerThread() 成立
        //说明当前写锁已经被持有了，且持有写锁的线程并非当前线程，返回 false，写锁获取失败
        if (w == 0 || current != getExclusiveOwnerThread())
            return false;
        if (w + exclusiveCount(acquires) > MAX_COUNT)
            throw new Error("Maximum lock count exceeded");
        // Reentrant acquire
        //能走到这一步，说明当前线程已经持有了写锁，由于写锁是可重入的
        //所以这里这里更新下写锁被持有的次数后就返回了 true
        setState(c + acquires);
        return true;
    }
    //能走到这一步，说明当前写锁还未被持有，则根据读写锁的公平性与否来完成写锁的申请
    if (writerShouldBlock() ||
        !compareAndSetState(c, c + acquires))
        return false;
    setExclusiveOwnerThread(current);
    return true;
}
```

线程在释放写锁时，由于 state 值的高 16 位肯定全是 0 （即读线程数量为 0），而低 16 位肯定不全是 0，所以主要就是来更新当前写锁被持有的次数

```java
protected final boolean tryRelease(int releases) {
    if (!isHeldExclusively())
         //如果当前线程并非写锁的持有线程，则抛出异常
        throw new IllegalMonitorStateException();
    int nextc = getState() - releases;
    //由于写锁是可重入的，所以这里也要判断线程是否已经撤销了所有的申请操作
    boolean free = exclusiveCount(nextc) == 0;
    if (free)
        //只有在写锁已经撤销了所有的申请操作后才会真正释放锁
        setExclusiveOwnerThread(null);
    setState(nextc);
    return free;
}
```

# 五、自旋锁、适应性自旋锁

在一个排他锁已经被持有且锁的持有线程只会占用锁一小段时间的情况下，如果此时将来申请同个锁实例的线程均进行暂停运行处理的话，锁在暂停和后续唤醒过程中所需的时间耗时甚至可能会长于锁被占用的时间，此时暂停线程就显得很不值得了。而自旋锁的实现出发点就基于这么一种观测结果：在很多时候锁的持有线程只需要占用锁一小段时间

自旋锁的实现前提是当前物理机器包含一个以上的处理器，即支持同时运行一个以上的线程，此时就可以让后面来申请锁的线程不放弃处理器时间片而是稍微“等一等”，看看锁是否很快就会被释放。让线程“等一等”，就是通过让线程反复执行**忙循环**（也称自旋，可以理解为执行空操作，实现原理也是 CAS）来实现的。如果锁被占用的时间很短，此时采用自旋锁的效果就会非常好，不会导致上下文切换。而如果锁被占用的时间比较长，自旋锁就会浪费很多处理器时间，因此也必须为自旋操作限定一个最大次数，当达到限定的最大次数后如果仍然没有获得锁的话就还是需要将线程进行暂停运行处理

因此，自旋锁适用于绝大多数线程对锁的持有时间比较短的情况，这样能够避免上下文切换的资源开销和过多的处理器时间开销。而对于系统中绝大多数线程对锁的持有时间比较长的情况，就还是采用直接暂停线程的策略比较适合

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/05d33adaa1764d919bb02695a7708e4d~tplv-k3u1fbpfcp-zoom-1.image)

在自旋锁出现的一开始，只能对 JVM 中的所有锁设定一个固定的最大自旋次数。而在后续也引入了**适应性自旋锁**。适应性意味着自旋的时间（次数）不再是固定的，而是由前一次在该锁上的自旋时间及其当前持有线程的状态来决定。对于某个锁，如果其当前正在被某个已经通过自旋成功获得锁的线程持有的话，那么 JVM 就会认为其它来申请同个锁的线程再次使用自旋也很能再次成功，进而将允许自旋等待相对更长的时间。如果对于某个锁自旋很少成功获得过，那么在以后尝试获取这个锁时将可能省略掉自旋过程，而是直接阻塞线程，避免浪费处理器资源

总的来说，通过采用自旋锁，锁的申请就并不一定会导致上下文切换了，自旋锁的自适应性也进一步降低了发生线程上下文切换的概率

# 六、偏向锁、轻量级锁、重量级锁

**偏向锁、轻量级锁、重量级锁**可以看做是三种**状态值**或者说是**操作手段**，用于描述 synchronized 所对应的内部锁所处的状态，在不同的状态下获取内部锁的实现步骤也各不相同，在理解这三种状态前需要先了解下什么是**对象头**

## 1、对象头

在 HotSpot 虚拟机里，对象在堆内存中的存储布局可以划分为三个部分：对象头（Header）、实例数据（Instance Data）和对齐填充（Padding） 。当中，对象头包含着对象自身的运行时数据，如 HashCode、GC 分代年龄、锁状态标志、线程持有的锁、偏向线程ID、偏向时间戳等，这部分数据的长度在 32 位和 64 位的虚拟机（未开启压缩指针）中分别为 32 个比特和 64 个比特，官方称它为“Mark Word”，它是实现偏向锁和轻量级锁的关键。Mark Word 被设计成一个有着动态定义的数据结构，在运行期间 Mark Word 里存储的数据会随着锁标志位的变化而变化，即在不同的状态下会分别存储具有不同含义的数据

例如，在 32 位的 HotSpot 虚拟机中，如果对象处于未被锁定状态，Mark Word 的 32 个比特存储空间中的 25 个比特会用于存储对象哈希码，4 个比特用于存储对象分代年龄，1个比特固定为 0，2 个比特用于存储锁标志位

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/487cbaf7e70f4b6ab4aa221f1cda2717~tplv-k3u1fbpfcp-zoom-1.image)

我们在使用 synchronized 时会显式或隐式地指定关联的**同步对象**（实例变量或者是 Class 对象），而 Java 平台中的任何一个对象都有一个唯一与之关联的锁，被称为**监视器（Monitor）**或者**内部锁**。同步对象的对象头所包含的运行时数据的变化过程，就是其内部锁在**偏向锁、轻量级锁、重量级锁**这四种状态下的切换过程

## 2、偏向锁

JVM 在实现  monitorenter（申请锁） 和 monitorexit（释放锁） 这两个字节码指令时需要借助一个原子操作（CAS 操作），这个操作代价相对来说比较昂贵。而如果在一段时间内一个锁实例先后只会由同一个线程来申请并使用的话，那么该线程每次申请和释放锁的代价就会被放大，显得很不值得了。而偏向锁的实现出发点就基于这么一种观测结果：大多数锁并没有被争用，并且在其整个生命周期内总是同一个线程来进行申请

偏向锁的执行流程是这样的：

1. 当某个锁第一次被线程申请到时，JVM 会把同步对象的 Mark Word 中的标志位设置为“01”，偏向模式设置为“1”，表示该锁进入了偏向模式。同时使用 CAS 操作把当前线程的 ID 记录在对象的 Mark Word 之中，如果 CAS 操作成功，该线程就会被记录为同步对象的偏好线程（Biased Thread），然后执行步骤 4。如果 CAS 操作失败，则直接步骤 3
2. 当又有线程前来申请锁时，如果判断到偏向锁指向的 Thread ID 即为当前线程，则直接执行步骤 4，即偏向线程以后每次进入这个锁相关的同步块时，都不用再进行任何加锁操作。如果偏向锁指向的 Thread ID 并非当前线程，说明当前系统存在多个线程竞争偏向锁，则通过 CAS 来竞争锁。如果竞争成功，则将Mark Word 中的线程 ID 设置为当前线程 ID，然后执行步骤 4；如果竞争失败，则执行步骤 3
3. 因为线程不会主动去释放偏向锁，所以如果 CAS 获取偏向锁失败，则表示当前存在多个线程一个竞争偏向锁。当到达全局安全点（safepoint）时，会首先暂停拥有偏向锁的线程，然后检查持有偏向锁的线程是否活着（因为持有偏向锁的线程可能已经执行完毕，但该线程并不会主动去释放偏向锁），如果线程不处于活动状态，则将对象头设置成无锁状态（标志位为“01”），然后重新偏向新的线程；如果线程仍然活跃，则撤销偏向锁，将其升级到轻量级锁状态（标志位变为“00”），此时轻量级锁由原持有偏向锁的线程继续持有，让其继续执行同步代码，而正在申请锁的线程则通过自旋等待获得该轻量级锁
4. 执行同步代码

引入偏向锁是为了提高带有 synchronized 同步操作但实际上无争用的代码块的性能，因为偏向线程在获取到偏向锁之后，每次进入这个锁相关的同步块时，都不用再进行任何同步操作（例如加锁、解锁及对 Mark Word 的更新操作等）。而且轻量级锁的获取及释放依赖多次 CAS 原子指令，而偏向锁只需要在置换 Thread ID 的时候执行一次 CAS 原子指令即可

偏向锁适用于存在相当大一部分锁并没有被争用的系统，如果系统中存在大量被争用的锁而没有被争用的锁仅占小部分，那么就可以考虑关闭偏向锁

## 3、轻量级锁

轻量级锁是 JDK 6 时加入的新型锁机制。当某个锁是偏向锁时，如果该锁被其它线程访问了，此时偏向锁就会升级为轻量级锁，其它线程会通过自旋的方式来尝试获取锁，此时该线程不会阻塞，从而提高了性能

在线程进入同步块的时候，如果同步对象锁没有被锁定（锁标志位为“01”），JVM 首先会在当前线程的栈帧中建立一个名为锁记录（Lock Record）的空间，然后拷贝对象头中的 Mark Word 到锁记录中。拷贝完成后，JVM 将使用 CAS 操作尝试将对象的 Mark Word 值更新为指向 Lock Record 的指针，并将 Lock Record 里的 owner 指针指向对象的 Mark Word。如果这个更新动作成功了，即代表这个线程拥有了该对象锁，并且 Mark Word 的锁标志位将变更为为 “00”，表示此对象处于轻量级锁定状态。如果这个更新操作失败了，那就意味着至少存在一个线程与当前线程竞争获取该对象锁。JVM 会先检查对象的 Mark Word 是否指向当前线程的栈帧，如果是，说明当前线程已经拥有了这个对象的锁，那直接进入同步块继续执行就可以了，否则就说明这个锁对象已经被其他线程抢占了。如果出现两个以上的线程争用同一个锁的情况，那轻量级锁就不再有效，必须要升级为重量级锁，锁标志的状态值变为“10”，此时 Mark Word 中存储的就是指向重量级锁（互斥量）的指针，此后等待锁的线程也必须进入阻塞状态

轻量级锁的实现出发点是基于这么一种观测结果：大多数锁在整个同步周期内都是不存在竞争的。这里需要和偏向锁区分开，偏向锁的理论基础是：大多数锁总是在其整个生命周期内被同一个线程所使用。而轻量级锁的理论基础是：锁可能会先后被多个线程使用，但由于线程间的交叉使用，所以大多数线程在使用同步资源时是不存在竞争的。偏向锁相对轻量级锁会更加“乐观”，所以轻量级锁就需要比偏向锁多出更多的“安全保障措施”

如果没有竞争，轻量级锁便通过 CAS 操作成功避免了使用互斥量的开销；但如果确实存在锁竞争，除了互斥量的本身开销外，还额外产生了 CAS 操作的开销。因此在有竞争的情况下轻量级锁会比传统的重量级锁更加消耗资源

## 4、重量级锁

升级为重量级锁时，Mark Word 中前 30 位存储的是指向重量级锁（互斥量）的指针，锁标志的状态值变为“10”，此后等待锁的线程就必须进入阻塞状态。重量级锁是实现锁申请操作最为消耗资源的一种做法

## 5、概述

综上，偏向锁通过对比 Mark Word 解决了加锁问题，避免执行 CAS 操作。而轻量级锁是通过用 CAS 操作和自旋来解决加锁问题，避免线程阻塞和唤醒而影响性能。重量级锁则是直接将除了拥有锁的线程以外的线程都阻塞

# 七、锁消除

锁消除是 JIT 编译器对内部锁的具体实现所做的一种优化。在动态编译同步块的时候，编译器会借助**逃逸分析**技术来判断同步块所使用的锁对象是否只会被一个线程使用。如果判断出该锁对象的确只能被一个线程访问，编译器在编译这个同步块的时候就不会生成 synchronize 所表示的 monitorenter 和 monitorexit 两个机器码，而仅生成临界区内的代码所对应的机器码，从而消除了锁的使用。这种优化措施就称为锁消除，锁消除使得在特定情况下可以完全消除锁的开销

例如，对于以下方法。StringBuffer 类本身是线程安全的，其内部多个方法（例如，append 方法）都使用到了内部锁，而在`toJson()`方法里 StringBuffer 是作为一个局部变量存在的，并不会存在多个线程同时访问的情况，此时 append 方法所使用到的内部锁就成了一种无谓的消耗。所以，编译器在编译 toJson 方法的时候就会将其调用的 StringBuffer.append 方法内联到该方法之中，相当于把 StringBuffer.append 方法的方法体中的指令复制到 toJson 方法体中，此时就可以避免 append 方法所声明的内部锁所带来的消耗

```java
public String toJson() {
    StringBuffer stringBuffer = new StringBuffer();
    stringBuffer.append("xxx");
    return stringBuffer.toString();
}
```

锁消除不一定会被编译器实施，这和同步代码块是否能被内联有关，所以虽然锁消除技术可以使得编译器为我们消除一部分的锁开销，但是这也不意味着开发者就可以随意使用内部锁了

# 八、锁粗化

锁粗化是指 JIT 编译器会将相邻的几个同步代码块合并为一个大的同步代码块的一种优化措施。通过锁粗化，可以避免一个线程反复申请和释放同一个锁所导致的开销，相应的也会导致一个线程持有一个锁的时间变长，从而使得锁的等待线程申请锁所需要的时间也相应变长

例如，对于以下代码。通过锁粗化技术就可以将多个同步代码块合并为一个，且同步代码块之间的代码也会被合并在一起，使得临界区的长度变长。而原本在锁 lockX 的持有线程执行完第一个同步代码块之后（即释放 lockX 后），其它等待线程是有机会获得 lockX 的，但是经过锁粗化后使得 lockX 的持有线程只有执行完全部同步代码块之后才会释放 lockX，使得等待线程申请 lockX 的时间相应变长了。因此，为了避免一个线程持有锁的时间过长，锁粗化不会被应用到循环体内的相邻同步代码块

```java
//锁粗化前
public void test() {
    synchronized (lockX) {
        doSomethind1();
    }
    x = 10;
    synchronized (lockX) {
        doSomethind2();
    }
    y = 10;
    synchronized (lockX) {
        doSomethind3();
    }
}

//锁粗化后
public void test() {
    synchronized (lockX) {
        doSomethind1();
        x = 10;
        doSomethind2();
        y = 10;
        doSomethind3();
    }
}
```

# 九、优化对锁的使用

以上所讲的大部分优化措施都是在编译器这个层次实施的，这一节再来介绍下如何在代码层次对锁进行优化

## 1、降低锁的争用程度

在之前的文章中有介绍过使用锁带来的主要开销，而如果必须使用锁且锁带来的开销很难避免，那么要降低锁的开销的思路之一就是降低锁的争用程度。锁的争用程度和程序中需要同时使用到该锁实例的线程数量有关，如果可以尽量降低每个线程来申请锁时该锁实例还被其它线程持有的情况，那么就可以降低锁的争用程度。降低锁的争用程度可以用两种方式来实现：**减少锁被持有的时间**和**降低锁的申请频率**

- 减少锁被持有的时间即让每个线程持有锁的时间尽量短，从而减少当某个线程申请锁时而锁的持有线程还未执行完临界区代码的情况，而且也有利于 Java 虚拟机的适应性锁发挥作用。可以通过减少临界区长度来缩减锁被持有的时间，例如：将不会导致竞态的代码（局部变量的访问等）放到临界区之外执行，使得每个线程在持有锁的过程中需要执行的指令尽量少。此外，也需要避免在临界区中执行阻塞式 IO 等阻塞操作，阻塞操作会导致线程被暂停和上下文切换，而在线程被暂停的过程中其持有的锁也不会被释放，这样会增大锁被争用的可能性

- 降低锁的申请频率可以通过减小锁的粒度来实现。例如，多个线程间存在多个共享变量，而共享变量之间并没有特定的关联关系，此时就可以分别使用不同的锁对象来保障不同的共享变量在多个线程间的线程安全性。假设多个线程间存在两个共享变量 A 和 B，如果变量 A 和变量 B 之间并没有关联关系，那么在访问共享变量的时候就可以使用不同的锁，线程 A 在访问变量 A 的时候可以使用 Lock A 来保障安全性，而在线程 A 持有 Lock A 的过程中也不妨碍线程 B 申请 Lock B 对变量 B 进行访问。通过这种使用不同的锁来保障不同共享数据的安全性，从而减少锁的争用程度。但如果锁的粒度过细也会增加锁调度的开销，需要在实际开发中衡量使用

## 2、使用可参数化锁

如果一个方法或者类的内部所使用的锁实例可以由其使用者来指定的话，那么就可以说这个锁是**可参数化**的，相应的这个锁就被称为**可参数化的锁**。使用可参数化锁有助于减少线程需要申请的锁实例的个数，从而减少锁的开销

例如，对于以下例子。假设 Printer 类是由第三方提供的工具类，其内部需要保障自身的线程安全性，所以使用到了内部锁，其锁实例默认是其本身变量实例（即 this） 。LogPrinter 类作为**客户端/使用者**，其内部也需要保障自身的线程安全性（例如：line++; ），所以也使用到了内部锁。但由于 Printer 的所有方法均由 LogPrinter 已经保障了线程安全性的方法进行调用，此时 Printer 内部使用到的内部锁就成了多余配置，增加了无谓的锁开销

由于 Java 平台中的锁都是可重入的，且锁的持有线程在未释放锁的情况下重复申请该锁的开销时所需要的开销比较小，所以此时就可以依靠 Printer 类提供的可参数化锁配置，将 LogPrinter 声明的锁实例 lock 作为构造参数传给 Printer，从而减少了锁开销

```java
class Printer {

    private final Object lock;

    public Printer(Object lock) {
        this.lock = lock;
    }

    public Printer() {
        this.lock = this;
    }

    public void print(String msg) {
        synchronized (lock) {
            System.out.println(msg);
        }
    }

}

class LogPrinter {

    private final Object lock = new Object();

    private final Printer printer = new Printer(lock);

    private int line;

    public void print(String msg) {
        synchronized (lock) {
            line++;
            printer.print(msg);
        }
    }

}
```

# 十、参考资料

1. [不可不说的Java“锁”事](https://tech.meituan.com/2018/11/15/java-lock.html) 