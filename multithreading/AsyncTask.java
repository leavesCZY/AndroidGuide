package android.os;

import android.annotation.MainThread;
import android.annotation.Nullable;
import android.annotation.WorkerThread;

import java.util.ArrayDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 作者：叶应是叶
 * 时间：2018/6/24 15:01
 * 描述：https://github.com/leavesC/Java_Android_Learn
 *      https://www.jianshu.com/u/9df45b87cfdf
 */
public abstract class AsyncTask<Params, Progress, Result> {

    private static final String LOG_TAG = "AsyncTask";

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

    //串行任务执行器，即提交给线程池的任务是按照顺序，等上一个执行结束再执行下一个的
    public static final Executor SERIAL_EXECUTOR = new SerialExecutor();

    //用于当任务完成时传递执行结果
    private static final int MESSAGE_POST_RESULT = 0x1;

    //用于更新任务的进度值
    private static final int MESSAGE_POST_PROGRESS = 0x2;

    //当前 Task 使用的任务执行器
    private static volatile Executor sDefaultExecutor = SERIAL_EXECUTOR;

    //按照正常情况来说，在初始化 AsyncTask 时我们使用的都是其无参构造函数
    //因此 InternalHandler 绑定的 Looper 对象即是与主线程关联的 Looper 对象
    //所以 InternalHandler 可以用来在 UI 线程回调某些抽象方法，例如 onProgressUpdate() 方法
    private static InternalHandler sHandler;

    //等于 sHandler
    private final Handler mHandler;

    private final WorkerRunnable<Params, Result> mWorker;

    private final FutureTask<Result> mFuture;

    //当前 Task 的状态
    private volatile Status mStatus = Status.PENDING;

    //用于标记当前后台任务是否已被取消
    private final AtomicBoolean mCancelled = new AtomicBoolean();

    //用于标记当前后台任务是否已被执行
    private final AtomicBoolean mTaskInvoked = new AtomicBoolean();

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

    private static abstract class WorkerRunnable<Params, Result> implements Callable<Result> {
        Params[] mParams;
    }

    @SuppressWarnings({"RawUseOfParameterizedType"})
    private static class AsyncTaskResult<Data> {

        final AsyncTask mTask;

        final Data[] mData;

        AsyncTaskResult(AsyncTask task, Data... data) {
            mTask = task;
            mData = data;
        }
    }

    //用于标记 Task 的当前状态
    public enum Status {
        //Task 还未运行
        PENDING,

        //Task 正在运行
        RUNNING,

        //Task 已经结束
        FINISHED,
    }

    //获取与主线程关联的 Looper 对象，以此为参数构建一个 Handler 对象
    //所以在 Task 的运行过程中，能够通过此 Handler 在 UI 线程执行操作
    private static Handler getMainHandler() {
        synchronized (AsyncTask.class) {
            if (sHandler == null) {
                sHandler = new InternalHandler(Looper.getMainLooper());
            }
            return sHandler;
        }
    }

    private Handler getHandler() {
        return mHandler;
    }

    //隐藏函数
    public static void setDefaultExecutor(Executor exec) {
        sDefaultExecutor = exec;
    }

    //创建一个新的异步任务，必须在UI线程上调用此构造函数
    public AsyncTask() {
        this((Looper) null);
    }

    /**
     * 隐藏的构造函数
     * 创建一个新的异步任务，必须在UI线程上调用此构造函数
     *
     * @hide
     */
    public AsyncTask(@Nullable Handler handler) {
        this(handler != null ? handler.getLooper() : null);
    }

    /**
     * 隐藏的构造函数
     * 创建一个新的异步任务，必须在UI线程上调用此构造函数
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

    private void postResultIfNotInvoked(Result result) {
        final boolean wasTaskInvoked = mTaskInvoked.get();
        if (!wasTaskInvoked) {
            postResult(result);
        }
    }

    private Result postResult(Result result) {
        @SuppressWarnings("unchecked")
        Message message = getHandler().obtainMessage(MESSAGE_POST_RESULT, new AsyncTaskResult<Result>(this, result));
        message.sendToTarget();
        return result;
    }

    //获取当前 Task 的状态
    public final Status getStatus() {
        return mStatus;
    }

    //在子线程中被调用，用于执行后台任务
    @WorkerThread
    protected abstract Result doInBackground(Params... params);

    //在 UI 线程中被调用，在 doInBackground() 方法之前调用，用于在后台任务开始前做一些准备工作
    @MainThread
    protected void onPreExecute() {
    }

    //在 UI 线程中被调用，在 doInBackground() 方法之后调用，用于处理后台任务的执行结果
    //参数 result 是 doInBackground() 方法的返回值
    @SuppressWarnings({"UnusedDeclaration"})
    @MainThread
    protected void onPostExecute(Result result) {
    }

    //在 UI 线程中被调用，当调用了 publishProgress() 方法后被触发
    //用于更新任务进度值
    @SuppressWarnings({"UnusedDeclaration"})
    @MainThread
    protected void onProgressUpdate(Progress... values) {
    }

    //在 UI 线程中被调用
    //当调用了 cancel(boolean) 方法取消后台任务后会被调用
    //在 doInBackground() 方法结束时也会被调用
    //方法内部默认调用了 onCancelled() 方法
    @SuppressWarnings({"UnusedParameters"})
    @MainThread
    protected void onCancelled(Result result) {
        onCancelled();
    }

    //在 UI 线程中被调用，被 onCancelled(Result) 方法调用
    @MainThread
    protected void onCancelled() {
    }

    //如果 Task 在完成之前被取消了则返回 true 
    public final boolean isCancelled() {
        return mCancelled.get();
    }

    //取消任务
    public final boolean cancel(boolean mayInterruptIfRunning) {
        mCancelled.set(true);
        return mFuture.cancel(mayInterruptIfRunning);
    }

    /**
     * Waits if necessary for the computation to complete, and then
     * retrieves its result.
     *
     * @return The computed result.
     * @throws CancellationException If the computation was cancelled.
     * @throws ExecutionException    If the computation threw an exception.
     * @throws InterruptedException  If the current thread was interrupted
     *                               while waiting.
     */
    public final Result get() throws InterruptedException, ExecutionException {
        return mFuture.get();
    }

    /**
     * Waits if necessary for at most the given time for the computation
     * to complete, and then retrieves its result.
     *
     * @param timeout Time to wait before cancelling the operation.
     * @param unit    The time unit for the timeout.
     * @return The computed result.
     * @throws CancellationException If the computation was cancelled.
     * @throws ExecutionException    If the computation threw an exception.
     * @throws InterruptedException  If the current thread was interrupted
     *                               while waiting.
     * @throws TimeoutException      If the wait timed out.
     */
    public final Result get(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {
        return mFuture.get(timeout, unit);
    }

    //以默认的串行任务执行器 sDefaultExecutor 来执行后台任务
    @MainThread
    public final AsyncTask<Params, Progress, Result> execute(Params... params) {
        return executeOnExecutor(sDefaultExecutor, params);
    }

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

    //直接执行 Runnable 指向的任务，不会触发各个抽象方法
    @MainThread
    public static void execute(Runnable runnable) {
        sDefaultExecutor.execute(runnable);
    }

    //运行于工作线程，此方法用于更新任务的进度值
    //会触发 onProgressUpdate() 被执行
    @WorkerThread
    protected final void publishProgress(Progress... values) {
        if (!isCancelled()) {
            //将与进度值相关的参数 Progress 包装到 AsyncTaskResult 对象当中，并传递给 Handler 进行处理 
            getHandler().obtainMessage(MESSAGE_POST_PROGRESS, new AsyncTaskResult<Progress>(this, values)).sendToTarget();
        }
    }

    //处理任务结果
    private void finish(Result result) {
        if (isCancelled()) {
            onCancelled(result);
        } else {
            onPostExecute(result);
        }
        //将 Task 的状态值置为结束状态
        mStatus = Status.FINISHED;
    }

}