> 公众号：[字节数组](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/adbc507fc3704fd8955aae739a433db2~tplv-k3u1fbpfcp-zoom-1.image)
>
> 希望对你有所帮助 🤣🤣

在 2019 年 Google I/O 大会上，Google 宣布了今后 Android 开发将优先使用 Kotlin ，即 Kotlin-first，随之在 Android 开发界兴起了一阵全民学习 Kotlin 的热潮。之后 Google 也推出了一系列用 Kotlin 实现的 ktx 扩展库，例如 `activity-ktx`、`fragment-ktx`、`core-ktx`等，提供了各种方便的扩展方法用于简化开发者的工作，Kotlin 协程目前也是官方在 Android 上进行异步编程的推荐解决方案

Google 推荐优先使用 Kotlin，也宣称不会放弃 Java，但目前各种 ktx 扩展库还是需要由 Kotlin 代码进行使用才能最大化地享受到其便利性，Java 代码来调用显得有点不伦不类。作为 Jetpack 主要组件之一的 Paging 3.x 版本目前也已经完全用 Kotlin 实现，为 Kotlin 协程提供了一流的支持。刚出正式版本不久的 Jetpack Compose 也只支持 Kotlin，Java 无缘声明式 UI

开发者可以感受到 Kotlin 在 Android 开发中的重要性在不断提高，虽然 Google 说不会放弃 Java，但以后的事谁说得准呢？开发者还是需要尽早迁移到 Kotlin，这也是必不可挡的技术趋势

Kotlin 在设计理念上有很多和 Java 不同的地方，开发者能够直观感受到的是语法层面上的差异性，背后也包含有**一系列隐藏的性能开销以及一些隐藏得很深的“坑”**，本篇文章就来介绍在使用 Kotlin 过程中存在的隐藏性能开销，帮助读者避坑，希望对你有所帮助 🤣🤣

# 慎用 @JvmOverloads

`@JvmOverloads` 注解大家应该不陌生，其作用在具有默认参数的方法上，用于向 Java 代码生成多个重载方法

例如，以下的 `println` 方法对于 Java 代码来说就相当于两个重载方法，默认使用空字符串作为入参参数

```kotlin
//Kotlin
@JvmOverloads
fun println(log: String = "") {

}
```

```java
//Java
public void println(String log) {

}

public void println() {
	println("");
}
```

`@JvmOverloads` 很方便，减少了 Java 代码调用 Kotlin 代码时的调用成本，使得 Java 代码也可以享受到默认参数的便利，但在某些特殊场景下也会引发一个隐藏得很深的 bug

举个例子

我们知道 Android 系统的 View 类包含有多个构造函数，我们在实现自定义 View 时至少就要声明一个**包含有两个参数的构造函数**，参数类型必须依次是 Context 和 AttributeSet，这样该自定义 View 才能在布局文件中使用。而 View 类的构造函数最多包含有四个入参参数，最少只有一个，为了省事，我们在用 Kotlin 代码实现自定义 View 时，就可以用 `@JvmOverloads` 来很方便地继承 View 类，就像以下代码

```kotlin
open class BaseView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0, defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes)
```

如果我们是像 BaseView 一样直接继承于 View 的话，此时使用`@JvmOverloads`就不会产生任何问题，可如果我们继承的是 TextView 的话，那么问题就来了

直接继承于 TextView 不做任何修改，在布局文件中分别使用 MyTextView 和 TextView，给它们完全一样的参数，看看运行效果

```kotlin
open class MyTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0, defStyleRes: Int = 0
) : TextView(context, attrs, defStyleAttr, defStyleRes)
```

```xml
<github.leavesc.demo.MyTextView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:gravity="center"
    android:text="业志陈"
    android:textSize="42sp" />

<TextView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:gravity="center"
    android:text="业志陈"
    android:textSize="42sp" />
```

此时两个 TextView 就会呈现出不一样的文本颜色了，十分神奇

![](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a8db31ff83054797a9a0048919df668a~tplv-k3u1fbpfcp-watermark.image)

这就是 `@JvmOverloads` 带来的一个隐藏问题。因为 TextView 的 `defStyleAttr` 实际上是有一个默认值的，即 `R.attr.textViewStyle`，当中就包含了 TextView 的默认文本颜色，而由于 MyTextView 为 `defStyleAttr` 指定了一个默认值 0，这就导致 MyTextView 丢失了一些默认风格属性

```java
public TextView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, com.android.internal.R.attr.textViewStyle);
}
```

因此，如果我们要直接继承的是 View 类的话可以直接使用`@JvmOverloads`，此时不会有任何问题，而如果我们要继承的是现有控件的话，就需要考虑应该如何设置默认值了

# 慎用 解构声明

有时我们会有把一个对象拆解成多个变量的需求，Kotlin 也提供了这类语法糖支持，称为解构声明

例如，以下代码就将 People 变量解构为了两个变量：name 和 nickname，变量名可以随意取，每个变量就按顺序对应着 People 中的字段

```java
data class People(val name: String, val nickname: String)

private fun printInfo(people: People) {
    val (name, nickname) = people
    println(name)
    println(nickname)
}
```

每个解构声明其实都会被编译成以下代码，解构操作其实就是在按照顺序获取特定方法的返回值

```java
String name = people.component1();

String nickname = people.component2();
```

`component1()` 和 `component2()` 函数是 Kotlin 为数据类自动生成的方法，People 反编译为 Java 代码后就可以看到，每个方法返回的其实都是成员变量，方法名包含的数字对应的就是成员变量在数据类中的声明顺序

```java
public final class People {
   @NotNull
   private final String name;
   @NotNull
   private final String nickname;

   @NotNull
   public final String component1() {
      return this.name;
   }

   @NotNull
   public final String component2() {
      return this.nickname;
   }
    
}
```

解构声明和数据类配套使用时就有一个隐藏的知识点，看以下例子

假设后续我们为 People 添加了一个新字段 city，此时 `printInfo` 方法一样可以正常调用，但 nickname 指向的其实就变成了 people 变量内的 city 字段了，含义悄悄发生了变化，此时就会导致逻辑错误了

```kotlin
data class People(val name: String, val city: String, val nickname: String)

private fun printInfo(people: People) {
    val (name, nickname) = people
    println(name)
    println(nickname)
}
```

数据类中的字段是可以随时增减或者变换位置的，从而使得解构结果和我们一开始预想的不一致，因此我觉得解构声明和数据类不太适合放在一起使用

# 慎用 toLowerCase 和 toUpperCase

当我们要以忽略大小写的方式比较两个字符串是否相等时，通常想到的是通过 `toUpperCase` 或 `toLowerCase` 方法将两个字符串转换为全大写或者全小写，然后再进行比较，这种方式完全可以满足需求，但当中也包含着一个隐藏开销

例如，以下的 Kotlin 代码反编译为 Java 代码后，可以看到每次调用`toUpperCase`方法都会创建一个新的临时变量，然后再去调用临时变量的 `equals` 方法进行比较

```kotlin
fun main() {
    val name = "leavesC"
    val nickname = "leavesc"
    println(name.toUpperCase() == nickname.toUpperCase())
}

public static final void main() {
    String name = "leavesC";
    String nickname = "leavesc";
    String var10000 = name.toUpperCase();
    String var10001 = nickname.toUpperCase();
    boolean var2 = Intrinsics.areEqual(var10000, var10001);
    System.out.println(var2);
}
```

以上代码就多创建了两个临时变量，这样的代码无疑会比较低效

有一个更好的解决方案，就是通过 Kotlin 提供的支持忽略大小写的 `equals` 扩展方法来进行比较，此方法内部会调用 String 类原生的 `equalsIgnoreCase`来进行比较，从而避免了创建临时变量，相对来说会比较高效一些

```kotlin
fun main() {
    val name = "leavesC"
    val nickname = "leavesc"
    println(name.equals(other = nickname, ignoreCase = true))
}

public static final void main() {
    String name = "leavesC";
    String nickname = "leavesc";
    boolean var2 = StringsKt.equals(name, nickname, true);
    boolean var3 = false;
    System.out.println(var2);
}
```

# 慎用 arrayOf

Kotlin 中的数组类型可以分为两类：

- `IntArray、LongArray、FloatArray` 形式的**基本数据类型数组**，通过 `intArrayOf、longArrayOf、floatArrayOf` 等方法来声明
- `Array<T>` 形式的**对象类型数组**，通过 `arrayOf、arrayOfNulls` 等方法来声明

例如，以下的 Kotlin 代码都是用于声明整数数组，但实际上存储的数据类型并不一样

```kotlin
val intArray: IntArray = intArrayOf(1, 2, 3)

val integerArray: Array<Int> = arrayOf(1, 2, 3)
```

将以上代码反编译为 Java 代码后，就可以明确地看出一种是基本数据类型 int，一种是包装类型 Integer，`arrayOf` 方法会自动对入参值进行**装箱**

```java
private final int[] intArray = new int[]{1, 2, 3};

private final Integer[] integerArray = new Integer[]{1, 2, 3};
```

为了表示基本数据类型的数组，Kotlin 为每一种基本数据类型都提供了若干相应的类并做了特殊的优化。例如，`IntArray、ByteArray、BooleanArray` 等类型都会被编译成普通的 Java 基本数据类型数组：`int[]、byte[]、boolean[]`，这些数组中的值在存储时不会进行装箱操作，而是使用了可能的最高效的方式

因此，如果没有必要的话，我们在开发中要慎用 `arrayOf` 方法，避免不必要的装箱消耗

# 慎用 vararg

和 Java 一样，Kotlin 也支持可变参数，允许将任意多个参数打包到一个数组中再一并传给函数，Kotlin 通过使用 varage 关键字来声明可变参数

我们可以向 `printValue` 方法传递任意数量的入参参数，也可以直接传入一个数组对象，但 Kotlin 要求显式地解包数组，以便每个数组元素在函数中能够作为单独的参数来调用，这个功能被称为展开运算符，使用方式就是在数组前加一个 *

```kotlin
fun printValue(vararg values: Int) {
    values.forEach {
        println(it)
    }
}

fun main() {
    printValue()
    printValue(1)
    printValue(2, 3)
    val values = intArrayOf(4, 5, 6)
    printValue(*values)
}
```

如果我们是以直接传递若干个入参参数的形式来调用 `printValue` 方法的话，Kotlin 会自动将这些参数打包为一个数组进行传递，这里面就包含着创建数组的开销，这方面和 Java 保持一致。 如果我们传入的参数就已经是数组的话，Kotlin 相比 Java 就存在着一个隐藏开销，Kotlin 会复制现有数组作为参数拿来使用，相当于多分配了额外的数组空间，这可以从反编译后的 Java 代码看出来

```java
public static final void printValue(@NotNull int... values) {
  	Intrinsics.checkNotNullParameter(values, "values");
  	int $i$f$forEach = false;
  	int[] var3 = values;
  	int var4 = values.length;

  	for(int var5 = 0; var5 < var4; ++var5) {
     	int element$iv = var3[var5];
     	int var8 = false;
     	boolean var9 = false;
     	System.out.println(element$iv);
  	}
}

public static final void main() {
  	printValue();
  	printValue(1);
  	printValue(2, 3);
  	int[] values = new int[]{4, 5, 6};
  	//复制后再进行调用
  	printValue(Arrays.copyOf(values, values.length));
}

// $FF: synthetic method
public static void main(String[] var0) {
  	main();
}
```

可以看到 Kotlin 会通过 `Arrays.copyOf` 复制现有数组，将复制后的数组作为参数进行调用，这样做的好处就是可以避免 `printValue` 方法影响到原有数组，坏处就是会额外消耗多一份的内存空间

# 慎用 lazy

我们经常会使用`lazy()`函数来惰性加载只读属性，将加载操作延迟到需要使用的时候，适用于某些不适合立刻加载或者加载成本较高的情况

例如，以下的 `lazyValue` 只会等到我们调用到的时候才会被赋值

```kotlin
val lazyValue by lazy {
    "it is lazy value"
}
```

而在使用`lazy()`函数时很容易被忽略的地方就是其包含有一个可选的 model 参数：

- LazyThreadSafetyMode.SYNCHRONIZED。只允许由单个线程来完成初始化，且初始化操作包含有双重锁检查，从而使得所有线程都得到相同的值
- LazyThreadSafetyMode.PUBLICATION。允许多个线程同时执行初始化操作，但只有第一个初始化成功的值会被当做最终值，最终所有线程也都会得到相同的值
- LazyThreadSafetyMode.NONE。允许多个线程同时执行初始化操作，不进行任何线程同步，导致不同线程可能会得到不同的初始化值，因此不应该用于多线程环境

`lazy()`函数默认情况下使用的就是`LazyThreadSafetyMode.SYNCHRONIZED`，从 SynchronizedLazyImpl 可以看到，其内部就使用到了`synchronized`来实现多线程同步，以此避免多线程竞争

```kotlin
public actual fun <T> lazy(initializer: () -> T): Lazy<T> = SynchronizedLazyImpl(initializer)

private class SynchronizedLazyImpl<out T>(initializer: () -> T, lock: Any? = null) : Lazy<T>, Serializable {
    private var initializer: (() -> T)? = initializer
    @Volatile private var _value: Any? = UNINITIALIZED_VALUE
    // final field is required to enable safe publication of constructed instance
    private val lock = lock ?: this

    override val value: T
        get() {
            val _v1 = _value
            if (_v1 !== UNINITIALIZED_VALUE) {
                @Suppress("UNCHECKED_CAST")
                return _v1 as T
            }

            return synchronized(lock) {
                val _v2 = _value
                if (_v2 !== UNINITIALIZED_VALUE) {
                    @Suppress("UNCHECKED_CAST") (_v2 as T)
                } else {
                    val typedValue = initializer!!()
                    _value = typedValue
                    initializer = null
                    typedValue
                }
            }
        }

    override fun isInitialized(): Boolean = _value !== UNINITIALIZED_VALUE

    override fun toString(): String = if (isInitialized()) value.toString() else "Lazy value not initialized yet."

    private fun writeReplace(): Any = InitializedLazyImpl(value)
}
```

对于 Android 开发者来说，大多数情况下我们都是在主线程中调用 `lazy()` 函数，此时使用 `LazyThreadSafetyMode.SYNCHRONIZED` 就会带来不必要的线程同步开销，因此可以根据实际情况考虑替换为`LazyThreadSafetyMode.NONE`

# 慎用 lateinit var

lateinit var 适用于某些不方便马上就初始化变量的场景，用于将初始化操作延后，同时也存在一些使用上的限制：如果在未初始化的情况下就使用该变量的话会导致 NPE

例如，如果在 name 变量还未初始化时就调用了 `print` 方法的话，此时就会导致 NPE。且由于 lateinit var 变量不允许为 null，因此此时我们也无法通过判空来得知 name 是否已经被初始化了，而且判空操作本身也相当于在调用 name 变量，在未初始化的时候一样会导致 NPE

```kotlin
lateinit var name: String

fun print() {
    println(name)
}
```

我们可以通过另一种方式来判断 lateinit 变量是否已初始化

lateinit 实际上是通过代理机制来实现的，关联的是 KProperty0 接口，KProperty0 就提供了一个扩展属性用于判断其代理的值是否已经初始化了

```kotlin
@SinceKotlin("1.2")
@InlineOnly
inline val @receiver:AccessibleLateinitPropertyLiteral KProperty0<*>.isInitialized: Boolean
    get() = throw NotImplementedError("Implementation is intrinsic")
```

因此我们可以通过以下方式来进行判断，从而避免不安全的访问操作

```kotlin
lateinit var name: String

fun print() {
    if (this::name.isInitialized) {
        println("isInitialized true")
        println(name)
    } else {
        println("isInitialized false")
        println(name) //会导致 NPE
    }
}
```

# lambda 表达式

lambda 表达式在语义上很简洁，既避免了冗长的函数声明，也解决了以前需要强类型声明函数类型的情况

例如，以下代码就通过 lambda 表达式声明了一个回调函数 callback，我们无需创建一个具体的函数类型，而只需声明需要的**入参参数、入参类型、函数返回值**就可以

```kotlin
fun requestHttp(callback: (code: Int, data: String) -> Unit) {
    callback(200, "success")
}

fun main() {
    requestHttp { code, data ->
        println("code: $code")
        println("data: $data")
    }
}
```

lambda 表达式语法虽然方便，但也隐藏着两个性能问题：

- 每次调用 lambda 表达式都相当于在创建一个对象
- lambda 表达式内部隐藏了自动装箱和自动拆箱的操作

将以上代码反编译为 Java 代码后，可以看到 callback 最终的实际类型就是 Function2，每次调用`requestHttp` 方法就相当于是在创建一个 Function2 变量

```java
public static final void requestHttp(@NotNull Function2 callback) {
	Intrinsics.checkNotNullParameter(callback, "callback");
	callback.invoke(200, "success");
}
```

Function2 是 Kotlin 提供的一个的泛型接口，数字 2 即代表其包含两个入参值

```kotlin
public interface Function2<in P1, in P2, out R> : Function<R> {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2): R
}
```

Kotlin 会在编译阶段将开发者声明的 lambda 表达式转换为相应的 FunctionX 对象，调用 lambda 表达式就相当于在调用其 `invoke` 方法，以此为低版本 JVM 平台（例如 Java 6 / 7）也能提供 lambda 表达式功能。此外，我们也知道泛型类型不可能是基本数据类型，因此我们在 Kotlin 中声明的 Int 最终会被自动装箱为 Integer，lambda 表达式内部自动完成了装箱和拆箱的操作

所以说，简洁的 lambda 表达式背后就隐藏了**自动创建 Function 对象进行中转调用，自动装箱和自动拆箱的过程，且最终创建的方法总数要多于表面上看到的**

如果想要避免 lambda 表达式的以上开销，可以通过使用 inline 内联函数来实现

在使用 inline 关键字修饰 `requestHttp` 方法后，可以看到此时 `requestHttp` 的逻辑就相当于被直接复制到了 `main` 方法内部，不会创建任何多余的对象，且此时使用的也是 int 而非 Integer

```kotlin
inline fun requestHttp(callback: (code: Int, data: String) -> Unit) {
    callback(200, "success")
}

fun main() {
    requestHttp { code, data ->
        println("code: $code")
        println("data: $data")
    }
}
```

```java
public static final void main() {
	String data = "success";
	int code = 200;
	String var4 = "code: " + code;
	System.out.println(var4);
	var4 = "data: " + data;
	System.out.println(var4);
}
```

通过内联函数，可以使得编译器直接在调用方中使用内联函数体中的代码，相当于直接把内联函数中的逻辑复制到了调用方中，完全避免了调用带来的开销。对于高阶函数，作为参数传递的 lambda 表达式的主体也将被内联，这使得：

- 声明和调用 lambda 表达式时，不会实例化 Function 对象
- 没有自动装箱和拆箱的操作
- 不会导致方法数增多，但如果内联函数方法体较大且被多处调用的话，可能导致最终代码量显著增加

# init 的声明顺序很重要

看以下代码，我们可以在 init 块中调用 parameter1，却无法调用 parameter2，从 IDE 的提示信息 ` Variable 'parameter2' must be initialized`也可以看出来，对于 init 块来说 parameter2 此时还未赋值，自然就无法使用了

```kotlin
class KotlinMode {

    private val parameter1 = "leavesC"

    init {
        println(parameter1)
        //error: Variable 'parameter2' must be initialized
        //println(parameter2)
    }

    private val parameter2 = "业志陈"

}
```

从反编译出的 Java 代码也可以看出来，由于 parameter2 是声明在 init 块之后，所以 parameter2 的赋值操作其实是放在构造函数中的最后面，因此 IDE 的语法检查器就会阻止我们在 init 块中来调用 parameter2 了

```java
public final class KotlinMode {
   private final String parameter1 = "leavesC";
   private final String parameter2;

   public KotlinMode() {
      String var1 = this.parameter1;
      System.out.println(var1);
      this.parameter2 = "业志陈";
   }
}
```

IDE 会阻止开发者去调用还未初始化的变量，防止我们写出不安全的代码，我们也可以用以下方式来绕过语法检查，但同时也写出了不安全的代码

我们可以通过在 init 块中调用 `print()` 方法的方式来间接访问 parameter2，此时代码是可以正常编译的，但此时 parameter2 也只会为 null

```kotlin
class KotlinMode {

    private val parameter1 = "leavesC"

    init {
        println(parameter1)
        print()
    }

    private fun print() {
        println(parameter2)
    }

    private val parameter2 = "业志陈"

}
```

从反编译出的 Java 代码可以看出来，`print()`方法依旧是会在 parameter2 初始化之前被调用，此时`print()`方法访问到的 parameter2 也只会为 null，从而引发意料之外的 NPE

```java
public final class KotlinMode {
   private final String parameter1 = "leavesC";
   private final String parameter2;

   private final void print() {
      String var1 = this.parameter2;
      System.out.println(var1);
   }

   public KotlinMode() {
      String var1 = this.parameter1;
      System.out.println(var1);
      this.print();
      this.parameter2 = "业志陈";
   }
}
```

所以说，init 块和成员变量之间的声明顺序决定了在构造函数中的初始化顺序，我们应该先声明成员变量再声明 init 块，否则就有可能导致 NPE

# Gson & data class

来看个小例子，猜猜其运行结果会是怎样的

UserBean 是一个 dataClass，其 userName 字段被声明为非 null 类型，而 json 字符串中 userName 对应的值明确就是 null，那用 Gson 到底能不能反序列化成功呢？程序能不能成功运行完以下三个步骤？

```kotlin
data class UserBean(val userName: String, val userAge: Int)

fun main() {
    val json = """{"userName":null,"userAge":26}"""  
    val userBean = Gson().fromJson(json, UserBean::class.java) //第一步
    println(userBean) //第二步
    printMsg(userBean.userName) //第三步
}

fun printMsg(msg: String) {

}
```

实际上程序能够正常运行到第二步，但在执行第三步的时候反而直接报 NPE 异常了

```kotlin
UserBean(userName=null, userAge=26)
Exception in thread "main" java.lang.NullPointerException: Parameter specified as non-null is null: method temp.TestKt.printMsg, parameter msg
	at temp.TestKt.printMsg(Test.kt)
	at temp.TestKt.main(Test.kt:16)
	at temp.TestKt.main(Test.kt)
```

`printMsg` 方法接收了参数后实际上什么也没做，为啥会抛出 NPE ？

将`printMsg`反编译为 Java 方法，可以发现方法内部会对入参进行空校验，当发现为 null 时就会直接抛出 NPE。这个比较好理解，毕竟 Kotlin 的类型系统会严格区分 **可 null** 和 **不可为 null** 两种类型，其区分手段之一就是会自动在我们的代码里插入一些类型校验逻辑，即自动加上了非空断言，当发现不可为 null 的参数传入了 null 的话就会马上抛出 NPE，即使我们并没有使用到该参数

```java
public static final void printMsg(@NotNull String msg) {
	Intrinsics.checkNotNullParameter(msg, "msg");
}
```

那既然 UserBean 中的 userName 字段已经被声明为非 null 类型了，那么为什么还可以反序列化成功呢？按照我自己的第一直觉，应该在进行反序列的时候就直接抛出异常才对

将 UserBean 反编译为 Java 代码后，也可以看到其构造函数中是有对 userName 进行 null 检查的，当发现为 null 的话会直接抛出 NPE

```java
public final class UserBean {
   @NotNull
   private final String userName;
   private final int userAge;

   @NotNull
   public final String getUserName() {
      return this.userName;
   }

   public final int getUserAge() {
      return this.userAge;
   }

   public UserBean(@NotNull String userName, int userAge) {
      //进行 null 检查
      Intrinsics.checkNotNullParameter(userName, "userName");
      super();
      this.userName = userName;
      this.userAge = userAge;
   }

   ···
    
}
```

那 Gson 是怎么绕过 Kotlin 的 null 检查的呢？

其实，通过查看 Gson 内部源码，可以知道 Gson 是通过 Unsafe 包来实例化 UserBean 对象的，Unsafe 提供了一个非常规实例化对象的方法：`allocateInstance`，该方法提供了通过 Class 对象就可以创建出相应实例的功能，而且不需要调用其构造函数、初始化代码、JVM 安全检查等，即使构造函数是 private 的也能通过此方法进行实例化。因此 Gson 实际上并不会调用到 UserBean 的构造函数，相当于绕过了 Kotlin 的 null 检查，所以即使 userName 值为 null 最终也能够反序列化成功


![](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/af135d36b80341e68befd0c0bbe091ce~tplv-k3u1fbpfcp-watermark.image?)

此问题的出现场景大多是在移动端解析服务端传来的数据的时候，移动端将数据声明为非空类型，但服务端给过来的数据却为 null 值，此时用户看到的可能就是应用崩溃了……

一方面，我觉得移动端应该对服务端传来的数据保持不信任的态度，不能觉得对方传来的数据就一定是符合约定的，为了保证安全需要将数据均声明为可空类型。另一方面，这也无疑导致移动端需要加上很多多余的判空操作，简直有点无解 =_=

# ARouter & JvmField

在 Java 中，字段和其访问器的组合被称作属性。在 Kotlin 中，属性是头等的语言特性，完全替代了字段和访问器方法。在类中声明一个属性和声明一个变量一样是使用 val 和 var 关键字，两者在使用上的差异就在于赋值后是否还允许修改，在字节码上的差异性之一就在于是否会自动生成相应的 `setValue` 方法

例如，以下的 Kotlin 代码在反编译为 Java 代码后，可以看到两个属性的可见性都变为了 private， name 变量会同时包含有`getValue`和`setValue` 方法，而 nickname 变量只有 `getValue` 方法，这也是我们在 Java 代码中只能以 `kotlinMode.getName()` 的方式来访问 name 变量的原因

```kotlin
class KotlinMode {

    var name = "业志陈"

    val nickname = "leavesC"

}
```

```java
public final class KotlinMode {
   @NotNull
   private String name = "业志陈";
   @NotNull
   private final String nickname = "leavesC";

   @NotNull
   public final String getName() {
      return this.name;
   }

   public final void setName(@NotNull String var1) {
      Intrinsics.checkNotNullParameter(var1, "<set-?>");
      this.name = var1;
   }

   @NotNull
   public final String getNickname() {
      return this.nickname;
   }
}
```

为了不让 Kotlin 的 var / val 变量自动生成 `getValue` 或 `setValue` 方法，达到和在 Java 代码中声明公开变量一样的效果，此时就需要为属性添加 `@JvmField` 注解了，添加后就会变为 public 类型的成员变量，且不包含任何 `getValue` 和 `setValue` 方法

```kotlin
class KotlinMode {

    @JvmField
    var name = "业志陈"

    @JvmField
    val nickname = "leavesC"

}

public final class KotlinMode {
   @JvmField
   @NotNull
   public String name = "业志陈";
   @JvmField
   @NotNull
   public final String nickname = "leavesC";
}
```

---

`@JvmField` 的一个使用场景就是在配套使用 ARouter 的时候。我们在使用 ARouter 进行参数自动注入时，就需要为待注入的参数添加 `@JvmField`注解，就像以下代码一样，不添加的话就会导致编译失败

```kotlin
@Route(path = RoutePath.USER_HOME)
class UserHomeActivity : AppCompatActivity() {

    @Autowired(name = RoutePath.USER_HOME_PARAMETER_ID)
    @JvmField
    var userId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_home)
        ARouter.getInstance().inject(this)
    }

}
```

那为什么不添加该注解就会导致编译失败呢？

其实，ARouter 实现参数自动注入是需要依靠注解处理器生成的辅助文件来实现的，即会生成以下的辅助代码，当中会以 `substitute.userId` 、`substitute.userName`的形式来调用 Activity 中的两个参数值，如果不添加 `@JvmField`注解，辅助文件就没法以直接调用变量名的方式来完成注入，自然就会导致编译失败了

```kotlin
public class UserHomeActivity$$ARouter$$Autowired implements ISyringe {
    
  private SerializationService serializationService;

  @Override
  public void inject(Object target) {
    serializationService = ARouter.getInstance().navigation(SerializationService.class);
    UserHomeActivity substitute = (UserHomeActivity)target;
    substitute.userId = substitute.getIntent().getLongExtra("userHomeId", substitute.userId);
  }
}
```

Kotlin 这套为属性自动生成 `getValue`和`setValue` 方法的机制有一个缺点，就是可能会导致方法数极速膨胀，使得 Android App 的 dex 文件很快就达到最大方法数限制，不得不进行分包处理

# 推荐阅读

- [Gson 和 Kotlin data class 的避坑指南](https://juejin.cn/post/6908391430977224718)

  更加详细地介绍了 Gson 和 Kotlin data class 在配套使用时可能出现的坑，值得一看 ！！！

- [两万六千字带你 Kotlin 入门](https://juejin.cn/post/6880602489297895438)

  读者是否还未开始学习 Kotlin 呢？或者是对 Kotlin 还了解得不够清楚呢？如果是的话，推荐阅读我的一篇 Kotlin 入门教程，全文两万六千多字，手把手教你学会 Kotlin ！！！