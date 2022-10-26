> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

Android 开发者们对 Jetpack Compose 应该已经很熟悉了吧？我在项目中也已经大规模应用了 Jetpack Compose，极大地解放了开发者的心智负担

最近我想要来为项目做一些能够提升 **应用性能** 或者是 **用户体验** 的优化项，想了想就敲定了一个目标：通过字节码插桩的方式，来为项目中所有和 Jetpack Compose 关联的业务实现全局的双击防抖功能

在之前，我已经为 Android 的 View 体系实现过相同的功能了：[ASM 字节码插桩：实现双击防抖](https://juejin.cn/post/7042328862872567838) ，想着在 Jetpack Compose 中应该也差不多，不会太麻烦，可在编码过程中才发现这一个功能并不好做，遇到了一些不太好解决的问题，后面来一一进行讲解

# 一、基本思路

在 Jetpack Compose 中，我们一般都是通过 Modifier  的 `clickable` 或者 `combinedClickable` 这两个扩展函数来为可组合函数的点击事件设置监听，方法均位于 `compose.foundation` 库的 Clickable 类中，一共有四个方法可供使用

`clickable` 和 `combinedClickable` 方法均包含了重载函数，差别只在于是否包含 `interactionSource` 和 `indication` 这两个入参参数，重载函数之间还是属于直接调用的关系，所以只需关注第二个和第四个方法即可

```kotlin
fun Modifier.clickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
) {
    Modifier.clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        onClick = onClick,
        role = role,
        indication = LocalIndication.current,
        interactionSource = remember { MutableInteractionSource() }
    )
}

fun Modifier.clickable(
    interactionSource: MutableInteractionSource,
    indication: Indication?,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
) {
    //TODO
}

fun Modifier.combinedClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    onClick: () -> Unit
){
    Modifier.combinedClickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        onLongClickLabel = onLongClickLabel,
        onLongClick = onLongClick,
        onDoubleClick = onDoubleClick,
        onClick = onClick,
        role = role,
        indication = LocalIndication.current,
        interactionSource = remember { MutableInteractionSource() }
    )
}

fun Modifier.combinedClickable(
    interactionSource: MutableInteractionSource,
    indication: Indication?,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    onClick: () -> Unit
){
    //TODO
}
```

为了实现双击防抖，我们需要限制 `onClick` 被重复执行时的最小时间间隔。此时最直接的思路，就是引入一个包装类 OnClickWrap，将 `onClick` 均改为 OnClickWrap，然后在 OnClickWrap 中实现防抖逻辑，选择性地执行 `onClick` 方法即可

```kotlin
class OnClickWrap(private val onClick: (() -> Unit)) : Function0<Unit> {

    companion object {

        private const val MIN_DURATION = 500L

        private var lastClickTime = 0L

    }

    override fun invoke() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime > MIN_DURATION) {
            lastClickTime = currentTime
            onClick()
            log("onClick isEnabled : true")
        } else {
            log("onClick isEnabled : false")
        }
    }

    private fun log(log: String) {
        Log.e(
            "OnClickWrap",
            "${System.identityHashCode(this)} ${System.identityHashCode(onClick)} $log"
        )
    }

}
```

也即是说，在插桩后，`clickable` 和 `combinedClickable` 这两个方法的伪代码应该如下所示

```kotlin
fun Modifier.clickable(
    interactionSource: MutableInteractionSource,
    indication: Indication?,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
) {
    onClick = OnClickWrap(onClick = onClick)
    //TODO
}

fun Modifier.combinedClickable(
    interactionSource: MutableInteractionSource,
    indication: Indication?,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    onClick: () -> Unit
){
    onClick = OnClickWrap(onClick = onClick)
    //TODO
}
```

# 二、反编译 Clickable 类

有了基本思路后，我们先通过 ClassVisitor 打印出 Clickable 类中所有方法的签名信息，这样在 Transform 阶段才能知道如何识别到目标方法

```kotlin
private class ComposeDoubleClickClassVisitor(
    private val nextClassVisitor: ClassVisitor,
    private val classData: ClassData
) : ClassNode(Opcodes.ASM5) {

    private companion object {

        private const val ComposeClickClassName = "androidx.compose.foundation.ClickableKt"

    }

    override fun visitEnd() {
        super.visitEnd()
        val className = classData.className
        if (className == ComposeClickClassName) {
            methods.forEach { methodNode ->
                val methodName = methodNode.name
                val methodDesc = methodNode.desc
                LogPrint.log("methodName: $methodName \n methodDesc: $methodDesc")
            }
        }
        accept(nextClassVisitor)
    }

}
```

除去一些无关方法后，令我意外的是，最终输出的方法签名信息居然有八个：四个 `clickable` 方法、四个 `combinedClickable` 方法

```kotlin
methodName: clickable-XHw0xAI 
methodDesc: (Landroidx/compose/ui/Modifier;ZLjava/lang/String;Landroidx/compose/ui/semantics/Role;Lkotlin/jvm/functions/Function0;)Landroidx/compose/ui/Modifier; 

methodName: clickable-XHw0xAI$default 
methodDesc: (Landroidx/compose/ui/Modifier;ZLjava/lang/String;Landroidx/compose/ui/semantics/Role;Lkotlin/jvm/functions/Function0;ILjava/lang/Object;)Landroidx/compose/ui/Modifier; 

methodName: clickable-O2vRcR0 
methodDesc: (Landroidx/compose/ui/Modifier;Landroidx/compose/foundation/interaction/MutableInteractionSource;Landroidx/compose/foundation/Indication;ZLjava/lang/String;Landroidx/compose/ui/semantics/Role;Lkotlin/jvm/functions/Function0;)Landroidx/compose/ui/Modifier; 

methodName: clickable-O2vRcR0$default 
methodDesc: (Landroidx/compose/ui/Modifier;Landroidx/compose/foundation/interaction/MutableInteractionSource;Landroidx/compose/foundation/Indication;ZLjava/lang/String;Landroidx/compose/ui/semantics/Role;Lkotlin/jvm/functions/Function0;ILjava/lang/Object;)Landroidx/compose/ui/Modifier; 

methodName: combinedClickable-cJG_KMw 
methodDesc: (Landroidx/compose/ui/Modifier;ZLjava/lang/String;Landroidx/compose/ui/semantics/Role;Ljava/lang/String;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;)Landroidx/compose/ui/Modifier; 

methodName: combinedClickable-cJG_KMw$default 
methodDesc: (Landroidx/compose/ui/Modifier;ZLjava/lang/String;Landroidx/compose/ui/semantics/Role;Ljava/lang/String;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;ILjava/lang/Object;)Landroidx/compose/ui/Modifier; 

methodName: combinedClickable-XVZzFYc 
methodDesc: (Landroidx/compose/ui/Modifier;Landroidx/compose/foundation/interaction/MutableInteractionSource;Landroidx/compose/foundation/Indication;ZLjava/lang/String;Landroidx/compose/ui/semantics/Role;Ljava/lang/String;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;)Landroidx/compose/ui/Modifier; 

methodName: combinedClickable-XVZzFYc$default 
methodDesc: (Landroidx/compose/ui/Modifier;Landroidx/compose/foundation/interaction/MutableInteractionSource;Landroidx/compose/foundation/Indication;ZLjava/lang/String;Landroidx/compose/ui/semantics/Role;Ljava/lang/String;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;ILjava/lang/Object;)Landroidx/compose/ui/Modifier; 
```

将开头的两个方法签名信息转换成易于理解的 Java 代码，方便读者理解

```kotlin
public static final Modifier clickable-XHw0xAI(Modifier modifier, boolean enabled, String onClickLabel, 
                                               Role role, Function0<Unit> onClick) {

}

public static Modifier clickable-XHw0xAI$default(Modifier modifier, boolean enabled, String onClickLabel, 
                                                 Role role, Function0 onClick, int flag, Object object) {

}
```

结合日志信息和伪代码，就可以很容易看出一些小细节了：

- 第一个方法对应的是 Clickable 类中仅包含四个参数的 `clickable` 方法。多出一个 Modifier 参数很正常，因为 Kotlin 的扩展函数本质上就相当于 Java 中的静态方法，扩展对象会成为该静态方法的第一个入参参数，大多数开发者应该都知道这一点
- 八个方法的命名方式明显是带有某种规律：一个 `methodName` 对应一个 `methodName$default`。例如：`clickable-XHw0xAI` 方法就对应着 `clickable-XHw0xAI$default` 方法
- 名字带有 `$default` 的方法，其入参列表的末尾都会多出两个参数：`ILjava/lang/Object;`，也即 Int 类型 和 Object 类型

打印出的日志信息明显和 Clickable 类差别很大，我以为是哪里出问题了，就再尝试着反编译打包进 Apk 中的 Clickable 类的源代码，最终代码总不会骗人

最后，我发现反编译出的源代码中也是有八个相同签名信息的方法，方法的命名规则和上述完全一致，而且多出来的 Int 类型参数在方法内部还进行了一系列 & 运算，似乎还起到了决定其它入参参数值的作用？

![](https://upload-images.jianshu.io/upload_images/2552605-e0a153fb68b6c364.png)

# 三、剖析原因

Clickable 类中只声明了四个相关方法，为何这里找到的又会是八个？多出来的 Int 类型和 Object 类型的入参参数的作用是什么？参数值又根据什么来决定的呢？

先说结论：**之所以会多出两个 Int 类型 和 Object 类型的入参参数，是由于扩展函数存在默认参数值导致的，扩展函数需要依靠该 Int 值来决定是否应该使用默认参数值**

这个结论听着很抽象，不太好理解，这里来举个小例子

为了方便理解，这里来模拟 `clickable` 方法的定义规则，声明 Modifier 和 Role 两个接口，以及相应的扩展函数

```kotlin
interface Modifier {

    companion object {

        val INSTANCE = object : Modifier {

        }

    }

}

interface Role

fun Modifier.clickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
) {

}

fun clickableDemo() {
    val modifier = Modifier.INSTANCE
    modifier.clickable(enabled = true, onClickLabel = "clickableDemo", role = null) {

    }
    modifier.clickable(enabled = true) {

    }
    modifier.clickable(onClickLabel = "clickableDemo") {

    }
}
```

将以上代码反编译为 Java 代码，格式化后大致如下所示

```java
public static final void clickable(@NotNull Modifier modifier,
                                   boolean enabled,
                                   @Nullable String onClickLabel,
                                   @Nullable Role role,
                                   @NotNull Function0 onClick) {
    Intrinsics.checkNotNullParameter(modifier, "modifier");
    Intrinsics.checkNotNullParameter(onClick, "onClick");
}

public static void clickable$default(Modifier modifier,
                                     boolean enabled,
                                     String onClickLabel,
                                     Role role,
                                     Function0 onClick,
                                     int flag,
                                     Object object) {
    if ((flag & 1) != 0) {
        enabled = true;
    }
    if ((flag & 2) != 0) {
        onClickLabel = (String) null;
    }
    if ((flag & 4) != 0) {
        role = (Role) null;
    }
    clickable(modifier, enabled, onClickLabel, role, onClick);
}

public static final void clickableDemo() {
    Modifier modifier = Modifier.Companion.getINSTANCE();
    clickable(modifier, true, "clickableDemo", (Role) null, (Function0) null.INSTANCE);
    clickable$default(modifier, true, (String) null, (Role) null, (Function0) null.INSTANCE, 6, (Object) null);
    clickable$default(modifier, false, "clickableDemo", (Role) null, (Function0) null.INSTANCE, 5, (Object) null);
}
```

以上就可以发现一些和 Clickable 类一致的地方了：

- 扩展函数最终会对应 `clickable` 和 `clickable$default` 这两个静态方法。命名规则和上面讲的一样
- `clickable` 方法相比原扩展函数会多出一个 modifier 参数，`clickable$default` 方法会多出 modifier、flag、object 三个参数。方法签名信息也和上面讲的一样
- `clickableDemo` 方法中，只有后两个方法调用的是 `clickable$default` 方法，传入的 flag 值分别是 6 和 5，object 值均为 null

多出一个 modifier 参数的原因上面已经讲了，object 值没有使用到，这里也不理会

那 flag 值的作用是什么呢？值的生成规则又是什么呢？

其实，flag 值起到的是一个标识作用：由于扩展函数的 `enabled`、`onClickLabel`、`role` 这三个参数都包含默认值，所以 Kotlin 就需要有一个标识符用于标识开发者到底有没有主动传入这三个参数值，没有的话就需要去使用默认值

Kotlin 会根据一个二进制数来标识开发者是否有主动传入方法的入参参数：如果这三个参数都有传入，就对应二进制 000；如果只传入了 enabled，就对应二进制 110。也即是说，参数在方法的参数列表中越靠前，在二进制中的位置就越靠后，有传入值的话就用 0 表示，没传入值就用 1 表示

对应上述的三个方法：

- 第一个方法我传入了所有参数，此时所有入参参数都不用使用到默认值，因此调用的是 Java 代码中的 `clickable` 方法
- 第二个和第三个方法我分别只传入了 `enabled` 和 `onClickLabel`，对应的二进制就是 110 和 101，得到的十进制值就分别是 6 和 5 了。`clickable$default` 方法内部就会根据 & 运算，来判断对应位置的二进制位是否为 1，是 1 的话就说明开发者没有主动传入该参数，此时就需要将该入参赋值为默认值了

所以说，虽然 Clickable 类只定义了四个和点击事件相关的扩展函数，但由于每个方法均包含默认参数值，所以在编译过后就会变成八个方法，多出来的方法也会多包含 flag 和 object 两个参数，这都是由 Kotlin 扩展函数的实现原理决定的

# 四、动手插桩

知道了 Kotlin 扩展函数的实现原理以及会带来的影响后，我们就可以明白 ClassVisitor 输出的方法签名信息并没有错，这些方法对应的就是源码中的那四个扩展函数，我们仅需要处理那两个不包含 flag 和 object 两个参数的方法即可，后面就可以来进行实际编码了

回顾一开始的思路：将 `clickable` 和 `combinedClickable` 这两个方法的入参参数 `onClick` 均重新赋值为 OnClickWrap 实例，在 OnClickWrap 中实现防抖逻辑，然后选择性地执行 `onClick` 方法

```kotlin
package github.leavesczy.asm.doubleClick.compose

class OnClickWrap(private val onClick: (() -> Unit)) : Function0<Unit> {

    companion object {

        private const val MIN_DURATION = 500L

        private var lastClickTime = 0L

    }

    override fun invoke() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime > MIN_DURATION) {
            lastClickTime = currentTime
            onClick()
            log("onClick isEnabled : true")
        } else {
            log("onClick isEnabled : false")
        }
    }

    private fun log(log: String) {
        Log.e(
            "OnClickWrap",
            "${System.identityHashCode(this)} ${System.identityHashCode(onClick)} $log"
        )
    }

}
```

对于 ClassVisitor 来说，Class 文件中的每一个方法均会对应一个 MethodNode，我们可以通过对比 MethodNode 的签名信息 desc 来识别到 `clickable` 和 `combinedClickable` 这两个方法。在编译过后，这两个方法的 `onClick` 参数在所有入参参数中的索引分别是 6 和 9，我们通过该索引就可以拿到入参值并将之重新赋值为 OnClickWrap 实例

对应以下代码：

```kotlin
methods.forEach { methodNode ->
    val methodName = methodNode.name
    val methodDesc = methodNode.desc
    LogPrint.log("methodName: $methodName \n methodDesc: $methodDesc")
    val onClickArgumentIndex = when (methodDesc) {
        ClickableMethodDesc -> {
            6
        }
        CombinedClickableMethodDesc -> {
            9
        }
        else -> {
            -1
        }
    }
    if (onClickArgumentIndex > 0) {
        val instructions = methodNode.instructions
        val input = InsnList()
        input.add(
            TypeInsnNode(
                Opcodes.NEW,
                "github/leavesczy/asm/doubleClick/compose/OnClickWrap"
            )
        )
        input.add(InsnNode(Opcodes.DUP))
        input.add(VarInsnNode(Opcodes.ALOAD, onClickArgumentIndex))
        input.add(
            MethodInsnNode(
                Opcodes.INVOKESPECIAL,
                "github/leavesczy/asm/doubleClick/compose/OnClickWrap",
                "<init>",
                "(Lkotlin/jvm/functions/Function0;)V",
                false
            )
        )
        input.add(VarInsnNode(Opcodes.ASTORE, onClickArgumentIndex))
        instructions.insert(input)
    }
}
```

还有个问题需要关注：由于上述代码是直接对 Clickable 类进行插桩，所以我们项目中所有使用到 `clickable` 和 `combinedClickable` 这两个方法的可组合函数都会受到影响，但未必所有地方都需要实现防抖，为了灵活性考虑，我们需要实现一个白名单机制，在白名单内的点击事件则不进行检查

这里我通过判断 `onClickLabel` 的值是否是 `noCheck` 来决定是否要启用双击防抖功能，伪代码如下所示

```kotlin
fun Modifier.clickable(
    interactionSource: MutableInteractionSource,
    indication: Indication?,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
) {
    if (onClickLabel != "noCheck") {
        onClick = OnClickWrap(onClick)
    }
    //TODO
}
```

对应的插桩代码：

```kotlin
if (onClickArgumentIndex > 0) {
    val onClickLabelArgumentIndex = 4
    val input = InsnList()
    input.add(LdcInsnNode("noCheck"))
    input.add(VarInsnNode(Opcodes.ALOAD, onClickLabelArgumentIndex))
    input.add(
        MethodInsnNode(
            Opcodes.INVOKEVIRTUAL,
            "java/lang/String",
            "equals",
            "(Ljava/lang/Object;)Z",
            false
        )
    )
    val label = LabelNode()
    input.add(JumpInsnNode(Opcodes.IFNE, label))
    input.add(
        TypeInsnNode(
            Opcodes.NEW,
            "github/leavesczy/asm/doubleClick/compose/OnClickWrap"
        )
    )
    input.add(InsnNode(Opcodes.DUP))
    input.add(VarInsnNode(Opcodes.ALOAD, onClickArgumentIndex))
    input.add(
        MethodInsnNode(
            Opcodes.INVOKESPECIAL,
            "github/leavesczy/asm/doubleClick/compose/OnClickWrap",
            "<init>",
            "(Lkotlin/jvm/functions/Function0;)V",
            false
        )
    )
    input.add(VarInsnNode(Opcodes.ASTORE, onClickArgumentIndex))
    input.add(label)
    methodNode.instructions.insert(input)
}
```

最终的防抖效果就可以很明显的看出来，当快速点击有启用双击防抖功能的控件时，index 值的递增速度明显慢于不防抖的控件

![](https://upload-images.jianshu.io/upload_images/2552605-83d2676cb7ce552a.gif)

# 五、还遗留的问题

以上的效果看着还可以，但其实并不完善，还遗留着几个问题

## 作用域不可控

由于我是直接修改 Clickable 类，因此防抖逻辑除了会作用于项目主体外，还包括其它任何使用了 Clickable 类的依赖库，从而导致我们无法自由控制防抖逻辑的作用域

## 白名单机制不完善

如果我们在代码中都是通过直接调用`clickable` 和 `combinedClickable` 这两个方法来监听点击事件的话，那通过判断 `onClickLabel` 的值来实现白名单功能是完全可行的，但对于 Jetpack Compose 提供的一些封装了点击事件的控件就不适用了。例如 Button 和 TextButton 内部都封装了 `clickable` 方法，但没有开放设置  `onClickLabel` 值的入口，所以这类控件就无法加入白名单了

## 防抖逻辑会互相关联

进行双击防抖的时候，防抖逻辑可以分为两种：

- 每个控件之间的防抖逻辑是互相关联的。也即是所，如果分别快速点击两个不同的控件，那么只有第一个点击事件会生效
- 每个控件之间的防抖逻辑是互相独立的。也即是说，如果分别快速点击两个不同的控件，那么这两个点击事件均会生效

在上文中隐藏了一个小细节：OnClickWrap 中定义的 `lastClickTime` 是静态变量。这就会导致不同的点击事件源会共享 `lastClickTime` 值且同时受到该值的限制，因此本文实现的效果是第一种。如果声明为非静态变量，那对应的就是第二种逻辑

而我之所以不将`lastClickTime` 声明为非静态变量，是因为被可组合函数的 **重组** 限制了：假如点击事件会造成对应的可组合函数重组，那么将 `onClick` 重置为 OnClickWrap 实例的代码将被再一次执行，此时声明为非静态变量的 `lastClickTime`  就又变成了默认值 0，从而也就无法实现双击防抖的效果了

# 六、结尾

想要解决以上遗留的问题，也许需要将思路改为从调用方进行插桩才能解决，但插桩逻辑就会变得很复杂，我目前还不知道该如何实现。希望本文能抛砖引玉，读者看完后能提出更好的实现方式

相关的代码我都上传到了 Github：[ASM_Transform](https://github.com/leavesCZY/ASM_Transform)

希望对你有所帮助 🤣🤣🤣