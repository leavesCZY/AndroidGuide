在两个多月前我开始写 **从源码看 Jetpack** 系列文章，从源码角度介绍 Jetpack 多个组件实现原理，写了一半就停笔去写 **Java 多线程编程** 的文章去了，本篇文章就再来补上 ViewModel 这一个最为基础也最为开发者熟悉的组件

本文所讲的的源代码基于以下依赖库当前最新版本：

```groovy
compileSdkVersion 30

implementation 'androidx.appcompat:appcompat:1.3.0-alpha02'
implementation "androidx.lifecycle:lifecycle-viewmodel:2.3.0-alpha07"
```

ViewModel 的使用方式基本是按照如下模板：ViewModelStoreOwner（Activity/Fragment）通过 ViewModelProvider 来完成 ViewModel 实例的初始化，并通过和 LifecycleOwner 绑定的方式来监听 LiveData 中数据的变化从而做出各种响应

```kotlin
/**
 * 作者：leavesC
 * 时间：2020/9/16 21:37
 * 描述：
 * GitHub：https://github.com/leavesC
 */
class MainActivity : AppCompatActivity() {

    private val myViewModel by lazy {
        ViewModelProvider(this).get(MyViewModel::class.java).apply {
            nameLiveData.observe(this@MainActivity, {

            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

}

class MyViewModel : ViewModel() {

    val nameLiveData = MutableLiveData<String>()

    override fun onCleared() {
        super.onCleared()
        Log.e("MyViewModel", "onCleared")
    }

}
```

### 一、如何初始化？

在上面的例子中，我们并没有看到 ViewModel 是如何进行初始化的，也没有手动调用 ViewModel 的构造函数来创建 ViewModel 实例，这是因为这个操作都隐藏在了 ViewModelProvider 内部，由 ViewModelProvider 自己来通过反射构建

`AppCompatActivity` 的父类 `androidx.activity.ComponentActivity` 已经实现了`ViewModelStoreOwner`接口，所以我们调用的构造函数会根据我们传入的 ViewModelStoreOwner 实例是否继承了 `HasDefaultViewModelProviderFactory` 接口来决定如何完成 `mFactory`变量的初始化：

- 继承了。那么就就直接使用其返回的 Factory 实例
- 没有继承。 那么就使用 NewInstanceFactory

```java
    private final Factory mFactory;
    private final ViewModelStore mViewModelStore;

	public ViewModelProvider(@NonNull ViewModelStoreOwner owner) {
        this(owner.getViewModelStore(), owner instanceof HasDefaultViewModelProviderFactory
                ? ((HasDefaultViewModelProviderFactory) owner).getDefaultViewModelProviderFactory()
                : NewInstanceFactory.getInstance());
    }

    public ViewModelProvider(@NonNull ViewModelStoreOwner owner, @NonNull Factory factory) {
        this(owner.getViewModelStore(), factory);
    }

    public ViewModelProvider(@NonNull ViewModelStore store, @NonNull Factory factory) {
        mFactory = factory;
        mViewModelStore = store;
    }
```

`Factory` 是 ViewModelProvider 的内部接口，用于实现初始化 ViewModel 实例的逻辑。例如，`NewInstanceFactory` 就实现了通过**反射**来初始化**构造函数无参数类型的 ViewModel** 的逻辑

```java
    /**
     * Implementations of {@code Factory} interface are responsible to instantiate ViewModels.
     */
    public interface Factory {
        /**
         * Creates a new instance of the given {@code Class}.
         * <p>
         *
         * @param modelClass a {@code Class} whose instance is requested
         * @param <T>        The type parameter for the ViewModel.
         * @return a newly created ViewModel
         */
        @NonNull
        <T extends ViewModel> T create(@NonNull Class<T> modelClass);
    }

 	public static class NewInstanceFactory implements Factory {

        private static NewInstanceFactory sInstance;

        /**
         * Retrieve a singleton instance of NewInstanceFactory.
         *
         * @return A valid {@link NewInstanceFactory}
         */
        @NonNull
        static NewInstanceFactory getInstance() {
            if (sInstance == null) {
                sInstance = new NewInstanceFactory();
            }
            return sInstance;
        }

        @SuppressWarnings("ClassNewInstance")
        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            //noinspection TryWithIdenticalCatches
            try {
                //直接通过反射来完成 ViewModel 的初始化
                //传入的 ViewModelClass 必须包含无参构造函数 
                return modelClass.newInstance();
            } catch (InstantiationException e) {
                throw new RuntimeException("Cannot create an instance of " + modelClass, e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Cannot create an instance of " + modelClass, e);
            }
        }
    }
```

由于 `AppCompatActivity` 的父类 `androidx.activity.ComponentActivity` 已经实现了 `HasDefaultViewModelProviderFactory` 接口，所以我们直接调用 ViewModelProvider 只包含一个参数的构造函数是行得通的

`ComponentActivity` 的 `getDefaultViewModelProviderFactory()` 方法返回的是 `SavedStateViewModelFactory`，它和 Jetpack 的另外一个组件 `SavedStateHandle` 有关，在下一篇文章中会介绍，在这里  `SavedStateViewModelFactory` 起的作用就和 `NewInstanceFactory` 完全一样

```java
    private ViewModelProvider.Factory mDefaultFactory;

	@NonNull
    @Override
    public ViewModelProvider.Factory getDefaultViewModelProviderFactory() {
        if (getApplication() == null) {
            throw new IllegalStateException("Your activity is not yet attached to the "
                    + "Application instance. You can't request ViewModel before onCreate call.");
        }
        if (mDefaultFactory == null) {
            mDefaultFactory = new SavedStateViewModelFactory(
                    getApplication(),
                    this,
                    getIntent() != null ? getIntent().getExtras() : null);
        }
        return mDefaultFactory;
    }
```

既然 Factory 实例也有了，下一步就是来调用 `get()` 方法了。`get()` 方法需要我们传入 Class 对象，ViewModelProvider 需要拿到 Class 才能完成反射操作。在此方法里主要是通过 modelClass 来自动生成一个字符串 Key，并将参数转发给另外一个 `get()` 方法

```java
    @NonNull
    @MainThread
    public <T extends ViewModel> T get(@NonNull Class<T> modelClass) {
        String canonicalName = modelClass.getCanonicalName();
        if (canonicalName == null) {
            throw new IllegalArgumentException("Local and anonymous classes can not be ViewModels");
        }
        return get(DEFAULT_KEY + ":" + canonicalName, modelClass);
    }
```

可以看出来，以下方法会通过 key 从 `ViewModelStore` 里取 ViewModel 实例，如果取不到值或者是取出来的值类型不符，则会通过 `mFactory.create(modelClass)` 方法来反射初始化 ViewModel，并在返回初始化结果前将它存到 `mViewModelStore` 中，这样就完成了 ViewModel 的初始化流程了

```java
    private final ViewModelStore mViewModelStore;

	@NonNull
    @MainThread
    public <T extends ViewModel> T get(@NonNull String key, @NonNull Class<T> modelClass) {
        ViewModel viewModel = mViewModelStore.get(key);

        if (modelClass.isInstance(viewModel)) {
            if (mFactory instanceof OnRequeryFactory) {
                ((OnRequeryFactory) mFactory).onRequery(viewModel);
            }
            return (T) viewModel;
        } else {
            //noinspection StatementWithEmptyBody
            if (viewModel != null) {
                // TODO: log a warning.
            }
        }
        if (mFactory instanceof KeyedFactory) {
            viewModel = ((KeyedFactory) mFactory).create(key, modelClass);
        } else {
            viewModel = mFactory.create(modelClass);
        }
        mViewModelStore.put(key, viewModel);
        return (T) viewModel;
    }
```

### 二、如何保持不变？

既然 Activity 每次创建的 ViewModel 实例都会保存到 ViewModelStore 中，而 Activity 可以在屏幕旋转的情况下保持持有的 ViewModel 实例不变，那么不也就间接说明了在这种情况下 ViewModelStore 也是可以一直保存而不被销毁的了吗

所以，想要知道 ViewModel 是如何保持不变的，那就看 ViewModelStore 实例是如何被保存不被销毁就可以了

ViewModelStore 本身实现的逻辑挺简单的，通过一个 HashMap 来缓存每一个 ViewModel 实例，并提供了存取 ViewModel 的方法

```java
public class ViewModelStore {

    private final HashMap<String, ViewModel> mMap = new HashMap<>();

    final void put(String key, ViewModel viewModel) {
        ViewModel oldViewModel = mMap.put(key, viewModel);
        if (oldViewModel != null) {
            oldViewModel.onCleared();
        }
    }

    final ViewModel get(String key) {
        return mMap.get(key);
    }

    Set<String> keys() {
        return new HashSet<>(mMap.keySet());
    }

    /**
     *  Clears internal storage and notifies ViewModels that they are no longer used.
     */
    public final void clear() {
        for (ViewModel vm : mMap.values()) {
            vm.clear();
        }
        mMap.clear();
    }
}
```

由于 `AppCompatActivity` 的父类 `androidx.activity.ComponentActivity` 已经实现了`ViewModelStoreOwner`接口，所以也相当于每个 `AppCompatActivity` 也都持有了一个 ViewModelStore 实例

```java
public interface ViewModelStoreOwner {
    /**
     * Returns owned {@link ViewModelStore}
     *
     * @return a {@code ViewModelStore}
     */
    @NonNull
    ViewModelStore getViewModelStore();
}
```

`ComponentActivity` 的 `getViewModelStore()` 方法获取 ViewModelStore 实例的来源有两种：

- 如果 NonConfigurationInstances 不为 null 则通过它获取。对应 Activity 由于配置更改导致重建的情况，此时就可以获取到上一个 Activity 保存的 ViewModelStore 实例
- 直接初始化 ViewModelStore 实例返回。对应 Activity 正常被启动的情况

这里只要看第一种情况即可

```java
    private ViewModelStore mViewModelStore;

    @NonNull
    @Override
    public ViewModelStore getViewModelStore() {
        if (getApplication() == null) {
            throw new IllegalStateException("Your activity is not yet attached to the "
                    + "Application instance. You can't request ViewModel before onCreate call.");
        }
        if (mViewModelStore == null) {
            NonConfigurationInstances nc =
                    (NonConfigurationInstances) getLastNonConfigurationInstance();
            if (nc != null) {
                // Restore the ViewModelStore from NonConfigurationInstances
                mViewModelStore = nc.viewModelStore;
            }
            if (mViewModelStore == null) {
                mViewModelStore = new ViewModelStore();
            }
        }
        return mViewModelStore;
    }
```

NonConfigurationInstances 是 ComponentActivity 的一个静态内部类，其内部就包含了一个 ViewModelStore 成员变量，在 Activity 被重建时，其对应的 ViewModelStore 就被保存在了这里

```java
    static final class NonConfigurationInstances {
        Object custom;
        ViewModelStore viewModelStore;
    }
```

通过查找引用，可以找到 ComponentActivity 就是在 `onRetainNonConfigurationInstance()` 方法里来完成 `NonConfigurationInstances.viewModelStore` 变量的赋值。从该方法名可以猜出，该方法就用于获取非配置项实例，以便在后续重建 Activity 时恢复数据

```java
    @Override
    @Nullable
    @SuppressWarnings("deprecation")
    public final Object onRetainNonConfigurationInstance() {
        // Maintain backward compatibility.
        Object custom = onRetainCustomNonConfigurationInstance();

        ViewModelStore viewModelStore = mViewModelStore;
        if (viewModelStore == null) {
            //如果 Activity 在第一次被重建后还未调用过 getViewModelStore() 方法
            //此时 mViewModelStore 就还是为 null
            //之后又发生了第二次重建，那就主动调用 getLastNonConfigurationInstance() 来获取第一次重建时保存的 ViewModelStore 实例
            
            // No one called getViewModelStore(), so see if there was an existing
            // ViewModelStore from our last NonConfigurationInstance
            NonConfigurationInstances nc =
                    (NonConfigurationInstances) getLastNonConfigurationInstance();
            if (nc != null) {
                viewModelStore = nc.viewModelStore;
            }
        }

        if (viewModelStore == null && custom == null) {
            return null;
        }
		
        NonConfigurationInstances nci = new NonConfigurationInstances();
        nci.custom = custom;
        //将 viewModelStore 打包带走
        nci.viewModelStore = viewModelStore;
        return nci;
    }
```

通过查找方法引用，可以知道 `onRetainNonConfigurationInstance()` 又是被父类 `android.app.Activity` 的以下方法所调用的

```java
	NonConfigurationInstances retainNonConfigurationInstances() {
        //拿到子类需要保存的数据
        Object activity = onRetainNonConfigurationInstance();
        
        HashMap<String, Object> children = onRetainNonConfigurationChildInstances();
        FragmentManagerNonConfig fragments = mFragments.retainNestedNonConfig();

        // We're already stopped but we've been asked to retain.
        // Our fragments are taken care of but we need to mark the loaders for retention.
        // In order to do this correctly we need to restart the loaders first before
        // handing them off to the next activity.
        mFragments.doLoaderStart();
        mFragments.doLoaderStop(true);
        ArrayMap<String, LoaderManager> loaders = mFragments.retainLoaderNonConfig();

        if (activity == null && children == null && fragments == null && loaders == null
                && mVoiceInteractor == null) {
            return null;
        }

        NonConfigurationInstances nci = new NonConfigurationInstances();
        //保存起来
        nci.activity = activity;
        nci.children = children;
        nci.fragments = fragments;
        nci.loaders = loaders;
        if (mVoiceInteractor != null) {
            mVoiceInteractor.retainInstance();
            nci.voiceInteractor = mVoiceInteractor;
        }
        return nci;
    }
```

> 从以上流程可以看出 Activity 的一些设计理念。由于  `android.app.Activity` 的逻辑是和特定的系统版本 SDK 关联的，我们无法决定用户手中的手机系统版本。而我们日常开发中都是选择直接继承于`androidx.appcompat.app.AppCompatActivity`，它又是作为一个依赖库来存在的，开发者可以自行决定要使用哪个版本号。所以，`android.app.Activity` 就将**非配置项实例数据**均当做一个 Object 实例，由子类通过实现`onRetainNonConfigurationInstance()` 方法来返回，父类 Activity 不限制方法返回值需要特定类型，不同的子类可以返回不同的类型，父类只负责在需要的时候将实例保存起来，然后在重建时返回给子类即可，由子类自己来进行数据的拆解和重建。这样，不管用户使用的手机是哪个系统版本，都可以保证在更新依赖库时有最大的发挥余地

再来看下 `retainNonConfigurationInstances()` 方法是在哪里调用的

通过搜索，可以找到在 `ActivityThread` 类的以下方法存在调用，该方法用于回调 Activity 的 `onDestroy` 方法，在回调前会先将数据保存到 `ActivityClientRecord` 的 `lastNonConfigurationInstances` 字段中

```java
	/** Core implementation of activity destroy call. */
    ActivityClientRecord performDestroyActivity(IBinder token, boolean finishing,
            int configChanges, boolean getNonConfigInstance, String reason) {
        ActivityClientRecord r = mActivities.get(token);
        ···
            if (getNonConfigInstance) {
                try {
                    //保存 Activity 返回的 NonConfigurationInstances
                    r.lastNonConfigurationInstances
                            = r.activity.retainNonConfigurationInstances();
                } catch (Exception e) {
                    if (!mInstrumentation.onException(r.activity, e)) {
                        throw new RuntimeException(
                                "Unable to retain activity "
                                + r.intent.getComponent().toShortString()
                                + ": " + e.toString(), e);
                    }
                }
            }
            
        ···
        //调用 Activity 的 onDestroy 方法
        mInstrumentation.callActivityOnDestroy(r.activity);    
        ···
        return r;
    }
```

在重新启动 Activity 时，又会将数据  attach 到新的 Activity 实例上，将其作为 `getLastNonConfigurationInstance()` 方法的返回值，从而完成了数据的交接

```java
	/**  Core implementation of activity launch. */
    private Activity performLaunchActivity(ActivityClientRecord r, Intent customIntent) {
       	···
        Activity activity = null;
        try {
            java.lang.ClassLoader cl = appContext.getClassLoader();
            activity = mInstrumentation.newActivity(
                    cl, component.getClassName(), r.intent);
            StrictMode.incrementExpectedActivityCount(activity.getClass());
            r.intent.setExtrasClassLoader(cl);
            r.intent.prepareToEnterProcess();
            if (r.state != null) {
                r.state.setClassLoader(cl);
            }
        } catch (Exception e) {
            if (!mInstrumentation.onException(activity, e)) {
                throw new RuntimeException(
                    "Unable to instantiate activity " + component
                    + ": " + e.toString(), e);
            }
        }
        
        ···
            
        //将 r.lastNonConfigurationInstances 传递进去
        activity.attach(appContext, this, getInstrumentation(), r.token,
                        r.ident, app, r.intent, r.activityInfo, title, r.parent,
                        r.embeddedID, r.lastNonConfigurationInstances, config,
                        r.referrer, r.voiceInteractor, window, r.configCallback,
                        r.assistToken);
        ···
        return activity;
    }
```

### 三、在什么时候被回收？

要知道 ViewModel 是在何时回收的，那么就只要看 ViewModelStore 是在什么时候清空 HashMap 就可以了

通过查找方法引用，可以发现是在 ComponentActivity 中调用了 ViewModelStore 的 `clear()` 方法。Activity 在收到 `ON_DESTROY` 事件时，如果判断到是由于配置项更改导致了 Activity 被销毁，那么就不会调用 `getViewModelStore().clear()` 。如果是正常退出 Activity 的话，就会导致 `getViewModelStore().clear()` 方法被调用，这样就可以清空掉所有缓存的 ViewModel 实例了

```java
  public ComponentActivity() {
        ···
        getLifecycle().addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(@NonNull LifecycleOwner source,
                    @NonNull Lifecycle.Event event) {
                if (event == Lifecycle.Event.ON_DESTROY) {
                    // Clear out the available context
                    mContextAwareHelper.clearAvailableContext();
                    // And clear the ViewModelStore
                    if (!isChangingConfigurations()) {
                        getViewModelStore().clear();
                    }
                }
            }
        });
	    ···
  }
```

```java
public class ViewModelStore {

    private final HashMap<String, ViewModel> mMap = new HashMap<>();

	···
        
    /**
     *  Clears internal storage and notifies ViewModels that they are no longer used.
     */
    public final void clear() {
        for (ViewModel vm : mMap.values()) {
            vm.clear();
        }
        mMap.clear();
    }
}
```

### 四、构造函数该如何调用？

ViewModelProvider 提供的 Factory 接口实现类有两个：

- NewInstanceFactory。用于通过反射初始化包含**无参构造函数**的 ViewModel
- AndroidViewModelFactory。用于通过反射初始化包含**参数仅有一个且为 Application 类型的构造函数**的 ViewModel

如果想要通过其它类型的构造函数来初始化 ViewModel 的话，就需要我们自己来实现 `ViewModelProvider.Factory` 接口完成初始化逻辑了

```java
/**
 * 作者：CZY
 * 时间：2020/9/17 14:07
 * 描述：
 * GitHub：https://github.com/leavesC
 */
class MainActivity : AppCompatActivity() {

    private val myViewModelA by lazy {
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MyViewModel(10) as T
            }
        }).get(
            MyViewModel::class.java
        ).apply {
            nameLiveData.observe(this@MainActivity, {

            })
        }
    }

    private val myViewModelB by lazy {
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MyViewModel(20) as T
            }
        }).get(
            MyViewModel::class.java
        ).apply {
            nameLiveData.observe(this@MainActivity, {

            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.e("myViewModelA", myViewModelA.toString() + " age: " + myViewModelA.age)
        Log.e("myViewModelB", myViewModelB.toString() + " age: " + myViewModelB.age)
    }

}

class MyViewModel(val age: Int) : ViewModel() {

    val nameLiveData = MutableLiveData<String>()

}
```

需要注意的是，虽然 myViewModelA 和 myViewModelB 都有各自不同的入参参数，但从日志输出结果来看它们其实都对应同一个内存地址，即最先初始化的那个 ViewModel 实例会被缓存下来重复使用

```java
 E/myViewModelA: github.leavesc.demo.MyViewModel@e24ac80 age: 10
 E/myViewModelB: github.leavesc.demo.MyViewModel@e24ac80 age: 10
```

之所以会出现以上情况，是因为在初始化 myViewModelA 和 myViewModelB 的时候它们默认对应的都是同个 Key，所以在初始化  myViewModelB 的时候就直接把之前已经初始化好的 myViewModelA 给返回了

```java
    @NonNull
    @MainThread
    public <T extends ViewModel> T get(@NonNull String key, @NonNull Class<T> modelClass) {
        ViewModel viewModel = mViewModelStore.get(key);
		
        if (modelClass.isInstance(viewModel)) {
            if (mFactory instanceof OnRequeryFactory) {
                ((OnRequeryFactory) mFactory).onRequery(viewModel);
            }
            //如果 mViewModelStore 里已经缓存了同个 key，且 value 也对应相同的 Class 类型，那么就直接返回 value 
            return (T) viewModel;
        } else {
            //noinspection StatementWithEmptyBody
            if (viewModel != null) {
                // TODO: log a warning.
            }
        }
        if (mFactory instanceof KeyedFactory) {
            viewModel = ((KeyedFactory) mFactory).create(key, modelClass);
        } else {
            viewModel = mFactory.create(modelClass);
        }
        mViewModelStore.put(key, viewModel);
        return (T) viewModel;
    }
```

如果希望 myViewModelA 和 myViewModelB 对应不同的实例对象，那么就需要在初始化的时候主动为它们指定**不同的 Key**，这样它们就可以一起被存到 ViewModelStore 的 HashMap 中

```java
    private val myViewModelA by lazy {
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MyViewModel(10) as T
            }
        }).get(
            "keyA", MyViewModel::class.java
        ).apply {
            nameLiveData.observe(this@MainActivity, {

            })
        }
    }

    private val myViewModelB by lazy {
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MyViewModel(20) as T
            }
        }).get(
            "keyB", MyViewModel::class.java
        ).apply {
            nameLiveData.observe(this@MainActivity, {

            })
        }
    }
```

```java
E/myViewModelA: github.leavesc.demo.MyViewModel@e24ac80 age: 10
E/myViewModelB: github.leavesc.demo.MyViewModel@9abd6fe age: 20
```

### 五、初始化陷阱

看以下代码，观察当应用启动时日志的输出结果

```java
/**
 * 作者：CZY
 * 时间：2020/9/17 14:07
 * 描述：
 * GitHub：https://github.com/leavesC
 */
class MainActivity : AppCompatActivity() {

    private val aViewModel by lazy {
        ViewModelProvider(this).get(
            "myKey", AViewModel::class.java
        )
    }

    private val bViewModel by lazy {
        ViewModelProvider(this).get(
            "myKey", BViewMode::class.java
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.e("aViewModel", aViewModel.toString())
        Log.e("bViewModel", bViewModel.toString())
        Log.e("MainActivity", "onCreate")
    }

}

class AViewModel() : ViewModel() {

    override fun onCleared() {
        super.onCleared()
        Log.e("AViewModel", "onCleared")
    }

}

class BViewMode : ViewModel() {

    override fun onCleared() {
        super.onCleared()
        Log.e("BViewMode", "onCleared")
    }

}
```

日志的输出会比较反直觉：AViewModel 在刚被初始化不久就被回收了，而此时 MainActivity 才刚执行到 onCreate 函数

```java
E/aViewModel: github.leavesc.demo.AViewModel@3c93503
E/AViewModel: onCleared
E/bViewModel: github.leavesc.demo.BViewMode@e24ac80
E/MainActivity: onCreate
```

造成这一结果的原因是因为 aViewModel 和 bViewModel 都使用了同个 key，这导致在将 ViewModel 存到 HashMap 的时候就会覆盖并回收掉旧值

```java
public class ViewModelStore {

    private final HashMap<String, ViewModel> mMap = new HashMap<>();

    final void put(String key, ViewModel viewModel) {
        ViewModel oldViewModel = mMap.put(key, viewModel);
        if (oldViewModel != null) {
            //存在旧值的话就将其回收
            oldViewModel.onCleared();
        }
    }

}
```

所以，对于不同类型的 ViewModel 实例，在初始化的时候不能指定相同的 Key