### 一、Binder连接池
之前几篇文章我已经介绍了两种 IPC 方案：AIDL 和 Messenger。当中，AIDL 也是 Messenger 的底层实现，所以对于 AIDL 开发者需要更为重视一点，这一篇文章也将继续对 AIDL 进行更深入的介绍

现在考虑一种情况，假设在一个设备上，有一个应用作为服务端存在，作为客户端的应用有十个，而这十个应用都需要与服务端进行通信，且用于与服务端进行通信的 AIDL 接口各不相同。那么，按照之前介绍的 AIDL 方案，现在服务端就需要创建十个 Service 来分别与十个客户端对应。这种结果起来是不可以接受的，因为 Service 作为四大组件之一，创建并运行太多 Service 会使服务端应用看起来太为重量级了 ，也不利于服务端应用的开发

为了解决这个问题，可以考虑使用 Binder 连接池来管理所有的 AIDL。机制是这样的，服务端只创建一个 Service，每个客户端在请求连接时，带上自己的唯一标识，服务端根据这个唯一标识，返回对应的 Binder 给客户端。这样，不同的客户端就可以都绑定到同一个 Service，而获得的 Binder 对象是唯一的，避免了创建多个 Service 的情况

这里就来模拟这种多个客户端的 IPC 过程，整个流程是这样的：有两个客户端，一个客户端传递两个整数给服务端进行加法操作，另一个客户端传递两个整数给服务端进行减法操作，所以总的是会有三个不同的应用

![](https://upload-images.jianshu.io/upload_images/2552605-64649232bfe38b2c.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

### 二、服务端
首先创建三个需要的 AIDL 接口，**IOperation.aidl** 用于提供加法操作，**ICompute.aidl** 用于提供减法操作，**IBinderPool.aidl** 是一个用于中转的 **Binder** 对象，含有一个 **queryBinder** 方法用于接收一个唯一标识，并返回客户端实际需要的 **Binder** 对象

```java
package com.czy.binder_pool_server;

interface IOperation {

    int add(int parameter1 , int parameter2);

}
```

```java
package com.czy.binder_pool_server;

interface ICompute {

    int subtraction(int parameter1 , int parameter2);

}
```

```java
package com.czy.binder_pool_server;

interface IBinderPool {

    IBinder queryBinder(int binderCode);
    
}
```
此外，服务端还需要有 **ICompute.Stub** 和 **IOperation.Stub** 的具体实现，因为需要创建这两个类的子类

```java
/**
 * 作者：叶应是叶
 * 时间：2018/3/23 21:17
 * 描述：https://github.com/leavesC
 */
public class IOperationImpl extends IOperation.Stub {

    @Override
    public int add(int parameter1, int parameter2) throws RemoteException {
        return parameter1 + parameter2;
    }

}
```

```java
/**
 * 作者：叶应是叶
 * 时间：2018/3/23 21:13
 * 描述：https://github.com/leavesC
 */
public class IComputeImpl extends ICompute.Stub {

    @Override
    public int subtraction(int parameter1, int parameter2) throws RemoteException {
        return parameter1 - parameter2;
    }

}
```
之后就是来创建那唯一的一个 Service 了。Service 直接返回的是 **BinderPoolImpl** 对象，而 **BinderPoolImpl** 对象的 **queryBinder** 可以根据传入的参数再返回对应的 Binder 对象，即进行中转转发，从而使客户端得到真实想要的 Binder 对象

```java
/**
 * 作者：叶应是叶
 * 时间：2018/3/23 21:55
 * 描述：https://github.com/leavesC
 */
public class BinderPoolService extends Service {

    private class BinderPoolImpl extends IBinderPool.Stub {

        @Override
        public IBinder queryBinder(int binderId) {
            switch (binderId) {
                case 100: {
                    return new IOperationImpl();
                }
                case 200: {
                    return new IComputeImpl();
                }
            }
            return null;
        }

    }

    private Binder binderPool;

    public BinderPoolService() {
        binderPool = new BinderPoolImpl();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binderPool;
    }

}
```
### 三、客户端
本客户端请求服务端进行加法操作，因此需要把 **IOperation.aidl** 文件和 **IBinderPool.aidl** 文件拷贝过来
与服务端的绑定操作与之前的文章介绍的 AIDL 机制大致相同，区别只在于在 **onServiceConnected** 中需要调用 **queryBinder** 方法获取真实的 Binder 对象

```java
/**
 * 作者：叶应是叶
 * 时间：2018/3/23 22:32
 * 描述：https://github.com/leavesC
 */
public class MainActivity extends AppCompatActivity {

    private IOperation operation;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                IBinderPool binderPool = IBinderPool.Stub.asInterface(service);
                //本客户端的唯一标识是 100
                //获取真实的 Binder 对象
                operation = IOperation.Stub.asInterface(binderPool.queryBinder(100));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            operation = null;
            bindService();
        }
    };

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindService();
        findViewById(R.id.btn_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (operation != null) {
                    try {
                        Log.e(TAG, "4+2 加法：" + operation.add(4, 2));
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
        if (operation != null) {
            unbindService(serviceConnection);
        }
    }

    private void bindService() {
        Intent intent = new Intent();
        intent.setClassName("com.czy.binder_pool_server", "com.czy.binder_pool_server.BinderPoolService");
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

}
```
另外一个客户端的操作也类似，这里不再赘述，具体的代码可以直接看我传到 GitHub 上的示例代码

运行结果如下所示
![](https://upload-images.jianshu.io/upload_images/2552605-a4d025b183014ae1.gif?imageMogr2/auto-orient/strip)

这样，以后每增加一个客户端，就可以再为其指定一个唯一标识，然后在服务端中返回对应的 Binder 对象即可，从而避免了创建多个 Service 的情况，极大的提高了开发效率

本系列关于 Android 平台下的 IPC 机制的文章到这里目前也就结束，之后如果还有其它值得介绍的内容的话，我也会继续写下一篇

**这里提供本系列所有的 IPC 示例代码：[IPC_Demo](https://github.com/leavesC/IPC_Demo)**
