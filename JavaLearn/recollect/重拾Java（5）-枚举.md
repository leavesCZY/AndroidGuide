从JDK 5开始，枚举被添加到了Java语言中，形式最简单的枚举看起来和其他语言中的枚举类似。但是，在Java中，通过将枚举定义为一种类类型，极大地扩展了枚举的功能，使得枚举可以具有构造函数、方法以及实例变量

## **一、基础知识**
创建枚举需要使用到关键字 enum ，如下所示：

```
	enum Letter{
		A,B,C,D
	}
```
标识符 A、B、C、D 等被称为枚举常量，每个枚举常量被隐式声明为 Letter 的公有静态final成员。每个枚举常量的类型是声明它们的枚举的类型
定义了枚举后，可以创建枚举类型的变量，但是不能使用new关键字来实例化枚举，而是通过与基本类型类似的方式声明，例如：

```
		Letter letter1=Letter.A;
		
		Letter letter2=Letter.B;
```
可以使用关系运算符“==”来比较两个枚举变量的相等性，例如：

```
		if (letter1 == letter2) {
			System.out.println("相等");
		} else {
			System.out.println("不相等");
		}
```
枚举值也可以用于控制switch语句，例如：

```
		switch (letter1) {
		case A:
			System.out.println("A");
			break;
		case B:
			System.out.println("B");
			break;
		}
```
在case语句中，枚举常量的名称没有使用枚举类型的名称进行限定，即使用的是 A 而不是 Letter.A ，这是因为 switch 表达式中的枚举类型已经隐式指定了case常量的枚举类型，所以在case语句中不需要使用枚举类型的名称对常量进行限定，如果试图这么做，会造成编译时错误

当使用println() 语句输出枚举变量时，会输出变量指向的枚举常量的名称，例如：

```
		//输出值为 A
		System.out.println(letter1);
```

## **二、枚举是类类型**
Java中枚举是类类型，虽然不能使用new关键字来实例化枚举，但是枚举却有和其他类相同的功能，可以为枚举提供构造函数、实例变量和方法，甚至可以实现接口
需要理解的是：每个枚举变量都是所属枚举类型的对象。因此，如果为枚举定义了构造函数，那么当创建每个枚举常量时都会调用该构造函数

为枚举 Letter 提供一个实例变量 index、一个返回index值的方法、两个不同的构造函数
对于每一个枚举常量，指定要调用的构造函数的方法是通过每个常量后面的圆括号中来加以指定
```
public enum Letter {

	KK(), A(1), B(2), C(3), D(5);

	private int index;

	Letter(int index) {
		this.index = index;
	}

	Letter() {
		this.index = 0;
	}
	
	public int getIndex() {
		return index;
	}

}
```

```
	public static void main(String[] args) {
		Letter letter = Letter.KK;
		//输出值为：0
		System.out.println("索引：" + letter.getIndex());
		 letter = Letter.B;
		 //输出值为：2
		System.out.println("索引：" + letter.getIndex());
	}
```
此外，枚举有两条限制：

 1. 枚举不能继承其他类。但是所有枚举都隐式自动继承超类 java.lang.Enum
 2. 枚举不能是超类。即枚举不能进行扩展

## **三、枚举的一些方法**
### 3.1、values() 
values() 方法返回一个包含枚举常量列表的数组，例如：

```
	enum Letter {
		A, B, C, D
	}

	public static void main(String[] args) {
		Letter[] values = Letter.values();
		for (Letter letter : values) {
			System.out.println(letter);
		}
	}
```
### 3.2、valueOf(String) 
valueOf(String) 方法返回与传递的字符串参数相对应的枚举常量，例如：

```
	enum Letter {
		A, B, C, D
	}

	public static void main(String[] args) {
		String str = "A";
		Letter le=Letter.valueOf(str);
		//输出值为：A
		System.out.println(le);
	}
```
### **3.3、ordinal()**
ordinal() 方法返回枚举常量在常量列表中位置的值，这称为枚举常量的序数值，例如：

```
	enum Letter {
		A, B, C, D
	}

	public static void main(String[] args) {
		Letter letter = Letter.A;
		//输出值为：0
		System.out.println("序数值：" + letter.ordinal());
		letter = Letter.B;
		//输出值为：1
		System.out.println("序数值：" + letter.ordinal());
	}
```
### **3.4、compareTo(Enum)**
compareTo(Enum) 方法用于比较相同类型的两个枚举常量的序数值，例如：

```
	enum Letter {
		A, B, C, D
	}

	public static void main(String[] args) {
		Letter letter1 = Letter.A;
		Letter letter2 = Letter.B;
		Letter letter3 = Letter.C;
		compare(letter1, letter2);
		compare(letter1, letter3);
		compare(letter2, letter3);
		// 输出值为：
		// A小于B
		// A小于C
		// B小于C
	}

	public static void compare(Letter letter1, Letter letter2) {
		if (letter1.compareTo(letter2) > 0) {
			System.out.println(letter1.name() + "大于" + letter2.name());
		} else if (letter1.compareTo(letter2) < 0) {
			System.out.println(letter1.name() + "小于" + letter2.name());
		} else {
			System.out.println(letter1.name() + "等于" + letter2.name());
		}
	}
```
## **3.5、equals(Enum)**
equals(Enum)用于比较两个对象的相等性，只有两个对象都引用同一个枚举中相同的常量时，返回值才为 true
```
	enum Letter {
		A, B, C, D
	}

	public static void main(String[] args) {
		Letter letter1 = Letter.A;
		Letter letter2 = Letter.B;
		Letter letter3 = Letter.A;
		//输出值：不相等
		if (letter1.equals(letter2)) {
			System.out.println("相等");
		} else {
			System.out.println("不相等");
		}
		//输出值：相等
		if (letter1.equals(letter3)) {
			System.out.println("相等");
		} else {
			System.out.println("不相等");
		}
	}
```
