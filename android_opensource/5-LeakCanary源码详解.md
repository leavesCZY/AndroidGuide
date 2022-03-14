> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

> 对于 Android Developer 来说，很多开源库都是属于**开发必备**的知识点，从使用方式到实现原理再到源码解析，这些都需要我们有一定程度的了解和运用能力。所以我打算来写一系列关于开源库**源码解析**和**实战演练**的文章，初定的目标是 **EventBus、ARouter、LeakCanary、Retrofit、Glide、OkHttp、Coil** 等七个知名开源库，希望对你有所帮助 🤣🤣

[LeakCanary](https://github.com/square/leakcanary/) 是由 [Square](https://github.com/square) 公司开源的用于 Android 的内存泄漏检测工具，可以帮助开发者发现内存泄露情况并且找出泄露源头，有助于减少 `OutOfMemoryError` 情况的发生。在目前的应用开发中也算作是性能优化的一个重要实现途径，很多面试官在考察性能优化时都会问到 LeakCanary 的实现原理

本文就来对其实现原理进行分析，具体的 Git 版本节点是：**9f62126e**，来了解 LeakCanary 的整体运行流程和实现原理 😂😂

# 一、支持的内存泄露类型

我们经常说 LeakCanary 能检测到应用内发生的内存泄露，那么它到底具体支持什么类型的内存泄露情况呢？LeakCanary   官网有对此进行介绍：

LeakCanary automatically detects leaks of the following objects:

- destroyed `Activity` instances
- destroyed `Fragment` instances
- destroyed fragment `View` instances
- cleared `ViewModel` instances

我们也可以从 LeakCanary 的 `AppWatcher.Config` 这个类找到答案。Config 类用于配置是否开启内存检测，从其配置项就可以看出来 leakcanary 支持：**Activity、Fragment、FragmentView、ViewModel** 等四种类型

```kotlin
data class Config(
    /**
     * Whether AppWatcher should automatically watch destroyed activity instances.
     *
     * Defaults to true.
     */
    val watchActivities: Boolean = true,

    /**
     * Whether AppWatcher should automatically watch destroyed fragment instances.
     *
     * Defaults to true.
     */
    val watchFragments: Boolean = true,

    /**
     * Whether AppWatcher should automatically watch destroyed fragment view instances.
     *
     * Defaults to true.
     */
    val watchFragmentViews: Boolean = true,

    /**
     * Whether AppWatcher should automatically watch cleared [androidx.lifecycle.ViewModel]
     * instances.
     *
     * Defaults to true.
     */
    val watchViewModels: Boolean = true,

    /**
     * How long to wait before reporting a watched object as retained.
     *
     * Default to 5 seconds.
     */
    val watchDurationMillis: Long = TimeUnit.SECONDS.toMillis(5),

    /**
     * Deprecated, this didn't need to be a part of the API.
     * Used to indicate whether AppWatcher should watch objects (by keeping weak references to
     * them). Currently a no-op.
     *
     * If you do need to stop watching objects, simply don't pass them to [objectWatcher].
     */
    @Deprecated("This didn't need to be a part of LeakCanary's API. No-Op.")
    val enabled: Boolean = true
  )
```

# 二、初始化

如今，我们在项目中引入 LeakCanary 只需要添加如下依赖即可，无须任何的初始化行为等附加操作，当应用启动时 LeakCanary 就会自动启动并开始监测了

```groovy
dependencies {
  // debugImplementation because LeakCanary should only run in debug builds.
  debugImplementation 'com.squareup.leakcanary:leakcanary-android:2.4'
}
```

一般来说，像这类第三方库都是需要由外部传入一个 `ApplicationContext` 对象进行初始化并启动的，LeakCanary 的 1.x 版本也是如此，但在 2.x 版本中，LeakCanary 将初始过程交由 `AppWatcherInstaller` 这个 `ContentProvider` 来自动完成

```kotlin
internal sealed class AppWatcherInstaller : ContentProvider() {

    /**
     * [MainProcess] automatically sets up the LeakCanary code that runs in the main app process.
     */
    internal class MainProcess : AppWatcherInstaller()

    /**
     * When using the `leakcanary-android-process` artifact instead of `leakcanary-android`,
     * [LeakCanaryProcess] automatically sets up the LeakCanary code
     */
    internal class LeakCanaryProcess : AppWatcherInstaller()

    override fun onCreate(): Boolean {
        val application = context!!.applicationContext as Application
        AppWatcher.manualInstall(application)
        return true
    }

    ···
}
```

由于 `ContentProvider` 会在 `Application` 被创建之前就由系统调用其 `onCreate()` 方法来完成初始化，所以 LeakCanary 通过 `AppWatcherInstaller` 就可以拿到 `Context` 来完成初始化并随应用一起启动，通过这种方式就简化了使用者的引入成本。而且由于我们的引用方式是 `debugImplementation`，所以**正式版本**会自动移除对 LeakCanary 的所有引用，进一步简化了引入成本

Jetpack 也包含了一个组件来实现**通过 ContentProvider 来完成初始化的逻辑**：[AppStartup](https://juejin.im/post/6847902224069165070)。在实现思路上两者很类似，但是如果每个三方库都通过自定义 `ContentProvider` 来实现初始化的话，那么应用的启动速度就会很感人了吧 :joy:，所以 Jetpack 官方推出的 `AppStartup` 应该是以后的主流才对

`AppWatcherInstaller` 最终会将 `Application` 对象传给 `InternalAppWatcher` 的 `install(Application)` 方法

```kotlin
/**
 * Note: this object must load fine in a JUnit environment
 */
internal object InternalAppWatcher {

  ···
    
  val objectWatcher = ObjectWatcher(
      clock = clock,
      checkRetainedExecutor = checkRetainedExecutor,
      isEnabled = { true }
  )

  fun install(application: Application) {
    //检测是否在 main 线程
    checkMainThread()
    //避免重复初始化
    if (this::application.isInitialized) {
      return
    }
    InternalAppWatcher.application = application
    if (isDebuggableBuild) {
      SharkLog.logger = DefaultCanaryLog()
    }
	//拿到默认配置，默认四种类型都进行检测
    val configProvider = { AppWatcher.config }
    //检测 Activity
    ActivityDestroyWatcher.install(application, objectWatcher, configProvider)
    //检测 Fragment、FragmentView、ViewModel
    FragmentDestroyWatcher.install(application, objectWatcher, configProvider)
    onAppWatcherInstalled(application)
  }

  ···
      
}
```

LeakCanary 具体进行内存泄露检测的逻辑可以分为三类：

- ObjectWatcher。检测 Object 的内存泄露情况
- ActivityDestroyWatcher。检测 Activity 的内存泄露情况
- FragmentDestroyWatcher。检测 Fragment、FragmentView、ViewModel 的内存泄露情况

当中，`ActivityDestroyWatcher` 和 `FragmentDestroyWatcher` 都需要依靠 `ObjectWatcher` 来完成，因为 `Activity、Fragment、FragmentView、ViewModel`  本质上都属于不同类型的 `Object` 

# 三、ObjectWatcher：检测任意对象

我们知道，当一个对象不再被我们引用时，如果该对象由于代码错误或者其它原因导致迟迟无法被系统回收，此时就是发生了内存泄露。那么 LeakCanary 是怎么知道应用是否发生了内存泄露呢？

这个可以依靠引用队列 `ReferenceQueue` 来实现。先来看个小例子

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 */
fun main() {
    val referenceQueue = ReferenceQueue<Pair<String, Int>?>()
    var pair: Pair<String, Int>? = Pair("name", 24)
    val weakReference = WeakReference(pair, referenceQueue)

    println(referenceQueue.poll()) //null

    pair = null

    System.gc()
    //GC 后休眠一段时间，等待 pair 被回收
    Thread.sleep(4000)

    println(referenceQueue.poll()) //java.lang.ref.WeakReference@d716361
}
```

可以看到，在 GC 过后 `referenceQueue.poll()` 的返回值变成了**非 null**，这是由于 `WeakReference` 和 `ReferenceQueue` 的一个组合特性导致的：在声明一个 `WeakReference` 对象时如果同时传入了 `ReferenceQueue` 作为构造参数的话，那么当 `WeakReference` 持有的对象被 GC 回收时，JVM 就会把这个**弱引用**存入与之关联的引用队列之中。依靠这个特性，我们就可以实现内存泄露的检测了

例如，当用户按返回键退出 Activity 时，正常情况下该 Activity 对象应该在不久后就被系统回收，我们可以监听 Activity 的 `onDestroy` 回调，在回调时把 Activity 对象保存到和 `ReferenceQueue` 关联的 `WeakReference` 中，在一段时间后（可以主动触发几次 GC）检测 `ReferenceQueue` 中是否有值，如果一直为 null 的话就说明发生了内存泄露。LeakCanary 就是通过这种方法来实现的

`ObjectWatcher` 中就封装了上述逻辑，这里来看看其实现逻辑

`ObjectWatcher` 的起始方法是 `watch(Any, String)`，该方法就用于监听指定对象

```kotlin
/**
 * References passed to [watch].
 * 用于保存要监听的对象，mapKey 是该对象的唯一标识、mapValue 是该对象的弱引用
 */
private val watchedObjects = mutableMapOf<String, KeyedWeakReference>()

//KeyedWeakReference 关联的引用队列
private val queue = ReferenceQueue<Any>()

/**
 * Watches the provided [watchedObject].
 *
 * @param description Describes why the object is watched.
 */
@Synchronized
fun watch(watchedObject: Any, description: String) {
    if (!isEnabled()) {
        return
    }
    removeWeaklyReachableObjects()
    //为 watchedObject 生成一个唯一标识
    val key = UUID.randomUUID().toString()
    val watchUptimeMillis = clock.uptimeMillis()
    //创建 watchedObject 关联的弱引用
    val reference = KeyedWeakReference(watchedObject, key, description, watchUptimeMillis, queue)
    ···
    watchedObjects[key] = reference
    checkRetainedExecutor.execute {
        moveToRetained(key)
    }
}
```

`watch()` 方法的主要逻辑：

1. 为每个 `watchedObject` 生成一个**唯一标识 key**，通过该 key 构建一个 `watchedObject` 的弱引用 `KeyedWeakReference`，将该弱引用保存到 `watchedObjects` 中。`ObjectWatcher` 可以先后监测多个对象，每个对象都会先被存入到 `watchedObjects` 中
2. 外部通过传入的 `checkRetainedExecutor` 来指定检测内存泄露的触发时机，通过 `moveToRetained` 方法来判断是否真的发生了内存泄露

`KeyedWeakReference`  是一个自定义的 `WeakReference` 子类，包含一个唯一 key 来标识特定对象，也包含一个 `retainedUptimeMillis` 字段用来标记是否发生了内存泄露

```kotlin
class KeyedWeakReference(
        referent: Any,
        val key: String,
        val description: String,
        val watchUptimeMillis: Long,
        referenceQueue: ReferenceQueue<Any>
) : WeakReference<Any>(
        referent, referenceQueue
) {
    /**
     * Time at which the associated object ([referent]) was considered retained, or -1 if it hasn't
     * been yet.
     * 用于标记 referent 是否还未被回收，是的话则值不为 -1
     */
    @Volatile
    var retainedUptimeMillis = -1L

    companion object {
        @Volatile
        @JvmStatic
        var heapDumpUptimeMillis = 0L
    }

}
```

`moveToRetained` 方法就用于判断指定 key 关联的对象是否已经泄露，如果没有泄露则移除对该对象的弱引用，有泄露的话则更新其 `retainedUptimeMillis` 值，以此来标记其发生了泄露，并同时通过回调 `onObjectRetainedListeners` 来分析内存泄露链

```kotlin
@Synchronized
private fun moveToRetained(key: String) {
    removeWeaklyReachableObjects()
    val retainedRef = watchedObjects[key]
    if (retainedRef != null) {
        //记录当前时间
        retainedRef.retainedUptimeMillis = clock.uptimeMillis()
        onObjectRetainedListeners.forEach { it.onObjectRetained() }
    }
}

//如果判断到一个对象没有发生内存泄露，那么就移除对该对象的弱引用
//此方法会先后调用多次
private fun removeWeaklyReachableObjects() {
    // WeakReferences are enqueued as soon as the object to which they point to becomes weakly
    // reachable. This is before finalization or garbage collection has actually happened.
    var ref: KeyedWeakReference?
    do {
        ref = queue.poll() as KeyedWeakReference?
        if (ref != null) {
            //如果 ref 不为 null，说明 ref 关联的对象没有发生内存泄露，那么就移除对该对象的引用
            watchedObjects.remove(ref.key)
        }
    } while (ref != null)
}
```

# 四、ActivityDestroyWatcher：检测Activity 

理解了 `ObjectWatcher` 的流程后来看 `ActivityDestroyWatcher` 就会比较简单了。`ActivityDestroyWatcher` 会向 `Application` 注册一个 `ActivityLifecycleCallbacks` 回调，当收到每个 Activity 执行了 `onDestroy` 的回调后，就会将将 Activity 对象转交由 `ObjectWatcher` 来进行监听

```kotlin
internal class ActivityDestroyWatcher private constructor(private val objectWatcher: ObjectWatcher, private val configProvider: () -> Config) {

    private val lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks by noOpDelegate() {
        override fun onActivityDestroyed(activity: Activity) {
            if (configProvider().watchActivities) {
                objectWatcher.watch(activity, "${activity::class.java.name} received Activity#onDestroy() callback"
                )
            }
        }
    }

    companion object {
        fun install(application: Application, objectWatcher: ObjectWatcher, configProvider: () -> Config) {
            val activityDestroyWatcher = ActivityDestroyWatcher(objectWatcher, configProvider)
            application.registerActivityLifecycleCallbacks(activityDestroyWatcher.lifecycleCallbacks)
        }
    }

}
```

# 五、FragmentDestroyWatcher：检测Fragment 

做 Android 应用开发的应该都知道，现在 Google 提供的基础依赖包分为了 **Support** 和 **AndroidX** 两种，**Support** 版本已经不再维护，主流的都是使用 **AndroidX** 了。而 LeakCanary 为了照顾老项目，就贴心的为这两种版本分别提供了 Fragment 的内存检测功能

`FragmentDestroyWatcher` 可以看做是一个分发器，它会根据外部环境的不同来选择不同的检测手段，其主要逻辑是：

- 系统版本大于等于 8.0。使用 AndroidOFragmentDestroyWatcher 来检测 Fragment、FragmentView 的内存泄露
- 开发者使用的是 Support 包。使用 AndroidSupportFragmentDestroyWatcher 来检测 Fragment、FragmentView 的内存泄露
- 开发者使用的是 AndroidX 包。使用 AndroidXFragmentDestroyWatcher 来检测 Fragment、FragmentView、ViewModel 的内存泄露
- 通过反射 Class.forName 来判断开发者使用的是 Support 包还是 AndroidX 包
- 由于 Fragment 都需要被挂载在 Activity 上，所有向 Application 注册一个 ActivityLifecycleCallback，每当有 Activity 被创建时就监听该 Activity 内可能存在的 Fragment

> 这里令我很疑惑的一个点就是：当系统版本大于等于  8.0 时，AndroidOFragmentDestroyWatcher 不就会和 AndroidSupportFragmentDestroyWatcher 或者 AndroidXFragmentDestroyWatcher 重复了吗？这算咋回事:joy:

```kotlin
internal object FragmentDestroyWatcher {

  private const val ANDROIDX_FRAGMENT_CLASS_NAME = "androidx.fragment.app.Fragment"
  private const val ANDROIDX_FRAGMENT_DESTROY_WATCHER_CLASS_NAME =
    "leakcanary.internal.AndroidXFragmentDestroyWatcher"

  // Using a string builder to prevent Jetifier from changing this string to Android X Fragment
  @Suppress("VariableNaming", "PropertyName")
  private val ANDROID_SUPPORT_FRAGMENT_CLASS_NAME =
    StringBuilder("android.").append("support.v4.app.Fragment")
        .toString()
  private const val ANDROID_SUPPORT_FRAGMENT_DESTROY_WATCHER_CLASS_NAME =
    "leakcanary.internal.AndroidSupportFragmentDestroyWatcher"

  fun install(
    application: Application,
    objectWatcher: ObjectWatcher,
    configProvider: () -> AppWatcher.Config
  ) {
    val fragmentDestroyWatchers = mutableListOf<(Activity) -> Unit>()

    if (SDK_INT >= O) {
      fragmentDestroyWatchers.add(
          AndroidOFragmentDestroyWatcher(objectWatcher, configProvider)
      )
    }

    //AndroidX 
    getWatcherIfAvailable(
        ANDROIDX_FRAGMENT_CLASS_NAME,
        ANDROIDX_FRAGMENT_DESTROY_WATCHER_CLASS_NAME,
        objectWatcher,
        configProvider
    )?.let {
      fragmentDestroyWatchers.add(it)
    }

    //Support 
    getWatcherIfAvailable(
        ANDROID_SUPPORT_FRAGMENT_CLASS_NAME,
        ANDROID_SUPPORT_FRAGMENT_DESTROY_WATCHER_CLASS_NAME,
        objectWatcher,
        configProvider
    )?.let {
      fragmentDestroyWatchers.add(it)
    }

    if (fragmentDestroyWatchers.size == 0) {
      return
    }

    application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks by noOpDelegate() {
      override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?
      ) {
        for (watcher in fragmentDestroyWatchers) {
          watcher(activity)
        }
      }
    })
  }

  ···
}
```

由于 `AndroidXFragmentDestroyWatcher`、`AndroidSupportFragmentDestroyWatcher`、`AndroidOFragmentDestroyWatcher` 在逻辑上很类似，且就 `AndroidXFragmentDestroyWatcher` 同时提供了 `ViewModel` 内存泄露的检测功能，所以这里只看 `AndroidXFragmentDestroyWatcher` 就行

`AndroidXFragmentDestroyWatcher` 的主要逻辑是：

- 在 invoke 方法里向 Activity 的 FragmentManager 以及 childFragmentManager 注册一个 FragmentLifecycleCallback，通过该回调拿到 onFragmentViewDestroyed 和 onFragmentDestroyed 的事件通知，收到通知时就通过 ObjectWatcher 启动检测
- 在 onFragmentCreated 回调里通过 ViewModelClearedWatcher 来启动和 Fragment 关联的 ViewModel 的内存泄露检测逻辑
- 在 invoke 方法里通过 ViewModelClearedWatcher 来启动和 Activity 关联的 ViewModel 的内存泄露检测

```kotlin
internal class AndroidXFragmentDestroyWatcher(
  private val objectWatcher: ObjectWatcher,
  private val configProvider: () -> Config
) : (Activity) -> Unit {

  private val fragmentLifecycleCallbacks = object : FragmentManager.FragmentLifecycleCallbacks() {

    override fun onFragmentCreated(
      fm: FragmentManager,
      fragment: Fragment,
      savedInstanceState: Bundle?
    ) {
      ViewModelClearedWatcher.install(fragment, objectWatcher, configProvider)
    }

    override fun onFragmentViewDestroyed(
      fm: FragmentManager,
      fragment: Fragment
    ) {
      val view = fragment.view
      if (view != null && configProvider().watchFragmentViews) {
        objectWatcher.watch(
            view, "${fragment::class.java.name} received Fragment#onDestroyView() callback " +
            "(references to its views should be cleared to prevent leaks)"
        )
      }
    }

    override fun onFragmentDestroyed(
      fm: FragmentManager,
      fragment: Fragment
    ) {
      if (configProvider().watchFragments) {
        objectWatcher.watch(
            fragment, "${fragment::class.java.name} received Fragment#onDestroy() callback"
        )
      }
    }
  }

  override fun invoke(activity: Activity) {
    if (activity is FragmentActivity) {
      val supportFragmentManager = activity.supportFragmentManager
      supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, true)
      ViewModelClearedWatcher.install(activity, objectWatcher, configProvider)
    }
  }
}
```

Fragment 和 FragmentView 走向 `Destroyed` 时，正常情况下它们都是不会被复用的，应该会很快就被 GC 回收，且它们本质上都只是一种对象，所以直接使用 `ObjectWatcher` 进行检测即可

# 六、ViewModelClearedWatcher：检测ViewModel

和 Fragment、FragmentView 相比，ViewModel 就比较特殊了，由于可能存在一个 Activity 和多个 Fragment 同时持有一个 ViewModel 实例的情况，而 leakcanary 无法知道 ViewModel 到底是同时被几个持有者所持有，所以无法通过单独一个 Activity 和 Fragment 的 `Destroyed` 回调来启动对 ViewModel 的检测。幸好 ViewMode 也提供了 `onCleared()` 的回调事件，leakcanary 就通过该回调来知道 ViewModel 是什么时候需要被回收。对 ViewModel 的实现原理不清楚的同学可以看我的这篇文章：[从源码看 Jetpack（6）-ViewModel源码详解](https://juejin.im/post/6873356946896846856)

`ViewModelClearedWatcher` 的主要逻辑是：

- ViewModelClearedWatcher 继承于 ViewModel，当拿到 ViewModelStoreOwner 实例（Activity 或者 Fragment）后，就创建一个和该实例绑定的 ViewModelClearedWatcher 对象
- ViewModelClearedWatcher 通过反射获取到 ViewModelStore 中的 mMap 变量，该变量就存储了所有的 Viewmodel 实例
- 当 ViewModelClearedWatcher 的 onCleared() 方法被回调了，就说明了所有和 Activity 或者 Fragment 绑定的 ViewModel 实例都不再被需要了，此时就可以开始监测所有的 ViewModel 实例了

```kotlin
internal class ViewModelClearedWatcher(
        storeOwner: ViewModelStoreOwner,
        private val objectWatcher: ObjectWatcher,
        private val configProvider: () -> Config
) : ViewModel() {

    private val viewModelMap: Map<String, ViewModel>?

    init {
        // We could call ViewModelStore#keys with a package spy in androidx.lifecycle instead,
        // however that was added in 2.1.0 and we support AndroidX first stable release. viewmodel-2.0.0
        // does not have ViewModelStore#keys. All versions currently have the mMap field.
        viewModelMap = try {
            val mMapField = ViewModelStore::class.java.getDeclaredField("mMap")
            mMapField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            mMapField[storeOwner.viewModelStore] as Map<String, ViewModel>
        } catch (ignored: Exception) {
            null
        }
    }

    override fun onCleared() {
        if (viewModelMap != null && configProvider().watchViewModels) {
            viewModelMap.values.forEach { viewModel ->
                objectWatcher.watch(
                        viewModel, "${viewModel::class.java.name} received ViewModel#onCleared() callback"
                )
            }
        }
    }

    companion object {
        fun install(storeOwner: ViewModelStoreOwner, objectWatcher: ObjectWatcher, configProvider: () -> Config) {
            val provider = ViewModelProvider(storeOwner, object : Factory {
              @Suppress("UNCHECKED_CAST")
              override fun <T : ViewModel?> create(modelClass: Class<T>): T =
                      ViewModelClearedWatcher(storeOwner, objectWatcher, configProvider) as T
            })
            provider.get(ViewModelClearedWatcher::class.java)
        }
    }
}
```

# 七、检测到内存泄露后的流程

我们不可能在 Activity 刚被回调了 `onDestroy` 方法就马上来判断 `ReferenceQueue` 中是否有值，因为 JVM 的 GC 时机是不确定的，Activity 对象可能不会那么快就被回收，所以需要延迟一段时间后再来检测。而即使延迟检测了，也可能会存在**应用没有发生内存泄露只是系统还未执行 GC** 的情况，所以就需要去主动触发 GC，经过几轮检测后才可以确定当前应用是否的确发生了内存泄露

这里就来看下具体的检测流程

`ObjectWatcher` 对象包含了一个 `Executor` 参数：`checkRetainedExecutor`。检测操作的触发时机就取决于向 `checkRetainedExecutor` 提交的任务在什么时候会被执行

```kotlin
class ObjectWatcher constructor(private val clock: Clock, private val checkRetainedExecutor: Executor,
        /**
         * Calls to [watch] will be ignored when [isEnabled] returns false
         */
        private val isEnabled: () -> Boolean = { true }
) {

    ···

    /**
     * Watches the provided [watchedObject].
     *
     * @param description Describes why the object is watched.
     */
    @Synchronized
    fun watch(
            watchedObject: Any,
            description: String
    ) {
        if (!isEnabled()) {
            return
        }
        removeWeaklyReachableObjects()
        val key = UUID.randomUUID()
                .toString()
        val watchUptimeMillis = clock.uptimeMillis()
        val reference =
                KeyedWeakReference(watchedObject, key, description, watchUptimeMillis, queue)
        SharkLog.d {
            "Watching " +
                    (if (watchedObject is Class<*>) watchedObject.toString() else "instance of ${watchedObject.javaClass.name}") +
                    (if (description.isNotEmpty()) " ($description)" else "") +
                    " with key $key"
        }
        watchedObjects[key] = reference
        //重点
        checkRetainedExecutor.execute {
            moveToRetained(key)
        }
    }
    
    //判断 key 关联的对象是否已经泄露
    //是的话则将更新其 retainedUptimeMillis 值，以此来标记其发生了泄露
    @Synchronized
    private fun moveToRetained(key: String) {
        removeWeaklyReachableObjects()
        val retainedRef = watchedObjects[key]
        if (retainedRef != null) {
            retainedRef.retainedUptimeMillis = clock.uptimeMillis()
            //重点，向外发出可能有内存泄露的通知
            onObjectRetainedListeners.forEach { it.onObjectRetained() }
        }
    }

    ···
    
}
```

`ObjectWatcher` 对象又是在 `InternalAppWatcher` 里初始化的，`checkRetainedExecutor` 在收到任务后会通过 `Handler` 来延时五秒执行

```kotlin
internal object InternalAppWatcher {

  ···	
    
  private val mainHandler by lazy {
    Handler(Looper.getMainLooper())
  }

  private val checkRetainedExecutor = Executor {
    mainHandler.postDelayed(it, AppWatcher.config.watchDurationMillis)
  }
  
  val objectWatcher = ObjectWatcher(
      clock = clock,
      checkRetainedExecutor = checkRetainedExecutor,
      isEnabled = { true }
  )

  ···

}
```

`ObjectWatcher` 的 `moveToRetained` 方法又会通过 `onObjectRetained` 向外发出通知：**当前可能发生了内存泄露**。`InternalLeakCanary` 会收到这个通知，然后交由 `HeapDumpTrigger` 来进行检测

```kotlin
internal object InternalLeakCanary : (Application) -> Unit, OnObjectRetainedListener {
   
    private lateinit var heapDumpTrigger: HeapDumpTrigger
    
    override fun onObjectRetained() = scheduleRetainedObjectCheck()

	fun scheduleRetainedObjectCheck() {
    	if (this::heapDumpTrigger.isInitialized) {
      		heapDumpTrigger.scheduleRetainedObjectCheck()
    	}
 	}
    
    ···
    
}
```

当 LeakCanary 判定当前真的存在内存泄露时，就会进行 **DumpHeap**，找到泄露对象的引用链，而这个操作是比较**费时费内存**的，可能会直接导致应用页面无响应，所以 LeakCanary 进行 DumpHeap 前会有许多前置检查操作和前置条件，就是为了尽量减少 DumpHeap 次数以及在 DumpHeap 时尽量减少对开发人员的干扰

 `heapDumpTrigger` 的 `scheduleRetainedObjectCheck()` 方法的主要逻辑是：

1. 获取当前还未回收的对象个数 retainedKeysCount。如果个数大于 0，则先主动触发 GC，尽量尝试回收对象，避免误判，然后执行第二步；如果个数为 0，那么流程就结束了
2. GC 过后再次更新 retainedKeysCount 值，如果对象都被回收了（即 retainedKeysCount 值为 0），那么流程就结束了，否则就执行第三步
3. 如果 retainedKeysCount 小于阈值 5，且当前“应用处于前台”或者是“应用处于后台但退到后台的时间还未超出五秒”，那么就启动一个定时任务，在二十秒后重新执行第一步，否则执行第四步
4. 如果上一次 DumpHeap 离现在不足一分钟，那么就启动一个定时任务，满一分钟后重新执行第一步，否则执行第五步
5. 此时各个条件都满足了，已经可以确定发生了内存泄漏，去执行 DumpHeap

```kotlin
internal class HeapDumpTrigger(
        private val application: Application,
        private val backgroundHandler: Handler,
        private val objectWatcher: ObjectWatcher,
        private val gcTrigger: GcTrigger,
        private val heapDumper: HeapDumper,
        private val configProvider: () -> Config
) {

    ···
    
    fun scheduleRetainedObjectCheck(
            delayMillis: Long = 0L
    ) {
        val checkCurrentlyScheduledAt = checkScheduledAt
        if (checkCurrentlyScheduledAt > 0) {
            //如果当前已经在进行检测了，则直接返回
            return
        }
        checkScheduledAt = SystemClock.uptimeMillis() + delayMillis
        backgroundHandler.postDelayed({
          checkScheduledAt = 0
          checkRetainedObjects()
        }, delayMillis)
    }

    private fun checkRetainedObjects() {
        val iCanHasHeap = HeapDumpControl.iCanHasHeap()

        val config = configProvider()

        if (iCanHasHeap is Nope) {
            if (iCanHasHeap is NotifyingNope) {
                // Before notifying that we can't dump heap, let's check if we still have retained object.
                var retainedReferenceCount = objectWatcher.retainedObjectCount

                if (retainedReferenceCount > 0) {
                    gcTrigger.runGc()
                    retainedReferenceCount = objectWatcher.retainedObjectCount
                }

                val nopeReason = iCanHasHeap.reason()
                val wouldDump = !checkRetainedCount(
                        retainedReferenceCount, config.retainedVisibleThreshold, nopeReason
                )

                if (wouldDump) {
                    val uppercaseReason = nopeReason[0].toUpperCase() + nopeReason.substring(1)
                    onRetainInstanceListener.onEvent(DumpingDisabled(uppercaseReason))
                    showRetainedCountNotification(
                            objectCount = retainedReferenceCount,
                            contentText = uppercaseReason
                    )
                }
            } else {
                SharkLog.d {
                    application.getString(
                            R.string.leak_canary_heap_dump_disabled_text, iCanHasHeap.reason()
                    )
                }
            }
            return
        }

        //获取当前还未回收的对象个数
        var retainedReferenceCount = objectWatcher.retainedObjectCount

        if (retainedReferenceCount > 0) {
            //主动触发 GC，尽量尝试回收对象，避免误判
            gcTrigger.runGc()
            retainedReferenceCount = objectWatcher.retainedObjectCount
        }

        if (checkRetainedCount(retainedReferenceCount, config.retainedVisibleThreshold))
            return

        val now = SystemClock.uptimeMillis()
        val elapsedSinceLastDumpMillis = now - lastHeapDumpUptimeMillis

        //如果上一次 DumpHeap 离现在不足一分钟，那么就启动一个定时任务，满一分钟后再次检查
        if (elapsedSinceLastDumpMillis < WAIT_BETWEEN_HEAP_DUMPS_MILLIS) {
            onRetainInstanceListener.onEvent(DumpHappenedRecently)
            showRetainedCountNotification(
                    objectCount = retainedReferenceCount,
                    contentText = application.getString(R.string.leak_canary_notification_retained_dump_wait)
            )
            scheduleRetainedObjectCheck(
                    delayMillis = WAIT_BETWEEN_HEAP_DUMPS_MILLIS - elapsedSinceLastDumpMillis
            )
            return
        }

        dismissRetainedCountNotification()
        //各个条件都满足了，已经可以确定发生了内存泄漏，去执行 DumpiHeap
        dumpHeap(retainedReferenceCount, retry = true)
    }

    /**
     * 判断当前是否符合 DumpHeap 的条件，符合的话返回 false
     * @param retainedKeysCount 当前还未回收的对象个数
     * @param retainedVisibleThreshold 触发 DumpHeap 的阈值
     * 只有当 retainedKeysCount 大于等于 retainedVisibleThreshold 时才会触发 DumpHeap，默认值是 5
     * @param nopeReason
     */
    private fun checkRetainedCount(
            retainedKeysCount: Int,
            retainedVisibleThreshold: Int,
            nopeReason: String? = null
    ): Boolean {
        //用于标记本次检测相对上次，未回收的对象个数是否发生了变化
        val countChanged = lastDisplayedRetainedObjectCount != retainedKeysCount
        lastDisplayedRetainedObjectCount = retainedKeysCount
        if (retainedKeysCount == 0) {
            if (countChanged) {
                //如果 retainedKeysCount 为 0，且值相对上次检测减少了，则说明有对象被回收了
                SharkLog.d { "All retained objects have been garbage collected" }
                onRetainInstanceListener.onEvent(NoMoreObjects)
                showNoMoreRetainedObjectNotification()
            }
            return true
        }

        //应用是否还在前台
        val applicationVisible = applicationVisible
        val applicationInvisibleLessThanWatchPeriod = applicationInvisibleLessThanWatchPeriod

        ···

        if (retainedKeysCount < retainedVisibleThreshold) { //还未达到阈值
            if (applicationVisible || applicationInvisibleLessThanWatchPeriod) {
                if (countChanged) {
                    onRetainInstanceListener.onEvent(BelowThreshold(retainedKeysCount))
                }
                //在通知栏显示当前未回收的对象个数
                showRetainedCountNotification(
                        objectCount = retainedKeysCount,
                        contentText = application.getString(
                                R.string.leak_canary_notification_retained_visible, retainedVisibleThreshold
                        )
                )
                //retainedKeysCount 还未达到阈值，且当前“应用处于前台”或者是“应用处于后台但退到后台的时间还未超出五秒”
                //此时就启动一个定时任务，在二十秒后重新再检测一遍
                scheduleRetainedObjectCheck(
                        delayMillis = WAIT_FOR_OBJECT_THRESHOLD_MILLIS
                )
                return true
            }
        }
        return false
    }

   ···

}
```

更后面的流程就涉及具体的 DumpHeap 操作了，这里就不再展开了，因为我也不太懂，后续有机会再单独写一篇文章来介绍了~~

# 八、小提示

## 1、检测任意对象

除了 LeakCanary 默认支持的四种类型外，我们还可以主动检测任意对象。例如，可以检测 Service： 

```kotlin
class MyService : Service {

  // ...

  override fun onDestroy() {
    super.onDestroy()
    AppWatcher.objectWatcher.watch(
      watchedObject = this,
      description = "MyService received Service#onDestroy() callback"
    )
  }
}
```

## 2、更改配置项

LeakCanary 提供的默认配置项大多数情况已经很适合我们在项目中直接使用了，而如果我们想要更改 LeakCanary 的默认配置项（例如不希望检测 FragmentView），可以在 Application 中进行更改：

```kotlin
class DebugExampleApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    AppWatcher.config = AppWatcher.config.copy(watchFragmentViews = false)
  }
}
```

由于 LeakCanary 的引用方式是 `debugImplementation`，在 `releas` 环境下是引用不到 LeakCanary 的，所以为了避免在生成 `release` 包时需要主动来删除这行配置项，需要将 `DebugExampleApplication` 放到 `src/debug/java` 文件夹中

# 九、结尾

可以看出 **Activity、Fragment、FragmentView、ViewModel** 等四种类型的内存检测都是需要依靠 `ObjectWatcher` 来完成的，因为这四种类型本质上都是属于不同的对象。而 `ObjectWatcher` 需要依靠引用队列 `ReferenceQueue` 来实现，因此 LeakCanary 的基本实现基础就是来源于 Java 的原生特性

LeakCanary 的整体源码讲得也差不多了，后边就再来写一篇关于**内存泄露**的扩展阅读 😂😂