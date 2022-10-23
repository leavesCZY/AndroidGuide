> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

> 对于 Android Developer 来说，很多开源库都是属于**开发必备**的知识点，从使用方式到实现原理再到源码解析，这些都需要我们有一定程度的了解和运用能力。所以我打算来写一系列关于开源库**源码解析**和**实战演练**的文章，初定的目标是 **EventBus、ARouter、LeakCanary、Retrofit、Glide、OkHttp、Coil** 等七个知名开源库，希望对你有所帮助 🤣🤣

Glide 的源码有点复杂，如果要细细展开来讲解，那么写个十篇文章也囊括不完 😂😂 所以我就想着换个思路来看源码：**以小点来划分，每个小点只包含 Glide 实现某个功能或目的时所涉及的流程，以此来简化理解难度，通过整合多个小的功能点来把控住 Glide 大的实现方向**

本文基于 Glide 的以下版本来进行讲解

```groovy
dependencies {
    implementation 'com.github.bumptech.glide:glide:4.11.0'
    kapt 'com.github.bumptech.glide:compiler:4.11.0'
}
```

# 一、概述

在开始看 Glide 源码前，需要先对 Glide 有一些基本的了解

Glide 的缓存机制分为 **内存缓存** 和 **磁盘缓存** 两级。默认情况下，Glide 会自动对加载的图片进行缓存，缓存途径就分为内存缓存和磁盘缓存两种，缓存逻辑均采用 LruCache 算法。在默认情况下，Glide 对于一张网络图片的取值路径按顺序如下所示：

1. 当启动一个加载图片的请求时，会先检查 ActiveResources 中是否有符合条件的图片，如果存在则直接取值，否则就执行下一步。ActiveResources 用于在内存中存储当前 **正在使用** 的图片资源（例如，某个 ImageView 正在展示这张图片），ActiveResources 通过弱引用来持有该图片的引用
2. 检查 MemoryCache 中是否有符合条件的图片，如果存在则直接取值，否则就执行下一步。MemoryCache 使用了 Lru 算法，用于在内存中缓存曾使用过但目前非使用状态的图片资源
3. 检查 DiskCache 中是否有符合条件的图片，如果存在则进行解码取值，否则就执行下一步。DiskCache 也使用了 Lru 算法，用于在本地磁盘中缓存曾经加载过的图片资源
4. 联网请求图片。当加载到图片后，会将图片缓存到磁盘和内存中，即保存到 DiskCache 、ActiveResources、MemoryCache 中，以便后续复用

所以说，Glide 的内存缓存分为了 ActiveResources 和 MemoryCache 两级

此外，Glide 最终会缓存到磁盘的图片类型可以分为两类，一类是原始图片，一类是将原始图片进行各种压缩裁剪变换等各种转换操作后得到的图片。Glide 的磁盘缓存策略（DiskCacheStrategy）就分为以下五种，用于决定如何对这两类图片进行磁盘保存

| 磁盘缓存策略                | 缓存策略说明                                                 |
| --------------------------- | ------------------------------------------------------------ |
| DiskCacheStrategy.NONE      | 不缓存任何内容                                               |
| DiskCacheStrategy.ALL       | 既缓存原始图片，也缓存转换过后的图片                         |
| DiskCacheStrategy.DATA      | 只缓存原始图片                                               |
| DiskCacheStrategy.RESOURCE  | 只缓存转换过后的图片                                         |
| DiskCacheStrategy.AUTOMATIC | 由 Glide 根据图片资源类型来自动选择使用哪一种缓存策略（默认选项） |

当中，比较特殊的缓存策略是 **DiskCacheStrategy.AUTOMATIC**，该策略会根据要加载的图片来源类型采用最佳的缓存策略。如果加载的是远程图片，仅会存储原始图片，不存储转换过后的图片，因为下载远程图片相比调整磁盘上已经存在的数据要昂贵得多。如果加载的是本地图片，则仅会存储转换过后的图片，因为即使需要再次生成另一个尺寸或类型的图片，取回原始图片也很容易

由于磁盘空间是有限的，所以 AUTOMATIC 是在衡量**所占磁盘空间大小**和**获取图片的成本**两者所做的一个居中选择

# 二、如何监听生命周期

通常，我们加载的图片最终是要显示在 ImageView 中的，而 ImageView 是会挂载在 Activity 或者 Fragment 等容器上的，当容器处于后台或者已经被 finish 时，此时加载图片的操作就应该被取消或者停止，否则也是在浪费宝贵的系统资源和网络资源，甚至可能发生内存泄露或者 NPE 问题。那么，显而易见的一个问题就是，Glide 是如何判断容器是否还处于活跃状态的呢？

**类似于 Jetpack 组件中的 Lifecycle 的实现思路，Glide 也是通过一个无 UI 界面的 Fragment 来间接获取容器的生命周期状态的。Lifecycle 的实现思路可以看我的这篇源码讲解文章：[从源码看 Jetpack（1） -Lifecycle源码解析](https://juejin.im/post/6847902220755992589)**

Glide 实现**生命周期监听**涉及到的类包含以下几个：

1. LifecycleListener
2. Lifecycle
3. ActivityFragmentLifecycle
4. ApplicationLifecycle
5. SupportRequestManagerFragment

首先，LifecycleListener 定义了三种事件通知回调，用于通知容器的活跃状态（是处于前台、后台、还是已经退出了）。Lifecycle 用于注册和移除 LifecycleListener 

```java
public interface LifecycleListener {
  void onStart();
  void onStop();
  void onDestroy();
}

public interface Lifecycle {
  void addListener(@NonNull LifecycleListener listener);
  void removeListener(@NonNull LifecycleListener listener);
}
```

对于一个容器实例，例如在一个 Activity 的整个生命周期中，Activity 可能会先后加载多张图片，相应的就需要先后启动多个加载图片的后台任务，当 Activity 的生命周期状态发生变化时，就需要通知到每个后台任务。这一整个通知过程就对应 ActivityFragmentLifecycle 这个类

ActivityFragmentLifecycle 用 **isStarted** 和 **isDestroyed** 两个布尔变量来标记 Activity 的当前活跃状态，并提供了保存并通知多个 LifecycleListener 的能力

```java
class ActivityFragmentLifecycle implements Lifecycle {
  private final Set<LifecycleListener> lifecycleListeners =
      Collections.newSetFromMap(new WeakHashMap<LifecycleListener, Boolean>());
  private boolean isStarted;
  private boolean isDestroyed;

  @Override
  public void addListener(@NonNull LifecycleListener listener) {
    lifecycleListeners.add(listener);

    if (isDestroyed) {
      listener.onDestroy();
    } else if (isStarted) {
      listener.onStart();
    } else {
      listener.onStop();
    }
  }

  @Override
  public void removeListener(@NonNull LifecycleListener listener) {
    lifecycleListeners.remove(listener);
  }

  void onStart() {
    isStarted = true;
    for (LifecycleListener lifecycleListener : Util.getSnapshot(lifecycleListeners)) {
      lifecycleListener.onStart();
    }
  }

  void onStop() {
    isStarted = false;
    for (LifecycleListener lifecycleListener : Util.getSnapshot(lifecycleListeners)) {
      lifecycleListener.onStop();
    }
  }

  void onDestroy() {
    isDestroyed = true;
    for (LifecycleListener lifecycleListener : Util.getSnapshot(lifecycleListeners)) {
      lifecycleListener.onDestroy();
    }
  }
}
```

ActivityFragmentLifecycle 用于 SupportRequestManagerFragment 这个 Fragment 中来使用（省略了部分代码）。可以看到，在 Fragment 的三个生命周期回调事件中，都会相应通知 ActivityFragmentLifecycle。那么，不管 ImageView 的载体是 Activity 还是 Fragment，我们都可以向其注入一个无 UI 界面的 SupportRequestManagerFragment，以此来监听载体在整个生命周期内活跃状态的变化

```java
public class SupportRequestManagerFragment extends Fragment {
    
  private static final String TAG = "SupportRMFragment";
    
  private final ActivityFragmentLifecycle lifecycle;

  public SupportRequestManagerFragment() {
    this(new ActivityFragmentLifecycle());
  }

  @VisibleForTesting
  @SuppressLint("ValidFragment")
  public SupportRequestManagerFragment(@NonNull ActivityFragmentLifecycle lifecycle) {
    this.lifecycle = lifecycle;
  }

  @NonNull
  ActivityFragmentLifecycle getGlideLifecycle() {
    return lifecycle;
  }

  @Override
  public void onStart() {
    super.onStart();
    lifecycle.onStart();
  }

  @Override
  public void onStop() {
    super.onStop();
    lifecycle.onStop();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    lifecycle.onDestroy();
    unregisterFragmentWithRoot();
  }

  @Override
  public String toString() {
    return super.toString() + "{parent=" + getParentFragmentUsingHint() + "}";
  }
    
}
```

在两种特殊情况下 Glide 无法进行生命周期监听，此时对应的 Lifecycle 实现类是 ApplicationLifecycle，默认且一直都处于 onStart 状态：

- 传给 Glide 的 Context 属于 Application 类型。Application 并不具备通常意义上的生命周期事件
- 在子线程中来加载图片。此时开发者可能是想直接拿到 Bitmap 对象

```java
class ApplicationLifecycle implements Lifecycle {
  @Override
  public void addListener(@NonNull LifecycleListener listener) {
    listener.onStart();
  }

  @Override
  public void removeListener(@NonNull LifecycleListener listener) {
    // Do nothing.
  }
}
```

# 三、怎么注入 Fragment

现在已经知道 Glide 是通过 SupportRequestManagerFragment 来拿到生命周期事件的，那么 SupportRequestManagerFragment 是如何挂载到 Activity 或者 Fragment 上的呢？

通过查找引用，可以定位到是在 RequestManagerRetriever 的 `getSupportRequestManagerFragment` 方法中完成 SupportRequestManagerFragment 的注入

```java
public class RequestManagerRetriever implements Handler.Callback {
    
  @NonNull
  private SupportRequestManagerFragment getSupportRequestManagerFragment(
      @NonNull final FragmentManager fm, @Nullable Fragment parentHint, boolean isParentVisible) {
    //通过 TAG 判断 FragmentManager 中是否已经包含了 SupportRequestManagerFragment
    SupportRequestManagerFragment current =
        (SupportRequestManagerFragment) fm.findFragmentByTag(FRAGMENT_TAG);
    if (current == null) {
      //current 为 null 说明还未注入过 SupportRequestManagerFragment
      //那么就构建一个 SupportRequestManagerFragment 实例并添加到 FragmentManager 中 
      current = pendingSupportRequestManagerFragments.get(fm);
      if (current == null) {
        current = new SupportRequestManagerFragment();
        current.setParentFragmentHint(parentHint);
        if (isParentVisible) {
          current.getGlideLifecycle().onStart();
        }
        pendingSupportRequestManagerFragments.put(fm, current);
        fm.beginTransaction().add(current, FRAGMENT_TAG).commitAllowingStateLoss();
        handler.obtainMessage(ID_REMOVE_SUPPORT_FRAGMENT_MANAGER, fm).sendToTarget();
      }
    }
    return current;
  }

}
```

那具体的注入时机是在什么时候呢？

我们使用 Glide 来加载一张图片往往是像以下所示那么的朴实无华，一行代码就搞定，Glide 在背后悄悄做了成吨的工作量

```java
Glide.with(FragmentActivity).load(url).into(ImageView)
```

当调用 `Glide.with(FragmentActivity)` 时，最终是会中转调用到 RequestManagerRetriever 的 `get(FragmentActivity)` 方法，在内部调用 `supportFragmentGet` 方法完成 SupportRequestManagerFragment 的注入，并最终返回一个 RequestManager 对象，RequestManager 中就存储了通过该 FragmentActivity 启动的所有图片加载任务 

```java
public class RequestManagerRetriever implements Handler.Callback {
    
  @NonNull
  public RequestManager get(@NonNull FragmentActivity activity) {
    if (Util.isOnBackgroundThread()) {
      //如果是后台线程的话，那么就使用 ApplicationLifecycle
      return get(activity.getApplicationContext());
    } else {
      assertNotDestroyed(activity);
      FragmentManager fm = activity.getSupportFragmentManager();
      return supportFragmentGet(activity, fm, /*parentHint=*/ null, isActivityVisible(activity));
    }
  }
    
  @NonNull
  private RequestManager supportFragmentGet(
      @NonNull Context context,
      @NonNull FragmentManager fm,
      @Nullable Fragment parentHint,
      boolean isParentVisible) {
    //在这里完成 SupportRequestManagerFragment 的注入操作
    SupportRequestManagerFragment current =
        getSupportRequestManagerFragment(fm, parentHint, isParentVisible);
    RequestManager requestManager = current.getRequestManager();
    if (requestManager == null) {
      // TODO(b/27524013): Factor out this Glide.get() call.
      Glide glide = Glide.get(context);
      requestManager =
          factory.build(
              glide, current.getGlideLifecycle(), current.getRequestManagerTreeNode(), context);
      current.setRequestManager(requestManager);
    }
    return requestManager;
  }
    
}
```

所以说，当我们调用 `Glide.with(FragmentActivity)` 方法时，此时就已经完成了 SupportRequestManagerFragment 的注入

而 RequestManagerRetriever 一共包含几种入参类型的 get 方法重载

1. Context
2. androidx.fragment.app.FragmentActivity
3. android.app.Activity
4. androidx.fragment.app.Fragment
5. android.app.Fragment（已废弃）
6. View

这几个 get 方法的逻辑可以总结为：

1. 如果外部是通过子线程来调用的，那么就统一使用 Application，此时就不需要注入 Fragment，直接使用 ApplicationLifecycle，不进行生命周期观察，默认外部会一直处于活跃状态
2. 如果外部传入的是 Application，那么步骤同上
3. 如果外部传入的 View 并没有关联到 Activity（例如，View 包含的 Context 属于 ServiceContext 类型），那么步骤同上
4. 除以上情况外，最终都会通过外部传入的参数查找到关联的 Activity 或者 Fragment，最终向其注入 RequestManagerFragment 或者 SupportRequestManagerFragment

> RequestManagerFragment 的功能和 SupportRequestManagerFragment 相同，但目前已经是废弃状态，此处就不再赘述

例如，`get(@NonNull Context context)` 就会根据调用者所在线程以及 Context 所属类型来判断如何注入 SupportRequestManagerFragment，从而得到不同的 RequestManager。如果最终没有注入 SupportRequestManagerFragment，那么使用的 RequestManager 对象就属于全局唯一的 Application 级别的 RequestManager 

```java
  /** The top application level RequestManager. */
  private volatile RequestManager applicationManager;

  @NonNull
  public RequestManager get(@NonNull Context context) {
    if (context == null) {
      throw new IllegalArgumentException("You cannot start a load on a null Context");
    } else if (Util.isOnMainThread() && !(context instanceof Application)) {
      //在主线程调用，且 context 并非 Application
        
      if (context instanceof FragmentActivity) {
        return get((FragmentActivity) context);
      } else if (context instanceof Activity) {
        return get((Activity) context);
      } else if (context instanceof ContextWrapper
          // Only unwrap a ContextWrapper if the baseContext has a non-null application context.
          // Context#createPackageContext may return a Context without an Application instance,
          // in which case a ContextWrapper may be used to attach one.
          && ((ContextWrapper) context).getBaseContext().getApplicationContext() != null) {
        return get(((ContextWrapper) context).getBaseContext());
      }
    }

    //在子线程调用或者 context 是 Application
    return getApplicationManager(context);
  }

  @NonNull
  private RequestManager getApplicationManager(@NonNull Context context) {
    // Either an application context or we're on a background thread.
    if (applicationManager == null) {
      synchronized (this) {
        if (applicationManager == null) {
          // Normally pause/resume is taken care of by the fragment we add to the fragment or
          // activity. However, in this case since the manager attached to the application will not
          // receive lifecycle events, we must force the manager to start resumed using
          // ApplicationLifecycle.

          // TODO(b/27524013): Factor out this Glide.get() call.
          Glide glide = Glide.get(context.getApplicationContext());
          applicationManager =
              factory.build(
                  glide,
                  new ApplicationLifecycle(),
                  new EmptyRequestManagerTreeNode(),
                  context.getApplicationContext());
        }
      }
    }

    return applicationManager;
  }
```

# 四、如何启动加载图片的任务

前文介绍了 Glide 是如何实现监听 Activity 的生命周期变化的，那么，Glide 是如何发起加载图片的任务的呢？

上面提到了，当我们调用了 `Glide.with(FragmentActivity)` 时，就会完成 SupportRequestManagerFragment 的注入操作。且对于同一个 Activity 实例，在其单次生命周期过程中只会注入一次。从 `supportFragmentGet` 方法也可以看到，每个 SupportRequestManagerFragment 都会包含一个 RequestManager 实例

```java
public class RequestManagerRetriever implements Handler.Callback {
    
  @NonNull
private RequestManager supportFragmentGet(
      @NonNull Context context,
      @NonNull FragmentManager fm,
      @Nullable Fragment parentHint,
      boolean isParentVisible) {
    //在这里完成 SupportRequestManagerFragment 的注入操作
    SupportRequestManagerFragment current =
        getSupportRequestManagerFragment(fm, parentHint, isParentVisible);
    RequestManager requestManager = current.getRequestManager();
    if (requestManager == null) {
      //如果 requestManager 为 null 就进行生成并设置到 SupportRequestManagerFragment 中
      // TODO(b/27524013): Factor out this Glide.get() call.
      Glide glide = Glide.get(context);
      requestManager =
          factory.build(
              glide, current.getGlideLifecycle(), current.getRequestManagerTreeNode(), context);
      current.setRequestManager(requestManager);
    }
    return requestManager;
  }
    
}
```

RequestManager 类就是用于启动并管理某个 Activity 前后启动的所有加载图片的任务的地方，当我们完整调用 `Glide.with(FragmentActivity).load(url).into(ImageView)` 的 `into` 方法后，就会构建出一个代表当前加载任务的 Request 对象，并且将该任务传递给 RequestManager，以此开始跟踪该任务

```java
  @NonNull
  public ViewTarget<ImageView, TranscodeType> into(@NonNull ImageView view) {
    ···
    return into(
        glideContext.buildImageViewTarget(view, transcodeClass),
        /*targetListener=*/ null,
        requestOptions,
        Executors.mainThreadExecutor());
  }

  private <Y extends Target<TranscodeType>> Y into(
      @NonNull Y target,
      @Nullable RequestListener<TranscodeType> targetListener,
      BaseRequestOptions<?> options,
      Executor callbackExecutor) {
    Preconditions.checkNotNull(target);
    if (!isModelSet) {
      throw new IllegalArgumentException("You must call #load() before calling #into()");
    }
    //构建一个代表加载任务的 Request 对象
    Request request = buildRequest(target, targetListener, options, callbackExecutor);
    ···
    requestManager.clear(target);
    target.setRequest(request);
    //将 request 传递给 requestManager，以此开始跟踪该任务
    requestManager.track(target, request);
    return target;
  }
```

重点还是 `requestManager.track(target, request)`这一句代码，这就是任务的发起点

```java
public class RequestManager implements ComponentCallbacks2, LifecycleListener, ModelTypes<RequestBuilder<Drawable>> {  

    //存储所有任务
	@GuardedBy("this")
  	private final RequestTracker requestTracker;

  	@GuardedBy("this")
  	private final TargetTracker targetTracker = new TargetTracker();  

  	synchronized void track(@NonNull Target<?> target, @NonNull Request request) {
    	targetTracker.track(target);
        //运行任务
      	requestTracker.runRequest(request);
  	}
    
}
```

当中，RequestTracker 就用于存储所有的 Request，即存储所有加载图片的任务，并提供了**开始、暂停和重启**所有任务的方法。外部通过改变 **isPaused** 变量值，用来控制当前是否允许启动任务，`runRequest` 方法中就会根据 isPaused 来判断当前是**马上启动任务**还是**将任务暂存到待处理列表 pendingRequests 中**

```java
public class RequestTracker {
  private static final String TAG = "RequestTracker";

  private final Set<Request> requests =
      Collections.newSetFromMap(new WeakHashMap<Request, Boolean>());

  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  private final List<Request> pendingRequests = new ArrayList<>();

  private boolean isPaused;

  /** Starts tracking the given request. */
  public void runRequest(@NonNull Request request) {
    //先将任务保存起来
    requests.add(request);
    //如果并非暂停状态，那么就开启任务，否则就将任务存入待处理列表
    if (!isPaused) {
      request.begin();
    } else {
      request.clear();
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(TAG, "Paused, delaying request");
      }
      pendingRequests.add(request);
    }
  }

  /** Stops any in progress requests. */
  public void pauseRequests() {
    isPaused = true;
    for (Request request : Util.getSnapshot(requests)) {
      if (request.isRunning()) {
        // Avoid clearing parts of requests that may have completed (thumbnails) to avoid blinking
        // in the UI, while still making sure that any in progress parts of requests are immediately
        // stopped.
        request.pause();
        pendingRequests.add(request);
      }
    }
  }

  /** Restarts failed requests and cancels and restarts in progress requests. */
  public void restartRequests() {
    for (Request request : Util.getSnapshot(requests)) {
      if (!request.isComplete() && !request.isCleared()) {
        request.clear();
        if (!isPaused) {
          request.begin();
        } else {
          // Ensure the request will be restarted in onResume.
          pendingRequests.add(request);
        }
      }
    }
  }

  ···
    
}
```

当 SupportRequestManagerFragment  走到 `onStop()` 状态时，就会中转调用到 RequestTracker，将 isPaused 置为 true。此外，当 SupportRequestManagerFragment 执行到 `onDestroy()` 时，就意味着 Activity 已经被 finish 了，此时就会回调通知到 RequestManager 的 `onDestroy()` 方法，在这里完成任务的清理以及解除各种注册事件

```java
@Override
public synchronized void onDestroy() {
    targetTracker.onDestroy();
    for (Target<?> target : targetTracker.getAll()) {
      clear(target);
    }
    targetTracker.clear();
    requestTracker.clearRequests();
    lifecycle.removeListener(this);
    lifecycle.removeListener(connectivityMonitor);
    mainHandler.removeCallbacks(addSelfToLifecycle);
    glide.unregisterRequestManager(this);
}
```

# 五、加载图片的具体流程

Request 是一个接口，代表的是每个图片加载请求，其包含有几个实现类，这里以 SingleRequest 为例。SingleRequest 的 `begin()` 方法会先对当前的任务状态进行校验，防止重复加载，然后去获取 **目标宽高** 或者 **ImageView 的宽高**，之后还会判断是否需要先展示占位符

```java
  public final class SingleRequest<R> implements Request, SizeReadyCallback, ResourceCallback {

  @Override
  public void begin() {
    synchronized (requestLock) {
      assertNotCallingCallbacks();
      stateVerifier.throwIfRecycled();
      startTime = LogTime.getLogTime();
      if (model == null) {
        if (Util.isValidDimensions(overrideWidth, overrideHeight)) {
          width = overrideWidth;
          height = overrideHeight;
        }
        // Only log at more verbose log levels if the user has set a fallback drawable, because
        // fallback Drawables indicate the user expects null models occasionally.
        int logLevel = getFallbackDrawable() == null ? Log.WARN : Log.DEBUG;
        //model 为 null，说明外部没有传入图片来源地址，直接走失败流程
        onLoadFailed(new GlideException("Received null model"), logLevel);
        return;
      }
	
      //防止任务正在运行时重复启动
      if (status == Status.RUNNING) {
        throw new IllegalArgumentException("Cannot restart a running request");
      }

      if (status == Status.COMPLETE) {
        //任务已经完成，直接返回已加载好的图片资源
        onResourceReady(resource, DataSource.MEMORY_CACHE);
        return;
      }

      // Restarts for requests that are neither complete nor running can be treated as new requests
      // and can run again from the beginning.
	
      //先获取目标宽高或者 ImageView 的宽高，按需加载
      status = Status.WAITING_FOR_SIZE;
      if (Util.isValidDimensions(overrideWidth, overrideHeight)) {
        onSizeReady(overrideWidth, overrideHeight);
      } else {
        target.getSize(this);
      }

      if ((status == Status.RUNNING || status == Status.WAITING_FOR_SIZE)
          && canNotifyStatusChanged()) {
        //先把占位符传出去
        target.onLoadStarted(getPlaceholderDrawable());
      }
      if (IS_VERBOSE_LOGGABLE) {
        logV("finished run method in " + LogTime.getElapsedMillis(startTime));
      }
    }
  }
    
}
```

可以看到，以上逻辑还没有涉及到具体的加载图片的逻辑，因为这个过程还需要在获取到目标宽高后才能进行。如果外部有传入具体的宽高值，那么就以外部值为准，否则就以 target（例如 ImageView）的宽高大小为准。只有在获取到宽高后才会真正开始加载，这都是为了实现按需加载，避免内存浪费

所以，重点还是要看 `onSizeReady` 方法。其内部会将当前的所有配置信息（图片地址，宽高、优先级、是否允许使用缓存等等）都转交给 Engine 的 `load` 方法，由其来完成图片的加载

```java
  private volatile Engine engine;  

  /** A callback method that should never be invoked directly. */
  @Override
  public void onSizeReady(int width, int height) {
    stateVerifier.throwIfRecycled();
    synchronized (requestLock) {
      if (IS_VERBOSE_LOGGABLE) {
        logV("Got onSizeReady in " + LogTime.getElapsedMillis(startTime));
      }
      if (status != Status.WAITING_FOR_SIZE) {
        return;
      }
      status = Status.RUNNING;
	
      //进行缩放处理
      float sizeMultiplier = requestOptions.getSizeMultiplier();
      this.width = maybeApplySizeMultiplier(width, sizeMultiplier);
      this.height = maybeApplySizeMultiplier(height, sizeMultiplier);

      if (IS_VERBOSE_LOGGABLE) {
        logV("finished setup for calling load in " + LogTime.getElapsedMillis(startTime));
      }
      //重点，正式开始加载图片
      loadStatus =
          engine.load(
              glideContext,
              model,
              requestOptions.getSignature(),
              this.width,
              this.height,
              requestOptions.getResourceClass(),
              transcodeClass,
              priority,
              requestOptions.getDiskCacheStrategy(),
              requestOptions.getTransformations(),
              requestOptions.isTransformationRequired(),
              requestOptions.isScaleOnlyOrNoTransform(),
              requestOptions.getOptions(),
              requestOptions.isMemoryCacheable(),
              requestOptions.getUseUnlimitedSourceGeneratorsPool(),
              requestOptions.getUseAnimationPool(),
              requestOptions.getOnlyRetrieveFromCache(),
              this,
              callbackExecutor);

      // This is a hack that's only useful for testing right now where loads complete synchronously
      // even though under any executor running on any thread but the main thread, the load would
      // have completed asynchronously.
      if (status != Status.RUNNING) {
        loadStatus = null;
      }
      if (IS_VERBOSE_LOGGABLE) {
        logV("finished onSizeReady in " + LogTime.getElapsedMillis(startTime));
      }
    }
  }
```

转交给 Engine 的配置信息同时还包含一个 ResourceCallback 对象，即 SingleRequest 本身，因为 SingleRequest 实现了 ResourceCallback 接口。从 ResourceCallback 包含的方法的名称来看，就可以知道当 Engine 在加载图片成功或者失败时，就会通过这两个方法将结果回调出来

```java
public interface ResourceCallback {

  void onResourceReady(Resource<?> resource, DataSource dataSource);

  void onLoadFailed(GlideException e);

  Object getLock();
}
```

`load` 方法会先为本次请求生成一个唯一标识 key，这个 key 就是判定是否可以实现图片复用的依据，然后根据这个 key 从内存缓存中取值，如果取得到的话就直接进行复用，否则就启动一个新任务来从磁盘加载或者联网加载，或者是为已存在的任务添加一个回调

```java
public <R> LoadStatus load(
      GlideContext glideContext,
      Object model,
      Key signature,
      int width,
      int height,
      Class<?> resourceClass,
      Class<R> transcodeClass,
      Priority priority,
      DiskCacheStrategy diskCacheStrategy,
      Map<Class<?>, Transformation<?>> transformations,
      boolean isTransformationRequired,
      boolean isScaleOnlyOrNoTransform,
      Options options,
      boolean isMemoryCacheable,
      boolean useUnlimitedSourceExecutorPool,
      boolean useAnimationPool,
      boolean onlyRetrieveFromCache,
      ResourceCallback cb,
      Executor callbackExecutor) {
    long startTime = VERBOSE_IS_LOGGABLE ? LogTime.getLogTime() : 0;
	
    //为本次请求生成一个唯一标识 key，这个 key 就是判定是否可以实现图片复用的依据
    EngineKey key =
        keyFactory.buildKey(
            model,
            signature,
            width,
            height,
            transformations,
            resourceClass,
            transcodeClass,
            options);

    EngineResource<?> memoryResource;
    synchronized (this) {
      //先从内存缓存中取值
      memoryResource = loadFromMemory(key, isMemoryCacheable, startTime);

      if (memoryResource == null) {
        //当前内存中不存在目标资源，那么就启动一个新任务来加载，或者是为已存在的任务添加一个回调
        return waitForExistingOrStartNewJob(
            glideContext,
            model,
            signature,
            width,
            height,
            resourceClass,
            transcodeClass,
            priority,
            diskCacheStrategy,
            transformations,
            isTransformationRequired,
            isScaleOnlyOrNoTransform,
            options,
            isMemoryCacheable,
            useUnlimitedSourceExecutorPool,
            useAnimationPool,
            onlyRetrieveFromCache,
            cb,
            callbackExecutor,
            key,
            startTime);
      }
    }

    // Avoid calling back while holding the engine lock, doing so makes it easier for callers to
    // deadlock.
    cb.onResourceReady(memoryResource, DataSource.MEMORY_CACHE);
    return null;
  }
```

对于一个加载网络图片的请求来说，`waitForExistingOrStartNewJob` 方法就对应着通过网络请求加载图片或者是加载本地磁盘文件的过程，如果目标图片还未下载过则去进行网络请求，如果之前已经缓存到了本地的话则去进行磁盘加载。`loadFromMemory` 方法则对应着尝试在内存中寻找目标图片的过程，因为目标图片可能之前已经加载到内存中了，此方法就用来尝试复用内存中的图片资源

这里就以加载一张网络图片为例，先后介绍**从网络请求到磁盘缓存，再到内存缓存**这整个过程

## 1、网络请求

`Glide.with(Context).load(Any)`的 `load` 方法是一个多重载形式的方法，支持 Integer、String、Uri、File 等多种入参类型，而且最终我们获取到的可能是 Bitmap、Drawable、GifDrawable 等多种结果。那么，Glide 是如何分辨我们不同的入参请求的呢？以及如何对不同的请求类型进行处理呢？

Glide 类中包含一个 `registry` 变量，相当于一个注册器，存储了对于特定的入参类型，其对应的处理逻辑，以及该入参类型希望得到的结果值类型

```java
registry
    .append(Uri.class, InputStream.class, new UriLoader.StreamFactory(contentResolver))
    .append(
        Uri.class,
        ParcelFileDescriptor.class,
        new UriLoader.FileDescriptorFactory(contentResolver))
    .append(
        Uri.class,
        AssetFileDescriptor.class,
        new UriLoader.AssetFileDescriptorFactory(contentResolver))
    .append(Uri.class, InputStream.class, new UrlUriLoader.StreamFactory())
    .append(URL.class, InputStream.class, new UrlLoader.StreamFactory())
    .append(Uri.class, File.class, new MediaStoreFileLoader.Factory(context))
    .append(GlideUrl.class, InputStream.class, new HttpGlideUrlLoader.Factory())
    .append(byte[].class, ByteBuffer.class, new ByteArrayLoader.ByteBufferFactory())
    .append(byte[].class, InputStream.class, new ByteArrayLoader.StreamFactory())
    .append(Uri.class, Uri.class, UnitModelLoader.Factory.<Uri>getInstance())
    .append(Drawable.class, Drawable.class, UnitModelLoader.Factory.<Drawable>getInstance())
    .append(Drawable.class, Drawable.class, new UnitDrawableDecoder())
    /* Transcoders */
    .register(Bitmap.class, BitmapDrawable.class, new BitmapDrawableTranscoder(resources))
    .register(Bitmap.class, byte[].class, bitmapBytesTranscoder)
    .register(
        Drawable.class,
        byte[].class,
        new DrawableBytesTranscoder(
            bitmapPool, bitmapBytesTranscoder, gifDrawableBytesTranscoder))
    .register(GifDrawable.class, byte[].class, gifDrawableBytesTranscoder);
```

例如，我们最常见的一种请求方式就是通过图片的 Url 来从网络获取图片，这就对应着以下配置。GlideUrl 就对应着我们传入的 ImageUrl，InputStream 即希望根据该 Url 从网络获取到相应的资源输入流，HttpGlideUrlLoader 就用来实现将 ImageUrl 转换为 InputStream 的过程

```java
append(GlideUrl.class, InputStream.class, new HttpGlideUrlLoader.Factory())
```

HttpGlideUrlLoader 会将 ImageUrl 传给 HttpUrlFetcher，由其来进行具体的网络请求

```java
public class HttpGlideUrlLoader implements ModelLoader<GlideUrl, InputStream> {
 
    @Override
  	public LoadData<InputStream> buildLoadData(
      	@NonNull GlideUrl model, int width, int height, @NonNull Options options) {
    	// GlideUrls memoize parsed URLs so caching them saves a few object instantiations and time
    	// spent parsing urls.
    	GlideUrl url = model;
    	if (modelCache != null) {
      		url = modelCache.get(model, 0, 0);
      			if (url == null) {
        	modelCache.put(model, 0, 0, model);
        	url = model;
      	}
    	}
    	int timeout = options.get(TIMEOUT);
    	return new LoadData<>(url, new HttpUrlFetcher(url, timeout));
  	  }
    
}
```

HttpUrlFetcher 会在 `loadDataWithRedirects` 方法中通过 HttpURLConnection 来请求图片，最终通过 DataCallback 将得到的图片输入流 InputStream 对象透传出去。此外，`loadDataWithRedirects` 方法会通过循环调用自己的方式来处理重定向的情况，不允许重复重定向到同个 Url，且最多重定向五次，否则就会直接走失败流程

```java
public class HttpUrlFetcher implements DataFetcher<InputStream> {
 
    private static final int MAXIMUM_REDIRECTS = 5;
    
  @Override
  public void loadData(
      @NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
    long startTime = LogTime.getLogTime();
    try {
      InputStream result = loadDataWithRedirects(glideUrl.toURL(), 0, null, glideUrl.getHeaders());
      callback.onDataReady(result);
    } catch (IOException e) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Failed to load data for url", e);
      }
      callback.onLoadFailed(e);
    } finally {
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(TAG, "Finished http url fetcher fetch in " + LogTime.getElapsedMillis(startTime));
      }
    }
  }
    
  private InputStream loadDataWithRedirects(
      URL url, int redirects, URL lastUrl, Map<String, String> headers) throws IOException {
    if (redirects >= MAXIMUM_REDIRECTS) {
       //重定向总次数达到五次，走失败流程
      throw new HttpException("Too many (> " + MAXIMUM_REDIRECTS + ") redirects!");
    } else {
      // Comparing the URLs using .equals performs additional network I/O and is generally broken.
      // See http://michaelscharf.blogspot.com/2006/11/javaneturlequals-and-hashcode-make.html.
      try {
        if (lastUrl != null && url.toURI().equals(lastUrl.toURI())) {
          //循环重定向到同个 Url，走失败流程
          throw new HttpException("In re-direct loop");
        }
      } catch (URISyntaxException e) {
        // Do nothing, this is best effort.
      }
    }

    urlConnection = connectionFactory.build(url);
    ···
    stream = urlConnection.getInputStream();
    if (isCancelled) {
      return null;
    }
    final int statusCode = urlConnection.getResponseCode();
    if (isHttpOk(statusCode)) {
      return getStreamForSuccessfulRequest(urlConnection);
    } else if (isHttpRedirect(statusCode)) {
      String redirectUrlString = urlConnection.getHeaderField("Location");
      if (TextUtils.isEmpty(redirectUrlString)) {
        throw new HttpException("Received empty or null redirect url");
      }
      URL redirectUrl = new URL(url, redirectUrlString);
      // Closing the stream specifically is required to avoid leaking ResponseBodys in addition
      // to disconnecting the url connection below. See #2352.
      cleanup();
      return loadDataWithRedirects(redirectUrl, redirects + 1, url, headers);
    } else if (statusCode == INVALID_STATUS_CODE) {
      throw new HttpException(statusCode);
    } else {
      throw new HttpException(urlConnection.getResponseMessage(), statusCode);
    }
  }
    
}
```

## 2、磁盘缓存

再回过头看 Engine 类的 `waitForExistingOrStartNewJob` 方法。当判断到当前内存缓存中没有目标图片时，就会启动 EngineJob 和 DecodeJob 进行磁盘文件加载或者联网请求加载

```java
private <R> LoadStatus waitForExistingOrStartNewJob(
      GlideContext glideContext,
      Object model,
      Key signature,
      int width,
      int height,
      Class<?> resourceClass,
      Class<R> transcodeClass,
      Priority priority,
      DiskCacheStrategy diskCacheStrategy,
      Map<Class<?>, Transformation<?>> transformations,
      boolean isTransformationRequired,
      boolean isScaleOnlyOrNoTransform,
      Options options,
      boolean isMemoryCacheable,
      boolean useUnlimitedSourceExecutorPool,
      boolean useAnimationPool,
      boolean onlyRetrieveFromCache,
      ResourceCallback cb,
      Executor callbackExecutor,
      EngineKey key,
      long startTime) {

    EngineJob<?> current = jobs.get(key, onlyRetrieveFromCache);
    if (current != null) {
      //如果已经启动了同个请求任务，那么就向其添加一个回调即可
      current.addCallback(cb, callbackExecutor);
      if (VERBOSE_IS_LOGGABLE) {
        logWithTimeAndKey("Added to existing load", startTime, key);
      }
      return new LoadStatus(cb, current);
    }

    EngineJob<R> engineJob =
        engineJobFactory.build(
            key,
            isMemoryCacheable,
            useUnlimitedSourceExecutorPool,
            useAnimationPool,
            onlyRetrieveFromCache);

    DecodeJob<R> decodeJob =
        decodeJobFactory.build(
            glideContext,
            model,
            key,
            signature,
            width,
            height,
            resourceClass,
            transcodeClass,
            priority,
            diskCacheStrategy,
            transformations,
            isTransformationRequired,
            isScaleOnlyOrNoTransform,
            onlyRetrieveFromCache,
            options,
            engineJob);

    jobs.put(key, engineJob);

    engineJob.addCallback(cb, callbackExecutor);
    //启动 decodeJob
    engineJob.start(decodeJob);

    if (VERBOSE_IS_LOGGABLE) {
      logWithTimeAndKey("Started new load", startTime, key);
    }
    return new LoadStatus(cb, engineJob);
  }
```

这里主要看 DecodeJob 类。前文有讲到，Glide 最终缓存到磁盘中的图片类型可以分为两类，一类是原始图片，一类是将原始图片进行各种压缩裁剪变换等各种转换操作后得到的图片，该行为就通过 `diskCacheStrategy` 参数来决定

```kotlin
Glide.with(context).load(imageUrl)
    .diskCacheStrategy(DiskCacheStrategy.DATA)
    .into(imageView)
```

如果我们使用的是 `DiskCacheStrategy.DATA`，那么就会缓存原图，在进行加载的时候也会去尝试加载本地缓存的原图，该属性即会影响写操作也会影响读操作。DecodeJob 会根据我们的缓存配置来选择相应的 DataFetcherGenerator 来进行处理，所以最终图片的来源类型就有三种可能：

1. 复用转换过的图片资源。对应 ResourceCacheGenerator，当缓存未命中时就执行下一步
2. 复用原始的图片资源。对应 DataCacheGenerator，当缓存未命中时就执行下一步
3. 本地没有符合条件的已缓存资源，需要全新加载（联网请求）。对应 SourceGenerator

```java
private DataFetcherGenerator getNextGenerator() {
    switch (stage) {
      case RESOURCE_CACHE:
        return new ResourceCacheGenerator(decodeHelper, this);
      case DATA_CACHE:
        return new DataCacheGenerator(decodeHelper, this);
      case SOURCE:
        return new SourceGenerator(decodeHelper, this);
      case FINISHED:
        return null;
      default:
        throw new IllegalStateException("Unrecognized stage: " + stage);
    }
}
```

例如，DataCacheGenerator 的主要逻辑就是 `startNext()` 方法，该方法会从 DiskCache 中取出原图，拿到缓存文件 cacheFile 以及相应的处理器 modelLoaders，modelLoaders 就包含了所有可以实现本次转换操作（例如，File 转 Drawable、File 转 Bitmap 等）的实现器，如果最终判定到存在缓存文件及相应的转换器，那么方法就会返回 true。当 DataCacheGenerator 加载目标数据成功后，就会回调 DecodeJob 的 `onDataFetcherReady` 方法，最终将目标数据存到 ActiveResources 中并通知所有 Target

```java
@Override
public boolean startNext() {
    while (modelLoaders == null || !hasNextModelLoader()) {
      sourceIdIndex++;
      if (sourceIdIndex >= cacheKeys.size()) {
        return false;
      }

      Key sourceId = cacheKeys.get(sourceIdIndex);
      // PMD.AvoidInstantiatingObjectsInLoops The loop iterates a limited number of times
      // and the actions it performs are much more expensive than a single allocation.
      @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
      Key originalKey = new DataCacheKey(sourceId, helper.getSignature());
      //从磁盘缓存中取值
      cacheFile = helper.getDiskCache().get(originalKey);
      if (cacheFile != null) {
        this.sourceKey = sourceId;
        //拿到所有的数据类型转换器
        modelLoaders = helper.getModelLoaders(cacheFile);
        modelLoaderIndex = 0;
      }
    }

    loadData = null;
    boolean started = false;
    while (!started && hasNextModelLoader()) {
      ModelLoader<File, ?> modelLoader = modelLoaders.get(modelLoaderIndex++);
      loadData =
          modelLoader.buildLoadData(
              cacheFile, helper.getWidth(), helper.getHeight(), helper.getOptions());
      if (loadData != null && helper.hasLoadPath(loadData.fetcher.getDataClass())) {
        started = true;
        loadData.fetcher.loadData(helper.getPriority(), this);
      }
    }
    return started;
  }
```

DataCacheGenerator 代表的是从本地磁盘缓存中取到目标图片的情况，而请求网络图片并将该图片写入到本地磁盘的逻辑还要看 SourceGenerator

SourceGenerator 在通过 HttpUrlFetcher 成功加载到图片后就会调用到 `onDataReadyInternal` 方法。如果本次请求不允许进行磁盘缓存，就会直接回调 DecodeJob 的 `onDataFetcherReady` 方法完成整个流程，这个过程就和 DataCacheGenerator 一致。而如果允许进行磁盘缓存，那么就会调用到 `reschedule()` 方法重新触发 `startNext()` 方法，在 `cacheData` 方法中完成磁盘文件的写入，在写入成功后就会构造一个 DataCacheGenerator，由 DataCacheGenerator 再来从磁盘中取值

```java
  void onDataReadyInternal(LoadData<?> loadData, Object data) {
    DiskCacheStrategy diskCacheStrategy = helper.getDiskCacheStrategy();
    if (data != null && diskCacheStrategy.isDataCacheable(loadData.fetcher.getDataSource())) {
      //允许进行磁盘缓存，先将 data 缓存到 dataToCache 变量 
      dataToCache = data;
      // We might be being called back on someone else's thread. Before doing anything, we should
      // reschedule to get back onto Glide's thread.
      cb.reschedule();
    } else {
      cb.onDataFetcherReady(
          loadData.sourceKey,
          data,
          loadData.fetcher,
          loadData.fetcher.getDataSource(),
          originalKey);
    }
  }

  @Override
  public boolean startNext() {
    if (dataToCache != null) {
      //dataToCache 不为 null，说明现在是要来将图片缓存到磁盘
      Object data = dataToCache;
      dataToCache = null;
      cacheData(data);
    }

    if (sourceCacheGenerator != null && sourceCacheGenerator.startNext()) {
      return true;
    }
    ···
    return started;
  }

  private void cacheData(Object dataToCache) {
    long startTime = LogTime.getLogTime();
    try {
      Encoder<Object> encoder = helper.getSourceEncoder(dataToCache);
      DataCacheWriter<Object> writer =
          new DataCacheWriter<>(encoder, dataToCache, helper.getOptions());
      originalKey = new DataCacheKey(loadData.sourceKey, helper.getSignature());
      //写入磁盘缓存
      helper.getDiskCache().put(originalKey, writer);
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(
            TAG,
            "Finished encoding source to cache"
                + ", key: "
                + originalKey
                + ", data: "
                + dataToCache
                + ", encoder: "
                + encoder
                + ", duration: "
                + LogTime.getElapsedMillis(startTime));
      }
    } finally {
      loadData.fetcher.cleanup();
    }

    sourceCacheGenerator =
        new DataCacheGenerator(Collections.singletonList(loadData.sourceKey), helper, this);
  }
```

> Glide 的磁盘缓存算法具体对应的是 DiskLruCache 类，这是 Glide 根据 JakeWharton 的 [DiskLruCache](https://github.com/JakeWharton/DiskLruCache) 开源库修改而来的，这里不过多赘述

不管 DecodeJob 是通过什么方式拿到图片，最终都会调用到 Engine 类的 `onEngineJobComplete` 方法，该方法就会将加载的图片缓存到内存中，这也是实现内存缓存的数据来源

```java
  @Override
  public synchronized void onEngineJobComplete(
      EngineJob<?> engineJob, Key key, EngineResource<?> resource) {
    // A null resource indicates that the load failed, usually due to an exception.
    if (resource != null && resource.isMemoryCacheable()) {
      activeResources.activate(key, resource);
    }
    jobs.removeIfCurrent(key, engineJob);
  }
```

## 3、内存缓存

再来看下内存缓存机制。前文说了，Glide 的内存缓存分为 ActiveResources 和 MemoryCache 两级，取内存缓存的操作就对应 Engine 类的 `loadFromMemory` 方法

- 根据 key 从 ActiveResources 中取值，如果取得到的话则调用 `acquire()` 方法将该资源的引用数加一，否则执行下一步
- 根据 key 从 MemoryCache 取值，如果取得到的话则调用 `acquire()` 方法将该资源的引用数加一，并同时将该资源从 MemoryCache 中移除并存入 ActiveResources 中，取不到值的话则返回 null

```java
  private final ActiveResources activeResources;

  private final MemoryCache cache;

  //尝试从内存中加载图片资源
  @Nullable
  private EngineResource<?> loadFromMemory(
      EngineKey key, boolean isMemoryCacheable, long startTime) {
    if (!isMemoryCacheable) { //如果配置了不允许使用内存缓存则直接返回
      return null;
    }
	
    //从 ActiveResources 加载
    EngineResource<?> active = loadFromActiveResources(key);
    if (active != null) {
      if (VERBOSE_IS_LOGGABLE) {
        logWithTimeAndKey("Loaded resource from active resources", startTime, key);
      }
      return active;
    }

    //从 MemoryCache 加载
    EngineResource<?> cached = loadFromCache(key);
    if (cached != null) {
      if (VERBOSE_IS_LOGGABLE) {
        logWithTimeAndKey("Loaded resource from cache", startTime, key);
      }
      return cached;
    }

    return null;
  }

  @Nullable
  private EngineResource<?> loadFromActiveResources(Key key) {
    EngineResource<?> active = activeResources.get(key);
    if (active != null) {
      active.acquire();
    }

    return active;
  }

  private EngineResource<?> loadFromCache(Key key) {
    EngineResource<?> cached = getEngineResourceFromCache(key);
    if (cached != null) {
      cached.acquire();
      activeResources.activate(key, cached);
    }
    return cached;
  }

  private EngineResource<?> getEngineResourceFromCache(Key key) {
    Resource<?> cached = cache.remove(key);

    final EngineResource<?> result;
    if (cached == null) {
      result = null;
    } else if (cached instanceof EngineResource) {
      // Save an object allocation if we've cached an EngineResource (the typical case).
      result = (EngineResource<?>) cached;
    } else {
      result =
          new EngineResource<>(
              cached, /*isMemoryCacheable=*/ true, /*isRecyclable=*/ true, key, /*listener=*/ this);
    }
    return result;
  }
```

ActiveResources 是通过弱引用的方式来保存当前所有正在被使用的图片资源。我们知道，如果一个对象只具有弱引用而不再被强引用，那么当发生 GC 时，弱引用中持有的引用就会被直接置空，同时弱引用对象本身就会被存入关联的 ReferenceQueue 中

当有一张新图片加载成功且被使用了，且当前允许内存缓存，那么该图片资源就会通过 `activate`方法保存到 `activeEngineResources` 中。当一张图片资源的引用计数 `acquired` 变为 0 时，说明该资源当前已经不再被外部使用了，此时就会通过 `deactivate` 方法将其从 `activeEngineResources` 中移除，消除对资源的引用，如果当前允许内存缓存的话则还会将该资源存入到 MemoryCache 中

```java
final class ActiveResources {
 
  final Map<Key, ResourceWeakReference> activeEngineResources = new HashMap<>();
 
  private final ReferenceQueue<EngineResource<?>> resourceReferenceQueue = new ReferenceQueue<>();
  
  synchronized void activate(Key key, EngineResource<?> resource) {
    ResourceWeakReference toPut =
        new ResourceWeakReference(
            key, resource, resourceReferenceQueue, isActiveResourceRetentionAllowed);

    ResourceWeakReference removed = activeEngineResources.put(key, toPut);
    if (removed != null) {
      removed.reset();
    }
  }

  synchronized void deactivate(Key key) {
    ResourceWeakReference removed = activeEngineResources.remove(key);
    if (removed != null) {
      removed.reset();
    }
  }
    
  @Synthetic
  void cleanupActiveReference(@NonNull ResourceWeakReference ref) {
    synchronized (this) {
      activeEngineResources.remove(ref.key);

      if (!ref.isCacheable || ref.resource == null) {
        return;
      }
    }

    EngineResource<?> newResource =
        new EngineResource<>(
            ref.resource, /*isMemoryCacheable=*/ true, /*isRecyclable=*/ false, ref.key, listener);
    listener.onResourceReleased(ref.key, newResource);
  }
    
}

 //对应 Engine 类
 @Override
 public void onResourceReleased(Key cacheKey, EngineResource<?> resource) {
    //从 activeResources 中移除该图片资源
    activeResources.deactivate(cacheKey);
    if (resource.isMemoryCacheable()) {
      //如果允许内存缓存的话则再将图片资源存到 MemoryCache 中
      cache.put(cacheKey, resource);
    } else {
      resourceRecycler.recycle(resource, /*forceNextFrame=*/ false);
    }
  }
```

MemoryCache 的默认实现则对应着 LruResourceCache 类。从名字也可以看出来，MemoryCache 使用的是 Lru 算法，其会根据外部传入的最大内存缓存大小来进行图片缓存，本身逻辑比较简单，不过多赘述

LruResourceCache 主要是包含了一个 ResourceRemovedListener 对象，用于当从内存缓存中移除了某个图片对象时回调通知 Engine，由 Engine 来回收该图片资源

```java
public class LruResourceCache extends LruCache<Key, Resource<?>> implements MemoryCache {
    
  @Override
  public void setResourceRemovedListener(@NonNull ResourceRemovedListener listener) {
    this.listener = listener;
  }

  @Override
  protected void onItemEvicted(@NonNull Key key, @Nullable Resource<?> item) {
    if (listener != null && item != null) {
      listener.onResourceRemoved(item);
    }
  }
    
}
```

好了，那就再来总结下 ActiveResources 和 MemoryCache 的逻辑和关系

1. ActiveResources 通过弱引用来保存当前处于使用状态的图片资源，当一张图片被加载成功且还处于使用状态时 ActiveResources 就会一直持有着对其的引用，当图片不再被使用时就会从 ActiveResources 中移除并存入到 MemoryCache 中
2. MemoryCache 使用了 Lrc 算法在内存中缓存图片资源，仅用于缓存当前并非处于使用状态的图片资源。当缓存在 MemoryCache 中的图片被外部复用时，该图片就会从 MemoryCache 中移除并再次存入 ActiveResources 中
3. MemoryCache 中保存的图片是当前处于强引用状态的资源，正常来说即使系统当前可用内存不足，系统即使抛出 OOM 也不会回收强引用，所以 Glide 的内存缓存先从 ActiveResources 取值就不会增大当前的已用内存。而系统内存大小是有限的，MemoryCache 使用 Lrc 算法就是为了尽量节省内存且尽量让最大概率还会被重用的图片可以被保留下来
4. Glide 将内存缓存分为 ActiveResources 和 MemoryCache 两级，而不是全都放到 MemoryCache 中，就避免了误将当前正处于活跃状态的图片资源给移除队列。且 ActiveResources 内部也一直在循环判断保存的图片资源是否已经不再被外部使用了，从而可以及时更新 MemoryCache，提高了 MemoryCache 的利用率和准确度

# 六、内存清理机制

Glide 的内存缓存机制是为了尽量复用图片资源，避免频繁地进行磁盘读写和内存读写，memoryCache、bitmapPool 和 arrayPool 的存在都是为了这个目的，但另一方面内存缓存也造成了有一部分内存空间一直被占用着，可能会造成系统的可用内存空间不足。当我们的应用退到后台时，如果之后系统的可用内存空间不足，那么系统就会按照优先级高低来清理掉一些后台进程，以便为前台进程腾出内存空间，为了提高应用在后台时的优先级，我们就需要主动降低我们的内存占用

所幸的是 Glide 也考虑到了这种情况，提供了缓存内存的自动清理机制。Glide 类的 `initializeGlide` 方法就默认向 Application 注册了一个 ComponentCallbacks，用于接收系统下发的内存状态变化的事件通知

```java
@GuardedBy("Glide.class")
@SuppressWarnings("deprecation")
private static void initializeGlide(
      @NonNull Context context,
      @NonNull GlideBuilder builder,
      @Nullable GeneratedAppGlideModule annotationGeneratedModule) {
    Context applicationContext = context.getApplicationContext();
    ···
    applicationContext.registerComponentCallbacks(glide);
    Glide.glide = glide;
}
```

对应的 ComponentCallbacks 实现类即 Glide 类本身，其相关的方法实现对应以下两个

```java
@Override
public void onTrimMemory(int level) {
	trimMemory(level);
}

@Override
public void onLowMemory() {
	clearMemory();
}
```

这两个方法会自动触发对 memoryCache、bitmapPool 和 arrayPool 的清理工作

```java
public void trimMemory(int level) {
    // Engine asserts this anyway when removing resources, fail faster and consistently
    Util.assertMainThread();
    // Request managers need to be trimmed before the caches and pools, in order for the latter to
    // have the most benefit.
    for (RequestManager manager : managers) {
      manager.onTrimMemory(level);
    }
    // memory cache needs to be trimmed before bitmap pool to trim re-pooled Bitmaps too. See #687.
    memoryCache.trimMemory(level);
    bitmapPool.trimMemory(level);
    arrayPool.trimMemory(level);
}

public void clearMemory() {
    // Engine asserts this anyway when removing resources, fail faster and consistently
    Util.assertMainThread();
    // memory cache needs to be cleared before bitmap pool to clear re-pooled Bitmaps too. See #687.
    memoryCache.clearMemory();
    bitmapPool.clearMemory();
    arrayPool.clearMemory();
}
```

# 七、包含几个线程池

先说结论，如果我没看遗漏的话，Glide 是一共包含七个线程池。**此处我所指的线程池的概念不单单指 ThreadPoolExecutor 类，而是指 `java.util.concurrent.Executor` 接口的任意实现类**

其中，前四个线程池可以从 EngineJob 类的构造参数得到答案

```java
class EngineJob<R> implements DecodeJob.Callback<R>, Poolable {
    
  EngineJob(
      GlideExecutor diskCacheExecutor,
      GlideExecutor sourceExecutor,
      GlideExecutor sourceUnlimitedExecutor,
      GlideExecutor animationExecutor,
      EngineJobListener engineJobListener,
      ResourceListener resourceListener,
      Pools.Pool<EngineJob<?>> pool) {
    this(
        diskCacheExecutor,
        sourceExecutor,
        sourceUnlimitedExecutor,
        animationExecutor,
        engineJobListener,
        resourceListener,
        pool,
        DEFAULT_FACTORY);
  }
    
}
```

其用途分别是：

1. diskCacheExecutor。用于加载磁盘缓存
2. sourceExecutor。用于执行非加载本地磁盘缓存的操作，例如，根据指定的 URI 或者 ImageUrl 去加载图片
3. sourceUnlimitedExecutor。同 sourceExecutor
4. animationExecutor。按官方的注释解释就是用于加载 Gif 

这四个线程池的创建逻辑可以看 GlideExecutor 类，这四个线程池的区别是：

1. diskCacheExecutor。核心线程数和最大线程数均为1，线程超时时间为0秒。因为 diskCacheExecutor 执行的是磁盘文件读写，核心线程数和最大线程数均为1就使得当线程池被启动后始终只有一个线程处于活跃状态，保证了文件读写时的有序性，避免了加锁操作
2. sourceExecutor。核心线程数和最大线程数根据设备的 CPU 个数来决定，至少是4个线程，线程超时时间为0秒。线程数量的设置就限制了 Glide 最多发起四个联网加载图片的请求
3. sourceUnlimitedExecutor。核心线程数为0，最大线程数为 Integer.MAX_VALUE，超时时间为10秒，当线程闲置时就会被马上回收。sourceUnlimitedExecutor 的目的是为了应对需要同时处理大量加载图片请求的需求，允许近乎无限制地新建线程来处理每个请求，在及时性上相对 sourceExecutor 可能会有所提升，但也可能反而会因为多线程竞争而降低效率，且也容易发生 OOM
4. animationExecutor。如果设备的 CPU 个数大于 4，则核心线程数和最大线程数设为2，否则设为1；线程超时时间为0秒

这四个线程池都用于 EngineJob 类。diskCacheExecutor 只用于磁盘缓存，只要本次请求允许使用磁盘缓存， diskCacheExecutor 就会被使用到。而其它三个线程池在我看来都是用于加载本地文件或者联网请求图片，如果 useUnlimitedSourceGeneratorPool 为 true，就使用 sourceUnlimitedExecutor，否则如果 useAnimationPool 为 true，就使用 animationExecutor，否则就使用 sourceExecutor

**useUnlimitedSourceGeneratorPool 的意义还好理解，就是为了控制同时并发请求的最大线程数，但区分 useAnimationPool 的意义我就不太理解了，懂的同学麻烦解答下**

```java
public synchronized void start(DecodeJob<R> decodeJob) {
    this.decodeJob = decodeJob;
    GlideExecutor executor =
        decodeJob.willDecodeFromCache() ? diskCacheExecutor : getActiveSourceExecutor();
    executor.execute(decodeJob);
}

private GlideExecutor getActiveSourceExecutor() {
    return useUnlimitedSourceGeneratorPool
        ? sourceUnlimitedExecutor
        : (useAnimationPool ? animationExecutor : sourceExecutor);
}
```

第五个线程池就位于 ActiveResources 类中。该线程池就用于不断从 ReferenceQueue 中取值判断，将当前已经不再被外部使用的图片资源缓存到 MemoryCache 中

```java
 ActiveResources(boolean isActiveResourceRetentionAllowed) {
    this(
        isActiveResourceRetentionAllowed,
        java.util.concurrent.Executors.newSingleThreadExecutor(
            new ThreadFactory() {
              @Override
              public Thread newThread(@NonNull final Runnable r) {
                return new Thread(
                    new Runnable() {
                      @Override
                      public void run() {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                        r.run();
                      }
                    },
                    "glide-active-resources");
              }
            }));
  }
```

其余的两个线程池则在 Executors 类中

1. MAIN_THREAD_EXECUTOR。用于当图片加载完成后，通过 Handler 切换到主线程来更新 UI

2. DIRECT_EXECUTOR。可以看做是一个空实现，会在原来的线程上执行 Runnable，当我们想直接取得图片资源而非更新 UI 时，例如 `Glide.with(this).load(url).submit()`，此时就会使用到

```java
public final class Executors {
  private Executors() {
    // Utility class.
  }

  private static final Executor MAIN_THREAD_EXECUTOR =
      new Executor() {
        private final Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable command) {
          handler.post(command);
        }
      };
  private static final Executor DIRECT_EXECUTOR =
      new Executor() {
        @Override
        public void execute(@NonNull Runnable command) {
          command.run();
        }
      };

  /** Posts executions to the main thread. */
  public static Executor mainThreadExecutor() {
    return MAIN_THREAD_EXECUTOR;
  }

  /** Immediately calls {@link Runnable#run()} on the current thread. */
  public static Executor directExecutor() {
    return DIRECT_EXECUTOR;
  }

  @VisibleForTesting
  public static void shutdownAndAwaitTermination(ExecutorService pool) {
    long shutdownSeconds = 5;
    pool.shutdownNow();
    try {
      if (!pool.awaitTermination(shutdownSeconds, TimeUnit.SECONDS)) {
        pool.shutdownNow();
        if (!pool.awaitTermination(shutdownSeconds, TimeUnit.SECONDS)) {
          throw new RuntimeException("Failed to shutdown");
        }
      }
    } catch (InterruptedException ie) {
      pool.shutdownNow();
      Thread.currentThread().interrupt();
      throw new RuntimeException(ie);
    }
  }
}
```

# 八、自定义网络请求库

默认情况下，Glide 是通过 HttpURLConnection 来联网加载图片的，相对于我们常用的 OkHttp 来说比较原始低效。而 Glide 也提供了 Registry 类，允许外部来自定义实现特定的请求逻辑

例如，如果你想要通过 OkHttp 来请求图片，那么可以依赖 Glide官方提供的支持库：

```groovy
dependencies {
    implementation "com.github.bumptech.glide:okhttp3-integration:4.11.0"
}
```

只要集成了 `okhttp3-integration`，那么 Glide 就会自动将网络类型的请求交由其内部的 OkHttp 来处理，因为其内部包含了一个声明了 **@GlideModule** 注解的 OkHttpLibraryGlideModule 类，可以在运行时被 Glide 解析到，之后就会将 GlideUrl 类型的加载请求交由 OkHttpUrlLoader 来进行处理

```java
@GlideModule
public final class OkHttpLibraryGlideModule extends LibraryGlideModule {
  @Override
  public void registerComponents(
      @NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
    registry.replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory());
  }
}
```

我们也可以将 `okhttp3-integration` 中的代码复制出来，在自定义的 AppGlideModule 类中传入自己实现的 OkHttpUrlLoader

```kotlin
@GlideModule
class MyAppGlideModule : AppGlideModule() {

    override fun isManifestParsingEnabled(): Boolean {
        return false
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        val okHttClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .eventListener(object : EventListener() {
                override fun callStart(call: okhttp3.Call) {
                    Log.e("TAG", "callStart： " + call.request().url().toString())
                }
            }).build()
        registry.replace(
            GlideUrl::class.java, InputStream::class.java,
            OkHttpUrlLoader.Factory(okHttClient)
        )
    }

}
```