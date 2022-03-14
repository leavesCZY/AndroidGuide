> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

> 对于 Android Developer 来说，很多开源库都是属于**开发必备**的知识点，从使用方式到实现原理再到源码解析，这些都需要我们有一定程度的了解和运用能力。所以我打算来写一系列关于开源库**源码解析**和**实战演练**的文章，初定的目标是 **EventBus、ARouter、LeakCanary、Retrofit、Glide、OkHttp、Coil** 等七个知名开源库，希望对你有所帮助 🤣🤣

上一篇文章中对 ARouter 的源码进行了一次全面解析，原理懂得了，那么就也需要进行一次实战才行。对于一个优秀的第三方库，开发者除了要学会如何使用外，更有难度的用法就是去了解实现原理、懂得如何改造甚至自己实现。本文就来自己动手实现一个路由框架，自己实现的目的不在于做到和 ARouter 一样功能完善，而只是一个练手项目，目的是在于加深对 ARouter 的原理理解，所以自己的自定义实现就叫 EasyRouter 吧 😂😂

EasyRouter 支持同个模块间及跨模块实现 Activity 的跳转，仅需要指定一个字符串 path 即可：

```kotlin
EasyRouter.navigation(EasyRouterPath.PATH_HOME)
```

最终实现的效果：

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/aba28a3aebf24575a181d2b9d116a02f~tplv-k3u1fbpfcp-zoom-1.image)

EasyRouter 的实现及使用一共涉及以下几个模块：

1. app。即项目的主模块，从这里跳到子模块
2. base。用于在多个模块间共享 path
3. easyrouter-annotation。用于定义和 EasyRouter 实现相关的**注解**和 **Bean 对象**
4. easyrouter-api。用于定义和 EasyRouter 实现相关的 API 入口
5. easyrouter-processor。用于定义和 EasyRouter 实现相关的注解处理器，在编译阶段使用
6. easyrouter_demo。子模块，用于测试 app 模块跳转到子模块是否正常

EasyRouter 的实现思路和 ARouter 略有不同。EasyArouter 将同个模块下的所有路由信息通过静态方法块来进行存储并初始化，最终会生成以下的辅助文件：

```java
package github.leavesc.easyrouter;

import java.util.HashMap;
import java.util.Map;

import github.leavesc.ctrlcv.easyrouter.EasyRouterHomeActivity;
import github.leavesc.ctrlcv.easyrouter.EasyRouterSubPageActivity;
import github.leavesc.easyrouterannotation.RouterBean;

/**
 * 这是自动生成的代码 by leavesC
 */
public class EasyRouterappLoader {
    public static final Map<String, RouterBean> routerMap = new HashMap<>();

    {
        routerMap.put("app/home", new RouterBean(EasyRouterHomeActivity.class, "app/home", "app"));
        routerMap.put("app/subPage", new RouterBean(EasyRouterSubPageActivity.class, "app/subPage", "app"));
    }
}
```

由于静态变量和静态方法块在类被加载前是不会被初始化的，所以也可以做到按需加载。即只有在外部发起跳转到 app 这个模块的请求的时候，EasyRouter 才会去实例化 EasyRouterappLoader 类，此时才会去加载 app 模块的所有路由表信息，从而避免了内存浪费

下面再来简单介绍下 EasyRouter 的实现过程

# 一、前置准备

由于路由框架是以模块为单位的，所以同个模块内的路由信息都可以存到同一个辅助文件中，而为了避免多个模块间出现生成的辅助文件重名的情况，所以外部需要主动配置每个模块的特定唯一标识，然后在编译阶段通过 `AbstractProcessor` 拿到这个唯一标识

例如，我为 `easyrouter-test` 这个模块设置的唯一标识就是 **RouterTest**

```groovy
kapt {
    arguments {
        arg("EASYROUTER_MODULE_NAME", "RouterTest")
    }
}
```

最终生成的辅助文件对应的包名会是固定的，但类名会包含这个唯一标识。而由于包名和类名的生成规则是有规律的，也方便在运行时拿到这个类，同时这也就要求同个模块下的路由路径 path 必须是属于同个 group

```java
package github.leavesc.easyrouter;

import java.util.HashMap;
import java.util.Map;

import github.leavesc.easyrouter_test.EasyRouterTestAActivity;
import github.leavesc.easyrouterannotation.RouterBean;

/**
 * 这是自动生成的代码 by leavesC
 */
public class EasyRouterRouterTestLoader {
    public static final Map<String, RouterBean> routerMap = new HashMap<>();

    {
        routerMap.put("RouterTest/testA", new RouterBean(EasyRouterTestAActivity.class, "RouterTest/testA", "RouterTest"));
    }
}
```

`@Router` 用于对 Activity 进行标注，仅需要设置一个参数 `path` 即可，`path` 包含的第一个单词就是 `group`

```kotlin
/**
 * @Author: leavesCZY
 * @Desc:
 * @Github：https://github.com/leavesCZY
 */
@MustBeDocumented
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Router(val path: String)

data class RouterBean(val targetClass: Class<*>, val path: String, val group: String)
```

# 二、注解处理器

声明一个 EasyRouterProcessor 类继承于 AbstractProcessor，在编译阶段通过扫描代码元素从而拿到 `@Router` 注解的信息

```kotlin
/**
 * @Author: leavesCZY
 * @Desc:
 * @Github：https://github.com/leavesCZY
 */
class EasyRouterProcessor : AbstractProcessor() {

    companion object {

        private const val KEY_MODULE_NAME = "EASYROUTER_MODULE_NAME"

        private const val PACKAGE_NAME = "github.leavesc.easyrouter"

        private const val DOC = "这是自动生成的代码 by leavesC"

    }

    private lateinit var elementUtils: Elements

    private lateinit var messager: Messager

    private lateinit var moduleName: String

    override fun init(processingEnvironment: ProcessingEnvironment) {
        super.init(processingEnvironment)
        elementUtils = processingEnv.elementUtils
        messager = processingEnv.messager
        val options = processingEnv.options
        moduleName = options[KEY_MODULE_NAME] ?: ""
        if (moduleName.isBlank()) {
            messager.printMessage(Diagnostic.Kind.ERROR, "$KEY_MODULE_NAME must not be null")
        }
    }

	···

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(Router::class.java.canonicalName)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.RELEASE_8
    }

    override fun getSupportedOptions(): Set<String> {
        return hashSetOf(KEY_MODULE_NAME)
    }

}
```

首先需要生成的 `routerMap`这个用于存储路由表信息的 Map 字段，其 key 值即 path，value 值即 path 对应的页面信息

```kotlin
//生成 routerMap 这个静态常量
private fun generateSubscriberField(): FieldSpec {
    val subscriberIndex = ParameterizedTypeName.get(
        ClassName.get(Map::class.java),
        ClassName.get(String::class.java),
        ClassName.get(RouterBean::class.java)
    )
    return FieldSpec.builder(subscriberIndex, "routerMap")
        .addModifiers(
            Modifier.PUBLIC,
            Modifier.STATIC,
            Modifier.FINAL
        )
        .initializer("new ${"$"}T<>()", HashMap::class.java)
        .build()
}
```

之后就需要生成静态方法块。拿到 `@Router` 注解包含的 path 属性，及被注解的类对应的 Class 对象，以此来构建一个 RouterBean 对象并存到 `routerMap`中

```kotlin
//生成静态方法块
private fun generateInitializerBlock(
    elements: MutableSet<out Element>,
    builder: TypeSpec.Builder
) {
    val codeBuilder = CodeBlock.builder()
    elements.forEach {
        val router = it.getAnnotation(Router::class.java)
        val path = router.path
        val group = path.substring(0, path.indexOf("/"))
        codeBuilder.add(
            "routerMap.put(${"$"}S, new ${"$"}T(${"$"}T.class, ${"$"}S, ${"$"}S));",
            path,
            RouterBean::class.java,
            it.asType(),
            path,
            group
        )
    }
    builder.addInitializerBlock(
        codeBuilder.build()
    )
}
```

然后在 `process`方法中完成辅助文件的生成

```kotlin
override fun process(
    mutableSet: MutableSet<out TypeElement>,
    roundEnvironment: RoundEnvironment
): Boolean {
    val elements: MutableSet<out Element> =
        roundEnvironment.getElementsAnnotatedWith(Router::class.java)
    if (elements.isNullOrEmpty()) {
        return true
    }
    val typeSpec = TypeSpec.classBuilder("EasyRouter" + moduleName + "Loader")
        .addModifiers(Modifier.PUBLIC)
        .addField(generateSubscriberField())
        .addJavadoc(DOC)
    generateInitializerBlock(elements, typeSpec)
    val javaFile = JavaFile.builder(PACKAGE_NAME, typeSpec.build())
        .build()
    try {
        javaFile.writeTo(processingEnv.filer)
    } catch (e: Throwable) {
        e.printStackTrace()
    }
    return true
}
```

# 三、EasyRouter

EasyRouter 这个单例对象即最终提供给外部的调用入口，总代码行数不到五十行。外部通过调用 `navigation` 方法并传入目标页面 path 来实现跳转，通过 path 来判断其所属 group，并尝试加载其所在模块生成的辅助文件，如果加载成功则能成功跳转，否则就 Toast 提示

```kotlin
/**
 * @Author: leavesCZY
 * @Desc:
 * @Github：https://github.com/leavesCZY
 */
object EasyRouter {

    private const val PACKAGE_NAME = "github.leavesc.easyrouter"

    private lateinit var context: Application

    private val routerByGroupMap = hashMapOf<String, Map<String, RouterBean>>()

    fun init(application: Application) {
        this.context = application
    }

    fun navigation(path: String) {
        val routerBean = getRouterLoader(path)
        if (routerBean == null) {
            Toast.makeText(context, "找不到匹配的路径：$path", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(context, routerBean.targetClass)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    private fun getRouterLoader(path: String): RouterBean? {
        val group = path.substring(0, path.indexOf("/"))
        val map = routerByGroupMap[group]
        if (map == null) {
            var routerMap: Map<String, RouterBean>? = null
            try {
                val classPath = PACKAGE_NAME + "." + "EasyRouter" + group + "Loader"
                val clazz = Class.forName(classPath)
                val instance = clazz.newInstance()
                val routerMapField = clazz.getDeclaredField("routerMap")
                routerMap =
                    (routerMapField.get(instance) as? Map<String, RouterBean>) ?: hashMapOf()
                routerByGroupMap[group] = routerMap
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                if (routerMap == null) {
                    routerByGroupMap[group] = hashMapOf()
                }
            }
        }
        return routerByGroupMap[group]?.get(path)
    }

}
```

# 四、GitHub

由于只是为了加深对 ARouter 的实现原理的理解，所以才来尝试实现 EasyRouter，也不打算实现得多么功能齐全，但对于一些读者来说我觉得还是有参考价值的😂😂 这里也提供上述代码的 GitHub 链接：[AndroidOpenSourceDemo](https://github.com/leavesCZY/AndroidOpenSourceDemo)