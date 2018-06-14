异常是运行时在代码序列中引起的非正常状态。在不支持异常处理的计算机语言中，必须手动检查和处理错误，Java语言则采用面向对象的方式管理运行时错误。

##**一、基础知识**
Java异常是用来描述在一段代码中发生的异常情况的对象。当出现引起异常的情况时，就会创建用来表示异常的对象，并在引起异常的方法中抛出异常对象。方法可以选择自己处理异常或者传递异常交由其他方法来处理。

Java异常处理通过五个关键字进行管理：try、catch、throw、throws、finally。
在try代码快中封装可能发生异常的程序语句，对这些语句进行监视。如果在try代码块中发生异常，就会将异常抛出。代码使用catch来捕获异常，并可以定义一些方法来处理异常。系统生成的异常由Java运行时系统自动抛出。为了手动抛出异常，需要使用throw关键字。从方法中抛出的任何异常都必须通过一条throws子句进行指定。在try代码块结束之后必须执行的所有代码则需要放在finally代码块中。

##**二、异常类型**
所有异常类型都是内置类Throwable的子类，Throwable的两个子类将异常分为两个不同的分支。一个分支是Exception类，这个类既可以用于用户程序当前捕获的异常情况，也可以用于创建自定义异常类型的子类。另一个分支是Error类，该类定义了在常规环境下不希望由程序捕获的异常。Error类型的异常由Java运行时系统使用，以指示运行时系统本身发生了某些错误。

##**三、未捕获的异常**
没有被程序捕获的所有异常，最终都会交由Java运行时系统提供的默认处理程序捕获。默认处理程序会显示一个描述异常的字符串，输出异常发生的堆栈踪迹并终止程序。
例如，运行如下代码：

```
public class Demo {

	public static void main(String[] args) {
		int a=2/0;
	}
	
}
```
生成的异常信息：

```
Exception in thread "main" java.lang.ArithmeticException: / by zero
	at Demo.main(Demo.java:5)
```
抛出的异常的类型是Exception的子类ArithmeticException，即算术异常，更具体地描述了发生的错误类型。Java提供了一些能与可能产生的各种运行时错误相匹配的内置异常类型。

##**四、使用try和catch**
进行主动的异常处理有两个优点：第一，语序允许修复错误；第二，阻止程序自动终止。
为了主动处理运行时错误，可以在try代码块中封装希望监视的代码，之后通过catch子句指定希望捕获的异常类型。
例如，参照如下代码：

```
	public static void main(String[] args) {
		try{
			int a=10/0;
			System.out.println("输出语句1");
		}catch(ArithmeticException e){
			System.out.println("输出语句2");
		}
		System.out.println("输出语句3");
	}
```
输出结果是：

```
输出语句2
输出语句3
```
代码在进行除零操作时发生了异常，此时“输出语句1”将无法得到被调用的机会。一旦抛出异常，程序控制就会从try代码块中转移出来，进入catch代码块中。执行了catch语句后，就会继续运行整个try/catch代码块的下一行。


##**五、多条catch子句**
在有些情况下，一个代码块可能会引发多个异常。对于这种情况，需要指定两条或多条catch子句，用于捕获不同类型的异常。当抛出异常时，按顺序检查每条catch语句，执行类型和异常相匹配的第一条catch子句，忽略其他catch子句，并继续执行try/catch代码块后面的代码。
需要注意的是，异常子类必须位于异常超类之前，因为使用了某个超类的catch语句会捕获这个超类及其所有子类的异常。因此，如果子类位于超类之后的话，永远也不会到达子类。不可到达的代码会被编译器提示错误。

参照如下代码，通过Math的静态方法random来随机产生0和1两个随机数，生成的不同数值就会引发不同的异常，分别由不同的catch子句进行处理
```
	public static void main(String[] args) {	
		int random=(int) Math.round(Math.random());
		try{
			int a=10/random;
			int[] array={10};
			array[random]=1;
		}catch (ArithmeticException e) {
			System.out.println("ArithmeticException");
		}catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("ArrayIndexOutOfBoundsException");
		}
		System.out.println("代码块结束");
	}
```

此外，也可以通过多重捕获的方式来使用相同的catch子句捕获两个或更多个异常。在catch子句中使用或运算符（|）分隔每个异常，每个多重捕获参数都被隐式地声明为final类型。

```
    public static void main(String[] args) {    
        int random=(int) Math.round(Math.random());
        try{
            int a=10/random;
            int[] array={10};
            array[random]=1;
        }catch(ArithmeticException | ArrayIndexOutOfBoundsException e){
        	System.out.println("两个异常之一");
        }
    }
```

##**六、throw**
在之前的例子中，捕获的异常都是由Java运行时系统抛出的异常，可以通过throw语句显式地抛出异常。
throw的一般形式如下所示：

```
throw ThrowableInstance
```
ThrowableInstance必须是Throwable或其子类类型的对象。throw语句之后的执行流程会立即停止，所有的后续语句都不会执行，然后按顺序依次检查所有的catch语句，检查是否和异常类型相匹配。如果没有找到匹配的catch语句，那么默认的异常处理程序会终止程序并输出堆栈踪迹。

例如，运行以下代码，将得到输出结果：“NullPointerException”
```
public class Demo {
	
	public static void demo(){
		throw new NullPointerException("NullPointer");
	}

	public static void main(String[] args) {
		try{
			demo();
		}catch (NullPointerException e) {
			System.out.println("NullPointerException");
		}
		
	}
	
}
```
但如果catch子句的异常与throw抛出的异常类型不匹配时，异常将由Java默认的异常处理程序来处理

例如，运行以下代码：

```
public class Demo {
	
	public static void demo(){
		throw new NullPointerException("NullPointer");
	}

	public static void main(String[] args) {
		try{
			demo();
		}catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("ArrayIndexOutOfBoundsException");
		}
		
	}
	
}
```
运行结果是：

```
Exception in thread "main" java.lang.NullPointerException: NullPointer
	at Demo.demo(Demo.java:4)
	at Demo.main(Demo.java:9)
```

##**七、throws**
如果方法可能引发自身不进行处理的异常，就必须通过throws关键字来向方法的调用者指明要抛出的异常类型。
方法可能抛出的所有异常都必须在throws子句中进行声明，否则将产生编译时错误。

例如，参照以下代码，该程序试图抛出无法匹配的异常，因为程序没有指定throws子句来声明这一事实，所以程序无法编译

```
public class Demo {
	
	public static void demo(){
		throw new IllegalAccessException("IllegalAccess");
	}

	public static void main(String[] args) {
			demo();
	}
	
}
```
为了使代码能够运行，需要将之进行如下修改

```
public class Demo {
	
	public static void demo() throws IllegalAccessException{
		throw new IllegalAccessException("IllegalAccess");
	}

	public static void main(String[] args) {
			try {
				demo();
			} catch (IllegalAccessException e) {
				System.out.println(e.getMessage());
			}
	}
	
}
```

##**八、finally**
当抛出异常后，方法的执行流程将不会按照原先的顺序进行，这对于某些方法来说是个问题。例如，如果在方法中打开了一个文件，我们并不希望因为抛出了异常导致关闭文件的代码被跳过而不执行。finally关键字就是来解决这种情况的。
使用finally可以创建一个代码块，该代码块会在执行try/catch代码块之后执行，且在执行try/catch代码块后面的代码之前执行。不管是否有异常抛出，都将执行finally代码块。
只要方法从try/catch代码块内部返回到调用者，不管是通过未捕获的异常还是使用显式的返回语句，都会在方法返回之前执行finally子句。

例如，如下的输出结果将是：2

```
public class Demo {
	
	public static int demo(){
		try{
			int a=10/0;
			System.out.println(a);
		}catch(Exception e){
			return 1;
		}finally {
			return 2;
		}
	}

	public static void main(String[] args) {
		System.out.println(demo());
	}
	
}
```

##**九、链式异常**
链式异常用于为异常关联另一个异常，第二个异常用于描述当前异常产生的原因。例如，某个方法从文件读取数值来作为除法运算的除数，由于发生了I/O错误导致获取到的数值是0，从而导致了ArithmeticException异常。
如果想要让代码调用者知道背后的原因是I/O错误，使用链式异常就可以来处理这种情况以及其他存在多层异常的情况。

为了使用链式异常，Throwable有两个构造函数和两个方法用于处理这种情况。
两个构造函数：

```
Throwable(Throwable cause)

Throwable(String message, Throwable cause)
```
cause即是用于指定引发当前异常的背后原因，message则可以用于同时指定异常描述。

两个方法：

```
Throwable getCause()

Throwable initCause(Throwable cause)
```
getCause() 方法用来返回引发当前异常的异常，如果不存在背后异常则返回null。
initCause(Throwable cause) 方法将cause和明面上的异常关联到一起，并返回对异常的引用。因此可以在创建异常之后将异常和背后异常关联到一起。但是，背后异常只能设置一次，即initCause(Throwable cause) 方法只能调用一次。此外，如果通过构造函数设置了背后异常，也不能再使用该方法来设置背后异常了。

例如，参照如下代码：

```
public class Demo {
	
	public static void demo(){
		NullPointerException nullPointerException=new NullPointerException("nullPointer");
		nullPointerException.initCause(new ArithmeticException("Arithmetic"));
		throw nullPointerException;
	}

	public static void main(String[] args) {
		try {
			demo();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			System.out.println(e.getCause().getMessage());
		}
	}
	
}
```
运行结果是：

```
nullPointer
Arithmetic
```
链式异常可以包含所需要的任意深度，但是，过长的异常链可能是一种不良的设计。
