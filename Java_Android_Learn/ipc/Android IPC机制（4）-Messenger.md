### 一、概述
除了使用 AIDL 进行 IPC 外，我们还可以使用 Messenger 来替代 AIDL。通过在 Message 对象中放入需要传递的对象，利用 Messenger 在不同进程间传递 Message 对象，就可以方便地进行进程间通信了
Messenger 是一种轻量级的 IPC 方案，底层实现依然是 AIDL，通过 Messenger 的两个构造方法就可以看出来

```java
    public Messenger(Handler target) {
        mTarget = target.getIMessenger();
    }
 
    public Messenger(IBinder target) {
        mTarget = IMessenger.Stub.asInterface(target);
    }
```

Messenger 对 AIDL 进行了封装，使开发者可以更简单地进行进程间通信。此外，由于 Messenger 一次只处理一个请求，不会出现并发执行的问题，因此在服务端不用考虑进行线程同步

这里通过 Messenger 来实现一个简单的进程间通信，客户端发送一个整数给服务端，服务端再把这个数值打印出来

### 二、服务端
与 AIDL 一样，服务端也要创建一个 Service 来处理客户端的连接请求，但服务端的代码要简单得多
首先，通过一个 Handler 对象来创建 Messenger 对象，然后在 onBind 方法中返回 Messenger 对象底层的 Binder 即可

```java
/**
 * 作者：叶应是叶
 * 时间：2018/3/22 20:13
 * 描述：https://github.com/leavesC
 */
public class MessengerService extends Service {

    private static final String TAG = "MessengerService";

    private static final int CODE_MESSAGE = 1;

    private static class MessengerHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CODE_MESSAGE: {
                    Log.e(TAG, "服务端收到了消息：" + msg.arg1);
                    break;
                }
            }
        }
    }

    private Messenger messenger = new Messenger(new MessengerHandler());

    public MessengerService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

}

```
### 三、客户端
客户端首先要绑定服务端的 **Service**，绑定成功后通过 **ServiceConnection** 对象的 **onServiceConnected** 方法的参数 **IBinder** 来构造一个 **Messenger** 对象，之后通过 Messenger 对象即可向服务端发送消息了

```java
/**
 * 作者：叶应是叶
 * 时间：2018/3/22 20:13
 * 描述：https://github.com/leavesC
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int CODE_MESSAGE = 1;

    private Messenger messenger;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            messenger = new Messenger(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            messenger = null;
        }
    };

    private EditText et_message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindService();
        initView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
    }

    private void bindService() {
        Intent intent = new Intent();
        intent.setClassName("com.czy.messenger_server", "com.czy.messenger_server.MessengerService");
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void initView() {
        et_message = findViewById(R.id.et_message);
        Button btn_sendMessage = findViewById(R.id.btn_sendMessage);
        btn_sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (messenger == null) {
                    return;
                }
                String content = et_message.getText().toString();
                if (TextUtils.isEmpty(content)) {
                    return;
                }
                int arg1 = Integer.valueOf(content);
                Message message = new Message();
                message.what = CODE_MESSAGE;
                message.arg1 = arg1;
                try {
                    messenger.send(message);
                    Log.e(TAG, "消息发送成功");
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
```
运行结果如下所示
![](https://upload-images.jianshu.io/upload_images/2552605-42a91fb87bdc7982.gif?imageMogr2/auto-orient/strip)

在上面的示例代码中我是用 Message 来承载需要发送的消息的，因为 Messenger 和 Message 都实现了 Parcelable 接口，所以可以跨进程传输。Message 中能用来承载数据的载体有 what、arg1、arg2、obj、Bundle、replyTo。当中，obj 字段在跨进程通信中只能用来承载系统提供的实现了 Parcelable 接口的对象，例如 Bundle 和 Intent。如果承载了非法数据（例如 String），则会发生运行时异常

![](https://upload-images.jianshu.io/upload_images/2552605-36bd85418eb37b2d.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

### 四、双向通信
以上的例子只是实现了单向通信，还要考虑下如何实现双向通信，即服务端如何向客户端反馈数据？这就需要客户端也需要通过 Handler 创建一个 Messenger 对象，并将该 Messenger 对象通过 Message 的 replyTo 参数传递给服务端，服务端取得该参数就可以回应客户端了

这里就直接修改上述 IPC 流程，将客户端发给服务端的 arg1 参数乘以 2 后再返回给客户端

首先修改服务端代码

```java
private static class MessengerHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CODE_MESSAGE: {
                    Log.e(TAG, "服务端收到了消息：" + msg.arg1 + " " + ((Intent) msg.obj).getAction());
                    //取得客户端的 Messenger 对象
                    Messenger messenger = msg.replyTo;
                    Message message = new Message();
                    message.what = CODE_MESSAGE;
                    message.arg1 = 2 * msg.arg1;
                    try {
                        messenger.send(message);
                        Log.e(TAG, "服务端回复消息成功");
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
    }
```
为了接收服务端的回复，客户端也需要通过 Handler 创建一个 Messenger 对象，并将该 Messenger 对象通过 Message 的 replyTo 参数传递给服务端

```java
	private Messenger replyMessenger;

    private static class MessengerHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CODE_MESSAGE: {
                    Log.e(TAG, "客户端收到了服务端回复的消息：" + msg.arg1);
                    break;
                }
            }
        }
    }
```

```java
		btn_sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (messenger == null) {
                    return;
                }
                String content = et_message.getText().toString();
                if (TextUtils.isEmpty(content)) {
                    return;
                }
                int arg1 = Integer.valueOf(content);
                Intent intent = new Intent("Action");
                Message message = new Message();
                message.what = CODE_MESSAGE;
                message.arg1 = arg1;
                message.obj = intent;
                //双向通信时需要加上这一句
                message.replyTo = replyMessenger;
                try {
                    messenger.send(message);
                    Log.e(TAG, "消息发送成功");
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
```
运行结果如下所示
![](https://upload-images.jianshu.io/upload_images/2552605-82be44a7624b0e7d.gif?imageMogr2/auto-orient/strip)

从以上介绍可以看出来，Messenger 的使用要比 AIDL 简单得多，因为 Messenger 对 AIDL 进行了封装，使之更加容易使用。但需要注意的是，Messenger 是以串行的方式处理客户端发送的消息，即使有大量的消息同时到达服务端，服务端也只能一个个处理，所以 Messenger 不适合用于处理大量的并发请求，此时就还是需要考虑使用 AIDL 了，因为 AIDL 支持并发通信

**这里提供本系列所有的 IPC 示例代码：[IPC_Demo](https://github.com/leavesC/IPC_Demo)**
