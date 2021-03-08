> 对于 Android Developer 来说，Google Jetpack 可以说是当前最为基础的架构组件之一了，自从推出以后极大地改变了我们的开发模式并降低了开发难度，这也要求我们对当中一些子组件的实现原理具有一定程度的了解，所以我就打算来写一系列关于 Jetpack 源码解析的文章，希望对你有所帮助 😁😁
>
> 公众号：**[字节数组](https://s3.ax1x.com/2021/02/18/yRiE4K.png)**

上篇文章详细讲述了 Lifecycle 的整个事件分发逻辑，本篇文章再来介绍下 Lifecycle 的几个开发者比较容易忽略的衍生产物

本文所讲的的源代码基于以下依赖库当前最新的 release 版本：

```groovy
	compileSdkVersion 29

    implementation "androidx.lifecycle:lifecycle-service:2.2.0"
    implementation "androidx.lifecycle:lifecycle-process:2.2.0"
```

### 一、LifecycleService

之前的文章有介绍过，LifecycleOwner 接口用于标记其实现类具备 Lifecycle 对象，即具备生命周期。而四大组件之一的 Service 本身从被**启动/绑定**再到被**停止**，具有着类似 **Activity/Fragment** 从前台到退出页面之间的一系列行为，所以 Jetpack 也提供了 `LifecycleService` 这么一个 Service 的子类，用于监听 Service 的生命周期活动

LifecycleService 的源码较为简单，仅仅是在各个生命周期函数中将当前的 Event 事件转发给 ServiceLifecycleDispatcher 进行处理，由其来进行具体的事件分发。当中，`onBind` 和 `onStart`所触发的均是 `Lifecycle.Event.ON_START` 事件，这是为了兼顾 `startService` 和 `bindService` 两种不同的情况

```java
public class LifecycleService extends Service implements LifecycleOwner {

    private final ServiceLifecycleDispatcher mDispatcher = new ServiceLifecycleDispatcher(this);

    @CallSuper
    @Override
    public void onCreate() {
        mDispatcher.onServicePreSuperOnCreate();
        super.onCreate();
    }

    @CallSuper
    @Nullable
    @Override
    public IBinder onBind(@NonNull Intent intent) {
        mDispatcher.onServicePreSuperOnBind();
        return null;
    }

    @SuppressWarnings("deprecation")
    @CallSuper
    @Override
    public void onStart(@Nullable Intent intent, int startId) {
        mDispatcher.onServicePreSuperOnStart();
        super.onStart(intent, startId);
    }

    // this method is added only to annotate it with @CallSuper.
    // In usual service super.onStartCommand is no-op, but in LifecycleService
    // it results in mDispatcher.onServicePreSuperOnStart() call, because
    // super.onStartCommand calls onStart().
    @CallSuper
    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @CallSuper
    @Override
    public void onDestroy() {
        mDispatcher.onServicePreSuperOnDestroy();
        super.onDestroy();
    }

    @Override
    @NonNull
    public Lifecycle getLifecycle() {
        return mDispatcher.getLifecycle();
    }
}
```

ServiceLifecycleDispatcher 的逻辑也较为简单，内部也使用到了 LifecycleRegistry 作为 LifecycleService 中 `getLifecycle()` 方法的返回值，这一点和 `androidx.appcompat.app.AppCompatActivity` 和 `androidx.fragment.app.Fragment` 保持一致

ServiceLifecycleDispatcher 将每一次的 Event 事件都包装为 DispatchRunnable 对象，然后转交由 mHandler 来执行。此外，为了保证 Lifecycle.Event 能被及时触发并保证有序性，`postDispatchRunnable()` 方法会主动调用之前的 mLastDispatchRunnable 对象（如果不为 null 的话）的 `run()` 方法。因为交由 Handler 执行的 Runnable 并不是可以保证就是实时完成的，为了保证 Event 值的有序性就会在有新 Event 到来时主动调用 `run()` 方法

> 而令我比较疑惑的一点就是，ServiceLifecycleDispatcher 的设计者为什么要将 Handler 作为默认的执行方式，而非通过直接调用 `mRegistry.handleLifecycleEvent(mEvent)`来完成事件的分发，使得如今 ServiceLifecycleDispatcher 内部的逻辑是两种调用方式都有可能被使用到，最后是通过什么方式来执行的具有不确定性

```java
/**
 * Helper class to dispatch lifecycle events for a service. Use it only if it is impossible
 * to use {@link LifecycleService}.
 */
@SuppressWarnings("WeakerAccess")
public class ServiceLifecycleDispatcher {
    private final LifecycleRegistry mRegistry;
    private final Handler mHandler;
    private DispatchRunnable mLastDispatchRunnable;

    /**
     * @param provider {@link LifecycleOwner} for a service, usually it is a service itself
     */
    public ServiceLifecycleDispatcher(@NonNull LifecycleOwner provider) {
        mRegistry = new LifecycleRegistry(provider);
        mHandler = new Handler();
    }

    private void postDispatchRunnable(Lifecycle.Event event) {
        //为了保证 Lifecycle.Event 能被及时触发并保证有序性
        //当有新的 Event 到来时，如果 mLastDispatchRunnable 不为 null
        //则主动执行 run 方法及时触发上一次的 Event，mLastDispatchRunnable 内部也做了避免重复调用的判断
        if (mLastDispatchRunnable != null) {
            mLastDispatchRunnable.run();
        }
        mLastDispatchRunnable = new DispatchRunnable(mRegistry, event);
        //将 mLastDispatchRunnable 插入到 mHandler 的队列头部
        mHandler.postAtFrontOfQueue(mLastDispatchRunnable);
    }

    /**
     * Must be a first call in {@link Service#onCreate()} method, even before super.onCreate call.
     */
    public void onServicePreSuperOnCreate() {
        postDispatchRunnable(Lifecycle.Event.ON_CREATE);
    }

    /**
     * Must be a first call in {@link Service#onBind(Intent)} method, even before super.onBind
     * call.
     */
    public void onServicePreSuperOnBind() {
        postDispatchRunnable(Lifecycle.Event.ON_START);
    }

    /**
     * Must be a first call in {@link Service#onStart(Intent, int)} or
     * {@link Service#onStartCommand(Intent, int, int)} methods, even before
     * a corresponding super call.
     */
    public void onServicePreSuperOnStart() {
        postDispatchRunnable(Lifecycle.Event.ON_START);
    }

    /**
     * Must be a first call in {@link Service#onDestroy()} method, even before super.OnDestroy
     * call.
     */
    public void onServicePreSuperOnDestroy() {
        postDispatchRunnable(Lifecycle.Event.ON_STOP);
        postDispatchRunnable(Lifecycle.Event.ON_DESTROY);
    }

    /**
     * @return {@link Lifecycle} for the given {@link LifecycleOwner}
     */
    @NonNull
    public Lifecycle getLifecycle() {
        return mRegistry;
    }

    static class DispatchRunnable implements Runnable {
        private final LifecycleRegistry mRegistry;
        final Lifecycle.Event mEvent;
        private boolean mWasExecuted = false;

        DispatchRunnable(@NonNull LifecycleRegistry registry, Lifecycle.Event event) {
            mRegistry = registry;
            mEvent = event;
        }
        
        @Override
        public void run() {
            //mWasExecuted 用于标记 run() 方法是否已经被执行过了
            //由于 Handler 和 postDispatchRunnable() 会先后调用 run() 方法
            //所以需要 mWasExecuted 来避免重复调用
            if (!mWasExecuted) {
                mRegistry.handleLifecycleEvent(mEvent);
                mWasExecuted = true;
            }
        }
    }
}
```

### 二、ProcessLifecycleOwner

ProcessLifecycleOwner 是 `androidx.lifecycle:lifecycle-process:xxx` 库下的一个 LifecycleOwner 实现类，可用于监听整个应用的前后台变化，在一些场景下（比如消息推送时的跳转、数据埋点）是比较有用的

以下就来介绍下 ProcessLifecycleOwner 的使用方法以及 `androidx.lifecycle:lifecycle-process:xxx` 库下的所有类

#### 1、如何使用

ProcessLifecycleOwner 是单例模式，获取到其唯一实例后向其添加 Observer 即可。需要注意的是，ProcessLifecycleOwner 是依靠于应用内所有 Activity 的生命周期的变化来定义生命周期事件的，所以对于那些完全无 UI 界面的应用来说使用 ProcessLifecycleOwner 是没有意义的

```java
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {

            override fun onCreate(owner: LifecycleOwner) {
                Log.e("TAG", "应用被启动")
            }

            override fun onResume(owner: LifecycleOwner) {
                Log.e("TAG", "应用进入前台")
            }

            override fun onStop(owner: LifecycleOwner) {
                Log.e("TAG", "应用进入后台")
            }

        })
```

#### 2、EmptyActivityLifecycleCallbacks

`EmptyActivityLifecycleCallbacks` 类实现了 `Application.ActivityLifecycleCallbacks` 接口，`ActivityLifecycleCallbacks` 接口提供了用于监听应用内所有 Activity 的生命周期回调的方法。例如，在 Activity 的 `onCreate()` 函数被**调用前（onActivityPreCreated）、被调用时（onActivityCreated）、被调用后（onActivityPostCreated）**，都提供了相应的监听回调

需要注意的是，`onActivityPreCreated` 和 `onActivityPostCreated` 这两类函数都是 SDK 为 29 时新增的默认函数，在 SDK 29 之前只包含 `onActivityCreated` 函数

```java
  public interface ActivityLifecycleCallbacks {

        /**
         * Called as the first step of the Activity being created. This is always called before
         * {@link Activity#onCreate}.
         */
        default void onActivityPreCreated(@NonNull Activity activity,
                @Nullable Bundle savedInstanceState) {
        }

        /**
         * Called when the Activity calls {@link Activity#onCreate super.onCreate()}.
         */
        void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState);

        /**
         * Called as the last step of the Activity being created. This is always called after
         * {@link Activity#onCreate}.
         */
        default void onActivityPostCreated(@NonNull Activity activity,
                @Nullable Bundle savedInstanceState) {
        }
		
    	//省略其它相似代码
    	···
    }

```

```java
class EmptyActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityResumed(Activity activity) {
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }
}
```

#### 3、LifecycleDispatcher

LifecycleDispatcher 的主要逻辑是用于向应用内所有 Activity 注入一个 ReportFragment，通过 ReportFragment 来辅助获取 Activity 的生命周期事件。并由外部通过调用 `init(Context)` 方法来进行初始化

使用 `registerActivityLifecycleCallbacks`来监听 Activity 的最大优势就是它的涉及范围可以囊括应用的所有自定义 Activity 和使用到的依赖库中的所有第三方 Activity，大多数情况下我们是无法也不会去直接修改第三方依赖库中的代码，通过系统提供的方法我们才可以比较简单地向其注入一些自定义逻辑

```java
class LifecycleDispatcher {

    private static AtomicBoolean sInitialized = new AtomicBoolean(false);

    static void init(Context context) {
        //避免重复初始化
        if (sInitialized.getAndSet(true)) {
            return;
        }
        ((Application) context.getApplicationContext())
                .registerActivityLifecycleCallbacks(new DispatcherActivityCallback());
    }

    @SuppressWarnings("WeakerAccess")
    @VisibleForTesting
    static class DispatcherActivityCallback extends EmptyActivityLifecycleCallbacks {

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            //当 Activity 被创建时，向其注入一个 ReportFragment（如果需要的话）
            ReportFragment.injectIfNeededIn(activity);
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }
    }

    private LifecycleDispatcher() {
    }
}
```

#### 4、ProcessLifecycleOwner

ProcessLifecycleOwner 实现了 LifecycleOwner 接口，也用到了 LifecycleRegistry 作为其生命周期实现。其构造函数是私有的，通过静态变量来实现单例模式

```java
public class ProcessLifecycleOwner implements LifecycleOwner {

    private final LifecycleRegistry mRegistry = new LifecycleRegistry(this);
    
	private static final ProcessLifecycleOwner sInstance = new ProcessLifecycleOwner();

    /**
     * The LifecycleOwner for the whole application process. Note that if your application
     * has multiple processes, this provider does not know about other processes.
     *
     * @return {@link LifecycleOwner} for the whole application.
     */
    @NonNull
    public static LifecycleOwner get() {
        return sInstance;
    }
    
    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return mRegistry;
    }

}
```

开放了 `init(Context)`函数来由外部进行变量初始化，该方法的主要逻辑是这样的：

- 通过外部传入的 context 对象获取到 Application 对象
- 向 Application 注册 registerActivityLifecycleCallbacks，通过 EmptyActivityLifecycleCallbacks 和 ReportFragment 来实现对所有系统版本下的 Activity 进行全局的生命周期事件监听

至此，就实现了对应用内所有 Activity 局的生命周期事件监听，再之后只要再来计算处于前台的 Activity 数量的变化，就可以判断出应用所处的状态了

```java
    private Handler mHandler;

    private final LifecycleRegistry mRegistry = new LifecycleRegistry(this);

	ActivityInitializationListener mInitializationListener =
            new ActivityInitializationListener() {
                @Override
                public void onCreate() {
                }

                @Override
                public void onStart() {
                    activityStarted();
                }

                @Override
                public void onResume() {
                    activityResumed();
                }
            };

	static void init(Context context) {
        sInstance.attach(context);
    }

	void attach(Context context) {
        mHandler = new Handler();
        //因为 ProcessLifecycleOwner 是针对于对整个应用的生命周期的监听
        //会执行到这一步的话说明应用肯定被启动了，此时就到了 Lifecycle.Event.ON_CREATE
        //且由于 attach 方法只会被调用一次，所以外部也只会收到一次 Lifecycle.Event.ON_CREATE 事件
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        Application app = (Application) context.getApplicationContext();
        app.registerActivityLifecycleCallbacks(new EmptyActivityLifecycleCallbacks() {
            
            //此方法是 SDK 29 时新增的，所以当 SDK 版本小于 29 时此方法是无效的
            @Override
            public void onActivityPreCreated(@NonNull Activity activity,
                                             @Nullable Bundle savedInstanceState) {
                // We need the ProcessLifecycleOwner to get ON_START and ON_RESUME precisely
                // before the first activity gets its LifecycleOwner started/resumed.
                // The activity's LifecycleOwner gets started/resumed via an activity registered
                // callback added in onCreate(). By adding our own activity registered callback in
                // onActivityPreCreated(), we get our callbacks first while still having the
                // right relative order compared to the Activity's onStart()/onResume() callbacks.
                
                //当 SDK 版本大于等于 29 时 activityStarted 和 activityResumed 这两个事件依靠于
                //EmptyActivityLifecycleCallbacks 的回调
                //当 SDK 版本小于 29 时，则需要依赖于 ReportFragment 的回调
                activity.registerActivityLifecycleCallbacks(new EmptyActivityLifecycleCallbacks() {
                    @Override
                    public void onActivityPostStarted(@NonNull Activity activity) {
                        activityStarted();
                    }

                    @Override
                    public void onActivityPostResumed(@NonNull Activity activity) {
                        activityResumed();
                    }
                });
            }

            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                // Only use ReportFragment pre API 29 - after that, we can use the
                // onActivityPostStarted and onActivityPostResumed callbacks registered in
                // onActivityPreCreated()
                if (Build.VERSION.SDK_INT < 29) {
                    //在 LifecycleDispatcher 中已经为每个 Activity 注入了 ReportFragment
                    //所以此处都可以成功获取到 ReportFragment 对象并设置回调事件
                    ReportFragment.get(activity).setProcessListener(mInitializationListener);
                }
            }

            @Override
            public void onActivityPaused(Activity activity) {
                activityPaused();
            }

            @Override
            public void onActivityStopped(Activity activity) {
                activityStopped();
            }
        });
    }
```

一般情况下，一个应用的 Activity 启动流程可以概括为以下几种：

1. 应用第一次打开时启动了 Activity A，此时会先后回调： A-onCreate、A-onStart、A-onResume
2. 用户按 Home 键退出 Activity A，此时会先后回调：A-onPause、A-onStop
3. 用户从后台启动了 Activity A，此时会先后回调：A-onStart、A-onResume
4. 用户旋转了屏幕导致  Activity A 被重建，此时会先后回调：A-onPause、A-onStop、A-onDestroy、A-onCreate、A-onStart、A-onResume
5. 用户从  Activity A 启动了  Activity B，此时会先后回调：A-onPause、B-onCreate、B-onStart、B-onResume、A-onStop
6. 用户按返回键从 Activity B 回到 Activity A，此时会先后回调：B-onPause、A-onStart、A-onResume、B-onStop、B-onDestroy
7. 用户按返回键退出  Activity A，此时会先后回调：A-onPause、A-onStop、A-onDestroy

可以看到，Activity 前后台切换的情况是比较复杂多样化的，且还存在 Activity 被重建的情况，所以 ProcessLifecycleOwner 就需要考虑各种情况并且将其映射为 Lifecycle.Event 的各个状态

ProcessLifecycleOwner 内部有几个变量作为状态标记位而存在

```java
	// ground truth counters
	//当有 Activity 走到 Started 状态时，则 mStartedCounter 加一
	//当有 Activity 走到 Stopped 状态时，则 mStartedCounter 减一
    private int mStartedCounter = 0;

	//当有 Activity 走到 Resumed 状态时，则 mResumedCounter 加一
	//当有 Activity 走到 Paused 状态时，则 mResumedCounter 减一
    private int mResumedCounter = 0;
	
	//当发布了 ON_RESUME 事件时，值变为 false
	//当发布了 ON_PAUSE 事件时，值变为 true
    private boolean mPauseSent = true;

	//当发布了 ON_START 事件时，值变为 false
	//当发布了 ON_STOP 事件时，值变为 true
    private boolean mStopSent = true;
```

当有 Activity 走到 onStart 状态时会调用 `activityStarted()`函数，而需要向外发布 ON_START 事件只在以下两种场景发生：

1. 应用从运行开始第一次启动了 Activity。此时 mStartedCounter 从 0 递增为 1，且 mStopSent 还保持着默认值 true
2. 应用从后台切换到了前台。此时需确保上一次发布的是 ON_STOP 事件，即 mStopSent 为 true 时，等式才能成立。因为存在这么一种特殊情况：应用只包含一个 Activity，且用户旋转了屏幕导致了该 Activity 被重建，此时 Activity 会重新走一遍生命周期流程，但对于开发者来说，Activity 还是处于前台，此时就不应该再次发布 ON_START 事件，所以 ProcessLifecycleOwner 内部对这种情况做了延时判断处理，只有上一次发布的是 ON_STOP 事件时，才会向外发布 ON_START 事件

```java
	void activityStarted() {
        mStartedCounter++;
        if (mStartedCounter == 1 && mStopSent) {
            mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
            mStopSent = false;
        }
    }
```

当有 Activity 走到 onResumed 状态时会调用 `activityResumed()`函数，而需要向外发布 ON_RESUME 事件只在以下两种场景发生：

1. 应用从运行开始第一次启动了 Activity。此时 mResumedCounter 从 0 递增为 1，且 mPauseSent 还保持着默认值 true
2. 当前处于前台的 Activity 数量为 1，且上一次发布的是 ON_PAUSE 事件时（即 mPauseSent 为 true），才会发布 ON_RESUME 事件。此时一样是为了兼容用户旋转了屏幕导致了 Activity 被重建的情况

```java
	//当有 Activity 走到 onResumed 时被调用
    void activityResumed() {
        mResumedCounter++;
        if (mResumedCounter == 1) {
            if (mPauseSent) {
                mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
                mPauseSent = false;
            } else {
                mHandler.removeCallbacks(mDelayedPauseRunnable);
            }
        }
    }
```

当有 Activity 走到 onPaused 状态时会调用 `activityPaused()`函数，而 `mResumedCounter == 0` 这个条件成立的可能原因有两种：

1. 应用从前台退到了后台。此时需要发布 ON_PAUSE 事件
2. 应用还保持在前台，但由于用户旋转了屏幕导致 Activity 处于重建中。所以为了避免由于第一种情况导致误判，此处会通过 Handler 来发送一个延迟消息，在 700 毫秒后（等待 Activity 重建完成）再来进行检查是否真的需要发布 ON_PAUSE 事件 

因此对于开发者来说，ON_PAUSE 事件是会有一定延时的

```java
    @VisibleForTesting
    static final long TIMEOUT_MS = 700; //mls

    private Runnable mDelayedPauseRunnable = new Runnable() {
        @Override
        public void run() {
            dispatchPauseIfNeeded();
            dispatchStopIfNeeded();
        }
    };

    void activityPaused() {
        mResumedCounter--;
        if (mResumedCounter == 0) {
            mHandler.postDelayed(mDelayedPauseRunnable, TIMEOUT_MS);
        }
    }

	//判断是否有需要向外传递 ON_PAUSE 事件
    void dispatchPauseIfNeeded() {
        if (mResumedCounter == 0) {
            //如果当前处于前台的 Activity 数量为 0，则向外传递 ON_PAUSE 事件
            mPauseSent = true;
            mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
        }
    }

	//判断是否有需要向外传递 ON_STOP 事件
    void dispatchStopIfNeeded() {
        if (mStartedCounter == 0 && mPauseSent) {
           	//如果当前处于 Started 状态的 Activity 数量还是为 0，则向外发送了 ON_PAUSE 事件
            //则向外传递 ON_STOP 事件
            mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
            mStopSent = true;
        }
    }
```

当有 Activity 走到 onStopped 状态时会调用 `activityStopped()`函数，而需要向外发布 ON_STOP 事件只在以下一种场景发生：

1. 应用从前台退到了后台。此时需要发布 ON_STOP 事件

```java
    void activityStopped() {
        mStartedCounter--;
        dispatchStopIfNeeded();
    }

	void dispatchStopIfNeeded() {
        if (mStartedCounter == 0 && mPauseSent) {
            mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
            mStopSent = true;
        }
    }
```

#### 5、ProcessLifecycleOwnerInitializer

上文还有个小细节，就是 `LifecycleDispatcher` 和 `ProcessLifecycleOwner` 两个类都需要外部传入 Context 对象以便进行初始化，但开发者在使用时其实是不需要手动初始化的，因为 Jetpack 已经将这个初始化过程都给隐藏在了 `ProcessLifecycleOwnerInitializer` 这个 ContentProvider 内部了，Application 在启动的过程中就会自动调用 ProcessLifecycleOwnerInitializer 的 `onCreate()`方法

```java
public class ProcessLifecycleOwnerInitializer extends ContentProvider {
    @Override
    public boolean onCreate() {
        LifecycleDispatcher.init(getContext());
        ProcessLifecycleOwner.init(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, String[] strings, String s, String[] strings1,
            String s1) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues contentValues) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, String s, String[] strings) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues contentValues, String s, String[] strings) {
        return 0;
    }
}
```

`androidx.lifecycle:lifecycle-process:xxx` 库中的 **AndroidManifest.xml** 文件也包含了对 ProcessLifecycleOwnerInitializer 的声明，在打包时会自动将声明语句合并到主项目工程中的 **AndroidManifest.xml** 文件中

```java
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="androidx.lifecycle.process" >

    <uses-sdk
        android:minSdkVersion="14"
        android:targetSdkVersion="28" />

    <application>
        <provider
            android:name="androidx.lifecycle.ProcessLifecycleOwnerInitializer"
            android:authorities="${applicationId}.lifecycle-process"
            android:exported="false"
            android:multiprocess="true" />
    </application>

</manifest>
```

#### 6、总结

- ProcessLifecycleOwner 是对整个应用的生命周期进行监听，但由于 ProcessLifecycleOwner 是依靠于应用内所有 Activity 的生命周期的变化来定义生命周期事件的，所以对于那些完全无 UI 界面的应用来说使用 ProcessLifecycleOwner 是没有意义的
- ON_CREATE 事件只会在 ProcessLifecycleOwner 初始化的时候被触发
- 当应用刚被打开，或者是从后台切换到前台时，会依次触发 ON_START、ON_RESUME 事件
- 当应用从前台退到后台时，会依次触发 ON_PAUSE、ON_STOP 事件
- 由于存在用户旋转了屏幕导致 Activity 被重建的情况，所以 ProcessLifecycleOwner 内部有延时判断的逻辑，因此 ON_PAUSE、ON_STOP 这两个事件会有 700 毫秒的延迟
- ON_DESTROY 事件永远不会被触发
- ProcessLifecycleOwner 的初始化过程由 ProcessLifecycleOwnerInitializer 隐式完成