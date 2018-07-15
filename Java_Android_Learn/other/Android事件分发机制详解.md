TouchEvent 事件分发机制算作是 Android 开发中很重要的知识点了，以前一直对这个传递过程有点模糊，现在来仔细研究下这整个过程

### 一、概念解释

触摸事件对应的是 MotionEvent 类，触摸事件的类型分为如下三种：

 - Action_Down ：用户手指的按下操作，标志着一次触摸事件的开始
 - Action_Move：用户手指按压屏幕后，在松开手指之前如果移动距离超出一定的阈值，则发生了Action _ Move 事件
 - Action_Up：用户手指离开屏幕时触发的操作，标志着当前触摸事件的结束

在一次屏幕触摸操作中，Action_Down 和 Action_Up 这两个事件是必需的，Action_Move 事件则视情况而定

通过 MotionEvent 对象可以得到点击事件发生的 x 和 y 坐标。系统提供了两组方法： **getX / getY** 和 **getRawX / getRawY** 。两组方法之间的区别在于：**getX / getY** 返回的是相对于当前 View 左上角的 x 和 y 坐标，而 **getRawX / getRawY** 返回的是相对于手机屏幕左上角的 x 和 y 坐标

### 二、事件传递的三个阶段

一次完整的事件传递包括三个阶段，分别是事件的发布、拦截和消费。发生事件传递的视图可以分为三类：Activity、View 和 ViewGroup

#### 2.1、发布（Dispatch）:

事件的发布对应着如下方法：

```java
public boolean dispatchTouchEvent(MotionEvent ev)
```
在 Android 系统中，所有的触摸事件都是通过这个方法来发布的，如果事件能够传递给当前 View，则此方法一定会被调用。在这个方法中，根据当前视图的具体实现逻辑，来决定是直接消费这个事件还是将事件继续发布给子视图处理。
返回 true 表示事件被当前视图消费掉，不再继续发布事件。返回 false 则依据视图类型会有所不同。返回 `super.dispatchTouchEvent(ev)` 表示继续发布该事件。如果当前视图是 ViewGroup 及其子类，则会调用 `onInterceptTouchEvent(MotionEvent ev)` 方法判定是否拦截该事件

#### 2.2、拦截（Intercept）:

事件的拦截对应着如下方法：

```java
public boolean onInterceptTouchEvent(MotionEvent ev)
```
这个方法只在 ViewGroup 及其子类中才有，在 View 和 Activity 中是不存在的。该方法通过返回值来决定是否拦截对应的事件。返回 true 表示拦截这个事件，不继续发布给子视图，同时交由自身的 `onTouchEvent(MotionEvent event)` 方法进行处理；返回 false 或者 `super.onInterceptTouchEvent(ev)` 表示不对事件进行拦截，继续传递给子视图。如果当前ViewGroup 拦截了某个事件，那么在同一个事件序列当中，此方法不会被再次调用

#### 2.3、消费（Consume）:

事件的消费对应着如下方法：

```java
public boolean onTouchEvent(MotionEvent event)
```
该方法返回 true 表示当前视图可以处理对应的事件，事件将不会传递给父视图；返回 false 表示当前视图不处理这个事件，事件会被传递给父视图的相同方法进行处理

#### 2.4、联系: 

三个方法之间的联系可以以如下伪代码来表示：
```java
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean consume = false;
        if (onInterceptTouchEvent(ev)) {
            consume = onTouchEvent(ev);
        } else {
            consume = child.dispatchTouchEvent(ev);
        }
        return consume;
    }
```
对于一个根 ViewGroup 来说，点击事件发生后，首先会传递给它，这时它的 `dispatchTouchEvent` 方法就会被调用，如果它的 `onInterceptTouchEvent` 方法返回 true 就表示要拦截当前事件，接着事件就会交由 ViewGroup 的 `onTouchEvent` 方法进行处理。如果 `onInterceptTouchEvent` 方法返回 fasle，就表示它不拦截当前事件，事件会继续传递给 ViewGroup 的子元素，再次重复以上步骤

此外，View 的 `onTouchEvent` 方法默认都会返回 true，即消耗事件，除非 View 是不可点击的（**clickable 和 longClickable 同时为 false**）。View 的 longClickable 属性默认都为 false，clickable 属性则不一定，例如 Button 的clickable 属性默认为 true，TextView 的clickable 属性默认为 false。View 的 enable 属性不影响 onTouchEvent 方法的默认返回值。即时View是 disable 状态的，只要它的 clickable 或者 longClickable 有一个为 true，则 onTouchEvent 方法就返回 true 

我们可以为 View 设置 `setOnTouchListener` 方法和 `setOnClickListener` 方法，与 View 内部的 `onTouchEvent` 方法的优先级进行比较，依次是 **setOnTouchListener > onTouchEvent > setOnClickListener** 
此外，如果为 View 设置了 `setOnClickListener` 方法和 `setOnLongClickListener` 方法，则会分别将 View 的 clickable 和 longClickable 设置为 true

### 三、View 的事件传递流程

首先继承 AppCompatTextView 类并重写其与 TouchEvent 事件发布相关的两个方法，输出相应的触摸事件类型

```java
/**
 * Created by CZY on 2017/6/7.
 */
public class MyTextView extends AppCompatTextView {

    private final String TAG = "MyTextView";

    public MyTextView(Context context) {
        super(context);
    }

    public MyTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.e(TAG, "dispatchTouchEvent  ACTION_DOWN");
                break;
            case MotionEvent.ACTION_MOVE:
                Log.e(TAG, "dispatchTouchEvent  ACTION_MOVE");
                break;
            case MotionEvent.ACTION_UP:
                Log.e(TAG, "dispatchTouchEvent  ACTION_UP");
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.e(TAG, "onTouchEvent  ACTION_DOWN");
                break;
            case MotionEvent.ACTION_MOVE:
                Log.e(TAG, "onTouchEvent  ACTION_MOVE");
                break;
            case MotionEvent.ACTION_UP:
                Log.e(TAG, "onTouchEvent  ACTION_UP");
                break;
        }
        return super.onTouchEvent(event);
    }

}

```
在布局文件中声明使用 MyTextView

```java
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:gravity="center"
    tools:context="com.czy.touchevent.MainActivity">

    <com.czy.touchevent.MyTextView
        android:id="@+id/myTextView"
        android:layout_width="200dp"
        android:layout_height="200dp"
        android:background="#abc"
        android:gravity="center"
        android:text="点击" />

</LinearLayout>

```

重写 MainActivity 中与触摸事件相关的两个方法，输出相应的触摸事件类型，并为 MyTextView 设置 TouchEvent 事件监听

```java
public class MainActivity extends AppCompatActivity implements View.OnTouchListener{

    private final String TAG = "Activity";

    private final String TAG_VIEW = "MyTextView";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.myTextView).setOnTouchListener(this);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.e(TAG, "dispatchTouchEvent  ACTION_DOWN");
                break;
            case MotionEvent.ACTION_MOVE:
                Log.e(TAG, "dispatchTouchEvent  ACTION_MOVE");
                break;
            case MotionEvent.ACTION_UP:
                Log.e(TAG, "dispatchTouchEvent  ACTION_UP");
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.e(TAG, "onTouchEvent  ACTION_DOWN");
                break;
            case MotionEvent.ACTION_MOVE:
                Log.e(TAG, "onTouchEvent  ACTION_MOVE");
                break;
            case MotionEvent.ACTION_UP:
                Log.e(TAG, "onTouchEvent  ACTION_UP");
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (v.getId()) {
            case R.id.myTextView:
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        Log.e(TAG_VIEW, "onTouch  ACTION_DOWN");
                        break;
                    case MotionEvent.ACTION_MOVE:
                        Log.e(TAG_VIEW, "onTouch  ACTION_MOVE");
                        break;
                    case MotionEvent.ACTION_UP:
                        Log.e(TAG_VIEW, "onTouch  ACTION_UP");
                        break;
                }
                break;
        }
        return false;
    }

}
```
此时，运行程序后点击 MyTextView 控件，输出的 Log 如下所示：

```java
06-13 12:13:26.384 9522-9522/com.czy.touchevent E/Activity: dispatchTouchEvent    ACTION_DOWN
06-13 12:13:26.384 9522-9522/com.czy.touchevent E/MyTextView: dispatchTouchEvent  ACTION_DOWN
06-13 12:13:26.385 9522-9522/com.czy.touchevent E/MyTextView: onTouch             ACTION_DOWN
06-13 12:13:26.385 9522-9522/com.czy.touchevent E/MyTextView: onTouchEvent        ACTION_DOWN
06-13 12:13:26.385 9522-9522/com.czy.touchevent E/Activity: onTouchEvent          ACTION_DOWN
06-13 12:13:26.480 9522-9522/com.czy.touchevent E/Activity: dispatchTouchEvent    ACTION_UP
06-13 12:13:26.480 9522-9522/com.czy.touchevent E/Activity: onTouchEvent          ACTION_UP
```
在默认情况下，Activity 与 MyTextView 的各个触摸事件相关方法的调用顺序如上所示。可以看到，MyTextView 的 onTouch 方法比 onTouchEvent 方法更早被调用，说明 onTouch 方法的优先级更高。

dispatchTouchEvent 方法和  onTouchEvent 方法的返回值存在三种情况：

 - 返回 true
 - 返回 false
 - 返回 父类的同名方法

通过不断改变 Activity 与 MyTextView 中各个方法的返回值，可以得到如下所示的TouchEvent事件发布机制流程图：
![这里写图片描述](http://upload-images.jianshu.io/upload_images/2552605-12aabc243379bdf8?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

从上面的流程图可以得出以下结论：

 - 触摸事件的传递流程是从 `dispatchTouchEvent` 方法开始的，如果默认返回父类的同名函数，则事件将会依照嵌套层次从外层向内层传递，到达最内层的 View 时，就由它的 `onTouchEvent` 方法进行处理。该方法返回 true 则表示消费了该事件；如果处理不了则返回 false，这时事件会重新向外层传递，并交由外层的 View、ViewGroup 或者 Activity 的 `onTouchEvent` 方法进行处理，依次类推
 - 如果事件传递在向内层传递过程中由于人为干预，事件处理函数返回 true ，则会导致事件被提前消费掉，内层 View 或 ViewGroup 将不会收到这个事件
 - View 控件的事件触发顺序是先执行 onTouch 方法，再执行 onTouchEvent 方法，onClick 方法排在最后。如果优先级高的方法返回了 true，则事件将不会继续传递

### 四、ViewGroup 的事件传递流程

ViewGroup 相比 View和Activity多出了一个 `onInterceptTouchEvent(MotionEvent ev)` 方法
首先继承 LinearLayout 类并重写其与 TouchEvent 事件发布相关的三个方法，输出相应的触摸事件类型
```java
/**
 * Created by CZY on 2017/6/7.
 */
public class MyLinearLayout extends LinearLayout {

    private final String TAG = "外层ViewGroup";

    public MyLinearLayout(Context context) {
        super(context);
    }

    public MyLinearLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MyLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.e(TAG, "dispatchTouchEvent  ACTION_DOWN");
                break;
            case MotionEvent.ACTION_MOVE:
                Log.e(TAG, "dispatchTouchEvent  ACTION_MOVE");
                break;
            case MotionEvent.ACTION_UP:
                Log.e(TAG, "dispatchTouchEvent  ACTION_UP");
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.e(TAG, "onInterceptTouchEvent   ACTION_DOWN");
                break;
            case MotionEvent.ACTION_MOVE:
                Log.e(TAG, "onInterceptTouchEvent   ACTION_MOVE");
                break;
            case MotionEvent.ACTION_UP:
                Log.e(TAG, "onInterceptTouchEvent   ACTION_UP");
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.e(TAG, "onTouchEvent  ACTION_DOWN");
                break;
            case MotionEvent.ACTION_MOVE:
                Log.e(TAG, "onTouchEvent  ACTION_MOVE");
                break;
            case MotionEvent.ACTION_UP:
                Log.e(TAG, "onTouchEvent  ACTION_UP");
                break;
        }
        return super.onTouchEvent(event);
    }

}
```
Activity 的布局文件代码如下所示：

```java
<?xml version="1.0" encoding="utf-8"?>
<com.czy.touchevent.MyLinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:gravity="center"
    tools:context="com.czy.touchevent.MainActivity">

    <com.czy.touchevent.MyTextView
        android:id="@+id/myTextView"
        android:layout_width="200dp"
        android:layout_height="200dp"
        android:background="#5deccf"
        android:gravity="center"
        android:text="点击" />

</com.czy.touchevent.MyLinearLayout>
```
运行程序后点击 MyTextView 控件，输出的Log如下所示：

```java
06-18 04:22:54.669 12309-12309/com.czy.touchevent E/Activity: dispatchTouchEvent  ACTION_DOWN
06-18 04:22:54.669 12309-12309/com.czy.touchevent E/外层ViewGroup: dispatchTouchEvent  ACTION_DOWN
06-18 04:22:54.669 12309-12309/com.czy.touchevent E/外层ViewGroup: onInterceptTouchEvent   ACTION_DOWN
06-18 04:22:54.669 12309-12309/com.czy.touchevent E/MyTextView: dispatchTouchEvent  ACTION_DOWN
06-18 04:22:54.669 12309-12309/com.czy.touchevent E/MyTextView: onTouch  ACTION_DOWN
06-18 04:22:54.669 12309-12309/com.czy.touchevent E/MyTextView: onTouchEvent  ACTION_DOWN
06-18 04:22:54.669 12309-12309/com.czy.touchevent E/外层ViewGroup: onTouchEvent  ACTION_DOWN
06-18 04:22:54.669 12309-12309/com.czy.touchevent E/Activity: onTouchEvent  ACTION_DOWN
06-18 04:22:54.764 12309-12309/com.czy.touchevent E/Activity: dispatchTouchEvent  ACTION_UP
06-18 04:22:54.764 12309-12309/com.czy.touchevent E/Activity: onTouchEvent  ACTION_UP

```
在默认情况下，Activity 、ViewGroup 和 View 的各个触摸事件相关方法的调用顺序如上所示
通过不断改变 Activity、ViewGroup 和 View 中各个方法的返回值，可以得到如下所示的 TouchEvent 事件发布机制流程图：
![这里写图片描述](http://upload-images.jianshu.io/upload_images/2552605-c301c1233d6c4dba?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

从上面的流程图可以得出以下结论：

 - ViewGroup 通过 `onInterceptTouchEvent` 方法对事件进行拦截。如果该方法返回 true，则事件不会继续传递给子View；如果返回 false 或 `super.onInterceptTouchEvent` ，则事件会继续传递给子 View
 - 在子 View 中对事件进行消费后，ViewGroup 将接收不到任何事件

### 五、事件传递的“记忆”功能

从上边展示的事件传递默认的方法调用顺序可以看出来，Action_Up 事件都是直接交由 Activity 进行处理，而没有传递给内部的 ViewGroup 或 View
其实，`dispatchTouchEvent` 方法具有“记忆”功能。如果 Action_Down 事件传递给了某 ViewGroup（或者Activity），ViewGroup 默认继续向下传递交由子View进行处理，ViewGroup 会记录该事件是否被子View给消费了。那 ViewGroup 如何知道子 View 是否消费了该事件呢？如果该事件会再次向上传递给 ViewGroup 的 `onTouchEvent` 方法进行处理，那就说明子 View 没能消费掉该事件。当第二次事件（Action_Move或者Action_Up）向下传递到该 ViewGroup， ViewGroup 的 `dispatchTouchEvent` 方法会进行判断，若子 View 消费了上次的 Action_Down 事件，那么本次事件就继续向下传递交由子 View 进行处理，若上次的事件没有被子 View 所消费，那么本次的事件就不会继续向下传递了，ViewGroup 直接调用自己的 `onTouchEvent` 方法来处理该事件
“记忆”的有效期只在单次的触摸事件中，即从Action_Down 事件开始，在 Action_Up 事件结束
