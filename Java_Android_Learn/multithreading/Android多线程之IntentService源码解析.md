> 想要了解 IntentService 的工作原理需要先对 Android 系统中以 Handler、Looper、MessageQueue 组成的异步消息处理机制以及 HandlerThread 有所了解，如果你还没有这方面的知识，可以先看我写的另外两篇文章：
>
> [Handler、Looper与MessageQueue源码解析](./Android多线程之Handler、Looper与MessageQueue源码解析.md)
>
> [Android 多线程之 HandlerThread 源码解析](./Android多线程之HandlerThread源码解析.md)

在介绍 Service 时我们经常会说 Service 适合于完成一些在后台工作的任务，例如播放音乐、下载文件等，但 Service 默认是运行于 UI 线程的，如果想要依靠其来完成一些耗时任务，就需要自己来建立子线程，这相对比较繁琐，所以官方也为开发者提供了 IntentService 来解决这一问题

IntentService 有以下几个特性：

- IntentService 内部创建了一个工作线程，用于在子线程内执行传递给 `onStartCommand()` 的所有 Intent，开发者无须关心多线程问题
- IntentService 内部通过 HandlerThread 和 Handler 来实现异步操作
- IntentService 是以串行方式处理外部传递来的任务，即只有当上一个任务完成时，新的任务才会被执行
- 在处理完所有任务请求后会自动停止，因此不必手动调用 `stopSelf()` 方法
- 提供了 `onBind()` 的默认实现（返回 null）
- IntentService 是四大组件之一，拥有较高的优先级，不易被系统杀死，因此适合于执行一些高优先级的异步任务

IntentService 总的代码不到一百行，如果你对 HandlerThread 有所了解，就可以很容易地了解 IntentService 的工作机制

先看下 IntentService 的类声明

IntentService 类是一个抽象类，包含一个抽象方法需要由子类来实现

```java
	public abstract class IntentService extends Service
```

包含的成员变量

```java
    private volatile Looper mServiceLooper;

    //关联了 mServiceLooper 的 Handler 
    private volatile ServiceHandler mServiceHandler;

    //IntentService 内部使用的子线程的线程名
    private String mName;

    //用于决定 onStartCommand() 方法的返回值
    private boolean mRedelivery;
```

构造函数

```java
    //传入子线程名
    public IntentService(String name) {
        super();
        mName = name;
    }
```

当 IntentService 第一次被启动时，`onCreate()` 方法就会调用

就如同我们在 **Activity** 中使用 **HandlerThread** 来完成耗时任务一样，在 **IntentService** 内部一样是将 **HandlerThread** 作为工作线程，因为 **mServiceHandler** 持有了 **HandlerThread** 的 **Looper** 对象，因此 **mServiceHandler** 就作为 **IntentService** 向 **HandlerThread** 发送 **Message** 的桥梁

```java
    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread("IntentService[" + mName + "]");
        //触发 HandlerThread 创建 Looper 对象
        thread.start();
        //获取 Looper 对象，以此来构建可以向子线程发送 Message 的 Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }
```

这里再看下 ServiceHandler 的类声明

当中，消息队列中的 Message 依次被传递给 `handleMessage(Message)` 方法进行处理，该方法是运行于子线程的，而耗时任务就交由抽象方法 `onHandleIntent(Intent)` 来完成，该方法需要交由子类来实现，并在该方法返回后调用 `stopSelf(Int)` 来停止 IntentService 

可以看到，整个流程就是上面所说的

1. IntentService 是以串行方式处理外部传递来的任务，即只有当上一个任务完成时，新的任务才会被执行
2. 在处理完所有任务请求后会自动停止，因此不必手动调用 `stopSelf()` 方法

但要注意的是，调用 `stopSelf(Int)`不一定会使 IntentService 停止，因为消息队列中可能还有未处理的消息，这个在后边会介绍

```java
    private final class ServiceHandler extends Handler {

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            //将耗时任务转交给 onHandleIntent() 方法来完成
            //onHandleIntent() 需要由子类来实现
            onHandleIntent((Intent)msg.obj);
            //msg.arg1 即 startId
            stopSelf(msg.arg1);
        }
    }
```

每次启动 IntentService 时，`onStartCommand()` 方法就会被调用，最后是通过 `onStart()` 方法向 **mServiceHandler** 发送 包含此次任务详情的 **Message** 对象

需要特别注意的是 **startId** 这个参数。每次回调 `onStartCommand()` 方法时，参数 startId 的值都是自动递增的，startId 用于唯一标识每次对 IntentService 发起的处理请求。如果 IntentService 同时处理多个 `onStartCommand()` 请求，则不应在处理完一个启动请求之后立即销毁 IntentService 。因为此时可能已经收到了新的启动请求，在第一个请求结束时停止服务会导致第二个请求被终止。为了避免这一问题，可以使用 `stopSelf(int)` 确保 IntentService 停止请求始终是基于最新一次的启动请求。也就是说，如果调用 `stopSelf(int)` 方法的参数值与 `onStartCommand()` 接受到的最新的 startId 值不相符的话，`stopSelf(int)` 方法就会失效，从而避免终止尚未处理的请求

```java
    @Override
    public void onStart(@Nullable Intent intent, int startId) {
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        mServiceHandler.sendMessage(msg);
    }

    //每次回调 onStartCommand() 方法时，参数 startId 的值都是自动递增的，startId 用于唯一标识每次对 Service 发起的处理请求
    //如果 Service 同时处理多个 onStartCommand() 请求，则不应在处理完一个启动请求之后立即销毁 Service
    //因为此时可能已经收到了新的启动请求，在第一个请求结束时停止服务会导致第二个请求被终止
    //为了避免这一问题，可以使用 stopSelf(int) 确保 Service 停止请求始终是基于最新一次的启动请求
    //也就是说，如果调用 stopSelf(int) 方法的参数值与 onStartCommand() 接受到的最新的 startId 值不相符的话，stopSelf(int) 方法就会失效，从而避免终止尚未处理的请求
    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        onStart(intent, startId);
        return mRedelivery ? START_REDELIVER_INTENT : START_NOT_STICKY;
    }
```

当中，`onStartCommand()` 方法的返回值可能由 `setIntentRedelivery(boolean)` 方法来决定

```java
    //用于决定 onStartCommand() 方法的返回值
    //如果参数为 true 则返回 START_REDELIVER_INTENT
    //如果系统在 onStartCommand() 返回后终止服务，则会重建服务，并通过传递给服务的最后一个 Intent 调用 onStartCommand()
    //任何挂起 Intent 均依次传递。这适用于主动执行应该立即恢复的作业（例如下载文件）的服务
    //如果参数为 false 则返回 START_NOT_STICKY
    //如果系统在 onStartCommand() 返回后终止服务，则除非有挂起 Intent 要传递，否则系统不会重建服务
    //这是最安全的选项，可以避免在不必要时以及应用能够轻松重启所有未完成的作业时运行服务
    public void setIntentRedelivery(boolean enabled) {
        mRedelivery = enabled;
    }
```

还有其它几个相关方法

```java
    @Override
    public void onDestroy() {
        //移除所有待发送的 Message
        mServiceLooper.quit();
    }

    @Override
    @Nullable
    public IBinder onBind(Intent intent) {
        return null;
    }

    //此方法需要由子类来实现
    //在此方式中完成具体的耗时任务
    @WorkerThread
    protected abstract void onHandleIntent(@Nullable Intent intent);
```

IntentService 的源码理解起来不算难，不过这是建立在对 Android 系统中以 Handler、Looper、MessageQueue 组成的异步消息处理机制以及 HandlerThread 有所了解的基础上的，最后就再贴上对 IntentService 的全部源码注释

```java
package android.app;

import android.annotation.WorkerThread;
import android.annotation.Nullable;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

/**
 * 作者：叶应是叶
 * 时间：2018/6/22 13:39
 * 描述：https://github.com/leavesC
 * https://www.jianshu.com/u/9df45b87cfdf
 */
public abstract class IntentService extends Service {

    private volatile Looper mServiceLooper;

    //关联了 mServiceLooper 的 Handler
    private volatile ServiceHandler mServiceHandler;

    //IntentService 内部使用的子线程的线程名
    private String mName;

    //用于决定 onStartCommand() 方法的返回值
    private boolean mRedelivery;

    private final class ServiceHandler extends Handler {

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            //将耗时任务转交给 onHandleIntent() 方法来完成
            //onHandleIntent() 需要由子类来实现
            onHandleIntent((Intent)msg.obj);
            //msg.arg1 即 startId
            //如果当前
            stopSelf(msg.arg1);
        }
    }

    //传入子线程名
    public IntentService(String name) {
        super();
        mName = name;
    }

    //用于决定 onStartCommand() 方法的返回值
    //如果参数为 true 则返回 START_REDELIVER_INTENT
    //如果系统在 onStartCommand() 返回后终止服务，则会重建服务，并通过传递给服务的最后一个 Intent 调用 onStartCommand()
    //任何挂起 Intent 均依次传递。这适用于主动执行应该立即恢复的作业（例如下载文件）的服务
    //如果参数为 false 则返回 START_NOT_STICKY
    //如果系统在 onStartCommand() 返回后终止服务，则除非有挂起 Intent 要传递，否则系统不会重建服务
    //这是最安全的选项，可以避免在不必要时以及应用能够轻松重启所有未完成的作业时运行服务
    public void setIntentRedelivery(boolean enabled) {
        mRedelivery = enabled;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread("IntentService[" + mName + "]");
        //触发 HandlerThread 创建 Looper 对象
        thread.start();
        //获取 Looper 对象，以此来构建可以向子线程发送 Message 的 Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public void onStart(@Nullable Intent intent, int startId) {
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        mServiceHandler.sendMessage(msg);
    }

    //每次回调 onStartCommand() 方法时，参数 startId 的值都是自动递增的，startId 用于唯一标识每次对 Service 发起的处理请求
    //如果 Service 同时处理多个 onStartCommand() 请求，则不应在处理完一个启动请求之后立即销毁 Service
    //因为此时可能已经收到了新的启动请求，在第一个请求结束时停止服务会导致第二个请求被终止
    //为了避免这一问题，可以使用 stopSelf(int) 确保 Service 停止请求始终是基于最新一次的启动请求
    //也就是说，如果调用 stopSelf(int) 方法的参数值与 onStartCommand() 接受到的最新的 startId 值不相符的话，stopSelf(int) 方法就会失效，从而避免终止尚未处理的请求
    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        onStart(intent, startId);
        return mRedelivery ? START_REDELIVER_INTENT : START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        //移除所有待发送的 Message
        mServiceLooper.quit();
    }

    @Override
    @Nullable
    public IBinder onBind(Intent intent) {
        return null;
    }

    //此方法需要由子类来实现
    //在此方式中完成具体的耗时任务
    @WorkerThread
    protected abstract void onHandleIntent(@Nullable Intent intent);

}
```

**更多的源码解读请看这里：[Java_Android_Learn](https://github.com/leavesC/Java_Android_Learn)**