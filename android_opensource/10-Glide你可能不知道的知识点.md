> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

> 对于 Android Developer 来说，很多开源库都是属于**开发必备**的知识点，从使用方式到实现原理再到源码解析，这些都需要我们有一定程度的了解和运用能力。所以我打算来写一系列关于开源库**源码解析**和**实战演练**的文章，初定的目标是 **EventBus、ARouter、LeakCanary、Retrofit、Glide、OkHttp、Coil** 等七个知名开源库，希望对你有所帮助 🤣🤣

# 一、利用 AppGlideModule 实现默认配置

在大多数情况下 Glide 的默认配置就已经能够满足我们的需求了，像缓存池大小，磁盘缓存策略等都不需要我们主动去设置，但 Glide 也提供了 AppGlideModule 让开发者可以去实现自定义配置。对于一个 App 来说，在加载图片的时候一般都是使用同一张 placeholder，如果每次加载图片时都需要来手动设置一遍的话就显得很多余了，此时就可以通过 AppGlideModule 来设置默认的 placeholder

首先需要继承于 AppGlideModule，在 `applyOptions`方法中设置配置参数，然后为实现类添加 @GlideModule 注解，这样在编译阶段 Glide 就可以通过 APT 解析到我们的这一个实现类，然后将我们的配置参数设置为默认值

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 */
@GlideModule
class MyAppGlideModule : AppGlideModule() {

    //用于控制是否需要从 Manifest 文件中解析配置文件
    override fun isManifestParsingEnabled(): Boolean {
        return false
    }

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        builder.setDiskCache(
            //配置磁盘缓存目录和最大缓存
            DiskLruCacheFactory(
                (context.externalCacheDir ?: context.cacheDir).absolutePath,
                "imageCache",
                1024 * 1024 * 50
            )
        )
        builder.setDefaultRequestOptions {
            return@setDefaultRequestOptions RequestOptions()
                .placeholder(android.R.drawable.ic_menu_upload_you_tube)
                .error(android.R.drawable.ic_menu_call)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .format(DecodeFormat.DEFAULT)
                .encodeQuality(90)
        }
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {

    }

}
```

在编译后，我们的工程目录中就会自动生成 GeneratedAppGlideModuleImpl 这个类，该类就包含了 MyAppGlideModule

```java
@SuppressWarnings("deprecation")
final class GeneratedAppGlideModuleImpl extends GeneratedAppGlideModule {
  private final MyAppGlideModule appGlideModule;

  public GeneratedAppGlideModuleImpl(Context context) {
    appGlideModule = new MyAppGlideModule();
    if (Log.isLoggable("Glide", Log.DEBUG)) {
      Log.d("Glide", "Discovered AppGlideModule from annotation: github.leavesc.glide.MyAppGlideModule");
    }
  }

  @Override
  public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
    appGlideModule.applyOptions(context, builder);
  }

  @Override
  public void registerComponents(@NonNull Context context, @NonNull Glide glide,
      @NonNull Registry registry) {
    appGlideModule.registerComponents(context, glide, registry);
  }

  @Override
  public boolean isManifestParsingEnabled() {
    return appGlideModule.isManifestParsingEnabled();
  }

  @Override
  @NonNull
  public Set<Class<?>> getExcludedModuleClasses() {
    return Collections.emptySet();
  }

  @Override
  @NonNull
  GeneratedRequestManagerFactory getRequestManagerFactory() {
    return new GeneratedRequestManagerFactory();
  }
}
```

在运行阶段，Glide 就会通过反射生成一个 GeneratedAppGlideModuleImpl 对象，然后根据我们的默认配置项来初始化 Glide 实例

```java
 @Nullable
  @SuppressWarnings({"unchecked", "TryWithIdenticalCatches", "PMD.UnusedFormalParameter"})
  private static GeneratedAppGlideModule getAnnotationGeneratedGlideModules(Context context) {
    GeneratedAppGlideModule result = null;
    try {
      //通过反射来生成一个 GeneratedAppGlideModuleImpl 对象
      Class<GeneratedAppGlideModule> clazz =
          (Class<GeneratedAppGlideModule>)
              Class.forName("com.bumptech.glide.GeneratedAppGlideModuleImpl");
      result =
          clazz.getDeclaredConstructor(Context.class).newInstance(context.getApplicationContext());
    } catch (ClassNotFoundException e) {
      if (Log.isLoggable(TAG, Log.WARN)) {
        Log.w(
            TAG,
            "Failed to find GeneratedAppGlideModule. You should include an"
                + " annotationProcessor compile dependency on com.github.bumptech.glide:compiler"
                + " in your application and a @GlideModule annotated AppGlideModule implementation"
                + " or LibraryGlideModules will be silently ignored");
      }
      // These exceptions can't be squashed across all versions of Android.
    } catch (InstantiationException e) {
      throwIncorrectGlideModule(e);
    } catch (IllegalAccessException e) {
      throwIncorrectGlideModule(e);
    } catch (NoSuchMethodException e) {
      throwIncorrectGlideModule(e);
    } catch (InvocationTargetException e) {
      throwIncorrectGlideModule(e);
    }
    return result;
  }


 private static void initializeGlide(
      @NonNull Context context,
      @NonNull GlideBuilder builder,
      @Nullable GeneratedAppGlideModule annotationGeneratedModule) {
    Context applicationContext = context.getApplicationContext();
    ···
    if (annotationGeneratedModule != null) {
      //调用 MyAppGlideModule 的 applyOptions 方法，对 GlideBuilder 进行设置
      annotationGeneratedModule.applyOptions(applicationContext, builder);
    }
    //根据 GlideBuilder 来生成 Glide 实例
    Glide glide = builder.build(applicationContext);
    ···
    if (annotationGeneratedModule != null) {
        //配置自定义组件
        annotationGeneratedModule.registerComponents(applicationContext, glide, glide.registry);
    }
    applicationContext.registerComponentCallbacks(glide);
    Glide.glide = glide;
  }
```

# 二、自定义网络请求组件

默认情况下，Glide 是通过 HttpURLConnection 来进行联网请求图片的，这个过程就由 HttpUrlFetcher 类来实现。HttpURLConnection 相对于我们常用的 OkHttp 来说比较原始低效，我们可以通过使用 Glide 官方提供的`okhttp3-integration`来将网络请求交由 OkHttp 完成

```groovy
dependencies {
    implementation "com.github.bumptech.glide:okhttp3-integration:4.11.0"
}
```

如果想方便后续修改的话，我们也可以将`okhttp3-integration`内的代码复制出来，通过 Glide 开放的 Registry 来注册一个自定义的 OkHttpStreamFetcher，这里我也提供一份 kotlin 版本的示例代码

首先需要继承于 DataFetcher，在拿到 GlideUrl 后完成网络请求，并将请求结果通过 DataCallback 回调出去

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 */
class OkHttpStreamFetcher(private val client: Call.Factory, private val url: GlideUrl) :
    DataFetcher<InputStream>, Callback {

    companion object {
        private const val TAG = "OkHttpFetcher"
    }

    private var stream: InputStream? = null

    private var responseBody: ResponseBody? = null

    private var callback: DataFetcher.DataCallback<in InputStream>? = null

    @Volatile
    private var call: Call? = null

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        val requestBuilder = Request.Builder().url(url.toStringUrl())
        for ((key, value) in url.headers) {
            requestBuilder.addHeader(key, value)
        }
        val request = requestBuilder.build()
        this.callback = callback
        call = client.newCall(request)
        call?.enqueue(this)
    }

    override fun onFailure(call: Call, e: IOException) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "OkHttp failed to obtain result", e)
        }
        callback?.onLoadFailed(e)
    }

    override fun onResponse(call: Call, response: Response) {
        if (response.isSuccessful) {
            responseBody = response.body()
            val contentLength = Preconditions.checkNotNull(responseBody).contentLength()
            stream = ContentLengthInputStream.obtain(responseBody!!.byteStream(), contentLength)
            callback?.onDataReady(stream)
        } else {
            callback?.onLoadFailed(HttpException(response.message(), response.code()))
        }
    }

    override fun cleanup() {
        try {
            stream?.close()
        } catch (e: IOException) {
            // Ignored
        }
        responseBody?.close()
        callback = null
    }

    override fun cancel() {
        call?.cancel()
    }

    override fun getDataClass(): Class<InputStream> {
        return InputStream::class.java
    }

    override fun getDataSource(): DataSource {
        return DataSource.REMOTE
    }

}
```

之后还需要继承于 ModelLoader，提供构建 OkHttpUrlLoader 的入口

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 */
class OkHttpUrlLoader(private val client: Call.Factory) : ModelLoader<GlideUrl, InputStream> {

    override fun buildLoadData(
        model: GlideUrl,
        width: Int,
        height: Int,
        options: Options
    ): LoadData<InputStream> {
        return LoadData(
            model,
            OkHttpStreamFetcher(client, model)
        )
    }

    override fun handles(model: GlideUrl): Boolean {
        return true
    }

    class Factory(private val client: Call.Factory) : ModelLoaderFactory<GlideUrl, InputStream> {

        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<GlideUrl, InputStream> {
            return OkHttpUrlLoader(client)
        }

        override fun teardown() {
            // Do nothing, this instance doesn't own the client.
        }

    }

}
```

最后注册 OkHttpUrlLoader ，之后 GlideUrl 类型的请求都会交由其处理

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 */
@GlideModule
class MyAppGlideModule : AppGlideModule() {

    override fun isManifestParsingEnabled(): Boolean {
        return false
    }

    override fun applyOptions(context: Context, builder: GlideBuilder) {

    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.replace(
            GlideUrl::class.java,
            InputStream::class.java,
            OkHttpUrlLoader.Factory(OkHttpClient())
        )
    }

}
```

# 三、实现图片加载进度监听

对于某些高清图片来说，可能一张就是十几MB甚至上百MB大小了，如果没有进度条的话用户可能就会等得有点难受了，这里我就提供一个基于 **OkHttp 拦截器**实现的监听图片加载进度的方法

首先需要对 OkHttp 原始的 ResponseBody 进行一层包装，在内部根据 **contentLength** 和**已读取到的流字节数**来计算当前进度值，然后向外部提供通过 imageUrl 来注册 ProgressListener 的入口

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 */
internal class ProgressResponseBody constructor(
    private val imageUrl: String,
    private val responseBody: ResponseBody?
) : ResponseBody() {

    interface ProgressListener {

        fun update(progress: Int)

    }

    companion object {

        private val progressMap = mutableMapOf<String, WeakReference<ProgressListener>>()

        fun addProgressListener(url: String, listener: ProgressListener) {
            progressMap[url] = WeakReference(listener)
        }

        fun removeProgressListener(url: String) {
            progressMap.remove(url)
        }

        private const val CODE_PROGRESS = 100

        private val mainHandler by lazy {
            object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    if (msg.what == CODE_PROGRESS) {
                        val pair = msg.obj as Pair<String, Int>
                        val progressListener = progressMap[pair.first]?.get()
                        progressListener?.update(pair.second)
                    }
                }
            }
        }

    }

    private var bufferedSource: BufferedSource? = null

    override fun contentType(): MediaType? {
        return responseBody?.contentType()
    }

    override fun contentLength(): Long {
        return responseBody?.contentLength() ?: -1
    }

    override fun source(): BufferedSource {
        if (bufferedSource == null) {
            bufferedSource = source(responseBody!!.source()).buffer()
        }
        return bufferedSource!!
    }

    private fun source(source: Source): Source {
        return object : ForwardingSource(source) {

            var totalBytesRead = 0L

            @Throws(IOException::class)
            override fun read(sink: Buffer, byteCount: Long): Long {
                val bytesRead = super.read(sink, byteCount)
                totalBytesRead += if (bytesRead != -1L) {
                    bytesRead
                } else {
                    0
                }
                val contentLength = contentLength()
                val progress = when {
                    bytesRead == -1L -> {
                        100
                    }
                    contentLength != -1L -> {
                        ((totalBytesRead * 1.0 / contentLength) * 100).toInt()
                    }
                    else -> {
                        0
                    }
                }
                mainHandler.sendMessage(Message().apply {
                    what = CODE_PROGRESS
                    obj = Pair(imageUrl, progress)
                })
                return bytesRead
            }
        }
    }

}
```

然后在 Interceptor 中使用 ProgressResponseBody 对原始的 ResponseBody 多进行一层包装，将我们的 ProgressResponseBody 作为一个代理，之后再将 ProgressInterceptor 添加给 OkHttpClient 即可

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 */
class ProgressInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val originalResponse = chain.proceed(request)
        val url = request.url.toString()
        return originalResponse.newBuilder()
            .body(ProgressResponseBody(url, originalResponse.body))
            .build()
    }

}
```

最终实现的效果：

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/dc45cc83a1f745af92317d2d735a9ad6~tplv-k3u1fbpfcp-zoom-1.image)

# 四、自定义磁盘缓存  key

在某些时候，我们拿到的图片 Url 可能是带有时效性的，需要在 Url 的尾部加上一个 token 值，在指定时间后 token 就会失效，防止图片被盗链。这种类型的 Url 在一定时间内就需要更换 token 才能拿到图片，可是 Url 的变化就会导致 Glide 的磁盘缓存机制完全失效

```kotlin
https://images.pexels.com/photos/1425174/pexels-photo-1425174.jpeg?auto=compress&cs=tinysrgb&dpr=2&h=750&w=1260&token=tokenValue
```

从我的上篇文章内容可以知道，一张图片在进行磁盘缓存时必定会同时对应一个唯一 Key，这样 Glide 在后续加载同样的图片时才能复用已有的缓存文件。对于一张网络图片来说，其唯一 Key 的生成就依赖于 GlideUrl 类的 `getCacheKey()`方法，该方法会直接返回网络图片的 Url 字符串。如果 Url 的 token 值会一直变化，那么 Glide 就无法对应上同一张图片了，导致磁盘缓存完全失效

```java
/**
 * @Author: leavesCZY
 * @Desc:
 * @Github：https://github.com/leavesCZY
 */
public class GlideUrl implements Key {
    
  @Nullable private final String stringUrl;
    
  public GlideUrl(String url) {
    this(url, Headers.DEFAULT);
  }

  public GlideUrl(String url, Headers headers) {
    this.url = null;
    this.stringUrl = Preconditions.checkNotEmpty(url);
    this.headers = Preconditions.checkNotNull(headers);
  }
    
  public String getCacheKey() {
    return stringUrl != null ? stringUrl : Preconditions.checkNotNull(url).toString();
  }
    
}
```

想要解决这个问题，就需要来手动定义磁盘缓存时的唯一 Key。这可以通过继承 GlideUrl，修改`getCacheKey()`方法的返回值来实现，将 Url 移除 token 键值对后的字符串作为缓存 Key 即可

```kotlin
/**
 * @Author: leavesCZY
 * @Desc:
 * @Github：https://github.com/leavesCZY
 */
class TokenGlideUrl(private val selfUrl: String) : GlideUrl(selfUrl) {

    override fun getCacheKey(): String {
        val uri = URI(selfUrl)
        val querySplit = uri.query.split("&".toRegex())
        querySplit.forEach {
            val kv = it.split("=".toRegex())
            if (kv.size == 2 && kv[0] == "token") {
                //将包含 token 的键值对移除
                return selfUrl.replace(it, "")
            }
        }
        return selfUrl
    }

}
```

然后在加载图片的时候使用 TokenGlideUrl 来传递图片 Url 即可

```kotlin
Glide.with(Context).load(TokenGlideUrl(ImageUrl)).into(ImageView)
```

# 五、如何直接拿到图片

如果想直接取得 Bitmap 而非显示在 ImageView 上的话，可以用以下同步请求的方式来获得 Bitmap。需要注意的是，`submit()`方法就会触发 Glide 去请求图片，此时请求操作还是运行于 Glide 内部的线程池的，但 `get()`操作就会直接阻塞所在线程，直到图片加载结束（不管成功与否）才会返回

```kotlin
thread {
    val futureTarget = Glide.with(this)
        .asBitmap()
        .load(url)
        .submit()
    val bitmap = futureTarget.get()
    runOnUiThread {
        iv_tokenUrl.setImageBitmap(bitmap)
    }
}
```

也可以用类似的方式来拿到 File 或者 Drawable

```kotlin
thread {
    val futureTarget = Glide.with(this)
        .asFile()
        .load(url)
        .submit()
    val file = futureTarget.get()
    runOnUiThread {
        showToast(file.absolutePath)
    }
}
```

Glide 也提供了以下的异步加载方式

```kotlin
Glide.with(this)
    .asBitmap()
    .load(url)
    .into(object : CustomTarget<Bitmap>() {
        override fun onLoadCleared(placeholder: Drawable?) {
            showToast("onLoadCleared")
        }

        override fun onResourceReady(
            resource: Bitmap,
            transition: Transition<in Bitmap>?
        ) {
            iv_tokenUrl.setImageBitmap(resource)
        }
    })
```

# 六、Glide 如何实现网络监听

在上篇文章我有讲到，RequestTracker 就用于存储所有加载图片的任务，并提供了**开始、暂停和重启**所有任务的方法，一个常见的需要重启任务的情形就是用户的网络**从无信号状态恢复正常了**，此时就应该自动重启所有未完成的任务

```java
ConnectivityMonitor  connectivityMonitor =
        factory.build(
            context.getApplicationContext(),
            new RequestManagerConnectivityListener(requestTracker));  


private class RequestManagerConnectivityListener
      implements ConnectivityMonitor.ConnectivityListener {
    @GuardedBy("RequestManager.this")
    private final RequestTracker requestTracker;

    RequestManagerConnectivityListener(@NonNull RequestTracker requestTracker) {
      this.requestTracker = requestTracker;
    }

    @Override
    public void onConnectivityChanged(boolean isConnected) {
      if (isConnected) {
        synchronized (RequestManager.this) {
          //重启未完成的任务
          requestTracker.restartRequests();
        }
      }
    }
  }
```

可以看出来，RequestManagerConnectivityListener 本身就只是一个回调函数，重点还需要看 ConnectivityMonitor 是如何实现的。ConnectivityMonitor 实现类就在 DefaultConnectivityMonitorFactory 中获取，内部会判断当前应用是否具有 `NETWORK_PERMISSION` 权限，如果没有的话则返回一个空实现 NullConnectivityMonitor，有权限的话就返回 DefaultConnectivityMonitor，在内部根据 ConnectivityManager 来判断当前的网络连接状态

```java
public class DefaultConnectivityMonitorFactory implements ConnectivityMonitorFactory {
  private static final String TAG = "ConnectivityMonitor";
  private static final String NETWORK_PERMISSION = "android.permission.ACCESS_NETWORK_STATE";

  @NonNull
  @Override
  public ConnectivityMonitor build(
      @NonNull Context context, @NonNull ConnectivityMonitor.ConnectivityListener listener) {
    int permissionResult = ContextCompat.checkSelfPermission(context, NETWORK_PERMISSION);
    boolean hasPermission = permissionResult == PackageManager.PERMISSION_GRANTED;
    if (Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(
          TAG,
          hasPermission
              ? "ACCESS_NETWORK_STATE permission granted, registering connectivity monitor"
              : "ACCESS_NETWORK_STATE permission missing, cannot register connectivity monitor");
    }
    return hasPermission
        ? new DefaultConnectivityMonitor(context, listener)
        : new NullConnectivityMonitor();
  }
}
```

DefaultConnectivityMonitor 的逻辑比较简单，不过多赘述。我觉得比较有价值的一点是：Glide 由于使用人数众多，有比较多的开发者会反馈 issues，DefaultConnectivityMonitor 内部就对各种可能抛出 Exception 的情况进行了捕获，这样相对来说会比我们自己实现的逻辑要考虑周全得多，所以我就把 DefaultConnectivityMonitor 复制出来转为 kotlin 以便后续自己复用了

```kotlin
/**
 * @Author: leavesCZY
 * @Desc:
 */
internal interface ConnectivityListener {
    fun onConnectivityChanged(isConnected: Boolean)
}

internal class DefaultConnectivityMonitor(
    context: Context,
    val listener: ConnectivityListener
) {

    private val appContext = context.applicationContext

    private var isConnected = false

    private var isRegistered = false

    private val connectivityReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val wasConnected = isConnected
            isConnected = isConnected(context)
            if (wasConnected != isConnected) {
                listener.onConnectivityChanged(isConnected)
            }
        }
    }

    private fun register() {
        if (isRegistered) {
            return
        }
        // Initialize isConnected.
        isConnected = isConnected(appContext)
        try {
            appContext.registerReceiver(
                connectivityReceiver,
                IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            )
            isRegistered = true
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun unregister() {
        if (!isRegistered) {
            return
        }
        appContext.unregisterReceiver(connectivityReceiver)
        isRegistered = false
    }

    @SuppressLint("MissingPermission")
    private fun isConnected(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true
        val networkInfo = try {
            connectivityManager.activeNetworkInfo
        } catch (e: RuntimeException) {
            return true
        }
        return networkInfo != null && networkInfo.isConnected
    }

    fun onStart() {
        register()
    }

    fun onStop() {
        unregister()
    }

}
```

# 七、GitHub

关于 Glide 的知识点扩展也介绍完了，上述的所有示例代码我也都放到 GitHub 了，欢迎 star：[AndroidOpenSourceDemo](https://github.com/leavesCZY/AndroidOpenSourceDemo)