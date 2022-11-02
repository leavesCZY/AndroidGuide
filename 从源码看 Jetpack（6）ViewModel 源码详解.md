> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

> Google Jetpack 自从推出以后，极大地改变了 Android 开发者们的开发模式，并降低了开发难度。这也要求我们对当中一些子组件的实现原理具有一定的了解，所以我就打算来写一系列 Jetpack 源码解析的文章，希望对你有所帮助 🤣🤣🤣

ViewModel 是 Jetpack 整个家族体系内最为基础的组件之一，基本是按照如下方式来进行初始化和使用的：

- ViewModelStoreOwner（Activity/Fragment）通过 ViewModelProvider 来得到一个 ViewModel 实例
- 通过和 LifecycleOwner 绑定的方式来监听 LiveData 数据的变化从而做出各种响应
- 当 Activity 由于意外情况被销毁重建时，Activity 依然能拿到同个 ViewModel 实例，并依靠之前已经保存的数据来进行状态还原，这也是 ViewModel 最大的特点和优势

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
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

下面就来通过提问的方式来拆解 ViewModel 的各个知识点，一步步介绍其实现原理，基于以下版本来进行讲解

```kotlin
compileSdkVersion 30
implementation 'androidx.appcompat:appcompat:1.3.0-beta01'
implementation "androidx.lifecycle:lifecycle-viewmodel:2.3.0"
```

# 一、如何初始化

在上面的例子中，我们并没有看到 ViewModel 是如何进行初始化的，也没有手动调用 ViewModel 的构造函数来创建 ViewModel 实例，这是因为这个操作都隐藏在了 ViewModelProvider 内部，由 ViewModelProvider 自己来通过反射构建出 ViewModel 实例

ViewModelProvider 一共包含三个构造函数，可以看到，不管是哪种方式，最终都是要拿到两个构造参数：ViewModelStore 和 Factory，且都不能为 null

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

AppCompatActivity 的父类 `androidx.activity.ComponentActivity` 已经实现了 ViewModelStoreOwner 和 HasDefaultViewModelProviderFactory 两个接口，所以我们可以直接使用只包含一个参数的构造函数，而如果传入的 ViewModelStoreOwner 实例没有继承 HasDefaultViewModelProviderFactory 接口的话，`mFactory` 就使用 NewInstanceFactory 来初始化

Factory 是 ViewModelProvider 的内部接口，用于实现初始化 ViewModel 的逻辑。例如，NewInstanceFactory 就通过反射来实例化 ViewModel 实例，但是也只适用于不包含构造参数的情况，如果是有参构造函数的话就需要我们来主动实现 Factory 接口，毕竟构造参数也需要我们来主动传入

```java
public interface Factory {
    @NonNull
    <T extends ViewModel> T create(@NonNull Class<T> modelClass);
}

public static class NewInstanceFactory implements Factory {

    private static NewInstanceFactory sInstance;

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

ComponentActivity 的 `getDefaultViewModelProviderFactory()` 方法返回的是 SavedStateViewModelFactory，它和 Jetpack 的另外一个组件 `SavedStateHandle` 有关，在下一篇文章中会介绍，在这里 SavedStateViewModelFactory 起的作用就和 NewInstanceFactory 完全一样

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

既然 Factory 实例也有了，下一步就是来调用 `ViewModelProvider(this).get()` 方法了。`get()` 方法需要我们传入 Class 对象，ViewModelProvider 需要拿到 Class 才能完成反射操作。在此方法里主要是通过 modelClass 来自动生成一个字符串 Key，并将参数转发给另外一个 `get()` 方法

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

可以看出来，以下方法会通过 key 从 ViewModelStore 里取 ViewModel 实例，如果取不到值或者是取出来的值类型不符，则会通过 `mFactory.create(modelClass)` 方法来反射初始化 ViewModel，并在返回初始化结果前将它存到 `mViewModelStore` 中，这样就完成了 ViewModel 的初始化流程了

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

# 二、如何保持不变

Activity 每次获取 ViewModel 实例都会先尝试从 `mViewModelStore` 中取值，只有在取不到值的时候才会去重新构建一个新的 ViewModel 实例，且构建后的 ViewModel 实例也会被保存在`mViewModelStore` 中。那既然 Activity 可以在页面销毁重建的情况下获取到之前的 ViewModel 实例，那么不也就间接说明了在这种情况下 ViewModelStore 也是一直被保留着而没有被回收吗？

所以，想要知道 ViewModel 是如何保持不变的，那就看 ViewModelStore 实例是如何被保留不被回收就可以了

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

由于 AppCompatActivity 的父类 `androidx.activity.ComponentActivity` 已经实现了 ViewModelStoreOwner 接口，所以也相当于每个 AppCompatActivity 都持有了一个 ViewModelStore 实例

```java
public interface ViewModelStoreOwner {
    @NonNull
    ViewModelStore getViewModelStore();
}
```

ComponentActivity 的 `getViewModelStore()` 方法获取 ViewModelStore 实例的来源有两种：

- 如果 NonConfigurationInstances 不为 null 则通过它获取。对应 Activity 由于配置更改导致重建的情况，NonConfigurationInstances 当中就保留了页面重建过程中被保留下来的数据，此时就可以获取到上一个 Activity 保存的 ViewModelStore 实例了
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
    ensureViewModelStore();
    return mViewModelStore;
}

@SuppressWarnings("WeakerAccess") /* synthetic access */
void ensureViewModelStore() {
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
    Object custom = onRetainCustomNonConfigurationInstance();
    ViewModelStore viewModelStore = mViewModelStore;
    if (viewModelStore == null) {
        //如果 Activity 在第一次被重建后还未调用过 getViewModelStore() 方法，此时 mViewModelStore 就还是为 null
        //之后又发生了第二次重建，那就主动调用 getLastNonConfigurationInstance() 来获取第一次重建时保存的 ViewModelStore 实例
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

通过查找方法引用，可以知道 `onRetainNonConfigurationInstance()` 又是被父类 `android.app.Activity` 的以下方法所调用，由父类去负责保留 NonConfigurationInstances 对象

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

> 从以上流程可以看出 Activity 的一些设计思路。由于  `android.app.Activity` 的逻辑是和特定的系统版本 SDK 关联的，我们无法决定用户手中的手机系统版本。而我们日常开发中都是选择直接继承于`androidx.appcompat.app.AppCompatActivity`，它又是作为一个依赖库来存在的，开发者可以自行决定要使用哪个版本号，Google 官方也可能随时推出新版本。所以，`android.app.Activity` 就将**非配置项实例数据**均当做一个 Object 实例来处理，由子类通过实现`onRetainNonConfigurationInstance()` 方法来返回，父类 Activity 不限制方法返回值需要特定类型，不同的子类可以返回不同的类型，父类只负责在需要的时候将实例保存起来，然后在重建时返回给子类即可，由子类自己来进行数据的拆解和重建。这样，不管用户使用的手机是哪个系统版本，都可以保证三方依赖库有最大的发挥余地

再来看下 `retainNonConfigurationInstances()` 方法是在哪里调用的

通过搜索，可以找到在 ActivityThread 类的以下方法存在调用，该方法用于回调 Activity 的 `onDestroy` 方法，在回调前会先将数据保存到 ActivityClientRecord 的 `lastNonConfigurationInstances` 字段中

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

在重新启动 Activity 时，又会将数据  `attach` 到新的 Activity 实例上，将其作为 `getLastNonConfigurationInstance()` 方法的返回值。通过这种数据交接，重建前的 ViewModelStore 实例就会被重建后的 Activity 拿到，当中就保留了重建前 Activity 初始化的所有 ViewModel 实例，从而保障了 ViewModel 实例的不变性

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

# 三、如何调用构造函数

ViewModelProvider 提供的 Factory 接口实现类有两个：

- NewInstanceFactory。通过反射来实例化 ViewModel，适用于包含无参构造函数的情况
- AndroidViewModelFactory。通过反射来实例化 ViewModel，适用于构造参数只有一个，且参数类型为 Application 的情况

如果想要通过其它类型的构造函数来初始化 ViewModel 的话，就需要我们自己来实现 `ViewModelProvider.Factory` 接口声明初始化逻辑了，就像以下这样

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
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

需要注意的是，虽然 `myViewModelA` 和 `myViewModelB` 都有各自不同的入参参数，但从日志输出结果来看它们其实是同一个对象，即最先初始化的那个 ViewModel 实例会被缓存下来重复使用

```kotlin
 E/myViewModelA: github.leavesc.demo.MyViewModel@e24ac80 age: 10
 E/myViewModelB: github.leavesc.demo.MyViewModel@e24ac80 age: 10
```

之所以会出现以上情况，是因为在初始化 `myViewModelA` 和 `myViewModelB` 的时候它们默认对应的都是同个 Key，ViewModelProvider 默认情况下是以 `DEFAULT_KEY + ":" + canonicalName` 作为 key 值来从 `mViewModelStore` 中取值，所以在初始化 `myViewModelB` 的时候就直接把之前已经初始化好的 `myViewModelA` 给返回了

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

如果希望 `myViewModelA` 和 `myViewModelB` 对应不同的实例对象，那么就需要在初始化的时候主动为它们**指定不同的 Key**，这样它们就可以一起被存到 ViewModelStore 的 HashMap 中了

```kotlin
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

```kotlin
E/myViewModelA: github.leavesc.demo.MyViewModel@e24ac80 age: 10
E/myViewModelB: github.leavesc.demo.MyViewModel@9abd6fe age: 20
```

# 四、什么时候回收

要知道 ViewModel 是在何时回收的，那么就只要看 ViewModelStore 是在什么时候清空 HashMap 就可以了

通过查找方法引用，可以发现是在 ComponentActivity 中调用了 ViewModelStore 的 `clear()` 方法。Activity 在收到 `ON_DESTROY` 事件时，如果判断到是由于配置项更改导致了 Activity 被销毁，那么就不会调用 `getViewModelStore().clear()` 。如果是正常退出 Activity，那就会调用 `getViewModelStore().clear()` 方法，这样就会清空掉所有缓存的 ViewModel 实例了，ViewModel 的 `clear()` 方法也同时会被调用

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

# 五、初始化陷阱

看以下代码，观察当应用启动时日志的输出结果

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
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

日志的输出会比较反直觉：`aViewModel` 在刚被初始化不久就被回收了，而此时 MainActivity 才刚执行到 `onCreate` 方法

```kotlin
E/aViewModel: github.leavesc.demo.AViewModel@3c93503
E/AViewModel: onCleared
E/bViewModel: github.leavesc.demo.BViewMode@e24ac80
E/MainActivity: onCreate
```

之所以造成这一结果，就是因为 `aViewModel` 和 `bViewModel` 都使用了同个 key，这就导致了在将 `bViewModel` 存到 HashMap 的时候就会覆盖并回收掉旧值 `aViewModel` 

```kotlin
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

所以，对于不同类型的 ViewModel，在初始化的时候不能指定相同的 Key