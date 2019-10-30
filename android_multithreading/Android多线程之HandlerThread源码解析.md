> 本系列文章会陆续对 Android 的多线程机制进行整体介绍，帮助读者了解 Android 环境下如何实现多线程编程，也算是对自己所学内容的一个总结归纳
>
> 项目主页：https://github.com/leavesC/JavaKotlinAndroidGuide
>
> 想要了解 HandlerThread 的工作原理需要先对 Android 系统中以 Handler、Looper、MessageQueue 组成的异步消息处理机制有所了解，如果你还没有这方面的知识，可以先看我写的另一篇文章：[Handler、Looper与MessageQueue源码解析](https://github.com/leavesC/JavaKotlinAndroidGuide/blob/master/multithreading/Android多线程之Handler、Looper与MessageQueue源码解析.md)

#### 一、概述

先来了解下 HandlerThread 的几个特性

- HandlerThread 继承于 Thread，本身就是一个线程类
- HandlerThread 在内部维护了自己的 Looper 对象，所以可以进行 looper 循环
- 创建 HandlerThread 后需要先调用 `HandlerThread.start()` 方法再向其下发任务，通过 `run()` 方法来创建 Looper 对象
- 通过传递 HandlerThread 的 Looper 对象给 Handler 对象，从而可以通过 Handler 来向 HandlerThread 下发耗时任务

#### 二、使用方式

再来看下 HandlerThread 的使用方式

创建 HandlerThread 并调用 `start()` 方法，使其在子线程内创建 Looper 对象

```java
	HandlerThread handlerThread = new HandlerThread("HandlerThread");
    handlerThread.start();
```

然后以 HandlerThread 内部的 Looper 对象为参数创建一个 Handler，通过 Handler 向子线程发送 Message，以此下发耗时任务，消息的接收者与任务的处理者则由回调函数 ChildCallback 来完成

```java
	Handler childThreadHandler = new Handler(handlerThread.getLooper(), new ChildCallback());
	
	//Callback 运行于子线程
    private class ChildCallback implements Handler.Callback {
        @Override
        public boolean handleMessage(Message msg) {
            //在此可以进行耗时操作
            //如果需要更新UI，则需要通过主线程的Handler来完成
            return false;
        }
    }
```

完整的示例代码如下所示

```java
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    //Callback 运行于子线程
    private class ChildCallback implements Handler.Callback {
        @Override
        public boolean handleMessage(Message msg) {
            //在此可以进行耗时操作
            //如果需要更新UI，则需要通过主线程的Handler来完成
            Log.e(TAG, "ChildCallback 当前线程名：" + Thread.currentThread().getName() + "   " + "当前线程ID：" + Thread.currentThread().getId());
            Log.e(TAG, "耗时任务开始");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.e(TAG, "耗时任务结束");
            //通知界面更新UI
            uiHandler.sendEmptyMessage(1);
            return false;
        }
    }

    //运行于主线程的Handler，用于更新UI
    @SuppressLint("HandlerLeak")
    private Handler uiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.e(TAG, "uiHandler 当前线程名：" + Thread.currentThread().getName() + "   " + "当前线程ID：" + Thread.currentThread().getId());
            Toast.makeText(MainActivity.this, "耗时操作完成", Toast.LENGTH_LONG).show();
        }
    };

    //用于向子线程发布耗时任务的Handler
    private Handler childThreadHandler;

    private HandlerThread handlerThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        handlerThread = new HandlerThread("HandlerThread");
        handlerThread.start();
        childThreadHandler = new Handler(handlerThread.getLooper(), new ChildCallback());
        Log.e(TAG, "onCreate 当前线程名：" + Thread.currentThread().getName() + "   " + "当前线程ID：" + Thread.currentThread().getId());
    }

    public void startTask(View view) {
        childThreadHandler.sendEmptyMessage(1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handlerThread.quit();
        childThreadHandler.removeCallbacksAndMessages(null);
    }

}
```

程序运行后的日志如下所示，可以看出各个方法在调用时所处的线程

```java
06-22 02:51:41.779 21977-21977/com.leavesc.myapplication E/MainActivity: onCreate 当前线程名：main   当前线程ID：2
06-22 02:51:44.927 21977-21995/com.leavesc.myapplication E/MainActivity: ChildCallback 当前线程名：HandlerThread   当前线程ID：497
06-22 02:51:44.928 21977-21995/com.leavesc.myapplication E/MainActivity: 耗时任务开始
06-22 02:51:49.930 21977-21995/com.leavesc.myapplication E/MainActivity: 耗时任务结束
06-22 02:51:49.930 21977-21977/com.leavesc.myapplication E/MainActivity: uiHandler 当前线程名：main   当前线程ID：2
```

#### 三、源码分析

先看下 HandlerThread 的类声明，是 Thread 的子类

```java
	public class HandlerThread extends Thread
```

两个构造函数，可以传入的参数分别是**线程名**和**线程优先级**

```java
	public HandlerThread(String name) {
        super(name);
        //使用默认的线程优先级
        mPriority = Process.THREAD_PRIORITY_DEFAULT;
    }

    public HandlerThread(String name, int priority) {
        super(name);
        //使用自定义的线程优先级
        mPriority = priority;
    }
```

看下其 `run()` 方法

```java
	@Override
    public void run() {
        mTid = Process.myTid();
        //触发当前线程创建 Looper 对象
        Looper.prepare();
        synchronized (this) {
            //获取 Looper 对象
            mLooper = Looper.myLooper();
            //唤醒在等待的线程
            //唤醒 getLooer() 中可能还处于等待状态的线程
            notifyAll();
        }
        //设置线程优先级
        Process.setThreadPriority(mPriority);
        onLooperPrepared();
        //开启消息循环
        Looper.loop();
        mTid = -1;
    }
```

`Looper.prepare()` 方法用于为当前线程创建一个 Looper 对象，在主线程需要依赖此 Looper 对象来构建一个 Handler 对象，通过该 Handler 对象来向子线程下发耗时任务

之后可以看到有一个同步代码块，在当中调用了 `notifyAll()`来唤醒等待线程，那该唤醒的又是哪个线程呢？这里需要明确各个方法是运行于哪个线程，`run()` 方法肯定是运行于子线程，但用于向 HandlerThread 下发任务的 Handler 是初始化于主线程，因此 `getLooper()`方法也是运行于主线程的。由于是两个不同的线程，`run()` 方法和 `getLooper()` 的运行先后顺序是不明确的，因此 `getLooper()` 方法需要确保 Looper 对象不为 **null** 时才返回，否则将一直阻塞等待 Looper 对象初始化完成

```java
	//获取与当前线程关联的 Looper 对象
    //因为 getLooper() 方法可能先于 run() 被调用，此时就需要先等待 Looper 对象被创建
    public Looper getLooper() {
        //如果当前线程未在运行，则返回 null 
        if (!isAlive()) {
            return null;
        }
        synchronized (this) {
            //如果当前线程已处理运行状态（已调用 start() 方法）且 Looper 对象还未创建
            //则调用 wait() 方法释放锁，等待 Looper 对象创建
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

Looper 对象初始化完成后，就需要调用 `Looper.loop()` 来开启消息循环，至此 HandlerThread 的初始化操作就完成了

最后再贴一下 HandlerThread 的完整源码注释

```java
package android.os;

import android.annotation.NonNull;
import android.annotation.Nullable;

/**
 * Handy class for starting a new thread that has a looper. The looper can then be 
 * used to create handler classes. Note that start() must still be called.
 */
public class HandlerThread extends Thread {

    //线程优先级
    int mPriority;

    //线程ID
    int mTid = -1;

    //当前线程持有的Looper对象
    Looper mLooper;

    //包含当前 Looper 对象的 Handler
    private @Nullable Handler mHandler;

    public HandlerThread(String name) {
        super(name);
        //使用默认的线程优先级
        mPriority = Process.THREAD_PRIORITY_DEFAULT;
    }

    public HandlerThread(String name, int priority) {
        super(name);
        //使用自定义的线程优先级
        mPriority = priority;
    }
    
    //在 Looper 循环启动前调用
    //此处是空实现，留待子类重写
    protected void onLooperPrepared() {
    }

    @Override
    public void run() {
        mTid = Process.myTid();
        //触发当前线程创建 Looper 对象
        Looper.prepare();
        synchronized (this) {
            //获取 Looper 对象
            mLooper = Looper.myLooper();
            //唤醒在等待的线程
            //唤醒 getLooer() 中可能还处于等待状态的线程
            notifyAll();
        }
        //设置线程优先级
        Process.setThreadPriority(mPriority);
        onLooperPrepared();
        //开启消息循环
        Looper.loop();
        mTid = -1;
    }
    
    //获取与当前线程关联的 Looper 对象
    //因为 getLooper() 方法可能先于 run() 被调用，此时就需要先等待 Looper 对象被创建
    public Looper getLooper() {
        //如果当前线程未在运行，则返回 null 
        if (!isAlive()) {
            return null;
        }
        synchronized (this) {
            //如果当前线程已处理运行状态（已调用 start() 方法）且 Looper 对象还未创建
            //则调用 wait() 方法释放锁，等待 Looper 对象创建
            while (isAlive() && mLooper == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
        }
        return mLooper;
    }

    /**
     * @return a shared {@link Handler} associated with this thread
     * @hide
     */
    @NonNull
    public Handler getThreadHandler() {
        if (mHandler == null) {
            mHandler = new Handler(getLooper());
        }
        return mHandler;
    }

    //清空消息队列中所有的消息
    public boolean quit() {
        Looper looper = getLooper();
        if (looper != null) {
            looper.quit();
            return true;
        }
        return false;
    }

    //清空消息队列中所有非延时消息
    public boolean quitSafely() {
        Looper looper = getLooper();
        if (looper != null) {
            looper.quitSafely();
            return true;
        }
        return false;
    }

    /**
     * Returns the identifier of this thread. See Process.myTid().
     */
    public int getThreadId() {
        return mTid;
    }

}
```



**更多的源码解读请看这里：[JavaKotlinAndroidGuide](https://github.com/leavesC/JavaKotlinAndroidGuide)**