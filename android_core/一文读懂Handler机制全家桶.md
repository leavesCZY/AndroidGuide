Handler 在整个 Android 开发体系中占据着很重要的地位，对开发者来说起到的作用很明确，就是为了实现线程切换或者是执行延时任务，稍微更高级一点的用法可能是为了保证多个任务在执行时的有序性。由于 Android 系统中的主线程有特殊地位，所以像 EventBus 和 Retrofit 这类并非 Android 独有的三方库，都是通过 Handler 来实现对 Android 系统的特殊平台支持。大部分开发者都已经对如何使用 Handler 很熟悉了，这里就再来了解下其内部具体是如何实现的

**本文基于 Android API 30（即 Android 11）的系统源码进行讲解**

### 一、动手实现 Handler

本文不会一上来就直接介绍源码，而是会先根据我们想要实现的效果来反推源码，一步步来自己动手实现一个简单的 Handler

#### 1、Message

首先，我们需要有个载体来表示要执行的任务，就叫它 Message 吧，Message 应该有什么参数呢？

- 需要有一个唯一标识，因为要执行的任务可能有多个，我们要分得清哪个是哪个，用个 Int 类型变量就足够表示了
- 需要能够承载数据，需要发送的数据类型会有很多种可能，那就直接用一个 Object 类型变量来表示吧，由开发者自己在使用时再来强转类型
- 需要有一个 long 类型变量来表示任务的执行时间戳

所以，Message 类就应该至少包含以下几个字段：

```java
/**
 * @Author: leavesC
 * @Date: 2020/12/1 13:31
 * @Desc:
 * GitHub：https://github.com/leavesC
 */
public final class Message {
    //唯一标识
    public int what;
    //数据
    public Object obj;
    //时间戳
    public long when;
}
```

#### 2、MessageQueue

因为 Message 并不是发送了就能够马上被消费掉，所以就肯定要有个可以用来存放的地方，就叫它 MessageQueue 吧，即消息队列。Message 可能需要延迟处理，那么 MessageQueue 在保存 Message 的时候就应该按照时间戳的大小来顺序存放，时间戳小的 Message 放在队列的头部，在消费 Message 的时候就直接从队列头取值即可

那么用什么数据结构来存放 Message 比较好呢？

- 用数组？不太合适，数组虽然在遍历的时候会比较快，但需要预先就申请固定的内存空间，导致在插入数据和移除数据时可能需要移动大量数据。而 MessageQueue 可能随时会收到数量不定、时间戳大小不定的 Message，消费完 Message 后还需要将该其移出队列，所以使用数组并不合适
- 用链表？好像可以，链表在插入数据和移除数据时只需要改变指针的引用即可，不需要移动数据，内存空间也只需要按需申请即可。虽然链表在随机访问的时候性能不高，但是对于 MessageQueue 而言无所谓，因为在消费 Message 的时候也只需要取队列头的值，并不需要随机访问

好了，既然决定用链表结构，那么 Message 就需要增加一个字段用于指向下一条消息才行

```java
/**
 * @Author: leavesC
 * @Date: 2020/12/1 13:31
 * @Desc:
 * GitHub：https://github.com/leavesC
 */
public final class Message {
    //唯一标识
    public int what;
    //数据
    public Object obj;
    //时间戳
    public long when;
	//下一个节点
    public Message next;
}
```

MessageQueue 需要提供一个 `enqueueMessage`方法用来向链表插入 Message，由于存在多个线程同时向队列发送消息的可能，所以方法内部还需要做下线程同步才行

```java
/**
 * @Author: leavesC
 * @Date: 2020/12/1 13:31
 * @Desc:
 * GitHub：https://github.com/leavesC
 */
public class MessageQueue {

    //链表中的第一条消息
    private Message mMessages;
    
    void enqueueMessage(Message msg, long when) {
        synchronized (this) {
            Message p = mMessages;
            //如果链表是空的，或者处于队头的消息的时间戳比 msg 要大，则将 msg 作为链表头部
            if (p == null || when == 0 || when < p.when) {
                msg.next = p;
                mMessages = msg;
            } else {
                Message prev;
                //从链表头向链表尾遍历，寻找链表中第一条时间戳比 msg 大的消息，将 msg 插到该消息的前面
                for (; ; ) {
                    prev = p;
                    p = p.next;
                    if (p == null || when < p.when) {
                        break;
                    }
                }
                msg.next = p;
                prev.next = msg;
            }
        }
    }
}
```

此外，MessageQueue 要有一个可以获取队头消息的方法才行，就叫做`next()`吧。外部有可能会随时向 MessageQueue 发送 Message，`next()`方法内部就直接来开启一个无限循环来反复取值吧。如果当前队头的消息可以直接处理的话（即消息的时间戳小于或等于当前时间），那么就直接返回队头消息。而如果队头消息的时间戳比当前时间还要大（即队头消息是一个延时消息），那么就计算当前时间和队头消息的时间戳的差值，计算 `next()` 方法需要阻塞等待的时间，调用 `nativePollOnce()`方法来等待一段时间后再继续循环遍历

```kotlin
    //用来标记 next() 方法是否正处于阻塞等待的状态
    private boolean mBlocked = false;

    Message next() {
        int nextPollTimeoutMillis = 0;
        for (; ; ) {
            nativePollOnce(nextPollTimeoutMillis);
            synchronized (this) {
                //当前时间
                final long now = SystemClock.uptimeMillis();
                
                Message msg = mMessages;
                if (msg != null) {
                    if (now < msg.when) {
                        //如果当前时间还未到达消息的的处理时间，那么就计算还需要等待的时间
                        nextPollTimeoutMillis = (int) Math.min(msg.when - now, Integer.MAX_VALUE);
                    } else {
                        //可以处理队头的消息了，第二条消息成为队头
                        mMessages = msg.next;
                        msg.next = null;
                        mBlocked = false;
                        return msg;
                    }
                } else {
                    // No more messages.
                    nextPollTimeoutMillis = -1;
                }
                mBlocked = true;
            }
        }
    }

    //将 next 方法的调用线程休眠指定时间
    private void nativePollOnce(long nextPollTimeoutMillis) {

    }
```

此时就需要考虑到一种情形：**当 `next()`还处于阻塞状态的时候，外部向消息队列插入了一个可以立即处理或者是阻塞等待时间比较短的 Message**。此时就需要唤醒休眠的线程，因此 `enqueueMessage`还需要再改动下，增加判断是否需要唤醒`next()`方法的逻辑

```java
	void enqueueMessage(Message msg, long when) {
        synchronized (this) {
            //用于标记是否需要唤醒 next 方法
            boolean needWake = false;         
            Message p = mMessages;
            //如果链表是空的，或者处于队头的消息的时间戳比 msg 要大，则将 msg 作为链表头部
            if (p == null || when == 0 || when < p.when) {
                msg.next = p;
                mMessages = msg;     
                //需要唤醒
                needWake = mBlocked;
            } else {
                Message prev;
                //从链表头向链表尾遍历，寻找链表中第一条时间戳比 msg 大的消息，将 msg 插到该消息的前面
                for (; ; ) {
                    prev = p;
                    p = p.next;
                    if (p == null || when < p.when) {
                        break;
                    }
                }
                msg.next = p;
                prev.next = msg;
            }  
            if (needWake) {
                //唤醒 next() 方法
                nativeWake();
            }
        }
    }

    //唤醒 next() 方法
    private void nativeWake() {

    }
```

#### 3、Handler

既然存放消息的地方已经确定就是 MessageQueue 了，那么自然就还需要有一个类可以用来向 MessageQueue 发送消息了，就叫它 Handler 吧。Handler 可以实现哪些功能呢？

- 希望除了可以发送 Message 类型的消息外还可以发送 Runnable 类型的消息。这个简单，Handler 内部将 Runnable 包装为 Message 即可
- 希望可以发送延时消息，以此来执行延时任务。这个也简单，用 Message 内部的 when 字段来标识希望任务执行时的时间戳即可
- 希望可以实现线程切换，即从子线程发送的 Message 可以在主线程被执行，反过来也一样。这个也不难，子线程可以向一个特定的 mainMessageQueue 发送消息，然后让主线程负责循环从该队列中取消息并执行即可，这样不就实现了线程切换了吗？

**所以说，Message 的定义和发送是由 Handler 来完成的，但 Message 的分发则可以交由其他线程来完成**

根据以上需求：**Runnable 要能够包装为 Message 类型，Message 的处理逻辑要交由 Handler 来定义**，所以 Message 就还需要增加两个字段才行

```java
/**
 * @Author: leavesC
 * @Date: 2020/12/1 13:31
 * @Desc:
 * GitHub：https://github.com/leavesC
 */
public final class Message {
    //唯一标识
    public int what;
    //数据
    public Object obj;
    //时间戳
    public long when;
	//下一个节点
    public Message next;
	//用于将 Runnable 包装为 Message
    public Runnable callback;
    //指向 Message 的发送者，同时也是 Message 的最终处理者
    public Handler target;
}
```

Handler 至少需要包含几个方法：用于发送 Message 和 Runnable 的方法、用来处理消息的 `handleMessage` 方法、用于分发消息的 `dispatchMessage`方法

```java
/**
 * @Author: leavesC
 * @Date: 2020/12/1 13:31
 * @Desc:
 * GitHub：https://github.com/leavesC
 */
public class Handler {

    private MessageQueue mQueue;
    
    public Handler(MessageQueue mQueue) {
        this.mQueue = mQueue;
    }

    public final void post(Runnable r) {
        sendMessageDelayed(getPostMessage(r), 0);
    }

    public final void postDelayed(Runnable r, long delayMillis) {
        sendMessageDelayed(getPostMessage(r), delayMillis);
    }

    public final void sendMessage(Message r) {
        sendMessageDelayed(r, 0);
    }

    public final void sendMessageDelayed(Message msg, long delayMillis) {
        if (delayMillis < 0) {
            delayMillis = 0;
        }
        sendMessageAtTime(msg, SystemClock.uptimeMillis() + delayMillis);
    }

    public void sendMessageAtTime(Message msg, long uptimeMillis) {
        msg.target = this;
        mQueue.enqueueMessage(msg, uptimeMillis);
    }

    private static Message getPostMessage(Runnable r) {
        Message m = new Message();
        m.callback = r;
        return m;
    }

    //由外部来重写该方法，以此来消费 Message
    public void handleMessage(Message msg) {

    }

    //用于分发消息
    public void dispatchMessage(Message msg) {
        if (msg.callback != null) {
            msg.callback.run();
        } else {
            handleMessage(msg);
        }
    }

}
```

之后，子线程就可以像这样来使用 Handler 了：**将子线程持有的 Handler 对象和主线程关联的 mainMessageQueue 绑定在一起，主线程负责循环从 mainMessageQueue 取出 Message 后再来调用 Handler 的 `dispatchMessage` 方法，以此实现线程切换的目的**

```java
        Handler handler = new Handler(mainThreadMessageQueue) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1: {
                        String ob = (String) msg.obj;
                        break;
                    }
                    case 2: {
                        List<String> ob = (List<String>) msg.obj;
                        break;
                    }
                }
            }
        };
        Message messageA = new Message();
        messageA.what = 1;
        messageA.obj = "https://github.com/leavesC";
        Message messageB = new Message();
        messageB.what = 2;
        messageB.obj = new ArrayList<String>();
        handler.sendMessage(messageA);
        handler.sendMessage(messageB);
```

#### 4、Looper

现在就再来想想怎么让 Handler 拿到和主线程关联的 MessageQueue，以及主线程怎么从 MessageQueue 获取 Message 并回调 Handler。这之间就一定需要一个中转器，就叫它 Looper 吧。Looper 具体需要实现什么功能呢？

- 每个 Looper 对象应该都是对应一个独有的 MessageQueue 实例和 Thread 实例，这样子线程和主线程才可以互相发送 Message 交由对方线程处理
- Looper 内部需要开启一个无限循环，其关联的线程就负责从 MessageQueue 循环获取 Message 进行处理
- 因为主线程较为特殊，所以和主线程关联的 Looper 对象要能够被子线程直接获取到，可以考虑将其作为静态变量存着

这样，Looper 的大体框架就出来了。通过 ThreadLocal 来为不同的线程单独维护一个 Looper 实例，每个线程通过 `prepare()`方法来初始化本线程独有的 Looper 实例 ，再通过 `myLooper()`方法来获取和当前线程关联的 Looper 对象，和主线程关联的 `sMainLooper` 作为静态变量存在，方便子线程获取

```java
/**
 * @Author: leavesC
 * @Date: 2020/12/1 13:31
 * @Desc:
 * GitHub：https://github.com/leavesC
 */
final class Looper {

    final MessageQueue mQueue;

    final Thread mThread;

    private static Looper sMainLooper;

    static final ThreadLocal<Looper> sThreadLocal = new ThreadLocal<Looper>();

    private Looper() {
        mQueue = new MessageQueue();
        mThread = Thread.currentThread();
    }

    public static void prepare() {
        if (sThreadLocal.get() != null) {
            throw new RuntimeException("Only one Looper may be created per thread");
        }
        sThreadLocal.set(new Looper());
    }

    public static void prepareMainLooper() {
        prepare();
        synchronized (Looper.class) {
            if (sMainLooper != null) {
                throw new IllegalStateException("The main Looper has already been prepared.");
            }
            sMainLooper = myLooper();
        }
    }

    public static Looper getMainLooper() {
        synchronized (Looper.class) {
            return sMainLooper;
        }
    }

    public static Looper myLooper() {
        return sThreadLocal.get();
    }

}
```

Looper 还需要有一个用于循环从 MessageQueue 获取消息并处理的方法，就叫它`loop()`吧。其作用也能简单，就是循环从 MessageQueue 中取出 Message，然后将 Message 再反过来分发给 Handler 即可

```java
	public static void loop() {
        final Looper me = myLooper();
        if (me == null) {
            throw new RuntimeException("No Looper; Looper.prepare() wasn't called on this thread.");
        }
        final MessageQueue queue = me.mQueue;
        for (; ; ) {
            Message msg = queue.next();//可能会阻塞
            msg.target.dispatchMessage(msg);
        }
    }
```

这样，主线程就先通过调用`prepareMainLooper()`来完成 sMainLooper 的初始化，然后调用`loop()`开始向 mainMessageQueue 循环取值并进行处理，没有消息的话主线程就暂时休眠着。子线程拿到 sMainLooper 后就以此来初始化 Handler，这样子线程向 Handler 发送的消息就都会被存到 mainMessageQueue 中，最终在主线程被消费掉

#### 5、做一个总结

这样一步步走下来后，读者对于 Message、MessageQueue、Handler、Looper 这四个类的定位就应该都很清晰了吧？不同线程之间就可以依靠拿到对方的 Looper 来实现消息的跨线程处理了

例如，对于以下代码，即使 Handler 是在 otherThread 中进行初始化，但 `handleMessage` 方法最终是会在 mainThread 被调用执行的，

```java
	    Thread mainThread = new Thread() {
            @Override
            public void run() {
                //初始化 mainLooper
                Looper.prepareMainLooper();
                //开启循环
                Looper.loop();
            }
        };

        Thread otherThread = new Thread() {
            @Override
            public void run() {
                Looper mainLooper = Looper.getMainLooper();
                Handler handler = new Handler(mainLooper.mQueue) {
                    @Override
                    public void handleMessage(Message msg) {
                        switch (msg.what) {
                            case 1: {
                                String ob = (String) msg.obj;
                                break;
                            }
                            case 2: {
                                List<String> ob = (List<String>) msg.obj;
                                break;
                            }
                        }
                    }
                };
                Message messageA = new Message();
                messageA.what = 1;
                messageA.obj = "https://github.com/leavesC";
                Message messageB = new Message();
                messageB.what = 2;
                messageB.obj = new ArrayList<String>();
                handler.sendMessage(messageA);
                handler.sendMessage(messageB);
            }
        };
```

再来做个简单的总结：

- Message：用来表示要执行的任务
- Handler：子线程持有的 Handler 如果绑定到的是主线程的 MessageQueue 的话，那么子线程发送的 Message 就可以由主线程来消费，以此来实现线程切换，执行 UI 更新操作等目的
- MessageQueue：即消息队列，通过 Handler 发送的消息并非都是立即执行的，需要先按照 Message 的优先级高低（延时时间的长短）保存到 MessageQueue 中，之后再来依次执行
- Looper：Looper 用于从 MessageQueue 中循环获取 Message 并将之传递给消息处理者（即消息发送者 Handler 本身）来进行消费，每条 Message 都有个 target 变量用来指向 Handler，以此把 Message 和其处理者关联起来。不同线程之间通过互相拿到对方的 Looper 对象，以此来实现跨线程发送消息

**有了以上的认知基础后，下面就来看看实际的源码实现 ~ ~**

### 二、Handler 源码

#### 1、Handler  如何初始化

Handler 的构造函数一共有七个，除去**两个已经废弃的和三个隐藏的**，实际上开发者可以使用的只有两个。而不管是使用哪个构造函数，最终的目的都是为了完成 **mLooper、mQueue、mCallback、mAsynchronous** 这四个常量的初始化，同时也可以看出来 MessageQueue 是由 Looper 来完成初始化的，而且 Handler 对于 Looper 和 MessageQueue 都是一对一的关系，一旦初始化后就不可改变

**大部分开发者使用的应该都是 Handler 的无参构造函数，而在 Android 11 中 Handler 的无参构造函数已经被标记为废弃的了**。Google 官方更推荐的做法是通过显式传入 Looper 对象来完成初始化，而非隐式使用当前线程关联的 Looper

> Handler 对于 Looper 和 MessageQueue 都是一对一的关系，但是 Looper 和 MessageQueue 对于 Handler 可以是一对多的关系，这个后面会讲到

```java
	@UnsupportedAppUsage
    final Looper mLooper;
    final MessageQueue mQueue;
    @UnsupportedAppUsage
    final Callback mCallback;
    final boolean mAsynchronous;

	//省略其它构造函数

    /**
     * @hide
     */
    public Handler(@Nullable Callback callback, boolean async) {
        if (FIND_POTENTIAL_LEAKS) {
            final Class<? extends Handler> klass = getClass();
            if ((klass.isAnonymousClass() || klass.isMemberClass() || klass.isLocalClass()) &&
                    (klass.getModifiers() & Modifier.STATIC) == 0) {
                Log.w(TAG, "The following Handler class should be static or leaks might occur: " +
                    klass.getCanonicalName());
            }
        }

        mLooper = Looper.myLooper();
        if (mLooper == null) {
            throw new RuntimeException(
                "Can't create handler inside thread " + Thread.currentThread()
                        + " that has not called Looper.prepare()");
        }
        mQueue = mLooper.mQueue;
        mCallback = callback;
        mAsynchronous = async;
    }
```

#### 2、Looper  如何初始化

在初始化 Handler 时，如果外部调用的构造函数没有传入 Looper，那就会调用`Looper.myLooper()`来获取和当前线程关联的 Looper 对象，再从 Looper 中取 MessageQueue。如果获取到的 Looper 对象为 null 就会抛出异常。根据异常信息 `Can't create handler inside thread that has not called Looper.prepare()` 可以看出来，在初始化 Handler 之前需要先调用 `Looper.prepare()`完成 Looper 的初始化

走进 `Looper` 类中可以看到，`myLooper()`方法是 Looper 类的静态方法，其只是单纯地从 `sThreadLocal` 变量中取值并返回而已。`sThreadLocal` 又是通过 `prepare(boolean)` 方法来进行初始化赋值的，且只能赋值一次，重复调用将抛出异常

我们知道，ThreadLocal 的特性就是可以为不同的线程分别维护单独的一个变量实例，所以，不同的线程就会分别对应着不同的 Looper 对象，是一一对应的关系

```java
  	@UnsupportedAppUsage
    static final ThreadLocal<Looper> sThreadLocal = new ThreadLocal<Looper>(); 

    /**
     * Return the Looper object associated with the current thread.  Returns
     * null if the calling thread is not associated with a Looper.
     */
    public static @Nullable Looper myLooper() {
        return sThreadLocal.get();
    }

    /** Initialize the current thread as a looper.
      * This gives you a chance to create handlers that then reference
      * this looper, before actually starting the loop. Be sure to call
      * {@link #loop()} after calling this method, and end it by calling
      * {@link #quit()}.
      */
    public static void prepare() {
        prepare(true);
    }

    private static void prepare(boolean quitAllowed) {
        if (sThreadLocal.get() != null) {
             //只允许赋值一次
        	//如果重复赋值则抛出异常
            throw new RuntimeException("Only one Looper may be created per thread");
        }
        sThreadLocal.set(new Looper(quitAllowed));
    }
```

此外，Looper 类的构造函数也是私有的，会初始化两个常量值：mQueue 和 mThread，这说明了 Looper 对于 MessageQueue 和 Thread 都是一一对应的关系，关联之后不能改变

```java
	@UnsupportedAppUsage
    final MessageQueue mQueue;

	final Thread mThread;    

    private Looper(boolean quitAllowed) {
        mQueue = new MessageQueue(quitAllowed);
        mThread = Thread.currentThread();
    }
```

在日常开发中，我们在通过 Handler 来执行 UI 刷新操作时，经常使用的是 Handler 的无参构造函数，那么此时肯定就是使用了和主线程关联的 Looper 对象，对应 Looper 类中的静态变量 `sMainLooper`

```java
    @UnsupportedAppUsage
    private static Looper sMainLooper;  // guarded by Looper.class

	//被标记为废弃的原因是因为 sMainLooper 会交由 Android 系统自动来完成初始化，外部不应该主动来初始化
    @Deprecated
    public static void prepareMainLooper() {
        prepare(false);
        synchronized (Looper.class) {
            if (sMainLooper != null) {
                throw new IllegalStateException("The main Looper has already been prepared.");
            }
            sMainLooper = myLooper();
        }
    }

    /**
     * Returns the application's main looper, which lives in the main thread of the application.
     */
    public static Looper getMainLooper() {
        synchronized (Looper.class) {
            return sMainLooper;
        }
    }
```

`prepareMainLooper()`就用于为主线程初始化 Looper 对象，该方法又是由 ActivityThread 类的 `main()` 方法来调用的。该 `main()` 方法即 Java 程序的运行起始点，所以当应用启动时系统就自动为我们在主线程做好了 mainLooper 的初始化，而且已经调用了`Looper.loop()`方法开启了消息的循环处理，应用在使用过程中的各种交互逻辑（例如：屏幕的触摸事件、列表的滑动等）就都是在这个循环里完成分发的

正是因为 Android 系统已经自动完成了主线程 Looper 的初始化，所以我们在主线程中才可以直接使用 Handler 的无参构造函数来完成 UI 相关事件的处理

```java
public final class ActivityThread extends ClientTransactionHandler {
 
    public static void main(String[] args) {
        ···
        Looper.prepareMainLooper();
        ···
        Looper.loop();
        throw new RuntimeException("Main thread loop unexpectedly exited");
    }
    
}
```

#### 3、Handler 发送消息

Handler 用于发送消息的方法非常多，有十几个，其中大部分最终调用到的都是 `sendMessageAtTime()` 方法。uptimeMillis 即 Message 具体要执行的时间戳，如果该时间戳比当前时间大，那么就意味着要执行的是延迟任务。如果为 mQueue 为 null，就会打印异常信息并直接返回，因为 Message 是需要交由 MessageQueue 来处理的

```java
 	public boolean sendMessageAtTime(@NonNull Message msg, long uptimeMillis) {
        MessageQueue queue = mQueue;
        if (queue == null) {
            RuntimeException e = new RuntimeException(
                    this + " sendMessageAtTime() called with no mQueue");
            Log.w("Looper", e.getMessage(), e);
            return false;
        }
        return enqueueMessage(queue, msg, uptimeMillis);
    }
```

需要注意 `msg.target = this` 这句代码，target 指向了**发送消息的主体**，即 Handler 对象本身，即由 Handler 对象发给 MessageQueue 的消息最后还是要交由 Handler 对象本身来处理

```java
	private boolean enqueueMessage(@NonNull MessageQueue queue, @NonNull Message msg,
            long uptimeMillis) {
        msg.target = this;
        msg.workSourceUid = ThreadLocalWorkSource.getUid();

        if (mAsynchronous) {
            msg.setAsynchronous(true);
        }
        //将消息交由 MessageQueue 处理
        return queue.enqueueMessage(msg, uptimeMillis);
    }
```

#### 4、MessageQueue

MessageQueue 通过 `enqueueMessage` 方法来接收消息

- 因为存在多个线程同时往一个 MessageQueue 发送消息的可能，所以 `enqueueMessage` 内部肯定需要进行线程同步
- 可以看出 MessageQueue 内部是以链表的结构来存储 Message 的（Message.next），根据 Message 的时间戳大小来决定其在消息队列中的位置
- mMessages 代表的是消息队列中的第一条消息。如果 mMessages 为空（消息队列是空的），或者 mMessages 的时间戳要比新消息的时间戳大，则将新消息插入到消息队列的头部；如果 mMessages 不为空，则寻找消息列队中第一条触发时间比新消息晚的非空消息，将新消息插到该消息的前面

到此，一个按照时间戳大小进行排序的消息队列就完成了，后边要做的就是从消息队列中依次取出消息进行处理了

```java
    boolean enqueueMessage(Message msg, long when) {
        if (msg.target == null) {
            throw new IllegalArgumentException("Message must have a target.");
        }

        synchronized (this) {
            ···
            msg.markInUse();
            msg.when = when;
            Message p = mMessages;
            //用于标记是否需要唤醒线程
            boolean needWake;
            //如果链表是空的，或者处于队头的消息的时间戳比 msg 要大，则将 msg 作为链表头部
            //when == 0 说明 Handler 调用的是 sendMessageAtFrontOfQueue 方法，直接将 msg 插到队列头部 
            if (p == null || when == 0 || when < p.when) {
                // New head, wake up the event queue if blocked.
                msg.next = p;
                mMessages = msg;
                needWake = mBlocked;
            } else {
                //如果当前线程处于休眠状态 + 队头消息是屏障消息 + msg 是异步消息
                //那么就需要唤醒线程
                needWake = mBlocked && p.target == null && msg.isAsynchronous();
                
                Message prev;
                //从链表头向链表尾遍历，寻找链表中第一条时间戳比 msg 大的消息，将 msg 插到该消息的前面
                for (;;) {
                    prev = p;
                    p = p.next;
                    if (p == null || when < p.when) {
                        break;
                    }
                    if (needWake && p.isAsynchronous()) {
                        //如果在 msg 之前队列中还有异步消息那么就不需要主动唤醒
                        //因为已经设定唤醒时间了
                        needWake = false;
                    }
                }
                msg.next = p; // invariant: p == prev.next
                prev.next = msg;
            }

            // We can assume mPtr != 0 because mQuitting is false.
            if (needWake) {
                nativeWake(mPtr);
            }
        }
        return true;
    }
```

知道了 Message 是如何保存的了，再来看下 MessageQueue 是如何取出 Message 并回调给 Handler 的。在 MessageQueue 中读取消息的操作对应的是`next()` 方法。`next()` 方法内部开启了一个无限循环，如果消息队列中没有消息或者是队头消息还没到可以处理的时间，该方法就会导致 Loop 线程休眠挂起，直到条件满足后再重新遍历消息

```java
	@UnsupportedAppUsage
    Message next() {
        ···
        for (;;) {
            if (nextPollTimeoutMillis != 0) {
                Binder.flushPendingCommands();
            }

            //将 Loop 线程休眠挂起
            nativePollOnce(ptr, nextPollTimeoutMillis);

            synchronized (this) {
                // Try to retrieve the next message.  Return if found.
                final long now = SystemClock.uptimeMillis();
                Message prevMsg = null;
                Message msg = mMessages;
                if (msg != null && msg.target == null) {
                    // Stalled by a barrier.  Find the next asynchronous message in the queue.
                    do {
                        prevMsg = msg;
                        msg = msg.next;
                    } while (msg != null && !msg.isAsynchronous());
                }
                if (msg != null) {
                    if (now < msg.when) {
                        //队头消息还未到处理时间，计算需要等待的时间
                        // Next message is not ready.  Set a timeout to wake up when it is ready.
                        nextPollTimeoutMillis = (int) Math.min(msg.when - now, Integer.MAX_VALUE);
                    } else {
                        // Got a message.
                        mBlocked = false;
                        if (prevMsg != null) {
                            prevMsg.next = msg.next;
                        } else {
                            mMessages = msg.next;
                        }
                        msg.next = null;
                        if (DEBUG) Log.v(TAG, "Returning message: " + msg);
                        msg.markInUse();
                        return msg;
                    }
                } else {
                    // No more messages.
                    nextPollTimeoutMillis = -1;
                }
                ···
            }
            ···
            }
			···
        }
    }
```

`next()` 方法又是通过 Looper 类的 `loop()` 方法来循环调用的，`loop()` 方法内也是一个无限循环，唯一跳出循环的条件就是 `queue.next()`方法返回为 null。因为 `next()` 方法可能会触发阻塞操作，所以没有消息需要处理时也会导致 `loop()` 方法被阻塞着，而当 MessageQueue 有了新的消息，Looper 就会及时地处理这条消息并调用 `msg.target.dispatchMessage(msg)` 方法将消息回传给 Handler 进行处理 

```java
	/**
     * Run the message queue in this thread. Be sure to call
     * {@link #quit()} to end the loop.
     */
    public static void loop() {
        final Looper me = myLooper();
        if (me == null) {
            throw new RuntimeException("No Looper; Looper.prepare() wasn't called on this thread.");
        }
        ···	
        for (;;) {
            Message msg = queue.next(); // might block
            if (msg == null) {
                // No message indicates that the message queue is quitting.
                return;
            }
            ···
            msg.target.dispatchMessage(msg);
            ···
        }
    }
```

Handler 的`dispatchMessage`方法就是在向外部分发 Message 了。至此，Message 的整个分发流程就结束了

```java
    /**
     * Handle system messages here.
     */
    public void dispatchMessage(@NonNull Message msg) {
        if (msg.callback != null) {
            handleCallback(msg);
        } else {
            if (mCallback != null) {
                if (mCallback.handleMessage(msg)) {
                    return;
                }
            }
            handleMessage(msg);
        }
    }
```

#### 5、消息屏障

Android 系统为了保证某些高优先级的 Message（异步消息） 能够被尽快执行，采用了一种消息屏障（Barrier）机制。其大致流程是：先发送一个屏障消息到 MessageQueue 中，当 MessageQueue 遍历到该屏障消息时，就会判断当前队列中是否存在异步消息，有的话则先跳过同步消息（开发者主动发送的都属于同步消息），优先执行异步消息。这种机制就会使得在异步消息被执行完之前，同步消息都不会得到处理

Handler 的构造函数中的`async`参数就用于控制发送的 Message 是否属于异步消息

```java
    public class Handler {

		final boolean mAsynchronous;

		public Handler(@NonNull Looper looper, @Nullable Callback callback, boolean async) {
        	···
        	mAsynchronous = async;
    	}

    	private boolean enqueueMessage(@NonNull MessageQueue queue, @NonNull Message msg,
            	long uptimeMillis) {
        	msg.target = this;
        	msg.workSourceUid = ThreadLocalWorkSource.getUid();
        	if (mAsynchronous) {
                //设为异步消息
            	msg.setAsynchronous(true);
        	}
        	return queue.enqueueMessage(msg, uptimeMillis);
    	}
       
    }
```

MessageQueue 在取队头消息的时候，如果判断到队头消息就是屏障消息的话，那么就会向后遍历找到第一条异步消息优先进行处理

```java
	@UnsupportedAppUsage
    Message next() {
        ···
        for (;;) {
            if (nextPollTimeoutMillis != 0) {
                Binder.flushPendingCommands();
            }
            nativePollOnce(ptr, nextPollTimeoutMillis);
            synchronized (this) {
                // Try to retrieve the next message.  Return if found.
                final long now = SystemClock.uptimeMillis();
                Message prevMsg = null;
                Message msg = mMessages;
                if (msg != null && msg.target == null) { //target 为 null 即属于屏障消息
                    // Stalled by a barrier.  Find the next asynchronous message in the queue.
                    //循环遍历，找到屏障消息后面的第一条异步消息进行处理
                    do {
                        prevMsg = msg;
                        msg = msg.next;
                    } while (msg != null && !msg.isAsynchronous());
                }
                ···
            }
            ···
        }
    }
```

#### 6、退出 Looper 循环

Looper 类本身做了方法限制，除了主线程外，子线程关联的 MessageQueue 都支持退出 Loop 循环，即 quitAllowed 只有主线程才能是 false

```java
public final class Looper {
    
    private Looper(boolean quitAllowed) {
        mQueue = new MessageQueue(quitAllowed);
        mThread = Thread.currentThread();
    }
    
    public static void prepare() {
        prepare(true);
    }

    private static void prepare(boolean quitAllowed) {
        if (sThreadLocal.get() != null) {
            throw new RuntimeException("Only one Looper may be created per thread");
        }
        sThreadLocal.set(new Looper(quitAllowed));
    }
    
}
```

MessageQueue 支持两种方式来退出 Loop：

- safe 为 true，只移除所有尚未执行的消息，不移除时间戳等于当前时间的消息
- safe 为 false，移除所有消息

```java
	void quit(boolean safe) {
        if (!mQuitAllowed) {
            //MessageQueue 设置了不允许退出循环，直接抛出异常
            throw new IllegalStateException("Main thread not allowed to quit.");
        }
        synchronized (this) {
            if (mQuitting) {
                //避免重复调用
                return;
            }
            mQuitting = true;
            if (safe) {
                //只移除所有尚未执行的消息，不移除时间戳等于当前时间的消息
                removeAllFutureMessagesLocked();
            } else {
                //移除所有消息
                removeAllMessagesLocked();
            }
            // We can assume mPtr != 0 because mQuitting was previously false.
            nativeWake(mPtr);
        }
    }
```

#### 7、IdleHandler

IdleHandler 是 MessageQueue 的一个内部接口，可以用于在 Loop 线程处于空闲状态的时候执行一些优先级不高的操作

```java
    public static interface IdleHandler {
        boolean queueIdle();
    }
```

MessageQueue 在获取队头消息时，如果发现当前没有需要执行的 Message 的话，那么就会去遍历 mIdleHandlers，依次执行 IdleHandler

```java
	private final ArrayList<IdleHandler> mIdleHandlers = new ArrayList<IdleHandler>();

	@UnsupportedAppUsage
    Message next() {
        ···
        int pendingIdleHandlerCount = -1; // -1 only during first iteration
        int nextPollTimeoutMillis = 0;
        for (;;) {
            ···
            synchronized (this) {
                ···
                //如果队头消息 mMessages 为 null 或者 mMessages 需要延迟处理
                //那么就来执行 IdleHandler
                if (pendingIdleHandlerCount < 0
                        && (mMessages == null || now < mMessages.when)) {
                    pendingIdleHandlerCount = mIdleHandlers.size();
                }
                if (pendingIdleHandlerCount <= 0) {
                    // No idle handlers to run.  Loop and wait some more.
                    mBlocked = true;
                    continue;
                }
                if (mPendingIdleHandlers == null) {
                    mPendingIdleHandlers = new IdleHandler[Math.max(pendingIdleHandlerCount, 4)];
                }
                mPendingIdleHandlers = mIdleHandlers.toArray(mPendingIdleHandlers);
            }
            for (int i = 0; i < pendingIdleHandlerCount; i++) {
                final IdleHandler idler = mPendingIdleHandlers[i];
                mPendingIdleHandlers[i] = null; // release the reference to the handler
                boolean keep = false;
                try {
                    //执行 IdleHandler
                    //如果返回 false 的话说明之后不再需要执行，那就将其移除
                    keep = idler.queueIdle();
                } catch (Throwable t) {
                    Log.wtf(TAG, "IdleHandler threw exception", t);
                }
                if (!keep) {
                    synchronized (this) {
                        mIdleHandlers.remove(idler);
                    }
                }
            }
			···
        }
    }
```

例如，ActivityThread 就向主线程 MessageQueue 添加了一个 GcIdler，用于在主线程空闲时尝试去执行 GC 操作

```java
public final class ActivityThread extends ClientTransactionHandler {
    
    @UnsupportedAppUsage
    void scheduleGcIdler() {
        if (!mGcIdlerScheduled) {
            mGcIdlerScheduled = true;
            //添加 IdleHandler
            Looper.myQueue().addIdleHandler(mGcIdler);
        }
        mH.removeMessages(H.GC_WHEN_IDLE);
    }
    
    final class GcIdler implements MessageQueue.IdleHandler {
        @Override
        public final boolean queueIdle() {
            //尝试 GC
            doGcIfNeeded();
            purgePendingResources();
            return false;
        }
    }
    
}
```

#### 8、做一个总结

再来总结下以上的所有内容

1. 每个 Handler 都会和一个 Looper 实例关联在一起，可以在初始化 Handler 时通过构造函数主动传入实例，否则就会默认使用和当前线程关联的 Looper 对象
2. 每个 Looper 都会和一个 MessageQueue 实例关联在一起，每个线程都需要通过调用 `Looper.prepare()`方法来初始化本线程独有的 Looper 实例，并通过调用`Looper.loop()`方法来使得本线程循环向 MessageQueue 取出消息并执行。Android 系统默认会为每个应用初始化和主线程关联的 Looper 对象，并且默认就开启了 loop 循环来处理主线程消息
3. MessageQueue 按照链接结构来保存 Message，执行时间早（即时间戳小）的 Message 会排在链表的头部，Looper 会循环从链表中取出 Message 并回调给  Handler，取值的过程可能会包含阻塞操作
4. Message、Handler、Looper、MessageQueue 这四者就构成了一个生产者和消费者模式。Message 相当于产品，MessageQueue 相当于传输管道，Handler 相当于生产者，Looper 相当于消费者
5. Handler 对于 Looper、Handler 对于 MessageQueue、Looper 对于 MessageQueue、Looper 对于 Thread ，这几个之间都是一一对应的关系，在关联后无法更改，但 Looper 对于 Handler、MessageQueue 对于 Handler 可以是一对多的关系
6. Handler 能用于更新 UI 包含了一个隐性的前提条件：Handler 与主线程 Looper 关联在了一起。在主线程中初始化的 Handler 会默认与主线程 Looper 关联在一起，所以其 `handleMessage(Message msg)` 方法就会由主线程来调用。在子线程初始化的 Handler 如果也想执行 UI 更新操作的话，则需要主动获取 mainLooper 来初始化 Handler
7. 对于我们自己在子线程中创建的 Looper，当不再需要的时候我们应该主动退出循环，否则子线程将一直无法得到释放。对于主线程 Loop 我们则不应该去主动退出，否则将导致应用崩溃
8. 我们可以通过向 MessageQueue 添加 IdleHandler 的方式，来实现在 Loop 线程处于空闲状态的时候执行一些优先级不高的任务。例如，假设我们有个需求是希望当主线程完成界面绘制等事件后再执行一些 UI 操作，那么就可以通过 IdleHandler 来实现，这可以避免拖慢用户看到首屏页面的速度

### 三、Handler 在系统中的应用

#### 1、HandlerThread

HandlerThread 是 Android SDK 中和 Handler 在同个包下的一个类，从其名字就可以看出来它是一个线程，而且使用到了 Handler

其用法类似于以下代码。通过 HandlerThread 内部的 Looper 对象来初始化 Handler，同时在 Handler 中声明需要执行的耗时任务，主线程通过向 Handler 发送消息来触发 HandlerThread 去执行耗时任务

```kotlin
class MainActivity : AppCompatActivity() {

    private val handlerThread = HandlerThread("I am HandlerThread")

    private val handler by lazy {
        object : Handler(handlerThread.looper) {
            override fun handleMessage(msg: Message) {
                Thread.sleep(2000)
                Log.e("MainActivity", "这里是子线程，可以用来执行耗时任务：" + Thread.currentThread().name)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn_test.setOnClickListener {
            handler.sendEmptyMessage(1)
        }
        handlerThread.start()
    }

}
```

HandlerThread 的源码还是挺简单的，只有一百多行

HandlerThread 是 Thread 的子类，其作用就是为了用来执行耗时任务，其 `run()`方法会自动为自己创建一个 Looper 对象并保存到 `mLooper`，之后就主动开启消息循环，这样 HandlerThread 就会来循环处理 Message 了

```java
public class HandlerThread extends Thread {
    
    //线程优先级
    int mPriority;
    //线程ID
    int mTid = -1;
    //当前线程持有的 Looper 对象
    Looper mLooper;
    
    private @Nullable Handler mHandler;

    public HandlerThread(String name) {
        super(name);
        mPriority = Process.THREAD_PRIORITY_DEFAULT;
    }

    public HandlerThread(String name, int priority) {
        super(name);
        mPriority = priority;
    }
    
    @Override
    public void run() {
        mTid = Process.myTid();
        //触发当前线程创建 Looper 对象
        Looper.prepare();
        synchronized (this) {
            //获取 Looper 对象
            mLooper = Looper.myLooper();
            //唤醒所有处于等待状态的线程
            notifyAll();
        }
        //设置线程优先级
        Process.setThreadPriority(mPriority);
        onLooperPrepared();
        //开启消息循环
        Looper.loop();
        mTid = -1;
    }
    
}
```

此外，HandlerThread 还包含一个`getLooper()`方法用于获取 Looper。当我们在外部调用`handlerThread.start()`启动线程后，由于其`run()`方法的执行时机依然是不确定的，所以 `getLooper()`方法就必须等到 Looper 初始化完毕后才能返回，否则就会由于`wait()`方法而一直阻塞等待。当`run()`方法初始化 Looper 完成后，就会调用`notifyAll()`来唤醒所有处于等待状态的线程。所以外部在使用 HandlerThread 前就记得必须先调用 `start()` 方法来启动 HandlerThread 

```java
    //获取与 HandlerThread 关联的 Looper 对象
    //因为 getLooper() 可能先于 run() 被执行
	//所以当 mLooper 为 null 时调用者线程就需要阻塞等待 Looper 对象创建完毕
    public Looper getLooper() {
        if (!isAlive()) {
            return null;
        }
        
        // If the thread has been started, wait until the looper has been created.
        synchronized (this) {
            while (isAlive() && mLooper == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
        }
        return mLooper;
    }
```

HandlerThread 起到的作用就是方便了主线程和子线程之间的交互，主线程可以直接通过 Handler 来声明耗时任务并交由子线程来执行。使用 HandlerThread 也方便在多个线程间共享，主线程和其它子线程都可以向 HandlerThread 下发任务，且 HandlerThread 可以保证多个任务执行时的有序性

#### 2、IntentService 

IntentService 是系统提供的 Service 子类，用于在后台串行执行耗时任务，在处理完所有任务后会自动停止，不必来手动调用 `stopSelf()` 方法。而且由于IntentService 是四大组件之一，拥有较高的优先级，不易被系统杀死，因此适合用于执行一些高优先级的异步任务

**Google 官方以前也推荐开发者使用 IntentService，但是在 Android 11 中已经被标记为废弃状态了，但这也不妨碍我们来了解下其实现原理**

IntentService 内部依靠 HandlerThread 来实现，其 `onCreate()`方法会创建一个 HandlerThread，拿到 Looper 对象来初始化 ServiceHandler。ServiceHandler 会将其接受到的每个 Message 都转交由抽象方法 `onHandleIntent`来处理，子类就通过实现该方法来声明耗时任务

```java
public abstract class IntentService extends Service {
    
    private volatile Looper mServiceLooper;
    @UnsupportedAppUsage
    private volatile ServiceHandler mServiceHandler;
    
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            onHandleIntent((Intent)msg.obj);
            stopSelf(msg.arg1);
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread("IntentService[" + mName + "]");
        //触发 HandlerThread 创建 Looper 对象
        thread.start();
		//获取 Looper 对象，构建可以向 HandlerThread 发送 Message 的 Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }
    
    @WorkerThread
    protected abstract void onHandleIntent(@Nullable Intent intent);
    
}
```

每次 start IntentService 时，`onStart()`方法就会被调用，将 `intent` 和 `startId` 包装为一个 Message 对象后发送给`mServiceHandler`。需要特别注意的是 **startId** 这个参数，它用于唯一标识每次对 IntentService 发起的任务请求，每次回调 `onStart()` 方法时，startId 的值都是自动递增的。IntentService 不应该在处理完一个 Message 之后就立即停止 IntentService，因为此时 MessageQueue 中可能还有待处理的任务还未取出来，所以如果当调用 `stopSelf(int)`方法时传入的参数不等于当前最新的 startId 值的话，那么`stopSelf(int)` 方法就不会导致 IntentService 被停止，从而避免了将尚未处理的 Message 给遗漏了

```java
    @Override
    public void onStart(@Nullable Intent intent, int startId) {
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        mServiceHandler.sendMessage(msg);
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        onStart(intent, startId);
        return mRedelivery ? START_REDELIVER_INTENT : START_NOT_STICKY;
    }
```

### 四、Handler 在三方库中的应用

#### 1、EventBus

EventBus 的 Github 上有这么一句介绍：[EventBus](https://greenrobot.org/eventbus/) is a publish/subscribe event bus for Android and Java.  这说明了 EventBus 是普遍适用于 Java 环境的，只是对 Android 系统做了特殊的平台支持而已。EventBus 的四种消息发送策略包含了`ThreadMode.MAIN` 用于指定在主线程进行消息回调，其内部就是通过 Handler 来实现的

EventBusBuilder 会去尝试获取 MainLooper，如果拿得到的话就可以用来初始化 HandlerPoster，从而实现主线程回调

```java
	MainThreadSupport getMainThreadSupport() {
        if (mainThreadSupport != null) {
            return mainThreadSupport;
        } else if (AndroidLogger.isAndroidLogAvailable()) {
            Object looperOrNull = getAndroidMainLooperOrNull();
            return looperOrNull == null ? null :
                    new MainThreadSupport.AndroidHandlerMainThreadSupport((Looper) looperOrNull);
        } else {
            return null;
        }
    }

    static Object getAndroidMainLooperOrNull() {
        try {
            return Looper.getMainLooper();
        } catch (RuntimeException e) {
            // Not really a functional Android (e.g. "Stub!" maven dependencies)
            return null;
        }
    }
```

```java
public class HandlerPoster extends Handler implements Poster {

    protected HandlerPoster(EventBus eventBus, Looper looper, int maxMillisInsideHandleMessage) {
        super(looper);
        ···
    }
    
	···
        
    @Override
    public void handleMessage(Message msg) {
        ···
    }
}
```

#### 2、Retrofit

和 EventBus 一样，Retrofit 的内部实现也不需要依赖于 Android 平台，而是可以用于任意的 Java 客户端，Retrofit 只是对 Android 平台进行了特殊实现而已

在构建 Retrofit 对象的时候，我们可以选择传递一个 Platform 对象用于标记调用方所处的平台

```java
public static final class Builder {
    
    private final Platform platform;
    
    Builder(Platform platform) {
      this.platform = platform;
    }

	···
}
```

Platform 类只具有一个唯一子类，即 Android 类。其主要逻辑就是重写了父类的 `defaultCallbackExecutor()`方法，通过 Handler 来实现在主线程回调网络请求结果

```java
static final class Android extends Platform {
    
    @Override
    public Executor defaultCallbackExecutor() {
      return new MainThreadExecutor();
    }

    ···

    static final class MainThreadExecutor implements Executor {
      private final Handler handler = new Handler(Looper.getMainLooper());

      @Override
      public void execute(Runnable r) {
        handler.post(r);
      }
    }
  }
```

### 五、面试环节

#### 1、Handler、Looper、MessageQueue、Thread 的对应关系

首先，Looper 中的 MessageQueue 和 Thread 两个字段都属于常量，且 Looper 实例是存在 ThreadLocal 中，这说明了 Looper 和 MessageQueue 之间是一对一应的关系，且一个 Thread 在其整个生命周期内都只会关联到同一个 Looper 对象和同一个 MessageQueue 对象

```java
public final class Looper {
 
   final MessageQueue mQueue;
   final Thread mThread;
   static final ThreadLocal<Looper> sThreadLocal = new ThreadLocal<Looper>();
    
   private Looper(boolean quitAllowed) {
        mQueue = new MessageQueue(quitAllowed);
        mThread = Thread.currentThread();
    }
    
}
```

Handler 中的 Looper 和 MessageQueue 两个字段也都属于常量，说明 Handler 对于 Looper 和 MessageQueue 都是一对一的关系。**但是 Looper 和 MessageQueue 对于 Handler 却可以是一对多的关系，例如，多个子线程内声明的 Handler 都可以关联到 mainLooper**

```java
public class Handler {
    
    @UnsupportedAppUsage
    final Looper mLooper;
    final MessageQueue mQueue;
    
}
```

#### 2、Handler 的同步机制

MessageQueue 在保存 Message 的时候，`enqueueMessage`方法内部已经加上了同步锁，从而避免了多个线程同时发送消息导致竞态问题。此外，`next()`方法内部也加上了同步锁，所以也保障了 Looper 分发 Message 的有序性。最重要的一点是，Looper 总是由一个特定的线程来执行遍历，所以在消费 Message 的时候也不存在竞态

```java
    boolean enqueueMessage(Message msg, long when) {
        if (msg.target == null) {
            throw new IllegalArgumentException("Message must have a target.");
        }

        synchronized (this) {
            ···
        }
        return true;
    }


    @UnsupportedAppUsage
    Message next() {
        ···
        for (;;) {
            if (nextPollTimeoutMillis != 0) {
                Binder.flushPendingCommands();
            }

            nativePollOnce(ptr, nextPollTimeoutMillis);

            synchronized (this) {
                ···
            }

            ···
        }
    }
```

#### 3、Handler 如何发送同步消息

如果我们在子线程通过 Handler 向主线程发送了一个消息，希望等到消息执行完毕后子线程才继续运行，这该如何实现？其实像这种涉及到多线程同步等待的问题，往往都是需要依赖于**线程休眠+线程唤醒**机制来实现的

Handler 本身就提供了一个`runWithScissors`方法可以用于实现这种功能，只是被隐藏了，我们无法直接调用到。`runWithScissors`首先会判断目标线程是否就是当前线程，是的话则直接执行 Runnable，否则就需要使用到 BlockingRunnable

```java
    /**
     * @hide
     */
    public final boolean runWithScissors(@NonNull Runnable r, long timeout) {
        if (r == null) {
            throw new IllegalArgumentException("runnable must not be null");
        }
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout must be non-negative");
        }

        if (Looper.myLooper() == mLooper) {
            r.run();
            return true;
        }

        BlockingRunnable br = new BlockingRunnable(r);
        return br.postAndWait(this, timeout);
    }
```

BlockingRunnable 的逻辑也很简单，在 Runnable 执行完前会通过调用 `wait()`方法来使发送者线程转为阻塞等待状态，当任务执行完毕后再通过`notifyAll()`来唤醒发送者线程，从而实现了在 Runnable 被执行完之前发送者线程都会一直处于等待状态

```java
private static final class BlockingRunnable implements Runnable {
    
        private final Runnable mTask;
    	//用于标记 mTask 是否已经执行完毕 
        private boolean mDone;

        public BlockingRunnable(Runnable task) {
            mTask = task;
        }

        @Override
        public void run() {
            try {
                mTask.run();
            } finally {
                synchronized (this) {
                    mDone = true;
                    notifyAll();
                }
            }
        }

        public boolean postAndWait(Handler handler, long timeout) {
            if (!handler.post(this)) {
                return false;
            }

            synchronized (this) {
                if (timeout > 0) {
                    final long expirationTime = SystemClock.uptimeMillis() + timeout;
                    while (!mDone) {
                        long delay = expirationTime - SystemClock.uptimeMillis();
                        if (delay <= 0) {
                            return false; // timeout
                        }
                        try {
                            //限时等待
                            wait(delay);
                        } catch (InterruptedException ex) {
                        }
                    }
                } else {
                    while (!mDone) {
                        try {
                            //无限期等待
                            wait();
                        } catch (InterruptedException ex) {
                        }
                    }
                }
            }
            return true;
        }
    }
```

虽然 `runWithScissors` 方法我们无法直接调用，但是我们也可以依靠这思路自己来实现 BlockingRunnable，折中实现这个功能。但这种方式并不安全，如果 Loop 意外退出循环导致该 Runnable 无法被执行的话，就会导致被暂停的线程一直无法被唤醒，需要谨慎使用

#### 4、Handler 如何避免内存泄漏

当退出 Activity 时，如果 Handler 中还保存着待处理的延时消息的话，那么就会导致内存泄漏，此时可以通过调用`Handler.removeCallbacksAndMessages(null)`来移除所有待处理的 Message

该方法会将消息队列中所有 `Message.obj` 等于 token 的 Message 均给移除掉，如果 token 为 null 的话则会移除所有 Message 

```java
    public final void removeCallbacksAndMessages(@Nullable Object token) {
        mQueue.removeCallbacksAndMessages(this, token);
    }
```

#### 5、Message 如何复用

因为 Android 系统本身就存在很多事件需要交由 Message 来交付给 mainLooper，所以 Message 的创建是很频繁的。为了减少 Message 频繁重复创建的情况，Message 提供了 MessagePool 用于实现 Message 的缓存复用，以此来优化内存使用

当 Looper 消费了 Message 后会调用`recycleUnchecked()`方法将 Message 进行回收，在清除了各项资源后会缓存到 sPool 变量上，同时将之前缓存的 Message 置为下一个节点 next，通过这种链表结构来缓存最多 50 个Message。这里使用到的是**享元设计模式**

`obtain()`方法则会判断当前是否有可用的缓存，有的话则将 sPool 从链表中移除后返回，否则就返回一个新的 Message 实例。所以我们在发送消息的时候应该尽量通过调用`Message.obtain()`或者`Handler.obtainMessage()`方法来获取 Message 实例

```java
public final class Message implements Parcelable {
    
    /** @hide */
    public static final Object sPoolSync = new Object();
    private static Message sPool;
    private static int sPoolSize = 0;
    private static final int MAX_POOL_SIZE = 50;
    
    public static Message obtain() {
        synchronized (sPoolSync) {
            if (sPool != null) {
                Message m = sPool;
                sPool = m.next;
                m.next = null;
                m.flags = 0; // clear in-use flag
                sPoolSize--;
                return m;
            }
        }
        return new Message();
    }
    
    @UnsupportedAppUsage
    void recycleUnchecked() {
        // Mark the message as in use while it remains in the recycled object pool.
        // Clear out all other details.
        flags = FLAG_IN_USE;
        what = 0;
        arg1 = 0;
        arg2 = 0;
        obj = null;
        replyTo = null;
        sendingUid = UID_NONE;
        workSourceUid = UID_NONE;
        when = 0;
        target = null;
        callback = null;
        data = null;
        synchronized (sPoolSync) {
            if (sPoolSize < MAX_POOL_SIZE) {
                next = sPool;
                sPool = this;
                sPoolSize++;
            }
        }
    }
    
}
```

#### 6、Message 复用机制存在的问题

由于 Message 采用了缓存复用机制，从而导致了一个 Message 失效问题。当 `handleMessage` 方法被回调后，Message 携带的所有参数都会被清空，而如果外部的 `handleMessage`方法是使用了异步线程来处理 Message 的话，那么异步线程只会得到一个空白的 Message

```kotlin
val handler = object : Handler() {
    override fun handleMessage(msg: Message) {
        handleMessageAsync(msg)
    }
}

fun handleMessageAsync(msg: Message) {
    thread {
        //只会得到一个空白的 Message 对象
        println(msg.obj)
    }
}
```

#### 7、Message 如何提高优先级

Handler 包含一个 `sendMessageAtFrontOfQueue`方法可以用于提高 Message 的处理优先级。该方法为 Message 设定的时间戳是 0，使得 Message 可以直接插入到 MessageQueue 的头部，从而做到优先处理。但官方并不推荐使用这个方法，因为最极端的情况下可能会使得其它 Message 一直得不到处理或者其它意想不到的情况

```java
    public final boolean sendMessageAtFrontOfQueue(@NonNull Message msg) {
        MessageQueue queue = mQueue;
        if (queue == null) {
            RuntimeException e = new RuntimeException(
                this + " sendMessageAtTime() called with no mQueue");
            Log.w("Looper", e.getMessage(), e);
            return false;
        }
        return enqueueMessage(queue, msg, 0);
    }
```

#### 8、检测 Looper 分发 Message 的效率

Looper 在进行 Loop 循环时，会通过 Observer 向外回调每个 Message 的回调事件。且如果设定了 `slowDispatchThresholdMs` 和 `slowDeliveryThresholdMs` 这两个阈值的话，则会对 Message 的**分发时机**和**分发耗时**进行监测，存在异常情况的话就会打印 Log。该机制可以用于实现应用性能监测，发现潜在的 Message 处理异常情况，但可惜监测方法被系统隐藏了

```java
	public static void loop() {
        final Looper me = myLooper();
        ···
        for (;;) {
            Message msg = queue.next(); // might block
			···
            //用于向外回调通知 Message 的分发事件
            final Observer observer = sObserver;

            final long traceTag = me.mTraceTag;
            //如果Looper分发Message的时间晚于预定时间且超出这个阈值，则认为Looper分发过慢
            long slowDispatchThresholdMs = me.mSlowDispatchThresholdMs;
            //如果向外分发出去的Message的处理时间超出这个阈值，则认为外部处理过慢
            long slowDeliveryThresholdMs = me.mSlowDeliveryThresholdMs;
            if (thresholdOverride > 0) {
                slowDispatchThresholdMs = thresholdOverride;
                slowDeliveryThresholdMs = thresholdOverride;
            }
            final boolean logSlowDelivery = (slowDeliveryThresholdMs > 0) && (msg.when > 0);
            final boolean logSlowDispatch = (slowDispatchThresholdMs > 0);

            final boolean needStartTime = logSlowDelivery || logSlowDispatch;
            final boolean needEndTime = logSlowDispatch;

            if (traceTag != 0 && Trace.isTagEnabled(traceTag)) {
                Trace.traceBegin(traceTag, msg.target.getTraceName(msg));
            }

            //开始分发 Message 的时间
            final long dispatchStart = needStartTime ? SystemClock.uptimeMillis() : 0;
            //Message 分发结束的时间
            final long dispatchEnd;
            Object token = null;
            if (observer != null) {
                //开始分发 Message 
                token = observer.messageDispatchStarting();
            }
            long origWorkSource = ThreadLocalWorkSource.setUid(msg.workSourceUid);
            try {
                msg.target.dispatchMessage(msg);
                if (observer != null) {
                    //完成 Message 的分发，且没有抛出异常
                    observer.messageDispatched(token, msg);
                }
                dispatchEnd = needEndTime ? SystemClock.uptimeMillis() : 0;
            } catch (Exception exception) {
                if (observer != null) {
                    //分发 Message 时抛出了异常
                    observer.dispatchingThrewException(token, msg, exception);
                }
                throw exception;
            } finally {
                ThreadLocalWorkSource.restore(origWorkSource);
                if (traceTag != 0) {
                    Trace.traceEnd(traceTag);
                }
            }
            if (logSlowDelivery) {
                if (slowDeliveryDetected) {
                    if ((dispatchStart - msg.when) <= 10) {
                        //如果 Message 的分发时间晚于预定时间，且间隔超出10毫秒，则认为属于延迟交付
                        Slog.w(TAG, "Drained");
                        slowDeliveryDetected = false;
                    }
                } else {
                    if (showSlowLog(slowDeliveryThresholdMs, msg.when, dispatchStart, "delivery",
                            msg)) {
                        // Once we write a slow delivery log, suppress until the queue drains.
                        slowDeliveryDetected = true;
                    }
                }
            }
            ···
        }
    }
```

#### 9、主线程 Looper 在哪里创建

由 ActivityThread 类的 `main()` 方法来创建。该 `main()` 方法即 Java 程序的运行起始点，当应用启动时系统就自动为我们在主线程做好了 mainLooper 的初始化，而且已经调用了`Looper.loop()`方法开启了消息的循环处理，应用在使用过程中的各种交互逻辑（例如：屏幕的触摸事件、列表的滑动等）就都是在这个循环里完成分发的。正是因为 Android 系统已经自动完成了主线程 Looper 的初始化，所以我们在主线程中才可以直接使用 Handler 的无参构造函数来完成 UI 相关事件的处理

```java
public final class ActivityThread extends ClientTransactionHandler {
 
    public static void main(String[] args) {
        ···
        Looper.prepareMainLooper();
        ···
        Looper.loop();
        throw new RuntimeException("Main thread loop unexpectedly exited");
    }
    
}
```

#### 10、主线程 Looper 什么时候退出循环

当 ActivityThread 内部的 Handler 收到了 EXIT_APPLICATION 消息后，就会退出 Looper 循环

```java
		public void handleMessage(Message msg) {
            switch (msg.what) {
                case EXIT_APPLICATION:
                    if (mInitialApplication != null) {
                        mInitialApplication.onTerminate();
                    }
                    Looper.myLooper().quit();
                    break;
            }
		}
```

#### 11、主线程 Looper.loop() 为什么不会导致 ANR

这个问题在网上很常见，我第一次看到时就觉得这种问题很奇怪，主线程凭啥会 ANR？这个问题感觉本身就是特意为了来误导人

看以下例子。`doSomeThing()`方法是放在 for 循环这个死循环的后边，对于该方法来说，主线程的确是被阻塞住了，导致该方法一直无法得到执行。可是对于应用来说，应用在主线程内的所有操作其实都是被放在了 for 循环之内，一直有得到执行，是个死循环也无所谓，所以对于应用来说主线程并没有被阻塞，自然不会导致 ANR。此外，当 MessageQueue 中当前没有消息需要处理时，也会依靠 epoll 机制挂起主线程，避免了其一直占用 CPU 资源

```java
    public static void main(String[] args) {
        for (; ; ) {
            //主线程执行....
        }
        doSomeThing();
    }
```

所以在 ActivityThread 的 main 方法中，在开启了消息循环之后，并没有声明什么有意义的代码。正常来说应用是不会退出 loop 循环的，如果能够跳出循环，也只会导致直接就抛出异常

```java
	public static void main(String[] args) {
        ···
        Looper.prepareMainLooper();
        ···
        Looper.loop();
        throw new RuntimeException("Main thread loop unexpectedly exited");
    }
```

所以说，loop 循环本身不会导致 ANR，会出现 ANR 是因为在 loop 循环之内 Message 处理时间过长

#### 12、子线程一定无法弹 Toast 吗

不一定，只能说是**在子线程中无法直接弹出 Toast，但可以实现**。因为 Toast 的构造函数中会要求拿到一个 Looper 对象，如果构造参数没有传入不为 null 的 Looper 实例的话，则尝试使用调用者线程关联的 Looper 对象，如果都获取不到的话则会抛出异常

```java
    public Toast(Context context) {
        this(context, null);
    }

	public Toast(@NonNull Context context, @Nullable Looper looper) {
        mContext = context;
        mToken = new Binder();
        looper = getLooper(looper);
        mHandler = new Handler(looper);
        ···
    }

    private Looper getLooper(@Nullable Looper looper) {
        if (looper != null) {
            return looper;
        }
        //Looper.myLooper() 为 null 的话就会直接抛出异常
        return checkNotNull(Looper.myLooper(),
                "Can't toast on a thread that has not called Looper.prepare()");
    }
```

为了在子线程弹 Toast，就需要主动为子线程创建 Looper 对象及开启 loop 循环。但这种方法会导致子线程一直无法退出循环，需要通过`Looper.myLooper().quit()`来主动退出循环

```kotlin
    inner class TestThread : Thread() {

        override fun run() {
            Looper.prepare()
            Toast.makeText(
                this@MainActivity,
                "Hello: " + Thread.currentThread().name,
                Toast.LENGTH_SHORT
            ).show()
            Looper.loop()
        }

    }
```

#### 13、子线程一定无法更新 UI？主线程就一定可以？

在子线程能够弹出 Toast 就已经说明了子线程也是可以更新 UI 的，**Android 系统只是限制了必须在同个线程内进行 ViewRootImpl 的创建和更新这两个操作**，而不是要求必须在主线程进行

如果使用不当的话，即使在主线程更新 UI 也可能会导致应用崩溃。例如，在子线程先通过 show+hide 来触发 ViewRootImpl 的创建，然后在主线程再来尝试显示该 Dialog，此时就会发现程序直接崩溃了

```kotlin
class MainActivity : AppCompatActivity() {

    private lateinit var alertDialog: AlertDialog

    private val thread = object : Thread("hello") {
        override fun run() {
            Looper.prepare()
            Handler().post {
                alertDialog =
                    AlertDialog.Builder(this@MainActivity).setMessage(Thread.currentThread().name)
                        .create()
                alertDialog.show()
                alertDialog.hide()
            }
            Looper.loop()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn_test.setOnClickListener {
            alertDialog.show()
        }
        thread.start()
    }

}
```

```java
    E/AndroidRuntime: FATAL EXCEPTION: main
    Process: github.leavesc.test, PID: 5243
    android.view.ViewRootImpl$CalledFromWrongThreadException: Only the original thread that created a view hierarchy can touch its views.
        at android.view.ViewRootImpl.checkThread(ViewRootImpl.java:6892)
        at android.view.ViewRootImpl.requestLayout(ViewRootImpl.java:1048)
        at android.view.View.requestLayout(View.java:19781)
        at android.view.View.setFlags(View.java:11478)
        at android.view.View.setVisibility(View.java:8069)
        at android.app.Dialog.show(Dialog.java:293)
```

ViewRootImpl 在初始化的时候会将当前线程保存到 mThread，在后续进行 UI 更新的时候就会调用`checkThread()`方法进行线程检查，如果发现存在多线程调用则直接抛出以上的异常信息

```java
public final class ViewRootImpl implements ViewParent,
        View.AttachInfo.Callbacks, ThreadedRenderer.DrawCallbacks {
            
     final Thread mThread;       
            
     public ViewRootImpl(Context context, Display display, IWindowSession session,
            boolean useSfChoreographer) {
        mThread = Thread.currentThread();
        ···
    }       
            
    void checkThread() {
        if (mThread != Thread.currentThread()) {
            throw new CalledFromWrongThreadException(
                    "Only the original thread that created a view hierarchy can touch its views.");
        }
    }
            
}
```

#### 14、为什么 UI 体系要采用单线程模型

其实这很好理解，就是为了提高运行效率和降低实现难度。如果允许多线程并发访问 UI 的话，为了避免竞态，很多即使只是小范围的局部刷新操作（例如，TextView.setText）都势必需要加上同步锁，这无疑会加大 UI 刷新操作的“成本”，降低了整个应用的运行效率。而且会导致 Android 的 UI 体系在实现时就被迫需要对多线程环境进行“防御”，即使开发者一直是使用同个线程来更新 UI，这就加大了系统的实现难度

所以，最为简单高效的方式就是采用单线程模型来访问 UI

#### 15、如何跨线程下发任务

通常情况下，两个线程之间的通信是比较麻烦的，需要做很多线程同步操作。而依靠 Looper 的特性，我们就可以用比较简单的方式来实现跨线程下发任务

看以下代码，从 TestThread 运行后弹出的线程名可以知道， Toast 是在 Thread_1 被弹出来的。如果将 Thread_2 想像成主线程的话，那么以下代码就相当于从主线程向子线程下发耗时任务了，这个实现思路就相当于 Android 提供的 HandlerThread 类

```java
	inner class TestThread : Thread("Thread_1") {

        override fun run() {
            Looper.prepare()
            val looper = Looper.myLooper()
            object : Thread("Thread_2") {
                override fun run() {
                    val handler = Handler(looper!!)
                    handler.post {
                        //输出结果是：Thread_1
                        Toast.makeText(
                            this@MainActivity,
                            Thread.currentThread().name,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }.start()
            Looper.loop()
        }

    }
```

#### 16、如何判断当前是不是主线程

通过 Looper 来判断

```kotlin
        if (Looper.myLooper() == Looper.getMainLooper()) {
            //是主线程
        }

        if (Looper.getMainLooper().isCurrentThread){
            //是主线程
        }
```

#### 17、如何全局捕获主线程异常

比较卧槽的一个做法就是**通过嵌套 Loop 循环来实现**。向主线程 Loop 发送 一个 Runnable，在 Runnable 里死循环执行 Loop 循环，这就会使得主线程消息队列中的所有任务都会被交由该 Runnable 来调用，只要加上 try catch 后就可以捕获主线程的任意异常了，做到**主线程永不崩溃**

```kotlin
        Handler(Looper.getMainLooper()).post {
            while (true) {
                try {
                    Looper.loop()
                } catch (throwable: Throwable) {
                    throwable.printStackTrace()
                    Log.e("TAG", throwable.message ?: "")
                }
            }
        }
```

### 六、文章推荐

[三方库源码笔记（1）-EventBus 源码详解](https://juejin.im/post/6881265680465788936)

[三方库源码笔记（2）-EventBus 自己实现一个？](https://juejin.im/post/6881808026647396366)

[三方库源码笔记（3）-ARouter 源码详解](https://juejin.im/post/6882553066285957134)

[三方库源码笔记（4）-ARouter 自己实现一个？](https://juejin.im/post/6883105868326862856)

[三方库源码笔记（5）-LeakCanary 源码详解](https://juejin.im/post/6884225131015569421)

[三方库源码笔记（6）-LeakCanary 扩展阅读](https://juejin.im/post/6884526739646185479)

[三方库源码笔记（7）-超详细的 Retrofit 源码解析](https://juejin.im/post/6886121327845965838)

[三方库源码笔记（8）-Retrofit 与 LiveData 的结合使用](https://juejin.im/post/6887408273213882375)

[三方库源码笔记（9）-超详细的 Glide 源码详解](https://juejin.im/post/6891307560557608967)

[三方库源码笔记（10）-Glide 你可能不知道的知识点](https://juejin.im/post/6892751013544263687)

[三方库源码笔记（11）-OkHttp 源码详解](https://juejin.im/post/6895369745445748749)

[三方库源码笔记（12）-OkHttp / Retrofit 开发调试利器](https://juejin.im/post/6895740949025177607)

[三方库源码笔记（13）-可能是全网第一篇 Coil 的源码分析文章](https://juejin.cn/post/6897872882051842061)