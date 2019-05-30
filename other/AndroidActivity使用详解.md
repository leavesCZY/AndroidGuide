Activity 是 Android 系统四大应用组件之一，用户可与 Activity 提供的屏幕进行交互，进行拨打电话、拍摄照片、发送电子邮件等操作。 每个 Activity 都会获得一个用于绘制其用户界面的窗口，窗口通常会充满屏幕，但也可小于屏幕并浮动在其他窗口之上

一个应用通常由多个彼此松散联系的 Activity 组成。一般会指定应用中的某个 Activity 为主 Activity，即首次启动应用时呈现给用户的那个 Activity。每个 Activity 均可启动另一个 Activity，以便执行不同的操作。 每次新 Activity 启动时，前一 Activity 便会停止，系统会在返回栈中保留该 Activity。 当新 Activity 启动时，系统会将其推送到返回栈上，并取得用户焦点。 返回栈遵循基本的“后进先出”堆栈机制，因此，当用户完成当前 Activity 并按“返回”按钮时，系统会从堆栈中将其弹出并销毁，然后恢复前一 Activity

当一个 Activity 因某个新 Activity 启动而停止时，系统会通过该 Activity 的生命周期回调方法通知其这一状态变化。Activity 因状态变化，比如创建、暂停、停止、恢复或销毁等而收到的回调方法有若干种，每一种回调都会为开发者提供执行与该状态变化相应的特定操作的机会。 例如，Activity 停止时， Activity 应释放任何大型对象，例如网络或数据库连接。 当 Activity 恢复时，可以重新获取所需资源，并恢复执行中断的操作

## **一、创建 Activity**

要创建 Activity，必须创建 Activity 的子类（或使用其现有子类），然后在子类中实现 Activity 在其生命周期的各种状态之间转变时系统调用的回调方法
两个最重要的回调方法是：

 - onCreate()
  必须实现此方法。系统会在创建 Activity 时调用此方法，应在此实现初始化 Activity 的必需组件。 最重要的是，必须在此方法内调用 **setContentView()** 方法，以定义 Activity 用户界面的布局
 - onPause()
  系统将此方法作为用户离开 Activity 的第一个信号（但并不总是意味着 Activity 会被销毁）进行调用。 通常应该在此方法内保存用户的任何有效更改或重要数据，因为 Activity 可能会被意外中断

此外还应使用几种其他生命周期回调方法，以便提供流畅的 Activity 间用户体验，以及妥善处理 Activity 停止甚至被意外中断的情况

## **二、实现用户界面**

Activity 的用户界面是由层级式视图：衍生自 View 类的对象 — 提供的。每个视图都控制 Activity 窗口内的特定矩形空间，可对用户交互作出响应。 例如，视图可以是在用户触摸时启动某项操作的按钮。此外，“布局”是衍生自 ViewGroup 的视图，为其子视图提供唯一布局模型，例如线性布局、网格布局或相对布局。还可以为 View 类和 ViewGroup 类创建子类（或使用其现有子类）来自行创建小部件和布局，然后将它们应用于 Activity 布局

利用视图定义布局的最常见方法是借助保存在应用资源内的 XML 布局文件。这样一来，就可以将用户界面的设计与定义 Activity 行为的源代码分开维护。通过 **setContentView()** 方法将布局设置为 Activity 的 UI，从而传递布局的资源 ID。不过，也可以在 Activity 代码中创建新 View，并通过将新 View 插入 ViewGroup 来创建视图层次，然后通过将根 ViewGroup 传递到 setContentView() 来使用该布局

## **三、在清单文件中声明 Activity**

必须在清单文件中声明要使用的 Activity，这样系统才能访问它。声明方式是将 **< activity >** 元素添加为 **< application >** 元素的子项

```java
    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/AppTheme">
		
		
        <activity android:name=".Example">
        </activity>
		
    </application>
```

还可以在此元素中加入几个其他属性值以定义 UI 风格或者运行属性。 **android:name** 属性是唯一必需的属性，它用于指定 Activity 的类名

更详细的介绍可以看这里：[Android Activity标签属性](https://www.jianshu.com/p/8598825222cc)

此外，**< activity >** 元素还可以指定各种 Intent 过滤器，使用 **< intent-filter >** 元素以声明其他应用组件激活它的方法

当使用 Android Stduio 创建新应用时，会自动为主 Activity 设置一个 Intent 过滤器，其中声明了该 Activity 响应主操作且置于“launcher”类别内

```java
        <activity android:name=".MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
```
**< action >** 元素指定这是应用的主入口点。**< category >** 元素指定此 Activity 应列入系统的应用启动器内，以便用户启动该 Activity

如果打算让应用成为独立应用，不允许其他应用激活其 Activity，则不需要任何其他 Intent 过滤器。不想提供给其他应用的 Activity 不应有任何 Intent 过滤器，可以利用显式 Intent 自行启动它们

不过，如果想让 Activity 对衍生自其他应用（以及自有应用）的隐式 Intent 作出响应，则必须为 Activity 定义、 Intent 过滤器。 对于想要作出响应的每一个 Intent 类型，都必须加入相应的 **< intent-filter >**元素，其中包括一个 **< action >** 元素，还可选择性地包括一个 **< category >** 元素或一个 **< data >** 元素。这些元素指定了 Activity 可以响应的 Intent 类型

## **四、启动 Activity**

可以通过调用 **startActivity()** 方法并传入描述了想启动的 Activity 信息的 Intent ，以此来启动 Activity。Intent 对象会指定想启动的具体 Activity 或描述想执行的操作类型（系统会选择合适的 Activity，甚至是来自其他应用的 Activity）。 Intent 对象还可以携带少量数据传递给所启动 Activity

在自有应用内工作时，经常只需要启动某个已知 Activity，则可以直接通过类名创建一个显式定义想启动的 Activity 的 Intent 对象来实现此目的
例如，可以通过以下代码让一个 Activity 启动另一个名为 SignInActivity 的 Activity：

```java
Intent intent = new Intent(this, SignInActivity.class);
startActivity(intent);
```
不过，应用可能还需要进行发送电子邮件、查看地位置、拨打电话等操作。在这种情况下，应用自身可能不具有执行此类操作所需的 Activity，因此可以改为利用设备上其他应用提供的 Activity 来执行这些操作，这便是 Intent 对象的真正价值所在。
通过创建一个 Intent 对象，对想执行的操作进行描述，系统会从其他应用启动相应的 Activity。 如果有多个 Activity 可以处理 Intent，则用户可以选择要使用哪一个。 例如，如果想允许用户发送电子邮件，可以创建以下 Intent：

```java
Intent intent = new Intent(Intent.ACTION_SEND);
intent.putExtra(Intent.EXTRA_EMAIL, recipientArray);
startActivity(intent);
```
添加到 Intent 中的数据包含要发送的电子邮件地址。 当电子邮件应用响应此 Intent 时，它会读取 extra 中提供的字符串数组，并将它们放入电子邮件撰写窗体的“收件人”字段

## **五、启动 Activity 以获得结果**

一般情况下，我们要从当前Activity跳转到另一个 Activity，采用的方法是通过 **startActivity(Intent)** 来携带数据并实现跳转 
有时候又需要跳转到的 Activity 能够在回退时返回处理结果给前一个   Activity，这就要用到以下方法了

```java
public void startActivityForResult(Intent intent, int requestCode)
```
该方法同样是用于实现 Activity 的跳转，不过多了一个 int 类型的请求码 **requestCode** 
**requestCode** 用于标示当 Activity 回退时，处理结果是从哪一个 Activity 传递来的，因为当前 Activity 有可能需要跳转到多个 Activity

此外，Activity 类还包含有以下方法

```java
protected void onActivityResult(int requestCode, int resultCode, Intent data)
```
**requestCode** 即是请求码，**resultCode** 用于标示数据处理结果，**data** 用于携带数据

因此，我们可以用以下代码实现Activity的跳转

```java
		private int REQUEST_CODE = 10;

        Intent intent = new Intent(MainActivity.this, Main2Activity.class);
        Bundle bundle = new Bundle();
        bundle.putString("requestText", "啊哈");
        intent.putExtras(bundle);
        startActivityForResult(intent, REQUEST_CODE);
```
向 Intent 中传入需要传递给 Main2Activity 的数据

然后，在 **onActivityResult()** 中处理 Main2Activity 回退后传递而来的数据处理结果，这里用 Log 来输出

```java
 @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE) {
            Bundle bundle = data.getExtras();
            Log.e("MainActivity", bundle.getString("responseText"));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
```
其中，需要判断 requestCode 是否与请求码相等 
此外，查看源码可知，resultCode 取值一般为以下三个，用来标示操作结果

```java
    /** Standard activity result: operation canceled. */
    //标示操作取消
    public static final int RESULT_CANCELED    = 0;

    /** Standard activity result: operation succeeded. */
    //标示操作成功
    public static final int RESULT_OK           = -1;

    /** Start of user-defined activity results. */
    public static final int RESULT_FIRST_USER   = 1;
```
以上都是在 MainActivity 中操作，此外还需要在 Main2Activity 中设置返回结果 
先取出传递而来的数据 requestText

```java
	private String requestText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        Intent intent = getIntent();
        if (intent != null) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                requestText = bundle.getString("requestText");
            }
        }
        init();
    }
```
通过 **setResult()** 方法设置处理结果为 **RESULT_OK**，并通过 **Intent** 携带处理结果

```java
	public void init() {
        findViewById(R.id.response).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                Bundle bundle = new Bundle();
                bundle.putString("responseText", "收到第一个Activity的请求内容为：" + requestText);
                intent.putExtras(bundle);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }
```
这样就可以实现当 Activity 回退时，携带处理结果到前一个 Activity 了

## **六、管理 Activity 生命周期**

通过实现回调方法管理 Activity 的生命周期对开发强大而又灵活的应用至关重要

Activity 基本上以三种状态存在：

 - 运行中
  此 Activity 位于屏幕前台并具有用户焦点
 - 暂停
  另一个 Activity 位于屏幕前台并具有用户焦点，但此 Activity 仍可见。也就是说，另一个 Activity 显示在此 Activity 上方，并且该 Activity 部分透明或未覆盖整个屏幕。 暂停的 Activity 处于完全活动状态（Activity 对象保留在内存中，它保留了所有状态和成员信息，并与窗口管理器保持连接），但在内存极度不足的情况下，可能会被系统终止
 - 停止
  该 Activity 被另一个 Activity 完全遮盖（该 Activity 目前位于后台完全不可见状态）。 已停止的 Activity 同样仍处于活动状态（Activity 对象保留在内存中，它保留了所有状态和成员信息，但未与窗口管理器连接）。 不过，它对用户不再可见，在系统需要内存时可能会被系统终止

如果 Activity 处于暂停或停止状态，系统可通将其结束（调用其 finish() 方法）或直接终止其进程，将其从内存中删除

## **七、实现生命周期回调**

当一个 Activity 转入和转出上述不同状态时，系统会通过各种回调方法向其发出通知

 - Activity 的整个生命周期发生在 onCreate() 调用与 onDestroy() 调用之间。 Activity 应在 onCreate() 中进行全局状态设置（例如定义布局），并在 onDestroy() 中释放所有资源。例如，如果 Activity 有一个在后台运行的用于从网络上下载数据的线程，它可能会在 onCreate() 中创建该线程，然后在 onDestroy() 中停止该线程
 - Activity 的可见生命周期发生在 onStart() 调用与 onStop() 调用之间。在这段时间，用户可以在屏幕上看到 Activity 并与其交互。 例如，当一个新 Activity 启动，并且此 Activity 不再可见时，系统会调用 onStop()。可以在调用这两个方法之间保留向用户显示 Activity 所需的资源。 例如，可以在 onStart() 中注册一个 BroadcastReceiver 以监控影响 UI 的变化，并在用户无法再看到显示的内容时在 onStop() 中将其取消注册。在 Activity 的整个生命周期，当 Activity 在对用户可见和隐藏两种状态中交替变化时，系统可能会多次调用 onStart() 和 onStop()
 - Activity 的前台生命周期发生在 onResume() 调用与 onPause() 调用之间。在这段时间，Activity 位于屏幕上的所有其他 Activity 之前，并具有用户输入焦点。 Activity 可频繁转入和转出前台。例如，当设备转入休眠状态或出现对话框时，系统会调用 onPause()。 由于此状态可能经常发生转变，因此这两个方法中应采用适度轻量级的代码，以避免因转变速度慢而让用户等待

下图说明了这些循环以及 Activity 在状态转变期间可能经过的路径

![](http://upload-images.jianshu.io/upload_images/2552605-7a9a18f68e2cd178?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

## **八、保存 Activity 状态**

当 Activity 暂停或停止时，Activity 对象仍保留在内存中，有关其成员和当前状态的所有信息仍处于活动状态。 因此，用户在 Activity 内所做的任何更改都会得到保留，这样一来，当 Activity 返回前台时，这些更改仍然存在

不过，有些设备配置可能会在运行时发生变化（例如屏幕方向切换，键盘可用性及语言改变等），默认情况下当前 Activity 就会被销毁并重新创建 。此行为旨在通过利用开发者提供的备用资源（例如适用于不同屏幕方向和屏幕尺寸的不同布局）来帮助应用适应新配置。此外，当系统为了获取更多内存时，后台 Activity 有可能会被销毁，然后在用户返回 Activity 时重建 Activity 对象。但用户并不知道系统销毁了 Activity 并又对其进行了重建，因此用户会认为 Activity 状态毫无变化
在这种情况下，可以实现另一个回调方法对有关 Activity 状态的信息进行保存：**onSaveInstanceState()**

系统在意外中断 Activity 前会先调用 **onSaveInstanceState()** 方法，系统会向该方法传递一个 Bundle，可以在其中使用 **putString()** 和 **putInt()** 等方法以键值对的形式保存有关 Activity 状态的信息。然后，如果系统终止了该 Activity ，并且用户返回到该 Activity，则系统会重建该 Activity并将 Bundle 同时传递给 **onCreate()** 和 **onRestoreInstanceState()** 两个方法。可以使用上述任一方法从 Bundle 提取保存的状态并恢复该 Activity 状态
一般是在 onRestoreInstanceState() 当中执行恢复操作比较合理，因为该方法只在 Bundle 不为空时才会被调用，而在 onCreate() 方法中还需要再次判断 Bundle 的值（在首次创建 Activity 时 Bundle 就为空）

无法保证系统一定会在销毁 Activity 前调用 onSaveInstanceState() 方法，因为存在不需要保存状态的情况，比如用户使用返回键离开了 Activity 时，因为用户的行为是在显式关闭 Activity ，所以不会调用该方法。如果系统要调用 onSaveInstanceState() 方法，它会在调用 onStop() 之前，并且可能会在调用 onPause() 之前进行调用

测试 Activity 从第一次创建到旋转屏幕各回调函数的执行顺序：

```java
com.czy.myapplication E/MainActivity: onCreate被调用
com.czy.myapplication E/MainActivity: onStart被调用
com.czy.myapplication E/MainActivity: onResume被调用

com.czy.myapplication E/MainActivity: onPause被调用
com.czy.myapplication E/MainActivity: onSaveInstanceState被调用
com.czy.myapplication E/MainActivity: onStop被调用
com.czy.myapplication E/MainActivity: onDestroy被调用

com.czy.myapplication E/MainActivity: onCreate被调用
com.czy.myapplication E/MainActivity: onStart被调用
com.czy.myapplication E/MainActivity: onRestoreInstanceState被调用
com.czy.myapplication E/MainActivity: onResume被调用
```

不过，即使开发者什么都不做，也不实现 onSaveInstanceState() 方法，Activity 类的 onSaveInstanceState() 默认实现也会恢复部分 Activity 状态。具体地讲，默认实现会为布局中的每个 View 调用相应的 onSaveInstanceState() 方法，让每个视图都能提供有关自身的应保存信息。Android 框架中几乎每个小部件都会根据需要实现此方法，以便在重建 Activity 时自动保存和恢复对 UI 所做的任何可见更改。例如，EditText 小部件保存用户输入的任何文本，CheckBox 小部件保存复选框的选中或未选中状态。前提是为想要保存其状态的每个小部件提供了一个唯一的 ID（通过 android:id 属性），如果小部件没有 ID，则系统无法保存其状态

尽管 onSaveInstanceState() 的默认实现会保存有关您的Activity UI 的有用信息，但有时仍需替换它以保存更多信息。例如，可能需要保存在 Activity 生命周期内发生了变化的成员值（它们可能与 UI 中恢复的值有关联，但默认情况下系统不会恢复储存这些 UI 值的成员）

由于 onSaveInstanceState() 的默认实现有助于保存 UI 的状态，因此如果为了保存更多状态信息而替换该方法，应始终先调用 onSaveInstanceState() 的超类实现，然后再执行任何操作。 同样，如果想替换 onRestoreInstanceState() 方法，也应调用它的超类实现，以便默认实现能够恢复视图状态

由于无法保证系统会调用 onSaveInstanceState()，因此开发者只应利用它来记录 Activity 的瞬态（UI 的状态），切勿使用它来存储持久性数据，而应使用 onPause() 在用户离开 Activity 后存储持久性数据

## **九、协调 Activity**

生命周期回调的顺序经过了明确定义，当两个 Activity 位于同一进程，并且由一个 Activity 启动另一个 Activity 时，其定义尤其明确
首先新建一个MainActivity，重写其各个回调函数，添加一句Log输出语句，类似如下所示

```java
 private final String TAG = "MainActivity";
    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "onStart被调用");
    }
```
再在主布局文件中添加一个按钮，用于启动第二个Activity——Main2Activity 。也重写Main2Activity的各大回调函数，与MainActivity类似

```java
private final String TAG = "第二个Activity";
     @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "onStart被调用");
    }
```
启动MainActivity，输出语句如下：

```java
com.czy.myapplication E/MainActivity: onCreate被调用
com.czy.myapplication E/MainActivity: onStart被调用
com.czy.myapplication E/MainActivity: onResume被调用
```
点击按钮切换Activity

```java
com.czy.myapplication E/MainActivity: onPause被调用
com.czy.myapplication E/第二个Activity: onCreate被调用
com.czy.myapplication E/第二个Activity: onStart被调用
com.czy.myapplication E/第二个Activity: onResume被调用
com.czy.myapplication E/MainActivity: onStop被调用
```
点击back键回退到MainActivity

```java
com.czy.myapplication E/第二个Activity: onPause被调用
com.czy.myapplication E/MainActivity: onRestart被调用
com.czy.myapplication E/MainActivity: onStart被调用
com.czy.myapplication E/MainActivity: onResume被调用
com.czy.myapplication E/第二个Activity: onStop被调用
com.czy.myapplication E/第二个Activity: onDestroy被调用
```
可以看出来，启动第二个 Activity 的过程与停止第一个 Activity 的过程存在重叠

可以利用这种可预测的生命周期回调顺序管理从一个 Activity 到另一个 Activity 的信息转变。 例如，如果必须在第一个 Activity 停止时向数据库写入数据，以便下一个 Activity 能够读取该数据，则应在 onPause() 而不是 onStop() 执行期间写入数据。不过，原 Activity 的 onPause() 方法不能用于执行耗时操作，否则会影响新的 Activity 的显示