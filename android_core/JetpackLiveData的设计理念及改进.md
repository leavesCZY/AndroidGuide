> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

# 一、架构指南

在日常的开发中，我们经常会讲到 MVC、MVP、MVVM 等多种开发模式，这其实都是**应用架构**的不同呈现方式，你目前又是使用的什么应用架构呢？

一个好的架构，其至少应该遵循两个原则

- 关注点分离。关注点分离指的是架构中的每一层应只专注于实现某一特定目的。一种常见的错误就是在 `Activity` 或 `Fragment` 中编写所有代码（例如，直接在界面层完成网络请求），这种基于界面的类应仅包含与系统和用户交互的逻辑，你应该使这些类尽可能保持精简，这样可以避免许多与生命周期相关的问题
- 通过模型驱动界面。模型是指负责处理应用数据的组件，模型应独立于应用的界面层和其它应用组件，这样才能不受应用的生命周期以及相关的关注点的影响

关于第一点。对于一个移动设备来说，其拥有的资源是固定且极其有限的，系统可能会随时终止某些应用进程以便为前台进程腾出内存空间。而且，即使是对于前台进程来说，我们也并非拥有`Activity` 和`Fragment`的完全所有权，系统也可能会随时因为**内存空间不足**或者**系统配置更改**等意外情况而销毁它们。做到关注点分离，就是将各个层次的职责划分开，避免将用户数据和界面的生命周期强绑定，这样当意外情况发生时用户数据才不会随之一起被销毁

关于第二点。通过模型驱动界面，即界面对模型来说应该是相当于一个观察者而独立存在，界面不应该直接持有数据。界面通过观察数据的变化来驱动自身，模型也可以通过改变自身来驱动界面更改。这里指的模型最好是**持久性模型**，即模型的生命周期需要比界面甚至应用进程更加长，典型代表即 Jetpack 中的 ViewModel 和 Room。这样当有意外情况发生时，用户也不会丢失数据，我们可以在随后就为用户恢复数据

Google 推荐的应用架构图如下所示

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbe9b55164e47ebb6c94ef60e9fe640~tplv-k3u1fbpfcp-zoom-1.image)

当中，每个组件仅依赖于其下一级的组件。ViewModel 就是**关注点分离原则**的一个具体实现，是作为用户数据的**承载体**和**处理者**而存在的，Activity/Fragment 仅依赖于 ViewModel，ViewModel 就用于响应界面层的输入和驱动界面层变化，Repository 用于为 ViewModel 提供一个单一的数据来源及数据存储域，Repository 可以同时依赖于持久性数据模型和远程服务器数据源

# 二、LiveData 的优势

本文想要讨论的就是 ViewModel 所包含的 LiveData

从 Google 官方推荐的应用架构图可以看到，[LiveData](https://developer.android.google.cn/topic/libraries/architecture/livedata) 是包含在 ViewModel 中的。LiveData 是一种可观察的数据存储器，Activity/Fragment 是观察者，LiveData 是被观察者。LiveData 具有生命周期感知能力，当我们向 LiveData 注册了一个和 LifecycleOwner 相绑定的 Observer 时，如果 LifecycleOwner 的生命周期处于[`STARTED`](https://developer.android.google.cn/reference/androidx/lifecycle/Lifecycle.State#STARTED) 或 [`RESUMED`](https://developer.android.google.cn/reference/androidx/lifecycle/Lifecycle.State#RESUMED) 状态，则认为该观察者当前处于活跃状态，此时 LiveData 才会向观察者发送事件通知，非活跃状态的观察者不会收到任何事件通知。且当 LifecycleOwner  的状态变为[`DESTROYED`](https://developer.android.google.cn/reference/androidx/lifecycle/Lifecycle.State#DESTROYED)时，LiveData 会自动解除和观察者之间的绑定关系，以防止内存泄漏和过多的内存消耗。所以说，LiveData 具有生命周期感知能力，Activity/Fragment 无需和 LiveData 创建明确且严格的依赖路径

ViewModel 和 LiveData 可以看做是对**关注点分离**和**通过模型驱动界面**原则的一个共同实现，ViewModel 提供了让用户数据独立于界面而存在的能力，LiveData 提供了安全地通知并驱动界面变化的能力

# 三、LiveData 的缺陷

LiveData 的设计初衷就决定了其具有以下三点缺陷（或者说特性）：

1. 只在 Observer 至少处于 STARTED 状态时才能收到事件通知。Activity 只有在 onStart 后和 onStop 前才能收到事件通知
2. LiveData 是**黏性**的。假设存在一个静态的 LiveData 变量，且已经包含了数据，对其进行监听的 Activity 都会收到其当前值的回调通知，即收到了黏性消息。这个概念就类似于 EventBus 中的 StickyEvent
3. 中间值可以被新值直接掩盖。当 Activity 处于后台时，如果 LiveData 先后接收到了多个值，那么当 Activity 回到前台时也只会收到最新值的一次回调，中间值直接被掩盖了，Activity 完全不会感受到中间值的存在

以上三点特性都是由于界面层的特点来决定的：

- 当界面处于后台时，此时就完全没有必要更新界面，因为此时界面对用户来说完全不可见，且界面有可能再也没有机会回到前台了，所以只有当界面回到前台时更新操作才是有意义的
- 当界面被意外销毁后，我们需要根据已有的数据来进行界面重建，所以 LiveData 被设计为黏性的
- 对于 LiveData 所代表的界面状态值来说，我们往往需要的只是其最新状态，不需要处理中间值，所以 LiveData 的中间值可以被新值直接掩盖

# 四、LiveData 作为消息通知组件

如果将 LiveData 单纯地作为界面层状态更新的载体来看待的话，那么以上三点特性就挺合情合理的了。但如果我们是将 LiveData 作为应用全局的消息通知组件的话，这三个特性就会给我们带来困扰了

相信很多开发者都尝试过将一个 LiveData 实例声明为静态变量，然后多个 Activity 通过同时监听该 LiveData 来实现数据通信。这种方式的优点是：**能够以非常简单的方式来实现跨页面通信，同时也保障了生命周期安全**。缺点是：**在 Activity 处于 onStop 状态时无法收到通知，且会收到黏性消息这种脏数据**

在 Activity 处于 onStop 状态时收到通知有什么意义呢？收到了黏性消息又会导致什么问题呢？这可以通过假设一个需求来说明

假设当前你的 App 包含一个**圈子列表页面**，每个圈子 item 包含了一个按钮用于改变对此圈子的**关注状态**，当点击关注后就会向用户展示一个几百毫秒的动画效果。点击 item 可以跳转到**圈子详情页**，在详情页也包含一个按钮用于改变圈子的关注状态

现在，产品要求两个页面间的关注状态是要能够实时统一的，即在圈子详情页改变关注状态后，圈子列表页面也要跟着一起改变关注状态。为了实现这个效果，可以声明一个全局的静态变量来实现跨页面通知圈子关注状态的变化

```kotlin
object FocusRepository {

    //String 表示圈子ID，Boolean 表示对该圈子的关注状态
    val focusLiveData = MutableLiveData<Pair<String, Boolean>>()

}

class CircleListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_circle_list)
        //建立监听
        FocusRepository.focusLiveData.observe(this, Observer {

        })
    }

}

class CircleDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_circle_details)
        onCircleFocusStateChanged("100", true)
    }

    private fun onCircleFocusStateChanged(circleId: String, focused: Boolean) {
        FocusRepository.focusLiveData.value = Pair(circleId, focused)
    }

}
```

这种方式就会导致三个问题：

- 当用户在 CircleDetailsActivity 改变了圈子的关注状态后返回，CircleListActivity 从后台回到前台后才会收到 focusLiveData 的事件通知，此时才会触发执行动画。而在这种情况下我们并不希望用户看到动画效果，而是希望能够在 CircleListActivity 改变关注状态的同时就实时在触发动画了。此时使用 LiveData 就无法满足我们的需求了，LiveData 不支持在 Activity 处于 onStop 状态时下发通知
- 如果在 CircleDetailsActivity 先后改变了多个圈子的关注状态的话，那么就会导致另一个问题：中间值被最新值直接掩盖了。这也是由于LiveData 不支持在 Activity 处于 onStop 状态时下发通知导致的
- 在 focusLiveData 已经有值的情况下，当用户第一次打开 CircleListActivity 时，就会收到 focusLiveData 的回调通知。而此时 CircleListActivity 的数据会从服务器获取，可以保证是最新的，并不需要本地值的回调通知，此时 focusLiveData 就相当于脏数据了。这种情况下，LiveData 也会给我们带来困扰，其黏性消息其实就相当于脏数据了

# 五、EventLiveData

考虑到 LiveData 不那么适合用做应用全局的消息通知组件，所以我就基于其源码实现了一个改良版的 EventLiveData，以此来解决 LiveData 的缺陷。EventLiveData 在使用上基本 LiveData 一样，我只是对其进行了功能扩展

发送消息：

```kotlin
val eventLiveData = EventLiveData<String>()

//主线程调用
eventLiveData.setValue("leavesC")
//子线程调用
eventLiveData.postValue("leavesC")
//任意线程都可以调用，内部会自动判断线程
eventLiveData.submitValue("leavesC")
```

不接收黏性消息：

```kotlin
//不接收黏性消息
//在 onResume 之后和 onDestroy 之前均能收到 Observer 回调
eventLiveData.observe(LifecycleOwner, Observer {

})

//不接收黏性消息
//在 onCreate 之后和 onDestroy 之前均能收到 Observer 回调
eventLiveData.observe(LifecycleOwner, Observer {

}, false)
```

接收黏性消息：

```kotlin
//接收黏性消息
//在 onResume 之后和 onDestroy 之前均能收到 Observer 回调
eventLiveData.observeSticky(LifecycleOwner, Observer {

})

//接收黏性消息
//在 onCreate 之后和 onDestroy 之前均能收到 Observer 回调
eventLiveData.observeSticky(LifecycleOwner, Observer {

}, false)
```

不和生命周期绑定：

```kotlin
//不接收黏性消息
eventLiveData.observeForever(Observer {

})

//接收黏性消息
eventLiveData.observeForeverSticky(Observer {

})
```

# 六、实现原理

EventLiveData 是基于 LiveData 的源码来改造实现的，在理解了 LiveData 的设计理念和实现原理后来进行自定义其实就非常简单了，这里就简单说下我的实现思路

LiveData 内部包含一个 `mVersion` 变量用来标记**当前值的版本，即值的新旧程度**，当外部传递了新值时（不管是 setValue 还是 postValue），mVersion 均会递增 +1

```java
@MainThread
private fun setValue(value: T) {
    assertMainThread(
        "setValue"
    )
    mVersion++
    mData = value
    dispatchingValue(null)
}
```

同时 ObserverWrapper 内部包含一个 `mLastVersion` 用于标记 Observer 内最后一个被回调的 value 的新旧程度

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

}
```

而 `considerNotify` 方法会根据 mLastVersion 的大小来决定是否需要向 Observer 回调值，那么我们只要控制 Observer 的 mLastVersion 的初始值大小不就可以避免旧值的通知了吗？

```java
private void considerNotify(ObserverWrapper observer) {
    ···
    if (observer.mLastVersion >= mVersion) {
        return;
    }
    observer.mLastVersion = mVersion;
    observer.mObserver.onChanged((T) mData);
}
```

再然后，LifecycleBoundObserver 的 `shouldBeActive()` 方法就限制了只有当 Lifecycle 的当前状态是 STARTED 或者 RESUMED 时才进行数据回调，那么我们只要改变此限制条件，就可以增大 Observer 的有效生命周期范围了

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
        return mOwner.getLifecycle().getCurrentState().isAtLeast(STARTED);
    }

}
```

# 七、引入依赖

EventLiveData 已托管到 jitpack，可以直接远程依赖。GitHub 地址：https://github.com/leavesCZY/EventLiveData

```groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}

dependencies {
    implementation 'com.github.leavesCZY:EventLiveData:1.0.2'
}
```

# 八、参考资料

- https://developer.android.google.cn/jetpack/guide