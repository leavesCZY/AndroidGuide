## **一、基础知识**

### **1.1、内存泄露、内存溢出：**

 - 内存泄露(Memory Leak)指一个无用对象持续占有内存或无用对象的内存得不到及时的释放，从而造成内存空间的浪费
  例如，当Activity的onDestroy()方法被调用以后，Activity 本身以及它涉及到的 View、Bitmap等都应该被回收。但是，如果有一个后台线程持有对这个Activity的引用，那么Activity占据的内存就不能被回收，严重时将导致OOM，最终Crash。
 - 内存溢出(Out Of Memory)指一个应用在申请内存时，没有足够的内存空间供其使用

相同点：都会导致应用运行出现问题、性能下降或崩溃。
不同点：

  1. 内存泄露是导致内存溢出的原因之一，内存泄露严重时将导致内存溢出
  2. 内存泄露是由于软件设计缺陷引起的，可以通过完善代码来避免；内存溢出可以通过调整配置来减少发生频率，但无法彻底避免

### **1.2、Java 的内存分配：**

  1. 静态存储区：在程序整个运行期间都存在，编译时就分配好空间，主要用于存放静态数据和常量
  2. 栈区：当一个方法被执行时会在栈区内存中创建方法体内部的局部变量，方法结束后自动释放内存
  堆区：通常存放 new 出来的对象，由 Java 垃圾回收器回收

### **1.3、四种引用类型：**

  1. 强引用(StrongReference)：Jvm宁可抛出 OOM （内存溢出）也不会让 GC（垃圾回收） 回收具有强引用的对象
  2. 软引用(SoftReference)：只有在内存空间不足时才会被回收的对象
  3. 弱引用(WeakReference)：在 GC 时，一旦发现了只具有弱引用的对象，不管当前内存空间足够与否，都会回收它的内存
  4. 虚引用(PhantomReference)：任何时候都可以被GC回收，当垃圾回收器准备回收一个对象时，如果发现它还有虚引用，就会在回收对象的内存之前，把这个虚引用加入到与之关联的引用队列中。程序可以通过判断引用队列中是否存在该对象的虚引用，来了解这个对象是否将要被回收。可以用来作为GC回收Object的标志。

内存泄漏就是指new出来的Object（强引用）无法被GC回收

### **1.4、非静态内部类和匿名类：**

非静态内部类和匿名类会隐式地持有一个外部类的引用

### **1.5、静态内部类：**

外部类不管有多少个实例，都是共享同一个静态内部类，因此静态内部类不会持有外部类的引用

## **二、内存泄漏情况分析**

### **2.1、资源未关闭**

在使用Cursor，InputStream/OutputStream，File的过程中往往都用到了缓冲，因此在不需要使用的时候就要及时关闭它们，以便及时回收内存。它们的缓冲不仅存在于 java虚拟机内，也存在于java虚拟机外，如果只是把引用设置为null而不关闭它们，往往会造成内存泄漏。
此外，对于需要注册的资源也要记得解除注册，例如：BroadcastReceiver。动画也要在界面不再对用户可见时停止。

### **2.2、Handler**

在如下代码中

```java
public class HandlerActivity extends AppCompatActivity {

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_handler);
    }
    
}
```

在声明Handler对象后，IDE会给开发者一个提示：

```java
This Handler class should be static or leaks might occur.
```
意思是：Handler需要声明为static类型的，否则可能产生内存泄漏

这里来进行具体原因分析：
应用在第一次启动时， 系统会在主线程创建Looper对象，Looper实现了一个简单的消息队列，用来循环处理Message。所有主要的应用层事件（例如Activity的生命周期方法回调、Button点击事件等）都会包含在Message里，系统会把Message添加到Looper中，然后Looper进行消息循环。主线程的Looper存在于整个应用的生命周期期间。
当主线程创建Handler对象时，会与Looepr对象绑定，被分发到消息队列的Message会持有对Handler的引用，以便系统在Looper处理到该Message时能调用Handle的handlerMessage(Message)方法。
在上述代码中，Handler不是静态内部类，所以会持有外部类（HandlerActivity）的一个引用。当Handler中有延迟的的任务或者等待执行的任务队列过长时，由于消息持有对Handler的引用，而Handler又持有对其外部类的潜在引用，这条引用关系会一直保持到消息得到处理为止，导致了HandlerActivity无法被垃圾回收器回收，从而导致了内存泄露。

比如，在如下代码中，在onCreate()方法中令handler每隔一秒就输出Log日记

```java
public class HandlerActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";

    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_handler);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "Hi");
                handler.postDelayed(this, 1000);
            }
        }, 6000);
    }

}
```
查看Handler的源码可以看到，postDelayed方法其实就是在发送一条延时的Message

```java
	public final boolean postDelayed(Runnable r, long delayMillis){
		return sendMessageDelayed(getPostMessage(r), delayMillis);
    }
```
首先要意识到，非静态类和匿名内部类都会持有外部类的隐式引用。当HandlerActivity生命周期结束后，延时发送的Message持有Handler的引用，而Handler持有外部类(HandlerActivity)的隐式引用。该引用会继续存在直到Message被处理完成，而此处并没有可以令Handler终止的条件语句，所以阻止了HandlerActivity的回收，最终导致内存泄漏。

此处使用 LeakCanary 来检测内存泄露情况（该工具下边会有介绍）
先启动HandlerActivity后退出，等个三四秒后，可以看到LeakCanary提示我们应用内存泄漏了

通过文字提示可以看到问题就出在Handler身上
![](http://upload-images.jianshu.io/upload_images/2552605-0460414eb8c7b7e6?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

解决办法就是在HandlerActivity退出后，移除Handler的所有回调和消息

```java
    @Override
    protected void onDestroy() {
        super.onDestroy();
       handler.removeCallbacksAndMessages(null);
    }
```

### **2.3、Thread**

当在开启一个子线程用于执行一个耗时操作后，此时如果改变配置（例如横竖屏切换）导致了Activity重新创建，一般来说旧Activity就将交给GC进行回收。但如果创建的线程被声明为非静态内部类或者匿名类，那么线程会保持有旧Activity的隐式引用。当线程的run()方法还没有执行结束时，线程是不会被销毁的，因此导致所引用的旧的Activity也不会被销毁，并且与该Activity相关的所有资源文件也不会被回收，因此造成严重的内存泄露。

因此总结来看， 线程产生内存泄露的主要原因有两点：

  1. 线程生命周期的不可控。Activity中的Thread和AsyncTask并不会因为Activity销毁而销毁，Thread会一直等到run()执行结束才会停止，AsyncTask的doInBackground()方法同理
  2. 非静态的内部类和匿名类会隐式地持有一个外部类的引用

例如如下代码，在onCreate()方法中启动一个线程，并用一个静态变量threadIndex标记当前创建的是第几个线程

```java
public class ThreadActivity extends AppCompatActivity {

    private final String TAG = "ThreadActivity";

    private static int threadIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thread);
        threadIndex++;
        new Thread(new Runnable() {
            @Override
            public void run() {
                int j = threadIndex;
                while (true) {
                    Log.e(TAG, "Hi--" + j);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

}
```
旋转几次屏幕，可以看到输出结果为：

```java
04-04 08:15:16.373 23731-23911/com.czy.leakdemo E/ThreadActivity: Hi--2
04-04 08:15:16.374 23731-26132/com.czy.leakdemo E/ThreadActivity: Hi--4
04-04 08:15:16.374 23731-23970/com.czy.leakdemo E/ThreadActivity: Hi--3
04-04 08:15:16.374 23731-23820/com.czy.leakdemo E/ThreadActivity: Hi--1
04-04 08:15:16.852 23731-26202/com.czy.leakdemo E/ThreadActivity: Hi--5
04-04 08:15:18.374 23731-23911/com.czy.leakdemo E/ThreadActivity: Hi--2
04-04 08:15:18.374 23731-26132/com.czy.leakdemo E/ThreadActivity: Hi--4
04-04 08:15:18.376 23731-23970/com.czy.leakdemo E/ThreadActivity: Hi--3
04-04 08:15:18.376 23731-23820/com.czy.leakdemo E/ThreadActivity: Hi--1
04-04 08:15:18.852 23731-26202/com.czy.leakdemo E/ThreadActivity: Hi--5
...
```
即使创建了新的Activity，旧的Activity中建立的线程依然还在执行，从而导致无法释放Activity占用的内存，从而造成严重的内存泄漏

LeakCanary的检测结果：
![](http://upload-images.jianshu.io/upload_images/2552605-6e83c7ea0fd32f9a?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

想要避免因为 Thread 造成内存泄漏，可以在 Activity 退出后主动停止 Thread
例如，可以为 Thread 设置一个布尔变量 threadSwitch 来控制线程的启动与停止

```java
public class ThreadActivity extends AppCompatActivity {

    private final String TAG = "ThreadActivity";

    private int threadIndex;

    private boolean threadSwitch = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thread);
        threadIndex++;
        new Thread(new Runnable() {
            @Override
            public void run() {
                int j = threadIndex;
                while (threadSwitch) {
                    Log.e(TAG, "Hi--" + j);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        threadSwitch = false;
    }

}
```
如果想保持Thread继续运行，可以按以下步骤来：

  1. 将线程改为静态内部类，切断Activity 对于Thread的强引用
  2. 在线程内部采用弱引用保存Context引用，切断Thread对于Activity 的强引用

```java
public class ThreadActivity extends AppCompatActivity {

    private static final String TAG = "ThreadActivity";

    private static int threadIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thread);
        threadIndex++;
        new MyThread(this).start();
    }

    private static class MyThread extends Thread {

        private WeakReference<ThreadActivity> activityWeakReference;

        MyThread(ThreadActivity threadActivity) {
            activityWeakReference = new WeakReference<>(threadActivity);
        }

        @Override
        public void run() {
            if (activityWeakReference == null) {
                return;
            }
            if (activityWeakReference.get() != null) {
                int i = threadIndex;
                while (true) {
                    Log.e(TAG, "Hi--" + i);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

}
```

### **2.4、Context**

在使用Toast的过程中，如果应用连续弹出多个Toast，那么就会造成Toast重叠显示的情况
因此，可以使用如下方法来保证当前应用任何时候只会显示一个Toast，且Toast的文本信息能够得到立即更新

```java
/**
 * 作者： leavesc
 * 时间： 2017/4/4 14:05
 * 描述：
 */
public class ToastUtils {

    private static Toast toast;

    public static void showToast(Context context, String info) {
        if (toast == null) {
            toast = Toast.makeText(context, info, Toast.LENGTH_SHORT);
        }
        toast.setText(info);
        toast.show();
    }

}
```
然后，在Activity中使用

```java
public class ToastActivity extends AppCompatActivity {

    private static int i = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toast);
    }

    public void showToast(View view) {
        ToastUtils.showToast(this, "显示Toast:" + (i++));
    }

}
```
先点击一次 Button 使 Toast 弹出后，退出 ToastActivity，此时 LeakCanary 又会提示说造成内存泄漏了
![](http://upload-images.jianshu.io/upload_images/2552605-8cf92759cdc2fe19?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

当中提及了 Toast.mContext，通过查看Toast类的源码可以看到，Toast类内部的mContext指向传入的Context。而ToastUtils中的toast变量是静态类型的，其生命周期是与整个应用一样长的，从而导致 ToastActivity 得不到释放。因此，对Context的引用不能超过它本身的生命周期。

```java
public Toast(Context context) {
        mContext = context;
        mTN = new TN();
        mTN.mY = context.getResources().getDimensionPixelSize(
                com.android.internal.R.dimen.toast_y_offset);
        mTN.mGravity = context.getResources().getInteger(
                com.android.internal.R.integer.config_toastDefaultGravity);
    }
```

解决办法是改为使用 ApplicationContext 即可，因为ApplicationContext会随着应用的存在而存在，而不依赖于Activity的生命周期

```java
/**
 * 作者： leavesc
 * 时间： 2017/4/4 14:05
 * 描述：
 */
public class ToastUtils {

    private static Toast toast;

    public static void showToast(Context context, String info) {
        if (toast == null) {
            toast = Toast.makeText(context.getApplicationContext(), info, Toast.LENGTH_SHORT);
        }
        toast.setText(info);
        toast.show();
    }

}
```

### **2.5、集合**

有时候我们需要把一些对象加入到集合容器（例如ArrayList）中，当不再需要当中某些对象时，如果不把该对象的引用从集合中清理掉，也会使得GC无法回收该对象。如果集合是static类型的话，那内存泄漏情况就会更为严重。
因此，当不再需要某对象时，需要主动将之从集合中移除

## **三、LeakCanary**

LeakCanary是Square公司开发的一个用于检测内存溢出问题的开源库，可以在 debug 包中轻松检测内存泄露
GitHub地址：[LeakCanary](https://github.com/square/leakcanary)

要引入LeakCanary库，只需要在项目的build.gradle文件添加

```java
    testCompile 'com.squareup.leakcanary:leakcanary-android-no-op:1.5'
    debugCompile 'com.squareup.leakcanary:leakcanary-android:1.5'
    releaseCompile 'com.squareup.leakcanary:leakcanary-android-no-op:1.5'
```
Gradle强大的可配置性，可以确保只在编译 debug 版本时才会检查内存泄露，而编译 release 等版本的时候则会自动跳过检查，避免影响性能

如果只是想监测Activity的内存泄漏，在自定义的Application中进行如下初始化即可

```java
/**
 * 作者： leavesc
 * 时间： 2017/4/4 12:41
 * 描述：
 */
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        LeakCanary.install(this);
    }

}
```
如果还想监测Fragmnet的内存泄漏情况，则在自定义的Application中进行如下初始化

```java
/**
 * 作者： leavesc
 * 时间： 2017/4/4 12:41
 * 描述：
 */
public class MyApplication extends Application {

    private RefWatcher refWatcher;

    @Override
    public void onCreate() {
        super.onCreate();
        refWatcher = LeakCanary.install(this);
    }

    public static RefWatcher getRefWatcher(Context context) {
        MyApplication application = (MyApplication) context.getApplicationContext();
        return application.refWatcher;
    }

}

```
然后在要监测的Fragment中的onDestroy()建立监听

```java
public class BaseFragment extends Fragment {
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        RefWatcher refWatcher = MyApplication.getRefWatcher();
        refWatcher.watch(this);
    }
	
}
```

当在测试debug版本的过程中出现内存泄露时，LeakCanary将会自动展示一个通知栏显示检测结果
![](http://upload-images.jianshu.io/upload_images/2552605-79f58a267695f59e?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
