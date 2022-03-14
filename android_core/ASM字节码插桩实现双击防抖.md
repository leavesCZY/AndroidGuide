> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

最近打算通过字节码插桩的方式来实现一个应用**双击防抖**的功能。简单来说，我希望通过字节码插桩的方式来为整个项目中所有使用了 OnClickListener 的回调方法中都插入一段逻辑代码，该段逻辑代码会对前后两次点击的时间进行判断，如果判断到时间小于某个阈值的话就直接 return

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
        if (!ViewDoubleClickCheck.canClick(view)){
            return
        }
        //TODO
    }
})
```

一个大型项目经过长久迭代后，设置 OnClickListener 的方式往往会有很多种，可能是使用了多种扩展框架，也可能是同类代码有多种实现形式。此时就至少需要考虑以下几种场景：

- 通过代码直接为 View 设置了 OnClickListener
- 在 XML 中为 View 声明了 onClick 属性
- 对第三方框架的支持程度。例如，如果项目中使用了 ButterKnife 的话，要照顾声明了 @OnClick 注解的方法。如果使用了 BaseRecyclerViewAdapterHelper 的话，要照顾为每个 Adapter 设置的 onItemClickListener 或者是 onItemChildClickListener

实际上，以上几种场景最终都要通过第一种方式来实现，其它几种方式的区别只在于设置 OnClickListener 的代码并不直接显式声明在我们的项目中，只要能解决第一种场景，其它场景只需要改变 hook 的范围和判断条件即可

而通过代码直接为 View 设置 OnClickListener，在代码的实现方式上又可以根据是否使用了 lambda 表达式分为两种：

```kotlin
view.setOnClickListener(object : View.OnClickListener {
    override fun onClick(v: View) {
        onClickView()
    }
})

view.setOnClickListener {
    onClickView()
}
```

我在通过 ASM 实现双击防抖功能的过程中，最大的难点就在于处理 lambda 表达式了，通过常规的 **定位包名全路径和方法签名信息** 没法直接对其进行处理，当中涉及到了原生 Java 平台和 Android 平台两者对 Java 8 的实现区别，以及一点点关于 Android 编译流程的知识点

本文要介绍的就是我在实现字节码插桩过程中总结出来的一些知识点，希望对你有所帮助 🤣🤣

# Java 8 的 Lambda

先看一个关于非 lambda 形式的例子

开发者应该都知道一个知识点：如果想要获取某个接口或者是抽象类的实例的话，我们可以不显式声明其实现类，而是可以采取**匿名内部类**的方式来声明实例对象

```java
public class Demo {

    void test() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                System.out.println("leavesC");
            }
        };
        runnable.run();
    }

}
```

将 Java 文件编译为字节码

```java
javac Demo.java
```

最终会生成两个 class 文件：`Demo.class`、`Demo$1.class`

可以看到 `Demo$1`实现了 Runnable 接口，也即所谓的匿名内部类了。上述代码中声明的 Runnable 变量指向的就是该具体的实现类，从`Demo.class` 第十一行也可以看到最终 new 出来的对象指向的也是 `Demo$1.class`

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/91939d14f74747a1ac5c8bf8cbf96885~tplv-k3u1fbpfcp-zoom-1.image)

将上述代码转换为 lambda 形式，再来查看其字节码有何变化

```java
public class Demo {

    void test() {
        Runnable runnable = () -> System.out.println("leavesC");
        runnable.run();
    }

}
```

此时生成的就只会有一个`Demo.class`文件

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/98a14defb8564f0784f5b5b5603fcdcf~tplv-k3u1fbpfcp-zoom-1.image)

前后两份字节码文件主要的差异点在于：

- 非 lambda 方式下，声明的 Runnable 变量最终会指向一个具体的接口实现类，也即我们常说的匿名内部类，在字节码中也可以看到有明确的生成该匿名内部类对象的过程，对应 new、dup、aload_0、invokespecial 等指令。Runnable 所要执行的代码块是在 `Demo$1.class` 的 run 方法中
- lambda 方式下，声明 Runnable 变量这个操作对应的是 invokedynamic、astore_1 等指令。Runnable 所要执行的代码块是在自动生成的私有静态方法`lambda$test$0()` 中

从这可以推导出一个结论：**在编译阶段，lambda 表达式不会生成相应的实现类，lambda 语法的实现机制有别于以前的匿名内部类**

当中的重点就在于 invokedynamic 指令了，Java 目前一共包含五种字节码调用指令

| 指令            | 作用                                                         |
| :-------------- | :----------------------------------------------------------- |
| invokevirtual   | 调用实例方法                                                 |
| invokestatic    | 调用静态方法                                                 |
| invokeinterface | 调用接口方法                                                 |
| invokespecial   | 调用特殊实例方法，包括实例初始化方法、父类方法               |
| invokedynamic   | 由用户引导方法决定，运行时动态解析出调用点限定符所引用的方法 |

在编译期间生成的 class 文件中，前四种指令通过常量池（Constant Pool）已经固定了目标方法的符号信息，包括 **类和接口的全局限定名、字段的名称和描述符、方法的名称和描述符** 等，运行阶段就可以依靠该符号信息直接定位到具体的方法从而直接调用

而 invokedynamic 是在 Java 7 中新增的字节码调用指令，作为 Java 支持动态类型语言的改进之一，在 Java 8 开始应用，lambda 表达式底层就依靠该指令来实现。invokedynamic 指令在常量池中并没有包含其目标方法的具体符号信息，存储的是 BootstapMethod 信息，在运行时再来通过引导方法机制动态确定方法的所属者和类型

进一步查看 `Demo.class` 的详细字节码信息

```java
javap -verbose Demo.class
```

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/44a6fc859fe64e88a677edbb8a755c8e~tplv-k3u1fbpfcp-zoom-1.image)

在执行了第十七行的 `invokedynamic` 指令后，就会存储栈顶值，也即 this，然后将其作为参数来调用 Runnable 接口的 `run()`方法。这说明了通过 `invokedynamic` 指令能够获取到一个 Runnable 对象

```java
0: invokedynamic #2,  0              // InvokeDynamic #0:run:()Ljava/lang/Runnable;
5: astore_1
6: aload_1
7: invokeinterface #3,  1            // InterfaceMethod java/lang/Runnable.run:()V
12: return
```

`InvokeDynamic` 指向了第四十二行的 BootstapMethods 区域， 当中会通过 invokestatic 指令去调用 LambdaMetafactory 的静态方法 `metafactory()`，此时就会在内存中来生成关联的接口实现类了

此外，标明的入参参数有三个：

- ()V。原始方法泛型擦除后的方法签名信息，也即 run 方法。由于 run 方法不包含泛型，所以和第三个参数的签名信息一样
- invokestatic lambdademo/Demo.lambda$test$0:()V。invokestatic 表达的是调用静态方法的操作，后面跟着的是自动生成的私有静态方法`lambda$test$0()`的签名信息，当中就包含了 lambda 表达式原本所要执行的代码块
- ()V。原始方法泛型擦除前的方法签名信息，也即 run 方法

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a01349c360704724b90d03fcefcdca82~tplv-k3u1fbpfcp-zoom-1.image)

从 InnerClassLambdaMetafactory 也可以看到，这里使用到了 ASM，生成的实现类按照 `ClassName + $$Lambda$ + 整数索引` 的方式进行命名，该实现类就会去负责调用 `lambda$test$0()` 方法

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/d9e91c65e37a42c593c6684685f29a16~tplv-k3u1fbpfcp-zoom-1.image)

通过打印 runnable 对象的 class 信息可以验证出其隶属于其它类而非 DemoClass

```kotlin
public class Demo {

    void test() {
        Runnable runnable = () -> System.out.println("leavesC");
        System.out.println(runnable.getClass().getSimpleName());

        Runnable runnable2 = () -> System.out.println("leavesC");
        System.out.println(runnable2.getClass().getSimpleName());
    }

    public static void main(String[] args) {
        Demo demo = new Demo();
        demo.test();
        //Demo$$Lambda$1
        //Demo$$Lambda$2
    }

}
```

所以说，使用 lambda 表达式并不是不会生成对应的实现类，而是将生成时机改成了运行时，当第一次执行到 lambda 表达式时，JVM 就会在内存中动态生成对应的实现类，当后续再次执行到该 lambda 表达式时也可以直接复用该类

# Android 的 Lambda

知道原生 Java 平台是如何实现 lambda 表达式后，再来讲下 Android 平台是如何支持 lambda 表达式的，因为 Android 的 lambda 和 Java 的 lambda 并不等同

大多数开发者应该知道，Java-Bytecode（JVM 字节码）是不能直接运行在 Android 系统上的，需要转换成 Android-Bytecode（Dalvik / ART 字节码），而 Dalvik / ART 并不支持 invokedynamic 指令，导致目前低版本的 Android 系统对 Java 8 支持得并不彻底， 此外某些 API 我们也无法使用，例如 `LocalDateTime.now()`

Android Gradle 插件 3.0.0 及更高版本支持所有 Java 7 语言功能，以及部分 Java 8 语言功能（具体因平台版本而异）。使用 Android Gradle 插件 4.0.0 及更高版本构建应用时，可以使用多种 Java 8 语言 API，而无需为应用设置最低 API 级别

为了能够支持 Java 8，目前 Android Gradle 插件是通过在 D8/R8 将 class 文件编译成 dex 文件的过程中插入字节码转换的操作来实现的，这个转换过程称为 desugar，也即 **脱糖**

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/f903bf22428c47968c1e59de98a7fa2b~tplv-k3u1fbpfcp-zoom-1.image)

desugar 操作就用于将某些目前 Android 系统还不支持的语法糖还原为简单的**基础语法结构**。例如，lambda 表达式经过 desugar 之后就会被转换为具体的实现类，并将生成的实现类直接写入到 dex 文件中，从而保证了 lambda 表达式也能够在低版本系统上正常运行，从而也就不存在兼容性问题了

可以写一个简单的 Android 应用来进行验证，随便写一段 lambda 表达式，然后反编译其 dex 文件，就可以看到 lambda 表达式在编译后的 dex 文件中就已经是直接以实现类的形式存在的了

```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val runnable = Runnable { println("leavesC") }
        runnable.run()
    }

}
```

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/138032e9f74d436fa25823e0a6c73dea~tplv-k3u1fbpfcp-zoom-1.image)

# 解析 Lambda 指令

有了以上基础后，此时就知道了 desugar 会给字节码插桩带来什么影响了：由于 Transform 是在 desugar 之前执行的，所以此时还未生成各个 lambda 表达式的具体实现类，导致我们没法直接根据 `View.OnClickListener` 的签名信息来定位到所有实现类

因此，在以下代码中，第一种方式的匿名内部类可以正常 hook，第二种方式就不行了，此时就只能通过解析 invokedynamic 指令来进行辅助判断了

```kotlin
view.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {

    }
});

view.setOnClickListener(v -> {

});
```

对于第二种方式，通过 ASM 框架，在字节码层面上我们能够获取到的信息有：

- 该方式包含 lambda 表达式，也即包含了一条 invokedynamic 指令，对应 ASM 中的 InvokeDynamicInsnNode
- invokedynamic 指令中包含了要生成的接口实例的签名信息，即 invokedynamic 指令中标明了要生成的是 OnClickListener 对象，且包含一个 onClick 方法，所以此时就可以通过遍历项目全局的 InvokeDynamicInsnNode 的 name 和 desc 两个属性，来查找到和 OnClickListener lambda 表达式关联的 InvokeDynamicInsnNode
- invokedynamic 指令会指向 BootstapMethod 区域，而 BootstapMethod 中已经标明了三个入参参数，第二个参数是编译期间就自动生成的私有静态方法，当中就包含了 onClick 方法应该执行的代码块。这三个参数就对应 InvokeDynamicInsnNode 的 bsmArgs 属性，所以通过 bsmArgs 我们就能够知道 onClick 方法最终要调用的方法的签名信息，通过向该方法插入需要的逻辑就可以实现 hook 了

看下实际的编码实现

声明两个扩展方法，用于筛选 InvokeDynamicInsnNode 指令，此处对比的就是 InvokeDynamicInsnNode 的 name 和 desc 两个属性

```kotlin
fun MethodNode.findHookPointLambda(config: DoubleClickConfig): List<InvokeDynamicInsnNode> {
    val onClickListenerLambda = findLambda {
        val nodeName = it.name
        val nodeDesc = it.desc
        val find = config.hookPointList.find { point ->
            nodeName == point.methodName && nodeDesc.endsWith(point.interfaceSignSuffix)
        }
        return@findLambda find != null
    }
    return onClickListenerLambda
}

private fun MethodNode.findLambda(
    filter: (InvokeDynamicInsnNode) -> Boolean
): List<InvokeDynamicInsnNode> {
    val handleList = mutableListOf<InvokeDynamicInsnNode>()
    val instructions = instructions?.iterator() ?: return handleList
    while (instructions.hasNext()) {
        val nextInstruction = instructions.next()
        if (nextInstruction is InvokeDynamicInsnNode) {
            if (filter(nextInstruction)) {
                handleList.add(nextInstruction)
            }
        }
    }
    return handleList
}
```

DoubleClickConfig 代表的是当前进行 hook 的所有配置信息，HookPoint 用于筛选 invokedynamic 指令

```kotlin
class DoubleClickConfig(
    private val doubleCheckClass: String = "github.leavesc.asm.double_click.ViewDoubleClickCheck",
    val doubleCheckMethodName: String = "canClick",
    val doubleCheckMethodDescriptor: String = "(Landroid/view/View;)Z",
    val hookPointList: List<HookPoint> = extraHookPoints
) {

    val formatDoubleCheckClass: String
        get() = doubleCheckClass.replace(".", "/")

}

data class HookPoint(
    val interfaceName: String,
    val methodName: String,
    val methodSign: String,
) {

    val interfaceSignSuffix = "L$interfaceName;"

}

private val extraHookPoints = listOf(
    HookPoint(
        interfaceName = "android/view/View\$OnClickListener",
        methodName = "onClick",
        methodSign = "onClick(Landroid/view/View;)V"
    )
)
```

每当拿到到一份字节码，就遍历其所有方法，判断方法内部是否包含和 OnClickListener 相关的 lambda 表达式，有的话则其指向的静态方法的签名信息保存起来。当拿到所有需要 hook 的方法后，再来向其插入 ViewDoubleClickCheck 防抖指令

```kotlin
class DoubleClickTransform(private val config: DoubleClickConfig) : BaseTransform() {

    override fun modifyClass(byteArray: ByteArray): ByteArray {
        val classReader = ClassReader(byteArray)
        val classNode = ClassNode()
        classReader.accept(classNode, ClassReader.EXPAND_FRAMES)
        val methods = classNode.methods
        if (!methods.isNullOrEmpty()) {
            val shouldHookMethodList = mutableSetOf<String>()
            for (methodNode in methods) {
                //判断方法内部是否有需要处理的 lambda 表达式
                val invokeDynamicInsnNodes = methodNode.findHookPointLambda(config)
                invokeDynamicInsnNodes.forEach {
                    val handle = it.bsmArgs[1] as? Handle
                    if (handle != null) {
                        shouldHookMethodList.add(handle.name + handle.desc)
                    }
                }
            }
            if (shouldHookMethodList.isNotEmpty()) {
                for (methodNode in methods) {
                    val methodNameWithDesc = methodNode.nameWithDesc
                    if (shouldHookMethodList.contains(methodNameWithDesc)) {
                        val argumentTypes = Type.getArgumentTypes(methodNode.desc)
                        val viewArgumentIndex = argumentTypes?.indexOfFirst {
                            it.descriptor == ViewDescriptor
                        } ?: -1
                        if (viewArgumentIndex >= 0) {
                            val instructions = methodNode.instructions
                            if (instructions != null && instructions.size() > 0) {
                                //插入 ViewDoubleClickCheck 防抖指令
                                val list = InsnList()
                                list.add(
                                    VarInsnNode(
                                        Opcodes.ALOAD, getVisitPosition(
                                            argumentTypes,
                                            viewArgumentIndex,
                                            methodNode.isStatic
                                        )
                                    )
                                )
                                list.add(
                                    MethodInsnNode(
                                        Opcodes.INVOKESTATIC,
                                        config.formatDoubleCheckClass,
                                        config.doubleCheckMethodName,
                                        config.doubleCheckMethodDescriptor
                                    )
                                )
                                val labelNode = LabelNode()
                                list.add(JumpInsnNode(Opcodes.IFNE, labelNode))
                                list.add(InsnNode(Opcodes.RETURN))
                                list.add(labelNode)
                                instructions.insert(list)
                            }
                        }
                    }
                }
                val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
                classNode.accept(classWriter)
                return classWriter.toByteArray()
            }
        }
        return byteArray
    }
    
}
```

# 匿名内部类

如果不使用 lambda 表达式的话，OnClickListener 在编译阶段就已经生成具体的接口实现类了，所以当判断到当前遍历的 Class 对象实现了 OnClickListener 接口的话，就拿到其 onClick 方法即可

`isHookPoint` 方法就用于判断 ClassNode 实现的所有接口中是否包含 OnClickListener，以及当前 MethodNode 的签名信息是否等于 `"onClick(Landroid/view/View;)V"`，都满足的话该 MethodNode 就是目标方法

```kotlin
class DoubleClickTransform(private val config: DoubleClickConfig) : BaseTransform() {

    override fun modifyClass(byteArray: ByteArray): ByteArray {
        val classReader = ClassReader(byteArray)
        val classNode = ClassNode()
        classReader.accept(classNode, ClassReader.EXPAND_FRAMES)
        val methods = classNode.methods
        if (!methods.isNullOrEmpty()) {
            val shouldHookMethodList = mutableSetOf<String>()
            for (methodNode in methods) {
                val methodNameWithDesc = methodNode.nameWithDesc
                //判断当前 methodNode 是否符合 hook 要求
                if (classNode.isHookPoint(config, methodNode)) {
                    shouldHookMethodList.add(methodNameWithDesc)
                    continue
                }
                //判断方法内部是否有需要处理的 lambda 表达式
                ···
            }
            if (shouldHookMethodList.isNotEmpty()) {
                //插入 ViewDoubleClickCheck 防抖指令
                ···
                val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
                classNode.accept(classWriter)
                return classWriter.toByteArray()
            }
        }
        return byteArray
    }
    
}

fun ClassNode.isHookPoint(config: DoubleClickConfig, methodNode: MethodNode): Boolean {
    val myInterfaces = interfaces
    if (myInterfaces.isNullOrEmpty()) {
        return false
    }
    val extraHookMethodList = config.hookPointList
    extraHookMethodList.forEach {
        if (myInterfaces.contains(it.interfaceName) && methodNode.nameWithDesc == it.methodSign) {
            return true
        }
    }
    return false
}
```

# XML onClick

在 XML 中声明的 onClick 属性会在 View 类解析 AttributeSet 时进行读取，View 类会为其设置一个自定义的 OnClickListener，通过反射 handlerName 方法的形式来实现回调

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/2a68cc44dc8b4f1c8fe124c9bcd0e9de~tplv-k3u1fbpfcp-zoom-1.image)

由于 Transform 无法作用于系统源码，所以我们没法对 DeclaredOnClickListener 进行 hook，有其它两种解决思路

第一种方案是为 XML 指向的 onClick 方法添加一个自定义注解，通过该注解来标明此方法需要进行 hook，也是本文采用的方法，采用此方案后使用场景就不只是局限于 XML onClick 了，也可以作用于任何符合签名信息的方法

首先是声明一个自定义注解，为需要进行 hook 的方法添加该注解

```kotlin
package github.leavesc.asm.double_click

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class CheckViewOnClick
```

之后需要先校验 MethodNode 的方法签名是否符合要求，即要求 **有一个入参参数，参数类型为 View ，方法返回值类型为 void。** 符合要求且包含 CheckViewOnClick 注解的话即目标方法

```kotlin
private const val OnClickViewMethodDescriptor = "(Landroid/view/View;)V"

val MethodNode.onlyOneViewParameter: Boolean
    get() = desc == OnClickViewMethodDescriptor

private fun MethodNode.hasAnnotation(annotationDesc: String): Boolean {
    return visibleAnnotations?.find { it.desc == annotationDesc } != null
}

fun MethodNode.hasCheckViewAnnotation(config: DoubleClickConfig): Boolean {
    return hasAnnotation(config.formatCheckViewOnClickAnnotation)
}


val methodNameWithDesc = methodNode.nameWithDesc
if (methodNode.onlyOneViewParameter) {
    if (methodNode.hasCheckViewAnnotation(config)) {
        //添加了 CheckViewOnClick 注解的情况
        shouldHookMethodList.add(methodNameWithDesc)
        continue
    }
}
```

> 相对应的，在某些情况下我们也不想对特定 OnClickListener 进行 hook，例如在业务上需要多次快速点击的时候，此时一样可以定义一个特定注解来作为白名单，对应本文源码中的 UncheckViewOnClick，详情可以参考文末给出的源码链接

第二种方案是对 AppCompat 包中的 AppCompatViewInflater 类进行 hook。目前大多数情况下我们使用的 Activity 都会继承于 AppCompatActivity， 而 AppCompatActivity 会通过 AppCompatViewInflater 来解析生成 View 对象，内部会尝试代理 onClick 属性并为 View 设置一个自定义的 OnClickListener。由于 AppCompatViewInflater 属于依赖库，Transform 可以通过扩大作用范围从而扫描到 AppCompatViewInflater 类，所以我们对 `checkOnClickListener` 方法或者是 DeclaredOnClickListener 类进行 hook 都可以

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/dd0e1987d6d4414d8cbd986bf129121f~tplv-k3u1fbpfcp-zoom-1.image)

# ButterKnife

ButterKnife 会为每个使用了 @OnClick 注解的类自动生成一个辅助文件，就像以下的 MainActivity_ViewBinding，而 ButterKnife 为每个 View 设置的 OnClickListener 都是框架内部自定义的实现类 DebouncingOnClickListener

```java
public final class MainActivity_ViewBinding implements Unbinder {
    
    ···

    @UiThread
    public MainActivity_ViewBinding(final MainActivity target, View source) {
        this.target = target;
        ···
        view.setOnClickListener(new DebouncingOnClickListener() {
            @Override
            public void doClick(View p0) {
                target.onClickViewByButterKnife(p0);
            }
        });
    }
  
}

public abstract class DebouncingOnClickListener implements View.OnClickListener {
  private static final Runnable ENABLE_AGAIN = () -> enabled = true;
  private static final Handler MAIN = new Handler(Looper.getMainLooper());

  static boolean enabled = true;

  @Override public final void onClick(View v) {
    if (enabled) {
      enabled = false;
      MAIN.post(ENABLE_AGAIN);
      doClick(v);
    }
  }

  public abstract void doClick(View v);
    
}
```

对 ButterKnife 进行 hook 的方案也有多种：

- 直接根据 @OnClick 注解来进行定位，只要解析到包含该注解的方法就对其进行 hook
- 根据 DebouncingOnClickListener 的匿名内部类来进行定位。ButterKnife 在为 View 设置 DebouncingOnClickListener 时没有使用 lambda 表达式，所以只要解析到其实现类然后对其 doClick 方法进行 hook 即可
- 将 Transform 的作用域扩大到所有依赖库。前两种方式仅需要扫描项目自有代码和通过 APT 生成的代码即可，而如果将 Transform 范围扩大到所有依赖库的话，Transform 阶段就可以看到 DebouncingOnClickListener 类了，此时就相当于在处理匿名内部类的情况

我采取的是第一种方案，最简单而且影响范围也最小

```kotlin
val methodNameWithDesc = methodNode.nameWithDesc
if (methodNode.onlyOneViewParameter) {
    if (methodNode.hasCheckViewAnnotation(config)) {
        //添加了 CheckViewOnClick 注解的情况
        shouldHookMethodList.add(methodNameWithDesc)
        continue
    } else if (methodNode.hasButterKnifeOnClickAnnotation()) {
        //使用了 ButterKnife，且当前 method 添加了 OnClick 注解
        shouldHookMethodList.add(methodNameWithDesc)
        continue
    }
}
```

# BaseRecyclerViewAdapterHelper

BaseRecyclerViewAdapterHelper 是一个封装了 RecyclerViewAdapter 常用操作的的三方库，可以很方便地来设置 onItemClickListener 和 onItemChildClickListener，按道理来说一个应用的双击防抖功能除了要作用于单个 View 外，也要对 RecyclerView 的每个 Item 进行处理才对

onItemClickListener 和 onItemChildClickListener 方法内部也一样是通过为 View 设置 OnClickListener 来实现的，此时一样可以通过扩大 Transform 的作用范围来处理，但为了减小影响范围，我 hook 的是这两个 onItem 方法

重点在于此时也需要考虑使用了 lambda 表达式的情况，也即需要同时处理以下两类代码：

```kotlin
adapter.onItemClickListener = BaseQuickAdapter.OnItemClickListener { adapter, view, position ->
    onClickView()
}

adapter.onItemClickListener = object :BaseQuickAdapter.OnItemClickListener{
    override fun onItemClick(adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int) {

    }
}
```

两个 onItem 方法的总体处理流程和 OnClickListener 一样，根据是否使用了 lambda 表达式走不同的判断逻辑，所以此时只要为 extraHookPoints 多添加两个需要 hook 的节点即可，具体的签名信息就需要查看该开源库的源码来得到了

```kotlin
private val extraHookPoints = listOf(
    HookPoint(
        interfaceName = "android/view/View\$OnClickListener",
        methodName = "onClick",
        methodSign = "onClick(Landroid/view/View;)V"
    ),
    HookPoint(
        interfaceName = "com/chad/library/adapter/base/BaseQuickAdapter\$OnItemClickListener",
        methodName = "onItemClick",
        methodSign = "onItemClick(Lcom/chad/library/adapter/base/BaseQuickAdapter;Landroid/view/View;I)V"
    ),
    HookPoint(
        interfaceName = "com/chad/library/adapter/base/BaseQuickAdapter\$OnItemChildClickListener",
        methodName = "onItemChildClick",
        methodSign = "onItemChildClick(Lcom/chad/library/adapter/base/BaseQuickAdapter;Landroid/view/View;I)V",
    )
)
```

# 结尾

最后也给出完整的源码：[ASM_Transform](https://github.com/leavesCZY/ASM_Transform)

这应该会是一个比较好的让读者入门字节码插桩的案例，读者根据项目实际情况进行简单修改后也可以把该双击防抖功能引入到自己项目中