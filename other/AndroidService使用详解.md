Service是Android系统中的四大组件之一，主要有两个应用场景：后台运行和跨进程访问。Service可以在后台执行长时间运行操作而不提供用户界面，除非系统必须回收内存资源，否则系统不会停止或销毁服务。服务可由其他应用组件启动，而且即使用户切换到其他应用，服务仍将在后台继续运行。 此外，组件可以绑定到服务，以与之进行交互，甚至是执行进程间通信 (IPC)
需要注意的是，Service是在主线程里执行操作的，可能会因为执行耗时操作而导致ANR

## **一、基础知识**

Service可以分为以下三种形式：

 - 启动
    当应用组件通过调用 **startService()** 启动服务时，服务即处于“启动”状态。一旦启动，服务即可在后台无限期运行，即使启动服务的组件已被销毁也不受影响。 已启动的服务通常是执行单一操作，而且不会将结果返回给调用方
 - 绑定
  当应用组件通过调用 **bindService()** 绑定到服务时，服务即处于“绑定”状态。绑定服务提供了一个客户端-服务器接口，允许组件与服务进行交互、发送请求、获取结果，甚至是利用进程间通信 (IPC) 跨进程执行这些操作。多个组件可以同时绑定服务，服务只会在组件与其绑定时运行，一旦该服务与所有组件之间的绑定全部取消，系统便会销毁它
 - 启动且绑定
  服务既可以是启动服务，也允许绑定。此时需要同时实现以下回调方法：**onStartCommand()**和 **onBind()**。系统不会在所有客户端都取消绑定时销毁服务。为此，必须通过调用 **stopSelf()** 或 **stopService()** 显式停止服务

无论应用是处于启动状态还是绑定状态，或者处于启动且绑定状态，任何应用组件均可像使用 Activity 那样通过调用 Intent 来使用服务（即使此服务来自另一应用），也可以通过清单文件将服务声明为私有服务，阻止其他应用访问

要使用服务，必须继承Service类（或者Service类的现有子类），在子类中重写某些回调方法，以处理服务生命周期的某些关键方面并提供一种机制将组件绑定到服务

 - onStartCommand()
  当组件通过调用 **startService()** 请求启动服务时，系统将调用此方法（如果是绑定服务则不会调用此方法）。一旦执行此方法，服务即会启动并可在后台无限期运行。 在指定任务完成后，通过调用 **stopSelf()** 或 **stopService()** 来停止服务
 - onBind()
  当一个组件想通过调用 **bindService()** 与服务绑定时，系统将调用此方法（如果是启动服务则不会调用此方法）。在此方法的实现中，必须通过返回 **IBinder** 提供一个接口，供客户端用来与服务进行通信
 - onCreate()
  首次创建服务时，系统将调用此方法来执行初始化操作（在调用 **onStartCommand()** 或 **onBind()** 之前）。如果在启动或绑定之前Service已在运行，则不会调用此方法
 - onDestroy()
  当服务不再使用且将被销毁时，系统将调用此方法，这是服务接收的最后一个调用，在此方法中应清理占用的资源

仅当内存过低必须回收系统资源以供前台 Activity 使用时，系统才会强制停止服务。如果将服务绑定到前台 Activity，则它不太可能会终止，如果将服务声明为在前台运行，则它几乎永远不会终止。或者，如果服务已启动并要长时间运行，则系统会随着时间的推移降低服务在后台任务列表中的位置，而服务也将随之变得非常容易被终止。如果服务是启动服务，则必须将其设计为能够妥善处理系统对它的重启。 如果系统终止服务，那么一旦资源变得再次可用，系统便会重启服务（这还取决于 onStartCommand() 的返回值）

## **二、声明Service**

如同其他组件一样，想要使用Service，必须在清单文件中对其进行声明
声明方式是添加 **< service >** 元素作为 **< application >** 元素的子元素
例如

```java
  <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/AppTheme">
        
        <service android:name=".MyService" />
        
    </application>
```
**android:name** 属性是唯一必需的属性，用于指定服务的类名，还可将其他属性包括在 **< service >** 元素中以定义一些特性

为了确保应用的安全性，最好始终使用显式 Intent 启动或绑定 Service，且不要为服务声明 Intent 过滤器。 启动哪个服务存在一定的不确定性，而如果对这种不确定性的考量非常有必要，则可为服务提供 Intent 过滤器并从 Intent 中排除相应的组件名称，但随后必须使用 **setPackage()** 方法设置 Intent 的软件包，这样可以充分消除目标服务的不确定性

此外，还可以通过添加 **android:exported** 属性并将其设置为 "**false**"，确保服务仅适用于本应用。这可以有效阻止其他应用启动本应用内的服务，即便在使用显式 Intent 时也是如此

Service包含的属性有

```java
<service android:description="string resource"
         android:directBootAware=["true" | "false"]
         android:enabled=["true" | "false"]
         android:exported=["true" | "false"]
         android:icon="drawable resource"
         android:isolatedProcess=["true" | "false"]
         android:label="string resource"
         android:name="string"
         android:permission="string"
         android:process="string" >
</service>
```

|      属性       |                             说明                             |
| :-------------: | :----------------------------------------------------------: |
|   description   | 对服务进行描述，属性值应为对字符串资源的引用，以便进行本地化 |
| directBootAware |     设置是否可以在用户解锁设备之前运行，默认值为“false”      |
|     enabled     | 设置是否可以由系统来实例化服务。< application >元素有自己的enabled属性，适用于包括服务在内的所有应用程序组件。要启用服务，< application >和< service >属性必须都为“true”（默认情况下都为true）。如果其中一个是“false”，则服务被禁用 |
|    exported     | 设置其他应用程序的组件是否可以调用本服务或与其交互，如果可以，则为“true”。当值为“false”时，只有同一个应用程序或具有相同用户ID的应用程序的组件可以启动该服务或绑定到该服务。该属性的默认值取决于服务是否包含Intent filters。没有任何过滤器意味着它只能通过指定其确切的类名来调用，这意味着该服务仅用于应用程序内部使用（因为其他人不知道类名）。所以在这种情况下，默认值为“false”。 另一方面，如果存在至少一个过滤器，意味着该服务打算供外部使用，因此默认值为“true” |
|      icon       | 服务的图标，属性值应是对drawable资源的引用。如果未设置，则将使用应用程序图标 |
| isolatedProcess | 设置该服务是否作为一个单独的进程运行，如果设置为true，此服务将在与系统其余部分隔离的特殊进程下运行，并且没有自己的权限，与它唯一的通信是通过服务API（绑定和启动） |
|      label      |   可以向用户显示的服务的名称，属性值应是对字符串资源的引用   |
|      name       |                      服务类的完全限定名                      |
|   permission    | 设定组件必须具有的权限，得以启动服务或绑定服务。如果startService（），bindService（）或stopService（）的调用者没有被授予此权限，则该方法将不会工作，并且Intent对象不会传递到服务中 |
|     process     | 用来运行服务的进程的名称。通常，应用程序的所有组件都运行在应用程序创建的默认进程中，它与应用程序包名具有相同的名称。 < application >元素的process属性可以为所有组件设置不同的默认值，但组件可以使用自己的进程属性覆盖默认值，从而允许跨多个进程扩展应用程序 |

## **三、启动Service**

启动服务由组件通过调用 **startService()** 启动，服务启动之后，其生命周期即独立于启动它的组件，并且可以在后台无限期地运行，即使启动服务的组件已被销毁也不受影响。因此，服务应通过调用 **stopSelf()** 来自行停止运行，或者由另一个组件调用 **stopService()** 来停止

可以通过扩展两个类来创建启动服务：

 - Service
  这是所有服务的父类。扩展此类时，如果要执行耗时操作，必须创建一个用于执行操作的新线程，因为默认情况下服务将运行于UI线程
 - IntentService
  这是 Service 的子类，它使用工作线程逐一处理所有启动请求。如果应用不需要同时处理多个请求，这是最好的选择。IntentService只需实现**构造函数**与 **onHandleIntent()** 方法即可，onHandleIntent()方法会接收每个启动请求的 Intent

### **3.1、继承Service**

这里举一个音乐播放器的例子
继承Service类实现自定义Service，提供在后台播放音乐、暂停音乐、停止音乐的方法

```java
public class MyService extends Service {

    private final String TAG = "MyService";

    private MediaPlayer mediaPlayer;

    private int startId;

    public enum Control {
        PLAY, PAUSE, STOP
    }

    public MyService() {
    }

    @Override
    public void onCreate() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.music);
            mediaPlayer.setLooping(false);
        }
        Log.e(TAG, "onCreate");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.startId = startId;
        Log.e(TAG, "onStartCommand---startId: " + startId);
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Control control = (Control) bundle.getSerializable("Key");
            if (control != null) {
                switch (control) {
                    case PLAY:
                        play();
                        break;
                    case PAUSE:
                        pause();
                        break;
                    case STOP:
                        stop();
                        break;
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        super.onDestroy();
    }

    private void play() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    private void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    private void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
        stopSelf(startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "onBind");
        throw new UnsupportedOperationException("Not yet implemented");
    }

}
```

在布局中添加三个按钮，用于控制音乐播放、暂停与停止

```java
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void playMusic(View view) {
        Intent intent = new Intent(this, MyService.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("Key", MyService.Control.PLAY);
        intent.putExtras(bundle);
        startService(intent);
    }

    public void pauseMusic(View view) {
        Intent intent = new Intent(this, MyService.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("Key", MyService.Control.PAUSE);
        intent.putExtras(bundle);
        startService(intent);
    }

    public void stopMusic(View view) {
        Intent intent = new Intent(this, MyService.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("Key", MyService.Control.STOP);
        intent.putExtras(bundle);
        startService(intent);
        //或者是直接如下调用
        //Intent intent = new Intent(this, MyService.class);
        //stopService(intent);
    }
    
}
```
在清单文件中声明Service，为其添加**label**标签，便于在系统中识别Service

```java
      <service
          android:name=".MyService"
          android:label="@string/app_name" />
```

![这里写图片描述](http://upload-images.jianshu.io/upload_images/2552605-c3690e2316b8f9a4?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

点击“播放音乐”按钮后，在后台将会运行着名为“Service测试”的服务

![这里写图片描述](http://upload-images.jianshu.io/upload_images/2552605-323881d8767e934e?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

通过Log日志可以发现，多次点击“播放音乐”按钮，“onCreate()”方法只会在初始时调用一次，“onStartCommand(Intent intent, int flags, int startId)”方法会在每次点击时都被调用，点击“停止音乐”按钮，“onDestroy()”方法会被调用

当中，每次回调onStartCommand()方法时，参数“startId”的值都是递增的，startId用于唯一标识每次对Service发起的处理请求
如果服务同时处理多个 onStartCommand() 请求，则不应在处理完一个启动请求之后立即销毁服务，因为此时可能已经收到了新的启动请求，在第一个请求结束时停止服务会导致第二个请求被终止。为了避免这一问题，可以使用 stopSelf(int) 确保服务停止请求始终基于最新一次的启动请求。也就是说，如果调用 stopSelf(int) 方法的参数值与onStartCommand()接受到的最新的startId值不相符的话，stopSelf()方法就会失效，从而避免终止尚未处理的请求

如果服务没有提供绑定，则使用 **startService()** 传递的 Intent 是应用组件与服务之间唯一的通信模式。如果希望服务返回结果，则启动服务的客户端可以为广播创建一个 **PendingIntent** （使用 getBroadcast()），并通过启动服务的 Intent 传递给服务。然后，服务就可以使用广播传递结果

当中，**onStartCommand()** 方法必须返回一个整数，用于描述系统应该如何应对服务被杀死的情况，返回值必须是以下常量之一：

 - START_NOT_STICKY
  如果系统在 onStartCommand() 返回后终止服务，则除非有挂起 Intent 要传递，否则系统不会重建服务。这是最安全的选项，可以避免在不必要时以及应用能够轻松重启所有未完成的作业时运行服务
 - START_STICKY
  如果系统在 onStartCommand() 返回后终止服务，则会重建服务并调用 onStartCommand()，但不会重新传递最后一个 Intent。相反，除非有挂起 Intent 要启动服务（在这种情况下，将传递这些 Intent ），否则系统会通过空 Intent 调用 onStartCommand()。这适用于不执行命令、但无限期运行并等待作业的媒体播放器（或类似服务）
 - START_REDELIVER_INTENT
  如果系统在 onStartCommand() 返回后终止服务，则会重建服务，并通过传递给服务的最后一个 Intent 调用 onStartCommand()。任何挂起 Intent 均依次传递。这适用于主动执行应该立即恢复的作业（例如下载文件）的服务

### **3.2、IntentService**

由于大多数启动服务都不必同时处理多个请求，因此使用 IntentService 类实现服务也许是最好的选择

IntentService 执行以下操作：

 - 创建默认的工作线程，用于在应用的主线程外执行传递给 onStartCommand() 的所有 Intent
 - 创建工作队列，用于将 Intent 逐一传递给 onHandleIntent() 实现，这样就不必担心多线程问题
 - 在处理完所有启动请求后停止服务，因此不必自己调用 stopSelf()方法
 - 提供 onBind() 的默认实现（返回 null）
 - 提供 onStartCommand() 的默认实现，可将 Intent 依次发送到工作队列和 onHandleIntent() 

因此，只需实现构造函数与 onHandleIntent() 方法即可

这里举一个关于输出日志的例子

```java
public class MyIntentService extends IntentService {

    private final String TAG = "MyIntentService";

    public MyIntentService() {
        super("MyIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            for (int i = 0; i < 5; i++) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.e(TAG, bundle.getString("key", "默认值"));
            }
        }
    }

}
```

```java
public class StartIntentServiceActivity extends AppCompatActivity {

    private int i = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_intent_service);
    }

    public void startService(View view) {
        Intent intent = new Intent(this, MyIntentService.class);
        Bundle bundle = new Bundle();
        bundle.putString("key", "当前值：" + i++);
        intent.putExtras(bundle);
        startService(intent);
    }

}
```
当中，**startService(View view)**方法与一个Button绑定，连续快速地多次点击Button，验证**IntentService**当中的日志是否依次输出，还是交叉着输出

可以看到是依次输出的，即IntentService的工作线程是逐一处理所有启动请求的

![这里写图片描述](http://upload-images.jianshu.io/upload_images/2552605-b33102bf7825eda6?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

此外，查看后台，可以看到当前后台应用程序进程中有两个服务

![这里写图片描述](http://upload-images.jianshu.io/upload_images/2552605-fbcce2e5b5bcfa77?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

## **四、绑定Service**

应用组件（客户端）通过调用 **bindService()** 绑定到服务，绑定是异步的，系统随后调用服务的 **onBind()** 方法，该方法返回用于与服务交互的 **IBinder**。要接收 **IBinder**，客户端必须提供一个 **ServiceConnection** 实例用于监控与服务的连接，并将其传递给 **bindService()**。当 Android 系统创建了客户端与服务之间的连接时，会回调**ServiceConnection** 对象的**onServiceConnected()**方法，向客户端传递用来与服务通信的 **IBinder**

多个客户端可同时连接到一个服务。不过，只有在第一个客户端绑定时，系统才会调用服务的 onBind() 方法来检索 IBinder。系统随后无需再次调用 onBind()，便可将同一 IBinder 传递至其他绑定的客户端。当所有客户端都取消了与服务的绑定后，系统会将服务销毁（除非 startService() 也启动了该服务）

另外，只有 Activity、服务和内容提供者可以绑定到服务，无法从广播接收器绑定到服务

可以通过以下三种方法定义IBinder接口：

 - 扩展 Binder 类
  如果服务是供本应用专用，并且运行在与客户端相同的进程中，则应通过扩展 Binder 类并从 onBind() 返回它的一个实例来创建接口。客户端收到 Binder 后，可利用它直接访问 Service 中可用的公共方法
 - 使用 Messenger
    如需让接口跨不同的进程工作，则可使用 Messenger 为服务创建接口。服务可以这种方式定义对应于不同类型 Message 对象的 Handler。此 Handler 是 Messenger 的基础，后者随后可与客户端分享一个 IBinder，从而让客户端能利用 Message 对象向服务发送命令。此外，客户端还可定义自有 Messenger，以便服务回传消息。这是执行进程间通信 (IPC) 的最简单方法，因为 Messenger 会在单一线程中创建包含所有请求的队列，这样就不必对服务进行线程安全设计
 - 使用 AIDL
  AIDL（Android 接口定义语言）执行所有将对象分解成原语的工作，操作系统可以识别这些原语并将它们编组到各进程中，以执行 IPC。 之前采用 Messenger 的方法实际上是以 AIDL 作为其底层结构。 如上所述，Messenger 会在单一线程中创建包含所有客户端请求的队列，以便服务一次接收一个请求。 不过，如果想让服务同时处理多个请求，则可直接使用 AIDL。 在此情况下，服务必须具备多线程处理能力，并采用线程安全式设计。如需直接使用 AIDL，必须创建一个定义编程接口的 .aidl 文件。Android SDK 工具利用该文件生成一个实现接口并处理 IPC 的抽象类，随后可在服务内对其进行扩展

### **4.1、绑定服务的具体步骤：**

#### **4.1.1、扩展 Binder 类**

如果服务仅供本地应用使用，不需要跨进程工作，则可以实现自有 Binder 类，让客户端通过该类直接访问服务中的公共方法。此方法只有在客户端和服务位于同一应用和进程内这一最常见的情况下方才有效
以下是具体的设置方法：

- 在服务中创建一个可满足下列任一要求的 Binder 实例：
    - 包含客户端可调用的公共方法
    - 返回当前 Service 实例，其中包含客户端可调用的公共方法
    - 或返回由服务承载的其他类的实例，其中包含客户端可调用的公共方法

- 从 onBind() 回调方法返回此 Binder 实例
- 在客户端中，从 onServiceConnected() 回调方法接收 Binder，并使用提供的方法调用绑定服务

#### **4.1.2、实现 ServiceConnection接口**

重写两个回调方法：

 - onServiceConnected()
  系统会调用该方法以传递服务的onBind() 方法返回的 IBinder
 - onServiceDisconnected()
  Android 系统会在与服务的连接意外中断时，例如当服务崩溃或被终止时调用该方法。当客户端取消绑定时，系统不会调用该方法

#### **4.1.3、调用 bindService()，传递 ServiceConnection 对象**

#### **4.1.4、当系统调用了 onServiceConnected() 的回调方法时，就可以通过IBinder对象操作服务了**

#### **4.1.5、要断开与服务的连接需调用 unbindService()方法。如果应用在客户端仍处于绑定状态时销毁客户端，会导致客户端取消绑定，更好的做法是在客户端与服务交互完成后立即取消绑定客户端，这样可以关闭空闲服务**


示例代码：
```java
public class MyBindService extends Service {

    private IBinder myBinder;

    private Random mGenerator;

    private final String TAG = "MyBindService";

    public class MyBinder extends Binder {
        MyBindService getService() {
            return MyBindService.this;
        }
    }

    @Override
    public void onCreate() {
        Log.e(TAG, "onCreate");
        myBinder = new MyBinder();
        mGenerator = new Random();
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "onBind");
        return myBinder;
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.e(TAG, "onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        Log.e(TAG, "onRebind");
        super.onRebind(intent);
    }

    public int getRandomNumber() {
        return mGenerator.nextInt(100);
    }

}
```

```java
public class BindServiceActivity extends AppCompatActivity {

    private MyBindService mService;

    private boolean mBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bind_service);
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MyBindService.MyBinder binder = (MyBindService.MyBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };

    public void bindService(View view) {
        Intent intent = new Intent(this, MyBindService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    public void unBindService(View view) {
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    public void getData(View view) {
        if (mBound) {
            Toast.makeText(this, "获取到的随机数：" + mService.getRandomNumber(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "服务未绑定", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

}
```

### **4.2、绑定服务的生命周期**

绑定服务的生命周期在同时启动服务的情况下比较特殊，想要终止服务，除了需要取消绑定服务外，还需要服务通过 stopSelf() 自行停止或其他组件调用 stopService() 

其中，如果服务已启动并接受绑定，则当系统调用了onUnbind() 方法，想要在客户端下一次绑定到服务时调用 onRebind() 方法的话，则onUnbind() 方法需返回 true。onRebind() 返回空值，但客户端仍可以在其 onServiceConnected() 回调中接收到 IBinder对象

![这里写图片描述](http://upload-images.jianshu.io/upload_images/2552605-568542042002a6d1?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

### **4.3、绑定时机**

 - 如果只需要在 Activity 可见时与服务交互，则应在 onStart() 期间绑定，在 onStop() 期间取消绑定
 - 如果希望 Activity 在后台停止运行状态下仍可接收响应，则可在 onCreate() 期间绑定，在 onDestroy() 期间取消绑定。这意味着 Activity 在其整个运行过程中（包括后台运行期间）都需要使用此服务
 - 通常情况下，切勿在 Activity 的 onResume() 和 onPause() 期间绑定和取消绑定，因为每一次生命周期转换都会发生这些回调，应该使发生在这些转换期间的处理保持在最低水平。假设有两个Activity需要绑定到同一服务，从Activity  A跳转到Activity  B，这个过程中会依次执行A-onPause，B-onCreate，B-onStart，B-onResume，A-onStop。这样系统会在A-onPause的时候销毁服务，又在B-onResume的时候重建服务。当Activity  B回退到Activity  A时，会依次执行B-onPause，A-onRestart，A-onStart，A-onResume，B-onStop，B-onDestroy。此时，系统会在B-onPause时销毁服务，又在A-onResume时重建服务。这样就造成了多次的销毁与重建，因此需要选定好绑定服务与取消绑定服务的时机



## **五、在前台运行Service**

前台服务被认为是用户主动意识到的一种服务，因此在内存不足时，系统也不会考虑将其终止。 前台服务必须在状态栏提供通知，放在“正在进行”标题下方，这意味着除非服务停止或从前台移除，否则不能清除通知

要请求让服务运行于前台，要调用 **startForeground()**方法，两个参数分别是：唯一标识通知的int类型整数和Notification对象

修改MyService当中的play()方法

```java
    private void play() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
            mBuilder.setSmallIcon(R.drawable.bird);
            mBuilder.setContentTitle("这是标题吧~叶应是叶");
            mBuilder.setContentText("http://blog.csdn.net/new_one_object");
            startForeground(1, mBuilder.build());
        }
    }
```
点击播放音乐后，状态栏就出现了一个通知

![这里写图片描述](http://upload-images.jianshu.io/upload_images/2552605-699e4b1e1c8cfef4?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

当中，提供给 startForeground() 的整型参数不得为 0。要从前台移除服务，需调用 stopForeground()方法，此方法不会停止服务。 但是，如果前台服务被停止，则通知也会被移除

## **六、Service的生命周期**

服务生命周期从创建到销毁可以遵循两条不同的路径：

 - 启动服务
  该服务在其他组件调用 startService() 时创建，然后无限期运行，必须通过调用 stopSelf() 来自行停止运行或通过其他组件调用 stopService() 来停止服务。服务停止后，系统会将其销毁
 - 绑定服务
  服务在另一个组件（客户端）调用 bindService() 时创建。然后，客户端通过 IBinder 接口与服务进行通信。客户端可以通过调用 unbindService() 关闭连接。多个客户端可以绑定到相同服务，而且当所有绑定全部取消后，系统即会销毁该服务（服务不必自行停止运行）

这两条路径并非完全独立。也就是说，可以绑定到已经使用 startService() 启动的服务。例如，可以通过使用 Intent（标识要播放的音乐）调用 startService() 来启动后台音乐服务。随后，可能在用户需要稍加控制播放器或获取有关当前播放歌曲的信息时，Activity 可以通过调用 bindService() 绑定到服务。在这种情况下，除非所有客户端均取消绑定，否则 stopService() 或 stopSelf() 不会实际停止服务
