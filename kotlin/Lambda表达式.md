### Lambda 表达式

#### 简介

Lambda 表达式本质上就是可以传递给其它函数的一小段代码，通过 Lambda 表达式可以把通用的代码结构抽取成库函数，也可以把 Lambda 表达式存储在一个变量中，把这个变量当做普通函数对待

Kotlin 中的 Lambda 表达式始终用花括号包围，通过箭头把实参列表和函数体分开

```kotlin
    val plus = { x: Int, y: Int -> x + y }
    println(plus(20, 22))   //42
```

#### 语法

Lambda 表达式最常见的用途就是和集合一起工作，看以下例子

要从一个人员列表中取出年龄最大的一位

```kotlin
data class Person(val name: String, val age: Int)

fun main(args: Array<String>) {
    val people = listOf(Person("leavesC", 24), Person("Ye", 22))
    println(people.maxBy { it.age }) //Person(name=leavesC, age=24)
}
```

当中，库函数 maxBy 可以在任何集合上调用，其需要一个实参：一个函数，用于指定要用来进行比较的函数。花括号中的代码 { it.age } 就是实现了这个逻辑的 Lambda 表达式

上述 maxBy 函数的实参是简化后的写法，这里来看下 maxBy 函数的简化过程

最原始的语法声明应该是这样的，用括号包裹着 Lambda 表达式

```kotlin
println(people.maxBy({ p: Person -> p.age }))
```

Kotlin 有一种语法约定，如果 Lambda 表达式是函数调用的最后一个实参，可以将之放到括号的外边

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

#### 在作用域中访问变量

Kotlin 和 Java 的一个显著区别就是，在 Kotlin 中函数内部的 Lambda 表达式不会仅限于访问函数的参数以及 final 变量，在 Lambda 内部也可以访问并修改非 final 变量

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

#### 成员引用

成员引用用于创建一个调用单个方法或者访问单个属性的函数值，通过双冒号把类名称和要引用的成员（一个方法或者一个属性）名称分隔开

成员引用的一个用途就是：如果要当做参数传递的代码块已经被定义成了函数，此时不必专门创建一个调用该函数的 Lambda 表达式，可以直接通过成员引用的方式来传递该函数（也可以传递属性）。此外，成员引用对扩展函数一样适用

```kotlin
data class Person(val name: String, val age: Int) {

    val myAge = age

    fun getPersonAge() = age
}

fun Person.filterAge() = age

fun main(args: Array<String>) {
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

fun main(args: Array<String>) {
    val t = ::test
}
```

也可以用构造方法引用存储或者延期执行创建类实例的动作

```kotlin
data class Person(val name: String, val age: Int)

fun main(args: Array<String>) {
    val createPerson = ::Person
    val person = createPerson("leavesC", 24)
    println(person)
}
```

