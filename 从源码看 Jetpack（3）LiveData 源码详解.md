> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

> Google Jetpack 自从推出以后，极大地改变了 Android 开发者们的开发模式，并降低了开发难度。这也要求我们对当中一些子组件的实现原理具有一定的了解，所以我就打算来写一系列 Jetpack 源码解析的文章，希望对你有所帮助 🤣🤣🤣

LiveData 是 Jetpack 的基础组件之一，在很多模块中都可以看到其身影。LiveData 可以和生命周期绑定，当 Activity 和 Fragment 处于活跃状态时才进行数据回调，并在 Lifecycle 处于销毁状态（DESTROYED）时自动移除数据监听行为，从而避免了常见的内存泄露和 NPE 问题

本文就来介绍下 LiveData 的内部实现源码，让读者能了解其实现原理和以下几点比较容易忽略的重要特性：

- 一个 Observer 对象只能和一个 Lifecycle 对象绑定，否则将抛出异常
- 同个 Observer 对象不能同时使用 observe 和 observeForever 方法，否则将抛出异常
- 存在丢值的可能性。如果连续 postValue，最终可能只有最后一个值能够被保留并回调
- 存在仅有部分 Observer 收到了回调，其它 Observer 又没有的可能性。当单线程连续传值或者多线程同时传值时，假设是先后传 valueA 和 valueB，最终可能只有部分 Observer 接收到了 valueA，所有 Observer 都接收到了 valueB

本文内容基于以下版本来进行讲解

```groovy
implementation "androidx.lifecycle:lifecycle-livedata:2.2.0"
```

# 一、LiveData

LiveData 包含两个用于添加 Observer 的方法：

- observe (LifecycleOwner , Observer)  
- observeForever (Observer)

两个方法的区别只在于是否提供了生命周期安全的保障

## observe

`observe(LifecycleOwner , Observer)` 方法传入的 LifecycleOwner 参数意味着携带了 Lifecycle 对象，LiveData 内部会判断 Lifecycle 是否处于活跃状态，是的话才会进行数据回调，在 Lifecycle 对象处于 DESTROYED 状态时也会自动移除 Observer，这是 LiveData 避免内存泄漏的重要基础

`observe` 方法会对外部传入的 Observer 进行去重校验。如果之前已经用同个 Observer 对象调用了此方法且 LifecycleOwner 不是同一个对象，则会直接抛出异常，即一个 Observer 只允许和单个 LifecycleOwner 进行绑定。因为如果允许一个 Observer 同时和多个不同的 LifecycleOwner 进行绑定的话，这会导致当 LiveData 数据发生变化时，处于 RESUMED 状态的 LifecycleOwner 和即将处于 DESTROYED 状态的另一个 LifecycleOwner 都收到了数据回调，而这破坏了 `observe` 方法所期望的生命周期安全

```java
@MainThread
public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
    //限定只能在主线程调用 observe 方法
    assertMainThread("observe");
    //当 Lifecycle 已经处于 DESTROYED 状态时，此时进行 observe 是没有意义的，直接返回
    if (owner.getLifecycle().getCurrentState() == DESTROYED) {
        // ignore
        return;
    }
    //根据传入参数构建一个新的代理 Observer
    LifecycleBoundObserver wrapper = new LifecycleBoundObserver(owner, observer);
    //将 observer 作为 key，wrapper 作为 value 进行存储
    //当 mObservers 不包含该 key 时，调用 putIfAbsent 会返回 null
    //当 mObservers 已包含该 key 时，调用 putIfAbsent 不会存储 key-value，并会返回之前保存的 value
    ObserverWrapper existing = mObservers.putIfAbsent(observer, wrapper);
    if (existing != null && !existing.isAttachedTo(owner)) {
        //走到此步，说明之前 LiveData 内部已经持有了 observer 对象，且该 observer 对象已经绑定了其它的 LifecycleOwner 对象
        //此时直接抛出异常
        throw new IllegalArgumentException("Cannot add the same observer"
                + " with different lifecycles");
    }
    if (existing != null) {
        //observer 之前已经和同个 owner 一起传进来过了，此处直接返回
        return;
    }
    owner.getLifecycle().addObserver(wrapper);
}
```

上面的代码使用到了 LifecycleBoundObserver，它是抽象类 ObserverWrapper 的实现类。ObserverWrapper 用于包装外部传进来的 Observer 对象，为子类定义好特定的抽象方法和共用逻辑，主要是提供了共用的状态分发方法

```java
private abstract class ObserverWrapper {

    //外部传进来的对 LiveData 进行数据监听的 Observer
    final Observer<? super T> mObserver;

    //用于标记 mObserver 是否处于活跃状态
    boolean mActive;

    //用于标记 Observer 内最后一个被回调的 value 的新旧程度
    int mLastVersion = START_VERSION;

    ObserverWrapper(Observer<? super T> observer) {
        mObserver = observer;
    }

    //用于获取当前 Lifecycle 是否处于活跃状态
    abstract boolean shouldBeActive();

    //用于判断 mObserver 是否和 LifecycleOwner（即 Lifecycle）有绑定关系
    boolean isAttachedTo(LifecycleOwner owner) {
        return false;
    }

    //移除 mObserver
    void detachObserver() {
    }

    void activeStateChanged(boolean newActive) {
        if (newActive == mActive) {
            return;
        }
        // immediately set active state, so we'd never dispatch anything to inactive
        // owner
        mActive = newActive;
        //判断当前 LiveData 所有的 Observer 是否都处于非活跃状态
        boolean wasInactive = LiveData.this.mActiveCount == 0;
        //更新 LiveData 当前所有处于活跃状态的 Observer 的数量
        LiveData.this.mActiveCount += mActive ? 1 : -1;
        if (wasInactive && mActive) {
            //如果 LiveData 处于活跃状态的 Observer 数量从 0 变成了 1,
            //则回调 onActive 方法
            onActive();
        }
        if (LiveData.this.mActiveCount == 0 && !mActive) {
            //如果 LiveData 处于活跃状态的 Observer 数量从 1 变成了 0,
            //则回调 onInactive 方法
            onInactive();
        }
        if (mActive) {
            //如果 mObserver 变成了活跃状态，则向其回调新值
            dispatchingValue(this);
        }
    }
}
```

ObserverWrapper 一共有两个子类：LifecycleBoundObserver 和 AlwaysActiveObserver，两者的差别就在于是否和生命周期相绑定

LifecycleBoundObserver 也实现了 LifecycleEventObserver 接口，从而可以收到 Lifecycle 的每次生命周期事件切换时的事件回调。其整个事件流程是这样的：

1. Lifecycle 的生命周期发生变化，从而回调 onStateChanged 方法
2. onStateChanged 方法首先判断 Lifecycle 是否已处于 DESTROYED 状态，是的话则直接移除 Observer，整个回调流程结束，否则继续以下流程
3. onStateChanged 通过 activeStateChanged 方法来判断 Lifecycle 是否从非活跃状态切换到了活跃状态，是的话则调用 dispatchingValue 方法来分发值，dispatchingValue 方法会根据 ObserverWrapper 内部的 mLastVersion 来判断是否有新值需要向外部 Observer 进行回调，是的话则向其回调新值，否则结束流程

```java
class LifecycleBoundObserver extends ObserverWrapper implements LifecycleEventObserver {
    @NonNull
    final LifecycleOwner mOwner;

    LifecycleBoundObserver(@NonNull LifecycleOwner owner, Observer<? super T> observer) {
        super(observer);
        mOwner = owner;
    }

    @Override
    boolean shouldBeActive() {
        //只有当 Lifecycle 的当前状态是 STARTED 或者 RESUMED 时
        //才认为 Lifecycle 是处于活跃状态
        return mOwner.getLifecycle().getCurrentState().isAtLeast(STARTED);
    }

    //LifecycleEventObserver 的实现方法
    //当 Lifecycle 的生命周期状态发生变化时就会调用此方法
    @Override
    public void onStateChanged(@NonNull LifecycleOwner source,
            @NonNull Lifecycle.Event event) {
        //如果 Lifecycle 已经处于 DESTROYED 状态了,则主动移除 mObserver
        //这就是 LiveData 可以避免内存泄露最重要的一个点
        if (mOwner.getLifecycle().getCurrentState() == DESTROYED) {
            removeObserver(mObserver);
            return;
        }
        activeStateChanged(shouldBeActive());
    }

    @Override
    boolean isAttachedTo(LifecycleOwner owner) {
        return mOwner == owner;
    }

    @Override
    void detachObserver() {
        //移除 mObserver
        mOwner.getLifecycle().removeObserver(this);
    }
}
```

## observeForever

`observeForever` 方法则不会考虑外部所处的生命周期状态，只要数据发生变化了就会进行数据回调，因此该方法是非生命周期安全的

```java
@MainThread
public void observeForever(@NonNull Observer<? super T> observer) {
    //限定只能在主线程调用 observe 方法
    assertMainThread("observeForever");
    AlwaysActiveObserver wrapper = new AlwaysActiveObserver(observer);
    ObserverWrapper existing = mObservers.putIfAbsent(observer, wrapper);
    if (existing instanceof LiveData.LifecycleBoundObserver) {
        //会走到这一步，是因为之前已经先用该 observer 对象调用了 observe(LifecycleOwner,Observer)
        //这里直接抛出异常
        throw new IllegalArgumentException("Cannot add the same observer"
                + " with different lifecycles");
    }
    if (existing != null) {
        //observer 之前已经和同个 owner 一起传进来过了，此处直接返回
        return;
    }
    //主动触发 activeStateChanged 方法，因为当前 LiveData 可能已经被设置值了
    wrapper.activeStateChanged(true);
}
```

上面代码使用到了 AlwaysActiveObserver，它也是抽象类 ObserverWrapper 的实现类，其 `shouldBeActive`方法固定返回 true，意味着只要有数据变化都会进行回调，所以 `observeForever` 方法要由开发者来主动移除 Observer，避免内存泄露和 NPE

```java
private class AlwaysActiveObserver extends ObserverWrapper {

    AlwaysActiveObserver(Observer<? super T> observer) {
        super(observer);
    }

    @Override
    boolean shouldBeActive() {
        //使其固定返回 true，则意味着只要有数据变化就都进行数据回调
        return true;
    }
}
```

## removeObserver

LiveData 开放了两个方法用于添加 Observer ，那么自然会有 `removeObserver` 的方法

```java
//移除指定的 Observer 对象
@MainThread
public void removeObserver(@NonNull final Observer<? super T> observer) {
    assertMainThread("removeObserver");
    ObserverWrapper removed = mObservers.remove(observer);
    if (removed == null) {
        return;
    }
    removed.detachObserver();
    removed.activeStateChanged(false);
}

//通过循环遍历移除所有和特定 LifecycleOwner 绑定的 Observer 对象
@SuppressWarnings("WeakerAccess")
@MainThread
public void removeObservers(@NonNull final LifecycleOwner owner) {
    assertMainThread("removeObservers");
    for (Map.Entry<Observer<? super T>, ObserverWrapper> entry : mObservers) {
        if (entry.getValue().isAttachedTo(owner)) {
            removeObserver(entry.getKey());
        }
    }
}
```

# 二、更新值

更新 LiveData 值的方法一共有两个，分别是：

- setValue(T value)
- postValue(T value)

## setValue

`setValue` 方法被限定在只能主线程进行调用

```java
private volatile Object mData;

private int mVersion;

@MainThread
protected void setValue(T value) {
    assertMainThread("setValue");
    //更新当前 value 的版本号，即 value 的新旧程度
    mVersion++;
    mData = value;
    dispatchingValue(null);
}
```

`dispatchingValue()` 方法设计得比较巧妙，用两个全局的布尔变量 `mDispatchingValue` 和 `mDispatchInvalidated` 就实现了**新旧值判断、旧值舍弃、新值重新全局发布**的逻辑。其中需要注意 `mObservers` 的遍历过程，由于每遍历一个 item 都会检查一次当前的 value 是否已经过时，是的话则中断遍历，所以会存在仅有部分 Observer 收到了旧值的情况

```java
//用于标记当前是否正处于向 mObservers 发布 value 的过程
private boolean mDispatchingValue;
//用于标记当前正在发布的 value 是否已经失效
//在 value 还未向所有 Observer 发布完成的时候，新 value 已经到来，此时旧 value 就是处于失效状态
@SuppressWarnings("FieldCanBeLocal")
private boolean mDispatchInvalidated;

//initiator 为 null 则说明需要遍历回调整个 mObservers
//initiator 不为 null 则说明仅回调 initiator 本身
@SuppressWarnings("WeakerAccess") /* synthetic access */
void dispatchingValue(@Nullable ObserverWrapper initiator) {
    if (mDispatchingValue) {
        //如果 mDispatchingValue 为 true，说明当前正处于向 mObservers 发布 mData 的过程中
        //而 dispatchingValue 方法只会在主线程进行调用，所以会出现 mDispatchingValue 为 true 的情况
        //说明 Observer 的 onChanged 方法内部又主动向 LiveData setValue
        //则将 mDispatchInvalidated 置为 true，用于标明有新值到来，正在回调的值是已经过时的了
        mDispatchInvalidated = true;
        return;
    }
    //用于标记当前正处于向 mObservers 发布 mData 的过程中
    mDispatchingValue = true;
    do {
        mDispatchInvalidated = false;
        if (initiator != null) {
            considerNotify(initiator);
            initiator = null;
        } else {
            for (Iterator<Map.Entry<Observer<? super T>, ObserverWrapper>> iterator =
                    mObservers.iteratorWithAdditions(); iterator.hasNext(); ) {
                considerNotify(iterator.next().getValue());
                if (mDispatchInvalidated) {
                    //如果 mDispatchInvalidated 为 true，则中断继续遍历过程
                    //用新值来重新循环一遍
                    break;
                }
            }
        }
    } while (mDispatchInvalidated);
    mDispatchingValue = false;
}
```

```java
@SuppressWarnings("unchecked")
private void considerNotify(ObserverWrapper observer) {
    //如果 observer 处于非活跃状态，则直接返回
    if (!observer.mActive) {
        return;
    }
    //此处判断主要是为了照顾 LifecycleBoundObserver
    //由于 Lifecycle 有可能状态值 State 已经切换到了非活跃状态，但 LifecycleBoundObserver 还未收到事件通知
    //所以为了避免意外情况，此处主动检查 observer 的活跃状态并判断是否需要更新其活跃状态
    if (!observer.shouldBeActive()) {
        observer.activeStateChanged(false);
        return;
    }
    //根据 observer 本部的 value 版本号 mLastVersion 来决定是否需要向其进行回调
    //为了避免重复向某个 observer 回调值，所以此处需要判断下
    if (observer.mLastVersion >= mVersion) {
        return;
    }
    observer.mLastVersion = mVersion;
    observer.mObserver.onChanged((T) mData);
}
```

## postValue

`postValue` 方法不限定调用者所在线程，不管是主线程还是子线程都可以调用，因此是存在多线程竞争的可能性的，`postValue` 方法的重点就在于需要理解其从子线程切换到主线程之间的状态变化

由于 LiveData 值回调的行为是会固定放在主线程完成的，所以 `postValue` 方法将值回调的逻辑放到  Runnable 中再 Post 给 Handler，最终交由主线程来执行，因此从调用`postValue` 方法到 Runnable 被执行之间是会有段时间差的，此时其它线程可能又调用了`setValue/postValue` 方法传递了新值

在 `mPostValueRunnable` 被执行前，所有通过 `postValue` 方法传递的 value 都会被保存到变量 `mPendingData` 上，且只会保留最后一个，直到 `mPostValueRunnable` 被执行后 `mPendingData` 才会被重置，所以使用 `postValue` 方法在多线程同时调用或者单线程连续调用的情况下是存在丢值（外部的 Observer 只能接收到最新值）的可能性的

```java
@SuppressWarnings("WeakerAccess") /* synthetic access */
final Object mDataLock = new Object();

@SuppressWarnings("WeakerAccess") /* synthetic access */
//mPendingData 的默认值
//当 mPendingData 等于 NOT_SET 时说明当前 LiveData 没有值需要通过 postValue 回调
static final Object NOT_SET = new Object();        

volatile Object mPendingData = NOT_SET;

//用于在主线程对值进行回调
private final Runnable mPostValueRunnable = new Runnable() {
    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        Object newValue;
        synchronized (mDataLock) {
            //通过加锁可以确保 newValue 指向的是当前最新值
            newValue = mPendingData;
            //重置 mPendingData
            mPendingData = NOT_SET;
        }
        setValue((T) newValue);
    }
};

protected void postValue(T value) {
    boolean postTask;
    //加锁以保证 mPendingData 值能够一直指向最新值
    synchronized (mDataLock) {
        postTask = mPendingData == NOT_SET;
        mPendingData = value;
    }
    //如果 postTask 为 false，则说明当前有旧值需要通过 postValue 进行回调
    //因为 postValue 可以在子线程调用，而 Observer 的 onChanged(value) 方法肯定是要在主线程被调用
    //从子线程切到主线程之间是有段时间间隔的
    //等到 mPostValueRunnable 真正执行时让其直接发送最新值 mPendingData 即可，所以此处直接返回
    if (!postTask) {
        return;
    }
    //向主线程发送一个 runnable，主要是为了在子线程调用 postValue，在主线程进行值回调
    ArchTaskExecutor.getInstance().postToMainThread(mPostValueRunnable);
}
```

# 三、判断是否是新值

再来介绍下 LiveData 是如何判断是否需要向 Observer 回调值的。之所以需要进行这个判断而不能每次接收到新值时都直接进行回调，这是基于以下两个原因的：

1. `observeForever` 方法是只要接收到 value 就会马上运行回调逻辑， 与 `observe` 方法根据 Lifecycle 的变化再来进行回调的时机的先后顺序具有不确定性，所以需要判断进行回调的 value 对于 Observer 来说是否是新值，避免重复回调
2. 外部可能在不同阶段先后调用了多次 `observe` 方法或者 `observeForever` 方法，此时也需要仅在没有对 Observer 传过值的情况下进行回调，避免重复回调

LiveData 在其构造方法内部就开始了新旧值的记录，主要是根据一个整数 `mVersion` 来记录当前 value 的版本号，即新旧程度

```java
static final int START_VERSION = -1;

private int mVersion;

/
 * Creates a LiveData initialized with the given {@code value}.
 *
 * @param value initial value
 */
public LiveData(T value) {
    mData = value;
    mVersion = START_VERSION + 1;
}

/
 * Creates a LiveData with no value assigned to it.
 */
public LiveData() {
    mData = NOT_SET;
    mVersion = START_VERSION;
}
```

而 `mVersion` 的改变只会在 `setValue` 方法接收到新值时才会递增加一，从而表明所有 Observer 内部的 data 均已过时，需要重新回调。由于 `postValue` 方法最终还是会调用 `setValue` 方法来完成回调逻辑，所以只需要看 `setValue` 方法即可

```java
@MainThread
protected void setValue(T value) {
    assertMainThread("setValue");
    mVersion++;
    mData = value;
    dispatchingValue(null);
}
```

当对 Observer 进行回调时，就需要先判断下 value 对于 Observer 来说是否是新值，非新值则直接返回，是的话则先保存当前 value 的版本号 `mVersion` 再进行回调

```java
@SuppressWarnings("unchecked")
private void considerNotify(ObserverWrapper observer) {
    //如果 observer 处于非活跃状态，则直接返回
    if (!observer.mActive) {
        return;
    }
    //此处判断主要是为了照顾 LifecycleBoundObserver
    //由于 Lifecycle 有可能状态值 State 已经切换到了非活跃状态，但 LifecycleBoundObserver 还未收到事件通知
    //所以为了避免意外情况，此处主动检查 observer 的活跃状态并判断是否需要更新其活跃状态
    if (!observer.shouldBeActive()) {
        observer.activeStateChanged(false);
        return;
    }
    //根据 observer 本部的 value 版本号 mLastVersion 来决定是否需要向其进行回调
    //为了避免重复向某个 observer 回调值，所以此处需要判断下
    if (observer.mLastVersion >= mVersion) {
        return;
    }
    observer.mLastVersion = mVersion;
    observer.mObserver.onChanged((T) mData);
}
```

# 四、改进 LiveData

LiveData 只会在 LifecycleOwner 处于活跃状态的时候才进行事件分发，这是 LiveData 的优点， 但同时也会给我们带来一些使用上的困扰，我通过复刻 LiveData 的源码对其进行了自定义，更多详情请看：[Jetpack LiveData 的设计理念及改进](https://juejin.cn/post/6903096576734920717)

