最近的项目需要实现一个 Android 手机之间无网络传输文件的功能，就研究了下 Wifi P2P（Wifi点对点) 这么一个功能，最后也实现了通过 Wifi 隔空传输文件 的功能，这里我也来整理下代码，分享给大家

Wifi P2P 是在 Android 4.0 以及更高版本系统中加入的功能，通过 Wifi P2P 可以在不连接网络的情况下，直接与配对的设备进行数据交换。相对于蓝牙，Wifi P2P 的搜索速度和传输速度更快，传输距离更远

项目主页：[Android 实现无网络传输文件](https://github.com/leavesCZY/WifiP2P) ，欢迎 star

实现的效果如下所示：

<img src="https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/ad0ec2fb76034d5b81bc8e6c212f4ea3~tplv-k3u1fbpfcp-watermark.image" style="zoom: 33%;" />

<img src="https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/13f7162a146d408783d865b210fb4c77~tplv-k3u1fbpfcp-watermark.image" style="zoom:33%;" />

开发步骤分为以下几点：

1. 在 AndroidManifest 中声明相关权限（网络和文件读写权限）
2. 获取 WifiP2pManager ，注册相关广播监听Wifi直连的状态变化
3. 指定某一台设备为服务器（用来接收文件），创建群组并作为群主存在，在指定端口监听客户端的连接请求，等待客户端发起连接请求以及文件传输请求
4. 客户端（用来发送文件）主动搜索附近的设备，加入到服务器创建的群组，获取服务器的 IP 地址，向其发起文件传输请求
5. 校验文件完整性

# 一、声明权限

Wifi P2P 技术并不会访问网络，但由于会使用到 Java Socket，所以需要申请网络权限。此外，由于是要实现文件互传，所以也需要申请SD卡读写权限。

```xml
    <uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

# 二、注册广播

与 Wifi P2P 相关的广播有以下几个：

1. WIFI_P2P_STATE_CHANGED_ACTION（ 用于指示 Wifi P2P 是否可用 ）
2. WIFI_P2P_PEERS_CHANGED_ACTION（ 对等节点列表发生了变化 ）
3. WIFI_P2P_CONNECTION_CHANGED_ACTION（ Wifi P2P 的连接状态发生了改变 ）
4. WIFI_P2P_THIS_DEVICE_CHANGED_ACTION（ 本设备的设备信息发生了变化 ）

当接收到这几个广播时，我们都需要到 WifiP2pManager （对等网络管理器）来进行相应的信息请求，此外还需要用到 Channel 对象作为请求参数

```java
mWifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
mChannel = mWifiP2pManager.initialize(this, getMainLooper(), this);
```

当收到 WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION 广播时，可以判断当前 Wifi P2P是否可用

```java
int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
    mDirectActionListener.wifiP2pEnabled(true);
} else {
    mDirectActionListener.wifiP2pEnabled(false);                
}
```

当收到 WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION 广播时，意味设备周围的可用设备列表发生了变化，可以通过 requestPeers 方法得到可用的设备列表，之后就可以选择当中的某一个设备进行连接操作

```java
mWifiP2pManager.requestPeers(mChannel, new WifiP2pManager.PeerListListener() {
    @Override
    public void onPeersAvailable(WifiP2pDeviceList peers) {
        mDirectActionListener.onPeersAvailable(peers.getDeviceList());
    }
});
```

当收到 WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION 广播时，意味着 Wifi P2P 的连接状态发生了变化，可能是连接到了某设备，或者是与某设备断开了连接

```java
NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
if (networkInfo.isConnected()) {
    mWifiP2pManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            mDirectActionListener.onConnectionInfoAvailable(info);
        }
    });
    Log.e(TAG, "已连接p2p设备");
} else {
    mDirectActionListener.onDisconnection();
    Log.e(TAG, "与p2p设备已断开连接");
}
```

如果是与某设备连接上了，则可以通过 requestConnectionInfo 方法获取到连接信息

当收到 WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION 广播时，则可以获取到本设备变化后的设备信息

```java
(WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
```

可以看出 Wifi P2P 的接口高度异步化，到现在已经用到了三个系统的回调函数，一个用于 WifiP2pManager 的初始化，两个用于在广播中异步请求数据，为了简化操作，此处统一使用一个自定义的回调函数，方法含义与系统的回调函数一致

```java
public interface DirectActionListener extends WifiP2pManager.ChannelListener {

    void wifiP2pEnabled(boolean enabled);

    void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo);

    void onDisconnection();

    void onSelfDeviceAvailable(WifiP2pDevice wifiP2pDevice);

    void onPeersAvailable(Collection<WifiP2pDevice> wifiP2pDeviceList);

}
```

所以，整个广播接收器使用到的所有代码是：

```java
/
 * @Author: leavesCZY
 * @Date: 2019/2/27 23:58
 * @Desc:
 * @Github：https://github.com/leavesCZY
 */
public class DirectBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "DirectBroadcastReceiver";

    private WifiP2pManager mWifiP2pManager;

    private WifiP2pManager.Channel mChannel;

    private DirectActionListener mDirectActionListener;

    public DirectBroadcastReceiver(WifiP2pManager wifiP2pManager, WifiP2pManager.Channel channel, DirectActionListener directActionListener) {
        mWifiP2pManager = wifiP2pManager;
        mChannel = channel;
        mDirectActionListener = directActionListener;
    }

    public static IntentFilter getIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        return intentFilter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG, "接收到广播： " + intent.getAction());
        if (!TextUtils.isEmpty(intent.getAction())) {
            switch (intent.getAction()) {
                // 用于指示 Wifi P2P 是否可用
                case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION: {
                    int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                    if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                        mDirectActionListener.wifiP2pEnabled(true);
                    } else {
                        mDirectActionListener.wifiP2pEnabled(false);
                        List<WifiP2pDevice> wifiP2pDeviceList = new ArrayList<>();
                        mDirectActionListener.onPeersAvailable(wifiP2pDeviceList);
                    }
                    break;
                }
                // 对等节点列表发生了变化
                case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION: {
                    mWifiP2pManager.requestPeers(mChannel, new WifiP2pManager.PeerListListener() {
                        @Override
                        public void onPeersAvailable(WifiP2pDeviceList peers) {
                            mDirectActionListener.onPeersAvailable(peers.getDeviceList());
                        }
                    });
                    break;
                }
                // Wifi P2P 的连接状态发生了改变
                case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION: {
                    NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                    if (networkInfo.isConnected()) {
                        mWifiP2pManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
                            @Override
                            public void onConnectionInfoAvailable(WifiP2pInfo info) {
                                mDirectActionListener.onConnectionInfoAvailable(info);
                            }
                        });
                        Log.e(TAG, "已连接p2p设备");
                    } else {
                        mDirectActionListener.onDisconnection();
                        Log.e(TAG, "与p2p设备已断开连接");
                    }
                    break;
                }
                //本设备的设备信息发生了变化
                case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION: {
                    mDirectActionListener.onSelfDeviceAvailable((WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));
                    break;
                }
            }
        }
    }

}
```

# 三、服务端

假设当设备A搜索到了设备B，并与设备B连接到了一起，此时系统会自动创建一个群组（Group）并随机指定一台设备为群主（GroupOwner）。此时，对于两台设备来说，群主的IP地址是可知的（系统回调函数中有提供），但客户端的IP地址需要再来通过其他方法来主动获取。例如，可以在设备连接成功后，客户端主动发起对服务器端的Socket连接请求，服务器端在指定端口监听客户端的连接请求，当连接成功后，服务器端就可以获取到客户端的IP地址了

此处为了简化操作，直接指定某台设备作为服务器端（群主），即直接指定某台设备用来接收文件

因此，服务器端要主动创建群组，并等待客户端的连接

```java
wifiP2pManager.createGroup(channel, new WifiP2pManager.ActionListener() {
    @Override
    public void onSuccess() {
        Log.e(TAG, "createGroup onSuccess");
        dismissLoadingDialog();
        showToast("onSuccess");
    }

     @Override
     public void onFailure(int reason) {
        Log.e(TAG, "createGroup onFailure: " + reason);
        dismissLoadingDialog();
        showToast("onFailure");
    }
});
```

此处，使用 IntentService 在后台监听客户端的 Socket 连接请求，并通过输入输出流来传输文件。此处的代码比较简单，就只是在指定端口一直堵塞监听客户端的连接请求，获取待传输的文件信息模型 FileTransfer ，之后就进行实际的数据传输

```java
   @Override
    protected void onHandleIntent(Intent intent) {
        clean();
        File file = null;
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(PORT));
            Socket client = serverSocket.accept();
            Log.e(TAG, "客户端IP地址 : " + client.getInetAddress().getHostAddress());
            inputStream = client.getInputStream();
            objectInputStream = new ObjectInputStream(inputStream);
            FileTransfer fileTransfer = (FileTransfer) objectInputStream.readObject();
            Log.e(TAG, "待接收的文件: " + fileTransfer);
            String name = new File(fileTransfer.getFilePath()).getName();
            //将文件存储至指定位置
            file = new File(Environment.getExternalStorageDirectory() + "/" + name);
            fileOutputStream = new FileOutputStream(file);
            byte buf[] = new byte[512];
            int len;
            long total = 0;
            int progress;
            while ((len = inputStream.read(buf)) != -1) {
                fileOutputStream.write(buf, 0, len);
                total += len;
                progress = (int) ((total * 100) / fileTransfer.getFileLength());
                Log.e(TAG, "文件接收进度: " + progress);
                if (progressChangListener != null) {
                    progressChangListener.onProgressChanged(fileTransfer, progress);
                }
            }
            serverSocket.close();
            inputStream.close();
            objectInputStream.close();
            fileOutputStream.close();
            serverSocket = null;
            inputStream = null;
            objectInputStream = null;
            fileOutputStream = null;
            Log.e(TAG, "文件接收成功，文件的MD5码是：" + Md5Util.getMd5(file));
        } catch (Exception e) {
            Log.e(TAG, "文件接收 Exception: " + e.getMessage());
        } finally {
            clean();
            if (progressChangListener != null) {
                progressChangListener.onTransferFinished(file);
            }
            //再次启动服务，等待客户端下次连接
            startService(new Intent(this, WifiServerService.class));
        }
    }
```

因为客户端可能会多次发起连接请求，所以当此处文件传输完成后（不管成功或失败），都需要重新 startService ，让服务再次堵塞等待客户端的连接请求

FileTransfer 包含三个字段，MD5码值用于校验文件的完整性，fileLength 是为了用于计算文件的传输进度

```java
public class FileTransfer implements Serializable {

    //文件路径
    private String filePath;

    //文件大小
    private long fileLength;

    //MD5码
    private String md5;

    ···
    
}
```

为了将文件传输进度发布到外部界面，所以除了需要启动Service外，界面还需要绑定Service，此处就需要用到一个更新文件传输状态的接口

```java
    public interface OnProgressChangListener {

        //当传输进度发生变化时
        void onProgressChanged(FileTransfer fileTransfer, int progress);

        //当传输结束时
        void onTransferFinished(File file);

    }
```

因此，需要将 progressChangListener 作为参数传给  WifiServerService ，并在进度变化时更新进度对话框

```java
 private WifiServerService.OnProgressChangListener progressChangListener = new WifiServerService.OnProgressChangListener() {
        @Override
        public void onProgressChanged(final FileTransfer fileTransfer, final int progress) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.setMessage("文件名： " + new File(fileTransfer.getFilePath()).getName());
                    progressDialog.setProgress(progress);
                    progressDialog.show();
                }
            });
        }

        @Override
        public void onTransferFinished(final File file) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.cancel();
                    if (file != null && file.exists()) {
                        openFile(file.getPath());
                    }
                }
            });
        }
    };
```

# 四、客户端

文件发送界面 SendFileActivity 需要实现 DirectActionListener 接口

首先，需要先注册P2P广播，以便获取周边设备信息以及连接状态

```java
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_file);
        initView();
        mWifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mWifiP2pManager.initialize(this, getMainLooper(), this);
        broadcastReceiver = new DirectBroadcastReceiver(mWifiP2pManager, mChannel, this);
        registerReceiver(broadcastReceiver, DirectBroadcastReceiver.getIntentFilter());
    }
```

通过 discoverPeers 方法搜索周边设备，回调函数用于通知方法是否调用成功

```java
mWifiP2pManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
    @Override
    public void onSuccess() {
        showToast("Success");
    }

    @Override
    public void onFailure(int reasonCode) {
        showToast("Failure");
        loadingDialog.cancel();
     }
});
```

当搜索结束后，系统就会触发 WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION 广播，此时就可以调用 requestPeers 方法获取设备列表信息，此处用 RecyclerView 展示列表，在  onPeersAvailable 方法刷新列表

```java
mWifiP2pManager.requestPeers(mChannel, new WifiP2pManager.PeerListListener() {
    @Override
    public void onPeersAvailable(WifiP2pDeviceList peers) {
        mDirectActionListener.onPeersAvailable(peers.getDeviceList());
    }
});
```

```java
    @Override
    public void onPeersAvailable(Collection<WifiP2pDevice> wifiP2pDeviceList) {
        Log.e(TAG, "onPeersAvailable :" + wifiP2pDeviceList.size());
        this.wifiP2pDeviceList.clear();
        this.wifiP2pDeviceList.addAll(wifiP2pDeviceList);
        deviceAdapter.notifyDataSetChanged();
        loadingDialog.cancel();
    }
```

之后，通过点击事件选中群主（服务器端）设备，通过 connect 方法请求与之进行连接

```java
private void connect() {
    WifiP2pConfig config = new WifiP2pConfig();
    if (config.deviceAddress != null && mWifiP2pDevice != null) {
        config.deviceAddress = mWifiP2pDevice.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        showLoadingDialog("正在连接 " + mWifiP2pDevice.deviceName);
        mWifiP2pManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.e(TAG, "connect onSuccess");
            }

            @Override
            public void onFailure(int reason) {
                showToast("连接失败 " + reason);
                dismissLoadingDialog();
            }
        });
    }
}
```

此处依然无法通过函数函数来判断连接结果，需要依靠系统发出的 WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION  方法来获取到连接结果，在此处可以通过 requestConnectionInfo 获取到组连接信息，信息最后通过 onConnectionInfoAvailable 方法传递出来，在此可以判断当前设备是否为群主，获取群组IP地址

```java
@Override
public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
    dismissLoadingDialog();
    wifiP2pDeviceList.clear();
    deviceAdapter.notifyDataSetChanged();
    btn_disconnect.setEnabled(true);
    btn_chooseFile.setEnabled(true);
    Log.e(TAG, "onConnectionInfoAvailable");
    Log.e(TAG, "onConnectionInfoAvailable groupFormed: " + wifiP2pInfo.groupFormed);
    Log.e(TAG, "onConnectionInfoAvailable isGroupOwner: " + wifiP2pInfo.isGroupOwner);
    Log.e(TAG, "onConnectionInfoAvailable getHostAddress: " + wifiP2pInfo.groupOwnerAddress.getHostAddress());
    StringBuilder stringBuilder = new StringBuilder();
    if (mWifiP2pDevice != null) {
        stringBuilder.append("连接的设备名：");
        stringBuilder.append(mWifiP2pDevice.deviceName);
        stringBuilder.append("\n");
        stringBuilder.append("连接的设备的地址：");
        stringBuilder.append(mWifiP2pDevice.deviceAddress);
    }
    stringBuilder.append("\n");
    stringBuilder.append("是否群主：");
    stringBuilder.append(wifiP2pInfo.isGroupOwner ? "是群主" : "非群主");
    stringBuilder.append("\n");
    stringBuilder.append("群主IP地址：");
    stringBuilder.append(wifiP2pInfo.groupOwnerAddress.getHostAddress());
    tv_status.setText(stringBuilder);
    if (wifiP2pInfo.groupFormed && !wifiP2pInfo.isGroupOwner) {
        this.wifiP2pInfo = wifiP2pInfo;
    }
}
```

至此服务器端和客户端已经通过 Wifi P2P 连接在了一起，客户端也获取到了服务器端的IP地址，在选取好待发送的文件后就可以主动发起对服务器端的连接请求了

发起选取文件的方法

```java
Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
intent.setType("*/*");
intent.addCategory(Intent.CATEGORY_OPENABLE);
startActivityForResult(intent, 1);
```

获取选取的文件的实际路径

```java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == 1) {
        if (resultCode == RESULT_OK) {
            Uri uri = data.getData();
            if (uri != null) {
                String path = getPath(this, uri);
                if (path != null) {
                    File file = new File(path);
                    if (file.exists() && wifiP2pInfo != null) {
                        FileTransfer fileTransfer = new FileTransfer(file.getPath(), file.length());
                        Log.e(TAG, "待发送的文件：" + fileTransfer);
                        new WifiClientTask(this, fileTransfer).execute(wifiP2pInfo.groupOwnerAddress.getHostAddress());
                    }
                }
            }
        }
    }
}

private String getPath(Context context, Uri uri) {
    if ("content".equalsIgnoreCase(uri.getScheme())) {
        Cursor cursor = context.getContentResolver().query(uri, new String[]{"_data"}, null, null, null);
        if (cursor != null) {
           if (cursor.moveToFirst()) {
                String data = cursor.getString(cursor.getColumnIndex("_data"));
                cursor.close();
                return data;
            }
        }
    } else if ("file".equalsIgnoreCase(uri.getScheme())) {
        return uri.getPath();
    }
    return null;
}
```

文件的发送操作放到 AsyncTask 中处理，将服务器端的IP地址作为参数传进来，在正式发送文件前，先发送包含文件信息（文件名，文件大小，文件MD5码）的信息模型 FileTransfer ，并在发送文件的过程中同时更新进度

```java
/
 * @Author: leavesCZY
 * @Date: 2019/2/27 23:56
 * @Desc: 客户端发送文件
 * @Github：https://github.com/leavesCZY
 */
public class WifiClientTask extends AsyncTask<String, Integer, Boolean> {

    private ProgressDialog progressDialog;

    private FileTransfer fileTransfer;

    private static final int PORT = 4786;

    private static final String TAG = "WifiClientTask";

    public WifiClientTask(Context context, FileTransfer fileTransfer) {
        this.fileTransfer = fileTransfer;
        progressDialog = new ProgressDialog(context);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setTitle("正在发送文件");
        progressDialog.setMax(100);
    }

    @Override
    protected void onPreExecute() {
        progressDialog.show();
    }

    @Override
    protected Boolean doInBackground(String... strings) {
        fileTransfer.setMd5(Md5Util.getMd5(new File(fileTransfer.getFilePath())));
        Log.e(TAG, "文件的MD5码值是：" + fileTransfer.getMd5());
        Socket socket = null;
        OutputStream outputStream = null;
        ObjectOutputStream objectOutputStream = null;
        InputStream inputStream = null;
        try {
            socket = new Socket();
            socket.bind(null);
            socket.connect((new InetSocketAddress(strings[0], PORT)), 10000);
            outputStream = socket.getOutputStream();
            objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(fileTransfer);
            inputStream = new FileInputStream(new File(fileTransfer.getFilePath()));
            long fileSize = fileTransfer.getFileLength();
            long total = 0;
            byte buf[] = new byte[512];
            int len;
            while ((len = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, len);
                total += len;
                int progress = (int) ((total * 100) / fileSize);
                publishProgress(progress);
                Log.e(TAG, "文件发送进度：" + progress);
            }
            outputStream.close();
            objectOutputStream.close();
            inputStream.close();
            socket.close();
            outputStream = null;
            objectOutputStream = null;
            inputStream = null;
            socket = null;
            Log.e(TAG, "文件发送成功");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "文件发送异常 Exception: " + e.getMessage());
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (objectOutputStream != null) {
                try {
                    objectOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        progressDialog.setProgress(values[0]);
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        progressDialog.cancel();
        Log.e(TAG, "onPostExecute: " + aBoolean);
    }

}
```

# 五、校验文件完整性

传输文件的完整性主要是通过计算文件的MD5码值来保证了，在发送文件前，即在 WifiClientTask 的 doInBackground 方法中进行计算，将MD5码值赋给 FileTransfer 模型，通过如下方法计算得到

```java
/
 * @Author: leavesCZY
 * @Date: 2019/2/27 23:57
 * @Desc:
 * @Github：https://github.com/leavesCZY
 */
public class Md5Util {

    public static String getMd5(File file) {
        InputStream inputStream = null;
        byte[] buffer = new byte[2048];
        int numRead;
        MessageDigest md5;
        try {
            inputStream = new FileInputStream(file);
            md5 = MessageDigest.getInstance("MD5");
            while ((numRead = inputStream.read(buffer)) > 0) {
                md5.update(buffer, 0, numRead);
            }
            inputStream.close();
            inputStream = null;
            return md5ToString(md5.digest());
        } catch (Exception e) {
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static String md5ToString(byte[] md5Bytes) {
        StringBuilder hexValue = new StringBuilder();
        for (byte b : md5Bytes) {
            int val = ((int) b) & 0xff;
            if (val < 16) {
                hexValue.append("0");
            }
            hexValue.append(Integer.toHexString(val));
        }
        return hexValue.toString();
    }

}
```

因为客户端会将 FileTransfer 传给服务器端，所以服务器端在文件传输结束后，可以重新计算文件的 MD5码值，进行对比以判断文件是否完整

项目主页：[Android 实现无网络传输文件](https://github.com/leavesCZY/WifiP2P) ，欢迎 star