> 以下内容是我对 **Java 8 编程参考官方教程（第9版）** 该书的读书笔记

### 一、概述

泛型是在 JDK 1.5 引入的，泛型的意思是参数化类型，通过泛型可以创建以类型安全的方法使用各种类型数据的类、接口以及方法，能够使一份算法独立于特定的数据类型，然后将算法应用于各种数据类型而不需要做额外的各种。
Object是所有其他类的超类，Object引用变量可以引用所有类型的对象，因此通过操作Object类型的引用，Java总是可以操作一般化的类、接口以及方法，但它们不能以类型安全的方式进行工作。
泛型提供了以前缺失的类型安全性，并且不再需要显式地使用强制类型转换，所有的类型转换都是自动和隐式进行的。

### 二、泛型示例
#### 2.1、带一个参数类型的泛型类
下面看一个简单的泛型类 User 

```
public class User<T> {

    private T t;

    public User(T t) {
        this.t = t;
    }

    public void showType() {
        System.out.println("T: " + t);
        System.out.println("Name: " + t.getClass().getName());
    }

}
```
其中，T是实际参数类型的占位符，当创建对象时，就会将实际类型传给 User。在 showType() 中输出参数值和参数值的类对象名称

```
public class GenericMain {

    public static void main(String[] args) {
        User<String> stringUser = new User<>("leavesC");
        stringUser.showType();

        System.out.println();

        User<Integer> integerUser = new User<>(24);
        integerUser.showType();
    }

}
```
运行结果
![image](http://upload-images.jianshu.io/upload_images/2552605-87c3cbd18351fdca?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

#### 2.2、带多个参数类型的泛型类
在泛型类中可以声明多个类型参数，通过逗号分隔参数列表

```
public class User<T, K> {

    private T t;

    private K k;

    public User(T t, K k) {
        this.t = t;
        this.k = k;
    }

    public void showType() {
        System.out.println("T: " + t);
        System.out.println("T Name: " + t.getClass().getName());
        System.out.println("K: " + k);
        System.out.println("K Name: " + k.getClass().getName());
    }

}
```

```
public class GenericMain {

    public static void main(String[] args) {
        User<String, Integer> stringUser = new User<>("leavesC", 24);
        stringUser.showType();

        System.out.println();

        User<String, Double> integerUser = new User<>("叶应是叶", 24.0);
        integerUser.showType();
    }

}
```
运行结果
![image](http://upload-images.jianshu.io/upload_images/2552605-79e14024a0a4c75c?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

此处指定了两个类型参数：T 和 K，在创建User对象时就需要为之传递两个类型参数，但不要求两个类型参数必须是相同的类型

#### 2.3、泛型接口
除了可以定义泛型类外，还可以定义泛型接口

例如，定义一个泛型接口
```
public interface Control<T> {

    T test();

}
```
在泛型类中实现泛型接口

```
public class User<T, K> implements Control<T> {

    private T t;

    private K k;

    public User(T t, K k) {
        this.t = t;
        this.k = k;
    }

    public void showType() {
        System.out.println("T: " + t);
        System.out.println("T Name: " + t.getClass().getName());
        System.out.println("K: " + k);
        System.out.println("K Name: " + k.getClass().getName());
    }

    @Override
    public T test() {
        return null;
    }
    
}
```
在非泛型类中实现泛型接口

```
public class Stats implements Control<String> {

    @Override
    public String test() {
        return null;
    }
    
}
```

### 三、泛型只能使用引用类型
当声明泛型类的实例时，传递的类型参数必须是引用类型，不能使用基本类型
例如，对于 User 泛型类来说，以下声明是非法的

```
User<int,double> user=new User<>(1,10,0);
```
IDE 会提示类型参数不能是基本数据类型
![image](http://upload-images.jianshu.io/upload_images/2552605-daf7e4beac1467e6?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

可以使用类型封装器封装基本类型来解决该问题

### 四、有界类型
对于 User 泛型类来说，可以使用任何类代替类型参数，但有时候我们也会有限制能够传递给类型参数的类型的需求。例如，希望创建一个对于所有的数值类型都能够计算平均值的方法，包括整数、单精度浮点数和双精度浮点数等。

对于所有的数值类，比如 Integer 以及 Double，都是 Number 类的子类，而 Number 类定义了 doubleValue() 方法，所以所有的数值封装器都可以使用该方法。但编译器不知道我们试图创建的是只使用数值类型的 Stats 对象，所以  doubleValue() 方法对编译器来说是未知方法，因此在试图编译时以下泛型类会报错。
```
public class Stats<T> {

    private T[] numbers;

    public Stats(T[] numbers) {
        this.numbers = numbers;
    }

    public double average() {
        double sum = 0;
        for (T t : numbers) {
            //错误，因为并不是每种类型参数都包含 doubleValue 方法
            //需要我们主动告诉编译器我们传入的类型参数都是Number类的子类
            sum += t.doubleValue();
        }
        return sum / numbers.length;
    }

}
```
为了处理这种情况，需要为泛型类提供有界类型。在指定类型参数时，可以创建声明超类的上界，所有类型参数都必须派生自超类，通过关键字 extends 来声明限制。

这样，T 只能被 Number 类或其子类替代，阻止创建非数值类型的Stats对象
```
public class Stats<T extends Number> {

    private T[] numbers;

    public Stats(T[] numbers) {
        this.numbers = numbers;
    }

    public double average() {
        double sum = 0;
        for (T t : numbers) {
            sum += t.doubleValue();
        }
        return sum / numbers.length;
    }

}
```

```
public class GenericMain {

    public static void main(String[] args) {
        Integer[] intNumbers = {1, 2, 3, 4};
        Stats<Integer> integerStats = new Stats<>(intNumbers);
        System.out.println("int numbers average: " + integerStats.average());

        System.out.println();

        Double[] doubleNumbers = {1.0, 2.0, 3.0, 4.0};
        Stats<Double> doubleStats = new Stats<>(doubleNumbers);
        System.out.println("double numbers average: " + doubleStats.average());
    }

}
```
运行结果
![image](http://upload-images.jianshu.io/upload_images/2552605-d4a66060279862f0?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

此外，除了使用类作为边界外，边界也可以包含一个类和一个或多个接口。声明方式是：先指定类类型，再使用 & 运算符连接接口
例如

```
	class Test<T extends MyClass & MyInterface1 & MyInterface2>
```

### 五、使用通配符参数
#### 5.1、一般用法
现在来为上一节的 Stats 泛型类添加一个比较 Stats 对象之间包含的数组的平均值是否相等的方法。
这看起来可能并不麻烦，似乎以下的代码就可以满足需求

```
public class Stats<T extends Number> {

    private T[] numbers;

    public Stats(T[] numbers) {
        this.numbers = numbers;
    }

    public double average() {
        double sum = 0;
        for (T t : numbers) {
            sum += t.doubleValue();
        }
        return sum / numbers.length;
    }

    public boolean sameAvg(Stats<T> ob) {
        return average() == ob.average();
    }

}
```
但在使用的过程中，你就会发现 sameAvg 方法给我们带来了极大的限制，因为 sameAvg() 方法的参数值泛型类型被指定为和被调用对象的参数类型是相同的

如下的调用方式会使编译器提示错误。因此，这种方式的适用范围很窄，无法得到泛型化的解决方案。

![image](http://upload-images.jianshu.io/upload_images/2552605-17f750ed5d3660a9?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

为了创建泛型化的 sameAvg() 方法，必须使用泛型的另一个特性：通配符参数。通配符参数由 “?” 指定，表示未知类型。在此，Stats< ? > 和所有的 Stats 对象相匹配，允许任意两个Stats对象比较它们的平均值。通配符只是简单地匹配所有有效的Stats对象

```
    public boolean sameAvg(Stats<?> ob) {
        return average() == ob.average();
    }
```
#### 5.2、有界通配符
如果要为通配符建立上边界，可以使用如下所示的通配符表达式：

```
 <? extends superClass>
```
其中，superClass 是作为上界的类的名称。这是一条包含语句，即形成上界的类（即superClass）也包含于边界之内

此外还可以为通配符添加一条super子句，为通配符指定下界

```
<? super subClass>
```
其中，只有 subClass 的超类是可接受参数。这是一条排除子句，即 subClass 指定的类不包含在内

### 六、创建泛型方法
泛型类是在实例化类的时候指明参数的具体类型，而泛型方法是在调用方法的时候指明参数的具体类型 。
创建泛型方法需要依照一定的规则：可以在非泛型类中创建泛型方法，类型参数列表要位于返回类型之前
类似于

```
    public <T> void mothodName(T t) {
        
    }
```
在泛型类中声明的一般方法与泛型方法是有一些概念与使用区别的
```
public class Stats<T> {

    private T[] numbers;

    public Stats(T[] numbers) {
        this.numbers = numbers;
    }

    //这不是泛型方法，只是使用了 T 作为泛型参数而已
    public double average(T t) {
        return 1;
    }

    //这是一个泛型参数，
    //在泛型类 Stats<T> 中声明了一个泛型方法，使用泛型 K，泛型 K 可以为任意类型，既可以与 T 相同，也可以不同
    //由于泛型方法声明了泛型参数<K>，因此即使在泛型类 Stats<T> 中并未声明 K，编译器也能够正确识别出泛型方法中使用到的参数类型
    public <K> void mothodName(K t) {

    }

}
```

### 七、使用泛型的限制
#### 7.1、模糊性错误
看如下例子
对泛型类 User< T, K > 而言，声明了两个泛型类参数：T 和 K。在类中试图根据类型参数的不同重载 set() 方法。这看起来没什么问题，可编译器会报错
```
public class User<T, K> {
    
    //重载错误
    public void set(T t) {
        
    }

	//重载错误
    public void set(K k) {

    }

}
```
首先，当声明 User 对象时，T 和 K 实际上不需要一定是不同的类型，以下的两种写法都是正确的

```
public class GenericMain {

    public static void main(String[] args) {
        User<String, Integer> stringIntegerUser = new User<>();
        User<String, String> stringStringUser = new User<>();
    }

}
```
对于第二种情况，T 和 K 都将被 String 替换，这使得 set() 方法的两个版本完全相同，所以会导致重载失败。

此外，对 set() 方法的类型擦除会使两个版本都变为如下形式：

```
	public void set(Object o) {
        
    }
```
一样会导致重载失败。

#### 7.2、不能实例化类型参数
不能创建类型参数的实例。因为编译器不知道创建哪种类型的对象，T 只是一个占位符
```
public class User<T> {

    private T t;

    public User() {
        //错误
        t = new T();
    }


}
```

#### 7.3、对静态成员的限制
静态成员不能使用在类中声明的类型参数，但是可以声明静态的泛型方法
```
public class User<T> {

    //错误
    private static T t;

    //错误
    public static T getT() {
        return t;
    }

    //正确
    public static <K> void test(K k) {

    }

}
```

#### 7.4、对泛型数组的限制
不能实例化元素类型为类型参数的数组，但是可以将数组指向类型兼容的数组的引用

```
public class User<T> {

    private T[] values;

    public User(T[] values) {
        //错误，不能实例化元素类型为类型参数的数组
        this.values = new T[5];
        //正确，可以将values 指向类型兼容的数组的引用
        this.values = values;
    }

}
```
此外，不能创建特定类型的泛型引用数组，但使用通配符的话可以创建指向泛型类型的引用的数组

```
public class User<T> {

    private T[] values;

    public User(T[] values) {
        this.values = values;
    }

}
```

```
public class GenericMain {

    public static void main(String[] args) {
        //错误，不能创建特定类型的泛型引用数组
        User<String>[] stringUsers = new User<>[10];
        //正确，使用通配符的话，可以创建指向泛型类型的引用的数组
        User<?>[] users = new User<?>[10];
    }

}
```

#### 7.5、对泛型异常的限制
泛型类不能扩展 Throwable，意味着不能创建泛型异常类


#### 我的GitHub主页： [leavesC](https://github.com/leavesC)
#### 我的简书主页： [叶应是叶](https://www.jianshu.com/u/9df45b87cfdf)
