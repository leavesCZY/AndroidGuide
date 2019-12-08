> 本文已收录至学习笔记大全：[JavaKotlinAndroidGuide](https://github.com/leavesC/JavaKotlinAndroidGuide)
>
> 作者：[leavesC](https://github.com/leavesC)

[TOC]



### 十五、扩展函数和扩展属性

#### 15.1、扩展函数

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

#### 15.2、扩展属性

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

#### 15.3、不可重写的扩展函数

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

#### 15.4、可空接收者

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

### 十六、Lambda 表达式

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

### 十七、标准库中的扩展函数

kotlin 标准库中提供了几个比较实用的扩展函数，定义在 Standard 文件下

#### 17.1、run

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

#### 17.2、with

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

#### 17.3、apply

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

#### 17.4、also

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

#### 17.5、let 

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

#### 17.6、takeIf

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

#### 17.7、takeUnless

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

### 十八、函数操作符

#### 18.1、总数操作符

##### 18.1.1、any

如果至少有一个元素符合给出的判断条件，则返回 true

```kotlin
    val list = listOf(1, 3, 5, 7, 9)
    println(list.any { it > 13 })  //false
    println(list.any { it > 7 })   //true
```

##### 18.1.2、all

如果全部的元素符合给出的判断条件，则返回 true

```kotlin
    val list = listOf(1, 3, 5, 7, 9)
    println(list.all { it > 13 })  //false
    println(list.all { it > 0 })   //true
```

##### 18.1.3、count

返回符合给出判断条件的元素总数

```kotlin
    val list = listOf(1, 3, 5, 7, 9)
    println(list.count { it > 7 })  //1
    println(list.count { it > 2 })  //4
```

##### 18.1.4、fold

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

##### 18.1.5、foldRight

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

##### 18.1.6、forEach

```kotlin
    val list = listOf(1, 3, 5, 7, 9)
    list.forEach { print(it + 1) } //246810
```

##### 18.1.7、forEachIndexed

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

##### 18.1.8、max

返回最大的一项，如果没有则返回null

```kotlin
    val list = listOf(1, 3, 5, 7, 9)
    println(list.max()) //9
```

##### 18.1.9、maxBy

根据给定的函数返回最大的一项，如果没有则返回 null

```kotlin
    val list = listOf(1, 3, 5, 7, 9)
    println(list.maxBy { -it }) //1
```

##### 18.1.10、min

返回最小的一项，如果没有则返回null

```kotlin
    val list = listOf(1, 3, 5, 7, 9)
    println(list.min()) //1
```

##### 18.1.11、minBy

根据给定的函数返回最小的一项，如果没有则返回null

```kotlin
    val list = listOf(1, 3, 5, 7, 9)
    println(list.minBy { -it }) //9
```

##### 18.1.12、none

如果没有任何元素与给定的函数匹配，则返回true

```kotlin
    val list = listOf(1, 3, 5, 7, 9)
    println(list.none { it > 10 }) //true
```

##### 18.1.13、reduce

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

##### 18.1.14、reduceRight

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

##### 18.1.15、sumBy

返回所有每一项通过函数转换之后的数据的总和

```kotlin
    val list = listOf(1, 3, 5, 7, 9)
    println(list.sumBy { it + 1 }) //30
```

#### 18.2、过滤操作符

##### 18.2.1、drop

返回包含去掉前n个元素的所有元素的列表

```kotlin
    val list = listOf(1, 3, 5, 7, 9)
    println(list.drop(2)) //[5, 7, 9]
```

##### 18.2.2、dropWhile

返回从第一个开始不符合给定函数的元素起之后的列表

```kotlin
    val list = listOf(1, 3, 5, 7, 9, 2)
    println(list.dropWhile { it < 4 }) //[5, 7, 9, 2]
```

##### 18.2.3、dropLastWhile

从最后一项开始，返回从开始不符合给定函数的元素起之后的列表

```kotlin
    val list = listOf(10, 1, 3, 5, 7, 9)
    println(list.dropLastWhile { it > 4 }) //[10, 1, 3]
```

##### 18.2.4、filter

过滤所有符合给定函数条件的元素

```kotlin
    val list = listOf(1, 3, 5, 7, 9, 2)
    println(list.filter { it < 4 }) //[1, 3, 2]
```

##### 18.2.5、filterNot

过滤所有不符合给定函数条件的元素

```kotlin
    val list = listOf(1, 3, 5, 7, 9, 2)
    println(list.filterNot { it < 4 }) //[5, 7, 9]
```

##### 18.2.6、filterNotNull

过滤所有元素中不是null的元素

```kotlin
    val list = listOf(1, 3, 5, 7, 9, 2, null)
    println(list.filterNotNull()) //[1, 3, 5, 7, 9, 2]
```

##### 18.2.7、slice

过滤一个list中指定index的元素

```kotlin
    val list = listOf(1, 3, 5, 7, 9, 2, null)
    println(list.slice(listOf(0, 3))) //[1, 7]
```

##### 18.2.8、take

返回从第一个开始的n个元素

```kotlin
    val list = listOf(1, 3, 5, 7, 9, 2, null)
    println(list.take(2)) //[1, 3]
```

##### 18.2.9、takeLast

返回从最后一个开始的n个元素

```kotlin
    val list = listOf(1, 3, 5, 7, 9, 2, null)
    println(list.takeLast(2)) //[2, null]
```

##### 18.2.10、takeWhile

返回从第一个开始符合给定函数条件的元素。

```kotlin
    val list = listOf(1, 3, 5, -1, 7, 9, 2)
    println(list.takeWhile { it > 2 }) //[]
    println(list.takeWhile { it > 0 }) //[1, 3, 5]
```

#### 18.3、映射操作符

##### 18.3.1、flatMap

遍历所有的元素，为每一个创建一个集合，最后把所有的集合放在一个集合中

```kotlin
    val list = listOf(1, 3, 5, -1, 7, 9, 2)
    println(list.flatMap { listOf(it, it + 1) }) //[1, 2, 3, 4, 5, 6, -1, 0, 7, 8, 9, 10, 2, 3]
```

##### 18.3.2、groupBy

返回一个根据给定函数分组后的map 

```kotlin
    val list = listOf(1, 3, 5, -1, 7, 9, 2)
    println(list.groupBy { listOf(it) }) //{[1]=[1], [3]=[3], [5]=[5], [-1]=[-1], [7]=[7], [9]=[9], [2]=[2]}
    println(list.groupBy { listOf(it, it + 1) }) //{[1, 2]=[1], [3, 4]=[3], [5, 6]=[5], [-1, 0]=[-1], [7, 8]=[7], [9, 10]=[9], [2, 3]=[2]}
```

##### 18.3.3、map 

返回一个每一个元素根据给定的函数转换所组成的List。 

```kotlin
    val list = listOf(1, 3, 5, -1, 7, 9, 2)
    println(list.map { listOf(it) }) //[[1], [3], [5], [-1], [7], [9], [2]]
    println(list.map { listOf(it, it + 1) }) //[[1, 2], [3, 4], [5, 6], [-1, 0], [7, 8], [9, 10], [2, 3]]
```

##### 18.3.4、mapIndexed 

返回一个每一个元素根据给定的包含元素index的函数转换所组成的List

```kotlin
    val list = listOf(1, 3, 5, -1, 7, 9, 2)
    println(list.mapIndexed { index, value -> index }) //[0, 1, 2, 3, 4, 5, 6]
    println(list.mapIndexed { index, value -> index * value }) //[0, 3, 10, -3, 28, 45, 12]
```

##### 18.3.5、mapNotNull 

返回一个每一个非null元素根据给定的函数转换所组成的List

```kotlin
    val list = listOf(1, 3, 5, -1, 7, 9, null, 2)
    println(list.mapNotNull { it }) //[1, 3, 5, -1, 7, 9, 2]
```

#### 18.4、元素操作符 

##### 18.4.1、contains 

如果指定元素可以在集合中找到，则返回true

```kotlin
    val list = listOf(1, 3, 5, -1, 7, 9, null, 2)
    println(list.contains(3)) //true
    println(list.contains(13)) //false
```

##### 18.4.2、elementAt 

返回给定index对应的元素，如果index数组越界则会抛出 IndexOutOfBoundsException

```kotlin
    val list = listOf(1, 3, 5, -1, 7, 9, null, 2)
    println(list.elementAt(3)) //-1
    println(list.elementAt(6)) //null
```

##### 11.4.3、elementAtOrElse 

返回给定index对应的元素，如果index数组越界则会根据给定函数返回默认值

```kotlin
    val list = listOf(1, 3, 5, -1, 7, 9, null, 2)
    println(list.elementAtOrElse(3, { it * 2 }))  //-1
    println(list.elementAtOrElse(16, { it * 2 })) //32

```

##### 18.4.4、elementAtOrNull 

返回给定index对应的元素，如果index数组越界则会返回null

```kotlin
    val list = listOf(1, 3, 5, -1, 7, 9, null, 2)
    println(list.elementAtOrNull(3))  //-1
    println(list.elementAtOrNull(16)) //null
```

##### 18.4.5、first 

返回符合给定函数条件的第一个元素

```kotlin
    val list = listOf(1, 3, 5, -1, 7, 9, 2)
    println(list.first { it % 3 == 0 })  //3
```

##### 18.4.6、firstOrNull 

返回符合给定函数条件的第一个元素，如果没有符合则返回null

```kotlin
    val list = listOf(1, 3, 5, -1, 7, 9, 2)
    println(list.firstOrNull { it % 3 == 0 })  //3
    println(list.firstOrNull { it % 8 == 0 })  //null
```

##### 18.4.7、indexOf 

返回指定元素的第一个index，如果不存在，则返回 -1

```kotlin
    val list = listOf(1, 3, 5, -1, 7, 9, 2)
    println(list.indexOf(5))  //2
    println(list.indexOf(12)) //-1
```

##### 18.4.8、indexOfFirst 

返回第一个符合给定函数条件的元素的index，如果没有符合则返回 -1

```kotlin
    val list = listOf(1, 3, 5, 1, 7, 9, 2)
    println(list.indexOfFirst { it % 2 == 0 })   //6
    println(list.indexOfFirst { it % 12 == 0 })  //-1
```

##### 18.4.9、indexOfLast 

返回最后一个符合给定函数条件的元素的index，如果没有符合则返回 -1

```kotlin
    val list = listOf(1, 3, 5, 6, 7, 9, 2)
    println(list.indexOfLast { it % 2 == 0 })   //6
    println(list.indexOfLast { it % 12 == 0 })  //-1
```

##### 18.4.10、last 

返回符合给定函数条件的最后一个元素

```kotlin
    val list = listOf(1, 3, 5, 6, 7, 9, 2)
    println(list.last { it % 2 == 0 })   //2
    println(list.last { it % 3 == 0 })   //9
```

##### 18.4.10、lastIndexOf 

返回指定元素的最后一个index，如果不存在，则返回 -1

```kotlin
    val list = listOf(1, 3, 2, 6, 7, 9, 2)
    println(list.lastIndexOf(2))    //6
    println(list.lastIndexOf(12))   //-1
```

##### 18.4.11、lastOrNull 

返回符合给定函数条件的最后一个元素，如果没有符合则返回null

```kotlin
    val list = listOf(1, 3, 2, 6, 7, 9, 2)
    println(list.lastOrNull { it / 3 == 3 })    //9
    println(list.lastOrNull { it == 10 })       //null
```

##### 18.4.12、single 

返回符合给定函数的单个元素，如果没有符合或者超过一个，则抛出异常

```kotlin
    val list = listOf(1, 9, 2, 6, 7, 9, 2)
    println(list.single { it % 7 == 0 })  //7
    println(list.single { it == 2 })      //IllegalArgumentException
```

##### 18.4.13、singleOrNull 

返回符合给定函数的单个元素，如果没有符合或者超过一个，则返回null

```kotlin
    val list = listOf(1, 9, 2, 6, 7, 9, 2)
    println(list.singleOrNull { it % 7 == 0 })  //7
    println(list.singleOrNull { it == 2 })      //null
```

#### 18.5、生产操作符 

##### 18.5.1、partition 

把一个给定的集合分割成两个，第一个集合是由原集合每一项元素匹配给定函数条 件返回 true 的元素组成，第二个集合是由原集合每一项元素匹配给定函数条件返回 false 的元素组成

```kotlin
    val list = listOf(1, 9, 2, 6, 7, 9, 2)
    val (list1, list2) = list.partition { it % 2 == 0 }
    println(list1)  //[2, 6, 2]
    println(list2)  //[1, 9, 7, 9]
```

##### 18.5.2、plus 

返回一个包含原集合和给定集合中所有元素的集合，因为函数的名字原因，我们可以使用 + 操作符

```kotlin
    val list1 = listOf(1, 9, 2, 6, 7, 9, 2)
    val list2 = listOf(1, 2, 4, 6, 8, 10)
    println(list1.plus(list2)) //[1, 9, 2, 6, 7, 9, 2, 1, 2, 4, 6, 8, 10]
    println(list1 + list2)  //[1, 9, 2, 6, 7, 9, 2, 1, 2, 4, 6, 8, 10]
```

##### 18.5.3、zip 

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

##### 18.5.4、unzip 

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

#### 18.6、顺序操作符 

##### 18.6.1、reverse 

返回一个与指定list相反顺序的list

```kotlin
    val list1 = listOf(Pair("leavesC", 1), Pair("leavesC_2", 2), Pair("leavesC_3", 3))
    val list2 = list1.reversed()
    println(list2)      //[(leavesC_3, 3), (leavesC_2, 2), (leavesC, 1)]
```

##### 18.6.2、sort 

返回一个自然排序后的list

```kotlin
    val list1 = listOf(2, 4, 1, 9, 5, 10)
    val list2 = list1.sorted()
    println(list2) //[1, 2, 4, 5, 9, 10]

    val list3 = listOf("a", "c", "ab", "b", "cdd", "cda")
    val list4 = list3.sorted()
    println(list4) //[a, ab, b, c, cda, cdd]
```

##### 18.6.3、sortBy 

返回一个根据指定函数排序后的list 

```kotlin
    val list1 = listOf(2, 4, 1, 9, 5, 10)
    val list2 = list1.sortedBy { it - 3 }
    println(list2) //[1, 2, 4, 5, 9, 10]
```

##### 18.6.4、sortDescending 

返回一个降序排序后的List

```kotlin
    val list1 = listOf(2, 4, 1, 9, 5, 10)
    val list2 = list1.sortedDescending()
    println(list2) //[10, 9, 5, 4, 2, 1]
```

##### 18.6.5、sortDescendingBy 

返回一个根据指定函数降序排序后的list

```kotlin
    val list1 = listOf(2, 4, 1, 9, 5, 10)
    val list2 = list1.sortedByDescending { it % 2 }
    println(list2) //[1, 9, 5, 2, 4, 10]
```

### 十九、异常

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

### 二十、运算符重载

kotlin 允许为类型提供预定义的操作符实现，这些操作符具有固定的符号表示（例如 + 和 * ）和固定的优先级，通过操作符重载可以将操作符的行为映射到指定的方法。为实现这样的操作符，需要为类提供一个固定名字的成员函数或扩展函数，相应的重载操作符的函数需要用 operator 修饰符标记

#### 20.1、一元操作符

| 操作符 |      函数      |
| ------ | :------------: |
| +a     | a.unaryPlus()  |
| -a     | a.unaryMinus() |
| !a     |    a.not()     |
| a++    |    a.inc()     |
| a--    |    a.dec()     |

#### 20.2、二元操作符

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

#### 20.3、数组操作符

| 操作符               |          函数           |
| -------------------- | :---------------------: |
| a[i]                 |        a.get(i)         |
| a[i, j]              |       a.get(i, j)       |
| a[i_1, ..., i_n]     |  a.get(i_1, ..., i_n)   |
| a[i] = b             |       a.set(i, b)       |
| a[i, j] = b          |     a.set(i, j, b)      |
| a[i_1, ..., i_n] = b | a.set(i_1, ..., i_n, b) |

#### 20.4、等于操作符

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

#### 20.5、比较操作符

| 操作符 |        函数         |
| ------ | :-----------------: |
| a > b  | a.compareTo(b) > 0  |
| a < b  | a.compareTo(b) < 0  |
| a >= b | a.compareTo(b) >= 0 |
| a <= b | a.compareTo(b) <= 0 |

所有的比较都转换为对  compareTo  的调用，这个函数需要返回  Int  值

#### 20.6、函数调用

| 方法             |          调用           |
| ---------------- | :---------------------: |
| a()              |       a.invoke()        |
| a(i)             |       a.invoke(i)       |
| a(i, j)          |     a.invoke(i, j)      |
| a(i_1, ..., i_n) | a.invoke(i_1, ..., i_n) |

#### 20.7、例子

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

### 二十一、中缀调用与解构声明

#### 21.1、中缀调用

可以以以下形式创建一个 Map 变量

```kotlin
fun main() {
    val maps = mapOf(1 to "leavesC", 2 to "ye", 3 to "czy")
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

#### 21.2、解构声明

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

### 二十二、Object 关键字

#### 22.1、对象声明

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

#### 22.2、伴生对象

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

##### 22.2.1、工厂模式

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

##### 22.2.2、指定名称

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

##### 22.2.3、实现接口

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

#### 22.3、对象表达式

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

### 二十三、委托

#### 23.1、委托模式

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

#### 23.2、属性委托

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

#### 23.3、延迟属性

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

#### 23.4、可观察属性

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

#### 23.5、把属性储存在映射中

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

#### 23.6、局部委托属性

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

### 二十四、注解

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