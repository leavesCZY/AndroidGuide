> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣🤣

# 一、Trace

在最近的一年多时间里，我先后写过六篇关于 Android 字节码插桩的文章，一共对应四个功能点

- [ASM 字节码插桩：实现双击防抖](https://juejin.cn/post/7042328862872567838)
- [ASM 字节码插桩：进行线程整治](https://juejin.cn/post/7044339202997092383)
- [ASM 字节码插桩：助力隐私合规](https://juejin.cn/post/7046207125785149448)
- [ASM 字节码插桩：监控大图加载](https://juejin.cn/post/7074970389188706318)
- [ASM 字节码插桩：从 Lambda 表达式讲起](https://juejin.cn/post/7151798531672506398)
- [ASM 字节码插桩：Jetpack Compose 实现双击防抖](https://juejin.cn/post/7158061389503250445)

发布这几篇文章后，有读者问过我是否支持直接远程依赖使用，由于当时我并没有将代码托管到公共 Maven，仅是在 Github 开源了相关的实现代码而已：[asm-samples](https://github.com/leavesCZY/asm-samples)，所以在当时是不支持的

最近比较有空，想着很久没有写文章了，想水一篇，就花了点时间将其中两个我觉得还比较实用的功能点抽取了出来，并新增了一个功能点一并发布到了 Github：[Trace](https://github.com/leavesCZY/Trace)，然后也将 Trace 发布到了 Gradle Plugins 官网，方便开发者在自己项目中直接远程依赖使用

这三个功能点包括：

- 应用双击防抖。包括 Android 原生的 View 体系以及目前流行的 Jetpack Compose，对应第一篇和最后两篇文章
- 替换 Class 的继承关系。可用于非侵入式地实现监控大图加载的功能，对应第四篇文章
- 修复 Toast 在 Android 7.1 上的系统 bug。这是我新写的一个功能点，用于解决在 Android 7.1 系统上 Toast 由于 WindowToken 失效从而导致应用崩溃的问题

下面就来介绍如何在项目中接入 Trace，主要的实现思路参照以上文章即可

# 二、引入

在项目根目录下的 `build.gradle` 或者 `build.gradle.kts` 中引入插件

```kotlin
//grovy
plugins {
    id "io.github.leavesczy.trace" version "latestVersion" apply false
}
```

```kotlin
//kts
plugins {
    id("io.github.leavesczy.trace").version("latestVersion").apply(false)
}
```

在项目主模块下的 `build.gradle` 或者 `build.gradle.kts` 中应用插件，需要哪些功能点就为其设置对应的参数即可

```kotlin
//grovy
plugins {
    id("io.github.leavesczy.trace")
}

clickTrace {
    view.onClickClass = "x"
    view.onClickMethodName = "x"
    view.uncheckViewOnClickAnnotation = "x"
    view.include = []
    view.exclude = []
    compose.onClickClass = "x"
    compose.onClickWhiteList = "x"
}

replaceClassTrace {
    originClass = "x"
    targetClass = "x"
    include = []
    exclude = []
}

toastTrace {
    toasterClass = "x"
    showToastMethodName = "x"
}
```

```kotlin
//kts
plugins {
    id("io.github.leavesczy.trace")
}

clickTrace {
    view {
        onClickClass = "x"
        onClickMethodName = "x"
        uncheckViewOnClickAnnotation = "x"
        include = listOf()
        exclude = listOf()
    }
    compose {
        onClickClass = "x"
        onClickWhiteList = "x"
    }
}

replaceClassTrace {
    originClass = "x"
    targetClass = "x"
    include = listOf()
    exclude = listOf()
}

toastTrace {
    toasterClass = "x"
    showToastMethodName = "x"
}
```

# 三、ClickTrace

ClickTrace 用于实现应用双击防抖功能，同时支持 Android 原生的 View 体系以及目前流行的 Jetpack Compose

ClickTrace  实现应用双击防抖功能的本质，就是为项目中所有使用了 `View.OnClickListener`（View 体系）和 `Modifier.clickable、Modifier.combinedClickable`（Jetpack Compose 体系）的回调方法中都插入一段逻辑代码，该段代码会计算前后两次点击事件的时间间隔，如果判断到时间间隔小于某个阈值的话就直接 return，否则就让其继续执行

## 1、View

想要实现 Android View 体系下的双击防抖功能，开发者一共需要设置两个必填参数和三个可选参数

```kotlin
clickTrace {
    //必填参数
    view.onClickClass = "x"
    view.onClickMethodName = "x"
    //可选参数
    view.uncheckViewOnClickAnnotation = "x"
    view.include = []
    view.exclude = []
}
```

View 体系的双击防抖功能对应的伪代码如下所示。开发者需要在自己的项目中提供一个方法，用于承接 ClickTrace 转发的所有 View 点击事件。ClickTrace 就负责将开发者提供的 `ViewClickMonitor.isEnabled(View)` 方法插入到 `View.OnClickListener` 的回调函数中，由方法返回值来决定是否要执行本次点击事件

```kotlin
//插桩前
view.setOnClickListener(object : View.OnClickListener {
    override fun onClick(view: View) {
        //TODO
    }
})

//插桩后
view.setOnClickListener(object : View.OnClickListener {
    override fun onClick(view: View) {
        if (!ViewClickMonitor.isEnabled(view)){
            return
        }
        //TODO
    }
})
```

ViewClickMonitor 的包名、类名、方法名均可以随意命名，ClickTrace 仅要求其包含一个静态方法，方法签名和 `isEnabled` 保持一致即可，返回值为 true 即代表允许执行本次点击事件

```kotlin
object ViewClickMonitor {

    @JvmStatic
    fun isEnabled(view: View): Boolean {
        val isEnabled: Boolean
        //TODO
        return isEnabled
    }

}
```

例如，开发者可以照着以下代码来实现 ViewClickMonitor，将每次点击事件的最小时间间隔设为五百毫秒

```kotlin
package github.leavesczy.trace

object ViewClickMonitor {

    private const val MIN_DURATION = 500L

    private var lastClickTime = 0L

    private var clickIndex = 0

    @JvmStatic
    fun isEnabled(view: View): Boolean {
        clickIndex++
        val currentTime = SystemClock.elapsedRealtime()
        val isEnabled = currentTime - lastClickTime > MIN_DURATION
        if (isEnabled) {
            lastClickTime = currentTime
        }
        log("onClick $clickIndex , isEnabled : $isEnabled")
        return isEnabled
    }

    private fun log(log: String) {
        Log.e(javaClass.simpleName, log)
    }

}
```

然后将 ViewClickMonitor 的类名和对应的方法名传给 ClickTrace 即可

```kotlin
clickTrace {
    view.onClickClass = "github.leavesczy.trace.ViewClickMonitor"
    view.onClickMethodName = "isEnabled"
}
```

在默认情况下，ClickTrace 会对整个项目中的所有 `onClick` 事件均进行拦截检测。如果想过滤特定的点击事件，或者是想过滤特定类或者是特定包名，可以通过 ClickTrace 的另外三个可选参数来实现

```kotlin
clickTrace {
    //过滤包含特定注解的 onClick 事件
    view.uncheckViewOnClickAnnotation = "x"
    //仅对特定类或者特定包名中的 onClick 事件进行拦截检测
    view.include = ["x"]
    //过滤特定类或者特定包名中的 onClick 事件
    view.exclude = ["x"]
}
```

例如，开发者可以自己声明一个 UncheckViewOnClick 注解

```kotlin
package github.leavesczy.trace

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class UncheckViewOnClick
```

将该注解的全路径传给 `uncheckViewOnClickAnnotation` 后，以下点击事件就会被过滤

```kotlin
findViewById<View>(R.id.tvObjectUnCheck).setOnClickListener(
    object : View.OnClickListener {
        @UncheckViewOnClick
        override fun onClick(view: View) {
            onClickView()
        }
    })
```

`include` 和 `exclude` 两个参数则用于以类名或包名为单位，共同控制 ClickTrace 的生效范围

- include 用于设定 ClickTrace 的生效范围。参数值在为空的情况下代表着对所有所有模块均生效，传值后则只对该参数值代表的模块生效
- exclude 用于设定 ClickTrace 的排除范围。用于在 include 限定的范围内再排除特定模块

`include` 和 `exclude` 均通过正则表达式来进行传值，ClickTrace 每当遍历到一个类时，均会拿其类名和 `include` 和 `exclude` 一起进行匹配，均匹配通过后才会对该类进行双击防抖

例如，以下参数就表示：

- 包含 UncheckViewOnClick 注解的 `onClick` 回调不会进行双击防抖
- 仅在 `github.leavesczy.trace.xxx` 包名下的类会进行双击防抖，但 `github.leavesczy.trace.mylibrary.xxx` 包名下的类除外

```kotlin
clickTrace {
    view.uncheckViewOnClickAnnotation = "github.leavesczy.trace.UncheckViewOnClick"
    view.include = ["^github\\.leavesczy\\.trace.*"]
    view.exclude = ["^github\\.leavesczy\\.trace\\.mylibrary.*"]
}
```

## 2、Jetpack Compose

想要实现 Jetpack Compose 的双击防抖功能，开发者一共需要设置一个必填参数和一个可选参数

```kotlin
clickTrace {
	//必填参数
	compose.onClickClass = "x"
	//可选参数
	compose.onClickWhiteList = "x"
}
```

和 View 体系一样，开发者也需要在自己项目中声明一个符合以下签名的类，ComposeOnClick 的包名和类名均可以随意命名，将该类的全路径作为参数值传递给 `onClickClass` 即可

```kotlin
class ComposeOnClick(private val onClick: () -> Unit) : Function0<Unit> {

    override fun invoke() {
        //TODO
    }
    
}
```

例如，开发者可以照着以下代码来实现 ComposeOnClick

```kotlin
package github.leavesczy.trace

class ComposeOnClick(private val onClick: () -> Unit) : Function0<Unit> {

    companion object {

        private const val MIN_DURATION = 500L

        private var lastClickTime = 0L

    }

    override fun invoke() {
        val currentTime = SystemClock.elapsedRealtime()
        val isEnabled = currentTime - lastClickTime > MIN_DURATION
        log("onClick isEnabled : $isEnabled")
        if (isEnabled) {
            lastClickTime = currentTime
            onClick()
        }
    }

    private fun log(log: String) {
        Log.e(
            javaClass.simpleName,
            "${System.identityHashCode(this)} ${System.identityHashCode(onClick)} $log"
        )
    }

}
```

另外，`onClickWhiteList` 即点击事件的白名单，对于某些不希望执行双击防抖的 `Modifier.clickable` 和 `Modifier.combinedClickable` 方法，通过将其 `onClickLabel` 设置为 `onClickWhiteList` 的属性值，就不会进行双击防抖

例如，以下参数就表示：`Modifier.clickable` 和 `Modifier.combinedClickable` 方法触发的点击事件均会被移交给 ComposeOnClick 处理，`onClickLabel` 属性值为 `notCheck` 的点击事件除外

```kotlin
clickTrace {
	compose.onClickClass = "github.leavesczy.trace.ComposeOnClick"
	compose.onClickWhiteList = "notCheck"
}
```

# 四、ReplaceClassTrace

ReplaceClassTrace 用于替换项目中类的继承关系。也就是说，ReplaceClassTrace 会将项目中每一个 `originClass` 的直接子类，均将其改为直接继承于 `targetClass`。此外，ReplaceClassTrace 还包含 `include` 和 `exclude` 两个可选参数，其作用和 ClickTrace 中的同名参数一致

```kotlin
replaceClassTrace {
    //必填参数
    originClass = "x"
    targetClass = "x"
    //可选参数
    include = []
    exclude = []
}
```

这个功能有什么意义呢？

以文章开头所说的 **监控大图加载** 功能作为例子

假设现在要来检测项目中的所有 ImageView 加载的图片尺寸是否过大，此时我们就可以自定义实现一个 ImageView 的子类 MonitorImageView，在其中实现好大图检测的功能，然后再通过 ReplaceClassTrace 将所有直接继承于 ImageView 的子类均改为直接继承于 MonitorImageView，从而使得大图检测的功能对整个项目均能生效，而且还不必手动修改现有代码

例如，以下参数就表示：将项目中所有直接继承于 ImageView 的子类，均改为直接继承于 MonitorImageView，但类名为 IgnoreImageView 的子类除外

```kotlin
replaceClassTrace {
    originClass = "android.widget.ImageView"
    targetClass = "github.leavesczy.trace.MonitorImageView"
    include = []
    exclude = [".*\\.IgnoreImageView\$"]
}
```

# 五、ToastTrace

ToastTrace 用于聚拢项目中所有调用系统 Toast 的显示操作，可用于解决在 Android 7.1 系统中 Toast 由于 WindowToken 失效从而导致应用崩溃的问题

开发者一共需要设置两个必填参数

```kotlin
toastTrace {
    toasterClass = "x"
    showToastMethodName = "x"
}
```

开发者需要在自己的项目中提供一个方法，用于承接 ToastTrace 转发的所有 Toast 显示操作。ToastTrace 就负责将项目中所有调用了 `toast.show()` 的操作都聚拢到开发者指定的方法下，开发者可以在该方法内对 Android 7.1 中 Toast 的系统 bug 进行修复

例如，开发者可以像如下代码一样来承接 `toast.show()` 操作，在 Android 7.1 系统版本上捕获系统抛出的异常。Toaster 的包名、类名、方法名均可以随意命名，ToastTrace 仅要求其包含一个静态方法，方法签名符合 `showToast` 的规则即可

```kotlin
package github.leavesczy.trace

object Toaster {

    @JvmStatic
    fun showToast(toast: Toast) {
        hookToastIfNeed(toast)
        toast.show()
    }

    @SuppressLint("DiscouragedPrivateApi")
    private fun hookToastIfNeed(toast: Toast) {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1) {
            try {
                val cToast = Toast::class.java
                val fTn = cToast.getDeclaredField("mTN")
                fTn.isAccessible = true
                val oTn = fTn.get(toast)
                val cTn = oTn.javaClass
                val fHandle = cTn.getDeclaredField("mHandler")
                fHandle.isAccessible = true
                fHandle.set(oTn, ProxyHandler(fHandle.get(oTn) as Handler))
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    private class ProxyHandler(private val mHandler: Handler) : Handler(mHandler.looper) {

        override fun handleMessage(msg: Message) {
            try {
                mHandler.handleMessage(msg)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

    }

}
```

然后，将 Toaster 对应的全路径和方法名传给 ToastTrace 即可

```kotlin
toastTrace {
    toasterClass = "github.leavesczy.trace.Toaster"
    showToastMethodName = "showToast"
}
```

# 六、结尾

Trace 现阶段就包含以上三个功能点，后续看时间规划我再继续更新

另外还有一些注意事项

- Trace 在 AGP 7.0+ 和 8.0+ 均已测试通过，更低版本的 AGP 则没有再特意进行试验
- Trace 目前处于刚起步阶段，可能还会存在一些 bug，但由于 Trace 是以 Gradle Plugin 的形式引入到项目中的，引入成本和移除成本都很低，有需要的话还是值得一试的
- Trace Plugin 也托管到了 GradlePluginPortal，可以在 [Trace](https://plugins.gradle.org/plugin/io.github.leavesczy.trace) 查看

希望对你有所帮助 ~

Trace 的 Github 地址：[Trace](https://github.com/leavesCZY/Trace)