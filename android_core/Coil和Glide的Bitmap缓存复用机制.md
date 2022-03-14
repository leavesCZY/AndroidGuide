> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

Android 开发者最熟悉的图片加载框架应该是 Glide 和 Picasso 这两个了，最近两年也出现了一个后起之秀：Coil

[Coil](https://github.com/coil-kt/coil) 是一个新兴的 Android 图片加载库，Coil 的名字由来：**Co**routine，**I**mage 和 **L**oader 得到 Coil，其特点有：

- **更快**: Coil 在性能上做了很多优化，包括内存缓存和磁盘缓存、对内存中的图片进行采样、复用 Bitmap、支持根据生命周期变化自动暂停和取消图片请求等
- **更轻量级**: Coil 大约会给你的 App 增加两千个方法（前提是你的 App 已经集成了 OkHttp 和 Coroutines），Coil 的方法数和 Picasso 相当，相比 Glide 和 Fresco 要轻量级很多
- **更容易使用**: Coil's API 充分利用了 Kotlin 语言的新特性，简化并减少了很多重复代码
- **更流行**: Coil 首选 Kotlin 语言开发，并且使用包含 Coroutines、OkHttp、Okio 和 AndroidX Lifecycles 在内的更现代化的开源库

Coil 有着一些独特的优势。例如，为了监听 UI 层的生命周期变化，Glide 是通过向 Activity 或者 Fragment 注入一个无 UI 界面的 Fragment 来实现间接监听的，而 Coil 则只需要直接监听 Lifecycle 即可，在实现方式上 Coil 会更加简单高效。此外，在联网请求图片的时候，Glide 需要通过线程池和多个回调来完成最终图片的显示，而 Coil 由于使用了 Kotlin 协程，可以很简洁地完成异步加载和线程切换，在流程上 Coil 会清晰很多。但实际上 Coil 也是借鉴了一些优秀开源库的实现思路，所以我看 Coil 的源码的时候就总会发现一些 Glide 和 OkHttp 的影子😅😅

**如果你的项目中已经大面积使用到了 Jetpack、Kotlin Coroutines、OkHttp 的话，那么 Coil 会更加契合你的项目**

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

之前在写关于 Glide 和 Coil 的源码解析文章的时候，对 Bitmap 的缓存复用逻辑没有特意做介绍，本文就再来补充下这一个知识点，希望对你有所帮助 🤣🤣

本文基于 Glide 和 Coil 当前的最新版本进行分析

```groovy
implementation "com.github.bumptech.glide:glide:4.12.0"
implementation "io.coil-kt:coil:1.2.0"
```

# 一、BitmapPool 

JDK 中的 ThreadPoolExecutor 相信大多数开发者都很熟悉，我们一般将之称为“线程池”。**池化**是一个很常见的概念，其目的都是为了实现对象复用，例如 ThreadPoolExecutor 就实现了线程的复用机制

在 Android 系统中也有着“池化”概念的实现。因为系统本身存在很多事件需要通过 Message 来交付给 Looper 进行处理，所以 Message 的创建是很频繁的。为了减少 Message 频繁重复创建的情况，Message 提供了 MessagePool 用于实现 Message 的缓存复用，以此来优化内存使用。当 Looper 消费了 Message 后会调用`recycleUnchecked()`方法将 Message 进行回收，在清除了各项资源后会缓存到 `sPool` 变量上，同时将之前缓存的 Message 置为下一个节点 next，通过这种链表结构来缓存最多 50 个Message。`obtain()`方法则会判断当前是否有可用的缓存，有的话则将 `sPool` 从链表中移除后返回，否则就返回一个新的 Message 实例。所以我们在发送消息的时候应该尽量通过调用`Message.obtain()`或者`Handler.obtainMessage()`方法来获取 Message 实例

```java
public final class Message implements Parcelable {
    
    /** @hide */
    public static final Object sPoolSync = new Object();
    private static Message sPool;
    private static int sPoolSize = 0;
    private static final int MAX_POOL_SIZE = 50;
    
    public static Message obtain() {
        synchronized (sPoolSync) {
            if (sPool != null) {
                Message m = sPool;
                sPool = m.next;
                m.next = null;
                m.flags = 0; // clear in-use flag
                sPoolSize--;
                return m;
            }
        }
        return new Message();
    }
    
    @UnsupportedAppUsage
    void recycleUnchecked() {
        // Mark the message as in use while it remains in the recycled object pool.
        // Clear out all other details.
        flags = FLAG_IN_USE;
        what = 0;
        arg1 = 0;
        arg2 = 0;
        obj = null;
        replyTo = null;
        sendingUid = UID_NONE;
        workSourceUid = UID_NONE;
        when = 0;
        target = null;
        callback = null;
        data = null;
        synchronized (sPoolSync) {
            if (sPoolSize < MAX_POOL_SIZE) {
                next = sPool;
                sPool = this;
                sPoolSize++;
            }
        }
    }
    
}
```

相对应的，BitmapPool 就是为了实现 Bitmap 的复用，目前所有流行的图片加载框架都需要使用到 BitmapPool 来减少内存消耗。而 Bitmap 应该是很多应用中最占据内存空间的一类资源了，也是导致应用 OOM 的常见原因之一，BitmapPool 对于应用来说是提升性能的一种很重要的手段

# 二、Bitmap 的回收与复用

根据 Google 官方的介绍：[管理位图内存](https://developer.android.google.cn/topic/performance/graphics/manage-memory#kotlin)  我们可以采取一些措施来促进 Bitmap 垃圾回收和复用，但具体的的策略需要取决于系统版本：

- 在 Android 2.3.3（API 级别 10）及更低版本上，建议通过 `bitmap.recycle()`来尽快回收 Bitmap，降低 `OutOfMemoryError`的概率。但只有当你确定 Bitmap 已不再使用时才应该使用 `recycle()`，否则如果调用了 `recycle()` 并在稍后尝试绘制 Bitmap，则会收到错误：`"Canvas: trying to use a recycled bitmap"`
- 在 Android 3.0（API 级别 11）开始，系统引入了 `BitmapFactory.Options.inBitmap` 字段。如果设置了此选项，那么采用 `Options` 对象的解码方法会在生成目标 Bitmap 时尝试复用 inBitmap，这意味着 inBitmap 的内存得到了重复使用，从而提高了性能，同时移除了内存分配和取消分配。不过 inBitmap 的使用方式存在某些限制，在 Android 4.4（API 级别 19）之前系统仅支持复用大小相同的位图，4.4 之后只要 inBitmap 的大小比目标 Bitmap 大即可

Glide 和 Coil 都在 BitmapPool 的基础上使用到了 inBitmap，从而进一步提高了 Bitmap 的复用效率 

# 三、Coil 对 Bitmap 的复用

Coil 的 BitmapPool 接口定义了缓存 Bitmap 的所有方法。BitmapPool 的存在意义是为了实现 Bitmap 的复用，那么自然就需要有相对应的存取方法，对应 `put` 方法和多个 `get` 方法。而缓存大小也不可能无限制增长，所以还需要有清理缓存的方法，对应 `trimMemory` 方法和 `clear` 方法。当中，`trimMemory`方法就用于根据应用或者系统当前的运行情况来决定如何清理缓存，例如，当应用退到后台时就可以通过该方法来主动减少内存占用，以此提升进程优先级，降低应用被系统杀死的概率

此外，`invoke` 是运算符重载方法，maxSize 即允许使用的最大缓存空间，maxSize 等于 0 则代表不进行缓存，那就使用空实现 EmptyBitmapPool，否则就使用 RealBitmapPool

```kotlin
interface BitmapPool {

    companion object {
        @JvmStatic
        @JvmName("create")
        operator fun invoke(maxSize: Int): BitmapPool {
            return if (maxSize == 0) EmptyBitmapPool() else RealBitmapPool(maxSize)
        }
    }

    //存储 Bitmap
    fun put(bitmap: Bitmap)
    
    //根据要求来获取缓存的 Bitmap 或者是构造一个新的 Bitmap
    fun get(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap
    fun getOrNull(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap?
    fun getDirty(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap
    fun getDirtyOrNull(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap?
    
    //根据应用或者系统当前的运行情况来决定如何清理缓存
    fun trimMemory(level: Int)
    //清空全部缓存
    fun clear()
}
```

RealBitmapPool 是 BitmapPool 接口唯一有意义的实现类。可以看到，虽然 RealBitmapPool 在存取 Bitmap 时都会相应地增减 bitmaps 这个 HashSet 中的元素值，但实际上外部取到的值都是从 strategy 中拿的，BitmapPoolStrategy 才是真正定义了缓存复用机制的地方

```kotlin
internal class RealBitmapPool(
    private val maxSize: Int,
    private val allowedConfigs: Set<Bitmap.Config> = ALLOWED_CONFIGS,
    private val strategy: BitmapPoolStrategy = BitmapPoolStrategy(),
    private val logger: Logger? = null
) : BitmapPool {

    private val bitmaps = hashSetOf<Bitmap>()
    
    @Synchronized
    override fun put(bitmap: Bitmap) {
        ···

        //交付给 BitmapPoolStrategy
        strategy.put(bitmap)

        bitmaps += bitmap
        currentSize += size
        puts++

        logger?.log(TAG, Log.VERBOSE) { "Put bitmap=${strategy.stringify(bitmap)}\n${logStats()}" }

        trimToSize(maxSize)
    }

    @Synchronized
    override fun getDirtyOrNull(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap? {
        require(!config.isHardware) { "Cannot create a mutable hardware bitmap." }

        //根据要求从 BitmapPoolStrategy 取
        val result = strategy.get(width, height, config)
        if (result == null) {
            logger?.log(TAG, Log.VERBOSE) { "Missing bitmap=${strategy.stringify(width, height, config)}" }
            misses++
        } else {
            bitmaps -= result
            currentSize -= result.allocationByteCountCompat
            hits++
            normalize(result)
        }

        logger?.log(TAG, Log.VERBOSE) { "Get bitmap=${strategy.stringify(width, height, config)}\n${logStats()}" }

        return result
    }

    ···
}
```

上文说了，Bitmap 的回收与复用机制在不同的系统版本上有着一些差异，而 BitmapPoolStrategy 就完全屏蔽了在不同 Android 系统版本中 Bitmap 复用规则的差异性，其内部会根据系统版本来决定采用哪种复用机制，使得外部可以通过统一的方法进行存取而无需关心内部实现。这里使用到了**策略模式**

- 在 Android 4.4 之前就采用 AttributeStrategy。AttributeStrategy 将 `bitmapWidth、bitmapHeight、bitmapConfig` 这三者作为 Bitmap 的唯一标识，只有和这三个属性完全相等的 Bitmap 才能拿来复用
- 从 Android 4.4 开始则采用 SizeStrategy。SizeStrategy 将 `bitmapSize` 作为 Bitmap 的唯一标识，只有不小于目标大小且大小不会超出四倍的 Bitmap 才能拿来复用。之所以有最大值的限制，应该是为了节约内存，毕竟如果拿来复用的 bitmap 超出太多的话也比较浪费

```kotlin
internal interface BitmapPoolStrategy {
    companion object {
        operator fun invoke(): BitmapPoolStrategy {
            return when {
                SDK_INT >= 19 -> SizeStrategy()
                else -> AttributeStrategy()
            }
        }
    }
    fun put(bitmap: Bitmap)
    fun get(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap?
    fun removeLast(): Bitmap?
    fun stringify(bitmap: Bitmap): String
    fun stringify(@Px width: Int, @Px height: Int, config: Bitmap.Config): String
}

internal class AttributeStrategy : BitmapPoolStrategy {

    private val entries = LinkedMultimap<Key, Bitmap>()

    override fun put(bitmap: Bitmap) {
        //将 width、height、config 三者同时作为唯一标识 key
        entries.put(Key(bitmap.width, bitmap.height, bitmap.config), bitmap)
    }

    override fun get(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap? {
        return entries.removeLast(Key(width, height, config))
    }

    ···

    private data class Key(
        @Px val width: Int,
        @Px val height: Int,
        val config: Bitmap.Config
    )
}

internal class SizeStrategy : BitmapPoolStrategy {

    companion object {
        private const val MAX_SIZE_MULTIPLE = 4
    }
    
    private val entries = LinkedMultimap<Int, Bitmap>()
    private val sizes = TreeMap<Int, Int>()

    override fun put(bitmap: Bitmap) {
        //将 bitmap 的大小作为其唯一标识 key
        val size = bitmap.allocationByteCountCompat
        entries.put(size, bitmap)

        val count = sizes[size]
        sizes[size] = if (count == null) 1 else count + 1
    }

    override fun get(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap? {
        //计算出目标 bitmap 占用的大小
        val size = Utils.calculateAllocationByteCount(width, height, config)

        //此步骤用于查找 entries 中最合适用来复用的 bitmap
        //先找到 sizes 中大于 size 且和 size 最接近的 key，如果 key 存在且其大小不超出 size 的四倍，则使用该 key，否则直接使用 size
        //即找到最接近 size 但又不超出 size 太多的 bitmap，否则如果拿来复用的 bitmap 太大的话也比较浪费
        val bestSize = sizes.ceilingKey(size)?.takeIf { it <= MAX_SIZE_MULTIPLE * size } ?: size

        // Always call removeLast so bestSize becomes the head of the linked list.
        val bitmap = entries.removeLast(bestSize)
        if (bitmap != null) {
            decrementSize(bestSize)
            //将拿来复用的 bitmap 转换为目标大小和目标配置
            bitmap.reconfigure(width, height, config)
        }
        return bitmap
    }

    ···
    
}
```

Coil 在**加载网络图片**和**对图片进行变换**的时候就会从 RealBitmapPool 取出 Bitmap 来进行复用

Coil 默认是通过 OkHttp 来请求网络图片的，在拿到 BufferedSource 后就会通过 BitmapFactoryDecoder 的 `decodeInterruptible` 方法来将 BufferedSource 转换为 Bitmap。Coil 内部也要使用到系统的 BitmapFactory 来生成 Bitmap，所以`decodeInterruptible` 方法会尝试向 BitmapFactory.Options 设置 inBitmap 属性来实现复用，inBitmap 的数据来源就是从 BitmapPool 中取的

```kotlin
private fun decodeInterruptible(
        pool: BitmapPool,
        source: Source,
        size: Size,
        options: Options
    ): DecodeResult = BitmapFactory.Options().run {
        val safeSource = ExceptionCatchingSource(source)
        val safeBufferedSource = safeSource.buffer()

    
        //先去读取 source 对应的 bitmap 的宽高大小
    	//即拿到 outWidth 和 outHeight
        inJustDecodeBounds = true
        BitmapFactory.decodeStream(safeBufferedSource.peek().inputStream(), null, this)
        safeSource.exception?.let { throw it }
    
        inJustDecodeBounds = false

        ···

        when {
            outWidth <= 0 || outHeight <= 0 -> {
                inSampleSize = 1
                inScaled = false
                //读取宽高属性失败，此时不知道无法获取到合适大小的 inBitmap，因此将其置为 null
                inBitmap = null
            }
            size !is PixelSize -> {
                // This occurs if size is OriginalSize.
                inSampleSize = 1
                inScaled = false
                if (inMutable) {
                    //外部在加载图片要求原图大小
                    //那么也按原图大小来生成 inBitmap
                    inBitmap = pool.getDirty(outWidth, outHeight, inPreferredConfig)
                }
            }
            else -> {
                ···
                if (inMutable) {
                    //根据是否采样 inSampleSize 和是否缩放 inScaled 来设置 inBitmap
                    inBitmap = when {
                        // If we're not scaling the image, use the image's source dimensions.
                        inSampleSize == 1 && !inScaled -> {
                            pool.getDirty(outWidth, outHeight, inPreferredConfig)
                        }
                        // We can only re-use bitmaps that don't match the image's source dimensions on API 19 and above.
                        SDK_INT >= 19 -> {
                            // Request a slightly larger bitmap than necessary as the output bitmap's dimensions
                            // may not match the requested dimensions exactly. This is due to intricacies in Android's
                            // downsampling algorithm across different API levels.
                            val sampledOutWidth = outWidth / inSampleSize.toDouble()
                            val sampledOutHeight = outHeight / inSampleSize.toDouble()
                            pool.getDirty(
                                width = ceil(scale * sampledOutWidth + 0.5).toInt(),
                                height = ceil(scale * sampledOutHeight + 0.5).toInt(),
                                config = inPreferredConfig
                            )
                        }
                        // Else, let BitmapFactory allocate the bitmap internally.
                        else -> null
                    }
                }
            }
        }

        val inBitmap: Bitmap? = inBitmap
        var outBitmap: Bitmap? = null
        try {
            outBitmap = safeBufferedSource.use {
                //在这里来真正生成 Bitmap
                if (SDK_INT < 19 && outMimeType == null) {
                    val bytes = it.readByteArray()
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, this)
                } else {
                    BitmapFactory.decodeStream(it.inputStream(), null, this)
                }
            }
            safeSource.exception?.let { throw it }
        } catch (throwable: Throwable) {
            //生成 Bitmap 的过程中抛出了异常，那么就尝试回收 inBitmap 和 outBitmap
            inBitmap?.let(pool::put)
            if (outBitmap !== inBitmap) {
                outBitmap?.let(pool::put)
            }
            throw throwable
        }

        ···
    }
```

此外，图片变换是基本所有的图片加载库都会支持的功能，Coil 对这个功能的抽象即 Transformation 接口，圆角和模糊处理等效果都需要通过该接口来实现。而在图片变换的过程中往往需要使用到一个空白的 Bitmap，所以`transform` 方法就包含了一个 BitmapPool 参数提供给子类使用

```kotlin
interface Transformation {
    fun key(): String
    suspend fun transform(pool: BitmapPool, input: Bitmap, size: Size): Bitmap
}
```

例如，Coil 默认提供了 CircleCropTransformation 用于实现圆角效果，CircleCropTransformation 会通过 BitmapPool 先获取到一个空白的 Bitmap，然后在这个空白的 Bitmap 上面将原始的 Bitmap 绘制上去，从而尽量复用现有的 Bitmap

```kotlin
class CircleCropTransformation : Transformation {

    override fun key(): String = CircleCropTransformation::class.java.name

    override suspend fun transform(pool: BitmapPool, input: Bitmap, size: Size): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val minSize = min(input.width, input.height)
        val radius = minSize / 2f
        
        //拿到一个空白的 Bitmap
        val output = pool.get(minSize, minSize, input.safeConfig)
        output.applyCanvas {
           	//先绘制圆形
            drawCircle(radius, radius, radius, paint)
            paint.xfermode = XFERMODE
            //在 output 之上绘制原始的 Bitmap
            drawBitmap(input, radius - input.width / 2f, radius - input.height / 2f, paint)
        }
        return output
    }

    override fun equals(other: Any?) = other is CircleCropTransformation

    override fun hashCode() = javaClass.hashCode()

    override fun toString() = "CircleCropTransformation()"

    private companion object {
        val XFERMODE = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    }
}
```

# 四、Glide 对 Bitmap 的复用

理解了 Coil 对于 Bitmap 的缓存复用逻辑之后，再来看 Glide 就会简单很多了，两者在这方面的实现高度一致，甚至接口名和类名都很类似。准确来说应该是 Coil 借鉴了 Glide 的实现思路，Coil 作为一个后起之秀借鉴了 Glide 和 OkHttp 这两个开源库很多实现思路，看 Coil 源码的时候就总能发现这两个开源库的影子

Glide 中也包含一个 BitmapPool 接口，其实现类有两个，一个是空实现 BitmapPoolAdapter，一个是有实际意义的 LruBitmapPool

```java
public interface BitmapPool {
  long getMaxSize();
  void setSizeMultiplier(float sizeMultiplier);
  void put(Bitmap bitmap);
  @NonNull
  Bitmap get(int width, int height, Bitmap.Config config);
  @NonNull
  Bitmap getDirty(int width, int height, Bitmap.Config config);
  void clearMemory();
  void trimMemory(int level);
}

public class LruBitmapPool implements BitmapPool {
  private static final String TAG = "LruBitmapPool";
  private static final Bitmap.Config DEFAULT_CONFIG = Bitmap.Config.ARGB_8888;

  private final LruPoolStrategy strategy;
  private final Set<Bitmap.Config> allowedConfigs;
  private final long initialMaxSize;
  private final BitmapTracker tracker;

  private long maxSize;
  private long currentSize;
  private int hits;
  private int misses;
  private int puts;
  private int evictions;

  // Exposed for testing only.
  LruBitmapPool(long maxSize, LruPoolStrategy strategy, Set<Bitmap.Config> allowedConfigs) {
    this.initialMaxSize = maxSize;
    this.maxSize = maxSize;
    this.strategy = strategy;
    this.allowedConfigs = allowedConfigs;
    this.tracker = new NullBitmapTracker();
  }

  ···
}
```

LruBitmapPool 关联了一个 LruPoolStrategy 对象，由 LruPoolStrategy 来实现具体的 Bitmap 缓存复用逻辑，Bitmap 实际上都是交由 LruPoolStrategy 来存取

```java
interface LruPoolStrategy {
  void put(Bitmap bitmap);
  @Nullable
  Bitmap get(int width, int height, Bitmap.Config config);
  @Nullable
  Bitmap removeLast();
  String logBitmap(Bitmap bitmap);
  String logBitmap(int width, int height, Bitmap.Config config);
  int getSize(Bitmap bitmap);
}
```

LruPoolStrategy 包含两个实现类，和 Coil 的设计基本一样

- AttributeStrategy。用于 Android 4.4 之前的系统，将 `bitmapWidth、bitmapHeight、bitmapConfig` 这三者作为 Bitmap 的唯一标识，只有和这三个属性完全相等的 Bitmap 才能拿来复用
- SizeConfigStrategy。用于 Android 4.4 及之后的系统，将 `bitmapSize、bitmapConfig`这两者作为 Bitmap 的唯一标识，只有不小于目标大小且大小不超出八倍的 Bitmap 才能拿来复用（Coil 则要求不超出四倍）

Glide 在**加载网络图片**和**对图片进行变换**的时候也会从 BitmapPool 取出 Bitmap 来进行复用

Downsampler 的 `decodeFromWrappedStreams` 方法就实现了具体的解码逻辑。例如，在加载网络图片时就会根据拿到的 InputStream 包装为 ImageReader 对象，如果能够拿到 InputStream 对应的 Bitmap 的宽高大小的话，就会调用 `setInBitmap`方法去为 BitmapFactory.Options 设置 inBitmap 属性，inBitmap 就是从 bitmapPool 中拿的

```java
private Bitmap decodeFromWrappedStreams(
      ImageReader imageReader,
      BitmapFactory.Options options,
      DownsampleStrategy downsampleStrategy,
      DecodeFormat decodeFormat,
      PreferredColorSpace preferredColorSpace,
      boolean isHardwareConfigAllowed,
      int requestedWidth,
      int requestedHeight,
      boolean fixBitmapToRequestedDimensions,
      DecodeCallbacks callbacks)
      throws IOException {
    
    ···
        
    if ((options.inSampleSize == 1 || isKitKatOrGreater) && shouldUsePool(imageType)) {
      ···
      // If this isn't an image, or BitmapFactory was unable to parse the size, width and height
      // will be -1 here.
      if (expectedWidth > 0 && expectedHeight > 0) {
          //拿到了 InputStream 对应的 Bitmap 宽高大小后，就为 Options 设置 inBitmap
        setInBitmap(options, bitmapPool, expectedWidth, expectedHeight);
      }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      boolean isP3Eligible =
          preferredColorSpace == PreferredColorSpace.DISPLAY_P3
              && options.outColorSpace != null
              && options.outColorSpace.isWideGamut();
      options.inPreferredColorSpace =
          ColorSpace.get(isP3Eligible ? ColorSpace.Named.DISPLAY_P3 : ColorSpace.Named.SRGB);
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      options.inPreferredColorSpace = ColorSpace.get(ColorSpace.Named.SRGB);
    }

    //去实际解码 Bitmap
    Bitmap downsampled = decodeStream(imageReader, options, callbacks, bitmapPool);
    
    ···
        
    Bitmap rotated = null;
    if (downsampled != null) {
      // If we scaled, the Bitmap density will be our inTargetDensity. Here we correct it back to
      // the expected density dpi.
      downsampled.setDensity(displayMetrics.densityDpi);

      rotated = TransformationUtils.rotateImageExif(bitmapPool, downsampled, orientation);
      if (!downsampled.equals(rotated)) {
        bitmapPool.put(downsampled);
      }
    }

    return rotated;
  }

  @TargetApi(Build.VERSION_CODES.O)
  private static void setInBitmap(BitmapFactory.Options options, BitmapPool bitmapPool, int width, int height) {
    @Nullable Bitmap.Config expectedConfig = null;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      if (options.inPreferredConfig == Config.HARDWARE) {
        return;
      }
      expectedConfig = options.outConfig;
    }

    if (expectedConfig == null) {
      expectedConfig = options.inPreferredConfig;
    }
    options.inBitmap = bitmapPool.getDirty(width, height, expectedConfig);
  }
```

此外，从系统源码中对 inBitmap 的注释说明可以看到，设置了此字段后如果解码失败将导致抛出 IllegalArgumentException

```java
/**
 * If set, decode methods that take the Options object will attempt to
 * reuse this bitmap when loading content. If the decode operation
 * cannot use this bitmap, the decode method will throw an
 * {@link java.lang.IllegalArgumentException}. 
 */
public Bitmap inBitmap;
```

Glide 的 `decodeStream` 方法就捕获了这个异常。如果在执行 `imageReader.decodeBitmap` 的过程中抛出了 IllegalArgumentException 且当前 inBitmap 不为 null 的话，那么就会捕获该异常，然后将 inBitmap 置为 null 再重新解码一次。如果 inBitmap 为 null 的情况下也发生了异常的话，`decodeStream`方法则会将异常直接抛出，即该方法最多进行两次解码。而 Coil 只会解码一次，没有 Glide 这种降级处理规则

```kotlin
private static Bitmap decodeStream(
      ImageReader imageReader,
      BitmapFactory.Options options,
      DecodeCallbacks callbacks,
      BitmapPool bitmapPool)
      throws IOException {
    if (!options.inJustDecodeBounds) {
      callbacks.onObtainBounds();
      imageReader.stopGrowingBuffers();
    }

    int sourceWidth = options.outWidth;
    int sourceHeight = options.outHeight;
    String outMimeType = options.outMimeType;
    final Bitmap result;
    TransformationUtils.getBitmapDrawableLock().lock();
    try {
      //去调用 BitmapFactory.decodeStream 方法来生成 Bitmap
      result = imageReader.decodeBitmap(options);
    } catch (IllegalArgumentException e) {
      IOException bitmapAssertionException =
          newIoExceptionForInBitmapAssertion(e, sourceWidth, sourceHeight, outMimeType, options);
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(
            TAG,
            "Failed to decode with inBitmap, trying again without Bitmap re-use",
            bitmapAssertionException);
      }
      if (options.inBitmap != null) {
        try {
          //回收 inBitmap，重新将其存到 bitmapPool 中
          //然后将 inBitmap 设为 null，再重新解码一次
          bitmapPool.put(options.inBitmap);
          options.inBitmap = null;
          return decodeStream(imageReader, options, callbacks, bitmapPool);
        } catch (IOException resetException) {
          throw bitmapAssertionException;
        }
      }
      throw bitmapAssertionException;
    } finally {
      TransformationUtils.getBitmapDrawableLock().unlock();
    }

    return result;
  }
```

Glide 对图片变换功能的抽象也叫做 Transformation 接口，我们一般是使用其子类 BitmapTransformation，用于对 Bitmap 做操作并返回操作结果，这一块逻辑和 Coil 基本一致

```java
public abstract class BitmapTransformation implements Transformation<Bitmap> {

  @NonNull
  @Override
  public final Resource<Bitmap> transform(
      @NonNull Context context, @NonNull Resource<Bitmap> resource, int outWidth, int outHeight) {
    if (!Util.isValidDimensions(outWidth, outHeight)) {
      throw new IllegalArgumentException(
          "Cannot apply transformation on width: "
              + outWidth
              + " or height: "
              + outHeight
              + " less than or equal to zero and not Target.SIZE_ORIGINAL");
    }
    //拿到 BitmapPool
    BitmapPool bitmapPool = Glide.get(context).getBitmapPool();
    //拿到原始的 Bitmap
    Bitmap toTransform = resource.get();
    //拿到目标宽高
    int targetWidth = outWidth == Target.SIZE_ORIGINAL ? toTransform.getWidth() : outWidth;
    int targetHeight = outHeight == Target.SIZE_ORIGINAL ? toTransform.getHeight() : outHeight;
    //去执行图片变换并拿到转换后的结果
    Bitmap transformed = transform(bitmapPool, toTransform, targetWidth, targetHeight);
    
    final Resource<Bitmap> result;
    if (toTransform.equals(transformed)) {
      result = resource;
    } else {
      result = BitmapResource.obtain(transformed, bitmapPool);
    }
    return result;
  }

  protected abstract Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight);
}
```

# 五、相关联文章

- [三方库源码笔记（9）-超详细的 Glide 源码详解](https://juejin.im/post/6891307560557608967)
- [三方库源码笔记（10）-Glide 你可能不知道的知识点](https://juejin.im/post/6892751013544263687)
- [三方库源码笔记（13）-可能是全网第一篇 Coil 的源码分析文章](https://juejin.cn/post/6897872882051842061)
- [聊聊 Bitmap 的一些知识点](https://juejin.cn/post/6952429810207424526)