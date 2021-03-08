### 一、Hello World

按照国际惯例，学习一门新的语言通常都是要从打印 Hello World 开始的

```kotlin
package main

fun main() {
    val msg: String = "Hello World"
    println(msg)
}
```

从这个简单的函数就可以列出 kotlin 和 Java 的几个不同点

1. 函数可以定义在文件的最外层，不需要把它放在类中
2. 用关键字 **fun** 来声明一个函数
3. 可以省略 `main` 方法的参数
4. 参数类型写在变量名之后，这有助于在类型自动推导时省略类型声明
5. 使用 `println` 代替了 `System.out.println` ，这是 kotlin 标准库提供的对 Java 标准库函数的简易封装
6. 可以省略代码结尾的分号

### 二、Package

kotlin 文件都以 **package** 开头，同个文件中定义的所有声明（类、函数和属性）都会被放到这个包中。同个包中的声明可以直接使用，不同包的声明需要导入后使用

包的声明要放在文件顶部，import 语句紧随其后

```kotlin
package base

import java.text.SimpleDateFormat
import java.util.*
```

kotlin 不区分导入的是类还是函数，可以使用 `import` 关键字导入任何种类的声明。此外也可以在包名称后加上 **.*** 来导入包中定义的所有声明，从而让包中定义的类、顶层函数、和属性都可以直接引用

```kotlin
package learn.package2

val index = 10

fun Test(status: Boolean) = println(status)

class Point(val x: Int, val y: Int) {

    val isEquals1: Boolean
        get() {
            return x == y
        }

    val isEquals2
        get() = x == y

    var isEquals3 = false
        get() = x > y
        set(value) {
            field = !value
        }

}
```

```kotlin
package learn.package1

import learn.package2.Point
import learn.package2.Test
import learn.package2.index

fun main() {
    val point = Point(10, index)
    Test(true)
}
```

Java 语言规定类要放到和包结构**匹配**的文件夹目录结构中，且文件名必须和类名相同。而 kotlin 允许把多个类放到同一个文件中，文件名也可以任意选择。kotlin 也没有文件夹目录施加任何限制，包层级结构不需要遵循同样的目录层级结构 ,但 kotlin 官方还是建议根据包声明把源码文件放到相应的目录中

如果包名出现命名冲突，可以使用  **as  关键字**在本地重命名冲突项来消歧义

```kotlin
import learn.package1.Point
import learn.package2.Point as PointTemp //PointTemp 可以用来表示 learn.package2.Point 了
```

kotlin 中也有一个类似的概念可以用来重命名现有类型，叫做类型别名。类型别名用于为现有类型提供一个替代名称，如果类型名称比较长，就可以通过引入一个较短或者更为简略的名称来方便记忆

类型别名不会引入新的类型，依然对应其底层类型，所以在下述代码中输出的 class 类型是一致的

```kotlin
class Base {

    class InnerClass

}

typealias BaseInner = Base.InnerClass

fun main() {
    val baseInner1 = Base.InnerClass()
    val baseInner2 = BaseInner()
    println(baseInner1.javaClass) //class test2.Base$InnerClass
    println(baseInner2.javaClass) //class test2.Base$InnerClass
}
```

### 三、变量与数据类型

在 Java 中，大部分的变量是可变的（非 final 的），意味着任何可以访问到这个变量的代码都可以去修改它。而在 kotlin 中，变量可以分为 **可变变量(var)** 和 **不可变变量(val)** 两类

声明变量的关键字有两个：

- val（value /  varible+final）——不可变引用。使用 val 声明的变量不能在初始化之后再次赋值，对应的是 Java 中的 final 变量
- var（variable）——可变引用。var 变量的值可以被改变，对应的是 Java 中的非 final 变量

不可变变量在赋值之后就不能再去改变它的状态了，因此不可变变量可以说是线程安全的，因为它们无法改变，所有线程访问到的对象都是同一个，因此也不需要去做访问控制。开发者应当尽可能地使用不可变变量，这样可以让代码更加接近函数式编程风格

编程领域也推崇一种开发原则：尽可能使用 val，不可变对象及纯函数来设计程序。这样可以尽量避免副作用带来的影响

```kotlin
fun main() {
    //只读变量即赋值后不可以改变值的变量，用 val 声明
    //声明一个整数类型的不可变变量
    val intValue: Int = 100

    //声明一个双精度类型的可变变量
    var doubleValue: Double = 100.0
}
```

在声明变量时我们通常不需要显式指明变量的类型，这可以由编译器根据上下文自动推导出来。如果只读变量在声明时没有初始的默认值，则必须指明变量类型，且在使用前必须确保在各个分支条件下变量可以被初始化，否则编译器就会报异常

```kotlin
fun main() {
    val intValue = 1002222222222
    val doubleValue = 100.0
    val longValue = 100L

    //如果只读变量在声明时没有初始值，则必须指明变量类型
    val intValue2: Int
    if (false) {
        intValue2 = 10
    }
    println(intValue2) //error， Variable 'intValue2' must be initialized
}
```

#### 1、基本数据类型

与 Java 不同，kotlin 并不区分基本数据类型和它的包装类，在 kotlin 中一切都是对象，可以在任何变量上调用其成员函数和属性。kotlin 没有像 Java 中那样的原始基本类型，但 **byte、char、integer、float 或者 boolean** 等类型仍然有保留，但是全部都作为对象存在

对于基本类型，kotlin 相比 Java 有几点特殊的地方

- 数字、字符和布尔值可以在运行时表示为原生类型值，但对开发者来说，它们看起来就像普通的类
- 对于数字**没有隐式拓宽转换**，而在 Java 中 int 可以隐式转换为 long
- 所有未超出 Int 最⼤值的整型值初始化的变量都会自动推断为 Int 类型，如果初始值超过了其最⼤值，则会推断为 Long 类型， 如需显式指定 Long 类型值可在该值后追加 L 后缀
- 字符不能视为数字
- 不支持八进制

```kotlin
    //在 kotlin 中，int、long、float 等类型仍然存在，但是是作为对象存在的

    val intIndex: Int = 100
    //等价于，编译器会自动进行类型推导
    val intIndex = 100

    //数字类型不会自动转型，必须要进行明确的类型转换
    val doubleIndex: Double = intIndex.toDouble()
    //以下代码会提示错误，需要进行明确的类型转换
    //val doubleIndex: Double = intIndex

    val intValue: Int = 1
    val longValue: Long = 1
    //以下代码会提示错误，因为两者的数据类型不一致，需要转换为同一类型后才能进行比较
    //println(intValue == longValue)

    //Char 不能直接作为数字来处理，需要主动转换
    val ch: Char = 'c'
    val charValue: Int = ch.toInt()
    //以下代码会提示错误
    //val charValue: Int = ch

    //二进制
    val value1 = 0b00101
    //十六进制
    val value2 = 0x123
```

此外，kotlin 的可空类型不能用 Java 的基本数据类型表示，因为 null 只能被存储在 Java 的引用类型的变量中，这意味着只要使用了基本数据类型的可空版本，它就会被编译成对应的包装类型

```kotlin
    //基本数据类型
    val intValue_1: Int = 200
    //包装类
    val intValue_2: Int? = intValue_1
    val intValue_3: Int? = intValue_1
    //== 比较的是数值相等性，因此结果是 true
    println(intValue_2 == intValue_3)
    //=== 比较的是引用是否相等，因此结果是 false
    println(intValue_2 === intValue_3)
```

> 如果 intValue_1 的值是100，就会发现 intValue_2 === intValue_3 的比较结果是 true，这就涉及到 Java 对包装类对象的复用问题了

#### 2、字符串

kotlin 与 Java 一样用  String  类型来表示字符串，字符串是不可变的，可以使用索引运算符访问`[]` 来访问包含的单个字符，也可以用  for  循环来迭代字符串，此外也可以用 + 来连接字符串

```kotlin
    val str = "leavesC"
    println(str[1])
    for (c in str) {
        println(c)
    }
    val str1 = str + " hello"
```

kotlin 支持在字符串字面值中引用局部变量，只需要在变量名前加上字符 \$ 即可，此外还可以包含用花括号括起来的表达式，此时会自动求值并把结果合并到字符串中

```kotlin
    val intValue = 100
    //可以直接包含变量
    println("intValue value is $intValue") //intValue value is 100
    //也可以包含表达式
    println("(intValue + 100) value is ${intValue + 100}")   //(intValue + 100) value is 200
```

如果你需要在原始字符串中表示字面值（$）字符（它不支持反斜杠转义），可以用下列语法：

```kotlin
    val price = "${'$'}100.99"
    println(price)  //$100.99
```

#### 3、数组

kotlin 中的数组是带有类型参数的类，其元素类型被指定为相应的类型参数，使用  Array  类来表示，  Array  类定义了  get  与  set  函数（按照运算符重载约定这会转变为  [ ]  ）以及  size  属性等

创建数组的方法有以下几种：

1. 用 arrayOf 函数创建一个数组，包含的元素是指定为该函数的实参
2. 用 arrayOfNulls 创建一个给定大小的数组，包含的元素均为 null，只能用来创建包含元素类型可空的数组
3. 调用 Array 类的构造方法，传递数组的大小和一个 lambda 表达式，调用 lambda 表达式来创建每一个数组元素

```kotlin
    //包含给定元素的字符串数组
    val array1 = arrayOf("leavesC", "叶", "https://github.com/leavesC")

    array1[0] = "leavesC"
    println(array1[1])
    println(array1.size)

    //初始元素均为 null ，大小为 10 的字符数组
    val array2 = arrayOfNulls<String>(10)

    //创建从 “a” 到 “z” 的字符串数组
    val array3 = Array(26) { i -> ('a' + i).toString() }
```

需要注意的是，数组类型的类型参数始终会变成对象类型，因此声明 **Array< Int >** 将是一个包含装箱类型（java.lang.Integer）的数组。如果想要创建没有装箱的基本数据类型的数组，必须使用一个基本数据类型数组的特殊类

为了表示基本数据类型的数组，kotlin 为每一种基本数据类型都提供了若干相应的类并做了特殊的优化。例如，有 **IntArray、ByteArray、BooleanArray** 等类型，这些类型都会被编译成普通的 Java 基本数据类型数组，比如 **int[]、byte[]、boolean[]** 等，这些数组中的值存储时没有进行装箱，而是使用了可能的最高效的方式。需要注意，IntArray 等并不是 Array 的子类

要创建一个基本数据类型的数组，有以下几种方式：

1. 向对应类型的类（如 IntArray）的构造函数传递数组大小，这将返回一个使用对应基本数据类型默认值初始化好的数组
2. 向对应类型的类（如 IntArray）的构造函数传递数组大小以及用来初始化每个元素的 lambda
3. 向工厂函数（如 charArrayOf）传递变长参数的值，从而得到指定元素值的数组

```kotlin
    //指定数组大小，包含的元素将是对应基本数据类型的默认值(int 的默认值是 0)
    val intArray = IntArray(5)
    //指定数组大小以及用于初始化每个元素的 lambda
    val doubleArray = DoubleArray(5) { Random().nextDouble() }
    //接收变长参数的值来创建存储这些值的数组
    val charArray = charArrayOf('H', 'e', 'l', 'l', 'o')
```

#### 4、Any 和 Any?

Any 类型是 kotlin 所有非空类型的超类型，包括像 Int 这样的基本数据类型

如果把基本数据类型的值赋给 Any 类型的变量，则会自动装箱

```kotlin
val any: Any = 100
println(any.javaClass) //class java.lang.Integer
```

如果想要使变量可以存储包括 null 在内的所有可能的值，则需要使用 Any?

```kotlin
val any: Any? = null
```

#### 5、Unit

kotlin 中的 Unit 类型类似于 Java 中的 void，可以用于函数没有返回值时的情况

```kotlin
fun check(): Unit {

}

//如果返回值为 Unit，则可以省略该声明
fun check() {

}
```

Unit 是一个完备的类型，可以作为类型参数，但 void 不行

```kotlin
interface Test<T> {
    fun test(): T
}

class NoResultClass : Test<Unit> {
    
    //返回 Unit，但可以省略类型说明，函数也不需要显式地 return 
    override fun test() {

    }

}
```

#### 6、Nothing

Nothing 类型没有任何值，只有被当做函数返回值使用，或者被当做泛型函数返回值的类型参数使用时才会有意义，可以用 Nothing 来表示一个函数不会被正常终止，从而帮助编译器对代码进行诊断

编译器知道返回值为 Nothing 类型的函数从不会正常终止，所以编译器会把 name1 的类型推断为非空，因为 name1 在为 null 时的分支处理会始终抛出异常

```kotlin
data class User(val name: String?)

fun fail(message: String): Nothing {
    throw IllegalStateException(message)
}

fun main() {
    val user = User("leavesC")
    val name = user.name ?: fail("no name")
    println(name) //leavesC

    val user1 = User(null)
    val name1 = user1.name ?: fail("no name")
    println(name1.length) //IllegalStateException
}
```

### 四、函数

kotlin 中的函数以关键字 fun 作为开头，函数名称紧随其后，再之后是用括号包裹起来的参数列表，如果函数有返回值，则再加上返回值类型，用一个冒号与参数列表隔开

```kotlin
        //fun 用于表示声明一个函数，getNameLastChar 是函数名
        //空括号表示该函数无传入参数，Char 表示函数的返回值类型是字符
        fun getNameLastChar(): Char {
            return name.get(name.length - 1)
        }

        //带有两个不同类型的参数，一个是 String 类型，一个是 Int 类型
        //返回值为 Int 类型
        fun test1(str: String, int: Int): Int {
            return str.length + int
        }
```

此外，表达式函数体的返回值类型可以省略，返回值类型可以自动推断，这种用单行表达式与等号定义的函数叫做**表达式函数体**。但对于一般情况下的有返回值的**代码块函数体**，**必须显式地**写出返回类型和 return 语句

```kotlin
        //getNameLastChar 函数的返回值类型以及 return 关键字是可以省略的
        //返回值类型可以由编译器根据上下文进行推导
        //因此，函数可以简写为以下形式
        fun getNameLastChar() = name.get(name.length - 1)
```

如果函数没有有意义的返回值，则可以声明为 Unit ，也可以省略 Unit

以下三种写法都是等价的

```kotlin
        fun test(str: String, int: Int): Unit {
            println(str.length + int)
        }

        fun test(str: String, int: Int) {
            println(str.length + int)
        }

        fun test(str: String, int: Int) = println(str.length + int)
```

#### 1、命名参数

为了增强代码的可读性，kotlin 允许我们使用命名参数，即在调用某函数的时候，可以将函数参数名一起标明，从而明确地表达该参数的含义与作用，但是在指定了一个参数的名称后，之后的所有参数都需要标明名称

```kotlin
fun main() {
    //错误，在指定了一个参数的名称后，之后的所有参数都需要标明名称
    //compute(index = 110, "leavesC")
    compute(index = 120, value = "leavesC")
    compute(130, value = "leavesC")
}

fun compute(index: Int, value: String) {

}
```

#### 2、默认参数值

可以在声明函数的时候指定参数的默认值，从而避免创建重载的函数

```kotlin
fun main() {
    compute(24)
    compute(24, "leavesC")
}

fun compute(age: Int, name: String = "leavesC") {

}
```

对于以上这个例子，如果按照常规的调用语法时，必须按照函数声明定义的参数顺序来给定参数，可以省略的只有排在末尾的参数

```kotlin
fun main() {
    //错误，不能省略参数 name
    // compute(24)
    // compute(24,100)
    //可以省略参数 value
    compute("leavesC", 24)
}

fun compute(name: String = "leavesC", age: Int, value: Int = 100) {}
```

如果使用命名参数，可以省略任何有默认值的参数，而且也可以按照任意顺序传入需要的参数

```kotlin
fun main() {
    compute(age = 24)
    compute(age = 24, name = "leavesC")
    compute(age = 24, value = 90, name = "leavesC")
    compute(value = 90, age = 24, name = "leavesC")
}

fun compute(name: String = "leavesC", age: Int, value: Int = 100) {

}
```

#### 3、可变参数

可变参数可以让我们把任意个数的参数打包到数组中传给函数，kotlin 的语法相比 Java 有所不同，改为通过使用 varage 关键字声明可变参数

例如，以下的几种函数调用方式都是正确的

```kotlin
fun main() {
    compute()
    compute("leavesC")
    compute("leavesC", "leavesc")
    compute("leavesC", "leavesc", "叶")
}

fun compute(vararg name: String) {
    name.forEach { println(it) }
}
```

在 Java 中，可以直接将数组传递给可变参数，而 kotlin 要求显式地解包数组，以便每个数组元素在函数中能够作为单独的参数来调用，这个功能被称为展开运算符，使用方式就是在数组参数前加一个 *

```kotlin
fun main() {
    val names = arrayOf("leavesC", "leavesc", "叶")
    compute(* names)
}

fun compute(vararg name: String) {
    name.forEach { println(it) }
}
```

#### 4、局部函数

kotlin 支持在函数中嵌套函数，被嵌套的函数称为局部函数

```kotlin
fun main() {
    compute("leavesC", "country")
}

fun compute(name: String, country: String) {
    fun check(string: String) {
        if (string.isEmpty()) {
            throw IllegalArgumentException("参数错误")
        }
    }
    check(name)
    check(country)
}
```

### 五、表达式和条件循环

#### 1、语句和表达式

这里需要先区分“语句”和“表达式”这两个概念。语句是可以单独执行，能够产生实际效果的代码，表现为赋值逻辑、打印操作、流程控制等形式，Java 中的流程控制（if，while，for）等都是语句。表达式可以是一个值、变量、常量、操作符、或它们之间的组合，表达式可以看做是包含返回值的语句

例如，以下的赋值操作、流程控制、打印输出都是语句，其是作为一个整体存在的，且不包含返回值

```kotlin
    val a = 10
    for (i in 0..a step 2) {
        println(i)
    }
```

再看几个表达式的例子

```kotlin
1       //字面表达式，返回 1

++1     //也属于表达式，自增会返回 2

//与 Java 不同，kotlin 中的 if 是作为表达式存在的，其可以拥有返回值
fun getLength(str: String?): Int {
    return if (str.isNullOrBlank()) 0 else str.length
}
```

#### 2、If 表达式

if 的分支可以是代码块，最后的表达式作为该块的返回值

```kotlin
    val maxValue = if (20 > 10) {
        println("maxValue is 20")
        20
    } else {
        println("maxValue is 10")
        10
    }
    println(maxValue) //20
```

以下代码可以显示地看出 if 的返回值，完全可以用来替代 Java 中的**三元运算符**，因此 kotlin 并没有**三元运算符**

```kotlin
    val list = listOf(1, 4, 10, 4, 10, 30)
    val value = if (list.size > 0) list.size else null
    println(value)  //6
```

如果 if 表达式分支是用于执行某个命令，那么此时的返回值类型就是 Unit ，此时的 if 语句就看起来和 Java 的一样了

```kotlin
    val value1 = if (list.size > 0) println("1") else println("2")
    println(value1.javaClass)   //class kotlin.Unit
```

如果将 if 作为表达式而不是语句（例如：返回它的值或者把它赋给变量），该表达式需要有 else 分支

#### 3、when 表达式

when 表达式与 Java 中的 **switch/case** 类似，但是要强大得多。when 既可以被当做表达式使用也可以被当做语句使用，when 将参数和所有的分支条件顺序比较直到某个分支满足条件，然后它会运行右边的表达式。如果 when 被当做表达式来使用，符合条件的分支的值就是整个表达式的值，如果当做语句使用， 则忽略个别分支的值。与 Java 的 switch/case 不同之处是 when 表达式的参数可以是任何类型，并且分支也可以是一个条件

和 if 一样，when 表达式每一个分支可以是一个代码块，它的值是代码块中最后的表达式的值，如果其它分支都不满足条件将会求值于 else 分支

如果 when 作为一个表达式使用，则必须有 else 分支， 除非编译器能够检测出所有的可能情况都已经覆盖了。如果很多分支需要用相同的方式处理，则可以把多个分支条件放在一起，用逗号分隔

```kotlin
    val value = 2
    when (value) {
        in 4..9 -> println("in 4..9") //区间判断
        3 -> println("value is 3")    //相等性判断
        2, 6 -> println("value is 2 or 6")    //多值相等性判断
        is Int -> println("is Int")   //类型判断
        else -> println("else")       //如果以上条件都不满足，则执行 else
    }
```

```kotlin
fun main() {
    //返回 when 表达式
    fun parser(obj: Any): String =
            when (obj) {
                1 -> "value is 1"
                "4" -> "value is string 4"
                is Long -> "value type is long"
                else -> "unknown"
            }
    println(parser(1))
    println(parser(1L))
    println(parser("4"))
    println(parser(100L))
    println(parser(100.0))
}

value is 1
value type is long
value is string 4
value type is long
unknown
```

此外，when 语句也可以不带参数来使用

```kotlin
    when {
        1 > 5 -> println("1 > 5")
        3 > 1 -> println("3 > 1")
    }
```

#### 4、for 循环

和 Java 中的 for 循环最为类似的形式是

```kotlin
    val list = listOf(1, 4, 10, 34, 10)
    for (value in list) {
        println(value)
    }
```

通过索引来遍历

```kotlin
    val items = listOf("H", "e", "l", "l", "o")
    //通过索引来遍历List
    for (index in items.indices) {
        println("${index}对应的值是：${items[index]}")
    }
```

也可以在每次循环中获取当前索引和相应的值

```kotlin
    val list = listOf(1, 4, 10, 34, 10)
    for ((index, value) in list.withIndex()) {
        println("index : $index , value :$value")
    }
```

也可以自定义循环区间

```kotlin
    for (index in 2..10) {
        println(index)
    }
```

#### 5、while 和 do/while 循环

while 和 do/while 与 Java 中的区别不大

```kotlin
    val list = listOf(1, 4, 15, 2, 4, 10, 0, 9)
    var index = 0
    while (index < list.size) {
        println(list[index])
        index++
    }
```

```kotlin
    val list = listOf(1, 4, 15, 2, 4, 10, 0, 9)
    var index = 0
    do {
        println(list[index])
        index++
    } while (index < list.size)
```

#### 6、返回和跳转

kotlin 有三种结构化跳转表达式：

- return 默认从最直接包围它的函数或者匿名函数返回
- break  终止最直接包围它的循环
- continue  继续下一次最直接包围它的循环

在 kotlin 中任何表达式都可以用标签（label）来标记，标签的格式为标识符后跟 @ 符号，例如：**abc@ 、fooBar@** 都是有效的标签

```kotlin
fun main() {
    fun1()
}

fun fun1() {
    val list = listOf(1, 4, 6, 8, 12, 23, 40)
    loop@ for (it in list) {
        if (it == 8) {
            continue
        }
        if (it == 23) {
            break@loop
        }
        println("value is $it")
    }
    println("function end")
}
```

```kotlin
value is 1
value is 4
value is 6
value is 12
function end
```

kotlin 有函数字面量、局部函数和对象表达式。因此 kotlin 的函数可以被嵌套

标签限制的 return 允许我们从外层函数返回，最重要的一个用途就是从 lambda 表达式中返回。通常情况下使用隐式标签更方便，该标签与接受该 lambda 的函数同名

```kotlin
fun main() {
    fun1()
    fun2()
    fun3()
    fun4()
    fun5()
}

fun fun1() {
    val list = listOf(1, 4, 6, 8, 12, 23, 40)
    list.forEach {
        if (it == 8) {
            return
        }
        println("value is $it")
    }
    println("function end")

//    value is 1
//    value is 4
//    value is 6
}

fun fun2() {
    val list = listOf(1, 4, 6, 8, 12, 23, 40)
    list.forEach {
        if (it == 8) {
            return@fun2
        }
        println("value is $it")
    }
    println("function end")

//    value is 1
//    value is 4
//    value is 6
}

//fun3() 和 fun4() 中使用的局部返回类似于在常规循环中使用 continue
fun fun3() {
    val list = listOf(1, 4, 6, 8, 12, 23, 40)
    list.forEach {
        if (it == 8) {
            return@forEach
        }
        println("value is $it")
    }
    println("function end")
    
//    value is 1
//    value is 4
//    value is 6
//    value is 12
//    value is 23
//    value is 40
//    function end
}

fun fun4() {
    val list = listOf(1, 4, 6, 8, 12, 23, 40)
    list.forEach loop@{
        if (it == 8) {
            return@loop
        }
        println("value is $it")
    }
    println("function end")

//    value is 1
//    value is 4
//    value is 6
//    value is 12
//    value is 23
//    value is 40
//    function end
}

fun fun5() {
    listOf(1, 2, 3, 4, 5).forEach(fun(value: Int) {
        if (value == 3) {
            //局部返回到匿名函数的调用者，即 forEach 循环
            return
        }
        println("value is $value")
    })
    println("function end")
}
```

### 六、区间

Ranges 表达式使用一个 **..**  操作符来声明一个闭区间，它被用于定义实现一个 RangTo 方法

以下三种声明方式都是等价的

```kotlin
    var index = 5
    
    if (index >= 0 && index <= 10) {

    }

    if (index in 0..10) {

    }

    if (index in 0.rangeTo(10)) {
        
    }
```

数字类型的 ranges 在被迭代时，编译器会将它们转换为与 Java 中使用 index 的 for 循环的相同字节码的方式来进行优化

Ranges 默认会自增长，所以像以下的代码就不会被执行

```kotlin
    for (index in 10..0) {
        println(index)
    }
```

可以改用 downTo 函数来将之改为递减

```kotlin
    for (index in 10 downTo 0) {
        println(index)
    }
```

可以在 ranges 中使用 step 来定义每次循环递增或递增的长度：

```kotlin
    for (index in 1..8 step 2){
        println(index)
    }
    for (index in 8 downTo 1 step 2) {
        println(index)
    }
```

以上声明的都是闭区间，如果想声明的是开区间，可以使用 until 函数：

```kotlin
    for (index in 0 until 4){
        println(index)
    }
```

扩展函数 `reversed()` 可用于返回将区间反转后的序列

```kotlin
    val rangeTo = 1.rangeTo(3)
    for (i in rangeTo) {
        println(i) //1  2  3
    }
    for (i in rangeTo.reversed()) {
        println(i) //3  2  1
    }
```

### 七、修饰符

#### 1、final 和  open

kotlin 中的类和方法默认都是 final 的，即不可继承的，如果想允许创建一个类的子类，需要使用 open 修饰符来标识这个类，此外，也需要为每一个希望被重写的属性和方法添加 open 修饰符

```kotlin
open class View {
    open fun click() {

    }
	//不能在子类中被重写
    fun longClick() {

    }
}

class Button : View() {
    override fun click() {
        super.click()
    }
}
```

如果重写了一个基类或者接口的成员，重写了的成员同样默认是 open 的。例如，如果 Button 类是 open 的，则其子类也可以重写其 click() 方法

```kotlin
open class Button : View() {
    override fun click() {
        super.click()
    }
}

class CompatButton : Button() {
    override fun click() {
        super.click()
    }
}
```

如果想要类的子类重写该方法的实现，可以显式地将重写的成员标记为 final

```kotlin
open class Button : View() {
    override final fun click() {
        super.click()
    }
}
```

#### 2、public

public 修饰符是限制级最低的修饰符，是默认的修饰符。如果一个定义为 public  的成员被包含在一个 private  修饰的类中，那么这个成员在这个类以外也是不可见的

#### 3、protected

protected 修饰符只能被用在类或者接口中的成员上。在 Java 中，可以从同一个包中访问一个 protected 的成员，但对于 kotlin 来说，protected 成员只在该类和它的子类中可见。此外，protected 不适用于顶层声明

#### 4、internal

一个定义为 internal 的包成员，对其所在的整个 module 可见，但对于其它 module 而言就是不可见的了。例如，假设我们想要发布一个开源库，库中包含某个类，我们希望这个类对于库本身是全局可见的，但对于外部使用者来说它不能被引用到，此时就可以选择将其声明为 internal 的来实现这个目的

> 根据 Jetbrains 的定义，一个 module  应该是一个单独的功能性的单位，可以看做是一起编译的 kotlin 文件的集合，它应该是可以被单独编译、运行、测试、debug 的。相当于在 Android Studio 中主工程引用的 module，Eclipse 中在一个 workspace 中的不同的 project

#### 5、private

private  修饰符是限制级最高的修饰符，kotlin 允许在顶层声明中使用 private 可见性，包括类、函数和属性，这表示只在自己所在的文件中可见，所以如果将一个类声明为 private，就不能在定义这个类之外的地方中使用它。此外，如果在一个类里面使用了 private  修饰符，那访问权限就被限制在这个类里面，继承这个类的子类也不能使用它。所以如果类、对象、接口等被定义为 private，那么它们只对被定义所在的文件可见。如果被定义在了类或者接口中，那它们只对这个类或者接口可见

#### 6、总结

| 修饰符         | 类成员       | 顶层声明     |
| -------------- | ------------ | ------------ |
| public（默认） | 所有地方可见 | 所有地方可见 |
| internal       | 模块中可见   | 模块中可见   |
| protected      | 子类中可见   |              |
| private        | 类中可见     | 文件中可见   |

### 八、空安全

#### 1、可空性

在 kotlin 中，类型系统将一个引用分为可以容纳  null （可空引用）或者不能容纳 null（非空引用）两种类型。 例如，String 类型的常规变量不能指向 null 

```kotlin
    var name: String = "leavesC"
    //编译错误
    //name = null
```

如果希望一个变量可以储存 null 引用，需要显式地在类型名称后面加上问号

```kotlin
    var name: String? = "leavesC"
    name = null
```

问号可以加在任何类型的后面来表示这个类型的变量可以存储 null 引用：`Int?、Doubld? 、Long?` 等

kotlin 对可空类型的显式支持有助于防止 **NullPointerException** 导致的异常问题，编译器不允许不对可空变量做 null 检查就直接调用其属性。这个强制规定使得开发者必须在编码初期就考虑好变量的可赋值范围并为其各个情况做好分支处理

```kotlin
fun check(name: String?): Boolean {
    //error，编译器不允许不对 name 做 null 检查就直接调用其属性
    return name.isNotEmpty()
}
```

正确的做法是显式地进行 null 检查

```kotlin
fun check(name: String?): Boolean {
    if (name != null) {
        return name.isNotEmpty()
    }
    return false
}
```

#### 2、安全调用运算符：?.

安全调用运算符：`?.` 允许把一次 null 检查和一次方法调用合并为一个操作，如果变量值非空，则方法或属性会被调用，否则直接返回 null

例如，以下两种写法是完全等同的：

```kotlin
fun check(name: String?) {
    if (name != null) {
        println(name.toUpperCase())
    } else {
        println(null)
    }
}

fun check(name: String?) {
    println(name?.toUpperCase())
}
```

#### 3、Elvis 运算符：?:

Elvis 运算符：`?:` 用于替代 `?.` 直接返回默认值 null 的情况，Elvis 运算符接收两个运算数，如果第一个运算数不为 null ，运算结果就是其运算结果值，如果第一个运算数为 null ，运算结果就是第二个运算数

例如，以下两种写法是完全等同的：

```kotlin
fun check(name: String?) {
    if (name != null) {
        println(name)
    } else {
        println("default")
    }
}

fun check(name: String?) {
    println(name ?: "default")
}
```

#### 4、安全转换运算符：as?

安全转换运算符：`as?` 用于把值转换为指定的类型，如果值不适合该类型则返回 null

```kotlin
fun check(any: Any?) {
    val result = any as? String
    println(result ?: println("is not String"))
}
```

#### 5、非空断言：!!

非空断言用于把任何值转换为非空类型，如果对 null 值做非空断言，则会抛出异常

```kotlin
fun main() {
    var name: String? = "leavesC"
    check(name) //7

    name = null
    check(name) //kotlinNullPointerException
}

fun check(name: String?) {
    println(name!!.length)
}
```

#### 6、可空类型的扩展

为可空类型定义扩展函数是一种更强大的处理 null 值的方式，可以允许接收者为 null 的调用，并在该函数中处理 null ，而不是在确保变量不为 null 之后再调用它的方法

例如，如下方法可以被正常调用而不会发生空指针异常

```kotlin
    val name: String? = null
    println(name.isNullOrEmpty()) //true
```

`isNullOrEmpty()` 的方法签名如下所示，可以看到这是为可空类型 **CharSequence?** 定义的扩展函数，方法中已经处理了方法调用者为 null 的情况

```kotlin
@kotlin.internal.InlineOnly
public inline fun CharSequence?.isNullOrEmpty(): Boolean {
    contract {
        returns(false) implies (this@isNullOrEmpty != null)
    }
    return this == null || this.length == 0
}
```

#### 7、平台类型

平台类型是 kotlin 对 java 所作的一种平衡性设计。kotlin 将对象的类型分为了可空类型和不可空类型两种，但 java 平台的一切对象类型均为可空的，当在 kotlin 中引用 java 变量时，如果将所有变量均归为可空类型，最终将多出许多 null 检查；如果均看成不可空类型，那么就很容易就写出忽略了NPE 风险的代码。为了平衡两者，kotlin 引入了平台类型，即当在 kotlin 中引用 java 变量值时，既可以将之看成可空类型，也可以将之看成不可空类型，由开发者自己来决定是否进行 null 检查

### 九、类型的检查与转换

#### 1、类型检查

**is 与 !is** 操作符用于在运行时检查对象是否符合给定类型：

```kotlin
fun main() {
    val strValue = "leavesC"
    parserType(strValue) //value is String , length : 7
    val intValue = 100
    parserType(intValue) //value is Int , toLong : 100
    val doubleValue = 100.22
    parserType(doubleValue) //value !is Long
    val longValue = 200L
    parserType(longValue) //unknown
}

fun parserType(value: Any) {
    when (value) {
        is String -> println("value is String , length : ${value.length}")
        is Int -> println("value is Int , toLong : ${value.toLong()}")
        !is Long -> println("value !is Long")
        else -> println("unknown")
    }
}
```

同时，is 操作符也附带了一个智能转换功能。在许多情况下，不需要在 kotlin 中使用显式转换操作符，因为编译器跟踪不可变值的 is 检查以及显式转换，并在需要时自动插入安全的转换

例如，在上面的例子中，当判断 value 为 String 类型通过时，就可以直接将 value 当做 String 类型变量并调用其内部属性，这个过程就叫做智能转换

编译器会指定根据上下文环境，将变量智能转换为合适的类型

```kotlin
    if (value !is String) return
    //如果 value 非 String 类型时直接被 return 了，所以此处可以直接访问其 length 属性
    println(value.length)

    // || 右侧的 value 被自动隐式转换为字符串，所以可以直接访问其 length 属性
    if (value !is String || value.length > 0) {

    }

    // && 右侧的 value 被自动隐式转换为字符串，所以可以直接访问其 length 属性
    if (value is String && value.length > 0) {

    }
```

#### 2、不安全的转换操作符

如果转换是不可能的，转换操作符 `as` 会抛出一个异常。因此，我们称之为不安全的转换操作符

```kotlin
fun main() {
    parserType("leavesC") //value is String , length is 7
    parserType(10) //会抛出异常 ClassCastException
}

fun parserType(value: Any) {
    val tempValue = value as String
    println("value is String , length is ${tempValue.length}")
}
```

需要注意的是，null 不能转换为 String 变量，因为该类型**不是可空的**

因此如下转换会抛出异常

```kotlin
    val x = null
    val y: String = x as String //会抛出异常 TypeCastException
```

为了匹配安全，可以转换的类型声明为可空类型

```kotlin
    val x = null
    val y: String? = x as String?
```

#### 3、安全的转换操作符

可以使用安全转换操作符 as? 来避免在转换时抛出异常，它在失败时返回 null

```kotlin
    val x = null
    val y: String? = x as? String
```

尽管以上例子 as? 的右边是一个非空类型的 String，但是其转换的结果是可空的

### 十、类

#### 1、基本概念

类的概念就是把数据和处理数据的代码封装成一个单一的实体。在 Java 中，数据存储在一个私有字段中，通过提供访问器方法：**getter 和 setter** 来访问或者修改数据

在 Java 中以下的示例代码是很常见的，Point 类包含很多重复的代码：通过构造函数把参数赋值给有着相同名称的字段，通过 getter 来获取属性值

```java
public final class Point {

   private final int x;
   
   private final int y;
   
   public Point(int x, int y) {
      this.x = x;
      this.y = y;
   }

   public final int getX() {
      return this.x;
   }

   public final int getY() {
      return this.y;
   }
   
}
```

使用 kotlin 来声明 Point 类则只需要一行代码，两者完全等同

```kotlin
class Point(val x: Int, val y: Int)
```

kotlin 也使用关键字 **class** 来声明类，类声明由类名、类头（指定其类型参数、主构造函数等）以及由花括号包围的类体构成，类头与类体都是可选的，如果一个类没有类体，可以省略花括号。此外，kotlin 中类默认是 **publish（公有的） 且 final （不可继承）的**

kotlin 区分了**主构造方法（在类体外部声明）和次构造方法（在类体内部声明）**，一个类可以有一个主构造函数和多个次构造函数，此外也允许在初始化代码块中 `init` 添加额外的初始化逻辑

#### 2、主构造函数

主构造函数是类头的一部分，跟在类名（和可选的类型参数）后，主构造函数的参数可以是可变的（var）或只读的（val）

```kotlin
class Point constructor(val x: Int, val y: Int) {

}
```

如果主构造函数没有任何注解或者可见性修饰符，可以省略 constructor 关键字

```kotlin
class Point(val x: Int, val y: Int) {

}

//如果不包含类体，则可以省略花括号
class Point(val x: Int, val y: Int)
```

如果构造函数有注解或可见性修饰符，则 constructor 关键字是必需的，并且这些修饰符在它前面

```kotlin
class Point public @Inject constructor(val x: Int, val y: Int) {

}
```

主构造函数不能包含任何的代码，初始化的代码可以放到以 init 关键字作为前缀的初始化块（initializer blocks）中，初始化块包含了在类被创建时执行的代码，主构造函数的参数可以在初始化块中使用。如果需要的话，也可以在一个类中声明多个初始化语句块。需要注意的是，构造函数的参数如果用 val/var 进行修饰，则相当于在类内部声明了一个同名的全局属性。如果不加 val/var 进行修饰，则构造函数只能在 init 函数块和全局属性初始化时进行引用

此外，要创建一个类的实例不需要使用 Java 中的 new 关键字，像普通函数一样调用构造函数即可

```kotlin
class Point(val x: Int, val y: Int) {

    init {
        println("initializer blocks , x value is: $x , y value is: $y")
    }

}

fun main() {
    Point(1, 2) // initializer blocks , x value is: 1 , y value is: 2
}
```

主构造函数的参数也可以在类体内声明的属性初始化器中使用

```kotlin
class Point(val x: Int, val y: Int) {

    private val localX = x + 1

    private val localY = y + 1

    init {
        println("initializer blocks , x value is: $x , y value is: $y")
        println("initializer blocks , localX value is: $localX , localY value is: $localY")
    }

}

fun main() {
    Point(1, 2)
    //initializer blocks , x value is: 1 , y value is: 2
    //initializer blocks , localX value is: 2 , localY value is: 3
}
```

#### 3、次构造函数

类也可以声明包含前缀 constructor 的次构造函数。如果类有一个主构造函数，每个次构造函数都需要直接委托给主构造函数或者委托给另一个次构造函数以此进行间接委托，用 this 关键字来进行指定即可

```kotlin
class Point(val x: Int, val y: Int) {

    private val localX = x + 1

    private val localY = y + 1

    init {
        println("initializer blocks , x value is: $x , y value is: $y")
        println("initializer blocks , localX value is: $localX , localY value is: $localY")
    }

    constructor(base: Int) : this(base + 1, base + 1) {
        println("constructor(base: Int)")
    }

    constructor(base: Long) : this(base.toInt()) {
        println("constructor(base: Long)")
    }

}

fun main() {
    Point(100)
    //initializer blocks , x value is: 101 , y value is: 101
    //initializer blocks , localX value is: 102 , localY value is: 102
    //constructor(base: Int)
    Point(100L)
    //initializer blocks , x value is: 101 , y value is: 101
    //initializer blocks , localX value is: 102 , localY value is: 102
    //constructor(base: Int)
    //constructor(base: Long)
}
```

初始化块中的代码实际上会成为主构造函数的一部分，委托给主构造函数会作为次构造函数的第一条语句，因此所有初始化块中的代码都会在次构造函数体之前执行。即使该类没有主构造函数，这种委托仍会隐式发生，并且仍会执行初始化块。如果一个非抽象类没有声明任何（主或次）构造函数，会默认生成一个不带参数的公有主构造函数

#### 4、属性

在 Java 中，字段和其访问器的组合被称作属性。在 kotlin 中，属性是头等的语言特性，完全替代了字段和访问器方法。在类中声明一个属性和声明一个变量一样是使用 val 和 var 关键字。val 变量只有一个 getter ，var 变量既有 getter 也有 setter

```kotlin
fun main() {
    val user = User()
    println(user.name)
    user.age = 200
}

class User() {

    val name: String = "leavesC"

    var age: Int = 25

}
```

#### 5、自定义访问器

访问器的默认实现逻辑很简单：创建一个存储值的字段，以及返回属性值的 getter 和更新属性值的 setter。如果需要的话，也可以自定义访问器

例如，以下就声明了三个带自定义访问器的属性

```kotlin
class Point(val x: Int, val y: Int) {

    val isEquals1: Boolean
        get() {
            return x == y
        }

    val isEquals2
        get() = x == y

    var isEquals3 = false
        get() = x > y
        set(value) {
            field = !value
        }

}
```

如果仅需要改变一个访问器的可见性或者为其添加注解，那么可以定义访问器而不定义其实现

```kotlin
fun main() {
    val point = Point(10, 10)
    println(point.isEquals1)
    //以下代码会报错
    //point.isEquals1 = true
}

class Point(val x: Int, val y: Int) {

    var isEquals1: Boolean = false
        get() {
            return x == y
        }
        private set
    
}
```

#### 6、延迟初始化

一般地，非空类型的属性必须在构造函数中初始化，但像使用了 Dagger2 这种依赖注入框架的项目来说就十分的不方便了，为了应对这种情况，可以用 lateinit 修饰符来标记该属性，用于告诉编译器该属性会在稍后的时间被初始化

用 lateinit 修饰的属性或变量必须为非空类型，并且不能是原生类型

```kotlin
class Point(val x: Int, val y: Int)

class Example {

    lateinit var point: Point

    var point2: Point

    constructor() {
        point2 = Point(10, 20)
    }
    
}
```

如果访问了一个未经过初始化的 lateinit 变量，则会抛出一个包含具体原因（该变量未初始化）的异常信息

```kotlin
Exception in thread "main" kotlin.UninitializedPropertyAccessException: lateinit property point has not been initialized
```

### 十一、类的分类

#### 1、抽象类

声明为 abstract 的类内部可以包含没有实现体的成员方法，且该成员方法也用 abstract 标记，这种类称为抽象类，没有实现体的方法就称为抽象方法

此外，我们并不需要用 open 标注一个抽象类或者抽象方法，因为这是默认声明的

```kotlin
abstract class BaseClass {
    abstract fun fun1()
}

class Derived : BaseClass() {
    override fun fun1() {
        
    }
}
```

#### 2、数据类

数据类是一种非常强大的类，可以避免重复创建 Java 中的用于保存状态但又操作非常简单的 POJO 的模版代码，它们通常只提供了用于访问它们属性的简单的 getter 和 setter

定义一个新的数据类非常简单，例如

```kotlin
data class Point(val x: Int, val y: Int)
```

数据类默认地为主构造函数中声明的所有属性生成了如下几个方法

- getter、setter（需要是 var）
- componentN()。按主构造函数的属性声明顺序进行对应
- copy() 
- toString()
- hashCode()
- equals()

为了确保生成的代码的一致性以及有意义的行为，数据类必须满足以下要求：

- 主构造函数需要包含一个参数
- 主构造函数的所有参数需要标记为 val 或 var
- 数据类不能是抽象、开放、密封或者内部的

可以利用 IDEA 来反编译查看 Point 类的 Java 实现，了解其内部实现

```java
public final class Point {
   private final int x;
   private final int y;

   public final int getX() {
      return this.x;
   }

   public final int getY() {
      return this.y;
   }

   public Point(int x, int y) {
      this.x = x;
      this.y = y;
   }

   public final int component1() {
      return this.x;
   }

   public final int component2() {
      return this.y;
   }

   @NotNull
   public final Point copy(int x, int y) {
      return new Point(x, y);
   }

   // $FF: synthetic method
   // $FF: bridge method
   @NotNull
   public static Point copy$default(Point var0, int var1, int var2, int var3, Object var4) {
      if ((var3 & 1) != 0) {
         var1 = var0.x;
      }

      if ((var3 & 2) != 0) {
         var2 = var0.y;
      }

      return var0.copy(var1, var2);
   }

   public String toString() {
      return "Point(x=" + this.x + ", y=" + this.y + ")";
   }

   public int hashCode() {
      return this.x * 31 + this.y;
   }

   public boolean equals(Object var1) {
      if (this != var1) {
         if (var1 instanceof Point) {
            Point var2 = (Point)var1;
            if (this.x == var2.x && this.y == var2.y) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }
}
```

通过数据类可以简化很多的通用操作，可以很方便地进行：格式化输出变量值、映射对象到变量、对比变量之间的相等性、复制变量等操作

```kotlin
fun main() {
    val point1 = Point(10, 20)
    val point2 = Point(10, 20)
    println("point1 toString() : $point1") //point1 toString() : Point(x=10, y=20)
    println("point2 toString() : $point2") //point2 toString() : Point(x=10, y=20)

    val (x, y) = point1
    println("point1 x is $x,point1 y is $y") //point1 x is 10,point1 y is 20

    //在 kotlin 中，“ == ” 相当于 Java 的 equals 方法
    //而 “ === ” 相当于 Java 的 “ == ” 方法
    println("point1 == point2 : ${point1 == point2}") //point1 == point2 : true
    println("point1 === point2 : ${point1 === point2}") //point1 === point2 : false

    val point3 = point1.copy(y = 30)
    println("point3 toString() : $point3") //point3 toString() : Point(x=10, y=30)
}
```

需要注意的是，数据类的 `toString()、equals()、hashCode()、copy()` 等方法只考虑主构造函数中声明的属性，因此在比较两个数据类对象的时候可能会有一些意想不到的结果

```kotlin
data class Point(val x: Int) {

    var y: Int = 0

}

fun main() {
    val point1 = Point(10)
    point1.y = 10

    val point2 = Point(10)
    point2.y = 20

    println("point1 == point2 : ${point1 == point2}") //point1 == point2 : true
    println("point1 === point2 : ${point1 === point2}") //point1 === point2 : false
}
```

#### 3、密封类

Sealed 类（密封类）用于对类可能创建的子类进行限制，用 Sealed 修饰的类的**直接子类**只允许被定义在 Sealed 类所在的文件中（密封类的间接继承者可以定义在其他文件中），这有助于帮助开发者掌握父类与子类之间的变动关系，避免由于代码更迭导致的潜在 bug，且密封类的构造函数只能是 private 的

例如，对于 View 类，其子类只能定义在与之同一个文件里，Sealed 修饰符修饰的类也隐含表示该类为 open 类，因此无需再显式地添加 open 修饰符

```kotlin
sealed class View {

    fun click() {

    }

}

class Button : View() {

}

class TextView : View() {

}
```

因为 Sealed 类的子类对于编译器来说是可控的，所以如果在 when 表达式中处理了所有 Sealed 类的子类，那就不需要再提供 else 默认分支。即使以后由于业务变动又新增了 View 子类，编译器也会检测到 check 方法缺少分支检查后报错，所以说 check 方法是类型安全的

```kotlin
fun check(view: View): Boolean {
    when (view) {
        is Button -> {
            println("is Button")
            return true
        }
        is TextView -> {
            println("is TextView")
            return true
        }
    }
}
```

#### 4、枚举类

kotlin 也提供了枚举的实现，相比 Java 需要多使用 class 关键字来声明枚举

```kotlin
enum class Day {
    SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY
}
```

枚举可以声明一些参数

```kotlin
enum class Day(val index: Int) {
    SUNDAY(0), MONDAY(1), TUESDAY(2), WEDNESDAY(3), THURSDAY(4), FRIDAY(5), SATURDAY(6)
}
```

此外，枚举也可以实现接口

```kotlin
interface OnChangedListener {

    fun onChanged()

}

enum class Day(val index: Int) : OnChangedListener {
    SUNDAY(0) {
        override fun onChanged() {

        }
    },
    MONDAY(1) {
        override fun onChanged() {
            
        }
    }
}
```

枚举也包含有一些共有函数

```kotlin
fun main() {
    val day = Day.FRIDAY
    //获取值
    val value = day.index  //5
    //通过 String 获取相应的枚举值
    val value1 = Day.valueOf("SUNDAY") //SUNDAY
    //获取包含所有枚举值的数组
    val value2 = Day.values()
    //获取枚举名
    val value3 = Day.SUNDAY.name //SUNDAY
    //获取枚举声明的位置
    val value4 = Day.TUESDAY.ordinal //2
}
```

#### 5、嵌套类

在 kotlin 中在类里面再定义的类默认是嵌套类，此时嵌套类不会包含对外部类的隐式引用

```kotlin
class Outer {

    private val bar = 1

    class Nested {
        fun foo1() = 2
        //错误
        //fun foo2() = bar
    }
}

fun main() {
    val demo = Outer.Nested().foo1()
}
```

以上代码通过 IDEA 反编译后可以看到其内部的 Java 实现方式

可以看到 Nested 其实就是一个静态类，因此 foo2() 不能访问外部类的非静态成员，也不用先声明 Outer 变量再指向 Nested 类，而是直接通过 Outer  类指向 Nested 类

```kotlin
public final class Outer {
   private final int bar = 1;

   public static final class Nested {
      public final int foo1() {
         return 2;
      }
   }
}

public final class MainkotlinKt {
   public static final void main(@NotNull String[] args) {
      Intrinsics.checkParameterIsNotNull(args, "args");
      int demo = (new Outer.Nested()).foo1();
   }
}
```

#### 6、内部类

如果需要去访问外部类的成员，需要用 inner 修饰符来标注被嵌套的类，这称为内部类。内部类会隐式持有对外部类的引用

```kotlin
class Outer {

    private val bar = 1

    inner class Nested {
        fun foo1() = 2
        fun foo2() = bar
    }
}

fun main() {
    val demo = Outer().Nested().foo2()
}
```

再来看其内部的 Java 实现方式

使用 inner 来声明 Nested 类后，就相当于将之声明为非静态内部类，因此 foo2() 能访问其外部类的非静态成员，在声明 Nested 变量前也需要通过 Outer 变量来指向其内部的 Nested 类

```kotlin
public final class Outer {
   private final int bar = 1;

   public final class Nested {
      public final int foo1() {
         return 2;
      }

      public final int foo2() {
         return Outer.this.bar;
      }
   }
}

public final class MainkotlinKt {
   public static final void main(@NotNull String[] args) {
      Intrinsics.checkParameterIsNotNull(args, "args");
      int demo = (new Outer().new Nested()).foo2();
   }
}
```

| 类A在类B中声明               | 在Java中       | 在kotlin中    |
| ---------------------------- | -------------- | ------------- |
| 嵌套类（不存储外部类的引用） | static class A | class A       |
| 内部类（存储外部类的引用）   | class A        | inner class A |

#### 7、匿名内部类

可以使用**对象表达式**来创建匿名内部类实例

```kotlin
interface OnClickListener {

    fun onClick()

}

class View {

    fun setClickListener(clickListener: OnClickListener) {

    }

}

fun main() {
    val view = View()
    view.setClickListener(object : OnClickListener {
        override fun onClick() {

        }

    })
}
```

#### 8、内联类

在有些时候，我们需要对原生类型进行包装以便提升程序的健壮性。例如，对于 `sendEmail` 方法的入参参数而言，我们无法严格限制入参参数的含义类型，有的开发者可能会将 delay 理解为以毫秒为单位，有的开发者可能又会理解为以分钟为单位

```kotlin
fun sendEmail(delay: Long) {
    println(delay)
}
```

为了提升程序的健壮性，我们可以通过声明一个包装类来作为参数类型：

```kotlin
fun sendEmail(delay: Time) {
    println(delay.second)
}

class Time(val second: Long)

class Minute(private val count: Int) {

    fun toTime(): Time {
        return Time(count * 60L)
    }

}

fun main() {
    sendEmail(Minute(10).toTime())
}
```

这样，在代码源头上就限制了开发者能够传入的参数类型，开发者通过类名就能直接表达出自己希望的时间大小。然而这种方式由于额外的堆内存分配问题，就引入了运行时的性能开销，新的包装类相对原生类型所需要的性能消耗要大得多，可是此时又需要考虑程序的健壮性和可读性，所以包装类也是需要的

内联类（InlineClass）就是为了解决这两者的矛盾而诞生的。上述代码可以改为以下方式来实现

```kotlin
fun sendEmail(delay: Time) {
    println(delay.second)
}

inline class Time(val second: Long)

inline class Minute(private val count: Int) {

    fun toTime(): Time {
        return Time(count * 60L)
    }

}

fun main() {
    sendEmail(Minute(10).toTime())
}
```

使用 inline 修饰的类就称为内联类，内联类必须含有唯一的一个属性在主构造函数中初始化，在运行时将使用这个唯一属性来表示内联类的实例，从而避免了包装类在运行时的额外开销

例如，通过查看字节码可以看到`sendEmail` 方法会被解释为一个以 long 类型作为入参类型的函数，并不包含任何对象

```java
   public static final void sendEmail_G1aXmDY/* $FF was: sendEmail-G1aXmDY*/(long delay) {
      boolean var4 = false;
      System.out.println(delay);
   }
```

### 十二、接口

#### 1、抽象方法与默认方法

kotlin 中的接口与 Java 8 中的类似，可以包含抽象方法的定义以及非抽象方法的实现，不需要使用 default 关键字来标注有默认实现的非抽象方法，但在实现接口的抽象方法时需要使用 override 进行标注

```kotlin
fun main() {
    val view = View()
    view.click()
    view.longClick()
}

class View : Clickable {
    
    override fun click() {
        println("clicked")
    }

}

interface Clickable {
    fun click()
    fun longClick() = println("longClicked")
}
```

如果一个类实现了多个接口，而接口包含带有默认实现且签名相同的方法，此时编译器就会要求开发者必须显式地实现该方法，可以选择在该方法中调用不同接口的相应实现

```kotlin
class View : Clickable, Clickable2 {

    override fun click() {
        println("clicked")
    }

    override fun longClick() {
        super<Clickable>.longClick()
        super<Clickable2>.longClick()
    }
}

interface Clickable {
    fun click()
    fun longClick() = println("longClicked")
}

interface Clickable2 {
    fun click()
    fun longClick() = println("longClicked2")
}
```

#### 2、抽象属性

接口中可以包含抽象属性声明，接口不定义该抽象属性是应该存储到一个支持字段还是通过 getter 来获取，接口本身并不包含任何状态，因此只有实现这个接口的类在需要的情况下会存储这个值

看以下例子，Button 类和 TextView 类都实现了 Clickable 接口，并都提供了取得 statusValue 值的方式

Button 类提供了一个自定义的 getter 用于在每次访问时重新获取 statusValue 值，因此在多次获取属性值时其值可能都会不一致，因为每次 getRandom() 方法都会被调用

TextView 类中的 statusValue 属性有一个支持字段来存储在类初始化时得到的数据，因此其值在初始化后是不会再次获取值，即 TextView 类中的 getRandom() 只会被调用一次

```kotlin
fun main() {
    val button = Button()
    println(button.statusValue)
    val textView = TextView()
    println(textView.statusValue)
}

class Button : Clickable {

    override val statusValue: Int
        get() = getRandom()

    private fun getRandom() = Random().nextInt(10)

}

class TextView : Clickable {

    override val statusValue: Int = getRandom()

    private fun getRandom() = Random().nextInt(10)

}

interface Clickable {

    val statusValue: Int

}
```

除了可以声明抽象属性外，接口还可以包含具有 getter 和 setter 的属性，只要它们没有引用一个支持字段（支持字段需要在接口中存储状态，而这是不允许的）

```kotlin
interface Clickable {

    val statusValue: Int

    val check: Boolean
        get() = statusValue > 10
    
}
```

### 十三、SAM 接口

对于以下例子，在 Kotlin 1.4 之前第二种写法是不支持的，我们必须完全实现 SelfRunnable 才可以调用 `setRunnable` 方法

```kotlin
/**
 * 作者：leavesC
 * 时间：2020/10/6 21:28
 * 描述：
 * GitHub：https://github.com/leavesC
 */
interface SelfRunnable {

    fun run()

}

fun setRunnable(selfRunnable: SelfRunnable) {
    selfRunnable.run()
}

fun main() {
    setRunnable(object : SelfRunnable {
        override fun run() {
            println("hello,leavesC")
        }
    })
    //错误，Kotlin 1.4 之前不支持
//    setRunnable {
//        println("hello,leavesC")
//    }
}
```

而在 Kotlin 1.4 之后，Kotlin 开始支持 **SAM 转换**。只有一个抽象方法的接口称为**函数式接口**或 **SAM（单一抽象方法）接口**，函数式接口可以有多个非抽象成员，但只能有一个抽象成员。SAM 转换即 `Single Abstract Method Conversions`，对于只有单个非默认抽象方法的接口，可以直接用 Lambda 来表示，前提是 Lambda 所表示的函数类型能够跟接口中的方法签名相匹配

所以，在 Kotlin 1.4 之后，就支持直接以 Lambda 的方式来声明 SelfRunnable 的实现类，从而使得在方法调用上可以更加简洁，但这也要求 interface 同时使用 fun 关键字修饰

```kotlin
/**
 * 作者：leavesC
 * 时间：2020/10/6 21:28
 * 描述：
 * GitHub：https://github.com/leavesC
 */
fun interface SelfRunnable {

    fun run()

}

fun setRunnable(selfRunnable: SelfRunnable) {
    selfRunnable.run()
}

fun main() {
    setRunnable {
        println("hello,leavesC")
    }
}
```

### 十四、继承

在 kotlin 中所有类都有一个共同的超类 **Any** ，对于没有超类声明的类来说它就是默认超类。需要注意的是， Any  并不是  **java.lang.Object**  ，它除了  **equals()  、 hashCode()  与 toString()**  外没有其他属性或者函数

要声明一个显式的超类，需要把父类名放到类头的冒号之后

```kotlin
open class Base()

class SubClass() : Base()
```

当中，类上的 open 标注与 Java 中的 final 含义相反，用于允许其它类从这个类继承。默认情况下，kotlin 中所有的类都是 final

如果派生类有一个主构造函数，其基类型必须直接或间接调用基类的主构造函数

```kotlin
open class Base(val str: String)

class SubClass(val strValue: String) : Base(strValue)

class SubClass2 : Base {

    constructor(strValue: String) : super(strValue)

    constructor(intValue: Int) : super(intValue.toString())

    constructor(doubValue: Double) : this(doubValue.toString())

}
```

#### 1、覆盖方法

与 Java 不同，kotlin 需要显式标注可覆盖的成员和覆盖后的成员：

```kotlin
open class Base() {
    open fun fun1() {

    }

    fun fun2() {
        
    }
}

class SubClass() : Base() {
    override fun fun1() {
        super.fun1()
    }
}
```

用 open 标注的函数才可以被子类重载，子类用 override 表示该函数是要对父类的同签名函数进行覆盖。标记为 override 的成员本身也是开放的，也就是说，它可以被子类覆盖。如果想禁止再次覆盖，可以使用 final 关键字标记
如果父类没有使用 open 对函数进行标注，则子类不允许定义相同签名的函数。对于一个 final 类（没有用 open 标注的类）来说，使用 open 标记属性和方法是无意义的

#### 2、属性覆盖

属性覆盖与方法覆盖类似。在超类中声明为 open 的属性，如果要进行覆盖则必须在派生类中重新声明且以 override 开头，并且它们必须具有兼容的类型

每个声明的属性可以由具有初始化器的属性或者具有 getter 方法的属性覆盖

```kotlin
open class Base {
    open val x = 10

    open val y: Int
        get() {
            return 100
        }
}

class SubClass : Base() {
    override val x = 100

    override var y = 200
}

fun main() {
    val base = Base()
    println(base.x) //10
    println(base.y) //100

    val base1: Base = SubClass()
    println(base1.x) //100
    println(base1.y) //200

    val subClass = SubClass()
    println(subClass.x) //100
    println(subClass.y) //200
}
```

此外，也可以用一个 var 属性覆盖一个 val 属性，但反之则不行。因为一个 val 属性本质上声明了一个 getter 方法，而将其覆盖为 var 只是在子类中额外声明一个 setter 方法

可以在主构造函数中使用  override  关键字作为属性声明的一部分

```kotlin
open class Base {
    open val str: String = "Base"
}

class SubClass(override val str: String) : Base()

fun main() {
    val base = Base()
    println(base.str) //Base

    val subClass = SubClass("leavesC")
    println(subClass.str) //leavesC
}
```

#### 3、调用超类实现

派生类可以通过 super 关键字调用其超类的函数与属性访问器的实现

```kotlin
open class BaseClass {
    open fun fun1() {
        println("BaseClass fun1")
    }
}

class SubClass : BaseClass() {

    override fun fun1() {
        super.fun1()
    }

}
```

对于内部类来说，其本身就可以直接调用调用外部类的函数

```kotlin
open class BaseClass2 {
    private fun fun1() {
        println("BaseClass fun1")
    }

    inner class InnerClass {
        fun fun2() {
            fun1()
        }
    }

}
```

但如果想要在一个内部类中访问外部类的超类，则需要通过由外部类名限定的 super 关键字来实现

```kotlin
open class BaseClass {
    open fun fun1() {
        println("BaseClass fun1")
    }
}

class SubClass : BaseClass() {

    override fun fun1() {
        println("SubClass fun1")
    }

    inner class InnerClass {

        fun fun2() {
            super@SubClass.fun1()
        }

    }

}

fun main() {
    val subClass = SubClass()
    val innerClass = subClass.InnerClass()
    //BaseClass fun1
    innerClass.fun2()
}
```

如果一个类从它的直接超类和实现的接口中继承了相同成员的多个实现， 则必须覆盖这个成员并提供其自己的实现来消除歧义

为了表示采用从哪个超类型继承的实现，使用由尖括号中超类型名限定的 super 来指定，如  super< BaseClass >

```kotlin
open class BaseClass {
    open fun fun1() {
        println("BaseClass fun1")
    }
}

interface BaseInterface {
    //接口成员默认就是 open 的
    fun fun1() {
        println("BaseInterface fun1")
    }
}

class SubClass() : BaseClass(), BaseInterface {
    override fun fun1() {
        //调用 SubClass 的 fun1() 函数
        super<BaseClass>.fun1()
        //调用 BaseInterface 的 fun1() 函数
        super<BaseInterface>.fun1()
    }
}
```

### 十五、集合

#### 1、只读集合与可变集合

kotlin 的集合设计和 Java 不同的另一项特性是：kotlin 把访问数据的接口和修改集合数据的接口分开了，`kotlin.collections.Collection` 接口提供了**遍历集合元素、获取集合大小、判断集合是否包含某元素**等操作，但这个接口没有提供**添加和移除元素**的方法。`kotlin.collections.MutableCollection` 接口继承于 `kotlin.collections.Collection` 接口，扩展出了用于**添加、移除、清空元素**的方法

就像 kotlin 对 `val` 和 `var` 的区分一样，只读集合接口与可变集合接口的分离能提高对代码的可控性，如果函数接收 `Collection` 作为形参，那么就可以知道该函数不会修改集合，而只是对数据进行读取

以下是用来创建不同类型集合的函数

| 集合元素 | 只读   | 可变                                              |
| -------- | ------ | ------------------------------------------------- |
| List     | listOf | mutableListOf、arrayListOf                        |
| Set      | setOf  | mutableSetOf、hashSetOf、linkedSetOf、sortedSetOf |
| Map      | mapOf  | mutableMapOf、hashMapOf、linkedMapOf、sortedMapOf |

```kotlin
    val list = listOf(10, 20, 30, 40)
    //不包含 add 方法
    //list.add(100)
    println(list.size)
    println(list.contains(20))

    val mutableList = mutableListOf("leavesC", "leavesc", "叶")
    mutableList.add("Ye")
    println(mutableList.size)
    println(mutableList.contains("leavesC"))
```

#### 2、集合与 Java

因为 Java 并不会区分只读集合与可变集合，即使 kotlin 中把集合声明为只读的， Java 代码也可以修改这个集合，而 Java 代码中的集合对 kotlin 来说也是可变性未知的，kotlin 代码可以将之视为只读的或者可变的，包含的元素也是可以为 null 或者不为 null 的

例如，在 Java 代码中 names 这么一个 List< String > 类型的变量

```java
public class JavaMain {

    public static List<String> names = new ArrayList<>();

    static {
        names.add("leavesC");
        names.add("Ye");
    }

}
```

在 kotlin 中可以用以下四种方式来引用变量 names 

```kotlin
    val list1: List<String?> = JavaMain.names

    val list2: List<String> = JavaMain.names

    val list3: MutableList<String> = JavaMain.names

    val list4: MutableList<String?> = JavaMain.names
```

#### 3、只读集合的可变性

只读集合不一定就是不可变的。例如，假设存在一个拥有只读类型接口的对象，该对象存在两个不同的引用，一个只读，一个可变，当可变引用修改了该对象后，这对只读引用来说就相当于“只读集合被修改了”，因此只读集合并不总是线程安全的。如果需要在多线程环境下处理数据，需要保证正确地同步了对数据的访问，或者使用支持并发访问的数据结构

例如，list1 和 list1 引用到同一个集合对象， list3 对集合的修改同时会影响到 list1

```kotlin
    val list1: List<String> = JavaMain.names
    val list3: MutableList<String> = JavaMain.names
    list1.forEach { it -> println(it) } //leavesC Ye
    list3.forEach { it -> println(it) } //leavesC Ye
    for (index in list3.indices) {
        list3[index] = list3[index].toUpperCase()
    }
    list1.forEach { it -> println(it) } //LEAVESC YE
```

#### 4、集合与可空性

集合的可空性可以分为三种：

1. 可以包含为 null 的集合元素
2. 集合本身可以为 null
3. 集合本身可以为 null，且可以包含为 null 的集合元素

例如，intList1 可以包含为 null 的集合元素，但集合本身不能指向 null；intList2 不可以包含为 null 的集合元素，但集合本身可以指向 null；intList3 可以包含为 null 的集合元素，且集合本身能指向 null

```kotlin
    //List<Int?> 是能持有 Int? 类型值的列表
    val intList1: List<Int?> = listOf(10, 20, 30, 40, null)
    //List<Int>? 是可以为 null 的列表
    var intList2: List<Int>? = listOf(10, 20, 30, 40)
    intList2 = null
    //List<Int?>? 是可以为 null 的列表，且能持有 Int? 类型值
    var intList3: List<Int?>? = listOf(10, 20, 30, 40, null)
    intList3 = null
```

### 十六、扩展函数和扩展属性

#### 1、扩展函数

扩展函数用于为一个类增加一种新的行为，这是为缺少有用函数的类进行扩展的途径。扩展函数的用途就类似于在 Java 中实现的静态工具方法。而在 kotlin 中使用扩展函数的一个优势就是我们不需要在调用方法的时候把整个对象当作参数传入，扩展函数表现得就像是属于这个类本身的一样，可以使用 this 关键字并直接调用其所有 public 方法

扩展函数并不允许你打破它的封装性，和在类内部定义的方法不同的是，扩展函数不能访问私有的或是受保护的成员

```kotlin
//为 String 类声明一个扩展函数 lastChar() ，用于返回字符串的最后一个字符
//get方法是 String 类的内部方法，length 是 String 类的内部成员变量，在此处可以直接调用
fun String.lastChar() = get(length - 1)

//为 Int 类声明一个扩展函数 doubleValue() ，用于返回其两倍值
//this 关键字代表了 Int 值本身
fun Int.doubleValue() = this * 2
```

之后，我们就可以像调用类本身内部声明的方法一样，直接调用扩展函数

```kotlin
fun main() {
    val name = "leavesC"
    println("$name lastChar is: " + name.lastChar())

    val age = 24
    println("$age doubleValue is: " + age.doubleValue())
}
```

如果需要声明一个静态的扩展函数，则必须将其定义在伴生对象上，这样就可以在没有 Namer 实例的情况下调用其扩展函数，就如同在调用 Java 的静态函数一样

```kotlin
class Namer {

    companion object {

        val defaultName = "mike"

    }

}

fun Namer.Companion.getName(): String {
    return defaultName
}

fun main() {
    Namer.getName()
}
```

需要注意的是，如果扩展函数声明于 class 内部，则该扩展函数只能该类和其子类内部调用，因为此时相当于声明了一个非静态函数，外部无法引用到。所以一般都是将扩展函数声明为全局函数

#### 2、扩展属性

扩展函数也可以用于属性

```kotlin
//扩展函数也可以用于属性
//为 String 类新增一个属性值 customLen
var String.customLen: Int
    get() = length
    set(value) {
        println("set")
    }

fun main() {
    val name = "leavesC"
    println(name.customLen)
    name.customLen = 10
    println(name.customLen)
    //7
    //set
    //7
}
```

#### 3、不可重写的扩展函数

看以下例子，子类 Button 重写了父类 View 的 click() 函数，此时如果声明一个 View 变量，并赋值为 Button 类型的对象，调用的 click() 函数将是 Button 类重写的方法

```kotlin
fun main() {
    val view: View = Button()
    view.click() //Button clicked
}

open class View {
    open fun click() = println("View clicked")
}

class Button : View() {
    override fun click() = println("Button clicked")
}
```

对于扩展函数来说，与以上的例子却不一样。如果基类和子类都分别定义了一个同名的扩展函数，此时要调用哪个扩展函数是由变量的静态类型来决定的，而非这个变量的运行时类型

```kotlin
fun main() {
    val view: View = Button()
    view.longClick() //View longClicked
}

open class View {
    open fun click() = println("View clicked")
}

class Button : View() {
    override fun click() = println("Button clicked")
}

fun View.longClick() = println("View longClicked")

fun Button.longClick() = println("Button longClicked")
```

此外，如果一个类的成员函数和扩展函数有相同的签名，**成员函数会被优先使用**

扩展函数并不是真正地修改了原来的类，其底层其实是以静态导入的方式来实现的。扩展函数可以被声明在任何一个文件中，因此有个通用的实践是把一系列有关的函数放在一个新建的文件里

需要注意的是，扩展函数不会自动地在整个项目范围内生效，如果需要使用到扩展函数，需要进行导入

#### 4、可空接收者

可以为可空的接收者类型定义扩展，即使接受者为 null，使得开发者在调用扩展函数前不必进行判空操作，且可以通过 `this == null` 来检查接收者是否为空

```kotlin
fun main() {
    var name: String? = null
    name.check() //this == null
    name = "leavesC"
    name.check() //this != null
}

fun String?.check() {
    if (this == null) {
        println("this == null")
        return
    }
    println("this != null")
}
```

### 十七、Lambda 表达式

Lambda 表达式本质上就是可以传递给其它函数的一小段代码，通过 Lambda 表达式可以把通用的代码结构抽取成库函数，也可以把 Lambda 表达式存储在一个变量中，把这个变量当做普通函数对待

```kotlin
    //由于存在类型推导，所以以下三种声明方式都是完全相同的
    val plus1: (Int, Int) -> Int = { x: Int, y: Int -> x + y }
    val plus2: (Int, Int) -> Int = { x, y -> x + y }
    val plus3 = { x: Int, y: Int -> x + y }
    println(plus3(1, 2))
```

1. 一个 Lambda 表达式始终用花括号包围，通过箭头把实参列表和函数体分开
2. 如果 Lambda 声明了函数类型，那么就可以省略函数体的类型声明
3. 如果 Lambda 声明了参数类型，且返回值支持类型推导，那么就可以省略函数类型声明

虽然说倾向于尽量避免让 Lambda 表达式引用外部变量以避免副作用，但有些情况下让 Lambda 引用外部变量也可以简化计算结构。访问了外部环境变量的 Lambda 表达式称之为闭包，闭包可以被当做参数传递或者直接使用。与 Java 不同，kotlin 中的闭包不仅可以访问外部变量也可以对其进行修改

例如，假设我们需要一个计算总和的方法，每次调用函数时都返回当前的总和大小。方法外部不提供保存当前总和的变量，由 Lambda 表达式内部进行存储

```kotlin
fun main() {
    val sum = sumFunc()
    println(sum(10)) //10
    println(sum(20)) //30
    println(sum(30)) //60
}

fun sumFunc(): (Int) -> Int {
    var base = 0
    return fun(va: Int): Int {
        base += va
        return base
    }
}
```

此外，kotlin 也支持一种自动运行的语法

```kotlin
{ va1: Int, va2: Int -> println(va1 + va2) }(10, 20)
```

Lambda 表达式最常见的用途就是和集合一起工作，看以下例子

要从一个人员列表中取出年龄最大的一位

```kotlin
data class Person(val name: String, val age: Int)

fun main() {
    val people = listOf(Person("leavesC", 24), Person("Ye", 22))
    println(people.maxBy { it.age }) //Person(name=leavesC, age=24)
}
```

当中，库函数 maxBy 可以在任何集合上调用，其需要一个实参：一个函数，用于指定要用来进行比较的函数。花括号中的代码 `{ it.age }` 就是实现了这个逻辑的 Lambda 表达式

上述 maxBy 函数的实参是简化后的写法，这里来看下 maxBy 函数的简化过程

最原始的语法声明应该是这样的，用括号包裹着 Lambda 表达式

```kotlin
println(people.maxBy({ p: Person -> p.age }))
```

kotlin 有一种语法约定，如果 Lambda 表达式是函数调用的最后一个实参，可以将之放到括号的外边

```kotlin
 println(people.maxBy() { p: Person -> p.age })
```

当 Lamdba 表达式是函数唯一的实参时，可以去掉调用代码中的空括号对

```
 println(people.maxBy { p: Person -> p.age })
```

当 Lambda 表达式的参数类型是可以被推导出来时就可以省略声明参数类型

```kotlin
println(people.maxBy { p -> p.age })
```

如果当前上下文期待的是只有一个参数的 Lambda 表达式且参数类型可以被推断出来，就会为该参数生成一个默认名称：it

```kotlin
 println(people.maxBy { it.age })
```

kotlin 和 Java 的一个显著区别就是，在 kotlin 中函数内部的 Lambda 表达式不会仅限于访问函数的参数以及 final 变量，在 Lambda 内部也可以访问并修改非 final 变量

从 Lambda 内部访问外部变量，我们称这些变量被 Lambda 捕捉。当捕捉 final 变量时，变量值和使用这个值的 Lambda 代码一起存储，对非 final 变量来说，其值被封装在一个特殊的包装器中，对这个包装器的引用会和 Lambda 代码一起存储

```kotlin
    var number = 0
    val list = listOf(10, 20, 30, 40)
    list.forEach {
        if (it > 20) {
            number++
        }
    }
    println(number) //2
```

成员引用用于创建一个调用单个方法或者访问单个属性的函数值，通过双冒号把类名称和要引用的成员（一个方法或者一个属性）名称分隔开

成员引用的一个用途就是：如果要当做参数传递的代码块已经被定义成了函数，此时不必专门创建一个调用该函数的 Lambda 表达式，可以直接通过成员引用的方式来传递该函数（也可以传递属性）。此外，成员引用对扩展函数一样适用

```kotlin
data class Person(val name: String, val age: Int) {

    val myAge = age

    fun getPersonAge() = age
}

fun Person.filterAge() = age

fun main() {
    val people = listOf(Person("leavesC", 24), Person("Ye", 22))
    println(people.maxBy { it.age })    //Person(name=leavesC, age=24)
    println(people.maxBy(Person::age))  //Person(name=leavesC, age=24)
    println(people.maxBy(Person::myAge))  //Person(name=leavesC, age=24)
    println(people.maxBy(Person::getPersonAge))  //Person(name=leavesC, age=24)
    println(people.maxBy(Person::filterAge))  //Person(name=leavesC, age=24)
}
```

不管引用的是函数还是属性，都不要在成员引用的名称后面加括号

此外，还可以引用顶层函数

```kotlin
fun test() {
    println("test")
}

fun main() {
    val t = ::test
}
```

也可以用构造方法引用存储或者延期执行创建类实例的动作

```kotlin
data class Person(val name: String, val age: Int)

fun main() {
    val createPerson = ::Person
    val person = createPerson("leavesC", 24)
    println(person)
}
```

### 十八、标准库中的扩展函数

kotlin 标准库中提供了几个比较实用的扩展函数，定义在 Standard 文件下

#### 1、run

run 函数接收一个函数参数并以该函数的返回值作为 run 函数的返回值

```kotlin
@kotlin.internal.InlineOnly
public inline fun <T, R> T.run(block: T.() -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}
```

用例

```kotlin
fun main() {
    var nickName = "leavesC"
    nickName = nickName.run {
        if (isNotEmpty()) {
            this
        } else {
            ""
        }
    }
    println(nickName)
}
```

#### 2、with

with 函数并不是扩展函数，不过由于作用相近，此处就一起介绍了。with 函数的第一个参数是接受者对象 receiver，第二个参数是在 receiver 对象类型上定义的扩展函数，所以可以在函数内部直接调用 receiver 其公开的方法和属性

```kotlin
@kotlin.internal.InlineOnly
public inline fun <T, R> with(receiver: T, block: T.() -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return receiver.block()
}
```

with 函数用于对同一个对象执行多次操作而不需要反复把对象的名称写出来

例如，为了构建一个包含指定内容的字符串，需要先后如下调用

```kotlin
fun main() {
    val result = StringBuilder()
    result.append("leavesC")
    result.append("\n")
    for (letter in 'A'..'Z') {
        result.append(letter)
    }
    println(result.toString())
 }
```

改为通过 with 函数来构建的话会代码会简洁许多

```kotlin
    val result = with(StringBuilder()) {
        append("leavesC")
        append("\n")
        for (letter in 'A'..'Z') {
            append(letter)
        }
        toString()
    }
    println(result)
```

with 函数是一个接受两个参数的函数，在这个例子中就是一个 StringBuilder 和一个 Lambda 表达式，这里利用了把 Lambda 表达式放在括号外的约定

with 函数的返回值是执行 Lambda 表达式的结果，该结果就是 Lambda 中的最后一个表达式的返回值，因此如果将代码修改为如下所示的话，因为 println() 方法无返回值，所以打印出来的内容将是 kotlin.Unit

```kotlin
    val result = with(StringBuilder()) {
        append("leavesC")
        append("\n")
        for (letter in 'A'..'Z') {
            append(letter)
        }
        println("Hello")
    }
    println(result)  //kotin.Unit
```

#### 3、apply

apply 函数被声明为类型 T 的扩展函数，它的接收者是作为实参的 Lambda 的接受者，最终函数返回 this 即对象本身

```kotlin
@kotlin.internal.InlineOnly
public inline fun <T> T.apply(block: T.() -> Unit): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
    return this
}
```

所以apply 函数和 with 函数的唯一区别在于：apply 函数始终会返回作为实参传递给它的对象

```kotlin
 val result = StringBuilder().apply {
        append("leavesC")
        append("\n")
        for (letter in 'A'..'Z') {
            append(letter)
        }
        toString()
    }
    println(result)
    println(result.javaClass) //class java.lang.StringBuilder
```

#### 4、also

also 函数接收一个函数类型的参数，该参数又以接收者本身作为参数，最终返回接收者对象本身

```kotlin
@kotlin.internal.InlineOnly
@SinceKotlin("1.1")
public inline fun <T> T.also(block: (T) -> Unit): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block(this)
    return this
}
```

用例

```kotlin
fun main() {
    val nickName = "leavesC"
    val also = nickName.also {
        it.length
    }
    println(also) //leavesC
}
```

#### 5、let 

also 函数接收一个函数类型的参数，该参数又以接收者本身作为参数，最终返回函数的求值结果

```kotlin
@kotlin.internal.InlineOnly
public inline fun <T, R> T.let(block: (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block(this)
}
```

用例

```kotlin
fun main() {
    val nickName = "leavesC"
    val also = nickName.let {
        it.length
    }
    println(also) //7
}
```

#### 6、takeIf

takeIf 接收一个返回值类型为 bool 的函数，当该参数返回值为 true 时返回接受者对象本身，否则返回 null

```kotlin
@kotlin.internal.InlineOnly
@SinceKotlin("1.1")
public inline fun <T> T.takeIf(predicate: (T) -> Boolean): T? {
    contract {
        callsInPlace(predicate, InvocationKind.EXACTLY_ONCE)
    }
    return if (predicate(this)) this else null
}
```

用例

```kotlin
fun main() {
    println(check("leavesC")) //7
    println(check(null)) //0
}

fun check(name: String?): Int {
    return name.takeIf { !it.isNullOrBlank() }?.length ?: 0
}
```

#### 7、takeUnless

takeUnless 的判断条件与 takeIf 相反，这里不再赘述

```kotlin
@kotlin.internal.InlineOnly
@SinceKotlin("1.1")
public inline fun <T> T.takeUnless(predicate: (T) -> Boolean): T? {
    contract {
        callsInPlace(predicate, InvocationKind.EXACTLY_ONCE)
    }
    return if (!predicate(this)) this else null
}
```

### 十九、函数操作符

#### 1、总数操作符

##### 1、any

如果至少有一个元素符合给出的判断条件，则返回 true

```kotlin
    val list = listOf(1, 3, 5, 7, 9)
    println(list.any { it > 13 })  //false
    println(list.any { it > 7 })   //true
```

##### 2、all

如果全部的元素符合给出的判断条件，则返回 true

```kotlin
    val list = listOf(1, 3, 5, 7, 9)
    println(list.all { it > 13 })  //false
    println(list.all { it > 0 })   //true
```

##### 3、count

返回符合给出判断条件的元素总数

```kotlin
    val list = listOf(1, 3, 5, 7, 9)
    println(list.count { it > 7 })  //1
    println(list.count { it > 2 })  //4
```

##### 4、fold

在一个初始值的基础上从第一项到最后一项通过一个函数累计所有的元素

```kotlin
fun main() {
    val list = listOf(1, 3, 5, 7, 9)
    println(list.fold(2) { total, next->
        println("$next , $total")
        next + total
    })
}
```

```kotlin
1 , 2
3 , 3
5 , 6
7 , 11
9 , 18
27
```

##### 5、foldRight

与 fold  一样，但顺序是从最后一项到第一项

```kotlin
    val list = listOf(1, 3, 5, 7, 9)
    println(list.foldRight(2) { next, total->
        println("$next , $total")
        next + total
    })
```

```kotlin
9 , 2
7 , 11
5 , 18
3 , 23
1 , 26
27
```

##### 6、forEach

```kotlin
    val list = listOf(1, 3, 5, 7, 9)
    list.forEach { print(it + 1) } //246810
```

##### 7、forEachIndexed

类似于 forEach ，同时可以得到元素的索引

```kotlin
    val list = listOf(1, 3, 5, 7, 9)
    list.forEachIndexed { index, value -> println("$index value is $value") }

    0 value is 1
	1 value is 3
	2 value is 5
    3 value is 7
	4 value is 9
```

##### 8、max

返回最大的一项，如果没有则返回null

```kotlin
    val list = listOf(1, 3, 5, 7, 9)
    println(list.max()) //9
```

##### 9、maxBy

根据给定的函数返回最大的一项，如果没有则返回 null

```kotlin
    val list = listOf(1, 3, 5, 7, 9)
    println(list.maxBy { -it }) //1
```

##### 10、min

返回最小的一项，如果没有则返回null

```kotlin
    val list = listOf(1, 3, 5, 7, 9)
    println(list.min()) //1
```

##### 11、minBy

根据给定的函数返回最小的一项，如果没有则返回null

```kotlin
    val list = listOf(1, 3, 5, 7, 9)
    println(list.minBy { -it }) //9
```

##### 12、none

如果没有任何元素与给定的函数匹配，则返回true

```kotlin
    val list = listOf(1, 3, 5, 7, 9)
    println(list.none { it > 10 }) //true
```

##### 13、reduce

与 fold  一样，但是没有一个初始值。通过一个函数从第一项到最后一项进行累计

```kotlin
    val list = listOf(1, 3, 5, 7, 9)
    println(list.reduce { total, next ->
        println("$next , $total")
        total + next
    })
	3 , 1
	5 , 4
	7 , 9
	9 , 16
	25
```

##### 14、reduceRight

与 reduce  一样，但是顺序是从最后一项到第一项

```kotlin
    val list = listOf(1, 3, 5, 7, 9)
    println(list.reduceRight { next, total ->
        println("$next , $total")
        total + next
    })

	7 , 9
	5 , 16
	3 , 21
	1 , 24
	25
```

##### 15、sumBy

返回所有每一项通过函数转换之后的数据的总和

```kotlin
    val list = listOf(1, 3, 5, 7, 9)
    println(list.sumBy { it + 1 }) //30
```

#### 2、过滤操作符

##### 1、drop

返回包含去掉前n个元素的所有元素的列表

```kotlin
    val list = listOf(1, 3, 5, 7, 9)
    println(list.drop(2)) //[5, 7, 9]
```

##### 2、dropWhile

返回从第一个开始不符合给定函数的元素起之后的列表

```kotlin
    val list = listOf(1, 3, 5, 7, 9, 2)
    println(list.dropWhile { it < 4 }) //[5, 7, 9, 2]
```

##### 3、dropLastWhile

从最后一项开始，返回从开始不符合给定函数的元素起之后的列表

```kotlin
    val list = listOf(10, 1, 3, 5, 7, 9)
    println(list.dropLastWhile { it > 4 }) //[10, 1, 3]
```

##### 4、filter

过滤所有符合给定函数条件的元素

```kotlin
    val list = listOf(1, 3, 5, 7, 9, 2)
    println(list.filter { it < 4 }) //[1, 3, 2]
```

##### 5、filterNot

过滤所有不符合给定函数条件的元素

```kotlin
    val list = listOf(1, 3, 5, 7, 9, 2)
    println(list.filterNot { it < 4 }) //[5, 7, 9]
```

##### 6、filterNotNull

过滤所有元素中不是null的元素

```kotlin
    val list = listOf(1, 3, 5, 7, 9, 2, null)
    println(list.filterNotNull()) //[1, 3, 5, 7, 9, 2]
```

##### 7、slice

过滤一个list中指定index的元素

```kotlin
    val list = listOf(1, 3, 5, 7, 9, 2, null)
    println(list.slice(listOf(0, 3))) //[1, 7]
```

##### 8、take

返回从第一个开始的n个元素

```kotlin
    val list = listOf(1, 3, 5, 7, 9, 2, null)
    println(list.take(2)) //[1, 3]
```

##### 9、takeLast

返回从最后一个开始的n个元素

```kotlin
    val list = listOf(1, 3, 5, 7, 9, 2, null)
    println(list.takeLast(2)) //[2, null]
```

##### 10、takeWhile

返回从第一个开始符合给定函数条件的元素。

```kotlin
    val list = listOf(1, 3, 5, -1, 7, 9, 2)
    println(list.takeWhile { it > 2 }) //[]
    println(list.takeWhile { it > 0 }) //[1, 3, 5]
```

#### 3、映射操作符

##### 1、flatMap

遍历所有的元素，为每一个创建一个集合，最后把所有的集合放在一个集合中

```kotlin
    val list = listOf(1, 3, 5, -1, 7, 9, 2)
    println(list.flatMap { listOf(it, it + 1) }) //[1, 2, 3, 4, 5, 6, -1, 0, 7, 8, 9, 10, 2, 3]
```

##### 2、groupBy

返回一个根据给定函数分组后的map 

```kotlin
    val list = listOf(1, 3, 5, -1, 7, 9, 2)
    println(list.groupBy { listOf(it) }) //{[1]=[1], [3]=[3], [5]=[5], [-1]=[-1], [7]=[7], [9]=[9], [2]=[2]}
    println(list.groupBy { listOf(it, it + 1) }) //{[1, 2]=[1], [3, 4]=[3], [5, 6]=[5], [-1, 0]=[-1], [7, 8]=[7], [9, 10]=[9], [2, 3]=[2]}
```

##### 3、map 

返回一个每一个元素根据给定的函数转换所组成的List。 

```kotlin
    val list = listOf(1, 3, 5, -1, 7, 9, 2)
    println(list.map { listOf(it) }) //[[1], [3], [5], [-1], [7], [9], [2]]
    println(list.map { listOf(it, it + 1) }) //[[1, 2], [3, 4], [5, 6], [-1, 0], [7, 8], [9, 10], [2, 3]]
```

##### 4、mapIndexed 

返回一个每一个元素根据给定的包含元素index的函数转换所组成的List

```kotlin
    val list = listOf(1, 3, 5, -1, 7, 9, 2)
    println(list.mapIndexed { index, value -> index }) //[0, 1, 2, 3, 4, 5, 6]
    println(list.mapIndexed { index, value -> index * value }) //[0, 3, 10, -3, 28, 45, 12]
```

##### 5、mapNotNull 

返回一个每一个非null元素根据给定的函数转换所组成的List

```kotlin
    val list = listOf(1, 3, 5, -1, 7, 9, null, 2)
    println(list.mapNotNull { it }) //[1, 3, 5, -1, 7, 9, 2]
```

#### 4、元素操作符 

##### 1、contains 

如果指定元素可以在集合中找到，则返回true

```kotlin
    val list = listOf(1, 3, 5, -1, 7, 9, null, 2)
    println(list.contains(3)) //true
    println(list.contains(13)) //false
```

##### 2、elementAt 

返回给定index对应的元素，如果index数组越界则会抛出 IndexOutOfBoundsException

```kotlin
    val list = listOf(1, 3, 5, -1, 7, 9, null, 2)
    println(list.elementAt(3)) //-1
    println(list.elementAt(6)) //null
```

##### 3、elementAtOrElse 

返回给定index对应的元素，如果index数组越界则会根据给定函数返回默认值

```kotlin
    val list = listOf(1, 3, 5, -1, 7, 9, null, 2)
    println(list.elementAtOrElse(3, { it * 2 }))  //-1
    println(list.elementAtOrElse(16, { it * 2 })) //32
```

##### 4、elementAtOrNull 

返回给定index对应的元素，如果index数组越界则会返回null

```kotlin
    val list = listOf(1, 3, 5, -1, 7, 9, null, 2)
    println(list.elementAtOrNull(3))  //-1
    println(list.elementAtOrNull(16)) //null
```

##### 5、first 

返回符合给定函数条件的第一个元素

```kotlin
    val list = listOf(1, 3, 5, -1, 7, 9, 2)
    println(list.first { it % 3 == 0 })  //3
```

##### 6、firstOrNull 

返回符合给定函数条件的第一个元素，如果没有符合则返回null

```kotlin
    val list = listOf(1, 3, 5, -1, 7, 9, 2)
    println(list.firstOrNull { it % 3 == 0 })  //3
    println(list.firstOrNull { it % 8 == 0 })  //null
```

##### 7、indexOf 

返回指定元素的第一个index，如果不存在，则返回 -1

```kotlin
    val list = listOf(1, 3, 5, -1, 7, 9, 2)
    println(list.indexOf(5))  //2
    println(list.indexOf(12)) //-1
```

##### 8、indexOfFirst 

返回第一个符合给定函数条件的元素的index，如果没有符合则返回 -1

```kotlin
    val list = listOf(1, 3, 5, 1, 7, 9, 2)
    println(list.indexOfFirst { it % 2 == 0 })   //6
    println(list.indexOfFirst { it % 12 == 0 })  //-1
```

##### 9、indexOfLast 

返回最后一个符合给定函数条件的元素的index，如果没有符合则返回 -1

```kotlin
    val list = listOf(1, 3, 5, 6, 7, 9, 2)
    println(list.indexOfLast { it % 2 == 0 })   //6
    println(list.indexOfLast { it % 12 == 0 })  //-1
```

##### 10、last 

返回符合给定函数条件的最后一个元素

```kotlin
    val list = listOf(1, 3, 5, 6, 7, 9, 2)
    println(list.last { it % 2 == 0 })   //2
    println(list.last { it % 3 == 0 })   //9
```

##### 11、lastIndexOf 

返回指定元素的最后一个index，如果不存在，则返回 -1

```kotlin
    val list = listOf(1, 3, 2, 6, 7, 9, 2)
    println(list.lastIndexOf(2))    //6
    println(list.lastIndexOf(12))   //-1
```

##### 12、lastOrNull 

返回符合给定函数条件的最后一个元素，如果没有符合则返回null

```kotlin
    val list = listOf(1, 3, 2, 6, 7, 9, 2)
    println(list.lastOrNull { it / 3 == 3 })    //9
    println(list.lastOrNull { it == 10 })       //null
```

##### 13、single 

返回符合给定函数的单个元素，如果没有符合或者超过一个，则抛出异常

```kotlin
    val list = listOf(1, 9, 2, 6, 7, 9, 2)
    println(list.single { it % 7 == 0 })  //7
    println(list.single { it == 2 })      //IllegalArgumentException
```

##### 14、singleOrNull 

返回符合给定函数的单个元素，如果没有符合或者超过一个，则返回null

```kotlin
    val list = listOf(1, 9, 2, 6, 7, 9, 2)
    println(list.singleOrNull { it % 7 == 0 })  //7
    println(list.singleOrNull { it == 2 })      //null
```

#### 5、生产操作符 

##### 1、partition 

把一个给定的集合分割成两个，第一个集合是由原集合每一项元素匹配给定函数条 件返回 true 的元素组成，第二个集合是由原集合每一项元素匹配给定函数条件返回 false 的元素组成

```kotlin
    val list = listOf(1, 9, 2, 6, 7, 9, 2)
    val (list1, list2) = list.partition { it % 2 == 0 }
    println(list1)  //[2, 6, 2]
    println(list2)  //[1, 9, 7, 9]
```

##### 2、plus 

返回一个包含原集合和给定集合中所有元素的集合，因为函数的名字原因，我们可以使用 + 操作符

```kotlin
    val list1 = listOf(1, 9, 2, 6, 7, 9, 2)
    val list2 = listOf(1, 2, 4, 6, 8, 10)
    println(list1.plus(list2)) //[1, 9, 2, 6, 7, 9, 2, 1, 2, 4, 6, 8, 10]
    println(list1 + list2)  //[1, 9, 2, 6, 7, 9, 2, 1, 2, 4, 6, 8, 10]
```

##### 3、zip 

返回由 pair 组成的List，每个 pair 由两个集合中相同index的元素组成。这个返回的List的大小由最小的那个集合决定

```kotlin
    val list1 = listOf(1, 9, 2, 6, 7, 9, 2)
    val list2 = listOf(1, 2, 4, 6, 8, 10)
    val list3 = list1.zip(list2)
    println(list3.javaClass)
    println(list3.get(0).javaClass)
    println("${list3.get(0).first} , ${list3.get(0).second}")
    list3.forEach { println(it) }
```

```kotlin
    class java.util.ArrayList
    class kotlin.Pair
    1 , 1
    (1, 1)
    (9, 2)
    (2, 4)
    (6, 6)
    (7, 8)
    (9, 10)
```

##### 4、unzip 

从包含pair的List中生成包含List的Pair

```kotlin
    val list1 = listOf(Pair("leavesC", 1), Pair("leavesC_2", 2), Pair("leavesC_3", 3))
    val list2 = list1.unzip()
    println(list2.javaClass)
    println(list2.first)
    println(list2.second)
```

```kotlin
    class kotlin.Pair
    [leavesC, leavesC_2, leavesC_3]
    [1, 2, 3]
```

#### 6、顺序操作符 

##### 1、reverse 

返回一个与指定list相反顺序的list

```kotlin
    val list1 = listOf(Pair("leavesC", 1), Pair("leavesC_2", 2), Pair("leavesC_3", 3))
    val list2 = list1.reversed()
    println(list2)      //[(leavesC_3, 3), (leavesC_2, 2), (leavesC, 1)]
```

##### 2、sort 

返回一个自然排序后的list

```kotlin
    val list1 = listOf(2, 4, 1, 9, 5, 10)
    val list2 = list1.sorted()
    println(list2) //[1, 2, 4, 5, 9, 10]

    val list3 = listOf("a", "c", "ab", "b", "cdd", "cda")
    val list4 = list3.sorted()
    println(list4) //[a, ab, b, c, cda, cdd]
```

##### 3、sortBy 

返回一个根据指定函数排序后的list 

```kotlin
    val list1 = listOf(2, 4, 1, 9, 5, 10)
    val list2 = list1.sortedBy { it - 3 }
    println(list2) //[1, 2, 4, 5, 9, 10]
```

##### 4、sortDescending 

返回一个降序排序后的List

```kotlin
    val list1 = listOf(2, 4, 1, 9, 5, 10)
    val list2 = list1.sortedDescending()
    println(list2) //[10, 9, 5, 4, 2, 1]
```

##### 5、sortDescendingBy 

返回一个根据指定函数降序排序后的list

```kotlin
    val list1 = listOf(2, 4, 1, 9, 5, 10)
    val list2 = list1.sortedByDescending { it % 2 }
    println(list2) //[1, 9, 5, 2, 4, 10]
```

### 二十、异常

kotlin 中异常处理的基本形式和 Java 类似

```kotlin
fun compute(index: Int): Boolean {
    if (index !in 0..10) {
        throw IllegalArgumentException("参数错误")
    }
    return true
}
```

和 Java 不同的是，kotlin 中 throw 结构是一个表达式，可以作为另一个表达式的一部分来使用

例如下面这个例子，如果条件不满足，则将抛出异常，从而导致 status 变量也不会初始化

```kotlin
val status = if (index in 0..10) index else throw IllegalArgumentException("参数错误")
```

此外，在 Java 中对于受检异常必须显式地处理，通过 try/catch 语句捕获异常或者是抛给其调用者来处理。而 kotlin 不区分受检异常和未受检异常，不用指定函数抛出的异常，可以处理也可以不处理异常

在 kotlin 中 ，try 关键字引入了一个表达式，从而可以把表达式的值赋给一个变量。如果一个 try 代码块执行正常，代码块中最后一个表达式就是结果，如果捕获到了一个异常，则相应 catch 代码块中最后一个表达式就是结果

看以下例子，如果 try 表达式包裹的表达式会抛出异常，则返回值为 null ，否则为 true 

```kotlin
fun main() {
    compute(5)   //fun end : true
    compute(100) //fun end : null
}

fun compute(index: Int) {
    val status = try {
        if (index in 0..10) true else throw IllegalArgumentException("参数错误")
    } catch (e: Exception) {
        null
    }
    println("fun end : " + status)
}
```

但是，如果在 catch 语句中使用 return 结束了 compute 函数，则没有任何输出

```kotlin
fun main() {
    compute(5)   //fun end : true
    compute(100) //没有任何输出
}

fun compute(index: Int) {
    val status = try {
        if (index in 0..10) true else throw IllegalArgumentException("参数错误")
    } catch (e: Exception) {
        return
    }
    println("fun end : " + status)
}
```

### 二十一、运算符重载

kotlin 允许为类型提供预定义的操作符实现，这些操作符具有固定的符号表示（例如 + 和 * ）和固定的优先级，通过操作符重载可以将操作符的行为映射到指定的方法。为实现这样的操作符，需要为类提供一个固定名字的成员函数或扩展函数，相应的重载操作符的函数需要用 operator 修饰符标记

#### 1、一元操作符

| 操作符 |      函数      |
| ------ | :------------: |
| +a     | a.unaryPlus()  |
| -a     | a.unaryMinus() |
| !a     |    a.not()     |
| a++    |    a.inc()     |
| a--    |    a.dec()     |

#### 2、二元操作符

| 操作符  |       函数       |
| ------- | :--------------: |
| a + b   |    a.plus(b)     |
| a - b   |    a.minus(b)    |
| a * b   |    a.times(b)    |
| a / b   |     a.div(b)     |
| a % b   |     a.rem(b)     |
| a..b    |   a.rangeTo(b)   |
| a in b  |  b.contains(a)   |
| a !in b |  !b.contains(a)  |
| a += b  | a.plusAssign(b)  |
| a -= b  | a.minusAssign(b) |
| a *= b  | a.timesAssign(b) |
| a /= b  |  a.divAssign(b)  |
| a %= b  |  a.remAssign(b)  |

#### 3、数组操作符

| 操作符               |          函数           |
| -------------------- | :---------------------: |
| a[i]                 |        a.get(i)         |
| a[i, j]              |       a.get(i, j)       |
| a[i_1, ..., i_n]     |  a.get(i_1, ..., i_n)   |
| a[i] = b             |       a.set(i, b)       |
| a[i, j] = b          |     a.set(i, j, b)      |
| a[i_1, ..., i_n] = b | a.set(i_1, ..., i_n, b) |

#### 4、等于操作符

| 操作符 |             函数              |
| ------ | :---------------------------: |
| a == b |  a?.equals(b) ?: b === null   |
| a != b | !(a?.equals(b) ?: b === null) |

相等操作符有一点不同，为了达到正确合适的相等检查做了更复杂的转换，因为要得到一个确切的函数结构比较，不仅仅是指定的名称

方法必须要如下准确地被实现：

```kotlin
operator fun equals(other: Any?): Boolean
```

操作符 ===  和 !==  用来做身份检查（它们分别是 Java 中的 ==  和 !=  ），并且它们不能被重载

#### 5、比较操作符

| 操作符 |        函数         |
| ------ | :-----------------: |
| a > b  | a.compareTo(b) > 0  |
| a < b  | a.compareTo(b) < 0  |
| a >= b | a.compareTo(b) >= 0 |
| a <= b | a.compareTo(b) <= 0 |

所有的比较都转换为对  compareTo  的调用，这个函数需要返回  Int  值

#### 6、函数调用

| 方法             |          调用           |
| ---------------- | :---------------------: |
| a()              |       a.invoke()        |
| a(i)             |       a.invoke(i)       |
| a(i, j)          |     a.invoke(i, j)      |
| a(i_1, ..., i_n) | a.invoke(i_1, ..., i_n) |

#### 7、例子

看几个例子

```kotlin
data class Point(val x: Int, val y: Int) {

    //+Point
    operator fun unaryPlus() = Point(+x, +y)

    //Point++ / ++Point
    operator fun inc() = Point(x + 1, y + 1)

    //Point + Point
    operator fun plus(point: Point) = Point(x + point.x, y + point.y)

    //Point + Int
    operator fun plus(value: Int) = Point(x + value, y + value)

    //Point[index]
    operator fun get(index: Int): Int {
        return when (index) {
            0 -> x
            1 -> y
            else -> throw IndexOutOfBoundsException("无效索引")
        }
    }

    //Point(index)
    operator fun invoke(index: Int) = when (index) {
        0 -> x
        1 -> y
        else -> throw IndexOutOfBoundsException("无效索引")
    }

}
```

```kotlin
fun main() {
    //+Point(x=10, y=-20)  =  Point(x=10, y=-20)
    println("+${Point(10, -20)}  =  ${+Point(10, -20)}")

    //Point(x=10, y=-20)++  =  Point(x=10, y=-20)
    var point = Point(10, -20)
    println("${Point(10, -20)}++  =  ${point++}")

    //++Point(x=10, y=-20)  =  Point(x=11, y=-19)
    point = Point(10, -20)
    println("++${Point(10, -20)}  =  ${++point}")

    //Point(x=10, y=-20) + Point(x=10, y=-20)  =  Point(x=20, y=-40)
    println("${Point(10, -20)} + ${Point(10, -20)}  =  ${Point(10, -20) + Point(10, -20)}")

    //Point(x=10, y=-20) + 5  =  Point(x=15, y=-15)
    println("${Point(10, -20)} + ${5}  =  ${Point(10, -20) + 5}")

    point = Point(10, -20)
    //point[0] value is: 10
    println("point[0] value is: ${point[0]}")
    //point[1] value is: -20
    println("point[1] value is: ${point[1]}")

    //point(0) values is: 10
    println("point(0) values is: ${point(0)}")
}
```

### 二十二、中缀调用与解构声明

#### 1、中缀调用

可以以以下形式创建一个 Map 变量

```kotlin
fun main() {
    val maps = mapOf(1 to "leavesC", 2 to "ye", 3 to "https://juejin.cn/user/923245496518439")
    maps.forEach { key, value -> println("key is : $key , value is : $value") }
}
```

使用 “to” 来声明 map 的 key 与 value 之间的对应关系，这种形式的函数调用被称为中缀调用

kotlin 标准库中对 to 函数的声明如下所示，其作为扩展函数存在，且是一个泛型函数，返回值 Pair 最终再通过解构声明分别将 key 和 value 传给 Map

```kotlin
public infix fun <A, B> A.to(that: B): Pair<A, B> = Pair(this, that)
```

中缀调用只能与只有一个参数的函数一起使用，无论是普通的函数还是扩展函数。中缀符号需要通过 **infix** 修饰符来进行标记

```kotlin
fun main() {
    val pair = 10 test "leavesC"
    val pair2 = 1.2 test 20
    println(pair2.javaClass) //class kotlin.Pair
}

infix fun Any.test(other: Any) = Pair(this, other)
```

对于 `mapOf` 函数来说，它可以接收不定数量的 `Pair` 类型对象，因此我们也可以通过自定义的中缀调用符 `test` 来创建一个 map 变量

```kotlin
public fun <K, V> mapOf(vararg pairs: Pair<K, V>): Map<K, V> =
    if (pairs.size > 0) pairs.toMap(LinkedHashMap(mapCapacity(pairs.size))) else emptyMap()
```

```kotlin
 val map = mapOf(10 test "leavesC", 20 test "hello")
```

#### 2、解构声明

有时会有把一个对象拆解成多个变量的需求，在 kotlin 中这种语法称为解构声明

例如，以下例子将 Person 变量结构为了两个新变量：name 和 age，并且可以独立使用它们

```kotlin
data class Person(val name: String, val age: Int)

fun main() {
    val (name, age) = Person("leavesC", 24)
    println("Name: $name , age: $age")
    //Name: leavesC , age: 24
}
```

一个解构声明会被编译成以下代码：

```kotlin
    val name = person.component1()
    val age = person.component2()
```

其中的 `component1()` 和 `component2()` 函数是在 kotlin 中广泛使用的约定原则的另一个例子。任何表达式都可以出现在解构声明的右侧，只要可以对它调用所需数量的 `component` 函数即可

需要注意的是，`componentN()` 函数需要用 `operator` 关键字标记，以允许在解构声明中使用它们

对于数据类来说，其自动生成了 `componentN()` 函数，而对非数据类，为了使用解构声明，需要我们自己来手动声明函数

```kotlin
class Point(val x: Int, val y: Int) {
    operator fun component1() = x
    operator fun component2() = y
}

fun main() {
    val point = Point(100, 200)
    val (x, y) = point
    println("x: $x , y: $y")
    //x: 100 , y: 200
}
```

如果我们需要从一个函数返回两个或者更多的值，这时候使用解构声明就会比较方便了

这里使用的是标准类 Pair 来包装要传递的数据，当然，也可以自定义数据类

```kotlin
fun computer(): Pair<String, Int> {
    //各种计算
    return Pair("leavesC", 24)
}

fun main() {
    val (name, age) = computer()
    println("Name: $name , age: $age")
}
```

此外，解构声明也可以用在 for 循环中

```kotlin
    val list = listOf(Person("leavesC", 24), Person("leavesC", 25))
    for ((name, age) in list) {
        println("Name: $name , age: $age")
    }
```

对于遍历 map 同样适用

```kotlin
    val map = mapOf("leavesC" to 24, "ye" to 25)
    for ((name, age) in map) {
        println("Name: $name , age: $age")
    }
```

同样也适用于 lambda 表达式

```kotlin
    val map = mapOf("leavesC" to 24, "ye" to 25)
    map.mapKeys { (key, value) -> println("key : $key , value : $value") }
```

如果在解构声明中不需要某个变量，那么可以用下划线取代其名称，此时不会调用相应的 `componentN()` 操作符函数

```kotlin
    val map = mapOf("leavesC" to 24, "ye" to 25)
    for ((_, age) in map) {
        println("age: $age")
    }
```

### 二十三、Object 关键字

#### 1、对象声明

在 kotlin 的世界中，可以通过**对象声明**这一功能来实现 Java 中的单例模式，将类声明与该类的单一实例声明结合到一起。与类一样，一个对象声明可以包含属性、方法、初始化语句块等的声明，且可以继承类和实现接口，唯一不被允许的是构造方法

与普通类的实例不同，对象声明在定义的时候就被立即创建了，不需要在代码的其它地方调用构造方法，因此为对象声明定义构造方法是没有意义的

```kotlin
interface Fly {

    fun fly()

}

open class Eat {

    fun eat() {
        println("eat")
    }

}

object Animal : Eat(), Fly {

    override fun fly() {
        println("fly")
    }

}

fun main() {
    Animal.fly()
    Animal.eat()
}
```

kotlin 中的对象声明被编译成了通过静态字段来持有它的单一实例的类，这个字段名字始终都是 INSTANCE

例如，对于 kotlin 中的如下两个对象声明

```kotlin
class Test {

    object SingleClass {
        val names = arrayListOf<String>()
    }

    object SingleClass2 {
        val names = arrayListOf<String>()
    }

}
```

在 Java 代码中来访问这两个对象

```java
    public static void main(String[] args) {
        Test.SingleClass.INSTANCE.getNames();
        Test.SingleClass2.INSTANCE.getNames();
    }
```

#### 2、伴生对象

如果需要一个可以在没有类实例的情况下调用但是需要访问类内部的函数（类似于 Java 中的静态变量/静态函数），可以将其写成那个类中的对象声明的成员

通过关键字 companion ，就可以获得通过容器类名称来访问这个对象的方法和属性的能力，不再需要显式地指明对象的名称

```kotlin
class Test {

    companion object {

        const val NAME = ""

        fun testFun() {

        }
    }

}

fun main() {
    Test.NAME
    Test.testFun()
}
```

##### 1、工厂模式

可以利用伴生对象来实现工厂模式

```kotlin
private class User private constructor(val name: String) {

    companion object {
        fun newById(id: Int) = User(id.toString())

        fun newByDouble(double: Double) = User(double.toString())
    }

}

fun main() {
    //构造函数私有，无法创建
    //val user1 = User("leavesC")
    val user2 = User.newById(10)
    val user3 = User.newByDouble(1.3)
}
```

##### 2、指定名称

伴生对象既可以为其指定名字，也可以直接使用其默认名 Companion，在引用伴生对象时，可以自由选择是否要在类名后加上伴生对象名

如果使用的是其默认名 Companion（没有自定义名称），则以下两种引用方式都是等价的

```kotlin
    val user2 = User.Companion.newById(10)
    val user3 = User.newByDouble(1.3)
```

如果为伴生对象声明了自定义名称，引用方式等同

```kotlin
private class User private constructor(val name: String) {

    companion object UserLoader {
        fun newById(id: Int) = User(id.toString())

        fun newByDouble(double: Double) = User(double.toString())
    }

}

fun main() {
    //构造函数私有，无法创建
    //val user1 = User("leavesC")
    val user2 = User.UserLoader.newById(10)
    val user3 = User.newByDouble(1.3)
}
```

##### 3、实现接口

伴生对象也可以实现接口，且可以直接将包含它的类的名字当做实现了该接口的对象实例来使用

```kotlin
private class User private constructor(val name: String) {

    companion object UserLoader : Runnable {

        override fun run() {

        }
    }

}

fun newThread(runnable: Runnable) = Thread(runnable)

fun main() {
    //User 会直接被当做 Runnable 的实例
    val thread = newThread(User)
    val thread2 = newThread(User.UserLoader)
}
```

#### 3、对象表达式

object 能用来声明匿名对象，可用于替代 Java 中的匿名内部类，且对象表达式中的代码可以访问并修改其外部的非 final 型的变量

```kotlin
fun newThread(runnable: Runnable) = Thread(runnable)

fun main() {
    var count = 0
    val thread = newThread(object : Runnable {
        override fun run() {
            count++
        }
    })
}
```

### 二十四、委托

#### 1、委托模式

委托模式是一种基本的设计模式，该模式下有两个对象参与处理同一个请求，接受请求的对象将请求委托给另一个对象来处理。kotlin 原生支持委托模式，可以零样板代码来实现，通过关键字 by 实现委托

```kotlin
interface Printer {

    fun print()
    
}

class DefaultPrinter : Printer {

    override fun print() {
         println("DefaultPrinter print")
    }

}

class CustomPrinter(val printer: Printer) : Printer by printer

fun main() {
    val printer = CustomPrinter(DefaultPrinter())
    printer.print() //DefaultPrinter print
}
```

CustomPrinter 的 by 子句表示将会在 CustomPrinter 中存储 printer 变量，并且编译器将为 CustomPrinter 隐式生成 Printer 接口的所有抽象方法，并将这些方法的调用操作转发给 printer 

此外，CustomPrinter 也可以决定自己实现部分方法或全部自己实现，但重写的成员不会在委托对象的成员中调用 ，委托对象的成员只能访问其自身对接口成员实现

```kotlin
interface Printer {

    val message: String

    fun print()

    fun reprint()

}

class DefaultPrinter : Printer {

    override val message: String = "DefaultPrinter message"

    override fun print() {
        println(message)
    }

    override fun reprint() {
        println("DefaultPrinter reprint")
    }

}

class CustomPrinter(val printer: Printer) : Printer by printer {

    override val message: String = "CustomPrinter message"

    override fun reprint() {
        println("CustomPrinter reprint")
    }

}

fun main() {
    val printer = CustomPrinter(DefaultPrinter())
    printer.print() //DefaultPrinter message
    printer.reprint() //CustomPrinter reprint
}
```

#### 2、属性委托

kotlin 支持通过委托属性将对一个属性的访问操作委托给另外一个对象来完成，对应的语法格式是：

```kotlin
val/var <属性名>: <类型> by <表达式>
```

**属性的委托**不必实现任何的接口，但需要提供一个 **getValue()** 方法与 **setValue()**（对于 var 属性），对一个属性的 get 和 set 操作会被委托给**属性的委托**的这两个方法

```kotlin
class Delegate {
    //第一个参数表示被委托的对象、第二个参数表示被委托对象自身的描述
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
    }
	//第一个参数表示被委托的对象、第二个参数表示被委托对象自身的描述，第三个参数是将要赋予的值
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
    }
}
```

看以下的小例子，通过输出值就可以看出各个方法的调用时机

```kotlin
package test

import kotlin.reflect.KProperty

class Delegate {

    private var message: String? = null

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        println("${thisRef?.javaClass?.name}, thank you for delegating '${property.name}' to me!")
        return message ?: "null value"
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        println("$value has been assigned to '${property.name}' in ${thisRef?.javaClass?.name}.")
        message = value
    }
}

class Example {
    var strValue: String by Delegate()
}

fun main() {
    val example = Example()
    println(example.strValue)
    example.strValue = "leaveC"
    println(example.strValue)
//    test.Example, thank you for delegating 'strValue' to me!
//    null value
//    leaveC has been assigned to 'strValue' in test.Example.
//    test.Example, thank you for delegating 'strValue' to me!
//    leaveC
}
```

#### 3、延迟属性

**lazy()** 是接受一个 lambda 并返回一个 **Lazy < T >** 实例的函数，返回的实例可以作为实现延迟属性的委托，第一次调用 **get()** 会执行已传递给 **lazy()** 函数的 lambda 表达式并记录结果， 后续调用 **get()** 只是返回记录的结果

```kotlin
class Example {

    val lazyValue1: String by lazy {
        println("lazyValue1 computed!")
        "Hello"
    }

    val lazyValue2: String by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        println("lazyValue2 computed!")
        computeLazyValue()
    }

    private fun computeLazyValue() = "leavesC"

}

fun main() {
    val example = Example()
    println(example.lazyValue1) //lazyValue1 computed!     Hello
    println(example.lazyValue1) //Hello
    println(example.lazyValue2) //lazyValue2 computed! leavesC
}
```

默认情况下，对于 lazy 属性的求值是带同步锁的（synchronized），即带有 **LazyThreadSafetyMode.SYNCHRONIZED** 参数，此时该值只允许同一时刻只能有一个线程对其进行初始化，并且所有线程会看到相同的初始化值。如果初始化委托的同步锁不是必需的，即如果允许多个线程同时执行，那么可以将  **LazyThreadSafetyMode.PUBLICATION**  作为参数传递给 **lazy()** 函数。 而如果你确定初始化将总是发生在单个线程，那么可以使用 **LazyThreadSafetyMode.NONE** 模式， 此时不会有任何线程安全的保证以及相关的资源开销

#### 4、可观察属性

**Delegates.observable()** 接受两个参数：初始值以及修改属性值时的回调函数。当为属性赋值后就会调用该回调函数，该回调函数包含三个参数：被赋值的属性、旧值与新值

```kotlin
fun main() {
    val example = Example()
    example.age = 24 //kProperty.name: age , oldValue: -100 , newValue: 24
    example.age = 27 //kProperty.name: age , oldValue: 24 , newValue: 27
}

class Example {
    var age: Int by Delegates.observable(-100) { kProperty: KProperty<*>, oldValue: Int, newValue: Int ->
        println("kProperty.name: ${kProperty.name} , oldValue: $oldValue , newValue: $newValue")
    }
}
```

如果想要拦截一个赋值操作并判断是否进行否决，可以使用 vetoable() 函数，通过返回一个布尔值来决定是否进行拦截，该判断逻辑是在属性被赋新值生效之前进行

```kotlin
fun main() {
    val example = Example()
    example.age = 24  //kProperty.name: age , oldValue: -100 , newValue: 24
    example.age = -10 //kProperty.name: age , oldValue: 24 , newValue: -10
    example.age = 30  //kProperty.name: age , oldValue: 24 , newValue: 30 (oldValue 依然是 24，说明第二次的赋值操作被否决了)
}

class Example {
    var age: Int by Delegates.vetoable(-100) { kProperty: KProperty<*>, oldValue: Int, newValue: Int ->
        println("kProperty.name: ${kProperty.name} , oldValue: $oldValue , newValue: $newValue")
        age <= 0 //返回true 则表示拦截该赋值操作
    }
}
```

#### 5、把属性储存在映射中

可以在一个 map 映射里存储属性的值，然后把属性的存取操作委托给 map 进行管理

```kotlin
fun main() {
    val student = Student(
        mapOf(
            "name" to "leavesC",
            "age" to 24
        )
    )
    println(student.name)
    println(student.age)
}

class Student(val map: Map<String, Any?>) {
    val name: String by map
    val age: Int by map
}
```

在上述示例中，属性 name 和 age 都是不可变的（val），因此 map 的类型也是 Map 而非 MutableMap（MutableMap 在赋值后可以修改），因此如果为了支持 var 属性，可以将只读的 Map 换成 MutableMap

#### 6、局部委托属性

可以将局部变量声明为委托属性

```kotlin
class Printer {

    fun print() {
        println("temp.Printer print")
    }

}

fun getPrinter(): Printer {
    println("temp.Printer getPrinter")
    return Printer()
}

//局部委托
fun example(getPrinter: () -> Printer) {
    val lPrinter by lazy(getPrinter)
    val valid = true
    if (valid) {
        lPrinter.print()
    }
}

fun main() {
    example { getPrinter() }
    //temp.Printer getPrinter
    //temp.Printer print
}
```

委托变量只会在第一次访问时才会进行初始化，因此如果 **valid** 为 false 的话，**getPrinter()** 方法就不会被调用

### 二十五、注解

注解是将元数据附加到代码元素上的一种方式，附件的元数据就可以在编译后的类文件或者运行时被相关的源代码工具访问

注解的语法格式如下所示：

```kotlin
    annotation class AnnotationName()
```

注解的附加属性可以通过用元注解标注注解类来指定：

- @Target            指定该注解标注的允许范围（类、函数、属性等）
- @Retention         指定该注解是否要存储在编译后的 class 文件中，如果要保存，则在运行时可以通过反射来获取到该注解值
- @Repeatable        标明允许在单个元素上多次使用相同的该注解
- @MustBeDocumented  指定该注解是公有 API 的一部分，并且应该包含在生成的 API 文档中显示的类或方法的签名中

```kotlin
    @Target(AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
    @Retention(AnnotationRetention.RUNTIME)
    @Repeatable
    @MustBeDocumented
    annotation class AnnotationName()
```

注解可以声明包含有参数的构造函数

```kotlin
    annotation class OnClick(val viewId: Long)
```

允许的参数类型有：

- 原生数据类型，对应 Java 原生的 int 、long、char 等
- 字符串
- class 对象
- 枚举
- 其他注解
- 以上类型的数组

注解参数不能包含有可空类型，因为 JVM 不支持将 null 作为注解属性的值来存储

看一个在运行时获取注解值的例子

```kotlin
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class OnClick(val viewId: Long)

class AnnotationsTest {

    @OnClick(200300)
    fun onClickButton() {
        println("Clicked")
    }

}

fun main() {
    val annotationsTest = AnnotationsTest()
    for (method in annotationsTest.javaClass.methods) {
        for (annotation in method.annotations) {
            if (annotation is OnClick) {
                println("method name: " + method.name)  //method name: onClickButton
                println("OnClick viewId: " + annotation.viewId)  //OnClick viewId: 200300
            }
        }
    }
}
```