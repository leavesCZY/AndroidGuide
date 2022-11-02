> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

> 目前，多线程编程可以说是在大部分平台和应用上都需要实现的一个基本需求。本系列文章就来对 **Java 平台下的多线程编程知识**进行讲解，从**概念入门**、**底层实现**到**上层应用**都会涉及到，预计一共会有五篇文章，希望对你有所帮助 😎😎
>
> 本篇文章是第三篇，来介绍四种不同类型的线程活性故障现象，这是开发者所必须应对的异常情况

**线程活性故障**是由于资源稀缺性或者程序自身的问题导致线程一直处于非 Runnable 状态，或者线程虽然处于 Runnable 状态但是其要执行的任务一直无法取得进展的一种故障现象

下面就来介绍几种常见类型的线程活性故障：

1. 死锁

2. 锁死

3. 线程饥饿

4. 活锁

# 一、死锁

如果多个线程因互相等待对方而被永远暂停（生命周期状态为 Blocked 或者 Waiting），那么就称这些线程产生了死锁（Deadlock）。由于产生死锁的线程的生命周期状态永远是非运行状态，所以如果没有外力作用，这些线程所要执行的任务就永远也无法取得进展

例如，线程 A 在持有锁 L1 的情况下申请锁 L2，同时线程 B 在持有锁 L2 的情况下在申请锁 L1，而线程 A 和线程 B 各自要求只有在取得对方的锁后才能释放持有的锁，这就导致了两个锁都将处于无限等待的状态，此时死锁就发生了

有关死锁的一个经典问题是**哲学家就餐问题**。五位哲学家围着一张圆桌，每位哲学家之间均放着一根筷子，即一共有五根筷子。每位哲学家要么处于思考状态，要么是在吃饭。吃饭前，每位哲学家均会先拿起左手边的筷子，再拿起右手边的筷子，只有当手上持有了一双筷子时哲学家才能够吃饭，且除非本次吃饭行为完成，否则哲学家不会放下手中已持有的筷子。哲学家吃完饭后就会放下手中的筷子，再次思考一段时间后再进行吃饭

在这个问题中，每位哲学家就相当于一个线程，每根筷子就相当于多条线程间的共享资源。且筷子明显是一个排他性资源，因为每根筷子每次只能由一位哲学家持有，因此哲学家在拿起筷子前需要先取得筷子对应的锁。由于筷子和哲学家的数量相等，而每位哲学家需要的筷子数量是现有的两倍，所以发生死锁的可能性还是很大的

我们可以用一段程序来模拟并验证上述的情况

先对筷子 Chopstick 进行定义，其能被操作的行为只有两种，即：**拿起**和**放下**

```kotlin
enum class ChopstickStatus {
    UP,
    Down
}

data class Chopstick(val id: Int) {

    var status = ChopstickStatus.Down
        private set

    fun pickUp() {
        status = ChopstickStatus.UP
    }

    fun putDown() {
        status = ChopstickStatus.Down
    }

}
```

每位哲学家 Philosopher 均对应一个唯一的标识 id，一根左手边的筷子 left，一根右手边的筷子 right。会不间断地进行“思考”和“吃饭”两种行为，每种行为均包含一段随机的时间间隔（随机的线程休眠）

```kotlin
private object Tools {

    fun randomSleep(max: Long = 30) {
        val realMax = max.coerceAtLeast(1)
        ThreadLocalRandom.current().nextLong(if (realMax == 1L) 0 else 1, max + 1)
    }

}

data class Philosopher(val id: Int, val left: Chopstick, val right: Chopstick) : Thread("Philosopher-$id") {

    override fun run() {
        while (true) {
            think()
            eat()
        }
    }

    private fun eat() {
        synchronized(left) {
            println("$name 拿起了左边的筷子: " + left.id)
            left.pickUp()
            synchronized(right) {
                println("$name 拿起了右边的筷子: " + right.id)
                right.pickUp()
                println("$name 开始吃饭.....")
                Tools.randomSleep(10)
                println("$name 吃饭结束!!!!!!!!!!")
                right.putDown()
            }
            left.putDown()
        }
    }

    private fun think() {
        println("$name 思考中....")
        Tools.randomSleep(100)
    }

}
```

然后，我们创建五根筷子，并将每根筷子按顺序分配给每位哲学家，然后就启动哲学家的思考和吃饭行为（启动线程）

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 */
fun main() {
    val philosopherNumber = 5

    val chopstickList = mutableListOf<Chopstick>()
    for (i in 0 until philosopherNumber) {
        chopstickList.add(Chopstick(i))
    }

    val philosopherList = mutableListOf<Philosopher>()
    for (index in 0 until philosopherNumber) {
        val left = chopstickList[index]
        val right = chopstickList.getOrNull(index - 1) ?: chopstickList.last()
        philosopherList.add(Philosopher(index, left, right))
    }

    philosopherList.forEach {
        println(it.name + " 左手边的筷子是：" + it.left + " 右手边的筷子是：" + it.right)
    }

    philosopherList.forEach {
        it.start()
    }
}
```

最后，运行程序后，只要我们为哲学家设定每次思考和吃饭的耗时时间不要太长，那么应该就能很快看到程序没有继续输出日志了，似乎被卡住了，此时即发生了死锁

```kotlin
Philosopher-0 左手边的筷子是：Chopstick(id=0) 右手边的筷子是：Chopstick(id=4)
Philosopher-1 左手边的筷子是：Chopstick(id=1) 右手边的筷子是：Chopstick(id=0)
Philosopher-2 左手边的筷子是：Chopstick(id=2) 右手边的筷子是：Chopstick(id=1)
Philosopher-3 左手边的筷子是：Chopstick(id=3) 右手边的筷子是：Chopstick(id=2)
Philosopher-4 左手边的筷子是：Chopstick(id=4) 右手边的筷子是：Chopstick(id=3)
Philosopher-0 思考中....
Philosopher-1 思考中....
Philosopher-2 思考中....
Philosopher-3 思考中....
Philosopher-4 思考中....
Philosopher-0 拿起了左边的筷子: 0
Philosopher-2 拿起了左边的筷子: 2
Philosopher-3 拿起了左边的筷子: 3
Philosopher-0 拿起了右边的筷子: 4
Philosopher-0 开始吃饭.....
Philosopher-0 吃饭结束!!!!!!!!!!
Philosopher-2 拿起了右边的筷子: 1
Philosopher-2 开始吃饭.....
Philosopher-2 吃饭结束!!!!!!!!!!
Philosopher-0 思考中....
Philosopher-0 拿起了左边的筷子: 0
Philosopher-4 拿起了左边的筷子: 4
Philosopher-3 拿起了右边的筷子: 2
Philosopher-3 开始吃饭.....
Philosopher-3 吃饭结束!!!!!!!!!!
Philosopher-3 思考中....
Philosopher-4 拿起了右边的筷子: 3
Philosopher-4 开始吃饭.....
Philosopher-4 吃饭结束!!!!!!!!!!
Philosopher-2 思考中....
Philosopher-4 思考中....
Philosopher-1 拿起了左边的筷子: 1
Philosopher-2 拿起了左边的筷子: 2
Philosopher-3 拿起了左边的筷子: 3
Philosopher-0 拿起了右边的筷子: 4
Philosopher-0 开始吃饭.....
Philosopher-0 吃饭结束!!!!!!!!!!
Philosopher-0 思考中....
Philosopher-0 拿起了左边的筷子: 0
Philosopher-4 拿起了左边的筷子: 4
```

根据输出日志可以分析出，最后每位哲学家均拿到了其左手边的筷子，且均在等待右手边的筷子被放下，但此时由于筷子是独占资源，所以每位哲学家都只能干瞪着眼无法吃饭，最终导致了死锁

# 二、死锁的产生条件

哲学家就餐问题反映了发生死锁的**必要条件**，线程一旦发生死锁，那么这些线程及相关的共享资源就一定同时满足以下条件：

1. 资源互斥。涉及的资源必须是排他性资源，即每个资源每次只能由一个线程持有
2. 资源不可抢夺。涉及的资源只能由其持有线程主动释放，其它线程无法从持有线程中主动夺得
3. 占用并等待其它资源。涉及的线程当前至少已经持有了一个排他性资源，并在申请其它资源，而这些资源同时又被其它线程所持有。在这个资源等待过程中，线程不会主动释放持有的现有资源
4. 循环等待资源。在涉及到的所有线程列表内部，每个线程均在互相等待其它线程释放持有的资源，形成了互相等待的圆形依赖关系。即存在一个处于等待状态的线程集合 {T1, T2, ..., Tn}，其中 Ti 等待的资源被 T(i+1) 占有（i 大于等于 1 小于 n），Tn 等待的资源被 T1 占有

以上条件是死锁产生的必要条件而非充分条件，即只要产生了死锁，以上条件就一定同时成立，但是上诉条件即使同时成立也未必就一定能产生死锁。例如，对于上诉的第四点，如果线程 T1 等待的资源数大于一，除了等待 T2 主动释放持有的一份资源外，T1 还可以通过获取**循环圈**外的多余资源来打破线程间的循环等待关系，从而避免造成死锁

# 三、规避死锁

如果把 Java 平台下的锁（Lock）当做一种资源，那么这种资源就正好符合“资源互斥”和“资源不可抢夺”的要求，在这种情况下，产生死锁的代码特征就是在持有一个锁的情况下去申请另外一个锁，这通常意味着锁的嵌套。但是，一个线程在已经持有一个锁的情况下再次申请这个锁并不会导致死锁，这是因为 Java 中的锁都是可重入的（Reentrant），这种情形下线程重复申请某个锁是可以成功的

从上诉的四个发生死锁的必要条件来反推，我们只要消除死锁产生的任意一个必要条件就可以规避死锁了。由于锁具有排他性且只能由其持有线程来主动释放，因此由锁导致的死锁只能从消除“占用并等待资源”和消除“循环等待资源”这两个方向入手。以下就来介绍基于这两个思路来规避死锁的方法

## 1、粗锁法

粗锁法即使用粗粒度的锁来代替多个锁。“占用并等待资源”这个条件隐含的情况即：线程在持有一个锁的同时还去申请另一个锁。那么，只要采用一个粒度较粗的锁来替代原先粒度较细的锁，使得涉及的资源都只需要申请一个锁就可以获得，那么就可以避免死锁

对应上诉的哲学家就餐问题，只要将 Philosopher 拿左手边筷子和拿右手边筷子的行为统一放到同个锁内，就可以消除“占用并等待资源”和“循环等待资源”这两个条件了

```kotlin
data class Philosopher(val id: Int, val left: Chopstick, val right: Chopstick) : Thread("Philosopher-$id") {

    companion object {
        private val LOCK = Object()
    }

    override fun run() {
        while (true) {
            think()
            eat()
        }
    }

    private fun eat() {
        synchronized(LOCK) {
            println("$name 拿起了左边的筷子: " + left.id)
            left.pickUp()
            println("$name 拿起了右边的筷子: " + right.id)
            right.pickUp()
            println("$name 开始吃饭.....")
            Tools.randomSleep(10)
            println("$name 吃饭结束!!!!!!!!!!")
            right.putDown()
            left.putDown()
        }
    }

    private fun think() {
        println("$name 思考中....")
        Tools.randomSleep(100)
    }

}
```

粗锁法的缺点就是它明显降低了并发性并可能导致资源浪费。修改过后的代码，每次只有一位哲学家能够吃饭。如果每位哲学家吃饭的耗时相对其思考的时间要长得多，那么在持有筷子的哲学家吃饭结束前，有可能其他哲学家都已经处于等待筷子的状态了（即锁的争用程度比较高，多个线程由于申请不到锁而被暂停，每次锁的争夺可能会经历多次线程上下文切换）。而如果每位哲学家吃饭的耗时相对其思考的时间要短得多，那么就有可能在非持有筷子的哲学家结束思考前，持有筷子的哲学家就已经吃饭结束了（即锁的争用比较低，每次只有一个线程来申请锁，此时就不会由于申请锁而导致线程上下文切换）

即使锁的争用程度比较低，一位哲学家在吃饭的时候也仅需要占用两根筷子，剩下的三根筷子本来还可以提供给另外一位哲学家使用，此时采用粗锁法就明显导致了资源的浪费。因此，粗锁法的适用范围较为有限

## 2、锁排序法

锁排序法的思路是：对所有锁按照一定规则进行排序，所有线程在申请锁之前均按照先后顺序进行申请，以此来消除“循环等待资源”这个条件，从而来规避死锁

例如，对上诉的哲学家问题进行简单化。假设哲学家的数量是 2。哲学家1的左手边是筷子2，右手边是筷子1；哲学家2的左手边是筷子1，右手边是筷子2。当两位哲学家同时拿起左手边的筷子时，此时就会发生死锁。而如果对筷子的申请顺序进行要求，要求哲学家需要先拿起 ID 较小的筷子才能去申请 ID 较大的筷子，那么此时先拿到筷子1的哲学家就可以无竞争地拿到筷子2，从而避免了“循环等待资源”的情况

```kotlin
data class Philosopher(val id: Int, val left: Chopstick, val right: Chopstick) : Thread("Philosopher-$id") {

    private val one: Chopstick

    private val theOther: Chopstick

    init {
        //每位哲学家都对其左右两边的筷子进行排序
        //都按照“先取ID小的筷子再取ID大的筷子”的这种规则来拿筷子
        if (left.id < right.id) {
            one = left
            theOther = right
        } else {
            one = right
            theOther = left
        }
    }

    override fun run() {
        while (true) {
            think()
            eat()
        }
    }

    private fun eat() {
        synchronized(one) {
            println("$name 拿起了左边的筷子: " + left.id)
            left.pickUp()
            synchronized(theOther) {
                println("$name 拿起了右边的筷子: " + right.id)
                right.pickUp()
                println("$name 开始吃饭.....")
                Tools.randomSleep(10)
                println("$name 吃饭结束!!!!!!!!!!")
                right.putDown()
                left.putDown()
            }
        }
    }

    private fun think() {
        println("$name 思考中....")
        Tools.randomSleep(100)
    }

}
```

## 3、资源限时申请

避免死锁的另一种方法是在申请资源时设定一个超时时间，避免无限制地等待资源，从而消除“占用并等待资源”这种情况。当等待时间超出既定的限制时，释放已持有的资源（哲学家放下左手边的筷子转而去继续思考）先给其它线程使用，待后续再重新申请资源

```kotlin
data class Philosopher(val id: Int, val left: Chopstick, val right: Chopstick) : Thread("Philosopher-$id") {

    companion object {

        private val LOCK_MAP = ConcurrentHashMap<Chopstick, ReentrantLock>()

    }

    init {
        LOCK_MAP.putIfAbsent(left, ReentrantLock())
        LOCK_MAP.putIfAbsent(right, ReentrantLock())
    }

    override fun run() {
        while (true) {
            think()
            eat()
        }
    }

    private fun eat() {
        val leftLock = LOCK_MAP[left]!!
        val leftLockAcquired = leftLock.tryLock(10, TimeUnit.MILLISECONDS)
        if (!leftLockAcquired) {
            return
        }
        val rightLock = LOCK_MAP[right]!!
        val rightLockAcquired = rightLock.tryLock(10, TimeUnit.MILLISECONDS)
        if (!rightLockAcquired) {
            leftLock.unlock()
            return
        }
        println("$name 拿起了左边的筷子: " + left.id)
        left.pickUp()
        println("$name 拿起了右边的筷子: " + right.id)
        right.pickUp()
        println("$name 开始吃饭.....")
        Tools.randomSleep(10)
        println("$name 吃饭结束!!!!!!!!!!")
        right.putDown()
        left.putDown()
        leftLock.unlock()
        rightLock.unlock()
    }

    private fun think() {
        println("$name 思考中....")
        Tools.randomSleep(100)
    }

}
```

# 四、死锁的恢复

死锁的恢复有着一定难度，原因主要有以下几点

- 如果代码中使用的是内部锁，或者使用的是显式锁的 `Lock.lock()` 方法，那么这些锁导致的死锁是无法恢复的，此时只能通过重启 Java 虚拟机来停止程序
- 可以通过定义一个工作者线程专门用于检测和恢复死锁。该线程定时检测系统中是否存在死锁，如果存在，则选择一个死锁线程向其发送中断。该中断使得相应的死锁线程被唤醒并抛出 InterruptedException 异常，死锁线程捕获到 InterruptedException 异常后主动释放已持有的资源，从而“消除并等待资源”这个条件。如果该死锁线程释放已持有的线程后依然存在死锁，工作者线程就继续选择一个死锁线程进行中断处理，直到消除死锁。这种方法依赖于发生死锁的线程能够响应中断，而**能响应中断的同时并释放已持有的资源**就意味着在一开始我们就考虑到了可能会发生死锁，那么我们应该在一开始就做好死锁的预防，而不是使死锁线程支持死锁的恢复处理
- 即使死锁线程能够在响应中断的同时并释放已持有的资源，那么检测死锁的工作者线程应该按照什么顺序来中断死锁线程依然是个问题，且被中断的死锁线程可能会丢失其之前已经完成的计算任务，从而导致各种意想不到的情况

这里根据第一节内容中会发生死锁的哲学家问题，来尝试恢复死锁

定义一个死锁检测线程 DeadlockDetector，它会每隔一段时间定时检测当前系统是否发生了死锁，如果发生了的话，则从涉及到死锁的所有线程中选择一个线程向其发送中断请求，被中断的线程内部需要捕获中断异常，然后自动释放其持有的资源，尝试将资源让给其它线程使用，从而打破**占用并等待其它资源**和**资源循环等待**两个条件

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 */
class DeadlockDetector(monitorInterval: Long) : Thread("DeadlockDetector") {

    companion object {

        private val tmb = ManagementFactory.getThreadMXBean()

        //获取发生死锁时涉及到的所有线程
        fun findDeadlockedThreads(): Array<ThreadInfo> {
            val ids = tmb.findDeadlockedThreads()
            return if (tmb.findDeadlockedThreads() == null) arrayOf() else tmb.getThreadInfo(ids) ?: arrayOf()
        }

        fun findThreadById(threadId: Long): Thread? {
            for (thread in getAllStackTraces().keys) {
                if (thread.id == threadId) {
                    return thread
                }
            }
            return null
        }

        //向线程发送中断
        fun interruptThread(threadId: Long): Boolean {
            val thread = findThreadById(threadId)
            if (thread != null) {
                thread.interrupt()
                return true
            }
            return false
        }
    }

    //检测周期，单位为毫秒
    private val monitorInterval: Long

    init {
        isDaemon = true
        this.monitorInterval = monitorInterval
    }

    override fun run() {
        var threadInfoList: Array<ThreadInfo>
        var ti: ThreadInfo?
        var i = 0
        try {
            while (true) {
                threadInfoList = findDeadlockedThreads()
                if (threadInfoList.isNotEmpty()) {
                    ti = threadInfoList[i++ % threadInfoList.size]
                    interruptThread(ti.threadId)
                    continue
                } else {
                    i = 0
                }
                sleep(monitorInterval)
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

}
```

Philosopher 通过 `Lock.lockInterruptibly()` 方法来申请锁，该方法可以响应中断。此时就可以在捕获到中断异常时自动释放已持有的资源

```kotlin
data class Philosopher(val id: Int, val left: Chopstick, val right: Chopstick) : Thread("Philosopher-$id") {

    companion object {

        private val LOCK_MAP = ConcurrentHashMap<Chopstick, ReentrantLock>()

    }

    init {
        LOCK_MAP.putIfAbsent(left, ReentrantLock())
        LOCK_MAP.putIfAbsent(right, ReentrantLock())
    }

    override fun run() {
        while (true) {
            think()
            eat()
        }
    }

    private fun eat() {
        val leftLock = LOCK_MAP[left]!!
        try {
            leftLock.lockInterruptibly()
        } catch (e: InterruptedException) {
            e.printStackTrace()
            println("$name ==========放弃等待左边的筷子: " + left.id)
            return
        }

        println("$name 拿起了左边的筷子: " + left.id)
        left.pickUp()

        val rightLock = LOCK_MAP[right]!!
        try {
            rightLock.lockInterruptibly()
        } catch (e: InterruptedException) {
            e.printStackTrace()
            println("$name ==========放弃等待右边的筷子: " + right.id)

            left.putDown()
            println("$name ====================放下已持有的左边的筷子: " + left.id)

            leftLock.unlock()
            return
        }

        println("$name 拿起了右边的筷子: " + right.id)
        right.pickUp()

        println("$name 开始吃饭.....")
        Tools.randomSleep(10)
        println("$name 吃饭结束!!!!!!!!!!")

        left.putDown()
        right.putDown()

        leftLock.unlock()
        rightLock.unlock()
    }

    private fun think() {
        println("$name 思考中....")
        Tools.randomSleep(100)
    }

}

/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 */
fun main() {
    val philosopherNumber = 5

    val chopstickList = mutableListOf<Chopstick>()
    for (i in 0 until philosopherNumber) {
        chopstickList.add(Chopstick(i))
    }

    val philosopherList = mutableListOf<Philosopher>()
    for (index in 0 until philosopherNumber) {
        val left = chopstickList[index]
        val right = chopstickList.getOrNull(index - 1) ?: chopstickList.last()
        philosopherList.add(Philosopher(index, left, right))
    }

    philosopherList.forEach {
        println(it.name + " 左手边的筷子是：" + it.left + " 右手边的筷子是：" + it.right)
    }

    philosopherList.forEach {
        it.start()
    }

    DeadlockDetector(2000).start()
}
```

从日志输出可以看出来，**线程 Philosopher-4** 在收到中断请求时释放了其持有的资源，但很快又发生了死锁，因为**线程 Philosopher-4** 释放的资源可以由所有需要的线程进行抢夺，可能在 **线程 Philosopher-4** 刚释放了持有的资源时又马上自己抢占到了该资源。最极端的情况就是：每次收到中断的线程均释放了其持有的资源，但随后又马上自己抢占到了该资源，接着该线程（或者其它线程）又收到中断请求，又释放持有的资源，又自己抢占到该资源……如此循环往复。这种情况下所有线程依然无法获得依赖的目标资源，反而由于反复的锁申请和锁释放操作造成多次的线程上下文切换。且释放锁可能会导致线程之前的任务被无效化。所以说，死锁的恢复实际意义不大

```kotlin
Philosopher-0 左手边的筷子是：Chopstick(id=0) 右手边的筷子是：Chopstick(id=4)
Philosopher-1 左手边的筷子是：Chopstick(id=1) 右手边的筷子是：Chopstick(id=0)
Philosopher-2 左手边的筷子是：Chopstick(id=2) 右手边的筷子是：Chopstick(id=1)
Philosopher-3 左手边的筷子是：Chopstick(id=3) 右手边的筷子是：Chopstick(id=2)
Philosopher-4 左手边的筷子是：Chopstick(id=4) 右手边的筷子是：Chopstick(id=3)
Philosopher-0 思考中....
Philosopher-1 思考中....
Philosopher-2 思考中....
Philosopher-3 思考中....
Philosopher-4 思考中....
Philosopher-2 拿起了左边的筷子: 2
Philosopher-3 拿起了左边的筷子: 3
Philosopher-4 拿起了左边的筷子: 4
Philosopher-0 拿起了左边的筷子: 0
Philosopher-2 拿起了右边的筷子: 1
Philosopher-2 开始吃饭.....
Philosopher-2 吃饭结束!!!!!!!!!!
Philosopher-2 思考中....
Philosopher-1 拿起了左边的筷子: 1
Philosopher-3 拿起了右边的筷子: 2
Philosopher-3 开始吃饭.....
Philosopher-3 吃饭结束!!!!!!!!!!
Philosopher-3 思考中....
Philosopher-2 拿起了左边的筷子: 2
Philosopher-3 拿起了左边的筷子: 3
java.lang.InterruptedException
at java.util.concurrent.locks.AbstractQueuedSynchronizer.doAcquireInterruptibly(AbstractQueuedSynchronizer.java:898)
at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireInterruptibly(AbstractQueuedSynchronizer.java:1222)
at java.util.concurrent.locks.ReentrantLock.lockInterruptibly(ReentrantLock.java:335)
at thread.Philosopher.eat(DeadLockDemo.kt:71)
at thread.Philosopher.run(DeadLockDemo.kt:52)
Philosopher-4 ==========放弃等待右边的筷子: 3
Philosopher-4 ====================放下已持有的左边的筷子: 4
Philosopher-4 思考中....
Philosopher-0 拿起了右边的筷子: 4
Philosopher-0 开始吃饭.....
Philosopher-0 吃饭结束!!!!!!!!!!
Philosopher-0 思考中....
Philosopher-1 拿起了右边的筷子: 0
Philosopher-1 开始吃饭.....
Philosopher-4 拿起了左边的筷子: 4
Philosopher-1 吃饭结束!!!!!!!!!!
Philosopher-1 思考中....
Philosopher-0 拿起了左边的筷子: 0
Philosopher-2 拿起了右边的筷子: 1
Philosopher-2 开始吃饭.....
Philosopher-2 吃饭结束!!!!!!!!!!
Philosopher-2 思考中....
Philosopher-3 拿起了右边的筷子: 2
Philosopher-1 拿起了左边的筷子: 1
Philosopher-3 开始吃饭.....
Philosopher-3 吃饭结束!!!!!!!!!!
Philosopher-3 思考中....
Philosopher-2 拿起了左边的筷子: 2
Philosopher-3 拿起了左边的筷子: 3
java.lang.InterruptedException
at java.util.concurrent.locks.AbstractQueuedSynchronizer.doAcquireInterruptibly(AbstractQueuedSynchronizer.java:898)
at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireInterruptibly(AbstractQueuedSynchronizer.java:1222)
at java.util.concurrent.locks.ReentrantLock.lockInterruptibly(ReentrantLock.java:335)
at thread.Philosopher.eat(DeadLockDemo.kt:71)
at thread.Philosopher.run(DeadLockDemo.kt:52)
Philosopher-4 ==========放弃等待右边的筷子: 3
Philosopher-4 ====================放下已持有的左边的筷子: 4
Philosopher-4 思考中....
Philosopher-0 拿起了右边的筷子: 4
Philosopher-0 开始吃饭.....
Philosopher-0 吃饭结束!!!!!!!!!!
Philosopher-0 思考中....
Philosopher-4 拿起了左边的筷子: 4
Philosopher-0 拿起了左边的筷子: 0
java.lang.InterruptedException
at java.util.concurrent.locks.AbstractQueuedSynchronizer.doAcquireInterruptibly(AbstractQueuedSynchronizer.java:898)
at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireInterruptibly(AbstractQueuedSynchronizer.java:1222)
at java.util.concurrent.locks.ReentrantLock.lockInterruptibly(ReentrantLock.java:335)
at thread.Philosopher.eat(DeadLockDemo.kt:71)
at thread.Philosopher.run(DeadLockDemo.kt:52)
Philosopher-4 ==========放弃等待右边的筷子: 3
Philosopher-4 ====================放下已持有的左边的筷子: 4
Philosopher-4 思考中....
Philosopher-0 拿起了右边的筷子: 4
Philosopher-0 开始吃饭.....
Philosopher-0 吃饭结束!!!!!!!!!!
Philosopher-0 思考中....
Philosopher-0 拿起了左边的筷子: 0
Philosopher-0 拿起了右边的筷子: 4
Philosopher-0 开始吃饭.....
Philosopher-0 吃饭结束!!!!!!!!!!
Philosopher-0 思考中....
Philosopher-0 拿起了左边的筷子: 0
Philosopher-0 拿起了右边的筷子: 4
Philosopher-0 开始吃饭.....
Philosopher-0 吃饭结束!!!!!!!!!!
Philosopher-0 思考中....
Philosopher-0 拿起了左边的筷子: 0
Philosopher-0 拿起了右边的筷子: 4
Philosopher-0 开始吃饭.....
Philosopher-0 吃饭结束!!!!!!!!!!
Philosopher-0 思考中....
Philosopher-0 拿起了左边的筷子: 0
Philosopher-4 拿起了左边的筷子: 4
java.lang.InterruptedException
at java.util.concurrent.locks.AbstractQueuedSynchronizer.doAcquireInterruptibly(AbstractQueuedSynchronizer.java:898)
at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireInterruptibly(AbstractQueuedSynchronizer.java:1222)
at java.util.concurrent.locks.ReentrantLock.lockInterruptibly(ReentrantLock.java:335)
at thread.Philosopher.eat(DeadLockDemo.kt:71)
at thread.Philosopher.run(DeadLockDemo.kt:52)
Philosopher-4 ==========放弃等待右边的筷子: 3
Philosopher-4 ====================放下已持有的左边的筷子: 4
Philosopher-4 思考中....
Philosopher-4 拿起了左边的筷子: 4


····
```

# 五、锁死

等待线程由于唤醒其所需的条件永远无法成立，或者是其它线程无法唤醒这个线程导致其一直处于非运行状态（线程并未终止）从而任务一直取得进展，那么我们称这个线程被锁死

锁死和死锁之间有着共同的外在表现：故障线程一直处于非运行状态而使得其任务无法进展。死锁针对的是多个线程，而锁死可能只是作用在一个线程上。例如，一个调用了 `Object.wait()` 处于等待状态的线程，由于发生异常或者是代码缺陷，导致一直没有外部线程调用 `Object.notify()` 方法来唤醒等待线程，使得线程一直处于等待状态无法运行，此时就可以说该线程被锁死

锁死和死锁的产生条件是不同的，即便是在产生死锁的所有必要条件都不成立的情况下（此时死锁不可能发生），锁死仍可能出现。因此应对死锁的办法未必能够用来避免锁死现象的发生。按照锁死产生的条件来分，锁死包括信号丢失锁死和嵌套监视器锁死

## 1、信号丢失锁死

信号丢失锁死是由于没有相应的通知线程来唤醒等待线程而使等待线程一直处于等待状态的一种活性故障

例如，某个等待线程在执行 `Object.wait()` 前没有对保护条件进行判断，而此时保护条件实际上已经成立了，然而此后可能并无其他线程会来唤醒等待线程，因为在等待线程获得 Object 内部锁之前保护条件已经是处于成立状态了，这就使得等待线程一直处于等待状态，其任务一直无法取得进展

信号丢失锁死的另外一个常见例子是由于 `CountDownLatch.countDown()` 没有放在 **finally** 块中，而如果 `CountDownLatch.countDown()` 的执行线程运行时抛出未捕获的异常时， `CountDownLatch.await()` 的执行线程就会一直处于等待状态从而任务一直无法取得进展

例如，对于以下代码，当 ServiceB 抛出异常时，main 线程就会由于一直无法收到唤醒通知从而一直处于等待状态

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

    private val countDownLatch = CountDownLatch(2)

    private val serviceList = mutableListOf<AbstractService>()

    init {
        serviceList.add(ServiceA("ServiceA", countDownLatch))
        serviceList.add(ServiceB("ServiceB", countDownLatch))
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
//            try {
//                success = doTask()
//            } finally {
//                countDownLatch.countDown()
//            }
            success = doTask()
            countDownLatch.countDown()
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
        Thread.sleep(3000)
        if (Random.nextBoolean()) {
            throw  RuntimeException("$serviceName failed")
        } else {
            println("${serviceName}执行完毕")
        }
        return true
    }

}
```

## 2、嵌套监视器锁死

嵌套监视器锁死是嵌套锁导致等待线程永远无法被唤醒的一种活性故障

来看以下伪代码。假设存在一个等待线程，其先后持有了 monitorX 和 monitorY 两个不同的锁，当等待线程监测到当前执行条件不成立时，调用了 `monitorY.wait()` 等待通知线程来唤醒自身，并同时释放了锁 monitorY

```kotlin
synchronized(monitorX) {
    //...
    synchronized(monitorY) {
        while (!somethingOk) {
            monitorY.wait()
        }
        //执行目标行为
    }
}
```

相应的通知线程其伪代码如下所示。通知线程需要持有了 monitorX 和 monitorY 两个锁才能执行到 `monitorY.notifyAll()` 这行代码来唤醒等待线程。而等待线程执行 `monitorY.wait()` 时仅会释放 monitorY，而不会释放 monitorX。这使得通知线程由于一直获得 monitorX， 从而导致等待线程一直无法被唤醒而一直处于 BLOCKED 状态

```kotlin
synchronized(monitorX) {
    //...
    synchronized(monitorY) {
        //...
        somethingOk = true
        monitorY.notifyAll()
        //...
    }
}
```

这种由于嵌套锁导致通知线程始终无法唤醒等待线程的活性故障就被称为嵌套监视器锁死

# 六、线程饥饿

线程饥饿是指线程一直无法获得所需资源从而导致任务无法取得进展的一种活性故障现象

产生线程饥饿的一种情况是：线程一直没有被分配到处理器时间片。这种情况一般是由于处理器时间片一直被高优先级的线程抢占，低优先级的线程一直无法获得运行机会，此时即发生了线程饥饿现象。`Thread` 类提供了修改线程优先级的成员方法`setPriority(Int)`，定义了整数一到十之间的十个优先级级别。不同的操作系统会有不同的线程优先级等级，JVM 会把这 Thread 类的十个优先级级别映射到具体的操作系统所定义的线程优先级关系上。但是我们所设置的线程优先级对线程调度器来说只是一个建议，当我们将一个线程设置为高优先级时，既可能会被线程调度器忽略，也可能会使该线程过度优先执行而别的线程一直得不到处理器时间片，从而导致线程饥饿。因此我们应该尽量避免修改线程的优先级

把锁看做一种资源，那么死锁也是一种线程饥饿。死锁的结果是所有故障线程都无法获得其所需的全部锁，从而使得其任务一直无法取得进展，这就相当于线程无法获得所需的全部资源从而导致任务无法取得进展，即产生了线程饥饿

发生线程饥饿并不一定同时存在死锁。因为线程饥饿可能只发生在一个线程上（例如上述的低优先级线程无法获得时间片），且即使是同时发生在多个线程上，也可能并不满足死锁发生的必要条件之一：循环等待资源，因为此时涉及到的多个线程所等待的资源可能并没有相互依赖关系

# 七、活锁

活锁指的是任务和任务的执行线程均没有被阻塞，但由于某些条件没有满足，导致线程一直在重复**尝试—失败—尝试**的过程，任务一直无法取得进展。也就是说，产生活锁的线程虽然处于 Runnable 状态，但是一直在做无用功

例如，对于上述的哲学家问题，假设某位哲学家“**比较有礼貌**”，当其拿起了左手边的筷子时，如果恰好有其他哲学家需要这根筷子，**有礼貌的哲学家**就主动放下筷子，让给其他哲学家使用。在最极端的情况下，每当有礼貌的哲学家一想要吃饭并拿起左手边的筷子时，就有其他哲学家需要这根筷子，此时有礼貌的哲学就会一直处于**拿起筷子-放下筷子-拿起筷子**这样一个循环过程中，导致一直无法吃饭。此时并没有发生死锁，但对于有礼貌的哲学家所代表的线程来说就是发生了活锁