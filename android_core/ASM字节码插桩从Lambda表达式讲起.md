> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

字节码插桩是如今 Android 开发中非常普遍的一种技术手段，其应用范围非常广泛，涉及各种业务强关联或者和业务无关的领域，例如：无痕埋点、隐私合规检测、耗时方法统计、性能检测、双击防抖等

我之前就写过几篇文章，介绍了几种通过 ASM 实现字节码插桩的案例

- [ASM 字节码插桩：实现双击防抖](https://juejin.cn/post/7042328862872567838)
- [ASM 字节码插桩：进行线程整治](https://juejin.cn/post/7044339202997092383)
- [ASM 字节码插桩：助力隐私合规](https://juejin.cn/post/7046207125785149448)
- [ASM 字节码插桩：监控大图加载](https://juejin.cn/post/7074970389188706318)

本篇文章再来详细介绍下在实现字节码插桩的过程中，我们难免会遇到的一个难点，也即从 Java 8 开始支持的一个新语法：Lambda 表达式，再以此扩展介绍向 Lambda 表达式进行字节码插桩的大致思路

# 匿名内部类

我们知道，如果想要声明某个接口或者抽象类的实例的话，我们可以不显式声明实现类，而是可以直接采取 **匿名内部类** 的方式来得到其实例对象

看一个小例子

```java
public class Lambda {

    private void lambda() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                System.out.println("Hello World!");
            }
        };
    }

}
```

将 `Lambda.java`文件编译为字节码

```java
javac Lambda.java
```

最终会生成两个 class 文件：`Lambda$1.class`、`Lambda.class`

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a6bc74a94ad941f4ba7aa223102f0a36~tplv-k3u1fbpfcp-zoom-1.image)

`Lambda$1.class` 可以很明确地就看出其实现了 Runnable 接口，是编译器自动生成的实现类。从 `Lambda.class` 文件也可以明确看出，`lambda` 方法中 new 的对象指向的也是 `Lambda$1`。所以说，对于代码中的匿名内部类，编译器会自动为其生成一个实现类，包含了其原有的内部逻辑：`System._out_.println("Hello World!")`，并将原有的匿名内部类自动替换为该具体的实现类

# Lambda 表达式

由于 Runnable 接口属于函数式接口，因此我们可以将上述代码转化为 Lambda 表达式，再来看其字节码会有什么变化

```java
public class Lambda {

    private void lambda() {
        Runnable runnable = () -> System.out.println("Hello World!");
    }

}
```

最终只会生成一个 class 文件：`Lambda.class`

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/54d1898879c64aacbfa23f9e8df0dd70~tplv-k3u1fbpfcp-zoom-1.image)

前后两份字节码文件主要的差异点在于：

- 匿名内部类声明的 Runnable 变量，最终会指向一个具体的接口实现类，在字节码中也可以看到有明确的声明类实例并调用其构造方法的过程，对应 `new、dup、aload_0、invokespecial` 等指令。该实现类的 `run` 方法中包含了所要执行的代码块
- Lambda 语法声明的 Runnable 变量，这个操作对应的是 `invokedynamic、astore_1` 等指令。Runnable 所要执行的代码块是在自动生成的静态方法 `lambda$lambda$0()` 中

从这可以推导出一个结论：**在编译阶段，Lambda 表达式并不会生成相应的实现类，Lambda 语法的实现机制有别于匿名内部类**

当中的重点就在于 invokedynamic 指令了，Java 目前一共包含五种字节码调用指令

| 指令            | 作用                                                         |
| --------------- | ------------------------------------------------------------ |
| invokevirtual   | 调用实例方法                                                 |
| invokestatic    | 调用静态方法                                                 |
| invokeinterface | 调用接口方法                                                 |
| invokespecial   | 调用特殊实例方法，包括实例初始化方法、父类方法               |
| invokedynamic   | 由用户引导方法决定，运行时动态解析出调用点限定符所引用的方法 |

在编译期间生成的 class 文件中，前四种指令通过常量池（Constant Pool）已经固定了目标方法的符号信息，包括 **类和接口的全局限定名、字段的名称和描述符、方法的名称和描述符** 等，运行阶段就可以依靠该符号信息直接定位到具体的方法从而直接调用

而 invokedynamic 是在 Java 7 中新增的字节码调用指令，作为 Java 支持动态类型语言的改进之一，在 Java 8 开始应用，Lambda 表达式底层就依靠该指令来实现。invokedynamic 指令在常量池中并没有包含其目标方法的具体符号信息，存储的是 BootstapMethod 信息，在运行时再来通过引导方法机制动态确定方法的所属者和类型

而不管怎么说，在编译过后，Lambda 表达式所对应的 **对象类型、要调用的方法的签名信息、要执行的代码块** 等信息依然是要被保存在字节码中的。进一步查看 `Lambda.class` 的详细字节码信息，看这些信息是存储在哪里

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/c0f9795ebce8462c9865ecd9f95d329f~tplv-k3u1fbpfcp-zoom-1.image)

可以看到，第十八行的 invokedynamic 指令就包含了 Runnable 接口和 run 方法完整的签名信息：`run:()Ljava/lang/Runnable`，同时指向了第四十一行的 BootstapMethods 区域， 当中会通过 invokestatic 指令去调用 LambdaMetafactory 的静态方法 `metafactory()`，通过该方法在内存中来生成关联的接口实现类

同时，Method arguments 所列出的参数有三个：

- 原始方法泛型擦除后的方法签名信息，也即 run 方法
- Lambda 表达式原本所要执行的代码块，也即自动生成的静态方法 `lambda$lambda$0()` 的签名信息，当中包含了 Lambda 表达式原本所要执行的代码块
- 原始方法泛型擦除前的方法签名信息，也即 run 方法。由于 run 方法不包含泛型，所以和第一个参数的签名信息一样

可以通过反射调用 `lambda$lambda$0()`方法来验证该方法是否真的存在，运行以下代码就会发现 Hello World! 打印了两次

```java
public class Lambda {

    private void lambda() {
        Runnable runnable = () -> System.out.println("Hello World!");
        runnable.run();
    }

    public static void main(String[] args) {
        Lambda lambda = new Lambda();
        lambda.lambda();
        try {
            Lambda.class.getDeclaredMethod("lambda$lambda$0").invoke(null);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

}
```

所以说，Lambda 表达式所对应的 **对象类型（Runnable 接口）、要调用的方法的签名信息（run 方法）、要执行的代码块（lambda$lambda$0 方法）** 等信息都是有被保存下来的，这样在运行时才能构成一个完整的调用链

# Lambda 的状态

假如一个 Lambda 表达式没有 "捕获" 任何外部实例对象，该表达式无需依赖于外部实例对象就可以单独运行，那么该 Lambda 表达式可以称为是 “无状态的"；假如使用到了外部实例对象，那么该 Lambda 表达式就是 “有状态的”

例如，在上述例子中，Runnable Lambda 表达式最终会对应一个自动生成的静态方法 `lambda$lambda$0()`，用于存储所要执行的代码逻辑。因为该表达式并没有使用到任何实例对象，所以可以以静态方法的形式存在。而 “有状态的” Lambda 表达式对应的方法将是实例方法

例如，以下四个 Lambda 表达式，因为只有 runnable1 使用到了实例变量，因此也只有它会生成一个实例方法。此外，runnable3 捕获的是局部变量，该变量和具体实例无关，因此也将对应一个静态方法，并将捕获的局部变量作为参数传入

```java
public class Lambda {

    private static String log1 = "Hello World!";

    private String log2 = "Hello World!";

    private void lambda() {
        Runnable runnable0 = () -> System.out.println(log1);
        Runnable runnable1 = () -> System.out.println(log2);
        Runnable runnable2 = () -> System.out.println("Hello World!");
        String log = "Hello World!";
        Runnable runnable3 = () -> System.out.println(log);
    }

}
```

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/e97859df45cb47b9afec0d3750470837~tplv-k3u1fbpfcp-zoom-1.image)

# Android Lambda

知道标准 Java 平台是如何处理 Lambda 表达式后，再来讲下 Android 平台是如何支持 Lambda 表达式的，因为 Android 的 Lambda 和 Java 的 Lambda 并不等同

虽然每一个 Android 应用进程都对应一个 Java 虚拟机，但 Android 虚拟机并不完全遵循 Java 虚拟机标准， Java-Bytecode（JVM 字节码）是不能直接运行在 Android 系统上的，需要转换成 Android-Bytecode（Dalvik / ART 字节码），而 Dalvik / ART 并不支持 invokedynamic 指令，导致目前 Android 系统对 Java 8 以及更高版本的 JDK 支持得并不彻底。某些 Java API 也只有高版本系统才可以使用，例如，`LocalDateTime.now()` 至少要 Android 8.0 的系统才可以使用

为了能够支持 Java 8，目前 AGP 是通过在 D8/R8 将 class 文件编译成 dex 文件的过程中，对字节码进行转换来实现的，这个转换过程称为 desugar，也即 **脱糖**

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/0c797e40ed1b495eb699cece97cb81c7~tplv-k3u1fbpfcp-zoom-1.image)

desugar 操作就用于将某些 Android 系统目前还不支持的语法糖还原为简单的 **基础语法结构** 。例如，上述的 Runnable Lambda 表达式经过 desugar 之后，就会被转换为具体的实现类，并将生成的实现类直接写入到 dex 文件中，就如同普通的匿名内部类一样，因此也就不存在兼容性问题了，从而保证了 Lambda 表达式也能够在 Android 低版本系统上正常运行

# 字节码插桩

由于 Android APK 编译流程中 Transform 和 desugar 两个操作的先后顺序问题，就给我们的字节码插桩带来了一点点困扰

举个例子。我曾经通过字节码插桩的方式为项目实现了一个全局的 **双击防抖** 功能。简单来说，我通过字节码插桩的方式来为整个项目中所有使用了 OnClickListener 的回调方法中都插入了一段逻辑代码，该段逻辑代码会对前后两次点击的时间进行判断，如果判断到时间小于某个阈值的话就直接 return

就像以下代码所示

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

Kotlin 中的 `object : View.OnClickListener` 就相当于 Java 中的匿名内部类，在编译阶段就会直接生成具体的实现类，因此可以很直接地通过 `View.OnClickListener` 接口和 `onClick` 方法两者的签信息名定位到需要插入代码的位置，难度不大

比较麻烦的是 Lambda 表达式

```kotlin
view.setOnClickListener {
    //TODO
}
```

由于 AGP 的 Transform 流程是在 desugar 之前执行的，Transform 时还未生成各个 Lambda 表达式的具体实现类，所以此时的 Lambda 表达式还对应着 invokedynamic 指令，我们无法直接 “看到” Lambda 表达式对应的代码块，因此也不能简单地通过签名信息就定位到目标方法

想要解析 Lambda 表达式，就还是要依靠上文介绍的 BootstapMethods，通过 BootstapMethods 来找到出目标方法

对于上述 Lambda 表达式，通过 ASM 框架，在字节码层面上我们能够获取到的信息有：

- 该表达式包含一条 invokedynamic 指令，对应 ASM 中的 InvokeDynamicInsnNode
- invokedynamic 指令中包含了要生成的接口实例的签名信息，即 invokedynamic 指令中标明了要生成的是 OnClickListener 对象，且包含一个 onClick 方法，所以此时就可以通过遍历项目全局的 InvokeDynamicInsnNode 的 name 和 desc 两个属性，来查找到和 OnClickListener Lambda 表达式关联的 InvokeDynamicInsnNode
- 上文已经讲到，invokedynamic 指令指向了字节码中的 BootstapMethod 区域，而 BootstapMethod 中已经标明了三个入参参数，第二个参数指向的是编译期间自动生成的方法，当中就包含了 onClick 方法应该执行的代码块。这三个参数就对应 InvokeDynamicInsnNode 的 bsmArgs 属性，所以通过 bsmArgs 我们就能够知道 onClick 方法最终要调用的方法的签名信息，通过向该方法插入需要的逻辑就可以实现插桩了

所以说，对于匿名内部类，我们的插桩思路是向 OnClickListener 接口的实现类的 onClick 方法插入代码；对于 Lambda 表达式，我们的插桩思路可以改为向其自动生成的方法插入代码，两者的最终效果都是一样的

对应具体代码

第一步。需要先遍历每一个 MethodNode 包含的所有指令 instructions，找出 name 和 desc 都符合的 InvokeDynamicInsnNode。此处之所以通过 endsWith 而非 equals 来筛选 desc，是因为 Lambda 有可能引用了外部实例，此时外部实例就会成为 OnClickListener 实现类的构造参数，那么 desc 就会变成类似于 `(Lgithub/leavesczy/asm/MainActivity;)Landroid/view/View$OnClickListener;` 这样的形式，所以需要通过 endsWith 来进行筛选

```kotlin
val dynamicInsnNodes = methodNode.filterLambda {
    val nodeName = it.name
    val nodeDesc = it.desc
    nodeName == "onClick" && nodeDesc.endsWith("Landroid/view/View\$OnClickListener;")
}

fun MethodNode.filterLambda(filter: (InvokeDynamicInsnNode) -> Boolean): List<InvokeDynamicInsnNode> {
    val mInstructions = instructions ?: return emptyList()
    val dynamicList = mutableListOf<InvokeDynamicInsnNode>()
    mInstructions.forEach { instruction ->
        if (instruction is InvokeDynamicInsnNode) {
            if (filter(instruction)) {
                dynamicList.add(instruction)
            }
        }
    }
    return dynamicList
}
```

第二步。找到需要插桩的 Lambda 表达式后，拿到 Method arguments 的第二个参数 `it.bsmArgs[1]`，该值就对应编译器要自动生成的方法，再通过该方法的签名信息 nameWithDesc 从 methods 中筛选出对应的 MethodNode，通过向该方法植入代码就可以实现插桩了

```kotlin
val shouldHookMethodList = mutableSetOf<MethodNode>()

dynamicInsnNodes.forEach {
    val handle = it.bsmArgs[1] as? Handle
    if (handle != null) {
        val nameWithDesc = handle.name + handle.desc
        val method = methods.find { it.nameWithDesc == nameWithDesc }!!
        shouldHookMethodList.add(method)
    }
}
```

第三步。此步骤就要来向目标方法植入代码了，但还有个问题需要先解决。由于 匿名内部类 和 Lambda 表达式 都有可能引用到了外部实例对象，因此经过 desugar 后，就会像以下代码所示，MainActivity 成为 OnClickListener 实现类的构造参数，该实现类再通过 MainActivity 对象来调用目标方法。这使得我们需要先知道 View 对象是作为当前方法的第几个参数，取到值后才能去调用 ViewDoubleClickCheck 进行防抖检查

```java
public final class MainActivity extends AppCompatActivity {

    /* access modifiers changed from: protected */
    @Override // androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, androidx.fragment.app.FragmentActivity
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((TextView) findViewById(R.id.tvViewDoubleClickCheck)).setOnClickListener(new MainActivity$$ExternalSyntheticLambda0(this));
    }

    /* access modifiers changed from: private */
    /* renamed from: onCreate$lambda-0  reason: not valid java name */
    public static final void m60onCreate$lambda0(MainActivity this$0, View view) {
        if (ViewDoubleClickCheck.canClick(view)) {
        	//TODO
        }
    }

}

public final /* synthetic */ class MainActivity$$ExternalSyntheticLambda0 implements View.OnClickListener {
    public final /* synthetic */ MainActivity f$0;

    public /* synthetic */ MainActivity$$ExternalSyntheticLambda0(MainActivity mainActivity) {
        this.f$0 = mainActivity;
    }

    public final void onClick(View view) {
        MainActivity.m60onCreate$lambda0(this.f$0, view);
    }
}
```

此外，点击事件不单单局限于 setOnClickListener 方法，RecyclerView 的每一个 item 的点击事件也需要进行防抖检查，这种情况也一样需要解析出 View 对象是作为当前方法的第几个参数

```kotlin
val clickDemoAdapter = ClickDemoAdapter()
clickDemoAdapter.setOnItemClickListener(object : OnItemClickListener {
    override fun onItemClick(adapter: BaseQuickAdapter<*, *>, view: View, position: Int) {
        if (ViewDoubleClickCheck.canClick(view)) {
        	//TODO
        }
    }
})
```

因此，我们需要先计算出 viewArgumentIndex，通过 viewArgumentIndex 执行 ALOAD 操作加载到 View 对象，之后才能去调用 ViewDoubleClickCheck

```kotlin
private fun hookMethod(modeNode: MethodNode) {
    val argumentTypes = Type.getArgumentTypes(modeNode.desc)
    //计算 View 对象是方法的第几个入参参数
    val viewArgumentIndex = argumentTypes?.indexOfFirst {
        it.descriptor == "Landroid/view/View;"
    } ?: -1
    if (viewArgumentIndex >= 0) {
        val instructions = modeNode.instructions
        if (instructions != null && instructions.size() > 0) {
            val list = InsnList()
            //引用 View 对象
            list.add(
                VarInsnNode(
                    Opcodes.ALOAD, getVisitPosition(
                        argumentTypes,
                        viewArgumentIndex,
                        modeNode.isStatic
                    )
                )
            )
            //去调用 ViewDoubleClickCheck 的 canClick 方法
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
```

# 结尾

本文相关的代码都上传到了 Github：[ASM_Transform](https://github.com/leavesCZY/ASM_Transform)

包含了几个完整的字节码插桩实践案例，读者可以一起参照