### with与apply函数

#### with函数

with函数用于对同一个对象执行多次操作而不需要反复把对象的名称写出来

例如，为了构建一个包含指定内容的字符串，需要先后如下调用

```kotlin
fun main(args: Array<String>) {
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

with 结构其实是一个接受两个参数的函数，在这个例子中就是一个 StringBuilder 和一个 Lambda 表达式，这里利用了把 Lambda 表达式放在括号外的约定

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
    println(result)  //kotlin.Unit
```

#### apply函数

apply函数和with函数的唯一区别在于：apply函数始终会返回作为实参传递给它的对象

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

apply函数被声明为一个扩展函数，它的接收者变成了作为实参的 Lambda 的接受者