## 一、Package

Kotlin 文件都能以一条 **package** 语句开头，而文件中定义的所有声明（类、函数和属性）都会被放到这个包中。如果其他文件中定义的声明也有相同的包，这个文件可以直接使用它们，如果包不相同，则需要导入它们

包的声明应处于源文件顶部，import 语句紧随其后

```kotlin
package base

import java.text.SimpleDateFormat
import java.util.*
```
Kotlin 不区分导入的是类还是函数，允许使用 import 关键字导入任何种类的声明。此外，也可以在包名称后加上 .* 来导入特定包中定义的所有声明，这不仅会让包中定义的类可见，也会让顶层函数和属性可见

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

fun main(args: Array<String>) {
    val point = Point(10, index)
    Test(true)
}
```

如果出现名字冲突，可以使用  as  关键字在本地重命名冲突项来消歧义

```kotlin
import learn.package1.Point
import learn.package2.Point as PointTemp //PointTemp 可以用来表示 learn.package2.Point 了
```

在 Java 中，要把类放到和包结构**匹配**的文件与目录结构中，而 Kotlin 允许把多个类放到同一个文件中，文件名也可以任意选择。Kotlin 也没有对磁盘上源文件的布局强加任何限制，包层级结构不需要遵循目录层级结构 ,但最好还是遵循 Java 的目录布局并根据包结构把源码文件放到相应的目录中

## 二、修饰符

### 2.1、final 和  oepn

Kotlin 中的类和方法默认都是 final 的，即不可继承的，如果想允许创建一个类的子类，需要使用 open 修饰符来标识这个类，此外，也需要为每一个希望被重写的属性和方法添加 open 修饰符

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

### 2.2、public

public 修饰符是限制级最低的修饰符，是默认的修饰符。如果一个定义为 public  的成员被包含在一个 private  修饰的类中，那么这个成员在这个类以外也是不可见的

### 2.3、protected

protected 修饰符只能被用在类或者接口中的成员上。在 Java 中，可以从同一个包中访问一个 protected 的成员，但对于 Kotlin 来说，protected 成员只在类和它的子类中可见。此外，protected 不适用于顶层声明

### 2.4、internal

如果是一个定义为 internal 的包成员的话，对所在的整个 module 可见。如果它是一个其它领域的成员，它就需要依赖那个领域的可见性了。比如，如果我们写了一个 private  类，那么它的 internal 修饰的函数的可见性就会限制于它所在的这个类的可见性

我们可以访问同一个 module  中的 internal  修饰的类，但是不能访问其它 module  的

> 根据 Jetbrains 的定义，一个 module  应该是一个单独的功能性的单位，它应该是可以被单独编译、运行、测试、debug 的。根据我们项目不同的模块，可以在 Android Studio 中创建不同的 module。在Eclipse中，这些 module  可以认为是在一个 workspace  中的不同的 project

### 2.5、private

private  修饰符是限制级最高的修饰符，Kotlin 允许在顶层声明中使用 private 可见性，包括类、函数和属性，这表示只在自己所在的文件中可见，所以如果将一个类声明为 private，就不能在定义这个类之外的地方中使用它。此外，如果在一个类里面使用了 private  修饰符，那访问权限就被限制在这个类里面，继承这个类的子类也不能使用它。所以如果类、对象、接口等被定义为 private，那么它们只对被定义所在的文件可见。如果被定义在了类或者接口中，那它们只对这个类或者接口可见



| 修饰符         | 类成员       | 顶层声明     |
| -------------- | ------------ | ------------ |
| public（默认） | 所有地方可见 | 所有地方可见 |
| internal       | 模块中可见   | 模块中可见   |
| protected      | 子类中可见   |              |
| private        | 类中可见     | 文件中可见   |



