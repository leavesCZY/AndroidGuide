> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

> 对于 Android Developer 来说，很多开源库都是属于**开发必备**的知识点，从使用方式到实现原理再到源码解析，这些都需要我们有一定程度的了解和运用能力。所以我打算来写一系列关于开源库**源码解析**和**实战演练**的文章，初定的目标是 **EventBus、ARouter、LeakCanary、Retrofit、Glide、OkHttp、Coil** 等七个知名开源库，希望对你有所帮助 🤣🤣

上一篇文章中对 EventBus 进行了一次全面的源码解析，原理懂得了，那么也需要来进行一次实战才行。对于一个优秀的第三方库，开发者除了要学会如何使用外，更有难度的用法就是去了解实现原理，懂得如何改造甚至是自己实现。本篇文章就来自己动手实现一个 EventBus，不求功能多齐全，就来实现简单的**注册、反注册、发送消息、接收消息**这些功能即可 😇😇

先来看下最终的实现效果

对于以下两个监听者：EasyEventBusMainActivity 和 EasyEventBusTest，通过标注 `@Event`注解来修饰监听方法，然后使用 EasyEventBus 这个自定义类来进行**注册、反注册和发送消息**

```kotlin
/**
 * @Author: leavesCZY
 * @Desc:
 * @Github：https://github.com/leavesCZY
 */
class EasyEventBusMainActivity : BaseActivity() {

    override val bind by getBind<ActivityEasyEventBusMainBinding>()

    private val eventTest = EasyEventBusTest()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EasyEventBus.register(this)
        eventTest.register()
        bind.btnPostString.setOnClickListener {
            EasyEventBus.post("Hello")
        }
        bind.btnPostBean.setOnClickListener {
            EasyEventBus.post(HelloBean("hi"))
        }
    }

    @Event
    fun stringFun(msg: String) {
        showToast("$msg ${this.javaClass.simpleName}")
    }

    @Event
    fun benFun(msg: HelloBean) {
        showToast("${msg.data} ${this.javaClass.simpleName}")
    }

    override fun onDestroy() {
        super.onDestroy()
        EasyEventBus.unregister(this)
        eventTest.unregister()
    }

}

class EasyEventBusTest {

    @Event
    fun stringFun(msg: String) {
        showToast("$msg ${this.javaClass.simpleName}")
    }

    @Event
    fun benFun(msg: HelloBean) {
        showToast("${msg.data} ${this.javaClass.simpleName}")
    }

    fun register() {
        EasyEventBus.register(this)
    }

    fun unregister() {
        EasyEventBus.unregister(this)
    }

}

data class HelloBean(val data: String)
```

使用起来和真正的 EvnetBus 差不多 😁😁，虽然实际上是要简陋很多的~~

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/d9ba6a949d284ba98fe74150a72f764a~tplv-k3u1fbpfcp-zoom-1.image)

最终自定义的 EasyEventBus 也只有五十行左右的代码量，仅向外部提供了三个方法用于进行**注册、反注册和发送消息**

```kotlin
/**
 * @Author: leavesCZY
 * @Desc:
 * @Github：https://github.com/leavesCZY
 */
object EasyEventBus {

    private val subscriptions = mutableSetOf<Any>()

    private const val PACKAGE_NAME = "github.leavesc.easyeventbus"

    private const val CLASS_NAME = "EventBusInject"

    private const val CLASS_PATH = "$PACKAGE_NAME.$CLASS_NAME"

    private val clazz = Class.forName(CLASS_PATH)

    //通过反射生成 EventBusInject 对象
    private val instance = clazz.newInstance()

    @Synchronized
    fun register(subscriber: Any) {
        subscriptions.add(subscriber)
    }

    @Synchronized
    fun unregister(subscriber: Any) {
        subscriptions.remove(subscriber)
    }

    @Synchronized
    fun post(event: Any) {
        subscriptions.forEach { subscriber ->
            val subscriberInfo =
                getSubscriberInfo(subscriber.javaClass)
            if (subscriberInfo != null) {
                val methodList = subscriberInfo.methodList
                methodList.forEach { method ->
                    if (method.eventType == event.javaClass) {
                        val declaredMethod = subscriber.javaClass.getDeclaredMethod(
                            method.methodName,
                            method.eventType
                        )
                        declaredMethod.invoke(subscriber, event)
                    }
                }
            }
        }
    }

    //通过反射调用 EventBusInject 的 getSubscriberInfo 方法
    private fun getSubscriberInfo(subscriberClass: Class<*>): SubscriberInfo? {
        val method = clazz.getMethod("getSubscriberInfo", Class::class.java)
        return method.invoke(instance, subscriberClass) as? SubscriberInfo
    }

}
```

# 一、怎么实现

这里先来想下这个自定义的 EasyEventBus 应该实现什么功能，以及怎么实现

EasyEventBus 的核心重点就在于其通过**注解处理器**生成辅助文件这个过程，这个过程使用者是感知不到的，这块逻辑也只会在编译阶段被触发到。我们希望在编译阶段就能够拿到所有声明了 `@Event` 的方法，免得在运行时才来反射，即在编译阶段就希望能够生成以下的辅助文件：

```java
/**
 * 这是自动生成的代码 by leavesCZY
 */
public class EventBusInject {

    private static final Map<Class<?>, SubscriberInfo> subscriberIndex = new HashMap<Class<?>, SubscriberInfo>();

    {
        List<EventMethodInfo> eventMethodInfoList = new ArrayList<EventMethodInfo>();
        eventMethodInfoList.add(new EventMethodInfo("stringFun", String.class));
        eventMethodInfoList.add(new EventMethodInfo("benFun", HelloBean.class));
        SubscriberInfo subscriberInfo = new SubscriberInfo(EasyEventBusMainActivity.class, eventMethodInfoList);
        putIndex(subscriberInfo);
    }

    {
        List<EventMethodInfo> eventMethodInfoList = new ArrayList<EventMethodInfo>();
        eventMethodInfoList.add(new EventMethodInfo("stringFun", String.class));
        eventMethodInfoList.add(new EventMethodInfo("benFun", HelloBean.class));
        SubscriberInfo subscriberInfo = new SubscriberInfo(EasyEventBusTest.class, eventMethodInfoList);
        putIndex(subscriberInfo);
    }

    private static final void putIndex(SubscriberInfo info) {
        subscriberIndex.put(info.getSubscriberClass(), info);
    }

    public final SubscriberInfo getSubscriberInfo(Class<?> subscriberClass) {
        return subscriberIndex.get(subscriberClass);
    }
}
```

可以看到，`subscriberIndex`中存储了所有监听方法的签名信息，在应用运行时我们我们只需要通过 `getSubscriberInfo` 方法就可以拿到 `subscriberClass` 的所有监听方法

最后，还需要向外提供一个 API 调用入口，即上面贴出来的自定义的 EasyEventBus 这个自定义类，是提供给使用者运行时调用的，在有消息需要发送的时候通过外部传入的 `subscriberClass` 从 EventBusInject 取出所有监听方法进行反射回调

所以，EasyEventBus 逻辑上会拆分为两个 moudle：

- easyeventbus_api。向外暴露 EasyEventBus 和 @Event
- easyeventbus_processor。不对外暴露，只在编译阶段生效

# 二、注解处理器

首先，我们需要提供一个注解对监听方法进行标记

```kotlin
@MustBeDocumented
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class Event
```

然后，我们在编译阶段需要预先把所有监听方法抽象保存起来，所以就需要定义两个 JavaBean 来作为承载体

```kotlin
/**
 * @Author: leavesCZY
 * @Desc:
 * @Github：https://github.com/leavesCZY
 */
data class EventMethodInfo(val methodName: String, val eventType: Class<*>)

data class SubscriberInfo(
    val subscriberClass: Class<*>,
    val methodList: List<EventMethodInfo>
)
```

然后声明一个 EasyEventBusProcessor 类继承于 AbstractProcessor，由编译器在编译阶段传入我们关心的代码元素

```kotlin
/**
 * @Author: leavesCZY
 * @Desc:
 * @Github：https://github.com/leavesCZY
 */
class EasyEventBusProcessor : AbstractProcessor() {

    companion object {

        private const val PACKAGE_NAME = "github.leavesc.easyeventbus"

        private const val CLASS_NAME = "EventBusInject"

        private const val DOC = "这是自动生成的代码 by leavesC"

    }

    private lateinit var elementUtils: Elements

    private val methodsByClass = LinkedHashMap<TypeElement, MutableList<ExecutableElement>>()

    override fun init(processingEnvironment: ProcessingEnvironment) {
        super.init(processingEnvironment)
        elementUtils = processingEnv.elementUtils
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        //只需要处理 Event 注解
        return mutableSetOf(Event::class.java.canonicalName)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.RELEASE_8
    }

    ···
   
}
```

通过 `collectSubscribers`方法拿到所有的监听方法，保存到 `methodsByClass`中，同时需要对方法签名进行校验：**只能是实例方法，且必须是 public 的，最多且至少包含一个入参参数**

```kotlin
override fun process(
    set: Set<TypeElement>,
    roundEnvironment: RoundEnvironment
): Boolean {
    val messager = processingEnv.messager
    collectSubscribers(set, roundEnvironment, messager)
    if (methodsByClass.isEmpty()) {
        messager.printMessage(Diagnostic.Kind.WARNING, "No @Event annotations found")
    } else {

       ···

    }
    return true
}

private fun collectSubscribers(
    annotations: Set<TypeElement>,
    env: RoundEnvironment,
    messager: Messager
) {
    for (annotation in annotations) {
        val elements = env.getElementsAnnotatedWith(annotation)
        for (element in elements) {
            if (element is ExecutableElement) {
                if (checkHasNoErrors(element, messager)) {
                    val classElement = element.enclosingElement as TypeElement
                    var list = methodsByClass[classElement]
                    if (list == null) {
                        list = mutableListOf()
                        methodsByClass[classElement] = list
                    }
                    list.add(element)
                }
            } else {
                //@Event 只能用于修改方法
                messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "@Event is only valid for methods",
                    element
                )
            }
        }
    }
}

/**
 * 校验方法签名是否合法
 */
private fun checkHasNoErrors(element: ExecutableElement, messager: Messager): Boolean {
    //不能是静态方法
    if (element.modifiers.contains(Modifier.STATIC)) {
        messager.printMessage(Diagnostic.Kind.ERROR, "Event method must not be static", element)
        return false
    }
    //必须是 public 方法
    if (!element.modifiers.contains(Modifier.PUBLIC)) {
        messager.printMessage(Diagnostic.Kind.ERROR, "Event method must be public", element)
        return false
    }
    //方法最多且只能包含一个参数
    val parameters = element.parameters
    if (parameters.size != 1) {
        messager.printMessage(
            Diagnostic.Kind.ERROR,
            "Event method must have exactly 1 parameter",
            element
        )
        return false
    }
    return true
}
```

然后，再来生成 `subscriberIndex` 这个静态常量，以及对应的静态方法块、`putIndex` 方法

```kotlin
//生成 subscriberIndex 这个静态常量
private fun generateSubscriberField(): FieldSpec {
    val subscriberIndex = ParameterizedTypeName.get(
        ClassName.get(Map::class.java),
        getClassAny(),
        ClassName.get(SubscriberInfo::class.java)
    )
    return FieldSpec.builder(subscriberIndex, "subscriberIndex")
        .addModifiers(
            Modifier.PRIVATE,
            Modifier.STATIC,
            Modifier.FINAL
        )
        .initializer(
            "new ${"$"}T<Class<?>, ${"$"}T>()",
            HashMap::class.java,
            SubscriberInfo::class.java
        )
        .build()
}

//生成静态方法块
private fun generateInitializerBlock(builder: TypeSpec.Builder) {
    for (item in methodsByClass) {
        val methods = item.value
        if (methods.isEmpty()) {
            break
        }
        val codeBuilder = CodeBlock.builder()
        codeBuilder.add(
            "${"$"}T<${"$"}T> eventMethodInfoList = new ${"$"}T<${"$"}T>();",
            List::class.java,
            EventMethodInfo::class.java,
            ArrayList::class.java,
            EventMethodInfo::class.java
        )
        methods.forEach {
            val methodName = it.simpleName.toString()
            val eventType = it.parameters[0].asType()
            codeBuilder.add(
                "eventMethodInfoList.add(new EventMethodInfo(${"$"}S, ${"$"}T.class));",
                methodName,
                eventType
            )
        }
        codeBuilder.add(
            "SubscriberInfo subscriberInfo = new SubscriberInfo(${"$"}T.class, eventMethodInfoList); putIndex(subscriberInfo);",
            item.key.asType()
        )
        builder.addInitializerBlock(
            codeBuilder.build()
        )
    }
}

//生成 putIndex 方法
private fun generateMethodPutIndex(): MethodSpec {
    return MethodSpec.methodBuilder("putIndex")
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
        .returns(Void.TYPE)
        .addParameter(SubscriberInfo::class.java, "info")
        .addCode(
            CodeBlock.builder().add("subscriberIndex.put(info.getSubscriberClass() , info);")
                .build()
        )
        .build()
}
```

然后，再来生成 `getSubscriberInfo` 这个公开方法，用于运行时调用

```kotlin
//生成 getSubscriberInfo 方法
private fun generateMethodGetSubscriberInfo(): MethodSpec {
    return MethodSpec.methodBuilder("getSubscriberInfo")
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .returns(SubscriberInfo::class.java)
        .addParameter(getClassAny(), "subscriberClass")
        .addCode(
            CodeBlock.builder().add("return subscriberIndex.get(subscriberClass);")
                .build()
        )
        .build()
}
```

完成以上方法的定义后，就可以在 `process` 方法中完成 EventBusInject 整个类文件的构建了

```kotlin
override fun process(
    set: Set<TypeElement>,
    roundEnvironment: RoundEnvironment
): Boolean {
    val messager = processingEnv.messager
    collectSubscribers(set, roundEnvironment, messager)
    if (methodsByClass.isEmpty()) {
        messager.printMessage(Diagnostic.Kind.WARNING, "No @Event annotations found")
    } else {
        val typeSpec = TypeSpec.classBuilder(CLASS_NAME)
            .addModifiers(Modifier.PUBLIC)
            .addJavadoc(DOC)
            .addField(generateSubscriberField())
            .addMethod(generateMethodPutIndex())
            .addMethod(generateMethodGetSubscriberInfo())
        generateInitializerBlock(typeSpec)
        val javaFile = JavaFile.builder(PACKAGE_NAME, typeSpec.build())
            .build()
        try {
            javaFile.writeTo(processingEnv.filer)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
    return true
}
```

# 三、EasyEventBus

EasyEventBus 的逻辑就很简单了，主要是通过反射来生成 EventBusInject 对象，拿到 `subscriber` 关联的 SubscriberInfo，然后在有消息被 Post 出来的时候进行遍历调用即可

```kotlin
/**
 * @Author: leavesCZY
 * @Desc:
 * @Github：https://github.com/leavesCZY
 */
object EasyEventBus {

    private val subscriptions = mutableSetOf<Any>()

    private const val PACKAGE_NAME = "github.leavesc.easyeventbus"

    private const val CLASS_NAME = "EventBusInject"

    private const val CLASS_PATH = "$PACKAGE_NAME.$CLASS_NAME"

    private val clazz = Class.forName(CLASS_PATH)

    //通过反射生成 EventBusInject 对象
    private val instance = clazz.newInstance()

    @Synchronized
    fun register(subscriber: Any) {
        subscriptions.add(subscriber)
    }

    @Synchronized
    fun unregister(subscriber: Any) {
        subscriptions.remove(subscriber)
    }

    @Synchronized
    fun post(event: Any) {
        subscriptions.forEach { subscriber ->
            val subscriberInfo =
                getSubscriberInfo(subscriber.javaClass)
            if (subscriberInfo != null) {
                val methodList = subscriberInfo.methodList
                methodList.forEach { method ->
                    if (method.eventType == event.javaClass) {
                        val declaredMethod = subscriber.javaClass.getDeclaredMethod(
                            method.methodName,
                            method.eventType
                        )
                        declaredMethod.invoke(subscriber, event)
                    }
                }
            }
        }
    }

    //通过反射调用 EventBusInject 的 getSubscriberInfo 方法
    private fun getSubscriberInfo(subscriberClass: Class<*>): SubscriberInfo? {
        val method = clazz.getMethod("getSubscriberInfo", Class::class.java)
        return method.invoke(instance, subscriberClass) as? SubscriberInfo
    }

}
```

# 四、GitHub

文本实现的 EasyEventBus 挺简陋的😂😂因为我的想法也只是通过自己动手来加深对 EventBus 的理解而已，这里也提供上述代码的 GitHub 链接：[AndroidOpenSourceDemo](https://github.com/leavesCZY/AndroidOpenSourceDemo)