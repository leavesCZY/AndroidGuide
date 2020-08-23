最近，Google Jetpack 官网上新增了一个名为 `App Startup` 的组件，链接：[App Startup](https://developer.android.com/topic/libraries/app-startup)。根据官方文档的介绍，`App Startup` 提供了一种直接、高效的方式用来在应用程序启动时对多个组件进行初始化，开发者可以依靠它来显式地设置多个组件间的初始化顺序并优化应用的启动时间

本文已收录至我的学习笔记：[AndroidGuide](https://github.com/leavesC/AndroidGuide)

本文内容均基于`App Startup`当前最新的 alpha 版本：

```java
implementation "androidx.startup:startup-runtime:1.0.0-alpha01"
```

### 一、App Startup 的意义

`App Startup` 允许 **Library 开发者**和 **App 开发者**共享同一个 ContentProvider 来完成各自的初始化逻辑，并支持设置组件之间的初始化先后顺序，避免为每个需要初始化的组件都单独定义一个 ContentProvider，从而大大缩短应用的启动时间

目前很多第三方依赖库为了简化使用者的使用成本，就选择通过声明一个 ContentProvider 来获取 Context 对象并自动完成初始化过程。例如 **Lifecycle** 组件就声明了一个 `ProcessLifecycleOwnerInitializer` 用于获取 context 对象并完成初始化。而在 **AndroidManifest.xml** 文件中声明的每一个 ContentProvider，在 `Application` 的 `onCreate()` 函数被调用之前就会预先被执行并调用内部的 `onCreate()` 方法。应用每构建并执行一个 ContentProvider 都是有着内存和时间的消耗成本，如果应用的 ContentProvider 过多，无疑会大大增加应用的启动时间

因此，`App Startup` 的存在，无疑是可以为很多依赖项（应用自身的组件和第三方组件）提供一个统一的初始化入口，当然这也需要等到 `App Startup` 发布 release 版本并被大多数三方依赖组件采用之后了

### 二、如何使用

假设我们的项目中一共有三个 Library 需要进行初始化。当中，Library A 依赖于 Library B，Library B 依赖于 Library C，Library C 不需要其它依赖项，则此时可以分别为三个 Library 建立三个 `Initializer` 实现类

`Initializer` 是 Startup 提供的用于声明初始化逻辑和初始化顺序的接口，在 `create(context: Context)`方法中完成初始化过程并返回结果值，在`dependencies()`中指定初始化此 Initializer 前需要先初始化的其它 Initializer 

```kotlin
	class InitializerA : Initializer<A> {

        //在此处完成组件的初始化，并返回初始化结果值
        override fun create(context: Context): A {
            return A.init(context)
        }
		
        //获取在初始化自身之前需要先初始化的 Initializer 列表
        //如果不需要依赖于其它组件，则可以返回一个空列表
        override fun dependencies(): List<Class<out Initializer<*>>> {
            return listOf(InitializerB::class.java)
        }

    }

    class InitializerB : Initializer<B> {

        override fun create(context: Context): B {
            return B.init(context)
        }

        override fun dependencies(): List<Class<out Initializer<*>>> {
            return listOf(InitializerC::class.java)
        }

    }

    class InitializerC : Initializer<C> {

        override fun create(context: Context): C {
            return C.init(context)
        }

        override fun dependencies(): List<Class<out Initializer<*>>> {
            return listOf()
        }

    }
```

Startup 提供了两种初始化方法，分别是**自动初始化**和**手动初始化（延迟初始化）**

#### 2.1、自动初始化

在 `AndroidManifest` 文件中对 Startup 提供的 `InitializationProvider` 进行声明，并且用 **meta-data** 标签声明 Initializer 实现类的包名路径，value 必须是 **"androidx.startup"**。在这里我们只需要声明 InitializerA 即可，因为 InitializerB 和 InitializerC 均可以通过 InitializerA 的 `dependencies()`方法的返回值链式定位到

```xml
        <provider
            android:name="androidx.startup.InitializationProvider"
            android:authorities="${applicationId}.androidx-startup"
            android:exported="false"
            tools:node="merge">
            <meta-data
                android:name="leavesc.lifecyclecore.core.InitializerA"
                android:value="androidx.startup" />
        </provider>
```

只要完成以上步骤，当应用启动时，Startup 就会自动按照我们规定的顺序依次进行初始化。需要注意的是，如果 Initializer 之间不存在依赖关系，且都希望由 InitializationProvider 为我们自动初始化的话，此时所有的 Initializer 就必须都进行显式声明，且 Initializer 的初始化顺序会和在 provider 中的声明顺序保持一致

#### 2.2、手动初始化

大部分情况下自动初始化的方式都能满足我们的要求，但在某些情况下并不适用，例如：组件的初始化成本（性能消耗或者时间消耗）较高且该组件最终未必会使用到，此时就可以将之改为在使用到的时候再来对其进行初始化了，即懒加载组件

手动初始化的 Initializer 不需要在 AndroidManifest 中进行声明，只需要通过调用以下方法进行初始化即可

```kotlin
val result = AppInitializer.getInstance(this).initializeComponent(InitializerA::class.java)
```

由于 Startup 内部会缓存 Initializer 的初始化结果值，所以重复调用 `initializeComponent`方法不会导致多次初始化，该方法也可用于自动初始化时获取初始化结果值

如果应用内的所有 Initializer 都不需要进行自动初始化的话，也可以不在 AndroidManifest 中声明 InitializationProvider 

### 三、注意事项

#### 3.1、移除 Initializer 

假设我们在项目中引入的某个第三方依赖库自身使用到了 Startup 进行自动初始化，我们希望将之改为懒加载的方式，但我们无法直接修改第三方依赖库的 AndroidManifest 文件，此时就可以通过 AndroidManifest 的合并规则来移除指定的 Initializer 

假设第三方依赖库的 Initializer 的包名路径是 `xxx.xxx.InitializerImpl`，在主项目工程的 AndroidManifest 文件中主动对其进行声明，并添加 `tools:node="remove"` 语句要求在合并 AndroidManifest 文件时移除自身，这样 Startup 就不会自动初始化 InitializerImpl 了

```xml
        <provider
            android:name="androidx.startup.InitializationProvider"
            android:authorities="${applicationId}.androidx-startup"
            android:exported="false"
            tools:node="merge">
            <meta-data
                android:name="leavesc.lifecyclecore.mylibrary.TestIn"
                android:value="androidx.startup"
                tools:node="remove" />
        </provider>
```

#### 3.2、禁止自动初始化

如果希望禁止 Startup 的所有自动初始化逻辑，但又不希望通过直接删除 provider 声明来实现的话，那么可以通过如上所述的方法来实现此目的

```xml
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    tools:node="remove" />
```

#### 3.3、Lint 检查

App Startup 包含一组 Lint 规则，可用于检查是否已正确定义了组件的初始化程序，可以通过运行 `./gradlew :app:lintDebug` 来执行检查规则

例如，如果项目中声明的 InitializerB 没有在 AndroidManifest 中进行声明，且也不包含在其它 Initializer 的依赖项列表里时，通过 Lint 检查就可以看到如下的警告语句：

```xml
Errors found:

xxxx\leavesc\lifecyclecore\core\InitializerHodler.kt:52: Error: Every Initializer needs to be accompanied by a corresponding <meta-data> entry in the AndroidManifest.xml file. [Ensur
eInitializerMetadata]
  class InitializerB : Initializer<B> {
  ^
```

### 四、源码解析

Startup 整个依赖库仅包含五个 Java 文件，整体逻辑比较简单，这里依次介绍下每个文件的作用

#### 4.1、StartupLogger

StartupLogger 是一个日志工具类，用于向控制台输出日志

```java
public final class StartupLogger {

    private StartupLogger() {
        // Does nothing.
    }

    /**
     * The log tag.
     */
    private static final String TAG = "StartupLogger";

    /**
     * To enable logging set this to true.
     */
    static final boolean DEBUG = false;

    /**
     * Info level logging.
     *
     * @param message The message being logged
     */
    public static void i(@NonNull String message) {
        Log.i(TAG, message);
    }

    /**
     * Error level logging
     *
     * @param message   The message being logged
     * @param throwable The optional {@link Throwable} exception
     */
    public static void e(@NonNull String message, @Nullable Throwable throwable) {
        Log.e(TAG, message, throwable);
    }
}
```

#### 4.2、StartupException

StartupException 是一个自定义的 RuntimeException 子类，当 Startup 在初始化过程中遇到意外之外的情况时（例如，Initializer 存在循环依赖、Initializer 反射失败等情况），就会抛出 StartupException

```java
public final class StartupException extends RuntimeException {
    public StartupException(@NonNull String message) {
        super(message);
    }

    public StartupException(@NonNull Throwable throwable) {
        super(throwable);
    }

    public StartupException(@NonNull String message, @NonNull Throwable throwable) {
        super(message, throwable);
    }
}
```

#### 4.3、Initializer

Initiaizer 是 Startup 提供的用于声明初始化逻辑和初始化顺序的接口，在 `create(context: Context)`方法中完成初始化过程并返回结果值，在`dependencies()`中指定初始化此 Initializer 前需要先初始化的其它 Initializer 

```java
public interface Initializer<T> {

    /**
     * Initializes and a component given the application {@link Context}
     * 
     * @param context The application context.
     */
    @NonNull
    T create(@NonNull Context context);

    /**
     * @return A list of dependencies that this {@link Initializer} depends on. This is
     * used to determine initialization order of {@link Initializer}s.
     * <br/>
     * For e.g. if a {@link Initializer} `B` defines another
     * {@link Initializer} `A` as its dependency, then `A` gets initialized before `B`.
     */
    @NonNull
    List<Class<? extends Initializer<?>>> dependencies();
}
```

#### 4.4、InitializationProvider

InitializationProvider 就是需要我们主动声明在 AndroidManifest.xml 文件中的 ContentProvider，Startup 的整个初始化逻辑都是在这里进行统一触发的

由于 InitializationProvider 的作用仅是用于统一多个依赖项的初始化入口并获得 Context 对象，所以除了 `onCreate()` 方法会由系统自动调用外，query、getType、insert、delete、update 等方法本身是没有意义的，如果开发者调用了这几个方法就会直接抛出异常

```java
public final class InitializationProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        Context context = getContext();
        if (context != null) {
            AppInitializer.getInstance(context).discoverAndInitialize();
        } else {
            throw new StartupException("Context cannot be null");
        }
        return true;
    }

    @Nullable
    @Override
    public Cursor query(
            @NonNull Uri uri,
            @Nullable String[] projection,
            @Nullable String selection,
            @Nullable String[] selectionArgs,
            @Nullable String sortOrder) {
        throw new IllegalStateException("Not allowed.");
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        throw new IllegalStateException("Not allowed.");
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        throw new IllegalStateException("Not allowed.");
    }

    @Override
    public int delete(
            @NonNull Uri uri,
            @Nullable String selection,
            @Nullable String[] selectionArgs) {
        throw new IllegalStateException("Not allowed.");
    }

    @Override
    public int update(
            @NonNull Uri uri,
            @Nullable ContentValues values,
            @Nullable String selection,
            @Nullable String[] selectionArgs) {
        throw new IllegalStateException("Not allowed.");
    }
}
```

#### 4.5、AppInitializer

AppInitializer 是 Startup 整个库的核心重点，整体代码量不足两百行，AppInitializer 的整体流程是：

- 由 InitializationProvider 传入 Context 对象以此来获得 AppInitializer 唯一实例，并调用 `discoverAndInitialize()` 函数完成所有的自动初始化逻辑
- `discoverAndInitialize()` 函数会先对 InitializationProvider 进行解析，获取到包含的所有 metadata，然后按声明顺序依次反射构建每个 metadata 指向的 Initializer 对象
- 当在初始化某个 Initializer 对象之前，会首先判断其关联的依赖项 **dependencies** 是否为空。如果为空的话则直接调用其 `create(Context)` 函数进行初始化。如果不为空的话则先对 **dependencies** 进行初始化，对每个 **dependency** 均重复此遍历操作，直到不包含  **dependencies** 的 Initializer 最先初始化完成后才原路返回依次进行初始化，从而保证了 Initializer 之间初始化顺序的有序性
- 当存在这几种情况时，Startup 会抛出异常：Initializer 实现类不包含无参构造函数、Initializer 之间存在循环依赖关系、Initializer 的初始化过程（`create(Context)` 函数）抛出了异常

AppInitializer 对外开放了 `getInstance(@NonNull Context context)` 方法用于获取唯一的静态实例

```java
public final class AppInitializer {

    /**
     * 唯一的静态实例
     * The {@link AppInitializer} instance.
     */
    private static AppInitializer sInstance;

    /**
     * 同步锁
     * Guards app initialization.
     */
    private static final Object sLock = new Object();

    //用于存储所有已进行初始化了的 Initializer 及对应的初始化结果
    @NonNull
    final Map<Class<?>, Object> mInitialized;

    @NonNull
    final Context mContext;

    /**
     * Creates an instance of {@link AppInitializer}
     *
     * @param context The application context
     */
    AppInitializer(@NonNull Context context) {
        mContext = context.getApplicationContext();
        mInitialized = new HashMap<>();
    }

    /**
     * @param context The Application {@link Context}
     * @return The instance of {@link AppInitializer} after initialization.
     */
    @NonNull
    @SuppressWarnings("UnusedReturnValue")
    public static AppInitializer getInstance(@NonNull Context context) {
        synchronized (sLock) {
            if (sInstance == null) {
                sInstance = new AppInitializer(context);
            }
            return sInstance;
        }
    }
    
    ···
    
}
```

`discoverAndInitialize()` 方法由 InitializationProvider 进行调用，由其触发所有需要进行默认初始化的依赖项的初始化操作

```java
	@SuppressWarnings("unchecked")
    void discoverAndInitialize() {
        try {
            Trace.beginSection(SECTION_NAME);
            
            //获取 InitializationProvider 包含的所有 metadata
            ComponentName provider = new ComponentName(mContext.getPackageName(),
                    InitializationProvider.class.getName());
            ProviderInfo providerInfo = mContext.getPackageManager()
                    .getProviderInfo(provider, GET_META_DATA);
            Bundle metadata = providerInfo.metaData;
            
            //获取到字符串 androidx.startup
            //因为 Startup 是以该字符串作为 metaData 的固定 value 来进行遍历的
            //所以如果在 AndroidManifest 文件中声明了不同 value 则不会被初始化
            String startup = mContext.getString(R.string.androidx_startup);
            
            if (metadata != null) {
                //用于标记正在准备进行初始化的 Initializer
                //用于判断是否存在循环依赖的情况
                Set<Class<?>> initializing = new HashSet<>();
                Set<String> keys = metadata.keySet();
                for (String key : keys) {
                    String value = metadata.getString(key, null);
                    if (startup.equals(value)) {
                        Class<?> clazz = Class.forName(key);
                        //确保 metaData 声明的包名路径指向的是 Initializer 的实现类
                        if (Initializer.class.isAssignableFrom(clazz)) {
                            Class<? extends Initializer<?>> component =
                                    (Class<? extends Initializer<?>>) clazz;
                            if (StartupLogger.DEBUG) {
                                StartupLogger.i(String.format("Discovered %s", key));
                            }
                            //进行实际的初始化过程
                            doInitialize(component, initializing);
                        }
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException | ClassNotFoundException exception) {
            throw new StartupException(exception);
        } finally {
            Trace.endSection();
        }
    }
```

`doInitialize()` 函数是实际调用了 Initializer 的 `create(context: Context)`的地方，其主要逻辑就是通过嵌套调用的方式来完成所有依赖项的初始化，当判断出存在循环依赖的情况时将抛出异常

```java
	@NonNull
    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
    <T> T doInitialize(
            @NonNull Class<? extends Initializer<?>> component,
            @NonNull Set<Class<?>> initializing) {
        synchronized (sLock) {
            boolean isTracingEnabled = Trace.isEnabled();
            try {
                if (isTracingEnabled) {
                    // Use the simpleName here because section names would get too big otherwise.
                    Trace.beginSection(component.getSimpleName());
                }
                if (initializing.contains(component)) {
                    //initializing 包含 component，说明 Initializer 之间存在循环依赖
                    //直接抛出异常
                    String message = String.format(
                            "Cannot initialize %s. Cycle detected.", component.getName()
                    );
                    throw new IllegalStateException(message);
                }
                Object result;
                if (!mInitialized.containsKey(component)) {
                    //如果 mInitialized 不包含 component
                    //说明 component 指向的 Initializer 还未进行初始化
                    initializing.add(component);
                    try {
                        //通过反射调用 component 的无参构造函数并初始化
                        Object instance = component.getDeclaredConstructor().newInstance();
                        Initializer<?> initializer = (Initializer<?>) instance;
                        //获取 initializer 的依赖项
                        List<Class<? extends Initializer<?>>> dependencies =
                                initializer.dependencies();

                        //如果 initializer 的依赖项 dependencies 不为空
                        //则遍历 dependencies 每个 item 进行初始化
                        if (!dependencies.isEmpty()) {
                            for (Class<? extends Initializer<?>> clazz : dependencies) {
                                if (!mInitialized.containsKey(clazz)) {
                                    doInitialize(clazz, initializing);
                                }
                            }
                        }
                        if (StartupLogger.DEBUG) {
                            StartupLogger.i(String.format("Initializing %s", component.getName()));
                        }
                        //进行初始化
                        result = initializer.create(mContext);
                        if (StartupLogger.DEBUG) {
                            StartupLogger.i(String.format("Initialized %s", component.getName()));
                        }
                        //将已经进行初始化的 component 从 initializing 中移除掉
                        //避免误判循环依赖
                        initializing.remove(component);
                        //将初始化结果保存起来
                        mInitialized.put(component, result);
                    } catch (Throwable throwable) {
                        throw new StartupException(throwable);
                    }
                } else {
                    //component 指向的 Initializer 已经进行初始化
                    //此处直接获取缓存值直接返回即可
                    result = mInitialized.get(component);
                }
                return (T) result;
            } finally {
                Trace.endSection();
            }
        }
    }
```

### 五、App Startup 的不足点

App Startup 的优点我在上边已经列举了，最后再来列举下它的几个不足点

1. InitializationProvider 的 `onCreate()` 函数是在主线程被调用的，导致我们的每个 Initializer 默认就都是运行在主线程，这对于某些初始化时间过长，需要运行在子线程的组件来说就不太适用了。且 Initializer 的 `create(context: Context)` 函数的本意是完成组件的初始化并返回初始化的结果值，如果在此处通过主动 new Thread 来运行耗时组件的初始化，那么我们就无法返回有意义的结果值，间接导致后续也无法通过 AppInitializer 获取到缓存的初始化结果值
2. 如果某组件的初始化需要依赖于其它耗时组件（初始化时间过长，需要运行在子线程）的结果值，此时 App Startup  一样不适用
3. 对于已经使用 ContentProvider 完成初始化逻辑的第三方依赖库，我们一般也无法直接修改其初始化逻辑（除非 clone 该项目导到本地直接修改源码），所以在初始阶段 App Startup 的意义主要在于统一项目本地组件的初始化入口，需要等到  App Startup 被大多数开发者接受并使用后，才更加具有性能优势