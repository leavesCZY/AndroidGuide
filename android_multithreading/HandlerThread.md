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