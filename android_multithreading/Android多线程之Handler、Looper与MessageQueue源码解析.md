> 本系列文章会陆续对 Android 的多线程机制进行整体介绍，帮助读者了解 Android 环境下如何实现多线程编程，也算是对自己所学内容的一个总结归纳
>
> 项目主页：https://github.com/leavesC/AndroidAllGuide

本文的目的是来分析下 Android 系统中以 Handler、Looper、MessageQueue 组成的异步消息处理机制，通过源码来了解整个消息处理流程的走向以及相关三者之间的关系

需要先了解以下几个基本概念

- Handler：主线程或者子线程通过 Handler 向 MessageQueue（消息队列） 发送 Message，以此来触发定时任务或者更新 UI 
- MessageQueue：通过 Handler 发送的消息并非是立即执行的，需要存入消息队列中来依次执行，消息队列中的任务依照消息的优先级高低（延时时间的长短）来顺序存放
- Looper：Looper 用于从 MessageQueue 中循环获取消息并将之传递给消息处理者（即消息发送者 Handler 本身）来进行处理，每条 Message 都有个 target 变量用来指向消息的发送者本身，以此把 Message 和其处理者关联起来
- 互斥机制：可能会有多条线程（1条 UI 线程，n 条子线程）同时向同一个消息队列插入消息，此时就需要有同步机制来保证消息的有序性以避免竞态

先从开发者日常的使用方法作为入口，以此来分析其整个流程的走向

Handler 发送消息的形式主要有以下几个方法，不管其是否是延时任务，其最终调用的都是 `sendMessageAtTime()` 方法

```java
    public final boolean sendMessage(Message msg){
        return sendMessageDelayed(msg, 0);
    }
    
    public final boolean post(Runnable r){
       return  sendMessageDelayed(getPostMessage(r), 0);
    }

    public final boolean sendMessageDelayed(Message msg, long delayMillis){
        if (delayMillis < 0) {
            delayMillis = 0;
        }
        return sendMessageAtTime(msg, SystemClock.uptimeMillis() + delayMillis);
    }
```

 `sendMessageAtTime()` 方法中需要一个已初始化的 `MessageQueue` 类型的全局变量 `mQueue`，否则程序无法继续走下去

```java
    public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
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

而 `mQueue` 变量是在构造函数中进行初始化的，且 `mQueue` 是成员常量，这说明 `Handler` 与 `MessageQueue` 是一一对应的关系，不可更改

如果构造函数没有传入 `Looper` 参数，则会默认使用当前线程关联的 `Looper` 对象，`mQueue` 需要依赖于从 `Looper` 对象中获取，如果 `Looper` 对象为 null ，则会直接抛出异常，且从异常信息 `Can't create handler inside thread that has not called Looper.prepare()` 中可以看到，在向 `Handler` 发送消息前，需要先调用 `Looper.prepare()`

```java
    public Handler(Callback callback, boolean async) {
        ···
        mLooper = Looper.myLooper();
        if (mLooper == null) {
            throw new RuntimeException(
                "Can't create handler inside thread that has not called Looper.prepare()");
        }
        mQueue = mLooper.mQueue;
        mCallback = callback;
        mAsynchronous = async;
    }
```

走进 `Looper` 类中，可以看到，`myLooper()` 方法是从 `sThreadLocal` 对象中获取 `Looper` 对象的，`sThreadLocal` 对象又是通过 `prepare(boolean)` 来进行赋值的，且该方法只允许调用一次，一个线程只能创建一个 Looper 对象，否则将抛出异常

```java
  	static final ThreadLocal<Looper> sThreadLocal = new ThreadLocal<Looper>();    

	public static @Nullable Looper myLooper() {
        return sThreadLocal.get();
    }

    private static void prepare(boolean quitAllowed) {
        //只允许赋值一次
        //如果重复赋值则抛出异常
        if (sThreadLocal.get() != null) {
            throw new RuntimeException("Only one Looper may be created per thread");
        }
        sThreadLocal.set(new Looper(quitAllowed));
    }
```

此处除了因为`prepare(boolean)`多次调用会抛出异常导致无法关联多个 Looper 外，Looper 类的构造函数也是私有的，且在构造函数中还初始化了一个线程常量 `mThread`，这都说明了 Looper 只能关联到一个线程，且关联之后不能改变

```java
	final Thread mThread;    

	private Looper(boolean quitAllowed) {
        mQueue = new MessageQueue(quitAllowed);
        mThread = Thread.currentThread();
    }
```

那么 `Looper.prepare(boolean)` 方法又是在哪里调用的呢？查找该方法的所有引用，可以发现在 `Looper` 类中有如下方法，从名字来看，可以猜测该方法是由主线程来调用的，查找其引用

```java
    public static void prepareMainLooper() {
        prepare(false);
        synchronized (Looper.class) {
            if (sMainLooper != null) {
                throw new IllegalStateException("The main Looper has already been prepared.");
            }
            sMainLooper = myLooper();
        }
    }
```

最后定位到 `ActivityThread` 类的 `main()` 方法

看到 `main()` 函数的方法签名，可以知道该方法就是一个应用的起始点，即当应用启动时， 系统就自动为我们在主线程做好了 `Handler` 的初始化操作， 因此在主线程里我们可以直接使用 `Handler`

如果是在子线程中创建 `Handler` ，则需要我们手动来调用 `Looper.prepare()` 方法

```java
	public static void main(String[] args) {
	    ···
        Looper.prepareMainLooper();

        ActivityThread thread = new ActivityThread();
        thread.attach(false);

        if (sMainThreadHandler == null) {
            sMainThreadHandler = thread.getHandler();
        }

        if (false) {
            Looper.myLooper().setMessageLogging(new
                    LogPrinter(Log.DEBUG, "ActivityThread"));
        }

        // End of event ActivityThreadMain.
        Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
        Looper.loop();

        throw new RuntimeException("Main thread loop unexpectedly exited");
    }
```

回到最开始，既然 `Looper` 对象已经由系统来为我们初始化好了，那我们就可以从中得到 `mQueue`对象

```java
    public Handler(Callback callback, boolean async) {
        ···
        mLooper = Looper.myLooper();
        if (mLooper == null) {
            throw new RuntimeException(
                "Can't create handler inside thread that has not called Looper.prepare()");
        }
        //获取 MessageQueue 对象
        mQueue = mLooper.mQueue;
        mCallback = callback;
        mAsynchronous = async;
    }
```

`mQueue` 又是在 `Looper` 类的构造函数中初始化的，且 `mQueue` 是 `Looper` 类的成员常量，这说明 Looper 与 MessageQueue 是一一对应的关系

```java
    private Looper(boolean quitAllowed) {
        mQueue = new MessageQueue(quitAllowed);
        mThread = Thread.currentThread();
    }
```

`sendMessageAtTime()` 方法中在处理 `Message` 时，最终调用的是 `enqueueMessage()` 方法

当中，需要注意 `msg.target = this` 这句代码，**target** 对象指向了**发送消息的主体**，即 **Handler** 对象本身，即由 **Handler** 对象发给 **MessageQueue** 的消息最后还是要交由 **Handler** 对象本身来处理

```java
    public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
        MessageQueue queue = mQueue;
        if (queue == null) {
            RuntimeException e = new RuntimeException(
                    this + " sendMessageAtTime() called with no mQueue");
            Log.w("Looper", e.getMessage(), e);
            return false;
        }
        return enqueueMessage(queue, msg, uptimeMillis);
    }

	private boolean enqueueMessage(MessageQueue queue, Message msg, long uptimeMillis) {
        //target 对象指向的也是发送消息的主体，即 Handler 对象
        //即由 Handler 对象发给 MessageQueue 的消息最后还是要交由 Handler 对象本身来处理
        msg.target = this;
        if (mAsynchronous) {
            msg.setAsynchronous(true);
        }
        return queue.enqueueMessage(msg, uptimeMillis);
    }
```

因为存在多个线程同时往同一个 Loop 线程的 MessageQueue 中插入消息的可能，所以 `enqueueMessage()` 内部需要进行同步。可以看出 MessageQueue 内部是以链表的结构来存储 Message 的（**Message.next**），根据 Message 的延时时间的长短来将决定其在消息队列中的位置

**mMessages** 代表的是消息队列中的第一条消息，如果 mMessages 为空，说明消息队列是空的，或者 mMessages 的触发时间要比新消息晚，则将新消息插入消息队列的头部；如果 mMessages 不为空，则寻找消息列队中第一条触发时间比新消息晚的非空消息，并将新消息插到该消息前面

到此，一个按照处理时间进行排序的消息队列就完成了，后边要做的就是从消息队列中依次取出消息进行处理了

```java
boolean enqueueMessage(Message msg, long when) {
    	//Message 必须有处理者
        if (msg.target == null) {
            throw new IllegalArgumentException("Message must have a target.");
        }
        if (msg.isInUse()) {
            throw new IllegalStateException(msg + " This message is already in use.");
        }

        synchronized (this) {
            if (mQuitting) {
                IllegalStateException e = new IllegalStateException(
                        msg.target + " sending message to a Handler on a dead thread");
                Log.w(TAG, e.getMessage(), e);
                msg.recycle();
                return false;
            }

            msg.markInUse();
            msg.when = when;
            Message p = mMessages;
            boolean needWake;
            //如果消息队列是空的或者队列中第一条的消息的触发时间要比新消息长，则将新消息作为链表头部
            if (p == null || when == 0 || when < p.when) {
                // New head, wake up the event queue if blocked.
                msg.next = p;
                mMessages = msg;
                needWake = mBlocked;
            } else {
                // Inserted within the middle of the queue.  Usually we don't have to wake
                // up the event queue unless there is a barrier at the head of the queue
                // and the message is the earliest asynchronous message in the queue.
                needWake = mBlocked && p.target == null && msg.isAsynchronous();
                Message prev;
                //寻找消息列队中第一条触发时间比新消息晚的消息，并将新消息插到该消息前面
                for (;;) {
                    prev = p;
                    p = p.next;
                    if (p == null || when < p.when) {
                        break;
                    }
                    if (needWake && p.isAsynchronous()) {
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

下面再看下 MessageQueue 是如何读取 Message 并回调给 Handler 的

在 MessageQueue 中消息的读取其实是通过内部的 `next()` 方法进行的，`next()` 方法是一个无限循环的方法，如果消息队列中没有消息，则该方法会一直阻塞，当有新消息来的时候 `next()` 方法会返回这条消息并将其从单链表中删除

```java
    Message next() {
        // Return here if the message loop has already quit and been disposed.
        // This can happen if the application tries to restart a looper after quit
        // which is not supported.
        final long ptr = mPtr;
        if (ptr == 0) {
            return null;
        }

        int pendingIdleHandlerCount = -1; // -1 only during first iteration
        int nextPollTimeoutMillis = 0;
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
                if (msg != null && msg.target == null) {
                    // Stalled by a barrier.  Find the next asynchronous message in the queue.
                    do {
                        prevMsg = msg;
                        msg = msg.next;
                    } while (msg != null && !msg.isAsynchronous());
                }
                if (msg != null) {
                    if (now < msg.when) {
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

                // Process the quit message now that all pending messages have been handled.
                if (mQuitting) {
                    dispose();
                    return null;
                }

                // If first time idle, then get the number of idlers to run.
                // Idle handles only run if the queue is empty or if the first message
                // in the queue (possibly a barrier) is due to be handled in the future.
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

            // Run the idle handlers.
            // We only ever reach this code block during the first iteration.
            for (int i = 0; i < pendingIdleHandlerCount; i++) {
                final IdleHandler idler = mPendingIdleHandlers[i];
                mPendingIdleHandlers[i] = null; // release the reference to the handler

                boolean keep = false;
                try {
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

            // Reset the idle handler count to 0 so we do not run them again.
            pendingIdleHandlerCount = 0;

            // While calling an idle handler, a new message could have been delivered
            // so go back and look again for a pending message without waiting.
            nextPollTimeoutMillis = 0;
        }
    }
```

`next()` 方法又是通过 `Looper` 类的 `loop()` 方法来循环调用的，而 `loop()` 方法也是一个无限循环，唯一跳出循环的条件就是 `queue.next()` 方法返回为null ，细心的读者可能已经发现了，`loop()` 就是在 `ActivityThread` 的 `main()`函数中调用的

因为 `next()` 方法是一个阻塞操作，所以当没有消息也会导致 `loop()` 方法一只阻塞着，而当 MessageQueue 一中有了新的消息，Looper 就会及时地处理这条消息并调用 `Message.target.dispatchMessage(Message)` 方法将消息传回给 Handler 进行处理 

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
        final MessageQueue queue = me.mQueue;

        // Make sure the identity of this thread is that of the local process,
        // and keep track of what that identity token actually is.
        Binder.clearCallingIdentity();
        final long ident = Binder.clearCallingIdentity();

        for (;;) {
            Message msg = queue.next(); // might block
            if (msg == null) {
                // No message indicates that the message queue is quitting.
                return;
            }

            // This must be in a local variable, in case a UI event sets the logger
            final Printer logging = me.mLogging;
            if (logging != null) {
                logging.println(">>>>> Dispatching to " + msg.target + " " +
                        msg.callback + ": " + msg.what);
            }

            final long slowDispatchThresholdMs = me.mSlowDispatchThresholdMs;

            final long traceTag = me.mTraceTag;
            if (traceTag != 0 && Trace.isTagEnabled(traceTag)) {
                Trace.traceBegin(traceTag, msg.target.getTraceName(msg));
            }
            final long start = (slowDispatchThresholdMs == 0) ? 0 : SystemClock.uptimeMillis();
            final long end;
            try {
                msg.target.dispatchMessage(msg);
                end = (slowDispatchThresholdMs == 0) ? 0 : SystemClock.uptimeMillis();
            } finally {
                if (traceTag != 0) {
                    Trace.traceEnd(traceTag);
                }
            }
            if (slowDispatchThresholdMs > 0) {
                final long time = end - start;
                if (time > slowDispatchThresholdMs) {
                    Slog.w(TAG, "Dispatch took " + time + "ms on "
                            + Thread.currentThread().getName() + ", h=" +
                            msg.target + " cb=" + msg.callback + " msg=" + msg.what);
                }
            }

            if (logging != null) {
                logging.println("<<<<< Finished to " + msg.target + " " + msg.callback);
            }

            // Make sure that during the course of dispatching the
            // identity of the thread wasn't corrupted.
            final long newIdent = Binder.clearCallingIdentity();
            if (ident != newIdent) {
                Log.wtf(TAG, "Thread identity changed from 0x"
                        + Long.toHexString(ident) + " to 0x"
                        + Long.toHexString(newIdent) + " while dispatching to "
                        + msg.target.getClass().getName() + " "
                        + msg.callback + " what=" + msg.what);
            }

            msg.recycleUnchecked();
        }
    }
```

看下 Handler 对象处理消息的方法

```java
    /**
     * Handle system messages here.
     */
    public void dispatchMessage(Message msg) {
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

如果 `msg.callback` 不为 null ，则调用 **callback** 对象的 `run()` 方法，该 callback 实际上就是一个 **Runnable** 对象，对应的是 Handler 对象的 `post()` 方法

```java
    private static void handleCallback(Message message) {
        message.callback.run();
    }
```

```java
    public final boolean post(Runnable r){
       return  sendMessageDelayed(getPostMessage(r), 0);
    }

    private static Message getPostMessage(Runnable r) {
        Message m = Message.obtain();
        m.callback = r;
        return m;
    }
```

如果 `mCallback` 不为 null ，则通过该回调接口来处理消息，如果在初始化 Handler 对象时没有通过构造函数传入 `Callback` 回调接口，则交由 `handleMessage(Message)` 方法来处理消息，我们一般也是通过重写 Handler 的 `handleMessage(Message)` 方法来处理消息

最后来总结下以上的内容

1. 在创建 Handler 实例时要么为构造函数提供一个 Looper 实例，要么默认使用当前线程关联的 Looper 对象，如果当前线程没有关联的 Looper 对象，则会导致抛出异常

2. Looper 与 Thread ，Looper 与 MessageQueue 都是一一对应的关系，在关联后无法更改，但 Handler 与 Looper 可以是多对一的关系

3. Handler 能用于更新 UI 有个前提条件：Handler 与主线程关联在了一起。在主线程中初始化的 Handler 会默认与主线程绑定在一起，所以此后在处理 Message 时，`handleMessage(Message msg)` 方法的所在线程就是主线程，因此 Handler 能用于更新 UI 

4. 可以创建关联到另一个线程 Looper 的 Handler，只要本线程能够拿到另外一个线程的 Looper 实例


```java
		new Thread("Thread_1") {
            @Override
            public void run() {
                Looper.prepare();
                final Looper looper = Looper.myLooper();
                new Thread("Thread_2") {
                    @Override
                    public void run() {
                        Handler handler = new Handler(looper);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                //输出结果是：Thread_1
                                Log.e(TAG, Thread.currentThread().getName());
                            }
                        });
                    }
                }.start();
                Looper.loop();
            }
        }.start();
```



**更多的源码解读请看这里：[AndroidAllGuide](https://github.com/leavesC/AndroidAllGuide)**