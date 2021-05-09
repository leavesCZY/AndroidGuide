> 公众号：[字节数组](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/36784c0d2b924b04afb5ee09eb16ca6f~tplv-k3u1fbpfcp-watermark.image)，希望对你有所帮助 😇😇

AsyncTask 是一个较为轻量级的异步任务类，在底层通过封装 ThreadPool 和 Handler 实现了线程的复用、后台任务执行顺序的控制、子线程和主线程的切换，使得开发者可以通过很简单的方法来执行一些耗时任务

此篇文章就基于 Android API 27 版本的源码来对 AsyncTask 进行一次整体分析，以便对其底层工作流程有所了解

一般，AsyncTask 是以类似于以下的方式来调用的

```java
        new AsyncTask<String, Integer, String>() {

            @Override
            protected String doInBackground(String... strings) {
                return null;
            }
            
        }.execute("leavesC");
```

所以这里就从 `execute()` 方法入手

```java
    //以默认的串行任务执行器 sDefaultExecutor 来执行后台任务
    @MainThread
    public final AsyncTask<Params, Progress, Result> execute(Params... params) {
        return executeOnExecutor(sDefaultExecutor, params);
    }
```

`execute(Params)`方法内部调用的是 `executeOnExecutor(sDefaultExecutor, params)`方法，当中 `sDefaultExecutor`用于定义任务队列的执行方式，AsyncTask 默认使用的是串行任务执行器

```java
	//以指定的任务执行器 Executor 来执行后台任务
    @MainThread
    public final AsyncTask<Params, Progress, Result> executeOnExecutor(Executor exec, Params... params) {
        //Task 只能被执行一次，如果 mStatus != Status.PENDING ，说明 Task 被重复执行，此时将抛出异常
        if (mStatus != Status.PENDING) {
            switch (mStatus) {
                case RUNNING:
                    throw new IllegalStateException("Cannot execute task:" + " the task is already running.");
                case FINISHED:
                    throw new IllegalStateException("Cannot execute task:" + " the task has already been executed " + "(a task can be executed only once)");
            }
        }
        //将状态值置为运行状态
        mStatus = Status.RUNNING;
        //在 doInBackground() 方法之前被调用，用于做一些界面层的准备工作
        onPreExecute();
        //执行耗时任务
        mWorker.mParams = params;
        exec.execute(mFuture);
        return this;
    }
```

`mStatus`是一个枚举变量，用于定义当前 Task 的运行状态，用于防止 Task 被重复执行

```java
	//用于标记 Task 的当前状态
    public enum Status {
        //Task 还未运行
        PENDING,

        //Task 正在运行
        RUNNING,

        //Task 已经结束
        FINISHED,
    }
```

之后就调用任务执行器，提交任务

```java
		//执行耗时任务
        mWorker.mParams = params;
        exec.execute(mFuture);
```

`executeOnExecutor(Executor, Params)`方法可以从外部传入自定义的任务执行器对象，例如可以传入 `AsyncTask.THREAD_POOL_EXECUTOR` 使 AsyncTask 中的任务队列以并行的方式来完成

这里先来看下默认的串行任务执行器是如何执行的

每一个被提交的任务都会被加入任务队列 `mTasks`当中，`mActive`表示当前在执行的任务，每当有新任务 `Runnable` 到来时，就会在 `Runnable` 的外层多包裹一层 `Runnable` ，然后将之插入到任务队列中，当 `execute(Runnable)`方法第一次被执行时，`mActive`为 null ，因此就会触发 `scheduleNext()`方法获取任务队列的第一个任务并提交给线程池 `THREAD_POOL_EXECUTOR` 进行处理，当 `r.run()`方法返回时（即任务处理结束），在 `finally`中又会获取下一个任务进行处理，从而实现了任务队列的串行执行

```java
	//串行任务执行器，即提交给线程池的任务是按照顺序一个接一个被执行的
    private static class SerialExecutor implements Executor {

        //任务队列
        final ArrayDeque<Runnable> mTasks = new ArrayDeque<Runnable>();

        //当前在执行的任务
        Runnable mActive;

        public synchronized void execute(final Runnable r) {
            //向任务队列尾端插入任务
            //在外部任务外部包装多一层 Runnable
            mTasks.offer(new Runnable() {
                public void run() {
                    try {
                        r.run();
                    } finally {
                        scheduleNext();
                    }
                }
            });
            //如果当前没有在执行任务，则调取队列中的任务进行处理
            if (mActive == null) {
                scheduleNext();
            }
        }

        //获取队列的首个任务并处理
        protected synchronized void scheduleNext() {
            if ((mActive = mTasks.poll()) != null) {
                THREAD_POOL_EXECUTOR.execute(mActive);
            }
        }
    }
```

再看下线程池 `THREAD_POOL_EXECUTOR` 是如何定义的

可以看到，具体的线程池实现类是 `ThreadPoolExecutor`，使用线程池从而避免了线程重复的创建与销毁操作，有利于提高系统性能

```java
	//CPU 核数量
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    //线程池中的核心线程数
    //至少有2个，最多4个，线程数至少要比 CPU 核数量少1个，以避免 CPU 与后台工作饱和
    private static final int CORE_POOL_SIZE = Math.max(2, Math.min(CPU_COUNT - 1, 4));

    //线程池容纳的最大线程数量
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;

    //线程在闲置时的存活时间（30秒），超出这个时间将被回收
    private static final int KEEP_ALIVE_SECONDS = 30;

    //线程队列
    //当 LinkedBlockingDeque 已满时，新增的任务会直接创建新线程来执行，当创建的线程数量超过最大线程数量 KEEP_ALIVE_SECONDS 时会抛出异常
    private static final BlockingQueue<Runnable> sPoolWorkQueue = new LinkedBlockingQueue<Runnable>(128);

    //线程工厂，提供创建新线程的功能，通过线程工厂可以对线程的一些属性进行定制
    private static final ThreadFactory sThreadFactory = new ThreadFactory() {

        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "AsyncTask #" + mCount.getAndIncrement());
        }

    };

    //线程池对象
    public static final Executor THREAD_POOL_EXECUTOR;

    static {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                sPoolWorkQueue, sThreadFactory);
        //包括核心线程在内的所有线程在闲置时间超出 KEEP_ALIVE_SECONDS 后都将其回收
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        THREAD_POOL_EXECUTOR = threadPoolExecutor;
    }

    //当前 Task 使用的任务执行器
    private static volatile Executor sDefaultExecutor = SERIAL_EXECUTOR;
```

看到线程池，这里就又引出了另外一个问题，后台任务是在子线程中调用的，那 AsyncTask 又是如何在主线程中回调 `onPreExecute()、onPostExecute(Result)、onProgressUpdate(Progress)`这几个方法的呢？

先看几个相关方法的声明

```java
 	//在子线程中被调用，用于执行后台任务
    @WorkerThread
    protected abstract Result doInBackground(Params... params);

    //在主线程中被调用，在 doInBackground() 方法之前调用，用于在后台任务开始前做一些准备工作
    @MainThread
    protected void onPreExecute() {
    }

    //在主线程中被调用，在 doInBackground() 方法之后调用，用于处理后台任务的执行结果
    //参数 result 是 doInBackground() 方法的返回值
    @SuppressWarnings({"UnusedDeclaration"})
    @MainThread
    protected void onPostExecute(Result result) {
    }

    //在主线程中被调用，当调用了 publishProgress() 方法后被触发
    //用于更新任务进度值
    @SuppressWarnings({"UnusedDeclaration"})
    @MainThread
    protected void onProgressUpdate(Progress... values) {
    }

    //在主线程中被调用
    //当调用了 cancel(boolean) 方法取消后台任务后会被调用
    //在 doInBackground() 方法结束时也会被调用
    //方法内部默认调用了 onCancelled() 方法
    @SuppressWarnings({"UnusedParameters"})
    @MainThread
    protected void onCancelled(Result result) {
        onCancelled();
    }

    //在主线程中被调用，被 onCancelled(Result) 方法调用
    @MainThread
    protected void onCancelled() {
    }
```

`onPreExecute()`在 `executeOnExecutor(Executor, Params)`中有被调用，因为 `executeOnExecutor()`方法被要求在主线程中调用，因此 `onPreExecute()`自然也会在主线程中被执行

其它方法的调用则涉及到了 Handler、Looper 与 MessageQueue 的相关知识点，关于这些可以从这里获取详细介绍： [**AndroidGuide**](https://github.com/leavesC/AndroidGuide) ，这里就简单介绍下

看下 AsyncTask 类的三个构造函数。当中，除了无参构造函数，其他两个构造函数都使用 `@hide`注解隐藏起来了，因此我们在一般情况下只能使用调用无参构造函数来初始化 AsyncTask

```java
	//创建一个新的异步任务，必须在主线程上调用此构造函数
    public AsyncTask() {
        this((Looper) null);
    }
    
    /**
     * 隐藏的构造函数
     * 创建一个新的异步任务，必须在主线程上调用此构造函数
     *
     * @hide
     */
    public AsyncTask(@Nullable Handler handler) {
        this(handler != null ? handler.getLooper() : null);
    }
    
    /**
     * 隐藏的构造函数
     * 创建一个新的异步任务，必须在主线程上调用此构造函数
     * @hide
     */
    public AsyncTask(@Nullable Looper callbackLooper) {
        //如果 callbackLooper 为 null 或者是等于主线程 Looper ，则以主线程 Looper 对象为参数构建一个与主线程关联的 Handler 对象
        //否则就以传入的 Looper 对象为参数来构建与子线程关联的 Handler
        mHandler = callbackLooper == null || callbackLooper == Looper.getMainLooper() ? getMainHandler() : new Handler(callbackLooper);
        mWorker = new WorkerRunnable<Params, Result>() {
            public Result call() throws Exception {
                mTaskInvoked.set(true);
                Result result = null;
                try {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                    //noinspection unchecked
                    result = doInBackground(mParams);
                    Binder.flushPendingCommands();
                } catch (Throwable tr) {
                    mCancelled.set(true);
                    throw tr;
                } finally {
                    postResult(result);
                }
                return result;
            }
        };

        mFuture = new FutureTask<Result>(mWorker) {
            @Override
            protected void done() {
                try {
                    postResultIfNotInvoked(get());
                } catch (InterruptedException e) {
                    android.util.Log.w(LOG_TAG, e);
                } catch (ExecutionException e) {
                    throw new RuntimeException("An error occurred while executing doInBackground()",
                            e.getCause());
                } catch (CancellationException e) {
                    postResultIfNotInvoked(null);
                }
            }
        };
    }
```
因此我们传给构造函数 `AsyncTask(Looper)` 的参数为 null ，因为 `mHandler` 变量其实是赋值为绑定了主线程 Looper 的 InternalHandler 变量

因为 InternalHandler 绑定了主线程的 Looper 对象，因此 `handleMessage(Message)`方法其实是在主线程被执行，从而实现了子线程和主线程之间的切换

```java
	
    //按照正常情况来说，在初始化 AsyncTask 时我们使用的都是其无参构造函数
    //因此 InternalHandler 绑定的 Looper 对象即是与主线程关联的 Looper 对象
    //所以 InternalHandler 可以用来在主线程回调某些抽象方法，例如 onProgressUpdate() 方法
    private static InternalHandler sHandler;

    //等于 sHandler
    private final Handler mHandler;

	private static class InternalHandler extends Handler {

        public InternalHandler(Looper looper) {
            super(looper);
        }

        @SuppressWarnings({"unchecked", "RawUseOfParameterizedType"})
        @Override
        public void handleMessage(Message msg) {
            AsyncTaskResult<?> result = (AsyncTaskResult<?>) msg.obj;
            switch (msg.what) {
                case MESSAGE_POST_RESULT:
                    //处理后台任务的执行结果
                    result.mTask.finish(result.mData[0]);
                    break;
                case MESSAGE_POST_PROGRESS:
                    //更新后台任务的进度
                    result.mTask.onProgressUpdate(result.mData);
                    break;
            }
        }
    }    

	//获取与主线程关联的 Looper 对象，以此为参数构建一个 Handler 对象
    //所以在 Task 的运行过程中，能够通过此 Handler 在主线程执行操作
    private static Handler getMainHandler() {
        synchronized (AsyncTask.class) {
            if (sHandler == null) {
                sHandler = new InternalHandler(Looper.getMainLooper());
            }
            return sHandler;
        }
    }
```

例如，在通过 `publishProgress(Progress)` 方法更新后台任务的执行进度时，在内部就会将进度值包装到 Message 中，然后传递给 Handler 进行处理

```java
    //运行于工作线程，此方法用于更新任务的进度值
    //会触发 onProgressUpdate() 被执行
    @WorkerThread
    protected final void publishProgress(Progress... values) {
        if (!isCancelled()) {
            //将与进度值相关的参数 Progress 包装到 AsyncTaskResult 对象当中，并传递给 Handler 进行处理 
            getHandler().obtainMessage(MESSAGE_POST_PROGRESS, new AsyncTaskResult<Progress>(this, values)).sendToTarget();
        }
    }
```

