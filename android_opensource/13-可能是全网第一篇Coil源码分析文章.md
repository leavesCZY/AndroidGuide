> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

> 对于 Android Developer 来说，很多开源库都是属于**开发必备**的知识点，从使用方式到实现原理再到源码解析，这些都需要我们有一定程度的了解和运用能力。所以我打算来写一系列关于开源库**源码解析**和**实战演练**的文章，初定的目标是 **EventBus、ARouter、LeakCanary、Retrofit、Glide、OkHttp、Coil** 等七个知名开源库，希望对你有所帮助 🤣🤣

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/259d24d1611d454ea49d453844377df2~tplv-k3u1fbpfcp-zoom-1.image)

Coil 是我最后一个要来分析的开源库，本篇也是我 **Android 主流开源库源码分析** 这个系列的最后一篇文章，包含 Coil 的入门介绍和源码分析。这一个系列的文章从动笔到结束花了要两个月时间，到今天也就结尾了，原创不易，觉得有用就请给个赞吧 😂😂😂

Coil 这个开源库我关注了蛮久的，因为其很多特性在我看来都挺有意思的。Coil 在 2020 年 10 月 22 日才发布了 1.0.0 版本，我当时在网上搜了搜 Coil 的资料，看到的文章都只是入门介绍，没看见到关于源码层次的分析，而本文的发表时间离 1.0.0 版本发布刚好才隔了一个月时间，应该没人比我还早了吧？就斗胆给文章起了这么个标题：**可能是全网第一篇 Coil 源码分析文章** ~

# 一、Coil 是什么

[Coil](https://github.com/coil-kt/coil) 是一个新兴的 Android 图片加载库，使用 Kotlin 协程来加载图片，有以下几个特点：

- **更快**: Coil 在性能上做了很多优化，包括内存缓存和磁盘缓存、对内存中的图片进行采样、复用 Bitmap、支持根据生命周期变化自动暂停和取消图片请求等
- **更轻量级**: Coil 大约会给你的 App 增加两千个方法（前提是你的 App 已经集成了 OkHttp 和 Coroutines），Coil 的方法数和 Picasso 相当，相比 Glide 和 Fresco 要轻量级很多
- **更容易使用**: Coil's API 充分利用了 Kotlin 语言的新特性，简化并减少了很多重复代码
- **更流行**: Coil 首选 Kotlin 语言开发，并且使用包含 Coroutines、OkHttp、Okio 和 AndroidX Lifecycles 在内的更现代化的开源库

Coil 的首字母由来：**Co**routine，**I**mage 和 **L**oader 得到 Coil

# 二、引入 Coil

Coil 要求 **AndroidX、Min SDK 14+、Java 8+** 环境

要启用 Java 8，需要在项目的 Gradle 构建脚本中添加如下配置：

Gradle (`.gradle`)：

```groovy
compileOptions {
    sourceCompatibility JavaVersion.VERSION_1_8
    targetCompatibility JavaVersion.VERSION_1_8
}

kotlinOptions {
    jvmTarget = "1.8"
}
```

Gradle Kotlin DSL (`.gradle.kts`)：

```groovy
compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlinOptions {
    jvmTarget = "1.8"
}
```

Coil 一共包含五个组件，可以在 `mavenCentral()`上获取到

- `io.coil-kt:coil-base`。基础组件，提供了基本的图片请求、图片解码、图片缓存、Bitmap 复用等功能
- `io.coil-kt:coil`。默认组件，依赖于`io.coil-kt:coil-base`，提供了 Coil 类的单例对象以及 ImageView 相关的扩展函数
- `io.coil-kt:coil-gif`。包含两个 [decoder](https://coil-kt.github.io/coil/api/coil-base/coil.decode/-decoder/) 用于支持解码 GIFs，有关更多详细信息，请参见 [GIF](https://coil-kt.github.io/coil/gifs/)
- `io.coil-kt:coil-svg`。包含一个 [decoder](https://coil-kt.github.io/coil/api/coil-base/coil.decode/-decoder) 用于支持解码 SVG。有关更多详细信息，请参见 [SVG](https://coil-kt.github.io/coil/svgs/)
- `io.coil-kt:coil-video`。包含两个 [fetchers](https://coil-kt.github.io/coil/api/coil-base/coil.fetch/-fetcher) 用于支持读取和解码 [任何 Android 的支持的视频格式](https://developer.android.com/guide/topics/media/media-formats#video-codecs) 的视频帧。有关更多详细信息，请参见 [videos](https://coil-kt.github.io/coil/videos/)

引入如下依赖就包含了 Coil 最基础的图片加载功能

```kotlin
implementation("io.coil-kt:coil:1.0.0")
```

如果想要显示 **Gif、SVG、视频帧**等类型的图片，则需要额外引入对应的支持库：

```kotlin
implementation("io.coil-kt:coil-gif:1.0.0")
implementation("io.coil-kt:coil-svg:1.0.0")
implementation("io.coil-kt:coil-video:1.0.0")
```

# 三、快速入门

## 1、load

要将图片显示到 ImageView 上，直接使用`ImageView`的扩展函数`load`即可

```kotlin
// URL
imageView.load("https://www.example.com/image.jpg")

// Resource
imageView.load(R.drawable.image)

// File
imageView.load(File("/path/to/image.jpg"))

// And more...
```

使用可选的 lambda 块来添加配置项

```kotlin
imageView.load("https://www.example.com/image.jpg") {
    crossfade(true) //淡入淡出
    placeholder(R.drawable.image) //占位图
    transformations(CircleCropTransformation()) //图片变换，将图片转为圆形
}
```

## 2、ImageRequest

如果要将图片加载到自定义的 target 中，可以通过 ImageRequest.Builder 来构建 ImageRequest 实例，并将请求提交给 ImageLoader

```kotlin
val request = ImageRequest.Builder(context)
    .data("https://www.example.com/image.jpg")
    .target { drawable ->
        // Handle the result.
    }
    .build()
context.imageLoader.enqueue(request)
```

## 3、ImageLoader

`imageView.load`使用单例对象 imageLoader 来执行 ImageRequest，可以使用 Context 的扩展函数来访问 ImageLoader

```kotlin
val imageLoader = context.imageLoader
```

可选地，你也可以构建自己的 ImageLoader 实例，并赋值给 Coil 来实现全局使用

```kotlin
Coil.setImageLoader(
    ImageLoader.Builder(application)
        .placeholder(ActivityCompat.getDrawable(application, R.drawable.icon_loading))
        .error(ActivityCompat.getDrawable(application, R.drawable.icon_error))
        .build()
)
```

## 4、execute

如果想直接拿到目标图片，可以调用 ImageLoader 的`execute`方法来实现

```kotlin
val request = ImageRequest.Builder(context)
    .data("https://www.example.com/image.jpg")
    .build()
val drawable = imageLoader.execute(request).drawable
```

## 5、R8 / Proguard

Coil 开箱即用，与 R8 完全兼容，不需要添加任何额外规则

如果你使用了 Proguard，你可能需要添加对应的混淆规则：[Coroutines](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/resources/META-INF/proguard/coroutines.pro)、[OkHttp](https://github.com/square/okhttp/blob/master/okhttp/src/main/resources/META-INF/proguard/okhttp3.pro) and [Okio](https://github.com/square/okio/blob/master/okio/src/jvmMain/resources/META-INF/proguard/okio.pro)。

## 6、License

```groovy
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

# 四、大体框架

Coil 在我看来是一个比较“**激进**”的开源库，热衷于使用当前最为流行的技术，包括 Coroutines、OkHttp、Okio，以及 Google 官方的 Jetpack Lifecycles、AndroidX 等，代码不仅完全由 Kotlin 语言来实现，连 gradle 脚本也是全部使用 kts，而且 gradle 版本也升级得很快，我一开始由于使用的 Android Studio 不是 4.x 版本，连 Coil 代码都跑不起来 =_=

**如果你的项目中已经大面积使用到了 Jetpack、Kotlin Coroutines、OkHttp 的话，那么 Coil 会更加契合你的项目**

当前在 Android 端最为流行的图片加载框架应该是 Glide 了，Coil 作为一个后起之秀相对 Glide 也有着一些独特的优势。例如，为了监听 UI 层的生命周期变化，Glide 是通过向 Activity 或者 Fragment 注入一个无 UI 界面的 Fragment 来实现间接监听的，而 Coil 则只需要直接监听 Lifecycle 即可，在实现方式上 Coil 会更加简单高效。此外，在联网请求图片的时候，Glide 需要通过线程池和多个回调来完成最终图片的显示，而 Coil 由于使用了 Kotlin 协程，可以很简洁地完成异步加载和线程切换，在流程上 Coil 会清晰很多。但实际上 Coil 也是借鉴了一些优秀开源库的实现思路，所以我看 Coil 的源码的时候就总会发现一些 Glide 和 OkHttp 的影子😅😅

这里就先来对 Coil 的各个特性和 Glide 做下简单的对比，先让读者有个大体的印象

1. 实现语言
   - Glide 全盘使用 Java 语言来实现，对于 Java 和 Kotlin 语言的友好程度差不多
   - Coil 全盘使用 Kotlin 语言来实现，为 ImageView 声明了多个用于加载图片的扩展函数，对 Kotlin 语言的友好程度会更高很多
2. 网络请求
   - Glide 默认是使用 HttpURLConnection，但也提供了更换网络请求实现途径的入口
   - Coil 默认是使用 OkHttp，但也提供了更换网络请求实现途径的入口
3. 生命周期监听
   - Glide 通过向 Activity 或者 Fragment 注入一个无 UI 界面的 Fragment 来实现监听
   - Coil 直接通过 Lifecycle 来实现监听
4. 内存缓存
   - Glide 的内存缓存分为 ActiveResources 和 MemoryCache 两级
   - Coil 的内存缓存分为 WeakMemoryCache 和 StrongMemoryCache 两级，本质上和 Glide 一样
5. 磁盘缓存
   - Glide 在加载到图片后通过 DiskLruCache 来进行磁盘缓存，且提供了**是否缓存、是否缓存原始图片、是否缓存转换过后的图片**等多个选择
   - Coil 通过 OkHttp 的网络请求缓存机制来实现磁盘缓存，且磁盘缓存只对通过网络请求加载到的原始图片生效，不缓存其它来源的图片和转换过后的图片
6. 网络缓存
   - Glide 不存在这个概念
   - Coil 相比 Glide 多出了一层网络缓存，可用于实现**不进行网络加载，而是强制使用本地缓存**（当然，如果本地缓存不存在的话就会报错）
7. 线程框架
   - Glide 使用原生的 ThreadPoolExecutor 来完成后台任务，通过 Handler 来实现线程切换
   - Coil 使用 Coroutines 来完成后台任务及线程切换

> 关于 Glide 的源码解析可以看我的这两篇文章：
>
> - [三方库源码笔记（9）-超详细的 Glide 源码详解](https://juejin.im/post/6891307560557608967)
> - [三方库源码笔记（10）-Glide 你可能不知道的知识点](https://juejin.im/post/6892751013544263687)

我看源码时习惯从最基础的使用方式来入手，分析其整个调用链关系和会涉及到的模块，这里也不例外，就从 Coil 加载一张网络图片来入手

最简单的加载方式只需要调用一个`load`方法即可，比 Glide 还简洁，想要添加配置项的话就在 lambda 块中添加

```kotlin
//直接加载图片，不添加任何配置项
imageView.load(imageUrl)

//在 lambda 块中添加配置项
imageView.load(imageUrl) {
    crossfade(true) //淡入淡出
    placeholder(android.R.drawable.presence_away) //占位图
    error(android.R.drawable.stat_notify_error) //图片加载失败时显示的图
    transformations(
        CircleCropTransformation() //将图片显示为圆形
    )
}
```

Coil 为 ImageView 声明了多个用于加载图片的扩展函数，均命名为 `load`，默认情况下我们只需要传一个图片来源地址即可，支持 **String、HttpUrl、Uri、File、Int、Drawable、Bitmap** 等多种入参类型

```kotlin
/** @see ImageView.loadAny */
@JvmSynthetic
inline fun ImageView.load(
    uri: String?,
    imageLoader: ImageLoader = context.imageLoader,
    builder: ImageRequest.Builder.() -> Unit = {}
): Disposable = loadAny(uri, imageLoader, builder)
```

不管传入的是什么类型的参数，最终都会中转调用到 `loadAny` 方法，通过 Builder 模式构建出本次的请求参数 ImageRequest，然后将 ImageRequest 提交给 ImageLoader，由其来完成图片的加载，最终返回一个 Disposable 对象

```kotlin
@JvmSynthetic
inline fun ImageView.loadAny(
    data: Any?,
    imageLoader: ImageLoader = context.imageLoader,
    builder: ImageRequest.Builder.() -> Unit = {}
): Disposable {
    val request = ImageRequest.Builder(context)
        .data(data)
        .target(this)
        .apply(builder)
        .build()
    return imageLoader.enqueue(request)
}
```

所以，一个简单的 `load` 方法就已经使用到了以下几个类：

1. ImageRequest。图片的请求参数
2. Disposable。用于取消图片加载或者等待图片加载完成
3. ImageLoader。向其提交 ImageRequest ，由其完成图片的加载

下面就来分析下这一整个流程

# 五、ImageRequest 

ImageRequest 基于 Builder 模式来构建，包含了加载图片时的各个配置项，其配置项很多，重点看前九个

```kotlin
private val context: Context //外部传入的 Context，例如 ImageView 包含的 Context
private var data: Any? //图片地址
private var target: Target? //图片加载成功后的接收类
private var lifecycle: Lifecycle? //ImageView 关联的生命周期
private var memoryCachePolicy: CachePolicy? //内存缓存配置
private var diskCachePolicy: CachePolicy? //磁盘缓存配置
private var networkCachePolicy: CachePolicy? //网络缓存配置
private var fetcher: Pair<Fetcher<*>, Class<*>>? //完成图片加载的处理器
private var decoder: Decoder? //完成图片转码的转换器

private var defaults: DefaultRequestOptions
private var listener: Listener?
private var memoryCacheKey: MemoryCache.Key?
private var placeholderMemoryCacheKey: MemoryCache.Key?
private var colorSpace: ColorSpace? = null
private var transformations: List<Transformation>
private var headers: Headers.Builder?
private var parameters: Parameters.Builder?
private var sizeResolver: SizeResolver?
private var scale: Scale?
private var dispatcher: CoroutineDispatcher?
private var transition: Transition?
private var precision: Precision?
private var bitmapConfig: Bitmap.Config?
private var allowHardware: Boolean?
private var allowRgb565: Boolean?
@DrawableRes private var placeholderResId: Int?
private var placeholderDrawable: Drawable?
@DrawableRes private var errorResId: Int?
private var errorDrawable: Drawable?
@DrawableRes private var fallbackResId: Int?
private var fallbackDrawable: Drawable?
private var resolvedLifecycle: Lifecycle?
private var resolvedSizeResolver: SizeResolver?
private var resolvedScale: Scale?
```

## 1、Target

Target 即最终图片的接收载体，ImageRequest 提供了 `target` 方法用于把 ImageView 包装为 Target 。如果最终图片的接收载体不是 ImageView 的话，就需要开发者自己来实现 Target 接口

```kotlin
fun target(imageView: ImageView) = target(ImageViewTarget(imageView))

fun target(target: Target?) = apply {
	this.target = target
	resetResolvedValues()
}
```

Target 接口提供了**图片开始加载、图片加载失败、图片加载成功**的事件回调，主要是为了显示**占位图、错误图、目标图**等几个

```kotlin
interface Target {
    /**
     * Called when the request starts.
     */
    @MainThread
    fun onStart(placeholder: Drawable?) {}

    /**
     * Called if an error occurs while executing the request.
     */
    @MainThread
    fun onError(error: Drawable?) {}

    /**
     * Called if the request completes successfully.
     */
    @MainThread
    fun onSuccess(result: Drawable) {}
}
```

ImageViewTarget 就是通过调用 `setImageDrawable` 来显式各个状态的图片，同时也实现了 DefaultLifecycleObserver 接口，意味着 ImageViewTarget 本身就**具备了监听生命周期事件的能力**

```kotlin
/** A [Target] that handles setting images on an [ImageView]. */
open class ImageViewTarget(
    override val view: ImageView
) : PoolableViewTarget<ImageView>, TransitionTarget, DefaultLifecycleObserver {

    private var isStarted = false

    override val drawable: Drawable? get() = view.drawable

    override fun onStart(placeholder: Drawable?) = setDrawable(placeholder)

    override fun onError(error: Drawable?) = setDrawable(error)

    override fun onSuccess(result: Drawable) = setDrawable(result)
    
    /** Replace the [ImageView]'s current drawable with [drawable]. */
    protected open fun setDrawable(drawable: Drawable?) {
        (view.drawable as? Animatable)?.stop()
        view.setImageDrawable(drawable)
        updateAnimation()
    }

	···
}
```

## 2、Lifecycle

每个 ImageRequest 都会关联一个 Context 对象，如果外部传入的是 ImageView，则会取 ImageView 内部的 Context。Coil 会判断 Context 是否属于 LifecycleOwner 类型，是的话则可以拿到**和 Activity 或者 Fragment 关联的 Lifecycle**，否则最终取 **GlobalLifecycle**

和 Activity 或者 Fragment 关联的 Lifecycle 才**具备有生命周期感知能力**，这样 Coil 才可以在 Activity 处于后台或者已经销毁的时候暂停任务或者停止任务。而 GlobalLifecycle 会默认且一直处于 RESUMED 状态，这样任务就会一直运行直到最终结束，这可能导致内存泄露

```kotlin
private fun resolveLifecycle(): Lifecycle {
	val target = target
	val context = if (target is ViewTarget<*>) target.view.context else context
    //context 属于 LifecycleOwner 类型则返回对应的 Lifecycle，否则返回 GlobalLifecycle
	return context.getLifecycle() ?: GlobalLifecycle
}

internal object GlobalLifecycle : Lifecycle() {

    private val owner = LifecycleOwner { this }

    override fun addObserver(observer: LifecycleObserver) {
        require(observer is DefaultLifecycleObserver) {
            "$observer must implement androidx.lifecycle.DefaultLifecycleObserver."
        }

        // Call the lifecycle methods in order and do not hold a reference to the observer.
        observer.onCreate(owner)
        observer.onStart(owner)
        observer.onResume(owner)
    }

    override fun removeObserver(observer: LifecycleObserver) {}

    override fun getCurrentState() = State.RESUMED

    override fun toString() = "coil.request.GlobalLifecycle"
}
```

## 3、CachePolicy

和 Glide 一样，Coil 也具备了多级缓存的能力，即**内存缓存 memoryCachePolicy、磁盘缓存 diskCachePolicy、网络缓存 networkCachePolicy**。这些缓存功能是否开启都是通过 CachePolicy 来定义，默认三级缓存全部可读可写

```kotlin
enum class CachePolicy(
    val readEnabled: Boolean,
    val writeEnabled: Boolean
) {
    ENABLED(true, true), //可读可写
    READ_ONLY(true, false), //只读
    WRITE_ONLY(false, true), //只写
    DISABLED(false, false) //不可读不可写，即禁用
}
```

## 4、Fetcher

Fetcher 是**根据图片来源地址转换为目标数据类型**的转换器。例如，我们传入了 Int 类型的 drawableResId，想要以此拿到 Drawable，那么这里的 `Class<*>` 即 `Class<Int>` ，`Fetcher<*>` 即 `Fetcher<Drawable>`

```kotlin
/** @see Builder.fetcher */
val fetcher: Pair<Fetcher<*>, Class<*>>?,
```

Fetcher 接口包含三个方法

```kotlin
interface Fetcher<T : Any> {

    /**
     * 如果能处理 data 则返回 true
     */
    fun handles(data: T): Boolean = true

    /**
     * 根据 data 来计算用于内存缓存时的唯一 key
     * 具有相同 key 的缓存将被 MemoryCache 视为相同的数据
     * 如果返回 null 则不会将 fetch 后的数据缓存到内存中
     */
    fun key(data: T): String?

    /**
     * 根据 data 将目标图片加载到内存中
     */
    suspend fun fetch(
        pool: BitmapPool,
        data: T,
        size: Size,
        options: Options
    ): FetchResult
}
```

Coil 默认提供了以下八种类型的 Fetcher，分别用于处理 **HttpUriUri、HttpUriUrl、File、Asset、ContentUri、Resource、Drawable、Bitmap** 等类型的图片来源地址

```kotlin
private val registry = componentRegistry.newBuilder()
    ···
    // Fetchers
    .add(HttpUriFetcher(callFactory))
    .add(HttpUrlFetcher(callFactory))
    .add(FileFetcher(addLastModifiedToFileCacheKey))
    .add(AssetUriFetcher(context))
    .add(ContentUriFetcher(context))
    .add(ResourceUriFetcher(context, drawableDecoder))
    .add(DrawableFetcher(drawableDecoder))
    .add(BitmapFetcher())
    ···
    .build()
```

## 5、Decoder

Decoder 接口用于提供将 BufferedSource 转码为 Drawable 的能力，BufferedSource 就对应着不同类型的图片资源

Coil 提供了以下几个 Decoder 实现类

- BitmapFactoryDecoder。用于实现 Bitmap 转码
- GifDecoder、ImageDecoderDecoder。用于实现 Gif、Animated WebPs、Animated HEIFs 转码
- SvgDecoder。用于实现 Svg 转码

```kotlin
interface Decoder {
    //如果此 Decoder 能够处理 source 则返回 true
    fun handles(source: BufferedSource, mimeType: String?): Boolean
    
    //用于将 source 解码为 Drawable
    suspend fun decode(
        pool: BitmapPool,
        source: BufferedSource,
        size: Size,
        options: Options
    ): DecodeResult
}
```

# 六、Disposable

Disposable 是我们调用 `load` 方法后的返回值，为外部提供用于**取消图片加载**或者**等待图片加载完成**的方法

```kotlin
interface Disposable {
    //如果任务已经完成或者取消的话，则返回 true
    val isDisposed: Boolean
    
    //取消正在进行的任务并释放与此任务关联的所有资源
    fun dispose()
    
    //非阻塞式地等待任务结束
    @ExperimentalCoilApi
    suspend fun await()
}
```

由于 Coil 是使用协程来加载图片，所以每个任务都会对应一个 Job

如果 ImageRequest 包含的 Target 对应着某个 View（即属于 ViewTarget 类型），那么返回的 Disposable 即 ViewTargetDisposable。而 View 可能需要先后请求多张图片（例如 RecyclerView 的每个 Item 都是 ImageView），那么当启动新任务后旧任务就应该被取消，所以 ViewTargetDisposable 就包含了一个 UUID 来唯一标识每个请求。其它情况就都是返回 BaseTargetDisposable

```kotlin
internal class BaseTargetDisposable(private val job: Job) : Disposable {

    override val isDisposed
        get() = !job.isActive

    override fun dispose() {
        if (isDisposed) return
        job.cancel()
    }

    @ExperimentalCoilApi
    override suspend fun await() {
        if (isDisposed) return
        job.join()
    }
}

internal class ViewTargetDisposable(
    private val requestId: UUID,
    private val target: ViewTarget<*>
) : Disposable {

    override val isDisposed
        get() = target.view.requestManager.currentRequestId != requestId

    override fun dispose() {
        if (isDisposed) return
        target.view.requestManager.clearCurrentRequest()
    }

    @ExperimentalCoilApi
    override suspend fun await() {
        if (isDisposed) return
        target.view.requestManager.currentRequestJob?.join()
    }
}
```

# 七、ImageLoader

上面有说过，`loadAny`方法最终是会通过调用 `imageLoader.enqueue(request)`来发起一个图片加载请求的，那么重点就是要来看 ImageLoader 是如何实现的

```kotlin
@JvmSynthetic
inline fun ImageView.loadAny(
    data: Any?,
    imageLoader: ImageLoader = context.imageLoader,
    builder: ImageRequest.Builder.() -> Unit = {}
): Disposable {
    val request = ImageRequest.Builder(context)
        .data(data)
        .target(this)
        .apply(builder)
        .build()
    return imageLoader.enqueue(request)
}
```

ImageLoader 是一个接口，是承载了所有图片加载任务和实现缓存复用的加载器

```kotlin
interface ImageLoader {
    //用于提供 ImageRequest 的默认配置项
    val defaults: DefaultRequestOptions
    //内存缓存
    val memoryCache: MemoryCache
	//Bitmap缓存池
    val bitmapPool: BitmapPool
	//异步加载图片
    fun enqueue(request: ImageRequest): Disposable
	//同步加载图片
    suspend fun execute(request: ImageRequest): ImageResult
	//停止全部任务
    fun shutdown()
}
```

ImageLoader 的唯一实现类是 RealImageLoader，其`enqueue`方法会启动一个协程，在 job 里执行 `executeMain` 方法得到 ImageResult，ImageResult 就包含了最终得到的图片。同时，job 会被包含在返回的 Disposable 对象里，这样外部才能**取消图片加载**或者**等待图片加载完成**

```kotlin
override fun enqueue(request: ImageRequest): Disposable {
    // Start executing the request on the main thread.
    val job = scope.launch {
        val result = executeMain(request, REQUEST_TYPE_ENQUEUE)
        if (result is ErrorResult) throw result.throwable
    }

    // Update the current request attached to the view and return a new disposable.
    return if (request.target is ViewTarget<*>) {
        val requestId = request.target.view.requestManager.setCurrentRequestJob(job)
        ViewTargetDisposable(requestId, request.target)
    } else {
        BaseTargetDisposable(job)
    }
}
```

executeMain 方法的逻辑也比较简单，可以概括为：

1. 为 target 和 request 创建一个代理类，用于支持 Bitmap 缓存和 Lifecycle 监听
2. 如果外部发起的是异步请求的话（即 REQUEST_TYPE_ENQUEUE），那么就需要等到 Lifecycle 至少处于 Started 状态之后才能继续执行，这样当 Activity 还处于后台时就不会发起请求了
3. 获取占位图并传给 target 
4. 获取 target 需要的图片尺寸大小，以便按需加载，对于 ImageViewTarget 来说，即获取 ImageView 的宽高属性
5. 调用 executeChain 方法拿到 ImageResult，判断是否成功，调用 target 对应的成功或者失败的方法

```kotlin
@MainThread
private suspend fun executeMain(initialRequest: ImageRequest, type: Int): ImageResult {
    ···

    // Apply this image loader's defaults to this request.
    val request = initialRequest.newBuilder().defaults(defaults).build()

    //target 代理，用于支持Bitmap池
    val targetDelegate = delegateService.createTargetDelegate(request.target, type, eventListener)

    //request 代理，用于支持 lifecycle
    val requestDelegate = delegateService.createRequestDelegate(request, targetDelegate, coroutineContext.job)

    try {
        //如果 data 为 null，那么就抛出异常
        if (request.data == NullRequestData) throw NullRequestDataException()

        //如果是异步请求的话，那么就需要等到 Lifecycle 至少处于 Started 状态之后才能继续执行
        if (type == REQUEST_TYPE_ENQUEUE) request.lifecycle.awaitStarted()

        //获取展位图传给 target，从内存缓存中加载或者从全新加载
        val cached = memoryCacheService[request.placeholderMemoryCacheKey]?.bitmap
        try {
            targetDelegate.metadata = null
            targetDelegate.start(cached?.toDrawable(request.context) ?: request.placeholder, cached)
            eventListener.onStart(request)
            request.listener?.onStart(request)
        } finally {
            referenceCounter.decrement(cached)
        }

        //获取 target 需要的图片尺寸大小，按需加载
        eventListener.resolveSizeStart(request)
        val size = request.sizeResolver.size()
        eventListener.resolveSizeEnd(request, size)

        // Execute the interceptor chain.
        val result = executeChain(request, type, size, cached, eventListener)

        // Set the result on the target.
        //判断 result 成功与否，调用相应的方法
        when (result) {
            is SuccessResult -> onSuccess(result, targetDelegate, eventListener)
            is ErrorResult -> onError(result, targetDelegate, eventListener)
        }
        return result
    } catch (throwable: Throwable) {
        if (throwable is CancellationException) {
            onCancel(request, eventListener)
            throw throwable
        } else {
            // Create the default error result if there's an uncaught exception.
            val result = requestService.errorResult(request, throwable)
            onError(result, targetDelegate, eventListener)
            return result
        }
    } finally {
        requestDelegate.complete()
    }
}
```

`executeChain`方法就比较有意思了，有看过 OkHttp 源码的同学应该会对 RealInterceptorChain 有点印象，OkHttp 的拦截器就是通过该同名类来实现的，很显然 Coil 借鉴了 OkHttp 的实现思路，极大方便了后续功能扩展，也给了外部控制整个图片加载流程的入口，可扩展性 +100

> 不了解 OkHttp 的 RealInterceptorChain 实现思路的可以看我的这篇文章，这里不再赘述：[三方库源码笔记（11）-OkHttp 源码详解](https://juejin.cn/post/6895369745445748749)

```kotlin
private val interceptors = registry.interceptors + EngineInterceptor(registry, bitmapPool, referenceCounter,
    strongMemoryCache, memoryCacheService, requestService, systemCallbacks, drawableDecoder, logger)

private suspend inline fun executeChain(
    request: ImageRequest,
    type: Int,
    size: Size,
    cached: Bitmap?,
    eventListener: EventListener
): ImageResult {
    val chain = RealInterceptorChain(request, type, interceptors, 0, request, size, cached, eventListener)
    return if (launchInterceptorChainOnMainThread) {
        chain.proceed(request)
    } else {
        withContext(request.dispatcher) {
            chain.proceed(request)
        }
    }
}
```

所以说，重点就还是要来看 EngineInterceptor 的 `intercept` 方法，其逻辑可以概括为：

1. 找到能处理本次请求的 fetcher，执行下一步
2. 计算本次要加载的图片在内存中的缓存 key，如果内存缓存可用的话就直接使用缓存，结束流程
3. 如果存在内存缓存但是不可用（可能是由于硬件加速配置不符或者是本次不允许使用缓存），那么就更新该缓存在内存中的可用状态并更新引用计数，执行下一步
4. 调用 execute 方法完成图片加载，得到 drawable，结束流程

`execute` 方法的逻辑可以概括为：

1. 通过 fetcher 来执行磁盘加载或者网络请求，得到 fetchResult，执行下一步
2. 如果 fetchResult 属于 DrawableResult 的话，那么就已经拿到目标图片类型 Drawable 了，那么直接返回，结束流程
3. 如果 fetchResult 属于 SourceResult 类型，即拿到的数据类型是 BufferedSource，此时还需要转码为 Drawable，执行下一步
4. 先判断本次请求是否属于**预加载**，即可能外部现在不需要使用到该图片，只是想先将图片缓存到本地磁盘，方便后续能够快速加载。预加载的判断标准就是：**异步请求 + target 为null + 不允许缓存到内存中**。属于预加载的话就不需要将加载到的图片进行转码了，此时就使用 EmptyDecoder，否则就还是需要去找能进行实际转码的 Decoder。拿到 Decoder 后就执行下一步
5. 通过 Decoder 完成图片转码，得到 Drawable，结束流程

```kotlin
/** The last interceptor in the chain which executes the [ImageRequest]. */
internal class EngineInterceptor(
    private val registry: ComponentRegistry,
    private val bitmapPool: BitmapPool,
    private val referenceCounter: BitmapReferenceCounter,
    private val strongMemoryCache: StrongMemoryCache,
    private val memoryCacheService: MemoryCacheService,
    private val requestService: RequestService,
    private val systemCallbacks: SystemCallbacks,
    private val drawableDecoder: DrawableDecoderService,
    private val logger: Logger?
) : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        try {
            // This interceptor uses some internal APIs.
            check(chain is RealInterceptorChain)

            val request = chain.request
            val context = request.context
            val data = request.data
            val size = chain.size
            val eventListener = chain.eventListener

            // Perform any data mapping.
            eventListener.mapStart(request, data)
            val mappedData = registry.mapData(data)
            eventListener.mapEnd(request, mappedData)

            //找到能处理本次请求的 fetcher
            val fetcher = request.fetcher(mappedData) ?: registry.requireFetcher(mappedData)
            //计算本次要加载的图片在内存中的缓存 key
            val memoryCacheKey = request.memoryCacheKey ?: computeMemoryCacheKey(request, mappedData, fetcher, size)
            //如果本次允许使用内存缓存的话，那么就尝试从 memoryCacheService 中获取缓存
            val value = if (request.memoryCachePolicy.readEnabled) memoryCacheService[memoryCacheKey] else null

            // Ignore the cached bitmap if it is hardware-backed and the request disallows hardware bitmaps.
            val cachedDrawable = value?.bitmap
                ?.takeIf { requestService.isConfigValidForHardware(request, it.safeConfig) }
                ?.toDrawable(context)

            //如果缓存可用，则直接返回缓存
            if (cachedDrawable != null && isCachedValueValid(memoryCacheKey, value, request, size)) {
                return SuccessResult(
                    drawable = value.bitmap.toDrawable(context),
                    request = request,
                    metadata = Metadata(
                        memoryCacheKey = memoryCacheKey,
                        isSampled = value.isSampled,
                        dataSource = DataSource.MEMORY_CACHE,
                        isPlaceholderMemoryCacheKeyPresent = chain.cached != null
                    )
                )
            }

            // Fetch, decode, transform, and cache the image on a background dispatcher.
            return withContext(request.dispatcher) {
                //如果 request.data 属于 BitmapDrawable 或者 Bitmap 类型
                //会执行到这里说明 data 不符合本次的使用条件，那么就在内存中将其标记为不可用状态
                invalidateData(request.data)

                //存在缓存但是没用上，引用计数减一
                if (value != null) referenceCounter.decrement(value.bitmap)

                // Fetch and decode the image.
                val (drawable, isSampled, dataSource) =
                    execute(mappedData, fetcher, request, chain.requestType, size, eventListener)

                // Mark the drawable's bitmap as eligible for pooling.
                validateDrawable(drawable)
                
                //尝试将获取到的 bitmap 缓存到内存中
                val isCached = writeToMemoryCache(request, memoryCacheKey, drawable, isSampled)

                // Return the result.
                SuccessResult(
                    drawable = drawable,
                    request = request,
                    metadata = Metadata(
                        memoryCacheKey = memoryCacheKey.takeIf { isCached },
                        isSampled = isSampled,
                        dataSource = dataSource,
                        isPlaceholderMemoryCacheKeyPresent = chain.cached != null
                    )
                )
            }
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                throw throwable
            } else {
                return requestService.errorResult(chain.request, throwable)
            }
        }
    }
    
    /** Load the [data] as a [Drawable]. Apply any [Transformation]s. */
    private suspend inline fun execute(
        data: Any,
        fetcher: Fetcher<Any>,
        request: ImageRequest,
        type: Int,
        size: Size,
        eventListener: EventListener
    ): DrawableResult {
        val options = requestService.options(request, size, systemCallbacks.isOnline)

        eventListener.fetchStart(request, fetcher, options)
        val fetchResult = fetcher.fetch(bitmapPool, data, size, options)
        eventListener.fetchEnd(request, fetcher, options, fetchResult)

        val baseResult = when (fetchResult) {
            is SourceResult -> {
                val decodeResult = try {
                    // Check if we're cancelled.
                    coroutineContext.ensureActive()

                    //判断本次请求是否属于预加载，即可能外部只是想先将图片加载到本地磁盘，方便后续使用
                    //预加载的判断标准就是：异步请求 + target为null + 不缓存到内存中
                    //属于预加载的话就不需要将加载到的图片进行转码了，就会使用 EmptyDecoder
                    //否则就还是需要去找能进行转码的 Decoder
                    val isDiskOnlyPreload = type == REQUEST_TYPE_ENQUEUE &&
                        request.target == null &&
                        !request.memoryCachePolicy.writeEnabled
                    val decoder = if (isDiskOnlyPreload) {
                        // Skip decoding the result if we are preloading the data and writing to the memory cache is
                        // disabled. Instead, we exhaust the source and return an empty result.
                        EmptyDecoder
                    } else {
                        request.decoder ?: registry.requireDecoder(request.data, fetchResult.source, fetchResult.mimeType)
                    }

                    // Decode the stream.
                    eventListener.decodeStart(request, decoder, options)
                    //进行转码，得到目标类型 Drawable
                    val decodeResult = decoder.decode(bitmapPool, fetchResult.source, size, options)
                    eventListener.decodeEnd(request, decoder, options, decodeResult)
                    decodeResult
                } catch (throwable: Throwable) {
                    // Only close the stream automatically if there is an uncaught exception.
                    // This allows custom decoders to continue to read the source after returning a drawable.
                    fetchResult.source.closeQuietly()
                    throw throwable
                }

                // Combine the fetch and decode operations' results.
                DrawableResult(
                    drawable = decodeResult.drawable,
                    isSampled = decodeResult.isSampled,
                    dataSource = fetchResult.dataSource
                )
            }
            is DrawableResult -> fetchResult
        }

        // Check if we're cancelled.
        coroutineContext.ensureActive()

        // Apply any transformations and prepare to draw.
        val finalResult = applyTransformations(baseResult, request, size, options, eventListener)
        (finalResult.drawable as? BitmapDrawable)?.bitmap?.prepareToDraw()
        return finalResult
    }
    
}
```

Fetcher 是**根据图片来源地址转换为目标数据类型**的转换器。Coil 默认提供了以下八种类型的 Fetcher，分别用于处理 **HttpUri、HttpUrl、File、Asset、ContentUri、Resource、Drawable、Bitmap** 等类型的图片来源地址

```kotlin
private val registry = componentRegistry.newBuilder()
    ···
    // Fetchers
    .add(HttpUriFetcher(callFactory))
    .add(HttpUrlFetcher(callFactory))
    .add(FileFetcher(addLastModifiedToFileCacheKey))
    .add(AssetUriFetcher(context))
    .add(ContentUriFetcher(context))
    .add(ResourceUriFetcher(context, drawableDecoder))
    .add(DrawableFetcher(drawableDecoder))
    .add(BitmapFetcher())
    ···
    .build()
```

所以，如果我们外部要加载的是一张网络图片，且传入的是 String 类型的 ImageUrl，那么最终对应上的就是 HttpUriFetcher，其父类 HttpFetcher 就会通过 OkHttp 来进行网络请求了。至此，整个图片加载流程就结束了

```kotlin
internal class HttpUriFetcher(callFactory: Call.Factory) : HttpFetcher<Uri>(callFactory) {

    override fun handles(data: Uri) = data.scheme == "http" || data.scheme == "https"

    override fun key(data: Uri) = data.toString()

    override fun Uri.toHttpUrl(): HttpUrl = HttpUrl.get(toString())
}

internal abstract class HttpFetcher<T : Any>(private val callFactory: Call.Factory) : Fetcher<T> {

    /**
     * Perform this conversion in a [Fetcher] instead of a [Mapper] so
     * [HttpUriFetcher] can execute [HttpUrl.get] on a background thread.
     */
    abstract fun T.toHttpUrl(): HttpUrl

    override suspend fun fetch(
        pool: BitmapPool,
        data: T,
        size: Size,
        options: Options
    ): FetchResult {
        val url = data.toHttpUrl()
        val request = Request.Builder().url(url).headers(options.headers)

        val networkRead = options.networkCachePolicy.readEnabled
        val diskRead = options.diskCachePolicy.readEnabled
        when {
            !networkRead && diskRead -> {
                request.cacheControl(CacheControl.FORCE_CACHE)
            }
            networkRead && !diskRead -> if (options.diskCachePolicy.writeEnabled) {
                request.cacheControl(CacheControl.FORCE_NETWORK)
            } else {
                request.cacheControl(CACHE_CONTROL_FORCE_NETWORK_NO_CACHE)
            }
            !networkRead && !diskRead -> {
                // This causes the request to fail with a 504 Unsatisfiable Request.
                request.cacheControl(CACHE_CONTROL_NO_NETWORK_NO_CACHE)
            }
        }

        val response = callFactory.newCall(request.build()).await()
        if (!response.isSuccessful) {
            response.body()?.close()
            throw HttpException(response)
        }
        val body = checkNotNull(response.body()) { "Null response body!" }

        return SourceResult(
            source = body.source(),
            mimeType = getMimeType(url, body),
            dataSource = if (response.cacheResponse() != null) DataSource.DISK else DataSource.NETWORK
        )
    }
    
}
```

# 八、缓存机制

Glide 的缓存机制是分为**内存缓存**和**磁盘缓存**两层，Coil 在这两个的基础上还增加了**网络缓存**这一层，这可以从 ImageRequest 的参数看出来，默认情况下，这三层缓存机制是全部启用的，即全部可读可写

```kotlin
//内存缓存
val memoryCachePolicy: CachePolicy,
//磁盘缓存
val diskCachePolicy: CachePolicy,
//网络缓存
val networkCachePolicy: CachePolicy,
```

```kotlin
enum class CachePolicy(
    val readEnabled: Boolean,
    val writeEnabled: Boolean
) {
    ENABLED(true, true),
    READ_ONLY(true, false),
    WRITE_ONLY(false, true),
    DISABLED(false, false)
}
```

在请求图片的时候，我们可以在 lambda 块中配置本次请求的缓存策略

```kotlin
imageView.load(imageUrl) {
    memoryCachePolicy(CachePolicy.ENABLED)
    diskCachePolicy(CachePolicy.ENABLED)
    networkCachePolicy(CachePolicy.ENABLED)
}
```

下面来看看 Coil 的缓存机制具体是如何定义和实现的

## 1、内存缓存

Coil 的内存缓存机制集中在 EngineInterceptor 中生效，有两个时机会来判断是否可以写入和读取内存缓存

1. 如果本次请求允许从内存中读取缓存的话，即 `request.memoryCachePolicy.readEnabled` 为 true，那么就尝试从 memoryCacheService 读取缓存
2. 如果本次请求允许将图片缓存到内存的话，即 `request.memoryCachePolicy.writeEnabled` 为 true，那么就将图片存到 strongMemoryCache 中

```kotlin
internal class EngineInterceptor(
    private val registry: ComponentRegistry,
    private val bitmapPool: BitmapPool,
    private val referenceCounter: BitmapReferenceCounter,
    private val strongMemoryCache: StrongMemoryCache,
    private val memoryCacheService: MemoryCacheService,
    private val requestService: RequestService,
    private val systemCallbacks: SystemCallbacks,
    private val drawableDecoder: DrawableDecoderService,
    private val logger: Logger?
) : Interceptor {
    
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        try {
            val request = chain.request           
            ··· 
            //如果本次允许使用内存缓存的话，那么就尝试从 memoryCacheService 中获取缓存
            val value = if (request.memoryCachePolicy.readEnabled) memoryCacheService[memoryCacheKey] else null
            ···
            return withContext(request.dispatcher) {
                ···
                //尝试将获取到的 bitmap 缓存到 strongMemoryCache 中
                val isCached = writeToMemoryCache(request, memoryCacheKey, drawable, isSampled)
                ···
            }
        } catch (throwable: Throwable) {
            ···
        }
    }
    
    private fun writeToMemoryCache(
        request: ImageRequest,
        key: MemoryCache.Key?,
        drawable: Drawable,
        isSampled: Boolean
    ): Boolean {
        if (!request.memoryCachePolicy.writeEnabled) {
            return false
        }

        if (key != null) {
            val bitmap = (drawable as? BitmapDrawable)?.bitmap
            if (bitmap != null) {
                strongMemoryCache.set(key, bitmap, isSampled)
                return true
            }
        }
        return false
    }
    
}
```

MemoryCacheService 相当于一个工具类，会先后尝试从 StrongMemoryCache 和 WeakMemoryCache 取值，取得到的话会同时通过 BitmapReferenceCounter 将其引用计数 +1

```kotlin
internal class MemoryCacheService(
    private val referenceCounter: BitmapReferenceCounter,
    private val strongMemoryCache: StrongMemoryCache,
    private val weakMemoryCache: WeakMemoryCache
) {

    operator fun get(key: MemoryCache.Key?): RealMemoryCache.Value? {
        key ?: return null
        val value = strongMemoryCache.get(key) ?: weakMemoryCache.get(key)
        if (value != null) referenceCounter.increment(value.bitmap)
        return value
    }
}
```

Coil 的内存缓存机制实际上是分为两级：

1. WeakMemoryCache
2. StrongMemoryCache

在默认情况下，Coil 的这两级内存缓存都是开启的，这两者的关系是：

1. RealWeakMemoryCache。通过弱引用来保存曾经加载到内存中的 Bitmap
2. RealBitmapPool。Bitmap 缓存池，用于在内存中缓存当前不再被使用的 Bitmap，可用于后续复用
3. RealBitmapReferenceCounter。RealBitmapReferenceCounter 也通过弱引用来保存 Bitmap，用于对当前处于使用状态的 Bitmap 进行引用标记，计算每个 Bitmap 当前的引用次数及可用状态。例如，当 EngineInterceptor 在 StrongMemoryCache 中找到了可以复用的 Bitmap 后，该 Bitmap 的引用计数就会 +1。当 StrongMemoryCache 由于容量限制需要移除某个 Bitmap 时，该 Bitmap 的引用计数就会 -1。当 Bitmap 的引用次数变为 0 且处于不可用状态时，就会将其从 RealWeakMemoryCache 中移除并存到 BitmapPool 中
4. RealStrongMemoryCache。RealStrongMemoryCache 通过最近最少使用算法 LruCache 来缓存 Bitmap，并且是通过强引用的方式来保存。当 EngineInterceptor 加载到一个 Bitmap 后，就会将其存到 RealStrongMemoryCache 的 LruCache 中，并同时将 RealBitmapReferenceCounter 的引用计数 +1，在移除元素时也会相应减少引用计数

这两级缓存的设计初衷是什么呢？或者说，将内存缓存设计为这两层是因为什么呢？

我们都知道，弱引用是不会阻止内存回收的，一个对象如果只具备弱引用，那么在 GC 过后该对象就会被回收，所以 RealWeakMemoryCache 的存在不会导致 Bitmap 被泄漏。而 RealStrongMemoryCache 是通过强引用和 LruCache 来存储 Bitmap 的，由于 LruCache 具有固定容量，那么就存在由于容量不足导致用户当前正在使用的 Bitmap 被移出 LruCache 的可能，如果之后又需要加载同一个 Bitmap 的话，就还可以通过 RealWeakMemoryCache 来取值，尽量复用已经加载在内存中的 Bitmap。所以说，RealStrongMemoryCache 和 RealWeakMemoryCache 的存在意义都是为了尽量复用 Bitmap

此外，BitmapPool 的存在意义是为了尽量避免频繁创建 Bitmap。在使用 Transformation 的时候需要用到 Bitmap 来作为载体，如果频繁创建 Bitmap 可能会造成内存抖动，所以即使当一个 Bitmap 不再被使用，也会将之存到 RealBitmapPool 中缓存起来，方便后续复用。RealBitmapReferenceCounter 会保存 Bitmap 的引用次数和可用状态，当引用次数小于等于 0 且处于不可用状态时，就会将其从 RealWeakMemoryCache 中移除并存到 BitmapPool 中

## 2、磁盘缓存、网络缓存

Coil 的**磁盘缓存**和**网络缓存**可以合在一起讲，因为 Coil 的磁盘缓存其实是通过 OkHttp 本身的网络缓存功能来间接实现的。RealImageLoader 在初始化的时候，默认构建了一个包含 cache 的 OkHttpClient，即默认支持缓存网络请求结果

```kotlin
private fun buildDefaultCallFactory() = lazyCallFactory {
    OkHttpClient.Builder()
        .cache(CoilUtils.createDefaultCache(applicationContext))
        .build()
}
```

而且，Coil 的磁盘缓存和网络缓存这两个配置也只会在 HttpFetcher 这里读取，即只在进行网络请求的时候生效，所以说，Coil **只会磁盘缓存通过网络请求得到的原始图片，而不缓存其它尺寸大小的图片**

HttpFetcher 的网络缓存和磁盘缓存策略是通过修改 Request 的 **cacheControl** 来实现的，每种缓存策略可以分别配置是否可读可写，一共有以下几种可能：

1. 不允许网络请求，允许磁盘读缓存。那么就强制使用本地缓存，如果本地缓存不存在的话就报错，加载失败
2. 允许网络请求，不允许磁盘读缓存
   1. 允许磁盘写缓存。那么就强制去网络请求，且将请求结果缓存到本地磁盘
   2. 不允许磁盘写缓存。那么就强制去网络请求，且不将请求结果缓存到本地磁盘
3. 不允许网络请求，不允许磁盘读缓存。这会导致请求失败，Http 报 504 错误，加载失败
4. 允许网络请求，也允许磁盘读缓存和磁盘写缓存。那么就会优先使用本地缓存，本地缓存不存在的话再去网络请求，并将网络请求结果缓存到本地磁盘

```kotlin
internal abstract class HttpFetcher<T : Any>(private val callFactory: Call.Factory) : Fetcher<T> {

    /**
     * Perform this conversion in a [Fetcher] instead of a [Mapper] so
     * [HttpUriFetcher] can execute [HttpUrl.get] on a background thread.
     */
    abstract fun T.toHttpUrl(): HttpUrl

    override suspend fun fetch(
        pool: BitmapPool,
        data: T,
        size: Size,
        options: Options
    ): FetchResult {
        val url = data.toHttpUrl()
        val request = Request.Builder().url(url).headers(options.headers)

        val networkRead = options.networkCachePolicy.readEnabled
        val diskRead = options.diskCachePolicy.readEnabled
        when {
            //1、不允许网络请求，允许磁盘读缓存
            //那么就强制使用本地缓存，如果不存在本地缓存的话就报错
            !networkRead && diskRead -> {
                request.cacheControl(CacheControl.FORCE_CACHE)
            }
            //2、允许网络请求，不允许磁盘读缓存
            networkRead && !diskRead ->
                if (options.diskCachePolicy.writeEnabled) {
                    //2.1、允许磁盘写缓存
                    //那么就强制去网络请求，且将请求结果缓存到本地磁盘
                    request.cacheControl(CacheControl.FORCE_NETWORK)
                } else {
                    //2.2、不允许磁盘写缓存
                    //那么就强制去网络请求，且不将请求结果缓存到本地磁盘
                    request.cacheControl(CACHE_CONTROL_FORCE_NETWORK_NO_CACHE)
                }
            !networkRead && !diskRead -> {
                //3、不允许网络请求，不允许磁盘读缓存
                //这会导致请求失败，就会导致请求失败，报 504 错误
                request.cacheControl(CACHE_CONTROL_NO_NETWORK_NO_CACHE)
            }
        }

        val response = callFactory.newCall(request.build()).await()
        if (!response.isSuccessful) {
            response.body()?.close()
            throw HttpException(response)
        }
        val body = checkNotNull(response.body()) { "Null response body!" }

        return SourceResult(
            source = body.source(),
            mimeType = getMimeType(url, body),
            dataSource = if (response.cacheResponse() != null) DataSource.DISK else DataSource.NETWORK
        )
    }

    ···
}
```

从以上逻辑也可以看出，networkCachePolicy 的 writeEnabled 属性并没有被用到，因为网络请求本身只有**发起**和**不发起**两种选择，用 readEnabled 就足够表示了，所以 writeEnabled 对于 networkCachePolicy 来说没有意义

此外，为了在无网络信号的时候可以快速结束整个流程，避免无意义的网络请求，RequestService 会在当前处于离线的时候（即 isOnline 为 false），将 networkCachePolicy 修改为完全禁用状态（CachePolicy.DISABLED）

```kotlin
internal class RequestService(private val logger: Logger?) {
    
    @WorkerThread
    fun options(
        request: ImageRequest,
        size: Size,
        isOnline: Boolean
    ): Options {
       	···
        // Disable fetching from the network if we know we're offline.
        val networkCachePolicy = if (isOnline) request.networkCachePolicy else CachePolicy.DISABLED
     	···
        return Options(
            context = request.context,
            config = config,
            colorSpace = request.colorSpace,
            scale = request.scale,
            allowInexactSize = request.allowInexactSize,
            allowRgb565 = allowRgb565,
            premultipliedAlpha = request.premultipliedAlpha,
            headers = request.headers,
            parameters = request.parameters,
            memoryCachePolicy = request.memoryCachePolicy,
            diskCachePolicy = request.diskCachePolicy,
            networkCachePolicy = networkCachePolicy
        )
    }

    
}
```

# 九、生命周期监听

前文有提到，每个 ImageRequest 都会关联一个 Context 对象，如果外部传入的是 ImageView，则会自动取 ImageView 内部的 Context。Coil 会判断 Context 是否属于 LifecycleOwner 类型，是的话则可以拿到**和 Activity 或者 Fragment 关联的 Lifecycle**，否则最终取 **GlobalLifecycle**

和 Activity 或者 Fragment 关联的 Lifecycle 才**具备有生命周期感知能力**，这样 Coil 才可以在 Activity 处于后台或者已经销毁的时候暂停或者停止任务。而 GlobalLifecycle 会默认且一直会处于 RESUMED 状态，这样任务就会一直运行直到最终结束，这可能导致内存泄露

那么，该  Lifecycle 对象具体是在什么地方起了作用呢？

这个主要看 RealImageLoader 的 `executeMain` 方法。在发起图片加载请求前，后先创建 request 的代理对象 requestDelegate，requestDelegate 中就包含了对 Lifecycle 的处理逻辑。此外，如果是异步请求的话，会等到 Lifecycle 至少处于 Started 状态之后才能发起请求，这样当 Activity 还处于后台时就不会发起请求了

```kotlin
@MainThread
private suspend fun executeMain(initialRequest: ImageRequest, type: Int): ImageResult {
    ···

    //创建 request 的代理对象
    val requestDelegate = delegateService.createRequestDelegate(request, targetDelegate, coroutineContext.job)

    try {
        ···

        //如果是异步请求的话，那么就需要等到 Lifecycle 至少处于 Started 状态之后才能继续执行
        if (type == REQUEST_TYPE_ENQUEUE) request.lifecycle.awaitStarted()

        ···
        return result
    } catch (throwable: Throwable) {
        if (throwable is CancellationException) {
            onCancel(request, eventListener)
            throw throwable
        } else {
            // Create the default error result if there's an uncaught exception.
            val result = requestService.errorResult(request, throwable)
            onError(result, targetDelegate, eventListener)
            return result
        }
    } finally {
        requestDelegate.complete()
    }
}
```

`createRequestDelegate` 方法的逻辑可以总结为：

1. 如果 target 对象属于 ViewTarget 类型，那么说明其包含特定 View
   - 将请求请求参数包装为 ViewTargetRequestDelegate 类型，而 ViewTargetRequestDelegate 实现了 DefaultLifecycleObserver 接口，其会在收到 onDestroy 事件的时候主动取消 Job 并清理各类资源。所以向 Lifecycle 添加该 Observer 就可以保证在 Activity 销毁后也能同时取消图片加载请求，避免内存泄漏
   - 如果 target 属于 LifecycleObserver 类型的话，则也向 Lifecycle 添加该 Observer 。ImageViewTarget 就实现了 DefaultLifecycleObserver 接口，这主要是为了判断 ImageView 对应的 Activity 或者 Fragment 是否处于前台，如果处于前台且存在 Animatable 的话就会自动启动动画，否则就自动停止动画。之所以需要先 removeObserver 再 addObserver，是因为 target 可能需要先后请求多张图片，我们不能重复向 Lifecycle 添加同一个 Observer 对象
   - 同时，如果 View 已经 Detached 了的话，那么就需要主动取消请求
2. 如果  target 对象不属于 ViewTarget 类型的话，创建的代理对象是 BaseRequestDelegate 类型，也会在收到 onDestroy 事件的时候主动取消 Job

```kotlin
/** Wrap [request] to automatically dispose (and for [ViewTarget]s restart) the [ImageRequest] based on its lifecycle. */
@MainThread
fun createRequestDelegate(
    request: ImageRequest,
    targetDelegate: TargetDelegate,
    job: Job
): RequestDelegate {
    val lifecycle = request.lifecycle
    val delegate: RequestDelegate
    when (val target = request.target) {
        //对应第1点
        is ViewTarget<*> -> {
            //对应第1.1点
            delegate = ViewTargetRequestDelegate(imageLoader, request, targetDelegate, job)
            lifecycle.addObserver(delegate)

            //对应第1.2点
            if (target is LifecycleObserver) {
                lifecycle.removeObserver(target)
                lifecycle.addObserver(target)
            }

            target.view.requestManager.setCurrentRequest(delegate)

            //对应第1.3点
            // Call onViewDetachedFromWindow immediately if the view is already detached.
            if (!target.view.isAttachedToWindowCompat) {
                target.view.requestManager.onViewDetachedFromWindow(target.view)
            }
        }
        //对应第2点
        else -> {
            delegate = BaseRequestDelegate(lifecycle, job)
            lifecycle.addObserver(delegate)
        }
    }
    return delegate
}
```

# 十、Transformation

图片变换是基本所有的图片加载库都会支持的功能，Coil 对这个概念的抽象即 Transformation 接口

注意，`key()`方法的返回值是用于计算图片在内存缓存中的唯一 Key 时的辅助参数，所以需要实现该方法，为 Transformation 生成一个可以唯一标识自身的字符串 Key。`transform` 方法包含了一个 BitmapPool 参数，我们在实现图形变换的时候往往是需要一个全新的 Bitmap，此时就应该通过 BitmapPool 来获取，尽量复用已有的 Bitmap

```kotlin
interface Transformation {

    /**
     * Return a unique key for this transformation.
     *
     * The key should contain any params that are part of this transformation (e.g. size, scale, color, radius, etc.).
     */
    fun key(): String

    /**
     * Apply the transformation to [input].
     *
     * @param pool A [BitmapPool] which can be used to request [Bitmap] instances.
     * @param input The input [Bitmap] to transform. Its config will always be [Bitmap.Config.ARGB_8888] or [Bitmap.Config.RGBA_F16].
     * @param size The size of the image request.
     */
    suspend fun transform(pool: BitmapPool, input: Bitmap, size: Size): Bitmap
}
```

Coil 默认提供了以下几个 Transformation 实现类

- BlurTransformation。用于实现高斯模糊
- CircleCropTransformation。用于将图片转换为圆形
- GrayscaleTransformation。用户实现将图片转换为灰色
- RoundedCornersTransformation。用于为图片添加圆角

我们可以学着官方给的例子，自己来实现两个 Transformation 

## 1、为图片添加水印

为图片添加水印的思路也很简单，只需要对 canvas 稍微坐下旋转，然后绘制文本即可

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 * @Desc: 为图片添加水印
 */
class WatermarkTransformation(
    private val watermark: String,
    @ColorInt private val textColor: Int,
    private val textSize: Float
) : Transformation {

    override fun key(): String {
        return "${WatermarkTransformation::class.java.name}-${watermark}-${textColor}-${textSize}"
    }

    override suspend fun transform(pool: BitmapPool, input: Bitmap, size: Size): Bitmap {
        val width = input.width
        val height = input.height
        val config = input.config

        val output = pool.get(width, height, config)

        val canvas = Canvas(output)
        val paint = Paint()
        paint.isAntiAlias = true
        canvas.drawBitmap(input, 0f, 0f, paint)

        canvas.rotate(40f, width / 2f, height / 2f)

        paint.textSize = textSize
        paint.color = textColor

        val textWidth = paint.measureText(watermark)

        canvas.drawText(watermark, (width - textWidth) / 2f, height / 2f, paint)

        return output
    }

}
```

```kotlin
imageView.load(imageUrl) {
    transformations(
        WatermarkTransformation("业志陈", Color.parseColor("#8D3700B3"), 120f)
    )
}
```

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/0342c509ce3340a5b82a6e342c1b8916~tplv-k3u1fbpfcp-zoom-1.image)

## 2、为图片添加蒙层

Android 的 Paint 原生就支持为 Bitmap 添加一个蒙层，只需要使用其 `colorFilter`方法即可

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 * @Desc: 添加蒙层
 */
class ColorFilterTransformation(
    @ColorInt private val color: Int
) : Transformation {

    override fun key(): String = "${ColorFilterTransformation::class.java.name}-$color"

    override suspend fun transform(pool: BitmapPool, input: Bitmap, size: Size): Bitmap {
        val width = input.width
        val height = input.height
        val config = input.config
        val output = pool.get(width, height, config)

        val canvas = Canvas(output)
        val paint = Paint()
        paint.isAntiAlias = true
        paint.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        canvas.drawBitmap(input, 0f, 0f, paint)

        return output
    }
}
```

```kotlin
imageView.load(imageUrl) {
    transformations(
        WatermarkTransformation("业志陈", Color.parseColor("#8D3700B3"), 120f),
        ColorFilterTransformation(Color.parseColor("#9CF44336"))
    )
}
```

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/e4417b3a98bc44258e0fe9395e73dc4e~tplv-k3u1fbpfcp-zoom-1.image)

更多 Transformation 效果看这里：[coil-transformations](https://github.com/Commit451/coil-transformations)

# 十一、实现全局默认配置

如果我们想要设置应用内所有图片在加载时固定显示同一张 loading 图，在加载失败时固定显示一张 error 图， 那么就需要为 Coil 设定一个全局的默认配置。Glide 是通过 AppGlideModule 来实现的，那 Coil 是如何来实现这个效果呢？

Coil 默认会在我们第一次触发图片加载的时候来初始化 RealImageLoader 的单例对象，而 RealImageLoader 的构造参数就包含了一个 DefaultRequestOptions 用于设置默认配置，所以我们可以通过自定义 RealImageLoader 的初始化逻辑来控制全局的默认请求配置

```kotlin
internal class RealImageLoader(
    context: Context,
    override val defaults: DefaultRequestOptions,
    override val bitmapPool: BitmapPool,
    private val referenceCounter: BitmapReferenceCounter,
    private val strongMemoryCache: StrongMemoryCache,
    private val weakMemoryCache: WeakMemoryCache,
    callFactory: Call.Factory,
    private val eventListenerFactory: EventListener.Factory,
    componentRegistry: ComponentRegistry,
    addLastModifiedToFileCacheKey: Boolean,
    private val launchInterceptorChainOnMainThread: Boolean,
    val logger: Logger?
) 
```

RealImageLoader 的单例对象就保存在另一个单例对象 Coil 中，Coil 以两种方式来完成 RealImageLoader 的初始化

- 如果项目中的 Application 继承了 ImageLoaderFactory 接口，那么就通过该接口来完成初始化
- 通过 `ImageLoader(context)` 来完成默认初始化

```kotlin
/**
 * A class that holds the singleton [ImageLoader] instance.
 */
object Coil {

    private var imageLoader: ImageLoader? = null
    private var imageLoaderFactory: ImageLoaderFactory? = null

    /**
     * Get the singleton [ImageLoader]. Creates a new instance if none has been set.
     */
    @JvmStatic
    fun imageLoader(context: Context): ImageLoader = imageLoader ?: newImageLoader(context)

    ···

    /** Create and set the new singleton [ImageLoader]. */
    @Synchronized
    private fun newImageLoader(context: Context): ImageLoader {
        // Check again in case imageLoader was just set.
        imageLoader?.let { return it }

        // Create a new ImageLoader.
        val newImageLoader = imageLoaderFactory?.newImageLoader()
            ?: (context.applicationContext as? ImageLoaderFactory)?.newImageLoader()
            ?: ImageLoader(context)
        imageLoaderFactory = null
        imageLoader = newImageLoader
        return newImageLoader
    }
    
}
```

为了设定默认配置，我们就需要**在应用启动之后，开始图片加载之前**向 Coil 注入自己的 ImageLoader 实例

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 * @Desc:
 */
object CoilHolder {

    fun init(application: Application) {
        Coil.setImageLoader(
            ImageLoader.Builder(application)
                .placeholder(ActivityCompat.getDrawable(application, R.drawable.icon_loading)) //占位符
                .error(ActivityCompat.getDrawable(application, R.drawable.icon_error)) //错误图
                .memoryCachePolicy(CachePolicy.ENABLED) //开启内存缓存
                .callFactory(createOkHttp(application)) //主动构造 OkHttpClient 实例
                .build()
        )
    }

    private fun createOkHttp(application: Application): OkHttpClient {
        return OkHttpClient.Builder()
            .cache(createDefaultCache(application))
            .build()
    }

    private fun createDefaultCache(context: Context): Cache {
        val cacheDirectory = getDefaultCacheDirectory(context)
        return Cache(cacheDirectory, 10 * 1024 * 1024)
    }

    private fun getDefaultCacheDirectory(context: Context): File {
        return File(context.cacheDir, "image_cache").apply { mkdirs() }
    }

}
```

# 十二、自定义网络请求

在讲 Coil 的**磁盘缓存**和**网络缓存**这一节内容的时候有提到，Coil 的网络请求是由 HttpFetcher 来完成的，那么我们是否有办法来替换该组件，自己来实现网络请求呢？先来看下 Coil 是如何实现将外部传入的图片地址和特定的 Fetcher 对应上的

RealImageLoader 包含一个 registry 变量，其包含的 Mapper 和 Fetcher 就用于实现数据映射

```kotlin
internal class RealImageLoader(
    context: Context,
    override val defaults: DefaultRequestOptions,
    override val bitmapPool: BitmapPool,
    private val referenceCounter: BitmapReferenceCounter,
    private val strongMemoryCache: StrongMemoryCache,
    private val weakMemoryCache: WeakMemoryCache,
    callFactory: Call.Factory,
    private val eventListenerFactory: EventListener.Factory,
    componentRegistry: ComponentRegistry,
    addLastModifiedToFileCacheKey: Boolean,
    private val launchInterceptorChainOnMainThread: Boolean,
    val logger: Logger?
) : ImageLoader {
    
 	private val registry = componentRegistry.newBuilder()
        // Mappers
        .add(StringMapper())
        .add(FileUriMapper())
        .add(ResourceUriMapper(context))
        .add(ResourceIntMapper(context))
        // Fetchers
        .add(HttpUriFetcher(callFactory))
        .add(HttpUrlFetcher(callFactory))
        .add(FileFetcher(addLastModifiedToFileCacheKey))
        .add(AssetUriFetcher(context))
        .add(ContentUriFetcher(context))
        .add(ResourceUriFetcher(context, drawableDecoder))
        .add(DrawableFetcher(drawableDecoder))
        .add(BitmapFetcher())
        // Decoders
        .add(BitmapFactoryDecoder(context))
        .build()
    
}
```

外部在调用 `load` 方法时，传入的 String 参数可能是完全不同的含义，既可能是指向本地资源文件，也可能是指向远程的网络图片，Coil 就依靠 Mapper 和 Fetcher 来区分资源类型

```kotlin
imageView.load("android.resource://example.package.name/drawable/image")

imageView.load("https://www.example.com/image.jpg")
```

StringMapper 首先会将 String 类型转换为 Uri

```kotlin
internal class StringMapper : Mapper<String, Uri> {

    override fun map(data: String) = data.toUri()
}
```

ResourceUriFetcher 会拿到 Uri，然后判断 Uri 的 `scheme` 是否是 `android.resource`，是的话就知道其指向的是本地的资源文件。HttpUriFetcher 则是判断 Uri 的 `scheme` 是否是`http`或者`https`，是的话就知道其指向的是远程网络图片

```kotlin
internal class ResourceUriFetcher(
    private val context: Context,
    private val drawableDecoder: DrawableDecoderService
) : Fetcher<Uri> {

    override fun handles(data: Uri) = data.scheme == ContentResolver.SCHEME_ANDROID_RESOURCE
    
}


internal class HttpUriFetcher(callFactory: Call.Factory) : HttpFetcher<Uri>(callFactory) {

    override fun handles(data: Uri) = data.scheme == "http" || data.scheme == "https"

    override fun key(data: Uri) = data.toString()

    override fun Uri.toHttpUrl(): HttpUrl = HttpUrl.get(toString())
    
}
```

以上这个**转换+判断+加载**的过程就发生在 EngineInterceptor 中

```kotlin
internal class EngineInterceptor(
    private val registry: ComponentRegistry,
    private val bitmapPool: BitmapPool,
    private val referenceCounter: BitmapReferenceCounter,
    private val strongMemoryCache: StrongMemoryCache,
    private val memoryCacheService: MemoryCacheService,
    private val requestService: RequestService,
    private val systemCallbacks: SystemCallbacks,
    private val drawableDecoder: DrawableDecoderService,
    private val logger: Logger?
) : Interceptor {
    
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        ···
        //将外部传入的数据进行类型转换
        val mappedData = registry.mapData(data)
        //找到能处理本次请求的 fetcher
        val fetcher = request.fetcher(mappedData) ?: registry.requireFetcher(mappedData)
        
        ···
        
    }
    
}
```

所以，要自定义网络请求组件，我们就需要向 ComponentRegistry 添加自己的 HttpFetcher 实现，在拿到 Uri 类型的网络地址后发起网络请求。这里我来写一个通过 Volley 来完成网络图片加载的 VolleyFetcher。**需要注意的是，我写的 VolleyFetcher 并不可靠，因为我也只是想写个 Demo 而已，正常来说还是应该使用 OkHttp**

```kotlin
/**
 * @Author: leavesCZY
 * @Github：https://github.com/leavesCZY
 * @Desc:
 */
class VolleyFetcher(private val application: Application) : Fetcher<Uri> {

    override fun handles(data: Uri) = data.scheme == "http" || data.scheme == "https"

    override fun key(data: Uri): String? {
        return data.toString()
    }

    private class ImageRequest(url: String, private val listener: RequestFuture<BufferedSource>) :
        Request<BufferedSource>(Method.GET, url, listener) {
        override fun parseNetworkResponse(response: NetworkResponse): Response<BufferedSource> {
            return Response.success(
                Buffer().write(response.data),
                HttpHeaderParser.parseCacheHeaders(response)
            )
        }

        override fun deliverResponse(response: BufferedSource) {
            listener.onResponse(response)
        }
    }

    override suspend fun fetch(
        pool: BitmapPool,
        data: Uri,
        size: Size,
        options: Options
    ): FetchResult {
        val url = data.toString()
        val newFuture = RequestFuture.newFuture<BufferedSource>()
        val request = ImageRequest(url, newFuture)
        newFuture.setRequest(request)
        Volley.newRequestQueue(application).apply {
            add(request)
            start()
        }
        val get = newFuture.get()
        return SourceResult(
            source = get,
            mimeType = "",
            dataSource = DataSource.NETWORK
        )
    }

}
```

然后为 ImageLoader 注册该 Fetcher 即可

```kotlin
fun init(application: Application) {
    val okHttpClient = createOkHttp(application)
    Coil.setImageLoader(
        ImageLoader.Builder(application)
            .placeholder(ActivityCompat.getDrawable(application, R.drawable.icon_loading))
            .error(ActivityCompat.getDrawable(application, R.drawable.icon_error))
            .memoryCachePolicy(CachePolicy.ENABLED)
            .callFactory(okHttpClient)
            .componentRegistry(
                ComponentRegistry.Builder()
                    .add(VolleyFetcher(application))
                    .add(OkHttpFetcher(okHttpClient)).build()
            )
            .build()
    )
}
```

# 十三、GitHub

上述的所有示例代码我都放到 GitHub 了，欢迎 star：[AndroidOpenSourceDemo](https://github.com/leavesCZY/AndroidOpenSourceDemo)