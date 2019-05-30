## **一、基本类型**

Java有八种基本类型，其中每一种都有特定的格式和大小

|基本类型|说明
|:----:|:----:
|byte|字节长度的整数，八位
|short|短整数，十六位
|int|整数，三十二位
|long|长整数，六十四位
|float|单精度浮点数，三十二位
|double|双精度浮点数，六十四位
|char|Unicode字符
|boolean|布尔值


## **二、字面值**

基本类型的字面值有四种子类型：整数字面值，浮点数字面值，字符字面值，布尔字面值

### **2.1、整数字面值**

整数字面值可以写为十进制，十六进制（加前缀0x或0X），八进制（加前缀0），二进制（加前缀0B或0b）

```java
		//十进制
		int x=10;
		//十六进制数，即十进制的32
		int y=0x20;
		//八进制，即十进制的15
		int z=017;
		//二进制，即十进制的7
		int u=0b0111;
```

整数字面值用于将值赋给byte，short，int和long类型的变量。所赋值不能超出变量的存储范围

例如，以下代码在IDE中就会提示错误，因为byte的最大值为127

```java
		byte b=250;
```

要将一个值赋给long类型时，在数字的后面要加上后缀字母L或l，否则，如下代码中的整数值其实是被看做是int类型的

```java
		long a=120;
```
在以下代码中，就会产生一个错误，因为99999999999超出了int类型的存储能力

```java
		long a=99999999999;
```

为了解决这个问题，需要在数字后加上后缀字母L或l

如果整数字面值太长，可读性会受到影响。从Java 7开始，可用在整数字面值中使用下划线将数字分隔开

```java
		//十进制
		int x=1_000_000;
		//十六进制数，即十进制的32
		int y=0x2_0;
		//八进制，即十进制的15
		int z=0_17;
		//二进制，即十进制的7
		int u=0b0_111;
```

### **2.2、浮点数字面值**

浮点数包含以下四个部分

 - 一个整数部分
 - 一个小数点
 - 一个小数部分
 - 一个可选的指数

例如，在1.7e8中，1是整数部分，7是小数部分，8是指数

在float和double类型中，0的整数部分是可选的

例如，**0.5**可以写成**.5**

浮点数字面值加上的后缀字母F或f表明其为float类型，如果没有标明，该浮点数字面值将是double类型

### **2.3、字符字面值**

字符字面值是一个Unicode字符，或者是单引号括起来的一个转义序列

例如

```java
'a'
'b'
'\b' 回退字符
'\n' 换行
'\r' 回车
```

### **2.4、布尔字面值**

布尔类型有两个值，分别为true和false

例如

声明一个布尔变量bool

```java
	boolean bool=true;
```

## **三、基本类型转换**

在涉及处理不同数据类型时，常常需要将一个变量的值赋给另一个变量，这就需要进行类型转换

### **3.1、加宽转换**

当从一种基本类型转换向另一种基本类型时，如果后者的大小和前者相同或者更大，就叫做加宽转换

例如，将int（32位）转为long（64位），此时不会有信息丢失的风险，且加宽转换是隐式发生的，不需要在代码中任何事情

例如

```java
	int a=10;
	long b=a;
```

### **3.2、收窄转换**

收窄转换发生在从一种基本类型转换为另一种更小类型的转换中，例如，从long（64位）到int（32位），收窄转换需要显示调用，用圆括号指定目标类型

```java
	long a=10;
	int b=(int)a;
```

如果被转换的值比目标类型的容量还要大的话，收窄转换将导致信息丢失，例如，9876543210对int类型来说太大了

```java
		long a=9876543210L;
		int b=(int)a;
		//输出值是1286608618
		System.out.println(b);
```

## **四、提升**

一些一元操作符（如+、-和~）和二元操作符（+、-、*、/），会导致自动提升，运算结果变成一种更大的类型
例如

```java
		byte x=10;
		byte y=+x;   //此处会报错
		byte z=-x;   //此处会报错
```

第二行和第三行会产生错误，因为该操作符使运算结果提升为int类型，要修复这个问题，需要进行收窄转换

```java
		byte x=10;
		byte y=(byte) +x;
		byte z=(byte) -x;
```

对于一元操作符来说，如果操作数的类型是byte，short或char，运算结果提升为int类型

对与二元操作符来说，提升规则是从以下几条依次选择一条执行

 - 如果操作数类型均为byte或short，那么两个数均转为int类型，结果数也将为int类型
 - 如果操作数包含double类型，那么另一个操作数也转为double，结果数也将为double类型
 - 如果操作数包含float类型，那么另一个操作数也转为float，结果数也将为float类型
 - 如果操作数包含long类型，那么另一个操作数也转为long，结果数也将为long类型

## **五、对象的内存分配**

当在类中声明了一个变量时，不管是在类级别还是方法级别，都将为赋值给该变量的数据分配内存空间。当超出变量的作用域后，Java会自动释放掉为该变量分配的内存空间，该内存空间可以立刻被另作他用

对于基本数据类型，容易计算其占用的内存量

例如，声明一个int类型会占用四个字节，声明一个double八个字节，但计算引用变量所需的空间则不同

当一个程序运行时，会为数据分配一些内存。这些数据空间从逻辑上分为两类，栈和堆

在函数中定义的基本类型变量和对象的引用变量是在函数的栈内存中分配。堆内存用于存放new出来的对象以及数组，在堆中分配的内存，由JVM自动垃圾回收器来管理。在堆中产生的对象和数组，可以在栈中定义一个变量，该变量的取值就等于对象或数组在堆内存中的首地址。如此一来，该变量就变成了对象或数组的引用变量，以后就可以在程序中使用栈内存中的引用变量来访问堆中的对象或数组

假设有一个名为Book的类

```java
	Book myBook=new Book();
	Book yourBook=myBook;
```

第二行代码将myBook的值赋给了yourBook，则myBook和yourBook都引用相同的Book对象

![](http://upload-images.jianshu.io/upload_images/2552605-f8eb8f1f1095d5ad?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

如下代码则创建了两个不同的Book对象

```java
	Book myBook=new Book();
	Book yourBook=new Book();
```

![](http://upload-images.jianshu.io/upload_images/2552605-c781103459e93ef5?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

如果Book对象中又包含了另一个对象呢？假设有一个Time类，且包含在Book类中

```java
public class Book(){
	Time time=new Time();
}
```
当创建了一个Book对象时

```java
	Book book=new Book();
```
内存分配图如下

![](http://upload-images.jianshu.io/upload_images/2552605-7055177b57b5f220?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

即Book对象中的time字段，拥有一个Time对象的引用，由此也允许Book对象操作该Time对象

## **六、访问控制**

Java有四种访问控制级别：public、protected、private和default（即不加修饰符，默认访问级别）

### **6.1、类访问控制修饰符**

类访问控制修饰符包括：public或默认访问级别。使用public访问控制修饰符使得类变为公有的，没有使用访问控制修饰符的类则具有默认的访问级别。公有类在任何地方都是可见的。默认访问级别的类只能由属于同一个包中的类使用

### **6.2、类成员访问控制修饰符**

类成员（方法、字段、构造方法等）可以具备四种访问控制级别之一

- public使得类成员成为公有的
- protected使得类成员成为受保护的
- private使得类成员成为私有的
- 没有使用访问控制修饰符的话，类成员将会拥有默认的访问级别

|访问级别|从其他包中的类来访问|从同一包中的其他类来访问|从同一个类
| :------:|:-----------:|:--:|:--:
|public|可以|可以|可以
|private|不可以|不可以|可以
|protected|若是子类，直接调用则可以，声明对象后再调用则不可以。若非子类，也不可以|可以|可以
|default|不可以|可以|可以


## **七、核心类**

### **7.1、java.lang.String**

String对象表示一个字符串，一旦创建了一个String对象，不能改变其值，即String对象是常量。由于String的不可变性，共享它们也是很安全的

创建String的方式有：

将一个字符串字面值赋值给一个String引用变量

```java
	String s="Java";
```
或者用new关键字来构建一个String对象

```java
	String s=new String("Java");
```
这两种创建方式的含义并不相同

当直接使用字符串字面值来赋值时，得到的String对象并不总是新的。如果该字符串“Java”之前已经创建过了话，该对象可能来自于一个缓冲池。如果使用new关键字，JVM每次都会创建String的一个新的实例

因此，使用字符串字面值会更好，因为JVM节省了一些本来需要用来构造新的实例的CPU周期

字符串比较包含有两个含义，即引用地址的比较或者字面值的比较

比如如下代码，“==”比较的是两个变量引用的地址是否相同

而因为s1和s2引用相同的字符串，所以输出为true

```java
		String s1="Java";
		String s2="Java";
		//输出true
		System.out.println(s1==s2);
```
在如下代码中，因为每次new关键字都会创建一个新的String对象，所以输出为false

```java
		String s1=new String("Java");
		String s2=new String("Java");
		//输出false
		System.out.println(s1==s2);
```

大多数时候我们只是想进行字面值的比较，这种情况下用的是**equals**方法

```java
		String s1="Java";
		String s2=new String("Java");
		String s3="C";
		//输出true
		System.out.println(s1.equals(s2));
		//输出false
		System.out.println(s1.equals(s3));
```

### **7.2、StringBuffer和StringBuilder**

String对象是不可变的，如果需要多次向其添加或者插入字符时，使用它并不合适，因为每次都会创建一个新的String对象

此时最好使用StringBuffer或者StringBuilder，在完成操作后再将其转为String对象

StringBuffer的方法是同步的，这使得StringBuffer适合在多线程环境下使用，但同步的代价是性能的下降。而StringBuilder是StringBuffer的异步版本，如果不需要同步的话，应优先选择StringBuilder

StringBuilde和StringBuffer拥有类似的构造函数和方法，这里只介绍StringBuilder用法

StringBuilder拥有四个构造方法

```java
	public StringBuilder()
	public StringBuilder(CharSequence seq)
	public StringBuilder(int capacity)
	public StringBuilder(String string)
```

如果在创建StringBuilder对象时没有指定大小，这个对象将拥有十六个字符的初始大小。如果内容超过了其容量，对象将自动增加大小。如果在创建前知道内容将会超过十六个字符，最好分配足够的空间，因为增加容量也会花费一定时间

StringBuilder有几个方法，主要有以下几个：

获取StringBuilder对象的容量
```java
	public int capacity();
```
获取StringBuilder对象所存储的字符串的长度
```java
	public int length();
```
将指定的String对象添加到StringBuilder对象末尾，包括多种重载形式
```java
	public StringBuilder append(String string);
```
在offset指定的位置插入指定字符串，包括多种重载形式
```java
	public StringBuilder insert(int offset,String string);
```
返回一个String对象
```java
	public String toString();
```

### **7.3、基本类型包装器**

为了保证性能，Java中并非所有内容都是对象，还包括一些基本类型，比如int、long、float等

若想要将基本类型转换为对象，可以使用包装器，即**Boolean、Character、Byte、Short、Integer、Long、Float、Double**

**7.3.1、Boolean**

java.lang.Boolean类包装了一个boolean类型，它的静态final字段是TRUE和FALSE，分别封装了基本类型true和false所对应的Boolean对象

构造方法有：

```java
	public Boolean(boolean value);
	public Boolean(String value);
```
将Boolean转换为boolean值

```java
	public boolean booleanValue();
```
将一个String解析为Boolean对象

```java
	public static Boolean valueOf(String string);
```

**7.3.2、Character**

java.lang.Character包装了一个char类型

构造方法

```java
public Character(char value);
```

将对象转为char类型

```java
	public char charValue();
```
判断指定参数是否是数字

```java
	public static boolean isDigit(char value);
```
将指定字符转为大写或小写形式

```java
	public static char toUpperCase(char ch);
	public static char toLowerCase(char ch); 
```

**7.3.3、Integer**

数字类型包装器具有类似的方法，这里只介绍Integer

java.lang.Integer类包装了一个int类型。Integer类有两个int类型的静态的final字段：MIN_VALUE和MAX_VALUE。MIN_VALUE即int类型的最小值，而MAX_VALUE即int类型的最大值

构造方法如下，可以用一个int值或String对象来构造

```java
	public Integer(int value);
	public Integer(String value);
```

Integer拥有类似如下的方法，可以将包装的值转为byte、short、int、long、double类型

```java
	public byte byteValue();
```

## **八、方法覆盖**

从一个子类中，可以访问其父类的共有的和受保护的方法和字段，但是，不能访问父类的私有方法。如果子类和父类位于同一个包中，还可以访问父类的默认字段和方法

当扩展一个类的时候，可以修改父类的方法的行为，这叫做方法覆盖。如果只有方法名称是相同的，但参数的列表不同，那么就叫做方法重载

覆盖一个方法就会修改其行为。要覆盖一个方法，直接在子类中编写新的方法，而不必修改其父类的任何内容

可以覆盖父类的公开的和受保护的方法。如果子类和父类位于相同的包中，还可以覆盖带有默认访问级别的方法，因为此时父类的默认访问级别的方法对子类来说才是可见的。而私有方法对其他类来说都是不可见的，所以无法覆盖

## **九、构造方法**

在用new关键字创建一个类的实例时，如果该类没有显式地编写一个构造函数，编译器会隐式地添加一个无参数的构造方法

当通过调用子类的构造方法来实例化一个子类时，该构造方法会首先调用直接父类的无参数的构造方法。而父类的构造方法也会调用其直接父类的构造方法，这个过程不断重复，直到到达了java.lang.Object类的构造方法，即子类的所有父类也会实例化

例如

```java
public class Base {
	
	public Base(){
		System.out.println("Base 的无参数的构造函数被调用");
	}
	
	public Base(String str){
		System.out.println("Base 的带一个参数的构造函数被调用"+"--"+str);
	}

}
```

```java
public class Sub extends Base{
	
	public Sub(){
		System.out.println("Sub 的无参数的构造函数被调用");
	}
	
	public Sub(String str){
		System.out.println("Sub 的带一个参数的构造函数被调用");
	}
	
	public static void main(String[] args) {
		Sub sub=new Sub();
	}
	
}

```
运行结果为

```java
	Base 的无参数的构造函数被调用
	Sub 的无参数的构造函数被调用

```

可以用super关键字从子类的构造方法中显式地从父类的构造方法，即指定要执行父类的哪一个构造方法
此时super必须是子类构造方法中的第一条语句

将Sub类的无参构造方法修改为如下所示

```java
	public Sub(){
		super("Java");
		System.out.println("Sub 的无参数的构造函数被调用");
	}
```

运行结果如下所示：

```java
	Base 的带一个参数的构造函数被调用--Java
	Sub 的无参数的构造函数被调用
```

现在考虑一种情况。如果父类没有无参数的构造方法，但有一个含参数的构造方法

```java
public class Base {
		
	public Base(String str){
		System.out.println("Base 的带一个参数的构造函数被调用"+"--"+str);
	}

}
```

此时子类没有显式调用父类含参数的构造方法，它的两个构造方法都会报错，因为编译器添加了对父类的无参数构造方法的一个隐式调用

但此时父类拥有一个含参数的构造函数，编译器也就不会隐式地为其添加一个无参数的构造方法，这就使得IDE报错

```java
public class Sub extends Base{
	
	//会报错
	public Sub(){
		System.out.println("Sub 的无参数的构造函数被调用");
	}
	
	//会报错
	public Sub(String str){
		System.out.println("Sub 的带一个参数的构造函数被调用");
	}
	
	public static void main(String[] args) {
		Sub sub=new Sub();
	}
	
}
```

解决问题的方法有两个，可以为父类添加一个无参构造函数，或者使子类的构造函数显示调用父类的有参构造函数

## **十、接口和抽象类**

### **10.1、接口**

接口（Interface）在Java语言中是一个抽象类型，是服务提供者和服务使用者之间的一个协议，在JDK 1.8之前一直是抽象方法的集合，一个类通过实现接口从而来实现两者间的协议

接口可以定义字段和方法。在JDK 1.8之前，接口中所有的方法都是抽象的，从JDK 1.8开始，也可以在接口中编写默认的和静态的方法。除非显式指定，否则接口方法都是抽象的

接口的特点：

 - 接口没有构造方法
 - 接口不能用于实例化对象
 - 接口中的字段必须初始化，并且隐式地设置为公有的、静态的和final的。因此，为了符合规范，接口中的字段名要全部大写
 - 接口不是被类继承，而是要被类实现
 - 接口中每一个方法默认是公有和抽象的，即接口中的方法会被隐式的指定为 **public abstract**。从JDK 1.8开始，可以在接口中编写默认的和静态的方法。声明默认方法需要使用关键字**default**
 - 当类实现接口时，类要实现接口中所有的方法。否则，类必须声明为抽象的
 - 接口支持多重继承，即可以继承多个接口

```java
public interface Name {

	String getName();
	// 等价于以下三种形式
	// public String getName();
	// public abstract String getName();
	// abstract String getName();

	// 静态方法，可以省略public声明，因为在接口中的静态方法默认就是公有的
	public static void setName(String name) {
		// 实现具体业务
	}
	
	// 默认方法
    default void defaultMethod(){
		// 实现具体业务
	}

}
```

### **10.2、抽象**

在面向对象的概念中，所有的对象都是通过类来描绘的。但并不是所有的类都是用来描绘对象的，如果一个类中没有包含足够的信息来描绘一个具体的对象，这样的类就是抽象类（Abstract）

抽象类除了不能实例化对象之外，类的其它功能依然存在，成员变量、成员方法和构造方法的访问方式和普通类一样。由于抽象类不能实例化对象，所以抽象类必须被继承才能被使用

如果想要设计这样一个类，该类包含一个特别的成员方法，方法的具体实现由它的子类确定，那么可以在父类中声明该方法为抽象方法

Abstract关键字同样可以用来声明抽象方法，抽象方法只包含一个方法名，而没有方法体

声明抽象方法会造成以下两个结果：

 - 如果一个类包含抽象方法，则该类必须声明为抽象类
 - 子类必须重写父类的抽象方法，否则自身也必须声明为抽象类

接口和抽象类的区别有：

 - 接口中的方法没有方法体，但抽象类可以实现方法的具体功能
 - 抽象类中的成员可以是各种类型的，接口中的成员变量只能是public static final类型的
 - 一个类只能继承一个抽象类，但能实现多个接口


## **十一、枚举**

枚举（Enum）是在Java 5中添加的新的类型

从技术上来讲，由于enum是一个类，一个enum可以有构造方法和方法。如果有构造方法，那必须是私有的。如果一个enum定义了枚举值之外的其他内容，枚举值必须在其他内容之前定义，并且最后的枚举值用一个分号结束

```java
	public enum Color {
		RED, GREEN, BLANK, YELLOW
	}
    // 遍历输出
	for (Color color : Color.values()) {
		System.out.println(color);
	}
```

输出结果为

```java
	RED
	GREEN
	BLANK
	YELLOW
```
可以声明构造函数以及其他方法，例如以下代码

```java
public enum Color {

	RED("红色", 1), GREEN("绿色", 2), BLUE("蓝色", 3);

	private String name;

	private int index;

	 private Color(String name, int index) {
		this.name = name;
		this.index = index;
	}

	public static String getName(int index) {
		for (Color c : Color.values()) {
			if (c.getIndex() == index) {
				return c.name;
			}
		}
		return null;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

}

public static void main(String[] args) {
		// 遍历输出
		for (Color color : Color.values()) {
			System.out.println(color.getName() + "idnex：" + color.getIndex());
		}
	}
```
输出结果为：

```java
	红色idnex：1
	绿色idnex：2
	蓝色idnex：3
```
此外还有其他的使用方式

```java
public interface Command {
	void execute();
}


public enum Color implements Command {

	RED, GREEN, BLUE;

	@Override
	public void execute() {
		switch (this) {
		case RED:
			System.out.println("选中的是红色");
			break;
		case GREEN:
			System.out.println("选中的是绿色");
			break;
		case BLUE:
			System.out.println("选中的是蓝色");
			break;
		}
	}
}
```
枚举实现了Command接口，在不同枚举值下执行不同的行为

```java
	public static void main(String[] args) {
		select(Color.BLUE);
		select(Color.RED);
	}
	
	public static void select(Color color){
		color.execute();
	}
```

运行结果如下

```java
	选中的是蓝色
	选中的是红色
```
而枚举Color也可以以另一种形式来实现Command接口，使用效果也完全相同

```java
public enum Color implements Command {
	
	RED {
		public void execute() {
			System.out.println("选中的是红色");
		}
	},
	GREEN {
		public void execute() {
			System.out.println("选中的是绿色");
		}
	},
	BLUE {
		public void execute() {
			System.out.println("选中的是蓝色");
		}
	};
}
```

## **十二、集合框架**

Java带有一组接口和类，使得操作成组的对象更为容易，这就是集合框架

集合框架主要用到的是Collection接口，Collection是将其他对象组织到一起的一个对象，提供了一种方法来存储、访问和操作其元素

List、Set和Queue是Collection的三个主要的子接口。此外，还有一个Map接口用于存储键值对

|接口|描述|
|:--:|:--:|
|Collection|Collection是最基本的集合接口，一个 Collection 代表一组Object，Java不提供直接继承自Collection的类，只提供继承于它的子接口|
|List|List接口是一个有序的Collection，使用此接口能够精确的控制每个元素插入的位置，能够通过索引来访问List中的元素，而且允许有相同的元素|
|Set|Set具有与Collection完全一样的接口，只是行为上不同，Set不保存重复的元素|
|Queue|Queue通过先进先出的方式来存储元素，即当获取元素时，最先获得的元素是最先添加的元素，依次递推|
|SortedSet|继承于Set保存有序的集合|
|Map|将唯一的键映射到值|
|Map.Entry|描述在一个Map中的一个元素（键/值对），是一个Map的内部类|
|SortedMap|继承于Map，使Key保持在升序排列|

![](http://upload-images.jianshu.io/upload_images/2552605-e959f56924e53c4a?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

存取对象的共同行为定义在Collection中，然而存取对象会有不同的需求

- 如果希望存入数据时记录每个对象的索引顺序，并可依次索引取回对象，应该使用List
- 如果希望存入的对象不重复，具有集合的行为，应该使用Set
- 如果希望传入对象时以队列方式，加入的对象至尾端，取得对象时从前端，则可以使用Queue
- 如果希望对Queue的两端进行加入、移除等操作，则可以使用Deque

List和Set的区别

 - List可以动态增长，根据实际存储的数据的多少自动增长List的容量。查找元素效率高，插入删除效率低，因为会引起其他元素位置改变 <实现类有ArrayList，LinkedList，Vector> 
 - Set 接口实例存储的是无序的，不重复的数据。List 接口实例存储的是有序的，可以重复的元素。Set检索效率低下，删除和插入效率高，插入和删除不会引起元素位置改变 <实现类有HashSet,TreeSet>


## **十三、泛型**

### **13.1、通配符？**

如果声明一个List< Type > myList，那么myList可以存储如下类型之一的对象

- 如果Type是类，则可以是Type的一个实例或Type的子类的一个实例
- 如果Type是接口，则可以是实现了Type的一个类的实例

假设有一个printList方法，用来打印出一个List的成员，如果Type不确定，可能你就需要写出许多个重载方法了。如果想要使printList接受任意类型的List，就需要使用通配符？了

```java
   public static void printList(List<?> myList){
		for(Object obj:myList){
			System.out.println(obj);
		}
	}

	public static void main(String[] args) {
		List<String> strList=new ArrayList<>();
		strList.add("Hello");
		strList.add("Hi");
		strList.add("Good");
		
		List<Integer> intList=new ArrayList<>();
		intList.add(1);
		intList.add(2);
		intList.add(3);
		
		printList(strList);
		printList(intList);
	}
```
输出结果

```java
Hello
Hi
Good
1
2
3
```

### **13.2、界限通配符**

有时候我们会想限制那些被允许传递到一个类型参数的类型种类范围，即定义一个类型变量的上界

例如，一个求平均值的方法只希望接受Number或者Number子类的实例，这就是有界类型参数的用途

语法规则如下

```java
	GenericType< ? extends upperBoundType >
```

```java
   public static double getAverage(List<? extends Number> numberList) {
		double total = 0.0d;
		for (Number number : numberList) {
			total += number.doubleValue();
		}
		return total / numberList.size();
	}

	public static void main(String[] args) {
		List<String> strList = new ArrayList<>();
		strList.add("Hello");
		strList.add("Hi");
		strList.add("Good");

		List<Integer> intList = new ArrayList<>();
		intList.add(1);
		intList.add(2);
		intList.add(3);
		// 输出2.0
		System.out.println(getAverage(intList));
		// 会产生一个编译错误
		//System.out.println(getAverage(strList));
	}
```
extends关键字用于定义一个类型变量的上界，也可以通过super关键字来定义一个类型变量的下界

例如，使用List< ? super Integer >作为一个方法参数的类型，表示可以传递一个List< Integer >或者Integer的一个超类的对象的List

### **13.3、泛型类**

泛型类的声明和非泛型类的声明类似，除了需要在类名后面添加类型参数声明

和泛型方法一样，泛型类的类型参数声明部分也包含一个或多个类型参数，参数间用逗号隔开

一个泛型参数，也被称为一个类型变量，是用于指定一个泛型类型名称的标识符。因为他们接受一个或多个参数，这些类被称为参数化的类或参数化的类型

```java
public class Point<T, V> {

	T x;
	
	V y;

	public Point(T x, V y) {
		this.x = x;
		this.y = y;
	}

	public T getX() {
		return x;
	}

	public V getY() {
		return y;
	}

}
```

```java
  Point<Integer,String> point1=new Point<>(4,"Hi");
			
  Point<String,Double> point2=new Point<>("4",0D);
```


### **13.4、泛型方法**

泛型方法是声明了自己的类型参数的方法。泛型方法的参数类型在一对尖括号中声明，并且放在方法的返回值之前
泛型方法可以在一个泛型类或非泛型类中使用

例如，java.util.Collections类的emptyList方法是一个泛型方法，该方法的签名如下：

```java
	public static final <T> List<T> emptyList()
```

```java
   public static <T> void printArray(T[] input) {
		for (T element : input) {
			System.out.printf("%s ", element);
		}
		System.out.println("");
	}
	
	public static void main(String args[]) {
		Integer[] intArray = { 1, 2, 3, 4, 5 };
		Double[] doubleArray = { 1.1, 2.2, 3.3, 4.4 };
		Character[] charArray = { 'H', 'E', 'L', 'L', 'O' };
		printArray(intArray); 
		printArray(doubleArray); 
		printArray(charArray); 
	}
```
输出结果为：

```java
1 2 3 4 5 
1.1 2.2 3.3 4.4 
H E L L O 
```
## **十四、嵌套类**

嵌套类是在一个类或接口中的主体中声明的一个类，嵌套类分为两种：静态的和非静态的。非静态的嵌套类叫做内部类
术语“顶级类”用来表示没有在其他类或接口之中定义的一个类，即没有类能够包含一个顶级类

对于顶级类来说，嵌套类就像其他的类成员一样。嵌套类可以有如下的四种访问修饰符之一：public、protected、default、private。这和顶级类不同，顶级类只能是public和default的

### **14.1、静态嵌套类**

静态嵌套类和内部类的行为并不完全相同

- 静态嵌套类可以有静态成员，内部类不能
- 就像是实例方法一样，内部类可以访问外部类的静态和非静态成员，包括私有成员。静态嵌套类只能访问外部类的静态成员

```java
public class Outer {

	private static int value = 10;

	protected static class Nested {
		public int getValue() {
			return value;
		}
	}

}

public static void main(String[] args) {
		Outer.Nested nested=new Outer.Nested();
		System.out.println(nested.getValue());
}
```

不需要创建外围类的实例即可实例化一个静态嵌套类，也可以在静态嵌套类中访问外围类的静态成员，但不能访问非静态成员，因为如以上代码所示，当静态嵌套类实例化时，外围类并没有实例化，也就不能访问其非静态成员了

### **14.2、内部类**

内部类有几种类型：

- 成员内部类
- 局部内部类
- 匿名内部类

内部类有以下一些优点：

- 能够访问外部类的所有成员，包括私有的
- 帮助隐藏一个类的实现
- 提供了一种简洁的方式，在Swing和其他基于事件的应用程序中编写监听器

#### **14.2.1、成员内部类** 

要创建一个成员内部类的实例，需要拥有其外部类的一个实例的引用

假设外部类为A，成员内部类为B，则创建一个B类的实例的语法规则如下：

```java
	A a = new A();
	A.B b = a.new B();
```

```java
 public class Outer {

	private int value = 10;
	
	private static int staticValue=11;

	protected  class Nested {
		public int getValue() {
			// 可以访问外部类的非静态、静态、私有成员
			return value+staticValue;
		}
	}

}


	public static void main(String[] args) {
		Outer outer=new Outer();
		Outer.Nested nested=outer.new Nested();
		System.out.println(nested.getValue());
	}
```

#### **14.2.2、局部内部类** 

 局部内部类可以简称为局部类，局部类可以在任何代码块中声明，并且其作用域位于代码块之中。例如，可以在一个方法快、一个if语句块、一个while语句块中声明一个局部类

 如果类的实例只在作用域内使用的话，使用局部类可以说是一个好办法

```java
public interface Logger {
	void log(String message);
}


public class Outer {

	String time = LocalDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));

	public Logger getLogger() {
		class LoggerImpl implements Logger {
			@Override
			public void log(String message) {
				System.out.println(time + " : " + message);
			}
		}
		return new LoggerImpl();
	}

}


public static void main(String[] args) {
		Outer outer=new Outer();
		Logger logger=outer.getLogger();
		logger.log("Hi");
	}
```

#### **14.2.3、匿名内部类** 

匿名内部类没有名称，这种类型的嵌套类用于编写一个接口实现

```java
    public interface Logger {
	   void log(String message);
    }

    public static void main(String[] args) {
		Logger logger=new Logger() {
			
			@Override
			public void log(String message) {
				System.out.println("Hi : "+message);
			}
		};
		logger.log("message");
	}
```