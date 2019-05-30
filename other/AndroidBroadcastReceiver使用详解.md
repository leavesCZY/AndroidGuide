在 Android 系统中，广播（Broadcast）是在组件之间传播数据的一种机制，这些组件可以位于不同的进程中，起到进程间通信的作用

**BroadcastReceiver** 是对发送出来的 **Broadcast** 进行过滤、接受和响应的组件。首先将要发送的消息和用于过滤的信息（Action，Category）装入一个 **Intent** 对象，然后通过调用 **Context.sendBroadcast()** 、 **sendOrderBroadcast()** 方法把 Intent 对象以广播形式发送出去。 广播发送出去后，所以已注册的 BroadcastReceiver 会检查注册时的 **IntentFilter** 是否与发送的 Intent 相匹配，若匹配则会调用 BroadcastReceiver 的 **onReceiver()** 方法

所以当我们定义一个 BroadcastReceiver 的时候，都需要实现 onReceiver() 方法。BroadcastReceiver 的生命周期很短，在执行 onReceiver() 方法时才有效，一旦执行完毕，该Receiver 的生命周期就结束了

Android中的广播分为两种类型，标准广播和有序广播

 - 标准广播
  标准广播是一种完全异步执行的广播，在广播发出后所有的广播接收器会在同一时间接收到这条广播，之间没有先后顺序，效率比较高，且无法被截断
 - 有序广播
  有序广播是一种同步执行的广播，在广播发出后同一时刻只有一个广播接收器能够接收到， 优先级高的广播接收器会优先接收，当优先级高的广播接收器的 onReceiver() 方法运行结束后，广播才会继续传递，且前面的广播接收器可以选择截断广播，这样后面的广播接收器就无法接收到这条广播了

## **一、静态注册**

静态注册即在**清单文件**中为 **BroadcastReceiver** 进行注册，使用**< receiver >**标签声明，并在标签内用 **< intent-filter >** 标签设置过滤器。这种形式的 BroadcastReceiver 的生命周期伴随着整个应用，如果这种方式处理的是系统广播，那么不管应用是否在运行，该广播接收器都能接收到该广播

### **1.1、发送标准广播**

首先，继承 BroadcastReceiver 类创建一个用于接收标准广播的Receiver，在 onReceive() 方法中取出 Intent 传递来的字符串

```java
public class NormalReceiver extends BroadcastReceiver {

    private static final String TAG = "NormalReceiver";

    public NormalReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String msg = intent.getStringExtra("Msg");
        Log.e(TAG, msg);
    }

}
```
在清单文件中声明的 BroadcastReceiver ，必须包含值为 **NORMAL_ACTION** 字符串的 **action** 属性，该广播接收器才能收到以下代码中发出的广播

发送标准广播调用的是 **sendBroadcast(Intent)** 方法
```java
public class MainActivity extends AppCompatActivity {

    private final String NORMAL_ACTION = "com.example.normal.receiver";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void sendBroadcast(View view) {
        Intent intent = new Intent(NORMAL_ACTION);
        intent.putExtra("Msg", "Hi");
        sendBroadcast(intent);
    }

}
```

在清单文件中注册 BroadcastReceiver

```java
    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/AppTheme">
        <activity android:name=".MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <receiver android:name=".NormalReceiver">
            <intent-filter>
                <action android:name="com.example.normal.receiver" />
            </intent-filter>
        </receiver>
        
    </application>
```

### **1.2、发送有序广播**

首先，继承 BroadcastReceiver 类创建三个用于接收有序广播的Receiver，名字依次命名为 OrderReceiver_1、OrderReceiver_2、OrderReceiver_3。此外，既然 Receiver 在接收广播时存在先后顺序，那么 Receiver 除了能从发送广播使用的 Intent 接收数据外，优先级高的 Receiver 也能在处理完操作后向优先级低的 Receiver 传送处理结果

```java
public class OrderReceiver_1 extends BroadcastReceiver {

    private final String TAG = "OrderReceiver_1";

    public OrderReceiver_1() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG, "OrderReceiver_1被调用了");

        //取出Intent当中传递来的数据
        String msg = intent.getStringExtra("Msg");
        Log.e(TAG, "OrderReceiver_1接收到的值： " + msg);

        //向下一优先级的Receiver传递数据
        Bundle bundle = new Bundle();
        bundle.putString("Data", "（Hello）");
        setResultExtras(bundle);
    }
}
```

```java
public class OrderReceiver_2 extends BroadcastReceiver {

    private final String TAG = "OrderReceiver_2";

    public OrderReceiver_2() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG, "OrderReceiver_2被调用了");

        //取出上一优先级的Receiver传递来的数据
        String data = getResultExtras(true).getString("Data");
        Log.e(TAG, "从上一优先级的Receiver传递来的数据--" + data);

        //向下一优先级的Receiver传递数据
        Bundle bundle = new Bundle();
        bundle.putString("Data", "（叶应是叶）");
        setResultExtras(bundle);
    }
}
```

```java
public class OrderReceiver_3 extends BroadcastReceiver {

    private final String TAG = "OrderReceiver_3";

    public OrderReceiver_3() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG, "OrderReceiver_3被调用了");

        //取出上一优先级的Receiver传递来的数据
        String data = getResultExtras(true).getString("Data");
        Log.e(TAG, "从上一优先级的Receiver传递来的数据--" + data);
    }
}
```

在清单文件中对三个 Receiver 进行注册，指定相同的 **action** 属性值，Receiver 之间的优先级使用 **priority** 属性来判定，数值越大，优先级越高
```java
	<application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/AppTheme">
        <activity android:name=".MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

		
        <receiver android:name=".OrderReceiver_1">
            <intent-filter android:priority="100">
                <action android:name="com.example.order.receiver" />
            </intent-filter>
        </receiver>
		
        <receiver android:name=".OrderReceiver_2">
            <intent-filter android:priority="99">
                <action android:name="com.example.order.receiver" />
            </intent-filter>
        </receiver>
		
        <receiver android:name=".OrderReceiver_3">
            <intent-filter android:priority="98">
                <action android:name="com.example.order.receiver" />
            </intent-filter>
        </receiver>
		
    </application>
```

发送有序广播调用的是 **sendOrderedBroadcast(Intent , String)** 方法，String 参数值在自定义权限时使用，下边会有介绍

```java
public class MainActivity extends AppCompatActivity {

    private final String NORMAL_ACTION = "com.example.normal.receiver";

    private final String ORDER_ACTION = "com.example.order.receiver";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void sendBroadcast(View view) {
        Intent intent = new Intent(NORMAL_ACTION);
        intent.putExtra("Msg", "Hi");
        sendBroadcast(intent);
    }

    public void sendOrderBroadcast(View view) {
        Intent intent = new Intent(ORDER_ACTION);
        intent.putExtra("Msg", "Hi");
        sendOrderedBroadcast(intent, null);
    }

}
```

运行结果是

```java
02-20 22:52:30.135 6714-6714/com.example.zy.myapplication E/OrderReceiver_1: OrderReceiver_1被调用了
02-20 22:52:30.135 6714-6714/com.example.zy.myapplication E/OrderReceiver_1: OrderReceiver_1接收到的值： Hi
02-20 22:52:30.143 6714-6714/com.example.zy.myapplication E/OrderReceiver_2: OrderReceiver_2被调用了
02-20 22:52:30.143 6714-6714/com.example.zy.myapplication E/OrderReceiver_2: 从上一优先级的Receiver传递来的数据--（Hello）
02-20 22:52:30.150 6714-6714/com.example.zy.myapplication E/OrderReceiver_3: OrderReceiver_3被调用了
02-20 22:52:30.150 6714-6714/com.example.zy.myapplication E/OrderReceiver_3: 从上一优先级的Receiver传递来的数据--（叶应是叶）
```

可以看出 Receiver 接收广播时不仅因为“**priority**”属性存在先后顺序，且 Receiver 之间也能够传递数据

此外，BroadcastReceiver 也能调用 **abortBroadcast()** 方法截断广播，这样低优先级的广播接收器就无法接收到广播了

## **二、动态注册**

动态注册 BroadcastReceiver 是在代码中定义并设置好一个 **IntentFilter** 对象，然后在需要注册的地方调用 **Context.registerReceiver()** 方法，调用 **Context.unregisterReceiver()** 方法取消注册，此时就不需要在清单文件中注册 Receiver 了

这里采用在 Service 中注册广播接收器的形式，分别在**注册广播接收器**、**取消注册广播接受器**和**接收到广播**时输出Log

```java
public class BroadcastService extends Service {

    private BroadcastReceiver receiver;

    private final String TAG = "BroadcastService";

    public BroadcastService() {
    }

    @Override
    public void onCreate() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MainActivity.ACTION);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.e(TAG, "BroadcastService接收到了广播");
            }
        };
        registerReceiver(receiver, intentFilter);
        Log.e(TAG, "BroadcastService注册了接收器");
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        Log.e(TAG, "BroadcastService取消注册接收器");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
```

提供启动服务，停止服务、发送广播的方法

```java
public class MainActivity extends AppCompatActivity {

    public final static String ACTION = "com.example.receiver";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void startService(View view) {
        Intent intent = new Intent(this, BroadcastService.class);
        startService(intent);
    }

    public void sendBroadcast(View view) {
        Intent intent = new Intent(ACTION);
        sendBroadcast(intent);
    }

    public void stopService(View view) {
        Intent intent = new Intent(this, BroadcastService.class);
        stopService(intent);
    }

}

```
运行结果如下所示

```java
02-20 23:55:20.967 22784-22784/com.example.zy.myapplication E/BroadcastService: BroadcastService注册了接收器
02-20 23:55:22.811 22784-22784/com.example.zy.myapplication E/BroadcastService: BroadcastService接收到了广播
02-20 23:55:23.179 22784-22784/com.example.zy.myapplication E/BroadcastService: BroadcastService接收到了广播
02-20 23:55:23.461 22784-22784/com.example.zy.myapplication E/BroadcastService: BroadcastService接收到了广播
02-20 23:55:23.694 22784-22784/com.example.zy.myapplication E/BroadcastService: BroadcastService接收到了广播
02-20 23:55:23.960 22784-22784/com.example.zy.myapplication E/BroadcastService: BroadcastService接收到了广播
02-20 23:55:24.282 22784-22784/com.example.zy.myapplication E/BroadcastService: BroadcastService接收到了广播
02-20 23:55:24.529 22784-22784/com.example.zy.myapplication E/BroadcastService: BroadcastService接收到了广播
02-20 23:55:24.916 22784-22784/com.example.zy.myapplication E/BroadcastService: BroadcastService取消注册接收器
```

## **三、本地广播**

之前发送和接收到的广播全都是属于系统全局广播，即发出的广播可以被其他应用接收到，而且也可以接收到其他应用发送出的广播，这样可能会有不安全因素

因此，在某些情况下可以采用本地广播机制，使用这个机制发出的广播只能在应用内部进行传递，而且广播接收器也只能接收本应用内自身发出的广播

本地广播是使用 **LocalBroadcastManager** 来对广播进行管理

|                             函数                             |     作用     |
| :----------------------------------------------------------: | :----------: |
| LocalBroadcastManager.getInstance(this).registerReceiver(BroadcastReceiver, IntentFilter) | 注册Receiver |
| LocalBroadcastManager.getInstance(this).unregisterReceiver(BroadcastReceiver); | 注销Receiver |
| LocalBroadcastManager.getInstance(this).sendBroadcast(Intent) | 发送异步广播 |
| LocalBroadcastManager.getInstance(this).sendBroadcastSync(Intent) | 发送同步广播 |



首先，创建一个 BroadcastReceiver 用于接收本地广播

```java
public class LocalReceiver extends BroadcastReceiver {

    private final String TAG = "LocalReceiver";

    public LocalReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG, "接收到了本地广播");
    }
    
}
```

之后就是使用 LocalBroadcastManager 对 LocalReceiver 进行注册和解除注册了

```java
	private LocalBroadcastManager localBroadcastManager;

    private LocalReceiver localReceiver;

    private final String LOCAL_ACTION = "com.example.local.receiver";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localReceiver = new LocalReceiver();
        IntentFilter filter = new IntentFilter(LOCAL_ACTION);
        localBroadcastManager.registerReceiver(localReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(batteryReceiver);
        localBroadcastManager.unregisterReceiver(localReceiver);
    }
	
	public void sendLocalBroadcast(View view) {
        Intent intent = new Intent(LOCAL_ACTION);
        localBroadcastManager.sendBroadcast(intent);
    }

```
需要注意的是，本地广播是无法通过静态注册的方式来接收的，因为静态注册广播主要是为了在程序未启动的情况下也能接收广播，而本地广播是应用自己发送的，此时应用肯定是启动的了

## **四、使用私有权限**

使用动态注册广播接收器存在一个问题，即系统内的任何应用均可监听并触发我们的 Receiver 。通常情况下我们是不希望如此的

解决办法之一是在清单文件中为 **< receiver >** 标签添加一个 **android:exported="false"** 属性，标明该 Receiver 仅限应用内部使用。这样，系统中的其他应用就无法接触到该 Receiver 了

此外，也可以选择创建自己的使用权限，即在清单文件中添加一个 **< permission >** 标签来声明自定义权限

```java
    <permission
        android:name="com.example.permission.receiver"
        android:protectionLevel="signature" />
```

自定义权限时必须同时指定 **protectionLevel** 属性值，系统根据该属性值确定自定义权限的使用方式

|      属性值       |                           限定方式                           |
| :---------------: | :----------------------------------------------------------: |
|      normal       | 默认值。较低风险的权限，对其他应用，系统和用户来说风险最小。系统在安装应用时会自动批准授予应用该类型的权限，不要求用户明确批准（虽然用户在安装之前总是可以选择查看这些权限） |
|     dangerous     | 较高风险的权限，请求该类型权限的应用程序会访问用户私有数据或对设备进行控制，从而可能对用户造成负面影响。因为这种类型的许可引入了潜在风险，所以系统可能不会自动将其授予请求的应用。例如，系统可以向用户显示由应用请求的任何危险许可，并且在继续之前需要确认，或者可以采取一些其他方法来避免用户自动允许 |
|     signature     | 只有在请求该权限的应用与声明权限的应用使用相同的证书签名时，系统才会授予权限。如果证书匹配，系统会自动授予权限而不通知用户或要求用户的明确批准 |
| signatureOrSystem | 系统仅授予Android系统映像中与声明权限的应用使用相同的证书签名的应用。请避免使用此选项，“signature”级别足以满足大多数需求，“signatureOrSystem”权限用于某些特殊情况 |

首先，新建一个新的工程，在它的清单文件中创建一个自定义权限，并声明该权限。**protectionLevel** 属性值设为“**signature**”

```java
    <permission
        android:name="com.example.permission.receiver"
        android:protectionLevel="signature" />

    <uses-permission android:name="com.example.permission.receiver" />
```
然后，发送含有该权限声明的 Broadcast 。这样，只有使用相同证书签名且声明该权限的应用才能接收到该 Broadcast 了

```java
    private final String PERMISSION_PRIVATE = "com.example.permission.receiver";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void sendPermissionBroadcast(View view) {
        sendBroadcast(new Intent("Hi"), PERMISSION_PRIVATE);
    }
```

回到之前的工程
首先在清单文件中声明权限

```java
<uses-permission android:name="com.example.permission.receiver" />
```
创建一个 BroadcastReceiver

```java
public class PermissionReceiver extends BroadcastReceiver {

    private final String TAG = "PermissionReceiver";

    public PermissionReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG, "接收到了私有权限广播");
    }
}
```
然后注册广播接收器。因为 Android Studio 在调试的时候会使用相同的证书为每个应用签名，所以，在之前新安装的App发送出广播后，PermissionReceiver 就会输出 Log 日志

```java
	private final String PERMISSION_PRIVATE = "com.example.permission.receiver";

    private PermissionReceiver permissionReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        IntentFilter intentFilter1 = new IntentFilter("Hi");
        permissionReceiver = new PermissionReceiver();
        registerReceiver(permissionReceiver, intentFilter1, PERMISSION_PRIVATE, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(permissionReceiver);
    }
```

## **五、实战演练**

### **5.1、监听网络状态变化**

首先需要一个用来监测当前网络状态的工具类

```java
/**
 * Created by 叶应是叶 on 2017/2/21.
 */

public class NetworkUtils {

    /**
     * 标记当前网络状态，分别是：移动数据、Wifi、未连接、网络状态已公布
     */
    public enum State {
        MOBILE, WIFI, UN_CONNECTED, PUBLISHED
    }

    /**
     * 为了避免因多次接收到广播反复提醒的情况而设置的标志位，用于缓存收到新的广播前的网络状态
     */
    private static State tempState;

    /**
     * 获取当前网络连接状态
     *
     * @param context Context
     * @return 网络状态
     */
    public static State getConnectState(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        State state = State.UN_CONNECTED;
        if (networkInfo != null && networkInfo.isAvailable()) {
            if (isMobileConnected(context)) {
                state = State.MOBILE;
            } else if (isWifiConnected(context)) {
                state = State.WIFI;
            }
        }
        if (state.equals(tempState)) {
            return State.PUBLISHED;
        }
        tempState = state;
        return state;
    }

    private static boolean isMobileConnected(Context context) {
        return isConnected(context, ConnectivityManager.TYPE_MOBILE);
    }

    private static boolean isWifiConnected(Context context) {
        return isConnected(context, ConnectivityManager.TYPE_WIFI);
    }

    private static boolean isConnected(Context context, int type) {
        //getAllNetworkInfo() 在 API 23 中被弃用
        //getAllNetworks() 在 API 21 中才添加
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            NetworkInfo[] allNetworkInfo = manager.getAllNetworkInfo();
            for (NetworkInfo info : allNetworkInfo) {
                if (info.getType() == type) {
                    return info.isAvailable();
                }
            }
        } else {
            Network[] networks = manager.getAllNetworks();
            for (Network network : networks) {
                NetworkInfo networkInfo = manager.getNetworkInfo(network);
                if (networkInfo.getType() == type) {
                    return networkInfo.isAvailable();
                }
            }
        }
        return false;
    }

}
```

然后声明一个 BroadcastReceiver ，在onReceive() 方法中用Log输出当前网络状态

```java
public class NetworkReceiver extends BroadcastReceiver {

    private static final String TAG = "NetworkReceiver";

    public NetworkReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (NetworkUtils.getConnectState(context)) {
            case MOBILE:
                Log.e(TAG, "当前连接了移动数据");
                break;
            case WIFI:
                Log.e(TAG, "当前连接了Wifi");
                break;
            case UN_CONNECTED:
                Log.e(TAG, "当前没有网络连接");
                break;
        }
    }

}
```
在清单文件中注册广播接收器，“**android.net.conn.CONNECTIVITY_CHANGE**”是系统预定义好的 **action** 值，只要系统网络状态发生变化，NetworkReceiver 就能收到广播

```javascript
        <receiver android:name=".NetworkReceiver">
            <intent-filter>
                <action android:name="android.net.conn.CONNECTIVITY_CHANGE" />
            </intent-filter>
        </receiver>
```
此外，还要申请查看网络状态的权限

```java
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### **5.2、监听电量变化**

因为系统规定监听电量变化的广播接收器不能静态注册，所以这里只能使用动态注册的方式了

```java
private final String TAG = "MainActivity";

    private BroadcastReceiver batteryReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // 当前电量
                int currentBattery = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                // 总电量
                int totalBattery = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0);         
                Log.e(TAG, "当前电量:" + currentBattery + "-总电量：" + totalBattery);
            }
        };
        registerReceiver(batteryReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(batteryReceiver);
    }
```
在 **onReceive(Context , Intent )** 中的 Intent 值包含了一些额外信息，可以取出当前电量和总电量

为了方便查看电量变化，可以在模拟器的“extended controls”面板中主动地改变模拟器的电量，查看Log输出

![](http://upload-images.jianshu.io/upload_images/2552605-dbe03d95cc3fb423?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

### **5.3、应用安装更新卸载监听**

首先，创建 BroadcastReceiver 

```java
public class AppReceiver extends BroadcastReceiver {

    private final String TAG = "AppReceiver";

    public AppReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        //判断广播类型
        String action = intent.getAction();
        //获取包名
        Uri appName = intent.getData();
        if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
            Log.e(TAG, "安装了：" + appName);
        } else if (Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
            Log.e(TAG, "更新了：" + appName);
        } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
            Log.e(TAG, "卸载了：" + appName);
        }
    }
}

```

注册广播接收器

```java
<receiver android:name=".train.AppReceiver">
            <intent-filter>
                <!--安装应用-->
                <action android:name="android.intent.action.PACKAGE_ADDED" />
                <!--更新应用-->
                <action android:name="android.intent.action.PACKAGE_REPLACED" />
                <!--卸载应用-->
                <action android:name="android.intent.action.PACKAGE_REMOVED" />
                <!--携带包名-->
                <data android:scheme="package" />
            </intent-filter>
        </receiver>
```
