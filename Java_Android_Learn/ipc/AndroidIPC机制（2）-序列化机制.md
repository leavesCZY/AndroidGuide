IPC（Inter-Process Communication）的含义即为进程间通信或者翻译为跨进程通信，是指两个进程之间进行数据交换的过程。一般情况下，在 Android 系统中一个应用就只享有一个进程，在最简单的情况下一个进程可以只包含有一个线程（当然，一般情况下是不可能的），即主线程，也称为 UI 线程

有时候应用因为某些原因需要采用多进程模式，此时如果要在应用内的不同进程间进行通信，就需要使用到 IPC 机制。或者是两个不同的应用需要进行数据交换，此时也一样需要依靠 Android 系统提供的 IPC 方案

本篇以及后续几篇文章，都会对 Android 系统下的 IPC 机制进行介绍，部分内容来自我对 `Android开发艺术探索` 该书的理解。

本篇文章就先介绍 Android 系统开启多进程的方法以及对象的序列化方法

### 一、开启多进程

为一个 Android 应用开启多进程模式的方法有两种。第一种方法是在 AndroidMenifest 中为四大组件指定 **android:process** 属性，为其声明要在哪个进程名下运行，即可开启多进程。第二种方法是通过 JNI 在 native 层中 fork 一个新的进程。这里只讨论第一种方法。

Android 应用默认在命名为包名的进程下运行，除非你为其指定了 **android:process** 属性
例如，这里创建一个应用，包名为 **com.czy.ipc** ，再指定四大组件之一的 Service 运行在其它进程下

```java
        <activity android:name=".MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".MyService"
            android:process="com.czy.process.test" />
```
并在启动 MainActivity 的同时启动 MyService ，这样，在系统进程列表中就可以看到这两个相关的进程
![](https://upload-images.jianshu.io/upload_images/2552605-06601ada5827dab8.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


虽然开启多进程的方法并不算麻烦，但当应用开启了多进程后，其实会对来很多的负面影响，主要有以下几个：

 - 静态成员与单例模式失效
 - 线程同步机制失效
 - SharedPreferences 可靠性下降
 - Application 被创建多次

为了解决多进程带来的问题，系统也为开发者提供了很多的跨进程通信方式，比如文件共享、ContentProvider、Messenger、AIDL、Socket 等

### 二、Serializable 

跨进程通信的目的就是为了进行数据交换，但并不是所有的数据类型都能被传递，除了基本数据类型外，还必须是实现了序列化和反序列化的数据类型才可以，即实现了 Serializable 接口或 Parcelable 接口的数据类型

Serializable 接口是由 Java 所提供的一个序列化接口，是一个空接口，为对象提供了标准的序列化和反序列化接口。类只要实现了该接口，即可自动实现默认的序列化过程。

```java
package java.io;

public interface Serializable {
}

```
此外，为了辅助系统完成对象的序列化和反序列化过程，还可以声明一个 `long` 型数据 `serivalVersionUID` 

```java
private static final long serivalVersionUID = 123456578689L;
```
序列化时系统会把对象的信息以及 serivalVersionUID 一起保存到某种介质中（例如文件或内存中），当反序列化时就会把介质中的 serivalVersionUID 与类中声明的 serivalVersionUID 进行对比，如果两者相同则说明序列化的类与当前类的版本是相同的，则可以序列化成功。如果两者不相等，则说明当前类的版本已经变化（可能是新增或删减了某个方法），则会导致序列化失败

如果没有手动声明 serivalVersionUID ，编译工具则会根据当前类的结构自动去生成 serivalVersionUID ，这样在反序列化时只有类的结构完全保持一致才能反序列化成功

为了当类的结构没有发生结构性变化时依然能够反序列化成功，一般是手动为 serivalVersionUID 指定一个固定的值。这样即使类增删了某个变量或方法体时，依然能够最大程度地恢复数据。当然，类的结构不能发生太大变化，否则依然会导致反序列化失败

此外，静态成员变量属于类不属于对象，所以不会参与序列化过程，用 transient 关键字标记的成员变量也不会参与序列化过程

### 三、Parcelable
Parcelable 接口是由 Android 系统提供的序列化接口，官方也推荐使用 Parcelable 进行序列化操作，Bundle 、 Intent 和 Bitmap 等都实现了 Parcelable 接口。Parcelable 接口相比 Serializable 更为高效，但实现方式也相比麻烦些
下面看个例子

```java
/**
 * 作者：叶应是叶
 * 时间：2018/3/18 11:08
 * 描述：
 */
public class Demo implements Parcelable {

    private String stringField;

    private int intField;

    private boolean booleanField;

    public Demo(String stringField, int intField, boolean booleanField) {
        this.stringField = stringField;
        this.intField = intField;
        this.booleanField = booleanField;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.stringField);
        dest.writeInt(this.intField);
        dest.writeByte(this.booleanField ? (byte) 1 : (byte) 0);
    }

    protected Demo(Parcel in) {
        this.stringField = in.readString();
        this.intField = in.readInt();
        this.booleanField = in.readByte() != 0;
    }

    public static final Creator<Demo> CREATOR = new Creator<Demo>() {
        @Override
        public Demo createFromParcel(Parcel source) {
            return new Demo(source);
        }

        @Override
        public Demo[] newArray(int size) {
            return new Demo[size];
        }
    };
}
```
实现 Parcelable 接口需要实现以上四个方法，用于进行序列化、反序列化和内容描述。一般我们也不需要手动实现 Parcelable 接口，可以通过 Android Studio的一个插件：Android Parcelable code generator 来自动完成
![](https://upload-images.jianshu.io/upload_images/2552605-d8b1687a60d93dbc.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

**这里提供本系列所有的 IPC 示例代码：[IPC_Demo](https://github.com/leavesC/IPC_Demo)**