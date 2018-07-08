### 一、概述
AIDL 意思即 Android Interface Definition Language，翻译过来就是 Android 接口定义语言，是用于定义服务端和客户端通信接口的一种描述语言，可以拿来生成用于 IPC 的代码。从某种意义上说 AIDL 其实是一个模板，因为在使用过程中，实际起作用的并不是 AIDL 文件，而是据此而生成的一个 IInterface 的实例代码，AIDL 其实是为了避免我们重复编写代码而出现的一个模板

设计 AIDL 这门语言的目的就是为了实现进程间通信。在 Android 系统中，每个进程都运行在一块独立的内存中，在其中完成自己的各项活动，与其他进程都分隔开来。可是有时候我们又有应用间进行互动的需求，比较传递数据或者任务委托等，AIDL 就是为了满足这种需求而诞生的。通过 AIDL，可以在一个进程中获取另一个进程的数据和调用其暴露出来的方法，从而满足进程间通信的需求

通常，暴露方法给其他应用进行调用的应用称为服务端，调用其他应用的方法的应用称为客户端，客户端通过绑定服务端的Service来进行交互

**更详细的内容我在以前的另一篇专门介绍 AIDL 的文章内讲过，所以比较基础的内容这里不再赘述，可以看这里**：[**Android AIDL使用详解**](https://www.jianshu.com/p/29999c1a93cd)

现在，我来模拟一种 IPC 的流程
服务端**（com.czy.aidl_server）**向外提供了进行数学计算的能力（其实就是对两个整数进行相乘）。客户端**（com.czy.aidl_client）**需要进行计算时就将数据（包含了一个整数值的序列化类）传递给服务端进行运算，运算结果会返回给客户端。注意，服务端和客户端是两个不同的应用，因此自然也是处于不同的进程中，以此来进行 IPC 

### 二、服务端
服务端是提供运算操作能力的一方，所以除了需要设定运算参数的格式外，还需要提供运算方法
此处，用 Parameter 类作为运算参数

```java
/**
 * 作者：叶应是叶
 * 时间：2018/3/18 17:29
 * 描述：包含一个进行运算操作的 int 类型数据
 */
public class Parameter implements Parcelable {

    private int param;

    public Parameter(int param) {
        this.param = param;
    }

    public int getParam() {
        return param;
    }

    public void setParam(int param) {
        this.param = param;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.param);
    }

    protected Parameter(Parcel in) {
        this.param = in.readInt();
    }

    public static final Parcelable.Creator<Parameter> CREATOR = new Parcelable.Creator<Parameter>() {
        @Override
        public Parameter createFromParcel(Parcel source) {
            return new Parameter(source);
        }

        @Override
        public Parameter[] newArray(int size) {
            return new Parameter[size];
        }
    };

}
```
相对应的 AIDL 文件

```java
package com.czy.aidl_server;

parcelable Parameter;
```
此外，还需要一个向外暴露运算方法的 AIDL 接口

```java
package com.czy.aidl_server;

import com.czy.aidl_server.Parameter;

interface IOperationManager {

   //接收两个参数，并将运算结果返回给客户端
   Parameter operation(in Parameter parameter1 , in Parameter parameter2);

}
```
然后，在 Service 中进行实际的运算操作，并将运算结果返回
```java
/**
 * 作者：叶应是叶
 * 时间：2018/3/18 17:35
 * 描述：https://github.com/leavesC
 */
public class AIDLService extends Service {

    private static final String TAG = "AIDLService";

    private IOperationManager.Stub stub = new IOperationManager.Stub() {
        @Override
        public Parameter operation(Parameter parameter1, Parameter parameter2) throws RemoteException {
            Log.e(TAG, "operation 被调用");
            int param1 = parameter1.getParam();
            int param2 = parameter2.getParam();
            return new Parameter(param1 * param2);
        }
    };

    public AIDLService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return stub;
    }

}
```
这样，服务端的接口就设计好了，文件目录如下所示
![](https://upload-images.jianshu.io/upload_images/2552605-fbdb5f6bac3c8ce1.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

### 三、客户端
将服务端的两个 AILD 文件以及 Parameter 类复制到客户端，保持文件路径（包名）不变

文件目录如下所示
![](https://upload-images.jianshu.io/upload_images/2552605-dca6492ffb337b48.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

指定服务端的包名和 Service 路径，绑定服务，向其传递两个待运算参数并将运算结果展示出来

```java
/**
 * 作者：叶应是叶
 * 时间：2018/3/18 17:51
 * 描述：https://github.com/leavesC
 * 客户端
 */
public class MainActivity extends AppCompatActivity {

    private EditText et_param1;

    private EditText et_param2;

    private EditText et_result;

    private IOperationManager iOperationManager;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            iOperationManager = IOperationManager.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            iOperationManager = null;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        bindService();
    }

    private void bindService() {
        Intent intent = new Intent();
        intent.setClassName("com.czy.aidl_server", "com.czy.aidl_server.AIDLService");
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void initView() {
        et_param1 = findViewById(R.id.et_param1);
        et_param2 = findViewById(R.id.et_param2);
        et_result = findViewById(R.id.et_result);
        Button btn_operation = findViewById(R.id.btn_operation);
        btn_operation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(et_param1.getText()) || TextUtils.isEmpty(et_param2.getText())) {
                    return;
                }
                int param1 = Integer.valueOf(et_param1.getText().toString());
                int param2 = Integer.valueOf(et_param2.getText().toString());
                Parameter parameter1 = new Parameter(param1);
                Parameter parameter2 = new Parameter(param2);
                if (iOperationManager != null) {
                    try {
                        Parameter resultParameter = iOperationManager.operation(parameter1, parameter2);
                        et_result.setText("运算结果： " + resultParameter.getParam());
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceConnection != null) {
            unbindService(serviceConnection);
        }
    }

}
```
运行结果如下所示
![](https://upload-images.jianshu.io/upload_images/2552605-d8e0b4e299aea2b1.gif?imageMogr2/auto-orient/strip)

可以看到，得到了正确的运算结果了，这就完成了一次简单的 IPC ：客户端将参数传递给了服务端，服务端接收参数并进行计算，并将计算结果返回给客户端

### 四、注册回调函数
在上一节的例子里的运算操作只是将参数进行乘法操作，当然能够很快获得返回值，但如果是要进行耗时操作，那这种方式就不太合适了，所以可以以注册回调函数的方式来获取运算结果。即客户端向服务端注册一个回调函数用于接收运算结果，而不用傻乎乎地一直等待返回值

因此，首先需要先声明一个 AIDL 接口 **`IOnOperationCompletedListener`**，用于传递运算结果

```java
package com.czy.aidl_server;

import com.czy.aidl_server.Parameter;

interface IOnOperationCompletedListener {

    void onOperationCompleted(in Parameter result);

}
```
将 **`IOperationManager`** 的**`operation`**  方法改为无返回值，新增注册回调函数和解除注册函数的方法

```java
package com.czy.aidl_server;

import com.czy.aidl_server.Parameter;
import com.czy.aidl_server.IOnOperationCompletedListener;

interface IOperationManager {

   void operation(in Parameter parameter1 , in Parameter parameter2);

   void registerListener(in IOnOperationCompletedListener listener);

   void unregisterListener(in IOnOperationCompletedListener listener);

}
```
在 **`operation`** 方法中让线程休眠五秒，模拟耗时操作，然后再将运算结果传递出去
```java
/**
 * 作者：叶应是叶
 * 时间：2018/3/18 17:35
 * 描述：https://github.com/leavesC
 */
public class AIDLService extends Service {

    private static final String TAG = "AIDLService";

    private CopyOnWriteArrayList<IOnOperationCompletedListener> copyOnWriteArrayList;

    private IOperationManager.Stub stub = new IOperationManager.Stub() {
        @Override
        public void operation(Parameter parameter1, Parameter parameter2) throws RemoteException {
            try {
                Log.e(TAG, "operation 被调用，延时5秒，模拟耗时计算");
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int param1 = parameter1.getParam();
            int param2 = parameter2.getParam();
            Parameter result = new Parameter(param1 * param2);
            for (IOnOperationCompletedListener listener : copyOnWriteArrayList) {
                listener.onOperationCompleted(result);
            }
            Log.e(TAG, "计算结束");
        }

        @Override
        public void registerListener(IOnOperationCompletedListener listener) throws RemoteException {
            Log.e(TAG, "registerListener");
            if (!copyOnWriteArrayList.contains(listener)) {
                Log.e(TAG, "注册回调成功");
                copyOnWriteArrayList.add(listener);
            } else {
                Log.e(TAG, "回调之前已注册");
            }
        }

        @Override
        public void unregisterListener(IOnOperationCompletedListener listener) throws RemoteException {
            Log.e(TAG, "unregisterListener");
            if (copyOnWriteArrayList.contains(listener)) {
                copyOnWriteArrayList.remove(listener);
                Log.e(TAG, "解除注册回调成功");
            } else {
                Log.e(TAG, "该回调没有被注册过");
            }
        }
    };

    public AIDLService() {
        copyOnWriteArrayList = new CopyOnWriteArrayList<>();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return stub;
    }

}
```
客户端这边一样要修改相应的 AIDL 文件
新增两个按钮用于注册和解除注册回调函数，并在回调函数中展示运算结果
```java
/**
 * 作者：叶应是叶
 * 时间：2018/3/18 17:51
 * 描述：https://github.com/leavesC
 * 客户端
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private EditText et_param1;

    private EditText et_param2;

    private EditText et_result;

    private IOperationManager iOperationManager;

    private IOnOperationCompletedListener completedListener = new IOnOperationCompletedListener.Stub() {
        @Override
        public void onOperationCompleted(Parameter result) throws RemoteException {
            et_result.setText("运算结果： " + result.getParam());
        }
    };

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            iOperationManager = IOperationManager.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            iOperationManager = null;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        bindService();
    }

    private void bindService() {
        Intent intent = new Intent();
        intent.setClassName("com.czy.aidl_server", "com.czy.aidl_server.AIDLService");
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void initView() {
        et_param1 = findViewById(R.id.et_param1);
        et_param2 = findViewById(R.id.et_param2);
        et_result = findViewById(R.id.et_result);
        Button btn_registerListener = findViewById(R.id.btn_registerListener);
        Button btn_unregisterListener = findViewById(R.id.btn_unregisterListener);
        Button btn_operation = findViewById(R.id.btn_operation);
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.btn_registerListener: {
                        if (iOperationManager != null) {
                            try {
                                iOperationManager.registerListener(completedListener);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    }
                    case R.id.btn_unregisterListener: {
                        if (iOperationManager != null) {
                            try {
                                iOperationManager.unregisterListener(completedListener);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    }
                    case R.id.btn_operation: {
                        if (TextUtils.isEmpty(et_param1.getText()) || TextUtils.isEmpty(et_param2.getText())) {
                            return;
                        }
                        int param1 = Integer.valueOf(et_param1.getText().toString());
                        int param2 = Integer.valueOf(et_param2.getText().toString());
                        Parameter parameter1 = new Parameter(param1);
                        Parameter parameter2 = new Parameter(param2);
                        if (iOperationManager != null) {
                            try {
                                iOperationManager.operation(parameter1, parameter2);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    }
                }
            }
        };
        btn_registerListener.setOnClickListener(clickListener);
        btn_unregisterListener.setOnClickListener(clickListener);
        btn_operation.setOnClickListener(clickListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceConnection != null) {
            unbindService(serviceConnection);
        }
    }

}
```
运行结果如下所示：
![](https://upload-images.jianshu.io/upload_images/2552605-0bb779351f8f0f24.gif?imageMogr2/auto-orient/strip)

### 五、正确使用 AIDL 回调接口
在上面的代码中我提供了一个按钮用于解除回调函数，但当点击按钮时，Logcat 却会打印出如下信息

![](https://upload-images.jianshu.io/upload_images/2552605-248798a67c906e02.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

该回调没有被注册过？但在注册回调函数和解除回调函数时，使用的都是同个对象啊！其实，这是因为回调函数被序列化了的原因，Binder 会把客户端传过来的对象序列化后转为一个新的对象传给服务端，即使客户端使用的一直是同个对象，但对服务端来说前后两个回调函数其实都是两个完全不相关的对象，对象的跨进程传输本质上都是序列化与反序列化的过程

为了能够无误地注册和解除注册回调函数，系统为开发者提供了 `RemoteCallbackList`，RemoteCallbackList 是一个泛型类，系统专门提供用于删除跨进程回调函数，支持管理任意的 AIDL 接口，因为所有的 AIDL 接口都继承自 `IInterface`，而  RemoteCallbackList 对于泛型类型有限制

```java
	public class RemoteCallbackList<E extends IInterface>
```
RemoteCallbackList 在内部有一个 ArrayMap 用于 保存所有的 AIDL 回调接口

```java
	ArrayMap<IBinder, Callback> mCallbacks  = new ArrayMap<IBinder, Callback>();
```
其中 Callback 封装了真正的远程回调函数，因为即使回调函数经过序列化和反序列化后会生成不同的对象，但这些对象的底层 Binder 对象是同一个。利用这个特征就可以通过遍历 RemoteCallbackList 的方式删除注册的回调函数了
此外，当客户端进程终止后，RemoteCallbackList 会自动移除客户端所注册的回调接口。而且 RemoteCallbackList 内部自动实现了线程同步的功能，所以我们使用它来注册和解注册时，不需要进行线程同步

以下就来修改代码，改为用 RemoteCallbackList 来存储 AIDL 接口

```java
    //声明
    private RemoteCallbackList<IOnOperationCompletedListener> callbackList;
```
注册接口和解除注册接口
```java
		@Override
        public void registerListener(IOnOperationCompletedListener listener) throws RemoteException {
            callbackList.register(listener);
            Log.e(TAG, "registerListener 注册回调成功");
        }

        @Override
        public void unregisterListener(IOnOperationCompletedListener listener) throws RemoteException {
            callbackList.unregister(listener);
            Log.e(TAG, "unregisterListener 解除注册回调成功");
        }
```
遍历回调接口
```java
			//在操作 RemoteCallbackList 前，必须先调用其 beginBroadcast 方法
            //此外，beginBroadcast 必须和 finishBroadcast配套使用
            int count = callbackList.beginBroadcast();
            for (int i = 0; i < count; i++) {
                IOnOperationCompletedListener listener = callbackList.getBroadcastItem(i);
                if (listener != null) {
                    listener.onOperationCompleted(result);
                }
            }
            callbackList.finishBroadcast();
```
按照上面的代码来修改后，客户端就可以正确地解除所注册的回调函数了

还有一个地方需要强调下，是关于远程方法调用时的线程问题。客户端在调用远程服务的方法时，被调用的方法是运行在服务端的 Binder 线程池中，同时客户端线程会被挂起，这时如果服务端方法执行比较耗时，就会导致客户端线程被堵塞。就如果上一节我为了模拟耗时计算，使线程休眠了五秒，当点击按钮时就可以明显看到按钮有一种被“卡住了”的反馈效果，这就是因为 UI 线程被堵塞了，这可能会导致 ANR。所以如果确定远程方法是耗时的，就要避免在 UI 线程中去调用远程方法。
所以，客户端调用远程方法 `operation` 的操作可以放到子线程中进行

```java
    new Thread(new Runnable() {
        @Override
        public void run() {
            Parameter parameter1 = new Parameter(param1);
            Parameter parameter2 = new Parameter(param2);
            if (iOperationManager != null) {
                try {
                    iOperationManager.operation(parameter1, parameter2);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                }
            }
    }).start();
```

此外，客户端的 `ServiceConnection`对象的 `onServiceConnected` 和 `onServiceDisconnected`都是运行在 UI 线程中，所以也不能用于调用耗时的远程方法。而由于服务端的方法本身就运行在服务端的 Binder 线程池中，所以服务端方法本身就可以用于执行耗时方法，不必再在服务端方法中开线程去执行异步任务

同理，当服务端需要调用客户端的回调接口中的方法时，被调用的方法也运行在客户端的 Binder 线程池中，所以一样不可以在服务端中调用客户端的耗时方法

最后，我们还需要考虑一个问题，那就是安全问题。假设有人反编译了服务端应用的代码，取得了 AIDL 接口，知道了应用的包名以及 Service 路径名后，就可以直接通过 AIDL 直接调用服务端的远程方法了，这当然不是应用开发者所希望面对的，因此服务端就需要对请求连接的客户端进行权限验证了

Android 平台下的权限验证机制我在以前的文章中有介绍过，这里不再赘述，可以参考这两篇文章的内容：

**[Android 系统权限](https://www.jianshu.com/p/73e5667f114e)**

**[Android BroadcastReceiver使用详解](https://www.jianshu.com/p/f348f6d7fe59)**

**这里提供本系列所有的 IPC 示例代码：[IPC_Demo](https://github.com/leavesC/IPC_Demo)**
