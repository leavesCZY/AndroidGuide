> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

Context 在我们的日常开发中经常会使用到，在代码层次上指的是**一个抽象类**，在概念上指的是**上下文环境**，在功能上则起到了**访问系统服务及系统资源的作用**。Activity、Service 和 Application 都间接地继承于 Context

Context 是一个抽象类，像我们平时经常使用的 `startActivity、sendBroadcast、getSharedPreferences`等方法都是由其来定义的，Context 的大体继承关系如下图所示

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/11bc4e18d88946c5bda641c72d89699f~tplv-k3u1fbpfcp-zoom-1.image)

- ContextImpl 和 ContextWrapper 都直接地继承于 Context，但真正实现了父类方法的是 ContextImpl。ContextWrapper 则不包含实际的实现逻辑，其内部包含一个 ContextImpl 类型实例的成员变量`mBase`，ContextWrapper 中几乎所有方法都通过调用`mBase`的相同方法来实现，起到了方法传递的作用，隐藏了方法体的具体实现逻辑
- ContextThemeWrapper、Service、Application 和 ReceiverRestrictedContext 都继承于 ContextWrapper，都可以通过 `mBase`来使用 Context 的方法，同时它们也是装饰类，在 ContextWrapper 的基础上又扩展了不同的功能
- Activity 需要支持和主题相关的功能，所以 Activity 又继承于 ContextThemeWrapper，以此在 ContextWrapper 的基础上实现了功能扩展，其它不需要主题功能的则直接继承于 ContextWrapper
- 系统限制了 BroadcastReceiver 不能用于注册广播和绑定服务，所以其 onReceive 方法传入的 Context 对象实际上属于 ReceiverRestrictedContext 类型。ReceiverRestrictedContext 重载了 registerReceiver 和 bindService 等方法，当被调用时会直接抛出异常
- 可以看出来，Contex 整体的实现关系是使用到了装饰模式，通过组合而非继承的方式来扩展或者是限制 ContextImpl 的功能，在运行时选择不同的装饰类来用于特定的功能场景

# 一、Activity

上文讲了，ContextWrapper 内部包含一个 ContextImpl 类型实例的成员变量`mBase`，因此 Activity 也同样包含。Activity 的`mBase`的初始化时机主要看 ActivityThread 的 `performLaunchActivity` 方法，该方法就用于在启动 Activity 时构建 Activity 实例。此外，我们知道 Activity 包含一个 `getApplication()`方法用于获取 Application 实例，那么在实例化 Activity 的时候需要一起把 ContextImpl 和 Application 传给 Activity

`performLaunchActivity`方法步骤大体上可以分为四步：

- 第一步，通过 createBaseContextForActivity 方法创建 ContextImpl 实例，得到 appContext
- 第二步，通过反射实例化 Activity，得到 activity
- 第三步，拿到 Application 实例，即 app
- 第四步，通过 attach 方法将 appContext 和 app 传给 activity，完成 mBase 和 Application 的初始化

```java
/**  Core implementation of activity launch. */
private Activity performLaunchActivity(ActivityClientRecord r, Intent customIntent) {
    ···
    //第一步
    ContextImpl appContext = createBaseContextForActivity(r);
    Activity activity = null;
    try {
        java.lang.ClassLoader cl = appContext.getClassLoader();
        //第二步
        activity = mInstrumentation.newActivity(
                cl, component.getClassName(), r.intent);
        ···
    } catch (Exception e) {
        ···
    }

    try {
        //第三步
        Application app = r.packageInfo.makeApplication(false, mInstrumentation);
        ···
        if (activity != null) {
            ···
            appContext.setOuterContext(activity);
            //第四步
            activity.attach(appContext, this, getInstrumentation(), r.token,
                    r.ident, app, r.intent, r.activityInfo, title, r.parent,
                    r.embeddedID, r.lastNonConfigurationInstances, config,
                    r.referrer, r.voiceInteractor, window, r.configCallback,
                    r.assistToken);

            ···
        }
        ···
    } catch (SuperNotCalledException e) {
        throw e;
    } catch (Exception e) {
        if (!mInstrumentation.onException(activity, e)) {
            throw new RuntimeException(
                "Unable to start activity " + component
                + ": " + e.toString(), e);
        }
    }
    return activity;
}
```

Activity 的 `attach`方法又会向 `mApplication` 和 `mBase` 两个成员变量赋值。以下属于伪代码，`mBase` 和 `attachBaseContext` 其实是声明在父类 ContextWrapper 中的，读者意会即可

```java
public class Activity extends ContextThemeWrapper {

    @UnsupportedAppUsage
    Context mBase;
    
    private Application mApplication;
    
    @UnsupportedAppUsage
    final void attach(Context context, ActivityThread aThread,
            Instrumentation instr, IBinder token, int ident,
            Application application, Intent intent, ActivityInfo info,
            CharSequence title, Activity parent, String id,
            NonConfigurationInstances lastNonConfigurationInstances,
            Configuration config, String referrer, IVoiceInteractor voiceInteractor,
            Window window, ActivityConfigCallback activityConfigCallback, IBinder assistToken) {
        attachBaseContext(context);
        ···
        mApplication = application;
        ···
    }

    protected void attachBaseContext(Context base) {
        if (mBase != null) {
            throw new IllegalStateException("Base context already set");
        }
        mBase = base;
    }
    
    public final Application getApplication() {
        return mApplication;
    }
    
}
```

所以说，Activity 包含的 mBase 和 Application 都是在启动过程中得到的，Activity 在被实例化后，其 `attach` 方法就会被调用从而初始化这两个成员变量。Activity 直接继承于 ContextThemeWrapper，ContextThemeWrapper 又直接继承于 ContextWrapper，Activity 拿到的 ContextImpl 就用来初始化声明在 ContextWrapper 中的 mBase，之后 Activity 就可以使用 Context 中的各个方法了

# 二、Service

Service 的 Context 创建过程与 Activity 类似，主要看 ActivityThread 的 `handleCreateService` 方法，该方法就用于创建 Service 实例并回调其 `onCreate` 方法

```java
@UnsupportedAppUsage
private void handleCreateService(CreateServiceData data) {
    ···
    Service service = null;
    try {
        if (localLOGV) Slog.v(TAG, "Creating service " + data.info.name);

        ContextImpl context = ContextImpl.createAppContext(this, packageInfo);
        Application app = packageInfo.makeApplication(false, mInstrumentation);

        ···

        service = packageInfo.getAppFactory()
                .instantiateService(cl, data.info.name, data.intent);
        service.attach(context, this, data.info.name, data.token, app,
                ActivityManager.getService());
        service.onCreate();

        mServices.put(data.token, service);
        try {
            ActivityManager.getService().serviceDoneExecuting(
                    data.token, SERVICE_DONE_EXECUTING_ANON, 0, 0);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    } catch (Exception e) {
        if (!mInstrumentation.onException(service, e)) {
            throw new RuntimeException(
                "Unable to create service " + data.info.name
                + ": " + e.toString(), e);
        }
    }
}
```

在拿到 ContextImpl 与 Application 实例后，Service 的 `attach` 方法也完成了自身 mBase 和 mApplication 两个成员变量的初始化，整个过程和 Activity 十分类似

```java
@UnsupportedAppUsage
public final void attach(
        Context context,
        ActivityThread thread, String className, IBinder token,
        Application application, Object activityManager) {
    attachBaseContext(context);
    ···
    mApplication = application;
    ···
}
```

# 三、BroadcastReceiver

BroadcastReceiver 的 Context 创建过程主要看 ActivityThread 的 `handleReceiver` 方法，该方法就用于创建 BroadcastReceiver 实例并回调其 `onReceive` 方法。由于系统限制了 BroadcastReceiver 不能用于注册广播和绑定服务，所以其 `onReceive` 方法传入的 Context 对象实际上属于 ContextWrapper 的子类  ReceiverRestrictedContext

```java
@UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P, trackingBug = 115609023)
private void handleReceiver(ReceiverData data) {
    ···
    Application app;
    BroadcastReceiver receiver;
    ContextImpl context;
    try {
        app = packageInfo.makeApplication(false, mInstrumentation);
        context = (ContextImpl) app.getBaseContext();
        ···
        receiver = packageInfo.getAppFactory()
                .instantiateReceiver(cl, data.info.name, data.intent);
    } catch (Exception e) {
        ···
    }
    try {
        ···
        //传递的是 ReceiverRestrictedContext
        receiver.onReceive(context.getReceiverRestrictedContext(),
                data.intent);
    }
    ···
}
```

ReceiverRestrictedContext 重载了`registerReceiver` 和 `bindService` 等方法，当被调用时会直接抛出异常，从而限制了 BroadcastReceiver 的相应功能

```java
class ReceiverRestrictedContext extends ContextWrapper {

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter,
            String broadcastPermission, Handler scheduler) {
        if (receiver == null) {
            // Allow retrieving current sticky broadcast; this is safe since we
            // aren't actually registering a receiver.
            return super.registerReceiver(null, filter, broadcastPermission, scheduler);
        } else {
            throw new ReceiverCallNotAllowedException(
                    "BroadcastReceiver components are not allowed to register to receive intents");
        }
    }

    @Override
    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
        throw new ReceiverCallNotAllowedException(
                "BroadcastReceiver components are not allowed to bind to services");
    }

    ···
}
```

# 四、ContentProvider

ContentProvider 并不是 Context 的子类，但由于其属于四大组件之一，这里就一起讲下吧

在应用启动时系统会自动初始化 ContentProvider 并传入 Context 对象，因此很多三方开源库都选择通过 ContentProvider 来拿到 Context 对象并初始化自身，这样对于引用开源库的一方来说侵入性会更低

ContentProvider 的 Context 创建过程主要看 ActivityThread 的 `installProvider` 方法，该方法就用于创建 ContentProvider 实例。该方法在拿到 ContextImpl 实例后，就会再通过反射得到 ContentProvider 实例，然后再调用 ContentProvider 的 `attachInfo` 方法

```java
@UnsupportedAppUsage
private ContentProviderHolder installProvider(Context context,
        ContentProviderHolder holder, ProviderInfo info,
        boolean noisy, boolean noReleaseNeeded, boolean stable) {
    ContentProvider localProvider = null;
    IContentProvider provider;
    if (holder == null || holder.provider == null) {
        if (DEBUG_PROVIDER || noisy) {
            Slog.d(TAG, "Loading provider " + info.authority + ": "
                    + info.name);
        }
        Context c = null;
        //得到 ContextImpl 对象
        c = context.createPackageContext(ai.packageName,
                        Context.CONTEXT_INCLUDE_CODE);
        ···
        try {
            ···
            //通过反射实例化 ContentProvider
            localProvider = packageInfo.getAppFactory()
                    .instantiateProvider(cl, info.name);
            ···
            //传入 Context 对象
            localProvider.attachInfo(c, info);
        } catch (java.lang.Exception e) {
            if (!mInstrumentation.onException(null, e)) {
                throw new RuntimeException(
                        "Unable to get provider " + info.name
                        + ": " + e.toString(), e);
            }
            return null;
        }
    } else {
        provider = holder.provider;
        if (DEBUG_PROVIDER) Slog.v(TAG, "Installing external provider " + info.authority + ": "
                + info.name);
    }
    ···
    return retHolder;
}
```

ContentProvider 的 `attachInfo` 方法最终就会初始化自身的 mContext 变量，然后再回调自身的 `onCreate()` 方法，从而完成自身的初始化

```java
@UnsupportedAppUsage
private Context mContext = null;

public void attachInfo(Context context, ProviderInfo info) {
    attachInfo(context, info, false);
}

private void attachInfo(Context context, ProviderInfo info, boolean testing) {
    ···
    if (mContext == null) {
        mContext = context;
        ···
        ContentProvider.this.onCreate();
    }
}
```

# 五、Application

回顾以上代码，可以看到 Activity、Service、BroadcastReceiver 三者都是通过 `LoadedApk.makeApplication` 方法拿到 Application 实例的，再来看下 Application Context 的创建流程

`makeApplication` 方法的流程可以分为四步：

- 第一步，通过 createAppContext 方法创建 ContextImpl 实例，得到 appContext
- 第二步，通过反射实例化 Application 并回调其 attach(Context) 方法，传入的 Context 参数即 appContext，从而初始化 Application 的 mBase 变量
- 第三步，将得到的 Application 保存为全局变量 mApplication
- 第四步，回调 Application 的 onCreate 方法

```java
@UnsupportedAppUsage
private Application mApplication;	

@UnsupportedAppUsage
public Application makeApplication(boolean forceDefaultAppClass,
        Instrumentation instrumentation) {
    if (mApplication != null) {
        return mApplication;
    }
    Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "makeApplication");

    Application app = null;

    ···
    try {
        ···
        //第一步
        ContextImpl appContext = ContextImpl.createAppContext(mActivityThread, this);
        ···
        //第二步    
        app = mActivityThread.mInstrumentation.newApplication(
                cl, appClass, appContext);
        appContext.setOuterContext(app);
    } catch (Exception e) {
        ···
    }
    mActivityThread.mAllApplications.add(app);
    //第三步
    mApplication = app;

    if (instrumentation != null) {
        try {
            //第四步
            instrumentation.callApplicationOnCreate(app);
        } catch (Exception e) {
            if (!instrumentation.onException(app, e)) {
                Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
                throw new RuntimeException(
                    "Unable to create application " + app.getClass().getName()
                    + ": " + e.toString(), e);
            }
        }
    }

    Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);

    return app;
}
```

Context 类还包含一个 `getApplicationContext()`方法用于获取 Application 类型的 Context，该方法的具体逻辑由 ContextImpl 实现。如果 `mPackageInfo` 不为 null 的话，则调用其 `getApplication()` 方法拿到上述第三步保存的 Application 对象，否则通过 `mMainThread` 来获取

由于在 Activity、Service、BroadcastReceiver 中调用`getApplicationContext()`方法时，应用已经是启动的了，所以此时 mPackageInfo 不会为 null，因此我们就这可以在这几个类中获取到当前进程中唯一的 ApplicationContext 实例

```java
@UnsupportedAppUsage
final @NonNull ActivityThread mMainThread;

@UnsupportedAppUsage
final @NonNull LoadedApk mPackageInfo;

@Override
public Context getApplicationContext() {
    return (mPackageInfo != null) ? mPackageInfo.getApplication() : mMainThread.getApplication();
}
```