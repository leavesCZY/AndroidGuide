Java 在需要使用到某个类时会载入 .class 文档，在 JVM 产生 Java.lang.Class 实例代表该文档，从 Class 实例开始，就可以获得类的许多类型。 .class 文档反映了类基本信息，因而从 Class 等API取得类信息的方法就称为反射

## **一、Class与.class文档**

Java 在真正需要某个类时才会加载对应的 .class 文档，而非在程序启动时就加载所有类，因为大部分时候我们只需要用到应用程序部分资源，有选择地加载可以节省系统资源

java.lang.Class 的实例代表 Java 应用程序运行时加载的 .class 文档，类、接口、Enum等编译过后，都会生成 .class 文档，所以 Class可以用来包含类、接口、Enum等信息

Class 类没有公开的构造函数，实例是由 JVM 自动产生，每个 .class 文档加载时， JVM 会自动生成对应的 Class 对象

可以通过 Object 的 getClass() 方法或者通过 .class 常量取得每个对象对应的 Class 对象。如果是基本类型，可以使用对象的包装类加载 .TYPE 取得 Class 对象

例如，使用 Integer.TYPE 可取得代表 int 基本类型的 Class，通过 Integer.class 取得代表 Integer.class 文档的 Class

在取得 Class 对象后，就可以操作 Class 对象的公开方法取得类基本信息
例如

```java
package com.czy.demo;

public class Student {
	
	public static void main(String[] args) {
		Class cl=Student.class;
		System.out.println("类名称:"+cl.getName());
		System.out.println("简单类名称:"+cl.getSimpleName());
		System.out.println("包名:"+cl.getPackage());
		System.out.println("是否为接口:"+cl.isInterface());
		System.out.println("是否为基本类型:"+cl.isPrimitive());
		System.out.println("是否为数组对象:"+cl.isArray());
		System.out.println("父类名称:"+cl.getSuperclass().getName());
	}

}
```
输出结果为

```
类名称:com.czy.demo.Student
简单类名称:Student
包名:package com.czy.demo
是否为接口:false
是否为基本类型:false
是否为数组对象:false
父类名称:java.lang.Object
```

Java 在真正需要类时才会加载 .class 文档，即在生成对象时才会加载 .class 文档。如果只是使用类声明了一个变量，此时并不会加载.class文档，而只是让编译程序检查对应的 .class 文档是否存在

例如，在 Stduent 类中定义了 static 静态区域块，在首次加载 .class 文档时会被执行（这是默认情况下，也可以指定不执行）

```java
public class Student {
	
	static {
		System.out.println("载入了 Student.class 文档");
	}
	
}
```

再来测试加载顺序

```java
package com.czy.demo;

public class Main {
	
	public static void main(String[] args) {
		Student student;
		System.out.println("声明了 Student 变量");
		student=new Student();
		System.out.println("生成了 Student 实例");
	}
	
}
```

输出结果为

```java
声明了 Student 变量
载入了 Student.class 文档
生成了 Student 实例
```


## **二、使用Class.forName()**

在某些情况下，会存在事先不知道类名称，需要事后指定类名称来动态加载类的情况

可以使用 Class.forName() 方法实现动态加载类，用字符串指定类名称来获得类相关信息

```java
package com.czy.demo;

public class Main {
	
	public static void main(String[] args) {
		try{
			Class cl=Class.forName("java.lang.String");
			System.out.println("类名称:"+cl.getName());
			System.out.println("简单类名称:"+cl.getSimpleName());
			System.out.println("包名:"+cl.getPackage());
			System.out.println("是否为接口:"+cl.isInterface());
			System.out.println("是否为基本类型:"+cl.isPrimitive());
			System.out.println("是否为数组对象:"+cl.isArray());
			System.out.println("父类名称:"+cl.getSuperclass().getName());
		}catch(ClassNotFoundException e){
			e.printStackTrace();
		}
	}
	
}
```
输出结果

```java
类名称:java.lang.String
简单类名称:String
包名:package java.lang, Java Platform API Specification, version 1.8
是否为接口:false
是否为基本类型:false
是否为数组对象:false
父类名称:java.lang.Object
```

Class.forName() 另一重载版本可以指定类名称、加载类时是否执行静态区域块、类加载器

```java
	public static Class<?> forName(String name, boolean initialize, ClassLoader loader)
```

例如

还是使用 Student 类

```java
package com.czy.demo;

public class Student {
	
	static {
		System.out.println("执行静态区域块");
	}
	
}
```
测试调用顺序

```java
public class Main {
	
	public static void main(String[] args) {
		try{
			Class cl=Class.forName("com.czy.demo.Student",false,Main.class.getClassLoader());
			System.out.println("已载入class文档");
			Student student;
			System.out.println("声明了Student变量");
			student=new Student();
			System.out.println("生成Student实例");
		}catch(ClassNotFoundException e){
			e.printStackTrace();
		}
	}
	
}
```

输出结果

```java
已载入class文档
声明了Student变量
执行静态区域块
生成Student实例
```
由于指定了加载类时不执行静态区域块，所以在建立了 Stduent 实例后，静态区域块才被执行。类加载器则是使用 Main.class 文档的类加载器

因此，只有一个参数的 Class.forName(String name) 方法，等同于

```java
   Class.forName(className,  true,  currentLoader)
```
即默认加载静态区域块，使用当前类的类加载器来载入类


## **三、从Class获得信息**

Class对象代表加载的.class文档，取得Class对象后，就可以取得.class文档中记载的信息，如包、构造函数、数据成员、方法成员等

每一种信息都对有对应的类型，如包对应的类型是 java.lang.Package，构造函数对应的类型是 java.lang.reflect.Constructor

例如，先来为Student类增添多种类型的不同信息

```java
package com.czy.demo;

public final class Student {
	
	enum Gender{
		male,female
	}
	
	private String name;
	
	public int age;
	
	protected Gender gender;
	
	public Student(String name,int age){
		
	}
	
	public Student(String name,int age,Gender gender){
		
	}
	
	private Student(){
		
	}
	
	public String getName() {
		return name;
	}
	
	private int getAge(){
		return age;
	}
	
	protected Gender getGender(){
		return gender;
	}
	
}

```

再来获取各种信息

```java
public class Main {

	public static void main(String[] args) {
		try {
			Class cl = Class.forName("com.czy.demo.Student");
			
			// 取得包对象
			Package p = cl.getPackage();
			System.out.println("包名:" + p.getName());
			// 访问修饰符
			int modifier = cl.getModifiers();
			System.out.println("类访问修饰符：" + Modifier.toString(modifier));

			System.out.println();
			
			//取得构造函数信息
			Constructor[] constructors=cl.getConstructors();
			for(Constructor constructor:constructors){
				System.out.print("访问修饰符：" + Modifier.toString(constructor.getModifiers()));
				System.out.print("   构造函数名："+constructor.getName());
				System.out.println();
			}
			
			System.out.println();
			
			//取得声明的数据成员
			Field[] fields = cl.getDeclaredFields();
			for (Field field : fields) {
				System.out.print("访问修饰符：" + Modifier.toString(field.getModifiers()));
				System.out.print("   类型："+field.getType().getName());
				System.out.print("   成员名："+field.getName());
				System.out.println();
			}
			
			System.out.println();
			
			//取得成员方法息
			Method[] methods=cl.getDeclaredMethods();
			for(Method method:methods){
				System.out.print("访问修饰符：" + Modifier.toString(method.getModifiers()));
				System.out.print("   返回值类型："+method.getReturnType().getName());
				System.out.print("   方法名："+method.getName());
				System.out.println();
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

}
```
运行结果

```java
包名:com.czy.demo
类访问修饰符：public final

访问修饰符：public   构造函数名：com.czy.demo.Student
访问修饰符：public   构造函数名：com.czy.demo.Student

访问修饰符：private   类型：java.lang.String   成员名：name
访问修饰符：public   类型：int   成员名：age
访问修饰符：protected   类型：com.czy.demo.Student$Gender   成员名：gender

访问修饰符：public   返回值类型：java.lang.String   方法名：getName
访问修饰符：private   返回值类型：int   方法名：getAge
访问修饰符：protected   返回值类型：com.czy.demo.Student$Gender   方法名：getGender

```

## **四、利用Class建立对象**

如果已有确切的类，那么就可以使用new关键字建立实例。如果不知道类名称，那么可以利用Class.forName() 动态加载.class文档，取得Class对象之后，利用其newInstance()方法建立实例

```java
		Class cl=Class.forName("ClassName");
		Object object=cl.newInstance();
```
这种事先不知道类名称，又需要建立类实例的需求，一般情况下都是由于开发者需要得到某个类对象并对其行为进行操纵，可是该类又是由他人开发且还未完工，因此就需要来动态加载.class文档

例如，你需要来控制学生、老师或者家长的唱歌行为，可是学生、老师和家长这些类又是由其他人来设计的，你只是对开始与暂停操作进行控制

那么，你可以规定学生类必须实现Sing接口

```java
public interface Sing {
	
	void start();
	
}
```
那么，就可以来进行自己的开发了，将动态加载的对象强转为Sing

```java
public class Main {

	public static void main(String[] args) {
		try {
			Sing palyer = (Sing) Class.forName("className").newInstance();
			palyer.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
```
然后规定他人设计的学生类必须实现Sing接口

```java
package com.czy.demo;

public class Student implements Sing {

	@Override
	public void start() {
		System.out.println("学生唱歌");
	}

}
```

这样，等到得到确切的类名称后，修改main方法的className即可

```java
public static void main(String[] args) {
		try {
			Sing palyer = (Sing) Class.forName("com.czy.demo.Student").newInstance();
			palyer.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
```

## **五、操作成员方法**

java.lang.reflect.Method 实例是方法的代表对象，可以使用 invoke() 方法来动态调用指定的方法

例如，修改Student类，将get方法都指定为公有的，将set方法指定为私有的

```java
package com.czy.demo;

public class Student {

	private String name;

	private int age;

	public Student() {

	}

	public Student(String name, int age) {
		this.name = name;
		this.age = age;
	}

	public String getName() {
		System.out.println("调用了getName方法，Name：" + name);
		return name;
	}

	public int getAge() {
		System.out.println("调用了getAge方法，Age：" + age);
		return age;
	}

	private void setName(String name) {
		this.name = name;
		System.out.println("调用了setName方法,name:" + name);
	}

	private void setAge(int age) {
		this.age = age;
		System.out.println("调用了setAge方法，age:" + age);
	}

}

```
一般情况下，类的私有方法只有在其内部才可以被调用，通过反射我们可以来突破这一限制

首先来调用公有方法

```java
public class Main {

	public static void main(String[] args) throws Exception {
		Class cl = Class.forName("com.czy.demo.Student");
		// 指定构造函数
		Constructor constructor = cl.getConstructor(String.class, Integer.TYPE);
		// 根据指定的构造函数来获取对象
		Object object = constructor.newInstance("叶", 22);

		// 指定方法名称来获取对应的公开的Method实例
		Method getName = cl.getMethod("getName");
		// 调用对象object的方法
		getName.invoke(object);

		// 指定方法名称来获取对应的公开的Method实例
		Method getAge = cl.getMethod("getAge");
		// 调用对象object的方法
		getAge.invoke(object);

	}

}
```
输出结果如下所示，可以知道Student对象的两个get方法成功被调用了

```java
调用了getName方法，Name：叶
调用了getAge方法，Age：22

```
受保护或私有方法的调用步骤略有不同

```java
public class Main {

	public static void main(String[] args) throws Exception {
		Class cl = Class.forName("com.czy.demo.Student");
		// 指定构造函数
		Constructor constructor = cl.getConstructor(String.class, Integer.TYPE);
		// 根据指定的构造函数来获取对象
		Object object = constructor.newInstance("叶", 22);

		// 指定方法名称来获取对应的私有的Method实例
		Method setName = cl.getDeclaredMethod("setName", String.class);
		setName.setAccessible(true);
		setName.invoke(object, "新的名字");
		
		// 指定方法名称来获取对应的私有的Method实例
		Method setAge = cl.getDeclaredMethod("setAge", Integer.TYPE);
		setAge.setAccessible(true);
		setAge.invoke(object, 23);
	}

}

```
输出结果如下所示，可以看到私有方法一样在外部被调用了

```java
调用了setName方法,name:新的名字
调用了setAge方法，age:23
```
