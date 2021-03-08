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
 * 作者：leavesc
 * 时间：2018/6/22 13:39
 * 描述：https://github.com/leavesC
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