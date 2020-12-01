本文所讲的的源代码基于以下依赖库当前最新版本：

```groovy
    compileSdkVersion 30

    implementation 'androidx.appcompat:appcompat:1.3.0-alpha02'
    implementation "androidx.lifecycle:lifecycle-viewmodel-savedstate:2.3.0-alpha07"
    implementation "androidx.savedstate:savedstate:1.1.0-alpha01"
```

### 一、Activity 的重建

我们知道，Activity 被意外销毁的情况可以分为两种：

1. 由于屏幕旋转等配置更改的原因导致 Activity 被销毁
2. 由于系统资源限制导致 Activity 被销毁

对于以上两种情况，我们当然希望 Activity 重建时之前**加载的数据**以及**用户状态**都能够得到恢复，每种情况目前都有着不同的恢复方法

对于第一种情况，Jetpack 提供了 ViewModel 这个组件来解决这个问题。ViewModel 可以在屏幕旋转后继续存留，适合用于在内存中存储比较复杂或者量比较大的数据，例如：用 RecyclerView 加载的多个列表项对应的 Data。但当第二种情况发生时 ViewModel 是无法被保留下来的，此后当用户重新回到 Activity 时，只会得到一个全新的 ViewModel 实例并且也需要重新加载列表数据。关于 ViewModel 的源码详解可以看我的这篇文章：[从源码看 Jetpack（6）-ViewModel源码详解](https://juejin.im/post/6873356946896846856)

对于第二种情况，需要依赖于 Activity 原生提供的数据保存及恢复机制，即依赖以下两个方法来实现数据的保存和恢复逻辑。`onSaveInstanceState(Bundle)` 方法保存的数据在配置更改和 Activity 被意外杀死时都会被保留，但也有着存储容量和存取速度的限制。因为 Bundle 有着容量限制，不适合用于存储大量数据，且 `onSavedInstanceState(Bundle)` 方法会将数据序列化到磁盘，如果要保存的数据很复杂，序列化会消耗大量的内存和时间。所以 `onSaveInstanceState(Bundle)` 仅适合用于存储少量的简单类型的数据

- onSaveInstanceState(Bundle)。用于保存数据

- onCreate(Bundle?)  或者 onRestoreInstanceState(Bundle)。用于恢复数据

Google 官方也对这两种情况进行了对比：

![](https://s1.ax1x.com/2020/09/19/w5Xbuj.png)

对于第二种情况，因为数据的保存和恢复流程被限制在了 Activity 的特定方法里，我们无法直接在 ViewModel 中决定哪些数据需要被保留，也无法直接拿到恢复后的数据，使得整个重建流程和 ViewModel 分裂开了

为了解决这个问题，Jetpack 提供了 `SavedStateHandle` 这么一个组件，可以看做是对 ViewModel 的功能扩展，使得开发者可以直接在 ViewModel 中直接操作整个数据的重建过程

### 二、SavedStateHandle 如何使用

`SavedStateHandle` 的引入使得开发者无需直接使用 `onSaveInstanceState(Bundle)` 等方法来完成数据的重建，而只需要在 ViewModel 里来完成即可。其使用入门看以下例子

```java
/**
 * 作者：CZY
 * 时间：2020/9/19 15:21
 * 描述：
 * GitHub：https://github.com/leavesC
 */
class MainActivity : AppCompatActivity() {

    private val savedStateViewModel by lazy {
        ViewModelProvider(this).get(SavedStateViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        log("savedStateViewModel: $savedStateViewModel")
        log("savedStateViewModel.name: ${savedStateViewModel.nameLiveData.value}")
        log("savedStateViewModel.age: ${savedStateViewModel.ageLiveData.value}")
        log("onCreate")
        btn_test.setOnClickListener {
            savedStateViewModel.nameLiveData.value = "leavesC"
            savedStateViewModel.ageLiveData.value = 26
        }
    }

    private fun log(log: String) {
        Log.e("MainActivity", log)
    }

}

class SavedStateViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    companion object {

        private const val KEY_NAME = "keyName"

    }

    val nameLiveData = savedStateHandle.getLiveData<String>(KEY_NAME)

    val ageLiveData = MutableLiveData<Int>()

}
```

打开开发者模式中"不保留活动"的选项，以此来模拟 Activity 由于系统内存不足被销毁的情况

当 MainActivity 第一次启动时，两个 LiveData 中的值都是为 null

```java
E/MainActivity: savedStateViewModel: github.leavesc.demo.SavedStateViewModel@df3fa77
E/MainActivity: savedStateViewModel.name: null
E/MainActivity: savedStateViewModel.age: null
E/MainActivity: onCreate
```

点击按钮为这两个 LiveData 进行赋值，按 Home 键退出应用，此时 MainActivity 在后台就会被销毁。重新打开应用，此时就可以看到 ViewModel 其实已经是新的一个实例了，但通过 `SavedStateHandle` 构建的 `nameLiveData` 中还保留着上一次的赋值，而 `ageLiveData` 中的值就还是默认值 null

```java
E/MainActivity: savedStateViewModel: github.leavesc.demo.SavedStateViewModel@f5fa30c
E/MainActivity: savedStateViewModel.name: leavesC
E/MainActivity: savedStateViewModel.age: null
E/MainActivity: onCreate
```

以上例子就展示了 `SavedStateHandle` 在 Activity 被意外杀死时也可以保留数据的能力，使得我们可以直接在 ViewModel 里完成整个数据的重建逻辑，下面就来介绍下其内部实现原理

### 三、源码讲解

`SavedStateHandle` 其实也是通过封装 `onSaveInstanceState(Bundle)`等方法来实现的，其整个数据重建流程主要涉及以下几个类和接口：

1. SavedStateRegistryOwner
2. SavedStateRegistryController
3. SavedStateRegistry
4. SavedStateHandle

#### 1、SavedStateRegistryOwner

`SavedStateRegistryOwner` 是一个接口，用于标记其实现类（Activity/Fragment）拥有着数据重建的能力。`androidx.activity.ComponentActivity` 就继承了 `SavedStateRegistryOwner` 接口，相当于 `ComponentActivity` 及其子类都拥有一个 `SavedStateRegistry` 对象

```java
public interface SavedStateRegistryOwner extends LifecycleOwner {
    /**
     * Returns owned {@link SavedStateRegistry}
     *
     * @return a {@link SavedStateRegistry}
     */
    @NonNull
    SavedStateRegistry getSavedStateRegistry();
}
```

#### 2、SavedStateRegistryController

`ComponentActivity` 将数据的**保存和恢复**逻辑都转发给了 `SavedStateRegistryController`来处理：

- 在 `onCreate` 方法里通过调用 `performRestore` 来恢复数据
- 在 `onSaveInstanceState` 方法里通过调用 `performSave` 来保存数据

```java
public class ComponentActivity extends androidx.core.app.ComponentActivity implements
        ContextAware,
        LifecycleOwner,
        ViewModelStoreOwner,
        HasDefaultViewModelProviderFactory,
        SavedStateRegistryOwner,
        OnBackPressedDispatcherOwner,
        ActivityResultRegistryOwner,
        ActivityResultCaller {
        
    final SavedStateRegistryController mSavedStateRegistryController =
            SavedStateRegistryController.create(this);
            
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Restore the Saved State first so that it is available to
        // OnContextAvailableListener instances
        //恢复数据
        mSavedStateRegistryController.performRestore(savedInstanceState);
        ···
        super.onCreate(savedInstanceState);
        ···
    }
    
    @CallSuper
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        ···
        super.onSaveInstanceState(outState);
        //保存数据
        mSavedStateRegistryController.performSave(outState);
        ···
    }
    
    @NonNull
    @Override
    public final SavedStateRegistry getSavedStateRegistry() {
        return mSavedStateRegistryController.getSavedStateRegistry();
    }
        
}
```

#### 3、SavedStateRegistry

`SavedStateRegistryController` 又会将逻辑转交由 `SavedStateRegistry` 的同名方法来完成

```java
public final class SavedStateRegistryController {
    private final SavedStateRegistryOwner mOwner;
    private final SavedStateRegistry mRegistry;

    private SavedStateRegistryController(SavedStateRegistryOwner owner) {
        mOwner = owner;
        mRegistry = new SavedStateRegistry();
    }
    
	···
        
    @MainThread
    public void performRestore(@Nullable Bundle savedState) {
        Lifecycle lifecycle = mOwner.getLifecycle();
        
        //必须在 Activity 的 onCreate 阶段进行数据恢复
        if (lifecycle.getCurrentState() != Lifecycle.State.INITIALIZED) {
            throw new IllegalStateException("Restarter must be created only during "
                    + "owner's initialization stage");
        }
        lifecycle.addObserver(new Recreator(mOwner));
        mRegistry.performRestore(lifecycle, savedState);
    }

    @MainThread
    public void performSave(@NonNull Bundle outBundle) {
        mRegistry.performSave(outBundle);
    }
    
	···
}
```

##### 1、拿数据的入口

`SavedStateRegistry` 是实际进行保存和恢复数据的地方，那么很自然地，外部就需要有一个可以指定 `SavedStateRegistry` 从哪里拿数据的入口，这个入口就是 `registerSavedStateProvider` 方法

外部通过 `SavedStateRegistry` 的内部接口 `SavedStateProvider` 来返回需要保存的数据。`SavedStateProvider` 接口仅包含一个抽象方法，用于返回一个 Bundle 对象，里面就存储了要保存的数据。因为并不是所有 Activity 被销毁的情况都需要进行数据的保存和恢复操作，例如用户按返回键退出 Activity 的情况就不需要保存数据，所以 `saveState()` 方法仅会在需要的时候才会被调用

```java
    private SafeIterableMap<String, SavedStateProvider> mComponents =
            new SafeIterableMap<>();   
	
	//外部通过一个唯一标识 key 来和要保存的数据 Bundle 相对应，后续也通过这个 key 来恢复数据
	@MainThread
    public void registerSavedStateProvider(@NonNull String key,
            @NonNull SavedStateProvider provider) {
        SavedStateProvider previous = mComponents.putIfAbsent(key, provider);
        if (previous != null) {
            throw new IllegalArgumentException("SavedStateProvider with the given key is"
                    + " already registered");
        }
    }

	/**
     * This interface marks a component that contributes to saved state.
     */
    public interface SavedStateProvider {
        /**
         * Called to retrieve a state from a component before being killed
         * so later the state can be received from {@link #consumeRestoredStateForKey(String)}
         *
         * @return S with your saved state.
         */
        @NonNull
        Bundle saveState();
    }
```

##### 2、保存数据

既然已经指定了拿数据的入口，那么就来看下 `performSave` 是如何保存数据的，其主要逻辑是：

1. 如果在上一次重建 Activity 时保存下来的数据还未消费完，那么再次重建 Activity 时就将未消费完的数据也保存起来
2. 遍历所有需要保存的数据，将所有 Bundle 保存起来
3. 将所有需要保存的数据都存到 `onSaveInstanceState` 给的 Bundle 对象里

```java
   private static final String SAVED_COMPONENTS_KEY =
            "androidx.lifecycle.BundlableSavedStateRegistry.key";

    @Nullable
    private Bundle mRestoredState;

	@MainThread
    void performSave(@NonNull Bundle outBundle) {
        Bundle components = new Bundle();
        if (mRestoredState != null) {
            //步骤1
            components.putAll(mRestoredState);
        }
        //步骤2
        for (Iterator<Map.Entry<String, SavedStateProvider>> it =
                mComponents.iteratorWithAdditions(); it.hasNext(); ) {
            Map.Entry<String, SavedStateProvider> entry1 = it.next();
            components.putBundle(entry1.getKey(), entry1.getValue().saveState());
        }
        //步骤3
        outBundle.putBundle(SAVED_COMPONENTS_KEY, components);
    }
```

##### 3、恢复数据

再来看下 `performRestore` 是如何恢复数据的，其主要逻辑是：

1. 拿到 `performSave` 方法保存到 Bundle 里的数据
2. 通过监听 lifecycle 来确定当前是否处于可以恢复数据的生命周期阶段，用一个布尔变量 `mAllowingSavingState` 来标记

```java
	private boolean mRestored;

    boolean mAllowingSavingState = true;
    
    @MainThread
    void performRestore(@NonNull Lifecycle lifecycle, @Nullable Bundle savedState) {
        if (mRestored) {
            //不应该重复恢复数据
            throw new IllegalStateException("SavedStateRegistry was already restored.");
        }
        if (savedState != null) {
            //步骤1
            mRestoredState = savedState.getBundle(SAVED_COMPONENTS_KEY);
        }
		
        //步骤2
        lifecycle.addObserver(new GenericLifecycleObserver() {
            @Override
            public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
                if (event == Lifecycle.Event.ON_START) {
                    mAllowingSavingState = true;
                } else if (event == Lifecycle.Event.ON_STOP) {
                    mAllowingSavingState = false;
                }
            }
        });

        mRestored = true;
    }
```

##### 4、消费数据

数据被恢复了并不意味着 Activity 已经恢复到了被销毁前的状态，被恢复的数据还存在 Bundle 里，此时还需要开发者通过取**键值对**的方式来消费数据，将**用户数据或者 UI 状态**恢复到销毁前的状态。

消费数据的入口就是 `consumeRestoredStateForKey`方法，其主要逻辑是：通过使用和 `registerSavedStateProvider` 方法相同的 key 来取数据，并在取了之后将数据从 `mRestoredState` 中移除。如果所有数据都被消费了的话，那么就将 `mRestoredState` 置为 null，标记着本次的所有数据恢复流程已经结束

```java
    @MainThread
    @Nullable
    public Bundle consumeRestoredStateForKey(@NonNull String key) {
        if (!mRestored) {
            throw new IllegalStateException("You can consumeRestoredStateForKey "
                    + "only after super.onCreate of corresponding component");
        }
        if (mRestoredState != null) {
            Bundle result = mRestoredState.getBundle(key);
            mRestoredState.remove(key);
            if (mRestoredState.isEmpty()) {
                mRestoredState = null;
            }
            return result;
        }
        return null;
    }
```

#### 4、SavedStateHandle

`SavedStateRegistry` 封装了 Activity 层次进行存数据和恢复数据的逻辑，恢复后的数据也需要转交给 `SavedStateHandle`，因为 `SavedStateHandle` 是作为 ViewModel 的构造参数来使用的，我们在 ViewMode 中能直接接触到的都是 `SavedStateHandle`。`SavedStateHandle` 包含两个构造函数，`initialState` 参数就是在之前 Activity 被销毁时保留下来的数据，或者是为需要保留下来的数据设定的默认值

```java
public final class SavedStateHandle {
    
    final Map<String, Object> mRegular;
    
    /**
     * Creates a handle with the given initial arguments.
     */
    public SavedStateHandle(@NonNull Map<String, Object> initialState) {
        mRegular = new HashMap<>(initialState);
    }

    /**
     * Creates a handle with the empty state.
     */
    public SavedStateHandle() {
        mRegular = new HashMap<>();
    }
    
    ···
    
}
```

成员变量 `mRegular` 就用于存储在数据重建流程中要保存的数据，对于希望保留的的数据，我们可以通过两种方式来向 `mRegular`存数据：

1. 通过调用 `set(@NonNull String key, @Nullable T value)` 方法来实现，该方法就类似于 `Map.put(Key,Value)` 的方式来存值，但是这种方式并不具备数据变化通知
2. 通过 `MutableLiveData.setValue`的方式来存值。首先通过  `getLiveData(@NonNull String key)` 方法拿到和特定 Key 绑定的 LiveData 对象，之后向该 LiveData.setValue 时都会同时更新 `mRegular`

上述的第一种方式也包含了第二种方式的操作，因为每次转换而成的 LiveData 对象都会缓存到 `mLiveDatas` 里，当外部通过 `set(@NonNull String key, @Nullable T value) `更新键值对时也会尝试同时通知 `mLiveDatas` 

```java
    private final Map<String, SavingStateLiveData<?>> mLiveDatas = new HashMap<>();

	@MainThread
    @NonNull
    public <T> MutableLiveData<T> getLiveData(@NonNull String key) {
        return getLiveDataInternal(key, false, null);
    }

	@NonNull
    private <T> MutableLiveData<T> getLiveDataInternal(
            @NonNull String key,
            boolean hasInitialValue,
            @Nullable T initialValue) {
        MutableLiveData<T> liveData = (MutableLiveData<T>) mLiveDatas.get(key);
        if (liveData != null) {
            return liveData;
        }
        SavingStateLiveData<T> mutableLd;
        // double hashing but null is valid value
        //将 key-value 形式的数据转为 LiveData，以此进行数据监听
        if (mRegular.containsKey(key)) {
            mutableLd = new SavingStateLiveData<>(this, key, (T) mRegular.get(key));
        } else if (hasInitialValue) {
            mutableLd = new SavingStateLiveData<>(this, key, initialValue);
        } else {
            mutableLd = new SavingStateLiveData<>(this, key);
        }
        mLiveDatas.put(key, mutableLd);
        return mutableLd;
    }

    @MainThread
    public <T> void set(@NonNull String key, @Nullable T value) {
        validateValue(value);
        @SuppressWarnings("unchecked")
        MutableLiveData<T> mutableLiveData = (MutableLiveData<T>) mLiveDatas.get(key);
        if (mutableLiveData != null) {
            // it will set value;
            mutableLiveData.setValue(value);
        } else {
            mRegular.put(key, value);
        }
    }
```

我们在 ViewModel 层通过向 `mRegular`存取值，就是在决定一旦 Activity 被意外销毁后被重建时，需要恢复的数据有哪些。那么就再来看下 `mRegular`是如何最终被存到 Bundle 里的

`SavedStateHandle` 包含一个 `mSavedStateProvider` 成员变量，其内部就实现了遍历 `mRegular`包含的所有 Key 和 Value 并分别存到不同的 ArrayList 里，最终存到 Bundle 里的逻辑

```java
public final class SavedStateHandle {
    
    private final SavedStateProvider mSavedStateProvider = new SavedStateProvider() {
        @SuppressWarnings("unchecked")
        @NonNull
        @Override
        public Bundle saveState() {
            // Get the saved state from each SavedStateProvider registered with this
            // SavedStateHandle, iterating through a copy to avoid re-entrance
            Map<String, SavedStateProvider> map = new HashMap<>(mSavedStateProviders);
            for (Map.Entry<String, SavedStateProvider> entry : map.entrySet()) {
                Bundle savedState = entry.getValue().saveState();
                set(entry.getKey(), savedState);
            }
            // Convert the Map of current values into a Bundle
            Set<String> keySet = mRegular.keySet();
            ArrayList keys = new ArrayList(keySet.size());
            ArrayList value = new ArrayList(keys.size());
            for (String key : keySet) {
                //遍历过程中分别存到不同的 ArrayList
                keys.add(key);
                value.add(mRegular.get(key));
            }

            Bundle res = new Bundle();
            // "parcelable" arraylists - lol
            res.putParcelableArrayList("keys", keys);
            res.putParcelableArrayList("values", value);
            return res;
        }
    };
    
    @NonNull
    SavedStateProvider savedStateProvider() {
        return mSavedStateProvider;
    }

}
```

最终 `mSavedStateProvider` 又会被注册到 `SavedStateRegistry` 的 `mComponents` 对象里，在需要的时候通过调用 `mSavedStateProvider.saveState()` 拿到我们希望被保存的数据