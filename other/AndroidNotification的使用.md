Notification（通知）是可以在应用的常规 UI 外部向用户显示的消息。当App告知系统发出通知时，它将先以图标的形式显示在通知区域中。用户可以打开抽屉式通知栏查看通知的详细信息

## **一、创建通知**
在 **NotificationCompat.Builder** 对象中为通知指定 UI 信息和操作，然后通过调用 **NotificationCompat.Builder.build()** 创建通知，它将返回包含符合指定要求的 Notification 对象
要发出通知，通过调用 **NotificationManager.notify()** 将 Notification 对象传递给系统

## **二、必需的通知内容**
Notification 对象必须包含以下内容：

 - 小图标，由 setSmallIcon() 设置
 - 标题，由 setContentTitle() 设置
 - 详细文本，由 setContentText() 设置

其他通知设置和内容都是可选的

## **三、通知操作**
尽管通知操作都是可选的，但是一般都需要向通知添加一个操作，例如让用户直接从通知跳转到应用中的 Activity，在其中查看一个更详细的信息
在 Notification 内部，操作本身由 **PendingIntent** 定义，后者包含在应用中启动 Activity 的 Intent。如果要在用户点击抽屉式通知栏中的通知文本时启动 Activity，则可通过调用 **setContentIntent()** 来添加 PendingIntent

## **四、通知优先级**
可以根据需要设置通知的优先级。优先级充当一个提示，提醒设备 UI 应该如何显示通知。 要设置通知的优先级，需调用 **NotificationCompat.Builder.setPriority()** 并传入一个 **NotificationCompat** 优先级常量。有五个优先级别，范围从 **PRIORITY_MIN (-2)** 到 **PRIORITY_MAX (2)**；如果未设置，则优先级默认为 **PRIORITY_DEFAULT (0)**

## **五、PendingIntent**
PendingIntent类封装了一个Intent和一个动作，当调用该类的send方法的时候，将会执行该动作。由于PendingIntent类是一个待处理的意图，这个动作通常是在将来的某个时刻要调用的一个操作，很可能是系统要调用的。例如，一个PendingIntent类可以用于构造一个Notification，以便当用户触碰该通知的时候，能够触发某些动作
PendingIntent类中的动作是Context类中的几个方法之一，例如**startActivity，startService，sendBroadcast**
使用PendingIntent来启动一个Activity

```java
   PendingIntent pi = PendingIntent.getActivity(Context context, int requestCode,Intent intent, int flags);
```
静态方法getActivity是返回PendingIntent类的一个实例的几个方法之一，其他的方法还有**getActivities、getService和getBroadcast**
这些方法决定了最终PendingIntent所能执行的动作，可以用来启动Activity，启动Service，发送广播等

## **六、NotificationManager**
要发布一个通知，可以使用NotificationManager，这是Android系统中的内建服务之一，是一个已有的系统服务，可以通过如下代码来获取它

```java
NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
```
然后，在该NotificationManager上调用notify方法来发布一个通知，需要传入唯一的ID和Notification对象

```java
mNotificationManager.notify(int id, notification);
```
通知ID用于标识该通知，在想要取消特定Notification的时候，就需要使用到它

```java
mNotificationManager.cancel(int id);
```

此外，除非发生以下情况之一，否则通知会一直可见：

 - 用户单独或通过使用“全部清除”清除了该通知（如果通知可以清除）
 - 用户点击通知，且在创建通知时调用了 **setAutoCancel(true)**
 - 针对特定的通知 ID 调用了 **cancel(int id)**
 - 调用了 **cancelAll()** 方法，该方法将删除之前发出的所有通知

## **七、创建简单通知**
在布局文件中加入两个按钮

```java
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/activity_main"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    tools:context="com.example.zy.notification.MainActivity">

    <Button
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:onClick="notification"
        android:text="显示通知" />

    <Button
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:onClick="cleanNotification"
        android:text="去除通知" />

</LinearLayout>

```

```java
    private int id = 1;

    public void notification(View view) {
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.lufei);
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        //设置小图标
        mBuilder.setSmallIcon(R.drawable.ic_launcher);
        //设置大图标
        mBuilder.setLargeIcon(bitmap);
        //设置标题
        mBuilder.setContentTitle("这是标题");
        //设置通知正文
        mBuilder.setContentText("这是正文，当前ID是：" + id);
        //设置摘要
        mBuilder.setSubText("这是摘要");
        //设置是否点击消息后自动clean
        mBuilder.setAutoCancel(true);
        //显示指定文本
        mBuilder.setContentInfo("Info");
        //与setContentInfo类似，但如果设置了setContentInfo则无效果
        //用于当显示了多个相同ID的Notification时，显示消息总数
        mBuilder.setNumber(2);
        //通知在状态栏显示时的文本
        mBuilder.setTicker("在状态栏上显示的文本");
        //设置优先级
        mBuilder.setPriority(NotificationCompat.PRIORITY_MAX);
        //自定义消息时间，以毫秒为单位，当前设置为比系统时间少一小时
        mBuilder.setWhen(System.currentTimeMillis() - 3600000);
        //设置为一个正在进行的通知，此时用户无法清除通知
        mBuilder.setOngoing(true);
        //设置消息的提醒方式，震动提醒：DEFAULT_VIBRATE     声音提醒：NotificationCompat.DEFAULT_SOUND
        //三色灯提醒NotificationCompat.DEFAULT_LIGHTS     以上三种方式一起：DEFAULT_ALL
        mBuilder.setDefaults(NotificationCompat.DEFAULT_SOUND);
        //设置震动方式，延迟零秒，震动一秒，延迟一秒、震动一秒
        mBuilder.setVibrate(new long[]{0, 1000, 1000, 1000});

        Intent intent = new Intent(this, ResultActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);
        mBuilder.setContentIntent(pIntent);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(id++, mBuilder.build());
    }


    public void cleanNotification(View view) {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();
        mNotificationManager.cancel(1);
    }
```
运行效果

![这里写图片描述](http://upload-images.jianshu.io/upload_images/2552605-d7c1baf35613191b?imageMogr2/auto-orient/strip)

## **八、创建多文本通知**

```java
public void notification(View view) {
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.lufei);
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle("这是标题");
        builder.setContentText("这是正文");
        builder.setSmallIcon(R.drawable.lufei);
        builder.setLargeIcon(bitmap);
        builder.setDefaults(NotificationCompat.DEFAULT_ALL);
        builder.setAutoCancel(true);

        NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle();
        style.bigText("这里可以写很长的一段话，不信你试试。这里可以写很长的一段话，不信你试试。这里可以写很长的一段话，不信你试试。");
        style.setBigContentTitle("点击后的标题");
        style.setSummaryText("这是摘要");
        builder.setStyle(style);

        Intent intent = new Intent(this, ResultActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);
        builder.setContentIntent(pIntent);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(id, builder.build());
    }
```
运行效果

![这里写图片描述](http://upload-images.jianshu.io/upload_images/2552605-57ac030cacab5f94?imageMogr2/auto-orient/strip)

## **九、将扩展布局应用于通知**

```java
public void notification(View view) {
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.lufei);
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle("这是标题");
        builder.setContentText("这是正文");
        builder.setSmallIcon(R.drawable.lufei);
        builder.setLargeIcon(bitmap);
        builder.setDefaults(NotificationCompat.DEFAULT_ALL);
        builder.setAutoCancel(true);
        
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        String[] events = {"第一", "第二", "第三", "第四", "第五", "第六", "第七"};
        inboxStyle.setBigContentTitle("展开后的标题");
        inboxStyle.setSummaryText("这是摘要");
        for (String event : events) {
            inboxStyle.addLine(event);
        }
        builder.setStyle(inboxStyle);

        Intent intent = new Intent(this, ResultActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);
        builder.setContentIntent(pIntent);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(id, builder.build());
    }
```
运行效果

![这里写图片描述](http://upload-images.jianshu.io/upload_images/2552605-f57079e2d5d19fae?imageMogr2/auto-orient/strip)

## **十、大图样式**

```java
public void notification(View view) {
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.lufei);
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle("这是标题");
        builder.setContentText("这是正文");
        builder.setSmallIcon(R.drawable.lufei);
        builder.setLargeIcon(bitmap);
        builder.setDefaults(NotificationCompat.DEFAULT_ALL);
        builder.setAutoCancel(true);

        NotificationCompat.BigPictureStyle style = new NotificationCompat.BigPictureStyle();
        style.setBigContentTitle("展开后的标题");
        style.setSummaryText("这是摘要");
        style.bigPicture(bitmap);
        builder.setStyle(style);

        Intent intent = new Intent(this, ResultActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);
        builder.setContentIntent(pIntent);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(id, builder.build());
    }
```

运行效果

![这里写图片描述](http://upload-images.jianshu.io/upload_images/2552605-185e149a7229d1b2?imageMogr2/auto-orient/strip)

## **十一、更新通知**
当需要为同一类型的事件多次发出同一通知时，应避免创建全新的通知， 而是应考虑通过更改之前通知的某些值来更新通知
想要将通知设置为能够更新的状态，需要通过调用 **NotificationManager.notify(int id, notification)** 发出带有通知 ID 的通知。 
要在发出之后更新此通知，请更新或创建 **NotificationCompat.Builder** 对象，从该对象构建 **Notification** 对象，并发出与之前所用 ID 相同的 **Notification**。如果之前的通知仍然可见，则系统会根据 **Notification** 对象的内容更新该通知。相反，如果之前的通知已被清除，系统则会创建一个新通知

## **十二、启动 Activity 时保留返回栈**
默认情况下，从应用外点击通知启动一个Activity后，按返回键会回到主屏幕。但某些时候需要按返回键后仍然留在当前应用的需求，这就要用到TaskStackBuilder了
执行以下步骤：

### **1. 在清单文件中定义应用的 Activity 层次结构**

 - 添加对 Android 4.0.3 及更低版本的支持。为此，需要通过添加 **< meta-data >** 元素作为**< activity >**的子项来指定通过点击通知启动后的Activity的父项
  对于此元素，设置

```java
 android:name="android.support.PARENT_ACTIVITY"
 android:value="<parent_activity_name>"
```
其中，**< parent_activity_name >** 是父Activity元素的**android:name**值

-  同样添加对 Android 4.1 及更高版本的支持。为此，将 **android:parentActivityName** 属性添加到启动的 Activity 的 < activity > 元素中

最终的 XML 应如下所示：

```java
        <activity android:name=".MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        <activity
            android:name=".ResultActivity"
            android:parentActivityName=".MainActivity">
            <meta-data
                android:name="android.support.PARENT_ACTIVITY"
                android:value=".MainActivity" />
        </activity>
```

### **2. 根据可启动 Activity 的 Intent 创建返回栈：**

 - 创建 Intent设置目标Activity
 - 通过调用 **TaskStackBuilder.create()** 创建堆栈生成器
 - 通过调用 **addParentStack()** 将返回栈添加到堆栈生成器。 对于在清单文件中所定义层次结构内的每个 Activity，返回栈均包含可启动 Activity 的 Intent 对象。此方法还会添加一些可在全新任务中启动堆栈的标志
 - 尽管 **addParentStack()** 的参数是对已启动 Activity 的引用，但是方法调用不会添加可启动 Activity 的 Intent，而是留待下一步进行处理
 - 通过调用 **addNextIntent()**，添加可从通知中启动 Activity 的 Intent。 将在第一步中创建的 Intent 作为 **addNextIntent()** 的参数传递
 - 如需，请通过调用 **TaskStackBuilder.editIntentAt()** 向堆栈中的 Intent 对象添加参数。有时，需要确保目标 Activity 在用户使用“返回”导航回它时会显示有意义的数据
 - 通过调用 **getPendingIntent()** 获得此返回栈的 PendingIntent。 然后，可以使用此 PendingIntent 作为 **setContentIntent()** 的参数

示例代码：

```java
   public void notification(View view) {
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.lufei);
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle("这是标题");
        builder.setContentText("这是正文");
        builder.setSmallIcon(R.drawable.lufei);
        builder.setLargeIcon(bitmap);
        builder.setDefaults(NotificationCompat.DEFAULT_ALL);
        builder.setAutoCancel(true);
        
        Intent resultIntent = new Intent(this, ResultActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(ResultActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(id, builder.build());
    }
```

运行效果

![这里写图片描述](http://upload-images.jianshu.io/upload_images/2552605-f65d56d10fffabd0?imageMogr2/auto-orient/strip)

## **十三、不保留返回栈**
默认情况下，从通知启动的Activity会在近期任务列表里出现。如果不需要在近期任务里显示，就不必在清单文件中定义其 Activity 层次结构，也不必调用 addParentStack() 来构建返回栈
取而代之的是，可使用清单文件设置 Activity 任务选项，并通过调用 getActivity() 创建 PendingIntent：

 - 在清单文件中，将以下属性添加到 Activity 的 < activity > 元素
  android:name = "activityclass"  （Activity 的完全限定类名）
  android:taskAffinity = ""            （与在代码中设置的 FLAG_ACTIVITY_NEW_TASK 标志相结合，这可确保此 Activity 不会进入应用的默认任务。任何具有应用默认关联的现有任务均不受影响）
  android:excludeFromRecents = "true"   （将新任务从“最新动态”中排除，这样用户就不会在无意中导航回它）

```java
        <activity
            android:name=".ResultActivity"
            android:excludeFromRecents="true"
            android:launchMode="singleTask"
            android:taskAffinity="" />
```
 -  构建并发出通知：
     - 创建 Intent设置目标Activity
     - 通过使用 **FLAG_ACTIVITY_NEW_TASK** 和 **FLAG_ACTIVITY_CLEAR_TASK** 标志调用 **setFlags()**，将 Activity 设置为在新的空任务中启动
     - 为 Intent 设置所需的任何其他选项
     - 通过调用 **getActivity()** 从 Intent 中创建 PendingIntent。 然后，使用此 PendingIntent 作为 **setContentIntent()** 的参数

示例代码：

```java
public void notification(View view) {
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.lufei);
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle("这是标题");
        builder.setContentText("这是正文");
        builder.setSmallIcon(R.drawable.lufei);
        builder.setLargeIcon(bitmap);
        builder.setDefaults(NotificationCompat.DEFAULT_ALL);
        builder.setAutoCancel(true);

        Intent notifyIntent = new Intent(this, ResultActivity.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(id, builder.build());
    }
```
运行结果

![这里写图片描述](http://upload-images.jianshu.io/upload_images/2552605-452de4c0cfc59738?imageMogr2/auto-orient/strip)

## **十四、在通知中显示进度**
通知可能包括动画形式的进度指示器，向用户显示正在进行的操作状态。 如果可以估计操作所需的时间以及任意时刻的完成进度，则使用“限定”形式的指示器（进度栏）。 如果无法估计操作的时长，则使用“非限定”形式的指示器（Activity 指示器）
要在 Android 4.0 及更高版本的平台上使用进度指示器，需调用 setProgress()。对于早期版本，必须创建包括 ProgressBar 视图的自定义通知布局

要显示限定形式的进度栏，需要通过调用 **setProgress(max, progress, false)** 将进度栏添加到通知，然后发出通知。随着操作的进行，递增 progress 并更新通知

```java
public void notification(View view) {
        final NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle("这是标题");
        builder.setContentText("这是正文");
        builder.setSmallIcon(R.drawable.lufei);

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int progress = 0; progress <= 100; progress += 5) {
                    builder.setProgress(100, progress, false);
                    mNotificationManager.notify(id, builder.build());
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                builder.setContentText("下载完成").setProgress(0, 0, false);
                mNotificationManager.notify(id, builder.build());
            }
        }).start();
 }
```
运行效果

![这里写图片描述](http://upload-images.jianshu.io/upload_images/2552605-677a9c2748199be9?imageMogr2/auto-orient/strip)

此外还有另一种形式的进度条，即非限定形式
要显示非限定形式的 Activity 指示器，需使用 **setProgress(0, 0, true)** 将其添加到通知（忽略前两个参数），然后发出通知。这样一来，指示器的样式就与进度栏相同，只是其动画还在继续

在操作开始之际发出通知。除非主动修改通知，否则动画将一直运行。 操作完成后，调用 **setProgress(0, 0, false)**，然后更新通知以删除 Activity 指示器否则，否则即使操作完成，动画仍将运行。同时，请记得更改通知文本，以表明操作已完成

```java
public void notification(View view) {
        final NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle("这是标题");
        builder.setContentText("这是正文");
        builder.setSmallIcon(R.drawable.lufei);
        builder.setProgress(0, 0, true);
        mNotificationManager.notify(id, builder.build());

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int progress = 0; progress <= 100; progress += 5) {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                builder.setContentText("下载完成").setProgress(0, 0, false);
                mNotificationManager.notify(id, builder.build());
            }
        }).start();
    }

```
运行效果

![这里写图片描述](http://upload-images.jianshu.io/upload_images/2552605-ed75f1cef678bbca?imageMogr2/auto-orient/strip)

## **十五、自定义通知布局**
可以利用通知框架定义自定义通知布局，由该布局定义通知在 **RemoteViews** 对象中的外观。 自定义布局通知类似于常规通知，但是它们是基于 XML 布局文件中所定义的 **RemoteViews**

自定义通知布局的可用高度取决于通知视图。普通视图布局限制为 **64 dp**，扩展视图布局限制为 **256 dp**

要定义自定义通知布局，需要首先实例化 RemoteViews 对象来扩充 XML 布局文件。然后，调用 **setContent()**。要在自定义通知中设置内容详细信息，请使用 RemoteViews 中的方法设置视图子项的值

注意：使用自定义通知布局时，要特别注意确保自定义布局适用于不同的设备方向和分辨率。 尽管这条建议适用于所有“视图”布局，但对通知尤为重要，因为抽屉式通知栏中的空间非常有限。 不要让自定义布局过于复杂，同时确保在各种配置中对其进行测试

这里新建一个message.xml文件，用于自定义通知布局

```java
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="#5caaf9"
    android:orientation="horizontal">

    <ImageView
        android:layout_width="50dp"
        android:layout_height="50dp"
        android:layout_gravity="center_horizontal"
        android:src="@drawable/lufei" />

    <TextView
        android:id="@+id/tv_title"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_gravity="center_vertical"
        android:gravity="center_horizontal"
        android:textColor="#000000" />

</LinearLayout>
```
主要代码：

```java
public void notification(View view) {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.lufei);

        RemoteViews rv = new RemoteViews(getPackageName(), R.layout.message);
        rv.setTextViewText(R.id.tv_title, "叶应是叶");
        builder.setContent(rv);

        mNotificationManager.notify(id++, builder.build());
    }
```

运行效果

![这里写图片描述](http://upload-images.jianshu.io/upload_images/2552605-7b1267c8408cd848?imageMogr2/auto-orient/strip)