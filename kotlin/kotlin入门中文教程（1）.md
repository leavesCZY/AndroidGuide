> 本文已收录至学习笔记大全：[JavaKotlinAndroidGuide](https://github.com/leavesC/JavaKotlinAndroidGuide)
>
> 作者：[leavesC](https://github.com/leavesC)

[TOC]



### 一、Hello World

按照国际惯例，学习一门新的语言通常都是从 Hello World 开始的，在这里也不例外

```kotlin
package main

fun main(args: Array<String>) {
    println("Hello World")
}
```

从这个简单的函数就可以列出 kotlin 和 Java 的几点不同

1. 函数可以定义在文件的最外层，不需要把它放在类中
2. 用关键字 **fun** 来声明一个函数
3. 参数类型写在变量名之后，这有助于在类型自动推导时省略类型声明
4. 数组就是类。和 Java 不同，kotlin 没有声明数组类型的特殊语法
5. 使用 `println` 代替了 `System.out.println` ，这是 kotlin 标准库提供的对 Java 标准库函数的封装
6. 可以省略代码结尾的分号

此外，kotlin 的最新版本已经可以省略 `main` 方法的参数了

### 二、Package

kotlin 文件都以一条 **package** 语句开头，文件中定义的所有声明（类、函数和属性）都会被放到这个包中。如果其他文件中定义的声明也有相同的包，这个文件可以直接使用它们，如果包不相同则需要导入它们

包的声明应处于源文件顶部，import 语句紧随其后

```kotlin
package base

import java.text.SimpleDateFormat
import java.util.*
```

kotlin 不区分导入的是类还是函数，允许使用 `import` 关键字导入任何种类的声明。此外，也可以在包名称后加上 **.*** 来导入特定包中定义的所有声明，这不仅会让包中定义的类可见，也会让顶层函数和属性可见

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

Java 语言规定类要放到和包结构**匹配**的文件夹目录结构中，而 kotlin 允许把多个类放到同一个文件中，文件名也可以任意选择。kotlin 也没有对磁盘上源文件的布局强加任何限制，包层级结构不需要遵循目录层级结构 ,但最好还是遵循 Java 的目录布局并根据包结构把源码文件放到相应的目录中

如果包名出现命名冲突，可以使用  **as  关键字**在本地重命名冲突项来消歧义

```kotlin
import learn.package1.Point
import learn.package2.Point as PointTemp //PointTemp 可以用来表示 learn.package2.Point 了
```

kotlin 中也有一个类似的概念可以用来重命名现有类型，叫做类型别名。类型别名用于为现有类型提供一个替代名称，如果类型名称比较长，就可以通过引入一个较短或者更为简略的名称来方便记忆

类型别名不会引入新的类型，依然相应其底层类型，所以在下述代码中输出的 class 类型是一致的

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

编程领域中也推崇一种开发原则：尽可能使用 val，不可变对象及纯函数来设计程序。这样可以尽量避免副作用带来的影响

```kotlin
fun main() {
    //只读变量即赋值后不可以改变值的变量，用 val 声明
    //声明一个整数类型的不可变变量
    val intValue: Int = 100

    //声明一个双精度类型的可变变量
    var doubleValue: Double = 100.0
}
```

在声明变量时我们通常不需要显式指明变量的类型，这可以由编译器根据上下文自动推导出来。如果只读变量在声明时没有初始值，则必须指明变量类型，且在使用前必须确保在各个分支条件下变量可以被初始化，否则编译期会报异常

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

#### 3.1、基本数据类型

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

> 如果 intValue_1 的值是100，就会发现 intValue_2 === intValue_3 的比较结果是 true，这就涉及到 Java 对包装类对象的重复使用问题了

#### 3.2、字符串

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

#### 3.3、数组

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

#### 3.4、Any 和 Any?

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

#### 3.5、Unit

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

#### 3.6、Nothing

Nothing 类型没有任何值，只有被当做函数返回值使用，或者被当做泛型函数返回值的类型参数使用时才会有意义，可以用 Nothing 来表示一个函数不会被正常终止，从而帮助编译器对代码进行诊断

编译器知道返回值为 Nothing 类型的函数从不正常终止，所以编译器会把 name1 的类型推断为非空，因为 name1 在为 null 时的分支处理会始终抛出异常

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
```

```kotlin
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

#### 4.1、命名参数

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

#### 4.2、默认参数值

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

#### 4.3、可变参数

可变参数可以让我们把任意个数的参数打包到数组中传给函数，kotlin 的语法相比 Java 有所不同，改为通过使用 varage 关键字声明可变参数

例如，以下的几种函数调用方式都是正确的

```kotlin
fun main() {
    compute()
    compute("leavesC")
    compute("leavesC", "叶应是叶")
    compute("leavesC", "叶应是叶", "叶")
}

fun compute(vararg name: String) {
    name.forEach { println(it) }
}
```

在 Java 中，可以直接将数组传递给可变参数，而 kotlin 要求显式地解包数组，以便每个数组元素在函数中能够作为单独的参数来调用，这个功能被称为展开运算符，使用方式就是在数组参数前加一个 *

```kotlin
fun main() {
    val names = arrayOf("leavesC", "叶应是叶", "叶")
    compute(* names)
}

fun compute(vararg name: String) {
    name.forEach { println(it) }
}
```

#### 4.4、局部函数

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

#### 5.1、语句和表达式

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
++1     //自增，返回 2
//与 Java 不同，kotlin 中的 if 是作为表达式存在的，其可以拥有返回值
fun getLength(str: String?): Int {
    return if (str.isNullOrBlank()) 0 else str.length
}
```

#### 5.2、If 表达式

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

#### 5.3、when 表达式

when 表达式与 Java 中的 **switch/case** 类似，但是要强大得多。when 既可以被当做表达式使用也可以被当做语句使用，when 将参数和所有的分支条件顺序比较直到某个分支满足条件，然后它会运行右边的表达式。如果 when 被当做表达式来使用，符合条件的分支的值就是整个表达式的值，如果当做语句使用， 则忽略个别分支的值。与 Java 的 switch/case 不同之处是 When 表达式的参数可以是任何类型，并且分支也可以是一个条件

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

此外，When 循环也可以不带参数

```kotlin
    when {
        1 > 5 -> println("1 > 5")
        3 > 1 -> println("3 > 1")
    }
```

#### 5.4、for 循环

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

#### 5.5、while 和 do/while 循环

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

#### 5.6、返回和跳转

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

Ranges 表达式使用一个 **..**  操作符来声明一个闭区间，它被用于定义实现了一个 RangTo  方法

以下三种声明形式是等价的

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

Ranges 默认会自增长，所以如果像以下的代码就不会被执行

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

#### 7.1、final 和  oepn

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

#### 7.2、public

public 修饰符是限制级最低的修饰符，是默认的修饰符。如果一个定义为 public  的成员被包含在一个 private  修饰的类中，那么这个成员在这个类以外也是不可见的

#### 7.3、protected

protected 修饰符只能被用在类或者接口中的成员上。在 Java 中，可以从同一个包中访问一个 protected 的成员，但对于 kotlin 来说，protected 成员只在该类和它的子类中可见。此外，protected 不适用于顶层声明

#### 7.4、internal

一个定义为 internal 的包成员，对其所在的整个 module 可见。如果它是一个其它领域的成员，它就需要依赖那个领域的可见性了。比如，如果我们写了一个 private  类，那么它的 internal 修饰的函数的可见性就会限制于它所在的这个类的可见性

我们可以访问同一个 module  中的 internal  修饰的类，但是其它 module  是访问不到该 internal  类的，该修饰符可用于对外发布的开源库，将开源库中不希望被外部引用的代码设为 internal 权限，可避免对外部引用库造成混淆

> 根据 Jetbrains 的定义，一个 module  应该是一个单独的功能性的单位，可以看做是一起编译的 kotlin 文件的集合，它应该是可以被单独编译、运行、测试、debug 的。相当于在 Android Studio 中主工程引用的 module，Eclipse 中在一个 workspace 中的不同的 project

#### 7.5、private

private  修饰符是限制级最高的修饰符，kotlin 允许在顶层声明中使用 private 可见性，包括类、函数和属性，这表示只在自己所在的文件中可见，所以如果将一个类声明为 private，就不能在定义这个类之外的地方中使用它。此外，如果在一个类里面使用了 private  修饰符，那访问权限就被限制在这个类里面，继承这个类的子类也不能使用它。所以如果类、对象、接口等被定义为 private，那么它们只对被定义所在的文件可见。如果被定义在了类或者接口中，那它们只对这个类或者接口可见

#### 7.6、总结

| 修饰符         | 类成员       | 顶层声明     |
| -------------- | ------------ | ------------ |
| public（默认） | 所有地方可见 | 所有地方可见 |
| internal       | 模块中可见   | 模块中可见   |
| protected      | 子类中可见   |              |
| private        | 类中可见     | 文件中可见   |



### 八、空安全

#### 8.1、可空性

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

正确的做法事显式地进行 null 检查

```kotlin
fun check(name: String?): Boolean {
    if (name != null) {
        return name.isNotEmpty()
    }
    return false
}
```

#### 8.2、安全调用运算符：?.

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

#### 8.3、Elvis 运算符：?:

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

#### 8.4、安全转换运算符：as?

安全转换运算符：`as?` 用于把值转换为指定的类型，如果值不适合该类型则返回 null

```kotlin
fun check(any: Any?) {
    val result = any as? String
    println(result ?: println("is not String"))
}
```

#### 8.5、非空断言：!!

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

#### 8.6、可空类型的扩展

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

#### 8.7、平台类型

平台类型是 kotlin 对 java 所作的一种平衡性设计。kotlin 将对象的类型分为了可空类型和不可空类型两种，但 java 平台的一切对象类型均为可空的，当在 kotlin 中引用 java 变量时，如果将所有变量均归为可空类型，最终将多出许多 null 检查；如果均看成不可空类型，那么就很容易就写出忽略了NPE 风险的代码。为了平衡两者，kotlin 引入了平台类型，即当在 kotlin 中引用 java 变量值时，既可以将之看成可空类型，也可以将之看成不可空类型，由开发者自己来决定是否进行 null 检查

### 九、类型的检查与转换

#### 9.1、类型检查

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

#### 9.2、智能转换

在许多情况下，不需要在 kotlin 中使用显式转换操作符，因为编译器跟踪不可变值的 is 检查以及显式转换，并在需要时自动插入安全的转换

例如，对于以下例子来说，当判断 value 为 String 类型通过时，就可以直接将 value 当做 String 类型变量并调用其内部属性

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

#### 9.3、不安全的转换操作符

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

#### 9.4、安全的转换操作符

可以使用安全转换操作符 as? 来避免在转换时抛出异常，它在失败时返回 null

```kotlin
    val x = null
    val y: String? = x as? String
```

尽管以上例子 as? 的右边是一个非空类型的 String，但是其转换的结果是可空的



### 十、类

#### 10.1、基本概念

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

#### 10.2、主构造函数

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

#### 10.3、次构造函数

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

#### 10.4、属性

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

#### 10.5、自定义访问器

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

#### 10.6、延迟初始化

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

#### 11.1、抽象类

声明为 abstract 的类内部可以包含没有实现体的成员方法，且该成员方法也用 abstract 标记，这种类称为抽象类，包含的没有实现体的方法称为抽象方法

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

#### 11.2、数据类

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

#### 11.3、密封类

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

#### 11.4、枚举类

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

#### 11.5、匿名内部类

使用**对象表达式**来创建匿名内部类实例

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

#### 11.6、嵌套类

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

#### 11.7、内部类

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

### 十二、接口

#### 12.1、抽象方法与默认方法

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

#### 12.2、抽象属性

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

### 十三、继承

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

#### 13.1、覆盖方法

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

#### 13.2、属性覆盖

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

#### 13.3、调用超类实现

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

### 十四、集合

#### 14.1、只读集合与可变集合

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

    val mutableList = mutableListOf("leavesC", "叶应是叶", "叶")
    mutableList.add("Ye")
    println(mutableList.size)
    println(mutableList.contains("leavesC"))
```

#### 14.2、集合与 Java

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

#### 14.3、只读集合的可变性

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

#### 14.4、集合与可空性

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

