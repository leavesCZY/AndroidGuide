上篇文章介绍了关于 LiveData 类的源码解析，本篇文章再来介绍下 LiveData 的一系列衍生类及衍生方法

本文所讲的的源代码基于以下依赖库当前最新的 release 版本：

```groovy
	compileSdkVersion 29

    implementation "androidx.lifecycle:lifecycle-livedata:2.2.0"
    implementation "androidx.lifecycle:lifecycle-livedata-core:2.2.0"
```

### 一、LiveData 的子类

先来介绍下 LiveData 的几个子类

LiveData 的 `setValue()` 和 `postValue()` 方法的访问权限都是 `protected`，因此我们在日常开发中基本都是使用其子类

#### 1、MutableLiveData

MutableLiveData 的源码很简单，只是将 `setValue()` 和 `postValue()` 方法的访问权限提升为了 `public`，从而让外部可以直接调用这两个方法

```java
public class MutableLiveData<T> extends LiveData<T> {

    /**
     * Creates a MutableLiveData initialized with the given {@code value}.
     *
     * @param value initial value
     */
    public MutableLiveData(T value) {
        super(value);
    }

    /**
     * Creates a MutableLiveData with no value assigned to it.
     */
    public MutableLiveData() {
        super();
    }

    @Override
    public void postValue(T value) {
        super.postValue(value);
    }

    @Override
    public void setValue(T value) {
        super.setValue(value);
    }
}
```

#### 2、MediatorLiveData

MediatorLiveData 是 MutableLiveData 的子类，源码也比较简单，总的也就一百行不到。MediatorLiveData 既可用于将其它 LiveData 作为数据源来进行监听，也可将其作为普通的 MutableLiveData 进行 setValue 和 postValue 

这里先来看个 MediatorLiveData 的简单用法示例。假设有一个 EditText 用于输入用户名，同时需要在界面上回显用户名的长度，此时就可以用 MediatorLiveData 将**用户名（String）**转换为我们需要的数据类型 **Int**

```kotlin
    private val nameLiveData = MutableLiveData<String>()

    private val nameLengthLiveData = MediatorLiveData<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //将 nameLiveData 作为数据源
        //只要 nameLiveData 的数据发生变化 nameLengthLiveData 就能收到通知
        nameLengthLiveData.addSource(nameLiveData) { name ->
            nameLengthLiveData.value = name.length
        }
        nameLengthLiveData.observe(this, Observer {
            Log.e("TAG", "name length: $it")
        })

        btn_print.setOnClickListener {
            nameLiveData.value = Random.nextInt(1, 200).toString()
        }
    }
```

先来看下其 addSource 方法。主要逻辑是将**外部数据源 source** 以及对应的**数据监听者 onChanged** 包装为 Source 对象，然后检查 source 对象和 onChanged 对象是否已经缓存在 mSources 内部，避免重复添加数据源及 Observer。但是，MediatorLiveData 允许通过多次调用 addSource 方法来添加多个不同的数据源，这使得我们可以将不同的数据渠道（例如：本地数据库缓存、网络请求结果等）进行汇总，最后再统一从一个出口进行分发

需要注意的是，MediatorLiveData 不允许对同个数据源添加多个 Observer 对象，因为就用法而言，开发者是会在 Observer 中将从数据源接收到的值经过一系列逻辑计算后再传递给 MediatorLiveData ，这个转换过程只需要一次即可，所以添加多个 Observer 对象是没有实际意义的

```java
    private SafeIterableMap<LiveData<?>, Source<?>> mSources = new SafeIterableMap<>();
	
    @MainThread
    public <S> void addSource(@NonNull LiveData<S> source, @NonNull Observer<? super S> onChanged) {
        Source<S> e = new Source<>(source, onChanged);
        Source<?> existing = mSources.putIfAbsent(source, e);
        if (existing != null && existing.mObserver != onChanged) {
        	//走到这一步，说明之前已经传进来过同个 source 对象
        	//但当时传的 Observer 对象与本地传递的 Observer 对象不是同一个
         	//直接抛出异常
            throw new IllegalArgumentException(
                    "This source was already added with the different observer");
        }
        if (existing != null) {
            //走到此步，说明之间已经用相同的 source 和 onChanged 对象调用过 addSource 方法
            //所以直接返回
            return;
        }
        //如果 MediatorLiveData 当前有处于活跃状态的 Observer 对其进行监听
        //则调用 Source 对象的 plug() 函数
        if (hasActiveObservers()) {
            e.plug();
        }
    }

	//移除对数据源的监听行为
	@MainThread
    public <S> void removeSource(@NonNull LiveData<S> toRemote) {
        Source<?> source = mSources.remove(toRemote);
        if (source != null) {
            source.unplug();
        }
    }
```

再来看下 Source 类的定义。主要是对外部数据源的 mLiveData 的数据变化进行中转转发，并开放了两个用于增减 Observer 的方法

```java
	 private static class Source<V> implements Observer<V> {
        final LiveData<V> mLiveData;
        final Observer<? super V> mObserver;
        int mVersion = START_VERSION;

        Source(LiveData<V> liveData, final Observer<? super V> observer) {
            mLiveData = liveData;
            mObserver = observer;
        }
		
        //对外部数据源 mLiveData 进行监听
        void plug() {
            mLiveData.observeForever(this);
        }

        //移除对外部数据源 mLiveData 的监听
        void unplug() {
            mLiveData.removeObserver(this);
        }

        @Override
        public void onChanged(@Nullable V v) {
            if (mVersion != mLiveData.getVersion()) {
                mVersion = mLiveData.getVersion();
                mObserver.onChanged(v);
            }
        }
    }
```

此外，为了做到性能最优化，当外部所有 Observer 都移除了对 MediatorLiveData 的监听行为时，MediatorLiveData 会主动移除对数据源的监听行为，因此此时即使对数据源进行监听，其结果值也没有接收者，所以此时继续对数据源进行监听是没有意义的，而这也是为了避免内存泄露。此外，当有 Observer 对 MediatorLiveData 进行监听时，也会触发其开始对数据源进行监听

```java
    @CallSuper
    @Override
    protected void onActive() {
        for (Map.Entry<LiveData<?>, Source<?>> source : mSources) {
            source.getValue().plug();
        }
    }

    @CallSuper
    @Override
    protected void onInactive() {
        for (Map.Entry<LiveData<?>, Source<?>> source : mSources) {
            source.getValue().unplug();
        }
    }
```

以上就是 MediatorLiveData 的所有源码介绍，只要先理解了 LiveData 的内部实现原理，就可以很快明白 MediatorLiveData 的整个事件回调流程

### 二、Transformations

Transformations 类是 `lifecycle-livedata` 库提供的一个工具类型的方法类，提供了三个静态方法用于简化对 MediatorLiveData 的使用，这里再来依次介绍下

#### 1、map

`map(LiveData<X> , Function<X, Y>)` 函数用于简化向 MediatorLiveData 添加数据源的过程。大多数情况下，我们在使用 MediatorLiveData 时就是先将**数据源类型 X** 转换我们的**目标数据类型 Y**，然后再通过 `setValue(Y)` 进行数据回调。map 函数将这个数据类型转换过程抽象为了接口 `Function<I, O>`，将  `setValue(Y)` 过程隐藏在了 map 函数内部

```java
    @MainThread
    @NonNull
    public static <X, Y> LiveData<Y> map(
            @NonNull LiveData<X> source,
            @NonNull final Function<X, Y> mapFunction) {
        final MediatorLiveData<Y> result = new MediatorLiveData<>();
        result.addSource(source, new Observer<X>() {
            @Override
            public void onChanged(@Nullable X x) {
                result.setValue(mapFunction.apply(x));
            }
        });
        return result;
    }

    public interface Function<I, O> {
        /**
        * Applies this function to the given input.
        *
        * @param input the input
        * @return the function result.
        */
        O apply(I input);
    }
```

#### 2、switchMap

switchMap 函数的逻辑相对来说会比较绕，在某些逻辑计算结果是通过 LiveData 来进行传递的情况下（比如 Room 数据库就支持将查询结果以 LiveData 的形式来返回）会比较有用。下面通过假设一个现实需求来理解其作用会更为简单

假设当前需要你来实现一个通过用户名来查询所有匹配的用户列表的功能，通过向数据库或者网络请求等耗时的方式来获得匹配结果，为了避免阻塞主线程，需要将这个匹配过程放在子线程来完成，主线程通过回调的方式来取得结果

首先，假设有一个 UserDataSource 提供了 `getUsersWithNameLiveData(String)`方法用于请求匹配结果，并通过 LiveData 作为返回值来传递请求结果。switchMap 函数内部也使用到了 MediatorLiveData，将 nameQueryLiveData 作为数据源，每当 `setNameQuery(String)`方法修改了用户名时，switchMap 就能收到更新通知，然后自动触发 `getUsersWithNameLiveData(String)` 函数来进行请求。最终外部只要监听 `getUsersWithNameLiveData()`函数的返回值即可得到最终的请求结果，而不必理会 ViewModel 内部究竟是通过什么方法来取得结果值

```kotlin
    class UserViewModel : ViewModel() {

        val nameQueryLiveData = MutableLiveData<String>()

        lateinit var userDataSource: UserDataSource

        fun getUsersWithNameLiveData(): LiveData<List<String>> {
            return Transformations.switchMap(nameQueryLiveData) { name ->
                return@switchMap userDataSource.getUsersWithNameLiveData(name)
            }
        }

        fun setNameQuery(name: String) {
            nameQueryLiveData.value = name
        }

    }

    interface UserDataSource {

        fun getUsersWithNameLiveData(name: String): LiveData<List<String>>

    }
```

理解了以上的需求后，再来看 switchMap 的实现逻辑就会简单许多，switchMap 也只是将对数据源的监听行为以及数据的变换过程给封装了起来而已，在某些特殊情况下（指结果以 LiveData 的形式来返回）多多少少也为开发者节省了代码量

```java
	@MainThread
    @NonNull
    public static <X, Y> LiveData<Y> switchMap(
            @NonNull LiveData<X> source,
            @NonNull final Function<X, LiveData<Y>> switchMapFunction) {
        final MediatorLiveData<Y> result = new MediatorLiveData<>();
        //先内部构建一个 MediatorLiveData，然后将 source 作为其数据源
        result.addSource(source, new Observer<X>() {
            
            //缓存每次的请求结果
            LiveData<Y> mSource;

            @Override
            public void onChanged(@Nullable X x) {
                //触发外界根据请求值 x 获得 LiveData 结果值的逻辑
                //对应上面例子的 getUsersWithNameLiveData 方法
                //这个过程是惰性的，即只有数据源 source 发生了变化才会触发请求
                LiveData<Y> newLiveData = switchMapFunction.apply(x);
                if (mSource == newLiveData) {
                    //如果 newLiveData 之前已经拿到了，则不必重复监听其回调结果
                    //直接返回即可
                    return;
                }
                if (mSource != null) {
                    //新值到来，移除对旧值的监听
                    result.removeSource(mSource);
                }
                mSource = newLiveData;
                if (mSource != null) {
                    result.addSource(mSource, new Observer<Y>() {
                        @Override
                        public void onChanged(@Nullable Y y) {
                            //等到拿到了getUsersWithNameLiveData 的请求结果后
                            //就将结果值回调出去
                            result.setValue(y);
                        }
                    });
                }
            }
        });
        return result;
    }
```

#### 3、distinctUntilChanged

`distinctUntilChanged()` 函数用于过滤掉连续重复的回调值，只有本次的回调结果和上次不一致，本次的回调值才被认为是有效的

```java
    @MainThread
    @NonNull
    public static <X> LiveData<X> distinctUntilChanged(@NonNull LiveData<X> source) {
        final MediatorLiveData<X> outputLiveData = new MediatorLiveData<>();
        outputLiveData.addSource(source, new Observer<X>() {
			
            //用于是否是第一次收到回调值
            boolean mFirstTime = true;

            @Override
            public void onChanged(X currentValue) {
                final X previousValue = outputLiveData.getValue();
                //等式成立的条件一共有三种，满足其一即可
                //1. 是第一次收到回调值，即 mFirstTime 为 true
                //2. 上次的回调值为 null，本次的回调值不为 null
                //3. 上次的回调值不为 null，且与本次的回调值不相等
                if (mFirstTime
                        || (previousValue == null && currentValue != null)
                        || (previousValue != null && !previousValue.equals(currentValue))) {
                    mFirstTime = false;
                    outputLiveData.setValue(currentValue);
                }
            }
        });
        return outputLiveData;
    }
```

### 三、ComputableLiveData

ComputableLiveData 是 `lifecycle-livedata` 库下的类，虽然命名上带有 LiveData，但实际上并没有直接继承于任何类和接口。ComputableLiveData 可以说是提供了一种更加安全地执行耗时任务的思路，其特点是：带有生命周期监听、响应式的触发耗时任务、以 LiveData 作为中介获取任务执行结果

先来看个简单的使用示例，明白其使用方法。假设当前需要实现一个对指定图片进行压缩，将压缩后的图片显示到 ImageView 上的功能，此时就需要考虑到以下几点：

- 压缩图片是一个耗时过程，需要放到子线程来完成
- 压缩图片这个过程不能同时触发多次，需要保证这个过程的原子性
- 需要通过回调的方式在主线程取得压缩结果
- 当页面退出时，此时需要能够取消回调，避免内存泄露及 NPE 问题

基于以上几点，用 ComputableLiveData 来实现会非常简单，它都提供了对以上几点的保障

通过对 ComputableLiveData 内部的 liveData 进行监听，就可以自动触发 ComputableLiveData 内的线程池来执行耗时任务，并最终在主线程得到任务的执行结果，且由于可以传入 LifecycleOwner 对象，也有了生命周期安全的保障

```kotlin
class CompressImgLiveData(private val filePath: String) : ComputableLiveData<Bitmap>() {

    override fun compute(): Bitmap {
        //执行耗时任务....
        return compress()
    }

    private fun compress(): Bitmap {
        TODO()
    }

}

 val compressImgLiveData = CompressImgLiveData("sdcard/xxxx.jpg")
 compressImgLiveData.liveData.observe(LifecycleOwner, Observer { bitmap ->
     //获取到执行结果 bitmap
 })
```

可以看到，ComputableLiveData 封装了大部分的处理逻辑，仅开放出了一个 `compute()`函数由外部来实现耗时任务的执行体，对使用者来说十分方便

这里再来具体介绍下 ComputableLiveData 的实现逻辑

一共有四个全局变量。为了保证耗时任务只能同时由一个线程来执行，所以使用到了两个 AtomicBoolean 变量来标记耗时任务的执行状态，避免在多线程情况下出现读写竞争的情况，保证 `compute()`函数的原子性

```java
    //用于执行耗时任务的线程池
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Executor mExecutor;

    //用于触发耗时任务并接收耗时任务的执行结果
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final LiveData<T> mLiveData;

    //用于标记当前的耗时任务结果值是否已经过时，已过时则值为 true
    //外部通过调用 invalidate() 函数将耗时任务置为过时状态
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final AtomicBoolean mInvalid = new AtomicBoolean(true);

    //用于标记当前是否正在执行耗时任务
    //正在执行状态则值为 true
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final AtomicBoolean mComputing = new AtomicBoolean(false);
```

有两个构造函数。主要是开放了由外部传入线程池对象的入口。当外部对 mLiveData 进行监听的 Observer 数量从无到有时，将自动触发执行 mRefreshRunnable 

```java
	/**
     * Creates a computable live data that computes values on the arch IO thread executor.
     */
    @SuppressWarnings("WeakerAccess")
    public ComputableLiveData() {
        //使用 Jetpack 提供的默认线程池
        this(ArchTaskExecutor.getIOThreadExecutor());
    }

    /**
     * Creates a computable live data that computes values on the specified executor.
     *
     * @param executor Executor that is used to compute new LiveData values.
     */
    @SuppressWarnings("WeakerAccess")
    public ComputableLiveData(@NonNull Executor executor) {
        //可以自定义线程池
        mExecutor = executor;
        mLiveData = new LiveData<T>() {
            @Override
            protected void onActive() {
                //当外部对 LiveData 进行监听的 Observer 数量从无到有时
                //则触发执行耗时任务
                mExecutor.execute(mRefreshRunnable);
            }
        };
    }
```

耗时任务的执行是放在 **mRefreshRunnable** 内部，通过两个 AtomicBoolean 变量来标记 `compute()`的执行状态，并将任务体放在 while 循环内部，在任务过时的时候自动重新执行

```java
	@VisibleForTesting
    final Runnable mRefreshRunnable = new Runnable() {
        @WorkerThread
        @Override
        public void run() {
            boolean computed;
            do {
                computed = false;
                // compute can happen only in 1 thread but no reason to lock others.

                //1. 如果 mComputing 为 false，则将其置为 true。意味着当前没有在执行 compute() 函数，从而使得等式成立，进入 if 内部
                //   从而限制了耗时任务只能同时在一个线程下执行
                //2. 如果 mComputing 为 true ，意味着当前正在执行 compute() 函数，等式不成立，直接跳出 do while 循环，避免阻塞当前线程
                if (mComputing.compareAndSet(false, true)) {
                    // as long as it is invalid, keep computing.
                    try {
                        T value = null;
                        //1. 当 mInvalid 为 true 时，则将其置为 false，等式成立，进入 while 内部，开始执行耗时任务
                        //2. 当 mInvalid 为 false 时，等式不成立，跳出 while 循环

                        //由于将 mInvalid 置为 false 的操作只会出现在这里，所以第二种情况只会出现在进入 while 循环后触发
                        while (mInvalid.compareAndSet(true, false)) {
                            computed = true;
                            value = compute();
                        }
                        //如果 computed 为 true，则意味着 compute() 执行完成，且执行过程中没有抛出异常
                        //此时就直接向外部回调结果值
                        if (computed) {
                            mLiveData.postValue(value);
                        }
                    } finally {
                        // release compute lock
                        //释放状态锁，将当前置为非计算状态
                        mComputing.set(false);
                    }
                }
                // check invalid after releasing compute lock to avoid the following scenario.
                // Thread A runs compute()
                // Thread A checks invalid, it is false
                // Main thread sets invalid to true
                // Thread B runs, fails to acquire compute lock and skips
                // Thread A releases compute lock
                // We've left invalid in set state. The check below recovers.

                //当 computed 和 mInvalid 均为 true 时，则重新开始一轮循环
                //会出现这种情况，意味着 compute() 函数执行成功，但外部将其置为已过时状态，需要重新执行一次 compute() 函数
            } while (computed && mInvalid.get());
        }
    };
```

当外部认定 `compute()`函数的结果值已失效时，可以通过 `invalidate()` 函数来触发任务体重新执行。当 **mInvalidationRunnable** 被执行时，mInvalid 的值一共有两种情况：

1. mInvalid 值为 true。if 等式不成立，说明此时 `compute()` 还未开始执行，或者之前已经被置为过时状态了（compute() 将自动重新执行），此时直接 return 即可
2. mInvalid 值为 false。mInvalid 将被置为 true，if 等式成立，此时 mRefreshRunnable 的执行状态分为几种
   - mRefreshRunnable 已执行完毕。此时只要 **isActive** 为 true，则将重新触发执行 mRefreshRunnable
   - mRefreshRunnable 还处于执行中，且`while (mInvalid.compareAndSet(true, false))`正在执行中。由于将 mInvalid 修改为了 true，则将导致 while 循环重新执行一次，从而达到重新触发 `compute()` 的目的
   - mRefreshRunnable 还处于执行中，`while (mInvalid.compareAndSet(true, false))`已执行完毕，但还未执行到 `while (computed && mInvalid.get())` 语句。此时将 mInvalid 修改为了 true，则将导致 `while (computed && mInvalid.get())`  等式成立， while 循环重新执行一次，从而达到重新触发 `compute()` 的目的

```java
    // invalidation check always happens on the main thread
    @VisibleForTesting
    final Runnable mInvalidationRunnable = new Runnable() {
        @MainThread
        @Override
        public void run() {
            //判断当前外部是否有对 LiveData 进行监听
            boolean isActive = mLiveData.hasActiveObservers();
            //如果 mInvalid 为 false，则将其置为 true，等式成立
            if (mInvalid.compareAndSet(false, true)) {
                //只在当前外部有对 LiveData 进行监听的情况下才触发耗时任务
                if (isActive) {
                    mExecutor.execute(mRefreshRunnable);
                }
            }
        }
    };

    /**
     * Invalidates the LiveData.
     * <p>
     * When there are active observers, this will trigger a call to {@link #compute()}.
     */
    //将当前的耗时任务置为过时状态
    //当外部有 Observer 对 LiveData 进行监听，则将重新触发执行耗时任务
    public void invalidate() {
        ArchTaskExecutor.getInstance().executeOnMainThread(mInvalidationRunnable);
    }
```

需要由子类实现的抽象方法，即任务的执行体

```java
    //由外部实现耗时任务的具体逻辑
    // TODO https://issuetracker.google.com/issues/112197238
    @SuppressWarnings({"WeakerAccess", "UnknownNullness"})
    @WorkerThread
    protected abstract T compute();
```