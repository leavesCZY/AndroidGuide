##**一、概述**
Java对多线程编程提供了内置支持，多线程是特殊形式的多任务处理，所有现代系统都支持多任务处理。多任务处理有两种不同的类型：基于进程的多任务处理和基于线程的多任务处理。基于进程的多任务处理就是允许计算机同时运行两个或更多个程序，程序是调度程序能够调度的最小代码单元。在基于线程的多任务处理意味着单个程序可以同时执行两个或更多个任务，最小的可调度代码单元是线程。

多任务线程需要的开销比多任务进程小。进程是重量级的任务，它们需要自己的地址空间，进程间通信开销很大而且有很多限制。从一个进程上下文切换到另一个进程上下文的开销也很大。线程是轻量级的任务，它们共享相同的地址空间，并且合作共享同一个重量级的进程。线程间通信的开销不大，并且从一个线程上下文切换到另一个线程上下文的开销更小。
使用多线程可以编写出更加高效的程序，以最大限度地利用系统提供的处理功能。

##**二、建立线程**
Java定义了创建线程的两种方法：

 1. 实现 Runnable 接口
 2. 继承 Thread 类

###**2.1、实现 Runnable 接口**
Runnable 接口抽象了一个可执行代码单元，可以依托任何实现了 Runnable 接口的对象来创建线程。
将实现了 Runnable 接口的对象作为参数来构造 Thread
```
public class MyThread implements Runnable {

	private Thread thread;

	public MyThread() {
		thread = new Thread(this);
		thread.start();
	}

	@Override
	public void run() {
		for (int i = 0; i < 5; i++) {
			System.out.println("Index: "+i);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
```
```
public class Test {
	
	public static void main(String[] args) {
		new MyThread();
	}
	
}
```
###**2.2、继承 Thread 类**
继承 Thread 类同样需要重写其 run() 方法，该方法是线程的入口点。此外，与上一种实现 Runnable 接口来创建线程的方式一样，均需要调用 Thread 类的 start() 方法以启动线程。

```
public class MyThread extends Thread {

	public MyThread() {
		start();
	}

	@Override
	public void run() {
		for (int i = 0; i < 5; i++) {
			System.out.println("Index: " + i);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
```
```
public class Test {
	
	public static void main(String[] args) {
		new MyThread();
	}
	
}
```

##**三、主线程**
当Java程序启动时，会立即开始运行一个线程，因为它是程序开始时执行的线程，所以这个线程通常称为程序的主线程。其他子线程都是从主线程产生的。

可以通过调用 currentThread() 方法获得对主线程的一个引用，该方法是Thread类的共有静态成员，返回对调用它的线程的引用，方法原型如下所示：

```
public static native Thread currentThread();
```

例如，运行以下Demo

```
public class Demo {

	public static void main(String[] args) {
		Thread t=Thread.currentThread();
		System.out.println("Thread:"+t);
		System.out.println("Thread Name:"+t.getName());
		
		t.setName("MyName");
		System.out.println("Thread:"+t);
		System.out.println("Thread Name:"+t.getName());
	}
	
}
```
输出值为：

```
Thread:Thread[main,5,main]
Thread Name:main
Thread:Thread[MyName,5,main]
Thread Name:MyName
```
当将线程 t 直接作为println() 方法的参数输出时，将依次显示线程的名称、优先级以及线程所属线程组的名称。默认情况下，主线程的名称是 main，优先级是5，主线程所属线程组的名称也是 main。线程组是将一类线程作为整体来控制状态的数据结构。可以通过 Thread.setName(String) 方法来设置线程名称。

##**四、线程的状态**
线程有多种状态。线程可以处于可运行（runnable）状态，只要获得CPU时间就运行。运行的线程可以被挂起（suspended），这会临时停止线程的活动。挂起的线程可以被恢复（resumed），从而允许线程从停止处恢复执行。当等待资源时，线程会被阻塞（blocked）。在任何时候，都可以终止线程，这会立即停止线程的执行。线程一旦终止，就不能再恢复。

对于给定的线程实例，可以使用 Thread.getState() 方法获取线程的状态，该方法返回 Thread.State类型的值（枚举类型），指示在调用该方法时线程所处的状态。
返回值的所有可能取值如下所示：

|值|状态|
|---|---|
|NEW|线程处于新建状态，还没有开始执行|
|RUNNABLE|线程要么当前正在执行，要么在获取CPU的访问权之后执行|
|BLOCKED|线程因为正在等待需要的锁而挂起执行|
|TERMINATED|线程已经完成执行|
|TIMED_WAITING|线程挂起执行一段指定的时间，例如当调用sleep()方法时就会处于这种状态。当调用wait()或join()方法的暂停版时，也会进入这种状态|
|WAITING|线程因为等待某些动作而挂起执行。例如，因为调用非暂停版的wait()或join()方法而等待时，会处于这种状态|

![这里写图片描述](http://upload-images.jianshu.io/upload_images/2552605-3eccba198c3ea615?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

其实，可以以更加通俗的方式来描述线程的生命周期：

 1. 新建状态:
使用 new 关键字建立一个线程对象后，线程就处于新建状态，线程保持这个状态直到程序 start() 这个线程
 2. 就绪状态:
当线程对象调用了 start() 方法之后，线程就进入就绪状态。就绪状态的线程处于就绪队列中，要等待 JVM 里线程调度器的调度
 3. 运行状态:
如果就绪状态的线程获取到 CPU 资源，就可以执行 run() 方法，此时线程便处于运行状态。处于运行状态的线程最为复杂，它可以变为阻塞状态、就绪状态和死亡状态
 4. 阻塞状态:
如果一个线程执行了sleep（睡眠）、suspend（挂起）等方法，失去所占用资源之后，该线程就从运行状态进入阻塞状态。在睡眠时间已到或获得设备资源后可以重新进入就绪状态。阻塞状态可以分为三种：
  -  等待阻塞：运行状态中的线程执行 wait() 方法，使线程进入到等待阻塞状态
  -  同步阻塞：线程在获取 synchronized 同步锁失败(因为同步锁被其他线程占用)
  -  其他阻塞：通过调用线程的 sleep() 或 join() 发出了 I/O 请求时，线程就会进入到阻塞状态。当sleep() 状态超时，join() 等待线程终止或超时，或者 I/O 处理完毕，线程重新转入就绪状态
 5. 死亡状态:
一个运行状态的线程完成任务或者其他终止条件发生时，该线程就切换到终止状态

##**五、线程的优先级**
线程调度程序根据线程优先级来决定每个线程应当何时运行。理论上，优先级高的线程比优先级低的线程会获得更多的CPU时间。但是，线程优先级不能保证线程执行的顺序，而且优先级设置也根据运行平台会有不同的表现
为了设置线程的优先级，需要使用如下方法来设置

```
public final void setPriority(int newPriority)
```
参数值 newPriority 指定了线程的优先级，取值必须在 MIN_PRIORITY 和  MAX_PRIORITY  之间，默认取值是 NORM_PRIORITY 

```
    /**
     * The minimum priority that a thread can have.
     */
    public final static int MIN_PRIORITY = 1;

   /**
     * The default priority that is assigned to a thread.
     */
    public final static int NORM_PRIORITY = 5;

    /**
     * The maximum priority that a thread can have.
     */
    public final static int MAX_PRIORITY = 10;
```
使用方法如下所示：

```
public class MyThread extends Thread {

	private int index;

	public MyThread(int index) {
		this.index = index;
	}

	@Override
	public void run() {
		for (int i = 0; i < 5; i++) {
			System.out.println("Index: " + index + "-----" + i);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
```
建立三个线程，分别设置不同的优先级
```
public class Test {

	public static void main(String[] args) {
		Thread thread1=new MyThread(1);
		Thread thread2=new MyThread(2);
		Thread thread3=new MyThread(3);
		thread1.setPriority(Thread.MIN_PRIORITY);
		thread2.setPriority(Thread.NORM_PRIORITY);
		thread3.setPriority(Thread.MAX_PRIORITY);
		thread1.start();
		thread2.start();
		thread3.start();
	}

}
```
输出结果为：

```
Index: 2-----0
Index: 1-----0
Index: 3-----0
Index: 3-----1
Index: 1-----1
Index: 2-----1
Index: 3-----2
Index: 1-----2
Index: 2-----2
Index: 3-----3
Index: 1-----3
Index: 2-----3
Index: 3-----4
Index: 2-----4
Index: 1-----4
```

##**六、线程同步**
因为多线程为程序引入了异步行为，所以必须提供一种在需要时强制同步的方法。当两个或多个线程需要访问共享的资源时，需要以某种方式来确保每次只有一个线程使用资源。例如，如果希望两个线程进行通信并共享某个复杂的数据结构（例如链表），就需要以某种方式来确保它们相互之间不会发生冲突。也就是说，当一个线程正在读取该数据结构时，必须阻止另外一个线程向该数据结构写入数据。
Java为同步提供了独特的，语言级的支持，同步的关键是监视器的概念，监视器是用作互斥锁的对象。在给定时刻，只有一个线程可以拥有监视器。当线程取得锁时，也就是进入了监视器。其他所有企图进入加锁监视器的线程都会被挂起，直到第一个线程退出监视器。
可以使用两种方法来同步代码，这两种方法都需要使用到 synchronized 关键字

###**6.1、synchronized 方法**
在Java中，所有对象都有与它们自身关联的隐式监视器。为了进入对象的隐式监视器，只需要调用 synchronized 关键字修饰过的方法。当某个线程进入同步方法中时，调用同一实例的该同步方法（或任何其他同步方法）的所有其他线程都必须等待。当线程从同步方法中返回时，线程就会退出监视器并将对象的控制权交给下一个等待线程。

这里来展示一个小例子

首先有一个 Call 类，在 action 方法中分段输出一段文字，在中间使线程睡眠一秒
```
public class Call {

	public Call() {

	}

	public void action(String message) {
		System.out.print("[ " + message);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println(" ]");
	}

}
```
在线程中调用 Call 对象的 action 方法
```
public class Caller extends Thread {

	private String message;

	private Call call;

	public Caller(String message, Call call) {
		this.message = message;
		this.call = call;
		start();
	}

	@Override
	public void run() {
		call.action(message);
	}

}
```
建立三个线程并调用同个 Call 对象
```
public class Test {

	public static void main(String[] args) {
		Call call=new Call();
		Caller caller1=new Caller("Hello",call);
		Caller caller2=new Caller("World",call);
		Caller caller3=new Caller("Synchronized",call);
	}

}
```
输出结果为：

```
[ Hello[ Synchronized[ World ]
 ]
 ]
```
可以看到，输出结果并不是一一对称的，说明Call对象被三个线程切换调用了
为了阻止三个线程在相同的时间调用同一个对象的同一个方法，即按顺序地调用 call() 方法，这就需要用到 synchronized 关键字了

修改 Call 类的 action() 方法，为其添加一个前缀

```
	public synchronized void action(String message) {
		System.out.print("[ " + message);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println(" ]");
	}
```
输出结果为：

```
[ Hello ]
[ World ]
[ Synchronized ]
```

需要注意的是，一旦线程进入了一个实例的同步方法，所有其他线程就都不能再进入相同实例的任何同步方法，但是仍然可以继续调用同一实例的非同步方法

###**6.2、synchronized 语句**
上面的例子并不完全能够满足实际情况，因为还存在这样一种情况： Call 不是由我们来创建的，而且我们也不能访问并修改源代码，但我们又希望同步对 action() 方法的访问，这时候该怎样办呢？
解决方案就是将 action() 的调用操作放到 synchronized 代码块中

```
	 synchronized(object){
 
	 }
```
当中， object 是对被同步对象的引用。 synchronized 代码块确保对 object 对象的成员方法的调用，只会在当前线程成功进入 object 的监视器之后发生

例如，可以将 Caller 类的 run() 方法修改为：

```
	@Override
	public void run() {
		synchronized (call) {
			call.action(message);
		}	
	}
```
其输出结果也将是：

```
[ Hello ]
[ Synchronized ]
[ World ]
```

##**七、死锁**
当两个线程循环依赖一对同步对象时，就会发生死锁（deadlock）。例如，假设一个线程进入对象A的监视器，另一个线程进入对象B的监视器。如果A中的线程试图调用B中的同步方法，就会由于监视器被占用而阻塞。如果此时B中的线程也试图调用A中的同步方法，那么也会由于同样的原因而被阻塞。
死锁是一种很难调试的错误，原因有两点：

 - 死锁通常很少发生，只有当两个线程恰好以这种方式获取CPU时钟周期时才会发生死锁
 - 死锁可能涉及更多的线程以及更多的同步对象
