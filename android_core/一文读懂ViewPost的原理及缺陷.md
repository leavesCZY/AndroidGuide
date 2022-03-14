> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

很多开发者都了解这么一个知识点：在 Activity 的 `onCreate` 方法里我们无法直接获取到 View 的宽高信息，但通过 `View.post(Runnable)`这种方式就可以，那背后的具体原因你是否有了解过呢？

读者可以尝试以下操作。可以发现，除了通过 `View.post(Runnable)`这种方式可以获得 View 的真实宽高外，其它方式取得的值都是 0

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 */
class MainActivity : AppCompatActivity() {

    private val view by lazy {
        findViewById<View>(R.id.view)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getWidthHeight("onCreate")
        view.post {
            getWidthHeight("view.Post")
        }
        Handler().post {
            getWidthHeight("handler")
        }
    }

    override fun onResume() {
        super.onResume()
        getWidthHeight("onResume")
    }

    private fun getWidthHeight(tag: String) {
        Log.e(tag, "width: " + view.width)
        Log.e(tag, "height: " + view.height)
    }

}
```

```java
github.leavesc.view E/onCreate: width: 0
github.leavesc.view E/onCreate: height: 0
github.leavesc.view E/onResume: width: 0
github.leavesc.view E/onResume: height: 0
github.leavesc.view E/handler: width: 0
github.leavesc.view E/handler: height: 0
github.leavesc.view E/view.Post: width: 263
github.leavesc.view E/view.Post: height: 263
```

从这就可以引申出几个疑问：

- `View.post(Runnable)` 为什么可以得到 View 的真实宽高
- `Handler.post(Runnable)`和`View.post(Runnable)`有什么区别
- 在 `onCreate`、`onResume` 函数中为什么无法直接得到 View 的真实宽高
- `View.post(Runnable)` 中的 Runnable 是由谁来执行的，可以保证一定会被执行吗

后边就来一一解答这几个疑问，本文基于 Android API 30 进行分析

# 一、View.post(Runnable)

看下 `View.post(Runnable)` 的方法签名，可以看出 Runnable 的处理逻辑分为两种：

- 如果 `mAttachInfo` 不为 null，则将 Runnable 交由`mAttachInfo`内部的 Handler 进行处理
- 如果 `mAttachInfo` 为 null，则将 Runnable 交由 HandlerActionQueue 进行处理

```java
public boolean post(Runnable action) {
    final AttachInfo attachInfo = mAttachInfo;
    if (attachInfo != null) {
        return attachInfo.mHandler.post(action);
    }
    // Postpone the runnable until we know on which thread it needs to run.
    // Assume that the runnable will be successfully placed after attach.
    getRunQueue().post(action);
    return true;
}

private HandlerActionQueue getRunQueue() {
    if (mRunQueue == null) {
        mRunQueue = new HandlerActionQueue();
    }
    return mRunQueue;
}
```

## AttachInfo

先来看`View.post(Runnable)`的第一种处理逻辑

AttachInfo 是 View 内部的一个静态类，其内部持有一个 Handler 对象，从注释可知它是由 ViewRootImpl 提供的

```java
final static class AttachInfo {
    
        /**
         * A Handler supplied by a view's {@link android.view.ViewRootImpl}. This
         * handler can be used to pump events in the UI events queue.
         */
        @UnsupportedAppUsage
        final Handler mHandler;

    	AttachInfo(IWindowSession session, IWindow window, Display display,
                ViewRootImpl viewRootImpl, Handler handler, Callbacks effectPlayer,
                Context context) {
            ···
            mHandler = handler;
            ···
    	}
    
    	···
}
```

查找 `mAttachInfo` 的赋值时机可以追踪到 View 的 `dispatchAttachedToWindow` 方法，该方法被调用就意味着 View 已经 Attach 到 Window 上了

```java
@UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P)
void dispatchAttachedToWindow(AttachInfo info, int visibility) {
    mAttachInfo = info;
    ···
}
```

再查找`dispatchAttachedToWindow` 方法的调用时机，可以跟踪到 ViewRootImpl 类。ViewRootImpl 内就包含一个 Handler 对象 `mHandler`，并在构造函数中以 `mHandler` 作为构造参数之一来初始化 `mAttachInfo`。ViewRootImpl 的`performTraversals()`方法就会调用 DecorView 的 `dispatchAttachedToWindow` 方法并传入 `mAttachInfo`，从而层层调用整个视图树中所有 View 的 `dispatchAttachedToWindow` 方法，使得所有 childView 都能获取到 `mAttachInfo` 对象

```java
final ViewRootHandler mHandler = new ViewRootHandler();

public ViewRootImpl(Context context, Display display, IWindowSession session,
                    boolean useSfChoreographer) {
    ···
    mAttachInfo = new View.AttachInfo(mWindowSession, mWindow, display, this, mHandler, this,
            context);
    ···
}

private void performTraversals() {
    ···
    if (mFirst) {
        ···
        host.dispatchAttachedToWindow(mAttachInfo, 0);
        ···
    }
    ···
    performMeasure(childWidthMeasureSpec, childHeightMeasureSpec);
    performLayout(lp, mWidth, mHeight);
    performDraw();
    ···
}
```

此外，`performTraversals()`方法也负责启动整个视图树的 Measure、Layout、Draw 流程，只有当 `performLayout` 被调用后 View 才能确定自己的宽高信息。而 `performTraversals()`本身也是交由 ViewRootHandler 来调用的，即整个视图树的绘制任务也是先插入到 MessageQueue 中，后续再由主线程取出任务进行执行。由于插入到 MessageQueue 中的消息是交由主线程来顺序执行的，所以 `attachInfo.mHandler.post(action)`就保证了 `action` 一定是在 `performTraversals` 执行完毕后才会被调用，因此我们就可以在 Runnable 中获取到 View 的真实宽高了

## HandlerActionQueue

再来看`View.post(Runnable)`的第二种处理逻辑

HandlerActionQueue 可以看做是一个专门用于存储 Runnable 的任务队列，`mActions` 就存储了所有要执行的 Runnable 和相应的延时时间。两个`post`方法就用于将要执行的 Runnable 对象保存到 `mActions`中，`executeActions`就负责将`mActions`中的所有任务提交给 Handler 执行

```java
public class HandlerActionQueue {
    
    private HandlerAction[] mActions;
    private int mCount;

    public void post(Runnable action) {
        postDelayed(action, 0);
    }

    public void postDelayed(Runnable action, long delayMillis) {
        final HandlerAction handlerAction = new HandlerAction(action, delayMillis);
        synchronized (this) {
            if (mActions == null) {
                mActions = new HandlerAction[4];
            }
            mActions = GrowingArrayUtils.append(mActions, mCount, handlerAction);
            mCount++;
        }
    }

    public void executeActions(Handler handler) {
        synchronized (this) {
            final HandlerAction[] actions = mActions;
            for (int i = 0, count = mCount; i < count; i++) {
                final HandlerAction handlerAction = actions[i];
                handler.postDelayed(handlerAction.action, handlerAction.delay);
            }

            mActions = null;
            mCount = 0;
        }
    }

    private static class HandlerAction {
        final Runnable action;
        final long delay;

        public HandlerAction(Runnable action, long delay) {
            this.action = action;
            this.delay = delay;
        }

        public boolean matches(Runnable otherAction) {
            return otherAction == null && action == null
                    || action != null && action.equals(otherAction);
        }
    }
    
    ···
    
}
```

所以说，`getRunQueue().post(action)`只是将我们提交的 Runnable 对象保存到了 `mActions` 中，还需要外部主动调用 `executeActions`方法来执行任务

而这个主动执行任务的操作也是由 View 的 `dispatchAttachedToWindow`来完成的，从而使得 `mActions` 中的所有任务都会被插入到 `mHandler` 的 MessageQueue 中，等到主线程执行完 `performTraversals()` 方法后就会来执行 `mActions`，所以此时我们依然可以获取到 View 的真实宽高

```java
@UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P)
void dispatchAttachedToWindow(AttachInfo info, int visibility) {
    mAttachInfo = info;
    ···
    // Transfer all pending runnables.
    if (mRunQueue != null) {
        mRunQueue.executeActions(info.mHandler);
        mRunQueue = null;
    }
    ···
}
```

# 二、Handler.post(Runnable)

`Handler.post(Runnable)`和`View.post(Runnable)`有什么区别呢？

从上面的源码分析就可以知道，`View.post(Runnable)`之所以可以获取到 View 的真实宽高，主要就是因为确保了**获取 View 宽高的操作**一定是在 **View 绘制完毕之后**才被执行，而 `Handler.post(Runnable)`之所以不行，就是其无法保证这一点

虽然这两种`post(Runnable)`的操作都是往同个 MessageQueue 插入任务，且最终都是交由主线程来执行。但绘制视图树的任务是在`onResume`被回调后才被提交的，所以我们在`onCreate`中用 Handler 提交的任务就会早于绘制视图树的任务被执行，因此也就无法获取到 View 的真实宽高了

# 三、onCreate  &  onResume

在 `onCreate`、`onResume` 函数中为什么无法也直接得到 View 的真实宽高呢？

从结果反推原因，这说明当 `onCreate`、`onResume`被回调时 ViewRootImpl 的 `performTraversals()`方法还未执行，那么`performTraversals()`方法的具体执行时机是什么时候呢？

这可以从 **ActivityThread -> WindowManagerImpl -> WindowManagerGlobal -> ViewRootImpl** 这条调用链上找到答案

首先，ActivityThread 的 `handleResumeActivity` 方法就负责来回调 Activity 的 `onResume` 方法，且如果当前 Activity 是第一次启动，则会向 ViewManager（wm）添加 DecorView

```java
@Override
public void handleResumeActivity(IBinder token, boolean finalStateRequest, boolean isForward,
        String reason) {
    ···
    //Activity 的 onResume 方法
    final ActivityClientRecord r = performResumeActivity(token, finalStateRequest, reason);
    ···
    if (r.window == null && !a.mFinished && willBeVisible) {
        ···
        ViewManager wm = a.getWindowManager();
        if (a.mVisibleFromClient) {
            if (!a.mWindowAdded) {
                a.mWindowAdded = true;
                //重点
                wm.addView(decor, l);
            } else {
                a.onWindowAttributesChanged(l);
            }
        }
    } else if (!willBeVisible) {
        if (localLOGV) Slog.v(TAG, "Launch " + r + " mStartedActivity set");
        r.hideForNow = true;
    }
    ···
}
```

此处的 ViewManager 的具体实现类即 WindowManagerImpl，WindowManagerImpl 会将操作转交给 WindowManagerGlobal

```java
@UnsupportedAppUsage
private final WindowManagerGlobal mGlobal = WindowManagerGlobal.getInstance();

@Override
public void addView(@NonNull View view, @NonNull ViewGroup.LayoutParams params) {
    applyDefaultToken(params);
    mGlobal.addView(view, params, mContext.getDisplayNoVerify(), mParentWindow,
            mContext.getUserId());
}
```

WindowManagerGlobal 就会完成 ViewRootImpl 的初始化并且调用其 `setView` 方法，该方法内部就会再去调用 `performTraversals` 方法启动视图树的绘制流程

```java
public void addView(View view, ViewGroup.LayoutParams params,
        Display display, Window parentWindow, int userId) {
    ···
    ViewRootImpl root;
    View panelParentView = null;
    synchronized (mLock) {
        ···
        root = new ViewRootImpl(view.getContext(), display);
        view.setLayoutParams(wparams);
        mViews.add(view);
        mRoots.add(root);
        mParams.add(wparams);
        // do this last because it fires off messages to start doing things
        try {
            root.setView(view, wparams, panelParentView, userId);
        } catch (RuntimeException e) {
            // BadTokenException or InvalidDisplayException, clean up.
            if (index >= 0) {
                removeViewLocked(index, true);
            }
            throw e;
        }
    }
}
```

所以说， `performTraversals` 方法的调用时机是在 `onResume` 方法之后，所以我们在 `onCreate`和`onResume` 函数中都无法获取到 View 的实际宽高。当然，当 Activity 在单次生命周期过程中第二次调用`onResume` 方法时自然就可以获取到 View 的宽高属性

# 四、View.post(Runnable) 的兼容性

从以上分析可以得出一个结论：由于 `View.post(Runnable)`最终都是往和主线程关联的 MessageQueue 中插入任务且最终由主线程来顺序执行，所以即使我们是在子线程中调用`View.post(Runnable)`，最终也可以得到 View 正确的宽高值

但该结论也只在 **API 24 及之后的版本**上才成立，`View.post(Runnable)` 方法也存在着一个版本兼容性问题，在 **API 23 及之前的版本**上有着不同的实现方式

```java
//Android API 24 及之后的版本
public boolean post(Runnable action) {
    final AttachInfo attachInfo = mAttachInfo;
    if (attachInfo != null) {
        return attachInfo.mHandler.post(action);
    }
    // Postpone the runnable until we know on which thread it needs to run.
    // Assume that the runnable will be successfully placed after attach.
    getRunQueue().post(action);
    return true;
}

//Android API 23 及之前的版本
public boolean post(Runnable action) {
    final AttachInfo attachInfo = mAttachInfo;
    if (attachInfo != null) {
        return attachInfo.mHandler.post(action);
    }
    // Assume that post will succeed later
    ViewRootImpl.getRunQueue().post(action);
    return true;
}
```

在 Android API 23 及之前的版本上，当 `attachInfo` 为 null 时，会将 Runnable 保存到 ViewRootImpl 内部的一个静态成员变量 `sRunQueues` 中。而 `sRunQueues` 内部是通过 ThreadLocal 来保存 RunQueue 的，这意味着不同线程获取到的 RunQueue 是不同对象，这也意味着**如果我们在子线程中调用`View.post(Runnable)` 方法的话，该 Runnable 永远不会被执行，因为主线程根本无法获取到子线程的 RunQueue**

```java
static final ThreadLocal<RunQueue> sRunQueues = new ThreadLocal<RunQueue>();

static RunQueue getRunQueue() {
    RunQueue rq = sRunQueues.get();
    if (rq != null) {
        return rq;
    }
    rq = new RunQueue();
    sRunQueues.set(rq);
    return rq;
}
```

此外，由于`sRunQueues` 是静态成员变量，主线程会一直对应同一个 RunQueue 对象，如果我们是在主线程中调用`View.post(Runnable)`方法的话，那么该 Runnable 就会被添加到和主线程关联的 RunQueue 中，后续主线程就会取出该 Runnable 来执行

即使该 View 是我们直接 new 出来的对象（就像以下的示例），以上结论依然生效，当系统需要绘制其它视图的时候就会顺便取出该任务，一般很快就会执行到。当然，由于此时 View 并没有 attachedToWindow，所以获取到的宽高值肯定也是 0

```kotlin
val view = View(Context)
view.post {
    getWidthHeight("view.Post")
}
```

对`View.post(Runnable)`方法的兼容性问题做下总结：

- 当 API < 24 时，如果是在主线程进行调用，那么不管 View 是否有 AttachedToWindow，提交的 Runnable 均会被执行。但只有在 View 被 AttachedToWindow 的情况下才可以获取到 View 的真实宽高
- 当 API < 24 时，如果是在子线程进行调用，那么不管 View 是否有 AttachedToWindow，提交的 Runnable 都将永远不会被执行
- 当 API >= 24 时，不管是在主线程还是子线程进行调用，只要 View 被 AttachedToWindow 后，提交的 Runnable 都会被执行，且都可以获取到 View 的真实宽高值。如果没有被 AttachedToWindow 的话，Runnable 也将永远不会被执行

# 五、总结

Activity 的 `onResume` 方法在第一次被调用后，绘制视图树的 Runnable 才会被 Post 到和主线程关联的 MessageQueue 中，虽然该 Runnable 和**回调 Activity 的 `onResume` 方法**的操作都是在主线程中执行的，但是该 Runnable 只有等到主线程后续将其从 MessageQueue 取出来后才会被执行，所以这两者其实是构成了异步行为，因此我们在`onCreate` 和`onResume` 这两个方法里才无法直接获取到 View 的宽高大小

当 View 还未绘制完成时，通过 `View.post(Runnable)`提交的 Runnable 会等到 `View.dispatchAttachedToWindow`方法被调用后才会被保存到 MessageQueue 中，这样也依然保证了该 Runnable 一定是会在 View 绘制完成后才会被执行，所以此时我们才能获取到 View 的宽高大小

除了`View.post(Runnable)`外，我们还可以通过 OnGlobalLayoutListener 来获取 View 的宽高属性，`onGlobalLayout` 方法会在视图树发生变化的时候被调用，在该方法中我们就可以来获取 View 的宽高大小

```kotlin
 view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
    override fun onGlobalLayout() {
        view.viewTreeObserver.removeOnGlobalLayoutListener(this)
        val width = view.width
    }
})
```

按照我自己的想法，系统提供`View.post(Runnable)`这个方法的目的不仅仅是为了用来获取 View 的宽高等属性这么简单，有可能是为了提供一种优化手段，使得我们可以在整个视图树均绘制完毕后才去执行一些**不紧急又必须执行**的操作，使得整个视图树可以尽快地呈现出来，以此优化用户体验