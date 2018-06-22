Java 从 JDK 5 开始支持注解，用于在源文件中嵌入补充信息，注解不会改变程序的动作，因此也不会改变程序的语义。

### 一、注解的基础知识
注解是通过基于接口的机制创建的，看如下这个例子
```
public @interface Vocation {

    String name();

    int age();

}
```
通过 **@interface** 来声明注解类型，此外，注解 Vocation 还包括两个变量（虽然看起来比较像方法声明）：name 和 age ，如果没有提供默认值，在声明注解时需要都为之赋值。

所有的注解类型都自动扩展了 Annotation 接口，即 Annotation 是所有注解的超接口。在声明注解之后，就可以用来注解声明了。所有类型的声明都可以有与之关联的注解。例如，类、方法、域变量、参数以及枚举常量都可以带有注解，甚至注解本身也可以被注解。对于所有情况，注解都要放在声明的最前面。

声明注解的语法如下所示。注解的名称以 @ 作为前缀，后面跟位于圆括号中的成员初始化列表。
```
    @Vocation(name = "monkey", age = 24)
    public static void annotationTest() {
        
    }
```

### 二、指定保留策略
Java 定义了三种注解保留策略，用于决定在什么位置丢弃注解。三种策略分别是 **SOURCE 、CLASS、RUNTIME**，即 RetentionPolicy 的枚举值

 - SOURCE 表示注解只在源文件中保留，在编译期间会被抛弃
 - CLASSS 表示注解在编译时会被存储到 .class 文件中，但在运行时通过 JVM 不能得到注解
 - RUNTIME 表示注解在编译时会被存储到 .class 文件中，并且在运行时可以通过 JVM 获取到注解，提供了最久远的注解

保留策略是通过Java的内置注解 **@Retention** 指定的，如果没有为注解指定保留策略，将使用默认策略 **CLASS**

```
@Retention(RetentionPolicy.RUNTIME)
public @interface Vocation {

    String name();

    int age();

}
```
### 三、在运行时使用反射获取注解
注解的目的主要是用于其他的开发和部署工具，但如果为注解指定 RUNTIME 保留策略，那么程序就在运行时都可以使用反射来查询注解。
反射可以在运行时获取类相关信息的特征，使用反射需要获取到 Class 对象，即使用了注解的类，通过 Class 对象就可以通过其内置的一些方法获取到特定条目的对象。例如，**通过 getMethod() 获取到方法信息，通过 getField() 获取到域变量信息，通过 getConstructor() 获取到构造函数信息。**

#### 3.1、获取方法注解
例如，getMethod() 方法签名如下所示：

```
public Method getMethod(String name, Class<?>... parameterTypes)
```
name 参数代表方法名，parameterTypes 时可变长度参数，代表该方法名可能需要的参数类型的 Class 对象。如果没有找到指定方法，则抛出 NoSuchMethodException 异常。
如果获取到了 Method 对象，则调用其 **getAnnotation()** 方法，获得与对象关联的特定注解

```
public <T extends Annotation> T getAnnotation(Class<T> annotationClass)
```
参数 annotationClass 代表想要获取到的注解的 Class 对象，方法返回对注解的一个引用，通过注解可以获取与注解成员关联的值，如果没有找到注解，则返回 null 。如果注解的保留策略不是 RUNTIME ，就会出现这种情况

```
@Retention(RetentionPolicy.RUNTIME)
public @interface Vocation {

    String name();

    int age();

}
```

```
public class Main {

    @Vocation(name = "monkey1", age = 24)
    public static void annotationTest1(String content) {
        System.out.println(content);
    }

    @Vocation(name = "monkey2", age = 25)
    public static void annotationTest2() {

    }

    public static void main(String[] args) {
        Main main = new Main();
        Class<?> cl = main.getClass();
        try {
            Method method = cl.getMethod("annotationTest1", String.class);
            Vocation vocation = method.getAnnotation(Vocation.class);
            System.out.println("name1: " + vocation.name());
            System.out.println("age: " + vocation.age());
            
            System.out.println();
            
            method = cl.getMethod("annotationTest2");
            vocation = method.getAnnotation(Vocation.class);
            System.out.println("name2: " + vocation.name());
            System.out.println("age: " + vocation.age());
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

}
```
输出结果为：
![](http://upload-images.jianshu.io/upload_images/2552605-169bd8699091a9a1.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

#### 3.2、获取所有注解
此外，也可以获取某个条目下具有 RUNTIME 保留策略的所有注解，通过 **getAnnotations()** 方法来获取
先声明三个注解类型，注解 Date 指定保留策略为 CLASS
```
@Retention(RetentionPolicy.RUNTIME)
public @interface Vocation {

    String name();

    int age();

}

@Retention(RetentionPolicy.RUNTIME)
public @interface Fruit {

    String name();

}

@Retention(RetentionPolicy.CLASS)
public @interface Date {

    String time();

}
```

```
public class Main {

    @Vocation(name = "monkey", age = 24)
    @Fruit(name = "banana")
    @Date(time = "today")
    public String annotation;

    public static void main(String[] args) {
        Main main = new Main();
        Class<?> cl = main.getClass();
        try {
            Field field = cl.getField("annotation");
            Annotation[] annotations = field.getAnnotations();
            for (Annotation annotation : annotations) {
                System.out.println(annotation.annotationType());
                System.out.println(annotation);
                System.out.println();
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

}
```

从运行结果可以看到没有获取到注解 Date
![](http://upload-images.jianshu.io/upload_images/2552605-ebef37b2f4febd8e.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

#### 3.3、判断某条目是否有声明指定注解
可以通过 isAnnotationPresent 判断指定注解是否与调用对象相关联，即判断是否有使用该注解声明调用对象

```
public class Main {

    @Vocation(name = "monkey", age = 24)
    public String annotation;

    public static void main(String[] args) {
        Main main = new Main();
        Class<?> cl = main.getClass();
        try {
            Field field = cl.getField("annotation");
            System.out.println("isAnnotationPresent Vocation: " + field.isAnnotationPresent(Vocation.class));
            System.out.println("isAnnotationPresent Fruit: " + field.isAnnotationPresent(Fruit.class));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

}
```
运行结果
![](http://upload-images.jianshu.io/upload_images/2552605-6ac7a82923a31fdf.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

### 四、为注解设置默认值
在应用注解时如果不想每次都显示赋值，那么可以为注解成员提供默认值。
设置默认值的语法如下所示：

```
@Retention(RetentionPolicy.RUNTIME)
public @interface Vocation {

    String name() default "monkey";

    int age() default 24;

}
```
在应用注解时，如果没有再次赋值，则 name 默认值就是 “monkey”，age 默认值就是 24

此外，如果注解只包含一个成员，应用注解时就可以不指定成员名称而直接为该成员赋值。但是，为了使用这种缩写形式，成员名称必须是 **value**。还有一种情况可以使用这种缩写方式，就是注解可以包含多个成员，但除了 value 成员外其他成员都直接使用声明的默认值。

```
@Retention(RetentionPolicy.RUNTIME)
public @interface Fruit {

    String value();

}

@Retention(RetentionPolicy.RUNTIME)
public @interface Vocation {

    String name() default "monkey";

    int value();

}
```

```
public class Main {

    @Fruit("apple")
    @Vocation(22)
    public String annotation;

    public static void main(String[] args) {
        Main main = new Main();
        Class<?> cl = main.getClass();
        try {
            Field field = cl.getField("annotation");
            Annotation[] annotations = field.getAnnotations();
            for (Annotation annotation : annotations) {
                System.out.println(annotation.annotationType());
                System.out.println(annotation);
                System.out.println();
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

}
```
运行结果
![](http://upload-images.jianshu.io/upload_images/2552605-af3cfcedba3743d5.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
