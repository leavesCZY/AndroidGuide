Android 应用不可避免的都会发生 crash，即程序崩溃

可能是系统或程序有 bug 等内部原因，或者是网络状况不佳等外部原因

当应用发生 crash 时，如果只是你一个人使用的应用，那自然容易检测出原因，可是如果应用有广泛的使用者，面对市面上众多的 Rom 和机型，就需要一个个获取发生 crash 时的系统情况了，将异常信息记录下来并发送到服务器，供开发者了解情况并调试

Android 提供有默认的异常处理方法，也可以自定义异常处理方法

在 Thread 类下有如下方法用来设置自定义异常处理器

```java
public static void setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler handler) {
    Thread.defaultUncaughtHandler = handler;
}
```

当 crash 发生时，系统会回调 UncaughtExceptionHandler 的如下方法，我们就需要来重写该方法

```java
public void uncaughtException(Thread thread, Throwable throwable){

}
```

因此，现在需要进行的步骤是：

1. 实现 Thread.UncaughtExceptionHandler 接口，自定义异常处理器
2. 重写 uncaughtException 方法
3. 在 uncaughtException 方法中将 crash 信息以及当前手机信息保存到文本中，然后将文本打包发送到服务器

新建 CrashHandler 类继承于 Thread.UncaughtExceptionHandler

```java
/**
 * Crash处理 leavesC
 * Created by ZY on 2016/8/27.
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {

    //定义文件存放路径
    private static final String PATH = Environment.getExternalStorageDirectory().getPath() + "/CrashInfo/";

    //定义文件后缀
    private static final String FILE_NAME_SUFFIX = ".txt";

    //系统默认的异常处理器
    private Thread.UncaughtExceptionHandler defaultCrashHandler;

    private static final String TAG = "CrashHandler";

    private static CrashHandler crashHandler = new CrashHandler();

    //私有化构造函数
    private CrashHandler() {
    }

    //获取实例
    public static CrashHandler getInstance() {
        return crashHandler;
    }

    public void init() {
        defaultCrashHandler = Thread.getDefaultUncaughtExceptionHandler();
        //设置系统的默认异常处理器
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        //记录异常信息到本地文本中
        dumpExceptionToSDCard(throwable);
        if (defaultCrashHandler != null) {
            //如果在自定义异常处理器之前，系统有自己的默认异常处理器的话，调用它来处理异常信息
            defaultCrashHandler.uncaughtException(thread, throwable);
        } else {
            Process.killProcess(Process.myPid());
        }
    }

    //记录异常信息到本地文本中
    private void dumpExceptionToSDCard(Throwable throwable) {
        //如果SD卡非正常挂载，则用Log输出异常信息
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Log.e(TAG, "SD卡出错");
            return;
        }
        File dir = new File(PATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        long currentTime = System.currentTimeMillis();
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(currentTime));
        //建立记录Crash信息的文本
        File file = new File(PATH + time + FILE_NAME_SUFFIX);
        try {
            PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(file)));
            printWriter.println(time);
            dumpPhoneInfo(printWriter);
            printWriter.println();
            throwable.printStackTrace(printWriter);
            printWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "记录Crash信息失败");
        }
    }

    //记录手机信息
    private void dumpPhoneInfo(PrintWriter printWriter) {
        //系统版本号
        printWriter.print("OS Version:");
        printWriter.print(Build.VERSION.RELEASE);
        printWriter.print("_");
        printWriter.println(Build.VERSION.SDK_INT);
        //硬件制造商
        printWriter.print("Vendor:");
        printWriter.println(Build.MANUFACTURER);
        //系统定制商
        printWriter.print("Brand:");
        printWriter.println(Build.BRAND);
    }

}

```

采用了单例模式，每个方法的作用也都注释了其作用

然后，选择在 Application 中初始化 CrashHandler

所以需要自定义 Application

```java
/**
 * Created by ZY on 2016/8/27.
 */
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init();
    }

}
```

需要在 AndroidManifest.xml 文件中声明之
即在 application 标签下添加以下一行语句

```java
android:name=".MyApplication"
```

此外，因为还涉及到了内存卡写操作，所以需要声明下权限

```java
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

然后，在主布局文件中声明个按钮，在点击后抛出一个自定义异常

```java
        findViewById(R.id.crashTest).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                throw new RuntimeException("自定义异常");
            }
        });
```

点击后程序异常退出，打开手机的文件内容管理器

可以看到多了一个 CrashInfo 文件夹

![](http://upload-images.jianshu.io/upload_images/2552605-2986c9e9e273a543?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

保存异常信息的文件

![](http://upload-images.jianshu.io/upload_images/2552605-fbb4ed4b1154c67e?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

异常信息

![](http://upload-images.jianshu.io/upload_images/2552605-b8bc98f98ad5c67c?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

这要，就获取到了异常信息，这里只是将信息保存到了SD卡上，在实际开发中再添加上传到服务器的操作即可