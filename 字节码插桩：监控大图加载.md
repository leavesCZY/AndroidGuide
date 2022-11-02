> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

加载图片是一个很常规的操作，同时也是一个“成本”较高的行为，因为加载一张图片可能需要先后历经 **网络请求、I/O 读写、内存占用** 等多个过程。我们一般是通过 Coil、Glide 等开源库来加载图片，完全无需关心其加载过程，而其中可能就隐藏着一个不是很合理的情况：加载的图片属于大图，超出了展示所需

加载展示所需的图片会造成不必要的性能浪费，同时也可能会引发 OOM，因此进行应用性能优化的一个点就是检测应用全局的图片加载情况，本文就来介绍如何通过字节码插桩的方式来实现全局大图检测

首先，什么类型的图片属于大图呢？我觉得可以从两个方面来进行认定：

- 图片的尺寸大于 ImageView 本身的尺寸。例如，ImageView 的宽高只有 100 dp，但图片却有 200 dp
- 图片的大小超过一定阈值。例如，我们可以规定单张图片最多不能超出 1 MB，大于该值的图片就认为是大图

我们项目中使用的 ImageView 的类型又可以分为两种：

- 系统内置的`android.widget.ImageView`。一般是在 XML 文件中通过 `<ImageView/>` 标签来进行使用
- 开发者自定义的 ImageView 子类。一般也是在 XML 文件使用

因此，基本的实现思路就是：通过定义一个统一的 ImageView 供项目全局使用，用于替代系统内置的 ImageView 和各个自定义子类的直接父类，当 `setImageDrawable` 和 `setImageBitmap` 两个方法被调用时，就对 Drawable 的尺寸和大小进行检测，当检测到属于大图时就按照实际的业务情况进行数据上报

```kotlin
open class MonitorImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : android.widget.ImageView(context, attrs, defStyleAttr), MessageQueue.IdleHandler {

    companion object {

        private const val MAX_ALARM_IMAGE_SIZE = 1024

    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        monitor()
    }

    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
        monitor()
    }

    private fun monitor() {
        Looper.myQueue().removeIdleHandler(this)
        Looper.myQueue().addIdleHandler(this)
    }

    override fun queueIdle(): Boolean {
        checkDrawable()
        return false
    }

    private fun checkDrawable() {
        val mDrawable = drawable ?: return
        val drawableWidth = mDrawable.intrinsicWidth
        val drawableHeight = mDrawable.intrinsicHeight
        val viewWidth = measuredWidth
        val viewHeight = measuredHeight
        val imageSize = calculateImageSize(mDrawable)
        if (imageSize > MAX_ALARM_IMAGE_SIZE) {
            log(log = "图片大小超标 -> $imageSize")
        }
        if (drawableWidth > viewWidth || drawableHeight > viewHeight) {
            log(log = "图片尺寸超标 -> drawable：$drawableWidth x $drawableHeight  view：$viewWidth x $viewHeight")
        }
    }

    private fun calculateImageSize(drawable: Drawable): Int {
        return when (drawable) {
            is BitmapDrawable -> {
                drawable.bitmap.byteCount
            }
            else -> {
                0
            }
        }
    }

    private fun log(log: String) {
        Log.e(javaClass.simpleName, log)
    }

}
```

当然，我们也不太可能采取硬编码的方式来直接修改项目中的原有逻辑，成本太高，不灵活，而且也无法照顾到外部依赖。此时通过字节码插桩的方式来实现就成了比较经济和高效的方案，可以做到多项目复用

对于开发者自定义的 ImageView 子类，我们只需要在 Transform 阶段，当检查到当前 Class 直接继承于系统的 ImageView，就将其改为继承于 MonitorImageView 即可。稍微麻烦一点的是在 XML 中声明的 `<ImageView/>` 标签

我们知道，在布局文件中声明的各个控件，在使用时都对应一个个具体的 View 实例对象，而想要将静态的 XML 声明转换为动态的实例对象，就需要通过解析 XML 文件并根据类路径来反射出实例对象了，这一部分逻辑就隐藏在 LayoutInflater 中，LayoutInflater 会根据我们传入的 layoutResID 来进行解析

![](https://upload-images.jianshu.io/upload_images/2552605-7228005523bcb4df.png)

另一方面，现如今我们在新建 Activity 时，一般都不会直接继承于系统内置的 `android.app.Activity`，而是使用 `androidx.appcompat.app.AppCompatActivity`，AppCompatActivity 提供了更多的兼容性保障，当中就包含了自定义实现的 LayoutInflater

AppCompatActivity 通过 AppCompatViewInflater 来解析 XML 文件，当判断到我们声明的是系统控件时（例如 TextView、ImageView、Button 等），就会使用对应的 AppCompatXXX 来生成相应的实例对象，ImageView 就对应 AppCompatImageView

![](https://upload-images.jianshu.io/upload_images/2552605-ced3d23ff32c40a7.png)

所以说，大多数情况下我们使用的 ImageView 实例对应的其实都是 `androidx.appcompat.widget.AppCompatImageView`，而非 `android.widget.ImageView`。这就为我们提供了一个 hook 点：只要我们能够将 AppCompatImageView 的父类修改为我们自定义的 MonitorImageView，就可以来为应用全局实现一个统一的大图检测功能了

有了上述思路后，相应的插桩代码也就很简单了

```kotlin
class LegalBitmapTransform(private val config: LegalBitmapConfig) : BaseTransform() {

    private companion object {

        private const val ImageViewClass = "android/widget/ImageView"

    }

    override fun modifyClass(byteArray: ByteArray): ByteArray {
        val classReader = ClassReader(byteArray)
        val className = classReader.className
        val superName = classReader.superName
        Log.log("className: $className superName: $superName")
        return if (className != config.formatMonitorImageViewClass && superName == ImageViewClass) {
            val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
            val classVisitor = object : ClassVisitor(Opcodes.ASM6, classWriter) {
                override fun visit(
                    version: Int,
                    access: Int,
                    name: String?,
                    signature: String?,
                    superName: String?,
                    interfaces: Array<out String>?
                ) {
                    super.visit(
                        version,
                        access,
                        name,
                        signature,
                        config.formatMonitorImageViewClass,
                        interfaces
                    )
                }
            }
            classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
            classWriter.toByteArray()
        } else {
            byteArray
        }
    }

}
```

最后也给出完整的源码：[ASM_Transform](https://github.com/leavesCZY/ASM_Transform)

字节码插桩的更多实践场景看这里：

- [ASM 字节码插桩：实现双击防抖](https://mp.weixin.qq.com/s?__biz=MzAxMTYzNTIyMA==&mid=2247492383&idx=1&sn=fe3a7db1ad7b5c1e506df5674836f016&chksm=9bbcbc64accb3572202cb1ea6163534b0cb66c67195a01f4d0ecc8040f66b55ea618a0a09ca9&token=1916624675&lang=zh_CN#rd)
- [ASM 字节码插桩：进行线程整治](https://mp.weixin.qq.com/s?__biz=MzAxMTYzNTIyMA==&mid=2247492405&idx=1&sn=69ba6a0af55c9ae0da5b87439f356ee5&chksm=9bbcbc4eaccb3558efa2b18e9a0a028f64828429fb1925cbac23ccaa43a5c6b8f6cd960f7f5c&token=1916624675&lang=zh_CN#rd)
- [ASM 字节码插桩：助力隐私合规](https://mp.weixin.qq.com/s?__biz=MzAxMTYzNTIyMA==&mid=2247492421&idx=1&sn=72bc5e58f028dd77abf59cf8fb8e0013&chksm=9bbcbc3eaccb35287e4e4b2b8f520e3baefea93a754e80fa5d2c2cb32b5b0475ea6e4b1db176#rd)